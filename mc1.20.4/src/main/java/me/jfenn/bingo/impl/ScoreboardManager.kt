package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.api.IObjectiveHandle
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.number.BlankNumberFormat
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

class ScoreboardManager : IScoreboardManager {

    override fun createDummyObjective(server: MinecraftServer, name: String, displayName: Text): IObjectiveHandle {
        val objective = server.scoreboard.getNullableObjective(name) ?: run {
            server.scoreboard.addObjective(
                name,
                ScoreboardCriterion.DUMMY,
                displayName,
                ScoreboardCriterion.RenderType.INTEGER,
                false,
                BlankNumberFormat.INSTANCE,
            )
        }

        return ObjectiveHandle(objective)
    }

    override fun setScoreboardText(server: MinecraftServer, handle: IObjectiveHandle, textLines: List<String>) {
        require(handle is ObjectiveHandle)

        // Remove any previous sidebar lines that have been changed
        for (score in server.scoreboard.getScoreboardEntries(handle.objective)) {
            if (!textLines.contains(score.owner)) {
                server.scoreboard.removeScore({ score.owner }, handle.objective)

                if (!server.isSingleplayer) {
                    // The server sometimes fails to send this packet, so we're adding it manually. I don't know why.
                    // - yes, this does need to be sent to all players, otherwise it may cause desync
                    for (player in server.playerManager.playerList)
                        player.networkHandler.sendPacket(ScoreboardScoreResetS2CPacket(score.owner, null))
                }
            }
        }

        textLines.forEachIndexed { i, text ->
            val score = server.scoreboard.getOrCreateScore({ text }, handle.objective, true)
            score.score = textLines.size - i - 1
        }
    }

    override fun sendScoreboardDisplay(player: ServerPlayerEntity, handle: IObjectiveHandle?) {
        require(handle is ObjectiveHandle?)
        player.networkHandler.sendPacket(ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, handle?.objective))

        // If the server is in singleplayer, the display packet is not handled
        if (player.server.isSingleplayer) {
            player.server.scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, handle?.objective)
        }
    }

    override fun sendScoreboardUpdates(player: ServerPlayerEntity, handle: IObjectiveHandle) {
        require(handle is ObjectiveHandle)
        val packets = player.server.scoreboard.createChangePackets(handle.objective)
        for (packet in packets) {
            player.networkHandler.sendPacket(packet)
        }
    }

    override fun getPlayerName(player: ServerPlayerEntity): String {
        return player.nameForScoreboard
    }
}

class ObjectiveHandle(
    val objective: ScoreboardObjective,
) : IObjectiveHandle {
    override val name: String
        get() = objective.name
}
