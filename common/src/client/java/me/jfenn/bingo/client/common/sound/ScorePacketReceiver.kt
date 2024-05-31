package me.jfenn.bingo.client.common.sound

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.common.game.GameOverPacket

class ScorePacketReceiver(
    clientPacketEvents: ClientPacketEvents,
    private val soundService: SoundService,
) {

    private fun onGameOver(packet: GameOverPacket) {
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

        clientPacketEvents.gameOverV1.onPacket { (packet) ->
            onGameOver(packet)
        }

        clientPacketEvents.gameOverV2.onPacket { (packet) ->
            onGameOver(packet)
        }

        clientPacketEvents.gameOverV3.onPacket { (packet) ->
            onGameOver(packet)
        }

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
        }
    }
}