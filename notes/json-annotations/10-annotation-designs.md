# 10 - Annotation designs

## 1. How to read this document

This document consumes the findings of surveys `01-postinit.md` through `06-structural-duplication.md`
and turns them into decisions. It is split in two.

- **Part one** owns the *data-shape* registry entries: `@Inline` and `@Delegate` (unwrapping a nested
  object onto its enclosing class), `@Flatten` (collapsing a single-field value), and the enum/parse
  entries `@Fallback` and `@Alias`, plus the two proposed element additions to existing annotations
  (`@Capture`, `@Lenient`).
- **Part two** owns the *reach-back and derivation* entries: `@Owner`/`@Parent`, ancestor-relative
  paths, `@Derive`, `@Index`/`@Join`, `@Tier`, `@Aggregate` and `@Bind`. Its headings are present and
  empty; a second pass fills them.

Every name is taken from the naming registry in `00-conventions.md` §6. No entry is renamed here. Two
entries are declined outright and one is declined in favour of a stock feature; those declines are
decisions, not omissions, and they are written up at the same length as the accepts so the argument
survives.

The surveys converged on an uncomfortable result that this document has to carry rather than soften:
**the annotation set is close to complete already.** Across six surveys and roughly forty findings,
the total library ask that survives scrutiny is *one behavioural change inside one existing factory*
and *one optional new annotation with a single unambiguous adoption site*. Everything else is either
an existing annotation that was never applied, a stock Gson or Lombok feature, or a consumer-side
lazy accessor.

## 2. The stock-first rule

**Before any entry in this document is accepted, it has to beat what already ships.** The user's goal
is less code, not more annotations, and `00-conventions.md` §4 prices a library change at a full
JitPack publish-and-re-pin cycle even for a one-line javadoc fix. An annotation that duplicates a
stock feature is strictly negative value: it costs the cycle, it costs a second way to spell the same
thing, and it costs every future reader the question "why not the stock one?".

Applied to this pack, the rule disqualifies four proposals before design work starts.

| Proposal | Already achievable with | Cost of the stock route | Verdict |
| --- | --- | --- | --- |
| `@Inline` - bind a sub-object's keys onto the enclosing class | **`@SerializedPath`** (`dev.simplified.gson.annotation`, already in the project, already used) | zero library change; 13 field declarations edited | **reject** |
| `@Delegate` - generate a forwarder to a nested object's field | **`lombok.experimental.Delegate`** (stock Lombok, on the classpath) | zero library change, but see `d10-delegate` | **reject** |
| `@Alias` - accept more than one JSON key for one field | **`@SerializedName(value = "x", alternate = {"y"})`** (stock gson, and `CaseInsensitiveEnumTypeAdapterFactory.java`:56-57 already honours alternates on enum constants) | zero library change; one annotation element | **reject** |
| `@Flatten` - collapse a single-key JSON object into its scalar | **`@JsonAdapter`** (stock gson, honoured on fields since 2.3; the project is on gson 2.11.0) | zero library change; ~20 lines in this module | **conditional - see `d10-flatten`** |

Two further stock facts are load-bearing later and are recorded here so no design re-derives them.

- **Gson honours `@JsonAdapter` on a field**, not only on a type. `ReflectiveTypeAdapterFactory`
  consults the field annotation when it builds the bound field, so a field-scoped shape transform
  needs no `TypeAdapterFactory` registration in `GsonSettings` at all. This is the single most
  under-used stock feature relative to what this pack was asked to design.
- **Gson honours `@SerializedName(alternate = ...)` on fields and, through
  `CaseInsensitiveEnumTypeAdapterFactory`, on enum constants.** Any "the upstream key was renamed"
  problem is already solved.

The rule cuts the other way exactly once. `@Fallback` has no stock equivalent, because Gson's
reflective binder assigns whatever a field's `TypeAdapter.read` returns, including `null`, and a
`TypeAdapter` has no way to say "decline to write this field". That gap is real and it is what
`d10-fallback` exists to close.

## 3. Design entry format

Each entry below carries this block, so `11-postinit-elimination.md` and `20-implementation-plan.md`
can machine-read the decisions. `Category` and `Effort` use the vocabularies from
`00-conventions.md` §5 and §4.

```
### d10-slug - `@Name`
- **Registry entry:** the row in 00-conventions.md §6 this answers
- **Verdict:** adopt | adopt narrowly | decline | reject
- **Category:** <one of the nine category slugs>
- **Answers findings:** f0N-slug, ...
- **Cheaper alternative:** what already ships that does this, or "none"
- **Library change:** none | additive file | existing factory edit
- **Adoption sites today:** N
- **Effort:** trivial | small | medium | large | xlarge
```

`Verdict` is defined as follows. **Adopt** means build it. **Adopt narrowly** means build it with a
smaller surface than the registry line implies, and the entry says exactly what was cut. **Decline**
means the concept is sound but the evidence does not pay for it now, and the entry states what would
change that. **Reject** means the concept is dominated by something that already exists and should
not be revisited.

# PART ONE - Data-shape annotations

## 4. d10-inline - `@Inline`

- **Registry entry:** `@Inline` - "binds the fields of a named JSON sub-object directly onto the
  enclosing class, retiring a private holder class that exists only to name that object"
- **Verdict:** reject
- **Category:** `value-shape-collapse`
- **Answers findings:** `f04-nested-holder-idiom`, `f04-delegate-rejected`,
  `f03-holder-collapse-serializedpath`, `f03-unreachable-private-holders`,
  `f04-holder-private-type-leak`
- **Cheaper alternative:** `@SerializedPath` - already exists, already used in this module
- **Library change:** none (reject) versus additive annotation plus new factory (if built)
- **Adoption sites today:** 9 holder classes, 13 held fields on `04-accessor-boilerplate.md` §2.1's
  accessor-idiom census; 11 classes across 8 files once `f03-holder-collapse-serializedpath`'s
  `HypixelPlayer.Stats` chain is unioned in - see §4.3
- **Effort:** `small` for the `@SerializedPath` adoption; `small` at absolute best for building
  `@Inline`, and that is before the JitPack cycle and re-pin

### 4.1 The problem it would remove

Nine classes in `response/` exist only to name a statically keyed JSON sub-object so its members can
bind. Each is paired with a `@Getter(AccessLevel.NONE)` field on the enclosing type and a
hand-written forwarding accessor. The full census is in `04-accessor-boilerplate.md` §2.1; the
totals are 9 holders, 13 held fields, 10 forwarders and ~79 lines of holder class body.

Two of the nine are worse than verbose. `AttributeShards.java`:12 and `Bestiary.java`:37 keep the
class-level `@Getter`, so Lombok emits a public accessor returning a `private static` nested class.
That is a compile error at every external call site - verified by javac reproduction in
`f04-holder-private-type-leak`: `error: Holder.getValue() is defined in an inaccessible class or
interface`. `shards.traps.active_traps` is an eleven-field `ActiveTrap` list that is parsed on every
profile fetch and then unreachable, and `bestiary.miscellaneous.max_kills_visible` and
`milestones_notifications` are likewise dead.

So the problem is real and it is worth solving. The question this entry answers is only whether it
needs an annotation.

### 4.2 Why `@SerializedPath` already does it

It does not need one. `@SerializedPath` binds an arbitrary value type at an arbitrary depth on read
and re-nests it on write - `SerializedPathTypeAdaptorFactory.java`:60-92 (write) and :96-140 (read).
Every holder in the package is a path expression that was never written.

The one property that makes this work for multi-field holders, and that a reader will doubt, is
prefix sharing on serialize. `SerializedPathTypeAdaptorFactory.java`:80-86 *reuses* an existing
nested object when the walk finds one already present rather than overwriting it, so three fields
declaring `profile.first_join`, `profile.personal_bank_upgrade` and `profile.cookie_buff_active`
land in one `profile` object: the first creates it, the second and third find it. Read is symmetric
at :105-141. That is the whole of what `@Inline` would have had to implement.

`@Inline` would earn its keep only where repeating a path prefix across N fields is worse than
declaring the prefix once. The census puts a hard number on N: **the largest holder in the package
is `SkyBlockMember.Profile` with 3 fields, and six of the nine have exactly 1.** Against
`@SerializedPath` the entire saving is at most two repetitions of the string `"profile."`. Weighed
against a new annotation, a new factory, a JitPack cycle and a re-pin, that is negative value, and
`00-conventions.md` §6 anticipated it ("`@SerializedPath` already covers the single-field case of
`@Inline`; `@Inline` only earns its keep for multi-field holders"). The evidence says the multi-field
holders do not exist here.

### 4.3 Usage before and after - `Rift`

Real code, `response/skyblock/member/rift/Rift.java`:23-25 and :44-54.

```java
// before
@Getter(AccessLevel.NONE)
@SerializedName("wither_cage")
private @NotNull Porhtal porhtal = new Porhtal();

public @NotNull ConcurrentList<String> getKilledEyes() {
    return this.porhtal.getKilledEyes();
}

@Getter
private static class Porhtal {

    @SerializedName("killed_eyes")
    private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();

}
```

```java
// after
@SerializedPath("wither_cage.killed_eyes")
private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();
```

`getKilledEyes()` survives, generated by the class-level `@Getter`. One class, one suppression, one
forwarder and the `AccessLevel` import all go, and `Rift` drops from 56 lines to 42.

The multi-field case, `SkyBlockMember.Profile` (`SkyBlockMember.java`:48, :166-180, :220-230), is the
only site where `@Inline` would have had anything to say:

```java
// after - three declarations replace one holder class and three forwarders
@SerializedPath("profile.first_join")
private SkyBlockDate.RealTime firstJoin;
@SerializedPath("profile.personal_bank_upgrade")
private int personalBankUpgrade;
@Accessors(fluent = true)
@SerializedPath("profile.cookie_buff_active")
private boolean isBoosterCookieActive;
```

`getFirstJoin()` narrows covariantly from `SkyBlockDate` to `SkyBlockDate.RealTime`
(`SkyBlockDate.java`:778 - `RealTime extends SkyBlockDate`), which no caller can observe.

Package-wide payoff: 9 classes removed (8 nested plus `Temples.java`, taking `response/` from 133
files to 132), 13 held fields relocated, 10 forwarders deleted, 2 dead data paths recovered, net
~108 lines. **Zero library change, zero JitPack cycle, zero re-pin.** It is the cheapest high-payoff
item in the pack.

One caveat on the number nine, because the two surveys count different sets and this entry answers
both. The figures above are `04-accessor-boilerplate.md` §2.1's census - nine holders identified by
the **accessor idiom**, including `AttributeShards.Traps` and `Bestiary.Miscellaneous`.
`f03-holder-collapse-serializedpath` counts a different nine: it files those two under
`f03-unreachable-private-holders` and adds the `HypixelPlayer.Stats` / `Stats.SkyBlock` chain, which
§2.1 never reached. **The union is 11 classes across 8 files, ~130 lines.** The `HypixelPlayer` pair
converts through the same annotation but is not the paired idiom - `stats` (`HypixelPlayer.java`:80)
has neither a suppressed getter nor a forwarder, and `Stats.SkyBlock` (:132) hand-writes
`getProfiles()` (:136-138) - so it relocates an accessor and a nested class and changes two public
accessor shapes. Sequence it apart from the seven clean sites. Both findings are `small` on
`00-conventions.md` §4, which reserves `trivial` for single-file consumer work.

Three risks carried forward from `f04-nested-holder-idiom`, unchanged by this entry:

1. `Rift`, `VillagePlaza` and `AttributeShards` have no `@SerializedPath` field today, so
   `SerializedPathTypeAdaptorFactory.create` returns the bare delegate for them (`:39-41`). After the
   change those three classes get wrapped, materialising the sub-tree as a `JsonObject` and re-parsing
   it (`:101-103`). `SkyBlockMember`, `Dungeons` and `Bestiary` already pay this on far larger
   objects, so the marginal cost is small - but measure it, do not assume it.
2. The flat key used on write is the `@SerializedName` value or the field name
   (`SerializedPathTypeAdaptorFactory.java`:160). Two `@SerializedPath` fields on one class must not
   share a flat key, and a flat key must not collide with a genuine top-level JSON key. None of the
   13 proposed names collide; the constraint is invisible in the source and will bite a later edit.
3. Deleting the `profile`, `events` and `temples` fields removes declared fields from
   `SkyBlockMember`. A bare `@Capture` catch-all added to that class later would then claim those
   keys - `CaptureTypeAdapterFactory.discoverKnownKeys` derives the known-key set from declared
   fields, and it already reads the first segment of a `@SerializedPath` (`:118-127`), so the hazard
   is bounded but real.

### 4.4 What would reopen this

A holder with roughly six or more fields, or a holder nested two levels deep so every field repeats a
long prefix. Neither exists in `response/`. The argument is about the observed distribution, not
about the concept, so if such a holder appears the row should be re-read rather than treated as
settled.

One thing that would *not* reopen it: a wish for the prefix to be declared once for readability.
That is a formatting preference, and it costs a library release.

## 5. d10-delegate - `@Delegate`

- **Registry entry:** `@Delegate` - "generates a forwarding accessor to a field of a nested object,
  retiring a hand-written pure-delegation getter"
- **Verdict:** reject
- **Category:** `accessor-boilerplate`
- **Answers findings:** `f04-delegate-rejected`, `f04-nested-holder-idiom`
- **Cheaper alternative:** `lombok.experimental.Delegate` (stock, zero library change) - and it still
  loses to `@SerializedPath`
- **Library change:** none
- **Adoption sites today:** 10 forwarders
- **Effort:** `trivial` (a decision, not a change)

### 5.1 Stock Lombok already ships this

**Say this loudly, because it is the cheapest thing in the room and it was evaluated seriously:**
there is no need to design a `@Delegate` annotation. Lombok already has one,
`lombok.experimental.Delegate`, it is on this project's classpath, and putting it on
`Rift.porhtal` would generate `getKilledEyes()` on `Rift` with no library change, no JitPack cycle
and no re-pin. On the effort scale that is `trivial`, which is cheaper than every other option in
this document.

It is still the wrong answer, for three reasons, the second of which is fatal.

### 5.2 Why it still loses

**1. It deletes the wrong half.** `@Delegate` removes the forwarder body and nothing else. The holder
class stays, the field stays, the extra nesting level in the Java shape stays, and
`f04-holder-private-type-leak`'s two unreachable data paths stay unreachable because the holder is
still a `private static class`. That is roughly 40 of the ~135 lines `@SerializedPath` removes, and
it leaves the correctness defect untouched.

**2. It cannot rename, and one site requires a rename.**
`VillagePlaza.getSeraphineStepIndex()` (`VillagePlaza.java`:33) forwards to
`Seraphine.getStepIndex()`. `@Delegate` forwards the delegate type's method signatures verbatim, so
it would generate `getStepIndex()` on `VillagePlaza` - which is meaningless there (which step
index?) and one step from a hard clash, because `VillagePlaza.Murder` also declares `step_index`
(`:40-41`) and delegating both would produce two `getStepIndex()` methods on one class. There is no
`@Delegate` element that fixes this.

**3. It is `lombok.experimental`,** with documented trouble on generic and self-referential types.
These DTOs are heavily generic - `ConcurrentMap<Floor, ConcurrentList<BestRun>>` and similar.
Adopting an experimental annotation to save 40 lines is a poor trade when a stable annotation already
in the project saves 135 and fixes a defect on the way.

Where `@Delegate` *would* be the right tool is the case `01-postinit.md` §4.2 raises: if a caller
genuinely must reach `CrimsonIsle`'s two `kuudra_party_finder` sub-objects through `getKuudra()`, a
forwarder on `CrimsonIsle` is the correct shape and mutating a child during bind is not. That is a
part-two question (`d10-owner-parent`, `d10-ancestor-path`) and the answer there is that nothing
outside `CrimsonIsle` reads them at all, so no forwarder is needed either.

**Conclusion:** the registry row is dominated twice over. Do not build a `@Delegate`; do not adopt
Lombok's; adopt `@SerializedPath` per `d10-inline`.

## 6. d10-flatten - `@Flatten`

- **Registry entry:** `@Flatten` - "collapses a single-valued JSON object (or single-field value
  class) into the scalar or collection the caller actually wants, removing a wrapper level"
- **Verdict:** adopt narrowly, and only as a rider - see `d10-flatten` §6.8 and §6.9
- **Category:** `value-shape-collapse`
- **Answers findings:** `f03-mapvalue-single-key`; partially `f03-dungeons-classmap-funnel`;
  explicitly declines `f03-biomewhispers-tier`
- **Cheaper alternative:** stock gson `@JsonAdapter` on the field - zero library change
- **Library change:** additive - one annotation file plus one self-contained factory, registered in
  `GsonSettings`
- **Adoption sites today:** **1** unambiguous (`Currencies.essence`), 1 declined
  (`HeartOfTheForest.BiomeWhispers.tiers`), 1 better served without it (`Dungeons.classMap`)
- **Effort:** `small` (the library floor - additive file, no existing factory edited, one JitPack
  cycle and one re-pin)

### 6.1 The problem it removes and the real sites

Hypixel wraps some map values in a one-key object. The Java side then declares a map of maps and
unwraps it by hand in an accessor, so the public type and the field type disagree.

| Site | Declared today | What the caller wants | Wrapper key |
| --- | --- | --- | --- |
| `member/Currencies.java`:17-24 `essence` | `ConcurrentMap<String, ConcurrentMap<String, Integer>>` | `ConcurrentMap<String, Integer>` | `current` |
| `member/dungeon/Dungeons.java`:30-32 `classMap` | `ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>>` | `ConcurrentMap<DungeonClass.Type, DungeonClass>` | `experience` |
| `member/foraging/HeartOfTheForest.java`:48-49 `tiers` | `ConcurrentMap<Integer, Tier>` | `ConcurrentMap<Integer, Integer>` | `spent` |

The distinguishing feature versus `d10-inline` is that the wrapper sits inside **every map value**,
so there is no static path for `@SerializedPath` to address. That is the gap the registry reserved
`@Flatten` for, and it is a genuine gap - no annotation in `dev.simplified.gson.annotation` reaches
the value side of a map.

Only the first row survives scrutiny, and `03-value-shape-collapse.md` §2 is the argument. The
wrapper is usually worth keeping, because adopting a new sibling key is then a one-line edit inside
the wrapper that no caller sees. `Currencies.essence` is the one place where that benefit is already
forfeit: `getEssence()` at `Currencies.java`:20-24 maps every value through `.get("current")` and
discards anything else, so the wrapper's absorptive capacity is **stated, not live**. A new `total`
key would be silently dropped today by an accessor nobody would think to change. Collapsing loses
nothing that is not already lost, and it moves the loss from a hidden accessor to a visible field
declaration.

Row two does not need the annotation at all. The JSON is already exactly the shape of
`DungeonClass`:

```json
{"healer": {"experience": 84271835.04}, "mage": {"experience": 409047204.36},
 "berserk": {"experience": 92858814.02}, "archer": {"experience": 98301741.50},
 "tank": {"experience": 96565524.56}}
```

`DungeonClass.java`:17-19 declares exactly one field, `private final double experience`. Declaring
`Dungeons.classes` as `ConcurrentMap<DungeonClass.Type, DungeonClass>` binds directly and deletes the
funnel field, the `@Getter(AccessLevel.NONE)`, the transient and six lines of `Dungeons.postInit()`
at :70-75 - at **zero** library cost. `@Flatten("experience")` onto a
`ConcurrentMap<DungeonClass.Type, Double>` reaches the same deletion and charges a JitPack cycle for
it. Take the free one.

Row three is declined on its own evidence. The fixture shows the whispers key family mid-growth -
`desert` gained a `total` key between the two profiles of one account - so `Tier` is the cheapest
possible place to absorb a future `refunded` key. The fix worth making there is two lines -
`@Getter(AccessLevel.NONE)` on `tiers`, plus `getSpent(int)` at `HeartOfTheForest.java`:51-55
switching from `this.getTiers()` to `this.tiers`, because the suppression deletes the very accessor
that body calls. That makes `Tier` an implementation detail behind the existing `getSpent(int)`
accessor and leaves the shape freely reversible. There is also a mechanical blocker: `tiers` is a
`@Capture` field, and §6.5 shows `@Flatten` cannot compose with `@Capture`.

### 6.2 Full declaration

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
 * Collapses a single-key JSON object into the value the caller wants, for every entry of a
 * {@link Map Map} or {@link Collection Collection} field.
 * <p>
 * During deserialization each element of the annotated field is replaced by the member named by
 * {@link #value()} before the field is bound, so the field declares the collapsed type rather than
 * the wrapper. During serialization each element is re-wrapped under the same key, so the document
 * round-trips unchanged.
 * <p>
 * An element that is not a JSON object, or that is an object without the named member, is left
 * untouched and therefore fails the declared type. Combine with {@link Lenient @Lenient} where the
 * wrapper is not guaranteed on every entry.
 * <p>
 * A statically keyed sub-object needs no collapse - {@link SerializedPath @SerializedPath} already
 * addresses it by path.
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
     * The member read out of each wrapper object on deserialize and written back on serialize.
     *
     * @return the wrapper member name
     */
    @NotNull String value();

}
```

One house-style note for the reviewer. `00-conventions.md` §8 says class and interface doc is a noun
phrase, but every sibling in `dev.simplified.gson.annotation` opens with a third-person verb
(`Split.java`:14 "Splits a single JSON string value...", `Collapse.java`:12 "Marks a `Map` or `List`
field...", `Lenient.java`:12 "Marks a `Map` or `Collection` field..."). The declaration above matches
its neighbours rather than the letter of the rule, on the grounds that an annotation type reads as a
directive. If the rule wins instead, the opening line becomes "Single-key JSON object collapsed into
the value the caller wants, ..." and the sibling files should be changed in the same commit rather
than leaving one odd file out.

### 6.3 Usage before and after - `Currencies`

`response/skyblock/member/Currencies.java`, whole file, before:

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

After:

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

The fixture value this binds, `profiles[1].members[...].currencies`:

```json
{"coin_purse": 54601987.695, "motes_purse": 806795,
 "essence": {"WITHER": {"current": 1955}, "DRAGON": {"current": 1132}, "UNDEAD": {"current": 5141},
             "DIAMOND": {"current": 8564}, "SPIDER": {"current": 312}, "GOLD": {"current": 3445},
             "ICE": {"current": 2557}, "CRIMSON": {"current": 4}}}
```

Payoff: one nested generic parameter, one `@Getter(AccessLevel.NONE)`, one five-line stream accessor,
and the `lombok.AccessLevel` import. Ten lines on a 26-line file. Behaviour is identical for every
caller, because `getEssence()` already returned `ConcurrentMap<String, Integer>` - the difference is
that the field now says so, and the value re-serializes correctly instead of not at all.

That last clause is worth spelling out, because it is the only *behaviour* improvement in the change.
Today `Currencies` round-trips fine only by accident: `essence` is bound as the map of maps and
serialized as the map of maps, and the collapse happens purely in the accessor. Under `@Flatten` the
collapse moves into the bind, so the factory owes the re-wrap on write, and §6.4 is where that is
paid for. An implementation that skipped the write path would turn `{"WITHER": {"current": 1955}}`
into `{"WITHER": 1955}` on the way out, silently changing the document. `@Lenient` and `@Collapse`
both preserve round-trip fidelity; that is the bar.

Note also what the change deliberately does *not* fix. `"current"` is a hedged key name - it reads
like a field expecting a `total` or `lifetime` sibling, and `Currencies` shows the API is not
internally consistent, since `motes_purse` and `coin_purse` sit unwrapped in the same object. If
growth is the worry the countermeasure is a fixture assertion on the key set, not a wrapper class
whose contents an accessor is already throwing away.

### 6.4 How the factory implements it

A new `FlattenTypeAdapterFactory` in `dev.simplified.gson.factory`, registered in
`GsonSettings.defaults()`. It is a **bind-time, pre-delegation tree rewrite**, not a post-bind
assignment, and it is modelled directly on `SplitTypeAdapterFactory` - the smallest self-contained
factory in the library and the right template for a per-field transform.

The shape, following `SplitTypeAdapterFactory.java`:42-52 exactly:

- `create(gson, typeToken)` scans the raw type's declared fields via `Reflection`, skipping
  `Modifier.isTransient` (every existing factory does this - `SplitTypeAdapterFactory.java`:223,
  `CaptureTypeAdapterFactory.java`:115), keeps the fields carrying `@Flatten` whose raw type is a
  `Map` or `Collection`, and returns `null` when there are none. Returning `null` matters: it is what
  keeps the factory off the hot path for the other 132 files in `response/`.
- `read(in)` reads the whole enclosing object once through `gson.getAdapter(JsonElement.class)`, and
  for each `@Flatten` field looks up its serialized key in the buffered `JsonObject`. For a map value
  it rewrites `{"WITHER": {"current": 1955}}` to `{"WITHER": 1955}` in place; for a collection it
  rewrites each element. Then it hands the rewritten tree to `delegateAdapter.fromJsonTree(...)`,
  which binds `ConcurrentMap<String, Integer>` with no further help. **No reflective field assignment
  is needed at all**, which is why this is simpler than `@Split`: the delegate does the typing.
- `write(out, value)` calls `delegateAdapter.toJsonTree(value)`, then for each `@Flatten` field wraps
  every element back into a fresh one-key `JsonObject` under `value()`, and writes the result through
  the `JsonElement` adapter. This is the inverse rewrite, and it is what buys round-trip fidelity.

Serialized-key resolution must match the rest of the library: `@SerializedName` value if present,
otherwise the field name (`SplitTypeAdapterFactory.java`:197-199). A field that *also* carries
`@SerializedPath` is out of reach entirely, in either registration order - `@SerializedPath` performs
no flat-key rewrite on the read side and binds its own fields by a route that bypasses the delegate
chain, so `@Flatten` is never consulted for such a field. §6.5 works that through from source. The
pair must throw at `create` time, on the same rule as `@Capture`.

Roughly 150 lines including the field-info record, in one new file plus one line in
`GsonSettings.defaults()`. That is exactly the `small` row of the effort scale: "a new annotation
whose whole implementation is a new `TypeAdapterFactory` registered in `GsonSettings`".

### 6.5 Ordering and interaction with the existing factories

`GsonSettings.java`:249-256 registers, in list order: `CaseInsensitiveEnum`, `Optional`, `Split`,
`SerializedPath`, `Lenient`, `Capture`, `Collapse`, `PostInit`. `GsonBuilder.create()` **reverses**
the user factory list before handing it to `Gson`, so the last registered is consulted first. The
resulting wrap chain, outermost first, is:

```
PostInit -> Collapse -> Capture -> Lenient -> SerializedPath -> Split -> Optional -> CaseInsensitiveEnum -> ... -> Reflective
```

`@Flatten` belongs **between `SerializedPath` and `Split`** - that is, inserted into the
`GsonSettings` list between `Split` and `SerializedPath`, at list index 3. The index is less
load-bearing than it looks, and the reasons are these:

- The inner bound is real: it must sit *outside* the reflective adapter, because it rewrites the tree
  the reflective adapter is about to read. There is no reason to place it inside `Split` or
  `Optional`, which operate on unrelated field shapes.
- The outer bound is unconstrained at the only adoption site. `Currencies.essence` carries `@Flatten`
  and nothing else, so no outer factory has any opinion about it. Index 3 is the conservative pick -
  it keeps the new factory as deep as possible, so it can only ever see the tree the outer factories
  chose to hand down, and its blast radius is correspondingly small.
- Exactly one composition would move it. If a site ever wants `@Flatten` together with `@Lenient`,
  `@Flatten` has to be the outer of the two, which means index 5, between `Lenient` and `Capture`.
  Nothing else in the chain objects to that move; it is a one-line change if the site appears.

Four interactions, two of which are hard limitations and must be documented on the annotation:

**`@Capture` - does not compose, by construction.** `CaptureTypeAdapter.read`
(`CaptureTypeAdapterFactory.java`:257-366) buffers the enclosing object, *removes* every key it
claims from the root object, builds the maps itself, and only then calls
`delegateAdapter.fromJsonTree(knownObject)` with those keys already gone. `Capture` is outer,
`Flatten` is inner, so a `@Flatten` on a `@Capture` field would never see its entries. **`@Capture`
and `@Flatten` on the same field are mutually exclusive** and the factory should fail loudly at
`create` time rather than silently no-op. This is exactly the mechanical reason
`HeartOfTheForest.BiomeWhispers.tiers` cannot use `@Flatten`, independent of the design argument for
keeping `Tier`.

**`@SerializedPath` - does not compose, in either order.** This is the one that is easy to get wrong,
so it is read straight off the source rather than assumed. `SerializedPathTypeAdaptorFactory.read`
(`:99-144`) buffers the original node and hands it to `delegateAdapter.fromJsonTree(outerJsonElement)`
**untouched** (`:103`); only afterwards does it walk each `@SerializedPath` field and assign it with
`this.getGson().fromJson(innerJsonElement, accessor.getGenericType())` (`:132`) followed by a
reflective `set` (`:135`). Two consequences. There is no flat-key rewrite on the read side at all -
the flat key exists only in `write` (`:62-93`), which removes it and re-nests it down the path. And
`gson.fromJson(...)` is a fresh **top-of-chain** lookup for the *field's own type*, so a
`@SerializedPath` field never binds through the enclosing class's delegate chain, which is where a
`@Flatten` adapter for that class would sit. Register `@Flatten` inside and it inspects the same
untouched tree, finding no root-level key; register it outside and it inspects that tree even
earlier, with the same result. Either way the annotation is a silent no-op. **`@SerializedPath` and
`@Flatten` on the same field are mutually exclusive**, and `create` must throw, exactly as for
`@Capture`. (§14.1 states the same read mechanism from the reach-back side.)

**`@Lenient` - does not compose at index 3, but the fix is a move rather than an exclusion.** At
index 3 `Lenient` is outer, so it sees the *un*collapsed values and would divert every wrapper object
to overflow as type-incompatible. That is the wrong way round. Index 5 puts `@Flatten` outside
`Lenient`, so the collapse happens first and `Lenient` is handed values it can type. The pair is
therefore resolvable - it just costs the deeper, more conservative default. Recommendation: ship at
index 3, document the exclusion on the annotation, and make the move only when a real site wants the
pair.

**`@Collapse` and `PostInit` - no interaction.** Both are outer and operate on the whole object;
`@Flatten` has finished rewriting before either is reached. `@Split` and `@Optional` are inner and
operate on different field shapes.

### 6.6 Failure modes and malformed input

Stated as behaviour, because "what happens when the wrapper is missing" is the question that decides
whether this annotation is safe to ship.

| Input for a `@Flatten("current") ConcurrentMap<String, Integer>` | Behaviour |
| --- | --- |
| `{"WITHER": {"current": 1955}}` | `{WITHER: 1955}` - the intended path |
| `{"WITHER": {"current": 1955, "total": 9000}}` | `{WITHER: 1955}`; `total` is **dropped**, exactly as `getEssence()` drops it today |
| `{"WITHER": {}}` | element left untouched, then fails the declared value type; Gson's reflective map adapter throws `JsonSyntaxException` unless `@Lenient` is present |
| `{"WITHER": 1955}` - already unwrapped | element left untouched and binds correctly, because it is already the collapsed shape |
| `{"WITHER": null}` | left untouched; binds as a null map value |
| field value is a JSON array, not an object | the whole field is passed through untouched to the delegate |
| annotated field is not a `Map` or `Collection` | `create` skips it; the annotation is inert. Prefer a `create`-time exception - a silently ignored annotation is the failure mode this pack keeps finding |
| annotated field is also `@Capture` | must throw at `create` time (§6.5) |
| annotated field is also `@SerializedPath` | must throw at `create` time - the annotation would otherwise be a silent no-op in either registration order (§6.5) |
| annotated field is `transient` | skipped, consistent with every existing factory |

The row that deserves a decision rather than a default is the third. `SplitTypeAdapterFactory.java`:
160-161 swallows a malformed value into an empty `catch (Exception ex) {}` - the same anti-pattern
`f01-postinit-aborts-silently` and `f02-postinit-silent-swallow` identify as the reason four defects
shipped unnoticed. **`@Flatten` must not copy that.** The safe design is: leave a non-conforming
element untouched and let the delegate's own typing decide, so a wrong shape surfaces as a normal
Gson error at the field rather than as a silently absent entry.

On write, an element that is `null` is written as JSON null rather than as `{"current": null}`, so a
null round-trips as a null. That is a deliberate asymmetry and it should be in the javadoc.

### 6.7 What it subsumes, and what subsumes it

**It subsumes nothing.** That is a mark against it, and it should be said plainly.

- Not `@SerializedPath`. That addresses a *static* path from the enclosing object; `@Flatten`
  addresses the *value* side of every entry of a collection. They do not overlap - `03`'s Group A and
  Group B are disjoint sets of sites - and §6.5 shows they cannot even be stacked on one field.
- Not `@Capture`. `@Capture` decides which keys become entries; `@Flatten` reshapes what an entry's
  value is. §6.5 shows they cannot even run on the same field.
- Not `@Collapse`. `@Collapse` moves an entry's *key* into the value object; `@Flatten` reaches into
  the value and takes a member out. Opposite directions.
- Not `@Lenient`. Different failure model entirely.

And one thing subsumes it, at the only site that justifies it: stock gson `@JsonAdapter`, next.

The class-level variant is declined explicitly, so it is not rediscovered. `03-value-shape-collapse.md`
§2 option 2 proposes `@Flatten` on a *class*, meaning "bind a bare scalar into my sole field when the
incoming JSON is not an object". It is technically sound and would immunise every single-field value
class against Hypixel wrapping or unwrapping a scalar between API versions. It is declined because
the fixture shows no such flip anywhere, it removes no code and no class, and it would give one
annotation name two unrelated meanings - a field-side collapse and a class-side widening. If the flip
is ever observed, it is a new registry row, not a second element on this one.

### 6.8 The cheaper alternative - stock `@JsonAdapter`

**Loudly: gson already lets a single field carry its own adapter, and the project is on gson 2.11.0
where that has worked since 2.3.** `com.google.gson.annotations.JsonAdapter` is honoured by
`ReflectiveTypeAdapterFactory` when it builds a bound field, so a field-scoped shape transform needs
no annotation in `gson-extras`, no factory registration in `GsonSettings`, no JitPack build and no
re-pin. It is `trivial` on the effort scale where `@Flatten` is `small`, and `small` is the library
floor that can never be beaten.

The consumer-side form at the one site:

```java
@JsonAdapter(EssenceAdapter.class)
private @NotNull ConcurrentMap<String, Integer> essence = Concurrent.newMap();
```

with `EssenceAdapter` a `TypeAdapter<ConcurrentMap<String, Integer>>` in
`api/simplified/hypixel/common/` that reads `{"WITHER": {"current": 1955}}` and writes it back.

Honest accounting, because this is the comparison that decides the entry and it is closer than it
looks:

| Option | Consumer lines | Library lines | JitPack cycles | Reusable |
| --- | --- | --- | --- | --- |
| Do nothing | 0 | 0 | 0 | n/a |
| `@JsonAdapter` + `EssenceAdapter` | -10, +~25 | 0 | **0** | no - one adapter per key name |
| `@Flatten` | -10 | +~190 | **1** | yes |

`@JsonAdapter` wins on cycles and loses on everything else. Its adapter class cannot be parameterised
by the wrapper key, because `@JsonAdapter` takes only a class literal, so every new wrapper key needs
a new adapter class. And a bespoke hand-written `TypeAdapter` in a DTO package is precisely the
hand-rolled deserialization this pack exists to delete - it would be the only one in `response/`.

**Doing nothing also scores better than it looks.** `essence` is `@Getter(AccessLevel.NONE)` and its
only public surface is `getEssence()`, which already returns the collapsed type. The "lying type"
`f03-mapvalue-single-key` complains about is visible inside one 26-line file and nowhere else.

### 6.9 Verdict

**Adopt narrowly, and only as a rider on a library cycle that is already being spent.**

The concept is right, the design is small and self-contained, the round-trip story is complete, and
it fills a genuine gap that no existing annotation reaches. It has **one** unambiguous adoption site.
One site is a thin justification for a JitPack publish, a wait for green, a dependency-pin edit and a
consumer rebuild - `00-conventions.md` §4 is explicit that round trips dominate.

The decision rule, stated so `20-implementation-plan.md` can act on it without re-opening the
argument:

- If `d10-fallback` is accepted, `gson-extras` is being published anyway. Ship `@Flatten` in the same
  commit and adopt it at `Currencies.essence`. Marginal cost is the review, not the cycle.
- If `d10-fallback` is declined, **decline `@Flatten` too**, and take the two zero-cost neighbours
  instead: retype `Dungeons.classMap` to `ConcurrentMap<DungeonClass.Type, DungeonClass>`
  (`f03-dungeons-classmap-funnel`) and put `@Getter(AccessLevel.NONE)` on
  `HeartOfTheForest.BiomeWhispers.tiers` (`f03-biomewhispers-tier`). Those two deliver more deleted
  lines than `@Flatten` does, at zero library cost, and they are the reason `@Flatten`'s adoption
  count is 1 rather than 3.
- Do not build it for `HeartOfTheForest`. §6.5 shows it cannot work there and §6.1 shows it should
  not.

One prerequisite either way: a round-trip test. Bind `Currencies` from the fixture, serialize it, and
assert the output object is byte-equal to the input for the `essence` key. `@Lenient` and `@Collapse`
both hold that line and this must too.

## 7. d10-fallback - `@Fallback`

- **Registry entry:** `@Fallback` - "supplies a default when the key is absent or the value fails to
  bind, replacing sentinel constants plus `getOrDefault` accessors"
- **Verdict:** adopt narrowly - **cut to the failed-bind half, and cut again to enum constants**
- **Category:** `correctness`
- **Answers findings:** `f06-enum-null-clobber`, `f06-capture-null-enum-key`, `f03-enum-unknown-null`,
  `f04-enum-null-fallback`; partially `f04-enum-of-parsers`. Explicitly does **not** answer
  `f06-completedat-zero-sentinel` or `f04-lookup-sentinel-drift`
- **Cheaper alternative:** none - this is the one gap with no stock equivalent
- **Library change:** existing factory edit (`CaseInsensitiveEnumTypeAdapterFactory`), plus one
  additive annotation file
- **Adoption sites today:** **17** `@NotNull` enum fields plus **7** enum-keyed `@Capture` maps
- **Effort:** `medium` per `00-conventions.md` §4, because it edits an existing factory - but see
  §7.4 for why the regression surface is nil

### 7.1 The problem it removes and the real sites

`CaseInsensitiveEnumTypeAdapterFactory.java`:82 is the whole defect:

```java
return nameToConstant.get(in.nextString().toUpperCase());
```

A plain map miss yields `null`. Gson 2.11's reflective `BoundField` then does
`if (fieldValue != null || !isPrimitive) field.set(...)`, and an enum is not primitive, so the `null`
is written straight over the field initializer. Every carefully chosen `UNKNOWN` / `NONE` / `COMMON`
default in this module was written to express "the API sent something I do not know" and **none of
them is ever reached**.

Proved, not inferred. `06-structural-duplication.md` ran this through the real `Gson`:

```
CrimsonIsle isle = gson.fromJson("{\"selected_faction\":\"cultists\"}", CrimsonIsle.class);
assertThat(isle.getSelectedFaction(), is(nullValue()));   // passes
```

Fourteen `@NotNull` enum fields carry a default and are exposed:

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

Three more have no default at all and are therefore already `null` whenever upstream sends something
new: `Banking.java`:24, `CommunityUpgrades.java`:59, `DungeonChest.java`:20. The sharpest instance is
`ActiveCommission.Status`, an enum whose entire body is one constant, `NOT_STARTED` - any commission
that is in progress or claimed binds to `null` on a `@NotNull` field.

**The same one line poisons seven `@Capture` maps**, which is the finding that decides the design.
`f06-capture-null-enum-key` proved it:

```
PROBE kuudra tiers = {null=4, BASIC=1}
```

for input `{"none":1,"brand_new_tier":4}`. No exception. `ConcurrentMap` accepts the entry, and
anything shaped like `map.keySet().stream().map(Enum::name)` throws far from the decode that caused
it. The seven sites are `Kuudra.java`:19 and :21, `Dojo.java`:16 and :18,
`HeartOfTheMountain.java`:50, `TrophyFishing.java`:25 and `Statistics.java`:89 - with
`TrophyFishing` the live risk, since Hypixel adds trophy fish and its enum lists 18 constants
including three `OBFUSCATED_FISH_n` placeholders.

Today all of this is latent: the fixture's values all resolve. It fires on a Hypixel content update,
which is the worst possible time to discover it.

### 7.2 Full declaration

The design is deliberately smaller than the registry line. It is an **enum-constant marker**, not a
field-level default supplier, and §7.7 argues why.

```java
package dev.simplified.gson.annotation;

import dev.simplified.gson.factory.CaseInsensitiveEnumTypeAdapterFactory;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nominates the enum constant returned when an incoming JSON value matches no declared constant.
 * <p>
 * Without this marker an unrecognized value deserializes to {@code null}, which the reflective
 * binder then writes over the field's initializer - so a sentinel default such as {@code UNKNOWN}
 * never survives a value the enum does not declare. Marking one constant makes that sentinel the
 * bind result instead.
 * <p>
 * The marker also governs enum keys of a {@link Capture @Capture} map, where an unmatched key
 * otherwise becomes a {@code null} key.
 * <p>
 * At most one constant per enum may carry this annotation. An enum with no marked constant keeps
 * the {@code null} behaviour unchanged.
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

`ElementType.FIELD` is correct and is not a workaround - an enum constant *is* a field of the enum
type, which is exactly how `CaseInsensitiveEnumTypeAdapter`'s constructor already reaches
`@SerializedName` on constants (`CaseInsensitiveEnumTypeAdapterFactory.java`:51).

### 7.3 Usage before and after - `Dungeons` and `CrimsonIsle`

The consumer change is one line per enum, not one per field. That ratio is the entry's main
argument: 17 exposed fields and 7 exposed maps are covered by annotating roughly a dozen enums.

```java
// before - response/skyblock/member/dungeon/DungeonClass.java:53-73
public enum Type {

    UNKNOWN,
    HEALER,
    MAGE,
    BERSERK,
    ARCHER,
    TANK

}
```

```java
// after
public enum Type {

    @Fallback
    UNKNOWN,
    HEALER,
    MAGE,
    BERSERK,
    ARCHER,
    TANK

}
```

That single edit repairs three separate sites at once, with no change to any of them:
`Dungeons.java`:38-39 `selectedClass`, `FloorData.java`:108-109 `BestRun.dungeonClass`, and the
enum-keyed `@Capture` map that `f04-floordata-most-damage-switch` proposes for `FloorData`'s five
`most_damage_*` keys. `CrimsonIsle.Faction` (`CrimsonIsle.java`:149-150) gets `@Fallback` on `NONE`
and `CrimsonIsle.java`:26-27 stops being nullable. `Kuudra.Tier` gets it on `BASIC`, which fixes both
`@Capture` maps at `Kuudra.java`:19 and :21 in one line.

Two enums need a constant before they can be marked - `Banking.Action` and `DungeonChest.Type` have
no sentinel at all, so this is a two-line change there rather than one.

**And one site needs no annotation whatsoever, which is worth shouting about.**
`f06-boardquest-complete-status` is not an unknown-value problem, it is a *misspelled* constant:
`BoardQuest.Status` declares `COMPLETED` while the wire vocabulary is `COMPLETE` x791, `ACTIVE` x19,
`INACTIVE` x1, and `COMPLETED` appears nowhere in the fixture. That is stock gson - either rename the
constant or add `@SerializedName("COMPLETE")`, both of which
`CaseInsensitiveEnumTypeAdapterFactory.java`:51-58 already honours, including `alternate`. It costs
three lines and zero library work, and it must land **before** `f06-objective-status-shape`'s
catch-all, which would take the blast radius from 5 objects to 800. Do not wait for `@Fallback` to
fix a typo.

### 7.4 How the factory implements it

**No new factory.** The whole implementation is a resolved-once field on the existing
`CaseInsensitiveEnumTypeAdapter` plus one branch in `read`.

Constructor, alongside the existing `@SerializedName` scan at
`CaseInsensitiveEnumTypeAdapterFactory.java`:47-64 - the loop already calls
`enumClass.getField(constant.name())`, so the annotation lookup is free:

```java
if (enumClass.getField(constant.name()).isAnnotationPresent(Fallback.class)) {
    if (this.fallback != null)
        throw new JsonException("Enum '%s' declares more than one @Fallback constant", enumClass.getName());

    this.fallback = constant;
}
```

Read, replacing `:82`:

```java
E constant = nameToConstant.get(in.nextString().toUpperCase());
return constant != null ? constant : this.fallback;
```

`this.fallback` is `null` when no constant is marked, so the expression degrades to today's exact
behaviour. **That is the point of the design.** The obvious alternative - "on a miss, return the
constant named `UNKNOWN`, or the first declared constant" - is a behaviour change for every enum in
every module already pinned to `gson-extras`, and `00-conventions.md` §4 prices that at a full
regression pass beyond this repo. An opt-in marker collapses the regression surface to the enums that
opt in, which are all in this module. It still rates `medium`, because the scale rates *any* edit to
an existing factory at `medium`, and that rating should not be argued down - but the risk inside that
rating is close to zero and the plan should say so.

Cost: roughly 8 changed lines in one existing file, plus one new 40-line annotation file, plus one
line of import. One JitPack cycle, one re-pin.

### 7.5 Ordering and interaction with the existing factories

`CaseInsensitiveEnumTypeAdapterFactory` is registered first in `GsonSettings.java`:249, so after
`GsonBuilder`'s reversal it is the *innermost* of the eight - a leaf adapter for enum types, reached
by every other factory through `gson.getAdapter(...)`. That position is what makes this fix reach so
far for so little:

- **Enum-valued fields.** The reflective adapter calls it directly. Fixed.
- **Enum-keyed `@Capture` maps.** `CaptureTypeAdapterFactory` resolves each captured key through the
  gson adapter for the map's key type, which is this adapter. Fixed - all seven sites in
  `f06-capture-null-enum-key`, with no change to `CaptureTypeAdapterFactory` at all. This is the
  reason the design lives in the enum adapter rather than on the field; see §7.7.
- **`@Split` pairs.** `SplitTypeAdapterFactory.java`:153-154 deserializes each half through
  `gson.fromJson(new JsonPrimitive(part), type)`. `TrophyFishing.lastCaught` is a
  `PairOptional<TrophyFish, TrophyFish.Tier>` bound with `@Split("/")`, so both halves route through
  this adapter and both are fixed.
- **`@Collapse` / `@Key`.** `Slayers` keys are `String`, so no interaction today, but an enum `@Key`
  would be covered by the same path.
- **`PostInit`.** No interaction - it runs after every field is bound.

There is no ordering hazard, because the change makes an innermost leaf adapter return a non-null
value where it previously returned `null`. Nothing in the chain branches on that null except the
reflective binder, which is the thing being fixed.

### 7.6 Failure modes and malformed input

| Situation | Behaviour |
| --- | --- |
| Value matches a constant, any case | unchanged - the constant |
| Value matches a `@SerializedName` value or `alternate` | unchanged - the constant |
| Value matches nothing, enum has a `@Fallback` constant | the fallback constant |
| Value matches nothing, enum has no `@Fallback` constant | `null`, exactly as today |
| JSON `null` | `null`, unchanged - `:77-80` returns before the lookup, and absence should stay distinguishable from an unrecognized value |
| Two constants marked `@Fallback` | `JsonException` at adapter construction, which is the first decode of that type |
| `@Fallback` on a non-enum field | inert. Consider rejecting it with a `create`-time check, since a silently ignored annotation is the exact failure class this pack keeps finding |
| Enum-keyed `@Capture` map, several unmatched keys | **all collapse onto the fallback constant, so later entries overwrite earlier ones** |

The last row is the one honest cost of this design and it must not be buried.
`f06-capture-null-enum-key` raises it directly: replacing a `null` key with a shared `UNKNOWN` key
turns "one poisoned entry" into "silent information loss across several entries". Two answers, and
the design takes the first:

1. **Accept it.** A `null` key in a `ConcurrentMap<Tier, Integer>` is strictly worse - it also loses
   the data, it additionally breaks every `keySet()` iteration, and it does so at a call site far
   from the decode. Losing several unknown tiers into one `UNKNOWN` bucket is a visible, typed,
   iterable outcome.
2. Give `@Capture` a `skipUnmatchedKeys` element so the entry is dropped instead. Rejected in
   `d10-capture-unmatched`: it loses the data *and* breaks round-trip fidelity, which `@Lenient` and
   `@Collapse` both preserve.

A serialization note that decides nothing but must be stated: writing is unaffected.
`constantToName` at `:72` maps the fallback constant to its own name, so an `UNKNOWN` that arrived
from an unrecognized value serializes as `"UNKNOWN"` and **not** as the original wire value. Round
trip is therefore lossy for unrecognized enum values - and it was already lossy, because `null`
serializes as JSON null today. This is not a regression, but anyone claiming round-trip fidelity for
`@Fallback` would be overclaiming.

### 7.7 The cheaper alternative - an enum-level sentinel convention

Three shapes were considered before landing on the enum-constant marker. Recording all three, since
`03`, `04` and `06` each reached for a different one.

**Shape 1 - `@Fallback` on the field**, as the registry line reads
(`@Fallback(DungeonClass.Type.UNKNOWN)` on `Dungeons.selectedClass`). Rejected on three counts. It
needs a whole new enclosing-class-scanning factory in the `SplitTypeAdapterFactory` mould, because a
`TypeAdapter` cannot decline to write a field and the repair therefore has to happen after
`delegate.fromJsonTree`. It needs 17 annotations instead of ~12. And decisively, **it cannot reach a
map key**, so all seven `@Capture` sites in `f06-capture-null-enum-key` stay broken. The field is the
wrong place to express a property of the enum.

**Shape 2 - a naming convention in the factory**: on a miss, return the constant named `UNKNOWN` or
`NONE` if one exists. Zero new annotation, one changed line. Rejected because it is a silent
behaviour change for every enum in every module pinned to `gson-extras` - any consumer relying on
`null` to detect an unrecognized value would start seeing a constant with no compile-time signal, and
`00-conventions.md` §4 requires a regression pass across siblings for exactly this. It also guesses:
`Kuudra.Tier`'s sentinel is `BASIC`, `SkyBlockAuction.rarity`'s is `COMMON`, `Crystal.State`'s is
`NOT_FOUND`. A convention keyed on the name `UNKNOWN` would miss four of the fourteen sites.

**Shape 3 - the enum-constant marker**, adopted. Explicit, opt-in, one line per enum, reaches field
values and map keys and `@Split` halves through one code path, and leaves un-annotated enums
bit-identical.

**The absent-key half of the registry line is cut entirely.** `04-accessor-boilerplate.md` is right:
Java field initializers already supply a default when a key is absent, and every `@NotNull` field in
`response/` has one. An annotation for that half would duplicate the language.

**And `f04-lookup-sentinel-drift` is explicitly out of scope.** Those six helpers
(`Dungeons.getClass`, `Toolkit.getTool`, `Skills.getSkill`, ...) are *lookup-time* misses on a
fully-populated map, long after binding. No bind-time annotation addresses them, and inventing one
would be a `TypeAdapterFactory` solving a problem with nothing to do with JSON. The real defect there
is the `@NotNull` lie at `Skills.java`:27, whose body is `matchFirstOrNull`, and it is a one-line
consumer fix.

### 7.8 What it subsumes

- **Subsumes the bind-path half of the `of(String)` idiom** (`f04-enum-of-parsers`). Five enums each
  hand-write a `values()` stream that falls back to a sentinel; the survey proved two of the three
  `of` bodies are never called at all and the remaining callers are `String`-to-enum conversions in
  Java code, not on the bind path. `@Fallback` covers the bind path properly, so those helpers should
  shrink to the shared static helper `f04-enum-of-parsers` proposes rather than growing.
- **Does not subsume `@Alias`.** Different problem, and stock gson already owns it (`d10-alias`).
- **Does not subsume `f06-completedat-zero-sentinel`.** That is `completed_at: 0` binding to
  `Optional.of(1969-12-31T19:00:00)`, so `isPresent()` is always true - a *successful* bind of a
  sentinel value, not a failed one. Widening `@Fallback` to "treat this value as absent" is a second,
  unrelated concept on one name. Take `06`'s honest `trivial` fix instead: declare
  `BoardQuest.completedAt` as `@NotNull SkyBlockDate.RealTime` with an epoch default and let callers
  test it. Five sites, zero library cost.
- **Does not subsume `@Split`.** `f03-kuudra-combat-range` is a separate adoption of an annotation
  that already exists: `Kuudra.SearchSettings.combat_level` is an `Optional<String>` holding `"5-10"`
  that `getCombatLevel()` splits and parses by hand at `Kuudra.java`:46-50, throwing on a malformed
  value. `@Split("-")` onto a `PairOptional<Integer, Integer>` binds it at bind time, yields
  `empty()` instead of throwing, and takes the snake_case Java field name with it. Zero library
  change; adopt it independently.

### 7.9 Verdict

**Adopt, narrowed as above.** It is the only entry in part one with no stock equivalent, the only one
whose findings were proved by executed probes in three separate surveys, and the only one that fixes
live `correctness` defects rather than deleting lines. Twenty-four exposed sites against roughly
twelve one-line consumer edits and eight changed library lines is the best ratio in the pack.

Three conditions on the acceptance:

1. **Land the consumer-side typo fixes first.** `f06-boardquest-complete-status` (`COMPLETE`, plus
   the unmodelled `INACTIVE`) and `f06-serialized-name-misses` (`starting_big` to `starting_bid`,
   `CommissionData.totalCompleted`) are `trivial`, consumer-only, and are real data loss today. They
   must not wait behind a JitPack cycle.
2. **Write the confirming test before the library change.** Decode
   `{"selected_dungeon_class": "necromancer"}` into `Dungeons` and assert the field. Both `03` and
   `04` flag that nothing in `MemberDtoMappingTest` covers the bind path for enums - the only
   `UNKNOWN` assertions there (`:104-105`) exercise `getOrDefault` lookups. That test is the whole
   cost of confirming the finding and it is also the regression guard.
3. **Grep the sibling modules for code relying on a `null` enum before landing**, even though the
   opt-in design means only annotated enums change. The habit is cheap; the alternative is finding
   out from a consumer.

## 8. d10-alias - `@Alias`

- **Registry entry:** `@Alias` - "accepts more than one JSON key for the same field, for API keys
  that were renamed upstream or that vary by profile age"
- **Verdict:** reject
- **Category:** `naming`
- **Answers findings:** `f06-boardquest-complete-status`, `f06-serialized-name-misses` (context only)
- **Cheaper alternative:** `@SerializedName(value = "...", alternate = {"..."})` - stock gson
- **Library change:** none
- **Adoption sites today:** 3, all already expressible
- **Effort:** `trivial` (a decision, not a change)

### 8.1 Stock gson already ships this

**Loudly: `com.google.gson.annotations.SerializedName` has an `alternate()` element, and it has since
gson 2.4.** It accepts a `String[]` of additional names honoured on read while `value()` is used on
write, which is precisely the registry line, and there is nothing left to design.

It is honoured on both halves of this codebase's needs:

- **On fields**, by Gson's own `ReflectiveTypeAdapterFactory`. `gson-extras` already assumes this -
  `CaptureTypeAdapterFactory.discoverKnownKeys` (`:133-138`) reads `sn.alternate()` when it computes
  which keys a `@Capture` catch-all must *not* claim, so alternates already participate correctly in
  this library's own key arbitration.
- **On enum constants**, by `CaseInsensitiveEnumTypeAdapterFactory.java`:51-58, which registers every
  alternate into `nameToConstant` uppercased. So a renamed upstream enum value is a one-element edit.

A `dev.simplified.gson.annotation.Alias` would therefore be a second spelling of a stock feature,
bought with a JitPack cycle, and every future reader would have to learn which of the two to reach
for. That is negative value with no upside.

### 8.2 Usage on the two real sites

**`BoardQuest.Status`** (`response/skyblock/member/crimson/BoardQuest.java`:20). The enum declares
`COMPLETED`; the wire vocabulary is `COMPLETE` x791, `ACTIVE` x19, `INACTIVE` x1, and `COMPLETED`
occurs nowhere in the fixture. If the Java name is worth keeping, this is the whole fix:

```java
public enum Status {

    UNKNOWN,
    INACTIVE,
    ACTIVE,
    @SerializedName(value = "COMPLETE", alternate = "COMPLETED")
    COMPLETED

}
```

The better answer is still to rename the constant to `COMPLETE` and add `INACTIVE`, because a
compile error at `getStatus() == Status.COMPLETED` call sites is the desired failure mode. `alternate`
is shown here only to demonstrate that the registry row has no work left in it.

**`CrimsonIsle.Quests.kuudraBossDaily`** (`CrimsonIsle.java`:69) carries
`@SerializedName("kuuda_boss_daily")`. The key really is misspelled upstream - the fixture contains
`kuuda_boss_daily` - so the annotation is correct today. The day Hypixel fixes the spelling,
`alternate = "kuudra_boss_daily"` covers both without a code change on either side of the transition.
That is exactly the "renamed upstream" case the registry row was reserved for, and it is a
one-element edit.

**`Kuudra.Tier.BASIC`** (`Kuudra.java`:28-29) already uses `@SerializedName("NONE")` to bind the wire
value `none` onto a better Java name, which is the same mechanism doing the same job.

Note what none of this fixes: `SkyBlockAuction.java`:41 is `@SerializedName("starting_big")`, a typo
on *our* side, so every auction's starting bid binds to `0`. That is not an alias problem, it is a
one-character correction (`f06-serialized-name-misses`), and adding an alternate would preserve the
bug alongside the fix.

## 9. d10-capture-unmatched - `@Capture` unmatched-key policy

- **Registry entry:** none - this is the "extend an existing annotation" route
  `00-conventions.md` §6.1 asks proposals to name explicitly
- **Verdict:** decline - subsumed by `d10-fallback`
- **Category:** `correctness`
- **Answers findings:** `f06-capture-null-enum-key`
- **Cheaper alternative:** `@Fallback` on the key enum - one line per enum, no factory edit
- **Library change:** would be an existing factory edit plus a new element
- **Adoption sites today:** 7 (all of them enum-keyed)
- **Effort:** `medium`

### 9.1 The problem it removes and the real sites

Seven `@Capture` maps narrow an open JSON key space onto a closed enum, and the narrowing has no
failure policy: an unmatched key becomes a `null` map key. `Kuudra.java`:19 and :21,
`Dojo.java`:16 and :18, `HeartOfTheMountain.java`:50, `TrophyFishing.java`:25 and
`Statistics.java`:89. Proved in `f06-capture-null-enum-key`:

```
PROBE kuudra tiers = {null=4, BASIC=1}
```

### 9.2 Full declaration of the added element

For completeness, the shape that was considered:

```java
/**
 * Drops captured entries whose key cannot be converted to the map's declared key type, instead of
 * inserting the unconvertible key.
 *
 * @return {@code true} to drop unconvertible entries
 */
boolean skipUnmatchedKeys() default false;
```

### 9.3 How the factory implements it

`CaptureTypeAdapterFactory.read` (`:257-366`) already resolves each captured key through the gson
adapter for the map's declared key type before inserting. The element would add one branch there: on
a `null` conversion, either insert as today or `continue`. Roughly five lines - but in the busiest
factory in the library, shared by twelve files in this module alone.

### 9.4 Failure modes and the information-loss objection

`skipUnmatchedKeys = true` **loses the entry outright and breaks round-trip fidelity**, which
`@Lenient` and `@Collapse` both preserve and which `00-conventions.md` §4 names as a cost that must
be stated. An unmatched key would vanish from the serialized document. The alternative,
`@Fallback`, keeps the entry, keeps it typed, keeps it iterable, and its cost is bounded and known:
several unmatched keys collapse onto one constant and later entries overwrite earlier ones (§7.6).

There is a third option nobody proposed and it should be named so it is not mistaken for a gap:
divert unmatched keys to the `@Capture` overflow that already exists
(`CaptureTypeAdapterFactory.java`:82, :387). That preserves round-trip fidelity *and* loses nothing,
but it needs a way to read the overflow back - and `@Extract` addresses a single named key, not "every
entry that failed key conversion". That is the same missing capability `d10-lenient-overflow` runs
into, and it is declined for the same reason: one general mechanism, two thin sites.

### 9.5 Verdict

**Decline.** `d10-fallback` fixes all seven sites through the enum adapter with no change to
`CaptureTypeAdapterFactory` at all, which keeps the busiest factory in the library out of the blast
radius. Two changes competing for the same seven sites is one change too many; take the one that
edits the smaller file.

Reopen only if a `@Capture` map appears with a non-enum key type whose conversion can fail - none
exists today; all seven are enum-keyed.

## 10. d10-capture-value-grouping - `@Capture` grouping one level deeper

- **Registry entry:** none - an element addition to `@Capture`
- **Verdict:** decline
- **Category:** `value-shape-collapse`
- **Answers findings:** `f03-skilltree-capture-holder`
- **Cheaper alternative:** keep the carrier class - it costs 8 lines
- **Library change:** would edit `CaptureTypeAdapterFactory`'s grouping selection
- **Adoption sites today:** 1
- **Effort:** `medium`

### 10.1 The problem it would remove

`response/skyblock/member/SkillTree.java`:16-17 and :32-39. `nodes` binds as
`ConcurrentMap<String, Skill>` where `Skill` declares nothing at all except a single annotated field:

```java
@Getter
@NoArgsConstructor
public static class Skill {

    @Capture
    private @NotNull ConcurrentMap<String, Node> entries = Concurrent.newMap();

}
```

`Skill` is not a domain concept. It is a **carrier for an annotation**. `@Capture` selects its
grouping mode from the declared *value* type (`Capture.java`:127-146,
`Capture.Grouping.AUTO`), so the only way to affix-group a map that is itself a map value is to
interpose a class with a `@Capture` field on it. The caller wants
`ConcurrentMap<String, ConcurrentMap<String, Node>>` and writes
`getNodes().get("mining").getEntries()` instead of `getNodes().get("mining")`.

The grouping itself is correct and is doing real work. `Node` at :43-50 uses `@SerializedName("")`
for `level` and `@SerializedName("toggle_")` for `enabled`, which is the affix contract documented at
`Capture.java`:37-47, so the fixture's `skill_tree.nodes.mining` -
`{"core_of_the_mountain": 10, "toggle_core_of_the_mountain": true, "mining_speed": 50,
"toggle_mining_speed": true, ...}` - folds each pair into one `Node`.

### 10.2 Why it is declined

Payoff is **one class and eight lines**, plus one level of caller indirection. Cost is a change to
the grouping-selection logic inside `CaptureTypeAdapterFactory`, which twelve files in this module
already depend on and which every other consumer of `gson-extras` shares. That is the worst
payoff-to-blast-radius ratio in part one, and it is not close.

It is recorded rather than dropped because it is real evidence about the existing annotation, not
about a missing one: **`@Capture`'s value-type-drives-mode rule has a structural cost.** The rule buys
a lot - it is why `@Capture` needs no mode element in the common case - and it is paid for exactly
once, here, with an eight-line class. That is a good trade and the design document should say so
rather than treating the carrier class as residue.

Nothing would reopen this at one site. A second and third carrier class appearing would.

## 11. d10-lenient-overflow - `@Lenient` typed overflow

- **Registry entry:** none - an element addition to `@Lenient`
- **Verdict:** decline - take the free partial instead
- **Category:** `value-shape-collapse`
- **Answers findings:** `f03-questrewards-mixed-values`; the free partial also lands
  `f03-object-escape-hatches`
- **Cheaper alternative:** `@Lenient` and `@Extract` exactly as they ship today
- **Library change:** would edit `LenientTypeAdapterFactory`, which seven files depend on
- **Adoption sites today:** 1 for the element; 3 for the free partial
- **Effort:** `medium` for the element; `small` for the partial, with no library change

### 11.1 The problem it would remove

`response/skyblock/member/crimson/CrimsonIsle.java`:65-66. `quest_rewards` is one JSON object
carrying two unrelated maps interleaved by value type:

```json
{"KADA_LEAD": 10, "crimson_isle_kill_barbarian_duke_x_c": "KADA_LEAD",
 "MOOGMA_PELT": 2, "crimson_isle_lavahorse_c": "MOOGMA_PELT",
 "GAZING_PEARL": 2, "crimson_isle_fetch_magmag_b": "GAZING_PEARL"}
```

`<itemId> -> <count>` with `int` values, and `<questId> -> <itemId>` with `String` values. The field
is `ConcurrentMap<String, Object>` and neither map is usable without a cast. `@Lenient` can divert one
value type to overflow, but `@Extract` addresses a **single named key**, so there is no way to pull
"every `String`-valued entry" back out, and two fields cannot both claim `quest_rewards` because Gson
rejects duplicate serialized names.

The element would be something like `@Extract`'s inverse - a sibling field that receives the *whole*
typed remainder of another field's overflow rather than one named entry of it.

### 11.2 The free partial that makes it unnecessary

`@Lenient ConcurrentMap<String, Integer>` alone, with no library change whatsoever, types the reward
counts correctly today and parks the quest-to-item mapping in overflow where it round-trips but is
not readable. That is already better than `Object` for half the data, and it costs one annotation.
One site does not pay for an edit to the second-most-used factory in the library. **Take the
partial.**

The same reasoning delivers the rest of `f03-object-escape-hatches` at zero library cost, and those
three sites are worth more than the declined element:

```java
// Statistics.java:39-40 - 29 of 31 rift statistics stop being Object
@Lenient
@SerializedName("rift")
private @NotNull ConcurrentMap<String, Integer> riftStats = Concurrent.newMap();
@Extract("riftStats.west_vermin_vacuumed")
private @NotNull VerminVacuumed verminVacuumed = new VerminVacuumed();
```

`@Extract` addresses the lenient field by its **Java** field name, not its serialized name - the
`Bestiary.java`:33 precedent is `@Extract("kills.last_killed_mob")`. The two object-valued entries go
to overflow and round-trip; the other 29 become `Integer`. `OwnedPet.java`:40 `extra` becomes
`@Lenient ConcurrentMap<String, Long>`, and `HypixelPlayer.java`:42-43 `achievementsOneTime` becomes
`@Lenient ConcurrentList<String>`, which deletes the hand-written filter at :82-91 **and** its memo
field - the annotation does at bind time what that accessor does lazily.

The other six `Object` sites must keep `Object`: `CrimsonIsle.java`:68, :70, :93,
`VillagePlaza.java`:22, `CrystalHollows.java`:29 and :38 are all `{}` or `[]` in the fixture, and
guessing a type from an empty container is how `@Lenient` overflow silently fills up.

## 12. Part one summary

**Part one proposes one new annotation and one factory edit, and rejects or declines five other
things.** The data-shape axis of this pack is close to solved already, and the reason is worth
stating once: `@SerializedPath`, `@Capture`, `@Collapse`/`@Key`, `@Lenient`/`@Extract` and `@Split`
between them already cover every shape mismatch found in 133 files except two - the value side of a
map entry (`@Flatten`) and an unrecognized enum value (`@Fallback`). Most of the residue is not a
missing annotation; it is an existing annotation that was never applied.

### 12.1 Registry disposition

| Registry entry | Verdict | Because | Library change | Effort |
| --- | --- | --- | --- | --- |
| `@Fallback` | **adopt narrowly** | 24 exposed sites, three executed probes, no stock equivalent; cut to an opt-in enum-constant marker | `CaseInsensitiveEnumTypeAdapterFactory` edit + 1 annotation file | `medium` |
| `@Flatten` | **adopt narrowly, as a rider** | 1 unambiguous site; concept sound, cannot buy its own JitPack cycle | 1 annotation file + 1 new factory | `small` |
| `@Inline` | **reject** | `@SerializedPath` covers all 9 census holders - 11 classes with the `HypixelPlayer` pair - at zero library cost; largest holder has 3 fields | none | `trivial` decision |
| `@Delegate` | **reject** | stock Lombok already ships it and it still loses - deletes 40 of 135 lines, cannot rename, `lombok.experimental` | none | `trivial` decision |
| `@Alias` | **reject** | `@SerializedName(alternate = ...)` is stock gson and is already honoured on fields, on enum constants, and by `@Capture`'s key arbitration | none | `trivial` decision |
| `@Capture` unmatched-key element | **decline** | subsumed by `@Fallback` through the enum adapter; keeps the busiest factory out of the blast radius | none | `medium` avoided |
| `@Capture` value-grouping element | **decline** | 1 site, 8 lines, against a change to grouping selection shared by 12 files | none | `medium` avoided |
| `@Lenient` typed-overflow element | **decline** | 1 site; the free partial types half of it today | none | `medium` avoided |

Adoptions of annotations that **already exist**, none of which needs a library cycle and all of which
outweigh the two new annotations combined:

| Existing annotation | Sites | Payoff |
| --- | --- | --- |
| `@SerializedPath` | 9 census holders / 11 classes unioned, 13 fields (`f04-nested-holder-idiom`, `f03-holder-collapse-serializedpath`) | ~108 net lines, ~130 with the `HypixelPlayer` pair; 1 file, 2 dead data paths recovered |
| `@Lenient` + `@Extract` | 3 (`f03-object-escape-hatches`) + 1 partial (`f03-questrewards-mixed-values`) | 29 rift statistics typed, 1 hand-written filter and its memo deleted |
| `@Capture` | 1 (`f04-floordata-most-damage-switch`) + 1 (`f06-objective-status-shape`) + 1 (`f01-dungeons-capture-grouping`) | 5 fields to 1 and a 10-line switch deleted; 792 unmapped keys to 0; one `PostInit` implementor retired |
| `@Collapse` + `@Key` | 1 (`f01-jacobscontest-collapse-key`) | 16 lines and one `PostInit` implementor |
| `@Split` | 1 (`f03-kuudra-combat-range`) | hand-rolled range parse deleted, throws becomes `empty()` |
| `@SerializedName` | 3 (`f06-boardquest-complete-status`, `f06-serialized-name-misses`) | every auction's starting bid stops binding to `0`; 791 quest statuses stop binding to `null` |
| stock `@Getter` | 3 (`f06-hoppity-unreadable-fields`) | 13 bound fields become readable for 3 added lines |

### 12.2 Sequencing and library cycles

**Part one asks for exactly one JitPack cycle.** Everything else is consumer-only and independently
revertable. Order matters, because two of the correctness fixes are prerequisites of structural work.

1. **Consumer-only correctness, no cycle.** `f06-boardquest-complete-status`,
   `f06-serialized-name-misses`, `f06-hoppity-unreadable-fields`,
   `f06-jacobscontest-derived-nontransient`. Four `trivial` fixes, one commit. The first is a
   prerequisite of step 3.
2. **Consumer-only shape adoption, no cycle.** `@SerializedPath` across the nine census holders
   (`d10-inline` §4.3), which also lands `f04-holder-private-type-leak`, then the
   `HypixelPlayer.Stats` / `Stats.SkyBlock` pair as a separate commit because it relocates
   `getProfiles()` and the nested `Profile` class rather than deleting a forwarder. Then
   `f03-object-escape-hatches`, `f03-kuudra-combat-range`, `f03-dungeons-classmap-funnel` and the
   two-line `HeartOfTheForest.BiomeWhispers` change - `@Getter(AccessLevel.NONE)` on `tiers` plus
   `getSpent(int)` moving to `this.tiers`. This is where most of part one's deleted lines come from.
3. **Consumer-only `@Capture` adoption, no cycle.** `f04-floordata-most-damage-switch`, then
   `f06-objective-status-shape` (blocked on step 1, or 791 of 811 entries bind to `null`). Both need
   a round-trip check that a filtered `@Capture` re-prefixes its keys on write - confirm against the
   existing `Kuudra.java`:18 user rather than assuming it.
4. **Write the enum bind-path test.** `{"selected_dungeon_class": "necromancer"}` into `Dungeons`,
   assert the field. No test covers this today.
5. **The single library cycle.** `@Fallback` (annotation file plus the
   `CaseInsensitiveEnumTypeAdapterFactory` edit) and, if it is being taken at all, `@Flatten`
   (annotation file plus factory plus the `GsonSettings` registration) in the *same* commit and the
   same JitPack build. Then one pin edit in this module. Then adopt: ~12 enum edits, and
   `Currencies.essence`.
6. **Round-trip tests for both.** `Currencies` byte-equality on `essence`; an unrecognized enum value
   serializing back as the fallback constant's name, documented as lossy.

If step 5 is not funded, drop `@Flatten` with it - §6.9 - and part one becomes a zero-library-cycle
change set that still delivers every line count in §12.1's second table.

One boundary this document does not own: `f01-postinit-aborts-silently` /
`f02-postinit-silent-swallow` also want a `gson-extras` change (make the empty catch log). If it
lands, it should ride the same commit as step 5. Its sequencing argument - do it *after* the consumer
conversions, or the logs are noise - belongs to part two.

### 12.3 Open questions carried into part two

Four things part one could not settle, listed so part two does not have to rediscover them.

1. **`@Flatten` needs a second adoption site or a shared cycle.** If part two accepts any library
   change, `@Flatten` rides it. If part two rejects everything, `@Fallback` is the only cycle in the
   pack and `@Flatten` should ride that or be dropped. Part two should state which.
2. **`@Fallback`'s name is now narrower than the registry line.** The row says "supplies a default
   when the key is absent **or** the value fails to bind"; §7.7 cut the absent-key half as duplicated
   by Java field initializers, and cut the field-level form as unable to reach a map key. If part two
   finds a derivation case wanting a field-level default, it should say whether that is the same name
   or a new row, not silently widen this one.
3. **The `f06-completedat-zero-sentinel` shape has no home yet.** "Treat value X as absent" is a
   real, five-site pattern and it is neither `@Fallback` nor any part-two entry as currently framed.
   Part one recommends the honest `trivial` consumer fix; if part two disagrees it owns the argument.
4. **Ancestor-relative `@SerializedPath` is part two's, but it is a *shape* capability.**
   `f02-kuudra-sibling-push` needs a path that can address a key outside the current node, and
   `02-parent-access.md` §4.4 argues it covers four of the six bind-path reach-back reads at lower
   lifecycle cost than `@Owner`. Part one deliberately did not design it, even though it is an
   extension of an annotation part one otherwise owns, because its justification is entirely
   reach-back evidence. Part two should note that it is an edit to
   `SerializedPathTypeAdaptorFactory`, not a new factory, and price it accordingly.

# PART TWO - Reach-back and derivation annotations

*Authored separately. The headings below are fixed - fill them in place, do not rename, split or
merge them, and do not edit part one. `d10-flatten` §6.9 and §12.3 carry four questions that part two
has to answer.*

## 13. d10-owner-parent - `@Owner` / `@Parent`

- **Registry entry:** `@Owner` / `@Parent` - "injects the enclosing or ancestor object into a nested
  object's field during bind, replacing a manual `initialize(parent)` reach-back"
- **Verdict:** decline
- **Category:** `parent-access`
- **Answers findings:** `f02-accessorybag-upstream`, `f02-skills-member-reachback`,
  `f02-profilestats-island-scalar`; constrained by `f02-postinit-bottom-up-order`. Explicitly does
  **not** answer `f02-kuudra-sibling-push` - see `d10-ancestor-path`
- **Cheaper alternative:** narrow the handover to the three values actually consumed and make every
  downstream read a lazy accessor - zero library change
- **Library change:** would be an additive annotation, a new top-down injection factory, and a
  serialization exclusion the factory must enforce itself
- **Adoption sites today:** **1**. `AccessoryBag` is the only bound type that needs it; `Skills` and
  `ProfileStats` are not deserialized at all
- **Effort:** `large`

### 13.1 The ordering hazard, and the one shape that survives it

The registry row hides the real question. It is not "`@Owner` or `@Parent`", it is **when does the
reference become usable**, and `f02-postinit-bottom-up-order` has already proved that the obvious
answer is wrong. This entry decides that question first, because the decision holds whether or not
the annotation is ever built - the consumer-side design in §13.3 obeys exactly the same rule.

**Decision: inject the reference during bind, defer every read through it to first access.**

The three candidates, and why the other two lose.

**Rejected - bind-time injection *and* bind-time reads.** The reference itself is perfectly safe: Gson
constructs the enclosing instance before it populates any field, so the identity a child would hold
is fixed for the object's whole life and never changes even as fields fill in behind it. What is not
safe is reading through it. The reflective adapter iterates the **JSON document**, so a child bound at
key position *n* can observe only parent fields whose keys appeared before *n*, and the fixture shows
that ordering is not contractual - across two profiles of one account `accessory_bag_storage` sits at
index 7 and index 3, `inventory` at 24 and 14, `nether_island_player_data` at 16 and 9, and `rift` is
present in one member and absent in the other. `AccessoryBag`'s three reads are exactly `inventory`,
`rift` and `nether_island_player_data`. Two members of one account would produce different magical
power from identical code. Partial success is the worst outcome available here, because it is
data-dependent and silent.

**Rejected - a post-bind top-down data pass.** Running the reads in a second phase after the root
completes is correct, and it is what a naive `@Owner` implies. It is also a new lifecycle guarantee
the whole pipeline must honour, which is the effort scale's `large` row verbatim, and
`05-cross-field-derivation.md` §4.4 shows it buys nothing that laziness does not already give for
free: ordering between derived values matters only while those values are computed eagerly.

**Chosen - reference at bind, values at first access.** This is the only option under which the
ordering hazard is structurally impossible rather than merely avoided. By the time any caller touches
an accessor the entire response has been decoded, so "is the parent populated yet" stops being a
question anyone can get wrong.

Where the reference can actually be installed is worth stating, because it is not where the registry
line implies. A child's `TypeAdapter` never sees the enclosing instance - `ReflectiveTypeAdapterFactory`
calls the field adapter's `read` and only afterwards does `field.set(instance, value)`, so there is no
handle to push down. The one implementable point is the **parent's** frame: after `delegate.read`
returns, walk the parent's `@Owner`-carrying fields and set the back-reference before returning.
`CollapseTypeAdapterFactory.injectKey` (`:239-255`) is the working precedent - it already mutates a
child field from the parent's adapter after the child is fully read.

That placement fixes two guarantees, and neither is negotiable by reordering `GsonSettings`:

- The reference is non-null from the instant the parent's read returns, and therefore **before the
  parent's own `postInit()`** - an owner factory sits inside `PostInit` in the wrap chain.
- It is installed **after the child's own `postInit()`**, which fired bottom-up inside the child's
  read. A class carrying both `@Owner` and `PostInit` would see a null owner in its own hook. The
  annotation would have to say so, loudly, because the combination reads as though it should work.

### 13.2 Why it declines anyway - the parameter is cheaper than the reference

Having settled the shape, the payoff is small enough to price exactly. `02-parent-access.md` §4.1
enumerates every read, and the total is three scalars, one NBT node and two settings objects.

| Site | What `@Owner` removes | What it does not remove |
| --- | --- | --- |
| `AccessoryBag` | 1 public method signature, 1 threaded `SkyBlockMember` parameter on a private helper, 1 cyclic package import (`AccessoryBag.java`:5) | the 110-line body - family de-duplication at `:74-126` is genuinely imperative |
| `Skills` / `SkillLevel` | 2 constructor parameters, 1 private-method parameter, 2 imports | nothing else; neither class retains the reference |
| `ProfileStats` | nothing - it is never deserialized | everything |

Against that: a new annotation, a new factory, a walk that must recurse into `Map` and `Collection`
values while treating the container as transparent, a serialization exclusion the factory has to
enforce rather than trusting an author to write `transient`, an answer for `equals`/`hashCode`/
`toString` on a now-cyclic graph, a defined behaviour when the owner is absent, and a JitPack cycle.

The comparison that settles it: **every one of those removals is also available by narrowing the
existing handover, at zero library cost.** `02-parent-access.md` §4.1 says the values are small and
explicitly permits it - "a design may legitimately choose to copy values down instead of handing a
reference up". Copying down is strictly better here than handing a reference up, because the
dependency stops being invisible: it appears in a method signature the compiler checks.

### 13.3 AccessoryBag under the chosen design

`AccessoryBag` is the one genuine customer, and it lands as **three values in, everything else lazy**.
`SkyBlockMember.postInit()` is the only frame in the module that holds both objects, and by the time
it runs every member field is bound - `postInit()` is bottom-up, so the member's hook is the last to
fire in its subtree.

```java
// SkyBlockMember.postInit() - the whole reach-back, in one call whose signature states it
this.accessoryBag.initialize(
    this.getInventory().getBags().getAccessories(),
    this.getRift().getAccess().hasConsumedPrism(),
    this.getCrimsonIsle().getAbiphone().getContacts().size()
);
```

```java
// AccessoryBag - replaces initialize(SkyBlockMember) at :55-164
/**
 * Supplies the three member-scoped values the accessory bag cannot reach from its own JSON node.
 *
 * @param contents the talisman bag item data, stored under the member's inventory
 * @param consumedPrism whether the rift prism has been consumed
 * @param abiphoneContacts the abiphone contact count, halved by an equipped abicase
 */
public void initialize(@NotNull NbtContent contents, boolean consumedPrism, int abiphoneContacts) {
    this.contents = contents;
    this.consumedPrism = consumedPrism;
    this.abiphoneContacts = abiphoneContacts;
}
```

Everything the old body computed becomes a memoised accessor over those three stores plus the bag's
own bound fields:

```java
@Getter(AccessLevel.NONE)
private transient ConcurrentList<AccessoryData> detectedAccessories;

/**
 * Accessories parsed out of the talisman bag and resolved against the accessory repository
 */
public @NotNull ConcurrentList<AccessoryData> getDetectedAccessories() {
    if (this.detectedAccessories == null)
        this.detectedAccessories = this.parseDetectedAccessories();

    return this.detectedAccessories;
}
```

What this buys beyond the deletion, and it is the reason to prefer it over `@Owner` rather than merely
a cheaper way to reach the same place:

- **`f02-accessorybag-dead-initialize` becomes unwritable.** The read-before-assign at `:57` versus
  `:138` exists only because 110 statements share one method and one implicit order. Under memoised
  accessors `getDetectedAccessories()` reads `this.contents` after `initialize` has necessarily run,
  and `getTuningPoints()` reads `getMagicalPower()` rather than a field that a later line was
  supposed to have filled. The dead store at `:129-136` has nowhere to hide, because there is no
  local to leave the value in.
- **The cyclic import goes.** `AccessoryBag` stops importing `SkyBlockMember`, so the package graph
  between `response.skyblock` and `response.skyblock.member` becomes acyclic. `@Owner` cannot deliver
  that - a typed owner field is that import.
- **Standalone decode keeps working unchanged.** `MemberDtoMappingTest.java`:111 decodes
  `AccessoryBag` in isolation from `accessory_bag_storage`; `initialize` is simply never called and
  the accessors return empty. Under `@Owner` the same test hands the accessors a null owner, and
  `02-parent-access.md` §4.3 flags that the design would owe an answer for it.
- **Nothing is computed until something is read.** Today every member of every profile pays the NBT
  parse and the family de-duplication on decode.

The honest cost: `initialize` is still a public method that must be called exactly once, which is the
"lifecycle hook masquerading as API" complaint in `f02-accessorybag-upstream`. That complaint is now
worth one three-argument signature instead of a `SkyBlockMember`, and the arguments make the
dependency legible where the old one-parameter form hid it.

### 13.4 Skills under the chosen design

`Skills` is the site that looks most like a reach-back and is not one. It has **no serialized
fields**, no no-arg constructor and never passes through Gson (`Skills.java`:15-24); it is built by
hand from a map plus the member. Neither it nor `SkillLevel` retains the member - the parameter
exists to survive one constructor call. No bind-time annotation can reach a type that is never bound,
so `@Owner` has nothing to attach to.

The whole fix is to stop building it eagerly:

```java
// SkyBlockMember - line 143 of postInit() deleted
@Getter(AccessLevel.NONE)
private transient Skills skills;

/**
 * Skill levels derived from the member's skill experience
 */
public @NotNull Skills getSkills() {
    if (this.skills == null)
        this.skills = new Skills(this.getPlayerData().getSkillExperience(), this);

    return this.skills;
}
```

That one change fixes `f02-skills-member-reachback` and `f05-derivation-ordering`'s first defect
outright. `SkillLevel.calcLevelSubtractor` reads `member.getCollectionUnlocked()`
(`SkillLevel.java`:32-33), which today is the empty initialiser because `postInit()` assigns it two
statements *after* it builds `Skills` - so the FORAGING subtractor is unconditionally `2`. Once both
are lazy, `getSkills()` calls `getCollectionUnlocked()`, which computes from `collection` and
`playerData` - two bound fields - and returns the right answer. **The call stack performs the
topological sort that `@Bind` was reserved to declare**, and it performs it at zero cost.

The member parameter stays, and that is deliberate. Passing an object into the constructor of a
non-bound helper is not residue; it is the same shape as `SkyBlockIsland.getProfileStats(member)`,
which `f02-profilestats-island-scalar` already ruled correct. `Skills` and `ProfileStats` are the
same pattern and get the same verdict.

### 13.5 ProfileStats under the chosen design

**Unchanged, and that is the finding.** `ProfileStats` lives outside `response/`, declares no
`@SerializedName` field, and is reached only through `SkyBlockIsland.getProfileStats(member,
calculateBonus)` (`SkyBlockIsland.java`:76-82). Its entire grandparent dependency is one `double` -
`banking.balance` at `ProfileStats.java`:69 - and the `calculateBonus` flag exists precisely so
callers can skip the expensive branch at `:143-210`.

Turning it into an `@Owner`-fed transient of `SkyBlockMember` would run that branch eagerly for every
member of every profile on every decode, to delete one constructor parameter. `@Owner` does not
merely fail to help here; adopting it would be a performance regression bought with an annotation.

The only defensible tightening is narrowing the parameter from `SkyBlockIsland` to the value it
consumes, and even that is arguable - the island keeps the door open for the community-upgrade and
game-mode reads `ProfileStats` will plausibly want. **Recommend no action.**

### 13.6 Hazards the decline avoids, and what would reopen it

Recorded so a future proposal does not rediscover them as surprises. Every one of these is a cost
`@Owner` carries and the three-value handover does not.

- **Cycles in serialization.** An owner field makes the graph cyclic and `Gson.toJson` recurses to
  `StackOverflowError`. Default `Excluder` behaviour skips `transient`, and `GsonSettings.defaults()`
  registers no exclusion strategy, so `transient` would be sufficient - but sufficient by accident is
  not the bar this pack sets for round-trip fidelity, so the factory would have to refuse to write
  the field itself.
- **Identity and rendering.** No response DTO uses `@EqualsAndHashCode` today and only `Election`
  hand-writes `equals`/`hashCode`/`toString`, so nothing breaks - by luck. An owner field inside a
  generated `equals` is infinite mutual recursion.
- **The container rule.** "Enclosing object" is ambiguous the moment a child sits in a collection.
  `SkyBlockIsland.members` is `ConcurrentLinkedMap<UUID, SkyBlockMember>` (`SkyBlockIsland.java`:38),
  so the naive walker hands a member the map. The rule has to be *nearest enclosing declared object,
  skipping container levels*, and it has to be stated because the obvious implementation gets it
  wrong.
- **Lifetime.** One retained child pins its parent, and for `SkyBlockProfiles` that means one member
  retains its island and every sibling member. A weak reference is the wrong fix, since a silently
  nulled owner is the exact failure mode the design is trying to avoid.
- **`WeakIdentityMap` as a field-free alternative.** The library already ships one and uses it three
  times as a per-instance side channel - `CaptureTypeAdapterFactory.OVERFLOW` (`:82`),
  `LenientTypeAdapterFactory.OVERFLOW` (`:62`), `CollapseTypeAdapterFactory.KEY_ORDER` (`:68`). Owners
  stored there would carry none of the cycle, equality or serialization hazards above. It is rejected
  explicitly rather than by omission: it trades a typed field for a static lookup helper at every use
  site, and it still serves one customer. If `@Owner` is ever revisited, **start here**, not with a
  field.

**What would reopen the entry.** A third and fourth bound type needing a genuine parent *object*
rather than two or three values - specifically, a child that calls more than a handful of distinct
getters on its parent, where enumerating them as parameters stops being legible. Nothing in 133 files
does that today; the widest consumer, `AccessoryBag`, needs three values. A second trigger would be a
requirement to serialize a decoded graph back out, which would make the `transient`-by-luck property
above load-bearing.

What would **not** reopen it: a wish to delete the `initialize` call from `SkyBlockMember.postInit()`.
That call is one statement, it is compile-checked, and replacing it with a reflective top-down walk
trades a visible line for an invisible guarantee.

## 14. d10-ancestor-path - ancestor-relative `@SerializedPath`

- **Registry entry:** none - the "extend an existing annotation" route, raised by
  `02-parent-access.md` §4.4 and handed to part two by §12.3 item 4
- **Verdict:** decline
- **Category:** `parent-access`
- **Answers findings:** would answer `f02-kuudra-sibling-push` and three of the reads in
  `f02-accessorybag-upstream`
- **Cheaper alternative:** for `Kuudra`, stop modelling the party finder as part of `Kuudra` - a
  rename, zero library change; for `AccessoryBag`, the three-value handover in `d10-owner-parent` §13.3
- **Library change:** `SerializedPathTypeAdaptorFactory` edit plus a new `ThreadLocal` ancestor stack
  and a static reachability scan at `create` time
- **Adoption sites today:** 2 classes, 5 reads
- **Effort:** `large` - it is priced as a factory edit per §12.3, but the mechanism it needs is a new
  cross-frame guarantee, not a new element

### 14.1 What the existing factory already gives away for free

Two properties of `SerializedPathTypeAdaptorFactory` are load-bearing for everything below and for
`d10-derive`, and neither is documented anywhere. Both are read straight off the source.

**It assigns after the object is otherwise complete.** `read` buffers the whole enclosing node into a
`JsonElement` (`:101`), binds the object through the delegate (`:103`), and only then walks the
`@SerializedPath` fields and assigns each one (`:105-141`). So at the moment of assignment `value` is
fully bound - every sibling field, and every child object underneath it, already populated. That is a
post-bind write executed inside the bind phase, and it is the only one in the library.

**It is the one factory that can write a `transient` field.** `FieldInfo.of` (`:163-175`) applies no
`Modifier.isTransient` filter, and `Reflection.getFields()` (`Reflection.java`:305-313) returns
declared fields unfiltered. Every other factory skips transients explicitly -
`CaptureTypeAdapterFactory.java`:115, `CollapseTypeAdapterFactory.java`:389,
`LenientTypeAdapterFactory.java`:434 and `:485`, `SplitTypeAdapterFactory.java`:223 - which is the
blanket constraint `05-cross-field-derivation.md` §4 states as "no existing factory can see a
`transient` field". `@SerializedPath` is the exception, and the exception is untested; anything built
on it needs a test that pins the behaviour before it is relied on.

Together those two make the *parent-side* half of this idea nearly free. They do nothing for the
child-side climb, which is what the row was actually asking for.

### 14.2 The climb cannot be spelled correctly

The wanted syntax is something like `@SerializedPath("^.kuudra_party_finder.search_settings")` on a
field of `Kuudra`, meaning "one level up, then down". Three blockers, in ascending order of how fatal
they are.

**1. The ancestor's tree is in a different frame.** `Kuudra`'s adapter is handed a reader positioned
at its own node; `CrimsonIsle`'s buffered `JsonObject` exists only as a local inside `CrimsonIsle`'s
`read`. Making it visible means a `ThreadLocal` stack pushed around the `fromJsonTree` call at
`:103`. The library has no `ThreadLocal` anywhere today, so this is new machinery, and it has to be
exception-safe on both edges or one malformed document poisons every later decode on that thread.

**2. The ancestor is only buffered by accident.** `create` returns the bare delegate for a class with
no `@SerializedPath` field (`:39-41`). `SkyBlockMember` buffers today only because it happens to
carry `attributes.stacks` and `objectives.tutorial` (`SkyBlockMember.java`:123,137) - and
`d10-inline` proposes adding eleven more, which makes the accident look permanent. It is not a
contract. Delete those fields and `AccessoryBag`'s climb stops working with no diagnostic. Making it
reliable means wrapping every class that *contains* a climbing descendant, which is a static
reachability scan over the field graph through generics, containers and cycles, performed at
`create` time for every type Gson touches.

**3. `^` counts frames, and the author is counting objects.** This is the one that cannot be
engineered around. `SkyBlockIsland.members` is `ConcurrentLinkedMap<UUID, SkyBlockMember>`
(`SkyBlockIsland.java`:38), so one frame above a member is the map adapter, not the island. Every
`@Capture` and `@Lenient` field adds frames of its own, because both call `fromJsonTree` from their
own stack depth. The annotation would have to mean "up one declared *object*, skipping containers" -
the same rule `d10-owner-parent` §13.6 needs - except now it is encoded in a string with no
compile-time check, no IDE navigation and no error until a document arrives that exercises it. A
mis-counted `^` selects the wrong ancestor and binds a plausible wrong value.

Blockers 1 and 2 are engineering. Blocker 3 is a design defect in the notation, and it is the reason
this declines rather than merely defers.

### 14.3 The buildable half - a parent-side descent target

Recorded because it is genuinely cheap and it will be proposed if it is not written down. §14.1 says
`SerializedPathTypeAdaptorFactory` assigns into a fully bound object, so the annotation could name a
**Java field path on the receiving side** rather than climbing on the reading side:

```java
// hypothetical - the annotation stays on the parent, where the JSON key lives
@SerializedPath(value = "kuudra_party_finder.search_settings", target = "kuudra.searchSettings")
private Kuudra.SearchSettings kuudraSearchSettings = new Kuudra.SearchSettings();
```

`FieldInfo` gains a second accessor chain; the assignment at `:135` walks it instead of setting the
host field. Roughly twenty library lines, no new frame, no `ThreadLocal`, no container ambiguity, and
`transient` targets already work per §14.1. That is honestly `small`.

It still fails on its own merits, twice.

- **The host field cannot go.** The annotation needs a field to live on and a type to bind through,
  so `CrimsonIsle` keeps both staging declarations. All that is retired is the two-line `postInit()`
  body - and the retirement is exactly what §14.4 gets for free.
- **Write is asymmetric.** `write` (`:64-91`) looks the flat key up in the delegate's tree. With a
  target the value now lives on a `transient` field of a child, so the delegate never emits it and
  the `kuudra_party_finder` key silently disappears from serialized output. Today those staging
  fields are non-`transient` and do round-trip. `@Lenient` and `@Collapse` both hold the round-trip
  line; a new element that quietly breaks it does not clear the bar.

### 14.4 `f02-kuudra-sibling-push` needs no annotation at all

The site the whole row was invented for turns out to be a **naming** problem wearing a `parent-access`
costume, and the fix costs nothing.

Hypixel puts party-finder state under `kuudra_party_finder`, a sibling of `kuudra_completed_tiers`.
The Java side decided `Kuudra` owns both, and then had to move two objects one level down to make
that true: two staging fields with snake-case names, two `@Getter(AccessLevel.NONE)` suppressions,
two package-private `transient` fields on `Kuudra` that only work because the classes share a package
(`Kuudra.java`:22-23), and the entire reason `CrimsonIsle` implements `PostInit`
(`CrimsonIsle.java`:38-43, :52-56).

Do not move them. The JSON says they are siblings, so let the Java say it:

```java
// CrimsonIsle - postInit(), the PostInit interface and both suppressions all go
@SerializedName("kuudra_completed_tiers")
private @NotNull Kuudra kuudra = new Kuudra();
@SerializedPath("kuudra_party_finder.search_settings")
private @NotNull Kuudra.SearchSettings partyFinderSearch = new Kuudra.SearchSettings();
@SerializedPath("kuudra_party_finder.group_builder")
private @NotNull Kuudra.GroupBuilder partyFinderGroupBuilder = new Kuudra.GroupBuilder();
```

`d10-delegate` §5.2 already established that nothing outside `CrimsonIsle` reads either object, so no
forwarder is owed on the way out. What lands: **one of the six `PostInit` implementors retired**, two
`transient` fields deleted from `Kuudra`, two suppressions deleted, one encapsulation leak closed,
two snake-case Java names corrected, and round-trip fidelity preserved because both fields stay bound
and non-`transient`. Effort `trivial`, consumer-only, one file plus two deletions in a second.

This is the cheapest `PostInit` scalp in the pack and it was hiding behind a proposal for a
cross-frame path mechanism.

### 14.5 Verdict

**Decline.** The child-side climb is not spellable without a frame-versus-object rule that a string
cannot carry (§14.2). The parent-side target is buildable and cheap but retires only the two lines
that §14.4 retires for free, and it breaks round-trip on the way (§14.3). Both of the row's customers
are better served without it: `Kuudra` by a rename, `AccessoryBag` by the three-value handover in
§13.3.

`02-parent-access.md` §4.4 expected this to be the cheaper alternative to `@Owner`, and on lifecycle
cost it genuinely is - no new phase, no cycle, no equality or serialization hazard. It loses on a
different axis, notation: the one thing it must express is "up one object", and that is precisely the
thing a path string cannot check.

**What would reopen it.** A JSON layout where a child needs several keys from a known ancestor and
the child is not reachable from any single frame that holds both - that is, a case where §13.3's
copy-down would need more than a handful of parameters. Nothing in 133 files is close. The frame
ambiguity is a property of this library's container handling, not of the JSON, so it will not
resolve on its own.

## 15. d10-derive - `@Derive`

- **Registry entry:** `@Derive` - "marks a transient field as computed after bind, naming the
  computation so the hook is declarative rather than an imperative `postInit()` body"
- **Verdict:** reject
- **Category:** `cross-field-derivation`
- **Answers findings:** `f05-collection-tier-join`, `f05-derivation-ordering`,
  `f05-jacobscontest-contest-key`, `f05-dungeons-master-pairing`, `f05-lazy-getter-convention`,
  `f02-skills-member-reachback`
- **Cheaper alternative:** the lazy memoised getter - already the convention in this package, in
  roughly fifteen places
- **Library change:** would be an additive annotation plus a new post-bind phase that deliberately
  opts into `transient` fields
- **Adoption sites today:** ~5, every one of which the alternative also serves
- **Effort:** `large`

### 15.1 It is `PostInit` with a string for a method name

`@Derive("computeCollectionUnlocked")` on a `transient` field, with a factory ordering the calls, can
express everything - repository lookups, NBT walks, arbitrary Java - because the body is ordinary
Java. That universality is the tell. `PostInit` already runs after bind, already lets a class compute
anything, and already exists. The only thing `@Derive` adds is **ordering between derived fields**,
and it pays for it by replacing a compile-checked method call with a reflected string that no
refactor, no rename and no "find usages" can follow.

Then the ordering itself evaporates. Ordering between derived values matters only while those values
are computed eagerly. A getter reads its inputs at the moment it is called, which is always after the
whole document is bound, so the dependency graph is resolved by the call stack - by the JVM, for
free, with a real stack trace if it ever goes wrong. `@Derive`'s sole advantage exists only inside
the problem it perpetuates.

The mechanical cost is worth stating too, because it is larger than the annotation looks. Every
derived field in this package is `transient` by design - it must not serialize - and every factory
except `SerializedPathTypeAdaptorFactory` skips `transient` fields outright (§14.1). So `@Derive`
cannot be a field-scanning factory in the `@Split` mould; it needs a new phase that deliberately opts
into transients and runs after the root completes. That is a new ordering guarantee the whole
pipeline must honour, which the effort scale prices at `large` before a line is written.

**The package already contains the answer, in fifteen places.** `Slayers.java`:24-52 folds over
`bosses` with no transient, no hook and no hazard. `Skills.java`:37-65 and `Dungeons.java`:96-130
compute the same four aggregates lazily. `Bestiary.java`:44-53 folds over `families`.
`HypixelPlayer.java`:82-91 is the memoised variant - transient backing field, null check, compute
once. Ten of the eleven repository derivations are one-line lazy getters. The correlation
`f05-lazy-getter-convention` reports is the whole argument: **every eager derivation in this package
is either wrong or fragile, and every lazy one is fine.**

### 15.2 `SkyBlockMember.collectionUnlocked`

The anchor derivation, and the one an annotation was most likely to claim. It is a join: for every
item in `collection`, the maximum tier in `player_data.unlocked_coll_tiers` whose string matches
`<itemId>_<digits>`, defaulting to `0`. The value side of `collection` is discarded entirely - the map
is used as the set of ids to iterate.

`05-cross-field-derivation.md` `f05-collection-tier-join` carries the replacement verbatim and it is
not repeated here. Three things about it that decide this entry:

- **It deletes eleven lines of `postInit()` and adds no annotation.** `SkyBlockMember.postInit()`
  drops from three statements to one, and §13.4 removes a second.
- **It is faster by construction, not by tuning.** The current form calls `String.matches` 100 x 775
  times per member per decode, compiling a fresh `Pattern` each time; splitting each tier string at
  its **last** underscore builds the index in one pass. The equivalence is proved rather than
  assumed - `^LOG_[0-9]+$` claims exactly `LOG_1`..`LOG_9` and never `LOG_2_5`, and a last-underscore
  split reproduces that exactly, which a "strip the longest known id prefix" index would not.
- **It fixes `f05-derivation-ordering` as a side effect**, because `SkillLevel` can no longer observe
  the map before it is filled - there is no "before" any more.

One thing the rewrite must keep, and must keep *visibly*: the regex `[\\d]+` never matched a leading
minus, so the 83 of 775 tier strings ending `_-1` are skipped. `f05-negative-tier-exclusion` proves
that is the right behaviour - every id carrying a `_-1` also carries `_1`..`_9`, and downstream
consumers compare against positive thresholds, so `0` serves them better than `-1`. The rewrite states
it as `if (tier < 0) continue;` instead of hiding it in a character class. An annotation would have
had to grow an element for it, and would have hidden it again.

### 15.3 `JacobsContest` contest parsing

The most annotation-shaped derivation in the module, and it needs no new annotation either - it needs
two that already ship.

Today `contestMap` is a `@Getter(AccessLevel.NONE)` map keyed `<year>:<month>_<day>:<collectionId>`,
and `postInit()` (`JacobsContest.java`:46-63) walks it, parses each key, writes `collectionName` and
`skyBlockDate` back into the already-bound `Contest`, and republishes the map as a `transient` list.
Two defects ride along: `Contest.skyBlockDate` and `Contest.collectionName`
(`JacobsContest.java`:108-109) are non-`transient`, so they serialize as keys Hypixel never sent.

`@Collapse` + `@Key` already inject an entry key into the value object during the parent field's
bind, and `@Key` already works on a `transient` field - `SlayerBoss.id` is exactly that, and
`Slayers` is the shipping user. So the key arrives on the child, and the child parses it lazily:

```java
// JacobsContest - contestMap, the transient list, postInit() and the interface all go
@Collapse
@SerializedName("contests")
private @NotNull ConcurrentList<Contest> contests = Concurrent.newList();
```

```java
// JacobsContest.Contest
@Key
private transient @NotNull String id = "";

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
```

This is the passage that kills the "extend `@Split` or `@Key` with key decomposition" proposal that
`f05-jacobscontest-contest-key` floats. The decomposition is genuinely subtle - 810 fixture keys, and
every `INK_SACK:3` contest splits into **four** parts rather than three, so a `parts = 3` or
`Pair`-shaped splitter would silently truncate the collection id to `INK_SACK`. Expressing "split on
`:`, and the last part keeps its remaining delimiters" as an annotation element is a genuine design
problem. Expressing it in Java is `StringUtil.join(parts, ":", 2, parts.length)`, which is what the
code already says. **Move the parse to where the key is, and the annotation problem stops existing.**

Payoff: one `PostInit` implementor retired, the holder-shaped `contestMap` and its suppression
deleted, the `transient` republished list deleted, and both phantom serialized keys gone because
`skyBlockDate` and `collectionName` stop being fields at all. Zero library change - `@Collapse`,
`@Key` and `PostInit`'s absence are all already in the box.

### 15.4 `Dungeons` master-mode pairing

The third derivation, and the one where the derivation mechanism is not the problem at all. The
pairing is a self-join on one map: each normal dungeon is matched to its `master_`-prefixed
counterpart. `Dungeons.postInit()` (`:56-76`) does it with two case-sensitive string literals against
data that is lowercase, so both halves miss:

- `filterKey(key -> !key.startsWith("MASTER_"))` at `:58` does not exclude `master_catacombs`, so it
  survives, maps through the case-insensitive `DungeonData.Type.of` to `UNKNOWN`, and appears as a
  spurious dungeon in `getWeight()`.
- `getOrDefault(String.format("MASTER_%s", type.name()), new FloorData())` at `:63-66` looks for
  `MASTER_CATACOMBS` when the key is `master_catacombs`, so **`DungeonData.masterMode` is empty for
  every profile ever decoded**.

Fixture ground truth is two keys, `catacombs` and `master_catacombs`, both lowercase. This is a live
`correctness` defect, not a shape problem, and no annotation in the registry addresses it - `@Index`
would have carried the same affix as a string and made the same mistake.

Lazily, with the case handled once:

```java
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
                floorData.getExperience(),
                floorData,
                floors.getOrDefault(String.format("master_%s", key), new FloorData())
            )))
            .collect(Concurrent.toUnmodifiableMap());
    }

    return this.dungeons;
}
```

Normalising the key space once, rather than comparing case-insensitively in two places, is what stops
the third occurrence of the bug: the filter and the lookup now share one spelling by construction.

Note what is *not* fixed by adding `MASTER_CATACOMBS` to the enum - nothing. The filter at `:58` would
still let the lowercase key through, so the spurious entry survives. `f05-dungeons-master-pairing`
makes this point and it is worth repeating, because "the enum is missing a constant" is the first
diagnosis anyone reaches for.

Two boundaries this entry does not cross. `01-postinit.md` proposes a `@Capture` route to the same
retirement (`f01-dungeons-capture-grouping`); both routes are zero library cost and
`11-postinit-elimination.md` should pick one rather than this document choosing for it. Whichever
wins, **it must handle the lowercase wire keys** - a `@Capture(filter = "^master_")` regex is
case-sensitive too, and the only case-insensitive step anywhere on this path today is
`DungeonData.Type.of`. Separately, `classes` (`Dungeons.java`:70-75) is deleted rather than made lazy:
`d10-flatten` §6.1 shows the JSON is already the shape of `DungeonClass`, so retyping `classMap` to
`ConcurrentMap<DungeonClass.Type, DungeonClass>` binds it directly and removes six more lines of the
same hook.

### 15.5 What stays imperative, and what the convention costs

Stated so `11-postinit-elimination.md` does not inherit an over-claim.

- **Repository-backed derivation stays imperative and stays lazy.** Eleven sites, and §16.3 rules
  them permanently out of annotation scope.
- **`AccessoryBag`'s body stays.** Family de-duplication (`:74-126`) is a real algorithm. Laziness
  moves it behind an accessor; it does not shrink it.
- **The four `Matcher.group` defects stay consumer work.** `Bestiary.java`:61-63 and
  `DungeonRun.java`:46,50,54 call `group(int)` on a matcher that was never matched, which throws
  `IllegalStateException` every time. In `Bestiary` the throw is swallowed and the whole feature is
  dark; in `DungeonRun` it reaches the caller. No annotation is involved either way, and these are the
  highest-payoff `trivial` fixes in the pack.

The convention has two honest costs and both should be written at each site rather than rediscovered.

**Memoisation on a mutable DTO races.** The null-check-then-assign pattern is not synchronised. Both
racers compute the same value from immutable inputs so the race is benign, and
`HypixelPlayer.java`:83-90 already ships it - but these DTOs use `Concurrent*` collections throughout
and are plainly expected to be shared, so the property should be asserted, not assumed.

**Exceptions move from decode to call.** Today a broken derivation is swallowed by
`PostInitTypeAdapterFactory`'s empty catch and presents as a quietly empty collection. Lazily, it
throws at the caller. That is the correct trade - it is how `f05-matcher-group-without-match` would
have been caught years ago - but it is a behaviour change for every consumer and it belongs in the
release note, not in a footnote.

Memoise only where measurement justifies it. `collectionUnlocked` earns it (775 strings x 100 ids).
`Slayers`' four aggregates do not, and do not have it.

### 15.6 Verdict

**Reject.** `@Derive` is a mechanism looking for a problem that laziness dissolves. It costs a new
lifecycle phase, a new transient-aware scan, a reflected method name and a JitPack cycle, to make
eager derivation safe - when not deriving eagerly is free, is already the house convention in fifteen
places, and fixes three live defects on the way past.

Cite this entry if `@Derive` is proposed again. The evidence that motivates it -
`f05-derivation-ordering`'s three wrong values - is real and strong, and it argues for laziness, not
for a hook.

## 16. d10-index-join - `@Index` / `@Join`

- **Registry entry:** `@Index` / `@Join` - "resolves a field's value by looking another field's value
  up in a keyed source - a sibling map, a sibling collection, or a static repository"
- **Verdict:** reject, both names
- **Category:** `cross-field-derivation`
- **Answers findings:** `f05-collection-tier-join`, `f05-repository-derivations`,
  `f05-idtier-key-family`, `f05-dungeons-master-pairing`
- **Cheaper alternative:** the lazy getter for the two sites that are lookups, and nothing at all for
  the site that is a parse
- **Library change:** would be an additive annotation plus the same new post-bind phase `@Derive`
  needs, plus - for the repository half - a resolver registry threaded through `GsonSettings`
- **Adoption sites today:** **1** for `@Index`, **1** for `@Join`, and they are not the same site
- **Effort:** `large` for the sibling half, `large` again for the repository half

### 16.1 The seeded pair resolves into two operations, and both lose

`00-conventions.md` §6 seeded these as one row because the survey evidence might not distinguish them,
and asked part two to decide. **They are two operations.** `@Index` is a keyed lookup - take this
field's value, use it as a key into that source, keep what comes back. `@Join` is a merge - take two
collections, pair their elements on a derived key. The three derivations part two was asked to cover
split cleanly across them, and a fourth pattern belongs to neither.

| Derivation | Operation | Source | Key rule | Reduce | Expressible? |
| --- | --- | --- | --- | --- | --- |
| `SkyBlockMember.collectionUnlocked` | `@Join`, restricted | `playerData.unlockedCollectionTiers` x `collection` | split at last `_` | `max`, default `0` | yes - with five elements, at one site |
| `Dungeons` master pairing | `@Join`, self | `dungeonMap` x itself | affix `master_`, case-insensitive | none | shape yes, defect no |
| `JacobsContest` contests | neither | the map key itself | split `:`, last part keeps delimiters | none | no - it is a parse |
| 11 repository sites | `@Index` | static `SkyBlockData` | id equality | none | see §16.3 |

Every row is a reason to reject.

**Row one is the only clean `@Join`, and it is one site.** The annotation it needs is
`@Join(source = "playerData.unlockedCollectionTiers", restrictTo = "collection", key = ..., reduce =
MAX, orElse = "0")`: five elements, one adoption site, and `restrictTo` is a genuinely strange knob -
"iterate the keys of field X, look them up in field Y" is a set operation the annotation would have
to name and document. It also bakes a Hypixel key convention (`<id>_<n>`, split at the last
underscore) into a general-purpose JSON library that has no other consumer wanting it. Eleven deleted
lines in one consumer class does not buy that.

**Row two is a self-join whose problem is not the join.** The shape is expressible; the defect is that
`startsWith("MASTER_")` and `String.format("MASTER_%s", ...)` are compared against lowercase keys
(§15.4). An affix element on an annotation would have carried the same literal and made the same
mistake, and it would have hidden it one level further from the data.

**Row three is not a lookup at all.** It parses a composite key into two typed values (§15.3). Nothing
is being looked up in anything.

**Row four is §16.3.**

The transient blocker applies here exactly as it does to `@Derive`: every derived field in this
package is `transient`, every factory but `SerializedPathTypeAdaptorFactory` skips transients (§14.1),
so `@Index`/`@Join` cannot be a field-scanning factory in the `@Split` mould. It needs the same new
post-bind phase, at the same `large` price, for one adoption site each.

### 16.2 What a lookup annotation would have to get right, and does not

Recorded because "it is only a map lookup" understates it, and because these are the properties that
make the Java version short.

- **Direction is semantic, not incidental.** `collectionUnlocked` iterates `collection` and looks up
  tiers, so an item with unlocked tiers but nothing collected is **absent** from the output. Reverse
  the loop and the map grows. The fixture happens to have zero such ids, so an annotation that got
  the direction wrong would pass every test and be wrong for some account.
- **The value side of the join input is discarded.** `collection` is `itemId -> amount collected`, and
  the amount never reaches the output; the map is used purely as a set of ids. An annotation whose
  mental model is "merge two maps" produces something else.
- **Absent means zero, and zero means something.** Eleven fixture items sit in `collection` with no
  tier string at all and land on the `orElse(0)`. So does an id whose only tier string is `_-1`. The
  two arrive at the same answer by different routes and both are correct - a fact §15.2 states in one
  line of Java and an annotation would need an element plus a paragraph to express.

### 16.3 Repository-backed derivation is permanently out of scope

The explicit answer to the question the assignment asked, stated once so no document re-evaluates it.

**Eleven of the derivation sites resolve a bound id against the static `SkyBlockData` repository**, and
they are the ones an adoption-site count would otherwise inflate:
`SkillLevel.getSkill()` (`SkillLevel.java`:42-44), `SlayerBoss.getType()` (`SlayerBoss.java`:35),
`OwnedPet.getPet()` (`OwnedPet.java`:49), `ForgeItem.getItem()` (`ForgeItem.java`:25),
`TrophyFish.getZone()` (`TrophyFish.java`:48), `ActiveTrap.getRegion()` (`ActiveTrap.java`:35),
`Bestiary.Family.getType()` (`Bestiary.java`:93-95), `Bestiary.Mob.getFamily()`
(`Bestiary.java`:135-137), `Bestiary.postInit()` (`Bestiary.java`:74-82), and
`AccessoryBag.initialize` at `:62`, `:149`, `:167`, `:175`.

They cannot be data annotations, for four independent reasons. Any one of them is sufficient.

**1. The dependency runs the wrong way and must keep doing so.** `gson-extras` has no dependency on
`dev.sbs.skyblockdata` and must not acquire one - it is a general JSON library pinned by unrelated
sibling modules, and adding a game-data dependency to it would propagate that dependency to every one
of them through the shared pin. This is not a preference; it is what makes the library reusable.

**2. The escape hatch is buildable and still worthless.** The dependency could be inverted with a
pluggable resolver registry - `GsonSettings` accepting a `Map<Class<?>, Function<Object, Object>>`,
threaded through every factory, so `@Index(via = Accessory.class, by = "id")` resolves through a
lookup supplied by the consumer. That is a real design and it would work. It is `large`, it needs the
post-bind transient phase as well, and **it deletes nothing**: ten of the eleven sites are already
one-line lazy getters, so the annotation would replace a compile-checked call with a reflected
string of equal length. The test for a library change is not "can it be built" but "what does it
remove", and the answer here is a negative number.

**3. The output shape is defined by the repository, not by the document.** This is the decisive one.
`Bestiary.postInit()` iterates the **entire** `BestiaryFamily` repository and builds one `Family` per
model, joining in whichever mobs the member's JSON happened to mention. Nothing in the incoming
document determines how many entries the result has. `Bestiary.Mob.getFamily()` does the reverse -
it scans every family's mob list looking for `<id>_<level>`. No annotation over JSON can express
"produce one entry per row of a table this library has never heard of", because the JSON is an input
to the computation, not its shape.

**4. Eager repository access would import a new failure mode.** A lookup at bind time makes decode
depend on repository load order and on the repository being populated at all. Ten of the eleven sites
are lazy today and are therefore immune. Making them declarative would make them eager, which
reintroduces exactly the class of defect `f05-derivation-ordering` documents - and
`PostInitTypeAdapterFactory`'s empty catch would hide it.

The one thing worth normalising is *when* these run, not *how*. The ten lazy sites are correct as
written. `Bestiary`'s eager one is the single site that diverges from the house pattern and the
single site that is silently broken; it should join the other ten.

`AccessoryBag` is the extreme case and worth naming separately: it needs a repository, an NBT
compound tag walk, and three values from its enclosing member. **None of its three inputs is a
sibling JSON field.** If an annotation set had a lower bound on what it can express, this is it.

### 16.4 Verdict

**Reject both names.** `@Index` has one adoption site among sibling fields and eleven that §16.3 puts
permanently out of reach. `@Join` has one clean site and one whose defect it would have reproduced.
The registry row is closed with both names retired rather than one chosen.

**What would reopen it.** Three or more sibling-field joins with the *same* key rule and the *same*
reduction - the current three share neither. A repository resolver would additionally need a second
consuming module that wants one, since a registry threaded through `GsonSettings` for a single
consumer is a configuration surface with one user.

## 17. d10-tier - `@Tier`

- **Registry entry:** `@Tier` - "reads a level/tier from an id-plus-number key family, taking the max
  match for a given base id, rather than hand-rolling the regex-and-max scan"
- **Verdict:** reject - subsumed by `@Index`, which is itself rejected. Kept in the registry as a
  documented alias per `00-conventions.md` §6, not deleted
- **Category:** `duplication`
- **Answers findings:** `f05-idtier-key-family`, `f05-negative-tier-exclusion`
- **Cheaper alternative:** one package-private static helper in this module
- **Library change:** would be an additive annotation plus the same post-bind transient phase
- **Adoption sites today:** **1 of 3** - the three sites that share the parse do not share the
  reduction
- **Effort:** `small` for the helper, `large` for the annotation

### 17.1 The parse is shared, the reduction is not

`00-conventions.md` §6 asked whether `@Index` subsumes `@Tier` cleanly. It does - `@Tier` is `@Index`
with the key rule and the reduction hard-coded - so this row inherits §16's rejection. But it also
fails on its own evidence, and the reason is sharper than "the general one lost".

Three sites hand-write the `<id>_<number>` scan. Only the *parse* repeats:

| Site | Source | Parse | Reduce | Output |
| --- | --- | --- | --- | --- |
| `SkyBlockMember.java`:150 | `unlocked_coll_tiers` | `^<itemId>_[\d]+$`, strip prefix | `max`, default `0` | `int` per item |
| `PlayerData.java`:58 | `crafted_generators` | `^<itemId>_[\d]+$`, strip prefix | keep **all**, sorted | `ConcurrentList<Integer>` |
| `Bestiary.java`:59 | `kills` / `deaths` map keys | `^([a-z_]+)_([0-9]+)$` | keep **both halves** | `(id, level)` pair |

A `@Tier` that only takes the max serves the first row. The second wants every match in order; the
third wants the id back as well as the number, and it feeds a repository join. Covering all three
means elements for source, pattern, reduction mode, output shape and default - which is `@Index`
again, five knobs for three call sites in one consumer module.

What is actually shared is worth extracting, in this module rather than in the library: one helper
that turns a list of `<id>_<n>` strings into a `Map<String, List<Integer>>` in a single pass. All
three sites then reduce that map differently, in two lines each, with their differences visible
instead of encoded as annotation elements. `small`, consumer-only, zero JitPack cycles.

Two correctness notes the helper inherits and must keep:

- **The prefix strip must be `substring` after `lastIndexOf('_')`, not `String.replace`.**
  `String.replace(CharSequence, CharSequence)` replaces *every* occurrence, not the leading one. No
  fixture id repeats its own `<id>_` prefix inside a tier string, so it does not misfire today; the
  substring form is correct by construction and faster.
- **The pattern interpolation is unquoted.** `String.format("^%s_[\\d]+$", itemId)` compiles a
  caller-supplied id as a regex. No fixture id or tier string contains a metacharacter, so it does not
  misfire today either. The helper does not compile a pattern at all, which retires the question.

`f05-negative-tier-exclusion` is the third property and §15.2 already carries it: skipping `_-1`
entries is correct, and the helper states it as `if (tier < 0) continue;` rather than as a character
class that omits the minus sign.

### 17.2 Verdict

**Reject the annotation, keep the row as an alias.** The registry entry is not deleted, because the
pattern it names is real and recurs; it is annotated as subsumed by `@Index`, which §16 rejected, and
served by a consumer-side helper. Reopen only if a second module wants the same scan - one module's
key convention does not belong in a general JSON library.

## 18. d10-aggregate - `@Aggregate`

- **Registry entry:** `@Aggregate` - "materializes a sum/max/average/count over a sibling collection
  into a field, for aggregates that are currently computed in `postInit()`"
- **Verdict:** reject
- **Category:** `cross-field-derivation`
- **Answers findings:** `f05-lazy-getter-convention`
- **Cheaper alternative:** the plain fold that every aggregate in the package already is
- **Library change:** would be an additive annotation plus the post-bind transient phase
- **Adoption sites today:** **0**
- **Effort:** `large`, for nothing

### 18.1 There are no materialized aggregates to retire

The registry line contains a factual premise - "aggregates that are currently computed in
`postInit()`" - and the premise is false. Across 133 files there is **not one** sum, average, max or
count over a sibling collection stored into a field. Every aggregate in the package is a lazy fold:

- `Slayers.java`:24-52 - `getAverage()`, `getExperience()`, `getProgressPercentage()`, `getWeight()`,
  all folding over `bosses`, which `@Collapse` produced at bind. No `transient`, no hook.
- `Skills.java`:37-65 - the same four over `skillLevels`.
- `Dungeons.java`:96-130 - the same four over `classes` and `dungeons`.
- `Bestiary.java`:44-53 - `getUnlocked()` and `getMilestone()` over `families`.

`Slayers` is the model. It has no `PostInit`, no derived field and no ordering hazard, and it computes
four aggregates from a collection an annotation filled.

An annotation with zero adoption sites is not a close call, but the entry is written up rather than
struck out because rejecting it has an argument attached that outlives the row: **materialising an
aggregate would introduce the ordering hazard where none currently exists.** A `@Aggregate` field is
by definition eager, so it acquires a dependency on its source collection being filled first - which
is precisely the failure `f05-derivation-ordering` documents three times over. The annotation would
make the wrong pattern convenient to write, in a package whose every eager derivation is currently
either wrong or fragile.

### 18.2 Verdict

**Reject.** Zero sites, a false premise in the registry line, and a design whose only effect would be
to import a hazard the package does not have. Reopen only if a profiler shows a specific fold is hot
enough to need memoising - and the answer to that is `HypixelPlayer.java`:82-91's shape, a memoised
getter, not an annotation.

## 19. d10-bind - `@Bind`

- **Registry entry:** `@Bind` - "naming placeholder for a general ordered post-bind phase, if the pack
  concludes `@Derive` needs explicit dependency ordering rather than a flat pass"
- **Verdict:** reject
- **Category:** `cross-field-derivation`
- **Answers findings:** `f05-derivation-ordering`, `f02-skills-member-reachback`,
  `f02-postinit-bottom-up-order`
- **Cheaper alternative:** the call stack
- **Library change:** would be a new phase plus a reordering of the factory chain
- **Adoption sites today:** ~5 eager derivations, all of which stop needing ordering once they are
  lazy
- **Effort:** `xlarge` - it reorders the factory chain, which is the semantic-break row

### 19.1 The evidence is real and it argues the other way

`@Bind` deserves a fair hearing, because it is the only registry row with a *proved* motivating
defect rather than a stylistic one. Ordering between derived fields genuinely matters today:
`SkyBlockMember.postInit()` builds `skills` at line 143 and assigns `collectionUnlocked` at line 145,
and `SkillLevel` reads `collectionUnlocked` from inside line 143. The dependency is expressed by
nothing but the vertical order of two statements. `f02-skills-member-reachback` reaches the same
conclusion from the other direction and states it precisely: even a perfect `@Owner` that guarantees a
fully *bound* parent does not fix this, because `collectionUnlocked` is not bound, it is derived. What
that site needs is ordering between derivations, which is exactly this row.

And it still loses, for one reason: **ordering is a property of eagerness, not of derivation.** A
lazy getter reads its inputs at the moment it is called. `getSkills()` calls
`getCollectionUnlocked()`, which reads `collection` and `playerData` - two bound fields - and returns.
The dependency graph is walked by the call stack, in the correct order, by the JVM, with a real stack
trace if anything goes wrong. `@Bind` asks for a new lifecycle phase, a topological sort, cycle
detection and a diagnostic story for cycles, in order to make eager derivation safe, when not
deriving eagerly is free and is already the convention in fifteen places.

One thing `@Bind` would have that laziness does not, stated so the trade is honest: **a derivation
cycle becomes a `StackOverflowError` instead of a declared error at startup.** A topological sort
detects `a depends on b depends on a` and can name both fields; mutually recursive getters blow the
stack at the first call, at an arbitrary caller, with a trace hundreds of frames deep. No cycle exists
in this package today - the derivations bottom out on bound fields within one or two hops - and the
cost of the alternative is an `xlarge` library change. Note the risk in the convention rather than
buying insurance against it.

### 19.2 The combined ordering model

`@Bind` is rejected, so no new phase exists. What follows is the model that the accepted design
actually delivers - the exact sequence for one object, and the guarantee available at each point.
This is the reference every other entry in part two assumes, and it is written here because it is what
`@Bind` would have replaced.

`GsonSettings.java`:249-256 registers `CaseInsensitiveEnum`, `Optional`, `Split`, `SerializedPath`,
`Lenient`, `Capture`, `Collapse`, `PostInit`, and `GsonBuilder.create()` **reverses** the list, so the
wrap chain outermost first is:

```
PostInit -> Collapse -> Capture -> Lenient -> SerializedPath -> [Flatten] -> Split -> Optional -> CaseInsensitiveEnum -> ... -> Reflective
```

`[Flatten]` is `d10-flatten`'s insertion point and is bracketed because part one made it conditional.
The outermost adapter is entered first and completes **last**; the reflective adapter is entered last
and completes first. Reading that chain as a sequence of events for a single object gives five stages.

**Stage 1 - key arbitration.** Before any field binds, the buffering factories materialise the
object's JSON node and rewrite it: `@Capture` claims and removes the keys its filters and catch-all
match, `@Lenient` diverts entries whose key or value type does not fit the declared generics into
overflow, `@Collapse` records entry-key order, `@SerializedPath` retains the untouched tree, and
`@Flatten` would unwrap each map value. What reaches the reflective adapter is the residual tree.
Available guarantee: **the whole node is in memory.** Nothing about any other object.

**Stage 2 - bind.** The reflective adapter constructs the instance and populates declared,
non-`transient` fields in **JSON document order**, recursing fully into each child - a child's entire
adapter chain, including its own `postInit()`, completes before the parent's next field is read.
Available guarantee: **none.** A field being bound may observe only fields whose keys preceded it, and
`f02-postinit-bottom-up-order` proves that order is not contractual: across two profiles of one
account `accessory_bag_storage` sits at index 7 and 3, `inventory` at 24 and 14, and `rift` is present
in one and absent in the other. **No annotation may read a sibling at this stage.**

**Stage 3 - per-object post-bind writes.** Control unwinds outward. `@SerializedPath` assigns its
path-addressed fields (`SerializedPathTypeAdaptorFactory.java`:105-141, and per §14.1 it is the only
factory that can write a `transient` field), `@Capture` assigns the maps it built, `@Lenient` records
overflow against the instance, `@Collapse` injects `@Key` into each value, and `PostInit` fires the
hook. Available guarantee: **every declared field of this object is bound and every descendant is
complete.** Not available: this object's siblings, its parent's other fields, or anything derived.

**Stage 4 - reach-back.** Not a phase and not a factory: one call from the one frame that holds both
objects. `SkyBlockMember.postInit()` hands `AccessoryBag` its three values (§13.3). It works because
stage 3 is bottom-up, so the member's hook is the last in its subtree to fire and every descendant is
complete. Available guarantee: **the whole member subtree.** Still not the island, and still nothing
derived.

**Stage 5 - derivation.** Not a phase either: first access to a lazy accessor, arbitrarily later.
Available guarantee: **the entire response is decoded, and every other derivation this one needs will
compute itself on demand, in dependency order, because the call stack enforces it.** This is the
strongest guarantee in the model and it is the only one that costs nothing.

### 19.3 What each stage may rely on, in one table

| Stage | Runs | May read | Must not read | May write |
| --- | --- | --- | --- | --- |
| 1 key arbitration | per object, before bind | its own buffered JSON node | anything typed | the buffered tree |
| 2 bind | per field, JSON document order | fields whose keys already appeared - **which is unknowable** | any sibling, any parent | its own field |
| 3 post-bind writes | per object, unwinding | all own fields, all descendants | siblings, ancestors, derived values | own fields, including `transient` via `@SerializedPath` |
| 4 reach-back | once, from the owning frame | the whole subtree of the frame that calls it | anything above that frame | the child's handover fields |
| 5 derivation | first access | everything, including other derivations | nothing | its own memo field |

Two rules fall out and both are worth stating as rules rather than as observations:

**A stage may only read what an earlier stage finished.** Stage 2 finished nothing, which is why the
rule "no annotation reads a sibling during bind" is absolute rather than a caution.

**Nothing at stage 3 or 4 may read a derived value.** That is what today's code violates: `SkillLevel`
is constructed at stage 4 and reads `collectionUnlocked`, a stage-5 value. Moving `Skills` behind a
lazy accessor (§13.4) moves the read to stage 5 where it is legal. Once that is done, the constraint
is enforced by construction rather than by review - a stage-5 value has no way to be observed earlier,
because nothing computes it until someone asks.

### 19.4 What `postInit()` is for after part two

The model above leaves `PostInit` with one job: **stage 4**. That is a large reduction and it is part
two's main structural claim, so it is stated with the per-implementor disposition and handed to
`11-postinit-elimination.md` to sequence.

| Implementor | Disposition | By |
| --- | --- | --- |
| `Election` | retired | two lazy getters over `year` |
| `CrimsonIsle` | retired | §14.4 - rename the two staging fields, stop pushing into `Kuudra` |
| `JacobsContest` | retired | §15.3 - `@Collapse` + `@Key`, then parse the id lazily |
| `Dungeons` | retired | §15.4 lazily, or `f01-dungeons-capture-grouping`'s `@Capture` route |
| `Bestiary` | retired | lazy `families`, plus the `Matcher.group` fix |
| `SkyBlockMember` | **survives**, as one statement | §13.3's three-value handover |

Five of six retire, and the survivor is the reach-back. That is the honest end state: `PostInit` is
not eliminated, it is narrowed to the one thing no annotation in this pack can express - handing a
child values from a frame it cannot see.

### 19.5 The one library change part two asks for

**Make `PostInitTypeAdapterFactory.java`:35-39 log.** It is a completely empty
`catch (Exception ex) {}`, while `PostInit.java`:12-13's own javadoc states that exceptions are
"logged and swallowed". This is therefore not a behaviour change at all - it is the code being made to
match its documented contract, which is why it is additive and safe for every module already pinned to
`gson-extras`. Rethrowing would be a real behaviour change and is not proposed.

It is worth the cycle even though `postInit()` is about to shrink to one statement, precisely
*because* it shrinks to one statement: the survivor is the reach-back handover, and a failure there is
exactly the silent class of defect this catch has been hiding. `f02-accessorybag-dead-initialize` is
the proof - an `NbtException` thrown on the first statement of every member's `postInit()`, aborting
the hook, leaving `skills` null and `collectionUnlocked` empty for every profile ever decoded, with no
signal anywhere. `f05-matcher-group-without-match` is the second proof, in `Bestiary`.

**Sequencing, which part one §12.2 explicitly handed to part two: land the consumer fixes first.**
Today `Bestiary` and `AccessoryBag` throw on every single decode, so switching the catch to a log
before those are fixed produces one log line per member per request and trains everyone to ignore it.
The order is: fix the four `Matcher.group` sites and `AccessoryBag`'s read-before-assign and dead
store, verify a clean decode of the fixture, then land the logging change in the same `gson-extras`
commit as `@Fallback`, then re-pin. Because the library change needs a JitPack cycle anyway, the
consumer work lands first naturally - the requirement is only that nobody reverses it for convenience.

### 19.6 Verdict

**Reject `@Bind`.** The ordering evidence behind it is the strongest in the pack and it argues for
laziness, not for an engine. The model in §19.2 is what the pack ships instead: five stages, no new
phase, no new guarantee, and the only stage that can read everything is the one that costs nothing.

Cite §19.2 rather than this verdict when a future proposal needs to know what it may rely on.

## 20. Part two summary

**Part two proposes no new annotation at all.** Seven registry rows were examined and seven are
rejected or declined. The single library change it asks for is nine lines of logging in
`PostInitTypeAdapterFactory`, and that change is a documentation-conformance fix rather than a new
capability.

That is a stronger version of part one's uncomfortable result, and the reason is different. Part one
found that most shape residue was an existing annotation nobody had applied. Part two finds something
else: **the reach-back and derivation residue is not an annotation gap, it is an eagerness bug.**
Every eager derivation in this package is either wrong or fragile - `SkyBlockMember`, `AccessoryBag`,
`Bestiary` and `Dungeons` are producing incorrect output right now - and every lazy one is fine.
Annotations in this space would have made the wrong pattern more convenient to write.

### 20.1 Registry disposition

| Registry entry | Verdict | Because | Library change | Effort avoided |
| --- | --- | --- | --- | --- |
| `@Owner` / `@Parent` | **decline** | 1 bound customer; three values copied down beat a reference handed up, and break the cyclic import too | none | `large` |
| ancestor-relative `@SerializedPath` | **decline** | `^` counts adapter frames, the author counts objects, and a string cannot check the difference; its one site needs a rename instead | none | `large` |
| `@Derive` | **reject** | `PostInit` with a reflected method name; its only advantage is ordering, which laziness dissolves | none | `large` |
| `@Index` / `@Join` | **reject** both | two operations, one site each; 11 of the lookups need a repository the library must never depend on | none | `large` x2 |
| `@Tier` | **reject**, kept as alias | 3 sites share the parse and none share the reduction | none | `large` |
| `@Aggregate` | **reject** | 0 materialized aggregates in 133 files; the row's premise is false | none | `large` |
| `@Bind` | **reject** | ordering is a symptom of eagerness; the call stack sorts the graph for free | none | `xlarge` |
| `PostInitTypeAdapterFactory` empty catch | **adopt** | `PostInit`'s own javadoc already promises logging; two shipped defects hid behind it | 9 lines, additive | - |

Consumer-side work part two asks for, none of which needs a library cycle:

| Change | Sites | Payoff |
| --- | --- | --- |
| Lazy memoised getters for the four eager derivations | `SkyBlockMember`, `Dungeons`, `Bestiary`, `JacobsContest` | 4 `PostInit` implementors retired, 3 live wrong values fixed, 77,500 regex compilations per member removed |
| `Skills` behind a lazy accessor | `SkyBlockMember`:143 | FORAGING level subtractor stops being unconditionally `2` |
| Narrow `AccessoryBag.initialize` to its three values | `AccessoryBag`, `SkyBlockMember` | cyclic package import removed, read-before-assign and dead store made unwritable |
| Rename `CrimsonIsle`'s staging fields, stop pushing into `Kuudra` | `CrimsonIsle`, `Kuudra` | 1 `PostInit` implementor retired, 4 fields and 2 suppressions deleted |
| `@Collapse` + `@Key` on `JacobsContest.contests` | `JacobsContest` | 1 `PostInit` implementor retired, 2 phantom serialized keys removed |
| Fix the four `Matcher.group` sites and the master-mode case mismatch | `Bestiary`, `DungeonRun`, `Dungeons` | the bestiary feature turns back on; master-mode floor data becomes visible |
| One `<id>_<n>` parse helper | `SkyBlockMember`, `PlayerData`, `Bestiary` | 3 hand-rolled scans unified without touching the library |

### 20.2 The four questions part one carried forward

**1. Does `@Flatten` get a shared cycle?** **Yes.** Part two asks for one library change - the
`PostInitTypeAdapterFactory` logging fix - and it rides the same commit as `@Fallback`. So the pack
has exactly one `gson-extras` publish, carrying `@Fallback`, the logging fix and, as a rider,
`@Flatten`. `d10-flatten` §6.9's condition is met and `@Flatten` should ship. If `@Fallback` slips,
the logging fix is not enough on its own to justify a cycle for a one-site annotation, and `@Flatten`
drops with it exactly as §6.9 says.

**2. Does any derivation want a field-level `@Fallback`?** **No.** The defaults that appear in
derivation code - `orElse(0)` in the collection join, `EMPTY_CLASS` and `EMPTY_DUNGEON` in
`Dungeons.java`:24-25, `getOrDefault` in `SkillLevel.java`:32-33 - are all *lookup-time* misses on a
fully populated map, long after binding. Part one already put that class out of scope as
`f04-lookup-sentinel-drift` and it is right. No widening, no new row; `@Fallback` stays the
enum-constant marker part one narrowed it to.

**3. Where does "treat value X as absent" live?** Part two agrees with part one: nowhere, and take the
`trivial` consumer fix. One thing to add, because it strengthens the answer rather than merely
confirming it - `f06-completedat-zero-sentinel` (`completed_at: 0` binding to a present
`Optional`) and `f05-negative-tier-exclusion` (`MELON_-1` meaning tier zero) are the **same shape**:
a wire value that means absent. Both are one explicit line in the consumer that *states* the intent,
and in both cases the annotation form would hide it again - which is exactly the defect, since the
tier exclusion is currently hidden inside a character class and reads as an oversight. Two instances
of a pattern whose correct treatment is "say it out loud" is evidence against the annotation, not for
it.

**4. How is ancestor-relative `@SerializedPath` priced?** As `large`, not as a factory edit. §12.3
correctly identified it as an edit to `SerializedPathTypeAdaptorFactory` rather than a new factory,
and §14.3 confirms the parent-side half is genuinely about twenty library lines. But the capability
the row actually wants - a child addressing a key above its own node - additionally needs a
`ThreadLocal` ancestor stack, a static reachability scan at `create` time to decide which ancestors
must buffer, and a frame-versus-object rule that the notation cannot express (§14.2). That is a new
cross-frame guarantee, which is the `large` row.

### 20.3 Sequencing

Part two's work is almost entirely consumer-side, so it sequences by dependency, not by cost.

1. **The correctness fixes, consumer-only, no cycle.** Four `Matcher.group` sites, `AccessoryBag`'s
   read-before-assign and dead store, the master-mode case mismatch. These turn two features back on
   and they are the acceptance test for everything after them.
2. **The reach-back narrowing.** `AccessoryBag.initialize` to three values, `AccessoryBag`'s derived
   state behind memoised accessors. Depends on step 1, because the current defects are inside the
   method being reshaped.
3. **The laziness conversions.** `collectionUnlocked`, `Skills`, `Dungeons`, `Bestiary`,
   `JacobsContest`. Four `PostInit` implementors retire here.
4. **`CrimsonIsle`'s rename.** Independent of everything above; the fifth implementor retires.
5. **The single library cycle** - `@Fallback`, the `PostInitTypeAdapterFactory` logging fix, and
   `@Flatten` as a rider - then one pin edit, then adopt. Deliberately last, so the new logging sees a
   clean decode rather than a defect per member (§19.5).

Steps 1-4 are one module, zero JitPack cycles, and individually revertable.
`20-implementation-plan.md` owns the final ordering; this is the dependency graph it has to respect.

## 21. Whole-document registry disposition

Every row of `00-conventions.md` §6.2, plus the four "extend an existing annotation" routes and the
one library defect, with its final verdict. This table is the document's output;
`11-postinit-elimination.md` and `20-implementation-plan.md` should read it and nothing else.

| Registry entry | Verdict | Entry | Library change | Effort |
| --- | --- | --- | --- | --- |
| `@Fallback` | **adopt narrowly** | `d10-fallback` | `CaseInsensitiveEnumTypeAdapterFactory` edit + 1 annotation file | `medium` |
| `@Flatten` | **adopt narrowly, as a rider** | `d10-flatten` | 1 annotation file + 1 new factory | `small` |
| `@Inline` | **reject** | `d10-inline` | none - `@SerializedPath` covers all 9 census holders, 11 classes unioned | `trivial` decision |
| `@Delegate` | **reject** | `d10-delegate` | none - stock Lombok ships it and still loses | `trivial` decision |
| `@Alias` | **reject** | `d10-alias` | none - `@SerializedName(alternate = ...)` is stock gson | `trivial` decision |
| `@Owner` / `@Parent` | **decline** | `d10-owner-parent` | none - narrow the handover instead | `large` avoided |
| `@Derive` | **reject** | `d10-derive` | none - lazy memoised getters | `large` avoided |
| `@Index` / `@Join` | **reject** both | `d10-index-join` | none - 1 site each, 11 need a forbidden dependency | `large` avoided |
| `@Tier` | **reject**, kept as alias of `@Index` | `d10-tier` | none - one consumer-side helper | `large` avoided |
| `@Aggregate` | **reject** | `d10-aggregate` | none - 0 adoption sites exist | `large` avoided |
| `@Bind` | **reject** | `d10-bind` | none - the call stack orders derivations | `xlarge` avoided |
| `@Capture` unmatched-key element | **decline** | `d10-capture-unmatched` | none - subsumed by `@Fallback` | `medium` avoided |
| `@Capture` value-grouping element | **decline** | `d10-capture-value-grouping` | none - 1 site, 8 lines | `medium` avoided |
| `@Lenient` typed-overflow element | **decline** | `d10-lenient-overflow` | none - take the free partial | `medium` avoided |
| ancestor-relative `@SerializedPath` | **decline** | `d10-ancestor-path` | none - its one site needs a rename | `large` avoided |
| `PostInitTypeAdapterFactory` empty catch | **adopt** | `d10-bind` §19.5 | 9 lines, additive, matches existing javadoc | `small` |

**Two accepts and one defect fix, out of sixteen proposals, in one JitPack cycle.**

The disproportion is the result, not an apology for it. `@SerializedPath`, `@Capture`,
`@Collapse`/`@Key`, `@Lenient`/`@Extract` and `@Split` already cover every shape mismatch in 133 files
except two - the value side of a map entry and an unrecognized enum value - and the reach-back and
derivation axes turn out not to want annotations at all. What the surveys called residue is, in
descending order of volume: an existing annotation that was never applied, a stock gson or Lombok
feature nobody reached for, a getter that should have been lazy, and - in a handful of places that
matter more than any of the above - a live defect.

Adoptions of annotations that **already ship**, which between them outweigh everything in the table
above and cost nothing:

| Existing annotation | Sites | Owner entry |
| --- | --- | --- |
| `@SerializedPath` | 9 census holders / 11 classes unioned, 13 fields | `d10-inline` §4.3 |
| `@Lenient` + `@Extract` | 3 full, 1 partial | `d10-lenient-overflow` |
| `@Capture` | 3 | `d10-fallback` §7.3, `d10-capture-value-grouping` |
| `@Collapse` + `@Key` | 2 | `d10-derive` §15.3 |
| `@Split` | 1 | `d10-fallback` §7.8 |
| `@SerializedName` | 3 | `d10-alias` |
| stock `@Getter` / `@Getter(AccessLevel.NONE)` | 4 | `d10-inline`, `d10-flatten` §6.1 |

**The single most valuable thing in this document is not an annotation.** It is the observation, which
three surveys reached independently, that `PostInitTypeAdapterFactory`'s empty catch has been hiding a
total failure of `SkyBlockMember.postInit()` on every decode, a dark bestiary, an always-empty
master-mode dungeon and a permanently wrong foraging subtractor. Nine lines of logging and seven
consumer-side fixes recover all of it, and none of that work needs a single row of the registry.
