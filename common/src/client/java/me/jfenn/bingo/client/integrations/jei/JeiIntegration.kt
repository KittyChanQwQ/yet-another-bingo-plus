package me.jfenn.bingo.client.integrations.jei

import dev.emi.emi.api.EmiApi
import dev.emi.emi.api.stack.EmiStack
import me.shedaniel.rei.api.client.view.ViewSearchBuilder
import me.shedaniel.rei.api.common.util.EntryStacks
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.ingredients.IIngredientType
import mezz.jei.api.recipe.RecipeIngredientRole
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.ItemStack
import org.slf4j.Logger

interface JeiIntegration {
    fun isInstalled(): Boolean
    fun openJeiRecipe(stack: ItemStack): Boolean

    companion object {
        private fun hasJEI() = FabricLoader.getInstance().isModLoaded("jei")

        private fun hasREI() = FabricLoader.getInstance().isModLoaded("roughlyenoughitems")

        private fun hasEMI() = FabricLoader.getInstance().isModLoaded("emi")

        fun create(log: Logger): JeiIntegration {
            return when {
                hasJEI() -> JeiIntegrationImpl(log)
                hasREI() -> ReiIntegrationImpl()
                hasEMI() -> EmiIntegrationImpl()
                else -> JeiIntegrationDummy()
            }
        }
    }
}

class JeiIntegrationDummy : JeiIntegration {
    override fun isInstalled(): Boolean = false
    override fun openJeiRecipe(stack: ItemStack) = false
}

class JeiIntegrationImpl(
    private val log: Logger,
) : JeiIntegration {

    private val plugin = JeiEntrypoint

    override fun isInstalled(): Boolean = true

    override fun openJeiRecipe(stack: ItemStack): Boolean {
        val runtime = plugin.runtime ?: run {
            log.error("JEI Runtime not available!")
            return false
        }

        val focus = runtime.jeiHelpers.focusFactory.createFocus(
            RecipeIngredientRole.OUTPUT,
            @Suppress("UNCHECKED_CAST") (VanillaTypes.ITEM_STACK as IIngredientType<ItemStack>),
            stack,
        )

        runtime.recipesGui.show(focus)
        return true
    }
}

class ReiIntegrationImpl: JeiIntegration {
    override fun isInstalled(): Boolean = true
    override fun openJeiRecipe(stack: ItemStack): Boolean {
        return ViewSearchBuilder.builder()
            .addRecipesFor(EntryStacks.of(stack))
            .open()
    }
}

class EmiIntegrationImpl: JeiIntegration {
    override fun isInstalled(): Boolean = true
    override fun openJeiRecipe(stack: ItemStack): Boolean {
        EmiApi.displayRecipes(EmiStack.of(stack))
        return true
    }
}
