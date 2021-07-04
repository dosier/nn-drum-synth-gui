package player

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.onChange
import java.io.File
import java.io.IOException
import java.lang.Exception
import javax.sound.midi.Sequence
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class MidiPlayer : Runnable {

    private lateinit var playingThread: Thread

    val running = SimpleBooleanProperty(false)

    val sequenceProperty = SimpleObjectProperty<Sequence>()

    val file = SimpleObjectProperty<File>()

    val play = SimpleBooleanProperty(true).apply {
        onChange {
            if (it && !running.get()){
                startThread()
            }
        }
    }

    val disable = SimpleBooleanProperty(false)

    fun startThread() {
        if (this@MidiPlayer::playingThread.isInitialized) {
            running.set(false)
            playingThread.join()
        }
        playingThread = Thread(this@MidiPlayer)
        playingThread.start()
    }

    override fun run() {
        try {
            val audioInputStream = AudioSystem.getAudioInputStream(file.get())
            val format = audioInputStream!!.format
            val info = DataLine.Info(SourceDataLine::class.java, format)
            val auline = AudioSystem.getLine(info) as SourceDataLine
            auline.open(format)
            auline.start()
            val totalLength = audioInputStream.available()
            var nBytesRead = 0
            val abData = ByteArray(EXTERNAL_BUFFER_SIZE)
            var offset = 0
            try {
                running.set(true)
                while (nBytesRead != -1) {
                    nBytesRead = audioInputStream.read(abData, 0, abData.size)
                    if (nBytesRead >= 0)
                        auline.write(abData, 0, nBytesRead)
                    offset += nBytesRead
                    if (nBytesRead >= totalLength) {
                        auline.stop()
                        break
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                auline.drain()
                auline.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        play.set(false)
        running.set(false)
    }

    companion object {
        private const val EXTERNAL_BUFFER_SIZE = 262_144 // 64Kb
    }
}