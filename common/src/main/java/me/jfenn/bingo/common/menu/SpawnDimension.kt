package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IServerWorldFactory
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import org.joml.Vector3d

internal const val MENU_DIMENSION_WIDTH = 2.0

internal fun MenuComponent.registerDimensionMenu(
    position: Vector3d,
    width: Double = MENU_DIMENSION_WIDTH,
    height: Double,
    state: BingoState = koinScope.get(),
    server: MinecraftServer = koinScope.get(),
    serverWorldFactory: IServerWorldFactory = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    val dimensions = serverWorldFactory.listWorlds(server)
        .map { it.identifier }
        .filter { it != LOBBY_WORLD_ID.toString() }
        .sorted()

    var dimension by state.options::spawnDimension
    val indexProp = DelegatedProperty(
        getter = { dimensions.indexOfFirst { it == dimension } },
        setter = { dimension = dimensions[it] },
    )

    registerRadioMenu(
        position = position,
        width = width,
        height = height,
        title = text.string(StringKey.OptionsSpawnDimension),
        options = dimensions.map { Text.literal(it.substringAfterLast(':').formatTitle()) },
        selectedIndexProp = indexProp,
    )
}
