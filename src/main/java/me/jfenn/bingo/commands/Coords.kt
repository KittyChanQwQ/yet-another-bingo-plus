package me.jfenn.bingo.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.GameState
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.utils.formatStringSmall
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import java.time.Duration
import java.time.Instant

object Coords {

    data class CoordinatesEntry(
        val player: String,
        val pos: BlockPos,
        val message: String?,
        val created: Instant,
    ) {

        fun formatText(): String {
            return with(pos) { "[$x, $y, $z] " } + (message ?: "")
        }

    }

    val coordinates = mutableMapOf<BingoTeam, MutableList<CoordinatesEntry>>()

    fun storeCoordinates(player: PlayerEntity, team: BingoTeam, message: String?) {
        val entry = CoordinatesEntry(
            player = player.entityName,
            pos = player.blockPos,
            message = message,
            created = Instant.now(),
        )

        // add the coordinates message to the stored list
        val list = coordinates.getOrPut(team) { mutableListOf() }
        list.add(entry)

        // send coordinates to the team
        team.broadcastMessage(
            Text.empty()
                .append(Text.literal("TEAM ${player.entityName}").formatted(team.formatting))
                .append(Text.literal(": ${entry.formatText()}"))
        )
    }

    fun listCoordinates(player: PlayerEntity, team: BingoTeam) {
        val coordMessages = coordinates[team] ?: emptyList()
        if (coordMessages.isEmpty()) return

        player.sendMessage(Text.literal("Coordinates:").formatted(Formatting.GRAY, Formatting.BOLD))
        for (entry in coordMessages) {
            val duration = Duration.between(entry.created, Instant.now())
            player.sendMessage(Text.literal("  • ${entry.player} (${duration.formatStringSmall()} ago): ${entry.formatText()}").formatted(Formatting.GRAY))
        }
    }

    fun register(): Sequence<LiteralArgumentBuilder<ServerCommandSource>> = sequence {
        // send current player coordinates to other team members
        LiteralArgumentBuilder.literal<ServerCommandSource?>("coords")
            .requires { BingoGame.state == GameState.PLAYING }
            .requires { ctx -> ctx.player?.let { BingoTeam.fromPlayer(it) } != null }
            .then(
                LiteralArgumentBuilder.literal<ServerCommandSource?>("list")
                    .executes { ctx ->
                        val player = ctx.source.player ?: return@executes 0
                        val team = BingoTeam.fromPlayer(player) ?: return@executes 0
                        listCoordinates(player, team)
                        1
                    }
            )
            .then(
                RequiredArgumentBuilder.argument<ServerCommandSource?, String?>(
                    "message",
                    StringArgumentType.greedyString()
                )
                    .executes { ctx ->
                        val player = ctx.source.player ?: return@executes 0
                        val team = BingoTeam.fromPlayer(player) ?: return@executes 0
                        val message = StringArgumentType.getString(ctx, "message")

                        storeCoordinates(player, team, message)
                        1
                    }
            )
            .executes { ctx ->
                val player = ctx.source.player ?: return@executes 0
                val team = BingoTeam.fromPlayer(player) ?: return@executes 0
                storeCoordinates(player, team, null)
                1
            }
            .also { yield(it) }
    }

}