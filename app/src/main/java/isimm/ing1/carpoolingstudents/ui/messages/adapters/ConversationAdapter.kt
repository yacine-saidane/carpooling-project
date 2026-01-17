package isimm.ing1.carpoolingstudents.ui.messages.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import isimm.ing1.carpoolingstudents.databinding.ItemConversationBinding
import isimm.ing1.carpoolingstudents.model.Conversation
import isimm.ing1.carpoolingstudents.model.User
import isimm.ing1.carpoolingstudents.model.utils.DateUtils
import isimm.ing1.carpoolingstudents.utils.Constants

class ConversationAdapter(
    private val currentUserId: String,
    private val onConversationClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    companion object {
        private const val TAG = "ConversationAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            binding.apply {
                val otherUserId = conversation.participants.find { it != currentUserId }

                if (otherUserId != null) {
                    loadUserName(otherUserId) { userName ->
                        userNameText.text = userName
                    }
                } else {
                    Log.e(TAG, "Could not find other user in conversation: ${conversation.conversationId}")
                    userNameText.text = "Utilisateur inconnu"
                }

                lastMessageText.text = conversation.lastMessage
                timeText.text = DateUtils.formatTime(conversation.lastMessageTime)

                root.setOnClickListener {
                    onConversationClick(conversation)
                }
            }
        }

        private fun loadUserName(userId: String, onResult: (String) -> Unit) {
            FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    onResult(user?.name ?: "Utilisateur")
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to load user name for: $userId")
                    onResult("Utilisateur")
                }
        }
    }

    class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.conversationId == newItem.conversationId
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}