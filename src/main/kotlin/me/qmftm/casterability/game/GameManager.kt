package me.qmftm.casterability.game

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.config.GameConfig
import me.qmftm.casterability.ability.AbilityTrigger
import me.qmftm.casterability.event.AbilityUseEvent
import me.qmftm.casterability.event.GameEndEvent
import me.qmftm.casterability.event.GameStartEvent
import me.qmftm.casterability.listener.PassiveEventBinder
import me.qmftm.casterability.util.BossBarManager
import me.qmftm.casterability.util.broadcast
import me.qmftm.casterability.util.toComponent
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.io.File
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

    /** 실제로 뜨는 게임 월드 이름. worldName 자체는 손대지 않는 원본(템플릿)이다. */
    private fun gameWorldName(cfg: GameConfig) = "${cfg.worldName}-game"

    // ── 게임 시작 ─────────────────────────────────────────

    fun startGame(sender: CommandSender) {
        if (isRunning) {
            sender.sendMessage("&c이미 능력자 게임이 진행 중입니다.".toComponent())
            (sender as? Player)?.playSound(sender.location, Sound.ENTITY_VILLAGER_NO, 0.5f, 0.85f)
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
            prepareGameWorld { world ->
                if (!isRunning) return@prepareGameWorld
                if (world == null) {
                    broadcast("&c게임 월드를 준비하지 못했습니다. 콘솔 로그를 확인하세요.")
                    stopGame(Bukkit.getConsoleSender())
                    return@prepareGameWorld
                }
                gameWorld = world
                setupWorld(world)
            }
        }, 20L)
    }

    // ── 월드 준비 (템플릿 복제 또는 새로 생성) ─────────────

    /**
     * `worldName` 폴더(템플릿)가 있으면 그걸 `worldName-game` 으로 통째로 복제해서 씁니다.
     * 없으면 `worldName-game` 을 새로 생성합니다. 어느 쪽이든 원본은 건드리지 않습니다.
     *
     * 파일 복사는 서버 스레드를 막지 않도록 비동기로 돌리고,
     * 월드를 실제로 불러오는 시점(`Bukkit.createWorld`)만 메인 스레드로 돌아옵니다.
     */
    private fun prepareGameWorld(onReady: (World?) -> Unit) {
        val cfg      = plugin.gameConfig
        val gameName = gameWorldName(cfg)

        // 크래시 등으로 이전 게임 월드가 아직 떠 있으면 먼저 내린다
        Bukkit.getWorld(gameName)?.let { teardownWorldFiles(it) { loadGameWorld(gameName, cfg, onReady) } }
            ?: loadGameWorld(gameName, cfg, onReady)
    }

    private fun loadGameWorld(gameName: String, cfg: GameConfig, onReady: (World?) -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val container = Bukkit.getWorldContainer()
            val gameFolder = File(container, gameName)
            if (gameFolder.exists()) gameFolder.deleteRecursively()

            val templateFolder = File(container, cfg.worldName)
            if (templateFolder.isDirectory) {
                plugin.logger.info("'${cfg.worldName}' 템플릿을 '$gameName' 으로 복제합니다...")
                templateFolder.copyRecursively(gameFolder, overwrite = true) { file, ex ->
                    // session.lock 은 잠겨 있을 수 있으니 실패해도 건너뛴다 — 없어도 로드에 지장 없다
                    if (file.name == "session.lock") OnErrorAction.SKIP
                    else { plugin.logger.warning("복제 실패: ${file.path} (${ex.message})"); OnErrorAction.SKIP }
                }
            } else {
                plugin.logger.info("'${cfg.worldName}' 템플릿이 없어 '$gameName' 을 새로 생성합니다...")
            }

            Bukkit.getScheduler().runTask(plugin, Runnable {
                val world = Bukkit.createWorld(WorldCreator(gameName))
                if (world == null) plugin.logger.severe("게임 월드('$gameName') 로드/생성 실패")
                onReady(world)
            })
        })
    }

    /** 월드를 내리고 폴더도 지운다. 안에 남은 플레이어는 다른 월드 스폰으로 옮긴다. */
    private fun teardownWorldFiles(world: World, onDone: () -> Unit = {}) {
        val fallback = Bukkit.getWorlds().firstOrNull { it != world }
        if (fallback != null) world.players.forEach { it.teleport(fallback.spawnLocation) }

        val name = world.name
        if (!Bukkit.unloadWorld(world, false)) {
            plugin.logger.warning("월드('$name')를 내리지 못했습니다 — 안에 플레이어가 남아 있을 수 있습니다.")
            onDone()
            return
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val folder = File(Bukkit.getWorldContainer(), name)
            if (folder.exists() && !folder.deleteRecursively()) {
                plugin.logger.warning("월드 폴더('$name')를 지우지 못했습니다.")
            }
            Bukkit.getScheduler().runTask(plugin, Runnable(onDone))
        })
    }

    // ── 월드 설정 ─────────────────────────────────────────

    private fun setupWorld(world: World) {
        val cfg = plugin.gameConfig

        // 바다·강·사막을 피해 중심을 고른다 (ChzzkAbility World.sk)
        val center = SpawnLocator.findWorldCenter(world, sampleRadius = cfg.worldborderMaxRadius)
        spawnX = center.x
        spawnZ = center.z

        countdownToTeleport(world, cfg, center, 3)
    }

    /** 텔레포트 전 3·2·1초 카운트다운 (ChzzkAbility 스타일 안내). */
    private fun countdownToTeleport(world: World, cfg: GameConfig, center: Location, secondsLeft: Int) {
        if (!isRunning) return
        if (secondsLeft <= 0) {
            teleportAndStart(world, cfg, center)
            return
        }
        broadcast("&e${secondsLeft}초 후 게임이 시작됩니다.")
        Bukkit.getOnlinePlayers().forEach {
            it.playSound(it.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        }
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            countdownToTeleport(world, cfg, center, secondsLeft - 1)
        }, 20L)
    }

    private fun teleportAndStart(world: World, cfg: GameConfig, center: Location) {
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

        gamePlayers.mapNotNull { Bukkit.getPlayer(it) }.forEach {
            it.playSound(it.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1f)
        }
        broadcast("&a능력자 게임이 시작되었습니다!")
        broadcastActiveOptions(cfg)
        phase = GamePhase.INVINCIBILITY
        startInvincibility()
    }

    /** 어떤 옵션이 켜져 있는지 안내 (ChzzkAbility Game.sk 포팅, 원본 포트에서 빠져 있었다). */
    private fun broadcastActiveOptions(cfg: GameConfig) {
        if (cfg.weatherClear)     broadcast("&a날씨가 &e맑음&a으로 고정됩니다")
        if (cfg.infinityDuration) broadcast("&c내구도 무제한&a이 적용됩니다")
        if (cfg.infinityHunger)   broadcast("&3배고픔 무제한&a이 적용됩니다")
        if (cfg.cooldownShield)   broadcast("&e방패 쿨타임&a 적용됨")
        if (cfg.cooldownBow)      broadcast("&6활 쿨타임&a 적용됨")
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
        Bukkit.getPluginManager().callEvent(GameStartEvent())
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

                    // 주기가 짧은 패시브는 초당 열 번씩 돌기도 합니다. 해당 능력자가
                    // 하나도 없는 경우가 대부분이라, 중간 리스트를 만들지 않고
                    // 한 번만 훑으면서 걸러냅니다.
                    for (uuid in gamePlayers) {
                        val p = Bukkit.getPlayer(uuid) ?: continue
                        if (AbilityRegistry.getPlayerClassId(p) != def.classId) continue
                        if (AbilityRegistry.isAbilityDisabled(p, def.id)) continue
                        if (AbilityRegistry.isOnCooldown(p, def.id)) continue

                        val event = AbilityUseEvent(p, def, AbilityTrigger.PASSIVE)
                        Bukkit.getPluginManager().callEvent(event)
                        // 스크립트가 cancel event 로 거부하면 쿨타임을 걸지 않는다
                        // (클릭 트리거와 같은 규칙)
                        if (!event.isCancelled && def.cooldownSeconds > 0)
                            AbilityRegistry.startCooldown(p, def.id, def.cooldownSeconds)
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
            (sender as? Player)?.playSound(sender.location, Sound.ENTITY_VILLAGER_NO, 0.5f, 0.85f)
            return
        }
        Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f) }
        isRunning = false
        phase = GamePhase.IDLE

        // 정리하기 전에 알린다 — 스크립트가 아직 누가 어떤 능력이었는지 볼 수 있어야
        // 능력이 플레이어에게 남겨둔 것(걷기 속도 등)을 되돌릴 수 있다
        Bukkit.getPluginManager().callEvent(GameEndEvent())

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

        // 게임 월드는 매번 새로 만드므로 여기서 통째로 지운다. 원본(템플릿)은 그대로 둔다.
        gameWorld?.let { teardownWorldFiles(it) }
        gameWorld = null

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