package me.qmftm.casterability.skript

import me.qmftm.casterability.skript.condition.CondHasAbility
import me.qmftm.casterability.skript.condition.CondHasAbilityClass
import me.qmftm.casterability.skript.condition.CondOnCooldown
import me.qmftm.casterability.skript.effect.*
import me.qmftm.casterability.skript.expression.*

/**
 * 플러그인 시작 시 여기서 모든 Skript 구문을 한번에 등록합니다.
 */
object SkriptRegistry {

    fun registerAll() {
        // ── 능력 정의 (DSL 블록) ──────────────────────────
        EffAbilityClassDef.register()
        EffAbilityDef.register()

        // ── 이벤트 ───────────────────────────────────────
        EvtAbilityUse.register()

        // ── Effect (능력 조작) ────────────────────────────
        EffSetAbilityClass.register()
        EffRemoveAbility.register()
        EffSetCooldown.register()
        EffResetCooldown.register()

        // ── Expression (값 읽기) ──────────────────────────
        ExprAbilityClassId.register()
        ExprAbilityClassName.register()
        ExprAbilityClassTier.register()
        ExprAbilityCooldown.register()

        // ── Condition (조건) ──────────────────────────────
        CondHasAbilityClass.register()
        CondOnCooldown.register()
        CondHasAbility.register()
    }
}
