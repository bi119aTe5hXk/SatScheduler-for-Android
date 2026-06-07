package net.bi119aTe5hXk.satscheduler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.bi119aTe5hXk.satscheduler.data.ObservationScheduleRequest
import net.bi119aTe5hXk.satscheduler.data.AppSettings
import net.bi119aTe5hXk.satscheduler.data.AppSettingsStore
import net.bi119aTe5hXk.satscheduler.data.AutoScheduleSortOrder
import net.bi119aTe5hXk.satscheduler.data.PassPredictionEngine
import net.bi119aTe5hXk.satscheduler.data.SatnogsApi
import net.bi119aTe5hXk.satscheduler.data.StationScheduleStore
import net.bi119aTe5hXk.satscheduler.data.TleCacheStore
import net.bi119aTe5hXk.satscheduler.data.TimeDisplayMode
import net.bi119aTe5hXk.satscheduler.data.WatchTargetStore
import net.bi119aTe5hXk.satscheduler.data.filterConflicts
import net.bi119aTe5hXk.satscheduler.data.newWatchTarget
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

private enum class AppTab(val title: String) {
    WatchList("Watch List"),
    Observations("Observations"),
    Timeline("Timeline"),
    Settings("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = { CenterAlignedTopAppBar(title = { Text("SatScheduler") }) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title) },
                        icon = {}
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
                    token = newToken
                    store.saveToken(newToken)
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

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Watch List", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Button(onClick = { showingAddDialog = true }) { Text("Add") }
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

private data class TimelineItem(
    val stationId: Int,
    val stationName: String,
    val label: String,
    val start: Instant,
    val end: Instant,
    val color: Color
)

@Composable
private fun PassTimeline(items: List<TimelineItem>, start: Instant, end: Instant) {
    val totalMillis = Duration.between(start, end).toMillis().coerceAtLeast(1)
    val stations = items
        .groupBy { it.stationId }
        .mapValues { entry -> entry.value.sortedBy { it.start } }
        .toSortedMap()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Station", modifier = Modifier.width(112.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatShortDateTime(start), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(formatShortDateTime(end), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        stations.forEach { (_, stationItems) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stationItems.firstOrNull()?.stationName ?: "Station",
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
    val scope = rememberCoroutineScope()
    var observerId by rememberSaveable { mutableStateOf("") }
    var futureOnly by rememberSaveable { mutableStateOf(true) }
    var observations by remember { mutableStateOf<List<Observation>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Observations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = observerId,
            onValueChange = { observerId = it },
            label = { Text("Observer ID") },
            placeholder = { Text("Optional") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Future only")
            Spacer(Modifier.width(8.dp))
            Switch(checked = futureOnly, onCheckedChange = { futureOnly = it })
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = ""
                        runCatching {
                            api.fetchObservations(observerId = observerId.ifBlank { null }, future = futureOnly)
                        }
                            .onSuccess { observations = it }
                            .onFailure { error = it.message ?: "Failed to fetch observations" }
                        loading = false
                    }
                },
                enabled = !loading
            ) { Text("Refresh") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (observations.isEmpty() && !loading) {
            EmptyState("Fetch observations from the SatNOGS Network API.")
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
private fun ObservationCard(observation: Observation) {
    Card(Modifier.fillMaxWidth()) {
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

@Composable
private fun TimelineScreen(api: SatnogsApi, targets: List<WatchTarget>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheduleStore = remember { StationScheduleStore(context) }
    val scope = rememberCoroutineScope()
    var observations by remember { mutableStateOf<List<Observation>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    val stationNames = remember(targets) {
        targets.flatMap { target -> target.stationNames.entries }.associate { it.key to it.value }
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

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Timeline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { refresh() }, enabled = !loading && targets.isNotEmpty()) {
                Text(if (loading) "Refreshing..." else "Refresh")
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
            val end = now.plus(Duration.ofDays(3))
            val timelineItems = observations.mapNotNull { observation ->
                val start = observation.start?.let { runCatching { Instant.parse(it) }.getOrNull() }
                val stop = observation.end?.let { runCatching { Instant.parse(it) }.getOrNull() }
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
            PassTimeline(items = timelineItems, start = now, end = end)
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(observations, key = { it.id }) { observation ->
                    ObservationCard(observation)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(token: String, onSaveToken: (String) -> Unit, modifier: Modifier = Modifier) {
    var draftToken by rememberSaveable(token) { mutableStateOf(token) }
    var savedMessage by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = draftToken,
            onValueChange = { draftToken = it },
            label = { Text("SatNOGS API token") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onSaveToken(draftToken)
                savedMessage = "Token saved locally."
            }
        ) { Text("Save token") }
        if (savedMessage.isNotBlank()) {
            Text(savedMessage, color = MaterialTheme.colorScheme.secondary)
        }
        Text("The token is stored in this app's private preferences and used for SatNOGS requests that need authentication.")
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

private fun Double.formatDeg(): String = "%g".format(this)

private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

private fun formatShortDateTime(instant: Instant): String = shortDateFormatter.format(instant)
