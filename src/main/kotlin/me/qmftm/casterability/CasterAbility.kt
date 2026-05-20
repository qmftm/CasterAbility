package me.qmftm.casterability

import ch.njol.skript.Skript
import me.qmftm.casterability.command.Commands
import me.qmftm.casterability.config.GameConfig
import me.qmftm.casterability.game.GameManager
import me.qmftm.casterability.gui.ConfigGui
import me.qmftm.casterability.gui.GuiManager
import me.qmftm.casterability.listener.AbilityDispatcher
import me.qmftm.casterability.listener.GameListener
import me.qmftm.casterability.listener.GuiListener
import me.qmftm.casterability.skript.SkriptRegistry
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class CasterAbility : JavaPlugin() {

    lateinit var gameConfig:  GameConfig  private set
    lateinit var gameManager: GameManager private set
    lateinit var guiManager:  GuiManager  private set
    lateinit var configGui:   ConfigGui   private set

    override fun onEnable() {
        saveDefaultConfig()
        gameConfig  = GameConfig.load(this)
        gameManager = GameManager(this)
        guiManager  = GuiManager(this)
        configGui   = ConfigGui(this)
        gameManager.prepareWorld()

        // ── Skript 애드온 구문 등록 ───────────────────────
        if (Skript.isAcceptRegistrations()) {
            SkriptRegistry.registerAll()
            logger.info("Skript 구문 등록 완료")
        } else {
            logger.warning("Skript 등록 시간이 지났습니다. 플러그인 로드 순서를 확인하세요.")
        }

        // ── 커맨드 ────────────────────────────────────────
        val cmd = Commands(this)
        getCommand("ca")?.setExecutor(cmd)
        getCommand("ca")?.tabCompleter = cmd

        // ── 리스너 ────────────────────────────────────────
        val pm = Bukkit.getPluginManager()
        pm.registerEvents(GameListener(this), this)
        pm.registerEvents(GuiListener(this), this)
        pm.registerEvents(AbilityDispatcher(gameManager), this)

        logger.info("CasterAbility v${description.version} 활성화됨")
        Bukkit.getOnlinePlayers()
            .filter { it.isOp }
            .forEach { it.sendMessage("§6[§aCaster§7Ability§6] §f플러그인이 활성화되었습니다.") }
    }

    fun reloadGameConfig() {
        gameConfig = GameConfig.load(this)
        gameManager.prepareWorld()
    }

    override fun onDisable() {
        if (::gameManager.isInitialized && gameManager.isRunning) {
            gameManager.stopGame(Bukkit.getConsoleSender())
        }
        logger.info("CasterAbility 비활성화됨")
    }
}
