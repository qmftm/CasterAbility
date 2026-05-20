package me.qmftm.casterability.config

import me.qmftm.casterability.CasterAbility

data class GameConfig(
    val basicHealth: Int,
    val basicLevel: Int,
    val autoSkipSecond: Int,
    val abilityChangeCount: Int,
    val infinityHunger: Boolean,
    val infinityDuration: Boolean,
    val cooldownBow: Boolean,
    val cooldownShield: Boolean,
    val weatherClear: Boolean,
    val wreck: Int,

    val worldName: String,
    val randomSpawn: Boolean,
    val randomRadius: Int,

    val dropItems: Boolean,
    val dropExp: Boolean,
    val kick: Boolean,

    val invincibilityEnable: Boolean,
    val invincibilitySecond: Int,
    val invincibilityShowBossbar: Boolean,
    val invincibilityInvisible: Boolean,

    val worldborderEnable: Boolean,
    val worldborderMaxRadius: Int,
    val worldborderMinRadius: Int,
    val worldborderShrinkSecond: Int,
    val worldborderShrinkCount: Int,
    val worldborderShrinkRandomCenter: Boolean,
    val worldborderShowBossbar: Boolean,
) {
    companion object {
        fun load(plugin: CasterAbility): GameConfig {
            plugin.reloadConfig()
            val c = plugin.config
            return GameConfig(
                basicHealth           = c.getInt("game.basic_health", 20),
                basicLevel            = c.getInt("game.basic_level", 50),
                autoSkipSecond        = c.getInt("game.auto_skip_second", 80),
                abilityChangeCount    = c.getInt("game.ability_change_count", 3),
                infinityHunger        = c.getBoolean("game.infinity_hunger", true),
                infinityDuration      = c.getBoolean("game.infinity_duration", true),
                cooldownBow           = c.getBoolean("game.cooldown_bow", false),
                cooldownShield        = c.getBoolean("game.cooldown_shield", false),
                weatherClear          = c.getBoolean("game.weather_clear", false),
                wreck                 = c.getInt("game.wreck", 0),

                worldName             = c.getString("spawn.world_name", "casterability") ?: "casterability",
                randomSpawn           = c.getBoolean("spawn.random_spawn", true),
                randomRadius          = c.getInt("spawn.random_radius", 100),

                dropItems             = c.getBoolean("death.drop_items", true),
                dropExp               = c.getBoolean("death.drop_exp", false),
                kick                  = c.getBoolean("death.kick", false),

                invincibilityEnable       = c.getBoolean("invincibility.enable", true),
                invincibilitySecond       = c.getInt("invincibility.second", 90),
                invincibilityShowBossbar  = c.getBoolean("invincibility.show_bossbar", true),
                invincibilityInvisible    = c.getBoolean("invincibility.invisible", true),

                worldborderEnable           = c.getBoolean("worldborder.enable", true),
                worldborderMaxRadius        = c.getInt("worldborder.max_radius", 500),
                worldborderMinRadius        = c.getInt("worldborder.min_radius", 5),
                worldborderShrinkSecond     = c.getInt("worldborder.shrink_second", 60),
                worldborderShrinkCount      = c.getInt("worldborder.shrink_count", 5),
                worldborderShrinkRandomCenter = c.getBoolean("worldborder.shrink_random_center", true),
                worldborderShowBossbar      = c.getBoolean("worldborder.show_bossbar", true),
            )
        }
    }
}