package me.qmftm.casterability.game

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.ability.AbilityTrigger
import me.qmftm.casterability.event.AbilityUseEvent
import me.qmftm.casterability.listener.PassiveEventBinder
import me.qmftm.casterability.util.BossBarManager
import me.qmftm.casterability.util.broadcast
import me.qmftm.casterability.util.toComponent
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class GameManager(private val plugin: CasterAbility) {

    val bossBar = BossBarManager(plugin)
    private val worldBorder = WorldBorderController(plugin, bossBar)

    var isRunning = false
        private set
    var phase: GamePhase = GamePhase.IDLE
        private set

    val gamePlayers = mutableSetOf<UUID>()

    private val drawAbility = mutableMapOf<UUID, String>()
    private val drawCount   = mutableMapOf<UUID, Int>()
    private var drawWaiting = 0

    private var spawnX = 0.0
    private var spawnZ = 0.0

    private val passiveTasks = mutableListOf<BukkitTask>()
    private val passiveEvents = PassiveEventBinder(plugin, this)

    private var gameWorld: World? = null

    // ── 월드 초기화 (onEnable 시 호출) ────────────────────

    fun prepareWorld() {
        val name = plugin.gameConfig.worldName
        gameWorld = Bukkit.getWorld(name) ?: run {
            plugin.logger.info("'$name' 월드를 찾을 수 없어 새로 생성합니다...")
            val world = Bukkit.createWorld(WorldCreator(name))
            if (world != null) plugin.logger.info("'$name' 월드 생성 완료")
            else plugin.logger.severe("'$name' 월드 생성 실패")
            world
        }
    }

    // ── 게임 시작 ─────────────────────────────────────────

    fun startGame(sender: CommandSender) {
        if (isRunning) {
            sender.sendMessage("&c이미 능력자 게임이 진행 중입니다.".toComponent())
            return
        }
        val cfg = plugin.gameConfig

        isRunning = true
        phase = GamePhase.STARTING
        gamePlayers.clear()
        drawAbility.clear()
        drawCount.clear()
        AbilityRegistry.clearAll()
        AbilityRegistry.setWreckPercent(cfg.wreck)

        broadcastIntro()

        // 3초 후 추첨 시작
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            phase = GamePhase.DRAWING
            for (p in Bukkit.getOnlinePlayers()) {
                gamePlayers.add(p.uniqueId)
                val ab = AbilityRegistry.getAllClasses().randomOrNull()?.id
                drawAbility[p.uniqueId] = ab ?: ""
                drawCount[p.uniqueId]   = cfg.abilityChangeCount
                plugin.guiManager.openDrawGui(p)
            }
            drawWaiting = gamePlayers.size

            bossBar.startTimer(
                "ca.auto_skip", "&a능력 선택", cfg.autoSkipSecond,
                BossBar.Color.BLUE, colorChange = false, broadcast = false, titleChange = true
            ) { autoSkipDraw() }
        }, 60L)
    }

    private fun autoSkipDraw() {
        if (!isRunning) return
        for (uid in gamePlayers) {
            val p = Bukkit.getPlayer(uid) ?: continue
            if (AbilityRegistry.getPlayerClass(p) == null) {
                val ab = drawAbility[uid] ?: continue
                if (ab.isNotEmpty()) AbilityRegistry.setPlayerClass(p, ab)
            }
        }
        broadcast("&c선택하지 않은 플레이어들의 능력이 자동으로 선택되었습니다.")
        afterStart()
    }

    fun afterStart() {
        if (!isRunning) return
        broadcast("&e모든 플레이어가 능력을 선택하였습니다. 게임이 곧 시작됩니다...")
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            phase = GamePhase.WORLD_GENERATING
            setupWorld()
        }, 20L)
    }

    // ── 월드 설정 ─────────────────────────────────────────

    private fun setupWorld() {
        val cfg   = plugin.gameConfig
        val world = gameWorld ?: run {
            plugin.logger.severe("게임 월드(${cfg.worldName})를 찾을 수 없습니다.")
            stopGame(Bukkit.getConsoleSender())
            return
        }

        // 바다·강·사막을 피해 중심을 고른다 (ChzzkAbility World.sk)
        val center = SpawnLocator.findWorldCenter(world, sampleRadius = cfg.worldborderMaxRadius)
        spawnX = center.x
        spawnZ = center.z

        for (uid in gamePlayers) {
            val p = Bukkit.getPlayer(uid) ?: continue
            val loc = if (cfg.randomSpawn) {
                SpawnLocator.findSpawn(world, spawnX, spawnZ, cfg.randomRadius)
            } else center

            // 텔레포트 전에 청크를 불러온다 (ChzzkAbility teleport.sk)
            world.getChunkAt(loc)
            p.teleport(loc)
            p.getAttribute(Attribute.MAX_HEALTH)?.baseValue = cfg.basicHealth.toDouble()
            p.health = cfg.basicHealth.toDouble()
            p.foodLevel = 20
            p.level = cfg.basicLevel
        }

        broadcast("&a능력자 게임이 시작되었습니다!")
        phase = GamePhase.INVINCIBILITY
        startInvincibility()
    }

    // ── 무적 시간 ─────────────────────────────────────────

    private fun startInvincibility() {
        val cfg = plugin.gameConfig
        if (!cfg.invincibilityEnable) {
            goInGame()
            return
        }
        // ChzzkAbility 개선: 무적 시간 시작 시에만 숨김 적용 (반복 제거)
        if (cfg.invincibilityInvisible) {
            gamePlayers.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
                p.addPotionEffect(PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    cfg.invincibilitySecond * 20, 0, false, false))
            }
        }
        timedAction(
            "ca.invincibility", "&b무적 시간", cfg.invincibilitySecond,
            BossBar.Color.BLUE, cfg.invincibilityShowBossbar, colorChange = true, broadcastCountdown = true
        ) {
            // ChzzkAbility 개선: 무적 종료 시 숨김 해제
            gamePlayers.mapNotNull { Bukkit.getPlayer(it) }
                .forEach { it.removePotionEffect(PotionEffectType.INVISIBILITY) }
            goInGame()
        }
    }

    private fun goInGame() {
        phase = GamePhase.IN_GAME
        startCooldownTicker()
        startPassiveTasks()
        startWorldBorderShrink()
    }

    // ── 게임 내 스케줄러 ──────────────────────────────────

    private fun startCooldownTicker() {
        // 쿨타임(매 틱) · 이펙트(2틱) · 액션바 UI(2틱)를 GameScheduler가 모두 관리한다.
        // 태스크는 GameScheduler가 들고 있다가 stopGame()에서 취소된다.
        GameScheduler.start(plugin) {
            if (!isRunning || phase != GamePhase.IN_GAME) emptyList()
            else gamePlayers.mapNotNull { Bukkit.getPlayer(it) }
        }
    }

    private fun startPassiveTasks() {
        // event: 로 정의한 패시브는 주기가 아니라 이벤트로 발동한다
        passiveEvents.bindAll()

        AbilityRegistry.getAllAbilities()
            .filter { it.trigger == AbilityTrigger.PASSIVE && !it.isEventDriven }
            .forEach { def ->
                val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                    if (!isRunning || phase != GamePhase.IN_GAME) return@Runnable
                    gamePlayers.mapNotNull { Bukkit.getPlayer(it) }
                        .filter { AbilityRegistry.getPlayerClassId(it) == def.classId }
                        .filter { !AbilityRegistry.isAbilityDisabled(it, def.id) }
                        .forEach { p ->
                            if (!AbilityRegistry.isOnCooldown(p, def.id)) {
                                val event = AbilityUseEvent(p, def, AbilityTrigger.PASSIVE)
                                Bukkit.getPluginManager().callEvent(event)
                                if (def.cooldownSeconds > 0)
                                    AbilityRegistry.startCooldown(p, def.id, def.cooldownSeconds)
                            }
                        }
                }, 0L, def.passiveIntervalTicks)
                passiveTasks.add(task)
            }
    }

    // ── 월드보더 수축 ─────────────────────────────────────

    private fun startWorldBorderShrink() {
        val world = gameWorld ?: return
        worldBorder.start(
            world = world,
            startX = spawnX,
            startZ = spawnZ,
            cfg = plugin.gameConfig,
            isRunning = { isRunning && phase == GamePhase.IN_GAME },
        ) { newX, newZ ->
            // 랜덤 스폰 기준점을 새 중앙으로 옮긴다
            spawnX = newX
            spawnZ = newZ
        }
    }

    // ── 게임 종료 ─────────────────────────────────────────

    fun stopGame(sender: CommandSender) {
        if (!isRunning) {
            sender.sendMessage("&c능력자 게임이 진행 중이지 않습니다.".toComponent())
            return
        }
        isRunning = false
        phase = GamePhase.IDLE

        passiveTasks.forEach { it.cancel() }
        passiveTasks.clear()
        passiveEvents.unbindAll()
        // 쿨타임/이펙트/UI 태스크 취소 — 액션바를 지우기 전에 멈춰야 다시 그려지지 않는다
        GameScheduler.stop()
        worldBorder.stop()

        gamePlayers.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
            p.closeInventory()
            p.removePotionEffect(PotionEffectType.INVISIBILITY)
            p.sendActionBar("".toComponent())
        }
        AbilityRegistry.clearAll()
        PlayerStateManager.clearAll()
        GameUIRenderer.clearAll()
        gamePlayers.clear()
        bossBar.deleteAll()

        gameWorld?.worldBorder?.reset()
        broadcast("&f관리자 &e${sender.name}&f님에 의해 능력자 게임이 종료되었습니다.")
    }

    // ── 추첨 GUI 연동 ─────────────────────────────────────

    fun rerollAbility(p: Player): String? {
        val left = drawCount[p.uniqueId] ?: 0
        if (left <= 0) return null
        drawCount[p.uniqueId] = left - 1
        val ab = AbilityRegistry.getAllClasses().randomOrNull()?.id ?: return null
        drawAbility[p.uniqueId] = ab
        return ab
    }

    fun confirmAbility(p: Player) {
        val classId = drawAbility[p.uniqueId] ?: return
        AbilityRegistry.setPlayerClass(p, classId)
        drawAbility.remove(p.uniqueId)
        p.closeInventory()
        drawWaiting--
        broadcast("&e${p.name}&a님이 능력 선택을 완료하셨습니다. (게임 시작까지: ${drawWaiting}명)")
        if (drawWaiting <= 0) {
            bossBar.delete("ca.auto_skip")
            afterStart()
        }
    }

    fun getDrawAbility(p: Player) = drawAbility[p.uniqueId]
    fun getDrawCount(p: Player)   = drawCount[p.uniqueId] ?: 0

    // ── 내부 유틸 ─────────────────────────────────────────

    /**
     * showBossbar=true 이면 BossBarManager 타이머를 사용하고,
     * false 이면 단순 runTaskLater 로 지연 후 onFinish 호출.
     */
    private fun timedAction(
        id: String,
        title: String,
        seconds: Int,
        color: BossBar.Color,
        showBossbar: Boolean,
        colorChange: Boolean,
        broadcastCountdown: Boolean,
        onFinish: () -> Unit,
    ) {
        if (showBossbar) {
            bossBar.startTimer(id, title, seconds, color, colorChange, broadcastCountdown, true, onFinish)
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable(onFinish), (seconds * 20).toLong())
        }
    }

    private fun broadcastIntro() {
        broadcast("&e----------------------------------")
        broadcast("&aCasterAbility &f- &7능력자 전쟁")
        broadcast("&b누구나 개발할 수 있는 능력자 전쟁")
        broadcast("&e----------------------------------")
    }
}