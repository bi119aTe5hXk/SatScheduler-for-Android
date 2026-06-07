package net.bi119aTe5hXk.satscheduler.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.bi119aTe5hXk.satscheduler.model.GroundStation
import net.bi119aTe5hXk.satscheduler.model.Observation
import net.bi119aTe5hXk.satscheduler.model.Satellite
import net.bi119aTe5hXk.satscheduler.model.TleEntry
import net.bi119aTe5hXk.satscheduler.model.Transmitter
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.URL
import java.time.Instant
import java.util.Base64

class SatnogsApi(private val tokenProvider: () -> String?) {
    suspend fun fetchAliveSatellites(query: String? = null): List<Satellite> {
        return getArray("https://db.satnogs.org/api/satellites/", mapOf("status" to "alive"))
            .map { it.toSatellite() }
    }

    suspend fun fetchTransmitters(satelliteId: String): List<Transmitter> {
        return getArray(
            "https://db.satnogs.org/api/transmitters/",
            mapOf("sat_id" to satelliteId)
        )
            .map { it.toTransmitter() }
            .let { transmitters ->
                transmitters.filter { it.satId == satelliteId && it.status == "active" }
                    .ifEmpty { transmitters.filter { it.satId == satelliteId } }
            }
    }

    suspend fun fetchLatestTle(satelliteId: String): TleEntry? {
        return getArray("https://db.satnogs.org/api/tle/", mapOf("sat_id" to satelliteId))
            .map { it.toTleEntry() }
            .filter { it.satId == satelliteId }
            .maxByOrNull { it.updatedInstant ?: Instant.EPOCH }
    }

    suspend fun fetchOnlineStations(query: String? = null): List<GroundStation> {
        return getArray("https://network.satnogs.org/api/stations/", mapOf("status" to "2"))
            .map { it.toGroundStation() }
    }

    suspend fun fetchObservations(observerId: String? = null, future: Boolean? = null): List<Observation> {
        val params = linkedMapOf<String, String>()
        if (!observerId.isNullOrBlank()) {
            params["observer"] = observerId.trim()
        }
        if (future != null) {
            params["future"] = if (future) "1" else "0"
        }
        return getObservationFirstPage(params)
    }

    suspend fun fetchGoodObservations(noradCatId: Int): List<Observation> {
        return fetchAllObservationPages(
            params = mapOf("status" to "good", "norad_cat_id" to noradCatId.toString()),
            maxPages = 2
        )
    }

    suspend fun fetchScheduledObservations(groundStationId: Int): List<Observation> {
        return fetchAllObservationPages(
            params = mapOf("status" to "future", "ground_station" to groundStationId.toString()),
            pageDelayMillis = 3_000
        )
    }

    suspend fun createObservations(requests: List<ObservationScheduleRequest>): List<Observation> {
        if (requests.isEmpty()) return emptyList()
        val body = JSONArray()
        requests.forEach { request ->
            body.put(
                JSONObject()
                    .put("ground_station", request.groundStationId)
                    .put("transmitter_uuid", request.transmitterUuid)
                    .put("start", request.start)
                    .put("end", request.end)
            )
        }
        val text = postText("https://network.satnogs.org/api/observations/", body.toString())
        val trimmed = text.trim()
        val array = if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONArray().put(JSONObject(trimmed))
        return array.objects().map { it.toObservation() }
    }

    private suspend fun getObservationFirstPage(params: Map<String, String>): List<Observation> {
        return getObservationPage(params).results
    }

    private suspend fun fetchAllObservationPages(
        params: Map<String, String>,
        maxPages: Int? = null,
        pageDelayMillis: Long = 0
    ): List<Observation> {
        val observations = mutableListOf<Observation>()
        var nextCursor: String? = null
        val seenCursors = mutableSetOf<String>()
        var pageCount = 0

        do {
            if (maxPages != null && pageCount >= maxPages) break

            val pageParams = params.toMutableMap()
            if (nextCursor != null) {
                if (!seenCursors.add(nextCursor)) break
                pageParams["cursor"] = nextCursor
            }

            val page = getObservationPage(pageParams)
            pageCount += 1
            observations += page.results
            nextCursor = page.nextCursor?.takeUnless { seenCursors.contains(it) }

            if (nextCursor != null && pageDelayMillis > 0) {
                delay(pageDelayMillis)
            }
        } while (nextCursor != null)

        return observations
    }

    private suspend fun getObservationPage(params: Map<String, String>): ObservationPage {
        val text = getText("https://network.satnogs.org/api/observations/", params)
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            val results = JSONArray(trimmed).objects().map { it.toObservation() }
            return ObservationPage(results, fallbackNextCursor(results))
        }

        val json = JSONObject(trimmed)
        val results = (json.optJSONArray("results") ?: JSONArray()).objects().map { it.toObservation() }
        return ObservationPage(
            results = results,
            nextCursor = cursorValue(json.stringOrNull("next")) ?: fallbackNextCursor(results)
        )
    }

    private data class ObservationPage(
        val results: List<Observation>,
        val nextCursor: String?
    )

    private fun cursorValue(nextUrl: String?): String? {
        if (nextUrl.isNullOrBlank()) return null
        val query = runCatching { URL(nextUrl).query }.getOrNull() ?: return null
        return query.split("&")
            .mapNotNull { item ->
                val parts = item.split("=", limit = 2)
                if (parts.size == 2 && parts[0] == "cursor") {
                    URLDecoder.decode(parts[1], "UTF-8")
                } else {
                    null
                }
            }
            .firstOrNull()
    }

    private fun fallbackNextCursor(observations: List<Observation>): String? {
        val lastStart = observations.lastOrNull()?.start ?: return null
        if (observations.size < 25) return null
        return runCatching {
            val encodedPosition = URLEncoder.encode(Instant.parse(lastStart).toString(), "UTF-8")
            Base64.getEncoder().encodeToString("p=$encodedPosition".toByteArray(Charsets.UTF_8))
        }.getOrNull()
    }

    private suspend fun getArray(url: String, params: Map<String, String>): List<JSONObject> {
        val text = getText(url, params)
        return JSONArray(text).objects()
    }

    private suspend fun getText(baseUrl: String, params: Map<String, String>): String = withContext(Dispatchers.IO) {
        val query = params
            .filterValues { it.isNotBlank() }
            .entries
            .joinToString("&") { entry ->
                "${entry.key}=${URLEncoder.encode(entry.value, "UTF-8")}"
            }
        val url = URL(if (query.isBlank()) baseUrl else "$baseUrl?$query")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            tokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
                setRequestProperty("Authorization", "Token $token")
            }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("SatNOGS returned HTTP $status: ${body.take(240)}")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun postText(baseUrl: String, body: String): String = withContext(Dispatchers.IO) {
        val token = tokenProvider()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("SatNOGS API token is required to schedule observations.")
        val connection = (URL(baseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Authorization", "Token $token")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("SatNOGS returned HTTP $status: ${responseBody.take(240)}")
            }
            responseBody
        } finally {
            connection.disconnect()
        }
    }
}

data class ObservationScheduleRequest(
    val groundStationId: Int,
    val transmitterUuid: String,
    val start: String,
    val end: String
)

private fun JSONArray.objects(): List<JSONObject> {
    return (0 until length()).mapNotNull { index -> optJSONObject(index) }
}

private fun JSONObject.stringOrNull(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.intOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.doubleOrNull(name: String): Double? = if (has(name) && !isNull(name)) optDouble(name) else null

private fun JSONObject.boolOrNull(name: String): Boolean? = if (has(name) && !isNull(name)) optBoolean(name) else null

private fun JSONObject.toSatellite(): Satellite {
    val rawNames = opt("names")
    val names = when (rawNames) {
        is JSONArray -> (0 until rawNames.length()).joinToString(", ") { rawNames.optString(it) }
        is String -> rawNames
        else -> null
    }
    return Satellite(
        satId = getString("sat_id"),
        name = stringOrNull("name"),
        names = names,
        noradCatId = intOrNull("norad_cat_id"),
        status = stringOrNull("status"),
        inOrbit = boolOrNull("in_orbit")
    )
}

private fun JSONObject.toTransmitter(): Transmitter {
    return Transmitter(
        uuid = getString("uuid"),
        satId = stringOrNull("sat_id"),
        description = stringOrNull("description"),
        status = stringOrNull("status"),
        downlinkLow = intOrNull("downlink_low"),
        downlinkHigh = intOrNull("downlink_high"),
        mode = stringOrNull("mode")
    )
}

private fun JSONObject.toTleEntry(): TleEntry {
    return TleEntry(
        tle0 = stringOrNull("tle0"),
        tle1 = getString("tle1"),
        tle2 = getString("tle2"),
        source = stringOrNull("tle_source"),
        satId = getString("sat_id"),
        noradCatId = intOrNull("norad_cat_id"),
        updated = stringOrNull("updated")
    )
}

private fun JSONObject.toGroundStation(): GroundStation {
    return GroundStation(
        id = getInt("id"),
        name = stringOrNull("name"),
        altitude = doubleOrNull("altitude"),
        minHorizon = doubleOrNull("min_horizon"),
        lat = doubleOrNull("lat"),
        lng = doubleOrNull("lng"),
        qthlocator = stringOrNull("qthlocator"),
        antennaText = optJSONArray("antenna")?.let { antennas ->
            (0 until antennas.length()).mapNotNull { index ->
                antennas.optJSONObject(index)?.let { antenna ->
                    listOfNotNull(
                        antenna.stringOrNull("band"),
                        antenna.stringOrNull("antenna_type_name")
                    ).joinToString(" / ").ifBlank { null }
                }
            }.joinToString(", ").ifBlank { null }
        },
        owner = stringOrNull("owner"),
        status = stringOrNull("status"),
        observations = intOrNull("observations"),
        futureObservations = intOrNull("future_observations"),
        successRate = intOrNull("success_rate")
    )
}

private fun JSONObject.toObservation(): Observation {
    return Observation(
        id = getInt("id"),
        start = stringOrNull("start"),
        end = stringOrNull("end"),
        groundStation = intOrNull("ground_station"),
        stationName = stringOrNull("station_name"),
        satelliteName = stringOrNull("satellite_name"),
        noradCatId = intOrNull("norad_cat_id"),
        status = stringOrNull("status"),
        transmitterDescription = stringOrNull("transmitter_description"),
        transmitterUuid = stringOrNull("transmitter_uuid"),
        observationFrequency = intOrNull("observation_frequency"),
        maxAltitude = doubleOrNull("max_altitude")
    )
}

private val TleEntry.updatedInstant: Instant?
    get() = updated?.let { runCatching { Instant.parse(it) }.getOrNull() }
