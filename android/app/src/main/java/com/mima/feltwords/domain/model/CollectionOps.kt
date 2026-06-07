package com.mima.feltwords.domain.model

fun upsertLearnedWord(current: List<LearnedWord>, word: LearnedWord): List<LearnedWord> =
    listOf(word) + current.filterNot { it.word.equals(word.word, ignoreCase = true) }

fun <T> restoreAtIndices(
    current: List<T>,
    restored: List<Pair<Int, T>>,
    idOf: (T) -> String,
): List<T> {
    val mutable = current.toMutableList()
    restored.sortedBy { it.first }.forEach { (index, item) ->
        mutable.removeAll { idOf(it) == idOf(item) }
        mutable.add(index.coerceIn(0, mutable.size), item)
    }
    return mutable
}

fun updateHistoryImage(
    history: List<RecognitionHistoryItem>,
    id: String,
    imageUrl: String?,
): List<RecognitionHistoryItem> =
    history.map { item -> if (item.id == id) item.copy(imageUrl = imageUrl) else item }
