# 06 - Structural Duplication, Dead Weight and Inconsistency

## 1. Scope and method

This survey owns the residue the other five do not: the same shape re-typed across sibling classes,
fields that no caller can reach, one upstream concept modelled two or three different ways, and
latent defects that surfaced while inventorying those things. Nested-holder forwarders, single-field
value classes, `postInit()` bodies and `initialize(parent)` reach-back belong to siblings and are
not re-reported here.

Method, in order of evidence strength:

1. **Executed probes.** Four claims below were proved by decoding real JSON through
   `GsonSettings.defaults().create()` in a throwaway JUnit test in this module, printing the bound
   values, and deleting the test afterwards. Those findings carry the literal probe output. Nothing
   was left behind in `src/test`.
2. **Fixture shape reads.** `src/main/resources/craftedfury.json` was queried with python for key
   vocabularies, value ranges and object shapes - never dumped.
3. **Whole-package inventories.** Field-level scans over all 133 files for temporal types, `Optional`
   conventions, constructor annotations, `@Getter(AccessLevel.NONE)` suppression, `transient`, and
   camel-case fields lacking a serialized name.
4. **Coverage.** `scripts/json_dto_diff.py` reports **792 unmapped keys, every one of them under
   `SkyBlockMember.objectives`**. Outside that node the DTO tree covers the fixture completely, so
   "dead JSON" is not a problem here - dead *Java* is.

Range checks were run for the `int`-versus-`long` question: every fixture value above
`Integer.MAX_VALUE` was extracted and mapped back to its declared field. The wide counters are
already wide - `SkyBlockMember.collection` is `ConcurrentMap<String, Long>`,
`Statistics.Auctions.goldEarned/goldSpent/goldFees` are `long`, the chocolate counters are `long`,
and every dungeon damage figure is `double`. **No int-overflow finding is raised**; that hypothesis
was tested and did not hold.

## 2. Findings index

| ID | Category | Effort | One line |
| --- | --- | --- | --- |
| `f06-hoppity-unreadable-fields` | `correctness` | `trivial` | 3 hoppity classes have no `@Getter`; 13 bound fields are unreachable |
| `f06-enum-null-clobber` | `correctness` | `medium` | An unknown enum value writes `null` over a `@NotNull` default - proved |
| `f06-boardquest-complete-status` | `correctness` | `trivial` | Upstream says `COMPLETE`, the enum says `COMPLETED`, result is `null` - proved |
| `f06-capture-null-enum-key` | `correctness` | `medium` | `@Capture` into an enum-keyed map inserts a `null` key - proved |
| `f06-serialized-name-misses` | `correctness` | `trivial` | `@SerializedName("starting_big")` is a typo; `CommissionData.totalCompleted` has no key |
| `f06-completedat-zero-sentinel` | `correctness` | `medium` | `completed_at: 0` binds to `Optional.of(1970-01-01)`, so `Optional` never signals absence - proved |
| `f06-jacobscontest-derived-nontransient` | `correctness` | `trivial` | Two `postInit`-derived fields are not `transient` and leak into serialize |
| `f06-temporal-type-split` | `duplication` | `medium` | One upstream concept (epoch millis) is modelled four different ways across 60 fields |
| `f06-crimson-npc-quest-family` | `duplication` | `small` | `NpcQuest`/`SuusQuest`/`MollimQuest` are the same class three times |
| `f06-trophyfish-tier-columns` | `duplication` | `small` | `TierData` spells a 4-constant enum out as 4 `int` columns |
| `f06-daily-effect-twin` | `duplication` | `trivial` | The identical `current_daily_effect` pair is modelled twice with different names |
| `f06-objective-status-shape` | `duplication` | `small` | 792 unmapped `objectives.*` entries have `BoardQuest`'s exact shape |
| `f06-noargs-constructor-drift` | `duplication` | `trivial` | Four constructor conventions for one need, none of which changes behaviour |
| `f06-rift-location-classes` | `duplication` | `medium` | The rift location classes look collapsible and are not - argued down |

## 3. Correctness findings

### f06-hoppity-unreadable-fields

- **Category:** `correctness`
- **Where:** `response/skyblock/member/hoppity/ChocolateShop.java`:8
- **Where:** `response/skyblock/member/hoppity/ChocolateTimeTower.java`:7
- **Where:** `response/skyblock/member/hoppity/RabbitHitman.java`:7
- **Where:** `response/hypixel/HypixelPlayer.java`:77
- **Where:** `response/hypixel/HypixelSocial.java`:10
- **What:** three hoppity DTO classes carry no `@Getter` and declare no accessor, so all thirteen of
  their fields deserialize successfully and can never be read.
- **Why it is residue:** the class body is Lombok-shaped in every respect except the one annotation
  that makes it useful. `ChocolateFactory` publicly exposes `getShop()`, `getTimeTower()` and
  `getHitman()`, so a caller reaches an object whose entire contents are private with no accessor.
- **Candidate annotation:** none - keep imperative
- **Effort:** `trivial`

Every other class in the package is annotated. A scan for class declarations without a preceding
`@Getter` returns exactly five hits, and only these three are unintentional:

| Site | Fields stranded | Verdict |
| --- | --- | --- |
| `ChocolateShop`:8 | 5 (`year`, `rabbits`, `chocolateSpent`, `chocolateFortune`, `rabbitsPurchased`) | missing `@Getter` |
| `ChocolateTimeTower`:7 | 4 (`charges`, `activationTime`, `level`, `lastChargeTime`) | missing `@Getter` |
| `RabbitHitman`:7 | 4 (`slots`, `missedUncollectedEggs`, `eggSlotCooldown`, `eggSlotCooldownSum`) | missing `@Getter` |
| `HypixelSocial`:8 | 1 (`prompt`) | deliberate - `links` carries a field-level `@Getter` |
| `HypixelPlayer.Stats.SkyBlock`:132 | 1 (`profiles`) | deliberate - a hand-written `getProfiles()` reads it |

A workspace-wide grep for `ChocolateShop`, `ChocolateTimeTower`, `RabbitHitman`, `getTimeTower` and
`getHitman` matches only the four hoppity files themselves - nothing consumes them today, which is
consistent with "nobody noticed the data was unreachable".

Two genuinely dead fields turned up in the same scan and are folded in here rather than given their
own finding, because both are one-line deletions:

- `HypixelPlayer.java`:77 `mostRecentMonthlyPackageRank` - `@Getter(AccessLevel.NONE)` and zero
  references anywhere in the file or the workspace. It is the only one of the ten suppressed
  `HypixelPlayer` rank fields that `getRank()` never consults. Serialization keeps a round-trip use,
  so deleting it is a *behaviour* change if anyone re-serializes a `HypixelPlayer`; nobody does.
  Low confidence that removal is desired, high confidence that no code reads it.
- `HypixelSocial.java`:10 `prompt` - same shape, and here the suppression is clearly deliberate.
  Listed for completeness, not proposed for deletion.

**Proposed change:** add `@Getter` to the three hoppity classes. **Payoff:** 13 fields become
readable; 0 lines deleted, 3 lines added. **Risk:** none - purely additive. The reason this is
`trivial` rather than free is that it changes a public API surface, so any downstream module that
was working around the gap needs a glance.

### f06-enum-null-clobber

- **Category:** `correctness`
- **Where:** `gson-extras/factory/CaseInsensitiveEnumTypeAdapterFactory.java`:82
- **Where:** `response/skyblock/member/crimson/CrimsonIsle.java`:27
- **Where:** `response/skyblock/garden/ActiveCommission.java`:16
- **Where:** `response/skyblock/island/Banking.java`:24
- **Where:** `response/skyblock/member/dungeon/DungeonChest.java`:20
- **What:** `CaseInsensitiveEnumTypeAdapter.read` returns `null` for any value it does not recognise,
  and Gson's reflective binder assigns that `null` over the field initializer, so a
  `@NotNull` enum field with a declared default becomes `null` the first time upstream adds a
  constant.
- **Why it is residue:** every one of the 14 sites wrote a default (`Faction.NONE`,
  `Type.UNKNOWN`, `Rarity.COMMON`, `Status.UNKNOWN`) precisely to express "unknown value here". The
  default is never reached, because the adapter overwrites it rather than declining to write.
- **Candidate annotation:** `@Fallback` (or a null-declining read in the existing factory)
- **Effort:** `medium`

Proved, not inferred. A throwaway test decoded `{"selected_faction":"cultists"}` into `CrimsonIsle`
and asserted `getSelectedFaction()` is `null`; the assertion passed:

```
CrimsonIsle isle = gson.fromJson("{\"selected_faction\":\"cultists\"}", CrimsonIsle.class);
assertThat(isle.getSelectedFaction(), is(nullValue()));   // passes
```

The mechanism is two lines in two projects. `CaseInsensitiveEnumTypeAdapterFactory`:82 is
`return nameToConstant.get(in.nextString().toUpperCase());` - a plain map miss yields `null`. Gson
2.11's reflective `BoundField` then does `if (fieldValue != null || !isPrimitive) field.set(...)`,
and an enum is not primitive, so the `null` lands.

Fourteen `@NotNull` enum fields carry a default and are exposed to this:

```
ActiveCommission.java:16        Status status = Status.NOT_STARTED
BoardQuest.java:15              Status status = Status.UNKNOWN
CrimsonIsle.java:27             Faction selectedFaction = Faction.NONE
Kuudra.java:40                  Kuudra.Tier tier = Kuudra.Tier.BASIC
Kuudra.java:42                  SearchSettings.Sort sort = Sort.RECENTLY_CREATED
DungeonRun.java:24              DungeonData.Type dungeonType = Type.UNKNOWN
Dungeons.java:39                DungeonClass.Type selectedClass = Type.UNKNOWN
FloorData.java:109              DungeonClass.Type dungeonClass = Type.UNKNOWN
ChocolateFactory.java:30        RabbitSort rabbitSort = RabbitSort.A_TO_Z
ChocolateFactory.java:32        RabbitFilter rabbitFilter = RabbitFilter.NONE
Crystal.java:10                 State state = State.NOT_FOUND
OwnedPet.java:32                Rarity baseRarity = Rarity.COMMON
SkyBlockAuction.java:40         Rarity rarity = Rarity.COMMON
SkyBlockIsland.java:34          GameMode gameMode = GameMode.CLASSIC
```

Three more enum fields have no default at all and are therefore *already* `null` whenever upstream
sends something new: `Banking.Action`:24, `CommunityUpgrades.Type`:59, `DungeonChest.Type`:20.

The sharpest instance is `ActiveCommission.Status`:34, an enum whose entire body is one constant,
`NOT_STARTED`. Any commission that is in progress or claimed binds to `null` on a field annotated
`@NotNull`.

**Proposed change:** two options, and they are not equivalent.

*Library fix, `medium`* - make the adapter decline rather than null: if the incoming name is not
recognised, do not write the field at all, leaving the initializer intact. Gson has no "skip this
field" hook in `TypeAdapter.read`, so this has to be expressed as "return a sentinel the reflective
binder ignores", which it cannot do - the honest library fix is to have the factory look up a
per-enum fallback constant (a constant named `UNKNOWN`/`NONE`, or one marked by a new annotation)
and return that. That is a behaviour change in a factory registered in `GsonSettings.defaults()`, so
every consumer of gson-extras is in the blast radius: `medium` by the effort scale, and it needs a
regression pass over the other modules' enums.

*Consumer fix, `trivial` per site but `medium` across the package* - drop `@NotNull`, accept the
`null`, and route reads through a `getXOrDefault()`. This is worse: 17 sites, and it moves the
problem into every caller.

The library fix is the right one, and the naming registry's `@Fallback` row is the natural home for
"supply this constant when the value fails to bind".

**Payoff:** removes a class of NPE that only appears when Hypixel ships a content update, which is
the worst possible time to discover it. **Risk:** any consumer currently *relying* on `null` to
detect an unrecognised value would silently start seeing the fallback constant. Grep before landing.

### f06-boardquest-complete-status

- **Category:** `correctness`
- **Where:** `response/skyblock/member/crimson/BoardQuest.java`:20
- **What:** `BoardQuest.Status` declares `COMPLETED`, but the Hypixel status vocabulary in the
  fixture is `COMPLETE`, so a finished board quest binds to `null`.
- **Why it is residue:** the constant name was guessed rather than read off the wire. Combined with
  `f06-enum-null-clobber` it turns the *most common* state of the object into a `null` on a
  `@NotNull` field.
- **Candidate annotation:** none - keep imperative (a `@SerializedName` on the constant fixes it)
- **Effort:** `trivial`

Proved. The same throwaway probe decoded both spellings through the real `Gson`:

```
PROBE status(COMPLETE)  = null
PROBE status(COMPLETED) = COMPLETED
```

The fixture's status vocabulary, counted across every `objectives` entry of every member, is
`COMPLETE` x791, `ACTIVE` x19, `INACTIVE` x1. `COMPLETED` does not occur anywhere in the document.
The `nether_island_player_data.quests.quest_data` node - the node `BoardQuest` actually binds - only
happens to contain `ACTIVE` x5 in this particular fixture, which is why the defect has stayed
invisible. `INACTIVE` is also unmodelled, so it binds to `null` too.

**Proposed change:** in `BoardQuest.Status`, rename `COMPLETED` to `COMPLETE` (or keep the Java name
and add `@SerializedName("COMPLETE")`), and add `INACTIVE`. Three lines.

```java
public enum Status {

    UNKNOWN,
    INACTIVE,
    ACTIVE,
    COMPLETE

}
```

**Payoff:** the five `BoardQuest` fields on `QuestBoard` stop returning `null` once any of them is
finished. **Risk:** `getStatus() == Status.COMPLETED` call sites break at compile time, which is the
desired failure mode. **Confidence:** the `COMPLETE`/`INACTIVE` vocabulary is read directly from the
fixture; that the *quest board* uses the same vocabulary as *objectives* is inference, but both
nodes share the identical `{status, progress, completed_at}` triple (see
`f06-objective-status-shape`), which makes a separate vocabulary unlikely.

### f06-capture-null-enum-key

- **Category:** `correctness`
- **Where:** `response/skyblock/member/crimson/Kuudra.java`:19
- **Where:** `response/skyblock/member/crimson/Kuudra.java`:21
- **Where:** `response/skyblock/member/crimson/Dojo.java`:16
- **Where:** `response/skyblock/member/crimson/Dojo.java`:18
- **Where:** `response/skyblock/member/mining/HeartOfTheMountain.java`:50
- **Where:** `response/skyblock/member/crimson/TrophyFishing.java`:25
- **Where:** `response/skyblock/member/Statistics.java`:89
- **What:** an enum-keyed `@Capture` map inserts a `null` key when the captured JSON key does not
  match any enum constant, rather than dropping the entry.
- **Why it is residue:** `@Capture` exists to absorb an open key space. Every one of these seven
  sites narrows that open space to a closed enum, and the narrowing has no failure policy.
- **Candidate annotation:** extend `@Capture` with a skip-unmatched-key policy, or reuse the
  `@Fallback` decision from `f06-enum-null-clobber`
- **Effort:** `medium`

Proved. Decoding `{"none":1,"brand_new_tier":4}` into `Kuudra` produced:

```
PROBE kuudra tiers = {null=4, BASIC=1}
```

No exception is thrown. The map is silently poisoned: `ConcurrentMap` accepts the entry, iteration
over `keySet()` reaches a `null`, and anything shaped like
`map.keySet().stream().map(Enum::name)` throws far away from the deserialize call that caused it.
`Dojo.Type` and `DungeonData.Type` both ship a hand-written `of(String)` helper that falls back to
`UNKNOWN` - evidence that the authors knew unmatched names happen - but neither helper is on the
deserialize path, only the adapter is.

The seven exposed sites and their key spaces:

| Site | Key enum | Key space |
| --- | --- | --- |
| `Kuudra`:19 | `Kuudra.Tier` | `highest_wave_<tier>`, 5 constants |
| `Kuudra`:21 | `Kuudra.Tier` | catch-all over `kuudra_completed_tiers`, 5 constants |
| `Dojo`:16 | `Dojo.Type` | `dojo_points_<type>`, 8 constants incl. `UNKNOWN` |
| `Dojo`:18 | `Dojo.Type` | `dojo_time_<type>`, 8 constants incl. `UNKNOWN` |
| `HeartOfTheMountain`:50 | `Powder.Type` | `powder_<type>`, 3 constants |
| `TrophyFishing`:25 | `TrophyFish` | catch-all, 18 constants - the one most likely to grow |
| `Statistics`:89 and 91-95 | `Mythos.Type` | `burrows_*`, 6 constants |

`TrophyFishing` is the live risk: Hypixel adds trophy fish, the enum lists 18 including three
`OBFUSCATED_FISH_n` placeholders, and any nineteenth fish lands under a `null` key.

**Proposed change:** the same decision as `f06-enum-null-clobber` covers both. If the enum adapter
gains a fallback constant, these maps collapse unmatched keys onto `UNKNOWN` instead of `null` -
which for `Dojo.Type` is exactly what the existing `UNKNOWN("")` constant was written for. If
instead `@Capture` grows a `skipUnmatchedKeys` element, the entry is dropped and round-trip fidelity
is lost, so the fallback route is preferable. **Payoff:** removes a `null` key from seven maps.
**Risk:** collapsing several unmatched keys onto one fallback constant means later entries overwrite
earlier ones; that is a real information loss and the design document has to weigh it against a
`null` key.

### f06-serialized-name-misses

- **Category:** `correctness`
- **Where:** `response/skyblock/SkyBlockAuction.java`:41
- **Where:** `response/skyblock/garden/CommissionData.java`:16
- **What:** `SkyBlockAuction.startingBid` is annotated `@SerializedName("starting_big")` - a typo for
  `starting_bid` - so every auction's starting bid binds to `0`; `CommissionData.totalCompleted`
  carries no serialized name at all while the upstream key is snake-case.
- **Why it is residue:** neither endpoint appears in the bundled fixture, so
  `scripts/json_dto_diff.py` cannot see them. These are the two places where the differ's coverage
  guarantee does not reach.
- **Candidate annotation:** none - keep imperative
- **Effort:** `trivial`

`starting_big` is confirmed: a grep for both spellings across the whole module matches exactly one
line, `SkyBlockAuction.java:41`. There is no compensating alternate name, no `@Alias`, and no
post-bind repair. `getStartingBid()` returns `0` for every auction the API has ever returned. The
neighbouring `highest_bid_amount` is spelled correctly, which is what hides it - a caller comparing
the two sees a plausible-looking pair.

```java
@SerializedName("starting_bid")
private long startingBid;
```

`CommissionData.totalCompleted` is the weaker of the two claims and is marked as such.
`GsonSettings` sets **no** `FieldNamingPolicy` (verified - no `setFieldNamingPolicy` call anywhere in
the file), so an unannotated field binds to its exact Java name. A package-wide scan for camel-case
fields under `skyblock/` with no serialized-name annotation returns 21 hits, and 20 of them are
correct because their endpoints genuinely emit camel-case: the auctions envelope
(`totalPages`, `totalAuctions`, `lastUpdated`), the bazaar product block (`sellPrice`, `buyVolume`,
`sellMovingWeek`, ...), and the pets array (`candyUsed`, `heldItem`). `CommissionData` is the lone
outlier - it sits inside the garden endpoint, whose sibling fields in the very same class
(`unique_npcs_served`) are snake-case. Confidence: high that the file is inconsistent, medium that
the upstream key is `total_completed`. Resolvable in seconds against a live `/skyblock/garden`
response; do that before landing.

**Payoff:** one field starts carrying data, one probably does. **Risk:** none for the auction fix.

### f06-completedat-zero-sentinel

- **Category:** `correctness`
- **Where:** `response/skyblock/member/crimson/BoardQuest.java`:18
- **Where:** `response/skyblock/member/crimson/Abiphone.java`:56
- **Where:** `response/skyblock/member/crimson/Abiphone.java`:58
- **Where:** `response/skyblock/member/foraging/MelodyHarp.java`:22
- **Where:** `response/skyblock/member/Experimentation.java`:38
- **What:** upstream writes `0` for "has not happened yet", and an `Optional<SkyBlockDate.RealTime>`
  field binds that to `Optional.of(1970-01-01)`, so `isPresent()` is always `true` and the
  `Optional` conveys nothing.
- **Why it is residue:** the `Optional` was chosen to model absence. Absence upstream is a zero, not
  a missing key, and nothing translates between the two.
- **Candidate annotation:** `@Fallback` (treat a nominated value as absent), or drop the `Optional`
- **Effort:** `medium`

Proved:

```
BoardQuest q = gson.fromJson("{\"status\":\"COMPLETE\",\"progress\":3,\"completed_at\":0}", ...);
PROBE completedAt(0) = Optional[1969-12-31T19:00:00]
```

The fixture confirms `0` is the real encoding: all five `quest_data` entries carry
`"completed_at": 0` while their status is `ACTIVE`. Two of the three `MelodyHarp`/`Experimentation`
sites go further and *pre-seed* the epoch as a non-`Optional` default -
`= new SkyBlockDate.RealTime(0)` - which is the same sentinel written by hand, so the package
already contains both halves of the idea without connecting them.

This is one slice of a wider convention split. The package holds 55 `Optional` DTO fields:

| Shape | Count | Reading |
| --- | --- | --- |
| `@NotNull Optional<X> f = Optional.empty()` | 44 | the intended house form |
| `Optional<X> f = Optional.empty()` (no `@NotNull`) | 9 | same behaviour, annotation forgotten |
| `@NotNull Optional<X>` with no initializer | 1 | `TrophyFish.zoneId`, a `final` enum component - fine |

The nine unannotated ones are `HypixelPlayerResponse`:11, `HypixelStatus`:25 and :26,
`Experimentation`:19 and :23, `Kuudra`:66, `HeartOfTheForest`:19, `HeartOfTheMountain`:22 and :45.
Purely cosmetic - `OptionalTypeAdapterFactory` never yields a bare `null` - so no separate finding.

The *object* convention, by contrast, is admirably uniform: 152 nested-object fields use the
`= new X()` default-instance form and only 7 are left nullable, of which 4 are enums already covered
by `f06-enum-null-clobber` and 3 are `HypixelPlayer` sub-objects. **There is no default-instance
versus nullable inconsistency worth reporting.** The inconsistency is entirely in the temporal and
`Optional` layers.

**Proposed change:** the cheap consumer-only version is to stop pretending - declare
`BoardQuest.completedAt` as `@NotNull SkyBlockDate.RealTime` with an epoch default and let callers
test it, which is `trivial` and honest. The declarative version is a `@Fallback`-style element that
nominates `0` as the absent marker so the `Optional` becomes truthful; that is a library change and
therefore `medium`. Prefer the honest `trivial` fix unless the design document finds the same
zero-means-absent pattern in enough other places to justify the annotation - it found five here.

### f06-jacobscontest-derived-nontransient

- **Category:** `correctness`
- **Where:** `response/skyblock/member/JacobsContest.java`:108
- **Where:** `response/skyblock/member/JacobsContest.java`:109
- **Where:** `response/skyblock/member/crimson/Kuudra.java`:22
- **Where:** `response/skyblock/member/crimson/Kuudra.java`:23
- **What:** `Contest.skyBlockDate` and `Contest.collectionName` are assigned by
  `JacobsContest.postInit()` but are not declared `transient`, so Gson both tries to bind them on
  read and emits them on write.
- **Why it is residue:** the enclosing class already gets this right - `JacobsContest.contests`:39,
  the collection built by the same `postInit()` body, *is* `transient`. Two fields on the nested
  class were missed.
- **Candidate annotation:** `@Derive` would make the distinction structural rather than a modifier
  the author must remember
- **Effort:** `trivial`

Three consequences, in descending severity:

1. **Round-trip pollution.** Serializing a `SkyBlockMember` emits `skyBlockDate` and
   `collectionName` keys that Hypixel never sent. `SkyBlockDate` has no registered type adapter, so
   it serializes by reflection over whatever fields that class holds.
2. **Clobber-on-read.** If a re-read of that emitted document happens, the reflective binder writes
   the two keys back before `postInit()` runs, then `postInit()` overwrites them. Harmless today,
   but it is the pattern that makes derived state non-idempotent.
3. **Convention drift.** The package has 22 `transient` fields and this is the one place the
   modifier is missing on a `postInit`-assigned field.

`Kuudra`:22-23 is a milder instance of the same drift - `searchSettings` and `groupBuilder` are
correctly `transient` but are declared **package-private**, the only two DTO fields in the package
with no access modifier, because `CrimsonIsle.postInit()` writes them across class boundaries within
the same package. That is a `parent-access`-shaped workaround and belongs to `02-parent-access.md`;
it is listed here only so the reader knows this survey saw it and passed.

**Proposed change:** add `transient` to `JacobsContest.java`:108-109. **Payoff:** two words; removes
two spurious keys from every serialized member. **Risk:** none.

## 4. Duplication and inconsistency findings

### f06-temporal-type-split

- **Category:** `duplication`
- **Where:** `response/skyblock/member/crimson/Matriarch.java`:15
- **Where:** `response/skyblock/member/crimson/SuusQuest.java`:14
- **Where:** `response/skyblock/member/crimson/SuusQuest.java`:16
- **Where:** `response/skyblock/member/crimson/NpcQuest.java`:14
- **Where:** `response/skyblock/member/crimson/SirihQuest.java`:14
- **Where:** `response/skyblock/member/crimson/PabloQuest.java`:19
- **Where:** `response/skyblock/member/crimson/DuelTrainingQuest.java`:14
- **Where:** `response/skyblock/member/hoppity/ChocolateFactory.java`:26
- **Where:** `response/skyblock/member/rift/TimecharmGallery.java`:25
- **What:** one upstream concept - a Unix epoch-milliseconds integer - is modelled four different
  ways across roughly 70 fields, and the choice is not correlated with anything.
- **Why it is residue:** every representation is reached by the same wire value. A reader cannot
  predict which accessor returns what, and cross-object comparisons need conversions that exist in
  none of these classes.
- **Candidate annotation:** none - keep imperative (this is a type decision, not a binding one)
- **Effort:** `medium`

The four representations, inventoried across all 133 files:

| Representation | Fields | Where it clusters |
| --- | --- | --- |
| `SkyBlockDate.RealTime` | ~40 | `skyblock/` broadly - auctions, island, member, resource |
| `java.time.Instant` | ~24 | `hypixel/`, `forum/`, and the newer member subtrees (`hoppity`, `attribute`, `rift`) |
| raw `long` epoch millis | 10 | `crimson/` quests only |
| `SkyBlockDate` / `SkyBlockDate.SkyBlockTime` | 6 | `firstJoinHub`, `lastDeath`, `ResourceCollections`, `Election.Cycle`, `JacobsContest.Contest` |

The clearest single proof that the split is arbitrary sits inside one JSON subtree. In
`nether_island_player_data` the fixture holds:

```
matriarch.last_attempt            = 1714964716810   ->  SkyBlockDate.RealTime   (Matriarch:15)
quests.suus_quest.last_completion = 1697144432124   ->  long                    (SuusQuest:16)
quests.sirih_quest.last_give      = 1669303312337   ->  long                    (SirihQuest:14)
quests.last_kuudra_relic          = 1656441390424   ->  long                    (CrimsonIsle:111)
abiphone.contact_data.*.last_call = 1786070864012   ->  Optional<RealTime>      (Abiphone:56)
```

Four representations, one node, all epoch millis. The ten raw `long` fields are the whole of the
inconsistency's cheap end - they are all in `crimson/` and were clearly written in one sitting:
`CrimsonIsle`:99, :111, :122, `DuelTrainingQuest`:14 and :18, `NpcQuest`:14, `PabloQuest`:19,
`SirihQuest`:14, `SuusQuest`:14 and :16.

The `Instant`-versus-`RealTime` split is a genuinely harder call and this survey does **not**
recommend collapsing it wholesale. `Instant` is right for `hypixel/` and `forum/`, which have no
SkyBlock calendar. `SkyBlockDate.RealTime` earns its keep inside `skyblock/` because it also answers
SkyBlock-calendar questions. What is not defensible is that `hoppity/`, `attribute/`,
`rift/TimecharmGallery` and `rift/Dreadfarm` sit inside `skyblock/member` and use `Instant` while
`rift/RiftAccess`, three lines away in the same package, uses `RealTime`.

Not every numeric temporal field is a bug. Several raw `int`s are genuinely SkyBlock *day numbers*,
not epochs - `CrimsonIsle.lastReset` is `90` in the fixture, and the `daily_..._day` family in
`HeartOfTheMountain` and `HeartOfTheForest` is the same idea. Those are correctly `int` and must not
be swept into a conversion.

**Proposed change, in two independently landable pieces:**

1. **The ten raw `long`s to `SkyBlockDate.RealTime`.** Mechanical, no library change, 6 files.
   This is the whole payoff for a small fraction of the cost.
2. **`Instant` to `SkyBlockDate.RealTime` inside `skyblock/`** - about 10 fields across `hoppity/`,
   `attribute/`, `rift/` and `SlayerQuest`. Larger blast radius because callers already type against
   `Instant`, and it needs a decision recorded somewhere that `skyblock/` is `RealTime` territory.

**Payoff:** no lines deleted - this trades zero code for one predictable rule. Rate it on
maintenance, not size. **Risk:** any consumer comparing `getLastCompletion()` as a `long` breaks at
compile time. **Effort `medium`** because it is 16-ish files and a convention decision, not because
it is hard; step 1 alone would be `small`.

### f06-crimson-npc-quest-family

- **Category:** `duplication`
- **Where:** `response/skyblock/member/crimson/NpcQuest.java`:9
- **Where:** `response/skyblock/member/crimson/SuusQuest.java`:9
- **Where:** `response/skyblock/member/crimson/MollimQuest.java`:9
- **Where:** `response/skyblock/member/crimson/CrimsonIsle.java`:78
- **Where:** `response/skyblock/member/crimson/CrimsonIsle.java`:80
- **Where:** `response/skyblock/member/crimson/CrimsonIsle.java`:90
- **Where:** `response/skyblock/member/crimson/CrimsonIsle.java`:92
- **What:** three top-level classes model the same NPC-quest object, differing by one field each,
  and one of them is already reused for two different quests.
- **Why it is residue:** `SuusQuest` is a strict superset of `NpcQuest`. `MollimQuest` differs from
  `NpcQuest` only in whether "finished" is expressed as a boolean or a timestamp.
- **Candidate annotation:** none - keep imperative (plain field union or inheritance)
- **Effort:** `small`

The three files side by side, in full - this is all of them:

```
NpcQuest      talked_to_npc:boolean   last_completion:long
SuusQuest     talked_to_npc:boolean   last_completion:long   last_toy_drop:long
MollimQuest   talked_to_npc:boolean   completed_quest:boolean
```

Fixture values confirm the objects are the same kind of thing:

```
pomtair_quest = {"talked_to_npc": true,  "last_completion": 1669401266316}
aranya_quest  = {"talked_to_npc": false, "last_completion": 1669401266316}
suus_quest    = {"talked_to_npc": false, "last_toy_drop": 1697143495810, "last_completion": 1697144432124}
mollim_quest  = {"talked_to_npc": true,  "completed_quest": true}
```

`CrimsonIsle.Quests` already treats `NpcQuest` as a shared type - `pomtairQuest`:78 and
`aranyaQuest`:92 are both `NpcQuest` - which is the pattern the other two should follow. Absent
keys bind to the type default, so a single class holding the union of four fields decodes all four
JSON objects correctly with no annotation work.

Two shapes are possible and the simpler one is better:

- **Union into `NpcQuest`** - add `lastToyDrop` and `completedQuest`, delete `SuusQuest` and
  `MollimQuest`. Cost: `mollimQuest`/`suusQuest` accessors change return type. Deletes **2 files,
  34 lines**, leaves `NpcQuest` at ~22 lines.
- **`SuusQuest extends NpcQuest`, `MollimQuest extends NpcQuest`** - keeps the domain names but
  keeps 3 files, and Gson binds inherited fields fine. Deletes ~8 lines. Not worth the ceremony.

Take the union. `MollimQuest.completedQuest` reads slightly oddly on a class named `NpcQuest`, but
so does having three names for one API object.

The rest of the crimson quest family is genuinely distinct and must not be swept in.
`AlchemistQuest`, `ChickenQuest`, `PabloQuest`, `SirihQuest`, `DuelTrainingQuest` and `EdelisQuest`
each prefix their keys with their own quest name (`alchemist_quest_start`,
`chicken_quest_progress`, `pablo_last_give`, `duel_training_phase_barbarians`), so they share no
serialized names at all - only the *file silhouette*. Nine quest files total 162 lines; this finding
claims 34 of them and deliberately leaves the other 128 alone.

**Payoff:** 2 files and 34 lines deleted, one fewer name for one API concept. **Risk:** low - the
two deleted types are referenced only from `CrimsonIsle.Quests`.

### f06-trophyfish-tier-columns

- **Category:** `duplication`
- **Where:** `response/skyblock/member/crimson/TrophyFishing.java`:29
- **Where:** `response/skyblock/member/crimson/TrophyFish.java`:51
- **Where:** `response/skyblock/member/mining/Powder.java`:9
- **What:** `TrophyFishing.TierData` spells out the four trophy-fish tiers as four `int` fields while
  `TrophyFish.Tier` declares the same four values as an enum eight lines away in a sibling file.
- **Why it is residue:** the enum is the domain model and the four columns are a transcription of
  it. A fifth tier upstream means editing two files, and the columns cannot be iterated.
- **Candidate annotation:** `@Capture` affix grouping already covers it - no new annotation
- **Effort:** `small`

```java
// TrophyFishing.java:29 - the columns
public static class TierData {
    @SerializedName("") private int total;
    private int bronze;
    private int silver;
    private int gold;
    private int diamond;
}

// TrophyFish.java:51 - the same four values, already an enum
public enum Tier { BRONZE, SILVER, GOLD, DIAMOND }
```

`TrophyFish.Tier` is not decorative: `TrophyFishing.lastCaught`:23 is a
`PairOptional<TrophyFish, TrophyFish.Tier>` bound with `@Split("/")`, so the enum is already on the
deserialize path in the same class that duplicates it as columns.

`Powder`:9 is the same idiom done the *other* way and is the model to copy - it keeps the affix
fields (`@SerializedName("")` for the bare key, `spent_` for the prefixed one) but the enclosing map
in `HeartOfTheMountain`:50 is keyed by `Powder.Type`, so the type dimension lives in the map key
rather than in columns.

**Proposed change:** replace the four columns with an enum-keyed map, using the affix grouping
`@Capture` already performs:

```java
@Getter
@NoArgsConstructor
public static class TierData {

    @SerializedName("")
    private int total;
    @Capture
    private @NotNull ConcurrentMap<TrophyFish.Tier, Integer> caught = Concurrent.newMap();

}
```

**Payoff:** 4 fields deleted, a fifth tier becomes a one-line enum edit, and `getCaught()` is
iterable. Roughly 4 lines net. **Risk:** real, and it is why this is `small` rather than `trivial` -
`@Capture`'s affix grouping is exactly the machinery that already splits keys against a value
class's field names, so nesting a `@Capture` map inside a class that is itself the value of an outer
`@Capture` (`TrophyFishing.fish`:25) needs a behavioural check before it is trusted. Verify against
the real `trophy_fish` node, which is absent from the bundled fixture. If nesting misbehaves, drop
this finding rather than working around it - four `int` fields is a small price.

### f06-daily-effect-twin

- **Category:** `duplication`
- **Where:** `response/skyblock/member/foraging/HeartOfTheForest.java`:18
- **Where:** `response/skyblock/member/foraging/HeartOfTheForest.java`:20
- **Where:** `response/skyblock/member/mining/HeartOfTheMountain.java`:21
- **Where:** `response/skyblock/member/mining/HeartOfTheMountain.java`:23
- **What:** the identical `current_daily_effect` / `current_daily_effect_last_changed` pair is
  declared in two classes under two different Java names.
- **Why it is residue:** upstream reuses one key pair for both the mining and foraging daily-effect
  systems. The Java side renamed them to the in-game feature (`skymall`, `lottery`), which is
  helpful naming but leaves two independent transcriptions of one contract.
- **Candidate annotation:** none - keep imperative
- **Effort:** `trivial`

```java
// HeartOfTheForest.java:18
@SerializedName("current_daily_effect")            private Optional<String> currentLotteryEffect = Optional.empty();
@SerializedName("current_daily_effect_last_changed") private int lotteryEffectLastChanged;

// HeartOfTheMountain.java:21
@SerializedName("current_daily_effect")            private Optional<String> currentSkymallEffect = Optional.empty();
@SerializedName("current_daily_effect_last_changed") private int skymallEffectLastChanged;
```

Byte-identical annotations, identical types, identical missing `@NotNull` on the `Optional` (both
appear in the nine-field list under `f06-completedat-zero-sentinel`). The `_last_changed` value is a
SkyBlock **day number**, not an epoch, so `int` is correct in both - this is not a
`f06-temporal-type-split` site.

**Proposed change:** honestly, do nothing structural. A shared `DailyEffect` value class would add a
nesting level for two fields, which is exactly the shape `03-value-shape-collapse.md` is trying to
remove. The defensible change is to make the two declarations *identical* - add `@NotNull` to both
`Optional`s and keep the domain-specific field names, which are genuinely more useful than
`currentDailyEffect` would be.

**Payoff:** 0 lines. This finding is recorded because a reader auditing daily-effect handling should
know both copies exist and must be changed together; it is not a refactor candidate. Ranked low
deliberately.

### f06-objective-status-shape

- **Category:** `duplication`
- **Where:** `response/skyblock/SkyBlockMember.java`:137
- **Where:** `response/skyblock/member/crimson/BoardQuest.java`:13
- **Where:** `response/skyblock/member/crimson/QuestBoard.java`:17
- **What:** the entire `objectives` node - 792 of 792 unmapped keys reported by the differ - is
  built from one repeated object shape that `BoardQuest` already models exactly, but only
  `objectives.tutorial` is bound, via `@SerializedPath`.
- **Why it is residue:** the shape is already typed. What is missing is the map that holds it, and
  `@Capture`'s catch-all mode is the tool for a key space of arbitrary objective ids.
- **Candidate annotation:** `@Capture` (bare catch-all, `Grouping.ENTRY`) - no new annotation
- **Effort:** `small`

Measured, not estimated. Across every member in the fixture:

| `objectives.*` value shape | Count |
| --- | --- |
| `{status, progress, completed_at}` | 789 |
| `{status, progress, completed_at, completions}` | 2 |
| `{status, progress, completed_at, <itemId>: n, ...}` | 6 |

That first row is `BoardQuest` field for field. The six item-bearing objectives carry extra
collection-requirement keys (`CARROT_ITEM`, `INK_SACK:3`, `ENCHANTED_SAND`) which a `@Lenient`
`ConcurrentMap<String, Integer>` overflow field absorbs.

`SkyBlockMember`:137 currently reaches into this node for one key:

```java
@SerializedPath("objectives.tutorial")
private @NotNull ConcurrentList<String> tutorialObjectives = Concurrent.newList();
```

**Proposed change:** rename `BoardQuest` to a neutral domain name shared by both callers (the
registry does not reserve DTO names, so the design document should choose; `QuestBoard` already
holds five of them and `objectives` would hold hundreds), then add a catch-all on `SkyBlockMember`:

```java
@SerializedPath("objectives")
@Capture(grouping = Capture.Grouping.ENTRY)
private @NotNull ConcurrentMap<String, BoardQuest> objectives = Concurrent.newMap();
```

`Grouping.ENTRY` is required here and is the recently added mode: without it, affix grouping would
try to split objective ids such as `talk_to_david_5` against `BoardQuest`'s field names and produce
nonsense. With it, each value is read whole.

**Payoff:** takes the module from 792 unmapped keys to 0 and adds ~4 lines. One class covers a node
that is 12% of the fixture by key count. **Risk:** two things must be checked first, and both are
cheap. (1) `objectives.tutorial` is a `List<String>`, not an objective object, so the catch-all's
value type has to tolerate it - either exclude it with a `filter`, or keep the existing
`@SerializedPath` field and let it claim the key before the catch-all sees it, which is the declared
`@Capture` precedence. (2) The status vocabulary here is `COMPLETE`/`INACTIVE`, so this change is
blocked on `f06-boardquest-complete-status` - land that first or 791 of 811 entries bind to `null`.

**Interaction note:** this finding is what makes `f06-boardquest-complete-status` urgent rather than
theoretical. Today the typo affects 5 objects; after this change it would affect 800.

### f06-noargs-constructor-drift

- **Category:** `duplication`
- **Where:** `response/skyblock/member/crimson/AlchemistQuest.java`:8
- **Where:** `response/skyblock/member/crimson/Matriarch.java`:10
- **Where:** `response/skyblock/SkyBlockAuction.java`:18
- **Where:** `response/skyblock/election/VotingBooth.java`:12
- **What:** the package uses four different constructor conventions on Gson-bound DTOs, and none of
  them changes how Gson instantiates the class.
- **Why it is residue:** the annotation was added as ceremony rather than for effect. A reader
  cannot tell whether its presence or absence means anything, so it costs attention on every file.
- **Candidate annotation:** none - keep imperative
- **Effort:** `trivial`

Counted over every class and nested class declaration in the package:

| Convention | Classes |
| --- | --- |
| no constructor annotation at all | 139 |
| `@NoArgsConstructor` | 36 |
| `@NoArgsConstructor(access = AccessLevel.PRIVATE)` | 30 |
| `@NoArgsConstructor(access = AccessLevel.PACKAGE)` | 3 |
| `@RequiredArgsConstructor` / `@AllArgsConstructor` only | 6 |

**Does it matter? Almost never, and the "almost" is worth stating precisely.** Java synthesises a
public no-arg constructor for any class that declares no constructor, so the 139 unannotated classes
already have one and Gson's `ConstructorConstructor` finds it. `@NoArgsConstructor` on those 69
classes generates the identical constructor. Access level is irrelevant to Gson, which calls
`setAccessible(true)`. So all four conventions behave identically.

The case where it *would* matter is a class that declares an arg-ful constructor and therefore loses
the synthesised default: Gson then falls back to `Unsafe.allocateInstance`, which skips the
constructor entirely, which means **field initializers never run** and every `= Concurrent.newList()`
/ `= new X()` default is `null`. A scan for that situation found nine classes and **none of them is
Gson-bound** - `HypixelRank`, `SpecialElection`, `Election.Cycle`, `Bestiary.Family`, `Bestiary.Mob`,
`DungeonClass`, `DungeonData`, `SkillLevel`, `Skills` are all constructed by hand in `postInit()` or
by an accessor. `Election` itself declares `Election(int year)` and correctly carries
`@NoArgsConstructor`:15 to restore the default - that is the one site where the annotation is
load-bearing, and it is right.

**Proposed change:** pick one rule and write it down. The defensible rule is *"annotate only where
the synthesised constructor was suppressed"* - which would leave exactly one `@NoArgsConstructor` in
the package (`Election`:15) and delete 68 annotations plus their `lombok.NoArgsConstructor` and
`lombok.AccessLevel` imports.

**Payoff:** ~68 annotation lines and a comparable number of now-unused imports removed, and the
remaining one carries real meaning. **Risk:** a class that later gains an arg-ful constructor
silently loses its default; a compile check will not catch it, only a decode will. That is the
argument for the opposite rule - *"annotate everything"* - which would add 139 annotations instead.
Either uniform answer beats the present four. **Ranked low**: this is 100% cosmetic today, and it
should not be sequenced ahead of anything in section 3.

### f06-rift-location-classes

- **Category:** `duplication`
- **Where:** `response/skyblock/member/rift/Rift.java`:30
- **Where:** `response/skyblock/member/rift/WizardTower.java`:7
- **Where:** `response/skyblock/member/rift/StillgoreChateau.java`:8
- **Where:** `response/skyblock/member/rift/BlackLagoon.java`:8
- **Where:** `response/skyblock/member/rift/EnigmasCrib.java`:11
- **Where:** `response/skyblock/member/rift/WyldWoods.java`:11
- **Where:** `response/skyblock/member/rift/Dreadfarm.java`:12
- **What:** seven rift location classes share an obvious silhouette - a handful of `boolean has*`
  progress flags plus an `int` step counter - and look like candidates for one parameterised type.
- **Why it is residue:** it is **not** residue. This finding exists to record that the idea was
  examined and rejected, so a later reader does not spend the same hour on it.
- **Candidate annotation:** none - keep imperative
- **Effort:** `medium` (if attempted, which it should not be)

The silhouette is real. The field *names* are not:

```
WizardTower       wizard_quest_step:int   crumbs_laid_out:int
StillgoreChateau  unlocked_pathway_skip:bool  fairy_step:int  grubber_stacks:int
BlackLagoon       talked_to_edwin:bool  received_science_paper:bool  delivered_science_paper:bool  completed_step:int
EnigmasCrib       bought_cloak:bool  found_souls:List<String>  claimed_bonus_index:int
WyldWoods         talked_threebrothers:List<String>  bughunter_step:int  sirius_started_q_a:bool  sirius_q_a_chain_done:bool  sirius_completed_q_a:bool  sirius_claimed_doubloon:bool
Dreadfarm         shania_stage:int  caducous_feeder_uses:List<Instant>
DeadCats          talked_to_jacquelle:bool  picked_up_detector:bool  found_cats:List<String>  unlocked_pet:bool  montezuma:Optional<OwnedPet>
```

Across all seven there is **not one shared serialized name**. Every key is specific to its NPC or
location. A shared base type could only hold the union, and the union is the concatenation - a base
class with `wizardQuestStep` and `grubberStacks` and `siriusQAChainDone` on it is strictly worse
than seven small classes, because every location would advertise every other location's fields.
The alternative, a generic `ConcurrentMap<String, Object>` per location, throws away the typing that
is the whole point of the DTO layer.

The one genuine overlap is `boolean has*` naming, which is already handled consistently by
`@Accessors(fluent = true)` - `hasTalkedToEdwin()`, `hasBoughtCloak()`, `hasUnlockedPathwaySkip()`.
That convention is applied at 11 of the rift sites and is correct.

**Verdict: do not do this.** Seven files, 236 lines total, averaging 34 lines each including package
and imports - there is no meaningful weight to remove. The same reasoning applies to
`WestVillage`'s four nested classes (`CrazyKloon`, `Mirrorverse`, `KatHouse`, `Glyphs`) and to
`VillagePlaza`'s five: they look like a family and are four unrelated JSON objects that happen to
sit under one key. Recorded so nobody proposes it again.

## 5. Minor observations - no finding raised

Small enough to fix in passing, too small to sequence.

- **`Crystal.Type.SAPHIRE`** - `response/skyblock/member/mining/Crystal.java`:32 misspells
  `SAPPHIRE`. Binding is unaffected because the constant carries
  `@SerializedName("sapphire_crystal")`, so this is `naming` only. It becomes load-bearing the day
  someone writes `Crystal.Type.valueOf("SAPPHIRE")`.
- **`CrimsonIsle.Quests.kuudraBossDaily`** - `CrimsonIsle.java`:69 is annotated
  `@SerializedName("kuuda_boss_daily")`. The key really is misspelled upstream (the fixture
  contains `kuuda_boss_daily`), so the annotation is *correct* and the Java name correctly fixes it.
  Noted because it looks exactly like the `starting_big` defect and is not one.
- **Nine `Optional` fields missing `@NotNull`** - listed inline under
  `f06-completedat-zero-sentinel`. Behaviourally inert; fold into whichever change touches those
  files.
- **`Kuudra` transient fields are package-private** - `Kuudra.java`:22-23 are the only two DTO
  fields in the package with no access modifier. Cause belongs to `02-parent-access.md`.
- **`Statistics.riftStats`** - `Statistics.java`:40 is
  `ConcurrentMap<String, Object>`, one of five `Map<String, Object>` escape hatches in the package
  (the others are `CrimsonIsle.Quests`:66, :68, :70 and `VillagePlaza`:22). These satisfy the differ
  without modelling anything. They are not duplication and not defects, but any coverage claim based
  on `json_dto_diff.py` should discount them.

## 6. What this survey deliberately did not report

**Owned by siblings.** Nested private holder classes and their forwarding accessors
(`Rift.Porhtal`, `VillagePlaza.Lonely`, `VillagePlaza.Seraphine`, `SkyBlockMember.Profile`,
`SkyBlockMember.Events`, `CrimsonIsle.kuudra_search_settings`) were seen at 34 sites and are left to
`03-value-shape-collapse.md` and `04-accessor-boilerplate.md`. Single-field value classes
(`Temples`, `HeartOfTheForest.BiomeWhispers.Tier`, `SkyBlockGarden.GreenhouseSlot`) likewise. The
`postInit()` bodies belong to `01-postinit.md`; the `collectionUnlocked` join and the
`^<itemId>_[0-9]+$` regex that excludes `MELON_-1` belong to `05-cross-field-derivation.md`.

**Hypotheses tested and dropped.** Three lines of enquiry were pursued and produced nothing worth
reporting; recording them saves a re-run.

1. **`int` overflow on large counters.** Every fixture value above `Integer.MAX_VALUE` was extracted
   and mapped to its declared field. Twenty-five distinct non-temporal paths exceed the range and
   every one already binds to a `long` or a `double`: `total_chocolate` 2.69e12 -> `long`,
   `auctions.gold_earned` 1.12e11 -> `long`, `master_catacombs.most_damage_mage.7` 9.12e10 ->
   `Double`, `banking.balance` 4.99e10 -> `double`. Collection amounts peak below 1e9 and
   `SkyBlockMember.collection` is `ConcurrentMap<String, Long>` regardless. **No finding.**
2. **Unmapped JSON keys outside `objectives`.** `scripts/json_dto_diff.py` reports 792 unmapped keys
   and all 792 are `objectives.*` (see `f06-objective-status-shape`). **No other coverage gap.**
3. **Default-instance versus nullable inconsistency for absent objects.** 152 nested-object fields
   use `= new X()` and only 7 are nullable, 4 of which are enums already covered by
   `f06-enum-null-clobber`. The convention is uniform enough that flagging it would be noise. The
   real inconsistency is in the temporal layer (`f06-temporal-type-split`) and in what `Optional`
   means (`f06-completedat-zero-sentinel`).

**Suggested landing order**, since several findings interact:

1. `f06-serialized-name-misses` and `f06-boardquest-complete-status` and
   `f06-jacobscontest-derived-nontransient` and `f06-hoppity-unreadable-fields` - four independent
   `trivial` consumer-only fixes, no library cycle, land together.
2. `f06-enum-null-clobber` with `f06-capture-null-enum-key` - one gson-extras cycle, one design
   decision, they share a fix.
3. `f06-objective-status-shape` - blocked on step 1.
4. `f06-crimson-npc-quest-family`, `f06-trophyfish-tier-columns`, `f06-temporal-type-split` step 1 -
   independent consumer-side cleanups.
5. `f06-completedat-zero-sentinel`, `f06-noargs-constructor-drift`, `f06-daily-effect-twin`,
   `f06-rift-location-classes` - defer or decline.
