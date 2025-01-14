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
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.api.BackgroundImageRequest
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.viewmodel.SharedCardItemViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class DetailFragment : Fragment() {

    private val CAMERA_REQUEST_CODE = 100
    private val CAMERA_PERMISSION_CODE = 101
    private var photoUri: Uri? = null
    private val sharedCardItemViewModel: SharedCardItemViewModel by activityViewModels()
    private lateinit var backgroundView: ImageView
    private var loadingOperations = 0
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

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

        // Populate UI
        titleTextView.text = cardItem.Name
        descriptionTextView.text = cardItem.Lore ?: "No description available"

        cardItem.Img?.let { img ->
            Glide.with(requireContext())
                .load(img)
                .into(imageView)
        } ?: run {
            imageView.setBackgroundResource(R.color.primary) // Default background
        }

        // Fetch and apply background image
        fetchAndApplyBackground(cardItem.Id, backgroundView)

        // Handle visibility of buttons
        if (cardItem.isFavorite) {
            addBackground.visibility = View.VISIBLE
            removeBackground.visibility = View.VISIBLE
        } else {
            addBackground.visibility = View.GONE
            removeBackground.visibility = View.GONE
        }

        addBackground.setOnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        removeBackground.setOnClickListener {
            val token = SharedPreferencesHelper.getToken(requireContext())
            if (token == null) return@setOnClickListener

            AlertDialog.Builder(requireContext()).setTitle("Remove fundo")
                .setMessage("Tens a certeza que queres remover o fundo?")
                .setPositiveButton("Sim") { _, _ ->
                    resetBackgroundToDefault(cardItem.Id, token)
                }.setNegativeButton("Não", null).show()
        }
    }

    private fun applyBackgroundWithGlide(base64Image: String, imageView: ImageView) {
        val imageUrl = base64Image
        Glide.with(requireContext())
            .load(imageUrl)
            .into(imageView)
    }

    private fun fetchAndApplyBackground(creatureId: Int, imageView: ImageView) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null) {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
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

    private fun resetBackgroundToDefault(creatureId: Int, token: String) {

        backgroundView.setImageDrawable(null)
        setLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.resetFavouriteCreatureBackground(
                    creatureId = creatureId, token = "Bearer $token"
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Fundo resetado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { setLoading(false) }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            loadingOperations++
            progressBar.visibility = View.VISIBLE
        } else {
            loadingOperations--
            if (loadingOperations <= 0) {
                loadingOperations = 0
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

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
            Toast.makeText(requireContext(), "Câmera não encontrada", Toast.LENGTH_SHORT).show()
        }
    }

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
                    "É preciso permissão para usar a câmera",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                val format = Bitmap.CompressFormat.JPEG
                val (base64Image, mimeType) = encodeImageToBase64(bitmap, format)

                // Call the API with the encoded image
                sendImageToApi(base64Image, mimeType, backgroundView)
            } ?: run {
                Toast.makeText(requireContext(), "Falha na captura de fotografia", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

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

    private fun sendImageToApi(base64Image: String, mimeType: String, view: ImageView) {
        val cardItem = sharedCardItemViewModel.selectedCardItem
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null || cardItem == null) {
            Toast.makeText(requireContext(), "Missing data for API call", Toast.LENGTH_SHORT).show()
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
                            "Background atualizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        applyBackgroundWithGlide(backgroundImg, view)
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Erro desconhecido"
                        Log.e("API Error", errorMessage)
                        Toast.makeText(
                            requireContext(),
                            "Erro ao atualizar o background: $errorMessage",
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
