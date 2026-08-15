package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.SerializedPath;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's Abiphone - the item that lets a player call NPCs remotely instead of walking to them.
 * <p>
 * Contacts are added by right-clicking an NPC with the phone in hand, and how many fit depends on the
 * phone's model plus the Contacts Trio items, Abicases and repaired network relays the member has
 * collected. The phone is a Crimson Isle item, which is why it hangs off the island's node.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Abiphones">Abiphones</a>
 */
@Getter
public class Abiphone {

    /**
     * Every contact in the phone, keyed by NPC id, holding that NPC's own state. This and
     * {@code collectedContacts} are two views of one roster off two wire keys - the state map and the
     * flat id list - and neither derives from the other.
     */
    @SerializedName("contact_data")
    private @NotNull ConcurrentMap<String, Contact> contacts = Concurrent.newMap();

    /**
     * Counters for the phone's mini-games, keyed however each game spells them. The map is flat, so a
     * game's several counters are sibling keys rather than one nested object.
     */
    private @NotNull ConcurrentMap<String, Integer> games = Concurrent.newMap();

    /**
     * The contact ids the member has actually collected - the flat-list view of the roster
     * {@code contacts} holds as state.
     */
    @SerializedName("active_contacts")
    private @NotNull ConcurrentList<String> collectedContacts = Concurrent.newList();

    /**
     * How far through the phone's network relay repairs the member has got, taken from a nested wire
     * node by {@link SerializedPath}. Zero on a phone with no operator chip at all, which is
     * indistinguishable from a chip with nothing repaired.
     */
    @SerializedPath("operator_chip.repaired_index")
    private int repairedOperatorRelays;

    /**
     * Abiphone Contacts Trio items the member has consumed, each worth extra contact slots.
     */
    @SerializedName("trio_contact_addons")
    private int trioContactAddons;

    /**
     * The ringtone chosen for incoming calls, and the one reference field here with no default - it is
     * {@code null} on a phone that never set one.
     */
    @SerializedName("selected_ringtone")
    private String selectedRingtone;

    /**
     * The single contact a Flip+ model calls on left-click.
     */
    @SerializedName("speed_dial")
    private @NotNull Optional<String> speedDial = Optional.empty();

    /**
     * The SkyBlock year Vincent last called about dye; the Java name and the wire key share no words.
     */
    @SerializedName("last_dye_called_year")
    private int lastYearVincentCalled;

    /**
     * Whether the Sirius personal phone number item has been consumed - fluent, so the accessor reads
     * {@code hasSiriusContact()}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("has_used_sirius_personal_phone_number_item")
    private boolean hasSiriusContact;

    /**
     * One NPC's entry in the phone - whether the member has met them, whether their quest is done,
     * whether their calls are silenced, and the call log in both directions.
     * <p>
     * A contact object can arrive completely empty. Every field defaults, so an empty entry reads as
     * untalked-to with no calls rather than throwing.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/NPC">NPC</a>
     */
    @Getter
    public static class Contact {

        /**
         * Whether the member has spoken to this NPC in person.
         */
        @SerializedName("talked_to")
        private boolean talkedTo;

        /**
         * Whether this NPC's quest is finished.
         */
        @SerializedName("completed_quest")
        private boolean questCompleted;

        /**
         * Whether the member has silenced this NPC's incoming calls, which the later phone models
         * allow.
         */
        @SerializedName("dnd_enabled")
        private boolean doNotDisturb;

        /**
         * Whether the member handed this NPC the items they asked for - fluent, so the accessor reads
         * {@code hasGivenItems()}.
         */
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("items_given")
        private boolean hasGivenItems;

        /**
         * State only this NPC has, held untyped as the wire sent it and empty on most contacts.
         */
        private @NotNull ConcurrentMap<String, Object> specific = Concurrent.newMap();

        // Calls

        /**
         * How many times this NPC has rung the member.
         */
        @SerializedName("incoming_calls_count")
        private int incomingCalls;

        /**
         * When the member last called this NPC. The wire key is the bare {@code last_call} and it is
         * the outgoing direction, whichever way the key reads.
         */
        @SerializedName("last_call")
        private @NotNull Optional<SkyBlockDate.RealTime> lastOutgoingCall = Optional.empty();

        /**
         * When this NPC last called the member.
         */
        @SerializedName("last_call_incoming")
        private @NotNull Optional<SkyBlockDate.RealTime> lastIncomingCall = Optional.empty();

    }

}
