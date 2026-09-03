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
 * 능력 하나하나의 상태를 보는 조건들.
 *
 * 사용 예:
 *   if player has ability "love":
 *   if ability "love" is disabled for player:
 */

// ── 특정 ability를 가지고 있는가 ──────────────────────────

class CondHasAbilityId : Condition() {
    companion object {
        fun register() {
            Skript.registerCondition(
                CondHasAbilityId::class.java,
                "%player% has ability %string%",
                "%player% doesn't have ability %string%"
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
        val result = AbilityRegistry.ownsAbility(p, id)
        return if (isNegated) !result else result
    }

    override fun toString(e: Event?, d: Boolean) =
        "${playerExpr.toString(e, d)} ${if (isNegated) "doesn't have" else "has"} ability ${abilityExpr.toString(e, d)}"
}

// ── 꺼져 있는가 ───────────────────────────────────────────

class CondAbilityDisabled : Condition() {
    companion object {
        fun register() {
            Skript.registerCondition(
                CondAbilityDisabled::class.java,
                "[ability] %string% is disabled for %player%",
                "[ability] %string% is enabled for %player%"
            )
        }
    }

    private lateinit var abilityExpr: Expression<String>
    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        abilityExpr = exprs[0] as Expression<String>
        playerExpr  = exprs[1] as Expression<Player>
        setNegated(i == 1)
        return true
    }

    override fun check(event: Event): Boolean {
        val id = abilityExpr.getSingle(event) ?: return false
        val p  = playerExpr.getSingle(event)  ?: return false
        val result = AbilityRegistry.isAbilityDisabled(p, id)
        return if (isNegated) !result else result
    }

    override fun toString(e: Event?, d: Boolean) =
        "ability ${abilityExpr.toString(e, d)} is ${if (isNegated) "enabled" else "disabled"} for ${playerExpr.toString(e, d)}"
}
