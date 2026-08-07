# 30 - Execution log

## Final state

Stages 0 through 10 complete. Stage 11, the twelve sibling pins, is not done - it is optional, it
touches twelve other repositories, and it is recorded as an owner decision below.

| | |
| --- | --- |
| `gson-extras` | `97d29a4`, pushed and built. **221 passing, 0 skipped**, from 134 |
| `hypixel` | `99b790c`, **committed locally only, not pushed**. **27 passing, 0 skipped**, from 16 |
| Pin | `build.gradle.kts`:44 `strictly("97d29a4")` |
| Cycles | two publishes, two re-pins, plus one test-only publish for cycle 0 |
| Differ | 792 unmapped, unchanged from baseline throughout |

Six library commits, six consumer commits. Every stage boundary was green, every form-B test had its
red observed before the fix landed, and every mechanism the work introduced was verified by breaking
it and confirming the suite noticed.

### What was adopted, and what deliberately was not

| Item | Outcome |
| --- | --- |
| `dgx-overflow-store` | adopted. One tagged `Overflow`; `@Extract` lifted into its own factory outside `@Capture` |
| `dgx-extract-filter` | adopted. Dotless `value()` claims a filtered remainder |
| `@Lenient` consequences | adopted. About ninety lines out, one silent swallow removed |
| `dgx-capture-unmatched` | adopted, branch B in full |
| `dgx-fallback` | annotation and guards **shipped**; **no enum marked** - see stage 10 |
| `dgx-flatten` | adopted at its one site |

### Decisions taken

| # | Decision | Taken |
| --- | --- | --- |
| 1 | `@Extract` no-op rows | keep the silent `continue` |
| 2 | Stage 4 branch B | full, diverting on a failed value as well as a failed key |
| 3 | Fold stages 7-8 into cycle 1 | no - kept separate, two cycles |
| 4 | `PostInit` residue | left in the DTO pack, not smuggled into cycle 2 |
| 5 | `Rarity` / `GameMode` | deferred - other repository, chained publish |
| 6 | Twelve sibling pins | **not taken** - left for the owner |
| 7 | Does anything serialize a `@Lenient` DTO | resolved by search: yes, in production |


Running record of what was actually observed at each stage boundary. Numbers here are measured, not
predicted. Where a measurement contradicts the plan, the plan is corrected here and the correction
is called out rather than silently absorbed.

## Stage 0 - `sgx-baseline`

Captured before any edit. `gson-extras` at `7cfc181`, `hypixel` at `2f6bb56` pinning `7cfc181`.

| Gate | Command | Result |
| --- | --- | --- |
| library build | `gradle_verify gson-extras compileJava test --rerun` | exit 0 |
| library tally | `test_tally gson-extras` | **134 passed, 0 skipped, 19 classes** |
| consumer build | `gradle_verify hypixel compileJava test --rerun` | exit 0 |
| consumer tally | `test_tally hypixel` | **16 passed, 0 skipped, 1 class** |
| differ | `py -3 scripts/json_dto_diff.py` | exit 1, **792 unmapped keys, 792 of them under `objectives`, 0 elsewhere** |
| pin graph | `dependencyInsight --dependency gson-extras` | exit 0, see below |
| composite compile | 6 modules from `W:/Workspace/Java/Simplified` | exit 0 |
| library HEAD | `git rev-parse --short HEAD` | `7cfc181` |
| JitPack | `jitpack_status gson-extras` | 28 records, 26 ok, 2 error, 0 in flight; `origin/master` = `7cfc181`, pushed, built ok |

Artifacts: `/tmp/sgx/diff-baseline.txt`, `/tmp/sgx/pins-baseline.txt`, `/tmp/sgx/composite-baseline.txt`.

### Correction - the forced upgrades are four modules across three shas

`README.md` §12.3 and `04-compatibility.md` §3 say "four forced upgrades: client, github, skyblock,
persistence". The dependency graph resolves **four consuming modules** but only **three distinct
source shas**, because `skyblock` and `client` were both compiled against the same one:

| Old sha | Reaches hypixel through | Forced to |
| --- | --- | --- |
| `b68510e` | `github:38da22c` -> `skyblock:33818f3` | `7cfc181` |
| `f42ee07` | `persistence:cacdb62` (direct, and via `skyblock`) | `7cfc181` |
| `37a2c2f` | `skyblock:33818f3` **and** `client:3d87a03` | `7cfc181` |

The distinction matters for stages 5 and 9 only in that the post-re-pin diff of
`/tmp/sgx/pins-baseline.txt` should expect **three** old-sha nodes, not four. The count of modules
at risk is unchanged.

## Stage 1 - `sgx-characterisation`

No `src/main` file touched in either repository. Four test files modified.

**Landed.** `gson-extras` `14ff7c3` "Characterise the @Lenient and @Extract paths before they move",
pushed, JitPack built ok in 73s. `hypixel` `b2ae503` "Cover the round trip through the @Lenient and
@Extract member DTOs", committed locally and **not pushed** - it sits on top of two commits that
predate this work, and cycle 0 needs only the library sha. `build.gradle.kts`:44 is untouched and
still reads `strictly("7cfc181")`: there is nothing in `14ff7c3` a consumer needs, so cycle 0 costs
one build and zero re-pins.

### Decision 7 - resolved by search: the write path is live in production

`20-implementation-order.md` §22 asks whether anything in the wider workspace actually serializes a
`@Lenient`-carrying DTO, and defaults to "assume it matters". It does not need assuming.

| Link | Evidence |
| --- | --- |
| The server replaces Spring's JSON converter with Gson | `Simplified-Dev/spring-framework` `config/ServerWebConfig.java`:53, :100 - `GsonHttpMessageConverter` seeded from `GsonSettings.defaults().create()`, with the Jackson converter removed |
| A controller returns a member-bearing DTO | `SkyBlock-Simplified/server` `controller/SkyBlockController.java`:56-57 - `@GetMapping("/profiles/{playerId}") SkyBlockProfiles getProfiles(...)` |
| That DTO reaches every `@Extract` and `@Lenient` site | `SkyBlockProfiles`:20 -> `ConcurrentList<SkyBlockIsland>` -> members -> `SkyBlockMember` -> `Loadouts`, `Bestiary`, `Dungeons`, `ChocolateFactory`, `Foraging` |

So `LenientTypeAdapter.write` runs on every `/skyblock/profiles` response, and the duplicate-emission
defect is not a latent library curiosity - it is **live on a shipped HTTP API**, adding
`lastKilledMob`, `equippedArmorSet` and `equippedEquipmentSet` to every serialized member under Java
field names no upstream document carries.

Two consequences. The round-trip fidelity the overflow rewrite protects is **not** theoretical and
its price does not need re-arguing. And `hypixel/src/main` having zero `toJson` calls - the fact the
pack used to price the risk - is true and irrelevant, because the serializing caller lives two
repositories away in a module that reaches the DTOs only as a transitive jar.

### Decision 1 - taken: keep the silent `continue`

An `@Extract` whose source can never match - misspelled, inherited, or unannotated - stays a silent
`continue` rather than throwing at `create`. Sibling modules share the pin, so throwing is a hard
break for any downstream module quietly doing nothing today, and the diagnostic is late either way
because `GsonSettings.prewarm` catches `Throwable` per type and defers it to first real use. The
`ExtractTypeAdapterFactory` rewrite therefore inherits three no-op rows unchanged, and the
`create`-time rejection tests the pack commissions for them are **not** written.

| Gate | Result |
| --- | --- |
| `gradle_verify gson-extras compileJava test --rerun` | exit 0 |
| `test_tally gson-extras` | **155 passed, 3 skipped, 158 discovered, 22 classes** |
| `gradle_verify hypixel compileJava test --rerun` | exit 0 |
| `test_tally hypixel` | **20 passed, 1 skipped, 21 discovered** |
| composite compile, 6 modules | exit 0 |
| `json_dto_diff.py` vs baseline | **byte-identical** |
| `reorder_imports --check` on the 4 changed files | 0 would change |

### Correction - the consumer tally is 20 + 1 skip, not 19

`20-implementation-order.md` §18 predicts "19 pass" for hypixel after stage 1, with all three skips
on the library side. That is internally inconsistent with §12, which says
`loadoutsSerializeHasNoRootExtractKeys` is "written `@Disabled` at stage 1 with its red observed on
`7cfc181`". A form-B test cannot have its red observed at stage 1 and also not exist at stage 1.

Resolved in favour of §12 and of invariant I2: the test is written here, its red is observed here,
and it ships `@Disabled`. It cannot be enabled before **stage 6**, because hypixel stays pinned to
`7cfc181` until the stage 5 re-pin. Revised per-boundary expectation:

| After stage | gson-extras | hypixel |
| --- | --- | --- |
| 0 | 134 pass, 0 skip | 16 pass, 0 skip |
| 1 | **155 pass, 3 skip** | **20 pass, 1 skip** |
| 2 | 157+ pass, 1 skip | 20 pass, 1 skip |
| 4 | 174+ pass, 0 skip | 20 pass, 1 skip |
| 6 | unchanged | 23+ pass, **0 skip** |

### Correction - row 4's named model cannot reach the branch row 4 covers

§7's test table commissions `lenientCollectionOverflowMergesBackOnWrite_ok` against the existing
`LenientToolkit` model and cites "the `JsonArray` branch `:142-143`". `LenientToolkit.tools` is
declared `ConcurrentMap<String, ConcurrentList<Tool>>` - a **Map** whose values are lists.
`LenientFieldInfo.map` is `Map.class.isAssignableFrom(rawType)`, so it is `true`, the read takes the
object branch at `:171` and the write takes the **object** branch at `:134`. The `JsonArray` branch
at `:139-144` is reachable only from a field that is itself a `Collection`, which in the whole
workspace means `Dungeons.unlockedJournals` alone.

Both were written rather than choosing between them:

- `LenientTests.lenientCollectionOverflowMergesBackOnWrite_ok` - a genuinely collection-shaped
  `@Lenient ConcurrentList<Integer>`, which is the only thing that reaches `:139-144`.
- `CollectionValueCompatibilityTest.lenientCollectionMapOverflowMergesBackOnWrite_ok` - the
  write-side case on `LenientToolkit` that the file table asked for, covering the object branch with
  collection-typed values.

## Finding - a `@Lenient` decode rewrites the caller's own JSON tree

Not in the pack, and found by three consumer tests failing for a reason that had nothing to do with
what they were asserting.

`Gson.fromJson(JsonElement, Class)` wraps the element in a `JsonTreeReader`, and
`TypeAdapters.JSON_ELEMENT.read` short-circuits for that reader by returning the **live element
instance** rather than parsing a copy. So `LenientTypeAdapter.read`'s `rootElement` **is** the
caller's object, and the filter phase's `replaceElement` at `:183` / `:197` overwrites the caller's
sub-object with the filtered one. Every overflowed entry is deleted from the tree the caller still
holds.

Observable consequence, on the bundled fixture:

| Decode | What the caller's tree loses |
| --- | --- |
| `Loadouts` | `loadout.armor.equipped_set`, `loadout.equipment.equipped_set` |
| `Bestiary` | `bestiary.kills.last_killed_mob` |
| `Dungeons` | all 25 entries of `dungeons.dungeon_journal.unlocked_journals` |

`MemberDtoMappingTest` parses one fixture in `@BeforeAll` and shares it across every test, so the
first decode of a subtree silently degrades the fixture for every later test that reads the same
subtree raw. The 16 pre-existing tests pass, but they pass over a tree that earlier tests have
already edited.

This is invariant A3 (`00-verified-facts.md` §11) - "`Lenient` mutates `rootObject` in place rather
than building a new tree" - promoted from an internal aliasing note to an externally observable
contract. It is pinned by `MemberDtoMappingTest.lenientDecodeRewritesCallerTree`. Stage 2 relocates
`@Extract` but leaves `@Lenient`'s filter phase alone, so this behaviour should survive; if it
changes, that test turns the change from silent into loud.

Test hygiene was fixed additively rather than by editing the shared `decode` helper: a `pristine`
deep copy is taken in `@BeforeAll` before any test runs, and the new tests read expectations through
`rawPristine(...)` and decode through `decodePristine(...)`. The 16 existing tests are untouched.

## Observed reds - the four form-B tests

Each was run once with `@Disabled` removed, its message recorded verbatim, then re-disabled with
that message as the reason string. Only these four failed; nothing else moved.

| Test | Observed on `7cfc181` |
| --- | --- |
| `GsonFactoryTest$ExtractTests.extractFieldIsNotEmittedAtRoot_ok` | `Expected: iterable with items ["id", "kills", "stat_health"] in any order but: not matched: "lastKilledMob"` |
| `GsonFactoryTest$ExtractTests.twoExtractsNotEmittedAtRoot_ok` | `Expected: iterable with items ["armor", "equipment", "loadouts"] in any order but: not matched: "equippedArmorSet"` |
| `MemberDtoMappingTest.loadoutsSerializeHasNoRootExtractKeys` | `Expected: is <[armor, equipment, loadouts]> but: was <[armor, equippedArmorSet, equipment, equippedEquipmentSet, loadouts]>` |
| `GsonFactoryTest$CaptureTests.unmatchedEnumKeyDoesNotCollapse_ok` | `Expected: not map containing [null->ANYTHING] but: was <{FORCE=100, null=2}>` |

The first three are the duplicate-emission defect, confirmed at both library and consumer level and
on real production data. The fourth is the N-1 loss: two distinct upstream keys, one surviving
value.

## Stage 2 - `sgx-overflow-store`

Cycle 1, commit 1 of 3. Two new library files, three edited, one new test class. **Zero consumer
source edits** - all six `@Extract` sites are byte-identical.

| Gate | Result |
| --- | --- |
| `gradle_verify gson-extras compileJava test --rerun` | exit 0 |
| `test_tally gson-extras` | **178 passed, 1 skipped, 179 discovered, 23 classes** |
| composite compile, 6 modules | exit 0 |
| `:Simplified-Api:hypixel:test` against the gson-extras **working tree** | 20 passed, 1 skipped |
| `json_dto_diff.py` vs baseline | byte-identical |
| `reorder_imports --check`, orphaned-import scan | clean |

Two skips became passes: `extractFieldIsNotEmittedAtRoot_ok` and `twoExtractsNotEmittedAtRoot_ok`.
One skip remains, `unmatchedEnumKeyDoesNotCollapse_ok`, which stage 4 owns.

### Decision 1 applied - failure modes 1, 2 and 3 are a skip, not a throw

`01-overflow-extract.md` §2.9 recommends a `create`-time `JsonException` for an `@Extract` whose
source is misspelled, inherited, or carries neither annotation. Decision 1 took the smaller blast
radius, so `ExtractFieldInfo.of` **skips** such a field instead. That choice has one consequence the
entry does not spell out: a skipped field is absent from `getExtractFields()`, so `write` never
removes its serialized key either, and the field keeps serializing at the root exactly as it does
today. Rejecting it at `create` and keeping it in the list are the only two coherent options -
keeping it in the list while it can never claim would delete the field's value from the output with
nowhere to put it.

The same reasoning gates the write on `isMapSource()`. A collection-shaped source has no key space,
`claim` returns nothing for it, and removing the companion's root key would be pure data loss.

### The dead `JsonArray` path is fixed in the move

`LenientTypeAdapterFactory`:108 called `computeIfAbsent` and installed a `JsonArray` for a
collection-shaped source, then `:110`'s `isJsonObject()` guard dropped the value - the entry was
created, never used, and left in the static store. The moved loop guards **before** `open`, so no
phantom entry is installed.

### Verification - the ordering guarantee, which nothing enforces

R2 says a factory registered between `Capture` and `Extract` silently reduces `@Extract` to its old
capability with no test failure, because every existing site is `@Lenient`-sourced and keeps passing.
Registering `ExtractTypeAdapterFactory` **before** `CaptureTypeAdapterFactory` - which makes it nest
*inside* - was applied as a mutation:

| Failed | Passed |
| --- | --- |
| `extractOverCaptureSource_ok` | every `@Lenient`-sourced row, all six sites' behaviour |
| `extractOverCaptureRoundTrip_ok` | |
| `extractNestsOutsideCapture_ok` | |
| `defaultFactoryOrderIsStable_ok` | |

R2's claim is exactly right, and it is now guarded four ways where it was guarded none. The residual
the entry admits stands: an SPI or `GsonContributor` factory lands outside the whole list and is not
catchable.

Disabling only the root-key removal hunk fails `extractFieldIsNotEmittedAtRoot_ok`,
`twoExtractsNotEmittedAtRoot_ok`, `extractOverCaptureSource_ok` and `extractNestedInsideCapture_ok` -
the form-B validation for the two rows this stage enables, red with the fix hunk out and green with
it in.

### The consumer-side fix is proven before the pin bump

`MemberDtoMappingTest.loadoutsSerializeHasNoRootExtractKeys` stays `@Disabled` because hypixel is
still pinned to a sha without this work. Enabling it temporarily and running hypixel through the
composite - against the gson-extras working tree - gives **21 discovered, 0 skipped, 0 failures**. So
stage 6's enable is evidence rather than hope, and a red run at stage 5 would be a binary-compatibility
problem rather than a behavioural one.

### The guards that could not be built at stage 1 can be built now

`Overflow` is package-private in `dev.simplified.gson.factory` and `OverflowTest` sits in the same
package, so the store is directly observable for the first time. That closes C1, which stage 1
recorded as unguardable:

- `lenientPublishesOverflowEvenWhenEmpty_ok` - every entry compatible, overflow empty, and
  `Overflow.find` returns it anyway.
- `captureDoesNotPublishAnEmptyOverflow_ok` - the mirror image, returning `null`.
- `lenientPublishesUnderFieldElementNotSourceObject_ok` and its `@Capture` twin pin the per-entry
  target tagging directly rather than through a serialize assertion.

C3 - `LenientFieldInfo.of` skipping a field that also carries `@Capture` - is **still** unguardable,
and the store does not change that. `@Capture` is the outer adapter and hands its delegate only
`knownObject`, so a `@Lenient` view of a `@Capture` field is never handed its own key, never
publishes, and never reaches the store at all.

## Mutation testing - are the form-A tests non-vacuous

A characterisation test that passes whether or not the mechanism it names works is worse than no
test, because it converts an invisible regression into a green suite that looks like evidence. Three
mutations were applied to `src/main` one at a time, the whole suite re-run, and `src/main` restored
with `git checkout --` each time. The working tree is clean of `src/main` changes.

**The column that matters is the last one.** It is the count of the original 134 tests that would
have caught the same mutation on `7cfc181` - that is, what the suite would have said before stage 1.

| # | Mutation | Invariant | Caught by, now | Caught by the original 134 |
| --- | --- | --- | --- | --- |
| 1 | `LenientTypeAdapter.write` `:99` - `@Extract` write-side re-injection disabled | B1, second half | **4** - `extractReinjectsOnWrite_ok`, `twoExtractsReturnToOwnSources_ok`, `extractOnHandBuiltObjectReachesDocument_ok`, `extractMutationReachesDocument_ok` | **0** |
| 2 | `LenientTypeAdapter.read` `:216` - `@Extract` read-side claim disabled | B1, first half | **4** - `lenientExtractRoundTrip_ok`, `twoExtractsReturnToOwnSources_ok`, `extractMutationReachesDocument_ok`, plus pre-existing `lenientExtractCapture_ok` | **1** |
| 3 | `LenientTypeAdapter.write` `:132` - overflow merged to the **root** instead of the field's own sub-object | **B2** | **9** - all seven of `LenientTests`/`ExtractTests`' write-path rows, `lenientAndCaptureOverflowGoToDifferentTargets_ok`, `lenientCollectionMapOverflowMergesBackOnWrite_ok` | **0** |

Mutation 3 is the one worth reading twice. `00-verified-facts.md` B2 says a `@Capture`-sourced claim
must not merge back through the `@Lenient` path, and calls wrong-place merge-back "silent and only
visible in a serialize test". It is the specific mistake a shared-`Overflow` design makes if it
treats the store as a map union instead of tagging each entry with its producer's target - which
`00-verified-facts.md` §11 H2 names as one of the two things most likely to be got wrong. Before
stage 1 the entire suite was blind to it.

Two honest results from the same exercise:

- **`extractReinjectsOnWrite_ok` survives mutation 2 and `lenientExtractRoundTrip_ok` survives
  mutation 1.** That is correct rather than a defect - one is a write-path test and the other is a
  round-trip test - but it means neither alone covers both halves of B1. Together they do, and
  `twoExtractsReturnToOwnSources_ok` and `extractMutationReachesDocument_ok` catch both.
- **`extractConversionFailureLeavesInitialiser_ok` survives every mutation above.** The field holds
  its initialiser whether the claim succeeded and the conversion failed, or the claim never
  happened. It pins B7 as a contract, which is what it was commissioned for, but it is not evidence
  that the swallow at `:246-247` is reached. Stage 2 replaces that catch, so this is worth knowing
  before it is used as a gate.

### Four tests were provably vacuous, and one mutation run proved all four at once

An adversarial review pass over the authored tests predicted four of them were vacuous. All four
mutations were then applied to `src/main` **simultaneously** - four separate mechanisms disabled in
one tree - and the whole suite passed with **zero failures**. That is proof, not suspicion.

| Test as first written | Mutation it did not notice | Why it could not |
| --- | --- | --- |
| `lenientNonObjectRootPassesThrough_ok` | `:156` non-object-root short-circuit disabled | With the guard dead, `getAsJsonObject()` on a `JsonArray` throws `IllegalStateException`, which gson rewraps as `JsonSyntaxException` - the same type the delegate throws. A bare `assertThrows(JsonSyntaxException.class)` cannot tell a rewrapped crash inside the factory from the delegate's complaint |
| `lenientFieldShapeMismatchIsUnfiltered_ok` | `:171` shape guard reduced to `isMap()` | Same rewrap. The test pinned "this input throws" rather than "the field reaches the delegate unfiltered" |
| `emptyLenientOverflowIsStillPublished_ok` | `:239` publish made conditional, adopting `@Capture`'s policy | An empty published overflow and an absent one are indistinguishable: the merge loop either skips on `null` or iterates zero entries |
| `captureWinsOverLenientOnOneField_ok` | `:437` `@Capture` skip in `LenientFieldInfo.of` removed | `@Capture` is the **outer** adapter and hands its delegate only `knownObject`, and `discoverKnownKeys` excludes `@Capture` fields - so a `@Lenient` view of a `@Capture` field can never be handed its own key |

Repairs, and what each is now worth:

| Test | Repair | Re-verified |
| --- | --- | --- |
| `lenientNonObjectRootPassesThrough_ok` | assert `fromJson("null", ...)` returns `null`, which only `:157` can produce | **catches its mutation** |
| `lenientFieldShapeMismatchIsUnfiltered_ok` | assert the thrown message names `BEGIN_ARRAY`, i.e. the failure came from the delegate's own read | **catches its mutation** |
| `emptyLenientOverflowIsStillPublished_ok` | **renamed** `lenientFieldWithNoOverflowRoundTripsExactly_ok`; the comment now states the asymmetry has no executable guard rather than implying one | still survives, by design |
| `captureWinsOverLenientOnOneField_ok` | **renamed** `captureClaimsFieldCarryingBothAnnotations_ok`; the comment now records that the skip has no observable consequence | still survives, by design |

**Two commissioned guards cannot be built, and saying so is the honest result.** `00-verified-facts.md`
C1 (the unconditional-publish asymmetry) and C3 (the `@Capture`-wins skip) are both real library
contracts and both are **unobservable through the public API**. `20-implementation-order.md` §7 row 14
commissions a test for C1; it cannot be written. The two renamed tests pin what is actually
observable and no longer claim more.

### Two coverage gaps closed, and one that cannot be closed yet

- **`LenientTypeAdapterFactory:309-312` had zero coverage anywhere.** It is the enum-**value**
  compatibility guard and it is one of the four companion guards stage 7 edits. No model in the
  module or in the suite declared an enum-valued `@Lenient` map. Closed by
  `LenientTests.lenientEnumValuedOverflowIsLossless_ok`, and verified: mutating that predicate to
  `return true` - which is exactly the stage 7 regression - fails the test and nothing else.
- **`GsonSettings` SPI insertion.** `defaultFactoryOrderIsStable_ok` originally asserted the whole
  factory list including the SPI-appended `ConcurrentTypeAdapterFactory`, which would go red for a
  classpath change unrelated to registration order. Split into an exact assertion on the eight
  built-ins plus `spiFactoriesAreAppendedLast_ok`.
- **The stage 7 canary cannot be written at stage 1.** `20-implementation-order.md` §7 row 19 says
  `enumValuedOverflowIsLossless_ok` "is the test that catches the stage 7 companion-guard
  regression". It is not, and cannot be: the regression only fires for a **marked** enum, and
  `@Fallback` does not exist until stage 7. What row 19 does deliver is the unchanged-behaviour
  baseline for unmarked enums. The real canary is stage 7's own
  `markedEnumValuedCaptureOverflowStaysLossless_ok`, which the pack already commissions.

### A defect in a pre-existing test

`MemberDtoMappingTest.mapsCandyFestivals` derived its expected festival count from
`populated.player_stats.candy_collected` **one line after** decoding `player_stats` from that same
tree. Both sides of the comparison came from a tree the adapter is free to edit in place, so an
in-place key removal would shrink the expectation and the actual together and the assertion would
absorb the loss instead of failing. It is safe today only because `CandyCollected.festivals` is a
non-descend `@Capture` and so never reaches `CaptureTypeAdapterFactory:280`'s `rootObject.remove`.
Flipping that field to `descend = true` would drop every festival and leave the test green.

Fixed by reading the expectation from `rawPristine("player_stats")`. This is the only pre-existing
test changed, it is a one-line change, and it was made because the contamination mechanism above is
what created the hazard.

### Two tests whose round trip closed through the duplicate rather than through `@Extract`

`lenientExtractRoundTrip_ok` and the consumer's `roundTripsLoadouts` both re-decoded a serialized
document that still carried the root-level duplicate. Neither `FullCombinationModel.lastKilledMob`
nor `Loadouts.equippedArmorSet`/`equippedEquipmentSet` carries a `@SerializedName`, so gson's
reflective binder set them straight from those root keys - the re-decode proved nothing about the
`@Extract` claim. Both now strip the duplicate keys from the intermediate document before
re-decoding, which makes the claim the only surviving route today and is a no-op after stage 2.

`lenientExtractRoundTrip_ok` also deserves a note that is not a defect but changes what it proves.
On `7cfc181` the serialized intermediate carries the extracted value **twice** - inside `kills` and
again at the root under `lastKilledMob` - and the root key is a `@Capture` known key, so the
reflective binder can set the field without `@Extract` running at all on the second read. The two
routes are not independent (the root key exists only because the first read's extraction worked, so
mutation 2 still fails the test) but after stage 2 removes the duplicate the only surviving route is
the claim, and the test gets strictly stronger. It is worth re-reading at stage 2 rather than
treating a green result there as unchanged meaning.
