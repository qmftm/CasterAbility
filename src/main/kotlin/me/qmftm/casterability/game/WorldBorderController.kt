package me.qmftm.casterability.game

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.config.GameConfig
import me.qmftm.casterability.util.BossBarManager
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.scheduler.BukkitTask
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 월드보더 수축을 진행합니다. ChzzkAbility의 border.sk 포팅입니다.
 *
 * 원본의 의도(코사인 이징으로 처음과 끝은 천천히, 중간은 빠르게)는 유지하되
 * 다음을 고쳤습니다.
 *
 * - 원본은 수축량을 `{_shrink::n} = max - value + {_shrink::n-2}` 로 누적했습니다.
 *   두 칸 전 값을 더하는 바람에 단계별 수축량이 들쭉날쭉하고 최종 반지름이
 *   min과 맞지 않았습니다. 여기서는 각 단계의 목표 반지름을 직접 구하고
 *   그 차이를 수축량으로 씁니다. count 단계를 마치면 정확히 min이 됩니다.
 * - 원본은 다음 중앙을 현재 중앙 ± 수축량 범위에서 뽑아 보더 밖으로 나갈 수
 *   있었습니다. 목표 반지름 안쪽에서만 뽑습니다.
 * - 원본은 wait 루프 안에서 `stop`으로 함수 전체를 빠져나가 보스바가 남았습니다.
 *   여기서는 stop()이 태스크와 보스바를 함께 정리합니다.
 */
class WorldBorderController(
    private val plugin: CasterAbility,
    private val bossBar: BossBarManager,
) {

    companion object {
        private const val BAR_WAIT   = "ca.worldborder.wait"
        private const val BAR_SHRINK = "ca.worldborder.shrink"

        /** 초당 줄어드는 반지름(블록) */
        private const val SPEED = 2.0
    }

    private val tasks = mutableListOf<BukkitTask>()
    private var running = false

    private var centerX = 0.0
    private var centerZ = 0.0

    /**
     * @param isRunning  게임이 아직 진행 중인지. false가 되면 스스로 멈춥니다.
     * @param onCenterMoved 중앙이 바뀔 때마다 호출 (랜덤 스폰 기준점 갱신용)
     */
    fun start(
        world: World,
        startX: Double,
        startZ: Double,
        cfg: GameConfig,
        isRunning: () -> Boolean,
        onCenterMoved: (Double, Double) -> Unit,
    ) {
        if (!cfg.worldborderEnable) return
        stop()

        running = true
        centerX = startX
        centerZ = startZ

        val max   = cfg.worldborderMaxRadius.toDouble()
        val min   = cfg.worldborderMinRadius.toDouble().coerceAtMost(max)
        val count = cfg.worldborderShrinkCount.coerceAtLeast(1)

        val border = world.worldBorder
        border.setCenter(centerX, centerZ)
        border.size = max * 2
        border.damageAmount = 0.5
        border.damageBuffer = 0.0
        border.warningDistance = 15

        // 각 단계가 끝났을 때의 목표 반지름 (코사인 이징)
        val radii = (0..count).map { n ->
            val t = n.toDouble() / count
            max - (max - min) * (1 - cos(t * PI)) / 2
        }

        runStep(world, cfg, radii, 1, count, isRunning, onCenterMoved)
    }

    fun stop() {
        running = false
        tasks.forEach { it.cancel() }
        tasks.clear()
        bossBar.delete(BAR_WAIT)
        bossBar.delete(BAR_SHRINK)
    }

    // ── 단계 진행 ─────────────────────────────────────────

    private fun runStep(
        world: World,
        cfg: GameConfig,
        radii: List<Double>,
        step: Int,
        count: Int,
        isRunning: () -> Boolean,
        onCenterMoved: (Double, Double) -> Unit,
    ) {
        if (!running || !isRunning()) { stop(); return }
        if (step > count) {
            bossBar.delete(BAR_WAIT)
            bossBar.delete(BAR_SHRINK)
            return
        }

        val from = radii[step - 1]
        val to   = radii[step]
        val amount = (from - to).coerceAtLeast(0.0)

        // 다음 중앙: 수축 후 반지름 안쪽에서만 뽑아 보더 밖으로 나가지 않게 한다
        val (goalX, goalZ) = if (cfg.worldborderShrinkRandomCenter && to > 1.0) {
            val span = (from - to).coerceAtMost(to)
            centerX + Random.nextDouble(-span, span) to centerZ + Random.nextDouble(-span, span)
        } else centerX to centerZ

        waitPhase(cfg, to, goalX, goalZ, isRunning) {
            shrinkPhase(world, cfg, amount, to, goalX, goalZ, isRunning) {
                onCenterMoved(centerX, centerZ)
                runStep(world, cfg, radii, step + 1, count, isRunning, onCenterMoved)
            }
        }
    }

    /** 수축 전 대기. 남은 시간과 다음 중앙을 보스바에 보여준다. */
    private fun waitPhase(
        cfg: GameConfig,
        nextRadius: Double,
        goalX: Double,
        goalZ: Double,
        isRunning: () -> Boolean,
        onDone: () -> Unit,
    ) {
        val seconds = cfg.worldborderShrinkSecond.coerceAtLeast(1)
        if (!cfg.worldborderShowBossbar) {
            schedule(seconds * 20L) { if (running && isRunning()) onDone() else stop() }
            return
        }

        bossBar.create(BAR_WAIT, "&c월드보더 수축까지", BossBar.Color.WHITE)
        var remaining = seconds

        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!running || !isRunning()) { stop(); return@Runnable }

            bossBar.refresh(BAR_WAIT)
            bossBar.setName(
                BAR_WAIT,
                "&7다음 크기: &c${round5(nextRadius)} &f| &7수축까지: &c${remaining}초 " +
                "&f| &7다음 중앙: &c(${goalX.roundToInt()}, ${goalZ.roundToInt()})"
            )
            bossBar.setProgress(BAR_WAIT, remaining.toFloat() / seconds)

            remaining--
            if (remaining < 0) {
                bossBar.delete(BAR_WAIT)
                onDone()
            }
        }, 0L, 20L)
        tasks.add(task)
    }

    /** 실제 수축. 랜덤 중앙이면 중앙을 조금씩 옮기면서 줄인다. */
    private fun shrinkPhase(
        world: World,
        cfg: GameConfig,
        amount: Double,
        targetRadius: Double,
        goalX: Double,
        goalZ: Double,
        isRunning: () -> Boolean,
        onDone: () -> Unit,
    ) {
        if (!running || !isRunning()) { stop(); return }

        val border = world.worldBorder
        val seconds = (amount / SPEED).roundToInt().coerceAtLeast(1)

        world.players.forEach {
            it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.85f)
        }

        // 크기 전환은 서버에 맡기고, 중앙만 직접 보간한다
        border.changeSize(targetRadius * 2, seconds.toLong())

        val fromX = centerX
        val fromZ = centerZ
        val totalTicks = seconds * 20
        var elapsed = 0

        if (cfg.worldborderShowBossbar) {
            bossBar.create(BAR_SHRINK, "&c월드보더 수축 중", BossBar.Color.RED)
        }

        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!running || !isRunning()) { stop(); return@Runnable }

            elapsed++
            val progress = (elapsed.toDouble() / totalTicks).coerceIn(0.0, 1.0)

            centerX = fromX + (goalX - fromX) * progress
            centerZ = fromZ + (goalZ - fromZ) * progress
            border.setCenter(centerX, centerZ)

            if (cfg.worldborderShowBossbar && elapsed % 20 == 0) {
                bossBar.refresh(BAR_SHRINK)
                bossBar.setName(
                    BAR_SHRINK,
                    "&7자기장 크기: &c${round5(border.size / 2)} &f| &7진행률: &c${(progress * 100).roundToInt()}%"
                )
                bossBar.setProgress(BAR_SHRINK, progress.toFloat())
            }

            if (elapsed >= totalTicks) {
                // 보간 오차 제거
                centerX = goalX
                centerZ = goalZ
                border.setCenter(centerX, centerZ)
                border.size = targetRadius * 2

                bossBar.delete(BAR_SHRINK)
                world.players.forEach {
                    it.playSound(it.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                }
                cancelTask()
                onDone()
            }
        }, 1L, 1L)
        tasks.add(task)
        currentTask = task
    }

    // ── 유틸 ──────────────────────────────────────────────

    private var currentTask: BukkitTask? = null

    private fun cancelTask() {
        currentTask?.let { it.cancel(); tasks.remove(it) }
        currentTask = null
    }

    private fun schedule(delayTicks: Long, action: () -> Unit) {
        tasks.add(plugin.server.scheduler.runTaskLater(plugin, Runnable(action), delayTicks))
    }

    /** 5 단위로 반올림해서 보여주기 (원본 border.sk와 동일) */
    private fun round5(value: Double): Int = (value / 5).roundToInt() * 5
}
