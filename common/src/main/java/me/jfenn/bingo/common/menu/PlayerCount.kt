package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.stats.StatsService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.toNbt
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

var statsHash: String = ""
var statsFuture: CompletableFuture<Duration?> = CompletableFuture.completedFuture(null)

fun MenuComponent.registerPlayerCount(
    position: Vec3d,
    server: MinecraftServer = koinScope.get(),
    text: TextProvider = koinScope.get(),
    options: BingoOptions = koinScope.get(),
    config: BingoConfig = koinScope.get(),
    stats: StatsService = koinScope.get(),
) {
    onTick {
        val playerCount = server.currentPlayerCount
        val optionsHash = options.getShaHash()
        val hash = "$playerCount:$optionsHash"

        // if the options change, create a new future
        if (statsHash != hash) {
            statsHash = hash
            statsFuture = stats.getBestTimeAsync(
                options = options,
                hostId = config.statsHostId,
                isSingleplayer = server.isSingleplayer && playerCount == 1
            ).whenComplete { _, _ ->
                markDirty()
            }
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
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

        putString("text", text.serialized(
            Text.empty().let {
                if (server.currentPlayerCount > 1)
                    it.append(playerCount).append("   ")
                it.append(bestTime)
            }
        ))
        putInt("line_width", 1000)
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
        put("transformation", Matrix4f().scale(MENU_TEXT_SCALE).toNbt())
    }
}
