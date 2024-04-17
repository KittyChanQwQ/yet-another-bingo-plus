package me.jfenn.bingo.common.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.sendInfoMessage
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.koin.core.scope.Scope
import kotlin.reflect.KMutableProperty1

class BingoPrefsCommand : BaseCommand() {

    fun changeBooleanSetting(
        player: ServerPlayerEntity,
        field: KMutableProperty1<PlayerSettings, Boolean>,
        value: Boolean,
        koinScope: Scope,
    ) {
        val handler = koinScope.get<PlayerSettingsService>()

        val settings = handler.getPlayer(player)
        field.set(settings, value)
        handler.setPlayer(player, settings, seenTutorial = true)

        player.sendInfoMessage("${field.name.formatTitle()}: $value")
    }

    init {
        var prefsCommand = LiteralArgumentBuilder.literal<ServerCommandSource>("bingoprefs")
            .require(
                requireBingoInstance(),
                requirePermission(Permission.CONFIGURE_PLAYER),
            )

        val fields = listOf(
            PlayerSettings::bossbar,
            PlayerSettings::scoreboard,
            PlayerSettings::scoreboardAutoHide,
            PlayerSettings::leadingMessages,
            PlayerSettings::scoreMessages,
            PlayerSettings::nightVision,
        )

        for (field in fields) {
            // replace any upper case letter to transform camelCase -> snake_case
            val fieldName = field.name.replace(Regex("[A-Z]")) { match -> "_" + match.value.lowercase() }
            prefsCommand = prefsCommand.then(
                LiteralArgumentBuilder.literal<ServerCommandSource>(fieldName)
                    .then(
                        RequiredArgumentBuilder.argument<ServerCommandSource, Boolean>(
                            "value",
                            BoolArgumentType.bool()
                        )
                            .executesInScope { ctx ->
                                val player = ctx.source.player ?: return@executesInScope 0
                                val value = BoolArgumentType.getBool(ctx, "value")

                                changeBooleanSetting(player, field, value, this)
                                1
                            }
                    )
            )
        }

        prefsCommand.register()
    }

}