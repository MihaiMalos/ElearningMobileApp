package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.api.ApiConfig
import com.elearning.ui.data.model.User
import com.elearning.ui.data.repository.AuthRepository
import com.elearning.ui.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository(ApiConfig.apiService)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Simple error message
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        checkLoginStatus()
    }

    fun checkLoginStatus() {
        val loggedIn = repository.isLoggedIn()
        _isLoggedIn.value = loggedIn
        
        if (loggedIn) {
            viewModelScope.launch {
                val result = repository.getCurrentUser()
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    _currentUser.value = user
                    user?.let {
                        TokenManager.saveUserId(it.id)
                        TokenManager.saveUserRole(it.role.name)
                    }
                } else {
                     // Token might be invalid or expired.
                     // Optionally logout if 401, but for now just don't set user
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.login(username, password)
            if (result.isSuccess) {
                // Fetch user details immediately before confirming login success
                val userResult = repository.getCurrentUser()
                val user = userResult.getOrNull()

                if (user != null) {
                    _currentUser.value = user
                    TokenManager.saveUserId(user.id)
                    TokenManager.saveUserRole(user.role.name)

                    // Only set logged in result after user details are saved
                    _isLoggedIn.value = true
                    _error.value = null
                } else {
                    _error.value = "Failed to retrieve user details."
                    // If user fetch fails, we should probably treat it as a login failure or partial state,
                    // but for now let's just show error.
                }
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
            
            _isLoading.value = false
        }
    }

    fun logout() {
        repository.logout()
        _isLoggedIn.value = false
        _currentUser.value = null
    }
}
