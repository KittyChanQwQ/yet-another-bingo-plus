package me.jfenn.bingo.common.event.packet

import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.game.GameOverPacket
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
import me.jfenn.bingo.platform.packet.IServerNetworking

internal class ServerPacketEvents(
    serverNetworking: IServerNetworking,
) {

    val cardResetV1 = serverNetworking.registerS2C(CardResetPacket.V1)
    val cardDisplayV1 = serverNetworking.registerS2C(CardDisplayPacket.V1)
    val cardTilesV1 = serverNetworking.registerS2C(CardTilesPacket.V1)
    val cardImageV1 = serverNetworking.registerS2C(CardImagePacket.V1)
    val cardShuffledV1 = serverNetworking.registerS2C(CardShuffledPacket.V1)
    val cardCompletedV1 = serverNetworking.registerS2C(CardCompletedPacket.V1)
    val gameOverV1 = serverNetworking.registerS2C(GameOverPacket.V1)
    val scoredItemV1 = serverNetworking.registerS2C(ScoredItemPacket.V1)
    val scoredItemLostV1 = serverNetworking.registerS2C(ScoredItemLostPacket.V1)
    val gameStatusV1 = serverNetworking.registerS2C(GameStatusPacket.V1)
    val gameMessageV1 = serverNetworking.registerS2C(GameMessagePacket.V1)
    val gameMessageClearV1 = serverNetworking.registerS2C(GameMessageClearPacket.V1)
    val timerV1 = serverNetworking.registerS2C(TimerPacket.V1)
    val countdownV1 = serverNetworking.registerS2C(CountdownPacket.V1)
    val tooltipV1 = serverNetworking.registerS2C(TooltipPacket.V1)

    val readyUpdateV1 = serverNetworking.registerS2C(ReadyUpdatePacket.V1)
    val readySetV1 = serverNetworking.registerC2S(SetReadyPacket.V1)

    val sendPlayerSettingsV1 = serverNetworking.registerC2S(PlayerSettings.V2)
    val receivePlayerSettingsV1 = serverNetworking.registerS2C(PlayerSettings.V2)

    val statsHashV1C2S = serverNetworking.registerC2S(StatsCheckPacket.V1)
    val statsHashV1S2C = serverNetworking.registerS2C(StatsCheckPacket.V1)
    val statsGameV1C2S = serverNetworking.registerC2S(StatsGamePacket.V1)
    val statsGameV1S2C = serverNetworking.registerS2C(StatsGamePacket.V1)
    val statsIndexV1C2S = serverNetworking.registerC2S(StatsIndexPacket.V1)
    val statsIndexV1S2C = serverNetworking.registerS2C(StatsIndexPacket.V1)

}