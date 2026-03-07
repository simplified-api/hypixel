package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
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
    private transient @NotNull Kuudra kuudra = new Kuudra();
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, Integer> kuudra_completed_tiers = Concurrent.newMap();
    @Getter(AccessLevel.NONE)
    @SerializedPath("kuudra_party_finder.search_settings")
    private Kuudra.SearchSettings kuudra_search_settings = new Kuudra.SearchSettings();
    @Getter(AccessLevel.NONE)
    @SerializedPath("kuudra_party_finder.group_builder")
    private Kuudra.GroupBuilder kuudra_group_builder = new Kuudra.GroupBuilder();

    // Dojo
    private transient @NotNull Dojo dojo = new Dojo();
    @Getter(AccessLevel.NONE)
    @SerializedName("dojo")
    private @NotNull ConcurrentMap<String, Integer> dojoMap = Concurrent.newMap();

    @Override
    public void postInit() {
        this.dojo = new Dojo(this.dojoMap);
        this.kuudra = new Kuudra(
            this.kuudra_completed_tiers,
            this.kuudra_search_settings,
            this.kuudra_group_builder
        );
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Quests {

        // TODO: jesus christ

    }

    public enum Faction {

        NONE,
        @SerializedName("mages")
        MAGE,
        @SerializedName("barbarians")
        BARBARIAN

    }

}
