package me.qmftm.casterability.skript.section

import ch.njol.skript.ScriptLoader
import ch.njol.skript.config.EntryNode
import ch.njol.skript.config.SectionNode

/**
 * `key: value` 형태의 자식 노드를 읽어 맵으로 돌려줍니다.
 *
 * Skript가 스크립트 파일을 어떤 모드로 읽었느냐에 따라 자식 노드가
 * EntryNode(키/값이 분리됨)일 수도, SimpleNode(줄 전체가 key)일 수도 있어서
 * 두 경우를 모두 처리합니다.
 *
 * 키는 소문자로 정규화하고, 값은 양쪽 큰따옴표를 벗겨서 돌려줍니다.
 */
internal fun SectionNode.readEntries(): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    for (node in this) {
        if (node is EntryNode) {
            val k = node.key?.trim() ?: continue
            out[k.lowercase()] = node.value.trim().withOptions().unquote()
            continue
        }
        val raw = node.key?.trim() ?: continue
        val idx = raw.indexOf(':')
        if (idx <= 0) continue
        out[raw.substring(0, idx).trim().lowercase()] = raw.substring(idx + 1).trim().withOptions().unquote()
    }
    return out
}

/**
 * 값 대신 여러 줄을 갖는 항목을 읽습니다.
 *
 *     description:
 *         "&f첫 줄"
 *         "&f둘째 줄"
 *
 * 해당 이름의 하위 블록이 없으면 빈 목록을 돌려줍니다.
 */
internal fun SectionNode.readLines(key: String): List<String> {
    val target = this.firstOrNull { it is SectionNode && it.key?.trim()?.trimEnd(':')?.lowercase() == key }
        as? SectionNode ?: return emptyList()
    return target.mapNotNull { it.key?.trim()?.withOptions()?.unquote() }
}

private fun String.unquote(): String =
    if (length >= 2 && startsWith('"') && endsWith('"')) substring(1, length - 1) else this

/**
 * `{@name}` 옵션 치환. Skript는 이 치환을 파서가 노드를 실제로 파싱할 때 수행하고
 * (ScriptLoader.replaceOptions 호출), 순수 텍스트인 node.key/value 자체에는 적용해두지
 * 않는다. 그래서 우리처럼 SectionNode를 직접 텍스트로 읽는 코드는 이걸 스스로 불러줘야
 * "{@option}"이 실제 옵션 값으로 바뀐다.
 */
private fun String.withOptions(): String = ScriptLoader.replaceOptions(this)
