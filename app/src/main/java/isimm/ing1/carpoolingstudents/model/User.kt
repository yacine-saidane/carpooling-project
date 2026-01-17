package isimm.ing1.carpoolingstudents.model

data class User(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val profilePicUrl: String = "",
    val isDriver: Boolean = false,
    val carInfo: CarInfo? = null,
    val rating: Double = 0.0,
    val totalRides: Int = 0,
    val fcmToken: String = ""
) {
    constructor() : this("", "", "", "", "", false, null, 0.0, 0)
}