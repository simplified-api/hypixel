package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
public class Powder {

    private final @NotNull ConcurrentMap<Type, Data> oreTypes;
    
    public Powder() {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public Powder(int mithrilAmount, int mithrilTotal, int mithrilSpent, int gemstoneAmount, int gemstoneTotal, int gemstoneSpent, int glaciteAmount, int glaciteTotal, int glaciteSpent) {
        ConcurrentMap<Type, Data> oreTypes = Concurrent.newMap();
        oreTypes.put(Type.MITHRIL, new Data(mithrilAmount, mithrilTotal, mithrilSpent));
        oreTypes.put(Type.GEMSTONE, new Data(gemstoneAmount, gemstoneTotal, gemstoneSpent));
        oreTypes.put(Type.GLACITE, new Data(glaciteAmount, glaciteTotal, glaciteSpent));
        this.oreTypes = oreTypes.toUnmodifiableMap();
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Data {

        private final int amount;
        private final int total;
        private final int spent;

    }

    public enum Type {

        MITHRIL,
        GEMSTONE,
        GLACITE

    }

}
