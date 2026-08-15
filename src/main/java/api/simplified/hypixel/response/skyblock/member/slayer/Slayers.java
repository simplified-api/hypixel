package api.simplified.hypixel.response.skyblock.member.slayer;

import api.simplified.hypixel.common.WeightedGroup;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Collapse;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The member's slayer branches and whichever quest is open against them.
 *
 * <p>
 * A member who has never opened the slayer menu has no slayer node on the wire at all, and the
 * instance that results is a non-null default carrying no branches. That is a privacy setting or an
 * untouched feature rather than an error.
 *
 * <p>
 * The aggregates inherited from {@link WeightedGroup} run over every branch - unlike the skills
 * group, nothing is filtered out.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Slayer">Slayer</a>
 */
@Getter
public class Slayers implements WeightedGroup<SlayerBoss> {

    /**
     * The quest currently open, empty when the member has none. It is final because our own code
     * never reassigns it, not because the decoder leaves it alone.
     */
    @SerializedName("slayer_quest")
    private final @NotNull Optional<SlayerQuest> activeQuest = Optional.empty();

    /**
     * One record per slayer branch. The wire's object of objects is collapsed into this list, each
     * key landing on its own branch's id.
     */
    @Collapse
    @SerializedName("slayer_bosses")
    private @NotNull ConcurrentList<SlayerBoss> bosses = Concurrent.newList();

    /** {@inheritDoc} */
    @Override
    public @NotNull ConcurrentList<SlayerBoss> getWeighted() {
        return this.getBosses();
    }

}
