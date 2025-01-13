package com.example.arcanedex_app.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcanedex_app.data.api.RetrofitClient
import com.example.arcanedex_app.data.models.UserProfile
import com.example.arcanedex_app.data.models.UserProfileRequest
import com.example.arcanedex_app.data.models.UserProfileResponse
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> get() = _userProfile

    private val _updateStatus = MutableLiveData<Result<UserProfileResponse>>()
    val updateStatus: LiveData<Result<UserProfileResponse>> get() = _updateStatus

    fun loadUserProfile(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile("Bearer $token")
                if (response.isSuccessful) {
                    _userProfile.postValue(response.body())
                    Log.d("ProfileViewModel", "API Response: ${response.body()}")
                } else {
                    _userProfile.postValue(null)
                    Log.e("ProfileViewModel", "API Error: ${response.message()}")
                }
            } catch (e: Exception) {
                _userProfile.postValue(null)
                Log.e("ProfileViewModel", "Exception: ${e.message}")
            }
        }
    }


    fun updateUserProfile(token: String, userProfile: UserProfileRequest) {
        viewModelScope.launch {
            try {
                val response =
                    RetrofitClient.instance.updateUserProfile("Bearer $token", userProfile)
                if (response.isSuccessful) {
                    _updateStatus.postValue(Result.success(response.body()!!))
                } else {
                    _updateStatus.postValue(Result.failure(Exception(response.message())))
                }
            } catch (e: Exception) {
                _updateStatus.postValue(Result.failure(e))
            }
        }
    }
}
