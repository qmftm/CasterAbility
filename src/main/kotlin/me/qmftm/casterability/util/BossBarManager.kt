package me.qmftm.casterability.util

import me.qmftm.casterability.CasterAbility
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

class BossBarManager(private val plugin: CasterAbility) {

    private val bars  = mutableMapOf<String, BossBar>()
    private val tasks = mutableMapOf<String, BukkitTask>()

    fun create(id: String, name: String, color: BossBar.Color, overlay: BossBar.Overlay = BossBar.Overlay.PROGRESS) {
        delete(id)
        val bar = BossBar.bossBar(
            LegacyComponentSerializer.legacyAmpersand().deserialize(name),
            1f, color, overlay
        )
        bars[id] = bar
        Bukkit.getOnlinePlayers().forEach { it.showBossBar(bar) }
    }

    fun delete(id: String) {
        bars.remove(id)?.let { bar -> Bukkit.getOnlinePlayers().forEach { it.hideBossBar(bar) } }
        tasks.remove(id)?.cancel()
    }

    fun deleteAll() {
        bars.keys.toList().forEach { delete(it) }
    }

    /** 중간에 접속한 플레이어에게도 보이도록 다시 표시한다. */
    fun refresh(id: String) {
        val bar = bars[id] ?: return
        Bukkit.getOnlinePlayers().forEach { it.showBossBar(bar) }
    }

    fun setName(id: String, name: String) {
        bars[id]?.name(LegacyComponentSerializer.legacyAmpersand().deserialize(name))
    }

    fun setColor(id: String, color: BossBar.Color) { bars[id]?.color(color) }

    fun setProgress(id: String, percent: Float) {
        bars[id]?.progress(percent.coerceIn(0f, 1f))
    }

    fun startTimer(
        id: String,
        name: String,
        seconds: Int,
        color: BossBar.Color,
        colorChange: Boolean,
        broadcast: Boolean,
        titleChange: Boolean,
        onFinish: () -> Unit,
    ) {
        create(id, name, color)
        val total = seconds
        val remaining = intArrayOf(seconds)

        // ChzzkAbility caTimerBossbar: 75/50/25% 지점을 한 번씩 안내하고, 마지막 3틱은 매초 안내한다.
        // 남은 %가 각 문턱 아래로 처음 내려가는 순간에만 방송하도록 다음 문턱을 하나씩 지워나간다.
        val stageThresholds = ArrayDeque(listOf(75, 50, 25))

        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!bars.containsKey(id)) return@Runnable

            Bukkit.getOnlinePlayers().forEach { bars[id]?.let { b -> it.showBossBar(b) } }

            if (titleChange) setName(id, "$name &f: ${remaining[0]}초 남음")

            val progress = remaining[0].toFloat() / total
            setProgress(id, progress)

            if (colorChange) {
                val pct = progress * 100
                setColor(id, when {
                    pct >= 75 -> BossBar.Color.BLUE
                    pct >= 50 -> BossBar.Color.GREEN
                    pct >= 25 -> BossBar.Color.YELLOW
                    else      -> BossBar.Color.RED
                })
            }

            if (broadcast) {
                val pct = progress * 100
                val stageMsg = when (remaining[0]) {
                    3, 2, 1 -> null // 마지막 3초는 아래에서 따로 처리
                    else -> {
                        if (stageThresholds.isNotEmpty() && pct <= stageThresholds.first()) {
                            stageThresholds.removeFirst()
                            "§b${remaining[0]}"
                        } else null
                    }
                }
                val countdownMsg = when (remaining[0]) {
                    3 -> "§e3"
                    2 -> "§62"
                    1 -> "§c1"
                    else -> null
                }
                (stageMsg ?: countdownMsg)?.let { time ->
                    Bukkit.broadcast(
                        "${name.replace("&", "§")}§a이(가) $time§a초 남았습니다.".toSectionComponent()
                    )
                }
            }

            remaining[0]--
            if (remaining[0] < 0) {
                delete(id)
                onFinish()
            }
        }, 0L, 20L)

        tasks[id] = task
    }
}
