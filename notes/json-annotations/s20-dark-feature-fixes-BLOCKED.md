# s20-dark-feature-fixes - two of the nine are blocked upstream

Seven of the nine stage-1 fixes landed. Two did not, and they are the two the pack ranked highest:
`AccessoryBag`'s read-before-assign and `Bestiary`'s matcher. Both are **written and correct** - they
are preserved verbatim in `s20-dark-feature-fixes-BLOCKED.patch` - and both are held out of the tree
because landing them turns a swallowed failure into an escaping `Error`.

## What blocks them

`SkyBlockData` cannot be class-initialized anywhere in this workspace.

```
java.lang.ExceptionInInitializerError
Caused by: java.lang.IllegalStateException: Cycle detected at: class dev.sbs.skyblockdata.model.Zone
    at dev.simplified.collection.sort.Graph.linearTopologicalSort(Graph.java:217)
    at dev.simplified.persistence.RepositoryFactory.resolveModels(RepositoryFactory.java:128)
    at dev.sbs.skyblockdata.SkyBlockFactory.<init>(SkyBlockFactory.java:19)
    at dev.sbs.skyblockdata.SkyBlockData.<clinit>(SkyBlockData.java:41)
```

`SkyBlockFactory` resolves its model set in a field initializer, so the failure is in
`SkyBlockData`'s static initializer and is unconditional - it does not need a session, and connecting
one cannot avoid it.

The cause is an ordinary bidirectional JPA association:

| Side | Declaration |
| --- | --- |
| `Region.java`:41-42 | `@OneToMany(mappedBy = "region") ConcurrentList<Zone> zones` |
| `Zone.java`:39-41 | `@ManyToOne Region region` |

`RepositoryFactory.resolveModels` builds a graph edge for every declared field whose type - or whose
type argument - is a `JpaModel`, so it follows the inverse side as well as the owning side and sees a
two-node cycle. `Graph.linearTopologicalSort` throws on the first back edge by design; its own javadoc
points at `layeredTopologicalSort` for graphs that need full-cycle reporting.

**This is not caused by anything in `hypixel`, and it is reproducible without any of this work.**
`toolsmith verify skyblock test` is red at HEAD with the identical trace -
`JpaModelTest::initializationError`, `Cycle detected at: class dev.sbs.skyblockdata.model.Region`.
Both modules pin the same `collections` (`652c22d`) and `persistence` (`cacdb62`) shas, so it is not
pin skew between them.

## Why that makes the two fixes unsafe to land alone

`PostInitTypeAdapterFactory` catches `Exception`. `ExceptionInInitializerError` and the
`NoClassDefFoundError` that follows it are `Error`s, so neither is caught.

| | Today | With the two fixes |
| --- | --- | --- |
| `Bestiary.postInit()` | `IllegalStateException` at the matcher, **caught**, `families` empty | reaches `SkyBlockData`, `ExceptionInInitializerError` **escapes the decode** |
| `AccessoryBag.initialize()` | `NbtException` on the empty default bag, **caught** | reaches `SkyBlockData`, same escape |

Those two throws are the only reason the module has never seen this. Removing them does not turn the
dark features on - the repository they need cannot exist - it converts every `Bestiary` and every
whole-`SkyBlockMember` decode from "silently empty" into "throws". Confirmed by running them: the
pre-existing `mapsBestiary` test fails with `ExceptionInInitializerError` the moment the matcher fix
is applied.

## What has to happen first

One of these, in preference order:

1. **Stop following inverse-side associations in `RepositoryFactory.resolveModels`.** Registration
   order is a property of the owning side; an `@OneToMany(mappedBy = ...)` field is a mirror and
   contributes no ordering constraint. Skipping fields that carry `mappedBy` removes this class of
   cycle at the source. `persistence` change, one JitPack cycle, re-pin `skyblock` and `hypixel`.
2. **Use a cycle-tolerant sort for model registration.** `layeredTopologicalSort` already exists.
   Larger blast radius - it changes registration order for every consumer, not just SkyBlock.
3. **Break the association in the model.** Drop `Region.zones` and let callers query by `regionId`.
   Cheapest to land, but it removes a legitimate mapping to work around a sort bug.

Whichever lands, `toolsmith verify skyblock test` going green is the gate. After that, apply the
patch, re-add the two held tests below, and the stage is complete.

## Applying the held work

```
git apply notes/json-annotations/s20-dark-feature-fixes-BLOCKED.patch
```

The patch carries, in `Bestiary`: the widened `MOB_PATTERN` (admits the two `flameboy101` mob ids the
old `[a-z_]+` id class rejected), one matcher carried through the stream so the groups are read off a
matcher that has matched, and a switch from `distinct()` over pairs to `distinct()` over keys.

**That last change is not in the pack and is worth keeping.** `kills` and `deaths` share 296 keys in
the fixture, 293 of them with different values, so `PairStream.distinct()` - which compares key *and*
value - emitted two `Mob` entries for each. `Family.getLevel()` sums `Mob::getKills` across its mobs,
so every one of those 293 would have been counted twice the moment `families` became non-empty. The
fix turns a feature on; without this it would have turned on wrong.

In `AccessoryBag`: the `contents` assignment moved above the NBT parse that reads it, and
`calculatedMagicalPower` assigned into `this.magicalPower` before `tuningPoints` and `logComponent`
derive from it.

The two held tests:

```java
    /**
     * Pins where the bestiary hook stops in a session-less test.
     * <p>
     * The mob parse used to throw {@link IllegalStateException} on the very first key, because the
     * matcher that ran {@code matches()} and the matcher the groups were read from were two different
     * objects. That throw landed in {@code PostInitTypeAdapterFactory}'s empty catch, so
     * {@code families} was empty for every profile ever decoded. The parse now runs to completion and
     * the hook reaches the family repository - which this test deliberately does not stand up, so the
     * failure that survives names the session rather than a matcher.
     */
    @Test
    @DisplayName("bestiary parses every mob key before it reaches the family repository")
    void bestiaryParsesEveryMobKey() {
        Bestiary bestiary = decodePristine("bestiary", Bestiary.class);

        assertThrows(JpaException.class, bestiary::postInit);
        assertThat(bestiary.getFamilies(), is(empty()));
    }

    @Test
    @DisplayName("the accessory bag loads the member's talisman bag before it parses it")
    void accessoryBagLoadsItsContentsFirst() {
        SkyBlockMember member = gson.fromJson(pristine.deepCopy(), SkyBlockMember.class);
        AccessoryBag bag = member.getAccessoryBag();
        String talismanBag = member.getInventory().getBags().getAccessories().getRawData();

        assertThat(talismanBag.isEmpty(), is(false));

        // initialize() read `contents` eighty lines before assigning it, so it always parsed the empty
        // default and threw NbtException on the first statement of every member's postInit. It now
        // reaches the accessory repository, which this test deliberately does not stand up
        assertThrows(JpaException.class, () -> bag.initialize(member));
        assertThat(bag.getContents().getRawData(), is(equalTo(talismanBag)));
    }
```

Both expect `JpaException` ("There are no active sessions") - the honest boundary for a test that does
not stand up a session. They need `dev.simplified.persistence.exception.JpaException` and a static
import of `org.junit.jupiter.api.Assertions.assertThrows`. Once the cycle is fixed **and** a session
is connected, `bestiaryParsesEveryMobKey` should be rewritten to assert the pack's real acceptance
value: `bestiary.getFamilies()` is non-empty.

## Consequences for the rest of the plan

- `AccessoryBag.getMagicalPower()` stays zero, so the dead-store fix remains unverifiable. The plan
  already said this stage could not verify it.
- `s20-derivation-retirements` assumes a whole `SkyBlockMember` becomes decodable once `postInit`
  goes away. It does not, while this stands - `SkillLevel.getSkill()`, `OwnedPet.getPet()` and every
  other repository lookup listed in `05-cross-field-derivation.md` §6 hit the same static initializer,
  and lazily they throw at the caller rather than into a catch.
- `ProfileStats` cannot work at all until this is fixed, which is worth knowing before anyone
  re-opens `loadMiningCore`.
