package me.jfenn.bingo.integrations.voice

import de.maxhenkel.voicechat.api.VoicechatApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent
import me.jfenn.bingo.api.BingoEvents
import me.jfenn.bingo.platform.scope.BingoKoin
import org.slf4j.LoggerFactory

internal object SimpleVoiceEntrypoint : VoicechatPlugin {

    private val logger = LoggerFactory.getLogger("bingo")
    private var integration: SimpleVoiceIntegration? = null
    private var controller: VoiceGroupController? = null

    override fun getPluginId(): String {
        return "yet-another-minecraft-bingo"
    }

    override fun initialize(api: VoicechatApi) {
        super.initialize(api)

        BingoEvents.INIT.register {
            val koin = BingoKoin.getScope(it.server)
                ?: error("Unable to fetch Koin scope from INIT!")

            controller = VoiceGroupController(
                configManager = koin.get(),
                playerManager = koin.get(),
                voiceIntegrationProp = ::integration,
                serverEvents = koin.get()
            )
        }

        BingoEvents.CLOSE.register {
            controller?.close()
            controller = null
        }
    }

    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(VoicechatServerStartedEvent::class.java, this::onServerStarted)
        registration.registerEvent(VoicechatServerStoppedEvent::class.java, this::onServerStopped)
        registration.registerEvent(PlayerConnectedEvent::class.java, this::onPlayerConnected)
    }

    fun onServerStarted(event: VoicechatServerStartedEvent) {
        logger.debug("[SimpleVoiceEntrypoint] Voice server started")
        integration = SimpleVoiceIntegration(event.voicechat)
    }

    fun onServerStopped(event: VoicechatServerStoppedEvent) {
        logger.debug("[SimpleVoiceEntrypoint] Voice server stopped")
        integration = null
    }

    fun onPlayerConnected(event: PlayerConnectedEvent) {
        try {
            val uuid = event.connection.player.uuid
            integration?.onPlayerConnected(uuid)
        } catch (e: Throwable) {
            logger.error("[SimpleVoiceEntrypoint] Error invoking callbacks:", e)
        }
    }
}