/**
 * The buff grammar folded onto a running number.
 *
 * <p>
 * A buff row is data and lives in {@code skyblock} beside the model, because every one of its types
 * is a column type on
 * {@link api.simplified.skyblock.model.Buff Buff} and {@code skyblock} cannot see this module. What
 * lives here is the half that reads them: the switch over the term kinds, the switch over the
 * comparison operators, and the fold in operation order.
 *
 * <p>
 * The rule that decides every placement in this tree is one line. <b>A type belongs here if and only
 * if it never names a table type; everything that writes a table stays at the root.</b> Nothing in
 * this package names
 * {@link api.simplified.hypixel.response.skyblock.stats.StatTable StatTable},
 * {@link api.simplified.hypixel.response.skyblock.stats.StatSink StatSink},
 * {@link api.simplified.hypixel.response.skyblock.stats.StatHalf StatHalf},
 * {@link api.simplified.hypixel.response.skyblock.stats.StatOrigin StatOrigin} or
 * {@link api.simplified.hypixel.response.skyblock.stats.Data Data} - the table work is the caller's,
 * which reads a cell, asks for a number and writes it back.
 *
 * <p>
 * That is what makes this the one cut worth making: it costs the table core nothing, and it is the
 * only subpackage this design has.
 */
package api.simplified.hypixel.response.skyblock.stats.buff;
