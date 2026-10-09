package me.jfenn.bingo.common.commands

import kotlinx.serialization.json.Json
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.platform.IJsonSerializers
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.text.Formatting
import net.minecraft.ChatFormatting
import org.slf4j.Logger
import java.util.*
import kotlin.random.Random
import kotlin.random.nextInt

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

                literal("createteams") {
                    integer("teams") { teamsArg ->
                        executes {
                            val teamService = scope.get<TeamService>()
                            repeat(getArgument(teamsArg)) {
                                val rand = Random.nextInt(0..9999)
                                val teamTemplate = BingoTeam(
                                    id = "team$rand",
                                    name = text.literal("Team $rand"),
                                    textColor = Formatting.entries.take(15).random(),
                                    playerMap = mutableMapOf(
                                        UUID.randomUUID() to BingoTeam.Player(
                                            uuid = UUID.randomUUID(),
                                            name = "Player $rand",
                                        )
                                    )
                                )
                                teamService.restoreTeam(teamTemplate)
                            }
                        }
                    }
                }
            }
        }
    }
}