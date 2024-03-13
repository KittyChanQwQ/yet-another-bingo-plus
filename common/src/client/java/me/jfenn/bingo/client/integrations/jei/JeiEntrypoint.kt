package me.jfenn.bingo.client.integrations.jei

import me.jfenn.bingo.common.MOD_ID
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.util.Identifier

@JeiPlugin
object JeiEntrypoint : IModPlugin {

    private val IDENTIFIER = Identifier(MOD_ID, "yet-another-minecraft-bingo")

    var runtime: IJeiRuntime? = null

    override fun getPluginUid(): Identifier = IDENTIFIER

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        this.runtime = jeiRuntime
    }

    override fun onRuntimeUnavailable() {
        runtime = null
    }
}