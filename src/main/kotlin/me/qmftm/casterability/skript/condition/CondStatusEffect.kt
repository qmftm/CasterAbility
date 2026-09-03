package me.qmftm.casterability.skript.condition

import ch.njol.skript.Skript
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import me.qmftm.casterability.game.PlayerStateManager
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 사용 예:
 *   if player has status effect "stun":
 *   if event-entity doesn't have status effect "stun":
 */
class CondHasStatusEffect : Condition() {
    companion object {
        fun register() {
            Skript.registerCondition(
                CondHasStatusEffect::class.java,
                "%player% has status effect %string%",
                "%player% (doesn't|does not) have status effect %string%"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>
    private lateinit var effectExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr = exprs[0] as Expression<Player>
        effectExpr = exprs[1] as Expression<String>
        setNegated(i == 1)
        return true
    }

    override fun check(event: Event): Boolean {
        val p  = playerExpr.getSingle(event) ?: return false
        val id = effectExpr.getSingle(event) ?: return false
        val result = PlayerStateManager.hasEffect(p, id)
        return if (isNegated) !result else result
    }

    override fun toString(e: Event?, d: Boolean) =
        "${playerExpr.toString(e, d)} ${if (isNegated) "doesn't have" else "has"} status effect ${effectExpr.toString(e, d)}"
}
