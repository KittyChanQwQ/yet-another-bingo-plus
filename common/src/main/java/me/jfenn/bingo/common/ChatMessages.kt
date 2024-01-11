package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.ScoreService
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.utils.formatString
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import kotlin.time.Duration

object ChatMessages {

    fun broadcastJoinedTeam(player: ServerPlayerEntity, team: BingoTeam) {
        player.sendMessage(
            Text.empty()
                .append(Text.literal("➾  You have been moved to ").formatted(Formatting.YELLOW))
                .append(team.text(player.server))
        )
    }

    fun broadcastStart(ctx: BingoContext) {
        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
            yield(Text.literal("  BINGO Start!").formatted(Formatting.GREEN))

            Text.empty()
                .append("  Game mode: ")
                .append(ctx.config.formatGameMode())
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  Features: ")
                .append(ctx.config.formatFeatures())
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  Win condition: ")
                .append(ctx.config.winCondition.formatName())
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            ctx.server.playerManager.broadcast(it, false)
        }
    }

    fun broadcastEnd(ctx: BingoContext, reason: String?) {
        val winningTeams = ctx.state.getRegisteredTeams()
            .filter { it.isWinner() }

        // The winning team, if there is only one winner
        val winningTeam = winningTeams
            .takeIf { it.size == 1 }
            ?.firstOrNull()

        val winMessage = Text.literal(reason ?: "Game over!")
            .takeIf { ctx.config.isRankedMode }
            ?: winningTeam?.let { winner ->
                Text.empty()
                    .append(winner.text(ctx.server))
                    .append(Text.literal(" completed their card!").formatted(Formatting.YELLOW))
            }
            ?: ScoreService.getLeadingByLines(ctx.state)?.let { leader ->
                Text.empty()
                    .append(Text.literal(reason?.plus(" ") ?: "").formatted(Formatting.YELLOW))
                    .append(leader.text(ctx.server))
                    .append(Text.literal(" had more lines and won!").formatted(Formatting.YELLOW))
            }
            ?: ScoreService.getLeadingByItems(ctx.state)?.let { leader ->
                Text.empty()
                    .append(Text.literal(reason?.plus(" ") ?: "").formatted(Formatting.YELLOW))
                    .append(leader.text(ctx.server))
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
                .append((ctx.state.ingameDuration() ?: Duration.ZERO).formatString())
                .also { yield(it) }

            Text.empty()
                .append(Text.literal("  Seed: "))
                .append(Texts.bracketedCopyable(ctx.server.overworld.seed.toString()))
                .also { yield(it) }

            if (ctx.config.isRankedMode) {
                yield(Text.empty())
                for (text in ctx.state.formatWinners())
                    yield(Text.empty().append("  ").append(text))
            }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            ctx.server.playerManager.broadcast(it, false)
        }

        for (player in ctx.server.playerManager.playerList) {
            // send the winning team title message
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO")))
            player.networkHandler.sendPacket(SubtitleS2CPacket(winMessage))

            Sounds.playGameOver(player)
        }
    }

    fun broadcastTeamCompleted(ctx: BingoContext, team: BingoTeam) {
        val message = Text.empty()
            .append(team.text(ctx.server))
            .append(Text.literal(" has completed their card!").formatted(Formatting.YELLOW))

        ctx.server.playerManager.playerList
            .forEach { it.sendMessage(message, false) }
    }

    fun broadcastScoreLeadingTeam(ctx: BingoContext, team: BingoTeam) {
        val score = team.score
        val message = Text.empty()
            .append(team.text(ctx.server))
            .append(Text.literal(
                " is in the lead"
                        + (if (ctx.config.showCompletedItems) " with ${score.items} item${"s".takeIf { score.items != 1 }.orEmpty()}" else "")
                        + "!"
            ).formatted(Formatting.YELLOW))

        ctx.server.playerManager.playerList
            .filter { ConfigService.getPlayer(it.uuidAsString).leadingMessages }
            .forEach { it.sendMessage(message, false) }
    }

    fun broadcastScoreTiedTeam(ctx: BingoContext, team: BingoTeam) {
        val score = team.score
        val message = Text.empty()
            .append(team.text(ctx.server))
            .append(Text.literal(
                " is tied in the lead"
                        + (if (ctx.config.showCompletedItems) " with ${score.items} item${"s".takeIf { score.items != 1 } ?: ""}" else "")
                        + "!"
            ).formatted(Formatting.YELLOW))

        ctx.server.playerManager.playerList
            .filter { ConfigService.getPlayer(it.uuidAsString).leadingMessages }
            .forEach { it.sendMessage(message, false) }
    }

}