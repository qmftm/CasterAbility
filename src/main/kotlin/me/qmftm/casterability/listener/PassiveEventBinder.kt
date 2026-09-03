package me.qmftm.casterability.listener

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityDefinition
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.ability.AbilityTrigger
import me.qmftm.casterability.event.AbilityUseEvent
import me.qmftm.casterability.game.GameManager
import me.qmftm.casterability.game.GamePhase
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerEvent

/**
 * `trigger: passive` + `event: <이벤트 이름>` 으로 정의한 능력을
 * 실제 Bukkit 이벤트에 붙여줍니다.
 *
 * 게임이 시작될 때 그 시점의 능력 정의를 보고 리스너를 답니다.
 * 게임이 끝나면 전부 뗍니다.
 */
class PassiveEventBinder(
    private val plugin: CasterAbility,
    private val game: GameManager,
) : Listener {

    private var bound = false

    /** 지금 실행 중인 (플레이어, 능력) — 능력이 자기 이벤트를 다시 부르는 것을 막는다 */
    private val running = mutableSetOf<String>()

    /** 이벤트에서 플레이어를 꺼내는 getPlayer() 캐시. 없는 이벤트는 null이 들어간다. */
    private val playerGetters = mutableMapOf<Class<*>, java.lang.reflect.Method?>()

    /** 비동기라 못 쓴다고 이미 알린 이벤트들 */
    private val warnedAsync = mutableSetOf<Class<*>>()

    // ── 붙이고 떼기 ───────────────────────────────────────

    fun bindAll() {
        unbindAll()

        val defs = AbilityRegistry.getAllAbilities().filter { it.isEventDriven }
        if (defs.isEmpty()) return

        // 이벤트 클래스별로 그 이벤트를 기다리는 능력들을 모은다
        val byClass = mutableMapOf<Class<out Event>, MutableList<AbilityDefinition>>()
        for (def in defs) {
            for (cls in mostGeneral(def.eventClasses)) {
                byClass.getOrPut(cls) { mutableListOf() }.add(def)
            }
        }

        var attached = 0
        for ((cls, waiting) in byClass) {
            try {
                Bukkit.getPluginManager().registerEvent(
                    cls, this, EventPriority.NORMAL,
                    { _, event -> if (cls.isInstance(event)) handle(waiting, event) },
                    plugin, false,
                )
                attached++
            } catch (e: Exception) {
                // 핸들러 목록이 없는 이벤트 등 — 그 능력만 포기하고 게임은 계속 간다
                plugin.logger.warning(
                    "'${cls.simpleName}' 이벤트에 연결하지 못했습니다: ${e.message} " +
                    "(능력: ${waiting.joinToString(", ") { it.id }})"
                )
            }
        }
        bound = true

        plugin.logger.info("이벤트로 발동하는 패시브 ${defs.size}개를 ${attached}종류의 이벤트에 연결했습니다.")
    }

    fun unbindAll() {
        if (bound) HandlerList.unregisterAll(this)
        bound = false
        running.clear()
        warnedAsync.clear()
    }

    /**
     * 한 능력이 상위/하위 관계인 이벤트를 같이 들고 있으면 상위 것만 남긴다.
     * 상위와 하위는 핸들러 목록을 공유해서, 둘 다 달면 한 번에 두 번 발동한다.
     */
    private fun mostGeneral(classes: List<Class<out Event>>): List<Class<out Event>> =
        classes.distinct().filter { candidate ->
            classes.none { other -> other != candidate && other.isAssignableFrom(candidate) }
        }

    // ── 실제 발동 ─────────────────────────────────────────

    private fun handle(waiting: List<AbilityDefinition>, event: Event) {
        if (event is AbilityUseEvent) return // 우리 이벤트를 다시 받지 않는다

        // 비동기 이벤트(채팅 등)에서는 능력을 발동시킬 수 없다
        if (event.isAsynchronous) {
            if (warnedAsync.add(event.javaClass)) {
                plugin.logger.warning(
                    "'${event.javaClass.simpleName}' 은 비동기 이벤트라 능력을 발동할 수 없습니다. " +
                    "(능력: ${waiting.joinToString(", ") { it.id }})"
                )
            }
            return
        }

        val player = playerOf(event) ?: return
        if (!game.isRunning || game.phase != GamePhase.IN_GAME) return
        if (player.uniqueId !in game.gamePlayers) return

        val classId = AbilityRegistry.getPlayerClassId(player) ?: return

        for (def in waiting) {
            if (def.classId != classId) continue
            if (AbilityRegistry.isAbilityDisabled(player, def.id)) continue
            if (AbilityRegistry.isOnCooldown(player, def.id)) continue

            val key = "${player.uniqueId}:${def.id}"
            if (!running.add(key)) continue // 이미 이 능력이 돌고 있는 중

            try {
                val use = AbilityUseEvent(
                    player      = player,
                    ability     = def,
                    trigger     = AbilityTrigger.PASSIVE,
                    target      = targetOf(event, player),
                    damage      = (event as? EntityDamageEvent)?.damage,
                    sourceEvent = event,
                )
                Bukkit.getPluginManager().callEvent(use)

                if (use.isCancelled) {
                    // 스크립트가 취소하면 원본 이벤트도 같이 취소하고 쿨타임은 걸지 않는다
                    (event as? Cancellable)?.isCancelled = true
                    continue
                }

                if (event is EntityDamageEvent) use.damage?.let { event.damage = it }
                if (def.cooldownSeconds > 0) {
                    AbilityRegistry.startCooldown(player, def.id, def.cooldownSeconds)
                }
            } finally {
                running.remove(key)
            }
        }
    }

    // ── 이벤트에서 값 꺼내기 ──────────────────────────────

    /** 이 이벤트의 주인공 플레이어 */
    private fun playerOf(event: Event): Player? {
        if (event is PlayerEvent) return event.player
        if (event is EntityEvent) (event.entity as? Player)?.let { return it }
        // BlockBreakEvent 처럼 PlayerEvent가 아니면서 getPlayer() 를 가진 이벤트들
        val getter = playerGetters.getOrPut(event.javaClass) {
            runCatching { event.javaClass.getMethod("getPlayer") }
                .getOrNull()
                ?.takeIf { Player::class.java.isAssignableFrom(it.returnType) }
        } ?: return null
        return runCatching { getter.invoke(event) as? Player }.getOrNull()
    }

    /** 상대편이 있으면 그쪽. 스크립트에서 event-entity 로 읽는다. */
    private fun targetOf(event: Event, owner: Player): Entity? = when (event) {
        is EntityDamageByEntityEvent -> when (owner) {
            event.entity  -> event.damager
            event.damager -> event.entity
            else          -> null
        }
        is PlayerDeathEvent -> event.entity.killer
        is EntityEvent      -> event.entity.takeIf { it != owner }
        else                -> null
    }
}
