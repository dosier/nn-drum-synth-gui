package player

import javafx.scene.paint.Color
import tornadofx.*

class PlayerStyles : Stylesheet() {

    companion object {
        val playButton by cssclass()
    }

    init {
        playButton {
            materialButtonStyle()
            shape = "M8 5v14l11-7z"
            and(selected) {
                shape = "M6 19h4V5H6v14zm8-14v14h4V5h-4z"
            }
        }
    }

    private fun CssSelectionBlock.materialButtonStyle() {
        backgroundColor += c("#2B2B2B")
        prefHeight = 24.px
        prefWidth = 24.px
    }
}