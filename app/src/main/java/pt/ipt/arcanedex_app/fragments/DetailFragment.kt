package pt.ipt.arcanedex_app.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.api.BackgroundImageRequest
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.viewmodel.SharedCardItemViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * Fragmento de detalhes que exibe informações de uma criatura específica.
 * O fragmento permite visualizar detalhes como o nome, descrição, imagem e fundo da criatura.
 * Além disso, o utilizador pode adicionar ou remover um fundo personalizado para a criatura.
 */
class DetailFragment : Fragment() {

    private val CAMERA_REQUEST_CODE = 100
    private val CAMERA_PERMISSION_CODE = 101
    private var photoUri: Uri? = null
    private val sharedCardItemViewModel: SharedCardItemViewModel by activityViewModels()
    private lateinit var backgroundView: ImageView
    private var loadingOperations = 0
    private lateinit var progressBar: ProgressBar

    /**
     * Cria a view para o fragmento.
     * Inicializa o layout do fragmento de detalhes da criatura.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    /**
     * Configura a interface do utilizador após a criação da view.
     * Popula os campos com informações da criatura selecionada.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.loading_spinner)
        progressBar.visibility = View.GONE // Inicialmente oculto

        val cardItem = sharedCardItemViewModel.selectedCardItem
        if (cardItem == null) {
            requireActivity().onBackPressed()
            return
        }

        val addBackground = view.findViewById<ImageView>(R.id.cameraImage)
        val removeBackground = view.findViewById<ImageView>(R.id.defaultImage)
        backgroundView = view.findViewById<ImageView>(R.id.backgroundView)
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        // Preenche a UI com dados da criatura
        titleTextView.text = cardItem.Name
        descriptionTextView.text = cardItem.Lore ?: "No description available"

        // Carrega imagem da criatura
        cardItem.Img?.let { img ->
            Glide.with(requireContext())
                .load(img)
                .into(imageView)
        } ?: run {
            imageView.setBackgroundResource(R.color.primary) // Fundo padrão
        }

        // Busca e aplica a imagem de fundo
        fetchAndApplyBackground(cardItem.Id, backgroundView)

        // Controla a visibilidade dos botões
        if (cardItem.isFavorite) {
            addBackground.visibility = View.VISIBLE
            removeBackground.visibility = View.VISIBLE
        } else {
            addBackground.visibility = View.GONE
            removeBackground.visibility = View.GONE
        }

        // Configura o botão para adicionar fundo
        addBackground.setOnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        // Configura o botão para remover fundo
        removeBackground.setOnClickListener {
            val token = SharedPreferencesHelper.getToken(requireContext())
            if (token == null) return@setOnClickListener

            AlertDialog.Builder(requireContext()).setTitle("Remover Fundo")
                .setMessage("Tem a certeza que pretende remover o fundo?")
                .setPositiveButton("Sim") { _, _ ->
                    resetBackgroundToDefault(cardItem.Id, token)
                }.setNegativeButton("Não", null).show()
        }
    }

    /**
     * Aplica o fundo fornecido usando Glide.
     *
     * @param base64Image Imagem codificada em base64.
     * @param imageView ImageView onde a imagem será aplicada.
     */
    private fun applyBackgroundWithGlide(base64Image: String, imageView: ImageView) {
        val imageUrl = base64Image
        Glide.with(requireContext())
            .load(imageUrl)
            .into(imageView)
    }

    /**
     * Restaura o fundo de uma criatura favorita para o estado padrão.
     *
     * @param creatureId O ID da criatura cuja imagem de fundo será redefinida.
     * @param token O token de autenticação do utilizador.
     */
    private fun resetBackgroundToDefault(creatureId: Int, token: String) {
        backgroundView.setImageDrawable(null) // Remove a imagem de fundo atual
        setLoading(true) // Ativa o estado de carregamento

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Faz uma chamada à API para redefinir o fundo
                val response = RetrofitClient.instance.resetFavouriteCreatureBackground(
                    creatureId = creatureId, token = "Bearer $token"
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Mostra uma mensagem de sucesso ao utilizador
                        Toast.makeText(
                            requireContext(),
                            "Fundo eliminado!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    setLoading(false) // Desativa o estado de carregamento
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Desativa o estado de carregamento mesmo em caso de erro
                    setLoading(false)
                }
            }
        }
    }

    /**
     * Busca os detalhes de uma criatura e aplica a imagem de fundo.
     *
     * @param creatureId ID da criatura para buscar os detalhes.
     * @param imageView ImageView onde a imagem de fundo será aplicada.
     */
    private fun fetchAndApplyBackground(creatureId: Int, imageView: ImageView) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null) {
            Toast.makeText(requireContext(), "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getCreatureDetails(
                    creatureId = creatureId,
                    token = "Bearer $token"
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val backgroundImgBase64 = response.body()?.BackgroundImg
                        if (!backgroundImgBase64.isNullOrEmpty()) {
                            applyBackgroundWithGlide(backgroundImgBase64, imageView)
                        }
                    }
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { setLoading(false) }
            }
        }
    }

    /**
     * Configura o estado de carregamento (visibilidade do ProgressBar).
     *
     * @param isLoading Define se o processo de carregamento está ativo ou não.
     */
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }
    }

    /**
     * Verifica se o utilizador tem permissão para usar a câmara.
     *
     * @return true se a permissão para usar a câmara foi concedida, caso contrário false.
     */
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Solicita permissão para usar a câmara.
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    /**
     * Abre a câmara para tirar uma foto.
     * A foto será salva em um arquivo temporário.
     */
    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.let {
                photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    it
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
            }
        } else {
            Toast.makeText(requireContext(), "Câmara não encontrada", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cria um arquivo temporário para armazenar a foto tirada pela câmara.
     *
     * @return o arquivo de imagem temporário ou null se houver um erro na criação.
     */
    private fun createImageFile(): File? {
        return try {
            val timestamp = System.currentTimeMillis().toString()
            val storageDir = requireContext().cacheDir
            File.createTempFile("IMG_$timestamp", ".jpg", storageDir)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }

    /**
     * Manipula o resultado da solicitação de permissão para usar a câmara.
     * Se a permissão for concedida, a câmara será aberta.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "É necessário permissão para utilizar a câmara",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Manipula o resultado da atividade de captura de imagem.
     * Quando uma imagem é tirada, ela é processada e enviada para a API.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                val format = Bitmap.CompressFormat.JPEG
                val (base64Image, mimeType) = encodeImageToBase64(bitmap, format)

                // Chama a API com a imagem codificada
                sendImageToApi(base64Image, mimeType, backgroundView)
            } ?: run {
                Toast.makeText(requireContext(), "Falha na captura de fotografia", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /**
     * Codifica a imagem para o formato Base64.
     *
     * @param bitmap A imagem a ser codificada.
     * @param format O formato de compressão da imagem.
     * @return Um par contendo a imagem codificada em Base64 e o tipo MIME da imagem.
     */
    private fun encodeImageToBase64(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat
    ): Pair<String, String> {
        val outputStream = ByteArrayOutputStream()
        val mimeType = when (format) {
            Bitmap.CompressFormat.JPEG -> "image/jpeg"
            Bitmap.CompressFormat.PNG -> "image/png"
            else -> throw IllegalArgumentException("Formato não suportado: $format")
        }
        bitmap.compress(format, 50, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        return Pair(base64Image, mimeType)
    }

    /**
     * Envia a imagem codificada para a API para atualizar o fundo da criatura.
     *
     * @param base64Image A imagem codificada em Base64.
     * @param mimeType O tipo MIME da imagem.
     * @param view O ImageView onde a nova imagem de fundo será exibida.
     */
    private fun sendImageToApi(base64Image: String, mimeType: String, view: ImageView) {
        val cardItem = sharedCardItemViewModel.selectedCardItem
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null || cardItem == null) {
            Toast.makeText(requireContext(), "Dados em falta para API", Toast.LENGTH_SHORT).show()
            return
        }

        val backgroundImg = "data:$mimeType;base64,$base64Image"
        val request = BackgroundImageRequest(BackgroundImg = backgroundImg)

        setLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.changeFavouriteCreatureBackground(
                    creatureId = cardItem.Id,
                    request = request,
                    token = "Bearer $token",
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Fundo atualizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        applyBackgroundWithGlide(backgroundImg, view)
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Erro desconhecido"
                        Log.e("API Error", errorMessage)
                        Toast.makeText(
                            requireContext(),
                            "Erro ao atualizar o fundo: $errorMessage",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("API Failure", e.message ?: "Erro desconhecido")
                    Toast.makeText(
                        requireContext(),
                        "Falha na requisição: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    setLoading(false)
                }
            }
        }
    }
}
