package me.qmftm.casterability.skript

import me.qmftm.casterability.skript.condition.CondAbilityDisabled
import me.qmftm.casterability.skript.condition.CondHasAbility
import me.qmftm.casterability.skript.condition.CondHasAbilityClass
import me.qmftm.casterability.skript.condition.CondHasAbilityId
import me.qmftm.casterability.skript.condition.CondOnCooldown
import me.qmftm.casterability.skript.effect.*
import me.qmftm.casterability.skript.expression.*
import ch.njol.skript.registrations.EventValues
import me.qmftm.casterability.event.AbilityUseEvent
import me.qmftm.casterability.event.StatusEffectApplyEvent
import me.qmftm.casterability.event.StatusEffectExpireEvent
import me.qmftm.casterability.skript.condition.CondHasStatusEffect
import me.qmftm.casterability.skript.section.SecAbility
import me.qmftm.casterability.skript.section.SecAbilityClass
import org.bukkit.entity.Entity
import org.skriptlang.skript.lang.converter.Converter

/**
 * 플러그인 시작 시 여기서 모든 Skript 구문을 한번에 등록합니다.
 */
object SkriptRegistry {

    fun registerAll() {
        // ── 능력 정의 (DSL 블록) ──────────────────────────
        // 들여쓴 블록을 받으므로 Effect가 아니라 Section으로 등록한다
        SecAbilityClass.register()
        SecAbility.register()

        // ── 이벤트 ───────────────────────────────────────
        EvtAbilityUse.register()
        EvtStatusEffectApply.register()
        EvtStatusEffectExpire.register()

        // ── Effect (능력 조작) ────────────────────────────
        EffSetAbilityClass.register()
        EffRemoveAbility.register()
        EffSetCooldown.register()
        EffResetCooldown.register()
        EffResetAllCooldowns.register()
        EffToggleAbility.register()
        EffTriggerAbility.register()

        // ── Effect (상태이상) ─────────────────────────────
        EffApplyStatusEffect.register()
        EffRemoveStatusEffect.register()
        EffStatusEffectName.register()

        // ── Effect (액션바 부가 정보) ─────────────────────
        EffSetAbilityInfo.register()
        EffClearAbilityInfo.register()

        // ── Expression (값 읽기) ──────────────────────────
        ExprAbilityClassId.register()
        ExprAbilityClassName.register()
        ExprAbilityClassTier.register()
        ExprAbilityCooldown.register()
        ExprAbilityDamage.register()

        // 등록된 능력 자체의 정보
        ExprClassNameOf.register()
        ExprClassTierOf.register()
        ExprTierDisplay.register()
        ExprAllClasses.register()
        ExprAllAbilities.register()
        ExprAbilitiesOf.register()
        ExprPlayersWithClass.register()
        ExprAbilityTrigger.register()
        ExprAbilityName.register()
        ExprAbilityEvent.register()
        ExprAbilityItem.register()
        ExprAbilityMaxCooldown.register()

        // 상태이상
        ExprStatusEffectTime.register()
        ExprStatusEffectsOf.register()
        ExprStatusEffectDuration.register()

        // ── Condition (조건) ──────────────────────────────
        CondHasAbilityClass.register()
        CondOnCooldown.register()
        CondHasAbility.register()
        CondHasAbilityId.register()
        CondAbilityDisabled.register()
        CondHasStatusEffect.register()

        registerEventValues()
    }

    /**
     * ability use 이벤트 안에서 쓸 수 있는 값들.
     *
     *   event-entity → 대상 (on_hit이면 맞은 쪽, on_damaged면 때린 쪽)
     *   event-number → 피해량
     *   event-string → 발동한 능력 id
     *
     * player는 AbilityUseEvent가 PlayerEvent를 상속하므로 Skript가 알아서 제공합니다.
     */
    private fun registerEventValues() {
        EventValues.registerEventValue(
            AbilityUseEvent::class.java, Entity::class.java,
            Converter { it.target }
        )
        EventValues.registerEventValue(
            AbilityUseEvent::class.java, Number::class.java,
            Converter { it.damage }
        )
        EventValues.registerEventValue(
            AbilityUseEvent::class.java, String::class.java,
            Converter { it.abilityId }
        )

        // 상태이상 이벤트: event-string → 상태이상 id, event-number → 걸릴 시간(tick)
        EventValues.registerEventValue(
            StatusEffectApplyEvent::class.java, String::class.java,
            Converter { it.effectId }
        )
        EventValues.registerEventValue(
            StatusEffectApplyEvent::class.java, Number::class.java,
            Converter { it.durationTicks }
        )
        EventValues.registerEventValue(
            StatusEffectExpireEvent::class.java, String::class.java,
            Converter { it.effectId }
        )
    }
}
