package me.jfenn.bingo.client.integrations

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.ConfigService
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

interface YetAnotherConfigLibIntegration {

    fun buildConfigScreen(parent: Screen): Screen?

    companion object {
        fun hasYACL() = FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")

        fun create(configService: ConfigService, config: BingoConfig): YetAnotherConfigLibIntegration {
            return if (hasYACL())
                YetAnotherConfigLibIntegrationImpl(configService, config)
            else YetAnotherConfigLibIntegrationDummy()
        }
    }

}

class YetAnotherConfigLibIntegrationDummy : YetAnotherConfigLibIntegration {
    override fun buildConfigScreen(parent: Screen): Screen? = null
}

class YetAnotherConfigLibIntegrationImpl(
    private val configService: ConfigService,
    private val config: BingoConfig,
) : YetAnotherConfigLibIntegration {

    override fun buildConfigScreen(parent: Screen): Screen {
        return YetAnotherConfigLib.createBuilder()
            .title(Text.translatable("category.bingo"))
            .category(
                ConfigCategory.createBuilder()
                    .name(Text.translatable("bingo.config.gui"))
                    .tooltip(Text.translatable("bingo.config.gui.tooltip"))
                    .option(
                        Option.createBuilder<Float>()
                            .name(Text.translatable("bingo.config.gui.cardScale"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.gui.cardScale.description")
                                )
                            )
                            .binding(
                                config.client.cardScale,
                                { config.client.cardScale },
                                {
                                    config.client.cardScale = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller {
                                FloatSliderControllerBuilder.create(it)
                                    .range(0.25f, 4.0f)
                                    .step(0.1f)
                            }
                            .build()
                    )
                    .build()
            )
            .build()
            .generateScreen(parent)
    }
}
