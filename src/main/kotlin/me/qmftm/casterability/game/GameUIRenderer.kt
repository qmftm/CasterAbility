package me.qmftm.casterability.game

import me.qmftm.casterability.util.toComponent
import org.bukkit.entity.Player

/**
 * 게임 중 플레이어의 액션바 UI를 렌더링합니다.
 *
 * ChzzkAbility 문제점 해결:
 * - UI.sk의 비효율적인 문자열 반복 연결 개선
 * - StringBuilder 사용으로 성능 최적화
 * - 불필요한 UI 업데이트 제거
 */
object GameUIRenderer {

    private val activeUi = mutableSetOf<String>()

    /**
     * 플레이어의 액션바를 업데이트합니다.
     */
    fun updateActionBar(player: Player) {
        val effects = PlayerStateManager.getAllEffects(player)
        val customUi = PlayerStateManager.getAllCustomUi(player)

        // 표시할 항목이 없으면 액션바 비우기
        if (effects.isEmpty() && customUi.isEmpty()) {
            if (activeUi.contains(player.uniqueId.toString())) {
                player.sendActionBar("".toComponent())
                activeUi.remove(player.uniqueId.toString())
            }
            return
        }

        activeUi.add(player.uniqueId.toString())

        // StringBuilder로 효율적으로 문자열 구성
        val sb = StringBuilder()
        var first = true

        // 이펙트 표시 (형식: "이펙트명: 남은시간")
        effects.forEach { (id, time) ->
            if (!first) sb.append(" &7| ")
            sb.append("&6$id&f: %.1f".format(time))
            first = false
        }

        // 커스텀 UI 표시
        customUi.forEach { (_, text) ->
            if (!first) sb.append(" &7| ")
            sb.append(text)
            first = false
        }

        player.sendActionBar(sb.toString().toComponent())
    }

    /**
     * 모든 활성 UI를 비웁니다.
     */
    fun clearAll() {
        activeUi.clear()
    }
}
