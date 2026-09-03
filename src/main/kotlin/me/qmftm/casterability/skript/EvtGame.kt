package me.qmftm.casterability.skript

import ch.njol.skript.Skript
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import me.qmftm.casterability.event.GameEndEvent
import me.qmftm.casterability.event.GameStartEvent
import org.bukkit.event.Event

/**
 * Skript DSL:
 *
 * on game start:
 *     broadcast "&a시작!"
 *
 * on game end:
 *     loop all players:
 *         if loop-player has ability class "pig":
 *             set loop-player's walk speed to 0.2
 *
 * game end 는 능력·쿨타임·상태이상이 지워지기 전에 발생합니다.
 * 그래서 이 안에서는 아직 누가 어떤 능력이었는지 볼 수 있습니다.
 */

class EvtGameStart : SkriptEvent() {

    companion object {
        fun register() {
            Skript.registerEvent(
                "Game Start",
                EvtGameStart::class.java,
                GameStartEvent::class.java,
                "[caster[ ]ability] game start[ed]"
            )
        }
    }

    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean = true

    override fun check(event: Event): Boolean = event is GameStartEvent

    override fun toString(event: Event?, debug: Boolean): String = "game start"
}

class EvtGameEnd : SkriptEvent() {

    companion object {
        fun register() {
            Skript.registerEvent(
                "Game End",
                EvtGameEnd::class.java,
                GameEndEvent::class.java,
                "[caster[ ]ability] game end[ed]",
                "[caster[ ]ability] game stop[ped]"
            )
        }
    }

    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean = true

    override fun check(event: Event): Boolean = event is GameEndEvent

    override fun toString(event: Event?, debug: Boolean): String = "game end"
}
