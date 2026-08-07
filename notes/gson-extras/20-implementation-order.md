# 20 - Implementation order

## 1. What this file is

An executable blueprint for the `gson-extras` work decided in `10-design-entries.md`, to be done
**before** the DTO research pack in `notes/json-annotations/`. It carries no design argument. Every
"why" in it is a pointer; every "what" in it is a step someone can run.

The five scoped items, and the design entry that owns each:

| # | Scope item | Design entry | Verdict | Category | Answers findings | Library change | Sites today | Effort |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Shared `Overflow` store, `@Extract` lifted into its own factory | `dgx-overflow-store` §2 | adopt | `correctness` | none directly - enabler for the next two | existing factory edit + two additive files | 0 | `large` |
| 2 | `@Extract` gains a `filter` element | `dgx-extract-filter` §3 | adopt narrowly | `correctness` | `f03-questrewards-mixed-values` | existing factory edit + one element | 1, up to 7 | `medium` |
| 3 | `@Lenient` consequences | inside §2 and §5 - no entry of its own | adopt | `correctness` | none | existing factory edit | 10 | folded |
| 4 | `@Capture` consequences, incl. the unmatched-enum-key fix | `dgx-capture-unmatched` §4 | adopt | `correctness` | `f06-capture-null-enum-key` | existing factory edit | 6 | `medium` |
| 5a | `@Fallback` | `dgx-fallback` §5 | adopt narrowly | `correctness` | `f06-enum-null-clobber`, `f03-enum-unknown-null`, `f04-enum-null-fallback` | three factory edits + one additive file | 14 behind 12 enums | `medium` |
| 5b | `@Flatten` | `dgx-flatten` §6 | adopt narrowly | `value-shape-collapse` | `f03-mapvalue-single-key` | two additive files + one registration line | 1 | `small` |

Scope items 3 and 4 have no separate cost line because they are consequences, not proposals. Item 3 is
`LenientTypeAdapterFactory` losing about ninety lines to item 1 and gaining two of item 5a's four
companion guards; it ships inside those two commits and never as one of its own. Item 4 is one
predicate clause and one build-time divert, and it is priced in its own entry.

**Two facts govern the shape of everything below.** `gson-extras` publishes by git sha through
JitPack, so there is no local snapshot loop and every library change costs a full publish-and-re-pin
cycle - which is why the stage list is organised around cycles rather than around files. And the
workspace composite substitutes `com.github.simplified-dev:gson-extras` onto the local project, so a
build launched from `W:/Workspace/Java/Simplified` compiles and tests **every** consumer against the
`gson-extras` working tree with no push at all. Per-stage verification therefore costs no cycle. The
only thing a cycle buys is the binary-compatibility pass against the published sibling jars.

## 2. What it supersedes in the DTO plan

`notes/json-annotations/20-implementation-plan.md` §12, stage 9, `s20-library-cycle`, is **deleted and
replaced by this file**. That stage sat last in the DTO plan and carried one JitPack publish with
three payloads. The owner reversed the order so the DTO stages can consume the library result rather
than wait behind it, and the five-item scope here is larger than what stage 9 carried.

| `s20-library-cycle` payload | Where it goes now |
| --- | --- |
| `@Fallback` - `Fallback.java` plus the `CaseInsensitiveEnumTypeAdapterFactory` edit | **Stage 7** below, plus four companion guards stage 9 did not know about |
| `@Flatten` - `Flatten.java`, `FlattenTypeAdapterFactory.java`, one `GsonSettings` line | **Stage 8** below, at registration index 5 rather than stage 9's index 3 |
| `PostInitTypeAdapterFactory` null-guard and log, plus the `PostInit.java` javadoc rewrite | **Not in this file's scope.** See below |
| The consumer half - enum markers plus `Currencies.essence` | **Stage 10** below, on a corrected adoption list |

Four things stage 9 asserted are corrected here rather than carried:

- **The registration index for `@Flatten`.** Stage 9 says index 3, between `Split` and
  `SerializedPath`. At that depth `@Flatten` nests **inner** to `Lenient`, which sees the uncollapsed
  wrappers, judges every one incompatible and diverts the whole field to overflow before `@Flatten` is
  consulted. `10-design-entries.md` §6.5 moves it to index 5.
- **The enum adoption list.** Stage 9 says "roughly a dozen one-line enum edits". Eight of the twelve
  eligible enums have no sentinel constant to mark, and `Kuudra.Tier.BASIC` - stage 9's named example -
  is wire-visible via `@SerializedName("NONE")`, so marking it makes an unknown tier overwrite a
  correct entry. `10-design-entries.md` §5.1 and §5.5 own the corrected list.
- **`@Fallback` alone does not fix `f06-capture-null-enum-key`.** Stage 9 assumed one marker served
  both the field-value path and the map-key path. It reaches the map-key path and does no harm there;
  the fix is scope item 4, in stage 4 below.
- **`@Flatten` does not preserve round-trip fidelity.** Stage 9's verify step asks for a byte-equal
  `Currencies` round trip. `10-design-entries.md` §6.6 reverses that claim - a wrapper carrying a
  sibling member reads fine and serializes back without it - and the fixture assertion in stage 8 is
  written to pin the loss as a declared contract rather than to assert an equality that will not hold.

**The `PostInit` residue, stated so it is not lost.** Stage 9's third payload - null-guarding `obj` and
logging the swallowed exception at `PostInitTypeAdapterFactory.java`:35-39, plus the `PostInit.java`
javadoc rewrite - is outside the five scoped items and is not designed here. It is a library edit, so
on its own it costs a whole cycle for two files. The cheap move is to let it ride **stage 8's commit**
inside cycle 2, where a publish is happening anyway; the alternative is that the DTO pack keeps one
library cycle purely for it. That is an owner decision (§22) and this file does not take it. Note the
dependency the DTO plan already records and that does not go away: turning the log on before
`s20-dark-feature-fixes` lands produces one warning per member per request, because `Bestiary` and
`AccessoryBag` throw on every decode today.

The rest of the DTO plan is unaffected in content. What changes is that stages 1 to 8 and 10 now run
**after** this file rather than before it, and several of them gain capability they did not have. §19
lists exactly which.

## 3. Stage map and the three JitPack cycles

Twelve stages, three cycles, **two re-pins**. Execution order is top to bottom and only the four
dependencies named in the last column are real.

| # | Stage id | Repo | Commits | Cycle | Re-pin | Effort | Depends on |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | `sgx-baseline` | both | **none** | - | no | `trivial` | none |
| 1 | `sgx-characterisation` | both | 2 - one per repo | 0 | **no** | `medium` | 0 |
| 2 | `sgx-overflow-store` | gson-extras | 1 | 1 | no | `large` | 1 |
| 3 | `sgx-extract-filter` | gson-extras | 1 | 1 | no | `medium` | 2 |
| 4 | `sgx-capture-unmatched` | gson-extras | 1 | 1 | no | `medium` | 2, 3 |
| 5 | `sgx-publish-one` | both | 1 - the pin edit | **1 publishes** | **yes** | `small` | 4 |
| 6 | `sgx-consumer-overflow` | hypixel | 1-3 | - | no | `small` | 5 |
| 7 | `sgx-fallback` | gson-extras | 1 | 2 | no | `medium` | 4 |
| 8 | `sgx-flatten` | gson-extras | 1 | 2 | no | `small` | 7 |
| 9 | `sgx-publish-two` | both | 1 - the pin edit | **2 publishes** | **yes** | `small` | 8 |
| 10 | `sgx-consumer-additive` | hypixel | 2 | - | no | `medium` | 9, plus the three naming fixes |
| 11 | `sgx-sibling-convergence` | 12 modules | 12 | - | 12 pins | `medium` | 9. Optional |

**Which stages share a cycle, and why.**

| Cycle | Stages | Publishes | Re-pins | Why these are one cycle |
| --- | --- | --- | --- | --- |
| **0** | 1 | yes, a build for the sha | **no** | Tests are not published. A test-only commit costs a build and zero re-pins, and it is what makes every later green run mean something. Shipping it as its own cycle is what buys the "characterisation passes on `7cfc181`" claim |
| **1** | 2, 3, 4 | yes | yes, at stage 5 | They share the store and cannot be separated at the pin boundary. Stage 2 has **zero adoption sites on its own** and is unverifiable end to end; stage 3 is what makes it verifiable; stage 4 is unreadable without both. Three commits, one publish |
| **2** | 7, 8 | yes | yes, at stage 9 | Behaviourally independent of cycle 1 and of each other, and separately revertable in reverse order. **Not file-disjoint from cycle 1** - stage 7's four companion guards edit `CaptureTypeAdapterFactory` and `LenientTypeAdapterFactory`, and one of them is the exact predicate stage 4 rewrites - so cycle 2 is authored **on top of** cycle 1's tree and neither of its commits can be cherry-picked alone |

**Which stages need their own cycle, and which do not.**

- **Stages 2, 3 and 4 must not have their own cycles.** Not because they are cheap, but because
  per-stage verification already happens through the composite at zero cycle cost, so a separate cycle
  buys nothing except an unpinnable intermediate sha. Stage 2 alone would publish a sha with no
  consumer-visible behaviour change and a `large` regression surface.
- **Stage 1 must have its own cycle**, or it does not exist as a checkpoint. Merging it into cycle 1
  means the characterisation tests and the change they characterise land on the same sha, and a test
  written after the change pins the new behaviour and proves nothing.
- **Stages 7 and 8 could fold into cycle 1** for one re-pin instead of two. The argument for keeping
  them apart is that a red hypixel run at stage 5 is then unambiguously the overflow work. Take the
  fold only if the owner accepts hand-attributing a red run across five items.
- **Stage 8 must be its own commit even inside cycle 2.** Folding `@Flatten` into the `@Fallback`
  commit makes a revert surgical rather than a `git revert`, and `@Flatten` is the lowest-value item in
  the cycle and the one most easily dropped if the cycle is cut short.
- **Stages 6 and 10 need no cycle at all.** They are consumer-only.
- **Stage 11 is twelve re-pins and no publish.** It is optional and it is an owner decision (§22).

**The one ordering guarantee this work introduces, and it is enforced by nothing.**
`ExtractTypeAdapterFactory` **must** nest outside `CaptureTypeAdapterFactory`. A factory registered
between them, or any downstream SPI or `GsonContributor` factory - which land outside the whole list -
silently reduces `@Extract` to its current capability with **no test failure**, because all six
existing sites are `@Lenient`-sourced and keep passing. That is what makes stage 2 `large` rather than
`medium`, and it is why `defaultFactoryOrderIsStable_ok` is not optional garnish.

The composed factory list both cycles are working toward, derived once in `10-design-entries.md` §7.
Registration index runs **opposite** to nesting depth - the last registered factory is the outermost:

| Index | Factory | Depth, 1 = outermost | Stage |
| --- | --- | --- | --- |
| 0 | `CaseInsensitiveEnumTypeAdapterFactory` | 10 | edited at 7 |
| 1 | `OptionalTypeAdapterFactory` | 9 | - |
| 2 | `SplitTypeAdapterFactory` | 8 | - |
| 3 | `SerializedPathTypeAdaptorFactory` | 7 | - |
| 4 | `LenientTypeAdapterFactory` | 6 | edited at 2, 7 |
| 5 | `FlattenTypeAdapterFactory` | 5 | **new at 8** |
| 6 | `CaptureTypeAdapterFactory` | 4 | edited at 2, 4, 7 |
| 7 | `ExtractTypeAdapterFactory` | 3 | **new at 2** |
| 8 | `CollapseTypeAdapterFactory` | 2 | - |
| 9 | `PostInitTypeAdapterFactory` | 1 | - |

Insertion preserves the relative order of every existing factory, so no existing pair is reordered.
It does shift four indices with no test signal today, which is why stage 1 ships the order test.

## 4. The cadence for one cycle

Seven steps. The order is not optional and no step is skippable. Everything is a `toolsmith` MCP tool
or a `toolsmith` CLI call; **never hand-roll the `curl .../api/builds/<version>` round** - the
per-version endpoint silently **starts** a build, so every retry is a real build on a third-party
service.

```
# 1  in W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras
git commit                                   # one commit per stage, never a squash across stages
git push                                     # JitPack cannot see an unpushed sha

# 2  read-only pre-flight
jitpack_status gson-extras                   # confirms the sha is pushed and unambiguous
                                             # baseline: 28 records, 26 ok, 2 error, 0 in flight

# 3  publish
jitpack_build gson-extras                    # triggers exactly ONE build and waits
jitpack_status gson-extras                   # read the resulting sha and its state

# 4  workspace-wide pin drift, read-only
toolsmith jitpack pins

# 5  in W:/Workspace/Java/Simplified/Simplified-Api/hypixel
#    build.gradle.kts:44
#    api("com.github.simplified-dev:gson-extras") { version { strictly("<new sha>") } }

# 6  prove the library change is behaviour-neutral BEFORE any consuming edit.
#    Run STANDALONE in the hypixel directory - this is the only configuration that
#    exercises the published sibling jars.
gradle_verify hypixel compileJava test --rerun
test_tally hypixel                           # must read 16/16, or 16 + whatever stage 1 added
py -3 scripts/json_dto_diff.py > /tmp/diff-after.txt
diff /tmp/diff-baseline.txt /tmp/diff-after.txt

# 7  only now, the consuming edits
```

**Step 6 is the step people skip and it is the one that makes the two halves separately revertable.**
At that point no DTO has changed, so a red build is unambiguously the library's fault.

**Standalone, not composite, and the distinction is load-bearing.** `skyblock`, `client`, `github` and
`persistence` reach hypixel as **jars** compiled against four different older `gson-extras` shas, and
hypixel's `strictly` force-upgrades all four to the new one. Only a standalone post-re-pin run can
surface a `NoSuchMethodError` or `NoClassDefFoundError` from that force-upgrade. Binary
incompatibility is invisible to `compileJava` and invisible to the composite, which recompiles every
sibling from source and therefore never sees it. This is the single reason a cycle exists at all.

**Rollback of a cycle is two repositories and the order is not optional.** Revert the **pin edit** in
every consuming module first, then the `gson-extras` commit. The published sha stays on JitPack and is
harmless once nothing references it. Never revert the library commit while a module is still pinned to
it. If only the consumer half is wrong, revert step 7 and leave the pin - the library change is inert
without the annotations.

## 5. The test-first rule, and how each test is validated

Two existing test classes were written this way and each was validated by stashing the source fix and
confirming the tests failed: `factory/CollectionValueCompatibilityTest.java` (5 tests, the `c944987`
regression set) and `factory/CaptureGroupingModeTest.java` (6 tests, the `b071689`/`7cfc181` set).
**Every library change in this file owes the same validation.** Two forms, and which form a test takes
is decided when it is written, not afterwards.

**Form A - characterisation.** Green before the change, green after. Pins behaviour the change must
not alter. Validation is that it passes on the pre-edit tree; there is nothing to stash.

**Form B - defect fix.** Red before the change, green after. Validation is mechanical:

```
# with the test written and the source fix already applied in the working tree
git -C W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras stash push -- src/main/java
gradle_verify gson-extras test          # MUST fail, and MUST fail on the named test only
git -C W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras stash pop
gradle_verify gson-extras test          # MUST pass
```

Record the observed failure message in the commit body. A form-B test that stays green with the source
stashed is not testing the fix and must be rewritten before the commit lands.

**How form B survives the green-at-every-step requirement.** A form-B test written at stage 1 is red on
`7cfc181` by construction, which would leave the suite red at a stage boundary. The resolution is
mechanical and it preserves both properties:

1. Write the test at stage 1. Run it. **Observe the red** - that observation is the validation, and it
   happens once, locally, before the commit.
2. Commit it with `@Disabled` carrying the observed failure as the reason string.
3. The stage that fixes the defect **removes the `@Disabled` in the same commit as the fix**, and
   validates by form B above.

The stage-1 suite is therefore green with N skips, and each later stage converts skips to passes. The
skip count is part of the tally assertion at every stage boundary, exactly like the pass count.

**A green suite is not evidence, and this is why the rule exists.** `LenientTypeAdapter.write` -
all 65 lines, `:86-150` - is unexecuted by all 134 tests. `@Extract` appears in exactly one test file
(`GsonFactoryTest.java` `:14`, `:2214-2224`, `:2227-2253`) and that test only deserializes. There is no
`LenientTests` nest. hypixel's `MemberDtoMappingTest` has 16 tests and **none calls `toJson`**. So the
workspace has **zero** round-trip coverage for `@Lenient`/`@Extract` at any level, and stage 2
relocates precisely that code. "134/134 and 16/16 after the change" is compatible with all six
`@Extract` sites having lost round-trip fidelity, both stores leaking orphan entries, and every
unmatched enum key still collapsing onto `null`.

**The proof that this is not theoretical.** Writing the serialize tests stage 1 requires is what
surfaced the duplicate-key defect: `@Extract` has never removed its own field's serialized key from
the output, so **all six sites emit their extracted value twice on every serialize**, today, on
`7cfc181`. It is visible in the first byte of any serialize assertion, and 134 green library tests plus
16 green hypixel tests never saw it.

## 6. Stage 0 - `sgx-baseline`

**Goal.** Capture every number the rest of the file compares against. No commit, no edit, no cycle.
Ten minutes, and skipping it makes every later "unchanged" claim unfalsifiable.

**Files touched.** None. Outputs land in `/tmp`.

**New tests.** None.

**Verify - this stage is entirely verification.**

```
gradle_verify gson-extras compileJava test --rerun     # expect exit 0
test_tally gson-extras                                 # expect 134/134
gradle_verify hypixel compileJava test --rerun         # expect exit 0
test_tally hypixel                                     # expect 16/16

cd W:/Workspace/Java/Simplified/Simplified-Api/hypixel
py -3 scripts/json_dto_diff.py > /tmp/diff-baseline.txt
# expect 792 unmapped keys, all under `objectives`, exit 1

./gradlew -q --console=plain dependencyInsight \
    --configuration compileClasspath --dependency gson-extras > /tmp/pins-baseline.txt
# expect four forced upgrades: client, github, skyblock, persistence

cd W:/Workspace/Java/Simplified
./gradlew --console=plain :Simplified-Api:hypixel:compileTestJava \
    :Simplified-Api:skyblock:compileJava :Simplified-Dev:persistence:compileJava \
    :Simplified-Dev:dataflow:compileJava :Simplified-Dev:client:compileJava \
    :Minecraft-Library:asset-renderer:compileJava
# expect exit 0 - the source-compatibility baseline

git -C W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras rev-parse --short HEAD   # 7cfc181
jitpack_status gson-extras                             # 28 records, 26 ok, 2 error, 0 in flight
```

Three things about these baselines that are easy to get wrong:

- **`python` is not on PATH in this environment; the launcher is `py -3`.**
- **`json_dto_diff.py` already exits 1 today**, with 792 unmapped keys. It is not a green gate. Using
  it means diffing `/tmp/diff-baseline.txt` against a later run, not checking the exit code.
- **The composite compile is the cross-module gate and it costs no cycle.** Every sibling substitutes
  to a local project, so this one build is a full-workspace **source**-compatibility check. It is what
  catches a `dataflow` break - `serde/PipelineGson.java` constructs `PostInitTypeAdapterFactory` and
  `CaseInsensitiveEnumTypeAdapterFactory` by hand, so any signature or visibility change to either
  compile-breaks it. Neither is touched by stages 2 to 4; stage 7 edits the second one's body only.

**Rollback.** Nothing to roll back.

**Estimate.** AI-assisted elapsed **15-25 minutes**, dominated by three `--rerun` builds;
human-developer **30-45 minutes**.

## 7. Stage 1 - `sgx-characterisation`

**Goal.** Close the coverage gap the redesign is aimed at, **before** any source edit, so that "green
after" becomes evidence instead of noise. This is cycle 0: a push and a JitPack build, **no re-pin**,
because tests are not published.

**Effort.** `medium`. Nineteen library tests and three consumer tests, all cheap individually because
the models already exist, and collectively the precondition for trusting every later run.

**Files touched.**

| Repo | File | Change |
| --- | --- | --- |
| gson-extras | `src/test/java/dev/simplified/gson/GsonFactoryTest.java` | new `LenientTests` nest - the suite has none - and a new `ExtractTests` nest |
| gson-extras | `src/test/java/dev/simplified/gson/factory/CollectionValueCompatibilityTest.java` | one write-side case on the existing `LenientToolkit` model `:107-109` |
| gson-extras | `src/test/java/dev/simplified/gson/GsonSettingsPrewarmTest.java` | the factory-order assertion |
| hypixel | `src/test/java/api/simplified/hypixel/response/skyblock/MemberDtoMappingTest.java` | three tests |

**No `src/main` file is touched at this stage.** That is the whole point of it.

**New tests required in `gson-extras`.** Form A unless marked. The `@Disabled` rows are the form-B
tests whose red is observed here and whose enable lands with the fix.

| # | Test | Form | Asserts | Closes | Enabled at |
| --- | --- | --- | --- | --- | --- |
| 1 | `extractReinjectsOnWrite_ok` | A | round-trip `FullCombinationModel` `:2214`; `kills.last_killed_mob` is present in the output | G2 | now |
| 2 | `extractFieldIsNotEmittedAtRoot_ok` | **B, `@Disabled`** | the root carries **no** `lastKilledMob` key. Red on `7cfc181` - this is the duplicate-emission defect | W1a | stage 2 |
| 3 | `lenientOverflowMergesBackOnWrite_ok` | A | `LenientWithCaptureModel` `:2175` with an incompatible entry; after `toJson` the entry is back inside the `stats` sub-object | G1, G3 object branch | now |
| 4 | `lenientCollectionOverflowMergesBackOnWrite_ok` | A | `LenientToolkit`; the `JsonArray` branch `:142-143` | G3 array branch | now |
| 5 | `lenientExtractRoundTrip_ok` | A | `fromJson` -> `toJson` -> `fromJson`; the second object equals the first including `lastKilledMob` | G1-G3 | now |
| 6 | `twoExtractsReturnToOwnSources_ok` | A | a `Loadouts`-shaped model with two `@Extract` fields off two `@Lenient` maps; each claim returns to its own source's sub-object | - | now |
| 7 | `twoExtractsNotEmittedAtRoot_ok` | **B, `@Disabled`** | neither field name appears at the root | W1a | stage 2 |
| 8 | `serializedPathLenientSourceRoundTrip_ok` | A | merge-back still resolves through `locateElement`'s segment branch `:340-350` | single-site path | now |
| 9 | `extractOnHandBuiltObjectReachesDocument_ok` | A | serialize an object that was never read; the entry still reaches the document | - | now |
| 10 | `extractMutationReachesDocument_ok` | A | read, mutate the extracted value, serialize; the mutation reaches the document | E2 | now |
| 11 | `captureOverflowMergesBackOnWrite_ok` | A | the `typeFilteredCapture_ok` `:642` input; after `toJson`, `"invalid": "not_an_int"` is back at the **root** | G5 | now |
| 12 | `lenientAndCaptureOverflowGoToDifferentTargets_ok` | A | one model with both; the `@Lenient` entry lands in the sub-object and the `@Capture` entry at the root | G5 + G3 | now |
| 13 | `extractConversionFailureLeavesInitialiser_ok` | A | pins the empty catch `:246-247` as current behaviour so changing it is a decision | G7 | now |
| 14 | `emptyLenientOverflowIsStillPublished_ok` | A | the `:236-239` versus `:386` publish asymmetry | G11 | now |
| 15 | `lenientNonObjectRootPassesThrough_ok` | A | a JSON array against a `@Lenient`-carrying type | G12 | now |
| 16 | `captureWinsOverLenientOnOneField_ok` | A | `LenientFieldInfo.of` `:437-438` makes `@Capture` win and `@Lenient` dead. Pins it as intended | G16 | now |
| 17 | `defaultFactoryOrderIsStable_ok` | A | the **exact class list and order** of `GsonSettings.defaults()`. Turns an index shift from silent into loud | G14 | now, updated at 2 and 8 |
| 18 | `unmatchedEnumKeyDoesNotCollapse_ok` | **B, `@Disabled`** | `EnumKeyCaptureModel` `:738` with two unmatched keys; the map has no `null` key and loses no entry. Red - it is `{null=4, BASIC=1}` today | G6 | stage 4 |
| 19 | `enumValuedOverflowIsLossless_ok` | A | an enum-**valued** `@Capture` map with an unrecognized value keeps it in overflow and round-trips it. Green today; **this is the test that catches the stage 7 companion-guard regression** | - | now |

**New tests required in hypixel.** All form A, all green on `7cfc181`.

| # | Test | Asserts |
| --- | --- | --- |
| 20 | `roundTripsLoadouts` | decode `loadout`, `toJson`, re-decode; `equippedArmorSet`, `equippedEquipmentSet` and `armorSets` key 1 survive. `Loadouts` is the densest `@Lenient`/`@Extract` class and the only one the suite already pins |
| 21 | `mapsBestiary` | `Bestiary` is never decoded today and carries one `@Extract` and two `@Lenient`. Decode the **subtree**, not `SkyBlockMember` - the latter's `postInit` needs a live JPA session |
| 22 | `mapsDungeonsUnlockedJournals` | `Dungeons.unlockedJournals` is the **only** collection-shaped `@Lenient` field in the workspace and no test in either module decodes it |

**Verify.**

```
gradle_verify gson-extras compileJava test --rerun
test_tally gson-extras          # expect 153 passed + 3 skipped (134 + 19, three disabled)
gradle_verify hypixel compileJava test --rerun
test_tally hypixel              # expect 19/19
```

Then the cycle-0 publish: commit, push, `jitpack_status gson-extras`, `jitpack_build gson-extras`.
**Do not edit `build.gradle.kts`:44.** There is nothing in the sha a consumer needs.

**Rollback.** `git revert` the test commit in each repo. There is no pin to unwind and no consumer to
re-verify. This is the only stage in the file with a single-repository rollback.

**Estimate.** AI-assisted elapsed **3-4 hours** - nineteen tests against models that exist, but each
form-B row needs its red observed and its message recorded, and rows 11, 12 and 19 need new models;
human-developer **1.5-2 days**.

## 8. Stage 2 - `sgx-overflow-store`

**Goal.** Replace the two independent static overflow stores with one per-entry-tagged `Overflow`, and
move `@Extract`'s read and write phases out of `LenientTypeAdapterFactory` into a new
`ExtractTypeAdapterFactory` nested **outside** `CaptureTypeAdapterFactory`. Cycle 1, commit 1 of 3.

**Effort.** `large`. Not by file count - two new files, three edited, two new test classes, zero
consumer files reads `medium` - but by the clause: it introduces **a new ordering guarantee between
factories**, and that is what `00-conventions.md` §4 prices at `large`.

**Files touched.**

| File | Change |
| --- | --- |
| `factory/Overflow.java` | **new.** The shared store: a `WeakIdentityMap` keyed by bound-container identity, values tagged per entry with a `Target` - `FIELD_ELEMENT` for `@Lenient`, `SOURCE_OBJECT` for `@Capture`. Operations `publish`, `find`, `open`, `claim`, `restore` |
| `factory/ExtractTypeAdapterFactory.java` | **new.** `ExtractTypeAdapter`, plus the `ExtractClaim` record and `ExtractFieldInfo` relocated from `LenientTypeAdapterFactory` `:377`, `:456-496` |
| `factory/LenientTypeAdapterFactory.java` | **-~90 lines** across five regions: `:62` store deleted; `:68`,`:70-72` `create` scans `@Lenient` only; `:98-118` write-side re-injection deleted; `:127` and `:239` route through `Overflow`; `:203-221` read-side extract phase deleted; `:242-248` post-assign deleted **including the empty catch at `:246-247`** |
| `factory/CaptureTypeAdapterFactory.java` | **-3 lines.** `:82` store deleted; `:239` `OVERFLOW.get` becomes `Overflow.find(mapObj, Target.SOURCE_OBJECT)`; `:386-387` publish routes through `Overflow.publish` |
| `GsonSettings.java` | one line at index 7, plus the factory list in the class javadoc `:215-218` |

**What deliberately does not change.** The `FieldOverflow` record and the whole `@Lenient` filter
phase. The two **publish policies** stay divergent - `@Lenient` publishes unconditionally even when
empty `:236-239`, `@Capture` only when non-empty `:386` - because unifying either way changes
behaviour a consumer can observe. `LenientTypeAdapterFactory.create`'s signature does not move,
`WeakIdentityMap` stays package-private, and `dataflow`'s two direct factory references are untouched.
**Zero consumer source edits.** All six `@Extract` sites are byte-identical after this stage.

**New tests required in `gson-extras`.** Beyond enabling stage 1 rows 2 and 7.

| Test class | Covers | Form |
| --- | --- | --- |
| `factory/OverflowTest.java` | `publish` / `find` / `open` / `claim` / `restore`; both `Target` values; **target mismatch returns `null`**; `claim` on an array-shaped entry returns nothing; identity keying survives a re-publish | A - new surface |
| `GsonFactoryTest.ExtractTests` | a `@Lenient`-sourced claim and a `@Capture`-sourced claim; the `create`-time rejection for each of the three no-op rows, **if** the owner takes them (§22) | mixed |
| `GsonFactoryTest.CombinationTests` | two rows: `extractOverCaptureSource_ok` and `extractNestedInsideCapture_ok` - the pair that fails if the ordering guarantee is ever violated | B against a deliberately mis-ordered `GsonSettings` |
| existing `defaultFactoryOrderIsStable_ok` | updated to the ten-entry list with `ExtractTypeAdapterFactory` at index 7 | A |

The `@Capture`-sourced claim test is the one that did not exist before and could not: **no `@Extract`
in the workspace names a `@Capture` field**, because until this stage doing so is a silent no-op.

**Verify.**

```
gradle_verify gson-extras compileJava test
test_tally gson-extras     # expect 134 + 19 + new, and ZERO skips lost - a DROP in count means a
                           # test silently stopped being discovered; the tally is not just pass/fail

# named re-runs, in full and not selectively - inserting a factory shifts every later index
#   GsonFactoryTest$CombinationTests   all 9, the only place the library observes nesting
#   GsonFactoryTest$CaptureTests       all 26
#   CaptureGroupingModeTest            all 6
#   CollectionValueCompatibilityTest   all 5
#   WeakIdentityMapTest                all 5 - Overflow becomes its only production caller
#   GsonSettingsPrewarmTest            all 5 - the path a create-time JsonException takes

# cross-module source compatibility, costs no cycle
cd W:/Workspace/Java/Simplified
./gradlew --console=plain :Simplified-Api:hypixel:compileTestJava \
    :Simplified-Api:skyblock:compileJava :Simplified-Dev:persistence:compileJava \
    :Simplified-Dev:dataflow:compileJava :Simplified-Dev:client:compileJava \
    :Minecraft-Library:asset-renderer:compileJava

# hypixel's 22 against the gson-extras WORKING TREE, before any push
./gradlew --console=plain :Simplified-Api:hypixel:test
```

Then print the resulting `GsonSettings.defaults()` list and confirm the intended nesting **depth**, not
the index.

**The one intended behaviour change, and it is the only thing that may differ.** All six `@Extract`
sites stop emitting their extracted value twice. Today's output minus the root duplicate; everything
the input carried is byte-identical and in the same place. Stage 1 rows 2 and 7 are exactly this.

**Rollback.** `git revert` the commit. Nothing outside `gson-extras` has changed and no pin exists yet,
so this is a single-repository revert with no consumer re-verify. That property is why stages 2, 3 and
4 are three commits rather than one, even though they share a cycle.

**Estimate.** AI-assisted elapsed **4-6 hours**; human-developer **3-4 days**. `large` may be
optimistic on the human column: roughly ninety lines come out of five separate regions of
`LenientTypeAdapterFactory`, the library's second-most-used file, and the new factory has to re-derive
the source field's `isMap()` and re-scan `@Lenient` fields with **exactly** the gates
`LenientFieldInfo.of` `:437-445` uses - it skips `transient`, `@Capture`-carrying and
non-`ParameterizedType` fields, and a scan with different gates installs orphan entries into a
`WeakIdentityMap` that has no `remove`.

## 9. Stage 3 - `sgx-extract-filter`

**Goal.** Give `@Extract` a selection axis. Today it addresses a single named key inside a named source
field; after this stage a **dotless** `value()` means "claim the remainder of that source", and an
optional `filter()` selects within it. Cycle 1, commit 2 of 3.

**Effort.** `medium` - `00-conventions.md` §4's price for "adds an element to an existing annotation".
It rides stage 2's cycle and costs no second re-pin, and it **must** ship in that cycle: stage 2 has no
adoption sites of its own and this element is what makes it verifiable end to end.

**Files touched.**

| File | Change |
| --- | --- |
| `annotation/Extract.java` | one new element, `String filter() default ""`, with `@return` on its accessor javadoc |
| `factory/ExtractTypeAdapterFactory.java` | `ExtractFieldInfo` gains `boolean remainder` and `@Nullable Pattern pattern` plus a `matches(String)`; `ExtractFieldInfo.of` gains the three-band sort and the two `create`-time rejections; `read` gains the `Overflow.claim(owner, info::matches)` branch; `write` gains the remainder re-injection branch |

**Mode is selected by the dot in `value()`, not by the presence of `filter()`.** `remainder` is
`path.indexOf('.') < 1`. A dotted value means single key and `filter()` must be empty; a dotless value
means remainder, and inside remainder mode an empty `filter()` is the catch-all - which mirrors
`@Capture`, where an empty filter is also the catch-all. Selecting on the presence of `filter()`
instead would make "empty filter" mean *exact key*, the inverted meaning, in the same codebase.

**Two cuts, both reversible additions later.** Selection **without stripping** - `@Capture` strips and
reconstructs via `literalPrefix`, and duplicating that doubles the places a regex with real
metacharacters silently fails to round-trip, and a `@Capture`-sourced claim would double-prefix
because `@Capture` deliberately stores overflow under the **original unstripped** key. And **no
remainder over an array-shaped overflow** - `Overflow.claim` returns nothing for an array-shaped
entry in either overload, which preserves today's silent no-op exactly.

**New tests required in `gson-extras`.** All in the `ExtractTests` nest stage 2 created.

| Test | Form | Asserts |
| --- | --- | --- |
| `extractRemainderFromLenientSource_ok` | A - new surface | a dotless `@Extract` claims every unclaimed entry of a `@Lenient` source into a typed map |
| `extractFilteredRemainder_ok` | A | `filter` selects by `Pattern.matcher(key).find()` - **`find()`, not `matches()`**, the same semantics `CaptureTypeAdapterFactory.java`:327 uses |
| `extractRemainderKeysAreNotStripped_ok` | A | the claimed keys carry their original prefix |
| `extractSingleKeyAndRemainderCoexist_ok` | A | band 0 before band 1 before band 2: an exact `@Extract` and a catch-all remainder on one source, in either declaration order, produce the same result |
| `extractRemainderRoundTrip_ok` | A | every claimed entry is re-injected into the **source's own** merge target, not the root |
| `extractRemainderFromCaptureSource_ok` | A | the `Target.SOURCE_OBJECT` half: claimed from a `@Capture` source, restored to the root |
| `extractRemainderConversionFailureRestoresAll_ok` | A | conversion is all-or-nothing; one unconvertible value leaves the field at its initialiser and puts **every** claimed entry back |
| `extractFilterOnDottedValue_throws` | B | `JsonException` at `create` |
| `extractTwoCatchAllRemaindersOnOneSource_throws` | B | `JsonException` at `create` - detectable, unlike two overlapping filtered remainders, which stay a documented hazard |
| `extractRemainderOverArrayOverflow_isNoOp` | A | pins the array cut as deliberate |

**Verify.** Identical to stage 2's block, plus: `gradle_verify gson-extras compileJava test`,
`test_tally gson-extras`, the named re-runs, the composite compile and
`./gradlew :Simplified-Api:hypixel:test` from the workspace root.

**No consumer edit at this stage.** The first `@Extract(value = ..., filter = ...)` site lands at
stage 6, and it carries a mandatory companion (§12).

**Rollback.** `git revert` the commit. It is additive at the annotation surface - the element has a
default, so it is both source and binary compatible - and stage 2 stands without it, minus its only
end-to-end verification.

**Estimate.** AI-assisted elapsed **2-3 hours**; human-developer **1-1.5 days**.

## 10. Stage 4 - `sgx-capture-unmatched`

**Goal.** Stop `@Capture` binding an unmatched enum key onto `null`. Six maps narrow an open JSON key
space onto a closed enum with no failure policy, and every unmatched key in a field collapses onto the
same `null` with last-write-wins. Proved by probe: `{null=4, BASIC=1}` - four distinct upstream keys
produced one entry. **The loss is N-1 values per field.** Cycle 1, commit 3 of 3.

**Effort.** `medium` - "modifies an existing factory's read path", plus a regression pass over all
sixteen live `@Capture` fields. **There is no new element.** `Capture.java` changes by one javadoc
paragraph and nothing else: no `skipUnmatchedKeys`, no policy enum, no new default to reason about at
seventeen existing sites.

**Files touched.**

| File | Change |
| --- | --- |
| `factory/CaptureTypeAdapterFactory.java` | **Branch A** - `isCompatibleCaptureEntry` `:490-494` becomes "compatible only if the conversion neither throws **nor yields `null`**". **Branch B** - `buildGroupedMap` `:474` diverts a group whose key failed, via a new `divertGroup` helper and a `groupSources` frame-local filled alongside `groups` at `:426`, `:441`, `:455`/`:466`; the `:384` overflow fetch moves **above** the build calls at `:377`/`:379` so the `:386` publish gate sees the diverted entries |
| `annotation/Capture.java` | one javadoc paragraph stating the diversion |

**No registration slot and no index shift.** `CaptureTypeAdapterFactory` stays exactly where it is.
The ordering this stage depends on is entirely stage 2's.

**Why the existing type check does not catch it.** Key conversion splits three ways. A `String` key
skips the check outright. An `Integer` key **throws** on a non-numeric string, so `:490-494` catches
it, returns `false`, and the entry lands in overflow - correct, lossless, round-trips, and seven sites
already behave this way. An `enum` key returns `nameToConstant.get(...)`, which is **`null` for an
unmatched name and throws nothing**. The predicate asks "did the conversion throw", and for enums that
question has the wrong answer.

**Reach, per site.** Branch A reaches the four entry-mode sites - `Dojo.points` `:15`, `Dojo.times`
`:17`, `Kuudra.highestWave` `:18`, `Kuudra.completedTiers` `:20`. Branch B is needed for the two
grouping-mode sites - `TrophyFishing.fish` `:24`, `HeartOfTheMountain.powder` `:49` - because grouping
mode skips the compatibility check entirely at `:332-334` and `:355` and therefore produces no overflow
at any point.

**One narrowing available (§22).** Branch B may divert **only** in the `key == null` case, leaving the
`:477-478` catch empty. It still reaches both grouping sites. Filling that catch as well diverts a
group whose **value** fails conversion too, which is strictly better - it is the treatment entry mode
already gives an incompatible value, and it gives a body to one of the library's five silent swallows.
Take the narrowing if the cycle wants the smallest diff in the library's busiest factory.

**New tests required in `gson-extras`.** Plus enabling stage 1 row 18.

| Test | Form | Asserts |
| --- | --- | --- |
| `unmatchedEnumKeyDivertsToOverflow_ok` | B | entry mode: `{"none":1,"brand_new_tier":4,"another_new_tier":2}` binds `{BASIC=1}` and overflows the other two under their **original unstripped** keys |
| `unmatchedEnumKeyRoundTrips_ok` | B | all three keys come back byte-exact on `toJson`. This is the half stage 1 row 18 does not cover |
| `unmatchedGroupKeyDivertsToOverflow_ok` | B | grouping mode: `literalPrefix + strippedKey` reconstruction is byte-exact - `powder_` + `mithril_total`, empty prefix for a catch-all |
| `unmatchedGroupKeyPublishesOverflow_ok` | B | the `:384` fetch reorder - without it a grouping-mode field diverts into an object nobody publishes |
| `unmatchedEnumKeyIsClaimableByExtract_ok` | A - new surface | the diverted entries are readable through a dotless `@Extract`. **This is the test that proves stages 2, 3 and 4 are one change** |
| `matchedEnumKeysUnchanged_ok` | A | `filterWithEnumKey_ok` `:738` and `bareEntryGroupingWithEnumKey_ok` `:966` semantics preserved for matching names |
| `integerKeyDiversionUnchanged_ok` | A | the seven `Integer`-keyed sites take exactly the same path they take today |

**Verify.** Stage 2's block verbatim, with `GsonFactoryTest$CaptureTests` (all 26) and
`CaptureGroupingModeTest` (all 6) treated as the primary regression anchors rather than as background.

**Rollback.** `git revert` the commit. Stages 2 and 3 stand without it.

**Estimate.** AI-assisted elapsed **2.5-3.5 hours** with branch B in full, **1.5-2 hours** narrowed;
human-developer **1.5-2 days**.

## 11. Stage 5 - `sgx-publish-one`

**Goal.** Publish cycle 1 and re-pin hypixel. Three commits, **one** build, **one** re-pin. This stage
adds no behaviour; it is the only place binary compatibility is testable.

**Effort.** `small`, and bounded by JitPack rather than by either party.

**Files touched.** `Simplified-Api/hypixel/build.gradle.kts`:44 - one string.

**New tests.** None.

**Steps.** §4 verbatim. Push all three commits together; `jitpack_build gson-extras` builds the tip.

**Verify - and this is the whole reason the stage exists.**

```
# STANDALONE, in the hypixel directory. Not the composite.
gradle_verify hypixel compileJava test --rerun
test_tally hypixel                                    # expect 19/19

cd W:/Workspace/Java/Simplified/Simplified-Api/hypixel
py -3 scripts/json_dto_diff.py > /tmp/diff-after-cycle1.txt
diff /tmp/diff-baseline.txt /tmp/diff-after-cycle1.txt      # expect EMPTY

./gradlew -q --console=plain dependencyInsight \
    --configuration compileClasspath --dependency gson-extras > /tmp/pins-after-cycle1.txt
diff /tmp/pins-baseline.txt /tmp/pins-after-cycle1.txt
# the four forced upgrades must still be four; a new one means a sibling was re-published mid-flight
```

**No DTO has changed at this point**, so a red run here is unambiguously the library's fault - and
because stages 2, 3 and 4 are separate commits, `git bisect` over three shas attributes it without
re-running the cycle.

**Any diff in the differ output after a library-only change is a differ-parser artefact, not a
coverage change.** The script never constructs a `Gson`; it walks the fixture and the DTO class graph
with a regex Java parser. It cannot detect a factory-behaviour regression at all.

**Rollback.** Revert `build.gradle.kts`:44 to `7cfc181` and re-verify hypixel. That is the whole
rollback - the published sha stays on JitPack and is harmless once nothing references it. Only if the
library itself is wrong do you then revert the gson-extras commits, and **only after** every pin is
back.

**Estimate.** AI-assisted elapsed **45-75 minutes**, most of it JitPack wait plus one `--rerun`;
human-developer **1.5-2 hours**. The build wait is the same minutes in both columns, which is why this
is the narrowest ratio in the file. Do not schedule it as a filler task.

## 12. Stage 6 - `sgx-consumer-overflow`

**Goal.** Adopt in hypixel what cycle 1 made possible. Every item here is **additive and per-site** -
the library fix at stage 4 already stopped destroying data with no consumer edit at all, so this stage
is about making the recovered data *visible*, not about making it correct.

**Effort.** `small`. No cycle, no pin, consumer-only.

**Files touched, and the one that is not optional.**

| # | File | Change | Optional |
| --- | --- | --- | --- |
| 1 | `scripts/json_dto_diff.py`:175 | `ann_value(ann, "Extract")` uses the **no-parameter** regex `@Extract\s*\(\s*"([^"]*)"\s*\)` at `:100`, which requires a lone string literal and an immediate `)`. The first `@Extract(value = ..., filter = ...)` makes it return `None`, the field falls back to its **Java name**, the real JSON key reports as unmapped and the Java name reports as a phantom binding. Patch: `ann_value(ann, "Extract", param="value")` with a fallback to the current call - three lines | **No. Must land in the same commit as item 2** |
| 2 | `member/crimson/CrimsonIsle.java`:65-66 | `Quests.questRewards` is one JSON object carrying two unrelated maps interleaved by value type - `<itemId> -> <count>` with integer values and `<questId> -> <itemId>` with string values, declared `ConcurrentMap<String, Object>` because the DTO gave up. Becomes a `@Lenient` map typed to one half plus a dotless `@Extract` remainder for the other | no - this is the stage's payload |
| 3 | `member/crimson/Kuudra.java`, `member/crimson/Dojo.java` | optional companion `@Extract("completedTiers")` / `@Extract("points")` fields, `ConcurrentMap<String, Integer>`, holding the keys no enum constant matched | yes, per site |
| 4 | `member/mining/HeartOfTheMountain.java`, `member/TrophyFishing.java` | the same companion, **only if** stage 4 took branch B; until then their overflow is always empty | yes, and gated |

**The companion field's key type is `String`, never the enum.** These are by definition the keys no
constant matched, and an enum-keyed remainder reproduces the defect one level up.

**The companion field is only free because stage 2 landed.** On `7cfc181`, adding an `@Extract` field
to a DTO adds a root-level `"unknownTiers": {...}` object to every serialize of that class - Java field
name, nested shape, invented by the model. On a sha carrying stage 2 it adds nothing to the document.

**New tests required in hypixel.**

| Test | Form | Asserts |
| --- | --- | --- |
| `mapsQuestRewards` | A - new surface | both halves of `quest_rewards` are typed and neither is lost |
| `mapsKuudraUnknownTiers` | A | the companion field holds the unmatched keys; `completedTiers` has no `null` key |
| `loadoutsSerializeHasNoRootExtractKeys` | **B, enabled here** | written `@Disabled` at stage 1 with its red observed on `7cfc181`; the consumer-side half of the duplicate-key fix |

**Verify.**

```
gradle_verify hypixel compileJava test
test_tally hypixel
py -3 scripts/json_dto_diff.py > /tmp/diff-after-stage6.txt
diff /tmp/diff-baseline.txt /tmp/diff-after-stage6.txt
# a diff IS expected here - it should be exactly the quest_rewards keys moving from unmapped to
# mapped, and NOTHING resembling a phantom binding under a Java field name. A phantom means item 1
# did not land
toolsmith reorder --check <changed paths>
toolsmith javadoc --scope <changed paths>
```

**Rollback.** Revert the consumer commits. The pin stays; the library change is inert without the
annotations. Item 1 can stay regardless - it is strictly a parser improvement.

**Estimate.** AI-assisted elapsed **1.5-2.5 hours**; human-developer **6-8 hours**.

## 13. Stage 7 - `sgx-fallback`

**Goal.** An enum-constant marker so an enum value the model does not declare reads as that constant
rather than as `null`. `CaseInsensitiveEnumTypeAdapterFactory.java`:82 is
`nameToConstant.get(in.nextString().toUpperCase())`, and a `Map.get` miss returns `null` and throws
nothing, so the reflective binder writes that `null` over the field's initialiser - the
sentinel-constant-plus-default idiom the DTOs are written in does not work. Cycle 2, commit 1 of 2.

**Effort.** `medium` for the in-module half. `large` if `Rarity` and `GameMode` are taken in the same
step, because they live in `Simplified-Api/skyblock`, which pins its own `gson-extras` sha
(`build.gradle.kts`:44, `2ba8143` against hypixel's `7cfc181`), and `00-conventions.md` §4 bumps a
two-cycle proposal one level. **Defer them** (§22).

**Files touched - four, not one.**

| File | Change |
| --- | --- |
| `annotation/Fallback.java` | **new.** A marker with no elements, `@Target(FIELD)`, `@Retention(RUNTIME)` - an enum constant is a field |
| `factory/CaseInsensitiveEnumTypeAdapterFactory.java` | ~22 lines in four hunks: imports; a `private final @Nullable E fallback` alongside the two maps at `:43-44`; the constructor loop at `:47-64` gains one `isAnnotationPresent` per constant and **no extra reflection** because it already calls `enumClass.getField(...)`, plus a `JsonException` on a second marked constant; `:82` becomes `return constant != null ? constant : this.fallback;`, plus a public static `isFallback(Gson, Type, Object)` query |
| `factory/CaptureTypeAdapterFactory.java` | **companion guards 1 and 2** - `:490-494` gains the fallback clause, `:538-541` gains `&& !isFallback(gson, valueType, result)` |
| `factory/LenientTypeAdapterFactory.java` | **companion guards 3 and 4** - `:258-264` and `:309-312`, the same two shapes |

**The four companion guards ship in this commit or the change is a silent regression.** Guards 2 and 4
are the invisible half: `CaptureTypeAdapterFactory.java`:538-541 and
`LenientTypeAdapterFactory.java`:309-312 both test `result != null` for enum **values**, so today an
enum-valued `@Capture` or `@Lenient` map keeps an unrecognized value in overflow and round-trips it.
Once the read returns a constant, `result != null` becomes true, the entry is judged compatible and it
binds onto the fallback - **lossless becomes lossy, for every sibling module on the shared pin**. No
site in hypixel has an enum-valued `@Capture` or `@Lenient` map today, so nothing here breaks; every
sibling inherits the change regardless. Since no consumer can mark an enum before the annotation
exists, shipping them together costs nothing and removes the window entirely.

Guard 1 is the same line stage 4 rewrites. Whichever lands second writes the combined clause; because
cycle 2 is authored on top of cycle 1's tree, that is this stage.

**Opt-in by construction.** `this.fallback` is `null` when no constant is marked, so the expression
degrades to today's exact behaviour and un-annotated enums are bit-identical. The obvious alternative -
"on a miss return the constant named `UNKNOWN`" - is a silent behaviour change for every enum in every
consumer and would guess wrong repeatedly here.

**What this stage does NOT fix, stated so the adoption list is not written on a false premise.**
`f06-capture-null-enum-key` belongs to stage 4, permanently. gson's stock `MapTypeAdapterFactory`
(2.11.0 `:199-204`) binds roughly forty enum-keyed plain maps in hypixel that no `@Capture` change and
no marker can repair: the marker turns one unknown key typed and nothing more, the original key is
still gone, and it still does not round-trip. That adapter branches on **duplication**, not on
nullness, and two unmatched keys already collide and already throw today - so the marker does no harm
there either, provided the eligibility rule below holds. The real answer for those forty is `@Lenient`
on the map, which moves them into stage 4's reach. **That is an adoption-list rule, not a code fix.**

**New tests required in `gson-extras`.**

| Test | Form | Asserts |
| --- | --- | --- |
| `unmarkedEnumUnknownValueStillBindsNull_ok` | A | the unchanged-behaviour guarantee. Written **before** the edit on a group A enum shape |
| `markedEnumUnknownValueBindsFallback_ok` | B | the marker itself |
| `markedEnumKnownValueUnchanged_ok` | A | a declared name still resolves to itself |
| `twoMarkedConstants_throws` | B | `JsonException` at adapter construction |
| `markedEnumValuedCaptureOverflowStaysLossless_ok` | **B, guards 2 and 4** | mark an enum, put it as the **value** type of a `@Capture` map, feed an unrecognized value; it must still reach overflow and round-trip. **Stash only the guard hunks and confirm this one fails** - that is the whole argument for shipping them together |
| `markedEnumValuedLenientOverflowStaysLossless_ok` | B | the `@Lenient` half of the same |
| `markedEnumKeyStillDivertsToOverflow_ok` | B | guard 1: a fallback-resolved **key** is judged incompatible and diverted, so stage 4's lossless behaviour survives the marker |
| stage 1 row 19 | A | `enumValuedOverflowIsLossless_ok` must still pass. If it goes red, guards 2 and 4 are wrong |

**Verify.** Stage 2's block, plus `GsonSettingsPrewarmTest` treated as a primary anchor: a `create`-time
`JsonException` is swallowed per type by `GsonSettings.prewarm` `:193-202`, which catches `Throwable`,
so the diagnostic is **delayed to first real use rather than immediate**. Also re-run the composite
compile - this is the one stage that edits a class `dataflow`'s `serde/PipelineGson.java` constructs by
hand, and although only its body changes, the compile is the proof.

**Rollback.** `git revert` the commit. It reverts cleanly as long as stage 8 is reverted first.

**Estimate.** AI-assisted elapsed **2.5-3.5 hours**; human-developer **1.5-2 days**.

## 14. Stage 8 - `sgx-flatten`

**Goal.** Collapse a single-valued JSON wrapper object into the map or collection the caller actually
wants. Cycle 2, commit 2 of 2, and deliberately **last** - it is the lowest-value item in the work and
the one most easily dropped if the cycle is cut short.

**Effort.** `small` - `00-conventions.md` §4's price for "a new annotation plus a self-contained new
factory registered in `GsonSettings`". Two new files, one registration line, **zero edits to any
existing factory**, one consumer field.

**Files touched.**

| File | Change |
| --- | --- |
| `annotation/Flatten.java` | **new.** One `String value()`, the wrapper member name |
| `factory/FlattenTypeAdapterFactory.java` | **new**, modelled on `SplitTypeAdapterFactory` - the smallest self-contained factory in the library. `create` returns `null` when the type carries no `@Flatten` field, which keeps it off the hot path. Serialized-key resolution copied verbatim from `SplitTypeAdapterFactory.java`:197-199 |
| `GsonSettings.java` | one line at **index 5**, between `LenientTypeAdapterFactory` and `CaptureTypeAdapterFactory`, plus the class javadoc list |

**Index 5, not the research pack's index 3.** At index 3 `@Flatten` nests inner to `Lenient`, which
sees the uncollapsed wrappers, judges every one incompatible with the declared value type, and diverts
the entire field to overflow before `@Flatten` is consulted - the field binds empty. There is no future
in which that composes. At index 5 the read works correctly and only the write of that one excluded
pair is wrong, for a reason a future overflow-aware design could address.

**Four `create`-time rejections, thrown rather than silently ignored.** Field is neither a `Map` nor a
`Collection`; field also carries `@Capture`; field also carries `@Lenient`; field also carries
`@SerializedPath`; and `@Flatten("")`. `@Lenient` is the one that departs furthest from the research
pack, which said the pair "does not compose at index 3, but the fix is a move rather than an
exclusion" - the second half only checks the read; the pair corrupts the round trip in **both**
registration orders. Closing it by a `create`-time throw means there is **no in-annotation escape hatch
for a missing wrapper**, which is the price of the exclusion and is stated on the annotation.

**Two properties that must be declared in the javadoc, because both reverse a research-pack claim.**

- **`@Flatten` is the first annotation in `gson-extras` that does not preserve round-trip fidelity.** A
  wrapper carrying a sibling member reads fine and serializes back **without** that member. Declare the
  loss; do not assert byte-equality.
- **A wrapper missing the named key aborts the entire document read** with a `JsonSyntaxException` -
  on a 1.6 MB profiles response - where today it degrades to a `null` map value. The research pack's
  recommended mitigation was `@Lenient`, which the paragraph above rejects at `create`.

**New tests required in `gson-extras` - and these are the only code that will ever execute
`FlattenTypeAdapter.write`.** The hypixel module never serializes: zero `toJson`/`toJsonTree` hits in
`hypixel/src`, so a write-side `@Flatten` regression passes all of hypixel's tests.

| Test | Form | Asserts |
| --- | --- | --- |
| `flattenMap_read` / `flattenCollection_read` | A - new | both branches. **The `JsonArray` branch has zero adoption sites** - the same single-site trap `Dungeons.unlockedJournals` represents for `@Lenient`'s array branch |
| `flattenMap_roundTrip` / `flattenCollection_roundTrip` | A | the write half |
| `flattenMultiMemberWrapper_roundTrip` | A | **pins the declared loss as a contract**, not an accident |
| `flattenAlreadyCollapsed_read` | A | pins the normalisation as intended |
| `flattenMissingMember_throws` | B | the exception rather than a silent empty map |
| `flattenIdleType_returnsNull` | A | `create` returns `null` for the other 132 files |
| four `create`-time rejection tests | B | one per exclusion |
| `flattenSiblingCapture_ok` | A | different fields on one class - a `@Flatten` field's key is a **known key**, copied verbatim into `knownObject` at `:315-318` |
| `flattenInsideCaptureValue_ok` | A | `@Flatten` declared on a `@Capture` map's **value class** works, because both map builders deserialize the value with a fresh top-of-chain lookup |
| `defaultFactoryOrderIsStable_ok` | A | updated to the final ten-entry list |

**One diagnostic that is not yet real, and say so rather than discovering it.** A mis-declared
`@Flatten` on a `@Capture` map's value class throws inside the `gson.fromJson` at `:399`/`:475`, which
sits in the existing empty catch at `:401-402` or `:477-478`, so it is **swallowed per entry** and the
map silently comes back short. Stage 4's optional branch-B widening is what makes it visible. That is
an existing defect eating a new diagnostic, not a new hazard.

**Verify.** Stage 2's block. The **whole** `CombinationTests` nest re-runs against transcription error,
because inserting a factory shifts every later index in `GsonSettings.defaults()` and no test asserts
the list's contents, order or length except the one stage 1 added. `WeakIdentityMapTest` is explicitly
**not** an anchor here - `@Flatten` adds no store, no static state and no cross-call lifetime.

**Rollback, three independent levels.** Revert the consumer commit (no re-pin, the library keeps an
unused annotation); remove the one `GsonSettings` line, leaving both files inert on disk (one publish);
or `git revert` the whole commit (one publish, and it does not disturb the overflow commits because
they share no files).

**Estimate.** AI-assisted elapsed **2-3 hours**; human-developer **1-1.5 days**.

## 15. Stage 9 - `sgx-publish-two`

**Goal.** Publish cycle 2 and re-pin hypixel. Two commits, **one** build, **one** re-pin.

**Effort.** `small`.

**Files touched.** `Simplified-Api/hypixel/build.gradle.kts`:44.

**New tests.** None.

**Steps and verify.** §4 verbatim, and §11's verify block verbatim, substituting
`/tmp/diff-after-cycle2.txt` and `/tmp/pins-after-cycle2.txt`. Expect an **empty** differ diff against
`/tmp/diff-after-stage6.txt`: neither `Fallback` nor `Flatten` appears anywhere in
`json_dto_diff.py`, so a marker reports nothing (correct by luck) and a `@Flatten` wrapper level would
report as unmapped - but nothing is adopted yet at this point, which is the reason step 6 comes before
step 7.

**Run the standalone hypixel pass again even though stage 5 already did one.** Cycle 2 edits
`CaseInsensitiveEnumTypeAdapterFactory`, which is the one class in this work that a sibling module
constructs by hand, and the force-upgrade of four sibling jars is re-evaluated on every re-pin.

**Rollback.** Revert `build.gradle.kts`:44 to cycle 1's sha and re-verify hypixel. If the library is at
fault, revert stage 8's commit first, then stage 7's - in that order, and only after the pin is back.

**Estimate.** AI-assisted elapsed **45-75 minutes**; human-developer **1.5-2 hours**.

## 16. Stage 10 - `sgx-consumer-additive`

**Goal.** Mark the eligible enums and adopt `@Flatten` at its single site. Two commits, consumer-only,
no cycle.

**Effort.** `medium`, and the research pack's "roughly a dozen one-line edits" does not survive
verification against the fixture: **eight of the twelve eligible enums have no sentinel constant to
mark**, because their existing default is itself a live wire value. Adding one changes `values()` for
that enum's own `of()` and `stream()` helpers, which is consumer work the pack did not count.

**Prerequisite that is not negotiable - three consumer-side naming fixes land FIRST**, in their own
commit, before any marker. All three are real data loss **today**, all three are consumer-only, all
three are `trivial`, and `@Fallback` would mask every one of them behind a confident typed sentinel:

| Fix | Defect |
| --- | --- |
| `BoardQuest.Status` | missing `COMPLETE` |
| `Dojo.Type` | its seven constants carry their wire names as a **constructor component** rather than as `@SerializedName` |
| `RabbitSort` | declares `highest_rarity` where the fixture carries `rarity_high_low` |

These are the `s20-dark-feature-fixes` family from the DTO plan. **Pull exactly these three forward**;
the rest of that stage stays where it is.

**Files touched.**

| Commit | Files | Change |
| --- | --- | --- |
| 1 | 3 enum files | the naming fixes above |
| 2 | 4 enum files | **group A - mark now.** `CrimsonIsle.Faction` (`NONE`), `DungeonData.Type` (`UNKNOWN`), `BoardQuest.Status` (`UNKNOWN`), `DungeonClass.Type` (`UNKNOWN`). One line each; the sentinel exists and the fixture never carries it |
| 2 | 8 enum files | **group B - new sentinel first.** `Kuudra.Tier`, `Kuudra.SearchSettings.Sort`, `RabbitSort`, `RabbitFilter`, `Crystal.State`, `Banking.Action`, `CommunityUpgrades.Type`, `DungeonChest.Type`. A new `UNKNOWN` plus a pass over that enum's own `values()` consumers |
| 3 | `member/Currencies.java`:17-24 | `essence` becomes `@Flatten("current") ConcurrentMap<String, Integer>`, deleting the nested generic, the `@Getter(AccessLevel.NONE)`, the five-line stream accessor and the `AccessLevel` import |

**The eligibility rule, and it is the only mechanical one.** Never mark a constant that has a wire
representation of its own, checked case-insensitively across `name()`, `@SerializedName.value` and
every `alternate`. **`Kuudra.Tier` gets a new `UNKNOWN`, never `BASIC`**: `Kuudra.java`:28-29 declares
`BASIC` as `@SerializedName("NONE")` and `highest_wave_none` is in the fixture, so marking it turns the
probe input `{"none":1,"brand_new_tier":4}` into `{BASIC=4}` where today it produces `{null=4,
BASIC=1}` - the unknown tier **overwrites a correct entry**, strictly worse than today's null key.

**Do not mark.** `Rarity` and `GameMode` live in `Simplified-Api/skyblock` and cost a chained publish
cycle for three field sites - deferred (§22). `ActiveCommission.Status` has a single constant and is
**under-modelled, not under-defaulted**; so is `RabbitSort` in part; the marker would hide the defect.
Six more - `Floor`, `Statistics.Mythos.Type`, `Statistics`' nested boss `Type`, `RabbitEmployee`,
`GlaciteTunnels.CorpseType`, `HypixelSocial.Type` - exist only as plain map keys, where a marker buys
one unknown key turning typed and nothing else: permitted, close to pointless.
`JacobsContest.Medal` is a genuine "do not mark" for an unrelated `Optional` reason.

**The safety of this stage lives in the adoption list, not in the code.** A contributor marking `Floor`
because it "looks like the others" reintroduces the whole hazard with **no compile error and no test
failure**. Encode the eligibility rule as a consumer test (below) so the list defends itself.

**New tests required in hypixel.**

| Test | Form | Asserts |
| --- | --- | --- |
| `noMarkedConstantIsWireVisible` | A | reflects over every `@Fallback`-carrying constant in the module and fails if its `name()`, `@SerializedName.value` or any `alternate` appears in the fixture, case-insensitively. **This is the rule, executable** |
| `decodesUnknownDungeonClass` | B | `{"selected_dungeon_class": "necromancer"}` into `Dungeons`; `null` today, `UNKNOWN` after. Nothing in `MemberDtoMappingTest` covers this - its only `UNKNOWN` assertions at `:104-105` exercise `getOrDefault` lookups, not the bind |
| `mapsCurrenciesEssence` | B | `essence` binds through the collapsed shape |
| `currenciesEssenceRoundTripDropsSiblings` | A | **pins the declared `@Flatten` loss** rather than asserting the byte-equality the research pack asked for |

**Also grep the sibling modules for code relying on a `null` enum before landing.** Cheap insurance
while every marked enum is in this module; load-bearing the moment `Rarity`/`GameMode` are scheduled.

**Verify.**

```
gradle_verify hypixel compileJava test
test_tally hypixel
py -3 scripts/json_dto_diff.py > /tmp/diff-after-stage10.txt
diff /tmp/diff-after-stage6.txt /tmp/diff-after-stage10.txt
# expect ONE change: the Currencies wrapper level. `Flatten` appears nowhere in the script, so the
# collapsed level reports as unmapped. That is a differ artefact, not a coverage regression
toolsmith reorder --check <changed paths>
toolsmith javadoc --scope <changed paths>
```

**Rollback.** Revert commit 3 to drop `@Flatten` with no re-pin; revert commit 2 to drop every marker
with no re-pin. Keep commit 1 regardless - the naming fixes are correct independently.

**Estimate.** AI-assisted elapsed **3-4 hours**, dominated by the eight group-B enums and their
`values()` consumers; human-developer **2-2.5 days**.

## 17. Stage 11 - `sgx-sibling-convergence`

**Optional, and an owner decision (§22).** Twelve modules pin `gson-extras` at `2ba8143`, three commits
behind hypixel's `7cfc181` before this work starts and five behind after it. This stage converges them.

**Effort.** `medium` - twelve `build.gradle.kts` edits and twelve module verifies, no publish.

**The measured argument for doing it.** hypixel is **already** running `skyblock`, `github`,
`persistence` and `client` against a `gson-extras` none of them was compiled against - its `strictly`
force-upgrades all four. The pins are nominal; the runtime has already converged whether or not the
build files say so. Making that explicit is cheaper than discovering it through a `NoSuchMethodError`.
Nothing in the consumer survey says any of the twelve would notice: none uses `@Capture`, `@Lenient` or
`@Extract`.

**The argument against deferring it.** Whenever convergence happens, those eleven modules receive
`c944987`'s collection-value change, `b071689`'s grouping-mode change **and** all of this work in one
step, against suites that exercised none of it. The drift compounds; the risk does not shrink.

**Verify.** `toolsmith jitpack pins` before and after, then `gradle_verify <module> compileJava test`
per module. Do them one at a time; a batch re-pin with a single verify at the end names no culprit.

**Rollback.** Per module, revert that module's pin line. Independent by construction.

**Estimate.** AI-assisted elapsed **1.5-2.5 hours**; human-developer **4-6 hours**.

## 18. Green at every step - the invariants

The requirement is that `gson-extras` compiles and its tests pass at **every** stage boundary and that
hypixel stays green throughout. Four invariants make that true rather than hoped for.

**I1 - the tally is a number, not a colour.** Assert the expected pass **and skip** counts at every
boundary. A *drop* in the discovered count means a test silently stopped being discovered, which is a
failure that reports as green. Use `test_tally`, never an inline script over
`build/test-results/test/*.xml`.

| After stage | gson-extras | hypixel | Notes |
| --- | --- | --- | --- |
| 0 | 134 pass, 0 skip | 16 pass | baseline |
| 1 | 153 pass, **3 skip** | 19 pass | +19 library, +3 consumer; three form-B tests disabled |
| 2 | 155+ pass, **1 skip** | 19 pass | rows 2 and 7 enabled; `OverflowTest` and `ExtractTests` added |
| 3 | 165+ pass, 1 skip | 19 pass | ten `ExtractTests` rows |
| 4 | 172+ pass, **0 skip** | 19 pass | row 18 enabled; every skip is now a pass |
| 5 | unchanged | 19 pass, standalone | the binary-compatibility pass |
| 6 | unchanged | 22 pass | one of the three is stage 1's disabled consumer row, enabled here |
| 7 | 180+ pass, 0 skip | 22 pass | |
| 8 | 195+ pass, 0 skip | 22 pass | `FlattenTests` plus two `CombinationTests` rows |
| 9 | unchanged | 22 pass, standalone | |
| 10 | unchanged | 26 pass | |

Counts after stage 2 are floors, not predictions; hold the floor and record the actual.

**I2 - a form-B test is never committed red.** It is committed `@Disabled` with the observed failure as
its reason string, and the `@Disabled` is removed in the same commit as the fix (§5). That is what lets
the duplicate-key defect and the null-enum-key defect be *proved* at stage 1 without leaving a red
suite behind.

**I3 - hypixel is verified two ways and the two answer different questions.** From the workspace root,
`./gradlew :Simplified-Api:hypixel:test` runs hypixel against the `gson-extras` **working tree** and
answers "did this stage change behaviour" - it costs no cycle and it is the per-stage gate. Standalone
in the hypixel directory after a re-pin, `gradle_verify hypixel compileJava test --rerun` runs against
the **published** artifact plus four sibling jars compiled against older shas, and it is the only run
that can answer "is this binary compatible". Neither substitutes for the other.

**I4 - `json_dto_diff.py` is a diff, not an exit code.** It exits 1 today with 792 unmapped keys. Save
the baseline and compare files. And it cannot detect a factory-behaviour regression at all - it never
constructs a `Gson`. Three of its parser behaviours will produce false signals during this work:
`@Extract` with named parameters (patched at stage 6), `@Fallback` and `@Flatten` (unknown to it), and
`@Lenient` (unmodelled, so it reports a `@Lenient` field as covering keys that actually went to
overflow - a standing false negative that stage 3's filter element adds a second instance of).

**One invariant this file cannot provide.** Nothing enforces that `ExtractTypeAdapterFactory` stays
outside `CaptureTypeAdapterFactory`. `defaultFactoryOrderIsStable_ok` catches a change to
`GsonSettings.defaults()`; it cannot catch an SPI or `GsonContributor` factory, which
`GsonSettings.java`:259-263 appends **after** the list and which therefore nests outside everything.
Against those the guarantee is not enforceable and this work does not claim it is.

## 19. What this unblocks in the DTO plan

Reading `notes/json-annotations/20-implementation-plan.md` §3's stage table against this file. Stage 9
of that plan is deleted (§2). Nothing else in it is deleted, and its remaining nine stages keep their
own ordering and their own four internal dependencies.

| DTO stage | Effect of this work | Unblocked by |
| --- | --- | --- |
| 1 `s20-dark-feature-fixes` | **Pull three enum naming fixes forward** - `BoardQuest.Status`, `Dojo.Type`, `RabbitSort` - because `@Fallback` would mask them. The rest of the stage is unaffected and stays where it is | nothing; it is a prerequisite of stage 10 here |
| 2 `s20-free-retirements` | unaffected | - |
| 3 `s20-holder-collapse` | unaffected. `@Flatten` collapses a **wrapper level**, not a holder class; the two do not overlap | - |
| 4 `s20-objectives-catchall` | unaffected in content, but it is the stage that takes the differ from 792 unmapped to 0. Run it **after** stage 6 here, so the `@Extract` parser patch is already in and the 792 -> 0 move is not entangled with a phantom binding | stage 6 here |
| 5 `s20-existing-annotation-sweep` | **Materially expanded.** Its job is typing 29 `Object` fields, and `CrimsonIsle.Quests.questRewards` was the one it could not do - one JSON object carrying two maps interleaved by value type, unreachable because `@Extract` had no filter axis and two fields cannot both claim `quest_rewards`. Stage 6 here does that site; the sweep inherits the pattern for any other it finds | **stage 5** here (the re-pin) |
| 6 `s20-shape-retirements` | unaffected | - |
| 7 `s20-derivation-retirements` | unaffected | - |
| 8 `s20-duplication-sweep` | unaffected | - |
| 9 `s20-library-cycle` | **deleted.** §2 | - |
| 10 `s20-skyblock-election` | unaffected, and it is the natural carrier if `Rarity`/`GameMode` are ever scheduled - both need the same `skyblock` publish cycle | - |

**Three capabilities the DTO pack declined that now exist**, and each is a row in `README.md` §6.2 that
should be re-read rather than relitigated:

- **"`@Lenient` typed-overflow element"** - declined as "one site". Superseded by stage 3. The decline
  is not overturned on new site evidence; there is still one site. It is overturned because the
  mechanism it needed is being built anyway.
- **"`@Capture` unmatched-key element"** - declined as "one site" and as "subsumed by `@Fallback`".
  Superseded by stage 4, which adds **no element at all**. The subsumption does not hold: `@Fallback`
  cannot be applied to the enum at the site the defect was proved at.
- Those two were counted as two thin proposals. They are **one** missing capability - `@Extract` had no
  filter axis and could not reach the `@Capture` store - and acting on that is what this whole file is.

**One row that is NOT part of that group and stays declined.** "`@Capture` value-grouping element"
concerns bind-side grouping-mode **selection** - how `CaptureFieldInfo` infers grouped versus entry
mode from the declared value type - and has nothing to do with overflow, with `@Extract`, or with the
store. Merging it into the group would be the same counting error in the opposite direction. Stage 4's
branch B edits `buildGroupedMap`, but only to divert a group whose key failed; it does not touch mode
inference, adds no `Grouping` constant, and leaves `Capture.Grouping`'s deliberate two-constant shape
exactly as it is.

**One consequence for the DTO plan's own gate.** Its §15.1 gate is `MemberDtoMappingTest`, which never
calls `toJson`. After stage 1 here it does, at three sites, and after stage 6 at four. Any DTO stage
that touches `Loadouts`, `Bestiary`, `Dungeons` or `Currencies` now has round-trip coverage it did not
have when that plan was written.

## 20. Estimates

**AI-assisted elapsed time first, human-developer elapsed as the comparison.** Both are wall-clock
elapsed, not effort-hours, and both include verification at the stage boundary. The AI-assisted figure
is the observe-correct loop - authoring is fast, but running the build, reading the failure and fixing
the next thing still bounds it.

| Stage | Effort | AI-assisted elapsed | Human-developer elapsed |
| --- | --- | --- | --- |
| 0 `sgx-baseline` | `trivial` | **15-25 minutes** | 30-45 minutes |
| 1 `sgx-characterisation` | `medium` | **3-4 hours** | 1.5-2 days |
| 2 `sgx-overflow-store` | `large` | **4-6 hours** | 3-4 days |
| 3 `sgx-extract-filter` | `medium` | **2-3 hours** | 1-1.5 days |
| 4 `sgx-capture-unmatched` | `medium` | **2.5-3.5 hours** | 1.5-2 days |
| 5 `sgx-publish-one` | `small` | **45-75 minutes** | 1.5-2 hours |
| 6 `sgx-consumer-overflow` | `small` | **1.5-2.5 hours** | 6-8 hours |
| 7 `sgx-fallback` | `medium` | **2.5-3.5 hours** | 1.5-2 days |
| 8 `sgx-flatten` | `small` | **2-3 hours** | 1-1.5 days |
| 9 `sgx-publish-two` | `small` | **45-75 minutes** | 1.5-2 hours |
| 10 `sgx-consumer-additive` | `medium` | **3-4 hours** | 2-2.5 days |
| **Subtotal, stages 0-10** | - | **23-33 hours elapsed** | **13-17 working days** |
| 11 `sgx-sibling-convergence` | `medium`, optional | **1.5-2.5 hours** | 4-6 hours |
| **Total with stage 11** | - | **25-36 hours elapsed** | **14-18 working days** |

The ratio is roughly 4:1 and it is not uniform. Where it is widest and narrowest is worth knowing
before scheduling:

- **Widest on stage 1 and stage 10** - nineteen tests against models that already exist, and twelve
  near-identical enum edits with a known shape. Mechanical and repetitive is what AI authoring is
  fastest at, and the human figure is dominated by typing and by re-checking each name against the
  fixture.
- **Narrowest on stages 5 and 9**, which are bounded by JitPack rather than by either party. The build
  wait is the same minutes in both columns. **Do not schedule either as a filler task expecting it to
  finish in a gap.**
- **Least certain on stage 2.** `large` may be optimistic on the human column and the AI column is the
  one most likely to need a second pass: ninety lines come out of five separate regions of the
  library's second-most-used file, and the failure mode of getting the `@Lenient` re-scan gates wrong
  is orphan entries in a `WeakIdentityMap` that has no `remove` - which no test naturally reaches.

**Comparison with what this replaces.** The DTO plan priced `s20-library-cycle` at **2-3 hours
AI-assisted / 6-8 hours human**, for `@Fallback` plus `@Flatten` plus the `PostInit` fix in one
`medium` stage. Stages 7 through 10 here cover its two annotation payloads at **9-13 hours AI-assisted
/ 6-8 days human**. The gap is not scope creep in the annotations; it is the corrected adoption list
(eight enums need a new sentinel), the four companion guards that stage did not know about, and the
`@Flatten` write-side tests that are the only coverage that mechanism will ever have. Stages 0 through
6 - **14-20 hours AI-assisted** - are new work that stage 9 never contained.

## 21. Rollback matrix

Rollback cost is a function of **how many pins reference the sha**, not of how large the diff was.
That is why the stage list separates the library commits from the publish that exposes them.

| Stage | Rollback move | Repos touched | Re-pin needed | Cost |
| --- | --- | --- | --- | --- |
| 0 | nothing to roll back | - | no | zero |
| 1 | `git revert` the test commit in each repo | 2 | no | zero - nothing pins a test-only sha |
| 2 | `git revert` the commit | 1 | no | one composite verify. No pin exists yet |
| 3 | `git revert` the commit | 1 | no | as above. Stage 2 stands without it |
| 4 | `git revert` the commit | 1 | no | as above. Stages 2 and 3 stand without it |
| 5 | **revert `build.gradle.kts`:44 to `7cfc181`**, re-verify hypixel standalone | 1 | yes, backwards | one verify. The published sha stays on JitPack, harmless once unreferenced |
| 6 | revert the consumer commits; keep the differ patch | 1 | no | one verify. The library change is inert without the annotations |
| 7 | `git revert` the commit - **after** stage 8 is reverted | 1 | no if pre-publish | reverts cleanly only in reverse order |
| 8 | three independent levels: revert the consumer adoption (no re-pin), remove the one `GsonSettings` line (one publish), or revert the commit (one publish) | 1 | depends on level | the consumer level is the one that matters - any `@Flatten` problem found after the pin bump is undone by reverting one DTO |
| 9 | revert `build.gradle.kts`:44 to cycle 1's sha | 1 | yes, backwards | one verify |
| 10 | revert commit 3 for `@Flatten`, commit 2 for the markers; **keep commit 1**, the naming fixes are correct independently | 1 | no | one verify |
| 11 | per module, revert that module's pin line | 1 each | yes, per module | independent by construction |

**The one rule that is not negotiable.** A two-repository rollback goes **pin first, library second**.
Revert the pin edit in every consuming module, verify, and only then revert the `gson-extras` commit.
Never revert a library commit while a module is still pinned to that sha.

**Why three commits inside one cycle is worth the bookkeeping.** Cycle 1 publishes one sha, so its pin
rollback is atomic - but a red run at stage 5 can be attributed by `git bisect` over three shas without
re-running the cycle. A single squashed commit would make the first failure cost more than the staging
saved.

## 22. Decisions the owner must make, and when

Seven, each with a deadline expressed as the stage that cannot start without it.

| # | Decision | Needed before | Default if unanswered |
| --- | --- | --- | --- |
| 1 | **Do the three `@Extract` no-op rows throw at `create`?** Misspelled source, superclass source, unannotated source. Today all three are a silent `continue`. Throwing is the right diagnostic - a claim that can never match is silent data loss no test catches, and `create` runs once per type - but it is a **hard break for any downstream module quietly doing nothing today**, and sibling modules share the pin. Note the diagnostic is late either way: `GsonSettings.prewarm` `:193-202` swallows `Throwable` per type, so it surfaces at first real use rather than at warm-up | **stage 2** | keep the `continue`. It changes no other part of the design and it is the smaller blast radius |
| 2 | **Is stage 4 branch B taken in full or narrowed?** Full diverts a group whose **value** fails as well as one whose key does, giving a body to one of the five silent swallows. Narrow diverts only on `key == null` and leaves `:477-478` empty. Both reach the two grouping-mode sites | **stage 4** | full, on the argument that it is the treatment entry mode already gives an incompatible value, and that it is what makes stage 8's `create`-time diagnostics visible inside a `@Capture` value class |
| 3 | **Do stages 7 and 8 fold into cycle 1?** One re-pin instead of two, at the cost of a red hypixel run at stage 5 that names no culprit across five items | **stage 5** | keep them separate. The cost is one extra JitPack build |
| 4 | **Does the `PostInit` residue ride stage 8's commit?** The `PostInitTypeAdapterFactory` null-guard and log plus the `PostInit.java` javadoc rewrite is the third payload of the deleted `s20-library-cycle` and is outside this file's scope. Riding cycle 2 costs nothing; leaving it means the DTO pack keeps a library cycle for two files. It is gated on `s20-dark-feature-fixes` either way, because `Bestiary` and `AccessoryBag` throw on every decode today | **stage 8** | leave it in the DTO pack. This file does not design it and should not smuggle it in |
| 5 | **Are `Rarity` and `GameMode` scheduled?** Three field sites behind a chained publish cycle through `Simplified-Api/skyblock`, which pins `2ba8143` against hypixel's `7cfc181`. `00-conventions.md` §4 bumps that half to `large` | **stage 10** | defer. Bundle with `s20-skyblock-election`, which needs the same cycle |
| 6 | **Do the twelve sibling pins converge?** Stage 11 | after **stage 9** | converge. The measured evidence favours it - hypixel already runs four siblings against a `gson-extras` none of them was compiled against |
| 7 | **Does anything in the wider workspace actually serialize a `@Lenient`-carrying DTO?** Nothing in either suite executes `LenientTypeAdapter.write`. If no production caller does either, the round-trip fidelity stage 2 is protecting is theoretical and its price should be re-argued. Resolved by a `toJson` search over `response/skyblock` types in the downstream `dev.sbs` modules | **stage 2**, and it is worth doing **before** pricing it | assume it matters. Losing fidelity silently is worse than paying for it |

Decisions 1 and 2 are the only two that change source. The rest change scheduling.

## 23. Risks carried into execution

Ranked by how badly a green suite would lie about them.

**R1 - the `@Extract` write path has zero test coverage in either module, and stage 2 relocates it.**
`@Extract` appears in exactly one test file, which only deserializes; there is no `LenientTests` nest;
`MemberDtoMappingTest` has 16 tests and none calls `toJson`. `LenientTypeAdapter.write` `:86-150` is
entirely unexecuted. **Stage 2 can ship broken with both baselines green.** Mitigation: stage 1 is a
separate cycle and is not skippable. This is the reason the file is ordered the way it is.

**R2 - the new ordering guarantee is enforced by nothing.** A factory registered between `Capture` and
`Extract`, or any downstream SPI factory, silently reduces `@Extract` to its current capability with no
test failure, because all six existing sites are `@Lenient`-sourced and keep passing. Mitigation:
`defaultFactoryOrderIsStable_ok` plus the two `CombinationTests` rows at stage 2. Residual: the SPI case
is not catchable at all.

**R3 - decision 1 turns three silent no-ops into a hard break** for any downstream module quietly doing
nothing today, and sibling modules share the pin. Mitigation: it is decision 1, taken deliberately, and
the default is the smaller blast radius.

**R4 - `@Extract` on a grouping-mode `@Capture` source is a permanent silent no-op and is not
statically checked.** Six of seventeen `@Capture` sites are grouping mode, **including two of the six
enum-key sites this work is aimed at** - `TrophyFishing.fish` and `HeartOfTheMountain.powder`.
Mitigation: stage 4 branch B is what gives those two an overflow at all; document the no-op on the
annotation. Residual: nothing stops a future site declaring it.

**R5 - gson's stock `MapTypeAdapterFactory` is out of reach and roughly forty enum-keyed plain maps sit
behind it**, including `Statistics.java`:89. It is not fixable in `gson-extras`. The consumer-side
answer is `@Lenient` on the map, which moves it into stage 4's reach - **an adoption-list rule, not a
code fix.** An enum-adapter-scoped fix at `CaseInsensitiveEnumTypeAdapterFactory.read`:82 is `xlarge`,
not `medium`: `create`:35-38 claims **every enum type in the JVM for every consumer** - hypixel 31
enums, `skyblock` 20, `asset-renderer` 90 - and no test in either suite would notice the change. It
needs its own cycle, its own tests and a convergence of all twelve pins, and it is explicitly not
batchable with anything here.

**R6 - stage 7 without its four companion guards is a silent regression for every sibling on the shared
pin.** `CaptureTypeAdapterFactory.java`:538-541 and `LenientTypeAdapterFactory.java`:309-312 both test
`result != null` for enum **values**, so a marked enum stops diverting unrecognized values to overflow -
lossless becomes lossy. Mitigation: same commit, plus `markedEnumValuedCaptureOverflowStaysLossless_ok`
validated by stashing **only** the guard hunks.

**R7 - the safety of stage 10 lives in the adoption list, not in the code.** A contributor marking
`Floor` because it "looks like the others" reintroduces the whole hazard with no compile error and no
test failure. `Kuudra.Tier.BASIC` is the concrete instance: wire-visible via `@SerializedName("NONE")`,
and marking it makes an unknown tier **overwrite a correct entry** - strictly worse than today's null
key. Mitigation: `noMarkedConstantIsWireVisible` makes the rule executable.

**R8 - `@Fallback` masks modelling defects rather than fixing them.** Three live instances verified
against the fixture: `Dojo.Type`'s seven internal names, `RabbitSort`'s `rarity_high_low`, and
`ActiveCommission.Status`'s single constant. Mitigation: the three naming fixes are commit 1 of stage
10 and are a hard prerequisite of commit 2.

**R9 - `@Flatten` does not preserve round-trip fidelity and the module never serializes.** It is the
first annotation in `gson-extras` that loses data on write, and zero `toJson`/`toJsonTree` calls exist
in `hypixel/src`, so a write-side regression passes all consumer tests. **The library's own tests are
the only coverage that mechanism will ever have.** Mitigation: stage 8's write-side rows, and a fixture
assertion that pins the loss rather than asserting byte-equality.

**R10 - single-site coverage traps, three of them, and none is decoded by any test today.**
`Dungeons.unlockedJournals` is the only collection-shaped `@Lenient` field in the workspace; it plus
`Statistics.spawnedSpookyBats` are the only two on the `@SerializedPath` branch; and `@Flatten`'s
`JsonArray` branch has **zero** adoption sites at all. Mitigation: stage 1 rows 4, 8 and 22, and stage
8's two collection rows.

**R11 - binary incompatibility is invisible to `compileJava` and to the composite.** `skyblock`,
`github`, `persistence` and `client` reach hypixel as jars compiled against `b68510e`/`f42ee07`/
`37a2c2f` and are force-upgraded by hypixel's `strictly`. Only a standalone post-re-pin run can surface
a `NoSuchMethodError`. Mitigation: stages 5 and 9 both run it, standalone, and both re-diff the pin
graph.

**R12 - a signature or visibility change to `PostInitTypeAdapterFactory` or
`CaseInsensitiveEnumTypeAdapterFactory` compile-breaks `Simplified-Dev/dataflow`**, which constructs
both by hand in `serde/PipelineGson.java`. Stage 7 changes only the second one's body. Mitigation: the
composite compile is in every stage's verify block for exactly this.

**R13 - inserting a factory shifts every later index in `GsonSettings.defaults()`:249-256, and no test
asserts the list's contents, order or length.** The only order signal today is six behavioural
`CombinationTests`. Mitigation: `defaultFactoryOrderIsStable_ok` at stage 1, updated at stages 2 and 8,
plus a full `CombinationTests` re-run at both.

**R14 - `json_dto_diff.py` degrades the moment `@Extract` gains a named parameter.** `ann_value(ann,
"Extract")` at `:175` uses the no-parameter regex, so the first `@Extract(value = ..., filter = ...)`
silently falls back to the Java field name and produces a phantom binding. Mitigation: the patch is
item 1 of stage 6 and must land in the same commit as item 2.

**R15 - the store merge is a shared-static-state lifetime change bought for lookup convenience.**
Keeping two stores and having `@Extract` consult both would be smaller **if** `Overflow` gains no
second consumer; the counter-argument is that the per-entry target tag has to live somewhere, and a
plain map union destroys the asymmetry that makes a `@Capture`-sourced claim round-trip. Mitigation:
`OverflowTest`'s target-mismatch row, and stage 1 row 12 - the one test that proves per-entry tagging.

**R16 - the twelve sibling pins are drifting and every deferral compounds it.** They are three commits
behind before this work and five after. Whenever they converge they receive `c944987`, `b071689` and
all of this in one step, against suites that exercised none of it. Mitigation: stage 11, and it is
decision 6.

## 24. Deferred - not in this order

**`@Owner` / `@Parent` reach-back is deferred by the owner until after the DTO research pack lands. It
is not designed here, not scheduled here, and nothing in this file should be read as designing it.** No
stage depends on it, constrains it, or reserves a slot for it. There is no placeholder element, no
half-specified hook and no reserved registration index anywhere in stages 0 through 11. Two things are
recorded only so the deferral is a decision rather than an omission: it is the one candidate in the
registry that needs a **post-bind lifecycle hook**, which puts every consumer's adapter chain in scope
rather than hypixel's alone; and stage 2 does not foreclose it, because `ExtractTypeAdapter.read`
delegates first and then assigns off the built object, which is the same structural position such a
hook would occupy. Whether a reach-back reuses that slot, sits elsewhere, or ends up inside
`PostInitTypeAdapterFactory` is exactly the question this work must not answer early - guessing it
would put an unused ordering guarantee into `GsonSettings` that stage 1's order test would then pin.

**The `@Capture` value-grouping element stays declined** on blast-radius grounds and is not part of the
overflow capability gap (§19).

**The enum-adapter-scoped unmatched-key fix is not in this order.** It reaches roughly forty more
fields including `Statistics.java`:89, and its reach is exactly its blast radius (R5). Its own cycle,
its own tests, and a convergence of all twelve pins.

**The `PostInit` residue is not designed here** and is decision 4 (§22).

**Six things this file deliberately does not do, so none of them is relitigated mid-execution.**

| Not doing | Why |
| --- | --- |
| Reorder the existing factories | No scope item needs one, and `00-conventions.md` §4 rates a chain reorder `xlarge`. Both new factories are **inserted**, preserving every existing pair's relative order |
| Unify the two publish policies | `@Lenient` publishes unconditionally even when empty; `@Capture` only when non-empty. Unifying either way changes behaviour a consumer can observe. The store tolerates absence and each caller keeps its policy |
| Strip keys on an `@Extract` remainder | `@Capture`'s `literalPrefix` reconstruction round-trips only because all eleven filters in the module are a literal plus `^`. A `boolean strip()` is additive later, knowingly rather than by accident |
| Add a remainder over an array-shaped overflow | Coherent, zero sites, and it doubles the new surface. `Overflow.claim` returns nothing for an array-shaped entry, preserving today's silent no-op exactly |
| Add a class-level `@Flatten` | It would be the **first type-level annotation in `gson-extras`** - all eight are `@Target(FIELD)` and every factory's discovery model is a single field walk with `setProcessingSuperclass(false)`. It forces an answer to what an annotation on a superclass means, and either answer sets a precedent for the other seven |
| Fix the other four silent swallows | Stage 2 removes one - `LenientTypeAdapterFactory.java`:246-247 - and stage 4's branch B optionally gives a body to a second. The remaining three, including `PostInitTypeAdapterFactory.java`:37, are not claimed by any stage here. Note `postInitExceptionSwallowed_ok` `:1838` pins that catch as a **contract**, so changing it is a deliberate decision and not a cleanup |

**What would reopen any of this.** A second wrapper-key family appearing in `response/` turns
`@Flatten` from a one-site annotation into a reusable one. A site that genuinely needs `@Flatten`
together with `@Lenient` reopens the exclusion, and the answer there is a read-to-write channel rather
than a registration index - which is to say it becomes a dependent of the shared `Overflow` rather than
an independent file. And a `@Capture` filter carrying real regex metacharacters breaks
`literalPrefix` reconstruction for stage 4's branch B, which is an existing limit of the existing
mechanism rather than new fragility - but it is the one that would arrive without warning.
