package pt.ipt.arcanedex_app.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.arcanedex_app.Arcane
import pt.ipt.arcanedex_app.ArcaneAdapter
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.models.Creature.CreatureRequestAdmin
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import java.io.ByteArrayOutputStream

/**
 * Atividade responsável por apresentar a página de gestão de Arcanes
 */
class AdminFragment : Fragment() {
    // === Variáveis Globais ===
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArcaneAdapter
    private lateinit var searchView: SearchView
    private lateinit var totalCountTextView: TextView
    private lateinit var loadMoreButton: Button
    private var searchJob: Job? = null
    private val arcaneList = mutableListOf<Arcane>()
    private var currentPage = 1
    private val pageSize = 6
    private var totalCreaturesCount = 0

    private val FILE_PICKER_REQUEST_CODE = 102
    private var selectedImageUri: Uri? = null
    private var encodedImage: String? = null
    private var mimeType: String? = null
    private var imagePreview: ImageView? = null

    // === Ciclo de Vida ===
    /**
     * Infla o layout do fragmento e configura a interface do utilizador.
     *
     * @param inflater O inflater utilizado para inflar o layout.
     * @param container O container onde o layout será colocado.
     * @param savedInstanceState O estado anterior do fragmento, caso exista.
     * @return A vista inflada do fragmento.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    /**
     * Configura os elementos da UI e realiza a chamada para buscar as criaturas.
     *
     * @param view A vista inflada para o fragmento.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI(view)
        fetchCreatures()
    }

    // === Configurações de UI ===
    /**
     * Configura os elementos da interface de utilizador, como RecyclerView, SearchView, botões, etc.
     *
     * @param view A vista inflada para o fragmento.
     */
    private fun setupUI(view: View) {
        recyclerView = view.findViewById(R.id.arcanesRecyclerView)
        searchView = view.findViewById(R.id.searchView)
        totalCountTextView = view.findViewById(R.id.totalCountTextView)
        loadMoreButton = view.findViewById(R.id.loadMoreButton)
        val addButton = view.findViewById<FloatingActionButton>(R.id.addFloatingButton)

        adapter = ArcaneAdapter(
            arcanes = arcaneList,
            onEditClick = { arcane, position ->
                showEditDialog(arcane) { newName, newDescription, newImage ->
                    updateArcane(arcane.id, newName, newDescription, newImage)
                    updateArcaneLocally(arcane.id, newName, newDescription, position)
                }
            }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Configuração do SearchView para buscar as criaturas
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                fetchCreatures(searchQuery = query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel() // Cancela a pesquisa anterior, se estiver em andamento
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300) // Atraso de 300ms
                    fetchCreatures(searchQuery = newText)
                }
                return true
            }
        })

        // Configura o botão de adicionar novo Arcane
        addButton.setOnClickListener {
            showEditDialog(null) { name, description, image ->
                addArcane(name, description, image)
            }
        }

        // Configura o botão de carregar mais criaturas
        loadMoreButton.setOnClickListener {
            fetchCreatures(loadMore = true)
        }
    }

    // === Atualizações Locais ===
    /**
     * Atualiza o contador total de criaturas na interface de utilizador.
     */
    private fun updateTotalCount() {
        totalCountTextView.text = "Total: $totalCreaturesCount criaturas"
    }

    /**
     * Atualiza as informações de um Arcane na lista local, refletindo a alteração na interface.
     *
     * @param id O ID do Arcane a ser atualizado.
     * @param newName O novo nome do Arcane.
     * @param newDescription A nova descrição do Arcane.
     * @param position A posição do Arcane na lista.
     */
    private fun updateArcaneLocally(id: Int, newName: String, newDescription: String, position: Int) {
        val index = arcaneList.indexOfFirst { it.id == id }
        if (index >= 0) {
            arcaneList[index] = arcaneList[index].copy(
                name = newName,
                description = newDescription
            )
            adapter.notifyItemChanged(position)
            updateTotalCount()
        }
    }

    // === Manipulação de Criaturas ===
    /**
     * Função responsável por buscar as criaturas do servidor.
     * A função pode carregar mais criaturas ou fazer uma nova pesquisa, com base nos parâmetros fornecidos.
     *
     * @param loadMore Indica se a função deve carregar mais criaturas ou não. O valor padrão é `false`.
     * @param searchQuery A consulta de pesquisa para filtrar as criaturas. O valor padrão é `null`, que significa que todas as criaturas serão retornadas.
     */
    private fun fetchCreatures(loadMore: Boolean = false, searchQuery: String? = null) {

        // Exibe o spinner de carregamento enquanto aguarda os dados
        view?.findViewById<ProgressBar>(R.id.loading_spinner)?.visibility = View.VISIBLE
        view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
        view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
        recyclerView.visibility = View.GONE
        loadMoreButton.visibility = View.GONE
        view?.findViewById<FloatingActionButton>(R.id.addFloatingButton)?.visibility = View.GONE

        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Faz a chamada à API para buscar as criaturas
                    val response = RetrofitClient.instance.getAdminAllCreatures(
                        token = "Bearer $token",
                        page = if (loadMore) currentPage else 1,
                        limit = pageSize,
                        name = searchQuery ?: ""
                    )
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val responseData = response.body()
                            // Mapeia os dados recebidos para objetos Arcane
                            val creatures = responseData?.data?.map { creature ->
                                Arcane(
                                    id = creature.Id,
                                    name = creature.Name,
                                    description = creature.Lore,
                                    image = creature.Img
                                )
                            } ?: emptyList()

                            // Atualiza a lista de criaturas dependendo se é uma carga inicial ou de mais itens
                            if (loadMore) {
                                arcaneList.addAll(creatures)
                            } else {
                                arcaneList.clear()
                                arcaneList.addAll(creatures)
                            }

                            adapter.notifyDataSetChanged()

                            // Esconde o spinner de carregamento e mostra a lista de criaturas ou mensagem de "nenhum dado"
                            view?.findViewById<ProgressBar>(R.id.loading_spinner)?.visibility = View.GONE
                            if (arcaneList.isEmpty()) {
                                view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.VISIBLE
                                view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.VISIBLE
                            } else {
                                recyclerView.visibility = View.VISIBLE
                                view?.findViewById<ImageView>(R.id.no_data_image)?.visibility = View.GONE
                                view?.findViewById<TextView>(R.id.noDataText)?.visibility = View.GONE
                            }
                            // Mostra o botão flutuante de adicionar e atualiza o contador total de criaturas
                            view?.findViewById<FloatingActionButton>(R.id.addFloatingButton)?.visibility = View.VISIBLE
                            totalCreaturesCount = responseData?.totalCount ?: 0
                            if (!loadMore) currentPage = 1
                            currentPage++

                            // Exibe ou oculta o botão de "carregar mais", dependendo do total de criaturas
                            loadMoreButton.visibility =
                                if (arcaneList.size >= totalCreaturesCount) View.GONE else View.VISIBLE
                            updateTotalCount()
                            toggleLoadMoreButton()
                        } else {
                            // Exibe uma mensagem de erro caso a resposta da API falhe
                            Toast.makeText(requireContext(), "Erro ao encontrar criaturas", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    // Exibe uma mensagem de erro caso ocorra uma falha na requisição
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Falha na requisição: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Exibe uma mensagem caso o token de autenticação não seja encontrado
            Toast.makeText(requireContext(), "Token não encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Mostra ou oculta o botão "Load More" com base no número de arcanes carregados.
     * O botão será visível se o número de arcanes carregados for menor que o total de criaturas disponíveis.
     */
    private fun toggleLoadMoreButton() {
        loadMoreButton.visibility =
            if (arcaneList.size < totalCreaturesCount) View.VISIBLE else View.GONE
    }

    /**
     * Adiciona um novo arcane ao sistema.
     *
     * @param name Nome do arcane.
     * @param description Descrição do arcane.
     * @param image Imagem em Base64 opcional para o arcane.
     */
    private fun addArcane(name: String, description: String, image: String?) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val request = CreatureRequestAdmin(Name = name, Lore = description, Img = image)
                val response = RetrofitClient.instance.addCreature(request, "Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val newArcane = Arcane(
                            id = response.body()?.Id ?: 0,
                            name = name,
                            description = description,
                            image = image
                        )
                        arcaneList.add(newArcane)
                        adapter.notifyItemInserted(arcaneList.size - 1)
                        updateTotalCount()

                        Toast.makeText(requireContext(), "Arcane adicionado: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao adicionar arcane: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Token não encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Atualiza as informações de um arcane existente.
     *
     * @param arcaneId ID do arcane a ser atualizado.
     * @param name Novo nome do arcane.
     * @param description Nova descrição do arcane.
     * @param image Nova imagem em Base64 opcional para o arcane.
     */
    private fun updateArcane(arcaneId: Int, name: String, description: String, image: String?) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val request = CreatureRequestAdmin(Name = name, Lore = description, Img = image.toString())
                val response = RetrofitClient.instance.editCreature(arcaneId, request, "Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Arcane atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        fetchCreatures()
                    }
                }
            }
        }
    }

    /**
     * Exibe um diálogo para editar ou adicionar um arcane.
     *
     * @param arcane O arcane a ser editado, ou null para adicionar um novo.
     * @param onSave Callback executado ao guardar as alterações ou adicionar um novo arcane.
     */
    private fun showEditDialog(arcane: Arcane?, onSave: (String, String, String?) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_arcane, null)
        val arcaneNameEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneName)
        val arcaneDescriptionEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneDescription)
        val saveButton = dialogView.findViewById<Button>(R.id.buttonSave)
        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)
        val selectImageButton = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        imagePreview = dialogView.findViewById(R.id.imagePreview)

        var updatedEncodedImage: String? = null

        arcane?.let {
            arcaneNameEditText.setText(it.name)
            arcaneDescriptionEditText.setText(it.description)

            if (!it.image.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(it.image)
                    .placeholder(R.drawable.error_image)
                    .error(R.drawable.error_image)
                    .into(imagePreview!!)
            }
        }

        selectImageButton.setOnClickListener { openFileExplorer() }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            val newName = arcaneNameEditText.text.toString().trim()
            val newDescription = arcaneDescriptionEditText.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Nome não pode estar vazio", Toast.LENGTH_SHORT).show()
            } else {
                onSave(newName, newDescription, encodedImage ?: arcane?.image)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    /**
     * Abre o explorador de arquivos para selecionar uma imagem.
     */
    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    /**
     * Processa o resultado da seleção de imagem no explorador de arquivos.
     *
     * @param requestCode Código da solicitação.
     * @param resultCode Código do resultado.
     * @param data Dados retornados da atividade de seleção.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                try {
                    val contentResolver = requireContext().contentResolver
                    mimeType = contentResolver.getType(uri)

                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val outputStream = ByteArrayOutputStream()
                    val format = when (mimeType) {
                        "image/png" -> Bitmap.CompressFormat.PNG
                        "image/webp" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            Bitmap.CompressFormat.WEBP_LOSSY
                        } else {
                            Bitmap.CompressFormat.WEBP
                        }
                        else -> Bitmap.CompressFormat.JPEG
                    }

                    bitmap.compress(format, 100, outputStream)
                    val byteArray = outputStream.toByteArray()
                    encodedImage = "data:$mimeType;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)

                    imagePreview?.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Erro ao processar a imagem", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
