package me.jfenn.bingo.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import me.jfenn.bingo.api.item.*
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.*
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.resource.featuretoggle.FeatureFlags
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.RawFilteredPair
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Unit
import org.slf4j.Logger

class ItemStackFactory(
    private val logger: Logger,
) : IItemStackFactory {
    override fun isEnabledInWorld(item: String, server: MinecraftServer): Boolean {
        val itemInstance = try {
            Registries.ITEM.get(Identifier.of(item))
        } catch (e: Throwable) {
            logger.error("[ItemStackFactory] Item '$item' not found", e)
            return false
        }

        val features = listOf(FeatureFlags.TRADE_REBALANCE, FeatureFlags.BUNDLE)
        return features.all {
            when {
                itemInstance.requiredFeatures.contains(it) -> server.overworld.enabledFeatures.contains(it)
                else -> true
            }
        }
    }

    override fun createStack(item: String, count: Int): IItemStack {
        return createStack(Identifier.of(item), count)
    }

    override fun createStack(item: Identifier, count: Int): IItemStack {
        val itemInstance = Registries.ITEM.get(item)
        if (itemInstance == Items.AIR && !item.path.equals("air", ignoreCase = true))
            throw IllegalArgumentException("[ItemStackFactory] Item $item not found")
        return createStack(itemInstance, count)
    }

    override fun createStack(item: Item, count: Int): IItemStack {
        return forStack(ItemStack(item, count))
    }

    override fun forStack(stack: ItemStack?): IItemStack {
        return ItemStackImpl(stack ?: ItemStack.EMPTY)
    }

    override fun createFilledMap(): IFilledMap = createStack(Items.FILLED_MAP, 1).asFilledMap()!!

    override fun createFireworkRocket(): IFireworkRocket = createStack(Items.FIREWORK_ROCKET, 1).asFireworkRocket()!!

    override fun createWrittenBook(): IWrittenBook = createStack(Items.WRITTEN_BOOK, 1).asWrittenBook()!!

    override fun createPlayerHead(): IPlayerHead = createStack(Items.PLAYER_HEAD, 1).asPlayerHead()!!

    open inner class ItemStackImpl(
        override val stack: ItemStack,
    ) : IItemStack {
        override val item: Item
            get() = stack.item

        override val identifier: Identifier
            get() = Registries.ITEM.getId(item)

        override val displayName: Text
            get() = stack.name

        override var count: Int
            get() = stack.count
            set(value) {
                stack.count = value
            }

        override val maxCount: Int
            get() = stack.maxCount

        override fun addCustomTag(tag: String) {
            val nbt = stack[DataComponentTypes.CUSTOM_DATA]?.copyNbt() ?: NbtCompound()
            nbt.putBoolean(tag, true)
            stack[DataComponentTypes.CUSTOM_DATA] = NbtComponent.of(nbt)
        }

        override fun removeCustomTag(tag: String) {
            val nbt = stack[DataComponentTypes.CUSTOM_DATA]?.copyNbt() ?: NbtCompound()
            nbt.remove(tag)
            stack[DataComponentTypes.CUSTOM_DATA] = NbtComponent.of(nbt)
        }

        override fun hasCustomTag(tag: String): Boolean {
            return stack[DataComponentTypes.CUSTOM_DATA]?.copyNbt()?.contains(tag) == true
        }

        override fun setDisplay(name: Text?, lore: List<Text>?) {
            stack[DataComponentTypes.CUSTOM_NAME] = name
            stack[DataComponentTypes.LORE] = lore?.let { LoreComponent(it) }
        }

        override fun setUnbreakable(value: Boolean) {
            stack[DataComponentTypes.UNBREAKABLE] = UnbreakableComponent(true)
        }

        override fun setHideFlags(hideFlags: Int) {
            stack[DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP] = Unit.INSTANCE
            stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).let {
                stack[DataComponentTypes.ENCHANTMENTS] = it.withShowInTooltip(false)
            }
            stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT).let {
                stack[DataComponentTypes.ATTRIBUTE_MODIFIERS] = it.withShowInTooltip(false)
            }
        }

        private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

        override fun setNbtString(nbt: String?) {
            val compound = nbt?.let { StringNbtReader.parse(it) }
            if (compound != null) {
                stack[DataComponentTypes.CUSTOM_DATA] = NbtComponent.of(compound)
            } else {
                stack.remove(DataComponentTypes.CUSTOM_DATA)
            }
        }

        override fun getNbtString(): String? {
            return stack[DataComponentTypes.CUSTOM_DATA]?.copyNbt()
                ?.takeIf { it.keys.isNotEmpty() }
                ?.toString()
        }

        override fun setComponentsString(components: Map<String, String?>) {
            for ((key, value) in components) {
                val id = Identifier.tryParse(key)
                val type = id?.let { Registries.DATA_COMPONENT_TYPE.get(it) }

                if (id == null || type == null) {
                    logger.error("[ItemStackFactory] Error parsing item component $key for ${item.translationKey}: not a valid identifier")
                    continue
                }

                if (value == null) {
                    stack.remove(type)
                    continue
                }

                val jsonElement = gson.fromJson(value, JsonElement::class.java)
                val component = try {
                    type.codec
                        ?.decode(JsonOps.INSTANCE, jsonElement)
                        ?.getOrThrow()
                        ?.first
                } catch (e: Throwable) {
                    logger.error("[ItemStackFactory] Error parsing item component $key for ${item.translationKey}", e)
                    null
                }

                stack.set(@Suppress("UNCHECKED_CAST") (type as ComponentType<Any>), component)
            }
        }

        override fun getComponentsString(): Map<String, String?>? {
            val components = mutableMapOf<String, String?>()

            for (component in stack.componentChanges.entrySet()) {
                if (component.key.shouldSkipSerialization()) continue
                val id = Registries.DATA_COMPONENT_TYPE.getId(component.key) ?: continue

                val jsonElement = stack.components
                    .find { it.type == component.key }
                    ?.encode(JsonOps.INSTANCE)
                    ?.getOrThrow()
                    ?.let { gson.toJson(it) }

                components[id.toString()] = jsonElement
            }

            return components.takeIf { it.isNotEmpty() }
        }

        override fun copy(): IItemStack {
            return ItemStackImpl(stack.copy())
        }

        override fun asFilledMap(): IFilledMap? {
            return (this as? FilledMapImpl)
                ?: takeIf { item == Items.FILLED_MAP }
                    ?.let { FilledMapImpl(stack) }
        }

        override fun asFireworkRocket(): IFireworkRocket? {
            return (this as? FireworkRocketImpl)
                ?: takeIf { item == Items.FIREWORK_ROCKET }
                    ?.let { FireworkRocketImpl(stack) }
        }

        override fun asWrittenBook(): IWrittenBook? {
            return (this as? WrittenBookImpl)
                ?: takeIf { item == Items.WRITTEN_BOOK }
                    ?.let { WrittenBookImpl(stack) }
        }

        override fun asPlayerHead(): IPlayerHead? {
            return (this as? PlayerHeadImpl)
                ?: takeIf { item == Items.PLAYER_HEAD }
                    ?.let { PlayerHeadImpl(stack) }
        }

    }

    inner class WrittenBookImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IWrittenBook {
        override var title: String?
            get() = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]?.title?.raw
            set(value) {
                val original = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]
                stack[DataComponentTypes.WRITTEN_BOOK_CONTENT] = WrittenBookContentComponent(
                    value?.let { RawFilteredPair.of(value) }
                        ?: RawFilteredPair.of(""),
                    original?.author ?: "",
                    original?.generation ?: 0,
                    original?.pages() ?: emptyList(),
                    original?.resolved ?: true
                )
            }

        override var author: String?
            get() = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]?.author
            set(value) {
                val original = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]
                stack[DataComponentTypes.WRITTEN_BOOK_CONTENT] = WrittenBookContentComponent(
                    original?.title ?: RawFilteredPair.of(""),
                    value ?: "",
                    original?.generation ?: 0,
                    original?.pages() ?: emptyList(),
                    original?.resolved ?: true
                )
            }

        override fun setPages(pages: List<Text>) {
            val original = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]
            stack[DataComponentTypes.WRITTEN_BOOK_CONTENT] = WrittenBookContentComponent(
                original?.title ?: RawFilteredPair.of(""),
                original?.author ?: "",
                original?.generation ?: 0,
                pages.map { RawFilteredPair.of(it) },
                original?.resolved ?: true
            )
        }
    }

    inner class FilledMapImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IFilledMap {
        override var mapId: Int?
            get() = stack[DataComponentTypes.MAP_ID]?.id
            set(value) {
                stack[DataComponentTypes.MAP_ID] = value?.let { MapIdComponent(it) }
            }

        override var mapColor: Int?
            get() = stack[DataComponentTypes.MAP_COLOR]?.rgb
            set(value) {
                stack[DataComponentTypes.MAP_COLOR] = value?.let { MapColorComponent(it) }
            }
    }

    inner class FireworkRocketImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IFireworkRocket {
        override var fireworks: List<kotlin.Unit>?
            get() = emptyList()
            set(value) {
                stack[DataComponentTypes.FIREWORKS] = value?.let { FireworksComponent(0, emptyList()) }
            }
    }

    inner class PlayerHeadImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IPlayerHead {
        override fun setSkullOwner(player: ServerPlayerEntity) {
            stack[DataComponentTypes.PROFILE] = ProfileComponent(player.gameProfile)
        }
    }
}
