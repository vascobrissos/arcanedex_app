package pt.ipt.arcanedex_app.fragments

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
import pt.ipt.arcanedex_app.CardAdapter
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.CardItem
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.viewmodel.SharedCardItemViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favourites, container, false)
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

        loadMoreButton.setOnClickListener {
            loadFavourites(name)
        }

        val sharedCardItemViewModel: SharedCardItemViewModel by activityViewModels()

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
            loadFavourites(name)
        }

    }


    private fun loadFavourites(name: String) {
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
                        Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT)
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

                Log.d("LoadFavourites", "Response: ${response.data}")

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
                            "No favorites to display!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        recyclerView.visibility = View.VISIBLE
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

    private fun performSearch(query: String?) {
        if (query.isNullOrEmpty()) {
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
                        Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT)
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
                }
            }
        }
    }


    private fun refreshData() {
        currentPage = 1
        cardItems.clear()
        loadFavourites(name)
    }

    private fun removeFavorite(creatureId: Int) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null) {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
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
                            "Removed from favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshData()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to remove from favorites: ${response.message()}",
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
