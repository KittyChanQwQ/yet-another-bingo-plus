package me.jfenn.bingo.impl.dialog

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.dialog.IConfirmationDialogBuilder
import me.jfenn.bingo.platform.dialog.IDialogHandle
import me.jfenn.bingo.platform.dialog.IDialogManager
import me.jfenn.bingo.platform.dialog.IMultiActionDialogBuilder
import net.minecraft.dialog.type.Dialog
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity

class DialogManager : IDialogManager {
    override fun confirmationBuilder(): IConfirmationDialogBuilder? = ConfirmationDialogBuilder()
    override fun multiActionBuilder(): IMultiActionDialogBuilder? = MultiActionDialogBuilder()

    override fun showDialog(player: IPlayerHandle, dialog: IDialogHandle) {
        require(dialog is DialogHandle)
        val player: ServerPlayerEntity = player.player
        player.openDialog(RegistryEntry.Direct(dialog.dialog))
    }
}

class DialogHandle(
    val dialog: Dialog
) : IDialogHandle
