package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.pet;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoPet {

    @SerializedName("rules_limit")
    private int rulesLimit;
    private @NotNull ConcurrentList<Rule> rules = Concurrent.newList();
    private boolean migrated;
    @SerializedName("migrated_2")
    private boolean migrated2;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Rule {

        @SerializedName("uuid")
        private UUID identifier;
        private String id;
        private String name;
        @SerializedName("uniqueId")
        private UUID petUniqueId;
        private boolean disabled;
        private @NotNull ConcurrentList<Exception> exceptions = Concurrent.newList();
        private @NotNull ConcurrentMap<String, String> data = Concurrent.newMap();

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Exception {

            private String id;
            private @NotNull ConcurrentMap<String, String> data = Concurrent.newMap();

        }

    }

}
