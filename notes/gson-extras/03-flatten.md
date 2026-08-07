# 03 - `@Flatten`

Design entry for the `@Flatten` annotation, reconciled against the research pack's already-accepted
design. The pack's entry is the baseline; every place this document departs from it is marked and
argued.

### d10-flatten - `@Flatten`

- **Registry entry:** `@Flatten` - "collapses a single-valued JSON object (or single-field value
  class) into the scalar or collection the caller actually wants, removing a wrapper level"
- **Verdict:** adopt narrowly
- **Category:** `value-shape-collapse`
- **Answers findings:** `f03-mapvalue-single-key`; partially `f03-dungeons-classmap-funnel`;
  explicitly declines `f03-biomewhispers-tier`
- **Cheaper alternative:** stock `com.google.gson.annotations.JsonAdapter` on the field - re-examined
  in §7 and no longer cheaper, because its one advantage was zero library cycles and the cycle is now
  sunk
- **Library change:** additive file - one annotation, one factory, one line in `GsonSettings.defaults()`
- **Adoption sites today:** 1
- **Effort:** `small`

The ID is carried forward from the research pack unchanged. `00-conventions.md` §3 freezes IDs, and
this entry refines `d10-flatten` rather than replacing it.

## 1. What this entry changes against the research pack

The pack's §6 is sound on the concept, the site selection, the declaration and the read-side rewrite.
Four things change, and three of them are corrections rather than refinements.

**1.1 - The conditional in the pack's verdict is now satisfied by construction.** `10-annotation-designs.md`
§6.9 rules: "If `d10-fallback` is accepted, `gson-extras` is being published anyway. Ship `@Flatten` in
the same commit... If `d10-fallback` is declined, decline `@Flatten` too." That rule was written when
the only other candidate cycle was `@Fallback`. This design cycle publishes `gson-extras` for the
shared-overflow and `@Extract` filter work regardless, so the cycle is sunk and the condition is met.
The verdict firms from conditional to unconditional **adopt narrowly**. §7 works the consequence
through for the `@JsonAdapter` comparison, because zero-cycles was that option's whole case.

**1.2 - CORRECTION: the pack's §6.5 recommendation of list index 3 is wrong, and it contradicts the
pack's own javadoc.** §6.2's proposed class doc says "Combine with `{@link Lenient @Lenient}` where the
wrapper is not guaranteed on every entry" (`10-annotation-designs.md`:381-382), while §6.5 says
`@Lenient` "does not compose at index 3" and recommends shipping at index 3 anyway. The documented
mitigation for the annotation's main failure mode is therefore unavailable in the recommended
configuration. §5 places the factory at index 5 instead and §9 deletes the javadoc line, because the
pair does not work at index 5 either.

**1.3 - CORRECTION: the pair `@Lenient` + `@Flatten` is not "resolvable by a move".** §6.5 says the
pair "is therefore resolvable - it just costs the deeper, more conservative default". It is not.
Index 3 loses the whole field to overflow on read; index 5 reads correctly and then corrupts the write
by re-wrapping the entries `LenientTypeAdapterFactory`:137-138 merged back. §9 traces both. The pair
must be a `create`-time exception, not a documented preference.

**1.4 - CORRECTION: `@Flatten` makes round-trip fidelity worse at the adoption site, not better.**
§6.3 claims "the value re-serializes correctly instead of not at all". The opposite is true. Today
`Currencies.essence` is declared `ConcurrentMap<String, ConcurrentMap<String, Integer>>`
(`Currencies.java`:18), so it binds the whole wrapper object and serializes it back byte-for-byte
including any sibling member. Under `@Flatten` the field holds only the collapsed value and the write
path re-wraps under exactly one key, so a sibling member such as `total` is **lost on serialize**.
§12 states this as the cost it is. `00-conventions.md` §4 requires that gap to be declared.

**1.5 - Refinement: silently-inert becomes a `create`-time throw.** §6.6 already prefers this for the
wrong-shape row. This entry extends it to every mis-declaration and names `JsonException` as the type
(§3). No factory in the library throws at `create` today - `SerializedPathTypeAdaptorFactory`:139 is
the closest and it throws at read time - so this is a new precedent, and §11 says what it costs.

## 2. Annotation declaration

`dev/simplified/gson/annotation/Flatten.java`. The pack's §6.2 declaration with three edits: the
`@Lenient` sentence is gone (§9 proves it wrong), the exclusions and the write-side loss are stated,
and the `@see` list matches the file that actually implements it.

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

Two house-style notes.

The opening line is a third-person verb, not the noun phrase `00-conventions.md` §8 asks for. That is
deliberate and it follows the pack's §6.2 argument: every sibling in the package opens with a verb
(`Split.java`:14 "Splits a single JSON string value...", `Lenient.java`:13 "Marks a `Map` or
`Collection` field..."). One new file is not the place to break a package's convention. If the rule
wins, all seven annotation files change in one commit.

`{@link Capture @Capture}`, `{@link Lenient @Lenient}` and `{@link SerializedPath @SerializedPath}`
resolve without imports because all four types share the `dev.simplified.gson.annotation` package -
the same-package case is not an inlined FQN and needs no import. `Map` and `Collection` do need the
imports shown, exactly as `Lenient.java`:9-10 carries them for the same reason.

## 3. Factory - the read path

`dev/simplified/gson/factory/FlattenTypeAdapterFactory.java`, modelled on `SplitTypeAdapterFactory`
- the smallest self-contained factory in the library (243 lines) and the only one whose shape is a
per-field transform rather than a per-object one.

`create` and the field scan:

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

The field-info holder, and the scan that builds it. Serialized-key resolution is copied verbatim from
`SplitTypeAdapterFactory.java`:197-199 so `@Flatten` addresses its field the same way every other
factory does - `@SerializedName` value if present, otherwise the Java field name:

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
**not inherited from a superclass field**. Consistent, and stated because the facts base calls it out
as the shared gotcha.

Returning `null` when idle is what keeps the other 132 DTO classes off the new path.
`LenientTypeAdapterFactory.create`:70-72 and `SerializedPathTypeAdaptorFactory.create`:39-41 return
the delegate instead, and that changes which factory a third factory's `getDelegateAdapter` resolves
to. A new factory must not join that pair.

`ConcurrentMap` satisfies `Map.class.isAssignableFrom` and `ConcurrentList` satisfies
`Collection.class.isAssignableFrom`, because `dev.simplified.collection.ConcurrentMap` declares
`extends Map<K, V>` (`ConcurrentMap.java`:23). The check is on the raw declared type, so it also
accepts the interface form the DTOs actually use.

The five throws are the new precedent. `JsonException` is the module's canonical failure type
(`JsonException.java`:14) and its `(@PrintFormat String, Object...)` constructor is the right one
here. §11 covers what a `create`-time throw costs.

The `read` body:

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

No reflective assignment anywhere. The delegate does the typing, which is why this is simpler than
`@Split` (which post-assigns at `SplitTypeAdapterFactory.java`:146-162 and swallows the failure at
`:160-161`). There is nothing here to swallow, so the factory ships with **zero** empty catch blocks -
worth stating explicitly against a library that currently carries five.

`JsonObject.add` replaces an existing member in place and keeps its position, because `JsonObject` is
backed by a `LinkedTreeMap` whose `put` on an existing key overwrites the value without re-linking.
So `rootObject.add(serializedName, collapsed)` swaps the field's value without disturbing key order.

One aliasing hazard, inherited rather than introduced. gson 2.11.0's
`TypeAdapters.JSON_ELEMENT.read` short-circuits when the reader is a `JsonTreeReader`
(`TypeAdapters.java`:858-861) and returns the **same** `JsonElement` reference via
`JsonTreeReader.nextJsonElement()` (`JsonTreeReader.java`:279-290) - no copy. Two consequences.
Buffering inside an existing tree chain is O(1), not a deep copy, so the marginal cost of one more
factory in the chain is a reference return. And a caller that hands a tree to
`gson.fromJson(JsonElement, Currencies.class)` has that tree **mutated in place** by the loop above.
`LenientTypeAdapterFactory` already does exactly this at `:183` and `:197`, so the contract is the
library's, not this factory's - but it is real, and the alternative (a `deepCopy()` of the field
element) costs a copy per flattened field and diverges from the sibling factories. Match the library.

## 4. Factory - the write path

The exact inverse, in the same shape as `SplitTypeAdapterFactory.write`:64-118.

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

Three properties of this path, two of them uncomfortable.

**The null asymmetry is deliberate.** `wrap` passes `JsonNull` through rather than emitting
`{"current": null}`, so a null entry value round-trips as a null rather than growing a wrapper. It is
also unobservable at the adoption site unless `GsonSettings.Builder.isSerializingNulls` is on -
`GsonSettings.create`:153-154 only calls `builder.serializeNulls()` when set, and `defaults()`
(`:240-257`) does not set it, so a null map value is dropped from the output entirely before `wrap`
matters. Keep the branch anyway; a downstream `GsonSettings` can turn nulls on.

**The write path is asymmetric with the read path, and the asymmetry is not decorative.** `unwrap`
tolerates an entry that is already collapsed (a bare `1955`) and leaves it alone; `wrap` wraps
everything. So a document that arrived half-wrapped serializes back fully wrapped. That is the
intended normalisation - the field's declared type is the collapsed one, so on the way out every
entry has the same shape - but it means `@Flatten` is not byte-identical on a mixed-shape input.

**The write path has no consumer-side coverage at all.** The hypixel module never serializes: a
`toJson` / `toJsonTree` search across `hypixel/src` returns zero hits. Every write path in
`gson-extras` is exercised only by the library's own 134 tests. A write-side regression in `@Flatten`
would pass all 16 hypixel tests, so the round-trip test §14 asks for is not optional garnish - it is
the only thing that will ever run this code.

## 5. The registration slot, derived

State the nesting depth first and derive the index from it, never the reverse. `GsonBuilder.create()`
reverses the user factory list (`GsonBuilder.java`:887-890) before the `Gson` constructor splices it
in at `Gson.java`:333, so **the last registered factory is the outermost wrapper** and registration
index runs opposite to nesting depth.

**Intended nesting: immediately outer to `Lenient`, immediately inner to `Capture`.** That is list
index 5 in `GsonSettings.defaults()`:248-257:

```java
            .withFactories(
                new CaseInsensitiveEnumTypeAdapterFactory(),
                new OptionalTypeAdapterFactory(),
                new SplitTypeAdapterFactory(),
                new SerializedPathTypeAdaptorFactory(),
                new LenientTypeAdapterFactory(),
                new FlattenTypeAdapterFactory(),
                new CaptureTypeAdapterFactory(),
                new CollapseTypeAdapterFactory(),
                new PostInitTypeAdapterFactory()
            );
```

Resulting chain, outermost first:

```
PostInit -> Collapse -> Capture -> Flatten -> Lenient -> SerializedPath -> Split -> Optional -> CaseInsensitiveEnum -> ... -> Reflective
```

The pack recommends index 3 instead (`10-annotation-designs.md`:534-536), which puts `@Flatten` inner
to `Lenient`. Here is why the slot moves.

**The honest starting point: the slot is almost entirely unconstrained.** Work the bounds from source
rather than from intuition.

| Neighbour | Does it constrain the slot? |
| --- | --- |
| Reflective binder | Yes. `@Flatten` rewrites the tree the binder reads, so it must be outer to it. Every candidate index satisfies this |
| `Capture` | No. A `@Flatten` field's serialized name is a **known key** - `discoverKnownKeys` (`CaptureTypeAdapterFactory.java`:109-146) walks every non-`@Capture` non-transient field and reads `@SerializedName` at `:135-140` - so the field survives into `knownObject` and reaches an inner `@Flatten` intact. Outer would work too |
| `Lenient` | No, once the same-field pair is excluded (§9). `Lenient` only rewrites its own fields' elements (`:183`, `:197`), `@Flatten` only rewrites its own. Disjoint fields, either order |
| `SerializedPath` | No. It hands the tree down untouched (`:103`) and binds its own fields with a fresh top-of-chain `gson.fromJson` at `:132`. Inner or outer, `@Flatten` sees the same tree |
| `Split` | No. It removes only its own `Pair`/`PairOptional` key (`:133`) |
| `Collapse`, `PostInit` | No. Both are outer and whole-object |

So the choice is a tiebreaker, and it should be made on a stated principle rather than on "deepest is
safest". The principle: **pick the slot from which the one excluded pair is at least reachable.**

At index 3, `Lenient` is outer. It sees the uncollapsed wrappers, judges every one incompatible with
the declared value type, and diverts the entire field to overflow before `@Flatten` is ever consulted.
The field binds empty. There is no future in which that composes.

At index 5, `Flatten` is outer. The read works correctly: collapse first, then `Lenient` types the
collapsed values and overflows only the entries that genuinely failed. Only the **write** is wrong,
and it is wrong for a reason (§9) that a future overflow-aware design could in principle address, at
the cost of state this entry deliberately does not add. Index 5 is therefore strictly closer to a
working pair than index 3, at zero cost today.

The pack's stated reason for index 3 - "it keeps the new factory as deep as possible, so it can only
ever see the tree the outer factories chose to hand down, and its blast radius is correspondingly
small" (`:542-544`) - does not survive the table above. `@Flatten` reads and writes exactly one key
per annotated field and nothing else; its blast radius is that field, at any index.

Two notes on the mechanics of inserting a factory at all.

Insertion **preserves the relative order of every existing factory**, so no existing pair is
reordered by this change. The facts base's A6 warns that inserting shifts every later index; the
shift is real but the ordering is not. The hazard is transcription - a later editor renumbering by
hand - and the guard is re-running the whole `GsonFactoryTest.CombinationTests` nest, which is the
only place in the library that observes nesting at all.

`GsonSettings.defaults()` also appends SPI factories (`:259`) and applies `GsonContributor`s
(`:261-263`) after the built-in list, and both land **outside** everything registered above. A
downstream module can therefore already wrap `@Flatten` from the outside, and no ordering guarantee
stated here is enforceable against it.

## 6. `Currencies.essence` - before and after

The whole file, `hypixel/src/main/java/api/simplified/hypixel/response/skyblock/member/Currencies.java`,
26 lines, verbatim as it stands today:

```java
package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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

After:

```java
package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Flatten;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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

Net: one nested generic parameter, one `@Getter(AccessLevel.NONE)`, one five-line stream accessor and
the `lombok.AccessLevel` import go; one import arrives. 26 lines to 19. `getEssence()` survives,
generated by the class-level `@Getter`, and its signature is unchanged - it already returned
`ConcurrentMap<String, Integer>`, which is the point of the finding.

The fixture value, `profiles[1].members[...].currencies`:

```json
{"coin_purse": 54601987.695, "motes_purse": 806795,
 "essence": {"WITHER": {"current": 1955}, "DRAGON": {"current": 1132}, "UNDEAD": {"current": 5141},
             "DIAMOND": {"current": 8564}, "SPIDER": {"current": 312}, "GOLD": {"current": 3445},
             "ICE": {"current": 2557}, "CRIMSON": {"current": 4}}}
```

Trace it against §3 and §5. `Currencies` carries no `@Capture`, `@Lenient`, `@SerializedPath`,
`@Split` or `@Collapse` field and does not implement `PostInit`, so every one of those factories
returns `null` or the bare delegate for this type and `FlattenTypeAdapter` is the **only** wrapper in
the chain above the reflective binder. It is therefore also the factory that pays the stream-to-tree
buffer, and that cost is new for this class - `Currencies` previously bound straight off the stream.
On a 25-key object nested in a 1.6 MB document that is not measurable, but it is a real change and
should not be described as free.

The delegate then binds `essence` as `ConcurrentMap<String, Integer>`, which resolves through the
collections SPI factory: `ConcurrentTypeAdapterFactory` (`:49-56`) maps `ConcurrentMap` to
`ConcurrentHashMap` and re-parameterises the token (`:76-81`), so the actual binder is gson's stock
`MapTypeAdapterFactory` with `TypeAdapters.INTEGER` on the value side. That matters for §11 - it is
the adapter that decides what a malformed entry does.

One behaviour change no reader should miss. Today a wrapper missing `current` produces
`value.get("current") == null`, and `Concurrent.newMap()` returns a `ConcurrentHashMap` backed by
`java.util.HashMap` (`ConcurrentHashMap.java`:20-26), which accepts null values - so the caller
silently receives a null. Under `@Flatten` the same input is left as an object, fails
`TypeAdapters.INTEGER`, and aborts the read of the **whole document**. §11 sizes that trade.

## 7. Stock `@JsonAdapter`, re-examined

The pack's §6.8 raised `com.google.gson.annotations.JsonAdapter` as the cheaper alternative and scored
it honestly: it wins on cycles, loses on everything else. The question this entry has to answer is
whether the loss of its one advantage changes the verdict.

**It does, and it settles the entry.** `@JsonAdapter`'s entire case was the zero in the "JitPack
cycles" column of `10-annotation-designs.md`:661-665. That column was decisive because the pack was
weighing `@Flatten` as a **standalone** cycle for a single adoption site, and `00-conventions.md` §4
is explicit that round trips dominate. This design cycle publishes `gson-extras` for the
shared-overflow and `@Extract` work whatever happens to `@Flatten`, so the cycle is sunk and that
column reads zero for both options. The table with the sunk cycle applied:

| Option | Consumer lines | Library lines | Marginal cycles | Hand-written adapter in `response/` | Reusable |
| --- | --- | --- | --- | --- | --- |
| Do nothing | 0 | 0 | 0 | no | n/a |
| `@JsonAdapter` + `EssenceAdapter` | -7, +~25 | 0 | **0** | **yes - the only one** | no |
| `@Flatten` | -7 | +~190 | **0** | no | yes |

With the cycles column tied, `@JsonAdapter` retains no advantage and keeps both of its disadvantages.
It cannot be parameterised by the wrapper key, because `@JsonAdapter` takes only a class literal
(`JsonAdapter.value()` is `Class<?>`), so a second wrapper key means a second adapter class. And a
hand-written `TypeAdapter` living in a DTO package is exactly the hand-rolled deserialization this
pack exists to delete - it would be the only one in `response/`.

Three things worth saying so the comparison is not read as a rout.

**`@JsonAdapter` is genuinely more precise about scope, and that is a real argument.** It attaches to
one field and bypasses the enclosing class's factory chain entirely for that field, so it cannot
interact with `@Lenient`, `@Capture` or anything else - the four exclusion rules in §8 to §10 simply
do not arise. `@Flatten` buys reusability by taking on a composition surface that has to be reasoned
about, tested and documented. That is the trade, stated plainly.

**"The cycle is sunk" is an argument about cost, not about risk.** Shipping `@Flatten` alongside the
overflow work does not make `@Flatten` safe; it makes it cheap. What makes it low-risk is that it
shares no code with the overflow work at all - a new file, a new annotation, one line in
`GsonSettings`. It touches no existing factory, so the regression baseline it can disturb is limited
to factory-nesting invariants, which §5 works through.

**That orthogonality has a sequencing consequence.** Ship `@Flatten` as its **own commit** inside the
shared publish, not folded into the overflow commit. Same JitPack build, same pin bump, separately
revertable. If the overflow work has to be backed out, `@Flatten` should not go with it, and a
combined commit makes that a surgical revert instead of a `git revert`.

**Doing nothing still scores better than it looks**, and the pack was right to say so. `essence` is
`@Getter(AccessLevel.NONE)` and its only public surface is `getEssence()`, which already returns the
collapsed type. The lying type is visible inside one 26-line file and nowhere else. Against that, the
positive case for `@Flatten` is not the seven deleted lines - it is that the collapse becomes
declarative and reusable, and that the library gains the one capability nothing in
`dev.simplified.gson.annotation` currently has: reaching the **value** side of a collection entry.

## 8. `@Capture` - three compositions, one of which works

"The flattened field is also a captured map value" is three different questions. They have different
answers and the pack only answered the first.

**8.1 - Same field carries both. Rejected at `create`.** `@Capture` is outer (§5). `CaptureTypeAdapter.read`
allocates a fresh empty `knownObject` (`:264`), classifies every root key into it or into the
frame-local captured/overflow maps (`:311-363`), and only then calls
`delegateAdapter.fromJsonTree(knownObject)` (`:366`). Every captured key is **absent** from the tree
the inner chain receives, and the `@Capture` field's own serialized name never appears in the wire
document at all for a catch-all or filtered capture. An inner `@Flatten` on that field therefore sees
nothing, forever, in any registration order that keeps `@Capture` outer. The map is built later still,
at `:377`/`:379`, by `buildGroupedMap`/`buildSimpleMap` - which `@Flatten` cannot reach.

This is the mechanical blocker the pack cites for `HeartOfTheForest.BiomeWhispers.tiers`
(`10-annotation-designs.md`:353-354), and it is correct. `tiers` is a grouping-mode `@Capture` field
with a catch-all filter, so `@Flatten("spent")` on it would be a silent no-op. §3's `create` throws
instead.

**8.2 - Different fields on the same class. Works, no constraint.** `discoverKnownKeys`
(`:109-146`) walks every non-`@Capture` non-transient field and registers its `@SerializedName` value
and alternates (`:135-140`) or its Java name (`:142`), so a sibling `@Flatten` field's key is a known
key, is copied verbatim into `knownObject` at `:315-318`, and reaches the inner `@Flatten` intact. No
test in the library covers this pair today; §14 asks for one.

**8.3 - `@Flatten` inside a `@Capture` map's value class. Works, and this is the interesting one.**
Both map builders deserialize the value with a fresh top-of-chain lookup -
`gson.fromJson(entry.getValue(), info.getValueType())` at `:399` for entry mode and
`gson.fromJson(group.getValue(), info.getValueType())` at `:475` for grouping mode. A top-of-chain
lookup for the value class runs that class's **own** adapter chain, including its own
`FlattenTypeAdapter`. So this composes:

```java
@Capture("^powder_")
private @NotNull ConcurrentMap<Powder.Type, Powder> powder = Concurrent.newMap();
```

with `Powder` declaring a `@Flatten` field of its own. `@Flatten` reaches the value side of a captured
entry perfectly well - it just has to be declared on the value class, not on the captured field.

Two caveats on 8.3, both adversarial.

The `create`-time throws from §3 fire during that `gson.fromJson`, which sits inside the empty catch
at `:401-402` (entry mode) or `:477-478` (grouping mode). A mis-declared `@Flatten` on a `@Capture`
value class is therefore **swallowed per entry**, and the map silently comes back short. That is the
library's fifth-and-sixth-worst existing defect eating this design's new diagnostic. It is not a
reason to place the throws elsewhere, but it is a reason the empty-catch work in this cycle and this
entry are coupled in one direction: fixing those catches makes `@Flatten`'s diagnostics real.

Nothing in `response/` needs 8.3 today. It is stated because it is the composition a reader will
assume is broken after reading 8.1, and assuming it is broken would push a future site toward a
hand-written adapter.

## 9. `@Lenient` - why the pair cannot round-trip in either order

This is the section that departs furthest from the pack, so it is traced rather than asserted.

The pack says the pair "does not compose at index 3, but the fix is a move rather than an exclusion"
and that at index 5 "the collapse happens first and `Lenient` is handed values it can type. The pair
is therefore resolvable" (`10-annotation-designs.md`:576-582). The first half is right. The second
half only checks the read.

Take `@Lenient @Flatten("current") ConcurrentMap<String, Integer> essence` against
`{"WITHER": {"current": 1955}, "BAD": {"total": 9}}`.

**Index 3 - `Flatten` inner, `Lenient` outer. Read is destroyed.** `LenientTypeAdapter.read` filters
first (`:165-200`): for each entry it calls `isCompatibleMapEntry(key, value, keyType, valueType)`
(`:177`) against the declared `Integer` value type. Both `{"current": 1955}` and `{"total": 9}` are
objects, both fail, both go to overflow (`:180`), and `replaceElement` writes an **empty** filtered
object back into the tree (`:183`). `@Flatten` then runs on an empty object. The field binds empty and
the entire content lives in the overflow store. Unusable.

**Index 5 - `Flatten` outer, `Lenient` inner. Read is correct, write is corrupt.**

Read: `@Flatten` collapses `WITHER` to `1955` and leaves `BAD` as `{"total": 9}` (no `current`
member, §3's `unwrap` returns the element unchanged). `Lenient` then types them - `WITHER` passes,
`BAD` fails and lands in overflow, keyed by the bound map instance at `:239`. Field is
`{WITHER: 1955}`, overflow is `{"BAD": {"total": 9}}`. Exactly what the pair is supposed to do.

Write, and this is the failure: the chain runs outer-to-inner, so `FlattenTypeAdapter.write` calls
`delegateAdapter.toJsonTree(value)` **first**, which runs `LenientTypeAdapter.write` to completion.
`Lenient` merges its overflow into the field's own sub-object at `:132-138` -
`locateElement(jsonObject, lenientInfo)` finds the `essence` object and copies every overflow entry
into it. `Flatten` then receives:

```json
{"essence": {"WITHER": 1955, "BAD": {"total": 9}}}
```

and wraps **every** entry, because §4's `wrap` has no way to tell a collapsed value from a
merged-back overflow entry:

```json
{"essence": {"WITHER": {"current": 1955}, "BAD": {"current": {"total": 9}}}}
```

`BAD` is now double-wrapped. The document does not round-trip.

**Every fix for that is worse than the exclusion.** Three were considered.

*Wrap only non-objects.* Works for `essence` because the collapsed type is `Integer`. Fails outright
for any `@Flatten` whose collapsed type is itself an object, which is the `Dungeons.classMap` shape.
A rule that silently depends on the collapsed type being scalar is the kind of implicit constraint
this pack keeps finding.

*Wrap only entries whose key is present in the bound map.* Sound in principle - overflow keys are not
in the bound map - but it requires `Flatten.write` to read the live field through the accessor and
serialize each Java key back to its JSON form to compare, which re-implements
`CaptureTypeAdapterFactory.serializeMapKey` and adds a conversion surface for one hypothetical site.

*Carry the collapse decision from read to write in a store.* This is the `@Lenient`/`@Capture`
overflow pattern - `WeakIdentityMap` is in the same package and would take it - and it is the correct
answer in the abstract. It is also the thing that turns `@Flatten` from an additive file with no
shared state into a fourth participant in this cycle's shared-store design, destroying the
orthogonality §7 relies on to justify shipping it at all. For zero adoption sites.

**Decision: `create` throws on the pair (§3), and the pack's javadoc line recommending `@Lenient` as
the mitigation for a missing wrapper (`10-annotation-designs.md`:381-382) is deleted.** That line is
the single most misleading sentence in the pack's §6, because it points a reader at the exact
combination that silently corrupts their document.

What is left as the mitigation for a missing wrapper is nothing, and §11 owns that.

One composition that does still work: `@Lenient` on field A and `@Flatten` on field B of the same
class. `LenientTypeAdapter` only rewrites the elements of its own fields (`:183`, `:197`) and
`FlattenTypeAdapter` only rewrites its own. Disjoint keys, no interaction, either index. No site
needs it today.

## 10. `@SerializedPath`, `@Split`, `@Collapse` and `PostInit`

**`@SerializedPath` - excluded, and the pack's reasoning is confirmed against source.** Read
`SerializedPathTypeAdaptorFactory.read`:99-144. It buffers the node (`:101`), hands it to
`delegateAdapter.fromJsonTree(outerJsonElement)` **untouched** (`:103`), and only afterwards walks
each `@SerializedPath` field, resolves it down the path (`:111-126`) and assigns it with
`gson.fromJson(innerJsonElement, fieldInfo.getAccessor().getGenericType())` (`:132`) plus a reflective
`set` (`:135`). Two consequences, both fatal to the pair.

There is **no flat-key rewrite on the read side at all** - the flat key exists only in `write`
(`:67-91`, `jsonObject.remove(flatKey)` at `:71`). So a `@Flatten` field that also carries
`@SerializedPath` has no root-level key for §3's `rootObject.get(info.getSerializedName())` to find,
at any index. And `gson.fromJson(..., getGenericType())` at `:132` is a **fresh top-of-chain lookup
for the field's declared type**, so the field never binds through the enclosing class's chain, which
is where a `FlattenTypeAdapter` for that class sits. The annotation is a silent no-op both ways.
`create` throws (§3).

Note the asymmetry this creates against §8.3. A `@Flatten` field on a **value class** binds through
that class's own chain and works; a `@Flatten` field alongside `@SerializedPath` on the **same field**
does not, because `@SerializedPath` re-enters at the top of the chain for the *field's type*, and
`@Flatten` is a property of the *field*, not of the type. That distinction is the whole reason
`@Flatten` cannot be expressed as a `TypeAdapterFactory` keyed on the value type.

**`@Split` - no interaction.** It claims only `Pair` and `PairOptional` raw types
(`SplitTypeAdapterFactory.java`:231-232), which are neither `Map` nor `Collection`, so no field can
carry both and pass §3's shape gate. It removes only its own key before delegating (`:133`).

**`@Collapse` - no interaction today, and `@Flatten` must stay inner if one appears.**
`CollapseTypeAdapterFactory` is outer at list index 6 and rewrites a `@Collapse` list-mode field's
JSON object into a JSON array before delegating. A hypothetical `@Collapse` + `@Flatten` pair would
need `@Flatten` inner, which every candidate index satisfies. Nothing in `response/` pairs them -
`@Collapse` has exactly one adoption site.

**`PostInit` - no interaction.** `PostInitTypeAdapterFactory` is the outermost factory and is a pure
`read`-then-`postInit()` pass-through. `@Flatten` has finished before it is reached. Worth one
sentence only because `PostInitTypeAdapterFactory`:37-38's empty catch swallows the entire
`postInit()` body including a `NullPointerException` from the unguarded dereference at `:36`, so a
`@Flatten`-induced failure surfacing inside a `postInit()` body would vanish. `Currencies` does not
implement `PostInit`.

**`CaseInsensitiveEnum` and `Optional`** claim enum and `Optional<?>` types and never wrap a POJO, so
they cannot see a `@Flatten` field's enclosing class. They can appear on the value side - a
`@Flatten("current") ConcurrentMap<String, Optional<Integer>>` would bind the collapsed value through
`OptionalTypeAdapterFactory` - and nothing about that is special.

## 11. Failure modes

Behaviour for `@Flatten("current") ConcurrentMap<String, Integer> essence`, which binds through
`ConcurrentTypeAdapterFactory` to gson's `MapTypeAdapterFactory` with `TypeAdapters.INTEGER` on the
value side (§6).

| Input | Read behaviour | Write behaviour |
| --- | --- | --- |
| `{"WITHER": {"current": 1955}}` | `{WITHER: 1955}` | `{"WITHER": {"current": 1955}}` - exact |
| `{"WITHER": {"current": 1955, "total": 9000}}` | `{WITHER: 1955}`, `total` read past | `{"WITHER": {"current": 1955}}` - **`total` lost** (§12) |
| `{"WITHER": {}}` - named key absent | element left as `{}`, then fails `Integer` | n/a, read aborted |
| `{"WITHER": {"total": 9000}}` - named key absent | element left as the object, then fails `Integer` | n/a, read aborted |
| `{"WITHER": 1955}` - already collapsed | binds; `unwrap` passes non-objects through | re-wrapped to `{"current": 1955}` - **normalised, not preserved** |
| `{"WITHER": null}` | left as `JsonNull`, binds as a null map value | `JsonNull` passed through by `wrap`, then dropped unless `isSerializingNulls` |
| `"essence": []` - array where a map is declared | the array branch collapses each element, then the map binder fails | n/a |
| `"essence": 5` - primitive | neither branch matches, passed to the delegate untouched, then fails | n/a |
| `"essence"` absent | `rootObject.get` returns null, field keeps its initialiser | field serialized empty |
| field is not a `Map` or `Collection` | `JsonException` at `create` | - |
| field also `@Capture` / `@Lenient` / `@SerializedPath` | `JsonException` at `create` | - |
| `@Flatten("")` | `JsonException` at `create` | - |
| field is `transient` | skipped, consistent with every other factory | - |

**The named key absent.** This is the row that decides whether the annotation is safe to ship, and it
is worse than the pack's table suggests. The element is left untouched and typed by the field's own
value type: `TypeAdapters.INTEGER.read` (`TypeAdapters.java`:266-279) calls `in.nextInt()`, which
throws `IllegalStateException` on a `BEGIN_OBJECT` token - not the `NumberFormatException` its
`catch` converts. `TypeAdapter.fromJsonTree` (`TypeAdapter.java`:241-248) catches only `IOException`,
so it propagates up the whole delegate chain to `Gson.fromJson(JsonReader, TypeToken)`, which converts
it at `:1371-1372`. The caller sees a `JsonSyntaxException` and **the entire document read fails**.

Sized against today's behaviour, that is a genuine regression in availability. Today the same input
gives `value.get("current") == null` and a null map value, and the profile parses. Under `@Flatten`
one malformed `essence` entry aborts a 1.6 MB `SkyBlockProfiles` response. The pack's §6.6 chose
"leave a non-conforming element untouched and let the delegate's own typing decide" as the *safe*
design, on the grounds that a wrong shape should surface as a normal Gson error rather than as a
silently absent entry. That reasoning is right about diagnosis and wrong about blast radius: the
error is not scoped to the field, it is scoped to the response.

Three things make it acceptable, and they should be stated together rather than assumed:

1. The pack's recommended mitigation - add `@Lenient` - does not exist. §9 proves the pair corrupts
   the round trip. There is no in-annotation escape hatch and this entry does not invent one.
2. The exposure is exactly one field of one class. The fixture shows `current` present on all eight
   `essence` entries across both profiles, and `essence` is the only site.
3. Failing loudly is this cycle's stated direction. The library carries five empty catches, and the
   research pack found four dark features behind one of them. Adding a sixth swallow to buy
   availability would be the wrong trade, and it is why §3's factory ships with none.

If that trade is judged unacceptable, the correct response is **not** to ship `@Flatten` with a
tolerance element. It is to leave `Currencies` alone: the do-nothing option in §7 already scores
close, and a `tolerant` element would be a second meaning bolted onto a one-site annotation.

**The wrapper carrying more than one key.** Read is fine and matches today's accessor exactly - both
take `current` and ignore the rest. The loss is on write and §12 owns it.

**A non-object value.** Two sub-cases and they behave differently. A non-object *entry* value (row 5)
is passed through by `unwrap` and binds if it already matches the declared type - deliberate
tolerance, so a partially-migrated upstream still reads. A non-object *field* value (rows 7 and 8) is
handed to the delegate untouched, exactly as `LenientTypeAdapterFactory` does for a shape mismatch
(`:171`, `:185` - neither branch matches, tree untouched), and the delegate's own typing decides.

**What a `create`-time throw costs.** No factory in the library throws at `create` today, so this is a
new precedent and it has two rough edges. The throw surfaces when the **enclosing** class's adapter is
built, which for a nested DTO can be far from the offending declaration - the message therefore names
the field, which is the only stable handle. And `GsonSettings.prewarm` swallows `Throwable` per type
(`:193-202`), so a mis-declaration on a prewarmed type is silent at warm-up; gson caches no adapter
for a failed resolution, so it re-throws on first real use. Net: the diagnostic is not lost, only
delayed. Both are acceptable; both should be in the commit message rather than discovered later.

## 12. Round-trip fidelity - the bar this annotation does not clear

`00-conventions.md` §4 is explicit: "`@Lenient` and `@Collapse` both preserve round-trip fidelity; any
new annotation that only handles reads must say so explicitly, and that gap is itself a cost." So say
it.

**`@Flatten` is a lossy projection.** The read takes one named member out of the wrapper and discards
the rest; the write reconstructs a wrapper containing only that member. Any sibling member the
document carried is gone from the output. That is structural, not an implementation gap - the field
no longer holds the information, so there is nothing to write back.

The pack asserts the opposite at `10-annotation-designs.md`:474-480: "Today `Currencies` round-trips
fine only by accident... Under `@Flatten` the collapse moves into the bind, so the factory owes the
re-wrap on write". The premise is inverted. Today `essence` is declared
`ConcurrentMap<String, ConcurrentMap<String, Integer>>` (`Currencies.java`:18) and binds the **whole
wrapper**, so gson serializes it back complete, sibling members and all. The collapse happens only in
`getEssence()` (`:20-24`), which is a read-side view. Today's round trip is not accidental - it is
total. `@Flatten` is the change that breaks it.

Concretely, if Hypixel adds a `total` key to `essence` values:

| | Today | Under `@Flatten("current")` |
| --- | --- | --- |
| Field content | `{WITHER: {current: 1955, total: 9000}}` | `{WITHER: 1955}` |
| `getEssence()` | `{WITHER: 1955}` - `total` invisible | `{WITHER: 1955}` - identical |
| Serialized back | `{"WITHER": {"current": 1955, "total": 9000}}` | `{"WITHER": {"current": 1955}}` |

The caller-visible behaviour is unchanged; the document is not. Three reasons this is still
acceptable at this site, in decreasing strength:

1. **The consuming module never serializes.** A `toJson`/`toJsonTree` search across `hypixel/src`
   returns zero hits. These are response DTOs read from an HTTP body. The loss is unobservable in the
   only consumer that exists.
2. **The absorptive capacity the wrapper theoretically buys is already forfeit.**
   `getEssence()`:21-23 maps every value through `.get("current")` and drops everything else, so a
   `total` key is silently discarded **today** by an accessor nobody would think to change. Collapsing
   moves that loss from a hidden accessor body to a visible field declaration.
3. It is one field of one class, and the fixture shows single-member wrappers on all eight entries.

What it is **not** acceptable to do is leave it undeclared. Two concrete requirements follow.

The javadoc must say it - §2's declaration carries the "projection, not a lossless transform"
paragraph for exactly this reason, and it is the one paragraph a reviewer should refuse to let be
trimmed.

The fixture assertion the pack asks for (`10-annotation-designs.md`:699-701) must be **stronger** than
"the output object is byte-equal to the input for the `essence` key". Byte equality passes trivially
on today's single-member fixture and would keep passing right up until the day it silently starts
dropping data. Assert instead that every `essence` value object has exactly one member and that it is
named `current`. That is the assertion that fires when the upstream shape moves, which is the only
event that matters.

For completeness, `@Flatten` also normalises rather than preserves in one direction: an input entry
that arrives already collapsed is written back wrapped (§4). Same class of deviation, much smaller.

## 13. The class-level form stays declined

The pack declines a class-level `@Flatten` at `10-annotation-designs.md`:631-637 - "bind a bare scalar
into my sole field when the incoming JSON is not an object" - on three grounds: no such flip appears
in the fixture, it removes no code and no class, and it would give one annotation name two unrelated
meanings. Those hold, and this entry adds a fourth that is specific to the library rather than to the
evidence.

**It would be the first type-level annotation in `gson-extras`.** All seven existing annotations are
`@Target(ElementType.FIELD)`; there is no type-level, method-level or parameter-level annotation
anywhere in the module. Every factory's discovery model is a single field walk with
`Reflection.setProcessingSuperclass(false)`. A class-level form introduces a second discovery axis -
`typeToken.getRawType().isAnnotationPresent(...)` - into a factory whose entire structure is the field
walk, and it would have to answer a question no existing factory faces: what an annotation on a
**superclass** means, given that the library's uniform answer for fields is "nothing, it is not
inherited". Annotations on types **are** inherited when marked `@Inherited` and are not otherwise, and
picking either answer sets a precedent for the other six annotations.

That is not a fatal objection on its own, but combined with zero evidence it settles it. If the flip
is ever observed, it is a new registry row with its own name, not a second element on this one.

## 14. Tests and regression anchors

Baseline to hold: gson-extras **134/134**, hypixel **16/16**.

New tests, in a `FlattenTests` nest in `GsonFactoryTest` alongside `CaptureTests` and the rest. The
write-side rows are not optional - §4 establishes that the consuming module never serializes, so
these are the only code that will ever execute `FlattenTypeAdapter.write`.

| Test | Asserts |
| --- | --- |
| `flattenMap_read` | `{"WITHER": {"current": 1955}}` binds `{WITHER: 1955}` |
| `flattenMap_roundTrip` | the same document serializes back byte-identical |
| `flattenCollection_read` | `[{"current": 1}, {"current": 2}]` binds `[1, 2]` - the array branch of §3 has **zero** adoption sites, exactly like `Dungeons.unlockedJournals` is the only site exercising `@Lenient`'s array branch |
| `flattenCollection_roundTrip` | the array branch re-wraps |
| `flattenAlreadyCollapsed_read` | `{"WITHER": 1955}` binds, and serializes back **wrapped** - pins the normalisation of §4 as intended, not accidental |
| `flattenMultiMemberWrapper_roundTrip` | `{"current": 1955, "total": 9000}` binds `1955` and serializes back **without** `total` - pins the §12 loss as a declared contract rather than a surprise |
| `flattenMissingMember_throws` | `{"WITHER": {}}` fails the read; assert the exception, not a silent empty map |
| `flattenNullValue_roundTrip` | a null entry survives as null with `isSerializingNulls` on |
| `flattenSerializedName` | the key resolves through `@SerializedName`, not the Java field name |
| `flattenWrongShape_throwsAtCreate` | `@Flatten` on a `String` field throws `JsonException` when the adapter is built |
| `flattenWithCapture_throwsAtCreate` | same field carrying both |
| `flattenWithLenient_throwsAtCreate` | same field carrying both - this is the §9 corruption, closed by construction |
| `flattenWithSerializedPath_throwsAtCreate` | same field carrying both |
| `flattenEmptyMember_throwsAtCreate` | `@Flatten("")` |
| `flattenIdleType_returnsNull` | a class with no `@Flatten` field gets no wrapper - guards the §3 `null`-when-idle rule that the two pass-through factories violate |

Two combination tests belong in `CombinationTests`, which is the only nest in the library that
observes factory nesting:

- `flattenSiblingCapture_ok` - `@Flatten` on field A, `@Capture` catch-all on field B of one class.
  Proves §8.2: A's key survives `discoverKnownKeys` into `knownObject`.
- `flattenInsideCaptureValue_ok` - a `@Capture` map whose value class carries `@Flatten`. Proves §8.3,
  which is the composition a reader will wrongly assume is broken.

Regression anchors to re-run unchanged, because inserting a factory into `GsonSettings.defaults()`
shifts indices even though it preserves relative order (§5):

- The **whole** `GsonFactoryTest.CombinationTests` nest, not just the new entries - specifically
  `lenientWithCapture_ok`, `lenientExtractCapture_ok`, and both `@Collapse` + `@Capture` pairs. These
  are the only tests that would detect a nesting mistake.
- `CaptureGroupingModeTest` and `CollectionValueCompatibilityTest` - the newest, least settled
  behaviour in the library.
- hypixel's 16, with `Currencies` newly load-bearing. Note that hypixel's suite exercises **read
  only**, so a green hypixel run says nothing about `@Flatten`'s write path.

One thing not to test for: `WeakIdentityMapTest` is untouched. `@Flatten` adds no store, no static
state and no cross-call lifetime. That is the property §7 leans on and it should stay true.

## 15. Verdict, sequencing and rollback

**Adopt narrowly.** Field-level only, `Map` and `Collection` only, mutually exclusive with `@Capture`,
`@Lenient` and `@SerializedPath`, lossy by declaration, one adoption site.

The pack's conditional is met: `gson-extras` is being published for this cycle regardless, so the
cycle cost that made `@JsonAdapter` competitive is sunk and `@Flatten` is the better shape for the
same marginal spend. What has changed versus the pack is not the verdict but the small print - a
different registration slot (§5), one composition promoted from "resolvable" to "rejected at `create`"
(§9), and a round-trip claim reversed (§12).

**Sequencing.** Three constraints, in order.

1. `@Flatten` shares no code with the shared-overflow and `@Extract` work. Ship it as its **own
   commit** inside the same publish, so it can be reverted without touching the overflow work and vice
   versa.
2. Land it **after** the overflow commits in the same branch, not before. It is the lowest-value item
   in the cycle and the one most easily dropped if the cycle has to be cut short; putting it last
   keeps that option open.
3. Adopt at `Currencies.essence` only after the pin bump, in a separate consumer commit.

**Take the two zero-cost neighbours regardless of what happens to `@Flatten`.** The pack is right that
they deliver more deleted lines than `@Flatten` does, at no library cost, and they are the reason the
adoption count is 1 rather than 3: retype `Dungeons.classMap` to
`ConcurrentMap<DungeonClass.Type, DungeonClass>`, which binds the existing JSON directly and deletes
six lines of `Dungeons.postInit()`; and put `@Getter(AccessLevel.NONE)` on
`HeartOfTheForest.BiomeWhispers.tiers` with `getSpent(int)` switched from `this.getTiers()` to
`this.tiers`. Neither needs this annotation and neither should wait for it.

**Rollback.** Three independent levels, which is the practical benefit of the additive shape:

| Level | Action | Cost |
| --- | --- | --- |
| Consumer | Revert the `Currencies` commit; the field returns to the map-of-maps and the accessor | No re-pin. The library keeps an unused annotation |
| Registration | Remove the one line from `GsonSettings.defaults()`; the annotation and factory stay on disk, inert | One publish and re-pin |
| Library | Revert the `@Flatten` commit entirely | One publish and re-pin, and it does not disturb the overflow commits because they share no files |

The consumer-level rollback is the one that matters, because it needs no JitPack cycle at all. Any
`@Flatten` problem found after the pin bump is undone by reverting one DTO.

**What would reopen the wider question.** A second wrapper key family appearing in `response/` turns
`@Flatten` from a one-site annotation into a reusable one and retires the "doing nothing scores
close" argument outright. A site that genuinely needs `@Flatten` together with `@Lenient` reopens §9,
and the answer there is a read-to-write channel, not a registration index - which is to say it becomes
a dependent of this cycle's shared-store work rather than an independent file.
