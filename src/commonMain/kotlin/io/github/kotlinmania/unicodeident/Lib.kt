// port-lint: source src/lib.rs
package io.github.kotlinmania.unicodeident

/**
 * [![github]](https://github.com/KotlinMania/unicode-ident-kotlin)
 *
 * Implementation of [Unicode Standard Annex #31][tr31] for determining which
 * [Char] values are valid in programming language identifiers.
 *
 * [tr31]: https://www.unicode.org/reports/tr31/
 *
 * This library is a Kotlin Multiplatform port of
 * [dtolnay/unicode-ident](https://github.com/dtolnay/unicode-ident),
 * a better optimized implementation of the older `unicode-xid` crate. It uses
 * less static storage, and is able to classify both ASCII and non-ASCII code
 * points with better performance, 6× faster than `unicode-xid`.
 *
 * <br>
 *
 * ## Comparison of data structures
 *
 * #### unicode-xid
 *
 * Uses a sorted array of character ranges and performs a binary search to look
 * up whether a given character lands inside one of those ranges.
 *
 * The static storage used by this data structure scales with the number of
 * contiguous ranges of identifier code points in Unicode. Every table entry
 * consumes 8 bytes.
 *
 * On a system with 64-byte cache lines, binary searching the table touches 7
 * cache lines on average. Each cache line fits only 8 table entries.
 * Additionally, the branching performed during the binary search is probably
 * mostly unpredictable to the branch predictor.
 *
 * #### ucd-trie
 *
 * Uses a compressed trie set tailored for Unicode code points, with a design
 * credited to Raph Levien in [rust-lang/rust#33098][pr33098]. The trie
 * achieves prefix compression by mapping the final states of the trie into
 * 64-bit integer leaves, where each bit position indicates whether a particular
 * code point is in the set. Lookups touch 1, 2, or 3 cache lines depending on
 * the trie partition (1- or 2-byte UTF-8, 3-byte UTF-8, or 4-byte UTF-8).
 *
 * [pr33098]: https://github.com/rust-lang/rust/pull/33098
 *
 * #### fst
 *
 * Uses a finite state transducer. No known advantage over `ucd-trie` for this
 * use case; it does not specialize for the fact that only 21 of the 32 bits in
 * a Unicode code point are meaningful.
 *
 * #### roaring
 *
 * A pure implementation of [Roaring Bitmap][roaring], a data structure for
 * storing sets of 32-bit unsigned integers. Reasonably competitive performance
 * but substantially worse compression: 6× as much storage for the same data.
 *
 * [roaring]: https://roaringbitmap.org/about/
 *
 * #### unicode-ident / unicode-ident-kotlin
 *
 * Most similar to `ucd-trie`, using bitmaps stored in the leaves of a trie,
 * achieving both prefix compression and suffix compression.
 *
 * Key differences from `ucd-trie`:
 *
 * - Uses a single 2-level trie instead of 3 disjoint partitions of different
 *   depth.
 * - Uses significantly larger chunks: 512 bits rather than 64 bits.
 * - Compresses the XID_Start and XID_Continue properties together
 *   simultaneously rather than duplicating identical trie leaf chunks across
 *   the two.
 *
 * This crate stores one 512-bit "row" of the XID bitmaps in the leaf level of
 * the trie and a single additional level to index into the leaves. There are
 * 134 unique 512-bit chunks across the two bitmaps.
 *
 * The chunk size of 512 bits is selected as the size that minimizes the total
 * size of the data structure. A smaller chunk would achieve better
 * deduplication but require a larger index; a larger chunk would increase
 * redundancy in the leaf bitmaps.
 *
 * The chunk data is compressed using the Kuhn–Munkres algorithm for bipartite
 * matching to eliminate redundancies between the second half of any chunk and
 * the first half of any other chunk. This achieves an additional 9%
 * compression of the leaf level, leaving 122 chunks indexed at the half-chunk
 * level using an 8-bit index. Performing lookups in this data structure is
 * straight-line code with no branching.
 */

private val zero: Byte = 0

/** Whether the character has the Unicode property XID_Start. */
fun isXidStart(ch: Char): Boolean {
    if (ch.code < 128) {
        val cp = ch.code
        return if (cp < 64) {
            (ASCII_START_LO ushr cp) and 1L != 0L
        } else {
            (ASCII_START_HI ushr (cp - 64)) and 1L != 0L
        }
    }
    val cp = ch.code
    val chunk = TRIE_START.getOrElse(cp / 8 / CHUNK) { zero }.toInt() and 0xFF
    val offset = chunk * CHUNK / 2 + cp / 8 % CHUNK
    return (LEAF[offset].toInt() and 0xFF) ushr (cp % 8) and 1 != 0
}

/** Whether the character has the Unicode property XID_Continue. */
fun isXidContinue(ch: Char): Boolean {
    if (ch.code < 128) {
        val cp = ch.code
        return if (cp < 64) {
            (ASCII_CONTINUE_LO ushr cp) and 1L != 0L
        } else {
            (ASCII_CONTINUE_HI ushr (cp - 64)) and 1L != 0L
        }
    }
    val cp = ch.code
    val chunk = TRIE_CONTINUE.getOrElse(cp / 8 / CHUNK) { zero }.toInt() and 0xFF
    val offset = chunk * CHUNK / 2 + cp / 8 % CHUNK
    return (LEAF[offset].toInt() and 0xFF) ushr (cp % 8) and 1 != 0
}
