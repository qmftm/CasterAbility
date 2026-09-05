package me.qmftm.casterability.gui

import me.qmftm.casterability.CasterAbility
import me.qmftm.casterability.ability.AbilityClass
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import me.qmftm.casterability.util.toSectionComponent
import net.kyori.adventure.text.format.TextDecoration

class GuiManager(private val plugin: CasterAbility) {

    companion object {
        const val DRAW_GUI_TITLE   = "§a§l능력 추첨"
        const val LIST_GUI_TITLE   = "§d§l능력 목록"

        const val SORT_NAME_SLOT = 0
        const val SORT_TIER_SLOT = 1
        private const val LIST_HEADER_ROWS = 1
    }

    enum class ListSort { DEFAULT, NAME, TIER }

    // 인벤토리 → 플레이어 UUID 역매핑
    private val openDrawGuis = mutableMapOf<java.util.UUID, Inventory>()
    private val openListGuis = mutableMapOf<java.util.UUID, Inventory>()
    private val listSort     = mutableMapOf<java.util.UUID, ListSort>()

    fun openDrawGui(p: Player) {
        val gm      = plugin.gameManager
        val classId = gm.getDrawAbility(p) ?: return
        val left    = gm.getDrawCount(p)
        val cls     = AbilityRegistry.getClass(classId)

        val inv = Bukkit.createInventory(null, 27, DRAW_GUI_TITLE.toSectionComponent())

        val bg = makeItem(Material.WHITE_STAINED_GLASS_PANE, " ")
        repeat(27) { inv.setItem(it, bg) }

        // 남은 재추첨 횟수 (슬롯 4)
        inv.setItem(4, if (left > 0)
            makeItem(Material.PAPER, "§e남은 재추첨 횟수: ${left}회")
        else
            makeItem(Material.GUNPOWDER, "§c남은 재추첨 횟수: 0회"))

        // 재추첨 (슬롯 10)
        inv.setItem(10, makeItem(Material.RED_DYE, "§c능력 재추첨", "§7클릭하여 능력을 재추첨합니다."))

        // 능력 표시 (슬롯 13)
        if (cls != null) inv.setItem(13, buildClassItem(cls))

        // 선택 확정 (슬롯 16)
        inv.setItem(16, makeItem(Material.LIME_DYE, "§a능력 선택", "§7클릭하여 능력을 선택합니다."))

        openDrawGuis[p.uniqueId] = inv
        p.openInventory(inv)
        p.playSound(p.location, org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.75f, 0.9f)
    }

    fun openAbilityList(p: Player, sort: ListSort = listSort[p.uniqueId] ?: ListSort.DEFAULT) {
        listSort[p.uniqueId] = sort
        val classes = sortClasses(AbilityRegistry.getAllClasses().toList(), sort)
        val rows    = maxOf(2, minOf(6, (classes.size + 8) / 9 + LIST_HEADER_ROWS + 1))
        val inv     = Bukkit.createInventory(null, rows * 9, LIST_GUI_TITLE.toSectionComponent())

        val bg = makeItem(Material.WHITE_STAINED_GLASS_PANE, " ")
        repeat(rows * 9) { inv.setItem(it, bg) }

        inv.setItem(SORT_NAME_SLOT, makeItem(
            Material.BOOK,
            if (sort == ListSort.NAME) "§a§l가나다순 정렬 ✔" else "§f가나다순 정렬",
            "§7클릭하여 이름 순서로 정렬합니다."
        ))
        inv.setItem(SORT_TIER_SLOT, makeItem(
            Material.NETHER_STAR,
            if (sort == ListSort.TIER) "§a§l등급순 정렬 ✔" else "§f등급순 정렬",
            "§7클릭하여 등급(레벨) 순서로 정렬합니다."
        ))

        classes.forEachIndexed { i, cls -> inv.setItem(LIST_HEADER_ROWS * 9 + i, buildClassItem(cls)) }

        openListGuis[p.uniqueId] = inv
        p.openInventory(inv)
        p.playSound(p.location, org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.75f, 0.9f)
    }

    private fun sortClasses(classes: List<AbilityClass>, sort: ListSort): List<AbilityClass> = when (sort) {
        ListSort.NAME    -> classes.sortedBy { it.name }
        ListSort.TIER    -> classes.sortedBy { it.tier }
        ListSort.DEFAULT -> classes
    }

    fun isDrawGui(p: Player, inv: Inventory): Boolean =
        openDrawGuis[p.uniqueId] == inv

    fun clearDrawGui(p: Player) = openDrawGuis.remove(p.uniqueId)

    fun isListGui(p: Player, inv: Inventory): Boolean =
        openListGuis[p.uniqueId] == inv

    fun clearListGui(p: Player) = openListGuis.remove(p.uniqueId)

    // ── 아이템 유틸 ───────────────────────────────────────

    private fun buildClassItem(cls: AbilityClass): ItemStack {
        val mat = when (cls.tier) {
            0    -> Material.GOLD_BLOCK
            1    -> Material.MAGENTA_WOOL
            2    -> Material.RED_WOOL
            3    -> Material.LIGHT_BLUE_WOOL
            4    -> Material.LIME_WOOL
            else -> Material.WHITE_WOOL
        }
        val abilities = AbilityRegistry.abilitiesOfClass(cls.id)
        val lore = mutableListOf<String>()
        lore.add("§7등급: ${cls.tierDisplay}")

        // 스크립트가 적어둔 설명이 있으면 그대로 보여준다 (& 색 코드 → §)
        if (cls.description.isNotEmpty()) {
            lore.add("")
            cls.description.forEach { lore.add(it.replace('&', '§')) }
        }

        lore.add("")
        abilities.forEach { ab ->
            lore.add("§f· ${ab.displayName} §7[${ab.trigger.skriptName}]" +
                if (ab.cooldownSeconds > 0) " §8(${ab.cooldownSeconds}s)" else "")
        }
        return makeItem(mat, "§f${cls.name} §8- ${cls.tierDisplay}", *lore.toTypedArray())
    }

    fun makeItem(mat: Material, name: String, vararg lore: String): ItemStack {
        val item = ItemStack(mat)
        val meta: ItemMeta = item.itemMeta ?: return item
        meta.displayName(name.toSectionComponent().decoration(TextDecoration.ITALIC, false))
        if (lore.isNotEmpty()) meta.lore(lore.map { it.toSectionComponent().decoration(TextDecoration.ITALIC, false) })
        item.itemMeta = meta
        return item
    }
}
