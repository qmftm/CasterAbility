package me.qmftm.casterability.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * 상태이상이 걸리기 직전에 발생하는 이벤트.
 * Skript의 `on status effect apply:` 가 이것을 받습니다.
 *
 * 취소하면 상태이상이 걸리지 않습니다. 면역 능력을 이걸로 만듭니다.
 * duration을 바꾸면 바뀐 시간으로 걸립니다.
 *
 * @param player        상태이상을 받을 플레이어
 * @param effectId      상태이상 id
 * @param durationTicks 걸릴 시간(tick). 스크립트에서 바꿀 수 있습니다.
 * @param mode          이미 걸려 있을 때의 처리 방식
 */
class StatusEffectApplyEvent(
    player: Player,
    val effectId: String,
    var durationTicks: Int,
    val mode: me.qmftm.casterability.game.StatusEffectMode,
) : PlayerEvent(player), Cancellable {

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
