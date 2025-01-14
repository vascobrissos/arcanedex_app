package pt.ipt.arcanedex_app.data.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.auth0.android.jwt.JWT
import java.util.Date

// Singleton object to manage shared preferences
object SharedPreferencesHelper {

    // Name of the SharedPreferences file
    private const val PREFS_NAME = "arcanedex_prefs"

    // Key for storing the token
    private const val KEY_TOKEN = "authToken"

    // Key for storing whether the user has accepted the terms
    private const val KEY_HAS_ACCEPTED_TERMS = "hasAcceptedTerms"

    /**
     * Retrieves the SharedPreferences instance for the app.
     *
     * @param context Context of the calling component.
     * @return The SharedPreferences instance.
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves the authentication token to SharedPreferences.
     *
     * @param context Context of the calling component.
     * @param token The authentication token to save.
     */
    fun saveToken(context: Context, token: String) {
        getSharedPreferences(context)
            .edit() // Open SharedPreferences for editing
            .putString(KEY_TOKEN, token) // Add the token
            .apply() // Save changes asynchronously
    }

    /**
     * Retrieves the saved authentication token from SharedPreferences.
     *
     * @param context Context of the calling component.
     * @return The saved token, or null if no token is found.
     */
    fun getToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_TOKEN, null)
    }

    /**
     * Clears the authentication token from SharedPreferences.
     *
     * @param context Context of the calling component.
     */
    fun clearToken(context: Context) {
        getSharedPreferences(context)
            .edit() // Open SharedPreferences for editing
            .remove(KEY_TOKEN) // Remove the token
            .apply() // Save changes asynchronously
    }

    /**
     * Checks if the user has accepted the terms of service.
     *
     * @param context Context of the calling component.
     * @return True if the terms have been accepted, false otherwise.
     */
    fun hasAcceptedTerms(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_HAS_ACCEPTED_TERMS, false)
    }

    /**
     * Updates the value indicating whether the user has accepted the terms of service.
     *
     * @param context Context of the calling component.
     * @param accepted Boolean value indicating if the terms have been accepted.
     */
    fun setHasAcceptedTerms(context: Context, accepted: Boolean) {
        getSharedPreferences(context)
            .edit()
            .putBoolean(KEY_HAS_ACCEPTED_TERMS, accepted)
            .apply()
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isPasswordValid(password: String): Boolean {
        // At least 8 characters and one special character
        val passwordPattern = Regex("^(?=.*[@#\$%^&+=!]).{8,}$")
        return passwordPattern.matches(password)
    }

    fun isUserAdmin(context: Context): Boolean {
        val token = getToken(context) ?: return false
        val jwt = JWT(token)
        val expiresAt = jwt.expiresAt
        val isTokenValid = expiresAt != null && expiresAt.after(Date())

        if (isTokenValid) {
            // Check if the user has an "admin" role
            val role = jwt.getClaim("role").asString()
            return role == "Admin"
        } else {
            // Clear the invalid token
            clearToken(context)
            return false
        }
    }

}
