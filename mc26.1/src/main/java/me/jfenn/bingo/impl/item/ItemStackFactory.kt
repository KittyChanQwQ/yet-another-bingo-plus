package me.jfenn.bingo.impl.item

import me.jfenn.bingo.impl.mixin
import me.jfenn.bingo.platform.item.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object ItemStackFactory : IItemStackFactory {

    override val emptyStack: IItemStack = ItemStackImpl(ItemStack.EMPTY)

    override fun listItems(server: MinecraftServer): List<String> {
        return BuiltInRegistries.ITEM.listElements()
            .map { it.key().identifier().toString() }
            .filter { isEnabledInWorld(it, server) }
            .toList()
    }

    override fun isEnabledInWorld(item: String, server: MinecraftServer): Boolean {
        val itemInstance = try {
            createStack(item, 1).item
        } catch (e: Throwable) {
            return false
        }

        val features = listOf(FeatureFlags.TRADE_REBALANCE, FeatureFlags.REDSTONE_EXPERIMENTS, FeatureFlags.MINECART_IMPROVEMENTS)
        return features.all {
            when {
                itemInstance.requiredFeatures().contains(it) -> server.overworld().enabledFeatures().contains(it)
                else -> true
            }
        }
    }

    override fun createStack(item: String, count: Int): IItemStack {
        return createStack(Identifier.parse(item), count)
    }

    override fun createStack(item: Identifier, count: Int): IItemStack {
        val itemInstance = BuiltInRegistries.ITEM.getValue(item)
        if (itemInstance == Items.AIR && !item.path.equals("air", ignoreCase = true))
            throw IllegalArgumentException("[ItemStackFactory] Item $item not found")
        return createStack(itemInstance, count)
    }

    override fun createStack(item: Item, count: Int): IItemStack {
        return forStack(ItemStack(item, count))
    }

    override fun forStack(stack: ItemStack?): IItemStack {
        return stack?.mixin?.`yamb$getItemStackImpl`() ?: emptyStack
    }

    override fun createFilledMap(): IFilledMap = createStack(Items.FILLED_MAP, 1).asFilledMap()!!

    override fun createFireworkRocket(): IFireworkRocket = createStack(Items.FIREWORK_ROCKET, 1).asFireworkRocket()!!

    override fun createWrittenBook(): IWrittenBook = createStack(Items.WRITTEN_BOOK, 1).asWrittenBook()!!

    override fun createPlayerHead(): IPlayerHead = createStack(Items.PLAYER_HEAD, 1).asPlayerHead()!!
}
