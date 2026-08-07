# 04 - Compatibility and blast radius

## 1. What this document is, and what it is not

This is the compatibility and blast-radius audit for the `gson-extras` design cycle. It designs
nothing. It answers one question for each of the five scoped items: **who else pays if this lands,
and what would notice if it went wrong?**

It carries **no design-entry blocks**. The block defined in `10-annotation-designs.md` §3 belongs to
`10-design-entries.md`; §10 here gives a per-scope-item blast-radius verdict instead, keyed by the
scope item rather than by a sibling's slug, because the slugs are being chosen concurrently.

`00-verified-facts.md` is authoritative for library internals and is not re-derived. This document
adds four things that file does not carry:

- the cross-module consumer survey (§2) and the **version-resolution** graph (§3), which is where the
  real risk lives and which no other document in this cycle looks at
- the `@Collapse`, `@Key`, `@Split` and `@SerializedPath` half of the adoption inventory (§4)
- the test-coverage **gap** list (§6.3) - the single most valuable output here
- the verification and JitPack plan (§8, §9)

Baseline to hold, verified at the shas below: gson-extras **134/134**, hypixel **16/16**.
gson-extras `HEAD` is `7cfc181`, pushed and green on JitPack (28 build records, 26 ok, 2 error, 0
in flight). `gradle_verify gson-extras compileJava compileTestJava` exits 0 today.

Two claims in this document are stated adversarially and should be read as warnings, not colour:

1. **The gson-extras suite has no write-side coverage for `@Lenient` or `@Extract` at all** (§6.3).
   Not thin coverage - none. Both overflow stores are, on the read path, write-only (`00-verified-facts.md`
   §6), and on the write path untested. A shared-store design can break round-trip fidelity at all six
   `@Extract` sites and every existing test still passes.
2. **hypixel's `strictly` pin silently upgrades four sibling artifacts onto a gson-extras they were
   never compiled against** (§3.2). Compilation cannot see this. Only a runtime pass can.

## 2. Workspace consumer survey

### 2.1 Every module that references `dev.simplified.gson`

Fourteen modules across the four families declare a `gson-extras` dependency or import its packages
(thirteen consumers plus the library itself). Counts are `.java` files containing the string
`dev.simplified.gson`.

| Module | Files | Declares the dependency at |
| --- | --- | --- |
| `Minecraft-Library/asset-renderer` | 84 | `build.gradle.kts`:1033 |
| `Simplified-Dev/gson-extras` | 38 | itself |
| `Simplified-Api/hypixel` | 29 | `build.gradle.kts`:44 |
| `Simplified-Dev/client` | 10 | `build.gradle.kts`:46 |
| `Simplified-Api/skyblock` | 6 | `build.gradle.kts`:44 |
| `Simplified-Dev/persistence` | 4 | `build.gradle.kts`:24 |
| `SkyBlock-Simplified/data` | 3 | `build.gradle.kts`:63 |
| `Simplified-Api/mojang` | 3 | `build.gradle.kts`:41 |
| `Simplified-Api/github` | 3 | `build.gradle.kts`:39 |
| `SkyBlock-Simplified/server` | 1 | `build.gradle.kts`:44 |
| `SkyBlock-Simplified/bot` | 1 | `build.gradle.kts`:46 |
| `SkyBlock-Simplified/api` | 1 | `build.gradle.kts`:44 |
| `Simplified-Dev/spring-framework` | 1 | `build.gradle.kts`:43 |
| `Simplified-Dev/dataflow` | 1 | `build.gradle.kts`:38 |

`Simplified-Dev/annotations` shows 84 hits, but they are all under `annotations/cache/` - a generated
mirror of other modules' sources, including its own `build.gradle.kts`:91 pin at `37a2c2f`. It is not
a hand-maintained consumer and needs no review, but it **is** a stale pin that a workspace-wide grep
will keep surfacing.

### 2.2 What each consumer actually uses

This is the finding that shrinks the blast radius. Imports, by module:

| Module | Imports from `dev.simplified.gson` | Touched by this cycle? |
| --- | --- | --- |
| `asset-renderer` | `JsonTree` x62, `GsonSettings` x28, `exception.JsonException` x5, `adapter.ColorTypeAdapter` x1, `GsonContributor` x1 | **No.** Zero annotation use. `JsonTree` is a separate surface |
| `client` | `GsonSettings` x10 | **No** |
| `skyblock` | `GsonSettings` x5, `annotation.SerializedPath` x1, `PostInit` x1, `GsonContributor` x1 | **Indirect only** - see §3.2 |
| `persistence` | `GsonSettings` x3, `PostInit` x1 | **Indirect only** |
| `mojang` | `GsonSettings` x3, `annotation.SerializedPath` x1, `GsonContributor` x1 | **No** |
| `github` | `GsonSettings` x3 | **No** |
| `data` / `bot` / `server` / `SkyBlock-Simplified/api` | `GsonSettings` x1-2, `GsonContributor` x1 | **No** |
| `spring-framework` | `GsonSettings` x1 | **No** |
| `dataflow` | `factory.PostInitTypeAdapterFactory`, `factory.CaseInsensitiveEnumTypeAdapterFactory` - **direct factory-class references** in `serde/PipelineGson.java` | **Compile-coupled.** Any signature or visibility change to either class breaks `dataflow` at compile time |
| `hypixel` | `GsonSettings`, all six annotations | **Yes - the only annotation consumer** |

**`hypixel` is the only module in the workspace that uses `@Capture`, `@Lenient`, `@Extract`,
`@Collapse`, `@Split` or `@Key`.** `@SerializedPath` is the only shared annotation, used by `skyblock`
(1 field) and `mojang` (1 field), and nothing in this cycle's scope touches
`SerializedPathTypeAdaptorFactory`.

`dataflow/serde/PipelineGson.java` is the one module that constructs library factories by hand rather
than going through `GsonSettings.defaults()`. It does **not** register `Lenient`, `Capture` or
`Extract`, so it is behaviourally unaffected - but it is the reason a `public` -> package-private
tightening anywhere in `dev.simplified.gson.factory` is not free.

Five modules ship a `GsonContributor` SPI file (`asset-renderer`, `hypixel`, `mojang`, `skyblock`,
`SkyBlock-Simplified/api`), and `Simplified-Dev/collections` ships an SPI
`com.google.gson.TypeAdapterFactory` naming `dev.simplified.collection.gson.ConcurrentTypeAdapterFactory`.
`GsonSettings.defaults()` **appends** both (`:259`, `:261-263`), so per `00-verified-facts.md` §2.1
the `collections` SPI factory sits **outside every library factory** in the live chain. Any ordering
argument this cycle makes is an argument about the library's own eight, not about the whole chain.

### 2.3 The composite build hides the pin - and that is good news

The workspace root `settings.gradle.kts` is a composite over all four families:

```
includeBuild("Simplified-Dev")   includeBuild("Simplified-Api")
includeBuild("Minecraft-Library") includeBuild("SkyBlock-Simplified")
```

and `Simplified-Dev/settings.gradle.kts`:33-37 substitutes
`com.github.simplified-dev:gson-extras` onto the local project. So **a build launched from
`W:/Workspace/Java/Simplified` compiles and tests every consumer against the gson-extras working tree,
with no push and no JitPack cycle.**

This is the single most useful fact in the verification plan (§8). The `00-conventions.md` §4 cost
floor - "any change to gson-extras costs the full cycle" - is true for *publishing*, but it is **not**
true for *verifying*. The blast radius can be measured locally before a single commit is pushed.

Caveat, and it is real: a build launched from inside `Simplified-Api/hypixel` (which is what
`toolsmith verify hypixel` does, and what JitPack does) resolves gson-extras from JitPack at the
pinned sha, **not** from the working tree. The two modes disagree, and §3 is why that matters.



## 3. Version resolution is the real blast radius

### 3.1 The pin table, as written in source

Fourteen pin sites, all `api(...)` or `implementation(...)` with a `strictly` version. They are
**not** converged, despite gson-extras `95cbac2` "Converge the sibling pins onto one sha per artifact".

| Pinned sha | Sites |
| --- | --- |
| `7cfc181` (HEAD) | `Simplified-Api/hypixel`:44 - **alone** |
| `2ba8143` | `asset-renderer`:1033, `github`:39, `mojang`:41, `skyblock`:44, `client`:46, `dataflow`:38, `persistence`:24, `spring-framework`:43, `SkyBlock-Simplified/api`:44, `bot`:46, `data`:63, `server`:44 - **twelve** |
| `37a2c2f` | `annotations/cache/asset-renderer`:91 (generated mirror) |

`2ba8143` is three commits behind `7cfc181`. The intervening three are exactly the recently-shipped
work this cycle must not re-propose: `c944987` (collection value types in `@Capture`/`@Lenient`),
`b071689` (whole-object entries in grouping mode), `7cfc181` (a javadoc trim).

So the twelve siblings are **already** behind by one behaviour-changing pair. Whatever this cycle
adds, the next convergence hands them `c944987` + `b071689` + this cycle's work in one step. That is
not a new cost this cycle creates, but it is a cost this cycle **increases**, and the plan should say
which side of the convergence it wants to land on.

### 3.2 What hypixel's `strictly` actually resolves

Run standalone in `W:/Workspace/Java/Simplified/Simplified-Api/hypixel`:

```
./gradlew -q dependencyInsight --configuration compileClasspath --dependency gson-extras
```

Output, trimmed to the graph:

```
com.github.simplified-dev:gson-extras:7cfc181 (by ancestor)

com.github.simplified-dev:gson-extras:{strictly 7cfc181} -> 7cfc181
\--- compileClasspath

com.github.simplified-dev:gson-extras:b68510e -> 7cfc181
\--- com.github.simplified-api:github:38da22c
     \--- com.github.simplified-api:skyblock:33818f3

com.github.simplified-dev:gson-extras:f42ee07 -> 7cfc181
\--- com.github.simplified-dev:persistence:cacdb62

com.github.simplified-dev:gson-extras:37a2c2f -> 7cfc181
+--- com.github.simplified-api:skyblock:33818f3
\--- com.github.simplified-dev:client:3d87a03
```

Four distinct transitive gson-extras versions - `b68510e`, `f42ee07`, `37a2c2f`, and skyblock's own -
are **all forced up to `7cfc181`** by hypixel's `strictly`. Note these are the shas the *published*
artifacts were built against; they are older than the shas in those modules' current working trees,
because the working tree is ahead of the last JitPack publish.

`37a2c2f` is the commit that moved `annotations` -> `annotation` and promoted `JsonTree` to the root
package. `client:3d87a03` and `skyblock:33818f3` were compiled against that package layout and are
being run against `7cfc181`.

### 3.3 Consequence - binary compatibility is a runtime concern, not a compile concern

`skyblock`, `github`, `persistence` and `client` reach hypixel as **jars**. hypixel never recompiles
them. So:

- A **source**-incompatible change to gson-extras is caught by `compileJava` in hypixel only for
  hypixel's own 29 files. The other four modules' uses of `GsonSettings` / `PostInit` /
  `SerializedPath` are already compiled and are invisible to the compiler.
- A **binary**-incompatible change - removing or renaming a public method, changing a parameter type,
  narrowing a class from `public` to package-private, changing a return type - produces
  `NoSuchMethodError` or `NoClassDefFoundError` at **runtime**, inside code hypixel did not write,
  during `gson.fromJson`.
- `MemberDtoMappingTest` calls `GsonSettings.defaults().create()` (`:55`), which runs
  `ServiceLoader.load(GsonContributor.class)` and instantiates
  `SkyBlockDataGsonContributor` from the skyblock jar. That is the one place the cross-module binary
  contract is actually exercised, and it is exercised by exactly one `@BeforeAll`.

**Rule this imposes on all four proposals:** every library change in this cycle must be
**additive only** at the public surface. New annotation, new annotation element with a `default`, new
factory class, new method - all safe. Any of the following is not safe and needs an explicit
argument:

- changing the signature or visibility of `PostInitTypeAdapterFactory` or
  `CaseInsensitiveEnumTypeAdapterFactory` (compile-breaks `dataflow`, §2.2)
- promoting `WeakIdentityMap` out of `dev.simplified.gson.factory` (`00-verified-facts.md` §7 - it is
  package-private today; a move is an API-surface decision)
- removing `@Extract` handling from `LenientTypeAdapterFactory` in a way that changes any **public**
  member of that class. Moving the *internal* `ExtractFieldInfo` scan is fine; the class's public
  `create` signature must not move.

`@Extract` gaining a `filter` element is source- and binary-compatible **provided it has a default**
(`00-verified-facts.md` §11 F4). `@Extract.value` has no default today and must keep none.



## 4. Adoption inventory - the regression surface

Every site below is under
`W:/Workspace/Java/Simplified/Simplified-Api/hypixel/src/main/java/api/simplified/hypixel/response/skyblock/`
and was read off source, not recalled. Line numbers are the **annotation** line.

Exact counts, from a single `grep -rn` over `src/main/java`:

| Annotation | Declaration lines |
| --- | --- |
| `@Capture` | 19 (16 live, 3 dead) |
| `@SerializedPath` | 22 |
| `@Lenient` | 10 |
| `@Extract` | 6 |
| `@Collapse` | 1 |
| `@Key` | 1 |
| `@Split` | 1 |

**60 annotation sites across 25 files.**

One deliberate divergence from `00-verified-facts.md`, stated rather than silently carried:
§10.3 there says "Seventeen fields across twelve classes" for `@Capture`, but its own table lists
sixteen live rows plus the three dead `Experimentation` fields. The grep finds **19 `@Capture`
declaration lines, 16 live and 3 dead**. Nothing downstream depends on which number is right - the
live set and the dead set are identical in both accounts - but the arithmetic should not be inherited
without checking.

### 4.1 `@Capture` - 16 live fields plus 3 dead

`00-verified-facts.md` §10.3 owns the mode/key/value analysis. The declaration lines, for the grep:

`member/AccessoryBag.java`:208, :221; `member/crimson/Dojo.java`:15, :17;
`member/crimson/Kuudra.java`:18, :20; `member/crimson/TrophyFishing.java`:24;
`member/mining/HeartOfTheMountain.java`:49; `member/foraging/HeartOfTheForest.java`:48;
`member/foraging/MelodyHarp.java`:23; `member/SkillTree.java`:36; `member/Statistics.java`:265;
`member/Toolkit.java`:21; `member/slayer/SlayerBoss.java`:26, :29, :31.

`AccessoryBag.java`:208 and :221 are on **nested** classes (`Tuning` and its slot type), not on
`AccessoryBag` itself - relevant because the enclosing class's factory chain is not the one they run
in. Dead, all `transient` and silently inert (`CaptureFieldInfo.of` `:775-776`):
`member/Experimentation.java`:44, :46, :48.

### 4.2 `@Lenient` - 10 fields

`member/Bestiary.java`:38, :40; `member/foraging/Foraging.java`:27;
`member/hoppity/ChocolateFactory.java`:42; `member/Loadouts.java`:20, :26, :32;
`member/dungeon/FloorData.java`:28; `member/Statistics.java`:41;
`member/dungeon/Dungeons.java`:33.

Shapes and the two single-site code paths are in `00-verified-facts.md` §10.2. Restating the two that
matter most here because they set the regression fixtures: `Dungeons.java`:33 is the **only**
collection-shaped `@Lenient` in the workspace, and `Dungeons.java`:33 plus `Statistics.java`:41 are the
**only two** that carry `@SerializedPath` as well and therefore drive `locateElement`'s segment branch.

### 4.3 `@Extract` - 6 fields

`member/Bestiary.java`:33, `member/foraging/Foraging.java`:30,
`member/hoppity/ChocolateFactory.java`:44, :46, `member/Loadouts.java`:23, :29.

Full table in `00-verified-facts.md` §10.1. All six source from a `@Lenient` map by **Java field
name**; none names a `@Capture` field; none names a collection-typed source.

### 4.4 `@Collapse` / `@Key` - 1 pair

One `@Collapse`, one `@Key`, one class pair, and they are on **different classes**:

- `member/slayer/Slayers.java`:20 - `@Collapse @SerializedName("slayer_bosses")`
  `ConcurrentList<SlayerBoss> bosses`. **List mode**, not map mode.
- `member/slayer/SlayerBoss.java`:22 - the `@Key` field the collapse injects the JSON key into.

`SlayerBoss` is the densest class in the module for factory interaction: one `@Key` and three
`@Capture` fields (`:26` with `descend = true`, `:29`, `:31`), reached through a `@Collapse` list.
`CollapseTypeAdapterFactory` rewrites the JSON object into a JSON array before delegating
(`00-verified-facts.md` §2.2), so this is the only site in the workspace where `@Capture` runs inside a
tree that `@Collapse` already reshaped. `GsonFactoryTest`:1979-2166 is the matching library fixture.

Note the `@Collapse` value class carries no `@Lenient` and no `@Extract`, so nothing in scope changes
this path directly - but any factory-index shift in `GsonSettings.defaults()` moves `Collapse`
relative to `Capture`, and this is the only consumer-side site that would notice.

### 4.5 `@Split` - 1 field

`member/crimson/TrophyFishing.java`:21 - `@Split("/") @SerializedName("last_caught")`
`PairOptional<TrophyFish, TrophyFish.Tier> lastCaught`.

Exactly one site in the entire workspace, on a class that also carries a catch-all `@Capture`
(`:24`, `ConcurrentMap<TrophyFish, TierData>`, grouping mode, enum key). `TrophyFishing` is therefore
the only class where `@Split` and `@Capture` compose, and it is one of the six unmatched-enum-key
sites (`00-verified-facts.md` §10.4). `@Split` **removes** its key from the tree before delegating
(§2.2), so `last_caught` is invisible to the `@Capture` catch-all - and that is load-bearing, because
without the removal a catch-all with an enum key would try to bind `last_caught` as a `TrophyFish`.

`SplitTypeAdapterFactory` is registered inner to `Capture`, so the removal happens **after** classify.
The reason `last_caught` does not reach the catch-all is not `@Split` at all - it is that
`last_caught` is a declared field and therefore a known key (`discoverKnownKeys` `:114-143`). Worth
stating because a design that changes known-key discovery would break this site silently.

### 4.6 `@SerializedPath` - 22 fields

`SkyBlockMember.java`:74, :101, :123, :137; `member/Bestiary.java`:35;
`member/JacobsContest.java`:26, :28, :31; `member/Statistics.java`:42, :239, :241;
`member/crimson/Abiphone.java`:24; `member/crimson/CrimsonIsle.java`:39, :42;
`member/dungeon/DungeonChest.java`:26, :28; `member/dungeon/Dungeons.java`:34;
`member/foraging/Foraging.java`:19, :34, :38; `member/mining/CrystalHollows.java`:19, :21.

Out of scope for this cycle, listed because two of them sit on `@Lenient` fields (§4.2) and because
`skyblock` and `mojang` each carry one, making it the only annotation with cross-module adoption.

### 4.7 Density map - which classes carry more than one

The regression fixtures, ranked by how many factories one instance drives:

| Class | Annotations on it | Factories in its chain |
| --- | --- | --- |
| `member/slayer/SlayerBoss.java` | `@Key` :22, `@Capture` :26 (descend) :29 :31 | Collapse (via `Slayers`), Capture, reflective |
| `member/Loadouts.java` | `@Lenient` :20 :26 :32, `@Extract` :23 :29 | Lenient, Optional, reflective |
| `member/foraging/Foraging.java` | `@SerializedPath` :19 :34 :38, `@Lenient` :27, `@Extract` :30 | SerializedPath, Lenient, reflective |
| `member/Statistics.java` | `@Lenient` :41 + `@SerializedPath` :42, `@SerializedPath` :239 :241, `@Capture` :265 | SerializedPath, Lenient, Capture, reflective |
| `member/crimson/TrophyFishing.java` | `@Split` :21, `@Capture` :24 | Split, Capture, reflective |
| `member/dungeon/Dungeons.java` | `@Lenient` :33 + `@SerializedPath` :34 | SerializedPath, Lenient, reflective, `PostInit` |
| `member/Bestiary.java` | `@Extract` :33, `@SerializedPath` :35, `@Lenient` :38 :40 | SerializedPath, Lenient, reflective, `PostInit` |
| `member/hoppity/ChocolateFactory.java` | `@Lenient` :42, `@Extract` :44 :46 | Lenient, reflective |

`Statistics` is the only class carrying `@Lenient`, `@SerializedPath` **and** `@Capture` at once, and
it is therefore the highest-value single consumer fixture for anything that reorders or inserts a
factory. It is decoded by `MemberDtoMappingTest.mapsStatistics` (`:193`) and
`mapsCandyFestivals` (`:207`) - but see §7.1 for what those actually assert.



## 5. The `@Extract` factory lift - every way it changes observable behaviour

### 5.1 What changes structurally

Today `@Lenient` and `@Extract` are handled by **one adapter object**, built by one `create` call
(`LenientTypeAdapterFactory.java`:64-73). That single fact underwrites four behaviours that a split
has to reproduce by hand:

1. **One `write` method does both halves in a fixed order.** `:98-118` re-injects every `@Extract`
   value into `OVERFLOW`, then `:121-145` merges `OVERFLOW` into the tree. Both loops read
   `this.getLenientFields()` and `this.getExtractFields()` off the same instance.
2. **The re-injection needs the source field's `isMap()`** (`:108`) to decide `JsonObject` vs
   `JsonArray`. That comes from the `LenientFieldInfo`, found by matching
   `lenientInfo.getFieldName()` against `extractInfo.getSourceFieldName()` (`:104`).
3. **One `read` frame holds the overflow list.** The extract phase (`:203-221`) reads the frame-local
   allocated at `:162`, and the post-assign at `:242-248` runs in the same frame as the overflow
   publish at `:230-240`.
4. **`create` returns the delegate when both lists are empty** (`:70-72`), so
   `LenientTypeAdapterFactory` occupies a chain slot for every type but wraps nothing when idle
   (`00-verified-facts.md` §2.3).

Split into two factories, (1) and (3) become **cross-adapter ordering problems**, and (2) becomes a
**duplicated field scan** that can disagree with the original.

### 5.2 Risk register

Every row is a concrete way the lift changes observable behaviour. Cited against current source.

| # | Risk | Mechanism | Detected by |
| --- | --- | --- | --- |
| L1 | **Write and read want opposite nesting.** See §5.3 - this is the headline | Gson unwinds `write` inner-first and `read` outer-first | Nothing in either suite (§6.3) |
| L2 | Re-injection lands in the store but never in the document | If `Extract` is outer to `Lenient`, `Lenient.write`'s merge-back at `:127` has already run by the time `Extract.write` calls `computeIfAbsent` at `:108`. The key is stored for the *next* write and dropped from this one | Nothing today |
| L3 | Duplicated `@Lenient` scan disagrees | `LenientFieldInfo.of` `:437-445` skips `transient`, skips fields carrying `@Capture`, and skips non-`ParameterizedType`. A separate `Extract` factory that re-scans for source fields with different gates keys `OVERFLOW` on a collection `Lenient` never publishes - a permanent orphan entry in a static map that has no `remove` (`00-verified-facts.md` §7) | Nothing today |
| L4 | `isMap()` is re-derived wrong | `:108` picks `JsonObject` vs `JsonArray` from the source field. Getting it backwards makes W3a's dead-`JsonArray` branch (`:110-111`) the live one - the value is created, dropped, and an empty `JsonArray` is installed in the store as a side effect | Nothing today |
| L5 | Tree contents change if `Extract` moves outside `Capture` | Outside `Capture`, the tree is the **full root**, not `knownObject` (`00-verified-facts.md` §2.2). `@Extract` then claims from the raw `@Lenient` sub-object rather than from its overflow, and the destructive `remove` at `:216` deletes the key before `Lenient`'s filter phase ever sees it. For the six current sites the resulting map is identical; the **write-side merge target** is not | `MemberDtoMappingTest.mapsLoadouts` only if the value changes, which it does not |
| L6 | Assignment order flips against `@Capture` | `@Extract`'s post-assign (`:242-248`) currently runs strictly inside `Capture`'s delegate call, so before `Capture`'s `accessor.set` at `:381`. Outside `Capture` it runs after. No class today has a field that is both an `@Extract` target and a `@Capture` field, so this is latent, not live | Nothing today |
| L7 | Assignment order flips against the reflective binder | B4 in `00-verified-facts.md` §11 - `@Extract` assigns **after** the binder and overwrites it. A new factory that assigns before the binder is silently overwritten instead. All six sites declare `@NotNull` initialisers and would revert to `Optional.empty()` / an empty map | `mapsLoadouts` `:130` catches exactly one of the six |
| L8 | `OVERFLOW` visibility | It is `private static final` on `LenientTypeAdapterFactory` (`:62`). A sibling factory in `dev.simplified.gson.factory` needs it package-private. Internal to the library, invisible to consumers - safe, but it is an edit to a `private` field on a class `dataflow` does not touch. No consumer risk | `compileJava` |
| L9 | Chain-slot change | If the new factory returns `null` when idle (recommended, §2.3) the chain length varies per type; if it returns the delegate, it permanently occupies a slot and changes what a third factory's `getDelegateAdapter` resolves to (invariant A7) | `GsonFactoryTest.CombinationTests` only |
| L10 | Index shift in `GsonSettings.defaults()` | Inserting anywhere in `:249-256` renumbers every later factory. The pairs that observe order are `Collapse`/`Capture` and `Lenient`/`Capture` (invariant A6) | `CombinationTests` `:1979-2253` |
| L11 | Dead configuration becomes live | `LenientFieldInfo.of` skips `@Capture` fields, so `@Lenient` + `@Capture` on one field is `@Capture`-wins and any `@Extract` naming it silently no-ops. Adding a filter axis makes that combination meaningful. No site uses it, so this is a contract change with zero adoption impact - but it should be stated, not discovered | Nothing |

### 5.3 The two placements, and why neither is free

This is the adversarial core of the section, and it argues against the simplest reading of the
"lift `@Extract` into its own factory" item.

Gson's two directions are not symmetric. On **read**, the outermost adapter pre-processes first and
the innermost post-assigns first. On **write**, the innermost produces the tree first and the
outermost post-processes last. So for two adapters A (outer) and B (inner):

```
read :   A.pre  ->  B.pre  ->  bind  ->  B.post  ->  A.post
write:            B.post-process  ->  A.post-process
```

Apply that to a split:

- **`Extract` inner to `Lenient`.** Write is correct: `Extract` re-injects into `OVERFLOW`, then
  `Lenient` merges it into the field's sub-object. Read is broken: `Lenient` publishes its overflow at
  `:239`, which is in its own **post**-assign - after `Extract` has already run. `Extract` still cannot
  see it.
- **`Extract` outer to `Lenient`.** Read is fixable: `Extract` pre-processes the raw tree before
  `Lenient` filters it. Write is broken: `Lenient`'s merge-back at `:121-145` completes before
  `Extract` re-injects, so the claimed key is stored for a future write and missing from this one
  (L2). All six sites lose round-trip fidelity.

**No registration index satisfies both directions.** Adapter nesting is the wrong mechanism for this,
which is the same conclusion `00-verified-facts.md` C3 reaches from the producer side. A split that
relies on nesting for either direction is wrong; a split needs an explicit read-scoped channel *and*
an explicit write-side ordering contract, and the write side is the half that is easy to forget
because **nothing tests it** (§6.3).

The cheapest correct shape, stated as a constraint rather than a design: whatever publishes and
whatever consumes must remain in **one adapter instance per class**, or the two must be sequenced by
something other than their chain positions. Keeping `@Extract`'s *write* half where it is while
lifting only the *read* half is a valid answer and costs less blast radius than a full lift - but it
makes `ExtractTypeAdapterFactory` a misnomer, and the design entry should say so.



## 6. Test coverage map

### 6.1 The 134, by file

Counted by `@Test` occurrences; the total is exactly 134, matching the stated baseline.

| File | Tests | Relevant to this cycle |
| --- | --- | --- |
| `GsonFactoryTest.java` | 73 | yes - all eight nests |
| `JsonTreeTest.java` | 13 | no |
| `JsonTreeAdditiveTest.java` | 12 | no |
| `adapter/ColorTypeAdapterTest.java` | 8 | no |
| `factory/CaptureGroupingModeTest.java` | 6 | yes - `b071689`/`7cfc181` regression set |
| `factory/CollectionValueCompatibilityTest.java` | 5 | yes - `c944987` regression set, one `@Lenient` model |
| `factory/WeakIdentityMapTest.java` | 5 | yes if the store type changes |
| `GsonSettingsPrewarmTest.java` | 5 | indirectly - `create()` prewarm resolves adapters eagerly |
| `adapter/SafeAdapterHardeningTest.java` | 4 | no |
| `GsonContributorTest.java` | 3 | no |

`GsonFactoryTest`'s 73 split as: `CaptureTests` 26, `CollapseTests` 12, `CombinationTests` 9,
`SerializedPathTests` 6, `OptionalTypeAdapterTests` 6, `SplitTests` 6, `PostInitTests` 5,
`HtmlEscapingTests` 3.

**There is no `LenientTests` nest.** Across the entire suite, `@Lenient` appears on exactly three
model classes and `@Extract` on exactly one:

- `GsonFactoryTest`:2175 `LenientWithCaptureModel` - asserted by `lenientWithCapture_ok` `:2183`
- `GsonFactoryTest`:2217-2219 `FullCombinationModel` - the only `@Extract` in the suite, asserted by
  `lenientExtractCapture_ok` `:2227`
- `CollectionValueCompatibilityTest`:109 `LenientToolkit` - asserted by
  `lenientFiltersObjectEntriesFromCollectionMap_ok` `:73`

Three tests for `@Lenient`. One for `@Extract`. Out of 134.

### 6.2 What each affected path is guarded by

| Path | Guarded by | Strength |
| --- | --- | --- |
| `@Capture` classify, entry mode | `CaptureTests` `:557-801` | strong |
| `@Capture` grouping mode | `CaptureTests` `:804-1216`, all 6 of `CaptureGroupingModeTest` | strong |
| `@Capture` collection value types | `CollectionValueCompatibilityTest` `:27-70` | strong |
| `@Capture` write, captured entries | `writeCapture_ok` `:579`, `filteredRoundTrip_ok` `:784`, `mapOfMapsCaptureRoundTrip_ok` `:1048`, the four grouping round-trips | adequate |
| `@Capture` `descend` | `descendCapture_ok` `:1905`, `descendCaptureRoundTrip_ok` `:1932`, `descendCaptureSimple_ok` `:1958` | adequate |
| Factory nesting, `Collapse` x `Capture` | `:2006`, `:2046`, `:2100`, `:2136` | adequate - the only nesting tests |
| Factory nesting, `Lenient` x `Capture` | `:2183`, `:2227` | **read-only** |
| `@Lenient` map filter | `:2183`, `:2227` | thin |
| `@Lenient` collection filter | `CollectionValueCompatibilityTest`:73 | single test |
| `@Extract` read, map source, scalar target | `:2227` | single test |
| `WeakIdentityMap` semantics | all 5 of `WeakIdentityMapTest` | strong |
| `@Split` | `SplitTests` `:1623-1737` incl. `splitRoundTrip_ok` | adequate |
| `@Collapse`/`@Key` | `CollapseTests`, 12 tests | strong |
| `PostInit` swallow as intended behaviour | `postInitExceptionSwallowed_ok` `:1838` | this pins the catch as a **contract** |

### 6.3 The gap list - affected paths with zero coverage

The most valuable output of this survey. Every row is a path one of the five scoped items touches,
that **no test in either module exercises**. A regression in any of these is 134/134 and 16/16 green.

| # | Uncovered path | Source | Which scope item touches it |
| --- | --- | --- | --- |
| **G1** | **`@Lenient` write, whole method.** No test in the suite calls `toJson` on a model carrying `@Lenient`. `LenientTypeAdapter.write` `:86-150` - all 65 lines - is unexecuted by the test suite | `LenientTypeAdapterFactory`:86-150 | 1, 2, 3 |
| **G2** | **`@Extract` write re-injection.** `:98-118` - unexecuted. The `computeIfAbsent` at `:108`, the `isMap()` branch at `:108`, the dead `JsonArray` drop at `:110-111` | `:98-118` | 1, 2 |
| **G3** | **`@Lenient` overflow merge-back.** `:121-145` - unexecuted. Both the object branch `:137-138` and the array branch `:142-143` | `:121-145` | 1, 2, 3 |
| **G4** | **The `Lenient` `OVERFLOW` store is never read by any test.** `:127` `OVERFLOW.get` is the only read on the write path and no test reaches it. `WeakIdentityMapTest` tests the map class, not this store | `:62`, `:127`, `:239` | 1 |
| **G5** | **The `Capture` `OVERFLOW` store is never read by any test.** `typeFilteredCapture_ok` `:642` *produces* overflow ("invalid": "not_an_int") but never serializes. `filteredRoundTrip_ok` `:784` serializes but its input has no incompatible entry, so `:386` never fires | `CaptureTypeAdapterFactory`:239-250 | 1, 4 |
| **G6** | **Unmatched enum key.** `filterWithEnumKey_ok` `:738` and `bareEntryGroupingWithEnumKey_ok` `:966` both use **only matching** enum names. No test presents an unmatched key, so the `{null=4, BASIC=1}` collapse has no signal at all | `:398`/`:474` | 4 |
| **G7** | **`@Extract` conversion failure.** The empty catch at `:246-247`. No test drives a type mismatch between the extracted JSON and the target field | `:242-248` | 1, 2, 5 |
| **G8** | **`@Extract` no-match.** The silent `continue` at `:211-212` when no `FieldOverflow` has the named source field. No test | `:206-212` | 2 |
| **G9** | **`@Extract` naming a collection-typed `@Lenient` source.** R5b - only a `JsonObject` overflow is searched (`:214`), so it no-ops. No test, and no adoption site | `:214` | 2 |
| **G10** | **Dotless `@Extract("field")`.** Yields `jsonKey = ""` (`:474-476`) and claims a key that never exists. No test | `:468-476` | 2 |
| **G11** | **Empty `@Lenient` overflow is published anyway** (`:236-239`), unlike `@Capture` (`:386`). No test observes the asymmetry, so a design that unifies the two stores can silently change it | `:236-239` vs `:386` | 1 |
| **G12** | **Non-object root short-circuits.** `Lenient` `:156-157` and `Capture` `:260-261`. No test feeds a JSON array or primitive to a model type carrying these annotations | `:156`, `:260` | 1 |
| **G13** | **Null delegate result.** `Lenient` `:226-227` and `Capture` `:368-369` discard that read's overflow. No test | `:226`, `:368` | 1 |
| **G14** | **Factory registration order.** No test asserts the contents, order or length of `GsonSettings.defaults()`'s factory list. `GsonSettingsPrewarmTest` counts adapter resolutions, not positions. The only order signal is behavioural, via 6 `CombinationTests` | `GsonSettings.java`:248-257 | 1 |
| **G15** | **`transient @Capture` is silently inert.** `Experimentation.java`:44, :46, :48. No test, no warning | `CaptureFieldInfo`:775-776 | 4 |
| **G16** | **`@Lenient` + `@Capture` on one field.** `LenientFieldInfo.of` `:437-438` makes `@Capture` win and `@Lenient` dead. No test pins that as intended | `:437-438` | 1, 3 |

### 6.4 Why the gap is worse than the count suggests

Four amplifications, stated adversarially.

1. **The gaps cluster exactly where the design is.** G1 through G5 are the entire write half of both
   overflow mechanisms - which is precisely what a shared store with per-entry merge-target tagging
   changes (`00-verified-facts.md` §11 H2). The mechanism this cycle is redesigning is the one
   mechanism with no assertions on it.
2. **The consumer suite cannot compensate.** `MemberDtoMappingTest` never calls `toJson` either
   (§7.1). So the workspace has **zero** end-to-end round-trip coverage for `@Lenient`/`@Extract`,
   at either level.
3. **A green suite is therefore not evidence.** "134/134 and 16/16 after the change" is compatible
   with all six `@Extract` sites having lost round-trip fidelity, with both overflow stores leaking
   orphan entries, and with every unmatched enum key still collapsing onto `null`.
4. **Half the gaps are cheap to close.** G1-G5, G7, G11 are each one `toJson` assertion on a model
   that already exists in the test file. They should be written **before** any library edit, so they
   pin current behaviour rather than the new behaviour (§8.4).



## 7. The two consumer-side checks, and their limits

### 7.1 `MemberDtoMappingTest` is read-only

`src/test/java/api/simplified/hypixel/response/skyblock/MemberDtoMappingTest.java`, 16 tests, is the
whole of hypixel's suite. It decodes subtrees of `src/main/resources/craftedfury.json` through
`GsonSettings.defaults().create()` (`:55`), taking a sparse member from profile 0 and a populated one
from profile 1 (`:65-66`).

**It never calls `toJson`.** Every assertion is on a `gson.fromJson` result (`:83-85`). So the
consumer suite cannot catch any write-side regression - it compounds G1-G5 rather than covering them.

What it actually asserts, against the 57 annotation sites:

| Annotation | Sites | Sites an assertion depends on |
| --- | --- | --- |
| `@Extract` | 6 | **1** - `Loadouts.equippedArmorSet`, via `mapsLoadouts` `:130` |
| `@Lenient` | 10 | **3** - `Loadouts.armorSets` `:127`, `.equipmentSets` `:129`, `.loadouts` `:131` |
| `@Capture` | 16 live | 7 - `AccessoryBag.slots` `:114`, `Toolkit.tools` `:142` and `:158`, `HeartOfTheForest.tiers` `:175`, `SkillTree.entries` `:188`, `Statistics.festivals` `:214`, `AccessoryBag` stats (via `:117` `not(hasKey("purchase_ts"))`) |
| `@Collapse`/`@Key` | 1 | **0 direct** - `mapsDungeonLookups` `:103` reaches `Dungeons`, not `Slayers`; no assertion decodes `Slayers.bosses` |
| `@Split` | 1 | **0** - `TrophyFishing.lastCaught` is never decoded |
| `@SerializedPath` | 22 | ~6 |

Three classes are **decoded but their scoped annotations unasserted**, which is the worst case -
the test exercises the code path and would only fail on a thrown exception, not on wrong data:

- `mapsChocolateFactory` `:223` decodes `ChocolateFactory` but asserts only `chocolateLevel` and
  `rabbitHotspot`. Both `@Extract` sites (`:44`, `:46`) and the `@Lenient` `rabbits` (`:42`) are silent.
- `mapsHuntingToolkit` `:154` decodes `Foraging` but asserts only `huntingToolkit`. The `@Lenient`
  `treeGifts` (`:27`) and the `@Extract` `milestoneTierClaimed` (`:30`) are silent.
- `Bestiary`, `FloorData` and `Dungeons.unlockedJournals` are never decoded at all. `Bestiary` carries
  one `@Extract` and two `@Lenient`; `Dungeons.unlockedJournals` is the **only** collection-shaped
  `@Lenient` in the workspace.

So of six `@Extract` sites the consumer suite pins **one**, and of ten `@Lenient` sites it pins
**three** - all three on the same class.

`SkyBlockMember` as a whole is deliberately never decoded (class javadoc `:41-46`): its `postInit`
needs a live JPA session. That means the `PostInit` half of the chain is only exercised on
`Dungeons`, `Bestiary`, `CrimsonIsle` and `JacobsContest`, and only where they are decoded.

### 7.2 `json_dto_diff.py` parses source, not behaviour

`scripts/json_dto_diff.py` walks the JSON fixture and the DTO class graph **in parallel with a regex
Java parser** (`parse_sources` `:140-212`). It never constructs a `Gson`. It therefore **cannot detect
a factory-behaviour regression at all** - it detects only DTO-to-JSON *coverage* drift.

Baseline, measured now:

```
cd W:/Workspace/Java/Simplified/Simplified-Api/hypixel
py -3 scripts/json_dto_diff.py
=== UNMAPPED JSON KEYS (792) ===
-- objectives (792)
...
exit 1
```

Two operational facts the plan must carry:

- `python` is not on PATH in this environment; the launcher is `py -3`.
- **The differ already exits 1 today, with 792 unmapped keys, all under `objectives`.** It is not a
  green gate. Using it means saving the 792-line output as a baseline file and diffing, not checking
  the exit code.

Three ways the differ's own parser can be broken by this cycle, each a false signal rather than a
real regression:

| # | Change | What the parser does | Result |
| --- | --- | --- | --- |
| D1 | `@Extract` gains a `filter` element and any site writes `@Extract(value = "a.b", filter = "...")` | `ann_value(ann, "Extract")` `:175` uses the **no-parameter** pattern `@Extract\s*\(\s*"([^"]*)"\s*\)` (`:100`), which requires a lone string literal and an immediate `)`. It returns `None` | The field's key silently falls back to the Java field name (`:181`), so the real JSON key reports as unmapped and the Java name reports as a phantom binding |
| D2 | `@Fallback` or `@Flatten` is adopted | Neither name appears anywhere in the script | A `@Flatten`ed wrapper level reports as unmapped; a `@Fallback` default reports nothing, which is correct by luck |
| D3 | Anything changes about `@Lenient` | **`Lenient` appears nowhere in the script.** A `@Lenient` field is parsed as an ordinary map | Overflow keys were never modelled by the differ and still are not - so the differ cannot see whether an entry landed in the map or in overflow |

D1 is the one that will actually fire, because scope item 2 is exactly "`@Extract` gains a filter
element". **The differ must be patched in the same commit as the first `@Extract(filter = ...)`
adoption site**, or its output becomes noise. That patch is `ann_value(ann, "Extract", param="value")`
with a fallback to the current call - three lines, consumer-side, no library cycle.



## 8. Pre-flight verification plan

The plan has two independent axes, and conflating them is the mistake to avoid:

- **Source compatibility across the workspace** - answered locally by the composite, no push needed.
- **Binary compatibility against the published sibling jars** - answered only by a standalone hypixel
  run after a re-pin, because that is the only configuration where `skyblock`, `client`, `github` and
  `persistence` arrive as jars compiled against an older gson-extras (§3.2).

Verified just now, from `W:/Workspace/Java/Simplified`:

```
./gradlew -q --console=plain :Simplified-Api:hypixel:dependencyInsight \
    --configuration compileClasspath --dependency gson-extras

project :Simplified-Dev:gson-extras (by composite build)
com.github.simplified-dev:gson-extras:{strictly 2ba8143} -> project :Simplified-Dev:gson-extras
+--- project :Simplified-Dev:client
+--- project :Simplified-Api:github
+--- project :Simplified-Api:skyblock
\--- project :Simplified-Dev:persistence
```

Every sibling substitutes to a local project too, so the composite recompiles all of them **from
source** against the gson-extras working tree. That is a full-workspace source-compatibility check for
the price of one build and zero JitPack cycles.

### 8.1 Before - capture the baseline

Run all of this **before touching a single library file**, and save every output. Ten minutes.

1. `gradle_verify gson-extras compileJava test --rerun` -> record exit 0 and the tally.
   `test_tally gson-extras` -> record **134/134**.
2. `gradle_verify hypixel compileJava test --rerun`; `test_tally hypixel` -> record **16/16**.
3. `cd W:/Workspace/Java/Simplified/Simplified-Api/hypixel && py -3 scripts/json_dto_diff.py > /tmp/diff-baseline.txt`
   -> record **792 unmapped, exit 1**. This file is the gate, not the exit code (§7.2).
4. From `W:/Workspace/Java/Simplified`, the composite compile of every consumer:
   `./gradlew --console=plain :Simplified-Api:hypixel:compileTestJava :Simplified-Api:skyblock:compileJava
   :Simplified-Dev:persistence:compileJava :Simplified-Dev:dataflow:compileJava
   :Simplified-Dev:client:compileJava :Minecraft-Library:asset-renderer:compileJava`.
   Record exit 0. This is the source-compatibility baseline.
5. Record the resolved pin graph:
   `cd Simplified-Api/hypixel && ./gradlew -q dependencyInsight --configuration compileClasspath
   --dependency gson-extras > /tmp/pins-baseline.txt`.
6. Record `git rev-parse --short HEAD` in gson-extras (`7cfc181`) and
   `jitpack_status gson-extras` (28 records, 26 ok).

### 8.2 During - the per-item gate

Run after **each** of the five scope items, in gson-extras only. Do not batch this.

1. `gradle_verify gson-extras compileJava test` - must stay at exactly 134 + whatever new tests that
   item shipped. A **drop** in count means a test silently stopped being discovered; the tally is not
   just pass/fail.
2. Named re-runs, per `00-verified-facts.md` §11 G, whenever the item touched `GsonSettings`,
   `LenientTypeAdapterFactory` or `CaptureTypeAdapterFactory`:
   `GsonFactoryTest$CombinationTests` (all 9), `GsonFactoryTest$CaptureTests` (all 26),
   `CaptureGroupingModeTest` (6), `CollectionValueCompatibilityTest` (5), `WeakIdentityMapTest` (5).
3. The composite compile from step 8.1.4 - this is what catches a `dataflow` break from a factory
   signature change (§2.2) before it ever reaches a push.
4. If the item added a factory to `GsonSettings.defaults()`, print the resulting list and confirm the
   intended nesting **depth**, not the index (invariant A1). There is no test for this (G14).

### 8.3 After - the consumer pass

1. From the composite root: `./gradlew --console=plain :Simplified-Api:hypixel:test` - this runs
   hypixel's 16 against the gson-extras **working tree**, before any push. It will catch behavioural
   regressions at the one `@Extract` and three `@Lenient` sites the suite pins (§7.1), plus every
   `@Capture` site, which is where most of the coverage actually is.
2. `py -3 scripts/json_dto_diff.py > /tmp/diff-after.txt && diff /tmp/diff-baseline.txt /tmp/diff-after.txt`
   - expect an **empty diff** unless an adoption site changed. Any change here after a
   library-only edit is a differ-parser artefact (D1-D3), not a coverage change.
3. Only after the above are green: commit, push, JitPack, re-pin (§9), then
   `gradle_verify hypixel compileJava test --rerun` **standalone** in the hypixel directory. This run,
   and only this run, exercises the published sibling jars against the new gson-extras and can surface
   a `NoSuchMethodError` / `NoClassDefFoundError` (§3.3).
4. Re-run `dependencyInsight` and diff against `/tmp/pins-baseline.txt`. The four forced upgrades
   should still be four; a new one means a sibling was re-published mid-flight.

### 8.4 New tests this cycle must ship

These close §6.3 and are the precondition for trusting any later green run. **Write them against
current behaviour first, on `7cfc181`, and confirm they pass before any library edit.** A test written
after the change pins the new behaviour and proves nothing.

Minimum set, in gson-extras, all cheap because the models already exist:

| Closes | Test | Shape |
| --- | --- | --- |
| G1, G3 | `lenientOverflowMergesBackOnWrite_ok` | `fromJson` `LenientWithCaptureModel` with an incompatible entry, `toJson`, assert the entry is back inside the `stats` sub-object |
| G2 | `extractReinjectsOnWrite_ok` | `fromJson` `FullCombinationModel` (`:2214`), `toJson`, assert `kills.last_killed_mob` is present in the output |
| G1, G2, G3 | `lenientExtractRoundTrip_ok` | `fromJson` -> `toJson` -> `fromJson`, assert the second object equals the first, including `lastKilledMob` |
| G3 array half | `lenientCollectionOverflowMergesBackOnWrite_ok` | `LenientToolkit` (`CollectionValueCompatibilityTest`:107), the `JsonArray` branch `:142-143` |
| G5 | `captureOverflowMergesBackOnWrite_ok` | the `typeFilteredCapture_ok` `:642` input, then `toJson`, assert `"invalid": "not_an_int"` is back at the **root** |
| G5 + G3 together | `lenientAndCaptureOverflowGoToDifferentTargets_ok` | one model with both, assert the `@Lenient` entry lands in the sub-object and the `@Capture` entry at the root. This is the test that would catch `00-verified-facts.md` §11 H2 |
| G6 | `unmatchedEnumKeyDoesNotCollapse_ok` | `EnumKeyCaptureModel` (`:738`) with `dojo_points_NOT_A_CONSTANT` **and** `dojo_points_ALSO_MISSING`, assert the map does not contain a `null` key and does not lose an entry |
| G7 | `extractConversionFailureLeavesInitialiser_ok` | pins the empty catch `:246-247` as current behaviour, so a change to it is a deliberate decision |
| G11 | `emptyLenientOverflowIsStillPublished_ok` | asserts the `:236-239` / `:386` asymmetry |
| G12 | `lenientNonObjectRootPassesThrough_ok` | a JSON array against a `@Lenient`-carrying type |
| G14 | `defaultFactoryOrderIsStable_ok` | asserts the exact class list and order of `GsonSettings.defaults()` factories. Cheap, and it turns an index shift from silent into loud |

Consumer side, one test in hypixel, closing the §7.1 gap:

| Closes | Test | Shape |
| --- | --- | --- |
| §7.1 | `MemberDtoMappingTest.roundTripsLoadouts` | decode `loadout`, `toJson`, re-decode, assert `equippedArmorSet`, `equippedEquipmentSet`, and that `armorSets` still has key 1. `Loadouts` is the densest `@Lenient`/`@Extract` class (§4.7) and the only one the suite already pins |
| §7.1 | `MemberDtoMappingTest.mapsBestiary` | `Bestiary` is never decoded today and carries one `@Extract` and two `@Lenient`. `postInit` is a concern - decode the subtree, not `SkyBlockMember` |

Eleven library tests and two consumer tests. That is a **medium** on its own by
`00-conventions.md` §4, and it should be costed into the cycle rather than assumed free.



## 9. JitPack cadence

### 9.1 The exact sequence for one cycle

gson-extras publishes by git sha through JitPack. One cycle, exactly:

1. Commit in `W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras`. Local only.
2. Push to `origin/master`. JitPack cannot see an unpushed sha.
3. `jitpack_status gson-extras` - read-only, confirms the sha is pushed and unambiguous, and reports
   whether a build record already exists. Baseline today: 28 records, 26 ok, 2 error, 0 in flight,
   `origin/HEAD` = `7cfc181` = `ok`.
4. `jitpack_build gson-extras` - triggers **one** build and waits. Never hand-roll the
   `curl .../api/builds/<version>` round: the per-version endpoint silently **starts** a build, so
   every retry is a real build on a third-party service.
5. Edit `Simplified-Api/hypixel/build.gradle.kts`:44 `strictly("<new-sha>")`.
6. `gradle_verify hypixel compileJava test --rerun`, run **standalone** in the hypixel directory. This
   is the only configuration that exercises the published sibling jars (§3.3, §8.3.3).
7. Optionally `toolsmith jitpack pins` for workspace-wide drift.

Steps 1-4 are the irreducible cost. Steps 5-6 are per consuming module, and today only hypixel needs
them. Wall clock is minutes per cycle and is not parallelizable across the sibling modules that share
the pin.

**A test-only commit costs zero re-pins.** Tests are not published, so the eleven library tests in
§8.4 can land as one pushed commit that nobody has to pin to. That is the cheapest possible first
step and it is the one that makes every later green run mean something.

### 9.2 Batch versus staged - the cost table

The five scope items are not independent. Item 2 (`@Extract` filter) needs item 1's store to have
something to filter. Items 3 and 4 are stated as *consequences* of item 1. Item 5 is the only one that
is genuinely separable - `@Fallback` and `@Flatten` are new annotations with new factories, additive
by construction.

| Strategy | Library cycles | Re-pins | What it buys | What it costs |
| --- | --- | --- | --- | --- |
| **One batch, all five** | 1 | 1 | Cheapest in wall clock | A red hypixel run names no culprit. Bisecting means re-running the whole cycle per hypothesis, so the *first* failure costs more than the staging would have. Rollback is all-or-nothing across five items |
| **Staged, per scope item** | 5 | 5 | Every failure is attributed | Items 2-4 cannot be tested in isolation anyway, because the composite (§8) already gives per-item attribution **without** a cycle |
| **Staged by dependency group** (recommended) | 3 | 2 | Attribution where it is possible, cycles where they are needed | One extra cycle over the batch |

Recommended grouping:

- **Cycle 0 - tests only.** The eleven library tests from §8.4, written against `7cfc181` behaviour.
  Push, JitPack build, **no re-pin**. Cost: 1 build, 0 re-pins.
- **Cycle 1 - the overflow group.** Items 1, 2, 3, 4 together. They share the store and cannot be
  meaningfully separated at the pin boundary. Push, build, re-pin hypixel, run 8.3.
  Cost: 1 build, 1 re-pin.
- **Cycle 2 - the additive annotations.** Item 5, `@Fallback` and `@Flatten`. Independent, additive,
  and low-risk. Could be folded into cycle 1 if the owner prefers one re-pin - the argument for
  keeping it separate is that a red run in cycle 1 is unambiguous.
  Cost: 1 build, 1 re-pin.

Every per-item gate in §8.2 runs through the composite and costs **no cycle at all**. The staging
question is therefore only about how many *red hypixel runs* you are willing to have to attribute
by hand, not about how much verification you get.

**One item is not safely batchable with the others.** The unmatched-enum-key fix (item 4) has two
possible homes and they differ by two effort levels:

- Inside `CaptureTypeAdapterFactory`'s key conversion (`:398`, `:474`) - scoped to `@Capture`, six
  adoption sites, all in hypixel. `medium`.
- Inside `CaseInsensitiveEnumTypeAdapterFactory.read` `:82` - which `create` `:35-38` applies to
  **every enum type in the JVM**, in every position, for every consumer. hypixel declares 31 enums,
  `skyblock` 20, `asset-renderer` 90. Changing `nameToConstant.get(...)` from returning `null` to
  throwing is an `xlarge` semantic break by `00-conventions.md` §4, and it is not observable to any
  of the 134 tests plus 16 - `filterWithEnumKey_ok` `:738` and `bareEntryGroupingWithEnumKey_ok` `:966`
  both use only matching names (G6).

`00-verified-facts.md` C6 correctly notes that an enum-side fix reaches `Statistics.java`:89, which a
`@Capture` element cannot. That is true and it is also the argument for **not** doing it in this
cycle: the reach is exactly the blast radius. If the enum-side change lands, it needs its own cycle,
its own tests, and a convergence of all twelve sibling pins - not a slot in a batch.

### 9.3 The sibling convergence decision

Twelve modules pin `2ba8143`, three commits behind (§3.1). This cycle can end in one of two states,
and the plan should pick one deliberately rather than by omission:

- **Leave them.** hypixel moves alone, as it already has. Cost: the drift grows from 3 commits to
  3 + N. The next convergence, whenever it happens, hands eleven modules `c944987`'s
  collection-value-type change, `b071689`'s grouping-mode change and this cycle's overflow work in one
  step, against suites that never exercised any of it.
- **Converge after cycle 2.** Twelve `build.gradle.kts` edits and twelve module verifies. Nothing in
  §2.2 says any of them would notice - none uses `@Capture`, `@Lenient` or `@Extract` - so the risk is
  low and the risk of *not* doing it compounds.

The measured evidence favours converging: §3.2 shows hypixel is **already** running `skyblock`,
`github`, `persistence` and `client` against a gson-extras none of them was compiled against. The pins
are nominal; the runtime has converged whether or not the build files say so. Making that explicit is
cheaper than discovering it through a `NoSuchMethodError`.



## 10. Verdicts - blast radius per scope item

Keyed by scope item, not by a sibling's design slug, because slugs are being assigned concurrently.
These are **blast-radius verdicts**, not design verdicts - the design entries in
`10-design-entries.md` own the adopt/decline call.

| Item | Modules at risk | Adoption sites | Uncovered paths it touches | Blast-radius verdict |
| --- | --- | --- | --- | --- |
| **1** Shared overflow store + `@Extract` into its own factory | hypixel only, behaviourally. `dataflow` if any `factory` class signature moves | 6 `@Extract` + 10 `@Lenient` + 16 `@Capture` = 32 | G1-G5, G11-G14, G16 - **nine** | **Riskier than it looks.** §5.3 shows no registration index satisfies both read and write. The write half is entirely untested. Do not lift both halves; lift the read half or keep one adapter |
| **2** `@Extract` gains a `filter` element | hypixel only. Plus `json_dto_diff.py` (D1) | 6 today, unknown after | G2, G7-G10 | **Low, if the element has a default** (F4 - source and binary compatible). The differ patch must land in the same commit as the first multi-element `@Extract` |
| **3** `@Lenient` consequences | hypixel only | 10 | G1, G3, G11, G12, G16 | **Moderate.** Two of the ten sites are single-coverage code paths (`Dungeons.unlockedJournals` is the only collection-shaped `@Lenient` in the workspace; it plus `Statistics.spawnedSpookyBats` are the only two on the `@SerializedPath` branch) and **neither is decoded by any test** |
| **4** `@Capture` consequences + unmatched-enum-key fix | hypixel, **if** scoped to `CaptureTypeAdapterFactory`. **Every consumer**, if scoped to `CaseInsensitiveEnumTypeAdapterFactory` | 6 defect sites of 16 live `@Capture` | G5, G6, G15 | **Split verdict.** `@Capture`-scoped: moderate, and `CaptureTests` gives real cover. Enum-adapter-scoped: `xlarge`, 141 enum declarations across three modules, zero test signal. Do not batch the second with anything |
| **5** `@Fallback` + `@Flatten` | hypixel only | 0 today - both are new | none of the listed gaps | **Lowest in scope.** Additive files, new factories, no edit to `Lenient`/`Capture`. Two caveats: each new factory takes a slot in `GsonSettings.defaults()` and shifts every later index (A6, G14), and `json_dto_diff.py` knows neither name (D2) |

Cross-cutting, applying to all five:

- **Additive-only at the public surface** is non-negotiable (§3.3). Consumers arrive as jars compiled
  against four different older gson-extras shas.
- **A green suite is not evidence** for items 1-4 until §8.4's tests exist. That is the single
  strongest recommendation in this document.
- **No scope item requires a `GsonSettings` reorder.** `00-verified-facts.md` C3 takes that `xlarge`
  off the table. Any proposal that reintroduces one should be challenged.
- **`@Owner` / `@Parent` is explicitly deferred** by the owner until after the research pack lands and
  is not analysed here. Noting only that it is the one candidate that would need a lifecycle hook
  threaded through every adapter, which `00-conventions.md` §4 rates `large` and which would put every
  consumer's chain in scope rather than hypixel's alone.

## 11. Open questions

1. **Does the write half of `@Extract` move at all?** §5.3 argues it cannot move without breaking
   round-trip fidelity at all six sites, and that nothing would detect it. If the design keeps the
   write half in `LenientTypeAdapterFactory`, the new factory's name should say `read` or the design
   should say plainly that it is a partial lift. Resolved by: the design entry for item 1.
2. **Is the enum-key fix `@Capture`-scoped or enum-adapter-scoped?** The two differ by two effort
   levels and by twelve modules (§9.2). `00-verified-facts.md` C6 shows the enum-side version reaches
   `Statistics.java`:89, which is real value - but that reach is the blast radius. Resolved by: the
   owner, before cycle 1 is planned.
3. **Do the eleven §8.4 tests ship as cycle 0, or inside cycle 1?** Shipping them first is the only way
   a cycle-1 green run means anything, but it costs one extra JitPack build (0 re-pins). Resolved by:
   the implementation plan.
4. **Do the twelve sibling pins converge in this cycle?** §9.3 argues yes on measured evidence.
   Resolved by: the owner.
5. **Does anything actually need the `@Lenient` write path?** G1 says nothing in either suite executes
   `LenientTypeAdapter.write`. It is possible - not established - that no production caller serializes
   a `@Lenient`-carrying DTO at all, in which case the round-trip fidelity this cycle is protecting is
   theoretical. Resolved by: a usage search for `toJson` over `response/skyblock` types in the
   downstream `SkyBlock-Simplified` modules. Worth doing before pricing item 1.
6. **Should `json_dto_diff.py` learn `@Lenient`?** It models overflow nowhere (D3), so it silently
   reports a `@Lenient` field as covering keys that actually went to overflow. Out of this cycle's
   scope, but it is a standing false-negative in the only coverage tool the module has.

