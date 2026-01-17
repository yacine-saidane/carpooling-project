package isimm.ing1.carpoolingstudents.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import isimm.ing1.carpoolingstudents.databinding.ActivityEditProfileBinding
import isimm.ing1.carpoolingstudents.model.CarInfo
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel
import isimm.ing1.carpoolingstudents.viewmodel.UserViewModel

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupObservers()
        setupClickListeners()
        loadUserData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Modifier le profil"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        userViewModel.userProfile.observe(this) { user ->
            user?.let {
                binding.nameInput.setText(it.name)
                binding.phoneInput.setText(it.phone)
                binding.driverCheckbox.isChecked = it.isDriver

                binding.carInfoLayout.visibility = if (it.isDriver) View.VISIBLE else View.GONE

                it.carInfo?.let { car ->
                    binding.carModelInput.setText(car.model)
                    binding.carColorInput.setText(car.color)
                    binding.carPlateInput.setText(car.plateNumber)
                    binding.carSeatsInput.setText(car.seats.toString())
                }
            }
        }

        userViewModel.saveStatus.observe(this) { success ->
            binding.progressBar.visibility = View.GONE
            binding.saveButton.isEnabled = true

            if (success) {
                Toast.makeText(this, "Profil mis à jour!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show()
            }
        }

        userViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        binding.driverCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.carInfoLayout.visibility = if (isChecked) View.VISIBLE else View.GONE

            if (!isChecked) {
                binding.carModelInput.setText("")
                binding.carColorInput.setText("")
                binding.carPlateInput.setText("")
                binding.carSeatsInput.setText("")
            }
        }
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        val userId = authViewModel.getCurrentUserId()
        if (userId != null) {
            userViewModel.loadUser(userId)
        } else {
            Toast.makeText(this, "Erreur: utilisateur non connecté", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveProfile() {
        val name = binding.nameInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val isDriver = binding.driverCheckbox.isChecked

        if (!validateBasicInfo(name, phone)) {
            return
        }

        val userId = authViewModel.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Erreur: utilisateur non connecté", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "isDriver" to isDriver
        )

        if (isDriver) {
            val carInfo = validateAndGetCarInfo()
            if (carInfo == null) {
                return
            }

            updates["carInfo"] = hashMapOf(
                "model" to carInfo.model,
                "color" to carInfo.color,
                "plateNumber" to carInfo.plateNumber,
                "seats" to carInfo.seats
            )
        } else {
            updates["carInfo"] = FieldValue.delete()
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false
        userViewModel.updateUser(userId, updates)
    }

    private fun validateBasicInfo(name: String, phone: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.nameLayout.error = "Nom requis"
            isValid = false
        } else {
            binding.nameLayout.error = null
        }

        if (phone.isEmpty()) {
            binding.phoneLayout.error = "Téléphone requis"
            isValid = false
        } else if (phone.length < 8) {
            binding.phoneLayout.error = "Numéro invalide"
            isValid = false
        } else {
            binding.phoneLayout.error = null
        }

        return isValid
    }

    private fun validateAndGetCarInfo(): CarInfo? {
        val carModel = binding.carModelInput.text.toString().trim()
        val carColor = binding.carColorInput.text.toString().trim()
        val carPlate = binding.carPlateInput.text.toString().trim()
        val carSeatsText = binding.carSeatsInput.text.toString().trim()

        var isValid = true

        if (carModel.isEmpty()) {
            binding.carModelLayout.error = "Modèle requis"
            isValid = false
        } else {
            binding.carModelLayout.error = null
        }

        if (carColor.isEmpty()) {
            binding.carColorLayout.error = "Couleur requise"
            isValid = false
        } else {
            binding.carColorLayout.error = null
        }

        if (carPlate.isEmpty()) {
            binding.carPlateLayout.error = "Matricule requis"
            isValid = false
        } else {
            binding.carPlateLayout.error = null
        }

        val carSeats = carSeatsText.toIntOrNull()
        if (carSeats == null || carSeats < 1 || carSeats > 8) {
            binding.carSeatsLayout.error = "Places invalides (1-8)"
            isValid = false
        } else {
            binding.carSeatsLayout.error = null
        }

        if (!isValid) {
            return null
        }

        return CarInfo(
            model = carModel,
            color = carColor,
            plateNumber = carPlate,
            seats = carSeats ?: 4
        )
    }
}