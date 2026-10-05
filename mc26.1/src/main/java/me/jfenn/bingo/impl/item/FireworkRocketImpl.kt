package me.jfenn.bingo.impl.item

import me.jfenn.bingo.platform.item.IFireworkRocket
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.Fireworks

class FireworkRocketImpl(
    override val stack: ItemStack,
) : ItemStackImpl(stack), IFireworkRocket {
    override var fireworks: List<Unit>?
        get() = emptyList()
        set(value) {
            stack[DataComponents.FIREWORKS] = value?.let { Fireworks(0, emptyList()) }
        }
}