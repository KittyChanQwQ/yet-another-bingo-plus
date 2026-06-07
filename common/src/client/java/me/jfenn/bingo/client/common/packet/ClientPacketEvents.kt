package me.jfenn.bingo.client.common.packet

import me.jfenn.bingo.client.platform.IClientNetworking
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.game.GameResetPacket
import me.jfenn.bingo.common.game.GameStatusPacket
import me.jfenn.bingo.common.map.*
import me.jfenn.bingo.common.menu.tooltips.TooltipPacket
import me.jfenn.bingo.common.ready.ReadyUpdatePacket
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.scoring.*
import me.jfenn.bingo.common.stats.packets.StatsCheckPacket
import me.jfenn.bingo.common.stats.packets.StatsGamePacket
import me.jfenn.bingo.common.stats.packets.StatsIndexPacket
import me.jfenn.bingo.common.timer.CountdownPacket
import me.jfenn.bingo.common.timer.TimerPacket

class ClientPacketEvents(
    clientNetworking: IClientNetworking,
) {

    val cardResetV1 = clientNetworking.registerS2C(CardResetPacket.V1)
    val cardDisplayV1 = clientNetworking.registerS2C(CardDisplayPacket.V1)
    val cardTilesV1 = clientNetworking.registerS2C(CardTilesPacket.V1)
    val cardShuffledV1 = clientNetworking.registerS2C(CardShuffledPacket.V1)
    val cardCompletedV1 = clientNetworking.registerS2C(CardCompletedPacket.V1)
    val cardImageV1 = clientNetworking.registerS2C(CardImagePacket.V1)
    val gameOverV1 = clientNetworking.registerS2C(GameOverPacket.V1)
    val scoredItemV1 = clientNetworking.registerS2C(ScoredItemPacket.V1)
    val scoredItemLostV1 = clientNetworking.registerS2C(ScoredItemLostPacket.V1)
    val gameStatusV1 = clientNetworking.registerS2C(GameStatusPacket.V1)
    val gameMessageV1 = clientNetworking.registerS2C(GameMessagePacket.V1)
    val gameMessageClearV1 = clientNetworking.registerS2C(GameMessageClearPacket.V1)
    val timerV1 = clientNetworking.registerS2C(TimerPacket.V1)
    val countdownV1 = clientNetworking.registerS2C(CountdownPacket.V1)
    val tooltipV1 = clientNetworking.registerS2C(TooltipPacket.V1)
    val gameResetV1 = clientNetworking.registerS2C(GameResetPacket.V1)

    val readyUpdateV1 = clientNetworking.registerS2C(ReadyUpdatePacket.V1)
    val readySetV1 = clientNetworking.registerC2S(SetReadyPacket.V1)

    val sendPlayerSettingsV2 = clientNetworking.registerC2S(PlayerSettings.V2)
    val receivePlayerSettingsV2 = clientNetworking.registerS2C(PlayerSettings.V2)

    val statsHashV1C2S = clientNetworking.registerC2S(StatsCheckPacket.V1)
    val statsHashV1S2C = clientNetworking.registerS2C(StatsCheckPacket.V1)
    val statsGameV1C2S = clientNetworking.registerC2S(StatsGamePacket.V1)
    val statsGameV1S2C = clientNetworking.registerS2C(StatsGamePacket.V1)
    val statsIndexV1C2S = clientNetworking.registerC2S(StatsIndexPacket.V1)
    val statsIndexV1S2C = clientNetworking.registerS2C(StatsIndexPacket.V1)

}