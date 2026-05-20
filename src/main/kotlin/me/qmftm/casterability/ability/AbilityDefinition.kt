package me.qmftm.casterability.ability

import org.bukkit.Material

/**
 * Skript DSL:
 *
 * ability "love":
 *     class: "menhera"
 *     trigger: right_click
 *     item: blaze_rod
 *     cooldown: 30
 *
 * 하나의 ability class에 여러 ability가 속할 수 있음.
 * 예) menhera 클래스 → love, self_harm 두 개의 ability
 */
data class AbilityDefinition(
    val id: String,
    val classId: String,
    val trigger: AbilityTrigger,
    val item: Material?,          // 트리거 아이템 (우클릭/좌클릭 시)
    val cooldownSeconds: Int,     // 0이면 쿨타임 없음
    val passiveIntervalTicks: Long, // PASSIVE 트리거일 때 주기 (기본 20 = 1초)
)
