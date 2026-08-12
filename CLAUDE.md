# hypixel

Feign contracts and the typed response tree for the Hypixel Public API v2, plus the `hypixel.net`
RSS feeds. Root `api.simplified.hypixel.**`. Ships no client - `simplified-dev/client` runs the
contracts and `simplified-dev/gson-extras` does the binding, so most of what looks like code here is
declaration.

## Build

- Gradle `group` is **`dev.sbs`**, the package root is `api.simplified.hypixel`, and the JitPack
  coordinate is `com.github.simplified-api:hypixel`. Three spellings, none derived from another.
- Every dependency is `api(...)` with an inline `strictly()` pin. A consumer that declares this
  module gets `Client`, `GsonSettings`, `ConcurrentList`, `SkyBlockDate` and `CompoundTag` and
  normally declares none of them.
- The `skyblock` sibling is a **compile dependency of the DTOs themselves**, not just of the stat
  layer: `SkyBlockDate`, `Season`, `Profile` and `GameMode` are field types in the response tree.
  Bumping that pin can change what a member decodes to.
- `HypixelApiGsonContributor` is reached only through
  `META-INF/services/dev.simplified.gson.GsonContributor`. `GsonSettings.defaults()` is what runs
  it; a bare `new Gson()` binds a mostly-empty tree and reports nothing.

## Gates

`./gradlew test` is two classes, `MemberDtoMappingTest` and `ElectionMappingTest`, and they are the
whole gate.

**A fresh clone runs both.** `MemberDtoMappingTest` reads `src/test/resources/craftedfury.json`, a
captured `GET /skyblock/profiles` response for the maintainer's own account, and `ElectionMappingTest`
reads `src/test/resources/elections.json`, an unauthenticated resource carrying mayors, perk text and
public vote counts. Both are **tracked**, and under `test/resources` neither reaches the jar.

`MemberDtoMappingTest` reads two members out of its capture: the first member of `profiles[0]` as the sparse case, the
first of `profiles[1]` as the populated one. Many assertions are pinned to that one capture - 792
objectives, 810 Jacob's contests, 100 collections, 29 rift counters, a `CATACOMBS` weight of
`200.0 / 2.03`, a `HEALER` weight of `90.6`. A different fixture breaks them by design; each is a
value captured from behaviour before a change, so they are updated deliberately and never loosened.

`python scripts/json_dto_diff.py` walks a capture against the DTO source and exits 1 on unmapped
keys. It parses Java with regex and knows `@SerializedName`, `@SerializedPath`, `@Extract`,
`@Capture`, `@Collapse`, `@Key` and `@Split` - **not `@Flatten` and not `@Lenient`**, so a field
using either reports its keys unmapped and that report is noise rather than a gap.

## Bound, derived, resolved

Three layers, and telling them apart is most of debugging this module.

| Layer | Produced by | Fails as |
|---|---|---|
| Bound | a field and its annotations | a default value, silently |
| Derived | an accessor on the DTO | a wrong number, or a swallowed parse |
| Resolved | a `SkyBlockData` repository | `JpaException`, with no session |

**Binding derives nothing and opens no session.** That is a property to preserve, not an
observation: derivation used to run at bind time inside a swallowing catch, so a throw left
`Bestiary.families` empty for every profile ever decoded and nothing anywhere said so.

`response/**` is the tree the wire binds into and it holds all three layers, not just the first.
`response.skyblock.stats` is where the derived layer keeps its own machinery: nothing under it binds,
nothing under it carries a serialization annotation, and everything under it is reached by a call a
caller makes by name. The line between the two halves is a field:

**A `stats` type appears under `response/**` as a parameter or a return type, never as a field.**
The decoded tree holds no stat, so no key on the wire pulls the stat layer in behind it and a session
opens only because a caller asked for one. `SkyBlockIsland.getProfileStats` and
`AccessoryBag.getDetectedAccessories` are what that looks like from the DTO side - a method the caller
names, on an object that decoded without one.

- `SkyBlockMember.getAccessoryBag()` calls `initialize` on every call, handing the bag the three
  member-scoped values it cannot reach from its own node. An `AccessoryBag` decoded standalone is
  never initialized and reads empty rather than throwing - both paths are asserted.
- `getSkills()` and `getCollectionUnlocked()` memoise into `transient` fields; `getCollectionUnlocked`
  returns an unmodifiable map and the same instance thereafter.
- `Bestiary.getFamilies()` and `AccessoryBag.getDetectedAccessories()` are the repository boundary.
  `getKills()` is not, and the parse behind the families completes with no session - only the lookup
  needs one.
- `ProfileStats.compute` is the top of the derived layer and the heaviest thing in it. It resolves
  every id against a repository, so it needs a session and it is not cheap.

## Serialization is not symmetric

- **A `@Lenient` decode rewrites the caller's own tree.** Gson hands `JsonTreeReader` the live
  element rather than a copy, so the filter phase's `replaceElement` strips overflow entries out of
  the object you passed in. Snapshot with `deepCopy()` **before** decoding or a comparison derives
  both sides from one mutated tree and a loss cancels itself out.
- **An enum map key returns as `name()`.** The wire's lowercase `none` comes back `NONE`, and a
  literal filter prefix in front of a constant gives `highest_wave_HOT`. Assert on a lowercased key
  set; case does not round-trip and that predates the annotations.
- **A partially overflowed array loses order.** Filtered entries go back at the end, not at the
  index the wire gave them. Membership round-trips; position does not.
- **No whole `SkyBlockMember` can be serialized.** `PlayerData.lastDeath` and several other date
  fields are null on a default sub-object and the `SkyBlockDate` adapters are not null-safe, so the
  write throws before reaching any path. Round-trip tests declare a small vehicle class carrying only
  the annotations under test - that is why `SharedPrefix` and `ObjectiveNode` exist rather than
  reusing the member.
- **A shared `@SerializedPath` prefix is a find-or-create.** The first field builds the object and
  every later one must reuse it. Three fields share `profile.` on the member and two share
  `treasures.` on `Dungeons`; two objects where there should be one means the last write won.
- **An `@Extract` field carries no `@SerializedName`**, so a re-decode of serialized output would
  bind it from the root key of the same name. It is asserted that no such root key is emitted.

## Wire shapes that mislead

- **Split an id at its LAST underscore.** `METAL_HEART_2` is tier 2 of `METAL_HEART` and `LOG_2_5` is
  tier 5 of `LOG_2`. `IdTiers.group` is the one place that happens; negative tiers survive it because
  only the caller knows whether they mean anything.
- **Collection ids carry colons.** `INK_SACK:3` and `INK_SACK` are different collections. A tier
  spelled `_-1` is visible-with-nothing-claimed, which is tier zero rather than a negative maximum,
  and 83 entries carry one.
- **A Jacob's contest key has four parts, not three.** `229:5_31:INK_SACK:3` truncated at three gives
  `INK_SACK` and passes every assertion written against the other 758 keys.
- **`master_catacombs` is lowercase on the wire.** A case-sensitive prefix filter let it through as
  its own dungeon under `Type.UNKNOWN` and left the real master floor unpaired. Master mode pairs onto
  its normal floor - `getFloorData(true)` - and carries completions with no `times_played` at all.
- **The finished objective status is `COMPLETE`, not `COMPLETED`.** The longer spelling appears
  nowhere on the wire, so 790 objectives and every finished board quest bound onto null over a
  `@NotNull` default.
- **An entry reaches overflow by its value's type, not its key.** `kills.last_killed_mob` is a String
  in a numeric map. `dungeon_journal.unlocked_journals` is every entry a String against a declared
  `ConcurrentList<Integer>`, so the field binds empty and the whole list is overflow - it is the only
  collection-shaped `@Lenient` field in the workspace and therefore the sole coverage of the
  `JsonArray` half of the factory.
- **Enum names must be the wire's names.** Dojo points are spelled `mob_kb`, `wall_jump` and so on;
  those names lived only in a constructor component, which the enum adapter never sees, so every
  entry missed. `EnumLookup.of(values(), name, FALLBACK)` is the shared case-insensitive lookup.
- Upstream typos are load-bearing: the chocolate factory hotspot key is `rabbit_hotspot_filer`.

## The @Fallback rule

Nothing carries `@Fallback`, and `noMarkedConstantIsWireVisible` is the statement of why. A marked
constant the wire can name collapses an unrecognised value and a real one onto the same member, and
in a map-key position the unknown entry silently overwrites a correct one - worse than the null it
replaces.

The test checks a constant's name against the **whole member subtree's vocabulary**, every key and
every string value, so it is deliberately stricter than the rule: `UNKNOWN` is rejected because some
unrelated subtree carries an `unknown` key. That is the trade taken on purpose - a wrong mark is
silent corruption with no compile error and no other failing test, a wrong refusal costs a null that
was already there. Every sentinel-shaped constant here is wire-named or collides, so adopting the
marker means adding a new constant first.

The scan guards itself with `enums.size() > 20` and loads classes off the build output directory, so
it sees compiled classes under `response/` and nothing else.

## Contracts

Two contracts, two clients, because the decoders differ - `HypixelContract` takes the Gson decoder,
`HypixelForumContract` needs the framework's `XmlDecoder` with the tree transformer that folds the
`<link>` / `<atom:link>` namespace collision before binding.

- `@Route("api.hypixel.net/v2")` and `@Route("hypixel.net")`; the route is where the rate-limit
  bucket key comes from.
- Auth is the `API-Key` dynamic header. `/resources/**`, the bazaar, both auction listings and
  fire sales need none.
- `HypixelErrorResponse` maps Hypixel's `cause` onto `reason` and adds `throttle` and `global`.
  `HypixelApiException` is built by the framework decoder, so its `(Gson, ErrorContext)` constructor
  is fixed by the `withErrorDecoder` method-reference shape and the five-constructor pattern does not
  apply.
- A missing subtree in a profiles response is a player's API privacy setting, not an error. Every
  collection field defaults non-null so that absence never reaches a caller as an NPE.

## Elections

`Election` identity is its **year alone**. `Cycle` declares no `equals`, so folding the derived
cycles into identity silently made two elections of one year unequal.

Voting opens late summer 27 of year N and closes late spring 27 of N+1; the term runs from that close
for a full year, so `term.start == voting.end` exactly. Both are computed on demand from `year`,
which is what lets gson bind the no-arg constructor without leaving a half-built object.

A ballot is a `ConcurrentList<Candidate>` on the `Election` itself, which is where the wire puts it -
under `mayor.election` and under `current` alike. It stays outside identity for the same reason the
cycles do: an election is its year, and two elections of one year stay equal however their candidate
lists differ. The whole package is three files - `Candidate` (plus its nested `Perk`), `Election`
(plus its nested `Cycle`) and `Mayor`. A candidate is one shape whether it stands on a ballot, sits
as minister or holds the office; `votes` is zero on the two the wire names outside any ballot.

**The wire spells a candidate's perks two ways.** Every node but the sitting minister sends `perks`
as an array; the minister sends the one perk it contributes as a lone `perk` **object**. Each
spelling binds to its own field and `getPerks()` reads whichever arrived, so the shape the wire chose
is the shape it reads back as. Binding both to one list with `@SerializedName(alternate = ...)` cannot
work - nothing in `dev.simplified.gson` coerces an object into an array, so the collection adapter
throws `Expected BEGIN_ARRAY but was BEGIN_OBJECT` and it propagates out through the `Optional`.

A ballot candidate's perks each carry a `minister` flag with **exactly one true**. The sitting
mayor's own perks carry no flag at all, because they are already in force, so `getMinisterPerk()` is
empty there and reads the flagged entry everywhere else.

## Debugging a mismatch

1. Decide which layer it is - bound, derived, or resolved - before reading any code.
2. `python scripts/json_dto_diff.py --section <node>` for a bound value that is simply absent.
3. Decode the one subtree in a scratch test rather than the whole member; that is what every test
   here does and it is why they are readable.
4. Read the expectation from a pristine `deepCopy()`, never from the tree you decoded.
5. For a derived value, check whether the accessor is reached at all - a `transient` memo field and
   an `initialize` call both make the first call the only one that computes anything.

## Skip these

- `build/`, `.gradle/` - Gradle output and daemon state.
- `.schema/` - generated JPA schema, excluded from the IDE module by `build.gradle.kts`.
- `src/test/resources/craftedfury.json` - the profiles fixture, 1.6 MB over 26,940 lines. Tracked, but
  read a slice through `scripts/json_dto_diff.py --section <node>` rather than opening it whole.
- `notes/` - gitignored working notes on the gson-extras and json-annotations efforts. Read one when
  picking up a live effort; nothing downstream reads them, so do not cite a `notes/` path or a note's
  entry number from a tracked file - the directory resolves for nobody who clones this.

## Decisions that stay closed

- Do not derive at bind time. It needs a repository the decoder has no reason to have, it puts the
  throw inside a swallowing catch, and it makes the DTO tree unusable without a database session.
- Do not put a `stats` type in a field under `response/**`. A parameter or a return type charges the
  compute to the caller that asked for it; a field puts it behind a decode, which is how derivation
  reached bind time the first time.
- Do not hand-write an adapter for a shape an annotation covers. The annotations round-trip back to
  the wire layout; a hand-written adapter has to be written twice and the write half is the one
  nobody tests.
- Do not give each NPC its own quest class. `NpcQuest` is one union of four fields, and a key an NPC
  does not send stays absent rather than binding a default - inventing a timestamp is worse than not
  having one.
- Do not split master mode into a second dungeon. It is a `FloorData` on its own floor of the same
  `DungeonData`, and the empty `times_played` is the wire's, not a bug.
- Do not fold the derived cycles into `Election` equality or `hashCode`, and do not fold the ballot
  in either - it is the election's contents rather than its name.
- Do not give the minister its own class. It is a `Candidate` whose perk the wire spells singular,
  and the two spellings are one field pair rather than two types.
- Do not hand-roll a delimiter parse. `Kuudra.SearchSettings.combatLevel` is `@Split("-")` with a
  `Range` default of `0..60`; the previous `Integer.parseInt` on both halves threw at the caller on an
  absent or malformed range.
- Do not reduce `player_classes` through a funnel. `DungeonClass` has one field and the wire node
  already is that shape.
- Do not mark a constant `@Fallback` to silence a null. Add a constant the wire cannot name.
