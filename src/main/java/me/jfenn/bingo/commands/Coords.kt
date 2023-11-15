package me.jfenn.bingo.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.Bingo
import me.jfenn.bingo.state.GameState
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BlockPosition
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.utils.formatStringSmall
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object Coords {

    @Serializable
    data class CoordinatesEntry(
        val player: String,
        val pos: BlockPosition,
        val message: String?,
        val created: Instant,
    ) {

        fun formatText(): String {
            return with(pos) { "[$x, $y, $z] " } + (message ?: "")
        }

    }

    fun storeCoordinates(context: BingoContext, player: PlayerEntity, team: BingoTeam, message: String?) {
        val entry = CoordinatesEntry(
            player = player.entityName,
            pos = BlockPosition.fromBlockPos(player.blockPos),
            message = message,
            created = Clock.System.now(),
        )

        // add the coordinates message to the stored list
        val list = context.state.teamChatCoordinates.getOrPut(team) { mutableListOf() }
        list.add(entry)

        // send coordinates to the team
        team.broadcastMessage(
            player.server!!,
            Text.empty()
                .append(Text.literal("TEAM ${player.entityName}").formatted(team.formatting))
                .append(Text.literal(": ${entry.formatText()}"))
        )
    }

    fun listCoordinates(context: BingoContext, player: PlayerEntity, team: BingoTeam) {
        val coordMessages = context.state.teamChatCoordinates[team] ?: emptyList()
        if (coordMessages.isEmpty()) return

        player.sendMessage(Text.literal("Coordinates:").formatted(Formatting.GRAY, Formatting.BOLD))
        for (entry in coordMessages) {
            val duration = Clock.System.now() - entry.created
            player.sendMessage(Text.literal("  • ${entry.player} (${duration.formatStringSmall()} ago): ${entry.formatText()}").formatted(Formatting.GRAY))
        }
    }

    fun register(): Sequence<LiteralArgumentBuilder<ServerCommandSource>> = sequence {
        // send current player coordinates to other team members
        LiteralArgumentBuilder.literal<ServerCommandSource?>("coords")
            .requireState(GameState.PLAYING)
            .requireBingoTeam()
            .then(
                LiteralArgumentBuilder.literal<ServerCommandSource?>("list")
                    .executes { ctx ->
                        val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                        val player = ctx.source.player ?: return@executes 0
                        val team = BingoTeam.fromPlayer(player) ?: return@executes 0
                        listCoordinates(context, player, team)
                        1
                    }
            )
            .then(
                RequiredArgumentBuilder.argument<ServerCommandSource?, String?>(
                    "message",
                    StringArgumentType.greedyString()
                )
                    .executes { ctx ->
                        val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                        val player = ctx.source.player ?: return@executes 0
                        val team = BingoTeam.fromPlayer(player) ?: return@executes 0
                        val message = StringArgumentType.getString(ctx, "message")

                        storeCoordinates(context, player, team, message)
                        1
                    }
            )
            .executes { ctx ->
                val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                val player = ctx.source.player ?: return@executes 0
                val team = BingoTeam.fromPlayer(player) ?: return@executes 0
                storeCoordinates(context, player, team, null)
                1
            }
            .also { yield(it) }
    }

}