package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Instant
import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoPlayerProfile
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

class AdvancementObjectiveManager(
    private val log: Logger,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val advancementManager: IAdvancementManager,
    private val itemStackFactory: IItemStackFactory,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        val dataKeys = objectiveRegistryState.objectives
            .filterValues { it is ObjectiveData.Advancement }
            .keys
        return advancementManager.listAdvancements(server) + dataKeys
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective.AdvancementEntry? {
        // If the objective is a data ID
        (objectiveRegistryState.objectives[id] as? ObjectiveData.Advancement)
            ?.let { data ->
                BingoObjective.AdvancementEntry(
                    id = id,
                    data = data,
                    advancementId = data.advancement,
                )
            }
            ?.takeIf { init(it, isDataObjective = true) }
            ?.let { return it }

        // If the objective is a direct advancement identifier
        return BingoObjective.AdvancementEntry(id, advancementId = id).takeIf { init(it) }
    }

    private fun init(goal: BingoObjective, isDataObjective: Boolean = false): Boolean {
        if (goal !is BingoObjective.AdvancementEntry) return false

        val advancement = advancementManager.getAdvancement(server, goal.advancementId)

        if (advancement == null) {
            if (isDataObjective) {
                log.error("[AdvancementObjectiveManager] ${goal.id}: Advancement ${goal.advancementId} could not be found.")
            }
            return false
        }

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
            decoration = CardTile.Decoration.ADVANCEMENT,
        )
        goal.display = objectiveDisplayService.resolve(goal.id, null, display)

        return true
    }

    override fun init(card: BingoCard) {
        for (goal in card.objectivesByInstance<BingoObjective.AdvancementEntry>()) {
            if (!init(goal)) {
                log.error("[AdvancementObjectiveManager] Unable to find advancement '${goal.advancementId}'")
            }
        }
    }

    override fun tick(card: BingoCard) {
        val now = state.updatedAt ?: Instant.DISTANT_PAST

        for (objective in card.objectivesByInstance<BingoObjective.AdvancementEntry>()) {
            val advancement = objective.advancement ?: continue

            objectiveService.updateTeamsOnceAchieved(objective)

            val teamPlayers = objectiveService.getPlayersByTeam()
                .mapKeys { it.key.key }

            val teamsProgress = teamPlayers.map { (team, players) ->
                val progress = players.maxOf { advancementManager.getProgress(it.player, advancement) }
                team to progress
            }.toMap()

            objectiveService.updateTeamsSeen(objective) { team ->
                teamPlayers[team]
                    ?.any { advancementManager.isAnyObtained(it.player, advancement) }
                    ?: false
            }

            val playersHolding = teamPlayers.asSequence()
                .flatMap { (team, players) -> players.map { team to it } } // List<(TeamKey, Player)>
                .filter { (_, player) -> advancementManager.isDone(player.player, advancement) }
                .associate { (team, player) ->
                    val capture = objective.playersHolding[player.uuid] ?: BingoObjectiveCapture(
                        team = team,
                        player = BingoPlayerProfile.fromPlayer(player),
                        instant = now,
                    )
                    player.uuid to capture
                }

            val players = objectiveService.getPlayers(
                playersHolding = playersHolding,
                players = objective.players,
                shouldRetainCaptures = false
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