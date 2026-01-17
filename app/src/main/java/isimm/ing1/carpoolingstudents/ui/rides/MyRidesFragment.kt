package isimm.ing1.carpoolingstudents.ui.rides

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import isimm.ing1.carpoolingstudents.databinding.FragmentMyRidesBinding
import isimm.ing1.carpoolingstudents.ui.rides.adapters.RideAdapter
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel
import isimm.ing1.carpoolingstudents.viewmodel.RideViewModel

class MyRidesFragment : Fragment() {

    private var _binding: FragmentMyRidesBinding? = null
    private val binding get() = _binding!!

    private val rideViewModel: RideViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var myRidesAdapter: RideAdapter
    private lateinit var bookedRidesAdapter: RideAdapter



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyRidesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupObservers()
    }

    private fun setupRecyclerViews() {

        myRidesAdapter = RideAdapter { ride ->
            val intent = Intent(requireContext(), RideDetailActivity::class.java)
            intent.putExtra("rideId", ride.rideId)
            startActivity(intent)
        }
        binding.myRidesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myRidesAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
        }

        bookedRidesAdapter = RideAdapter { ride ->
            val intent = Intent(requireContext(), RideDetailActivity::class.java)
            intent.putExtra("rideId", ride.rideId)
            startActivity(intent)
        }
        binding.bookedRidesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookedRidesAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
        }
    }
    private fun setupObservers() {

        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                rideViewModel.loadMyRides(user.userId)
                rideViewModel.loadBookedRides(user.userId)
            }
        }

        rideViewModel.myRides.observe(viewLifecycleOwner) { rides ->

            myRidesAdapter.submitList(rides) {
                binding.myRidesRecyclerView.post {
                    binding.myRidesRecyclerView.requestLayout()
                }
            }

            binding.myRidesEmptyState.visibility = if (rides.isEmpty()) View.VISIBLE else View.GONE
            binding.myRidesRecyclerView.visibility = if (rides.isEmpty()) View.GONE else View.VISIBLE
        }

        rideViewModel.bookedRides.observe(viewLifecycleOwner) { rides ->

            bookedRidesAdapter.submitList(rides) {
                binding.bookedRidesRecyclerView.post {
                    binding.bookedRidesRecyclerView.requestLayout()
                }
            }

            binding.bookedRidesEmptyState.visibility = if (rides.isEmpty()) View.VISIBLE else View.GONE
            binding.bookedRidesRecyclerView.visibility = if (rides.isEmpty()) View.GONE else View.VISIBLE
        }

        rideViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

    }

    private fun loadRides() {
        val userId = authViewModel.getCurrentUserId()

        if (userId != null) {
            rideViewModel.loadMyRides(userId)
            rideViewModel.loadBookedRides(userId)
        } else {
            Toast.makeText(requireContext(), "Veuillez vous connecter", Toast.LENGTH_SHORT).show()

        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}