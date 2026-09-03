package me.qmftm.casterability.skript.expression

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import me.qmftm.casterability.event.AbilityUseEvent
import org.bukkit.event.Event

/**
 * ability use 이벤트 안에서 피해량을 읽고 바꿉니다.
 *
 * on ability use "obsession":
 *     set ability damage to ability damage * 1.25
 *
 * on_hit / on_damaged 트리거일 때만 값이 있습니다.
 * 여기서 바꾼 값은 AbilityDispatcher가 원래 이벤트에 다시 반영합니다.
 */
class ExprAbilityDamage : SimpleExpression<Number>() {

    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityDamage::class.java, Number::class.java,
                ExpressionType.SIMPLE,
                "[the] ability damage"
            )
        }
    }

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean = true

    override fun get(event: Event): Array<Number?> {
        val e = event as? AbilityUseEvent ?: return arrayOfNulls(1)
        return arrayOf(e.damage)
    }

    override fun acceptChange(mode: Changer.ChangeMode): Array<Class<*>>? =
        if (mode == Changer.ChangeMode.SET) arrayOf(Number::class.java) else null

    override fun change(event: Event, delta: Array<out Any>?, mode: Changer.ChangeMode) {
        if (mode != Changer.ChangeMode.SET) return
        val e = event as? AbilityUseEvent ?: return
        val value = (delta?.firstOrNull() as? Number)?.toDouble() ?: return
        e.damage = value.coerceAtLeast(0.0)
    }

    override fun isSingle() = true
    override fun getReturnType(): Class<out Number> = Number::class.java
    override fun toString(event: Event?, debug: Boolean) = "ability damage"
}
