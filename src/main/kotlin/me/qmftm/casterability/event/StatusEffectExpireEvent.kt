package me.qmftm.casterability.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * 상태이상의 시간이 다 되어 저절로 풀릴 때 발생하는 이벤트.
 * Skript의 `on status effect expire:` 가 이것을 받습니다.
 *
 * `remove status effect ...` 로 직접 지운 경우에는 발생하지 않습니다.
 * 시간이 다 된 것과 누가 걷어낸 것을 구분할 수 있어야 하기 때문입니다.
 */
class StatusEffectExpireEvent(
    player: Player,
    val effectId: String,
) : PlayerEvent(player) {

    override fun getHandlers(): HandlerList = HANDLER_LIST

    companion object {
        @JvmField
        val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }
}
