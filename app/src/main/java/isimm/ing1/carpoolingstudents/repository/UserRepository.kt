package isimm.ing1.carpoolingstudents.repository

import com.google.firebase.firestore.FirebaseFirestore
import isimm.ing1.carpoolingstudents.model.CarInfo
import isimm.ing1.carpoolingstudents.model.User
import isimm.ing1.carpoolingstudents.utils.Constants

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun saveUser(user: User, onResult: (Boolean) -> Unit) {
        val userMap = hashMapOf<String, Any>(
            "userId" to user.userId,
            "email" to user.email,
            "name" to user.name,
            "phone" to user.phone,
            "profilePicUrl" to user.profilePicUrl,
            "isDriver" to user.isDriver,
            "rating" to user.rating,
            "totalRides" to user.totalRides
        )

        user.carInfo?.let { car ->
            userMap["carInfo"] = hashMapOf(
                "model" to car.model,
                "color" to car.color,
                "plateNumber" to car.plateNumber,
                "seats" to car.seats
            )
        }

        firestore.collection(Constants.COLLECTION_USERS)
            .document(user.userId)
            .set(userMap)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getUser(userId: String, onResult: (User?) -> Unit) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    val user = User(
                        userId = data?.get("userId") as? String ?: "",
                        email = data?.get("email") as? String ?: "",
                        name = data?.get("name") as? String ?: "",
                        phone = data?.get("phone") as? String ?: "",
                        profilePicUrl = data?.get("profilePicUrl") as? String ?: "",
                        isDriver = data?.get("isDriver") as? Boolean ?: false,
                        carInfo = (data?.get("carInfo") as? Map<*, *>)?.let {
                            CarInfo(
                                model = it["model"] as? String ?: "",
                                color = it["color"] as? String ?: "",
                                plateNumber = it["plateNumber"] as? String ?: "",
                                seats = (it["seats"] as? Long)?.toInt() ?: 4
                            )
                        },
                        rating = (data?.get("rating") as? Number)?.toDouble() ?: 0.0,
                        totalRides = (data?.get("totalRides") as? Long)?.toInt() ?: 0
                    )
                    onResult(user)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    fun updateUser(userId: String, updates: Map<String, Any>, onResult: (Boolean) -> Unit) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update(updates)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun updateDriverStatus(userId: String, isDriver: Boolean, onResult: (Boolean) -> Unit) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update("isDriver", isDriver)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

}