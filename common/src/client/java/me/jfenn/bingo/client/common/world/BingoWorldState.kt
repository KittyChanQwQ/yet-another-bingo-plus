package me.jfenn.bingo.client.common.world

class BingoWorldState {
    var state: ScreenState? = null
    var isApplyingLobbyDataPack: Boolean = false
}

enum class ScreenState {
    ImmediatelyReopenBingoWorld,
    CreateBingoWorld,
    OpenBingoWorld,
    ConfirmExperimentalFeatures,
}
