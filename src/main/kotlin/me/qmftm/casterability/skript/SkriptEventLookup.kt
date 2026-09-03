package me.qmftm.casterability.skript

import ch.njol.skript.Skript
import ch.njol.skript.patterns.PatternCompiler
import org.bukkit.event.Event

/**
 * 스크립트에 쓰는 `on ~~~:` 이벤트 이름을 그대로 받아서
 * 실제 Bukkit 이벤트 클래스로 바꿉니다.
 *
 * Skript가 자기 이벤트들을 등록해 둔 목록을 그대로 뒤지기 때문에,
 * `on damage:` 라고 쓸 수 있으면 `event: damage` 도 됩니다.
 * 애드온이 추가한 이벤트도 같이 잡힙니다.
 *
 * 이름만 보고 이벤트 종류를 고르는 것이라,
 * `on right click with a stick` 처럼 뒤에 붙는 조건은 무시됩니다.
 * 그런 조건은 스크립트 안에서 직접 확인하세요.
 */
object SkriptEventLookup {

    sealed interface Result {
        /** @param label Skript가 쓰는 이벤트 이름 (오류 메시지용) */
        data class Found(val label: String, val classes: List<Class<out Event>>) : Result
        data class NotFound(val suggestions: List<String>) : Result
    }

    private val typePlaceholder = Regex("%[^%]*%")
    private val spaces = Regex("\\s+")

    fun resolve(raw: String): Result {
        val input = normalize(raw)
        if (input.isEmpty()) return Result.NotFound(emptyList())

        val events = Skript.getEvents()

        // 1) Skript가 붙여둔 이벤트 이름과 그대로 같은 경우 ("Damage", "Join" …)
        events.firstOrNull { it.name.lowercase() == input }
            ?.let { return found(it.name, it.events) }

        // 2) 실제 이벤트 패턴에 맞춰보기 ("damag(e|ing)" ← "damaging")
        for (info in events) {
            for (pattern in info.patterns) {
                if (matches(pattern, input)) return found(info.name, info.events)
            }
        }

        // 3) 패턴 앞의 %타입% 을 떼고 다시 맞춰보기
        //    ("%entitydata% (move|walk|step)[ing]" ← "move")
        for (info in events) {
            for (pattern in info.patterns) {
                val stripped = stripTypes(pattern)
                if (stripped.isNotEmpty() && matches(stripped, input)) return found(info.name, info.events)
            }
        }

        return Result.NotFound(suggest(input))
    }

    // ── 내부 ──────────────────────────────────────────────

    private fun normalize(raw: String): String =
        raw.trim()
            .removeSurrounding("\"")
            .trim()
            .removeSuffix(":")
            .trim()
            .lowercase()
            .removePrefix("on ")
            .trim()
            .replace(spaces, " ")

    private fun found(name: String, classes: Array<Class<out Event>>?): Result {
        val list = classes?.filterNotNull().orEmpty()
        return if (list.isEmpty()) Result.NotFound(emptyList()) else Result.Found(name, list)
    }

    /** 패턴 하나에 문자열이 맞는지. Skript가 아직 덜 뜬 상태면 그냥 안 맞는 것으로 친다. */
    private fun matches(pattern: String, input: String): Boolean = try {
        PatternCompiler.compile(pattern).match(input) != null
    } catch (_: Throwable) {
        false
    }

    private fun stripTypes(pattern: String): String =
        pattern.replace(typePlaceholder, " ").replace(spaces, " ").trim()

    /** 못 찾았을 때 비슷해 보이는 이름 몇 개 */
    private fun suggest(input: String): List<String> {
        val word = input.substringBefore(' ')
        return Skript.getEvents()
            .map { it.name }
            .filter { it.lowercase().contains(word) }
            .distinct()
            .sorted()
            .take(8)
    }
}
