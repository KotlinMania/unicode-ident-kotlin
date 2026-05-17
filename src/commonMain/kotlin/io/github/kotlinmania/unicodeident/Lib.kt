// port-lint: source lib.rs
package io.github.kotlinmania.unicodeident

/**
 * [![github]](https://github.com/KotlinMania/unicode-ident-kotlin)&ensp;[![maven-central]](https://search.maven.org/artifact/io.github.kotlinmania/unicode-ident-kotlin)
 *
 * [github]: https://img.shields.io/badge/github-8da0cb?style=for-the-badge&labelColor=555555&logo=github
 * [maven-central]: https://img.shields.io/badge/maven--central-fc8d62?style=for-the-badge&labelColor=555555&logo=apachemaven
 *
 * Implementation of [Unicode Standard Annex #31][tr31] for determining which
 * [Char] values are valid in programming language identifiers.
 *
 * [tr31]: https://www.unicode.org/reports/tr31/
 *
 * This library is a better optimized implementation of the older `unicode-xid`
 * approach. It uses less static storage, and is able to classify both ASCII and
 * non-ASCII codepoints with better performance, 6× faster than `unicode-xid`.
 *
 * <br>
 *
 * ## Comparison of performance
 *
 * The following table shows a comparison between five Unicode identifier
 * implementations.
 *
 * - `unicode-ident` is the original Rust crate;
 * - [`unicode-xid`] is a widely used crate run by the "unicode-rs" org;
 * - `ucd-trie` and `fst` are two data structures supported by the
 *   [`ucd-generate`] tool;
 * - [`roaring`] is a Rust implementation of Roaring bitmap.
 *
 * The *static storage* column shows the total size of `static` tables that the
 * implementation bakes into your binary, measured in 1000s of bytes.
 *
 * The remaining columns show the **cost per call** to evaluate whether a
 * single [Char] has the XID_Start or XID_Continue Unicode property, comparing
 * across different ratios of ASCII to non-ASCII codepoints in the input data.
 *
 * [`unicode-xid`]: https://github.com/unicode-rs/unicode-xid
 * [`ucd-generate`]: https://github.com/BurntSushi/ucd-generate
 * [`roaring`]: https://github.com/RoaringBitmap/roaring-rs
 *
 * | | static storage | 0% nonascii | 1% | 10% | 100% nonascii |
 * |---|---|---|---|---|---|
 * | **`unicode-ident`** | 10.0 K | 0.36 ns | 0.37 ns | 0.37 ns | 0.43 ns |
 * | **`unicode-xid`** | 12.0 K | 1.63 ns | 1.70 ns | 1.82 ns | 4.56 ns |
 * | **`ucd-trie`** | 10.4 K | 1.01 ns | 0.73 ns | 0.97 ns | 1.09 ns |
 * | **`fst`** | 144 K | 22.0 ns | 21.9 ns | 20.9 ns | 10.5 ns |
 * | **`roaring`** | 66.1 K | 1.91 ns | 1.90 ns | 1.94 ns | 2.67 ns |
 *
 * Source code for the benchmark is provided in the *bench* directory of the
 * upstream repo and may be repeated by running `cargo criterion`.
 *
 * **Note:** These numbers are from the upstream Rust crate's benchmarks,
 * not measurements of this Kotlin port. Kotlin/JVM runtime characteristics
 * (GC pauses, JIT warmup, bounds checks) will differ substantially.
 *
 * <br>
 *
 * ## Comparison of data structures
 *
* #### unicode-xid
 *
 * They use a sorted array of character ranges, and do a binary search to look
 * up whether a given character lands inside one of those ranges.
 *
 * ```kotlin
 * val xidContinueTable: Array<Pair<Char, Char>> = arrayOf(
 *     Pair('\u0030', '\u0039'),  // 0-9
 *     Pair('\u0041', '\u005A'),  // A-Z
 *     // …
 *     Pair('\uE0100', '\uE01EF'),  // beyond BMP, needs Int codepoint
 * )
 * ```
 *
 * The static storage used by this data structure scales with the number of
 * contiguous ranges of identifier codepoints in Unicode. Every table entry
 * consumes 8 bytes, because the upstream Rust crate `unicode-xid` stores each
 * entry as a pair of 32-bit `char` values (Rust `char` is a 21-bit code point
 * padded to 32 bits; Kotlin [Char] is a 16-bit UTF-16 code unit, so
 * supplementary entries like U+E0100 would need an `Int`-based representation).
 *
 * In some ranges of the Unicode codepoint space, this is quite a sparse
 * representation &mdash; there are some ranges where tens of thousands of
 * adjacent codepoints are all valid identifier characters. In other places,
 * the representation is quite inefficient. A character like `µ` (U+00B5)
 * which is surrounded by non-identifier codepoints consumes 64 bits in the
 * table, while it would be just 1 bit in a dense bitmap.
 *
 * On a system with 64-byte cache lines, binary searching the table touches 7
 * cache lines on average. Each cache line fits only 8 table entries.
 * Additionally, the branching performed during the binary search is probably
 * mostly unpredictable to the branch predictor.
 *
 * Overall, the implementation ends up being about 6× slower on non-ASCII input
 * compared to the fastest implementation.
 *
 * A potential improvement would be to pack the table entries more compactly.
 * Rust's `char` is a 21-bit integer padded to 32 bits, which means every table
 * entry is holding 22 bits of wasted space, adding up to 3.9 K. They could
 * instead fit every table entry into 6 bytes, leaving out some of the
 * padding, for a 25% improvement in space used. With some cleverness it may
 * be possible to fit in 5 bytes or even 4 bytes by storing a low char and an
 * extent, instead of low char and high char. Performance would likely not
 * improve much but this could be the most efficient for space across all the
 * libraries, needing only about 7 K to store.
 *
 * #### ucd-trie
 *
 * Their data structure is a compressed trie set specifically tailored for
 * Unicode codepoints. The design is credited to Raph Levien in
 * [rust-lang/rust#33098][pr33098].
 *
 * [pr33098]: https://github.com/rust-lang/rust/pull/33098
 *
 * ```kotlin
 * class TrieSet(
 *     val tree1Level1: LongArray,   // 32 Longs
 *     val tree2Level1: ByteArray,   // 992 Bytes
 *     val tree2Level2: LongArray,
 *     val tree3Level1: ByteArray,   // 256 Bytes
 *     val tree3Level2: ByteArray,
 *     val tree3Level3: LongArray,
 * )
 * ```
 *
 * It represents codepoint sets using a trie to achieve prefix compression. The
 * final states of the trie are embedded in leaves or "chunks", where each
 * chunk is a 64-bit integer. Each bit position of the integer corresponds to
 * whether a particular codepoint is in the set or not. These chunks are not
 * just a compact representation of the final states of the trie, but are also
 * a form of suffix compression. In particular, if multiple ranges of 64
 * contiguous codepoints have the same Unicode properties, then they all map to
 * the same chunk in the final level of the trie.
 *
 * Being tailored for Unicode codepoints, this trie is partitioned into three
 * disjoint sets: tree1, tree2, tree3. The first set corresponds to codepoints
 * [0, 0x800), the second [0x800, 0x10000) and the third [0x10000,
 * 0x110000). These partitions conveniently correspond to the space of 1 or 2
 * byte UTF-8 encoded codepoints, 3 byte UTF-8 encoded codepoints and 4 byte
 * UTF-8 encoded codepoints, respectively.
 *
 * Lookups in this data structure are significantly more efficient than binary
 * search. A lookup touches either 1, 2, or 3 cache lines based on which of
 * the trie partitions is being accessed.
 *
 * One possible performance improvement would be for `ucd-trie` to
 * expose a way to query based on a UTF-8 encoded string, returning the
 * Unicode property corresponding to the first character in the string. Without
 * such an API, the caller is required to tokenize their UTF-8 encoded input
 * data into [Char], hand the [Char] into `ucd-trie`, only for `ucd-trie` to
 * undo that work by converting back into the variable-length representation for
 * trie traversal.
 *
 * #### fst
 *
 * Uses a [finite state transducer][fst]. This representation is built into
 * [`ucd-generate`] but the upstream author is not aware of any advantage over
 * the `ucd-trie` representation. In particular `ucd-trie` is optimized for
 * storing Unicode properties while `fst` is not.
 *
 * [fst]: https://github.com/BurntSushi/fst
 *
 * The main thing that appears to cause `fst` to have large size and slow
 * lookups for this use case relative to `ucd-trie` is that it does not
 * specialize for the fact that only 21 of the 32 bits in a Rust `char` are
 * meaningful. There are some dense arrays in the structure with large ranges
 * that could never possibly be used.
 *
 * #### roaring
 *
 * The `roaring` crate is a pure-Rust implementation of [Roaring Bitmap], a data
 * structure designed for storing sets of 32-bit unsigned integers.
 *
 * [Roaring Bitmap]: https://roaringbitmap.org/about/
 *
 * Roaring bitmaps are compressed bitmaps which tend to outperform conventional
 * compressed bitmaps such as WAH, EWAH or Concise. In some instances, they can
 * be hundreds of times faster and they often offer significantly better
 * compression.
 *
 * In this use case the performance was reasonably competitive but still
 * substantially slower than the Unicode-optimized implementations. Meanwhile
 * the compression was significantly worse, requiring 6× as much storage for
 * the data structure.
 *
 * The [`croaring`] crate is an FFI wrapper around the C reference
 * implementation of Roaring Bitmap. The upstream benchmark found it consistently
 * about 15% slower than pure-Rust `roaring`, which could just be FFI overhead.
 *
 * [`croaring`]: https://crates.io/crates/croaring
 *
 * #### unicode-ident
 *
 * The `unicode-ident` crate is most similar to the `ucd-trie` library, in that it's
 * based on bitmaps stored in the leaves of a trie representation, achieving
 * both prefix compression and suffix compression.
 *
 * The key differences are:
 *
 * - Uses a single 2-level trie, rather than 3 disjoint partitions of different
 *   depth each.
 * - Uses significantly larger chunks: 512 bits rather than 64 bits.
 * - Compresses the XID_Start and XID_Continue properties together
 *   simultaneously, rather than duplicating identical trie leaf chunks across
 *   the two.
 *
 * The following diagram show the XID_Start and XID_Continue Unicode boolean
 * properties in uncompressed form, in row-major order:
 *
 * <table>
 * <tr><th>XID_Start</th><th>XID_Continue</th></tr>
 * <tr>
 * <td><img alt="XID_Start bitmap" width="256" src="https://user-images.githubusercontent.com/1940490/168647353-c6eeb922-afec-49b2-9ef5-c03e9d1e0760.png"></td>
 * <td><img alt="XID_Continue bitmap" width="256" src="https://user-images.githubusercontent.com/1940490/168647367-f447cca7-2362-4d7d-8cd7-d21c011d329b.png"></td>
 * </tr>
 * </table>
 *
 * Uncompressed, these would take 140 K to store, which is beyond what would be
 * reasonable. However, as you can see there is a large degree of similarity
 * between the two bitmaps and across the rows, which lends well to
 * compression.
 *
 * `unicode-ident` stores one 512-bit "row" of the above bitmaps in the leaf
 * level of a trie, and a single additional level to index into the leaves.
 * There are 134 unique 512-bit chunks across the two bitmaps.
 *
 * The chunk size of 512 bits is selected as the size that minimizes the total
 * size of the data structure. A smaller chunk, like 256 or 128 bits, would
 * achieve better deduplication but require a larger index. A larger chunk
 * would increase redundancy in the leaf bitmaps. 512 bit chunks are the
 * optimum for total size of the index plus leaf bitmaps.
 *
 * The chunk data is compressed using the Kuhn–Munkres algorithm for bipartite
 * matching to eliminate redundancies between the second half of any chunk and
 * the first half of any other chunk. This achieves an additional 9%
 * compression of the leaf level, leaving 122 chunks that can be indexed at the
 * half-chunk level using an 8-bit index. Note that this is not the same as
 * using chunks which are half the size, because it does not necessitate raising
 * the size of the trie's first level.
 *
 * In contrast to binary search or the `ucd-trie` crate, performing
 * lookups in this data structure is straight-line code with no need for
 * branching.
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
