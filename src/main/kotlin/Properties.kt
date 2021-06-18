import javafx.beans.property.*
import java.io.File
import java.lang.Exception
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend
 * @since   22/01/2021
 */
object Properties {

    val inputMidiDirectory =  SimpleObjectProperty(Paths.get("data/midi").toFile())
    val inputDatDirectory =  SimpleObjectProperty(Paths.get("data/dat").toFile())

    val outputMidiFile = SimpleObjectProperty(Paths.get("data/midi/generated").toFile())
    val outputWavFile = SimpleObjectProperty(Paths.get("data/wav").toFile())
    val outputDatFile = SimpleObjectProperty(Paths.get("data/dat").toFile())

    fun bind(sessionManager: PropertiesManager) {
        sessionManager.bindFile("inputMidiPath", inputMidiDirectory)
        sessionManager.bindFile("outputMidiPath", outputMidiFile)
        sessionManager.bindFile("outputWavPath", outputWavFile)
        sessionManager.bindFile("outputDatPath", outputDatFile)
    }
}

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend
 * @since   28/01/2021
 */
class PropertiesManager(
    private val saveFilePath: Path = Paths.get("session.properties")
) {
    private val properties = Properties()
    private lateinit var saveThread : Thread

    fun loadFromFile() {

        if (!saveFilePath.toFile().exists())
            return

        try {
            properties.load(saveFilePath.toFile().reader())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!this::saveThread.isInitialized){
            saveThread = Thread {
                saveToFile()
                Thread.sleep(2500L)
            }
            saveThread.start()
        }
    }

    fun saveToFile() {
        try {
            properties.store(saveFilePath.toFile().writer(), "Contains properties for the Qodat application.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun<T> bind(key: String, property: Property<T>, serializer: (T) -> String = { it.toString() }, deserialiser: (String) -> T){
        val value = properties.getProperty(key)
        if (value != null)
            property.value = deserialiser.invoke(value)
        property.addListener { _ ->
            properties.setProperty(key, serializer.invoke(property.value!!))
        }
    }

    fun bindPath(key: String, property: ObjectProperty<Path>) = bind(key, property)
    { Paths.get(it) }

    fun bindFile(key: String, property: ObjectProperty<File>) = bind(key, property,
        deserialiser = { Paths.get(it).toFile() },
        serializer = { it.absolutePath })

    fun bindBoolean(key: String, property: BooleanProperty) = bind(key, property)
    { java.lang.Boolean.parseBoolean(it) }

    fun bindDouble(key: String, property: DoubleProperty) = bind(key, property)
    { java.lang.Double.parseDouble(it) }

    fun bindInt(key: String, property: IntegerProperty) = bind(key, property)
    { java.lang.Integer.parseInt(it) }

    fun bindString(key: String, property: StringProperty) = bind(key, property)
    { it }
}