package dev.sbs.minecraftapi.client.hypixel;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.client.Client;
import dev.sbs.api.client.exception.ApiErrorDecoder;
import dev.sbs.api.client.response.CFCacheStatus;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.ConcurrentSet;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelRequest;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public final class HypixelClient extends Client<HypixelRequest> {

    public HypixelClient() {
        super("api.hypixel.net");
    }

    @Override
    protected @NotNull ConcurrentSet<String> configureResponseHeaders() {
        return Concurrent.newUnmodifiableSet(
            CFCacheStatus.HEADER_KEY,
            "RateLimit-Limit",
            "RateLimit-Remaining",
            "RateLimit-Reset"
        );
    }

    @Override
    protected @NotNull ApiErrorDecoder configureErrorDecoder() {
        return (methodKey, response) -> {
            throw new HypixelApiException(FeignException.errorStatus(methodKey, response));
        };
    }

    @Override
    protected @NotNull ConcurrentMap<String, Supplier<Optional<String>>> configureDynamicHeaders() {
        return Concurrent.newUnmodifiableMap(
            Pair.of("API-Key", SimplifiedApi.getKeyManager().getSupplier("HYPIXEL_API_KEY"))
        );
    }

}
