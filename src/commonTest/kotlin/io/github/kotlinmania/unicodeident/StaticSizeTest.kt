// port-lint: tests tests/static_size.rs
package io.github.kotlinmania.unicodeident

// static_size.rs asserts mem::size_of_val on ASCII_START, ASCII_CONTINUE,
// TRIE_START, TRIE_CONTINUE, and LEAF, plus the ucd-trie TrieSet fields, the
// fst binary blobs, and the roaring bitmap serialized sizes. Kotlin/KMP has
// no equivalent layout-size guarantee — object layout is JVM/JS/Native
// specific and not under the program's control — so these assertions do not
// port.
internal object StaticSizeTestPlaceholder
