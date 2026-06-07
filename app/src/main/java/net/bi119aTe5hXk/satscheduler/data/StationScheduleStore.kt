package net.bi119aTe5hXk.satscheduler.data

import android.content.Context
import kotlinx.coroutines.delay
import net.bi119aTe5hXk.satscheduler.model.Observation
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class StationScheduleStore(context: Context) {
    private val prefs = context.getSharedPreferences("sat_scheduler_station_schedule", Context.MODE_PRIVATE)

    fun load(stationIds: List<Int>): List<Observation> {
        return stationIds.flatMap { stationId ->
            val raw = prefs.getString(keyForStation(stationId), "[]") ?: "[]"
            val array = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.toObservationCache()
            }
        }.sortedBy { it.start ?: "" }
    }

    fun lastUpdatedAt(stationId: Int): String? {
        return prefs.getString(keyForUpdatedAt(stationId), null)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun mergeCreatedObservations(observations: List<Observation>) {
        val stationIds = observations.mapNotNull { it.groundStation }.distinct()
        stationIds.forEach { stationId ->
            val mergedById = (load(listOf(stationId)) + observations.filter { it.groundStation == stationId })
                .associateBy { it.id }
            saveStation(stationId, mergedById.values.sortedBy { it.start ?: "" })
        }
    }

    suspend fun refresh(
        api: SatnogsApi,
        stationIds: List<Int>,
        stationDelayMillis: Long = 3_000
    ): List<Observation> {
        val uniqueStationIds = stationIds.distinct().sorted()
        val refreshed = mutableListOf<Observation>()

        uniqueStationIds.forEachIndexed { index, stationId ->
            val observations = api.fetchScheduledObservations(stationId)
            saveStation(stationId, observations)
            refreshed += observations

            if (index < uniqueStationIds.lastIndex) {
                delay(stationDelayMillis)
            }
        }

        return refreshed.sortedBy { it.start ?: "" }
    }

    private fun saveStation(stationId: Int, observations: List<Observation>) {
        val array = JSONArray()
        observations.forEach { array.put(it.toJson()) }
        prefs.edit()
            .putString(keyForStation(stationId), array.toString())
            .putString(keyForUpdatedAt(stationId), Instant.now().toString())
            .apply()
    }

    private fun keyForStation(stationId: Int): String = "station_$stationId"

    private fun keyForUpdatedAt(stationId: Int): String = "station_${stationId}_updated_at"
}

private fun Observation.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("start", start)
        .put("end", end)
        .put("groundStation", groundStation)
        .put("transmitter", transmitter)
        .put("satId", satId)
        .put("stationName", stationName)
        .put("stationLat", stationLat)
        .put("stationLng", stationLng)
        .put("stationAlt", stationAlt)
        .put("satelliteName", satelliteName)
        .put("noradCatId", noradCatId)
        .put("payload", payload)
        .put("waterfall", waterfall)
        .put("status", status)
        .put("vettedStatus", vettedStatus)
        .put("riseAzimuth", riseAzimuth)
        .put("setAzimuth", setAzimuth)
        .put("transmitterDescription", transmitterDescription)
        .put("transmitterUuid", transmitterUuid)
        .put("transmitterType", transmitterType)
        .put("transmitterMode", transmitterMode)
        .put("transmitterDownlinkLow", transmitterDownlinkLow)
        .put("transmitterDownlinkHigh", transmitterDownlinkHigh)
        .put("tle0", tle0)
        .put("tle1", tle1)
        .put("tle2", tle2)
        .put("centerFrequency", centerFrequency)
        .put("observer", observer)
        .put("observationFrequency", observationFrequency)
        .put("maxAltitude", maxAltitude)
}

private fun JSONObject.toObservationCache(): Observation {
    return Observation(
        id = optInt("id"),
        start = stringOrNull("start"),
        end = stringOrNull("end"),
        groundStation = intOrNull("groundStation"),
        transmitter = stringOrNull("transmitter"),
        satId = stringOrNull("satId"),
        stationName = stringOrNull("stationName"),
        stationLat = doubleOrNull("stationLat"),
        stationLng = doubleOrNull("stationLng"),
        stationAlt = doubleOrNull("stationAlt"),
        satelliteName = stringOrNull("satelliteName"),
        noradCatId = intOrNull("noradCatId"),
        payload = stringOrNull("payload"),
        waterfall = stringOrNull("waterfall"),
        status = stringOrNull("status"),
        vettedStatus = stringOrNull("vettedStatus"),
        riseAzimuth = doubleOrNull("riseAzimuth"),
        setAzimuth = doubleOrNull("setAzimuth"),
        transmitterDescription = stringOrNull("transmitterDescription"),
        transmitterUuid = stringOrNull("transmitterUuid"),
        transmitterType = stringOrNull("transmitterType"),
        transmitterMode = stringOrNull("transmitterMode"),
        transmitterDownlinkLow = intOrNull("transmitterDownlinkLow"),
        transmitterDownlinkHigh = intOrNull("transmitterDownlinkHigh"),
        tle0 = stringOrNull("tle0"),
        tle1 = stringOrNull("tle1"),
        tle2 = stringOrNull("tle2"),
        centerFrequency = intOrNull("centerFrequency"),
        observer = stringOrNull("observer"),
        observationFrequency = intOrNull("observationFrequency"),
        maxAltitude = doubleOrNull("maxAltitude")
    )
}

private fun JSONObject.stringOrNull(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.intOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.doubleOrNull(name: String): Double? = if (has(name) && !isNull(name)) optDouble(name) else null
