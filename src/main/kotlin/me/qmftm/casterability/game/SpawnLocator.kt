package me.qmftm.casterability.game

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import kotlin.random.Random

/**
 * 스폰 지점을 고릅니다. ChzzkAbility의 teleport.sk / World.sk 포팅입니다.
 *
 * 원본은 바다 · 강 · 사막을 피해서 지점을 뽑고, 청크를 불러온 뒤 텔레포트했습니다.
 * 같은 규칙을 유지하되 다음을 고쳤습니다.
 *
 * - 원본 caRandomTeleport는 재시도할 때마다 {_x}, {_z}를 직전 값 기준으로 다시 뽑아
 *   시도를 거듭할수록 중심에서 멀어졌습니다. 항상 중심 기준으로 뽑습니다.
 * - 원본은 재시도 횟수 {_loop}를 초기화하지 않아 함수 재호출 시 이어서 셌습니다.
 * - 못 찾았을 때 아무 곳도 돌려주지 않던 경로를 없애고, 마지막 후보를 돌려줍니다.
 */
object SpawnLocator {

    /** 이 단어가 바이옴 이름에 들어가면 스폰 후보에서 제외 */
    private val BAD_BIOMES = listOf("ocean", "river", "desert")

    fun isBadBiome(block: Block): Boolean {
        val name = block.biome.key.key.lowercase()
        return BAD_BIOMES.any { name.contains(it) }
    }

    /**
     * 중심에서 radius 안의 지상 지점을 찾습니다.
     * attempts번 안에 좋은 바이옴을 못 찾으면 마지막 후보를 그대로 돌려줍니다.
     */
    fun findSpawn(
        world: World,
        centerX: Double,
        centerZ: Double,
        radius: Int,
        attempts: Int = 10,
    ): Location {
        var last: Location? = null
        repeat(attempts.coerceAtLeast(1)) {
            val x = centerX + Random.nextDouble(-radius.toDouble(), radius.toDouble())
            val z = centerZ + Random.nextDouble(-radius.toDouble(), radius.toDouble())
            val block = world.getHighestBlockAt(x.toInt(), z.toInt())
            val loc = block.location.add(0.5, 1.0, 0.5)
            last = loc
            if (!isBadBiome(block)) return loc
        }
        return last ?: world.spawnLocation
    }

    /**
     * 게임 월드의 중심 스폰을 찾습니다 (World.sk의 caWorldGenerate 부분).
     *
     * 후보 지점 주변을 표본으로 확인해서, 대부분이 바다/강/사막이면
     * 중심을 바깥으로 밀어가며 다시 찾습니다.
     *
     * @param step 한 번 실패할 때마다 중심을 밀어낼 거리
     */
    fun findWorldCenter(
        world: World,
        sampleRadius: Int,
        step: Int = 500,
        maxTries: Int = 10,
        samples: Int = 5,
        required: Int = 4,
    ): Location {
        for (tryIndex in 0 until maxTries.coerceAtLeast(1)) {
            val centerX = (tryIndex * step).toDouble()
            val centerZ = (tryIndex * step).toDouble()

            var good = 0
            repeat(samples) {
                val x = centerX + Random.nextDouble(-sampleRadius.toDouble(), sampleRadius.toDouble())
                val z = centerZ + Random.nextDouble(-sampleRadius.toDouble(), sampleRadius.toDouble())
                if (!isBadBiome(world.getHighestBlockAt(x.toInt(), z.toInt()))) good++
            }

            if (good >= required) {
                return world.getHighestBlockAt(centerX.toInt(), centerZ.toInt())
                    .location.add(0.5, 1.0, 0.5)
            }
        }
        return world.spawnLocation
    }
}
