package com.example.findit.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findit.adapter.ProductsAdapter
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
    private lateinit var adapter: ProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWishlistProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // RecyclerView
        binding.rvWishlistProducts.layoutManager = LinearLayoutManager(this)

        // load wishlist products
        loadWishlistProducts()

        // search products
        binding.wishlistSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                loadWishlistProducts()
                return false
            }
        })
    }

    private fun loadWishlistProducts() {
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        val firebaseAuth = FirebaseAuth.getInstance()
        reference.child(firebaseAuth.uid!!).child("Wishlist")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // does not let products reappear after deleting
                    productList.clear()
                    for(ds in snapshot.children) {
                        val productId = "${ds.child("productId").value}"
                        val productRef = FirebaseDatabase.getInstance().getReference("Products")
                        productRef.child(productId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(productSnapshot: DataSnapshot) {
                                    try {
                                        val product = productSnapshot.getValue(Products::class.java)
                                        productList.add(product!!)
                                    } catch (e : Exception) {
                                        Toast.makeText(this@WishlistProductsActivity, e.message, Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {

                                }

                            })
                    }

                    // sometimes wishlist products don't load due to nested db listeners
                    Handler().postDelayed({
                        // Add the list to RecyclerView
                        adapter = ProductsAdapter(productList)
                        binding.rvWishlistProducts.adapter = adapter

                        // Open product details when clicked on product
                        adapter.onItemClick = { product ->
                            Intent(this@WishlistProductsActivity, ProductDetailsActivity::class.java).also { intent ->
                                intent.putExtra("EXTRA_NAME", product.name)
                                intent.putExtra("EXTRA_PRICE", product.price)
                                intent.putExtra("EXTRA_LOCATION", product.location)
                                intent.putExtra("EXTRA_DESCRIPTION", product.description)
                                intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                                startActivity(intent)
                            }
                        }
                    }, 500)
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}