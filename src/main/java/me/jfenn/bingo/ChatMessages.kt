package me.jfenn.bingo

import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.ScoreService
import me.jfenn.bingo.config.ConfigService
import me.jfenn.bingo.map.MapItemService
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.utils.formatString
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import kotlin.time.Duration

object ChatMessages {

    fun broadcastEnd(ctx: BingoContext, reason: String?) {
        val winMessage = ScoreService.getWinner(ctx.state)?.let { winner ->
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

    fun broadcastScoreLeadingTeam(ctx: BingoContext, team: BingoTeam) {
        val score = ctx.state.getTeamScore(team)
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
        val score = ctx.state.getTeamScore(team)
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