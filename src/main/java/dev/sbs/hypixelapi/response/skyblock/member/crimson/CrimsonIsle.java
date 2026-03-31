package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
public class CrimsonIsle implements PostInit {

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

    // Kuudra
    @SerializedName("kuudra_completed_tiers")
    private @NotNull Kuudra kuudra = new Kuudra();
    @Getter(AccessLevel.NONE)
    @SerializedPath("kuudra_party_finder.search_settings")
    private Kuudra.SearchSettings kuudra_search_settings = new Kuudra.SearchSettings();
    @Getter(AccessLevel.NONE)
    @SerializedPath("kuudra_party_finder.group_builder")
    private Kuudra.GroupBuilder kuudra_group_builder = new Kuudra.GroupBuilder();

    // Dojo
    @SerializedName("dojo")
    private @NotNull Dojo dojo = new Dojo();

    @Override
    public void postInit() {
        this.kuudra.searchSettings = this.kuudra_search_settings;
        this.kuudra.groupBuilder = this.kuudra_group_builder;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Quests {

        // TODO: claude implement

    }

    public enum Faction {

        NONE,
        @SerializedName("mages")
        MAGE,
        @SerializedName("barbarians")
        BARBARIAN

    }

}
