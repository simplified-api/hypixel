package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.member.CenturyCake;
import api.simplified.hypixel.response.skyblock.member.SkillTree;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonData;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.hypixel.response.skyblock.member.pet.Pets;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.BonusPetPerkStat;
import api.simplified.skyblock.model.HotmPerk;
import api.simplified.skyblock.model.MelodySong;
import api.simplified.skyblock.model.Pet;
import api.simplified.skyblock.model.PetItem;
import api.simplified.skyblock.model.Potion;
import api.simplified.skyblock.model.ShopPerk;
import api.simplified.skyblock.model.Skill;
import api.simplified.skyblock.model.Slayer;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The sixteen sources a member's stats are split between.
 * <p>
 * Each constant owns its whole contribution and writes it through a sink it cannot read back, so no
 * source can depend on another having run and the order they are declared in decides nothing. Totals
 * are published between passes instead, where every source has finished and the number is settled.
 * <p>
 * All but the accessory power are fixed for the member, since the rest depend on progression rather
 * than on gear. The selected power is the one an optimiser is free to change, so it alone has to be
 * recomputed per candidate.
 */
@Getter
@RequiredArgsConstructor
public enum StatSource implements StatOrigin {

    /**
     * Stats granted by the accessory bag's selected power, scaled by its magical power.
     */
    ACCESSORY_POWER(false) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            context.getAccessoryBag()
                .getSelectedPowerStats()
                .forEach((statId, value) -> sink.add(this, statId, StatHalf.BONUS, value));
        }

    },

    /**
     * Stats and perk stats from the pet currently summoned, scaled by its level.
     */
    ACTIVE_PET(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            Optional<OwnedPet> optionalActivePet = context.getMember().getPets().getActivePet();

            if (optionalActivePet.isEmpty())
                return;

            OwnedPet activePet = optionalActivePet.get();

            if (activePet.getId().isEmpty())
                return;

            Optional<Pet> optionalPet = SkyBlockData.getRepository(Pet.class).findFirst(Pet::getId, activePet.getId());

            if (optionalPet.isEmpty())
                return;

            Pet petModel = optionalPet.get();
            Rarity petRarity = activePet.getRarity();

            // the pet's own sub-steps read back what the earlier ones gave, so they accumulate here
            // and reach the sink once, settled
            ConcurrentMap<Stat, Double> bonuses = Concurrent.newMap();

            // Load Rarity Filtered Pet Stats
            petModel.getStats(petRarity)
                .forEach(substitute -> substitute.getStat()
                    .ifPresent(stat -> {
                        Pet.Substitute.Value value = substitute.getValues().get(petRarity);
                        if (value != null)
                            bonuses.merge(stat, value.getBase() + (value.getScalar() * activePet.getLevel()), Double::sum);
                    })
                );

            // Save Pet Stats to Expression Variables
            bonuses.forEach((statModel, value) -> context.getVariables().put(String.format("STAT_PET_%s", statModel.getId()), value));

            // Load Rarity Filtered Perk Stats
            petModel.getPerks(petRarity).forEach(perk -> {
                // Load Bonus Pet Perk Stats
                SkyBlockData.getRepository(BonusPetPerkStat.class)
                    .findFirst(
                        Pair.of(BonusPetPerkStat::getPetId, petModel.getId()),
                        Pair.of(BonusPetPerkStat::getPerkName, perk.getName())
                    )
                    .ifPresent(context.getBonusPetPerkStats()::add);

                perk.getStats(petRarity).forEach(substitute -> {
                    Pet.Substitute.Value value = substitute.getValues().get(petRarity);
                    double perkValue = (value != null)
                        ? value.getBase() + (value.getScalar() * activePet.getLevel())
                        : 0.0;

                    // Save Perk Stat
                    substitute.getStat().ifPresent(stat -> bonuses.merge(stat, perkValue, Double::sum));

                    // Store Bonus Pet Perk
                    String statKey = substitute.getStat().map(stat -> "_" + stat.getId()).orElse("");
                    context.getVariables().put(String.format("PET_PERK_%s%s", perk.getName(), statKey), perkValue);
                });
            });

            String heldItemId = activePet.getHeldItem().orElse("");

            // Handle Static Pet Item Bonuses
            if (!heldItemId.isEmpty()) {
                SkyBlockData.getRepository(PetItem.class).findFirst(PetItem::getId, heldItemId)
                    .filter(PetItem::notPercentage)
                    .ifPresent(petItem -> petItem.getStats().forEach(sub ->
                        sub.getStat().ifPresent(stat -> bonuses.merge(stat, sub.getValues().getOrDefault(1, 0.0), Double::sum))
                    ));
            }

            // Handle Static Pet Stat Bonuses
            context.getBonusPetPerkStats()
                .stream()
                .filter(BonusPetPerkStat::notPercentage)
                .filter(BonusPetPerkStat::noRequiredItem)
                .filter(BonusPetPerkStat::noRequiredMobType)
                .forEach(bonusPetPerkStat -> {
                    BuffEvaluator evaluator = BuffEvaluator.compile(bonusPetPerkStat);
                    bonuses.replaceAll((statModel, value) -> evaluator.apply(statModel, value, context.getVariables(), Operation.Pass.BONUS));
                });

            // Handle Percentage Pet Item Bonuses
            if (!heldItemId.isEmpty()) {
                SkyBlockData.getRepository(PetItem.class).findFirst(PetItem::getId, heldItemId)
                    .filter(PetItem::isPercentage)
                    .ifPresent(petItem -> petItem.getStats().forEach(sub ->
                        sub.getStat().ifPresent(stat -> bonuses.put(
                            stat,
                            bonuses.getOrDefault(stat, 0.0) * (1 + (sub.getValues().getOrDefault(1, 0.0) / 100.0))
                        ))
                    ));
            }

            bonuses.forEach((statModel, value) -> sink.add(this, statModel, StatHalf.BONUS, value));
        }

    },

    /**
     * Stats from potion effects currently running.
     */
    ACTIVE_POTIONS(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            context.getMember()
                .getPlayerData()
                .getActivePotions()
                .stream()
                .filter(potion -> !context.getMember().getPlayerData().getDisabledPotions().contains(potion.getEffect()))
                .forEach(potion -> {
                    ConcurrentMap<Stat, Double> potionStatEffects = Concurrent.newMap();

                    // Load Potion
                    SkyBlockData.getRepository(Potion.class).findFirst(Potion::getId, potion.getEffect().toUpperCase())
                        .ifPresent(potionModel -> potionModel.getStats()
                            .stream()
                            .filter(sub -> sub.getValues().containsKey(potion.getLevel()))
                            .forEach(sub -> sub.getStat().ifPresent(stat ->
                                potionStatEffects.put(stat, sub.getValues().get(potion.getLevel()) + potionStatEffects.getOrDefault(stat, 0.0))
                            ))
                        );

                    // Brew modifiers skipped for now (brew model migration pending)

                    // Save Active Potions
                    potionStatEffects.forEach((statModel, value) -> sink.add(this, statModel, StatHalf.BONUS, value));
                });
        }

    },

    /**
     * The flat starting values every player has before anything is earned.
     */
    BASE_STATS(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            SkyBlockData.getRepository(Stat.class).findAll()
                .forEach(statModel -> sink.add(this, statModel, StatHalf.BASE, statModel.getBase()));
        }

    },

    /**
     * Stats from bestiary milestones.
     */
    BESTIARY(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            sink.add(this, "HEALTH", StatHalf.BASE, context.getMember().getBestiary().getMilestone() * 2.0);
        }

    },

    /**
     * Stats from an active booster cookie.
     */
    BOOSTER_COOKIE(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            if (!context.getMember().isBoosterCookieActive())
                return;

            sink.add(this, "MAGIC_FIND", StatHalf.BASE, 15);

            // the reference data names the wisdom family by suffix rather than declaring it anywhere
            SkyBlockData.getRepository(Stat.class)
                .stream()
                .filter(statModel -> statModel.getId().endsWith("_WISDOM"))
                .forEach(wisdomStatModel -> sink.add(this, wisdomStatModel, StatHalf.BASE, 25));
        }

    },

    /**
     * Stats from century cakes still in date.
     */
    CENTURY_CAKES(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            context.getMember()
                .getPlayerData()
                .getCenturyCakes()
                .stream()
                .filter(CenturyCake::isActive)
                .forEach(centuryCake -> sink.add(this, centuryCake.getStatId(), StatHalf.BONUS, centuryCake.getAmount()));
        }

    },

    /**
     * Stats from Catacombs levels and the selected dungeon class.
     */
    DUNGEONS(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            for (DungeonData.Type dungeonType : DungeonData.Type.values()) {
                if (dungeonType == DungeonData.Type.UNKNOWN) continue;

                int dungeonLevel = context.getMember()
                    .getDungeons()
                    .getDungeon(dungeonType)
                    .getLevel();

                if (dungeonLevel > 0)
                    sink.add(this, "HEALTH", StatHalf.BASE, dungeonLevel * 2.0);
            }
        }

    },

    /**
     * Stats from essence perks bought in the corresponding menus.
     */
    ESSENCE(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            context.getMember()
                .getPlayerData()
                .getShopPerks()
                .forEach(entry -> SkyBlockData.getRepository(ShopPerk.class).findFirst(ShopPerk::getId, entry.getKey().toUpperCase())
                    .ifPresent(shopPerk -> shopPerk.getStats()
                        .forEach(sub -> sub.getStat()
                            .ifPresent(stat -> sink.add(this, stat, StatHalf.BONUS, sub.getValues().getOrDefault(entry.getValue(), 0.0)))
                        )
                    )
                );
        }

    },

    /**
     * Stats from SkyBlock levels.
     */
    SKYBLOCK_LEVELS(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            double level = context.getMember().getLeveling().getLevel();
            sink.add(this, "HEALTH", StatHalf.BASE, level * 5.0);
            sink.add(this, "STRENGTH", StatHalf.BASE, level / 5.0);
        }

    },

    /**
     * Stats from Jacob's farming perks, the permanent farming fortune upgrades.
     */
    JACOBS_FARMING(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            sink.add(this, "FARMING_FORTUNE", StatHalf.BASE, context.getMember().getJacobsContest().getDoubleDrops() * 4.0);
        }

    },

    /**
     * Stats from the songs completed on Melody's Harp.
     */
    MELODYS_HARP(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            context.getMember()
                .getForaging()
                .getMelodyHarp()
                .getSongs()
                .forEach((songName, songData) -> SkyBlockData.getRepository(MelodySong.class).findFirst(MelodySong::getId, songName.toUpperCase())
                    .ifPresent(melodySongModel -> sink.add(this, "INTELLIGENCE", StatHalf.BONUS, melodySongModel.getIntelligenceReward()))
                );
        }

    },

    /**
     * Stats from Heart of the Mountain perks.
     */
    MINING_CORE(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            context.getMember()
                .getSkillTree()
                .getNodes(SkillTree.Tree.MINING)
                .map(SkillTree.Skill::getEntries)
                .ifPresent(entries -> entries.stream()
                    .filter(entry -> entry.getValue().isEnabled()) // a perk switched off grants nothing
                    .forEach(entry -> SkyBlockData.getRepository(HotmPerk.class).findFirst(HotmPerk::getId, entry.getKey().toUpperCase())
                        .ifPresent(hotmPerk -> hotmPerk.getStats()
                            .forEach(sub -> sub.getStat()
                                .ifPresent(stat -> sink.add(this, stat, StatHalf.BONUS, sub.getValues().getOrDefault(entry.getValue().getLevel(), 0.0)))
                            )
                        )
                    )
                );
        }

    },

    /**
     * Stats from pet score, which counts the distinct pets collected by rarity.
     */
    PET_SCORE(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            int petScore = context.getMember().getPets().getPetScore();

            sink.add(this, "MAGIC_FIND", StatHalf.BASE, Pets.PET_SCORE
                .stream()
                .filter(breakpoint -> petScore >= breakpoint)
                .count());
        }

    },

    /**
     * Stats from skill levels.
     */
    SKILLS(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            SkyBlockData.getRepository(Skill.class).findAll()
                .forEach(skillModel -> {
                    int skillLevel = context.getMember()
                        .getSkills()
                        .getSkill(skillModel.getId())
                        .getLevel();

                    if (skillLevel > 0) {
                        skillModel.getLevels()
                            .stream()
                            .filter(skillLevelModel -> skillLevelModel.getLevel() <= skillLevel)
                            .map(Skill.Level::getEffects)
                            .flatMap(ConcurrentMap::stream)
                            .forEach(entry -> sink.add(this, entry.getKey(), StatHalf.BASE, entry.getValue()));
                    }
                });
        }

    },

    /**
     * Stats from slayer levels.
     */
    SLAYERS(true) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            context.getMember()
                .getSlayers()
                .getBosses()
                .forEach(slayerBoss -> SkyBlockData.getRepository(Slayer.class).findFirst(Slayer::getId, slayerBoss.getId())
                    .ifPresent(slayerModel -> {
                        int slayerLevel = slayerBoss.getLevel();

                        if (slayerLevel > 0) {
                            slayerModel.getLevels()
                                .stream()
                                .filter(slayerLevelModel -> slayerLevelModel.getLevel() <= slayerLevel)
                                .map(Slayer.Level::getEffects)
                                .flatMap(ConcurrentMap::stream)
                                .forEach(entry -> sink.add(this, entry.getKey(), StatHalf.BASE, entry.getValue()));
                        }
                    })
                );
        }

    };

    /**
     * Whether this source is fixed for the member, so an optimiser need not recompute it.
     */
    private final boolean optimizerConstant;

    /**
     * Writes everything this source gives into the table.
     *
     * @param context the member, the island and the reference data to read from
     * @param sink where to write
     */
    public abstract void contribute(@NotNull StatContext context, @NotNull StatSink sink);

}
