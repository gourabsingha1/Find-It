package com.example.findit.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findit.activity.ProductDetailsActivity
import com.example.findit.adapter.ProductsAdapter
import com.example.findit.databinding.FragmentHomeBinding
import com.example.findit.model.Products
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.lang.Exception

// Search results not updated after wrong keyword

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val productList = ArrayList<Products>()
    private lateinit var adapter: ProductsAdapter
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = FragmentHomeBinding.inflate(layoutInflater)
        
        // RecyclerView
        binding.rvHomeProducts.layoutManager = LinearLayoutManager(requireContext())

        // load products
        loadProducts()

        // search products
        binding.homeSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                loadProducts()
                return false
            }
        })

        // location
        binding.tvHomeChooseLocation.setOnClickListener {
            Toast.makeText(requireContext(), "Location feature yet to implement", Toast.LENGTH_LONG).show()
        }

        return binding.root
    }

    // load products
    private fun loadProducts() {
        val reference = FirebaseDatabase.getInstance().getReference("Products")
        reference.addValueEventListener(object : ValueEventListener {
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