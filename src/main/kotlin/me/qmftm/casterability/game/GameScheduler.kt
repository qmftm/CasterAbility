package me.qmftm.casterability.game

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/**
 * 게임 중 실행되는 주기 작업을 한 곳에서 관리합니다.
 *
 * ChzzkAbility scheduler.sk 대비 개선점:
 * - UI 갱신이 2틱/20틱 두 곳에서 중복 실행되던 것을 2틱 한 곳으로 통합
 * - 상태이상과 쿨타임은 tick 단위라 매 틱, UI는 2틱마다로 주기를 나눔
 * - 모든 태스크를 필드로 들고 있다가 stop()에서 확실히 취소 (태스크 누수 방지)
 */
object GameScheduler {

    private var effectTask: BukkitTask? = null
    private var cooldownTask: BukkitTask? = null
    private var uiTask: BukkitTask? = null

    /**
     * @param players 매 실행 시점의 대상 플레이어를 돌려주는 람다.
     *                게임이 IN_GAME이 아니면 빈 리스트를 돌려주면 된다.
     */
    fun start(plugin: CasterAbility, players: () -> List<Player>) {
        stop()
        val scheduler = plugin.server.scheduler

        // 상태이상: 남은 시간을 tick 단위로 들고 호출당 1씩 깎으므로 매 틱 실행.
        // 상태이상에 붙은 홀로그램도 여기서 같이 따라다니게 한다.
        effectTask = scheduler.runTaskTimer(plugin, Runnable {
            PlayerStateManager.tickEffects()
            EffectHologramManager.follow()
        }, 0L, 1L)

        // 쿨타임: AbilityRegistry가 '틱' 단위로 저장하고 호출당 1씩 깎으므로 매 틱 실행.
        // (20틱 주기로 돌리면 쿨타임이 20배로 늘어난다)
        cooldownTask = scheduler.runTaskTimer(plugin, Runnable {
            AbilityRegistry.tickCooldowns()
        }, 0L, 1L)

        // 액션바 UI: 2틱 주기 한 곳에서만 갱신
        uiTask = scheduler.runTaskTimer(plugin, Runnable {
            players().forEach { GameUIRenderer.updateActionBar(it) }
        }, 0L, 2L)
    }

    fun stop() {
        effectTask?.cancel()
        cooldownTask?.cancel()
        uiTask?.cancel()
        effectTask = null
        cooldownTask = null
        uiTask = null
    }

    val isRunning: Boolean get() = effectTask != null
}
