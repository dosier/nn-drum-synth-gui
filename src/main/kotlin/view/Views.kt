package view

import ModelType
import Task
import client
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import midi.Instrument
import player.AudioPlayer
import player.MidiPlayer
import tornadofx.*
import java.util.concurrent.Executors

private val executor = Executors.newSingleThreadExecutor()

private val audioPlayer = AudioPlayer()
private val midiPlayer = MidiPlayer()
private val taskCompletionProperty = SimpleDoubleProperty(0.0)

class MainView : View() {

    override val root = borderpane {
        center<CenterView>()
        bottom<BottomView>()
    }
}


class BottomView : View() {

    override val root = vbox {
        isFillWidth = true
        progressbar(taskCompletionProperty) {
            maxWidth = Double.MAX_VALUE
        }
    }
}

class CenterView : View() {

    private val controller: CenterViewController by inject()

    override val root = vbox {
        vbox {
            alignment = Pos.CENTER
            vbox {
                spacing = 5.0
                alignment = Pos.CENTER
                hbox {
                    alignment = Pos.CENTER
                    spacing = 5.0
                    label("BPM")
                    slider(50, 250) {
                        valueProperty().bindBidirectional(controller.bpmProperty)
                    }
                    label(controller.bpmProperty.asString())
                }
                hbox {
                    alignment = Pos.CENTER
                    spacing = 5.0
                    label("Iterative applications")
                    slider(1, 10) {
                        valueProperty().bindBidirectional(controller.repeatProperty)
                    }
                    label(controller.repeatProperty.asString())
                }
                combobox<ModelType> {
                    items.addAll(ModelType.values())
                    selectionModel.select(ModelType.MANY_TO_MANY_4)
                    controller.modelTypeProperty.bind(selectionModel.selectedItemProperty())
                }
            }
            hbox {
                spacing = 5.0
                gridpane {
                    alignment = Pos.CENTER
                    gridpaneColumnConstraints {
                        percentWidth = 25.0
                    }
                    for ((i, instrument) in Instrument.values().withIndex()) {
                        val children = ArrayList<Node>()
                        children += Label(instrument.name)
                        repeat(16) {
                            children += ToggleButton().apply {
                                selectedProperty().onChange { value ->
                                    controller.inStates[it][i] = value
                                }
                            }
                        }
                        addRow(i, *children.toTypedArray())
                    }
                }
                vbox {
                    alignment = Pos.CENTER
                    button("Predict") {
                        action {
                            client.predict(Task(
                                controller.modelTypeProperty.get(),
                                controller.bpmProperty.get(),
                                controller.repeatProperty.get(),
                                controller.inStates)
                             { data, midiFilePath ->
//                                val audioFile = MidiConverter(File(midiFilePath)).export(MidiConverter.ExportType.WAV)
//                                controller.midiPlayer.file.set(audioFile)
                                for ((i, row) in data.withIndex()) {
                                    for ((j, value) in row.withIndex()) {
                                        controller.outStates[i][j].set(value)
                                    }
                                }
                            })
                        }
                    }
                }
                gridpane {
                    alignment = Pos.CENTER
                    gridpaneColumnConstraints {
                        percentWidth = 25.0
                    }
                    for ((i, instrument) in Instrument.values().withIndex()) {
                        val children = ArrayList<Node>()
                        children += Label(instrument.name)
                        repeat(16) {
                            children += ToggleButton().apply {
                                selectedProperty().bind(controller.outStates[it][i])
                            }
                        }
                        addRow(i, *children.toTypedArray())
                    }
                }
            }
//            hbox {
//                alignment = Pos.CENTER
//                button("PLAY") {
//                    disableProperty().bind(controller.midiPlayer.running)
//                    action {
//                        controller.midiPlayer.startThread()
//                    }
//                }
//            }
        }
    }
}

class CenterViewController : Controller() {
    val midiPlayer = MidiPlayer()
    val modelTypeProperty = SimpleObjectProperty(ModelType.MANY_TO_MANY_4)
    val bpmProperty = SimpleIntegerProperty(100)
    val repeatProperty = SimpleIntegerProperty(100)
    val toggleRows = FXCollections.observableArrayList<Instrument>()
    val inStates = Array(16) {
        Array(Instrument.values().size) {
            false
        }
    }
    val outStates = Array(16) {
        Array(Instrument.values().size) {
            SimpleBooleanProperty()
        }
    }
}