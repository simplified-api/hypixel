# Contributing to Hypixel API

Thank you for your interest in contributing! This document explains how to get started, what to expect during the review process, and the conventions this project follows.

## Table of Contents

- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Development Setup](#development-setup)
  - [IntelliJ IDEA](#intellij-idea)
- [Making Changes](#making-changes)
  - [Branching Strategy](#branching-strategy)
  - [Code Style](#code-style)
  - [Commit Messages](#commit-messages)
  - [Validating Output](#validating-output)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Reporting Issues](#reporting-issues)
- [Project Architecture](#project-architecture)
- [Legal](#legal)

## Getting Started

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| JDK | **21+** | Required |
| Gradle | 8.x | Wrapper is bundled (`./gradlew`) |
| Git | 2.x+ | For cloning and contributing |
| Python | 3.x | For `scripts/json_dto_diff.py` |
| Hypixel API key | - | To verify a change against the live API |
| IDE | Any | IntelliJ IDEA is the recommended editor |

Both test fixtures are in the repository, so a fresh clone runs the whole suite with no setup beyond the JDK.

### Development Setup

1. **Fork and clone the repository**

   [Fork the repository](https://github.com/simplified-api/hypixel/fork), then clone your fork:

   ```bash
   git clone https://github.com/<your-username>/hypixel.git
   cd hypixel
   ```

2. **Verify the JDK toolchain**

   Gradle's Java toolchain feature will download JDK 21 automatically if needed. Confirm with:

   ```bash
   ./gradlew --version
   ```

3. **Run the build**

   ```bash
   ./gradlew build
   ```

   `MemberDtoMappingTest` reads `src/test/resources/craftedfury.json` and takes two members out of it - the first member of `profiles[0]` as the sparse case and the first of `profiles[1]` as the populated one. Many assertions are pinned to exact counts from that capture (792 objectives, 810 Jacob's contests, a `CATACOMBS` weight of `200.0 / 2.03`). Refreshing the capture moves them by design; each is a value captured from behaviour before a change, so they are updated deliberately and never loosened.

4. **Build against local siblings (optional)**

   Every upstream dependency is `strictly()`-pinned to a JitPack SHA in `build.gradle.kts`. To test against unpublished sibling changes - most often the `skyblock` data models - build from the `Simplified-Api` parent, whose `settings.gradle.kts` substitutes those coordinates for local sources.

### IntelliJ IDEA

1. Open the project root (the directory containing `settings.gradle.kts`). IntelliJ auto-imports the Gradle build.
2. Ensure the **Project SDK** under **File > Project Structure** is set to a JDK 21 installation.
3. Enable **annotation processing** - Lombok generates nearly every accessor in the response tree, and the IDE reports phantom errors until the processor runs.
4. The build script excludes `.schema/` from the IDE module, so the generated JPA schema does not get indexed.
5. Open the `Simplified-Api` parent instead when you need the `skyblock` sibling's unpublished models on the classpath.

## Making Changes

### Branching Strategy

- Create a feature branch from `master` for your work.
- Use a descriptive branch name: `fix/jacobs-contest-collection-id`, `feat/foraging-whispers`, `docs/weight-formula`.

```bash
git checkout -b feat/my-feature master
```

### Code Style

The repository uses Lombok for boilerplate reduction and enforces a consistent Javadoc, exception, and control-flow style.

#### Javadoc

- **Punctuation** - Single hyphens ` - ` only as separators. Never em dashes, `&mdash;`, or `--`.
- **Voice** - Class/interface = noun phrase. Method = third-person singular verb ("Returns the..."). Field = sentence fragment, no tags.
- **Tags** - Always include `@param`, `@return`, `@throws` where applicable. Lowercase sentence fragments, no trailing period. Single space after the parameter name - never column-align.
- **Cross-references** - Use `{@link}` / `{@linkplain}` / `@see`. Use `{@code}` for inline code. Import link targets so they render with short names.
- **Overrides** - Use `/** {@inheritDoc} */` for methods that override library/framework types. Do not rewrite the parent doc.
- **Field getters** - Field-like interface methods (no params, non-void return) use a noun-phrase fragment without `@return` and without "Gets"/"Returns". Lombok `@Getter` implementations carry their doc on the field, not a separate method Javadoc block.
- **Structure** - `<p>` on its own line between paragraphs; `<ul>` / `<li>` for lists; `<b>` for emphasis inside list items.
- **Forbidden tags** - Never use `@author` or `@since`.
- **Document the wire, not the field** - a DTO field whose name already says what it holds needs no doc. A field whose *binding* is surprising - a path, a capture, a split, an id that carries colons - needs one, and it should state what the wire does rather than what the field is.

#### Control flow

Omit braces on single-line bodies; use braces when the body wraps across multiple lines. Applies to all single-statement forms (`if`, `for`, `while`, `do`, lambda bodies).

```java
if (split < 0) continue;

for (String entry : entries) {
    Integer tier = NumberUtil.tryParseInt(entry.substring(split + 1));
    if (tier == null)
        continue;
    grouped.add(tier);
}
```

#### Collections

Use `getFirst()` / `getLast()` for sequenced access - never `get(0)` or `get(size() - 1)`. This excludes non-`SequencedCollection` types such as Gson's `JsonArray`.

#### Exception classes

Project exceptions follow a **five-constructor pattern** in this order:

1. `(Throwable cause)`
2. `(String message)`
3. `(Throwable cause, String message)`
4. `(@PrintFormat String message, Object... args)`
5. `(Throwable cause, @PrintFormat String message, Object... args)`

Root exceptions (extending `RuntimeException`) reverse the `super()` parameter order:

```java
super(message, cause);
super(String.format(message, args), cause);
```

Child exceptions pass through to the parent, which handles the reversal:

```java
super(cause, message);
super(cause, message, args);
```

Message conventions:

- No trailing punctuation.
- Start with an uppercase letter.
- Use `'%s'` for interpolated values in format strings.

Annotations:

- `@NotNull` on `Throwable cause` and `String message` parameters.
- `@PrintFormat` on format string parameters (from `org.intellij.lang.annotations`).
- `@Nullable` on `Object... args` parameters.

Javadoc:

- **Class-level** - "Thrown when [condition]." Never use the words "unchecked" or "exception" in the description.
- **Constructor** - "Constructs a new {@code ClassName} with [description]."
- **`@param` tags** - lowercase, no trailing period.

> [!NOTE]
> `HypixelApiException` is not one of these. It is built by the framework's error decoder, so its single `(Gson, ErrorContext)` constructor is fixed by the `ClientConfig.withErrorDecoder` method-reference shape.

#### DTOs

- Prefer an annotation over a hand-written adapter. `@SerializedPath`, `@Capture`, `@Extract`, `@Collapse`, `@Flatten`, `@Split` and `@Lenient` cover nearly every awkward shape Hypixel sends, and unlike a custom adapter they round-trip back to the wire layout for free.
- Give every collection field a non-null default (`Concurrent.newList()` / `Concurrent.newMap()`). A member with the subtree hidden by an API setting must read as empty, never as null.
- Wrap a genuinely optional scalar in `Optional`, not `@Nullable`. The absence is meaningful and the caller has to face it.
- Every enum bound from the wire needs a fallback constant, spelled the way the wire spells its members. `EnumLookup.of(values(), name, FALLBACK)` is the shared case-insensitive lookup.
- Do not mark a fallback constant `@Fallback` unless its name appears nowhere in the wire vocabulary - a marked constant the wire can name collapses real values onto the sentinel, and in a map-key position it silently overwrites a correct entry. `noMarkedConstantIsWireVisible` enforces this.
- Derive nothing at bind time. A value that needs another field, a parse, or a repository belongs in an accessor that computes it on first call.

### Commit Messages

Write clear, concise commit messages that describe *what* changed and *why*.

```
Share one weight formula between the dungeon progressions

DungeonData and DungeonClass carried byte-identical copies of the
formula. SkyBlockMember.getTotalWeight() sums both, so a divergence
between them would have cancelled out there rather than showing up.
```

- Use the imperative mood ("Add", "Fix", "Update", not "Added", "Fixes").
- Keep the subject line under 72 characters.
- Add a body when the *why* isn't obvious from the subject.
- Dependency bumps use the `build(deps):` prefix and name the artifact and the new SHA.

### Validating Output

- **Test suite**

  ```bash
  ./gradlew test
  ```

- **Coverage audit** - required when your change touches the response tree. Every Hypixel update adds keys, and Gson drops what nothing declares without a word:

  ```bash
  python scripts/json_dto_diff.py
  python scripts/json_dto_diff.py --section dungeons
  ```

  It exits non-zero when unmapped keys remain, so it doubles as a gate.

- **Round-trip proof** - required when your change adds or moves a binding annotation. Decode the subtree, serialize it back, and assert the output keys equal the input keys. Read the expectation from a `deepCopy()` taken **before** the decode: a `@Lenient` decode rewrites the caller's own tree in place, so comparing two views of the same live object lets a loss cancel itself out.

- **Live verification** - the fixture is one account at one moment. When your change touches an endpoint, a request line, or a shape you have not seen in the fixture, call the real API once and say so in the PR.

> [!TIP]
> When a test's expected value comes from the fixture, read it out of the fixture rather than hardcoding it - `raw.getAsJsonObject("kills").size() - 1` survives a fixture swap where `4127` does not. Hardcode only a value captured from the *old behaviour* you are changing, and say so in a comment.

## Submitting a Pull Request

1. **Push your branch** to your fork.

   ```bash
   git push origin feat/my-feature
   ```

2. **Open a Pull Request** against the `master` branch of [simplified-api/hypixel](https://github.com/simplified-api/hypixel).

3. **In the PR description**, include:
   - A summary of the changes and the motivation behind them.
   - The `json_dto_diff.py` output before and after, if the response tree moved.
   - Whether you verified against the live API, and against which endpoint.
   - Any fixture-pinned assertion you had to change, and why the new value is right.

4. **Respond to review feedback.** PRs may go through one or more rounds of review before being merged.

### What gets reviewed

- **Nothing is lost.** A binding change is judged by what the round trip reproduces. An entry that decodes but does not serialize back is a regression even when no test names it.
- **Nothing is invented.** A union type that gives every NPC a field only one of them sends, or a default that fabricates a timestamp, is worse than an absent value.
- **Binding stays free of derivation.** A decode that reads a repository, parses a display name, or computes a level blocks a merge - it makes the DTO tree unusable without a database session and moves failures into a swallowing catch.
- **Fallback constants are unreachable from the wire.** See the `@Fallback` rule above.
- **Javadoc and exception style** as documented above. Inconsistent style will be flagged.

## Reporting Issues

Use [GitHub Issues](https://github.com/simplified-api/hypixel/issues) to report bugs or request features.

When reporting a bug, include:

- **JDK version** (`java -version`)
- **Operating system**
- **The endpoint and the DTO** involved
- **The raw JSON subtree** that reproduces it, trimmed to the smallest failing shape
- **Expected vs. actual bound value**
- **Full stack trace** (if applicable)
- **Whether the value is bound or derived** - a wrong number out of `getLevel()` and a wrong number out of the wire are different bugs

> [!CAUTION]
> Never paste an API key into an issue, a test fixture, or a commit. Trim captured responses to the failing subtree - a full profiles response carries another player's entire inventory.

## Project Architecture

A brief overview to help you find your way around the codebase:

```
api.simplified.hypixel/
├── HypixelContract.java           # api.hypixel.net/v2 - JSON
├── HypixelForumContract.java      # hypixel.net - RSS 2.0
├── HypixelApiGsonContributor.java # SPI hook; registers the NBT adapter with GsonSettings.defaults()
├── common/                        # Experience, Weight, Weighted, WeightedGroup, NbtContent,
│                                  #   IdTiers, EnumLookup - the shared vocabulary
├── exception/                     # HypixelApiException + the decoded HypixelErrorResponse
├── profile_stats/                 # ProfileStats: the stat sheet folded out of a member
└── response/
    ├── forum/                     # RSS feed items
    ├── hypixel/                   # player, guild, counts, status, punishments
    ├── resource/                  # /resources/* definition documents
    └── skyblock/                  # profiles, islands, auctions, bazaar, fire sales
        └── member/                # the member document - one package per subtree
```

### Decode flow

```
HypixelContract method
  -> Feign proxy, framework decoder
  -> GsonSettings.defaults()          # SPI: HypixelApiGsonContributor + ConcurrentTypeAdapterFactory
  -> gson-extras factories            # @SerializedPath / @Capture / @Extract / @Collapse / @Lenient
  -> SkyBlockProfiles -> SkyBlockIsland -> SkyBlockMember
                                       # bound values only; nothing derived, no session opened
  -> accessor                          # levels, weights, detected accessories, families
  -> SkyBlockData repositories         # only from the accessors that resolve ids
```

### Where the value comes from

Three layers, and telling them apart is most of debugging this module:

| Layer | Produced by | Fails as |
|-------|-------------|----------|
| **Bound** | a field plus its annotations | a default value, silently |
| **Derived** | an accessor on the DTO | a wrong number, or a swallowed parse |
| **Resolved** | a SkyBlock data repository | `JpaException` without a session |

## Legal

By submitting a pull request, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE.md), the same license that covers this project.

**Do not commit credentials, and do not commit anyone else's capture.** An API key must never enter the repository. The two fixtures under `src/test/resources/` are the maintainer's own account and a public resource document, both published deliberately; a profiles response for any other account contains that player's full inventory, which is theirs and not yours to publish.

Hypixel and SkyBlock are properties of Hypixel Inc. This library is an independent client for their public API and is not affiliated with, endorsed by, or sponsored by Hypixel Inc. or Mojang AB. Contributions must comply with the [Hypixel Public API terms](https://api.hypixel.net/).
