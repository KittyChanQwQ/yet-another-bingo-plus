package me.jfenn.bingo.client.common.packet

import me.jfenn.bingo.client.api.IClientNetworking
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.CardDisplayPacket
import me.jfenn.bingo.common.map.CardImagePacket
import me.jfenn.bingo.common.map.CardUpdatePacket
import me.jfenn.bingo.common.menu.tooltips.TooltipPacket
import me.jfenn.bingo.common.ready.ReadyUpdatePacket
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.scoring.CardCompletedPacket
import me.jfenn.bingo.common.scoring.ScoreMessagePacket
import me.jfenn.bingo.common.scoring.ScoredItemLostPacket
import me.jfenn.bingo.common.scoring.ScoredItemPacket
import me.jfenn.bingo.common.stats.packets.StatsCheckPacket
import me.jfenn.bingo.common.stats.packets.StatsGamePacket
import me.jfenn.bingo.common.stats.packets.StatsIndexPacket
import me.jfenn.bingo.common.timer.CountdownPacket
import me.jfenn.bingo.common.timer.TimerPacket

class ClientPacketEvents(
    clientNetworking: IClientNetworking,
) {

    val cardDisplayV1 = clientNetworking.registerS2C(CardDisplayPacket.V1)
    val cardUpdateV1 = clientNetworking.registerS2C(CardUpdatePacket.V1)
    val cardUpdateV2 = clientNetworking.registerS2C(CardUpdatePacket.V2)
    val cardUpdateV3 = clientNetworking.registerS2C(CardUpdatePacket.V3)
    val cardUpdateV4 = clientNetworking.registerS2C(CardUpdatePacket.V4)
    val cardUpdateV5 = clientNetworking.registerS2C(CardUpdatePacket.V5)
    val cardCompletedV1 = clientNetworking.registerS2C(CardCompletedPacket.V1)
    val cardImageV1 = clientNetworking.registerS2C(CardImagePacket.V1)
    val gameOverV1 = clientNetworking.registerS2C(GameOverPacket.V1)
    val gameOverV2 = clientNetworking.registerS2C(GameOverPacket.V2)
    val gameOverV3 = clientNetworking.registerS2C(GameOverPacket.V3)
    val gameOverV4 = clientNetworking.registerS2C(GameOverPacket.V4)
    val gameOverV5 = clientNetworking.registerS2C(GameOverPacket.V5)
    val gameOverV6 = clientNetworking.registerS2C(GameOverPacket.V6)
    val scoredItemV1 = clientNetworking.registerS2C(ScoredItemPacket.V1)
    val scoredItemLostV1 = clientNetworking.registerS2C(ScoredItemLostPacket.V1)
    val scoreMessageV1 = clientNetworking.registerS2C(ScoreMessagePacket.V1)
    val scoreMessageV2 = clientNetworking.registerS2C(ScoreMessagePacket.V2)
    val timerV1 = clientNetworking.registerS2C(TimerPacket.V1)
    val countdownV1 = clientNetworking.registerS2C(CountdownPacket.V1)
    val tooltipV1 = clientNetworking.registerS2C(TooltipPacket.V1)

    val readyUpdateV1 = clientNetworking.registerS2C(ReadyUpdatePacket.V1)
    val readyUpdateV2 = clientNetworking.registerS2C(ReadyUpdatePacket.V2)
    val readySetV1 = clientNetworking.registerC2S(SetReadyPacket.V1)

    val sendPlayerSettingsV1 = clientNetworking.registerC2S(PlayerSettings.V1_C2S)
    val receivePlayerSettingsV1 = clientNetworking.registerS2C(PlayerSettings.V1_S2C)
    val sendPlayerSettingsV2 = clientNetworking.registerC2S(PlayerSettings.V2)
    val receivePlayerSettingsV2 = clientNetworking.registerS2C(PlayerSettings.V2)

    val statsHashV1C2S = clientNetworking.registerC2S(StatsCheckPacket.V1)
    val statsHashV1S2C = clientNetworking.registerS2C(StatsCheckPacket.V1)
    val statsGameV1C2S = clientNetworking.registerC2S(StatsGamePacket.V1)
    val statsGameV1S2C = clientNetworking.registerS2C(StatsGamePacket.V1)
    val statsIndexV1C2S = clientNetworking.registerC2S(StatsIndexPacket.V1)
    val statsIndexV1S2C = clientNetworking.registerS2C(StatsIndexPacket.V1)

}