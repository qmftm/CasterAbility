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
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.roundToInt

/**
 * ChzzkAbility의 menhera.sk를 CasterAbility 구조로 옮긴 예제입니다.
 *
 * 멘헤라 (B등급)
 *  · 패시브 "사랑해줘" : 자연 회복을 하지 않는다.
 *    남에게 맞으면 (받은 피해 × 0.5)초 동안 재생 2를 얻는다.
 *  · 철괴 우클릭 "자신감 해로" (쿨타임 50초) :
 *    최대 체력의 25%만큼 자해한 뒤 20초간 신속 · 힘 · 저항을 얻는다.
 *    남은 체력이 최대 체력의 25% 이하면 사용할 수 없다.
 */
class MenheraAbility(private val plugin: CasterAbility) : Listener {

    companion object {
        const val CLASS_ID     = "menhera"
        const val ID_LOVE      = "menhera_love"
        const val ID_SELFHARM  = "menhera_selfharm"

        const val COOLTIME          = 50
        const val LOVE_MULTIPLE     = 0.5
        const val LOVE_REGEN_LEVEL  = 2     // 재생 2
        const val SELFHARM_PERCENT  = 25.0
        const val SELFHARM_SECONDS  = 20

        private const val UI_LOVE = "menhera_love"

        fun register() {
            AbilityRegistry.registerClass(AbilityClass(CLASS_ID, "멘헤라", tier = 3))

            AbilityRegistry.registerAbility(
                AbilityDefinition(ID_LOVE, CLASS_ID, AbilityTrigger.ON_DAMAGED,
                    item = null, cooldownSeconds = 0, passiveIntervalTicks = 0L)
            )
            AbilityRegistry.registerAbility(
                AbilityDefinition(ID_SELFHARM, CLASS_ID, AbilityTrigger.RIGHT_CLICK,
                    item = Material.BLAZE_ROD, cooldownSeconds = COOLTIME, passiveIntervalTicks = 0L)
            )
            PlayerStateManager.registerEffect(UI_LOVE, "사랑")
        }
    }

    // ── 패시브: 자연 회복 차단 ──────────────────────────────

    @EventHandler
    fun onRegain(event: EntityRegainHealthEvent) {
        val p = event.entity as? Player ?: return
        if (AbilityRegistry.getPlayerClassId(p) != CLASS_ID) return
        if (event.regainReason == EntityRegainHealthEvent.RegainReason.SATIATED ||
            event.regainReason == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.isCancelled = true
        }
    }

    // ── 능력 발동 ──────────────────────────────────────────

    @EventHandler
    fun onAbilityUse(event: AbilityUseEvent) {
        when (event.abilityId) {
            ID_LOVE     -> onDamaged(event)
            ID_SELFHARM -> onSelfHarm(event)
        }
    }

    /** 남에게 맞으면 재생 효과 */
    private fun onDamaged(event: AbilityUseEvent) {
        val self     = event.player
        val attacker = event.target as? Player ?: return
        if (attacker.uniqueId == self.uniqueId) return

        val damage  = event.damage ?: return
        val seconds = damage * LOVE_MULTIPLE
        if (seconds <= 0) return

        self.addPotionEffect(PotionEffect(
            PotionEffectType.REGENERATION,
            (seconds * 20).roundToInt(),
            LOVE_REGEN_LEVEL - 1,
            false, false,
        ))
        PlayerStateManager.addEffect(self, UI_LOVE, seconds, mode = "set")
    }

    /** 철괴 우클릭: 자신감 해로 */
    private fun onSelfHarm(event: AbilityUseEvent) {
        val p = event.player
        val maxHealth = p.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        val threshold = maxHealth * (SELFHARM_PERCENT / 100.0)

        if (p.health <= threshold) {
            p.sendMessage("&c남은 체력이 ${SELFHARM_PERCENT.toInt()}%% 이하라 사용할 수 없습니다.".toComponent())
            // 조건 미달이면 쿨타임을 되돌린다 (Dispatcher가 이벤트 직후에 걸기 때문에 다음 틱)
            plugin.server.scheduler.runTask(plugin, Runnable {
                AbilityRegistry.setCooldown(p, ID_SELFHARM, 0)
            })
            return
        }

        p.damage(threshold)

        val ticks = SELFHARM_SECONDS * 20
        listOf(
            PotionEffectType.SPEED,
            PotionEffectType.STRENGTH,
            PotionEffectType.RESISTANCE,
        ).forEach { p.addPotionEffect(PotionEffect(it, ticks, 0, false, false)) }

        p.sendMessage("&e자신감 해로! &f${SELFHARM_SECONDS}초간 강화됩니다.".toComponent())
    }
}
