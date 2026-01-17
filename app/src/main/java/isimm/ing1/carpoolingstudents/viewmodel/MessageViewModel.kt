package isimm.ing1.carpoolingstudents.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import isimm.ing1.carpoolingstudents.model.Conversation
import isimm.ing1.carpoolingstudents.model.Message
import isimm.ing1.carpoolingstudents.repository.MessageRepository

class MessageViewModel : ViewModel() {
    private val messageRepository = MessageRepository()


    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _sendStatus = MutableLiveData<Boolean>()
    val sendStatus: LiveData<Boolean> = _sendStatus

    fun loadConversations(userId: String) {
        _isLoading.value = true
        messageRepository.getConversations(userId) { conversations ->
            _conversations.value = conversations
            _isLoading.value = false
        }
    }

    fun loadMessages(userId1: String, userId2: String) {
        messageRepository.getMessages(userId1, userId2) { messages ->
            _messages.value = messages
        }
    }

    fun sendMessage(message: Message) {
        messageRepository.sendMessage(message) { success ->
            _sendStatus.value = success
            if (success) {
                updateConversation(message)
            }
        }
    }

    private fun updateConversation(message: Message) {
        messageRepository.updateConversation(
            senderId = message.senderId,
            receiverId = message.receiverId,
            lastMessage = message.text,
            timestamp = message.timestamp
        ) { success ->
        }
    }

    fun markMessagesAsRead(userId: String, otherUserId: String) {
        messageRepository.markMessagesAsRead(userId, otherUserId) { success ->
        }
    }


}