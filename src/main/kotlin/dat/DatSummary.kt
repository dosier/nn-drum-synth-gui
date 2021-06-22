package dat

import midi.Instrument

data class DatSummary(
    val minIntervalLengths: Map<Instrument, Int>,
    val maxIntervalLengths: Map<Instrument, Int>,
    val meanIntervalLengths: Map<Instrument, Double>,
)