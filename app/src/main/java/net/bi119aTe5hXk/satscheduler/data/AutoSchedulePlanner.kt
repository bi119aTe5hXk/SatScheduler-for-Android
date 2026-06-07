package net.bi119aTe5hXk.satscheduler.data

import net.bi119aTe5hXk.satscheduler.model.Observation
import net.bi119aTe5hXk.satscheduler.model.PredictedPass
import net.bi119aTe5hXk.satscheduler.model.WatchTarget
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class ScheduleCandidateStatus {
    Pending,
    Scheduled,
    Failed
}

data class AutoScheduleCandidate(
    val id: String = UUID.randomUUID().toString(),
    val targetIndex: Int,
    val target: WatchTarget,
    val pass: PredictedPass,
    val status: ScheduleCandidateStatus = ScheduleCandidateStatus.Pending,
    val errorMessage: String? = null
) {
    val request: ObservationScheduleRequest
        get() = pass.toScheduleRequest(target)
}

data class AutoSchedulePlan(
    val createdAt: Instant,
    val start: Instant,
    val end: Instant,
    val priorityMode: AutoScheduleSortOrder,
    val satelliteCount: Int,
    val candidateCount: Int,
    val selectedCandidates: List<AutoScheduleCandidate>,
    val skippedCount: Int,
    val existingObservations: List<Observation>
)

class AutoSchedulePlanner(
    private val predictionEngine: PassPredictionEngine = PassPredictionEngine()
) {
    suspend fun buildPlan(
        api: SatnogsApi,
        tleCacheStore: TleCacheStore,
        scheduleStore: StationScheduleStore,
        targets: List<WatchTarget>,
        priorityMode: AutoScheduleSortOrder
    ): AutoSchedulePlan {
        val now = Instant.now()
        val start = now
        val end = now.plus(Duration.ofDays(2))
        val stationIds = targets.flatMap { it.stationIds }.distinct().sorted()
        val existingObservations = runCatching {
            scheduleStore.refresh(api, stationIds)
        }.getOrElse {
            scheduleStore.load(stationIds)
        }

        val candidates = targets.flatMapIndexed { index, target ->
            val tle = tleCacheStore.fetchLatest(api, target.satelliteId) ?: return@flatMapIndexed emptyList()
            predictionEngine.predict(target, tle, now).map { pass ->
                AutoScheduleCandidate(
                    targetIndex = index,
                    target = target,
                    pass = pass
                )
            }
        }

        val sortedCandidates = sortCandidates(candidates, priorityMode)
        val selected = selectNonConflicting(sortedCandidates, existingObservations)

        return AutoSchedulePlan(
            createdAt = now,
            start = start,
            end = end,
            priorityMode = priorityMode,
            satelliteCount = targets.size,
            candidateCount = candidates.size,
            selectedCandidates = selected,
            skippedCount = candidates.size - selected.size,
            existingObservations = existingObservations
        )
    }

    fun sortCandidates(
        candidates: List<AutoScheduleCandidate>,
        priorityMode: AutoScheduleSortOrder
    ): List<AutoScheduleCandidate> {
        return when (priorityMode) {
            AutoScheduleSortOrder.WatchListOrder -> candidates.sortedWith(
                compareBy<AutoScheduleCandidate> { it.targetIndex }
                    .thenBy { it.pass.startInstant }
            )
            AutoScheduleSortOrder.WatchListOrderThenPeakElevation -> candidates.sortedWith(
                compareBy<AutoScheduleCandidate> { it.targetIndex }
                    .thenByDescending { it.pass.maxElevation }
                    .thenBy { it.pass.startInstant }
            )
            AutoScheduleSortOrder.PeakElevationFirst -> candidates.sortedWith(
                compareByDescending<AutoScheduleCandidate> { it.pass.maxElevation }
                    .thenBy { it.targetIndex }
                    .thenBy { it.pass.startInstant }
            )
        }
    }

    fun selectNonConflicting(
        candidates: List<AutoScheduleCandidate>,
        existingObservations: List<Observation>,
        buffer: Duration = Duration.ofMinutes(5)
    ): List<AutoScheduleCandidate> {
        val selected = mutableListOf<AutoScheduleCandidate>()
        candidates.forEach { candidate ->
            val conflictsExisting = existingObservations.any { observation ->
                observation.groundStation == candidate.pass.stationId &&
                    overlaps(
                        candidate.pass.startInstant.minus(buffer),
                        candidate.pass.endInstant.plus(buffer),
                        parseSatnogsInstantOrNull(observation.start),
                        parseSatnogsInstantOrNull(observation.end)
                    )
                }
            val conflictsSelected = selected.any { selectedCandidate ->
                selectedCandidate.pass.stationId == candidate.pass.stationId &&
                    overlaps(
                        candidate.pass.startInstant.minus(buffer),
                        candidate.pass.endInstant.plus(buffer),
                        selectedCandidate.pass.startInstant.minus(buffer),
                        selectedCandidate.pass.endInstant.plus(buffer)
                    )
            }
            if (!conflictsExisting && !conflictsSelected) {
                selected += candidate
            }
        }
        return selected
    }

    private fun overlaps(start: Instant, end: Instant, otherStart: Instant?, otherEnd: Instant?): Boolean {
        if (otherStart == null || otherEnd == null) return false
        return start < otherEnd && end > otherStart
    }
}
