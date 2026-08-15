package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.SerializedPath;
import org.jetbrains.annotations.NotNull;

/**
 * A member's progress in the Village Plaza, the Rift's mirror of the Village and its social hub.
 *
 * <p>
 * The Living Cave is reached from the plaza through the Déjà Vu Alley. Four separate threads hang
 * off it - the scammer, a murder investigation, the Barry Center and the cowboy's carrot trade -
 * alongside two values lifted straight out of single-key sibling nodes rather than given holders of
 * their own. The plaza's counters are member statistics rather than location state, so none of them
 * are here.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Village_Plaza">Village Plaza</a>
 */
@Getter
public class VillagePlaza {

    /**
     * Whether the plaza's scammer got the member, bound from {@code got_scammed}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("got_scammed")
    private boolean hasBeenScammed;

    /**
     * The plaza's murder investigation; the field name already matches the wire key.
     */
    private @NotNull Murder murder = new Murder();

    /**
     * The Barry Center, bound from {@code barry_center}.
     */
    @SerializedName("barry_center")
    private @NotNull BarryCenter barryCenter = new BarryCenter();

    /**
     * The plaza's barter bank, bound from {@code barter_bank}. Its value shape is not modelled -
     * entries arrive as plain objects.
     */
    @SerializedName("barter_bank")
    private @NotNull ConcurrentMap<String, Object> barterBank = Concurrent.newMap();

    /**
     * The cowboy's carrot trade; the field name already matches the wire key.
     */
    private @NotNull Cowboy cowboy = new Cowboy();

    /**
     * Seconds spent sitting with the lonely NPC. {@link SerializedPath} lifts the value out of the
     * nested {@code lonely} node, which carries this single key and gets no class of its own.
     */
    @SerializedPath("lonely.seconds_sitting")
    private int secondsSitting;

    /**
     * How far Detransfigured Seraphine's dialogue has run. {@link SerializedPath} lifts the value
     * out of the nested {@code seraphine} node, which carries this single key and gets no class of
     * its own. This is the Rift's Seraphine, not the Hub's Community Center clerk.
     */
    @SerializedPath("seraphine.step_index")
    private int seraphineStepIndex;

    /**
     * The Village Plaza murder investigation, run in three parts.
     */
    @Getter
    public static class Murder {

        /**
         * How far part one has run, bound from {@code step_index} - part one is the only one whose
         * key carries no part suffix.
         */
        @SerializedName("step_index")
        private int stepIndex;

        /**
         * How far part two has run, bound from {@code step_index_pt2}.
         */
        @SerializedName("step_index_pt2")
        private int stepIndexPt2;

        /**
         * How far part three has run, bound from {@code step_index_pt3}.
         */
        @SerializedName("step_index_pt3")
        private int stepIndexPt3;

        /**
         * Ids of the clues found in the room, bound from {@code room_clues}. The ids follow no
         * uniform scheme - a few are numbered and most are not.
         */
        @SerializedName("room_clues")
        private @NotNull ConcurrentList<String> roomClues = Concurrent.newList();

    }

    /**
     * The Barry Center, the Village Plaza's take on the Community Center.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Barry_Center">Barry Center</a>
     */
    @Getter
    public static class BarryCenter {

        /**
         * Whether Barry has been spoken to for the first time, bound from
         * {@code first_talk_to_barry}.
         */
        @SerializedName("first_talk_to_barry")
        private boolean firstTalkToBarry;

        /**
         * Whether the centre's reward has been received, bound from {@code received_reward}.
         */
        @SerializedName("received_reward")
        private boolean receivedReward;

        /**
         * Lowercase ids of the NPCs talked round; the field name already matches the wire key.
         */
        private @NotNull ConcurrentList<String> convinced = Concurrent.newList();

    }

    /**
     * The cowboy's carrot trade in the Village Plaza.
     */
    @Getter
    public static class Cowboy {

        /**
         * How far the cowboy's chain has run; the field name already matches the wire key.
         */
        private int stage;

        /**
         * Hay eaten, bound from {@code hay_eaten}.
         */
        @SerializedName("hay_eaten")
        private int hayEaten;

        /**
         * The name the member gave the rabbit, bound from {@code rabbit_name}. This is free text
         * the player typed rather than an id, so nothing can be looked up from it.
         */
        @SerializedName("rabbit_name")
        private String rabbitName;

        /**
         * Carrots exported, bound from {@code exported_carrots}.
         */
        @SerializedName("exported_carrots")
        private int exportedCarrots;

    }

}
