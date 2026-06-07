package net.bi119aTe5hXk.satscheduler.data

import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.TLE
import net.bi119aTe5hXk.satscheduler.model.PredictedPass
import net.bi119aTe5hXk.satscheduler.model.PredictionConflictSummary
import net.bi119aTe5hXk.satscheduler.model.TleEntry
import net.bi119aTe5hXk.satscheduler.model.WatchStationSnapshot
import net.bi119aTe5hXk.satscheduler.model.WatchTarget
import java.time.Duration
import java.time.Instant
import java.util.Date
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class PassPredictionEngine(
    private val scheduleLeadTime: Duration = Duration.ofMinutes(5),
    private val predictionWindow: Duration = Duration.ofDays(2)
) {
    fun predict(target: WatchTarget, tle: TleEntry, now: Instant = Instant.now()): List<PredictedPass> {
        val start = now.plus(scheduleLeadTime)
        val end = now.plus(predictionWindow)
        val hoursAhead = predictionWindow.toHours().toInt()
        val tleModel = TLE(arrayOf(tle.displayName, tle.tle1, tle.tle2))

        return target.stationIds.flatMap { stationId ->
            val station = target.stationSnapshots[stationId] ?: return@flatMap emptyList()
            predictForStation(target, tleModel, station, start, end, hoursAhead)
        }.sortedBy { it.startInstant }
    }

    private fun predictForStation(
        target: WatchTarget,
        tle: TLE,
        station: WatchStationSnapshot,
        start: Instant,
        end: Instant,
        hoursAhead: Int
    ): List<PredictedPass> {
        val latitude = station.latitude ?: return emptyList()
        val longitude = station.longitude ?: return emptyList()
        val altitudeMeters = station.altitude ?: 0.0
        val predictor = PassPredictor(
            tle,
            GroundStationPosition(latitude, longitude, altitudeMeters, station.name)
        )

        return predictor.getPasses(Date.from(start), hoursAhead, false)
            .map { pass ->
                val samples = sampleAzimuths(predictor, pass.startTime.toInstant(), pass.endTime.toInstant())
                PredictedPass(
                    stationId = station.id,
                    stationName = station.name,
                    start = pass.startTime.toInstant().toString(),
                    end = pass.endTime.toInstant().toString(),
                    maxElevation = pass.maxEl,
                    azimuthStart = pass.aosAzimuth.toDouble(),
                    azimuthEnd = pass.losAzimuth.toDouble(),
                    azimuthSamples = samples
                )
            }
            .filter { pass ->
                pass.endInstant.isAfter(start) &&
                    pass.startInstant.isBefore(end) &&
                    Duration.between(pass.startInstant, pass.endInstant) >= Duration.ofMinutes(3) &&
                    pass.maxElevation >= (station.minHorizon ?: 0.0) &&
                    passesPeakElevationFilter(pass, target) &&
                    passesAzimuthFilter(pass, target) &&
                    passesDaylightFilter(pass, target, station)
            }
    }

    private fun sampleAzimuths(predictor: PassPredictor, start: Instant, end: Instant): List<Double> {
        val samples = mutableListOf<Double>()
        var cursor = start
        while (!cursor.isAfter(end)) {
            val pos = predictor.getSatPos(Date.from(cursor))
            samples += radiansToDegrees(pos.azimuth).normalizeAzimuth()
            cursor = cursor.plusSeconds(60)
        }
        return samples
    }

    private fun passesPeakElevationFilter(pass: PredictedPass, target: WatchTarget): Boolean {
        val min = target.minPeakElevation
        val max = target.maxPeakElevation
        return (min == null || pass.maxElevation >= min) &&
            (max == null || pass.maxElevation <= max)
    }

    private fun passesAzimuthFilter(pass: PredictedPass, target: WatchTarget): Boolean {
        val min = target.minAzimuth ?: return true
        val max = target.maxAzimuth ?: return true
        return pass.azimuthSamples.any { it.isInsideAzimuthRange(min, max) }
    }

    private fun passesDaylightFilter(
        pass: PredictedPass,
        target: WatchTarget,
        station: WatchStationSnapshot
    ): Boolean {
        if (!target.requireStationDaylight) return true
        val latitude = station.latitude ?: return false
        val longitude = station.longitude ?: return false
        val midpoint = pass.startInstant.plusMillis(
            Duration.between(pass.startInstant, pass.endInstant).toMillis() / 2
        )
        return solarElevation(midpoint, latitude, longitude) > 0.0
    }
}

fun PredictedPass.toScheduleRequest(target: WatchTarget): ObservationScheduleRequest {
    return ObservationScheduleRequest(
        groundStationId = stationId,
        transmitterUuid = target.transmitterId,
        start = start,
        end = end
    )
}

fun filterConflicts(
    predictedPasses: List<PredictedPass>,
    existingObservations: List<net.bi119aTe5hXk.satscheduler.model.Observation>,
    buffer: Duration = Duration.ofMinutes(5)
): ConflictFilterResult {
    val visible = predictedPasses.filter { pass ->
        existingObservations.none { observation ->
            observation.groundStation == pass.stationId &&
                overlaps(
                    pass.startInstant.minus(buffer),
                    pass.endInstant.plus(buffer),
                    observation.start?.let(Instant::parse),
                    observation.end?.let(Instant::parse)
                )
        }
    }
    return ConflictFilterResult(
        visiblePasses = visible,
        summary = PredictionConflictSummary(
            predictedCount = predictedPasses.size,
            visibleCount = visible.size,
            hiddenCount = predictedPasses.size - visible.size
        )
    )
}

data class ConflictFilterResult(
    val visiblePasses: List<PredictedPass>,
    val summary: PredictionConflictSummary
)

private fun overlaps(start: Instant, end: Instant, otherStart: Instant?, otherEnd: Instant?): Boolean {
    if (otherStart == null || otherEnd == null) return false
    return start < otherEnd && end > otherStart
}

private fun radiansToDegrees(value: Double): Double = value * 180.0 / PI

private fun Double.normalizeAzimuth(): Double {
    val normalized = this % 360.0
    return if (normalized >= 0) normalized else normalized + 360.0
}

private fun Double.isInsideAzimuthRange(min: Double, max: Double): Boolean {
    val value = normalizeAzimuth()
    val normalizedMin = min.normalizeAzimuth()
    val normalizedMax = max.normalizeAzimuth()
    return if (normalizedMin <= normalizedMax) {
        value in normalizedMin..normalizedMax
    } else {
        value >= normalizedMin || value <= normalizedMax
    }
}

private fun solarElevation(time: Instant, latitude: Double, longitude: Double): Double {
    val days = time.epochSecond / 86_400.0 + 2_440_587.5 - 2_451_545.0
    val meanLongitude = (280.46 + 0.9856474 * days).normalizeAzimuth()
    val meanAnomaly = Math.toRadians((357.528 + 0.9856003 * days).normalizeAzimuth())
    val eclipticLongitude = Math.toRadians(
        (meanLongitude + 1.915 * sin(meanAnomaly) + 0.020 * sin(2.0 * meanAnomaly)).normalizeAzimuth()
    )
    val obliquity = Math.toRadians(23.439 - 0.0000004 * days)
    val rightAscension = kotlin.math.atan2(cos(obliquity) * sin(eclipticLongitude), cos(eclipticLongitude))
    val declination = kotlin.math.asin(sin(obliquity) * sin(eclipticLongitude))
    val gmst = (280.46061837 + 360.98564736629 * days).normalizeAzimuth()
    val localSidereal = Math.toRadians((gmst + longitude).normalizeAzimuth())
    val hourAngle = localSidereal - rightAscension
    val latRad = Math.toRadians(latitude)
    return radiansToDegrees(acos(cos(latRad) * cos(declination) * cos(hourAngle) + sin(latRad) * sin(declination)) - PI / 2.0) * -1.0
}
