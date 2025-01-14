package pt.ipt.arcanedex_app.data.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.auth0.android.jwt.JWT
import java.util.Date

// Singleton para gerir as preferências partilhadas
object SharedPreferencesHelper {

    // Nome do ficheiro de preferências partilhadas
    private const val PREFS_NAME = "arcanedex_prefs"

    // Chave para armazenar o token de autenticação
    private const val KEY_TOKEN = "authToken"

    // Chave para armazenar se o utilizador aceitou os termos
    private const val KEY_HAS_ACCEPTED_TERMS = "hasAcceptedTerms"

    /**
     * Recupera a instância das preferências partilhadas para a aplicação.
     *
     * @param context Contexto do componente que faz a chamada.
     * @return A instância das preferências partilhadas.
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Guarda o token de autenticação nas preferências partilhadas.
     *
     * @param context Contexto do componente que faz a chamada.
     * @param token O token de autenticação a guardar.
     */
    fun saveToken(context: Context, token: String) {
        getSharedPreferences(context)
            .edit() // Abre as preferências para edição
            .putString(KEY_TOKEN, token) // Adiciona o token
            .apply() // Guarda as alterações de forma assíncrona
    }

    /**
     * Recupera o token de autenticação guardado nas preferências partilhadas.
     *
     * @param context Contexto do componente que faz a chamada.
     * @return O token guardado ou null se não houver token guardado.
     */
    fun getToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_TOKEN, null)
    }

    /**
     * Apaga o token de autenticação das preferências partilhadas.
     *
     * @param context Contexto do componente que faz a chamada.
     */
    fun clearToken(context: Context) {
        getSharedPreferences(context)
            .edit() // Abre as preferências para edição
            .remove(KEY_TOKEN) // Remove o token
            .apply() // Guarda as alterações de forma assíncrona
    }

    /**
     * Verifica se o utilizador aceitou os termos de serviço.
     *
     * @param context Contexto do componente que faz a chamada.
     * @return Verdadeiro se os termos foram aceites, falso caso contrário.
     */
    fun hasAcceptedTerms(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_HAS_ACCEPTED_TERMS, false)
    }

    /**
     * Atualiza o valor indicando se o utilizador aceitou os termos de serviço.
     *
     * @param context Contexto do componente que faz a chamada.
     * @param accepted Valor booleano indicando se os termos foram aceites.
     */
    fun setHasAcceptedTerms(context: Context, accepted: Boolean) {
        getSharedPreferences(context)
            .edit()
            .putBoolean(KEY_HAS_ACCEPTED_TERMS, accepted)
            .apply()
    }

    /**
     * Verifica se a rede está disponível e se tem acesso à Internet.
     *
     * @param context Contexto da aplicação.
     * @return Verdadeiro se a rede estiver disponível, falso caso contrário.
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Verifica se a senha fornecida é válida. A senha deve ter pelo menos 8 caracteres e um carácter especial.
     *
     * @param password A senha a verificar.
     * @return Verdadeiro se a senha for válida, falso caso contrário.
     */
    fun isPasswordValid(password: String): Boolean {
        // Pelo menos 8 caracteres e um carácter especial
        val passwordPattern = Regex("^(?=.*[@#\$%^&+=!]).{8,}$")
        return passwordPattern.matches(password)
    }

    /**
     * Verifica se o utilizador tem o papel de "Admin" com base no token de autenticação.
     *
     * @param context Contexto da aplicação.
     * @return Verdadeiro se o utilizador for um "Admin", falso caso contrário.
     */
    fun isUserAdmin(context: Context): Boolean {
        val token = getToken(context) ?: return false
        val jwt = JWT(token)
        val expiresAt = jwt.expiresAt
        val isTokenValid = expiresAt != null && expiresAt.after(Date())

        if (isTokenValid) {
            // Verifica se o utilizador tem o papel de "Admin"
            val role = jwt.getClaim("role").asString()
            return role == "Admin"
        } else {
            // Apaga o token inválido
            clearToken(context)
            return false
        }
    }

}
