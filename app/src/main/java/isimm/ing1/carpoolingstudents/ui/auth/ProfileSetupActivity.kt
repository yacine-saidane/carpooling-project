package isimm.ing1.carpoolingstudents.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import isimm.ing1.carpoolingstudents.databinding.ActivityProfileSetupBinding
import isimm.ing1.carpoolingstudents.model.CarInfo
import isimm.ing1.carpoolingstudents.model.User
import isimm.ing1.carpoolingstudents.ui.home.HomeActivity
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel
import isimm.ing1.carpoolingstudents.viewmodel.UserViewModel

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private var userId: String = ""
    private var userEmail: String = ""
    private var userName: String = ""
    private var userPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("userId") ?: authViewModel.getCurrentUserId() ?: ""
        userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        userName = intent.getStringExtra("userName") ?: ""
        userPhone = intent.getStringExtra("userPhone") ?: ""


        if (userId.isEmpty()) {
            Toast.makeText(this, "Erreur: utilisateur non trouvé", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        userViewModel.saveStatus.observe(this) { success ->
            binding.progressBar.visibility = View.GONE
            binding.saveButton.isEnabled = true


            if (success) {
                Toast.makeText(this, "Profil enregistré!", Toast.LENGTH_SHORT).show()
                goToHome()
            } else {
                Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show()
            }
        }

        binding.driverCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.carInfoLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val isDriver = binding.driverCheckbox.isChecked


            if (validateInput(isDriver)) {
                val user = createUser(isDriver)

                binding.progressBar.visibility = View.VISIBLE
                binding.saveButton.isEnabled = false
                userViewModel.saveUser(user)
            }
        }
    }

    private fun validateInput(isDriver: Boolean): Boolean {
        if (isDriver) {
            val carModel = binding.carModelInput.text.toString().trim()
            val carSeats = binding.carSeatsInput.text.toString().trim()

            if (carModel.isEmpty()) {
                binding.carModelLayout.error = "Modèle de voiture requis"
                return false
            }
            if (carSeats.isEmpty() || carSeats.toIntOrNull() == null) {
                binding.carSeatsLayout.error = "Nombre de places requis"
                return false
            }
        }

        binding.carModelLayout.error = null
        binding.carSeatsLayout.error = null
        return true
    }

    private fun createUser(isDriver: Boolean): User {
        val carInfo = if (isDriver) {
            CarInfo(
                model = binding.carModelInput.text.toString().trim(),
                color = binding.carColorInput.text.toString().trim(),
                plateNumber = binding.carPlateInput.text.toString().trim(),
                seats = binding.carSeatsInput.text.toString().toIntOrNull() ?: 4
            )
        } else null

        return User(
            userId = userId,
            email = userEmail,
            name = userName,
            phone = userPhone,
            isDriver = isDriver,
            carInfo = carInfo
        )
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}