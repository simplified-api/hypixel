# 00 - Conventions

The naming/format spine for the JSON-annotation research pack. Every sibling document obeys this
file. Sibling documents do not read each other.

## 1. Purpose

`api/simplified/hypixel/response` is 133 DTO classes / ~6600 LOC that model a hostile JSON surface -
Hypixel's SkyBlock API. The existing `gson-extras` annotation set (`@SerializedPath`, `@Capture`,
`@Collapse`/`@Key`, `@Lenient`/`@Extract`, `@Split`) already removed a large amount of hand-rolled
deserialization, but a residue remains: `PostInit` bodies that hand-compute derived state, delegation
getters that exist only to forward through a private nested holder, transient fields wired up by an
`initialize(parent)` reach-back, and the same structural pattern re-typed in a dozen classes.

This pack answers one question: **which of that residue is a missing declarative annotation, and what
would it cost to build?** It is a research pack, not a plan of record - it surveys, proposes, and
estimates. The implementation document (`20-implementation-plan.md`) is the only file that sequences
work, and it sequences only what the surveys justified.

How the files fit together:

- **Surveys (01-06)** each own one axis of the residue. They read source, cite exact
  `file:line` evidence, and emit findings. A survey never proposes an annotation design - it states
  the problem and, at most, names the registry entry it believes applies.
- **Designs (10-11)** consume the surveys' findings by ID, group them into candidate annotations, and
  specify each one: annotation source, factory changes, semantics, failure modes, and what would
  *not* be covered. `11-postinit-elimination.md` is the focused question of whether `PostInit` can
  shrink toward zero.
- **Plan (20)** sequences the accepted designs into ordered, individually revertable steps with the
  gson-extras publish/re-pin cost made explicit at each boundary.
- **README.md** is the reading order and a one-line summary per file.

## 2. Doc map

All files live in `notes/json-annotations/`. These filenames are fixed - do not invent, rename, split
or merge. If a survey has nothing to report, it still ships with its headings and an explicit
"no findings" statement.

| File | Owns | Emits finding IDs prefixed |
| --- | --- | --- |
| `00-conventions.md` | This spine - doc map, IDs, effort, categories, naming registry, glossary, house style | none |
| `01-postinit.md` | Survey of all 6 `PostInit` implementors: what each `postInit()` actually computes, its inputs, and whether the computation is bind-expressible | `f01-` |
| `02-parent-access.md` | Survey of reach-back: `initialize(parent)`, transient fields needing the enclosing object, and cross-object references that Gson cannot supply | `f02-` |
| `03-value-shape-collapse.md` | Survey of shape mismatch between JSON and field: private holder classes, wrapper-only nested types, single-field objects, `Map<K, Map<String, V>>` funnels | `f03-` |
| `04-accessor-boilerplate.md` | Survey of accessors that carry no logic: pure delegation getters, `@Getter(AccessLevel.NONE)` + hand-written forwarder pairs, `getOrDefault`-on-empty-sentinel accessors | `f04-` |
| `05-cross-field-derivation.md` | Survey of fields computed from *other* fields: joins, max-of-matching-key scans, id-to-repository lookups, sums and averages materialized into transients | `f05-` |
| `06-structural-duplication.md` | Survey of the same shape re-implemented across classes: repeated nested-holder idioms, repeated enum-keyed count maps, repeated quest/boss/floor skeletons | `f06-` |
| `10-annotation-designs.md` | Designs for every proposed annotation drawn from the registry - signature, retention/target, factory work, semantics, interaction with existing annotations, rejected alternatives | `d10-` |
| `11-postinit-elimination.md` | The `PostInit` end-state: per-implementor, which proposed annotation retires it, what stays imperative, and why | `d11-` |
| `20-implementation-plan.md` | Ordered steps, gson-extras publish boundaries, JitPack re-pin points, verification per step, rollback | `s20-` |
| `README.md` | Reading order, one-line summary per file, top findings by effort/payoff | none |

Cross-references between files use the finding or design ID, never a page/section number - e.g.
"see `f05-collection-tier-join`", not "see 05 section 3".

## 3. Finding ID scheme

Shape: `<prefix><survey-number>-<kebab-slug>`.

```
f01-skyblockmember-collection-join
f02-accessorybag-upstream
f03-dungeons-floordata-holder
d10-derive-annotation
s20-publish-gson-extras
```

Rules:

- Prefix is `f` for a survey finding, `d` for a design entry, `s` for a plan step.
- The number is the owning document's number, zero-padded to two digits, and never changes even if
  the finding is later discussed elsewhere. A finding is owned by exactly one document.
- The slug is kebab-case, lowercase ASCII, no underscores, no dots. Aim for 2-4 words. Lead with the
  subject class in lowercase when the finding is class-specific (`accessorybag`, `skyblockmember`,
  `jacobscontest`) - class names lose their camel-case, they are not hyphenated apart.
- Slugs are unique within a document; the prefix+number makes them unique pack-wide.
- Once written, an ID is frozen. Renaming an ID breaks every sibling reference, and siblings cannot
  see the rename.

Every finding is presented with this fixed field block so downstream docs can machine-read them:

```
### f0N-slug
- **Category:** <one of the category list, exactly as spelled below>
- **Where:** <repo-relative path>:<line> (repeat for each site)
- **What:** one sentence, present tense, states the current behavior
- **Why it is residue:** one or two sentences
- **Candidate annotation:** <registry name, or "none - keep imperative">
- **Effort:** <trivial | small | medium | large | xlarge>
```

Design entries (`d10-`, `d11-`) use their own block defined in their own file, but keep the same ID
shape and the same `Category` / `Effort` vocabularies.

## 4. Effort scale

**The library cost floor.** `gson-extras` is consumed by git sha through JitPack. There is no local
snapshot loop and no version range. Therefore *any* change to `gson-extras` - even a one-line javadoc
fix - costs the full cycle: commit, push, trigger a JitPack build, wait for it to go green, then edit
the consuming module's dependency pin to the new sha, then rebuild and re-test the consumer. That
cycle is minutes of wall clock and is not parallelizable across sibling modules that share the pin.

Consequence, and state it in every estimate: **no proposal that touches `gson-extras` can be rated
`trivial`.** `small` is the floor for a library change, and only when the library change is a single
additive file with no factory edit. A proposal that changes an existing factory's behavior starts at
`medium` because the blast radius includes every module already pinned to that factory.

| Level | Files touched | Library change | JitPack re-pin | Typical shape |
| --- | --- | --- | --- | --- |
| `trivial` | 1 file, consumer only | none | no | Delete a redundant delegation getter; add `@Accessors(fluent = true)`; rename a field to match the JSON key so `@SerializedName` can go |
| `small` | 1-3 files, or 1 additive library file | additive only - a new annotation with no factory change, or a new annotation plus a self-contained new factory | yes, one cycle | Introduce an annotation whose whole implementation is a new `TypeAdapterFactory` registered in `GsonSettings`; adopt it at 1-3 call sites |
| `medium` | 4-15 files, or an edit to an existing factory | modifies an existing factory's read/write path, or adds an element to an existing annotation | yes, one cycle, plus a regression pass over every existing user of that factory | Extend `@Capture` with a new mode; add a post-bind phase to `PostInitTypeAdapterFactory`; retire 3-5 `postInit()` bodies |
| `large` | 16-40 files, or a new adapter *phase* | new ordering guarantee between factories, or a new lifecycle hook the whole pipeline must honor | yes, and the pin must land in lockstep across consuming modules | A reach-back mechanism requiring parent context threaded through every adapter; a derivation phase that must run after all binds complete |
| `xlarge` | 40+ files, or a semantic break | changes the meaning of an existing annotation for existing users, or reorders the factory chain | yes, and every consumer of the changed annotation needs review, not just recompile | Redefining `@Capture` grouping defaults; replacing `PostInit` with a general dependency-ordered derivation engine |

Notes for estimators:

- Count *files touched*, not lines. A 200-line rewrite of one class is smaller than a 3-line edit
  across 30 classes, because the second one is where review and regression cost lives.
- If a proposal needs two library cycles (add annotation, then adopt, then fix, then re-pin), bump it
  one level. Round-trips dominate.
- Serialization is not optional. `@Lenient` and `@Collapse` both preserve round-trip fidelity; any
  new annotation that only handles reads must say so explicitly, and that gap is itself a cost.
- If the estimate is uncertain, give the higher level and say what would resolve the uncertainty.

## 5. Category list

Exactly these nine slugs. Spell them as written - they are compared across documents. One category
per finding; if two seem to apply, pick the one describing the *cause*, not the symptom.

- **`annotation-abstraction`** - a JSON shape is handled by hand that a declarative annotation could
  express. The generic bucket, and the parent of most of the others. Use it only when no more
  specific category below fits.
- **`postinit-elimination`** - work performed inside a `PostInit.postInit()` body that need not be
  imperative. The finding is about the *hook*, not merely about derived state; if the computation
  happens somewhere other than `postInit()`, it is `cross-field-derivation` instead.
- **`parent-access`** - a nested object needs a reference to its enclosing (or an ancestor) object to
  finish binding. The current workaround is a manual `initialize(parent)` call from the parent's
  `postInit()`, or an assignment that pushes state downward into a child field.
- **`value-shape-collapse`** - the Java shape carries a level of nesting the caller does not want:
  a private holder class existing only to name a JSON sub-object, a single-field object that should
  be its scalar, a map-of-maps that should be a map-of-values.
- **`accessor-boilerplate`** - accessors with no logic. Pure forwarders, `@Getter(AccessLevel.NONE)`
  paired with a hand-written getter that just reads through, and default-sentinel lookups repeated
  per class.
- **`cross-field-derivation`** - a field's value is a function of one or more sibling fields, or of a
  static repository keyed by a sibling field. Joins, max-of-matching-keys, id-to-model resolution,
  and materialized aggregates all land here.
- **`duplication`** - the same structural idiom re-typed across classes with only names changed.
  About repetition of *shape*, not repetition of *logic* - repeated logic that computes from siblings
  is `cross-field-derivation`.
- **`correctness`** - the current code is wrong, lossy, or silently drops data. Includes regexes that
  exclude valid inputs, `@Lenient` overflow that is never extracted, enum fallbacks that swallow new
  API values, and non-round-trippable reads. These are reported even when no annotation is proposed.
- **`naming`** - a field, class, accessor or serialized name misleads: a Java name that does not match
  its JSON meaning, a holder class named after JSON rather than after the domain, or an accessor whose
  name promises something its body does not do.

## 6. Naming registry - proposed annotations

**The rule:** if a survey wants to name a concept, it takes a name from this table. Later documents
may *refine* an entry's semantics, narrow it, split its options, or reject it outright - but they must
not **rename** it, because siblings that already cite the old name cannot see the change. If a
genuinely new concept appears that no entry covers, add a row rather than reusing a near-miss;
say "new registry entry" when you do, so the design document knows it was not seeded here.

Two names for one concept in this table (`@Owner`/`@Parent`, `@Index`/`@Join`) are *alternatives* to
be decided in `10-annotation-designs.md`. Until that decision, cite the pair as written here
(`@Owner`/`@Parent`) rather than picking one silently.

### 6.1 Already exists - do not re-propose under a new name

| Name | Package | Owns |
| --- | --- | --- |
| `@SerializedName` | `com.google.gson.annotations` | flat key rename |
| `@SerializedPath` | `dev.simplified.gson.annotation` | dot-path descent to a nested value |
| `@Capture` | `dev.simplified.gson.annotation` | dynamic keys - regex filter, catch-all, affix grouping, `Grouping.ENTRY` |
| `@Collapse` + `@Key` | `dev.simplified.gson.annotation` | JSON object to `Map`/`List` with the entry key injected into the value |
| `@Lenient` + `@Extract` | `dev.simplified.gson.annotation` | type-incompatible entries to overflow, and pulling one named entry back out |
| `@Split` | `dev.simplified.gson.annotation` | one delimited string into a `Pair`/`PairOptional` |
| `PostInit` | `dev.simplified.gson` | imperative post-deserialization hook |

If the right answer is "add an element to `@Capture`" or "extend `@Lenient`", say exactly that -
extending an existing annotation is a different (usually cheaper, sometimes riskier) proposal than a
new one, and it is rated `medium` or above per the effort scale.

### 6.2 Reserved candidates

| Name | Intent (one line) |
| --- | --- |
| `@Owner` / `@Parent` | Injects the enclosing or ancestor object into a nested object's field during bind, replacing a manual `initialize(parent)` reach-back |
| `@Derive` | Marks a transient field as computed after bind, naming the computation so the hook is declarative rather than an imperative `postInit()` body |
| `@Index` / `@Join` | Resolves a field's value by looking another field's value up in a keyed source - a sibling map, a sibling collection, or a static repository |
| `@Flatten` | Collapses a single-valued JSON object (or single-field value class) into the scalar or collection the caller actually wants, removing a wrapper level |
| `@Alias` | Accepts more than one JSON key for the same field, for API keys that were renamed upstream or that vary by profile age |
| `@Tier` | Reads a level/tier from an id-plus-number key family, taking the max match for a given base id, rather than hand-rolling the regex-and-max scan |
| `@Inline` | Binds the fields of a named JSON sub-object directly onto the enclosing class, retiring a private holder class that exists only to name that object |
| `@Fallback` | Supplies a default when the key is absent or the value fails to bind, replacing sentinel constants plus `getOrDefault` accessors |
| `@Delegate` | Generates a forwarding accessor to a field of a nested object, retiring a hand-written pure-delegation getter |
| `@Aggregate` | Materializes a sum/max/average/count over a sibling collection into a field, for aggregates that are currently computed in `postInit()` |
| `@Bind` | Naming placeholder for a general ordered post-bind phase, if the pack concludes `@Derive` needs explicit dependency ordering rather than a flat pass |

Guidance on the seeded pairs and near-neighbours - resolve these in `10-annotation-designs.md`,
do not resolve them silently in a survey:

- `@Owner`/`@Parent` versus threading context. Both names describe *field injection*. If the pack
  concludes the real need is a context object available to every adapter, that is a different design;
  name it in the design doc and note that it supersedes this row.
- `@Index` versus `@Join`. `@Index` reads as "look me up by key"; `@Join` reads as "merge two
  sibling collections". If both operations turn out to be needed they get separate rows - but they
  are seeded as one row because the survey evidence so far may not distinguish them.
- `@Flatten` versus `@Inline`. `@Flatten` removes a level on the *value* side (one JSON object to one
  scalar/collection). `@Inline` removes a level on the *field* side (a sub-object's keys become the
  enclosing class's fields). `@SerializedPath` already covers the single-field case of `@Inline`;
  `@Inline` only earns its keep for multi-field holders.
- `@Tier` versus `@Index`. `@Tier` is the narrow, well-evidenced case (an id-and-number key family);
  `@Index` is the general lookup. If `@Index` subsumes `@Tier` cleanly, say so and keep `@Tier` as a
  documented alias in the design doc rather than deleting the row.
- `@Derive` versus `@Aggregate` versus `@Bind`. All three are post-bind computation. `@Derive` is the
  general hook, `@Aggregate` is the narrow arithmetic case, `@Bind` is the escape hatch if ordering
  between derived fields turns out to matter. Prefer the narrowest one the evidence supports.

## 7. Glossary

Use these terms as defined. They are the vocabulary the surveys and designs share.

- **bind phase** - the part of deserialization where Gson and the registered
  `TypeAdapterFactory` chain read JSON and populate fields. Everything an annotation can influence
  today happens here. A value that can be produced during bind needs no hook.
- **post-bind phase** - anything that runs after a value's fields are populated. Today this is only
  `PostInit.postInit()`, invoked by `PostInitTypeAdapterFactory`; it runs per-object, exceptions from
  it are logged and swallowed, and there is no declared ordering between sibling objects. A proposal
  that needs guaranteed ordering must say so, because that ordering does not currently exist.
- **reach-back** - a nested object reading state from its enclosing or ancestor object. Gson gives a
  child no reference to its parent, so the current workaround is the parent calling a method on the
  child and passing itself, as in `SkyBlockMember.postInit()` calling
  `accessoryBag.initialize(this)`.
- **catch-all** - a bare `@Capture` with no `filter`. It collects every JSON entry on the object that
  no declared field and no filtered `@Capture` claimed. At most one per class.
- **affix grouping** - `@Capture`'s class-value mode. When the map's value type is a class with
  fields, keys are split against that class's serialized names and grouped, so
  `song_hymn_joy_completions` becomes `songs["hymn_joy"].completions`. Plain field names are treated
  as auto-suffixes with `_` prepended, which is why a key like `daily_progress` can be split apart
  unintentionally; `@Capture(grouping = Grouping.ENTRY)` forces a whole-object read instead.
- **entry mode** - `@Capture`'s simple mode, selected automatically for primitive, `String`, enum,
  `Map` and `Collection` value types, or forced with `Grouping.ENTRY`. Each captured entry's value is
  read whole, with no affix splitting.
- **overflow** - entries a `@Lenient` field discarded because their key or value type did not match
  the declared generics. They are kept aside for round-trip fidelity and merged back on serialize.
  `@Extract` is the only way to read one back into a typed field, and it addresses the source by its
  **Java field name**, not its serialized name - `@Extract("kills.last_killed_mob")` refers to the
  Java field `kills`.
- **holder class** - a private nested class whose only purpose is to name a JSON sub-object so its
  keys can bind, typically paired with `@Getter(AccessLevel.NONE)` on the field and hand-written
  forwarding accessors on the enclosing class. `SkyBlockMember.Profile`, `SkyBlockMember.Events` and
  `Dungeons.DungeonTreasures` are the canonical examples.
- **residue** - the hand-written code that survives after the existing annotations have done their
  work. The pack's subject matter.

## 8. House style digest

Every Java snippet in this pack - proposed annotation sources, factory sketches, before/after DTO
excerpts - must already comply. Reviewers read snippets as if they were about to be pasted in.

**Javadoc**

- Single hyphens ` - ` only. Never em dashes, never `&mdash;`, never `--`.
- Class and interface doc is a **noun phrase**. Method doc is a **third-person verb**
  ("Reads...", "Returns...", "Injects..."). Field and record-component doc is a **fragment with no
  trailing period** and no tags.
- Include `@param` / `@return` / `@throws` where applicable, lowercase fragments, no trailing period,
  a single space after the param name - never column-aligned.
- Never `@author`, never `@since`.
- Import javadoc link targets rather than inlining fully-qualified names; use `{@link}` /
  `{@linkplain}` / `@see` for references and `{@code}` inline. (The one exception in the wider
  codebase is `package-info.java`, which is out of scope for this pack.)
- Field-like docs live on the field, never on the accessor - including Lombok `@Getter`-generated
  accessors. No `Gets ` / `Returns ` prefix there, no `@return`.
- `<p>` on its own line between paragraphs; `<ul>`/`<li>` for lists.
- Overrides get `/** {@inheritDoc} */` and nothing more.

**Control flow**

- Omit braces on a single-line body. Add braces only when the body wraps across lines. Applies to
  every single-statement form.

**Collections**

- `getFirst()` / `getLast()` for sequenced access - never `get(0)` or `get(size() - 1)`. Excludes
  non-`SequencedCollection` APIs such as Gson's `JsonArray`.

**Lombok**

- `@Getter` on the class; `@Getter(AccessLevel.NONE)` to suppress an individual field;
  `@Accessors(fluent = true)` for the `has*` / `is*` boolean style already used across these DTOs.

**No design scaffolding in Java**

- Java comments and javadoc must stand alone. Never cite a phase number, decision id, defect number,
  finding id, or any filename from this pack inside a `.java` file. State the what and the why
  inline. Notes files may cross-reference each other freely - only the Java is constrained.

**Exceptions** (only relevant if a proposal adds one)

- Constructor order `(cause)`, `(message)`, `(cause, message)`, `(message, args)`,
  `(cause, message, args)`; message starts uppercase with no trailing punctuation and uses `'%s'`
  around interpolated values; class doc reads "Thrown when [condition]."

## 9. Shared ground facts

Established. Do not re-derive these; cite them.

**Paths**

- Response DTOs - `src/main/java/api/simplified/hypixel/response` (133 files, ~6600 LOC), split into
  `forum/`, `hypixel/`, `resource/`, `skyblock/`.
- Library - `W:/Workspace/Java/Simplified/Simplified-Dev/gson-extras/src/main/java/dev/simplified/gson`,
  with `annotation/`, `factory/`, `adapter/`, plus `PostInit`, `GsonSettings`, `JsonTree`.
- `GsonSettings.defaults().create()` builds the `Gson`; a new factory must be registered there.
- Fixture - `src/main/resources/craftedfury.json`, a `SkyBlockProfiles` response shaped
  `{success, profiles: [{..., members: {uuid: {...}}}]}`. It is 1.6 MB - read shapes with a python
  one-liner, never by dumping the file.
- Coverage differ - `scripts/json_dto_diff.py`. Run it rather than eyeballing which JSON keys are
  unmodeled.

**PostInit implementors - exactly six**

`SkyBlockMember`, `Bestiary`, `Dungeons`, `CrimsonIsle`, `JacobsContest`, and
`response/skyblock/election/Election`. There are no others in this module. `01-postinit.md` owns the
full account of each; other documents cite `f01-` findings rather than re-reading the bodies.

**Annotation usage today** (files using each, in `response/`): `@Capture` 12, `@Lenient` 7,
`@Collapse` 1 (`Slayers`), `@Extract` 1 (`Bestiary`), `@Getter(AccessLevel.NONE)` 14.

**Decoded already - `SkyBlockMember.collectionUnlocked`**

A join, computed in `SkyBlockMember.postInit()`. `collection` is `itemId -> total collected`.
`player_data.unlocked_coll_tiers` is a **flat 775-element list of strings** shaped
`<itemId>_<tier>` - `INK_SACK:3_9`, `METAL_HEART_2`, `MELON_-1`. For each collected item the code
regex-matches `^<itemId>_[0-9]+$`, parses the tier, and takes the max, defaulting to `0`. Note the
regex excludes negative tiers such as `MELON_-1` - that exclusion is a `correctness` observation, and
it belongs to whichever survey files it first (`05-cross-field-derivation.md` by default).

**Recently fixed in `@Capture`** - an unmatched key whose value is already a complete object is now
read as that object, and `@Capture(grouping = Capture.Grouping.ENTRY)` forces whole-object reads.
Proposals must not re-solve this.
