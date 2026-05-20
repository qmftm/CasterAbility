package me.qmftm.casterability.gui

import me.qmftm.casterability.CasterAbility
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID

class ConfigGui(private val plugin: CasterAbility) {

    companion object {
        const val TITLE = "§6⚙ §lCasterAbility 설정"
        const val SIZE  = 54
    }

    private enum class EntryType { BOOL, INT, STRING_READONLY }

    private data class Entry(
        val slot: Int,
        val key: String,
        val label: String,
        val type: EntryType,
        val min: Int = 0,
        val max: Int = Int.MAX_VALUE,
        val step: Int = 1,
    )

    // ── 슬롯 배치 ─────────────────────────────────────────
    //
    //  Row 0 (0-8):   [게임헤더] h lv skip ab_ch inf_hun inf_dur bow shield
    //  Row 1 (9-17):  weather wreck . [스폰헤더] world r_spwn r_rad . [사망헤더]
    //  Row 2 (18-26): drop_i drop_e kick . [무적헤더] inv_en inv_s inv_bb inv_iv
    //  Row 3 (27-35): . [WB헤더] wb_en wb_max wb_min wb_ss wb_sc wb_src wb_bb
    //  Row 4 (36-44): (배경)
    //  Row 5 (45-53): (배경) [저장:49] (배경) [닫기:53]

    private val entries = listOf(
        Entry(1,  "game.basic_health",         "§f기본 체력",          EntryType.INT,  1,   1024),
        Entry(2,  "game.basic_level",          "§f기본 레벨",          EntryType.INT,  0,   1000),
        Entry(3,  "game.auto_skip_second",     "§f자동 스킵(초)",      EntryType.INT,  5,   600),
        Entry(4,  "game.ability_change_count", "§f능력 재추첨 횟수",   EntryType.INT,  0,   20),
        Entry(5,  "game.infinity_hunger",      "§f무한 배고픔",        EntryType.BOOL),
        Entry(6,  "game.infinity_duration",    "§f무한 내구도",        EntryType.BOOL),
        Entry(7,  "game.cooldown_bow",         "§f활 쿨타임",          EntryType.BOOL),
        Entry(8,  "game.cooldown_shield",      "§f방패 쿨타임",        EntryType.BOOL),
        Entry(9,  "game.weather_clear",        "§f맑은 날씨 강제",     EntryType.BOOL),
        Entry(10, "game.wreck",                "§f파괴력",             EntryType.INT,  0,   10),

        Entry(13, "spawn.world_name",          "§f게임 월드 이름",     EntryType.STRING_READONLY),
        Entry(14, "spawn.random_spawn",        "§f랜덤 스폰",          EntryType.BOOL),
        Entry(15, "spawn.random_radius",       "§f랜덤 스폰 반경",     EntryType.INT,  10,  2000, 10),

        Entry(18, "death.drop_items",          "§f아이템 드롭",        EntryType.BOOL),
        Entry(19, "death.drop_exp",            "§f경험치 드롭",        EntryType.BOOL),
        Entry(20, "death.kick",                "§f사망 시 강퇴",       EntryType.BOOL),

        Entry(23, "invincibility.enable",       "§f무적 시간 활성화",  EntryType.BOOL),
        Entry(24, "invincibility.second",       "§f무적 시간(초)",     EntryType.INT,  5,   600),
        Entry(25, "invincibility.show_bossbar", "§f무적 보스바 표시",  EntryType.BOOL),
        Entry(26, "invincibility.invisible",    "§f무적 중 투명화",    EntryType.BOOL),

        Entry(29, "worldborder.enable",               "§f월드보더 활성화",   EntryType.BOOL),
        Entry(30, "worldborder.max_radius",           "§f최대 반경",         EntryType.INT,  50,  10000, 50),
        Entry(31, "worldborder.min_radius",           "§f최소 반경",         EntryType.INT,  5,   1000,  5),
        Entry(32, "worldborder.shrink_second",        "§f수축 간격(초)",     EntryType.INT,  10,  3600,  10),
        Entry(33, "worldborder.shrink_count",         "§f수축 횟수",         EntryType.INT,  1,   50),
        Entry(34, "worldborder.shrink_random_center", "§f수축 중심 랜덤",    EntryType.BOOL),
        Entry(35, "worldborder.show_bossbar",         "§f수축 보스바 표시",  EntryType.BOOL),
    )

    private val slotIndex = entries.associateBy { it.slot }

    private val headers = listOf(
        Triple(0,  Material.ORANGE_STAINED_GLASS_PANE,     "§6§l⚙ 게임 설정"),
        Triple(12, Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b§l⚙ 스폰 설정"),
        Triple(17, Material.RED_STAINED_GLASS_PANE,        "§c§l⚙ 사망 설정"),
        Triple(22, Material.CYAN_STAINED_GLASS_PANE,       "§3§l⚙ 무적 설정"),
        Triple(28, Material.GREEN_STAINED_GLASS_PANE,      "§a§l⚙ 월드보더 설정"),
    )

    private val sessions = mutableMapOf<UUID, MutableMap<String, Any>>()
    private val openInvs = mutableMapOf<UUID, Inventory>()

    // ── 열기 ───────────────────────────────────────────────

    fun open(player: Player) {
        val session = loadSession()
        sessions[player.uniqueId] = session
        val inv = buildInv(session)
        openInvs[player.uniqueId] = inv
        player.openInventory(inv)
    }

    private fun loadSession(): MutableMap<String, Any> {
        val c = plugin.config
        return entries.associateTo(mutableMapOf()) { e ->
            e.key to when (e.type) {
                EntryType.BOOL            -> c.getBoolean(e.key, false)
                EntryType.INT             -> c.getInt(e.key, 0)
                EntryType.STRING_READONLY -> c.getString(e.key, "") ?: ""
            }
        }
    }

    // ── 렌더 ───────────────────────────────────────────────

    private fun buildInv(session: MutableMap<String, Any>): Inventory {
        val inv  = Bukkit.createInventory(null, SIZE, TITLE)
        val fill = glass(Material.WHITE_STAINED_GLASS_PANE)
        repeat(SIZE) { inv.setItem(it, fill) }

        headers.forEach { (slot, mat, name) -> inv.setItem(slot, glass(mat, name)) }
        entries.forEach  { e -> inv.setItem(e.slot, entryItem(e, session[e.key])) }

        inv.setItem(49, makeItem(Material.EMERALD, "§a§l💾 저장",  "§7클릭하여 설정을 저장합니다."))
        inv.setItem(53, makeItem(Material.BARRIER, "§c§l✖ 닫기",  "§7저장하지 않고 닫습니다."))
        return inv
    }

    private fun refresh(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        val inv     = openInvs[player.uniqueId] ?: return
        entries.forEach { e -> inv.setItem(e.slot, entryItem(e, session[e.key])) }
    }

    private fun entryItem(e: Entry, value: Any?): ItemStack = when (e.type) {
        EntryType.BOOL -> {
            val on = value as? Boolean ?: false
            makeItem(
                if (on) Material.LIME_DYE else Material.GRAY_DYE,
                e.label,
                "§7현재: ${if (on) "§aON" else "§cOFF"}",
                "§7클릭하여 전환",
            )
        }
        EntryType.INT -> {
            val v = value as? Int ?: 0
            makeItem(
                Material.PAPER,
                e.label,
                "§7현재: §e$v",
                "§7좌클릭: §f+${e.step}  §7우클릭: §f-${e.step}",
                "§7Shift+클릭: §f×10",
            )
        }
        EntryType.STRING_READONLY -> {
            makeItem(
                Material.BOOK,
                e.label,
                "§7현재: §f${value as? String ?: ""}",
                "§8config.yml 에서 수정 후 /ca reload",
            )
        }
    }

    // ── 클릭 처리 ─────────────────────────────────────────

    fun handleClick(player: Player, slot: Int, isShift: Boolean, isRight: Boolean) {
        when (slot) {
            49   -> { save(player); return }
            53   -> { player.closeInventory(); return }
        }
        val entry   = slotIndex[slot] ?: return
        val session = sessions[player.uniqueId] ?: return

        when (entry.type) {
            EntryType.BOOL -> {
                session[entry.key] = !((session[entry.key] as? Boolean) ?: false)
            }
            EntryType.INT -> {
                val cur   = session[entry.key] as? Int ?: 0
                val multi = if (isShift) 10 else 1
                val sign  = if (isRight) -1 else 1
                session[entry.key] = (cur + entry.step * multi * sign).coerceIn(entry.min, entry.max)
            }
            EntryType.STRING_READONLY -> return
        }

        openInvs[player.uniqueId]?.setItem(entry.slot, entryItem(entry, session[entry.key]))
    }

    // ── 저장 ───────────────────────────────────────────────

    fun save(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        val c = plugin.config
        session.forEach { (key, value) ->
            when (value) {
                is Boolean -> c.set(key, value)
                is Int     -> c.set(key, value)
                is String  -> c.set(key, value)
            }
        }
        plugin.saveConfig()
        plugin.reloadGameConfig()
        player.sendMessage("§a설정이 저장되었습니다.")
        player.closeInventory()
    }

    // ── 유틸 ───────────────────────────────────────────────

    fun isConfigGui(player: Player, inv: Inventory): Boolean =
        openInvs[player.uniqueId] == inv

    fun close(player: Player) {
        sessions.remove(player.uniqueId)
        openInvs.remove(player.uniqueId)
    }

    private fun glass(mat: Material, name: String = " "): ItemStack {
        val item = ItemStack(mat)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }

    private fun makeItem(mat: Material, name: String, vararg lore: String): ItemStack {
        val item = ItemStack(mat)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(name)
        if (lore.isNotEmpty()) meta.lore = lore.toList()
        item.itemMeta = meta
        return item
    }
}