// port-lint: tests tests/compare.rs
package io.github.kotlinmania.unicodeident

import io.github.kotlinmania.unicodeident.tests.tables.byName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnicodeIdentTest {
    // Sanity checks over ASCII: verify that ASCII letters, digits, and
    // underscore behave according to Unicode Standard Annex #31.

    @Test
    fun asciiLettersAreXidStart() {
        for (ch in 'A'..'Z') assertTrue(isXidStart(ch), "Expected $ch to be XID_Start")
        for (ch in 'a'..'z') assertTrue(isXidStart(ch), "Expected $ch to be XID_Start")
    }

    @Test
    fun asciiLettersAreXidContinue() {
        for (ch in 'A'..'Z') assertTrue(isXidContinue(ch), "Expected $ch to be XID_Continue")
        for (ch in 'a'..'z') assertTrue(isXidContinue(ch), "Expected $ch to be XID_Continue")
    }

    @Test
    fun asciiDigitsAreNotXidStart() {
        for (ch in '0'..'9') assertFalse(isXidStart(ch), "Expected $ch to not be XID_Start")
    }

    @Test
    fun asciiDigitsAreXidContinue() {
        for (ch in '0'..'9') assertTrue(isXidContinue(ch), "Expected $ch to be XID_Continue")
    }

    @Test
    fun underscoreIsNotXidStart() {
        assertFalse(isXidStart('_'))
    }

    @Test
    fun underscoreIsXidContinue() {
        assertTrue(isXidContinue('_'))
    }

    @Test
    fun punctuationIsNotXidStartOrContinue() {
        for (ch in listOf(' ', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=')) {
            assertFalse(isXidStart(ch), "Expected $ch to not be XID_Start")
            assertFalse(isXidContinue(ch), "Expected $ch to not be XID_Continue")
        }
    }

    @Test
    fun selectedNonAsciiCharsAreXidStart() {
        // Greek small letter alpha U+03B1
        assertTrue(isXidStart('\u03B1'))
        // CJK Unified Ideograph U+4E2D
        assertTrue(isXidStart('\u4E2D'))
        // Latin small letter e with acute U+00E9
        assertTrue(isXidStart('\u00E9'))
    }

    @Test
    fun unicodeVersionIsPopulated() {
        val (major, minor, patch) = UNICODE_VERSION
        assertTrue(major > 0, "Major version should be positive, got $major")
        assertEquals(0, minor.toInt())
        assertEquals(0, patch.toInt())
    }

    @Test
    fun scalarApiUsesTheExistingTablesBeyondUtf16Char() {
        assertTrue(isXidStartCodePoint(0x10400)) // Deseret capital letter long I
        assertTrue(isXidContinueCodePoint(0x10400))
        assertFalse(isXidStartCodePoint(0x1D7CE)) // Mathematical bold digit zero
        assertTrue(isXidContinueCodePoint(0x1D7CE))
        assertFalse(isXidStartCodePoint(0x1F600)) // Grinning face
        assertFalse(isXidContinueCodePoint(0x1F600))
        assertFalse(isXidStartCodePoint(0xE0100)) // Variation selector
        assertTrue(isXidContinueCodePoint(0xE0100))
    }

    @Test
    fun invalidScalarValuesAreRejected() {
        for (cp in listOf(Int.MIN_VALUE, -1, 0xD800, 0xDBFF, 0xDC00, 0xDFFF, 0x110000, Int.MAX_VALUE)) {
            assertFalse(isXidStartCodePoint(cp), cp.toString())
            assertFalse(isXidContinueCodePoint(cp), cp.toString())
        }
    }

    @Test
    fun charApiRetainsItsBmpBehavior() {
        for (ch in listOf('\u0000', 'A', '0', '_', '\u00E9', '\u0301', '\u4E2D', '\uD7FF', '\uD800', '\uDFFF', '\uE000', '\uFFFF')) {
            assertEquals(isXidStartCodePoint(ch.code), isXidStart(ch))
            assertEquals(isXidContinueCodePoint(ch.code), isXidContinue(ch))
        }
    }

    @Test
    fun compareAllImplementations() {
        val xidStartRanges = byName.single { it.first == "XID_Start" }.second
        val xidContinueRanges = byName.single { it.first == "XID_Continue" }.second
        var startRangeIndex = 0
        var continueRangeIndex = 0
        var scalarCount = 0
        var bmpScalarCount = 0

        // Character iteration covers every Unicode scalar and excludes UTF-16 surrogates.
        // The independent Unicode 17 range tables supply the expected classifications.
        for (cp in 0..0x10FFFF) {
            if (cp in 0xD800..0xDFFF) continue

            while (startRangeIndex < xidStartRanges.size && cp > xidStartRanges[startRangeIndex].last) {
                startRangeIndex++
            }
            while (continueRangeIndex < xidContinueRanges.size && cp > xidContinueRanges[continueRangeIndex].last) {
                continueRangeIndex++
            }
            val expectedStart = startRangeIndex < xidStartRanges.size && cp >= xidStartRanges[startRangeIndex].first
            val expectedContinue = continueRangeIndex < xidContinueRanges.size && cp >= xidContinueRanges[continueRangeIndex].first
            val thoughtToBeStart = isXidStartCodePoint(cp)
            val thoughtToBeContinue = isXidContinueCodePoint(cp)

            assertEquals(expectedStart, thoughtToBeStart, "XID Start at U+${cp.toString(16)}")
            assertEquals(expectedContinue, thoughtToBeContinue, "XID Continue at U+${cp.toString(16)}")
            scalarCount++

            if (cp <= Char.MAX_VALUE.code) {
                assertEquals(expectedStart, isXidStart(cp.toChar()), "Char XID Start at U+${cp.toString(16)}")
                assertEquals(expectedContinue, isXidContinue(cp.toChar()), "Char XID Continue at U+${cp.toString(16)}")
                bmpScalarCount++
            }

            // unicode-xid: its external comparator requires a Kotlin sibling port.
            // ucd-trie: its external comparator requires a Kotlin sibling port.
            // fst: its external comparator requires a Kotlin sibling port.
            // roaring: its external comparator requires a Kotlin sibling port.
        }
        assertEquals(1_112_064, scalarCount)
        assertEquals(63_488, bmpScalarCount)
    }

    // compare.rs also cross-validates isXidStart/isXidContinue against the
    // fst, roaring, ucd-trie, and unicode-xid Rust crates; those crates have
    // no Kotlin sibling ports, so the cross-validation subset is not ported.
}
