package me.jfenn.bingo.integrations.voice

import de.maxhenkel.voicechat.api.VoicechatApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent
import me.jfenn.bingo.common.MOD_ID_BINGO
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object SimpleVoiceEntrypoint : VoicechatPlugin {

    private val logger = LoggerFactory.getLogger("bingo")
    var serverApi: VoicechatServerApi? = null
    val onPlayerConnectedCallbacks = ConcurrentHashMap<UUID, () -> Unit>()

    override fun getPluginId(): String {
        return MOD_ID_BINGO
    }

    override fun initialize(api: VoicechatApi) {
        super.initialize(api)
    }

    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(VoicechatServerStartedEvent::class.java, this::onServerStarted)
        registration.registerEvent(VoicechatServerStoppedEvent::class.java, this::onServerStopped)
        registration.registerEvent(PlayerConnectedEvent::class.java, this::onPlayerConnected)
    }

    fun onServerStarted(event: VoicechatServerStartedEvent) {
        logger.debug("[SimpleVoiceEntrypoint] Voice server started")
        serverApi = event.voicechat
    }

    fun onServerStopped(event: VoicechatServerStoppedEvent) {
        logger.debug("[SimpleVoiceEntrypoint] Voice server stopped")
        serverApi = null
    }

    fun onPlayerConnected(event: PlayerConnectedEvent) {
        try {
            val uuid = event.connection.player.uuid
            onPlayerConnectedCallbacks.remove(uuid)?.invoke()
        } catch (e: Throwable) {
            logger.error("[SimpleVoiceEntrypoint] Error invoking callbacks:", e)
        }
    }
}