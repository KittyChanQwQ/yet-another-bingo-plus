package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.OfflinePlayerCache
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

class AdvancementObjectiveManager(
    private val log: Logger,
    private val server: MinecraftServer,
    private val advancementManager: IAdvancementManager,
    private val itemStackFactory: IItemStackFactory,
    private val state: BingoState,
    private val offlinePlayerCache: OfflinePlayerCache,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return advancementManager.listAdvancements(server)
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective.AdvancementEntry? {
        return BingoObjective.AdvancementEntry(id, advancementId = id).takeIf { init(it) }
    }

    private fun init(goal: BingoObjective): Boolean {
        if (goal !is BingoObjective.AdvancementEntry) return false

        val advancement = advancementManager.getAdvancement(server, goal.advancementId)
            ?: return false

        goal.advancement = advancement

        val display = ObjectiveDisplay.Resolved(
            name = advancement.name
                ?.let {
                    Text.empty()
                        .append(it)
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GREEN))
                },
            lore = advancement.description
                ?.string
                ?.split("\n")
                ?.map { Text.literal(it) }
                ?: emptyList(),
            item = advancement.displayIcon?.let { itemStackFactory.forStack(it) },
        )
        goal.display = objectiveDisplayService.resolve(goal.id, null, display)

        return true
    }

    override fun init(card: BingoCard) {
        for (goal in card.objectivesByInstance<BingoObjective.AdvancementEntry>()) {
            if (!init(goal)) {
                log.error("[AdvancementGoalManager] Unable to find advancement '${goal.advancementId}'")
            }
        }
    }

    override fun tick(card: BingoCard) {
        val now = Clock.System.now()

        for (objective in card.objectivesByInstance<BingoObjective.AdvancementEntry>()) {
            val advancement = objective.advancement ?: continue

            objectiveService.updateTeamsOnceAchieved(objective)

            val teamPlayers = state.getRegisteredTeams()
                .associate { it.key to it.players }
                .mapValues { (_, players) ->
                    players.map { offlinePlayerCache.getOfflinePlayer(it) }
                }

            val teamsProgress = teamPlayers.map { (team, players) ->
                val progress = players.maxOf { advancementManager.getProgress(it.player, advancement) }
                team to progress
            }.toMap()

            objectiveService.updateTeamsSeen(objective) { team ->
                teamPlayers[team]
                    ?.any { advancementManager.isAnyObtained(it.player, advancement) }
                    ?: false
            }

            val playersHolding = run {
                val currentPlayers = objective.playersHolding.toMutableMap()

                // Add players that have just achieved the advancement
                teamPlayers.flatMap { (team, players) -> players.map { team to it } } // List<(TeamKey, Player)>
                    // filter players that already have the advancement
                    .filter { (_, player) -> !objective.playersHolding.containsKey(player.uuid) }
                    .filter { (_, player) -> advancementManager.isDone(player.player, advancement) }
                    .onEach { (team, player) ->
                        currentPlayers[player.uuid] = BingoObjectiveCapture(
                            team = team,
                            player = BingoPlayerProfile.fromPlayer(player),
                            instant = now,
                        )
                    }

                // Remove players that no longer have the advancement
                objective.playersHolding.entries
                    .map { offlinePlayerCache.getOfflinePlayer(it.value.player) }
                    .filter { !advancementManager.isDone(it.player, advancement) }
                    .onEach { currentPlayers.remove(it.uuid) }

                currentPlayers
            }

            val players = objectiveService.getPlayers(
                playersHolding = playersHolding,
                players = objective.players
            )

            val teams = objectiveService.getTeams(
                players = players
            )

            objectiveService.update(
                objective = objective,
                teamsProgress = teamsProgress,
                playersHolding = playersHolding,
                players = players,
                teams = teams
            )
        }
    }
}