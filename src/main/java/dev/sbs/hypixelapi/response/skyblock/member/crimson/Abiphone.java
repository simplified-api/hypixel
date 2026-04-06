package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.SerializedPath;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class Abiphone {

    @SerializedName("contact_data")
    private @NotNull ConcurrentMap<String, Contact> contacts = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Integer> games = Concurrent.newMap();
    @SerializedName("active_contacts")
    private @NotNull ConcurrentList<String> collectedContacts = Concurrent.newList();

    @SerializedPath("operator_chip.repaired_index")
    private int repairedOperatorRelays;
    @SerializedName("trio_contact_addons")
    private int trioContactAddons;
    @SerializedName("selected_ringtone")
    private String selectedRingtone;
    @SerializedName("speed_dial")
    private @NotNull Optional<String> speedDial = Optional.empty();
    @SerializedName("last_dye_called_year")
    private int lastYearVincentCalled;
    @Accessors(fluent = true)
    @SerializedName("has_used_sirius_personal_phone_number_item")
    private boolean hasSiriusContact;

    @Getter
    public static class Contact {

        @SerializedName("talked_to")
        private boolean talkedTo;
        @SerializedName("completed_quest")
        private boolean questCompleted;
        @SerializedName("dnd_enabled")
        private boolean doNotDisturb;
        private @NotNull ConcurrentMap<String, Object> specific = Concurrent.newMap();

        // Calls
        @SerializedName("incoming_calls_count")
        private int incomingCalls;
        @SerializedName("last_call")
        private @NotNull Optional<SkyBlockDate.RealTime> lastOutgoingCall = Optional.empty();
        @SerializedName("last_call_incoming")
        private @NotNull Optional<SkyBlockDate.RealTime> lastIncomingCall = Optional.empty();

    }

}
