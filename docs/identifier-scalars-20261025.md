# Unicode scalar entry points for identifier validation

The original Rust `src/lib.rs` accepts `char`, which includes supplementary
Unicode scalars. Kotlin's existing `Char` API exposed only UTF-16 code units.
The existing Unicode 17 tables already contain supplementary-plane data.

Added `isXidStartCodePoint(Int)` and `isXidContinueCodePoint(Int)` retain the
original ASCII/trie lookup and make that data accessible. Invalid scalar values
return false; this is an explicit Kotlin boundary policy because Rust `char`
cannot represent them. Existing `Char` entry points delegate to the same lookup
and retain their signatures. No new classification algorithm or tables were added.

The immediate consumer is proc-macro2's existing constructor/tokenizer; its
previous supplementary-character approximation differed from Rust. This is a
bounded dependency repair, not a claim that this whole file was regenerated.

Validation: 12 JVM and 12 macOS ARM64 tests pass, covering existing ASCII/BMP
behavior, supplementary letters, continuation-only digits/variation selectors,
emoji and invalid scalar boundaries. Local publication of
`io.github.kotlinmania:unicode-ident-kotlin:0.1.1` succeeded with the existing
targets and pins. Logs: `tmp/identifier-scalars-20261025/tests.log` and
`publish.log`. Other platform execution and Swift export are unverified.
