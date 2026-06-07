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
        .put("stationName", stationName)
        .put("satelliteName", satelliteName)
        .put("noradCatId", noradCatId)
        .put("status", status)
        .put("transmitterDescription", transmitterDescription)
        .put("transmitterUuid", transmitterUuid)
        .put("observationFrequency", observationFrequency)
        .put("maxAltitude", maxAltitude)
}

private fun JSONObject.toObservationCache(): Observation {
    return Observation(
        id = optInt("id"),
        start = stringOrNull("start"),
        end = stringOrNull("end"),
        groundStation = intOrNull("groundStation"),
        stationName = stringOrNull("stationName"),
        satelliteName = stringOrNull("satelliteName"),
        noradCatId = intOrNull("noradCatId"),
        status = stringOrNull("status"),
        transmitterDescription = stringOrNull("transmitterDescription"),
        transmitterUuid = stringOrNull("transmitterUuid"),
        observationFrequency = intOrNull("observationFrequency"),
        maxAltitude = doubleOrNull("maxAltitude")
    )
}

private fun JSONObject.stringOrNull(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.intOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.doubleOrNull(name: String): Double? = if (has(name) && !isNull(name)) optDouble(name) else null
