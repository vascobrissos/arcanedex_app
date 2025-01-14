package pt.ipt.arcanedex_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.arcanedex_app.CardAdapter
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.CardItem
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.database.AppDatabase
import pt.ipt.arcanedex_app.data.models.Creature.ArcaneEntity
import pt.ipt.arcanedex_app.data.models.Creature.FavoriteRequest
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.viewmodel.SharedCardItemViewModel

/**
 * Fragmento responsável por apresentar a listagem de arcanes.
 */
class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var loadMoreButton: Button
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var adapter: CardAdapter
    private var searchJob: Job? = null
    private var name = ""
    private val cardItems = mutableListOf<CardItem>()
    private var currentPage = 1 // Página atual para paginação
    private var totalcountNotFavorites = 0
    private var isLoading = false // Para evitar múltiplas requisições simultâneas
    private var isOfflineDataFetched = false

    /**
     * Infla o layout do fragmento e inicializa as views principais.
     *
     * @param inflater O inflador de layout para criar a interface.
     * @param container O contêiner pai, se existir.
     * @param savedInstanceState O estado salvo, caso haja.
     * @return A vista inflada.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    /**
     * Método chamado após a criação da view do fragmento, onde a lógica de inicialização é definida.
     *
     * @param view A vista inflada no método onCreateView.
     * @param savedInstanceState O estado salvo, caso haja.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialização das views
        recyclerView = view.findViewById(R.id.recyclerview)
        searchView = view.findViewById(R.id.search_bar)
        loadMoreButton = view.findViewById(R.id.load_more_button)
        loadingSpinner = view.findViewById(R.id.loading_spinner)

        // Inicializa o layout e o adaptador
        recyclerView.visibility = View.GONE
        loadMoreButton.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val sharedCardItemViewModel: SharedCardItemViewModel by activityViewModels()

        adapter = CardAdapter(
            items = cardItems,
            onItemClick = { clickedItem ->
                // Navega para o detalhe do card
                val bundle = Bundle().apply {
                    putParcelable("cardItem", clickedItem)
                }
                findNavController().navigate(R.id.action_homeFragment_to_detailFragment)
                sharedCardItemViewModel.selectedCardItem = clickedItem
            },
            onFavoriteToggle = { favoriteItem ->
                // Alterna o estado de favorito do item
                toggleFavorite(favoriteItem.Id)
            },
            showFavorites = true
        )

        // Carrega dados offline uma única vez
        if (!isOfflineDataFetched) {
            saveDataOfflineOnce()
        }

        recyclerView.adapter = adapter

        // Manipula o foco de pesquisa
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                loadMoreButton.visibility = View.GONE
            } else {
                if (searchView.query.isNullOrEmpty()) {
                    loadMoreButton.visibility = View.VISIBLE // Mostra o botão somente se a busca estiver vazia
                }
            }
        }

        // Carrega mais dados quando o botão "Carregar mais" é pressionado
        loadMoreButton.setOnClickListener {
            loadMoreData(name)
        }

        // Configura o comportamento da pesquisa
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Realiza a busca ao submeter a consulta
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Cancela a pesquisa anterior, se estiver em andamento
                searchJob?.cancel()
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300) // Atraso de 300ms
                    performSearch(newText)
                }
                return true
            }
        })

        // Carrega a primeira página de dados
        if (cardItems.isEmpty()) {
            loadMoreData(name)
        }
    }

    /**
     * Carrega mais dados da API com base no nome fornecido e implementa a lógica de paginação.
     *
     * @param name Nome ou filtro para a pesquisa.
     */
    private fun loadMoreData(name: String) {
        if (isLoading) return // Previne múltiplas solicitações simultâneas
        isLoading = true

        loadingSpinner.visibility = View.VISIBLE // Mostra o spinner de carregamento
        recyclerView.visibility = View.GONE // Oculta o RecyclerView enquanto carrega
        loadMoreButton.visibility = View.GONE // Oculta o botão de carregar mais

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Utilizador sem sessão iniciada!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    isLoading = false
                    return@launch
                }

                // Chamada à API para obter criaturas
                val response = RetrofitClient.instance.getAllCreatures(
                    token = "Bearer $token",
                    page = currentPage,
                    limit = 6,
                    name = name,
                    onlyFavoriteArcanes = false,
                    toSaveOffline = false
                )
                totalcountNotFavorites = response.count
                withContext(Dispatchers.Main) {
                    val newCardItems = response.data.map { creature ->
                        CardItem(
                            Id = creature.Id,
                            Name = creature.Name,
                            Img = creature.Img,
                            Lore = creature.Lore,
                            isFavorite = creature.isFavoriteToUser
                        )
                    }

                    // Adiciona os novos itens ao RecyclerView
                    cardItems.addAll(newCardItems)
                    adapter.notifyDataSetChanged()

                    currentPage++ // Incrementa a página atual
                    isLoading = false

                    loadingSpinner.visibility = View.GONE // Esconde o spinner de carregamento

                    if (cardItems.isEmpty()) {
                        // Mostra mensagens de "Sem dados" se não houver itens
                        view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.VISIBLE
                        view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE // Mostra o RecyclerView
                        view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
                        view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
                    }

                    // Mostra ou oculta o botão "Carregar mais" com base no número de itens carregados
                    loadMoreButton.visibility =
                        if (cardItems.size >= totalcountNotFavorites) View.GONE else View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Erro: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    isLoading = false
                    loadingSpinner.visibility =
                        View.GONE // Esconde o spinner de carregamento em caso de erro
                }
            }
        }
    }

    /**
     * Alterna o estado de favorito para uma criatura, adicionando-a ou removendo-a da lista de favoritos.
     *
     * @param creatureId O ID da criatura a ser adicionada ou removida dos favoritos.
     */
    private fun toggleFavorite(creatureId: Int) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null) {
            Toast.makeText(requireContext(), "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.addCreatureToFavorites(
                    token = "Bearer $token",
                    favoriteRequest = FavoriteRequest(CreatureId = creatureId)
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Favoritos Atualizado!", Toast.LENGTH_SHORT).show()
                        refreshData() // Atualiza a lista de criaturas não favoritas após alternar o estado.
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Falha na atualização de favoritos: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Erro: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Atualiza os dados, reiniciando a página atual e recarregando as criaturas.
     */
    private fun refreshData() {
        currentPage = 1
        cardItems.clear()
        loadMoreData(name)
    }

    /**
     * Realiza uma pesquisa de criaturas com base na consulta fornecida.
     * Caso a consulta esteja vazia, exibe todos os dados novamente.
     *
     * @param query O termo de pesquisa a ser utilizado.
     */
    private fun performSearch(query: String?) {
        if (query.isNullOrEmpty()) {
            view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
            refreshData()
            return
        }

        loadingSpinner.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        loadMoreButton.visibility = View.GONE
        view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE // Esconde a imagem de "Sem Dados"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.instance.getAllCreatures(
                    token = "Bearer $token",
                    page = 1,
                    limit = 6,
                    onlyFavoriteArcanes = false,
                    toSaveOffline = false,
                    name = query
                )

                withContext(Dispatchers.Main) {
                    val searchResults = response.data.map { creature ->
                        CardItem(
                            Id = creature.Id,
                            Name = creature.Name,
                            Img = creature.Img,
                            Lore = creature.Lore,
                            isFavorite = creature.isFavoriteToUser
                        )
                    }

                    cardItems.clear()
                    cardItems.addAll(searchResults)
                    adapter.notifyDataSetChanged()

                    if (searchResults.isEmpty()) {
                        // Mostra a imagem de "Sem Dados"
                        recyclerView.visibility = View.GONE
                        view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.VISIBLE
                        view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
                        view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
                    }

                    loadingSpinner.visibility = View.GONE
                    loadMoreButton.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Erro: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
                    loadingSpinner.visibility = View.GONE
                    view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Salva os dados de criaturas offline pela primeira vez.
     * Faz uma requisição para obter dados e os salva localmente.
     *
     * Caso os dados já tenham sido obtidos, uma mensagem é exibida.
     */
    private fun saveDataOfflineOnce() {
        if (isOfflineDataFetched) {
            Toast.makeText(requireContext(), "Data para offline já foi obtida", Toast.LENGTH_SHORT).show()
            return
        }

        loadingSpinner.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.instance.getAllCreatures(
                    token = "Bearer $token",
                    page = 1,
                    limit = 10, // Ajuste conforme necessário
                    name = "",
                    onlyFavoriteArcanes = false,
                    toSaveOffline = true // Garantir que é para guardar offline
                )

                val newCardItems = response.data.map { creature ->
                    CardItem(
                        Id = creature.Id,
                        Name = creature.Name,
                        Img = creature.Img,
                        Lore = creature.Lore,
                        isFavorite = creature.isFavoriteToUser
                    )
                }

                // Guardar na cache usando Room
                saveToCache(newCardItems)

                withContext(Dispatchers.Main) {
                    isOfflineDataFetched = true // Marca como feito para a sessão atual
                    loadingSpinner.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Erro: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    loadingSpinner.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Salva a lista de itens de criaturas no BD local (Room).
     *
     * @param cardItems Lista de itens de criaturas a serem armazenados na BD.
     */
    private fun saveToCache(cardItems: List<CardItem>) {
        val db = AppDatabase.getDatabase(requireContext()) // Obtenha a instância da BD
        val entities = cardItems.map { item ->
            ArcaneEntity(
                id = item.Id,
                name = item.Name,
                img = item.Img,
                lore = item.Lore
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            db.arcaneDao().insertAll(entities) // Guardar os dados na BD
        }
    }
}
