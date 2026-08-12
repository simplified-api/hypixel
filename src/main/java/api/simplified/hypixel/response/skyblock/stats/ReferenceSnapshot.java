package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Accessory;
import api.simplified.skyblock.model.BonusArmorSet;
import api.simplified.skyblock.model.BonusItemStat;
import api.simplified.skyblock.model.BonusPetPerkStat;
import api.simplified.skyblock.model.BonusReforgeStat;
import api.simplified.skyblock.model.Enchantment;
import api.simplified.skyblock.model.Gemstone;
import api.simplified.skyblock.model.HotPotatoStat;
import api.simplified.skyblock.model.HotmPerk;
import api.simplified.skyblock.model.Item;
import api.simplified.skyblock.model.MelodySong;
import api.simplified.skyblock.model.Pet;
import api.simplified.skyblock.model.PetItem;
import api.simplified.skyblock.model.Potion;
import api.simplified.skyblock.model.Power;
import api.simplified.skyblock.model.Reforge;
import api.simplified.skyblock.model.ShopPerk;
import api.simplified.skyblock.model.Skill;
import api.simplified.skyblock.model.Slayer;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.persistence.JpaModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * Every reference table one computation reads, resolved once and indexed by id.
 * <p>
 * A repository lookup is a full criteria select followed by a linear filter in Java, and a model
 * carrying foreign ids materialises the tables it points at before any predicate runs - so resolving
 * one reforge from a tag walks the whole item table. Reading each table once and asking a map
 * afterwards answers the same questions from the same rows.
 * <p>
 * A snapshot is taken per computation. It is a picture of the repositories at one moment and it goes
 * out of date the moment they refresh, which is why nothing holds one beyond the compute that made
 * it.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReferenceSnapshot {

    private final @NotNull ConcurrentList<Stat> stats;
    private final @NotNull ConcurrentList<Skill> skills;
    private final @NotNull ConcurrentList<Slayer> slayers;
    private final @NotNull ConcurrentList<Gemstone> gemstones;
    private final @NotNull ConcurrentList<HotPotatoStat> hotPotatoStats;
    private final @NotNull ConcurrentList<BonusArmorSet> bonusArmorSets;

    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, Stat> statsById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, Item> itemsById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, Accessory> accessoriesById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, Reforge> reforgesById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, Enchantment> enchantmentsById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, Pet> petsById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, PetItem> petItemsById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, Potion> potionsById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, Power> powersById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, ShopPerk> shopPerksById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, HotmPerk> hotmPerksById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, MelodySong> melodySongsById;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, BonusReforgeStat> bonusReforgeStatsByReforgeId;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentMap<String, ConcurrentList<BonusItemStat>> bonusItemStatsByItemId;
    @Getter(AccessLevel.NONE) private final @NotNull ConcurrentList<BonusPetPerkStat> bonusPetPerkStats;

    /**
     * The stat of a given id, empty when no row carries it.
     *
     * @param statId the id to resolve
     * @return the stat row
     */
    public @NotNull Optional<Stat> getStat(@NotNull String statId) {
        return this.statsById.getOptional(statId);
    }

    /**
     * The item of a given id, empty when no row carries it.
     *
     * @param itemId the id to resolve
     * @return the item row
     */
    public @NotNull Optional<Item> getItem(@NotNull String itemId) {
        return this.itemsById.getOptional(itemId);
    }

    /**
     * The accessory of a given id, empty when no row carries it.
     *
     * @param accessoryId the id to resolve
     * @return the accessory row
     */
    public @NotNull Optional<Accessory> getAccessory(@NotNull String accessoryId) {
        return this.accessoriesById.getOptional(accessoryId);
    }

    /**
     * The reforge of a given id, empty when no row carries it.
     *
     * @param reforgeId the id to resolve
     * @return the reforge row
     */
    public @NotNull Optional<Reforge> getReforge(@NotNull String reforgeId) {
        return this.reforgesById.getOptional(reforgeId);
    }

    /**
     * The enchantment of a given id, empty when no row carries it.
     *
     * @param enchantmentId the id to resolve
     * @return the enchantment row
     */
    public @NotNull Optional<Enchantment> getEnchantment(@NotNull String enchantmentId) {
        return this.enchantmentsById.getOptional(enchantmentId);
    }

    /**
     * The pet of a given id, empty when no row carries it.
     *
     * @param petId the id to resolve
     * @return the pet row
     */
    public @NotNull Optional<Pet> getPet(@NotNull String petId) {
        return this.petsById.getOptional(petId);
    }

    /**
     * The pet item of a given id, empty when no row carries it.
     *
     * @param petItemId the id to resolve
     * @return the pet item row
     */
    public @NotNull Optional<PetItem> getPetItem(@NotNull String petItemId) {
        return this.petItemsById.getOptional(petItemId);
    }

    /**
     * The potion of a given id, empty when no row carries it.
     *
     * @param potionId the id to resolve
     * @return the potion row
     */
    public @NotNull Optional<Potion> getPotion(@NotNull String potionId) {
        return this.potionsById.getOptional(potionId);
    }

    /**
     * The power of a given id, empty when no row carries it.
     *
     * @param powerId the id to resolve
     * @return the power row
     */
    public @NotNull Optional<Power> getPower(@NotNull String powerId) {
        return this.powersById.getOptional(powerId);
    }

    /**
     * The essence perk of a given id, empty when no row carries it.
     *
     * @param shopPerkId the id to resolve
     * @return the essence perk row
     */
    public @NotNull Optional<ShopPerk> getShopPerk(@NotNull String shopPerkId) {
        return this.shopPerksById.getOptional(shopPerkId);
    }

    /**
     * The Heart of the Mountain perk of a given id, empty when no row carries it.
     *
     * @param hotmPerkId the id to resolve
     * @return the perk row
     */
    public @NotNull Optional<HotmPerk> getHotmPerk(@NotNull String hotmPerkId) {
        return this.hotmPerksById.getOptional(hotmPerkId);
    }

    /**
     * The Melody's Harp song of a given id, empty when no row carries it.
     *
     * @param melodySongId the id to resolve
     * @return the song row
     */
    public @NotNull Optional<MelodySong> getMelodySong(@NotNull String melodySongId) {
        return this.melodySongsById.getOptional(melodySongId);
    }

    /**
     * The conditional bonus declared for a reforge, empty when it declares none.
     *
     * @param reforgeId the reforge to read
     * @return the bonus row
     */
    public @NotNull Optional<BonusReforgeStat> getBonusReforgeStat(@NotNull String reforgeId) {
        return this.bonusReforgeStatsByReforgeId.getOptional(reforgeId);
    }

    /**
     * Every conditional bonus declared for an item, empty when it declares none.
     *
     * @param itemId the item to read
     * @return the bonus rows
     */
    public @NotNull ConcurrentList<BonusItemStat> getBonusItemStats(@NotNull String itemId) {
        return this.bonusItemStatsByItemId.getOrDefault(itemId, Concurrent.newUnmodifiableList());
    }

    /**
     * The conditional bonus declared for one of a pet's perks, empty when it declares none.
     *
     * @param petId the pet to read
     * @param perkName the perk's own name, which is what the row names it by
     * @return the bonus row
     */
    public @NotNull Optional<BonusPetPerkStat> getBonusPetPerkStat(@NotNull String petId, @NotNull String perkName) {
        return this.bonusPetPerkStats.findFirst(
            Pair.of(BonusPetPerkStat::getPetId, petId),
            Pair.of(BonusPetPerkStat::getPerkName, perkName)
        );
    }

    /**
     * Every stat in the wisdom family, which the reference data names by suffix rather than by
     * declaring the family anywhere.
     *
     * @return the wisdom stats
     */
    public @NotNull ConcurrentList<Stat> getWisdomStats() {
        return this.getStats()
            .stream()
            .filter(statModel -> statModel.getId().endsWith("_WISDOM"))
            .collect(Concurrent.toUnmodifiableList());
    }

    /**
     * Every hot potato book grant that applies to an item category.
     *
     * @param categoryId the item category to read
     * @return the grants that name it
     */
    public @NotNull ConcurrentList<HotPotatoStat> getHotPotatoStats(@NotNull String categoryId) {
        return this.getHotPotatoStats()
            .stream()
            .filter(hotPotatoStat -> hotPotatoStat.getItemTypes().contains(categoryId))
            .collect(Concurrent.toUnmodifiableList());
    }

    /**
     * Reads every table this computation needs and indexes each by the id it is looked up under.
     * <p>
     * Needs a connected session, and is the only place in the computation that does.
     *
     * @return the snapshot
     */
    public static @NotNull ReferenceSnapshot load() {
        ConcurrentList<Stat> stats = findAll(Stat.class);
        ConcurrentList<BonusItemStat> bonusItemStats = findAll(BonusItemStat.class);

        return new ReferenceSnapshot(
            stats,
            findAll(Skill.class),
            findAll(Slayer.class),
            findAll(Gemstone.class),
            findAll(HotPotatoStat.class),
            findAll(BonusArmorSet.class),
            index(stats, Stat::getId),
            index(findAll(Item.class), Item::getId),
            index(findAll(Accessory.class), Accessory::getId),
            index(findAll(Reforge.class), Reforge::getId),
            index(findAll(Enchantment.class), Enchantment::getId),
            index(findAll(Pet.class), Pet::getId),
            index(findAll(PetItem.class), PetItem::getId),
            index(findAll(Potion.class), Potion::getId),
            index(findAll(Power.class), Power::getId),
            index(findAll(ShopPerk.class), ShopPerk::getId),
            index(findAll(HotmPerk.class), HotmPerk::getId),
            index(findAll(MelodySong.class), MelodySong::getId),
            index(findAll(BonusReforgeStat.class), BonusReforgeStat::getReforgeId),
            group(bonusItemStats, BonusItemStat::getItemId),
            findAll(BonusPetPerkStat.class)
        );
    }

    private static <T extends JpaModel> @NotNull ConcurrentList<T> findAll(@NotNull Class<T> model) {
        return SkyBlockData.getRepository(model).findAll();
    }

    private static <T> @NotNull ConcurrentMap<String, T> index(@NotNull ConcurrentList<T> rows, @NotNull Function<T, String> id) {
        ConcurrentMap<String, T> indexed = Concurrent.newMap();
        // first row wins, which is what a findFirst over the same list answers
        rows.forEach(row -> indexed.putIfAbsent(id.apply(row), row));
        return indexed.toUnmodifiable();
    }

    private static <T> @NotNull ConcurrentMap<String, ConcurrentList<T>> group(@NotNull ConcurrentList<T> rows, @NotNull Function<T, String> id) {
        ConcurrentMap<String, ConcurrentList<T>> grouped = Concurrent.newMap();
        rows.forEach(row -> grouped.computeIfAbsent(id.apply(row), key -> Concurrent.newList()).add(row));

        // the query this replaces collected an unmodifiable list, so each group is frozen before it is handed out
        ConcurrentMap<String, ConcurrentList<T>> frozen = Concurrent.newMap();
        grouped.forEach((key, group) -> frozen.put(key, Concurrent.newUnmodifiableList(group)));
        return frozen.toUnmodifiable();
    }

}
