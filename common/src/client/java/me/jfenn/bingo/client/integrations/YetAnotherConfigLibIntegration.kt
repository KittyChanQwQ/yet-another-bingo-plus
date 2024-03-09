package me.jfenn.bingo.client.integrations

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.CardAlignment
import me.jfenn.bingo.common.config.ConfigService
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Formatting

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
                    .options(listOf(
                        Option.createBuilder<Boolean>()
                            .name(Text.translatable("bingo.config.support_client_hud"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.support_client_hud.description"),
                                    Text.empty(),
                                    Text.translatable("bingo.config.needs_restart").formatted(Formatting.YELLOW)
                                )
                            )
                            .binding(
                                config.supportClientHud,
                                { config.supportClientHud },
                                {
                                    config.supportClientHud = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller { BooleanControllerBuilder.create(it).coloured(true) }
                            .build(),
                        Option.createBuilder<Float>()
                            .name(Text.translatable("bingo.config.card_scale"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.card_scale.description")
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
                            .build(),
                        Option.createBuilder<CardAlignment>()
                            .name(Text.translatable("bingo.config.card_alignment"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.card_alignment.description")
                                )
                            )
                            .binding(
                                config.client.cardAlignment,
                                { config.client.cardAlignment },
                                {
                                    config.client.cardAlignment = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller {
                                EnumControllerBuilder.create(it)
                                    .enumClass(CardAlignment::class.java)
                                    .formatValue { value -> Text.translatable("bingo.config.card_alignment.${value.name.lowercase()}") }
                            }
                            .build(),
                        Option.createBuilder<Int>()
                            .name(Text.translatable("bingo.config.card_offset_x"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.card_offset_x.description")
                                )
                            )
                            .binding(
                                config.client.cardOffsetX,
                                { config.client.cardOffsetX },
                                {
                                    config.client.cardOffsetX = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller {
                                IntegerSliderControllerBuilder.create(it)
                                    .range(0, MinecraftClient.getInstance().window.scaledWidth/2)
                                    .step(1)
                            }
                            .build(),
                        Option.createBuilder<Int>()
                            .name(Text.translatable("bingo.config.card_offset_y"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.card_offset_y.description")
                                )
                            )
                            .binding(
                                config.client.cardOffsetY,
                                { config.client.cardOffsetY },
                                {
                                    config.client.cardOffsetY = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller {
                                IntegerSliderControllerBuilder.create(it)
                                    .range(0, MinecraftClient.getInstance().window.scaledHeight/2)
                                    .step(1)
                            }
                            .build(),
                    ))
                    .build()
            )
            .build()
            .generateScreen(parent)
    }
}
