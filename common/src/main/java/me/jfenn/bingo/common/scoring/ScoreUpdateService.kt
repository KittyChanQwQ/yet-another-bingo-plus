package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.PlayerSoundCategory
import me.jfenn.bingo.api.PlayerSoundEvent
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.BingoPlayerProfile
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.state.TeamScore
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting
import java.util.*

class ScoreUpdateService(
    private val server: MinecraftServer,
    private val packetManager: ServerPacketEvents,
    private val options: BingoOptions,
    private val textProvider: TextProvider,
    private val playerSettingsService: PlayerSettingsService,
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
) {

    fun sendItemCaptured(
        scoredPlayer: ServerPlayerEntity,
        team: BingoTeam,
        entry: BingoCardEntry,
    ) {
        val packet = ScoredItemPacket(
            player = BingoPlayerProfile.fromPlayer(scoredPlayer),
            isViewerOnTeam = true,
            team = team.key,
            teamColor = team.formatting,
            score = team.score.copy(items = team.score.items + 1),
            item = entry.visualItem
                ?.item
                ?.let { Registries.ITEM.getId(it) }
        )

        // unless showOtherTeamCompletions=true, the exact scored item should be hidden from other teams
        val otherTeamPacket = when {
            options.showOtherTeamCompletions -> packet.copy(isViewerOnTeam = false)
            else -> packet.copy(isViewerOnTeam = false, score = TeamScore.ZERO, item = null)
        }

        // decide if all players should get notified when a different team completes an item
        val sendToEveryone = options.showOtherTeamCompletions || options.showCompletedItems
        val playerList = when {
            sendToEveryone -> playerManager.getPlayers()
            else -> team.players.mapNotNull { playerManager.getPlayer(UUID.fromString(it.uuid)) }
        }

        for (player in playerList) {
            val isOnTeam = team.includesPlayer(player.player)
            val playerPacket = if (isOnTeam) packet else otherTeamPacket

            when {
                packetManager.scoredItemV1.send(player.player, playerPacket) -> {}
                else -> {
                    // play the doink sound
                    if (isOnTeam) {
                        val score = team.score.items
                        player.playSound(
                            PlayerSoundEvent.ENTITY_PLAYER_LEVELUP,
                            PlayerSoundCategory.RECORDS,
                            1f, 1f + (score / 25f)
                        )
                    } else {
                        player.playSound(
                            PlayerSoundEvent.BLOCK_NOTE_BLOCK_BASS,
                            PlayerSoundCategory.RECORDS,
                            1f, 1f
                        )
                    }
                }
            }

            // TODO: send a "<player> scored <item>" chat message?
        }
    }

    fun sendLineCaptured(team: BingoTeam) {
        if (!options.showCompletedLines) return

        val teamName = teamService.getTeamOrPlayerName(team)
        val message = textProvider.string(StringKey.GameMessageScoredLine, teamName, textProvider.lineCount(team.score.lines))
            .formatted(Formatting.YELLOW)

        server.playerManager.playerList
            .filter { playerSettingsService.getPlayer(it).scoreMessages }
            .forEach { it.sendMessage(message, false) }
    }

}