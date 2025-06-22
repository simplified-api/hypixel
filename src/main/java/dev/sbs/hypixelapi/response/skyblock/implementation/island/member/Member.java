package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.reflection.Reflection;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.*;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.accessories.AccessoryBag;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.bestiary.Bestiary;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.crimson_isle.CrimsonIsle;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.crimson_isle.TrophyFish;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.dungeon.DungeonData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.mining.ForgeItem;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.mining.Mining;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.pet.PetData;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @SerializedName("player_id")
    protected @NotNull UUID uniqueId;
    protected Rift rift = new Rift();
    protected Stats stats = new Stats();
    protected Bestiary bestiary = new Bestiary();
    @SerializedName("accessory_bag_storage")
    protected AccessoryBag accessoryBag = new AccessoryBag();
    protected Leveling leveling = new Leveling();
    @SerializedName("dungeons")
    protected DungeonData dungeonData = new DungeonData();
    @SerializedName("nether_island_player_data")
    protected CrimsonIsle crimsonIsle = new CrimsonIsle();
    protected Experimentation experimentation = new Experimentation();
    protected Mining mining = new Mining();
    @SerializedName("player_stats")
    protected PlayerStats playerStats = new PlayerStats();
    @SerializedName("fairy_soul")
    protected FairySouls fairySouls = new FairySouls();
    @SerializedName("player_data")
    protected PlayerData playerData = new PlayerData();
    protected Currencies currencies = new Currencies();
    protected Slayer slayer = new Slayer();
    @SerializedName("item_data")
    protected ItemSettings itemSettings = new ItemSettings();
    @SerializedName("jacobs_contest")
    protected JacobsContest jacobsContest = new JacobsContest();
    protected Inventory inventory = new Inventory();
    @SerializedName("pet_data")
    protected PetData petData = new PetData();
    protected Optional<Quests> quests = Optional.empty();

    // Profile
    @SerializedName("first_join_hub")
    protected SkyBlockDate.SkyBlockTime firstJoinHub;
    @SerializedPath("profile.first_join")
    protected SkyBlockDate.RealTime firstJoin;
    @SerializedPath("profile.personal_bank_upgrade")
    protected int personalBankUpgrade;
    @SerializedPath("profile.cookie_buff_active")
    protected boolean boosterCookieActive;

    // Maps
    @SerializedName("trophy_fish")
    @Getter(AccessLevel.NONE)
    protected @NotNull ConcurrentMap<String, Object> trophyFishMap = Concurrent.newMap();
    protected @NotNull ConcurrentMap<String, Long> collection = Concurrent.newMap();
    @SerializedPath("objectives.tutorial")
    protected @NotNull ConcurrentList<String> tutorialObjectives = Concurrent.newList();
    @SerializedPath("forge.forge_processes.forge_1")
    protected @NotNull ConcurrentMap<Integer, ForgeItem> forge = Concurrent.newMap();

    // Custom Initialization
    protected transient TrophyFish trophyFish;
    @Getter(AccessLevel.NONE)
    protected transient boolean accessoryBagLoaded;

    /**
     * Wraps this class with database access.
     * <br><br>
     * Requires an active database session.
     */
    public @NotNull EnhancedMember asEnhanced() {
        return new EnhancedMember(this);
    }

    @SuppressWarnings("all")
    public @NotNull AccessoryBag getAccessoryBag() {
        if (!this.accessoryBagLoaded) {
            Reflection.of(AccessoryBag.class).invokeMethod("initialize", this.accessoryBag, this);
            this.accessoryBagLoaded = true;
        }

        return this.accessoryBag;
    }

    public @NotNull TrophyFish getTrophyFish() {
        if (this.trophyFish == null)
            this.trophyFish = new TrophyFish(this.trophyFishMap);

        return this.trophyFish;
    }

}
