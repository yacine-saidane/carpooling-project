package isimm.ing1.carpoolingstudents.ui.messages.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import isimm.ing1.carpoolingstudents.databinding.ItemMessageBinding
import isimm.ing1.carpoolingstudents.model.Message
import isimm.ing1.carpoolingstudents.model.utils.DateUtils

class MessageAdapter(
    private val currentUserId: String
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val currentMessage = getItem(position)
        val previousMessage = if (position > 0) getItem(position - 1) else null
        holder.bind(currentMessage, previousMessage)
    }

    inner class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, previousMessage: Message?) {
            val isSentByMe = message.senderId == currentUserId

            val shouldShowTimestamp = shouldShowTimestamp(message, previousMessage)

            binding.apply {
                if (isSentByMe) {
                    sentMessageCard.visibility = View.VISIBLE
                    receivedMessageCard.visibility = View.GONE

                    sentMessageText.text = message.text
                    sentTimeText.text = DateUtils.formatTime(message.timestamp)

                    if (message.isRead) {
                        readStatusIcon.visibility = View.VISIBLE
                         readStatusIcon.setImageResource(isimm.ing1.carpoolingstudents.R.drawable.ic_double_check)
                    } else {
                        readStatusIcon.visibility = View.VISIBLE
                        readStatusIcon.setImageResource(isimm.ing1.carpoolingstudents.R.drawable.ic_check)
                    }

                    val layoutParams = sentMessageCard.layoutParams as ViewGroup.MarginLayoutParams
                    if (shouldShowTimestamp) {
                        layoutParams.topMargin = 8
                    } else {
                        layoutParams.topMargin = 2
                    }
                    sentMessageCard.layoutParams = layoutParams

                } else {
                    sentMessageCard.visibility = View.GONE
                    receivedMessageCard.visibility = View.VISIBLE

                    receivedMessageText.text = message.text
                    receivedTimeText.text = DateUtils.formatTime(message.timestamp)

                    val layoutParams = receivedMessageCard.layoutParams as ViewGroup.MarginLayoutParams
                    if (shouldShowTimestamp) {
                        layoutParams.topMargin = 8
                    } else {
                        layoutParams.topMargin = 2
                    }
                    receivedMessageCard.layoutParams = layoutParams
                }
            }
        }

        private fun shouldShowTimestamp(currentMessage: Message, previousMessage: Message?): Boolean {
            if (previousMessage == null) return true

            if (currentMessage.senderId != previousMessage.senderId) return true

            val timeDifference = currentMessage.timestamp - previousMessage.timestamp
            return timeDifference > 60000 // 1 minute in milliseconds
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}