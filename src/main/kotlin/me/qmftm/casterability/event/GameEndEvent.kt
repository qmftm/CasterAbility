package me.qmftm.casterability.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 게임이 끝날 때 발생합니다.
 * Skript의 `on game end:` 가 이것을 받습니다.
 *
 * 플레이어의 능력·쿨타임·상태이상을 지우기 **전에** 발생하므로,
 * 이 안에서는 아직 누가 어떤 능력이었는지 볼 수 있습니다.
 * 능력이 플레이어에게 남겨둔 것(걷기 속도 등)을 되돌리는 자리입니다.
 */
class GameEndEvent : Event() {

    override fun getHandlers(): HandlerList = HANDLER_LIST

    companion object {
        @JvmField
        val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }
}
