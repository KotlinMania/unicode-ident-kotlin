# unicode-ident-kotlin

Kotlin Multiplatform port of [dtolnay/unicode-ident](https://github.com/dtolnay/unicode-ident).

The upstream Rust crate exposes two predicates that determine whether a Unicode
code point belongs to the `XID_Start` or `XID_Continue` property classes
defined in [Unicode Standard Annex #31](https://www.unicode.org/reports/tr31/),
which is the property set Rust (and most other modern languages) use to decide
which characters may begin or continue an identifier.

```kotlin
import io.github.kotlinmania.unicodeident.isXidStart
import io.github.kotlinmania.unicodeident.isXidContinue
```

This port is consumed by [`proc-macro2-kotlin`](https://github.com/KotlinMania/proc-macro2-kotlin)
for identifier validation in the `Fallback` token-parser, mirroring the
`unicode_ident::is_xid_start` / `unicode_ident::is_xid_continue` calls in the
upstream Rust `fallback.rs`.

## Targets

Kotlin Multiplatform: macOS arm64, Linux x64, mingw x64, iOS arm64 / x64 /
simulator-arm64, JS (browser + Node), Wasm-JS (browser + Node), Android, and
JVM.

## Build

```bash
./gradlew build
./gradlew test
```

## Licenses

Upstream `unicode-ident` is `(MIT OR Apache-2.0) AND Unicode-3.0`. The same
combination applies to this port. License texts are mirrored from upstream as
[`LICENSE-APACHE`](./LICENSE-APACHE), [`LICENSE-MIT`](./LICENSE-MIT), and
[`LICENSE-UNICODE`](./LICENSE-UNICODE).

Upstream README is preserved verbatim as [`README-UPSTREAM.md`](./README-UPSTREAM.md).

## Installation

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation("io.github.kotlinmania:unicode-ident-kotlin:0.1.0")
}
```
