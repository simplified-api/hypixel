# 02 - Parent Access

Survey of reach-back: every place a nested DTO needs its declaring object, a sibling subtree, or an
ancestor in order to finish. Owns finding IDs prefixed `f02-`.

## 1. Scope and method

A reach-back is a nested object reading state it does not own. Three shapes exist in this module and
they are not the same problem:

- **Upward** - a child reads its declaring object (`AccessoryBag` reading `SkyBlockMember`).
- **Sideways** - a child reads a *sibling subtree* of its declaring object. Every real reach-back in
  this module is actually this shape; the parent is only the routing hop.
- **Downward push** - the parent binds something at its own level and assigns it into a child's
  field, because the child's own JSON node does not contain it (`CrimsonIsle` into `Kuudra`).

Method: `Grep` for `initialize(`, for constructors and methods taking a DTO type as a parameter, and
for every `transient` field in `response/`; then read each `postInit()` body. Cross-checked against
`src/main/resources/craftedfury.json` with python for JSON key presence and, critically, **key
order** - see `f02-postinit-bottom-up-order`. The library side is
`Simplified-Dev/gson-extras/src/main/java/dev/simplified/gson`.

Result: the reach-back surface is **small and shallow**. Two bind-path sites (`AccessoryBag`,
`Skills`), one downward push (`CrimsonIsle` to `Kuudra`), one key-derived push (`JacobsContest`), and
one non-bind consumer (`ProfileStats`). Nothing anywhere needs the root response, and nothing needs
more than one level of ancestry. That smallness is the single most important input to the
`@Owner`/`@Parent` decision: the mechanism being contemplated is a pipeline-wide lifecycle change,
and it has two and a half customers.

The survey also turns up three defects. They are reported here rather than deferred because they are
all *caused by* the reach-back being hand-written and imperative, and one of them silently disables
`SkyBlockMember.postInit()` in its entirety.

## 2. Inventory of reach-back sites

### 2.1 AccessoryBag - the only explicit `initialize(parent)`

One call site, `SkyBlockMember.java:142`, inside `postInit()`. The method is
`AccessoryBag.initialize(SkyBlockMember)` at `AccessoryBag.java:55-164`, 110 lines.

Exhaustive list of what it consumes from the parent - three reads, all of them *sibling subtrees*,
none of them a field of `SkyBlockMember` itself:

| Site | Expression | Reaches |
| --- | --- | --- |
| `AccessoryBag.java:138` | `member.getInventory().getBags().getAccessories()` | `inventory.bag_contents.talisman_bag` (an `NbtContent`) |
| `AccessoryBag.java:135` | `member.getRift().getAccess().hasConsumedPrism()` | `rift.access.consumed_prism` (a boolean, worth +11 magical power) |
| `AccessoryBag.java:190` | `member.getCrimsonIsle().getAbiphone().getContacts().size()` | `nether_island_player_data.abiphone` contact count, halved, only when the accessory is `ABICASE` |

That is the whole upstream dependency: **one NBT blob, one boolean, one collection size**. The
`SkyBlockMember` parameter is threaded through `initialize` and again through the private
`handleMagicalPower(AccessoryData, SkyBlockMember)` at `AccessoryBag.java:182` purely to carry that
third read down one more frame.

Everything else in the 110 lines is local: it parses `contents` into `detectedAccessories`
(`:57-71`), de-duplicates accessory families into `accessories` (`:74-126`), sums magical power
(`:129-136`), then derives `tuningPoints`, `logComponent` and `selectedPowerStats` (`:139-163`). Six
`transient` fields exist only to hold those results (`AccessoryBag.java:34-36,43,48-49,53`).

The three reads are not equally awkward. `consumed_prism` and the abiphone contact count are
**scalars pulled across the object graph**; the talisman bag is a whole `NbtContent` node that
`AccessoryBag` arguably should have owned in the first place, except that the API puts it under
`inventory`, not under `accessory_bag_storage`. Confirmed in the fixture: `accessory_bag_storage`
holds only `tuning`, `selected_power`, `bag_upgrades_purchased`, `unlocked_powers`,
`highest_magical_power` - no item data at all.

### 2.2 Skills and SkillLevel - reach-back through a constructor

`SkyBlockMember.java:143` builds `new Skills(this.getPlayerData().getSkillExperience(), this)`.
`Skills` is not a bound type at all - it has no serialized fields, it is constructed from a map plus
the member (`Skills.java:19-24`), and it stores nothing of the member. It forwards the reference one
level further into `new SkillLevel(id, experience, member)` (`Skills.java:22`).

`SkillLevel` also does not retain the member. It uses it exactly once, in
`calcLevelSubtractor(SkyBlockMember)` at `SkillLevel.java:27-40`, which reads:

- `member.getJacobsContest().getFarmingLevelCap()` - sibling subtree, for `FARMING`.
- `member.getCollectionUnlocked().getOrDefault("FIG_LOG", 0)` and `"MANGROVE_LOG"` - for `FORAGING`.

The second one is the interesting case, and it is broken. `collectionUnlocked` is a `transient` field
of `SkyBlockMember` (`SkyBlockMember.java:130`) that is itself computed in `postInit()` - at
`SkyBlockMember.java:145`, **two statements after** the `new Skills(...)` on line 143. So
`SkillLevel` reads a map that the enclosing `postInit()` has not filled yet. It is always the empty
initial `Concurrent.newMap()`, so both `getOrDefault` calls return `0`, both are `< 9`, and the
foraging subtractor is unconditionally `2`.

The fixture does not expose it: in profile `Pineapple` the highest unlocked tiers are `FIG_LOG_7` and
`MANGROVE_LOG_7`, both under 9, so the correct answer is also `2`. For any account that has finished
either collection the computed subtractor is wrong by up to 2 levels, which propagates into
`SkillLevel.getLevel()`, into `Skills.getAverage()`, and from there into the `SKILL_AVERAGE`
expression variable at `ProfileStats.java:66`. This is a statement-order bug inside a single method
body, and it is precisely the failure mode a declarative reach-back is supposed to make impossible.

### 2.3 ProfileStats - parent plus grandparent, outside the bind path

`SkyBlockIsland.getProfileStats(member)` at `SkyBlockIsland.java:76-82` constructs
`new ProfileStats(this, member, calculateBonus)`. The seed for this survey described `ProfileStats`
as needing "banking, community upgrades" from the island. That is half right and the half that is
wrong changes the conclusion.

Grepping `skyBlockIsland` across the whole 637-line `ProfileStats.java` returns three hits: the two
constructor signatures (`:49`, `:53`) and **one** use:

```
ProfileStats.java:69   this.expressionVariables.put("BANK", skyBlockIsland.getBanking().map(Banking::getBalance).orElse(0.0));
```

`getCommunityUpgrades()` is never read from `ProfileStats`; its only consumer is
`SkyBlockIsland.getUniqueMinions()` at `SkyBlockIsland.java:89`, inside the island itself. So the
entire grandparent dependency of `ProfileStats` is **one `double`**.

Two structural facts matter more than the size, though:

- `ProfileStats` is **never deserialized**. It lives in `api/simplified/hypixel/profile_stats`, not in
  `response/`, it has no `@SerializedName` fields, and it is only ever reached through the explicit
  factory method on the island. No bind-time annotation can touch it.
- It is **parameterized and expensive**. `getProfileStats(member, calculateBonus)` exists precisely so
  the caller can skip the bonus pass (`ProfileStats.java:143-210`). Turning it into an eagerly bound
  transient field of `SkyBlockMember` would compute the expensive branch for every member of every
  profile on every decode.

`ProfileStats` therefore is not evidence for `@Owner`/`@Parent`. It is evidence that the current
`getProfileStats(member)` factory shape is the correct one, and that the reach-back it performs is a
deliberate call-time argument rather than residue.

### 2.4 CrimsonIsle to Kuudra - the downward push

`CrimsonIsle.postInit()` at `CrimsonIsle.java:52-56` is two assignment statements and nothing else:

```
this.kuudra.searchSettings = this.kuudra_search_settings;
this.kuudra.groupBuilder = this.kuudra_group_builder;
```

The cause is a JSON layout split. `Kuudra` is bound from the member key `kuudra_completed_tiers`
(`CrimsonIsle.java:36-37`), but its party-finder settings live under a *different* sibling key,
`kuudra_party_finder.search_settings` and `kuudra_party_finder.group_builder`. So `CrimsonIsle`
carries two staging fields whose only purpose is to hold values in transit
(`CrimsonIsle.java:38-43`), both suppressed with `@Getter(AccessLevel.NONE)`, both named in snake
case (`kuudra_search_settings`, `kuudra_group_builder`) because they are named after JSON rather than
after anything in the domain. The receiving fields on the far side are package-private and
`transient` (`Kuudra.java:22-23`), which only works because `CrimsonIsle` and `Kuudra` share a
package.

Cost: 2 staging fields, 2 `@Getter(AccessLevel.NONE)` suppressions, 2 `@SerializedPath` annotations,
one whole `PostInit` implementation on `CrimsonIsle`, and 2 package-private fields that break
encapsulation - to move two objects one level down the tree.

This is the mirror image of the `AccessoryBag` problem, and it is worth noting that the two want
*opposite* mechanisms. `AccessoryBag` wants to read up. `Kuudra` wants a `@SerializedPath` that can
climb: something that reads `../kuudra_party_finder/search_settings` from inside `Kuudra`. Both are
"the value I need is not under my node", but a parent *reference* solves the first and an
ancestor-relative *path* solves the second. A design that only delivers a typed parent object still
leaves `CrimsonIsle.postInit()` in place, because `Kuudra` would then have to write
`owner.getSearchSettings()` and `CrimsonIsle` would still need somewhere to bind that value.

### 2.5 JacobsContest to Contest - key-derived push

`JacobsContest.postInit()` at `JacobsContest.java:46-63` walks the `contestMap` and writes two fields
into each child value:

```
contest.collectionName = StringUtil.join(dataString, ":", 2, dataString.length);
contest.skyBlockDate = new SkyBlockDate(year, month, day);
```

Both are parsed out of the **map entry key** (`"<year>:<month>_<day>:<collection>"`, split at
`JacobsContest.java:52-56`), not out of the parent's other fields. The child is reaching back only as
far as its own key.

This is `@Collapse` + `@Key` territory rather than parent access - the existing pair already injects
an entry key into the value object during the parent field's bind (`CollapseTypeAdapterFactory`
`injectKey` at `:239-255`, used today by `Slayers`). What `@Key` cannot do is *derive* three
components out of one composite key. The gap is a key parser, not a parent reference, so this site
belongs to whichever survey owns `@Split` / `@Collapse` extensions; it is recorded here only so the
design document does not mistake it for a fourth `@Owner` customer.

### 2.6 What is not a reach-back

Ruled out after checking, so later documents do not re-derive them:

- **`SkyBlockIsland.getCollection()`, `getCollectionUnlocked()`, `getCraftedMinions()`,
  `getUniqueMinions()`** (`SkyBlockIsland.java:44-97`) walk *down* into `members` and aggregate. The
  direction is parent to child; no reference is needed that Gson does not already supply.
- **`SkyBlockMember.getChocolateFactory()`, `getFirstJoin()`, `getPersonalBankUpgrade()`,
  `getUnlockedTemples()`, `isBoosterCookieActive()`** (`SkyBlockMember.java:162-180`) forward into the
  private `Profile`, `Events` and `Temples` holders. Downward delegation - `04-accessor-boilerplate`.
- **`Bestiary.postInit()`, `Dungeons.postInit()`** compute purely from their own fields
  (`Bestiary.java:57-82`, `Dungeons.java:57-75`). No reach-back.
- **`Experimentation.Table`** has three `transient` maps (`Experimentation.java:45-49`) that look like
  post-bind state but are filled declaratively by `@Capture(filter = ...)`. Already solved.
- **`SlayerBoss.id`** (`transient`, `SlayerBoss.java`) is the `@Key` injection target, section 2.5.
- **`Election`** implements `PostInit` but derives both transients from its own `year`
  (`Election.java:44-53`). Its `postInit()` is even called directly from a constructor
  (`Election.java:24`), which is a self-contained pattern with no ancestor involvement.

## 3. Findings

### f02-accessorybag-upstream

- **Category:** `parent-access`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:142;
  `src/main/java/api/simplified/hypixel/response/skyblock/member/AccessoryBag.java`:55,135,138,182,190
- **What:** `SkyBlockMember.postInit()` calls `accessoryBag.initialize(this)`, and `AccessoryBag`
  threads that reference through two method frames to read three sibling subtrees of the member.
- **Why it is residue:** the child cannot see its declaring object, so the parent has to hand itself
  over manually. The public `initialize(SkyBlockMember)` method is a lifecycle hook masquerading as
  API - any caller can invoke it twice, or never, and nothing detects either.
- **Candidate annotation:** `@Owner`/`@Parent`
- **Effort:** `large`

Cost today: one public method that must never be called by consumers, one extra `SkyBlockMember`
parameter on a private helper, an import of `SkyBlockMember` into a leaf DTO
(`AccessoryBag.java:5`) that makes the package graph cyclic, and total dependence on statement order
inside `initialize` - see the next finding for what that costs in practice.

Payoff if a reach-back mechanism existed: the `initialize` method disappears as *public API* but its
body does not disappear - it becomes a `@Derive`-style hook or a lazy accessor. Realistically this
removes 1 public method signature, 1 threaded parameter, and the `SkyBlockMember` reference from
`Skills`/`SkillLevel` as well (next finding). It does not remove 110 lines; the family
de-duplication logic at `AccessoryBag.java:74-126` is genuinely imperative and stays.

Risk: this is the only site in the module that needs a full typed parent, and paying for a
pipeline-wide ordering guarantee to serve one site is a bad trade unless `01-postinit.md` and
`05-cross-field-derivation.md` independently want the same post-bind phase.

### f02-accessorybag-dead-initialize

- **Category:** `correctness`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/AccessoryBag.java`:34,57,129,138,139
- **What:** `initialize` reads `this.getContents()` at line 57 but only assigns `contents` from the
  member at line 138, and the magical power it computes at line 129 is never stored.
- **Why it is residue:** the reach-back is a bare sequence of statements with no declared
  dependencies, so a use-before-assign is invisible to the compiler and to the test suite.
- **Candidate annotation:** none - keep imperative, fix the ordering
- **Effort:** `trivial`

Three separate defects in one method, all consequences of the same hand-written ordering:

1. **`contents` is read before it is written.** `contents` is `transient` with the initialiser
   `new NbtContent()` (`:34`), so its `rawData` is `""`. Line 57 calls `this.getContents().getNbtData()`
   on that empty instance. `NbtContent.getNbtData()` calls `NbtFactory.fromBase64("")`, which decodes
   to a zero-length array and hits `requireRemaining(1)` in `NbtInputBuffer`
   (`Minecraft-Library/nbt-factory/.../io/buffer/NbtInputBuffer.java`:94,211), throwing
   `NbtFormatException`. `NbtFactory.fromByteArray` catches it and rethrows `NbtException`, which
   `extends RuntimeException`. Line 138, which would have supplied real data, is 81 lines too late.
2. **The throw is not local.** It escapes `initialize`, escapes `SkyBlockMember.postInit()`, and is
   swallowed by the empty `catch (Exception ex) {}` in `PostInitTypeAdapterFactory`:37-38. So for
   **every member**, `postInit()` aborts on its first statement: `skills` is left `null`
   (`SkyBlockMember.java:58`) and `collectionUnlocked` is left empty (`:130`). Every downstream
   consumer of `member.getSkills()` - `ProfileStats.java`:66,74,107,571 - dereferences null.
3. **`calculatedMagicalPower` is dead.** Lines 129-136 compute it, add the rift prism bonus, and never
   assign it to `this.magicalPower`. Lines 139-140 then derive `tuningPoints` and `logComponent` from
   the still-zero field, and `logComponent` is `Math.pow(Math.log(1), 1.2)` = 0, which zeroes every
   entry of `selectedPowerStats` at `:151`.

Not caught by tests because `MemberDtoMappingTest.java`:111 decodes `AccessoryBag` **in isolation**
from the `accessory_bag_storage` sub-object; `initialize` is never invoked from a test.

This finding is listed second only because the reach-back inventory frames it. In value order it is
first: it is a total, silent failure of `SkyBlockMember.postInit()` on every decode, and the fix is a
consumer-only statement reorder plus one missing assignment. It must land before any annotation work,
because it is also the acceptance test for whatever replaces the reach-back.

### f02-skills-member-reachback

- **Category:** `parent-access`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:143,145;
  `src/main/java/api/simplified/hypixel/response/skyblock/member/skill/Skills.java`:19,22;
  `src/main/java/api/simplified/hypixel/response/skyblock/member/skill/SkillLevel.java`:21,27,32,33
- **What:** the member reference is passed into `Skills`, forwarded into every `SkillLevel`, and used
  once per instance to read two sibling subtrees - one of which the enclosing `postInit()` has not
  computed yet.
- **Why it is residue:** neither `Skills` nor `SkillLevel` retains the member; the parameter exists
  solely to survive one constructor call. The dependency it encodes - "level subtractor needs
  `jacobs_contest` and `collectionUnlocked`" - is invisible at the declaration site.
- **Candidate annotation:** `@Owner`/`@Parent`
- **Effort:** `medium`

Cost today: a `SkyBlockMember` parameter on two constructors and one private method, an import of the
root member type into two leaf classes, and a latent wrong answer. `SkillLevel.calcLevelSubtractor`
reads `member.getCollectionUnlocked()` while that map is still the empty initialiser, because
`SkyBlockMember.postInit()` assigns it at line 145 and constructs `Skills` at line 143. Correct
foraging subtractor for an account with `FIG_LOG` and `MANGROVE_LOG` at tier 9 is `0`; computed value
is `2`. The `craftedfury.json` fixture tops out at `FIG_LOG_7` / `MANGROVE_LOG_7` so both paths agree
there and the bug stays hidden.

Note the shape: this is not a parent-population hazard, it is a *sibling-derivation* hazard. Both
`collectionUnlocked` and `skills` are transients computed in the same `postInit()` body, and one
depends on the other. Even a perfect `@Owner` that guarantees a fully *bound* parent does not fix it,
because `collectionUnlocked` is not bound - it is derived. Fixing this needs ordering **between
derived fields**, which is the `@Bind` registry entry, not `@Owner`. Swapping lines 143 and 145 is
the one-line fix available today.

Payoff of an annotation here: 2 constructor parameters and 1 method parameter removed, 2 imports
removed, and the ordering constraint stated declaratively instead of by line number.

### f02-postinit-bottom-up-order

- **Category:** `parent-access`
- **Where:** `Simplified-Dev/gson-extras/.../factory/PostInitTypeAdapterFactory.java`:32-41;
  `src/main/resources/craftedfury.json` (member key order);
  `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockMember.java`:141
- **What:** `postInit()` fires inside the child's own `read`, so it always runs **before** the parent
  finishes binding, and a child bound at JSON key *n* can only ever observe parent fields whose keys
  appeared at positions `< n`.
- **Why it is residue:** it is not residue - it is the constraint that decides whether the whole
  `@Owner`/`@Parent` idea is buildable at bind time. Recorded as a finding so the design document can
  cite it rather than rediscover it.
- **Candidate annotation:** none - this is the hazard, not the fix
- **Effort:** `large` (as the cost of any mechanism that must work around it)

The mechanics, from source. `PostInitTypeAdapterFactory.create` wraps the delegate and calls
`postInit()` the instant `delegate.read(in)` returns for *that* object. Gson's reflective adapter
constructs the enclosing instance first, then populates fields one at a time in **JSON document
order**, calling each field's adapter inline. Therefore:

- The parent *object* exists before any child binds. A parent **reference** is available at
  child-bind time - this part is fine.
- The parent's *fields* are populated after the child returns. A parent's **data** is not available.
- `postInit()` ordering is strictly bottom-up: every descendant's `postInit()` has already run by the
  time the parent's runs. `SkyBlockMember.postInit()` is the last hook to fire in the member subtree.

The empirical half, from the fixture. Member key order is not stable and is not contractual:

| Key | index in `Pineapple` | index in `Raspberry` |
| --- | --- | --- |
| `rift` | 0 | absent |
| `player_data` | 1 | 0 |
| `accessory_bag_storage` | 7 | 3 |
| `jacobs_contest` | 10 | absent |
| `nether_island_player_data` | 16 | 9 |
| `inventory` | 24 | 14 |
| `collection` | 35 | absent |

If `AccessoryBag` held an injected parent and used it during its own bind, then in `Pineapple` it
would see a populated `rift` (index 0 < 7) but an **empty** `nether_island_player_data` (16) and an
**empty** `inventory` (24) - the two reads that carry the actual accessory data. In `Raspberry` the
same three keys sit at different indices and `rift` is missing entirely. Two members of the same
account would produce different answers from identical code, decided by the order Hypixel happened to
emit keys. There is no annotation on the child that can repair that, because the ordering is a
property of the document, not of the schema.

Conclusion, stated bluntly: **a reach-back that reads parent data during the child's bind cannot be
made correct.** Any `@Owner`/`@Parent` that promises a *usable* parent must either (a) inject only
the reference at bind and defer all reads to after the parent completes, or (b) run in a new
top-down pass that starts after the root finishes. Both are new lifecycle guarantees, which is why
this rates `large` per the effort scale's "new ordering guarantee between factories" row.

### f02-kuudra-sibling-push

- **Category:** `parent-access`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/CrimsonIsle.java`:38-43,52-56;
  `src/main/java/api/simplified/hypixel/response/skyblock/member/crimson/Kuudra.java`:22-23
- **What:** `CrimsonIsle` binds two `kuudra_party_finder.*` sub-objects into suppressed staging fields
  and pushes them into `Kuudra`'s package-private transients from `postInit()`.
- **Why it is residue:** `Kuudra`'s data is split across two sibling JSON keys and the DTO can only
  name one of them, so the second arrives by assignment from above.
- **Candidate annotation:** `@Owner`/`@Parent`, but see the caveat
- **Effort:** `medium`

Cost today: 2 staging fields with snake-case Java names, 2 `@Getter(AccessLevel.NONE)`, 2
`@SerializedPath`, 2 package-private `transient` fields on `Kuudra`, and the entire reason
`CrimsonIsle` implements `PostInit` at all - remove these two lines and the interface goes with them.
Payoff is exactly that: 1 `PostInit` implementor retired, 4 fields deleted, 1 encapsulation leak
closed.

The caveat, and it is the reason this is ranked below the `AccessoryBag` pair: a typed-parent
annotation does **not** solve it. Injecting a `CrimsonIsle` reference into `Kuudra` would let
`Kuudra` write `owner.getSearchSettings()`, but `CrimsonIsle` would still need the staging fields to
hold the bound value. What removes the staging fields is an **ancestor-relative path** on the child -
`@SerializedPath` that can address a key outside its own node - which is a different capability with
a different name. `10-annotation-designs.md` should decide whether that lives as a `@SerializedPath`
extension or a new registry row; this survey deliberately does not name it, per the rule that a
survey does not invent names.

Note also that this site is the one place where the reach-back is a *pure data move* with no
computation attached. If any proposal can retire it declaratively it retires a `PostInit` implementor
outright, which makes it the cheapest scalp in `11-postinit-elimination.md`.

### f02-profilestats-island-scalar

- **Category:** `parent-access`
- **Where:** `src/main/java/api/simplified/hypixel/response/skyblock/SkyBlockIsland.java`:76-82;
  `src/main/java/api/simplified/hypixel/profile_stats/ProfileStats.java`:49,53,69
- **What:** `ProfileStats` takes a whole `SkyBlockIsland` as a constructor argument and uses it once,
  to read `banking.balance` into an expression variable.
- **Why it is residue:** it is not, and this finding says so. Recorded because the site looks like a
  grandparent reach-back and will be mistaken for one otherwise.
- **Candidate annotation:** none - keep imperative
- **Effort:** `trivial` (if narrowed at all)

`ProfileStats` is not a bound type. It has no serialized fields, it never passes through Gson, and it
is constructed on demand by a factory method that takes a `calculateBonus` flag precisely so callers
can skip the expensive branch (`ProfileStats.java:143-210`). Making it a transient field of
`SkyBlockMember` so that `@Owner` could feed it would force that computation eagerly for every member
of every profile, which is a large performance regression traded for the deletion of one constructor
parameter.

Adversarially: the only defensible tightening here is narrowing the parameter from `SkyBlockIsland`
to the `double` it actually consumes, or to `Optional<Banking>`. That is a one-file signature change
with no library involvement and no annotation, and even that is arguable - passing the island keeps
the door open for the community-upgrade and game-mode reads that `ProfileStats` will plausibly want
later. **Ranked last. Recommend no action.**

### f02-postinit-silent-swallow

- **Category:** `correctness`
- **Where:** `Simplified-Dev/gson-extras/.../factory/PostInitTypeAdapterFactory.java`:35-39;
  `Simplified-Dev/gson-extras/.../PostInit.java`:12-13
- **What:** exceptions from `postInit()` are caught by a completely empty `catch (Exception ex) {}`,
  while `PostInit`'s own javadoc states they are "logged and swallowed".
- **Why it is residue:** the hook that performs every reach-back in the module has no failure signal
  at all, which is why `f02-accessorybag-dead-initialize` has survived undetected.
- **Candidate annotation:** none - library fix
- **Effort:** `small`

The empty catch block is what converts a hard `NbtException` into "`skills` is quietly null". Any
reach-back mechanism inherits this property, and a top-down injection pass would inherit it twice
over: a null or absent parent would fail silently in exactly the same way. Whatever
`10-annotation-designs.md` proposes, the failure policy has to be decided explicitly rather than
inherited from this catch block. The javadoc/behaviour mismatch is itself a defect worth fixing on
its own, independent of this pack.

## 4. Requirements for a reach-back capability

These are requirements and hazards a design must answer, not a design. `10-annotation-designs.md`
owns the decision between `@Owner` and `@Parent` and between field injection and a threaded context.

### 4.1 What actually has to be reachable

Enumerated from section 2, with no generalisation added:

| Consumer | Needs | Kind | Depth |
| --- | --- | --- | --- |
| `AccessoryBag` | `inventory.bag_contents.talisman_bag` | whole `NbtContent` node | parent, sibling subtree |
| `AccessoryBag` | `rift.access.consumed_prism` | `boolean` | parent, sibling subtree |
| `AccessoryBag` | `nether_island_player_data.abiphone` contact count | `int` | parent, sibling subtree |
| `SkillLevel` | `jacobs_contest.perks.farming_level_cap` | `int` | parent, sibling subtree |
| `SkillLevel` | `SkyBlockMember.collectionUnlocked` | derived transient | parent, **derived not bound** |
| `Kuudra` | `kuudra_party_finder.search_settings` / `.group_builder` | 2 objects | parent, sibling subtree |
| `ProfileStats` | `banking.balance` | `double` | grandparent, but not bound - excluded |

Requirements that fall straight out of the table:

- **Depth is one.** Nothing needs a grandparent inside the bind path, and nothing whatsoever needs the
  root `SkyBlockProfiles`. A design that ships grandparent or root access is shipping capability with
  zero evidence behind it. If the design wants a chain anyway, it should say which future case it is
  for and accept that the pack found none.
- **Every read is a sibling subtree, never a parent scalar.** The parent is a routing hop, not a data
  source. That matters: it means what the child actually wants is *a path expression rooted higher in
  the document*, and a typed parent object is one - fairly clumsy - way to spell that. The design
  should compare the two directly rather than assuming field injection.
- **One read is of a derived value, not a bound one** (`collectionUnlocked`). A mechanism that
  guarantees "parent fully bound" is insufficient for this row. It needs "parent fully *derived*",
  which is a strictly stronger and separately ordered guarantee.
- **The values are small.** Two `int`s, one `boolean`, one `double`, one NBT node, two settings
  objects. Nothing here is a large graph, so a design may legitimately choose to copy values down
  instead of handing a reference up.

### 4.2 The ordering hazard, stated adversarially

The crux question was: can a reach-back work at bind time? The answer is **no, and not marginally
so**. The argument, in the order a sceptic should attack it:

1. *"The parent object exists when the child binds, so inject it then."* True and useless. Gson
   constructs the enclosing instance before reading any field, so a reference is obtainable. But the
   reference points at an object whose fields are still being filled. Injection succeeding tells you
   nothing about whether reading through it will.
2. *"Then just declare the fields in an order that works."* Java field order is irrelevant. The
   reflective adapter iterates the **JSON document**, not the class. Reordering
   `SkyBlockMember`'s declarations changes nothing.
3. *"Then require the API to emit keys in a fixed order."* The fixture already refutes this. Across
   the two profiles of one account, `accessory_bag_storage` is at index 7 and index 3;
   `nether_island_player_data` at 16 and 9; `inventory` at 24 and 14; `rift` is present in one and
   **absent** in the other. Hypixel makes no ordering guarantee and the observed order varies per
   profile within a single response.
4. *"Then buffer the parent object into a `JsonObject` first and bind children afterwards."* This is
   the only bind-time variant that could work, and the library already does exactly this shape in
   `CaptureTypeAdapterFactory` (`fromJsonTree` at `:261,366`) and `LenientTypeAdapterFactory`. But
   buffering only reorders *binding*; it does not let the child see a bound parent, because the parent
   is still a `JsonElement` at that moment. To give the child a bound sibling you would have to bind
   the sibling first, which means the buffering factory has to compute a dependency order across
   fields - a topological sort over `@Owner` reads that no annotation in this library expresses.
5. *"Then let the child read raw JSON from the parent's buffered tree."* Now the child is coupled to
   the parent's *serialized* shape rather than its Java shape, which reintroduces every string-keyed
   path the annotation set was built to remove, and it still fails for `collectionUnlocked`, which
   exists in no JSON at all.

Each step forces the mechanism further from "inject a field" and closer to "run a second pass". The
honest conclusion is that a reach-back is **not a bind-time feature**. It is a post-bind lifecycle
feature that happens to be spelled as a field annotation, and the design document should price it
that way.

A second-order hazard worth stating: partial success is worse than failure here. If injection happens
at bind and *usually* works because the keys usually arrive in a friendly order, the failures are
data-dependent, silent (`f02-postinit-silent-swallow`), and will present as "this one account has
wrong magical power". The existing `f02-accessorybag-dead-initialize` is the same class of bug and it
has evidently been shipping for some time.

### 4.3 Injection timing - the three candidate points

Three places the parent reference could be installed. The design must pick one and state why.

**A. At child bind.** Ruled out by section 4.2 for any *use*, but note it is still viable for the
*reference* alone if every use is deferred. Requires a per-thread stack of enclosing instances
maintained by a wrapper factory; the library has no `ThreadLocal` anywhere today, so this is new
machinery. Interacts badly with the buffering factories, which call `fromJsonTree` from a different
stack depth than the surrounding read.

**B. At parent bind completion, top-down, driven by the parent's adapter.** The parent finishes
`delegate.read`, then a pass walks its `@Owner`-annotated children and sets the back-reference before
returning. This has a working precedent in the library: `CollapseTypeAdapterFactory.injectKey`
(`:239-255`) already mutates a child field from the parent's field adapter, after the child is fully
read. The requirements it exposes:

- The walk must recurse into `Map` and `Collection` values (`Kuudra` is a direct field, but
  `SkyBlockIsland.members` is a `ConcurrentLinkedMap<UUID, SkyBlockMember>`), and it must not treat
  the container as the parent - see 4.4.
- It must run **before** the parent's own `postInit()`, or the parent's hook cannot rely on children
  being wired.
- It cannot help the child's own `postInit()`, which has already run. This is the direct collision
  with `PostInit` covered in 4.6.

**C. Lazily, at first access.** The reference is installed as in B, but nothing is *computed* during
deserialization at all - `AccessoryBag.getAccessories()` becomes a memoised accessor that reads
through the owner on demand. This is the only option that makes the ordering hazard structurally
impossible rather than merely avoided, because by the time any caller touches an accessor the whole
response has been decoded.

Its costs are real and must be priced: memoisation state per field, thread-safety on that state
(these DTOs use `Concurrent*` collections throughout and are plainly expected to be shared), a
defined behaviour when the owner is `null`, and the loss of the current property that a decoded
object is fully computed and immutable. It also changes when exceptions surface - from "swallowed at
decode" to "thrown at the caller", which is arguably the correct trade but is a behaviour change for
every consumer.

Whichever is chosen, one requirement is non-negotiable and is easy to overlook:
**standalone decode must keep working.** `MemberDtoMappingTest.java`:111 decodes `AccessoryBag`
directly from the `accessory_bag_storage` sub-object, with no member anywhere. Under B or C the owner
field is `null` there. The design must say whether that is an empty-default, an exception, or an
`Optional` owner, and the DTOs must be written against that answer.

### 4.4 Typing, depth, containers and the root

**Typed or untyped.** Typed. Every consumer in section 2 immediately calls a specific getter chain
(`member.getRift().getAccess()...`), so an `Object` owner would be cast at every use site and would
lose the compile-time check that is the main reason to prefer an annotation over the status quo. The
field's declared type is the natural contract, and the factory should fail loudly at adapter-creation
time if the enclosing type is not assignable to it. That failure has to be loud - see 4.6.

**The container problem, which is the one that will bite.** "Enclosing object" is ambiguous when the
child lives in a collection. `SkyBlockIsland.members` is
`ConcurrentLinkedMap<UUID, SkyBlockMember>` (`SkyBlockIsland.java:38`), so a naive walker injecting
"the object that held this field" would hand a `SkyBlockMember` a `ConcurrentLinkedMap`, not a
`SkyBlockIsland`. Same for `Dungeons.dungeonMap`, `JacobsContest.contestMap`, `Kuudra.highestWave`
and every `@Capture` map. The rule must be **nearest enclosing declared *object*, skipping
container levels**, and the design must say it explicitly because the obvious implementation gets it
wrong. `@Collapse`'s `injectKey` sidesteps this by being driven from the field-level adapter, which
knows both the container and its declaring object - that is a hint at where the walk belongs.

**Depth and root.** No evidence for either (4.1). If the design ships an ancestor selector anyway
(`@Owner(SkyBlockIsland.class)` walking up until the type matches), it should be scored as speculative
and it should not be what justifies the effort rating. A type-directed walk also has a nasty edge:
`AccessoryBag` sits under `SkyBlockMember` under `SkyBlockIsland`, so a mis-specified type silently
selects the wrong ancestor rather than failing.

**Ancestor paths versus ancestor objects.** Restating the `f02-kuudra-sibling-push` caveat because it
constrains the requirement set: three of the six bind-path reads want a *sibling subtree at a known
key*, which a path expression rooted above the current node expresses directly and a parent reference
expresses only as "here is the object, now write the getter chain yourself". The design should
evaluate whether an ancestor-relative `@SerializedPath` covers more of section 2's table than
`@Owner` does, at lower lifecycle cost, because on the evidence here it plausibly does - it needs no
post-bind pass at all if the buffering factories already hold the ancestor's `JsonObject`.

### 4.5 Cycles, equality and serialization

An owner field makes the object graph cyclic, which breaks three things that are currently fine by
accident.

**Serialization.** `Gson.toJson` on a cyclic graph recurses until `StackOverflowError`. Gson's
default `Excluder` skips `transient` and `static` fields, and `GsonSettings.defaults()` registers no
exclusion strategies (`GsonSettings.java:240-266`), so declaring the owner field `transient` is
sufficient - but "sufficient by default" is not the same as "guaranteed", and the conventions file is
explicit that round-trip fidelity is not optional. The requirement is that the factory itself refuses
to write the owner field, rather than relying on the author remembering `transient`. Note that this
module never calls `toJson` on a response DTO today, so the hazard is latent rather than active; that
makes it more likely to be discovered by a future consumer than by this pack's tests.

**Equality and hashing.** No response DTO uses `@EqualsAndHashCode` and only `Election` hand-writes
`equals`/`hashCode`/`toString` (`Election.java:27-58`), so nothing breaks today. That is luck, not
design. An owner field participating in a generated `equals` produces infinite recursion between
parent and child; the same for `toString`. The requirement is that the annotation's contract states
the field is excluded from identity and rendering, and that the house Lombok usage (`@Getter` on the
class) does not accidentally reintroduce it.

**A field-free alternative exists and should be evaluated.** The library already ships
`WeakIdentityMap` (`factory/WeakIdentityMap.java`), used three times as a per-instance side channel:
`CaptureTypeAdapterFactory.OVERFLOW` (`:82`), `LenientTypeAdapterFactory.OVERFLOW` (`:62`) and
`CollapseTypeAdapterFactory.KEY_ORDER` (`:68`). Keys are held weakly and matched by reference
identity. Storing owners there instead of in a field removes the serialization, equality and
`toString` hazards outright, at the cost of losing the typed field and needing a static lookup helper
at every use site. It is worth the design document's time to reject explicitly rather than by
omission, because it is the cheapest thing in the room that already exists.

**Lifetime.** An owner reference keeps the parent alive for as long as any child is reachable. For
`SkyBlockProfiles` that means holding one member alive retains its island and every sibling member.
Nothing in this module caches individual sub-objects today, so this is a caution rather than a
finding, but a weak reference would be the wrong fix - a silently-nulled owner is exactly the failure
mode 4.3 warns about.

### 4.6 Interaction with PostInit

This is where the two mechanisms genuinely fight, and it is not resolvable by ordering the factories
differently.

`PostInit` runs **bottom-up**: the child's hook fires inside the child's `read`, before the parent
exists in a populated state. Owner injection is necessarily **top-down**: the parent must be complete
before the child can be handed a usable reference. So for any class that both implements `PostInit`
and carries an owner, the hook runs strictly *before* the reference is usable. The two orderings are
opposites, and no registration order in `GsonSettings.defaults()` changes that, because the ordering
is imposed by Gson's recursion, not by the factory chain.

Concretely, `AccessoryBag` does not implement `PostInit` today - it is driven by
`SkyBlockMember.postInit()` calling it explicitly, which is exactly the manual workaround for this
collision. If `AccessoryBag` were given `@Owner` plus its own `postInit()`, that hook would fire with
a half-built member and would be *worse* than today's arrangement, which at least fires after the
member's fields are bound.

The requirements that follow:

- A reach-back capability implies **a second post-bind phase** that runs top-down after the root
  completes, distinct from `PostInit`'s bottom-up per-object hook. That is a new lifecycle guarantee
  the whole pipeline must honour - the effort scale's `large` row, verbatim.
- Within that phase, `f02-skills-member-reachback` shows that ordering **between derived fields** is
  also needed (`skills` depends on `collectionUnlocked`, both derived on the same object). So the new
  phase cannot be a flat pass; it needs at least a declared dependency between derivations. That is
  the `@Bind` registry entry's territory, and `@Owner` alone is insufficient for the site that most
  obviously motivates it.
- The failure policy must be chosen, not inherited. `PostInitTypeAdapterFactory`'s empty catch
  (`:37-38`) would turn "owner was never injected" into a silent null, which is how
  `f02-accessorybag-dead-initialize` stayed invisible.
- Two of the six `PostInit` implementors are involved in reach-back at all (`SkyBlockMember`,
  `CrimsonIsle`). `11-postinit-elimination.md` should not assume a reach-back mechanism retires more
  than those two, and it only retires `CrimsonIsle` cleanly - `SkyBlockMember.postInit()` still has
  the collection join to perform.

### 4.7 What a reach-back annotation would not fix

Stated so the design document does not over-claim payoff:

- **The 110 lines of `AccessoryBag.initialize`.** Removing the parameter removes the *signature*, not
  the body. Family de-duplication (`:74-126`), magical-power summation and the power-stat merge are
  imperative computation over a repository lookup; they move to a hook or a lazy accessor and stay
  roughly the same size.
- **`ProfileStats`.** Never bound, out of reach of any annotation (`f02-profilestats-island-scalar`).
- **The `JacobsContest` key parse.** Wants a composite-key splitter, not an ancestor
  (section 2.5).
- **`CrimsonIsle`'s staging fields**, unless the design also delivers ancestor-relative paths
  (`f02-kuudra-sibling-push`).
- **`collectionUnlocked` ordering**, which needs derivation ordering, not parent access
  (`f02-skills-member-reachback`).
- **The three defects.** `f02-accessorybag-dead-initialize`, the `SkillLevel` ordering bug and
  `f02-postinit-silent-swallow` are all fixable today, with no library change for the first two. They
  should not be bundled into an annotation proposal, because bundling makes the fix hostage to a
  JitPack publish cycle.

## 5. Ranked summary

Ranked by value delivered per unit of risk, not by size.

| Rank | Finding | Category | Effort | Payoff |
| --- | --- | --- | --- | --- |
| 1 | `f02-accessorybag-dead-initialize` | `correctness` | `trivial` | Restores `SkyBlockMember.postInit()` entirely - `skills` and `collectionUnlocked` are currently dead for every member. Consumer-only, 3 statements |
| 2 | `f02-postinit-silent-swallow` | `correctness` | `small` | Makes every future reach-back failure visible; one library file, additive |
| 3 | `f02-kuudra-sibling-push` | `parent-access` | `medium` | Retires 1 of 6 `PostInit` implementors, deletes 4 fields and 2 getter suppressions - but only if ancestor-relative paths ship, not if a typed parent ships |
| 4 | `f02-postinit-bottom-up-order` | `parent-access` | `large` | No payoff - it is the constraint. Cited by every other entry |
| 5 | `f02-accessorybag-upstream` | `parent-access` | `large` | 1 public method, 1 threaded parameter, 1 cyclic package import. Real but small, against a pipeline-wide lifecycle change |
| 6 | `f02-skills-member-reachback` | `parent-access` | `medium` | 3 parameters and 2 imports, plus a latent wrong answer - though the wrong answer is fixed today by swapping two lines |
| 7 | `f02-profilestats-island-scalar` | `parent-access` | `trivial` | None. Recommend no action |

The verdict this survey hands to `10-annotation-designs.md`:

**`@Owner`/`@Parent` as a bind-time field injection is not buildable correctly** (4.2), and as a
post-bind top-down phase it is a `large` lifecycle change serving **two** genuine customers
(`AccessoryBag`, `SkillLevel`) whose combined payoff is roughly one public method, three parameters
and two imports. On this survey's evidence alone it does not pay for itself.

It becomes defensible only if `01-postinit.md` and `05-cross-field-derivation.md` independently
conclude that a top-down, dependency-ordered post-bind phase is needed for *derivation* - in which
case owner injection is a small addition to a phase that is being built anyway, and should be scoped
as such rather than as a standalone annotation.

The cheaper alternative the design document should price against it is an **ancestor-relative path**
capability. It covers four of the six bind-path reads in 4.1 directly, needs no new lifecycle phase,
has no cycle, equality or serialization hazard, and is the only option that retires
`CrimsonIsle.postInit()`.

Independent of either, findings 1 and 2 should be fixed now. They are not annotation work, they are
the reason nobody noticed there was annotation work to do.
