package me.jfenn.bingo.integrations.voice

import de.maxhenkel.voicechat.api.Group
import de.maxhenkel.voicechat.api.VoicechatServerApi
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.platform.IPlayerHandle
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import java.util.*

interface VoiceIntegration {

    fun createGroup(settings: VoiceGroupSettings): IGroupHandle?

    companion object {
        fun isSimpleInstalled() = FabricLoader.getInstance().isModLoaded("voicechat")
        // fun isPlasmoInstalled() = FabricLoader.getInstance().isModLoaded("plasmovoice")

        fun create(
            log: Logger,
            config: BingoConfig,
        ): VoiceIntegration {
            return when {
                !config.integrations.voice.enabled -> {
                    log.info("Integrations: voice.enabled is false in config.json")
                    VoiceIntegrationDummy
                }
                isSimpleInstalled() -> {
                    log.info("Integrations: Simple Voice Chat is installed!")
                    SimpleVoiceIntegration(log)
                }
                // isPlasmoInstalled() -> {
                //     log.info("Integrations: Plasmo Voice is installed!")
                //     PlasmoVoiceIntegration(config.integrations.voice)
                // }
                else -> {
                    log.info("Integrations: No voice mod installed.")
                    VoiceIntegrationDummy
                }
            }
        }
    }
}

@Serializable
data class VoiceGroupSettings(
    val name: String = "Bingo",
    val type: VoiceGroupType = VoiceGroupType.OPEN,
    val password: String? = null,
    val hidden: Boolean = false,
)

@Serializable
enum class VoiceGroupType { OPEN, NORMAL, ISOLATED }

interface IGroupHandle {
    fun addPlayer(player: IPlayerHandle)
    fun removePlayer(player: IPlayerHandle)
    fun close()
}

object VoiceIntegrationDummy : VoiceIntegration {
    override fun createGroup(settings: VoiceGroupSettings) = null
}

class SimpleVoiceIntegration(
    private val log: Logger,
): VoiceIntegration {
    val serverApi: VoicechatServerApi?
        get() {
            return SimpleVoiceEntrypoint.serverApi ?: run {
                log.warn("[SimpleVoiceIntegration] Attempting to use Simple Voice Chat, but the voice server has not started!")
                null
            }
        }

    override fun createGroup(settings: VoiceGroupSettings): IGroupHandle? {
        // If the group is already created, return the existing instance
        serverApi?.groups
            ?.find { it.name == settings.name }
            ?.let { return GroupHandle(it.id) }

        val groupType = when (settings.type) {
            VoiceGroupType.OPEN -> Group.Type.OPEN
            VoiceGroupType.NORMAL -> Group.Type.NORMAL
            VoiceGroupType.ISOLATED -> Group.Type.ISOLATED
        }

        val group = serverApi?.groupBuilder()
            ?.setPersistent(true)
            ?.setName(settings.name)
            ?.setType(groupType)
            ?.setPassword(settings.password)
            ?.setHidden(settings.hidden)
            ?.build()

        return group?.let { GroupHandle(it.id) }
    }

    inner class GroupHandle(
        private val groupId: UUID,
    ): IGroupHandle {
        override fun addPlayer(player: IPlayerHandle) {
            val connection = serverApi?.getConnectionOf(player.uuid) ?: return
            if (connection.isConnected) {
                val group = serverApi?.getGroup(groupId) ?: return
                connection.group = group
            } else {
                SimpleVoiceEntrypoint.onPlayerConnectedCallbacks[player.uuid] = {
                    addPlayer(player)
                }
            }
        }

        override fun removePlayer(player: IPlayerHandle) {
            val connection = serverApi?.getConnectionOf(player.uuid) ?: return
            if (connection.group?.id == groupId) {
                connection.group = null
            }
        }

        override fun close() {
            serverApi?.removeGroup(groupId)
        }
    }
}

