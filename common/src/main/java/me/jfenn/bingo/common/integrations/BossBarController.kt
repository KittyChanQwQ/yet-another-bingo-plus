package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.CommandBossBar
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

/**
 * Displays bossbar score/timer information for all players.
 */
class BossBarController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
) {

    private val ID = Identifier.of("fennifith", "bingo")

    fun getBossbar(): CommandBossBar {
        // if the boss bar already exists, recreate it
        server.bossBarManager.get(ID)
            ?.also { server.bossBarManager.remove(it) }

        return server.bossBarManager.add(ID, Text.literal("Time Remaining"))
            .apply {
                color = BossBar.Color.WHITE
                style = BossBar.Style.PROGRESS
                value = 0
                maxValue = 10_000
            }
    }

    private val bossBar by lazy { getBossbar() }

    init {
        events.onChangePlayerSettings { player ->
            val settings = ConfigService.getPlayer(player.uuidAsString)

            if (settings.bossbar)
                bossBar.addPlayer(player)
            else bossBar.removePlayer(player)
        }

        events.onUpdateTick {
            if (options.showRemainingTime) {
                val timeLimit = options.timeLimit
                val duration = state.ingameDuration()
                if (timeLimit != null && duration != null) {
                    bossBar.value = ((1f - (duration.inWholeSeconds / timeLimit.inWholeSeconds.toFloat())) * bossBar.maxValue).toInt()
                }
            }

            bossBar.name = Text.empty()
                .let {
                    if (options.showRemainingTime) {
                        it.append(Text.literal("Time Remaining: "))
                            .append(state.formatTimeRemaining())
                            .append(Text.literal(" or "))
                    } else it
                }
                .append(
                    Text.literal(options.winCondition.name.formatTitle())
                        .formatted(Formatting.BOLD)
                )
        }
    }

}