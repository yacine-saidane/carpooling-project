package isimm.ing1.carpoolingstudents.ui.rides

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import isimm.ing1.carpoolingstudents.databinding.FragmentRideListBinding
import isimm.ing1.carpoolingstudents.ui.rides.adapters.RideAdapter
import isimm.ing1.carpoolingstudents.viewmodel.RideViewModel

class RideListFragment : Fragment() {

    private var _binding: FragmentRideListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RideViewModel by viewModels()
    private lateinit var adapter: RideAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRideListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupSearchButton()

        viewModel.loadAvailableRides(requireContext())
    }

    private fun setupRecyclerView() {
        adapter = RideAdapter { ride ->
            val intent = Intent(requireContext(), RideDetailActivity::class.java)
            intent.putExtra("rideId", ride.rideId)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.availableRides.observe(viewLifecycleOwner) { rides ->
            adapter.submitList(rides)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupSearchButton() {
        binding.searchButton.setOnClickListener {
            val destination = binding.searchInput.text.toString().trim()
            if (destination.isNotEmpty()) {
                viewModel.searchRides(destination)
            } else {
                viewModel.loadAvailableRides(requireContext())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAvailableRides(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}