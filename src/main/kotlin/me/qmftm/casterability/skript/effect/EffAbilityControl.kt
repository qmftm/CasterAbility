package me.qmftm.casterability.skript.effect

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 능력 조작 Effect들
 *
 * 사용 예:
 *   set ability class of player to "menhera"
 *   remove ability from player
 *   set cooldown of player for ability "love" to 30
 *   reset cooldown of player for ability "love"
 */

// ── set ability class ─────────────────────────────────────

class EffSetAbilityClass : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffSetAbilityClass::class.java,
                "set ability class of %player% to %string%",
                "give %player% ability class %string%"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>
    private lateinit var classExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr = exprs[0] as Expression<Player>
        classExpr  = exprs[1] as Expression<String>
        return true
    }

    override fun execute(event: Event) {
        val p  = playerExpr.getSingle(event)  ?: return
        val id = classExpr.getSingle(event)   ?: return
        if (AbilityRegistry.getClass(id) == null) {
            Skript.warning("ability class '$id' 가 등록되지 않았습니다.")
            return
        }
        AbilityRegistry.setPlayerClass(p, id)
    }

    override fun toString(e: Event?, d: Boolean) =
        "set ability class of ${playerExpr.toString(e, d)} to ${classExpr.toString(e, d)}"
}

// ── remove ability ────────────────────────────────────────

class EffRemoveAbility : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffRemoveAbility::class.java,
                "remove ability [class] from %player%",
                "clear ability [class] of %player%"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr = exprs[0] as Expression<Player>
        return true
    }

    override fun execute(event: Event) {
        val p = playerExpr.getSingle(event) ?: return
        AbilityRegistry.clearPlayerClass(p)
    }

    override fun toString(e: Event?, d: Boolean) =
        "remove ability from ${playerExpr.toString(e, d)}"
}

// ── set cooldown ──────────────────────────────────────────

class EffSetCooldown : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffSetCooldown::class.java,
                "set [ability] cooldown of %player% for [ability] %string% to %number%"
            )
        }
    }

    private lateinit var playerExpr:  Expression<Player>
    private lateinit var abilityExpr: Expression<String>
    private lateinit var secondsExpr: Expression<Number>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr  = exprs[0] as Expression<Player>
        abilityExpr = exprs[1] as Expression<String>
        secondsExpr = exprs[2] as Expression<Number>
        return true
    }

    override fun execute(event: Event) {
        val p       = playerExpr.getSingle(event)  ?: return
        val id      = abilityExpr.getSingle(event) ?: return
        val seconds = secondsExpr.getSingle(event)?.toInt() ?: return
        AbilityRegistry.setCooldown(p, id, seconds * 20)
    }

    override fun toString(e: Event?, d: Boolean) =
        "set cooldown of ${playerExpr.toString(e, d)} for ability ${abilityExpr.toString(e, d)}"
}

// ── reset cooldown ────────────────────────────────────────

class EffResetCooldown : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffResetCooldown::class.java,
                "reset [ability] cooldown of %player% for [ability] %string%"
            )
        }
    }

    private lateinit var playerExpr:  Expression<Player>
    private lateinit var abilityExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        playerExpr  = exprs[0] as Expression<Player>
        abilityExpr = exprs[1] as Expression<String>
        return true
    }

    override fun execute(event: Event) {
        val p  = playerExpr.getSingle(event)  ?: return
        val id = abilityExpr.getSingle(event) ?: return
        AbilityRegistry.setCooldown(p, id, 0)
    }

    override fun toString(e: Event?, d: Boolean) =
        "reset cooldown of ${playerExpr.toString(e, d)} for ability ${abilityExpr.toString(e, d)}"
}
