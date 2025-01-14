package pt.ipt.arcanedex_app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.ipt.arcanedex_app.CardAdapter
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.CardItem
import pt.ipt.arcanedex_app.data.database.AppDatabase
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * Atividade responsável por exibir os dados em modo offline.
 * Permite ao utilizador visualizar os itens em cache, mesmo sem ligação à internet.
 */
class OfflineActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView // RecyclerView para exibir os dados offline
    private lateinit var adapter: CardAdapter // Adapter para gerir os itens na RecyclerView
    private lateinit var NoDataImage: ImageView // Imagem exibida quando não há dados
    private lateinit var NoDataText: TextView // Texto exibido quando não há dados
    private val cardItems = mutableListOf<CardItem>() // Lista de itens para exibição
    private var timer: Timer? = null // Timer para verificar conexão de internet periodicamente
    private var isNavigatingToLogin = false // Evitar múltiplas transições para a atividade de login

    /**
     * Método chamado ao criar a atividade.
     * Configura a RecyclerView e inicia a verificação de internet.
     *
     * @param savedInstanceState Estado salvo da atividade (se disponível).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline)

        NoDataImage = findViewById(R.id.no_data_image) // Inicializa o componente de imagem
        NoDataText = findViewById(R.id.noDataText) // Inicializa o componente de texto
        NoDataText.visibility = View.VISIBLE // Torna o texto visível inicialmente
        NoDataImage.visibility = View.VISIBLE // Torna a imagem visível inicialmente
        recyclerView = findViewById(R.id.recyclerview) // Inicializa a RecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 2) // Define o layout em grelha

        // Configuração do adapter
        adapter = CardAdapter(
            items = cardItems,
            onItemClick = { clickedItem ->
                Log.d(
                    "OfflineActivity",
                    "Clicked on: ${clickedItem.Name}"
                ) // Regista o item clicado
            },
            onFavoriteToggle = {
                Toast.makeText(this, "Não pode favoritar enquanto offline", Toast.LENGTH_SHORT)
                    .show() // Mostra mensagem ao tentar favoritar offline
            },
            showFavorites = false // Desativa a exibição de favoritos no modo offline
        )

        recyclerView.adapter = adapter // Associa o adapter à RecyclerView

        // Carrega os dados em cache
        loadCachedArcanes()

        // Inicia a verificação periódica de ligação à internet
        startInternetCheck()
    }

    /**
     * Carrega os itens em cache da base de dados local e atualiza a interface.
     */
    private fun loadCachedArcanes() {
        CoroutineScope(Dispatchers.IO).launch {
            val db =
                AppDatabase.getDatabase(applicationContext) // Obtém a instância da base de dados
            val cachedArcanes = db.arcaneDao().getAllArcanes() // Busca os dados em cache

            if (cachedArcanes.isEmpty()) {
                runOnUiThread {
                    NoDataImage.visibility = View.VISIBLE // Mostra a imagem de "sem dados"
                    NoDataText.visibility = View.VISIBLE // Mostra o texto de "sem dados"
                    recyclerView.visibility = View.GONE // Oculta a RecyclerView
                }
            } else {
                runOnUiThread {
                    NoDataImage.visibility = View.GONE // Oculta a imagem de "sem dados"
                    NoDataText.visibility = View.GONE // Oculta o texto de "sem dados"
                    recyclerView.visibility = View.VISIBLE // Torna a RecyclerView visível
                }

                cardItems.clear() // Limpa a lista atual de itens
                cardItems.addAll(cachedArcanes.map { arcane ->
                    CardItem(
                        Id = arcane.id,
                        Name = arcane.name,
                        Img = arcane.img,
                        Lore = arcane.lore,
                        isFavorite = false
                    )
                })

                runOnUiThread {
                    adapter.notifyDataSetChanged() // Notifica o adapter sobre as mudanças nos dados
                }
            }
        }
    }

    /**
     * Inicia a verificação periódica de ligação à internet.
     * Redireciona o utilizador para a tela de login se a internet estiver disponível.
     */
    private fun startInternetCheck() {
        timer = Timer()
        timer?.schedule(timerTask {
            if (!isNavigatingToLogin && SharedPreferencesHelper.isInternetAvailable(this@OfflineActivity)) {
                runOnUiThread {
                    isNavigatingToLogin = true // Evita múltiplas navegações
                    SharedPreferencesHelper.clearToken(this@OfflineActivity) // Limpa o token
                    val intent = Intent(this@OfflineActivity, MainActivity::class.java)
                    startActivity(intent) // Inicia a atividade de login
                    finish() // Termina a atividade atual
                }
            }
        }, 0, 5000) // Verifica a cada 5 segundos
    }

    /**
     * Método chamado ao destruir a atividade.
     * Cancela o timer para evitar memória vazada.
     */
    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel() // Cancela o timer
    }
}
