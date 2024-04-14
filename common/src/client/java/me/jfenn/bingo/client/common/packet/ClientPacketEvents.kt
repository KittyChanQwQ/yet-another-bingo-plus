package me.jfenn.bingo.client.common.packet

import me.jfenn.bingo.client.api.IClientNetworking
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.map.CardDisplayPacket
import me.jfenn.bingo.common.map.CardUpdatePacket
import me.jfenn.bingo.common.scoring.ScoredItemPacket
import me.jfenn.bingo.common.text.GameOverPacket
import me.jfenn.bingo.common.timer.CountdownPacket
import me.jfenn.bingo.common.timer.TimerPacket

class ClientPacketEvents(
    clientNetworking: IClientNetworking,
) {

    val cardDisplayV1 = clientNetworking.registerS2C(CardDisplayPacket.V1)
    val cardUpdateV1 = clientNetworking.registerS2C(CardUpdatePacket.V1)
    val gameOverV1 = clientNetworking.registerS2C(GameOverPacket.V1)
    val gameOverV2 = clientNetworking.registerS2C(GameOverPacket.V2)
    val scoredItemV1 = clientNetworking.registerS2C(ScoredItemPacket.V1)
    val timerV1 = clientNetworking.registerS2C(TimerPacket.V1)
    val countdownV1 = clientNetworking.registerS2C(CountdownPacket.V1)

    val sendPlayerSettingsV1 = clientNetworking.registerC2S(PlayerSettings.V1_C2S)
    val receivePlayerSettingsV1 = clientNetworking.registerS2C(PlayerSettings.V1_S2C)

}