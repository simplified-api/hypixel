# 03 - Value shape collapse

## 1. Scope and method

This survey owns the mismatch between the shape Hypixel sends and the shape the DTO declares:
private holder classes, wrapper-only nested types, single-field value objects, and
`Map<K, Map<String, V>>` funnels. It also covers the inverse - a declared type too *weak* for the
data, which is where `ConcurrentMap<String, Object>` and `UNKNOWN`-defaulted enums land.

Every class in `response/` was brace-scanned for instance-field count, and every
`private static class` and `@Getter(AccessLevel.NONE)` field was enumerated. Twenty classes have
exactly one instance field; eight nested classes are `private`. JSON shapes were read out of
`src/main/resources/craftedfury.json` with python (2 profiles, 2 member objects, 88 pets).

Boundaries with sibling surveys: the *accessors* deleted by a collapse are counted here as payoff
but the accessor idiom itself belongs to `04-accessor-boilerplate.md`; the repetition of a shape
across unrelated classes belongs to `06-structural-duplication.md`. Where a `postInit()` body only
exists because the bound shape is wrong, this survey files the shape and names the
`01-postinit.md` / `05-cross-field-derivation.md` consequence rather than re-deriving it.

## 2. The central question - does a single-field wrapper absorb growth?

The anchor case: `HeartOfTheForest.BiomeWhispers.Tier` is a five-line class holding one `int spent`,
so `ConcurrentMap<Integer, Tier>` could be `ConcurrentMap<Integer, Integer>`. The stated objection is
that collapsing is brittle - if Hypixel adds a second key to that object, the class absorbs it and
the scalar cannot.

That objection is half right, and the half that is wrong matters more than the half that is right.

**What the wrapper actually buys.** A wrapper does *not* absorb a new JSON key on its own. Gson
drops any key no field declares, wrapper or not. What the wrapper buys is that *adopting* the new key
is a one-line edit inside the wrapper (`private int refunded;`) that no caller sees, whereas adopting
it after a collapse means changing the field's declared type back, which every caller sees. So the
wrapper is not insurance against the API changing - it is insurance against the *source-compatibility
cost* of reacting to the API changing.

**Which makes the real question a visibility question, not a shape question.** The cost of
un-collapsing is proportional to how many callers can see the collapsed type. Where the collapsed
value is already funnelled through a narrow accessor, that number is zero and the objection
evaporates. `BiomeWhispers` already has exactly such an accessor:

```
src/main/java/api/simplified/hypixel/response/skyblock/member/foraging/HeartOfTheForest.java:51-55
    public int getSpent(int tier) {
        return Optional.ofNullable(this.getTiers().get(tier))
            .map(Tier::getSpent)
            .orElse(0);
    }
```

The leak is `@Getter` on the class also publishing `getTiers()`, which exports `Tier` to every caller.
Suppress that one accessor and the internal representation is free to flip in either direction at any
time, at the cost of one line.

**Where the argument inverts completely.** In two places the wrapper is not absorbing anything,
because the code *already throws the wrapper away* at the boundary:

- `Currencies.java:20-24` binds `essence` as `ConcurrentMap<String, ConcurrentMap<String, Integer>>`
  and the public `getEssence()` maps every value through `.get("current")`, discarding any sibling
  key that arrives. The wrapper has zero absorptive capacity here - a new `total` key would be
  silently dropped by an accessor nobody would think to change.
- `Dungeons.java:70-75` binds `player_classes` as
  `ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>>` and `postInit()` reduces each
  value to `entry.getValue().get("experience")`.

For those two, "the class absorbs it" is not a live benefit, it is a stated one. Collapsing loses
nothing that is not already lost.

**Can an annotation give both?** Three shapes were considered.

1. `@Flatten("current")` on the *field*, rewriting the JSON value before bind. Keeps the caller-facing
   type scalar, deletes the wrapper. Does **not** give both - absorbing a new sibling key still means
   changing the field's declared type. It is the honest, cheap option, and its correct scope is
   precisely the two sites above where the wrapper is already being discarded.
2. `@Flatten` on the *class*, meaning "bind a bare scalar into my sole field when the JSON is not an
   object". This is the genuine both-ways answer for the *inverse* risk - Hypixel wrapping or
   unwrapping a scalar between API versions. It removes no class and no line; it only removes a
   future breakage. There is no evidence of such a flip in the fixture, so it is proposed and ranked
   low, not recommended.
3. A wrapper that "reads transparently as its sole field" via a generated accessor. This is
   `@Delegate`, not `@Flatten`, and it deletes nothing - the class, the field and the nesting all
   survive. Rejected on this axis.

**Verdict carried into the findings.** `@Flatten` earns its keep on *map values* (`f03-mapvalue-single-key`,
`f03-dungeons-classmap-funnel`), where no existing annotation can reach the value of every entry.
It does not earn its keep on *statically keyed sub-objects* - `@SerializedPath` already collapses
those today with no library change at all (`f03-holder-collapse-serializedpath`), which is by far the
larger and cheaper win. And it should be declined on `Tier` itself (`f03-biomewhispers-tier`).

## 3. Inventory - every single-field class in the package

Twenty classes declare exactly one instance field. They fall into four groups, and only the first two
are collapse candidates.

**Group A - statically keyed holders (collapsible today with `@SerializedPath`, no library change).**

| Site | Sole field | JSON path | Visible? |
| --- | --- | --- | --- |
| `SkyBlockMember.java:213` `Events` | `chocolateFactory` | `events.easter` | private + forwarder |
| `skyblock/member/foraging/Temples.java:10` | `unlockedTemples` | `temples.unlocked_temples` | own file, forwarded from `SkyBlockMember:174` |
| `skyblock/member/WinterIsland.java:7` | `refinedJyrreUses` | `winter_player_data.refined_jyrre_uses` | own file, public getter |
| `skyblock/member/rift/VillagePlaza.java:76` `Lonely` | `secondsSitting` | `lonely.seconds_sitting` | private + forwarder |
| `skyblock/member/rift/VillagePlaza.java:84` `Seraphine` | `stepIndex` | `seraphine.step_index` | private + forwarder |
| `skyblock/member/rift/Rift.java:49` `Porhtal` | `killedEyes` | `wither_cage.killed_eyes` | private + forwarder |
| `skyblock/member/attribute/AttributeShards.java:18` `Traps` | `activeTraps` | `traps.active_traps` | private, **no forwarder** |
| `hypixel/HypixelPlayer.java:127` `Stats` | `skyBlock` | `stats.SkyBlock` | public |
| `hypixel/HypixelPlayer.java:132` `Stats.SkyBlock` | `profiles` | `stats.SkyBlock.profiles` | public |
| `skyblock/member/crimson/EdelisQuest.java:10` | `hasHeardStoryStatue` | `edelis_quest.heard_story_statue` | own file |

Two multi-field holders belong with them because the same mechanism applies:
`SkyBlockMember.java:221` `Profile` (3 fields, 3 forwarders), `Dungeons.java:141` `DungeonTreasures`
(2 fields, 2 forwarders), `Bestiary.java:142` `Miscellaneous` (2 fields, no forwarder).

**Group B - map/list value classes (need `@Flatten`; no existing annotation reaches them).**

| Site | Shape | Sole payload |
| --- | --- | --- |
| `HeartOfTheForest.java:59` `Tier` | `ConcurrentMap<Integer, Tier>` | `spent` |
| `Currencies.java:18` `essence` | `ConcurrentMap<String, ConcurrentMap<String, Integer>>` | `current` |
| `Dungeons.java:32` `classMap` | `ConcurrentMap<Type, ConcurrentMap<String, Double>>` | `experience` |
| `SkillTree.java:34` `Skill` | `ConcurrentMap<String, Skill>` | `@Capture` map of `Node` |

**Group C - domain classes that happen to have one field. Not collapse candidates.**
`DungeonClass.java:17` (one `double experience`, six behaviour methods implementing `Experience` /
`Weighted`), `skill/Skills.java:15` (one list, seven methods), `election/SpecialElection.java:9`,
`election/VotingCandidate.java:9` and `election/VotingBooth.java:12` (both extend `Candidate` /
`Election`, so the "one field" is one field *added* to an inherited set),
`forum/HypixelForum.java:63` (`channel` is the RSS document root - the nesting is the format).

**Group D - stubs whose upstream object is empty in the fixture.**
`CrystalHollows.java:26` `MinesOfDivan` and `CrystalHollows.java:35` `LostPrecursorCity`. Both read
`{}` in every member of the fixture. See `f03-crystalhollows-biomes` under section 5 - these must
not be collapsed.

## 4. Findings

### f03-holder-collapse-serializedpath

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:48, 83, 105, 162-176, 212-230
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/foraging/Temples.java`:1-15 (whole file)
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/rift/VillagePlaza.java`:24-35, 75-89
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/rift/Rift.java`:23-25, 44-54
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:42-43, 132-146
- **Where:** `src/main/java/api/simplified/hypixel/response/hypixel/HypixelPlayer.java`:80, 126-153
- **What:** Nine nested holder classes exist only to name a statically keyed JSON sub-object so its
  keys can bind. Seven of them use the full idiom - a `@Getter(AccessLevel.NONE)` field paired with a
  hand-written forwarder on the enclosing class. The other two, `HypixelPlayer.Stats` and
  `Stats.SkyBlock`, are a two-level chain of *public* holders reached through Lombok-generated
  accessors, with one hand-written `getProfiles()` at `HypixelPlayer.java`:136-138.
- **Why it is residue:** `@SerializedPath` already binds a dot path on read *and* re-nests it on
  write (`SerializedPathTypeAdaptorFactory.java`:60-92 write, 96-140 read). Every one of these
  holders is a path that `@SerializedPath` can express directly. No annotation needs to be built;
  the residue is that the annotation exists and was not applied here.
- **Candidate annotation:** `@SerializedPath` - already exists, no library change
- **Effort:** `small` - 6 files touched, consumer only, no library cycle. `00-conventions.md` §4
  reserves `trivial` for a single consumer file, and this is not one.

**Two surveys count this differently, and neither count is wrong.** `04-accessor-boilerplate.md` §2.1
censuses nine *holders* by the accessor idiom: the seven above plus `AttributeShards.Traps` and
`Bestiary.Miscellaneous`, and it never reaches `HypixelPlayer`. This finding counts nine *classes* by
the `@SerializedPath` conversion, which adds `HypixelPlayer.Stats` and `Stats.SkyBlock` and files
`Traps` / `Miscellaneous` separately as `f03-unreachable-private-holders`, because their defect is
unreachable data rather than verbosity. The union across the two findings is **11 classes across 8
files**. Cite the union or cite a finding id - never a bare "nine".

The shape of the change, using the `Rift` case:

```java
// before - Rift.java:23-25 plus 44-54
@Getter(AccessLevel.NONE)
@SerializedName("wither_cage")
private @NotNull Porhtal porhtal = new Porhtal();

public @NotNull ConcurrentList<String> getKilledEyes() {
    return this.porhtal.getKilledEyes();
}

@Getter
private static class Porhtal {

    @SerializedName("killed_eyes")
    private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();

}

// after
@SerializedPath("wither_cage.killed_eyes")
private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();
```

Payoff, counted per site (class body + field + forwarder + the import it drags in):

| Site | Classes removed | Lines removed | Public API change |
| --- | --- | --- | --- |
| `Rift.Porhtal` | 1 | 14 | none - `getKilledEyes()` survives, Lombok-generated |
| `VillagePlaza.Lonely` + `Seraphine` | 2 | 26 | none - both getters survive |
| `SkyBlockMember.Events` | 1 | 12 | none - `getChocolateFactory()` survives |
| `SkyBlockMember.Profile` | 1 | 22 | none - 3 forwarders become 3 fields |
| `SkyBlockMember` + `Temples` | 1 file, 1 class | 21 | `Temples` type disappears |
| `Dungeons.DungeonTreasures` | 1 | 15 | none - `getRuns()` / `getChests()` survive |
| `HypixelPlayer.Stats` + `Stats.SkyBlock` | 2 | 20 | `HypixelPlayer.getStats()` disappears |
| **total** | **9 classes, 1 file** | **~130** | 2 types leave the public surface |

Risks, in descending order:

1. `@SerializedPath`'s read path skips a segment whose value is an **empty object or array**
   (`SerializedPathTypeAdaptorFactory.java`:120-124). For these sites that is benign - skipping
   leaves the field at its initializer, which is what the holder produced anyway.
2. `Temples` and `HypixelPlayer.Stats` are named types in the public surface. `Temples` is referenced
   only by `SkyBlockMember` (import at :11, field at :84, forwarder at :174); `Stats` has no
   in-module reference other than the field. Both are safe to delete inside this repo but are a
   binary-compatibility break for any downstream consumer.
3. Fixture evidence that these paths really are single-key: across both members,
   `temples` union is `{unlocked_temples}`, `winter_player_data` union is `{refined_jyrre_uses}`,
   `events` union is `{easter}`. `winter_player_data` is the shakiest - Jerry's Workshop is a
   seasonal event whose payload has historically grown - so `WinterIsland` is listed in section 5
   as a decline, not here.
4. The `HypixelPlayer.Stats` chain is not the paired idiom and does not convert like the other seven.
   `HypixelPlayer.java`:80 declares `private Stats stats;` with no `@Getter(AccessLevel.NONE)` and no
   forwarder, so Lombok publishes `getStats()`; `Stats.SkyBlock` (:132) carries no class-level
   `@Getter` at all but hand-writes `getProfiles()` (:136-138), which wraps the map in an
   unmodifiable list. Collapsing to `@SerializedPath("stats.SkyBlock.profiles")` therefore relocates
   both `getProfiles()` and the nested `Profile` class onto `HypixelPlayer`, and it changes two
   public accessor shapes rather than none. Sequence it apart from the seven clean sites.

### f03-unreachable-private-holders

- **Category:** `correctness`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/attribute/AttributeShards.java`:12, 17-23
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Bestiary.java`:36, 141-150
- **What:** Two holder fields are typed as a `private static class` and carry a class-level `@Getter`
  with no `@Getter(AccessLevel.NONE)` and no forwarder, so Lombok emits `public Traps getTraps()` and
  `public Miscellaneous getMiscellaneous()` returning types no caller outside the enclosing class can
  name. Three bound values are therefore decoded and then unreachable.
- **Why it is residue:** the holder pattern was applied without the forwarder half. Compare
  `Rift.java`:23-25 and `VillagePlaza.java`:24-35, which get it right. The data lost is
  `shards.traps.active_traps` (a whole `ActiveTrap` list plus the `ActiveTrap` class at
  `skyblock/member/attribute/ActiveTrap.java`), `bestiary.miscellaneous.max_kills_visible` and
  `bestiary.miscellaneous.milestones_notifications`.
- **Candidate annotation:** `@SerializedPath` - already exists, no library change
- **Effort:** `trivial`

```java
// AttributeShards.java - replaces the field at :12 and the class at :17-23
@SerializedPath("traps.active_traps")
private @NotNull ConcurrentList<ActiveTrap> activeTraps = Concurrent.newList();
```

This is filed as `correctness` rather than `value-shape-collapse` because the defect is not that the
shape is verbose - it is that `ActiveTrap` and both `Miscellaneous` booleans are dead. A grep over
`src/` finds no reference to `getTraps`, `getActiveTraps` or `getMiscellaneous` anywhere, including
tests. Fixture note: `shards.traps` is `{}` in the only member that has it, so the loss is currently
invisible at runtime - which is exactly why it has survived.

Payoff: 2 classes removed, ~17 lines, and 3 fields become reachable for the first time.

### f03-mapvalue-single-key

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Currencies.java`:17-24
- **What:** `essence` binds as `ConcurrentMap<String, ConcurrentMap<String, Integer>>` because
  Hypixel wraps each essence count in a one-key object; the accessor immediately re-reduces every
  value through `.get("current")` and hands back `ConcurrentMap<String, Integer>`.
- **Why it is residue:** the declared type, the `@Getter(AccessLevel.NONE)` suppression and the
  five-line stream accessor all exist to undo one level of JSON nesting that no other annotation can
  reach - `@SerializedPath` addresses a static path, and there is no static path here because the
  wrapper sits inside every map *value*. This is the case `@Flatten` was reserved for.
- **Candidate annotation:** `@Flatten`
- **Effort:** `small`

Fixture, `profiles[1].members[…].currencies`:

```json
{"coin_purse": 54601987.695, "motes_purse": 806795,
 "essence": {"WITHER": {"current": 1955}, "DRAGON": {"current": 1132}, "UNDEAD": {"current": 5141},
             "DIAMOND": {"current": 8564}, "SPIDER": {"current": 312}, "GOLD": {"current": 3445},
             "ICE": {"current": 2557}, "CRIMSON": {"current": 4}}}
```

Proposed:

```java
@Flatten("current")
private @NotNull ConcurrentMap<String, Integer> essence = Concurrent.newMap();
```

Payoff: one nested generic parameter removed, one `@Getter(AccessLevel.NONE)`, one five-line
accessor, one `AccessLevel` import. Ten lines, but the real gain is that the public type stops lying:
`getEssence()` currently promises a plain map and the field says otherwise.

Adversarial note on this exact site. `"current"` is a hedged key name - it reads like a field that
expects a `total` or `lifetime` sibling, which is the strongest growth signal in the whole survey.
And `Currencies` itself shows the API is not consistent: `motes_purse` and `coin_purse` sit
*unwrapped* in the same object while `essence` is wrapped. So this object genuinely may grow.
That does not save the wrapper, because `getEssence()` already discards anything that is not
`current` and would keep discarding it. Collapsing makes the loss visible at the field rather than
hiding it in an accessor - which is an improvement, not a regression. If growth is the real worry,
the countermeasure is a fixture assertion on the key set, not a wrapper class.

Serialization must be part of the design: `@Flatten` has to re-wrap on write or `essence` round-trips
as `{"WITHER": 1955}` and silently changes the document. `@Lenient` and `@Collapse` both preserve
round-trip fidelity, so this is the established bar.

### f03-biomewhispers-tier

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/foraging/HeartOfTheForest.java`:45-65
- **What:** `BiomeWhispers.tiers` is `ConcurrentMap<Integer, Tier>` where `Tier` is a five-line class
  holding one `int spent`; the only consumer is `getSpent(int tier)`, which unwraps it.
- **Why it is residue:** structurally identical to `f03-mapvalue-single-key`, and `@Flatten("spent")`
  would collapse it to `ConcurrentMap<Integer, Integer>`.
- **Candidate annotation:** `@Flatten` - but see the verdict below
- **Effort:** `trivial` once `@Flatten` exists; `small` if it is built for this site alone

**Verdict: decline the collapse, take the visibility fix instead.**

The fixture argues against collapsing here in a way it does not for `essence`:

```json
"whispers": {"forest": {"1": {"spent": 9919828}, "2": {"spent": 0}, "3": {"spent": 0},
                        "4": {"spent": 0}, "5": {"spent": 0}, "total": 9930772},
             "desert": {"1": {"spent": 969512}, ..., "total": 1016315}}
```

and in the *other* profile, `desert` has no `total` at all. So this key family is demonstrably
mid-growth: Hypixel added a sibling key to the whispers object between profile states, and the
per-tier objects sit one level below that same churn. `Tier` is the cheapest possible place to absorb
a `refunded` or `claimed_at` key when it appears.

The cost of keeping `Tier` is five lines and one class. The cost of collapsing and reverting is a
public type change on a map exported by a Lombok `@Getter`. Those are not symmetric, and the
asymmetry is entirely due to `getTiers()` being public. So the change worth making is to stop
publishing the map:

```java
@Capture
@Getter(AccessLevel.NONE)
private @NotNull ConcurrentMap<Integer, Tier> tiers = Concurrent.newMap();

public int getSpent(int tier) {
    return Optional.ofNullable(this.tiers.get(tier))
        .map(Tier::getSpent)
        .orElse(0);
}
```

**Two lines, not one.** `getSpent(int)` at :51-55 reads `this.getTiers()` today, and `getTiers()` is
exactly the accessor `@Getter(AccessLevel.NONE)` deletes, so the body has to move to the field or the
class stops compiling. `AccessLevel` is already imported at :8, so the suppression drags in nothing.
After that, `Tier` is an implementation detail and can be collapsed or grown at any later date with
no caller impact - which is the "both" the question was asking for, obtained without any annotation.

This is the survey's clearest demonstration that the wrapper-versus-scalar debate is usually decided
by accessor visibility rather than by the shape itself.

### f03-skilltree-capture-holder

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/SkillTree.java`:16-17, 32-39
- **What:** `nodes` binds as `ConcurrentMap<String, Skill>` where `Skill` declares nothing but a
  single `@Capture ConcurrentMap<String, Node> entries`; the caller wants
  `ConcurrentMap<String, ConcurrentMap<String, Node>>`.
- **Why it is residue:** `Skill` is not a domain concept, it is a *carrier for an annotation*.
  `@Capture` selects affix-grouping mode from the declared value type, so the only way to affix-group
  a map that is itself a map value is to interpose a class with a `@Capture` field on it. The
  wrapper exists because the annotation cannot be expressed one level deeper.
- **Candidate annotation:** an element on `@Capture` that applies to a map's value type, or
  `@Flatten` reading through a class whose sole field is the capture target
- **Effort:** `medium` - modifies `CaptureTypeAdapterFactory`'s grouping selection, which every
  existing `@Capture` user shares

Fixture, `skill_tree.nodes.mining`:

```json
{"core_of_the_mountain": 10, "toggle_core_of_the_mountain": true,
 "mining_speed": 50, "toggle_mining_speed": true, "mining_fortune": 50, ...}
```

`Node` at :43-50 uses `@SerializedName("")` for `level` and `@SerializedName("toggle_")` for
`enabled`, which is the affix-grouping contract - a bare name is treated as an auto-suffix and
`toggle_` as a prefix, so `mining_speed` / `toggle_mining_speed` fold into one `Node`.

Payoff is one class and 8 lines, and one level of caller indirection
(`getNodes().get("mining").getEntries()` becomes `getNodes().get("mining")`). Rated low priority:
the payoff is the smallest of the collapse findings and the cost is the highest, because it lands in
the busiest existing factory. Recorded here so the design document can see that `@Capture`'s
value-type-drives-mode rule has a structural cost, not because the site is worth fixing on its own.

### f03-dungeons-classmap-funnel

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:30-32, 53, 70-75
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonClass.java`:17-19
- **What:** `player_classes` binds to `ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>>`
  under a suppressed getter, and half of `Dungeons.postInit()` exists solely to turn that into
  `ConcurrentMap<DungeonClass.Type, DungeonClass>` by reading `.get("experience")` out of each value.
- **Why it is residue:** the JSON is already exactly the shape of `DungeonClass`. The funnel type is
  a transcription of the JSON rather than of the domain, and the `postInit()` loop is the cost of
  that choice.
- **Candidate annotation:** none needed for the direct form; `@Flatten` for the scalar form
- **Effort:** `small`

Fixture, `dungeons.player_classes`:

```json
{"healer": {"experience": 84271835.04}, "mage": {"experience": 409047204.36},
 "berserk": {"experience": 92858814.02}, "archer": {"experience": 98301741.50},
 "tank": {"experience": 96565524.56}}
```

`DungeonClass` declares exactly one field, `private final double experience`, and nothing else that
binds. Declaring the map as `ConcurrentMap<DungeonClass.Type, DungeonClass>` therefore binds
directly, deletes the funnel field, the `@Getter(AccessLevel.NONE)`, the transient `classes` field
and the second half of `postInit()` at :70-75 - six lines of stream code, plus it removes one of the
two reasons `Dungeons` implements `PostInit` at all. `11-postinit-elimination.md` should pick this up.

Two things to verify before adopting, both cheap and both real:

1. `DungeonClass` has `@RequiredArgsConstructor` and no no-argument constructor, so Gson would
   instantiate it through its unsafe allocator and assign a **`final`** field reflectively. That
   works, but the safe form is to drop `final` and add
   `@NoArgsConstructor(access = AccessLevel.PRIVATE)`, matching what `FloorData` already does.
   `DungeonClass` is currently never Gson-bound - it is only ever constructed by hand at
   `Dungeons.java`:24 and :73 - so this would be its first time through the reflective adapter.
2. The map key is `DungeonClass.Type`, which is bound by `CaseInsensitiveEnumTypeAdapterFactory`;
   see `f03-enum-unknown-null` for what happens to a class name Hypixel adds later.

If keeping `DungeonClass` out of the binder is preferred, `@Flatten("experience")` on a
`ConcurrentMap<DungeonClass.Type, Double>` gets the same `postInit()` deletion at the cost of the
library cycle. The direct form is strictly cheaper and is the recommendation.

### f03-object-escape-hatches

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Statistics.java`:39-40
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/pet/OwnedPet.java`:40
- **Where:** `src/main/java/api/simplified/hypixel/response/hypixel/HypixelPlayer.java`:42-43, 82-91
- **What:** Eleven fields across eight files are typed `ConcurrentMap<String, Object>` or
  `ConcurrentList<Object>`. Two of them are typeable today with annotations that already exist; one
  is a filter written by hand; the rest are genuinely unknown and should stay.
- **Why it is residue:** `Object` was chosen because one entry in the object had a different value
  type from the rest, which is precisely what `@Lenient` plus `@Extract` was built for.
- **Candidate annotation:** `@Lenient` + `@Extract` - already exist, no library change
- **Effort:** `small`

The full census, with the fixture verdict for each:

| Site | Fixture content | Verdict |
| --- | --- | --- |
| `Statistics.java:40` `riftStats` | 31 keys: 29 `int`, 2 objects (`west_vermin_vacuumed`, `shen_item_bought`) | typeable now |
| `OwnedPet.java:40` `extra` | 3 non-empty across 88 pets, all `int` | typeable now |
| `HypixelPlayer.java:43` `achievementsOneTime` | mixed, filtered by hand at :82-91 | typeable now |
| `CrimsonIsle.java:66` `questRewards` | mixed `int` and `String` by key | see `f03-questrewards-mixed-values` |
| `CrimsonIsle.java:68` `minibossDaily` | `{}` | keep `Object` |
| `CrimsonIsle.java:70` `kuudraBossDaily` | `{}` | keep `Object` |
| `CrimsonIsle.java:93` `rulenor` | `{}` | keep `Object` |
| `Abiphone.java:50` `specific` | `{"last_mistake": 1666206466366, "color_index_given": 5}` - `int` and `long` | keep `Object` until a wider sample exists |
| `VillagePlaza.java:22` `barterBank` | `{}` | keep `Object` |
| `CrystalHollows.java:29` `placedStatues` | `[]` | keep `Object` |
| `CrystalHollows.java:38` `deliveredParts` | `[]` | keep `Object` |

The `riftStats` case is the strongest, because the escape hatch is paying for exactly two keys:

```java
@Lenient
@SerializedName("rift")
private @NotNull ConcurrentMap<String, Integer> riftStats = Concurrent.newMap();
@Extract("riftStats.west_vermin_vacuumed")
private @NotNull VerminVacuumed verminVacuumed = new VerminVacuumed();
```

`@Lenient` diverts the two object-valued entries to overflow and preserves them for round-trip;
`@Extract` pulls the one that has a known shape back into a typed field by the lenient field's
**Java** name. 29 statistics stop being `Object`. This costs one new four-field nested class and
no library change.

`OwnedPet.extra` becomes `@Lenient ConcurrentMap<String, Long>` - strictly better than `Object`
because a non-numeric value lands in overflow rather than silently becoming an untyped entry, and
the 88-pet sample only shows integers. Rated lower confidence than `riftStats` because `extra` is
an open per-pet bag by design.

`HypixelPlayer.achievementsOneTime` is the same idea done imperatively - `getAchievementsOneTime()`
at :82-91 lazily filters the list to `String` and memoizes it into a second suppressed field.
`@Lenient ConcurrentList<String>` does that at bind time and deletes both fields, the accessor and
the memo. That one is `trivial`; it is included here because the `Object` type is the cause.

The six empty-object sites must keep `Object` - the fixture proves nothing about a `{}`, and guessing
a type there is how `@Lenient` overflow silently fills up.

### f03-questrewards-mixed-values

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/CrimsonIsle.java`:65-66
- **What:** `quest_rewards` is one JSON object carrying two unrelated maps interleaved by value type -
  `<questId> -> <itemId>` with `String` values, and `<itemId> -> <count>` with `int` values - so the
  field is typed `ConcurrentMap<String, Object>` and neither map is usable without a cast.
- **Why it is residue:** the caller wants two typed maps. `@Lenient` can divert one value type to
  overflow, but `@Extract` addresses a single named key, so there is no way to pull "every
  `String`-valued entry" back out. Two fields cannot both claim `quest_rewards` because Gson rejects
  duplicate serialized names.
- **Candidate annotation:** an element on `@Lenient` that types the overflow into a sibling field,
  rather than a new annotation
- **Effort:** `medium` - edits `LenientTypeAdapterFactory`, which seven files already depend on

Fixture, `nether_island_player_data.quests.quest_rewards`:

```json
{"KADA_LEAD": 10, "crimson_isle_kill_barbarian_duke_x_c": "KADA_LEAD",
 "MOOGMA_PELT": 2, "crimson_isle_lavahorse_c": "MOOGMA_PELT",
 "GAZING_PEARL": 2, "crimson_isle_fetch_magmag_b": "GAZING_PEARL",
 "BEZOS": 3, "crimson_isle_dojo_test_of_wall_jump_brating_a": "BEZOS",
 "LEATHER_CLOTH": 2, "crimson_isle_fight_kuudra_burning_tier_s": "LEATHER_CLOTH"}
```

Ranked low. It is a single site, the required library change lands in the second-most-used factory,
and there is a zero-cost partial: `@Lenient ConcurrentMap<String, Integer>` alone types the counts
correctly today and parks the quest-to-item mapping in overflow, where it round-trips but is not
readable. That is already better than `Object` for half the data, and it needs nothing built. Take
the partial; do not build the general overflow-typing element for one site.

### f03-enum-unknown-null

- **Category:** `correctness`
- **Where:** `W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras/src/main/java/dev/simplified/gson/factory/CaseInsensitiveEnumTypeAdapterFactory.java`:76-83
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:38-39
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonRun.java`:23-24
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/FloorData.java`:108-109
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/BoardQuest.java`:15
- **What:** Four fields declare `@NotNull SomeEnum x = SomeEnum.UNKNOWN`, but
  `CaseInsensitiveEnumTypeAdapter.read` returns `nameToConstant.get(in.nextString().toUpperCase())`,
  which is `null` for any value not in the enum. Gson's reflective binder assigns that `null` over
  the initializer for a non-primitive field, so the `UNKNOWN` default never survives an unrecognized
  value - the field ends up `null` behind a `@NotNull`.
- **Why it is residue:** the `UNKNOWN` constant plus the hand-written
  `of(String)` fallback are an attempt to express "unknown API value maps to a sentinel" at the wrong
  layer. The bind layer overwrites the sentinel, and the `of(String)` methods that would have
  restored it are mostly not on the bind path at all.
- **Candidate annotation:** `@Fallback` on the field, or an enum-level unknown-constant convention in
  `CaseInsensitiveEnumTypeAdapterFactory`
- **Effort:** `medium` - `CaseInsensitiveEnumTypeAdapterFactory` is registered for every enum in
  every consuming module, so any change to its miss behaviour needs a regression pass beyond this
  repo

Evidence that the `of(String)` layer is not carrying its weight. Five enums declare
`UNKNOWN` and/or a static `of(String)`; only two of the `of` methods are ever called:

| Enum | `UNKNOWN` | `of(String)` | Callers of `of` |
| --- | --- | --- | --- |
| `DungeonData.Type`:61-77 | yes | yes | **none** |
| `Statistics.EndIsland.DragonFight.Type`:169-190 | yes | yes | **none** |
| `DungeonClass.Type`:53-73 | yes | yes | 1 - `DungeonRun.java`:50, from a display-name regex, not from JSON |
| `Dojo.Type`:22-42 | yes | no | n/a - matches on `internalName` too |
| `BoardQuest.Status`:20-25 | yes | no | n/a |
| `HypixelRank.Type`:33 | no | yes | 5 - `HypixelPlayer.java`:97-109, all from `String` fields |

So two of the three `of(String)` bodies are dead code, and the remaining bind-path safety net does
not exist: when Hypixel ships a seventh dungeon class, `Dungeons.selectedClass` becomes `null`, not
`UNKNOWN`, and every `@NotNull` on that field is wrong. Nothing in
`src/test/java/.../MemberDtoMappingTest.java` covers this - the only `UNKNOWN` assertions there
(:104-105) exercise the `getOrDefault` lookups, not the bind.

The cheap consumer-side confirmation is a one-line test decoding
`{"selected_dungeon_class": "necromancer"}`. That test should be written before any library change is
scoped, because it decides whether this is a two-line factory fix or a `@Fallback` design.

The *repetition* of the `UNKNOWN` + `of(String)` idiom across files is real but is
`06-structural-duplication.md`'s to file; what is owned here is the declared-type-versus-data
mismatch, which is a defect regardless of how many times it appears.

### f03-optional-audit

- **Category:** `value-shape-collapse`
- **Where:** 56 `Optional<T>` fields across 30 files in `response/`
- **What:** the brief asked for `Optional<T>` fields that are never absent in practice and could be
  demoted to the bare type. **No finding.** The audit is reported because a negative result here is
  load-bearing for the other findings.
- **Why it is not residue:** Hypixel emits *explicit nulls*, not just missing keys, and `Optional`
  is the correct model for that. Across the 88 pets in the fixture: `uuid` is present 88/88 with 1
  explicit `null`, `heldItem` 88/88 with 11 nulls, `skin` 88/88 with 70 nulls, `heldItemUuid` 8/88.
  A demotion to `UUID` / `String` would replace a modelled absence with a raw `null` and move the
  check from the type system into every caller.
- **Candidate annotation:** none - keep as is
- **Effort:** n/a

Two observations that fall out of the audit and belong to other surveys:

- The fixture is only 2 members and 2 profiles. It cannot support a claim that any key is *never*
  absent, so no demotion could be justified from it even where one looked plausible. Recording this
  so the question is not re-opened on the same evidence.
- Boxed-versus-primitive inconsistency was checked and is not a real pattern.
  `Loadouts.Loadout`:79-88 uses `Optional<Integer>` for four slot ids where absence means "no slot
  selected", which is a genuine tri-state, while flat counters elsewhere use `int`. The two are
  modelling different things, not the same thing inconsistently.

One real shape finding did surface from the audit and is recorded here rather than given its own ID,
because it is a single site with an annotation that already exists:

### f03-kuudra-combat-range

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/Kuudra.java`:42-50
- **What:** `combat_level` binds as `Optional<String>` holding a delimited range such as `"5-10"`,
  suppressed by `@Getter(AccessLevel.NONE)`, and `getCombatLevel()` splits it on `-`, parses both
  halves and builds a `Range<Integer>` by hand.
- **Why it is residue:** `@Split` exists precisely to turn one delimited string into a
  `Pair` / `PairOptional` at bind time, deserializing each half to its generic argument. The
  hand-written `StringUtil.split` plus two `Integer.parseInt` calls is the shape mismatch made
  imperative, and it throws on a malformed value where `PairOptional` would yield `empty()`.

```java
@Split("-")
@Getter(AccessLevel.NONE)
private @NotNull PairOptional<Integer, Integer> combatLevel = PairOptional.empty();
```

`getCombatLevel()` survives, reduced to building the `Range` from the pair - the parse and the
null-guard both go.
- **Candidate annotation:** `@Split` - already exists, no library change
- **Effort:** `trivial`

The Java field name `combat_level` is snake_case, which is `naming` rather than shape and belongs to
whichever survey files naming defects; it is mentioned only because it disappears with the same edit.

## 5. Rejected - shapes that should stay as they are

Each of these looks like a collapse candidate to the brace-scan and is not one. They are listed with
their reason so the same ground is not re-surveyed.

### f03-crystalhollows-biomes

`CrystalHollows.java`:24-40. `MinesOfDivan` and `LostPrecursorCity` each hold one
`ConcurrentList<Object>`, and both are `{}` in every member of the fixture. The decisive evidence is
their two siblings in the same parent object: `mining_core.biomes.goblin` and
`mining_core.biomes.jungle` both carry **two** keys each, and they bind to `GoblinHoldout` and
`JungleTemple` at :44-62. All four are the same kind of thing - a Crystal Hollows area - so the two
that currently look single-valued are single-valued only because this account has not progressed
them. Collapsing them would guarantee a revert. Keep both classes.

Related observation while here: `@SerializedPath("goblin")` and `@SerializedPath("jungle")` at :19-22
are single-segment paths, which is what `@SerializedName` does. Harmless, but they pull the whole
class through `SerializedPathTypeAdaptorFactory`'s tree round-trip for no gain, and they read as if
something deeper were intended.

### f03-winterisland-seasonal

`skyblock/member/WinterIsland.java` is a whole file for one `int refined_jyrre_uses`, and the fixture
union for `winter_player_data` is exactly that one key. Structurally it is a Group A collapse.
Declined anyway: Jerry's Workshop is a seasonal event, the fixture was captured outside the event
window, and a seasonal payload observed at its minimum is the worst possible evidence for a
permanent shape decision. Cost of keeping it is 12 lines. Revisit with a December capture, not with
this one.

### f03-quest-family-symmetry

`crimson/EdelisQuest.java` holds one boolean and sits in a family of ten `*Quest` classes bound from
`CrimsonIsle.Quests`:73-92 - `AlchemistQuest`, `ChickenQuest`, `NpcQuest`, `SuusQuest`, `PabloQuest`,
`DuelTrainingQuest`, `SirihQuest`, `MollimQuest`. Collapsing the one that currently has a single
field would make it the only quest addressed by path instead of by type, for a saving of ten lines.
The family symmetry is worth more than the lines. Keep.

### f03-domain-single-field-classes

`DungeonClass.java`:17, `skill/Skills.java`:15, `election/SpecialElection.java`:9,
`election/VotingCandidate.java`:9, `election/VotingBooth.java`:12, `forum/HypixelForum.java`:63.
These have one *field* but are not wrappers - four of them carry behaviour (`DungeonClass` and
`Skills` implement `Experience` / `Weighted` with six and seven methods respectively), two add one
field to an inherited base (`VotingCandidate extends Candidate`, `VotingBooth extends Election`), and
`HypixelForum.channel` is the RSS document root, where the nesting is the wire format rather than a
modelling choice. No action.

### The class-level `@Flatten` idea

Section 2 option 2 - a `@Flatten` on the *class* meaning "bind a bare scalar into my sole field when
the incoming JSON is not an object" - is technically sound and would immunise every Group B class
against Hypixel wrapping or unwrapping a scalar. It is not proposed for adoption: the fixture shows
no such flip anywhere, it removes no code, and it would add a second meaning to an annotation name
already reserved for the field-side collapse. Recorded so the design document can decline it
explicitly rather than rediscover it.

## 6. Summary table

Ordered by payoff per unit of effort.

| ID | Category | Candidate | Effort | Payoff | Library cycle |
| --- | --- | --- | --- | --- | --- |
| `f03-holder-collapse-serializedpath` | `value-shape-collapse` | `@SerializedPath` (exists) | `small` | 9 classes, 1 file, ~130 lines (11 classes with `f03-unreachable-private-holders`) | none |
| `f03-unreachable-private-holders` | `correctness` | `@SerializedPath` (exists) | `trivial` | 2 classes, ~17 lines, 3 fields made reachable | none |
| `f03-dungeons-classmap-funnel` | `value-shape-collapse` | none - retype the map | `small` | 1 funnel type, 1 transient, 6 lines of `postInit()` | none |
| `f03-object-escape-hatches` | `value-shape-collapse` | `@Lenient` + `@Extract` (exist) | `small` | 3 of 11 `Object` fields typed; 29 rift statistics become `Integer` | none |
| `f03-kuudra-combat-range` | `value-shape-collapse` | `@Split` (exists) | `trivial` | hand-rolled range parse deleted | none |
| `f03-optional-audit` | `value-shape-collapse` | none - keep as is | n/a | negative result, closes the question | none |
| `f03-mapvalue-single-key` | `value-shape-collapse` | `@Flatten` | `small` | 1 nested generic, 1 accessor, ~10 lines | one |
| `f03-enum-unknown-null` | `correctness` | `@Fallback` or factory fix | `medium` | 4 `@NotNull` fields stop being nullable; 2 dead `of(String)` bodies | one, plus cross-module regression |
| `f03-biomewhispers-tier` | `value-shape-collapse` | declined - suppress the getter instead | `trivial` | 2 lines; makes the shape freely reversible | none |
| `f03-skilltree-capture-holder` | `value-shape-collapse` | `@Capture` element | `medium` | 1 class, 8 lines | one, touches the busiest factory |
| `f03-questrewards-mixed-values` | `value-shape-collapse` | `@Lenient` element - take the partial instead | `medium` | half the site typed for free; full fix not worth it | none for the partial |

Three conclusions the design document should carry forward.

1. **The largest win on this axis needs no annotation.** `@SerializedPath` already does what nine
   holder classes are doing by hand. Around 150 of the ~180 lines this survey can delete come from
   two zero-library-cycle findings that build nothing. Anything that spends a JitPack cycle should be
   sequenced after those.
2. **`@Flatten` is justified, but narrowly.** Its only unambiguous site is `Currencies.essence`,
   because that is where the wrapper sits inside a map value *and* the wrapper is already being
   discarded by an accessor. `Dungeons.player_classes` reaches the same end more cheaply without it,
   and `HeartOfTheForest.Tier` should keep its wrapper. One clear site is a thin justification for a
   library cycle - the design document should decide whether it rides along with another change.
3. **The wrapper-versus-scalar tradeoff is decided by accessor visibility, not by the shape.**
   Wherever the sole consumer is a narrow accessor, the internal shape is free and the "brittleness"
   objection costs nothing to satisfy. Where the wrapper is published by a Lombok `@Getter`, it is
   the publication - not the class - that should be reconsidered first.
