package me.jfenn.bingo.config

import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.utils.ContextEventListener
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.reflect.KMutableProperty1

object ConfigService {

    var config: BingoConfig by config("bingo/config.json")
    var playerConfig: PlayerConfig by config("bingo/players.json")

    val onChangePlayerSettings = ContextEventListener<BingoContext, ServerPlayerEntity>()

    init {
        // onChangePlayerSettings should also be invoked if a player logs in
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            val state = Bingo.getContext(player.server) ?: return@register
            onChangePlayerSettings(state, player)
        }
    }

    fun writeConfig(config: BingoConfig) {
        this.config = config
    }

    fun getPlayer(uuid: String): PlayerSettings {
        return playerConfig.players[uuid] ?: PlayerSettings()
    }

    fun setPlayer(uuid: String, settings: PlayerSettings) {
        val players = playerConfig.players.toMutableMap()
        players[uuid] = settings
        playerConfig = playerConfig.copy(players = players)
    }

    fun setPlayerConfig(
        player: ServerPlayerEntity,
        field: KMutableProperty1<PlayerSettings, Boolean>,
        value: Boolean
    ) {
        val config = getPlayer(player.uuidAsString)
        field.set(config, value)
        setPlayer(player.uuidAsString, config)

        Bingo.getContext(player.server)?.let {
            onChangePlayerSettings(it, player)
        }
    }

}