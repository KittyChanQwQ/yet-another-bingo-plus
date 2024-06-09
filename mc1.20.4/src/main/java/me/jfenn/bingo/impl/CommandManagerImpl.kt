package me.jfenn.bingo.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import me.jfenn.bingo.api.commands.*
import me.jfenn.bingo.common.scope.BingoKoin
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.TextArgumentType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.koin.core.scope.Scope
import org.slf4j.Logger

class CommandManagerImpl(
    private val log: Logger,
) : ICommandManager {

    private val root = CommandNode.Root()

    override fun register(configure: CommandBuilder.() -> Unit) {
        CommandBuilder(root).configure()
    }

    private fun constructCommand(
        command: CommandNode,
        registryAccess: CommandRegistryAccess,
    ): ArgumentBuilder<ServerCommandSource, *> {
        var builder: ArgumentBuilder<ServerCommandSource, *> = when (command) {
            is CommandNode.Literal -> LiteralArgumentBuilder.literal(command.name)
            is CommandNode.RequiredArgument<*> -> {
                when (val arg = command.arg) {
                    is CommandArgument.Text -> RequiredArgumentBuilder.argument(
                        arg.name,
                        TextArgumentType.text()
                    )

                    is CommandArgument.Integer -> RequiredArgumentBuilder.argument(
                        arg.name,
                        IntegerArgumentType.integer(arg.min, arg.max)
                    )

                    is CommandArgument.Player -> RequiredArgumentBuilder.argument(
                        arg.name,
                        EntityArgumentType.player()
                    )
                }
            }

            is CommandNode.Root -> throw IllegalArgumentException("Root nodes must not be provided within the tree!")
        }

        for (child in command.children) {
            val childCommand: ArgumentBuilder<ServerCommandSource, *> = constructCommand(child, registryAccess)

            @Suppress("UNCHECKED_CAST")
            builder = builder.then(childCommand) as ArgumentBuilder<ServerCommandSource, *>
        }

        if (command.requires != null) {
            builder.requires { source ->
                try {
                    command.requires?.invoke(ExecutionSourceImpl(source)) ?: true
                } catch (e: Throwable) {
                    log.error("Error in command predicate:", e)
                    false
                }
            }
        }

        if (command.callback != null) {
            builder.executes { ctx ->
                try {
                    command.callback?.invoke(ExecutionContextImpl(ctx))
                    1
                } catch (e: Throwable) {
                    if (e is CommandSyntaxException) throw e
                    log.error("Error in command handler:", e)
                    0
                }
            }
        }

        return builder
    }

    init {
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, _ ->
            for (command in root.children) {
                val builder = constructCommand(command, registryAccess) as? LiteralArgumentBuilder<ServerCommandSource>
                    ?: throw IllegalArgumentException("Commands on the root node must only be literal()!")

                dispatcher.register(builder)
            }
        }
    }
}

open class ExecutionSourceImpl(
    source: ServerCommandSource,
) : IExecutionSource {
    override val server: MinecraftServer = source.server
    override val scope: Scope = BingoKoin.getScope(source.server)
        ?: throw IllegalArgumentException("No scope registered for this server!")
    override val player: ServerPlayerEntity? = source.player
}

class ExecutionContextImpl(
    private val ctx: CommandContext<ServerCommandSource>,
) : ExecutionSourceImpl(ctx.source), IExecutionContext {
    override fun sendMessage(text: Text) {
        ctx.source.sendMessage(text)
    }
    override fun sendFeedback(text: Text) {
        ctx.source.sendFeedback({ text }, true)
    }
    override fun <T> getArgument(arg: CommandArgument<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (arg) {
            is CommandArgument.Text -> TextArgumentType.getTextArgument(ctx, arg.name) as T
            is CommandArgument.Integer -> IntegerArgumentType.getInteger(ctx, arg.name) as T
            is CommandArgument.Player -> EntityArgumentType.getPlayer(ctx, arg.name) as T
        }
    }
}
