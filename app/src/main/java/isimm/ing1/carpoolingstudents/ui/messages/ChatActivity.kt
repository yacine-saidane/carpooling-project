package isimm.ing1.carpoolingstudents.ui.messages

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import isimm.ing1.carpoolingstudents.databinding.ActivityChatBinding
import isimm.ing1.carpoolingstudents.model.Message
import isimm.ing1.carpoolingstudents.ui.messages.adapters.MessageAdapter
import isimm.ing1.carpoolingstudents.viewmodel.AuthViewModel
import isimm.ing1.carpoolingstudents.viewmodel.MessageViewModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val messageViewModel: MessageViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private var otherUserId: String = ""
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        otherUserId = intent.getStringExtra("userId") ?: ""
        val otherUserName = intent.getStringExtra("userName") ?: "Chat"
        currentUserId = authViewModel.getCurrentUserId() ?: ""

        if (otherUserId.isEmpty() || currentUserId.isEmpty()) {
            finish()
            return
        }

        setupToolbar(otherUserName)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        loadMessages()
        markMessagesAsRead()
    }

    private fun setupToolbar(userName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = userName
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(currentUserId)
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messagesRecyclerView.adapter = adapter
    }

    private fun setupObservers() {
        messageViewModel.messages.observe(this) { messages ->
            adapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }

        }

        messageViewModel.sendStatus.observe(this) { success ->
            if (success) {
                binding.messageInput.text?.clear()
            }
        }
    }

    private fun setupClickListeners() {
        binding.sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadMessages() {
        messageViewModel.loadMessages(currentUserId, otherUserId)
    }

    private fun markMessagesAsRead() {
        messageViewModel.markMessagesAsRead(currentUserId, otherUserId)
    }

    private fun sendMessage() {
        val text = binding.messageInput.text.toString().trim()

        if (text.isEmpty()) {
            return
        }

        val message = Message(
            senderId = currentUserId,
            receiverId = otherUserId,
            text = text,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        messageViewModel.sendMessage(message)
    }
}