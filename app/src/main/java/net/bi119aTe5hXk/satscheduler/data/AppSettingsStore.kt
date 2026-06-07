package net.bi119aTe5hXk.satscheduler.data

import android.content.Context

data class AppSettings(
    val observerId: String = "",
    val timeDisplayMode: TimeDisplayMode = TimeDisplayMode.Local,
    val autoSchedulePreviewEnabled: Boolean = true,
    val autoScheduleSortOrder: AutoScheduleSortOrder = AutoScheduleSortOrder.WatchListOrder,
    val autoScheduleBatchSize: Int = 10
)

enum class TimeDisplayMode(val label: String) {
    Local("Local"),
    Utc("UTC")
}

enum class AutoScheduleSortOrder(val label: String) {
    WatchListOrder("Watch List order"),
    WatchListOrderThenPeakElevation("Watch List order + best elevation"),
    PeakElevationFirst("Best elevation first")
}

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("sat_scheduler_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        return AppSettings(
            observerId = prefs.getString(KEY_OBSERVER_ID, "") ?: "",
            timeDisplayMode = runCatching {
                TimeDisplayMode.valueOf(prefs.getString(KEY_TIME_MODE, TimeDisplayMode.Local.name) ?: TimeDisplayMode.Local.name)
            }.getOrDefault(TimeDisplayMode.Local),
            autoSchedulePreviewEnabled = prefs.getBoolean(KEY_PREVIEW_ENABLED, true),
            autoScheduleSortOrder = runCatching {
                AutoScheduleSortOrder.valueOf(
                    prefs.getString(KEY_SORT_ORDER, AutoScheduleSortOrder.WatchListOrder.name) ?: AutoScheduleSortOrder.WatchListOrder.name
                )
            }.getOrDefault(AutoScheduleSortOrder.WatchListOrder),
            autoScheduleBatchSize = prefs.getInt(KEY_BATCH_SIZE, 10).coerceIn(1, 50)
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_OBSERVER_ID, settings.observerId.trim())
            .putString(KEY_TIME_MODE, settings.timeDisplayMode.name)
            .putBoolean(KEY_PREVIEW_ENABLED, settings.autoSchedulePreviewEnabled)
            .putString(KEY_SORT_ORDER, settings.autoScheduleSortOrder.name)
            .putInt(KEY_BATCH_SIZE, settings.autoScheduleBatchSize.coerceIn(1, 50))
            .apply()
    }

    companion object {
        private const val KEY_OBSERVER_ID = "observer_id"
        private const val KEY_TIME_MODE = "time_display_mode"
        private const val KEY_PREVIEW_ENABLED = "auto_schedule_preview_enabled"
        private const val KEY_SORT_ORDER = "auto_schedule_sort_order"
        private const val KEY_BATCH_SIZE = "auto_schedule_batch_size"
    }
}
