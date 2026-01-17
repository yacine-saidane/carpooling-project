// Update RideViewModel.kt - Add status change methods

package isimm.ing1.carpoolingstudents.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import isimm.ing1.carpoolingstudents.model.Ride
import isimm.ing1.carpoolingstudents.model.RideStatus
import isimm.ing1.carpoolingstudents.repository.RideRepository

class RideViewModel : ViewModel() {
    private val rideRepository = RideRepository()

    private val _availableRides = MutableLiveData<List<Ride>>()
    val availableRides: LiveData<List<Ride>> = _availableRides

    private val _myRides = MutableLiveData<List<Ride>>()
    val myRides: LiveData<List<Ride>> = _myRides

    private val _bookedRides = MutableLiveData<List<Ride>>()
    val bookedRides: LiveData<List<Ride>> = _bookedRides

    private val _rideDetails = MutableLiveData<Ride?>()
    val rideDetails: LiveData<Ride?> = _rideDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationStatus = MutableLiveData<Pair<Boolean, String>>()
    val operationStatus: LiveData<Pair<Boolean, String>> = _operationStatus

    fun loadAvailableRides(context: Context) {
        _isLoading.value = true
        rideRepository.getAvailableRides(context) { rides ->
            _availableRides.value = rides
            _isLoading.value = false
        }
    }
    fun loadMyRides(driverId: String) {
        _isLoading.value = true
        rideRepository.getRidesByDriver(driverId) { rides ->
            _myRides.value = rides
            _isLoading.value = false
        }
    }


    fun loadBookedRides(passengerId: String) {

        _isLoading.value = true
        rideRepository.getRidesByPassenger(passengerId) { rides ->
            _bookedRides.value = rides
            _isLoading.value = false
        }
    }

    fun createRide(ride: Ride, context: Context) {
        _isLoading.value = true
        rideRepository.createRide(ride, context) { success ->
            _isLoading.value = false
            if (success) {
                _operationStatus.value = Pair(true, "Trajet créé avec succès")
                loadMyRides(ride.driverId)
            } else {
                _operationStatus.value = Pair(false, "Erreur lors de la création")
            }
        }
    }
    fun bookRide(rideId: String, passengerId: String, numberOfSeats: Int, context: Context, departureTime: Long) {
        _isLoading.value = true
        rideRepository.bookRide(rideId, passengerId, numberOfSeats, context, departureTime) { success ->
            _isLoading.value = false
            if (success) {
                val seatsText = if (numberOfSeats == 1) "place réservée" else "places réservées"
                _operationStatus.value = Pair(true, "$numberOfSeats $seatsText avec succès")
                loadAvailableRides(context)
                loadRideDetails(rideId)
            } else {
                _operationStatus.value = Pair(false, "Erreur lors de la réservation")
            }
        }
    }
    fun cancelBooking(rideId: String, passengerId: String, context: Context) {
        _isLoading.value = true
        rideRepository.cancelBooking(rideId, passengerId, context) { success ->
            _isLoading.value = false
            if (success) {
                _operationStatus.value = Pair(true, "Réservation annulée")
                loadBookedRides(passengerId)
                loadRideDetails(rideId)
            } else {
                _operationStatus.value = Pair(false, "Erreur lors de l'annulation")
            }
        }
    }

    fun loadRideDetails(rideId: String) {
        rideRepository.getRide(rideId) { ride ->
            _rideDetails.value = ride
        }
    }

    fun searchRides(destination: String) {
        _isLoading.value = true
        rideRepository.searchRidesByDestination(destination) { rides ->
            _availableRides.value = rides
            _isLoading.value = false
        }
    }


    // ========== MANUAL STATUS MANAGEMENT ==========

    fun startTrip(rideId: String) {
        _isLoading.value = true
        rideRepository.updateRideStatus(rideId, RideStatus.IN_PROGRESS) { success ->
            _isLoading.value = false
            if (success) {
                _operationStatus.value = Pair(true, "Trajet démarré")
                loadRideDetails(rideId)
            } else {
                _operationStatus.value = Pair(false, "Erreur lors du démarrage")
            }
        }
    }

    fun completeTrip(rideId: String) {
        _isLoading.value = true
        rideRepository.updateRideStatus(rideId, RideStatus.COMPLETED) { success ->
            _isLoading.value = false
            if (success) {
                _operationStatus.value = Pair(true, "Trajet terminé")
                loadRideDetails(rideId)
            } else {
                _operationStatus.value = Pair(false, "Erreur lors de la finalisation")
            }
        }
    }

    fun cancelTrip(rideId: String, context: Context) {
        _isLoading.value = true
        rideRepository.cancelTrip(rideId, context) { success ->
            _isLoading.value = false
            if (success) {
                _operationStatus.value = Pair(true, "Trajet annulé - Passagers notifiés")
                loadRideDetails(rideId)
            } else {
                _operationStatus.value = Pair(false, "Erreur lors de l'annulation")
            }
        }
    }

    fun deleteRide(rideId: String, context: Context) {
        _isLoading.value = true
        rideRepository.deleteRide(rideId, context) { success ->
            _isLoading.value = false
            if (success) {
                _operationStatus.value = Pair(true, "Trajet supprimé")
            } else {
                _operationStatus.value = Pair(false, "Erreur lors de la suppression")
            }
        }
    }
    fun rateDriver(rideId: String, passengerId: String, rating: Int) {
        _isLoading.value = true
        rideRepository.rateDriver(rideId, passengerId, rating) { success ->
            _isLoading.value = false
            if (success) {
                _operationStatus.value = Pair(true, "Évaluation envoyée avec succès")
                loadRideDetails(rideId)
            } else {
                _operationStatus.value = Pair(false, "Erreur lors de l'évaluation")
            }
        }
    }
}