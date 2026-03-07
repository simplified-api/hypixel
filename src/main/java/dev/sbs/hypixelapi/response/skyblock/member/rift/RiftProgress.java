package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class RiftProgress {

    private @NotNull RiftAccess access = new RiftAccess();
    @SerializedName("slayer_quest")
    private @NotNull RiftSlayerQuest slayerQuest = new RiftSlayerQuest();
    private @NotNull RiftInventory inventory = new RiftInventory();
    @SerializedName("gallery")
    private NbtContent timecharmGallery = new NbtContent();
    @SerializedName("lifetime_purchased_boundaries")
    private @NotNull ConcurrentList<String> purchasedBoundaries = Concurrent.newList();
    @SerializedName("enigma")
    private EnigmasCrib enigmasCrib = new EnigmasCrib();
    @Getter(AccessLevel.NONE)
    @SerializedName("wither_cage")
    private Porhtal porhtal = new Porhtal();
    @SerializedName("dead_cats")
    private DeadCats deadCats = new DeadCats();

    // Locations
    @SerializedName("wizard_tower")
    private WizardTower wizardTower = new WizardTower();
    @SerializedName("wyld_woods")
    private WyldWoods wyldWoods = new WyldWoods();
    @SerializedName("black_lagoon")
    private BlackLagoon blackLagoon = new BlackLagoon();
    @SerializedName("west_village")
    private WestVillage westVillage = new WestVillage();
    private Dreadfarm dreadfarm = new Dreadfarm();
    @SerializedName("village_plaza")
    private VillagePlaza villagePlaza = new VillagePlaza();
    @SerializedName("castle")
    private StillgoreChateau stillgoreChateau = new StillgoreChateau();

    public @NotNull ConcurrentList<String> getKilledEyes() {
        return this.porhtal.getKilledEyes();
    }

    @Getter
    private static class Porhtal {

        @SerializedName("killed_eyes")
        private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();

    }

}
