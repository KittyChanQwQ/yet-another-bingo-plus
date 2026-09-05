package me.jfenn.bingo.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.modules.SerializersModule
import me.jfenn.bingo.platform.IJsonSerializers
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.text.IText
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import kotlin.reflect.KClass

class JsonSerializers(
    private val itemStackFactory: IItemStackFactory,
    server: MinecraftServer?,
) : IJsonSerializers {

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    private val gsonAdapter = gson.getAdapter(JsonElement::class.java)
    private val registryOps = server?.registryAccess()?.createSerializationContext(JsonOps.INSTANCE) ?: JsonOps.INSTANCE

    inner class CodecSerializer<T: Any>(
        private val codec: Codec<T>,
        kClass: KClass<T>,
    ) : KSerializer<T> {
        @OptIn(InternalSerializationApi::class)
        override val descriptor: SerialDescriptor = buildSerialDescriptor("CodecSerializer(${kClass.qualifiedName})", SerialKind.CONTEXTUAL)

        override fun deserialize(decoder: Decoder): T {
            require(decoder is JsonDecoder)
            val str = decoder.json.encodeToString(decoder.decodeJsonElement())
            val gsonElement = gsonAdapter.fromJson(str)
            return codec.parse(registryOps, gsonElement).getOrThrow()
        }

        override fun serialize(encoder: Encoder, value: T) {
            require(encoder is JsonEncoder)
            val gsonElement = codec.encodeStart(registryOps, value).getOrThrow()
            val str = gsonAdapter.toJson(gsonElement)
            encoder.encodeJsonElement(encoder.json.parseToJsonElement(str))
        }
    }

    private val module = SerializersModule {
        contextual(IText::class, CodecSerializer(
            codec = ComponentSerialization.CODEC.xmap(
                { TextImpl(it.copy()) },
                { it.value }
            ),
            kClass = IText::class,
        ))

        contextual(IItemStack::class, CodecSerializer(
            codec = ItemStack.OPTIONAL_CODEC.xmap(
                { itemStackFactory.forStack(it) },
                { it.stack }
            ),
            kClass = IItemStack::class,
        ))
    }

    override val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
        serializersModule = module
    }
}