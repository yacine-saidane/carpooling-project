package isimm.ing1.carpoolingstudents.ui.messages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import isimm.ing1.carpoolingstudents.databinding.FragmentConversationListBinding
import isimm.ing1.carpoolingstudents.ui.messages.adapters.ConversationAdapter
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel
import isimm.ing1.carpoolingstudents.viewmodel.MessageViewModel

class ConversationListFragment : Fragment() {

    private var _binding: FragmentConversationListBinding? = null
    private val binding get() = _binding!!
    private val messageViewModel: MessageViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        loadConversations()
    }

    private fun setupRecyclerView() {
        val currentUserId = authViewModel.getCurrentUserId() ?: ""

        adapter = ConversationAdapter(currentUserId) { conversation ->
            val otherUserId = conversation.participants.find { it != currentUserId } ?: return@ConversationAdapter

            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("userId", otherUserId)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        messageViewModel.conversations.observe(viewLifecycleOwner) { conversations ->
            adapter.submitList(conversations)
        }

        messageViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadConversations() {
        val userId = authViewModel.getCurrentUserId()
        if (userId != null) {
            messageViewModel.loadConversations(userId)
        }
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}