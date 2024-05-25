package com.example.findit.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findit.R
import com.example.findit.activity.AISearchActivity
import com.example.findit.activity.LocationPickerActivity
import com.example.findit.activity.ProductDetailsActivity
import com.example.findit.adapter.ProductsAdapter
import com.example.findit.databinding.FragmentHomeBinding
import com.example.findit.model.Products
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val productList = ArrayList<Products>()
    private lateinit var adapter1: ProductsAdapter
    private lateinit var adapter2: ProductsAdapter
    private lateinit var adapter3: ProductsAdapter
    private lateinit var locationSp: SharedPreferences
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentAddress = ""
    private val MAX_DISTANCE_TO_LOAD_PRODUCTS_KM = 15

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = FragmentHomeBinding.inflate(layoutInflater)

        // Set Location
        locationSp = requireContext().getSharedPreferences("LOCATION_SP", Context.MODE_PRIVATE)
        currentLatitude = locationSp.getFloat("CURRENT_LATITUDE", 0.0f).toDouble()
        currentLongitude = locationSp.getFloat("CURRENT_LONGITUDE", 0.0f).toDouble()
        currentAddress = locationSp.getString("CURRENT_ADDRESS", "")!!
        if (currentLatitude != 0.0 && currentLongitude != 0.0) {
            binding.tvHomeChooseLocation.text = currentAddress
        }

        // Load products
        loadProducts()

        // Search products
        binding.svHomeSearch.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                adapter3.filter.filter(s.toString())
                binding.rvHomeSearchView.adapter = adapter3
                // Open product details when clicked on product
                adapter3.onItemClick = { product ->
                    Intent(
                        requireContext(), ProductDetailsActivity::class.java
                    ).also { intent ->
                        intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                        startActivity(intent)
                    }
                }
            }

        })

        // Location
        binding.cvHomeChooseLocation.setOnClickListener {
            val intent = Intent(requireContext(), LocationPickerActivity::class.java)
            locationPickerActivityResultLauncher.launch(intent)
        }

        // Gemini
        binding.ivGemini.setOnClickListener {
            startActivity(Intent(requireContext(), AISearchActivity::class.java))
        }

        return binding.root
    }

    // Load products
    private fun loadProducts() {
        FirebaseDatabase.getInstance().getReference("Products")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // does not let products reappear after deleting
                    productList.clear()

                    for (ds in snapshot.children) {
                        try {
                            val product = ds.getValue(Products::class.java)
                            val distance = calculateDistanceKm(
                                product?.latitude ?: 0.0, product?.longitude ?: 0.0
                            )
                            if (distance <= MAX_DISTANCE_TO_LOAD_PRODUCTS_KM) {
                                productList.add(product!!)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(), "HERE -> ${e.message}", Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    val productListLeft = arrayListOf<Products>()
                    val productListRight = arrayListOf<Products>()
                    val sizeLeft = (productList.size + 1) / 2
                    for (product in productList) {
                        if(productListLeft.size < sizeLeft) {
                            productListLeft.add(product)
                        } else {
                            productListRight.add(product)
                        }
                    }

                    // Add the list to RecyclerView
                    adapter1 = ProductsAdapter(productListLeft)
                    binding.rvHomeProducts1.adapter = adapter1
                    adapter2 = ProductsAdapter(productListRight)
                    binding.rvHomeProducts2.adapter = adapter2
                    adapter3 = ProductsAdapter(productList)
                    binding.pbHome.visibility = View.INVISIBLE

                    // Open product details when clicked on product
                    adapter1.onItemClick = { product ->
                        Intent(
                            requireContext(), ProductDetailsActivity::class.java
                        ).also { intent ->
                            intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                            startActivity(intent)
                        }
                    }
                    // Open product details when clicked on product
                    adapter2.onItemClick = { product ->
                        Intent(
                            requireContext(), ProductDetailsActivity::class.java
                        ).also { intent ->
                            intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                            startActivity(intent)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun calculateDistanceKm(productLatitude: Double, productLongitude: Double): Double {
        val startPoint = Location(LocationManager.NETWORK_PROVIDER)
        startPoint.latitude = currentLatitude
        startPoint.longitude = currentLongitude
        val endPoint = Location(LocationManager.NETWORK_PROVIDER)
        endPoint.latitude = productLatitude
        endPoint.longitude = productLongitude
        val distanceInMeters = startPoint.distanceTo(endPoint).toDouble()
        return distanceInMeters / 1000
    }

    // Get location result
    private val locationPickerActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    currentLatitude = data.getDoubleExtra("latitude", 0.0)
                    currentLongitude = data.getDoubleExtra("longitude", 0.0)
                    currentAddress = data.getStringExtra("address") ?: ""
                    locationSp.edit().putFloat("CURRENT_LATITUDE", currentLatitude.toFloat())
                        .putFloat("CURRENT_LONGITUDE", currentLongitude.toFloat())
                        .putString("CURRENT_ADDRESS", currentAddress).apply()
                    binding.tvHomeChooseLocation.text = currentAddress
                    loadProducts()
                } else {
                    Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }
}