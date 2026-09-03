package me.qmftm.casterability.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 무적 시간이 끝나고 실제 전투가 시작될 때(IN_GAME) 발생합니다.
 * Skript의 `on game start:` 가 이것을 받습니다.
 *
 * 능력 추첨이나 월드 생성 시점이 아니라, 능력이 실제로 도는 시점입니다.
 */
class GameStartEvent : Event() {

    override fun getHandlers(): HandlerList = HANDLER_LIST

    companion object {
        @JvmField
        val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }
}
