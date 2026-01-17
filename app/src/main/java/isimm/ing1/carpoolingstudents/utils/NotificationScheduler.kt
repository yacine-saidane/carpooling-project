package isimm.ing1.carpoolingstudents.utils

import android.content.Context
import androidx.work.*
import isimm.ing1.carpoolingstudents.workers.RideNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleRideNotification(
        context: Context,
        rideId: String,
        userId: String,
        isDriver: Boolean,
        departureTime: Long
    ) {
        val notificationTime = departureTime - TimeUnit.MINUTES.toMillis(20)
        val currentTime = System.currentTimeMillis()
        val delay = notificationTime - currentTime

        if (delay > 0) {
            val inputData = Data.Builder()
                .putString(RideNotificationWorker.RIDE_ID, rideId)
                .putString(RideNotificationWorker.USER_ID, userId)
                .putBoolean(RideNotificationWorker.IS_DRIVER, isDriver)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<RideNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("ride_notification_$rideId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ride_notification_${rideId}_$userId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun cancelRideNotification(context: Context, rideId: String, userId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("ride_notification_${rideId}_$userId")
    }

    fun cancelAllNotificationsForRide(context: Context, rideId: String) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("ride_notification_$rideId")
    }
}