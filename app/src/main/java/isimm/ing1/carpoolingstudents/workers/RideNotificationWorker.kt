package isimm.ing1.carpoolingstudents.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import isimm.ing1.carpoolingstudents.R
import isimm.ing1.carpoolingstudents.model.Ride
import isimm.ing1.carpoolingstudents.ui.rides.RideDetailActivity
import isimm.ing1.carpoolingstudents.utils.Constants

class RideNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val RIDE_ID = "ride_id"
        const val USER_ID = "user_id"
        const val IS_DRIVER = "is_driver"
        const val CHANNEL_ID = "ride_reminders"
        const val NOTIFICATION_ID = 1001
    }

    override fun doWork(): Result {
        val rideId = inputData.getString(RIDE_ID) ?: return Result.failure()
        val userId = inputData.getString(USER_ID) ?: return Result.failure()
        val isDriver = inputData.getBoolean(IS_DRIVER, false)

        val firestore = FirebaseFirestore.getInstance()

        try {
            val rideDoc = firestore.collection(Constants.COLLECTION_RIDES)
                .document(rideId)
                .get()
                .addOnSuccessListener { document ->
                    val ride = document.toObject(Ride::class.java)
                    if (ride != null) {
                        sendNotification(ride, isDriver)
                    }
                }
                .addOnFailureListener {
                }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun sendNotification(ride: Ride, isDriver: Boolean) {
        createNotificationChannel()

        val intent = Intent(applicationContext, RideDetailActivity::class.java).apply {
            putExtra("rideId", ride.rideId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isDriver) {
            " Votre trajet commence dans 20 minutes"
        } else {
            " Votre trajet part dans 20 minutes"
        }

        val message = if (isDriver) {
            "De ${ride.from} à ${ride.to}\n${ride.passengers.size} passager(s) vous attendent"
        } else {
            "De ${ride.from} à ${ride.to}\nConducteur: ${ride.driverName}"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + ride.rideId.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Rappels de trajet"
            val descriptionText = "Notifications 20 minutes avant le départ"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}