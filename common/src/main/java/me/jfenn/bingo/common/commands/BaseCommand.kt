package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.BingoKoin
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.ServerCommandSource
import org.koin.core.Koin
import org.koin.core.component.KoinComponent

abstract class BaseCommand : KoinComponent {

    override fun getKoin(): Koin {
        return BingoKoin.koinApp.koin
    }

    private val commands = mutableListOf<LiteralArgumentBuilder<ServerCommandSource>>()

    fun LiteralArgumentBuilder<ServerCommandSource>.register() {
        commands.add(this@register)
    }

    init {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            for (command in commands)
                dispatcher.register(command)
        }
    }
}