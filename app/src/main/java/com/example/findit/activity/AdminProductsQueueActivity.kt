package com.example.findit.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.R
import com.example.findit.adapter.ProductsAdapter
import com.example.findit.databinding.ActivityAdminProductsQueueBinding
import com.example.findit.model.Products
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminProductsQueueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProductsQueueBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val productList = ArrayList<Products>()
    private lateinit var adapter1: ProductsAdapter
    private lateinit var adapter2: ProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProductsQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Go back
        binding.ivAdminProductsQueueBack.setOnClickListener {
            Intent(this@AdminProductsQueueActivity, AdminActivity::class.java).also {
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finishAffinity()
            }
        }

        // Load products
        loadProducts()

        binding.pbAdminProductsQueue.visibility = View.VISIBLE
    }

    // Load products
    private fun loadProducts() {
        FirebaseDatabase.getInstance().getReference("ProductsQueue")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // does not let products reappear after deleting
                    productList.clear()

                    for (ds in snapshot.children) {
                        try {
                            val product = ds.getValue(Products::class.java)
                            productList.add(product!!)
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@AdminProductsQueueActivity, "HERE -> ${e.message}", Toast.LENGTH_LONG
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
                    adapter1 = ProductsAdapter(productListLeft, "ProductsQueue")
                    binding.rvAdminProductsQueue1.adapter = adapter1
                    adapter2 = ProductsAdapter(productListRight, "ProductsQueue")
                    binding.rvAdminProductsQueue2.adapter = adapter2
                    binding.pbAdminProductsQueue.visibility = View.INVISIBLE

                    // Open product details when clicked on product
                    adapter1.onItemClick = { product ->
                        Intent(
                            this@AdminProductsQueueActivity, AdminProductsProductDetailsActivity::class.java
                        ).also { intent ->
                            intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                            intent.putExtra("EXTRA_PATH", "ProductsQueue")
                            startActivity(intent)
                        }
                    }
                    // Open product details when clicked on product
                    adapter2.onItemClick = { product ->
                        Intent(
                            this@AdminProductsQueueActivity, AdminProductsProductDetailsActivity::class.java
                        ).also { intent ->
                            intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                            intent.putExtra("EXTRA_PATH", "ProductsQueue")
                            startActivity(intent)
                        }
                    }

                    // If queue is empty
                    if(adapter1.itemCount == 0) {
                        binding.tvNoProductsInQueue.visibility = View.VISIBLE
                    } else {
                        binding.tvNoProductsInQueue.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}