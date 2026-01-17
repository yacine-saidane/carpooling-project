package isimm.ing1.carpoolingstudents.model

data class Conversation(
    val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastSenderId: String = "",
    val unreadCount: Int = 0
)