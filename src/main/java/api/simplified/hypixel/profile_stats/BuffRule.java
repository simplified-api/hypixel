package api.simplified.hypixel.profile_stats;

import api.simplified.skyblock.model.Stat;
import dev.simplified.util.NumberUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One {@code buff_effects} entry, with its key split into an operation and a target and its value
 * resolved to either a constant or an expression.
 * <p>
 * The split happens once per reference row rather than once per stat, which is what lets one
 * expression text be compiled once and evaluated against every stat it applies to.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BuffRule {

    /**
     * What the key's prefix does to the running total.
     */
    private final @NotNull Operation operation;

    /**
     * What is left of the key once the prefix is stripped - a stat id, or one of the two spellings
     * that mean every stat.
     */
    private final @NotNull String target;

    /**
     * Whether the target names every stat rather than one.
     */
    private final boolean everyStat;

    /**
     * The value when it is a plain number, null when it is an expression.
     */
    private final @Nullable Double constant;

    /**
     * The value when it is an expression, null when it is a plain number.
     */
    private final @Nullable String expressionText;

    /**
     * Splits one buff key and classifies its value.
     *
     * @param buffKey the whole key, an operation prefix followed by a target suffix
     * @param buffValue the raw value, a JSON number or an expression string
     * @return the parsed rule
     */
    public static @NotNull BuffRule parse(@NotNull String buffKey, @NotNull Object buffValue) {
        Operation operation = Operation.of(buffKey);
        String target = buffKey.substring(operation.getPrefix().length());
        String valueText = String.valueOf(buffValue);
        boolean creatable = NumberUtil.isCreatable(valueText);

        return new BuffRule(
            operation,
            target,
            "ALL".equals(target) || "PET_ALL".equals(target),
            creatable ? Double.valueOf(valueText) : null,
            creatable ? null : valueText
        );
    }

    /**
     * The same rule with its expression text replaced, which is how an nbt reference is substituted.
     *
     * @param expressionText the text to carry instead
     * @return the rewritten rule
     */
    @NotNull BuffRule withExpressionText(@NotNull String expressionText) {
        return new BuffRule(this.operation, this.target, this.everyStat, this.constant, expressionText);
    }

    /**
     * Whether this rule touches a given stat.
     *
     * @param statModel the stat being totalled
     * @return whether the target matches and the operation is allowed to touch it
     */
    public boolean matches(@NotNull Stat statModel) {
        return (this.isEveryStat() || statModel.getId().equals(this.getTarget()))
            && this.getOperation().appliesTo(statModel);
    }

}
