package me.qmftm.casterability.game

import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.util.toComponent
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID

/**
 * 게임 중 플레이어의 액션바 UI를 렌더링합니다.
 *
 * 순서는 항상 이렇게 고정입니다. 비어 있는 칸은 구분선까지 통째로 건너뜁니다.
 *
 *     상태이상  |  능력 부가 정보  |  능력 쿨타임
 *     §c기절: 2.4 §7| §e충전 3/5 §7| §b질투: 12.5
 *
 * ChzzkAbility UI.sk 대비 개선점:
 * - 반복문마다 문자열 전체를 다시 만들던 것을 StringBuilder 한 번으로 대체
 * - 표시할 내용이 없을 때만 한 번 액션바를 비우고, 이후 매 틱 빈 문자열을 보내지 않음
 */
object GameUIRenderer {

    /** 현재 액션바에 무언가를 그려둔 플레이어 */
    private val activeUi = mutableSetOf<UUID>()

    fun updateActionBar(player: Player) {
        val effects   = PlayerStateManager.getAllEffects(player)
        val customUi  = PlayerStateManager.getAllCustomUi(player)
        val cooldowns = AbilityRegistry.getAllCooldowns(player)

        if (effects.isEmpty() && customUi.isEmpty() && cooldowns.isEmpty()) {
            // 직전까지 무언가 그려져 있었을 때만 한 번 지운다
            if (activeUi.remove(player.uniqueId)) {
                player.sendActionBar("".toComponent())
            }
            return
        }

        activeUi.add(player.uniqueId)

        val sb = StringBuilder()

        // 1. 상태이상
        effects.forEach { (id, time) ->
            sb.separate()
            val name = PlayerStateManager.effectDisplayName(id)
            sb.append("&6").append(name).append("&f: ").append(seconds(time))
        }

        // 2. 능력 부가 정보 — 스크립트가 넣은 문자열을 그대로 쓴다
        customUi.forEach { (_, text) ->
            sb.separate()
            sb.append(text)
        }

        // 3. 능력 쿨타임
        cooldowns.forEach { (abilityId, ticks) ->
            sb.separate()
            val name = AbilityRegistry.getAbility(abilityId)?.displayName ?: abilityId
            sb.append("&b").append(name).append("&f: ").append(seconds(ticks / 20.0))
        }

        player.sendActionBar(sb.toString().toComponent())
    }

    /** 앞에 이미 뭔가 있을 때만 구분선을 넣는다 — 빈 칸 때문에 구분선만 남지 않게 */
    private fun StringBuilder.separate() {
        if (isNotEmpty()) append(" &7| ")
    }

    /** 서버 로케일이 뭐든 소수점은 마침표로 찍는다 */
    private fun seconds(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

    fun clear(player: Player) {
        activeUi.remove(player.uniqueId)
    }

    fun clearAll() {
        activeUi.clear()
    }
}
