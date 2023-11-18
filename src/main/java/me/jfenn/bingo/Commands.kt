package me.jfenn.bingo

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.commands.BingoCommands
import me.jfenn.bingo.commands.ChatCommad
import me.jfenn.bingo.commands.CoordsCommand
import me.jfenn.bingo.commands.RestartCommand
import net.minecraft.server.command.ServerCommandSource

/**
 * Return a sequence of commands related to the game
 */
fun commands() : Sequence<LiteralArgumentBuilder<ServerCommandSource>> = sequence {
    yieldAll(CoordsCommand.commands())
    yieldAll(ChatCommad.commands())
    yieldAll(RestartCommand.commands())
    yieldAll(BingoCommands.commands())
}