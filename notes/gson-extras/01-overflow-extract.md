# 01 - Shared overflow store and the `@Extract` lift

## 1. Scope, and how this file is used

Two design entries, `dgx-overflow-store` and `dgx-extract-filter`. The first is a library-internal
refactor with no annotation surface of its own; the second is one added element on `@Extract` that is
only buildable once the first lands. They are written as one file because the second is meaningless
without the first, and because the second is the reason the first is worth its cost.

Every fact cited below is either read off current source in this session, with `file:line`, or taken
from `00-verified-facts.md`, which is authoritative where the seeding brief disagrees with it. Three
places where I disagree with the brief and one where I disagree with the assignment prompt itself are
called out in the open, not smoothed over - §2.1 and §2.7 are both corrections.

Sibling entries `dgx-capture-unmatched`, `dgx-fallback` and `dgx-flatten` are written concurrently.
This file names what it hands them (§6) but does not design them.

## 2. dgx-overflow-store - shared `Overflow` and `ExtractTypeAdapterFactory`

- **Registry entry:** none - library-internal, no new annotation
- **Verdict:** adopt
- **Category:** `correctness`
- **Answers findings:** `f03-questrewards-mixed-values` (through `dgx-extract-filter`),
  `f06-capture-null-enum-key` (through `dgx-capture-unmatched`); answers neither on its own
- **Cheaper alternative:** none - see §2.10, where the two candidates are named and both lose
- **Library change:** existing factory edit, plus two additive files
- **Adoption sites today:** 0 - nothing in any consuming module names this; it is the enabler
- **Effort:** `large`

### 2.1 Why merging the two static stores fixes nothing on its own

The seeding brief and this design agree on the conclusion and disagree on the reason. The conclusion
is worth reaching from the code, because the reason decides the shape of the fix.

There are two stores. `LenientTypeAdapterFactory.java`:62 holds
`WeakIdentityMap<Object, JsonElement>`; `CaptureTypeAdapterFactory.java`:82 holds
`WeakIdentityMap<Object, JsonObject>`. Both are `private static final`. Now read what each is used
for, in both directions.

`@Extract`'s read-side claim is `LenientTypeAdapterFactory.java`:206-220:

```java
FieldOverflow sourceOverflow = overflows.stream()
    .filter(o -> o.fieldName().equals(extractInfo.getSourceFieldName()))
    .findFirst()
    .orElse(null);
```

`overflows` is declared at `:162` - `ConcurrentList<FieldOverflow> overflows = Concurrent.newList()`
- a **method-frame local** of `LenientTypeAdapter.read`. It is filled at `:184` and `:198` by the
filter phase and it is the only thing the extract phase looks at. The static `OVERFLOW` field is not
read anywhere in `read`; its single read site is `:127`, inside `write`.

So: **on the read path both static stores are write-only.** `Lenient` writes to its store at `:239`,
`Capture` writes to its store at `:387`, and nothing reads either until a later serialize. Union the
two maps into one and the extract phase at `:206` still consults `overflows`, still finds only
`@Lenient` field names in it, and still cannot see a single `@Capture` entry. The union is invisible
to the code that would need it.

That is the whole argument, and it holds regardless of which factory wraps which.

### 2.2 What the real gap is, once the ordering claim is dropped

The brief attributes the failure to factory order: "`@Extract` runs BEFORE the delegate, so
`@Capture`'s overflow does not exist yet." `00-verified-facts.md` C1 and C2 establish that this is
backwards. `GsonBuilder.create()` reverses the user factory list (gson 2.11.0
`GsonBuilder.java`:887-890), so the **last** registered factory is the **outermost**, and
`CaptureTypeAdapterFactory` is registered after `LenientTypeAdapterFactory`
(`GsonSettings.java`:253-254). `Capture` therefore wraps `Lenient`, and by the time
`CaptureTypeAdapter.read` reaches `delegateAdapter.fromJsonTree(knownObject)` at `:366` - the call
that eventually enters `LenientTypeAdapter.read` - its classify pass at `:311-363` has already
finished filling `overflowMaps`.

The `@Capture` overflow content **exists** when `@Extract` runs. Two things stop `@Extract` reaching
it:

1. `overflowMaps` is a frame-local (`CaptureTypeAdapterFactory.java`:268) in a stack frame
   `@Extract` is nested inside, with no channel between them.
2. The identity key the store wants - the built `Map` - does not exist yet. It is constructed at
   `:377`/`:379` and installed at `:381`, both **after** the delegate returns at `:366`.

Point 2 is the one that rules out the obvious fix of publishing early. `Capture` cannot put anything
in an identity-keyed store before the delegate call, because the identity does not exist yet.

The resolution this entry takes: **do not make `@Extract` run earlier - make it run later.** Move it
out of `LenientTypeAdapterFactory` into its own factory registered outside `CaptureTypeAdapterFactory`,
so its phase begins after `Capture.read` has returned and both producers have published. At that
point the identity keys exist, the store entries exist, and both are reachable by reflection from the
object that has just been built. No read-scoped channel, no `ThreadLocal`, no reorder of the existing
five factories.

### 2.3 The `Overflow` type - full source

Package-private in `dev.simplified.gson.factory`, alongside `WeakIdentityMap`, which is itself
package-private (`WeakIdentityMap.java`:29) and needs no visibility bump.

The store is **not a map union**. Each entry carries the write target that produced it, because
`@Lenient` merges back into the field's own sub-object (`LenientTypeAdapterFactory.java`:132) and
`@Capture` merges back into the root object or a descending capture's nested node
(`CaptureTypeAdapterFactory.java`:242-244), and both are correct for their own semantics. A claimed
entry has to be returnable to the right one.

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
 * bind produced, so a caller that mutates that container afterwards still finds its overflow, and
 * an entry disappears once its container does.
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
     * @param element the entries themselves - a JSON object for a map-shaped owner, a JSON array
     *     for a collection-shaped one
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
     * Returns the overflow a container holds for the given target, or {@code null} when it holds
     * none or holds one another producer put there.
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
     * The first publisher decides the target - a container already holding an overflow keeps it,
     * and the supplied target is ignored rather than overwriting one a producer is relying on.
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

API notes, each checked against source rather than recalled:

- `WeakIdentityMap` exposes exactly `get`, `put`, `computeIfAbsent` and `size`
  (`WeakIdentityMap.java`:40, :51, :67, :83). `Overflow` uses all four and adds nothing to it - the
  removal a claim needs is a removal **from the stored `JsonObject`**, not from the map, so no
  `WeakIdentityMap.remove` is required. That was the one gap `00-verified-facts.md` §7 flagged, and
  claiming in place closes it without touching the class `WeakIdentityMapTest` covers.
- `JsonObject.keySet()`, `remove(String)`, `add(String, JsonElement)` and `isEmpty()` all exist on
  gson 2.11.0 (verified by `javap` on the resolved jar).
- `claim(owner, Predicate)` snapshots `keySet()` through `Concurrent.newList(Collection)`
  (`Concurrent.java`:379) before removing. Iterating `entrySet()` while calling `remove` on the same
  `JsonObject` is a `ConcurrentModificationException`; the snapshot is not optional.
- The two `claim` overloads differ in their second parameter type, so they resolve without a cast at
  every call site in `ExtractTypeAdapterFactory`.

### 2.4 The `claim` operation, and why it generalises the current exact-key remove

Today's claim is one line, `LenientTypeAdapterFactory.java`:216:

```java
JsonElement claimed = overflowObj.remove(extractInfo.getJsonKey());
```

`claim(owner, String)` is that line with the owner resolved through the store instead of through a
frame-local list. Four properties are deliberately carried over unchanged, and one is deliberately
not.

**Carried over.**

- **Destructive.** The entry leaves the overflow. This is not an optimisation - it is what stops the
  write path emitting the key twice, once from the `@Extract` field's own re-injection and once from
  the producer's merge-back. `00-verified-facts.md` B1 makes it load-bearing at all six sites.
- **Object-shaped only.** An array-shaped overflow returns `null`, exactly as `:214`'s
  `isJsonObject()` guard does today, so an `@Extract` naming a collection-shaped `@Lenient` field
  stays the silent no-op it already is. `Dungeons.unlockedJournals` is the module's only
  collection-shaped `@Lenient` field and carries no `@Extract`, so this preserves the only site that
  could notice.
- **First claimant wins.** Two `@Extract` fields naming the same key means the second finds nothing.
  Unchanged.
- **Original key form.** The claimed key is whatever the producer stored. `@Lenient` stores the key
  as it appeared inside the field's own sub-object; `@Capture` stores the **original unstripped**
  root key (`CaptureTypeAdapterFactory.java`:338, `:358`), never the stripped one. `claim` does not
  normalise between them, and §3.4 is where that asymmetry gets its consequence.

**Not carried over.** Today a claim that fails to convert is lost from both the object and the
document - the entry was removed at `:216` and the conversion failure is swallowed whole at
`:246-247`. `Overflow.restore` exists so the new factory can put a failed claim back. The field still
stays at its initialiser, so `00-verified-facts.md` B7 is honoured and no consumer sees a new
exception, but the entry survives into the merge-back. That is a strict improvement and it costs four
lines.

`claim(owner, Predicate)` is the generalisation. The predicate is the only thing the caller varies:
an exact key is `k -> k.equals(key)` in effect, a filter is `k -> pattern.matcher(k).find()`, and a
catch-all is `k -> true`. The exact-key overload is kept as its own method rather than folded into
the predicate form because the two return different things - one element versus an object of entries
- and collapsing them would force every caller to unwrap.

### 2.5 What changes in `LenientTypeAdapterFactory` and `CaptureTypeAdapterFactory`

Both edits are small and mechanical. Neither factory gains a phase; both lose state.

**`LenientTypeAdapterFactory`.**

| Site | Now | After |
| --- | --- | --- |
| `:62` | `private static final WeakIdentityMap<Object, JsonElement> OVERFLOW` | deleted |
| `:68`, `:70-72` | `create` builds `ExtractFieldInfo.of(...)` and wraps when **either** list is non-empty | builds `LenientFieldInfo` only, wraps only when that list is non-empty |
| `:98-118` | write-side `@Extract` re-injection | deleted - moves to `ExtractTypeAdapter.write` |
| `:127` | `OVERFLOW.get(collection)` | `Overflow.find(collection, Target.FIELD_ELEMENT)` |
| `:203-221` | read-side extract phase | deleted |
| `:239` | `OVERFLOW.put(collection, fieldOverflow.overflow())` | `Overflow.publish(collection, Target.FIELD_ELEMENT, fieldOverflow.overflow())` |
| `:242-248` | post-assign `@Extract`, including the empty catch at `:246-247` | deleted - moves, with `restore` in the catch |
| `:377`, `:456-496` | `ExtractClaim` record and `ExtractFieldInfo` class | move to the new factory |

Net: the class loses roughly 90 lines and one of the library's five silent swallows leaves it. The
`FieldOverflow` record and the whole filter phase are untouched.

One behavioural consequence, and it is an improvement: a class carrying `@Extract` but no `@Lenient`
currently still builds a `LenientTypeAdapter` (`:70`, because `extractFields` is non-empty), which
buffers the entire subtree to a `JsonElement` at `:154` and back at `:224` for an extract phase that
can never match anything (`00-verified-facts.md` §3.1). After the change such a class gets the
delegate handed straight back. None of the six existing sites is of that shape, so nothing in the
module changes, but the dead round-trip goes away for downstream consumers.

**`CaptureTypeAdapterFactory`.** Two lines.

| Site | Now | After |
| --- | --- | --- |
| `:82` | `private static final WeakIdentityMap<Object, JsonObject> OVERFLOW` | deleted |
| `:239` | `JsonObject overflow = OVERFLOW.get(mapObj)` | `JsonElement overflow = Overflow.find(mapObj, Target.SOURCE_OBJECT)`, then the existing loop iterates `overflow.getAsJsonObject().entrySet()` |
| `:386-387` | `if (overflow.size() > 0) OVERFLOW.put(capturedMap, overflow)` | `if (!overflow.isEmpty()) Overflow.publish(capturedMap, Target.SOURCE_OBJECT, overflow)` |

The publish-only-when-non-empty asymmetry against `@Lenient`, which publishes unconditionally
(`:236-239`), is **preserved rather than unified**. Unifying would put an empty entry in the store
for all seventeen `@Capture` fields on every object read, and `00-verified-facts.md` C1 records that
a consumer can depend on `@Lenient`'s empty-container behaviour. The store tolerates absence; the
callers keep their own policy.

### 2.6 `ExtractTypeAdapterFactory` - registration, nesting, and the tree it sees

**State the nesting depth, then derive the index.** `@Extract` must run after both producers have
published, so it must be **outer to `CaptureTypeAdapterFactory`**, which is itself outer to
`LenientTypeAdapterFactory`. Registration index runs opposite to outerness
(`00-verified-facts.md` §2.1), so outer-to-`Capture` means **registered after `Capture`**. It also
has to stay inner to `PostInitTypeAdapterFactory`, so a `postInit()` body still observes extracted
values - `SkyBlockMember`, `Bestiary` and four others depend on that today only because `@Extract`
sits far deeper than `PostInit`.

`GsonSettings.java`:248-257 becomes:

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
`SerializedPath`, `Split`. `Collapse` stays outer to `Capture` and `Lenient` stays inner to
`Capture`, so both dark-ordering pairs the test suite pins (`GsonFactoryTest`:1979-2166 and
`:2168-2253`) keep their relative order. The class javadoc at `GsonSettings.java`:215-218 lists the
factories and must gain the new one.

**What tree it sees on read: none.** This is the part worth dwelling on. `@Extract` never inspected
the tree on read - it inspected the overflow, which by construction is the part of the tree the
producer already removed. So `read` does not buffer the stream at all. `write` does buffer, once, and
for a reason that has nothing to do with claiming: the delegate's tree still carries the `@Extract`
field's own serialized key, which nothing removes today
(`00-verified-facts.md` §3.2 W1a, `10-design-entries.md` §2.3), so the adapter takes the tree back,
drops that key and writes it through a `jsonElementAdapter` the way the other tree-rewriting factories
do:

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

            if (extractValue == null || owner == null)
                continue;

            JsonElement overflow = Overflow.open(owner, info.getTarget(), info::newOverflow);

            if (!overflow.isJsonObject())
                continue;

            JsonObject overflowObject = overflow.getAsJsonObject();
            JsonElement tree = this.getGson().toJsonTree(extractValue);

            if (!info.isRemainder())
                overflowObject.add(info.getJsonKey(), tree);
            else if (tree.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : tree.getAsJsonObject().entrySet())
                    overflowObject.add(entry.getKey(), entry.getValue());
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
do and avoiding the `getDelegateAdapter` resolution hazard `00-verified-facts.md` A7 describes:

```java
@Override
public <T> @Nullable TypeAdapter<T> create(@NotNull Gson gson, @NotNull TypeToken<T> typeToken) {
    ConcurrentList<ExtractFieldInfo> extractFields = ExtractFieldInfo.of(typeToken.getRawType());
    return extractFields.isEmpty() ? null : new ExtractTypeAdapter<>(gson, gson.getDelegateAdapter(this, typeToken), extractFields);
}
```

`ExtractFieldInfo` moves out of `LenientTypeAdapterFactory`:456-496 and gains five resolved
properties. All five are resolved once per type at `create` time, from the same
`Reflection<>(clazz)` scan with `setProcessingSuperclass(false)` it already uses.

| Property | Resolved from |
| --- | --- |
| `sourceAccessor` | the `FieldAccessor` whose `getName()` equals `sourceFieldName` - **Java field name**, never `@SerializedName`, which `00-verified-facts.md` B3 shows four of six sites depend on |
| `serializedName` | the `@Extract` field's own `@SerializedName` value if it carries one, else its Java field name - the same resolution `LenientFieldInfo`:394 makes. This is the key `write` removes from the delegate's tree |
| `target` | `FIELD_ELEMENT` when the source field carries `@Lenient`, `SOURCE_OBJECT` when it carries `@Capture` |
| `remainder` / `pattern` | §3.3 |
| `newOverflow()` | `new JsonObject()` for a `Map`-assignable source, `new JsonArray()` otherwise - the same rule `LenientTypeAdapterFactory.java`:108 applies today through `lenientInfo.isMap()` |

### 2.7 Reflection-set-after-build - is it equivalent for the six existing sites

The assignment that seeded this entry asks me to confront a semantic change: "`@Extract` currently
mutates the tree BEFORE the delegate builds the object, whereas after the lift it must set fields on
an object that already exists". **Both halves of that premise are false, and the code says so
plainly.** Getting this right matters, because if it were true the lift would carry real risk, and
it does not.

**`@Extract` does not mutate the tree.** Read the filter phase. `LenientTypeAdapterFactory.java`:172-184
builds two fresh objects, `filtered` and `overflow`, from `original`. Only `filtered` goes back into
the tree, at `:183` through `replaceElement`. `overflow` is appended to a local list at `:184` and is
never attached to `rootObject`. The extract phase's `overflowObj.remove(...)` at `:216` therefore
removes from an object that is not in the tree. The tree the delegate reads at `:224` is byte-for-byte
identical whether or not any `@Extract` field exists.

**`@Extract` already sets fields on an object that already exists.** `:242-248` runs after
`:224`. `claim.info().getAccessor().set(value, extractValue)` at `:245` is a reflective set on the
object the delegate has already built and the reflective binder has already populated.
`00-verified-facts.md` B4 records the same thing.

So reflection-set-after-build is not a change at all - it is the current implementation, moved.
What actually moves is the **claim**, from before the delegate call to after it. Six things could
notice, and here is each one, checked:

| # | Could notice | Verdict |
| --- | --- | --- |
| 1 | The store's contents at end of read. Today `:216` removes, then `:239` publishes. After, the producer publishes, then the claim removes | **Identical final state.** `OVERFLOW.put(collection, fieldOverflow.overflow())` at `:239` stores the reference to the very `JsonObject` created at `:174`, not a copy. Removing from it before or after publication leaves the same object in the same state. The only difference is a window inside one `read` call during which the store holds the key, and nothing reads the store during a read |
| 2 | The value the field receives | **Identical.** `gson.fromJson(element, accessor.getGenericType())` against the same `Gson`, resolving from the top of the chain in both designs. `Loadouts`' two `Optional<Integer>` targets still reach `OptionalTypeAdapterFactory` the same way |
| 3 | A null bind | **Identical.** Today `:226-227` returns null and discards the claims; after, `delegateAdapter.read` returns null and the new factory returns before claiming. `00-verified-facts.md` E4 already prices this as a pre-existing fidelity hole and it does not widen |
| 4 | The binder overwriting the extracted value | **Identical, and still in `@Extract`'s favour.** The set happens after the binder in both designs. It now also happens after `CaptureTypeAdapter`'s `accessor.set` at `:381`, but that only writes `@Capture` fields, and `CaptureFieldInfo.of` (`:769-793`) requires `Map`-assignable and `ParameterizedType`, so it cannot target any of the six `@Extract` fields |
| 5 | `postInit()` seeing the extracted value | **Identical.** `PostInitTypeAdapterFactory` stays outermost, so `@Extract` still completes first. `Bestiary.postInit()` reads `kills` and `deaths`, not `lastKilledMob`, so it is indifferent either way - but the guarantee is preserved for the general case |
| 6 | `@Capture`'s post-`set` map instance (`00-verified-facts.md` D5) | **Changes, and this is the point.** Today `@Extract` runs before `:381`, so a `@Capture` field's live map does not exist when it looks. After the lift it does. This is the capability the entry exists to create, and no existing site can regress on it because no existing `@Extract` names a `@Capture` field |

Per-site, the six from `00-verified-facts.md` §10.1: `Bestiary.lastKilledMob` (`Optional<String>` from
`kills`), `Foraging` (`ConcurrentMap<String, Integer>` from `treeGifts`), the two `ChocolateFactory`
fields (`ConcurrentMap<String, Long>` and `ConcurrentMap<String, ConcurrentList<String>>` from
`rabbits`), and `Loadouts`' two `Optional<Integer>` from `armorSets` and `equipmentSets`. All six
are `@Lenient`-sourced, all six target a `JsonObject` overflow, and all six take the exact-key path
unchanged. Rows 1 through 5 above cover every one of them; row 6 touches none.

**Where the real risk is, and it is not here.** The lift's exposure is the write path (§2.8) and the
two `create`-time checks (§2.9), not the read-side assignment. A design that spent its caution budget
on set-after-build would be guarding the wrong thing.

### 2.8 The write path after the lift

This is the hard half, and it nearly sinks the design. Work the direction of travel first.

A `TypeAdapter.write` chain runs **outermost first on the way down and innermost first on the way
back**. `LenientTypeAdapter.write`:92 calls `delegateAdapter.toJsonTree(value)` and then
post-processes the returned tree at `:98-145`. So for the proposed chain the sequence is:

```
Extract.write        entered first
  Capture.write      entered second
    Lenient.write    entered third
      ... reflective binder produces the flat tree ...
    Lenient post-processes   <-- merge-back runs HERE, first
  Capture post-processes     <-- merge-back runs HERE, second
Extract post-processes       <-- would run LAST
```

Today `@Extract`'s re-injection at `:98-118` and `@Lenient`'s merge-back at `:121-145` are
consecutive loops in **one method**, so the ordering is free. After the lift it is not: if
`ExtractTypeAdapter` post-processed the tree the way every other factory does, both producers would
have merged back before it got a turn, and the re-injected entries would never reach the document.
An outermost factory cannot post-process its way out of this.

**The resolution: the half of `@Extract`'s write that has to happen early is not tree work, so it does
not have to wait for the tree.** Re-injection mutates the overflow container in the store, not the
output JSON. It can therefore run **before** the delegate call, which is exactly where the sketch in
§2.6 puts it. Both producers then read an already-restored container during their own merge-back and
put each entry in their own correct place - `@Lenient` into the field's sub-object through
`locateElement` (`:132`), `@Capture` into the root or the descend node (`:242-244`). The other half -
removing the `@Extract` field's own key - is tree work, and it wants the opposite end of the same call:
after every producer has merged. An outermost factory is the only position that offers both ends.

That is why per-entry target tagging is enough and no target *routing* is needed: `@Extract` never
decides where an entry lands. It hands the entry back to the producer that owned it, and the producer
already knows.

Traced end to end for `Loadouts`, whose two `Optional<Integer>` fields came out of two different
`@Lenient` maps:

```
Extract.write(loadouts)
  equippedArmorSet = Optional.of(2)      -> owner = the live armorSets map
     Overflow.open(armorSets, FIELD_ELEMENT, JsonObject::new)
     overflow.add("equipped_set", 2)
  equippedEquipmentSet likewise into the equipmentSets overflow
  delegate.write ->
    Capture.write   - Loadouts has no @Capture field, so CaptureTypeAdapterFactory.create
                      returned null and this frame does not exist
    Lenient.write
      :92  toJsonTree -> {"armor": {...}, "equipment": {...}, "loadouts": {...}}
      :121 merge-back: Overflow.find(armorSets, FIELD_ELEMENT) now carries "equipped_set"
      :132 locateElement -> the "armor" sub-object
      :137 armor["equipped_set"] = 2
  tree returned = {"armor": {..., "equipped_set": 2}, "equipment": {...}, "loadouts": {...},
                   "equippedArmorSet": 2, "equippedEquipmentSet": 4}
  remove "equippedArmorSet", remove "equippedEquipmentSet"
```

Today's output minus two root keys the input never carried - the W1a duplicates, which nothing removes
today. Everything the input carried is byte-identical and in the same place. The same trace with a
`@Capture`-sourced claim differs only in
which producer picks the entry up: `Overflow.find(capturedMap, SOURCE_OBJECT)` at
`CaptureTypeAdapterFactory.java`:239, merged into the root at `:247-248` under the original
unstripped key, and `literalPrefix` is not applied to overflow keys - which is correct, because
overflow keys were stored unstripped in the first place (`00-verified-facts.md` D3).

Four write-path details that are easy to get wrong:

- **`Overflow.open` must record a target, and the first publisher wins.** On a read-then-write the
  producer published first and its target stands. On a write of a hand-built object that was never
  read, `@Extract` publishes first and supplies the target from the source field's annotation. Both
  give the producer the target it expects, because both derive it from the same annotation.
- **`@Capture` publishes only when non-empty (`:386`).** A hand-built object, or one read from a
  document with no `@Capture` overflow, has no store entry, so `open` has to create one. It does.
  The entry then reaches `:239` on the same write and merges into the root.
- **Re-injection is idempotent across repeated writes.** `JsonObject.add` replaces an existing key,
  so writing the same object twice does not duplicate. Today's `:111` relies on the same property.
- **Fix the dead-`JsonArray` path while moving it.** `:108` calls `computeIfAbsent` and installs a
  `JsonArray` for a collection-shaped source, then `:110`'s `isJsonObject()` guard drops the value on
  the floor - the entry is created and never used, and the empty array is left in the static store as
  a side effect (`00-verified-facts.md` W3a, E5). The moved loop should guard **before** `open`
  rather than after it, `if (!info.isMapSource()) continue;`, so no phantom entry is installed. No
  site exercises this today; it is free to fix while the code is in hand and it shrinks the store.

### 2.9 Failure modes

| # | Condition | Today | Proposed | Risk |
| --- | --- | --- | --- | --- |
| 1 | `@Extract` names a field that does not exist on the class | silent no-op - `:206-212` finds no `FieldOverflow` and `continue`s | `JsonException` from `create`, once per type | **The single biggest downstream break.** It is loud where it was silent, and `create` failures surface at first use of the type, or are swallowed per-type by `GsonSettings.prewarm` (`:197-200`). All six existing sites name a field on their own class, so the module is safe - a downstream module with a typo is not |
| 2 | `@Extract` names a field on a **superclass** | silent no-op - `setProcessingSuperclass(false)` (`:481`) hides it | same `JsonException` as #1 | Same shape as #1 and easier to hit by accident, because inheritance makes the field look present. Nothing in this module inherits an `@Extract` source |
| 3 | `@Extract` names a field carrying neither `@Lenient` nor `@Capture` | silent no-op | `JsonException` from `create` - there is no target to derive | Correct to reject: nothing would ever publish an overflow for that field |
| 4 | The source is a `@Capture` field in grouping mode | n/a - unreachable today | **silent permanent no-op** | Grouping mode skips the compatibility check entirely (`:332-334`, `:355`) so it produces **no overflow ever** (`00-verified-facts.md` D6). Six of seventeen `@Capture` sites are grouping mode. This is not statically checked - doing so would duplicate `CaptureFieldInfo`'s inference in a second place - so it is documented on the annotation instead |
| 5 | Conversion of a claim fails | entry removed at `:216`, failure swallowed at `:246-247`, entry lost from the object **and** from the document | entry restored to the overflow, field stays at its initialiser | Strictly better. `00-verified-facts.md` B7 is preserved: no new exception reaches a consumer, and the six sites keep their `@NotNull` initialisers |
| 6 | Two threads serialize the **same** object concurrently | both mutate the same overflow `JsonObject` through `:108`/`:111`; `LinkedTreeMap` is not thread-safe | unchanged | Pre-existing. `WeakIdentityMap` is concurrency-safe, the `JsonElement` it stores is not, and neither design changes that |
| 7 | A downstream SPI or `GsonContributor` factory produces overflow | n/a | unreachable by `@Extract` | `GsonSettings.java`:259-263 appends SPI factories **after** the built-ins, so they nest outside `ExtractTypeAdapterFactory`. The new ordering guarantee is not enforceable against them (`00-verified-facts.md` F3) and the entry does not claim otherwise |
| 8 | Store growth and sweep cost | two maps, bare `JsonElement` values | one map, one `Entry` record per value | One extra small allocation per stored overflow, and `sweep()`'s full scan (`WeakIdentityMap.java`:96-104) now runs over one larger key set rather than two smaller ones. Net a wash; call it slightly worse and bounded by the same object lifetimes |
| 9 | `sourceAccessor.get(value)` throws | n/a | escapes the read | Parity, not a new hole: `LenientTypeAdapterFactory.java`:231 already makes an unguarded reflective `get` on the post-bind path |
| 10 | A `@Capture` field's overflow is empty | published only when non-empty (`:386`), so no entry | unchanged | A claim finds nothing, which is correct. The write path's `open` creates a container if a re-injection needs one |

Failure modes 1, 2 and 3 are one decision: **fail at adapter-build time on a misspelled or
unannotated source**. I recommend taking it, because a claim that can never match is a silent data
loss that no test catches, and `create` runs once per type rather than per document so the check is
free at runtime. But it is the one place this entry deliberately trades a silent no-op for an
exception, and if the owner wants a smaller blast radius the fallback is to keep the `continue` and
emit the diagnostic through whatever channel `dgx-capture-unmatched` settles on. That choice does not
change any other part of the design.

### 2.10 The cheaper alternative

Two real ones. Both are named because pretending otherwise would make this entry look better than it
is.

**Alternative A - keep `@Extract` where it is and publish a read-scoped frame.** This is the shape
`00-verified-facts.md` C3 points at. `CaptureTypeAdapter.read` pushes `overflowMaps` onto a
`ThreadLocal` before `:366` and pops it in a `finally`; `LenientTypeAdapter`'s extract phase consults
the pushed frame as well as its own local list. It works on the read path, and - checked, because it
is not obvious - it also works on the write path, since `Lenient.write` post-processes **before**
`Capture.write` does, so a shared, tagged store would receive the re-injection in time for
`:239` to see it.

It loses on three counts, none of which is "it cannot be made to work":

- The frame is ambient state with a lifetime, and it needs an identity tag. `@Extract` and its source
  must be on the **same class**, but a nested POJO's `LenientTypeAdapter` would see the enclosing
  class's pushed frame and could match a same-named field on it. Guarding that means tagging frames
  with the adapter or raw type and comparing, which is a correctness argument no test naturally
  reaches.
- It keeps the extract phase inside the library's second-most-used factory, keeps the dead tree
  round-trip for `@Extract`-only classes, and keeps one of the five silent swallows where it is.
- It does not reduce the blast radius: it still edits both `LenientTypeAdapterFactory` and
  `CaptureTypeAdapterFactory`, and it still needs the shared tagged store for the write path.

The lift trades a **lifetime** problem for a **lookup** problem, and a lookup off a built object is
directly testable. That is the whole of the argument for it.

**Alternative B - do not build this at all, and take the two lossy cheap routes the research pack
already accepted.** `d10-fallback` closes all six unmatched-enum-key sites with one line per enum and
no change to `CaptureTypeAdapterFactory`. A plain `@Lenient ConcurrentMap<String, Integer>` closes
half of `f03-questrewards-mixed-values` with no library change whatsoever.

State the trade honestly: **this entry is not the cheapest way to close either symptom. It is the
way to close them without losing data.** `@Fallback` collapses every unmatched key onto one enum
constant and later entries overwrite earlier ones, which is the same N-1 loss the defect already has,
merely made visible and typed. The `@Lenient` partial parks the string half of `quest_rewards` in an
overflow no code can read. Both are cheap and both are lossy. The store plus the lift is neither
cheap nor lossy. If the owner's priority were minimum cost this entry would decline; the standing
decision to do the library work first is what makes it adopt.

### 2.11 Verdict and effort

**Adopt.** `large`.

The effort rating comes from the clause, not the file count. `00-conventions.md` §4 puts a change at
`large` when it introduces "a new ordering guarantee between factories, or a new lifecycle hook the
whole pipeline must honor", and this does exactly one of those: `ExtractTypeAdapterFactory` **must**
nest outside `CaptureTypeAdapterFactory` or it silently reverts to the current capability. By file
count alone it would read `medium` - two new library files (`Overflow`, `ExtractTypeAdapterFactory`),
three edited (`LenientTypeAdapterFactory`, `CaptureTypeAdapterFactory`, `GsonSettings`), two new test
classes, zero consumer files. Take the higher rating: the ordering guarantee is the part that breaks
quietly if a future factory is registered in the wrong place, and `00-conventions.md` §4 says to give
the higher level when uncertain.

One JitPack cycle, shared with `dgx-extract-filter` if the two ship together - which they should,
because the store on its own has zero adoption sites and is unverifiable end to end without the
filter to exercise it.

## 3. dgx-extract-filter - `@Extract` gains a filter element

- **Registry entry:** none - the "extend an existing annotation" route `00-conventions.md` §6.1 asks
  proposals to name explicitly
- **Verdict:** adopt narrowly - selection only, no key stripping, no array-shaped remainder (§3.4)
- **Category:** `correctness`
- **Answers findings:** `f03-questrewards-mixed-values`; unlocks `f06-capture-null-enum-key` for
  `dgx-capture-unmatched`, which cannot use overflow without it
- **Cheaper alternative:** `@Lenient` alone, typing half the map and leaving the rest unreadable -
  the route the research pack accepted, and it is lossy
- **Library change:** existing factory edit, plus one element on an existing annotation
- **Adoption sites today:** 1, plus 6 conditional on `dgx-capture-unmatched`
- **Effort:** `medium`

### 3.1 The one site today, and the six the sibling entry unlocks

**The site.** `response/skyblock/member/crimson/CrimsonIsle.java`:65-66, `Quests.questRewards`:

```java
@SerializedName("quest_rewards")
private @NotNull ConcurrentMap<String, Object> questRewards = Concurrent.newMap();
```

One JSON object carrying two unrelated maps interleaved by value type - `<itemId> -> <count>` with
integer values and `<questId> -> <itemId>` with string values. `@Lenient` can divert one value type
to overflow, but `@Extract` addresses a single named key, so there is no way to pull "every
string-valued entry" back out, and two fields cannot both claim `quest_rewards` because gson rejects
duplicate serialized names. That is the whole of `d10-lenient-overflow`'s problem statement and it is
unchanged.

**The six.** `Dojo.java`:15-16 and `:17-18`, `Kuudra.java`:18-19 and `:20-21`,
`TrophyFishing.java`:24-25, `HeartOfTheMountain.java`:49-50 - six `@Capture` maps that narrow an open
key space onto a closed enum with no failure policy. `00-verified-facts.md` C6 corrects the pack's
count from seven to six and removes `Statistics.java`:89, which is not a `@Capture` field at all.

Those six are **not** this entry's adoption sites and it must not claim them. `dgx-capture-unmatched`
owns them. What this entry supplies is the half they are missing: `10-annotation-designs.md` §9.4
names diverting unmatched keys to `@Capture`'s existing overflow as the only option that keeps the
data **and** keeps round-trip fidelity, and then declines it because "`@Extract` addresses a single
named key, not every entry that failed key conversion". This element is that missing axis. Whether
the sibling entry takes it is the sibling's call.

Note the trap before anyone counts higher: two of those six, `TrophyFishing.fish` and
`HeartOfTheMountain.powder`, are **grouping mode**, and grouping mode produces no overflow at all
(`00-verified-facts.md` D6). Reaching them needs more than a filter on `@Extract`.

### 3.2 Full declaration of `@Extract` after the change

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
 * {@link Capture @Capture} - both park the entries they could not take in the same place.
 * <p>
 * Two modes, selected by whether {@link #value()} carries a dot:
 * <ul>
 *     <li><b>Single key</b> ({@code "sourceField.jsonKey"}) - claims the one entry stored under
 *         {@code jsonKey} and reads it as this field's type. {@link #filter()} must be empty.</li>
 *     <li><b>Remainder</b> ({@code "sourceField"}) - claims every entry {@link #filter()} accepts
 *         and reads them together, so the field is a map. An empty filter, the default, claims the
 *         whole remaining overflow.</li>
 * </ul>
 * <p>
 * A claimed entry leaves the source's overflow and this field's value is put back into it on
 * serialize, so the document round-trips in either mode. Keys are claimed and restored exactly as
 * the source stored them - nothing is stripped, so a {@code @Capture} source yields whatever
 * prefix its own filter matched.
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
(`00-verified-facts.md` F4), so no existing `@Extract` site recompiles differently. The class javadoc
is rewritten because the old one asserts the source is a `@Lenient` field and names
`LenientTypeAdapterFactory` as the implementor; both stop being true.

### 3.3 Mode selection - the dot in `value()`, not the presence of `filter()`

Three ways to combine the two elements were considered. The choice matters because
`00-conventions.md` §4 rates "changes the meaning of an existing annotation for existing users" as
`xlarge`, and only one of the three avoids that.

| Option | Rule | Verdict |
| --- | --- | --- |
| Exclusive on `filter()` | `filter()` empty means today's exact-key mode; non-empty means remainder | **Rejected.** It makes "empty filter" mean *exact key*, which is the opposite of `@Capture`, where an empty filter is the catch-all. Two annotations, one word, inverted meanings |
| `value()` reinterpreted as a key regex | drop the source-plus-key path, name the source elsewhere | **Rejected.** `xlarge` by definition - all six existing sites change meaning |
| **The dot in `value()` selects the mode** | dotted value means single key, `filter()` must be empty; dotless value means remainder, and inside remainder mode an empty `filter()` is the catch-all | **Chosen** |

The chosen rule reads off one line of existing code. `ExtractFieldInfo`'s constructor
(`LenientTypeAdapterFactory.java`:468-476) already branches on `path.indexOf('.')`, and the
**else** branch is dead:

```java
} else {
    this.sourceFieldName = path;
    this.jsonKey = "";
}
```

A dotless `@Extract("kills")` claims the literal empty key, which no document carries, so the field
silently keeps its initialiser (`00-verified-facts.md` B6). Zero sites use it. That makes the dotless
form a **free semantic slot**: giving it a meaning changes the behaviour of no site, in this module
or downstream, that was doing anything at all.

So `filter()` mirrors `@Capture.filter()` exactly where it counts. Inside remainder mode, empty is
catch-all and a regex selects by `Pattern.matcher(key).find()` - the same `find()` semantics
`CaptureTypeAdapterFactory.java`:327 uses, not `matches()`. What it does **not** mirror is stripping,
which is §3.4.

One cost, stated rather than buried: a typo. `@Extract("kills")` written when
`@Extract("kills.last_killed_mob")` was meant used to do nothing; it now claims the entire overflow
and tries to read it as the field's type. The conversion will almost always fail, and §2.9 #5 restores
the entries rather than losing them, so the failure is silent and non-destructive - the same outcome
as today, reached by a different route. A dotless `@Extract` on a non-map field is a good candidate
for the `create`-time check in §2.9.

### 3.4 Selection without stripping, and why that diverges from `@Capture`

`@Capture` strips: `key.replaceFirst(info.getFilter(), "")` at `CaptureTypeAdapterFactory.java`:330,
and the write path reconstructs the original by prepending `literalPrefix` at `:218-220` and
`:228-230`. `@Extract`'s filter does **not** strip. Three reasons, in descending order of weight.

1. **Stripping would put a second reconstruction on the write path, and the first one is already
   fragile.** `literalPrefix` is the filter with a leading `^` and trailing `$` removed
   (`:654`) - a literal, not a regex inverse. It round-trips only because all eleven `@Capture`
   filters in the module are a literal plus `^` (`00-verified-facts.md` D4). Duplicating that
   fragility in a second annotation doubles the number of places a regex with real metacharacters
   silently fails to round-trip.
2. **A `@Capture`-sourced claim would double-prefix.** `@Capture` stores overflow under the
   **original unstripped** key (`:338`, `:358`) precisely so its merge-back at `:247-248` can copy it
   back verbatim, bypassing `literalPrefix`. If `@Extract` stripped on claim, restoring would have to
   re-apply a prefix that the overflow path deliberately never removed. That is a live double-prefix
   bug, not a hypothetical one, and not stripping removes it by construction.
3. **The one real site wants the keys.** `quest_rewards`' string half maps
   `crimson_isle_kill_barbarian_duke_x_c -> KADA_LEAD`. The quest id **is** the key; stripping
   `^crimson_isle_` off it would produce `kill_barbarian_duke_x_c`, which is not what the upstream
   API calls that quest anywhere else.

This is the "narrowly" in the verdict. If a site later wants stripped keys it can be added as a
separate `boolean strip() default false`, and at that point it inherits `literalPrefix`'s limits
knowingly rather than by accident.

The second cut is **remainder mode over an array-shaped overflow**. A collection-shaped `@Lenient`
field produces a `JsonArray` overflow (`:188`, `:198`), which has no key space for a filter to match.
Claiming the whole array into a typed list is a coherent feature and it has zero sites -
`Dungeons.unlockedJournals` is the module's only collection-shaped `@Lenient` field and carries no
`@Extract`. So `Overflow.claim` returns nothing for an array-shaped entry in either overload, which
preserves today's silent no-op exactly (`00-verified-facts.md` §3.1 R5b) and keeps the new surface to
one shape.

### 3.5 How the factory implements remainder mode

Two `ExtractFieldInfo` properties carry it, both resolved once per type at `create`:

```java
private final boolean remainder;
private final @Nullable Pattern pattern;

boolean matches(@NotNull String key) {
    return this.pattern == null || this.pattern.matcher(key).find();
}
```

`remainder` is `path.indexOf('.') < 1`; `pattern` is `Pattern.compile(filter)` when the filter is
non-empty and `null` otherwise, mirroring `CaptureFieldInfo.java`:653. `matches` returning `true` on
a null pattern is what makes an empty filter the catch-all, and it is the whole of the difference
between the two remainder cases.

The read path in §2.6 then reduces to the one branch already shown - `Overflow.claim(owner,
info::matches)` returns a `JsonObject` of the selected entries under their original keys, and
`gson.fromJson(claimed, accessor.getGenericType())` reads it as the declared map type. That is the
same conversion call the single-key path makes; only the element handed to it differs.

Two consequences fall out of using `fromJson` on the assembled object rather than converting
entry-by-entry:

- **Key conversion is gson's, not the factory's.** A `ConcurrentMap<String, String>` target takes
  every key; a `ConcurrentMap<SomeEnum, String>` target hits the same unmatched-enum-key behaviour
  `dgx-capture-unmatched` is about, one level up. Remainder mode should be documented as
  string-keyed in practice until that entry lands.
- **The conversion is all-or-nothing.** One unconvertible value fails the whole map and §2.9 #5
  restores every claimed entry, so the field keeps its initialiser and the document is intact. That
  is coarser than `buildSimpleMap`'s per-entry catch (`:401-402`) and it is the right coarseness
  here: a partial map with no signal is worse than an empty one, because the empty one is visible.

### 3.6 Claim ordering and overlapping filters

Claiming is destructive, so order is semantics. `@Capture` resolves the same problem by trying
filtered fields in declaration order with first-match-wins (`:323-343`, break at `:342`) and only
then falling through to the catch-all (`:349-358`). That works but it makes reordering two fields in
a DTO a behaviour change (`00-verified-facts.md` D1), and it rests on `Class.getDeclaredFields`
returning declaration order, which the JVM specification does not promise.

`@Extract` should not inherit that. Sort the resolved fields into three bands in `ExtractFieldInfo.of`
and the common compositions become order-independent:

| Band | Shape | Rationale |
| --- | --- | --- |
| 0 | single key | The most specific claim there is. Running it first lets an exact `@Extract` and a catch-all remainder coexist on one source, with the named key landing in its own typed field and everything else in the map |
| 1 | filtered remainder | Mirrors `@Capture`'s "filtered first" |
| 2 | catch-all remainder | Mirrors `@Capture`'s catch-all fallthrough, and there is at most one useful one per source |

```java
result.sort(Comparator.comparingInt(ExtractFieldInfo::getClaimOrder));
```

`ConcurrentList` inherits `List.sort`; `CaptureFieldInfo.discoverGroupAffixes` already uses it at
`:754-755`.

What that leaves genuinely ambiguous is **two filtered remainders on the same source whose regexes
overlap**. The first in scan order claims the intersection and the second sees only what is left, and
scan order is the same field order `@Capture` already depends on. This is not fixed - it is declared:

- Filters on one source should be disjoint. Overlapping filters are order-dependent and the order is
  not specified.
- It is not detectable statically. Deciding whether two regexes intersect is not something a
  `create`-time check can do, and an approximation that rejects safe pairs would be worse than the
  documented hazard.
- Two catch-alls on one source is detectable, and is the same silent-second-one-does-nothing shape as
  `@Capture`'s D2. Reject it at `create` time along with the checks in §2.9.

### 3.7 Failure modes and round-trip fidelity

| # | Condition | Behaviour | Note |
| --- | --- | --- | --- |
| 1 | `filter` set on a dotted `value` | `JsonException` from `create` | A new combination that cannot exist in any current source, so rejecting it regresses nothing |
| 2 | Remainder target is not a map | conversion fails, every claimed entry restored, field keeps its initialiser | Silent. A `create`-time check on the declared type is possible and cheap; recommended alongside §2.9's |
| 3 | Two catch-all remainders on one source | `JsonException` from `create` | See §3.6 |
| 4 | Two overlapping filtered remainders | first in scan order wins the intersection | Declared, not fixed. See §3.6 |
| 5 | Source is a grouping-mode `@Capture` field | claims nothing, forever | `00-verified-facts.md` D6. Two of the six enum sites, `TrophyFishing.fish` and `HeartOfTheMountain.powder`, are exactly this |
| 6 | Enum-keyed remainder target | keys that fail enum conversion collapse onto `null` | The same defect one level up. Remainder mode is string-keyed in practice until `dgx-capture-unmatched` lands |
| 7 | Remainder claimed, then serialized | every entry of the field's map is re-injected under its own key, and the field's own root-level key is removed | Exact round-trip on **two** conditions. Keys must serialize back to what they were - a `String` key does, an enum key writes its `@SerializedName` or constant name (`CaptureTypeAdapterFactory.java`:597-606 is the precedent), which need not equal the original document key, another reason for #6. And the §2.6 key removal must be in the same sha, or the remainder is emitted whole at the root **as well as** merged back and N keys are duplicated (`00-verified-facts.md` W1a) |
| 8 | Caller mutates the extracted map after the read | the mutation is serialized | Consistent with `@Lenient`, whose identity keying exists precisely so a caller can mutate a bound container and keep its overflow (`WeakIdentityMap.java`:16-19). Treat as intended |
| 9 | Read with one `Gson`, written with another | works - the store is `static` | Unchanged from today, and unchanged by merging the two stores (`00-verified-facts.md` E1) |

Round-trip in one line: a remainder claim removes N entries from the overflow on read and adds N
entries back on write, under the same keys, into the same container, which the producer then merges
into the same place it would have merged them from - and the field itself is then removed from the
root so those N entries appear once, not twice. The only lossy paths are #5, #6 and #7, and all three
are properties of the source or the target type rather than of the mechanism.

### 3.8 The cheaper alternative

At one site, two things beat this element on cost.

**`@Lenient` alone.** `10-annotation-designs.md` §11.2's position: declare `questRewards` as
`@Lenient ConcurrentMap<String, Integer>` and the integer half is typed correctly today, at zero
library cost, with the string half parked in an overflow that round-trips but that nothing can read.
Half the data, none of the cycle.

**A field-scoped `@JsonAdapter`.** The stock-first rule in `10-annotation-designs.md` §2 records that
gson honours `@JsonAdapter` on a **field**, not only on a type, so a ~25-line `TypeAdapter` in the
consuming module could read `quest_rewards` into a two-map holder and write it back, with no library
change and no registration in `GsonSettings`. For one site that is cheaper than a JitPack cycle,
and it is the honest comparison this entry has to survive rather than one it gets to skip.

It survives on three grounds, none of which is "the annotation is nicer":

1. **It is the only lossless route to the six enum sites.** `@Fallback` types them and collapses
   N unmatched keys onto one constant. `skipUnmatchedKeys` drops them. Overflow plus a filtered
   `@Extract` keeps every one of them, in the object and in the document.
2. **The marginal cost is small.** `dgx-overflow-store` is being built by owner decision. On top of
   an `ExtractTypeAdapterFactory` that already exists, this element is one annotation field, one
   `Pattern`, one `boolean`, one sort, and one extra branch in each of `read` and `write`.
3. **A hand-rolled `TypeAdapter` per site is the residue the pack exists to remove.** One is cheaper
   than a cycle; the fourth one is not.

If the owner had not already decided to do the library work first, this entry would read **decline -
take the `@Lenient` partial**, exactly as `d10-lenient-overflow` did. The decision changed the
denominator, not the evidence, and this entry says so rather than reverse-engineering a stronger case
than exists.

### 3.9 Verdict and effort

**Adopt narrowly.** `medium`.

Narrow in two named ways: selection only, with no key stripping (§3.4), and object-shaped overflow
only, with no array-shaped remainder (§3.4). Both cuts are reversible additions later; neither is a
semantic they would have to break.

`medium` is what `00-conventions.md` §4 prices "adds an element to an existing annotation" at. It
rides `dgx-overflow-store`'s JitPack cycle, so it costs no second re-pin, and it must ship in the
same cycle - the store has no adoption sites of its own and this element is what makes it verifiable
end to end.

## 4. Regression checklist for the six existing `@Extract` sites

**Start here, because it changes how the rest of this list should be read.** `@Extract` appears in
exactly one test file in the whole library - `GsonFactoryTest.java`, at the import on `:14`, the
`FullCombinationModel` at `:2214-2224` and `lenientExtractCapture_ok` at `:2227-2253` - and that test
only deserializes. There is no `LenientTests` nested class; the nested classes are `SerializedPath`,
`OptionalTypeAdapter`, `Capture`, `Collapse`, `Split`, `PostInit`, `Combination` and `HtmlEscaping`.
On the consumer side, `MemberDtoMappingTest` is the module's only test class, all 16 of its tests
decode, and none calls `toJson`.

So **the `@Extract` write path - the re-injection at `LenientTypeAdapterFactory.java`:98-118 and its
interaction with the merge-back at `:121-145` - has zero coverage in either module today.** That is
precisely the code §2.8 relocates and re-orders. The 134/134 and 16/16 baselines will both stay green
through a write-path regression. Any implementation of this entry that does not add serialize tests
**first** is flying blind, and that ordering is not negotiable.

**Step 1 - characterisation tests against the current sha, before any edit.** Written against
`7cfc181`, they must pass unchanged afterwards.

| # | Test | Asserts |
| --- | --- | --- |
| 1 | Round-trip `FullCombinationModel` | `last_killed_mob` reappears inside the `kills` object, not at the root, and appears exactly once |
| 2 | Round-trip a two-`@Extract` model shaped like `Loadouts` | each claim returns to its own source's sub-object |
| 3 | Round-trip a `@SerializedPath`-located `@Lenient` source | merge-back still resolves through `locateElement`'s segment branch (`:340-350`), the branch only `Statistics.spawnedSpookyBats` and `Dungeons.unlockedJournals` drive |
| 4 | Round-trip a collection-shaped `@Lenient` field | the `JsonArray` half of the factory (`:139-144`), single-site coverage today |
| 5 | Serialize a hand-built object that was never read | `computeIfAbsent`/`open` creates the container and the entry still reaches the document |
| 6 | Read, mutate the extracted value, serialize | the mutation reaches the document (`00-verified-facts.md` E2) |

**Step 2 - the six consumer sites**, `00-verified-facts.md` §10.1. Decode-and-assert exists for four
of them; the round-trip assertion does not exist for any.

| Site | Source | Target | What to pin |
| --- | --- | --- | --- |
| `Bestiary.lastKilledMob` `:33` | `kills` `Map<String, Integer>` | `Optional<String>` | Value binds; `postInit()` still computes `families` from `kills`/`deaths` with the claimed key absent |
| `Foraging` `:30-31` | `treeGifts` `Map<String, Integer>`, `@SerializedName("tree_gifts")` | `ConcurrentMap<String, Integer>` | A **container** target, not a scalar - the `gson.fromJson(element, genericType)` path must still produce a map |
| `ChocolateFactory` `:44-45` | `rabbits` | `ConcurrentMap<String, Long>` | Container target, `Long` values |
| `ChocolateFactory` `:46-47` | `rabbits` | `ConcurrentMap<String, ConcurrentList<String>>` | Nested container; **two `@Extract` fields on one source**, so both claims must survive each other |
| `Loadouts` `:23-24` | `armorSets` `Map<Integer, ArmorSet>` | `Optional<Integer>` | Reaches overflow because the **key** fails `Integer` conversion, not the value. Binds through `OptionalTypeAdapterFactory` from the top of the chain |
| `Loadouts` `:29-30` | `equipmentSets` `Map<Integer, EquipmentSet>` | `Optional<Integer>` | As above, plus `Loadouts` carries a third `@Lenient` field with no `@Extract`, which must keep its overflow untouched |

**Step 3 - library test anchors to re-run in full**, not selectively. Inserting a factory shifts every
later index in `GsonSettings.defaults()`.

- `GsonFactoryTest.CombinationTests` (`:1879-2255`) entire - it is the only place in the library that
  observes factory nesting at all. Specifically `lenientWithCapture_ok` (`:2183`),
  `lenientExtractCapture_ok` (`:2227`), the `@Collapse` + `@Capture` pair (`:1979-2070`) and the
  `@Collapse` + `@Capture(descend)` pair (`:2071-2166`).
- `GsonFactoryTest.CaptureTests` (`:443-1216`) - eighteen models, and `CaptureTypeAdapterFactory` is
  edited at three lines.
- `CaptureGroupingModeTest` and `CollectionValueCompatibilityTest` - the `c944987` and
  `b071689`/`7cfc181` regression sets, the newest and least settled behaviour.
- `WeakIdentityMapTest` - the class is not modified, but `Overflow` becomes its only production
  caller and a `V` that is now a record rather than a `JsonElement`.
- `GsonFactoryTest.PostInitTests`, including `FailingPostInitModel` (`:1766`). Nothing here changes
  `PostInitTypeAdapterFactory`, but that test pins the `:37` swallow as intended behaviour and the
  nesting depth `PostInit` sits at is being recomputed.
- `GsonSettingsPrewarmTest` - `prewarm` swallows `Throwable` per type (`GsonSettings.java`:197-200),
  which is the path a `create`-time `JsonException` from §2.9 would take.

**Step 4 - new tests the change owes.** `OverflowTest` (publish/find/open/claim/restore, both
targets, target mismatch returns `null`, claim on an array-shaped entry returns nothing); an
`ExtractTests` nested class covering both modes, both source annotations, the three claim bands, and
each `create`-time rejection; and a `@Capture`-sourced round-trip proving the entry returns to the
**root** rather than to a field sub-object, which is the single assertion that proves the per-entry
target tagging works.

**Baseline to hold:** gson-extras 134/134 to 134 plus the new count; hypixel 16/16.

## 5. Risks I would not argue away

Ranked by how likely they are to be discovered late.

1. **The write path is being re-ordered with no test coverage.** §4 is the mitigation and it is the
   only one. The relocation in §2.8 is sound on paper - re-injection is store work, so it can run
   before the delegate - but "sound on paper, zero tests, and both baselines stay green either way"
   is the exact profile of a change that ships broken. This is the top risk in the entry and it is
   not reducible by better design, only by writing tests against the current sha first.
2. **A new ordering guarantee that nothing enforces.** `ExtractTypeAdapterFactory` must nest outside
   `CaptureTypeAdapterFactory`. Nothing in the code says so. A future factory registered between them
   in `GsonSettings.defaults()`, or a downstream SPI factory (`00-verified-facts.md` F3), silently
   reduces `@Extract` to its current capability with no test failure - the six existing sites are all
   `@Lenient`-sourced and would keep passing. A test that asserts the resolved chain order, rather
   than a comment, is the cheapest guard.
3. **Turning three silent no-ops into `create`-time exceptions** (§2.9 rows 1-3). It is the right
   call for this module, where all six sites are clean. It is a hard break for any downstream module
   with a misspelled or inherited `@Extract` source that has been quietly doing nothing for however
   long. Sibling modules share the pin (`00-verified-facts.md` F2), so they get the change whether or
   not they wanted it.
4. **The store merge is a lifetime change nobody asked for.** Two `static` maps become one. Nothing
   in the module reads with one `Gson` and writes with another (E1), and nothing will notice - but
   the entry is spending a shared-state change to buy a lookup convenience, and if the `Overflow`
   type ends up with no second consumer beyond `@Extract`, keeping two stores and having `@Extract`
   consult both would have been smaller. I do not think that is the right call, because the target
   tag has to live somewhere and a per-store implicit tag cannot express "this entry came from
   somewhere else", but it is a defensible position and it should be argued down rather than skipped.
5. **`@Extract` on a grouping-mode `@Capture` source is a permanent silent no-op** (§2.9 row 4,
   §3.7 row 5) and it is not statically checked. Six of seventeen `@Capture` sites are grouping mode,
   including two of the six enum-key sites this work is partly aimed at. Somebody will annotate one
   and get nothing.
6. **`large` may still be optimistic.** The estimate assumes `LenientTypeAdapterFactory` loses its
   extract phase cleanly. It has ninety lines coming out across five separate regions of one file,
   and the file is the second-most-used in the library. `00-conventions.md` §4 says to give the
   higher level when uncertain; the thing that would resolve this is doing step 1 of §4 and seeing
   whether the characterisation tests are writable in an hour or a day.

## 6. What this entry hands to the sibling entries

Stated as contracts, not suggestions, so the siblings can rely on them without reading this file end
to end.

- **`Overflow` exists, is package-private in `dev.simplified.gson.factory`, and tags every entry with
  a write target.** Any factory in that package can publish into it and any can read from it.
- **`@Extract` can claim from a `@Capture` overflow**, by naming the `@Capture` field's Java name in
  `value()`, and can claim a set rather than a single key. This is what makes
  `10-annotation-designs.md` §9.4's "third option nobody proposed" - divert unmatched keys to the
  overflow that already exists - buildable. `dgx-capture-unmatched` decides whether to take it; this
  entry only removes the reason it was declined.
- **Diverting an unmatched key to overflow costs one branch, not a redesign.**
  `CaptureTypeAdapterFactory.java`:490-495 already resolves each key through the gson adapter for the
  declared key type; the enum case succeeds with `null` rather than throwing
  (`00-verified-facts.md` §10.3). A `null` result routed to `overflowMaps` under the original key,
  the same way `:338` and `:358` already route incompatible entries, is the whole change.
- **Nothing here fixes grouping mode.** Grouping-mode `@Capture` fields produce no overflow at any
  point (`:332-334`, `:355`), so `TrophyFishing.fish` and `HeartOfTheMountain.powder` are out of
  reach of an overflow-based fix. Whatever `dgx-capture-unmatched` proposes has to say what happens
  to those two.
- **One silent swallow leaves the library.** `LenientTypeAdapterFactory.java`:246-247 is deleted; its
  replacement restores the claim instead of dropping it (§2.4). Four of the five remain
  (`00-verified-facts.md` §9) and this entry claims none of them.
- **Factory count and registration order change.** `GsonSettings.defaults()` goes from eight
  factories to nine and every index after `Capture` shifts. Any sibling that states an index must
  state the nesting depth it wants and derive the index from it.

## 7. Deferred - explicitly not designed here

**`@Owner` / `@Parent` reach-back.** Out of scope by the owner's decision, deferred until after the
research pack lands. Named here only so that a reader does not mistake `Overflow`'s per-read
publication for the beginnings of a parent-context channel - it is not one. `Overflow` is keyed by a
bound container's identity and carries JSON, not object references, and it is deliberately reachable
only from inside `dev.simplified.gson.factory`. Nothing in this entry makes reach-back cheaper or
harder.

Four smaller things are deferred with reasons, so they are not mistaken for oversights.

- **`boolean strip()` on `@Extract`.** §3.4. Additive later, inherits `literalPrefix`'s limits when
  it arrives, has no site now.
- **Array-shaped remainder mode.** §3.4. Zero sites; the module's only collection-shaped `@Lenient`
  field carries no `@Extract`.
- **Unifying the two publish policies.** §2.5. `@Lenient` publishes unconditionally, `@Capture` only
  when non-empty. Both are preserved because unifying either way changes behaviour a consumer can
  observe.
- **A diagnostic channel for silent no-ops.** The library has five silent swallows and at least seven
  silently dark features (`00-verified-facts.md` §9, §10.3). §2.9 proposes `create`-time exceptions
  for three specific misconfigurations rather than a general reporting mechanism, because a general
  one is its own design and this entry is already `large`.
