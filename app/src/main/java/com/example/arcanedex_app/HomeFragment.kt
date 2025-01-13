package com.example.arcanedex_app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arcanedex_app.data.CardItem
import com.example.arcanedex_app.data.api.RetrofitClient
import com.example.arcanedex_app.data.database.AppDatabase
import com.example.arcanedex_app.data.models.ArcaneEntity
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: CardAdapter
    private val cardItems = mutableListOf<CardItem>() // Full list for non-favorites
    private val filteredItems = mutableListOf<CardItem>() // Filtered list for display
    private var isLoading = false // To prevent multiple simultaneous loads
    private var currentPage = 1 // Current page for pagination
    private var totalRecords = 0

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
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = CardAdapter(
            items = filteredItems,
            onItemClick = { clickedItem ->
                val bundle = Bundle().apply {
                    putParcelable("cardItem", clickedItem)
                }
                findNavController().navigate(R.id.action_homeFragment_to_detailFragment, bundle)
            },
            onFavoriteToggle = { favoriteItem ->
                favoriteItem.isFavorite = !favoriteItem.isFavorite
                adapter.notifyDataSetChanged()
            },
            showFavorites = false
        )

        recyclerView.adapter = adapter

        // Set scroll listener for infinite scrolling
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // Trigger loading more data when the user scrolls to the bottom
                if (!isLoading && lastVisibleItemPosition + 1 >= totalItemCount && cardItems.size < totalRecords) {
                    loadMoreData()
                }
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterCards(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCards(newText)
                return true
            }
        })

        // Load the first page of data
        if (cardItems.isEmpty()) {
            loadMoreData()
        }
    }

    private fun loadMoreData() {
        if (isLoading) return // Prevent multiple requests
        isLoading = true

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
                    limit = 4
                )

                totalRecords = response.count

                withContext(Dispatchers.Main) {
                    val newCardItems = response.data.map { creature ->
                        CardItem(
                            Id = creature.Id,
                            Name = creature.Name,
                            Img = creature.Img,
                            Lore = creature.Lore,
                            isFavorite = creature.isFavorite
                        )
                    }

                    saveToCache(newCardItems) // Salvar no cache

                    val nonFavoriteItems = newCardItems.filter { !it.isFavorite }
                    cardItems.addAll(nonFavoriteItems)
                    filteredItems.addAll(nonFavoriteItems)
                    adapter.notifyDataSetChanged()

                    currentPage++
                    isLoading = false
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
                }
            }
        }
    }

    private fun filterCards(query: String?) {
        val searchText = query?.trim()?.lowercase() ?: ""
        filteredItems.clear()

        if (searchText.isEmpty()) {
            filteredItems.addAll(cardItems)
        } else {
            val filtered = cardItems.filter { it.Name.lowercase().contains(searchText) }
            filteredItems.addAll(filtered)
        }

        adapter.notifyDataSetChanged()
    }

    private fun saveToCache(cardItems: List<CardItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val entities = cardItems.map { item ->
                ArcaneEntity(
                    id = item.Id,
                    name = item.Name,
                    img = item.Img,
                    lore = item.Lore
                )
            }
            db.arcaneDao().insertAll(entities)
        }
    }

    private fun loadFromCache() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val cachedItems = db.arcaneDao().getAllArcanes()

            withContext(Dispatchers.Main) {
                val cachedCardItems = cachedItems.map { entity ->
                    CardItem(
                        Id = entity.id,
                        Name = entity.name,
                        Img = entity.img,
                        Lore = entity.lore,
                        isFavorite = false
                    )
                }

                cardItems.clear()
                filteredItems.clear()
                cardItems.addAll(cachedCardItems)
                filteredItems.addAll(cachedCardItems)
                adapter.notifyDataSetChanged()
            }
        }
    }
}
