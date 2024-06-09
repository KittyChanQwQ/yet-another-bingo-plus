package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.scoreboard.ScoreChange
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.number.BlankNumberFormat
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.*

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
            }
        }

        textLines.forEachIndexed { i, text ->
            val score = server.scoreboard.getOrCreateScore({ text }, handle.objective, true)
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
            ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, handle.objective)
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

        Text.empty().toString()

        for (change in changes) {
            when (change) {
                is ScoreChange.Create -> {
                    player.networkHandler.sendPacket(ScoreboardScoreUpdateS2CPacket(change.name, handle.name, change.value, Optional.of(change.text), Optional.empty()))
                }
                is ScoreChange.Update -> {
                    player.networkHandler.sendPacket(ScoreboardScoreUpdateS2CPacket(change.name, handle.name, change.value, Optional.of(change.text), Optional.empty()))
                }
                is ScoreChange.Remove -> {
                    player.networkHandler.sendPacket(ScoreboardScoreResetS2CPacket(change.name, handle.name))
                }
            }
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
