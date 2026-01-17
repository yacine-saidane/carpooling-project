package isimm.ing1.carpoolingstudents.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import isimm.ing1.carpoolingstudents.databinding.ActivityRegisterBinding
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)



        setupObservers()
        setupClickListeners()
    }


    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val phone = binding.phoneInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            if (validateInput(name, email, phone, password, confirmPassword)) {
                binding.progressBar.visibility = View.VISIBLE
                binding.registerButton.isEnabled = false

                viewModel.register(email, password)
            }
        }
        binding.loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.registerStatus.observe(this) { (success, userId) ->
            binding.progressBar.visibility = View.GONE
            binding.registerButton.isEnabled = true

            if (success && userId != null) {
                val intent = Intent(this, ProfileSetupActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userName", binding.nameInput.text.toString().trim())
                intent.putExtra("userPhone", binding.phoneInput.text.toString().trim())
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Erreur lors de l'inscription", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            binding.nameLayout.error = "Nom requis"
            return false
        }
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email requis"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Email invalide"
            return false
        }
        if (phone.isEmpty()) {
            binding.phoneLayout.error = "Téléphone requis"
            return false
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Mot de passe requis"
            return false
        }
        if (password.length < 6) {
            binding.passwordLayout.error = "Minimum 6 caractères"
            return false
        }
        if (password != confirmPassword) {
            binding.confirmPasswordLayout.error = "Mots de passe différents"
            return false
        }

        binding.nameLayout.error = null
        binding.emailLayout.error = null
        binding.phoneLayout.error = null
        binding.passwordLayout.error = null
        binding.confirmPasswordLayout.error = null
        return true
    }
}