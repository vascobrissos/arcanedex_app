package pt.ipt.arcanedex_app.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.models.User.UserProfile
import pt.ipt.arcanedex_app.data.models.User.UserProfileRequest
import pt.ipt.arcanedex_app.data.models.User.UserProfileResponse

/**
 * ViewModel responsável pela gestão do perfil do utilizador.
 * Inclui métodos para carregar e atualizar o perfil do utilizador.
 */
class ProfileViewModel : ViewModel() {

    /**
     * LiveData que contém o perfil do utilizador.
     */
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> get() = _userProfile

    /**
     * LiveData que contém o status da atualização do perfil do utilizador.
     */
    private val _updateStatus = MutableLiveData<Result<UserProfileResponse>>()
    val updateStatus: LiveData<Result<UserProfileResponse>> get() = _updateStatus

    /**
     * Carrega o perfil do utilizador a partir da API.
     *
     * Esta função envia uma requisição à API para obter os dados do perfil do utilizador
     * utilizando o token de autenticação fornecido.
     *
     * @param token Token de autenticação do utilizador.
     */
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

    /**
     * Atualiza os dados do perfil do utilizador na API.
     *
     * Esta função envia uma requisição à API para atualizar os dados do perfil
     * utilizando o token de autenticação e os dados fornecidos no parâmetro `userProfile`.
     *
     * @param token Token de autenticação do utilizador.
     * @param userProfile Objeto contendo os dados a serem atualizados no perfil do utilizador.
     */
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
