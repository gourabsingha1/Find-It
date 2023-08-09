package com.example.findit.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.findit.activity.ProductDetailsActivity
import com.example.findit.adapter.HomeProductsAdapter
import com.example.findit.databinding.FragmentHomeBinding
import com.example.findit.model.Products
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.lang.Exception

// Fragment overlapping problem
// Search results not updated after wrong keyword

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val productList = ArrayList<Products>()
    private lateinit var adapter: HomeProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = FragmentHomeBinding.inflate(layoutInflater)
        
        // RecyclerView
        binding.rvHomeProducts.layoutManager = LinearLayoutManager(requireContext())

        loadProducts()

        // search products
        binding.etHomeSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val query = s.toString()
                    adapter.filter.filter(query)
                } catch (e : Exception) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

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
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                    }
                }

                // Add the list to RecyclerView
                adapter = HomeProductsAdapter(productList)
                binding.rvHomeProducts.adapter = adapter

                // Open product details when clicked on product
                adapter.onItemClick = { product ->
                    Intent(requireContext(), ProductDetailsActivity::class.java).also { intent ->
                        intent.putExtra("EXTRA_PRODUCT", product)
                        startActivity(intent)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }
}