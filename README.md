# Hypixel API

Feign contracts and a fully typed response tree for the [Hypixel Public API v2](https://api.hypixel.net/), plus the XenForo RSS feeds served by `hypixel.net`. Binds the whole SkyBlock member document - skills, slayers, dungeons, pets, collections, the Rift, the Crimson Isle, the Garden and the rest - into ~150 DTOs, and derives levels, weights and profile stats on top of them.

> [!IMPORTANT]
> Most endpoints need a Hypixel API key, supplied as the `API-Key` header. Keys are issued per Hypixel account through the [developer dashboard](https://developer.hypixel.net/) and are rate-limited per key. Everything under `/resources/` and the Bazaar and Auction listings are public and need no key.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Usage](#usage)
- [Endpoints](#endpoints)
- [Response Model](#response-model)
  - [Binding annotations](#binding-annotations)
  - [Derivation on access](#derivation-on-access)
- [Derived Values](#derived-values)
  - [Experience and levels](#experience-and-levels)
  - [Weight](#weight)
  - [Profile stats](#profile-stats)
- [Forum Feeds](#forum-feeds)
- [Error Handling](#error-handling)
- [Gradle Tasks](#gradle-tasks)
  - [Build and Test](#build-and-test)
- [Package Structure](#package-structure)
- [Developer Scripts](#developer-scripts)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Two contracts** - `HypixelContract` for the JSON API v2 (`api.hypixel.net/v2`) and `HypixelForumContract` for the RSS feeds (`hypixel.net`)
- **The whole member document, typed** - every SkyBlock member subtree binds to a class rather than a `Map<String, Object>`, including the shapes the wire spells inconsistently
- **Declarative rebinding** - `@SerializedPath`, `@Capture`, `@Extract`, `@Collapse`, `@Split` and `@Lenient` from [simplified-dev/gson-extras](https://github.com/simplified-dev/gson-extras) reshape awkward wire layouts without hand-written adapters, and round-trip back to the original shape on write
- **Zero-cost decode** - binding runs no derivation and touches no database; levels, weights and detected accessories are computed the first time something asks and memoised after
- **NBT inline** - base64 `data` blobs decode to `CompoundTag` through [minecraft-library/nbt-factory](https://github.com/minecraft-library/nbt-factory) via a `TypeAdapter` registered automatically by SPI
- **Shared progression contracts** - `Experience`, `Weight`, `Weighted` and `WeightedGroup` give skills, slayers, dungeons and dungeon classes one level formula and one weight formula between them
- **Profile stats** - `ProfileStats` folds armor, accessories, pets, potions, essence and dungeon bonuses into a single stat sheet backed by the SkyBlock data models

## Getting Started

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| [JDK](https://adoptium.net/) | **21+** | Required |
| [Gradle](https://gradle.org/) | 8.x | Wrapper is bundled (`./gradlew`) |
| [Git](https://git-scm.com/) | 2.x+ | For cloning the repository |
| Hypixel API key | - | Required for player, guild, profile and museum endpoints |
| [Python](https://www.python.org/) | 3.x | Optional, for `scripts/json_dto_diff.py` |

### Installation

Add the JitPack repository and the dependency to your `build.gradle.kts`:

```kotlin
repositories {
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.simplified-api:hypixel:master-SNAPSHOT")
}
```

The contracts need a client to run them; both come in transitively as `api` dependencies, so declaring `hypixel` is normally enough:

| Transitive | Provides |
|------------|----------|
| `simplified-dev/client` | `Client`, `ClientConfig`, rate limiting, conditional requests |
| `simplified-dev/gson-extras` | `GsonSettings` and the binding annotations |
| `simplified-dev/collections` | `ConcurrentList` / `ConcurrentMap` and their Gson bindings |
| `simplified-api/skyblock` | `SkyBlockDate`, `Profile`, `GameMode` and the SkyBlock data models |
| `minecraft-library/nbt-factory` | `CompoundTag` decoding for inventory blobs |
| `minecraft-library/text` | `ChatFormat` for the `§`-coded display names Hypixel sends |

Or clone and build locally:

```bash
git clone https://github.com/simplified-api/hypixel.git
cd hypixel
./gradlew build
```

> [!TIP]
> Building from the `Simplified-Api` parent substitutes the `skyblock` sibling for local sources through `includeBuild`, so a data-model change is visible here without a JitPack round trip.

### Usage

```java
// The API key is a dynamic header, evaluated per request rather than baked into the proxy.
Client<HypixelContract> client = Client.create(
    ClientConfig.builder(HypixelContract.class, GsonSettings.defaults())
        .withDynamicHeader("API-Key", () -> Optional.ofNullable(System.getenv("HYPIXEL_API_KEY")))
        .withErrorDecoder(HypixelApiException::new)
        .build()
);

HypixelContract hypixel = client.getContract();

// Profiles come back as islands; pick the one the player has selected.
SkyBlockProfiles profiles = hypixel.getProfiles(playerId);
SkyBlockIsland island = profiles.getSelected();
SkyBlockMember member = island.getMembers().get(playerId);

// Nothing above ran a derivation. These do, on first call, and memoise after.
System.out.println(member.getSkills().getAverage());
System.out.println(member.getDungeons().getDungeon(DungeonData.Type.CATACOMBS).getLevel());
System.out.println(member.getTotalWeight().getTotal());

// Rate-limit state is tracked from the response headers.
System.out.printf("%d requests left%n", client.getRemainingRequests());
```

> [!NOTE]
> `GsonSettings.defaults()` is what makes the tree bind. It discovers this module's `HypixelApiGsonContributor` and the collections module's type-adapter factory through `ServiceLoader`, so a bare `new Gson()` produces a mostly-empty object rather than an error.

## Endpoints

`HypixelContract` is routed at `api.hypixel.net/v2`.

| Method | Endpoint | Key |
|--------|----------|:---:|
| `getCounts()` | `/counts` | ✅ |
| `getPlayer(uuid)` | `/player` | ✅ |
| `getStatus(uuid)` | `/status` | ✅ |
| `getPunishmentStats()` | `/punishmentstats` | ✅ |
| `getGuildById` / `getGuildByName` / `getGuildByPlayer` | `/guild` | ✅ |
| `getProfiles(uuid)` | `/skyblock/profiles` | ✅ |
| `getMuseum(profile)` | `/skyblock/museum` | ✅ |
| `getGarden(profile)` | `/skyblock/garden` | ✅ |
| `getAuctionById` / `getAuctionByIsland` / `getAuctionByPlayer` | `/skyblock/auction` | ✅ |
| `getNews()` | `/skyblock/news` | ✅ |
| `getAuctions()` / `getAuctions(page)` | `/skyblock/auctions` | ❌ |
| `getEndedAuctions()` | `/skyblock/auctions_ended` | ❌ |
| `getBazaar()` | `/skyblock/bazaar` | ❌ |
| `getFireSales()` | `/skyblock/firesales` | ❌ |
| `getGames()` | `/resources/games` | ❌ |
| `getSkills()` / `getCollections()` / `getItems()` / `getElection()` | `/resources/skyblock/*` | ❌ |

> [!TIP]
> `getProfiles` and `getMuseum` return only what each member's in-game API settings expose. A missing subtree is a privacy setting, not an error - the DTOs default to empty rather than null so the absence never reaches a caller as an NPE.

## Response Model

### Binding annotations

The wire shape and the shape a caller wants are frequently not the same, and the gap is closed by annotation rather than by a hand-written adapter. Every one of these round-trips: serializing the DTO reproduces the original wire layout.

| Annotation | Closes the gap when | Example |
|------------|---------------------|---------|
| `@SerializedPath` | a value is buried under keys that carry no other meaning | `profile.first_join` binds straight onto `SkyBlockMember.firstJoin` |
| `@Capture` | sibling keys are data, not schema | 792 objective ids become one map instead of 792 fields |
| `@Extract` | one named entry must leave a map that otherwise binds wholesale | `objectives.tutorial` is a list; every sibling is an objective |
| `@Collapse` + `@Key` | an object's keys are ids its values do not carry | `slayer_bosses` becomes a list of `SlayerBoss`, each holding its own id |
| `@Flatten` | a wrapper object carries one field worth keeping | `essence.WITHER.current` flattens to `Map<String, Integer>` |
| `@Split` | one string encodes two values | Kuudra's `combat_level` is `"5-60"`, bound as a `Range` |
| `@Lenient` | some entries cannot bind to the declared type | a `String` in a numeric map goes to overflow instead of failing the decode |

### Derivation on access

Decoding a member runs no derivation, opens no database session, and reads no repository. Everything derived - skill levels, weights, the accessory bag, bestiary families, unlocked collection tiers - is computed on the first call to its accessor and memoised.

```java
SkyBlockMember member = gson.fromJson(json, SkyBlockMember.class);   // no repository touched
member.getCollectionUnlocked();                                       // computed here, cached after
member.getAccessoryBag().getDetectedAccessories();                    // needs a SkyBlockData session
```

> [!IMPORTANT]
> The accessors that resolve ids against the SkyBlock data models - `getDetectedAccessories()`, `Bestiary.getFamilies()`, everything on `ProfileStats` - need a live `SkyBlockData` session and throw `JpaException` without one. Decoding never does, which is what lets a caller parse and inspect a profile with no database at all.

## Derived Values

### Experience and levels

`Experience` is the shared progression contract - one implementation supplies its experience total and its tier table, and gets levels, remaining experience and progress percentages from the interface.

```java
SkillLevel combat = member.getSkills().getSkill("COMBAT");

combat.getLevel();                // capped at maxLevel - levelSubtractor
combat.getRawLevel();             // uncapped tier index
combat.getProgressExperience();   // experience earned inside the current level
combat.getMissingExperience();    // experience left until the next level
combat.getProgressPercentage();   // 0..100 within the current level
```

`WeightedGroup` folds a set of those into four aggregates - mean level, summed experience, mean progress, and the per-member weight map - so skills, slayers, dungeons and dungeon classes report them identically.

### Weight

`Weight` is a `(value, overflow)` pair: the score up to the level cap, plus the score earned past it. `getTotal()` adds them.

```java
Weight total = member.getTotalWeight();   // skills + slayers + dungeons + dungeon classes
total.getValue();
total.getOverflow();
total.getTotal();
```

### Profile stats

`ProfileStats` computes a full stat sheet for one member of one island, layering base stats with armor, accessories, pets, active potions, essence, bestiary milestones, century cakes, the booster cookie and dungeon bonuses. Each layer is a `ProfileStats.Type`, so a caller can read the final number or ask what contributed to it.

```java
ProfileStats stats = new ProfileStats(island, member);
```

Bonus stats - reforges, armor-set effects, pet abilities - are evaluated through [simplified-dev/expression](https://github.com/simplified-dev/expression) against variables the constructor seeds (`SKILL_AVERAGE`, `SKYBLOCK_LEVEL`, `PET_LEVEL`, `DUNGEON_LEVEL_*`, `COLLECTION_*`, and one per skill). Pass `calculateBonusStats = false` to stop after the base layers.

## Forum Feeds

`HypixelForumContract` is routed at `hypixel.net` and reads the XenForo RSS 2.0 feeds. Sections under `/forums/` and the top-level sections that are not use different request lines, and named helpers cover the four feeds that get read most.

```java
HypixelForum patchNotes = forum.getSkyBlockPatchNotes();      // skyblock-patch-notes.158
HypixelForum alpha      = forum.getSkyBlockAlpha();           // skyblock-alpha, no /forums/ prefix
HypixelForum news       = forum.getNewsAndAnnouncements();    // news-and-announcements.4
HypixelForum other      = forum.getForumFeed("some-section.42");
```

Responses are XML, not JSON, so this contract needs a client configured with the framework's `XmlDecoder`. The decoder's tree transformer folds the `<link>` / `<atom:link>` namespace collision before binding, which is why `HypixelForum` exposes a scalar `link`.

## Error Handling

Any status of 400 or higher surfaces as `HypixelApiException`, carrying the full response and the decoded `HypixelErrorResponse`.

```java
try {
    hypixel.getProfiles(playerId);
} catch (HypixelApiException e) {
    e.getStatus().getCode();               // 403, 429, ...
    e.getResponse().getReason();           // Hypixel's own `cause` string
    e.getResponse().isThrottle();          // rate limited
    e.getResponse().isGlobal();            // network-wide throttle rather than key-specific
}
```

Hypixel reports quota in `RateLimit-Limit`, `RateLimit-Remaining` and `RateLimit-Reset`, which the framework's rate-limit manager reads off every response. Ask the client before spending a request:

```java
if (!client.isRateLimited())
    hypixel.getProfiles(playerId);
```

## Gradle Tasks

### Build and Test

```bash
./gradlew build       # compile, test, assemble jar
./gradlew test        # JUnit 5 suite
```

Both fixtures ship with the repository, so a fresh clone runs the whole suite. `MemberDtoMappingTest` reads a captured `GET /skyblock/profiles` response from `src/test/resources/craftedfury.json` and takes two members out of it, the first of `profiles[0]` as the sparse case and the first of `profiles[1]` as the populated one; `ElectionMappingTest` reads `src/test/resources/elections.json`.

## Package Structure

```
hypixel/
├── src/
│   ├── main/java/api/simplified/hypixel/
│   │   ├── HypixelContract.java             # api.hypixel.net/v2
│   │   ├── HypixelForumContract.java        # hypixel.net RSS
│   │   ├── HypixelApiGsonContributor.java   # SPI hook registering the NBT adapter
│   │   ├── common/                          # Experience, Weight, Weighted, WeightedGroup,
│   │   │                                    #   NbtContent, IdTiers, EnumLookup
│   │   ├── exception/                       # HypixelApiException, HypixelErrorResponse
│   │   ├── profile_stats/                   # ProfileStats + its Data/ItemData/StatData helpers
│   │   └── response/
│   │       ├── forum/                       # HypixelForum (RSS)
│   │       ├── hypixel/                     # player, guild, counts, status, punishments
│   │       ├── resource/                    # /resources/* definition documents
│   │       └── skyblock/                    # profiles, islands, auctions, bazaar, fire sales
│   │           ├── election/                # mayors, candidates, voting cycles
│   │           ├── garden/                  # commissions, composter
│   │           ├── island/                  # banking, community upgrades
│   │           └── member/                  # the member document, one package per subtree:
│   │                                        #   attribute/ crimson/ dungeon/ foraging/
│   │                                        #   hoppity/ mining/ pet/ rift/ skill/ slayer/
│   ├── main/resources/META-INF/services/    # GsonContributor SPI registration
│   ├── test/java/                           # member DTO mapping + round-trip suite
│   └── test/resources/                      # craftedfury.json (profiles), elections.json
├── scripts/json_dto_diff.py                 # wire-vs-DTO coverage audit
├── build.gradle.kts  settings.gradle.kts  gradle/libs.versions.toml
└── LICENSE.md  CONTRIBUTING.md  CLAUDE.md
```

## Developer Scripts

`scripts/json_dto_diff.py` walks a captured API response and the DTO class graph in parallel and reports every wire key no field maps to. It understands the binding annotations, so a key reached through `@SerializedPath` or swept up by `@Capture` counts as covered.

```bash
python scripts/json_dto_diff.py                       # audit every member in the fixture
python scripts/json_dto_diff.py --section dungeons    # one member subtree
python scripts/json_dto_diff.py --root SkyBlockIsland --node profile
python scripts/json_dto_diff.py --show-mapped         # also list the covered keys
```

It exits non-zero when unmapped keys are found, so it works as a gate. Run it after every Hypixel update - a new subtree is silent otherwise, because Gson drops keys nothing declares without a word.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, code style guidelines, and how to submit a pull request.

## License

This project is licensed under the **Apache License 2.0** - see [LICENSE](LICENSE.md) for the full text.

Hypixel and SkyBlock are properties of Hypixel Inc. This library is an independent client for their public API and is not affiliated with, endorsed by, or sponsored by Hypixel Inc. or Mojang AB. Use of the API is subject to the [Hypixel Public API terms](https://api.hypixel.net/).
