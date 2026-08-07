# 00 - Verified facts

Read-only fact base for the `gson-extras` design cycle. Every claim here was read off current source
at the shas below, not recalled. Four design entries in `10-design-entries.md` rely on this document
instead of re-reading the library.

Sources read for this document:

- `W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras/src/main/java/dev/simplified/gson/` - all
  eight factories, all seven annotations, `GsonSettings`, `PostInit`
- `W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras/src/test/java/dev/simplified/gson/GsonFactoryTest.java`
- gson `2.11.0` sources (`gradle/libs.versions.toml`:4) - `GsonBuilder.create()`, `Gson` constructor,
  `Gson.getDelegateAdapter`
- `W:/Workspace/Java/Simplified/Simplified-Api/hypixel/src/main/java/` - every adoption site

## 1. Corrections - read this first

Six claims in the brief that seeded this cycle are wrong. Three of them change the design.

**C1 - THE NESTING DIRECTION IS INVERTED. `@Capture` wraps `@Lenient`, not the reverse.**
The brief asserted "Earlier registration = OUTER adapter. So Lenient wraps Capture." That is backwards.
`GsonBuilder.create()` **reverses** the user factory list before constructing `Gson`
(gson 2.11.0 `GsonBuilder.java`:887-890), so the **last** registered factory is consulted **first** and
is therefore the outermost wrapper. §2.1 derives this from source. The library's own javadoc already
says so correctly (`GsonSettings.java`:215-218 - "Gson checks last-registered first, so the outermost
wrapper is registered last"), and so does the research pack
(`10-annotation-designs.md` §6.5:531, which prints the correct chain). The brief is the only place the
inversion appears. **Designers must not carry it forward.**

**C2 - `@Capture`'s overflow content DOES exist when `@Extract` runs. It is unreachable, not absent.**
Follows from C1. Because `Capture` is outer, `CaptureTypeAdapter.read` has finished classifying every
root entry into `capturedJsonMaps` and `overflowMaps` (`:270-363`) **before** it calls
`delegateAdapter.fromJsonTree(knownObject)` at `:366`, and that delegate call is what reaches
`LenientTypeAdapter.read` and its `@Extract` phase. So the brief's "@Capture's overflow does not exist
yet" is false. What is true is narrower and cheaper to fix: `overflowMaps` is a **frame-local**
`ConcurrentMap<String, JsonObject>` in `Capture.read`, and the static store cannot hold it yet because
the store is keyed by the built `Map` instance, which is not constructed until `:379`/`:387`, after the
delegate returns.

**C3 - This is NOT a factory-ordering problem, so a reorder is NOT the fix.**
The brief's "CONSEQUENCE 3: merging the two static stores alone fixes NOTHING. This is an ordering
problem." is wrong on the diagnosis. The order is already the favourable one. It is a **publication
and lifetime** problem: producer-side data is frame-local, and the identity key that the store wants
does not exist during the window in which the consumer runs. The fix shape that follows is a
read-scoped channel (a per-read frame the outer factory publishes into and the inner factory reads
from), not a `GsonSettings` list edit. A `GsonSettings` reorder is rated `xlarge` by
`00-conventions.md` §4; a read-scoped channel is an additive file plus two factory edits. **The
correction takes an `xlarge` off the table.**

**C4 - There are six `@Extract` adoption sites, not three.**
The brief listed `Bestiary.lastKilledMob`, `ChocolateFactory.eggs`, `ChocolateFactory.locations`.
The real set is six (§10.1), and two of the brief's three names are wrong - the `ChocolateFactory`
fields are `@Extract("rabbits.collected_eggs")` and `@Extract("rabbits.collected_locations")`, both
sourced from a `rabbits` field, not from `eggs`/`locations`. The regression surface for lifting
`@Extract` into its own factory is **twice** what the brief implied. All six are still
`@Lenient`-sourced, so that part of the brief holds.

**C5 - There are five empty catch blocks of the "silent swallow" class, not three.**
The brief named three. §9 lists five (`ex`-named, zero-statement bodies) plus one `ignored`-named
zero-statement body, and separates them from nine deliberate narrow catches that have bodies or are
scoped to `NoSuchFieldException`. The two the brief missed are
`CaptureTypeAdapterFactory.java`:477-478 (`buildGroupedMap`, the twin of the `buildSimpleMap` one it
did name) and `SplitTypeAdapterFactory.java`:160-161.

**C6 - The unmatched-enum-key defect has six `@Capture` sites, not seven, and `Statistics` is not one
of them.** The pack's site list (`10-annotation-designs.md` §9.1:1144-1146) cites
`Statistics.java`:89. Line 89 is `Statistics.Mythos.completedChains`, a plain
`ConcurrentMap<Type, Integer>` with `@SerializedName` and **no `@Capture`** - it binds through gson's
stock `MapTypeAdapterFactory`, not through `CaptureTypeAdapterFactory`. The only `@Capture` field in
`Statistics.java` is `:265` and its key type is `String`. The defect is real at that site but it is a
**different code path**, so a `@Capture` element cannot reach it and an enum-side fix can. §10.4 gives
the corrected list. This does not change the pack's §9 verdict; it strengthens it.

Everything else in the brief was checked and holds: the two static stores and their line numbers, the
differing merge-back targets, `@Extract` addressing by Java field name, `@Extract` removing on read and
re-injecting on write, the catch-all/regex selection model, `Grouping.ENTRY` with no `GROUPED`
constant, and "no single field carries both `@Lenient` and `@Capture`".

## 2. Factory registration and the resulting adapter nesting

### 2.1 The registration-to-nesting derivation, from gson source

Four hops, each read off source. Do not shortcut this - it is the fact the brief got backwards.

1. `GsonSettings.defaults()` (`:248-257`) seeds the list in this order:
   `CaseInsensitiveEnum`, `Optional`, `Split`, `SerializedPath`, `Lenient`, `Capture`, `Collapse`,
   `PostInit`. `Builder.withFactories` **appends** (`:410-413`, `:420-423`), and `defaults()` then
   appends SPI factories (`:259`) and whatever `GsonContributor`s add (`:261-263`).
2. `GsonSettings.create()` (`:149`) replays that list in order into
   `GsonBuilder.registerTypeAdapterFactory`, which **appends** (gson `GsonBuilder.java`:751-755).
3. `GsonBuilder.create()` copies the list and calls `Collections.reverse(factories)` on it
   (gson `GsonBuilder.java`:887-890) before handing it to the `Gson` constructor.
4. The `Gson` constructor splices that reversed list in at `Gson.java`:333, between the excluder
   (`:330`) and the stock platform factories (`:336`+), with `ReflectiveTypeAdapterFactory` last.
   `Gson.getAdapter` walks the list front-to-back and takes the first non-null;
   `Gson.getDelegateAdapter(skipPast, type)` (`Gson.java`:730-759) walks the same list, discards
   everything up to and including `skipPast`, then takes the first non-null after it.

Net: **position in the final list = outerness**, and the reverse at step 3 means **last registered is
outermost**. Registration index and nesting depth run in opposite directions.

### 2.2 The nesting diagram and what tree each factory sees

For a POJO `C`, `getAdapter(C)` resolves to this chain. Outermost first; each arrow is a
`getDelegateAdapter` hop. `String`/`Color`/`Instant`/`OffsetDateTime`/`UUID` adapters registered via
`registerTypeAdapter` sit outside all of this because `GsonSettings.create()` registers them after the
factories (`:150-151`) and the same reverse applies.

```
  [contributor factories]        (reversed; none registered by the hypixel module)
      |
  [SPI factories]               (reversed; collections ships ConcurrentTypeAdapterFactory here -
      |                          returns null for a POJO, so no wrap)
  PostInit                       wraps only if C implements PostInit
      |
  Collapse                       wraps only if C has an @Collapse field
      |
  Capture                        wraps only if C has a @Capture Map field
      |
  Lenient                        ALWAYS in the chain (see 2.3); wraps only if C has @Lenient/@Extract
      |
  SerializedPath                 ALWAYS in the chain (see 2.3); wraps only if C has @SerializedPath
      |
  Split                          wraps only if C has a @Split Pair/PairOptional field
      |
  Optional / CaseInsensitiveEnum return null for a POJO; they claim Optional<?> and enum types
      |
  ... stock gson platform factories ...
      |
  ReflectiveTypeAdapterFactory   the innermost, actually binds fields
```

What each one sees, on read:

| Factory | Tree handed to it | Tree it hands down |
| --- | --- | --- |
| `PostInit` | whatever is on the wire | unchanged - it is a pure `read`-then-`postInit()` pass-through |
| `Collapse` | full root object | root with each `@Collapse` list-mode field's JSON object rewritten to a JSON array (`:182-190`); map-mode fields untouched |
| `Capture` | full root object | **`knownObject` only** - a freshly built `JsonObject` holding declared-field keys plus any unmatched key when no catch-all exists (`:264`, `:315-318`, `:359-362`). Every captured and every overflowed key is **absent** |
| `Lenient` | whatever `Capture` handed down | same root object, mutated in place: each `@Lenient` field's element replaced by the type-compatible subset (`:183`, `:197`), and each claimed `@Extract` key **removed** from the local overflow (`:216`) |
| `SerializedPath` | whatever `Lenient` handed down | **unchanged** (`:103` passes `outerJsonElement` straight through); it assigns its fields afterwards with a fresh top-of-chain `gson.fromJson` (`:132`) |
| `Split` | unchanged root | root with each `@Split` field's key **removed** (`:133`) |
| Reflective | that | binds |

Three consequences worth stating on their own:

- A `@Lenient` field's own serialized name is a **known key** to `Capture` (`discoverKnownKeys`
  `:114-143` walks every non-`@Capture` non-transient field), so a `@Lenient` field always survives
  into `knownObject` and reaches `Lenient` intact. The two annotations compose today, and
  `GsonFactoryTest.lenientWithCapture_ok` (`:2182-2208`) and
  `GsonFactoryTest.lenientExtractCapture_ok` (`:2226-2253`) prove it.
- An `@Extract` field's Java name is likewise registered as a known key. Harmless (no JSON carries it)
  but it means `Capture`'s catch-all will never claim a key colliding with an `@Extract` field name.
- `SerializedPath` does not thread its fields through the enclosing class's delegate chain at all. A
  `@SerializedPath` field binds through a **fresh** `gson.fromJson` at the top of the chain. Anything
  that depends on being inside the enclosing object's chain is invisible to it.

### 2.3 Two factories that never return null

`LenientTypeAdapterFactory.create` (`:70-72`) and `SerializedPathTypeAdaptorFactory.create` (`:39-41`)
return `delegateAdapter` when they have no work, rather than `null`. Every other factory in the
library returns `null`. Two consequences:

- Both occupy a slot in the search for **every type**, so `create` (and its `Reflection<>` field scan)
  runs once per `TypeToken` for every type gson ever resolves. Gson caches the resulting adapter, so
  the scan is once-per-type, not per-document.
- Because they hand back the delegate by identity, no wrapper object and no extra `JsonElement`
  round-trip is created when they have no work. There is no runtime cost, only the one-off scan.

A new factory added for this cycle should return `null` when idle, matching the majority convention -
`ExtractTypeAdapterFactory` in particular, since it will be asked about every type.

## 3. Call-order trace - `LenientTypeAdapterFactory`

### 3.1 read

`LenientTypeAdapter.read`, `:153-251`. Line numbers are exact.

| Step | Lines | What happens |
| --- | --- | --- |
| R1 | `:154` | Buffers the whole incoming stream to a `JsonElement` via `jsonElementAdapter` |
| R2 | `:156-157` | Non-object root short-circuits straight to `delegateAdapter.fromJsonTree`. **No `@Lenient`, no `@Extract`, no overflow for a non-object root** |
| R3 | `:162` | Allocates `ConcurrentList<FieldOverflow> overflows` - a **method-frame local**, keyed by Java field name |
| R4 | `:165-200` | FILTER PHASE, per `@Lenient` field. `locateElement` (`:166`, impl `:339-354`) resolves by `@SerializedPath` segments if present, else by `@SerializedName`/field name |
| R4a | `:171-184` | Map branch. Splits `original` into `filtered` and `overflow` by `isCompatibleMapEntry` (`:253-271`), `replaceElement` writes `filtered` back into the tree (`:183`), and the overflow `JsonObject` is appended to the local list (`:184`) |
| R4b | `:185-199` | Collection branch, same shape, `JsonArray` throughout, gated on `isCompatibleElement` (`:273-327`) |
| R4c | - | A field whose declared shape and JSON shape disagree (map field / array JSON, or the reverse) matches **neither** branch: no filtering, no overflow entry, tree untouched. The delegate then sees the raw value |
| R5 | `:203-221` | EXTRACT PHASE. For each `@Extract` field, finds the `FieldOverflow` whose `fieldName` equals `extractInfo.sourceFieldName` **in the local list from R3** (`:206-209`). No match - `continue`, silently (`:211-212`) |
| R5a | `:216` | `overflowObj.remove(jsonKey)` - **destructive**. The claimed entry leaves the overflow and is parked in a local `ExtractClaim` |
| R5b | `:214` | Only a `JsonObject` overflow is searched. An `@Extract` naming a **collection**-typed `@Lenient` source silently no-ops |
| R6 | `:224` | `delegateAdapter.fromJsonTree(rootObject)` - this is where `SerializedPath`, then `Split`, then the reflective binder run |
| R7 | `:226-227` | Null result short-circuits. **Overflow is discarded and never published** |
| R8 | `:230-240` | POST-ASSIGN OVERFLOW. Reads each `@Lenient` field's now-bound collection instance off `value` and does `OVERFLOW.put(collection, fieldOverflow.overflow())` (`:239`). Note `put` is unconditional - an **empty** overflow is stored too (contrast `Capture` `:386`) |
| R9 | `:242-248` | POST-ASSIGN EXTRACT. `gson.fromJson(claim.element(), accessor.getGenericType())` then `accessor.set`. Wrapped in the empty catch at `:246-247` |

Three facts that fall out of this trace:

- **`@Extract` never consults `OVERFLOW` on read.** It reads the R3 frame-local. Confirmed. Merging
  the two static stores changes nothing about the read path.
- **`@Extract` runs before the delegate**, so the extracted value is set on an object the reflective
  binder has already populated - R9 is a post-bind reflective `set` and will overwrite whatever the
  binder put there.
- `LenientFieldInfo.of` (`:428-452`) **skips any field carrying `@Capture`** (`:437-438`) and any field
  whose generic type is not a `ParameterizedType` (`:444-445`). `ExtractFieldInfo.of` (`:479-494`) has
  no such filters beyond `transient`. So a class with `@Extract` but no `@Lenient` still builds a
  `LenientTypeAdapter` (`:70`, `extractFields` non-empty) whose extract phase can never match anything.

### 3.2 write

`LenientTypeAdapter.write`, `:86-150`.

| Step | Lines | What happens |
| --- | --- | --- |
| W1 | `:92` | `delegateAdapter.toJsonTree(value)` produces the flat tree |
| W1a | `:92` | That tree **already contains the `@Extract` field's own serialized key**. The reflective binder serializes every non-transient field, and `@Extract` fields are not transient - `ExtractFieldInfo.of` (`:479-494`) skips transient ones, so no site can be. **Nothing in this factory removes that key**; contrast `CaptureTypeAdapterFactory.java`:181, which removes its own field's serialized key before merging |
| W2 | `:94`, `:148-149` | Non-object tree bypasses everything and writes through the delegate |
| W3 | `:98-118` | RE-INJECT `@Extract`. For each non-null `@Extract` value, finds the `@Lenient` field whose Java name matches `sourceFieldName` (`:104`), takes that field's live collection instance, and `OVERFLOW.computeIfAbsent(collection, ...)` (`:108`) creating a `JsonObject` for a map source or a `JsonArray` for a collection source |
| W3a | `:110-111` | Only adds when the overflow is a `JsonObject`. A collection-sourced `@Extract` value is **created-then-dropped**, and `computeIfAbsent` has by then installed an empty `JsonArray` into the static store as a side effect |
| W4 | `:121-145` | MERGE-BACK. For each `@Lenient` field: `OVERFLOW.get(collection)` (`:127`), `locateElement(jsonObject, lenientInfo)` (`:132`) to find **the field's own sub-object**, then copy every overflow entry into it (`:137-138` object, `:142-143` array) |
| W4a | `:134`, `:139` | Shape must agree on both sides - object-into-object or array-into-array. A mismatch is silently skipped |
| W5 | `:147` | Writes the merged tree |

`@Extract` is therefore **not a pure reader**. It removes on read (R5a) and re-injects on write (W3),
and the re-injection target is defined by the `@Lenient` source field's own sub-object (W4). Any design
that lets `@Extract` claim from a different producer must carry the producer's merge-back target with
the entry - a plain union of the two stores would re-inject a `Capture`-sourced key into a `@Lenient`
field's sub-object, which is the wrong place (§6).

**W1a is a live defect and `@Extract` does not round-trip today.** Re-injection at W3 puts the value
back where it came from, but the flat tree from W1 still carries the field's own key, so a
read-then-write **emits the value twice**: once at the root under the Java field name (or its
`@SerializedName`), once inside the source. A round-trip of `GsonFactoryTest.FullCombinationModel`
emits `kills.last_killed_mob` **and** a root-level `"lastKilledMob"`; `ChocolateFactory` emits `"eggs"`
and `"locations"` at the root as well as inside `rabbits`; `Loadouts` emits `"equippedArmorSet"` at the
root as well as inside `armor` whenever the `Optional` is present (an empty one writes a null that
gson drops, since `GsonSettings` only calls `serializeNulls()` when asked, `:154`, and hypixel
registers no `ExclusionStrategy`). This has never been observed because the whole write path is
untested (`04-compatibility.md` §6.3 G1-G5, and §11 C4 here) and the one `@Extract` test,
`GsonFactoryTest`:2227, only deserializes. Treat any
statement in the design pack about `@Extract` "round-tripping" as a statement about the **overflow
entry**, not about the emitted document.

## 4. Call-order trace - `CaptureTypeAdapterFactory`

### 4.1 read

`CaptureTypeAdapter.read`, `:257-391`.

| Step | Lines | What happens |
| --- | --- | --- |
| R1 | `:258` | Buffers the stream to a `JsonElement` |
| R2 | `:260-261` | Non-object root short-circuits to the delegate |
| R3 | `:264` | Allocates `knownObject` - a **new empty** `JsonObject`. Nothing survives to the delegate unless it is explicitly copied in |
| R4 | `:267-273` | Allocates `capturedJsonMaps` and `overflowMaps`, both `ConcurrentMap<String, JsonObject>` keyed by Java field name. Both are **method-frame locals**. One empty `JsonObject` is pre-seeded per `@Capture` field |
| R5 | `:276-308` | DESCEND PRE-PASS, for `descend = true` fields only. `rootObject.remove(serializedName)` (`:280`) pulls the nested object **out of the root entirely**, then classifies its entries into the same two locals. A missing or non-object nested value is skipped after the remove (`:282-283`) |
| R6 | `:311-363` | CLASSIFY, one pass over the remaining root entries |
| R6a | `:315-318` | `knownKeys.contains(key)` - copy verbatim into `knownObject`, done |
| R6b | `:323-343` | Filtered `@Capture` fields, **first match wins** in declaration order (`:342` breaks). `pattern.matcher(key).find()` selects; `key.replaceFirst(filter, "")` strips (`:330`) |
| R6c | `:332-334` | Grouping mode stores the stripped key raw and **skips the compatibility check entirely** |
| R6d | `:335-339` | Entry mode gates on `isCompatibleCaptureEntry(strippedKey, value, info)`; incompatible entries go to `overflowMaps` under the **original unstripped key** (`:338`) |
| R6e | `:349-358` | Catch-all (first `@Capture` with an empty filter), same compatible/overflow split, original key throughout |
| R6f | `:359-362` | No filtered match and no catch-all - the key is copied into `knownObject` so the delegate still sees it |
| R7 | `:366` | `delegateAdapter.fromJsonTree(knownObject)` - **this is the call that reaches `Lenient`** |
| R8 | `:368-369` | Null result short-circuits. Captured maps and overflow are discarded |
| R9 | `:372-388` | POST-ASSIGN. Per field: `buildGroupedMap` (`:377`) or `buildSimpleMap` (`:379`) turns the captured `JsonObject` into a `Map`, `accessor.set(result, capturedMap)` (`:381`) **replaces the field's initialiser instance**, then `OVERFLOW.put(capturedMap, overflow)` **only when non-empty** (`:386-387`) |

Facts that fall out:

- The `Map` instance that keys the static store **does not exist** until `:379`/`:377`, which is after
  the delegate call at `:366`. That is the whole of the reachability problem (C2).
- `accessor.set` at `:381` discards the field initialiser, so any reference a caller held to the
  original `Concurrent.newMap()` is stale. The store is correctly keyed to the **new** instance.
- Grouping mode never runs `isCompatibleCaptureEntry`, so a grouping-mode `@Capture` **cannot produce
  overflow at all** from the classify pass - `overflowMaps` stays empty and `:386` never fires.
  Grouping-mode loss happens later and silently, inside `buildGroupedMap`.
- `buildSimpleMap` (`:393-406`) and `buildGroupedMap` (`:408-482`) each convert the key via
  `gson.fromJson(new JsonPrimitive(key), info.keyType())` (`:398`, `:474`) and each wrap the whole
  put in an empty catch (`:401-402`, `:477-478`).
- `buildGroupedMap`'s unmatched branch (`:448-468`) drops an entry **entirely and silently** when it
  matches no affix, there is no bare field, and its value is not a `JsonObject`.

### 4.2 write

`CaptureTypeAdapter.write`, `:159-254`.

| Step | Lines | What happens |
| --- | --- | --- |
| W1 | `:165` | `delegateAdapter.toJsonTree(value)` - for a class that also has `@Lenient`, this is the tree `Lenient.write` already merged and re-injected into |
| W2 | `:167-170` | Non-object tree bypasses |
| W3 | `:181` | Removes the `@Capture` field's own serialized name from the output - the delegate serialised the map as a nested object and that is not the wire shape |
| W4 | `:185` | Picks the target: a **fresh `JsonObject`** for `descend`, otherwise the **root object itself** |
| W5 | `:187-224` | Grouping mode - splits each value object's fields back out and reconstructs the original key by affix direction (`:198-216`), re-applying `literalPrefix` when the field has a filter (`:218-220`) |
| W6 | `:226-233` | Entry mode - one key per map entry, same `literalPrefix` re-application |
| W7 | `:235-236` | `descend` re-attaches the built object under the serialized name |
| W8 | `:239-250` | MERGE-BACK. `OVERFLOW.get(mapObj)` where `mapObj` is the **live field value**, then copy every overflow entry into `jsonObject` (the **root**) or into the descend sub-object (`:242-244`) |

The merge-back target difference against `@Lenient` is real and both are correct for their own
semantics: `@Lenient` overflow belongs inside the field's own sub-object because that is where the
entry came from; `@Capture` overflow belongs at the root (or the descend node) because that is where
its entries came from. `literalPrefix` (`:654`) is what makes a filtered `@Capture` round-trip - it is
the filter with a leading `^` and trailing `$` stripped, prepended back onto every written key.
Overflow keys bypass that reconstruction entirely because R6d/R6e stored them **unstripped**.

## 5. Interleaved trace - one read through both factories

The single sequence a designer needs. `C` has one `@Capture` field, one `@Lenient` field and one
`@Extract` field. Indentation is stack depth.

```
Capture.read                                              CaptureTypeAdapterFactory :257
  :258  buffer stream to rootElement
  :270  seed capturedJsonMaps / overflowMaps          <-- frame-locals, empty
  :276  descend pre-pass
  :311  classify every root key
          -> knownObject          (declared-field keys, incl. the @Lenient field's key)
          -> capturedJsonMaps     (claimed keys, stripped)
          -> overflowMaps         (type-incompatible claimed keys, unstripped)
  ###   CAPTURE OVERFLOW IS NOW FULLY COMPUTED, AND ONLY A LOCAL   ###
  :366  delegateAdapter.fromJsonTree(knownObject)
    |
    Lenient.read                                          LenientTypeAdapterFactory :153
      :162  allocate overflows                        <-- frame-local, empty
      :165  filter phase -> per-@Lenient-field overflow appended to overflows
      :203  extract phase -> searches ONLY overflows  <-- the reachability gap
      :224  delegateAdapter.fromJsonTree(rootObject)
        |
        SerializedPath.read :99 -> Split.read :121 -> Reflective binder
        |
      :230  OVERFLOW.put(lenientCollection, fieldOverflow)   [Lenient static store]
      :242  gson.fromJson(claim) + accessor.set              [@Extract assignment]
      :250  return value
    |
  :372  per @Capture field:
  :377/:379  build the Map instance                    <-- the identity key finally exists
  :381  accessor.set(result, capturedMap)
  :387  OVERFLOW.put(capturedMap, overflow)            [Capture static store]
  :390  return result
```

The gap is the six lines between `###` and `:203`. The producer's data is complete and the consumer is
about to run, but they are in different stack frames with no channel between them, and the key the
static store wants (`capturedMap`) is fourteen source lines from existing. Both static stores are
written **after** every read-side consumer has already run; on the read path they are write-only.

## 6. Where overflow is produced and consumed

Consolidated answer to "where is overflow produced and consumed, on both read and write".

| | `@Lenient` overflow | `@Capture` overflow |
| --- | --- | --- |
| Store | `LenientTypeAdapterFactory.java`:62 `WeakIdentityMap<Object, JsonElement>` | `CaptureTypeAdapterFactory.java`:82 `WeakIdentityMap<Object, JsonObject>` |
| Value shape | `JsonObject` for a map field, `JsonArray` for a collection field | always `JsonObject` |
| Identity key | the bound collection instance | the bound `Map` instance built at `:377`/`:379` |
| PRODUCED (read) | `:174`/`:188` populated in the filter phase; `:239` published to the store | `:272` seeded, `:300`/`:305`/`:338`/`:358` populated in classify; `:387` published |
| Published only if | always, even when empty (`:236-239`) | non-empty only (`:386`) |
| CONSUMED (read) | **frame-local only**, `:206-209`, by `@Extract`. The store is never read on the read path | **never**. Nothing reads a `@Capture` overflow on the read path |
| PRODUCED (write) | `:108` `computeIfAbsent` when an `@Extract` field re-injects | never written on the write path |
| CONSUMED (write) | `:127` `OVERFLOW.get(collection)`, merged at `:137-138` / `:142-143` | `:239` `OVERFLOW.get(mapObj)`, merged at `:247-248` |
| Merge target | the `@Lenient` field's own sub-object, `locateElement` `:132` | the root object, or the descend sub-object, `:242-244` |
| Key form stored | the original JSON key inside that sub-object | the **original unstripped** root key |

Cross-cutting facts:

- Both stores are `private static final` on their own factory class, so they are shared across every
  `Gson` instance in the JVM. Identity keying is what keeps that safe.
- Their key spaces are disjoint **today by construction**, not by design: no field carries both
  annotations, and `LenientFieldInfo.of` `:437-438` would exclude it from the `@Lenient` side if one
  did. Two annotations on one field therefore yields `@Capture` semantics and a silently dead
  `@Lenient`.
- Neither store is ever cleared, removed from, or iterated. The only mutators are `put` and
  `computeIfAbsent`; the only reader is `get`. `size()` exists but is used only by tests.
- A read that returns `null` from the delegate (`Lenient` `:226`, `Capture` `:368`) drops that read's
  overflow on the floor. Round-trip fidelity is already conditional on a non-null bind.

## 7. `WeakIdentityMap` - exact current surface

`dev/simplified/gson/factory/WeakIdentityMap.java`, 157 lines. **Package-private, not `public`**
(`:29` `final class WeakIdentityMap<K, V>`). A proposed `Overflow` type in the same
`dev.simplified.gson.factory` package can use it as-is; anywhere else needs a visibility bump, which
is an API-surface decision, not a refactor.

The complete operation set - there are four, and no more:

```java
@Nullable V get(@NotNull K key)                                              // :40
void put(@NotNull K key, @NotNull V value)                                   // :51
@NotNull V computeIfAbsent(@NotNull K key, @NotNull Supplier<? extends V>)   // :67
int size()                                                                   // :83
```

There is **no** `remove`, no `containsKey`, no `keySet`/`entrySet`/`values`, no iteration, no `clear`,
no `putIfAbsent`, no `getOrDefault`, no `merge`, no bulk constructor. Anything a design needs beyond
those four is new code.

Behaviour, read off source:

- Backed by `ConcurrentHashMap<IdentityKey<K>, V>` (`:32`) plus a `ReferenceQueue<K>` (`:31`).
- `IdentityKey` (`:111-155`) extends `WeakReference<K>`, caches `System.identityHashCode(referent)` at
  construction (`:133`), and `equals` compares referents by `==` (`:151-152`). A key whose referent has
  been cleared equals nothing, including itself by content.
- Two `IdentityKey` constructors: `:121` unregistered, for **lookups only**, and `:131` registered
  against the queue, for **stored** keys. `get` builds an unregistered one (`:42`); `put` and
  `computeIfAbsent` build registered ones (`:53`, `:74`).
- `sweep()` (`:96-104`) drains the queue, and if anything was drained does a **full scan**
  `entries.keySet().removeIf(key -> key.get() == null)`. It is called at the head of `get`, `put` and
  `size`, and transitively by `computeIfAbsent` through `get`. Cost is O(n) on the reads that follow a
  GC that cleared at least one key.
- Concurrency contract, stated in the class javadoc (`:24`): every operation is safe for concurrent
  use, but **only `put` is atomic**. `computeIfAbsent` may run the supplier more than once
  concurrently; `putIfAbsent` at `:74` guarantees only one produced value is stored and every caller
  is handed that one (`:75`).
- Values are `@NotNull`. There is no way to store a null, and `get` returning `null` unambiguously
  means "no entry".

Three notes for a design that builds on this:

- Storing a mutable value object (rather than the current bare `JsonElement`) needs no change here -
  `V` is unconstrained.
- A shared store keyed by a **read-scoped frame** rather than by a bound collection would not use this
  class at all; a `ThreadLocal` is the natural carrier for that, and the two are complementary, not
  alternatives. `WeakIdentityMap` is the right home for write-side merge-back state, which must outlive
  the read.
- Adding `remove` is the one gap a claim-and-consume design would hit, since `@Extract` claiming from a
  shared store needs the claimed entry gone from the producer's overflow (the `@Lenient` path achieves
  this today by mutating the `JsonObject` in place at `:216`, not by touching the store).

## 8. Annotation inventory

All seven live in `dev.simplified.gson.annotation`, all are `@Retention(RUNTIME)` and all are
`@Target(ElementType.FIELD)`. There is no type-level, method-level or parameter-level annotation in
the library. `PostInit` is an interface in `dev.simplified.gson`, not an annotation.

"Reg idx" is the position in the `GsonSettings.defaults()` list (`:249-256`), zero-based. **Higher
index = outer** (§2.1). "Nesting" gives the same information the way a designer thinks about it.

| Annotation | Elements | Implemented by | Reg idx | Nesting (1 = outermost) |
| --- | --- | --- | --- | --- |
| `@SerializedName` (stock gson) | `value`, `alternate[]` | `ReflectiveTypeAdapterFactory`, plus read by five of the eight factories | n/a | innermost |
| `@Split` | `@NotNull String value()` - literal delimiter | `SplitTypeAdapterFactory` | 2 | 6 |
| `@SerializedPath` | `@NotNull String value()` - dot path | `SerializedPathTypeAdaptorFactory` | 3 | 5 |
| `@Lenient` | **none** - marker | `LenientTypeAdapterFactory` | 4 | 4 |
| `@Extract` | `@NotNull String value()` - `"sourceField.jsonKey"` | `LenientTypeAdapterFactory` (same factory, separate field scan) | 4 | 4 |
| `@Capture` | `@Language("RegExp") @NotNull String filter() default ""`; `boolean descend() default false`; `@NotNull Grouping grouping() default AUTO` | `CaptureTypeAdapterFactory` | 5 | 3 |
| `@Collapse` | **none** - marker | `CollapseTypeAdapterFactory` | 6 | 2 |
| `@Key` | **none** - marker; read from inside a `@Collapse` value class | `CollapseTypeAdapterFactory` | 6 | 2 |
| `PostInit` (interface) | `postInit()` | `PostInitTypeAdapterFactory` | 7 | 1 |

The two remaining registered factories carry no annotation: `CaseInsensitiveEnumTypeAdapterFactory`
(idx 0) claims every `enum` type and `OptionalTypeAdapterFactory` (idx 1) claims `Optional<?>`.
Neither wraps a POJO.

`Capture.Grouping` has exactly two constants (`Capture.java`:132-146): `AUTO` (infer from value type)
and `ENTRY` (force whole-object reads). There is deliberately **no** `GROUPED` constant. The inference
lives at `CaptureFieldInfo` `:680-693` - grouping is inferred when the raw value type is not a
primitive, `Number`, `String`, `Boolean`, `enum`, `Object`, `Map` or `Collection`.

Field-discovery gates, which are where "the annotation is silently ignored" comes from:

| Factory | Gates applied when scanning fields |
| --- | --- |
| `Lenient` (`:428-452`) | skips `transient`; skips any field also carrying `@Capture`; skips non-`ParameterizedType` |
| `Extract` (`:479-494`) | skips `transient` only |
| `Capture` (`:769-793`) | skips `transient`; requires `ParameterizedType`; requires the field type be assignable to `Map` |
| `Collapse` (`:383-399`) | skips `transient` |
| `Split` (`:217-238`) | skips `transient`; requires the raw type be exactly `Pair` or `PairOptional` |
| `SerializedPath` (`:163-175`) | **no gates at all** - not even `transient` |

`Reflection.setProcessingSuperclass(false)` is set in every one of those scans, so **no annotation in
this library is inherited from a superclass field**. `CollapseFieldInfo.findKeyAccessor` (`:361-371`)
is the single exception - it sets `true`, so `@Key` alone is found on inherited fields.

## 9. Empty catch inventory

The brief named three. There are **five** of the silent-swallow class, plus one `ignored`-named empty
body. Ranked by what they hide.

| # | Site | Catches | What is swallowed |
| --- | --- | --- | --- |
| 1 | `PostInitTypeAdapterFactory.java`:37-38 | `Exception` | The **entire** `postInit()` body of every model that implements `PostInit`. Also swallows the `NullPointerException` when the delegate returned `null` (`:36` dereferences `obj` unguarded) and any `ClassCastException` from the `(PostInit)` cast at `:36`. The research pack found four silently dark features behind this one |
| 2 | `LenientTypeAdapterFactory.java`:246-247 | `Exception` | An `@Extract` failure - **both** the `gson.fromJson` conversion at `:244` **and** the reflective `accessor.set` at `:245`. A type mismatch between the extracted JSON and the companion field leaves the field at its initialiser with no signal, and because the entry was already removed from the overflow at `:216` it is **also gone from the write-side merge-back** |
| 3 | `CaptureTypeAdapterFactory.java`:401-402 | `Exception` | `buildSimpleMap` per-entry. Key conversion (`:398`), value conversion (`:399`) and `map.put` (`:400`). Drops the entry from the map, and the entry is **not** in overflow either - it was judged compatible at classify time |
| 4 | `CaptureTypeAdapterFactory.java`:477-478 | `Exception` | `buildGroupedMap` per-group, the exact twin of #3 at `:474-476`. **Missed by the brief.** This is the one that covers the two grouping-mode enum-keyed sites |
| 5 | `SplitTypeAdapterFactory.java`:160-161 | `Exception` | `@Split` post-assign - the `split`, both `gson.fromJson` calls and the reflective `set` (`:148-159`). A malformed delimited value leaves the field at `PairOptional.empty()`. **Missed by the brief.** Note the source key was already `remove`d at `:133`, so the value is **lost from the output document too** |
| 6 | `CollapseTypeAdapterFactory.java`:256-257 | `Exception`, named `ignored` | `injectKey` - the whole reflective key injection including `Integer.parseInt`/`Long.parseLong` (`:253`, `:255`) and `parseEnum`. A non-numeric JSON key against an `int` `@Key` field silently leaves the key unset |

Deliberate narrow catches, listed so a fixer does not sweep them up by mistake - these have bodies, or
are scoped to `NoSuchFieldException`, or are documented:

`CaseInsensitiveEnumTypeAdapterFactory.java`:59; `CaptureTypeAdapterFactory.java`:492-494, `:499-501`,
`:561-563`, `:603`, `:719-722`; `LenientTypeAdapterFactory.java`:261-263, `:268-270`, `:324-326`;
`CollapseTypeAdapterFactory.java`:264-269, `:282-284`, `:305-306`; `GsonSettings.java`:197-200.

Two shared properties of #2 through #6 that matter to this cycle:

- All five are **post-delegate reflective assignment** blocks. They exist because the delegate has
  already produced an object and the factory does not want a late failure to void the whole read. A
  design that adds a phase in the same position inherits the same tension and should say what it does
  about it rather than copying the empty block.
- Each of them can also fire on an `Error` subclass escaping as an `Exception` wrapper from the
  reflective layer, and none distinguishes "expected type mismatch" from "our own bug".

## 10. Adoption-site inventory

Paths below are relative to
`W:/Workspace/Java/Simplified/Simplified-Api/hypixel/src/main/java/api/simplified/hypixel/response/skyblock/`.

### 10.1 `@Extract` sites

**Six, not the three the brief listed (C4).** All six source from a `@Lenient` field, all six name that
field by its **Java** name, and all six target a `JsonObject` overflow.

| # | Site | Declaration | Source field | Target type |
| --- | --- | --- | --- | --- |
| 1 | `member/Bestiary.java`:33-34 | `@Extract("kills.last_killed_mob")` | `kills` `Map<String, Integer>` | `Optional<String>` |
| 2 | `member/foraging/Foraging.java`:30-31 | `@Extract("treeGifts.milestone_tier_claimed")` | `treeGifts` `Map<String, Integer>` | `ConcurrentMap<String, Integer>` |
| 3 | `member/hoppity/ChocolateFactory.java`:44-45 | `@Extract("rabbits.collected_eggs")` | `rabbits` `Map<String, Integer>` | `ConcurrentMap<String, Long>` |
| 4 | `member/hoppity/ChocolateFactory.java`:46-47 | `@Extract("rabbits.collected_locations")` | `rabbits` `Map<String, Integer>` | `ConcurrentMap<String, ConcurrentList<String>>` |
| 5 | `member/Loadouts.java`:23-24 | `@Extract("armorSets.equipped_set")` | `armorSets` `Map<Integer, ArmorSet>` | `Optional<Integer>` |
| 6 | `member/Loadouts.java`:29-30 | `@Extract("equipmentSets.equipped_set")` | `equipmentSets` `Map<Integer, EquipmentSet>` | `Optional<Integer>` |

Notes a designer needs:

- Sites 5 and 6 are the interesting ones. The source maps are `Map<Integer, ...>`, so `equipped_set`
  reaches overflow because the **key** fails `Integer` conversion, not because the value type is wrong.
  Both `@Extract` targets are `Optional<Integer>`, which binds through `OptionalTypeAdapterFactory` at
  the innermost end of the chain via the fresh `gson.fromJson` at `LenientTypeAdapterFactory`:244.
- Sites 2, 3, 4 extract a **container** out of overflow, not a scalar. Whatever a filter element does,
  it must not assume the `@Extract` target is scalar.
- `Loadouts` carries three `@Lenient` fields and two `@Extract` fields on one class - the densest site
  and the right shape for a regression fixture.
- No `@Extract` names a `@Capture` field today, and none names a collection-typed `@Lenient` field
  (which would silently no-op, §3.1 R5b).

### 10.2 `@Lenient` sites

Ten fields across seven classes.

| Site | Shape | Also carries |
| --- | --- | --- |
| `member/Bestiary.java`:38-39 `kills` | `Map<String, Integer>` | - |
| `member/Bestiary.java`:40-41 `deaths` | `Map<String, Integer>` | - |
| `member/foraging/Foraging.java`:27-29 `treeGifts` | `Map<String, Integer>` | `@SerializedName("tree_gifts")` |
| `member/hoppity/ChocolateFactory.java`:42-43 `rabbits` | `Map<String, Integer>` | - |
| `member/Loadouts.java`:20-22 `armorSets` | `Map<Integer, ArmorSet>` | `@SerializedName("armor")` |
| `member/Loadouts.java`:26-28 `equipmentSets` | `Map<Integer, EquipmentSet>` | `@SerializedName("equipment")` |
| `member/Loadouts.java`:32-34 `loadouts` | `Map<Integer, Loadout>` | `@SerializedName("loadouts")` |
| `member/dungeon/FloorData.java`:28-30 `completions` | `Map<Floor, Integer>` **enum key** | `@SerializedName("tier_completions")` |
| `member/Statistics.java`:41-43 `spawnedSpookyBats` | `Map<Integer, Integer>` | **`@SerializedPath("spooky.bats_spawned")`** |
| `member/dungeon/Dungeons.java`:33-35 `unlockedJournals` | **`ConcurrentList<Integer>`** | **`@SerializedPath("dungeon_journal.unlocked_journals")`** |

Two of these exercise code paths the common case does not, and both belong in any regression set:
`Dungeons.unlockedJournals` is the **only** collection-shaped `@Lenient` field in the module (the
`JsonArray` branch at `:185-199` and the array merge-back at `:139-144`), and it plus
`Statistics.spawnedSpookyBats` are the **only** two that drive `locateElement`/`replaceElement` down
the `@SerializedPath` segment branch (`:340-350`, `:357-368`) rather than the flat-name branch.
`FloorData.completions` is the only enum-keyed `@Lenient`, so it is the only site where `@Lenient`'s
own key-conversion check (`:256-264`) does real work.

### 10.3 `@Capture` sites and the key-conversion surface

Seventeen fields across twelve classes. Mode is what `CaptureFieldInfo` `:690-693` infers.

| Site | Key | Value | Mode | Filter / flags |
| --- | --- | --- | --- | --- |
| `member/AccessoryBag.java`:208-209 `slots` | `Integer` | `Slot` | grouped | `^slot_` |
| `member/AccessoryBag.java`:221-222 `stats` | `String` | `Integer` | entry | catch-all |
| `member/crimson/Dojo.java`:15-16 `points` | **enum `Type`** | `Integer` | entry | `^dojo_points_` |
| `member/crimson/Dojo.java`:17-18 `times` | **enum `Type`** | `Integer` | entry | `^dojo_time_` |
| `member/crimson/Kuudra.java`:18-19 `highestWave` | **enum `Tier`** | `Integer` | entry | `^highest_wave_` |
| `member/crimson/Kuudra.java`:20-21 `completedTiers` | **enum `Tier`** | `Integer` | entry | catch-all |
| `member/crimson/TrophyFishing.java`:24-25 `fish` | **enum `TrophyFish`** | `TierData` | grouped | catch-all |
| `member/mining/HeartOfTheMountain.java`:49-50 `powder` | **enum `Powder.Type`** | `Powder` | grouped | `^powder_` |
| `member/foraging/HeartOfTheForest.java`:48-49 `tiers` | `Integer` | `Tier` | grouped | catch-all |
| `member/foraging/MelodyHarp.java`:23-24 `songs` | `String` | `Song` | grouped | `^song_` |
| `member/SkillTree.java`:36-37 `entries` | `String` | `Node` | grouped | catch-all |
| `member/Statistics.java`:265-266 `festivals` | `String` | `FestivalCandy` | grouped | catch-all |
| `member/Toolkit.java`:21-22 `tools` | `String` | `ConcurrentList<NbtContent>` | entry | catch-all |
| `member/slayer/SlayerBoss.java`:26-28 `claimedLevels` | `Integer` | `ClaimedLevel` | grouped | `^level_`, `descend = true` |
| `member/slayer/SlayerBoss.java`:29-30 `kills` | `Integer` | `Integer` | entry | `^boss_kills_tier_` |
| `member/slayer/SlayerBoss.java`:31-32 `attempts` | `Integer` | `Integer` | entry | `^boss_attempts_tier_` |
| `member/Experimentation.java`:44-49 (three fields) | `Integer` | `Integer` | - | **DEAD - see below** |

**`Experimentation.Table`'s three `@Capture` fields are inert.** `attempts` (`:45`), `claims` (`:47`)
and `bestScore` (`:49`) are all declared `transient`, and `CaptureFieldInfo.of` skips `transient`
fields at `:775-776`. Nothing populates them; nothing warns. That is a seventh silently dark feature,
in the same class as the four the research pack found behind the `PostInit` catch, and it is worth a
line in whatever diagnostics this cycle adds.

Key-conversion behaviour splits three ways, and the split is the whole of §10.4:

- **`String` key** - `isCompatibleCaptureEntry` skips the check outright (`:489`). Cannot fail.
- **`Integer` key** - gson's `Integer` adapter **throws** on a non-numeric string, so
  `isCompatibleCaptureEntry` `:490-494` catches it and returns `false`, and the entry lands in
  overflow. Correct, lossless, round-trips. Seven sites behave this way.
- **`enum` key** - `CaseInsensitiveEnumTypeAdapterFactory.read` `:82` returns `nameToConstant.get(...)`,
  which is **`null` for an unmatched name and throws nothing**. So `:491` succeeds, the entry is judged
  compatible, and it is never diverted to overflow. Six sites behave this way.

### 10.4 The unmatched-enum-key defect, mechanism traced

Six `@Capture` sites, not seven (C6): `Dojo.java`:15-16 and :17-18, `Kuudra.java`:18-19 and :20-21,
`TrophyFishing.java`:24-25, `HeartOfTheMountain.java`:49-50. The pack's seventh,
`Statistics.java`:89, is not a `@Capture` field at all - see C6.

The chain, end to end:

1. Classify judges the entry compatible, because the enum adapter returns `null` rather than throwing
   (`CaptureTypeAdapterFactory`:490-495). The entry goes to `capturedJsonMaps`, **not** to overflow.
   For the two grouping-mode sites the check is not even reached (`:332-334`, `:355`).
2. `buildSimpleMap`:398 or `buildGroupedMap`:474 converts the key and gets `null`.
3. `map.put(null, value)` at `:400` / `:476`. The map instance came from `newMapInstance()` (`:714-723`)
   and is a `dev.simplified.collection.ConcurrentMap`, which tolerates a null key - so no exception
   fires and the empty catch at `:401-402` / `:477-478` never even gets a chance to hide it.
4. Every unmatched key in that field collapses onto the **same** `null` key. Last write wins. The
   observed probe `{null=4, BASIC=1}` means four distinct upstream keys produced one entry and
   **three values were silently discarded**.
5. Nothing lands in overflow, so the write side does not restore them either. The data is gone from
   both the object and the round trip.

Two amplifications the pack's §9 does not state, and a design entry should:

- The loss is **N-1 values per field**, not "one odd key". Collapse is the real cost.
- It is **not** caught by the empty catches. Fixing catch #3 and #4 from §9 would not surface this.

## 11. What would break - the regression baseline

Written adversarially. Every row is a behaviour a consumer can depend on **today** that inserting a
factory, moving a factory, or moving `@Extract` out of `LenientTypeAdapterFactory` could disturb.
Baseline to hold: gson-extras **134/134**, hypixel **16/16**.

**A. Position-in-chain invariants.**

| # | Invariant | How it breaks | Guard |
| --- | --- | --- | --- |
| A1 | A new factory registered **after** `Capture` in the `GsonSettings` list becomes **outer** to it, not inner. The list reads bottom-up | Author places `ExtractTypeAdapterFactory` "after Lenient" intending inner, gets outer | State the intended nesting depth, then derive the index; never state the index alone |
| A2 | `Capture` hands the delegate `knownObject`, which contains **only** declared-field keys. A factory placed inside `Capture` can never see a captured or overflowed key | A new inner factory that wants to read a dynamic key silently no-ops | Any factory needing dynamic keys must sit **outside** `Capture` |
| A3 | `Lenient` mutates `rootObject` **in place** (`:183`, `:197`) rather than building a new tree. A factory outside it that retained a reference to that same object sees the filtered version | Aliasing bug across a newly inserted outer factory | Do not retain the tree across a `fromJsonTree` call |
| A4 | `SerializedPath` binds its fields with a **fresh top-of-chain** `gson.fromJson` (`:132`), so a `@SerializedPath` field never traverses the enclosing class's delegate chain | A new factory expecting to intercept a `@SerializedPath` field's bind never runs | Treat `@SerializedPath` fields as out of chain, both directions |
| A5 | `Split` **removes** its key from the tree before delegating (`:133`); `Capture` removes descend nodes (`:280`) and every claimed key | An inner factory looking for a key an outer one already consumed | Enumerate consumed keys per factory before choosing an index |
| A6 | Inserting **any** factory shifts every later index in `GsonSettings.defaults()`, and the two dark-ordering pairs are `Collapse`/`Capture` (`GsonFactoryTest` `:1979-2166`) and `Lenient`/`Capture` (`:2168-2253`) | An index shift silently reorders an unrelated pair | Re-run the whole `CombinationTests` nest, not just the new tests |
| A7 | `Lenient` and `SerializedPath` return the **delegate**, never `null` (§2.3). `getDelegateAdapter` skips past the caller and takes the first non-null after it, so any factory registered inner to `Lenient` is reached only through `Lenient`'s pass-through | A new factory that returns the delegate instead of `null` when idle changes which factory a **third** factory's `getDelegateAdapter` resolves to | New factories return `null` when idle |

**B. `@Extract` behaviours, if it moves to its own factory.**

| # | Behaviour | Why it is load-bearing |
| --- | --- | --- |
| B1 | `@Extract` **removes** the claimed key from the `@Lenient` overflow on read (`:216`) and **re-adds** it on write (`:108-111`). A split that reads without removing double-writes the key; one that removes without re-adding drops it | Round-trip fidelity at all six sites |
| B2 | The re-injection target is `computeIfAbsent(collection, ...)` on the **`@Lenient` store**, and the merge target is the `@Lenient` field's own sub-object (`:132`). A `Capture`-sourced claim must not use that path (§4.2) | Wrong-place merge-back is silent and only visible in a serialize test |
| B3 | `@Extract` resolves its source by **Java field name** (`:471`), never by `@SerializedName`. Sites 5 and 6 prove it - `armorSets` vs `@SerializedName("armor")` | Any move to serialized-name lookup breaks four of six sites |
| B4 | `@Extract` runs **before** the reflective binder (`:205-221` precedes `:224`) but **assigns after** it (`:242-248`). The assignment therefore overwrites whatever the binder wrote | A factory that assigns before the binder gets overwritten instead |
| B5 | The path splits on the **first** dot (`:468`). `@Extract("a.b.c")` yields source `a`, key `b.c` - a literal key containing a dot, not a nested path | Do not "fix" this into a path walk; no site needs it and it changes existing semantics |
| B6 | A dotless `@Extract("field")` yields `jsonKey = ""` (`:474-476`) and then claims the literal empty key, which never exists. Silent no-op, no site uses it | A filter element must define what a dotless value means rather than inheriting this |
| B7 | `@Extract` conversion failure is swallowed at `:246-247`, leaving the field at its initialiser. Six sites all declare `@NotNull` initialisers and rely on that | Making it throw is a behaviour change for consumers, not just the library |
| B8 | An `@Extract` field's Java name is registered as a `Capture` known key (`:114-143`), so a catch-all can never claim it | Removing `@Extract` fields from `Lenient`'s scan does not remove them from `Capture`'s known-key scan - those are separate walks |

**C. `@Lenient` behaviours.**

| # | Behaviour |
| --- | --- |
| C1 | Overflow is published **unconditionally**, even when empty (`:236-239`), unlike `Capture` (`:386`). A consumer that serializes an object whose `@Lenient` field had no overflow relies on `get` returning an empty container, not `null` |
| C2 | Shape mismatch (map field / array JSON) matches neither branch, so the field is left **unfiltered** and handed raw to the delegate. That is the pre-`c944987` failure mode and it is still reachable |
| C3 | `LenientFieldInfo.of` skips any field also carrying `@Capture` (`:437-438`). Two annotations on one field is `@Capture`-wins today; changing that changes nothing in the module but changes the library contract |
| C4 | Only two sites drive the `@SerializedPath` branch of `locateElement`/`replaceElement`, and exactly one site (`Dungeons.unlockedJournals`) drives the whole `JsonArray` half of the factory. Both are single-site coverage - a regression there passes 15 of 16 hypixel tests |
| C5 | `isCompatibleElement` `:318-321` accepts a `JsonObject` for any non-primitive non-collection type. A stricter check moves entries into overflow that currently bind, silently shrinking maps |
| C6 | Integer/Long compatibility requires `d == Math.floor(d)` and, for `int`, range containment (`:294-303`). A value of `3.0` binds as `Integer`; `3.5` goes to overflow |

**D. `@Capture` behaviours.**

| # | Behaviour |
| --- | --- |
| D1 | Filtered fields are tried in **declaration order, first match wins** (`:323-343`, break at `:342`). `SlayerBoss`'s `^boss_kills_tier_` and `^boss_attempts_tier_` are disjoint, but `Dojo`'s `^dojo_points_`/`^dojo_time_` and `Kuudra`'s `^highest_wave_` plus catch-all depend on order. Reordering fields in a DTO is a behaviour change |
| D2 | Catch-all is `.filter(info -> !info.hasFilter()).findFirst()` (`:349-352`). A second catch-all on one class is silently ignored, not rejected |
| D3 | Overflow stores the **unstripped original** key (`:338`, `:358`), while captured entries store the **stripped** key. The write path re-applies `literalPrefix` (`:218-220`, `:228-230`) to captured keys only. Any shared-store design must preserve that asymmetry or keys get double-prefixed |
| D4 | `literalPrefix` is the filter with `^` and `$` stripped (`:654`) - a **literal**, not a regex inverse. A filter with real regex metacharacters beyond the anchors does not round-trip. No site does this today; all eleven filters are literal-plus-`^` |
| D5 | `accessor.set(result, capturedMap)` (`:381`) **replaces** the field's initialiser instance. Any design that keys a store by the map must key the post-`set` instance |
| D6 | Grouping mode skips the compatibility check entirely, so grouping-mode fields produce **no overflow ever**. Six of the seventeen sites are grouping mode |
| D7 | `buildGroupedMap`'s no-affix / non-object branch (`:448-468`) drops the entry with no record. `HeartOfTheForest.tiers`, `MelodyHarp.songs`, `SkillTree.entries` and `Statistics.festivals` all rely on the object-merge fallback at `:456-467` that was added by `b071689`/`7cfc181` |
| D8 | `descend = true` `remove`s the nested node from the root (`:280`) **before** classify runs, so its keys are invisible to every other `@Capture` field on the class. `SlayerBoss` has one descend field and two non-descend ones |

**E. Store lifetime and identity.**

| # | Behaviour |
| --- | --- |
| E1 | Both stores are `private static final`, shared across **every** `Gson` instance in the JVM. A per-`Gson` or per-read store changes lifetime semantics for a consumer that reads with one `Gson` and writes with another. Nothing in hypixel does this - but nothing prevents it |
| E2 | Identity keying means a consumer may mutate a bound collection after the read and still find its overflow on write. `WeakIdentityMap`'s class javadoc names this as the reason it exists |
| E3 | Entries are reclaimed only when the key is collected, and `sweep()` is a **full scan** on the first access after any clear. A store that grows to hold more per entry raises that cost |
| E4 | A `null` bind result discards that read's overflow (`Lenient` `:226`, `Capture` `:368`). Already true, already a fidelity hole |
| E5 | `computeIfAbsent` at `:108` installs an entry as a **side effect** of the write path, including the dead `JsonArray` case at `:108`/`:110`. A shared store inherits that |

**F. Build-cycle and cross-module.**

| # | Behaviour |
| --- | --- |
| F1 | hypixel pins gson-extras at sha `7cfc181`. Every library change is one full JitPack cycle before the consumer can be tested at all (`00-conventions.md` §4) |
| F2 | Sibling modules share the pin. `git log` shows repeated "converge the sibling pins onto one sha" work; a change that lands in gson-extras is visible to every sibling on the next pin bump, whether or not they wanted it |
| F3 | `GsonSettings.defaults()` also loads SPI `TypeAdapterFactory`s and `GsonContributor`s (`:259-263`). A downstream module can already register a factory **outside** everything the library registers. A new ordering guarantee is therefore not enforceable against SPI factories |
| F4 | Adding an element to an existing annotation with a `default` is source- and binary-compatible for consumers; adding one **without** a default is not. Every element on `@Capture` and `@Split` today has a default except `@Split.value` and `@Extract.value` |

**G. Test anchors to re-run, by name.**

- `GsonFactoryTest.CombinationTests` - the whole nest, especially `lenientWithCapture_ok` (`:2182`),
  `lenientExtractCapture_ok` (`:2226`), the `@Collapse` + `@Capture` pair (`:1979-2070`) and the
  `@Collapse` + `@Capture(descend)` pair (`:2071-2166`). These are the only tests in the library that
  observe factory nesting at all.
- `GsonFactoryTest.CaptureTests` - eighteen model classes covering catch-all, filtered, multi-filter,
  `@SerializedName`, `PostInit`, grouping, bare-entry, enum-key, map-of-maps, prefix and caret-prefix.
- `CaptureGroupingModeTest`, `CollectionValueCompatibilityTest` - the `c944987` and `b071689`/`7cfc181`
  regression sets. These are the newest behaviour and the least settled.
- `WeakIdentityMapTest` - if the store type changes at all.
- `GsonFactoryTest.PostInitTests` including `FailingPostInitModel` (`:1766`) - the one test that pins
  the swallow at `PostInitTypeAdapterFactory`:37 as **current intended behaviour**. Any change to that
  catch changes this test, which means it is a deliberate contract, not an oversight.
- hypixel's 16 - and specifically anything touching `Loadouts`, `Dungeons`, `Statistics`,
  `ChocolateFactory`, `Bestiary` and `Foraging`, which are the six `@Extract`/`@Lenient` classes.

**H. The two things most likely to be got wrong, stated plainly.**

1. Writing a design on the brief's inverted nesting (C1). Everything downstream of that premise -
   which factory can see which tree, whether a reorder is needed, whether the fix is `medium` or
   `xlarge` - comes out wrong.
2. Assuming a shared store is a **map union**. It is not. Each entry has to carry its producer's
   merge-back target, because `@Lenient` re-injects into the field's own sub-object and `@Capture`
   re-injects into the root or a descend node, and both are correct (§6).

