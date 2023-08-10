package com.example.findit.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findit.adapter.ProductsAdapter
import com.example.findit.databinding.ActivityMyProductsBinding
import com.example.findit.model.Products
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProductsBinding
    private val productList = ArrayList<Products>()
    private lateinit var adapter: ProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // RecyclerView
        binding.rvMyProducts.layoutManager = LinearLayoutManager(this)

        loadMyProducts()

        // search products
        binding.etMyProductsSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val query = s.toString()
                    adapter.filter.filter(query)
                } catch (e : Exception) {
                    Toast.makeText(this@MyProductsActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }

    private fun loadMyProducts() {
        val reference = FirebaseDatabase.getInstance().getReference("Products")
        val firebaseAuth = FirebaseAuth.getInstance()
        reference.orderByChild("uid").equalTo(firebaseAuth.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // does not let products reappear after deleting
                    productList.clear()
                    for(ds in snapshot.children) {
                        try {
                            val product = ds.getValue(Products::class.java)
                            productList.add(product!!)
                        } catch (e : Exception) {
                            Toast.makeText(this@MyProductsActivity, e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    adapter = ProductsAdapter(productList)
                    binding.rvMyProducts.adapter = adapter

                    // Open product details when clicked on product
                    adapter.onItemClick = { product ->
                        Intent(this@MyProductsActivity, ProductDetailsActivity::class.java).also { intent ->
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