package me.jfenn.bingo.common.config

import assertk.assertThat
import assertk.assertions.isDataClassEqualTo
import me.jfenn.bingo.common.isEqualToFile
import me.jfenn.bingo.platform.IModEnvironment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import kotlin.io.path.writeText
import kotlin.reflect.typeOf

class ConfigManagerTest {
    @TempDir
    lateinit var tempDir: Path

    private fun fakeModEnvironment(configDir: Path) = object : IModEnvironment {
        override val configDir: Path = configDir
        override val gameDir: Path get() = throw NotImplementedError()
        override val envType: IModEnvironment.EnvType get() = throw NotImplementedError()
        override fun isModLoaded(modId: String) = throw NotImplementedError()
    }

    val subject: ConfigManager get() = ConfigManager(
        environment = fakeModEnvironment(tempDir)
    )

    @Test
    fun `given the default configuration, when written to a file, the result matches the expected snapshot`() {
        val config = BingoConfig(
            statsHostId = UUID(0L, 0L)
        )
        subject.write(typeOf<BingoConfig>(), "config.json", config)
        assertThat(tempDir.resolve("config.json").toFile().readText()).isEqualToFile("/config/default.json")
    }

    @Test
    fun `given the configuration snapshot, when read to the config class, the result matches the default values`() {
        val expectedConfig = BingoConfig(
            statsHostId = UUID(0L, 0L)
        )
        val actualConfigJson = this::class.java.getResource("/config/default.json")!!.readText()
        tempDir.resolve("config-fixture.json").writeText(actualConfigJson)

        val actualConfig = subject.read<BingoConfig>(typeOf<BingoConfig>(), "config-fixture.json")
        assertThat(actualConfig).isDataClassEqualTo(expectedConfig)
    }

    @Test
    fun `given a non-default configuration, when written and read back, the result matches the original`() {
        val config = BingoConfig(
            difficultyPresets = mapOf(
                "ohno" to listOf(25, 0, 0, 0, 0)
            ),
            server = ServerConfig(
                fixedSeed = "meow".hashCode().toLong(),
                fixedSeedEnabled = true,
            )
        )
        subject.write(typeOf<BingoConfig>(), "config-modified.json", config)
        val actual = subject.read<BingoConfig>(typeOf<BingoConfig>(), "config-modified.json")
        assertThat(actual).isDataClassEqualTo(config)
    }
}