package me.jfenn.bingo.impl.dialog

import me.jfenn.bingo.impl.TextImpl
import me.jfenn.bingo.platform.dialog.*
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.text.IText
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.*
import net.minecraft.server.dialog.action.Action
import net.minecraft.server.dialog.action.CommandTemplate
import net.minecraft.server.dialog.action.ParsedTemplate
import net.minecraft.server.dialog.action.StaticAction
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.body.ItemBody
import net.minecraft.server.dialog.body.PlainMessage
import net.minecraft.server.dialog.input.BooleanInput
import net.minecraft.world.item.ItemStackTemplate
import java.util.*

abstract class DialogBuilder : IDialogBuilder {
    override var title: IText = TextImpl(Component.empty())
    private val body = mutableListOf<DialogBody>()
    private val inputs = mutableListOf<Input>()

    override fun addText(text: IText, width: Int) {
        body.add(PlainMessage(text.value, width))
    }

    override fun addItem(item: IItemStack) {
        body.add(ItemBody(ItemStackTemplate.fromNonEmptyStack(item.stack), Optional.empty(), true, true, 1, 1))
    }

    override fun addInput(input: IDialogInput) {
        when (input) {
            is IDialogInput.Boolean -> {
                inputs.add(
                    Input(
                        input.key,
                        BooleanInput(input.label.value, false, "true", "false")
                    )
                )
            }
        }
    }

    internal fun buildCommon(): CommonDialogData {
        return CommonDialogData(
            title.value,
            Optional.empty(),
            true,
            false,
            DialogAction.CLOSE,
            body,
            inputs,
        )
    }
}

class NoticeDialogBuilder : DialogBuilder(), INoticeDialogBuilder {
    private var exitAction: ActionButton? = null

    override fun setAction(label: IText, action: IDialogAction) {
        exitAction = ActionButton(
            CommonButtonData(
                label.value,
                CommonButtonData.DEFAULT_WIDTH,
            ),
            action.toDialogAction()
        )
    }

    override fun build(): IDialogHandle {
        val dialog = NoticeDialog(
            buildCommon(),
            exitAction ?: NoticeDialog.DEFAULT_ACTION,
        )
        return DialogHandle(dialog)
    }
}

class ConfirmationDialogBuilder : DialogBuilder(), IConfirmationDialogBuilder {
    private var yes: ActionButton? = null
    private var no: ActionButton? = null

    override fun setYes(label: IText, action: IDialogAction) {
        yes = ActionButton(
            CommonButtonData(
                label.value,
                CommonButtonData.DEFAULT_WIDTH
            ),
            action.toDialogAction()
        )
    }

    override fun setNo(label: IText, action: IDialogAction) {
        no = ActionButton(
            CommonButtonData(
                label.value,
                CommonButtonData.DEFAULT_WIDTH
            ),
            action.toDialogAction()
        )
    }

    override fun build(): IDialogHandle {
        val dialog = ConfirmationDialog(
            buildCommon(),
            requireNotNull(yes),
            requireNotNull(no)
        )
        return DialogHandle(dialog)
    }
}

class MultiActionDialogBuilder : DialogBuilder(), IMultiActionDialogBuilder {
    override var columns: Int = 2
    private val actions = mutableListOf<ActionButton>()
    private var exitAction: ActionButton? = null

    override fun addAction(label: IText, action: IDialogAction) {
        actions.add(
            ActionButton(
                CommonButtonData(
                    label.value,
                    CommonButtonData.DEFAULT_WIDTH,
                ),
                action.toDialogAction()
            )
        )
    }

    override fun setExitAction(label: IText, action: IDialogAction) {
        exitAction = ActionButton(
            CommonButtonData(
                label.value,
                CommonButtonData.DEFAULT_WIDTH,
            ),
            action.toDialogAction()
        )
    }

    override fun build(): IDialogHandle {
        if (actions.isEmpty()) {
            throw IllegalArgumentException("Dialog actions must not be empty")
        }
        val dialog = MultiActionDialog(
            buildCommon(),
            actions,
            Optional.ofNullable(exitAction),
            columns,
        )
        return DialogHandle(dialog)
    }
}

private fun IDialogAction.toDialogAction(): Optional<Action> = when (this) {
    is IDialogAction.None -> Optional.empty()
    is IDialogAction.RunCommand -> {
        Optional.of(
            StaticAction(ClickEvent.RunCommand(command))
        )
    }
    is IDialogAction.DynamicRunCommand -> {
        Optional.of(
            CommandTemplate(
                ParsedTemplate.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(command))
                    .getOrThrow { IllegalArgumentException(it) }
            )
        )
    }
}
