package me.qmftm.casterability.listener

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityRegistry
import me.qmftm.casterability.game.GamePhase
import me.qmftm.casterability.util.toComponent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.weather.WeatherChangeEvent

// ─────────────────────────────────────────────────────────
//  게임 규칙 리스너
// ─────────────────────────────────────────────────────────

class GameListener(private val plugin: CasterAbility) : Listener {

    private val gm  get() = plugin.gameManager
    private val cfg get() = plugin.gameConfig

    @EventHandler(priority = EventPriority.HIGH)
    fun onDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        if (!gm.isRunning || gm.phase != GamePhase.IN_GAME) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFood(event: FoodLevelChangeEvent) {
        if (!gm.isRunning || !cfg.infinityHunger) return
        event.isCancelled = true
        event.foodLevel = 20
    }

    @EventHandler
    fun onItemDamage(event: PlayerItemDamageEvent) {
        if (!gm.isRunning || !cfg.infinityDuration) return
        event.isCancelled = true
    }

    @EventHandler
    fun onWeather(event: WeatherChangeEvent) {
        if (!gm.isRunning || !cfg.weatherClear) return
        if (event.toWeatherState()) event.isCancelled = true
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        if (!gm.isRunning) return
        if (!cfg.dropItems) event.keepInventory = true
        if (!cfg.dropExp)   event.keepLevel     = true
        if (cfg.kick) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                event.entity.kick("&c게임에서 탈락하였습니다.".toComponent())
            }, 20L)
        }
    }
}

// ─────────────────────────────────────────────────────────
//  추첨 GUI 리스너
// ─────────────────────────────────────────────────────────

class GuiListener(private val plugin: CasterAbility) : Listener {

    private val gm  get() = plugin.gameManager
    private val gui get() = plugin.guiManager

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val p = event.whoClicked as? Player ?: return

        // 설정 GUI
        if (plugin.configGui.isConfigGui(p, event.inventory)) {
            event.isCancelled = true
            plugin.configGui.handleClick(
                p, event.rawSlot,
                isShift = event.isShiftClick,
                isRight = event.isRightClick,
            )
            return
        }

        if (!gui.isDrawGui(p, event.inventory)) return

        event.isCancelled = true
        if (!gm.isRunning) return

        when (event.rawSlot) {
            10 -> {
                val next = gm.rerollAbility(p)
                if (next == null) {
                    p.sendMessage("§c남은 재추첨 횟수가 없습니다.")
                    p.playSound(p.location, org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 0.85f)
                } else {
                    p.playSound(p.location, org.bukkit.Sound.BLOCK_ANVIL_USE, 0.5f, 1.25f)
                }
                gui.openDrawGui(p)
            }
            16 -> {
                gm.confirmAbility(p)
                gui.clearDrawGui(p)
                p.playSound(p.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.25f)
            }
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val p = event.player as? Player ?: return

        // 설정 GUI 세션 정리
        if (plugin.configGui.isConfigGui(p, event.inventory)) {
            plugin.configGui.close(p)
            return
        }

        if (!gui.isDrawGui(p, event.inventory)) return
        if (!gm.isRunning) { gui.clearDrawGui(p); return }

        if (AbilityRegistry.getPlayerClass(p) == null) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                gui.openDrawGui(p)
            }, 1L)
        } else {
            gui.clearDrawGui(p)
        }
    }
}