# 02 - `@Fallback`

Refines `d10-fallback` (`10-annotation-designs.md` §7) into an implementation-ready entry. The pack's
core choice - an **enum-constant marker**, not a field-level default supplier - is confirmed and
re-argued in §3. Two of its supporting claims are wrong against current source; §2 lists them and §6
works them. Neither is blocking - §6.2 reaches that conclusion explicitly, and corrects an earlier
draft of this document that said otherwise.

## 1. Design entry

### d02-fallback - `@Fallback`

- **Registry entry:** `@Fallback` - "supplies a default when the key is absent or the value fails to
  bind, replacing sentinel constants plus `getOrDefault` accessors"
- **Verdict:** adopt narrowly - the failed-bind half only, as an enum-constant marker, and applied
  only to enums that pass the §6.3 eligibility rule
- **Category:** `correctness`
- **Answers findings:** `f06-enum-null-clobber`, `f03-enum-unknown-null`, `f04-enum-null-fallback`;
  partially `f04-enum-of-parsers`. **Does not answer `f06-capture-null-enum-key`** - see §6, that
  finding belongs to the `@Capture` entry now. Explicitly does not answer
  `f06-completedat-zero-sentinel` or `f04-lookup-sentinel-drift`
- **Cheaper alternative:** none for the field-value path. For the map-key path there is one, it is
  cheaper, and it is better - §6.2
- **Library change:** existing factory edit (`CaseInsensitiveEnumTypeAdapterFactory`) plus the four
  compatibility guards in `CaptureTypeAdapterFactory` and `LenientTypeAdapterFactory` (§10), which are
  part of this change rather than a follow-up, plus one additive annotation file. Three edited
  factories
- **Adoption sites today:** **14** enum-valued bind sites behind **12** in-module enums, 8 of which
  need a sentinel constant added before they can be marked, plus the 2 `@Capture` key sites on
  `Kuudra.Tier`. Three further sites sit behind a second publish cycle in `Simplified-Api/skyblock`
  (§7). Down from the pack's "17 fields plus 7 maps"
- **Effort:** `medium` for the in-module half. `large` if the cross-module `Rarity` / `GameMode`
  half is in the same step, because that is a second library-publish cycle in a different repo and
  `00-conventions.md` §4 bumps a two-cycle proposal one level

## 2. What changes against the research pack's accepted design

Five deltas. The first two are corrections, the rest are refinements.

**D1 - the map-key half does not do what the pack claims, and the pack's ordering argument for it is
wrong.** `10-annotation-designs.md` §7.5 states "there is no ordering hazard, because the change makes
an innermost leaf adapter return a non-null value where it previously returned `null`. Nothing in the
chain branches on that null except the reflective binder." Gson's own `MapTypeAdapterFactory` branches
on it - not on the null itself but on what `Map.put` returns
(gson 2.11.0 `MapTypeAdapterFactory.java`:199-204) - and **about forty enum-keyed maps** in this module
bind through that adapter, not through `CaptureTypeAdapterFactory`. §6.2 works every case. The
conclusion is narrower than an earlier draft of this document claimed: the branch is on **duplication**,
not on nullness, so a sentinel that no wire value names moves nothing. The marker is therefore not
harmful on that path - it is merely close to useless there, upgrading one unknown key from `null` to a
typed constant and leaving everything else as it is. One marker does not fix both paths; the key path
belongs to the `@Capture` diversion (§10).

**D2 - `Kuudra.Tier.BASIC` must not be the marked constant.** §7.3 of the pack says "`Kuudra.Tier`
gets it on `BASIC`, which fixes both `@Capture` maps at `Kuudra.java`:19 and :21 in one line."
`Kuudra.java`:28-29 declares `BASIC` with `@SerializedName("NONE")`, so `BASIC` **is** on the wire.
Marking it makes the probe input `{"none":1,"brand_new_tier":4}` produce `{BASIC=4}` where it
produces `{null=4, BASIC=1}` today - the unknown tier now overwrites a **correct** entry. A fallback
constant that has its own wire representation destroys real data rather than parking unknown data.
§6.3 turns this into a rule.

**D3 - the site count is smaller and differently shaped.** The pack's fourteen defaulted fields
survive re-verification at their current line numbers, but two of them (`Kuudra.java`:40 and :42)
bind only indirectly through `CrimsonIsle.java`:40 and :43, `Kuudra.java`:65 is a fifteenth the pack
missed, and `Rarity` / `GameMode` are **not in this module** - they live in
`dev.sbs.skyblockdata.common`, in `Simplified-Api/skyblock`, which pins its own gson-extras sha
(`build.gradle.kts`:44, currently `2ba8143` against hypixel's `7cfc181`). Three of the pack's sites
are therefore behind a second module's publish cycle. §7 gives the corrected table.

**D4 - `@Fallback` deletes the only signal that an enum is under-modelled.** `ActiveCommission.Status`
(`ActiveCommission.java`:34-38) declares exactly one constant, `NOT_STARTED`. Marking it means every
in-progress and every claimed commission binds to `NOT_STARTED` - a confident, typed, wrong answer
where today there is a `null` that eventually throws somewhere and gets noticed. §11 makes this an
explicit precondition rather than a footnote: `@Fallback` is for values the model does not **want**
to know, never for values the model has not got round to modelling.

**D5 - the annotation should carry a `@Nullable` contract note and the adapter should expose its
resolved constant.** The `@Capture` and `@Lenient` compatibility checks need to distinguish "resolved
to a declared constant" from "resolved to the fallback" in order to keep unknown map keys in overflow.
That needs one package-private accessor, designed in §4 and consumed by the `@Capture` entry.

Everything else in the pack's §7 stands: the enum-constant marker beats both the field-level
annotation (its Shape 1) and the naming convention (its Shape 2), the absent-key half of the registry
line stays cut, `f06-boardquest-complete-status` is a typo fix that must not wait for this, and
`f04-lookup-sentinel-drift` stays out of scope.

## 3. Full annotation declaration

One new file, `dev/simplified/gson/annotation/Fallback.java`. Shape and javadoc follow `Lenient.java`
(a marker with no elements, a `Marks ...` opening, one `<pre>{@code ...}</pre>` example, a `@see` to
the implementing factory) rather than the pack's draft, so the file reads as a sibling of the six
annotations already in that package.

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

Three notes on the declaration.

- **`ElementType.FIELD` is exact, not a workaround.** An enum constant is a `public static final` field
  of the enum type, and `CaseInsensitiveEnumTypeAdapterFactory.java`:51 already reaches constant
  annotations that way (`enumClass.getField(constant.name()).getAnnotation(SerializedName.class)`).
  The narrower `ElementType.TYPE` would be wrong - the marker names a constant, not a type.
- **No elements.** Every element added later is a source-compatible addition only if it carries a
  default, and the entry has no evidence for one. In particular there is no `keys = false` element:
  the annotation sits on the constant, so it cannot see whether the constant is being read as a map
  key or as a field value. §6.2 is why that asymmetry needs no element: with a rule-1-compliant
  sentinel the map-key path is harmless, so there is nothing for a `keys = false` to protect.
- **No `@Documented`, no `@Inherited`.** Neither appears on any of the six existing annotations, and
  `@Inherited` does not apply to fields at all.

## 4. `CaseInsensitiveEnumTypeAdapterFactory` - the exact edit

No new factory. The file is 87 lines today; the edit adds one field, reuses the `getField` call the
constructor already makes, changes one `return`, and adds one static query. Four hunks.

**Hunk 1 - imports.** Add `dev.simplified.gson.annotation.Fallback`,
`dev.simplified.gson.exception.JsonException`, `java.lang.reflect.Field`, `java.lang.reflect.Type`.
`Gson`, `TypeAdapter`, `TypeToken`, `NotNull` and `Nullable` are already imported (`:3-12`).

**Hunk 2 - the resolved field.** Inside `CaseInsensitiveEnumTypeAdapter`, alongside the two maps at
`:43-44`:

```java
private final @Nullable E fallback;
```

**Hunk 3 - the constructor.** The loop at `:47-64` already calls `enumClass.getField(constant.name())`
inside a `try` that catches `NoSuchFieldException`, so the marker lookup costs one extra
`isAnnotationPresent` per constant and no extra reflection. Hoisting the `Field` into a local is the
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
catch. It fires during `create`, which is the first `gson.getAdapter` for that enum - in practice the
first decode that touches it. One wrinkle: `GsonSettings.prewarm` (`GsonSettings.java`:193-201)
catches `Throwable` and moves on, so if a consumer lists an enum in `withPrewarmTypes` the duplicate
is swallowed there and resurfaces at first real use. That is existing prewarm behaviour, not
something this entry should change.

**Hunk 4 - `read`, and the published query.** `:82` becomes:

```java
E constant = nameToConstant.get(in.nextString().toUpperCase());
return constant != null ? constant : this.fallback;
```

and the factory gains one static method, which is the contract the `@Capture` and `@Lenient`
compatibility checks consume (§10):

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

The private-field read is legal - `CaseInsensitiveEnumTypeAdapter` is a nested class of the factory,
so the enclosing class is inside its nest and needs no accessor. That keeps the adapter `private` and
adds no Lombok to a file that currently has none.

`gson.getAdapter(TypeToken.get(enumType))` returns this adapter **by identity**: for an enum type
`LenientTypeAdapterFactory` and `SerializedPathTypeAdaptorFactory` hand their delegate straight back
rather than wrapping (verified facts §2.3), and every other registered factory returns `null` for an
enum. The one degradation path is a downstream SPI factory that wraps enum types - then the
`instanceof` fails, `isFallback` returns `false`, and the caller falls back to the collapse behaviour.
Silent, but safe in the direction that matters.

Cost: about 22 changed lines in one existing file, one new 45-line annotation file. One JitPack cycle
for gson-extras, one re-pin in hypixel - and a second cycle in `Simplified-Api/skyblock` if the
`Rarity` / `GameMode` half is in scope (§7).

## 5. Registered first, resolved last - what index 0 actually buys

`CaseInsensitiveEnumTypeAdapterFactory` is the **first** entry in `GsonSettings.defaults()`
(`GsonSettings.java`:249). `GsonBuilder.create()` reverses the user list before constructing `Gson`
(gson 2.11.0 `GsonBuilder.java`:887-890), so index 0 is the **last** user factory consulted and
therefore the **innermost** of the eight - a leaf adapter, never a wrapper. It claims exactly one
thing (`create` returns `null` unless `rawType.isEnum()`, `:35-36`) and it delegates to nothing.

Three consequences, and the third is the whole of §6.

**It adds no index.** This entry registers no factory, so `GsonSettings.defaults()` is untouched and
every ordering invariant in the regression baseline (verified facts §11 A1-A7) is unaffected. No other
entry in this cycle can say that. The `medium` rating comes purely from "edits an existing factory",
not from blast radius through the chain - and note that the factories edited are three, not one, since
§10's four compatibility guards are part of this change and land in `CaptureTypeAdapterFactory` and
`LenientTypeAdapterFactory`. Four library files total, which is still inside `medium`'s band.

**Everything reaches it, and nothing can intercept its result.** A leaf has no delegate to hand a
repaired value to, and no wrapper above it inspects the enum result before it lands. These are all the
call sites that reach it in the current library, read off source:

| Caller | Site | What it reads as an enum |
| --- | --- | --- |
| gson reflective binder | `ReflectiveTypeAdapterFactory.java`:265-274 | every enum-typed field |
| gson map adapter | `MapTypeAdapterFactory.java`:196-205 | **map keys and values of every enum-keyed map** |
| gson collection adapter | `CollectionTypeAdapterFactory` | enum elements of a list or set |
| `@Capture` build | `CaptureTypeAdapterFactory.java`:398, :399, :474, :475 | captured keys and values |
| `@Capture` classify | `CaptureTypeAdapterFactory.java`:491, :539 | the compatibility probe for keys and values |
| `@Lenient` filter | `LenientTypeAdapterFactory.java`:260, :310 | the compatibility probe for keys and values |
| `@Extract` assign | `LenientTypeAdapterFactory.java`:244 | an extracted value bound fresh from the top of the chain |
| `@Split` | `SplitTypeAdapterFactory.java`:153-154 | both halves of a delimited pair |
| `@Collapse` / `@Key` | `CollapseTypeAdapterFactory.java`:253-257 | an enum-typed injected key |
| `Optional<E>` | `OptionalTypeAdapterFactory.java`:54 | `Optional.ofNullable(adapter.read(in))` |
| `@SerializedPath` | `SerializedPathTypeAdaptorFactory.java`:132 | a fresh top-of-chain bind |

The pack read that table as pure upside - "this is what makes this fix reach so far for so little".
Most of it is. The `MapTypeAdapterFactory` row is the one that needs working rather than asserting,
because it is the one caller in the list that **gson-extras does not own** and cannot teach about the
marker. Rows 4 through 11 can all be taught via `isFallback` (§4). Rows 1 through 3 cannot. §6.2 works
row 2 case by case and finds it harmless for the only marking the eligibility rule permits.

**The position is not adjustable.** There is no index at which an enum leaf adapter stops being
reached by gson's own map adapter, so "scope the fallback to field values only" is not achievable by
reordering, by wrapping, or by an annotation element. §6.2 is why that turns out not to matter.

## 6. The map-key path, verified

The pack's design rests on one marker serving two paths - the enum-typed **field value** and the
enum-typed **map key**. The value path is straightforward and §4 covers it. This section takes the key
path apart, because that is where the design is doing something it was not designed for. The answer it
reaches is that the key path is safe and nearly useless, which is different from both "it works" and
"it blocks".

### 6.1 `@Capture` key conversion does route through the enum adapter

Confirmed, by source and by an executed probe. This was the stated blocking question and the answer is
**no block**.

`CaptureFieldInfo` takes `keyType` from the field's first type argument
(`CaptureTypeAdapterFactory.java`:658-661), and every key conversion in the factory is the same call:

```java
Object key = this.getGson().fromJson(new JsonPrimitive(entry.getKey()), info.getKeyType());
```

at `:398` (`buildSimpleMap`), `:474` (`buildGroupedMap`), and as the compatibility probe at `:491`
(`isCompatibleCaptureEntry`). `Gson.fromJson(JsonElement, Type)` resolves through
`getAdapter(TypeToken.get(type))`, and the reversed user-factory list is spliced into `Gson`'s search
order **ahead of** the stock platform factories (`Gson.java`:333 versus `:336`+), so an enum type is
claimed by `CaseInsensitiveEnumTypeAdapterFactory` (`:35-38`) before gson's own
`TypeAdapters.ENUM_FACTORY` ever sees it.

The empirical clincher is already in the pack, unremarked. `f06-capture-null-enum-key`'s probe fed
`{"none":1,"brand_new_tier":4}` into `Kuudra` and got `{null=4, BASIC=1}`. `Kuudra.Tier.BASIC` carries
`@SerializedName("NONE")` (`Kuudra.java`:28-29) and the wire key is **lowercase** `none`. Gson's stock
enum adapter matches `@SerializedName` case-**sensitively**; only
`CaseInsensitiveEnumTypeAdapterFactory.java`:82's `.toUpperCase()` turns `none` into `NONE`. That
probe therefore proves the map-**key** conversion ran through this adapter and not a stock one. A
`@Fallback` constant will be returned there.

Two smaller confirmations while the path is open. `serializeMapKey` on the write side
(`CaptureTypeAdapterFactory.java`:584-606) does **not** use the adapter - it hand-rolls
`@SerializedName`-then-`name()`, so a marked constant writes as its own name with no special casing.
And grouping mode converts its key at `:474` from a group key assembled out of affix matching, so the
six enum-keyed sites split four entry-mode (`Dojo` x2, `Kuudra` x2) and two grouped
(`TrophyFishing.fish`, `HeartOfTheMountain.powder`) - both reach the adapter.

### 6.2 The map-key path under gson's own map adapter - what it costs and what it does not

**Correction, and it changes this section's conclusion.** An earlier draft of this section called the
`MapTypeAdapterFactory` behaviour below a *blocking* finding and derived a second eligibility rule from
it. That derivation was unsound: its only genuine regression row required the marked constant to be
nameable from the wire, which rule 1 below independently forbids. Worked with a rule-1-compliant
sentinel the plain-map path is **not a regression in any case** - it is a small improvement in one and
unchanged in the rest. The section is rewritten to what the evidence supports. The consequence is that
the eligibility list in §7 is shorter than it was, not longer, and that `DungeonClass.Type` is markable
today rather than gated.

Reaching the six `@Capture` sites is not the same as fixing them, and the sites that are **not**
`@Capture` are where the question is.

`FloorData.java` alone declares **seventeen** `ConcurrentMap<Floor, ...>` fields (`:24` through `:68`),
none of them carrying `@Capture` or `@Lenient`. `Statistics.java` adds nine more across
`Mythos` (`:89-95`), `Auctions` (`:116-118`) and the nested boss block (`:157-165`); `JacobsContest`
two (`:25`, `:41`); plus `Dungeons.classMap` (`:32`), `ChocolateFactory.employees` (`:37`),
`GlaciteTunnels.lootedCorpses` (`:18`) and `HypixelSocial.links` (`:11`). That is roughly **forty**
enum-keyed maps that bind through `ConcurrentTypeAdapterFactory`
(`collections`, `:62-74`, which just re-resolves `ConcurrentMap<K, V>` to `ConcurrentHashMap<K, V>`)
and then through gson's stock `MapTypeAdapterFactory`.

That adapter's read loop is (gson 2.11.0 `MapTypeAdapterFactory.java`:196-205):

```java
K key = keyTypeAdapter.read(in);
V value = valueTypeAdapter.read(in);
V replaced = map.put(key, value);
if (replaced != null) {
  throw new JsonSyntaxException("duplicate key: " + key);
}
```

A null key is not special to that loop. It is a key like any other, and the throw fires on the
**second** occurrence of any key, null included, because `HashMap.put(null, v)` returns the previous
value and `dev.simplified.collection.ConcurrentHashMap` is backed by a plain `HashMap` (`:20-27`), not
by `java.util.concurrent.ConcurrentHashMap`. That is why `{null=3}` is reachable at all.

Work the cases for `ConcurrentMap<Floor, Integer> timesPlayed`, given `Floor`'s constants are
`@SerializedName("0")` through `("7")` (`Floor.java`:13-28). **Two different markings have to be
distinguished, and conflating them is where the earlier draft went wrong.**

**Marking A - a constant that is nameable from the wire**, say `ENTRANCE` with its `@SerializedName("0")`.
This is what the earlier table modelled:

| Wire | Today | Marking A |
| --- | --- | --- |
| `{"0":5, "8":3}` | `{ENTRANCE=5, null=3}`, silent | **`JsonSyntaxException: duplicate key`** - the whole profile decode aborts |
| `{"8":3}` | `{null=3}`, silent | `{ENTRANCE=3}` - foreign data **attributed to a real floor**, silent and undetectable |
| `{"8":3, "9":1}` | `JsonSyntaxException` | `JsonSyntaxException` - unchanged |

Both bad rows are real, and **both are already forbidden by rule 1**, which exists for the independent
`Kuudra.Tier.BASIC` reason in §7 and would forbid marking `ENTRANCE` even if `Floor` were never a map
key. Marking A is not evidence about the map-key path; it is evidence about rule 1.

**Marking B - a rule-1-compliant sentinel**, a new `UNKNOWN` with no `@SerializedName`, no `alternate`,
and a `name()` the wire never carries. This is the only marking §7 ever recommends, and it is the one
the earlier table never worked:

| Wire | Today | Marking B |
| --- | --- | --- |
| `{"0":5, "8":3}` - one unknown | `{ENTRANCE=5, null=3}`, silent | `{ENTRANCE=5, UNKNOWN=3}` - **better**: typed, iterable, and it cannot break a `keySet()` pipeline the way a `null` key does |
| `{"8":3}` - one unknown | `{null=3}`, silent | `{UNKNOWN=3}` - same improvement. Nothing is attributed to a real floor, because rule 1 guarantees the sentinel is not one |
| `{"8":3, "9":1}` - two unknowns | `JsonSyntaxException` | `JsonSyntaxException` - unchanged, and for the identical mechanical reason: the second `put` under the same key returns non-null |
| the sentinel's own key on the wire | n/a | **unreachable** - rule 1 is exactly the statement that this row does not exist |

**There is no regression row.** The duplicate-key throw the earlier draft treated as the blocker needs
two entries to collapse onto one key, and today two unmatched entries already collapse onto `null` and
already throw. The marker changes which key is in the exception message and nothing else. Note that
rule 1 has to be read **case-insensitively** - `CaseInsensitiveEnumTypeAdapterFactory.java`:82
uppercases the incoming string and `:63` registers `constant.name().toUpperCase()`, so a wire key
`"unknown"` would reach an `UNKNOWN` sentinel.

What the marker does **not** do on this path is worth stating as plainly as what it does:

- **It does not restore the data.** One unknown key becomes typed instead of null; two or more still
  abort the decode. Neither case tells you what the wire actually said.
- **It does not round-trip either.** Today a `null` key writes as the string `"null"`, because gson's
  map adapter stringifies `keyTypeAdapter.toJsonTree(key)` and a null enum yields `JsonNull`. With the
  marker it writes as `"UNKNOWN"`. Both are keys Hypixel never sent; the marker changes the fabricated
  key, not the fabrication.
- **The position is not adjustable.** There is no index at which an enum leaf stops being reached by
  gson's own map adapter, so "scope the marker to field values only" is not achievable by reordering,
  by wrapping, or by an element. With marking B that no longer matters, because the reach is harmless.

The same rows apply to `@Lenient`'s one enum-keyed site, `FloorData.completions` (`:28-30`), because
`isCompatibleMapEntry` (`LenientTypeAdapterFactory.java`:258-264) treats the key as compatible whenever
the conversion **does not throw** - a `null` result passes, and so would a fallback result. The entry
survives the filter, is handed to the delegate, and lands in the same stock map adapter. §4's third
companion guard is what changes that, and it changes it for the better: an unknown key becomes
incompatible, is diverted to overflow under the key the document carried, and round-trips losslessly.

One claim in `10-annotation-designs.md` §7.5 still does not hold as written: "nothing in the chain
branches on that null except the reflective binder". `MapTypeAdapterFactory` branches on `put`'s
return. What is now clear is that it branches on **duplication**, not on nullness, so replacing the
null with a sentinel that no wire value names moves the branch point nowhere.

### 6.3 The eligibility rule that follows

**One mechanical rule, not two.** `@Fallback` is safe on an enum if and only if:

1. **The marked constant has no wire representation of its own.** It is a sentinel that no incoming
   value legitimately names - not via `name()`, not via `@SerializedName.value`, not via an
   `alternate`, and the comparison is **case-insensitive** because the adapter uppercases both sides.
   Otherwise unknown values overwrite a correct entry (`Kuudra.Tier.BASIC`, D2) or a correct field, and
   on a stock-bound map they turn a document that decodes today into a `JsonSyntaxException` (§6.2,
   marking A).

That rule carries the whole of the mechanical safety argument, on every path the marker reaches. §6.2
worked the stock map-adapter path with a rule-1-compliant sentinel and found no regression in any case.

**What was a second rule is now a limitation, and the distinction matters because it decides the
adoption list.** A plain `Map<E, V>` bound by gson's stock adapter is not *harmed* by a marked `E`, but
it is barely *helped* either: one unknown key becomes a typed sentinel instead of `null`, two or more
still abort the decode exactly as they do today, and neither case round-trips. So the marker is not
worth adding to an enum whose only appearances are stock-bound map keys - not because it is unsafe, but
because it buys almost nothing there and every sentinel addition costs a look at the enum's own
`values()` consumers. Where the enum **also** has a field-value site, mark it: the field-value repair is
the point, and the map-key path comes along for free and does no harm.

The improvement that *is* available on the map path is not the marker, it is the diversion. Putting
`@Lenient` on the map moves the key check into `LenientTypeAdapterFactory`, where the `isFallback`
query is available and an unknown key is sent to overflow under the key the document carried - typed,
lossless, and it round-trips. That is a per-field consumer annotation, not a library change, and it is
now an **optional upgrade** rather than a gate on marking the enum.

A second rule is a modelling precondition rather than a mechanical one, and D4 is its evidence: **do
not mark an enum that is merely incomplete.** `ActiveCommission.Status` (`ActiveCommission.java`:34-38)
has one constant. Marking it does not repair a defect, it hides one.

## 7. Which enums get the marker

Every enum in `response/` was checked against the rule in §6.3, and the wire vocabulary was read off
the real fixture (`src/main/resources/craftedfury.json`) rather than assumed. The pack's "roughly a
dozen one-line consumer edits" does not survive that check: **eight of the twelve eligible enums have
no sentinel constant to mark**, because their existing default is itself a live wire value.

**Group A - mark now, the sentinel already exists and never appears on the wire.**

| Enum | Marked constant | Repairs | Map-key exposure |
| --- | --- | --- | --- |
| `CrimsonIsle.Faction` (`CrimsonIsle.java`:144-152) | `NONE` | `CrimsonIsle.java`:27 `selectedFaction` | none - never a map key |
| `DungeonData.Type` (`DungeonData.java`:61-67) | `UNKNOWN` | `DungeonRun.java`:24 `dungeonType` | none - its only map use is `Dungeons.java`:52, which is `transient` and never binds |
| `BoardQuest.Status` (`BoardQuest.java`:20-26) | `UNKNOWN` | `BoardQuest.java`:15 `status` | none |
| `DungeonClass.Type` (`DungeonClass.java`:52-58) | `UNKNOWN` | `Dungeons.java`:39 `selectedClass`, `FloorData.java`:109 `BestRun.dungeonClass` | `Dungeons.java`:32 `classMap`, stock-bound - harmless under §6.2 marking B. `@Lenient` on `classMap` is the optional upgrade, not a gate |

Fixture check: `selected_faction` only ever carries `barbarians`, and `mages`/`barbarians` are the only
two `@SerializedName` values, so `NONE` is unreachable from the wire. `UNKNOWN` is unreachable in the
other three - `DungeonClass.Type.UNKNOWN` carries no `@SerializedName` and the fixture's class names are
`healer`, `mage`, `berserk`, `archer` and `tank`.

`DungeonClass.Type` is the pack's headline example - its §7.3 claims one edit "repairs three separate
sites at once". An earlier draft of this document demoted it to a blocked group on the strength of the
§6.2 duplicate-key argument. That argument was unsound for a rule-1-compliant sentinel, so the headline
example stands: two field-value sites repaired, and `classMap` goes from `{null=...}` to
`{UNKNOWN=...}` on one unknown class and throws on two, exactly as it does today.

**Group B - eligible, but the enum needs a new sentinel constant first.** In each of these the field's
current default is a real wire value, so marking it would violate rule 1.

| Enum | Current default is on the wire as | Add and mark | Repairs |
| --- | --- | --- | --- |
| `Kuudra.Tier` | `BASIC` is `@SerializedName("NONE")`, and `highest_wave_none` is in the fixture | `UNKNOWN` | `Kuudra.java`:40, `:65`; keys of `Kuudra.java`:19, `:21` |
| `Kuudra.SearchSettings.Sort` | `RECENTLY_CREATED` | `UNKNOWN` | `Kuudra.java`:42 |
| `RabbitSort` | `A_TO_Z` is `@SerializedName("a_to_z")` | `UNKNOWN` | `ChocolateFactory.java`:30 |
| `RabbitFilter` | `NONE` has no `@SerializedName`, so wire `none` matches it | `UNKNOWN` | `ChocolateFactory.java`:32 |
| `Crystal.State` | `NOT_FOUND` - the fixture carries both `FOUND` and `NOT_FOUND` | `UNKNOWN` | `Crystal.java`:10 |
| `Banking.Action` | no default at all; `WITHDRAW`/`DEPOSIT` are both live | `UNKNOWN` | `Banking.java`:24 |
| `CommunityUpgrades.Type` | no default; all five constants are live | `UNKNOWN(0)` - the enum has a `maxLevel` component | `CommunityUpgrades.java`:59 |
| `DungeonChest.Type` | no default; all six are live | `UNKNOWN` | `DungeonChest.java`:20 |

Adding a constant is not free: it changes `values()` for `Dojo.Type.of`-style helpers and for any
`Arrays.stream(values())` reduction. Each addition needs a look at the enum's own static helpers.

**Group C - deferred for cost, not for safety.**

| Enum | Field sites it would repair | Why deferred |
| --- | --- | --- |
| `Rarity` (`dev.sbs.skyblockdata.common`) | `OwnedPet.java`:32 `baseRarity`, `SkyBlockAuction.java`:40 `rarity` | no sentinel (`COMMON` is live) **and** a chained publish cycle |

`Rarity` lives in `Simplified-Api/skyblock`, which pins its own gson-extras sha
(`build.gradle.kts`:44, `2ba8143` today against hypixel's `7cfc181`). Marking it costs: bump skyblock's
pin to the new gson-extras sha, add `UNKNOWN`, publish skyblock, re-pin skyblock in hypixel. Two
chained publish cycles for two field sites. **Defer it out of the first step.** Its two stock-bound map
uses (`Statistics.java`:116 `totalBought`, `:118` `totalSold`) are not a reason to defer and do not need
annotating - §6.2 marking B leaves them where they are.

**Group D - no field-value site worth the sentinel. Marking is permitted and is close to pointless.**

`Floor` (17 maps in `FloorData.java`:24-68 plus the `@Lenient` `completions` at `:30`),
`Statistics.Mythos.Type` (`:89-95`), `Statistics`' nested boss `Type` (`:157-165`),
`RabbitEmployee` (`ChocolateFactory.java`:37), `GlaciteTunnels.CorpseType` (`:18`),
`HypixelSocial.Type` (`:11`).

These appear only as stock-bound map keys. An earlier draft said "never mark - marking them is a
regression"; that was the unsound §6.2 argument and it is withdrawn. What is true is that marking them
buys only the one-unknown-key upgrade from `null` to a typed sentinel, costs a new constant plus a look
at every `values()` consumer, and leaves the two-or-more case throwing exactly as it does today. Skip
them because the trade is poor, not because they are unsafe. The real fix for these seventeen `Floor`
maps is `@Lenient` plus §4's diversion, which is a separate decision this entry does not make.

`JacobsContest.Medal` (`:25`, `:41`) is a genuine "do not mark", and for a reason unrelated to maps: its
one field site is `Optional<Medal>` at `:113`, and `OptionalTypeAdapterFactory.java`:54 is
`Optional.ofNullable(...)`, so a marked constant flips `isPresent()` from `false` to `true` on every
unrecognized value (§11).

**Group E - `@Capture` keys only. Prefer the overflow diversion, treat the marker as optional.**
`Dojo.Type`, `Powder.Type`, `TrophyFish`. §9 and §10 explain why, with fixture evidence.

**Group F - cross-module.** `GameMode` (`dev.sbs.skyblockdata.common`) repairs `SkyBlockIsland.java`:34
and is never a map key, but still costs the skyblock cycle. Bundle it with `Rarity` in the deferred
step.

**Group G - do not mark, the enum is under-modelled rather than under-defaulted.**
`ActiveCommission.Status` declares one constant, `NOT_STARTED`. Two more from the fixture are
unmodelled. And `RabbitSort` sits half in this group: the fixture carries `rarity_high_low`, which
matches none of its four `@SerializedName` values, so `ChocolateFactory.rabbitSort` is **null in
production today** for a modelling reason. `@Fallback` would turn that into a confident `UNKNOWN` and
the missing `@SerializedName` would never be found. Fix the names first, then mark.

Net for a first step: **14 enum-valued bind sites behind 12 in-module enums**, 8 of which need a
constant added, plus the 2 `@Capture` key sites on `Kuudra.Tier`. Group C adds 2 more sites and Group F
adds 1, both behind the skyblock publish cycle. The count moved up by two sites and one enum against an
earlier draft, because `DungeonClass.Type` is in group A rather than behind a rule that did not survive
§6.2.

## 8. An enum with no marked constant - the unchanged-behaviour guarantee

The guarantee is that `this.fallback` is `null` for an unmarked enum, so
`return constant != null ? constant : this.fallback;` returns exactly what
`return nameToConstant.get(...)` returns today, for every input. This is worth stating precisely
because it is the entire argument for an opt-in marker over the pack's rejected Shape 2, and because
"unchanged" has to mean unchanged for **every** consumer of gson-extras, not just this module.

What is guaranteed, and why:

- **The read result is bit-identical.** `nameToConstant` is populated exactly as before - hunk 3 moves
  the `getField` call into a local and adds a branch, it does not touch the three `put` calls at
  `:61-63`. The `JsonToken.NULL` short-circuit at `:77-80` is untouched, so a JSON null still reads as
  `null` whether or not the enum is marked.
- **The write result is bit-identical.** `write` (`:68-73`) is not edited at all. `constantToName` is
  unchanged, so a marked enum still serializes every constant - including the fallback - by its own
  name or `@SerializedName` value.
- **`create` still returns `null` for every non-enum type** (`:35-36`), so no type that does not
  already resolve to this adapter starts resolving to it.
- **No factory is added or moved**, so `getDelegateAdapter` resolves identically for every other
  factory in the chain (verified facts §11 A6, A7).
- **`isFallback` returns `false` for an unmarked enum** without needing a special case, because
  `adapter.fallback` is `null` and the method's first line rejects a `null` `value`. A caller that asks
  about an unmarked enum gets the "not a fallback" answer, which is the answer that preserves current
  behaviour in the `@Capture` and `@Lenient` compatibility checks.

What is **not** guaranteed, and must be said:

- **`@Fallback` on a non-enum field is inert.** `create` never builds the adapter for a non-enum, so
  the marker is silently ignored - the exact failure class this whole cycle keeps finding. The pack
  flagged this as "consider rejecting it". Rejecting it properly would mean a scan of every field of
  every type, which is a new cost in a leaf adapter that currently does none. The cheap and correct
  place for the check is a compile-time one, and there is none available. Recommendation: leave it
  inert, and put the constraint in the annotation javadoc where §3 already puts it (the marker names a
  constant), rather than paying for a runtime scan that only fires on a mistake nobody has made yet.
- **The `@Retention(RUNTIME)` marker is visible to reflection generally.** Anything in a consuming
  module that enumerates field annotations sees a new one. Nothing in this workspace does.

## 9. Round-trip, and the information the collapse destroys

An unmatched wire value collapsing onto the fallback constant loses the original string. That is real
and unavoidable, and the two paths deserve different answers.

**The field-value path: accept the loss.** `write` is untouched, so `constantToName` maps the fallback
constant to its own name (`:72`) and an `UNKNOWN` that arrived as `"necromancer"` serializes as
`"UNKNOWN"`. Compare against today honestly:

| | Today | With `@Fallback` |
| --- | --- | --- |
| In-memory value | `null` on a `@NotNull` field | the sentinel constant |
| Serialized output | **the key is omitted entirely** - `GsonSettings.Builder.serializingNulls` is uninitialised at `:304`, so `serializeNulls()` at `:154` is never called | the key is present with the sentinel's name |
| Fidelity | lossy - the value is gone | lossy - the value is replaced |

Neither round-trips. The pack's §7.6 note ("it was already lossy, because `null` serializes as JSON
null today") is directionally right and mechanically wrong - with `serializeNulls` off the field does
not serialize at all. **Accept it**, for three reasons: nothing in this module re-serializes a profile
back to Hypixel, so the write path here is a debugging and caching surface rather than an API contract;
a scalar field has no container to hang an overflow off, so preserving the raw text would mean a new
per-object store keyed by the bound instance, which is a whole mechanism for zero current consumers;
and a typed sentinel is more useful at a call site than an omitted key. Anyone claiming round-trip
fidelity for `@Fallback` would be overclaiming, and this entry does not.

**The map-key path: do not accept it, because overflow already exists there.** This is where the
collapse is not merely lossy but **N-1 lossy per field**, and where a fabricated key gets written back.
The fixture proves it at a live site. `HeartOfTheMountain.powder` is
`@Capture(filter = "^powder_")` over `ConcurrentMap<Powder.Type, Powder>`, and after the `^powder_`
strip and affix grouping the fixture yields six groups - `mithril`, `gemstone`, `glacite`, `buff`,
`ghast`, `ghast_1`. Three convert; three do not, because `Powder.Type` declares only
`MITHRIL`, `GEMSTONE`, `GLACITE` (`Powder.java`:17-23). So today:

| | In-memory | Written back |
| --- | --- | --- |
| Today | `{MITHRIL, GEMSTONE, GLACITE, null=<whichever of buff/ghast/ghast_1 was last>}` | `powder_null` - a key Hypixel never sent; two groups gone |
| With `@Fallback` | `{..., UNKNOWN=<whichever was last>}` | `powder_UNKNOWN` - still fabricated; still two groups gone |
| With overflow diversion | `{MITHRIL, GEMSTONE, GLACITE}` | `powder_buff`, `powder_ghast`, `powder_ghast_1` verbatim; nothing lost |

`serializeMapKey` (`CaptureTypeAdapterFactory.java`:584-595) is what produces `powder_null` today - it
returns the literal string `"null"` for a null key at `:585-586`, and `literalPrefix` is re-applied at
`:228-230`. `@Capture` overflow, by contrast, stores the **original unstripped** key (`:338`, `:358`)
and merges it back into the root verbatim (`:239-249`), which is exactly the fidelity the fabricated
key destroys.

So on the key path `@Fallback` buys one thing only - the poisoned entry is typed and iterable instead
of being a `null` key that breaks `keySet()` stream pipelines far from the decode. That was the pack's
argument in §7.6 and it is a real improvement over `null`. It is simply a much smaller improvement than
diverting to overflow, which costs a comparable edit and loses nothing. §10 hands that decision to the
`@Capture` entry.

One asymmetry to carry over: entry-mode `@Capture` fields can be diverted at classify time, because
`isCompatibleCaptureEntry` (`:484-502`) already runs there. Grouping-mode fields cannot - `:332-334`
and `:355` skip the compatibility check entirely, so a grouped site's diversion has to happen inside
`buildGroupedMap` at the `:474` conversion. That is feasible without restructuring: `read`'s post-assign
loop builds the map at `:377`/`:379` and only reads `overflowMaps` afterwards at `:384-387`, so
`buildGroupedMap` can still add to it. Both grouped enum-keyed sites - `TrophyFishing.fish` and
`HeartOfTheMountain.powder` - need that path.

## 10. Interaction with the other four entries in this cycle

`@Fallback` adds no factory and no store, so it collides with nothing structurally. It does change the
meaning of one expression that two other factories already depend on, and that is a hard sequencing
constraint rather than a soft one.

**The shared expression.** Both `@Capture` and `@Lenient` decide "is this entry compatible with the
declared generics" partly by reading it as an enum and testing the result against `null`:

| Factory | Site | Today's meaning of a non-null result |
| --- | --- | --- |
| `@Capture` key | `CaptureTypeAdapterFactory.java`:490-494 | conversion did not **throw** - a `null` result still counts as compatible |
| `@Capture` value | `CaptureTypeAdapterFactory.java`:538-541 | `result != null` - an unrecognized enum **value** is diverted to overflow |
| `@Lenient` key | `LenientTypeAdapterFactory.java`:258-264 | did not throw - a `null` result still counts as compatible |
| `@Lenient` value | `LenientTypeAdapterFactory.java`:309-312 | `result != null` - an unrecognized enum **value** is diverted to overflow |

Rows 2 and 4 are the invisible regression. Today an enum-**valued** `@Capture` or `@Lenient` map keeps
an unrecognized value in overflow and round-trips it. Once `@Fallback` makes the read return a
constant, `result != null` becomes true, the entry is judged compatible, and it binds onto the fallback
instead - **silently turning a lossless behaviour into a lossy one**. No adoption site in this module
has an enum-valued `@Capture` or `@Lenient` map today (the seventeen `@Capture` value types are
`Slot`, `Integer`, `TierData`, `Powder`, `Tier`, `Song`, `Node`, `FestivalCandy`,
`ConcurrentList<NbtContent>`, `ClaimedLevel`; the ten `@Lenient` fields carry `Integer`, `ArmorSet`,
`EquipmentSet` and `Loadout` values, plus the `Integer` element type of the one collection-shaped
field), so nothing here breaks - but every sibling module on the shared
pin inherits the change, and this is precisely the "visible only in a serialize test" failure the
verified facts warn about.

**The contract, and who owns each half.** `@Fallback` publishes `isFallback` (§4). The four sites above
consume it:

- Rows 2 and 4 add `&& !isFallback(gson, valueType, result)`, which restores today's behaviour exactly.
  This is a **required** companion edit, not an optional one.
- Rows 1 and 3 are where the `@Capture` and `@Lenient` entries own the unmatched-enum-key fix. The
  shape §9 argues for is "compatible if and only if the conversion neither throws, nor yields `null`,
  nor yields the fallback", which diverts an unknown key to overflow. Note that the `null` half of that
  is a fix in its own right and does not need `@Fallback` at all - it repairs `Dojo.points`,
  `Dojo.times`, `Kuudra.highestWave`, `Kuudra.completedTiers`, `TrophyFishing.fish`,
  `HeartOfTheMountain.powder` and the `@Lenient` `FloorData.completions` whether or not any enum is
  ever marked.

**Sequencing.** `@Fallback` and the four compatibility-check edits must ship in the **same** gson-extras
commit and the same publish. A sha that carries `@Fallback` without them is a sha on which any consumer
that marks an enum silently loses its enum-valued overflow. Since no consumer can mark an enum before
the annotation exists, shipping them together costs nothing and removes the window entirely.

**The other two entries.**

- **Shared overflow store, `@Extract` in its own factory.** No interaction with the store itself.
  `@Extract`'s post-assign bind (`LenientTypeAdapterFactory.java`:244) is a fresh top-of-chain
  `gson.fromJson`, so an `@Extract` target that is an enum or an `Optional<enum>` picks the fallback up
  for free once the enum is marked. None of the six current sites is enum-typed. The one thing the
  `@Extract` entry must not do is start treating a `null` conversion result as a failure - the empty
  catch at `:246-247` fires on a **thrown** conversion, and a `null` enum today assigns cleanly.
- **`@Extract` filter element.** No interaction. A typed remainder lands through the same fresh
  `gson.fromJson`, so it inherits the fallback behaviour with no coordination.
- **`@Flatten`.** No interaction beyond sharing a publish. If a flattened scalar is enum-typed it
  routes through the same leaf adapter, which is the intended reach.
- **`@Owner` / `@Parent`.** Deferred by the owner until after the research pack lands; nothing in this
  entry depends on it or constrains it.

**One consumer-side interaction worth naming.** `@Fallback` is what makes the §6.3 diversion upgrade
work: putting `@Lenient` on `Dungeons.classMap` only helps if `isCompatibleMapEntry` treats a
fallback-resolved key as incompatible. So that upgrade depends on the row 3 edit landing, not just on
the marker - which is a reason to ship the guards together, not a reason to gate marking
`DungeonClass.Type` on annotating `classMap`.

## 11. Failure-mode table

| Situation | Behaviour |
| --- | --- |
| Value matches a constant name, any case | unchanged - the constant (`:63`, `:82`) |
| Value matches a `@SerializedName` value or `alternate`, any case | unchanged - the constant (`:57`, `:62`) |
| Value matches nothing, enum has a marked constant | the marked constant |
| Value matches nothing, enum has no marked constant | `null`, exactly as today |
| JSON null | `null`, unchanged - `:77-80` returns before the lookup, so absence stays distinguishable from an unrecognized value |
| Two constants marked | `JsonException` at adapter construction, which is the enum's first decode. Swallowed if the enum is in a `withPrewarmTypes` list (`GsonSettings.java`:197-200) and rethrown at first real use |
| Marker on a non-enum field | inert, silently. §8 argues for leaving it inert |
| `Optional<E>` field, unmatched value | `Optional.of(fallback)` instead of `Optional.empty()` - `OptionalTypeAdapterFactory.java`:54 is `Optional.ofNullable(adapter.read(in))`, so `isPresent()` flips from `false` to `true`. One site: `JacobsContest.java`:113, and this row is the reason `JacobsContest.Medal` is a "do not mark" (§7) |
| `@Split` half, unmatched value | the fallback constant rather than whatever `PairOptional.of` does with a `null` half today. `SplitTypeAdapterFactory.java`:153-159 is wrapped in the empty catch at `:160-161`, so today's behaviour there is unobservable either way. One site: `TrophyFishing.lastCaught` |
| Enum-keyed `@Capture` map, several unmatched keys | **all collapse onto the fallback constant; later entries overwrite earlier ones**, and the write path fabricates one key named after the constant (§9) |
| Enum-**valued** `@Capture` or `@Lenient` map, unmatched value | **regression unless the row 2 / row 4 guard of §10 lands with it** - the entry stops going to overflow and binds onto the fallback |
| Enum-keyed **plain** map (no `@Capture`, no `@Lenient`), one unmatched key | `{UNKNOWN=v}` instead of `{null=v}` - typed rather than a `null` key, and still not what the wire said. A small improvement, not a fix. §6.2 marking B |
| Enum-keyed plain map, two or more unmatched keys | `JsonSyntaxException` - unchanged, this already throws today, because two `put` calls under `null` already collide |
| Enum-keyed plain map, marked constant has a wire representation | `JsonSyntaxException: duplicate key` from gson `MapTypeAdapterFactory.java`:203, aborting the whole decode. **A document that decodes today stops decoding** - and this is a rule-1 violation, not a property of the map path. §6.2 marking A |
| Enum-keyed plain map, serialized back | the fabricated key changes from `"null"` to `"UNKNOWN"`. Neither round-trips |
| The enum is under-modelled rather than under-defaulted | the marker converts a detectable `null` into an undetectable wrong value. §6.3's modelling rule, and `RabbitSort` is the live instance |

Read the table as one risk profile. The design is a clean win in row 3, a clean no-op in row 4, and a
small win on the plain-map rows. The genuinely bad rows are exactly two: an enum-valued `@Capture` or
`@Lenient` map without the §10 guards, which is why those guards are not optional, and a marked
constant that the wire can name, which is rule 1. Both are governed by the adoption list and by the
commit contents rather than by anything in the library. **The safety of this design lives in the
adoption list, not in the code.** That is unusual and worth saying out loud: the eight-line factory
edit is the easy part, and a future contributor marking a constant that Hypixel happens to send
reintroduces the whole hazard with no compile error and no test failure unless one is written for it
(§12).

## 12. Test plan and regression anchors

Baseline to hold: gson-extras **134/134**, hypixel **16/16**.

**New library tests**, as a `FallbackTests` nest in `GsonFactoryTest` alongside the existing
`CaptureTests` / `CombinationTests` nests. Two model enums are enough - one marked, one not.

| Test | Asserts |
| --- | --- |
| `unmarkedEnum_unmatchedValue_null` | the §8 guarantee - an unmarked enum still reads `null`. This is the test that makes the opt-in claim checkable |
| `markedEnum_unmatchedValue_fallback` | the core behaviour |
| `markedEnum_jsonNull_null` | `:77-80` still short-circuits; absence stays distinguishable |
| `markedEnum_exactAndCaseInsensitiveMatch_wins` | the fallback never shadows a real match |
| `markedEnum_serializedNameAlternate_wins` | `:56-57` alternates still beat the fallback |
| `markedEnum_write_usesOwnName` | serializing the fallback emits its own name, not the original wire value - pins the §9 loss as intended |
| `twoMarkedConstants_throwsOnFirstDecode` | the `JsonException` path, message included |
| `markedEnumValue_captureOverflowPreserved` | the §10 row 2 guard - an enum-**valued** `@Capture` map still overflows an unrecognized value |
| `markedEnumValue_lenientOverflowPreserved` | the §10 row 4 guard, same for `@Lenient` |
| `markedEnumKey_plainMap_collapses` | pins §6.2 marking B as **known** behaviour so nobody discovers it in production. Assert both sub-cases: one unmatched key yields `{UNKNOWN=v}`, and two yield `JsonSyntaxException` exactly as an unmarked enum does |
| `nameableFallbackConstant_plainMap_throws` | pins §6.2 marking A - the rule-1 violation, asserted as the thing rule 1 exists to prevent rather than as a property of the map path |
| `isFallback_unmarkedEnum_false` | the published query degrades correctly |

Rows 8, 9 and 10 are the ones that would not exist if this entry had been written from the pack alone,
and they are the ones that matter.

**Existing library anchors to re-run in full**, not selectively: the whole `GsonFactoryTest` nest,
because the change is inside the adapter every other test's models reach. Specifically
`CaptureTests.filterWithEnumKey_ok` (`:738-756`) and `CaptureTests.bareEntryGroupingWithEnumKey_ok`
(`:966-985`) are the two that already exercise enum keys through `@Capture`; `CombinationTests` covers
the nesting; `CaptureGroupingModeTest` and `CollectionValueCompatibilityTest` are the newest and least
settled behaviour.

**New consumer tests**, in `MemberDtoMappingTest`. The pack's condition 2 names
`{"selected_dungeon_class": "necromancer"}` into `Dungeons`, and that test stands as written -
`DungeonClass.Type` is group A, so it is markable in the first step. Add
`{"selected_faction": "cultists"}` into `CrimsonIsle` asserting `Faction.NONE` as a second, since
`CrimsonIsle.Faction` has no map exposure at all and isolates the field-value path. Write both before
the library change; both fail today with `null`, which is the confirmation the finding is real.

Three more, all of which should be written **red** because they fail against the current fixture and
none of them is fixed by `@Fallback`:

- `HeartOfTheMountain.powder` `keySet()` contains no `null`. Fails today - `powder_buff`,
  `powder_ghast` and `powder_ghast_1` all collapse onto one `null` key. Fixed by the `@Capture`
  overflow diversion.
- `Dojo.points` has seven entries. Fails today with one, because the fixture keys are the enum's
  `internalName` values (`dojo_points_mob_kb`, `dojo_points_wall_jump`, ...) and `Dojo.Type` carries
  them as a constructor component rather than as `@SerializedName`. Fixed by seven `@SerializedName`
  annotations, zero library cost. `@Fallback` would turn `{null=X}` into `{UNKNOWN=X}` and change
  nothing that matters.
- `ChocolateFactory.rabbitSort` is non-null. Fails today - the fixture carries `rarity_high_low` and
  `RabbitSort` declares `highest_rarity` / `lowest_rarity`. Fixed by correcting the `@SerializedName`.

**One structural guard worth building, and it guards rule 1 rather than the map path.** §11 concludes
that the safety of this design lives in the adoption list. The rule that has to hold is that a marked
constant is not nameable from the wire, and that is testable in the consumer: walk every enum under
`response/`, find the marked constant, and fail if it carries a `@SerializedName` or an `alternate` at
all, or if its `name()` appears - case-insensitively - anywhere in the fixture's value vocabulary for a
field of that type. Roughly thirty lines, no new dependency, and it turns "a future contributor marks
`Kuudra.Tier.BASIC`" from a production incident into a red test. An earlier draft proposed a different
guard, scanning for enum-keyed plain maps; §6.2 withdrew the rule that guard enforced, so it is
replaced rather than kept. Nothing in the library can enforce either one, because the library cannot
see the consumer's fields or its fixture.

## 13. Verdict and conditions

**Adopt, narrowed further than the pack narrowed it.** The enum-constant marker is the right shape and
the pack's reasoning for choosing it over a field-level annotation and over a naming convention holds.
The library edit is small, opt-in, and provably inert for an unmarked enum. What does not hold is the
claim that one marker fixes both the field-value path and the map-key path: it fixes the first, and on
the second it is a strictly weaker answer than the overflow diversion that the `@Capture` and
`@Lenient` entries can implement anyway.

Being adversarial about the entry as a whole: this is riskier than the pack's §7 reads, and the risk is
not in the code. Eight lines in a leaf adapter is genuinely near-zero risk. The risk is that the
annotation, once it exists, is trivially applicable to any enum, reads as obviously beneficial, and is
actively harmful whenever the marked constant is a value the wire can name - a property of the upstream
API's vocabulary, not of the Java, so it produces no compile error, no test failure and no log line.
An annotation whose correct use depends on what a third party sends is a sharp tool. It is still worth
having, because the fourteen bind sites it repairs are real `correctness` defects and there is no stock
equivalent. But it should ship with the §12 structural guard, not just with a paragraph in a design
note.

One thing this section deliberately no longer says: an earlier draft called the annotation "actively
harmful on about forty fields", meaning every enum-keyed plain map in the module. §6.2 withdrew that.
Those forty fields are unaffected unless rule 1 is violated, and the correction makes the entry's
adoption list larger and its prohibition list shorter, not the reverse.

Six conditions on the acceptance.

1. **Ship the four compatibility-check guards in the same commit and the same publish** (§10). A sha
   carrying `@Fallback` without them silently converts enum-valued overflow from lossless to lossy for
   every consumer on the shared pin.
2. **Land the consumer-side naming fixes first, before any marker.** `f06-boardquest-complete-status`
   (`COMPLETE`, plus the unmodelled `INACTIVE`), `f06-serialized-name-misses`, the seven `Dojo.Type`
   internal names, and `RabbitSort`'s `rarity_high_low` are all `trivial`, consumer-only, and are real
   data loss today. Every one of them is a defect `@Fallback` would **mask** rather than fix. They must
   not wait behind a JitPack cycle.
3. **Obey the §6.3 eligibility rule, and encode it as a test** (§12). No marker on a constant that has
   a wire representation, checked case-insensitively across `name()`, `@SerializedName.value` and every
   `alternate`. `Kuudra.Tier` gets a new `UNKNOWN`, not `BASIC`. There is no second mechanical rule -
   an enum that keys a plain map is not disqualified.
4. **Defer `Rarity` and `GameMode` to a separate step.** They are in `Simplified-Api/skyblock`, which
   pins its own gson-extras sha, so they cost a second chained publish cycle for three field sites. The
   first step is the twelve in-module enums.
5. **Write the confirming test before the library change**, on a group A enum. Both
   `{"selected_dungeon_class": "necromancer"}` into `Dungeons` and `{"selected_faction": "cultists"}`
   into `CrimsonIsle` qualify; the second isolates the field-value path because `CrimsonIsle.Faction`
   has no map exposure at all.
6. **Grep the sibling modules for code that relies on a `null` enum before landing.** The opt-in design
   means only marked enums change behaviour and all marked enums are in this module, so this is cheap
   insurance rather than a real risk - but `Rarity` in particular is consumed across the whole
   `dev.sbs` family and the condition becomes load-bearing the moment step 4 is scheduled.

**Sequencing summary.** Consumer naming fixes (no library cost) - then one gson-extras commit carrying
`Fallback.java`, the four-hunk `CaseInsensitiveEnumTypeAdapterFactory` edit, and the four compatibility
guards - then one publish and one re-pin - then the twelve in-module enum edits plus the structural
guard test - then, separately, the skyblock cycle for `Rarity` and `GameMode`.
