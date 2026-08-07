# Stage progress and settled risks

What has landed against `20-implementation-plan.md`, and what the open questions in its §18 and
`README.md` §9 turned out to be once executed. Blockers live in
`s20-dark-feature-fixes-BLOCKED.md`; this file is the ledger.

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
| `s20-dark-feature-fixes` | **7 of 9** | `AccessoryBag` and `Bestiary` held - see the blockers file |
| `s20-free-retirements` | not started | |
| `s20-holder-collapse` | **done** | 8 files, 13 fields, `Temples.java` deleted |
| `s20-objectives-catchall` | **done** | differ 792 to 0, exit 0 |

The differ is a hard gate from here. `python scripts/json_dto_diff.py` exits 0 today; any later stage
that raises the count above 0 has broken a binding.

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

## Still open

- `CommissionData.totalCompleted` - unchanged, still needs a live `/skyblock/garden` response.
- Filtered `@Capture` re-prefixing on write - not exercised yet; gates `s20-existing-annotation-sweep`
  commit 5.
- `DungeonClass` binding through `UnsafeAllocator` - not exercised yet; gates
  `s20-shape-retirements`.
