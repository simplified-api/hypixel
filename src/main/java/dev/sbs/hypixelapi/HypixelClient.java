package dev.sbs.minecraftapi.client.hypixel;

import com.google.gson.Gson;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelEndpoint;
import dev.simplified.client.Client;
import dev.simplified.client.decoder.ClientErrorDecoder;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public final class HypixelClient extends Client<HypixelEndpoint> {

    @Override
    protected @NotNull Gson getGson() {
        return MinecraftApi.getGson();
    }

    @Override
    protected @NotNull ClientErrorDecoder configureErrorDecoder() {
        return (methodKey, response) -> {
            throw new HypixelApiException(methodKey, response);
        };
    }

    @Override
    protected @NotNull ConcurrentMap<String, Supplier<Optional<String>>> configureDynamicHeaders() {
        return Concurrent.newUnmodifiableMap(
            Pair.of("API-Key", MinecraftApi.getKeyManager().getSupplier("HYPIXEL_API_KEY"))
        );
    }

}
