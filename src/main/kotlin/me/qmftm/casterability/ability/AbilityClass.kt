package me.qmftm.casterability.ability

/**
 * Skript DSL:
 *
 * ability class "menhera":
 *     name: "멘헤라"
 *     tier: 3
 *
 * tier: 0=Legendary  1=S  2=A  3=B  4=C
 */
data class AbilityClass(
    val id: String,
    val name: String,
    val tier: Int,
) {
    val tierDisplay: String get() = when (tier) {
        0    -> "§6§lLegendary"
        1    -> "§d§lS"
        2    -> "§c§lA"
        3    -> "§9§lB"
        4    -> "§a§lC"
        else -> "§7Unknown"
    }
}
