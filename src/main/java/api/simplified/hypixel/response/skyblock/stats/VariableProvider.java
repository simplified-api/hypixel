package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.island.Banking;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonClass;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonData;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Skill;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Everything a bonus expression can name that is not a stat total.
 * <p>
 * A constant names a source of variables rather than a variable: five are scalar and the rest fan out
 * over data rather than over Java, so {@link #SKILL_LEVEL} covers whatever skills the corpus carries
 * and {@link #COLLECTION} whatever collections the wire sends. Enumerating either here would copy the
 * corpus into a place it can rot.
 * <p>
 * The seed runs before the flat pass and every constant writes through a sink it cannot read back, so
 * no provider can name another's variable and none can name a stat total - a value that depends on a
 * total is published at a pass boundary instead.
 */
enum VariableProvider {

    /**
     * The active pet's level, absent when none is summoned.
     */
    PET_LEVEL {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            context.getMember()
                .getPets()
                .getActivePet()
                .ifPresent(activePet -> sink.put(this.name(), activePet.getLevel()));
        }

    },

    /**
     * The member's average skill level.
     */
    SKILL_AVERAGE {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            sink.put(this.name(), context.getMember().getSkills().getAverage());
        }

    },

    /**
     * The member's SkyBlock level.
     */
    SKYBLOCK_LEVEL {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            sink.put(this.name(), context.getMember().getLeveling().getLevel());
        }

    },

    /**
     * The bestiary milestone the member has reached.
     */
    BESTIARY_MILESTONE {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            sink.put(this.name(), context.getMember().getBestiary().getMilestone());
        }

    },

    /**
     * The profile's shared bank balance, zero for a profile whose banking is switched off.
     */
    BANK {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            sink.put(this.name(), context.getIsland().getBanking().map(Banking::getBalance).orElse(0.0));
        }

    },

    /**
     * The member's level in each skill, one variable per skill the corpus carries.
     */
    SKILL_LEVEL {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            SkyBlockData.getRepository(Skill.class)
                .findAll()
                .forEach(skillModel -> sink.put(
                    String.format("%s_%s", this.name(), skillModel.getId()),
                    context.getMember().getSkills().getSkill(skillModel.getId()).getLevel()
                ));
        }

    },

    /**
     * The member's level in each dungeon.
     */
    DUNGEON_LEVEL {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            for (DungeonData.Type dungeonType : DungeonData.Type.values()) {
                if (dungeonType == DungeonData.Type.UNKNOWN)
                    continue;

                sink.put(
                    String.format("%s_%s", this.name(), dungeonType.name()),
                    context.getMember().getDungeons().getDungeon(dungeonType).getLevel()
                );
            }
        }

    },

    /**
     * The member's level in each dungeon class.
     */
    DUNGEON_CLASS_LEVEL {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            for (DungeonClass.Type classType : DungeonClass.Type.values()) {
                if (classType == DungeonClass.Type.UNKNOWN)
                    continue;

                sink.put(
                    String.format("%s_%s", this.name(), classType.name()),
                    context.getMember().getDungeons().getClass(classType).getLevel()
                );
            }
        }

    },

    /**
     * How much of each item the member has collected, one variable per collection the wire sends.
     */
    COLLECTION {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            context.getMember()
                .getCollection()
                .forEach((itemId, collected) -> sink.put(String.format("%s_%s", this.name(), itemId), collected));
        }

    },

    /**
     * What each of the active pet's perks contributes, one variable per stat a perk names.
     * <p>
     * This cannot be read back off the stat table the way a total can, because the table folds every
     * perk into the one pet origin - so it is computed here, off the same resolved pet the source
     * reads rather than off a second copy of the arithmetic.
     */
    PET_PERK {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            ResolvedPet.of(context.getMember())
                .ifPresent(resolvedPet -> resolvedPet.getPerkStats().forEach(perkStat -> sink.put(
                    String.format(
                        "%s_%s%s",
                        this.name(),
                        perkStat.perkName(),
                        perkStat.stat().map(stat -> "_" + stat.getId()).orElse("")
                    ),
                    perkStat.value()
                )));
        }

    },

    /**
     * Damage scaling earned from combat levels, as a fraction rather than a percentage.
     */
    DAMAGE_MULTIPLIER {

        @Override
        public void provide(@NotNull StatContext context, @NotNull VariableSink sink) {
            sink.put(this.name(), SkyBlockData.getRepository(Skill.class)
                .findFirst(Skill::getId, "COMBAT")
                .map(skillModel -> {
                    int skillLevel = context.getMember()
                        .getSkills()
                        .getSkill(skillModel.getId())
                        .getLevel();

                    if (skillLevel > 0) {
                        return skillModel.getLevels()
                            .stream()
                            .filter(skillLevelModel -> skillLevelModel.getLevel() <= skillLevel)
                            .map(Skill.Level::getEffects)
                            .flatMap(map -> map.entrySet().stream())
                            .mapToDouble(Map.Entry::getValue)
                            .sum();
                    }

                    return 0.0;
                })
                .orElse(0.0) / 100.0);
        }

    };

    /**
     * Publishes whatever this constant contributes.
     *
     * @param context the member and the island to read from
     * @param sink where the variables are written
     */
    public abstract void provide(@NotNull StatContext context, @NotNull VariableSink sink);

}
