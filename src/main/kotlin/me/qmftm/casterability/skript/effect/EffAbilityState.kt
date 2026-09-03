package me.qmftm.casterability.skript.effect

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.event.AbilityUseEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 능력을 켜고 끄거나 직접 발동시키는 Effect들.
 *
 * 사용 예:
 *   disable ability "love" for player
 *   enable ability "love" for player
 *   enable all abilities for player
 *   reset all cooldowns of player
 *   force player to use ability "love"
 *   force player to use ability "love" on event-entity
 */

// ── 능력 켜고 끄기 ────────────────────────────────────────

class EffToggleAbility : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffToggleAbility::class.java,
                "disable [ability] %string% for %player%",
                "enable [ability] %string% for %player%",
                "enable all abilities for %player%"
            )
        }
    }

    private var mode = 0 // 0=disable 1=enable 2=enable all
    private var abilityExpr: Expression<String>? = null
    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        mode = i
        if (i == 2) {
            playerExpr = exprs[0] as Expression<Player>
        } else {
            abilityExpr = exprs[0] as Expression<String>
            playerExpr  = exprs[1] as Expression<Player>
        }
        return true
    }

    override fun execute(event: Event) {
        val p = playerExpr.getSingle(event) ?: return
        if (mode == 2) {
            AbilityRegistry.clearDisabled(p)
            return
        }
        val id = abilityExpr?.getSingle(event) ?: return
        AbilityRegistry.setAbilityEnabled(p, id, enabled = mode == 1)
    }

    override fun toString(e: Event?, d: Boolean) = when (mode) {
        2    -> "enable all abilities for ${playerExpr.toString(e, d)}"
        1    -> "enable ability for ${playerExpr.toString(e, d)}"
        else -> "disable ability for ${playerExpr.toString(e, d)}"
    }
}

// ── 쿨타임 전부 초기화 ────────────────────────────────────

class EffResetAllCooldowns : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffResetAllCooldowns::class.java,
                "reset all [ability] cooldowns of %player%",
                "clear all [ability] cooldowns of %player%"
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
        AbilityRegistry.clearCooldowns(p)
    }

    override fun toString(e: Event?, d: Boolean) = "reset all cooldowns of ${playerExpr.toString(e, d)}"
}

// ── 능력 강제 발동 ────────────────────────────────────────

/**
 * `on ability use` 이벤트를 직접 발생시킵니다.
 * 쿨타임은 확인하지도, 새로 걸지도 않습니다. 필요하면 스크립트에서 직접 거세요.
 */
class EffTriggerAbility : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffTriggerAbility::class.java,
                "force %player% to use [ability] %string% [on %-entity%]",
                "trigger [ability] %string% for %player% [on %-entity%]"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>
    private lateinit var abilityExpr: Expression<String>
    private var targetExpr: Expression<Entity>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        if (i == 0) {
            playerExpr  = exprs[0] as Expression<Player>
            abilityExpr = exprs[1] as Expression<String>
        } else {
            abilityExpr = exprs[0] as Expression<String>
            playerExpr  = exprs[1] as Expression<Player>
        }
        targetExpr = exprs.getOrNull(2) as? Expression<Entity>
        return true
    }

    override fun execute(event: Event) {
        val p  = playerExpr.getSingle(event)  ?: return
        val id = abilityExpr.getSingle(event) ?: return
        val def = AbilityRegistry.getAbility(id) ?: run {
            Skript.warning("ability '$id' 가 등록되지 않았습니다.")
            return
        }
        val target = targetExpr?.getSingle(event)
        Bukkit.getPluginManager().callEvent(
            AbilityUseEvent(player = p, ability = def, trigger = def.trigger, target = target)
        )
    }

    override fun toString(e: Event?, d: Boolean) =
        "force ${playerExpr.toString(e, d)} to use ability ${abilityExpr.toString(e, d)}"
}
