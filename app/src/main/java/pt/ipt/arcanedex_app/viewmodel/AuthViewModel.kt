package pt.ipt.arcanedex_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.models.LoginRequest
import pt.ipt.arcanedex_app.data.models.RegisterRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AuthViewModel : ViewModel() {

    fun registerUser(request: RegisterRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.registerUser(request)
                onResult(true, null) // Success
            } catch (e: HttpException) {
                // Handle API error
                val errorMessage = e.response()?.errorBody()?.string() ?: "Erro desconhecido"
                onResult(false, errorMessage)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Algo correu mal. Tente novamente.")
            }
        }
    }

    fun loginUser(request: LoginRequest, onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.loginUser(request)
                onResult(response.token, null) // Success
            } catch (e: HttpException) {
                // Extract error message from API response
                val rawErrorMessage = e.response()?.errorBody()?.string() ?: "Erro desconhecido"

                // Translate specific errors
                val translatedErrorMessage = when {
                    rawErrorMessage.contains(
                        "Invalid password",
                        ignoreCase = true
                    ) -> "Password errada"

                    rawErrorMessage.contains(
                        "User not found",
                        ignoreCase = true
                    ) -> "Utilizador não existe"

                    else -> "Erro desconhecido: $rawErrorMessage"
                }

                // Return translated error message
                onResult(null, translatedErrorMessage)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null, "Algo correu mal. Tente novamente.")
            }
        }
    }

}
