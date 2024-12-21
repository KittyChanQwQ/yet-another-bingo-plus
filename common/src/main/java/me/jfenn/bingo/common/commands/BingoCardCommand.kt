package me.jfenn.bingo.common.commands

import me.jfenn.bingo.platform.commands.CommandArgument
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.commands.IExecutionSource
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.cacheFor
import me.jfenn.bingo.common.utils.joinText
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import kotlin.time.Duration.Companion.seconds

class BingoCardCommand(
    commandManager: ICommandManager,
    private val config: BingoConfig,
    private val text: TextProvider,
) : BingoComponent() {

    companion object {
        private val CARD_KEYS = "bingo".toCharArray()
            .flatMap { col -> (1..5).map { "$col$it" } }

        private val CARD_POSITIONS = buildMap {
            val text = "bingo".toCharArray()
            for (x in 0..4) for (y in 0..4) {
                val row = y+1
                this["${text[x]}$row"] = Pair(x, y)
            }
        }
    }

    private fun IExecutionSource.hasConfigureGame() = hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
    private val IExecutionContext.state get() = scope.get<BingoState>()
    private val IExecutionContext.cardService get() = scope.get<CardService>()
    private val IExecutionContext.cardViewService get() = scope.get<CardViewService>()

    private fun IExecutionContext.suggestObjectives(): Iterable<String> {
        val objectiveManager = scope.get<BingoObjectiveManager>()
        return objectiveManager.list()
    }

    private val objectivesCache = cacheFor(5.seconds) { ctx: IExecutionContext ->
        ctx.suggestObjectives()
    }

    private fun IExecutionContext.setObjective(tile: String, objectiveId: String) {
        val (x, y) = CARD_POSITIONS[tile]
            ?: throw IllegalArgumentException("'$tile' is not a valid bingo tile position: ${CARD_POSITIONS.keys}")

        cardService.replaceEntry(x, y, objectiveId)
    }

    private fun IExecutionContext.setFreeSpace(tile: String) {
        val (x, y) = CARD_POSITIONS[tile]
            ?: throw IllegalArgumentException("'$tile' is not a valid bingo tile position: ${CARD_POSITIONS.keys}")

        cardService.replaceEntry(x, y, BingoObjective.FreeSpace())
    }

    private fun IExecutionContext.getCardSeed() {
        val card = cardViewService.getPlayerCard(player)
        sendInfoMessage(
            Text.empty()
                .append("The current Bingo Card seed is: ")
                .append(Texts.bracketedCopyable(card.seed.toString()))
        )
    }

    private fun IExecutionContext.setCardSeed(seed: Long) {
        val card = cardViewService.getPlayerCard(player)
        val newCard = cardService.generateCard(card, seed = seed)
        sendInfoFeedback(
            Text.empty()
                .append("Changed the Bingo Card seed to: ")
                .append(Texts.bracketedCopyable(newCard.seed.toString()))
        )
    }

    private fun IExecutionContext.setItemDist(itemDistInput: List<Int>) {
        val events = scope.get<ScopedEvents>()
        val card = cardViewService.getPlayerCard(player)
        val itemDist = List(5) { itemDistInput.getOrNull(it) ?: 0 }

        if (itemDist.sum() > 25) {
            sendWarningMessage(Text.literal("Item distribution must add up to 25!"))
            throw IllegalArgumentException("Item distribution must add up to 25!")
        }

        card.options.itemDistribution = itemDist
        events.onChangeOptions(Unit)
        cardService.generateCard(card)
        sendInfoFeedback(Text.literal("Item distribution changed: $itemDist"))
    }

    private fun IExecutionContext.rerollCard() {
        val card = cardViewService.getPlayerCard(player)
        cardService.shuffleCard(card)
        sendInfoFeedback(Text.literal("Card re-rolled."))
    }

    private fun IExecutionContext.getCardTags() = buildMap {
        for ((i, card) in state.cards.withIndex()) {
            this["#${i+1}"] = card
        }
    }

    private fun IExecutionContext.getCardStack() {
        sendMessage(Text.literal("Bingo Cards:").formatted(Formatting.GRAY))

        for ((i, card) in state.cards.withIndex()) {
            val teams = state.getRegisteredTeams()
                .filter { it.cardId == card.id }
                .map { it.getName(text, symbol = false, teamNameKey = null) }

            val nextCardIndex = state.cards.indexOfFirst { it.id == card.nextCardId }
                .takeIf { it != -1 }

            val mode = card.options.formatGameMode()
                .map { text.string(it) }
                .joinText()

            val goal = card.options.goal.format(text)

            val itemDist = card.options.formatItemDist()

            buildList {
                add(Text.literal("  #${i+1}"))
                add(itemDist)
                add(mode)
                add(goal)
                if (teams.isNotEmpty()) {
                    add(teams.joinText(Text.literal(" ")))
                }
                if (nextCardIndex != null && nextCardIndex != i+1) {
                    add(Text.literal(" -> #${nextCardIndex+1}"))
                }
            }
                .joinText(Text.literal(" | ").formatted(Formatting.GRAY))
                .also { sendMessage(it) }
        }
    }

    private fun IExecutionContext.assignTeamCard(teamLabel: String, cardTag: String) {
        val team = state.teams.values
            .find { it.key.label == teamLabel }
            ?: throw IllegalArgumentException("Could not find team '$teamLabel'")

        val card = getCardTags()[cardTag]
            ?: throw IllegalArgumentException("Could not find card '$cardTag'")

        team.cardId = card.id
        sendInfoFeedback(
            Text.empty()
                .append("Team ")
                .append(team.getName(text))
                .append(" assigned to card $cardTag")
        )
    }

    init {
        commandManager.register("bingo") {
            literal("card") {
                requires { hasConfigureGame() }
                literal("set") {
                    string("tile", { CARD_KEYS }) { tileArg ->
                        string("objective", { objectivesCache.get(this) }, greedy = true) { objectiveArg ->
                            executes {
                                val tile = getArgument(tileArg).lowercase()
                                val objectiveId = getArgument(objectiveArg)
                                setObjective(tile, objectiveId)
                            }
                        }
                        literal("free_space") {
                            executes {
                                setFreeSpace(getArgument(tileArg).lowercase())
                            }
                        }
                    }
                }

                literal("seed") {
                    executes { getCardSeed() }

                    long("seed") { seedArg ->
                        executes { setCardSeed(getArgument(seedArg)) }
                    }
                }

                literal("assign") {
                    string("team", { state.teams.keys.map { it.label } }) { teamArg ->
                        string("card", { getCardTags().keys }, greedy = true) { cardArg ->
                            executes {
                                val teamLabel = getArgument(teamArg)
                                val cardTag = getArgument(cardArg)
                                assignTeamCard(teamLabel, cardTag)
                            }
                        }
                    }
                }

                literal("stack") {
                    executes { getCardStack() }

                    literal("push") {
                        requires { hasConfigureGame() && hasState(GameState.PREGAME) }
                        executes {
                            val prevCard = state.getActiveCard()
                            val newCard = cardService.newCard(prevCard.options.copy())
                                .also { state.pushCard(it) }

                            state.teams.values
                                .filter { it.cardId == null || it.cardId == prevCard.id }
                                .forEach { it.cardId = newCard.id }
                        }
                    }

                    literal("pop") {
                        requires { hasConfigureGame() && hasState(GameState.PREGAME) }
                        executes { state.popCard() }
                    }
                }
            }

            literal("difficulty") {
                requires { hasConfigureGame() }

                var builder = this@literal
                val args = mutableListOf<CommandArgument<Int>>()
                for (tier in TierLabel.entries) {
                    builder.integer(tier.name, 0, 25) {
                        args.add(it)
                        builder = this@integer
                    }
                }

                builder.executes {
                    setItemDist(args.map { getArgument(it) })
                }

                for ((name, difficulty) in config.difficultyPresets) {
                    literal(name) {
                        executes { setItemDist(difficulty) }
                    }
                }
            }

            literal("reroll") {
                requires { hasConfigureGame() }
                executes { rerollCard() }
            }
        }
    }

}