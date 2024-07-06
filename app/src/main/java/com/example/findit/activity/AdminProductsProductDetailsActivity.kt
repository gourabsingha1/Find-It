package com.example.findit.activity

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.R
import com.example.findit.adapter.ViewPagerProductDetailsAdapter
import com.example.findit.databinding.ActivityAdminProductsProductDetailsBinding
import com.example.findit.model.Products
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminProductsProductDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProductsProductDetailsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var product: Products

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProductsProductDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init progressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Load data
        val productId = intent.getStringExtra("EXTRA_PRODUCT_ID")!!
        val path = intent.getStringExtra("EXTRA_PATH")!!
        if(path == "Products") {
            binding.tvAdminProductsProductDetailsRemove.visibility = View.VISIBLE
            binding.tvAdminProductsProductDetailsDecline.visibility = View.GONE
            binding.tvAdminProductsProductDetailsApprove.visibility = View.GONE
        } else {
            binding.tvAdminProductsProductDetailsRemove.visibility = View.GONE
            binding.tvAdminProductsProductDetailsDecline.visibility = View.VISIBLE
            binding.tvAdminProductsProductDetailsApprove.visibility = View.VISIBLE
        }
        loadProduct(productId, path)

        binding.tvAdminProductsProductDetailsApprove.setOnClickListener {
            uploadProduct()
        }

        binding.tvAdminProductsProductDetailsDecline.setOnClickListener {
            showDeleteDialog(productId, path)
        }

        binding.tvAdminProductsProductDetailsRemove.setOnClickListener {
            showDeleteDialog(productId, path)
        }
    }

    private fun loadProduct(productId: String, path: String) {
        val reference = FirebaseDatabase.getInstance().getReference(path)
        reference.child(productId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()) {
                    product = snapshot.getValue(Products::class.java)!!
                    binding.tvAdminProductsProductDetailsProductName.text = product.name
                    binding.tvAdminProductsProductDetailsProductPrice.text = "₹${product.price}"
                    binding.tvAdminProductsProductDetailsProductLocation.text = product.address
                    binding.tvAdminProductsProductDetailsProductDescription.text = product.description
                    loadImages(productId, path)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun loadImages(productId: String, path: String) {
        val reference = FirebaseDatabase.getInstance().getReference(path)
        reference.child(productId).child("images")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val imageUrls = arrayListOf<String>()
                    for (ds in snapshot.children) {
                        val imageUrl = "${ds.child("imageUrl").value}"
                        imageUrls.add(imageUrl)
                    }
                    val adapter =
                        ViewPagerProductDetailsAdapter(this@AdminProductsProductDetailsActivity, imageUrls)
                    binding.vpAdminProductsProductDetails.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun uploadProduct() {
        val reference = FirebaseDatabase.getInstance().getReference("Products")
        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = product.name
        hashMap["price"] = product.price
        hashMap["description"] = product.description
        hashMap["images"] = product.images
        hashMap["timestamp"] = product.timestamp
        hashMap["uid"] = product.uid
        hashMap["productId"] = product.productId
        hashMap["searchTags"] = product.searchTags
        hashMap["latitude"] = product.latitude
        hashMap["longitude"] = product.longitude
        hashMap["address"] = product.address

        // Set data to firebase realtime db
        reference.child(product.productId!!).setValue(hashMap).addOnSuccessListener {
            deleteProductsFromQueue(product.productId!!)
        }.addOnFailureListener { e ->
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteDialog(productId: String, path: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_my_products_delete)
        val tvMyProductsDeleteDelete = dialog.findViewById<TextView>(R.id.tvMyProductsDeleteDelete)
        val tvMyProductsDeleteCancel = dialog.findViewById<TextView>(R.id.tvMyProductsDeleteCancel)

        tvMyProductsDeleteDelete.setOnClickListener {
            dialog.dismiss()
            if(path == "Products") {
                removeProduct(productId)
            } else {
                deleteProductsFromQueue(productId)
            }
        }

        tvMyProductsDeleteCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
    }

    // Delete product from queue
    private fun deleteProductsFromQueue(productId: String) {
        try{
            binding.pbAdminProductsProductDetailsQueue.visibility = View.VISIBLE
            FirebaseDatabase.getInstance().getReference("ProductsQueue")
                .child(productId).removeValue().addOnSuccessListener {
                    Intent(this@AdminProductsProductDetailsActivity, AdminProductsQueueActivity::class.java).also {
                        startActivity(it)
                        finishAffinity()
                    }
                    binding.pbAdminProductsProductDetailsQueue.visibility = View.GONE
                }.addOnFailureListener {
                    Toast.makeText(this@AdminProductsProductDetailsActivity, "Couldn't delete", Toast.LENGTH_LONG).show()
                    binding.pbAdminProductsProductDetailsQueue.visibility = View.GONE
                }
        } catch (e: Exception) {
            Log.e("AdminProductsProductDetailsActivityError", e.toString())
        }
    }

    // Remove product
    private fun removeProduct(productId: String) {
        try{
            binding.pbAdminProductsProductDetailsQueue.visibility = View.VISIBLE
            FirebaseDatabase.getInstance().getReference("Products")
                .child(productId).removeValue().addOnSuccessListener {
                    Toast.makeText(this@AdminProductsProductDetailsActivity, "Product deleted successfully", Toast.LENGTH_LONG).show()
                    Intent(this@AdminProductsProductDetailsActivity, AdminProductsActivity::class.java).also {
                        startActivity(it)
                        finishAffinity()
                    }
                    binding.pbAdminProductsProductDetailsQueue.visibility = View.GONE
                }.addOnFailureListener {
                    Toast.makeText(this@AdminProductsProductDetailsActivity, "Couldn't delete", Toast.LENGTH_LONG).show()
                    binding.pbAdminProductsProductDetailsQueue.visibility = View.GONE
                }
        } catch (e: Exception) {
            Log.e("AdminProductsProductDetailsActivityError", e.toString())
        }
    }
}