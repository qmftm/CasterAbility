package me.qmftm.casterability.command

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class Commands(private val plugin: CasterAbility) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(color("&c사용법: /$label <start|stop|reload|list|config>"))
            return true
        }

        when (args[0].lowercase()) {
            "start"  -> plugin.gameManager.startGame(sender)
            "stop"   -> plugin.gameManager.stopGame(sender)
            "reload" -> {
                plugin.reloadGameConfig()
                AbilityRegistry.clearDefinitions()
                sender.sendMessage(color("&a설정을 리로드했습니다. Skript 리로드도 함께 해주세요."))
            }
            "list"   -> {
                if (sender !is Player) { sender.sendMessage("플레이어만 사용 가능합니다."); return true }
                plugin.guiManager.openAbilityList(sender)
            }
            "config" -> {
                if (sender !is Player) { sender.sendMessage("플레이어만 사용 가능합니다."); return true }
                plugin.configGui.open(sender)
            }
            else -> sender.sendMessage(color("&c사용법: /$label <start|stop|reload|list|config>"))
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, alias: String, args: Array<String>
    ) = if (args.size == 1) listOf("start", "stop", "reload", "list", "config") else emptyList()

    private fun color(s: String) = s.replace("&", "§")
}
