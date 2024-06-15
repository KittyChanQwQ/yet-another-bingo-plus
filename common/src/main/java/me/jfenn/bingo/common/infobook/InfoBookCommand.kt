package me.jfenn.bingo.common.infobook

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.common.scope.BingoComponent

class InfoBookCommand(
    commandManager: ICommandManager,
) : BingoComponent() {
    init {
        commandManager.register("bingo") {
            literal("book") {
                executes {
                    val player = player ?: throw IllegalArgumentException("Could not resolve player")
                    val bookService = scope.get<InfoBookService>()
                    val playerManager = scope.get<IPlayerManager>()
                    bookService.giveBookItem(playerManager.forPlayer(player))
                }
            }
        }
    }
}