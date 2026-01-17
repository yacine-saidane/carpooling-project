package isimm.ing1.carpoolingstudents.repository

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import isimm.ing1.carpoolingstudents.model.Ride
import isimm.ing1.carpoolingstudents.model.RideStatus
import isimm.ing1.carpoolingstudents.model.utils.DateUtils
import isimm.ing1.carpoolingstudents.utils.Constants
import isimm.ing1.carpoolingstudents.utils.FCMHelper
import isimm.ing1.carpoolingstudents.utils.NotificationScheduler
import java.util.concurrent.TimeUnit

class RideRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun cancelTrip(rideId: String, context: Context, onResult: (Boolean) -> Unit) {
        val rideRef = firestore.collection(Constants.COLLECTION_RIDES).document(rideId)

        rideRef.get()
            .addOnSuccessListener { document ->
                val ride = document.toObject(Ride::class.java)

                if (ride != null) {
                    rideRef.update("status", RideStatus.CANCELLED.name)
                        .addOnSuccessListener {
                            NotificationScheduler.cancelAllNotificationsForRide(context, rideId)

                            FCMHelper.sendCancellationNotifications(
                                context = context,
                                rideId = ride.rideId,
                                from = ride.from,
                                to = ride.to,
                                driverName = ride.driverName,
                                passengerIds = ride.passengers)

                            onResult(true)
                        }
                        .addOnFailureListener {
                            onResult(false)
                        }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener {
                Log.e("RideRepository", "Failed to get ride", it)
                onResult(false)
            }
    }


    fun createRide(ride: Ride, context: Context, onResult: (Boolean) -> Unit) {
        val rideId = firestore.collection(Constants.COLLECTION_RIDES).document().id
        val newRide = ride.copy(rideId = rideId)

        firestore.collection(Constants.COLLECTION_RIDES)
            .document(rideId)
            .set(newRide)
            .addOnSuccessListener {
                NotificationScheduler.scheduleRideNotification(
                    context = context,
                    rideId = rideId,
                    userId = newRide.driverId,
                    isDriver = true,
                    departureTime = newRide.departureTime
                )

                onResult(true)
            }
            .addOnFailureListener { e ->
                onResult(false)
            }
    }
    fun getAvailableRides(context: Context, onResult: (List<Ride>) -> Unit) {
        autoCancelLateRides(context) {
            val currentTime = System.currentTimeMillis()

            firestore.collection(Constants.COLLECTION_RIDES)
                .whereEqualTo("status", RideStatus.AVAILABLE.name)
                .whereGreaterThan("departureTime", currentTime)
                .orderBy("departureTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val rides = documents.mapNotNull {
                        it.toObject(Ride::class.java)
                    }
                    onResult(rides)
                }
                .addOnFailureListener { e ->
                    onResult(emptyList())
                }
        }
    }

    fun searchRidesByDestination(destination: String, onResult: (List<Ride>) -> Unit) {
        val currentTime = System.currentTimeMillis()

        firestore.collection(Constants.COLLECTION_RIDES)
            .whereEqualTo("status", RideStatus.AVAILABLE.name)
            .whereEqualTo("to", destination)
            .whereGreaterThan("departureTime", currentTime)
            .orderBy("departureTime", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val rides = documents.mapNotNull { it.toObject(Ride::class.java) }
                onResult(rides)
            }
            .addOnFailureListener { e ->
                onResult(emptyList())
            }
    }

    fun getRidesByDriver(driverId: String, onResult: (List<Ride>) -> Unit) {
        firestore.collection(Constants.COLLECTION_RIDES)
            .whereEqualTo("driverId", driverId)
            .orderBy("departureTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val rides = documents.mapNotNull { it.toObject(Ride::class.java) }
                onResult(rides)
            }
            .addOnFailureListener { e ->
                onResult(emptyList())
            }
    }

    fun getRidesByPassenger(passengerId: String, onResult: (List<Ride>) -> Unit) {
        firestore.collection(Constants.COLLECTION_RIDES)
            .whereArrayContains("passengers", passengerId)
            .orderBy("departureTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val rides = documents.mapNotNull { it.toObject(Ride::class.java) }
                onResult(rides)
            }
            .addOnFailureListener { e ->
                onResult(emptyList())
            }
    }

    fun bookRide(rideId: String, passengerId: String, numberOfSeats: Int, context: Context, departureTime: Long, onResult: (Boolean) -> Unit) {
        val rideRef = firestore.collection(Constants.COLLECTION_RIDES).document(rideId)
        firestore.runTransaction { transaction ->
            val ride = transaction.get(rideRef).toObject(Ride::class.java)

            if (ride != null &&
                ride.availableSeats >= numberOfSeats &&
                !ride.passengers.contains(passengerId)) {

                val updatedPassengers = ride.passengers + passengerId
                val updatedPassengerSeats = ride.passengerSeats.toMutableMap()
                updatedPassengerSeats[passengerId] = numberOfSeats

                val updatedSeats = ride.availableSeats - numberOfSeats
                val newStatus = if (updatedSeats == 0) RideStatus.FULL else RideStatus.AVAILABLE

                transaction.update(rideRef, mapOf(
                    "passengers" to updatedPassengers,
                    "passengerSeats" to updatedPassengerSeats,
                    "availableSeats" to updatedSeats,
                    "status" to newStatus.name
                ))
            } else {
                throw Exception("Pas assez de places disponibles")
            }
        }
            .addOnSuccessListener {
                NotificationScheduler.scheduleRideNotification(
                    context = context,
                    rideId = rideId,
                    userId = passengerId,
                    isDriver = false,
                    departureTime = departureTime
                )
                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun cancelBooking(
        rideId: String,
        passengerId: String,
        context: Context,
        onResult: (Boolean) -> Unit
    ) {
        val rideRef = firestore.collection(Constants.COLLECTION_RIDES).document(rideId)

        firestore.runTransaction { transaction ->
            val ride = transaction.get(rideRef).toObject(Ride::class.java)

            if (ride != null && ride.passengers.contains(passengerId)) {
                val updatedPassengers = ride.passengers - passengerId
                val seatsToReturn = ride.passengerSeats[passengerId] ?: 1
                val updatedPassengerSeats = ride.passengerSeats.toMutableMap()
                updatedPassengerSeats.remove(passengerId)

                val updatedSeats = ride.availableSeats + seatsToReturn
                val newStatus = if (updatedSeats > 0) RideStatus.AVAILABLE else ride.status

                transaction.update(rideRef, mapOf(
                    "passengers" to updatedPassengers,
                    "passengerSeats" to updatedPassengerSeats,
                    "availableSeats" to updatedSeats,
                    "status" to newStatus.name
                ))
            }
        }
            .addOnSuccessListener {
                NotificationScheduler.cancelRideNotification(context, rideId, passengerId)

                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun getRide(rideId: String, onResult: (Ride?) -> Unit) {
        firestore.collection(Constants.COLLECTION_RIDES)
            .document(rideId)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.toObject(Ride::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }



    fun deleteRide(rideId: String, context: Context, onResult: (Boolean) -> Unit) {
        firestore.collection(Constants.COLLECTION_RIDES)
            .document(rideId)
            .delete()
            .addOnSuccessListener {
                // Cancel all notifications for this ride
                NotificationScheduler.cancelAllNotificationsForRide(context, rideId)

                onResult(true)
            }
            .addOnFailureListener { onResult(false) }
    }
    fun updateRideStatus(rideId: String, status: RideStatus, onResult: (Boolean) -> Unit) {
        firestore.collection(Constants.COLLECTION_RIDES)
            .document(rideId)
            .update("status", status.name)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
    fun rateDriver(rideId: String, passengerId: String, rating: Int, onResult: (Boolean) -> Unit) {
        val rideRef = firestore.collection(Constants.COLLECTION_RIDES).document(rideId)

        firestore.runTransaction { transaction ->
            val ride = transaction.get(rideRef).toObject(Ride::class.java)

            if (ride != null &&
                ride.passengers.contains(passengerId) &&
                !ride.hasRated.contains(passengerId) &&
                ride.status == RideStatus.COMPLETED) {

                val updatedRatings = ride.ratings.toMutableMap()
                updatedRatings[passengerId] = rating

                val updatedHasRated = ride.hasRated + passengerId

                transaction.update(rideRef, mapOf(
                    "ratings" to updatedRatings,
                    "hasRated" to updatedHasRated
                ))

                ride.driverId
            } else {
                throw Exception("Cannot rate this ride")
            }
        }
            .addOnSuccessListener { driverId ->
                updateDriverRating(driverId as String, rating, onResult)
            }
            .addOnFailureListener {e->
                onResult(false)
            }
    }

    private fun updateDriverRating(driverId: String, newRating: Int, onResult: (Boolean) -> Unit) {
        val userRef = firestore.collection(Constants.COLLECTION_USERS).document(driverId)

        firestore.runTransaction { transaction ->
            val document = transaction.get(userRef)
            val currentRating = (document.getDouble("rating") ?: 0.0)
            val totalRides = (document.getLong("totalRides") ?: 0L).toInt()

            val newAverage = if (totalRides == 0) {
                newRating.toDouble()
            } else {
                ((currentRating * totalRides) + newRating) / (totalRides + 1)
            }

            transaction.update(userRef, mapOf(
                "rating" to newAverage,
                "totalRides" to totalRides + 1
            ))
        }
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
    private fun autoCancelLateRides(context: Context, onComplete: () -> Unit) {
        val now = System.currentTimeMillis()
        val gracePeriod = TimeUnit.MINUTES.toMillis(5) // 5 minutes
        val cutoffTime = now - gracePeriod

        firestore.collection(Constants.COLLECTION_RIDES)
            .whereIn("status", listOf(RideStatus.AVAILABLE.name, RideStatus.FULL.name))
            .whereLessThan("departureTime", cutoffTime)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onComplete()
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                val cancelledRides = mutableListOf<Ride>()

                documents.forEach { doc ->
                    val ride = doc.toObject(Ride::class.java)
                    batch.update(doc.reference, "status", RideStatus.CANCELLED.name)
                    cancelledRides.add(ride)
                }

                batch.commit()
                    .addOnSuccessListener {

                        cancelledRides.forEach { ride ->
                            NotificationScheduler.cancelAllNotificationsForRide(context, ride.rideId)

                            ride.passengers.forEach { passengerId ->
                                sendLateCancellationNotification(context, ride, passengerId)
                            }
                        }

                        onComplete()
                    }
                    .addOnFailureListener {
                        onComplete()
                    }
            }
            .addOnFailureListener {
                Log.e("RideRepository", "Failed to query late rides", it)
                onComplete()
            }
    }

    private fun sendLateCancellationNotification(context: Context, ride: Ride, passengerId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ride_cancellations",
                "Annulations de trajets",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications d'annulation de trajets"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "ride_cancellations")
            .setSmallIcon(isimm.ing1.carpoolingstudents.R.drawable.ic_notification)
            .setContentTitle("❌ Trajet annulé automatiquement")
            .setContentText("Le trajet ${ride.from} → ${ride.to} n'a pas démarré")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(
                    "Le trajet ${ride.from} → ${ride.to} prévu pour ${DateUtils.formatTime(ride.departureTime)} " +
                            "a été annulé automatiquement car le conducteur ne l'a pas démarré."
                ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationId = (ride.rideId + passengerId + "autocancel").hashCode()
        notificationManager.notify(notificationId, notification)
    }

}