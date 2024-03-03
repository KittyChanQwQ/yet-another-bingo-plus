package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.scope.BingoKoin
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.ServerCommandSource
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger

abstract class BaseCommand : KoinComponent {

    private val log by inject<Logger>()

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

            log.debug("Registered [${this::class.simpleName}]")
        }
    }
}