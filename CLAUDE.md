# Claude Code Project Instructions

## Project Overview

This is **unicode-ident-kotlin**, a Kotlin Multiplatform port of [dtolnay/unicode-ident](https://github.com/dtolnay/unicode-ident). The upstream crate determines whether characters have the `XID_Start` or `XID_Continue` properties according to Unicode Standard Annex #31. The Rust sources are in `tmp/unicode-ident/` (read-only translation oracle); the Kotlin implementation lives under `src/commonMain/kotlin/io/github/kotlinmania/unicodeident/`.

## Translator's mindset

This is a translation project, not a software-engineering project. While porting a file, you are
the Kotlin author of the same document a Rust author wrote. Architecture, optimization, design
critique, drift measurement — all later. While translating, the only job is the translation.

The discipline:

1. **Read the whole upstream file before you type.** A line-by-line port composes only when you
   know how the file ends. If the file is too long to read in one sitting, split your turn into
   "read the file" and "write the file" — never start typing on a file you've only half-read.

2. **One Rust file → one Kotlin file. Always.** No splitting one `.rs` across several `.kt`. No
   merging several `.rs` into one `.kt`. The 1:1 mapping is the contract; everything downstream
   (ast_distance, port-lint headers, code review) assumes it. If a `.rs` is genuinely too big for
   one Kotlin file, that's a sign you're in `mod.rs`-equivalent territory and the upstream itself
   is a re-export — verify, don't split.

3. **Translate top to bottom in upstream order.** Preserve the declaration order. Don't reorder
   for "logical flow" — the upstream's order *is* the logical flow. The reader who already knows
   the Rust file should be able to scroll the Kotlin file and find every item in the same place.

4. **Comments are content.** License header, module-level doc, every `///` block, every inline
   `//` note, every upstream `// TODO`/`// FIXME` — all translate. Rust syntax inside doc comments
   gets rewritten to Kotlin equivalents (`Vec<T>` → `List<T>`, `Self::foo()` → `foo()`, lifetimes
   dropped, `cfg(test)` and `#[derive(...)]` lifted into prose). You are translating a *document*,
   not just the code.

5. **When a Rust idiom has no Kotlin analog, apply the mapping rule and move on.** `Box<T>`,
   `Arc<T>`, `Cell<T>`, `RefCell<T>`, `Rc<T>`, lifetimes, `PhantomData`, `mem::forget`,
   `drop_in_place`, `Pin`, `MaybeUninit`, `dyn Trait` — all collapse per the mapping table.
   Don't relitigate. A proc-macro becomes a builder/runtime API, not nothing. An upstream Rust
   crate with no KMP equivalent becomes a *separate Kotlin port*, not a `// TODO` placeholder.
   Pay the snowball cost upfront — the next consumer will thank you.

6. **Don't measure mid-port.** ast_distance, FnSim, similarity reports — useful *after* a file is
   done, useless *during*. Mid-translation measurement is procrastination dressed as rigor. Run
   the tools when a file lands or when a port phase wraps, not while you're choosing between
   `Result<T>` and `T?`.

7. **Don't optimize the translation.** "This Kotlin shape would be simpler" is the wrong
   thought. The upstream shape is the spec. If a faithful translation produces a function that
   takes a parameter you'd never write in Kotlin from scratch, take it. Optimization is a
   separate, named pass after parity is reached — never blended into the translation.

8. **Don't re-architect mid-port.** "This whole module would be cleaner if..." — write the
   thought on a sticky note, throw the sticky note away, finish the file. The current architecture
   is the upstream's architecture. Earn the right to redesign by first reaching parity.

9. **Compile errors during translation are normal and expected.** A bottom-of-tree file compiles
   when its deps are ported, not before. Don't pause to "make it compile" mid-port — that pulls
   you into stub-shaped fixes that you'll have to undo. Climb the dep tree bottom-up; the leaves
   compile first, then their parents, then everything compiles together at the end.

10. **Bottom-up always.** Port dependencies before consumers. If `state.rs` uses `EvalException`,
    port `eval_exception.rs` first. If `eval_exception.rs` uses `Error`/`WithDiagnostic`/`CallStack`,
    port those first. The order isn't optional; trying to port top-down produces a tree of stubs
    that all need replacing.

11. **Hard files are not skippable.** logos-codegen, lalrpop's table generator, an annotate-snippets
    equivalent — when you hit one, port it. Skipping leaves a `// TODO`-shaped hole that grows
    every time another consumer needs it. The snowball is the whole point: each hard port done
    makes the next port easier, because the dep is now in Kotlin.

12. **Warnings are real, but `@Suppress` is never the answer.** `UNUSED_PARAMETER` on a callback
    helper means the function shape doesn't fit Kotlin — restructure the signature, don't suppress.
    `UNCHECKED_CAST` means the type system is missing an invariant — encode it. Every warning is
    either a real bug or a translation choice that needs revisiting; treat them as compile errors.

13. **Stop at file boundaries, not function boundaries.** After every completed file, exhale,
    commit, move on. Don't pause mid-function to second-guess a choice. The whole-file context
    is what makes individual choices coherent.

14. **Doc-port discipline applies even when the upstream doc is awkward.** If the upstream
    author wrote a tortured English sentence in a doc comment, translate the tortured sentence.
    Don't smooth it. Don't paraphrase. Their doc is the contract for the Kotlin doc.

15. **The cheat detector is your friend.** If `ast_distance` forces your file's score to 0
    because you left snake_case identifiers or `pub` keywords in Kotlin comments, take it as a
    literal instruction: rewrite those comments to be Kotlin-native. Rust syntax in Kotlin source
    — code or comments — is the cheat we're catching.

The sticky-note version: **"Read the file. Translate it. Don't think about anything else."**

## Critical Workflows

### 0. No Subagents

**Do not delegate translation work to subagents (Agent / Task tool).**

Subagents cheat with abandon because they don't feel the user's reaction when
they cut corners. They strip substantive content along with porting prose,
add Kotlin-only filler functions to inflate similarity scores, hollow out
KDoc when "translating" it, and lose track of which declarations have Rust
counterparts — and the only signal back is a tidy summary that masks the
damage.

Translation must happen in the main conversation loop where Sydney can see
each edit and correct course immediately. If the volume feels overwhelming,
slow down — don't parallelize.

Use Bash, Read, Edit, and Write directly. Never call the Agent / Task tool
for porting, doc translation, or any work that touches `.kt` files in this
repo.

### 1. No Swarm/Task System

This repo is **not** using the swarm/task-assignment workflow (`tasks.json`). Do not create or depend on it.

Use file comparisons and directory-level checks instead:

```bash
# Deep comparison over directories
./tools/ast_distance/ast_distance --deep tmp/unicode-ident/src rust src/commonMain/kotlin/io/github/kotlinmania/starlark kotlin

# Rank missing/priority (if available in your ast_distance build)
./tools/ast_distance/ast_distance --rank tmp/unicode-ident/src rust src/commonMain/kotlin/io/github/kotlinmania/starlark kotlin
```

### 2. Port-Lint Headers (REQUIRED)

Every Kotlin file MUST start with:

```kotlin
// port-lint: source <path-relative-to-tmp/unicode-ident>
package io.github.kotlinmania.starlark.module
```

Example:
```kotlin
// port-lint: source src/values/layout.rs
package io.github.kotlinmania.starlark.values
```

This is how `ast_distance` tracks provenance — which Rust file each Kotlin file was translated from. Without this header, the file is invisible to all port analysis tooling. Never remove, move, or alter the header unless the file is being re-targeted to a different Rust source.

### 3. Quality Verification

After porting a file, verify with:

```bash
./tools/ast_distance/ast_distance \
  tmp/unicode-ident/src/values/layout.rs rust \
  src/commonMain/kotlin/io/github/kotlinmania/starlark/values/Layout.kt kotlin
```

**Target: Similarity ≥ 0.85** (excellent port)

## Build Commands

```bash
# Full build
./gradlew build

# Run tests
./gradlew test

# Specific platform
./gradlew jvmTest
./gradlew macosArm64Test
```

## Porting Guidelines

See [AGENTS.md](./AGENTS.md) for complete porting patterns.

**Key principles:**
1. **Semantic parity** - Port behavior, not just syntax
2. **Research first** - Don't guess at Rust semantics
3. **Line-by-line** - Maintain file structure
4. **Documentation** - Translate all doc comments to KDoc
5. **No oversimplification** - Replicate formatting logic, ANSI handling, etc.

## STRICT RULES — Translation, Not Engineering

### This is a translation project.

Every Kotlin file is a line-by-line port of a Rust source file in `tmp/unicode-ident/src/`. The `// port-lint: source` header at the top of each `.kt` file tells you which Rust file it came from. That header is how `ast_distance` tracks provenance — never remove or change it.

**When you encounter a compile error, the fix is ALWAYS in the Rust source.** Do not invent solutions to make the Kotlin compiler happy. Do not make classes extend Exception because it "seems right." Do not change visibility, delete code, or add shims. Read the corresponding Rust file and translate faithfully.

### No code stubs. Period.

Do not write stub files, placeholder classes, empty implementations, or skeleton code. Every line of Kotlin must be a faithful translation of the corresponding Rust source. If you can't fully translate a file, don't create it at all — a missing file is better than a stub that will conflict with the real implementation later.

This means:
- **No `class Foo` with an empty body** when the Rust struct has fields and methods
- **No `fun bar() = TODO()`** or `fun bar() { error("not implemented") }`
- **No partial ports** that translate the class declaration but skip the methods
- **No "placeholder until the evaluator is ready"** comments with commented-out code

If a dependency doesn't exist yet, port that dependency first. If the dependency chain is too deep, pick a different file to work on.

### Use ast_distance for all analysis.

The `tools/ast_distance/ast_distance` tool is the single source of truth for:
- `--import-map`: Finding unresolved types, duplicate definitions, and ambiguous imports
- `--symbols-duplicates`: Finding duplicate symbol definitions across files
- `--compiler-fixup`: Suggesting import fixes from gradle error output
- `--symbol-parity`: Comparing Rust vs Kotlin symbol coverage
- `--deep`: Full cross-language AST comparison report

### Do NOT pipe, redirect, or wrap ast_distance output.

The tool detects and rejects stdout piping (`|`), redirection (`>`), and wrappers like `script -q`. Run it directly in the terminal. Read its output directly from the tool result.

### Do NOT create typealias re-export files.

Root-package `.kt` files that re-export types from subpackages via `typealias` cause massive ambiguity errors across the codebase. Types like `Ty`, `Value`, `Freezer` must be imported from their actual defining package, not from a convenience re-export.

### Do NOT create stub/placeholder types.

If a type doesn't exist yet, port the file that defines it. Don't create placeholder classes like `class Ty` or `class CodeMap` in random files — they conflict with real implementations when those files get ported.

### Wrapper class policy

Faithful ports use Kotlin idioms for Kotlin types and only wrap when Rust itself wraps. Specifically:

- **If Rust uses a stdlib type directly** (`HashMap`, `Vec`, `BTreeSet`), Kotlin uses the Kotlin stdlib equivalent directly. Don't write a wrapper class to host Rust-named methods (`len()`, `iter()`, `insert()`) — those add no domain logic and exist only to inflate ast_distance scores. Use Kotlin's `size`, `iterator()`, `add()`.

- **If Rust wraps a stdlib type in a domain struct** (`pub struct StringValueInterner { map: HashTable<...> }` with real methods like `intern()`), Kotlin wraps the same way. The class is the port; the underlying storage is plain Kotlin collections.

- **Rust language primitives without Kotlin equivalents** (`Box<T>`, `Cell<T>`, `RefCell<T>`, `Arc<T>`, `Rc<T>`, `NonNull<T>`, `MaybeUninit<T>`, `dyn Trait`) are not ported as classes. They get inlined or replaced with the closest Kotlin idiom (plain reference, `var`, atomic ref where threaded). Kotlin-side `traceBox` / `traceCell` / `traceRefCell` / `traceUnsafeCell` style functions to mirror Rust's `impl Trace for Box<T>` etc. are shims, not port — those Rust impls have no Kotlin counterpart, so the corresponding Kotlin functions shouldn't exist.

- **Rust `pub type X = Y`** becomes a Kotlin `typealias X = Y` only when the Rust source actually contains a `pub type` declaration with that name. ast_distance reads both files and credits the pairing automatically (canonical name match). A Kotlin typealias with no matching Rust `pub type` is reported as a Kotlin-only invention -- there is no annotation that bypasses this. Don't add typealiases for ergonomics ("typealiasing creates a spiderweb of impossible debugging" -- when something breaks at `Set<K>`, callers can't see whether they're hitting Kotlin stdlib `Set`, `BTreeSet`, or some third thing the alias points at).

- **Iterator types**: classes implementing `kotlin.collections.Iterator` are the faithful port of `impl Iterator for X` in Rust. The forced `hasNext()` is the documented Kotlin idiom collision; don't add Rust-named `nextBack()`/`len()`/`map()` shim methods alongside unless Rust actually has the corresponding struct. Pure forwarding shims like `class Keys(val inner: Iter): Iterator<K>` are rustification, not port.

- **ast_distance score is a signal, not a target.** If a faithful port scores poorly, fix ast_distance — don't bend the code. If you find yourself adding Rust-named methods to a Kotlin built-in wrapper to push the score, you're rustifying.

## Progress Tracking

```bash
# Overall progress
./tools/ast_distance/ast_distance --deep tmp/unicode-ident rust src kotlin
```

## Naming Conventions

- **Files:** PascalCase (e.g., `module.rs` → `Module.kt`)
- **Packages:** Mirror Rust crate structure
- **Functions/Variables:** camelCase (Rust snake_case → Kotlin camelCase)
- **Types:** PascalCase
- **Constants:** UPPER_SNAKE_CASE

## Common Patterns

### Rust → Kotlin Mappings

- `Result<T, E>` → `Result<T>` with exceptions
- `Option<T>` → `T?` (nullable types)
- `Vec<T>` → `MutableList<T>` or `List<T>`
- `HashMap<K, V>` → `MutableMap<K, V>` or `Map<K, V>`
- `Arc<T>` → Appropriate Kotlin concurrency primitives
- Trait → Interface
- Enum with data → Sealed class
- Struct → Data class

### Error Handling

Preserve error messages and context. Use Kotlin's `Result` type or throw appropriate exceptions.

### Trait default methods with `where` clauses

Rust trait default methods gated by `where T: SomeBound` translate to
Kotlin **extension functions whose own generic type parameter carries
the bound** — never tighten the interface, never add a runtime
`is Comparable<*>` cast, never make the method abstract just to dodge
the issue. Concrete subtypes specialise by declaring a same-named
member (no `override` keyword); Kotlin resolves to the member for the
concrete static receiver type and to the extension for the interface
type, exactly mirroring Rust's per-impl override of a trait default.
When the bound lives on a *class* parameter rather than a trait method,
fall back to the `Comparator<in K>` field pattern with a
comparator-or-natural dispatch helper. See
[AGENTS.md](./AGENTS.md) §"Trait default methods with `where` clauses"
for the worked recipe and rationale.

## File Organization

```
src/
├── commonMain/kotlin/io/github/kotlinmania/starlark/
│   ├── values/      # Port of tmp/unicode-ident/src/values/
│   ├── eval/        # Port of tmp/unicode-ident/src/eval/
│   ├── typing/      # Port of tmp/unicode-ident/src/typing/
│   └── ...
├── jvmMain/
├── nativeMain/
└── commonTest/
```

## Testing

- Port all Rust tests
- Use `kotlin.test` for multiplatform compatibility
- Maintain test structure and coverage

## Dependencies

Minimal approach:
- kotlinx-coroutines-core
- kotlinx-serialization
- kotlinx-collections-immutable
- kotlinx-datetime

Add new dependencies only when necessary.

## Documentation References

- [AGENTS.md](./AGENTS.md) - Detailed porting patterns
- [PORTING.md](./PORTING.md) - Quick reference workflow
- [README.md](./README.md) - Project overview
- [Starlark Spec](https://github.com/bazelbuild/starlark/blob/master/spec.md)
- [Starlark Rust Docs](https://docs.rs/starlark/)

## Commit Messages

Follow Sydney's style:
- No AI branding or attribution
- Clear, descriptive messages focused on what changed and why
- No "Co-Authored-By" lines
- No emoji or robot references

Example:
```
Add layout module port from Rust

Port layout.rs to Layout.kt with semantic parity. Includes:
- Value allocation and freezing logic
- Heap management structures
- AST similarity: 0.89
```

## When to Ask

Ask the user for clarification if:
- Rust semantics are unclear and documentation doesn't help
- Architectural decisions affect multiple files
- Build configuration needs changes
- You encounter blocking issues
- **You want to add a TODO comment** - get user approval first

## TODO Policy

**DO NOT add TODO comments without explicit user approval.**

If you encounter something that cannot be fully implemented:
1. Ask the user if a TODO is appropriate
2. If approved, use the format: `// TODO: <description>`
3. Better: Ask the user how they want to handle the incomplete functionality
4. Best: Complete the implementation or find an alternative approach

Avoid TODOs by:
- Researching Rust documentation thoroughly
- Looking at similar patterns in the codebase
- Asking the user for guidance on complex Rust idioms
- Using placeholder implementations only when explicitly approved

## Priority Files (Top 10)

1. `layout.value` (160 deps)
2. `typing.ty` (107 deps)
3. `layout.heap` (100 deps)
4. `params.display` (68 deps)
5. `typing.starlark_value` (67 deps)
6. `debug` (53 deps)
7. `assert` (53 deps)
8. `runtime.evaluator` (46 deps)
9. `values.trace` (46 deps)
10. `values.alloc_value` (38 deps)

Start with leaf nodes (low dependency count) and work up to high-dependency modules.

## Re-exports from upstream `mod.rs` files

When an upstream Rust `mod.rs` is **only re-exporting** something that actually lives elsewhere
(`pub use <crate-path>::<Name>;`, often under a different name), do **not** preserve that
re-export shape in Kotlin as a "central alias" API. Do not write a `typealias` for the
re-exported name. The existing `Forbidden` rule against "Re-export typealias files at root
packages" is enforced through this procedure.

Workflow:

1. **Identify what the `mod.rs` is re-exporting and the name it's exported as.** Record both
   the original symbol's fully-qualified upstream path and the (possibly different) re-export
   name.

2. **Find callers across the kotlinmania monorepo.** A caller is any Kotlin file in another
   `*-kotlin` repo that has both a `tmp/` folder and a Cargo.toml depending on the Rust
   counterpart of *this* crate, where the file references the re-exported name. Search for:
   - direct imports: `import <reexport-package>.<Name>`
   - wildcard imports of the re-export package, when `<Name>` is used in the file body
   - fully-qualified inline references

3. **Rewrite each caller to reference the upstream/original symbol directly.** If the caller
   still needs to write `<Name>` unchanged, use Kotlin aliasing:
   `import <upstream-fully-qualified-name> as <Name>`. Never bridge with a Kotlin `typealias`.

4. **Keep `Mod.kt` (or the equivalent file for that package) as a tracking file.** It carries
   the translated upstream module-level comments and a literal-quoted reference to each upstream
   `pub use` line (e.g. `// pub use crate::lib::result::Result;`). Each time a caller is migrated
   off the re-export, append the caller's absolute path under a `// Callers migrated:` ledger in
   `Mod.kt`. Append, never delete. Once all callers are migrated, the `typealias` (if any) is
   removed; the tracking file remains as the ledger of the migration.

Reference example: [/Volumes/stuff/Projects/kotlinmania/serde-kotlin/tmp/serde/serde_core/src/private/mod.rs](/Volumes/stuff/Projects/kotlinmania/serde-kotlin/tmp/serde/serde_core/src/private/mod.rs)
re-exports `Result` from `crate::lib::result`. The Kotlin tracking file lives at
[/Volumes/stuff/Projects/kotlinmania/serde-kotlin/src/commonMain/kotlin/io/github/kotlinmania/serde/core/private/Mod.kt](/Volumes/stuff/Projects/kotlinmania/serde-kotlin/src/commonMain/kotlin/io/github/kotlinmania/serde/core/private/Mod.kt).
A caller that previously did `import io.github.kotlinmania.serde.core.private.Result` is
rewritten to `import kotlin.Result as Result` (or just removes the import and relies on the
auto-imported `kotlin.Result`).
