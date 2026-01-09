package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.api.ApiConfig
import com.elearning.ui.data.model.User
import com.elearning.ui.data.repository.AuthRepository
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
                    _currentUser.value = result.getOrNull()
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
                _isLoggedIn.value = true
                _error.value = null
                // Fetch user details immediately
                val userResult = repository.getCurrentUser()
                _currentUser.value = userResult.getOrNull()
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
