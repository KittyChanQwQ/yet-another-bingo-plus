package me.jfenn.bingo.integrations

import me.jfenn.bingo.Bingo
import me.jfenn.bingo.config.ConfigService
import me.jfenn.bingo.utils.formatTitle
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.CommandBossBar
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

object BingoBossBar {

    private val ID = Identifier.of("fennifith", "bingo")

    fun getBossbar(server: MinecraftServer): CommandBossBar {
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

    fun init() {
        ConfigService.onChangePlayerSettings { player ->
            val settings = ConfigService.getPlayer(player.uuidAsString)

            if (settings.bossbar)
                bossBar.addPlayer(player)
            else bossBar.removePlayer(player)
        }

        Bingo.onUpdateTick {
            if (config.showRemainingTime) {
                val timeLimit = config.timeLimit
                val duration = state.ingameDuration()
                if (timeLimit != null && duration != null) {
                    bossBar.value = ((1f - (duration.inWholeSeconds / timeLimit.inWholeSeconds.toFloat())) * bossBar.maxValue).toInt()
                }
            }

            bossBar.name = Text.empty()
                .let {
                    if (config.showRemainingTime) {
                        it.append(Text.literal("Time Remaining: "))
                            .append(state.formatTimeRemaining())
                            .append(Text.literal(" or "))
                    } else it
                }
                .append(
                    Text.literal(config.winCondition.name.formatTitle())
                        .formatted(Formatting.BOLD)
                )
        }
    }

}