package me.qmftm.casterability.event

import me.qmftm.casterability.ability.AbilityDefinition
import me.qmftm.casterability.ability.AbilityTrigger
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * 능력이 발동될 때 발생하는 커스텀 이벤트.
 * Skript의 `on ability use "..."` 이벤트가 이것을 수신합니다.
 *
 * @param player    능력을 사용한 플레이어
 * @param ability   발동된 능력 정의
 * @param trigger   어떤 트리거로 발동되었는지
 * @param target    대상 엔티티 (on_hit / on_damaged 시 설정됨, 없으면 null)
 * @param damage    관련 피해량 (on_hit / on_damaged 시, 없으면 null)
 * @param sourceEvent 이 능력을 발동시킨 원본 Bukkit 이벤트.
 *                    `trigger: passive` + `event:` 로 정의한 능력일 때만 들어 있습니다.
 *                    스크립트가 cancel event 를 하면 이 이벤트도 같이 취소됩니다.
 */
class AbilityUseEvent(
    player: Player,
    val ability: AbilityDefinition,
    val trigger: AbilityTrigger,
    val target: Entity? = null,
    var damage: Double? = null,
    val sourceEvent: org.bukkit.event.Event? = null,
) : PlayerEvent(player), Cancellable {

    val abilityId: String get() = ability.id

    private var cancelled = false
    override fun isCancelled() = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }

    override fun getHandlers(): HandlerList = HANDLER_LIST

    companion object {
        @JvmField
        val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }
}
