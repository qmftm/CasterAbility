package me.qmftm.casterability.skript

import ch.njol.skript.Skript
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import me.qmftm.casterability.event.StatusEffectApplyEvent
import me.qmftm.casterability.event.StatusEffectExpireEvent
import org.bukkit.event.Event

/**
 * Skript DSL:
 *
 * on status effect apply:              ← 아무 상태이상이나
 * on status effect apply "stun":       ← 특정 상태이상만
 *     if player has status effect "immune":
 *         cancel event
 *
 * on status effect expire "stun":
 *     send "&a정신이 든다." to player
 *
 * 이벤트 안에서 쓸 수 있는 값:
 *   player                   대상 플레이어
 *   event-string             상태이상 id
 *   status effect duration   걸릴 시간(초). apply에서는 바꿀 수도 있다
 */

// ── 걸릴 때 ───────────────────────────────────────────────

class EvtStatusEffectApply : SkriptEvent() {

    companion object {
        fun register() {
            Skript.registerEvent(
                "Status Effect Apply",
                EvtStatusEffectApply::class.java,
                StatusEffectApplyEvent::class.java,
                "status effect apply [%-string%]",
                "status effect applied [%-string%]"
            )
        }
    }

    private var effectId: Literal<String>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        effectId = args.getOrNull(0) as? Literal<String>
        return true
    }

    override fun check(event: Event): Boolean {
        if (event !is StatusEffectApplyEvent) return false
        val wanted = effectId?.getSingle(event) ?: return true // 이름을 안 적었으면 전부 받는다
        return event.effectId == wanted
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "status effect apply ${effectId?.toString(event, debug) ?: ""}"
}

// ── 시간이 다 되어 풀릴 때 ────────────────────────────────

class EvtStatusEffectExpire : SkriptEvent() {

    companion object {
        fun register() {
            Skript.registerEvent(
                "Status Effect Expire",
                EvtStatusEffectExpire::class.java,
                StatusEffectExpireEvent::class.java,
                "status effect expire [%-string%]",
                "status effect expired [%-string%]"
            )
        }
    }

    private var effectId: Literal<String>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        effectId = args.getOrNull(0) as? Literal<String>
        return true
    }

    override fun check(event: Event): Boolean {
        if (event !is StatusEffectExpireEvent) return false
        val wanted = effectId?.getSingle(event) ?: return true
        return event.effectId == wanted
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "status effect expire ${effectId?.toString(event, debug) ?: ""}"
}
