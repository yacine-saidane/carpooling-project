package isimm.ing1.carpoolingstudents.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import isimm.ing1.carpoolingstudents.databinding.ActivityLoginBinding
import isimm.ing1.carpoolingstudents.ui.home.HomeActivity
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (viewModel.isLoggedIn()) {
            saveFCMToken()
            goToHome()
            return
        }

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.loginStatus.observe(this) { success ->
            binding.progressBar.visibility = View.GONE
            binding.loginButton.isEnabled = true

            if (success) {
                saveFCMToken()
                goToHome()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            binding.progressBar.visibility = View.GONE
            binding.loginButton.isEnabled = true
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (validateInput(email, password)) {
                binding.progressBar.visibility = View.VISIBLE
                binding.loginButton.isEnabled = false
                viewModel.login(email, password)
            }
        }

        binding.registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email requis"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Email invalide"
            return false
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Mot de passe requis"
            return false
        }
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        return true
    }

    /**
     * Save FCM token to Firestore after login
     */
    private fun saveFCMToken() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid


        if (userId == null) {
            return
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                }
                .addOnFailureListener { e ->
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { e2 ->
                        }
                }
        }.addOnFailureListener { e ->
        }
    }
    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}