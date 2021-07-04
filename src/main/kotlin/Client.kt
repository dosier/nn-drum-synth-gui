import com.sun.javafx.application.PlatformImpl
import midi.Instrument
import java.io.*
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList



class Task(
    val modelType: ModelType,
    val bpm: Int,
    val repeat: Int,
    val inStates: Array<Array<Boolean>>,
    val result: (Array<Array<Boolean>>, String) -> Unit
)
class Client(private val socket: Socket) : Runnable {

    private var running = true
    private var tasks = CopyOnWriteArrayList<Task>()

    fun predict(task: Task) {
        tasks += task
    }

    fun start() {
        running = true
    }

    fun pause() {
        running = false
    }

    override fun run() {
        val `in` = DataInputStream(socket.getInputStream())
        val writer = DataOutputStream(socket.getOutputStream())
        while(running) {
            while(tasks.isEmpty()){
                Thread.sleep(1000L)
            }
            println(`in`.readLine())
            if (tasks.isNotEmpty()) {
                for (task in tasks){
                    val type = task.modelType
                    val bpm = task.bpm
                    val repeat = task.repeat
                    val matrix = task.inStates
                    val bytes = matrix.flatten().map { if (it) 1.toByte() else 0.toByte() }.toByteArray()
                    writer.write(byteArrayOf(type.ordinal.toByte(), bpm.toByte(), repeat.toByte()) + bytes)
                    writer.flush()
                    `in`.read(bytes)
                    writer.write(0)
                    writer.flush()
                    val midiFilePath = `in`.readLine()
                    println("midiFilePath = $midiFilePath")
                    PlatformImpl.runAndWait {
                        val instrumentsCount = Instrument.values().size
                        val result = Array(16) { Array(instrumentsCount) { false } }
                        var idx = 0
                        for (i in 0 until 16) {
                            for (j in 0 until instrumentsCount) {
                                result[i][j] = bytes[idx++] == 1.toByte()
                            }
                        }
                        task.result(result, midiFilePath)
                    }
                }
                tasks.clear()
            }
            Thread.sleep(5000L)
        }
    }
}