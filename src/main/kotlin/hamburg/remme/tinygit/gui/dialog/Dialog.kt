package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.fontSize
import hamburg.remme.tinygit.gui.builder.disabledWhen
import hamburg.remme.tinygit.gui.builder.imageView
import hamburg.remme.tinygit.gui.builder.isCancel
import hamburg.remme.tinygit.gui.builder.isOk
import javafx.beans.value.ObservableBooleanValue
import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback
import javafx.scene.control.Dialog as FXDialog

abstract class Dialog<T>(window: Window, title: String, resizable: Boolean = false) {

    var header: String
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            dialog.dialogPane.headerText = value
        }
    var image: Image
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            dialog.graphic = imageView {
                image = value
                isSmooth = true
                fitWidth = fontSize * 3.0
                fitHeight = fontSize * 3.0
            }
        }
    var graphic: Node
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            dialog.graphic = value
        }
    protected var content: Node
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            dialog.dialogPane.content = value
        }
    protected val dialogWindow get() = dialog.dialogPane.scene.window!!
    protected var okAction: () -> T? = { null }
    protected var cancelAction: () -> T? = { null }
    protected var focusAction: () -> Unit = { }
    private val dialog = FXDialog<T>()
    private val state = TinyGit.get<State>()

    init {
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(window)
        dialog.title = title
        dialog.isResizable = resizable
        dialog.resultConverter = Callback {
            when {
                it == null -> cancelAction()
                it.isOk() -> okAction()
                it.isCancel() -> cancelAction()
                else -> null
            }
        }
        dialogWindow.focusedProperty().addListener { _, _, it -> if (it) focusAction() }
    }

    fun show() {
        state.isModal.set(true)
        dialog.show()
    }

    fun showAndWait(): T? {
        state.isModal.set(true)
        return dialog.showAndWait().orElse(null)
    }

    protected operator fun DialogButton.unaryPlus() {
        dialog.dialogPane.buttonTypes += type
        disabled?.let { dialog.dialogPane.lookupButton(type).disabledWhen(it) }
    }

    protected class DialogButton(val type: ButtonType, val disabled: ObservableBooleanValue? = null) {

        companion object {
            val OK = ButtonType.OK!!
            val CANCEL = ButtonType.CANCEL!!
            val CLOSE = ButtonType.CLOSE!!
            fun ok(text: String) = ButtonType(text, ButtonBar.ButtonData.OK_DONE)
        }

    }

}
