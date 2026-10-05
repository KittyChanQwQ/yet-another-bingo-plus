package me.jfenn.bingo.impl.item

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.item.IPlayerHead
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ResolvableProfile

class PlayerHeadImpl(
    override val stack: ItemStack,
) : ItemStackImpl(stack), IPlayerHead {
    override fun setSkullOwner(player: IPlayerHandle) {
        stack[DataComponents.PROFILE] = ResolvableProfile.createResolved(player.player.gameProfile)
    }
}