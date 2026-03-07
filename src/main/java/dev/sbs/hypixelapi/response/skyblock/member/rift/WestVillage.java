package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
public class WestVillage {

    @SerializedName("crazy_kloon")
    private @NotNull CrazyKloon crazyKloon = new CrazyKloon();
    private @NotNull Mirrorverse mirrorverse = new Mirrorverse();
    @SerializedName("kat_house")
    private @NotNull KatHouse katHouse = new KatHouse();
    private @NotNull Glyphs glyphs = new Glyphs();

    @Getter
    public static class CrazyKloon {

        @SerializedName("selected_colors")
        private @NotNull ConcurrentMap<String, String> selectedColors = Concurrent.newMap();
        @Accessors(fluent = true)
        @SerializedName("talked")
        private boolean hasTalked;
        @SerializedName("hacked_terminals")
        private @NotNull ConcurrentList<String> hackedTerminals = Concurrent.newList();
        @SerializedName("quest_complete")
        private boolean questComplete;

    }

    @Getter
    public static class Mirrorverse {

        @SerializedName("visited_rooms")
        private @NotNull ConcurrentList<String> visitedRooms = Concurrent.newList();
        @SerializedName("upside_down_hard")
        private boolean upsideDownHardCompleted;
        @SerializedName("claimed_chest_items")
        private @NotNull ConcurrentList<String> claimedChestItems = Concurrent.newList();
        @SerializedName("claimed_reward")
        private boolean claimedReward;

    }

    @Getter
    public static class KatHouse {

        @SerializedName("bin_collected_mosquito")
        private int collectedMosquito;
        @SerializedName("bin_collected_spider")
        private int collectedSpider;
        @SerializedName("bin_collected_silverfish")
        private int collectedSilverfish;

    }

    @Getter
    public static class Glyphs {

        @SerializedName("claimed_wand")
        private boolean claimedWand;
        @SerializedName("current_glyph_delivered")
        private boolean currentGlyphDelivered;
        @SerializedName("current_glyph_completed")
        private boolean currentGlyphCompleted;
        @SerializedName("current_glyph")
        private int currentGlyph;
        private boolean completed;
        @SerializedName("claimed_bracelet")
        private boolean claimedBracelet;

    }

}
