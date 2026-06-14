package com.mima.feltwords.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mima.feltwords.data.ServiceLocator
import com.mima.feltwords.data.api.AgnesRepository
import com.mima.feltwords.data.store.LocalStore
import com.mima.feltwords.data.store.ProfileStore
import com.mima.feltwords.data.weather.WeatherRepository
import com.mima.feltwords.domain.model.DailyTask
import com.mima.feltwords.domain.model.LearnedWord
import com.mima.feltwords.domain.model.RecognitionHistoryItem
import com.mima.feltwords.domain.model.RecognitionResult
import com.mima.feltwords.domain.model.Storybook
import com.mima.feltwords.domain.model.restoreAtIndices
import com.mima.feltwords.domain.model.updateHistoryImage
import com.mima.feltwords.domain.model.upsertLearnedWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

/**
 * 正在后台生成的绘本占位（内存态，不持久化）。
 * 对齐 iOS GeneratingStory 结构体。
 */
data class GeneratingStory(
    val id: String = UUID.randomUUID().toString(),
    val focusWord: String,
    val progressDone: Int = 0,
    val progressTotal: Int = 4,
    val coverUrl: String? = null,
    val failed: Boolean = false,
    /** 识别结果，重试时需要 */
    val result: RecognitionResult,
    /** 参考图，内存持有，重试时复用 */
    val reference: Bitmap? = null,
)

/**
 * 吉祥物每日主题 —— 对齐 iOS MascotDailyTheme。
 */
data class MascotDailyTheme(
    val assetName: String,
    val title: String,
    val subtitle: String,
)

/**
 * 共享 ViewModel —— 在 RootScaffold 层创建，向下传递给各页面。
 * P3: 绘本相关状态与方法
 * P4: 扩展单词本/历史/首页/头像/今日任务/天气
 *
 * 对齐 iOS AppModel 完整语义。
 */
class AppViewModel : ViewModel() {

    private val repository: AgnesRepository = ServiceLocator.agnesRepository
    private val localStore: LocalStore = ServiceLocator.localStore
    private val profileStore: ProfileStore = ServiceLocator.profileStore
    val weather: WeatherRepository = ServiceLocator.weatherRepository

    // ──────────────── 已完成绘本 ────────────────

    private val _stories = MutableStateFlow<List<Storybook>>(emptyList())
    val stories: StateFlow<List<Storybook>> = _stories.asStateFlow()

    // ──────────────── 生成中占位卡片（内存态） ────────────────

    private val _generatingStories = MutableStateFlow<List<GeneratingStory>>(emptyList())
    val generatingStories: StateFlow<List<GeneratingStory>> = _generatingStories.asStateFlow()

    // ──────────────── 单词本 ────────────────

    private val _words = MutableStateFlow<List<LearnedWord>>(emptyList())
    val words: StateFlow<List<LearnedWord>> = _words.asStateFlow()
    private val _backfillingWordIDs = MutableStateFlow<Set<String>>(emptySet())
    val backfillingWordIDs: StateFlow<Set<String>> = _backfillingWordIDs.asStateFlow()

    // ──────────────── 历史记录 ────────────────

    private val _history = MutableStateFlow<List<RecognitionHistoryItem>>(emptyList())
    val history: StateFlow<List<RecognitionHistoryItem>> = _history.asStateFlow()

    /** 正在生成毛毡图的历史条目 id（仅内存，重启后不残留"生成中"假态） */
    private val _generatingHistoryIDs = MutableStateFlow<Set<String>>(emptySet())
    val generatingHistoryIDs: StateFlow<Set<String>> = _generatingHistoryIDs.asStateFlow()

    // ──────────────── 头像 ────────────────

    private val _avatarImage = MutableStateFlow<Bitmap?>(null)
    val avatarImage: StateFlow<Bitmap?> = _avatarImage.asStateFlow()

    // ──────────────── 今日任务 ────────────────

    private val _tasks = MutableStateFlow(defaultTasks())
    val tasks: StateFlow<List<DailyTask>> = _tasks.asStateFlow()

    // ──────────────── 吉祥物每日主题 ────────────────

    val mascotDailyTheme: MascotDailyTheme =
        mascotThemes[profileStore.nextMascotThemeIndex(mascotThemes.size)]

    // ──────────────── 今日统计 ────────────────

    val todayHistoryCount: Int
        get() = _history.value.count { isToday(it.recognizedAt) }

    val todayWordCount: Int
        get() = _words.value.count { isToday(it.learnedAt) }

    val todayStoryCount: Int
        get() = _stories.value.count { isToday(it.createdAt) }

    // ──────────────── 初始化 ────────────────

    init {
        viewModelScope.launch {
            _stories.value = localStore.loadStories()
            _words.value = localStore.loadWords()
            linkStoryImagesToWords()
            _history.value = localStore.loadHistory()
            _tasks.value = localStore.loadTasks() ?: defaultTasks()
            _avatarImage.value = profileStore.loadAvatar()
            // 加载天气
            weather.loadIfNeeded()
        }
    }

    // ══════════════════════════════════════════
    //  绘本管理
    // ══════════════════════════════════════════

    fun startStoryGeneration(
        result: RecognitionResult,
        reference: Bitmap?,
        coverUrl: String?,
    ) {
        val job = GeneratingStory(
            focusWord = result.word,
            progressDone = 0,
            progressTotal = 4,
            coverUrl = coverUrl,
            failed = false,
            result = result,
            reference = reference,
        )
        _generatingStories.update { listOf(job) + it }
        runStoryJob(job.id)
    }

    fun retryStoryGeneration(id: String) {
        _generatingStories.update { list ->
            list.map { job ->
                if (job.id == id) job.copy(failed = false, progressDone = 0) else job
            }
        }
        runStoryJob(id)
    }

    fun dismissStoryJob(id: String) {
        _generatingStories.update { it.filter { job -> job.id != id } }
    }

    fun deleteStory(id: String) {
        _stories.update { it.filter { s -> s.id != id } }
        viewModelScope.launch { localStore.saveStories(_stories.value) }
    }

    fun restoreStories(restored: List<Pair<Int, Storybook>>) {
        _stories.update { current ->
            val mutable = current.toMutableList()
            for ((index, story) in restored.sortedBy { it.first }) {
                mutable.removeAll { it.id == story.id }
                mutable.add(index.coerceAtMost(mutable.size), story)
            }
            mutable
        }
        viewModelScope.launch { localStore.saveStories(_stories.value) }
    }

    private fun runStoryJob(id: String) {
        val job = _generatingStories.value.find { it.id == id } ?: return
        viewModelScope.launch {
            try {
                val storybook = repository.generateIllustratedStory(
                    result = job.result,
                    reference = job.reference,
                    onProgress = { done, total ->
                        _generatingStories.update { list ->
                            list.map { j ->
                                if (j.id == id) j.copy(progressDone = done, progressTotal = total) else j
                            }
                        }
                    },
                )
                _generatingStories.update { it.filter { j -> j.id != id } }
                _stories.update { listOf(storybook) + it }
                localStore.saveStories(_stories.value)
            } catch (_: Exception) {
                _generatingStories.update { list ->
                    list.map { j -> if (j.id == id) j.copy(failed = true) else j }
                }
            }
        }
    }

    // ══════════════════════════════════════════
    //  单词本管理
    // ══════════════════════════════════════════

    /** 保存到单词本（去重）—— 对齐 iOS AppModel.save(word) */
    fun saveWord(result: RecognitionResult, imageUrl: String?) {
        val word = LearnedWord(
            word = result.word,
            displayNameZh = result.displayNameZh,
            exampleSentence = result.exampleSentence,
            category = result.category,
            imageUrl = imageUrl,
        )
        _words.value = upsertLearnedWord(_words.value, word)
        viewModelScope.launch { localStore.saveWords(_words.value) }
    }

    fun deleteWord(id: String) {
        _words.update { it.filter { w -> w.id != id } }
        viewModelScope.launch { localStore.saveWords(_words.value) }
    }

    fun restoreWords(restored: List<Pair<Int, LearnedWord>>) {
        _words.update { current -> restoreAtIndices(current, restored, LearnedWord::id) }
        viewModelScope.launch { localStore.saveWords(_words.value) }
    }

    fun isSavedToWordbook(word: String): Boolean =
        _words.value.any { it.word.equals(word, ignoreCase = true) }

    fun backfillWordImage(id: String) {
        val word = _words.value.firstOrNull { it.id == id } ?: return
        if (word.imageUrl != null || id in _backfillingWordIDs.value) return
        _backfillingWordIDs.update { it + id }
        viewModelScope.launch {
            try {
                val result = RecognitionResult(
                    word = word.word,
                    displayNameZh = word.displayNameZh,
                    confidence = 1.0,
                    category = word.category,
                    childFriendlyDefinition = word.displayNameZh,
                    exampleSentence = word.exampleSentence,
                    visualDescription = word.word,
                )
                val url = repository.generateFeltImage(result)
                _words.update { list -> list.map { if (it.id == id) it.copy(imageUrl = url) else it } }
                localStore.saveWords(_words.value)
            } catch (_: Exception) {
                // 补图属于增强项，失败时保留占位并允许再次点击。
            } finally {
                _backfillingWordIDs.update { it - id }
            }
        }
    }

    private suspend fun linkStoryImagesToWords() {
        var changed = false
        _words.update { words ->
            words.map { word ->
                if (word.imageUrl != null) return@map word
                val image = _stories.value
                    .firstOrNull { it.focusWord.equals(word.word, ignoreCase = true) }
                    ?.pages?.firstOrNull()?.imageUrl
                if (image != null) {
                    changed = true
                    word.copy(imageUrl = image)
                } else word
            }
        }
        if (changed) localStore.saveWords(_words.value)
    }

    // ══════════════════════════════════════════
    //  历史记录管理
    // ══════════════════════════════════════════

    fun saveHistory(result: RecognitionResult, capturedImagePath: String?): String {
        val item = RecognitionHistoryItem(result = result, capturedImagePath = capturedImagePath)
        _history.update { listOf(item) + it }
        _generatingHistoryIDs.update { it + item.id }
        viewModelScope.launch { localStore.saveHistory(_history.value) }
        return item.id
    }

    fun updateHistoryImage(id: String, imageUrl: String?) {
        val recognizedWord = _history.value.firstOrNull { it.id == id }?.result?.word
        _history.update { history -> updateHistoryImage(history, id, imageUrl) }
        if (imageUrl != null && recognizedWord != null) {
            _words.update { words ->
                words.map { word ->
                    if (word.imageUrl == null && word.word.equals(recognizedWord, ignoreCase = true)) {
                        word.copy(imageUrl = imageUrl)
                    } else {
                        word
                    }
                }
            }
        }
        // 生成结束（成功或失败）都解除"生成中"
        _generatingHistoryIDs.update { it - id }
        viewModelScope.launch {
            localStore.saveHistory(_history.value)
            if (imageUrl != null && recognizedWord != null) localStore.saveWords(_words.value)
        }
    }

    /** 删除历史记录条目 */
    fun deleteHistory(id: String) {
        _history.update { it.filter { h -> h.id != id } }
        viewModelScope.launch { localStore.saveHistory(_history.value) }
    }

    fun restoreHistory(restored: List<Pair<Int, RecognitionHistoryItem>>) {
        _history.update { current -> restoreAtIndices(current, restored, RecognitionHistoryItem::id) }
        viewModelScope.launch { localStore.saveHistory(_history.value) }
    }

    // ══════════════════════════════════════════
    //  头像管理
    // ══════════════════════════════════════════

    fun setAvatar(image: Bitmap) {
        _avatarImage.value = image
        viewModelScope.launch { profileStore.saveAvatar(image) }
    }

    fun deleteAvatar() {
        _avatarImage.value = null
        viewModelScope.launch { profileStore.deleteAvatar() }
    }

    // ══════════════════════════════════════════
    //  今日任务
    // ══════════════════════════════════════════

    fun updateTask(index: Int, newCount: Int) {
        _tasks.update { list ->
            list.mapIndexed { i, task ->
                if (i == index) task.copy(count = newCount.coerceIn(1, 99)) else task
            }
        }
        viewModelScope.launch { localStore.saveTasks(_tasks.value) }
    }

    // ══════════════════════════════════════════
    //  工具方法
    // ══════════════════════════════════════════

    private fun isToday(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        cal.timeInMillis = timestamp
        return cal.get(Calendar.DAY_OF_YEAR) == today && cal.get(Calendar.YEAR) == year
    }

    companion object {
        fun defaultTasks(): List<DailyTask> = listOf(
            DailyTask(icon = "camera.fill", prefix = "拍照找 ", count = 1, suffix = " 个英文"),
            DailyTask(icon = "speaker.wave.2.fill", prefix = "听 ", count = 3, suffix = " 次发音"),
            DailyTask(icon = "book.fill", prefix = "看 ", count = 1, suffix = " 本小绘本"),
        )

        private val mascotThemes = listOf(
            MascotDailyTheme("mascot_key_art", "毛毛和朋友们今天也在等你", "一起发现身边的新单词"),
            MascotDailyTheme("daily_eating", "认真吃饭，身体有力量", "今天也要尝一口蔬菜"),
            MascotDailyTheme("daily_learning", "每天认识一个新单词", "小小进步也很了不起"),
            MascotDailyTheme("daily_playing", "一起玩，也要互相照顾", "分享会让快乐变更多"),
            MascotDailyTheme("daily_tidying", "玩具回家，房间更舒服", "我们一起收拾吧"),
            MascotDailyTheme("daily_bedtime", "早点睡觉，明天更有精神", "毛毛和你说晚安"),
        )

    }
}
