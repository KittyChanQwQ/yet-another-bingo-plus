package me.jfenn.bingo.common.event.packet

import me.jfenn.bingo.api.IServerNetworking
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.map.CardDisplayPacket
import me.jfenn.bingo.common.map.CardUpdatePacket
import me.jfenn.bingo.common.scoring.ScoredItemPacket
import me.jfenn.bingo.common.text.GameOverPacket
import me.jfenn.bingo.common.timer.CountdownPacket
import me.jfenn.bingo.common.timer.TimerPacket

class ServerPacketEvents(
    serverNetworking: IServerNetworking,
) {

    val cardDisplayV1 = serverNetworking.registerS2C(CardDisplayPacket.V1)
    val cardUpdateV1 = serverNetworking.registerS2C(CardUpdatePacket.V1)
    val gameOverV1 = serverNetworking.registerS2C(GameOverPacket.V1)
    val gameOverV2 = serverNetworking.registerS2C(GameOverPacket.V2)
    val scoredItemV1 = serverNetworking.registerS2C(ScoredItemPacket.V1)
    val timerV1 = serverNetworking.registerS2C(TimerPacket.V1)
    val countdownV1 = serverNetworking.registerS2C(CountdownPacket.V1)

    val sendPlayerSettingsV1 = serverNetworking.registerC2S(PlayerSettings.V1_C2S)
    val receivePlayerSettingsV1 = serverNetworking.registerS2C(PlayerSettings.V1_S2C)

}