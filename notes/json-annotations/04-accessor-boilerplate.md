# 04 - Accessor Boilerplate

Survey of accessors that carry no logic, and of declaration shapes that a shared base type would
erase. Owns finding IDs prefixed `f04-`. Conventions, categories, effort scale and the naming
registry come from `00-conventions.md`.

## 1. Scope and method

This survey looked for four things: the private-nested-holder idiom, `@SerializedPath` used only to
reach a scalar through a wrapper, repeated map-lookup helpers, and hand-rolled code where Lombok or
an existing annotation already has a feature.

Everything below is grounded in source at a cited line, in `craftedfury.json` read through python, or
in a compile experiment. Two claims were verified experimentally rather than argued:

- **Private return types are unreachable.** A throwaway two-package `javac` run confirms that a
  public getter returning a `private static` nested class cannot be dereferenced from another file:
  `error: Holder.getValue() is defined in an inaccessible class or interface`. This decides
  `f04-holder-private-type-leak`.
- **`DungeonData.getWeight()` and `DungeonClass.getWeight()` are byte-identical.** `diff -u` over
  `DungeonData.java:24-57` against `DungeonClass.java:21-50` reports exactly two hunks: the
  `DEFAULT_TIERS` versus `DungeonData.DEFAULT_TIERS` qualifier, and `getFloorData` present only in
  `DungeonData`. Every other line matches. This decides `f04-dungeon-weight-duplication`.

The headline result is negative for the library and positive for the module: **the holder idiom needs
no new annotation at all.** `@SerializedPath` already reaches an arbitrary value type at an arbitrary
depth, and `SerializedPathTypeAdaptorFactory` already re-nests siblings that share a path prefix on
write. Every holder in the package is a `@SerializedPath` that was never written. That makes the
registry's `@Inline` and `@Delegate` entries the two lowest-value rows in the table, and this survey
argues against both.

## 2. Census

### 2.1 Holder classes

Nine holders. Eight are `private static` nested classes; one (`Temples`) is a whole public file.
"Fields" is how many members the holder declares - the number of `@SerializedPath` lines that would
replace it. "Fwd" is how many forwarding accessors the enclosing class hand-writes.

| # | Holder | Field decl | Class body | JSON path | Fields | Fwd |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `SkyBlockMember.Profile` | `SkyBlockMember.java:48` | `:220-230` | `profile` | 3 | 3 |
| 2 | `SkyBlockMember.Events` | `SkyBlockMember.java:105` | `:212-218` | `events` | 1 | 1 |
| 3 | `Temples` (own file) | `SkyBlockMember.java:83` | `Temples.java:1-16` | `temples` | 1 | 1 |
| 4 | `Dungeons.DungeonTreasures` | `Dungeons.java:42` | `:140-146` | `treasures` | 2 | 2 |
| 5 | `Rift.Porhtal` | `Rift.java:23` | `:48-54` | `wither_cage` | 1 | 1 |
| 6 | `VillagePlaza.Lonely` | `VillagePlaza.java:24` | `:75-81` | `lonely` | 1 | 1 |
| 7 | `VillagePlaza.Seraphine` | `VillagePlaza.java:26` | `:83-89` | `seraphine` | 1 | 1 |
| 8 | `AttributeShards.Traps` | `AttributeShards.java:12` | `:17-23` | `traps` | 1 | **0** |
| 9 | `Bestiary.Miscellaneous` | `Bestiary.java:37` | `:141-150` | `miscellaneous` | 2 | **0** |

Totals: 9 holders, 13 held fields, 10 forwarders, ~79 lines of class body.

This census is scoped to the **accessor idiom**, which is why `HypixelPlayer.Stats` and its nested
`Stats.SkyBlock` are absent from it: `HypixelPlayer.java:80` declares `private Stats stats;` with no
suppression and no forwarder, so that pair is a shape finding rather than an accessor one and it is
owned by `f03-holder-collapse-serializedpath`. Counting both sets, the package holds **11 holder
classes across 8 files** - so a bare "nine" is ambiguous between the two surveys, and either the
union or a finding id should be cited instead.

Every path in the table is confirmed against the fixture. Reading the largest member object
(1.15 MB) gives exactly the expected sub-objects: `profile -> [first_join, personal_bank_upgrade,
cookie_buff_active]`, `events -> [easter]`, `temples -> [unlocked_temples]`, `shards -> [traps,
owned, fused]`, `bestiary.miscellaneous -> [max_kills_visible, milestones_notifications]`,
`dungeons.treasures -> [runs, chests]`, `rift.wither_cage -> [killed_eyes]`,
`rift.village_plaza.lonely -> [seconds_sitting]`, `rift.village_plaza.seraphine -> [step_index]`.

The maximum field count on any holder is 3. That number is the whole argument against `@Inline`, and
it is developed in `f04-delegate-rejected`.

### 2.2 `@Getter(AccessLevel.NONE)` - what it is actually used for

34 occurrences across 14 files. Only 7 belong to the holder idiom. Classifying the rest matters,
because a proposal aimed at "the `@Getter(AccessLevel.NONE)` pattern" would be aimed mostly at code
that is doing real work.

| Use | Count | Sites |
| --- | --- | --- |
| Holder suppression (this survey) | 7 | `SkyBlockMember.java:48,83,105`, `Dungeons.java:42`, `Rift.java:23`, `VillagePlaza.java:24,26` |
| Raw field feeding a transforming getter | 7 | `Currencies.java:17`, `Banking.java:25`, `SkyBlockArticle.java:17`, `SkyBlockAuction.java:35`, `Kuudra.java:43`, `JacobsContest.java:111`, `HypixelGuild.java:73` |
| Rank inputs consumed by one composite getter | 10 | `HypixelPlayer.java:62-77` |
| Memoization pair | 2 | `HypixelPlayer.java:42,44` |
| `postInit()` input, no accessor at all | 3 | `Dungeons.java:28,31`, `JacobsContest.java:36` |
| Staging field pushed downward in `postInit()` | 2 | `CrimsonIsle.java:38,41` |
| Enum-keyed sibling family behind one dispatch method | 5 | `FloorData.java:46,49,52,55,58` |

The transforming-getter group is not boilerplate and is not this survey's business - `Currencies`
collapses a map of maps, `SkyBlockAuction` splits a lore string, `Banking` strips an API artifact.
Those belong to `03-value-shape-collapse.md`. The `postInit()` input and staging-field groups belong
to `01-postinit.md` and `02-parent-access.md`. Only rows one and seven produce findings here.

### 2.3 Pure forwarders

Ten accessors whose entire body reads one member of one field. Seven forward through a getter, three
read the holder's field directly.

| Accessor | Site | Body |
| --- | --- | --- |
| `SkyBlockMember.getChocolateFactory()` | `SkyBlockMember.java:162` | `this.events.getChocolateFactory()` |
| `SkyBlockMember.getFirstJoin()` | `:166` | `this.profile.firstJoin` |
| `SkyBlockMember.getPersonalBankUpgrade()` | `:170` | `this.profile.personalBankUpgrade` |
| `SkyBlockMember.getUnlockedTemples()` | `:174` | `this.temples.getUnlockedTemples()` |
| `SkyBlockMember.isBoosterCookieActive()` | `:178` | `this.profile.boosterCookieActive` |
| `Dungeons.getChests()` | `Dungeons.java:132` | `this.treasures.getChests()` |
| `Dungeons.getRuns()` | `:136` | `this.treasures.getRuns()` |
| `Rift.getKilledEyes()` | `Rift.java:44` | `this.porhtal.getKilledEyes()` |
| `VillagePlaza.getSecondsSitting()` | `VillagePlaza.java:29` | `this.lonely.getSecondsSitting()` |
| `VillagePlaza.getSeraphineStepIndex()` | `:33` | `this.seraphine.getStepIndex()` |

`SkyBlockMember.getFirstJoin()` is the only one that is not signature-preserving: it declares
`SkyBlockDate` while `Profile.firstJoin` is a `SkyBlockDate.RealTime`
(`SkyBlockDate.java:778 - RealTime extends SkyBlockDate`). Deleting the forwarder narrows the
declared return type covariantly, which no caller can observe.

`VillagePlaza.getSeraphineStepIndex()` is the only one that renames. It matters in
`f04-delegate-rejected`.

## 3. Findings

### f04-nested-holder-idiom

- **Category:** `accessor-boilerplate`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:48, :83, :105, :162-180, :212-230
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:42, :132-146
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/rift/Rift.java`:23, :44-54
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/rift/VillagePlaza.java`:24, :26, :29-35, :75-89
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/attribute/AttributeShards.java`:12, :17-23
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Bestiary.java`:37, :141-150
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/foraging/Temples.java`:1-16
- **What:** Nine classes exist only to name a JSON sub-object so its members can bind, each paired
  with a suppressed getter and hand-written forwarders on the enclosing type.
- **Why it is residue:** `@SerializedPath` already binds any value type at any depth, so each holder
  is a path expression that was never written. The idiom survives by habit, not by necessity.
- **Candidate annotation:** none - `@SerializedPath` (existing) covers all nine
- **Effort:** `small`

**What it costs today.** ~79 lines of holder class body, 10 forwarder methods (~40 lines with their
blank-line separators), 9 holder field declarations, and one whole `.java` file. The Java shape
carries a nesting level the caller never wants: `member.getUnlockedTemples()` exists because
`member.getTemples().getUnlockedTemples()` would be worse, and `Temples` is a public class whose only
appearance outside its own file is one field and one import.

**Proposed change.** Delete each holder and re-declare its members on the enclosing class with the
holder's JSON key as the path prefix. In full:

```java
// SkyBlockMember - replaces Profile, Events and the Temples field
@SerializedPath("profile.first_join")
private SkyBlockDate.RealTime firstJoin;
@SerializedPath("profile.personal_bank_upgrade")
private int personalBankUpgrade;
@Accessors(fluent = true)
@SerializedPath("profile.cookie_buff_active")
private boolean isBoosterCookieActive;
@SerializedPath("events.easter")
private @NotNull ChocolateFactory chocolateFactory = new ChocolateFactory();
@SerializedPath("temples.unlocked_temples")
private @NotNull ConcurrentList<String> unlockedTemples = Concurrent.newList();
```

```java
// Rift - replaces Porhtal
@SerializedPath("wither_cage.killed_eyes")
private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();
```

```java
// VillagePlaza - replaces Lonely and Seraphine
@SerializedPath("lonely.seconds_sitting")
private int secondsSitting;
@SerializedPath("seraphine.step_index")
private int seraphineStepIndex;
```

`Dungeons`, `AttributeShards` and `Bestiary` follow the same form
(`treasures.runs`, `treasures.chests`, `traps.active_traps`, `miscellaneous.max_kills_visible`,
`miscellaneous.milestones_notifications`).

**Why the round trip survives.** `SerializedPathTypeAdaptorFactory.write` removes the flat key and
walks the path, and at `SerializedPathTypeAdaptorFactory.java:80-86` it *reuses* an existing nested
object when one is already present rather than overwriting it. Three fields sharing the `profile.`
prefix therefore land in one `profile` object: the first creates it, the second and third find it.
Read is symmetric at `:105-141`.

**Payoff.** 9 classes removed (8 nested plus `Temples.java`, taking the package from 133 files to
132), 13 held fields relocated, 10 forwarders deleted, 2 previously unreachable data paths recovered
(see `f04-holder-private-type-leak`). Net ~108 lines: ~135 deleted against ~27 added.

**Risk.** Three honest ones.

1. `Rift`, `VillagePlaza` and `AttributeShards` currently have no `@SerializedPath` field, so
   `SerializedPathTypeAdaptorFactory.create` returns the bare delegate for them today
   (`:39-41`). After the change those three classes get wrapped, which materializes the whole
   sub-tree as a `JsonObject` and then re-parses it (`:101-103`). It is a real per-object cost. It is
   also already paid by `SkyBlockMember`, `Dungeons` and `Bestiary`, which are far larger objects, so
   the marginal cost is small - but it should be measured, not assumed.
2. The flat key used on write is `@SerializedName` value or field name
   (`SerializedPathTypeAdaptorFactory.java:160`). Two `@SerializedPath` fields on one class must not
   share a flat key, and a flat key must not collide with a genuine top-level JSON key. None of the
   13 proposed names collide, but the constraint is invisible in the source and will bite a later
   edit.
3. Deleting the `profile`, `events`, `temples` fields removes declared fields from
   `SkyBlockMember`. If a bare `@Capture` catch-all is ever added to that class it would then claim
   those keys. `SkyBlockMember` has no `@Capture` today, so this is a future hazard, not a present
   one.

**Effort justification.** Eight consumer files, zero library files, no JitPack cycle, no re-pin. That
is below the `small` row's "1 additive library file" but above the `trivial` row's "1 file, consumer
only". Rated `small` on file count alone, and it is the cheapest high-payoff item in the pack because
it is the only one that costs no library round trip.

### f04-holder-private-type-leak

- **Category:** `correctness`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/attribute/AttributeShards.java`:12, :17-23
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Bestiary.java`:37, :141-150
- **What:** Two holder fields keep the class-level `@Getter`, so Lombok emits a public accessor whose
  return type is a `private static` nested class, and the held data cannot be read from any other
  file.
- **Why it is residue:** These are the two holders where nobody wrote the forwarder. The idiom is
  applied inconsistently, and the inconsistency silently deletes data rather than failing loudly.
- **Candidate annotation:** none - subsumed by `f04-nested-holder-idiom`
- **Effort:** `trivial`

**Verified, not argued.** A two-package `javac` reproduction - a public `getHolder()` returning a
`private static class Holder` with a public `getValue()` - fails to compile at the external call
site with `error: Holder.getValue() is defined in an inaccessible class or interface`. The method is
public; the type that declares it is not, and JLS member access checks the declaring type. So
`shards.getTraps().getActiveTraps()` and `bestiary.getMiscellaneous().isMaxKillsVisible()` are
compile errors anywhere outside `AttributeShards.java` and `Bestiary.java`.

**What is lost.** `shards.traps.active_traps` is a `ConcurrentList<ActiveTrap>`, and `ActiveTrap`
models eleven fields plus a `Region` repository lookup (`ActiveTrap.java:13-40`). All of it is
deserialized on every profile fetch and then thrown away, because no caller can name it.
`bestiary.miscellaneous` loses `max_kills_visible` and `milestones_notifications`. A grep across
`src/` for `getTraps()`, `getMiscellaneous()`, `getActiveTraps()`, `isMaxKillsVisible` and
`hasNotificationsEnabled` returns exactly one hit - the field declaration at `Bestiary.java:148`.
Nothing calls them, because nothing can.

**Proposed change.** Fixed for free by `f04-nested-holder-idiom`: the holders disappear and the
members become ordinary Lombok-generated accessors on `AttributeShards` and `Bestiary`. If that
finding is rejected, the minimum fix is to make the two nested classes `public` or add the two
missing forwarders - but that keeps the shape the finding above argues against.

**Payoff.** Two data paths recovered, one of them an eleven-field model. Zero net lines if bundled.

**Risk.** None. Making unreachable data reachable cannot break a caller, because there are no
callers.

### f04-delegate-rejected

- **Category:** `accessor-boilerplate`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/rift/VillagePlaza.java`:29-35, :38-49, :83-89
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:162-180
- **What:** The registry reserves `@Delegate` (generate a forwarder to a nested object's field) and
  `@Inline` (bind a sub-object's keys onto the enclosing class) for the holder idiom.
- **Why it is residue:** It is not. Both entries are dominated by `@SerializedPath`, and this finding
  exists to record the argument so `10-annotation-designs.md` does not re-derive it.
- **Candidate annotation:** none - reject `@Delegate`, reject `@Inline`
- **Effort:** `trivial` (a decision, not a change)

**Against Lombok `@Delegate`.** It was evaluated seriously - it needs no library change at all, which
would make it the cheapest option in the pack. It fails on three counts, the second fatally.

1. **It deletes the wrong half.** `@Delegate` removes the forwarder body and nothing else. The holder
   class stays, the field stays, the extra nesting level in the Java shape stays. That is ~40 lines
   of the ~135 that `@SerializedPath` removes, and it leaves the holder classes in place - so the
   `f04-holder-private-type-leak` sites would still need a separate fix.
2. **It cannot rename, and one site requires a rename.** `VillagePlaza.getSeraphineStepIndex()`
   (`:33`) forwards to `Seraphine.getStepIndex()`. `@Delegate` would generate `getStepIndex()` on
   `VillagePlaza` - which is both meaningless there (which step index?) and one step from a hard
   clash, because `VillagePlaza.Murder` also declares `step_index` (`:40-41`) and delegating both
   would produce two `getStepIndex()` methods on the same class. There is no `@Delegate` element that
   fixes this; the annotation forwards the delegate type's method signatures verbatim.
3. **It is `lombok.experimental`,** with documented trouble on generic and self-referential types. The
   DTOs are heavily generic (`ConcurrentMap<Floor, ConcurrentList<BestRun>>` and similar). Taking on
   an experimental annotation to save 40 lines is a poor trade when a stable annotation already in
   the project saves 135.

**Against `@Inline`.** `@Inline` would earn its keep only where repeating a path prefix across N
fields is worse than declaring the prefix once. The census puts a hard number on N: the largest
holder in the package is `SkyBlockMember.Profile` with **3** fields, and six of the nine holders have
exactly **1**. Against `@SerializedPath` the saving is at most two repetitions of the string
`"profile."`. Weighed against the effort scale's library floor - a new annotation plus a factory,
`small` at best, plus a JitPack cycle and a re-pin - `@Inline` is negative value here.
`00-conventions.md` already anticipated this ("`@SerializedPath` already covers the single-field case
of `@Inline`; `@Inline` only earns its keep for multi-field holders"). The evidence says the
multi-field holders do not exist in this package.

**What would change the verdict.** A holder with ~6 or more fields, or a holder nested two levels
deep so that every field repeats a long prefix. Neither appears in `response/`. If one appears later,
reopen this - the argument is about the observed distribution, not about the concept.

### f04-floordata-most-damage-switch

- **Category:** `accessor-boilerplate`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/FloorData.java`:45-60, :70-79
- **What:** Five sibling fields named `most_damage_<class>` are each suppressed with
  `@Getter(AccessLevel.NONE)` and reached through one `switch` that maps a `DungeonClass.Type` back
  to the field it names.
- **Why it is residue:** The five keys are one enum-keyed family. The `switch` is a hand-written map
  lookup over a map the JSON already had, and it must be edited by hand whenever Hypixel adds a
  class.
- **Candidate annotation:** `@Capture` (existing, no change)
- **Effort:** `trivial`

**What it costs today.** Five field declarations, five `@Getter(AccessLevel.NONE)`, five
`@SerializedName`, and a ten-line `switch` with a `default` arm - roughly 26 lines to express
"group these five keys by their suffix".

**Proposed change.**

```java
@Capture(filter = "^most_damage_")
private @NotNull ConcurrentMap<DungeonClass.Type, ConcurrentMap<Floor, Double>> mostDamage = Concurrent.newMap();

public @NotNull ConcurrentMap<Floor, Double> getMostDamage(@NotNull DungeonClass.Type classType) {
    return this.getMostDamage().getOrDefault(classType, Concurrent.newUnmodifiableMap());
}
```

**Why the existing `@Capture` is enough.** The filter strips its match from the key
(`Capture.java:23-26`), leaving `healer`, `mage`, `berserk`, `archer`, `tank`, and
`CaseInsensitiveEnumTypeAdapterFactory` resolves those to the enum regardless of case. The declared
value type is a `Map`, which selects entry mode automatically (`Capture.Grouping.AUTO`,
`Capture.java:134-139`), so each captured value is read whole with no affix splitting - exactly what
is needed, since the values are `{"7": 139413368.51746204}` floor-keyed maps. The precedent is
`Kuudra.java:18-19`, which already captures `^highest_wave_` into a `ConcurrentMap<Tier, Integer>`.

**Fixture check.** The `catacombs` entry of `dungeons.dungeon_types` in the largest member carries
exactly `most_damage_archer`, `most_damage_berserk`, `most_damage_healer`, `most_damage_mage`,
`most_damage_tank`. The two neighbours that could be captured by accident, `most_healing` and
`most_mobs_killed`, do not match `^most_damage_`.

**Payoff.** 5 fields to 1, ~26 lines to ~6, and the `default -> Concurrent.newUnmodifiableMap()` arm
stops being a silent hole when a sixth class ships.

**Risk.** Low but not zero. `FloorData` has no `@Capture` today, so it gains the
`CaptureTypeAdapterFactory` wrapper. Round-trip fidelity needs a check: a filtered `@Capture` must
re-prefix its keys on write, and that behavior should be confirmed against the existing `Kuudra` and
`SlayerBoss` users rather than assumed.

### f04-dungeon-weight-duplication

- **Category:** `duplication`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonData.java`:24-27, :33-57
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonClass.java`:21-24, :26-50
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/skill/SkillLevel.java`:63
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/slayer/SlayerBoss.java`:54
- **What:** `DungeonData` and `DungeonClass` carry byte-identical `getExperienceTiers()`,
  `getMaxLevel()` and `getWeight()` implementations.
- **Why it is residue:** Both are `Experience, Weighted` over the same 50-entry tier table with the
  same exponent formula. The only genuine difference is that one also holds two `FloorData` fields.
- **Candidate annotation:** none - keep imperative, extract a shared type
- **Effort:** `trivial`

**Measured, not eyeballed.** `diff -u` of `DungeonData.java:24-57` against `DungeonClass.java:21-50`
produces two hunks and nothing else:

- `return DEFAULT_TIERS;` versus `return DungeonData.DEFAULT_TIERS;` - one line, and it differs only
  because `DEFAULT_TIERS` lives in `DungeonData`.
- `getFloorData(boolean)` exists only in `DungeonData` - a genuine difference, not duplication.

Everything else matches character for character: `getMaxLevel()` (3 lines of body) and `getWeight()`
(19 lines of body, including the `0.0000045254834` multiplier, the `Math.pow(rawLevel, 4.5)` base,
the `0.968` overflow exponent and both `NumberUtil.round(..., 2)` calls). **23 duplicated lines**
across the two files.

**Proposed change.** A package-private `interface DungeonWeighted extends Experience, Weighted` in
`response/skyblock/member/dungeon/` holding `DEFAULT_TIERS`, `getExperienceTiers()`, `getMaxLevel()`
and `getWeight()` as defaults. `DungeonData` and `DungeonClass` then declare `implements
DungeonWeighted` and keep only their own state - `DungeonData` keeps `getFloorData(boolean)`, and the
`DEFAULT_TIERS` constant moves off `DungeonData` so the cross-class reference at
`DungeonClass.java:23` disappears with it.

**Payoff.** 23 lines to 0 in the consumers, one copy of the weight formula instead of two. The real
prize is that a formula correction now lands in one place; today an edit to `DungeonData.getWeight()`
that misses `DungeonClass.getWeight()` produces a silently divergent total in
`SkyBlockMember.getTotalWeight()` (`SkyBlockMember.java:184-195`), which sums both.

**Not merged with the other two.** `SkillLevel.getWeight()` (`:57-78`) and `SlayerBoss.getWeight()`
(`:49-75`) are genuinely different formulas - `SkillLevel` uses a repository-supplied exponent and
divider, `SlayerBoss` runs an iterative overflow loop. They share the *shape* (guard, base,
`NumberUtil.round`, overflow branch) but not the arithmetic. Merging them would be a false
abstraction; leave them alone.

**Adjacent house-style defect.** `SkillLevel.java:63` and `SlayerBoss.java:54` both write
`experienceTiers.get(experienceTiers.size() - 1)`, while `DungeonData.java:42` and
`DungeonClass.java:35` write `experienceTiers.getLast()` for the identical expression.
`ConcurrentList` is a `SequencedCollection`, so the first form violates the collections rule. Two
one-token edits, unrelated to the abstraction but in the same four files.

**Risk.** None to serialization - neither class is Gson-constructed from JSON, both are built in
`Dungeons.postInit()` (`Dungeons.java:57-75`) through `@RequiredArgsConstructor`.

### f04-aggregate-block-triplication

- **Category:** `duplication`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/skill/Skills.java`:37-64
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/slayer/Slayers.java`:25-52
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:96-130
- **What:** Three classes each hand-write the same four aggregates - average level, summed
  experience, averaged progress percentage, and an element-to-`Weight` map - over a collection of
  things that are `Experience, Weighted`.
- **Why it is residue:** The bodies differ only in the element type and in how the collection is
  reached. The computation is identical in all three.
- **Candidate annotation:** none - keep imperative, extract a shared type
- **Effort:** `small`

**The three copies, aligned.**

| Aggregate | `Skills` | `Slayers` | `Dungeons` |
| --- | --- | --- | --- |
| average level | `getAverage()` :37 | `getAverage()` :25 | `getClassAverage()` :96 |
| summed experience | `getExperience()` :45 | `getExperience()` :33 | `getClassExperience()` :105 |
| averaged progress | `getProgressPercentage()` :53 | `getProgressPercentage()` :41 | `getClassProgressPercentage()` :113 |
| element to weight | `getWeight()` :61 | `getWeight()` :48 | `getClassWeight()` :122 |

Each body is the same three-call chain - `stream()`, `mapToDouble(X::getLevel)`, `average().orElse(0.0)`
and its variants. The only structural difference is the source: `Skills` filters through
`getSkillLevels(false)` to drop cosmetic skills, `Slayers` reads `getBosses()` directly, `Dungeons`
reads `getClasses().stream().map(Map.Entry::getValue)` out of a map. ~95 lines across the three.

`Dungeons` additionally carries a fifth copy of the weight-map aggregate at `:86-94`, over
`getDungeons()` rather than `getClasses()` - the same four lines with a different source.

**Proposed change.** One interface in `common/`:

```java
/**
 * Aggregate statistics over a collection of experience-bearing, weighted elements.
 */
public interface WeightedGroup<T extends Experience & Weighted> {

    @NotNull ConcurrentList<T> getWeightedElements();

    default double getAverage() {
        return this.getWeightedElements().stream().mapToDouble(Experience::getLevel).average().orElse(0.0);
    }

    // getExperience, getProgressPercentage, getWeight follow the same form

}
```

`Skills` implements it with `getWeightedElements()` returning `getSkillLevels(false)`, `Slayers` with
`getBosses()`, and `Dungeons` with a small adapter for the two map-valued collections - which is the
awkward part, because `Dungeons` needs the block twice over two different collections and a Java
class can implement a generic interface only once. The honest resolution is that `Dungeons` extracts
its class collection into a small `DungeonClasses` value type that implements the interface, or that
`Dungeons` keeps its five methods as thin calls into a static helper rather than implementing the
interface at all.

**Payoff.** ~95 lines to ~30 in one place plus three one-line suppliers. Realistically ~60 lines net,
because `Dungeons` will not collapse as cleanly as the other two.

**Risk.** Naming churn. `Dungeons.getClassAverage()` cannot become `getAverage()` without becoming
ambiguous against the dungeon-level aggregates on the same class, so the `Dungeons` half of this
finding is worth less than the `Skills`/`Slayers` half. If only the cheap half is taken, say so and
rate it `trivial` - two files, one new interface, no behavior change.

### f04-enum-of-parsers

- **Category:** `duplication`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonData.java`:70-75
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonClass.java`:66-71
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Statistics.java`:183-188
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/Dojo.java`:35-40
- **Where:** `src/main/java/api/simplified/hypixel/response/hypixel/HypixelRank.java`:67-72
- **What:** Five enums each declare a `static of(String)` that streams `values()`, compares with
  `equalsIgnoreCase`, and falls back to a sentinel constant.
- **Why it is residue:** Three of the five bodies are character-for-character identical. The fourth
  adds one extra `||` clause; the fifth differs only in which constant it falls back to.
- **Candidate annotation:** none - a static helper, not an annotation
- **Effort:** `trivial`

**The five bodies.** `DungeonData.Type.of`, `DungeonClass.Type.of` and the nested `Type.of` in
`Statistics` are identical - same four-line stream, same `orElse(UNKNOWN)`. `Dojo.Type.of` adds
`|| type.getInternalName().equalsIgnoreCase(name)`. `HypixelRank.Type.of` ends `orElse(NONE)`.
~30 lines, ~24 of them redundant.

**Proposed change.** One static helper. No such utility exists in the workspace today - a search for
`EnumUtil` across all modules returns nothing, and `dev.simplified.util.StringUtil` has no enum
member - so this is a new file, in `api/simplified/hypixel/common/` rather than in a shared library,
because it is not worth a JitPack cycle:

```java
public static <E extends Enum<E>> @NotNull E of(@NotNull Class<E> type, @NotNull String name, @NotNull E fallback) {
    return Arrays.stream(type.getEnumConstants())
        .filter(constant -> constant.name().equalsIgnoreCase(name))
        .findFirst()
        .orElse(fallback);
}
```

`Dojo.Type.of` keeps its own body, since its second predicate is real behavior.

**Payoff.** ~24 lines. Small, and honestly this finding is the weakest in the survey - it is worth
taking only as a rider on work that already opens those files.

**Risk.** None. All five are pure functions with no serialization role; none is called by Gson.

**Where they are actually used.** Only two of the five sit on a hot path.
`DungeonData.Type::of` is called from `Dungeons.postInit()` (`Dungeons.java:59`) to turn a JSON
*key* into an enum, which is why it exists at all - `CaseInsensitiveEnumTypeAdapterFactory` handles
enum *values* but a map key arriving as a `String` in a `postInit()` body has no adapter in scope.
`HypixelRank.Type::of` is called five times from `HypixelPlayer.getRank()` (`:97-109`). These are not
redundant with the enum adapter, and this finding does not propose removing them.

### f04-enum-null-fallback

- **Category:** `correctness`
- **Where:** `Simplified-Dev/gson-extras/.../factory/CaseInsensitiveEnumTypeAdapterFactory.java`:76-83
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:38-39
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/CrimsonIsle.java`:26-27
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/FloorData.java`:108-109
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockAuction.java`:38-39
- **What:** The enum adapter resolves an incoming value with `nameToConstant.get(in.nextString().toUpperCase())`
  and returns whatever that map lookup produces, which is `null` for any constant the enum does not
  declare.
- **Why it is residue:** Gson's reflective binder assigns a `null` returned by a field adapter to any
  non-primitive field, so a `@NotNull` enum field with a carefully chosen default initializer is
  overwritten with `null` the first time Hypixel ships a new constant. The default was written
  precisely to prevent that and does not.
- **Candidate annotation:** `@Fallback` (registry) - but see the cheaper alternative below
- **Effort:** `medium`

**The exposed fields.** Every one of these declares `@NotNull`, initializes to a sentinel, and is
bound directly from a JSON string:

| Field | Default | JSON key |
| --- | --- | --- |
| `Dungeons.selectedClass` | `DungeonClass.Type.UNKNOWN` | `selected_dungeon_class` |
| `CrimsonIsle.selectedFaction` | `Faction.NONE` | `selected_faction` |
| `FloorData.BestRun.dungeonClass` | `DungeonClass.Type.UNKNOWN` | `dungeon_class` |
| `SkyBlockAuction.rarity` | `Rarity.COMMON` | `tier` |

**Fixture state.** Currently benign. The largest fixture carries `selected_dungeon_class: "mage"`,
144 `best_runs` entries with `dungeon_class: "mage"`, and `selected_faction: "barbarians"` - all of
which resolve, the first two case-insensitively and the third through
`@SerializedName("barbarians")` on `Faction.BARBARIAN` (`CrimsonIsle.java:149-150`). The hazard is
latent, and it will fire on a game update rather than on today's data. That is exactly the kind of
defect this pack should record even though nothing is broken right now.

**Two ways to fix, and the cheap one is not `@Fallback`.**

1. **One line in the existing factory.** Have `read` return the field's declared default rather than
   `null` on a miss. The adapter does not know the field's default, so in practice this means
   returning a designated constant or leaving the field untouched - the latter requires cooperation
   from Gson's binder and is not reachable from a `TypeAdapter`. The workable version is: on a miss,
   fall back to a constant nominated on the enum itself (a `@SerializedName("")` constant, or the
   first declared constant), which costs one method in `CaseInsensitiveEnumTypeAdapter`.
2. **`@Fallback` on the field.** The registry's intent line - "supplies a default when the key is
   absent or the value fails to bind" - covers this exactly, and a field-level annotation is more
   explicit than an enum-level convention. It is also more expensive: a new annotation plus a new
   factory, plus every enum-valued field that wants it has to be annotated.

**Recommendation.** Option 1. The absent-key half of `@Fallback`'s intent is already fully served by
Java field initializers - every `@NotNull` field in this package already has one - so `@Fallback`
would earn its keep only on the failed-bind half, and the failed-bind half is a single factory's
behavior. Rated `medium` per the effort scale because it edits an existing factory, and the blast
radius is every module pinned to `gson-extras`: changing a `null` return into a constant return is a
behavior change for any consumer currently relying on the `null` to detect an unknown value.

**Verification owed.** The claim "Gson assigns the `null` to the field" is standard
`ReflectiveTypeAdapterFactory` behavior for non-primitive fields, but it has not been executed
against this pipeline. Before acting, deserialize a `FloorData.BestRun` with
`"dungeon_class": "SOMETHING_NEW"` and assert on the field. That test is the whole cost of confirming
the finding.

### f04-lookup-sentinel-drift

- **Category:** `naming`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:24-25, :78-84
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Toolkit.java`:24-32
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/skill/Skills.java`:27-29
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/AccessoryBag.java`:200-202
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/slayer/SlayerBoss.java`:77-80
- **What:** Six "look one element up in a sibling map" helpers use four different strategies for the
  miss case, with no signal in the accessor name about which one a caller is getting.
- **Why it is residue:** Not an annotation gap - a consistency gap. The repetition is small; the
  divergence is the problem.
- **Candidate annotation:** none - keep imperative
- **Effort:** `trivial`

**The four strategies.**

| Helper | Miss returns |
| --- | --- |
| `Dungeons.getClass(Type)`, `Dungeons.getDungeon(Type)` | a shared `static final` sentinel instance (`EMPTY_CLASS`, `EMPTY_DUNGEON`) |
| `Toolkit.getTool(String)`, `Toolkit.isInUse(String, int)` | a freshly allocated empty collection, or `false` |
| `Skills.getSkill(String)` | `null`, via `matchFirstOrNull` |
| `AccessoryBag.Tuning.getSlot(int)` | `Optional.empty()` |
| `SlayerBoss.isClaimed(int)` | `false`, after an explicit `data != null` guard |

`Skills.getSkill(String)` is the sharp edge: it is declared `@NotNull` at `Skills.java:27` and its
body is `matchFirstOrNull(...)`, which returns `null`. The annotation is a lie, and it is the only
one of the six a caller would reasonably dereference without checking.

**Why no annotation helps.** `@Fallback` as reserved is a *bind-time* mechanism - it supplies a value
when a key is absent from JSON. These are *lookup-time* misses on a fully-populated map, long after
binding. No annotation in the registry addresses them, and inventing one would be a
`TypeAdapterFactory` solving a problem that has nothing to do with JSON.

**Proposed change.** Pick one convention and apply it. `Optional` for scalar-or-absent, an
unmodifiable empty collection for collection-valued, and delete the two `static final EMPTY_`
sentinels in `Dungeons` in favour of `Optional` - a `DungeonData` with zero experience and two empty
`FloorData` instances reads as real data to a caller and is not distinguishable from an actual empty
dungeon.

**Payoff.** ~10 lines and two static fields. The value here is the removed `@NotNull` lie at
`Skills.java:27`, not the line count.

**Risk.** Signature changes on public accessors ripple into whatever consumes this module. Rank this
last; it is the only finding in the survey whose payoff is smaller than its review cost.

## 4. Registry entries this survey touches

Verdicts for `10-annotation-designs.md`. None of the registry names are renamed; two are argued
against on evidence.

| Registry entry | Verdict from this survey | Finding |
| --- | --- | --- |
| `@Inline` | **Reject.** Largest holder in the package has 3 fields, six of nine have 1. `@SerializedPath` covers every case at zero library cost. | `f04-delegate-rejected` |
| `@Delegate` | **Reject.** Deletes only the forwarder, keeps the holder and the nesting; cannot rename, and one site requires a rename that would also clash. Experimental Lombok. | `f04-delegate-rejected` |
| `@Fallback` | **Narrow, or reject.** The absent-key half is fully served by field initializers. The failed-bind half is real but is one behavior change inside `CaseInsensitiveEnumTypeAdapterFactory`, which is cheaper than a new annotation. | `f04-enum-null-fallback` |
| `@SerializedPath` (existing) | **Adopt more widely.** Nine holders and 13 fields are waiting for it. No library change. | `f04-nested-holder-idiom` |
| `@Capture` (existing) | **Adopt at one more site.** `FloorData`'s five `most_damage_*` keys are an enum-keyed family. No library change. | `f04-floordata-most-damage-switch` |

Net library ask from this survey: **one optional line in one existing factory**, and nothing else.
Everything else is consumer-side.

## 5. What this survey deliberately does not claim

**Transforming getters are not boilerplate.** Seven of the 34 `@Getter(AccessLevel.NONE)` sites feed
a getter that does real work - `Currencies.getEssence()` collapses a map of maps by pulling
`"current"`, `SkyBlockAuction.getLore()` splits on `\n`, `Kuudra.SearchSettings.getCombatLevel()`
parses a `"0-60"` range, `Banking.Transaction.getInitiatorName()` strips a mojibake artifact,
`SkyBlockArticle.getUrl()` builds a `URL`. Those are shape problems, and `03-value-shape-collapse.md`
owns them. Naming them here would double-count.

**`HypixelPlayer`'s ten suppressed fields are not boilerplate either.** All ten feed one
`getRank()` (`:93-124`) that applies five precedence rules and two colour overrides. That is domain
logic with no declarative form.

**`CrimsonIsle.kuudra_search_settings` and `kuudra_group_builder` are out of scope here.** They are
suppressed fields with no accessor at all, existing only so `postInit()` can push them into
`Kuudra` (`CrimsonIsle.java:53-56`). That is reach-back, owned by `02-parent-access.md`. Worth one
note in passing though: they are the only two `snake_case` Java field names in the package, and the
name misleads about what they are - a `naming` observation that belongs with whichever finding
retires them.

**A latent bug in `SerializedPathTypeAdaptorFactory` was noticed but is not a finding.** At
`:124` the empty-array guard reads `innerJsonObject.isJsonArray()` where it clearly means
`innerJsonElement.isJsonArray()`. `innerJsonObject` is the result of `getAsJsonObject()` one line
earlier, so it is never an array and the branch is dead. The effect is that an empty JSON array at
the end of a path is *not* skipped and is bound as an empty collection - which is the desirable
outcome, so nothing is broken. It is recorded here so a future reader does not mistake the dead guard
for a working one, and so `f04-nested-holder-idiom`'s expanded use of the factory is not blamed for
it later.

**No claim about `Loadouts`, `Statistics`, `HypixelForum` or `CommunityUpgrades`.** They were read.
`Statistics` (282 lines) is the largest file in the package and is almost entirely flat field
declarations with `@SerializedName` - no accessor residue. `HypixelForum` (281 lines) is the same.
Neither has a holder, a forwarder, or a repeated lookup helper.

**Serialization fidelity is asserted for `@SerializedPath`, not tested.** The re-nesting logic at
`SerializedPathTypeAdaptorFactory.java:77-89` was read closely and the prefix-sharing case is
handled, but no round-trip test was executed for a three-field shared prefix. That test is a
prerequisite of `f04-nested-holder-idiom`, not a reason to doubt it.
