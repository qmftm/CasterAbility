package me.qmftm.casterability.util

import me.qmftm.casterability.CasterAbility
import java.io.File

/**
 * 플러그인이 켜질 때 예제 Skript 파일을 `plugins/CasterAbility/examples/` 에 꺼내둡니다.
 *
 * Skript의 scripts 폴더에 직접 쓰지 않는 이유:
 * 남의 플러그인 폴더에 실행되는 스크립트를 말없이 넣으면 서버가 켜지자마자
 * 예상 못 한 능력이 등록됩니다. 여기에 꺼내두고, 쓸 것만 복사하게 합니다.
 *
 * 이미 있는 파일은 덮어쓰지 않습니다. 예제를 다시 받고 싶으면 그 파일을 지우고
 * 서버를 재시작하면 됩니다.
 */
object ExampleScripts {

    private val FILES = listOf(
        "README.txt",
        "yandere.sk",
        "menhera.sk",
    )

    fun install(plugin: CasterAbility) {
        val dir = File(plugin.dataFolder, "examples")
        if (!dir.isDirectory && !dir.mkdirs()) {
            plugin.logger.warning("예제 폴더를 만들지 못했습니다: ${dir.path}")
            return
        }

        val written = mutableListOf<String>()
        for (name in FILES) {
            val target = File(dir, name)
            if (target.exists()) continue

            val resource = plugin.getResource("examples/$name")
            if (resource == null) {
                plugin.logger.warning("JAR 안에 예제 리소스가 없습니다: examples/$name")
                continue
            }
            runCatching {
                resource.use { input -> target.outputStream().use { input.copyTo(it) } }
            }.onFailure {
                plugin.logger.warning("예제 파일을 쓰지 못했습니다 (${name}): ${it.message}")
                return@onFailure
            }.onSuccess { written += name }
        }

        if (written.isNotEmpty()) {
            plugin.logger.info("예제 스크립트를 생성했습니다: ${written.joinToString(", ")}")
            plugin.logger.info("위치: ${dir.path}")
            plugin.logger.info("쓰려면 plugins/Skript/scripts/ 로 복사한 뒤 /sk reload all 하세요.")
        }
    }
}
