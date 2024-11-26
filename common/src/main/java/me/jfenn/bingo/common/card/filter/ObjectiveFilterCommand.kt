package me.jfenn.bingo.common.card.filter

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.tag.TagService
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class ObjectiveFilterCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) {
    private fun IExecutionContext.setItemFilters(mutate: (ObjectiveFilterList) -> ObjectiveFilterList) {
        val events = scope.get<ScopedEvents>()
        val cardService = scope.get<CardService>()
        val cardViewService = scope.get<CardViewService>()
        val objectiveFilterService = scope.get<ObjectiveFilterService>()
        val card = cardViewService.getPlayerCard(player!!)

        card.options.itemFilter = mutate(card.options.itemFilter)

        events.onChangeOptions(Unit)
        cardService.generateCard()
        Text.literal("ℹ  ")
            .append(text.string(StringKey.OptionsFilter))
            .append(": ")
            .append(objectiveFilterService.formatFilter(card.options.itemFilter))
            .formatted(Formatting.AQUA)
            .also { sendFeedback(it) }
    }

    private fun IExecutionContext.listTags(): List<String> {
        return scope.get<TagService>().getTags().keys.toList()
    }

    private fun IExecutionContext.listActiveTags(): List<String> {
        return scope.get<CardViewService>()
            .getPlayerCard(player!!)
            .options
            .itemFilter
            .map { ObjectiveFilterList(it).toString() }
    }

    private fun IExecutionContext.listPresets(): List<String> {
        return scope.get<ObjectiveFilterService>().getPresetFilters().keys.toList()
    }

    init {
        commandManager.register("bingo") {
            literal("filter") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }

                literal("preset") {
                    string("preset", { listPresets() }) { presetArg ->
                        executes {
                            val presetName = getArgument(presetArg)
                            val preset = scope.get<ObjectiveFilterService>()
                                .getPresetFilters()[presetName]!!

                            setItemFilters { preset }
                        }
                    }
                }

                literal("only") {
                    string("tag", { listTags() }, greedy = true) { tagArg ->
                        executes {
                            val tag = getArgument(tagArg)
                            require(tag in listTags()) { "Tag '$tag' does not exist!" }
                            setItemFilters {
                                it.withFilter(ObjectiveFilter.Include(tag))
                            }
                        }
                    }
                }

                literal("exclude") {
                    string("tag", { listTags() }, greedy = true) { tagArg ->
                        executes {
                            val tag = getArgument(tagArg)
                            require(tag in listTags()) { "Tag '$tag' does not exist!" }
                            setItemFilters {
                                it.withFilter(ObjectiveFilter.Exclude(tag))
                            }
                        }
                    }
                }

                literal("count") {
                    integer("count", min = 1, max = 25) { countArg ->
                        string("tag", { listTags() }, greedy = true) { tagArg ->
                            executes {
                                val tag = getArgument(tagArg)
                                val count = getArgument(countArg)
                                require(tag in listTags()) { "Tag '$tag' does not exist!" }
                                setItemFilters {
                                    it.withFilter(ObjectiveFilter.Count(tag, count))
                                }
                            }
                        }
                    }
                }

                literal("remove") {
                    string("tag", { listActiveTags() }, greedy = true) { tagArg ->
                        executes {
                            val filterStr = getArgument(tagArg)
                            require(filterStr in listActiveTags()) { "Tag '$filterStr' does not exist!" }
                            val filter = filterStr.let { ObjectiveFilterList.fromString(it) }
                                .firstOrNull()
                                ?: error("Filter '$filterStr' could not be parsed?")

                            setItemFilters {
                                it.withoutFilter(filter.tag)
                            }
                        }
                    }

                    executes {
                        setItemFilters { ObjectiveFilterList.EVERYTHING }
                    }
                }
            }
        }
    }
}