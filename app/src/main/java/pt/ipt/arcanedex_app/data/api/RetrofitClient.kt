package pt.ipt.arcanedex_app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Objeto responsável pela configuração e criação do cliente Retrofit.
 * Este cliente é utilizado para fazer chamadas à API.
 */
object RetrofitClient {

    // URL base para as requisições à API
    private const val BASE_URL = "http://legismente.ddns.net:3000"

    // Interceptor para logging das chamadas à API
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level =
            HttpLoggingInterceptor.Level.BODY // Configura o nível de logging para capturar o corpo das requisições
    }

    // Cliente HTTP configurado com o interceptor
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // Adiciona o interceptor para logging
        .build()

    // Instância do serviço API criada de forma lazy (somente quando for utilizada pela primeira vez)
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // Define a URL base da API
            .client(okHttpClient) // Usa o cliente HTTP configurado
            .addConverterFactory(GsonConverterFactory.create()) // Adiciona suporte à conversão de JSON usando Gson
            .build() // Constrói a instância do Retrofit
            .create(ApiService::class.java) // Cria a implementação da interface ApiService
    }
}
