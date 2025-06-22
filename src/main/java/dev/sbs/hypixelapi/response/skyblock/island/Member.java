package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.crimson_isle.CrimsonIsle;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.crimson_isle.TrophyFish;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.dungeon.DungeonData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.mining.ForgeItem;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.mining.Mining;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.pet.PetData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.slayer.Slayer;
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
public class Member implements PostInit {

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

    // PostInit
    protected transient TrophyFish trophyFish;

    @Override
    public void postInit() {
        this.accessoryBag.initialize(this);
        this.trophyFish = new TrophyFish(this.trophyFishMap);
    }

}
