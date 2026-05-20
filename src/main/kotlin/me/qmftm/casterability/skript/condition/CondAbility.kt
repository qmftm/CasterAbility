package me.qmftm.casterability.skript.condition

import ch.njol.skript.Skript
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * Skript 조건문들
 *
 * 사용 예:
 *   if player has ability class "menhera":
 *   if player is on cooldown for ability "love":
 *   if player has no ability:
 */

// ── has ability class ─────────────────────────────────────

class CondHasAbilityClass : Condition() {
    companion object {
        fun register() {
            Skript.registerCondition(
                CondHasAbilityClass::class.java,
                "%player% has ability class %string%",
                "%player% doesn't have ability class %string%"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>
    private lateinit var classExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr = exprs[0] as Expression<Player>
        classExpr  = exprs[1] as Expression<String>
        setNegated(i == 1)
        return true
    }

    override fun check(event: Event): Boolean {
        val p  = playerExpr.getSingle(event)  ?: return false
        val id = classExpr.getSingle(event)   ?: return false
        val result = AbilityRegistry.getPlayerClassId(p) == id
        return if (isNegated) !result else result
    }

    override fun toString(e: Event?, d: Boolean) =
        "${playerExpr.toString(e, d)} ${if (isNegated) "doesn't have" else "has"} ability class ${classExpr.toString(e, d)}"
}

// ── is on cooldown ────────────────────────────────────────

class CondOnCooldown : Condition() {
    companion object {
        fun register() {
            Skript.registerCondition(
                CondOnCooldown::class.java,
                "%player% is on cooldown for [ability] %string%",
                "%player% is not on cooldown for [ability] %string%"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>
    private lateinit var abilityExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr  = exprs[0] as Expression<Player>
        abilityExpr = exprs[1] as Expression<String>
        setNegated(i == 1)
        return true
    }

    override fun check(event: Event): Boolean {
        val p  = playerExpr.getSingle(event)  ?: return false
        val id = abilityExpr.getSingle(event) ?: return false
        val result = AbilityRegistry.isOnCooldown(p, id)
        return if (isNegated) !result else result
    }

    override fun toString(e: Event?, d: Boolean) =
        "${playerExpr.toString(e, d)} ${if (isNegated) "is not" else "is"} on cooldown for ${abilityExpr.toString(e, d)}"
}

// ── has any ability ───────────────────────────────────────

class CondHasAbility : Condition() {
    companion object {
        fun register() {
            Skript.registerCondition(
                CondHasAbility::class.java,
                "%player% has [an] ability",
                "%player% has no ability"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr = exprs[0] as Expression<Player>
        setNegated(i == 1)
        return true
    }

    override fun check(event: Event): Boolean {
        val p = playerExpr.getSingle(event) ?: return false
        val result = AbilityRegistry.getPlayerClass(p) != null
        return if (isNegated) !result else result
    }

    override fun toString(e: Event?, d: Boolean) =
        "${playerExpr.toString(e, d)} ${if (isNegated) "has no" else "has an"} ability"
}
