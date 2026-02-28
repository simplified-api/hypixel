package dev.sbs.minecraftapi.client.hypixel;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.client.Client;
import dev.sbs.api.client.decoder.ClientErrorDecoder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelEndpoints;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public final class HypixelClient extends Client<HypixelEndpoints> {

    @Override
    protected @NotNull ClientErrorDecoder configureErrorDecoder() {
        return (methodKey, response) -> {
            throw new HypixelApiException(methodKey, response);
        };
    }

    @Override
    protected @NotNull ConcurrentMap<String, Supplier<Optional<String>>> configureDynamicHeaders() {
        return Concurrent.newUnmodifiableMap(
            Pair.of("API-Key", SimplifiedApi.getKeyManager().getSupplier("HYPIXEL_API_KEY"))
        );
    }

}
