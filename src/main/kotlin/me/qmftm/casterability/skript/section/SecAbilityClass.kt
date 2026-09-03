package me.qmftm.casterability.skript.section

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityClass
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.event.Event

/**
 * Skript DSL:
 *
 * on load:
 *     ability class "yandere":
 *         name: "얀데레"
 *         tier: 2
 *         description:
 *             "&d[&7패시브 &f- &d처음 본 순간]"
 *             "&f처음 공격한 플레이어를 &c집착 대상&f으로 지정합니다."
 *
 * tier: 0=Legendary 1=S 2=A 3=B 4=C
 *
 * 들여쓴 블록을 가지므로 Effect가 아니라 Section으로 등록해야 합니다.
 * Skript는 SectionNode를 Effect로 파싱하지 않습니다.
 */
class SecAbilityClass : Section() {

    companion object {
        fun register() {
            Skript.registerSection(SecAbilityClass::class.java, "ability class %string%")
        }
    }

    private lateinit var classId: Expression<String>
    private var displayName: String = ""
    private var tier: Int = 4
    private var description: List<String> = emptyList()

    @Suppress("UNCHECKED_CAST")
    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode,
        triggerItems: MutableList<TriggerItem>,
    ): Boolean {
        classId = exprs[0] as Expression<String>

        // 블록 안은 설정 항목이므로 코드로 파싱하지 않는다 (loadCode 호출 안 함)
        val entries = sectionNode.readEntries()
        displayName = entries["name"] ?: ""

        val tierRaw = entries["tier"]
        if (tierRaw != null) {
            val parsed = tierRaw.toIntOrNull()
            if (parsed == null) {
                Skript.error("ability class의 tier는 숫자여야 합니다: '$tierRaw'")
                return false
            }
            if (parsed !in 0..4) {
                Skript.error("ability class의 tier는 0~4 사이여야 합니다 (받은 값: $parsed)")
                return false
            }
            tier = parsed
        }

        description = sectionNode.readLines("description")
        return true
    }

    override fun walk(event: Event): TriggerItem? {
        val id = classId.getSingle(event)
        if (id != null) {
            AbilityRegistry.registerClass(
                AbilityClass(
                    id = id,
                    name = displayName.ifEmpty { id },
                    tier = tier,
                    description = description,
                )
            )
        }
        // 블록 내용은 실행하지 않고 다음 줄로 넘어간다
        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "ability class ${classId.toString(event, debug)}"
}
