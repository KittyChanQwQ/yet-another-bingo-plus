package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.BingoPlayerProfile
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.packet.ServerPacketManager
import me.jfenn.bingo.common.state.TeamScore
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Formatting

class ScoreUpdateService(
    private val server: MinecraftServer,
    private val packetManager: ServerPacketManager,
    private val options: BingoOptions,
    private val textProvider: TextProvider,
    private val playerSettingsService: PlayerSettingsService,
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
            sendToEveryone -> server.playerManager.playerList
            else -> team.players.mapNotNull { server.playerManager.getPlayer(it.uuid) }
        }

        for (player in playerList) {
            val isOnTeam = team.includesPlayer(player)
            val playerPacket = if (isOnTeam) packet else otherTeamPacket

            when {
                packetManager.sendPacketS2C(player, ScoredItemPacket.V1, playerPacket) -> {}
                else -> {
                    // play the doink sound
                    if (isOnTeam) {
                        val score = team.score.items
                        player.playSound(
                            SoundEvents.ENTITY_PLAYER_LEVELUP,
                            SoundCategory.RECORDS,
                            1f, 1f + (score / 25f)
                        )
                    } else {
                        player.playSound(
                            SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                            SoundCategory.RECORDS,
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

        val message = textProvider.string(StringKey.GameMessageScoredLine, team.teamOrPlayerName(), textProvider.lineCount(team.score.lines))
            .formatted(Formatting.YELLOW)

        server.playerManager.playerList
            .filter { playerSettingsService.getPlayer(it).scoreMessages }
            .forEach { it.sendMessage(message, false) }
    }

}