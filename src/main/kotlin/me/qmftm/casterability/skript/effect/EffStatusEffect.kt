package me.qmftm.casterability.skript.effect

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import me.qmftm.casterability.game.PlayerStateManager
import me.qmftm.casterability.game.StatusEffectMode
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 상태이상을 걸고 푸는 Effect들.
 *
 * 사용 예:
 *   apply status effect "stun" to event-entity for 3 seconds
 *   stack status effect "bleed" on event-entity for 2 seconds
 *   refresh status effect "frost" on player for 5 seconds
 *   apply status effect "mark" to player for 10 seconds if not active
 *   remove status effect "stun" from player
 *   clear all status effects from player
 *   set the name of status effect "stun" to "&c기절"
 */

// ── 걸기 ──────────────────────────────────────────────────

class EffApplyStatusEffect : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffApplyStatusEffect::class.java,
                // 0: 더 긴 쪽만 남긴다 (기본)
                "apply status effect %string% to %player% for %number% [second[s]]",
                // 1: 남은 시간에 더한다
                "stack status effect %string% on %player% for %number% [second[s]]",
                // 2: 무조건 새 값으로 덮어쓴다
                "refresh status effect %string% on %player% for %number% [second[s]]",
                // 3: 이미 걸려 있으면 아무것도 하지 않는다
                "apply status effect %string% to %player% for %number% [second[s]] if not active"
            )
        }
    }

    private lateinit var effectExpr: Expression<String>
    private lateinit var playerExpr: Expression<Player>
    private lateinit var secondsExpr: Expression<Number>
    private var mode = StatusEffectMode.LONGEST

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        effectExpr  = exprs[0] as Expression<String>
        playerExpr  = exprs[1] as Expression<Player>
        secondsExpr = exprs[2] as Expression<Number>
        mode = when (i) {
            1    -> StatusEffectMode.STACK
            2    -> StatusEffectMode.REPLACE
            3    -> StatusEffectMode.IGNORE
            else -> StatusEffectMode.LONGEST
        }
        return true
    }

    override fun execute(event: Event) {
        val id      = effectExpr.getSingle(event)  ?: return
        val seconds = secondsExpr.getSingle(event)?.toDouble() ?: return
        // 여러 명에게 한 번에 걸 수 있게 한다 — loop 없이 all players 같은 것에 바로 쓴다
        for (p in playerExpr.getArray(event)) {
            PlayerStateManager.applyEffect(p, id, seconds, mode)
        }
    }

    override fun toString(e: Event?, d: Boolean) =
        "apply status effect ${effectExpr.toString(e, d)} to ${playerExpr.toString(e, d)}"
}

// ── 풀기 ──────────────────────────────────────────────────

class EffRemoveStatusEffect : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffRemoveStatusEffect::class.java,
                "remove status effect %string% from %player%",
                "clear [all] status effects (from|of) %player%"
            )
        }
    }

    private var effectExpr: Expression<String>? = null
    private lateinit var playerExpr: Expression<Player>
    private var clearAll = false

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        clearAll = i == 1
        if (clearAll) {
            playerExpr = exprs[0] as Expression<Player>
        } else {
            effectExpr = exprs[0] as Expression<String>
            playerExpr = exprs[1] as Expression<Player>
        }
        return true
    }

    override fun execute(event: Event) {
        for (p in playerExpr.getArray(event)) {
            if (clearAll) {
                PlayerStateManager.clearEffects(p)
            } else {
                val id = effectExpr?.getSingle(event) ?: return
                PlayerStateManager.deleteEffect(p, id)
            }
        }
    }

    override fun toString(e: Event?, d: Boolean) =
        if (clearAll) "clear all status effects from ${playerExpr.toString(e, d)}"
        else "remove status effect from ${playerExpr.toString(e, d)}"
}

// ── 액션바 표시 이름 ──────────────────────────────────────

class EffStatusEffectName : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffStatusEffectName::class.java,
                "set [the] name of status effect %string% to %string%"
            )
        }
    }

    private lateinit var effectExpr: Expression<String>
    private lateinit var nameExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        effectExpr = exprs[0] as Expression<String>
        nameExpr   = exprs[1] as Expression<String>
        return true
    }

    override fun execute(event: Event) {
        val id   = effectExpr.getSingle(event) ?: return
        val name = nameExpr.getSingle(event)   ?: return
        PlayerStateManager.registerEffect(id, name)
    }

    override fun toString(e: Event?, d: Boolean) =
        "set the name of status effect ${effectExpr.toString(e, d)} to ${nameExpr.toString(e, d)}"
}
