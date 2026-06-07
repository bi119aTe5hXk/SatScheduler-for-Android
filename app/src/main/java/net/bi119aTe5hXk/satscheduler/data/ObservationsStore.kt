package net.bi119aTe5hXk.satscheduler.data

import android.content.Context
import net.bi119aTe5hXk.satscheduler.model.Observation
import org.json.JSONArray

class ObservationsStore(context: Context) {
    private val prefs = context.getSharedPreferences("sat_scheduler_observations", Context.MODE_PRIVATE)

    fun loadUnknownObservations(observerId: String): List<Observation> {
        val raw = prefs.getString(key(observerId), "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.toObservationCache()
        }.sortedByDescending { it.start ?: "" }
    }

    fun saveUnknownObservations(observerId: String, observations: List<Observation>) {
        val array = JSONArray()
        observations
            .distinctBy { it.id }
            .sortedByDescending { it.start ?: "" }
            .forEach { array.put(it.toCacheJson()) }
        prefs.edit().putString(key(observerId), array.toString()).apply()
    }

    private fun key(observerId: String): String = "unknown_${observerId.trim()}"
}
