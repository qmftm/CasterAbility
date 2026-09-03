package me.qmftm.casterability.game

import org.bukkit.entity.Player
import java.util.UUID

/**
 * 게임 중 플레이어의 상태를 관리합니다.
 * 능력별 변수, 이펙트, UI 데이터 등을 중앙에서 관리합니다.
 *
 * ChzzkAbility의 {ca.player::*}, {ca.effect::*}, {ca.ui.custom::*} 등을 개선하여 구현
 */
object PlayerStateManager {

    // ── 능력별 플레이어 변수 ────────────────────────────────
    // {ca.player::%{player}%::%{name}%} → playerVariables[uuid][name]
    private val playerVariables = mutableMapOf<UUID, MutableMap<String, Any?>>()

    // ── 이펙트 (체력 감소 시간 등) ────────────────────────
    // {ca.effect::%{player}%::%{id}%::time} → effects[uuid][id]
    private val effects = mutableMapOf<UUID, MutableMap<String, Double>>()

    // ── 커스텀 UI 메시지 ────────────────────────────────────
    // {ca.ui.custom::%{player}%::%{id}%} → customUi[uuid][id]
    private val customUi = mutableMapOf<UUID, MutableMap<String, String>>()

    // ── 플레이어 변수 관리 ──────────────────────────────────

    fun setPlayerVar(player: Player, key: String, value: Any?) {
        playerVariables.getOrPut(player.uniqueId) { mutableMapOf() }[key] = value
    }

    fun getPlayerVar(player: Player, key: String): Any? =
        playerVariables[player.uniqueId]?.get(key)

    fun deletePlayerVar(player: Player, key: String) {
        playerVariables[player.uniqueId]?.remove(key)
    }

    fun deleteAllPlayerVars(player: Player) {
        playerVariables.remove(player.uniqueId)
    }

    // ── 이펙트 관리 (0.1씩 감소) ────────────────────────────

    fun addEffect(player: Player, effectId: String, durationSeconds: Double, mode: String = "add") {
        val playerEffects = effects.getOrPut(player.uniqueId) { mutableMapOf() }
        when (mode) {
            "add" -> playerEffects[effectId] = (playerEffects[effectId] ?: 0.0) + durationSeconds
            "set" -> {
                val existing = playerEffects[effectId] ?: 0.0
                if (existing < durationSeconds) {
                    playerEffects[effectId] = durationSeconds
                }
            }
        }
    }

    fun getEffect(player: Player, effectId: String): Double =
        effects[player.uniqueId]?.get(effectId) ?: 0.0

    fun hasEffect(player: Player, effectId: String): Boolean =
        getEffect(player, effectId) > 0.0

    fun getAllEffects(player: Player): Map<String, Double> =
        effects[player.uniqueId]?.toMap() ?: emptyMap()

    fun deleteEffect(player: Player, effectId: String) {
        effects[player.uniqueId]?.remove(effectId)
    }

    fun deleteAllEffects(player: Player) {
        effects.remove(player.uniqueId)
    }

    // ── UI 관리 ────────────────────────────────────────────

    fun setCustomUi(player: Player, uiId: String, text: String) {
        customUi.getOrPut(player.uniqueId) { mutableMapOf() }[uiId] = text
    }

    fun removeCustomUi(player: Player, uiId: String) {
        customUi[player.uniqueId]?.remove(uiId)
    }

    fun getAllCustomUi(player: Player): Map<String, String> =
        customUi[player.uniqueId]?.toMap() ?: emptyMap()

    fun deleteAllCustomUi(player: Player) {
        customUi.remove(player.uniqueId)
    }

    // ── 플레이어 클리어 (게임 종료 시) ──────────────────────

    fun clearPlayer(player: Player) {
        val uuid = player.uniqueId
        playerVariables.remove(uuid)
        effects.remove(uuid)
        customUi.remove(uuid)
    }

    fun clearAll() {
        playerVariables.clear()
        effects.clear()
        customUi.clear()
    }

    // ── 이펙트 틱 처리 (매 2 틱마다 호출) ──────────────────

    fun tickEffects() {
        effects.forEach { (_, playerEffects) ->
            val iter = playerEffects.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                val newTime = entry.value - 0.1
                if (newTime <= 0) {
                    iter.remove()
                } else {
                    entry.setValue(newTime)
                }
            }
        }
    }
}
