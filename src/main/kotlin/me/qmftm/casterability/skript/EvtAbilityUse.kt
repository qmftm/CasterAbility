package me.qmftm.casterability.skript

import ch.njol.skript.Skript
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import me.qmftm.casterability.event.AbilityUseEvent
import org.bukkit.event.Event

/**
 * Skript DSL:
 *
 * on ability use "love":
 *     send "사용!" to player
 *     damage victim by 5
 *
 * on ability use "love" by player:   ← 특정 플레이어 조건 (추후 확장)
 *
 * 이 이벤트 안에서 사용할 수 있는 값:
 *   - player          → 능력을 사용한 플레이어
 *   - ability id      → 능력 ID (문자열)
 *   - event-entity    → 대상 엔티티 (on_hit / on_damaged 트리거 시)
 *   - event-damage    → 피해량 (on_hit / on_damaged 트리거 시)
 */
class EvtAbilityUse : SkriptEvent() {

    companion object {
        fun register() {
            Skript.registerEvent(
                "Ability Use",
                EvtAbilityUse::class.java,
                AbilityUseEvent::class.java,
                "ability use %string%",
                "ability used %string%"
            )
        }
    }

    private lateinit var abilityId: Literal<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        abilityId = args[0] as Literal<String>
        return true
    }

    override fun check(event: Event): Boolean {
        if (event !is AbilityUseEvent) return false
        val id = abilityId.getSingle(event) ?: return false
        return event.abilityId == id
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "ability use ${abilityId.toString(event, debug)}"
}
