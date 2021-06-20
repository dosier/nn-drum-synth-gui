import com.sun.javafx.application.PlatformImpl
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class Task(val file: File, val result: (File) -> Unit)
class Client(private val socket: Socket) : Runnable {

    private var running = true
    private var tasks = CopyOnWriteArrayList<Task>()

    fun predict(file: File, update: (File) -> Unit) {
        tasks += Task(file, update)
    }

    fun start() {
        running = true
    }

    fun pause() {
        running = false
    }

    override fun run() {
        val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
        val out = PrintWriter(socket.getOutputStream())
        while(running) {
            println(`in`.readLine())
            if (tasks.isNotEmpty()) {
                for (task in tasks){
                    out.println(task.file.absolutePath)
                    out.flush()
                    val predictedPath = `in`.readLine()
                    val file = File(predictedPath)
                    PlatformImpl.runAndWait {
                        task.result(file)
                    }
                }
            }
        }
    }
}