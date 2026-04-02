package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Rift {

    private @NotNull RiftAccess access = new RiftAccess();
    @SerializedName("slayer_quest")
    private @NotNull RiftSlayerQuest slayerQuest = new RiftSlayerQuest();
    private @NotNull RiftInventory inventory = new RiftInventory();
    @SerializedName("gallery")
    private @NotNull TimecharmGallery timecharmGallery = new TimecharmGallery();
    @SerializedName("lifetime_purchased_boundaries")
    private @NotNull ConcurrentList<String> purchasedBoundaries = Concurrent.newList();
    @SerializedName("enigma")
    private @NotNull EnigmasCrib enigmasCrib = new EnigmasCrib();
    @Getter(AccessLevel.NONE)
    @SerializedName("wither_cage")
    private @NotNull Porhtal porhtal = new Porhtal();
    @SerializedName("dead_cats")
    private @NotNull DeadCats deadCats = new DeadCats();

    // Locations
    @SerializedName("wizard_tower")
    private @NotNull WizardTower wizardTower = new WizardTower();
    @SerializedName("wyld_woods")
    private @NotNull WyldWoods wyldWoods = new WyldWoods();
    @SerializedName("black_lagoon")
    private @NotNull BlackLagoon blackLagoon = new BlackLagoon();
    @SerializedName("west_village")
    private @NotNull WestVillage westVillage = new WestVillage();
    private @NotNull Dreadfarm dreadfarm = new Dreadfarm();
    @SerializedName("village_plaza")
    private @NotNull VillagePlaza villagePlaza = new VillagePlaza();
    @SerializedName("castle")
    private @NotNull StillgoreChateau stillgoreChateau = new StillgoreChateau();

    public @NotNull ConcurrentList<String> getKilledEyes() {
        return this.porhtal.getKilledEyes();
    }

    @Getter
    private static class Porhtal {

        @SerializedName("killed_eyes")
        private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();

    }

}
