package isimm.ing1.carpoolingstudents.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import isimm.ing1.carpoolingstudents.model.User
import isimm.ing1.carpoolingstudents.repository.UserRepository

class UserViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun saveUser(user: User) {
        _isLoading.value = true
        userRepository.saveUser(user) { success ->
            _saveStatus.value = success
            _isLoading.value = false
            if (success) {
                _userProfile.value = user
            }
        }
    }

    fun loadUser(userId: String) {
        _isLoading.value = true
        userRepository.getUser(userId) { user ->
            _userProfile.value = user
            _isLoading.value = false
        }
    }

    fun updateUser(userId: String, updates: Map<String, Any>) {
        _isLoading.value = true
        userRepository.updateUser(userId, updates) { success ->
            _saveStatus.value = success
            _isLoading.value = false
            if (success) {
                loadUser(userId)
            }
        }
    }


}