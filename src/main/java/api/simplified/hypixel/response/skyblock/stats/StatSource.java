package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.hypixel.response.skyblock.member.CenturyCake;
import api.simplified.hypixel.response.skyblock.member.SkillTree;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonData;
import api.simplified.hypixel.response.skyblock.member.pet.Pets;
import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.HotmPerk;
import api.simplified.skyblock.model.Item;
import api.simplified.skyblock.model.MelodySong;
import api.simplified.skyblock.model.Potion;
import api.simplified.skyblock.model.ShopPerk;
import api.simplified.skyblock.model.Skill;
import api.simplified.skyblock.model.Slayer;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.StringTag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The eighteen sources a member's stats are split between.
 * <p>
 * Each constant owns its whole contribution and writes it through a sink it cannot read back, so no
 * source can depend on another having run and the order they are declared in decides nothing. Totals
 * are published between passes instead, where every source has finished and the number is settled.
 * <p>
 * Two of the eighteen are containers: they write no cell of their own, opening a slot per item
 * instance and filling it. All but those two and the accessory power are fixed for the member, since
 * the rest depend on progression rather than on gear - so the gear and the selected power are what an
 * optimiser has to recompute per candidate.
 */
@Getter
@RequiredArgsConstructor
public enum StatSource implements StatOrigin {

    /**
     * The accessories that count toward magical power, each filling a slot of its own.
     * <p>
     * Nothing lands under this constant's own name. A container opens a slot per instance and the
     * cells go there, keyed by the item buckets, which is what keeps one accessory's contribution
     * separable from the next one's.
     */
    ACCESSORIES(false) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            ConcurrentList<AccessoryBag.DetectedAccessory> detectedAccessories = context.getMember()
                .getAccessoryBag()
                .getAccessories();

            for (int index = 0; index < detectedAccessories.size(); index++) {
                AccessoryBag.DetectedAccessory detectedAccessory = detectedAccessories.get(index);

                ItemStack stack = new ItemStack(
                    detectedAccessory.getAccessory().getItem(),
                    Optional.of(detectedAccessory.getAccessory()),
                    detectedAccessory.getCompoundTag()
                );

                ItemSlot slot = new ItemSlot(ItemSlot.Kind.ACCESSORY, index);
                stack.fill(slot.kind(), sink.open(slot, stack));
            }
        }

    },

    /**
     * Stats granted by the accessory bag's selected power, scaled by its magical power.
     */
    ACCESSORY_POWER(false) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            context.getMember().getAccessoryBag()
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
            Optional<ResolvedPet> optionalPet = ResolvedPet.of(context.getMember());

            if (optionalPet.isEmpty())
                return;

            ResolvedPet resolvedPet = optionalPet.get();

            // the pet's own sub-steps read back what the earlier ones gave, so they accumulate here
            // and reach the sink once, settled
            ConcurrentMap<Stat, Double> bonuses = Concurrent.newMap();

            // Load Rarity Filtered Pet Stats
            resolvedPet.getStats().forEach((stat, value) -> bonuses.merge(stat, value, Double::sum));

            // Load Rarity Filtered Perk Stats
            resolvedPet.getPerkStats()
                .forEach(perkStat -> perkStat.stat().ifPresent(stat -> bonuses.merge(stat, perkStat.value(), Double::sum)));

            String heldItemId = resolvedPet.getHeldItemId();

            // Handle Pet Item Bonuses
            // one lookup where there were two, because what split them was a flag saying whether the
            // amount was a percentage - and that is the operation, which a row now carries
            if (!heldItemId.isEmpty()) {
                BuffEvaluator.Context petContext = WorldFacts.of(context).fill(BuffEvaluator.Context.of(Concurrent.newMap()))
                    .carrier(Buff.Term.Carrier.ID, heldItemId)
                    .carrier(Buff.Term.Carrier.HOLDER, resolvedPet.getPet().getId())
                    .carrier(Buff.Term.Carrier.RARITY, resolvedPet.getRarity().name())
                    .carrier(Buff.Term.Carrier.LEVEL, resolvedPet.getLevel());

                BuffEvaluator.select(Buff.Subject.Kind.PET_ITEM, heldItemId, null)
                    .stream()
                    .map(BuffEvaluator::compile)
                    .forEach(evaluator -> bonuses.replaceAll((statModel, value) -> evaluator.apply(
                        statModel,
                        Buff.Channel.VALUE,
                        Buff.Rule.Stage.BONUS,
                        value,
                        petContext
                    )));
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
     * The worn armour pieces, each filling a slot of its own.
     * <p>
     * Nothing lands under this constant's own name, and an unworn piece leaves its slot unopened
     * rather than shifting the pieces after it.
     */
    ARMOR(false) {

        @Override
        public void contribute(@NotNull StatContext context, @NotNull StatSink sink) {
            ConcurrentList<CompoundTag> armorTags = context.getMember()
                .getInventory()
                .getArmor()
                .getNbtData()
                .<CompoundTag>getListTag("i")
                .stream()
                .collect(Concurrent.toList())
                .reversed();

            for (int index = 0; index < armorTags.size(); index++) {
                CompoundTag itemTag = armorTags.get(index);

                if (itemTag.isEmpty())
                    continue;

                Optional<Item> itemModel = SkyBlockData.getRepository(Item.class)
                    .findFirst(Item::getId, itemTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue());

                if (itemModel.isEmpty())
                    continue;

                ItemStack stack = new ItemStack(itemModel.get(), Optional.empty(), itemTag);
                ItemSlot slot = new ItemSlot(ItemSlot.Kind.ARMOR, index);
                stack.fill(slot.kind(), sink.open(slot, stack));
            }
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
                        .ifPresent(hotmPerk -> this.contributePerk(hotmPerk, entry.getValue().getLevel(), WorldFacts.of(context), sink))
                    )
                );
        }

        /**
         * Writes what one unlocked perk grants, read off the rows attached to it.
         *
         * <p>
         * A perk resolves for its identity and carries no payload of its own, so the level is a
         * carrier the rows read rather than a key into a map the perk holds. Every stat is offered to
         * each row starting from nothing, because a row that introduces a stat the member has no cell
         * for is the whole point of attaching one to a perk - a rule whose target does not match hands
         * back the zero it was given, and only what a row actually wrote reaches the sink.
         *
         * @param hotmPerk the perk the tree named, resolved for its identity
         * @param level the level the tree has it at
         * @param world what the compute can know about the world it runs in
         * @param sink the write face of the member's table
         */
        private void contributePerk(@NotNull HotmPerk hotmPerk, int level, @NotNull WorldFacts world, @NotNull StatSink sink) {
            ConcurrentList<Buff> rows = BuffEvaluator.select(Buff.Subject.Kind.PERK, hotmPerk.getId(), SkillTree.Tree.MINING.name());

            if (rows.isEmpty())
                return;

            BuffEvaluator.Context perkContext = world.fill(BuffEvaluator.Context.of(Concurrent.newMap()))
                .carrier(Buff.Term.Carrier.ID, hotmPerk.getId())
                .carrier(Buff.Term.Carrier.LEVEL, level);

            ConcurrentList<Stat> statModels = SkyBlockData.getRepository(Stat.class).findAll();

            rows.stream()
                .map(BuffEvaluator::compile)
                .forEach(evaluator -> statModels.forEach(statModel -> {
                    double value = evaluator.apply(statModel, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, 0.0, perkContext);

                    if (value != 0.0)
                        sink.add(this, statModel, StatHalf.BONUS, value);
                }));
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
