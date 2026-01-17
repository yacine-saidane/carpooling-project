package isimm.ing1.carpoolingstudents.repository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                onResult(true, userId)
            }
            .addOnFailureListener { exception ->
                onResult(false, null)
            }
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onResult(true)
            }
            .addOnFailureListener { exception ->
                onResult(false)
            }
    }

    fun getCurrentUserId(): String? {
        val userId = auth.currentUser?.uid
        return userId
    }



    fun logout() {
        auth.signOut()
    }
}