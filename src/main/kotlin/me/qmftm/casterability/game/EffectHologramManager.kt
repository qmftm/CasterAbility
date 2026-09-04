package me.qmftm.casterability.game

import me.qmftm.casterability.util.toComponent
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import java.util.UUID

/**
 * 상태이상에 등록해둔 홀로그램 글자를, 그 상태이상이 걸린 플레이어 머리 위에
 * 자동으로 띄우고 따라다니게 하고, 풀리면 지웁니다.
 *
 * AbilityWar의 Stun 이펙트와 같은 방식 — 투명 아머스탠드를 이름표만 보이게 씁니다.
 * 등록해두지 않은 상태이상은 지금까지처럼 액션바에만 뜨고 홀로그램은 안 붙습니다.
 */
object EffectHologramManager {

    private const val HEIGHT_OFFSET = 2.3

    /** 상태이상 id → 홀로그램에 쓸 글자 (& 색 코드) */
    private val hologramTexts = mutableMapOf<String, String>()

    /** 플레이어 UUID → 상태이상 id → 그 상태이상을 표시 중인 아머스탠드 */
    private val stands = mutableMapOf<UUID, MutableMap<String, ArmorStand>>()

    fun register(effectId: String, text: String) {
        hologramTexts[effectId] = text
    }

    /** 상태이상이 걸렸을 때 부른다. 등록된 글자가 없으면 아무것도 하지 않는다. */
    fun spawn(player: Player, effectId: String) {
        val text = hologramTexts[effectId] ?: return
        val playerStands = stands.getOrPut(player.uniqueId) { mutableMapOf() }
        if (playerStands.containsKey(effectId)) return // 이미 떠 있으면 다시 안 만든다

        val stand = player.world.spawn(player.location, ArmorStand::class.java) { s ->
            s.setVisible(false)
            s.setGravity(false)
            s.setInvulnerable(true)
            s.setMarker(true) // 히트박스와 상호작용을 없앤다
            s.setCustomNameVisible(true)
            s.customName(text.toComponent())
            s.setPersistent(false) // 청크 저장에 안 남긴다 — 어차피 매번 다시 만든다
        }
        playerStands[effectId] = stand
    }

    /** 상태이상이 풀렸을 때(자연 만료든 직접 제거든) 부른다. 플레이어가 오프라인이어도 된다. */
    fun despawn(uuid: UUID, effectId: String) {
        val playerStands = stands[uuid] ?: return
        playerStands.remove(effectId)?.remove()
        if (playerStands.isEmpty()) stands.remove(uuid)
    }

    /** 이 플레이어의 홀로그램을 전부 지운다. */
    fun despawnAll(uuid: UUID) {
        stands.remove(uuid)?.values?.forEach { it.remove() }
    }

    fun clearAll() {
        stands.values.forEach { map -> map.values.forEach { it.remove() } }
        stands.clear()
    }

    /** 매 틱 호출 — 살아 있는 홀로그램을 전부 주인 머리 위로 옮긴다. */
    fun follow() {
        if (stands.isEmpty()) return
        for ((uuid, map) in stands) {
            val player = Bukkit.getPlayer(uuid) ?: continue // 오프라인이면 있던 자리에 그대로 둔다
            val above = player.location.clone().add(0.0, HEIGHT_OFFSET, 0.0)
            for (stand in map.values) {
                if (stand.isValid) stand.teleport(above)
            }
        }
    }
}
