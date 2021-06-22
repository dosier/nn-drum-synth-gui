package view

import player.AudioPlayer
import dat.DatConverter
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.geometry.Orientation
import javafx.scene.layout.Priority
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
                    vbox {
                        vgrow = Priority.NEVER
                        selectDirectoryButton("MIDI in", Properties.inputMidiDirectory)
                        selectDirectoryButton("DAT out", Properties.outputDatDirectory)
                        button("Convert all to DAT") {
                            maxWidth = Double.MAX_VALUE
                            disableProperty().bind(Properties.inputMidiDirectory.isNull)
                            action {
                                val task = object : Task<Void?>() {

                                    override fun call(): Void? {
                                        val files = ArrayList<File>()
                                        Files.walkFileTree(
                                            Properties.inputMidiDirectory.get().toPath(),
                                            object : FileVisitor<Path> {
                                                override fun preVisitDirectory(
                                                    dir: Path?,
                                                    attrs: BasicFileAttributes?
                                                ) = FileVisitResult.CONTINUE

                                                override fun visitFile(
                                                    file: Path?,
                                                    attrs: BasicFileAttributes?
                                                ): FileVisitResult {
                                                    val f = file!!.toFile()
                                                    if (f.isFile && (f.extension == "midi" || f.extension == "mid"))
                                                        files += f
                                                    return FileVisitResult.CONTINUE
                                                }

                                                override fun visitFileFailed(file: Path?, exc: IOException?) =
                                                    FileVisitResult.CONTINUE

                                                override fun postVisitDirectory(dir: Path?, exc: IOException?) =
                                                    FileVisitResult.CONTINUE

                                            })
                                        taskCompletionProperty.set(0.0)
                                        for ((index, file) in files.withIndex()) {
                                            val converter = MidiConverter(file)
                                            converter.export(MidiConverter.ExportType.DAT)
                                            taskCompletionProperty.set(index.toDouble() / files.size)
                                        }
                                        return null
                                    }
                                }
                                executor.submit(task)
                            }
                        }
                    }
                }
            }
            tab("DAT") {
                splitpane(Orientation.VERTICAL) {
                    fileListView(controller.datFileList, controller.selectedDatFile)
                    selectDirectoryButton("DAT", Properties.inputDatDirectory)
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
            generateButton("Generate WAV", 0, 0) {
                disableProperty().bind(controller.selectedMidiFile.isNull)
                action { controller.generateWAVFile() }
            }
            generateButton("Generate MIDI", 1, 0) {
                disableProperty().bind(controller.selectedDatFile.isNull)
                action { controller.generateMIDIFile() }
            }
            generateButton("Generate DAT", 2, 0) {
                disableProperty().bind(controller.selectedMidiFile.isNull)
                action { controller.generateDATFile() }
            }
            generateButton("Print Summary", 2, 1) {
                disableProperty().bind(controller.selectedDatFile.isNull)
                action { controller.printSummary() }
            }
//            playButton("WAV",0, 1, audioPlayer.file.isNull.or(audioPlayer.disable), audioPlayer.play)
//            playButton("MIDI",0, 2, midiPlayer.file.isNull.or(midiPlayer.disable), midiPlayer.play)
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
        Properties.inputMidiDirectory.onChange { selectDirectory(midiFileList, it!!, "mid", "midi") }
        Properties.inputMidiDirectory.get()?.apply { selectDirectory(midiFileList, this,"mid", "midi") }

        Properties.inputDatDirectory.onChange { selectDirectory(datFileList, it!!, "dat") }
        Properties.inputDatDirectory.get()?.apply { selectDirectory(datFileList, this, "dat") }
    }

    private fun selectDirectory(fileList: ObservableList<File>, directory: File, vararg extensions: String) {
        if (!directory.exists())
            directory.mkdir()
        var midiDirectory1 = directory
        if (!midiDirectory1.isDirectory)
            midiDirectory1 = midiDirectory1.parentFile
        val files = ArrayList<File>()
        Files.walkFileTree(midiDirectory1.toPath(), object : FileVisitor<Path> {
            override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?) = FileVisitResult.CONTINUE
            override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                file?.toFile()?.apply {
                    if (isFile && (extensions.isEmpty() || extensions.contains(extension)))
                        files.add(this)
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path?, exc: IOException?) = FileVisitResult.CONTINUE
            override fun postVisitDirectory(dir: Path?, exc: IOException?) = FileVisitResult.CONTINUE

        })
        fileList.setAll(files)
    }

    fun generateMIDIFile() {
        val progressProperty = SimpleDoubleProperty()
        val task =  object : Task<File>() {
            override fun call(): File {
                return DatConverter(selectedDatFile.get(), progressProperty).exportToMidi()
            }
        }
        taskCompletionProperty.bind(progressProperty)
        task.setOnFailed {
            task.exception.printStackTrace()
        }
        task.setOnSucceeded {
            taskCompletionProperty.unbind()
            taskCompletionProperty.set(0.0)
        }
        executor.submit(task)
    }

    fun printSummary() {
        val summary = DatConverter(selectedDatFile.get()).exportToSummary()
        println("Min interval lengths:")
        for ((instrument, minInterval) in summary.minIntervalLengths) {
            println("\t$instrument = $minInterval")
        }
        println("Max interval lengths:")
        for ((instrument, minInterval) in summary.maxIntervalLengths) {
            println("\t$instrument = $minInterval")
        }
        println("Mean interval lengths:")
        for ((instrument, minInterval) in summary.meanIntervalLengths) {
            println("\t$instrument = $minInterval")
        }
    }

    fun generateDATFile() {
        val progressProperty = SimpleDoubleProperty()
        val task =  object : Task<File>() {
            override fun call(): File {
                return MidiConverter(selectedMidiFile.get(), progressProperty).export(MidiConverter.ExportType.DAT)
            }
        }
        taskCompletionProperty.bind(progressProperty)
        task.setOnFailed {
            task.exception.printStackTrace()
        }
        task.setOnSucceeded {
            taskCompletionProperty.unbind()
            taskCompletionProperty.set(0.0)
        }
        executor.submit(task)
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