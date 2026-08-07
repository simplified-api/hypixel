# 10 - Design entries

## 1. How to read this document

This is the decision document for the `gson-extras` cycle that runs **before** the DTO work in
`notes/json-annotations/`. It supersedes that pack's `s20-library-cycle`, which had the library last;
the owner reversed the order so the DTO stages can consume the result.

Five entries, in two groups.

- **`dgx-overflow-store`, `dgx-extract-filter`, `dgx-capture-unmatched`** (§2, §3, §4) are **one
  change**, not three. The first builds a shared overflow store and moves `@Extract` into its own
  factory; the second gives `@Extract` the selection axis it has never had; the third is a defect that
  the first two turn from unfixable-without-loss into a one-branch correction. They ship in one commit,
  one JitPack build and one re-pin. Reading §4 without §2 and §3 makes it look like the `@Capture` edit
  the research pack declined, which is exactly what it is not.
- **`dgx-fallback`, `dgx-flatten`** (§5, §6) are the two the research pack already accepted. They are
  additive, behaviourally independent of the first group, and separable at the pin boundary - but
  **not file-disjoint from it**: §5's four companion guards edit the same two factories §2, §3 and §4
  rewrite, so cycle 2 is authored on top of cycle 1's tree (§7).

Every entry opens with the field block defined in `10-annotation-designs.md` §3, using the `Category`
and `Effort` vocabularies from `00-conventions.md` §5 and §4. **Adopt** means build it. **Adopt
narrowly** means build it with a smaller surface than the concept implies, and the entry names each
cut. **Decline** means the concept is sound and the evidence does not pay for it now. **Reject** means
something that already ships dominates it.

Supporting documents, and what each is authoritative for:

| File | Authoritative for |
| --- | --- |
| `00-verified-facts.md` | Every library internal cited here - factory nesting, call-order traces, both overflow stores, `WeakIdentityMap`, the adoption inventory, the regression baseline |
| `01-overflow-extract.md` | The long-form working of §2 and §3, including the `Overflow` source, the write-path relocation argument and the six-site regression checklist |
| `02-fallback.md` | The long-form working of §5, including the enum eligibility rule and the case-by-case working of the `MapTypeAdapterFactory` path |
| `03-flatten.md` | The long-form working of §6 |
| `04-compatibility.md` | Blast radius, cross-module consumers, version resolution, the test-coverage gap list and the JitPack cadence |

**One correction to carry, because everything downstream depends on it.** The brief that seeded this
cycle states that earlier registration means the outer adapter. It is backwards.
`GsonBuilder.create()` reverses the user factory list (gson 2.11.0 `GsonBuilder.java`:887-890), so the
**last** registered factory is the **outermost**. `@Capture` therefore wraps `@Lenient`, not the
reverse (`00-verified-facts.md` C1). No entry here states a registration index without first stating
the nesting depth it wants and deriving the index from it.

**Two things are out of scope and are not designed anywhere in this document.** `@Owner` / `@Parent`
reach-back is deferred by the owner until after the research pack lands (§8.1). The `@Capture`
value-grouping element stays declined on blast-radius grounds and is not part of the overflow group
(§8.2).

## 2. dgx-overflow-store - shared `Overflow` and `ExtractTypeAdapterFactory`

- **Registry entry:** none - library-internal, no new annotation and no new element
- **Verdict:** adopt
- **Category:** `correctness`
- **Answers findings:** none on its own. It is the enabler for `f03-questrewards-mixed-values`
  (through `dgx-extract-filter`) and `f06-capture-null-enum-key` (through `dgx-capture-unmatched`)
- **Cheaper alternative:** none - the two candidates are named and both lose (§2.6)
- **Library change:** existing factory edit, plus two additive files
- **Adoption sites today:** 0 - no consumer names it; it is what the next two entries stand on
- **Effort:** `large`

### 2.1 The problem it removes and the real sites

Two overflow stores exist and neither is readable by the one thing that wants to read them.

```
LenientTypeAdapterFactory.java:62   private static final WeakIdentityMap<Object, JsonElement> OVERFLOW
CaptureTypeAdapterFactory.java:82   private static final WeakIdentityMap<Object, JsonObject>  OVERFLOW
```

**On the read path both are write-only.** `@Lenient` publishes at `:239`, `@Capture` publishes at
`:387`, and the only `get` on either is on the write path - `LenientTypeAdapterFactory.java`:127 and
`CaptureTypeAdapterFactory.java`:239. `@Extract`'s claim reads neither. It searches a **method-frame
local** allocated at `LenientTypeAdapterFactory.java`:162:

```java
FieldOverflow sourceOverflow = overflows.stream()
    .filter(o -> o.fieldName().equals(extractInfo.getSourceFieldName()))
    .findFirst()
    .orElse(null);
```

That list is filled by `@Lenient`'s own filter phase and by nothing else, so a union of the two static
maps would change nothing at all: the extract phase at `:206` would still be looking at `overflows`
and would still find only `@Lenient` field names in it. **Merging the stores is not the fix, and the
reason is not the one the brief gives.**

The brief attributes the failure to ordering - "`@Extract` runs BEFORE the delegate, so `@Capture`'s
overflow does not exist yet". `00-verified-facts.md` C1 and C2 establish that the nesting is inverted
from the brief's premise and that the content **does** exist. `@Capture` is outer, its classify pass
(`:311-363`) has finished filling `overflowMaps` before it calls `delegateAdapter.fromJsonTree` at
`:366`, and that call is what eventually enters `@Extract`'s phase. Two things, neither of them
ordering, stop the claim reaching it:

1. `overflowMaps` is a frame-local (`CaptureTypeAdapterFactory.java`:268) in a frame `@Extract` is
   nested **inside**, with no channel between them.
2. The identity key the store wants - the built `Map` - does not exist yet. It is constructed at
   `:377`/`:379` and installed at `:381`, all **after** the delegate returns at `:366`.

Point 2 is what rules out publishing early: `@Capture` cannot key an identity-keyed store before the
identity exists. This is a **publication and lifetime** problem, not an ordering one, which is what
takes a `GsonSettings` reorder - `xlarge` by `00-conventions.md` §4 - off the table entirely
(`00-verified-facts.md` C3).

**The real sites, and there are more of them than the brief said.** Six `@Extract` fields, not three
(`00-verified-facts.md` C4). All six source from a `@Lenient` map by Java field name; the brief named
two `ChocolateFactory` fields that do not exist.

| Site | Declaration | Source | Target |
| --- | --- | --- | --- |
| `member/Bestiary.java`:33 | `@Extract("kills.last_killed_mob")` | `kills` | `Optional<String>` |
| `member/foraging/Foraging.java`:30 | `@Extract("treeGifts.milestone_tier_claimed")` | `treeGifts` | `ConcurrentMap<String, Integer>` |
| `member/hoppity/ChocolateFactory.java`:44 | `@Extract("rabbits.collected_eggs")` | `rabbits` | `ConcurrentMap<String, Long>` |
| `member/hoppity/ChocolateFactory.java`:46 | `@Extract("rabbits.collected_locations")` | `rabbits` | `ConcurrentMap<String, ConcurrentList<String>>` |
| `member/Loadouts.java`:23 | `@Extract("armorSets.equipped_set")` | `armorSets` | `Optional<Integer>` |
| `member/Loadouts.java`:29 | `@Extract("equipmentSets.equipped_set")` | `equipmentSets` | `Optional<Integer>` |

The regression surface is therefore twice what the brief implied, and it is entirely `@Lenient`-sourced
- **no `@Extract` in the workspace names a `@Capture` field**, because until now doing so has been a
silent no-op.

### 2.2 What the shared `Overflow` type holds

**Not a map union.** The two producers merge back into different places and both are correct for
their own semantics:

| Producer | Merge-back target | Site |
| --- | --- | --- |
| `@Lenient` | the field's **own sub-object**, located by serialized name or `@SerializedPath` | `LenientTypeAdapterFactory.java`:132 |
| `@Capture` | the **root object**, or the nested node a descending capture reads | `CaptureTypeAdapterFactory.java`:242-244 |

`@Extract` removes a claimed entry on read (`:216`) and re-injects it on write (`:108-111`), so it is
not a pure reader - it participates in round-trip fidelity. A plain union would re-inject a
`@Capture`-sourced key into a `@Lenient` field's sub-object, which is silently the wrong place and is
visible only in a serialize test. Every entry therefore carries the target that produced it.

**One correction to what "round-trip fidelity" can mean here, because the rest of this entry depends on
it.** Re-injection puts the entry back in the right place, but it is only half of a round-trip, and the
other half does not work today: nothing removes the `@Extract` field's **own** serialized key from the
tree the delegate produced, so a read-then-write emits the value **twice** - once at the root under the
field name, once inside the source (`00-verified-facts.md` §3.2 W1a). `@Capture` does not have this
problem because `CaptureTypeAdapterFactory.java`:181 removes its field's key explicitly. §2.3 adds the
same removal to `ExtractTypeAdapter.write`; until that paragraph, read every "round-trips" in this
document as a claim about the **overflow entry**, not about the emitted document.

`Overflow` is package-private in `dev.simplified.gson.factory`, alongside `WeakIdentityMap`, which is
package-private today (`WeakIdentityMap.java`:29) and needs no visibility bump.

```java
package dev.simplified.gson.factory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.simplified.collection.Concurrent;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Side channel holding the JSON entries a bound container could not take, so a later write of that
 * same container can put them back where they came from.
 * <p>
 * Both {@link Lenient @Lenient} and {@link Capture @Capture} fill it, and {@link Extract @Extract}
 * reads from either without knowing which. Entries are keyed by the identity of the container the
 * bind produced, so a caller that mutates that container afterwards still finds its overflow, and an
 * entry disappears once its container does.
 * <p>
 * Each entry carries its own write target, because the two producers merge back into different
 * places and a claim has to be returnable to the one it came from.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Overflow {

    private static final WeakIdentityMap<Object, Entry> ENTRIES = new WeakIdentityMap<>();

    /**
     * Where a write puts an overflow back.
     */
    enum Target {

        /** the owning field's own JSON element, located by serialized name or serialized path */
        FIELD_ELEMENT,

        /** the object the entries were classified out of - the enclosing object, or the nested node a descending capture reads */
        SOURCE_OBJECT

    }

    /**
     * One container's overflow together with the target that produced it.
     *
     * @param target where a write merges this overflow back
     * @param element the entries themselves - a JSON object for a map-shaped owner, a JSON array for
     *     a collection-shaped one
     */
    record Entry(@NotNull Target target, @NotNull JsonElement element) { }

    /**
     * Stores the overflow belonging to a bound container, replacing whatever it held before.
     *
     * @param owner the container the bind produced
     * @param target where a write merges this overflow back
     * @param element the overflowed entries
     */
    static void publish(@NotNull Object owner, @NotNull Target target, @NotNull JsonElement element) {
        ENTRIES.put(owner, new Entry(target, element));
    }

    /**
     * Returns the overflow a container holds for the given target, or {@code null} when it holds none
     * or holds one another producer put there.
     *
     * @param owner the container to look up
     * @param target the target the caller merges into
     * @return the overflowed entries, or {@code null} if the container holds none for that target
     */
    static @Nullable JsonElement find(@NotNull Object owner, @NotNull Target target) {
        Entry entry = ENTRIES.get(owner);
        return entry != null && entry.target() == target ? entry.element() : null;
    }

    /**
     * Returns the overflow a container holds, storing and returning a supplied empty one when it
     * holds none.
     * <p>
     * The first publisher decides the target - a container already holding an overflow keeps it, and
     * the supplied target is ignored rather than overwriting one a producer is relying on.
     *
     * @param owner the container to look up
     * @param target the target to record if nothing is stored yet
     * @param ifAbsent produces the empty container to store when nothing is stored yet
     * @return the overflowed entries the container now holds
     */
    static @NotNull JsonElement open(@NotNull Object owner, @NotNull Target target, @NotNull Supplier<JsonElement> ifAbsent) {
        return ENTRIES.computeIfAbsent(owner, () -> new Entry(target, ifAbsent.get())).element();
    }

    /**
     * Removes and returns the overflowed entry stored under one key.
     *
     * @param owner the container to claim from
     * @param key the JSON key to claim
     * @return the claimed element, or {@code null} if the container holds no object-shaped overflow
     *     or no entry under that key
     */
    static @Nullable JsonElement claim(@NotNull Object owner, @NotNull String key) {
        Entry entry = ENTRIES.get(owner);

        if (entry == null || !entry.element().isJsonObject())
            return null;

        return entry.element().getAsJsonObject().remove(key);
    }

    /**
     * Removes and returns every overflowed entry whose key the given filter accepts.
     *
     * @param owner the container to claim from
     * @param filter decides which keys to claim
     * @return the claimed entries under their original keys, empty when nothing matched
     */
    static @NotNull JsonObject claim(@NotNull Object owner, @NotNull Predicate<String> filter) {
        JsonObject claimed = new JsonObject();
        Entry entry = ENTRIES.get(owner);

        if (entry == null || !entry.element().isJsonObject())
            return claimed;

        JsonObject overflow = entry.element().getAsJsonObject();

        for (String key : Concurrent.newList(overflow.keySet())) {
            if (filter.test(key))
                claimed.add(key, overflow.remove(key));
        }

        return claimed;
    }

    /**
     * Puts a claimed entry back, for a claim that could not be converted into its target field.
     *
     * @param owner the container to restore into
     * @param key the JSON key the entry was claimed under
     * @param element the claimed element
     */
    static void restore(@NotNull Object owner, @NotNull String key, @NotNull JsonElement element) {
        Entry entry = ENTRIES.get(owner);

        if (entry != null && entry.element().isJsonObject())
            entry.element().getAsJsonObject().add(key, element);
    }

    /**
     * Returns the number of containers still holding an overflow.
     *
     * @return the live entry count
     */
    static int size() {
        return ENTRIES.size();
    }

}
```

Four API notes, each checked against source rather than recalled:

- `WeakIdentityMap` exposes exactly `get`, `put`, `computeIfAbsent` and `size` (`:40`, `:51`, `:67`,
  `:83`). `Overflow` uses all four and adds nothing to it. The removal a claim needs is a removal
  **from the stored `JsonObject`**, not from the map, so the missing `WeakIdentityMap.remove` that
  `00-verified-facts.md` §7 flagged is never needed and the class `WeakIdentityMapTest` covers is not
  touched.
- `claim(owner, Predicate)` snapshots `keySet()` through `Concurrent.newList(Collection)` before
  removing. Iterating a `JsonObject`'s entry set while removing from it is a
  `ConcurrentModificationException`; the snapshot is not optional.
- The two `claim` overloads differ in their second parameter type, so every call site resolves without
  a cast. They are kept separate rather than folded together because one returns an element and the
  other returns an object of entries, and collapsing them would force every caller to unwrap.
- `restore` is the one operation with no equivalent today. Today a claim that fails to convert is lost
  from the object **and** from the document - removed at `:216`, swallowed at `:246-247`. Four lines
  buy that back.

### 2.3 Lifting `@Extract` into `ExtractTypeAdapterFactory`

**Do not make `@Extract` run earlier - make it run later.** Its phase moves out of
`LenientTypeAdapterFactory` into a factory nested **outside** `CaptureTypeAdapterFactory`, so it
begins after both producers have published. At that point both identity keys exist, both store entries
exist, and both are reachable by reflection off the object that has just been built. No read-scoped
channel, no `ThreadLocal`, no reorder of the existing factories.

**`LenientTypeAdapterFactory` loses about ninety lines and one of the library's five silent swallows.**

| Site | Now | After |
| --- | --- | --- |
| `:62` | `private static final WeakIdentityMap<Object, JsonElement> OVERFLOW` | deleted |
| `:68`, `:70-72` | `create` scans both `@Lenient` and `@Extract` and wraps when **either** list is non-empty | scans `@Lenient` only, wraps only when that list is non-empty |
| `:98-118` | write-side `@Extract` re-injection | deleted, moves |
| `:127` | `OVERFLOW.get(collection)` | `Overflow.find(collection, Target.FIELD_ELEMENT)` |
| `:203-221` | read-side extract phase | deleted, moves |
| `:239` | `OVERFLOW.put(collection, fieldOverflow.overflow())` | `Overflow.publish(collection, Target.FIELD_ELEMENT, fieldOverflow.overflow())` |
| `:242-248` | post-assign `@Extract`, including the empty catch at `:246-247` | deleted, moves, with `restore` in the catch |
| `:377`, `:456-496` | `ExtractClaim` record and `ExtractFieldInfo` | move to the new factory |

The `FieldOverflow` record and the whole filter phase are untouched. One behavioural consequence, and
it is an improvement: a class carrying `@Extract` but no `@Lenient` currently still builds a
`LenientTypeAdapter` (`:70`), which buffers the whole subtree to a `JsonElement` and back for an
extract phase that can never match anything. After the change such a class gets the delegate handed
straight back. No existing site is that shape.

**`CaptureTypeAdapterFactory` loses three lines.**

| Site | Now | After |
| --- | --- | --- |
| `:82` | `private static final WeakIdentityMap<Object, JsonObject> OVERFLOW` | deleted |
| `:239` | `JsonObject overflow = OVERFLOW.get(mapObj)` | `JsonElement overflow = Overflow.find(mapObj, Target.SOURCE_OBJECT)`, then the existing loop iterates `overflow.getAsJsonObject().entrySet()` |
| `:386-387` | `if (overflow.size() > 0) OVERFLOW.put(capturedMap, overflow)` | `if (!overflow.isEmpty()) Overflow.publish(capturedMap, Target.SOURCE_OBJECT, overflow)` |

The publish policies stay **different**. `@Lenient` publishes unconditionally, even for an empty
overflow (`:236-239`); `@Capture` publishes only when non-empty (`:386`). Unifying either way changes
behaviour a consumer can observe (`00-verified-facts.md` C1, E-series), so the store tolerates absence
and each caller keeps its own policy.

**Before and after at a real site - `member/Loadouts.java`, the densest `@Lenient`/`@Extract` class in
the module.** The DTO does not change. That is the point of this entry, not an omission:

```java
@Lenient
@SerializedName("armor")
private @NotNull ConcurrentMap<Integer, ArmorSet> armorSets = Concurrent.newMap();
@Extract("armorSets.equipped_set")
private @NotNull Optional<Integer> equippedArmorSet = Optional.empty();
```

What changes is underneath it. Before, one `LenientTypeAdapter` filters `armor`, claims
`equipped_set` out of a frame-local, and assigns the field, all inside one `read`. After, the same
`LenientTypeAdapter` filters and publishes, and an `ExtractTypeAdapter` one nesting level further out
claims `equipped_set` from the store by the identity of the bound `armorSets` map and assigns the
field. `equipped_set` reaches overflow because the **key** fails `Integer` conversion, not because the
value type is wrong, and that is unchanged. The bound value is identical, the emitted document is
identical, and the only observable difference is that the same annotation would now also work if
`armorSets` carried `@Capture`.

**The read path buffers nothing; the write path buffers once, and it has to.** On read `@Extract` never
inspected the tree - it inspected the overflow, which by construction is the part of the tree the
producer already removed - so `read` needs no `jsonElementAdapter` round-trip and does not take one. On
write it does, because of W1a: the delegate's tree still carries the `@Extract` field's own serialized
key and that key has to come out. `ExtractTypeAdapter` therefore holds the same
`gson.getAdapter(JsonElement.class)` the other five tree-rewriting factories hold, and `write` is
re-injection **before** the delegate plus key removal **after** it. The `isRemainder()` branches are
`dgx-extract-filter`'s and are specified in §3.4; everything else is the current code, relocated.

```java
@Getter
@RequiredArgsConstructor
private static class ExtractTypeAdapter<T> extends TypeAdapter<T> {

    private final @NotNull Gson gson;
    private final @NotNull TypeAdapter<T> delegateAdapter;
    private final @NotNull TypeAdapter<JsonElement> jsonElementAdapter;
    private final @NotNull ConcurrentList<ExtractFieldInfo> extractFields;

    @Override
    public void write(@NotNull JsonWriter out, @Nullable T value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        // Re-inject before delegating - every producer's merge-back runs inside this call
        for (ExtractFieldInfo info : this.getExtractFields()) {
            Object extractValue = info.getAccessor().get(value);
            Object owner = info.getSourceAccessor().get(value);

            if (extractValue == null || owner == null || !info.isMapSource())
                continue;

            JsonObject overflow = Overflow.open(owner, info.getTarget(), JsonObject::new).getAsJsonObject();
            JsonElement tree = this.getGson().toJsonTree(extractValue);

            if (!info.isRemainder())
                overflow.add(info.getJsonKey(), tree);
            else if (tree.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : tree.getAsJsonObject().entrySet())
                    overflow.add(entry.getKey(), entry.getValue());
            }
        }

        JsonElement jsonTree = this.getDelegateAdapter().toJsonTree(value);

        if (!jsonTree.isJsonObject()) {
            this.getDelegateAdapter().write(out, value);
            return;
        }

        // The extracted value belongs in its source, not beside it
        JsonObject jsonObject = jsonTree.getAsJsonObject();

        for (ExtractFieldInfo info : this.getExtractFields())
            jsonObject.remove(info.getSerializedName());

        this.getJsonElementAdapter().write(out, jsonObject);
    }

    @Override
    public @Nullable T read(@NotNull JsonReader in) throws IOException {
        T value = this.getDelegateAdapter().read(in);

        if (value == null)
            return null;

        for (ExtractFieldInfo info : this.getExtractFields()) {
            Object owner = info.getSourceAccessor().get(value);

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

    private void assign(@NotNull T value, @NotNull ExtractFieldInfo info, @NotNull Object owner, @NotNull JsonElement claimed) {
        try {
            Object converted = this.getGson().fromJson(claimed, info.getAccessor().getGenericType());
            info.getAccessor().set(value, converted);
        } catch (Exception ex) {
            if (info.isRemainder() && claimed.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : claimed.getAsJsonObject().entrySet())
                    Overflow.restore(owner, entry.getKey(), entry.getValue());
            } else
                Overflow.restore(owner, info.getJsonKey(), claimed);
        }
    }

}
```

`create` returns `null` when the class carries no `@Extract`, matching the six factories that already
do and avoiding the `getDelegateAdapter` hazard `00-verified-facts.md` A7 describes - the two factories
that hand back the delegate instead change which factory a **third** factory's `getDelegateAdapter`
resolves to:

```java
@Override
public <T> @Nullable TypeAdapter<T> create(@NotNull Gson gson, @NotNull TypeToken<T> typeToken) {
    ConcurrentList<ExtractFieldInfo> extractFields = ExtractFieldInfo.of(typeToken.getRawType());

    return extractFields.isEmpty()
        ? null
        : new ExtractTypeAdapter<>(gson, gson.getDelegateAdapter(this, typeToken), gson.getAdapter(JsonElement.class), extractFields);
}
```

`ExtractFieldInfo` moves out of `LenientTypeAdapterFactory`:456-496 and gains five resolved
properties, all computed once per type at `create` from the same `Reflection<>(clazz)` scan with
`setProcessingSuperclass(false)` it already uses.

| Property | Resolved from |
| --- | --- |
| `sourceAccessor` | the `FieldAccessor` whose `getName()` equals `sourceFieldName` - **Java field name**, never `@SerializedName`. Four of the six sites depend on that distinction (`armorSets` versus `@SerializedName("armor")`) |
| `serializedName` | the `@Extract` field's own `@SerializedName` value if it carries one, else its Java field name - the same two-line resolution `LenientFieldInfo`:394 and `CaptureFieldInfo` already make. This is the key `write` removes |
| `target` | `FIELD_ELEMENT` when the source field carries `@Lenient`, `SOURCE_OBJECT` when it carries `@Capture` |
| `mapSource` | whether the source field's type is `Map`-assignable, the same test `LenientTypeAdapterFactory.java`:108 makes today through `lenientInfo.isMap()` |
| `remainder` / `pattern` | §3.4 |

Guarding on `isMapSource()` **before** `open` rather than after it fixes a live defect in passing.
Today `:108` calls `computeIfAbsent` and installs a `JsonArray` for a collection-shaped source, then
`:110`'s `isJsonObject()` guard drops the value on the floor - the entry is created, never used, and
the empty array is left in the static store as a side effect (`00-verified-facts.md` W3a, E5). No site
exercises it; it is free to fix while the code is in hand.

**The key removal is a second live defect fixed in passing, and it is the one behaviour change this
entry makes to the emitted document.** Today every `@Extract` field is serialized **twice** - the
reflective binder emits it at the root because it is a non-transient field, and re-injection puts it
back inside its source as well (`00-verified-facts.md` §3.2 W1a). `Bestiary` emits `"lastKilledMob"`
beside `kills.last_killed_mob`; `ChocolateFactory` emits `"eggs"` and `"locations"` beside
`rabbits.collected_eggs` and `rabbits.collected_locations`. Three things make removing it the right
call rather than a risk:

- **It is the treatment `@Capture` already gives its own field**, at `CaptureTypeAdapterFactory.java`:181,
  for exactly the same reason: the delegate serialized the field as the model shapes it, and that is not
  the wire shape. `@Extract` is the only overflow-participating annotation that never learned to do it.
- **The duplicate is unreachable by any current test**, so nothing pins it. The one `@Extract` test
  (`GsonFactoryTest`:2227) only deserializes and `MemberDtoMappingTest` never calls `toJson`, which is
  how a defect this visible survived (`04-compatibility.md` §6.3 G1-G5). §2.7 turns that into the one
  step-1 test that is deliberately **red** before the change.
- **`remove` on a key that is not there is a no-op**, so a `@SerializedPath`-located or otherwise absent
  `@Extract` field costs one failed lookup and changes nothing.

The one hazard is a collision: an `@Extract` field whose serialized name is also a real wire key that
some other field or a `@Capture` catch-all legitimately writes at the root. Removal is unconditional, so
that key would be dropped. No site is shaped that way - all six serialized names (`lastKilledMob`,
`claimedMilestoneTiers`, `eggs`, `locations`, `equippedArmorSet`, `equippedEquipmentSet`) are Java
identifiers with no `@SerializedName`, and the wire keys are snake_case - and it is the identical hazard
`@Capture` has carried at `:181` since it shipped. Recorded in §2.5 rather than guarded against.

### 2.4 Ordering and interaction with the existing factories

**State the nesting depth, then derive the index** (`00-verified-facts.md` A1). `@Extract` must run
after both producers have published, so it must be **outer to `CaptureTypeAdapterFactory`**, which is
itself outer to `LenientTypeAdapterFactory`. It must stay **inner to `PostInitTypeAdapterFactory`** so
a `postInit()` body still observes extracted values. Outer to `Capture` means registered **after**
`Capture`, because registration index runs opposite to outerness.

```java
.withFactories(
    new CaseInsensitiveEnumTypeAdapterFactory(),
    new OptionalTypeAdapterFactory(),
    new SplitTypeAdapterFactory(),
    new SerializedPathTypeAdaptorFactory(),
    new LenientTypeAdapterFactory(),
    new CaptureTypeAdapterFactory(),
    new ExtractTypeAdapterFactory(),
    new CollapseTypeAdapterFactory(),
    new PostInitTypeAdapterFactory()
)
```

Resulting nesting, outermost first: `PostInit`, `Collapse`, **`Extract`**, `Capture`, `Lenient`,
`SerializedPath`, `Split`. `Collapse` stays outer to `Capture` and `Lenient` stays inner to `Capture`,
so both dark-ordering pairs the suite pins (`GsonFactoryTest`:1979-2166 and `:2168-2253`) keep their
relative order. The class javadoc at `GsonSettings.java`:215-218 lists the factories and gains the new
one.

**The write direction, and an objection this entry has to answer rather than route around.**
`04-compatibility.md` §5.3 argues that no registration index satisfies both directions: `Extract`
inner to `Lenient` fixes write and breaks read; `Extract` outer to `Lenient` fixes read and breaks
write, because `Lenient`'s merge-back completes before `Extract` gets a turn and the claimed key is
stored for the *next* write and missing from this one. That derivation is correct **for a factory that
post-processes the tree**, and every existing factory in the library does exactly that.

`ExtractTypeAdapter` does not - and specifically, **the half of its write that has to happen early
touches no tree at all.** Re-injection mutates the overflow container in the **store**, so it has no
reason to wait for one. The key removal does need the tree, but it needs it *after* every producer has
merged, which is exactly where an outer factory sits. The two halves want opposite ends of the same
call, and an outer factory is the only position that offers both:

```
Extract.write        re-injects into the store, THEN asks the delegate for a tree
  Capture.write      entered second
    Lenient.write    entered third
      ... reflective binder produces the flat tree ...
    Lenient post-processes   <-- reads an already-restored container
  Capture post-processes     <-- reads an already-restored container
Extract              removes its own fields' keys from the returned tree, then writes it
```

Each producer then puts each entry in its own correct place - `@Lenient` into the field's sub-object
through `locateElement` (`:132`), `@Capture` into the root or the descend node (`:242-244`). That is
also why per-entry target **tagging** is enough and no target *routing* is needed: `@Extract` never
decides where an entry lands, it hands the entry back to the producer that owns it, and the producer
already knows. So `04-compatibility.md` §5.3's conclusion holds for its premise and its premise does
not hold here; the full lift is viable and the "read half only, and rename the factory" fallback it
proposes is not needed.

Traced for `Loadouts`, whose two `Optional<Integer>` fields came out of two different `@Lenient` maps:

```
Extract.write(loadouts)
  equippedArmorSet = Optional.of(2)   -> owner = the live armorSets map
     Overflow.open(armorSets, FIELD_ELEMENT, JsonObject::new)
     overflow.add("equipped_set", 2)
  equippedEquipmentSet likewise into the equipmentSets overflow
  delegate.write ->
    Capture.write   - Loadouts has no @Capture field, so create returned null and this frame
                      does not exist
    Lenient.write
      :92   toJsonTree -> {"armor": {...}, "equipment": {...}, "loadouts": {...}}
      :121  merge-back: Overflow.find(armorSets, FIELD_ELEMENT) now carries "equipped_set"
      :132  locateElement -> the "armor" sub-object
      :137  armor["equipped_set"] = 2
  tree returned = {"armor": {..., "equipped_set": 2}, "equipment": {...}, "loadouts": {...},
                   "equippedArmorSet": 2, "equippedEquipmentSet": 4}
  remove "equippedArmorSet", remove "equippedEquipmentSet"
```

**Today's output minus two root keys that were never in the input**, which is the §2.3 fix and the
single intended difference. Everything the input carried is byte-identical and in the same place; what
disappears is the pair of duplicates the reflective binder emits beside them. A `@Capture`-sourced claim
differs only in which producer picks the entry up: `Overflow.find(capturedMap, SOURCE_OBJECT)` at
`CaptureTypeAdapterFactory.java`:239, merged into the root at `:247-248` under the original unstripped
key, with `literalPrefix` correctly not applied because overflow keys were never stripped
(`00-verified-facts.md` D3).

Per-factory interaction, stated so nothing has to be inferred:

| Factory | Interaction |
| --- | --- |
| `PostInit` | Stays outermost. `@Extract` still completes before any `postInit()` body, which is the guarantee six implementors rely on today only because `@Extract` sits far deeper |
| `Collapse` | None. It rewrites list-mode fields into arrays before delegating and carries no overflow. It stays outer to `Extract`, so a `@Collapse` value class's own `@Extract` fields are handled inside the collapse, as they are today |
| `Capture` | Publishes into the shared store during its post-assign, which is inside `Extract`'s delegate call. This is the whole point of the placement |
| `Lenient` | Publishes during its own post-assign, two levels in. Unchanged apart from the store call |
| `SerializedPath` | Out of chain in both directions - it binds its fields with a fresh top-of-chain `gson.fromJson` (`:132`), so an `@Extract` field that is also `@SerializedPath`-located never traverses this chain (`00-verified-facts.md` A4). No site does that |
| `Split` | None. It removes its key before delegating; `@Extract` reads no keys |
| SPI / `GsonContributor` factories | `GsonSettings.defaults()` appends them (`:259-263`), so they nest **outside** `ExtractTypeAdapterFactory`. The ordering guarantee is not enforceable against them and this entry does not claim it is (`00-verified-facts.md` F3) |

### 2.5 Failure modes and malformed input

| # | Condition | Today | Proposed |
| --- | --- | --- | --- |
| 1 | `@Extract` names a field that does not exist on the class | silent no-op - `:206-212` finds no `FieldOverflow` and continues | `JsonException` from `create`, once per type |
| 2 | `@Extract` names a field on a **superclass** | silent no-op - `setProcessingSuperclass(false)` (`:481`) hides it | same `JsonException` |
| 3 | `@Extract` names a field carrying neither `@Lenient` nor `@Capture` | silent no-op | `JsonException` from `create` - there is no target to derive, and nothing would ever publish for that field |
| 4 | The source is a grouping-mode `@Capture` field | unreachable today | **silent permanent no-op**, documented on the annotation. Grouping mode produces no overflow at all (`00-verified-facts.md` D6) and §4.3 is where that gets addressed |
| 5 | A claim fails to convert | entry removed at `:216`, failure swallowed at `:246-247`, lost from the object **and** the document | entry restored to the overflow, field stays at its initialiser |
| 6 | Two threads serialize the same object concurrently | both mutate the same overflow `JsonObject`; `LinkedTreeMap` is not thread-safe | unchanged - pre-existing, and `WeakIdentityMap` was already the safe half |
| 7 | A downstream SPI factory produces overflow | n/a | unreachable by `@Extract`, and the entry does not claim otherwise |
| 8 | Store growth and sweep cost | two maps, bare `JsonElement` values | one map, one `Entry` record per value. `sweep()`'s full scan runs over one larger key set rather than two smaller ones - a wash, call it slightly worse |
| 9 | `sourceAccessor.get(value)` throws | n/a | escapes the read. Parity, not a new hole - `:231` already makes an unguarded reflective `get` on the post-bind path |
| 10 | A `@Capture` field's overflow is empty | published only when non-empty (`:386`), so no entry | unchanged. A claim finds nothing, which is correct; the write path's `open` creates a container if a re-injection needs one |
| 11 | The delegate binds to `null` | `:226`/`:368` discard that read's overflow | unchanged. `ExtractTypeAdapter.read` returns before claiming. A pre-existing fidelity hole that does not widen |
| 12 | A read-then-write of any `@Extract` site | the value is emitted **twice** - at the root under the field's serialized name and inside its source (`00-verified-facts.md` W1a) | emitted once, inside its source. The one intended output change (§2.3) |
| 13 | An `@Extract` field's serialized name is also a real wire key at the root | the wire key survives, because it is also what the duplicate is written under | the key is removed unconditionally, so the wire value is dropped. No site is shaped this way, and it is `@Capture`'s existing hazard at `:181` |
| 14 | The delegate's tree is not a JSON object | n/a - `@Extract` never touched the tree | falls through to `delegateAdapter.write`, the same bypass `Lenient` `:94`/`:148-149` and `Capture` `:167-170` take. Unreachable for a class that has fields |

Rows 1, 2 and 3 are one decision: **fail at adapter-build time on a misspelled, inherited or
unannotated source.** Recommended, because a claim that can never match is silent data loss that no
test catches, and `create` runs once per type rather than once per document. It is also the one place
this entry trades a silent no-op for an exception, and it is a hard break for any downstream module
that has been quietly doing nothing - sibling modules share the pin (`00-verified-facts.md` F2). The
smaller-blast-radius fallback is to keep the `continue`; that choice changes no other part of the
design. Note that a `create`-time `JsonException` is swallowed per type by `GsonSettings.prewarm`
(`:197-200`) and resurfaces at first real use, so the diagnostic is late but not lost.

Rows 5 and 12 are the failure modes that improve. On row 5, `00-verified-facts.md` B7 is preserved
either way - no new exception reaches a consumer and all six sites keep their `@NotNull` initialisers -
but the entry survives into the merge-back instead of vanishing. Row 12 is the only row in this table
that changes bytes a consumer already receives, and row 13 is the price of it.

### 2.6 The cheaper alternative

Two real ones. Both are named because pretending otherwise would make this entry look better than it
is.

**A - keep `@Extract` where it is and publish a read-scoped frame.** `CaptureTypeAdapter.read` pushes
`overflowMaps` onto a `ThreadLocal` before `:366` and pops it in a `finally`; the extract phase
consults the pushed frame as well as its own local list. It works on the read path, and - checked,
because it is not obvious - it also works on the write path, since `Lenient.write` post-processes
before `Capture.write` does, so a shared tagged store would receive the re-injection in time.

It loses on three counts, none of which is "it cannot be made to work":

- The frame is ambient state with a lifetime and it needs an identity tag. A nested POJO's
  `LenientTypeAdapter` would see the enclosing class's pushed frame and could match a same-named field
  on it. Guarding that means tagging frames with the adapter or raw type and comparing - a correctness
  argument no test naturally reaches.
- It keeps the extract phase inside the library's second-most-used factory, keeps the dead tree
  round-trip for `@Extract`-only classes, and keeps one of the five silent swallows where it is.
- It does not reduce the blast radius. It still edits both factories and still needs the shared tagged
  store for the write path.

The lift trades a **lifetime** problem for a **lookup** problem, and a lookup off a built object is
directly testable. That is the whole of the argument for it.

**B - do not build this at all, and take the two lossy cheap routes the research pack already
accepted.** `dgx-fallback` closes the six unmatched-enum-key sites with one line per enum and no
change to `CaptureTypeAdapterFactory`. A plain `@Lenient ConcurrentMap<String, Integer>` closes half
of `f03-questrewards-mixed-values` with no library change whatsoever.

Stated honestly: **this entry is not the cheapest way to close either symptom. It is the way to close
them without losing data.** `@Fallback` collapses every unmatched key onto one constant and later
entries overwrite earlier ones, which is the same N-1 loss the defect already has, made visible and
typed. The `@Lenient` partial parks the string half of `quest_rewards` in an overflow no code can
read. Both are cheap and both are lossy. If the owner's priority were minimum cost this entry would
decline; the standing decision to do the library work first is what makes it adopt.

### 2.7 Migration and regression surface

**Consumer migration is zero source edits.** No DTO changes, no annotation changes, no import
changes. The change is additive at the public surface, which is the non-negotiable constraint from
`04-compatibility.md` §3.3: `skyblock`, `client`, `github` and `persistence` reach hypixel as **jars**
compiled against four different older gson-extras shas, so a binary-incompatible change surfaces as a
`NoSuchMethodError` at runtime inside code hypixel did not write. Nothing here touches a public
member. `LenientTypeAdapterFactory.create`'s signature does not move, `WeakIdentityMap` stays
package-private, and `dataflow`'s two direct factory references
(`PostInitTypeAdapterFactory`, `CaseInsensitiveEnumTypeAdapterFactory`) are untouched.

**The blind spot, stated first because it changes how everything else is read.** `@Extract` appears in
exactly one test in the library - `lenientExtractCapture_ok` (`GsonFactoryTest`:2227) - and that test
only deserializes. `MemberDtoMappingTest` never calls `toJson` either. So **the `@Extract` write path
and the whole of `LenientTypeAdapter.write` have zero coverage in either module**
(`04-compatibility.md` §6.3, G1-G5), which is precisely the code §2.4 relocates. Both baselines stay
green through a total write-path regression. **Any implementation that does not write serialize tests
first is flying blind, and that ordering is not negotiable.** The W1a duplicate key is the proof: a
defect on every one of the six sites, visible in the first byte of any serialize assertion, and 134
green library tests plus 16 green hypixel tests never saw it.

**Step 1 - characterisation tests against `7cfc181`, before any edit.** Rows 2 to 7 must pass unchanged
afterwards; a test written after the change pins the new behaviour and proves nothing. **Row 1 is the
exception and is deliberately red**, because §2.3 fixes an output defect rather than preserving it.

| # | Test | Asserts |
| --- | --- | --- |
| 1 | Round-trip `FullCombinationModel` | `last_killed_mob` reappears inside the `kills` object exactly once **and the root carries no `lastKilledMob` key**. Split the second clause into its own `@Test`, `extractFieldIsNotEmittedAtRoot_ok`: written against `7cfc181` it **fails**, which is the confirmation the W1a duplicate is real, and it is the only step-1 test that flips from red to green. The first clause passes today and must keep passing |
| 2 | Round-trip a two-`@Extract` model shaped like `Loadouts` | each claim returns to its own source's sub-object, and neither `equippedArmorSet` nor `equippedEquipmentSet` appears at the root |
| 3 | Round-trip a `@SerializedPath`-located `@Lenient` source | merge-back still resolves through `locateElement`'s segment branch (`:340-350`) |
| 4 | Round-trip a collection-shaped `@Lenient` field | the `JsonArray` half (`:139-144`), single-site coverage today |
| 5 | Serialize a hand-built object that was never read | `open` creates the container and the entry still reaches the document |
| 6 | Read, mutate the extracted value, serialize | the mutation reaches the document (`00-verified-facts.md` E2) |
| 7 | `@Lenient` and `@Capture` overflow on one model | the `@Lenient` entry lands in the sub-object and the `@Capture` entry at the root - the one test that proves per-entry target tagging |

**Step 2 - the six consumer sites.** Decode-and-assert exists for one of them; a round-trip assertion
exists for none. `Bestiary` is never decoded at all today. `ChocolateFactory` and `Foraging` are
decoded with their `@Extract` fields unasserted, which is the worst case - the path runs and only a
thrown exception would fail the test.

**Step 3 - library anchors to re-run in full, not selectively**, because inserting a factory shifts
every later index in `GsonSettings.defaults()`:

- `GsonFactoryTest.CombinationTests` entire (`:1879-2255`) - the only place in the library that
  observes factory nesting at all.
- `GsonFactoryTest.CaptureTests` (`:443-1216`) - `CaptureTypeAdapterFactory` is edited at three lines.
- `CaptureGroupingModeTest`, `CollectionValueCompatibilityTest` - the newest and least settled
  behaviour.
- `WeakIdentityMapTest` - the class is not modified, but `Overflow` becomes its only production caller
  and its `V` is now a record.
- `GsonSettingsPrewarmTest` - the path a `create`-time `JsonException` would take.

**Step 4 - what the change owes.** `OverflowTest` covering publish/find/open/claim/restore, both
targets, target mismatch returning `null`, and claim on an array-shaped entry returning nothing; an
`ExtractTests` nest covering both source annotations and each `create`-time rejection; and one test
asserting the exact class list and order of `GsonSettings.defaults()`, which turns an index shift from
silent into loud (G14). Baseline to hold: gson-extras 134 plus the new count, hypixel 16/16.

**Verification does not cost a JitPack cycle.** The workspace composite substitutes
`com.github.simplified-dev:gson-extras` onto the local project, so a build launched from
`W:/Workspace/Java/Simplified` compiles and tests **every** consumer against the gson-extras working
tree with no push (`04-compatibility.md` §2.3). Only the final binary-compatibility pass - standalone
in the hypixel directory after a re-pin - needs the published artifact.

### 2.8 Verdict

**Adopt.** `large`.

The rating comes from the clause, not the file count. `00-conventions.md` §4 puts a change at `large`
when it introduces "a new ordering guarantee between factories, or a new lifecycle hook the whole
pipeline must honor", and this does exactly one of those: `ExtractTypeAdapterFactory` **must** nest
outside `CaptureTypeAdapterFactory` or it silently reverts to the current capability. By file count
alone it reads `medium` - two new library files, three edited, two new test classes, zero consumer
files - and `00-conventions.md` §4 says to give the higher level when uncertain.

**What it subsumes.** Three things stop being separate problems:

- `d10-lenient-overflow`'s "there is no way to read a set of overflowed entries back" and
  `d10-capture-unmatched`'s "`@Extract` cannot reach the `@Capture` store" were counted as two thin
  one-site proposals and declined separately. They are one missing capability. This entry supplies the
  reachability half and §3 supplies the selection half.
- The read-scoped-channel design (`00-verified-facts.md` C3's fix shape) is subsumed rather than
  implemented - the same result with no ambient state.
- One of the library's five silent swallows leaves it. `LenientTypeAdapterFactory.java`:246-247 is
  deleted and its replacement restores the claim instead of dropping it. The other four remain and
  this entry claims none of them.
- The W1a duplicate emission goes with it. `@Extract` has never removed its own field's serialized key
  from the output, so all six sites emit their extracted value twice on every serialize. §2.3 adds the
  removal `CaptureTypeAdapterFactory.java`:181 has always done. This was not a goal of the lift; it was
  found by writing the serialize tests the lift requires, which is the argument for §2.7's ordering
  made concrete.

**What it does not fix, stated so it is not mistaken for coverage.** Grouping-mode `@Capture` fields
produce no overflow at any point, so `TrophyFishing.fish` and `HeartOfTheMountain.powder` are out of
reach of anything built on overflow; §4.3 owns them. The two publish policies stay divergent. And a
future factory registered between `Capture` and `Extract`, or a downstream SPI factory, silently
reduces `@Extract` to its current capability with no test failure unless the order test in §2.7 exists.

Ships with `dgx-extract-filter` and `dgx-capture-unmatched` in one commit and one JitPack cycle. On
its own it has zero adoption sites and is unverifiable end to end.

## 3. dgx-extract-filter - `@Extract`

- **Registry entry:** none - the "extend an existing annotation" route `00-conventions.md` §6.1 asks
  proposals to name explicitly. It supersedes the declined `d10-lenient-overflow`
- **Verdict:** adopt narrowly - selection only, no key stripping, no array-shaped remainder (§3.4)
- **Category:** `correctness`
- **Answers findings:** `f03-questrewards-mixed-values`; unlocks `f06-capture-null-enum-key` for
  `dgx-capture-unmatched`, which cannot use overflow without it
- **Cheaper alternative:** `@Lenient` alone, typing half the map and leaving the rest unreadable - the
  route the research pack accepted, and it is lossy (§3.7)
- **Library change:** existing factory edit, plus one element on an existing annotation
- **Adoption sites today:** 1, plus up to 6 conditional on `dgx-capture-unmatched` - four immediately,
  two only if its grouping-mode branch lands
- **Effort:** `medium`

### 3.1 The problem it removes and the real sites

**The site.** `member/crimson/CrimsonIsle.java`:65-66, `Quests.questRewards`:

```java
@SerializedName("quest_rewards")
private @NotNull ConcurrentMap<String, Object> questRewards = Concurrent.newMap();
```

One JSON object carrying two unrelated maps interleaved by value type - `<itemId> -> <count>` with
integer values, and `<questId> -> <itemId>` with string values. The `Object` value type is what a DTO
writes when it has given up. `@Lenient` can divert one value type to overflow, but `@Extract`
addresses a **single named key**, so there is no way to pull "every string-valued entry" back out,
and two fields cannot both claim `quest_rewards` because gson rejects duplicate serialized names.
That is `d10-lenient-overflow`'s problem statement, unchanged.

**The six the sibling entry unlocks.** `Dojo.java`:15 and `:17`, `Kuudra.java`:18 and `:20`,
`TrophyFishing.java`:24, `HeartOfTheMountain.java`:49 - six `@Capture` maps that narrow an open key
space onto a closed enum with no failure policy. `00-verified-facts.md` C6 corrects the research
pack's count from seven to six and removes `Statistics.java`:89, which is not a `@Capture` field at
all and binds through gson's stock map adapter.

Those six are **not** this entry's adoption sites and it must not claim them; §4 owns them. What this
entry supplies is the half they are missing. `10-annotation-designs.md` §9.4 names diverting unmatched
keys to `@Capture`'s existing overflow as the only option that keeps the data **and** keeps round-trip
fidelity, then declines it because "`@Extract` addresses a single named key, not every entry that
failed key conversion". This element is that missing axis.

Note the trap before anyone counts higher: `TrophyFishing.fish` and `HeartOfTheMountain.powder` are
**grouping mode**, and grouping mode produces no overflow at all from the classify pass
(`00-verified-facts.md` D6). Reaching those two needs more than a filter on `@Extract` - it needs §4.3.

**The free semantic slot this lands in.** `ExtractFieldInfo`'s constructor
(`LenientTypeAdapterFactory.java`:468-476) already branches on `path.indexOf('.')`, and the **else**
branch is dead:

```java
} else {
    this.sourceFieldName = path;
    this.jsonKey = "";
}
```

A dotless `@Extract("kills")` claims the literal empty key, which no document carries, so the field
silently keeps its initialiser (`00-verified-facts.md` B6). Zero sites use it, here or downstream.
Giving that form a meaning changes the behaviour of nothing that was doing anything.

### 3.2 Full declaration of the added element

The whole file is shown because the class javadoc has to be rewritten as well - the current one
asserts the source is a `@Lenient` field and names `LenientTypeAdapterFactory` as the implementor, and
both stop being true. The `<pre>{@code ...}</pre>` example form matches what `Extract.java` and
`Capture.java` already use.

```java
package dev.simplified.gson.annotation;

import dev.simplified.gson.factory.ExtractTypeAdapterFactory;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extracts entries from another field's filtered overflow into a typed companion field.
 * <p>
 * The source is named by {@link #value()} and may carry either {@link Lenient @Lenient} or
 * {@link Capture @Capture} - both park the entries they could not take in the same place. A
 * {@code @Capture} source in grouping mode never produces overflow, so nothing can be claimed from
 * one.
 * <p>
 * Two modes, selected by whether {@link #value()} carries a dot:
 * <ul>
 *     <li><b>Single key</b> ({@code "sourceField.jsonKey"}) - claims the one entry stored under
 *         {@code jsonKey} and reads it as this field's type. {@link #filter()} must be empty.</li>
 *     <li><b>Remainder</b> ({@code "sourceField"}) - claims every entry {@link #filter()} accepts and
 *         reads them together, so the field is a map. An empty filter, the default, claims the whole
 *         remaining overflow.</li>
 * </ul>
 * <p>
 * A claimed entry leaves the source's overflow and this field's value is put back into it on
 * serialize, so the document round-trips in either mode. Keys are claimed and restored exactly as the
 * source stored them - nothing is stripped, so a {@code @Capture} source yields whatever prefix its
 * own filter matched.
 * <p>
 * Example - one key out of a lenient map:
 * <pre>{@code
 * @Lenient
 * private ConcurrentMap<String, Integer> kills = Concurrent.newMap();
 *
 * @Extract("kills.last_killed_mob")
 * private Optional<String> lastKilledMob = Optional.empty();
 * }</pre>
 * Example - the typed remainder of one:
 * <pre>{@code
 * @Lenient
 * @SerializedName("quest_rewards")
 * private ConcurrentMap<String, Integer> questRewards = Concurrent.newMap();
 *
 * @Extract(value = "questRewards", filter = "^crimson_isle_")
 * private ConcurrentMap<String, String> questItems = Concurrent.newMap();
 * }</pre>
 * The integer rewards bind into {@code questRewards}; every {@code crimson_isle_} entry failed that
 * map's value type, landed in overflow, and is read out as {@code String} here.
 *
 * @see Lenient
 * @see Capture
 * @see ExtractTypeAdapterFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Extract {

    /**
     * The source field, optionally followed by a dot and the single JSON key to claim.
     * <p>
     * The first segment is the Java field name of the {@link Lenient @Lenient} or
     * {@link Capture @Capture} source, never its serialized name. Everything after the first dot is
     * one literal JSON key, dots included, and selects single-key mode; a value with no dot selects
     * remainder mode.
     *
     * @return the source field name, optionally dot-separated from one JSON key
     */
    @NotNull String value();

    /**
     * Regex selecting which of the source's overflowed keys this field claims.
     * <p>
     * Applies in remainder mode only - a {@link #value()} carrying a dot already names one key and
     * rejects a filter. An empty string, the default, claims every remaining entry. The matched
     * portion is <b>not</b> stripped, so the map holds the keys the document carried.
     *
     * @return the regex filter, or empty to claim the whole remaining overflow
     */
    @Language("RegExp")
    @NotNull String filter() default "";

}
```

Adding an element **with** a default is source- and binary-compatible for every consumer
(`00-verified-facts.md` F4, `04-compatibility.md` §3.3), so no existing `@Extract` site recompiles
differently. `value()` has no default today and must keep none.

### 3.3 Usage before and after

`member/crimson/CrimsonIsle.java`, the `Quests` nested class. The wire shape is one object with two
unrelated maps in it:

```json
"quest_rewards": {
  "ENCHANTED_SULPHUR": 12,
  "MAGMA_FISH": 3,
  "crimson_isle_kill_barbarian_duke_x_c": "KADA_LEAD"
}
```

**Before** - one map typed to the join of both halves, so every caller unwraps by hand:

```java
@SerializedName("quest_rewards")
private @NotNull ConcurrentMap<String, Object> questRewards = Concurrent.newMap();
```

**After** - the integer half binds, the string half is claimed out of the overflow it already lands
in:

```java
@Lenient
@SerializedName("quest_rewards")
private @NotNull ConcurrentMap<String, Integer> questRewards = Concurrent.newMap();
@Extract("questRewards")
private @NotNull ConcurrentMap<String, String> questItems = Concurrent.newMap();
```

`@Lenient` alone gets the first field right today and leaves `questItems` unreachable - that is the
free partial the research pack accepted, and it is exactly half the data. The dotless `@Extract` with
its default empty filter is the catch-all remainder: it claims everything the integer map rejected. A
filter is only needed when two typed remainders share one source.

Read: `@Lenient` splits `quest_rewards` into `{ENCHANTED_SULPHUR: 12, MAGMA_FISH: 3}` in the tree and
`{crimson_isle_kill_barbarian_duke_x_c: "KADA_LEAD"}` in the overflow, publishes the overflow under
the bound map's identity, and `ExtractTypeAdapter` claims it and reads it as
`ConcurrentMap<String, String>`.

Write: `ExtractTypeAdapter.write` puts each `questItems` entry back into the overflow under its own
key, `@Lenient`'s merge-back copies the overflow into the `quest_rewards` sub-object, and
`ExtractTypeAdapter` then removes the root-level `questItems` key the reflective binder emitted
(§2.3). The emitted document is the input document.

**That last step is load-bearing in remainder mode, more than in single-key mode.** Without it the
whole `questItems` map is emitted at the root *as well as* merged into `quest_rewards`, so a remainder
does not merely duplicate one key - it duplicates N, and the second copy is under a Java field name no
upstream consumer knows. The §2.3 removal is what makes "the emitted document is the input document"
true rather than aspirational, and it is why §2 and §3 cannot ship apart.

Two adjacent fields on the same class, `minibossDaily` (`:68`) and `kuudraBossDaily` (`:70`), are also
`ConcurrentMap<String, Object>`. They are **not** counted as adoption sites here: `Object` there may
be homogeneity that was never checked rather than a genuine mix, and a survey of the fixture is owed
before either is re-typed. Adoption sites today stays at **1**.

### 3.4 How the factory implements it

**Mode is selected by the dot in `value()`, not by the presence of `filter()`.** Three combinations
were considered, and only one avoids `00-conventions.md` §4's `xlarge` clause for "changes the meaning
of an existing annotation for existing users":

| Option | Rule | Verdict |
| --- | --- | --- |
| Exclusive on `filter()` | empty filter means today's exact-key mode, non-empty means remainder | **Rejected.** It makes "empty filter" mean *exact key*, the opposite of `@Capture`, where an empty filter is the catch-all. Two annotations, one word, inverted meanings |
| `value()` reinterpreted as a key regex | drop the source-plus-key path and name the source elsewhere | **Rejected.** `xlarge` by definition - all six existing sites change meaning |
| **The dot in `value()`** | dotted value means single key and `filter()` must be empty; dotless value means remainder, and inside remainder mode an empty `filter()` is the catch-all | **Chosen** - it lands in the dead branch from §3.1 |

Inside remainder mode `filter()` then mirrors `@Capture.filter()` where it counts: empty is catch-all,
and a regex selects by `Pattern.matcher(key).find()` - the same `find()` semantics
`CaptureTypeAdapterFactory.java`:327 uses, not `matches()`. What it does **not** mirror is stripping.

Two `ExtractFieldInfo` properties carry it, both resolved once per type at `create`:

```java
private final boolean remainder;
private final @Nullable Pattern pattern;

boolean matches(@NotNull String key) {
    return this.pattern == null || this.pattern.matcher(key).find();
}
```

`remainder` is `path.indexOf('.') < 1`; `pattern` is `Pattern.compile(filter)` when the filter is
non-empty and `null` otherwise, mirroring `CaptureFieldInfo.java`:653. `matches` returning `true` on a
null pattern is the whole of the difference between the two remainder cases.

The read path is then the branch already shown in §2.3: `Overflow.claim(owner, info::matches)` returns
a `JsonObject` of the selected entries under their original keys, and
`gson.fromJson(claimed, accessor.getGenericType())` reads it as the declared map type. That is the same
conversion call the single-key path makes; only the element handed to it differs. Two consequences:

- **Key conversion is gson's, not the factory's.** A `ConcurrentMap<String, String>` target takes every
  key; an enum-keyed target hits the same unmatched-key behaviour §4 is about, one level up. Remainder
  mode is string-keyed in practice until §4 lands.
- **The conversion is all-or-nothing.** One unconvertible value fails the whole map, and
  `Overflow.restore` puts every claimed entry back, so the field keeps its initialiser and the document
  is intact. That is coarser than `buildSimpleMap`'s per-entry catch (`:401-402`) and it is the right
  coarseness: a partial map with no signal is worse than an empty one, because the empty one is
  visible.

**Claim ordering.** Claiming is destructive, so order is semantics. `@Capture` resolves this by trying
filtered fields in declaration order with first-match-wins and only then falling through to the
catch-all, which makes reordering two fields in a DTO a behaviour change (`00-verified-facts.md` D1)
and rests on `Class.getDeclaredFields` returning declaration order, which the JVM specification does
not promise. `@Extract` should not inherit that. `ExtractFieldInfo.of` sorts into three bands, and the
common compositions become order-independent:

| Band | Shape | Rationale |
| --- | --- | --- |
| 0 | single key | The most specific claim there is. Running it first lets an exact `@Extract` and a catch-all remainder coexist on one source, with the named key in its own typed field and everything else in the map |
| 1 | filtered remainder | Mirrors `@Capture`'s "filtered first" |
| 2 | catch-all remainder | Mirrors `@Capture`'s catch-all fallthrough, and at most one is useful per source |

```java
result.sort(Comparator.comparingInt(ExtractFieldInfo::getClaimOrder));
```

`ConcurrentList` inherits `List.sort`, and `CaptureFieldInfo.discoverGroupAffixes` already uses it.
What stays genuinely ambiguous is **two filtered remainders on one source whose regexes overlap**: the
first in scan order claims the intersection. That is declared, not fixed - deciding whether two
regexes intersect is not something a `create`-time check can do, and an approximation that rejected
safe pairs would be worse than the documented hazard. Two catch-alls on one source **is** detectable
and is rejected at `create` time, which is the same silent-second-one-does-nothing shape as
`@Capture`'s D2.

**The two cuts that make the verdict "adopt narrowly".**

1. **Selection without stripping.** `@Capture` strips (`key.replaceFirst(info.getFilter(), "")` at
   `:330`) and reconstructs on write by prepending `literalPrefix` (`:218-220`, `:228-230`).
   `literalPrefix` is the filter with a leading `^` and trailing `$` removed (`:654`) - a **literal**,
   not a regex inverse - so it round-trips only because all eleven `@Capture` filters in the module are
   a literal plus `^` (`00-verified-facts.md` D4). Duplicating that in a second annotation doubles the
   places a regex with real metacharacters silently fails to round-trip. Worse, a `@Capture`-sourced
   claim would **double-prefix**: `@Capture` deliberately stores overflow under the original
   **unstripped** key (`:338`, `:358`) so its merge-back can copy it back verbatim, so stripping on
   claim would require re-applying a prefix the overflow path never removed. And the one real site
   wants the keys - `crimson_isle_kill_barbarian_duke_x_c` **is** the quest id, and
   `kill_barbarian_duke_x_c` is not what the upstream API calls it anywhere else. A `boolean strip()`
   is additive later and would inherit `literalPrefix`'s limits knowingly rather than by accident.
2. **No remainder over an array-shaped overflow.** A collection-shaped `@Lenient` field produces a
   `JsonArray` overflow (`:188`, `:198`), which has no key space for a filter to match. Claiming the
   whole array into a typed list is coherent and has zero sites - `Dungeons.unlockedJournals` is the
   module's only collection-shaped `@Lenient` field and carries no `@Extract`. `Overflow.claim` returns
   nothing for an array-shaped entry in either overload, which preserves today's silent no-op exactly
   (`00-verified-facts.md` §3.1 R5b) and keeps the new surface to one shape.

### 3.5 Interaction with the shared `Overflow`

Four things this element consumes from §2, none of which exists today:

| Consumed | Why the element needs it |
| --- | --- |
| `claim(owner, Predicate)` | The selection axis itself. Today's claim is `overflowObj.remove(jsonKey)` (`:216`) - one key, no predicate |
| The per-entry `Target` tag | A remainder claimed from a `@Capture` source must be restored to `SOURCE_OBJECT` and merged at the root; one claimed from a `@Lenient` source must be restored to `FIELD_ELEMENT` and merged into the field's sub-object. A remainder can hold N entries, so getting this wrong misplaces N keys, not one |
| `restore` | Remainder conversion is all-or-nothing, so a failure has to put back a whole set. Today's single-key failure loses one entry silently; a remainder failure would lose the lot |
| The `Overflow` lookup by bound-container identity | A remainder over a `@Capture` source is only reachable because `ExtractTypeAdapter` runs outside `CaptureTypeAdapter` and the captured `Map` instance exists by then (`:381`). Inside, the identity does not exist yet |

**The `Target` tag is what makes a `@Capture` remainder round-trip.** `@Capture` stores overflow under
the **original unstripped** root key, and its merge-back copies entries into the root verbatim,
bypassing `literalPrefix` (`00-verified-facts.md` D3). A remainder claimed from such a source therefore
holds full original keys, and putting them back under those same keys is exactly what the producer
expects. This is the asymmetry a plain map union would have destroyed.

**On write, `open` decides the target only when nobody else has.** For a read-then-write the producer
published first and its target stands. For a hand-built object that was never read there is no entry,
so `@Extract` publishes first and supplies the target from the source field's annotation. Both routes
derive the target from the same annotation, so both agree. `@Capture`'s publish-only-when-non-empty
policy (`:386`) is what makes the second route reachable at all for a `@Capture` source, and `open`
covers it.

**One dead configuration becomes live, and it should be stated rather than discovered.**
`LenientFieldInfo.of` skips any field also carrying `@Capture` (`:437-438`), so `@Lenient` plus
`@Capture` on one field is `@Capture`-wins with a silently dead `@Lenient`, and any `@Extract` naming
it no-ops. Adding a selection axis makes that combination meaningful for the first time. No site uses
it, so this is a contract change with zero adoption impact - but it is a contract change
(`04-compatibility.md` §5.2 L11), and the right response is a test pinning `@Capture`-wins rather than
a silent reinterpretation.

### 3.6 Failure modes and round-trip fidelity

| # | Condition | Behaviour | Note |
| --- | --- | --- | --- |
| 1 | `filter` set on a dotted `value` | `JsonException` from `create` | A combination no current source can express, so rejecting it regresses nothing |
| 2 | Remainder target is not a map | conversion fails, every claimed entry restored, field keeps its initialiser | Silent. A `create`-time check on the declared type is cheap and recommended alongside §2.5's |
| 3 | Two catch-all remainders on one source | `JsonException` from `create` | §3.4 |
| 4 | Two overlapping filtered remainders | first in scan order wins the intersection | Declared, not fixed. §3.4 |
| 5 | Source is a grouping-mode `@Capture` field | claims nothing, forever | `00-verified-facts.md` D6. Two of the six enum sites are exactly this, and §4.3 is where they are addressed |
| 6 | Enum-keyed remainder target | keys that fail enum conversion collapse onto `null` | The same defect one level up. Remainder mode is string-keyed in practice until §4 lands |
| 7 | Remainder claimed, then serialized | every entry is re-injected under its own key, and the field's own root-level key is removed | Exact round-trip **on two conditions**. First, keys must serialize back to what they were: a `String` key does, an enum key writes its `@SerializedName` or constant name, which need not equal the original document key - another reason for #6. Second, §2.3's key removal must be in the same sha; without it the remainder is emitted whole at the root **as well as** merged back, so N keys are duplicated rather than round-tripped (`00-verified-facts.md` W1a) |
| 8 | Caller mutates the extracted map after the read | the mutation is serialized | Consistent with `@Lenient`, whose identity keying exists precisely so a caller can mutate a bound container and keep its overflow. Intended |
| 9 | Read with one `Gson`, written with another | works - the store is `static` | Unchanged from today and unchanged by merging the stores (`00-verified-facts.md` E1) |
| 10 | A dotted `@Extract` typo written dotless | claims the whole overflow instead of doing nothing | `@Extract("kills")` used to be a silent no-op; it is now a catch-all remainder whose conversion will almost always fail, and #2 restores the entries. Same outcome, different route - but it is a behaviour change for a form that no site uses |

Round-trip in one line: a remainder claim removes N entries from the overflow on read and adds N
entries back on write, under the same keys, into the same container, which the producer then merges
into the same place it would have merged them from - and the field itself is then removed from the
root so the N entries appear once, not twice. The only lossy paths are #5, #6 and #7, and all three are
properties of the source or the target type rather than of the mechanism.

**One tool-side consequence outside the library.** `scripts/json_dto_diff.py` reads `@Extract` with a
no-parameter regex that requires a lone string literal and an immediate `)`
(`04-compatibility.md` §7.2 D1), so the first `@Extract(value = ..., filter = ...)` site makes it
return `None` and report the real JSON key as unmapped and the Java field name as a phantom binding.
The differ patch is three consumer-side lines and **must land in the same commit as the first
multi-element `@Extract`**, or the only coverage tool the module has becomes noise.

### 3.7 The cheaper alternative

At one site, three things beat this element on cost. The third is not in the research pack and is the
sharpest of the three, so it is stated rather than skipped.

**A - `@Lenient` alone.** `d10-lenient-overflow`'s position: declare `questRewards` as
`@Lenient ConcurrentMap<String, Integer>` and the integer half is typed correctly today, at zero
library cost, with the string half parked in an overflow that round-trips but that nothing can read.
Half the data, none of the cycle.

**B - a field-scoped `@JsonAdapter`.** The stock-first rule (`10-annotation-designs.md` §2) records
that gson honours `@JsonAdapter` on a **field**, not only on a type, so a ~25-line `TypeAdapter` in
the consuming module could read `quest_rewards` into a two-map holder and write it back, with no
library change and no `GsonSettings` registration. For one site that is cheaper than a JitPack cycle,
and it is the honest comparison this entry has to survive rather than one it gets to skip.

**C - build the selection axis where `@Extract` already lives.** A predicate claim over the
frame-local `FieldOverflow` list is roughly fifteen lines inside `LenientTypeAdapterFactory`, and it
closes `questRewards` completely without `dgx-overflow-store` existing at all. This is the strongest
of the three and it has to be conceded plainly: **the one adoption site today does not need the
store.** What it does not do is reach a `@Capture` source, which is the entire six-site half; it has
no `restore`, so a failed remainder still loses every claimed entry; and it is code that gets
rewritten when the lift lands, so taking it means paying for the element twice.

It survives on three grounds, none of which is "the annotation is nicer":

1. **It is the only lossless route to the six enum sites.** `@Fallback` types them and collapses N
   unmatched keys onto one constant. A `skipUnmatchedKeys` element drops them. Overflow plus a
   filtered `@Extract` keeps every one of them, in the object and in the document.
2. **The marginal cost is small.** `dgx-overflow-store` is being built by owner decision. On top of an
   `ExtractTypeAdapterFactory` that already exists, this element is one annotation element, one
   `Pattern`, one `boolean`, one sort and one extra branch in each of `read` and `write`.
3. **A hand-rolled `TypeAdapter` per site is the residue the pack exists to remove.** One is cheaper
   than a cycle. The fourth one is not.

If the owner had not already decided to do the library work first, this entry would read **decline -
take the `@Lenient` partial**, exactly as `d10-lenient-overflow` did. The decision changed the
denominator, not the evidence, and this entry says so rather than reverse-engineering a stronger case
than exists.

### 3.8 Verdict

**Adopt narrowly.** `medium`.

Narrow in the two ways §3.4 names: selection only with no key stripping, and object-shaped overflow
only with no array-shaped remainder. Both cuts are reversible additions later; neither is a semantic
that would have to be broken to add them.

`medium` is what `00-conventions.md` §4 prices "adds an element to an existing annotation" at. It rides
`dgx-overflow-store`'s JitPack cycle and costs no second re-pin, and it **must** ship in that cycle -
the store has no adoption sites of its own, and this element is what makes it verifiable end to end.

**What it subsumes.** `d10-lenient-overflow`, declined in the research pack as "one site, take the free
partial". The decline is not overturned on new site evidence - there is still one site - it is
overturned because the cost side changed: the mechanism it needed is being built anyway, and because
the same missing axis is what blocked `d10-capture-unmatched`. `README.md` §6.2 counted those two rows
as "one site each" and the owner identified that as a counting error; this entry and §4 are the action
on it.

## 4. dgx-capture-unmatched - `@Capture`

- **Registry entry:** none - the "extend an existing annotation" route `00-conventions.md` §6.1 names.
  It supersedes the declined `d10-capture-unmatched`, and it adds **no element**
- **Verdict:** adopt
- **Category:** `correctness`
- **Answers findings:** `f06-capture-null-enum-key`
- **Cheaper alternative:** `@Fallback` on the key enum - one line per enum, no factory edit, and it is
  lossy in a way §4.6 prices
- **Library change:** existing factory edit - two branches, no new element and no new file
- **Adoption sites today:** 6 - four entry-mode, two grouping-mode
- **Effort:** `medium`

### 4.1 The problem it removes and the real sites

Six `@Capture` maps narrow an open JSON key space onto a closed enum with no failure policy, and an
unmatched key becomes a `null` map key. Proved by probe against `Kuudra`:

```
PROBE kuudra tiers = {null=4, BASIC=1}
```

Four distinct upstream keys produced one entry. **The loss is N-1 values per field, not "one odd
key"** - collapse is the real cost, and nothing anywhere records it.

| Site | Key type | Value | Mode | Filter |
| --- | --- | --- | --- | --- |
| `member/crimson/Dojo.java`:15 `points` | `Type` | `Integer` | entry | `^dojo_points_` |
| `member/crimson/Dojo.java`:17 `times` | `Type` | `Integer` | entry | `^dojo_time_` |
| `member/crimson/Kuudra.java`:18 `highestWave` | `Tier` | `Integer` | entry | `^highest_wave_` |
| `member/crimson/Kuudra.java`:20 `completedTiers` | `Tier` | `Integer` | entry | catch-all |
| `member/crimson/TrophyFishing.java`:24 `fish` | `TrophyFish` | `TierData` | **grouped** | catch-all |
| `member/mining/HeartOfTheMountain.java`:49 `powder` | `Powder.Type` | `Powder` | **grouped** | `^powder_` |

Six, not the research pack's seven. `Statistics.java`:89 is `Statistics.Mythos.completedChains`, a
plain `ConcurrentMap<Type, Integer>` with `@SerializedName` and **no `@Capture`** - it binds through
gson's stock `MapTypeAdapterFactory`, so no `@Capture` change can reach it
(`00-verified-facts.md` C6). That does not weaken the pack's §9 verdict; it sharpens the scope.

**Why the existing type check does not catch it.** Key conversion splits three ways
(`00-verified-facts.md` §10.3):

- **`String` key** - `isCompatibleCaptureEntry` skips the check outright (`:489`). Cannot fail.
- **`Integer` key** - gson's `Integer` adapter **throws** on a non-numeric string, so `:490-494`
  catches it, returns `false`, and the entry lands in overflow. Correct, lossless, round-trips. Seven
  sites already behave this way.
- **`enum` key** - `CaseInsensitiveEnumTypeAdapterFactory.read`:82 returns
  `nameToConstant.get(...)`, which is **`null` for an unmatched name and throws nothing**. So `:491`
  succeeds, the entry is judged compatible, it never reaches overflow, and it is put under a `null`
  key later.

Stated plainly: **the enum key path is the only one that does not already do the right thing.** The
predicate at `:490-494` asks "did the conversion throw", and for enums that question has the wrong
answer.

The rest of the chain, traced end to end:

1. Classify judges the entry compatible and routes it to `capturedJsonMaps`, not to overflow. For the
   two grouping-mode sites the check is not even reached (`:332-334`, `:355`).
2. `buildSimpleMap`:398 or `buildGroupedMap`:474 converts the key and gets `null`.
3. `map.put(null, value)` at `:400`/`:476`. The map came from `newMapInstance()`, a
   `dev.simplified.collection.ConcurrentMap`, which **tolerates a null key** - so nothing throws and
   the empty catch at `:401-402`/`:477-478` never even gets a chance to hide it. Fixing those two
   catches would not surface this.
4. Every unmatched key in the field collapses onto the same `null`. Last write wins.
5. Nothing lands in overflow, so the write side does not restore them either. The data is gone from
   the object **and** from the round trip.

### 4.2 Full declaration of the added element

**There is no added element.** That is the entry's central claim and the reason it is not the proposal
the research pack declined. `@Capture`'s source file does not change apart from one javadoc paragraph;
no `skipUnmatchedKeys`, no policy enum, no new default to reason about at seventeen existing sites,
and nothing for a consumer to opt into. What changes is a predicate that is already wrong.

```java
/**
 * Returns whether a captured entry can be bound onto the declared map generics.
 * <p>
 * An entry that cannot is diverted to the field's overflow under its original key, where it
 * round-trips and stays claimable by an extracting field, rather than being bound onto a key the
 * conversion could not produce.
 *
 * @param key the JSON key, already stripped of any matched filter
 * @param value the JSON value
 * @param info the capturing field
 * @return {@code true} when both the key and the value convert
 */
private boolean isCompatibleCaptureEntry(@NotNull String key, @NotNull JsonElement value, @NotNull CaptureFieldInfo info) {
    try {
        // Check key compatibility
        Class<?> rawKeyType = getRawType(info.getKeyType());

        if (rawKeyType != String.class) {
            try {
                if (this.getGson().fromJson(new JsonPrimitive(key), info.getKeyType()) == null)
                    return false;
            } catch (Exception ex) {
                return false;
            }
        }

        // Check value compatibility
        return isCompatibleValue(value, info.getValueType());
    } catch (Exception ignored) {
        return false;
    }
}
```

One added clause, and it is a no-op for every key type except an enum: gson's `Integer`, `Long`,
`UUID` and `Instant` adapters all **throw** on an unusable string rather than returning `null`, and
the `String` branch never reaches the conversion at all. Only `CaseInsensitiveEnumTypeAdapterFactory`
returns `null` without throwing (`:82`).

It also brings the key check into line with the **value** check, which has always been written the
right way round. `isCompatibleValue`'s enum branch tests `result != null` and diverts an unrecognized
enum **value** to overflow (`CaptureTypeAdapterFactory.java`:538-541), and `@Lenient` does the same at
`:309-312`. So an enum-valued `@Capture` map already keeps unknown values losslessly, and an
enum-keyed one does not. This entry removes an inconsistency rather than introducing a policy.

The javadoc paragraph `@Capture` gains, stating the behaviour where a consumer will look for it:

```java
 * A captured entry whose key or value does not convert to the declared generics is kept in the
 * field's overflow under the key the document carried, so it survives a round trip and can be read
 * into a companion field with {@link Extract @Extract}. An enum key that matches no constant is
 * treated the same way as a numeric key that will not parse.
```

**One clause is deliberately absent, and it belongs to `dgx-fallback`.** Once an enum can resolve to a
marked fallback constant, "did the conversion yield `null`" stops being the whole question and becomes
"did it yield `null` **or the fallback**" - otherwise a marked enum silently turns this lossless
behaviour back into a lossy one. `02-fallback.md` §10 makes that a required companion edit at four
sites, two of which are the two lines this entry changes. The null half needs no `@Fallback` and
stands on its own, so the two can ship in separate cycles, in either order, without a window where
either is wrong.

### 4.3 How the factory implements it

Two branches, one per mode. Neither adds a factory, so **there is no registration slot and no index
shift** - `CaptureTypeAdapterFactory` stays exactly where it is and every other factory keeps its
nesting depth. The ordering this entry depends on is entirely §2's: `ExtractTypeAdapterFactory` must
nest outside `CaptureTypeAdapterFactory`, or the diverted entries round-trip but stay unreadable.

**Branch A - entry mode, at classify.** The predicate in §4.2, and nothing else. An entry whose enum
key matches no constant now fails `isCompatibleCaptureEntry`, so classify routes it to
`overflowMaps` under the **original unstripped** key at `:338` (filtered) or `:358` (catch-all), which
is the path seven `Integer`-keyed sites already take. `:386-387` publishes it, and on write
`:239-248` copies it back into the root object verbatim. Reaches `Dojo.points`, `Dojo.times`,
`Kuudra.highestWave` and `Kuudra.completedTiers`.

**Branch B - grouping mode, at build.** Branch A cannot reach the other two, because grouping mode
skips the compatibility check entirely (`:332-334`, `:355`) and therefore produces no overflow at any
point (`00-verified-facts.md` D6). The unmatched key is not discovered until `buildGroupedMap`
converts the assembled group key at `:474`, by which time the original entries have been split apart
into a synthesized group object. Divert there instead, restoring the entries the group was built from:

```java
    // Deserialize each group as value type
    for (Map.Entry<String, JsonObject> group : groups.entrySet()) {
        try {
            Object key = this.getGson().fromJson(new JsonPrimitive(group.getKey()), info.getKeyType());

            if (key == null) {
                divertGroup(overflow, groupSources.get(group.getKey()), info);
                continue;
            }

            map.put(key, this.getGson().fromJson(group.getValue(), info.getValueType()));
        } catch (Exception ex) {
            divertGroup(overflow, groupSources.get(group.getKey()), info);
        }
    }
```

```java
/**
 * Moves every JSON entry that fed one group back into the field's overflow, under the key the
 * document carried.
 *
 * @param overflow the field's overflow
 * @param source the entries that fed the group, under their filtered keys
 * @param info the capturing field
 */
private static void divertGroup(@NotNull JsonObject overflow, @NotNull JsonObject source, @NotNull CaptureFieldInfo info) {
    for (Map.Entry<String, JsonElement> entry : source.entrySet())
        overflow.add(info.getLiteralPrefix() + entry.getKey(), entry.getValue());
}
```

`groupSources` is one extra `ConcurrentMap<String, JsonObject>` filled alongside `groups` in the same
three places the grouping loop already writes to `groups` (`:426`, `:441`, `:455`/`:466`), holding each
group's contributing entries under their filtered keys. Three mechanical facts make this exact rather
than approximate:

- **Key reconstruction reuses the mechanism the write path already relies on.** `literalPrefix` is the
  filter with `^` and `$` stripped (`:654`), and grouping mode's own write path already prepends it to
  every captured key (`:218-220`). All eleven filters in the module are a literal plus `^`, so
  `literalPrefix + strippedKey` is byte-exact: `powder_` + `mithril_total` for
  `HeartOfTheMountain.powder`, and an empty prefix for the catch-all `TrophyFishing.fish`. A filter
  carrying real regex metacharacters does not round-trip - but that is `00-verified-facts.md` D4, an
  existing limit of the existing mechanism, not new fragility.
- **The overflow fetch has to move up.** `:384` currently reads `overflowMaps.get(...)` **after** the
  build call at `:377`/`:379`. It moves above them so the object can be passed in, which also makes
  the `:386` publish gate see the diverted entries - without that reorder a grouping-mode field would
  divert into an object nobody publishes.
- **`buildSimpleMap` needs nothing.** After branch A a key reaching it has already been judged
  convertible, so `key == null` is unreachable there.

**One deliberate widening, and the narrower option if it is unwanted.** Filling the `catch` at
`:477-478` diverts a group whose **value** fails conversion as well, not only one whose key does. That
is strictly better - it is the treatment entry mode already gives an incompatible value - and it gives
a body to one of the five silent swallows `00-verified-facts.md` §9 lists. It is also more than the
defect asked for. The narrower version diverts only in the `key == null` branch and leaves the catch
empty; it reaches both grouping sites just the same, and it is the right call if the cycle wants the
smallest possible diff inside the library's busiest factory.

**Before and after at a real site - `member/crimson/Kuudra.java`, the class the probe was run on.**
The DTO is unchanged by the fix itself:

```java
@Capture(filter = "^highest_wave_")
private @NotNull ConcurrentMap<Tier, Integer> highestWave = Concurrent.newMap();
@Capture
private @NotNull ConcurrentMap<Tier, Integer> completedTiers = Concurrent.newMap();
```

Given `{"highest_wave_none": 5, "none": 1, "brand_new_tier": 4, "another_new_tier": 2}`:

| | `completedTiers` | Overflow | Serialized back |
| --- | --- | --- | --- |
| Before | `{null=2, BASIC=1}` - `brand_new_tier`'s value overwritten by `another_new_tier`'s | empty | `BASIC` only; both new tiers gone |
| After | `{BASIC=1}` | `{brand_new_tier: 4, another_new_tier: 2}` | all three keys, byte-exact |

That is the whole of the correctness fix, and it needs **no consumer edit at all**: the data stops
being destroyed and the document round-trips. Making the unmatched keys *visible* is the optional
second step, and it is `dgx-extract-filter`'s element rather than anything new here:

```java
@Capture
private @NotNull ConcurrentMap<Tier, Integer> completedTiers = Concurrent.newMap();
@Extract("completedTiers")
private @NotNull ConcurrentMap<String, Integer> unknownTiers = Concurrent.newMap();
```

`unknownTiers` then holds `{brand_new_tier: 4, another_new_tier: 2}` - typed, iterable, and still
re-injected on write, into the root under those same two keys, with the root-level `unknownTiers` key
the binder emits removed by §2.3. Note the key type is `String`, not `Tier`: these are by definition
the keys no `Tier` constant matched, and an enum-keyed remainder would reproduce the defect one level
up (§3.6 #6). `Dojo` takes the same two lines per field, with the same shape.

**The companion field is only free because §2.3 removes its key.** Adding an `@Extract` field to a DTO
on today's library adds a root-level `"unknownTiers": {...}` object to every serialize of that class -
Java field name, nested shape, invented by the model. On a sha carrying §2 it adds nothing to the
document at all, which is what makes "per-site and additive" true.

**The adoption is per-site and additive.** Six sites get the library fix at once; each decides
separately whether it wants a companion field. Four can add one immediately; the two grouping-mode
sites can only once branch B lands, because until then their overflow is always empty.

### 4.4 Why this supersedes the declined entry

**What was declined.** `d10-capture-unmatched` proposed an element on `@Capture`:

```java
boolean skipUnmatchedKeys() default false;
```

with one branch in the classify pass: on a `null` conversion, either insert as today or `continue`.
`10-annotation-designs.md` §9.4 declined it on its own terms - `skipUnmatchedKeys = true` "**loses the
entry outright and breaks round-trip fidelity**", which is the cost `00-conventions.md` §4 says must
be stated - and §9.5 preferred `@Fallback` because it "fixes all seven sites through the enum adapter
with no change to `CaptureTypeAdapterFactory` at all".

**The pack also named the right answer, and declined it for a reason that no longer holds.** §9.4,
verbatim: "There is a third option nobody proposed and it should be named so it is not mistaken for a
gap: divert unmatched keys to the `@Capture` overflow that already exists. That preserves round-trip
fidelity *and* loses nothing, but it needs a way to read the overflow back - and `@Extract` addresses
a single named key, not 'every entry that failed key conversion'."

Both halves of that blocker are removed upstream of this entry, and neither is removed by anything
inside `CaptureTypeAdapterFactory`:

- **`dgx-overflow-store`** makes the `@Capture` store reachable from `@Extract` at all. Before it, the
  store is write-only on the read path and the claim reads a frame-local that only `@Lenient` fills,
  so a diverted entry would sit in a map nothing could ever look in.
- **`dgx-extract-filter`** supplies the missing axis: "every entry that failed key conversion" is a
  dotless `@Extract` with an empty filter.

So the difference between the declined entry and this one is not a better branch in the same place.
It is that the entry no longer has to choose between losing the data and leaving it unreadable:

| | `d10-capture-unmatched` | This entry |
| --- | --- | --- |
| Element added to `@Capture` | `skipUnmatchedKeys()` | **none** |
| Unmatched entry | dropped | kept in overflow under the document's own key |
| Round trip | **broken** - the key vanishes from the output | exact, including the two grouping-mode sites |
| Readable back | no | yes, through one `@Extract` companion field, per site and optional |
| Consumer must opt in | yes, per site, or keep the defect | no - the fix is unconditional |
| `CaptureTypeAdapterFactory` | one branch plus element plumbing | one clause in an existing predicate, plus a build-time divert |

**Precision about what still changes inside the busiest factory**, because overclaiming here would be
the easiest way to make this entry wrong. Two branches do land in `CaptureTypeAdapterFactory` (§4.3).
What does *not* land is a key-handling **policy**: no element, no default, no per-site opt-in, no new
mode to reason about at the other eleven `@Capture` sites, and no behaviour that a consumer can
configure into losing data. The clause in §4.2 makes the enum key path behave the way the `Integer`
key path and both value paths already behave. The blast-radius argument that carried the decline - "two
changes competing for the same seven sites is one change too many; take the one that edits the smaller
file" - was an argument against **adding a policy** to that file, and it is not an argument against
correcting a predicate in it.

**The counting error this acts on.** `README.md` §6.2 declined two rows - "`@Lenient` typed-overflow
element" and "`@Capture` unmatched-key element" - as "one site each". They are not two thin proposals.
They are two symptoms of one missing capability: `@Extract` has no filter axis and cannot reach the
`@Capture` store. Counted as one mechanism the evidence is **seven sites** - one `@Lenient` mix and six
enum-keyed `@Capture` maps - plus a defect that destroys N-1 values per field. That is what §2, §3 and
§4 answer together, and it is why they ship as one commit rather than three.

The neighbouring row is **not** part of this group and stays declined. "`@Capture` value-grouping
element" (`d10-capture-value-grouping`) concerns bind-side grouping-mode **selection**, not overflow;
its payoff is one carrier class and eight lines against a change to the grouping-selection logic that
twelve files depend on. Nothing here reopens it.

### 4.5 Failure modes and the information-loss objection

**The objection, stated at full strength.** Diverting an entry to overflow shrinks the bound map. A
consumer that iterates `completedTiers` today sees an entry for the unknown tier - under a `null` key,
with only one of the N values, but an entry. After the change the map has only the keys that resolved,
and unless somebody adds an `@Extract` companion the unknown ones are invisible from the object
graph. So the fix trades a **visible wrong answer** for an **invisible right one**, and that is a real
trade rather than a rhetorical one.

Three things settle it. The `null` key is not a signal anyone consumes - it is a key no caller can
name, and `{null=4, BASIC=1}` reads as `size() == 2` to every caller that does not special-case it.
The entries are not discarded: they are in the overflow, in the document, and one annotation away from
being typed. And the alternative that keeps them in the map keeps only **one of N**, which means the
"visible" answer is visible and wrong about four fifths of the data.

| # | Condition | Before | After |
| --- | --- | --- | --- |
| 1 | One unmatched enum key, entry mode | `null` key inserted | diverted to overflow under the document's key, round-trips |
| 2 | One unmatched enum key, grouping mode | `null` key inserted | every entry that fed the group diverted, under `literalPrefix` plus its filtered key |
| 3 | N unmatched keys on one field | all collapse onto `null`, N-1 values destroyed | N separate overflow entries, nothing destroyed |
| 4 | Key matches a constant, a `@SerializedName` or an `alternate`, in any case | binds | unchanged - `CaseInsensitiveEnumTypeAdapterFactory` still resolves it first |
| 5 | No `@Extract` companion on the field | `null` key visible in the map | map is smaller; entries live only in the overflow until a companion is added |
| 6 | Grouping-mode **value** conversion failure | dropped silently at `:477-478` | diverted, or still dropped if the narrow option in §4.3 is taken |
| 7 | `descend = true` `@Capture` | n/a - no descend field is a defect site | diverted entries follow the descend target automatically, because merge-back already picks the nested node at `:242-244` |
| 8 | The enum is later marked with `@Fallback` | n/a | **lossy again unless `dgx-fallback`'s companion clause lands with it** - a fallback-resolved key is non-null and would be judged compatible. `02-fallback.md` §10 makes that a required companion edit, not an optional one |
| 9 | A plain enum-keyed `Map` field with no `@Capture` | collapses onto `null` | **unchanged and unreachable.** `Statistics.java`:89 and roughly forty other enum-keyed maps bind through gson's stock map adapter. Only an enum-adapter-scoped change reaches them, and that is `xlarge` and out of this cycle (§4.6) |
| 10 | Store growth | six fields publish nothing | six fields publish a non-empty overflow on every profile read, so `WeakIdentityMap` holds more entries and `sweep()`'s full scan runs over a larger key set. Bounded by the same object lifetimes |

**Coverage, and it is the worst in the cycle.** `04-compatibility.md` §6.3 G6:
`filterWithEnumKey_ok` (`:738`) and `bareEntryGroupingWithEnumKey_ok` (`:966`) both use **only
matching** enum names, so the `{null=4, BASIC=1}` collapse has no test signal at all today, and
`captureOverflowMergesBackOnWrite` does not exist either (G5). The characterisation test has to be
written against `7cfc181` **first**, asserting the collapse as current behaviour, and then inverted -
otherwise the new test pins the new behaviour and proves only that the code does what it does. Minimum
set: an unmatched key in entry mode, two unmatched keys in entry mode (the N-1 assertion), an unmatched
group in grouping mode with a filter and one without, a round-trip assertion on each, and the
`@Extract` companion reading them back.

### 4.6 The cheaper alternative

**A - `@Fallback` on the key enum.** The research pack's reason for declining `d10-capture-unmatched`:
one line per enum, no factory edit, and the busiest file in the library stays out of the blast radius.
It is genuinely cheaper and it **does not fix this defect**, for three reasons that were established
after the pack was written:

- `02-fallback.md` §6.3 rule 1 requires the marked constant to have **no wire representation of its
  own**. `Kuudra.Tier.BASIC` carries `@SerializedName("NONE")` (`Kuudra.java`:28-29), so marking it
  turns the probe input into `{BASIC=4}` where it produces `{null=4, BASIC=1}` today - the unknown
  tier now overwrites a **correct** entry. That is worse than the defect at the site the defect was
  discovered at.
- Nine of the eleven otherwise-eligible enums in the module have **no sentinel constant to mark**,
  because their existing default is itself a live wire value.
- Even where it is eligible, it collapses every unmatched key onto one constant with later entries
  overwriting earlier ones - the same N-1 loss, made typed and confident - and the write path then
  fabricates a key named after the constant, so the document does not round-trip either.

`02-fallback.md`'s own entry block already records the conclusion: it **does not answer**
`f06-capture-null-enum-key`, and that finding belongs here. The two entries are complementary, not
competing, and §4.2's absent clause is the seam between them.

**B - the declined element, `skipUnmatchedKeys = true`.** Cheapest of all and the only one that is
strictly worse than doing nothing at the document level: the entry vanishes from the serialized output
as well as from the map. §4.4 is the full argument.

**C - fix it in the enum adapter.** Make `CaseInsensitiveEnumTypeAdapterFactory.read`:82 throw instead
of returning `null` for an unmatched name. This is the only route that reaches `Statistics.java`:89
and the roughly forty other enum-keyed maps that bind through gson's stock map adapter, which is real
value that no `@Capture`-scoped change can deliver. It is also `xlarge`: `create` (`:35-38`) claims
**every enum type in the JVM**, in every position, for every consumer - hypixel declares 31 enums,
`skyblock` 20, `asset-renderer` 90 - and it is invisible to all 134 plus 16 tests. `04-compatibility.md`
§9.2 is unambiguous that if it lands it needs its own cycle, its own tests and a convergence of all
twelve sibling pins. **Its reach is exactly its blast radius.** Not in this cycle, and explicitly not
batchable with anything in it.

**D - accept the defect.** Named for completeness. The cost is four values per `Kuudra` profile in the
probe, silently, forever, and the same shape at five more sites.

### 4.7 Verdict

**Adopt.** `medium`.

`medium` is `00-conventions.md` §4's price for "modifies an existing factory's read path", plus a
regression pass over every existing user of that factory - sixteen live `@Capture` fields, all in
hypixel. The rating does not double-count `dgx-overflow-store`'s `large`: the enabling cost is booked
there, and what is booked here is one clause, one build-time divert, one extra frame-local, and the
tests that close G5 and G6.

Three conditions on the adopt, all of them stated in full above rather than implied:

1. **It ships with `dgx-overflow-store` and `dgx-extract-filter`, in that order, in one commit.** On
   its own the diverted entries round-trip and are unreadable, which is the state the research pack
   declined.
2. **Branch B may be narrowed** to the `key == null` case only, leaving the `:477-478` catch empty. It
   still reaches both grouping-mode sites. Take the narrowing if the cycle wants the smallest diff in
   the library's busiest factory.
3. **Whenever `dgx-fallback` lands, its companion clause lands with it.** A marked enum resolves to a
   non-null constant, which would be judged compatible and would silently turn this lossless behaviour
   back into a lossy one.

**What it subsumes.** `d10-capture-unmatched`, declined as "subsumed by `d10-fallback`" - a
substitution that §4.6 shows does not hold, because `@Fallback` cannot be applied to the enum at the
site the defect was proved at. It also removes `f06-capture-null-enum-key` from `@Fallback`'s scope
permanently, which `02-fallback.md` has already accepted, so the two entries stop competing for the
same six sites.

**What it does not fix.** Every enum-keyed map that binds through gson's stock `MapTypeAdapterFactory`
- roughly forty fields, including `Statistics.java`:89 - still collapses unmatched keys, onto `null` or,
if §5 marks the key enum, onto the sentinel. Either way the original key is gone. That is a different
code path, it needs an enum-adapter-scoped change, and that change is `xlarge` and belongs to its own
cycle. The consumer-side answer available today is `@Lenient` on the map, which moves it into this
entry's reach.

## 5. dgx-fallback - `@Fallback`

- **Registry entry:** `@Fallback` - "supplies a default when the key is absent or the value fails to
  bind, replacing sentinel constants plus `getOrDefault` accessors"
- **Verdict:** adopt narrowly - the failed-bind half only, as an enum-constant marker, and only on
  enums that pass the eligibility rule in §5.5
- **Category:** `correctness`
- **Answers findings:** `f06-enum-null-clobber`, `f03-enum-unknown-null`, `f04-enum-null-fallback`;
  partially `f04-enum-of-parsers`. **Does not** answer `f06-capture-null-enum-key` - that finding
  belongs to §4. Explicitly does not answer `f06-completedat-zero-sentinel` or
  `f04-lookup-sentinel-drift`
- **Cheaper alternative:** none for the field-value path. For the map-key path there is one, it is
  cheaper, and it is better - it is §4 (§5.7)
- **Library change:** existing factory edit - `CaseInsensitiveEnumTypeAdapterFactory` for the marker
  itself, plus the four compatibility guards in `CaptureTypeAdapterFactory` and
  `LenientTypeAdapterFactory` (§5.4), which are part of this change - plus one additive annotation file.
  Three edited factories, not one
- **Adoption sites today:** 14 enum-valued bind sites behind 12 in-module enums, 8 of which need a
  sentinel constant added before they can be marked. 3 more sit behind a second publish cycle in
  `Simplified-Api/skyblock`
- **Effort:** `medium` for the in-module half - four library files, which is inside the band even with
  the companion guards counted. `large` if the cross-module `Rarity` / `GameMode` half is in the same
  step, because that is a second publish cycle in a different repo and `00-conventions.md` §4 bumps a
  two-cycle proposal one level

**One count moves out of this entry.** `02-fallback.md`'s own block adds "plus the 2 `@Capture` key
sites on `Kuudra.Tier`". Those are no longer this entry's, and the reason is §4.7 condition 3: once a
fallback-resolved key is judged **incompatible** and diverted to overflow, the marker has no effect on
the key path at all. `02-fallback.md` §9 reached the same conclusion and handed the decision to the
`@Capture` entry; this entry records the arithmetic that follows from it.

### 5.1 The problem it removes and the real sites

An enum value the model does not declare reads as `null`, and the reflective binder writes that `null`
over the field's initialiser. So the sentinel-constant-plus-default idiom the DTOs are written in does
not work: a field declared `private Faction selectedFaction = Faction.NONE;` holds `null` the moment
Hypixel ships a faction the enum has not got.

The mechanism is one line. `CaseInsensitiveEnumTypeAdapterFactory.java`:82 is
`nameToConstant.get(in.nextString().toUpperCase())`, and a `Map.get` miss **returns `null` and throws
nothing**. Every other leaf conversion in the pipeline throws on an unusable value, which is why every
other type already behaves sanely - and it is the same asymmetry §4.2 corrects one level up, from the
other side.

**The sites, re-verified against source and against the real fixture**
(`src/main/resources/craftedfury.json`) rather than assumed. The research pack's "roughly a dozen
one-line consumer edits" does not survive that check: **eight of the twelve eligible enums have no
sentinel constant to mark**, because their existing default is itself a live wire value
(`02-fallback.md` §7). One departure to record: that section's summary line once said nine while its own
group B table listed eight, and the table is the authority - four enums are markable as they stand and
eight are not, which is twelve.

| Group | Enums | What it costs to mark |
| --- | --- | --- |
| **A - mark now** | `CrimsonIsle.Faction` (`NONE`), `DungeonData.Type` (`UNKNOWN`), `BoardQuest.Status` (`UNKNOWN`), `DungeonClass.Type` (`UNKNOWN`) | one line each. The sentinel already exists and the fixture never carries it |
| **B - needs a new sentinel first** | `Kuudra.Tier`, `Kuudra.SearchSettings.Sort`, `RabbitSort`, `RabbitFilter`, `Crystal.State`, `Banking.Action`, `CommunityUpgrades.Type`, `DungeonChest.Type` | a new `UNKNOWN` constant plus a look at the enum's own `values()` consumers |
| **C - deferred for cost** | `Rarity` | no sentinel **and** a chained publish cycle through `Simplified-Api/skyblock`. Not a safety gate (§5.5) |
| **D - not worth the sentinel** | `Floor`, `Statistics.Mythos.Type`, `Statistics`' nested boss `Type`, `RabbitEmployee`, `GlaciteTunnels.CorpseType`, `HypixelSocial.Type` | these exist only as plain map keys, where the marker buys one unknown key turning typed and nothing else. Permitted, close to pointless. `JacobsContest.Medal` is a genuine "do not mark" for the unrelated `Optional` reason in §5.6 #9 |
| **F - cross-module** | `GameMode`, and `Rarity` again | a chained publish cycle through `Simplified-Api/skyblock` for three field sites. Deferred |
| **G - under-modelled, not under-defaulted** | `ActiveCommission.Status`, and `RabbitSort` in part | the marker would **hide** the defect. Fix the model first |

**Two corrections the entry is built on, both against the research pack's §7.**

`Kuudra.Tier.BASIC` must not be the marked constant. `Kuudra.java`:28-29 declares it
`@SerializedName("NONE")`, so `BASIC` **is** on the wire. Marking it turns the probe input
`{"none":1,"brand_new_tier":4}` into `{BASIC=4}` where it produces `{null=4, BASIC=1}` today: the
unknown tier overwrites a **correct** entry. A fallback constant with a wire representation of its own
destroys real data instead of parking unknown data. That is the eligibility rule of §5.5, and it is the
only mechanical one.

The pack's headline claim that one marker fixes both the field-value path and the map-key path is
still false, but for a weaker reason than an earlier draft of this entry gave. That draft said gson's
`MapTypeAdapterFactory` turns a tolerated duplicate into a thrown `JsonSyntaxException` on about
**forty** enum-keyed maps. §5.5 works the cases and withdraws it: that adapter branches on
**duplication**, not on nullness, and two unmatched keys already collide on `null` and already throw
today. With a sentinel the rule forbids naming, the plain-map path is unchanged except that one
unmatched key becomes typed rather than `null`. So the marker **reaches** the map-key path and does no
harm there; what it does not do is **fix** it - the data is still not what the wire said, the key still
does not round-trip, and §4's diversion is still the answer (§5.7).

**Three consumer-side defects must land before any marker, and none of them costs a cycle.**
`BoardQuest.Status` is missing `COMPLETE`; the seven `Dojo.Type` constants carry their wire names as a
constructor component rather than as `@SerializedName`; `RabbitSort` declares `highest_rarity` where
the fixture carries `rarity_high_low`. All three are real data loss **today**, all three are
consumer-only, and `@Fallback` would mask every one of them behind a confident typed sentinel. They
are `trivial` and they must not wait behind a JitPack cycle.

### 5.2 Full declaration

One new file, `dev/simplified/gson/annotation/Fallback.java`. Shape and javadoc follow `Lenient.java`
- a marker with no elements, a `Marks ...` opening, one `<pre>{@code ...}</pre>` example and a `@see`
to the implementing factory - so the file reads as a sibling of the seven annotations already in the
package rather than as an import from a design note.

```java
package dev.simplified.gson.annotation;

import dev.simplified.gson.factory.CaseInsensitiveEnumTypeAdapterFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the enum constant an unrecognized JSON value deserializes to.
 * <p>
 * Without this marker an unrecognized value reads as {@code null}, and the reflective binder writes
 * that {@code null} over the field's initialiser - so a sentinel default such as {@code UNKNOWN}
 * never survives a value the enum does not declare. Marking one constant makes that sentinel the
 * bind result instead. A JSON null still reads as {@code null}, so an absent value stays
 * distinguishable from an unrecognized one.
 * <p>
 * At most one constant per enum may carry this marker; a second one fails the enum's first decode.
 * An enum with no marked constant is unaffected.
 * <p>
 * The marked constant should be a sentinel that carries no wire representation of its own. Marking a
 * constant that incoming JSON can also name makes every unrecognized value indistinguishable from
 * that constant, and collapses them onto it.
 * <p>
 * A map key that resolves to the marked constant is treated as unconvertible by
 * {@link Capture @Capture} and {@link Lenient @Lenient}, so it is kept in the field's overflow under
 * the key the document carried rather than collapsed onto the constant.
 * <p>
 * Example:
 * <pre>{@code
 * public enum Type {
 *
 *     @Fallback
 *     UNKNOWN,
 *     CATACOMBS
 *
 * }
 * }</pre>
 * JSON {@code "some_new_floor"} produces {@code Type.UNKNOWN} rather than {@code null}.
 *
 * @see CaseInsensitiveEnumTypeAdapterFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Fallback { }
```

The fourth paragraph is this document's addition to `02-fallback.md` §3's declaration. It states the
seam with §4 where a consumer will look for it, and it is the sentence that stops somebody reading
"unrecognized values become the constant" as covering map keys as well.

Three notes on the declaration.

- **`ElementType.FIELD` is exact, not a workaround.** An enum constant is a `public static final` field
  of the enum type, and `CaseInsensitiveEnumTypeAdapterFactory.java`:51 already reaches constant
  annotations that way (`enumClass.getField(constant.name()).getAnnotation(SerializedName.class)`).
  `ElementType.TYPE` would be wrong - the marker names a constant, not a type - and it would also make
  this the library's first type-level annotation, which §6.5 declines for `@Flatten` on the same
  grounds.
- **No elements.** Every element added later is a source-compatible addition only if it carries a
  default (`00-verified-facts.md` F4), and there is no evidence for one. In particular there is no
  `keys = false` element: the marker sits on the constant, so it cannot see whether the constant is
  being read as a map key or as a field value. That asymmetry is handled by eligibility (§5.5) and by
  §4's diversion, not by an element.
- **No `@Documented`, no `@Inherited`.** Neither appears on any of the seven existing annotations, and
  `@Inherited` does not apply to fields at all.

### 5.3 Usage before and after

**The group A case, which is the whole adoption for three sites.**
`member/crimson/CrimsonIsle.java`:26-27 and its nested `Faction`, verbatim as they stand:

```java
@SerializedName("selected_faction")
private @NotNull Faction selectedFaction = Faction.NONE;
```

```java
public enum Faction {

    NONE,
    @SerializedName("mages")
    MAGE,
    @SerializedName("barbarians")
    BARBARIAN

}
```

After - **the field does not change**, which is the point of choosing a constant marker over a
field-level annotation:

```java
public enum Faction {

    @Fallback
    NONE,
    @SerializedName("mages")
    MAGE,
    @SerializedName("barbarians")
    BARBARIAN

}
```

Given `{"selected_faction": "cultists"}`, `selectedFaction` is `NONE` instead of `null` on a field
declared `@NotNull`. `NONE` is unreachable from the wire - the only two `@SerializedName` values are
`mages` and `barbarians`, and the fixture only ever carries `barbarians` - so rule 1 of §5.5 holds and
no real value is ever attributed to the sentinel.

**The group B case, and why it is not a one-liner.** `member/crimson/Kuudra.java`:25-33 is the enum the
research pack said to mark on `BASIC`:

```java
public enum Tier {

    @SerializedName("NONE")
    BASIC,
    HOT,
    BURNING,
    FIERY,
    INFERNAL

}
```

`BASIC` is on the wire under the name `NONE`, so marking it is rule 1's exact prohibition. The edit is
a **new constant**:

```java
public enum Tier {

    @Fallback
    UNKNOWN,
    @SerializedName("NONE")
    BASIC,
    HOT,
    BURNING,
    FIERY,
    INFERNAL

}
```

That repairs `Kuudra.SearchSettings.tier` (`:40`, reached through `CrimsonIsle.java`:40) and
`Kuudra.GroupBuilder`'s tier field (`:65`). It does **not** touch `highestWave` (`:17-18`) or
`completedTiers` (`:19-20`), because those are `@Capture` **keys** and §4 keeps an unmatched key in
overflow rather than binding it onto any constant, marked or not. Eight of the twelve in-module enums
need this same shape of edit, and adding a constant is not free - it changes `values()` for every
`Arrays.stream(values())` reduction and every `of`-style static helper on the enum.

**The `DungeonClass.Type` case, which is one edit and an optional second.** It repairs
`Dungeons.java`:39 and `FloorData.java`:109, and its `UNKNOWN` constant already exists with no
`@SerializedName`, so it is a group A one-liner. An earlier draft of this entry held it back behind a
companion `@Lenient` on `Dungeons.java`:32 `classMap` - a plain
`ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>>` that binds through gson's own map
adapter - on the grounds that the marker armed a `JsonSyntaxException` there. §5.5 withdraws that: the
throw needs the marked constant to be nameable from the wire, which the eligibility rule already
forbids, and `classMap` simply goes from `{null=...}` to `{UNKNOWN=...}` on one unknown class and
throws on two exactly as it does today. The `@Lenient` is still worth adding:

```java
@Lenient
private @NotNull ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>> classMap = Concurrent.newMap();
```

It moves the key check into `LenientTypeAdapterFactory`, where §5.4's `isFallback` query is available
and an unmatched key is diverted to overflow under the key the document carried - typed, lossless and
round-tripping, which is more than the marker gives on that path. It is an **upgrade, not a gate**, and
it can land in a later commit. Note that `03-flatten.md` §15 independently recommends retyping
`classMap` to `ConcurrentMap<DungeonClass.Type, DungeonClass>` for unrelated reasons; the two edits are
compatible in either order.

### 5.4 How the factory implements it

**No new factory.** `CaseInsensitiveEnumTypeAdapterFactory` is 87 lines today; the edit adds one field,
reuses the `getField` call the constructor already makes, changes one `return`, and adds one static
query. Four hunks, about 22 changed lines.

**Hunk 1 - imports.** Add `dev.simplified.gson.annotation.Fallback`,
`dev.simplified.gson.exception.JsonException`, `java.lang.reflect.Field` and `java.lang.reflect.Type`.
`Gson`, `TypeAdapter`, `TypeToken`, `NotNull` and `Nullable` are already imported (`:3-12`).

**Hunk 2 - the resolved field**, inside `CaseInsensitiveEnumTypeAdapter` alongside the two maps at
`:43-44`:

```java
private final @Nullable E fallback;
```

**Hunk 3 - the constructor.** The loop at `:47-64` already calls `enumClass.getField(constant.name())`
inside a `try` that catches `NoSuchFieldException`, so the marker lookup costs one extra
`isAnnotationPresent` per constant and **no extra reflection**. Hoisting the `Field` into a local is the
only structural change:

```java
CaseInsensitiveEnumTypeAdapter(@NotNull Class<E> enumClass) {
    E resolved = null;

    for (E constant : enumClass.getEnumConstants()) {
        String writeName = constant.name();

        try {
            Field field = enumClass.getField(constant.name());
            SerializedName annotation = field.getAnnotation(SerializedName.class);

            if (annotation != null) {
                writeName = annotation.value();

                for (String alternate : annotation.alternate())
                    nameToConstant.put(alternate.toUpperCase(), constant);
            }

            if (field.isAnnotationPresent(Fallback.class)) {
                if (resolved != null)
                    throw new JsonException("Enum '%s' declares more than one fallback constant", enumClass.getName());

                resolved = constant;
            }
        } catch (NoSuchFieldException ignored) { }

        constantToName.put(constant, writeName);
        nameToConstant.put(writeName.toUpperCase(), constant);
        nameToConstant.put(constant.name().toUpperCase(), constant);
    }

    this.fallback = resolved;
}
```

`JsonException` is a `RuntimeException`, so it passes straight through the `NoSuchFieldException`
catch. It fires during `create`, which is the enum's first `gson.getAdapter` - in practice its first
decode. Same wrinkle as §2.5: `GsonSettings.prewarm` (`:193-201`) catches `Throwable` per type, so a
prewarmed enum swallows it at warm-up and resurfaces at first real use. Late, not lost.

**Hunk 4 - `read`, plus the published query.** `:82` becomes:

```java
E constant = nameToConstant.get(in.nextString().toUpperCase());
return constant != null ? constant : this.fallback;
```

and the factory gains the one method the other two factories consume:

```java
/**
 * Returns whether a value read as the given type is that enum's fallback constant.
 *
 * @param gson the gson instance whose adapter cache resolves the type
 * @param type the declared type the value was read as
 * @param value the value produced by reading that type
 * @return {@code true} when the value is the enum's fallback constant rather than a declared match
 */
public static boolean isFallback(@NotNull Gson gson, @NotNull Type type, @Nullable Object value) {
    if (value == null)
        return false;

    return gson.getAdapter(TypeToken.get(type)) instanceof CaseInsensitiveEnumTypeAdapter<?> adapter
        && value == adapter.fallback;
}
```

The private-field read is legal - `CaseInsensitiveEnumTypeAdapter` is a nested class of the factory, so
the enclosing class is inside its nest and needs no accessor. That keeps the adapter `private` and adds
no Lombok to a file that has none. `gson.getAdapter(TypeToken.get(enumType))` returns this adapter **by
identity**: `LenientTypeAdapterFactory` and `SerializedPathTypeAdaptorFactory` hand their delegate
straight back for an enum rather than wrapping (`00-verified-facts.md` §2.3) and every other registered
factory returns `null` for one. The single degradation path is a downstream SPI factory that wraps enum
types - then the `instanceof` fails, `isFallback` returns `false`, and the caller falls back to today's
collapse behaviour. Silent, but safe in the direction that matters.

**The four companion guards, which are part of this change and not a follow-up.** Both `@Capture` and
`@Lenient` decide compatibility partly by reading an entry as an enum and testing the result:

| # | Factory | Site | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `@Capture` key | `CaptureTypeAdapterFactory.java`:490-494 | did not **throw** - a `null` result counts as compatible | §4.2 owns this one: compatible only if it neither throws, nor yields `null`, **nor yields the fallback** |
| 2 | `@Capture` value | `CaptureTypeAdapterFactory.java`:538-541 | `result != null` - an unrecognized enum value goes to overflow | add `&& !isFallback(gson, valueType, result)`, which restores today's behaviour exactly |
| 3 | `@Lenient` key | `LenientTypeAdapterFactory.java`:258-264 | did not throw | same shape as row 1, and it is what makes `@Lenient` on a plain enum-keyed map a genuine upgrade (§5.3) |
| 4 | `@Lenient` value | `LenientTypeAdapterFactory.java`:309-312 | `result != null` | same guard as row 2 |

Rows 2 and 4 are the invisible regression, and they are the reason this is a **required** companion
edit rather than an optional one. Today an enum-**valued** `@Capture` or `@Lenient` map keeps an
unrecognized value in overflow and round-trips it. Once the read returns a constant, `result != null`
becomes true, the entry is judged compatible, and it binds onto the fallback - **silently turning a
lossless behaviour into a lossy one**. No site in this module has an enum-valued `@Capture` or
`@Lenient` map today, so nothing here breaks; every sibling module on the shared pin inherits the
change regardless (`00-verified-facts.md` F2), and this is precisely the "visible only in a serialize
test" failure class. A sha carrying `Fallback.java` without rows 2 and 4 is a sha on which marking any
enum silently loses enum-valued overflow. Since no consumer can mark an enum before the annotation
exists, shipping them together costs nothing and removes the window entirely.

### 5.5 Ordering and interaction with the existing factories

**This entry registers no factory, so it adds no index and shifts none.**
`CaseInsensitiveEnumTypeAdapterFactory` is already at list index 0 (`GsonSettings.java`:249), which
after `GsonBuilder.create()`'s reverse makes it the **last** user factory consulted and therefore the
**innermost** of the eight - a leaf, never a wrapper. It claims exactly one thing (`create` returns
`null` unless `rawType.isEnum()`, `:35-36`) and delegates to nothing. Every ordering invariant in
`00-verified-facts.md` §11 A1-A7 is untouched, and `GsonSettings.defaults()` is not edited. No other
entry in this cycle can say that; the `medium` rating comes purely from "edits an existing factory".

**Everything reaches it, and nothing can intercept its result.** A leaf has no delegate to hand a
repaired value to, and no wrapper above it inspects the enum result before it lands:

| Caller | Site | What it reads as an enum |
| --- | --- | --- |
| gson reflective binder | `ReflectiveTypeAdapterFactory.java`:265-274 | every enum-typed field |
| gson map adapter | `MapTypeAdapterFactory.java`:196-205 | **keys and values of every enum-keyed plain map** |
| gson collection adapter | `CollectionTypeAdapterFactory` | enum elements of a list or set |
| `@Capture` build | `CaptureTypeAdapterFactory.java`:398, `:399`, `:474`, `:475` | captured keys and values |
| `@Capture` classify | `CaptureTypeAdapterFactory.java`:491, `:539` | the compatibility probe |
| `@Lenient` filter | `LenientTypeAdapterFactory.java`:260, `:310` | the compatibility probe |
| `@Extract` assign | the fresh `gson.fromJson` in `ExtractTypeAdapter.assign` (§2.3) | an extracted value bound from the top of the chain |
| `@Split` | `SplitTypeAdapterFactory.java`:153-154 | both halves of a delimited pair |
| `@Collapse` / `@Key` | `CollapseTypeAdapterFactory.java`:253-257 | an enum-typed injected key |
| `Optional<E>` | `OptionalTypeAdapterFactory.java`:54 | `Optional.ofNullable(adapter.read(in))` |
| `@SerializedPath` | `SerializedPathTypeAdaptorFactory.java`:132 | a fresh top-of-chain bind |

The research pack read that table as pure upside. Rows 4 through 11 can all be taught about the marker
through `isFallback`. **Rows 1 through 3 cannot** - they are gson's own code, and row 2 is the one that
needs working rather than asserting.

**The row that was called blocking, worked properly.** `MapTypeAdapterFactory`'s read loop is
`V replaced = map.put(key, value); if (replaced != null) throw new JsonSyntaxException("duplicate key: " + key);`
(gson 2.11.0 `:196-205`). Roughly **forty** enum-keyed maps in this module bind through it -
seventeen in `FloorData.java` (`:24-68`) alone, nine across `Statistics.java`, two in
`JacobsContest.java`, plus `Dungeons.classMap`, `ChocolateFactory.employees`,
`GlaciteTunnels.lootedCorpses` and `HypixelSocial.links`. A `null` key is not special to that loop:
`dev.simplified.collection.ConcurrentHashMap` is backed by a plain `HashMap` (`:20-27`), so
`put(null, v)` stores and returns the previous value like any other key, which is why `{null=3}` is
reachable at all and why two unmatched keys already throw today.

For `ConcurrentMap<Floor, Integer>`, **two markings have to be kept apart**, and an earlier draft of
this entry conflated them. Marking a constant the wire can name - `ENTRANCE`, with its
`@SerializedName("0")` (`Floor.java`:13-28):

| Wire | Today | Marking a nameable constant |
| --- | --- | --- |
| `{"0":5, "8":3}` | `{ENTRANCE=5, null=3}`, silent | **`JsonSyntaxException: duplicate key`** - a document that decodes today stops decoding |
| `{"8":3}` | `{null=3}`, silent | `{ENTRANCE=3}` - foreign data **attributed to a real floor**, silent and undetectable |
| `{"8":3, "9":1}` | `JsonSyntaxException` | unchanged |

Both bad rows are real and both are already forbidden by the rule below, for the independent
`Kuudra.Tier.BASIC` reason. They are evidence about that rule, not about the map path. Marking a
rule-compliant sentinel - a new `UNKNOWN`, no `@SerializedName`, no `alternate`, a `name()` the wire
never carries - is the only marking this entry ever recommends:

| Wire | Today | Marking a sentinel |
| --- | --- | --- |
| `{"0":5, "8":3}` - one unknown | `{ENTRANCE=5, null=3}` | `{ENTRANCE=5, UNKNOWN=3}` - **better**: typed, iterable, and it cannot break a `keySet()` pipeline the way a `null` key does |
| `{"8":3}` - one unknown | `{null=3}` | `{UNKNOWN=3}` - same improvement, and nothing is attributed to a real floor |
| `{"8":3, "9":1}` - two unknowns | `JsonSyntaxException` | `JsonSyntaxException` - unchanged, identical mechanism |
| the sentinel's own key on the wire | n/a | **unreachable** - that is what the rule says |

**There is no regression row.** The throw needs two entries on one key, and today two unmatched entries
already collapse onto `null` and already throw; the marker changes the key in the message and nothing
else. **The position is still not adjustable** - there is no index at which an enum leaf stops being
reached by gson's own map adapter - but with a compliant sentinel that no longer matters, because the
reach is harmless. What the marker does not buy on this path is the fix: one unknown key becomes typed,
two still abort, and neither round-trips, because a `null` key writes as the string `"null"` and a
sentinel writes as `"UNKNOWN"`, both keys Hypixel never sent.

**The eligibility rule that follows - one mechanical rule, not two.** `@Fallback` is safe on an enum if
and only if:

1. **The marked constant has no wire representation of its own** - not via `name()`, not via
   `@SerializedName.value`, not via an `alternate`, compared **case-insensitively** because
   `CaseInsensitiveEnumTypeAdapterFactory.java`:82 uppercases the incoming string and `:63` registers
   `constant.name().toUpperCase()`. Otherwise unknown values overwrite a correct entry
   (`Kuudra.Tier.BASIC`, §5.1) or, on a stock-bound map, abort the decode.

What an earlier draft made a second rule - never mark an enum that keys a plain map - is not an
eligibility rule and is withdrawn. It over-restricted: it is what put `DungeonClass.Type` behind a gate
and put six enums in a "never mark" group, and neither survives the table above. It leaves behind a
**preference**: an enum whose only appearances are stock-bound map keys is not worth a sentinel, since
the marker buys almost nothing there. Where the enum also has a field-value site, mark it. The genuine
improvement on the map path is `@Lenient` plus §4's diversion, which sends an unmatched key to overflow
under the document's own key - lossless and round-tripping - and that is an upgrade available
independently of the marker, not a gate on it. A second rule is a modelling precondition rather than a
mechanical one: **do not mark an enum that is merely incomplete** (group G).

**Interaction with the other four entries.**

| Entry | Interaction |
| --- | --- |
| §2 `dgx-overflow-store` | None structurally - no store, no factory, no shared state. One constraint on §2: `ExtractTypeAdapter.assign` must not start treating a `null` conversion **result** as a failure. Its catch fires on a thrown conversion, and a `null` enum assigns cleanly today. None of the six `@Extract` sites is enum-typed |
| §3 `dgx-extract-filter` | None. A typed remainder lands through the same fresh `gson.fromJson`, so it inherits the fallback behaviour with no coordination. §3.6 #6 already warns that an enum-keyed remainder target reproduces the collapse one level up, which the eligibility rule does not cover and §3 declares rather than fixes |
| §4 `dgx-capture-unmatched` | **The one real coupling, and it runs both ways.** §4's null half needs no marker and stands alone; §4.7 condition 3 makes the fallback half of row 1 a required companion whenever this entry lands, or a marked enum silently turns §4's lossless key behaviour back into a lossy one. The two can ship in either order, in separate cycles, with no window where either is wrong |
| §6 `dgx-flatten` | None beyond sharing a publish. A flattened scalar that is enum-typed routes through the same leaf adapter, which is the intended reach |

### 5.6 Failure modes and malformed input

| # | Condition | Behaviour |
| --- | --- | --- |
| 1 | Value matches a constant name, any case | unchanged - the constant (`:63`, `:82`) |
| 2 | Value matches a `@SerializedName` value or an `alternate`, any case | unchanged - the constant (`:57`, `:62`). The fallback never shadows a real match |
| 3 | Value matches nothing, enum marked | the marked constant. The whole of the fix |
| 4 | Value matches nothing, enum unmarked | `null`, exactly as today |
| 5 | JSON null | `null`, unchanged - `:77-80` returns before the lookup, so absence stays distinguishable from an unrecognized value |
| 6 | Two constants marked | `JsonException` at adapter construction, which is the enum's first decode. Swallowed by `GsonSettings.prewarm` (`:197-200`) and rethrown at first real use |
| 7 | Marker on a non-enum field | **inert, silently** - `create` never builds the adapter for a non-enum. The exact failure class this cycle keeps finding, and §5.7 argues for accepting it |
| 8 | Serializing the fallback constant | its own name or `@SerializedName` value - `write` (`:68-73`) is not edited and `constantToName` is unchanged. An `UNKNOWN` that arrived as `"necromancer"` writes as `"UNKNOWN"` |
| 9 | `Optional<E>` field, unmatched value | `Optional.of(fallback)` instead of `Optional.empty()` - `OptionalTypeAdapterFactory.java`:54 is `Optional.ofNullable(...)`, so `isPresent()` flips. One site, `JacobsContest.java`:113, and this row is the whole reason `JacobsContest.Medal` is a "do not mark" |
| 10 | `@Split` half, unmatched value | the fallback rather than whatever `PairOptional.of` does with a `null` half. `SplitTypeAdapterFactory.java`:153-159 sits inside the empty catch at `:160-161`, so today's behaviour there is unobservable either way. One site, `TrophyFishing.lastCaught` |
| 11 | Enum-**valued** `@Capture` or `@Lenient` map, unmatched value | **regression unless §5.4 rows 2 and 4 land with it** - the entry stops going to overflow and binds onto the fallback |
| 12 | Enum-**keyed** `@Capture` map, unmatched key | with §4: diverted to overflow under the document's key. Without §4 and with the marker: every unmatched key collapses onto one constant, later entries overwrite earlier ones, and the write path fabricates a key named after the constant |
| 13 | Enum-keyed **plain** map, one unmatched key | `{UNKNOWN=v}` where today it is `{null=v}` - typed and iterable rather than a `null` key, and still not what the wire said. A small improvement, not a fix. §5.5 |
| 14 | Enum-keyed plain map, two or more unmatched keys | `JsonSyntaxException` - unchanged, this already throws today, because two `put` calls under `null` already collide |
| 15 | Enum-keyed plain map, marked constant is nameable from the wire | `JsonSyntaxException: duplicate key`, aborting the whole decode. **A document that decodes today stops decoding** - and this is an eligibility-rule violation, not a property of the map path |
| 16 | Enum-keyed plain map, serialized back | the fabricated key changes from `"null"` to `"UNKNOWN"`. Neither round-trips |
| 17 | The enum is under-modelled rather than under-defaulted | the marker converts a detectable `null` into an undetectable wrong value. `RabbitSort` is the live instance |

**The unchanged-behaviour guarantee, stated precisely**, because it is the entire argument for an
opt-in marker. `this.fallback` is `null` for an unmarked enum, so
`return constant != null ? constant : this.fallback;` returns exactly what `return nameToConstant.get(...)`
returns today, for every input. `nameToConstant` is populated identically - hunk 3 hoists the
`getField` call into a local and adds a branch, it does not touch the three `put` calls at `:61-63`.
`write` is not edited. `create` still returns `null` for every non-enum type. No factory is added or
moved. And `isFallback` returns `false` for an unmarked enum without a special case, because
`adapter.fallback` is `null` and the method's first line rejects a `null` value - which is the answer
that preserves current behaviour in all four compatibility checks.

**Rows 11, 15 and 17 are where the real risk lives, and none of it is in the code.** Eight lines in a
leaf adapter is near-zero risk. Row 11 is a commit-contents problem and §5.4 fixes it by construction.
Rows 15 and 17 are adoption-list problems: the annotation, once it exists, is trivially applicable to
any enum, reads as obviously beneficial, and is actively harmful whenever the marked constant is a
value the wire can name - which is a property of the upstream API's vocabulary, not of the Java, so it
produces no compile error, no test failure and no log line. **The safety of this design lives in the
adoption list, not in the library.** That is testable, but only in the consumer: walk every enum under
`response/`, find its marked constant, and fail if it carries a `@SerializedName` or an `alternate` at
all, or if its `name()` appears case-insensitively in the fixture's vocabulary for a field of that
type. Roughly thirty lines, no new dependency, and it turns "a future contributor marks
`Kuudra.Tier.BASIC` because it is the natural-looking default" from a production incident into a red
test. Nothing in the library can enforce it, because the library cannot see the consumer's fields or
its fixture. An earlier draft proposed a different guard, scanning for enum-keyed plain maps; §5.5
withdrew the rule that guard enforced, so it is replaced rather than kept.

**Coverage.** A `FallbackTests` nest in `GsonFactoryTest`, two model enums - one marked, one not.
The rows that would not exist if this entry had been written from the research pack alone are the ones
that matter: `unmarkedEnum_unmatchedValue_null` (the opt-in claim, made checkable),
`markedEnumValue_captureOverflowPreserved` and `markedEnumValue_lenientOverflowPreserved` (the §5.4 row
2 and row 4 guards), `markedEnumKey_plainMap_collapses`, which pins §5.5's sentinel sub-cases as
**known** behaviour so nobody discovers them in production, and
`nameableFallbackConstant_plainMap_throws`, which pins the nameable-constant case as the thing the
eligibility rule exists to prevent rather than as a property of the map path. Consumer side, write the
confirming test **before** the library change, on a group A enum. The research pack's
`{"selected_dungeon_class": "necromancer"}` into `Dungeons` stands as written - `DungeonClass.Type` is
group A. Add `{"selected_faction": "cultists"}` into `CrimsonIsle` asserting `Faction.NONE` as a
second, since `CrimsonIsle.Faction` has no map exposure at all and isolates the field-value path. Both
fail today with `null`, which is the confirmation the finding is real.

### 5.7 The cheaper alternative

**For the field-value path there is none, and the reason is mechanical.** The sentinel-plus-initialiser
idiom the DTOs already use is the cheap answer, and it does not work: the reflective binder assigns the
`null` over the initialiser, so no consumer-side default survives. Four candidates were checked and
each is either not cheaper or not equivalent.

**A - defaulting in the accessor.** `@Getter(AccessLevel.NONE)` plus a hand-written
`getSelectedFaction()` returning `this.selectedFaction == null ? Faction.NONE : this.selectedFaction`.
Zero library cost and it works. It is also `accessor-boilerplate` re-introduced at every one of fourteen
sites, on `@NotNull`-declared fields that would still be null, which is the idiom this pack exists to
delete. It does not scale and it lies about nullability.

**B - fix the `@SerializedName` coverage instead.** For three of the fourteen sites this is not merely
cheaper, it is **correct where `@Fallback` is merely tolerable**: `BoardQuest.Status` is missing
`COMPLETE`, `RabbitSort` is missing `rarity_high_low`, and seven `Dojo.Type` constants carry their wire
names as a constructor component rather than as annotations. `@Fallback` would turn each of those from
a detectable `null` into a confident wrong constant. **Do B first regardless of what happens to this
entry** - it is `trivial`, consumer-only, and costs no cycle. What B cannot do is cover a value the
upstream API has not shipped yet, which is the whole of the residual case.

**C - a hand-written `TypeAdapter` per enum, or a field-scoped `@JsonAdapter`.** The stock-first rule
makes this the honest comparison, and for one enum it is genuinely cheaper than a cycle. For twelve it
is twelve adapter classes in `response/`, which is the hand-rolled deserialization the pack is
deleting, and `@JsonAdapter` takes only a class literal so nothing is shared between them.

**D - do nothing.** The cost is fourteen `@NotNull` fields that hold `null` whenever Hypixel ships a
value the model does not declare, and a `null` that surfaces as an NPE somewhere far from the decode.
Named for completeness; it is the status quo the three findings describe.

**For the map-key path the cheaper alternative exists, it wins, and it is in this document.** §4
diverts an unmatched enum key to the field's overflow under the key the document carried. Set the two
against each other at `HeartOfTheMountain.powder`, where the fixture yields six affix groups -
`mithril`, `gemstone`, `glacite`, `buff`, `ghast`, `ghast_1` - and `Powder.Type` declares three:

| | In-memory | Written back |
| --- | --- | --- |
| Today | `{MITHRIL, GEMSTONE, GLACITE, null=<last of buff/ghast/ghast_1>}` | `powder_null` - a key Hypixel never sent; two groups gone |
| With `@Fallback` | `{..., UNKNOWN=<last of the three>}` | `powder_UNKNOWN` - still fabricated; still two groups gone |
| With §4's diversion | `{MITHRIL, GEMSTONE, GLACITE}` | `powder_buff`, `powder_ghast`, `powder_ghast_1` verbatim; nothing lost |

`@Fallback` buys exactly one thing there - the poisoned entry is typed and iterable instead of being a
`null` key that breaks a `keySet()` pipeline far from the decode. That is a real improvement over
`null` and a much smaller one than diverting, which costs a comparable edit and loses nothing. This
entry therefore **cedes the key path to §4 rather than competing for it**, which is also why
`f06-capture-null-enum-key` is not in its answers list and why the two `Kuudra.Tier` key sites are not
in its adoption count.

### 5.8 Verdict

**Adopt narrowly.** `medium` for the in-module half; `large` if the cross-module `Rarity` / `GameMode`
half is taken in the same step, because `00-conventions.md` §4 bumps a two-cycle proposal one level.

`medium` survives the corrected blast radius rather than depending on the understated one. Counting
files the way §4 asks: four library files (`Fallback.java` new, plus
`CaseInsensitiveEnumTypeAdapterFactory`, `CaptureTypeAdapterFactory` and `LenientTypeAdapterFactory`
edited), which is inside `medium`'s "4-15 files, or an edit to an existing factory" band and nowhere
near `large`'s "new ordering guarantee between factories" clause - this entry registers nothing and
reorders nothing. Three edited factories rather than one raises the regression pass, not the level.

Narrow in three ways: the **failed-bind half only** (the absent-key half of the registry line stays
cut - a missing key never reaches an enum adapter at all), the **enum-constant marker** rather than a
field-level annotation or a naming convention, and **only on enums that pass §5.5's eligibility rule**.

Six conditions on the adopt, each of which is stated in full above rather than implied.

1. **Ship the four compatibility guards in the same commit and the same publish** (§5.4). A sha
   carrying the annotation without them silently converts enum-valued overflow from lossless to lossy
   for every consumer on the shared pin.
2. **Land the consumer-side naming fixes first** (§5.7 B). Every one of them is a defect `@Fallback`
   would mask rather than fix, and none of them costs a cycle.
3. **Obey §5.5's eligibility rule and encode it as a consumer test** (§5.6). No marker on a constant
   with a wire representation, checked case-insensitively across `name()`, `@SerializedName.value` and
   every `alternate`. `Kuudra.Tier` gets a new `UNKNOWN`, never `BASIC`. There is no second mechanical
   rule - an enum that keys a plain map is not disqualified.
4. **Defer `Rarity` and `GameMode`.** They live in `Simplified-Api/skyblock`, which pins its own
   gson-extras sha (`build.gradle.kts`:44, `2ba8143` against hypixel's `7cfc181`), so they cost a
   chained publish cycle for three field sites.
5. **Write the confirming test before the library change**, on a group A enum.
6. **Grep the sibling modules for code relying on a `null` enum before landing.** Cheap insurance while
   every marked enum is in this module; load-bearing the moment condition 4 is scheduled.

**What it subsumes.** `d10-fallback`, accepted in the research pack. The shape is confirmed; three of
its supporting claims are not, and each is corrected above: `Kuudra.Tier.BASIC` is not markable, the
site count is smaller and differently shaped than "17 fields plus 7 maps", and one marker does **not**
serve both the field-value and the map-key path - it reaches the second and does no harm there, but it
does not fix it.

**One correction this entry makes against an earlier draft of itself.** That draft carried a second
eligibility rule - never mark an enum that keys a plain map - and derived it from a duplicate-key throw
that only fires when the marked constant is nameable from the wire, which rule 1 already forbids. §5.5
withdraws it. The effect is that `DungeonClass.Type` is markable now rather than gated on a companion
`@Lenient`, that the "never mark" group loses six of its seven enums to a much weaker "not worth the
sentinel", and that the adoption count rises from 12 sites behind 11 enums to 14 behind 12. The error
was conservative - it forbade too much - but it was still an error, and the verdict is unchanged either
way.

**What it does not fix.** `f06-capture-null-enum-key` - that is §4's, permanently, and the two entries
stop competing for the same six sites. The roughly forty enum-keyed plain maps get one unknown key
turned typed and nothing more; the real answer for them is `@Lenient` plus §4's diversion, which this
entry does not schedule. And an enum that is under-modelled rather than under-defaulted is made worse,
not better, which is a property of the adoption list rather than of the code.

Ships in the same cycle as §6 and separately from §2-§4 (§7). **It is not file-disjoint from them**:
its four companion guards (§5.4) edit `CaptureTypeAdapterFactory`:490-494 and `:538-541` and
`LenientTypeAdapterFactory`:258-264 and `:309-312`, and those are the same two files §2, §3 and §4
rewrite. §7's "Who touches what" table is the authority on that overlap, and it is why this entry wants
its own commit even inside a shared publish, and why the cycle-2 / cycle-1 split is a **commit-order**
argument rather than a file-disjointness one.

## 6. dgx-flatten - `@Flatten`

- **Registry entry:** `@Flatten` - "collapses a single-valued JSON object (or single-field value class)
  into the scalar or collection the caller actually wants, removing a wrapper level"
- **Verdict:** adopt narrowly - field-level only, `Map` and `Collection` only, mutually exclusive with
  `@Capture`, `@Lenient` and `@SerializedPath`, and lossy by declaration
- **Category:** `value-shape-collapse`
- **Answers findings:** `f03-mapvalue-single-key`; partially `f03-dungeons-classmap-funnel`;
  explicitly declines `f03-biomewhispers-tier`
- **Cheaper alternative:** stock `com.google.gson.annotations.JsonAdapter` on the field - re-examined
  in §6.7 and no longer cheaper, because its one advantage was zero library cycles and the cycle is now
  sunk
- **Library change:** additive file - one annotation, one factory, one line in `GsonSettings.defaults()`
- **Adoption sites today:** 1
- **Effort:** `small`

The `d10-flatten` id is carried forward from the research pack. `00-conventions.md` §3 freezes ids, so
this entry refines that one rather than replacing it, and the `dgx-` prefix marks only that the
decision now belongs to this cycle.

### 6.1 The problem it removes and the real sites

**The site is one field.** `member/Currencies.java`:18 declares a map of maps where the caller wants a
map of values, and pays for it with a suppressed getter and a five-line stream accessor:

```java
@Getter(AccessLevel.NONE)
private @NotNull ConcurrentMap<String, ConcurrentMap<String, Integer>> essence = Concurrent.newMap();

public @NotNull ConcurrentMap<String, Integer> getEssence() {
    return this.essence.stream()
        .mapValue(value -> value.get("current"))
        .collect(Concurrent.toMap());
}
```

The wire shape, from `profiles[1].members[...].currencies` in the fixture:

```json
"essence": {"WITHER": {"current": 1955}, "DRAGON": {"current": 1132}, "UNDEAD": {"current": 5141},
            "DIAMOND": {"current": 8564}, "SPIDER": {"current": 312}, "GOLD": {"current": 3445},
            "ICE": {"current": 2557}, "CRIMSON": {"current": 4}}
```

Every value is a single-member wrapper. The declared type carries the wrapper level; the accessor
deletes it again; the public surface has always been the collapsed type. That is exactly
`value-shape-collapse` - "a map-of-maps that should be a map-of-values" - and it is the only place in
`response/` where the wrapper is uniform enough to be declarative.

**The count is 1, and the two obvious neighbours are deliberately not counted.**

`f03-dungeons-classmap-funnel` is **partially** answered and should not be adopted with this
annotation. `Dungeons.java`:32 `classMap` is
`ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>>`, and the cheaper answer is to retype
it to `ConcurrentMap<DungeonClass.Type, DungeonClass>`, which binds the existing JSON directly and
deletes six lines of `Dungeons.postInit()`. Zero library cost, more deleted lines than `@Flatten`
delivers, and it does not wait for a cycle. Note it is the same field §5.3 wants `@Lenient` on for an
unrelated reason; the two edits compose.

`f03-biomewhispers-tier` is **explicitly declined**, and the blocker is mechanical rather than
economic. `HeartOfTheForest.BiomeWhispers.tiers` is a grouping-mode `@Capture` field with a catch-all
filter. `CaptureTypeAdapter.read` allocates a fresh empty `knownObject` (`:264`), classifies every root
key into it or into the frame-local captured and overflow maps (`:311-363`), and only then calls
`delegateAdapter.fromJsonTree(knownObject)` (`:366`). Every captured key is **absent** from the tree
the inner chain receives, so an inner `@Flatten` on that field would see nothing, forever, in any
registration order that keeps `@Capture` outer - which §6.5 does. §6.4's `create` rejects the pair
rather than shipping a silent no-op. The zero-cost answer at that site is
`@Getter(AccessLevel.NONE)` plus switching `getSpent(int)` from `this.getTiers()` to `this.tiers`.

**What the library gains that no existing annotation has.** Every one of the seven annotations in
`dev.simplified.gson.annotation` addresses a **field** or a **key**: `@SerializedPath` descends to a
node, `@Capture` claims keys, `@Collapse` turns an object into a list with the key injected, `@Split`
divides one string. None of them reaches the **value** side of a collection entry. That capability, not
the seven deleted lines at `Currencies`, is the positive case for the file.

### 6.2 Full declaration

`dev/simplified/gson/annotation/Flatten.java`. The research pack's declaration with three edits: the
line recommending `@Lenient` as the mitigation for a missing wrapper is **gone** (§6.5 proves that pair
corrupts the round trip), the exclusions and the write-side loss are stated, and the `@see` names the
file that implements it.

```java
package dev.simplified.gson.annotation;

import dev.simplified.gson.factory.FlattenTypeAdapterFactory;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Map;

/**
 * Collapses a single-key wrapper object into the value it holds, for every entry of a
 * {@link Map Map} or {@link Collection Collection} field.
 * <p>
 * During deserialization each entry value is replaced by the member named by {@link #value()}
 * before the field is bound, so the field declares the collapsed type rather than the wrapper.
 * During serialization each entry value is wrapped back under the same name.
 * <p>
 * The collapse is a projection, not a lossless transform. Any other member of the wrapper is
 * read past and is not written back, so a wrapper that carries more than the named member does
 * not round-trip. An entry that is not an object, or that is an object without the named
 * member, is left as it stands and is then typed by the field's own value type.
 * <p>
 * Cannot be combined with {@link Capture @Capture}, {@link Lenient @Lenient} or
 * {@link SerializedPath @SerializedPath} on the same field - each pair is rejected when the
 * adapter is built. A statically keyed sub-object needs no collapse;
 * {@link SerializedPath @SerializedPath} already addresses it by path.
 * <p>
 * Example:
 * <pre>{@code
 * @Flatten("current")
 * private ConcurrentMap<String, Integer> essence = Concurrent.newMap();
 * }</pre>
 * JSON {@code {"WITHER": {"current": 1955}}} produces {@code essence = {WITHER: 1955}} and
 * serializes back to {@code {"WITHER": {"current": 1955}}}.
 *
 * @see FlattenTypeAdapterFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Flatten {

    /**
     * The wrapper member read out of each entry on deserialize and written back on serialize.
     *
     * @return the wrapper member name
     */
    @NotNull String value();

}
```

Two house-style notes, both of which are deliberate departures rather than oversights.

The opening line is a third-person verb rather than the noun phrase the house rule asks for, because
every sibling in the package opens with one (`Split.java`:14 "Splits a single JSON string value...",
`Lenient.java`:13 "Marks a `Map` or `Collection` field..."). One new file is not the place to break a
package's convention; if the rule wins, all eight annotation files change in one commit.

`{@link Capture @Capture}`, `{@link Lenient @Lenient}` and `{@link SerializedPath @SerializedPath}`
resolve without imports because all four types share the `dev.simplified.gson.annotation` package - the
same-package case is not an inlined FQN. `Map` and `Collection` do need the imports shown, exactly as
`Lenient.java`:9-10 carries them for the same reason.

**The third paragraph is the one a reviewer should refuse to let be trimmed.** It is the entire
declaration of §6.6's round-trip loss, and it is the difference between a documented projection and a
surprise.

### 6.3 Usage before and after

`member/Currencies.java`, 26 lines, verbatim as it stands:

```java
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

After, 19 lines:

```java
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

One nested generic parameter, one `@Getter(AccessLevel.NONE)`, one five-line stream accessor and the
`lombok.AccessLevel` import go; one import arrives. `getEssence()` survives, generated by the
class-level `@Getter`, and **its signature is unchanged** - it already returned
`ConcurrentMap<String, Integer>`, which is the finding.

Traced against §6.4 and §6.5: `Currencies` carries no `@Capture`, `@Lenient`, `@SerializedPath`,
`@Split`, `@Extract` or `@Collapse` field and does not implement `PostInit`, so every other factory
returns `null` or the bare delegate for this type and `FlattenTypeAdapter` is the **only** wrapper
above the reflective binder. It is therefore also the adapter that pays the stream-to-tree buffer, and
that cost is new for this class - `Currencies` previously bound straight off the stream. On a 25-key
object inside a 1.6 MB document it is not measurable, but it is real and should not be called free.

The delegate then binds `essence` as `ConcurrentMap<String, Integer>`, which resolves through the
`collections` SPI factory - `ConcurrentTypeAdapterFactory` maps `ConcurrentMap` to `ConcurrentHashMap`
and re-parameterises the token - so the actual binder is gson's stock `MapTypeAdapterFactory` with
`TypeAdapters.INTEGER` on the value side. That is the adapter that decides what a malformed entry does,
and §6.6 is where that decision bites.

**One behaviour change no reader should miss.** Today a wrapper missing `current` produces
`value.get("current") == null` and the caller silently receives a null map value, because
`Concurrent.newMap()` is backed by a `HashMap` that accepts them. Under `@Flatten` the same input is
left as an object, fails `TypeAdapters.INTEGER`, and aborts the read of the **whole document**. §6.6
sizes that trade; it is the row that decides whether the annotation is safe to ship.

### 6.4 How the factory implements it

`dev/simplified/gson/factory/FlattenTypeAdapterFactory.java`, modelled on `SplitTypeAdapterFactory` -
the smallest self-contained factory in the library at 243 lines, and the only one whose shape is a
per-field transform rather than a per-object one.

```java
@NoArgsConstructor
public final class FlattenTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> @Nullable TypeAdapter<T> create(@NotNull Gson gson, @NotNull TypeToken<T> typeToken) {
        ConcurrentList<FlattenFieldInfo> flattenFields = FlattenFieldInfo.of(typeToken.getRawType());

        if (flattenFields.isEmpty())
            return null;

        TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, typeToken);

        return new FlattenTypeAdapter<>(delegateAdapter, gson.getAdapter(JsonElement.class), flattenFields);
    }
```

The field-info holder and the scan that builds it. Serialized-key resolution is copied verbatim from
`SplitTypeAdapterFactory.java`:197-199, so `@Flatten` addresses its field the way every other factory
does - `@SerializedName` value if present, otherwise the Java field name:

```java
    @Getter
    private static final class FlattenFieldInfo {

        private final @NotNull FieldAccessor<?> accessor;
        private final @NotNull String serializedName;
        private final @NotNull String member;

        private FlattenFieldInfo(@NotNull FieldAccessor<?> accessor) {
            this.accessor = accessor;
            this.serializedName = accessor.getAnnotation(SerializedName.class)
                .map(SerializedName::value)
                .orElse(accessor.getName());
            this.member = accessor.getAnnotation(Flatten.class)
                .map(Flatten::value)
                .orElse("");
        }

        private static @NotNull ConcurrentList<FlattenFieldInfo> of(@NotNull Class<?> clazz) {
            Reflection<?> reflection = new Reflection<>(clazz);
            reflection.setProcessingSuperclass(false);
            ConcurrentList<FlattenFieldInfo> result = Concurrent.newList();

            for (FieldAccessor<?> accessor : reflection.getFields()) {
                if (Modifier.isTransient(accessor.getModifiers()))
                    continue;

                if (!accessor.hasAnnotation(Flatten.class))
                    continue;

                Class<?> rawType = accessor.getFieldType();

                if (!Map.class.isAssignableFrom(rawType) && !Collection.class.isAssignableFrom(rawType))
                    throw new JsonException("Field '%s' carries @Flatten but is neither a Map nor a Collection", accessor.getName());

                if (accessor.hasAnnotation(Capture.class))
                    throw new JsonException("Field '%s' cannot carry both @Flatten and @Capture", accessor.getName());

                if (accessor.hasAnnotation(Lenient.class))
                    throw new JsonException("Field '%s' cannot carry both @Flatten and @Lenient", accessor.getName());

                if (accessor.hasAnnotation(SerializedPath.class))
                    throw new JsonException("Field '%s' cannot carry both @Flatten and @SerializedPath", accessor.getName());

                FlattenFieldInfo info = new FlattenFieldInfo(accessor);

                if (info.getMember().isEmpty())
                    throw new JsonException("Field '%s' carries @Flatten with an empty member name", accessor.getName());

                result.add(info);
            }

            return result;
        }

    }
```

Four things in that scan are load-bearing.

`Reflection.setProcessingSuperclass(false)` matches every other scan in the library, so `@Flatten` is
**not inherited from a superclass field** - the library's uniform answer, stated because it is the
shared gotcha.

**Returning `null` when idle is what keeps the other 132 DTO classes off the new path**, and it is the
same rule §2.3 applies to `ExtractTypeAdapterFactory`. `LenientTypeAdapterFactory.create`:70-72 and
`SerializedPathTypeAdaptorFactory.create`:39-41 hand back the delegate instead, which changes which
factory a **third** factory's `getDelegateAdapter` resolves to (`00-verified-facts.md` A7). Two new
factories land in this cycle and neither may join that pair.

`ConcurrentMap` satisfies `Map.class.isAssignableFrom` and `ConcurrentList` satisfies
`Collection.class.isAssignableFrom`, because `dev.simplified.collection.ConcurrentMap` declares
`extends Map<K, V>` (`ConcurrentMap.java`:23). The check is on the raw declared type, so it accepts the
interface form the DTOs actually use.

**The five throws are a new precedent.** No factory in the library throws at `create` today;
`SerializedPathTypeAdaptorFactory`:139 is the closest and it throws at read time. §2.5 rows 1-3 propose
the same move for `@Extract` and mark it as the one place that entry trades a silent no-op for an
exception. Here it is cheaper, because `@Flatten` has no existing consumers to break: every rejected
combination is a declaration nobody can have written yet. `JsonException` is the library's canonical
failure type and its `(@PrintFormat String, Object...)` constructor is the right one.

**The read path.** No reflective assignment anywhere - the delegate does the typing, which is why this
is simpler than `@Split` (which post-assigns at `SplitTypeAdapterFactory.java`:146-162 and swallows the
failure at `:160-161`):

```java
        @Override
        public @Nullable T read(@NotNull JsonReader in) throws IOException {
            JsonElement rootElement = this.getJsonElementAdapter().read(in);

            if (!rootElement.isJsonObject())
                return this.getDelegateAdapter().fromJsonTree(rootElement);

            JsonObject rootObject = rootElement.getAsJsonObject();

            for (FlattenFieldInfo info : this.getFlattenFields()) {
                JsonElement fieldElement = rootObject.get(info.getSerializedName());

                if (fieldElement == null)
                    continue;

                if (fieldElement.isJsonObject()) {
                    JsonObject collapsed = new JsonObject();

                    for (Map.Entry<String, JsonElement> entry : fieldElement.getAsJsonObject().entrySet())
                        collapsed.add(entry.getKey(), unwrap(entry.getValue(), info.getMember()));

                    rootObject.add(info.getSerializedName(), collapsed);
                } else if (fieldElement.isJsonArray()) {
                    JsonArray collapsed = new JsonArray();

                    for (JsonElement element : fieldElement.getAsJsonArray())
                        collapsed.add(unwrap(element, info.getMember()));

                    rootObject.add(info.getSerializedName(), collapsed);
                }
            }

            return this.getDelegateAdapter().fromJsonTree(rootObject);
        }

        private static @NotNull JsonElement unwrap(@NotNull JsonElement element, @NotNull String member) {
            if (!element.isJsonObject())
                return element;

            JsonObject wrapper = element.getAsJsonObject();

            return wrapper.has(member) ? wrapper.get(member) : element;
        }
```

**The write path, the exact inverse:**

```java
        @Override
        public void write(@NotNull JsonWriter out, @Nullable T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            JsonElement jsonTree = this.getDelegateAdapter().toJsonTree(value);

            if (!jsonTree.isJsonObject()) {
                this.getDelegateAdapter().write(out, value);
                return;
            }

            JsonObject jsonObject = jsonTree.getAsJsonObject();

            for (FlattenFieldInfo info : this.getFlattenFields()) {
                JsonElement fieldElement = jsonObject.get(info.getSerializedName());

                if (fieldElement == null)
                    continue;

                if (fieldElement.isJsonObject()) {
                    JsonObject wrapped = new JsonObject();

                    for (Map.Entry<String, JsonElement> entry : fieldElement.getAsJsonObject().entrySet())
                        wrapped.add(entry.getKey(), wrap(entry.getValue(), info.getMember()));

                    jsonObject.add(info.getSerializedName(), wrapped);
                } else if (fieldElement.isJsonArray()) {
                    JsonArray wrapped = new JsonArray();

                    for (JsonElement element : fieldElement.getAsJsonArray())
                        wrapped.add(wrap(element, info.getMember()));

                    jsonObject.add(info.getSerializedName(), wrapped);
                }
            }

            this.getJsonElementAdapter().write(out, jsonObject);
        }

        private static @NotNull JsonElement wrap(@NotNull JsonElement element, @NotNull String member) {
            if (element.isJsonNull())
                return element;

            JsonObject wrapper = new JsonObject();
            wrapper.add(member, element);

            return wrapper;
        }
```

Four properties of the pair, two of them uncomfortable.

**Zero empty catches.** There is nothing to swallow, because nothing is assigned reflectively. Worth
stating explicitly against a library that carries five (`00-verified-facts.md` §9) and a cycle whose
other entries remove one (§2.3) and give a body to another (§4.3).

**`JsonObject.add` replaces in place and keeps key position**, because `JsonObject` is backed by a
`LinkedTreeMap` whose `put` on an existing key overwrites without re-linking. So swapping the field's
value does not disturb key order in the emitted document.

**The read/write asymmetry is not decorative.** `unwrap` tolerates an entry that is already collapsed -
a bare `1955` - and leaves it alone; `wrap` wraps everything. A document that arrived half-wrapped
therefore serializes back fully wrapped. That is intended normalisation, since the field's declared
type is the collapsed one, but it means `@Flatten` is not byte-identical on a mixed-shape input.

**One aliasing hazard, inherited rather than introduced.** gson 2.11.0's `TypeAdapters.JSON_ELEMENT.read`
short-circuits when the reader is a `JsonTreeReader` (`TypeAdapters.java`:858-861) and returns the
**same** `JsonElement` reference, no copy. So buffering inside an existing tree chain is O(1) - the
marginal cost of one more factory in the chain is a reference return - and a caller that hands a tree
to `gson.fromJson(JsonElement, Currencies.class)` has that tree **mutated in place** by the loop above.
`LenientTypeAdapterFactory` already does exactly this at `:183` and `:197`, so the contract is the
library's rather than this factory's. The alternative, a `deepCopy()` per flattened field, costs a copy
and diverges from the siblings. Match the library.

### 6.5 Ordering and interaction with the existing factories

**State the nesting depth, then derive the index** (`00-verified-facts.md` A1), exactly as §2.4 does.
Intended nesting: **immediately outer to `Lenient`, immediately inner to `Capture`**. In a list that
also carries §2's `ExtractTypeAdapterFactory` that is position 5 of ten:

```java
.withFactories(
    new CaseInsensitiveEnumTypeAdapterFactory(),
    new OptionalTypeAdapterFactory(),
    new SplitTypeAdapterFactory(),
    new SerializedPathTypeAdaptorFactory(),
    new LenientTypeAdapterFactory(),
    new FlattenTypeAdapterFactory(),
    new CaptureTypeAdapterFactory(),
    new ExtractTypeAdapterFactory(),
    new CollapseTypeAdapterFactory(),
    new PostInitTypeAdapterFactory()
)
```

§7 derives that composed list once, for both inserting entries together. Read it there rather than
inferring it from either entry alone - this is the exact place `00-verified-facts.md` H1 says mistakes
are made.

**The honest starting point: the slot is almost entirely unconstrained.** Worked from source rather
than intuition:

| Neighbour | Does it constrain the slot? |
| --- | --- |
| Reflective binder | **Yes.** `@Flatten` rewrites the tree the binder reads, so it must be outer to it. Every candidate index satisfies this |
| `Capture` | No. A `@Flatten` field's serialized name is a **known key** - `discoverKnownKeys` (`:109-146`) walks every non-`@Capture` non-transient field and reads `@SerializedName` at `:135-140` - so the field survives into `knownObject` and reaches an inner `@Flatten` intact. Outer would work too |
| `Lenient` | No, once the same-field pair is excluded. `Lenient` only rewrites its own fields' elements (`:183`, `:197`), `@Flatten` only rewrites its own. Disjoint fields, either order |
| `Extract` | No. It reads no keys from the tree at all - it claims from the overflow store off a bound container (§2.3) - and a `@Flatten` field can never be an `@Extract` source (see below) |
| `SerializedPath` | No. It hands the tree down untouched (`:103`) and binds its own fields with a fresh top-of-chain `gson.fromJson` (`:132`) |
| `Split` | No. It removes only its own `Pair` / `PairOptional` key (`:133`) |
| `Collapse`, `PostInit` | No. Both are outer and whole-object |

So the choice is a tiebreaker and it should be made on a stated principle rather than on "deepest is
safest". The principle: **pick the slot from which the one excluded pair is at least reachable.**

The research pack recommends index 3, which puts `@Flatten` **inner** to `Lenient`. At that depth
`Lenient` sees the uncollapsed wrappers, judges every one incompatible with the declared value type,
and diverts the entire field to overflow before `@Flatten` is ever consulted. The field binds empty.
There is no future in which that composes. At index 5 the read works correctly - collapse first, then
`Lenient` types the collapsed values and overflows only what genuinely failed - and only the **write**
is wrong, for a reason a future overflow-aware design could in principle address. Index 5 is strictly
closer to a working pair at zero cost today. The pack's stated reason for index 3, that a deeper
factory has a smaller blast radius, does not survive the table above: `@Flatten` reads and writes
exactly one key per annotated field and nothing else, at any index.

**`@Capture` is three different questions, and only the first is a conflict.**

*Same field carries both - rejected at `create` (§6.4).* `@Capture` is outer, so every captured key is
absent from the tree the inner chain receives and the `@Capture` field's own serialized name never
appears in the wire document at all for a catch-all or filtered capture. The map is built later still,
at `:377`/`:379`, which `@Flatten` cannot reach. This is the mechanical blocker behind
`f03-biomewhispers-tier` (§6.1).

*Different fields on one class - works, no constraint.* The `@Flatten` field's key is a known key, is
copied verbatim into `knownObject` at `:315-318`, and reaches the inner `@Flatten` intact. No test in
the library covers this pair today; §6.8 asks for one.

*`@Flatten` inside a `@Capture` map's value class - works, and it is the interesting one.* Both map
builders deserialize the value with a fresh top-of-chain lookup (`:399` entry mode, `:475` grouping
mode), which runs the value class's **own** adapter chain including its own `FlattenTypeAdapter`. So
`@Flatten` reaches the value side of a captured entry perfectly well - it just has to be declared on
the value class, not on the captured field. Nothing in `response/` needs it today; it is stated because
it is the composition a reader will wrongly assume is broken after reading the first case, and
assuming it is broken would push a future site toward a hand-written adapter. One adversarial caveat:
§6.4's `create`-time throws fire inside that `gson.fromJson`, which sits in the empty catch at
`:401-402` or `:477-478`, so a mis-declared `@Flatten` on a `@Capture` value class is **swallowed per
entry** and the map silently comes back short. That is an existing defect eating a new diagnostic, and
it is the one direction in which this entry is coupled to the empty-catch work: §4.3's optional
widening of `:477-478` makes `@Flatten`'s diagnostics real.

**`@Lenient` on the same field cannot round-trip in either order, and this is where the entry departs
furthest from the research pack.** The pack says the pair "does not compose at index 3, but the fix is
a move rather than an exclusion". The first half is right; the second half only checks the read. Take
`@Lenient @Flatten("current") ConcurrentMap<String, Integer> essence` against
`{"WITHER": {"current": 1955}, "BAD": {"total": 9}}`:

- **Index 3, `Flatten` inner.** `Lenient` filters first (`:165-200`). Both values are objects, both fail
  `isCompatibleMapEntry` against the declared `Integer`, both go to overflow, and `replaceElement`
  writes an **empty** filtered object back into the tree (`:183`). `@Flatten` then runs on nothing.
  Unusable.
- **Index 5, `Flatten` outer. Read correct, write corrupt.** Read: `@Flatten` collapses `WITHER` to
  `1955` and leaves `BAD` alone; `Lenient` types them, `WITHER` binds and `BAD` overflows. Exactly what
  the pair is for. Write: the chain runs outer to inner, so `FlattenTypeAdapter.write` calls
  `delegateAdapter.toJsonTree(value)` **first**, which runs `LenientTypeAdapter.write` to completion -
  including its merge-back at `:132-138`, which copies `{"BAD": {"total": 9}}` back into the `essence`
  sub-object. `Flatten` then wraps **every** entry, because `wrap` cannot tell a collapsed value from a
  merged-back overflow entry, and emits `{"BAD": {"current": {"total": 9}}}`. Double-wrapped.

Every fix is worse than the exclusion. *Wrap only non-objects* works for `essence` and fails outright
whenever the collapsed type is itself an object. *Wrap only keys present in the bound map* re-implements
`CaptureTypeAdapterFactory.serializeMapKey` for one hypothetical site. *Carry the collapse decision
from read to write in a store* is the correct answer in the abstract - and it is the thing that turns
`@Flatten` from an additive file into a fourth participant in §2's shared-store design, destroying the
orthogonality §6.7 relies on to justify shipping it at all, for zero adoption sites. **Decision:
`create` throws on the pair, and the pack's javadoc line recommending `@Lenient` as the mitigation for
a missing wrapper is deleted.** That line is the single most misleading sentence in the pack's §6,
because it points a reader at the exact combination that silently corrupts their document. What is
left as the mitigation for a missing wrapper is nothing, and §6.6 owns that.

`@Lenient` on field A and `@Flatten` on field B of one class still works, at either index, and no site
needs it today.

**The remaining five, briefly.**

`@SerializedPath` is **excluded**, and for two independent reasons either of which is fatal. There is
no flat-key rewrite on the read side at all - the flat key exists only in `write` (`:67-91`) - so a
`@Flatten` field that also carries `@SerializedPath` has no root-level key for §6.4's
`rootObject.get(...)` to find, at any index. And `gson.fromJson(..., getGenericType())` at `:132` is a
fresh top-of-chain lookup for the field's declared type, so the field never binds through the enclosing
class's chain, which is where its `FlattenTypeAdapter` sits. Note the asymmetry against the value-class
case above: `@Flatten` on a value class binds through that class's own chain and works, because
`@SerializedPath` re-enters at the top of the chain **for the field's type** while `@Flatten` is a
property of the **field**. That distinction is the whole reason `@Flatten` cannot be expressed as a
`TypeAdapterFactory` keyed on the value type.

`@Split` cannot collide - it claims only `Pair` and `PairOptional` raw types, which are neither `Map`
nor `Collection`, so no field can carry both and pass §6.4's shape gate.

`@Collapse` has no interaction today and `@Flatten` must stay inner if one ever appears, which every
candidate index satisfies. `@Collapse` has exactly one adoption site and does not pair with this.

`PostInit` has no interaction - it is outermost and is a pure `read`-then-`postInit()` pass-through, so
`@Flatten` has finished before it is reached. Worth one sentence only because its empty catch at
`:37-38` swallows the entire `postInit()` body, so a `@Flatten`-induced failure surfacing inside one
would vanish. `Currencies` does not implement `PostInit`.

**`@Extract` and `@Flatten` cannot meet, and the guard is already written.** An `@Extract` source must
carry `@Lenient` or `@Capture` (§2.5 row 3, which fails at `create` when it carries neither), and
§6.4 rejects both of those on a `@Flatten` field. So `@Extract("essence.something")` is a `create`-time
`JsonException` from one side or the other, in either build order, with no silent no-op available.
That is the two entries' exclusion rules agreeing by construction rather than by coordination.

### 6.6 Failure modes and malformed input

Behaviour for `@Flatten("current") ConcurrentMap<String, Integer> essence`, which binds through
`ConcurrentTypeAdapterFactory` to gson's `MapTypeAdapterFactory` with `TypeAdapters.INTEGER` on the
value side (§6.3).

| # | Input | Read | Write |
| --- | --- | --- | --- |
| 1 | `{"WITHER": {"current": 1955}}` | `{WITHER: 1955}` | `{"WITHER": {"current": 1955}}` - exact |
| 2 | `{"WITHER": {"current": 1955, "total": 9000}}` | `{WITHER: 1955}`, `total` read past | `{"WITHER": {"current": 1955}}` - **`total` lost** |
| 3 | `{"WITHER": {}}` - named member absent | element left as `{}`, then fails `Integer` | n/a, read aborted |
| 4 | `{"WITHER": {"total": 9000}}` - named member absent | element left as the object, then fails `Integer` | n/a, read aborted |
| 5 | `{"WITHER": 1955}` - already collapsed | binds; `unwrap` passes non-objects through | re-wrapped - **normalised, not preserved** |
| 6 | `{"WITHER": null}` | left as `JsonNull`, binds as a null map value | passed through by `wrap`, then dropped unless `isSerializingNulls` |
| 7 | `"essence": []` - array where a map is declared | the array branch collapses each element, then the map binder fails | n/a |
| 8 | `"essence": 5` - primitive | neither branch matches, handed to the delegate untouched, then fails | n/a |
| 9 | `"essence"` absent | `rootObject.get` returns null, field keeps its initialiser | serialized empty |
| 10 | field is not a `Map` or `Collection` | `JsonException` at `create` | - |
| 11 | field also `@Capture` / `@Lenient` / `@SerializedPath` | `JsonException` at `create` | - |
| 12 | `@Flatten("")` | `JsonException` at `create` | - |
| 13 | field is `transient` | skipped, consistent with every other factory | - |

**Rows 3 and 4 decide whether the annotation is safe to ship, and they are worse than the research
pack's table suggests.** The element is left untouched and typed by the field's own value type.
`TypeAdapters.INTEGER.read` calls `in.nextInt()`, which throws `IllegalStateException` on a
`BEGIN_OBJECT` token - not the `NumberFormatException` its own `catch` converts.
`TypeAdapter.fromJsonTree` catches only `IOException`, so it propagates up the whole delegate chain to
`Gson.fromJson(JsonReader, TypeToken)`, which converts it at `:1371-1372`. The caller sees a
`JsonSyntaxException` and **the entire document read fails**. Sized against today, that is a genuine
availability regression: today the same input yields a null map value and the profile parses; under
`@Flatten` one malformed `essence` entry aborts a 1.6 MB `SkyBlockProfiles` response. The pack chose
"leave a non-conforming element untouched and let the delegate's own typing decide" as the *safe*
design, on the grounds that a wrong shape should surface as a normal gson error rather than a silently
absent entry. That reasoning is right about diagnosis and wrong about blast radius - the error is not
scoped to the field, it is scoped to the response.

Three things make it acceptable, and they belong together rather than assumed:

1. **The pack's recommended mitigation does not exist.** §6.5 proves `@Lenient` corrupts the round trip
   on the same field. There is no in-annotation escape hatch and this entry does not invent one.
2. **The exposure is one field of one class.** The fixture carries `current` on all eight `essence`
   entries across both profiles, and `essence` is the only site.
3. **Failing loudly is this cycle's direction.** The library carries five empty catches and the
   research pack found four dark features behind one of them. Adding a sixth swallow to buy
   availability would be the wrong trade, and it is why §6.4 ships with none.

If that trade is judged unacceptable the correct response is **not** a `tolerant` element - a second
meaning bolted onto a one-site annotation - it is to leave `Currencies` alone. §6.7's do-nothing option
already scores close.

**Round-trip fidelity - the bar this annotation does not clear.** `00-conventions.md` §4 requires the
gap to be declared, so: **`@Flatten` is a lossy projection.** The read takes one named member and
discards the rest; the write reconstructs a wrapper containing only that member. Any sibling member the
document carried is gone from the output. That is structural - the field no longer holds the
information, so there is nothing to write back.

The research pack asserts the opposite ("the value re-serializes correctly instead of not at all"), and
the premise is inverted. Today `essence` is declared
`ConcurrentMap<String, ConcurrentMap<String, Integer>>` and binds the **whole wrapper**, so gson
serializes it back complete, sibling members and all; the collapse happens only in `getEssence()`,
which is a read-side view. **Today's round trip is not accidental, it is total, and `@Flatten` is the
change that breaks it.** Concretely, if Hypixel adds a `total` key:

| | Today | Under `@Flatten("current")` |
| --- | --- | --- |
| Field content | `{WITHER: {current: 1955, total: 9000}}` | `{WITHER: 1955}` |
| `getEssence()` | `{WITHER: 1955}` - `total` invisible | `{WITHER: 1955}` - identical |
| Serialized back | `{"WITHER": {"current": 1955, "total": 9000}}` | `{"WITHER": {"current": 1955}}` |

Caller-visible behaviour is unchanged; the document is not. Three reasons it is still acceptable here,
in decreasing strength: the consuming module **never serializes** - a `toJson` / `toJsonTree` search
across `hypixel/src` returns zero hits, so the loss is unobservable in the only consumer that exists;
the absorptive capacity the wrapper theoretically buys is **already forfeit**, because `getEssence()`
maps every value through `.get("current")` and drops the rest today, so collapsing moves a hidden loss
from an accessor body to a visible field declaration; and it is one field of one class with
single-member wrappers on all eight fixture entries.

What is not acceptable is leaving it undeclared. Two requirements follow: the javadoc paragraph in
§6.2 stays, and the fixture assertion must be **stronger** than "the `essence` key is byte-equal on the
way out". Byte equality passes trivially on today's single-member fixture and keeps passing right up
until the day it silently starts dropping data. Assert instead that every `essence` value object has
exactly one member and that it is named `current` - that is the assertion that fires when the upstream
shape moves, which is the only event that matters.

### 6.7 The cheaper alternative

**A - stock `@JsonAdapter` on the field.** The stock-first rule makes this the alternative that has to
be beaten, and the research pack scored it honestly: it wins on cycles, loses on everything else. The
question this entry has to answer is what happens when its one advantage disappears.

`@JsonAdapter`'s entire case was the zero in the JitPack-cycles column, and that column was decisive
because the pack weighed `@Flatten` as a **standalone** cycle for a single adoption site. This design
cycle publishes gson-extras for §2 through §4 whatever happens to `@Flatten`, so the cycle is sunk and
the column now reads zero for both:

| Option | Consumer lines | Library lines | Marginal cycles | Hand-written adapter in `response/` | Reusable |
| --- | --- | --- | --- | --- | --- |
| Do nothing | 0 | 0 | 0 | no | n/a |
| `@JsonAdapter` + `EssenceAdapter` | -7, +~25 | 0 | **0** | **yes - the only one** | no |
| `@Flatten` | -7 | +~190 | **0** | no | yes |

With the cycles column tied, `@JsonAdapter` retains no advantage and keeps both disadvantages. It
cannot be parameterised by the wrapper key, because `JsonAdapter.value()` is a `Class<?>`, so a second
wrapper key means a second adapter class. And a hand-written `TypeAdapter` living in a DTO package is
exactly the hand-rolled deserialization this pack exists to delete - it would be the only one in
`response/`.

Two things so the comparison is not read as a rout. **`@JsonAdapter` is genuinely more precise about
scope**, and that is a real argument: it attaches to one field and bypasses the enclosing class's
factory chain entirely for that field, so every exclusion rule in §6.5 simply does not arise.
`@Flatten` buys reusability by taking on a composition surface that has to be reasoned about, tested
and documented. And **"the cycle is sunk" is an argument about cost, not about risk** - shipping
alongside the overflow work makes `@Flatten` cheap, not safe. What makes it low-risk is that it shares
no code with that work at all: a new annotation, a new factory, one line in `GsonSettings`. It edits no
existing factory, so the only regression baseline it can disturb is the factory-nesting invariants
§6.5 works through.

**B - do nothing, and it scores better than it looks.** `essence` is `@Getter(AccessLevel.NONE)` and
its only public surface is `getEssence()`, which already returns the collapsed type. The lying type is
visible inside one 26-line file and nowhere else. The positive case for `@Flatten` is therefore not the
seven deleted lines - it is that the collapse becomes declarative and reusable, and that the library
gains the value-side reach §6.1 names.

**C - the two zero-cost neighbours, which beat this entry on deleted lines and should be taken
regardless of what happens to it.** Retype `Dungeons.classMap` to
`ConcurrentMap<DungeonClass.Type, DungeonClass>`, which binds the existing JSON directly and deletes
six lines of `Dungeons.postInit()`; and put `@Getter(AccessLevel.NONE)` on
`HeartOfTheForest.BiomeWhispers.tiers` with `getSpent(int)` switched from `this.getTiers()` to
`this.tiers`. Neither needs this annotation, neither should wait for it, and together they are the
reason the adoption count is 1 rather than 3.

### 6.8 Verdict

**Adopt narrowly.** `small`.

`small` is `00-conventions.md` §4's price for "a new annotation plus a self-contained new factory
registered in `GsonSettings`", which is exactly the shape: two new files, one line in
`GsonSettings.defaults()`, zero edits to any existing factory, one consumer field. Narrow in four ways
- field-level only, `Map` and `Collection` only, mutually exclusive with `@Capture`, `@Lenient` and
`@SerializedPath`, and lossy by declaration.

The research pack's conditional ("if `@Fallback` is accepted, ship `@Flatten` in the same commit; if
`@Fallback` is declined, decline `@Flatten` too") is **satisfied by construction** - this cycle
publishes regardless - so the verdict firms from conditional to unconditional. What changed against the
pack is not the verdict but the small print: a different registration slot (§6.5), one composition
promoted from "resolvable by a move" to "rejected at `create`" (§6.5), and a round-trip claim reversed
(§6.6).

**The class-level form stays declined.** A class-level `@Flatten` - "bind a bare scalar into my sole
field when the incoming JSON is not an object" - has no instance in the fixture, removes no code and no
class, and would give one annotation name two unrelated meanings. This entry adds a fourth ground
specific to the library: it would be the **first type-level annotation in gson-extras**. All eight
annotations are `@Target(ElementType.FIELD)` and every factory's discovery model is a single field walk
with `setProcessingSuperclass(false)`. A class-level form introduces a second discovery axis and forces
an answer to a question no existing factory faces - what an annotation on a **superclass** means, given
that the library's uniform answer for fields is "nothing" - and either answer sets a precedent for the
other seven. If the flip is ever observed it is a new registry row with its own name, not a second
element on this one.

**Coverage, and it is not optional garnish.** A `FlattenTests` nest in `GsonFactoryTest`, plus two rows
in `CombinationTests`, which is the only nest in the library that observes nesting. The write-side rows
matter most: §6.4 establishes that the consuming module never serializes, so **the library's own tests
are the only code that will ever execute `FlattenTypeAdapter.write`**. The rows that carry real
information are `flattenMultiMemberWrapper_roundTrip` (pins §6.6's loss as a declared contract),
`flattenAlreadyCollapsed_read` (pins the normalisation as intended rather than accidental),
`flattenMissingMember_throws` (asserts the exception rather than a silent empty map),
`flattenCollection_read` and `flattenCollection_roundTrip` (the array branch has **zero** adoption
sites, exactly as `Dungeons.unlockedJournals` is the only site exercising `@Lenient`'s array branch),
`flattenIdleType_returnsNull`, the four `create`-time rejections, and the two combination rows -
`flattenSiblingCapture_ok` and `flattenInsideCaptureValue_ok`. Regression anchors: the whole
`CombinationTests` nest, `CaptureGroupingModeTest` and `CollectionValueCompatibilityTest`, and
hypixel's 16 with `Currencies` newly load-bearing. `WeakIdentityMapTest` is explicitly **not** an
anchor - `@Flatten` adds no store, no static state and no cross-call lifetime, and that property is
what §6.7 leans on.

**Sequencing.** Three constraints, in order. Ship it as its **own commit** inside the shared publish,
so it can be reverted without touching the overflow work and vice versa. Land it **after** the overflow
commits in the same branch - it is the lowest-value item in the cycle and the one most easily dropped
if the cycle is cut short, and putting it last keeps that option open. Adopt at `Currencies.essence`
only after the pin bump, in a separate consumer commit.

**Rollback, three independent levels**, which is the practical benefit of the additive shape:

| Level | Action | Cost |
| --- | --- | --- |
| Consumer | Revert the `Currencies` commit | **No re-pin.** The library keeps an unused annotation |
| Registration | Remove the one line from `GsonSettings.defaults()`; annotation and factory stay on disk, inert | one publish and re-pin |
| Library | Revert the `@Flatten` commit entirely | one publish and re-pin, and it does not disturb the overflow commits because they share no files |

The consumer level is the one that matters: any `@Flatten` problem found after the pin bump is undone
by reverting one DTO, with no JitPack cycle at all.

**What would reopen the wider question.** A second wrapper-key family appearing in `response/` turns
this from a one-site annotation into a reusable one and retires §6.7's do-nothing argument outright. A
site that genuinely needs `@Flatten` together with `@Lenient` reopens §6.5, and the answer there is a
read-to-write channel rather than a registration index - which is to say it becomes a dependent of
§2's shared store rather than an independent file.

## 7. How these five fit together

Two of the five register a factory, and each derives its slot from a nesting requirement in its own
section. Composed once, here, so nobody has to infer it from either section alone. §2.4 requires
`Extract` **outer to `Capture`** and inner to `PostInit`; §6.5 requires `Flatten` **outer to `Lenient`
and inner to `Capture`**. Both are satisfied by one list, and registration index still runs opposite to
nesting depth (`00-verified-facts.md` §2.1 - the **last** registered factory is the **outermost**):

| List index in `GsonSettings.defaults()` | Factory | Nesting depth, 1 = outermost | This cycle |
| --- | --- | --- | --- |
| 0 | `CaseInsensitiveEnumTypeAdapterFactory` | 10 - the leaf | **edited** (§5) |
| 1 | `OptionalTypeAdapterFactory` | 9 | - |
| 2 | `SplitTypeAdapterFactory` | 8 | - |
| 3 | `SerializedPathTypeAdaptorFactory` | 7 | - |
| 4 | `LenientTypeAdapterFactory` | 6 | **edited** (§2) |
| 5 | `FlattenTypeAdapterFactory` | 5 | **new** (§6) |
| 6 | `CaptureTypeAdapterFactory` | 4 | **edited** (§2, §4, §5) |
| 7 | `ExtractTypeAdapterFactory` | 3 | **new** (§2, §3) |
| 8 | `CollapseTypeAdapterFactory` | 2 | - |
| 9 | `PostInitTypeAdapterFactory` | 1 | - |

Below depth 10 sit the stock gson platform factories and, innermost of all, the reflective binder. SPI
and `GsonContributor` factories are appended after this list (`GsonSettings.java`:259-263) and
therefore nest **outside** all of it, which is why no ordering guarantee stated here is enforceable
against them (`00-verified-facts.md` F3).

Read the depth column as the call order on read: `Extract` runs after `Capture`, which is the
whole of §2; `Flatten` runs before `Lenient`, which is the whole of §6.5; and
`CaseInsensitiveEnumTypeAdapterFactory` sits at the bottom as a leaf that everything reaches and
nothing wraps, which is the whole of §5.5. `Collapse` stays outer to `Capture` and `Lenient` stays
inner to it, so both dark-ordering pairs the suite pins keep their relative order.

Insertion preserves the relative order of every existing factory, so **no existing pair is reordered by
this cycle**. What it does do is shift four indices, silently and with no test signal today - which is
why §2.7 makes `defaultFactoryOrderIsStable_ok`, asserting the exact class list and order, part of what
this cycle owes.

**Who touches what.** The five entries overlap in three files - `GsonSettings`, which is a registration
list, and the two overflow factories, which §5's companion guards reach into as well:

| Entry | New files | Edited files | Registration slot |
| --- | --- | --- | --- |
| §2 `dgx-overflow-store` | `Overflow.java`, `ExtractTypeAdapterFactory.java` | `LenientTypeAdapterFactory`, `CaptureTypeAdapterFactory`, `GsonSettings` | index 7 |
| §3 `dgx-extract-filter` | none | `Extract.java`, `ExtractTypeAdapterFactory` | none |
| §4 `dgx-capture-unmatched` | none | `CaptureTypeAdapterFactory`, `Capture.java` javadoc | none |
| §5 `dgx-fallback` | `Fallback.java` | `CaseInsensitiveEnumTypeAdapterFactory`, plus four compatibility guards in `CaptureTypeAdapterFactory` and `LenientTypeAdapterFactory` | none |
| §6 `dgx-flatten` | `Flatten.java`, `FlattenTypeAdapterFactory.java` | `GsonSettings` | index 5 |

§2, §3 and §4 all edit `CaptureTypeAdapterFactory` or `LenientTypeAdapterFactory`, which is one reason
they are one commit. §5's four guards land in those same two files, which is the only file-level
contact between the two groups and the reason §5 wants its own commit even inside a shared publish.
**This table is the authority on §5's blast radius**: its entry block and §5.8 describe the marker
itself, which is one factory, but the change as scoped in §5.4 edits three - the guards are declared
"part of this change and not a follow-up", so they count. In particular `CaptureTypeAdapterFactory`:490-494
is the same predicate §4.2 rewrites, so those two edits meet on one line and whichever lands second
writes the combined clause.

**Which entries share a JitPack cycle.** gson-extras publishes by git sha, so a cycle is commit, push,
`jitpack_build`, then edit `hypixel/build.gradle.kts`:44 `strictly("<sha>")` and re-verify standalone.
Three cycles, two re-pins:

| Cycle | Entries | Commits | Re-pin | Why grouped |
| --- | --- | --- | --- | --- |
| **0 - tests only** | none | 1 | **no** | The characterisation tests from §2.7, §4.5 and §5.6, written against `7cfc181` and passing **before** any edit. Tests are not published, so this costs a build and no pin. It is what makes every later green run mean anything |
| **1 - the overflow group** | §2, §3, §4 | 3, in that order | yes | They share the store and cannot be separated at the pin boundary. §2 has zero adoption sites alone, §3 is what makes it verifiable end to end, and §4 is unreadable without both |
| **2 - the additive annotations** | §5, §6 | 2, separately revertable | yes | Behaviourally independent of cycle 1 and of each other; **not file-disjoint from it** - §5's four guards edit the same two factories cycle 1 rewrites, and one of them is the exact predicate §4.2 changes. So cycle 2 must be authored on top of cycle 1's tree, and "separately revertable" means each commit reverts cleanly in reverse order, not that either can be cherry-picked alone. Could fold into cycle 1 for one re-pin; the argument for keeping it apart is that a red hypixel run in cycle 1 is then unambiguous |

Per-item verification costs **no cycle at all**: the workspace composite substitutes
`com.github.simplified-dev:gson-extras` onto the local project, so a build from
`W:/Workspace/Java/Simplified` compiles and tests every consumer against the gson-extras working tree.
Only the final binary-compatibility pass - standalone in the hypixel directory after a re-pin - needs
the published artifact. The staging question is therefore only about how many red runs you are willing
to attribute by hand, not about how much verification you get.

**Three couplings that cross a cycle boundary, all of them one-directional and all stated in full
above.** §4.7 condition 3: whenever §5 lands, the fallback clause in `isCompatibleCaptureEntry` lands
with it, or a marked enum turns §4's lossless key behaviour back into a lossy one. §5.4: the four
compatibility guards ship in §5's own commit, never later. §6.5: §4.3's optional widening of the
`buildGroupedMap` catch is what makes §6's `create`-time diagnostics visible inside a `@Capture` value
class - useful, not required. Nothing else in the five depends on anything else in the five.

**One consumer-side change is not optional and has no cycle of its own.** `scripts/json_dto_diff.py`
reads `@Extract` with a regex that requires a lone string literal and an immediate `)`, so the first
`@Extract(value = ..., filter = ...)` makes it return `None` and report a phantom binding. That patch
is three lines and **must land in the same commit as the first multi-element `@Extract`** (§3.6), or
the only coverage tool the module has becomes noise. The differ also knows neither `@Fallback` nor
`@Flatten`.

## 8. Deferred - not designed in this cycle

### 8.1 `@Owner` / `@Parent` reach-back

**Deferred by the owner until after the DTO research pack lands. It is not designed here, and nothing
in this document should be read as designing it.** No entry above depends on it, constrains it, or
reserves a slot for it; there is no partial implementation, no placeholder element and no
half-specified hook anywhere in §2 through §6.

Three things are recorded so the deferral is a decision rather than an omission.

**It is the one candidate in the registry that needs a lifecycle hook, and that is why it is deferred
rather than folded in.** `00-conventions.md` §4 rates `large` as "a new lifecycle hook the whole
pipeline must honor", and a reach-back is the textbook instance: the enclosing object does not exist
while its children are binding, so nothing a child's own adapter can do produces a parent reference.
Every consumer's adapter chain is in scope, not hypixel's alone.

**The eager-versus-lazy decision recorded in the conversation settles the shape it will have to take.**
Eager was chosen: the parent reference must be present on the child before any consumer touches it,
rather than resolved on first access through a lazy holder. Eager injection cannot happen inside the
child's bind, for the reason above, so **it will need a post-bind hook** - a phase that runs after the
enclosing object is fully constructed and walks its bound children to inject the reference. That is a
statement of consequence, not a design: which factory owns the phase, how it discovers children, what
it does about collections and about cycles, and how it interacts with `PostInit` are all open and all
belong to the entry that eventually gets written.

**This cycle does not foreclose it, and one piece of it is now easier.** §2 establishes the precedent
of a factory whose whole job runs **after** the delegate returns - `ExtractTypeAdapter.read` delegates
first and then assigns off the built object - which is the same structural position a post-bind hook
occupies. Whether a reach-back reuses `ExtractTypeAdapterFactory`'s slot, sits between it and
`Collapse`, or ends up inside `PostInitTypeAdapterFactory` is exactly the kind of question this cycle
must not answer early: guessing it would put an unused ordering guarantee into `GsonSettings` that
§7's order test would then pin.

### 8.2 `@Capture` value-grouping element

**Stays declined, on blast-radius grounds, and it is not part of the overflow capability gap.**

`README.md` §6.2 declined three neighbouring rows. Two of them - "`@Lenient` typed-overflow element"
and "`@Capture` unmatched-key element" - were declined as "one site each", and §3.8 and §4.4 act on the
owner's finding that this was a counting error: they are two symptoms of **one** missing capability,
namely that `@Extract` had no filter axis and could not reach the `@Capture` store. **The
value-grouping row is not the third symptom.** It concerns bind-side grouping-mode **selection** - how
`CaptureFieldInfo` (`:680-693`) infers grouped versus entry mode from the declared value type - and it
has nothing to do with overflow, with `@Extract`, or with the store. Merging it into that group would
be the same counting error in the opposite direction.

Its own economics are unchanged by anything in this cycle: the payoff is one carrier class and about
eight lines against a change to grouping-selection logic that twelve files depend on. Nothing in §2
through §6 reopens it, and one adjacency is worth naming so it is not mistaken for a reopening: §4.3
branch B edits `buildGroupedMap`, but only to divert a group whose key failed conversion. It does not
touch mode inference, does not add a `Grouping` constant, and leaves `Capture.Grouping`'s deliberate
two-constant shape (`AUTO`, `ENTRY`, with no `GROUPED`) exactly as it is.

### 8.3 Open questions carried forward

Two of `04-compatibility.md` §11's six are answered by this document and are recorded here as closed so
the next reader does not re-open them.

**Closed.** *Does the write half of `@Extract` move at all?* Yes - §2.4 moves both halves and shows why
`04-compatibility.md` §5.3's "no index satisfies both directions" holds for its premise (a factory that
post-processes the tree) and that `ExtractTypeAdapter` is not one, because its write work mutates the
store rather than the output JSON. The "read half only, and rename the factory" fallback is not needed.
*Is the enum-key fix `@Capture`-scoped or enum-adapter-scoped?* `@Capture`-scoped, in §4. The
enum-adapter route reaches roughly forty more fields including `Statistics.java`:89, and its reach is
exactly its blast radius - 141 enum declarations across three modules, invisible to all 134 plus 16
tests. If it ever lands it needs its own cycle, its own tests and a convergence of all twelve sibling
pins, and it is explicitly not batchable with anything here.

**Open, and who resolves each.**

| # | Question | Resolved by |
| --- | --- | --- |
| 1 | Do §2.5 rows 1-3 throw at `create` on a misspelled, inherited or unannotated `@Extract` source, or keep today's silent `continue`? It is the one place this cycle trades a silent no-op for an exception, and it is a hard break for any downstream module that has been quietly doing nothing | the owner, before cycle 1 |
| 2 | Is §4.3 branch B taken in full - diverting a group whose **value** fails as well as one whose key does, which gives a body to one of the five silent swallows - or narrowed to the `key == null` case? Both reach the two grouping-mode sites | the implementation plan |
| 3 | Do the characterisation tests ship as cycle 0, or inside cycle 1? Shipping them first is the only way a cycle-1 green run means anything; it costs one extra build and zero re-pins | the implementation plan |
| 4 | Do the twelve sibling pins converge in this cycle? §7's cycle 2 is the natural point, and the measured evidence favours it - hypixel already runs four siblings against a gson-extras none of them was compiled against | the owner |
| 5 | Does anything in the wider workspace actually serialize a `@Lenient`-carrying DTO? Nothing in either suite executes `LenientTypeAdapter.write`. If no production caller does either, the round-trip fidelity §2 is protecting is theoretical - worth a `toJson` search over `response/skyblock` types in the downstream `dev.sbs` modules **before** pricing §2 | a usage search, before cycle 1 |
| 6 | Is `Rarity` / `GameMode` scheduled at all? It is a chained publish cycle through `Simplified-Api/skyblock` for three field sites (§5.8 condition 4) | the owner, after cycle 2 |
| 7 | Should `json_dto_diff.py` learn `@Lenient`? It models overflow nowhere, so it reports a `@Lenient` field as covering keys that actually went to overflow. Out of scope here, but it is a standing false negative in the only coverage tool the module has - and §3's filter element adds a second one | out of this cycle |

One question this document deliberately does **not** carry forward: whether `@Flatten` ships at all. §6
resolves it to adopt, and §6.8 makes it the last commit in the cycle precisely so that dropping it
stays a scheduling decision rather than a design one.
