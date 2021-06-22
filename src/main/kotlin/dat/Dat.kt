package dat

import midi.Instrument

data class Dat(
    val divisionType: Float,
    val resolution: Int,
    val events: Map<Int, Map<Instrument, Boolean>>
)