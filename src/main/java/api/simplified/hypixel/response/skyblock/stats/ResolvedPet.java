package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.Pet;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The pet a member has summoned, resolved once - its row, its rarity and its level, with everything
 * it grants already multiplied out.
 * <p>
 * A pet grants a base amount plus a scalar for each level it has, and two things need the result: the
 * source that writes the cells, and the provider that names each perk's contribution as a variable.
 * Neither can read the other, so resolving once here is what keeps one arithmetic from being written
 * twice and drifting. It is to a pet what an item stack is to an item.
 */
@Getter
final class ResolvedPet {

    /**
     * Reference data for the pet this is one of.
     */
    private final @NotNull Pet pet;

    /**
     * The rarity the member holds it at, which decides which stats and perks apply.
     */
    private final @NotNull Rarity rarity;

    /**
     * The level the member has it at.
     */
    private final int level;

    /**
     * Id of the item the pet holds, empty when it holds none.
     */
    private final @NotNull String heldItemId;

    /**
     * What the pet's own stats come to at this rarity and level.
     */
    private final @NotNull ConcurrentMap<Stat, Double> stats;

    /**
     * What each of the pet's perks contributes, one entry per stat a perk names.
     */
    private final @NotNull ConcurrentList<PerkStat> perkStats;

    private ResolvedPet(@NotNull Pet pet, @NotNull OwnedPet activePet) {
        this.pet = pet;
        this.rarity = activePet.getRarity();
        this.level = activePet.getLevel();
        this.heldItemId = activePet.getHeldItem().orElse("");
        this.stats = Concurrent.newMap();
        this.perkStats = Concurrent.newList();

        pet.getStats(this.rarity)
            .forEach(substitute -> substitute.getStat().ifPresent(stat -> {
                Pet.Substitute.Value value = substitute.getValues().get(this.rarity);

                if (value != null)
                    this.stats.merge(stat, this.scale(value), Double::sum);
            }));

        pet.getPerks(this.rarity).forEach(perk -> perk.getStats(this.rarity).forEach(substitute -> {
            Pet.Substitute.Value value = substitute.getValues().get(this.rarity);
            this.perkStats.add(new PerkStat(perk.getName(), substitute.getStat(), value == null ? 0.0 : this.scale(value)));
        }));
    }

    /**
     * The member's summoned pet, empty when none is summoned or no row carries its id.
     *
     * @param member the member to read
     * @return the resolved pet
     */
    static @NotNull Optional<ResolvedPet> of(@NotNull SkyBlockMember member) {
        Optional<OwnedPet> optionalActivePet = member.getPets().getActivePet();

        if (optionalActivePet.isEmpty())
            return Optional.empty();

        OwnedPet activePet = optionalActivePet.get();

        if (activePet.getId().isEmpty())
            return Optional.empty();

        return SkyBlockData.getRepository(Pet.class)
            .findFirst(Pet::getId, activePet.getId())
            .map(petModel -> new ResolvedPet(petModel, activePet));
    }

    private double scale(@NotNull Pet.Substitute.Value value) {
        return value.getBase() + (value.getScalar() * this.level);
    }

    /**
     * One stat a perk names, and what that perk contributes to it.
     *
     * @param perkName the perk's own name, which is what a variable is keyed by
     * @param stat the stat it contributes to, empty for a perk that names none
     * @param value the amount, already scaled to the pet's level
     */
    record PerkStat(@NotNull String perkName, @NotNull Optional<Stat> stat, double value) {

    }

}
