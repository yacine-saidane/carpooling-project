package isimm.ing1.carpoolingstudents.utils

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayInputStream

object FCMHelper {


    private val client = OkHttpClient()
    private val firestore = FirebaseFirestore.getInstance()

    private var cachedAccessToken: String? = null
    private var tokenExpiryTime: Long = 0


    fun sendCancellationNotifications(
        context: Context,
        rideId: String,
        from: String,
        to: String,
        driverName: String,
        passengerIds: List<String>
    ) {

        CoroutineScope(Dispatchers.IO).launch {
            passengerIds.forEach { passengerId ->
                try {

                    val userDoc = firestore.collection("users")
                        .document(passengerId)
                        .get()
                        .await()

                    val fcmToken = userDoc.getString("fcmToken")

                    if (fcmToken != null) {

                        sendNotificationV1(
                            context = context,
                            token = fcmToken,
                            title = " Trajet annulé",
                            body = "Le trajet $from → $to a été annulé par $driverName",
                            rideId = rideId
                        )
                    }
                } catch (e: Exception) {
                }
            }
        }
    }


    private fun sendNotificationV1(
        context: Context,
        token: String,
        title: String,
        body: String,
        rideId: String
    ) {
        try {

            val accessToken = getAccessToken(context)
            if (accessToken == null) {
                return
            }

            val message = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", token)
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                    })
                    put("data", JSONObject().apply {
                        put("rideId", rideId)
                        put("type", "RIDE_CANCELLED")
                    })
                    put("android", JSONObject().apply {
                        put("priority", "high")
                        put("notification", JSONObject().apply {
                            put("channel_id", "ride_notifications")
                            put("sound", "default")
                        })
                    })
                })
            }


            val requestBody = message.toString()
                .toRequestBody("application/json".toMediaType())

            val projectId = getProjectId(context)
            val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                }
            }
        } catch (e: Exception) {
        }
    }


    private fun getAccessToken(context: Context): String? {
        try {
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
                return cachedAccessToken
            }

            val serviceAccountJson = context.assets.open("service-account.json")
                .bufferedReader()
                .use { it.readText() }

            val credentials = GoogleCredentials.fromStream(
                ByteArrayInputStream(serviceAccountJson.toByteArray())
            ).createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

            credentials.refreshIfExpired()

            val accessToken = credentials.accessToken.tokenValue

            cachedAccessToken = accessToken
            tokenExpiryTime = System.currentTimeMillis() + 3000000

            return accessToken

        } catch (e: Exception) {
            return null
        }
    }


    private fun getProjectId(context: Context): String {
        return try {
            val serviceAccountJson = context.assets.open("service-account.json")
                .bufferedReader()
                .use { it.readText() }

            val json = JSONObject(serviceAccountJson)
            json.getString("project_id")
        } catch (e: Exception) {
            "carpooling-students"
        }
    }
}