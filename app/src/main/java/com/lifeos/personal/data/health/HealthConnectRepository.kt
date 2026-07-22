package com.lifeos.personal.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

data class HealthSnapshot(
    val steps: Long = 0,
    val distanceKm: Double = 0.0,
    val activeCalories: Double = 0.0,
    val sleepHours: Double = 0.0,
    val averageHeartRate: Long? = null,
    val restingHeartRate: Long? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val exerciseSessions: Int = 0,
    val updatedAt: Instant? = null,
)

class HealthConnectRepository(private val context: Context) {
    val availability: Int get() = HealthConnectClient.getSdkStatus(context)
    val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        StepsRecord::class, SleepSessionRecord::class, HeartRateRecord::class,
        RestingHeartRateRecord::class, ExerciseSessionRecord::class, WeightRecord::class,
        BodyFatRecord::class, DistanceRecord::class, ActiveCaloriesBurnedRecord::class,
    ).mapTo(mutableSetOf()) { HealthPermission.getReadPermission(it) }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean =
        availability == HealthConnectClient.SDK_AVAILABLE && client.permissionController.getGrantedPermissions().containsAll(permissions)

    suspend fun readSnapshot(): HealthSnapshot {
        val end = Instant.now()
        val dayStart = end.minus(Duration.ofHours(24))
        val monthStart = end.minus(Duration.ofDays(30))
        val day = TimeRangeFilter.between(dayStart, end)
        val month = TimeRangeFilter.between(monthStart, end)
        val steps = read<StepsRecord>(day).sumOf { it.count }
        val distance = read<DistanceRecord>(day).sumOf { it.distance.inMeters } / 1000.0
        val calories = read<ActiveCaloriesBurnedRecord>(day).sumOf { it.energy.inKilocalories }
        val sleep = read<SleepSessionRecord>(day).sumOf { Duration.between(it.startTime, it.endTime).toMinutes() } / 60.0
        val heartSamples = read<HeartRateRecord>(day).flatMap { it.samples }.map { it.beatsPerMinute }
        val resting = read<RestingHeartRateRecord>(month).maxByOrNull { it.time }?.beatsPerMinute
        val weight = read<WeightRecord>(month).maxByOrNull { it.time }?.weight?.inKilograms
        val fat = read<BodyFatRecord>(month).maxByOrNull { it.time }?.percentage?.value
        return HealthSnapshot(
            steps = steps,
            distanceKm = distance,
            activeCalories = calories,
            sleepHours = sleep,
            averageHeartRate = heartSamples.takeIf { it.isNotEmpty() }?.average()?.toLong(),
            restingHeartRate = resting,
            weightKg = weight,
            bodyFatPercent = fat,
            exerciseSessions = read<ExerciseSessionRecord>(day).size,
            updatedAt = end,
        )
    }

    private suspend inline fun <reified T : Record> read(filter: TimeRangeFilter): List<T> =
        client.readRecords(ReadRecordsRequest(T::class, filter)).records
}
