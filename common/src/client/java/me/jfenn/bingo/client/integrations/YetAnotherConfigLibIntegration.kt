package me.jfenn.bingo.client.integrations

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import me.jfenn.bingo.client.common.settings.ClientSettingsService
import me.jfenn.bingo.common.config.*
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
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
            text: TextProvider,
        ): YetAnotherConfigLibIntegration {
            return if (hasYACL())
                YetAnotherConfigLibIntegrationImpl(configService, config, clientSettings, text)
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
    private val text: TextProvider,
) : YetAnotherConfigLibIntegration {

    private val defaultConfig = BingoConfig()
    private val defaultPlayerSettings = PlayerSettings()

    private val playerSettings get() = clientSettings.getSettings()

    override fun buildConfigScreen(parent: Screen): Screen {
        return YetAnotherConfigLib.createBuilder()
            .title(text.string(StringKey.FullName))
            .category(
                ConfigCategory.createBuilder()
                    .name(text.string(StringKey.ConfigGui))
                    .tooltip(text.string(StringKey.ConfigGuiTooltip))
                    .options(listOf(
                        Option.createBuilder<Boolean>()
                            .name(text.string(StringKey.ConfigSupportClientHud))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigSupportClientHudDescription),
                                    Text.empty(),
                                    text.string(StringKey.ConfigNeedsRestart).formatted(Formatting.YELLOW),
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
                            .name(text.string(StringKey.ConfigCardScale))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigCardScaleDescription)
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
                            .name(text.string(StringKey.ConfigCardAlignment))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigCardAlignmentDescription)
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
                                    .formatValue { value -> text.string(value.string) }
                            }
                            .build(),
                        Option.createBuilder<CardOverlap>()
                            .name(text.string(StringKey.ConfigCardOverlap))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigCardOverlapDescription)
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
                                    .formatValue { value -> text.string(value.string) }
                            }
                            .build(),
                        Option.createBuilder<Int>()
                            .name(text.string(StringKey.ConfigCardOffsetX))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigCardOffsetXDescription)
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
                            .name(text.string(StringKey.ConfigCardOffsetY))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigCardOffsetYDescription)
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
                            .name(text.string(StringKey.ConfigShowMultipleCards))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigShowMultipleCardsDescription)
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
                            .name(text.string(StringKey.ConfigHideOnF3))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigHideOnF3Description)
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
                            .name(text.string(StringKey.ConfigHideOnChat))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.ConfigHideOnChatDescription)
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
                    .name(text.string(StringKey.PlayerSettings))
                    .tooltip(text.string(StringKey.PlayerSettingsTooltip))
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(text.string(StringKey.PlayerSettingsBossbar))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.PlayerSettingsBossbarDescription)
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
                            .name(text.string(StringKey.PlayerSettingsScoreboard))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.PlayerSettingsScoreboardDescription)
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
                            .name(text.string(StringKey.PlayerSettingsScoreboardAutoHide))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.PlayerSettingsScoreboardAutoHideDescription)
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
                            .name(text.string(StringKey.PlayerSettingsNightVision))
                            .description(
                                OptionDescription.of(
                                    text.string(StringKey.PlayerSettingsNightVisionDescription)
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
                            .name(text.string(StringKey.PlayerSettingsMessages))
                            .option(
                                Option.createBuilder<Boolean>()
                                    .name(text.string(StringKey.PlayerSettingsMessagesLeading))
                                    .description(
                                        OptionDescription.of(
                                            text.string(StringKey.PlayerSettingsMessagesLeadingDescription)
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
                                    .name(text.string(StringKey.PlayerSettingsMessagesLines))
                                    .description(
                                        OptionDescription.of(
                                            text.string(StringKey.PlayerSettingsMessagesLinesDescription)
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
