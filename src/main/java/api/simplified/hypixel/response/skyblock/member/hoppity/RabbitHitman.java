package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.time.Instant;

@Getter
public class RabbitHitman {

    @SerializedName("rabbit_hitmen_slots")
    private int slots;
    @SerializedName("missed_uncollected_eggs")
    private int missedUncollectedEggs;
    @SerializedName("egg_slot_cooldown_mark")
    private Instant eggSlotCooldown;
    @SerializedName("egg_slot_cooldown_sum")
    private int eggSlotCooldownSum;

}
