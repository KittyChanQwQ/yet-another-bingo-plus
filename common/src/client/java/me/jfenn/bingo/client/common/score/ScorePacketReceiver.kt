package me.jfenn.bingo.client.common.score

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundCategory

class ScorePacketReceiver(
    clientPacketEvents: ClientPacketEvents,
) {

    init {
        clientPacketEvents.scoredItemV1.onPacket { (packet) ->
            val score = packet.score.items
            MinecraftClient.getInstance().player?.playSound(
                if (packet.isViewerOnTeam) ClientSounds.ITEM_SCORED else ClientSounds.OPPONENT_SCORED,
                SoundCategory.RECORDS,
                0.8f, 0.5f + (score / 25f)
            )
        }

        clientPacketEvents.gameOverV1.onPacket { (packet) ->
            MinecraftClient.getInstance().player?.playSound(
                if (packet.isWinner) ClientSounds.GAME_WON else ClientSounds.GAME_LOST,
                SoundCategory.RECORDS,
                1f, 1f
            )
        }

        clientPacketEvents.gameOverV2.onPacket { (packet) ->
            MinecraftClient.getInstance().player?.playSound(
                if (packet.isWinner) ClientSounds.GAME_WON else ClientSounds.GAME_LOST,
                SoundCategory.RECORDS,
                1f, 1f
            )
        }

        clientPacketEvents.timerV1.onPacket { (packet) ->
            if (packet.secondsRemaining in 1..20 && packet.secondsRemaining % 2 != 0) {
                val volume = 1f - (packet.secondsRemaining / 20f)

                MinecraftClient.getInstance().player?.playSound(
                    ClientSounds.TIMER_TICK,
                    SoundCategory.RECORDS,
                    volume * 0.8f, volume * 0.5f + 0.5f
                )
            }
        }

        clientPacketEvents.countdownV1.onPacket { (packet) ->
            when (packet.secondsRemaining) {
                in 1..3 -> {
                    MinecraftClient.getInstance().player?.playSound(
                        ClientSounds.START_COUNTDOWN,
                        SoundCategory.RECORDS,
                        0.8f, 1f
                    )
                }
                0 -> {
                    MinecraftClient.getInstance().player?.playSound(
                        ClientSounds.START_RELEASE,
                        SoundCategory.RECORDS,
                        0.8f, 1f
                    )
                }
            }
        }
    }
}