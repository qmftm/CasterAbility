package me.qmftm.casterability.game

import me.qmftm.casterability.util.toComponent
import org.bukkit.entity.Player
import java.util.UUID

/**
 * 게임 중 플레이어의 액션바 UI를 렌더링합니다.
 *
 * ChzzkAbility UI.sk 대비 개선점:
 * - 반복문마다 문자열 전체를 다시 만들던 것을 StringBuilder 한 번으로 대체
 * - 표시할 내용이 없을 때만 한 번 액션바를 비우고, 이후 매 틱 빈 문자열을 보내지 않음
 */
object GameUIRenderer {

    /** 현재 액션바에 무언가를 그려둔 플레이어 */
    private val activeUi = mutableSetOf<UUID>()

    fun updateActionBar(player: Player) {
        val effects  = PlayerStateManager.getAllEffects(player)
        val customUi = PlayerStateManager.getAllCustomUi(player)

        if (effects.isEmpty() && customUi.isEmpty()) {
            // 직전까지 무언가 그려져 있었을 때만 한 번 지운다
            if (activeUi.remove(player.uniqueId)) {
                player.sendActionBar("".toComponent())
            }
            return
        }

        activeUi.add(player.uniqueId)

        val sb = StringBuilder()
        effects.forEach { (id, time) ->
            if (sb.isNotEmpty()) sb.append(" &7| ")
            val name = PlayerStateManager.effectDisplayName(id)
            sb.append("&6").append(name).append("&f: ").append(String.format("%.1f", time))
        }
        customUi.forEach { (_, text) ->
            if (sb.isNotEmpty()) sb.append(" &7| ")
            sb.append(text)
        }

        player.sendActionBar(sb.toString().toComponent())
    }

    fun clear(player: Player) {
        activeUi.remove(player.uniqueId)
    }

    fun clearAll() {
        activeUi.clear()
    }
}
