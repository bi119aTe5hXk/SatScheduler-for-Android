package net.bi119aTe5hXk.satscheduler.data

import android.content.Context
import net.bi119aTe5hXk.satscheduler.model.WatchTarget
import net.bi119aTe5hXk.satscheduler.model.WatchStationSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WatchTargetStore(context: Context) {
    private val prefs = context.getSharedPreferences("sat_scheduler", Context.MODE_PRIVATE)

    fun loadToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    fun loadTargets(): List<WatchTarget> {
        val raw = prefs.getString(KEY_TARGETS, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.toWatchTarget()
        }
    }

    fun saveTargets(targets: List<WatchTarget>) {
        val array = JSONArray()
        targets.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_TARGETS, array.toString()).apply()
    }

    fun exportTargets(targets: List<WatchTarget> = loadTargets()): String {
        val array = JSONArray()
        targets.forEach { array.put(it.toJson()) }
        return array.toString(2)
    }

    fun importTargets(rawJson: String): List<WatchTarget> {
        val array = JSONArray(rawJson)
        val targets = (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.toWatchTarget()
        }
        saveTargets(targets)
        return targets
    }

    companion object {
        private const val KEY_TOKEN = "api_token"
        private const val KEY_TARGETS = "watch_targets"
    }
}

fun newWatchTarget(
    name: String,
    satelliteId: String,
    satelliteName: String?,
    transmitterId: String,
    transmitterDescription: String?,
    centerFrequency: Int?,
    stationIds: List<Int>,
    stationNames: Map<Int, String>,
    stationSnapshots: Map<Int, WatchStationSnapshot>,
    requireStationDaylight: Boolean,
    minPeakElevation: Double?,
    maxPeakElevation: Double?,
    minAzimuth: Double?,
    maxAzimuth: Double?
): WatchTarget {
    return WatchTarget(
        id = UUID.randomUUID().toString(),
        name = name.ifBlank { satelliteName ?: satelliteId },
        satelliteId = satelliteId,
        satelliteName = satelliteName,
        transmitterId = transmitterId,
        transmitterDescription = transmitterDescription,
        centerFrequency = centerFrequency,
        stationIds = stationIds,
        stationNames = stationNames,
        stationSnapshots = stationSnapshots,
        requireStationDaylight = requireStationDaylight,
        minPeakElevation = minPeakElevation,
        maxPeakElevation = maxPeakElevation,
        minAzimuth = minAzimuth,
        maxAzimuth = maxAzimuth
    )
}

private fun JSONObject.toWatchTarget(): WatchTarget {
    val stations = optJSONArray("stationIds") ?: JSONArray()
    val stationNamesJson = optJSONObject("stationNames") ?: JSONObject()
    val stationNames = stationNamesJson.keys().asSequence().mapNotNull { key ->
        key.toIntOrNull()?.let { id -> id to stationNamesJson.optString(key) }
    }.toMap()
    val stationSnapshotsJson = optJSONObject("stationSnapshots") ?: JSONObject()
    val stationSnapshots = stationSnapshotsJson.keys().asSequence().mapNotNull { key ->
        val id = key.toIntOrNull() ?: return@mapNotNull null
        stationSnapshotsJson.optJSONObject(key)?.toStationSnapshot()?.let { id to it }
    }.toMap()
    return WatchTarget(
        id = optString("id"),
        name = optString("name"),
        satelliteId = optString("satelliteId"),
        satelliteName = optString("satelliteName").takeIf { it.isNotBlank() },
        transmitterId = optString("transmitterId"),
        transmitterDescription = optString("transmitterDescription").takeIf { it.isNotBlank() },
        centerFrequency = if (has("centerFrequency") && !isNull("centerFrequency")) optInt("centerFrequency") else null,
        stationIds = (0 until stations.length()).mapNotNull { index ->
            if (stations.isNull(index)) null else stations.optInt(index)
        },
        stationNames = stationNames,
        stationSnapshots = stationSnapshots,
        enabled = optBoolean("enabled", true),
        requireStationDaylight = optBoolean("requireStationDaylight", false),
        minElevation = if (has("minElevation") && !isNull("minElevation")) optDouble("minElevation") else null,
        minPeakElevation = if (has("minPeakElevation") && !isNull("minPeakElevation")) optDouble("minPeakElevation") else null,
        maxPeakElevation = if (has("maxPeakElevation") && !isNull("maxPeakElevation")) optDouble("maxPeakElevation") else null,
        minAzimuth = if (has("minAzimuth") && !isNull("minAzimuth")) optDouble("minAzimuth") else null,
        maxAzimuth = if (has("maxAzimuth") && !isNull("maxAzimuth")) optDouble("maxAzimuth") else null
    )
}

private fun WatchTarget.toJson(): JSONObject {
    val stationNamesJson = JSONObject()
    stationNames.forEach { (id, name) -> stationNamesJson.put(id.toString(), name) }
    val stationSnapshotsJson = JSONObject()
    stationSnapshots.forEach { (id, snapshot) -> stationSnapshotsJson.put(id.toString(), snapshot.toJson()) }
    return JSONObject()
        .put("id", id)
        .put("name", name)
        .put("satelliteId", satelliteId)
        .put("satelliteName", satelliteName)
        .put("transmitterId", transmitterId)
        .put("transmitterDescription", transmitterDescription)
        .put("centerFrequency", centerFrequency)
        .put("stationIds", JSONArray(stationIds))
        .put("stationNames", stationNamesJson)
        .put("stationSnapshots", stationSnapshotsJson)
        .put("enabled", enabled)
        .put("requireStationDaylight", requireStationDaylight)
        .put("minElevation", minElevation)
        .put("minPeakElevation", minPeakElevation)
        .put("maxPeakElevation", maxPeakElevation)
        .put("minAzimuth", minAzimuth)
        .put("maxAzimuth", maxAzimuth)
}

private fun JSONObject.toStationSnapshot(): WatchStationSnapshot {
    return WatchStationSnapshot(
        id = optInt("id"),
        name = optString("name"),
        latitude = if (has("latitude") && !isNull("latitude")) optDouble("latitude") else null,
        longitude = if (has("longitude") && !isNull("longitude")) optDouble("longitude") else null,
        altitude = if (has("altitude") && !isNull("altitude")) optDouble("altitude") else null,
        minHorizon = if (has("minHorizon") && !isNull("minHorizon")) optDouble("minHorizon") else null
    )
}

private fun WatchStationSnapshot.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("name", name)
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("altitude", altitude)
        .put("minHorizon", minHorizon)
}
