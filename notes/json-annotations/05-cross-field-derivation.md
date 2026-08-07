# 05 - Cross-Field Derivation

## 1. Scope and method

This survey owns every place a field's value is a function of *other* fields rather than of the JSON
key it is named after. That includes joins between two sibling collections, max-of-matching-key scans,
id-to-repository resolution, string-key parsing that produces typed siblings, and materialized
aggregates.

Boundary with siblings, stated once so nothing is double-claimed:

- The **hook** - whether `postInit()` should exist at all - belongs to `01-postinit.md`. This document
  categorises by *cause*, so a computation that happens to live in a `postInit()` body is still
  `cross-field-derivation` here when the finding is about what it computes, not about where it runs.
- The **reach-back** in `AccessoryBag.initialize(member)` belongs to `02-parent-access.md`. This
  document claims only the *ordering* and *dead-store* defects inside that method, because their cause
  is derivation order, not parent access.
- `Experimentation.Table`'s `transient` + `@Capture` fields and the `Kuudra.SearchSettings`
  combat-level string split are bind-shape questions, not derivation. Left to `03` and to whichever
  survey files the `@Split` widening.

Evidence rules followed: every claim below cites `file:line` against real source, and every JSON-shape
claim was measured against `src/main/resources/craftedfury.json` with python rather than assumed. The
fixture holds 2 profiles and 2 members; the richer member has 100 `collection` entries and 775
`unlocked_coll_tiers` strings.

Three of the nine findings are `correctness`, and two of those are live, currently-wrong output rather
than latent risk. That is the headline result: the derivation residue is not merely verbose, it is
already producing wrong numbers, and it does so *silently* because
`PostInitTypeAdapterFactory.java:35-38` catches `Exception` into an empty block.

## 2. The anchor - `SkyBlockMember.collectionUnlocked` decoded

### 2.1 Inputs

Two fields, on two different objects, one nested inside the other.

| Java | JSON key | Shape | Fixture size |
| --- | --- | --- | --- |
| `SkyBlockMember.collection` (`SkyBlockMember.java:129`) | `collection` | `itemId -> total ever collected` | 100 entries |
| `SkyBlockMember.playerData.unlockedCollectionTiers` (`PlayerData.java:32-33`) | `player_data.unlocked_coll_tiers` | flat `List<String>`, each `"<itemId>_<tier>"` | 775 strings |

The output is `SkyBlockMember.collectionUnlocked` (`SkyBlockMember.java:130`), a
`transient ConcurrentMap<String, Integer>` of `itemId -> highest tier unlocked`. It is written once,
in `SkyBlockMember.postInit()` (`SkyBlockMember.java:145-155`), and never again.

The two inputs are *not* siblings on the same class. `collection` is a direct field of
`SkyBlockMember`; `unlockedCollectionTiers` is a field of `PlayerData`, which is a field of
`SkyBlockMember`. Any declarative annotation would therefore have to address its source by a **Java
field path across an object boundary** - `playerData.unlockedCollectionTiers` - which is the same
addressing style `@Extract` already uses (`Bestiary.java:33` reads
`@Extract("kills.last_killed_mob")`, where `kills` is a Java field name). That precedent exists, but it
has so far only ever crossed from a field into that field's own overflow, never into another DTO.

### 2.2 Pseudo-code

The user asked for this explicitly. Read it as: *"for every item I have ever collected, what is the
highest collection tier I have unlocked for it?"*

```
collectionUnlocked = {}

for each (itemId, amountCollected) in collection:          # 100 iterations
    best = 0                                               # default when nothing matches
    for each tierString in playerData.unlockedCollectionTiers:   # 775 iterations
        if tierString fully matches  ^<itemId>_<one-or-more-digits>$ :
            n = integer( tierString with the literal prefix "<itemId>_" removed )
            best = max(best, n)
    collectionUnlocked[itemId] = best                       # amountCollected is DISCARDED
```

Four things that are easy to forget and are the reason the method reads as noise:

1. **The value side of `collection` is thrown away.** `amountCollected` never reaches the output. The
   map is used purely as *the set of item ids to iterate*. The join is therefore
   "tier list, restricted to items I have collected at least once", not a merge of two values.
2. **The direction matters.** The loop is driven by `collection`, so an item that has unlocked tiers
   but zero collected is **absent** from the output and reads back as `0` through
   `getOrDefault`. In the fixture this loses nothing (0 orphan ids), but it is a real semantic choice,
   not an accident of iteration.
3. **A missing item is `0`, not absent-with-meaning.** Eleven fixture items (`ENCHANTED_REDSTONE`,
   `ENCHANTED_MELON`, `CRUDE_GABAGOOL`, ...) are in `collection` with no tier string at all and land on
   the `orElse(0)` at `SkyBlockMember.java:153`.
4. **The regex only accepts non-negative tiers.** `[\\d]+` does not match a leading `-`, so
   `MELON_-1`-style entries are skipped. Section `f05-negative-tier-exclusion` shows this is
   accidentally the right behaviour.

### 2.3 What the fixture says

Measured, not assumed:

- `775` tier strings, `100` collection ids, `89` distinct ids appearing in tier strings.
- `83` of the 775 tier strings end in `_-1`. That is 11% of the list, so the negative tier is a normal,
  expected marker, not corruption.
- **No item's maximum tier is negative.** Every id that has a `_-1` entry also has `_1` through `_9`.
- `0` tier-string ids are absent from `collection`, so the join direction loses nothing *today*.
- `0` collection ids and `0` tier strings contain a regex metacharacter, so the unquoted
  `String.format("^%s_[\\d]+$", itemId)` at `SkyBlockMember.java:150` does not currently misfire. It is
  still unquoted user-ish data compiled as a pattern; `Pattern.quote` costs nothing.
- The `LOG` / `LOG_2` pair is the interesting adversarial case, and the current regex handles it
  **correctly**: `^LOG_[0-9]+$` claims exactly `LOG_1`..`LOG_9` and never `LOG_2_5`, while
  `^LOG_2_[0-9]+$` claims exactly `LOG_2_1`..`LOG_2_9`. A naive "strip the longest known id prefix"
  index would break this; an index that splits each tier string at its **last** underscore reproduces
  the regex exactly. That equivalence is what makes the O(n+m) rewrite in
  `f05-collection-tier-join` safe.
- Cost today: `100 x 775 = 77,500` calls to `String.matches`, each of which compiles a fresh
  `Pattern`, per member, per decode.

## 3. Findings

### f05-collection-tier-join

- **Category:** cross-field-derivation
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:130,
  `SkyBlockMember.java`:145-155, `src/main/java/api/simplified/hypixel/response/skyblock/member/PlayerData.java`:32-33
- **What:** `collectionUnlocked` is materialized in `postInit()` by scanning all 775
  `unlocked_coll_tiers` strings once per collected item with a freshly compiled regex, taking the max
  tier and defaulting to `0`.
- **Why it is residue:** the output is a pure function of two already-bound fields, it is `transient`
  so it is never serialized, and nothing consumes it before `postInit()` would have run - except, by
  accident, `SkillLevel` (see `f05-derivation-ordering`). Eager materialization buys nothing and costs
  correctness.
- **Candidate annotation:** `@Index` / `@Join` in principle; **rejected** in section 5 in favour of a
  lazy memoized getter with an O(n+m) index.
- **Effort:** trivial (consumer only, no library change)

Cost today: 77,500 `Pattern` compilations per member per decode, plus 11 lines of `postInit()` body,
plus a `transient` field, plus a silent dependency on statement order.

The replacement builds the index once. Splitting each tier string at its **last** underscore is
provably equivalent to the per-item regex (see 2.3, including the `LOG` / `LOG_2` case), and drops the
work to a single pass over 775 strings plus 100 map lookups:

```java
/**
 * Highest unlocked collection tier per collected item id, defaulting to zero when the collection
 * has no unlocked tier
 */
public @NotNull ConcurrentMap<String, Integer> getCollectionUnlocked() {
    if (this.collectionUnlocked == null) {
        ConcurrentMap<String, Integer> highestTiers = Concurrent.newMap();

        for (String unlocked : this.getPlayerData().getUnlockedCollectionTiers()) {
            int split = unlocked.lastIndexOf('_');
            if (split < 0) continue;

            String itemId = unlocked.substring(0, split);
            int tier = NumberUtil.tryParseInt(unlocked.substring(split + 1));
            if (tier < 0 || !this.getCollection().containsKey(itemId)) continue;

            highestTiers.merge(itemId, tier, Math::max);
        }

        this.getCollection().forEach((itemId, collected) -> highestTiers.putIfAbsent(itemId, 0));
        this.collectionUnlocked = highestTiers.toUnmodifiableMap();
    }

    return this.collectionUnlocked;
}
```

Payoff: 11 lines of `postInit()` deleted, one of two remaining statements in
`SkyBlockMember.postInit()` removed, 77,500 regex compilations per member removed, and the ordering
defect in `f05-derivation-ordering` fixed as a side effect. Risk: the memo field is written without
synchronization, matching the existing precedent at `HypixelPlayer.java:83-90`; the race is benign
because both racing threads compute the same value, but it should be stated rather than discovered.

### f05-derivation-ordering

- **Category:** correctness
- **Where:** `SkyBlockMember.java`:143 versus `SkyBlockMember.java`:145,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/skill/SkillLevel.java`:24,32-33,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/AccessoryBag.java`:57 versus 138,
  `AccessoryBag.java`:129-136 versus 139-140
- **What:** eagerly derived fields are computed in an order that reads their inputs before those inputs
  are assigned, so three derived values are permanently wrong.
- **Why it is residue:** nothing declares that `skills` depends on `collectionUnlocked`. The dependency
  is expressed only by the vertical order of two statements, and moving either one silently changes
  output. This is the single strongest argument in the pack about derivation.
- **Candidate annotation:** none - the defect is caused by eagerness, and disappears entirely under the
  lazy-getter convention. See section 5.
- **Effort:** trivial (reordering) / small (converting the three sites to lazy getters)

Three concrete defects, all confirmed against source and fixture:

**1. `SkillLevel` FORAGING subtractor is always `2`.** `SkyBlockMember.postInit()` runs
`this.skills = new Skills(..., this)` at line 143, and assigns `this.collectionUnlocked` at line 145.
`Skills`' constructor (`Skills.java:19-24`) is eager - it terminates the stream with `collect`, so
every `SkillLevel` is built at line 143. `SkillLevel`'s constructor computes the `final` field
`levelSubtractor` (`SkillLevel.java:24`), whose FORAGING branch reads
`member.getCollectionUnlocked().getOrDefault("FIG_LOG", 0)` and `"MANGROVE_LOG"`
(`SkillLevel.java:32-33`). At line 143 that map is still the empty initializer from
`SkyBlockMember.java:130`, so both lookups return `0`, both are `< 9`, and `levelSubtractor` is `2`
unconditionally. The fixture's richer member *does* carry both `FIG_LOG` and `MANGROVE_LOG` in
`collection`, so this is live wrong output on the shipped test data, not a hypothetical.

**2. `AccessoryBag.detectedAccessories` is built from an empty bag.** `initialize()` reads
`this.getContents()` at line 57 to enumerate accessories, but `this.contents` is not assigned from the
member's inventory until line 138 - 81 lines later. Every read at line 57 sees the default
`new NbtContent()` from `AccessoryBag.java:34`.

**3. `AccessoryBag.magicalPower` is a dead store.** Lines 129-136 compute `calculatedMagicalPower` into
a **local** and never assign it to the field. Lines 139-140 then derive `tuningPoints` and
`logComponent` from `this.magicalPower`, which is still `0`, so `tuningPoints` is `0` and
`logComponent` is `0.0` for every profile.

Defects 2 and 3 sit inside a method owned by `02-parent-access.md` as a reach-back; only the ordering
and dead-store defects are claimed here. All three share one cause: the value of a derived field
depends on where its assignment happens to sit in an imperative body.

### f05-matcher-group-without-match

- **Category:** correctness
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Bestiary.java`:61-63,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonRun.java`:46,50,54
- **What:** four call sites invoke `Matcher.group(int)` on a matcher on which no match has been
  attempted, which throws `IllegalStateException: No match found` every time.
- **Why it is residue:** `Matcher.group` requires a prior successful `matches()`, `find()` or
  `lookingAt()`. `Bestiary.java:59` does call `matches()` - but on a **different, discarded** matcher
  instance created inside `filterKey`. Line 61 then creates a fresh matcher and reads `group(1)` from
  it immediately.
- **Candidate annotation:** none - keep imperative, fix the call.
- **Effort:** trivial

`Bestiary` is the severe one. The throw happens inside `postInit()`, and
`PostInitTypeAdapterFactory.java:35-38` is:

```java
try {
    ((PostInit) obj).postInit();
} catch (Exception ex) {
}
```

An empty catch with no logging. So `Bestiary.families` is left at its empty initializer
(`Bestiary.java:42`) for every profile ever decoded, and every consumer silently reads zero:
`Bestiary.getUnlocked()` (`Bestiary.java:48-53`) sums an empty list to `0`, `getMilestone()`
(`Bestiary.java:44-46`) returns `0 / 10`, and `ProfileStats.java:68` publishes
`BESTIARY_MILESTONE = 0.0` into the expression-variable map that drives stat calculation. A whole
feature is dark and nothing reports it.

`DungeonRun.Participant` is the loud one. `getClassLevel()`, `getClassType()` and `getName()`
(`DungeonRun.java:46,50,54`) each build a matcher and read a group with no match attempt. These are not
inside `postInit()`, so they throw straight to the caller. Any code path that reads a dungeon run
participant's class or name is dead.

Two independent instances of the same mistake, one silenced by an empty catch and one not, is
sufficient evidence that the empty catch at `PostInitTypeAdapterFactory.java:37-38` should at minimum
log. That is a `small` library change on its own and worth calling out to `20-implementation-plan.md`
independently of any annotation work.

### f05-idtier-key-family

- **Category:** duplication
- **Where:** `SkyBlockMember.java`:150-153, `PlayerData.java`:58-61,
  `Bestiary.java`:29 and 59-63
- **What:** the same `<id>_<number>` key-family scan is hand-written three times, each time with a
  different reducer and a different output shape.
- **Why it is residue:** the *parse* is identical in all three, the *reduction* is not. That asymmetry
  is the whole argument against a narrow `@Tier` annotation.
- **Candidate annotation:** `@Tier` was reserved for exactly this; **rejected** in section 5 as too
  narrow to cover more than one of the three sites.
- **Effort:** small (a shared helper in this module, no library change)

| Site | Source | Parse | Reduce | Output |
| --- | --- | --- | --- | --- |
| `SkyBlockMember.java`:150 | `unlocked_coll_tiers` | `^<itemId>_[\d]+$`, strip prefix | `max`, default `0` | `int` per item |
| `PlayerData.java`:58 | `crafted_generators` | `^<itemId>_[\d]+$`, strip prefix | keep **all**, sorted | `ConcurrentList<Integer>` |
| `Bestiary.java`:59 | `kills` / `deaths` map keys | `^([a-z_]+)_([0-9]+)$` | keep **both** halves | `(id, level)` pair |

A single annotation covering all three would need elements for source path, key pattern, reduction
mode, output shape and default - five knobs on a library annotation to serve three call sites in one
consumer module. That is a bad trade, and section 5 argues it explicitly. What *is* worth sharing is a
package-private static helper in this module that turns a `<id>_<n>` string list into a
`Map<String, List<Integer>>` once; all three sites then reduce that map differently in two lines each.

Note the two `SkyBlockMember`/`PlayerData` sites also share the identical
`String.format("^%s_[\\d]+$", itemId)` unquoted-interpolation and `String.replace` prefix-strip idiom.
`String.replace(CharSequence, CharSequence)` replaces **every** occurrence, not just the leading one;
the fixture contains no id whose `<id>_` prefix repeats inside a tier string, so it does not misfire
today, but `substring` after a `lastIndexOf('_')` is both correct by construction and faster.

### f05-negative-tier-exclusion

- **Category:** correctness
- **Where:** `SkyBlockMember.java`:150
- **What:** the tier regex `^%s_[\\d]+$` does not match a leading minus, so `unlocked_coll_tiers`
  entries such as `MELON_-1` and `SEEDS_-1` are skipped.
- **Why it is residue:** the exclusion is not documented anywhere and reads as an oversight, so it will
  be "fixed" by someone eventually. It should be recorded as deliberate.
- **Candidate annotation:** none - keep imperative, add a comment.
- **Effort:** trivial

**Verdict: the exclusion is an accident that happens to be correct, and including negatives would make
the output worse.** The reasoning, measured against the fixture:

- `_-1` is common and expected - 83 of 775 strings, 11% of the list. It is a normal marker, not
  corruption, so "the API sometimes emits garbage" is not the explanation.
- **No id in the fixture has a negative maximum.** Every id carrying `_-1` also carries `_1`..`_9`. So
  for every id present today, `max` over non-negative tiers and `max` over all tiers agree. The
  exclusion is invisible in current data.
- The only case where the two differ is an id whose *only* entry is `_-1`. Current code yields the
  `orElse(0)` default; including negatives would yield `-1`. Downstream consumers compare against
  positive thresholds - `SkillLevel.java:32-33` tests `< 9`, `Bestiary.getMilestone()` divides by 10 -
  and every one of them is better served by `0` than by `-1`. A `-1` tier means "collection visible,
  no tier claimed", which *is* tier zero.
- Therefore the correct behaviour is exactly what the code does. The defect is that it is undocumented
  and expressed as a regex side effect rather than as an intent.

The rewrite in `f05-collection-tier-join` preserves this deliberately with an explicit
`if (tier < 0) continue`, which states the intent instead of hiding it in a character class. This is
the finding to cite if anyone proposes "widening the regex to `-?[\d]+`" - do not.

### f05-jacobscontest-contest-key

- **Category:** cross-field-derivation
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/JacobsContest.java`:36-39,
  47-63, 108-109
- **What:** `postInit()` walks a `Map<String, Contest>` whose keys encode
  `<year>:<month>_<day>:<collectionId>`, parses each key into a `SkyBlockDate` and a collection name,
  writes both back into the already-bound `Contest` value, and republishes the map as a
  `transient ConcurrentList<Contest>`.
- **Why it is residue:** the key carries three typed values that end up as fields on the value object.
  That is precisely what `@Collapse` + `@Key` exist for - except `@Key` injects the key *whole*, and
  here it must be decomposed first.
- **Candidate annotation:** `@Collapse` + `@Key` with a decomposition step, or `@Split` widened beyond
  `Pair`. Ranked below the anchor - see section 5.
- **Effort:** medium (existing factory change plus a JitPack cycle)

Measured from the fixture: 810 distinct contest keys, all matching
`<year>:<month>_<day>:<collection>`.

The parse is **more subtle than it looks and is currently correct**, which is worth recording because
the obvious annotation design would break it:

- `entry.getKey().split(":")` is unlimited, so `278:1_2:INK_SACK:3` splits into **four** parts, not
  three. Twenty-plus fixture keys are of this shape - every `INK_SACK:3` (brown dye) contest.
- `JacobsContest.java:58` recovers from that with
  `StringUtil.join(dataString, ":", 2, dataString.length)`, rejoining everything from index 2 onward.
  The collection id survives intact as `INK_SACK:3`.
- A naive `@Split(delimiter = ":", parts = 3)` or a `Pair`-shaped `@Split` would truncate the
  collection id to `INK_SACK` for those keys. Any design in `10-annotation-designs.md` must handle
  "split into N parts, last part keeps the remaining delimiters".

Two smaller observations at the same site. `Contest.skyBlockDate` and `Contest.collectionName`
(`JacobsContest.java:108-109`) are non-`transient`, non-`@SerializedName` fields written only by the
enclosing class's `postInit()` - so they participate in serialization as `skyBlockDate` /
`collectionName` keys that Hypixel never sent, which breaks round-trip fidelity. And `contestMap`
(`JacobsContest.java:36-38`) is `@Getter(AccessLevel.NONE)` and exists only to be transformed, which is
the holder-class idiom `03-value-shape-collapse.md` owns.

### f05-dungeons-master-pairing

- **Category:** correctness
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:57-68,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonData.java`:61-75
- **What:** `postInit()` pairs each normal dungeon with its master-mode counterpart by filtering out
  keys starting with `MASTER_` and then re-looking-up `MASTER_<TYPE>` - but the JSON keys are
  **lowercase**, so both the filter and the lookup miss.
- **Why it is residue:** the pairing is a self-join on one map, expressed with two case-sensitive
  string literals against data that is not in that case. Nothing in the type system catches it.
- **Candidate annotation:** `@Capture` affix grouping is the closest existing fit; see section 5 for
  why it is still not a clean win.
- **Effort:** trivial to fix, small to restructure

Fixture ground truth: `dungeon_types` has exactly two keys, `catacombs` and `master_catacombs`. Both
lowercase. Tracing `Dungeons.java:57-68` against that:

1. `filterKey(key -> !key.startsWith("MASTER_"))` at line 58 is case-sensitive. `"master_catacombs"`
   does **not** start with `"MASTER_"`, so it passes the filter instead of being excluded. Both keys
   survive.
2. `mapKey(DungeonData.Type::of)` at line 59 - `Type.of` is case-**insensitive**
   (`DungeonData.java:70-75`) and the enum has only `UNKNOWN` and `CATACOMBS`
   (`DungeonData.java:63-64`). So `catacombs` maps to `CATACOMBS` and `master_catacombs` maps to
   `UNKNOWN`.
3. For the `CATACOMBS` entry, line 63-66 looks up
   `String.format("MASTER_%s", type.name())` = `"MASTER_CATACOMBS"`. The actual key is
   `"master_catacombs"`. The lookup misses and `getOrDefault` returns a fresh empty `FloorData()`.

Three consequences, all live:

- **`DungeonData.masterMode` is always empty.** `getFloorData(true)` (`DungeonData.java:29-31`) returns
  an empty `FloorData` for every profile, so master-mode best runs, best scores and fastest times are
  invisible.
- **`getDungeons()` contains a spurious `Type.UNKNOWN` entry** whose `normalMode` is actually the
  master-mode data. It flows into `Dungeons.getWeight()` (`Dungeons.java:86-94`) as an extra dungeon.
- The extra entry is currently harmless in weight terms only because `master_catacombs` carries no
  `experience` key in the fixture, so its experience is `0` and its weight rounds to zero. That is luck,
  not design - if Hypixel ever emits master experience the total weight silently inflates.

Note this is a **case mismatch, not a missing enum constant**: adding `MASTER_CATACOMBS` to the enum
would not fix it, because the filter at line 58 would still let the lowercase key through. The minimal
fix is to compare case-insensitively in both places.

### f05-repository-derivations

- **Category:** cross-field-derivation
- **Where:** `Bestiary.java`:74-82,93-95,135-137,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/skill/SkillLevel.java`:42-44,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/slayer/SlayerBoss.java`:35,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/pet/OwnedPet.java`:49,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/mining/ForgeItem.java`:25,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/TrophyFish.java`:48,
  `src/main/java/api/simplified/hypixel/response/skyblock/member/attribute/ActiveTrap.java`:35,
  `AccessoryBag.java`:62,149,167,175
- **What:** eleven sites resolve a bound id against a static `SkyBlockData` repository to produce a
  domain model.
- **Why it is residue:** it is not residue, and that is the point of filing it. These derivations
  **cannot** become data annotations, and the pack should say so once, explicitly, so no design
  document tries.
- **Candidate annotation:** none - keep imperative. Explicitly out of reach of `@Index` / `@Join`.
- **Effort:** n/a

`gson-extras` has no dependency on `dev.sbs.skyblockdata` and must not acquire one - it is a general
JSON library consumed by unrelated modules. Any annotation that resolves ids through `SkyBlockData`
would either invert that dependency or require a pluggable resolver registry threaded through
`GsonSettings`, which is `large` effort for zero deletion.

The important observation is the **split in how these eleven sites are written**, because it decides
section 5:

- **Ten of eleven are already lazy getters** with no field, no `transient`, and no `postInit()`
  involvement. `SkillLevel.getSkill()`, `SlayerBoss.getType()`, `OwnedPet.getPet()`,
  `ForgeItem.getItem()`, `TrophyFish.getZone()`, `ActiveTrap.getRegion()`,
  `Bestiary.Family.getType()` and `Bestiary.Mob.getFamily()` are all one-liners of the shape
  `SkyBlockData.getRepository(X.class).findFirstOrNull(X::getId, this.getId())`.
- **One materializes** - `Bestiary.postInit()` at lines 74-82 builds `families` eagerly into a
  `transient` field. That is the single site that diverges from the house pattern, and it is also the
  single site that is silently broken (`f05-matcher-group-without-match`).

The correlation is not a coincidence. Every eager derivation in this package is either wrong
(`Bestiary`, `Dungeons`, `AccessoryBag`, `SkyBlockMember`) or fragile; every lazy one is fine.

### f05-lazy-getter-convention

- **Category:** cross-field-derivation
- **Where:** `HypixelPlayer.java`:44-45,82-91,
  `src/main/java/api/simplified/hypixel/response/skyblock/election/Election.java`:19-20,43-53,
  `Slayers.java`:24-52, `Skills.java`:37-65, `Dungeons.java`:96-130, `Bestiary.java`:44-53
- **What:** the package already contains a working, idiomatic answer to cross-field derivation - a
  plain getter that computes from siblings on demand, memoized into a `transient` field only when the
  cost justifies it.
- **Why it is residue:** the convention exists but is applied inconsistently, and the registry reserved
  `@Aggregate` for a problem this convention has already solved everywhere it appears.
- **Candidate annotation:** none - the convention needs no annotation. `@Aggregate` should be rejected.
- **Effort:** trivial

Evidence that the convention is established and works:

- `HypixelPlayer.getAchievementsOneTime()` (`HypixelPlayer.java:82-91`) is the memoized form:
  `transient` backing field, null check, compute once, return. This is the exact shape
  `f05-collection-tier-join` proposes, already shipping in this package.
- `Slayers` (`Slayers.java:24-52`) is the unmemoized form. `getAverage()`, `getExperience()`,
  `getProgressPercentage()` and `getWeight()` all fold over `bosses`, which `@Collapse` produced at
  bind. No `transient`, no `PostInit`, no ordering hazard. `Slayers` is the model the other classes
  should look like.
- `Skills.java:37-65` and `Dungeons.java:96-130` compute the identical four aggregates lazily over
  their respective collections.
- `Bestiary.getUnlocked()` / `getMilestone()` (`Bestiary.java:44-53`) are lazy folds over `families`.

**`@Aggregate` is unjustified and should be rejected in `10-annotation-designs.md`.** Across the whole
133-file package there is **not one** materialized sum, average, max or count over a sibling
collection. Every aggregate is a lazy getter already. An annotation that materializes aggregates would
have zero adoption sites, would introduce the ordering hazard of `f05-derivation-ordering` where none
currently exists, and would cost a full library cycle to add.

`Election` (`Election.java:19-20,43-53`) is the cleanest single deletion in this survey. Its
`postInit()` derives `voting` and `term` purely from `year` - one already-bound `int`, no JSON, no
repository, no siblings on other objects. Two lazy getters replace the hook outright and remove one of
the six `PostInit` implementors at `trivial` cost. The one wrinkle is `equals`/`hashCode`
(`Election.java:27-41`) reading `getVoting()` / `getTerm()`; since both are pure functions of `year`,
they contribute nothing to identity and can be dropped from both, simplifying the class further. That
detail is flagged here and left for `11-postinit-elimination.md` to sequence.

## 4. Annotation options, head to head

One constraint applies to every option that writes a field, and it is easy to miss: **no existing
factory can see a `transient` field.** `CaptureTypeAdapterFactory.java:115`,
`CollapseTypeAdapterFactory.java:389`, `LenientTypeAdapterFactory.java:434,485` and
`SplitTypeAdapterFactory.java:223` all `continue` on `Modifier.isTransient`. Since every derived field
in this package is `transient` by design (it must not serialize), any annotation-driven option needs a
new phase that deliberately opts *into* transient fields - which is a new ordering guarantee, and by
the effort scale that starts at `large`.

### 4.1 Option A - `@Index` / `@Join` (declarative lookup)

Shape: an annotation on the derived field naming a source path, a key rule and a reduction.

```java
@Index(source = "playerData.unlockedCollectionTiers", restrictTo = "collection",
       key = Index.Key.PREFIX_BEFORE_LAST_UNDERSCORE, reduce = Index.Reduce.MAX, orElse = "0")
private transient @NotNull ConcurrentMap<String, Integer> collectionUnlocked = Concurrent.newMap();
```

Against the evidence:

- It covers `f05-collection-tier-join` and *only* that. `PlayerData.getCraftedMinions` needs
  `reduce = ALL` plus a sort plus a per-call argument rather than a field; `Bestiary` needs both halves
  of the key as a tuple, not a reduction. Five elements to serve one adoption site.
- `restrictTo` is a genuinely odd element - "iterate the keys of field X, look them up in field Y" is a
  set operation the annotation would have to name.
- It bakes a Hypixel key convention (`<id>_<n>`, split at the last underscore) into a general-purpose
  JSON library that has no other consumer wanting it.
- It cannot express `f05-repository-derivations` at all, which is eleven of the derivation sites.
- Effort: `large`. New annotation, new post-bind phase that reads transient fields, new ordering
  guarantee, JitPack cycle, plus a regression pass.

Deleting roughly 11 lines of one consumer class for that is a bad trade. **Rank: low.**

### 4.2 Option B - `@Derive` (named post-bind method)

Shape: `@Derive` on a `transient` field naming the method that computes it, with the factory ordering
the calls.

```java
@Derive("computeCollectionUnlocked")
private transient @NotNull ConcurrentMap<String, Integer> collectionUnlocked = Concurrent.newMap();
```

Against the evidence:

- It *can* express everything, including repository lookups and NBT walks, because the body is
  ordinary Java. That is its one real advantage over Option A.
- But that is also the tell: it is `PostInit` with per-field granularity and reflection-driven method
  dispatch. `PostInit` already exists, already runs after bind, and already lets a class compute
  anything. `@Derive` buys ordering and nothing else.
- Ordering is only needed **because the computation is eager**. Under Option C the ordering problem
  does not exist, so the sole advantage evaporates.
- It replaces a compile-checked method call with a reflected string method name, which is strictly
  worse for refactoring and for IDE navigation.
- Effort: `large`, same reasons as Option A, plus the risk that ordering between *sibling objects* is
  still undefined (`00-conventions.md` §7 notes `PostInit` has no such ordering today), so
  `AccessoryBag` reaching into `member.getRift()` is still unordered.

**Rank: low.** It is a mechanism looking for the problem that Option C dissolves.

### 4.3 Option C - lazy memoized getter, no annotation

Shape: delete the `postInit()` body; make the accessor compute from its inputs at call time, memoizing
into the existing `transient` field only where measurement justifies it.

Against the evidence:

- **It fixes `f05-derivation-ordering` by construction.** A getter reads its inputs when it is called,
  which is always after every field is bound. The FORAGING subtractor defect and the
  `AccessoryBag.contents` defect are both ordering artifacts of eagerness and both vanish. No
  annotation, no factory, no ordering guarantee needed.
- It is already the house convention - ten of eleven repository derivations, every aggregate in
  `Slayers`, `Skills`, `Dungeons` and `Bestiary`, and the memoized
  `HypixelPlayer.getAchievementsOneTime()` (`HypixelPlayer.java:82-91`) are all this shape.
- Zero library change, therefore **zero JitPack cycle** - the only option on this list that can be
  rated `trivial` under the effort scale, and the only one deliverable in a single commit to one
  module.
- It deletes rather than adds: `SkyBlockMember.postInit()` loses 11 lines, `Election` loses its
  `PostInit` implementation entirely, `JacobsContest` and `Dungeons` lose theirs once their parses move
  into the accessor.
- Serialization is unaffected - the fields are `transient`, so nothing was written before and nothing is
  written after. This matters because `00-conventions.md` §4 warns that read-only mechanisms carry a
  round-trip gap; here there is none.

Honest limitations, stated rather than glossed:

- **Memoization on a mutable DTO is not thread-safe.** The null-check-then-assign pattern races. Both
  racers compute the same value from immutable inputs so the race is benign, and
  `HypixelPlayer.java:83-90` already accepts it, but it is a real caveat and should be written down at
  each memo site rather than rediscovered.
- **It cannot solve `AccessoryBag`.** That derivation needs the enclosing `SkyBlockMember`
  (`AccessoryBag.java:135,138,190`), and a getter on a child has no reference to its parent. Option C
  is orthogonal to, not a replacement for, `@Owner` / `@Parent` - which remains
  `02-parent-access.md`'s finding and is genuinely needed.
- It does not reduce **duplication** on its own. `f05-idtier-key-family` still wants a shared helper;
  that helper just lives in this module rather than in `gson-extras`.

**Rank: winner.**

### 4.4 Option D - `@Bind` (ordered post-bind phase)

Shape: the registry's escape hatch - an explicit dependency-ordered derivation engine, so
`collectionUnlocked` can declare that it must run before `skills`.

`f05-derivation-ordering` is the only evidence that could justify this, and it is genuinely strong
evidence: three separate defects in two classes, all caused by undeclared ordering. So the option
deserves a fair hearing rather than a dismissal.

It still loses, for one reason: **it treats the symptom.** Ordering matters only among eager
derivations. Convert those derivations to lazy and the dependency graph is resolved by the call stack
at zero cost, with the JVM rather than a reflected annotation guaranteeing that a value's inputs are
ready before it is read. Option D asks for a new lifecycle phase, a topological sort, cycle detection
and a diagnostic story for cycles - `xlarge` by the effort scale, since it reorders the factory chain -
in order to make eager derivation safe, when not deriving eagerly is free.

**Rank: reject.** Cite this finding if `@Bind` is proposed later; the ordering evidence is real, but it
argues for laziness, not for an engine.

### 4.5 Comparison table

| | A `@Index`/`@Join` | B `@Derive` | C lazy getter | D `@Bind` |
| --- | --- | --- | --- | --- |
| Fixes the three ordering defects | no | partly | **yes, by construction** | yes |
| Expresses repository derivations (11 sites) | no | yes | **yes** | yes |
| Adoption sites in this module | 1 | ~5 | **~15** | ~5 |
| `gson-extras` change | new annotation + new phase | new annotation + new phase | **none** | new phase + chain reorder |
| JitPack cycles | 1+ | 1+ | **0** | 2+ |
| Can see `transient` fields today | no | no | **n/a** | no |
| Round-trip fidelity impact | none | none | **none** | none |
| Effort | large | large | **trivial to small** | xlarge |
| Rank | low | low | **winner** | reject |

## 5. Recommendation

**Cross-field derivation needs no new annotation. It needs the lazy-getter convention applied
consistently, and it needs three live defects fixed.**

The survey went looking for a missing annotation and found instead that the package already contains
the right pattern in fifteen places and the wrong pattern in four - and that all four wrong ones are
producing incorrect output right now. Adding `@Index`, `@Derive`, `@Aggregate` or `@Bind` would make
the wrong pattern more convenient to write.

Recommended disposition for `10-annotation-designs.md`:

| Registry entry | Disposition from this survey |
| --- | --- |
| `@Index` / `@Join` | **Reject.** One adoption site, five elements, `large` effort. See 4.1. |
| `@Tier` | **Reject.** The `<id>_<n>` family has three sites with three different reducers; a shared helper in this module covers them. See `f05-idtier-key-family`. |
| `@Derive` | **Reject.** `PostInit` already does this; the only thing it adds is ordering, which laziness removes. See 4.2. |
| `@Aggregate` | **Reject.** Zero materialized aggregates exist in 133 files. See `f05-lazy-getter-convention`. |
| `@Bind` | **Reject.** Ordering is a symptom of eagerness. See 4.4. |
| `@Owner` / `@Parent` | **Keep** - but on `02-parent-access.md`'s evidence, not this survey's. `AccessoryBag` is the one derivation that laziness cannot reach. |
| `@Collapse` + `@Key` | **Extend, maybe.** `f05-jacobscontest-contest-key` is the only derivation with a real claim on an annotation, and only if key decomposition is added. Rank it below the correctness work. |

Ordered by payoff per unit of effort, what this survey actually asks for:

1. **Fix `f05-matcher-group-without-match`** - `trivial`, consumer only. Four one-line fixes turn the
   whole bestiary feature back on and stop three dungeon-participant accessors from throwing. Highest
   payoff in the survey by a wide margin.
2. **Make `PostInitTypeAdapterFactory.java:37-38` log** - `small`, library, one JitPack cycle. The
   empty catch is why defect 1 survived. Every future `postInit()` failure is invisible until this
   lands.
3. **Fix `f05-dungeons-master-pairing`** - `trivial`. Case-insensitive comparison in two places
   restores master-mode floor data and removes the spurious `Type.UNKNOWN` dungeon.
4. **Convert `SkyBlockMember.collectionUnlocked` to a memoized lazy getter** - `trivial`. Deletes 11
   lines, removes 77,500 regex compilations per member, and fixes the FORAGING subtractor defect as a
   side effect.
5. **Retire `Election`'s `PostInit`** - `trivial`. Two lazy getters, one fewer implementor of the six.
6. **Extract the `<id>_<n>` helper** - `small`, consumer only. Unifies `f05-idtier-key-family`'s three
   sites without touching the library.
7. **Fix `AccessoryBag`'s dead store and read-before-assign** - `trivial` to fix, but coordinate with
   `02-parent-access.md` since the surrounding method is being redesigned there.

Steps 1-5 and 7 touch no library code at all, so they cost **zero JitPack cycles** and can land as one
commit to this module. Step 2 is the only library change this survey asks for, and it is additive
logging rather than a semantic change.

## 6. Derivations that need `SkyBlockData` - not expressible as data annotations

Called out separately because the assignment asked for it explicitly, and because a design document
scanning for adoption sites would otherwise count these.

Eleven sites resolve ids through the static `SkyBlockData` repository (full list in
`f05-repository-derivations`). They fall into three groups:

- **Simple id resolution** - `SkillLevel.getSkill()` (`SkillLevel.java:42-44`),
  `SlayerBoss.getType()` (`SlayerBoss.java:35`), `OwnedPet.getPet()` (`OwnedPet.java:49`),
  `ForgeItem.getItem()` (`ForgeItem.java:25`), `TrophyFish.getZone()` (`TrophyFish.java:48`),
  `ActiveTrap.getRegion()` (`ActiveTrap.java:35`), `Bestiary.Family.getType()` (`Bestiary.java:93-95`).
  One line each, already lazy, already correct. Leave alone.
- **Repository-driven set construction** - `Bestiary.postInit()` (`Bestiary.java:74-82`) iterates the
  entire `BestiaryFamily` repository and builds a `Family` per model, joining the member's parsed mobs
  in. The *shape of the output is defined by the repository*, not by the JSON, which is the strongest
  possible statement that no data annotation can express it. `Bestiary.Mob.getFamily()`
  (`Bestiary.java:135-137`) does the reverse lookup by scanning every family's mob list for
  `<id>_<level>`.
- **Repository plus NBT plus parent** - `AccessoryBag.initialize()` (`AccessoryBag.java:62,149,167,175`)
  walks an NBT compound tag, resolves each item id against the `Accessory` repository, groups by
  family, then multiplies stat values by `Stat.getPowerCoefficient()`. Three inputs, none of them a
  sibling JSON field.

The hard constraint: `gson-extras` does not depend on `dev.sbs.skyblockdata`, and must not. Making any
of these declarative would require either a dependency inversion or a pluggable resolver registry
threaded through `GsonSettings` and every factory - `large` at best, and it would delete no code, since
these bodies are already one-liners or genuinely complex algorithms. **Recommendation: state in
`10-annotation-designs.md` that repository-backed derivation is permanently out of scope for the
annotation set, and stop re-evaluating it.**

The one thing worth normalizing is *when* they run, not *how*: the ten lazy sites are fine, and
`Bestiary`'s eager one should join them.

## 7. Out of scope - handed to sibling surveys

Encountered while surveying, deliberately not claimed here, recorded so nothing is lost:

- **`AccessoryBag.initialize(member)` as a reach-back** (`AccessoryBag.java:55`) - `02-parent-access.md`.
  This survey claims only the ordering defect at line 57 versus 138 and the dead store at lines 129-140.
- **`JacobsContest.contestMap`** (`JacobsContest.java:36-38`), **`Dungeons.dungeonMap` / `classMap`**
  (`Dungeons.java:27-32`) and **`Dungeons.DungeonTreasures`** (`Dungeons.java:140-146`) as holder
  classes with `@Getter(AccessLevel.NONE)` - `03-value-shape-collapse.md`.
- **`Experimentation.Table`'s `transient` + `@Capture` fields** (`Experimentation.java:44-49`) - every
  factory skips transient fields during bind (`CaptureTypeAdapterFactory.java:115`), so these three
  maps look unbindable. Worth verifying in `03-value-shape-collapse.md`; it is a bind-shape question,
  not a derivation one.
- **`Kuudra.SearchSettings.getCombatLevel()`** (`Kuudra.java:46-50`) parses `"0-60"` into a
  `Range<Integer>` and is derived from a single field, not from siblings. It is a `@Split` widening
  candidate for whichever survey owns `@Split`.
- **`CrimsonIsle.postInit()`** (`CrimsonIsle.java:52-56`) pushes two `@SerializedPath` holder fields
  down into `Kuudra`'s transients. That is downward state propagation, which
  `00-conventions.md` §5 assigns to `parent-access`.
- **`Contest.skyBlockDate` / `Contest.collectionName`** (`JacobsContest.java:108-109`) are
  non-`transient` derived fields and therefore serialize keys the API never sent. Noted under
  `f05-jacobscontest-contest-key`; whoever owns round-trip fidelity should confirm the fix is simply
  marking them `transient`.
- **`Dungeons.getClass(...)` / `getDungeon(...)` empty-sentinel accessors** (`Dungeons.java:24-25,78-84`)
  - `04-accessor-boilerplate.md`, as `@Fallback` evidence.
