package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.scoreboard.ScoreChange
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardPlayerUpdateS2CPacket
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.ServerScoreboard
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
            )
        }

        return ObjectiveHandle(objective)
    }

    override fun setScoreboardText(server: MinecraftServer, handle: IObjectiveHandle, textLines: List<String>) {
        require(handle is ObjectiveHandle)

        // Remove any previous sidebar lines that have been changed
        for (score in server.scoreboard.getAllPlayerScores(handle.objective)) {
            if (!textLines.contains(score.playerName)) {
                server.scoreboard.resetPlayerScore(score.playerName, handle.objective)
            }
        }

        textLines.forEachIndexed { i, text ->
            val score = server.scoreboard.getPlayerScore(text, handle.objective)
            score.score = textLines.size - i - 1
        }
    }

    override fun sendObjectiveCreate(player: ServerPlayerEntity, handle: IObjectiveHandle) {
        require(handle is ObjectiveHandle)

        player.networkHandler.sendPacket(
            ScoreboardObjectiveUpdateS2CPacket(
                handle.objective,
                ScoreboardObjectiveUpdateS2CPacket.ADD_MODE,
            )
        )

        player.networkHandler.sendPacket(
            ScoreboardDisplayS2CPacket(1, handle.objective)
        )
    }

    override fun sendObjectiveDelete(player: ServerPlayerEntity, handle: IObjectiveHandle) {
        require(handle is ObjectiveHandle)

        player.networkHandler.sendPacket(
            ScoreboardObjectiveUpdateS2CPacket(
                handle.objective,
                ScoreboardObjectiveUpdateS2CPacket.REMOVE_MODE,
            )
        )
    }

    override fun sendScoreChanges(player: ServerPlayerEntity, handle: IObjectiveHandle, changes: List<ScoreChange>) {
        require(handle is ObjectiveHandle)

        for (change in changes) {
            when (change) {
                is ScoreChange.Create -> {
                    player.networkHandler.sendPacket(ScoreboardPlayerUpdateS2CPacket(ServerScoreboard.UpdateMode.CHANGE, handle.name, change.name, change.value))
                }
                is ScoreChange.Update -> {
                    player.networkHandler.sendPacket(ScoreboardPlayerUpdateS2CPacket(ServerScoreboard.UpdateMode.CHANGE, handle.name, change.name, change.value))
                }
                is ScoreChange.Remove -> {
                    player.networkHandler.sendPacket(ScoreboardPlayerUpdateS2CPacket(ServerScoreboard.UpdateMode.REMOVE, handle.name, change.name, change.value))
                }
            }
        }
    }

    override fun getPlayerName(player: ServerPlayerEntity): String {
        return player.entityName
    }
}

class ObjectiveHandle(
    val objective: ScoreboardObjective,
) : IObjectiveHandle {
    override val name: String
        get() = objective.name
}
