# 01 - PostInit survey

## 1. Scope and method

Every `PostInit` implementor in `api/simplified/hypixel/response` - six classes, confirmed by
symbol search, no others. Each `postInit()` body is quoted, classified, and tested against one
question: *could this be a lazy computed accessor with zero semantic change, and if not, what blocks
it?*

Claims here are not read-only inferences. A throwaway probe test decoded the populated member of
`src/main/resources/craftedfury.json` through `GsonSettings.defaults().create()` and printed the
post-bind state. The probe has been deleted; its output is reproduced verbatim where it is cited, and
every one of the four correctness findings below is an observed value, not a reading of the source.

```
PROBE dungeon keys = [UNKNOWN, CATACOMBS]
PROBE UNKNOWN exp=0.0 normalTier=7 masterTier=0
PROBE CATACOMBS exp=5.932601809190001E8 normalTier=7 masterTier=0
PROBE classes = [HEALER, MAGE, TANK, BERSERK, ARCHER]
PROBE skills == null ? true
PROBE collectionUnlocked size = 0
PROBE collection size = 100
PROBE accessories = 0 magicalPower=0
PROBE contests = 810
PROBE empty NbtContent.getNbtData() THREW lib.minecraft.nbt.exception.NbtException:
      lib.minecraft.nbt.exception.NbtFormatException: Truncated NBT input - need 1 bytes at offset 0
PROBE bag rawData length = 158552
PROBE bestiary families = 0
```

Read that block before reading anything else in this document. Three of the six `postInit()` bodies
do not currently produce the values they claim to produce, and nothing anywhere reports it.

## 2. The mechanism

`dev.simplified.gson.PostInit` is a one-method interface. The whole implementation is 46 lines.

`gson-extras/src/main/java/dev/simplified/gson/factory/PostInitTypeAdapterFactory.java:31-41`:

```java
@Override
public T read(JsonReader in) throws IOException {
    T obj = delegate.read(in);

    try {
        ((PostInit) obj).postInit();
    } catch (Exception ex) {
    }

    return obj;
}
```

Four properties follow, and every finding in this document is downstream of one of them.

- **The catch is empty.** `PostInit.java:13-14` states "Exceptions thrown from `postInit()` are
  logged and swallowed". Nothing is logged. There is no logger in the file, no `ex` use, not even a
  comment. The javadoc is wrong, and that gap is exactly why the defects below survived.
- **There is no ordering.** The hook fires per object as its adapter completes, so children run
  before parents. Within a body, ordering is whatever the author wrote, and there is no declaration
  of what depends on what. `SkyBlockMember.postInit()` gets this wrong today.
- **A body is all-or-nothing.** One throwing statement abandons every later statement in the same
  body. The object is still returned, fully constructed and partly initialized, indistinguishable
  from a healthy one.
- **`obj` is not null-checked.** A JSON `null` for a `PostInit`-typed field makes
  `((PostInit) obj).postInit()` throw `NullPointerException`, which the same empty catch eats. Benign
  today, but it means the catch is load-bearing for normal input, not just for errors.

**Consumers outside hypixel.** Changing `PostInit` is not a hypixel-local decision.

- `Simplified-Dev/persistence/src/main/java/dev/simplified/persistence/JpaRepository.java:255-256`
  invokes `postInit()` **manually**, on every entity, before an upsert. `PostInit` is therefore not a
  Gson hook - it is a general "finish initializing yourself" contract with a second call site. An
  entity loaded from JSON gets `postInit()` twice, so every body must be idempotent. All six here
  are (pure assignment of derived state), but nothing enforces it.
- `Simplified-Dev/dataflow/src/main/java/dev/simplified/dataflow/serde/PipelineGson.java:49`
  registers the factory in a second, independently built `Gson`.
- `Simplified-Api/skyblock/src/main/java/dev/sbs/skyblockdata/date/Election.java:14` is a seventh
  implementor in a sibling module - not in scope, but it means "delete `PostInit`" is off the table
  regardless of what this module does.
- `Simplified-Api/hypixel/.../election/Election.java:24` calls `this.postInit()` from a **constructor**,
  a third invocation path that has nothing to do with deserialization.

The correct framing: retiring `PostInit` *usage in hypixel* is a consumer-only exercise. Retiring the
*interface* is not, and is not proposed.

## 3. Inventory

| Class | Body lines | Inputs | Outputs written | Needs parent | Needs `SkyBlockData` | Lazy-able |
| --- | --- | --- | --- | --- | --- | --- |
| `election/Election` | 44-53 (9) | `year` | 2 transient | no | no | yes, as-is |
| `crimson/CrimsonIsle` | 53-56 (2) | 2 sibling holders | 2 fields on a *child* | no | no | yes, as-is |
| `dungeon/Dungeons` | 56-76 (20) | 2 sibling maps | 2 transient | no | no | not needed - bind it |
| `member/JacobsContest` | 47-63 (16) | 1 sibling map | 1 transient + 2 fields on values | no | no | not needed - bind it |
| `member/Bestiary` | 56-83 (27) | 2 sibling maps | 1 transient | no | **yes** | yes, wants memoizing |
| `skyblock/SkyBlockMember` | 141-156 (15) | 3 siblings + `this` | 2 transient + a reach-back | **yes** | indirectly | yes, wants memoizing |

Totals: **89 lines of imperative post-bind code across 6 classes**, producing 10 transient fields
directly and driving 7 more through `AccessoryBag.initialize(SkyBlockMember)`.

Not one of the six needs a value that is unavailable at accessor-call time. That is the headline of
section 8.

## 4. Per-implementor dissection

### 4.1 Election

`response/skyblock/election/Election.java:43-53`:

```java
@Override
public void postInit() {
    this.voting = new Cycle(
        new SkyBlockDate(this.getYear(), Season.LATE_SUMMER, 27, 0),
        new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0)
    );
    this.term = new Cycle(
        new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0),
        new SkyBlockDate(this.getYear() + 2, Season.LATE_SPRING, 27, 0)
    );
}
```

**Classification:** (e) pure derivation. One input, `year`, an `int` bound by plain Gson. No siblings,
no parent, no repository, no JSON shape problem at all. The two outputs are immutable value objects
built by arithmetic on a single field.

**Lazy-able:** yes, with zero semantic change and zero library work. `getVoting()` and `getTerm()`
become computed accessors; `voting`, `term`, the `postInit()` body, the `this.postInit()` call in the
`Election(int)` constructor (line 24) and `implements PostInit` all delete. `equals`, `hashCode` and
`toString` (lines 27-58) already route through `getVoting()`/`getTerm()`, so they keep working
unchanged. `SpecialElection` (`SpecialElection.java:14`) and `VotingBooth` (`VotingBooth.java:12`)
extend `Election` and inherit the fix.

**Blocks:** none. This is the control case that proves the rest of the pack's thesis - the hook here
buys literally nothing.

**Cost of the current form:** `VotingBooth` and `Mayor.electionResults` (`Mayor.java:16`) are
deserialized routinely; each pays two `Cycle` and four `SkyBlockDate` allocations whether or not
anyone asks. Trivially small, but it is pure waste plus a `PostInit` implementation on the class.

### 4.2 CrimsonIsle

`response/skyblock/member/crimson/CrimsonIsle.java:52-56`:

```java
@Override
public void postInit() {
    this.kuudra.searchSettings = this.kuudra_search_settings;
    this.kuudra.groupBuilder = this.kuudra_group_builder;
}
```

**Classification:** not derivation at all - a downward field copy, the mirror image of a reach-back.
The cause is a JSON/Java shape mismatch: `kuudra` binds from the key `kuudra_completed_tiers`
(line 36-37) while the settings it wants live under an unrelated top-level key,
`kuudra_party_finder.search_settings` and `.group_builder` (confirmed in the fixture). Two
`@SerializedPath` fields land the data on `CrimsonIsle`, then the body pushes it one level down into
a *different object* by writing package-private fields on `Kuudra` (`Kuudra.java:22-23`, deliberately
non-private for exactly this).

**Lazy-able:** yes and better - delete the copy entirely. Nothing outside `CrimsonIsle` calls
`Kuudra.getSearchSettings()` or `getGroupBuilder()`; a symbol search over the workspace returns no
hits. The two `@SerializedPath` fields can stay on `CrimsonIsle`, lose their
`@Getter(AccessLevel.NONE)`, get real names (`kuudraSearchSettings`, `kuudraGroupBuilder` - the
current `kuudra_search_settings` / `kuudra_group_builder` are snake_case Java identifiers, a `naming`
defect in their own right), and `Kuudra` loses two transient fields plus its unnatural
package-private visibility.

**Blocks:** none technical. The only argument for keeping the copy is the API's own grouping - a
caller who thinks in "Kuudra" wants one object. If that is the requirement, the fix is a
`@Delegate`-style forwarder on `CrimsonIsle`, not a mutation of a child during bind.

**Adversarial note:** do not build an annotation for this. A "write this path into that nested
field" annotation would be a target-path inverse of `@SerializedPath`, and it would exist to serve a
two-line body at exactly one site. The registry has no entry for it and should not gain one.

### 4.3 Dungeons

`response/skyblock/member/dungeon/Dungeons.java:55-76`:

```java
@Override
public void postInit() {
    this.dungeons = this.dungeonMap.stream()
        .filterKey(key -> !key.startsWith("MASTER_"))
        .mapKey(DungeonData.Type::of)
        .map((type, value) -> Pair.of(type, new DungeonData(
            value.getExperience(),
            value,
            this.dungeonMap.getOrDefault(
                String.format("MASTER_%s", type.name()),
                new FloorData()
            )
        )))
        .collect(Concurrent.toUnmodifiableMap());

    this.classes = this.classMap.stream()
        .map(entry -> Pair.of(
            entry.getKey(),
            new DungeonClass(entry.getValue().get("experience"))
        ))
        .collect(Concurrent.toUnmodifiableMap());
}
```

Two unrelated computations sharing a body.

**`classes` - (a) reshaping one field into another typed field.** `classMap` is
`ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>>` (line 30-32) and the JSON is
`{"healer": {"experience": 84271835.04}, ...}`. The body's whole job is to pull `["experience"]` out
of each value map. But `DungeonClass` (`DungeonClass.java:19`) has exactly one field,
`private final double experience`. So the JSON already *is* a `DungeonClass`. Declaring the field as
`ConcurrentMap<DungeonClass.Type, DungeonClass>` binds it directly - no annotation, no postInit, no
`classMap`. Gson sets final fields reflectively and `CaseInsensitiveEnumTypeAdapterFactory` maps
`healer` to `HEALER`; the probe confirms the enum keys already resolve correctly today. This half of
the body is **pure ceremony around a map-of-maps funnel that never needed to exist**.

**`dungeons` - (b) joining two sibling entries of the same map.** Keys are `catacombs` and
`master_catacombs`; the code pairs them into one `DungeonData(experience, normalMode, masterMode)`.
This is the only genuinely structural operation in the file, and `@Capture`'s affix grouping already
does it. `Capture.java:37-47` documents prefix affixes (`@SerializedName("master_")`) and a bare
field (`@SerializedName("")`) matching the base key, and
`CaptureTypeAdapterFactory.java:432-455` implements exactly that grouping. So:

```java
@Capture(descend = true)
@SerializedName("dungeon_types")
private @NotNull ConcurrentMap<DungeonData.Type, DungeonData> dungeons = Concurrent.newMap();
```

with `DungeonData` carrying `@SerializedName("") FloorData normalMode` and
`@SerializedName("master_") FloorData masterMode` produces `{CATACOMBS: DungeonData(normal, master)}`
in one bind. `DungeonData.experience` becomes `getNormalMode().getExperience()` - it is already
nothing else (line 60-61 of the body passes `value.getExperience()`).

**Lazy-able:** the question does not arise. Both halves are *bind*-expressible, which is strictly
better than lazy - no hook, no transient, no recomputation.

**Blocks:** none for `classes`. For `dungeons`, one unverified interaction: the `descend` javadoc
example pairs `descend = true` with a non-empty `filter`, and a bare `@Capture` is documented as a
catch-all limited to one per class. Whether `descend = true` plus an empty filter is a supported
combination needs a test against `gson-extras` before this is planned. That is a spike, not a
library change.

**Correctness:** the current body is broken - see `f01-dungeons-master-case`.

### 4.4 JacobsContest

`response/skyblock/member/JacobsContest.java:46-63`:

```java
@Override
public void postInit() {
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
}
```

**Classification:** (a) reshaping, specifically *key decomposition* - the map key carries three facts
(`99:6_27:PUMPKIN` = year 99, date 6/27, collection `PUMPKIN`) that the value object needs. The body
converts a keyed map into a list and back-fills two fields on each value.

**This is `@Collapse` + `@Key`, which already exists.** `Collapse.java:37-42` documents the list form
verbatim: a JSON object becomes a `List<V>` with each entry's key injected into a `@Key` field. So
`contestMap` and the `.collect()` disappear:

```java
@Collapse
@SerializedName("contests")
private @NotNull ConcurrentList<Contest> contests = Concurrent.newList();
```

and `Contest` gains `@Key private transient String id;`. What remains is parsing that key, and that
is three lazy accessors on `Contest` - `getSkyBlockDate()`, `getCollectionName()`, and a private
split - reading the injected key. No sibling state is involved, so they are pure functions of one
field.

**Lazy-able:** yes, once `@Collapse` supplies the key. Note the current design already mutates
`Contest` from outside the class (lines 58-59 write non-final package-visible fields on another
object), which `@Collapse` removes as a side effect: `@Key` injection is the factory's job and the
fields become read-only.

**Blocks:** none. `@Split` cannot help - it splits into exactly two parts (`Split.java:14-19`) and
this key has three segments plus a nested `_` split, so key parsing stays hand-written. That is
fine; it is 4 lines in an accessor rather than 16 lines in a hook.

**Volume:** the probe decoded **810 contests** for one member. Today that is 810 eager
`String.split` pairs, 810 `StringUtil.join` calls and 810 `SkyBlockDate` allocations per member
parse, for data a caller usually filters down to a handful of crops
(`SkyBlock-Simplified/bot/.../SkyBlockUserCommand.java:691` is the only external consumer). Lazy
accessors turn that into per-access work on the few that are read.

**Fragility worth recording:** `dataString[1]` and `calendarString[1]` are unguarded index reads. One
malformed contest key throws `ArrayIndexOutOfBoundsException`, the factory swallows it, and
`contests` silently stays **empty for the whole member** - not just for the bad entry. The stream
form makes a single bad key fatal to all 810.

### 4.5 Bestiary

`response/skyblock/member/Bestiary.java:55-83`:

```java
@Override
public void postInit() {
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
        .map(family -> new Family(
            family.getId(),
            mobs.stream().filter(mob -> mob.getFamily().equals(family)).collect(Concurrent.toUnmodifiableList())
        ))
        .collect(Concurrent.toUnmodifiableList());
}
```

**Classification:** (b) sibling join - `kills` and `deaths` are zipped by key into `Mob` - followed by
(d) an external-repository join, grouping those mobs under every `BestiaryFamily` row. Both halves
are pure functions of `kills`, `deaths` and a static repository.

**Lazy-able:** yes, and it is the *only correct* option. The current form couples deserialization to
`SkyBlockData` availability. `SkyBlockData.getRepository` (`skyblock/.../SkyBlockData.java:50-52`)
delegates to a static `sessionManager`; with no live JPA session it fails, the empty catch eats it,
and `families` stays empty forever - the object cannot recover even once the session comes up. The
probe shows `bestiary families = 0` for a member whose `kills` map holds **1023 entries**.

The module's own test suite already concedes this. `MemberDtoMappingTest.java:42-45` explains that it
decodes subtrees individually because "a whole `SkyBlockMember` runs `postInit` against the SkyBlock
model repositories, which need a live JPA session this test deliberately does not stand up." A
post-bind hook that reaches into a database has made the DTO untestable as a DTO.

**Blocks:** nothing structural, but a naive lazy `getFamilies()` would be expensive - `Family.getType()`
(line 92-95), `getTiers()`, `getBracket()` and `getMaxTier()` each re-query the repository, and
`getUnlocked()`/`getMilestone()` (lines 44-53) walk every family. Memoize the computed list behind
the accessor. That is a real, if small, rewrite - not a one-line change.

**Correctness, filed here, not proposed for annotation:** `MOB_PATTERN` is
`^([a-z_]+)_([0-9]+)$` (line 29). `[a-z_]+` cannot match a digit, so any mob id containing one is
dropped. The fixture contains `master_crypt_undead_flameboy101_45` and
`master_crypt_undead_flameboy101_25` in `deaths` - real keys, silently excluded from every family.
The pattern wants `^(.*)_([0-9]+)$` with a greedy base. Note the parallel to the
`unlocked_coll_tiers` regex owned by `05-cross-field-derivation.md`; same class of defect, different
site, and this one is inside a `postInit()` body so it is filed here.

**Already declarative, for contrast:** `@Lenient` on `kills`/`deaths` (lines 38-41) plus
`@Extract("kills.last_killed_mob")` (line 33) correctly divert the one non-`int` entry the fixture
carries. That pair is the model the rest of this document is arguing for.

### 4.6 SkyBlockMember

`response/skyblock/SkyBlockMember.java:140-156`:

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

Three statements, three different problems.

**Statement 1 - (c) reach-back.** `accessoryBag.initialize(this)` hands the child its parent, the
canonical case `02-parent-access.md` owns. It is also the statement that kills the other two: see
`f01-postinit-aborts-silently`.

**Statement 2 - (c) reach-back, again.** `new Skills(skillExperience, this)` passes the member so
each `SkillLevel` can compute `levelSubtractor` (`SkillLevel.java:21-40`) from
`member.getJacobsContest()` and `member.getCollectionUnlocked()`. Because `skills` is built here and
`collectionUnlocked` three lines later, the `FORAGING` branch reads an **empty map**, so both
`getOrDefault(..., 0) < 9` tests pass and the subtractor is unconditionally 2. That is the ordering
hazard from section 2 made concrete: two derived fields with a real dependency, declared in the wrong
order, with nothing in the language or the library to catch it.

**Statement 3 - (b) sibling join.** `collection` (item id to total collected) joined against
`player_data.unlocked_coll_tiers` (a flat 775-element list of `<itemId>_<tier>` strings), taking the
max tier per item. Semantics and the negative-tier exclusion belong to
`05-cross-field-derivation.md`; the *hook* aspect belongs here.

Its cost is not small. `String.format` and `String.matches` are both inside the inner `filter`, so
each is evaluated per (item, tier) pair - `matches` compiles a fresh `Pattern` every time. The
fixture's populated member has 100 collections and 775 tiers: **77,500 `String.format` calls and
77,500 `Pattern` compilations per member parse**, eagerly, for a map read at exactly two sites
(`SkillLevel.java:32-33`) and one aggregate (`SkyBlockIsland.java:55-58`).

**Lazy-able:** all three, yes.

- `getSkills()` becomes a memoized accessor computing `new Skills(playerData.getSkillExperience(), this)`
  on demand. `this` is available in an instance method, so the reach-back stops being a bind-time
  problem. This also *fixes* the ordering bug for free, because by the time anything calls
  `getSkills()`, `getCollectionUnlocked()` returns real data.
- `getCollectionUnlocked()` becomes a memoized accessor. `PlayerData.getCraftedMinions(String)`
  (`PlayerData.java:55-62`) is the same regex-and-parse idiom already written as a lazy method on the
  same data - the precedent exists in the codebase.
- `accessoryBag` is the hard one, and `02-parent-access.md` owns the mechanism. Note only that the
  same trick applies: the derived accessors could live on `SkyBlockMember`, or take the member as a
  parameter, in which case no reach-back happens during bind at all.

**Blocks:** memoization needs a holder (a `volatile` field plus a null check, or a memoizing
supplier). That is the only genuinely new code any of these six conversions requires.

## 5. Taxonomy

Every statement across the six bodies, against the axes in the brief.

| Axis | Statements | Sites |
| --- | --- | --- |
| (a) reshape one field into another typed field | 3 | `Dungeons.classes`, `JacobsContest.contests`, `CrimsonIsle` x2 (counted once) |
| (b) join two sibling fields | 3 | `Dungeons.dungeons`, `Bestiary` kills+deaths, `SkyBlockMember.collectionUnlocked` |
| (c) needs the enclosing/parent object | 2 | `accessoryBag.initialize(this)`, `new Skills(..., this)` |
| (d) needs an external repository | 1 | `Bestiary.families` |
| (e) pure derivation, lazy-able as-is | 2 | `Election.voting`, `Election.term` |

The distribution is the finding. **Six of eleven statements (a and b) are shape problems, not
computations** - the JSON already contains the value in a layout the field could have declared. Two
of those six are covered by annotations that ship today and were never applied (`@Capture` affix
grouping, `@Collapse` + `@Key`). One (`Dungeons.classes`) needs no annotation at all, only a
correct field type.

Axes (c), (d) and (e) - five statements - are not shape problems, but none of them needs a hook
either. Each is a pure function of state that is fully bound by the time any accessor runs. The only
thing `postInit()` provides that a lazy accessor does not is *eagerness*, and eagerness is precisely
what is producing the four defects in section 7.

**What a `@Derive` annotation would buy here: nothing.** Naming a method in an annotation and having
a factory call it after bind is the same imperative body with an indirection in front of it. It keeps
the eagerness, keeps the swallowed exception, keeps the undeclared ordering, and adds a library
release cycle. `@Aggregate` and `@Bind` fare worse - `@Bind`'s reason to exist is dependency ordering
between derived fields, and the one place ordering matters (`skills` before `collectionUnlocked`)
stops mattering the moment both are lazy. This survey's evidence does not support any of the three
registry entries, and says so plainly.

## 6. Transient-field census

The transient-plus-`postInit` idiom the brief asks about, counted exactly. `response/` declares
**22 transient fields** in 10 files.

**Written by a `postInit()` body, directly or through the chain it drives - 17 of 22 (77%):**

| Field | Site | Written by |
| --- | --- | --- |
| `voting`, `term` | `election/Election.java:19-20` | own body |
| `contents`, `detectedAccessories`, `accessories`, `selectedPowerStats`, `magicalPower`, `logComponent`, `tuningPoints` | `member/AccessoryBag.java:34-53` | `SkyBlockMember.postInit()` via `initialize(this)` |
| `families` | `member/Bestiary.java:42` | own body |
| `searchSettings`, `groupBuilder` | `member/crimson/Kuudra.java:22-23` | `CrimsonIsle.postInit()`, from another class |
| `dungeons`, `classes` | `member/dungeon/Dungeons.java:52-53` | own body |
| `contests` | `member/JacobsContest.java:39` | own body |
| `skills`, `collectionUnlocked` | `skyblock/SkyBlockMember.java:58,130` | own body |

**Not postInit-related - 5 of 22:** `HypixelPlayer.java:45`, `Experimentation.java:45,47,49` (all
`@Capture` targets), `slayer/SlayerBoss.java:23` (a `@Key` target - the declarative pattern, already
working).

**Fields that exist ONLY as raw input to a `postInit()` body - 5.** They share one unmistakable tell:
`@Getter(AccessLevel.NONE)`, because nothing outside the body may see them.

| Field | Site | Sole reader |
| --- | --- | --- |
| `dungeonMap` | `dungeon/Dungeons.java:27-29` | `Dungeons.postInit():57,63` |
| `classMap` | `dungeon/Dungeons.java:30-32` | `Dungeons.postInit():70` |
| `contestMap` | `member/JacobsContest.java:36-38` | `JacobsContest.postInit():48` |
| `kuudra_search_settings` | `crimson/CrimsonIsle.java:38-40` | `CrimsonIsle.postInit():54` |
| `kuudra_group_builder` | `crimson/CrimsonIsle.java:41-43` | `CrimsonIsle.postInit():55` |

Near-misses, deliberately excluded: `SkyBlockMember.collection` (line 129) is read by
`ProfileStats.java:97` and `SkyBlockIsland.java:47`, so it is real data as well as input.
`Bestiary.kills`/`deaths` have public getters and no external callers, but they carry `@Lenient`
overflow for round-trip fidelity, so they must stay bound regardless.

**Net accounting for the conversions in section 7:** 5 input-only fields delete outright, 10 of the
17 transient outputs delete (the 7 `AccessoryBag` ones are `02-parent-access.md`'s call), 89 lines of
`postInit()` bodies delete, and 6 `implements PostInit` declarations delete. `Dungeons` also loses
`EMPTY_DUNGEON`'s reason to exist as a three-argument construction. Nothing in `gson-extras` is
touched.

## 7. Findings

### f01-postinit-aborts-silently

- **Category:** `correctness`
- **Where:** `Simplified-Dev/gson-extras/src/main/java/dev/simplified/gson/factory/PostInitTypeAdapterFactory.java`:35-38
- **Where:** `Simplified-Dev/gson-extras/src/main/java/dev/simplified/gson/PostInit.java`:13-14
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:141-156
- **What:** `postInit()` exceptions are caught and discarded with an empty block, so a body that
  throws on its first statement leaves every later statement unexecuted and reports nothing.
- **Why it is residue:** the hook has no failure channel at all, which is what let three of six
  bodies ship broken. `SkyBlockMember.postInit()` throws on line 142 for **every member**, observed:
  `skills == null` and `collectionUnlocked` empty while `collection` holds 100 entries. Every caller
  of `member.getSkills()` - `ProfileStats.java:66,74,107,571` and `SkyBlockMember.getTotalWeight()`
  at line 186 - dereferences null.
- **Candidate annotation:** none - keep imperative, but the catch must at minimum log, and the
  javadoc claiming it already does must be corrected.
- **Effort:** `small` (one-line library change plus javadoc, one JitPack cycle; consumer fixes are
  the separate findings below)

The library fix and the consumer fix are independent. Fixing the catch turns four silent failures
into four loud ones, which is the point, but it should land *after* the consumer conversions or the
logs will be noise. Note the null `obj` path from section 2 is also swallowed here, so a naive
`throw` would change behavior for JSON nulls - guard `obj != null` first.

### f01-accessorybag-order-inversion

- **Category:** `correctness`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/AccessoryBag.java`:55-57
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/AccessoryBag.java`:128-140
- **What:** `initialize()` reads `this.getContents()` at line 57 but does not assign `contents` from
  the member until line 138, and it computes `calculatedMagicalPower` at line 129 without ever
  storing it into `this.magicalPower`.
- **Why it is residue:** `contents` is `transient` (line 34), so at line 57 it is always the default
  empty `NbtContent`, and `NbtFactory.fromBase64("")` throws - verified directly:
  `NbtException: Truncated NBT input - need 1 bytes at offset 0, only 0 available`. That throw
  propagates out of `initialize()`, out of `SkyBlockMember.postInit()`, into the empty catch. The
  member's real accessory NBT is present and 158,552 base64 characters long. Observed result:
  `accessories = 0, magicalPower = 0`. Even if the read order were fixed, `magicalPower` is a dead
  store, so `tuningPoints` (line 139) and `logComponent` (line 140) would still compute from 0,
  `Math.pow(Math.log(1), 1.2)` is 0, and every value in `selectedPowerStats` (lines 144-153) would be
  multiplied to zero.
- **Candidate annotation:** none - keep imperative; this is a plain sequencing defect.
- **Effort:** `trivial` (one file, consumer only, no library change)

Two independent bugs stacked, either of which alone zeroes magical power. This is the strongest
argument in the pack for the eager-hook pattern being unsafe: the class has no test, the exception is
invisible, and the wrong answer is a plausible-looking `0`.

### f01-dungeons-master-case

- **Category:** `correctness`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:58,63-66
- **What:** the body filters and looks up master-mode floors with an uppercase `MASTER_` prefix, but
  the API's keys are lowercase `catacombs` / `master_catacombs`.
- **Why it is residue:** `filterKey(key -> !key.startsWith("MASTER_"))` excludes nothing, so
  `master_catacombs` survives and `DungeonData.Type.of` maps it to `UNKNOWN`; the paired lookup
  `String.format("MASTER_%s", type.name())` asks for `MASTER_CATACOMBS`, misses, and substitutes
  `new FloorData()`. Observed: `dungeon keys = [UNKNOWN, CATACOMBS]`, `CATACOMBS ... masterTier=0`
  against a fixture where master catacombs has `highest_tier_completed=7`. So master-mode data is
  lost for the real dungeon and re-surfaces under a bogus `UNKNOWN` key that `getWeight()`
  (line 86-94) then folds into the member's total weight. `ProfileStats.java:78,482` defends itself
  by skipping `UNKNOWN`; `SkyBlockMember.getTotalWeight()` does not.
- **Candidate annotation:** `@Capture` (already exists) - see `f01-dungeons-capture-grouping`, which
  removes the hand-rolled prefix matching that caused this.
- **Effort:** `trivial` as a spot fix (lowercase both literals, one file); the structural fix is the
  next finding.

Fixing the case in place is two characters and is worth doing immediately regardless of whether the
`@Capture` conversion is ever planned - it is a data-loss bug in a live path.

### f01-dungeons-capture-grouping

- **Category:** `postinit-elimination`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/Dungeons.java`:27-32,52-76
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonData.java`:18-22
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/dungeon/DungeonClass.java`:17-19
- **What:** a 20-line `postInit()` rebuilds two maps that the existing annotations bind directly -
  `classes` needs no annotation at all, and `dungeons` is exactly `@Capture` prefix grouping.
- **Why it is residue:** `classMap` funnels through `ConcurrentMap<Type, ConcurrentMap<String, Double>>`
  only to read `["experience"]` out of a value class that *has a single field named `experience`*.
  `dungeonMap` hand-rolls the base-key-plus-prefix join that `CaptureTypeAdapterFactory.java:432-455`
  implements generically, documented at `Capture.java:37-47` and `64-76`.
- **Candidate annotation:** `@Capture` (already exists, no library change)
- **Effort:** `small` (3 consumer files - `Dungeons`, `DungeonData`, `DungeonClass` - no library
  change, no JitPack cycle; rated above `trivial` because `DungeonData` changes from
  `@RequiredArgsConstructor` final fields to a bound shape and the `EMPTY_DUNGEON` sentinel at
  line 25 must be re-expressed)

Payoff: `postInit()` and `implements PostInit` gone, `dungeonMap` and `classMap` gone (2 of the 5
input-only fields), `dungeons` and `classes` stop being transient, 20 lines deleted, and
`f01-dungeons-master-case` becomes unreachable because the factory does the affix matching.

Risk, stated plainly: the `descend = true` plus empty-`filter` combination is not exercised by the
documented examples, and the "one catch-all per class" rule may interact with it. Verify with a test
against `gson-extras` before planning. If it turns out unsupported, the fallback is
`@Capture(filter = "^(master_)?")`-style filtering, which is uglier and should lower this finding's
rank rather than justify a library change.

### f01-jacobscontest-collapse-key

- **Category:** `postinit-elimination`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/JacobsContest.java`:36-39,46-63
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/JacobsContest.java`:108-109
- **What:** a 16-line `postInit()` converts a keyed map into a list and writes two fields on each
  value from the map key - which is the documented job of `@Collapse` + `@Key`.
- **Why it is residue:** `Collapse.java:37-42` specifies the list form exactly: a JSON object becomes
  a `List<V>` with each entry's key injected into the value's `@Key` field, and serialization is
  reversible. The only part the annotation does not cover is decomposing `99:6_27:PUMPKIN` into
  year/month/day/collection, and that is a pure function of the injected key, so it belongs in
  accessors on `Contest`, not in the enclosing class's hook.
- **Candidate annotation:** `@Collapse` + `@Key` (already exist, no library change)
- **Effort:** `small` (2 consumer files, no library change; `Contest.skyBlockDate` and
  `collectionName` at lines 108-109 stop being externally-written mutable fields)

Payoff: `postInit()` and `implements PostInit` gone, `contestMap` gone (1 of the 5 input-only
fields), `contests` stops being transient, 16 lines deleted, and 810 eager `SkyBlockDate`
allocations per member parse become on-demand. It also removes cross-object field mutation - lines
58-59 currently assign into another class's instance from outside it.

Secondary benefit worth naming: today one malformed contest key throws inside the stream and, via the
empty catch, empties all 810 contests. Per-`Contest` lazy parsing confines a bad key to that one
contest.

### f01-skyblockmember-lazy-skills

- **Category:** `postinit-elimination`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:58,130,141-156
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/skill/SkillLevel.java`:21-40
- **What:** `skills` is constructed before `collectionUnlocked` is assigned, but `SkillLevel`'s
  constructor reads `member.getCollectionUnlocked()` - so the `FORAGING` level subtractor is computed
  against an empty map every time.
- **Why it is residue:** two derived fields with a genuine dependency, ordered by nothing but source
  position, in a hook with no ordering contract. Even with the abort in
  `f01-postinit-aborts-silently` fixed, `SkillLevel.calcLevelSubtractor` would still see an empty map
  and unconditionally add 2 to the subtractor for `FORAGING`. Converting both to memoized accessors
  removes the ordering question entirely rather than answering it.
- **Candidate annotation:** none - keep imperative, as a lazy accessor
- **Effort:** `small` (2 consumer files plus a memoization holder; no library change. Rated above
  `trivial` because `skills` is currently nullable and callers may rely on the eager field)

Payoff: 15 lines deleted, `implements PostInit` gone from the module's largest DTO, 2 transient
fields become computed accessors, the ordering bug is structurally eliminated, and the eager
77,500 `Pattern` compilations per member parse (section 4.6) become per-access work at the two sites
that read the map.

Deferred by ownership: the join's semantics and the negative-tier regex exclusion belong to
`05-cross-field-derivation.md`; `accessoryBag.initialize(this)` belongs to `02-parent-access.md`.
This finding is only about the two statements that can move to accessors without solving reach-back.

### f01-bestiary-lazy-families

- **Category:** `postinit-elimination`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/Bestiary.java`:29,42,55-83
- **Where:** `src/test/java/api/simplified/hypixel/response/skyblock/MemberDtoMappingTest.java`:42-45
- **What:** `postInit()` queries the `SkyBlockData` JPA repository during deserialization, so
  `families` is permanently empty whenever no session is live when the JSON is parsed.
- **Why it is residue:** binding and domain resolution are different concerns bolted together by the
  hook. The failure is unrecoverable - once the empty list is assigned, a session coming up later
  changes nothing - and it is invisible, because the exception goes into the empty catch. The module's
  own test suite documents the workaround: it refuses to decode a whole `SkyBlockMember` for exactly
  this reason. Observed with no session: `bestiary families = 0` for a member with 1023 `kills`
  entries.
- **Candidate annotation:** none - keep imperative, as a memoized lazy accessor
- **Effort:** `small` (1 consumer file plus memoization; no library change. Not `trivial` because
  `Family.getType`/`getTiers`/`getBracket`/`getMaxTier` at lines 92-121 each re-query the repository,
  so an unmemoized accessor would be markedly slower than today)

Payoff: 27 lines deleted, `implements PostInit` gone, 1 transient field becomes an accessor,
`Bestiary` becomes decodable in a plain unit test, and the repository dependency moves to the point
of use where a missing session is a recoverable, observable error.

Carried defect, not fixed by the conversion: `MOB_PATTERN` at line 29 is `^([a-z_]+)_([0-9]+)$` and
silently drops any mob id containing a digit - `master_crypt_undead_flameboy101_45` and
`master_crypt_undead_flameboy101_25` are present in the fixture's `deaths` map and never reach a
family. Fix the pattern in the same change.

### f01-election-lazy-cycles

- **Category:** `postinit-elimination`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/election/Election.java`:19-25,43-53
- **What:** two transient fields are computed by arithmetic on a single `int` field, in a hook, with
  a duplicate invocation from the constructor.
- **Why it is residue:** there is no JSON shape involved and no sibling state - `voting` and `term`
  are functions of `year` alone. This is the pure case: the hook exists only to make the values eager.
- **Candidate annotation:** none - keep imperative, as computed accessors
- **Effort:** `trivial` (1 consumer file, no library change)

Payoff: 9 lines of body plus the constructor's `this.postInit()` call deleted, 2 transient fields
deleted, `implements PostInit` deleted, and `SpecialElection`/`VotingBooth` inherit the result with
no edit. `equals`, `hashCode` and `toString` already call `getVoting()`/`getTerm()`, so they are
unaffected.

Risk: none identified. Each accessor call allocates a fresh `Cycle`, so identity comparison on the
result would change behavior - nothing does that, `Cycle` has no `equals` and is compared only
through `Objects.equals` on the enclosing `Election`, which would then compare non-equal `Cycle`
instances by identity. That is a real trap: either memoize, or give `Cycle` an `equals`. Prefer
memoizing, since `Election.equals` is used.

### f01-crimsonisle-field-copy

- **Category:** `value-shape-collapse`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/CrimsonIsle.java`:38-43,52-56
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/Kuudra.java`:22-23
- **What:** two `@SerializedPath` fields on `CrimsonIsle` are copied down into package-private
  transient fields on `Kuudra` purely so a caller can reach them through `getKuudra()`.
- **Why it is residue:** the JSON puts Kuudra completion tiers and Kuudra party-finder settings under
  two unrelated top-level keys; the Java shape wants one object, so the hook stitches them. Nothing
  outside `CrimsonIsle` reads `Kuudra.getSearchSettings()` or `getGroupBuilder()` - a workspace-wide
  symbol search returns no callers - so the stitching serves no consumer.
- **Candidate annotation:** none - keep imperative, or rather delete it; a target-path annotation for
  one two-line site is not worth a library cycle
- **Effort:** `trivial` (2 consumer files, no library change)

Payoff: 2 lines of body deleted, `implements PostInit` deleted, 2 input-only fields promoted to real
named fields, 2 transient fields on `Kuudra` deleted, and `Kuudra`'s two package-private fields
regain `private`. Also clears a `naming` defect - `kuudra_search_settings` and `kuudra_group_builder`
are snake_case Java identifiers.

Rank this last. It is correct today, it costs nothing at runtime, and it is the only one of the six
whose removal delivers no bug fix and no measurable saving. It is worth doing only as part of a sweep
that is already touching `implements PostInit` declarations.

## 8. Verdict

**All six `postInit()` bodies in this module can go, and not one of them needs a new annotation.**

That is not the answer the pack's framing anticipated, so it is worth stating precisely why. The
question posed was "what fraction of `postInit` usage could be eliminated by new annotations". The
honest answer is **zero percent by new annotations, one hundred percent by two annotations that
already ship and a lazy-accessor idiom**.

| Implementor | Retired by | New library work |
| --- | --- | --- |
| `Dungeons` | `@Capture` affix grouping + a corrected field type | none |
| `JacobsContest` | `@Collapse` + `@Key` + key-parsing accessors | none |
| `Bestiary` | memoized lazy accessor | none |
| `SkyBlockMember` | memoized lazy accessors (2 of 3 statements) | none |
| `Election` | computed accessors | none |
| `CrimsonIsle` | delete the copy, relocate two fields | none |

Two of six are shape problems the existing annotation set already solves and that were simply never
applied. Four of six are eagerness, and eagerness is not something an annotation improves - naming
the computation in a `@Derive` element keeps every property that makes the current form bad
(swallowed exceptions, undeclared ordering, parse-time repository coupling, unconditional cost) and
adds a `gson-extras` release cycle on top. **This survey recommends against `@Derive`, `@Aggregate`
and `@Bind` on the evidence it collected**, and notes that the one site where inter-field ordering
matters - `skills` before `collectionUnlocked` - stops existing under lazy accessors, which removes
`@Bind`'s entire justification.

**The irreducible core.** Within `response/`, after the nine findings above, it is empty: zero
`implements PostInit` declarations remain. Three things survive and must not be confused with it.

- **The reach-back into `SkyBlockMember`.** `accessoryBag.initialize(this)` is real and
  `02-parent-access.md` owns it. It is not irreducibly a *post-bind* problem though - the derived
  accessors can take the member as a parameter or live on the member, in which case the child never
  needs a parent reference at bind time at all. Whether that is the right design is 02's call.
- **The `PostInit` interface itself.** `JpaRepository.java:255-256` calls it manually on entities and
  `skyblockdata/date/Election.java:14` implements it in a sibling module. The interface and the
  factory stay; only hypixel's use of them goes.
- **Key parsing.** Decomposing `99:6_27:PUMPKIN` and `<itemId>_<tier>` stays hand-written Java.
  `@Split` handles two-part strings only. A `@Tier`-style annotation is `05-cross-field-derivation.md`'s
  to argue for; from this survey's angle it would move four lines out of an accessor and is not
  justified by `postInit` evidence alone.

**Sequencing implication for `20-implementation-plan.md`.** Every finding here is consumer-only and
independently revertable, so this survey contributes no JitPack boundary except the optional one-line
logging fix in `f01-postinit-aborts-silently`. Do the four correctness findings first - they are
live data-loss bugs, they are cheap, and fixing them before the structural conversions means the
conversions can be verified against known-good expected values instead of against today's silently
wrong output.

**Recommended order.** `f01-accessorybag-order-inversion` and `f01-dungeons-master-case` (both
`trivial`, both fix data loss), then `f01-election-lazy-cycles` and `f01-crimsonisle-field-copy`
(both `trivial`, both prove the accessor pattern on the two simplest classes), then
`f01-jacobscontest-collapse-key`, `f01-dungeons-capture-grouping`, `f01-bestiary-lazy-families` and
`f01-skyblockmember-lazy-skills` (all `small`), and last the library-side
`f01-postinit-aborts-silently`, once there is nothing left in this module for it to shout about.
