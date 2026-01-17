package isimm.ing1.carpoolingstudents.ui.rides

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.android.material.slider.Slider
import isimm.ing1.carpoolingstudents.R
import isimm.ing1.carpoolingstudents.databinding.ActivityRideDetailBinding
import isimm.ing1.carpoolingstudents.model.Ride
import isimm.ing1.carpoolingstudents.model.RideStatus
import isimm.ing1.carpoolingstudents.model.utils.DateUtils
import isimm.ing1.carpoolingstudents.ui.messages.ChatActivity
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel
import isimm.ing1.carpoolingstudents.viewmodel.RideViewModel

class RideDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRideDetailBinding
    private val rideViewModel: RideViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var currentRide: Ride? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRideDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rideId = intent.getStringExtra("rideId")
        currentUserId = authViewModel.getCurrentUserId()

        if (rideId == null) {
            Toast.makeText(this, "Erreur: trajet non trouvé", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupObservers()

        rideViewModel.loadRideDetails(rideId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Détails du trajet"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        rideViewModel.rideDetails.observe(this) { ride ->
            currentRide = ride
            if (ride != null) {
                displayRideDetails(ride)
            }
        }

        rideViewModel.operationStatus.observe(this) { (success, message) ->
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            if (success) {
                if (message.contains("supprimé")) {
                    finish()
                } else {
                    currentRide?.let { rideViewModel.loadRideDetails(it.rideId) }
                }
            }
        }
    }

    private fun displayRideDetails(ride: Ride) {
        binding.apply {
            fromText.text = ride.from
            toText.text = ride.to
            dateText.text = DateUtils.formatDate(ride.departureTime)
            timeText.text = DateUtils.formatTime(ride.departureTime)
            seatsText.text = "${ride.availableSeats} places disponibles"
            priceText.text = "${ride.pricePerSeat} DT/place"

            driverNameText.text = ride.driverName
            driverPhoneText.text = ride.driverPhone
            driverRatingText.text = "★ ${ride.driverRating}"
            ride.carInfo?.let {
                carInfoText.text = "${it.model} - ${it.color}"
            }

            updateStatusBadge(ride.status)

            val isDriver = currentUserId == ride.driverId
            val isPassenger = ride.passengers.contains(currentUserId)

            when {
                isDriver -> showDriverControls(ride)
                isPassenger -> showPassengerControls(ride)
                else -> showGuestControls(ride)
            }
        }
    }

    private fun updateStatusBadge(status: RideStatus) {
        binding.driverInfoText.visibility = View.VISIBLE
        binding.driverInfoText.text = when (status) {
            RideStatus.AVAILABLE -> " Disponible"
            RideStatus.FULL -> "Complet"
            RideStatus.IN_PROGRESS -> " En cours"
            RideStatus.COMPLETED -> " Terminé"
            RideStatus.CANCELLED -> " Annulé"
        }

        binding.driverInfoText.setTextColor(when (status) {
            RideStatus.AVAILABLE -> 0xFF4CAF50.toInt() // Green
            RideStatus.FULL -> 0xFFFF9800.toInt() // Orange
            RideStatus.IN_PROGRESS -> 0xFF2196F3.toInt() // Blue
            RideStatus.COMPLETED -> 0xFF9E9E9E.toInt() // Gray
            RideStatus.CANCELLED -> 0xFFF44336.toInt() // Red
        })
    }

    private fun showDriverControls(ride: Ride) {
        binding.apply {
            // Reset all buttons
            bookButton.visibility = View.GONE
            cancelButton.visibility = View.GONE
            contactButton.visibility = View.GONE

            when (ride.status) {
                RideStatus.AVAILABLE, RideStatus.FULL -> {
                    val currentTime = System.currentTimeMillis()
                    val timeUntilDeparture = ride.departureTime - currentTime
                    val minutesUntilDeparture = timeUntilDeparture / (1000 * 60)


                    val canStartRide = minutesUntilDeparture <= 15 && minutesUntilDeparture >= -10

                    if (canStartRide) {
                        bookButton.apply {
                            text = " Démarrer le trajet"
                            visibility = View.VISIBLE
                            setOnClickListener { showStartTripConfirmation() }
                        }
                    }

                    cancelButton.apply {
                        text = " Annuler le trajet"
                        visibility = View.VISIBLE
                        setOnClickListener { showCancelTripConfirmation() }
                    }
                }
                RideStatus.IN_PROGRESS -> {
                    bookButton.apply {
                        text = " Terminer le trajet"
                        visibility = View.VISIBLE
                        setOnClickListener { showCompleteTripConfirmation() }
                    }
                }
                RideStatus.COMPLETED, RideStatus.CANCELLED -> {
                    cancelButton.apply {
                        text = "Supprimer le trajet"
                        visibility = View.VISIBLE
                        setOnClickListener { showDeleteRideConfirmation() }
                    }
                }
            }
        }
    }
    private fun showPassengerControls(ride: Ride) {
        binding.apply {
            bookButton.visibility = View.GONE

            when (ride.status) {
                RideStatus.AVAILABLE, RideStatus.FULL -> {
                    cancelButton.apply {
                        text = "Annuler la réservation"
                        visibility = View.VISIBLE
                        setOnClickListener { showCancelBookingConfirmation() }
                    }

                    contactButton.apply {
                        visibility = View.VISIBLE
                        setOnClickListener { openChat() }
                    }
                }
                RideStatus.IN_PROGRESS -> {
                    cancelButton.visibility = View.GONE
                    contactButton.apply {
                        visibility = View.VISIBLE
                        setOnClickListener { openChat() }
                    }
                }
                RideStatus.COMPLETED -> {
                    val hasRated = ride.hasPassengerRated(currentUserId ?: "")

                    if (!hasRated) {
                        bookButton.apply {
                            text = "Évaluer le conducteur"
                            visibility = View.VISIBLE
                            setOnClickListener { showRatingDialog() }
                        }
                    } else {
                        driverInfoText.apply {
                            visibility = View.VISIBLE
                            text = "Vous avez évalué ce trajet: ${ride.ratings[currentUserId]} "
                            setTextColor(0xFF4CAF50.toInt())
                        }
                    }

                    cancelButton.visibility = View.GONE
                    contactButton.visibility = View.GONE
                }
                RideStatus.CANCELLED -> {
                    cancelButton.visibility = View.GONE
                    contactButton.visibility = View.GONE
                }
            }
        }
    }
    private fun showGuestControls(ride: Ride) {
        binding.apply {
            cancelButton.visibility = View.GONE
            contactButton.visibility = View.GONE

            if (ride.status == RideStatus.AVAILABLE && ride.availableSeats > 0) {
                bookButton.apply {
                    text = "Réserver"
                    visibility = View.VISIBLE
                    setOnClickListener { showSeatSelectionDialog() }
                }
            } else {
                bookButton.visibility = View.GONE
            }
        }
    }
    private fun showRatingDialog() {
        val ride = currentRide ?: return

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = android.widget.TextView(this).apply {
            text = "Évaluer le conducteur"
            textSize = 20f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(0, 0, 0, 20)
            gravity = android.view.Gravity.CENTER
        }

        val subtitleText = android.widget.TextView(this).apply {
            text = "Comment s'est passé votre trajet avec ${ride.driverName}?"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            setPadding(0, 0, 0, 30)
            gravity = android.view.Gravity.CENTER
        }

        val ratingBar = android.widget.RatingBar(this, null, android.R.attr.ratingBarStyle).apply {
            numStars = 5
            stepSize = 1f
            rating = 5f
            setPadding(0, 20, 0, 20)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
        }

        val ratingText = android.widget.TextView(this).apply {
            text = "5 étoiles"
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 10, 0, 20)
        }

        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val stars = rating.toInt()
            ratingText.text = "$stars ${if (stars == 1) "étoile" else "étoiles"}"
        }

        container.addView(titleText)
        container.addView(subtitleText)
        container.addView(ratingBar)
        container.addView(ratingText)

        AlertDialog.Builder(this)
            .setView(container)
            .setPositiveButton("Envoyer") { _, _ ->
                val rating = ratingBar.rating.toInt()
                if (rating > 0) {
                    submitRating(rating)
                } else {
                    Toast.makeText(this, "Veuillez donner une note", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }    private fun submitRating(rating: Int) {
        val ride = currentRide ?: return
        val userId = currentUserId ?: return

        binding.progressBar.visibility = View.VISIBLE
        rideViewModel.rateDriver(ride.rideId, userId, rating)
    }

    //========== DRIVER ACTIONS ==========

    private fun showStartTripConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(" Démarrer le trajet")
            .setMessage("Confirmez que vous démarrez le trajet maintenant ?")
            .setPositiveButton("Démarrer") { _, _ ->
                startTrip()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun startTrip() {
        val ride = currentRide ?: return
        binding.progressBar.visibility = View.VISIBLE
        rideViewModel.startTrip(ride.rideId)
    }

    private fun showCompleteTripConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(" Terminer le trajet")
            .setMessage("Confirmez que le trajet est terminé ?")
            .setPositiveButton("Terminer") { _, _ ->
                completeTrip()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun completeTrip() {
        val ride = currentRide ?: return
        binding.progressBar.visibility = View.VISIBLE
        rideViewModel.completeTrip(ride.rideId)
    }

    private fun showCancelTripConfirmation() {
        val ride = currentRide ?: return
        val passengerCount = ride.passengers.size

        AlertDialog.Builder(this)
            .setTitle("Annuler le trajet")
            .setMessage(
                if (passengerCount > 0) {
                    "Les passagers seront notifiés de l'annulation.\n\nÊtes-vous sûr de vouloir annuler ce trajet ?"
                } else {
                    "Voulez-vous annuler ce trajet ?"
                }
            )
            .setPositiveButton("Oui, annuler") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                rideViewModel.cancelTrip(ride.rideId, applicationContext)
            }
            .setNegativeButton("Non", null)
            .show()
    }


    private fun showDeleteRideConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le trajet")
            .setMessage("Cette action est irréversible.\n\nVoulez-vous vraiment supprimer ce trajet ?")
            .setPositiveButton("Supprimer") { _, _ ->
                deleteRide()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteRide() {
        val ride = currentRide ?: return
        binding.progressBar.visibility = View.VISIBLE
        rideViewModel.deleteRide(ride.rideId, applicationContext) // Pass context
    }

    // ========== PASSENGER ACTIONS ==========

    @SuppressLint("MissingInflatedId")
    private fun showSeatSelectionDialog() {
        val ride = currentRide ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_seat_selection, null)
        val slider = dialogView.findViewById<Slider>(R.id.seatSlider)
        val selectedSeatsText = dialogView.findViewById<TextView>(R.id.selectedSeatsText)
        val totalPriceText = dialogView.findViewById<TextView>(R.id.totalPriceText)

        slider.valueTo = ride.availableSeats.toFloat()
        slider.value = 1f

        totalPriceText.text = "${ride.pricePerSeat} DT"

        slider.addOnChangeListener { _, value, _ ->
            val seats = value.toInt()
            selectedSeatsText.text = if (seats == 1) "1 place sélectionnée" else "$seats places sélectionnées"
            totalPriceText.text = "${seats * ride.pricePerSeat} DT"
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Réserver") { _, _ ->
                val selectedSeats = slider.value.toInt()
                showBookConfirmation(selectedSeats)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showBookConfirmation(numberOfSeats: Int) {
        val ride = currentRide ?: return
        val totalPrice = numberOfSeats * ride.pricePerSeat
        val seatsText = if (numberOfSeats == 1) "1 place" else "$numberOfSeats places"

        AlertDialog.Builder(this)
            .setTitle("Confirmer la réservation")
            .setMessage("Voulez-vous réserver $seatsText?\nPrix total: $totalPrice DT")
            .setPositiveButton("Oui") { _, _ ->
                bookRide(numberOfSeats)
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun bookRide(numberOfSeats: Int) {
        val ride = currentRide ?: return
        val userId = currentUserId ?: return

        binding.progressBar.visibility = View.VISIBLE
        rideViewModel.bookRide(ride.rideId, userId, numberOfSeats, applicationContext, ride.departureTime)
    }

    private fun showCancelBookingConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Annuler la réservation")
            .setMessage("Voulez-vous annuler votre réservation?")
            .setPositiveButton("Oui") { _, _ ->
                cancelBooking()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun cancelBooking() {
        val ride = currentRide ?: return
        val userId = currentUserId ?: return

        binding.progressBar.visibility = View.VISIBLE
        rideViewModel.cancelBooking(ride.rideId, userId, applicationContext)
    }

    private fun openChat() {
        val ride = currentRide ?: return
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("userId", ride.driverId)
        intent.putExtra("userName", ride.driverName)
        startActivity(intent)
    }
}