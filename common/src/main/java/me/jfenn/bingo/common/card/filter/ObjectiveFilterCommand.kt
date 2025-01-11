package me.jfenn.bingo.common.card.filter

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.tag.TagService
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.options.optionsContext
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import net.minecraft.text.Text

class ObjectiveFilterCommand(
    commandManager: ICommandManager,
) {
    private fun IExecutionContext.listTags(): List<String> {
        return scope.get<TagService>().getTags().keys.toList()
    }

    private fun IExecutionContext.listPresets(): List<String> {
        return scope.get<ObjectiveFilterService>().getPresetFilters().keys.toList()
    }

    private fun IExecutionContext.suggestFilterList(partial: String): List<String> {
        val parts = partial.split(' ')
        val existingTags = parts
            .dropLast(1)
            .map {
                it.removePrefix("-")
                    .removePrefix("+")
                    .substringBeforeLast('#')
            }
            .toSet()

        val substr = partial.substringBeforeLast(' ', "")
            .takeIf { it.isNotEmpty() }

        return listTags()
            .minus(existingTags)
            .flatMap {
                buildList {
                    add("+$it")
                    add("-$it")
                    for (i in 1..25) {
                        add("$it#$i")
                    }
                }
            }
            .map {
                substr?.plus(" $it") ?: it
            }
    }

    init {
        commandManager.register("bingo") {
            literal("filter") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }

                string("filter", { listPresets() + suggestFilterList(it) }, true) { filterArg ->
                    executes {
                        val filterStr = getArgument(filterArg)
                        val presets = scope.get<ObjectiveFilterService>().getPresetFilters()
                        val list = presets[filterStr] ?: ObjectiveFilterList.fromString(filterStr)

                        val tags = listTags().toSet()
                        for (tag in list) {
                            if (!tags.contains(tag.tag)) {
                                error(Text.literal("Tag '${tag.tag}' does not exist!"))
                            }
                        }

                        scope.get<OptionsService>().setCardFilter(
                            ctx = optionsContext,
                            filter = list,
                            includePresetDetails = true,
                        )
                        scope.get<ScopedEvents>().onChangeOptions(Unit)
                    }
                }
            }
        }
    }
}