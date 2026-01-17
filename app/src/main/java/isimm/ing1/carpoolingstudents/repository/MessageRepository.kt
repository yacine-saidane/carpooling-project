package isimm.ing1.carpoolingstudents.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import isimm.ing1.carpoolingstudents.model.Conversation
import isimm.ing1.carpoolingstudents.model.Message
import isimm.ing1.carpoolingstudents.utils.Constants

class MessageRepository {
    private val firestore = FirebaseFirestore.getInstance()



    fun getConversations(userId: String, onResult: (List<Conversation>) -> Unit) {

        firestore.collection(Constants.COLLECTION_CONVERSATIONS)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Conversation::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()


                onResult(conversations)
            }
    }

    fun getMessages(userId: String, otherUserId: String, onResult: (List<Message>) -> Unit) {
        firestore.collection(Constants.COLLECTION_MESSAGES)
            .whereArrayContains("participants", userId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val allMessages = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()

                val filteredMessages = allMessages.filter { it.participants.contains(otherUserId) }
                onResult(filteredMessages)
            }
    }
    fun sendMessage(message: Message, onResult: (Boolean) -> Unit) {
        val messageId = firestore.collection(Constants.COLLECTION_MESSAGES).document().id

        val participants = listOf(message.senderId, message.receiverId)

        val newMessage = hashMapOf(
            "messageId" to messageId,
            "senderId" to message.senderId,
            "receiverId" to message.receiverId,
            "text" to message.text,
            "timestamp" to message.timestamp,
            "isRead" to message.isRead,
            "participants" to participants
        )

        firestore.collection(Constants.COLLECTION_MESSAGES)
            .document(messageId)
            .set(newMessage)
            .addOnSuccessListener { onResult(true) }
    }
    fun updateConversation(
        senderId: String,
        receiverId: String,
        lastMessage: String,
        timestamp: Long,
        onResult: (Boolean) -> Unit
    ) {

        val participants = listOf(senderId, receiverId).sorted()
        val conversationId = participants.joinToString("_")

        val conversation = Conversation(
            conversationId = conversationId,
            participants = participants,
            lastMessage = lastMessage,
            lastMessageTime = timestamp,
            lastSenderId = senderId
        )

        firestore.collection(Constants.COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .set(conversation)
            .addOnSuccessListener {
                onResult(true)
            }
            .addOnFailureListener { e ->
                onResult(false)
            }
    }

    fun markMessagesAsRead(userId: String, otherUserId: String, onResult: (Boolean) -> Unit) {

        firestore.collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("senderId", otherUserId)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                val batch = firestore.batch()
                documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }

                batch.commit()
                    .addOnSuccessListener {
                        onResult(true)
                    }
                    .addOnFailureListener { e ->
                        onResult(false)
                    }
            }
            .addOnFailureListener { e ->
                onResult(false)
            }
    }

    fun getUnreadMessageCount(userId: String, onResult: (Int) -> Unit) {

        firestore.collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                onResult(count)
            }
            .addOnFailureListener { e ->
                onResult(0)
            }
    }
}