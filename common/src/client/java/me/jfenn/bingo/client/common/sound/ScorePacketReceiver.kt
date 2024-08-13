package me.jfenn.bingo.client.common.sound

import me.jfenn.bingo.client.api.ClientPacket
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.common.game.GameOverPacket
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.DownloadingTerrainScreen

class ScorePacketReceiver(
    clientPacketEvents: ClientPacketEvents,
    private val soundService: SoundService,
) {

    private fun onGameOver(clientPacket: ClientPacket<GameOverPacket>) {
        val (packet) = clientPacket
        when {
            packet.isUpdate -> {} // if the packet is an update packet, do nothing
            packet.isWinner -> soundService.play(ClientSounds.Key.GAME_WON)
            else -> soundService.play(ClientSounds.Key.GAME_LOST)
        }
    }

    init {
        clientPacketEvents.scoredItemV1.onPacket { (packet) ->
            val score = packet.score.items
            soundService.play(
                sound = when {
                    packet.isViewerOnTeam -> ClientSounds.Key.ITEM_SCORED
                    else -> ClientSounds.Key.OPPONENT_SCORED
                },
                pitch = 0.5f + (score / 25f),
            )
        }

        clientPacketEvents.gameOverV1.onPacket(::onGameOver)
        clientPacketEvents.gameOverV2.onPacket(::onGameOver)
        clientPacketEvents.gameOverV3.onPacket(::onGameOver)
        clientPacketEvents.gameOverV4.onPacket(::onGameOver)
        clientPacketEvents.gameOverV5.onPacket(::onGameOver)

        clientPacketEvents.timerV1.onPacket { (packet) ->
            if (packet.secondsRemaining in 1..20 && packet.secondsRemaining % 2 != 0) {
                val volume = 1f - (packet.secondsRemaining / 20f)

                soundService.play(
                    sound = ClientSounds.Key.TIMER_TICK,
                    volume = volume * 0.8f,
                    pitch = volume * 0.5f + 0.5f,
                )
            }
        }

        clientPacketEvents.countdownV1.onPacket { (packet) ->
            when (packet.secondsRemaining) {
                in 1..3 -> soundService.play(ClientSounds.Key.START_COUNTDOWN)
                0 -> soundService.play(ClientSounds.Key.START_RELEASE)
            }

            // If the countdown starts while the player is loading terrain, just close the screen!
            // (hack for WaitUntilLoadedController not doing its job)
            val client = MinecraftClient.getInstance()
            // Surely this is safe...
            if (client.currentScreen is DownloadingTerrainScreen) {
                // Nothing could go wrong here, I think.
                client.setScreen(null)
            }
        }
    }
}