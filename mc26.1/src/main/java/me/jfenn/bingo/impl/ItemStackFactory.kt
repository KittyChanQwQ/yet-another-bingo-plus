package me.jfenn.bingo.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.mojang.serialization.JsonOps
import me.jfenn.bingo.impl.inventory.ListInventoryImpl
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.*
import me.jfenn.bingo.platform.text.IText
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.Filterable
import net.minecraft.util.Unit
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.*
import net.minecraft.world.level.saveddata.maps.MapId
import org.slf4j.Logger

class ItemStackFactory(
    private val logger: Logger,
    server: MinecraftServer?
) : IItemStackFactory {

    private val jsonOps = server?.registryAccess()?.createSerializationContext(JsonOps.INSTANCE) ?: JsonOps.INSTANCE
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

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
        return stack?.let(::ItemStackImpl) ?: emptyStack
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
            get() = BuiltInRegistries.ITEM.getKey(item)

        override val displayName: IText
            get() = TextImpl(stack.displayName.copy())

        override val lore: List<IText>
            get() = stack[DataComponents.LORE]?.lines?.map { TextImpl(it.copy()) } ?: emptyList()

        override var count: Int
            get() = stack.count
            set(value) {
                stack.count = value
            }

        override val maxCount: Int
            get() = stack.maxStackSize

        override fun addCustomTag(tag: String) {
            val nbt = stack[DataComponents.CUSTOM_DATA]?.copyTag() ?: CompoundTag()
            nbt.putBoolean(tag, true)
            stack[DataComponents.CUSTOM_DATA] = CustomData.of(nbt)
        }

        override fun removeCustomTag(tag: String) {
            val nbt = stack[DataComponents.CUSTOM_DATA]?.copyTag() ?: CompoundTag()
            nbt.remove(tag)
            if (nbt.isEmpty) {
                stack.remove(DataComponents.CUSTOM_DATA)
            } else {
                stack[DataComponents.CUSTOM_DATA] = CustomData.of(nbt)
            }
        }

        override fun hasCustomTag(tag: String): Boolean {
            return stack[DataComponents.CUSTOM_DATA]?.copyTag()?.contains(tag) == true
        }

        override fun setDisplay(name: IText?, lore: List<IText>?) {
            stack[DataComponents.CUSTOM_NAME] = name?.value
            stack[DataComponents.LORE] = lore?.let { ItemLore(it.map { t -> t.value }) }
        }

        override fun setUnbreakable(value: Boolean) {
            stack[DataComponents.UNBREAKABLE] = Unit.INSTANCE
        }

        override fun setHideFlags(hideFlags: Int) {
            stack[DataComponents.TOOLTIP_DISPLAY] = TooltipDisplay(
                false,
                LinkedHashSet<DataComponentType<*>>().apply {
                    add(DataComponents.ENCHANTMENTS)
                    add(DataComponents.MAP_ID)
                    add(DataComponents.WRITTEN_BOOK_CONTENT)
                    add(DataComponents.ATTRIBUTE_MODIFIERS)
                }
            )
        }

        override fun setNbtString(nbt: String?) = true

        override fun getNbtString(): String? = null

        override fun setComponentsString(components: Map<String, String?>): Boolean {
            var isSuccessful = true

            for ((key, value) in components) {
                val id = Identifier.tryParse(key)
                val type = id?.let { BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(it) }

                if (id == null || type == null) {
                    logger.error("[ItemStackFactory] Error parsing item component $key for ${item.descriptionId}: not a valid identifier")
                    isSuccessful = false
                    continue
                }

                if (value == null) {
                    stack.remove(type)
                    continue
                }

                val jsonElement = gson.fromJson(value, JsonElement::class.java)
                if (jsonElement.isJsonNull) {
                    stack.remove(type)
                    continue
                }

                val component = try {
                    type.codecOrThrow()
                        .decode(jsonOps, jsonElement)
                        .getOrThrow()
                        .first
                } catch (e: Throwable) {
                    logger.error("[ItemStackFactory] Error parsing item component $key for ${item.descriptionId}", e)
                    isSuccessful = false
                    null
                }

                stack.set(@Suppress("UNCHECKED_CAST") (type as DataComponentType<Any>), component)
            }

            return isSuccessful
        }

        override fun getComponentsString(): Map<String, String?>? {
            val components = mutableMapOf<String, String?>()

            for (component in stack.componentsPatch.entrySet()) {
                if (component.key.isTransient) continue
                val id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.key) ?: continue

                val jsonElement = try {
                    stack.components
                        .find { it.type == component.key }
                        ?.encodeValue(jsonOps)
                        ?.getOrThrow()
                        ?.let { gson.toJson(it) }
                } catch (e: Throwable) {
                    logger.warn("Unable to encode component '${component.key}'", e)
                    null
                }

                if (jsonElement != null) {
                    components[id.toString()] = jsonElement
                }
            }

            return components.takeIf { it.isNotEmpty() }
        }

        private fun gsonOverlapsRecursive(predicate: JsonElement, actual: JsonElement): Boolean {
            if (predicate.isJsonNull && actual.isJsonNull) {
                return true
            }
            if (predicate.isJsonPrimitive && actual.isJsonPrimitive) {
                return predicate.asString == actual.asString
            }
            if (predicate.isJsonArray && actual.isJsonArray) {
                val predicateArr = predicate.asJsonArray
                val actualArr = actual.asJsonArray
                return predicateArr.all { predicateItem ->
                    actualArr.any { actualItem -> gsonOverlapsRecursive(predicateItem, actualItem) }
                }
            }
            if (predicate.isJsonObject && actual.isJsonObject) {
                val predicateObj = predicate.asJsonObject
                val actualObj = actual.asJsonObject
                return predicateObj.keySet().all {
                    gsonOverlapsRecursive(predicateObj.get(it) ?: JsonNull.INSTANCE, actualObj.get(it) ?: JsonNull.INSTANCE)
                }
            }
            return false
        }

        override fun isDataOverlapping(nbt: String?, components: Map<String, String?>?): Boolean {
            if (components == null) return true

            val itemComponents = getComponentsString()
                ?.mapValues { (_, value) -> value?.let { gson.fromJson(it, JsonElement::class.java) } }
                ?: return components.isEmpty()

            val predicateComponents = try {
                components.mapValues { (_, value) ->
                    value?.let { gson.fromJson(it, JsonElement::class.java) }
                }
            } catch (e: Throwable) {
                logger.error("[ItemStackFactory] Error parsing item components for ${item.descriptionId} predicate", e)
                return false
            }

            return predicateComponents.all { (key, predicateComponent) ->
                val itemComponent = itemComponents[key]?.takeIf { !it.isJsonNull }
                when {
                    predicateComponent == null -> itemComponent != null
                    itemComponent == null -> false
                    else -> gsonOverlapsRecursive(predicateComponent, itemComponent)
                }
            }
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

        override val bundleInventory: IInventory?
            get() = stack[DataComponents.BUNDLE_CONTENTS]
                ?.items()
                ?.map { forStack(it.create()) }
                .orEmpty()
                .let { ListInventoryImpl(it) }

        override fun setBundleInventory(inventory: Collection<IItemStack>) {
            stack[DataComponents.BUNDLE_CONTENTS] = BundleContents(
                inventory.filter { !it.isEmpty }
                    .map { ItemStackTemplate.fromNonEmptyStack(it.stack) }
            )
        }

        override val containerInventory: IInventory?
            get() = stack[DataComponents.CONTAINER]
                ?.allItemsCopyStream()
                ?.map { forStack(it) }
                ?.toList()
                .orEmpty()
                .let { ListInventoryImpl(it) }

        override fun setContainerInventory(inventory: Collection<IItemStack>) {
            stack[DataComponents.CONTAINER] = ItemContainerContents.fromItems(
                inventory.map { it.stack }
            )
        }
    }

    inner class WrittenBookImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IWrittenBook {
        override var title: String?
            get() = stack[DataComponents.WRITTEN_BOOK_CONTENT]?.title?.raw
            set(value) {
                val original = stack[DataComponents.WRITTEN_BOOK_CONTENT]
                stack[DataComponents.WRITTEN_BOOK_CONTENT] = WrittenBookContent(
                    value?.let { Filterable.passThrough(value) }
                        ?: Filterable.passThrough(""),
                    original?.author ?: "",
                    original?.generation ?: 0,
                    original?.pages() ?: emptyList(),
                    original?.resolved ?: true
                )
            }

        override var author: String?
            get() = stack[DataComponents.WRITTEN_BOOK_CONTENT]?.author
            set(value) {
                val original = stack[DataComponents.WRITTEN_BOOK_CONTENT]
                stack[DataComponents.WRITTEN_BOOK_CONTENT] = WrittenBookContent(
                    original?.title ?: Filterable.passThrough(""),
                    value ?: "",
                    original?.generation ?: 0,
                    original?.pages() ?: emptyList(),
                    original?.resolved ?: true
                )
            }

        override fun setPages(pages: List<IText>) {
            val original = stack[DataComponents.WRITTEN_BOOK_CONTENT]
            stack[DataComponents.WRITTEN_BOOK_CONTENT] = WrittenBookContent(
                original?.title ?: Filterable.passThrough(""),
                original?.author ?: "",
                original?.generation ?: 0,
                pages.map { Filterable.passThrough(it.value) },
                original?.resolved ?: true
            )
        }
    }

    inner class FilledMapImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IFilledMap {
        override var mapId: Int?
            get() = stack[DataComponents.MAP_ID]?.id
            set(value) {
                stack[DataComponents.MAP_ID] = value?.let { MapId(it) }
            }

        override var mapColor: Int?
            get() = stack[DataComponents.MAP_COLOR]?.rgb
            set(value) {
                stack[DataComponents.MAP_COLOR] = value?.let { MapItemColor(it) }
            }
    }

    inner class FireworkRocketImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IFireworkRocket {
        override var fireworks: List<kotlin.Unit>?
            get() = emptyList()
            set(value) {
                stack[DataComponents.FIREWORKS] = value?.let { Fireworks(0, emptyList()) }
            }
    }

    inner class PlayerHeadImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IPlayerHead {
        override fun setSkullOwner(player: IPlayerHandle) {
            stack[DataComponents.PROFILE] = ResolvableProfile.createResolved(player.player.gameProfile)
        }
    }
}
