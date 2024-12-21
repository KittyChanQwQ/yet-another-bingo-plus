package me.jfenn.bingo.common.commands

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.text.Text
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1

class BingoPrefsCommand(
    commandManager: ICommandManager,
) : BingoComponent() {

    private fun IExecutionContext.toggleBooleanSetting(
        player: IPlayerHandle,
        field: KMutableProperty1<PlayerSettings, Boolean>,
    ) {
        val handler = scope.get<PlayerSettingsService>()

        val settings = handler.getPlayer(player)
        val newValue = !field.get(settings)
        changeBooleanSetting(player, field, newValue)
    }

    private fun IExecutionContext.changeBooleanSetting(
        player: IPlayerHandle,
        field: KMutableProperty1<PlayerSettings, Boolean>,
        value: Boolean,
    ) {
        val handler = scope.get<PlayerSettingsService>()

        val settings = handler.getPlayer(player)
        field.set(settings, value)
        handler.setPlayer(player, settings, seenTutorial = true)

        sendInfoMessage(Text.literal("${field.name.formatTitle()}: $value"))
    }

    init {
        commandManager.register("bingoprefs") {
            requires {
                hasPermission(Permission.CONFIGURE_PLAYER)
            }

            val fields = listOf(
                PlayerSettings::bossbar,
                PlayerSettings::scoreboard,
                PlayerSettings::scoreboardAutoHide,
                PlayerSettings::leadingMessages,
                PlayerSettings::scoreMessages,
                PlayerSettings::itemMessages,
                PlayerSettings::nightVision,
                PlayerSettings::hideLobbyPrompt,
            )

            for (field in fields) {
                literal(getFieldName(field)) {
                    boolean("value") { newValue ->
                        executes {
                            changeBooleanSetting(playerOrThrow, field, getArgument(newValue))
                        }
                    }

                    executes {
                        toggleBooleanSetting(playerOrThrow, field)
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