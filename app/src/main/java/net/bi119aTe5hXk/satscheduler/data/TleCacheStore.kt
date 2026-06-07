package net.bi119aTe5hXk.satscheduler.data

import android.content.Context
import net.bi119aTe5hXk.satscheduler.model.TleEntry
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

class TleCacheStore(context: Context) {
    private val prefs = context.getSharedPreferences("sat_scheduler_tle_cache", Context.MODE_PRIVATE)

    fun loadFresh(satelliteId: String, maxAge: Duration = Duration.ofHours(1)): TleEntry? {
        val cachedAt = prefs.getString(cacheTimeKey(satelliteId), null)
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: return null
        if (Duration.between(cachedAt, Instant.now()) > maxAge) return null

        val raw = prefs.getString(tleKey(satelliteId), null) ?: return null
        return runCatching { JSONObject(raw).toTleEntryCache() }.getOrNull()
    }

    suspend fun fetchLatest(api: SatnogsApi, satelliteId: String): TleEntry? {
        val cached = loadFresh(satelliteId)
        if (cached != null) return cached

        val fetched = api.fetchLatestTle(satelliteId)
        if (fetched != null) save(fetched)
        return fetched
    }

    fun save(tle: TleEntry) {
        prefs.edit()
            .putString(tleKey(tle.satId), tle.toJson().toString())
            .putString(cacheTimeKey(tle.satId), Instant.now().toString())
            .apply()
    }

    private fun tleKey(satelliteId: String): String = "tle_$satelliteId"

    private fun cacheTimeKey(satelliteId: String): String = "tle_${satelliteId}_cached_at"
}

private fun TleEntry.toJson(): JSONObject {
    return JSONObject()
        .put("tle0", tle0)
        .put("tle1", tle1)
        .put("tle2", tle2)
        .put("source", source)
        .put("satId", satId)
        .put("noradCatId", noradCatId)
        .put("updated", updated)
}

private fun JSONObject.toTleEntryCache(): TleEntry {
    return TleEntry(
        tle0 = stringOrNull("tle0"),
        tle1 = getString("tle1"),
        tle2 = getString("tle2"),
        source = stringOrNull("source"),
        satId = getString("satId"),
        noradCatId = intOrNull("noradCatId"),
        updated = stringOrNull("updated")
    )
}

private fun JSONObject.stringOrNull(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.intOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name) else null
