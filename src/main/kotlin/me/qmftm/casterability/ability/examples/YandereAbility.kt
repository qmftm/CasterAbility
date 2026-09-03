package me.qmftm.casterability.ability.examples

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityClass
import me.qmftm.casterability.ability.AbilityDefinition
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.ability.AbilityTrigger
import me.qmftm.casterability.event.AbilityUseEvent
import me.qmftm.casterability.game.PlayerStateManager
import me.qmftm.casterability.util.toComponent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import java.util.UUID

/**
 * ChzzkAbility의 Yandere.sk를 CasterAbility 구조로 옮긴 예제입니다.
 *
 * - 능력 정의는 AbilityRegistry에 등록하고,
 * - 실제 동작은 AbilityDispatcher가 쏘는 AbilityUseEvent를 받아서 처리합니다.
 *
 * 얀데레 (A등급)
 *  · 패시브 "처음 본 순간" : 처음 때린 상대를 집착 대상으로 지정.
 *    집착 대상과 20칸 이내면 주는 피해 +25%, 받는 피해 -25%.
 *  · 철괴 우클릭 "질투" (쿨타임 45초) : 집착 대상이 마지막으로 때린 상대가
 *    내가 아니면, 그 상대와 집착 대상에게 마지막 피해의 5배를 준다.
 */
class YandereAbility(private val plugin: CasterAbility) : Listener {

    companion object {
        const val CLASS_ID    = "yandere"
        const val ID_OBSESS   = "yandere_obsession"   // 때릴 때
        const val ID_DEVOTION = "yandere_devotion"    // 맞을 때
        const val ID_JEALOUSY = "yandere_jealousy"    // 철괴 우클릭

        const val COOLTIME           = 45
        const val OBSESSION_DISTANCE = 20.0
        const val OBSESSION_PERCENT  = 25.0
        const val JEALOUSY_MULTIPLE  = 5.0

        private const val VAR_OBSESSION = "obsession"
        private const val UI_OBSESSION  = "obsession"

        /** 능력 클래스와 능력 정의를 레지스트리에 등록한다. */
        fun register() {
            AbilityRegistry.registerClass(AbilityClass(CLASS_ID, "얀데레", tier = 2))

            AbilityRegistry.registerAbility(
                AbilityDefinition(ID_OBSESS, CLASS_ID, AbilityTrigger.ON_HIT,
                    item = null, cooldownSeconds = 0, passiveIntervalTicks = 0L)
            )
            AbilityRegistry.registerAbility(
                AbilityDefinition(ID_DEVOTION, CLASS_ID, AbilityTrigger.ON_DAMAGED,
                    item = null, cooldownSeconds = 0, passiveIntervalTicks = 0L)
            )
            AbilityRegistry.registerAbility(
                AbilityDefinition(ID_JEALOUSY, CLASS_ID, AbilityTrigger.RIGHT_CLICK,
                    item = Material.BLAZE_ROD, cooldownSeconds = COOLTIME, passiveIntervalTicks = 0L)
            )
        }
    }

    /** 플레이어가 마지막으로 가한 공격 (대상, 피해량) — 질투 스킬이 참조한다. */
    private val lastAttack = mutableMapOf<UUID, Pair<UUID, Double>>()

    // ── 모든 플레이어의 마지막 공격 기록 ────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun trackLastAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim   = event.entity  as? Player ?: return
        lastAttack[attacker.uniqueId] = victim.uniqueId to event.finalDamage
    }

    // ── 능력 발동 ──────────────────────────────────────────

    @EventHandler
    fun onAbilityUse(event: AbilityUseEvent) {
        when (event.abilityId) {
            ID_OBSESS   -> onHit(event)
            ID_DEVOTION -> onDamaged(event)
            ID_JEALOUSY -> onJealousy(event)
        }
    }

    /** 때릴 때: 첫 대상을 집착 대상으로 지정하고, 집착 대상이면 피해 증가 */
    private fun onHit(event: AbilityUseEvent) {
        val attacker = event.player
        val victim   = event.target as? Player ?: return

        val obsession = PlayerStateManager.getPlayerRefVar(attacker, VAR_OBSESSION)
        if (obsession == null) {
            PlayerStateManager.setPlayerRefVar(attacker, VAR_OBSESSION, victim)
            PlayerStateManager.setCustomUi(attacker, UI_OBSESSION, "&d집착 대상: ${victim.name}")
            attacker.sendMessage("&d${victim.name}님을 집착 대상으로 지정하셨습니다.".toComponent())
            return
        }
        if (obsession != victim.uniqueId) return
        if (!withinObsessionRange(attacker, victim)) return

        val base = event.damage ?: return
        event.damage = base * (1 + OBSESSION_PERCENT / 100.0)
    }

    /** 맞을 때: 집착 대상에게 맞은 것이고 가까우면 피해 감소 */
    private fun onDamaged(event: AbilityUseEvent) {
        val self     = event.player
        val attacker = event.target as? Player ?: return

        val obsession = PlayerStateManager.getPlayerRefVar(self, VAR_OBSESSION) ?: return
        if (obsession != attacker.uniqueId) return
        if (!withinObsessionRange(self, attacker)) return

        val base = event.damage ?: return
        event.damage = base * (1 - OBSESSION_PERCENT / 100.0)
    }

    /** 철괴 우클릭: 질투 */
    private fun onJealousy(event: AbilityUseEvent) {
        val p = event.player

        val obsessionId = PlayerStateManager.getPlayerRefVar(p, VAR_OBSESSION)
        if (obsessionId == null) {
            fail(p, "&c집착 대상이 없습니다.")
            return
        }
        val obsession = plugin.server.getPlayer(obsessionId)
        if (obsession == null || !obsession.isOnline) {
            fail(p, "&c집착 대상이 접속 중이 아닙니다.")
            return
        }

        val (lastTargetId, lastDamage) = lastAttack[obsessionId] ?: run {
            fail(p, "&c집착 대상이 아직 아무도 공격하지 않았습니다.")
            return
        }
        if (lastTargetId == p.uniqueId) {
            fail(p, "&c집착 대상이 마지막으로 공격한 대상이 자신입니다.")
            return
        }
        val lastTarget = plugin.server.getPlayer(lastTargetId)
        if (lastTarget == null || !lastTarget.isOnline) {
            fail(p, "&c집착 대상이 마지막으로 공격한 상대가 접속 중이 아닙니다.")
            return
        }

        val damage = lastDamage * JEALOUSY_MULTIPLE
        lastTarget.damage(damage, p)
        obsession.damage(damage, p)
        p.sendMessage("&d질투! &f%s&f와 &c%s&f에게 &c%.1f&f의 피해를 주었습니다."
            .format(lastTarget.name, obsession.name, damage).toComponent())
    }

    // ── 집착 대상이 죽으면 지정 해제 ────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDeath(event: PlayerDeathEvent) {
        val deadId = event.entity.uniqueId
        lastAttack.remove(deadId)

        // 죽은 플레이어를 집착 대상으로 삼고 있던 모두의 지정을 해제
        for (online in plugin.server.onlinePlayers) {
            if (AbilityRegistry.getPlayerClassId(online) != CLASS_ID) continue
            if (PlayerStateManager.getPlayerRefVar(online, VAR_OBSESSION) != deadId) continue
            PlayerStateManager.deletePlayerVar(online, VAR_OBSESSION)
            PlayerStateManager.removeCustomUi(online, UI_OBSESSION)
            online.sendMessage("&7집착 대상이 사망하여 지정이 해제되었습니다.".toComponent())
        }
    }

    // ── 유틸 ──────────────────────────────────────────────

    private fun withinObsessionRange(a: Player, b: Player): Boolean =
        a.world == b.world && a.location.distance(b.location) <= OBSESSION_DISTANCE

    /**
     * 발동 조건을 못 맞췄을 때. AbilityDispatcher는 이벤트 호출 직후 쿨타임을
     * 걸어버리므로, 다음 틱에 쿨타임을 되돌려 준다.
     */
    private fun fail(p: Player, message: String) {
        p.sendMessage(message.toComponent())
        plugin.server.scheduler.runTask(plugin, Runnable {
            AbilityRegistry.setCooldown(p, ID_JEALOUSY, 0)
        })
    }
}
