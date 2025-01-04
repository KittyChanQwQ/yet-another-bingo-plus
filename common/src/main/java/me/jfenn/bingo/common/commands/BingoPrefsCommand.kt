package me.jfenn.bingo.common.commands

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1

class BingoPrefsCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) : BingoComponent() {

    data class SettingEntry(
        val field: KMutableProperty1<PlayerSettings, Boolean>,
        val string: StringKey,
    )

    private fun IExecutionContext.toggleBooleanSetting(
        player: IPlayerHandle,
        field: KMutableProperty1<PlayerSettings, Boolean>,
        string: StringKey?,
    ) {
        val handler = scope.get<PlayerSettingsService>()

        val settings = handler.getPlayer(player)
        val newValue = !field.get(settings)
        changeBooleanSetting(player, field, string, newValue)
    }

    private fun IExecutionContext.changeBooleanSetting(
        player: IPlayerHandle,
        field: KMutableProperty1<PlayerSettings, Boolean>,
        string: StringKey?,
        value: Boolean,
    ) {
        val handler = scope.get<PlayerSettingsService>()

        val settings = handler.getPlayer(player)
        field.set(settings, value)
        handler.setPlayer(player, settings, seenTutorial = true)

        if (string != null) {
            sendMessage(
                text.string(StringKey.OptionsNotifyChanged, string, text.boolean(value))
            )
        }
    }

    init {
        commandManager.register("bingoprefs") {
            requires {
                hasPermission(Permission.CONFIGURE_PLAYER)
            }

            val fields = listOf(
                SettingEntry(PlayerSettings::bossbar, StringKey.PlayerSettingsBossbar),
                SettingEntry(PlayerSettings::scoreboard, StringKey.PlayerSettingsScoreboard),
                SettingEntry(PlayerSettings::scoreboardAutoHide, StringKey.PlayerSettingsScoreboardAutoHide),
                SettingEntry(PlayerSettings::leadingMessages, StringKey.PlayerSettingsMessagesLeading),
                SettingEntry(PlayerSettings::scoreMessages, StringKey.PlayerSettingsMessagesLines),
                SettingEntry(PlayerSettings::itemMessages, StringKey.PlayerSettingsMessagesItems),
                SettingEntry(PlayerSettings::nightVision, StringKey.PlayerSettingsNightVision),
            )

            for ((field, string) in fields) {
                literal(getFieldName(field)) {
                    boolean("value") { newValue ->
                        executes {
                            changeBooleanSetting(playerOrThrow, field, string, getArgument(newValue))
                        }
                    }

                    executes {
                        toggleBooleanSetting(playerOrThrow, field, string)
                    }
                }

                literal(getFieldName(PlayerSettings::hideLobbyPrompt)) {
                    boolean("value") { newValue ->
                        executes {
                            changeBooleanSetting(playerOrThrow, PlayerSettings::hideLobbyPrompt, null, getArgument(newValue))
                        }
                    }
                }
            }
        }
    }

    companion object {
        private fun getFieldName(
            field: KMutableProperty<*>,
        ): String {
            // replace any upper case letter to transform camelCase -> snake_case
            return field.name.replace(Regex("[A-Z]")) { match -> "_" + match.value.lowercase() }
        }

        fun <T> getCommand(
            field: KMutableProperty1<PlayerSettings, T>,
            value: T,
        ): String {
            return "/bingoprefs ${getFieldName(field)} $value"
        }
    }

}