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
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK   -> AbilityTrigger.LEFT_CLICK
            else -> return
        }

        // 플레이어의 클래스에 속한 ability 중 트리거/아이템이 맞는 것을 발동
        AbilityRegistry.triggerableAbilities(p, trigger)
            .filter { def ->
                // item이 지정된 경우 손에 들고 있어야 함
                def.item == null || def.item == p.inventory.itemInMainHand.type
            }
            .forEach { def ->
                if (AbilityRegistry.isOnCooldown(p, def.id)) {
                    val remain = AbilityRegistry.getCooldown(p, def.id) / 20
                    p.sendMessage("§c[${def.id}] 쿨타임 ${remain}초 남음")
                    return@forEach
                }
                val used = dispatch(p, def.id, trigger)
                // 스크립트가 cancel event 로 거부하면 쿨타임을 걸지 않는다
                if (used != null && !used.isCancelled && def.cooldownSeconds > 0) {
                    AbilityRegistry.startCooldown(p, def.id, def.cooldownSeconds)
                }
            }
    }

    // ── ON_HIT ───────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
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

    @EventHandler(priority = EventPriority.HIGH)
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

    @EventHandler
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

        AbilityRegistry.triggerableAbilities(p, AbilityTrigger.ON_DEATH)
            .forEach { def -> dispatch(p, def.id, AbilityTrigger.ON_DEATH) }
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
