package me.qmftm.casterability.listener

import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.ability.AbilityTrigger
import me.qmftm.casterability.event.AbilityUseEvent
import me.qmftm.casterability.game.GameManager
import me.qmftm.casterability.game.GamePhase
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent

/**
 * 플레이어의 행동을 감지하고, 해당 ability 트리거에 맞는
 * AbilityUseEvent를 발생시킵니다.
 *
 * Skript의 `on ability use "..."` 이벤트가 이것을 수신합니다.
 */
class AbilityDispatcher(private val game: GameManager) : Listener {

    // ── RIGHT_CLICK / LEFT_CLICK ──────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        val p = event.player
        if (!isInGame(p)) return

        val trigger = when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> AbilityTrigger.RIGHT_CLICK
            // 좌클릭이 상대를 맞히면 여긴 안 뜨고 onLeftClickHit()이 대신 잡는다.
            // 여긴 허공/블록을 향해 휘둘렀을 때만 온다.
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK   -> AbilityTrigger.LEFT_CLICK
            else -> return
        }

        // 허공/블록을 향해 일부러 휘두른 것이므로 쿨타임이면 알려준다
        handleClick(p, trigger, notifyCooldown = true)
    }

    /**
     * 좌클릭이 대상(플레이어든 몹이든)을 실제로 맞혔을 때도 발동시킨다.
     *
     * 곡괭이질 같은 채굴은 이 이벤트가 안 뜨므로 광질 스팸 걱정은 없다 —
     * EntityDamageByEntityEvent는 뭔가를 실제로 때렸을 때만 발생한다.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onLeftClickHit(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        if (!isInGame(attacker)) return
        // 전투 중에는 초당 몇 번씩 들어오므로 쿨타임 안내를 하지 않는다.
        // 안내는 허공을 향해 휘둘렀을 때(onInteract)만 뜬다.
        handleClick(attacker, AbilityTrigger.LEFT_CLICK, notifyCooldown = false)
    }

    /**
     * RIGHT_CLICK/LEFT_CLICK 공통: 아이템을 확인하고, 쿨타임이면 건너뛰고, 아니면 발동시킨다.
     *
     * @param notifyCooldown 쿨타임 중일 때 남은 시간을 채팅으로 알려줄지
     */
    private fun handleClick(p: Player, trigger: AbilityTrigger, notifyCooldown: Boolean) {
        val defs = AbilityRegistry.triggerableAbilities(p, trigger)
        if (defs.isEmpty()) return

        // 손에 든 아이템은 한 번만 읽는다
        val held = p.inventory.itemInMainHand.type

        for (def in defs) {
            // item이 지정된 경우 손에 들고 있어야 함
            if (def.item != null && def.item != held) continue

            if (AbilityRegistry.isOnCooldown(p, def.id)) {
                if (notifyCooldown) {
                    val remain = AbilityRegistry.getCooldownSeconds(p, def.id)
                    p.sendMessage("§c[${def.displayName}] 쿨타임 ${remain}초 남음")
                }
                continue
            }

            val used = dispatch(p, def.id, trigger)
            // 스크립트가 cancel event 로 거부하면 쿨타임을 걸지 않는다
            if (used != null && !used.isCancelled && def.cooldownSeconds > 0) {
                AbilityRegistry.startCooldown(p, def.id, def.cooldownSeconds)
            }
        }
    }

    // ── ON_HIT ───────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onHit(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim   = event.entity   as? Player ?: return
        if (!isInGame(attacker)) return

        AbilityRegistry.triggerableAbilities(attacker, AbilityTrigger.ON_HIT)
            .forEach { def ->
                val useEvent = AbilityUseEvent(
                    player  = attacker,
                    ability = def,
                    trigger = AbilityTrigger.ON_HIT,
                    target  = victim,
                    damage  = event.damage,
                )
                Bukkit.getPluginManager().callEvent(useEvent)
                if (!useEvent.isCancelled) {
                    useEvent.damage?.let { event.damage = it }
                }
            }
    }

    // ── ON_DAMAGED ───────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDamaged(event: EntityDamageByEntityEvent) {
        val victim   = event.entity   as? Player ?: return
        val attacker = event.damager  as? Player ?: return
        if (!isInGame(victim)) return

        AbilityRegistry.triggerableAbilities(victim, AbilityTrigger.ON_DAMAGED)
            .forEach { def ->
                val useEvent = AbilityUseEvent(
                    player  = victim,
                    ability = def,
                    trigger = AbilityTrigger.ON_DAMAGED,
                    target  = attacker,
                    damage  = event.damage,
                )
                Bukkit.getPluginManager().callEvent(useEvent)
                if (!useEvent.isCancelled) {
                    useEvent.damage?.let { event.damage = it }
                }
            }
    }

    // ── ON_KILL ──────────────────────────────────────────

    // on_hit / on_damaged 가 HIGH에서 피해량을 고친 뒤에 봐야 하므로 그보다 뒤에 선다.
    // NORMAL에서 보면 능력으로 늘어난 피해가 반영되기 전이라, 그 능력 때문에
    // 죽는 경우에 on_kill 이 뜨지 않는다.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onKill(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        if (victim.health - event.finalDamage > 0) return
        val killer = event.damager as? Player ?: return
        if (!isInGame(killer)) return

        AbilityRegistry.triggerableAbilities(killer, AbilityTrigger.ON_KILL)
            .forEach { def -> dispatch(killer, def.id, AbilityTrigger.ON_KILL, victim) }
    }

    // ── ON_DEATH ─────────────────────────────────────────

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val p = event.entity
        if (!isInGame(p)) return

        // event-entity → 나를 죽인 플레이어. 몹이나 환경사면 null
        AbilityRegistry.triggerableAbilities(p, AbilityTrigger.ON_DEATH)
            .forEach { def -> dispatch(p, def.id, AbilityTrigger.ON_DEATH, event.entity.killer) }
    }

    // ── 공통 dispatch ─────────────────────────────────────

    private fun dispatch(
        player: Player,
        abilityId: String,
        trigger: AbilityTrigger,
        target: org.bukkit.entity.Entity? = null,
    ): AbilityUseEvent? {
        val def = AbilityRegistry.getAbility(abilityId) ?: return null
        val useEvent = AbilityUseEvent(
            player  = player,
            ability = def,
            trigger = trigger,
            target  = target,
        )
        Bukkit.getPluginManager().callEvent(useEvent)
        return useEvent
    }

    private fun isInGame(p: Player): Boolean =
        game.isRunning && game.phase == GamePhase.IN_GAME &&
        p.uniqueId in game.gamePlayers
}
