package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.NbtContent;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.stats.ItemStack;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Accessory;
import api.simplified.skyblock.model.Power;
import api.simplified.skyblock.model.Stat;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.ConcurrentSet;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.gson.annotation.Capture;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.StringTag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * A member's accessory bag, the selected power tuned over it, and everything the two contribute.
 * <p>
 * Three of the values this needs are not in its own wire node - the talisman bag's contents, whether
 * the rift prism has been consumed, and how many abiphone contacts the member holds. They arrive
 * through {@link #initialize(NbtContent, boolean, int)}, so a bag decoded on its own is never wired
 * and reads empty rather than throwing.
 * <p>
 * {@link #getDetectedAccessories()} is the repository boundary and needs a session; the magical power
 * and power stats derived from it need one too.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Accessory_Bag">Accessory Bag</a>
 */
@Getter
public class AccessoryBag {

    /**
     * Slot upgrades bought for the bag, each widening how many accessories it holds.
     */
    @SerializedName("bag_upgrades_purchased")
    private int bagUpgradesPurchased;

    // Member-scoped values, supplied by initialize because they live outside this node
    private transient @NotNull NbtContent contents = new NbtContent();
    @Getter(AccessLevel.NONE)
    private transient boolean consumedPrism;
    @Getter(AccessLevel.NONE)
    private transient int abiphoneContacts;

    @Getter(AccessLevel.NONE)
    private transient ConcurrentList<DetectedAccessory> detectedAccessories;
    @Getter(AccessLevel.NONE)
    private transient ConcurrentList<DetectedAccessory> accessories;

    // Power

    /**
     * Id of the power the member has selected, empty when none is.
     */
    @SerializedName("selected_power")
    private @NotNull Optional<String> selectedPowerId = Optional.empty();

    /**
     * Ids of every power the member has unlocked and may select between.
     */
    @SerializedName("unlocked_powers")
    private @NotNull ConcurrentList<String> unlockedPowerIds = Concurrent.newUnmodifiableList();

    @Getter(AccessLevel.NONE)
    private transient ConcurrentMap<String, Double> selectedPowerStats;

    // Magical Power

    /**
     * The most magical power the member has ever held, which the game keeps as its own record.
     */
    @SerializedName("highest_magical_power")
    private int highestMagicalPower;
    @Getter(AccessLevel.NONE)
    private transient Integer magicalPower;

    // Tuning

    /**
     * Tuning points the member has spent across the bag's slots.
     */
    private @NotNull Tuning tuning = new Tuning();

    /**
     * Supplies the three member-scoped values this bag cannot reach from its own node.
     *
     * @param contents the talisman bag item data, stored under the member's inventory
     * @param consumedPrism whether the rift prism has been consumed
     * @param abiphoneContacts the abiphone contact count, halved by an equipped abicase
     * @return this bag
     */
    public @NotNull AccessoryBag initialize(@NotNull NbtContent contents, boolean consumedPrism, int abiphoneContacts) {
        this.contents = contents;
        this.consumedPrism = consumedPrism;
        this.abiphoneContacts = abiphoneContacts;
        return this;
    }

    /**
     * Accessories parsed out of the talisman bag and resolved against the accessory repository,
     * empty for a bag decoded on its own.
     */
    public @NotNull ConcurrentList<DetectedAccessory> getDetectedAccessories() {
        if (this.detectedAccessories == null) {
            // a bag with nothing to parse resolves nothing, so it never reaches the reference data
            this.detectedAccessories = this.contents.getRawData().isEmpty()
                ? Concurrent.newUnmodifiableList()
                : this.contents
                    .getNbtData()
                    .<CompoundTag>getListTag("i")
                    .stream()
                    .filter(CompoundTag::notEmpty)
                    .flatMap(compoundTag -> SkyBlockData.getRepository(Accessory.class)
                        .findFirst(
                            Accessory::getId,
                            compoundTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue()
                        )
                        .map(accessory -> new DetectedAccessory(
                            accessory,
                            compoundTag,
                            ItemStack.resolveRarity(accessory.getItem(), compoundTag)
                        ))
                        .stream()
                    )
                    .collect(Concurrent.toList());
        }

        return this.detectedAccessories;
    }

    /**
     * The detected accessories that count toward magical power, with the lower-ranked members of each
     * family dropped.
     */
    public @NotNull ConcurrentList<DetectedAccessory> getAccessories() {
        return this.accessories != null
            ? this.accessories
            : this.dropOutrankedAccessories(this.getDetectedAccessories());
    }

    private @NotNull ConcurrentList<DetectedAccessory> dropOutrankedAccessories(@NotNull ConcurrentList<DetectedAccessory> detectedAccessories) {
        // Store Families
        ConcurrentMap<String, ConcurrentSet<Accessory>> familyAccessoryDataMap = Concurrent.newMap();
        detectedAccessories
            .stream()
            .filter(accessoryData -> accessoryData.getAccessory().getFamily().isPresent())
            .forEach(accessoryData -> {
                // New Accessory Family
                String familyId = accessoryData.getAccessory().getFamily().get().getId();
                if (!familyAccessoryDataMap.containsKey(familyId))
                    familyAccessoryDataMap.put(familyId, Concurrent.newSet());

                // Store Accessory
                familyAccessoryDataMap.get(familyId).add(accessoryData.getAccessory());
            });

        // Store Non-Stackable Families
        ConcurrentSet<Accessory> processedAccessories = Concurrent.newSet();
        this.accessories = detectedAccessories
            .stream()
            .filter(accessoryData -> {
                if (accessoryData.getAccessory().getFamily().isPresent()) {
                    // Handle Families
                    ConcurrentList<Accessory> familyData = Concurrent.newList(familyAccessoryDataMap.get(
                        accessoryData.getAccessory().getFamily().get().getId()
                    ));

                    if (accessoryData.getAccessory().getFamily().get().getRank() >= 0) {
                        // Sort By Highest
                        Function<Accessory, Integer> byFamilyRank = accessory -> accessory.getFamily()
                            .map(Accessory.Family::getRank)
                            .orElse(0);

                        familyData = familyData.sorted(byFamilyRank).reversed();

                        // Ignore Lowest Accessories
                        Accessory topAccessory = familyData.removeFirst();
                        processedAccessories.addAll(familyData);

                        // Top Accessory Only
                        if (!accessoryData.getAccessory().equals(topAccessory))
                            return false;
                    } else {
                        if (processedAccessories.contains(accessoryData.getAccessory()))
                            return false;

                        // Ignore All Accessories
                        processedAccessories.addAll(familyData);
                        return true;
                    }
                }

                return processedAccessories.add(accessoryData.getAccessory());
            })
            .collect(Concurrent.toList());

        return this.accessories;
    }

    /**
     * Magical power granted by the counting accessories, plus the rift prism bonus.
     */
    public int getMagicalPower() {
        if (this.magicalPower == null) {
            int calculated = this.getAccessories()
                .stream()
                .mapToInt(this::handleMagicalPower)
                .sum();

            // Rift Prism
            if (this.consumedPrism)
                calculated += 11;

            this.magicalPower = calculated;
        }

        return this.magicalPower;
    }

    /**
     * The diminishing curve magical power is fed through before it scales a power's stats.
     */
    public double getLogComponent() {
        return Math.pow(Math.log(1 + (0.0019 * this.getMagicalPower())), 1.2);
    }

    /**
     * Stats granted by the selected power, scaled by its coefficient and this bag's magical power.
     */
    public @NotNull ConcurrentMap<String, Double> getSelectedPowerStats() {
        if (this.selectedPowerStats == null) {
            ConcurrentMap<String, Double> stats = this.getSelectedPower()
                .stream()
                .flatMap(power -> power.getBaseValues().stream())
                .map(entry -> Pair.of(
                    entry.getKey(),
                    SkyBlockData.getRepository(Stat.class)
                        .findFirstOrNull(Stat::getId, entry.getKey())
                        .getPowerCoefficient() * this.getLogComponent() * entry.getValue()
                ))
                .collect(Concurrent.toMap());

            this.getSelectedPower().ifPresent(power -> power.getBonuses()
                .forEach((statId, value) -> stats.merge(
                    statId,
                    value,
                    Double::sum
                ))
            );

            this.selectedPowerStats = stats.toUnmodifiable();
        }

        return this.selectedPowerStats;
    }

    /**
     * The selected power resolved against the power repository, empty when none is selected or the
     * repository does not know it.
     */
    public @NotNull Optional<Power> getSelectedPower() {
        return this.getSelectedPowerId().flatMap(powerId -> SkyBlockData.getRepository(Power.class)
            .findFirst(Power::getId, powerId.toUpperCase(Locale.ROOT))
        );
    }

    /**
     * The unlocked powers resolved against the power repository, dropping any id it does not know.
     */
    public @NotNull ConcurrentList<Power> getUnlockedPowers() {
        return this.getUnlockedPowerIds()
            .stream()
            .map(powerId -> SkyBlockData.getRepository(Power.class)
                .findFirst(Power::getId, powerId.toUpperCase(Locale.ROOT))
            )
            .flatMap(Optional::stream)
            .collect(Concurrent.toUnmodifiableList());
    }

    private int handleMagicalPower(@NotNull DetectedAccessory detectedAccessory) {
        int magicalPower = detectedAccessory.getRarity().getMagicPower();

        // TODO: Dynamic
        if (detectedAccessory.getAccessory().getId().equals("HEGEMONY_ARTIFACT"))
            magicalPower *= 2;

        if (detectedAccessory.getAccessory().getId().equals("ABICASE"))
            magicalPower += this.abiphoneContacts / 2;

        return magicalPower;
    }

    /**
     * One item found in a member's accessory bag, resolved far enough to be counted and ranked.
     * <p>
     * This is what magical power and the family filter need and no more - neither reads a stat, so
     * neither pays for one. Totalling what the accessory gives is a separate step, and the result of
     * it belongs to the computation that asked rather than to the bag.
     */
    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class DetectedAccessory {

        /**
         * Reference data for the accessory this instance is of.
         */
        private final @NotNull Accessory accessory;

        /**
         * The accessory's NBT tag exactly as it was decoded.
         */
        private final @NotNull CompoundTag compoundTag;

        /**
         * Rarity after every upgrade the instance carries, which may sit above the accessory's own.
         */
        private final @NotNull Rarity rarity;

    }

    /**
     * How a member has spent their tuning points, slot by slot.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Maxwell">Maxwell</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Tuning {

        /**
         * The highest tuning slot the member has unlocked.
         */
        @SerializedName("highest_unlocked_slot")
        private int highestUnlockedSlot;

        /**
         * Whether the first tuning point refund has been claimed.
         */
        @SerializedName("refund_1")
        @Accessors(fluent = true)
        private boolean hasClaimedRefund;

        /**
         * Whether the second tuning point refund has been claimed.
         */
        @SerializedName("refund_2")
        @Accessors(fluent = true)
        private boolean hasClaimedSecondRefund;

        /**
         * Each tuning slot the member has spent into, keyed by slot number.
         * <p>
         * The wire spells these as sibling {@code slot_<n>} keys rather than as one object, so every
         * key matching that prefix is folded in here and the number becomes the key.
         */
        @Capture(filter = "^slot_")
        private @NotNull ConcurrentMap<Integer, Slot> slots = Concurrent.newMap();

        /**
         * Reads one tuning slot.
         *
         * @param slot the slot number to read
         * @return that slot, empty when nothing has been spent into it
         */
        public @NotNull Optional<Slot> getSlot(int slot) {
            return Optional.ofNullable(this.getSlots().get(slot));
        }

        /**
         * One tuning slot and the stats the member has tuned into it.
         */
        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Slot {

            /**
             * When the slot was bought, empty for a slot that came free.
             */
            @SerializedName("purchase_ts")
            private @NotNull Optional<SkyBlockDate.RealTime> purchased = Optional.empty();

            /**
             * Points tuned into each stat, keyed by the stat id the wire names.
             */
            @Capture
            private @NotNull ConcurrentMap<String, Integer> stats = Concurrent.newMap();

        }

    }

}
