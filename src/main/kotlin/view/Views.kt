package view

import player.AudioPlayer
import dat.DatToMidiConverter
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.geometry.Orientation
import midi.*
import player.MidiPlayer
import tornadofx.*
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
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

    override val root = splitpane(Orientation.HORIZONTAL) {
        tabpane {
            tab("MIDI") {
                splitpane(Orientation.VERTICAL) {
                    fileListView(controller.midiFileList, controller.selectedMidiFile)
                    selectDirectoryButton("MIDI") {
                        action { controller.chooseDirectory(controller.midiFileList) }
                    }
                }
            }
            tab("DAT") {
                splitpane(Orientation.VERTICAL) {
                    fileListView(controller.datFileList, controller.selectedDatFile)
                    selectDirectoryButton("DAT") {
                        action { controller.chooseDirectory(controller.datFileList) }
                    }
                }
            }
        }
        vbox {
            createTreeView(midiPlayer.sequenceProperty)
        }
        gridpane {
            gridpaneColumnConstraints {
                percentWidth = 25.0
            }
            generateButton("WAV", 0, 0) {
                disableProperty().bind(controller.selectedMidiFile.isNull)
                action { controller.generateWAVFile() }
            }
            generateButton("MIDI", 1, 0) {
                disableProperty().bind(controller.selectedDatFile.isNull)
                action { controller.generateMIDIFile() }
            }
            playButton("WAV",0, 1, audioPlayer.file.isNull.or(audioPlayer.disable), audioPlayer.play)
            playButton("MIDI",0, 2, midiPlayer.file.isNull.or(midiPlayer.disable), midiPlayer.play)
        }
    }
}

class CenterViewController : Controller() {

    internal val midiFileList = FXCollections.observableArrayList<File>()
    internal val selectedMidiFile = SimpleObjectProperty<File>().apply {
        onChange { midiFile ->
            audioPlayer.file.set(null)
            if (midiFile != null) {
                val sequence = openMidiSequence(midiFile)
                midiPlayer.sequenceProperty.set(sequence)
            }
        }
    }
    internal val datFileList = FXCollections.observableArrayList<File>()
    internal val selectedDatFile = SimpleObjectProperty<File>()

    init {
        selectDirectory(midiFileList, Paths.get("data/midi").toFile())
        selectDirectory(datFileList, Paths.get("data/dat").toFile())
    }

    fun chooseDirectory(fileList: ObservableList<File>) {
        val midiDirectory = chooseDirectory() ?:return
        selectDirectory(fileList, midiDirectory)
    }

    private fun selectDirectory(fileList: ObservableList<File>, directory: File) {
        if (!directory.exists())
            directory.mkdir()
        var midiDirectory1 = directory
        if (!midiDirectory1.isDirectory)
            midiDirectory1 = midiDirectory1.parentFile
        val files = ArrayList<File>()
        Files.walkFileTree(midiDirectory1.toPath(), object : FileVisitor<Path> {
            override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?) = FileVisitResult.CONTINUE
            override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                file?.apply { files.add(toFile()) }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path?, exc: IOException?) = FileVisitResult.CONTINUE
            override fun postVisitDirectory(dir: Path?, exc: IOException?) = FileVisitResult.CONTINUE

        })
        fileList.setAll(files)
    }

    fun generateMIDIFile() {
        DatToMidiConverter(selectedDatFile.get()?:return).exportToMidi()
    }

    fun generateWAVFile(){
        val progressProperty = SimpleDoubleProperty()
        val task =  object : Task<File>() {
            override fun call(): File {
                return MidiConverter(selectedMidiFile.get(), progressProperty).export(MidiConverter.ExportType.WAV)
            }
        }
        taskCompletionProperty.bind(progressProperty)
        task.setOnFailed {
            task.exception.printStackTrace()
        }
        task.setOnSucceeded {
            audioPlayer.file.set(task.value)
            taskCompletionProperty.unbind()
            taskCompletionProperty.set(0.0)
        }
        executor.submit(task)
    }
}