package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.VanishIntegration
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.world.GameMode
import org.koin.core.component.inject

class SpectatorCommand : BaseCommand() {

    private val vanish by inject<VanishIntegration>()

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
                requireNotPlaying(),
            )
            .executes { ctx ->
                val player = ctx.source.player ?: return@executes 0
                val gameMode = when (player.interactionManager.gameMode) {
                    GameMode.ADVENTURE -> GameMode.SPECTATOR
                    else -> GameMode.ADVENTURE
                }
                player.changeGameMode(gameMode)
                1
            }
            .register()
    }
}