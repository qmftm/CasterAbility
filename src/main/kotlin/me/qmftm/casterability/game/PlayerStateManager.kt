package me.qmftm.casterability.game

import me.qmftm.casterability.event.StatusEffectApplyEvent
import me.qmftm.casterability.event.StatusEffectExpireEvent
import org.bukkit.Bukkit
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
    /** 플레이어 UUID → 상태이상 id → 남은 tick */
    private val effects         = mutableMapOf<UUID, MutableMap<String, Int>>()
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

    // ── 상태이상 ──────────────────────────────────────────

    /** 액션바에 쓸 상태이상 이름을 등록한다. 등록하지 않으면 id를 그대로 쓴다. */
    fun registerEffect(id: String, displayName: String) {
        effectNames[id] = displayName
    }

    fun effectDisplayName(id: String): String = effectNames[id] ?: id

    /**
     * 상태이상을 건다. 시간은 tick 단위.
     *
     * 거는 순간 [StatusEffectApplyEvent]가 발생하므로, 스크립트가 취소하거나
     * 시간을 바꿀 수 있다. 취소되면 아무것도 걸리지 않는다.
     *
     * @return 실제로 걸렸으면 true
     */
    fun applyEffect(
        player: Player,
        effectId: String,
        durationTicks: Int,
        mode: StatusEffectMode = StatusEffectMode.LONGEST,
    ): Boolean {
        val current = getEffect(player, effectId)
        if (mode == StatusEffectMode.IGNORE && current > 0) return false

        val event = StatusEffectApplyEvent(player, effectId, durationTicks, mode)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return false

        val duration = event.durationTicks
        if (duration <= 0) return false

        val map = effects.getOrPut(player.uniqueId) { mutableMapOf() }
        map[effectId] = when (mode) {
            StatusEffectMode.STACK   -> current + duration
            StatusEffectMode.REPLACE -> duration
            // LONGEST / IGNORE — IGNORE는 위에서 이미 걸러졌으므로 여기선 안 걸린 상태다
            else -> maxOf(current, duration)
        }
        return true
    }

    /** 남은 시간 (tick 단위, 없으면 0) */
    fun getEffect(player: Player, effectId: String): Int =
        effects[player.uniqueId]?.get(effectId) ?: 0

    fun hasEffect(player: Player, effectId: String): Boolean = getEffect(player, effectId) > 0

    fun getAllEffects(player: Player): Map<String, Int> =
        effects[player.uniqueId] ?: emptyMap()

    fun deleteEffect(player: Player, effectId: String) {
        effects[player.uniqueId]?.remove(effectId)
    }

    /** 이 플레이어의 상태이상을 전부 없앤다. 능력 변수나 커스텀 UI는 건드리지 않는다. */
    fun clearEffects(player: Player) {
        effects.remove(player.uniqueId)
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

    /** 이 플레이어의 부가 정보를 전부 지운다. 상태이상이나 능력 변수는 건드리지 않는다. */
    fun clearCustomUi(player: Player) {
        customUi.remove(player.uniqueId)
    }

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

    // ── 틱 처리 (GameScheduler에서 매 틱 호출) ─────────────

    fun tickEffects() {
        // 시간이 다 된 것들. 순회 중에 이벤트를 부르면 스크립트가 effects를 건드릴 수
        // 있으므로, 다 돌고 나서 한꺼번에 알린다.
        val expired = mutableListOf<Pair<UUID, String>>()

        val playerIter = effects.entries.iterator()
        while (playerIter.hasNext()) {
            val entry = playerIter.next()
            val map = entry.value
            val iter = map.entries.iterator()
            while (iter.hasNext()) {
                val e = iter.next()
                if (e.value <= 1) {
                    iter.remove()
                    expired += entry.key to e.key
                } else {
                    e.setValue(e.value - 1)
                }
            }
            if (map.isEmpty()) playerIter.remove()
        }

        for ((uuid, effectId) in expired) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            Bukkit.getPluginManager().callEvent(StatusEffectExpireEvent(player, effectId))
        }
    }
}
