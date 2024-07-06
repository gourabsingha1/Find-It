package com.example.findit.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.R
import com.example.findit.adapter.ViewPagerProductDetailsAdapter
import com.example.findit.databinding.ActivityProductDetailsBinding
import com.example.findit.model.Products
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailsBinding
    private var alreadyInWishlist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Load data
        val productId = intent.getStringExtra("EXTRA_PRODUCT_ID")!!
        loadProduct(productId)

        // Check if already in wishlist
        checkIfInWishlist(productId)

        // Add to wishlist
        binding.ivProductDetailsWishlist.setOnClickListener {
            if (alreadyInWishlist) {
                removeFromWishlist(productId)
            } else {
                addToWishlist(productId)
            }
        }
    }

    private fun loadProduct(productId: String) {
        val reference = FirebaseDatabase.getInstance().getReference("Products")
        reference.child(productId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val product = snapshot.getValue(Products::class.java)!!
                binding.tvProductName.text = product.name
                binding.tvProductPrice.text = "₹${product.price}"
                binding.tvProductLocation.text = product.address
                binding.tvProductDescription.text = product.description
                loadImages(productId)
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun loadImages(productId: String) {
        val reference = FirebaseDatabase.getInstance().getReference("Products")
        reference.child(productId).child("images")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val imageUrls = arrayListOf<String>()
                    for (ds in snapshot.children) {
                        val imageUrl = "${ds.child("imageUrl").value}"
                        imageUrls.add(imageUrl)
                    }
                    val adapter =
                        ViewPagerProductDetailsAdapter(this@ProductDetailsActivity, imageUrls)
                    binding.vpProductDetails.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun checkIfInWishlist(productId: String) {
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null) {
            reference.child(firebaseAuth.uid!!).child("Wishlist")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            if (snapshot.child(productId).exists()) {
                                alreadyInWishlist = true
                                binding.ivProductDetailsWishlist.setImageResource(R.drawable.baseline_favorite_24_red)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
        }
    }

    private fun addToWishlist(productId: String) {
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null) {
            Toast.makeText(this, "You're not logged in", Toast.LENGTH_LONG).show()
        } else {
            val hashMap = HashMap<String, Any>()
            hashMap["productId"] = productId
            reference.child(firebaseAuth.uid!!).child("Wishlist").child(productId).setValue(hashMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Added to wishlist", Toast.LENGTH_SHORT).show()
                    alreadyInWishlist = true
                    binding.ivProductDetailsWishlist.setImageResource(R.drawable.baseline_favorite_24_red)
                }.addOnFailureListener { e ->
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun removeFromWishlist(productId: String) {
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null) {
            Toast.makeText(this, "You're not logged in", Toast.LENGTH_LONG).show()
        } else {
            reference.child(firebaseAuth.uid!!).child("Wishlist").child(productId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                    alreadyInWishlist = false
                    binding.ivProductDetailsWishlist.setImageResource(R.drawable.baseline_favorite_border_24)
                }.addOnFailureListener { e ->
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
        }
    }
}