package me.jfenn.bingo.common.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.formatStringSmall
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object CoordsCommand : KoinComponent {

    private val scoreboardManager by inject<IScoreboardManager>()

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

    fun storeCoordinates(player: ServerPlayerEntity, team: BingoTeam, message: String?) {
        val entry = CoordinatesEntry(
            player = scoreboardManager.getPlayerName(player),
            pos = BlockPosition.fromBlockPos(player.blockPos),
            dimension = player.world.registryKey.value.path.removePrefix("the_"),
            message = message,
            created = Clock.System.now(),
        )

        // add the coordinates message to the stored list
        team.chatCoordinates.add(entry)

        // send coordinates to the team
        ChatCommand.sendTeamMessage(player, team, Text.literal(entry.formatText()))
    }

    fun listCoordinates(player: ServerPlayerEntity, team: BingoTeam) {
        val coordMessages = team.chatCoordinates
        if (coordMessages.isEmpty()) return

        player.sendMessage(Text.literal("Coordinates:").formatted(Formatting.GRAY, Formatting.BOLD))
        for (entry in coordMessages) {
            val duration = Clock.System.now() - entry.created
            player.sendMessage(Text.literal("  • ${entry.player} (${duration.formatStringSmall()} ago): ${entry.formatText()}").formatted(Formatting.GRAY))
        }
    }

    fun init() = registerCommands {
        // send current player coordinates to other team members
        LiteralArgumentBuilder.literal<ServerCommandSource?>("coords")
            .requireState(GameState.PLAYING)
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