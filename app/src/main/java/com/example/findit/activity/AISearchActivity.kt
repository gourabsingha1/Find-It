package com.example.findit.activity

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findit.adapter.ImagesPickedAdapter
import com.example.findit.adapter.ProductsAdapter
import com.example.findit.adapter.SearchTagsAdapter
import com.example.findit.databinding.ActivityAiSearchBinding
import com.example.findit.model.ImagesPicked
import com.example.findit.model.Products
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AISearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiSearchBinding
    private val productList = ArrayList<Products>()
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView
        binding.rvAISearchTags.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Open gallery
        binding.ivAISearchImage.setOnClickListener {
            showImagePickOptions()
        }

        binding.btnAISearchSearch.setOnClickListener {
            binding.pbAISearch.visibility = View.VISIBLE
            binding.tvAISearchNoProductsFound.visibility = View.INVISIBLE

            try{
                val generativeModel = GenerativeModel(
                    modelName = "gemini-pro-vision",
                    apiKey = "AIzaSyDOopZ9zt_n4ISQT35steAwbLZKeEbfms0"
                )
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                CoroutineScope(Dispatchers.IO).launch {
                    val inputContent = content {
                        image(bitmap)
                        text("Print JUST the name of the thing, then maximum 14 unique items similar to it. I just want the items in a single line separated by semi colon.")
                    }
                    val response = generativeModel.generateContent(inputContent)
                    withContext(Dispatchers.Main) {
                        val names = extractItems(response.text!!)
                        binding.rvAISearchTags.adapter = SearchTagsAdapter(names)
                        loadProducts(names)
                    }
                }
            } catch (e : Exception) {
                Toast.makeText(this, "Pick a photo", Toast.LENGTH_SHORT).show()
                binding.pbAISearch.visibility = View.INVISIBLE
            }
        }
    }

    private fun showImagePickOptions() {
        val popupMenu = PopupMenu(this, binding.ivAISearchImage)
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
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
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
            binding.ivAISearchImage.setImageURI(imageUri)
            binding.tvPickAPhoto.visibility = View.INVISIBLE
        } else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
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
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_IMAGE_TITLE")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMP_IMAGE_DESCRIPTION")
        imageUri = contentResolver?.insert(
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
            binding.ivAISearchImage.setImageURI(imageUri)
            binding.tvPickAPhoto.visibility = View.INVISIBLE
        } else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Add AI generated items in the array
    private fun extractItems(text: String): ArrayList<String> {
        val res = ArrayList<String>()
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

    // Load products
    private fun loadProducts(names: ArrayList<String>) {
        FirebaseDatabase.getInstance().getReference("Products")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // does not let products reappear after deleting
                    productList.clear()

                    for(ds in snapshot.children) {
                        try {
                            val product = ds.getValue(Products::class.java)!!
                            val searchTags = product.searchTags!!
                            for(name in names) {
                                if(product !in productList && name in searchTags) {
                                    productList.add(product)
                                }
                                break
                            }
                        } catch (e: Exception) {
                            Log.e("AISearchActivityError: ", e.message!!)
                        }
                    }

                    if(productList.size == 0) {
                        binding.tvAISearchNoProductsFound.visibility = View.VISIBLE
                    }

                    // Add the list to RecyclerView
                    val productsAdapter = ProductsAdapter(productList)
                    binding.rvAISearch.adapter = productsAdapter
                    binding.tvSearchTags.visibility = View.VISIBLE
                    binding.pbAISearch.visibility = View.INVISIBLE

                    // Open product details when clicked on product
                    productsAdapter.onItemClick = { product ->
                        Intent(this@AISearchActivity, ProductDetailsActivity::class.java).also { intent ->
                            intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                            startActivity(intent)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AISearchActivityError", error.message)
                    binding.pbAISearch.visibility = View.INVISIBLE
                }

            })
    }
}