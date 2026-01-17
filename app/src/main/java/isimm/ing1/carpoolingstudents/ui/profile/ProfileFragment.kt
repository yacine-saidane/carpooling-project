package isimm.ing1.carpoolingstudents.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import isimm.ing1.carpoolingstudents.databinding.FragmentProfileBinding
import isimm.ing1.carpoolingstudents.ui.auth.LoginActivity
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel
import isimm.ing1.carpoolingstudents.viewmodel.UserViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
        loadUserProfile()
    }

    private fun setupObservers() {
        userViewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.nameText.text = it.name
                binding.emailText.text = it.email
                binding.phoneText.text = it.phone
                binding.ratingText.text = "★ ${it.rating}"
                binding.totalRidesText.text = "${it.totalRides} "

                if (it.isDriver) {
                    binding.driverBadge.visibility = View.VISIBLE
                    binding.carInfoLayout.visibility = View.VISIBLE
                    it.carInfo?.let { car ->
                        binding.carModelText.text = car.model
                        binding.carColorText.text = car.color
                        binding.carSeatsText.text = "${car.seats} places"
                    }
                } else {
                    binding.driverBadge.visibility = View.GONE
                    binding.carInfoLayout.visibility = View.GONE
                }
            }
        }

        userViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.editProfileButton.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserProfile() {
        val userId = authViewModel.getCurrentUserId()
        if (userId != null) {
            userViewModel.loadUser(userId)
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous vous déconnecter?")
            .setPositiveButton("Oui") { _, _ ->
                logout()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun logout() {
        authViewModel.logout()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}