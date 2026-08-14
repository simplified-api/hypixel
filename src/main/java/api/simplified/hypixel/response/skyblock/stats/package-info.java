/**
 * Every stat one member has on one profile, and the machinery that totals them.
 *
 * <p>
 * This is the derived layer. Nothing here binds and nothing here carries a serialization annotation -
 * a total is something a caller asks for by name through
 * {@link api.simplified.hypixel.response.skyblock.stats.ProfileStats#compute ProfileStats.compute},
 * which resolves every id against a repository and therefore needs a session.
 *
 * <p>
 * A type from this package reaches the wire tree as a parameter or a return type and never as a
 * field, so a decode pulls none of it in and binding still derives nothing.
 *
 * <p>
 * The characterisation harness lives in this package rather than beside it, which is what lets
 * {@link api.simplified.hypixel.response.skyblock.stats.StatTable StatTable} and its neighbours stay
 * package-private.
 */
package api.simplified.hypixel.response.skyblock.stats;
