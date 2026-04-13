package me.jfenn.bingo.integrations.jei

import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.resources.Identifier

@JeiPlugin
internal class JeiEntrypoint : IModPlugin {

    companion object {
        private val IDENTIFIER = Identifier.fromNamespaceAndPath("yet-another-minecraft-bingo", "bingo")
        var runtime: IJeiRuntime? = null
    }

    override fun getPluginUid(): Identifier = IDENTIFIER

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        runtime = jeiRuntime
    }

    override fun onRuntimeUnavailable() {
        runtime = null
    }
}