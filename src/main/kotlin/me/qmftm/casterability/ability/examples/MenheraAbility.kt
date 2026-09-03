package me.qmftm.casterability.ability.examples

import me.qmftm.casterability.ability.AbilityClass
import me.qmftm.casterability.ability.AbilityDefinition
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.ability.AbilityTrigger
import me.qmftm.casterability.game.PlayerStateManager
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * ChzzkAbility의 Menhera 능력을 Kotlin으로 구현한 예제입니다.
 *
 * 개선사항:
 * - Skript의 복잡한 효과 관리를 Kotlin 객체로 관리
 * - 자동 회복 방지 + 피해 주면 재생 효과
 * - 자해 스킬로 체력 소비 후 강화
 */
class MenheraAbility {

    companion object {
        const val CLASS_ID = "menhera"
        const val PASSIVE_ID = "menhera_passive"
        const val SKILL_ID = "menhera_selfharm"

        // 설정
        const val COOLTIME = 50
        const val LOVE_MULTIPLE = 0.5
        const val LOVE_REGENERATION = 2
        const val SELFHARM_PERCENT = 25
        const val SELFHARM_EFFECT = 20

        fun register() {
            // 능력 클래스 등록
            val menheraClass = AbilityClass(
                id = CLASS_ID,
                displayName = "멘헤라",
                grade = 3,
                description = listOf(
                    "&d[&7패시브 &f- &d사랑해줘]",
                    "&f자연 회복을 하지 않습니다.",
                    "&f자신이 아닌 플레이어에게 대미지를 입으면 (입은 피해량 * $LOVE_MULTIPLE&f)초 만큼 &d재생 $LOVE_REGENERATION &f효과를 받습니다.",
                    "",
                    "&d[&7철괴 우클릭 &f- &d자신감 해로] &7(쿨타임 ${COOLTIME}초)",
                    "&f자신에게 &c최대 체력의 $SELFHARM_PERCENT%&f만큼 피해를 입힌 뒤,",
                    "&f${SELFHARM_EFFECT}초의 &b신속, &e힘, &7저항 &f효과를 얻습니다.",
                    "&f남은 체력이 $SELFHARM_PERCENT% 이하 일 때에는 사용할 수 없습니다."
                )
            )

            // 패시브 능력 등록
            val passiveDef = AbilityDefinition(
                id = PASSIVE_ID,
                classId = CLASS_ID,
                displayName = "사랑해줘",
                trigger = AbilityTrigger.PASSIVE,
                passiveIntervalTicks = 20,
                cooldownSeconds = 0
            )

            // 스킬 능력 등록
            val skillDef = AbilityDefinition(
                id = SKILL_ID,
                classId = CLASS_ID,
                displayName = "자신감 해로",
                trigger = AbilityTrigger.SKILL,
                passiveIntervalTicks = 0,
                cooldownSeconds = COOLTIME
            )

            AbilityRegistry.registerClass(menheraClass)
            AbilityRegistry.registerAbility(passiveDef)
            AbilityRegistry.registerAbility(skillDef)
        }

        /**
         * 패시브: 다른 플레이어에게 피해를 입으면 재생 효과 획득
         */
        fun onDealDamage(attacker: Player, victim: Player, damage: Double) {
            if (AbilityRegistry.getPlayerClassId(attacker) != CLASS_ID) return

            // 자신이 아닌 플레이어에게 피해를 입었을 때
            if (attacker.uniqueId != victim.uniqueId) {
                val regenerationDuration = (damage * LOVE_MULTIPLE).toInt()
                attacker.addPotionEffect(
                    PotionEffect(PotionEffectType.REGENERATION, regenerationDuration * 20, 0, false, false)
                )
                PlayerStateManager.setCustomUi(attacker, "love_effect", "&d사랑 효과: ${regenerationDuration}초")
            }
        }

        /**
         * 패시브: 자연 회복 방지
         */
        fun preventNaturalRegen(player: Player): Boolean {
            return AbilityRegistry.getPlayerClassId(player) == CLASS_ID
        }

        /**
         * 스킬: 자신감 해로
         * 자신에게 최대 체력의 25% 피해를 입힌 뒤 강화 효과
         */
        fun useSelfHarmSkill(player: Player): Boolean {
            if (AbilityRegistry.getPlayerClassId(player) != CLASS_ID) return false

            val maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
            val healthThreshold = maxHealth * (SELFHARM_PERCENT / 100.0)

            // 체력이 threshold 이하면 사용 불가
            if (player.health <= healthThreshold) {
                player.sendMessage("&c남은 체력이 부족합니다.".replace("&", "§"))
                return false
            }

            // 자신에게 피해 입히기
            val damage = maxHealth * (SELFHARM_PERCENT / 100.0)
            player.damage(damage)

            // 강화 효과 적용
            val effects = listOf(
                PotionEffect(PotionEffectType.SPEED, SELFHARM_EFFECT * 20, 0, false, false),
                PotionEffect(PotionEffectType.STRENGTH, SELFHARM_EFFECT * 20, 0, false, false),
                PotionEffect(PotionEffectType.RESISTANCE, SELFHARM_EFFECT * 20, 0, false, false)
            )

            effects.forEach { player.addPotionEffect(it) }

            PlayerStateManager.setCustomUi(player, "selfharm_effect", "&b강화: ${SELFHARM_EFFECT}초")
            player.sendMessage("&e자신감 해로를 사용했습니다!".replace("&", "§"))

            return true
        }
    }
}
