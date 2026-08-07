# 11 - PostInit elimination

## 1. The question

`PostInit` was the precursor. Every annotation in `dev.simplified.gson.annotation` peeled one category
of post-deserialization work off the hook and made it declarative:

| Annotation | Peeled off `postInit()` | Shape of the work |
| --- | --- | --- |
| `@SerializedName` | "read this key into that field" | key rename |
| `@SerializedPath` | "walk down to a nested value and assign it" | static descent |
| `@Capture` | "collect the keys nobody declared, and group them" | dynamic keys, affix joins |
| `@Collapse` + `@Key` | "turn this keyed object into a list and tell each value its key" | key injection |
| `@Lenient` + `@Extract` | "the map has one entry of the wrong type - deal with it" | type-incompatible entries |
| `@Split` | "this one string is two values" | scalar decomposition |

Six categories, six annotations, and each time the residue in `postInit()` got smaller. This document
finishes the sequence or proves it cannot be finished. One question:

**Can `PostInit` be rendered obsolete, and exactly how?**

"Obsolete" is deliberately ambiguous and the ambiguity has to be resolved before the question can be
answered, because the two readings have different answers:

- **Obsolete as a usage in `api/simplified/hypixel/response`** - can all six `implements PostInit`
  declarations go?
- **Obsolete as an interface in `dev.simplified.gson`** - can `PostInit` and
  `PostInitTypeAdapterFactory` be deleted from the library?

`01-postinit.md` §8 answered the first and declined to answer the second. `10-annotation-designs.md`
§19.4 answered the first differently - five of six retire, `SkyBlockMember` "survives, as one
statement". This document is the place where that last statement gets attacked properly, and where the
interface question gets a real answer instead of a deferral.

## 2. The answer, up front

**Yes for the usage. No for the interface, and the reason is not backwards compatibility.**

Stated precisely, in the order the evidence supports:

1. **All six `implements PostInit` declarations in `response/` can go, and the seventh in
   `Simplified-Api/skyblock` can go with them.** That is one more than `10-annotation-designs.md`
   §19.4 concluded. The extra scalp is `SkyBlockMember`, and §5 shows the surviving reach-back
   statement moving out of a hook and into `getAccessoryBag()`, where it is legal by construction
   rather than by the accident that `postInit()` happens to fire bottom-up.
2. **Not one of the six needs a new annotation.** Two are absorbed by annotations that ship today and
   were never applied (`@Capture`, `@Collapse` + `@Key`). One is absorbed by a corrected field type
   and no annotation at all. Three are absorbed by the lazy-accessor convention this package already
   uses in roughly fifteen places. `10-annotation-designs.md` §21 rejected or declined all sixteen
   registry proposals and nothing here reopens one.
3. **The residue is imperative code, not an imperative hook.** After the six conversions there are
   still ~110 lines of NBT walking and family de-duplication in `AccessoryBag`, eleven repository
   lookups, and three key parsers. All of it stays hand-written Java. None of it needs to run at a
   defined time, which is the only thing `PostInit` was ever supplying.
4. **The interface stays, with zero implementors, for one reason that is not compatibility.**
   `JpaRepository.java`:255-256 invokes `postInit()` manually on every entity before an upsert, and
   the `dev.sbs.skyblockdata.model` entities use **field access** (`@Id` and `@Column` sit on fields).
   Hibernate reads fields, not accessors, so a persisted derived column genuinely cannot be lazy. No
   such entity exists today - the branch is dead - but it is the one coherent use of the contract, and
   deleting the interface would remove it. §11.4 argues this properly, including the case for deleting
   it anyway.
5. **The one library change is the one `10-annotation-designs.md` §19.5 already accepted** - make
   `PostInitTypeAdapterFactory`'s empty catch log, and null-guard `obj`. This document adds a javadoc
   rewrite to the same commit, because after the migration the interface's own documentation is the
   only thing standing between the next author and a seventh implementor.

The framing in §1 turns out to be almost right and wrong in an interesting way. Each annotation did
peel a category off the hook - but the last category is not a seventh annotation. It is **laziness**,
which is not a library feature at all. `PostInit`'s remaining job was never "compute this"; accessors
compute. It was "compute this *now*", and no site in 133 files needs *now*.

## 3. Entry format

Design entries in §14 use the shape `00-conventions.md` §3 fixes for this document, with the
vocabularies from §4 (effort) and §5 (category) of that file:

```
### d11-slug
- **Retires:** the class whose `implements PostInit` this removes, or "none"
- **Absorbed by:** the annotation or idiom that takes the work over
- **Category:** <one of the nine category slugs>
- **Answers findings:** f0N-slug, ...
- **Library change:** none | additive file | existing factory edit
- **Blocks:** what must land first, or "none"
- **Effort:** trivial | small | medium | large | xlarge
```

Two conventions that apply to every Java snippet below. They are already in `00-conventions.md` §8 and
are repeated because a reader will paste from here. Braces are omitted on single-line bodies;
`getFirst()`/`getLast()` replace indexed access on sequenced collections; javadoc uses single hyphens,
field docs are fragments with no trailing period, and no snippet cites a finding id, a section number
or a filename from this pack.

## 4. The six implementors, and what absorbs each

The table the pack was commissioned for. One row per implementor, one column per question, and the
residual column is the honest one - it is what is still hand-written Java after the conversion.

| Implementor | What `postInit()` does | Absorbed by | Library change | Residual |
| --- | --- | --- | --- | --- |
| `skyblock/SkyBlockMember` (`:141-156`, 15 lines) | (1) hands `AccessoryBag` the member; (2) builds `Skills` eagerly; (3) joins `collection` against `player_data.unlocked_coll_tiers` taking the max tier | (1) a **wire-on-access `getAccessoryBag()`** - no annotation; (2) memoised `getSkills()`; (3) memoised `getCollectionUnlocked()` with a one-pass index | none | 3 values handed over in an accessor; the `<id>_<n>` parse |
| `member/dungeon/Dungeons` (`:55-76`, 20 lines) | (1) rebuilds `classMap` into `classes` by pulling `["experience"]` out of each value map; (2) self-joins `catacombs` with `master_catacombs` into `DungeonData` | (1) a **corrected field type** - the JSON already *is* a `DungeonClass`; (2) memoised `getDungeons()` now, `@Capture` affix grouping after a spike | none | the normal/master pairing, 8 lines in an accessor |
| `member/JacobsContest` (`:46-63`, 16 lines) | converts a keyed `Map<String, Contest>` into a list and writes `skyBlockDate` and `collectionName` onto each value from the map key | **`@Collapse` + `@Key`** (both already ship) plus two lazy accessors on `Contest` | none | the `<year>:<month>_<day>:<collection>` parse, 6 lines on `Contest` |
| `member/Bestiary` (`:55-83`, 27 lines) | zips `kills` and `deaths` into `Mob`, then groups them under every `BestiaryFamily` row from the JPA repository | a **memoised `getFamilies()`** - repository access moves to the point of use | none | the whole computation, unchanged in size, plus the `MOB_PATTERN` and `Matcher.group` fixes |
| `member/crimson/CrimsonIsle` (`:52-56`, 2 lines) | copies two `@SerializedPath` staging fields down into `Kuudra`'s package-private transients | **nothing - delete it.** The JSON says the party finder is a sibling of the completion tiers; the Java should too | none | none. This one leaves no residue at all |
| `skyblock/election/Election` (`:43-53`, 9 lines) | derives two `Cycle` value objects by arithmetic on `year` | **two computed accessors** | none | none, once `equals`/`hashCode` drop the derived fields |

Read the "Library change" column as the result: **six retirements, zero library changes.** The only
`gson-extras` edit this document asks for (§11.3) fixes the hook's exception handling for the benefit
of the *other* modules, not for anything in `response/`.

Three observations the table makes visible that the per-class sections then argue.

**Half the work was never a computation.** `Dungeons.classes` and `JacobsContest.contests` are shape
mismatches: the JSON already carries the value in a layout the field could have declared, and the hook
existed to bridge a declaration that was written wrong. `CrimsonIsle` is a third, in the same
direction - a Java shape decision the JSON never asked for. Three of six implementors exist because a
field declaration is one annotation short.

**The other half is eagerness.** `Bestiary`, `Election` and two of `SkyBlockMember`'s three statements
compute values that are pure functions of already-bound state. The hook adds nothing except that the
value exists before anyone asks for it - and that eagerness is what produces the defects the surveys
found, because a value computed too early reads inputs that are not there yet.

**The residual column is never an annotation gap.** Every entry in it is either a string parse, a
repository lookup or an NBT walk. `10-annotation-designs.md` §16.3 put repository-backed derivation
permanently out of scope and §15.3 showed the composite-key parse is shorter in Java than in
annotation elements. Nothing in the residual column is waiting for a design.

## 5. SkyBlockMember, line by line

The module's largest DTO, and the only implementor `10-annotation-designs.md` §19.4 expected to
survive. `SkyBlockMember.java`:140-156, whole body:

```java
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

Three statements. Two of them never execute.

### 5.1 Statement 1 - `accessoryBag.initialize(this)`

**What it does today.** Hands the child its parent. `AccessoryBag.initialize(SkyBlockMember)`
(`AccessoryBag.java`:55-164) then reads exactly three things through that reference -
`member.getInventory().getBags().getAccessories()` at `:138`,
`member.getRift().getAccess().hasConsumedPrism()` at `:135`, and
`member.getCrimsonIsle().getAbiphone().getContacts().size()` at `:190`, reached by threading the
member through the private `handleMagicalPower` at `:182`. Everything else in the 110 lines is local.

**What it actually does today.** It throws. Line 57 calls `this.getContents().getNbtData()` before
line 138 assigns `contents` from the member, so it parses the default `new NbtContent()` whose
`rawData` is `""`; `NbtFactory.fromBase64("")` throws `NbtException`. That escapes `initialize`,
escapes `postInit()`, and lands in `PostInitTypeAdapterFactory`'s empty catch. **For every member of
every profile, statements 2 and 3 below never run.** `skills` stays `null` and `collectionUnlocked`
stays empty - the observed probe values in `01-postinit.md` §1.

**Why it cannot be an annotation.** The three values are not siblings of `AccessoryBag`'s own JSON
node, they are siblings of its *parent's* node, and `f02-postinit-bottom-up-order` proves nothing can
read a parent's sibling during bind: across two profiles of one account `inventory` sits at key index
24 and 14, `rift` at 0 and absent, `nether_island_player_data` at 16 and 9. A bind-time reach-back
would compute different magical power for two members of the same account.
`10-annotation-designs.md` §13 declined `@Owner`/`@Parent` on exactly this evidence.

**Where it goes.** `10-annotation-designs.md` §13.3 narrows the handover from a reference to three
values and leaves the call inside `postInit()`. That is one step short. The call does not need a hook
at all - it needs a frame that runs after the member is bound, and **the member's own accessor is such
a frame**. Suppress Lombok's generated `getAccessoryBag()` and hand-write it:

```java
@Getter(AccessLevel.NONE)
@SerializedName("accessory_bag_storage")
private @NotNull AccessoryBag accessoryBag = new AccessoryBag();

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
```

`initialize` becomes three field assignments returning `this`, and every derived value on the bag
becomes a memoised accessor over those three stores plus the bag's own bound fields - the shape
`10-annotation-designs.md` §13.3 already specifies for `getDetectedAccessories()`.

Four properties follow, and together they are why this finishes the job rather than merely relocating
it.

- **The read-before-assign becomes unwritable.** `getDetectedAccessories()` reads `this.contents`, and
  `contents` was assigned by `initialize` before any accessor could be called. There is no ordering to
  get wrong because there is no second statement.
- **The dead store has nowhere to hide.** `magicalPower` is currently computed into a local at
  `:129-136` and never assigned, so `tuningPoints` and `logComponent` derive from `0`. As
  `getMagicalPower()` there is no local to leave the value in.
- **The cyclic package import goes.** `AccessoryBag.java`:5 imports `SkyBlockMember` today, so
  `response.skyblock` and `response.skyblock.member` import each other. A typed owner field *is* that
  import; three scalar parameters are not.
- **Standalone decode is unaffected.** `MemberDtoMappingTest.java`:111 decodes `AccessoryBag` straight
  from the `accessory_bag_storage` sub-object. `initialize` is simply never called and the accessors
  return empty - which is what the test already asserts.

### 5.2 Statement 2 - `new Skills(skillExperience, this)`

**What it does today.** Builds every `SkillLevel` eagerly, passing the member so
`SkillLevel.calcLevelSubtractor` (`SkillLevel.java`:27-40) can read
`member.getJacobsContest().getFarmingLevelCap()` for `FARMING` and
`member.getCollectionUnlocked().getOrDefault("FIG_LOG", 0)` / `"MANGROVE_LOG"` for `FORAGING`.

**Why it is wrong even when it runs.** `collectionUnlocked` is assigned by statement 3, two statements
later. `Skills`' constructor terminates its stream with `collect` (`Skills.java`:20-23), so every
`SkillLevel` is built at line 143 and every `FORAGING` lookup reads the empty initialiser from
`SkyBlockMember.java`:130. Both `getOrDefault` calls return `0`, both are `< 9`, and the subtractor is
unconditionally `2`. The fixture hides it - `FIG_LOG_7` and `MANGROVE_LOG_7` are both under 9, so the
right answer is also `2` - but any account that has finished either collection gets a level that is
wrong by up to 2, and it propagates into `Skills.getAverage()` and into `ProfileStats.java`:66's
`SKILL_AVERAGE`.

**Why no annotation reaches it.** `Skills` has no serialized fields, no no-arg constructor and never
passes through Gson (`Skills.java`:15-24). There is nothing for a bind-time annotation to attach to.
The dependency it encodes - a derived value depending on another derived value on the same object - is
what `@Bind` was reserved for, and `10-annotation-designs.md` §19 rejected it: ordering is a property
of eagerness, and the call stack sorts the graph for free once both sides are lazy.

**Where it goes.** A memoised accessor. `getSkills()` calls `getCollectionUnlocked()`, which computes
from two bound fields, so the topological sort happens at the call site with a real stack trace if it
ever cycles.

### 5.3 Statement 3 - the collection tier join

**What it does today.** For each of the ~100 entries of `collection`, scans all 775 strings of
`player_data.unlocked_coll_tiers` for `^<itemId>_[\d]+$`, parses the tier, takes the max, defaults to
`0`. The value side of `collection` - the amount collected - is discarded; the map is used purely as
the set of ids to iterate.

**What it costs.** `String.format` and `String.matches` are both inside the inner filter, and
`matches` compiles a fresh `Pattern` every call: **77,500 format calls and 77,500 pattern compilations
per member per decode**, for a map read at two sites (`SkillLevel.java`:32-33) and one aggregate
(`SkyBlockIsland.java`:55-58).

**Why no annotation reaches it.** `10-annotation-designs.md` §16.1 priced it exactly: `@Join(source =
"playerData.unlockedCollectionTiers", restrictTo = "collection", key = ..., reduce = MAX, orElse =
"0")` is five elements for one adoption site, and it bakes a Hypixel key convention into a
general-purpose JSON library. Three semantics an annotation would have to carry and the Java says in
one line each - the join direction, the discarded value side, and "absent means zero" - are
enumerated in §16.2.

**Where it goes.** The memoised one-pass index in `f05-collection-tier-join`. Splitting each tier
string at its **last** underscore is provably equivalent to the per-item regex, including the
adversarial `LOG` / `LOG_2` pair, and it turns 100 x 775 into one pass over 775 plus 100 lookups. The
negative-tier exclusion stays and becomes visible: `if (tier < 0) continue;` states what
`[\\d]+` was hiding, and `f05-negative-tier-exclusion` proves the exclusion is correct.

### 5.4 The end state, as real Java

`postInit()`, `implements PostInit` and the `dev.simplified.gson.PostInit` import all go. What
replaces them, in place:

```java
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

Both memo fields lose their `@NotNull` eager initialisers and become plain nullable transients, which
is what makes the null check the memo test. `Comparator`, `Pair` and `PostInit` drop out of the import
list; `NumberUtil` comes in.

Net for this file: 15 lines of hook deleted, 3 accessors added, one `implements` clause removed, one
import cycle between two packages broken, 77,500 pattern compilations per member removed, and two
defects - the aborted hook and the always-`2` foraging subtractor - fixed structurally rather than by
reordering statements.

### 5.5 What the wire-on-access accessor costs

Stated adversarially, because §5.1 is the one move in this document that goes beyond what
`10-annotation-designs.md` accepted, and it should not be taken on enthusiasm.

- **`initialize` runs on every call, not once.** Three field assignments plus three getter chains,
  each of which is plain field access on bound objects. It is cheap, it is idempotent by construction
  (assignment, not accumulation), and it removes the "must be called exactly once" hazard that
  `f02-accessorybag-upstream` complains about. If a profiler ever objects, the guard is a boolean
  field, not a redesign.
- **`initialize` stays public.** `AccessoryBag` is in `response.skyblock.member` and `SkyBlockMember`
  in `response.skyblock`, so package-private is not available. It is still a method a consumer could
  call with wrong arguments - but it now takes three typed values whose meaning is in the signature,
  rather than a whole member whose relevant parts are invisible.
- **The memo race is real and benign.** `getSkills()` and `getCollectionUnlocked()` use the
  unsynchronised null-check-then-assign pattern. Two racing threads compute equal values from
  immutable inputs, and `HypixelPlayer.java`:82-91 already ships exactly this. It should be written
  down at each site, not rediscovered.
- **Exceptions move from decode to call.** A broken derivation is currently swallowed and presents as
  an empty collection; lazily it throws at the caller. That is the correct trade - it is how the
  `Bestiary` matcher defect would have been caught years ago - but it is a behaviour change for every
  consumer of these DTOs and belongs in the release note.
- **A decoded object is no longer fully computed.** Today (in principle) a `SkyBlockMember` is
  finished when `fromJson` returns. Afterwards it finishes on demand. Nothing in this module or in
  `SkyBlock-Simplified` depends on the stronger property, and §11.2 is where the one place that could
  is examined.

## 6. Dungeons

Two unrelated computations sharing one body, `Dungeons.java`:55-76. They are separated here because
they have different answers and one of them is not a derivation at all.

### 6.1 `classes` - delete it, do not derive it

```java
this.classes = this.classMap.stream()
    .map(entry -> Pair.of(
        entry.getKey(),
        new DungeonClass(entry.getValue().get("experience"))
    ))
    .collect(Concurrent.toUnmodifiableMap());
```

`classMap` is declared `ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>>`
(`Dungeons.java`:30-32) and the JSON is:

```json
{"healer": {"experience": 84271835.04}, "mage": {"experience": 409047204.36},
 "berserk": {"experience": 92858814.02}, "archer": {"experience": 98301741.50},
 "tank": {"experience": 96565524.56}}
```

`DungeonClass` (`DungeonClass.java`:17-19) declares exactly one field, `private final double
experience`. **The JSON already is a `DungeonClass`.** The map-of-maps funnel, the
`@Getter(AccessLevel.NONE)`, the transient output and six lines of hook exist because the field was
declared one level too deep. Retyping it binds directly:

```java
@SerializedName("player_classes")
private @NotNull ConcurrentMap<DungeonClass.Type, DungeonClass> classes = Concurrent.newMap();
```

No annotation. `CaseInsensitiveEnumTypeAdapterFactory` already maps `healer` to `HEALER` - the probe
in `01-postinit.md` §1 confirms the enum keys resolve correctly today - and Gson sets the `final`
field reflectively, allocating through `UnsafeAllocator` because `@RequiredArgsConstructor` leaves no
no-arg constructor. That allocation path is the same one `DungeonData` already relies on, so it is not
new behaviour; it is worth one assertion in the mapping test rather than an assumption.

`10-annotation-designs.md` §6.1 reaches the same conclusion from the `@Flatten` side and declines to
spend a library cycle on it: `@Flatten("experience")` onto `ConcurrentMap<Type, Double>` gets to the
same deletion and charges a JitPack round trip for it. Take the free one.

### 6.2 `dungeons` - the self-join, and the case defect inside it

```java
this.dungeons = this.dungeonMap.stream()
    .filterKey(key -> !key.startsWith("MASTER_"))
    .mapKey(DungeonData.Type::of)
    .map((type, value) -> Pair.of(type, new DungeonData(
        value.getExperience(),
        value,
        this.dungeonMap.getOrDefault(String.format("MASTER_%s", type.name()), new FloorData())
    )))
    .collect(Concurrent.toUnmodifiableMap());
```

This is the only genuinely structural operation in the file: pair each floor set with its master-mode
counterpart. It is also broken in both halves, and the two halves fail in opposite directions.

Fixture ground truth: `dungeon_types` has exactly two keys, `catacombs` and `master_catacombs`, both
**lowercase**.

1. `filterKey(key -> !key.startsWith("MASTER_"))` is case-sensitive, so `master_catacombs` is not
   excluded. It survives, `DungeonData.Type.of` maps it case-insensitively to `UNKNOWN`
   (`DungeonData.java`:70-75, and the enum has only `UNKNOWN` and `CATACOMBS`), and it appears in
   `getDungeons()` as a spurious dungeon that `getWeight()` at `:86-94` folds into the member's total.
2. `String.format("MASTER_%s", type.name())` asks for `MASTER_CATACOMBS`, misses `master_catacombs`,
   and substitutes `new FloorData()`. **`DungeonData.masterMode` is empty for every profile ever
   decoded**, against a fixture where master catacombs has `highest_tier_completed = 7`.

Two things worth stating because they are the first two diagnoses anyone reaches for and both are
wrong. Adding `MASTER_CATACOMBS` to the enum fixes nothing - the filter at `:58` still lets the
lowercase key through. And an annotation would not have helped either: `@Capture(filter = "^master_")`
is a case-sensitive regex too, and an `@Index`-style affix element would have carried the same string
literal one level further from the data.

The fix is to normalise the key space once, so the filter and the lookup share one spelling by
construction rather than by two authors agreeing.

### 6.3 The end state, as real Java

```java
@Getter
public class Dungeons {

    private static final @NotNull DungeonClass EMPTY_CLASS = new DungeonClass(0);
    private static final @NotNull DungeonData EMPTY_DUNGEON = new DungeonData(new FloorData(), new FloorData());

    @SerializedName("dungeon_types")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, FloorData> dungeonMap = Concurrent.newMap();
    @SerializedName("player_classes")
    private @NotNull ConcurrentMap<DungeonClass.Type, DungeonClass> classes = Concurrent.newMap();

    // ... unchanged fields ...

    @Getter(AccessLevel.NONE)
    private transient ConcurrentMap<DungeonData.Type, DungeonData> dungeons;

    /**
     * Dungeons keyed by type, each pairing its normal-mode floors with its master-mode floors
     */
    public @NotNull ConcurrentMap<DungeonData.Type, DungeonData> getDungeons() {
        if (this.dungeons == null) {
            ConcurrentMap<String, FloorData> floors = this.dungeonMap.stream()
                .mapKey(String::toLowerCase)
                .collect(Concurrent.toMap());

            this.dungeons = floors.stream()
                .filterKey(key -> !key.startsWith("master_"))
                .map((key, floorData) -> Pair.of(DungeonData.Type.of(key), new DungeonData(
                    floorData,
                    floors.getOrDefault(String.format("master_%s", key), new FloorData())
                )))
                .collect(Concurrent.toUnmodifiableMap());
        }

        return this.dungeons;
    }

}
```

`implements PostInit`, the 20-line body, `classMap`, its `@Getter(AccessLevel.NONE)` and the transient
`classes` all go. `classes` becomes a plain bound field with a generated getter, so `getClassAverage()`,
`getClassExperience()`, `getClassProgressPercentage()` and `getClassWeight()` (`:96-130`) keep working
unchanged - they already route through `getClasses()`.

One deliberate change beyond the hook: `DungeonData`'s `experience` field is dropped and its
constructor narrows to `(normalMode, masterMode)`. The hook passed `value.getExperience()` as the
first argument and `value` as the second, so the field was always
`getNormalMode().getExperience()`; making that an accessor removes a field that could disagree with
its own source. `EMPTY_DUNGEON` (`:25`) follows the constructor. `DungeonData.getExperience()` is
required by the `Experience` interface, so it becomes:

```java
/** {@inheritDoc} */
@Override
public double getExperience() {
    return this.getNormalMode().getExperience();
}
```

### 6.4 The `@Capture` variant, and the spike that gates it

`f01-dungeons-capture-grouping` proposes retiring the pairing entirely rather than making it lazy,
using `@Capture` affix grouping - the mechanism `CaptureTypeAdapterFactory.java`:432-455 implements
and `Capture.java`:37-47 documents:

```java
@Capture(descend = true)
@SerializedName("dungeon_types")
private @NotNull ConcurrentMap<DungeonData.Type, DungeonData> dungeons = Concurrent.newMap();
```

with `DungeonData` carrying `@SerializedName("") FloorData normalMode` and
`@SerializedName("master_") FloorData masterMode`. `10-annotation-designs.md` §15.4 explicitly hands
this document the choice between the two routes. **The choice is: ship §6.3 now, and treat the
`@Capture` form as a follow-up gated on a spike.** Three reasons, in order of weight.

1. **The combination is not exercised anywhere.** `Capture.java`:96-116 documents `descend = true`
   only alongside a non-empty `filter`, and an empty filter is documented as a catch-all limited to
   one per class. Whether "descend into a named object and catch-all inside it" is supported, and what
   the known-key set means inside a descended object, is unverified. That is a test against
   `gson-extras`, not a library change - but it is a test that has not been run.
2. **`DungeonData` has to lose `experience` first either way.** Affix grouping splits keys against the
   value class's field names, and a plain field named `experience` is treated as the auto-suffix
   `_experience`, so it would look for `catacombs_experience` and find nothing. §6.3 already makes
   that change, which means the `@Capture` route is strictly downstream of it.
3. **Both routes are zero library cost, so there is no cycle to save by choosing early.** Deferring
   costs nothing; guessing wrong costs a debugging session against a factory whose grouping rules are
   subtle enough that `Capture.java`:118-127 needed a `Grouping.ENTRY` escape hatch added to them.

If the spike passes, the payoff over §6.3 is real: `dungeonMap` and its suppression disappear, the
transient memo disappears, the pairing becomes a declaration, and the case defect becomes structurally
unreachable because the factory does the affix matching instead of two string literals. If it fails,
the fallback in `f01-dungeons-capture-grouping` - a `^(master_)?`-style filter - is uglier than §6.3
and should be dropped rather than adopted.

## 7. JacobsContest

### 7.1 `@Collapse` + `@Key` does the whole transform

`JacobsContest.java`:46-63:

```java
this.contests = this.contestMap.stream()
    .map(entry -> {
        Contest contest = entry.getValue();

        String[] dataString = entry.getKey().split(":");
        String[] calendarString = dataString[1].split("_");
        int year = NumberUtil.toInt(dataString[0]);
        int month = NumberUtil.toInt(calendarString[0]);
        int day = NumberUtil.toInt(calendarString[1]);

        contest.collectionName = StringUtil.join(dataString, ":", 2, dataString.length);
        contest.skyBlockDate = new SkyBlockDate(year, month, day);
        return contest;
    })
    .collect(Concurrent.toUnmodifiableList());
```

Three operations stacked: turn a keyed map into a list, hand each value its own key, and parse that
key into two typed values. The first two are the documented job of `@Collapse` + `@Key`
(`Collapse.java`:37-42), which ship today and are already in production use on `Slayers`. The third is
a string parse that belongs on the object that owns the string.

The parse is subtler than it looks and the subtlety is the reason no annotation should claim it.
`entry.getKey().split(":")` is unlimited, so `278:1_2:INK_SACK:3` splits into **four** parts, not
three, and line 58 recovers by rejoining everything from index 2 onward. Twenty-plus of the fixture's
810 contest keys are that shape - every brown-dye contest. A `parts = 3` splitter, or a `Pair`-shaped
`@Split` (`Split.java`:14-19 splits into exactly two), truncates the collection id to `INK_SACK` and
passes every test written against the other 790 keys. `10-annotation-designs.md` §15.3 makes this the
passage that kills key-decomposition elements on `@Split` and `@Key`.

Two smaller defects ride along and both are fixed by the same change. `Contest.skyBlockDate` and
`Contest.collectionName` (`:108-109`) are non-`transient` fields written from *outside* the class, so
they serialize as `skyBlockDate` and `collectionName` keys that Hypixel never sent - a round-trip
break. As accessors they stop being fields at all.

### 7.2 The end state, as real Java

```java
@Collapse
@SerializedName("contests")
private @NotNull ConcurrentList<Contest> contests = Concurrent.newList();
```

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public static class Contest {

    @Key
    private transient @NotNull String id = "";
    private int collected;
    @Accessors(fluent = true)
    @SerializedName("claimed_rewards")
    private boolean hasClaimedRewards;
    @SerializedName("claimed_position")
    private int position;
    @SerializedName("claimed_participants")
    private int participants;

    @Getter(AccessLevel.NONE)
    @SerializedName("claimed_medal")
    private @NotNull Optional<Medal> claimedMedal = Optional.empty();

    /**
     * In-game date the contest was held, parsed from the contest id
     */
    public @NotNull SkyBlockDate getSkyBlockDate() {
        String[] parts = StringUtil.split(this.getId(), ":");
        String[] calendar = StringUtil.split(parts[1], "_");

        return new SkyBlockDate(
            NumberUtil.toInt(parts[0]),
            NumberUtil.toInt(calendar[0]),
            NumberUtil.toInt(calendar[1])
        );
    }

    /**
     * Collection id the contest was held for, preserving colons embedded in the id
     */
    public @NotNull String getCollectionName() {
        String[] parts = StringUtil.split(this.getId(), ":");

        return StringUtil.join(parts, ":", 2, parts.length);
    }

}
```

`implements PostInit`, the 16-line body, `contestMap`, its `@Getter(AccessLevel.NONE)` and the
transient `contests` all go. `@Key` on a `transient` field is the shipping pattern - `SlayerBoss.id`
is exactly this and `Slayers` is the working user.

Three payoffs beyond the retirement, all of which matter more than the line count.

- **810 eager `SkyBlockDate` allocations per member parse become zero.** The probe decoded 810
  contests for one member; the only external consumer
  (`SkyBlock-Simplified/bot/.../SkyBlockUserCommand.java`:691) filters down to a handful of crops.
- **One malformed key stops emptying all 810.** `dataString[1]` and `calendarString[1]` are unguarded
  index reads inside a stream, so today one bad key throws, the empty catch eats it, and `contests`
  is empty for the whole member. Per-`Contest` parsing confines the failure to the contest that
  caused it - and surfaces it, rather than silently returning an empty list.
- **Cross-object field mutation goes.** Lines 58-59 currently assign into another class's instance
  from outside it; `@Key` injection is the factory's job and both fields become read-only.

The parse is still hand-written and still unguarded against a key with fewer than three segments. That
is a deliberate hold: `getSkyBlockDate()` throwing `ArrayIndexOutOfBoundsException` on a malformed id
is a visible failure at one contest, which is strictly better than an invisible failure at all 810.
Guard it if a malformed key is ever observed; do not guard it speculatively and swallow the result.

## 8. Bestiary

### 8.1 Two joins, one repository, one broken matcher

`Bestiary.java`:55-83 does two things:

```java
ConcurrentList<Mob> mobs = PairStream.of(Stream.concat(this.kills.stream(), this.deaths.stream()))
    .distinct()
    .filterKey(key -> MOB_PATTERN.matcher(key).matches())
    .collapseToSingle((key, value) -> {
        Matcher matcher = MOB_PATTERN.matcher(key);
        String id = matcher.group(1).toUpperCase();
        int level = NumberUtil.tryParseInt(matcher.group(2));

        return new Mob(id, level, this.kills.getOrDefault(key, 0), this.deaths.getOrDefault(key, 0));
    })
    .collect(Concurrent.toUnmodifiableList());

this.families = SkyBlockData.getRepository(BestiaryFamily.class)
    .stream()
    .map(family -> new Family(family.getId(), mobs.stream()
        .filter(mob -> mob.getFamily().equals(family))
        .collect(Concurrent.toUnmodifiableList())))
    .collect(Concurrent.toUnmodifiableList());
```

**It has never produced a non-empty result.** Line 61 builds a fresh `Matcher` and calls `group(1)`
on it without a prior `matches()`, `find()` or `lookingAt()` - the `matches()` at line 59 ran on a
*different, discarded* matcher inside `filterKey`. `Matcher.group` throws `IllegalStateException: No
match found`, the empty catch eats it, and `families` stays at its empty initialiser for every profile
ever decoded. Downstream, `getUnlocked()` sums an empty list to `0`, `getMilestone()` returns `0 / 10`,
and `ProfileStats.java`:68 publishes `BESTIARY_MILESTONE = 0.0` into the expression-variable map. A
whole feature is dark and nothing reports it. The probe's `bestiary families = 0` is against a member
whose `kills` map holds 1023 entries.

The second half is a repository join, and it is the reason this class cannot stay eager even if the
matcher is fixed. `SkyBlockData.getRepository` (`SkyBlockData.java`:50-52) delegates to a static
session manager; with no live JPA session it throws, the same empty catch eats it, and `families` is
permanently empty - **a session coming up later changes nothing**, because the assignment already
happened. The module's own test suite documents the workaround: `MemberDtoMappingTest.java`:42-45
decodes subtrees individually because "a whole `SkyBlockMember` runs `postInit` against the SkyBlock
model repositories, which need a live JPA session this test deliberately does not stand up."

`10-annotation-designs.md` §16.3 rules the repository half permanently out of annotation scope, and
the decisive reason is not the dependency direction - it is that **the output shape is defined by the
repository, not by the document**. `families` has one entry per `BestiaryFamily` row regardless of
what the JSON contains. No annotation over JSON can express that.

### 8.2 The end state, as real Java

```java
private static final @NotNull Pattern MOB_PATTERN = Pattern.compile("^(.*)_([0-9]+)$");

@Getter(AccessLevel.NONE)
private transient ConcurrentList<Family> families;

/**
 * Bestiary families, each carrying the member's kills and deaths for its mobs
 */
public @NotNull ConcurrentList<Family> getFamilies() {
    if (this.families == null) {
        ConcurrentList<Mob> mobs = PairStream.of(Stream.concat(this.kills.stream(), this.deaths.stream()))
            .distinct()
            .mapKeyValue((key, value) -> Pair.of(MOB_PATTERN.matcher(key), key))
            .filterKey(Matcher::matches)
            .collapseToSingle((matcher, key) -> new Mob(
                matcher.group(1).toUpperCase(),
                NumberUtil.tryParseInt(matcher.group(2)),
                this.kills.getOrDefault(key, 0),
                this.deaths.getOrDefault(key, 0)
            ))
            .collect(Concurrent.toUnmodifiableList());

        this.families = SkyBlockData.getRepository(BestiaryFamily.class)
            .stream()
            .map(family -> new Family(family.getId(), mobs.stream()
                .filter(mob -> mob.getFamily().equals(family))
                .collect(Concurrent.toUnmodifiableList())))
            .collect(Concurrent.toUnmodifiableList());
    }

    return this.families;
}
```

Two corrections are folded in and both must land in the same change, because verifying the conversion
requires the computation to actually produce something.

- **One matcher, matched once.** The stream carries the `Matcher` rather than rebuilding it, so
  `group(1)` is only ever reached on a matcher whose `matches()` returned `true`. The exact stream
  spelling depends on what `PairStream` offers; the invariant is what matters - never call `group` on
  a matcher you have not matched.
- **`MOB_PATTERN` widens from `^([a-z_]+)_([0-9]+)$` to `^(.*)_([0-9]+)$`.** `[a-z_]+` cannot match a
  digit, so every mob id containing one is silently dropped. The fixture's `deaths` map carries
  `master_crypt_undead_flameboy101_45` and `master_crypt_undead_flameboy101_25` - real keys, excluded
  from every family today. A greedy base with a trailing numeric group reproduces the intended split
  and admits them.

Memoisation is not optional here, unlike elsewhere. `Family.getType()`, `getTiers()`, `getBracket()`
and `getMaxTier()` (`:92-121`) each re-query the repository, and `getUnlocked()` / `getMilestone()`
walk every family, so an unmemoised `getFamilies()` would be markedly slower than today's broken
version.

Payoff: 27 lines of hook deleted, `implements PostInit` gone, one transient becomes an accessor,
`Bestiary` becomes decodable in a plain unit test with no JPA session, and the repository dependency
moves to the point of use - where a missing session is a recoverable, observable error instead of a
permanently empty list.

## 9. CrimsonIsle

### 9.1 The downward push is a naming decision

`CrimsonIsle.java`:52-56, the whole body:

```java
this.kuudra.searchSettings = this.kuudra_search_settings;
this.kuudra.groupBuilder = this.kuudra_group_builder;
```

Not a derivation - a downward field copy, the mirror image of a reach-back. Hypixel puts
`kuudra_party_finder` next to `kuudra_completed_tiers` as a sibling key. The Java decided `Kuudra`
owns both, and then had to move two objects one level down to make that true. The bill:

- 2 staging fields with snake-case Java identifiers, `kuudra_search_settings` and
  `kuudra_group_builder` (`:38-43`), a `naming` defect in their own right
- 2 `@Getter(AccessLevel.NONE)` suppressions on them
- 2 package-private `transient` fields on `Kuudra` (`Kuudra.java`:22-23) that only compile because the
  classes share a package
- the entire reason `CrimsonIsle` implements `PostInit`

And nothing reads the result. A workspace-wide symbol search finds no caller of
`Kuudra.getSearchSettings()` or `getGroupBuilder()` outside `CrimsonIsle` itself. The stitching serves
no consumer.

`02-parent-access.md` §4.4 expected this site to need an ancestor-relative `@SerializedPath` - a
`@SerializedPath` that can climb. `10-annotation-designs.md` §14.2 declined that, and §14.4 showed
why the site never needed it: this is a naming problem wearing a `parent-access` costume. The JSON
says the two objects are siblings. Let the Java say it.

### 9.2 The end state, as real Java

```java
// Kuudra
@SerializedName("kuudra_completed_tiers")
private @NotNull Kuudra kuudra = new Kuudra();
@SerializedPath("kuudra_party_finder.search_settings")
private @NotNull Kuudra.SearchSettings partyFinderSearch = new Kuudra.SearchSettings();
@SerializedPath("kuudra_party_finder.group_builder")
private @NotNull Kuudra.GroupBuilder partyFinderGroupBuilder = new Kuudra.GroupBuilder();
```

and on `Kuudra`, both `transient` fields deleted:

```java
@Getter
public class Kuudra {

    @Capture(filter = "^highest_wave_")
    private @NotNull ConcurrentMap<Tier, Integer> highestWave = Concurrent.newMap();
    @Capture
    private @NotNull ConcurrentMap<Tier, Integer> completedTiers = Concurrent.newMap();

    // ... nested SearchSettings and GroupBuilder unchanged ...

}
```

`implements PostInit`, the body, the `PostInit` import, both `@Getter(AccessLevel.NONE)` and both
`transient` fields go. `lombok.AccessLevel` survives on `CrimsonIsle` only if another field needs it -
it does not, so that import goes too.

This is the cheapest scalp in the pack: one `PostInit` implementor retired, four fields deleted, one
encapsulation leak closed, two snake-case identifiers corrected, and round-trip fidelity **improved** -
both fields stay bound and non-`transient`, where the old `Kuudra` transients were never serialized at
all.

Rank it by cost, not by value: it fixes no bug and saves no measurable time. It is worth doing because
it is the last `implements PostInit` in its package and because leaving one behind means the interface
looks alive.

**Do not build an annotation for this shape.** A "write this path into that nested field" element
would be a target-path inverse of `@SerializedPath`, serving a two-line body at exactly one site.
`10-annotation-designs.md` §14.3 costed the buildable version at roughly twenty library lines and then
showed it breaks serialization on the way, because the value would live on a `transient` field of a
child and the delegate would never emit it. The registry has no row for it and should not gain one.

## 10. Election

### 10.1 The control case

`Election.java`:43-53:

```java
this.voting = new Cycle(
    new SkyBlockDate(this.getYear(), Season.LATE_SUMMER, 27, 0),
    new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0)
);
this.term = new Cycle(
    new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0),
    new SkyBlockDate(this.getYear() + 2, Season.LATE_SPRING, 27, 0)
);
```

One input, `year`, a plain `int` bound by stock Gson. No siblings, no parent, no repository, no JSON
shape problem of any kind. Two immutable value objects built by arithmetic.

This is the control case for the whole document's thesis: **the hook here buys literally nothing but
eagerness.** There is no shape to declare, so no annotation could ever have claimed it; there is no
sibling to order against, so no ordering mechanism helps; and the values are cheap enough that even
the eagerness is not a performance argument in either direction. What is left is a hook that exists
because the author needed somewhere to put two assignments.

It is also the one implementor with a third invocation path: `Election(int)` at `:22-25` calls
`this.postInit()` from a **constructor**, which has nothing to do with deserialization. That call site
is how the sibling copy in `Simplified-Api/skyblock` is used exclusively (§12.4).

### 10.2 The end state, as real Java

```java
@Getter
@NoArgsConstructor
public class Election {

    private int year;

    public Election(int year) {
        this.year = year;
    }

    /**
     * Voting cycle for this election, running from late summer of its year to late spring of the
     * next
     */
    public @NotNull Cycle getVoting() {
        return new Cycle(
            new SkyBlockDate(this.getYear(), Season.LATE_SUMMER, 27, 0),
            new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0)
        );
    }

    /**
     * Term the elected mayor serves, running from late spring of the following year for one year
     */
    public @NotNull Cycle getTerm() {
        return new Cycle(
            new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0),
            new SkyBlockDate(this.getYear() + 2, Season.LATE_SPRING, 27, 0)
        );
    }

}
```

`implements PostInit`, the body, the `PostInit` import, both `transient` fields and the constructor's
`this.postInit()` call all go. `@Getter` no longer has `voting` and `term` to generate accessors for,
so the hand-written ones take over with no call-site change. `SpecialElection` (`:14`) and
`VotingBooth` (`:12`) extend `Election` and inherit the result with no edit.

### 10.3 The `equals` trap

`Election` is the only response DTO that hand-writes `equals`/`hashCode`/`toString` (`:27-58`), and
all three route through `getVoting()` and `getTerm()`. That is what makes this conversion look free
and is exactly where it is not.

`Cycle` (`:60-72`) declares no `equals`. Today `voting` is a field, so two `Election` instances with
the same `year` hold the *same* `Cycle` object only if they are the same instance - meaning
`Objects.equals(this.getVoting(), election.getVoting())` already compares by identity and already
returns `false` for two separately-constructed equal elections. Converting to computed accessors does
not introduce that bug; it makes an existing one impossible to miss, because now every call allocates.

There are two correct fixes and one wrong one.

- **Correct, and preferred: drop the derived values from identity.** `voting` and `term` are pure
  functions of `year`, so they contribute nothing. `equals` becomes `this.getYear() ==
  election.getYear()`, `hashCode` becomes `Objects.hash(this.getYear())`. The class gets simpler and
  `SkyBlockDate.java`:389's `mayors.add(new Election(mayorDate.getYear()))` starts behaving the way
  its author plainly intended.
- Correct but heavier: give `Cycle` an `equals`/`hashCode`. Worth doing only if something else needs
  to compare cycles, and nothing does.
- **Wrong: memoise the accessors to preserve identity comparison.** That keeps a broken `equals`
  working by accident and reintroduces a field for no other reason.

`f01-election-lazy-cycles` recommends memoising because `Election.equals` is used; this document
overrides that on the ground that the identity comparison is already wrong. Memoisation is a
performance decision, and two `Cycle` plus four `SkyBlockDate` allocations do not need one.

## 11. The irreducible residue

### 11.1 What genuinely stays imperative

Counted honestly, after all six conversions. None of it shrinks, and pretending otherwise is the main
way a document like this over-claims.

| Residue | Where | Lines | Why no annotation reaches it |
| --- | --- | --- | --- |
| Accessory family de-duplication and NBT walk | `AccessoryBag.java`:57-163 | ~110 | Three inputs, none of them a sibling JSON field: an NBT compound tag, the `Accessory` repository, and three values from the enclosing member |
| Repository id resolution | 11 sites, listed in `f05-repository-derivations` | ~1 each | `gson-extras` must not depend on `dev.sbs.skyblockdata`, and 10 of the 11 are already one-line lazy getters that an annotation would replace with a reflected string of equal length |
| Repository-shaped output | `Bestiary.getFamilies()`, `Bestiary.Mob.getFamily()` | ~15 | The result has one entry per repository row; the JSON is an input to the computation, not its shape |
| Composite key parsing | `Contest.getSkyBlockDate()` / `getCollectionName()` | 6 | "Split on `:`, last part keeps its remaining delimiters" is one `StringUtil.join` call and an annotation element nobody has designed |
| `<id>_<n>` key-family scans | `SkyBlockMember`, `PlayerData.java`:55-62, `Bestiary` | ~6 each | Three sites share the parse and none share the reduction - max, all-sorted, and both-halves |
| The three-value handover | `SkyBlockMember.getAccessoryBag()` | 5 | A child cannot see its parent, and §5.1 shows why no bind-time mechanism can fix that correctly |

That is roughly 150 lines of genuinely imperative Java across 133 files, and it is the same 150 lines
before and after this document. **What changes is not how much imperative code exists - it is when it
runs.** Every row above moves from "eagerly, during decode, with its exception swallowed" to "on
demand, from an accessor, with its exception thrown at the caller".

The one thing worth consolidating is the `<id>_<n>` scan. `10-annotation-designs.md` §17.1 rejected
`@Tier` and proposed a package-private helper in this module instead - one pass turning a list of
`<id>_<n>` strings into a `Map<String, List<Integer>>`, with each of the three sites reducing it
differently in two lines. That is `small`, consumer-only, and it belongs after the conversions rather
than inside them, so the conversions can be verified one class at a time.

### 11.2 Where eagerness is genuinely required

The whole argument turns on the claim that nothing needs a value to exist before it is asked for.
That claim deserves an adversarial pass, because if it fails anywhere the hook survives there.

**1. Fail-fast on malformed input at decode.** A real reason to be eager in general. It does not apply
here, because the hook's exceptions go into an empty `catch (Exception ex) {}` - the current design
already refuses to fail fast. Making the catch log (§11.3) improves the diagnosis but does not restore
fail-fast, and rethrowing was explicitly not proposed.

**2. Immutability of a decoded object.** Today a `SkyBlockMember` is (in principle) finished when
`fromJson` returns, and memoised accessors give that up. The property is worth less than it sounds:
three of the six hooks do not currently run to completion, so the "finished" object is finished with
wrong values. Nothing in this module or in `SkyBlock-Simplified` branches on the distinction.

**3. Thread safety.** Related and slightly stronger. These DTOs use `Concurrent*` collections
throughout and are plainly expected to be shared. The memo pattern races. Both racers compute equal
values from immutable inputs so the race is benign, and `HypixelPlayer.java`:82-91 already ships it -
but this is a genuine cost, not a non-issue, and each memo site should say so in a comment.

**4. Serialization.** No effect. Every derived field is `transient` and Gson's default `Excluder`
skips transients, so nothing was written before and nothing is written after. Where laziness *helps*
round-tripping is `JacobsContest.Contest`, whose two derived fields are non-`transient` today and
serialize keys the API never sent.

**5. JPA persistence.** **This is the one place eagerness is genuinely required, and it is the reason
the interface survives.** `JpaRepository.persistToDatabase` (`:250-268`) calls `postInit()` on every
entity before `statelessSession.upsertMultiple(entities)`. The `dev.sbs.skyblockdata.model` entities
use **field access** - `@Id` and `@Column` sit on fields, as in `BestiaryFamily.java`:30-37 - so
Hibernate reads fields directly and never calls an accessor. A derived value that must be persisted
therefore has to exist in a field before the upsert, and a lazy accessor is invisible to it.

The honest qualifier: **no entity in the workspace implements `PostInit` today.** A symbol search over
every module returns six implementors in `response/` and one in `Simplified-Api/skyblock`, and that
seventh (`dev.sbs.skyblockdata.date.Election`) carries no `@Entity` annotation - it is a value type
built by `new Election(year)` from `SkyBlockDate.java`:389. So `JpaRepository.java`:255-256 is a live
call site with zero live implementors. It is an extension point, not a dependency.

Everything else on this list is a preference. Point 5 is a real constraint with no current user.

### 11.3 The smallest surface for the hook that remains

If something genuinely needs imperative post-bind code - and per §11.2 exactly one shape does,
persisted derived state on a JPA entity - what is the smallest thing that serves it?

Three candidates, and the smallest is not a new mechanism.

**A. Keep `PostInit` exactly as it is.** Zero work. Also keeps the empty catch, the wrong javadoc, the
unchecked null and, most importantly, the absence of any statement about what a `postInit()` body may
legally read. The next author writes the seventh implementor and repeats
`f01-accessorybag-order-inversion`.

**B. Keep `PostInit`, narrow its contract.** No new API. Three changes to two files in `gson-extras`,
all of which fit in the single publish cycle `10-annotation-designs.md` §20.2 already budgets:

```java
@Override
public T read(JsonReader in) throws IOException {
    T obj = delegate.read(in);

    if (obj != null) {
        try {
            ((PostInit) obj).postInit();
        } catch (Exception ex) {
            LOGGER.warn("Post-initialization of '{}' failed", obj.getClass().getName(), ex);
        }
    }

    return obj;
}
```

The null guard matters and is easy to miss: a JSON `null` for a `PostInit`-typed field currently makes
the cast throw `NullPointerException`, which the same empty catch eats. Adding logging without the
guard turns a benign, normal-input path into a warning per null field. This is not a behaviour change -
`PostInit.java`:13-14 already promises exceptions are "logged and swallowed", and the code simply
never did it - which is why it is safe for every module already pinned to `gson-extras`.

The javadoc rewrite is the half that outlives the fix, and it is the actual deliverable:

```java
/**
 * Callback interface for post-deserialization initialization.
 * <p>
 * Implementors have {@link #postInit()} invoked by {@link PostInitTypeAdapterFactory} the moment
 * their own deserialization completes. The hook fires bottom-up - every descendant has already run
 * when a parent's hook runs - so a body may read its own fields and anything beneath them, and may
 * not read a sibling, an ancestor, or a value another hook derives. There is no ordering between
 * sibling objects and none between derived fields.
 * <p>
 * Prefer a computed accessor. A value derived from bound state can be produced on demand, at which
 * point the whole document is available and the ordering constraints above stop applying. This hook
 * exists for state that must exist in a field before something reads the field directly rather than
 * through an accessor - persistence with field access is the motivating case.
 * <p>
 * Exceptions thrown from {@code postInit()} are logged and swallowed - the deserialized object is
 * still returned, partially initialized, and indistinguishable from one that succeeded.
 *
 * @see PostInitTypeAdapterFactory
 */
```

**C. Replace it with something narrower** - a `@PostBind`-style method annotation, or a per-field
computed marker. Rejected, and the argument is already on the record:
`10-annotation-designs.md` §15 rejects `@Derive` for being `PostInit` with a reflected method name,
and §19 rejects `@Bind` for buying ordering that laziness makes free. A narrower *annotation* would be
a new library surface serving the one use case (§11.2 point 5) that the existing interface already
serves adequately. Narrowing the **contract** costs nothing; narrowing the **mechanism** costs a
design, a factory and a cycle.

**Choose B.** It is the smallest possible surface because it adds no surface at all - nine lines of
behaviour that the documentation already claimed, plus a paragraph that tells the next author to use
an accessor.

### 11.4 Should `PostInit` survive, and in what form

**Survive, unimplemented, with the contract from §11.3.** The reasoning, including the case against.

**The case for deleting it.** After this document's six conversions and §12.4's seventh, `PostInit`
has zero implementors in the workspace. `PostInitTypeAdapterFactory` then wraps nothing -
`create` returns `null` for every type that is not `PostInit`-assignable, so it costs one
`isAssignableFrom` per type token and nothing at runtime. An interface with no implementors is dead
weight in three modules, and dead weight in a library is how the next author concludes it is the
intended pattern. `01-postinit.md` §2 already notes the interface is not really a Gson hook at all -
it has a second, manual call site - which is a design smell in its own right.

**The case for keeping it, which wins.** Three reasons, in ascending order.

1. **`gson-extras` is a published library pinned by sha across sibling modules.** Removing a public
   interface is the `xlarge` row of the effort scale - a semantic break requiring review, not
   recompilation, at every consumer. This is the weakest of the three reasons and it should not be the
   one anyone cites, because "we cannot remove it" is not the same as "we should keep it".
2. **`JpaRepository` has a coherent, currently-unused need for it** (§11.2 point 5, §12.2). Field
   access means a persisted derived column cannot be lazy. Deleting the interface deletes the only
   contract that expresses "finish yourself before I read your fields directly".
3. **The escape hatch has to exist somewhere.** This pack rejected sixteen registry proposals largely
   on the argument that laziness is better than a mechanism. That argument is strong precisely because
   there is still a hook available for the case it does not cover. Removing the hook would make the
   next unusual shape into a library redesign instead of a five-line body.

**What survival must not mean.** Not "leave it as it is". An interface kept for an extension point
with no users, documented as though it were the default way to compute derived state, is how six
implementors accumulated in one module. The javadoc in §11.3 is the load-bearing part of this
recommendation, not the logging.

**What would change the answer.** If `JpaRepository.java`:255-256 is removed - and it is a plausible
removal, since it has never had an implementor - then reason 2 evaporates and only the compatibility
argument remains. At that point deprecating the interface with a pointer to the accessor convention is
the right call, and deleting it a release later is defensible. That decision belongs to whoever owns
`persistence`, not to this pack.

## 12. Blast radius outside hypixel

### 12.1 The five consumers

Every reference to `PostInit` or `PostInitTypeAdapterFactory` outside `api/simplified/hypixel`,
enumerated by symbol search over all four module families. There are five, in four modules.

| Site | Kind | Affected by retiring hypixel's six? |
| --- | --- | --- |
| `Simplified-Dev/gson-extras/.../PostInit.java` | the interface | no - unchanged, then re-documented |
| `Simplified-Dev/gson-extras/.../factory/PostInitTypeAdapterFactory.java` | the factory, registered in `GsonSettings.java`:249-256 | no - the wrap chain is unchanged; it simply never wraps anything from this module again |
| `Simplified-Dev/gson-extras/src/test/.../GsonFactoryTest.java` | library tests - `PostInitTests` and `CaptureWithPostInitModel` | no - they declare their own models; they are the only remaining implementors after the migration and they should stay |
| `Simplified-Dev/persistence/.../JpaRepository.java`:9,255-256 | manual invocation before upsert | no - see §12.2 |
| `Simplified-Dev/dataflow/.../serde/PipelineGson.java`:21,41,49 | registers the factory in a second, independently built `Gson` | no - see §12.3 |

Plus one implementor in a fifth module, `Simplified-Api/skyblock` (§12.4).

**The load-bearing conclusion: retiring hypixel's usage breaks nothing outside hypixel.** Every
conversion in §5 to §10 is consumer-only, touches one module, and is individually revertable.
`gson-extras` is not recompiled, not re-published, and not re-pinned for any of it. That is what makes
the staged plan in §13 safe: the library change is optional, separable, and last.

### 12.2 `JpaRepository` - the one caller with a coherent future

`JpaRepository.java`:250-268:

```java
void persistToDatabase(@NotNull Source<T> source) throws JpaException {
    ConcurrentList<T> entities = source.load(this);
    this.lastLoadedEntities = entities;

    entities.forEach(entity -> {
        if (entity instanceof PostInit)
            ((PostInit) entity).postInit();
    });

    try (StatelessSession statelessSession = this.getSession().getSessionFactory().openStatelessSession()) {
        ...
    }
}
```

Three facts about this call site, all verified rather than assumed.

- **It is not a Gson hook.** `PostInit` here is a general "finish initializing yourself" contract with
  a second, imperative call path. An entity that arrives from JSON therefore gets `postInit()` **twice** -
  once from the factory during decode, once here - so every body must be idempotent. All six hypixel
  bodies happen to be (pure assignment of derived state), and nothing enforces it.
- **It has no implementors.** No `@Entity` class in the workspace implements `PostInit`. The branch
  has never executed.
- **It is nonetheless the one coherent use of the interface.** The `dev.sbs.skyblockdata.model`
  entities use field access, so Hibernate reads fields directly. A derived value that must be
  persisted has to be in a field before `upsertMultiple` runs, and no accessor - lazy or otherwise -
  is consulted.

**What a deprecation path owes it.** Nothing, for the migration in §13 - retiring hypixel's six
implementors cannot affect a branch with zero implementors. But if `PostInit` is ever *deleted*, this
call site is the thing that has to be answered first, and the answer is a decision rather than a
refactor: either accept that entities can never carry derived persisted state, or replace the contract
with something narrower owned by `persistence` rather than by a JSON library. Both are reasonable;
neither is this pack's call. §11.4 records that if this call site goes, the argument for keeping the
interface goes with it.

### 12.3 `PipelineGson` and `GsonSettings`

`PipelineGson.java`:49 builds a second `Gson` and registers `new PostInitTypeAdapterFactory()`
alongside `CaseInsensitiveEnumTypeAdapterFactory`, with the factory named in its class javadoc at
`:41`. `GsonSettings.java`:249-256 registers the same factory in the primary settings chain.

Neither is affected by any consumer-side retirement, for a structural reason worth stating so nobody
re-checks it: `PostInitTypeAdapterFactory.create` returns `null` for any type that is not
`PostInit`-assignable (`:19-20`), so it never enters the wrap chain for a type that does not implement
the interface. Removing `implements PostInit` from six classes changes what the factory wraps; it does
not change how it is registered, ordered or built.

The one thing that *would* touch both is a change to the factory's behaviour, which is exactly the
change §11.3 proposes. Logging and a null guard are additive and match the documented contract, so
`PipelineGson` inherits an improvement rather than a break. Two consequences for sequencing:

- **`PipelineGson` gets the new log line too**, from a `Gson` built for a different purpose with
  different models. If any dataflow model implements `PostInit` and throws today, it starts logging.
  None does, as of the symbol search above, but the pin lands for both modules at once and dataflow's
  maintainer should be told rather than surprised.
- **The re-pin is workspace-wide, not module-scoped.** `gson-extras` is consumed by sha, and the
  sibling modules share the pin. `00-conventions.md` §4 prices this: publish, wait for green, edit the
  pin, rebuild, re-test - and it is not parallelizable across siblings. That cost is why §13 puts the
  library change last and alone.

### 12.4 The seventh implementor, in `Simplified-Api/skyblock`

`Simplified-Api/skyblock/src/main/java/dev/sbs/skyblockdata/date/Election.java`:14 is a
**character-for-character duplicate** of the hypixel `Election` in §10 - same two `transient Cycle`
fields, same `postInit()` body, same constructor calling `this.postInit()`, same
`equals`/`hashCode`/`toString` routing through the derived accessors.

Two facts decide its disposition.

- **It is never deserialized.** Its only construction site is `SkyBlockDate.java`:389,
  `mayors.add(new Election(mayorDate.getYear()))`, and `SpecialElection.java`:13 for the subclass. It
  reaches `postInit()` exclusively through its own constructor.
- **It is not an entity.** It carries no `@Entity`, no `@Id`, no `@Column`, and lives in
  `dev.sbs.skyblockdata.date` rather than `.model`. So it is not the `JpaRepository` customer either.

Its `implements PostInit` is therefore decorative: the interface it implements is never used by
anything that calls it. The §10.2 conversion applies verbatim, in one file, with no JSON involvement
at all, and it is the change that takes the workspace-wide implementor count to zero.

**It is deliberately not in this pack's scope** - `01-postinit.md` §2 correctly says "not in scope",
and `skyblock` is a separate git repo with its own build. Two things follow for the plan:

- §13's stages must not *depend* on it. Hypixel's six retire whether or not skyblock's seventh does.
- The claim "`PostInit` has zero implementors" is only true after it lands, so any decision that rests
  on that claim - deprecating the interface, §11.4's reopen condition - is gated on a change in
  another repository. Say so rather than quietly assuming it.

The one genuine coupling to check before touching it: `skyblock` is a dependency of `hypixel`
(`SkyBlockMember` imports `dev.sbs.skyblockdata.date.SkyBlockDate`), so a change there means a
`skyblock` publish and a `hypixel` re-pin - the same JitPack cost as a `gson-extras` change. Bundle it
with a `skyblock` publish that is happening anyway; do not spend a cycle on it alone.

## 13. Staged deprecation plan

`20-implementation-plan.md` owns the final ordering. This section owns the dependency graph it has to
respect, expressed as the question the brief asked: **which annotations must land before `PostInit`
can be removed, and in what order, so the module never breaks mid-migration?**

The answer to the first half is short enough to state before the table: **none must land.** Every
annotation these conversions use - `@Capture`, `@Collapse`, `@Key`, `@SerializedPath`,
`@SerializedName` - already ships and is already in production use in this module. The prerequisite
graph is therefore about *defects* and *field types*, not about library capability.

### 13.1 What must land before `implements PostInit` can go, per class

| Class | Prerequisite | Kind | Why it must come first |
| --- | --- | --- | --- |
| `Election` | none | - | Self-contained. The `equals` decision (§10.3) is part of the same edit, not a prerequisite |
| `CrimsonIsle` | none | - | Two field renames and two deletions in a second file |
| `JacobsContest` | none | `@Collapse` + `@Key`, both shipping | `@Key` on a transient is already proven by `SlayerBoss` |
| `Dungeons` | `DungeonData` loses its `experience` field | consumer retype | Both the lazy route and the `@Capture` route need it; the `@Capture` route additionally needs a spike (§6.4) |
| `Bestiary` | the `Matcher.group` fix and the `MOB_PATTERN` widening | correctness | Without them the conversion cannot be verified - the before and after are both empty |
| `SkyBlockMember` | `AccessoryBag`'s read-before-assign and dead store fixed; `AccessoryBag.initialize` narrowed to three values | correctness, then reshape | The hook currently aborts on its first statement, so nothing downstream of it has ever run. Convert against known-good values, not against today's silently wrong output |

Two prerequisites are worth calling out because they are easy to schedule wrongly.

**The `gson-extras` logging fix must come last, not first.** It is tempting to land it early so the
conversions can be observed. Do the opposite. `Bestiary` and `AccessoryBag` throw on **every** decode
today, so enabling the log before those are fixed produces one warning per member per request and
trains everyone to filter it out. `10-annotation-designs.md` §19.5 states the same sequencing.

**The `@Capture` spike for `Dungeons` gates nothing.** §6.3 retires the implementor without it. The
spike decides whether a later, purely-additive simplification is available, so it must not sit on the
critical path.

### 13.2 The stages

Six stages. Stages 1 to 4 are one module, zero JitPack cycles, and individually revertable.

**Stage 1 - correctness, consumer-only.** No structural change, no `implements PostInit` removed. Fix
`AccessoryBag`'s read-before-assign (`:57` versus `:138`) and its dead `magicalPower` store
(`:129-136`); fix the four `Matcher.group`-without-match sites (`Bestiary.java`:61-63 and
`DungeonRun.java`:46,50,54); widen `MOB_PATTERN`; lowercase both master-mode literals in
`Dungeons.postInit()`. This turns four dark features back on and establishes the expected values every
later stage is verified against. It is also the stage that can be shipped on its own if everything
after it is abandoned.

**Stage 2 - the two free retirements.** `Election` (§10.2, including the `equals` simplification) and
`CrimsonIsle` (§9.2). Both are `trivial`, neither depends on stage 1, and between them they prove the
computed-accessor pattern and the sibling-rename pattern on the two simplest classes in the set. Two
of six implementors gone.

**Stage 3 - the shape retirements.** `JacobsContest` via `@Collapse` + `@Key` (§7.2) and `Dungeons`
via the retype plus the lazy pairing (§6.3). Both are `small`, both are annotation-or-type changes
rather than logic changes, and both delete a `@Getter(AccessLevel.NONE)` staging field. Four of six
gone.

**Stage 4 - the derivation retirements.** `Bestiary` (§8.2, depends on stage 1) and `SkyBlockMember`
(§5.4, depends on stage 1's `AccessoryBag` fixes), including narrowing `AccessoryBag.initialize` to
three values and putting the bag's derived state behind memoised accessors. This is the largest stage
and the only one that changes a public method signature. **Six of six gone; `response/` contains no
`implements PostInit`.**

**Stage 5 - the single library cycle.** `PostInitTypeAdapterFactory` gets the null guard and the log;
`PostInit` gets the rewritten javadoc from §11.3. Publish `gson-extras`, wait for JitPack green, edit
the pin, rebuild, re-test. `10-annotation-designs.md` §20.2 puts `@Fallback` and `@Flatten` in this
same commit, which is the whole reason a cycle is being spent at all - the logging fix alone does not
justify one.

**Stage 6 - the seventh implementor, in another repository.** `Simplified-Api/skyblock`'s `Election`
(§12.4), bundled with a `skyblock` publish that is happening anyway. Only after this does the
workspace-wide implementor count reach zero, and only then can §11.4's deprecation question be asked.

### 13.3 Why the module never breaks mid-migration

Four properties, and each one is what makes a given stage boundary safe to stop at.

**Every stage leaves a compiling, passing module.** No stage removes a method another stage adds. The
only signature change in the set is `AccessoryBag.initialize`, and both its call site and its
declaration are inside stage 4.

**`implements PostInit` is removed per class, never in a sweep.** The factory keys off
`PostInit.class.isAssignableFrom(type.getRawType())` per type, so a module where three classes
implement the interface and three do not is a perfectly normal state. There is no "all or nothing"
boundary and no intermediate configuration to hold.

**Accessor names do not change.** Every conversion replaces a Lombok-generated getter with a
hand-written one of the same name and same return type, or replaces a transient field with a bound
field of the same name. `ProfileStats.java`, `SkyBlockIsland.java` and `SkyBlockUserCommand.java` are
not edited by any stage. The two exceptions are deliberate and both narrow: `DungeonData`'s
constructor loses a parameter (stage 3, and `EMPTY_DUNGEON` is its only external caller), and
`SkyBlockMember.getFirstJoin()` would narrow covariantly if the unrelated `@SerializedPath` adoption
from `10-annotation-designs.md` §4.3 is taken in the same window - which it should not be, for exactly
this reason. Keep the two migrations apart.

**The library is untouched until stage 5.** Stages 1 to 4 cannot be broken by a JitPack failure, a pin
conflict or a sibling module's rebuild, because they do not involve `gson-extras`. This is the
property that makes the plan cheap to abandon halfway.

One ordering constraint that is *not* obvious and must be honoured: **stage 1 before stage 4, always.**
Converting `SkyBlockMember` while its hook still aborts means comparing new output against nothing at
all. The whole point of doing the correctness work first is that the conversions get a real expected
value to verify against.

### 13.4 Verification per stage

The module has a fixture and a differ; use them rather than reasoning about the diff.

| Stage | Verification |
| --- | --- |
| 1 | Decode the fixture's populated member and assert the four previously-dark values are non-empty: `accessoryBag.getMagicalPower() > 0`, `bestiary.getFamilies()` non-empty, `dungeons.getDungeon(CATACOMBS).getFloorData(true)` carries `highest_tier_completed = 7`, and no `UNKNOWN` key in `getDungeons()`. `skills` and `collectionUnlocked` become non-null as a side effect, which is the acceptance test for the hook no longer aborting |
| 2 | `Election.getVoting()` / `getTerm()` equal the pre-change values for a fixed year; two `Election`s with the same year are now `equals`. `CrimsonIsle` - assert `partyFinderSearch` and `partyFinderGroupBuilder` bind, and that the class round-trips including `kuudra_party_finder` |
| 3 | `JacobsContest.getContests()` size is 810 for the fixture member, `getCollectionName()` on a brown-dye contest is `INK_SACK:3`, and serializing a `Contest` emits no `skyBlockDate` or `collectionName` key. `Dungeons.getClasses()` has five entries with the pre-change experience values |
| 4 | `Bestiary.getFamilies()` matches stage 1's values with no JPA session stood up for the *decode* - the session is only needed at accessor time. `SkyBlockMember` - `getCollectionUnlocked()` equals stage 1's map exactly, and the `FORAGING` level subtractor is `0` for a synthetic member with `FIG_LOG_9` |
| 5 | `gradle_verify` on `gson-extras` (`GsonFactoryTest`'s `PostInitTests` still pass, including the failing-model test), then re-pin and `gradle_verify` on every consuming module, then confirm a full fixture decode produces **zero** post-init warnings |
| 6 | `skyblock` compiles and `SkyBlockDate`'s mayor calendar produces unchanged cycles |

Run `scripts/json_dto_diff.py` at the end of stages 3 and 4. Both change which JSON keys are claimed
by a declared field - `@Collapse` on `contests`, the `player_classes` retype, `kuudra_party_finder`
becoming two named fields - and the differ is the cheapest way to catch a key that silently stopped
binding.

### 13.5 Rollback

Per stage, and every one is a `git revert` of a single commit in a single module.

- **Stages 1 to 4** touch only `Simplified-Api/hypixel`. Revert the commit; nothing else moves. There
  is no published artifact and no pin to unwind. Within a stage, each class is its own commit, so
  reverting `Bestiary` does not take `SkyBlockMember` with it.
- **Stage 5** is the only stage with a two-repository rollback. Revert the pin edit in every consuming
  module first, then the `gson-extras` commit. The published sha stays on JitPack and is harmless -
  nothing references it once the pins are back. Do not revert the `gson-extras` commit while any
  module is still pinned to it.
- **Stage 6** is a `skyblock` revert plus a `hypixel` pin revert, in that order, and only if it was
  bundled into a publish rather than riding one.

The one irreversible thing in the plan is not code. Stage 4 changes *when* exceptions surface for
every consumer of these DTOs - from swallowed at decode to thrown at the caller. That is the correct
trade and it is why `Bestiary`'s dark feature stayed dark for so long, but a consumer that has been
silently tolerating an empty collection will start seeing a stack trace. It belongs in the release
note, not in a comment.

## 14. Design entries

### d11-skyblockmember-wire-on-access

- **Retires:** `SkyBlockMember`
- **Absorbed by:** a wire-on-access `getAccessoryBag()` plus two memoised accessors - no annotation
- **Category:** `postinit-elimination`
- **Answers findings:** `f01-skyblockmember-lazy-skills`, `f02-accessorybag-upstream`,
  `f02-skills-member-reachback`, `f05-collection-tier-join`, `f05-derivation-ordering`
- **Library change:** none
- **Blocks:** `AccessoryBag`'s read-before-assign and dead store must be fixed first
- **Effort:** `small` (3 consumer files - `SkyBlockMember`, `AccessoryBag`, plus the mapping test)

The one entry that goes beyond `10-annotation-designs.md` §19.4, which left this implementor alive as
a single reach-back statement. §5.1 shows the statement does not need a hook, only a frame that runs
after the member is bound, and the member's own accessor is one. Payoff over §13.3's baseline: the
sixth `implements PostInit` retires, the `response.skyblock` to `response.skyblock.member` import
cycle breaks, and the "public method that must be called exactly once" hazard becomes "public method
that is called on every access and is idempotent by construction".

Risk, stated in §5.5 and not repeated here: `initialize` still exists as public API, and the two memo
fields race benignly.

### d11-dungeons-retype-and-lazy

- **Retires:** `Dungeons`
- **Absorbed by:** a corrected field type for `classes`, a memoised `getDungeons()` for the pairing
- **Category:** `postinit-elimination`
- **Answers findings:** `f01-dungeons-capture-grouping`, `f01-dungeons-master-case`,
  `f05-dungeons-master-pairing`, `f03-dungeons-classmap-funnel`
- **Library change:** none
- **Blocks:** `DungeonData` must lose its `experience` field first
- **Effort:** `small` (3 consumer files - `Dungeons`, `DungeonData`, `DungeonClass`)

Half the body was never a computation: the JSON under `player_classes` is already the shape of
`DungeonClass`, so the map-of-maps funnel deletes outright. The other half is a self-join whose two
string literals are in the wrong case, which normalising the key space once makes unrepeatable.

The `@Capture` affix-grouping form (§6.4) is strictly better and strictly later - it needs a spike
against `gson-extras` to confirm `descend = true` with an empty filter is supported. Do not put the
spike on the critical path.

### d11-jacobscontest-collapse-key

- **Retires:** `JacobsContest`
- **Absorbed by:** `@Collapse` + `@Key`, both already shipping, plus two lazy accessors on `Contest`
- **Category:** `postinit-elimination`
- **Answers findings:** `f01-jacobscontest-collapse-key`, `f05-jacobscontest-contest-key`
- **Library change:** none
- **Blocks:** none
- **Effort:** `small` (1 consumer file)

The most annotation-shaped body in the module, and it wants two annotations that already ship. The
part `@Collapse` cannot do - decomposing `<year>:<month>_<day>:<collection>` where the last segment
keeps its own colons - is six lines on the object that owns the key, and
`10-annotation-designs.md` §15.3 shows why encoding it as an annotation element would have silently
truncated every `INK_SACK:3` contest.

Side effects worth as much as the retirement: 810 eager `SkyBlockDate` allocations per member become
on-demand, one malformed key stops emptying all 810, and two phantom serialized keys disappear.

### d11-bestiary-lazy-families

- **Retires:** `Bestiary`
- **Absorbed by:** a memoised `getFamilies()`
- **Category:** `postinit-elimination`
- **Answers findings:** `f01-bestiary-lazy-families`, `f05-matcher-group-without-match`,
  `f05-repository-derivations`
- **Library change:** none
- **Blocks:** the `Matcher.group` fix and the `MOB_PATTERN` widening must land first, or the
  conversion cannot be verified
- **Effort:** `small` (1 consumer file plus memoisation)

The computation does not shrink - it is a repository-shaped join and stays exactly as long. What
changes is that binding stops depending on a live JPA session, which is why
`MemberDtoMappingTest.java`:42-45 currently refuses to decode a whole `SkyBlockMember` at all. A
missing session becomes a recoverable, observable error at the point of use instead of a permanently
empty list assigned once and never revisited.

### d11-crimsonisle-sibling-rename

- **Retires:** `CrimsonIsle`
- **Absorbed by:** nothing - the downward push is deleted and two fields are renamed
- **Category:** `value-shape-collapse`
- **Answers findings:** `f01-crimsonisle-field-copy`, `f02-kuudra-sibling-push`
- **Library change:** none
- **Blocks:** none
- **Effort:** `trivial` (2 consumer files)

The cheapest scalp in the pack, and the one that was hiding behind a proposal for a cross-frame path
mechanism. `02-parent-access.md` §4.4 expected it to need an ancestor-relative `@SerializedPath`;
`10-annotation-designs.md` §14.4 showed the JSON already says the two objects are siblings and the
Java simply disagreed with it.

Delivers no bug fix and no measurable saving. Do it because it is one of six, not because it pays.

### d11-election-computed-cycles

- **Retires:** `Election` - and, in another repository, `dev.sbs.skyblockdata.date.Election`
- **Absorbed by:** two computed accessors
- **Category:** `postinit-elimination`
- **Answers findings:** `f01-election-lazy-cycles`, `f05-lazy-getter-convention`
- **Library change:** none
- **Blocks:** none
- **Effort:** `trivial` in hypixel; `trivial` plus a publish-and-re-pin for the skyblock twin

The control case. No JSON shape, no siblings, no repository - a hook that exists only to make two
values eager. `SpecialElection` and `VotingBooth` inherit the fix with no edit.

This entry **overrides** `f01-election-lazy-cycles`'s recommendation to memoise. That recommendation
was made to preserve `Election.equals`, which compares `Cycle` instances that declare no `equals` and
therefore already compares by identity - so it is already wrong for two separately-constructed equal
elections. §10.3 drops the derived values from identity instead, which fixes the comparison and makes
memoisation unnecessary.

### d11-postinit-narrowed-contract

- **Retires:** nothing - this is the library-side half
- **Absorbed by:** n/a
- **Category:** `correctness`
- **Answers findings:** `f01-postinit-aborts-silently`, `f02-postinit-silent-swallow`
- **Library change:** existing factory edit, plus a javadoc rewrite on the interface
- **Blocks:** all six consumer conversions should land first, so the new logging sees a clean decode
- **Effort:** `small` (2 library files, one JitPack cycle - shared with `@Fallback` and `@Flatten`)

Three changes, none of them a behaviour change relative to the *documented* contract: null-guard
`obj`, log the caught exception, and rewrite `PostInit`'s javadoc to state what a body may legally
read and to point the next author at a computed accessor first. `PostInit.java`:13-14 already promises
"logged and swallowed" and the factory never logged, so this makes the code match its own
documentation.

The javadoc is the durable half. The logging fixes a diagnosis problem that, after this migration, has
no remaining subject in this module; the javadoc is what stops a seventh implementor appearing.

### d11-postinit-interface-retained

- **Retires:** nothing
- **Absorbed by:** n/a
- **Category:** `annotation-abstraction`
- **Answers findings:** the interface-scope half of `f01-postinit-aborts-silently`
- **Library change:** none
- **Blocks:** n/a
- **Effort:** `trivial` (a decision)

**The interface and the factory stay.** Not primarily for compatibility - although removing a public
interface from a sha-pinned library is the `xlarge` row - but because `JpaRepository.java`:255-256
expresses a need that laziness structurally cannot serve: Hibernate reads fields, so a persisted
derived column has to exist before the upsert. That branch has zero implementors today, which makes it
an extension point rather than a dependency, and §11.4 records the condition under which the decision
flips.

The registry row in `00-conventions.md` §6.1 should be annotated accordingly: `PostInit` still exists,
still works, and is no longer the pattern to reach for.

## 15. Verdict

**`PostInit` can be rendered obsolete as a usage and cannot be rendered obsolete as an interface, and
the second half of that sentence has nothing to do with the first.**

All six implementors in `api/simplified/hypixel/response` retire. Zero new annotations are required.
The seventh, in `Simplified-Api/skyblock`, retires with the same edit as the sixth. After that the
interface has no implementors in the workspace and is kept anyway, because `JpaRepository` expresses a
real need for it that no accessor can serve.

The framing this document was handed - "`PostInit` was the precursor to `@Capture`, `@Lenient`,
`@Key`, `@Extract`, `@Collapse`, `@Split`" - is right, and the sequence does finish. It just does not
finish with a seventh annotation. Sorted by what actually absorbed each body:

| Absorbed by | Implementors | Library cost |
| --- | --- | --- |
| An annotation that already ships (`@Collapse` + `@Key`, `@SerializedPath`) | 2 | none |
| A corrected field type, no annotation | half of `Dungeons` | none |
| A computed or memoised accessor | 3 and a half | none |
| A new annotation | **0** | - |

Three results worth carrying into `20-implementation-plan.md`, in this order.

**1. The last category was laziness, and laziness is not a library feature.** Each earlier annotation
removed a *shape* problem from the hook. What was left after all six was not a seventh shape - it was
timing. `PostInit`'s only remaining contribution to any of these classes was that the value existed
before anyone asked for it, and §11.2 walks every candidate reason to want that and finds exactly one,
with no implementor. A getter reads its inputs when it is called, which is always after the whole
document is bound; the JVM performs the dependency ordering that `@Bind` was reserved to declare, for
free, with a stack trace if it ever cycles.

**2. Every eager derivation in this package is currently wrong, and every lazy one is fine.** That
correlation is the pack's strongest empirical claim and it is what makes the recommendation something
other than taste. `SkyBlockMember.postInit()` aborts on its first statement for every member ever
decoded. `Bestiary.families` has never been non-empty. `DungeonData.masterMode` has always been empty.
The `FORAGING` level subtractor has always been `2`. Four silent failures, all in eager post-bind
code, all invisible behind one empty `catch (Exception ex) {}`. Meanwhile `Slayers`, `Skills`,
`HypixelPlayer` and ten of eleven repository lookups do the same class of work lazily and are correct.

**3. The migration is cheap, staged and abandonable.** Stages 1 to 4 touch one module, need no
annotation that does not already ship, need no JitPack cycle, and can be stopped after any stage with
the module compiling and passing. The only library change - a null guard, a log line and a javadoc
rewrite - is last, optional, and rides a cycle `@Fallback` is already paying for.

The uncomfortable part, stated plainly because a reader who skips to this section deserves it: the
answer to "can `PostInit` be rendered obsolete" is **yes, and it could have been at any point since
these classes were written**, because nothing the answer requires was ever missing. Two annotations
that shipped were never applied, one field was declared a level too deep, and four values were
computed too early. What was actually missing was a log line.
