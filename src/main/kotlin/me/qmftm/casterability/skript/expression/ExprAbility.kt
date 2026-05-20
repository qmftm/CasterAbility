package me.qmftm.casterability.skript.expression

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * Skript에서 플레이어의 능력 정보를 읽는 Expression들
 *
 * 사용 예:
 *   ability class of player          → "menhera"
 *   ability class name of player     → "멘헤라"
 *   ability class tier of player     → 3
 *   player's ability cooldown "love" → 남은 쿨타임 (초)
 */

// ── ability class id ──────────────────────────────────────

class ExprAbilityClassId : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityClassId::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] ability class [id] of %player%",
                "%player%'s ability class [id]"
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
        val p = playerExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getPlayerClassId(p))
    }

    override fun isSingle() = true
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "ability class of ${playerExpr.toString(e, d)}"
}

// ── ability class name ────────────────────────────────────

class ExprAbilityClassName : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityClassName::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] ability class name of %player%",
                "%player%'s ability class name"
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
        val p = playerExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getPlayerClass(p)?.name)
    }

    override fun isSingle() = true
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "ability class name of ${playerExpr.toString(e, d)}"
}

// ── ability class tier ────────────────────────────────────

class ExprAbilityClassTier : SimpleExpression<Int>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityClassTier::class.java, Int::class.javaObjectType,
                ExpressionType.COMBINED,
                "[the] ability [class] tier of %player%",
                "%player%'s ability [class] tier"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr = exprs[0] as Expression<Player>
        return true
    }

    override fun get(event: Event): Array<Int?> {
        val p = playerExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getPlayerClass(p)?.tier)
    }

    override fun isSingle() = true
    override fun getReturnType() = Int::class.javaObjectType
    override fun toString(e: Event?, d: Boolean) = "ability tier of ${playerExpr.toString(e, d)}"
}

// ── ability cooldown ──────────────────────────────────────

class ExprAbilityCooldown : SimpleExpression<Int>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityCooldown::class.java, Int::class.javaObjectType,
                ExpressionType.COMBINED,
                "[the] [ability] cooldown of %player% for [ability] %string%",
                "%player%'s [ability] cooldown for [ability] %string%"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>
    private lateinit var abilityExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr  = exprs[0] as Expression<Player>
        abilityExpr = exprs[1] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<Int?> {
        val p  = playerExpr.getSingle(event)  ?: return arrayOfNulls(1)
        val id = abilityExpr.getSingle(event) ?: return arrayOfNulls(1)
        val ticks = AbilityRegistry.getCooldown(p, id)
        return arrayOf(ticks / 20) // tick → 초
    }

    override fun isSingle() = true
    override fun getReturnType() = Int::class.javaObjectType
    override fun toString(e: Event?, d: Boolean) =
        "cooldown of ${playerExpr.toString(e, d)} for ability ${abilityExpr.toString(e, d)}"
}
