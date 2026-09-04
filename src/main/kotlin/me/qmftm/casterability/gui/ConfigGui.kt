package me.qmftm.casterability.gui

import me.qmftm.casterability.CasterAbility
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID
import me.qmftm.casterability.util.toSectionComponent
import net.kyori.adventure.text.format.TextDecoration

/**
 * 설정 GUI. server/1.21.8 [CasterAbility]/plugins/-Skript/scripts/ChzzkAbility 의
 * gui/Inventory.sk + config/Config.sk 화면 구성(메인 메뉴 → 카테고리별 하위 화면,
 * 아이콘, 배치)을 그대로 옮겨왔다.
 */
class ConfigGui(private val plugin: CasterAbility) {

    private enum class Screen(val rows: Int, val title: String, val bg: Material) {
        MAIN(3,          "§8§l설정 메뉴",       Material.GRAY_STAINED_GLASS_PANE),
        GAME(5,          "§f§l게임 설정",       Material.WHITE_STAINED_GLASS_PANE),
        SPAWN(3,         "§2§l스폰 설정",       Material.LIME_STAINED_GLASS_PANE),
        DEATH(3,         "§c§l사망 설정",       Material.RED_STAINED_GLASS_PANE),
        INVINCIBILITY(3, "§6§l무적 설정",       Material.YELLOW_STAINED_GLASS_PANE),
        WORLDBORDER(4,   "§9§l월드보더 설정",   Material.LIGHT_BLUE_STAINED_GLASS_PANE),
    }

    private enum class EntryType { BOOL, INT, STRING_READONLY }

    private data class Entry(
        val slot: Int,
        val key: String,
        val label: String,
        val type: EntryType,
        val onIcon: Material = Material.LIME_DYE,
        val offIcon: Material = Material.GRAY_DYE,
        val icon: Material = Material.PAPER,
        val unit: String = "",
        val min: Int = Int.MIN_VALUE,
        val max: Int = Int.MAX_VALUE,
        val step: Int = 1,
        val shiftStep: Int = 10,
    )

    private data class Category(val slot: Int, val screen: Screen, val icon: Material, val name: String, val desc: String)

    // ── 메인 메뉴 카테고리 ────────────────────────────────
    private val categories = listOf(
        Category(9,  Screen.GAME,          Material.DIAMOND_SWORD,     "§d게임 설정",     "§7게임 관련 설정을 관리합니다."),
        Category(11, Screen.SPAWN,         Material.ENDER_PEARL,       "§a스폰 설정",     "§7스폰 관련 설정을 관리합니다."),
        Category(13, Screen.DEATH,         Material.SKELETON_SKULL,   "§c사망 설정",     "§7사망 관련 설정을 관리합니다."),
        Category(15, Screen.INVINCIBILITY, Material.TOTEM_OF_UNDYING, "§e무적 설정",     "§7무적 관련 설정을 관리합니다."),
        Category(17, Screen.WORLDBORDER,   Material.FILLED_MAP,        "§b월드보더 설정", "§7월드보더 관련 설정을 관리합니다."),
    )

    // ── 게임 설정 ─────────────────────────────────────────
    // WRECK 은 0/25/50/75/90 중 하나를 고르는 5칸짜리 패드 (슬롯 2~6)
    private val wreckSlots = linkedMapOf(2 to 0, 3 to 25, 4 to 50, 5 to 75, 6 to 90)
    private val wreckColor = mapOf(0 to "§f", 25 to "§e", 50 to "§6", 75 to "§c", 90 to "§5")

    private val gameEntries = listOf(
        Entry(20, "game.infinity_hunger",      "§e무제한 허기",      EntryType.BOOL, onIcon = Material.COOKED_BEEF, offIcon = Material.ROTTEN_FLESH),
        Entry(21, "game.infinity_duration",    "§8무제한 내구도",    EntryType.BOOL, onIcon = Material.ANVIL,       offIcon = Material.DAMAGED_ANVIL),
        Entry(22, "game.basic_health",         "§c기본 체력",        EntryType.INT,  icon = Material.GOLDEN_APPLE,     unit = "hp", min = 1,  max = 1024, step = 1,  shiftStep = 1),
        Entry(24, "game.cooldown_bow",         "§6활 쿨타임",        EntryType.BOOL, onIcon = Material.BOW,         offIcon = Material.BOW),
        Entry(25, "game.cooldown_shield",      "§6방패 쿨타임",      EntryType.BOOL, onIcon = Material.SHIELD,      offIcon = Material.SHIELD),
        Entry(29, "game.auto_skip_second",     "§e능력 추첨 자동 스킵 시간", EntryType.INT, icon = Material.CLOCK,             unit = "초", min = 5,  max = 600,  step = 10, shiftStep = 10),
        Entry(30, "game.ability_change_count", "§3능력 추첨 횟수",   EntryType.INT,  icon = Material.DISPENSER,        unit = "회", min = 0,  max = 20,   step = 1,  shiftStep = 1),
        Entry(32, "game.basic_level",          "§a기본 레벨",        EntryType.INT,  icon = Material.EXPERIENCE_BOTTLE, unit = "레벨", min = 0, max = 1000, step = 10, shiftStep = 10),
        Entry(33, "game.weather_clear",        "§b날씨 고정",        EntryType.BOOL, onIcon = Material.SUNFLOWER,   offIcon = Material.SNOWBALL),
    )

    private val spawnEntries = listOf(
        Entry(11, "spawn.random_spawn",  "§a랜덤 스폰",        EntryType.BOOL,           onIcon = Material.REDSTONE_TORCH, offIcon = Material.REPEATER),
        Entry(13, "spawn.world_name",    "§f월드 템플릿 이름", EntryType.STRING_READONLY),
        Entry(15, "spawn.random_radius", "§e랜덤 스폰 반지름", EntryType.INT, icon = Material.COMPASS, unit = "블록", min = 10, max = 2000, step = 10, shiftStep = 10),
    )

    private val deathEntries = listOf(
        Entry(11, "death.drop_items", "§c아이템 드롭",   EntryType.BOOL, onIcon = Material.IRON_SWORD,       offIcon = Material.STONE_SWORD),
        Entry(13, "death.drop_exp",   "§e경험치 드롭",   EntryType.BOOL, onIcon = Material.EXPERIENCE_BOTTLE, offIcon = Material.GLASS_BOTTLE),
        Entry(15, "death.kick",       "§4추방",          EntryType.BOOL, onIcon = Material.IRON_DOOR,        offIcon = Material.OAK_DOOR),
    )

    private val invincibilityEntries = listOf(
        Entry(10, "invincibility.enable",       "§a무적 활성화",   EntryType.BOOL, onIcon = Material.REDSTONE_TORCH,     offIcon = Material.REPEATER),
        Entry(12, "invincibility.second",       "§e무적 시간",     EntryType.INT,  icon = Material.CLOCK, unit = "초", min = 5, max = 600, step = 15, shiftStep = 60),
        Entry(14, "invincibility.show_bossbar", "§d무적 보스바",   EntryType.BOOL, onIcon = Material.OAK_SIGN,           offIcon = Material.DARK_OAK_SIGN),
        Entry(16, "invincibility.invisible",    "§5무적 투명화",   EntryType.BOOL, onIcon = Material.POTION,             offIcon = Material.GLASS_BOTTLE),
    )

    private val worldborderEntries = listOf(
        Entry(10, "worldborder.enable",               "§a월드보더 활성화",     EntryType.BOOL, onIcon = Material.REDSTONE_TORCH, offIcon = Material.REPEATER),
        Entry(12, "worldborder.max_radius",           "§b월드보더 최대 반지름", EntryType.INT,  icon = Material.FILLED_MAP, unit = "블록", min = 50, max = 10000, step = 10, shiftStep = 40),
        Entry(14, "worldborder.min_radius",           "§3월드보더 최소 반지름", EntryType.INT,  icon = Material.MAP,        unit = "블록", min = 5,  max = 1000,  step = 1,  shiftStep = 10),
        Entry(16, "worldborder.show_bossbar",         "§d월드보더 보스바",     EntryType.BOOL, onIcon = Material.OAK_SIGN,       offIcon = Material.DARK_OAK_SIGN),
        Entry(20, "worldborder.shrink_second",        "§e월드보더 수축 시간",   EntryType.INT,  icon = Material.CLOCK,      unit = "초", min = 10, max = 3600, step = 5, shiftStep = 10),
        Entry(22, "worldborder.shrink_count",         "§6월드보더 수축 횟수",   EntryType.INT,  icon = Material.COMPASS,    unit = "회", min = 1,  max = 50,   step = 1, shiftStep = 1),
        Entry(24, "worldborder.shrink_random_center", "§a월드보더 수축 랜덤 중심", EntryType.BOOL, onIcon = Material.BEACON,     offIcon = Material.BEACON),
    )

    private fun entriesFor(screen: Screen): List<Entry> = when (screen) {
        Screen.GAME          -> gameEntries
        Screen.SPAWN         -> spawnEntries
        Screen.DEATH         -> deathEntries
        Screen.INVINCIBILITY -> invincibilityEntries
        Screen.WORLDBORDER   -> worldborderEntries
        Screen.MAIN          -> emptyList()
    }

    private val allEntries = gameEntries + spawnEntries + deathEntries + invincibilityEntries + worldborderEntries

    // ── 세션 ─────────────────────────────────────────────

    private class Session(
        val values: MutableMap<String, Any>,
        var screen: Screen,
        var inv: Inventory,
        var navigating: Boolean = false,
    )

    private val sessions = mutableMapOf<UUID, Session>()

    // ── 열기 ───────────────────────────────────────────────

    fun open(player: Player) {
        val values = loadValues()
        val inv = buildInv(Screen.MAIN, values)
        sessions[player.uniqueId] = Session(values, Screen.MAIN, inv)
        player.openInventory(inv)
        player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 0.75f, 0.9f)
    }

    private fun openScreen(player: Player, screen: Screen) {
        val session = sessions[player.uniqueId] ?: return
        val inv = buildInv(screen, session.values)
        session.screen = screen
        session.inv = inv
        session.navigating = true
        player.openInventory(inv)
        session.navigating = false
        player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 0.75f, 0.9f)
    }

    private fun loadValues(): MutableMap<String, Any> {
        val c = plugin.config
        val map = allEntries.associateTo(mutableMapOf<String, Any>()) { e ->
            e.key to when (e.type) {
                EntryType.BOOL            -> c.getBoolean(e.key, false)
                EntryType.INT             -> c.getInt(e.key, 0)
                EntryType.STRING_READONLY -> c.getString(e.key, "") ?: ""
            }
        }
        map["game.wreck"] = c.getInt("game.wreck", 0)
        return map
    }

    // ── 렌더 ───────────────────────────────────────────────

    private fun buildInv(screen: Screen, values: Map<String, Any>): Inventory {
        val inv = Bukkit.createInventory(null, screen.rows * 9, screen.title.toSectionComponent())
        val fill = glass(screen.bg)
        repeat(screen.rows * 9) { inv.setItem(it, fill) }

        when (screen) {
            Screen.MAIN -> {
                categories.forEach { cat -> inv.setItem(cat.slot, makeItem(cat.icon, cat.name, "", cat.desc)) }
            }
            Screen.GAME -> {
                val wreck = values["game.wreck"] as? Int ?: 0
                wreckSlots.forEach { (slot, value) ->
                    val on = wreck == value
                    inv.setItem(slot, makeItem(
                        if (on) Material.END_CRYSTAL else Material.CLAY_BALL,
                        "§d§lWRECK §b:: ${wreckColor[value]}$value%",
                        "§7능력자 게임의 WRECK 수치를 설정합니다.",
                        "§7WRECK 수치는 쿨타임을 감소시킵니다.",
                        "",
                        if (on) "§a선택됨" else "§7클릭하여 선택",
                    ))
                }
                gameEntries.forEach { e -> inv.setItem(e.slot, entryItem(e, values[e.key])) }
            }
            else -> entriesFor(screen).forEach { e -> inv.setItem(e.slot, entryItem(e, values[e.key])) }
        }

        val lastRowBase = (screen.rows - 1) * 9
        if (screen != Screen.MAIN) {
            inv.setItem(lastRowBase, makeItem(Material.ARROW, "§f◀ 뒤로", "§7설정 메뉴로 돌아갑니다."))
        }

        return inv
    }

    private fun refresh(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        val inv = session.inv
        when (session.screen) {
            Screen.GAME -> {
                val wreck = session.values["game.wreck"] as? Int ?: 0
                wreckSlots.forEach { (slot, value) ->
                    val on = wreck == value
                    inv.setItem(slot, makeItem(
                        if (on) Material.END_CRYSTAL else Material.CLAY_BALL,
                        "§d§lWRECK §b:: ${wreckColor[value]}$value%",
                        "§7능력자 게임의 WRECK 수치를 설정합니다.",
                        "§7WRECK 수치는 쿨타임을 감소시킵니다.",
                        "",
                        if (on) "§a선택됨" else "§7클릭하여 선택",
                    ))
                }
                gameEntries.forEach { e -> inv.setItem(e.slot, entryItem(e, session.values[e.key])) }
            }
            Screen.MAIN -> {}
            else -> entriesFor(session.screen).forEach { e -> inv.setItem(e.slot, entryItem(e, session.values[e.key])) }
        }
    }

    private fun entryItem(e: Entry, value: Any?): ItemStack = when (e.type) {
        EntryType.BOOL -> {
            val on = value as? Boolean ?: false
            makeItem(
                if (on) e.onIcon else e.offIcon,
                e.label,
                "§7현재: ${if (on) "§aON" else "§cOFF"}",
                "§7클릭하여 전환",
            )
        }
        EntryType.INT -> {
            val v = value as? Int ?: 0
            makeItem(
                e.icon,
                e.label,
                "§7현재: §e$v${e.unit}",
                "§7좌클릭: §f+${e.step}  §7우클릭: §f-${e.step}",
                "§7Shift+클릭: §f±${e.shiftStep}",
            )
        }
        EntryType.STRING_READONLY -> {
            makeItem(
                Material.BOOK,
                e.label,
                "§7현재: §f${value as? String ?: ""}",
                "§8config.yml 에서 수정 후 /ca reload",
                "§8이 폴더가 있으면 게임마다 복제해서 씁니다.",
                "§8없으면 그때그때 새로 생성합니다.",
            )
        }
    }

    // ── 클릭 처리 ─────────────────────────────────────────

    fun handleClick(player: Player, slot: Int, isShift: Boolean, isRight: Boolean) {
        val session = sessions[player.uniqueId] ?: return
        val lastRowBase = (session.screen.rows - 1) * 9

        if (session.screen != Screen.MAIN && slot == lastRowBase) {
            openScreen(player, Screen.MAIN)
            return
        }

        when (session.screen) {
            Screen.MAIN -> {
                val cat = categories.find { it.slot == slot } ?: return
                openScreen(player, cat.screen)
            }
            Screen.GAME -> {
                val wreckValue = wreckSlots[slot]
                if (wreckValue != null) {
                    session.values["game.wreck"] = wreckValue
                    persist("game.wreck", wreckValue)
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, (wreckValue / 50f) + 0.1f)
                    refresh(player)
                    return
                }
                applyEntryClick(session, gameEntries, slot, isShift, isRight, player)
            }
            else -> applyEntryClick(session, entriesFor(session.screen), slot, isShift, isRight, player)
        }
    }

    private fun applyEntryClick(session: Session, entries: List<Entry>, slot: Int, isShift: Boolean, isRight: Boolean, player: Player) {
        val entry = entries.find { it.slot == slot } ?: return
        val newValue: Any = when (entry.type) {
            EntryType.BOOL -> !((session.values[entry.key] as? Boolean) ?: false)
            EntryType.INT -> {
                val cur   = session.values[entry.key] as? Int ?: 0
                val delta = if (isShift) entry.shiftStep else entry.step
                val sign  = if (isRight) -1 else 1
                (cur + delta * sign).coerceIn(entry.min, entry.max)
            }
            EntryType.STRING_READONLY -> return
        }
        session.values[entry.key] = newValue
        persist(entry.key, newValue)
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.6f, 1f)
        refresh(player)
    }

    // ── 저장 ───────────────────────────────────────────────
    // 클릭할 때마다 바로 config 에 반영한다 (별도 저장 버튼 없음).

    private fun persist(key: String, value: Any) {
        val c = plugin.config
        when (value) {
            is Boolean -> c.set(key, value)
            is Int     -> c.set(key, value)
            is String  -> c.set(key, value)
        }
        plugin.saveConfig()
        plugin.reloadGameConfig()
    }

    // ── 유틸 ───────────────────────────────────────────────

    fun isConfigGui(player: Player, inv: Inventory): Boolean =
        sessions[player.uniqueId]?.inv == inv

    fun close(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        if (session.navigating) return
        sessions.remove(player.uniqueId)
    }

    private fun glass(mat: Material, name: String = " "): ItemStack {
        val item = ItemStack(mat)
        val meta = item.itemMeta ?: return item
        meta.displayName(name.toSectionComponent().decoration(TextDecoration.ITALIC, false))
        item.itemMeta = meta
        return item
    }

    private fun makeItem(mat: Material, name: String, vararg lore: String): ItemStack {
        val item = ItemStack(mat)
        val meta = item.itemMeta ?: return item
        meta.displayName(name.toSectionComponent().decoration(TextDecoration.ITALIC, false))
        if (lore.isNotEmpty()) meta.lore(lore.map { it.toSectionComponent().decoration(TextDecoration.ITALIC, false) })
        item.itemMeta = meta
        return item
    }
}
