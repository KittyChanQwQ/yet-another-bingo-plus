package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.stats.StatsService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Matrix4f
import org.joml.Vector3d
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

private var statsHash: String = ""
private var statsFuture: CompletableFuture<Duration?> = CompletableFuture.completedFuture(null)

fun MenuComponent.registerPlayerCount(
    position: Vector3d,
    server: MinecraftServer = koinScope.get(),
    text: TextProvider = koinScope.get(),
    state: BingoState = koinScope.get(),
    options: BingoOptions = koinScope.get(),
    stats: StatsService = koinScope.get(),
) {
    onTick {
        val playerCount = server.currentPlayerCount
        val optionsHash = options.getShaHash()
        val hash = "${state.gameId}:$playerCount:$optionsHash"

        // if the options change, create a new future
        if (statsHash != hash) {
            statsHash = hash
            statsFuture = stats.getBestTimeAsync(
                options = options,
                isSingleplayer = server.isSingleplayer && server.currentPlayerCount <= 1
            ).whenComplete { _, _ ->
                markDirty()
            }
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position

        val playerCount = text.string(
            StringKey.OptionsPlayerCount,
            Text.literal("${server.currentPlayerCount} / ${server.maxPlayerCount}")
                .formatted(Formatting.GREEN)
        )

        val bestTime = text.string(
            StringKey.StatsBestTime,
            statsFuture.getNow(null)
                ?.formatString()
                ?.let { Text.literal(it).formatted(Formatting.GREEN) }
                ?: text.string(StringKey.StatsBestTimeNotSet).formatted(Formatting.GRAY)
        )

        this.value = Text.empty().let {
            if (server.currentPlayerCount > 1)
                it.append(playerCount).append("   ")
            it.append(bestTime)
        }

        lineWidth = 1000
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}
