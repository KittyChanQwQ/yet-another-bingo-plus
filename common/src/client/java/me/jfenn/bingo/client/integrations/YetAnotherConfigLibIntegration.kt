package me.jfenn.bingo.client.integrations

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import me.jfenn.bingo.client.common.settings.ClientSettingsService
import me.jfenn.bingo.common.config.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Formatting

interface YetAnotherConfigLibIntegration {

    fun buildConfigScreen(parent: Screen): Screen?

    companion object {
        fun hasYACL() = FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")

        fun create(
            configService: ConfigService,
            config: BingoConfig,
            clientSettings: ClientSettingsService,
        ): YetAnotherConfigLibIntegration {
            return if (hasYACL())
                YetAnotherConfigLibIntegrationImpl(configService, config, clientSettings)
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
    private val clientSettings: ClientSettingsService,
) : YetAnotherConfigLibIntegration {

    private val defaultConfig = BingoConfig()
    private val defaultPlayerSettings = PlayerSettings()

    private val playerSettings get() = clientSettings.getSettings()

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
                                defaultConfig.supportClientHud,
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
                                defaultConfig.client.cardScale,
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
                                defaultConfig.client.cardAlignment,
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
                        Option.createBuilder<CardOverlap>()
                            .name(Text.translatable("bingo.config.card_overlap"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.card_overlap.description")
                                )
                            )
                            .binding(
                                defaultConfig.client.cardOverlap,
                                { config.client.cardOverlap },
                                {
                                    config.client.cardOverlap = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller {
                                EnumControllerBuilder.create(it)
                                    .enumClass(CardOverlap::class.java)
                                    .formatValue { value -> Text.translatable("bingo.config.card_overlap.${value.name.lowercase()}") }
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
                                defaultConfig.client.cardOffsetX,
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
                                defaultConfig.client.cardOffsetY,
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
                        Option.createBuilder<Boolean>()
                            .name(Text.translatable("bingo.config.show_multiple_cards"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.show_multiple_cards.description")
                                )
                            )
                            .binding(
                                defaultConfig.client.showMultipleCards,
                                { config.client.showMultipleCards },
                                {
                                    config.client.showMultipleCards = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller { BooleanControllerBuilder.create(it) }
                            .build(),
                        Option.createBuilder<Boolean>()
                            .name(Text.translatable("bingo.config.hide_on_f3"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.hide_on_f3.description")
                                )
                            )
                            .binding(
                                defaultConfig.client.hideOnF3,
                                { config.client.hideOnF3 },
                                {
                                    config.client.hideOnF3 = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller { BooleanControllerBuilder.create(it) }
                            .build(),
                        Option.createBuilder<Boolean>()
                            .name(Text.translatable("bingo.config.hide_on_chat"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.config.hide_on_chat.description")
                                )
                            )
                            .binding(
                                defaultConfig.client.hideOnChat,
                                { config.client.hideOnChat },
                                {
                                    config.client.hideOnChat = it
                                    configService.writeConfig(config)
                                }
                            )
                            .controller { BooleanControllerBuilder.create(it) }
                            .build(),
                    ))
                    .build()
            )
            .category(
                ConfigCategory.createBuilder()
                    .name(Text.translatable("bingo.player_settings"))
                    .tooltip(Text.translatable("bingo.player_settings.tooltip"))
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Text.translatable("bingo.player_settings.bossbar"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.player_settings.bossbar.description")
                                )
                            )
                            .binding(
                                defaultPlayerSettings.bossbar,
                                { playerSettings.bossbar },
                                {
                                    clientSettings.update(playerSettings.copy(bossbar = it))
                                }
                            )
                            .controller { BooleanControllerBuilder.create(it) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Text.translatable("bingo.player_settings.scoreboard"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.player_settings.scoreboard.description")
                                )
                            )
                            .binding(
                                defaultPlayerSettings.scoreboard,
                                { playerSettings.scoreboard },
                                {
                                    clientSettings.update(playerSettings.copy(scoreboard = it))
                                }
                            )
                            .controller { BooleanControllerBuilder.create(it) }
                            .build()
                    )
                    .let { builder ->
                        Option.createBuilder<Boolean>()
                            .name(Text.translatable("bingo.player_settings.scoreboard_auto_hide"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.player_settings.scoreboard_auto_hide.description")
                                )
                            )
                            .binding(
                                defaultPlayerSettings.scoreboardAutoHide,
                                { playerSettings.scoreboardAutoHide },
                                {
                                    clientSettings.update(playerSettings.copy(scoreboardAutoHide = it))
                                }
                            )
                            .controller { BooleanControllerBuilder.create(it) }
                            .build()
                            .takeIf { !config.supportClientHud }
                            ?.let { builder.option(it) }
                            ?: builder
                    }
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Text.translatable("bingo.player_settings.night_vision"))
                            .description(
                                OptionDescription.of(
                                    Text.translatable("bingo.player_settings.night_vision.description")
                                )
                            )
                            .binding(
                                defaultPlayerSettings.nightVision,
                                { playerSettings.nightVision },
                                {
                                    clientSettings.update(playerSettings.copy(nightVision = it))
                                }
                            )
                            .controller { BooleanControllerBuilder.create(it) }
                            .build(),
                    )
                    .group(
                        OptionGroup.createBuilder()
                            .collapsed(false)
                            .name(Text.translatable("bingo.player_settings.messages"))
                            .option(
                                Option.createBuilder<Boolean>()
                                    .name(Text.translatable("bingo.player_settings.messages.leading"))
                                    .description(
                                        OptionDescription.of(
                                            Text.translatable("bingo.player_settings.messages.leading.description")
                                        )
                                    )
                                    .binding(
                                        defaultPlayerSettings.leadingMessages,
                                        { playerSettings.leadingMessages },
                                        {
                                            clientSettings.update(playerSettings.copy(leadingMessages = it))
                                        }
                                    )
                                    .controller { BooleanControllerBuilder.create(it) }
                                    .build()
                            )
                            .option(
                                Option.createBuilder<Boolean>()
                                    .name(Text.translatable("bingo.player_settings.messages.lines"))
                                    .description(
                                        OptionDescription.of(
                                            Text.translatable("bingo.player_settings.messages.lines.description")
                                        )
                                    )
                                    .binding(
                                        defaultPlayerSettings.scoreMessages,
                                        { playerSettings.scoreMessages },
                                        {
                                            clientSettings.update(playerSettings.copy(scoreMessages = it))
                                        }
                                    )
                                    .controller { BooleanControllerBuilder.create(it) }
                                    .build(),
                            )
                            .build()
                    )
                    .build(),

            )
            .build()
            .generateScreen(parent)
    }
}
