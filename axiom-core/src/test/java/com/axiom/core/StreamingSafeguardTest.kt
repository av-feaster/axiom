package com.axiom.core

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for StreamingSafeguard stopping conditions
 */
@RunWith(RobolectricTestRunner::class)
class StreamingSafeguardTest {

    @Test
    fun testShouldStopGeneration_withStopToken() {
        val text = "Some text <|end|> more text"
        val stopTokens = listOf("<|end|>")
        val result = StreamingSafeguard.shouldStopGeneration(text, stopTokens)
        assertTrue("Should stop when stop token is present", result)
    }

    @Test
    fun testShouldStopGeneration_withoutStopToken() {
        val text = "Some text without stop token"
        val stopTokens = listOf("<|end|>")
        val result = StreamingSafeguard.shouldStopGeneration(text, stopTokens)
        assertFalse("Should not stop when stop token is absent", result)
    }

    @Test
    fun testShouldStopGeneration_withUserMarker() {
        val text = "Some text ### User: more text"
        val stopTokens = emptyList<String>()
        val result = StreamingSafeguard.shouldStopGeneration(text, stopTokens)
        assertTrue("Should stop when user marker is present", result)
    }

    @Test
    fun testShouldStopGeneration_withSystemMarker() {
        val text = "Some text ### System: more text"
        val stopTokens = emptyList<String>()
        val result = StreamingSafeguard.shouldStopGeneration(text, stopTokens)
        assertTrue("Should stop when system marker is present", result)
    }

    @Test
    fun testTrimGeneratedText_withStopToken() {
        val text = "Some text <|end|> more text"
        val stopTokens = listOf("<|end|>")
        val result = StreamingSafeguard.trimGeneratedText(text, stopTokens)
        assertEquals("Should trim at stop token", "Some text", result)
    }

    @Test
    fun testTrimGeneratedText_withUserMarker() {
        val text = "Some text ### User: more text"
        val stopTokens = emptyList<String>()
        val result = StreamingSafeguard.trimGeneratedText(text, stopTokens)
        assertEquals("Should trim at user marker", "Some text", result)
    }

    @Test
    fun testShouldStopOnChatEcho_withMarker() {
        val text = "Assistant response ### User:"
        val result = StreamingSafeguard.shouldStopOnChatEcho(text)
        assertTrue("Should stop on chat echo marker", result)
    }

    @Test
    fun testShouldStopOnChatEcho_withoutMarker() {
        val text = "Assistant response without marker"
        val result = StreamingSafeguard.shouldStopOnChatEcho(text)
        assertFalse("Should not stop without chat echo marker", result)
    }

    @Test
    fun testTrimOnChatEcho() {
        val text = "Assistant response ### User: more text"
        val result = StreamingSafeguard.trimOnChatEcho(text)
        assertEquals("Should trim at chat echo marker", "Assistant response", result)
    }

    @Test
    fun testShouldStopOnGarbage_withConsecutiveGarbage() {
        val text = "Normal text \uFFFD\uFFFD\uFFFD"
        val result = StreamingSafeguard.shouldStopOnGarbage(text, maxConsecutiveGarbage = 3)
        assertTrue("Should stop on consecutive garbage characters", result)
    }

    @Test
    fun testShouldStopOnGarbage_withoutGarbage() {
        val text = "Normal text without garbage"
        val result = StreamingSafeguard.shouldStopOnGarbage(text, maxConsecutiveGarbage = 3)
        assertFalse("Should not stop without garbage characters", result)
    }

    @Test
    fun testShouldStopOnGarbage_withFewGarbage() {
        val text = "Normal text \uFFFD\uFFFD"
        val result = StreamingSafeguard.shouldStopOnGarbage(text, maxConsecutiveGarbage = 3)
        assertFalse("Should not stop with fewer than threshold garbage", result)
    }

    @Test
    fun testShouldStopOnRepetition_withRepetition() {
        val text = "aaaaaaaaaa"
        val result = StreamingSafeguard.shouldStopOnRepetition(text, maxRepetition = 10)
        assertTrue("Should stop on character repetition", result)
    }

    @Test
    fun testShouldStopOnRepetition_withoutRepetition() {
        val text = "Normal text without repetition"
        val result = StreamingSafeguard.shouldStopOnRepetition(text, maxRepetition = 10)
        assertFalse("Should not stop without repetition", result)
    }

    @Test
    fun testShouldStopOnRepetition_withShortText() {
        val text = "aa"
        val result = StreamingSafeguard.shouldStopOnRepetition(text, maxRepetition = 10)
        assertFalse("Should not stop on short text", result)
    }

    @Test
    fun testMatchesStopMarkerPrefix_withPrefix() {
        val text = "user"
        val result = StreamingSafeguard.matchesStopMarkerPrefix(text)
        assertTrue("Should match stop marker prefix", result)
    }

    @Test
    fun testMatchesStopMarkerPrefix_withoutPrefix() {
        val text = "normal"
        val result = StreamingSafeguard.matchesStopMarkerPrefix(text)
        assertFalse("Should not match non-prefix", result)
    }

    @Test
    fun testMatchesStopMarkerPrefix_caseInsensitive() {
        val text = "USER"
        val result = StreamingSafeguard.matchesStopMarkerPrefix(text)
        assertTrue("Should match case-insensitive", result)
    }

    @Test
    fun testIsSafeToDisplay_withShortText() {
        val text = "ab"
        val result = StreamingSafeguard.isSafeToDisplay(text)
        assertFalse("Should not display very short text", result)
    }

    @Test
    fun testIsSafeToDisplay_withForbiddenStart() {
        val text = "### User: message"
        val result = StreamingSafeguard.isSafeToDisplay(text)
        assertFalse("Should not display text with forbidden start", result)
    }

    @Test
    fun testIsSafeToDisplay_withNormalText() {
        val text = "Normal safe text"
        val result = StreamingSafeguard.isSafeToDisplay(text)
        assertTrue("Should display normal text", result)
    }

    @Test
    fun testStripThinkingTags_withCompleteBlock() {
        val text = "<|think|>Reasoning here<|/think|>Final answer"
        val result = StreamingSafeguard.stripThinkingTags(text)
        assertEquals("Should strip complete thinking block", "Final answer", result)
    }

    @Test
    fun testStripThinkingTags_withIncompleteBlock() {
        val text = "<|think|>Reasoning here"
        val result = StreamingSafeguard.stripThinkingTags(text)
        assertEquals("Should strip incomplete thinking block", "", result)
    }

    @Test
    fun testStripThinkingTags_withoutTags() {
        val text = "Normal text without tags"
        val result = StreamingSafeguard.stripThinkingTags(text)
        assertEquals("Should return unchanged text without tags", "Normal text without tags", result)
    }

    @Test
    fun testStripThinkingTags_multipleBlocks() {
        val text = "<|think|>First reasoning<|/think|>Answer <|think|>Second reasoning<|/think|>Final"
        val result = StreamingSafeguard.stripThinkingTags(text)
        assertEquals("Should strip multiple thinking blocks", "Answer Final", result)
    }
}
