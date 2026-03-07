package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;
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
