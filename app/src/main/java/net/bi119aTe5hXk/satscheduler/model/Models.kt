package net.bi119aTe5hXk.satscheduler.model

data class Satellite(
    val satId: String,
    val name: String?,
    val names: String?,
    val noradCatId: Int?,
    val status: String?,
    val inOrbit: Boolean?
) {
    val displayName: String
        get() = name ?: names ?: satId
}

data class GroundStation(
    val id: Int,
    val name: String?,
    val altitude: Double?,
    val minHorizon: Double?,
    val lat: Double?,
    val lng: Double?,
    val qthlocator: String?,
    val antennaText: String?,
    val owner: String?,
    val status: String?,
    val observations: Int?,
    val futureObservations: Int?,
    val successRate: Int?
) {
    val displayName: String
        get() = name ?: "Station $id"
}

data class Transmitter(
    val uuid: String,
    val satId: String?,
    val description: String?,
    val status: String?,
    val downlinkLow: Int?,
    val downlinkHigh: Int?,
    val mode: String?
) {
    val displayName: String
        get() = listOfNotNull(description?.takeIf { it.isNotBlank() }, mode?.takeIf { it.isNotBlank() })
            .joinToString(" / ")
            .ifBlank { uuid }

    val frequencyText: String
        get() = when {
            downlinkLow == null -> "-"
            downlinkHigh != null && downlinkHigh != downlinkLow ->
                "${formatFrequencyMHz(downlinkLow)}-${formatFrequencyMHz(downlinkHigh)} MHz"
            else -> "${formatFrequencyMHz(downlinkLow)} MHz"
        }

    val defaultCenterFrequencyHz: Int?
        get() = when {
            downlinkLow == null -> null
            downlinkHigh != null && downlinkHigh != downlinkLow -> (downlinkLow + downlinkHigh) / 2
            else -> downlinkLow
        }

    val requiresManualCenterFrequency: Boolean
        get() = downlinkLow != null && downlinkHigh != null && downlinkLow != downlinkHigh
}

data class Observation(
    val id: Int,
    val start: String?,
    val end: String?,
    val groundStation: Int?,
    val transmitter: String?,
    val satId: String?,
    val stationName: String?,
    val stationLat: Double?,
    val stationLng: Double?,
    val stationAlt: Double?,
    val satelliteName: String?,
    val noradCatId: Int?,
    val payload: String?,
    val waterfall: String?,
    val status: String?,
    val vettedStatus: String?,
    val riseAzimuth: Double?,
    val setAzimuth: Double?,
    val transmitterDescription: String?,
    val transmitterUuid: String?,
    val transmitterType: String?,
    val transmitterMode: String?,
    val transmitterDownlinkLow: Int?,
    val transmitterDownlinkHigh: Int?,
    val tle0: String?,
    val tle1: String?,
    val tle2: String?,
    val centerFrequency: Int?,
    val observer: String?,
    val observationFrequency: Int?,
    val maxAltitude: Double?
) {
    val stationDisplayName: String
        get() = stationName ?: groundStation?.let { "Station $it" } ?: "Station"

    val title: String
        get() = satelliteName ?: tle0 ?: noradCatId?.let { "NORAD $it" } ?: "Observation $id"

    val transmitterDisplayName: String
        get() = transmitterDescription ?: transmitterUuid ?: transmitter ?: "Transmitter"

    val frequencyText: String
        get() = (observationFrequency ?: transmitterDownlinkLow ?: centerFrequency)
            ?.let { "%.3f MHz".format(it / 1_000_000.0) } ?: "-"

    val maxAltitudeText: String
        get() = maxAltitude?.let { "%.0f deg".format(it) } ?: "-"

    val vettedStatusDisplayText: String
        get() = vettedStatus ?: "unknown"
}

data class WatchTarget(
    val id: String,
    val name: String,
    val satelliteId: String,
    val satelliteName: String?,
    val transmitterId: String,
    val transmitterDescription: String?,
    val centerFrequency: Int?,
    val stationIds: List<Int>,
    val stationNames: Map<Int, String> = emptyMap(),
    val stationSnapshots: Map<Int, WatchStationSnapshot> = emptyMap(),
    val enabled: Boolean = true,
    val requireStationDaylight: Boolean = false,
    val minElevation: Double? = null,
    val minPeakElevation: Double? = null,
    val maxPeakElevation: Double? = null,
    val minAzimuth: Double? = null,
    val maxAzimuth: Double? = null
)

data class WatchStationSnapshot(
    val id: Int,
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val minHorizon: Double?
)

data class TleEntry(
    val tle0: String?,
    val tle1: String,
    val tle2: String,
    val source: String?,
    val satId: String,
    val noradCatId: Int?,
    val updated: String?
) {
    val displayName: String
        get() = tle0?.takeIf { it.isNotBlank() } ?: satId
}

data class PredictedPass(
    val stationId: Int,
    val stationName: String,
    val start: String,
    val end: String,
    val maxElevation: Double,
    val azimuthStart: Double,
    val azimuthEnd: Double,
    val azimuthSamples: List<Double>
) {
    val startInstant: java.time.Instant
        get() = net.bi119aTe5hXk.satscheduler.data.parseSatnogsInstant(start)

    val endInstant: java.time.Instant
        get() = net.bi119aTe5hXk.satscheduler.data.parseSatnogsInstant(end)
}

data class PredictionConflictSummary(
    val predictedCount: Int,
    val visibleCount: Int,
    val hiddenCount: Int
)

private fun formatFrequencyMHz(frequencyHz: Int): String {
    return "%.6f".format(frequencyHz / 1_000_000.0)
        .trimEnd('0')
        .trimEnd('.')
}
