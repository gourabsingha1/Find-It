package com.example.findit.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findit.adapter.ProductsAdapter
import com.example.findit.adapter.SearchTagsAdapter
import com.example.findit.databinding.ActivityAiSearchBinding
import com.example.findit.model.Products
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.asTextOrNull
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
        binding.rvAISearch.layoutManager = LinearLayoutManager(this)

        // Open gallery
        binding.ivAISearchImage.setOnClickListener {
            resultLauncher.launch("image/*")
        }

        binding.btnAISearchSearch.setOnClickListener {
            binding.pbAISearch.visibility = View.VISIBLE
            try{
                val generativeModel = GenerativeModel(
                    modelName = "gemini-pro-vision",
                    apiKey = "AIzaSyBwS-5ig3w_zxg14n5c7PgP85Ak4wwCvSo"
                )
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                CoroutineScope(Dispatchers.IO).launch {
                    val inputContent = content {
                        image(bitmap)
                        text("Print JUST the name of the thing in first line. Then print few similar names in next lines.")
                    }
                    val response = generativeModel.generateContent(inputContent)
                    withContext(Dispatchers.Main) {
                        // Add AI generated names in the array, to search using these names
                        val names = sentenceToWords(response.text!!)
                        // Add names to search tags list
                        binding.rvAISearchTags.adapter = SearchTagsAdapter(names)
                        loadProducts(names)
                    }
                }
            } catch (e : Exception) {
                Toast.makeText(this, "Server error", Toast.LENGTH_SHORT).show()
                binding.pbAISearch.visibility = View.INVISIBLE
            }
        }
    }

    // Set image in app
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
        binding.ivAISearchImage.setImageURI(it)
    }

    private fun sentenceToWords(text: String) : ArrayList<String> {
        val names = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            if (text[i].isLetter()) {
                val j = i
                while (i < text.length && (text[i].isLetter() || text[i].isDigit() || text[i] == ' ')) {
                    i++
                }
                val name = text.substring(j, i).trim().toLowerCase(Locale.ROOT)
                if(name.length < 20) {
                    names.add(name)
                }
            }
            i++
        }
        return names
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

                    // Add the list to RecyclerView
                    val productsAdapter = ProductsAdapter(productList)
                    binding.rvAISearch.adapter = productsAdapter
                    binding.tvSearchTags.visibility = View.VISIBLE
                    binding.pbAISearch.visibility = View.INVISIBLE

                    // Open product details when clicked on product
                    productsAdapter.onItemClick = { product ->
                        Intent(this@AISearchActivity, ProductDetailsActivity::class.java).also { intent ->
                            intent.putExtra("EXTRA_NAME", product.name)
                            intent.putExtra("EXTRA_PRICE", product.price)
                            intent.putExtra("EXTRA_LOCATION", product.location)
                            intent.putExtra("EXTRA_DESCRIPTION", product.description)
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