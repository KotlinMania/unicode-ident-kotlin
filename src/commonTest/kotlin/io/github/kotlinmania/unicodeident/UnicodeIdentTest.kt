// port-lint: source tests/compare.rs
package io.github.kotlinmania.unicodeident

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

    // compare.rs also cross-validates isXidStart/isXidContinue against the
    // fst, roaring, ucd-trie, and unicode-xid Rust crates; those crates have
    // no Kotlin sibling ports, so the cross-validation subset is not ported.
}
