# gson-extras - design pack

## 1. What this changes, and why it runs before the DTO pack

Five scoped changes to `gson-extras`, designed to implementation-ready depth. Three of them are one
mechanism: **`@Extract` cannot see `@Capture`'s overflow, and has no way to claim more than one named
key at a time.** The other two are the annotations the DTO research pack already accepted and never
had a cycle for.

This supersedes `notes/json-annotations/20-implementation-plan.md` §12, stage 9
(`s20-library-cycle`), which sat **last** in the DTO plan. Reversing the order buys three things:

| | |
| --- | --- |
| DTO stage 5 `s20-existing-annotation-sweep` | Gets `CrimsonIsle.Quests.questRewards` - the one `Object` field it could not type, because two fields cannot both claim `quest_rewards` |
| DTO stage 4 `s20-objectives-catchall` | Runs after the `json_dto_diff.py` parser patch, so the 792 -> 0 unmapped move is not entangled with a phantom binding |
| DTO stage 1 `s20-dark-feature-fixes` | Three enum naming fixes get **pulled forward**, because `@Fallback` would mask them |

Baselines to hold throughout: gson-extras **134/134**, hypixel **16/16**.

## 2. The five items at a glance

Item 5 carries two annotations, so six design entries answer five scope items. Every one is
**adopt** or **adopt narrowly**; nothing in scope was declined.

| # | Entry | Verdict | What it does, in one line | Library change | Sites | Effort | Cycle |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `dgx-overflow-store` | adopt | One `Overflow` store that tags each entry with its write target, and `@Extract` lifted into `ExtractTypeAdapterFactory` registered **outside** `Capture` | 2 new files, 3 edited | 0 - it is the enabler | `large` | 1 |
| 2 | `dgx-extract-filter` | adopt narrowly | `@Extract` gains `filter()` - a dotless `value()` claims the whole remaining overflow, filtered by regex, into a typed map | 1 element, 1 factory | 1, up to 7 | `medium` | 1 |
| 3 | `@Lenient` | adopt | No entry of its own - it **loses** ~90 lines to item 1 and gains two of item 5a's guards | existing factory edit | 10 | folded | 1 and 2 |
| 4 | `dgx-capture-unmatched` | adopt | An enum key matching no constant goes to overflow instead of becoming a `null` map key. **No new element** | 1 predicate clause, 1 build-time divert | 6 | `medium` | 1 |
| 5a | `dgx-fallback` | adopt narrowly | `@Fallback` marks the enum constant an unrecognized value reads as, instead of `null` | 1 new file, 3 edited factories | 14 behind 12 enums | `medium` | 2 |
| 5b | `dgx-flatten` | adopt narrowly | `@Flatten("current")` collapses a single-key wrapper out of every map or collection entry | 2 new files, 1 registration line | 1 | `small` | 2 |

**Three cycles, two re-pins.** Cycle 0 is characterisation tests only and costs no pin. Cycle 1 is
items 1, 2 and 4 - they share the store and cannot be split at a pin boundary. Cycle 2 is 5a and 5b.
Estimated **23-33 hours elapsed AI-assisted** for stages 0-10, against **13-17 working days**
human-developer.

## 3. Corrections that changed the design

Six claims in the brief that seeded this cycle were checked against source and are wrong. Three of
them change the design, and the first one changes it a lot. Full derivations in
`00-verified-facts.md` §1.

| # | The claim | The fact | Consequence |
| --- | --- | --- | --- |
| C1 | "Earlier registration = OUTER. So Lenient wraps Capture" | **Inverted.** `GsonBuilder.create()` reverses the user factory list (gson 2.11.0 `GsonBuilder.java`:887-890), so the **last** registered is the **outermost**. `Capture` wraps `Lenient` | Everything downstream of the premise. The library's own javadoc (`GsonSettings.java`:215-218) already says so correctly |
| C2 | "`@Capture`'s overflow does not exist yet when `@Extract` runs" | It is fully computed. It is **frame-local**, and the identity key the store wants does not exist yet | The gap is publication and lifetime, not order |
| C3 | "This is an ordering problem" | The order is already the favourable one | **Takes an `xlarge` off the table.** No `GsonSettings` reorder is needed; both new factories are inserted |
| C4 | Three `@Extract` sites | **Six**, and two of the three names were wrong - the `ChocolateFactory` fields source from `rabbits`, not `eggs`/`locations` | The regression surface for the lift is twice what the brief implied |
| C5 | Three empty catch blocks | **Five** of the silent-swallow class, plus one `ignored`-named | Two more than the brief costed |
| C6 | Seven unmatched-enum-key `@Capture` sites incl. `Statistics.java`:89 | **Six.** `Statistics.java`:89 has no `@Capture` at all - it binds through gson's stock map adapter | No `@Capture`-scoped change can reach it. It is R5, and it is out of this cycle |

One more, found while writing the tests item 1 owes rather than by reading:

> **`@Extract` has never removed its own field's serialized key from the output.** All six sites emit
> their extracted value **twice** on every serialize, today, on `7cfc181` - once inside the source,
> once at the root under the Java field name. Invisible because `LenientTypeAdapter.write` is
> unexecuted by all 134 library tests and `MemberDtoMappingTest` never calls `toJson`.

## 4. Gallery - the shared `Overflow` and the `@Extract` lift

### 4.1 The gap, traced rather than asserted

One read of a class carrying one `@Capture`, one `@Lenient` and one `@Extract` field. Indentation is
stack depth. This is the whole problem on one screen.

```
Capture.read                                            CaptureTypeAdapterFactory :257
  :258  buffer stream to rootElement
  :270  seed capturedJsonMaps / overflowMaps        <-- FRAME-LOCALS
  :311  classify every root key
          -> knownObject        declared-field keys
          -> capturedJsonMaps   claimed keys, stripped
          -> overflowMaps       keys that failed the declared generics, UNSTRIPPED
  ###   CAPTURE OVERFLOW IS NOW FULLY COMPUTED, AND ONLY A LOCAL   ###
  :366  delegateAdapter.fromJsonTree(knownObject)
    |
    Lenient.read                                        LenientTypeAdapterFactory :153
      :162  allocate overflows                     <-- A DIFFERENT FRAME-LOCAL
      :165  filter phase -> per-@Lenient-field overflow appended to overflows
      :203  extract phase -> searches ONLY overflows     <-- THE REACHABILITY GAP
      :224  delegateAdapter.fromJsonTree(rootObject) -> reflective binder
      :230  OVERFLOW.put(lenientCollection, ...)         [Lenient static store]
      :242  gson.fromJson(claim) + accessor.set          [@Extract assignment]
    |
  :372  per @Capture field:
  :377  build the Map instance                      <-- THE IDENTITY KEY FINALLY EXISTS
  :381  accessor.set(result, capturedMap)
  :387  OVERFLOW.put(capturedMap, overflow)              [Capture static store]
```

Two facts, both read off source:

1. **`@Capture`'s overflow already exists** when the extract phase runs at `:203`. It is complete at
   `:363`, three lines before the delegate call. It is simply in another stack frame.
2. **The identity key does not.** The `Map` that keys `Capture`'s store is constructed at `:377`,
   fourteen source lines after `@Extract` has finished. `Capture` **cannot** publish early even if it
   wanted to.

### 4.2 Why merging the two static stores fixes nothing

The obvious fix is to union `LenientTypeAdapterFactory.java`:62 and
`CaptureTypeAdapterFactory.java`:82 into one map. Here is the claim `@Extract` actually makes, verbatim
from `LenientTypeAdapterFactory.java`:206-220:

```java
FieldOverflow sourceOverflow = overflows.stream()                       // :206
    .filter(o -> o.fieldName().equals(extractInfo.getSourceFieldName()))
    .findFirst()
    .orElse(null);

if (sourceOverflow == null)
    continue;                                                           // :212 - silent

JsonElement overflowObj = sourceOverflow.overflow();

if (overflowObj.isJsonObject()) {
    JsonElement claimed = overflowObj.getAsJsonObject()
        .remove(extractInfo.getJsonKey());                              // :216 - destructive
    ...
}
```

`overflows` is `LenientTypeAdapterFactory.java`:162 - `ConcurrentList<FieldOverflow> overflows =
Concurrent.newList()`, a **method-frame local**. The static `OVERFLOW` field is not read anywhere in
`read`; its only read site is `:127`, inside `write`.

> **On the read path both static stores are write-only.** Union them and `:206` still consults
> `overflows`, still finds only `@Lenient` field names in it, and still cannot see a single
> `@Capture` entry. The union is invisible to the code that would need it.

And a plain union would be wrong on the **write** side even if it worked on the read side, because
the two producers merge back into different places and both are correct:

| | `@Lenient` overflow | `@Capture` overflow |
| --- | --- | --- |
| Merge target | the field's **own sub-object**, `locateElement` `:132` | the **root** object, or the descend node, `:242-244` |
| Key form stored | the key as it appeared inside that sub-object | the **original unstripped** root key |
| Published when | always, **even when empty** (`:236-239`) | non-empty only (`:386`) |

An entry claimed out of `Capture`'s overflow has to be re-injected the `Capture` way. That is why the
store needs **per-entry target tagging**, not a map union.

### 4.3 The fix - move `@Extract` outward, tag every entry with its write target

**Do not make `@Extract` run earlier. Make it run later.** Lift it out of
`LenientTypeAdapterFactory` into its own factory registered **after** `Capture` in
`GsonSettings.defaults()`, which by C1 means nesting **outside** it. Its phase then begins once both
producers have published, so both identity keys exist and both are reachable by reflection off the
object that was just built.

```java
// GsonSettings.java:248 - insertion only. No existing pair is reordered.
    new LenientTypeAdapterFactory(),
    new FlattenTypeAdapterFactory(),      // new, item 5b
    new CaptureTypeAdapterFactory(),
    new ExtractTypeAdapterFactory(),      // new, item 1 - MUST be after Capture
    new CollapseTypeAdapterFactory(),
    new PostInitTypeAdapterFactory()
```

Resulting nesting, outermost first:

```
PostInit -> Collapse -> Extract -> Capture -> Flatten -> Lenient -> SerializedPath -> Split -> ... -> Reflective
```

The store. Package-private in `dev.simplified.gson.factory` beside `WeakIdentityMap`, which needs no
visibility bump. The `Target` tag is the entire point:

```java
enum Target {

    /** the owning field's own JSON element, located by serialized name or serialized path */
    FIELD_ELEMENT,

    /** the object the entries were classified out of - the enclosing object, or the nested node a descending capture reads */
    SOURCE_OBJECT

}

record Entry(@NotNull Target target, @NotNull JsonElement element) { }

static void publish(@NotNull Object owner, @NotNull Target target, @NotNull JsonElement element);
static @Nullable JsonElement find(@NotNull Object owner, @NotNull Target target);
static @NotNull JsonElement open(@NotNull Object owner, @NotNull Target target, @NotNull Supplier<JsonElement> ifAbsent);
static @Nullable JsonElement claim(@NotNull Object owner, @NotNull String key);
static @NotNull JsonObject claim(@NotNull Object owner, @NotNull Predicate<String> filter);
static void restore(@NotNull Object owner, @NotNull String key, @NotNull JsonElement element);
```

The read path becomes a lookup off a built object, which is directly testable:

```java
@Override
public @Nullable T read(@NotNull JsonReader in) throws IOException {
    T value = this.getDelegateAdapter().read(in);        // every producer has now published

    if (value == null)
        return null;

    for (ExtractFieldInfo info : this.getExtractFields()) {
        Object owner = info.getSourceAccessor().get(value);   // the @Lenient collection, or the @Capture map

        if (owner == null)
            continue;

        if (info.isRemainder()) {
            JsonObject claimed = Overflow.claim(owner, info::matches);

            if (!claimed.isEmpty())
                this.assign(value, info, owner, claimed);
        } else {
            JsonElement claimed = Overflow.claim(owner, info.getJsonKey());

            if (claimed != null)
                this.assign(value, info, owner, claimed);
        }
    }

    return value;
}
```

Two side effects worth having:

- **`read` no longer buffers the stream at all.** `@Extract` never inspected the tree - it inspected
  the overflow. A class carrying `@Extract` but no `@Lenient` currently still builds a
  `LenientTypeAdapter` and pays a full `JsonElement` round-trip for a phase that can never match.
- **One of the library's five silent swallows leaves.** `LenientTypeAdapterFactory.java`:246-247
  swallowed an `@Extract` conversion failure **after** the entry had already been removed at `:216`,
  so the value was gone from the object **and** from the document. The replacement calls
  `Overflow.restore` in the catch. The field still keeps its initialiser, so no consumer sees a new
  exception.

### 4.4 The write path, which is where it nearly falls over

Today `@Extract`'s re-injection (`:98-118`) and `@Lenient`'s merge-back (`:121-145`) are consecutive
loops in **one method**, so their order is free. After the lift it is not. A write chain runs
outermost-first on the way down and innermost-first on the way back:

```
Extract.write        entered first
  Capture.write      entered second
    Lenient.write    entered third
      ... reflective binder produces the flat tree ...
    Lenient post-processes   <-- merge-back runs HERE, first
  Capture post-processes     <-- merge-back runs HERE, second
Extract post-processes       <-- would run LAST, far too late
```

An outermost factory cannot post-process its way out of this. **The resolution is that the half of
`@Extract`'s write that has to happen early is not tree work at all.** Re-injection mutates the
overflow container in the store, not the output JSON, so it runs **before** the delegate call. The
other half - removing the `@Extract` field's own root key, which is the duplicate-key defect from §3 -
is tree work and wants the opposite end of the same call. Outermost is the only position offering
both ends.

`Loadouts`, whose two `Optional<Integer>` fields come out of two different `@Lenient` maps:

```
Extract.write(loadouts)
  equippedArmorSet = Optional.of(2)     -> owner = the live armorSets map
     Overflow.open(armorSets, FIELD_ELEMENT, JsonObject::new)
     overflow.add("equipped_set", 2)
  equippedEquipmentSet likewise into the equipmentSets overflow
  delegate.write ->
    Lenient.write
      :92  toJsonTree -> {"armor": {...}, "equipment": {...}, "loadouts": {...}}
      :121 merge-back: Overflow.find(armorSets, FIELD_ELEMENT) now carries "equipped_set"
      :132 locateElement -> the "armor" sub-object
      :137 armor["equipped_set"] = 2
  tree = {"armor": {..., "equipped_set": 2}, ..., "equippedArmorSet": 2, "equippedEquipmentSet": 4}
  remove "equippedArmorSet", remove "equippedEquipmentSet"
```

Byte-identical to today's output **minus two root keys the input never carried**. `@Extract` never
decides where an entry lands - it hands the entry back to the producer that owned it, and the producer
already knows. That is why per-entry tagging is enough and no target *routing* is needed.

## 5. Gallery - `@Extract` gains a filter

One JSON object carrying two unrelated maps, interleaved by value type. `CrimsonIsle.java`:65-66:

```java
// BEFORE - the one Object field the DTO pack's annotation sweep could not type
@SerializedName("quest_rewards")
private @NotNull ConcurrentMap<String, Object> questRewards = Concurrent.newMap();
```

```java
// AFTER - @Lenient types the integer half, the filtered remainder types the string half
@Lenient
@SerializedName("quest_rewards")
private @NotNull ConcurrentMap<String, Integer> questRewards = Concurrent.newMap();

@Extract(value = "questRewards", filter = "^crimson_isle_")
private @NotNull ConcurrentMap<String, String> questItems = Concurrent.newMap();
```

**Mode is selected by the dot in `value()`, not by the presence of `filter()`.** That choice is what
keeps the change out of `xlarge` territory:

| `value()` | Mode | `filter()` |
| --- | --- | --- |
| `"kills.last_killed_mob"` | single key - today's behaviour, all six existing sites | must be empty, else `JsonException` at `create` |
| `"questRewards"` | remainder - claims every entry the filter accepts | empty is the catch-all, mirroring `@Capture` |

The dotless form is a **free semantic slot**. `ExtractFieldInfo`'s constructor already branches on
`path.indexOf('.')` and the else branch is dead - it sets `jsonKey = ""` and then claims a literal
empty key no document carries. Zero sites use it, so giving it a meaning changes the behaviour of
nothing that was doing anything.

Two deliberate cuts, both reversible later:

- **Selection, no stripping.** `@Capture` strips and reconstructs via `literalPrefix`, which is the
  filter with `^`/`$` removed - a literal, not a regex inverse. Duplicating that in a second
  annotation doubles the places a real metacharacter silently fails to round-trip, and a
  `@Capture`-sourced claim would **double-prefix**, because `@Capture` deliberately stores overflow
  unstripped. The one real site wants the keys anyway: the quest id **is** the key.
- **Object-shaped overflow only.** A collection-shaped `@Lenient` field yields a `JsonArray`, which
  has no key space for a filter. `Overflow.claim` returns nothing there, preserving today's silent
  no-op exactly.

Claim order is destructive, so it is banded rather than left to `getDeclaredFields`: single key
first, then filtered remainder, then catch-all remainder. Two catch-alls on one source is a `create`
rejection. **Two overlapping filtered remainders is declared, not fixed** - deciding whether two
regexes intersect is not something a `create` check can do.

## 6. Gallery - `@Capture` and the unmatched enum key

**The DTO is not edited at all.** `Kuudra.java`, the class the probe was run on:

```java
@Capture(filter = "^highest_wave_")
private @NotNull ConcurrentMap<Tier, Integer> highestWave = Concurrent.newMap();
@Capture
private @NotNull ConcurrentMap<Tier, Integer> completedTiers = Concurrent.newMap();
```

Given `{"highest_wave_none": 5, "none": 1, "brand_new_tier": 4, "another_new_tier": 2}`:

| | `completedTiers` | Overflow | Serialized back |
| --- | --- | --- | --- |
| Before | `{null=2, BASIC=1}` - `brand_new_tier`'s value **overwritten** by `another_new_tier`'s | empty | `BASIC` only; both new tiers gone |
| After | `{BASIC=1}` | `{brand_new_tier: 4, another_new_tier: 2}` | all three keys, byte-exact |

The loss is **N-1 values per field**, not "one odd key". The library fix is one clause in an existing
predicate. **No new element**, which is what makes it a different proposal from the one the research
pack declined:

```java
// CaptureTypeAdapterFactory.isCompatibleCaptureEntry
if (rawKeyType != String.class) {
    try {
        if (this.getGson().fromJson(new JsonPrimitive(key), info.getKeyType()) == null)
            return false;                                   // <-- the added clause
    } catch (Exception ex) {
        return false;
    }
}
```

It is a **no-op for every key type except an enum**. Gson's `Integer`, `Long`, `UUID` and `Instant`
adapters all throw on an unusable string; the `String` branch never reaches the conversion. Only
`CaseInsensitiveEnumTypeAdapterFactory.read`:82 returns `null` without throwing. It also brings the
key check into line with the **value** check, which has always been written the right way round
(`:538-541` tests `result != null`). This removes an inconsistency; it does not add a policy.

Grouping mode needs a second branch, because it skips the compatibility check entirely and therefore
produces **no overflow at any point**. `buildGroupedMap` diverts at the `:474` conversion instead,
restoring the entries the group was built from under `literalPrefix + strippedKey`. That is what
reaches `TrophyFishing.fish` and `HeartOfTheMountain.powder`.

Making the diverted keys **visible** is optional, per site, and is item 2's element rather than
anything new here:

```java
@Capture
private @NotNull ConcurrentMap<Tier, Integer> completedTiers = Concurrent.newMap();
@Extract("completedTiers")
private @NotNull ConcurrentMap<String, Integer> unknownTiers = Concurrent.newMap();
```

Key type is `String`, not `Tier`, by definition - these are the keys no `Tier` matched, and an
enum-keyed remainder reproduces the defect one level up. **The companion field is only free because
item 1 removes its root key**; on today's library it would add a phantom `"unknownTiers": {...}`
object to every serialize.

## 7. Gallery - `@Fallback`

An enum-constant marker, not a field-level default supplier. The whole library change is one field,
one changed `return`, and one static query:

```java
// BEFORE - CaseInsensitiveEnumTypeAdapterFactory:82
return nameToConstant.get(in.nextString().toUpperCase());

// AFTER
E constant = nameToConstant.get(in.nextString().toUpperCase());
return constant != null ? constant : this.fallback;
```

```java
// The consumer edit, once per enum
public enum Faction {

    @Fallback
    NONE,
    @SerializedName("mages")
    MAGES,
    @SerializedName("barbarians")
    BARBARIANS

}
```

`this.fallback` is `null` for an unmarked enum, so the read result is **bit-identical** for every
consumer that does not opt in. `write` is not edited at all. A JSON null still reads as `null`, so
absence stays distinguishable from an unrecognized value.

**The safety of this design lives in the adoption list, not in the code.** Two things follow, and
both are conditions on the acceptance:

| Rule | Why |
| --- | --- |
| **Never mark a constant the wire can name.** `Kuudra.Tier.BASIC` carries `@SerializedName("NONE")`, so marking it turns `{null=4, BASIC=1}` into `{BASIC=4}` - the unknown tier now **overwrites a correct entry**. Worse than the defect | The rule is a property of the upstream API's vocabulary, not of the Java. No compile error, no test failure. `noMarkedConstantIsWireVisible` makes it executable |
| **Ship the four compatibility guards in the same commit.** `CaptureTypeAdapterFactory.java`:538-541 and `LenientTypeAdapterFactory.java`:309-312 test `result != null` for enum **values** | A marked enum otherwise stops diverting unrecognized values to overflow - lossless silently becomes lossy, for every sibling on the shared pin |

Eight of the twelve eligible in-module enums have **no sentinel constant to mark** - their current
default is itself a live wire value - so they need a new `UNKNOWN` first, and each addition needs a
look at that enum's `values()` consumers. `Rarity` and `GameMode` live in `Simplified-Api/skyblock`
and cost a second chained publish cycle for three field sites; deferred.

**Three live modelling defects `@Fallback` would mask, and they must land first**: `Dojo.Type`'s
seven internal names, `RabbitSort`'s `rarity_high_low`, and `ActiveCommission.Status`'s single
constant. All are `trivial`, consumer-only, and real data loss today.

## 8. Gallery - `@Flatten`

`Currencies.java`, the whole file, 26 lines to 19. One adoption site in the module.

```java
// BEFORE
@Getter
public class Currencies {

    @SerializedName("motes_purse")
    private int motes;
    @SerializedName("coin_purse")
    private double purse;
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, ConcurrentMap<String, Integer>> essence = Concurrent.newMap();

    public @NotNull ConcurrentMap<String, Integer> getEssence() {
        return this.essence.stream()
            .mapValue(value -> value.get("current"))
            .collect(Concurrent.toMap());
    }

}
```

```java
// AFTER
@Getter
public class Currencies {

    @SerializedName("motes_purse")
    private int motes;
    @SerializedName("coin_purse")
    private double purse;
    @Flatten("current")
    private @NotNull ConcurrentMap<String, Integer> essence = Concurrent.newMap();

}
```

`getEssence()` survives with an unchanged signature, generated by the class-level `@Getter`. Wire
shape `{"WITHER": {"current": 1955}}` binds `{WITHER: 1955}` and serializes back wrapped.

Three corrections against the research pack's accepted design, all in this pack's favour to state
loudly:

| | Pack said | Actually |
| --- | --- | --- |
| Registration | index 3 | **index 5.** At index 3 `Lenient` is outer, sees the uncollapsed wrappers, judges every one incompatible and diverts the whole field to overflow before `@Flatten` is consulted |
| `@Lenient` + `@Flatten` | "resolvable, it just costs the deeper default" | **Rejected at `create`.** Index 3 loses the field; index 5 reads correctly and then corrupts the write by re-wrapping what `Lenient` merged back |
| Round trip | "the value re-serializes correctly instead of not at all" | **Worse, not better.** The collapse is a projection - a wrapper carrying a sibling member such as `total` reads fine and is **lost on serialize**. Declared as a contract, and pinned by `flattenMultiMemberWrapper_roundTrip` |

The pack's conditional ("ship only if `@Fallback` ships") is now satisfied by construction, and the
stock `@JsonAdapter` alternative loses its only advantage: its entire case was the zero in the
JitPack-cycles column, and the cycle is sunk.

**Two zero-cost neighbours to take regardless of what happens to `@Flatten`** - they delete more
lines than it does and need no library at all: retype `Dungeons.classMap` to
`ConcurrentMap<DungeonClass.Type, DungeonClass>` (deletes six lines of `postInit()`), and put
`@Getter(AccessLevel.NONE)` on `HeartOfTheForest.BiomeWhispers.tiers`.

## 9. `@Lenient` - all consequence, no entry of its own

Nothing is proposed for `@Lenient`. It is where the cost of the other four lands.

| Site | Now | After |
| --- | --- | --- |
| `:62` | `private static final WeakIdentityMap<Object, JsonElement> OVERFLOW` | deleted - `Overflow` replaces it |
| `:68`, `:70-72` | wraps when **either** `@Lenient` or `@Extract` fields exist | wraps only for `@Lenient`; an `@Extract`-only class gets the delegate handed straight back |
| `:98-118` | write-side `@Extract` re-injection | moves to `ExtractTypeAdapter.write` |
| `:127` | `OVERFLOW.get(collection)` | `Overflow.find(collection, Target.FIELD_ELEMENT)` |
| `:203-221` | read-side extract phase | deleted |
| `:239` | `OVERFLOW.put(...)` | `Overflow.publish(collection, Target.FIELD_ELEMENT, ...)` |
| `:242-248` | post-assign `@Extract`, **including the empty catch** | moves, with `Overflow.restore` in the catch |
| `:258-264`, `:309-312` | key/value compatibility checks | gain `&& !isFallback(...)` in cycle 2 |

Net: about **ninety lines out of five separate regions** of the library's second-most-used file. The
`FieldOverflow` record and the whole filter phase are untouched. Three policies are deliberately
**preserved rather than unified**:

- `@Lenient` publishes its overflow **unconditionally, even when empty**; `@Capture` only when
  non-empty. Unifying either way changes behaviour a consumer can observe.
- A shape mismatch (map field, array JSON) still matches neither branch and is handed raw to the
  delegate.
- `LenientFieldInfo.of` still skips any field also carrying `@Capture`, so two annotations on one
  field is still `@Capture`-wins.

One consumer-side note that is an adoption rule, not a code change: **`@Lenient` on a plain
enum-keyed map is what moves it into item 4's reach.** Roughly forty enum-keyed maps in the module
bind through gson's stock `MapTypeAdapterFactory` and are unreachable by anything in this cycle.

## 10. The one semantic change that carries risk

The brief framed this as "`@Extract` moves from tree-mutation to post-build field assignment". **Both
halves of that premise are false, and the code says so plainly** - which matters, because if it were
true the lift would carry real risk, and it does not.

- **`@Extract` does not mutate the tree.** The filter phase builds two fresh objects, `filtered` and
  `overflow`, from `original` (`:172-184`). Only `filtered` goes back into the tree at `:183`;
  `overflow` is appended to a local list and never attached to `rootObject`. The `remove` at `:216`
  therefore removes from an object that is **not in the tree**. The tree the delegate reads at `:224`
  is byte-for-byte identical whether or not any `@Extract` field exists.
- **`@Extract` already assigns post-build.** `:242-248` runs after `:224`. `accessor.set(value, ...)`
  is a reflective set on an object the reflective binder has already populated.

Reflection-set-after-build is not a change. It is the current implementation, moved. What actually
moves is the **claim**, from before the delegate call to after it, and six things could notice:

| Could notice | Verdict |
| --- | --- |
| The store's contents at end of read | **Identical.** `:239` stores the reference to the `JsonObject` created at `:174`, not a copy. Removing before or after publication leaves the same object in the same state |
| The value the field receives | **Identical.** Same `gson.fromJson(element, genericType)`, resolving from the top of the chain in both designs |
| A null bind | **Identical.** Pre-existing fidelity hole, not widened |
| The binder overwriting the extracted value | **Identical.** The set happens after the binder in both designs |
| `postInit()` seeing the extracted value | **Identical.** `PostInit` stays outermost |
| `@Capture`'s post-`set` map instance | **Changes, and this is the point.** It is the capability the entry exists to create, and no existing site can regress on it |

**Where the real risk is: the write path, which is being re-ordered with zero test coverage in either
module.** `LenientTypeAdapter.write` - all 65 lines, `:86-150` - is unexecuted by all 134 library
tests. `@Extract` appears in exactly one test file and that test only deserializes. There is no
`LenientTests` nest. `MemberDtoMappingTest` has 16 tests and none calls `toJson`.

> **"134/134 and 16/16 after the change" is compatible with all six `@Extract` sites having lost
> round-trip fidelity.** This risk is not reducible by better design. It is reducible only by writing
> the characterisation tests against `7cfc181` first, which is why cycle 0 exists and is not
> skippable.

**Six sites must be regression-checked, not three.** The brief named three and got two of the names
wrong. All six are `@Lenient`-sourced, all six target a `JsonObject` overflow, all six take the
exact-key path unchanged:

| # | Site | Declaration | Source | Target | Why it is on the list |
| --- | --- | --- | --- | --- | --- |
| 1 | `Bestiary.java`:33 | `@Extract("kills.last_killed_mob")` | `kills` `Map<String, Integer>` | `Optional<String>` | The only site the library's one `@Extract` test resembles; `postInit()` reads the same source |
| 2 | `Foraging.java`:30 | `@Extract("treeGifts.milestone_tier_claimed")` | `treeGifts`, `@SerializedName("tree_gifts")` | `ConcurrentMap<String, Integer>` | **Container** target, not scalar |
| 3 | `ChocolateFactory.java`:44 | `@Extract("rabbits.collected_eggs")` | `rabbits` | `ConcurrentMap<String, Long>` | Container target, `Long` values |
| 4 | `ChocolateFactory.java`:46 | `@Extract("rabbits.collected_locations")` | `rabbits` | `ConcurrentMap<String, ConcurrentList<String>>` | **Two `@Extract` fields on one source** - both claims must survive each other |
| 5 | `Loadouts.java`:23 | `@Extract("armorSets.equipped_set")` | `armorSets` `Map<Integer, ArmorSet>`, `@SerializedName("armor")` | `Optional<Integer>` | Reaches overflow because the **key** fails `Integer` conversion. Proves source lookup is by **Java field name**, never `@SerializedName` |
| 6 | `Loadouts.java`:29 | `@Extract("equipmentSets.equipped_set")` | `equipmentSets`, `@SerializedName("equipment")` | `Optional<Integer>` | Plus a third `@Lenient` field with no `@Extract`, whose overflow must stay untouched |

`Loadouts` is the densest site - three `@Lenient` and two `@Extract` on one class - and is the right
shape for the regression fixture.

## 11. Deferred, and why

**`@Owner` / `@Parent` reach-back - deferred by you until after the DTO research pack lands.** Not
designed here, not scheduled here. No entry depends on it, constrains it, or reserves a registration
index for it; there is no placeholder element and no half-specified hook anywhere in the pack. Three
things are recorded only so the deferral reads as a decision:

- It is the one registry candidate that needs a **post-bind lifecycle hook**, which puts every
  consumer's adapter chain in scope, not just hypixel's. That is `large` by definition.
- The eager-versus-lazy question is already settled as **eager**, and eager injection cannot happen
  inside the child's bind, so a post-bind phase is forced.
- This cycle does not foreclose it and makes one piece easier: `ExtractTypeAdapter.read` delegates
  first and then assigns off the built object, which is the same structural position such a hook
  occupies. **Whether a reach-back reuses that slot is exactly the question not to answer early** -
  guessing would put an unused ordering guarantee into `GsonSettings` that the order test would pin.

**`@Capture` value-grouping element - stays declined, on blast-radius grounds.** `README.md` §6.2 of
the DTO pack declined three neighbouring rows. Two of them - "`@Lenient` typed-overflow element" and
"`@Capture` unmatched-key element" - were declined as "one site each", and this pack acts on your
finding that that was a counting error: they are two symptoms of **one** missing capability.
**Value-grouping is not the third symptom.** It concerns bind-side grouping-mode *selection* - how
`CaptureFieldInfo` infers grouped versus entry mode from the declared value type - and has nothing to
do with overflow, with `@Extract`, or with the store. Merging it into that group would be the same
counting error in the opposite direction. Item 4's branch B edits `buildGroupedMap` but touches no
mode inference, adds no `Grouping` constant, and leaves the deliberate two-constant `AUTO`/`ENTRY`
shape alone.

Four more, each with a reason so none reads as an oversight:

| Deferred | Why |
| --- | --- |
| **The enum-adapter-scoped unmatched-key fix** - make `CaseInsensitiveEnumTypeAdapterFactory.read`:82 throw | Reaches roughly forty more fields including `Statistics.java`:89. **Its reach is exactly its blast radius** - `create` claims every enum type in the JVM for every consumer (hypixel 31, `skyblock` 20, `asset-renderer` 90) and no test in either suite would notice. `xlarge`, own cycle, twelve-pin convergence. Explicitly not batchable |
| **`boolean strip()` on `@Extract`** | Additive later, inheriting `literalPrefix`'s limits knowingly rather than by accident. No site now |
| **Array-shaped `@Extract` remainder** | Coherent, zero sites, doubles the new surface. `Dungeons.unlockedJournals` is the module's only collection-shaped `@Lenient` field and carries no `@Extract` |
| **Class-level `@Flatten`** | Would be the first type-level annotation in the library. All eight are `@Target(FIELD)` and every discovery model is a single field walk with `setProcessingSuperclass(false)` |
| **The `PostInit` residue** - null-guard, log, javadoc rewrite | The third payload of the deleted `s20-library-cycle`, outside the five scoped items. It can ride stage 8's commit for free, or stay in the DTO pack. Decision 4 |

## 12. Verification - what to write, and how each test is validated

### 12.1 The stash-and-rerun requirement

Two existing test classes were written this way and each was validated by stashing the source fix and
confirming the tests failed: `CollectionValueCompatibilityTest` (the `c944987` set) and
`CaptureGroupingModeTest` (the `b071689`/`7cfc181` set). **Every library change in this pack owes the
same validation.**

**Form A - characterisation.** Green before, green after. Pins behaviour the change must not alter.
Nothing to stash.

**Form B - defect fix.** Red before, green after. Validation is mechanical:

```bash
# with the test written and the source fix already applied in the working tree
git -C W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras stash push -- src/main/java
gradle_verify gson-extras test          # MUST fail, and MUST fail on the named test only
git -C W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras stash pop
gradle_verify gson-extras test          # MUST pass
```

Record the observed failure message in the commit body. **A form-B test that stays green with the
source stashed is not testing the fix** and must be rewritten before the commit lands.

Form B collides with "green at every stage boundary", and the resolution is mechanical: write the
test at stage 1, **observe the red once locally**, commit it `@Disabled` with the observed failure as
the reason string, and remove the `@Disabled` in the same commit as the fix. The skip count is part of
the tally assertion at every boundary, exactly like the pass count.

### 12.2 The tests that close the real gaps

Written against `7cfc181` **before any library edit**. A test written after the change pins the new
behaviour and proves nothing.

| Test | Closes | Form |
| --- | --- | --- |
| `extractReinjectsOnWrite_ok` | The `@Extract` write path has **zero** coverage anywhere | A |
| `lenientOverflowMergesBackOnWrite_ok` | `@Lenient` merge-back into the field's own sub-object | A |
| `lenientCollectionOverflowMergesBackOnWrite_ok` | The `JsonArray` half of the factory - **single-site** coverage in the whole workspace | A |
| `captureOverflowMergesBackOnWrite_ok` | `@Capture` merge-back into the **root** | A |
| `lenientAndCaptureOverflowGoToDifferentTargets_ok` | **The one test that proves per-entry target tagging works.** Assert the `@Lenient` entry lands in the sub-object and the `@Capture` entry at the root | A |
| `extractDoesNotDuplicateItsOwnKey` | The duplicate-key defect from §3 - red on `7cfc181` | **B** |
| `unmatchedEnumKeyDoesNotCollapse_ok` | Two unmatched keys in one field, asserting the **N-1 loss** rather than one odd key | **B** |
| `markedEnumValuedCaptureOverflowStaysLossless_ok` | The four `@Fallback` companion guards, validated by stashing **only** the guard hunks | **B** |
| `noMarkedConstantIsWireVisible` | Consumer-side structural guard: walk every enum under `response/`, fail if a marked constant carries a `@SerializedName` or an `alternate`, or if its `name()` appears in the fixture's vocabulary | A |
| `defaultFactoryOrderIsStable_ok` | Asserts the exact class list and order of `GsonSettings.defaults()`. Turns an index shift from silent into loud | A |
| `flattenMultiMemberWrapper_roundTrip` | Pins the `@Flatten` write-side loss as a **declared contract** rather than a surprise | A |
| `MemberDtoMappingTest.roundTripsLoadouts` | The consumer side. `Loadouts` is the densest site and the only one the suite already pins | A |

### 12.3 The two verification axes, which answer different questions

Conflating these is the mistake to avoid.

| Axis | How | Costs a cycle |
| --- | --- | --- |
| **Source compatibility across the workspace** | `./gradlew :Simplified-Api:hypixel:test` from `W:/Workspace/Java/Simplified`. The composite substitutes `gson-extras` onto the local project and recompiles **every** sibling from source against the working tree | **No.** This is the per-stage gate |
| **Binary compatibility against published sibling jars** | `gradle_verify hypixel compileJava test --rerun` **standalone in the hypixel directory**, after the re-pin | **Yes.** This is the only reason a cycle exists |

`skyblock`, `client`, `github` and `persistence` reach hypixel as jars compiled against four
different older `gson-extras` shas, and hypixel's `strictly` force-upgrades all four. **A
`NoSuchMethodError` from that force-upgrade is invisible to `compileJava` and invisible to the
composite**, which recompiles the siblings from source and therefore never sees it.

**Step 6 of the cadence is the step people skip.** Prove the library change is behaviour-neutral
standalone **before** any consuming edit - at that point no DTO has changed, so a red build is
unambiguously the library's fault, and the two halves stay separately revertable.

`json_dto_diff.py` is a **diff, not an exit code**: it exits 1 today with 792 unmapped keys. Save the
baseline and compare files. It never constructs a `Gson`, so it cannot detect a factory-behaviour
regression at all.

## 13. Schedule - three cycles, two re-pins

Twelve stages. Only the four dependencies in the last column are real.

| # | Stage | Repo | Cycle | Re-pin | Effort | AI-assisted elapsed | Depends on |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | `sgx-baseline` | both | - | no | `trivial` | 15-25 min | - |
| 1 | `sgx-characterisation` | both | 0 | **no** | `medium` | 3-4 h | 0 |
| 2 | `sgx-overflow-store` | gson-extras | 1 | no | `large` | 4-6 h | 1 |
| 3 | `sgx-extract-filter` | gson-extras | 1 | no | `medium` | 2-3 h | 2 |
| 4 | `sgx-capture-unmatched` | gson-extras | 1 | no | `medium` | 2.5-3.5 h | 2, 3 |
| 5 | `sgx-publish-one` | both | **1 publishes** | **yes** | `small` | 45-75 min | 4 |
| 6 | `sgx-consumer-overflow` | hypixel | - | no | `small` | 1.5-2.5 h | 5 |
| 7 | `sgx-fallback` | gson-extras | 2 | no | `medium` | 2.5-3.5 h | 4 |
| 8 | `sgx-flatten` | gson-extras | 2 | no | `small` | 2-3 h | 7 |
| 9 | `sgx-publish-two` | both | **2 publishes** | **yes** | `small` | 45-75 min | 8 |
| 10 | `sgx-consumer-additive` | hypixel | - | no | `medium` | 3-4 h | 9, plus the naming fixes |
| 11 | `sgx-sibling-convergence` | 12 modules | - | 12 pins | `medium`, optional | 1.5-2.5 h | 9 |

**Totals: 23-33 hours elapsed AI-assisted for stages 0-10, 25-36 hours with stage 11. Human-developer
comparison: 13-17 working days, 14-18 with stage 11.** Both figures are wall-clock elapsed including
verification at each boundary. The ratio is widest on stages 1 and 10 (mechanical and repetitive) and
narrowest on 5 and 9, which are bounded by JitPack in both columns - **do not schedule either as a
filler task expecting it to finish in a gap**.

Why the cycles group the way they do:

- **Cycle 0 must exist as its own checkpoint.** Merging it into cycle 1 means the characterisation
  tests and the change they characterise land on the same sha. Tests are not published, so it costs
  one build and zero re-pins.
- **Cycle 1 cannot be split at a pin boundary.** Stage 2 has zero adoption sites alone and is
  unverifiable end to end; stage 3 is what makes it verifiable; stage 4 is unreadable without both.
- **Cycle 2 is authored on top of cycle 1's tree, not cherry-pickable from it.** Stage 7's four
  companion guards edit `CaptureTypeAdapterFactory` and `LenientTypeAdapterFactory`, and one of them
  is the exact predicate stage 4 rewrites. "Separately revertable" means in reverse order.

**Rollback is two repositories and the order is not negotiable: pin first, library second.** Revert
the pin edit in every consuming module, verify, and only then revert the `gson-extras` commit. Never
revert a library commit while a module is still pinned to that sha. If only the consumer half is
wrong, revert it and leave the pin - the library change is inert without the annotations.

The one thing nothing enforces: **`ExtractTypeAdapterFactory` must nest outside
`CaptureTypeAdapterFactory`.** A factory registered between them, or any downstream SPI /
`GsonContributor` factory - which land outside the whole list - silently reduces `@Extract` to its
current capability with **no test failure**, because all six existing sites are `@Lenient`-sourced and
keep passing. That is what makes stage 2 `large` rather than `medium`.

## 14. Decisions waiting on you

Seven, each with the stage that cannot start without it. Two change source; the rest change
scheduling.

| # | Decision | Needed before | Default if unanswered |
| --- | --- | --- | --- |
| 1 | **Do the three `@Extract` no-op rows throw at `create`?** Misspelled source, superclass source, unannotated source - all three are a silent `continue` today. Throwing is the right diagnostic, but it is a **hard break for any downstream module quietly doing nothing**, and siblings share the pin | stage 2 | keep the `continue` - smaller blast radius |
| 2 | **Is stage 4 branch B full or narrowed?** Full also diverts a group whose **value** fails, giving a body to one of the five silent swallows. Narrow diverts only on `key == null`. Both reach the two grouping-mode sites | stage 4 | full |
| 3 | **Do stages 7 and 8 fold into cycle 1?** One re-pin instead of two, at the cost of a red hypixel run at stage 5 that names no culprit across five items | stage 5 | keep separate |
| 4 | **Does the `PostInit` residue ride stage 8's commit?** Free inside a publish that is happening anyway; otherwise the DTO pack keeps a library cycle for two files | stage 8 | leave it in the DTO pack |
| 5 | **Are `Rarity` and `GameMode` scheduled?** Three field sites behind a chained publish through `Simplified-Api/skyblock`, which pins `2ba8143` against hypixel's `7cfc181` | stage 10 | defer, bundle with `s20-skyblock-election` |
| 6 | **Do the twelve sibling pins converge?** They are three commits behind before this work and five after. Whenever they converge they receive `c944987`, `b071689` and all of this in one step | after stage 9 | converge |
| 7 | **Does anything in the wider workspace actually serialize a `@Lenient`-carrying DTO?** Nothing in either suite executes `LenientTypeAdapter.write`. If no production caller does either, the round-trip fidelity item 1 is protecting is theoretical and its price should be re-argued. Resolved by a `toJson` search over `response/skyblock` types in the downstream `dev.sbs` modules | stage 2, and worth doing **before** pricing it | assume it matters |

## 15. The detail docs

| File | What it is | Go there for |
| --- | --- | --- |
| `00-verified-facts.md` | Read-only fact base. Every claim read off source with `file:line`, not recalled | The six brief corrections (§1); the nesting derivation from gson source (§2.1); line-exact call-order traces for both factories (§3, §4); the interleaved read trace (§5); `WeakIdentityMap`'s complete four-method surface (§7); the empty-catch inventory (§9); every adoption site (§10); **the adversarial regression baseline, 35 numbered invariants (§11)** |
| `01-overflow-extract.md` | Design entries 1 and 2, written as one file because the second is meaningless without the first | Full `Overflow` source (§2.3); what changes line-by-line in both existing factories (§2.5); the set-after-build equivalence argument (§2.7); the write-path relocation (§2.8); ten failure modes (§2.9); **the two cheaper alternatives, both named and both losing (§2.10)**; mode selection and the three rejected combinations (§3.3); why no stripping (§3.4); the six-site regression checklist (§4); six risks not argued away (§5) |
| `02-fallback.md` | Design entry 5a | The five deltas against the research pack (§2); the four-hunk factory edit (§4); **the map-key path worked case by case, including the one place an earlier draft was wrong (§6.2)**; the eligibility rule (§6.3); which enums get the marker, in seven groups (§7); the unchanged-behaviour guarantee (§8); the fifteen-row failure table (§11); six conditions on the acceptance (§13) |
| `03-flatten.md` | Design entry 5b | The four corrections against the pack (§1); read and write paths in full (§3, §4); **the registration slot derived from a constraint table rather than from intuition (§5)**; `Currencies.essence` traced against the fixture (§6); `@JsonAdapter` re-examined now the cycle is sunk (§7); the three `@Capture` compositions (§8); why `@Lenient` cannot pair in either order (§9); the round-trip bar it does not clear (§12); three-level rollback (§15) |
| `04-compatibility.md` | Blast radius across the workspace | Every module referencing `dev.simplified.gson` (§2); **why the composite hides the pin and that is good news (§2.3)**; version resolution and the four forced upgrades (§3); the full adoption inventory, 56 fields (§4); the 134 tests by file and **the gap list of affected paths with zero coverage (§6.3)**; why the two consumer-side checks cannot see behaviour (§7); the pre-flight plan (§8); JitPack cadence and the batch-versus-staged cost table (§9) |
| `10-design-entries.md` | All six entries in the `00-conventions.md` §3 block format, self-contained | The canonical entry blocks; **how the five fit together, with the composed factory table and the who-touches-what map (§7)**; the deferral records for `@Owner`/`@Parent` and value-grouping (§8) |
| `20-implementation-order.md` | The executable blueprint. No design argument - every "why" is a pointer, every "what" is a step | What it supersedes in the DTO plan (§2); the stage map (§3); **the seven-step cadence for one cycle (§4)**; the test-first rule and stash-and-rerun (§5); stages 0-11 in full (§6-17); the per-boundary tally table (§18); what it unblocks in the DTO plan (§19); estimates (§20); the rollback matrix (§21); sixteen execution risks ranked by how badly a green suite would lie about them (§23) |

**Reading order for implementation:** `20-implementation-order.md` end to end, then the entry file for
whichever stage is next. `00-verified-facts.md` §11 is the checklist to re-read before any edit to
`LenientTypeAdapterFactory` or `CaptureTypeAdapterFactory`.

**Cross-references into the DTO pack**, none of which this pack contradicts without saying so:
`notes/json-annotations/10-annotation-designs.md` §3 (the entry format), §6 (`@Flatten`), §7
(`@Fallback`), §9 (the declined `@Capture` unmatched-key policy), §11 (the declined `@Lenient` typed
overflow); `notes/json-annotations/00-conventions.md` §4 (the effort scale), §6 (the registry);
`notes/json-annotations/README.md` §6.2 (the rejection table).
