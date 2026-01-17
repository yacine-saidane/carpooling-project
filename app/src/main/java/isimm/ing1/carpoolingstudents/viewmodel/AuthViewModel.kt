package isimm.ing1.carpoolingstudents.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import isimm.ing1.carpoolingstudents.model.User
import isimm.ing1.carpoolingstudents.repository.AuthRepository
import isimm.ing1.carpoolingstudents.repository.UserRepository

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> = _loginStatus

    private val _registerStatus = MutableLiveData<Pair<Boolean, String?>>()
    val registerStatus: LiveData<Pair<Boolean, String?>> = _registerStatus

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            loadCurrentUser()
        } else {
            _currentUser.value = null
        }
    }

    fun loadCurrentUser() {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            userRepository.getUser(userId) { user ->
                _currentUser.value = user
            }
        } else {
            _currentUser.value = null
        }
    }
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email et mot de passe requis"
            return
        }

        authRepository.login(email, password) { success ->
            _loginStatus.value = success
            if (success) {
                loadCurrentUser()
            } else {
                _errorMessage.value = "Email ou mot de passe incorrect"
            }
        }
    }


    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email et mot de passe requis"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "Le mot de passe doit contenir au moins 6 caractères"
            return
        }

        authRepository.register(email, password) { success, userId ->
            _registerStatus.value = Pair(success, userId)
            if (!success) {
                _errorMessage.value = "Erreur lors de l'inscription"
            }
        }
    }



    fun isLoggedIn(): Boolean {
        return authRepository.getCurrentUserId() != null
    }

    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }
}