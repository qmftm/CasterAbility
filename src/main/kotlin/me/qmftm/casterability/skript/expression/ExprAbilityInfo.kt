package me.qmftm.casterability.skript.expression

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import me.qmftm.casterability.ability.AbilityRegistry
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 플레이어와 상관없이 "등록된 능력 자체"의 정보를 읽는 Expression들.
 *
 * 사용 예:
 *   name of ability class "menhera"        → "멘헤라"
 *   tier of ability class "menhera"        → 3
 *   tier display of ability class "menhera" → "§9§lB"
 *   all ability classes                    → "menhera", "yandere", ...
 *   all abilities                          → "love", "self_harm", ...
 *   abilities of ability class "menhera"   → "love", "self_harm"
 *   abilities of player                    → 플레이어가 가진 능력 id 목록
 *   players with ability class "menhera"   → 그 클래스를 가진 접속 중인 플레이어들
 *   trigger of ability "love"              → "right_click"
 *   item of ability "love"                 → "blaze_rod"
 *   max cooldown of ability "love"         → 30 (정의된 쿨타임, 남은 쿨타임 아님)
 */

// ── ability class name (id로 조회) ────────────────────────

class ExprClassNameOf : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprClassNameOf::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] name of [ability] class %string%"
            )
        }
    }

    private lateinit var classExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        classExpr = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<String?> {
        val id = classExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getClass(id)?.name)
    }

    override fun isSingle() = true
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "name of ability class ${classExpr.toString(e, d)}"
}

// ── ability class tier (id로 조회) ────────────────────────

class ExprClassTierOf : SimpleExpression<Int>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprClassTierOf::class.java, Int::class.javaObjectType,
                ExpressionType.COMBINED,
                "[the] tier of [ability] class %string%"
            )
        }
    }

    private lateinit var classExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        classExpr = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<Int?> {
        val id = classExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getClass(id)?.tier)
    }

    override fun isSingle() = true
    override fun getReturnType() = Int::class.javaObjectType
    override fun toString(e: Event?, d: Boolean) = "tier of ability class ${classExpr.toString(e, d)}"
}

// ── tier 표시 문자열 ──────────────────────────────────────

class ExprTierDisplay : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprTierDisplay::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] tier display of [ability] class %string%",
                "[the] tier display of %player%"
            )
        }
    }

    private var byPlayer = false
    private var classExpr: Expression<String>? = null
    private var playerExpr: Expression<Player>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        byPlayer = i == 1
        if (byPlayer) playerExpr = exprs[0] as Expression<Player>
        else          classExpr  = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<String?> {
        val cls = if (byPlayer) {
            val p = playerExpr?.getSingle(event) ?: return arrayOfNulls(1)
            AbilityRegistry.getPlayerClass(p)
        } else {
            val id = classExpr?.getSingle(event) ?: return arrayOfNulls(1)
            AbilityRegistry.getClass(id)
        }
        return arrayOf(cls?.tierDisplay)
    }

    override fun isSingle() = true
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "tier display"
}

// ── 등록된 ability class 전체 ─────────────────────────────

class ExprAllClasses : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAllClasses::class.java, String::class.java,
                ExpressionType.SIMPLE,
                "[all] [registered] ability classes"
            )
        }
    }

    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult) = true

    override fun get(event: Event): Array<String?> =
        AbilityRegistry.getAllClasses().map { it.id }.toTypedArray()

    override fun isSingle() = false
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "all ability classes"
}

// ── 등록된 ability 전체 ───────────────────────────────────

class ExprAllAbilities : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAllAbilities::class.java, String::class.java,
                ExpressionType.SIMPLE,
                "[all] [registered] abilities"
            )
        }
    }

    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult) = true

    override fun get(event: Event): Array<String?> =
        AbilityRegistry.getAllAbilities().map { it.id }.toTypedArray()

    override fun isSingle() = false
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "all abilities"
}

// ── 클래스/플레이어에 속한 ability 목록 ───────────────────

class ExprAbilitiesOf : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilitiesOf::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] abilities of [ability] class %string%",
                "[the] abilities of %player%",
                "%player%'s abilities"
            )
        }
    }

    private var byPlayer = false
    private var classExpr: Expression<String>? = null
    private var playerExpr: Expression<Player>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        byPlayer = i != 0
        if (byPlayer) playerExpr = exprs[0] as Expression<Player>
        else          classExpr  = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<String?> {
        val defs = if (byPlayer) {
            val p = playerExpr?.getSingle(event) ?: return arrayOfNulls(0)
            AbilityRegistry.abilitiesOfPlayer(p)
        } else {
            val id = classExpr?.getSingle(event) ?: return arrayOfNulls(0)
            AbilityRegistry.abilitiesOfClass(id)
        }
        return defs.map { it.id }.toTypedArray()
    }

    override fun isSingle() = false
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "abilities of ..."
}

// ── 특정 클래스를 가진 플레이어들 ─────────────────────────

class ExprPlayersWithClass : SimpleExpression<Player>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprPlayersWithClass::class.java, Player::class.java,
                ExpressionType.COMBINED,
                "[all] players with ability class %string%"
            )
        }
    }

    private lateinit var classExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        classExpr = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<Player?> {
        val id = classExpr.getSingle(event) ?: return arrayOfNulls(0)
        return AbilityRegistry.playersWithClass(id).toTypedArray()
    }

    override fun isSingle() = false
    override fun getReturnType() = Player::class.java
    override fun toString(e: Event?, d: Boolean) = "players with ability class ${classExpr.toString(e, d)}"
}

// ── ability 정의 정보 ─────────────────────────────────────

class ExprAbilityTrigger : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityTrigger::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] trigger of [ability] %string%"
            )
        }
    }

    private lateinit var abilityExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        abilityExpr = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<String?> {
        val id = abilityExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getAbility(id)?.trigger?.skriptName)
    }

    override fun isSingle() = true
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "trigger of ability ${abilityExpr.toString(e, d)}"
}

/** 능력에 붙여둔 표시 이름. 안 적었으면 id를 그대로 돌려준다. */
class ExprAbilityName : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityName::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] name of ability %string%"
            )
        }
    }

    private lateinit var abilityExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        abilityExpr = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<String?> {
        val id = abilityExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getAbility(id)?.displayName)
    }

    override fun isSingle() = true
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "name of ability ${abilityExpr.toString(e, d)}"
}

/** `trigger: passive` + `event:` 로 붙여둔 이벤트 이름. 아니면 비어 있다. */
class ExprAbilityEvent : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityEvent::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] event of [ability] %string%"
            )
        }
    }

    private lateinit var abilityExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        abilityExpr = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<String?> {
        val id = abilityExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getAbility(id)?.eventName)
    }

    override fun isSingle() = true
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "event of ability ${abilityExpr.toString(e, d)}"
}

class ExprAbilityItem : SimpleExpression<String>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityItem::class.java, String::class.java,
                ExpressionType.COMBINED,
                "[the] item of [ability] %string%"
            )
        }
    }

    private lateinit var abilityExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        abilityExpr = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<String?> {
        val id = abilityExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getAbility(id)?.item?.name?.lowercase())
    }

    override fun isSingle() = true
    override fun getReturnType() = String::class.java
    override fun toString(e: Event?, d: Boolean) = "item of ability ${abilityExpr.toString(e, d)}"
}

/** 정의에 적힌 쿨타임(초). 남은 쿨타임이 아니라 원래 값. */
class ExprAbilityMaxCooldown : SimpleExpression<Int>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                ExprAbilityMaxCooldown::class.java, Int::class.javaObjectType,
                ExpressionType.COMBINED,
                "[the] max[imum] cooldown of [ability] %string%"
            )
        }
    }

    private lateinit var abilityExpr: Expression<String>

    @Suppress("UNCHECKED_CAST")
    override fun init(exprs: Array<out Expression<*>>, i: Int, k: Kleenean, p: SkriptParser.ParseResult): Boolean {
        abilityExpr = exprs[0] as Expression<String>
        return true
    }

    override fun get(event: Event): Array<Int?> {
        val id = abilityExpr.getSingle(event) ?: return arrayOfNulls(1)
        return arrayOf(AbilityRegistry.getAbility(id)?.cooldownSeconds)
    }

    override fun isSingle() = true
    override fun getReturnType() = Int::class.javaObjectType
    override fun toString(e: Event?, d: Boolean) = "max cooldown of ability ${abilityExpr.toString(e, d)}"
}
