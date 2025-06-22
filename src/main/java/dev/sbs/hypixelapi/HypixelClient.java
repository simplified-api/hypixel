package dev.sbs.minecraftapi.client.hypixel;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.client.Client;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelRequest;
import dev.sbs.api.client.response.CFCacheStatus;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.stream.pair.Pair;
import feign.FeignException;

public final class HypixelClient extends Client<HypixelRequest> {

    public HypixelClient() {
        super("api.hypixel.net");
        super.setCachedResponseHeaders(Concurrent.newUnmodifiableSet(
            CFCacheStatus.HEADER_KEY,
            "RateLimit-Limit",
            "RateLimit-Remaining",
            "RateLimit-Reset"
        ));
        super.setErrorDecoder((methodKey, response) -> {
            throw new HypixelApiException(FeignException.errorStatus(methodKey, response));
        });
        super.setDynamicRequestHeaders(Concurrent.newUnmodifiableMap(
            Pair.of("API-Key", SimplifiedApi.getKeyManager().getSupplier("HYPIXEL_API_KEY"))
        ));
    }

}
