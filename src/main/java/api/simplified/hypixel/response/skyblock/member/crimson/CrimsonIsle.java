package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's whole state on the Crimson Isle - the faction they swore to, the reputation they hold, the
 * Kuudra clears and Dojo scores they have, their Abiphone and their faction quests.
 * <p>
 * The wire node is {@code nether_island_player_data}, the name the island shipped under before it was
 * reworked; prose calls it the Crimson Isle throughout. Every field defaults non-null, because a
 * missing subtree is a player's API privacy setting rather than an error.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Crimson_Isle">Crimson Isle</a>
 */
@Getter
public class CrimsonIsle {

    /**
     * The member's Abiphone, its contacts and its call log.
     */
    private @NotNull Abiphone abiphone = new Abiphone();

    /**
     * The member's Heavy Pearl collection from The Matriarch.
     */
    private @NotNull Matriarch matriarch = new Matriarch();

    /**
     * The minibosses the member most recently killed, one id per entry. It is a history rather than a
     * set, so the same id can appear more than once.
     */
    @SerializedName("last_minibosses_killed")
    private @NotNull ConcurrentList<String> lastMinibossesKilled = Concurrent.newList();

    // Factions

    /**
     * The faction the member currently serves, which can be swapped at any time.
     */
    @SerializedName("selected_faction")
    private @NotNull Faction selectedFaction = Faction.NONE;

    /**
     * The member's standing with the Mages. Reputation is per faction and its thresholds are what gate
     * the Kuudra tiers; the field name is singular where the wire key is plural.
     */
    @SerializedName("mages_reputation")
    private int mageReputation;

    /**
     * The member's standing with the Barbarians.
     */
    @SerializedName("barbarians_reputation")
    private int barbarianReputation;

    /**
     * The best Barbarian standing the member has ever reached. There is no Mage counterpart bound here.
     */
    @SerializedName("barbarians_reputation_highest")
    private int highestBarbarianReputation;

    // Kuudra

    /**
     * The member's whole Kuudra record. The wire key names only the completions, but the highest waves
     * are siblings inside the same node and bind here too.
     */
    @SerializedName("kuudra_completed_tiers")
    private @NotNull Kuudra kuudra = new Kuudra();

    /**
     * The filters the member last set while browsing Kuudra party finder groups, from a nested wire
     * node. It shares its {@link SerializedPath} prefix with the group builder, which makes the pair a
     * find-or-create - the first field written builds the object and the second has to reuse it, or the
     * last write wins and one of the two vanishes.
     */
    @SerializedPath("kuudra_party_finder.search_settings")
    private @NotNull Kuudra.SearchSettings partyFinderSearch = new Kuudra.SearchSettings();

    /**
     * The Kuudra group the member last advertised, from the same nested wire node as the search
     * filters.
     */
    @SerializedPath("kuudra_party_finder.group_builder")
    private @NotNull Kuudra.GroupBuilder partyFinderGroupBuilder = new Kuudra.GroupBuilder();

    // Dojo

    /**
     * The member's per-test Dojo points and times.
     */
    @SerializedName("dojo")
    private @NotNull Dojo dojo = new Dojo();

    // Quests

    /**
     * The faction quest board and every one-off NPC quest.
     */
    private @NotNull Quests quests = new Quests();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Quests {

        // Quest Board
        @SerializedName("quest_data")
        private @NotNull QuestBoard questBoard = new QuestBoard();
        @Lenient
        @SerializedName("quest_rewards")
        private @NotNull ConcurrentMap<String, Integer> questRewards = Concurrent.newMap();
        @Extract(value = "questRewards", filter = "^crimson_isle_")
        private @NotNull ConcurrentMap<String, String> questItems = Concurrent.newMap();
        @SerializedName("miniboss_daily")
        private @NotNull ConcurrentMap<String, Object> minibossDaily = Concurrent.newMap();
        @SerializedName("kuuda_boss_daily")
        private @NotNull ConcurrentMap<String, Object> kuudraBossDaily = Concurrent.newMap();

        // Individual Quests
        @SerializedName("alchemist_quest")
        private @NotNull AlchemistQuest alchemistQuest = new AlchemistQuest();
        @SerializedName("chicken_quest")
        private @NotNull ChickenQuest chickenQuest = new ChickenQuest();
        @SerializedName("pomtair_quest")
        private @NotNull NpcQuest pomtairQuest = new NpcQuest();
        @SerializedName("suus_quest")
        private @NotNull NpcQuest suusQuest = new NpcQuest();
        @SerializedName("pablo_quest")
        private @NotNull PabloQuest pabloQuest = new PabloQuest();
        @SerializedName("duel_training_quest")
        private @NotNull DuelTrainingQuest duelTrainingQuest = new DuelTrainingQuest();
        @SerializedName("sirih_quest")
        private @NotNull SirihQuest sirihQuest = new SirihQuest();
        @SerializedName("edelis_quest")
        private @NotNull EdelisQuest edelisQuest = new EdelisQuest();
        @SerializedName("mollim_quest")
        private @NotNull NpcQuest mollimQuest = new NpcQuest();
        @SerializedName("aranya_quest")
        private @NotNull NpcQuest aranyaQuest = new NpcQuest();
        private @NotNull ConcurrentMap<String, Object> rulenor = Concurrent.newMap();

        // Misc
        @SerializedName("last_reset")
        private int lastReset;
        @SerializedName("chicken_quest_handed_in")
        private @NotNull Optional<SkyBlockDate.RealTime> chickenQuestHandedIn = Optional.empty();
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("paid_bruuh")
        private boolean hasPaidBruuh;
        @SerializedName("miniboss_data")
        private @NotNull ConcurrentMap<String, Boolean> minibossData = Concurrent.newMap();

        // Kuudra Discovery
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("found_kuudra_book")
        private boolean hasFoundKuudraBook;
        @SerializedName("last_kuudra_relic")
        private @NotNull Optional<SkyBlockDate.RealTime> lastKuudraRelic = Optional.empty();
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("found_kuudra_leggings")
        private boolean hasFoundKuudraLeggings;
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("kuudra_loremaster")
        private boolean isKuudraLoremaster;
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("found_kuudra_chestplate")
        private boolean hasFoundKuudraChestplate;
        @SerializedName("last_believer_blessing")
        private @NotNull Optional<SkyBlockDate.RealTime> lastBelieverBlessing = Optional.empty();
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("weird_sailor")
        private boolean hasMetWeirdSailor;
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("fished_wet_napkin")
        private boolean hasFishedWetNapkin;
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("found_kuudra_helmet")
        private boolean hasFoundKuudraHelmet;
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("found_kuudra_boots")
        private boolean hasFoundKuudraBoots;

        // Cavity
        @SerializedName("unlocked_cavity_npcs")
        private @NotNull ConcurrentList<String> unlockedCavityNpcs = Concurrent.newList();
        @SerializedName("cavity_rarity")
        private @NotNull Optional<String> cavityRarity = Optional.empty();

    }

    public enum Faction {

        NONE,
        @SerializedName("mages")
        MAGE,
        @SerializedName("barbarians")
        BARBARIAN

    }

}
