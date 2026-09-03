package me.qmftm.casterability.skript.effect

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import me.qmftm.casterability.game.PlayerStateManager
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 액션바 가운데 칸(능력 부가 정보)에 글을 올리고 내립니다.
 *
 * 칸을 id로 나눠 쓰기 때문에 능력 여러 개가 동시에 글을 올려도
 * 서로 덮어쓰지 않고 나란히 표시됩니다.
 *
 * 사용 예:
 *   set ability info "charge" of player to "&e충전 3/5"
 *   clear ability info "charge" of player
 *   clear all ability info of player
 */

class EffSetAbilityInfo : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffSetAbilityInfo::class.java,
                "set ability info %string% (of|for) %player% to %string%"
            )
        }
    }

    private lateinit var idExpr: Expression<String>
    private lateinit var playerExpr: Expression<Player>
    private lateinit var textExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        idExpr     = exprs[0] as Expression<String>
        playerExpr = exprs[1] as Expression<Player>
        textExpr   = exprs[2] as Expression<String>
        return true
    }

    override fun execute(event: Event) {
        val id   = idExpr.getSingle(event)   ?: return
        val text = textExpr.getSingle(event) ?: return
        for (p in playerExpr.getArray(event)) {
            // 빈 문자열을 넣으면 구분선만 남으므로 지우는 것으로 친다
            if (text.isEmpty()) PlayerStateManager.removeCustomUi(p, id)
            else                PlayerStateManager.setCustomUi(p, id, text)
        }
    }

    override fun toString(e: Event?, d: Boolean) =
        "set ability info ${idExpr.toString(e, d)} of ${playerExpr.toString(e, d)}"
}

class EffClearAbilityInfo : Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(
                EffClearAbilityInfo::class.java,
                "clear ability info %string% (of|for) %player%",
                "clear all ability info (of|for) %player%"
            )
        }
    }

    private var idExpr: Expression<String>? = null
    private lateinit var playerExpr: Expression<Player>
    private var clearAll = false

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        clearAll = i == 1
        if (clearAll) {
            playerExpr = exprs[0] as Expression<Player>
        } else {
            idExpr     = exprs[0] as Expression<String>
            playerExpr = exprs[1] as Expression<Player>
        }
        return true
    }

    override fun execute(event: Event) {
        for (p in playerExpr.getArray(event)) {
            if (clearAll) {
                PlayerStateManager.clearCustomUi(p)
            } else {
                val id = idExpr?.getSingle(event) ?: return
                PlayerStateManager.removeCustomUi(p, id)
            }
        }
    }

    override fun toString(e: Event?, d: Boolean) =
        if (clearAll) "clear all ability info of ${playerExpr.toString(e, d)}"
        else "clear ability info of ${playerExpr.toString(e, d)}"
}
