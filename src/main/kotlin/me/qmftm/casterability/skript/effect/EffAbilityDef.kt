package me.qmftm.casterability.skript.effect

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.log.SkriptLogger
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityDefinition
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.ability.AbilityTrigger
import org.bukkit.Material
import org.bukkit.event.Event

/**
 * Skript DSL:
 *
 * ability "love":
 *     class: "menhera"
 *     trigger: right_click
 *     item: blaze_rod
 *     cooldown: 30
 *     passive_interval: 20    ← passive 트리거일 때 tick 간격 (기본 20)
 */
class EffAbilityDef : Effect() {

    companion object {
        fun register() {
            Skript.registerEffect(
                EffAbilityDef::class.java,
                "ability %string%"
            )
        }
    }

    private lateinit var abilityId: Expression<String>

    private var classId: String = ""
    private var trigger: AbilityTrigger = AbilityTrigger.RIGHT_CLICK
    private var item: Material? = null
    private var cooldown: Int = 0
    private var passiveInterval: Long = 20L

    @Suppress("UNCHECKED_CAST")
    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        abilityId = exprs[0] as Expression<String>

        val node = SkriptLogger.getNode() as? SectionNode ?: return true
        for (subNode in node) {
            val key = subNode.key?.trim() ?: continue
            when {
                key.startsWith("class:") -> {
                    classId = key.removePrefix("class:").trim().trim('"')
                }
                key.startsWith("trigger:") -> {
                    val triggerStr = key.removePrefix("trigger:").trim()
                    trigger = AbilityTrigger.fromString(triggerStr)
                        ?: run {
                            Skript.error("알 수 없는 trigger 타입: '$triggerStr'")
                            return false
                        }
                }
                key.startsWith("item:") -> {
                    val matStr = key.removePrefix("item:").trim()
                        .uppercase().replace(" ", "_")
                    item = Material.matchMaterial(matStr) ?: run {
                        Skript.warning("알 수 없는 아이템: '$matStr' — 무시됩니다")
                        null
                    }
                }
                key.startsWith("cooldown:") -> {
                    cooldown = key.removePrefix("cooldown:").trim()
                        .removeSuffix("s").trim().toIntOrNull() ?: 0
                }
                key.startsWith("passive_interval:") -> {
                    passiveInterval = key.removePrefix("passive_interval:").trim()
                        .toLongOrNull() ?: 20L
                }
            }
        }

        if (classId.isEmpty()) {
            Skript.error("ability 블록에 'class:' 가 없습니다.")
            return false
        }
        return true
    }

    override fun execute(event: Event) {
        val id = abilityId.getSingle(event) ?: return
        val def = AbilityDefinition(
            id = id,
            classId = classId,
            trigger = trigger,
            item = item,
            cooldownSeconds = cooldown,
            passiveIntervalTicks = passiveInterval,
        )
        AbilityRegistry.registerAbility(def)
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "ability ${abilityId.toString(event, debug)}"
}
