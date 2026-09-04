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

    /**
     * class id → 그 클래스에 속한 ability 목록.
     *
     * abilities를 매번 훑지 않으려고 등록할 때 미리 묶어 둡니다.
     * 피해 이벤트 하나에 리스너가 여럿 붙어서 이 조회가 자주 일어납니다.
     */
    private val byClass = mutableMapOf<String, MutableList<AbilityDefinition>>()

    // ── 플레이어 상태 ──────────────────────────────────────

    /** 플레이어 UUID → 현재 장착된 ability class id */
    private val playerClass = mutableMapOf<UUID, String>()

    /** 플레이어 UUID → ability id → 쿨타임 남은 tick */
    private val cooldowns = mutableMapOf<UUID, MutableMap<String, Int>>()

    /** 플레이어 UUID → 잠시 꺼둔 ability id 집합 */
    private val disabled = mutableMapOf<UUID, MutableSet<String>>()

    /** config.yml game.wreck (0~100). 능력 사용 후 자동으로 거는 쿨타임을 이 비율만큼 줄인다. */
    private var wreckPercent = 0

    // ── 등록 ──────────────────────────────────────────────

    fun registerClass(cls: AbilityClass) {
        classes[cls.id] = cls
    }

    fun registerAbility(def: AbilityDefinition) {
        // 같은 id를 다시 등록하면(스크립트 리로드) 예전 정의를 색인에서 빼야
        // 목록에 유령이 남지 않습니다.
        abilities.put(def.id, def)?.let { old ->
            byClass[old.classId]?.removeIf { it.id == old.id }
        }
        byClass.getOrPut(def.classId) { mutableListOf() }.add(def)
    }

    // ── 조회 ──────────────────────────────────────────────

    fun getClass(id: String): AbilityClass? = classes[id]
    fun getAbility(id: String): AbilityDefinition? = abilities[id]

    fun getAllClasses(): Collection<AbilityClass>   = classes.values
    fun getAllAbilities(): Collection<AbilityDefinition> = abilities.values

    /** 특정 클래스에 속한 모든 ability */
    fun abilitiesOfClass(classId: String): List<AbilityDefinition> =
        byClass[classId] ?: emptyList()

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
    fun triggerableAbilities(player: Player, trigger: AbilityTrigger): List<AbilityDefinition> {
        val defs = abilitiesOfPlayer(player)
        if (defs.isEmpty()) return emptyList()

        // 대부분의 호출은 아무것도 걸리지 않습니다. 그때 리스트를 만들지 않으려고
        // 처음 하나가 걸릴 때까지 미룹니다. (피해 이벤트마다 여러 번 불립니다)
        var matched: MutableList<AbilityDefinition>? = null
        for (def in defs) {
            if (def.trigger != trigger) continue
            if (isAbilityDisabled(player, def.id)) continue
            val list = matched ?: mutableListOf<AbilityDefinition>().also { matched = it }
            list.add(def)
        }
        return matched ?: emptyList()
    }

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
        wreckPercent = 0
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

    /** config.yml game.wreck 값. GameManager가 게임 시작 시 설정한다. */
    fun setWreckPercent(percent: Int) {
        wreckPercent = percent.coerceIn(0, 100)
    }

    /** 쿨타임 설정 (tick 단위) */
    fun setCooldown(player: Player, abilityId: String, ticks: Int) {
        cooldowns.getOrPut(player.uniqueId) { mutableMapOf() }[abilityId] = ticks
    }

    /**
     * 능력을 실제로 사용한 뒤 자동으로 거는 쿨타임.
     * wreck 설정 비율만큼 줄여서 건다. (wreck 50 → 쿨타임 절반)
     * 스크립트가 `set cooldown ...` 으로 직접 거는 것과는 다르다 — 그건 그대로 적용된다.
     */
    fun startCooldown(player: Player, abilityId: String, baseSeconds: Int) {
        val ticks = (baseSeconds * 20L * (100 - wreckPercent) / 100).toInt().coerceAtLeast(0)
        setCooldown(player, abilityId, ticks)
    }

    /** 쿨타임 조회 (tick 단위, 없으면 0) */
    fun getCooldown(player: Player, abilityId: String): Int =
        cooldowns[player.uniqueId]?.get(abilityId) ?: 0

    /**
     * 쿨타임 조회 (초 단위, 올림). 화면·채팅에 보여줄 때는 이걸 쓴다.
     * 내림으로 하면 남은 시간이 있는데도 0초로 보이는 순간이 생긴다.
     */
    fun getCooldownSeconds(player: Player, abilityId: String): Int =
        (getCooldown(player, abilityId) + 19) / 20

    /**
     * 지금 돌고 있는 쿨타임 전부 (ability id → 남은 tick).
     * 다 된 쿨타임은 tickCooldowns()가 지우므로 여기에는 남아 있지 않다.
     */
    fun getAllCooldowns(player: Player): Map<String, Int> =
        cooldowns[player.uniqueId] ?: emptyMap()

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
        byClass.clear()
    }
}
