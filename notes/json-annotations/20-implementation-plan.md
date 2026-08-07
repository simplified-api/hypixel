# 20 - Implementation plan

## 1. Scope and how to use this file

This is the only file in the pack that sequences work. It consumes the verdicts in
`10-annotation-designs.md` §21 and the per-implementor dispositions in `11-postinit-elimination.md`
§13, and turns them into ten stages that each leave the module compiling, tests passing and the
coverage gate no worse than it was.

Nothing here re-argues a decision. If a stage looks wrong, the argument is in the survey or design
document cited on the stage, not here.

**What the pack actually asks for, in one paragraph.** Sixteen registry proposals were examined and
fourteen were rejected or declined. The surviving library ask is **one JitPack cycle** carrying one
new annotation (`@Fallback`), one rider annotation (`@Flatten`), and a nine-line correctness fix to
`PostInitTypeAdapterFactory`. Everything else - and it is the overwhelming majority of the deleted
lines and every one of the fixed defects - is consumer-side work in
`Simplified-Api/hypixel`, using annotations that already ship.

**Stage id scheme.** `s20-<kebab-slug>`, frozen once written, per `00-conventions.md` §3. Cite a
stage by id, never by number, because a stage can be dropped without renumbering the rest.

**Effort vocabulary** is `00-conventions.md` §4 verbatim: `trivial` (1 consumer file, no library),
`small` (1-3 files or 1 additive library file), `medium` (4-15 files or an existing-factory edit),
`large`, `xlarge`. The library floor is `small` - no stage that touches `gson-extras` can be rated
`trivial`, and the one stage that edits an existing factory is `medium` by the scale regardless of
how small its diff is.

**Baseline measured at the time of writing**, so drift is visible:

| Measure | Value |
| --- | --- |
| `response/` file count | 133 |
| `implements PostInit` in `response/` | 6 |
| `scripts/json_dto_diff.py` unmapped keys | **792**, all under `SkyBlockMember.objectives` |
| Differ exit status | `1` - it is already a red gate, and stays red until `s20-objectives-catchall` |
| `gson-extras` pin (`build.gradle.kts`:44) | `strictly("7cfc181")` |
| `skyblock` pin (`build.gradle.kts`:38) | `strictly("33818f3")` |
| Test file | `src/test/java/api/simplified/hypixel/response/skyblock/MemberDtoMappingTest.java`, 293 lines, 16 tests |

## 2. The library cadence

`gson-extras` is consumed by git sha through JitPack. There is no local snapshot loop and no version
range, so **a library change and its consuming DTO change can never land in one commit**. The rule,
which applies to every stage that touches `gson-extras` and to the one stage that touches
`Simplified-Api/skyblock`:

1. Commit the library change in its own repo, on its own branch.
2. Push.
3. Trigger and wait for a JitPack build - `toolsmith jitpack build gson-extras` (or the
   `jitpack_build` MCP tool). Never hand-roll the `curl .../api/builds/...` round; the per-version
   endpoint silently *starts* a build and every retry is a real build on a third-party service.
4. Read the resulting sha with `toolsmith jitpack status gson-extras`.
5. Edit `build.gradle.kts`:44 in this module to that sha.
6. `toolsmith verify hypixel compileJava test` on the **unchanged** DTOs. This is the step people
   skip. A green build here proves the library change is behaviour-neutral for existing consumers
   before any consuming edit muddies the signal.
7. Only now land the consuming DTO change, as a separate commit.

Steps 1-6 are the library half; step 7 is the consumer half. **Both halves are separately
revertable, and step 6 is what makes that true.**

Two facts that shape the plan around this cadence.

**The pin is workspace-wide, not module-scoped.** `gson-extras` is also pinned by `persistence`,
`dataflow` and the sibling API modules. `dev/dataflow/serde/PipelineGson.java`:49 registers
`PostInitTypeAdapterFactory` in a second, independently built `Gson`, so the logging change in
`s20-library-cycle` reaches a module this pack never read. Run `toolsmith jitpack pins` before the
re-pin to see workspace-wide drift, and tell dataflow's owner rather than surprising them.

**The single library cycle is deliberately last, not first.** This is the one place the plan
knowingly inverts the "library first" instinct, and both design documents demand it
(`10-annotation-designs.md` §19.5, `11-postinit-elimination.md` §13.1). The library change makes
`PostInitTypeAdapterFactory`'s empty catch log. Today `Bestiary` and `AccessoryBag` throw on **every
single decode**, so enabling the log before the consumer fixes land produces one warning per member
per request and trains everyone to filter it out. The cadence in this section is absolute *within*
the stage; the stage's position in the ordering is set by that constraint.

The three-way consequence, stated so it is not mistaken for an oversight:

- Stages 1 to 8 touch **one module**, cost **zero JitPack cycles**, and cannot be broken by a
  JitPack failure, a pin conflict or a sibling rebuild.
- `s20-library-cycle` is the only stage with a two-repository rollback.
- `s20-skyblock-election` is a second repository's cycle and is explicitly *not* a dependency of
  anything before it.

## 3. Stage ranking

Ordered by payoff per unit of effort, cheapest first, which is also the execution order. Where a
dependency forces a stage above or below its natural rank the **Blocked by** column says so.

| # | Stage id | Effort | Payoff (lines / classes / fields) | Library cycle | Blocked by |
| --- | --- | --- | --- | --- | --- |
| 1 | `s20-dark-feature-fixes` | `trivial` x9 | 0 net lines; **4 dark features turned back on**, 1 auction field starts binding, 791 quest statuses stop binding to `null`, 13 fields become readable | no | none |
| 2 | `s20-free-retirements` | `trivial` | 2 `PostInit` implementors, 11 lines of hook, 6 fields, 2 suppressions, 1 encapsulation leak | no | none |
| 3 | `s20-holder-collapse` | `small` | **7 classes + 1 file deleted, ~108 net lines**, 10 forwarders, 2 unreachable data paths recovered | no | none |
| 4 | `s20-objectives-catchall` | `small` | +4 lines; **792 unmapped keys to 0** - the coverage gate turns green | no | stage 1 |
| 5 | `s20-existing-annotation-sweep` | `small` | 4 classes, ~55 lines, 29 `Object` fields typed, 5 fields to 1, 1 memo + filter deleted | no | none |
| 6 | `s20-shape-retirements` | `small` | 2 `PostInit` implementors, 36 lines of hook, 3 input-only fields, 810 eager allocations, 2 phantom serialized keys | no | none |
| 7 | `s20-derivation-retirements` | `medium` | **2 `PostInit` implementors - the last two**, 42 lines of hook, 1 package import cycle, 77,500 regex compilations per member | no | stage 1 |
| 8 | `s20-duplication-sweep` | `medium` | 2 files, ~200 lines, 23 duplicated weight lines, 24 duplicated `of(String)` lines, 4 tier columns | no | stages 6, 7 |
| 9 | `s20-library-cycle` | `medium` | 24 exposed enum sites stop binding to `null`; 1 map-of-maps collapsed; every future `postInit()` failure becomes visible | **yes, 1** | stages 1, 7 |
| 10 | `s20-skyblock-election` | `small` | the 7th `PostInit` implementor, in another repo | **yes, 1** (skyblock) | none |

**Cumulative `implements PostInit` count**, which is the pack's headline metric:

| After stage | Remaining in `response/` |
| --- | --- |
| baseline | 6 |
| `s20-free-retirements` | 4 |
| `s20-shape-retirements` | 2 |
| `s20-derivation-retirements` | **0** |
| `s20-skyblock-election` | 0 workspace-wide |

**Cumulative coverage gate** (`scripts/json_dto_diff.py` unmapped-key count):

| After stage | Unmapped | Exit |
| --- | --- | --- |
| baseline | 792 | `1` |
| `s20-objectives-catchall` | **0** | `0` |
| every stage after that | 0 | `0` - regressions are now a hard failure |

Two things this table deliberately does not claim. The line counts are net deletions from the
surveys, not measured diffs; treat them as ranking input rather than as acceptance criteria. And
`s20-duplication-sweep` is the one stage with no defect fix and no `PostInit` scalp in it - it is
pure polish and it is ranked last among the consumer stages for that reason.

## 4. Stage 1 - s20-dark-feature-fixes

**Goal.** Stop the module producing silently wrong output, before anything is restructured. Every
later stage is verified by comparing against values this stage makes real for the first time. Nothing
here is a refactor; no `implements PostInit` is removed and no field is retyped.

**Effort.** `trivial` x9 - nine independent one-file changes, each its own commit. Counted as one
change it is `medium` on file count, which is why it is nine commits rather than one.

**Library change.** None. No JitPack build, no re-pin, `build.gradle.kts` untouched.

**Files touched** (repo-relative to `Simplified-Api/hypixel`):

| # | File | Change | Finding |
| --- | --- | --- | --- |
| 1 | `src/main/java/api/simplified/hypixel/response/skyblock/member/AccessoryBag.java` | move the `contents` assignment (`:138`) above the parse at `:57`; assign `calculatedMagicalPower` into `this.magicalPower` (`:129-136`) | `f02-accessorybag-dead-initialize`, `f01-accessorybag-order-inversion` |
| 2 | `src/main/java/api/simplified/hypixel/response/skyblock/member/Bestiary.java` | carry one matcher through the stream so `group(1)`/`group(2)` (`:61-63`) are only reached after a successful `matches()` | `f05-matcher-group-without-match` |
| 3 | `src/main/java/api/simplified/hypixel/response/skyblock/member/Bestiary.java` | widen `MOB_PATTERN` (`:29`) from `^([a-z_]+)_([0-9]+)$` to `^(.*)_([0-9]+)$` | `f01-bestiary-lazy-families` |
| 4 | `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonRun.java` | same matcher fix at `:46`, `:50`, `:54` | `f05-matcher-group-without-match` |
| 5 | `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java` | lowercase both master-mode literals (`:58`, `:63-66`) | `f01-dungeons-master-case`, `f05-dungeons-master-pairing` |
| 6 | `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/BoardQuest.java` | rename `Status.COMPLETED` to `COMPLETE`, add `INACTIVE` (`:20`) | `f06-boardquest-complete-status` |
| 7 | `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockAuction.java` | `@SerializedName("starting_big")` to `"starting_bid"` (`:41`) | `f06-serialized-name-misses` |
| 8 | `.../member/hoppity/ChocolateShop.java`, `ChocolateTimeTower.java`, `RabbitHitman.java` | add the missing class-level `@Getter` | `f06-hoppity-unreadable-fields` |
| 9 | `src/main/java/api/simplified/hypixel/response/skyblock/member/JacobsContest.java` | add `transient` to `skyBlockDate` and `collectionName` (`:108-109`) | `f06-jacobscontest-derived-nontransient` |

**Deliberately held back.** `CommissionData.totalCompleted` (`response/skyblock/garden/CommissionData.java`:16)
is the weaker half of `f06-serialized-name-misses` - the endpoint is not in the fixture, so the
differ cannot see it and the upstream key is inferred rather than read. Resolve it against a live
`/skyblock/garden` response before touching it; do not guess `total_completed` into the source.

**Why the order inside the stage matters exactly once.** Fix 5 must not be "add `MASTER_CATACOMBS`
to the enum" - that is the first diagnosis everyone reaches for and it fixes nothing, because the
case-sensitive filter at `:58` still lets the lowercase key through. Compare case-insensitively in
both places, or normalise the key space once. `s20-shape-retirements` replaces this code entirely;
the two-character fix is worth landing anyway because it is a live data-loss bug on a shipping path.

**Verify.**

```
toolsmith verify hypixel compileJava test
python scripts/json_dto_diff.py --section objectives
```

New assertions in `MemberDtoMappingTest`, all against the populated member. These are the acceptance
values every later stage compares against, so write them down rather than eyeballing:

- `bestiary.getFamilies()` is non-empty. It has been empty for every profile ever decoded.
- `dungeons.getDungeon(DungeonData.Type.CATACOMBS).getFloorData(true)` carries
  `highestTierCompleted = 7`, and `dungeons.getDungeons()` has **no** `Type.UNKNOWN` key.
- `accessoryBag` decoded standalone still passes the existing `mapsTuningSlots` test unchanged - the
  reorder must not disturb the isolated-decode path at `MemberDtoMappingTest.java`:111.
- A `BoardQuest` decoded from `{"status":"COMPLETE","progress":3,"completed_at":0}` has
  `getStatus() == Status.COMPLETE`. Today it is `null`.
- A `SkyBlockAuction` with `starting_bid` binds a non-zero `getStartingBid()`.

The differ's unmapped count stays at 792 through this stage - the `objectives` node is not addressed
until `s20-objectives-catchall`. Run it anyway so a *regression* is caught: any number above 792 is
a failure.

**One thing this stage cannot verify, and must not pretend to.** `AccessoryBag.getMagicalPower()`
only becomes non-zero when `initialize` is actually called, which happens inside
`SkyBlockMember.postInit()` - a path that also needs a live JPA session for `Bestiary`. Assert the
magical-power value in a dedicated test that constructs the bag and calls `initialize` directly,
rather than trying to decode a whole `SkyBlockMember`. The whole-member decode becomes testable in
`s20-derivation-retirements`, and that is where the end-to-end assertion belongs.

**Rollback.** Nine independent `git revert`s in one module. No published artifact, no pin to unwind,
no cross-file dependency between the nine. Reverting the `Bestiary` matcher fix does not disturb the
`Dungeons` case fix.

**Estimate.** AI-assisted elapsed **45-75 minutes**; human-developer **4-6 hours** (most of it
re-reading `AccessoryBag.initialize` to be sure the reorder is safe).

## 5. Stage 2 - s20-free-retirements

**Goal.** Retire the two `PostInit` implementors that need no annotation, no retype and no
correctness prerequisite, and in doing so prove the two patterns every later retirement uses - the
computed accessor, and the sibling rename.

**Effort.** `trivial` x2 - `Election` is one file, `CrimsonIsle`/`Kuudra` is two. Two commits.

**Library change.** None. No JitPack build, no re-pin.

**Files touched:**

- `src/main/java/api/simplified/hypixel/response/skyblock/election/Election.java`
- `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/CrimsonIsle.java`
- `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/Kuudra.java`

**Commit 1 - `Election`** (`d11-election-computed-cycles`, `11-postinit-elimination.md` §10.2).
`voting` and `term` are pure functions of one bound `int`. Delete both transient fields, the 9-line
`postInit()` body, the `this.postInit()` call in the `Election(int)` constructor (`:24`),
`implements PostInit` and the `PostInit` import. Add `getVoting()` and `getTerm()` as computed
accessors. `SpecialElection` and `VotingBooth` extend `Election` and inherit the fix with no edit.

The one trap, and it is the reason this is not a mechanical delete: `Election` hand-writes
`equals`/`hashCode`/`toString` (`:27-58`) and all three route through `getVoting()`/`getTerm()`.
`Cycle` declares no `equals`, so that comparison is **already** by identity and already returns
`false` for two separately-constructed elections of the same year. **Drop the derived values from
identity** - `equals` becomes a `year` comparison, `hashCode` becomes `Objects.hash(this.getYear())`.
Do not memoise the accessors to preserve the old behaviour; that keeps a broken `equals` working by
accident. This overrides `f01-election-lazy-cycles`, which recommended memoising, on the ground
stated in `11-postinit-elimination.md` §10.3.

**Commit 2 - `CrimsonIsle` and `Kuudra`** (`d11-crimsonisle-sibling-rename`, §9.2). The JSON puts
`kuudra_party_finder` beside `kuudra_completed_tiers` as a sibling; the Java decided `Kuudra` owns
both and pushed two objects down a level to make it true. Stop pushing. Rename the two snake-case
staging fields to `partyFinderSearch` and `partyFinderGroupBuilder`, drop both
`@Getter(AccessLevel.NONE)` suppressions, delete the 2-line `postInit()`, `implements PostInit` and
the `PostInit` import; on `Kuudra`, delete both package-private `transient` fields (`:22-23`).

Round-trip fidelity **improves** here rather than merely surviving: the two fields stay bound and
non-`transient`, where the old `Kuudra` transients were never serialized at all. Nothing outside
`CrimsonIsle` reads `Kuudra.getSearchSettings()` or `getGroupBuilder()` - confirmed by a
workspace-wide symbol search - so no forwarder is owed.

**Verify.**

```
toolsmith verify hypixel compileJava test
python scripts/json_dto_diff.py --section nether_island_player_data
```

Assertions to add to `MemberDtoMappingTest`:

- `new Election(278).getVoting()` and `.getTerm()` equal the pre-change `Cycle` bounds for a fixed
  year. Capture those two values *before* the edit; they are the only regression guard.
- Two `Election` instances with the same year are now `equals`, and hash to the same bucket.
- `CrimsonIsle` decoded from the fixture exposes non-default `partyFinderSearch` and
  `partyFinderGroupBuilder`, and the existing `mapsCrimsonIsle` test still passes unchanged.
- Serialize the decoded `CrimsonIsle` and assert the `kuudra_party_finder` object survives with both
  sub-objects. This is a new capability - the old shape never emitted it.

The differ must still report 792 and no more. Both changes rename Java fields without changing which
JSON keys are claimed, so a count change here means something bound differently and is a bug.

**Rollback.** Two independent `git revert`s. Commit 2 spans two files and must revert as one unit -
`Kuudra`'s transient fields and `CrimsonIsle`'s `postInit()` are two halves of one shape.

**Estimate.** AI-assisted elapsed **30-45 minutes**; human-developer **2-3 hours**.

## 6. Stage 3 - s20-holder-collapse

**Goal.** Delete the nested-holder idiom. Seven private holder classes plus one whole file exist only
to name a statically keyed JSON sub-object; `@SerializedPath` already binds any value type at any
depth and re-nests it on write. This is the largest line payoff in the pack and it costs no library
cycle at all.

**Effort.** `small` - 8 consumer files, zero library files. Above `trivial` on file count only.

**Library change.** None. `@SerializedPath` ships today and is already used in this module.

**Files touched:**

| File | Holder removed | Fields relocated | Forwarders deleted |
| --- | --- | --- | --- |
| `.../skyblock/SkyBlockMember.java` | `Profile` (`:220-230`), `Events` (`:212-218`) | 4 | 5 |
| `.../skyblock/member/foraging/Temples.java` | the whole file | 1 | 1 |
| `.../skyblock/member/dungeon/Dungeons.java` | `DungeonTreasures` (`:140-146`) | 2 | 2 |
| `.../skyblock/member/rift/Rift.java` | `Porhtal` (`:48-54`) | 1 | 1 |
| `.../skyblock/member/rift/VillagePlaza.java` | `Lonely` (`:75-81`), `Seraphine` (`:83-89`) | 2 | 2 |
| `.../skyblock/member/attribute/AttributeShards.java` | `Traps` (`:17-23`) | 1 | **0** |
| `.../skyblock/member/Bestiary.java` | `Miscellaneous` (`:141-150`) | 2 | **0** |

The two zero-forwarder rows are `f04-holder-private-type-leak` / `f03-unreachable-private-holders`,
and they are the reason this stage is `correctness` as well as cleanup: both keep the class-level
`@Getter`, so Lombok emits a public accessor whose return type is a `private static` nested class.
Verified by javac reproduction - `error: Holder.getValue() is defined in an inaccessible class or
interface`. `shards.traps.active_traps` is an eleven-field `ActiveTrap` list that is parsed on every
profile fetch and thrown away, and `bestiary.miscellaneous`'s two booleans are likewise dead. This
stage is the first time any caller can read them.

The shape, using `Rift`:

```java
// before - Rift.java:23-25 plus 44-54: a suppressed field, a forwarder and a holder class
// after
@SerializedPath("wither_cage.killed_eyes")
private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();
```

`getKilledEyes()` survives, Lombok-generated, with the same signature. Every other site follows the
same form: `profile.first_join`, `profile.personal_bank_upgrade`, `profile.cookie_buff_active`,
`events.easter`, `temples.unlocked_temples`, `treasures.runs`, `treasures.chests`,
`lonely.seconds_sitting`, `seraphine.step_index`, `traps.active_traps`,
`miscellaneous.max_kills_visible`, `miscellaneous.milestones_notifications`.

**Held back deliberately - `HypixelPlayer.Stats`.** It converts through the same annotation but is
not the paired idiom: `HypixelPlayer.java`:80 has no suppression and no forwarder, and
`Stats.SkyBlock` (`:132`) hand-writes `getProfiles()` (`:136-138`), so collapsing relocates an
accessor and a nested class and changes two public accessor shapes. It goes in
`s20-existing-annotation-sweep` as its own commit. Cite the union - **11 classes across 8 files** -
whenever both sets are meant; a bare "nine" is ambiguous between two surveys.

**Three risks, all real and all cheap to check.**

1. `Rift`, `VillagePlaza` and `AttributeShards` carry no `@SerializedPath` today, so
   `SerializedPathTypeAdaptorFactory.create` returns the bare delegate for them (`:39-41`). After
   this stage they get wrapped, which materialises the sub-tree as a `JsonObject` and re-parses it
   (`:101-103`). `SkyBlockMember`, `Dungeons` and `Bestiary` already pay this on far larger objects,
   so the marginal cost is small - **measure it once, do not assume it**.
2. The flat key used on write is the `@SerializedName` value or the field name
   (`SerializedPathTypeAdaptorFactory.java`:160). Two `@SerializedPath` fields on one class must not
   share a flat key, and a flat key must not collide with a real top-level JSON key. None of the 13
   proposed names collide; the constraint is invisible in source and will bite a later edit.
3. `SkyBlockMember.getFirstJoin()` narrows covariantly from `SkyBlockDate` to
   `SkyBlockDate.RealTime`. No caller can observe it, but **keep this stage away from
   `s20-derivation-retirements`**, which also edits `SkyBlockMember`. Two migrations in one file in
   one window is how a covariant narrowing gets blamed on a lazy accessor.

**Verify.**

```
toolsmith verify hypixel compileJava test
python scripts/json_dto_diff.py
```

Assertions, and one of them is the whole point of the stage:

- **Round-trip a three-field shared prefix.** Decode a member, serialize it, assert the output
  carries one `profile` object holding all three of `first_join`, `personal_bank_upgrade` and
  `cookie_buff_active`. `SerializedPathTypeAdaptorFactory.java`:80-86 reuses an existing nested
  object rather than overwriting it, so the first field creates `profile` and the other two find it.
  That behaviour was read closely and **never executed**; this test is a prerequisite of the stage,
  not a nicety.
- `member.getUnlockedTemples()`, `getChocolateFactory()`, `getFirstJoin()`, `isBoosterCookieActive()`,
  `dungeons.getRuns()`, `dungeons.getChests()`, `rift.getKilledEyes()`,
  `villagePlaza.getSecondsSitting()`, `villagePlaza.getSeraphineStepIndex()` all return the
  pre-change values. Capture them before the edit.
- `shards.getActiveTraps()` and `bestiary.isMaxKillsVisible()` compile and are readable from the test
  package. They could not be before.

The differ count must stay at exactly 792. This stage changes *how* keys are claimed for 13 fields;
a count change means one stopped binding.

**Rollback.** One `git revert` per file, in any order - the eight files do not reference each other's
holders. `Temples.java` is a file deletion, so its revert restores the file and the import at
`SkyBlockMember.java`:11.

**Estimate.** AI-assisted elapsed **1.5-2.5 hours**; human-developer **1-1.5 days**.

## 7. Stage 4 - s20-objectives-catchall

**Goal.** Take the coverage gate from 792 unmapped keys to 0 with roughly four lines. Every one of
the 792 is under `SkyBlockMember.objectives`, and 789 of them have `BoardQuest`'s exact shape.

**Effort.** `small` - 3 consumer files, no library change. The rename is what lifts it above
`trivial`.

**Library change.** None. `@Capture` with `Grouping.ENTRY` ships today.

**Files touched:**

- `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java` (`:137`)
- `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/BoardQuest.java`
- `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/QuestBoard.java` (`:17`)

**The changes.**

1. **Rename `BoardQuest` to a neutral domain name.** It is about to be shared by `QuestBoard`, which
   holds five, and `SkyBlockMember.objectives`, which holds hundreds. The registry reserves no DTO
   names, so pick one and use `toolsmith:java-file-mover` / IntelliJ `rename_refactoring` rather than
   `sed` - the type is referenced from two files plus the test.
2. **Add the catch-all** on `SkyBlockMember`:

```java
@SerializedPath("objectives")
@Capture(grouping = Capture.Grouping.ENTRY)
private @NotNull ConcurrentMap<String, BoardQuest> objectives = Concurrent.newMap();
```

`Grouping.ENTRY` is load-bearing and is the recently added mode. Without it, affix grouping tries to
split objective ids such as `talk_to_david_5` against the value class's field names and produces
nonsense. With it, each value is read whole.

3. **Absorb the six item-bearing objectives.** The fixture's value shapes are
   `{status, progress, completed_at}` x789, `{..., completions}` x2, and
   `{..., <itemId>: n, ...}` x6 carrying collection requirements such as `CARROT_ITEM` and
   `INK_SACK:3`. A `@Lenient ConcurrentMap<String, Integer>` overflow field on the value class
   absorbs the last group and round-trips it.

**The prerequisite is not optional.** `s20-dark-feature-fixes` fix 6 - `COMPLETED` to `COMPLETE`,
plus the unmodelled `INACTIVE` - **must already be in**. The wire vocabulary across every
`objectives` entry is `COMPLETE` x791, `ACTIVE` x19, `INACTIVE` x1, and `COMPLETED` appears nowhere
in the document. Land this stage first and 791 of 811 statuses bind to `null` on a `@NotNull` field.
That is the interaction `f06-objective-status-shape` flags: today the typo affects 5 objects, after
this stage it would affect 800.

**One thing to check before writing the field.** `objectives.tutorial` is a `List<String>`, not an
objective object, and `SkyBlockMember.java`:137 already binds it with
`@SerializedPath("objectives.tutorial")`. Two routes, and the second is preferred: either exclude it
with a `@Capture` filter, or keep the existing `@SerializedPath` field and let it claim the key
before the catch-all sees it - which is the declared `@Capture` precedence, since
`CaptureTypeAdapterFactory.discoverKnownKeys` already reads the first segment of a `@SerializedPath`
(`:118-127`). Confirm which by test rather than by reading; it decides whether the field needs a
filter.

**Verify.**

```
toolsmith verify hypixel compileJava test
python scripts/json_dto_diff.py
```

This is the stage where the differ becomes a real gate, so the assertions are about it:

- `python scripts/json_dto_diff.py` reports **0 unmapped keys and exits 0**. From here on, a non-zero
  exit in any later stage is a hard failure rather than a known baseline.
- `member.getObjectives()` has 811 entries for the populated member, and
  `getObjectives().get("<a completed objective id>").getStatus()` is `COMPLETE`, not `null`.
- `member.getTutorialObjectives()` still returns the same `List<String>` it did before - the
  precedence check above, expressed as a test.
- Serialize the member and assert the `objectives` object round-trips with its key count intact,
  including the six item-bearing entries whose extra keys went to `@Lenient` overflow.

**Rollback.** One `git revert`. The rename is the awkward half - revert it with the same refactoring
tool rather than by hand, or the test imports drift. If only the catch-all needs to go and the rename
is worth keeping, revert the `SkyBlockMember` hunk alone; the two are independent.

**Estimate.** AI-assisted elapsed **1-1.5 hours**; human-developer **4-6 hours**.

## 8. Stage 5 - s20-existing-annotation-sweep

**Goal.** Apply the annotations that already ship at every remaining site the surveys found. Six
independent commits, none of which depends on another, all of which delete hand-written
deserialization.

**Effort.** `small` - 7 consumer files across six commits, zero library files.

**Library change.** None. `@Lenient`, `@Extract`, `@Split`, `@Capture`, `@SerializedPath` and stock
Lombok all ship today.

**Files touched and what lands in each commit:**

| # | File | Change | Finding |
| --- | --- | --- | --- |
| 1 | `.../skyblock/member/Statistics.java`:39-40 | `@Lenient ConcurrentMap<String, Integer> riftStats` plus `@Extract("riftStats.west_vermin_vacuumed")` into a new 4-field `VerminVacuumed` class | `f03-object-escape-hatches` |
| 2 | `.../skyblock/member/pet/OwnedPet.java`:40 | `extra` from `Map<String, Object>` to `@Lenient ConcurrentMap<String, Long>` | `f03-object-escape-hatches` |
| 3 | `.../hypixel/HypixelPlayer.java`:42-43, :82-91 | `achievementsOneTime` to `@Lenient ConcurrentList<String>`; **deletes the hand-written filter and its memo field** | `f03-object-escape-hatches` |
| 4 | `.../skyblock/member/crimson/Kuudra.java`:42-50 | `@Split("-")` onto `PairOptional<Integer, Integer> combatLevel`; `getCombatLevel()` shrinks to building the `Range` | `f03-kuudra-combat-range` |
| 5 | `.../skyblock/member/dungeon/FloorData.java`:45-60, :70-79 | five `most_damage_*` fields plus a 10-line `switch` to `@Capture(filter = "^most_damage_")` into `ConcurrentMap<DungeonClass.Type, ConcurrentMap<Floor, Double>>` | `f04-floordata-most-damage-switch` |
| 6 | `.../skyblock/member/foraging/HeartOfTheForest.java`:45-65 | `@Getter(AccessLevel.NONE)` on `tiers`, and `getSpent(int)` reads `this.tiers` instead of `this.getTiers()` | `f03-biomewhispers-tier` |
| 7 | `.../hypixel/HypixelPlayer.java`:80, :126-153 | the `Stats` / `Stats.SkyBlock` chain to `@SerializedPath("stats.SkyBlock.profiles")`; relocates `getProfiles()` and the nested `Profile` class | `f03-holder-collapse-serializedpath` |
| 8 | `.../skyblock/member/crimson/CrimsonIsle.java`:65-66 | `questRewards` to `@Lenient ConcurrentMap<String, Integer>` - the **free partial** | `f03-questrewards-mixed-values` |

Commit 6 is **two lines, not one**, and getting it wrong stops the class compiling: `getSpent(int)`
currently calls `this.getTiers()`, which is exactly the accessor the suppression deletes. This is a
deliberate *decline* of the `@Flatten` collapse - the whispers key family is demonstrably mid-growth
(one profile's `desert` has a `total` key the other's does not), so `Tier` is the cheapest place to
absorb a future key. Suppressing the getter makes the shape freely reversible at any later date,
which is what the collapse was trying to buy.

Commit 8 takes the partial deliberately: `@Lenient ConcurrentMap<String, Integer>` types the reward
counts today and parks the quest-to-item mapping in overflow, where it round-trips but is not
readable. That is better than `Object` for half the data at zero cost. **Do not build the general
overflow-typing element on `@Lenient` for this one site** - see §17.

**Six `Object` fields stay `Object`.** `CrimsonIsle.java`:68, :70, :93, `VillagePlaza.java`:22,
`CrystalHollows.java`:29 and :38 are all `{}` or `[]` in the fixture. Guessing a type from an empty
container is how `@Lenient` overflow silently fills up.

**Risk on commit 5.** `FloorData` has no `@Capture` today, so it gains the `CaptureTypeAdapterFactory`
wrapper. A filtered `@Capture` must re-prefix its keys on write; confirm that against the existing
`Kuudra.java`:18 user (`^highest_wave_`) rather than assuming it. The two neighbouring keys that
could be captured by accident, `most_healing` and `most_mobs_killed`, do not match `^most_damage_`.

**Verify.**

```
toolsmith verify hypixel compileJava test
python scripts/json_dto_diff.py
```

Differ must stay at **0** - commits 1, 3, 5, 7 and 8 all change which fields claim which keys, and
this is the first stage where the gate is green enough to catch that.

Assertions:

- `statistics.getRiftStats().get("<any int-valued rift key>")` is an `Integer`, the map has 29
  entries, and `statistics.getVerminVacuumed()` is populated. Serialize and assert both
  object-valued entries survive through `@Lenient` overflow.
- `kuudra.getSearchSettings().getCombatLevel()` is `Range[5, 10]` for the fixture value, and a
  malformed `"abc"` yields an empty range rather than throwing - the behaviour change `@Split` buys.
- `floorData.getMostDamage(DungeonClass.Type.MAGE)` returns the floor-keyed map the old `switch`
  returned, and `getMostDamage(Type.UNKNOWN)` returns empty rather than hitting a `default` arm.
  Round-trip `FloorData` and assert all five `most_damage_*` keys come back prefixed.
- `heartOfTheForest.getSpent(1)` is unchanged and `getTiers()` no longer compiles from the test
  package.
- `hypixelPlayer.getProfiles()` returns the same list it did before the `Stats` collapse.
- `player.getAchievementsOneTime()` returns only strings, and the memo field is gone from the source.

**Rollback.** Eight independent `git revert`s. Commit 7 is the only one that changes a public
accessor shape (`getStats()` disappears), so revert it first if the stage is being unwound partially.

**Estimate.** AI-assisted elapsed **2-3 hours**; human-developer **1-1.5 days**.

## 9. Stage 6 - s20-shape-retirements

**Goal.** Retire the two `PostInit` implementors whose bodies were never computations at all - they
were bridging a field declaration that was written one level too deep. Four of six gone after this.

**Effort.** `small` x2 - `JacobsContest` is one file, `Dungeons` is three. Two commits.

**Library change.** None. `@Collapse` and `@Key` ship today and `Slayers` is the working user.

**Files touched:**

- `src/main/java/api/simplified/hypixel/response/skyblock/member/JacobsContest.java`
- `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`
- `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonData.java`
- `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonClass.java`

**Commit 1 - `JacobsContest`** (`d11-jacobscontest-collapse-key`, `11-postinit-elimination.md` §7.2).
`contestMap` becomes `@Collapse @SerializedName("contests") ConcurrentList<Contest> contests`, and
`Contest` gains `@Key private transient @NotNull String id = ""`. The 16-line `postInit()`, the
`@Getter(AccessLevel.NONE)` staging map, the transient republished list, `implements PostInit` and
the `PostInit` import all go. `getSkyBlockDate()` and `getCollectionName()` become lazy accessors on
`Contest`, parsing the injected key.

**Do not encode the key parse as an annotation element.** `entry.getKey().split(":")` is unlimited,
so `278:1_2:INK_SACK:3` splits into **four** parts, not three - twenty-plus of the fixture's 810
keys are that shape, every brown-dye contest. A `parts = 3` splitter or a `Pair`-shaped `@Split`
truncates the collection id to `INK_SACK` and passes every test written against the other 790 keys.
`StringUtil.join(parts, ":", 2, parts.length)` says it in one line.

The parse stays unguarded against a key with fewer than three segments, deliberately.
`getSkyBlockDate()` throwing on one malformed id is a visible failure at one contest, which is
strictly better than today, where one bad key throws inside the stream, the empty catch eats it, and
`contests` is empty for the **whole member**. Guard it when a malformed key is observed; do not guard
it speculatively and swallow the result.

**Commit 2 - `Dungeons`** (`d11-dungeons-retype-and-lazy`, §6.3). Two unrelated halves:

- `classes` - **delete the derivation, do not make it lazy.** `DungeonClass` declares exactly one
  field, `double experience`, and the JSON under `player_classes` is already that shape. Retyping
  `classMap` to `ConcurrentMap<DungeonClass.Type, DungeonClass> classes` binds directly. The funnel
  field, its suppression, the transient output and six lines of hook all go. Zero annotations.
- `dungeons` - a memoised `getDungeons()` that lowercases the key space **once**, so the filter and
  the master lookup share one spelling by construction rather than by two authors agreeing. This
  supersedes `s20-dark-feature-fixes` fix 5, which was the two-character stopgap.

Both halves need `DungeonData` to lose its `experience` field first, in the same commit. The hook
passed `value.getExperience()` as the first constructor argument and `value` as the second, so the
field was always `getNormalMode().getExperience()`. Drop it, narrow the constructor to
`(normalMode, masterMode)`, update `EMPTY_DUNGEON` (`:25`), and make `getExperience()` an
`{@inheritDoc}` override delegating to `getNormalMode()`. This is a prerequisite of the `@Capture`
route too, so it is not wasted either way.

**Two things to verify rather than assume, both cheap.** `DungeonClass` has
`@RequiredArgsConstructor` and no no-arg constructor, so Gson will instantiate it through
`UnsafeAllocator` and set a `final` field reflectively. That works and `DungeonData` already relies
on the same path, but this is `DungeonClass`'s first trip through the reflective adapter - assert it.
The safer form, matching what `FloorData` already does, is to drop `final` and add
`@NoArgsConstructor(access = AccessLevel.PRIVATE)`.

**The `@Capture` route is a follow-up, not this stage** (`11-postinit-elimination.md` §6.4).
`@Capture(descend = true)` with an empty filter, plus `@SerializedName("")` / `@SerializedName("master_")`
on `DungeonData`, would make the pairing a declaration and the case defect structurally unreachable.
It is gated on a spike - `Capture.java`:96-116 documents `descend = true` only alongside a non-empty
filter, and a bare `@Capture` is documented as a catch-all limited to one per class. **Keep the spike
off the critical path**; §6.3's lazy form retires the implementor without it, and both routes cost
zero library cycles so there is no cycle to save by choosing early.

**Verify.**

```
toolsmith verify hypixel compileJava test
python scripts/json_dto_diff.py
```

The differ matters more here than anywhere else - `@Collapse` on `contests` and the `player_classes`
retype both change which fields claim which keys. It must stay at **0**.

Assertions:

- `jacobsContest.getContests()` has **810** entries for the populated member.
- `getCollectionName()` on a brown-dye contest is `INK_SACK:3`, not `INK_SACK`. This is the single
  assertion that guards the whole key-parse argument.
- `getSkyBlockDate()` on a known contest equals the pre-change value.
- Serializing a `Contest` emits **no** `skyBlockDate` and no `collectionName` key. Both were
  non-`transient` fields until now and polluted every round trip.
- `dungeons.getClasses()` has 5 entries with the pre-change experience values, and
  `getClassAverage()` / `getClassExperience()` / `getClassWeight()` are unchanged - they already
  route through `getClasses()`.
- `dungeons.getDungeons()` has exactly one key, `CATACOMBS`, and
  `getDungeon(CATACOMBS).getFloorData(true).getHighestTierCompleted()` is `7`. Compare against the
  values `s20-dark-feature-fixes` captured.

**Rollback.** Two `git revert`s. Commit 2 spans three files and reverts as one unit - `DungeonData`'s
constructor narrowing and `Dungeons`' retype are one change. Note `EMPTY_DUNGEON` is
`DungeonData`'s only external caller, so nothing outside these three files moves.

**Estimate.** AI-assisted elapsed **1.5-2.5 hours**; human-developer **6-8 hours**.

## 10. Stage 7 - s20-derivation-retirements

**Goal.** Retire the last two `PostInit` implementors by making their derivations lazy. This is the
largest stage, the only one that changes a public method signature, and the one that takes
`response/` to **zero** `implements PostInit`.

**Effort.** `medium` - 4 consumer files plus the test, and a public signature change. Two commits.

**Library change.** None.

**Blocked by `s20-dark-feature-fixes`, absolutely.** `SkyBlockMember.postInit()` currently throws on
its **first** statement for every member of every profile, so statements 2 and 3 have never run;
`Bestiary.families` has never been non-empty. Converting either class before those are fixed means
comparing new output against nothing at all. The correctness stage exists to give this stage a real
expected value.

**Files touched:**

- `src/main/java/api/simplified/hypixel/response/skyblock/member/Bestiary.java`
- `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`
- `src/main/java/api/simplified/hypixel/response/skyblock/member/AccessoryBag.java`
- `src/test/java/api/simplified/hypixel/response/skyblock/MemberDtoMappingTest.java`

**Commit 1 - `Bestiary`** (`d11-bestiary-lazy-families`, `11-postinit-elimination.md` §8.2). The
27-line body becomes a memoised `getFamilies()`. The computation does **not** shrink - it is a
repository-shaped join and stays exactly as long. What changes is that *binding* stops depending on a
live JPA session. `SkyBlockData.getRepository` delegates to a static session manager; with no session
it throws, the empty catch eats it, and `families` is permanently empty - a session coming up later
changes nothing, because the assignment already happened. Lazily, a missing session is a recoverable,
observable error at the point of use.

Memoisation is not optional here, unlike elsewhere: `Family.getType()`, `getTiers()`, `getBracket()`
and `getMaxTier()` (`:92-121`) each re-query the repository and `getUnlocked()`/`getMilestone()` walk
every family, so an unmemoised accessor would be markedly slower than today's broken version.

**Commit 2 - `SkyBlockMember` and `AccessoryBag`** (`d11-skyblockmember-wire-on-access`, §5.4). Three
statements, three different answers:

- **`accessoryBag.initialize(this)` becomes a wire-on-access accessor.** Narrow `initialize` from a
  `SkyBlockMember` to the three values it actually consumes - the talisman bag `NbtContent`, the
  rift prism boolean, and the abiphone contact count - returning `this`. Suppress Lombok's
  `getAccessoryBag()` and hand-write it to call `initialize` with those three. Everything the old
  110-line body computed becomes a memoised accessor on the bag over those three stores plus its own
  bound fields.
- **`new Skills(...)` becomes a memoised `getSkills()`.** This alone fixes the FORAGING subtractor,
  which is unconditionally `2` today because `SkillLevel` reads `collectionUnlocked` two statements
  before it is assigned. Once both are lazy, `getSkills()` calls `getCollectionUnlocked()`, which
  computes from two bound fields - **the call stack performs the topological sort**.
- **`collectionUnlocked` becomes a memoised one-pass index.** Splitting each tier string at its
  **last** underscore is provably equivalent to the per-item regex, including the adversarial
  `LOG` / `LOG_2` pair, and turns 100 x 775 into one pass over 775 plus 100 lookups - 77,500
  `Pattern` compilations per member per decode removed.

Keep `if (tier < 0) continue;` **visible**. The old regex `[\\d]+` never matched a leading minus, so
83 of 775 `_-1` entries were skipped, and `f05-negative-tier-exclusion` proves that is correct - every
id carrying a `_-1` also carries `_1`..`_9`, and every downstream consumer compares against positive
thresholds. State it as a line of code with a comment, not as a character class. **Do not widen the
regex to `-?[\d]+`**; see §17.

**What this commit buys beyond the retirement.** The `response.skyblock` to `response.skyblock.member`
package import cycle breaks - `AccessoryBag.java`:5 stops importing `SkyBlockMember`, which a typed
owner field could never have delivered. The read-before-assign and the dead store become *unwritable*
rather than merely fixed, because there is no second statement and no local to leave a value in. And
standalone decode is unaffected: `MemberDtoMappingTest.java`:111 decodes `AccessoryBag` from
`accessory_bag_storage` with no member anywhere, `initialize` is simply never called, and the
accessors return empty.

**Three costs, stated so they are not discovered.** `initialize` runs on every `getAccessoryBag()`
call rather than once - three assignments plus three getter chains, idempotent by construction; if a
profiler objects the guard is a boolean field, not a redesign. The memo fields race benignly, exactly
as `HypixelPlayer.java`:83-90 already ships, and each memo site should say so in a comment. And
**exceptions move from decode to call** - a broken derivation currently presents as an empty
collection and will now throw at the caller. That is the correct trade and it is why `Bestiary` stayed
dark for years, but it is a behaviour change for every consumer of these DTOs and belongs in the
release note.

**Verify.**

```
toolsmith verify hypixel compileJava test
toolsmith tally hypixel
python scripts/json_dto_diff.py
```

Assertions - and this stage finally makes a whole-member decode testable, which the test class
currently refuses to do:

- **A whole `SkyBlockMember` decodes with no JPA session stood up.** Rewrite the class javadoc at
  `MemberDtoMappingTest.java`:42-45, which documents the old workaround. `bestiary.getFamilies()` is
  the only accessor that needs the session, and only at call time.
- `bestiary.getFamilies()` matches the values captured in `s20-dark-feature-fixes` exactly.
- `member.getCollectionUnlocked()` equals stage 1's map entry for entry. Capture it there.
- For a synthetic member carrying `FIG_LOG_9` and `MANGROVE_LOG_9`, the `FORAGING` level subtractor
  is **`0`**. It is `2` today for every input, and the fixture cannot show the difference because it
  tops out at tier 7 - so this assertion needs a hand-built member, not the fixture.
- `member.getSkills()` is non-null and `member.getTotalWeight()` no longer dereferences null.
- `member.getAccessoryBag().getMagicalPower()` is greater than zero for the populated member.
- A second `getCollectionUnlocked()` call returns the **same instance** - the memo test.

Differ stays at **0**. No key mapping changes in this stage, so any movement is a bug.

**Rollback.** Two `git revert`s in one module. Commit 2 spans three files including the test and
reverts as one unit, because `AccessoryBag.initialize`'s declaration and its only call site are both
inside it. Reverting commit 1 does not take commit 2 with it.

**Estimate.** AI-assisted elapsed **3-5 hours**; human-developer **2-3 days**.

## 11. Stage 8 - s20-duplication-sweep

**Goal.** The pure-polish stage. No defect is fixed and no `PostInit` implementor retires; this is
repeated shape and repeated logic, collapsed. It is ranked last among the consumer stages for exactly
that reason, and it is the first thing to drop if the plan is cut short.

**Effort.** `medium` - roughly 12 consumer files across six independent commits, zero library files.

**Library change.** None.

**Sequenced after `s20-shape-retirements` and `s20-derivation-retirements`** because three of its
commits touch files those stages rewrite. Landing it earlier means merging the same file twice.

**Files touched and what lands in each commit:**

| # | Change | Files | Payoff | Finding |
| --- | --- | --- | --- | --- |
| 1 | Package-private `interface DungeonWeighted extends Experience, Weighted` holding `DEFAULT_TIERS`, `getExperienceTiers()`, `getMaxLevel()` and `getWeight()` as defaults | `DungeonData`, `DungeonClass`, +1 new | **23 byte-identical lines** to 0; one copy of the weight formula | `f04-dungeon-weight-duplication` |
| 2 | One `<id>_<n>` parse helper in this module, turning a `List<String>` into `Map<String, List<Integer>>` in one pass; three sites reduce it differently in two lines each | `SkyBlockMember`, `PlayerData`, `Bestiary`, +1 new | 3 hand-rolled scans unified; the unquoted `String.format` pattern retired | `f05-idtier-key-family` |
| 3 | Static enum-lookup helper in `api/simplified/hypixel/common/` replacing 3 identical `of(String)` bodies | `DungeonData`, `DungeonClass`, `Statistics` | ~24 lines | `f04-enum-of-parsers` |
| 4 | `WeightedGroup<T extends Experience & Weighted>` interface for the four repeated aggregates | `Skills`, `Slayers`, +1 new | ~60 net lines - take the `Skills`/`Slayers` half only | `f04-aggregate-block-triplication` |
| 5 | Union `SuusQuest` and `MollimQuest` into `NpcQuest` | `NpcQuest`, `CrimsonIsle`, -2 files | **2 files, 34 lines** | `f06-crimson-npc-quest-family` |
| 6 | The ten raw `long` epoch fields in `crimson/` to `SkyBlockDate.RealTime` | 6 files | 0 lines; one predictable rule | `f06-temporal-type-split` step 1 |
| 7 | `getFirst()`/`getLast()` for `experienceTiers.get(size() - 1)` | `SkillLevel`:63, `SlayerBoss`:54 | 2 tokens; a house-style violation | `f04-dungeon-weight-duplication` |

**Commit 4 takes the cheap half only.** `Dungeons` needs the aggregate block twice over two different
collections and a Java class can implement a generic interface once, and `Dungeons.getClassAverage()`
cannot become `getAverage()` without colliding with the dungeon-level aggregates on the same class.
Leave `Dungeons` alone; two files and one new interface is `trivial` and delivers most of the value.
`SkillLevel.getWeight()` and `SlayerBoss.getWeight()` are **not** merged with commit 1 - they share
the shape but not the arithmetic, and merging them is a false abstraction.

**Commit 6 is a convention decision as much as an edit.** It converts only the ten raw `long`s, all
of which are in `crimson/` and were clearly written in one sitting. It does **not** convert the ~24
`java.time.Instant` fields; that is a larger blast radius and needs a recorded decision that
`skyblock/` is `RealTime` territory. And it must not sweep in the genuinely-`int` SkyBlock **day
numbers** - `CrimsonIsle.lastReset` is `90` in the fixture, and the `daily_..._day` family in
`HeartOfTheMountain`/`HeartOfTheForest` is the same idea. Those are correctly `int`.

**Also worth landing here, or explicitly declining:** `f06-trophyfish-tier-columns` replaces
`TierData`'s four `int` columns with `@Capture ConcurrentMap<TrophyFish.Tier, Integer>`. It is
`small` and the payoff is real (a fifth tier becomes a one-line enum edit), but it nests a `@Capture`
map inside a class that is itself the value of an outer `@Capture`, and the `trophy_fish` node is
**absent from the bundled fixture**. Verify against a real response first; if nesting misbehaves,
drop the finding rather than working around it. Four `int` fields is a small price.

**Verify.**

```
toolsmith verify hypixel compileJava test
toolsmith reorder --check src/main/java
toolsmith javadoc --scope src/main/java
python scripts/json_dto_diff.py
```

Assertions:

- `dungeonData.getWeight()` and `dungeonClass.getWeight()` return the pre-change values for a fixed
  experience input. Capture both before commit 1; a divergence here is the exact defect the shared
  interface exists to prevent, since `SkyBlockMember.getTotalWeight()` sums both.
- `skills.getAverage()`, `getExperience()`, `getProgressPercentage()` and `getWeight()` and the four
  `Slayers` equivalents are unchanged after commit 4.
- `crimsonIsle.getQuests().getSuusQuest()` and `getMollimQuest()` decode from the fixture into the
  unioned `NpcQuest` with the same field values.
- The ten converted temporal fields return the same epoch millis through `RealTime` as they did as
  `long`.

Differ stays at **0**. Commit 5 changes a declared type but not a serialized name, so a count change
would mean the union lost a key.

**Rollback.** Seven independent `git revert`s. Commit 5 deletes two files, so its revert restores
them plus the two `CrimsonIsle.Quests` field declarations. Nothing outside these files references
`SuusQuest` or `MollimQuest`.

**Estimate.** AI-assisted elapsed **2-3 hours**; human-developer **1-1.5 days**.

## 12. Stage 9 - s20-library-cycle

**Goal.** The pack's only `gson-extras` publish. It carries three things: `@Fallback` (the one
registry proposal with no stock equivalent), the `PostInitTypeAdapterFactory` correctness fix, and
`@Flatten` as a rider. One commit, one build, one re-pin.

**Effort.** `medium` - it edits two existing factories, which the effort scale rates `medium`
regardless of diff size. The risk inside that rating is close to zero and the plan says so, because
both edits are opt-in.

**Library change.** Yes - seven files in `Simplified-Dev/gson-extras`, two of them edits to existing
factories. **One** JitPack build, **one** re-pin at `build.gradle.kts`:44. This is the pack's only
library cycle; the cadence in §12.2 is mandatory and its steps are not reorderable.

**Blocked by `s20-dark-feature-fixes` and `s20-derivation-retirements`.** This is the deliberate
inversion described in §2. Today `Bestiary` and `AccessoryBag` throw on **every** decode; turning on
the log before those are fixed produces one warning per member per request and trains everyone to
filter it out.

### 12.1 The library half - `Simplified-Dev/gson-extras`

| File | Change | Design entry |
| --- | --- | --- |
| `src/main/java/dev/simplified/gson/annotation/Fallback.java` | new - a marker with no elements, `@Target(FIELD)`, `@Retention(RUNTIME)` | `d10-fallback` §7.2 |
| `src/main/java/dev/simplified/gson/factory/CaseInsensitiveEnumTypeAdapterFactory.java` | ~8 lines - resolve the marked constant in the constructor loop that already calls `enumClass.getField(...)`, throw on a second marked constant, and change `:82` to return `constant != null ? constant : this.fallback` | `d10-fallback` §7.4 |
| `src/main/java/dev/simplified/gson/factory/PostInitTypeAdapterFactory.java` | null-guard `obj`, log the caught exception instead of discarding it (`:35-39`) | `d11-postinit-narrowed-contract` |
| `src/main/java/dev/simplified/gson/PostInit.java` | rewrite the javadoc to state what a body may legally read and to point the next author at a computed accessor first | `11-postinit-elimination.md` §11.3 |
| `src/main/java/dev/simplified/gson/annotation/Flatten.java` | new - one `String value()`, the wrapper member name | `d10-flatten` §6.2 |
| `src/main/java/dev/simplified/gson/factory/FlattenTypeAdapterFactory.java` | new, ~150 lines, modelled on `SplitTypeAdapterFactory` - a bind-time pre-delegation tree rewrite with the inverse rewrite on write | `d10-flatten` §6.4 |
| `src/main/java/dev/simplified/gson/GsonSettings.java` | register `Flatten` at **list index 3**, between `Split` and `SerializedPath` | `d10-flatten` §6.5 |
| `src/test/java/.../GsonFactoryTest.java` | new cases; existing `PostInitTests` and `CaptureWithPostInitModel` must stay | - |

Three properties that make this safe for the modules already pinned to `gson-extras`:

- **`@Fallback` is opt-in.** `this.fallback` is `null` when no constant is marked, so the expression
  degrades to today's exact behaviour and un-annotated enums are bit-identical. The obvious
  alternative - "on a miss return the constant named `UNKNOWN`" - is a silent behaviour change for
  every enum in every consumer, and it would guess wrong four times out of fourteen here
  (`Kuudra.Tier` is `BASIC`, `SkyBlockAuction.rarity` is `COMMON`, `Crystal.State` is `NOT_FOUND`).
- **The logging fix is not a behaviour change relative to the documented contract.**
  `PostInit.java`:13-14 already promises exceptions are "logged and swallowed" and the code never
  logged. Rethrowing would be a real change and is **not** proposed. The null guard matters and is
  easy to miss: a JSON `null` for a `PostInit`-typed field currently makes the cast throw
  `NullPointerException` into the same empty catch, so logging without the guard turns a benign
  normal-input path into a warning per null field.
- **`FlattenTypeAdapterFactory` returns `null` when a type carries no `@Flatten` field**, which keeps
  it off the hot path for the other 132 files.

Two hard exclusions the factory must enforce at `create` time, with a thrown exception rather than a
silent no-op - a silently ignored annotation is the exact failure class this pack keeps finding:
`@Flatten` with `@Capture` on one field (Capture is outer and removes the keys before Flatten sees
them), and `@Flatten` with `@SerializedPath` on one field (SerializedPath binds by a route that
bypasses the delegate chain entirely, in either registration order).

### 12.2 The cadence, spelled out

```
# 1-2  in Simplified-Dev/gson-extras
git commit  (one commit carrying all seven files)
git push

# 3-4  publish
toolsmith jitpack build gson-extras          # triggers + waits for one build
toolsmith jitpack status gson-extras         # read the resulting sha

# 5    in Simplified-Api/hypixel
#      build.gradle.kts:44
#      api("com.github.simplified-dev:gson-extras") { version { strictly("<new sha>") } }

# 6    prove the library change is behaviour-neutral BEFORE any consuming edit
toolsmith verify hypixel compileJava test
python scripts/json_dto_diff.py

# 7    only now, the consuming edits
```

Step 6 is the step people skip and it is the one that makes the two halves separately revertable. At
that point **no DTO has changed**, so a red build is unambiguously the library's fault.

Run `toolsmith jitpack pins` before step 5 to see workspace-wide drift, and tell `dataflow`'s owner:
`PipelineGson.java`:49 registers `PostInitTypeAdapterFactory` in a second, independently built
`Gson`, so it inherits the new log line from models this pack never read.

### 12.3 The consumer half

Roughly a dozen one-line enum edits plus one field:

- `@Fallback` on the sentinel constant of `DungeonClass.Type` (`UNKNOWN`), `DungeonData.Type`
  (`UNKNOWN`), `CrimsonIsle.Faction` (`NONE`), `Kuudra.Tier` (`BASIC`), `SearchSettings.Sort`,
  `ActiveCommission.Status` (`NOT_STARTED`), `BoardQuest.Status` (`UNKNOWN`), `RabbitSort`,
  `RabbitFilter`, `Crystal.State` (`NOT_FOUND`), `Rarity` (`COMMON`), `GameMode` (`CLASSIC`),
  `Dojo.Type` (`UNKNOWN`), `Powder.Type`, `Mythos.Type`, `TrophyFish`. **One edit per enum repairs
  several fields at once** - marking `DungeonClass.Type` fixes `Dungeons.selectedClass`,
  `FloorData.BestRun.dungeonClass` and the enum-keyed `@Capture` map from
  `s20-existing-annotation-sweep` together.
- **Two enums need a constant before they can be marked.** `Banking.Action` and `DungeonChest.Type`
  have no sentinel at all, so those are two-line changes.
- `Currencies.essence` (`.../skyblock/member/Currencies.java`:17-24) becomes
  `@Flatten("current") ConcurrentMap<String, Integer>`, deleting the nested generic, the
  `@Getter(AccessLevel.NONE)`, the five-line stream accessor and the `AccessLevel` import.

**If `@Fallback` slips, drop `@Flatten` with it.** `@Flatten` has exactly one unambiguous adoption
site and cannot justify a JitPack publish on its own; `d10-flatten` §6.9 makes that conditional
explicit. The two zero-cost neighbours already landed in earlier stages either way.

**Verify.**

```
toolsmith verify gson-extras compileJava test      # library half, before publishing
toolsmith verify hypixel compileJava test          # after step 5, before step 7
toolsmith verify hypixel compileJava test          # again, after step 7
python scripts/json_dto_diff.py
```

- Write the enum bind-path test **before** the library change. Decode
  `{"selected_dungeon_class": "necromancer"}` into `Dungeons` and assert the field. It is `null`
  today, it must be `UNKNOWN` afterwards, and nothing in `MemberDtoMappingTest` covers this - the
  only `UNKNOWN` assertions there (`:104-105`) exercise `getOrDefault` lookups, not the bind.
- Decode `{"none":1,"brand_new_tier":4}` into `Kuudra` and assert the map has **no `null` key**. It
  is `{null=4, BASIC=1}` today.
- Round-trip `Currencies` from the fixture and assert the `essence` object is byte-equal to the
  input. `@Lenient` and `@Collapse` both hold this line and `@Flatten` must too.
- Decode the whole fixture and assert **zero** post-init warnings are logged. That is the acceptance
  test for stages 1 to 7 having actually worked.
- `gson-extras`' own `GsonFactoryTest.PostInitTests` still pass, including the failing-model case.

**Rollback.** The only two-repository rollback in the plan, and the order is not optional. **Revert
the pin edit in every consuming module first, then the `gson-extras` commit.** The published sha
stays on JitPack and is harmless once nothing references it. Never revert the library commit while a
module is still pinned to it. If only the consumer half is wrong, revert step 7 and leave the pin -
the library change is inert without the annotations.

**Estimate.** AI-assisted elapsed **2-3 hours** including JitPack wait time; human-developer
**6-8 hours**.

## 13. Stage 10 - s20-skyblock-election

**Goal.** Retire the seventh `PostInit` implementor, which lives in another repository. Only after
this does the workspace-wide implementor count reach zero.

**Effort.** `small` - one consumer file, plus a publish-and-re-pin cycle in a second repo.

**Library change.** Not `gson-extras`, but `Simplified-Api/skyblock` is pinned by this module at
`build.gradle.kts`:38, so it carries the same cadence cost.

**The file.** `Simplified-Api/skyblock/src/main/java/dev/sbs/skyblockdata/date/Election.java`:14 is a
**character-for-character duplicate** of the hypixel `Election` retired in `s20-free-retirements` -
same two `transient Cycle` fields, same body, same constructor calling `this.postInit()`, same
`equals`/`hashCode`/`toString` routing through the derived accessors. Apply the §5 conversion
verbatim, including the identity simplification.

**Two facts that make it purely decorative work.** It is never deserialized - its only construction
sites are `SkyBlockDate.java`:389 (`mayors.add(new Election(mayorDate.getYear()))`) and
`SpecialElection.java`:13 - so it reaches `postInit()` exclusively through its own constructor. And
it is not an entity: no `@Entity`, no `@Id`, no `@Column`, and it lives in `dev.sbs.skyblockdata.date`
rather than `.model`, so it is not the `JpaRepository` customer either. The interface it implements
is never used by anything that calls it.

**This stage is deliberately not a dependency of anything.** Hypixel's six retire whether or not this
lands. What *is* gated on it is the claim "`PostInit` has zero implementors in the workspace", so any
decision resting on that claim - deprecating the interface, the reopen condition in
`11-postinit-elimination.md` §11.4 - is gated on a change in another repository. Say so rather than
quietly assuming it.

**Cadence.** Same seven steps as §12.2, substituting `skyblock` for `gson-extras` and
`build.gradle.kts`:38 for `:44`. **Bundle it with a `skyblock` publish that is happening anyway** -
do not spend a cycle on a decorative change alone. `skyblock` is a dependency of `hypixel`
(`SkyBlockMember` imports `dev.sbs.skyblockdata.date.SkyBlockDate`), so the re-pin is mandatory once
it lands.

**Verify.**

```
toolsmith verify skyblock compileJava test
# then, after the re-pin
toolsmith verify hypixel compileJava test
python scripts/json_dto_diff.py
```

- `skyblock` compiles and `SkyBlockDate`'s mayor calendar produces unchanged cycles for a fixed year.
- A workspace-wide symbol search for `implements PostInit` returns **zero** hits outside
  `gson-extras`' own test models.
- Differ stays at 0 - `Election` is not a response DTO and claims no JSON keys.

**Rollback.** A `skyblock` revert plus a `hypixel` pin revert, in that order, and only if it was
bundled into a publish rather than riding one.

**Estimate.** AI-assisted elapsed **45-60 minutes** including the cycle; human-developer
**3-4 hours**.

## 14. Independence and re-ordering

The ordering in §3 is by payoff per unit of effort, not by necessity. Only four dependencies are
real, and everything else is free to move.

**The four real dependencies:**

| Dependency | Why |
| --- | --- |
| `s20-dark-feature-fixes` before `s20-derivation-retirements` | `SkyBlockMember.postInit()` aborts on its first statement and `Bestiary.families` has never been non-empty. Converting either before the fix means comparing new output against nothing |
| `s20-dark-feature-fixes` before `s20-objectives-catchall` | the `COMPLETED`/`COMPLETE` typo affects 5 objects today and 800 after the catch-all |
| `s20-dark-feature-fixes` and `s20-derivation-retirements` before `s20-library-cycle` | the new logging must see a clean decode, not a warning per member per request |
| `s20-shape-retirements` and `s20-derivation-retirements` before `s20-duplication-sweep` | three of the sweep's commits touch files those stages rewrite |

**Fully independent - do these in any order, or not at all:**

- **`s20-free-retirements`** depends on nothing and blocks nothing. Both halves are two-file changes
  and either can ship alone. It is placed second only because it proves the computed-accessor and
  sibling-rename patterns on the two simplest classes before they are used on hard ones.
- **`s20-holder-collapse`** is entirely self-contained - it changes how 13 fields are addressed and
  touches no derivation, no hook and no enum. It could be first, last, or split across eight
  independent commits landed over weeks.
- **`s20-existing-annotation-sweep`** is eight unrelated commits sharing only a theme. Any subset is
  a valid stage.
- **`s20-shape-retirements`** commit 1 (`JacobsContest`) and commit 2 (`Dungeons`) are unrelated to
  each other and to everything before them.
- **`s20-skyblock-election`** is in another repository and gates nothing.

**One anti-dependency, and it matters.** Keep `s20-holder-collapse` and
`s20-derivation-retirements` in **separate windows**. Both edit `SkyBlockMember`, and the holder
collapse narrows `getFirstJoin()` covariantly from `SkyBlockDate` to `SkyBlockDate.RealTime`. No
caller can observe that, but two migrations in one file in one window is how a covariant narrowing
gets blamed on a lazy accessor.

**Why the module never breaks mid-migration.** Four properties, and each is what makes a stage
boundary safe to stop at:

- **No stage removes a method another stage adds.** The only signature change in the whole plan is
  `AccessoryBag.initialize`, and both its declaration and its only call site are inside one commit of
  `s20-derivation-retirements`.
- **`implements PostInit` is removed per class, never in a sweep.**
  `PostInitTypeAdapterFactory.create` keys off `PostInit.class.isAssignableFrom(...)` per type, so a
  module where three classes implement the interface and three do not is a perfectly normal state.
  There is no all-or-nothing boundary and no intermediate configuration to hold.
- **Accessor names do not change.** Every conversion replaces a Lombok-generated getter with a
  hand-written one of the same name and return type, or a transient field with a bound field of the
  same name. `ProfileStats.java`, `SkyBlockIsland.java` and
  `SkyBlock-Simplified/.../SkyBlockUserCommand.java` are edited by **no** stage. The two exceptions
  are deliberate and both narrow: `DungeonData`'s constructor loses a parameter, and
  `HypixelPlayer.getStats()` disappears.
- **The library is untouched until `s20-library-cycle`.** Stages 1 to 8 cannot be broken by a JitPack
  failure, a pin conflict or a sibling module's rebuild. That is the property that makes the plan
  cheap to abandon halfway.

**The minimum viable cut.** If only one stage is ever funded, take `s20-dark-feature-fixes`. It fixes
four features that have been silently dark, costs nothing, and is the only stage whose value does not
depend on anything after it. If two, add `s20-objectives-catchall` - 792 unmapped keys to 0 for four
lines, and it turns the coverage gate green so every later change is guarded.

## 15. The two verification gates

Two gates exist. Both run in seconds and both must be run at every stage boundary; neither is
sufficient alone, because they check different things.

### 15.1 Gate one - `MemberDtoMappingTest`

`src/test/java/api/simplified/hypixel/response/skyblock/MemberDtoMappingTest.java`, 293 lines, 16
tests, decoding the bundled 1.6 MB fixture through `GsonSettings.defaults().create()`.

```
toolsmith verify hypixel compileJava test
toolsmith tally hypixel
```

Never hand-write `cd ... && ./gradlew ... | grep -vE incubating | tail -N` - the tool captures the
true exit code and strips the noise. Never inline a python or awk pass over
`build/test-results/test/*.xml`; that is what `tally` is.

Three things about this test the plan depends on:

- **It decodes subtrees, not whole members**, and its class javadoc at `:42-45` says exactly why:
  "a whole `SkyBlockMember` runs `postInit` against the SkyBlock model repositories, which need a
  live JPA session this test deliberately does not stand up." **`s20-derivation-retirements` deletes
  that constraint**, and rewriting the javadoc is part of that stage. A whole-member decode becoming
  possible is the single clearest proof the migration worked.
- **It reads two members** - `sparse` (profile 0) and `populated` (profile 1) - through the
  `firstMember(root, index)` helper at `:70-81`. Assertions about key *order* must not be written
  against either; the two profiles emit the same keys at different indices, which is the whole basis
  of `f02-postinit-bottom-up-order`.
- **`decode(member, key, type)` at `:83-85`** is the idiom to extend. New assertions should follow
  it rather than reaching into `JsonObject` by hand.

**Capture-before-change is the discipline this plan runs on.** Six stages verify against
"the pre-change value". Those values must be *recorded in an assertion before the edit*, not
recalled afterwards. `s20-dark-feature-fixes` is where most of them are captured, because it is the
first stage where those values are correct at all.

### 15.2 Gate two - `scripts/json_dto_diff.py`

Walks the fixture and the DTO class graph in parallel and reports JSON keys no DTO field maps to. It
understands `@SerializedName`, `@SerializedPath`, `@Extract`, `@Capture`, `@Collapse`, `@Key` and
`@Split`, which is exactly the annotation set this plan moves around - so it is the cheapest way to
catch a key that silently stopped binding.

```
python scripts/json_dto_diff.py                      # whole member, exits 1 when unmapped
python scripts/json_dto_diff.py --section dungeons    # one member subtree
python scripts/json_dto_diff.py --root SkyBlockIsland --node profile
python scripts/json_dto_diff.py --show-unresolved     # types the parser could not follow
```

**It exits 1 while unmapped keys remain**, so it already works as a CI gate. Today it exits 1 with
792, all under `SkyBlockMember.objectives`. After `s20-objectives-catchall` it exits 0, and from that
point a non-zero exit in any later stage is a hard failure rather than a known baseline. That
transition is worth more than the 792 keys themselves.

**Run it at every stage, not only the ones that obviously move keys.** The stages that change key
claims are `s20-holder-collapse` (13 fields move from holder-relative to path-addressed),
`s20-objectives-catchall`, `s20-existing-annotation-sweep` (five commits), `s20-shape-retirements`
(`@Collapse` on `contests`, the `player_classes` retype) and `s20-free-retirements`
(`kuudra_party_finder` becomes two named fields). The others should produce an identical count, and
an identical count is itself the assertion.

**What the differ cannot see**, so it must not be treated as complete coverage:

- Endpoints absent from the bundled fixture. `SkyBlockAuction.startingBid` and
  `CommissionData.totalCompleted` are both `f06-serialized-name-misses` and both invisible here.
- `Map<String, Object>` escape hatches satisfy the differ without modelling anything. Five remain
  after this plan and any coverage claim should discount them.
- Round-trip fidelity. The differ is read-only. Serialization assertions belong in gate one, and
  three stages owe one: the shared-prefix re-nesting in `s20-holder-collapse`, the `essence` re-wrap
  in `s20-library-cycle`, and the absence of phantom `skyBlockDate`/`collectionName` keys in
  `s20-shape-retirements`.

### 15.3 Two house-style gates worth running in the same breath

```
toolsmith reorder --check src/main/java     # IntelliJ Default import layout, idempotent
toolsmith javadoc --scope src/main/java     # single hyphens, no @author/@since, field-doc shape
```

Every stage deletes imports (`PostInit`, `Comparator`, `Pair`, `AccessLevel`) and adds javadoc to
new accessors. Running these at the stage boundary is cheaper than a review round.

## 16. Estimates

**AI-assisted elapsed time first, human-developer time as the comparison.** Both are wall-clock
elapsed, not effort-hours, and both include verification at the stage boundary. The AI-assisted
figure is the observe-correct loop - authoring is fast, but running the build, reading the failure
and fixing the next thing still bounds it.

| Stage | Effort | AI-assisted elapsed | Human-developer elapsed |
| --- | --- | --- | --- |
| `s20-dark-feature-fixes` | `trivial` x9 | **45-75 minutes** | 4-6 hours |
| `s20-free-retirements` | `trivial` x2 | **30-45 minutes** | 2-3 hours |
| `s20-holder-collapse` | `small` | **1.5-2.5 hours** | 1-1.5 days |
| `s20-objectives-catchall` | `small` | **1-1.5 hours** | 4-6 hours |
| `s20-existing-annotation-sweep` | `small` | **2-3 hours** | 1-1.5 days |
| `s20-shape-retirements` | `small` x2 | **1.5-2.5 hours** | 6-8 hours |
| `s20-derivation-retirements` | `medium` | **3-5 hours** | 2-3 days |
| `s20-duplication-sweep` | `medium` | **2-3 hours** | 1-1.5 days |
| `s20-library-cycle` | `medium` | **2-3 hours** | 6-8 hours |
| `s20-skyblock-election` | `small` | **45-60 minutes** | 3-4 hours |
| **Total** | - | **16-24 hours elapsed** | **9-13 working days** |

The ratio is roughly 5:1 and it is not uniform. Where it is widest and where it is narrowest is worth
knowing before scheduling:

- **Widest on `s20-holder-collapse` and `s20-existing-annotation-sweep`** - mechanical, repetitive,
  thirteen near-identical edits with a known shape. This is what AI authoring is fastest at, and the
  human figure is dominated by typing and by re-checking each JSON path against the fixture.
- **Narrowest on `s20-derivation-retirements`** - the only stage with genuine design judgement
  (memoisation placement, what `initialize` should take, whether the `FORAGING` assertion needs a
  synthetic member) and the only one whose verification needs a hand-built input rather than the
  fixture. Expect the observe-correct loop to dominate.
- **`s20-library-cycle` is bounded by JitPack, not by either party.** The build wait is the same
  minutes for both columns, which is why its ratio is the smallest in the table. Do not schedule it
  as a filler task expecting it to finish in a gap.

**One estimate that is deliberately absent.** The `@Capture(descend = true)` spike gating the
follow-up form of `s20-shape-retirements` is not estimated, because its outcome decides whether there
is any work at all. Budget **30-45 minutes AI-assisted** for the spike itself - one test against
`gson-extras` - and estimate the follow-up only if it passes.

## 17. Do not do

Sixteen library proposals were examined across the pack and **fourteen were rejected or declined**.
This section exists so none of them is relitigated. Every row carries the reason and the entry that
owns the argument at full length.

### 17.1 Registry entries - do not build these

| Proposal | Verdict | Reason | Owner |
| --- | --- | --- | --- |
| `@Inline` | **reject** | `@SerializedPath` covers all 9 census holders (11 classes unioned) at zero library cost. The largest holder in the package has **3** fields and six of nine have **1**, so the entire saving is two repetitions of the string `"profile."` | `d10-inline` |
| `@Delegate` | **reject** | Stock `lombok.experimental.Delegate` already ships it and **still loses**: it deletes the forwarder but keeps the holder, the field and the nesting - 40 of 135 lines - and it cannot rename, which `VillagePlaza.getSeraphineStepIndex()` requires and where `Murder.step_index` would clash | `d10-delegate` |
| `@Alias` | **reject** | `@SerializedName(value = ..., alternate = {...})` is stock gson, honoured on fields, on enum constants by `CaseInsensitiveEnumTypeAdapterFactory`:51-58, and by `@Capture`'s own key arbitration | `d10-alias` |
| `@Owner` / `@Parent` | **decline** | **One** bound customer. Three values copied down beat a reference handed up, and the copy-down also breaks the `response.skyblock` package import cycle, which a typed owner field *is* | `d10-owner-parent` |
| ancestor-relative `@SerializedPath` | **decline** | `^` counts adapter frames and the author counts objects, and a path string cannot check the difference. Its one site needed a rename, not a mechanism | `d10-ancestor-path` |
| `@Derive` | **reject** | `PostInit` with a reflected method name. Its only addition is ordering between derived fields, and ordering is a property of eagerness - the call stack sorts the graph for free once both sides are lazy | `d10-derive` |
| `@Index` / `@Join` | **reject** both | Two different operations with **one adoption site each**, and 11 of the lookups need a `SkyBlockData` dependency that `gson-extras` must never acquire | `d10-index-join` |
| `@Tier` | **reject**, kept as a documented alias | Three sites share the *parse* and **none share the reduction** - max, all-sorted, and both-halves. A shared helper in this module covers them | `d10-tier` |
| `@Aggregate` | **reject** | The registry line's premise is false: across 133 files there is **not one** materialized aggregate. An annotation with zero adoption sites, whose only effect would be to import an ordering hazard the package does not have | `d10-aggregate` |
| `@Bind` | **reject** | `xlarge` - it reorders the factory chain. The ordering evidence behind it is the strongest in the pack and it argues for **laziness**, not for an engine | `d10-bind` |
| `@Capture` unmatched-key element | **decline** | Subsumed by `@Fallback` through the enum adapter, which fixes all seven sites with **no change to the busiest factory in the library**. `skipUnmatchedKeys` would also break round-trip fidelity | `d10-capture-unmatched` |
| `@Capture` value-grouping element | **decline** | One site, eight lines, against a change to grouping selection shared by twelve files. `SkillTree.Skill` is the one price `@Capture`'s value-type-drives-mode rule ever charges, and it is a good trade | `d10-capture-value-grouping` |
| `@Lenient` typed-overflow element | **decline** | One site, and the free partial (`@Lenient ConcurrentMap<String, Integer>` on `questRewards`) types half of it today at zero cost | `d10-lenient-overflow` |
| class-level `@Flatten` | **decline** | "Bind a bare scalar into my sole field when the JSON is not an object" is technically sound and immunises against a wrap/unwrap flip that **the fixture shows nowhere**. It removes no code and gives one name two unrelated meanings | `d10-flatten` §6.7 |
| a field-level `@Fallback` | **cut from the accepted design** | The absent-key half is already served by Java field initializers, and a field-level form **cannot reach a map key**, so all seven `@Capture` sites would stay broken. The accepted design is an enum-constant marker | `d10-fallback` §7.7 |
| deleting the `PostInit` interface | **do not** | Not primarily compatibility. `JpaRepository.java`:255-256 calls it manually before an upsert, and `dev.sbs.skyblockdata.model` entities use **field access**, so a persisted derived column genuinely cannot be lazy. Zero implementors makes it an extension point, not a dependency | `d11-postinit-interface-retained` |

### 17.2 Consumer-side changes that look right and are not

- **Do not collapse `HeartOfTheForest.BiomeWhispers.Tier` to a scalar.** The fixture shows the
  whispers key family mid-growth - `desert` gained a `total` key between two profiles of one account -
  so `Tier` is the cheapest place to absorb a future `refunded` key. Suppress `getTiers()` instead;
  that makes the shape freely reversible, which is what the collapse was trying to buy.
  (`f03-biomewhispers-tier`)
- **Do not collapse `CrystalHollows.MinesOfDivan` or `LostPrecursorCity`.** Both are `{}` in every
  fixture member, but their two siblings in the same parent object (`goblin`, `jungle`) carry two
  keys each. They are single-valued only because this account has not progressed them.
  (`f03-crystalhollows-biomes`)
- **Do not collapse `WinterIsland`.** Jerry's Workshop is seasonal, the fixture was captured outside
  the event window, and a seasonal payload observed at its minimum is the worst possible evidence for
  a permanent shape decision. Revisit with a December capture. (`f03-winterisland-seasonal`)
- **Do not collapse `EdelisQuest`.** It sits in a family of ten `*Quest` classes bound from
  `CrimsonIsle.Quests`; collapsing the one that currently has a single field would make it the only
  quest addressed by path instead of by type, for ten lines. Family symmetry is worth more.
  (`f03-quest-family-symmetry`)
- **Do not merge the seven rift location classes.** Across all seven there is **not one shared
  serialized name**. A base class holding the union would advertise every location's fields on every
  other location. Same for `WestVillage`'s four nested classes and `VillagePlaza`'s five.
  (`f06-rift-location-classes`)
- **Do not merge `SkillLevel.getWeight()` or `SlayerBoss.getWeight()` into `DungeonWeighted`.** They
  share the shape - guard, base, `NumberUtil.round`, overflow branch - but not the arithmetic.
  `SkillLevel` uses a repository-supplied exponent, `SlayerBoss` runs an iterative loop. Merging them
  is a false abstraction. (`f04-dungeon-weight-duplication`)
- **Do not demote any `Optional<T>` field.** Hypixel emits **explicit nulls**, not just missing keys -
  across 88 fixture pets, `skin` is present 88/88 with 70 nulls. Demotion replaces a modelled absence
  with a raw null and moves the check into every caller. The fixture is 2 members and 2 profiles and
  cannot support a "never absent" claim anyway. (`f03-optional-audit`)
- **Do not normalise `@NoArgsConstructor` usage.** Four conventions exist across 208 class
  declarations and **all four behave identically** - Java synthesises the constructor, Gson calls
  `setAccessible(true)`. It is 100% cosmetic, and the nine classes where it would matter are all
  hand-constructed rather than Gson-bound. `Election`:15 is the one load-bearing use and it is
  correct. (`f06-noargs-constructor-drift`)
- **Do not extract a shared `DailyEffect` class** for the `current_daily_effect` pair duplicated in
  `HeartOfTheForest` and `HeartOfTheMountain`. It would add a nesting level for two fields - exactly
  the shape `s20-holder-collapse` is removing. Add `@NotNull` to both `Optional`s and keep the
  domain-specific names. (`f06-daily-effect-twin`)
- **Do not narrow `ProfileStats`' `SkyBlockIsland` parameter, and do not make it a bound field.** It
  is never deserialized, and its `calculateBonus` flag exists precisely so callers can skip the
  expensive branch. Making it an eagerly-populated transient would run that branch for every member
  of every profile on every decode, to delete one constructor parameter. **Recommend no action.**
  (`f02-profilestats-island-scalar`)

### 17.3 Two specific "fixes" that would be regressions

- **Do not widen the collection-tier regex to `-?[\d]+`.** The exclusion of `MELON_-1`-style entries
  looks like an oversight and is correct: 83 of 775 tier strings end `_-1`, **no id's maximum tier is
  negative** (every id carrying `_-1` also carries `_1`..`_9`), and every downstream consumer
  compares against positive thresholds, so `0` serves them better than `-1`. A `-1` tier means
  "collection visible, nothing claimed", which *is* tier zero. `s20-derivation-retirements` states it
  as `if (tier < 0) continue;` precisely so nobody "fixes" it again. (`f05-negative-tier-exclusion`)
- **Do not add `MASTER_CATACOMBS` to `DungeonData.Type`.** It is the first diagnosis everyone reaches
  for and it fixes nothing - the case-sensitive filter at `Dungeons.java`:58 still lets the lowercase
  `master_catacombs` key through, so the spurious `UNKNOWN` dungeon survives. This is a case mismatch,
  not a missing constant. (`f05-dungeons-master-pairing`)

### 17.4 One deferred, not rejected

**`f04-lookup-sentinel-drift`** - six "look one element up in a sibling map" helpers using four
different miss strategies (shared sentinel instance, fresh empty collection, `null`,
`Optional.empty()`). No annotation addresses it; these are lookup-time misses on a fully populated
map, long after binding. The one part worth doing now is the `@NotNull` lie at `Skills.java`:27,
whose body is `matchFirstOrNull`. Fold that single fix into whichever stage next opens `Skills.java`
(`s20-duplication-sweep` commit 4). The wider convention change is signature churn on public
accessors and is the only finding in the pack whose payoff is smaller than its review cost.

## 18. Open risks and spikes

Everything the plan asserts but has not executed, with the cheapest way to settle each. None of these
blocks stage 1; three of them gate a specific later stage and say so.

| # | Question | Gates | How to settle |
| --- | --- | --- | --- |
| 1 | Does `@SerializedPath` re-nest **three** fields sharing one prefix into one `profile` object on write? `SerializedPathTypeAdaptorFactory.java`:80-86 was read closely and never run | `s20-holder-collapse` | One round-trip test. It is a **prerequisite of the stage**, not a nicety |
| 2 | Does a filtered `@Capture` re-prefix its keys on write? | `s20-existing-annotation-sweep` commit 5 | Assert against the shipping `Kuudra.java`:18 (`^highest_wave_`) user rather than the new `FloorData` one |
| 3 | Does `@Capture(descend = true)` with an **empty** filter work, and what does the known-key set mean inside a descended object? `Capture.java`:96-116 documents `descend` only alongside a non-empty filter | the follow-up form of `s20-shape-retirements` only | One test against `gson-extras`. **Keep it off the critical path** - §6.3's lazy form retires the implementor without it |
| 4 | Does `DungeonClass` bind correctly through `UnsafeAllocator` with a `final` field and no no-arg constructor? | `s20-shape-retirements` commit 2 | One decode assertion. Safer form: drop `final`, add `@NoArgsConstructor(access = AccessLevel.PRIVATE)`, matching `FloorData` |
| 5 | Does `objectives.tutorial` get claimed by the existing `@SerializedPath` before the catch-all sees it? | `s20-objectives-catchall` | One decode assertion. It decides whether the catch-all needs a `filter` |
| 6 | Is `CommissionData.totalCompleted`'s upstream key really `total_completed`? The endpoint is not in the fixture | held out of `s20-dark-feature-fixes` | Resolve against a live `/skyblock/garden` response. **Do not guess it into the source** |
| 7 | Does the `TrophyFishing.TierData` `@Capture` nest correctly inside an outer `@Capture` value class? The `trophy_fish` node is absent from the fixture | `s20-duplication-sweep`, optional item | Verify against a real response. If nesting misbehaves, **drop the finding** - four `int` fields is a small price |
| 8 | What does the extra `SerializedPathTypeAdaptorFactory` wrapping cost on `Rift`, `VillagePlaza` and `AttributeShards`, which have no `@SerializedPath` today? | `s20-holder-collapse` | Measure once. `SkyBlockMember`, `Dungeons` and `Bestiary` already pay it on far larger objects, so the marginal cost is expected to be small - but measured, not assumed |

**Three known latent issues that are recorded rather than scheduled.**

- **A dead guard in `SerializedPathTypeAdaptorFactory`:124** reads `innerJsonObject.isJsonArray()`
  where it means `innerJsonElement.isJsonArray()`. `innerJsonObject` is the result of
  `getAsJsonObject()` one line earlier, so the branch is unreachable. The effect is that an empty
  JSON array at the end of a path is bound as an empty collection instead of being skipped - which is
  the desirable outcome, so nothing is broken. Recorded so `s20-holder-collapse`'s expanded use of
  that factory is not blamed for it later.
- **`Crystal.Type.SAPHIRE`** (`.../member/mining/Crystal.java`:32) misspells `SAPPHIRE`. Binding is
  unaffected because the constant carries `@SerializedName("sapphire_crystal")`. It becomes
  load-bearing the day someone writes `Crystal.Type.valueOf("SAPPHIRE")`.
- **`CrimsonIsle.Quests.kuudraBossDaily`** carries `@SerializedName("kuuda_boss_daily")` and that
  annotation is **correct** - the key really is misspelled upstream and the fixture contains
  `kuuda_boss_daily`. It looks exactly like the `starting_big` defect and is not one. Do not "fix" it.

**The one irreversible thing in the plan is not code.** `s20-derivation-retirements` changes *when*
exceptions surface for every consumer of these DTOs - from swallowed at decode to thrown at the
caller. That is the correct trade and it is why `Bestiary`'s feature stayed dark for years, but a
consumer that has been silently tolerating an empty collection will start seeing a stack trace. It
belongs in the release note, not in a code comment.

**Two claims that rest on evidence from a single fixture**, and would need a second capture to
strengthen: that `temples`, `winter_player_data` and `events` really are single-key sub-objects
(`s20-holder-collapse` risk 3), and that no collection id has a negative maximum tier (§17.3). Both
are correct for the 2 profiles and 2 members available; neither is provable from them.

