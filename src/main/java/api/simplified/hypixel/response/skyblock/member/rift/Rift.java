package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.annotation.SerializedPath;
import org.jetbrains.annotations.NotNull;

/**
 * A member's whole state inside the Rift, the separate dimension entered through the portal in the
 * Hub's Wizard Tower.
 *
 * <p>
 * The Rift keeps its own currency, its own clock and its own containers, and this class is the
 * router over all of it - entry state, the vampire slayer quest, the dimension's inventory, the
 * timecharm gallery, and one field per location the member has progress in. A member who never
 * entered leaves the whole subtree off the wire, so every field defaults to an empty instance rather
 * than to null.
 *
 * <p>
 * Everything here is bound straight from the wire; nothing in this tree is derived or looked up.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Rift_Dimension">Rift Dimension</a>
 */
@Getter
public class Rift {

    /**
     * Entry state - the infusion credit left and whether the Rift Prism was consumed.
     */
    private @NotNull RiftAccess access = new RiftAccess();

    /**
     * The vampire slayer quest in progress, bound from {@code slayer_quest}.
     */
    @SerializedName("slayer_quest")
    private @NotNull RiftSlayerQuest slayerQuest = new RiftSlayerQuest();

    /**
     * The Rift's own inventory, armour, equipment and ender chest.
     */
    private @NotNull RiftInventory inventory = new RiftInventory();

    /**
     * The timecharms donated to the Rift Gallery, bound from the wire key {@code gallery}.
     */
    @SerializedName("gallery")
    private @NotNull TimecharmGallery timecharmGallery = new TimecharmGallery();

    /**
     * Ids of the zones bought from the Wizard over the member's lifetime, bound from
     * {@code lifetime_purchased_boundaries}. These are zone ids rather than location objects, and
     * several of them - {@code colosseum}, {@code barrier_street}, {@code mountaintop} and
     * {@code living_cave} - have no class of their own anywhere in this tree.
     */
    @SerializedName("lifetime_purchased_boundaries")
    private @NotNull ConcurrentList<String> purchasedBoundaries = Concurrent.newList();

    /**
     * Enigma's Crib progress, bound from the wire key {@code enigma}.
     */
    @SerializedName("enigma")
    private @NotNull EnigmasCrib enigmasCrib = new EnigmasCrib();

    /**
     * Ids of the rogue eyes calmed for Porhtal in the Broken Cage, each one a permanent Rift Time
     * bonus and a teleport endpoint. {@link SerializedPath} lifts the list out of the nested
     * {@code wither_cage} node, which carries this single key and gets no class of its own.
     */
    @SerializedPath("wither_cage.killed_eyes")
    private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();

    /**
     * The dead cat hunt that unlocks the Montezuma pet, bound from {@code dead_cats}.
     */
    @SerializedName("dead_cats")
    private @NotNull DeadCats deadCats = new DeadCats();

    // Locations

    /**
     * Progress in the Rift's own Wizard Tower, bound from {@code wizard_tower}.
     */
    @SerializedName("wizard_tower")
    private @NotNull WizardTower wizardTower = new WizardTower();

    /**
     * Progress in the Wyld Woods, bound from {@code wyld_woods}.
     */
    @SerializedName("wyld_woods")
    private @NotNull WyldWoods wyldWoods = new WyldWoods();

    /**
     * Progress in the Black Lagoon, bound from {@code black_lagoon}.
     */
    @SerializedName("black_lagoon")
    private @NotNull BlackLagoon blackLagoon = new BlackLagoon();

    /**
     * Progress in the West Village, bound from {@code west_village}.
     */
    @SerializedName("west_village")
    private @NotNull WestVillage westVillage = new WestVillage();

    /**
     * Progress on the Dreadfarm; the field name already matches the wire key.
     */
    private @NotNull Dreadfarm dreadfarm = new Dreadfarm();

    /**
     * Progress in the Village Plaza, bound from {@code village_plaza}.
     */
    @SerializedName("village_plaza")
    private @NotNull VillagePlaza villagePlaza = new VillagePlaza();

    /**
     * Progress in the Stillgore Château, bound from the wire key {@code castle}.
     */
    @SerializedName("castle")
    private @NotNull StillgoreChateau stillgoreChateau = new StillgoreChateau();

}
