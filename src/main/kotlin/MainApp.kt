import tornadofx.App
import tornadofx.launch
import view.MainView
import player.PlayerStyles

class MainApp : App(MainView::class, PlayerStyles::class)

fun main(args: Array<String>) {
    launch<MainApp>(args)
}