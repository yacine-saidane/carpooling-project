package isimm.ing1.carpoolingstudents.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val participants: List<String> = emptyList(),
    val text: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
)