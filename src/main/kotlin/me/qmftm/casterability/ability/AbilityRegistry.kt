package me.qmftm.casterability.ability

import org.bukkit.Bukkit
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

    /** 플레이어 UUID → 잠시 꺼둔 ability id 집합 */
    private val disabled = mutableMapOf<UUID, MutableSet<String>>()

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

    /** 플레이어가 현재 가진 클래스에 속한 모든 ability */
    fun abilitiesOfPlayer(player: Player): List<AbilityDefinition> =
        getPlayerClassId(player)?.let { abilitiesOfClass(it) } ?: emptyList()

    /** 플레이어가 이 ability를 실제로 가지고 있는지 (클래스가 일치하는지) */
    fun ownsAbility(player: Player, abilityId: String): Boolean {
        val def = abilities[abilityId] ?: return false
        return getPlayerClassId(player) == def.classId
    }

    /**
     * 지금 이 트리거로 발동할 수 있는 ability 목록.
     * 클래스가 맞고, 꺼두지 않은 것만 남긴다.
     */
    fun triggerableAbilities(player: Player, trigger: AbilityTrigger): List<AbilityDefinition> =
        abilitiesOfPlayer(player).filter { it.trigger == trigger && !isAbilityDisabled(player, it.id) }

    /** 해당 클래스를 가진 접속 중인 플레이어들 */
    fun playersWithClass(classId: String): List<Player> =
        Bukkit.getOnlinePlayers().filter { getPlayerClassId(it) == classId }

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
        disabled.remove(player.uniqueId)
    }

    fun clearAll() {
        playerClass.clear()
        cooldowns.clear()
        disabled.clear()
    }

    // ── 능력 켜고 끄기 ────────────────────────────────────

    /** 특정 플레이어에게만 이 ability를 잠시 꺼두거나 다시 켠다. */
    fun setAbilityEnabled(player: Player, abilityId: String, enabled: Boolean) {
        if (enabled) {
            disabled[player.uniqueId]?.remove(abilityId)
        } else {
            disabled.getOrPut(player.uniqueId) { mutableSetOf() }.add(abilityId)
        }
    }

    fun isAbilityDisabled(player: Player, abilityId: String): Boolean =
        disabled[player.uniqueId]?.contains(abilityId) == true

    /** 꺼둔 능력을 전부 다시 켠다. */
    fun clearDisabled(player: Player) {
        disabled.remove(player.uniqueId)
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

    /** 이 플레이어의 쿨타임을 전부 없앤다. */
    fun clearCooldowns(player: Player) {
        cooldowns.remove(player.uniqueId)
    }

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
