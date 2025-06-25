package me.jfenn.bingo.platform.dialog

import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.text.IText

interface IDialogBuilder {
    var title: IText
    fun addText(text: IText, width: Int = 200)
    fun addItem(item: IItemStack)
    fun build(): IDialogHandle
}

interface INoticeDialogBuilder : IDialogBuilder

interface IConfirmationDialogBuilder : IDialogBuilder {
    fun setYes(label: IText, action: IDialogAction)
    fun setNo(label: IText, action: IDialogAction)
}

interface IMultiActionDialogBuilder : IDialogBuilder {
    var columns: Int
    fun addAction(label: IText, action: IDialogAction)
    fun setExitAction(label: IText, action: IDialogAction)
}

sealed interface IDialogAction {
    object None : IDialogAction
    class RunCommand(val command: String) : IDialogAction
}
