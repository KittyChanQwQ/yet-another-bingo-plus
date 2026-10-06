package me.jfenn.bingo.common.config

import assertk.assertThat
import assertk.assertions.isDataClassEqualTo
import me.jfenn.bingo.common.isEqualToFile
import me.jfenn.bingo.integrations.voice.VoiceConfig
import me.jfenn.bingo.platform.IModEnvironment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.*
import kotlin.io.path.writeText
import kotlin.reflect.KType
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

    companion object {
        @JvmStatic
        private fun testArgs() = listOf(
            Arguments.of("config.jsonc", typeOf<BingoConfig>(), BingoConfig(
                statsHostId = UUID(0L, 0L)
            )),
            Arguments.of("voice-chat.jsonc", typeOf<VoiceConfig>(), VoiceConfig())
        )
    }

    @ParameterizedTest
    @MethodSource("testArgs")
    fun `given the {0} configuration, when written to a file, the result matches the expected snapshot`(
        name: String,
        type: KType,
        value: Any
    ) {
        subject.write(type, name, value)
        assertThat(tempDir.resolve(name).toFile().readText()).isEqualToFile("/config/${name}")
    }

    @ParameterizedTest
    @MethodSource("testArgs")
    fun `given the configuration snapshot, when read to the config class, the result matches the default values`(
        name: String,
        type: KType,
        value: Any
    ) {
        val actualConfigJson = this::class.java.getResource("/config/${name}")!!.readText()
        tempDir.resolve(name).writeText(actualConfigJson)

        val actualConfig = subject.read<Any>(type, name)
        assertThat(actualConfig).isDataClassEqualTo(value)
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