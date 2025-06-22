package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.member;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.Collection;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.Slayer;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.accessories.EnhancedAccessoryBag;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.dungeon.Dungeon;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.skill.EnhancedSkill;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.skill.Skill;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weight;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.minecraftapi.data.model.collection_data.collection_items.CollectionItemModel;
import dev.sbs.minecraftapi.data.model.collection_data.collections.CollectionModel;
import dev.sbs.minecraftapi.data.model.minion_data.minions.MinionModel;
import dev.sbs.api.mutable.MutableDouble;
import dev.sbs.api.stream.pair.Pair;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

@Getter
public class EnhancedMember extends Member {

    private final @NotNull EnhancedAccessoryBag accessoryBag;

    public EnhancedMember(@NotNull Member member) {
        super(
            member.getUniqueId(),
            member.getRift(),
            member.getStats(),
            member.getBestiary(),
            member.getAccessoryBag(),
            member.getLeveling(),
            member.getDungeonData(),
            member.getCrimsonIsle(),
            member.getExperimentation(),
            member.getMining(),
            member.getPlayerStats(),
            member.getFairySouls(),
            member.getPlayerData(),
            member.getCurrencies(),
            member.getSlayer(),
            member.getItemSettings(),
            member.getJacobsContest(),
            member.getInventory(),
            member.getPetData(),
            member.getQuests(),
            member.getFirstJoinHub(),
            member.getFirstJoin(),
            member.getPersonalBankUpgrade(),
            member.isBoosterCookieActive(),
            member.trophyFishMap,
            member.getCollection(),
            member.getTutorialObjectives(),
            member.getForge(),
            member.trophyFish,
            member.accessoryBagLoaded
        );

        this.accessoryBag = super.getAccessoryBag().asEnhanced();
    }

    public @NotNull Collection getCollection(@NotNull CollectionModel type) {
        ConcurrentLinkedMap<CollectionItemModel, Long> collectedItems = Concurrent.newLinkedMap();
        ConcurrentLinkedMap<CollectionItemModel, Integer> collectionUnlocked = Concurrent.newLinkedMap();

        // Fill Collection
        SimplifiedApi.getRepositoryOf(CollectionItemModel.class)
            .findAll(CollectionItemModel::getCollection, type)
            .forEach(collectionItemModel -> {
                collectedItems.put(collectionItemModel, this.collection.getOrDefault(collectionItemModel.getItem().getItemId(), 0L));

                this.getPlayerData()
                    .getUnlockedCollectionTiers()
                    .stream()
                    .filter(tier -> tier.matches(String.format("^%s_[\\d]+$", collectionItemModel.getItem().getItemId())))
                    .forEach(tier -> collectionUnlocked.put(
                        collectionItemModel,
                        Math.max(
                            collectionUnlocked.getOrDefault(collectionItemModel, 0),
                            Integer.parseInt(tier.replace(String.format("%s_", collectionItemModel.getItem().getItemId()), ""))
                        )
                    ));
            });

        return new Collection(type, collectedItems, collectionUnlocked);
    }

    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull MinionModel type) {
        return this.getPlayerData().getCraftedMinions(type.getKey());
    }

    // Dungeons

    public double getDungeonClassAverage() {
        ConcurrentList<Dungeon.Class> dungeonClasses = this.getDungeonData().getClasses();
        return dungeonClasses.stream()
            .map(Dungeon.Class::asEnhanced)
            .mapToDouble(Dungeon.EnhancedClass::getLevel)
            .sum() / dungeonClasses.size();
    }

    public double getDungeonClassExperience() {
        ConcurrentList<Dungeon.Class> dungeonClasses = this.getDungeonData().getClasses();
        return dungeonClasses.stream().mapToDouble(Dungeon.Class::getExperience).sum();
    }

    public double getDungeonClassProgressPercentage() {
        ConcurrentList<Dungeon.Class> dungeonClasses = this.getDungeonData().getClasses();
        return dungeonClasses.stream()
            .map(Dungeon.Class::asEnhanced)
            .mapToDouble(Dungeon.EnhancedClass::getTotalProgressPercentage).sum() / dungeonClasses.size();
    }

    public @NotNull ConcurrentMap<Dungeon.Type, Weight> getDungeonWeight() {
        return Arrays.stream(Dungeon.Type.values())
            .map(type -> Pair.of(
                type,
                this.getDungeonData()
                    .getDungeon(type)
                    .asEnhanced()
                    .getWeight()
            ))
            .collect(Concurrent.toMap());
    }

    public @NotNull ConcurrentMap<Dungeon.Class.Type, Weight> getDungeonClassWeight() {
        return Arrays.stream(Dungeon.Class.Type.values())
            .map(type -> Pair.of(
                type,
                this.getDungeonData()
                    .getClass(type)
                    .asEnhanced()
                    .getWeight()
            ))
            .collect(Concurrent.toMap());
    }

    // Skills

    public double getSkillAverage() {
        return this.getPlayerData()
            .getSkills(false)
            .stream()
            .map(skill -> skill.asEnhanced(this.getJacobsContest()))
            .mapToDouble(EnhancedSkill::getLevel)
            .average()
            .orElse(0.0);
    }

    public double getSkillExperience() {
        ConcurrentList<Skill> skills = this.getPlayerData().getSkills(false);
        return skills.stream().mapToDouble(Skill::getExperience).sum();
    }

    public double getSkillProgressPercentage() {
        ConcurrentList<Skill> skills = this.getPlayerData().getSkills(false);
        return skills.stream()
            .map(skill -> skill.asEnhanced(this.getJacobsContest()))
            .mapToDouble(EnhancedSkill::getTotalProgressPercentage)
            .sum() / skills.size();
    }

    public @NotNull ConcurrentMap<Skill.Type, Weight> getSkillWeight() {
        return this.getPlayerData()
            .getSkills(false)
            .stream()
            .map(skill -> skill.asEnhanced(this.getJacobsContest()))
            .map(skill -> Pair.of(skill.getType(), skill.getWeight()))
            .collect(Concurrent.toMap());
    }

    // Slayer

    public double getSlayerAverage() {
        ConcurrentList<Slayer.Boss> bosses = this.getSlayer().getBosses();
        return bosses.stream()
            .map(Slayer.Boss::asEnhanced)
            .mapToDouble(Slayer.EnhancedBoss::getLevel)
            .sum() / bosses.size();
    }

    public double getSlayerExperience() {
        return this.getSlayer()
            .getBosses()
            .stream()
            .map(Slayer.Boss::asEnhanced)
            .mapToDouble(Slayer.EnhancedBoss::getExperience).sum();
    }

    public double getSlayerProgressPercentage() {
        ConcurrentList<Slayer.Boss> bosses = this.getSlayer().getBosses();
        return bosses.stream()
            .map(Slayer.Boss::asEnhanced)
            .mapToDouble(Slayer.EnhancedBoss::getTotalProgressPercentage).sum() / bosses.size();
    }

    public @NotNull ConcurrentMap<Slayer.Type, Weight> getSlayerWeight() {
        return Arrays.stream(Slayer.Type.values())
            .map(type -> Pair.of(
                type,
                this.getSlayer()
                    .getBoss(type)
                    .asEnhanced()
                    .getWeight()
            ))
            .collect(Concurrent.toMap());
    }

    // Weight

    public @NotNull Weight getTotalWeight() {
        // Load Weights
        Weight skillWeight = this.getTotalWeight(EnhancedMember::getSkillWeight);
        Weight slayerWeight = this.getTotalWeight(EnhancedMember::getSlayerWeight);
        Weight dungeonWeight = this.getTotalWeight(EnhancedMember::getDungeonWeight);
        Weight dungeonClassWeight = this.getTotalWeight(EnhancedMember::getDungeonClassWeight);

        return Weight.of(
            skillWeight.getValue() + slayerWeight.getValue() + dungeonWeight.getValue() + dungeonClassWeight.getValue(),
            skillWeight.getOverflow() + slayerWeight.getOverflow() + dungeonWeight.getOverflow() + dungeonClassWeight.getOverflow()
        );
    }

    private Weight getTotalWeight(Function<EnhancedMember, ConcurrentMap<?, Weight>> weightMapFunction) {
        MutableDouble totalWeight = new MutableDouble();
        MutableDouble totalOverflow = new MutableDouble();

        weightMapFunction.apply(this)
            .stream()
            .map(Map.Entry::getValue)
            .forEach(skillWeight -> {
                totalWeight.add(skillWeight.getValue());
                totalOverflow.add(skillWeight.getOverflow());
            });

        return Weight.of(totalWeight.get(), totalOverflow.get());
    }

}
