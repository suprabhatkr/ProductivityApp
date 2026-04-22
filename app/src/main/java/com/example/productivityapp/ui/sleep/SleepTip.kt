package com.example.productivityapp.ui.sleep

import com.example.productivityapp.data.entities.SleepEntity
import kotlin.math.roundToInt

internal data class SleepTip(
    val title: String,
    val message: String,
)

internal fun buildSleepTip(
    activeSession: SleepEntity?,
    pendingDetectedReviewSession: SleepEntity?,
    averageQuality: Double?,
    bedtimeDeviationMinutes: Double?,
    wakeDeviationMinutes: Double?,
    napCount: Int,
): SleepTip {
    return when {
        activeSession != null -> SleepTip(
            title = "Let the session run",
            message = "Keep the room dark and avoid checking your phone while sleep is in progress.",
        )

        pendingDetectedReviewSession != null -> SleepTip(
            title = "Review the detected session",
            message = "Confirm, merge, or adjust it so the detector keeps learning your routine.",
        )

        averageQuality != null && averageQuality < 3.5 -> SleepTip(
            title = "Sleep quality is running low",
            message = "Try a steadier bedtime and fewer screens before sleeping tonight.",
        )

        bedtimeDeviationMinutes != null && bedtimeDeviationMinutes.roundToInt() >= 30 -> SleepTip(
            title = "Bedtime is drifting",
            message = "Aim closer to your target bedtime to make sleep more consistent.",
        )

        wakeDeviationMinutes != null && wakeDeviationMinutes.roundToInt() >= 30 -> SleepTip(
            title = "Wake time is drifting",
            message = "A consistent wake time helps the next night’s sleep stay on track.",
        )

        napCount > 0 -> SleepTip(
            title = "Use the nap timer",
            message = "Short daytime rest works best when the nap stays clearly separate from overnight sleep.",
        )

        else -> SleepTip(
            title = "Keep the routine steady",
            message = "A regular bedtime and quick review of detected sessions help the detector learn.",
        )
    }
}
