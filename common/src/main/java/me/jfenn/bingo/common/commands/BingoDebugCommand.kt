package me.jfenn.bingo.common.commands

import kotlinx.serialization.json.Json
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.platform.IJsonSerializers
import me.jfenn.bingo.platform.commands.ICommandManager
import net.minecraft.ChatFormatting
import org.slf4j.Logger

class BingoDebugCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
    private val log: Logger,
) : BingoComponent() {

    init {
        commandManager.register("bingo") {
            literal("debug") {
                requires {
                    hasPermission(Permission.COMMAND_DEBUG)
                }
                executes {
                    val state = scope.get<BingoState>()
                    val json = Json(scope.get<IJsonSerializers>().json) {
                        prettyPrint = true
                    }

                    log.info(json.encodeToString(state))

                    sendMessage(text.literal("Debug info printed to server logs.").formatted(ChatFormatting.GRAY))
                }
            }
        }
    }
}