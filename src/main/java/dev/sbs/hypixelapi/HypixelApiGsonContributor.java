package dev.sbs.hypixelapi;

import dev.sbs.hypixelapi.common.NbtContent;
import dev.simplified.gson.GsonContributor;
import dev.simplified.gson.GsonSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the Hypixel-specific type adapters with {@link GsonSettings#defaults()}.
 * <p>
 * Discovered via the {@link java.util.ServiceLoader} entry at
 * {@code META-INF/services/dev.simplified.gson.GsonContributor} whenever this
 * module is on the classpath; consumers of {@code GsonSettings.defaults()} get
 * the adapters automatically without touching their own bootstrap code.
 */
public final class HypixelApiGsonContributor implements GsonContributor {

    @Override
    public void contribute(GsonSettings.@NotNull Builder builder) {
        builder.withTypeAdapter(NbtContent.class, new NbtContent.Adapter());
    }

}
