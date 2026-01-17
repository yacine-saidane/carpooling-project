package isimm.ing1.carpoolingstudents.ui.rides

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import isimm.ing1.carpoolingstudents.databinding.ActivityCreateRideBinding
import isimm.ing1.carpoolingstudents.model.Ride
import isimm.ing1.carpoolingstudents.model.RideStatus
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel
import isimm.ing1.carpoolingstudents.viewmodel.RideViewModel
import isimm.ing1.carpoolingstudents.viewmodel.UserViewModel
import java.util.*
import java.util.concurrent.TimeUnit

class CreateRideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateRideBinding
    private val rideViewModel: RideViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private var selectedDateTime: Long = 0

    companion object {
        private const val TIME_BUFFER_HOURS = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateRideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupObservers()
        setupClickListeners()
        checkDriverStatus()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Créer un trajet"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        rideViewModel.operationStatus.observe(this) { (success, message) ->
            binding.progressBar.visibility = View.GONE
            binding.createButton.isEnabled = true

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (success) {
                finish()
            }
        }

        userViewModel.userProfile.observe(this) { user ->


            user?.let {



                    if (!it.isDriver) {
                        showNotDriverDialog()
                        return@observe
                    }

                if (it.carInfo == null) {
                    showNoCarInfoDialog()
                    return@observe
                }


                binding.driverNameText.text = it.name
                binding.carInfoText.text = "${it.carInfo.model} - ${it.carInfo.seats} places"

                enableForm(true)

                checkExistingRides()
            } ?: run {
                Toast.makeText(this, "Erreur: profil utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        userViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                if (binding.createButton.isEnabled) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }


    }

    private fun setupClickListeners() {
        binding.dateTimeButton.setOnClickListener {
            showDateTimePicker()
        }

        binding.createButton.setOnClickListener {
            createRide()
        }
    }

    private fun checkDriverStatus() {
        val userId = authViewModel.getCurrentUserId()

        if (userId != null) {
            enableForm(false)
            userViewModel.loadUser(userId)
        } else {
            Toast.makeText(this, "Erreur: utilisateur non connecté", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkExistingRides() {
        val userId = authViewModel.getCurrentUserId()
        if (userId != null) {
            rideViewModel.loadMyRides(userId)
        }
    }

    private fun enableForm(enabled: Boolean) {
        binding.fromInput.isEnabled = enabled
        binding.toInput.isEnabled = enabled
        binding.seatsInput.isEnabled = enabled
        binding.priceInput.isEnabled = enabled
        binding.dateTimeButton.isEnabled = enabled
        binding.createButton.isEnabled = enabled

        if (enabled) {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun showNotDriverDialog() {
        AlertDialog.Builder(this)
            .setTitle("Compte conducteur requis")
            .setMessage(
                "Pour créer un trajet, vous devez avoir un compte conducteur.\n\n" +
                        "Veuillez mettre à jour votre profil avec les informations de votre véhicule."
            )
            .setPositiveButton("Mettre à jour le profil") { _, _ ->
                val intent = Intent(this, isimm.ing1.carpoolingstudents.ui.profile.EditProfileActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Annuler") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showNoCarInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("Informations véhicule manquantes")
            .setMessage(
                "Pour créer un trajet, vous devez ajouter les informations de votre véhicule.\n\n" +
                        "Veuillez compléter votre profil conducteur."
            )
            .setPositiveButton("Compléter le profil") { _, _ ->
                val intent = Intent(this, isimm.ing1.carpoolingstudents.ui.profile.EditProfileActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Annuler") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.HOUR_OF_DAY, 1) // At least 1 hour in the future

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        calendar.set(year, month, day, hour, minute)
                        selectedDateTime = calendar.timeInMillis

                        if (selectedDateTime < System.currentTimeMillis()) {
                            Toast.makeText(this, "Veuillez sélectionner une date future", Toast.LENGTH_SHORT).show()
                            selectedDateTime = 0
                            binding.dateTimeText.text = "Sélectionner date et heure"
                        } else {
                            binding.dateTimeText.text =
                                "${day}/${month + 1}/${year} à ${hour}:${String.format("%02d", minute)}"
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.show()
    }

    private fun createRide() {
        val from = binding.fromInput.text.toString().trim()
        val to = binding.toInput.text.toString().trim()
        val seats = binding.seatsInput.text.toString().toIntOrNull() ?: 0
        val price = binding.priceInput.text.toString().toDoubleOrNull() ?: 0.0

        if (!validateInput(from, to, seats, price)) {
            return
        }

        if (!checkTimeConflicts()) {
            return
        }

        val userId = authViewModel.getCurrentUserId()
        val user = userViewModel.userProfile.value


        if (userId == null || user == null) {
            Toast.makeText(this, "Erreur: utilisateur non trouvé", Toast.LENGTH_SHORT).show()
            return
        }

        if (!user.isDriver) {
            Toast.makeText(this, "Erreur: compte conducteur requis", Toast.LENGTH_SHORT).show()
            showNotDriverDialog()
            return
        }

        if (user.carInfo == null) {
            Toast.makeText(this, "Erreur: informations véhicule manquantes", Toast.LENGTH_SHORT).show()
            showNoCarInfoDialog()
            return
        }

        val ride = Ride(
            driverId = userId,
            driverName = user.name,
            driverPhone = user.phone,
            driverRating = user.rating,
            carInfo = user.carInfo,
            from = from,
            to = to,
            departureTime = selectedDateTime,
            availableSeats = seats,
            pricePerSeat = price,
            status = RideStatus.AVAILABLE
        )

        binding.progressBar.visibility = View.VISIBLE
        binding.createButton.isEnabled = false
        rideViewModel.createRide(ride,applicationContext)
    }

    private fun checkTimeConflicts(): Boolean {
        val existingRides = rideViewModel.myRides.value ?: emptyList()
        val currentTime = System.currentTimeMillis()

        val upcomingRides = existingRides.filter {
            it.departureTime > currentTime - TimeUnit.HOURS.toMillis(2) &&
                    it.status != RideStatus.CANCELLED &&
                    it.status != RideStatus.COMPLETED
        }

        val bufferMillis = TimeUnit.HOURS.toMillis(TIME_BUFFER_HOURS.toLong())
        val windowStart = selectedDateTime - bufferMillis
        val windowEnd = selectedDateTime + bufferMillis

        for (existingRide in upcomingRides) {
            if (existingRide.departureTime in windowStart..windowEnd) {
                showTimeConflictDialog(existingRide)
                return false
            }
        }

        return true
    }

    private fun showTimeConflictDialog(conflictingRide: Ride) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = conflictingRide.departureTime

        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val dateStr = "$dayOfMonth/$month/$year à $hour:${String.format("%02d", minute)}"

        val timeDiff = Math.abs(selectedDateTime - conflictingRide.departureTime)
        val hoursDiff = TimeUnit.MILLISECONDS.toHours(timeDiff)
        val minutesDiff = TimeUnit.MILLISECONDS.toMinutes(timeDiff) % 60

        AlertDialog.Builder(this)
            .setTitle("Conflit d'horaire")
            .setMessage(
                "Vous avez déjà un trajet prévu trop proche de cet horaire:\n\n" +
                        "📍 ${conflictingRide.from} → ${conflictingRide.to}\n" +
                        "🕐 $dateStr\n\n" +
                        "Temps entre les trajets: ${hoursDiff}h ${minutesDiff}min\n" +
                        "Minimum requis: $TIME_BUFFER_HOURS heures\n\n" +
                        "Veuillez choisir un horaire avec au moins $TIME_BUFFER_HOURS heures d'écart."
            )
            .setPositiveButton("Compris", null)
            .setNegativeButton("Voir mes trajets") { _, _ ->
                finish()
            }
            .show()
    }

    private fun validateInput(from: String, to: String, seats: Int, price: Double): Boolean {
        val user = userViewModel.userProfile.value

        if (from.isEmpty()) {
            binding.fromLayout.error = "Départ requis"
            return false
        }
        if (to.isEmpty()) {
            binding.toLayout.error = "Destination requise"
            return false
        }
        if (from.equals(to, ignoreCase = true)) {
            binding.toLayout.error = "La destination doit être différente du départ"
            return false
        }
        if (selectedDateTime == 0L) {
            Toast.makeText(this, "Veuillez sélectionner une date et heure", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedDateTime < System.currentTimeMillis()) {
            Toast.makeText(this, "La date doit être dans le futur", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate seats against car capacity
        val maxSeats = user?.carInfo?.seats ?: 8
        if (seats <= 0 || seats > maxSeats) {
            binding.seatsLayout.error = "Nombre de places invalide (1-$maxSeats)"
            return false
        }

        if (price < 0) {
            binding.priceLayout.error = "Prix invalide"
            return false
        }

        binding.fromLayout.error = null
        binding.toLayout.error = null
        binding.seatsLayout.error = null
        binding.priceLayout.error = null
        return true
    }


}