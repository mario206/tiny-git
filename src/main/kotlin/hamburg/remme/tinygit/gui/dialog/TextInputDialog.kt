package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.textArea
import hamburg.remme.tinygit.gui.builder.vbox
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Window

class TextInputDialog(ok: String, textArea: Boolean, window: Window) : Dialog<String>(window, "Input") {

    var defaultValue: String
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            input.text = value
        }
    var description: String
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            label.text = value
        }
    private val label = Label().apply { managedWhen(textProperty().isNotEmpty) }
    private val input = if (textArea) textArea {} else TextField()

    init {
        input.minWidth = 300.0
        Platform.runLater { input.requestFocus() }

        content = vbox {
            spacing = 6.0
            +label
            +input
        }

        +DialogButton(DialogButton.ok(ok))
        +DialogButton(DialogButton.CANCEL)

        okAction = { input.text }
    }

}