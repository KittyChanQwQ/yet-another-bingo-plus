package me.jfenn.bingo.client.common.packet

import me.jfenn.bingo.common.map.CardDisplayPacket
import me.jfenn.bingo.common.map.CardUpdatePacket
import me.jfenn.bingo.common.scoring.ScoredItemPacket
import me.jfenn.bingo.common.text.GameOverPacket
import me.jfenn.bingo.common.timer.CountdownPacket
import me.jfenn.bingo.common.timer.TimerPacket
import me.jfenn.bingo.common.utils.EventListener

class ClientPacketEvents(
    packetManager: ClientPacketManager,
) {

    val onCardDisplay = EventListener<CardDisplayPacket>()

    val onCardUpdate = EventListener<CardUpdatePacket>()

    val onGameOver = EventListener<GameOverPacket>()

    val onScoredItem = EventListener<ScoredItemPacket>()

    val onTimer = EventListener<TimerPacket>()

    val onCountdown = EventListener<CountdownPacket>()

    init {
        packetManager.registerGlobalReceiver(CardDisplayPacket.V1) { client, handler, packet, response ->
            onCardDisplay(packet)
        }

        packetManager.registerGlobalReceiver(CardUpdatePacket.V1) { client, handler, packet, response ->
            onCardUpdate(packet)
        }

        packetManager.registerGlobalReceiver(GameOverPacket.V1, GameOverPacket.V2) { client, handler, packet, response ->
            onGameOver(packet)
        }

        packetManager.registerGlobalReceiver(ScoredItemPacket.V1) { client, handler, packet, response ->
            onScoredItem(packet)
        }

        packetManager.registerGlobalReceiver(TimerPacket.V1) { client, handler, packet, response ->
            onTimer(packet)
        }

        packetManager.registerGlobalReceiver(CountdownPacket.V1) { client, handler, packet, response ->
            onCountdown(packet)
        }
    }

}