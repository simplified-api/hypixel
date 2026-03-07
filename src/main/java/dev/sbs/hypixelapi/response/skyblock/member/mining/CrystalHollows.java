package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.io.gson.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
public class CrystalHollows {

    @SerializedName("dwarven")
    private @NotNull MinesOfDivan minesOfDivan = new MinesOfDivan();
    @SerializedName("precursor")
    private @NotNull LostPrecursorCity lostPrecursorCity = new LostPrecursorCity();
    @SerializedPath("goblin")
    private @NotNull GoblinHoldout goblinHoldout = new GoblinHoldout();
    @SerializedPath("jungle")
    private @NotNull JungleTemple jungleTemple = new JungleTemple();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MinesOfDivan {

        @SerializedName("statues_placed")
        private @NotNull ConcurrentList<Object> placedStatues = Concurrent.newList();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LostPrecursorCity {

        @SerializedName("parts_delivered")
        private @NotNull ConcurrentList<Object> deliveredParts = Concurrent.newList();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GoblinHoldout {

        @SerializedName("king_quest_active")
        private boolean kingQuestActive;
        @SerializedName("king_quests_completed")
        private int completedKingQuests;

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JungleTemple {

        @SerializedName("jungle_temple_open")
        private boolean open;
        @SerializedName("jungle_temple_chest_uses")
        private int chestUses;

    }

}
