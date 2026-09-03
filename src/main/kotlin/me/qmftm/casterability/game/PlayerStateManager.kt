package me.qmftm.casterability.game

import org.bukkit.entity.Player
import java.util.UUID

/**
 * 게임 중 플레이어별 상태(능력 변수 / 이펙트 / 커스텀 UI)를 관리합니다.
 *
 * ChzzkAbility의 {ca.player::*}, {ca.effect::*}, {ca.ui.custom::*}를 대체합니다.
 *
 * 개선점:
 * - addPlayerVar가 리스트에 값을 밀어넣던 버그(`add ... to {...::*}`)를 실제 덧셈으로 수정
 * - 플레이어를 UUID로만 참조해 Player 객체를 붙들지 않음 (메모리 누수 방지)
 * - 게임 종료 시 clearAll()로 전부 정리
 */
object PlayerStateManager {

    private val playerVariables = mutableMapOf<UUID, MutableMap<String, Any>>()
    private val effects         = mutableMapOf<UUID, MutableMap<String, Double>>()
    private val customUi        = mutableMapOf<UUID, MutableMap<String, String>>()

    /** 이펙트 id → 액션바에 표시할 이름 (ChzzkAbility의 caEffects) */
    private val effectNames = mutableMapOf<String, String>()

    // ── 능력 변수 ──────────────────────────────────────────

    fun setPlayerVar(player: Player, key: String, value: Any) {
        playerVariables.getOrPut(player.uniqueId) { mutableMapOf() }[key] = value
    }

    fun getPlayerVar(player: Player, key: String): Any? =
        playerVariables[player.uniqueId]?.get(key)

    /** 숫자 변수에 값을 더한다. 값이 없으면 0에서 시작. */
    fun addPlayerVar(player: Player, key: String, amount: Double): Double {
        val map = playerVariables.getOrPut(player.uniqueId) { mutableMapOf() }
        val current = (map[key] as? Number)?.toDouble() ?: 0.0
        val next = current + amount
        map[key] = next
        return next
    }

    /** 플레이어를 값으로 저장할 때는 UUID로 저장한다. */
    fun setPlayerRefVar(player: Player, key: String, target: Player) {
        setPlayerVar(player, key, target.uniqueId)
    }

    fun getPlayerRefVar(player: Player, key: String): UUID? =
        getPlayerVar(player, key) as? UUID

    fun deletePlayerVar(player: Player, key: String) {
        playerVariables[player.uniqueId]?.remove(key)
    }

    // ── 이펙트 ────────────────────────────────────────────

    /** 액션바에 쓸 이펙트 이름을 등록한다. */
    fun registerEffect(id: String, displayName: String) {
        effectNames[id] = displayName
    }

    fun effectDisplayName(id: String): String = effectNames[id] ?: id

    /**
     * @param mode "add" = 남은 시간에 더함, "set" = 기존보다 길 때만 갱신
     */
    fun addEffect(player: Player, effectId: String, durationSeconds: Double, mode: String = "add") {
        val map = effects.getOrPut(player.uniqueId) { mutableMapOf() }
        when (mode) {
            "set" -> if ((map[effectId] ?: 0.0) < durationSeconds) map[effectId] = durationSeconds
            else  -> map[effectId] = (map[effectId] ?: 0.0) + durationSeconds
        }
    }

    fun getEffect(player: Player, effectId: String): Double =
        effects[player.uniqueId]?.get(effectId) ?: 0.0

    fun hasEffect(player: Player, effectId: String): Boolean = getEffect(player, effectId) > 0.0

    fun getAllEffects(player: Player): Map<String, Double> =
        effects[player.uniqueId] ?: emptyMap()

    fun deleteEffect(player: Player, effectId: String) {
        effects[player.uniqueId]?.remove(effectId)
    }

    // ── 커스텀 UI ─────────────────────────────────────────

    fun setCustomUi(player: Player, uiId: String, text: String) {
        customUi.getOrPut(player.uniqueId) { mutableMapOf() }[uiId] = text
    }

    fun removeCustomUi(player: Player, uiId: String) {
        customUi[player.uniqueId]?.remove(uiId)
    }

    fun getAllCustomUi(player: Player): Map<String, String> =
        customUi[player.uniqueId] ?: emptyMap()

    // ── 정리 ──────────────────────────────────────────────

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

    // ── 틱 처리 (GameScheduler에서 2틱마다 호출) ───────────

    fun tickEffects() {
        val playerIter = effects.entries.iterator()
        while (playerIter.hasNext()) {
            val entry = playerIter.next()
            val map = entry.value
            val iter = map.entries.iterator()
            while (iter.hasNext()) {
                val e = iter.next()
                val next = e.value - 0.1
                if (next <= 0.0) iter.remove() else e.setValue(next)
            }
            if (map.isEmpty()) playerIter.remove()
        }
    }
}
