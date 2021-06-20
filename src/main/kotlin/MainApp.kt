import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import view.MainView
import player.PlayerStyles
import java.io.File
import java.net.Socket

lateinit var client: Client

class MainApp : App(MainView::class, PlayerStyles::class) {

    /**
     * Handles the serialisation of [Properties].
     */
    private val propertiesManager = PropertiesManager()

    override fun start(stage: Stage) {
        propertiesManager.loadFromFile()
        Properties.bind(propertiesManager)
        client = Client(Socket("localhost", 9999))
        Thread(client).start()
        client.predict(File("/Users/stanvanderbend/IdeaProjects/nn-project-data/data/midi/generated/138_funk-fast_125_fill_4-4.midi")) {
            println("Predicted: $it")
        }
        super.start(stage)
        stage.setOnCloseRequest {
            propertiesManager.saveToFile()
        }
    }
}



fun main(args: Array<String>) {
    launch<MainApp>(args)
}