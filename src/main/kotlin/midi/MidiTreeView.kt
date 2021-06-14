package midi

import javafx.beans.property.ObjectProperty
import javafx.event.EventTarget
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import tornadofx.*
import javax.sound.midi.*


fun EventTarget.createTreeView(sequenceProperty: ObjectProperty<Sequence>, populate: (Sequence, TreeItem<Any>, TreeItem<Any>) -> Iterable<Any>? = { sequence, root, parent ->
    val value = parent.value
    when  {
        parent === root -> sequence.tracks.asIterable()
        value is Track -> Array(value.size()) { value[it] }.asIterable()
        value is MidiEvent -> arrayOf(value.message).asIterable()
        else -> null
    }
}) : TreeView<Any> {
    val tree = treeview<Any> {
        root = TreeItem("ROOT")
        cellFormat {
            text = when (it) {
                is String -> it
                is Track -> "size = ${it.size()}"
                is MidiEvent -> {
                    "tick = ${it.tick} | status =  ${it.message.status} | length = ${it.message.length}"
                }
                is MetaMessage -> {
                    it.getMessageType()?.name?:"NULL"
                }
                is ShortMessage -> {
                    when (val statusByte = it.status) {
                        // Channel Note Off
                        in 128..143 -> {
                            val channel = statusByte - 127  // (1..16)
                            val noteNumber = it.data1       // (0..127)
                            val noteVelocity = it.data2     // (0..127
                            "NOTE_OFF\t\tchannel = $channel\tnote = $noteNumber\tnoteVelocity=$noteVelocity"
                        }
                        // Channel Note On
                        in 144..159 -> {
                            val channel = statusByte - 143  // (1..16)
                            val noteNumber = it.data1       // (0..127)
                            val noteVelocity = it.data2     // (0..127
                            "NOTE_ON\t\tchannel = $channel\tnote = $noteNumber\tnoteVelocity=$noteVelocity"
                        }
                        // Channel Polyphonic Aftertouch
                        in 160..175 -> {
                            val channel = statusByte - 159  // (1..16)
                            val noteNumber = it.data1       // (0..127)
                            val pressure = it.data2         // (0..127
                            "A_TOUCH\t\tchannel = $channel\tnote = $noteNumber\tpressure=$pressure"
                        }
                        // Channel Control/Mode Change
                        // see https://www.midi.org/specifications-old/item/table-3-control-change-messages-data-bytes-2
                        in 176..191 -> {
                            val channel = statusByte - 175  // (1..16)
                            val unknown1 = it.data1         // (0..127)
                            val unknown2 = it.data2         // (0..127
                            "CM_CHANGE\tchannel = $channel\t???? = $unknown1\t???????=$unknown2"
                        }
                        // Channel Program Change
                        in 192..207 -> {
                            val channel = statusByte - 191  // (1..16)
                            val program = it.data1          // (0..127)
                            "PR_CHANGE\tchannel = $channel\tprog = $program"
                        }
                        // Channel Channel Aftertouch
                        in 208..223 -> {
                            val channel = statusByte - 207  // (1..16)
                            val pressure = it.data1         // (0..127)
                            "C_A_TOUCH\tchannel = $channel\tpressure = $pressure"
                        }
                        else -> it.getMessageStatus()?.name?:"NULL"
                    }
                }
                else -> throw IllegalArgumentException("Invalid value type")
            }
        }
    }
    sequenceProperty.onChange {  sequence ->
        if (sequence != null) {
            tree.populate {
                populate(sequence, tree.root, it)
            }
        } else
            tree.getChildList()?.clear()
    }
    return tree
}
