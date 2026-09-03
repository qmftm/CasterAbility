package me.qmftm.casterability.skript.expression

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import me.qmftm.casterability.event.StatusEffectApplyEvent
import me.qmftm.casterability.game.PlayerStateManager
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 상태이상 정보를 읽는 Expression들.
 *
 * 상태이상 시간은 전부 tick 단위입니다. (20 tick = 1초)
 *
 * 사용 예:
 *   remaining ticks of status effect "stun" for player  → 48
 *   status effects of player                            → "stun", "bleed"
 *   status effect duration                              → apply 이벤트 안에서만. set 가능
 */

// ── 남은 시간 ─────────────────────────────────────────────

class ExprStatusEffectTime : SimpleExpression<Number>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprStatusEffectTime::class.java, Number::class.java,
                ExpressionType.COMBINED,
                "[the] remaining tick[s] of status effect %string% (for|of) %player%",
                "%player%'s remaining tick[s] of status effect %string%"
            )
        }
    }

    private lateinit var effectExpr: Expression<String>
    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        if (i == 0) {
            effectExpr = exprs[0] as Expression<String>
            playerExpr = exprs[1] as Expression<Player>
        } else {
            playerExpr = exprs[0] as Expression<Player>
            effectExpr = exprs[1] as Expression<String>
        }
        return true
    }

    override fun get(event: Event): Array<Number?> {
        val id = effectExpr.getSingle(event) ?: return arrayOfNulls(1)
        val p  = playerExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(PlayerStateManager.getEffect(p, id))
    }

    override fun isSingle() = true
    override fun getReturnType(): Class<out Number> = Number::class.java
    override fun toString(e: Event?, d: Boolean) =
        "remaining ticks of status effect ${effectExpr.toString(e, d)} for ${playerExpr.toString(e, d)}"
}

// ── 걸려 있는 상태이상 목록 ───────────────────────────────

class ExprStatusEffectsOf : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprStatusEffectsOf::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[all] status effects of %player%",
                "%player%'s status effects"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr = exprs[0] as Expression<Player>
        return true
    }

    override fun get(event: Event): Array<String?> {
        val p = playerExpr.getSingle(event) ?: return arrayOfNulls(0)
        return PlayerStateManager.getAllEffects(p).keys.toTypedArray()
    }

    override fun isSingle() = false
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "status effects of ${playerExpr.toString(e, d)}"
}

// ── apply 이벤트 안에서 걸릴 시간 ─────────────────────────

/**
 * `on status effect apply` 안에서 걸릴 시간(tick)을 읽고 바꾼다.
 *
 * on status effect apply "stun":
 *     if player has ability class "menhera":
 *         set status effect duration to status effect duration / 2
 */
class ExprStatusEffectDuration : SimpleExpression<Number>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprStatusEffectDuration::class.java, Number::class.java,
                ExpressionType.SIMPLE,
                "[the] status effect duration"
            )
        }
    }

    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult) = true

    override fun get(event: Event): Array<Number?> {
        val e = event as? StatusEffectApplyEvent ?: return arrayOfNulls(1)
        return arrayOf(e.durationTicks)
    }

    override fun acceptChange(mode: Changer.ChangeMode): Array<Class<*>>? =
        if (mode == Changer.ChangeMode.SET) arrayOf(Number::class.java) else null

    override fun change(event: Event, delta: Array<out Any>?, mode: Changer.ChangeMode) {
        if (mode != Changer.ChangeMode.SET) return
        val e = event as? StatusEffectApplyEvent ?: return
        val value = (delta?.firstOrNull() as? Number)?.toDouble() ?: return
        e.durationTicks = Math.round(value).toInt().coerceAtLeast(0)
    }

    override fun isSingle() = true
    override fun getReturnType(): Class<out Number> = Number::class.java
    override fun toString(e: Event?, d: Boolean) = "status effect duration"
}
