package me.jfenn.bingo.common.game

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.spawn.ChestService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.Duration.Companion.seconds

class GameService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val server: MinecraftServer,
    private val events: ScopedEvents,
    private val spawnService: SpawnService,
    private val chestService: ChestService,
    private val teamService: TeamService,
    private val configService: ConfigService,
    private val cardService: CardService,
    private val playerManager: IPlayerManager,
    private val permissions: PermissionsIntegration,
    private val text: TextProvider,
) {

    private val notifySpamCooldown = 2.seconds
    private var notifySpamTime = Instant.DISTANT_PAST

    private fun notifyPlayersMissingTeams(playersWithoutATeam: List<IPlayerHandle>) {
        if (!state.isLobbyMode) return

        // Prevent spamming titles (making them unreadable) if someone spams the start button
        val now = Clock.System.now()
        if (now - notifySpamTime < notifySpamCooldown)
            return

        notifySpamTime = now

        val title = text.string(StringKey.CommandStartJoinATeam)
        val message = text.string(StringKey.CommandStartJoinATeamMessage)

        for (player in playersWithoutATeam) {
            if (permissions.hasPermission(player.player, Permission.COMMAND_JOIN)) {
                player.player.networkHandler.sendPacket(TitleS2CPacket(title))
                player.player.networkHandler.sendPacket(SubtitleS2CPacket(message))
            }
        }
    }

    fun start(
        sendFeedback: (Text) -> Unit = {},
        ignoreWarnings: Boolean = false,
        allowSpectators: Boolean = false,
    ): Boolean {
        val playersWithoutATeam = playerManager.getPlayers()
            .filter { teamService.getPlayerTeam(it.player) == null }

        if (playersWithoutATeam.isNotEmpty() && !allowSpectators) {
            // If there's more than one player, teams need to be set up manually!
            if (playerManager.getPlayers().size > 1) {
                sendFeedback(text.string(StringKey.CommandStartPlayersMissingTeams, playersWithoutATeam.joinToString { it.name }))
                sendFeedback(text.string(StringKey.CommandStartPlayersMissingTeamsHint))
                notifyPlayersMissingTeams(playersWithoutATeam)
                return false
            }

            // Otherwise, we can just auto-assign a team
            for (player in playersWithoutATeam) {
                teamService.joinTeam(player.player, BingoTeam.TEAM_BLOCKS.values.random())
            }
        }

        // Ensure that all selected tier lists are supported
        if (!ignoreWarnings && !cardService.isSupported(sendFeedback))
            return false

        // if the game has already been started, ignore
        if (state.state != GameState.PREGAME) {
            sendFeedback(text.string(StringKey.CommandStartAlreadyStarted))
            return false
        }

        if (!options.isValid()) {
            sendFeedback(text.string(StringKey.CommandStartInvalidDistribution))
            return false
        }

        // save the current config, if overwrite option is set
        configService.writeOptions(options)

        for (player in server.playerManager.playerList) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(
                TitleS2CPacket(
                    text.string(StringKey.GameAnnounceBingo).formatted(team?.formatting ?: Formatting.WHITE)
                )
            )
            player.networkHandler.sendPacket(SubtitleS2CPacket(text.string(StringKey.GameAnnounceStarting)))
        }

        if (state.isLobbyMode) {
            // spread player teams to spawnpoint
            spawnService.createSpawnpoints()
            chestService.createChestSpawnpoints()
        }

        // change to the loading state (until all players have loaded terrain)
        state.changeState(events, GameState.PRELOADING)

        broadcastStart()

        return true
    }

    fun end(reason: GameEndReason?) {
        state.endReason = reason
        state.endedAt = Clock.System.now()
        state.changeState(events, GameState.POSTGAME)

        // if lobbyMode=false, immediately change state back to PREGAME once the POSTGAME events are sent
        if (!state.isLobbyMode) {
            state.reset()
            state.changeState(events, GameState.PREGAME)
        }
    }

    private fun broadcastStart() {
        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))

            Text.literal("  ")
                .append(text.string(StringKey.GameStart))
                .formatted(Formatting.GREEN)
                .also { yield(it) }

            Text.literal("  ")
                .append(text.string(StringKey.ScoreboardMode, options.formatGameMode().map { text.string(it) }.joinText()))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.literal("  ")
                .append(text.string(StringKey.ScoreboardFeatures, options.formatFeatures().map { text.string(it) }.joinText()))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            Text.empty()
                .append("  ")
                .append(text.string(StringKey.ScoreboardGoal, options.cardGoal.format(text)))
                .formatted(Formatting.GRAY)
                .also { yield(it) }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            server.playerManager.broadcast(it, false)
        }
    }

}