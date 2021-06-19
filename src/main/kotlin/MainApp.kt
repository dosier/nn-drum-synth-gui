import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import view.MainView
import player.PlayerStyles

class MainApp : App(MainView::class, PlayerStyles::class) {

    /**
     * Handles the serialisation of [Properties].
     */
    private val propertiesManager = PropertiesManager()

    override fun start(stage: Stage) {
        propertiesManager.loadFromFile()
        Properties.bind(propertiesManager)
        super.start(stage)
        stage.setOnCloseRequest {
            propertiesManager.saveToFile()
        }
    }
}



fun main(args: Array<String>) {
    launch<MainApp>(args)
}