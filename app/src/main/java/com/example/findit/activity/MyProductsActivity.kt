package com.example.findit.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.adapter.HomeProductsAdapter
import com.example.findit.adapter.MyProductsAdapter
import com.example.findit.databinding.ActivityMyProductsBinding
import com.example.findit.model.Products
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class MyProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProductsBinding
    private val productList = ArrayList<Products>()
    private lateinit var adapter: HomeProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        loadMyProducts()

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
                    adapter = HomeProductsAdapter(productList)
                    binding.rvMyProducts.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}