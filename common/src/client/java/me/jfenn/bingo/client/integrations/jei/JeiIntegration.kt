package me.jfenn.bingo.client.integrations.jei

import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.ingredients.IIngredientType
import mezz.jei.api.recipe.RecipeIngredientRole
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.ItemStack
import org.slf4j.Logger

interface JeiIntegration {
    fun openJeiRecipe(stack: ItemStack)

    companion object {
        fun hasJEI() = FabricLoader.getInstance().isModLoaded("jei")

        fun create(log: Logger): JeiIntegration {
            return if (hasJEI())
                JeiIntegrationImpl(log)
            else JeiIntegrationDummy()
        }
    }
}

class JeiIntegrationDummy : JeiIntegration {
    override fun openJeiRecipe(stack: ItemStack) {}
}

class JeiIntegrationImpl(
    private val log: Logger,
) : JeiIntegration {

    val plugin = JeiEntrypoint

    override fun openJeiRecipe(stack: ItemStack) {
        val runtime = plugin.runtime ?: run {
            log.error("JEI Runtime not available!")
            return
        }

        val focus = runtime.jeiHelpers.focusFactory.createFocus(
            RecipeIngredientRole.OUTPUT,
            VanillaTypes.ITEM_STACK as IIngredientType<ItemStack>,
            stack,
        )

        runtime.recipesGui.show(focus)
    }
}
