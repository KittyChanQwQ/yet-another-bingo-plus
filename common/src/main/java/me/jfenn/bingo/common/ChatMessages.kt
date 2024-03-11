package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.ScoreService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.formatString
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import kotlin.time.Duration

object ChatMessages {

    fun broadcastLobbyMuted(player: ServerPlayerEntity, overlay: Boolean = false) {
        player.sendMessage(
            Text.literal("Certain sounds in this lobby may be muted to avoid spam.")
                .formatted(Formatting.GRAY, Formatting.ITALIC),
            overlay
        )
    }

    fun broadcastJoinedTeam(player: ServerPlayerEntity, team: BingoTeam) {
        player.sendMessage(
            Text.empty()
                .append(Text.literal("➾  You have been moved to ").formatted(Formatting.YELLOW))
                .append(team.text())
        )
    }

    fun broadcastStart(server: MinecraftServer, options: BingoOptions) {
        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
            yield(Text.literal("  BINGO Start!").formatted(Formatting.GREEN))

            Text.empty()
                .append("  Game mode: ")
                .append(options.formatGameMode())
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  Features: ")
                .append(options.formatFeatures())
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  Win condition: ")
                .append(options.goal.formatName())
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            server.playerManager.broadcast(it, false)
        }
    }

    fun broadcastEnd(server: MinecraftServer, options: BingoOptions, state: BingoState, reason: String?) {
        val winningTeams = state.getRegisteredTeams()
            .filter { it.isWinner() }

        // The winning team, if there is only one winner
        val winningTeam = winningTeams
            .takeIf { it.size == 1 }
            ?.firstOrNull()

        val winMessage = Text.literal(reason ?: "Game over!")
            .takeIf { options.isRankedMode }
            ?: winningTeam?.let { winner ->
                Text.empty()
                    .append(winner.text())
                    .append(Text.literal(" completed their card!").formatted(Formatting.YELLOW))
            }
            ?: ScoreService.getLeadingByLines(state)?.let { leader ->
                Text.empty()
                    .append(Text.literal(reason?.plus(" ") ?: "").formatted(Formatting.YELLOW))
                    .append(leader.text())
                    .append(Text.literal(" had more lines and won!").formatted(Formatting.YELLOW))
            }
            ?: ScoreService.getLeadingByItems(state)?.let { leader ->
                Text.empty()
                    .append(Text.literal(reason?.plus(" ") ?: "").formatted(Formatting.YELLOW))
                    .append(leader.text())
                    .append(Text.literal(" had more items and won!").formatted(Formatting.YELLOW))
            }
            ?: run {
                Text.empty()
                    .append(Text.literal(reason?.plus(" ") ?: "").formatted(Formatting.YELLOW))
                    .append(Text.literal("The game is a draw!").formatted(Formatting.YELLOW))
            }

        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
            yield(Text.empty().append("  ").append(winMessage))

            Text.empty()
                .append(Text.literal("  Game duration: ").formatted(Formatting.YELLOW))
                .append((state.ingameDuration() ?: Duration.ZERO).formatString())
                .also { yield(it) }

            Text.empty()
                .append(Text.literal("  Seed: "))
                .append(Texts.bracketedCopyable(server.overworld.seed.toString()))
                .also { yield(it) }

            if (options.isRankedMode) {
                yield(Text.empty())
                for (text in state.formatWinners())
                    yield(Text.empty().append("  ").append(text))
            }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            server.playerManager.broadcast(it, false)
        }

        for (player in server.playerManager.playerList) {
            // send the winning team title message
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO")))
            player.networkHandler.sendPacket(SubtitleS2CPacket(winMessage))

            Sounds.playGameOver(player)
        }
    }

    fun broadcastTeamCompleted(server: MinecraftServer, team: BingoTeam) {
        val message = Text.empty()
            .append(team.text())
            .append(Text.literal(" has completed their card!").formatted(Formatting.YELLOW))

        server.playerManager.playerList
            .forEach { it.sendMessage(message, false) }
    }

    fun broadcastScoreLeadingTeam(server: MinecraftServer, options: BingoOptions, configService: ConfigService, team: BingoTeam) {
        val score = team.score
        val message = Text.empty()
            .append(team.text())
            .append(Text.literal(
                " is in the lead"
                        + (if (options.showCompletedItems) " with ${score.items} item${"s".takeIf { score.items != 1 }.orEmpty()}" else "")
                        + "!"
            ).formatted(Formatting.YELLOW))

        server.playerManager.playerList
            .filter { configService.playerConfig[it.uuid].leadingMessages }
            .forEach { it.sendMessage(message, false) }
    }

    fun broadcastScoreTiedTeam(server: MinecraftServer, options: BingoOptions, configService: ConfigService, team: BingoTeam) {
        val score = team.score
        val message = Text.empty()
            .append(team.text())
            .append(Text.literal(
                " is tied in the lead"
                        + (if (options.showCompletedItems) " with ${score.items} item${"s".takeIf { score.items != 1 } ?: ""}" else "")
                        + "!"
            ).formatted(Formatting.YELLOW))

        server.playerManager.playerList
            .filter { configService.playerConfig[it.uuid].leadingMessages }
            .forEach { it.sendMessage(message, false) }
    }

}