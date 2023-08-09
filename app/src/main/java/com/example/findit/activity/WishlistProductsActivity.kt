package com.example.findit.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findit.adapter.HomeProductsAdapter
import com.example.findit.adapter.WishlistProductsAdapter
import com.example.findit.databinding.ActivityWishlistProductsBinding
import com.example.findit.model.Products
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WishlistProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWishlistProductsBinding
    private val productList = ArrayList<Products>()
    private lateinit var adapter: HomeProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWishlistProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // RecyclerView
        binding.rvWishlistProducts.layoutManager = LinearLayoutManager(this)

        loadWishlistProducts()

    }

    private fun loadWishlistProducts() {
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        val firebaseAuth = FirebaseAuth.getInstance()
        reference.child(firebaseAuth.uid!!).child("Wishlist")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    productList.clear()
                    for(ds in snapshot.children) {
                        val productId = "${ds.child("productId").value}"
                        val productRef = FirebaseDatabase.getInstance().getReference("Products")
                        productRef.child(productId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    try {
                                        val product = snapshot.getValue(Products::class.java)
                                        productList.add(product!!)
                                    } catch (e : Exception) {
                                        Toast.makeText(this@WishlistProductsActivity, e.message, Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {

                                }

                            })
                    }

                    // Add the list to RecyclerView
                    adapter = HomeProductsAdapter(productList)
                    binding.rvWishlistProducts.adapter = adapter

                    // Open product details when clicked on product
                    adapter.onItemClick = { product ->
                        Intent(this@WishlistProductsActivity, ProductDetailsActivity::class.java).also { intent ->
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