package com.mima.feltwords.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionOpsTest {
    @Test
    fun upsertLearnedWordMovesDuplicateToFrontIgnoringCase() {
        val oldApple = word(id = "old", value = "Apple")
        val ball = word(id = "ball", value = "ball")
        val newApple = word(id = "new", value = "apple")

        val result = upsertLearnedWord(listOf(oldApple, ball), newApple)

        assertEquals(listOf("new", "ball"), result.map { it.id })
    }

    @Test
    fun restoreAtIndicesPreservesOriginalOrderAndRemovesDuplicates() {
        val current = listOf(word("c", "cat"))
        val restored = listOf(1 to word("b", "ball"), 0 to word("a", "apple"))

        val result = restoreAtIndices(current, restored, LearnedWord::id)

        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun updateHistoryImageOnlyChangesMatchingItem() {
        val first = history("a")
        val second = history("b")

        val result = updateHistoryImage(listOf(first, second), "b", "/tmp/felt.jpg")

        assertEquals(null, result[0].imageUrl)
        assertEquals("/tmp/felt.jpg", result[1].imageUrl)
    }

    private fun word(id: String, value: String) = LearnedWord(
        id = id,
        word = value,
        displayNameZh = value,
        exampleSentence = value,
        category = "test",
    )

    private fun history(id: String) = RecognitionHistoryItem(
        id = id,
        result = RecognitionResult("apple", "苹果", 1.0, "food", "fruit", "An apple.", "apple"),
    )
}
