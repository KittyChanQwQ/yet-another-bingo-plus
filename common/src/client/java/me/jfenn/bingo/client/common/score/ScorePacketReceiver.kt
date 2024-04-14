package me.jfenn.bingo.client.common.score

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.packet.ClientPacketManager
import me.jfenn.bingo.common.scoring.ScoredItemPacket
import me.jfenn.bingo.common.text.GameOverPacket
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundCategory

class ScorePacketReceiver(
    clientPacketEvents: ClientPacketEvents,
) {

    init {
        clientPacketEvents.onScoredItem { packet ->
            val score = packet.score.items
            MinecraftClient.getInstance().player?.playSound(
                if (packet.isViewerOnTeam) ClientSounds.ITEM_SCORED else ClientSounds.OPPONENT_SCORED,
                SoundCategory.RECORDS,
                0.5f, 0.5f + (score / 25f)
            )
        }

        clientPacketEvents.onGameOver { packet ->
            MinecraftClient.getInstance().player?.playSound(
                if (packet.isWinner) ClientSounds.GAME_WON else ClientSounds.GAME_LOST,
                SoundCategory.RECORDS,
                0.5f, 1f
            )
        }

        clientPacketEvents.onTimer { packet ->
            if (packet.secondsRemaining in 1..20 && packet.secondsRemaining % 2 != 0) {
                val volume = 1f - (packet.secondsRemaining / 20f)

                MinecraftClient.getInstance().player?.playSound(
                    ClientSounds.TIMER_TICK,
                    SoundCategory.RECORDS,
                    volume * 0.4f, volume * 0.5f + 0.5f
                )
            }
        }

        clientPacketEvents.onCountdown { packet ->
            when (packet.secondsRemaining) {
                in 1..3 -> {
                    MinecraftClient.getInstance().player?.playSound(
                        ClientSounds.START_COUNTDOWN,
                        SoundCategory.RECORDS,
                        0.5f, 1f
                    )
                }
                0 -> {
                    MinecraftClient.getInstance().player?.playSound(
                        ClientSounds.START_RELEASE,
                        SoundCategory.RECORDS,
                        0.5f, 1f
                    )
                }
            }
        }
    }
}