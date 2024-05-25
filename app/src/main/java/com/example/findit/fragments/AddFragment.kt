package com.example.findit.fragments

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.findit.R
import com.example.findit.activity.LocationPickerActivity
import com.example.findit.adapter.ImagesPickedAdapter
import com.example.findit.databinding.FragmentAddBinding
import com.example.findit.model.ImagesPicked
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// findNavController glitching

class AddFragment : Fragment() {

    private lateinit var binding: FragmentAddBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var imageUri: Uri? = null
    private lateinit var etAddName: String
    private var etAddPrice = 0.0
    private lateinit var etAddDescription: String
    private lateinit var address: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var imagePickedAdapter: ImagesPickedAdapter
    private var imagePickedArraylist: ArrayList<ImagesPicked> = ArrayList()
    private lateinit var progressDialog: ProgressDialog
    private lateinit var generativeModel: GenerativeModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddBinding.inflate(layoutInflater)

        // Init progressDialog
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Get location
        binding.acAddLocation.setOnClickListener {
            val intent = Intent(requireContext(), LocationPickerActivity::class.java)
            locationPickerActivityResultLauncher.launch(intent)
        }

        // Open gallery
        binding.ivAddImages.setOnClickListener {
            showImagePickOptions()
        }

        // Submit Product
        binding.btnSubmitProduct.setOnClickListener {
            validateData()
        }

        binding.btnUseAI.setOnClickListener {
            if(binding.etAddName.text.toString().trim().isEmpty()) {
                binding.etAddName.error = "Enter product name"
                binding.etAddName.requestFocus()
            } else {
                createDescription(binding.etAddName.text.toString().trim())
            }
        }

        // Initialize Gemini model
        generativeModel = GenerativeModel(
            modelName = "gemini-pro", apiKey = "AIzaSyBwS-5ig3w_zxg14n5c7PgP85Ak4wwCvSo"
        )

        return binding.root
    }

    // Create description using AI
    private fun createDescription(productName: String) {
        binding.pbAdd.visibility = View.VISIBLE
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val searchQuery = "Describe $productName within 20 words."
                val response = generativeModel.generateContent(searchQuery)
                withContext(Dispatchers.Main) {
                    binding.etAddDescription.setText(response.text!!)
                    binding.pbAdd.visibility = View.INVISIBLE
                }
            }
        } catch (e: Exception) {
            binding.pbAdd.visibility = View.INVISIBLE
            Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
        }
    }

    // Get location result
    private val locationPickerActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    latitude = data.getDoubleExtra("latitude", 0.0)
                    longitude = data.getDoubleExtra("longitude", 0.0)
                    address = data.getStringExtra("address") ?: ""
                    binding.acAddLocation.setText(address)
                } else {
                    Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun showImagePickOptions() {
        val popupMenu = PopupMenu(requireContext(), binding.ivAddImages)
        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if (itemId == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val cameraPermissions = arrayOf(Manifest.permission.CAMERA)
                    requestCameraPermission.launch(cameraPermissions)
                } else {
                    val cameraPermissions = arrayOf(
                        Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    requestCameraPermission.launch(cameraPermissions)
                }
            } else if (itemId == 2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickImageGallery()
                } else {
                    val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                    requestStoragePermission.launch(storagePermission)
                }
            }
            true
        }
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            imageUri = data!!.data
            val timestamp = "${System.currentTimeMillis()}"
            val imagePicked = ImagesPicked(timestamp, imageUri, null, false)
            imagePickedArraylist.add(imagePicked)
            loadImages()
        } else {
            Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Load images picked
    private fun loadImages() {
        imagePickedAdapter = ImagesPickedAdapter(requireContext(), imagePickedArraylist)
        binding.rvImages.adapter = imagePickedAdapter
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        var areAllGranted = true
        for (isGranted in result.values) {
            areAllGranted = areAllGranted && isGranted
        }
        if (areAllGranted) {
            pickImageCamera()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_IMAGE_TITLE")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMP_IMAGE_DESCRIPTION")
        imageUri = activity?.contentResolver?.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val timestamp = "${System.currentTimeMillis()}"
            val imagePicked = ImagesPicked(timestamp, imageUri, null, false)
            imagePickedArraylist.add(imagePicked)
            loadImages()
        } else {
            Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Validate data
    private fun validateData() {
        etAddName = binding.etAddName.text.toString().trim()
        etAddDescription = binding.etAddDescription.text.toString().trim()
        address = binding.acAddLocation.text.toString().trim()
        if (etAddName.isEmpty()) {
            binding.etAddName.error = "Enter product name"
            binding.etAddName.requestFocus()
        } else if (binding.etAddPrice.length() == 0) {
            binding.etAddPrice.error = "Enter product price"
            binding.etAddPrice.requestFocus()
        } else if (address.isEmpty()) {
            Toast.makeText(requireContext(), "Choose product location", Toast.LENGTH_SHORT).show()
        } else if (etAddDescription.isEmpty()) {
            binding.etAddDescription.error = "Enter product description"
            binding.etAddDescription.requestFocus()
        } else if (imagePickedArraylist.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one image", Toast.LENGTH_SHORT).show()
        } else {
            etAddPrice = binding.etAddPrice.text.toString().trim().toDouble()
            // If AI is able to generate search tags out of given image, then only add the product
            getProductSearchTags(etAddName)
        }
    }

    // Get product search tags
    private fun getProductSearchTags(productName: String) {
        binding.pbAdd.visibility = View.VISIBLE
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val searchQuery =
                    "Maximum 9 items similar to $productName. I just want the items in a single line separated by semi colon."
                val response = generativeModel.generateContent(searchQuery)
                withContext(Dispatchers.Main) {
                    uploadProduct(extractItems(productName, response.text!!))
                }
            }
        } catch (e: Exception) {
            binding.pbAdd.visibility = View.INVISIBLE
            Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
        }
    }

    // Add AI generated items in the array
    private fun extractItems(productName: String, text: String): ArrayList<String> {
        val res = ArrayList<String>()
        res.add(productName)
        val n = text.length
        var i = 0
        while (i < n) {
            var name = ""
            while (i < n && text[i] != ';') {
                name += text[i++]
            }
            res.add(name.trim())
            i++
        }
        return res
    }

    private fun uploadProduct(searchTags: ArrayList<String>) {
        val reference = FirebaseDatabase.getInstance().getReference("Products")
        val productId = reference.push().key!!
        val timestamp = System.currentTimeMillis()
        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = etAddName
        hashMap["price"] = etAddPrice
        hashMap["description"] = etAddDescription
        hashMap["timestamp"] = timestamp
        hashMap["uid"] = firebaseAuth.uid
        hashMap["productId"] = productId
        hashMap["searchTags"] = searchTags
        hashMap["latitude"] = latitude
        hashMap["longitude"] = longitude
        hashMap["address"] = address

        // Set data to firebase realtime db
        reference.child(productId).setValue(hashMap).addOnSuccessListener {
            uploadImagesToStorage(productId)
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    // Upload product image to firebase storage
    private fun uploadImagesToStorage(productId: String) {
        for (i in imagePickedArraylist.indices) {
            val imagePicked = imagePickedArraylist[i]
            val imageName = imagePicked.id
            val filePathAndName = "Products/$imageName"
            val imageIndexForProgress = i + 1
            val storageRef = FirebaseStorage.getInstance().getReference(filePathAndName)
            storageRef.putFile(imagePicked.imageUri!!).addOnProgressListener { snapshot ->
                    val progress = 100.0 * snapshot.bytesTransferred / snapshot.totalByteCount
                    val message =
                        "Uploading $imageIndexForProgress of ${imagePickedArraylist.size} images. Progress ${progress.toInt()}%"
                    progressDialog.setMessage(message)
                    progressDialog.show()
                }.addOnSuccessListener { taskSnapshot ->
                    val uriTask = taskSnapshot.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val uploadedImageUrl = uriTask.result
                    if (uriTask.isSuccessful) {
                        val hashMap = HashMap<String, Any>()
                        hashMap["id"] = "${imagePicked.id}"
                        hashMap["imageUrl"] = "$uploadedImageUrl"
                        val ref = FirebaseDatabase.getInstance().getReference("Products")
                        ref.child(productId).child("images").child(imageName)
                            .updateChildren(hashMap)
                    }
                    progressDialog.dismiss()
                }.addOnFailureListener {
                    Log.e("", it.toString())
                    progressDialog.dismiss()
                }
        }
//        Handler(Looper.getMainLooper()).postDelayed({
//            Toast.makeText(requireContext(), "Product added successfully", Toast.LENGTH_SHORT).show()
            binding.pbAdd.visibility = View.INVISIBLE
//        }, 2000)

        // Erase previous data
//        binding.etAddName.text.clear()
//        binding.etAddPrice.text.clear()
//        binding.etAddDescription.text.clear()
//        imageUri = null
//        binding.pbAdd.visibility = View.INVISIBLE
//        findNavController().navigate(R.id.action_addFragment_to_homeFragment)
    }
}