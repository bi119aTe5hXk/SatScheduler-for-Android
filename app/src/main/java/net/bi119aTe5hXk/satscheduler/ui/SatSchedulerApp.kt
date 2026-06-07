package net.bi119aTe5hXk.satscheduler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.bi119aTe5hXk.satscheduler.data.ObservationScheduleRequest
import net.bi119aTe5hXk.satscheduler.data.ObservationPage
import net.bi119aTe5hXk.satscheduler.data.AppSettings
import net.bi119aTe5hXk.satscheduler.data.AppSettingsStore
import net.bi119aTe5hXk.satscheduler.data.AutoScheduleSortOrder
import net.bi119aTe5hXk.satscheduler.data.AutoScheduleCandidate
import net.bi119aTe5hXk.satscheduler.data.AutoSchedulePlan
import net.bi119aTe5hXk.satscheduler.data.AutoSchedulePlanner
import net.bi119aTe5hXk.satscheduler.data.PassPredictionEngine
import net.bi119aTe5hXk.satscheduler.data.ScheduleCandidateStatus
import net.bi119aTe5hXk.satscheduler.data.SatnogsApi
import net.bi119aTe5hXk.satscheduler.data.StationScheduleStore
import net.bi119aTe5hXk.satscheduler.data.TleCacheStore
import net.bi119aTe5hXk.satscheduler.data.TimeDisplayMode
import net.bi119aTe5hXk.satscheduler.data.WatchTargetStore
import net.bi119aTe5hXk.satscheduler.data.filterConflicts
import net.bi119aTe5hXk.satscheduler.data.newWatchTarget
import net.bi119aTe5hXk.satscheduler.data.parseSatnogsInstantOrNull
import net.bi119aTe5hXk.satscheduler.data.toScheduleRequest
import net.bi119aTe5hXk.satscheduler.model.GroundStation
import net.bi119aTe5hXk.satscheduler.model.Observation
import net.bi119aTe5hXk.satscheduler.model.PredictedPass
import net.bi119aTe5hXk.satscheduler.model.PredictionConflictSummary
import net.bi119aTe5hXk.satscheduler.model.Satellite
import net.bi119aTe5hXk.satscheduler.model.TleEntry
import net.bi119aTe5hXk.satscheduler.model.Transmitter
import net.bi119aTe5hXk.satscheduler.model.WatchTarget
import net.bi119aTe5hXk.satscheduler.model.WatchStationSnapshot
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.net.URL
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class AppTab(val title: String) {
    WatchList("Watch List"),
    Observations("Observations"),
    Timeline("Timeline"),
    Settings("Settings")
}

private val AppTab.icon: ImageVector
    get() = when (this) {
        AppTab.WatchList -> Icons.Default.Star
        AppTab.Observations -> Icons.Default.GraphicEq
        AppTab.Timeline -> Icons.Default.Timeline
        AppTab.Settings -> Icons.Default.Settings
    }

@Composable
fun SatSchedulerApp() {
    val context = LocalContext.current
    val store = remember { WatchTargetStore(context) }
    var token by rememberSaveable { mutableStateOf("") }
    var targets by remember { mutableStateOf(emptyList<WatchTarget>()) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.WatchList) }
    val api = remember(token) { SatnogsApi { token } }

    LaunchedEffect(Unit) {
        token = store.loadToken()
        targets = store.loadTargets()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.WatchList -> WatchListScreen(
                api = api,
                targets = targets,
                onTargetsChanged = { updated ->
                    targets = updated
                    store.saveTargets(updated)
                },
                modifier = Modifier.padding(padding)
            )

            AppTab.Observations -> ObservationsScreen(api = api, modifier = Modifier.padding(padding))
            AppTab.Timeline -> TimelineScreen(api = api, targets = targets, modifier = Modifier.padding(padding))
            AppTab.Settings -> SettingsScreen(
                token = token,
                targets = targets,
                onSaveToken = { newToken ->
                    val trimmedToken = newToken.trim()
                    token = trimmedToken
                    store.saveToken(trimmedToken)
                },
                onTargetsChanged = { updated ->
                    targets = updated
                    store.saveTargets(updated)
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun WatchListScreen(
    api: SatnogsApi,
    targets: List<WatchTarget>,
    onTargetsChanged: (List<WatchTarget>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showingAddDialog by rememberSaveable { mutableStateOf(false) }
    var predictionTarget by remember { mutableStateOf<WatchTarget?>(null) }
    var showingBatchSchedule by rememberSaveable { mutableStateOf(false) }

    if (showingBatchSchedule) {
        BatchScheduleScreen(
            api = api,
            targets = targets,
            onBack = { showingBatchSchedule = false },
            modifier = modifier
        )
        return
    }

    Column(modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Watch List", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { showingBatchSchedule = true }, enabled = targets.isNotEmpty()) {
                    Icon(Icons.Default.Sync, contentDescription = "Auto schedule")
                }
                IconButton(onClick = { showingAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add watch target")
                }
            }
        }
        if (targets.isEmpty()) {
            EmptyState("Add a satellite from the SatNOGS alive list, choose a transmitter and one or more online stations.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(targets, key = { it.id }) { target ->
                    WatchTargetCard(
                        target = target,
                        onPredict = { predictionTarget = target },
                        onDelete = { onTargetsChanged(targets.filterNot { it.id == target.id }) }
                    )
                }
            }
        }
    }

    if (showingAddDialog) {
        AddTargetDialog(
            api = api,
            onDismiss = { showingAddDialog = false },
            onAdd = { target ->
                onTargetsChanged(targets + target)
                showingAddDialog = false
            }
        )
    }
    predictionTarget?.let { target ->
        PredictionDialog(
            api = api,
            target = target,
            onDismiss = { predictionTarget = null }
        )
    }
}

@Composable
private fun AddTargetDialog(api: SatnogsApi, onDismiss: () -> Unit, onAdd: (WatchTarget) -> Unit) {
    val scope = rememberCoroutineScope()
    var satellites by remember { mutableStateOf<List<Satellite>>(emptyList()) }
    var stations by remember { mutableStateOf<List<GroundStation>>(emptyList()) }
    var transmitters by remember { mutableStateOf<List<Transmitter>>(emptyList()) }
    var selectedSatellite by remember { mutableStateOf<Satellite?>(null) }
    var selectedTransmitterId by rememberSaveable { mutableStateOf<String?>(null) }
    var recommendedTransmitterId by rememberSaveable { mutableStateOf<String?>(null) }
    var recommendedCount by rememberSaveable { mutableStateOf(0) }
    var selectedStationIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var satelliteQuery by rememberSaveable { mutableStateOf("") }
    var stationQuery by rememberSaveable { mutableStateOf("") }
    var centerFrequencyMHz by rememberSaveable { mutableStateOf("") }
    var requireDaylight by rememberSaveable { mutableStateOf(false) }
    var minPeakElevation by rememberSaveable { mutableStateOf("") }
    var maxPeakElevation by rememberSaveable { mutableStateOf("") }
    var minAzimuth by rememberSaveable { mutableStateOf("") }
    var maxAzimuth by rememberSaveable { mutableStateOf("") }
    var loadingSatellites by rememberSaveable { mutableStateOf(false) }
    var loadingStations by rememberSaveable { mutableStateOf(false) }
    var loadingTransmitters by rememberSaveable { mutableStateOf(false) }
    var loadingRecommendation by rememberSaveable { mutableStateOf(false) }
    var transmitterMenuOpen by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }

    val filteredSatellites by remember(satellites, satelliteQuery) {
        derivedStateOf { satellites.filterBySatelliteQuery(satelliteQuery).take(100) }
    }
    val filteredStations by remember(stations, stationQuery) {
        derivedStateOf { stations.filterByStationQuery(stationQuery) }
    }
    val selectedTransmitter = transmitters.firstOrNull { it.uuid == selectedTransmitterId }
    val centerFrequencyHz = centerFrequencyMHz.toDoubleOrNull()?.takeIf { it > 0 }?.let { (it * 1_000_000).toInt() }
    val peakError = peakElevationError(minPeakElevation, maxPeakElevation)
    val azimuthError = azimuthError(minAzimuth, maxAzimuth)
    val canAdd = selectedSatellite != null &&
        selectedTransmitter != null &&
        selectedStationIds.isNotEmpty() &&
        centerFrequencyHz != null &&
        peakError == null &&
        azimuthError == null

    LaunchedEffect(Unit) {
        loadingSatellites = true
        loadingStations = true
        runCatching { api.fetchAliveSatellites() }
            .onSuccess { satellites = it }
            .onFailure { message = it.message ?: "Failed to load satellites" }
        loadingSatellites = false
        runCatching { api.fetchOnlineStations() }
            .onSuccess { stations = it }
            .onFailure { message = it.message ?: "Failed to load stations" }
        loadingStations = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Watch Target") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SectionTitle("Satellite") }
                item {
                    OutlinedTextField(
                        value = satelliteQuery,
                        onValueChange = { satelliteQuery = it },
                        label = { Text("Search by name, alias, SatNOGS ID, or NORAD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (loadingSatellites) {
                    item { LoadingRow("Loading alive satellites...") }
                } else if (filteredSatellites.isEmpty()) {
                    item { Text("No satellites found.", color = MaterialTheme.colorScheme.secondary) }
                } else {
                    items(filteredSatellites, key = { it.satId }) { satellite ->
                        SelectableSatelliteRow(
                            satellite = satellite,
                            selected = selectedSatellite?.satId == satellite.satId,
                            onClick = {
                                selectedSatellite = satellite
                                satelliteQuery = satellite.displayName
                                transmitters = emptyList()
                                selectedTransmitterId = null
                                recommendedTransmitterId = null
                                recommendedCount = 0
                                centerFrequencyMHz = ""
                                scope.launch {
                                    loadingTransmitters = true
                                    message = ""
                                    runCatching { api.fetchTransmitters(satellite.satId) }
                                        .onSuccess { loaded ->
                                            transmitters = loaded
                                            if (loaded.size == 1) {
                                                val only = loaded.first()
                                                recommendedTransmitterId = only.uuid
                                                selectedTransmitterId = only.uuid
                                                centerFrequencyMHz = only.defaultCenterFrequencyHz?.toMHzText().orEmpty()
                                            } else if (loaded.isNotEmpty()) {
                                                loadingRecommendation = true
                                                runCatching {
                                                    satellite.noradCatId?.let { api.fetchGoodObservations(it) } ?: emptyList()
                                                }
                                                    .onSuccess { observations ->
                                                        val best = recommendedTransmitter(loaded, observations)
                                                        recommendedTransmitterId = best?.first
                                                        recommendedCount = best?.second ?: 0
                                                        if (best != null) {
                                                            selectedTransmitterId = best.first
                                                            centerFrequencyMHz = loaded.firstOrNull { it.uuid == best.first }
                                                                ?.defaultCenterFrequencyHz
                                                                ?.toMHzText()
                                                                .orEmpty()
                                                        }
                                                    }
                                                    .onFailure { message = it.message ?: "Failed to find transmitter recommendation" }
                                                loadingRecommendation = false
                                            }
                                        }
                                        .onFailure { message = it.message ?: "Failed to load transmitters" }
                                    loadingTransmitters = false
                                }
                            }
                        )
                    }
                }

                item { SectionTitle("Transmitter") }
                item {
                    when {
                        selectedSatellite == null -> Text("Select a satellite first.", color = MaterialTheme.colorScheme.secondary)
                        loadingTransmitters -> LoadingRow("Loading transmitters...")
                        transmitters.isEmpty() -> Text("No transmitters found.", color = MaterialTheme.colorScheme.secondary)
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { transmitterMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(selectedTransmitter?.let { transmitterLabel(it, recommendedTransmitterId) } ?: "Select transmitter")
                                }
                                DropdownMenu(expanded = transmitterMenuOpen, onDismissRequest = { transmitterMenuOpen = false }) {
                                    transmitters.forEach { transmitter ->
                                        DropdownMenuItem(
                                            text = { Text(transmitterLabel(transmitter, recommendedTransmitterId)) },
                                            onClick = {
                                                selectedTransmitterId = transmitter.uuid
                                                centerFrequencyMHz = transmitter.defaultCenterFrequencyHz?.toMHzText().orEmpty()
                                                transmitterMenuOpen = false
                                            }
                                        )
                                    }
                                }
                                if (loadingRecommendation) {
                                    LoadingRow("Finding recommended transmitter...")
                                } else {
                                    recommendedTransmitterId?.let { id ->
                                        val recommended = transmitters.firstOrNull { it.uuid == id }
                                        Text(
                                            "Recommended: ${recommended?.displayName ?: id} based on $recommendedCount good observation(s).",
                                            color = MaterialTheme.colorScheme.secondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                selectedTransmitter?.let { transmitter ->
                                    Text("Downlink: ${transmitter.frequencyText}", style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(
                                        value = centerFrequencyMHz,
                                        onValueChange = { centerFrequencyMHz = it },
                                        label = { Text("Center frequency MHz") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (transmitter.requiresManualCenterFrequency) {
                                        Text("This transmitter has a frequency range; confirm the center frequency used for scheduling.", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                item { SectionTitle("Ground Stations") }
                item {
                    OutlinedTextField(
                        value = stationQuery,
                        onValueChange = { stationQuery = it },
                        label = { Text("Search online stations") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (loadingStations) {
                    item { LoadingRow("Loading online stations...") }
                } else if (filteredStations.isEmpty()) {
                    item { Text("No ground stations found.", color = MaterialTheme.colorScheme.secondary) }
                } else {
                    items(filteredStations, key = { it.id }) { station ->
                        SelectableStationRow(
                            station = station,
                            selected = selectedStationIds.contains(station.id),
                            onToggle = {
                                selectedStationIds = if (selectedStationIds.contains(station.id)) {
                                    selectedStationIds - station.id
                                } else {
                                    selectedStationIds + station.id
                                }
                            }
                        )
                    }
                }

                item { SectionTitle("Scheduling Options") }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Require station daylight")
                        Switch(checked = requireDaylight, onCheckedChange = { requireDaylight = it })
                    }
                }
                item {
                    RangeFields(
                        title = "Peak elevation range",
                        minValue = minPeakElevation,
                        maxValue = maxPeakElevation,
                        minLabel = "Min deg",
                        maxLabel = "Max deg",
                        onMinChange = { minPeakElevation = it },
                        onMaxChange = { maxPeakElevation = it },
                        error = peakError
                    )
                }
                item {
                    RangeFields(
                        title = "Azimuth range",
                        minValue = minAzimuth,
                        maxValue = maxAzimuth,
                        minLabel = "Min deg",
                        maxLabel = "Max deg",
                        onMinChange = { minAzimuth = it },
                        onMaxChange = { maxAzimuth = it },
                        error = azimuthError
                    )
                }
                if (message.isNotBlank()) {
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val satellite = selectedSatellite ?: return@Button
                    val transmitter = selectedTransmitter ?: return@Button
                    val selectedStations = stations.filter { selectedStationIds.contains(it.id) }
                    onAdd(
                        newWatchTarget(
                            name = satellite.displayName,
                            satelliteId = satellite.satId,
                            satelliteName = satellite.displayName,
                            transmitterId = transmitter.uuid,
                            transmitterDescription = transmitter.displayName,
                            centerFrequency = centerFrequencyHz,
                            stationIds = selectedStationIds.sorted(),
                            stationNames = selectedStations.associate { it.id to it.displayName },
                            stationSnapshots = selectedStations.associate { station ->
                                station.id to WatchStationSnapshot(
                                    id = station.id,
                                    name = station.displayName,
                                    latitude = station.lat,
                                    longitude = station.lng,
                                    altitude = station.altitude,
                                    minHorizon = station.minHorizon
                                )
                            },
                            requireStationDaylight = requireDaylight,
                            minPeakElevation = parsePeakElevation(minPeakElevation),
                            maxPeakElevation = parsePeakElevation(maxPeakElevation),
                            minAzimuth = parseAzimuth(minAzimuth),
                            maxAzimuth = parseAzimuth(maxAzimuth)
                        )
                    )
                },
                enabled = canAdd
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SelectableSatelliteRow(satellite: Satellite, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(satellite.displayName, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(
                    "ID: ${satellite.satId}",
                    satellite.noradCatId?.let { "NORAD: $it" },
                    satellite.status?.let { "Status: $it" }
                ).joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            satellite.names?.takeIf { it != satellite.displayName }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Checkbox(checked = selected, onCheckedChange = { onClick() })
    }
}

@Composable
private fun SelectableStationRow(station: GroundStation, selected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = selected, onValueChange = { onToggle() }).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(station.displayName, fontWeight = FontWeight.SemiBold)
            Text("ID: ${station.id} / ${station.status ?: "-"}", style = MaterialTheme.typography.bodySmall)
            Text(
                listOfNotNull(
                    station.antennaText,
                    station.altitude?.let { "Alt: ${it.toInt()} m" },
                    station.successRate?.let { "Success: $it%" },
                    station.futureObservations?.let { "Future: $it" }
                ).joinToString(" / ").ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun RangeFields(
    title: String,
    minValue: String,
    maxValue: String,
    minLabel: String,
    maxLabel: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    error: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = minValue, onValueChange = onMinChange, label = { Text(minLabel) }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = maxValue, onValueChange = onMaxChange, label = { Text(maxLabel) }, modifier = Modifier.weight(1f))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun LoadingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
        Text(text, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun WatchTargetCard(target: WatchTarget, onPredict: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(target.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(target.satelliteName ?: target.satelliteId, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            Text("Transmitter: ${target.transmitterDescription ?: target.transmitterId}")
            target.centerFrequency?.let { Text("Center frequency: ${it.toMHzText()} MHz") }
            Text("Stations: ${target.stationIds.joinToString(", ") { id -> target.stationNames[id] ?: id.toString() }}")
            val options = listOfNotNull(
                if (target.requireStationDaylight) "daylight only" else null,
                rangeText("peak", target.minPeakElevation, target.maxPeakElevation),
                rangeText("azimuth", target.minAzimuth, target.maxAzimuth)
            )
            if (options.isNotEmpty()) Text("Options: ${options.joinToString(" / ")}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPredict) { Text("Predict") }
            }
        }
    }
}

@Composable
private fun PredictionDialog(api: SatnogsApi, target: WatchTarget, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scheduleStore = remember { StationScheduleStore(context) }
    val tleCacheStore = remember { TleCacheStore(context) }
    val scope = rememberCoroutineScope()
    var loading by rememberSaveable { mutableStateOf(true) }
    var loadingText by rememberSaveable { mutableStateOf("Fetching TLE data...") }
    var tle by remember { mutableStateOf<TleEntry?>(null) }
    var existingCount by rememberSaveable { mutableStateOf<Int?>(null) }
    var predictedPasses by remember { mutableStateOf<List<PredictedPass>>(emptyList()) }
    var conflictSummary by remember { mutableStateOf<PredictionConflictSummary?>(null) }
    var predictedRequests by remember { mutableStateOf<List<ObservationScheduleRequest>>(emptyList()) }
    var message by rememberSaveable { mutableStateOf("") }
    var scheduling by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(target.id) {
        loading = true
        message = ""
        runCatching { tleCacheStore.fetchLatest(api, target.satelliteId) }
            .onSuccess { fetchedTle ->
                tle = fetchedTle
                if (fetchedTle == null) {
                    message = "TLE not found for ${target.satelliteId}."
                }
            }
            .onFailure { message = it.message ?: "Failed to fetch TLE" }

        if (tle != null) {
            loadingText = "Calculating pass windows locally..."
            val rawPasses = runCatching {
                PassPredictionEngine().predict(target, tle!!)
            }.onFailure {
                message = it.message ?: "Failed to calculate pass windows"
            }.getOrDefault(emptyList())

            loadingText = "Checking future station schedules..."
            val existingObservations = runCatching {
                scheduleStore.refresh(api, target.stationIds)
            }.getOrElse {
                message = "Failed to fetch full station schedule, using cache: ${it.message ?: "unknown error"}"
                scheduleStore.load(target.stationIds)
            }
            val conflictResult = filterConflicts(rawPasses, existingObservations)
            existingCount = existingObservations.size
            predictedPasses = conflictResult.visiblePasses
            conflictSummary = conflictResult.summary
            predictedRequests = predictedPasses.map { it.toScheduleRequest(target) }
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prediction Timeline") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Satellite: ${target.satelliteName ?: target.satelliteId}")
                Text("Transmitter: ${target.transmitterDescription ?: target.transmitterId}")
                Text("Stations: ${target.stationIds.joinToString(", ") { target.stationNames[it] ?: it.toString() }}")
                HorizontalDivider()
                if (loading) {
                    LoadingRow(loadingText)
                } else {
                    tle?.let { entry ->
                        Text("TLE: ${entry.displayName}")
                        Text("Updated: ${entry.updated ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        Text(entry.tle1, style = MaterialTheme.typography.bodySmall)
                        Text(entry.tle2, style = MaterialTheme.typography.bodySmall)
                    }
                    existingCount?.let {
                        Text("Existing future observations on selected stations: $it")
                    }
                    conflictSummary?.let {
                        Text(
                            "Predicted: ${it.predictedCount}, visible: ${it.visibleCount}, hidden: ${it.hiddenCount}",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (predictedPasses.isEmpty()) {
                        Text("No predicted windows are available yet.", color = MaterialTheme.colorScheme.secondary)
                    } else {
                        PassTimeline(
                            items = predictedPasses.map {
                                TimelineItem(
                                    stationId = it.stationId,
                                    stationName = it.stationName,
                                    label = "Peak ${"%.1f".format(it.maxElevation)} deg",
                                    start = it.startInstant,
                                    end = it.endInstant,
                                    color = Color(0xFF2F6FED)
                                )
                            },
                            start = Instant.now(),
                            end = Instant.now().plus(Duration.ofDays(2))
                        )
                        LazyColumn(Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(predictedPasses, key = { "${it.stationId}-${it.start}" }) { pass ->
                                PredictedPassRow(pass)
                            }
                        }
                    }
                }
                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        scheduling = true
                        runCatching { api.createObservations(predictedRequests) }
                            .onSuccess { message = "Scheduled ${it.size} observation(s)." }
                            .onFailure { message = "Schedule failed: ${it.message ?: "unknown error"}" }
                        scheduling = false
                    }
                },
                enabled = predictedRequests.isNotEmpty() && !scheduling
            ) { Text(if (scheduling) "Scheduling..." else "Schedule Predicted Windows") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun PredictedPassRow(pass: PredictedPass) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${pass.stationName} (${pass.stationId})", fontWeight = FontWeight.SemiBold)
            Text("${pass.start} -> ${pass.end}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Peak: ${"%.1f".format(pass.maxElevation)} deg / Az: ${"%.0f".format(pass.azimuthStart)}-${"%.0f".format(pass.azimuthEnd)} deg",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BatchScheduleScreen(
    api: SatnogsApi,
    targets: List<WatchTarget>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val scheduleStore = remember { StationScheduleStore(context) }
    val tleCacheStore = remember { TleCacheStore(context) }
    val planner = remember { AutoSchedulePlanner() }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(settingsStore.load()) }
    var plan by remember { mutableStateOf<AutoSchedulePlan?>(null) }
    var candidates by remember { mutableStateOf<List<AutoScheduleCandidate>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var executing by rememberSaveable { mutableStateOf(false) }
    var executedOnce by rememberSaveable { mutableStateOf(false) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var progressText by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var sortMenuOpen by rememberSaveable { mutableStateOf(false) }

    fun rebuild(priorityMode: AutoScheduleSortOrder = settings.autoScheduleSortOrder) {
        scope.launch {
            loading = true
            message = ""
            progressText = "Calculating candidates..."
            val newPlan = planner.buildPlan(
                api = api,
                tleCacheStore = tleCacheStore,
                scheduleStore = scheduleStore,
                targets = targets,
                priorityMode = priorityMode
            )
            plan = newPlan
            candidates = newPlan.selectedCandidates
            progressText = ""
            loading = false
            if (!settings.autoSchedulePreviewEnabled) {
                executeBatch(
                    api = api,
                    scheduleStore = scheduleStore,
                    settings = settings,
                    candidates = candidates,
                    retryFailedOnly = false,
                    onCandidates = { candidates = it },
                    onExecuting = { executing = it },
                    onExecutedOnce = { executedOnce = it },
                    onProgress = { progressText = it },
                    onMessage = { message = it }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        rebuild()
    }

    val failedCount = candidates.count { it.status == ScheduleCandidateStatus.Failed }
    val pendingCount = candidates.count { it.status == ScheduleCandidateStatus.Pending }
    val canExecute = !loading && !executing && candidates.isNotEmpty() && (!executedOnce || failedCount > 0)
    val timelineItems = (plan?.existingObservations ?: emptyList()).mapNotNull { observation ->
        val stationId = observation.groundStation ?: return@mapNotNull null
        val start = parseSatnogsInstantOrNull(observation.start) ?: return@mapNotNull null
        val end = parseSatnogsInstantOrNull(observation.end) ?: return@mapNotNull null
        TimelineItem(
            stationId = stationId,
            stationName = observation.stationName ?: "Station $stationId",
            label = observation.title,
            start = start,
            end = end,
            color = Color(0xFF2F6FED)
        )
    } + candidates.map { candidate ->
        TimelineItem(
            stationId = candidate.pass.stationId,
            stationName = candidate.pass.stationName,
            label = candidate.target.name,
            start = candidate.pass.startInstant,
            end = candidate.pass.endInstant,
            color = when (candidate.status) {
                ScheduleCandidateStatus.Pending -> Color(0xFFE6A700)
                ScheduleCandidateStatus.Scheduled -> Color(0xFF1F9D6E)
                ScheduleCandidateStatus.Failed -> Color(0xFFD84A4A)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (executing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(progressText, color = MaterialTheme.colorScheme.secondary)
                }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canExecute && pendingCount + failedCount > 0,
                onClick = {
                    scope.launch {
                        executeBatch(
                            api = api,
                            scheduleStore = scheduleStore,
                            settings = settings,
                            candidates = candidates,
                            retryFailedOnly = executedOnce && failedCount > 0,
                            onCandidates = { candidates = it },
                            onExecuting = { executing = it },
                            onExecutedOnce = { executedOnce = it },
                            onProgress = { progressText = it },
                            onMessage = { message = it }
                        )
                    }
                }
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        executing -> "Scheduling..."
                        executedOnce && failedCount > 0 -> "Retry failed"
                        executedOnce -> "Scheduled"
                        else -> "Schedule all"
                    }
                )
            }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, enabled = !executing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Auto Schedule Preview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { sortMenuOpen = true }, enabled = !executing) {
                        Icon(Icons.Default.Sync, contentDescription = "Sort")
                    }
                    IconButton(onClick = { editMode = !editMode }, enabled = !executing) {
                        Icon(Icons.Default.Edit, contentDescription = if (editMode) "Done editing" else "Edit")
                    }
                    IconButton(onClick = { rebuild() }, enabled = !loading && !executing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
            Text("Priority: ${settings.autoScheduleSortOrder.label}", color = MaterialTheme.colorScheme.secondary)
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                AutoScheduleSortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.label) },
                        onClick = {
                            settings = settings.copy(autoScheduleSortOrder = order)
                            settingsStore.save(settings)
                            sortMenuOpen = false
                            val currentPlan = plan
                            if (currentPlan != null) {
                                val resorted = planner.sortCandidates(candidates, order)
                                candidates = resorted
                                plan = currentPlan.copy(priorityMode = order, selectedCandidates = resorted)
                            }
                        }
                    )
                }
            }
            if (loading) {
                LoadingRow(progressText.ifBlank { "Calculating auto schedule plan..." })
            }
            plan?.let { currentPlan ->
                Text(
                    "Satellites: ${currentPlan.satelliteCount}, calculated: ${currentPlan.candidateCount}, scheduled: ${candidates.count { it.status == ScheduleCandidateStatus.Scheduled }}, selected: ${candidates.size}, skipped: ${currentPlan.skippedCount}"
                )
                Text(
                    "Range: ${formatShortDateTime(currentPlan.start)} - ${formatShortDateTime(currentPlan.end)}",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (timelineItems.isNotEmpty()) {
                PassTimeline(
                    items = timelineItems,
                    start = plan?.start ?: Instant.now(),
                    end = plan?.end ?: Instant.now().plus(Duration.ofDays(2))
                )
            }
            if (message.isNotBlank()) {
                Text(message, color = if (failedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(candidates, key = { it.id }) { candidate ->
                    AutoScheduleCandidateRow(
                        candidate = candidate,
                        editMode = editMode && !executing,
                        canDelete = !executedOnce,
                        onDelete = { candidates = candidates.filterNot { it.id == candidate.id } },
                        onMoveUp = {
                            val index = candidates.indexOfFirst { it.id == candidate.id }
                            if (index > 0) candidates = candidates.toMutableList().also {
                                val item = it.removeAt(index)
                                it.add(index - 1, item)
                            }
                        },
                        onMoveDown = {
                            val index = candidates.indexOfFirst { it.id == candidate.id }
                            if (index >= 0 && index < candidates.lastIndex) candidates = candidates.toMutableList().also {
                                val item = it.removeAt(index)
                                it.add(index + 1, item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoScheduleCandidateRow(
    candidate: AutoScheduleCandidate,
    editMode: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(candidate.target.name, fontWeight = FontWeight.SemiBold)
                    Text("${candidate.pass.stationName} (${candidate.pass.stationId})", style = MaterialTheme.typography.bodySmall)
                }
                Text(candidate.status.name)
            }
            Text("${candidate.pass.start} -> ${candidate.pass.end}", style = MaterialTheme.typography.bodySmall)
            Text("Peak: ${"%.1f".format(candidate.pass.maxElevation)} deg / Az: ${"%.0f".format(candidate.pass.azimuthStart)}-${"%.0f".format(candidate.pass.azimuthEnd)} deg", style = MaterialTheme.typography.bodySmall)
            candidate.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (editMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun executeBatch(
    api: SatnogsApi,
    scheduleStore: StationScheduleStore,
    settings: AppSettings,
    candidates: List<AutoScheduleCandidate>,
    retryFailedOnly: Boolean,
    onCandidates: (List<AutoScheduleCandidate>) -> Unit,
    onExecuting: (Boolean) -> Unit,
    onExecutedOnce: (Boolean) -> Unit,
    onProgress: (String) -> Unit,
    onMessage: (String) -> Unit
) {
    onExecuting(true)
    var current = candidates
    val targetStatuses = if (retryFailedOnly) setOf(ScheduleCandidateStatus.Failed) else setOf(ScheduleCandidateStatus.Pending)
    val targetIds = current.filter { targetStatuses.contains(it.status) }.map { it.id }.toSet()
    val pending = current.filter { targetIds.contains(it.id) }
    if (pending.isEmpty()) {
        onMessage("No requests to schedule.")
        onExecuting(false)
        return
    }
    val batchSize = settings.autoScheduleBatchSize.coerceIn(1, 50)
    var completed = 0
    val createdObservations = mutableListOf<Observation>()

    pending.chunked(batchSize).forEach { batch ->
        onProgress("Scheduling ${completed + 1}-${completed + batch.size} / ${pending.size}...")
        runCatching { api.createObservations(batch.map { it.request }) }
            .onSuccess { observations ->
                createdObservations += observations
                val batchIds = batch.map { it.id }.toSet()
                current = current.map {
                    if (batchIds.contains(it.id)) it.copy(status = ScheduleCandidateStatus.Scheduled, errorMessage = null) else it
                }
            }
            .onFailure { error ->
                val batchIds = batch.map { it.id }.toSet()
                current = current.map {
                    if (batchIds.contains(it.id)) it.copy(status = ScheduleCandidateStatus.Failed, errorMessage = error.message ?: "Schedule failed") else it
                }
            }
        completed += batch.size
        onCandidates(current)
    }

    if (createdObservations.isNotEmpty()) {
        scheduleStore.mergeCreatedObservations(createdObservations)
    }
    val failedCount = current.count { it.status == ScheduleCandidateStatus.Failed }
    onExecutedOnce(true)
    onExecuting(false)
    onProgress("")
    onMessage(
        if (failedCount > 0) {
            "Scheduled ${createdObservations.size} observation(s), failed $failedCount request(s)."
        } else {
            "Scheduled ${createdObservations.size} observation(s)."
        }
    )
}

private data class TimelineItem(
    val stationId: Int,
    val stationName: String,
    val label: String,
    val start: Instant,
    val end: Instant,
    val color: Color
)

@Composable
private fun PassTimeline(
    items: List<TimelineItem>,
    start: Instant,
    end: Instant,
    stationNames: Map<Int, String> = emptyMap()
) {
    val totalMillis = Duration.between(start, end).toMillis().coerceAtLeast(1)
    val stations = items
        .groupBy { it.stationId }
        .mapValues { entry -> entry.value.sortedBy { it.start } }
    val stationIds = (stations.keys + stationNames.keys).sorted()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Station", modifier = Modifier.width(112.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatShortDateTime(start), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(formatShortDateTime(end), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        stationIds.forEach { stationId ->
            val stationItems = stations[stationId].orEmpty()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stationNames[stationId] ?: stationItems.firstOrNull()?.stationName ?: "Station $stationId",
                    modifier = Modifier.width(112.dp),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall
                )
                Canvas(Modifier.weight(1f).height(34.dp)) {
                    drawRoundRect(
                        color = Color(0x1A808080),
                        topLeft = Offset(0f, size.height / 2f - 10f),
                        size = Size(size.width, 20f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    val now = Instant.now()
                    if (!now.isBefore(start) && !now.isAfter(end)) {
                        val x = Duration.between(start, now).toMillis().toFloat() / totalMillis.toFloat() * size.width
                        drawRect(
                            color = Color(0xFF2A9D55),
                            topLeft = Offset(x, 0f),
                            size = Size(2f, size.height)
                        )
                    }
                    stationItems.forEach { item ->
                        val leftRatio = Duration.between(start, item.start).toMillis().toFloat() / totalMillis.toFloat()
                        val rightRatio = Duration.between(start, item.end).toMillis().toFloat() / totalMillis.toFloat()
                        val left = (leftRatio.coerceIn(0f, 1f)) * size.width
                        val right = (rightRatio.coerceIn(0f, 1f)) * size.width
                        val width = (right - left).coerceAtLeast(3f)
                        drawRoundRect(
                            color = item.color.copy(alpha = 0.78f),
                            topLeft = Offset(left, size.height / 2f - 9f),
                            size = Size(width, 18f),
                            cornerRadius = CornerRadius(7f, 7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservationsScreen(api: SatnogsApi, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(AppSettings()) }
    var observerIdDraft by rememberSaveable { mutableStateOf("") }
    var showingObserverIdDialog by rememberSaveable { mutableStateOf(false) }
    var detailObservation by remember { mutableStateOf<Observation?>(null) }
    var observations by remember { mutableStateOf<List<Observation>>(emptyList()) }
    var nextCursor by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var loadingNextPage by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    val savedObserverId = settings.observerId.trim()

    LaunchedEffect(Unit) {
        settings = settingsStore.load()
    }

    if (showingObserverIdDialog) {
        ObserverIdDialog(
            observerId = observerIdDraft,
            onObserverIdChange = { observerIdDraft = it },
            onDismiss = { showingObserverIdDialog = false },
            onSave = {
                val updated = settings.copy(observerId = observerIdDraft.trim())
                settingsStore.save(updated)
                settings = updated
                showingObserverIdDialog = false
                error = ""
            }
        )
    }

    detailObservation?.let { observation ->
        ObservationDetailScreen(
            observation = observation,
            onBack = { detailObservation = null },
            modifier = modifier
        )
        return
    }

    fun loadFirstPage() {
        scope.launch {
            if (savedObserverId.isBlank()) {
                observerIdDraft = ""
                showingObserverIdDialog = true
                return@launch
            }
            loading = true
            error = ""
            runCatching {
                api.fetchUnknownObservationsPage(observerId = savedObserverId)
            }
                .onSuccess { page ->
                    observations = page.results
                    nextCursor = page.nextCursor
                }
                .onFailure { error = it.message ?: "Failed to fetch observations" }
            loading = false
        }
    }

    fun loadNextPage() {
        val cursor = nextCursor ?: return
        if (loading || loadingNextPage || savedObserverId.isBlank()) return
        scope.launch {
            loadingNextPage = true
            runCatching {
                api.fetchUnknownObservationsPage(observerId = savedObserverId, cursor = cursor)
            }
                .onSuccess { page ->
                    val existingIds = observations.map { it.id }.toSet()
                    observations = observations + page.results.filterNot { existingIds.contains(it.id) }
                    nextCursor = page.nextCursor
                }
                .onFailure { error = it.message ?: "Failed to load more observations" }
            loadingNextPage = false
        }
    }

    Column(modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Observations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { loadFirstPage() },
                enabled = !loading && !loadingNextPage
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh observations")
            }
        }
        if (savedObserverId.isBlank()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Observer ID is not set.", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Set your SatNOGS observer ID to load observations that need rating.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Button(
                        onClick = {
                            observerIdDraft = ""
                            showingObserverIdDialog = true
                        }
                    ) { Text("Set Observer ID") }
                }
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (observations.isEmpty() && !loading) {
            EmptyState(
                if (savedObserverId.isBlank()) {
                    "Set an Observer ID before loading observations."
                } else {
                    "No unrated observations for this observer."
                }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(observations, key = { it.id }) { observation ->
                    if (nextCursor != null && observations.indexOf(observation) >= observations.size - 5) {
                        LaunchedEffect(observation.id, nextCursor) {
                            loadNextPage()
                        }
                    }
                    ObservationCard(
                        observation = observation,
                        onClick = { detailObservation = observation }
                    )
                }
                if (loadingNextPage) {
                    item { LoadingRow("Loading more observations...") }
                }
            }
        }
    }
}

@Composable
private fun ObserverIdDialog(
    observerId: String,
    onObserverIdChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Observer ID") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = observerId,
                    onValueChange = onObserverIdChange,
                    label = { Text("Observer ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "To find your Observer ID, open SatNOGS Network in a browser and check the message in the top-right corner, such as '<number> observations needs rating.' For your own account, the page URL usually includes observer=XXXX. To find another user's ID, select that username in the Observer filter, click Search, then check observer=XXXX in the updated URL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = observerId.trim().toIntOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ObservationCard(observation: Observation, onClick: (() -> Unit)? = null) {
    Card(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(observation.title, fontWeight = FontWeight.SemiBold)
                Text(observation.status ?: "unknown")
            }
            Text(observation.stationDisplayName)
            Text("${observation.start ?: "-"} -> ${observation.end ?: "-"}")
            Text("Frequency: ${observation.frequencyText}   Max alt: ${observation.maxAltitudeText}")
            observation.transmitterDescription?.let { Text(it) }
        }
    }
}

private enum class ObservationDetailTab(val title: String) {
    Info("Info"),
    Waterfall("Waterfall"),
    Audio("Audio")
}

@Composable
private fun ObservationDetailScreen(
    observation: Observation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(ObservationDetailTab.Info) }

    Column(modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(observation.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        PrimaryTabRow(selectedTabIndex = ObservationDetailTab.entries.indexOf(selectedTab)) {
            ObservationDetailTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.title) }
                )
            }
        }
        when (selectedTab) {
            ObservationDetailTab.Info -> ObservationInfoTab(observation)
            ObservationDetailTab.Waterfall -> ObservationWaterfallTab(observation)
            ObservationDetailTab.Audio -> ObservationAudioTab(observation)
        }
    }
}

@Composable
private fun ObservationInfoTab(observation: Observation) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            InfoCard("Observation") {
                InfoRow("ID", observation.id.toString())
                InfoRow("Status", observation.status ?: "unknown")
                InfoRow("Vetted Status", observation.vettedStatusDisplayText)
                InfoRow("Start", observation.start ?: "-")
                InfoRow("End", observation.end ?: "-")
                observation.riseAzimuth?.let { InfoRow("Rise Azimuth", formatDegrees(it)) }
                observation.setAzimuth?.let { InfoRow("Set Azimuth", formatDegrees(it)) }
                observation.maxAltitude?.let { InfoRow("Max Altitude", formatDegrees(it)) }
            }
        }
        item {
            InfoCard("Satellite") {
                InfoRow("Name", observation.title)
                observation.noradCatId?.let { InfoRow("NORAD", it.toString()) }
                observation.satId?.let { InfoRow("Sat ID", it) }
                observation.tle0?.let { InfoRow("TLE 0", it) }
            }
        }
        item {
            InfoCard("Station / Transmitter") {
                InfoRow("Ground Station", observation.stationDisplayName)
                observation.groundStation?.let { InfoRow("Station ID", it.toString()) }
                observation.stationLat?.let { InfoRow("Station Lat", "%.5f".format(it)) }
                observation.stationLng?.let { InfoRow("Station Lng", "%.5f".format(it)) }
                observation.stationAlt?.let { InfoRow("Station Alt", "%.0f m".format(it)) }
                InfoRow("Transmitter", observation.transmitterDisplayName)
                observation.transmitterMode?.let { InfoRow("Mode", it) }
                observation.transmitterType?.let { InfoRow("Type", it) }
                InfoRow("Frequency", observation.frequencyText)
            }
        }
        if (observation.riseAzimuth != null || observation.setAzimuth != null || observation.maxAltitude != null) {
            item {
                InfoCard("Polar Plot") {
                    ObservationPolarPlot(
                        riseAzimuth = observation.riseAzimuth,
                        setAzimuth = observation.setAzimuth,
                        maxAltitude = observation.maxAltitude,
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(128.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ObservationPolarPlot(
    riseAzimuth: Double?,
    setAzimuth: Double?,
    maxAltitude: Double?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val radius = min(size.width, size.height) / 2f - 28f
        val center = Offset(size.width / 2f, size.height / 2f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.Gray.toArgb()
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        val markerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.Gray.toArgb()
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { scale ->
            drawCircle(
                color = Color.Gray.copy(alpha = if (scale == 1f) 0.45f else 0.22f),
                radius = radius * scale,
                center = center,
                style = Stroke(width = if (scale == 1f) 1.3f else 0.8f)
            )
        }
        drawLine(Color.Gray.copy(alpha = 0.25f), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1f)
        drawLine(Color.Gray.copy(alpha = 0.25f), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1f)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText("N", center.x, center.y - radius - 12f, textPaint)
            canvas.nativeCanvas.drawText("E", center.x + radius + 18f, center.y + 9f, textPaint)
            canvas.nativeCanvas.drawText("S", center.x, center.y + radius + 28f, textPaint)
            canvas.nativeCanvas.drawText("W", center.x - radius - 18f, center.y + 9f, textPaint)
        }

        if (riseAzimuth != null && setAzimuth != null && maxAltitude != null) {
            val points = sampledPolarPoints(riseAzimuth, setAzimuth, maxAltitude, center, radius)
            if (points.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, color = Color(0xFF1C6DD0), style = Stroke(width = 3f))
            }
        }
        riseAzimuth?.let {
            val point = polarPoint(it, 0.0, center, radius)
            drawCircle(Color(0xFF1C6DD0), radius = 5f, center = point)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("Rise ${formatDegreesShort(it)}", point.x, point.y - 12f, markerTextPaint)
            }
        }
        setAzimuth?.let {
            val point = polarPoint(it, 0.0, center, radius)
            drawCircle(Color(0xFFE57A00), radius = 5f, center = point)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("Set ${formatDegreesShort(it)}", point.x, point.y - 12f, markerTextPaint)
            }
        }
        if (riseAzimuth != null && setAzimuth != null && maxAltitude != null) {
            val maxPoint = polarPoint(interpolatedAzimuth(riseAzimuth, setAzimuth, 0.5), maxAltitude, center, radius)
            drawCircle(
                Color(0xFF2A9D55),
                radius = 5f,
                center = maxPoint
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("Max ${formatDegreesShort(maxAltitude)}", maxPoint.x, maxPoint.y - 12f, markerTextPaint)
            }
        }
    }
}

@Composable
private fun ObservationWaterfallTab(observation: Observation) {
    val context = LocalContext.current
    val waterfallUrl = observation.waterfall
    var bitmap by remember(waterfallUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loading by rememberSaveable(waterfallUrl) { mutableStateOf(false) }
    var error by rememberSaveable(waterfallUrl) { mutableStateOf("") }

    LaunchedEffect(waterfallUrl) {
        bitmap = null
        error = ""
        if (!waterfallUrl.isNullOrBlank()) {
            loading = true
            runCatching {
                withContext(Dispatchers.IO) {
                    URL(waterfallUrl).openStream().use { BitmapFactory.decodeStream(it) }
                }
            }
                .onSuccess { bitmap = it }
                .onFailure { error = it.message ?: "Failed to load waterfall" }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            waterfallUrl.isNullOrBlank() -> EmptyState("No waterfall image.")
            loading -> LoadingRow("Loading waterfall...")
            bitmap != null -> Image(bitmap!!.asImageBitmap(), contentDescription = "Waterfall", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            error.isNotBlank() -> Text(error, color = MaterialTheme.colorScheme.error)
        }
        waterfallUrl?.let { url ->
            OutlinedButton(onClick = { openUrl(context, url) }) { Text("Open Waterfall") }
        }
    }
}

@Composable
private fun ObservationAudioTab(observation: Observation) {
    val context = LocalContext.current
    val audioUrl = observation.payload
    var player by remember(audioUrl) { mutableStateOf<MediaPlayer?>(null) }
    var preparing by rememberSaveable(audioUrl) { mutableStateOf(false) }
    var playing by rememberSaveable(audioUrl) { mutableStateOf(false) }
    var status by rememberSaveable(audioUrl) { mutableStateOf("Ready") }

    DisposableEffect(audioUrl) {
        onDispose {
            player?.runCatching {
                stop()
                release()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (audioUrl.isNullOrBlank()) {
            EmptyState("No audio playback.")
            return@Column
        }
        Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.height(64.dp), tint = MaterialTheme.colorScheme.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (playing) {
                        player?.pause()
                        playing = false
                        status = "Paused"
                    } else if (player != null) {
                        player?.start()
                        playing = true
                        status = "Playing"
                    } else {
                        preparing = true
                        status = "Preparing..."
                        runCatching {
                            MediaPlayer().also { mediaPlayer ->
                                player = mediaPlayer
                                mediaPlayer.setOnPreparedListener {
                                    preparing = false
                                    it.start()
                                    playing = true
                                    status = "Playing"
                                }
                                mediaPlayer.setOnCompletionListener {
                                    playing = false
                                    status = "Completed"
                                }
                                mediaPlayer.setOnErrorListener { mp, _, _ ->
                                    preparing = false
                                    playing = false
                                    status = "Playback failed. This device may not support the audio stream."
                                    mp.reset()
                                    true
                                }
                                mediaPlayer.setDataSource(audioUrl)
                                mediaPlayer.prepareAsync()
                            }
                        }.onFailure {
                            preparing = false
                            status = it.message ?: "Playback failed."
                        }
                    }
                },
                enabled = !preparing
            ) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (playing) "Pause" else "Play")
            }
            OutlinedButton(
                onClick = {
                    player?.runCatching {
                        stop()
                        release()
                    }
                    player = null
                    playing = false
                    preparing = false
                    status = "Stopped"
                }
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Stop")
            }
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(audioUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        OutlinedButton(onClick = { openUrl(context, audioUrl) }) { Text("Open Audio") }
    }
}

@Composable
private fun TimelineScreen(api: SatnogsApi, targets: List<WatchTarget>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheduleStore = remember { StationScheduleStore(context) }
    val scope = rememberCoroutineScope()
    var observations by remember { mutableStateOf<List<Observation>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var showingObservationList by rememberSaveable { mutableStateOf(false) }
    val stationNames = remember(targets) {
        targets.flatMap { target -> target.stationNames.entries }.associate { it.key to it.value }
    }

    if (showingObservationList) {
        TimelineObservationListScreen(
            observations = observations,
            onBack = { showingObservationList = false },
            modifier = modifier
        )
        return
    }

    fun refresh() {
        scope.launch {
            loading = true
            message = ""
            val stationIds = targets.flatMap { it.stationIds }.distinct().sorted()
            observations = scheduleStore.load(stationIds)
            runCatching { scheduleStore.refresh(api, stationIds) }
                .onSuccess { observations = it }
                .onFailure { message = "Refresh failed, showing cache: ${it.message ?: "unknown error"}" }
            loading = false
        }
    }

    LaunchedEffect(targets.map { it.id }) {
        val stationIds = targets.flatMap { it.stationIds }.distinct().sorted()
        observations = scheduleStore.load(stationIds)
    }

    Column(modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Timeline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Row {
                IconButton(onClick = { showingObservationList = true }, enabled = observations.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Observation list")
                }
                IconButton(onClick = { refresh() }, enabled = !loading && targets.isNotEmpty()) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh timeline")
                }
            }
        }
        if (targets.isEmpty()) {
            EmptyState("Timeline will summarize saved watch targets after you add them.")
        } else if (loading && observations.isEmpty()) {
            LoadingRow("Loading future observations...")
        } else if (observations.isEmpty()) {
            EmptyState("No future observations found for watched stations.")
        } else {
            val now = Instant.now()
            val end = now.plus(Duration.ofDays(2))
            val timelineItems = observations.mapNotNull { observation ->
                val start = parseSatnogsInstantOrNull(observation.start)
                val stop = parseSatnogsInstantOrNull(observation.end)
                val stationId = observation.groundStation
                if (start == null || stop == null || stationId == null) null else {
                    TimelineItem(
                        stationId = stationId,
                        stationName = observation.stationName ?: stationNames[stationId] ?: "Station $stationId",
                        label = observation.title,
                        start = start,
                        end = stop,
                        color = Color(0xFF1F9D6E)
                    )
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    PassTimeline(
                        items = timelineItems,
                        start = now,
                        end = end,
                        stationNames = stationNames
                    )
                }
                if (message.isNotBlank()) {
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun TimelineObservationListScreen(
    observations: List<Observation>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Observation List", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        if (observations.isEmpty()) {
            EmptyState("No cached future observations.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(observations, key = { it.id }) { observation ->
                    ObservationCard(observation)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    token: String,
    targets: List<WatchTarget>,
    onSaveToken: (String) -> Unit,
    onTargetsChanged: (List<WatchTarget>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val watchTargetStore = remember { WatchTargetStore(context) }
    val stationScheduleStore = remember { StationScheduleStore(context) }
    var settings by remember { mutableStateOf(settingsStore.load()) }
    var draftToken by rememberSaveable(token) { mutableStateOf(token) }
    var observerId by rememberSaveable(settings.observerId) { mutableStateOf(settings.observerId) }
    var batchSizeText by rememberSaveable(settings.autoScheduleBatchSize) { mutableStateOf(settings.autoScheduleBatchSize.toString()) }
    var timeMenuOpen by rememberSaveable { mutableStateOf(false) }
    var sortMenuOpen by rememberSaveable { mutableStateOf(false) }
    var importText by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    fun saveSettings(updated: AppSettings = settings) {
        val normalized = updated.copy(
            observerId = observerId.trim(),
            autoScheduleBatchSize = batchSizeText.toIntOrNull()?.coerceIn(1, 50) ?: 10
        )
        settings = normalized
        settingsStore.save(normalized)
        message = "Settings saved."
    }

    LazyColumn(
        modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }

        item {
            SettingsSection("Account") {
                OutlinedTextField(
                    value = draftToken,
                    onValueChange = { draftToken = it },
                    label = { Text("SatNOGS API token") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = observerId,
                    onValueChange = { observerId = it },
                    label = { Text("Observer ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onSaveToken(draftToken)
                        message = "API token saved."
                    }
                ) { Text("Save API token") }
                Button(
                    onClick = {
                        saveSettings()
                        message = "Observer ID saved."
                    },
                    enabled = observerId.isBlank() || observerId.trim().toIntOrNull() != null
                ) { Text("Save Observer ID") }
                Text("Observer ID is used to load observations that need rating.", style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            SettingsSection("Display") {
                OutlinedButton(onClick = { timeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Time display: ${settings.timeDisplayMode.label}")
                }
                DropdownMenu(expanded = timeMenuOpen, onDismissRequest = { timeMenuOpen = false }) {
                    TimeDisplayMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                settings = settings.copy(timeDisplayMode = mode)
                                settingsStore.save(settings)
                                timeMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        item {
            SettingsSection("Batch auto schedule") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Preview before scheduling")
                    Switch(
                        checked = settings.autoSchedulePreviewEnabled,
                        onCheckedChange = {
                            settings = settings.copy(autoSchedulePreviewEnabled = it)
                            settingsStore.save(settings)
                        }
                    )
                }
                OutlinedButton(onClick = { sortMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sort order: ${settings.autoScheduleSortOrder.label}")
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    AutoScheduleSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                settings = settings.copy(autoScheduleSortOrder = order)
                                settingsStore.save(settings)
                                sortMenuOpen = false
                            }
                        )
                    }
                }
                Text(settings.autoScheduleSortOrder.descriptionText(), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = batchSizeText,
                    onValueChange = { batchSizeText = it },
                    label = { Text("Batch size") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { saveSettings() }) { Text("Save batch settings") }
                Text("Batch auto schedule is not implemented yet; these values are saved for the upcoming workflow.", style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            SettingsSection("Watch list import / export") {
                Text("Saved watch targets: ${targets.size}", color = MaterialTheme.colorScheme.secondary)
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("SatScheduler Watch List", watchTargetStore.exportTargets(targets))
                        )
                        message = "Watch list copied to clipboard."
                    }
                ) { Text("Export watch list") }
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Import JSON") },
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
                Button(
                    onClick = {
                        runCatching { watchTargetStore.importTargets(importText) }
                            .onSuccess {
                                onTargetsChanged(it)
                                message = "Imported ${it.size} watch target(s)."
                            }
                            .onFailure { message = "Import failed: ${it.message ?: "invalid JSON"}" }
                    }
                ) { Text("Import watch list") }
            }
        }

        item {
            SettingsSection("Local cache") {
                Button(
                    onClick = {
                        stationScheduleStore.clearAll()
                        message = "Timeline cache deleted."
                    }
                ) { Text("Delete timeline cache") }
            }
        }

        if (message.isNotBlank()) {
            item { Text(message, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text)
            Spacer(Modifier.height(2.dp))
        }
    }
}

private fun List<Satellite>.filterBySatelliteQuery(query: String): List<Satellite> {
    val keyword = query.trim().lowercase()
    if (keyword.isEmpty()) return this
    return filter { satellite ->
        listOfNotNull(
            satellite.displayName,
            satellite.names,
            satellite.satId,
            satellite.noradCatId?.toString()
        ).any { it.lowercase().contains(keyword) }
    }
}

private fun List<GroundStation>.filterByStationQuery(query: String): List<GroundStation> {
    val keyword = query.trim().lowercase()
    if (keyword.isEmpty()) return this
    return filter { station ->
        listOfNotNull(
            station.id.toString(),
            station.displayName,
            station.status,
            station.qthlocator,
            station.owner,
            station.antennaText
        ).any { it.lowercase().contains(keyword) }
    }
}

private fun recommendedTransmitter(transmitters: List<Transmitter>, observations: List<Observation>): Pair<String, Int>? {
    val available = transmitters.map { it.uuid }.toSet()
    return observations
        .mapNotNull { it.transmitterUuid }
        .filter { available.contains(it) }
        .groupingBy { it }
        .eachCount()
        .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key })
        ?.let { it.key to it.value }
}

private fun transmitterLabel(transmitter: Transmitter, recommendedId: String?): String {
    val prefix = if (transmitter.uuid == recommendedId) "👍 " else ""
    return "$prefix${transmitter.displayName} / ${transmitter.frequencyText}"
}

private fun peakElevationError(minText: String, maxText: String): String? {
    val min = parsePeakElevation(minText)
    val max = parsePeakElevation(maxText)
    if (minText.isNotBlank() && min == null) return "Minimum peak elevation must be between 0 and 90 degrees."
    if (maxText.isNotBlank() && max == null) return "Maximum peak elevation must be between 0 and 90 degrees."
    if (min != null && max != null && min > max) return "Minimum peak elevation must not be greater than maximum peak elevation."
    return null
}

private fun azimuthError(minText: String, maxText: String): String? {
    val min = parseAzimuth(minText)
    val max = parseAzimuth(maxText)
    if (minText.isBlank() && maxText.isBlank()) return null
    if (minText.isBlank() || maxText.isBlank()) return "Input both minimum and maximum azimuth, or leave both blank."
    if (min == null) return "Minimum azimuth must be between 0 and 360 degrees."
    if (max == null) return "Maximum azimuth must be between 0 and 360 degrees."
    return null
}

private fun parsePeakElevation(text: String): Double? {
    val value = text.trim().toDoubleOrNull() ?: return null
    return value.takeIf { it in 0.0..90.0 }
}

private fun parseAzimuth(text: String): Double? {
    val value = text.trim().toDoubleOrNull() ?: return null
    return value.takeIf { it in 0.0..360.0 }
}

private fun Int.toMHzText(): String {
    return "%.6f".format(this / 1_000_000.0).trimEnd('0').trimEnd('.')
}

private fun rangeText(label: String, min: Double?, max: Double?): String? {
    if (min == null && max == null) return null
    return "$label ${min?.formatDeg() ?: "-"}-${max?.formatDeg() ?: "-"} deg"
}

private fun AutoScheduleSortOrder.descriptionText(): String {
    return when (this) {
        AutoScheduleSortOrder.WatchListOrder -> "Use the manual Watch List order as the scheduling priority."
        AutoScheduleSortOrder.WatchListOrderThenPeakElevation -> "Keep Watch List priority, but prefer higher peak elevation when choosing passes for the same target."
        AutoScheduleSortOrder.PeakElevationFirst -> "Prefer higher peak elevation across all targets, regardless of Watch List order."
    }
}

private fun Double.formatDeg(): String = "%g".format(this)

private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

private fun formatShortDateTime(instant: Instant): String = shortDateFormatter.format(instant)

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun formatDegrees(value: Double): String = "%.1f deg".format(value)

private fun formatDegreesShort(value: Double): String = "%.0f deg".format(value)

private fun sampledPolarPoints(
    riseAzimuth: Double,
    setAzimuth: Double,
    maxAltitude: Double,
    center: Offset,
    radius: Float
): List<Offset> {
    val risePoint = polarPoint(riseAzimuth, 0.0, center, radius)
    val setPoint = polarPoint(setAzimuth, 0.0, center, radius)
    val maxPoint = polarPoint(interpolatedAzimuth(riseAzimuth, setAzimuth, 0.5), maxAltitude, center, radius)
    val control = Offset(
        x = 2 * maxPoint.x - (risePoint.x + setPoint.x) / 2,
        y = 2 * maxPoint.y - (risePoint.y + setPoint.y) / 2
    )
    return (0..72).map { index ->
        val t = index / 72f
        val oneMinusT = 1f - t
        Offset(
            x = oneMinusT * oneMinusT * risePoint.x + 2 * oneMinusT * t * control.x + t * t * setPoint.x,
            y = oneMinusT * oneMinusT * risePoint.y + 2 * oneMinusT * t * control.y + t * t * setPoint.y
        )
    }
}

private fun polarPoint(azimuth: Double, altitude: Double, center: Offset, radius: Float): Offset {
    val normalizedAltitude = altitude.coerceIn(0.0, 90.0) / 90.0
    val distance = radius * (1.0 - normalizedAltitude).toFloat()
    val radians = azimuth * Math.PI / 180.0
    return Offset(
        x = center.x + distance * sin(radians).toFloat(),
        y = center.y - distance * cos(radians).toFloat()
    )
}

private fun interpolatedAzimuth(start: Double, end: Double, progress: Double): Double {
    var delta = end - start
    if (delta > 180) {
        delta -= 360
    } else if (delta < -180) {
        delta += 360
    }
    val value = start + delta * progress
    return if (value < 0) value + 360 else value % 360
}
