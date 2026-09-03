package me.qmftm.casterability.skript.section

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityDefinition
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.ability.AbilityTrigger
import me.qmftm.casterability.skript.SkriptEventLookup
import org.bukkit.Material
import org.bukkit.event.Event

/**
 * Skript DSL:
 *
 * on load:
 *     ability "jealousy":
 *         class: "yandere"
 *         trigger: right_click
 *         item: blaze_rod
 *         cooldown: 45
 *         passive_interval: 20    ← trigger가 passive일 때만 의미 있음
 *         event: damage           ← trigger가 passive일 때만. 주기 대신 이 이벤트로 발동
 *
 * trigger 종류: right_click, left_click, passive, on_hit, on_damaged, on_kill, on_death
 */
class SecAbility : Section() {

    companion object {
        fun register() {
            Skript.registerSection(SecAbility::class.java, "ability %string%")
        }
    }

    private lateinit var abilityId: Expression<String>

    private var classId: String = ""
    private var name: String? = null
    private var trigger: AbilityTrigger = AbilityTrigger.RIGHT_CLICK
    private var item: Material? = null
    private var cooldown: Int = 0
    private var passiveInterval: Long = 20L
    private var eventName: String? = null
    private var eventClasses: List<Class<out Event>> = emptyList()

    @Suppress("UNCHECKED_CAST")
    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode,
        triggerItems: MutableList<TriggerItem>,
    ): Boolean {
        abilityId = exprs[0] as Expression<String>

        val entries = sectionNode.readEntries()

        classId = entries["class"] ?: ""
        if (classId.isEmpty()) {
            Skript.error("ability 블록에 'class:' 가 없습니다.")
            return false
        }

        name = entries["name"]?.takeIf { it.isNotBlank() }

        entries["trigger"]?.let { raw ->
            trigger = AbilityTrigger.fromString(raw) ?: run {
                Skript.error(
                    "알 수 없는 trigger: '$raw' — " +
                    AbilityTrigger.entries.joinToString(", ") { it.skriptName } + " 중 하나여야 합니다."
                )
                return false
            }
        }

        entries["item"]?.let { raw ->
            val matName = raw.uppercase().replace(' ', '_')
            item = Material.matchMaterial(matName)
            if (item == null) {
                Skript.error("알 수 없는 아이템: '$raw'")
                return false
            }
        }

        entries["cooldown"]?.let { raw ->
            val parsed = raw.removeSuffix("s").trim().toIntOrNull()
            if (parsed == null || parsed < 0) {
                Skript.error("cooldown은 0 이상의 숫자(초)여야 합니다: '$raw'")
                return false
            }
            cooldown = parsed
        }

        entries["passive_interval"]?.let { raw ->
            val parsed = raw.toLongOrNull()
            if (parsed == null || parsed < 1) {
                Skript.error("passive_interval은 1 이상의 숫자(tick)여야 합니다: '$raw'")
                return false
            }
            passiveInterval = parsed
        }

        entries["event"]?.let { raw ->
            if (trigger != AbilityTrigger.PASSIVE) {
                Skript.error("event: 는 trigger가 passive일 때만 쓸 수 있습니다. (지금은 ${trigger.skriptName})")
                return false
            }
            when (val result = SkriptEventLookup.resolve(raw)) {
                is SkriptEventLookup.Result.Found -> {
                    eventName = result.label
                    eventClasses = result.classes
                }
                is SkriptEventLookup.Result.NotFound -> {
                    val hint = if (result.suggestions.isEmpty()) ""
                               else " 혹시 이건가요: " + result.suggestions.joinToString(", ")
                    Skript.error(
                        "알 수 없는 event: '$raw' — 스크립트에서 'on ~~~:' 로 쓸 수 있는 " +
                        "이벤트 이름을 그대로 적으세요.$hint"
                    )
                    return false
                }
            }
        }

        return true
    }

    override fun walk(event: Event): TriggerItem? {
        val id = abilityId.getSingle(event)
        if (id != null) {
            if (AbilityRegistry.getClass(classId) == null) {
                Skript.warning("ability '$id' 가 등록되지 않은 class '$classId' 를 참조합니다. " +
                    "같은 on load 블록 안에서 ability class를 먼저 정의했는지 확인하세요.")
            }
            AbilityRegistry.registerAbility(
                AbilityDefinition(
                    id = id,
                    classId = classId,
                    name = name,
                    trigger = trigger,
                    item = item,
                    cooldownSeconds = cooldown,
                    passiveIntervalTicks = passiveInterval,
                    eventName = eventName,
                    eventClasses = eventClasses,
                )
            )
        }
        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "ability ${abilityId.toString(event, debug)}"
}
