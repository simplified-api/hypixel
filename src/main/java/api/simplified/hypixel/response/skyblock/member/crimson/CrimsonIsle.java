package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class CrimsonIsle {

    private @NotNull Abiphone abiphone = new Abiphone();
    private @NotNull Matriarch matriarch = new Matriarch();
    @SerializedName("last_minibosses_killed")
    private @NotNull ConcurrentList<String> lastMinibossesKilled = Concurrent.newList();

    // Factions
    @SerializedName("selected_faction")
    private @NotNull Faction selectedFaction = Faction.NONE;
    @SerializedName("mages_reputation")
    private int mageReputation;
    @SerializedName("barbarians_reputation")
    private int barbarianReputation;
    @SerializedName("barbarians_reputation_highest")
    private int highestBarbarianReputation;

    // Kuudra
    @SerializedName("kuudra_completed_tiers")
    private @NotNull Kuudra kuudra = new Kuudra();
    // the wire puts kuudra_party_finder beside kuudra_completed_tiers, so these are Kuudra's
    // siblings rather than its children and are named for where they live
    @SerializedPath("kuudra_party_finder.search_settings")
    private @NotNull Kuudra.SearchSettings partyFinderSearch = new Kuudra.SearchSettings();
    @SerializedPath("kuudra_party_finder.group_builder")
    private @NotNull Kuudra.GroupBuilder partyFinderGroupBuilder = new Kuudra.GroupBuilder();

    // Dojo
    @SerializedName("dojo")
    private @NotNull Dojo dojo = new Dojo();

    // Quests
    private @NotNull Quests quests = new Quests();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Quests {

        // Quest Board
        @SerializedName("quest_data")
        private @NotNull QuestBoard questBoard = new QuestBoard();
        // one JSON object carrying two unrelated maps interleaved by value type: an item id to a
        // count, and a quest id to the item it rewards. @Lenient takes the integer half and
        // filters the rest into overflow, and the filtered remainder claims it back typed
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
        private @NotNull SuusQuest suusQuest = new SuusQuest();
        @SerializedName("pablo_quest")
        private @NotNull PabloQuest pabloQuest = new PabloQuest();
        @SerializedName("duel_training_quest")
        private @NotNull DuelTrainingQuest duelTrainingQuest = new DuelTrainingQuest();
        @SerializedName("sirih_quest")
        private @NotNull SirihQuest sirihQuest = new SirihQuest();
        @SerializedName("edelis_quest")
        private @NotNull EdelisQuest edelisQuest = new EdelisQuest();
        @SerializedName("mollim_quest")
        private @NotNull MollimQuest mollimQuest = new MollimQuest();
        @SerializedName("aranya_quest")
        private @NotNull NpcQuest aranyaQuest = new NpcQuest();
        private @NotNull ConcurrentMap<String, Object> rulenor = Concurrent.newMap();

        // Misc
        @SerializedName("last_reset")
        private int lastReset;
        @SerializedName("chicken_quest_handed_in")
        private long chickenQuestHandedIn;
        @Accessors(fluent = true)
        @SerializedName("paid_bruuh")
        private boolean hasPaidBruuh;
        @SerializedName("miniboss_data")
        private @NotNull ConcurrentMap<String, Boolean> minibossData = Concurrent.newMap();

        // Kuudra Discovery
        @Accessors(fluent = true)
        @SerializedName("found_kuudra_book")
        private boolean hasFoundKuudraBook;
        @SerializedName("last_kuudra_relic")
        private long lastKuudraRelic;
        @Accessors(fluent = true)
        @SerializedName("found_kuudra_leggings")
        private boolean hasFoundKuudraLeggings;
        @Accessors(fluent = true)
        @SerializedName("kuudra_loremaster")
        private boolean isKuudraLoremaster;
        @Accessors(fluent = true)
        @SerializedName("found_kuudra_chestplate")
        private boolean hasFoundKuudraChestplate;
        @SerializedName("last_believer_blessing")
        private long lastBelieverBlessing;
        @Accessors(fluent = true)
        @SerializedName("weird_sailor")
        private boolean hasMetWeirdSailor;
        @Accessors(fluent = true)
        @SerializedName("fished_wet_napkin")
        private boolean hasFishedWetNapkin;
        @Accessors(fluent = true)
        @SerializedName("found_kuudra_helmet")
        private boolean hasFoundKuudraHelmet;
        @Accessors(fluent = true)
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
