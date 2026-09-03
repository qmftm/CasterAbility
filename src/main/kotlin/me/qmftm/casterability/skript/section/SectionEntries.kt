package me.qmftm.casterability.skript.section

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
            out[k.lowercase()] = node.value.trim().unquote()
            continue
        }
        val raw = node.key?.trim() ?: continue
        val idx = raw.indexOf(':')
        if (idx <= 0) continue
        out[raw.substring(0, idx).trim().lowercase()] = raw.substring(idx + 1).trim().unquote()
    }
    return out
}

private fun String.unquote(): String =
    if (length >= 2 && startsWith('"') && endsWith('"')) substring(1, length - 1) else this
