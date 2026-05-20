package me.qmftm.casterability.skript.effect

import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser.ParseResult
import ch.njol.util.Kleenean
import org.bukkit.Bukkit
import org.bukkit.event.Event

class EffCaTest : Effect() {

    override fun init(
        expressions: Array<out Expression<*>>?,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: ParseResult
    ): Boolean {
        return true
    }

    override fun execute(event: Event) {
        Bukkit.broadcastMessage("CasterAbility test successful")
    }

    override fun toString(event: Event?, debug: Boolean): String {
        return "ca test"
    }
}