package me.qmftm.casterability.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit

private val ampSerializer = LegacyComponentSerializer.legacyAmpersand()

fun String.toComponent(): Component = ampSerializer.deserialize(this)

fun broadcast(message: String) = Bukkit.broadcast(message.toComponent())