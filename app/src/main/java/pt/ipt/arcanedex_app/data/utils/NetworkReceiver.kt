package pt.ipt.arcanedex_app.data.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Um [BroadcastReceiver] que escuta mudanças na conectividade da rede.
 * Este receptor verifica se a rede está disponível e chama o método `onNetworkChange`
 * sempre que a conectividade da rede muda.
 */
open class NetworkReceiver : BroadcastReceiver() {

    /**
     * Método chamado sempre que há uma mudança na conectividade da rede.
     * Verifica se a rede está disponível e chama `onNetworkChange` com o estado da conexão.
     *
     * @param context O contexto da aplicação.
     * @param intent A intenção que foi recebida.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        val isConnected = isNetworkAvailable(context)
        onNetworkChange(isConnected)
    }

    /**
     * Método a ser sobrescrito para definir o comportamento ao detectar uma mudança na rede.
     *
     * @param isConnected Indica se a rede está disponível (verdadeiro) ou não (falso).
     */
    open fun onNetworkChange(isConnected: Boolean) {
        // Sobrescrever no local onde você deseja usar o comportamento
    }

    /**
     * Verifica se há uma conexão de rede ativa com a capacidade de acessar a Internet.
     *
     * @param context O contexto da aplicação.
     * @return `true` se a rede está disponível e tem capacidade de Internet, `false` caso contrário.
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
