package me.qmftm.casterability.util

import me.qmftm.casterability.CasterAbility
import org.bukkit.Bukkit
import java.io.File

/**
 * 기본 능력(classic) 세트를 설치합니다.
 *
 * 설치 위치는 항상 여기입니다.
 *
 *     plugins/Skript/scripts/CasterAbility/classic/<능력>.sk
 *
 * Skript는 자기 scripts 폴더 아래만 읽습니다. 다른 곳에 두면 손으로 옮기기
 * 전까지 기본 능력이 되지 않으므로, 다른 위치로 물러나지 않습니다.
 * Skript를 못 찾으면 설치를 건너뜁니다. (plugin.yml에 depend로 걸려 있어
 * 정상적인 서버에서는 일어나지 않습니다.)
 *
 * 설치 판단은 **classic 폴더가 있는지**로 합니다.
 * - 폴더가 없으면 처음 설치로 보고 전부 씁니다.
 * - 폴더가 있으면 손대지 않습니다. 그래서 마음에 안 드는 능력 파일 하나를
 *   지워도 재시작할 때 되살아나지 않습니다.
 * - 기본 능력을 처음 상태로 되돌리려면 classic 폴더째 지우고 재시작하세요.
 *
 * config.yml의 `classic.install: false` 로 끌 수 있습니다.
 */
object ClassicAbilities {

    /** JAR의 resources/classic/ 에 들어 있는 파일 목록 */
    private val ABILITIES = listOf(
        "yandere.sk",
        "menhera.sk",
        "pig.sk",
    )

    private const val SYNTAX_DOC = "SYNTAX.txt"

    /** Skript 데이터 폴더 기준 설치 경로 */
    private const val CLASSIC_PATH = "scripts/CasterAbility/classic"

    fun install(plugin: CasterAbility) {
        // 문법 문서는 스크립트가 아니므로 플러그인 폴더에 둔다
        writeIfAbsent(plugin, File(plugin.dataFolder, SYNTAX_DOC), SYNTAX_DOC)

        if (!plugin.config.getBoolean("classic.install", true)) return

        val skript = Bukkit.getPluginManager().getPlugin("Skript")
        if (skript == null) {
            plugin.logger.severe("Skript를 찾을 수 없어 기본 능력을 설치하지 못했습니다.")
            return
        }

        val dir = File(skript.dataFolder, CLASSIC_PATH)
        if (dir.isDirectory) return   // 이미 설치됨 — 건드리지 않는다

        // scripts 폴더가 아직 없어도 통째로 만든다
        if (!dir.mkdirs()) {
            plugin.logger.warning("기본 능력 폴더를 만들지 못했습니다: ${dir.path}")
            return
        }

        val written = ABILITIES.filter { name ->
            writeIfAbsent(plugin, File(dir, name), "classic/$name")
        }
        if (written.isEmpty()) return

        plugin.logger.info("기본 능력을 설치했습니다: ${written.joinToString(", ")}")
        plugin.logger.info("위치: ${dir.path}")
        plugin.logger.info("적용하려면 /sk reload all 을 실행하거나 서버를 재시작하세요.")
    }

    /** @return 실제로 새로 썼으면 true */
    private fun writeIfAbsent(plugin: CasterAbility, target: File, resource: String): Boolean {
        if (target.exists()) return false

        val stream = plugin.getResource(resource)
        if (stream == null) {
            plugin.logger.warning("JAR 안에 리소스가 없습니다: $resource")
            return false
        }
        target.parentFile?.mkdirs()
        return runCatching {
            stream.use { input -> target.outputStream().use { input.copyTo(it) } }
        }.onFailure {
            plugin.logger.warning("파일을 쓰지 못했습니다 (${target.name}): ${it.message}")
        }.isSuccess
    }
}
