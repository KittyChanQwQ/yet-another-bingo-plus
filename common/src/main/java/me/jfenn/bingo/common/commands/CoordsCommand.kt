package me.jfenn.bingo.common.commands

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.formatStringSmall
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class CoordsCommand(
    commandManager: ICommandManager,
) : BingoComponent() {

    @Serializable
    data class CoordinatesEntry(
        val player: String,
        val pos: BlockPosition,
        val dimension: String,
        val message: String?,
        val created: Instant,
    ) {

        fun formatText(): String {
            return with(pos) { "[$x, $y, $z] " } +
                    ("($dimension) ".takeIf { dimension != "overworld" } ?: "") +
                    (message ?: "")
        }

    }

    private fun IExecutionContext.storeCoordinates(player: IPlayerHandle, team: BingoTeam, message: String?) {
        val entry = CoordinatesEntry(
            player = player.playerName,
            pos = player.blockPos,
            dimension = player.world.registryKey.value.path.removePrefix("the_"),
            message = message,
            created = Clock.System.now(),
        )

        // add the coordinates message to the stored list
        team.chatCoordinates.add(entry)

        // send coordinates to the team
        ChatCommand.sendTeamMessage(player, team, Text.literal(entry.formatText()), scope.get<IPlayerManager>())
    }

    private fun listCoordinates(player: IPlayerHandle, team: BingoTeam) {
        val coordMessages = team.chatCoordinates
        if (coordMessages.isEmpty()) {
            player.sendMessage(Text.literal("No coordinates have been saved yet.").formatted(Formatting.GRAY))
            return
        }

        player.sendMessage(Text.literal("Coordinates:").formatted(Formatting.GRAY, Formatting.BOLD))
        for (entry in coordMessages) {
            val duration = Clock.System.now() - entry.created
            player.sendMessage(
                Text.literal("  • ${entry.player} (${duration.formatStringSmall()} ago): ${entry.formatText()}")
                    .formatted(Formatting.GRAY)
            )
        }
    }

    init {
        // send current player coordinates to other team members
        commandManager.register("coords") {
            requires {
                hasState(GameState.PLAYING, GameState.POSTGAME)
                        && hasPermission(Permission.COMMAND_COORDS)
                        && scope.get<TeamService>().isPlaying(player!!)
            }

            literal("list") {
                executes {
                    listCoordinates(player!!, BingoTeam.fromPlayer(player!!.player)!!)
                }
            }

            string("message", null, greedy = true) { message ->
                executes {
                    storeCoordinates(player!!, BingoTeam.fromPlayer(player!!.player)!!, getArgument(message))
                }
            }

            executes {
                storeCoordinates(player!!, BingoTeam.fromPlayer(player!!.player)!!, null)
            }
        }
    }

}