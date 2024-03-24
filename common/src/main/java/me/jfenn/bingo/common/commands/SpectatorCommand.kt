package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.VanishIntegration
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.GameMode
import org.koin.core.component.inject

class SpectatorCommand : BaseCommand() {

    private val vanish by inject<VanishIntegration>()

    private fun toggleSpectator(state: BingoState, player: ServerPlayerEntity) {
        val gameMode = if (player.isSpectator) {
            // This needs to match the game mode logic in [PlayerController]
            when (state.state) {
                GameState.PLAYING -> GameMode.ADVENTURE
                GameState.POSTGAME -> GameMode.CREATIVE
                else -> {
                    log.error("[PlayerController] BUG: toggleSpectator was called in an unsupported GameState!")
                    return
                }
            }
        } else {
            GameMode.SPECTATOR
        }

        player.changeGameMode(gameMode)
    }

    init {
        // Switch between adventure and spectator modes
        LiteralArgumentBuilder.literal<ServerCommandSource>("spectator")
            .require(
                when {
                    // If vanish is installed, allow when spectating a game
                    vanish.isInstalled() -> requireState(GameState.PLAYING, GameState.POSTGAME)
                    // Otherwise, only allow in postgame
                    else -> requireState(GameState.POSTGAME)
                },
                requirePermission(Permission.COMMAND_SPECTATOR),
                requireInScope { source ->
                    val player = source.player ?: return@requireInScope false
                    !get<TeamService>().isPlaying(player) ||
                            get<BingoState>().state == GameState.POSTGAME
                },
            )
            .executesInScope { ctx ->
                val player = ctx.source.player ?: return@executesInScope 0
                toggleSpectator(get(), player)
                1
            }
            .register()
    }
}