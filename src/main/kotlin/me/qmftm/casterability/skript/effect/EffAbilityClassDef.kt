package me.qmftm.casterability.skript.effect

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.log.SkriptLogger
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityClass
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.event.Event

/**
 * Skript DSL:
 *
 * ability class "menhera":
 *     name: "멘헤라"
 *     tier: 3
 *
 * SectionNode를 파싱해서 AbilityClass를 AbilityRegistry에 등록합니다.
 */
class EffAbilityClassDef : Effect() {

    companion object {
        fun register() {
            Skript.registerEffect(
                EffAbilityClassDef::class.java,
                "ability class %string%"
            )
        }
    }

    private lateinit var classId: Expression<String>

    // SectionNode에서 읽어온 값들
    private var displayName: String = ""
    private var tier: Int = 4

    @Suppress("UNCHECKED_CAST")
    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        classId = exprs[0] as Expression<String>

        // SectionNode 파싱
        val node = SkriptLogger.getNode() as? SectionNode ?: return true
        for (subNode in node) {
            val key = subNode.key?.trim() ?: continue
            when {
                key.startsWith("name:") -> {
                    displayName = key.removePrefix("name:").trim().trim('"')
                }
                key.startsWith("tier:") -> {
                    tier = key.removePrefix("tier:").trim().toIntOrNull() ?: 4
                }
            }
        }
        return true
    }

    override fun execute(event: Event) {
        val id = classId.getSingle(event) ?: return
        val cls = AbilityClass(
            id = id,
            name = displayName.ifEmpty { id },
            tier = tier,
        )
        AbilityRegistry.registerClass(cls)
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "ability class ${classId.toString(event, debug)}"
}
