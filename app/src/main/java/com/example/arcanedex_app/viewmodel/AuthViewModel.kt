package com.example.arcanedex_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcanedex_app.data.api.RetrofitClient
import com.example.arcanedex_app.data.models.LoginRequest
import com.example.arcanedex_app.data.models.RegisterRequest
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    fun registerUser(registerRequest: RegisterRequest, onResult: (Boolean) -> Unit) {
        // Launch coroutine in ViewModel scope
        viewModelScope.launch {
            try {
                RetrofitClient.instance.registerUser(registerRequest)
                onResult(true) // Success
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false) // Failure
            }
        }
    }

    fun loginUser(loginRequest: LoginRequest, onResult: (String?) -> Unit) {
        // Launch coroutine in ViewModel scope
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.loginUser(loginRequest)
                onResult(response.token) // Success
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null) // Failure
            }
        }
    }
}
