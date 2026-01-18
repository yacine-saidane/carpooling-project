package isimm.ing1.carpoolingstudents.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import isimm.ing1.carpoolingstudents.R
import isimm.ing1.carpoolingstudents.ui.rides.RideDetailActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "ride_notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()

        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            return
        }


        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .set(
                mapOf("fcmToken" to token),
                SetOptions.merge()
            )

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)


        val title = message.data["title"] ?: message.notification?.title ?: "Notification"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val rideId = message.data["rideId"]

        showNotification(title, body, rideId)
    }

    private fun showNotification(title: String, body: String, rideId: String?) {
        createNotificationChannel()

        val intent = Intent(this, RideDetailActivity::class.java).apply {
            rideId?.let { putExtra("rideId", it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            rideId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(rideId?.hashCode() ?: 0, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ride Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for ride updates and cancellations"
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}