package me.qmftm.casterability.game

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

/**
 * 게임 중 실행되는 스케줄러를 관리합니다.
 *
 * ChzzkAbility 문제점 해결:
 * - scheduler.sk의 UI 중복 업데이트 제거 (2 ticks 버전만 사용)
 * - 효과 틱 처리와 쿨타임 처리 분리
 * - 메모리 누수 방지를 위한 명시적 정리
 */
object GameScheduler {

    private var effectTask: BukkitTask? = null
    private var cooldownTask: BukkitTask? = null
    private var uiTask: BukkitTask? = null

    /**
     * 게임 IN_GAME 단계에서 호출되어 주기적인 작업을 시작합니다.
     */
    fun startSchedulers(plugin: CasterAbility) {
        // ── 이펙트 처리 (2 틱마다) ──────────────────────────
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            PlayerStateManager.tickEffects()
        }, 0L, 2L)

        // ── 쿨타임 처리 (20 틱마다 = 1초) ──────────────────
        cooldownTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            AbilityRegistry.tickCooldowns()
        }, 0L, 20L)
    }

    /**
     * 모든 스케줄러를 중지합니다.
     */
    fun stopSchedulers() {
        effectTask?.cancel()
        cooldownTask?.cancel()
        uiTask?.cancel()

        effectTask = null
        cooldownTask = null
        uiTask = null
    }

    /**
     * 스케줄러가 실행 중인지 확인합니다.
     */
    fun isRunning(): Boolean = effectTask != null || cooldownTask != null
}
