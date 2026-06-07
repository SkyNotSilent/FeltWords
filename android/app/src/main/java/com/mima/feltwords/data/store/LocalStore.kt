package com.mima.feltwords.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mima.feltwords.domain.model.DailyTask
import com.mima.feltwords.domain.model.LearnedWord
import com.mima.feltwords.domain.model.RecognitionHistoryItem
import com.mima.feltwords.domain.model.Storybook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** DataStore 实例（应用级单例） */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "feltwords")

/**
 * 本地存储 —— 用 DataStore(Preferences) 持久化 4 个 JSON 列表，
 * 对齐 iOS LocalStore（UserDefaults 键值）。
 */
class LocalStore(context: Context) {

    private val dataStore = context.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    // ──────────────── Preference Keys ────────────────

    companion object {
        private val KEY_WORDS = stringPreferencesKey("feltwords.words")
        private val KEY_STORIES = stringPreferencesKey("feltwords.stories")
        private val KEY_HISTORY = stringPreferencesKey("feltwords.history")
        private val KEY_TASKS = stringPreferencesKey("feltwords.tasks")
    }

    // ──────────────── Load ────────────────

    suspend fun loadWords(): List<LearnedWord> = load(KEY_WORDS) ?: emptyList()

    suspend fun loadStories(): List<Storybook> = load(KEY_STORIES) ?: emptyList()

    suspend fun loadHistory(): List<RecognitionHistoryItem> = load(KEY_HISTORY) ?: emptyList()

    /** 返回 null 表示从未保存过，调用方可使用默认值 */
    suspend fun loadTasks(): List<DailyTask>? = load(KEY_TASKS)

    // ──────────────── Save ────────────────

    suspend fun saveWords(words: List<LearnedWord>) = save(KEY_WORDS, words)

    suspend fun saveStories(stories: List<Storybook>) = save(KEY_STORIES, stories)

    suspend fun saveHistory(history: List<RecognitionHistoryItem>) = save(KEY_HISTORY, history)

    suspend fun saveTasks(tasks: List<DailyTask>) = save(KEY_TASKS, tasks)

    // ──────────────── 泛型序列化工具 ────────────────

    private suspend inline fun <reified T> load(key: Preferences.Key<String>): T? {
        val raw = dataStore.data.map { it[key] }.first() ?: return null
        return try {
            json.decodeFromString<T>(raw)
        } catch (_: Exception) {
            null
        }
    }

    private suspend inline fun <reified T> save(key: Preferences.Key<String>, value: T) {
        val raw = json.encodeToString(value)
        dataStore.edit { prefs -> prefs[key] = raw }
    }
}
