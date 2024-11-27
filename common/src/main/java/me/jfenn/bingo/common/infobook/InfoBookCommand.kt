package me.jfenn.bingo.common.infobook

import me.jfenn.bingo.platform.commands.ICommandManager
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
                    bookService.giveBookItem(player)
                }
            }
        }
    }
}