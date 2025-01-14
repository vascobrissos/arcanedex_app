package com.example.arcanedex_app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Query
import com.example.arcanedex_app.CardAdapter
import com.example.arcanedex_app.R
import com.example.arcanedex_app.data.CardItem
import com.example.arcanedex_app.data.api.RetrofitClient
import com.example.arcanedex_app.data.database.AppDatabase
import com.example.arcanedex_app.data.models.ArcaneEntity
import com.example.arcanedex_app.data.models.FavoriteRequest
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper
import com.example.arcanedex_app.viewmodel.SharedCardItemViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var loadMoreButton: Button
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var adapter: CardAdapter
    private var searchJob: Job? = null
    private var name = ""
    private val cardItems = mutableListOf<CardItem>()
    private var currentPage = 1 // Current page for pagination
    private var totalcountNotFavorites = 0
    private var isLoading = false // To prevent multiple simultaneous requests
    private var isOfflineDataFetched = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerview)
        searchView = view.findViewById(R.id.search_bar)
        loadMoreButton = view.findViewById(R.id.load_more_button)
        loadingSpinner = view.findViewById(R.id.loading_spinner)

        recyclerView.visibility = View.GONE
        loadMoreButton.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val sharedCardItemViewModel: SharedCardItemViewModel by activityViewModels()

        adapter = CardAdapter(
            items = cardItems,
            onItemClick = { clickedItem ->
                val bundle = Bundle().apply {
                    putParcelable("cardItem", clickedItem)
                }
                findNavController().navigate(R.id.action_homeFragment_to_detailFragment)
                sharedCardItemViewModel.selectedCardItem = clickedItem

            },
            onFavoriteToggle = { favoriteItem ->
                toggleFavorite(favoriteItem.Id)
            },
            showFavorites = true
        )

        if (!isOfflineDataFetched) {
            saveDataOfflineOnce()
        }
        recyclerView.adapter = adapter

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                loadMoreButton.visibility = View.GONE
            } else {
                if (searchView.query.isNullOrEmpty()) {
                    loadMoreButton.visibility = View.VISIBLE // Mostra o botão somente se a busca estiver vazia
                }
            }
        }

        loadMoreButton.setOnClickListener {
            loadMoreData(name)
        }

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


        // Load the first page of data
        if (cardItems.isEmpty()) {
            loadMoreData(name)
        }
    }

    private fun loadMoreData(name:String) {
        if (isLoading) return // Prevent simultaneous requests
        isLoading = true

        loadingSpinner.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        loadMoreButton.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                    return@launch
                }

                val response = RetrofitClient.instance.getAllCreatures(
                    token = "Bearer $token",
                    page = currentPage,
                    limit = 6,
                    name = name,
                    onlyFavoriteArcanes = false,
                    toSaveOffline = false
                )
                totalcountNotFavorites = response.count
                Log.d("LoadMoreData", "Total de não favoritos: $totalcountNotFavorites")
                Log.d("Data", "Total Dados: ${response.data}")
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
                        Toast.makeText(
                            requireContext(),
                            "No cards to display!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        recyclerView.visibility = View.VISIBLE
                    }

                    Log.d("CardItemSize", "Total carditem size: ${cardItems.size}")
                    loadMoreButton.visibility =
                        if (cardItems.size >= totalcountNotFavorites) View.GONE else View.VISIBLE
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

    private fun toggleFavorite(creatureId: Int) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null) {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(requireContext(), "Updated favorites", Toast.LENGTH_SHORT).show()
                        refreshData() // Refresh non-favorites after toggling
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update favorites: ${response.message()}",
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

    private fun refreshData() {
        currentPage = 1
        cardItems.clear()
        loadMoreData(name)
    }

    private fun performSearch(query: String?) {
        if (query.isNullOrEmpty()) {
            refreshData()
            return
        }

        loadingSpinner.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        loadMoreButton.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
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

                    recyclerView.visibility = View.VISIBLE
                    loadingSpinner.visibility = View.GONE
                    loadMoreButton.visibility = View.GONE
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
                }
            }
        }
    }

    private fun saveDataOfflineOnce() {
        if (isOfflineDataFetched) {
            Toast.makeText(requireContext(), "Offline data already fetched in this session.", Toast.LENGTH_SHORT).show()
            return
        }

        loadingSpinner.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.instance.getAllCreatures(
                    token = "Bearer $token",
                    page = 1,
                    limit = 10, // Ajuste conforme necessário
                    name = "",
                    onlyFavoriteArcanes = false,
                    toSaveOffline = true // Garantir que é para salvar offline
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

                // Salvar na cache usando Room
                saveToCache(newCardItems)
                Log.d("ArcaneDao", "Inserting data: $newCardItems")

                withContext(Dispatchers.Main) {
                    isOfflineDataFetched = true // Marca como feito para a sessão atual
                    loadingSpinner.visibility = View.GONE
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
                }
            }
        }
    }

    private fun saveToCache(cardItems: List<CardItem>) {
        val db = AppDatabase.getDatabase(requireContext()) // Obtenha a instância do banco de dados
        val entities = cardItems.map { item ->
            ArcaneEntity(
                id = item.Id,
                name = item.Name,
                img = item.Img,
                lore = item.Lore
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            db.arcaneDao().insertAll(entities) // Salvar os dados no banco de dados
        }
    }


}
