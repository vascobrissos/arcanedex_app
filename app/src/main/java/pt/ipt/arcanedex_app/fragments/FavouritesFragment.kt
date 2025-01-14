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
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.viewmodel.SharedCardItemViewModel

/**
 * Fragmento que lista Arcanes favoritos de um utilizador.
 */
class FavouritesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var loadMoreButton: Button
    private lateinit var adapter: CardAdapter
    private var searchJob: Job? = null
    private var name = ""
    private val cardItems = mutableListOf<CardItem>()
    private var currentPage = 1 // Current page for pagination
    private var isLoading = false // To prevent multiple simultaneous requests

    /**
     * Infla a view do fragmento de favoritos.
     *
     * @param inflater O objeto LayoutInflater utilizado para inflar a view.
     * @param container O contêiner que pode hospedar a view.
     * @param savedInstanceState O estado previamente salvo, caso exista.
     * @return A view inflada.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favourites, container, false)
    }

    /**
     * Inicializa os componentes do layout e configura a lógica de pesquisa, exibição de favoritos
     * e a navegação para o detalhe do item selecionado.
     *
     * @param view A view associada ao fragmento.
     * @param savedInstanceState O estado previamente salvo, caso exista.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerview)
        searchView = view.findViewById(R.id.search_bar)
        loadMoreButton = view.findViewById(R.id.load_more_button)
        loadingSpinner = view.findViewById(R.id.loading_spinner)

        // Inicializa o estado das views
        recyclerView.visibility = View.GONE
        loadMoreButton.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        // Define o layout do RecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Ação ao clicar no botão "Carregar mais"
        loadMoreButton.setOnClickListener {
            loadFavourites(name)
        }

        val sharedCardItemViewModel: SharedCardItemViewModel by activityViewModels()

        // Inicializa o adaptador do RecyclerView
        adapter = CardAdapter(
            items = cardItems,
            onItemClick = { clickedItem ->
                val bundle = Bundle().apply {
                    putParcelable("cardItem", clickedItem)
                }
                findNavController().navigate(R.id.action_favouritesFragment_to_detailFragment)
                sharedCardItemViewModel.selectedCardItem = clickedItem
            },
            onFavoriteToggle = { favoriteItem ->
                removeFavorite(favoriteItem.Id)
            },
            showFavorites = true
        )

        recyclerView.adapter = adapter

        // Ações ao mudar o foco do campo de pesquisa
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                loadMoreButton.visibility = View.GONE
            } else {
                if (searchView.query.isNullOrEmpty()) {
                    loadMoreButton.visibility =
                        View.VISIBLE // Mostra o botão somente se a busca estiver vazia
                }
            }
        }

        // Configura o comportamento de pesquisa com atraso
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel() // Cancela a pesquisa anterior, se estiver em andamento
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300) // Atraso de 300ms
                    performSearch(newText)
                }
                return true
            }
        })

        // Carrega a primeira página de dados
        if (cardItems.isEmpty()) {
            loadFavourites(name)
        }
    }

    /**
     * Carrega os itens favoritos a partir da API e atualiza a UI com os resultados.
     *
     * A função faz uma requisição à API para obter as criaturas favoritas, adicionando-as
     * à lista existente. O botão "Carregar mais" só será mostrado se houver mais dados
     * para carregar.
     *
     * @param name O nome para filtrar as criaturas. Se vazio, carrega todas as criaturas favoritas.
     */
    private fun loadFavourites(name: String) {
        if (isLoading) return // Previne requisições simultâneas
        isLoading = true

        loadingSpinner.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        loadMoreButton.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    isLoading = false
                    return@launch
                }

                val response = RetrofitClient.instance.getAllCreatures(
                    token = "Bearer $token",
                    page = currentPage,
                    limit = 6,
                    name = name,
                    onlyFavoriteArcanes = true, // Somente favoritos
                    toSaveOffline = false
                )

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

                    cardItems.addAll(newCardItems)
                    adapter.notifyDataSetChanged()

                    currentPage++
                    isLoading = false

                    loadingSpinner.visibility = View.GONE

                    if (cardItems.isEmpty()) {
                        view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.VISIBLE
                        view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
                        view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
                    }

                    loadMoreButton.visibility =
                        if (cardItems.size >= response.count) View.GONE else View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    isLoading = false
                    loadingSpinner.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Realiza a pesquisa de criaturas favoritas com base no texto fornecido.
     *
     * A função faz uma requisição à API filtrando pelo nome da criatura e atualiza a UI
     * com os resultados encontrados. Caso a pesquisa não retorne resultados, é exibida
     * uma mensagem informando que não há dados.
     *
     * @param query O texto de pesquisa para filtrar as criaturas.
     */
    private fun performSearch(query: String?) {
        if (query.isNullOrEmpty()) {
            view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
            refreshData()
            return
        }

        // Ocultar o botão de Load More durante a busca
        loadMoreButton.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        cardItems.clear()
        adapter.notifyDataSetChanged()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    return@launch
                }

                val response = RetrofitClient.instance.getAllCreatures(
                    token = "Bearer $token",
                    page = 1, // Sempre inicia na primeira página ao buscar
                    limit = 6,
                    onlyFavoriteArcanes = true, // Apenas favoritos
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

                    recyclerView.visibility = View.VISIBLE
                    loadingSpinner.visibility = View.GONE
                    loadMoreButton.visibility = View.GONE // Garante que o botão não reapareça
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    loadingSpinner.visibility = View.GONE
                    view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
                    view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Atualiza os dados da lista de favoritos, reiniciando a página e recarregando os itens.
     *
     * Esta função redefine a variável `currentPage` para 1 e limpa a lista `cardItems`.
     * Em seguida, chama a função `loadFavourites` para recarregar os favoritos.
     */
    private fun refreshData() {
        currentPage = 1
        cardItems.clear()
        loadFavourites(name)
    }

    /**
     * Remove uma criatura da lista de favoritos.
     *
     * A função envia uma requisição à API para remover a criatura identificada pelo `creatureId`
     * dos favoritos do utilizador. Após a remoção, os dados são recarregados.
     * Caso haja falha na remoção ou na comunicação com a API, uma mensagem de erro é exibida.
     *
     * @param creatureId O ID da criatura a ser removida dos favoritos.
     */
    private fun removeFavorite(creatureId: Int) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null) {
            Toast.makeText(requireContext(), "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.removeCreatureFromFavorites(
                    token = "Bearer $token",
                    creatureId = creatureId
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Removido dos Favoritos",
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshData()  // Recarrega os dados após a remoção
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Falha ao remover dos favoritos: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
