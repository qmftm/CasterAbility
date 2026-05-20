package me.qmftm.casterability.ability

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Skript에서 `ability class` / `ability` 블록을 파싱하면
 * 여기에 등록됩니다.
 *
 * 플러그인 전체에서 싱글톤으로 사용합니다.
 */
object AbilityRegistry {

    // ── 정의 저장소 ────────────────────────────────────────

    private val classes    = mutableMapOf<String, AbilityClass>()
    private val abilities  = mutableMapOf<String, AbilityDefinition>()

    // ── 플레이어 상태 ──────────────────────────────────────

    /** 플레이어 UUID → 현재 장착된 ability class id */
    private val playerClass = mutableMapOf<UUID, String>()

    /** 플레이어 UUID → ability id → 쿨타임 남은 tick */
    private val cooldowns = mutableMapOf<UUID, MutableMap<String, Int>>()

    // ── 등록 ──────────────────────────────────────────────

    fun registerClass(cls: AbilityClass) {
        classes[cls.id] = cls
    }

    fun registerAbility(def: AbilityDefinition) {
        abilities[def.id] = def
    }

    // ── 조회 ──────────────────────────────────────────────

    fun getClass(id: String): AbilityClass? = classes[id]
    fun getAbility(id: String): AbilityDefinition? = abilities[id]

    fun getAllClasses(): Collection<AbilityClass>   = classes.values
    fun getAllAbilities(): Collection<AbilityDefinition> = abilities.values

    /** 특정 클래스에 속한 모든 ability */
    fun abilitiesOfClass(classId: String): List<AbilityDefinition> =
        abilities.values.filter { it.classId == classId }

    // ── 플레이어 클래스 ────────────────────────────────────

    fun setPlayerClass(player: Player, classId: String) {
        playerClass[player.uniqueId] = classId
    }

    fun getPlayerClass(player: Player): AbilityClass? =
        playerClass[player.uniqueId]?.let { classes[it] }

    fun getPlayerClassId(player: Player): String? =
        playerClass[player.uniqueId]

    fun clearPlayerClass(player: Player) {
        playerClass.remove(player.uniqueId)
        cooldowns.remove(player.uniqueId)
    }

    fun clearAll() {
        playerClass.clear()
        cooldowns.clear()
    }

    // ── 쿨타임 ────────────────────────────────────────────

    /** 쿨타임 설정 (tick 단위) */
    fun setCooldown(player: Player, abilityId: String, ticks: Int) {
        cooldowns.getOrPut(player.uniqueId) { mutableMapOf() }[abilityId] = ticks
    }

    /** 쿨타임 조회 (tick 단위, 없으면 0) */
    fun getCooldown(player: Player, abilityId: String): Int =
        cooldowns[player.uniqueId]?.get(abilityId) ?: 0

    fun isOnCooldown(player: Player, abilityId: String): Boolean =
        getCooldown(player, abilityId) > 0

    /** 매 tick 감소 (GameManager 스케줄러에서 호출) */
    fun tickCooldowns() {
        cooldowns.forEach { (_, map) ->
            val iter = map.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                if (entry.value <= 1) iter.remove()
                else entry.setValue(entry.value - 1)
            }
        }
    }

    // ── 초기화 ────────────────────────────────────────────

    /** 서버 리로드 시 정의 초기화 (Skript가 다시 파싱) */
    fun clearDefinitions() {
        classes.clear()
        abilities.clear()
    }
}
