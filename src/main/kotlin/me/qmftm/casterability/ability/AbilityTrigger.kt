package me.qmftm.casterability.ability

/**
 * 능력 발동 트리거 종류
 *
 * Skript에서:
 *   ability "fireball":
 *       trigger: right_click
 *       trigger: passive        ← 매 tick 주기적으로
 *       trigger: on_hit         ← 내가 남을 공격할 때
 *       trigger: on_damaged     ← 내가 피해를 받을 때
 *       trigger: on_kill        ← 내가 킬 했을 때
 *       trigger: on_death       ← 내가 죽을 때
 */
enum class AbilityTrigger(val skriptName: String) {
    RIGHT_CLICK("right_click"),
    LEFT_CLICK("left_click"),
    PASSIVE("passive"),
    ON_HIT("on_hit"),
    ON_DAMAGED("on_damaged"),
    ON_KILL("on_kill"),
    ON_DEATH("on_death");

    companion object {
        fun fromString(s: String): AbilityTrigger? =
            entries.firstOrNull { it.skriptName == s.lowercase() }
    }
}
