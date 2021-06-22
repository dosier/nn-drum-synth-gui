package dat

import Properties
import com.sun.media.sound.StandardMidiFileWriter
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import midi.Instrument
import java.io.DataInputStream
import java.io.File
import javax.sound.midi.MidiEvent
import javax.sound.midi.ShortMessage


class DatConverter(private val datFile: File, private val progressProperty: SimpleDoubleProperty? = null) {

    private lateinit var dat: Dat

    private fun getOrCreateDat() : Dat {
        if (!this::dat.isInitialized)
            dat = createDat()
        return dat
    }

    private fun createDat() : Dat {
        progressProperty?.set(0.0)
        val inputStream = DataInputStream(datFile.inputStream())
        val divisionType = inputStream.readFloat()
        val resolution = inputStream.readInt()
        val events = HashMap<Int, Map<Instrument, Boolean>>()
        val total = inputStream.available()
        while(inputStream.available() > 0) {
            val tick = inputStream.readInt()
            val notesOnOrOffCount = inputStream.readByte()
            val stateChanges = HashMap<Instrument, Boolean>()
            for (i in 0 until notesOnOrOffCount) {
                val instrumentIndex = inputStream.readByte().toInt()
                val instrument = Instrument.values()[instrumentIndex]
                val onOrOffValue = inputStream.readBoolean()
                stateChanges[instrument] = onOrOffValue
            }
            if (events.containsKey(tick)){
                println("SAME TICK??")
            }
            events[tick] = stateChanges
            progressProperty?.set(total.toDouble().div(inputStream.available()))
        }
        return Dat(divisionType, resolution, events.toSortedMap())
    }

    fun exportToSummary() : DatSummary {
        val dat = getOrCreateDat()
        val playingInstruments = HashMap<Instrument, Int>()
        val intervalLengths = HashMap<Instrument, ArrayList<Int>>()

        for ((tick, stateChanges) in dat.events) {
            for ((instrument, playing) in stateChanges) {
                if (playing) {
                    if (playingInstruments.containsKey(instrument)){
                        val playTick = playingInstruments.remove(instrument)!!
                        val lengths = intervalLengths.getOrPut(instrument) { ArrayList() }
                        lengths.add((tick-playTick))
                    }
                    playingInstruments[instrument] = tick
                } else {
                    if (!playingInstruments.containsKey(instrument)){
//                        System.err.println("Received note off event at tick {$tick} for instrument {$instrument} but is not playing?")
                        continue
                    }
                    val playTick = playingInstruments.remove(instrument)!!
                    val lengths = intervalLengths.getOrPut(instrument) { ArrayList() }
                    lengths.add((tick-playTick))
                }
            }
        }
        return DatSummary(
            Instrument.values().associate { it to (intervalLengths[it]?.minOrNull()?:-1) },
            Instrument.values().associate { it to (intervalLengths[it]?.maxOrNull()?:-1) },
            Instrument.values().associate { it to (intervalLengths[it]?.average()?:-1.0) },
        )
    }

    fun exportToMidi() : File {
        progressProperty?.set(0.0)
        val dat = getOrCreateDat()
        val sequence = javax.sound.midi.Sequence(dat.divisionType, dat.resolution)
        val track = sequence.createTrack()
        var i = 0
        for ((tick, stateChanges) in dat.events) {
            for ((instrument, onOrOffValue) in stateChanges) {
                val onOrOffStatus = if (onOrOffValue)  144 else 128 // 144 is on, 128 is off
                val message = ShortMessage(onOrOffStatus, instrument.pitches.first(), 64)
                val event = MidiEvent(message, tick.toLong())
                track.add(event)
            }
            progressProperty?.set(i.toDouble().div(dat.events.size))
            i++
        }
        val output = Properties.outputMidiDirectory.getOrMakeFile(datFile.nameWithoutExtension, "midi")
        val midiFileWriter = StandardMidiFileWriter()
        midiFileWriter.write(sequence, 1, output)
        return output
    }

    private fun ObjectProperty<File>.getOrMakeFile(name: String, extension: String) = value.toPath().resolve("$name.$extension").toFile().apply {
        if (!parentFile.exists())
            parentFile.mkdirs()
    }
}