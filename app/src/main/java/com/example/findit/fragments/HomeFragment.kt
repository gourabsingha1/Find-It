package com.example.findit.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findit.R
import com.example.findit.activity.AISearchActivity
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
    private lateinit var adapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = FragmentHomeBinding.inflate(layoutInflater)

        // RecyclerView
        binding.rvHomeProducts.layoutManager = LinearLayoutManager(requireContext())

        // Enable smooth ScrollView
        binding.svHomeProducts.isSmoothScrollingEnabled = true

        // Load products
        loadProducts()

        // Search products
        binding.etHomeSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    val query = p0.toString()
                    adapter.filter.filter(query)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Search error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        // Location
        binding.tvHomeChooseLocation.setOnClickListener {
            Toast.makeText(requireContext(), "Location feature yet to implement", Toast.LENGTH_SHORT).show()
        }

        // Gemini
        binding.tvAISearch.setOnClickListener {
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

                for(ds in snapshot.children) {
                    try {
                        val product = ds.getValue(Products::class.java)
                        productList.add(product!!)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "HERE -> ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                // Add the list to RecyclerView
                adapter = ProductsAdapter(productList)
                binding.rvHomeProducts.adapter = adapter
                binding.pbHome.visibility = View.INVISIBLE

                // Open product details when clicked on product
                adapter.onItemClick = { product ->
                    Intent(requireContext(), ProductDetailsActivity::class.java).also { intent ->
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

            }

        })
    }
}