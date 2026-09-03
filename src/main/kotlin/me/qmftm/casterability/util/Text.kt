package me.qmftm.casterability.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit

private val ampSerializer     = LegacyComponentSerializer.legacyAmpersand()
private val sectionSerializer = LegacyComponentSerializer.legacySection()

/** `&` 색 코드 문자열용 */
fun String.toComponent(): Component = ampSerializer.deserialize(this)

/** `§` 색 코드 문자열용 (GUI 아이템 이름 등) */
fun String.toSectionComponent(): Component = sectionSerializer.deserialize(this)

fun broadcast(message: String) = Bukkit.broadcast(message.toComponent())