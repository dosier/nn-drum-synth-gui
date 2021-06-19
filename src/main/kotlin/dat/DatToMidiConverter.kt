package dat

import com.sun.media.sound.StandardMidiFileWriter
import javafx.beans.property.SimpleDoubleProperty
import midi.Instrument
import java.io.DataInputStream
import java.io.File
import javax.sound.midi.MidiEvent
import javax.sound.midi.ShortMessage

class DatToMidiConverter(private val datFile: File, private val progressProperty: SimpleDoubleProperty) {

    fun exportToMidi() : File {
        val inputStream = DataInputStream(datFile.inputStream())
        val divisionType = inputStream.readFloat()
        val resolution = inputStream.readInt()

        val sequence = javax.sound.midi.Sequence(divisionType, resolution)
        val track = sequence.createTrack()

        val total = inputStream.available()
        while(inputStream.available() > 0) {
            val tick = inputStream.readInt()
            val notesOnOrOffCount = inputStream.readByte()
            for (i in 0 until notesOnOrOffCount) {
                val instrumentIndex = inputStream.readByte().toInt()
                val instrument = Instrument.values()[instrumentIndex]
                val onOrOffValue = inputStream.readBoolean()
                val onOrOffStatus = if (onOrOffValue)  144 else 128 // 144 is on, 128 is off
                val message = ShortMessage(onOrOffStatus, instrument.pitches.first(), 64)
                val event = MidiEvent(message, tick.toLong())
                track.add(event)
            }
            progressProperty.set(total.toDouble().div(inputStream.available()))
        }
        val output = Properties.outputDatDirectory.get().toPath().resolve("${datFile.nameWithoutExtension}.midi").toFile()
        if (!output.parentFile.exists())
            output.parentFile.mkdirs()
        val midiFileWriter = StandardMidiFileWriter()
        midiFileWriter.write(sequence, 1, output)
        return output
    }
}