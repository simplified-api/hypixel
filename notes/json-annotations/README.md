# JSON annotations - what the pack found, and what it proposes

Entry point for `notes/json-annotations/`. Ten documents, 133 DTO files surveyed, sixteen annotation
proposals examined. This file is the summary and the pseudo-code surface; §8 says which document owns
each argument at full length.

## 1. What I found

You commissioned this to find the missing annotations. **There are almost none.** The residue in
`response/` is, in descending order of volume: an annotation that already ships and was never applied,
a stock gson or Lombok feature nobody reached for, a getter that should have been lazy, and - in four
places that matter more than the rest of the pack combined - a live defect that has been silently
producing wrong output for years.

Headline opportunities, priority order, payoff first:

| # | Opportunity | Payoff | Cost | Owner doc |
| --- | --- | --- | --- | --- |
| 1 | **Four features are dark right now.** `Bestiary.families` has never been non-empty; `DungeonData.masterMode` has always been empty; `SkyBlockMember.postInit()` throws on its **first statement** for every member ever decoded; the `FORAGING` level subtractor is unconditionally `2` | 4 features turn back on. All four hid behind one empty `catch (Exception ex) {}` in `PostInitTypeAdapterFactory` | `trivial` x9, one module, no library | `01`, `05`, `20` §4 |
| 2 | **`@SerializedPath` retires the nested-holder idiom** - 9 private holder classes, 13 held fields, 10 forwarders. The annotation shipped years ago and was never pointed at them | 7 classes + 1 whole file deleted, ~108 net lines, 2 unreachable data paths recovered | `small`, **zero library change** | `10` §4, `20` §6 |
| 3 | **All 6 `implements PostInit` retire, and zero new annotations are needed to do it.** Two absorbed by annotations that already ship, one by a corrected field type, three by lazy accessors | 6 implementors, ~90 lines of hook, 77,500 regex compilations per member per decode, 810 eager allocations per member | `trivial` to `medium`, no library | `11` |
| 4 | **792 unmapped JSON keys go to 0** with a 4-line `@Capture` catch-all on `SkyBlockMember.objectives` | `scripts/json_dto_diff.py` turns green and becomes a hard gate | `small` | `20` §7 |
| 5 | **Two new annotations are genuinely worth building** - `@Fallback` (enum sentinel on unrecognised wire values, 24 exposed sites) and `@Flatten` (map-value unwrap, 1 site, rides along) | 24 enum-typed fields stop binding to `null`; 1 map-of-maps collapses | `medium`, **one JitPack cycle, the only one in the pack** | `10` §6-7, `20` §12 |
| 6 | **Nine lines of logging in `gson-extras`** so `postInit()` failures stop being invisible | Every future hook failure becomes visible. `PostInit`'s own javadoc already promises this and the code never did it | `small`, rides cycle #5 | `11` §11.3 |

The single most valuable line in the pack is not an annotation. It is the observation that
**every eager derivation in this package is currently wrong, and every lazy one is fine.** That
correlation is what turns the recommendation from taste into evidence.

## 2. The numbers

| Measure | Before | After |
| --- | --- | --- |
| Registry proposals examined | 16 | **2 accepted**, 14 rejected or declined |
| `implements PostInit` in `response/` | 6 | **0** |
| `implements PostInit` workspace-wide | 7 | **0** (interface survives, unimplemented) |
| JitPack cycles required | - | **1** (`hypixel`), plus 1 optional in `skyblock` |
| `json_dto_diff.py` unmapped keys | 792 | **0** |
| `response/` file count | 133 | ~124 |
| Live defects fixed | - | 4 dark features, 1 auction field, 791 quest statuses |
| Total elapsed | - | **16-24 hours AI-assisted**; 9-13 working days human-developer |

Stages 1-8 touch **one module, zero JitPack cycles**, and are individually revertable. The library
cycle is deliberately **last**, not first - enabling the new logging before the consumer fixes land
would produce one warning per member per request and train everyone to filter it out.

## 3. Before / after gallery

Real code from this repo, before on the left of each pair. This is the surface to judge the pack on.

### 3.1 SkyBlockMember - the whole `postInit()` disappears

The module's largest DTO and the only implementor the design doc expected to survive. It does not.

```java
// BEFORE - SkyBlockMember.java:140-156
@Override
public void postInit() {
    this.accessoryBag.initialize(this);
    this.skills = new Skills(this.getPlayerData().getSkillExperience(), this);

    this.collectionUnlocked = this.getCollection()
        .stream()
        .map((itemId, value) -> Pair.of(itemId, this.getPlayerData()
            .getUnlockedCollectionTiers()
            .stream()
            .filter(tier -> tier.matches(String.format("^%s_[\\d]+$", itemId)))
            .map(tier -> Integer.parseInt(tier.replace(String.format("%s_", itemId), "")))
            .max(Comparator.naturalOrder())
            .orElse(0)
        ))
        .collect(Concurrent.toUnmodifiableMap());
}
```

Three statements. **Statement 1 throws on every decode**, so statements 2 and 3 have never run -
`skills` is `null` and `collectionUnlocked` is empty for every member of every profile. The
exception lands in `PostInitTypeAdapterFactory`'s empty catch and nothing reports it.

```java
// AFTER - no hook, no `implements PostInit`, no PostInit import
@Getter(AccessLevel.NONE)
@SerializedName("accessory_bag_storage")
private @NotNull AccessoryBag accessoryBag = new AccessoryBag();
@Getter(AccessLevel.NONE)
private transient Skills skills;
@Getter(AccessLevel.NONE)
private transient ConcurrentMap<String, Integer> collectionUnlocked;

/**
 * Accessory bag, wired with the member-scoped values it cannot reach from its own node
 */
public @NotNull AccessoryBag getAccessoryBag() {
    return this.accessoryBag.initialize(
        this.getInventory().getBags().getAccessories(),
        this.getRift().getAccess().hasConsumedPrism(),
        this.getCrimsonIsle().getAbiphone().getContacts().size()
    );
}

/**
 * Skill levels derived from the member's skill experience
 */
public @NotNull Skills getSkills() {
    if (this.skills == null)
        this.skills = new Skills(this.getPlayerData().getSkillExperience(), this);

    return this.skills;
}
```

`getCollectionUnlocked()` is §3.2. Net: 15 lines of hook deleted, 3 accessors added, one `implements`
clause removed, **one package import cycle broken**, 77,500 pattern compilations per member removed,
and two defects fixed structurally rather than by reordering statements. The topological sort that
`@Bind` was reserved to declare is performed by the call stack, for free - `getSkills()` calls
`getCollectionUnlocked()`, which reads two bound fields.

### 3.2 `collectionUnlocked` - the join

Plain-English explanation in §4. This is the shape.

```java
// BEFORE - inside postInit(), quoted in full above
// for each of ~100 collected items, scan all 775 tier strings with a fresh regex
this.collectionUnlocked = this.getCollection()
    .stream()
    .map((itemId, value) -> Pair.of(itemId, this.getPlayerData()
        .getUnlockedCollectionTiers()
        .stream()
        .filter(tier -> tier.matches(String.format("^%s_[\\d]+$", itemId)))
        .map(tier -> Integer.parseInt(tier.replace(String.format("%s_", itemId), "")))
        .max(Comparator.naturalOrder())
        .orElse(0)
    ))
    .collect(Concurrent.toUnmodifiableMap());
```

```java
// AFTER - one pass over 775 strings plus 100 lookups, on demand
/**
 * Highest unlocked collection tier per collected item id, defaulting to zero when no tier is
 * unlocked
 */
public @NotNull ConcurrentMap<String, Integer> getCollectionUnlocked() {
    if (this.collectionUnlocked == null) {
        ConcurrentMap<String, Integer> highestTiers = Concurrent.newMap();

        for (String unlocked : this.getPlayerData().getUnlockedCollectionTiers()) {
            int split = unlocked.lastIndexOf('_');
            if (split < 0) continue;

            String itemId = unlocked.substring(0, split);
            int tier = NumberUtil.tryParseInt(unlocked.substring(split + 1));

            // a negative tier marks a visible collection with nothing claimed, which is tier zero
            if (tier < 0 || !this.getCollection().containsKey(itemId)) continue;

            highestTiers.merge(itemId, tier, Math::max);
        }

        this.getCollection().forEach((itemId, collected) -> highestTiers.putIfAbsent(itemId, 0));
        this.collectionUnlocked = highestTiers.toUnmodifiableMap();
    }

    return this.collectionUnlocked;
}
```

Splitting at the **last** underscore is provably equivalent to the per-item regex, including the
adversarial `LOG` / `LOG_2` pair. `100 x 775` becomes `775 + 100`. The negative-tier exclusion that
`[\\d]+` was hiding becomes a line that says what it means.

**No annotation was proposed for this.** `@Join(source = "playerData.unlockedCollectionTiers",
restrictTo = "collection", key = ..., reduce = MAX, orElse = "0")` is five elements for one adoption
site, and it bakes a Hypixel key convention into a general-purpose JSON library.

### 3.3 AccessoryBag - the reach-back

This is where `@Owner` / `@Parent` was supposed to live. It has **one** bound customer in 133 files.

```java
// BEFORE - AccessoryBag.java:5, :55, :135-190
import api.simplified.hypixel.response.skyblock.SkyBlockMember;   // cyclic package import

public void initialize(@NotNull SkyBlockMember member) {
    this.detectedAccessories = this.getContents()      // :57 - reads `contents`...
        .getNbtData()
        ...
    this.contents = member.getInventory().getBags().getAccessories();   // :138 - ...assigned here
    ...
    // reads member.getRift().getAccess().hasConsumedPrism()            at :135
    // reads member.getCrimsonIsle().getAbiphone().getContacts().size() at :190
}
```

110 lines behind one implicit statement order, and the order is wrong: `:57` reads `contents` before
`:138` assigns it, so it parses the default `new NbtContent()` whose `rawData` is `""` and
`NbtFactory.fromBase64("")` throws. That throw is what kills the rest of `SkyBlockMember.postInit()`.

```java
// AFTER - three values in, everything else memoised
/**
 * Supplies the three member-scoped values the accessory bag cannot reach from its own JSON node.
 *
 * @param contents the talisman bag item data, stored under the member's inventory
 * @param consumedPrism whether the rift prism has been consumed
 * @param abiphoneContacts the abiphone contact count, halved by an equipped abicase
 */
public @NotNull AccessoryBag initialize(@NotNull NbtContent contents, boolean consumedPrism, int abiphoneContacts) {
    this.contents = contents;
    this.consumedPrism = consumedPrism;
    this.abiphoneContacts = abiphoneContacts;
    return this;
}

@Getter(AccessLevel.NONE)
private transient ConcurrentList<AccessoryData> detectedAccessories;

/**
 * Accessories parsed out of the talisman bag and resolved against the accessory repository
 */
public @NotNull ConcurrentList<AccessoryData> getDetectedAccessories() {
    if (this.detectedAccessories == null)
        this.detectedAccessories = this.parseDetectedAccessories();

    return this.detectedAccessories;
}
```

Why the three-value handover beats an `@Owner` annotation, and it is not just cost:

| Property | Three values | `@Owner` field |
| --- | --- | --- |
| Read-before-assign at `:57` vs `:138` | **unwritable** - no second statement to get wrong | survives unchanged |
| Dead store at `:129-136` (`magicalPower` computed into a local, never assigned) | **unwritable** - no local to leave it in | survives unchanged |
| `response.skyblock` <-> `response.skyblock.member` import cycle | **broken** | a typed owner field *is* that import |
| Standalone `AccessoryBag` decode in `MemberDtoMappingTest`:111 | works, `initialize` simply never runs | hands every accessor a null owner |
| Bind-time correctness | n/a - runs after bind, from an accessor | **impossible**: `inventory` sits at key index 24 in one profile and 14 in another, so a bind-time reach-back computes different magical power for two members of one account |
| Serialization cycles, `equals` recursion, parent lifetime pinning | none | all three, plus a container rule for `ConcurrentLinkedMap<UUID, SkyBlockMember>` |

Honest cost: `initialize` stays public and must be called. It is now three typed parameters whose
meaning is in the signature instead of a whole member whose relevant parts were invisible.

### 3.4 Rift - the nested-holder idiom

The cheapest high-payoff item in the pack, and it needs nothing that does not already ship.

```java
// BEFORE - response/skyblock/member/rift/Rift.java:23-25, :44-54
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
```

```java
// AFTER
@SerializedPath("wither_cage.killed_eyes")
private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();
```

`getKilledEyes()` survives, generated by the class-level `@Getter`. `Rift` drops from 56 lines to 42.

The multi-field case is the only place `@Inline` would have had anything to say, and it is three
lines:

```java
// AFTER - SkyBlockMember.Profile, 3 declarations replace 1 holder class and 3 forwarders
@SerializedPath("profile.first_join")
private SkyBlockDate.RealTime firstJoin;
@SerializedPath("profile.personal_bank_upgrade")
private int personalBankUpgrade;
@Accessors(fluent = true)
@SerializedPath("profile.cookie_buff_active")
private boolean isBoosterCookieActive;
```

Three fields sharing one prefix land in one `profile` object on write - the factory *reuses* an
existing nested object rather than overwriting it. That is the whole of what `@Inline` would have
had to implement, and it was already implemented.

Package-wide: **9 classes removed** (8 nested plus `Temples.java`), 13 held fields relocated, 10
forwarders deleted, **2 dead data paths recovered** (`shards.traps.active_traps` - an eleven-field
`ActiveTrap` list parsed on every profile fetch and then unreachable - and two `bestiary.miscellaneous`
fields). ~108 net lines, ~130 with the `HypixelPlayer` pair. **Zero library change, zero JitPack
cycle, zero re-pin.**

Two of the nine are worse than verbose: `AttributeShards`:12 and `Bestiary`:37 keep the class-level
`@Getter`, so Lombok emits a public accessor returning a `private static` nested class - a compile
error at every external call site, verified by javac reproduction.

### 3.5 SkyBlockIsland.getProfileStats - the one that does not change

Included because it is the most tempting `@Owner` site in the codebase and adopting it would be a
**performance regression bought with an annotation**.

```java
// BEFORE - SkyBlockIsland.java:76-82
public @NotNull ProfileStats getProfileStats(@NotNull SkyBlockMember member) {
    return this.getProfileStats(member, true);
}

public @NotNull ProfileStats getProfileStats(@NotNull SkyBlockMember member, boolean calculateBonus) {
    return new ProfileStats(this, member, calculateBonus);
}
```

```java
// AFTER
// (identical - recommend no action)
```

The reasoning, because "no change" is a finding and not an omission:

| Fact | Consequence |
| --- | --- |
| `ProfileStats` lives in `hypixel/profile_stats`, not `response/`, declares no `@SerializedName` field and **never passes through Gson** | there is nothing for a bind-time annotation to attach to |
| Grepping `skyBlockIsland` across all 637 lines returns 3 hits, and only one reads data: `banking.balance` at `:69` | the entire grandparent dependency is **one `double`** |
| `calculateBonus` exists precisely so callers can skip `:143-210` | making it an `@Owner`-fed transient of `SkyBlockMember` would run that branch for **every member of every profile on every decode**, to delete one constructor parameter |

`Skills` is the same pattern and gets the same verdict - passing an object into the constructor of a
non-bound helper is not residue.

### 3.6 Three more, in brief

**`JacobsContest`** - `@Collapse` + `@Key` both ship today and do the whole transform. 16-line hook,
`contestMap`, its suppression and the transient `contests` all go:

```java
// AFTER
@Collapse
@SerializedName("contests")
private @NotNull ConcurrentList<Contest> contests = Concurrent.newList();

// ... and on Contest
@Key
private transient @NotNull String id = "";
```

810 eager `SkyBlockDate` allocations per member become zero, one malformed key stops emptying all 810,
and two phantom serialized keys (`skyBlockDate`, `collectionName`, written from *outside* the class)
disappear.

**`Dungeons.classes`** - six lines of hook exist because the field was declared one level too deep.
The JSON `{"healer": {"experience": 84271835.04}, ...}` already *is* a `DungeonClass`:

```java
// BEFORE
private @NotNull ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>> classMap = ...;
// plus a transient `classes` and a 6-line rebuild in postInit()

// AFTER
@SerializedName("player_classes")
private @NotNull ConcurrentMap<DungeonClass.Type, DungeonClass> classes = Concurrent.newMap();
```

**`CrimsonIsle`** - the cheapest scalp. Hypixel puts `kuudra_party_finder` **beside**
`kuudra_completed_tiers`; the Java decided `Kuudra` owns both and pushed two objects down a level to
make it true. Let the Java say what the JSON says:

```java
// AFTER - two snake-case staging fields and two Kuudra transients deleted
@SerializedPath("kuudra_party_finder.search_settings")
private @NotNull Kuudra.SearchSettings partyFinderSearch = new Kuudra.SearchSettings();
@SerializedPath("kuudra_party_finder.group_builder")
private @NotNull Kuudra.GroupBuilder partyFinderGroupBuilder = new Kuudra.GroupBuilder();
```

Round-trip fidelity **improves** - both fields stay bound and non-`transient`, where the old `Kuudra`
transients were never serialized at all.

## 4. What `collectionUnlocked` actually does

**One sentence: for every item the member has ever collected, it is the highest collection tier they
have unlocked for that item - or zero if they have unlocked none.**

It is a **join between two unrelated JSON nodes** that happen to be keyed by the same item id in two
different spellings. That is why it is hard to remember: neither node alone tells you anything.

Two inputs:

| Input | JSON | Shape | Example |
| --- | --- | --- | --- |
| `collection` | `members[uuid].collection` | `itemId -> total amount collected` | `{"WHEAT": 4812901, "INK_SACK:3": 12034, "MELON": 88}` - ~100 entries |
| `unlockedCollectionTiers` | `members[uuid].player_data.unlocked_coll_tiers` | a **flat list of 775 strings**, each `<itemId>_<tier>` | `["INK_SACK:3_9", "METAL_HEART_2", "MELON_-1", "WHEAT_1", "WHEAT_2", ...]` |

The second one is the confusing half. It is not a map and it is not grouped - it is one long flat
list where a fully-completed item contributes nine separate strings (`WHEAT_1` through `WHEAT_9`).
The tier is the **suffix after the last underscore**, and the item id can itself contain underscores
and colons (`METAL_HEART`, `INK_SACK:3`).

The pseudo-code, in the direction the new implementation reads:

```
highestTiers = {}

for each string in unlocked_coll_tiers:                # 775 of them
    split at the LAST underscore  ->  itemId, tier     # "METAL_HEART_2" -> "METAL_HEART", 2
    skip if tier < 0                                   # "MELON_-1" means visible but nothing claimed
    skip if itemId is not in `collection`              # never collected it, so it does not appear
    highestTiers[itemId] = max(highestTiers[itemId], tier)

for each itemId in collection:                         # ~100 of them
    highestTiers.putIfAbsent(itemId, 0)                # collected, but no tier claimed yet

return highestTiers                                    # itemId -> highest unlocked tier, 0..9
```

Three things worth knowing, because each one looks like a bug and only one is:

- **The `collection` *values* are thrown away.** The map is used purely as the set of item ids to
  keep. `getCollection()` is what you want for amounts; `getCollectionUnlocked()` is what you want
  for tiers.
- **Negative tiers are excluded on purpose.** 83 of the 775 strings end `_-1`. No id's maximum tier
  is negative - every id carrying `_-1` also carries `_1`..`_9` - and every downstream consumer
  compares against positive thresholds, so `0` serves them better than `-1`. A `-1` tier means
  "collection visible, nothing claimed", which *is* tier zero. **Do not widen the regex to `-?[\d]+`.**
- **It has been empty for every profile ever decoded**, because `postInit()` throws two statements
  earlier. Its two consumers (`SkillLevel.java`:32-33 and `SkyBlockIsland.java`:55-58) have been
  reading an empty map the whole time.

## 5. The proposed annotation set

Sixteen proposals in, **two out**. Both ride the same single JitPack cycle, along with a nine-line
correctness fix to an existing factory.

### 5.1 Accepted - build these

| Name | Purpose | Replaces | Sites | Library change | Effort |
| --- | --- | --- | --- | --- | --- |
| `@Fallback` | Marks the enum constant a case-insensitive enum adapter falls back to when the wire value is unrecognised | 24 exposed fields that bind to `null` today, plus 7 `@Capture` maps that silently drop the entry | ~12 enum edits, 1 line each | `CaseInsensitiveEnumTypeAdapterFactory` edit + 1 annotation file | `medium` |
| `@Flatten("key")` | Unwraps a single-valued JSON object on the **value side of a map entry** into the scalar the caller actually wants, and re-wraps it on write | `Currencies.essence`'s `Map<String, Map<String, Integer>>` funnel and its 5-line stream accessor | 1 (rides the cycle) | 1 annotation file + 1 new factory + `GsonSettings` registration | `small` |
| *(not an annotation)* `PostInitTypeAdapterFactory` logging | Makes the empty `catch (Exception ex) {}` log, and null-guards `obj` | Four features that have been silently dark for years | 9 lines, additive | existing factory, matches its own javadoc | `small` |

Usage, both of them, in full:

```java
// @Fallback - one edit repairs three sites at once
public enum Type {

    @Fallback
    UNKNOWN,
    HEALER,
    MAGE,
    BERSERK,
    ARCHER,
    TANK

}
```

```java
// @Flatten - Currencies.java, before
@Getter(AccessLevel.NONE)
private @NotNull ConcurrentMap<String, ConcurrentMap<String, Integer>> essence = Concurrent.newMap();

public @NotNull ConcurrentMap<String, Integer> getEssence() {
    return this.essence.stream()
        .mapValue(value -> value.get("current"))
        .collect(Concurrent.toMap());
}

// after
@Flatten("current")
private @NotNull ConcurrentMap<String, Integer> essence = Concurrent.newMap();
```

`@Fallback` was narrowed during design: the registry line said "supplies a default when the key is
absent **or** the value fails to bind". The absent-key half is already served by Java field
initializers, and a field-level form **cannot reach a map key**, which would have left all seven
`@Capture` sites broken. What ships is an opt-in enum-constant marker.

### 5.2 Annotations that already ship and were never applied

This table is bigger than the one above, and it costs nothing.

| Annotation | Sites | Payoff |
| --- | --- | --- |
| `@SerializedPath` | 9 census holders / 11 classes unioned, 13 fields | ~108 net lines, 9 classes deleted, 2 dead data paths recovered |
| `@Lenient` + `@Extract` | 3 full, 1 partial | 29 rift statistics typed, 1 hand-written filter and its memo deleted |
| `@Capture` | 3 | 5 fields to 1 and a 10-line switch deleted; **792 unmapped keys to 0**; 1 `PostInit` implementor retired |
| `@Collapse` + `@Key` | 2 | 16 lines and 1 `PostInit` implementor |
| `@Split` | 1 | hand-rolled range parse deleted, a `throws` becomes `empty()` |
| `@SerializedName` | 3 | every auction's starting bid stops binding to `0`; 791 quest statuses stop binding to `null` |
| stock `@Getter` | 3 | 13 bound fields become readable for 3 added lines |

## 6. What was rejected, and why

Fourteen of sixteen. This section exists so none of them gets relitigated, and so the ones that were
close calls are visible as close calls.

### 6.1 The single-field collapse tradeoff you raised

You raised it against `HeartOfTheForest.BiomeWhispers.Tier` - a five-line class holding one
`int spent`, so `ConcurrentMap<Integer, Tier>` could be `ConcurrentMap<Integer, Integer>`. Your
objection: **collapsing is brittle, because if Hypixel adds a second key the class absorbs it and the
scalar cannot.**

The verdict is that you are half right, and the half that is wrong changes where the line gets drawn.

**A wrapper does not absorb a new JSON key on its own.** Gson drops any key no field declares,
wrapper or not. What the wrapper actually buys is that *adopting* the new key is a one-line edit
inside the wrapper (`private int refunded;`) that no caller sees, whereas adopting it after a collapse
means changing the field's declared type back, which every caller sees. The wrapper is not insurance
against the API changing - it is insurance against the **source-compatibility cost of reacting** to
the API changing.

Which makes it a **visibility** question, not a shape question. The cost of un-collapsing is
proportional to how many callers can see the collapsed type:

```java
// HeartOfTheForest.java:51-55 - the funnel already exists
public int getSpent(int tier) {
    return Optional.ofNullable(this.getTiers().get(tier))
        .map(Tier::getSpent)
        .orElse(0);
}
```

Every caller already goes through `getSpent(int)`. The only leak is that the class-level `@Getter`
also publishes `getTiers()`, which exports `Tier` to everyone. **Suppress that one accessor and the
internal representation is free to flip in either direction at any time, for one line.**

So the recommendation splits three ways:

| Site | Verdict | Because |
| --- | --- | --- |
| `BiomeWhispers.Tier` | **do not collapse.** Add `@Getter(AccessLevel.NONE)` on `tiers` and point `getSpent(int)` at `this.tiers` | the fixture shows this key family mid-growth - `desert` gained a `total` key between two profiles of one account. Your objection is live here, and the two-line change makes the shape freely reversible, which is what the collapse was trying to buy |
| `Currencies.essence` | **collapse** (`@Flatten("current")`) | the accessor already discards every sibling key. The wrapper has **zero absorptive capacity** - a new `total` key would be silently dropped by an accessor nobody would think to change |
| `Dungeons.classMap` | **collapse** (retype, no annotation) | same - `postInit()` already reduces each value to `.get("experience")` |

The rule that falls out: **collapse where the code already throws the wrapper away; keep the wrapper
where something still reads it, and suppress the accessor that leaks it.** Four more sites were
examined and all four kept their wrappers - see §6.2.

### 6.2 Everything else, in one table

| Proposal | Verdict | Reason | Effort avoided |
| --- | --- | --- | --- |
| `@Inline` | **reject** | `@SerializedPath` covers all 9 census holders at zero library cost. The largest holder in the package has **3** fields and six of nine have **1** - the entire saving is two repetitions of the string `"profile."` | `small`+ |
| `@Delegate` | **reject** | Stock `lombok.experimental.Delegate` already ships it and **still loses**: deletes the forwarder but keeps the holder, the field and the nesting (40 of 135 lines), and cannot rename, which `VillagePlaza.getSeraphineStepIndex()` requires | `small`+ |
| `@Alias` | **reject** | `@SerializedName(value = ..., alternate = {...})` is stock gson, already honoured on fields, on enum constants, and by `@Capture`'s key arbitration | `small`+ |
| `@Owner` / `@Parent` | **decline** | **One** bound customer (§3.3). Three values copied down beat a reference handed up, and also break the package import cycle that a typed owner field *is* | `large` |
| ancestor-relative `@SerializedPath` | **decline** | `^` counts adapter frames, the author counts objects, and a path string cannot check the difference. Its one site needed a rename, not a mechanism | `large` |
| `@Derive` | **reject** | It is `PostInit` with a reflected method name. Its only addition is ordering between derived fields, and ordering is a symptom of eagerness - the call stack sorts the graph for free once both sides are lazy | `large` |
| `@Index` / `@Join` | **reject** both | Two different operations with **one adoption site each**, and 11 of the lookups need a `SkyBlockData` dependency that `gson-extras` must never acquire | `large` x2 |
| `@Tier` | **reject**, kept as a documented alias | Three sites share the *parse* and **none share the reduction** - max, all-sorted, and both-halves. A package-private helper in this module covers them | `large` |
| `@Aggregate` | **reject** | The row's premise is false: across 133 files there is **not one** materialized aggregate | `large` |
| `@Bind` | **reject** | `xlarge` - it reorders the factory chain. The ordering evidence behind it is the strongest in the pack and it argues for **laziness**, not for an engine | `xlarge` |
| `@Capture` unmatched-key element | **decline** | Subsumed by `@Fallback` through the enum adapter, with no change to the busiest factory in the library. It would also break round-trip fidelity | `medium` |
| `@Capture` value-grouping element | **decline** | One site, eight lines, against a change to grouping selection shared by twelve files | `medium` |
| `@Lenient` typed-overflow element | **decline** | One site, and the free partial types half of it today at zero cost | `medium` |
| class-level `@Flatten` | **decline** | Technically sound, immunises against a wrap/unwrap flip **the fixture shows nowhere**, removes no code, and gives one name two unrelated meanings | - |
| a field-level `@Fallback` | **cut from the accepted design** | Cannot reach a map key, so all seven `@Capture` sites would stay broken | - |
| deleting the `PostInit` interface | **do not** | Not primarily compatibility. `JpaRepository.java`:255-256 calls it manually before an upsert and the entities use **field access**, so a persisted derived column genuinely cannot be lazy. Zero implementors makes it an extension point, not a dependency | `xlarge` |

Consumer-side changes that look right and are not, in one line each:

- **Do not collapse `CrystalHollows.MinesOfDivan` or `LostPrecursorCity`** - `{}` in every fixture
  member, but their two siblings in the same parent carry two keys each. Single-valued only because
  this account has not progressed them.
- **Do not collapse `WinterIsland`** - Jerry's Workshop is seasonal and the fixture was captured
  outside the event window. Revisit with a December capture.
- **Do not collapse `EdelisQuest`** - it sits in a family of ten `*Quest` classes; collapsing the one
  that currently has a single field makes it the only quest addressed by path instead of by type.
- **Do not merge the seven rift location classes** - across all seven there is **not one shared
  serialized name**. A base class holding the union would advertise every location's fields on every
  other location.
- **Do not merge `SkillLevel.getWeight()` or `SlayerBoss.getWeight()`** - they share the shape but not
  the arithmetic. One uses a repository-supplied exponent, the other runs an iterative loop.
- **Do not demote any `Optional<T>` field** - Hypixel emits **explicit nulls**: across 88 fixture
  pets, `skin` is present 88/88 with 70 nulls.
- **Do not normalise `@NoArgsConstructor` usage** - four conventions across 208 class declarations and
  **all four behave identically**. 100% cosmetic.

### 6.3 Two "fixes" that would be regressions

- **Do not widen the collection-tier regex to `-?[\d]+`.** Reasoning in §4. `s20-derivation-retirements`
  states it as `if (tier < 0) continue;` precisely so nobody "fixes" it again.
- **Do not add `MASTER_CATACOMBS` to `DungeonData.Type`.** It is the first diagnosis everyone reaches
  for and it fixes nothing - the case-sensitive filter at `Dungeons.java`:58 still lets the lowercase
  `master_catacombs` key through, so the spurious `UNKNOWN` dungeon survives. This is a case mismatch,
  not a missing constant.

One more that looks like a defect and is not: `CrimsonIsle.Quests.kuudraBossDaily` carries
`@SerializedName("kuuda_boss_daily")` and that annotation is **correct** - the key really is
misspelled upstream and the fixture contains `kuuda_boss_daily`. It looks exactly like the
`starting_big` defect (which *is* real) and is not one.

## 7. The plan, at a glance

Ten stages, ranked by payoff per unit of effort, cheapest first - which is also the execution order.
Full detail, per-stage verification and rollback in `20-implementation-plan.md`.

| # | Stage | Effort | Payoff | Cycle | Blocked by | AI-assisted |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `s20-dark-feature-fixes` | `trivial` x9 | **4 dark features turned back on**, 791 quest statuses stop binding to `null`, 13 fields become readable | no | none | 45-75 min |
| 2 | `s20-free-retirements` | `trivial` x2 | 2 `PostInit` implementors (`Election`, `CrimsonIsle`), 6 fields, 1 encapsulation leak | no | none | 30-45 min |
| 3 | `s20-holder-collapse` | `small` | **7 classes + 1 file deleted, ~108 net lines**, 10 forwarders, 2 unreachable data paths | no | none | 1.5-2.5 h |
| 4 | `s20-objectives-catchall` | `small` | **792 unmapped keys to 0** - the coverage gate turns green | no | 1 | 1-1.5 h |
| 5 | `s20-existing-annotation-sweep` | `small` | 4 classes, ~55 lines, 29 `Object` fields typed, 5 fields to 1 | no | none | 2-3 h |
| 6 | `s20-shape-retirements` | `small` x2 | 2 `PostInit` implementors (`JacobsContest`, `Dungeons`), 810 eager allocations | no | none | 1.5-2.5 h |
| 7 | `s20-derivation-retirements` | `medium` | **the last 2 `PostInit` implementors**, 1 package import cycle, 77,500 regex compilations per member | no | 1 | 3-5 h |
| 8 | `s20-duplication-sweep` | `medium` | 2 files, ~200 lines, 23 duplicated weight lines | no | 6, 7 | 2-3 h |
| 9 | `s20-library-cycle` | `medium` | 24 enum sites stop binding to `null`; every future `postInit()` failure becomes visible | **yes, 1** | 1, 7 | 2-3 h |
| 10 | `s20-skyblock-election` | `small` | the 7th `PostInit` implementor, in another repo | **yes, 1** | none | 45-60 min |

**Total: 16-24 hours AI-assisted elapsed, against 9-13 working days human-developer.** The ratio is
widest on stages 3 and 5 (mechanical, thirteen near-identical edits) and narrowest on stage 7 (the
only stage with genuine design judgement). Stage 9 is bounded by JitPack, not by either party - do
not schedule it as a filler task expecting it to finish in a gap.

`implements PostInit` count: 6 -> 4 (stage 2) -> 2 (stage 6) -> **0** (stage 7) -> 0 workspace-wide
(stage 10).

**The library cycle is last on purpose.** Enabling the new logging before the consumer fixes land
produces one warning per member per request and trains everyone to filter it out.

**The one irreversible thing in the plan is not code.** Stage 7 changes *when* exceptions surface for
every consumer of these DTOs - from swallowed at decode to thrown at the caller. That is the correct
trade and it is why `Bestiary` stayed dark for years, but a consumer that has been silently tolerating
an empty collection will start seeing a stack trace. Release note, not code comment.

Two verification gates run at every stage boundary: `MemberDtoMappingTest` (293 lines, 16 tests) and
`python scripts/json_dto_diff.py`, which is a **red gate today** at 792 unmapped keys and becomes a
hard failure gate after stage 4.

## 8. Where to dig

Read in this order if you are reading all of it. Read `20` alone if you just want to start.

| File | What it owns | Read it when |
| --- | --- | --- |
| `00-conventions.md` | The spine - doc map, finding-id scheme, effort scale, the nine categories, the naming registry every proposal draws from, house style, shared ground facts | you want the vocabulary, or you are adding a document |
| `01-postinit.md` | Survey of all 6 `PostInit` implementors, each body quoted and classified, backed by an **executed probe** that decoded the fixture and printed the post-bind state | you doubt that three of the six never run |
| `02-parent-access.md` | Survey of reach-back - upward, sideways and downward push. Proves bind-order instability from fixture **key order** across two profiles | you are tempted by `@Owner` again |
| `03-value-shape-collapse.md` | Survey of shape mismatch - holder classes, single-field wrappers, map-of-map funnels. **§2 is your collapse tradeoff, answered at length** | you want the full collapse argument, or the inventory of all 20 single-field classes |
| `04-accessor-boilerplate.md` | Survey of no-logic accessors, and the 9-holder / 13-field / 10-forwarder census. Includes the javac reproduction of the private-return-type compile error | you are doing stage 3 and want the per-site list |
| `05-cross-field-derivation.md` | Survey of fields computed from other fields - joins, max-of-matching-key scans, 11 repository lookups. Owns the `collectionUnlocked` decode and the negative-tier finding | you want the derivation evidence, or the tier-exclusion proof |
| `06-structural-duplication.md` | Survey of repeated shape, dead Java, and the latent defects found while inventorying. Owns the 792-unmapped-key measurement | you want the coverage numbers or the `@SerializedName` misses |
| `10-annotation-designs.md` | **The design document.** All 16 proposals, each with signature, factory work, semantics, failure modes, before/after, and what would reopen it. §21 is the whole-document disposition table | you want to know *why* a proposal lost, in full |
| `11-postinit-elimination.md` | The `PostInit` end-state, per implementor, as real Java. Also: whether the interface itself should be deleted (no - `JpaRepository` field access), and the blast radius across 5 consuming modules | you are doing stages 2, 6 or 7 |
| `20-implementation-plan.md` | **The only file that sequences work.** Ten stages, files touched, verification, rollback and estimate per stage; the JitPack cadence; §17 "do not do"; §18 open risks | you are about to start |

## 9. Open risks

Everything the pack asserts but has not executed. **None of these blocks stage 1.** Full list with
settlement instructions in `20-implementation-plan.md` §18.

| Question | Gates | How to settle |
| --- | --- | --- |
| Does `@SerializedPath` re-nest **three** fields sharing one prefix into one `profile` object on write? The factory was read closely and never run | stage 3 | one round-trip test - it is a **prerequisite of the stage**, not a nicety |
| Does a filtered `@Capture` re-prefix its keys on write? | stage 5 | assert against the shipping `Kuudra.java`:18 user, not the new one |
| Does `@Capture(descend = true)` with an **empty** filter work? | the optional follow-up form of stage 6 only | one test against `gson-extras`. **Keep it off the critical path** - the lazy form retires the implementor without it |
| Does `DungeonClass` bind through `UnsafeAllocator` with a `final` field and no no-arg constructor? | stage 6 | one decode assertion; safer form is to drop `final` and add `@NoArgsConstructor(access = PRIVATE)` |
| Is `CommissionData.totalCompleted`'s upstream key really `total_completed`? The endpoint is not in the fixture | held out of stage 1 | resolve against a live `/skyblock/garden` response. **Do not guess it into the source** |

**Two claims rest on a single fixture** and would need a second capture to strengthen: that `temples`,
`winter_player_data` and `events` really are single-key sub-objects, and that no collection id has a
negative maximum tier. Both are correct for the 2 profiles and 2 members available; neither is
provable from them.

**One latent library issue, recorded not scheduled.** `SerializedPathTypeAdaptorFactory`:124 reads
`innerJsonObject.isJsonArray()` where it means `innerJsonElement.isJsonArray()`, so the branch is
unreachable. The effect is that an empty JSON array at the end of a path binds as an empty collection
instead of being skipped - which is the desirable outcome, so nothing is broken. Noted so stage 3's
expanded use of that factory is not blamed for it later.
