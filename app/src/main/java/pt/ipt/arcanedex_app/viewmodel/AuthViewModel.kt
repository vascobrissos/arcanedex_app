package pt.ipt.arcanedex_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.models.user.LoginRequest
import pt.ipt.arcanedex_app.data.models.user.RegisterRequest
import retrofit2.HttpException

/**
 * ViewModel responsável pela gestão da autenticação do utilizador.
 * Inclui métodos para o registo e login de utilizadores.
 */
class AuthViewModel : ViewModel() {

    /**
     * Regista um novo utilizador.
     *
     * Esta função envia uma requisição à API para registar um novo utilizador utilizando
     * os dados fornecidos no parâmetro `request`. A função retorna um resultado indicando
     * se o registo foi bem-sucedido ou não, junto com uma mensagem de erro, se aplicável.
     *
     * @param request Objeto contendo os dados necessários para o registo do utilizador.
     * @param onResult Função de callback que recebe dois parâmetros:
     *                  - Um valor `Boolean` que indica o sucesso ou falha do registo.
     *                  - Uma mensagem de erro, caso haja falha (nulo em caso de sucesso).
     */
    fun registerUser(request: RegisterRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.registerUser(request)
                onResult(true, null) // Sucesso
            } catch (e: HttpException) {
                // Lida com erro da API
                val errorMessage = e.response()?.errorBody()?.string() ?: ""
                onResult(false, errorMessage)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Algo correu mal. Tente novamente.")
            }
        }
    }

    /**
     * Realiza o login de um utilizador.
     *
     * Esta função envia uma requisição à API para fazer login com as credenciais fornecidas
     * no parâmetro `request`. Se o login for bem-sucedido, retorna o token de autenticação.
     * Caso contrário, retorna uma mensagem de erro adequada.
     *
     * @param request Objeto contendo os dados necessários para o login do utilizador.
     * @param onResult Função de callback que recebe dois parâmetros:
     *                  - Um `String?` que é o token de autenticação, ou nulo em caso de falha.
     *                  - Uma mensagem de erro, caso o login falhe (nulo em caso de sucesso).
     */
    fun loginUser(request: LoginRequest, onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.loginUser(request)
                onResult(response.token, null) // Sucesso
            } catch (e: HttpException) {
                // Extrai a mensagem de erro da resposta da API
                val rawErrorMessage = e.response()?.errorBody()?.string() ?: ""

                // Debug para verificar a mensagem recebida
                println("Erro da API: $rawErrorMessage")

                // Traduz erros específicos ou usa uma mensagem padrão
                val translatedErrorMessage = when {
                    rawErrorMessage.contains("Password inválida", ignoreCase = true) ||
                            rawErrorMessage.contains("Utilizador não encontrado", ignoreCase = true) ->
                        "As credenciais fornecidas estão incorretas"


                    else -> "Erro inesperado. Por favor, tente novamente."
                }

                // Retorna a mensagem de erro traduzida
                onResult(null, translatedErrorMessage)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null, "Algo correu mal. Tente novamente.")
            }
        }
    }


}