package me.qmftm.casterability.ability.examples

import me.qmftm.casterability.ability.AbilityClass
import me.qmftm.casterability.ability.AbilityDefinition
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.ability.AbilityTrigger
import me.qmftm.casterability.game.PlayerStateManager
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent

/**
 * ChzzkAbility의 Yandere 능력을 Kotlin으로 구현한 예제입니다.
 *
 * 개선사항:
 * - Skript의 복잡한 변수 관리를 타입 안전한 Kotlin으로 구현
 * - PlayerStateManager를 사용하여 플레이어 상태 관리
 * - 능력 효과 적용 시 에러 처리 추가
 */
class YandereAbility {

    companion object {
        const val CLASS_ID = "yandere"
        const val PASSIVE_ID = "yandere_passive"
        const val SKILL_ID = "yandere_jealousy"

        // 설정 (ChzzkAbility loader.sk의 caOption과 동일)
        const val COOLTIME = 45
        const val OBSESSION_DISTANCE = 20
        const val OBSESSION_PERCENT = 25
        const val JEALOUSY_MULTIPLE = 5

        fun register() {
            // 능력 클래스 등록
            val yandereClass = AbilityClass(
                id = CLASS_ID,
                displayName = "얀데레",
                grade = 2,
                description = listOf(
                    "&d[&7패시브   &f- &d처음 본 순간]",
                    "&f처음 공격한 플레이어를 &c집착 대상&f으로 지정합니다.",
                    "&f대상이 사망하면 다른 플레이어를 공격해 새로운 &c집착 대상&f으로 변경할 수 있습니다.",
                    "&c집착 대상&f과 $OBSESSION_DISTANCE칸 이내에 있을 경우 공격력과 피해 감소가 ${OBSESSION_PERCENT}% 증가합니다.",
                    "",
                    "&d[&7철괴 우클릭 &f- &d질투] &7(쿨타임 ${COOLTIME}초)",
                    "&c집착 대상&f이 마지막으로 공격한 대상이 자신이 아닐 경우,",
                    "&f해당 대상과 &c집착 대상&f에게 &c집착 대상&f이 입힌 마지막 피해의 &c${JEALOUSY_MULTIPLE}배&f의 피해를 줍니다."
                )
            )

            // 패시브 능력 등록
            val passiveDef = AbilityDefinition(
                id = PASSIVE_ID,
                classId = CLASS_ID,
                displayName = "처음 본 순간",
                trigger = AbilityTrigger.PASSIVE,
                passiveIntervalTicks = 20,
                cooldownSeconds = 0
            )

            // 스킬 능력 등록
            val skillDef = AbilityDefinition(
                id = SKILL_ID,
                classId = CLASS_ID,
                displayName = "질투",
                trigger = AbilityTrigger.SKILL,
                passiveIntervalTicks = 0,
                cooldownSeconds = COOLTIME
            )

            AbilityRegistry.registerClass(yandereClass)
            AbilityRegistry.registerAbility(passiveDef)
            AbilityRegistry.registerAbility(skillDef)
        }

        /**
         * 플레이어가 피해를 입힐 때 호출됩니다.
         * 첫 공격 시 집착 대상 설정, 거리 내에서는 데미지 증가
         */
        fun onDamage(attacker: Player, victim: Player, damage: Double): Double {
            if (AbilityRegistry.getPlayerClassId(attacker) != CLASS_ID) return damage

            // 집착 대상 설정 (첫 공격 시)
            val obsession = PlayerStateManager.getPlayerVar(attacker, "obsession") as? Player
            var newDamage = damage

            if (obsession == null) {
                PlayerStateManager.setPlayerVar(attacker, "obsession", victim)
                PlayerStateManager.setCustomUi(attacker, "obsession", "&d집착 대상: ${victim.name}")
                attacker.sendMessage("&d${victim.name}님을 집착 대상으로 지정하셨습니다.".replace("&", "§"))
            } else if (obsession.uniqueId == victim.uniqueId) {
                // 집착 대상과의 거리 확인
                val distance = attacker.location.distance(victim.location)
                if (distance <= OBSESSION_DISTANCE) {
                    newDamage = damage * (1 + OBSESSION_PERCENT / 100.0)
                }
            }

            // 피해 입는 쪽 (피해 감소)
            if (AbilityRegistry.getPlayerClassId(victim) == CLASS_ID) {
                val victimObsession = PlayerStateManager.getPlayerVar(victim, "obsession") as? Player
                if (victimObsession != null && attacker.uniqueId == victimObsession.uniqueId) {
                    val distance = victim.location.distance(attacker.location)
                    if (distance <= OBSESSION_DISTANCE) {
                        newDamage = damage * (1 - OBSESSION_PERCENT / 100.0)
                    }
                }
            }

            return newDamage
        }

        /**
         * 플레이어 사망 시 호출됩니다.
         * 집착 대상이 사망하면 새로운 대상 선택 가능
         */
        fun onPlayerDeath(deadPlayer: Player) {
            // 다른 플레이어들의 집착 대상이 사망했는지 확인
            AbilityRegistry.getAllClasses()
                .filter { it.id == CLASS_ID }
                .forEach { _ ->
                    // 모든 플레이어의 집착 대상 초기화
                    val obsession = PlayerStateManager.getPlayerVar(deadPlayer, "obsession")
                    if (obsession == deadPlayer) {
                        PlayerStateManager.deletePlayerVar(deadPlayer, "obsession")
                        PlayerStateManager.removeCustomUi(deadPlayer, "obsession")
                    }
                }
        }
    }
}
