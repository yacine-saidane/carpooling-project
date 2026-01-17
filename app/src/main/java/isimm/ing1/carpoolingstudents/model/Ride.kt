package isimm.ing1.carpoolingstudents.model

data class Ride(
    val rideId: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val from: String = "",
    val to: String = "",
    val departureTime: Long = 0,
    val availableSeats: Int = 0,
    val pricePerSeat: Double = 0.0,
    val passengers: List<String> = emptyList(),
    val passengerSeats: Map<String, Int> = emptyMap(),
    val status: RideStatus = RideStatus.AVAILABLE,
    val driverRating: Double = 0.0,
    val carInfo: CarInfo? = null,
    val driverPhone: String = "",
    val ratings: Map<String, Int> = emptyMap(),
    val hasRated: List<String> = emptyList()
) {


    fun hasPassengerRated(passengerId: String): Boolean {
        return hasRated.contains(passengerId)
    }


}

enum class RideStatus {
    AVAILABLE,
    FULL,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,

}