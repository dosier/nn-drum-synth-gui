package midi

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.concurrent.Task
import midi.MessageType.*
import java.io.File
import javax.sound.midi.MetaMessage
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequence
import javax.sound.midi.ShortMessage

fun openMidiSequence(midiFile: File, action: (Sequence) -> Unit = {}) : Sequence {
    val sequence = MidiSystem.getSequence(midiFile)
    action.invoke(sequence)
    return sequence
}

fun parseShortMessage(message: ShortMessage, noteOnOrOff: Array<Boolean?>) {
    when (message.status) {
        // Channel Note Off
        in 128..143 -> noteOnOrOff[message.getInstrument().ordinal] = false
        // Channel Note On
        in 144..159 -> noteOnOrOff[message.getInstrument().ordinal] = true
    }
}

fun parseMetaMessage(
    message: MetaMessage,
    sequence: Sequence,
    microsecondsPerQuarterNote: Int
): Int {
    var microsecondsPerQuarterNote1 = microsecondsPerQuarterNote
    val messageType = message.getMessageType()
    when (sequence.divisionType) {
        Sequence.PPQ -> {
            when (messageType) {
                SEQUENCE_NUMBER -> {
                    val sequenceNumber = message.read2ByteInt()
                    println("SEQUENCE_NUMBER = $sequenceNumber")
                }
                CHANNEL_PREFIX -> {
                    val channelPrefix = message.read1ByteInt()
                    println("CHANNEL_PREFIX = $channelPrefix")
                }
                SET_TEMPO -> {
                    microsecondsPerQuarterNote1 = message.read3ByteInt()
                    println("SET_TEMPO = $microsecondsPerQuarterNote1")
                }
                KEY_SIGNATURE -> {
                    val keySignature = message.read2ByteInt()
                    println("KEY_SIGNATURE = $keySignature")
                }
                else -> println("ignored message of type {$messageType}")
            }
        }
    }
    return microsecondsPerQuarterNote1
}


fun MetaMessage.read1ByteInt() =
    (data[0].toInt() and 0xff)
fun MetaMessage.read2ByteInt() =
    (data[0].toInt() and 0xff shl 8) or (data[1].toInt() and 0xff)
fun MetaMessage.read3ByteInt() =
    (data[0].toInt() and 0xff shl 16) or (data[1].toInt() and 0xff shl 8) or (data[2].toInt() and 0xff)

enum class MessageType(val metaType: Int) {
    SEQUENCE_NUMBER(0x00),
    TEXT(0x01),
    COPYRIGHT_NOTICE(0x02),
    TRACK_NAME(0x03),
    INSTRUMENT_NAME(0x04),
    LYRICS(0x05),
    MARKER(0x06),
    CUE_POINT(0x07),
    CHANNEL_PREFIX(0x20),
    END_OF_TRACK(0x2F),
    SET_TEMPO(0x51),
    SMPTE_OFFSET(0x54),
    TIME_SIGNATURE(0x58),
    KEY_SIGNATURE(0x59),
    SEQUENCER_SPECIFIC(0x7F)
}
fun MetaMessage.getMessageType() =
    MessageType.values().find { it.metaType == type }

enum class MessageStatus {
    ACTIVE_SENSING,
    CHANNEL_PRESSURE,
    CONTINUE,
    CONTROL_CHANGE,
    END_OF_EXCLUSIVE,
    MIDI_TIME_CODE,
    NOTE_OFF,
    NOTE_ON,
    PITCH_BEND,
    POLY_PRESSURE,
    PROGRAM_CHANGE,
    SONG_POSITION_POINTER,
    SONG_SELECT,
    START,
    STOP,
    SYSTEM_RESET,
    TIMING_CLOCK,
    TUNE_REQUEST
}

fun ShortMessage.getMessageStatus() = when(status) {
    ShortMessage.ACTIVE_SENSING -> MessageStatus.ACTIVE_SENSING
    ShortMessage.CHANNEL_PRESSURE -> MessageStatus.CHANNEL_PRESSURE
    ShortMessage.CONTINUE -> MessageStatus.CONTINUE
    ShortMessage.CONTROL_CHANGE -> MessageStatus.CONTROL_CHANGE
    ShortMessage.END_OF_EXCLUSIVE -> MessageStatus.END_OF_EXCLUSIVE
    ShortMessage.MIDI_TIME_CODE -> MessageStatus.MIDI_TIME_CODE
    ShortMessage.NOTE_OFF -> MessageStatus.NOTE_OFF
    ShortMessage.NOTE_ON -> MessageStatus.NOTE_ON
    ShortMessage.PITCH_BEND -> MessageStatus.PITCH_BEND
    ShortMessage.POLY_PRESSURE -> MessageStatus.POLY_PRESSURE
    ShortMessage.PROGRAM_CHANGE -> MessageStatus.PROGRAM_CHANGE
    ShortMessage.SONG_POSITION_POINTER -> MessageStatus.SONG_POSITION_POINTER
    ShortMessage.SONG_SELECT -> MessageStatus.SONG_SELECT
    ShortMessage.START -> MessageStatus.START
    ShortMessage.STOP -> MessageStatus.STOP
    ShortMessage.SYSTEM_RESET -> MessageStatus.SYSTEM_RESET
    ShortMessage.TIMING_CLOCK -> MessageStatus.TIMING_CLOCK
    ShortMessage.TUNE_REQUEST -> MessageStatus.TUNE_REQUEST
    else -> null
}

/**
 * See https://magenta.tensorflow.org/datasets/groove#drum-mapping for reference
 *
 * @param pitches TODO: one pitch per instrument?
 */
enum class Instrument(vararg val pitches: Int) {
    KICK(36),
    SNARE(38, 40, 37),
//    HIGH_TOM(48, 50),
//    LOW_MID_TOM(45, 47),
//    HIGH_FLOOR_TOM(43, 58),
    OPEN_HI_HAT(46, 26),
    CLOSED_HI_HAT(42, 22, 44),
//    CRASH(49, 55, 57, 52),
//    RIDE(51, 59, 53)
}
fun ShortMessage.getInstrument() = if (status in 128..175)
    Instrument.values().find { it.pitches.contains(data1) }!!
else
    throw Exception("Invalid status, could not get instrument")