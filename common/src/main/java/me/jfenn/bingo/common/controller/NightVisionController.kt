package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

class NightVisionController(
    private val server: MinecraftServer,
    private val config: BingoConfig,
    private val state: BingoState,
    private val options: BingoOptions,
    private val teamService: TeamService,
    private val playerSettingsService: PlayerSettingsService,
    events: ScopedEvents,
) : BingoComponent() {

    private fun updateNightVision(player: ServerPlayerEntity) {
        val isOnTeam = teamService.isPlaying(player)

        // give the player night_vision if configured
        // ...and remove it if not
        if (
            config.nightVisionInPostgame || config.nightVisionInSpectator || options.isNightVision
        ) {
            val shouldHaveNightVision = (
                    (state.state == GameState.PLAYING && !isOnTeam && config.nightVisionInSpectator) ||
                            (state.state == GameState.POSTGAME && config.nightVisionInPostgame) ||
                            (state.state == GameState.PLAYING && options.isNightVision)
                    ) && playerSettingsService.getPlayer(player).nightVision

            if (shouldHaveNightVision) {
                player.addStatusEffect(
                    StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0, false, false),
                )
            } else {
                // if the player has infinite night vision (and should not), remove it
                if (player.getStatusEffect(StatusEffects.NIGHT_VISION)?.duration == -1) {
                    player.removeStatusEffect(StatusEffects.NIGHT_VISION)
                }
            }
        }
    }

    init {
        events.onUpdateTick {
            for (player in server.playerManager.playerList) {
                // Night vision is apparently super buggy and doesn't apply if invoked in `resetPlayerHealth()`...
                // (seems like effects get cleared in a few other places - so we need to make sure it persists)
                updateNightVision(player)
            }
        }
    }

}