package isimm.ing1.carpoolingstudents.ui.home

import android.Manifest
import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import isimm.ing1.carpoolingstudents.R
import isimm.ing1.carpoolingstudents.databinding.ActivityHomeBinding
import isimm.ing1.carpoolingstudents.ui.messages.ConversationListFragment
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.SetOptions
import isimm.ing1.carpoolingstudents.ui.profile.ProfileFragment
import isimm.ing1.carpoolingstudents.ui.rides.MyRidesFragment
import isimm.ing1.carpoolingstudents.ui.rides.RideListFragment
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import isimm.ing1.carpoolingstudents.ui.rides.CreateRideActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showNotificationDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_LONG).show()
            return
        }

        if (savedInstanceState == null) {
            loadFragment(RideListFragment())
        }

        setupBottomNavigation()
        setupFab()
        requestNotificationPermission()
        saveFCMTokenManually()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> {
                    loadFragment(RideListFragment())
                    binding.fab.show()
                    true
                }
                R.id.nav_my_rides -> {
                    loadFragment(MyRidesFragment())
                    binding.fab.show()
                    true
                }
                R.id.nav_messages -> {
                    loadFragment(ConversationListFragment())
                    binding.fab.hide()
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    binding.fab.hide()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, CreateRideActivity::class.java))
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showNotificationRationaleDialog()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showNotificationRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("📬 Notifications importantes")
            .setMessage(
                "Nous avons besoin de votre autorisation pour vous envoyer des notifications.\n\n" +
                        "Vous recevrez:\n" +
                        "• Rappels 20 minutes avant vos trajets\n" +
                        "• Notifications de réservation\n" +
                        "• Messages importants\n\n" +
                        "Cela vous aide à ne pas manquer vos trajets!"
            )
            .setPositiveButton("Autoriser") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Plus tard", null)
            .setCancelable(false)
            .show()
    }

    private fun showNotificationDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Notifications désactivées")
            .setMessage(
                "Vous ne recevrez pas de rappels de trajet.\n\n" +
                        "Vous pouvez activer les notifications plus tard dans les paramètres de l'application."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveFCMTokenManually() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            return
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(
                    mapOf("fcmToken" to token),
                    SetOptions.merge()
                )

        }
    }
}