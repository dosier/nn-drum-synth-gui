package midi

import com.sun.media.sound.AudioSynthesizer
import javafx.beans.property.DoubleProperty
import java.io.DataOutputStream
import java.io.File
import javax.sound.midi.*
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * Class for converting a MIDI formatted file into a file of [type][ExportType].
 *
 * @param midiFile the input MIDI formatted file.
 * @param progressProperty an optional property to track conversion progress.
 */
class MidiConverter(private val midiFile: File, private val progressProperty: DoubleProperty? = null) {

    private var microsecondsPerQuarterNote = 500_000

    private val messages = ArrayList<Pair<Long, MidiMessage>>()

    /**
     *
     */
    private val features = HashMap<Int, Array<Boolean?>>()

    private var totalTime = 0L
    private var lastTick = 0L

    /**
     * Exports the input [midiFile] as a new [File] of [type].
     */
    fun export(type: ExportType) : File {

        val audioSynthesizer = MidiSystem.getSynthesizer() as AudioSynthesizer
        val sequence = MidiSystem.getSequence(midiFile)

        val fileName = midiFile.nameWithoutExtension
        val trackDuration = sequence.importTrackEvents()

        return when (type) {
            ExportType.WAV -> writeWavFormat(audioSynthesizer, sequence, trackDuration, fileName)
            ExportType.DAT -> writeDatFormat(fileName, sequence)
        }
    }

    private fun Sequence.importTrackEvents() : Double {

        lastTick = 0L
        totalTime = 0L
        messages.clear()
        features.clear()

        val trackEventIndices = IntArray(tracks.size)

        while (true) {
            val (nextEvent, nextTrackIndex) = selectNextTrack(trackEventIndices,)
            if (nextEvent == null || nextTrackIndex == -1)
                break
            trackEventIndices[nextTrackIndex]++

            val tick = nextEvent.tick
            assert(tick <= Int.MAX_VALUE)
            if (divisionType == Sequence.PPQ)
                totalTime += ((tick - lastTick) * microsecondsPerQuarterNote / resolution)
            else
                totalTime = (tick * 1_000_000.0 * divisionType / resolution).toLong()
            lastTick = tick

            val noteOnOrOff = features.getOrPut(tick.toInt()) { arrayOfNulls(instrumentsCount) }
            val message = nextEvent.message
            when (message) {
                is ShortMessage -> parseShortMessage(message, noteOnOrOff)
                is MetaMessage -> microsecondsPerQuarterNote = parseMetaMessage(message, this, microsecondsPerQuarterNote)
            }
            if(message !is MetaMessage)
                messages += totalTime to message
        }
        return (totalTime / 1_000_000.0) + 40L
    }

    private fun Sequence.selectNextTrack(trackEventIndices: IntArray, ): Pair<MidiEvent?, Int> {
        var nextEvent: MidiEvent? = null
        var nextTrackIndex: Int = -1
        for ((trackIndex, track) in tracks.withIndex()) {
            val eventIndex = trackEventIndices[trackIndex]
            if (eventIndex < track.size()) {
                val event = track.get(eventIndex)
                if (nextEvent == null || event.tick < nextEvent.tick) {
                    nextEvent = event
                    nextTrackIndex = trackIndex
                }
            }
        }
        return Pair(nextEvent, nextTrackIndex)
    }

    private fun writeWavFormat(
        audioSynthesizer: AudioSynthesizer,
        sequence: Sequence,
        trackDuration: Double,
        fileName: String
    ): File {
        val sequencer = MidiSystem.getSequencer(false)
        sequencer.transmitter.receiver = audioSynthesizer.receiver

        var audioInputStream = audioSynthesizer.openStream(audioFormat, audioExportOptions)

        assert(sequence.divisionType == Sequence.PPQ)

        val length = (audioFormat.frameRate * trackDuration).toLong()

        for ((currentTime, message) in messages) {
            progressProperty?.set((currentTime.toDouble() / totalTime))
            if (message !is MetaMessage)
                audioSynthesizer.receiver.send(message, currentTime)
        }

        audioInputStream = AudioInputStream(audioInputStream, audioInputStream.format, length)

        val output = File("data/wav/$fileName.wav")
        if (!output.parentFile.exists())
            output.parentFile.mkdir()
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, output)

        audioSynthesizer.close()
        return output
    }

    /**
     * @param name the name of the file to write to
     * @param sequence the [midi sequence][javax.sound.midi.Sequence] to convert
     */
    private fun writeDatFormat(name: String, sequence: Sequence, ) : File {
        val file = File("vector/$name.dat")
        val bos = DataOutputStream(file.outputStream())
        bos.writeFloat(sequence.divisionType)
        bos.writeInt(sequence.resolution)
        for ((tick, notesOnOrOff) in features) {
            val notesOnOrOffCount = notesOnOrOff.count { it != null }
            if (notesOnOrOffCount > 0) {
                bos.writeInt(tick)
                bos.writeByte(notesOnOrOffCount)
                for (instrumentIndex in 0 until instrumentsCount) {
                    val onOrOff = notesOnOrOff[instrumentIndex]
                    if (onOrOff != null) {
                        bos.writeByte(instrumentIndex)
                        bos.writeBoolean(onOrOff)
                    }
                }
            }
        }
        bos.flush()
        bos.close()
        return file
    }

    enum class ExportType {

        /**
         * Represent an audio file format (waveform).
         */
        WAV,

        /**
         * Represents a custom (simplified) midi-like format.
         */
        DAT
    }

    companion object {

        private val audioFormat = AudioFormat(96_000F, 24, 2, true, false)
        private val audioExportOptions = mapOf(
            "interpolation" to "sinc",
            "max polyphony" to "1024")
        private val instrumentsCount = Instrument.values().size
    }
}