package view

import javafx.beans.binding.BooleanBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.control.Button
import javafx.scene.layout.Priority
import player.PlayerStyles
import tornadofx.*
import java.io.File

fun EventTarget.playButton(typeName: String, col: Int, row: Int, disableBinding: BooleanBinding, playBinding: BooleanProperty) {
    label("Control for $typeName") {
        useMaxHeight = true
        gridpaneConstraints {
            columnRowIndex(col, row)
        }
    }
    togglebutton {
        gridpaneConstraints {
            columnRowIndex(col + 1, row)
        }
        disableProperty().bind(disableBinding)
        selectedProperty().bindBidirectional(playBinding)
        addClass(PlayerStyles.playButton)
        isPickOnBounds = true
    }
}

fun EventTarget.generateButton(typeName: String, col: Int, row: Int, init: Button.() -> Unit) {
    button("Generate $typeName file") {
        useMaxWidth = true
        gridpaneConstraints {
            columnRowIndex(col, row)
        }
        init()
    }
}

fun EventTarget.fileListView(fileList: ObservableList<File>, selectedFileProperty: ObjectProperty<File>) {
    listview(fileList) {
        selectedFileProperty.bind(selectionModel.selectedItemProperty())
        cellFormat {
            text = it.nameWithoutExtension
        }
    }
}

fun EventTarget.selectDirectoryButton(typeName: String, directoryProperty: ObjectProperty<File>, init: Button.() -> Unit = {}) {
    button("Select $typeName Directory") {
        maxWidth = Double.MAX_VALUE
        init()
        action {
            directoryProperty.set(chooseDirectory {
                title = "Choose $typeName Directory"
                initialDirectory = directoryProperty.get()
            })
        }
    }
}