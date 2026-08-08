# Stage progress and settled risks

What has landed against `20-implementation-plan.md`, and what the open questions in its §18 and
`README.md` §9 turned out to be once executed. Blockers live in
`s20-dark-feature-fixes-RESOLVED.md`; this file is the ledger.

## State on arrival

The pack was written against a tree that had already moved on. Six commits landed after it, and one
of them was the library cycle the plan schedules **last**: `gson-extras` is pinned at `97d29a4`, not
the `7cfc181` in the baseline table, and `@Fallback`, `@Flatten` and the shared overflow store all
ship. `s20-library-cycle` is therefore already done apart from the `PostInitTypeAdapterFactory`
logging, and `s20-existing-annotation-sweep` is partly done - `Currencies.essence`, `Dojo`, `Kuudra`
and the `quest_rewards` split are in.

Read the pack's baseline table as history, not as current state.

## Landed

| Stage | State | Notes |
| --- | --- | --- |
| `s20-dark-feature-fixes` | **done** | all nine; the two held fixes landed once the upstream chain cleared |
| `s20-free-retirements` | **done** | `Election`, `CrimsonIsle`/`Kuudra`; `implements PostInit` 6 to 4 |
| `s20-holder-collapse` | **done** | 8 files, 13 fields, `Temples.java` deleted |
| `s20-objectives-catchall` | **done** | differ 792 to 0, exit 0 |
| `s20-existing-annotation-sweep` | **done** | 4 commits rather than 8 - commit 8 had already landed |
| `s20-shape-retirements` | **done** | `JacobsContest`, `Dungeons`; `implements PostInit` 4 to 2 |
| `s20-derivation-retirements` | **done** | `Bestiary`, `SkyBlockMember`; **`implements PostInit` in `response/` is 0** |
| `s20-duplication-sweep` | **done** | 6 of 7 commits; the trophy-fish follow-up declined, see below |

The differ is a hard gate from here. `python scripts/json_dto_diff.py` exits 0 today; any later stage
that raises the count above 0 has broken a binding.

`implements PostInit` in `response/` is **zero**, which is the pack's headline metric. The interface
survives unimplemented, deliberately - `JpaRepository` calls it manually before an upsert and those
entities use field access, so a persisted derived column genuinely cannot be lazy.

A whole `SkyBlockMember` now decodes with no JPA session stood up, which the test class previously
refused to attempt. Binding runs no derivation at all; the repositories are reached only by the
accessors that need them, and only when something asks. The class javadoc that documented the old
workaround is rewritten.

The `Election` regression guard is real rather than circular: the four cycle bounds for year 278 were
read off the hook before it was deleted and are asserted as literals
(`1684145700000`, `1684480500000`, `1684480500000`, `1684926900000`). Note there are **two** `Election`
classes in the workspace - `api.simplified.hypixel.response.skyblock.election.Election` is the one
this stage retired; `dev.sbs.skyblockdata.date.Election` is the separate copy `s20-skyblock-election`
targets, and it still implements `PostInit`.

## Settled risks

**`@SerializedPath` re-nests fields sharing a prefix into one object.** Confirmed, executed. The write
path reuses an existing nested object rather than overwriting it, for three fields sharing a prefix
and for two. Covered by `roundTripsSharedPathPrefix` and `mapsRelocatedHolderFields`.

**`@Capture(descend = true)` with an empty filter works.** Confirmed - it is what `objectives` uses.
The pack held this out as gating only an optional follow-up form of `s20-shape-retirements`; it is now
on the critical path and it works.

**`@Capture` beats `@SerializedPath` for the same node, and the plan guessed the other way.**
`20-implementation-plan.md` §7 offered two routes for `objectives.tutorial` and preferred the second -
"keep the existing `@SerializedPath` field and let it claim the key before the catch-all sees it".
**That route does not work.** With both fields declared, `getTutorialObjectives()` came back empty
while the catch-all bound all 792 objectives: the capture descends and consumes the node first, and a
path-addressed field never sees the key. `discoverKnownKeys` reading a `@SerializedPath`'s first
segment governs sibling keys at the declaring class's own level, not keys inside a descended object.

The working shape is `@Extract`, which is what the annotation is for - it claims one key out of a
capture's overflow and puts it back on write:

```java
@SerializedName("objectives")
@Capture(grouping = Capture.Grouping.ENTRY, descend = true)
private @NotNull ConcurrentMap<String, Objective> objectives = Concurrent.newMap();

@Extract("objectives.tutorial")
private @NotNull ConcurrentList<String> tutorialObjectives = Concurrent.newList();
```

`@Extract`'s own javadoc warns that a source in `@Capture` **grouping** mode never produces an
overflow, so an `@Extract` naming one is a silent no-op. `Grouping.ENTRY` is not grouping mode and
does produce overflow, so this works - but the two facts are one word apart and worth keeping
together.

**`BoardQuest` is now `Objective`**, moved from `member.crimson` to `member`, shared by `QuestBoard`
(five) and `SkyBlockMember.objectives` (792). It gained `completions` and a bare `@Capture`
`requirements` map for the per-objective progress keys the wire stores inline. `@Capture` rather than
the `@Lenient` the plan suggested, because the differ understands `@Capture` and does not understand
`@Lenient` - a `@Lenient` overflow would have left those keys reported unmapped and the gate short of
zero.

**A filtered `@Capture` does re-prefix its keys on write.** Confirmed, executed, on `FloorData`'s
`^most_damage_`. Nothing is lost - but an enum map key is written as its constant name, so the wire's
lowercase `most_damage_healer` returns as `most_damage_HEALER`. That is the same case drift the kuudra
tiers already pin, and it predates this work.

**`@Lenient` round-trips a partially overflowed array's membership but not its order.** The filtered
entries are appended rather than restored to their original index. Whole-array overflow
(`dungeons.dungeon_journal.unlocked_journals`) is unaffected because there is nothing to interleave
with. Pinned by `mapsHypixelPlayerProfiles`.

**`DungeonClass` never had to go through `UnsafeAllocator`.** The plan offered that as the thing to
assert and the safer form as an alternative; the safer form was taken. Dropping `final` makes
`@RequiredArgsConstructor` generate a no-arg constructor, which then collides with
`@NoArgsConstructor` - so the pair is `@AllArgsConstructor` + `@NoArgsConstructor(PRIVATE)`, not
`@RequiredArgsConstructor` + `@NoArgsConstructor`.

**`s20-duplication-sweep` landed six of its seven commits, and one differently than written.**
Commits 5 and 6 merged: unioning the npc quests and typing the crimson timestamps are the same edit,
because `Optional<RealTime>` is what keeps the union honest - a raw `long` would have invented a zero
completion for the npc that records completion as a flag and emitted it on write.

Two declines, both on the pack's own reasoning rather than against it:

- **`Bestiary` keeps its own `<id>_<n>` parse.** The shared grouping returns id to numbers, but
  `Bestiary` needs the original key back to look the mob up in `kills` and `deaths`, so routing it
  through would mean rebuilding the key it started from. Two of the three sites adopted it.
- **`f06-trophyfish-tier-columns` not attempted.** `20-implementation-plan.md` §11 makes it
  conditional on verifying against a real response, and `trophy_fish` is absent from the fixture. The
  plan's own instruction is to drop it rather than work around it.

`Dungeons` is also deliberately left out of the weighted-group interface: it needs the same four
aggregates twice over two collections, and a class implements a generic interface once.

## Still open

- `CommissionData.totalCompleted` - unchanged, still needs a live `/skyblock/garden` response.
- The `@Capture(descend = true)` route for the master-mode pairing, which would make the case defect
  structurally unreachable. Deliberately off the critical path - the memoised accessor already
  retired the implementor, and neither route costs a library cycle.
