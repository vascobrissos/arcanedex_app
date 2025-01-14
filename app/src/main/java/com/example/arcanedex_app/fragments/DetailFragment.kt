package com.example.arcanedex_app.fragments

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
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.arcanedex_app.R
import com.example.arcanedex_app.data.api.BackgroundImageRequest
import com.example.arcanedex_app.data.api.RetrofitClient
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper
import com.example.arcanedex_app.viewmodel.SharedCardItemViewModel
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
    private var capturedImageBase64: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    private val sharedCardItemViewModel: SharedCardItemViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardItem = sharedCardItemViewModel.selectedCardItem
        if (cardItem == null) {
            Toast.makeText(requireContext(), "Failed to load card details", Toast.LENGTH_SHORT)
                .show()
            requireActivity().onBackPressed()
            return
        }
        val addBackground = view.findViewById<ImageView>(R.id.cameraImage)
        val removeBackground = view.findViewById<ImageView>(R.id.defaultImage)


        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        // Populate UI with CardItem details
        titleTextView.text = cardItem.Name
        descriptionTextView.text = cardItem.Lore ?: "No description available"

        cardItem.Img?.let { img ->
            Glide.with(requireContext())
                .load(img)
                .into(imageView)
        } ?: run {
            imageView.setBackgroundResource(R.color.primary) // Default background
        }

        // Show buttons only if the card is marked as favorite
        if (cardItem.isFavorite) { // Check if favorite is true
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
            val cardItem = sharedCardItemViewModel.selectedCardItem
            val token = SharedPreferencesHelper.getToken(requireContext())

            if (cardItem == null || token == null) {
                Toast.makeText(requireContext(), "Missing data for API call", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Confirm removal
            AlertDialog.Builder(requireContext()).setTitle("Remove Background")
                .setMessage("Are you sure you want to remove the background?")
                .setPositiveButton("Yes") { _, _ ->
                    resetBackgroundToDefault(cardItem.Id, token)
                }.setNegativeButton("No", null).show()
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

    private var photoUri: Uri? = null

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile = createImageFile() // Create a temporary file
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
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
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
                    "Camera permission is required to take a picture",
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
                sendImageToApi(base64Image, mimeType)
            } ?: run {
                Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT)
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
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
        bitmap.compress(format, 50, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        return Pair(base64Image, mimeType)
    }


    private fun sendImageToApi(base64Image: String, mimeType: String) {
        val cardItem = sharedCardItemViewModel.selectedCardItem
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token == null || cardItem == null) {
            Toast.makeText(requireContext(), "Missing data for API call", Toast.LENGTH_SHORT).show()
            return
        }

        // Construir o objeto de requisição
        val backgroundImg = "data:$mimeType;base64,$base64Image"
        val request = BackgroundImageRequest(BackgroundImg = backgroundImg)

        // Chamada Retrofit com Coroutine
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
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Erro desconhecido"
                        Log.e("API Error", errorMessage)
                        Toast.makeText(
                            requireContext(),
                            "Erro ao atualizar o background: $errorMessage",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("API Failure", e.message ?: "Erro desconhecido")
                    Toast.makeText(
                        requireContext(),
                        "Falha na requisição: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun resetBackgroundToDefault(creatureId: Int, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.resetFavouriteCreatureBackground(
                    creatureId = creatureId, token = "Bearer $token"
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Background reset to default successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        /*
                        // Reset the UI image to a default placeholder
                        view?.findViewById<ImageView>(R.id.imageView)
                            ?.setImageResource(R.drawable.error_image)*/


                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("API Error", errorMessage)
                        Toast.makeText(
                            requireContext(),
                            "Failed to reset background: $errorMessage",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("API Failure", e.message ?: "Unknown error")
                    Toast.makeText(
                        requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


}
