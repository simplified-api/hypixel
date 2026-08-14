package api.simplified.hypixel.response.skyblock.stats;

import org.jetbrains.annotations.NotNull;

/**
 * Where on a member one item instance sits, as an address rather than as the instance.
 * <p>
 * A slot is what a stat sheet files a table under, so two instances of one item in two places stay
 * two sets of cells. The address is stable against what fills it - an unworn armour piece leaves its
 * index unused rather than shifting the pieces after it.
 *
 * @param kind which family of slot this is one of
 * @param index which slot of that family, counted from zero
 */
record ItemSlot(@NotNull Kind kind, int index) {

    /**
     * The families of slot a member has.
     */
    enum Kind {

        /**
         * A worn armour piece, helmet first.
         */
        ARMOR,

        /**
         * An accessory that counts toward magical power.
         */
        ACCESSORY

    }

}
