package me.qmftm.casterability.ability

import org.bukkit.Material
import org.bukkit.event.Event

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

    /** 액션바 등에 보여줄 이름. 없으면 id를 그대로 쓴다. */
    val name: String? = null,

    /** PASSIVE + `event:` 일 때 스크립트에 적힌 이벤트 이름 (없으면 null) */
    val eventName: String? = null,

    /** 위 이름을 풀어놓은 실제 Bukkit 이벤트 클래스들 */
    val eventClasses: List<Class<out Event>> = emptyList(),
) {
    /** 주기가 아니라 이벤트를 받아 발동하는 패시브인지 */
    val isEventDriven: Boolean get() = trigger == AbilityTrigger.PASSIVE && eventClasses.isNotEmpty()

    /** 화면에 보여줄 이름. name을 안 적었으면 id를 그대로 쓴다. */
    val displayName: String get() = name ?: id
}
