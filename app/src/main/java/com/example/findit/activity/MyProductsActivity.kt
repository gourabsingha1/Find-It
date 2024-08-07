package com.example.findit.activity

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.R
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

        loadMyProducts()
    }

    private fun loadMyProducts() {
        val firebaseAuth = FirebaseAuth.getInstance()
        FirebaseDatabase.getInstance().getReference("Products").orderByChild("uid").equalTo(firebaseAuth.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // does not let products reappear after deleting
                    productList.clear()
                    for(ds in snapshot.children) {
                        try {
                            val product = ds.getValue(Products::class.java)
                            productList.add(product!!)
                        } catch (e : Exception) {
                            Log.e("MyProductsActivityError", e.toString())
                        }
                    }
                    adapter = ProductsAdapter(productList, "Products")
                    binding.rvMyProducts.adapter = adapter
                    binding.pbMyProducts.visibility = View.INVISIBLE

                    // Set total items
                    binding.toolbarMyProducts.subtitle = "${productList.size} items"

                    // Open bottom sheet dialog when clicked on product
                    adapter.onItemClick = { product ->
                        showDialog(product)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MyProductsActivityError", error.message)
                    binding.pbMyProducts.visibility = View.INVISIBLE
                }

            })
    }

    private fun showDialog(product: Products) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottomsheetlayout_my_products)
        val tvMyProductsEdit = dialog.findViewById<TextView>(R.id.tvMyProductsEdit)
        val tvMyProductsDelete = dialog.findViewById<TextView>(R.id.tvMyProductsDelete)
        val tvMyProductsSeeProduct = dialog.findViewById<TextView>(R.id.tvMyProductsSeeProduct)

        tvMyProductsEdit.setOnClickListener {
            dialog.dismiss()
            Intent(this, EditProductActivity::class.java).also { intent ->
                intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                startActivity(intent)
            }
        }

        tvMyProductsDelete.setOnClickListener {
            dialog.dismiss()
            showDeleteDialog(product.productId!!)
        }

        tvMyProductsSeeProduct.setOnClickListener {
            dialog.dismiss()
            Intent(this, ProductDetailsActivity::class.java).also { intent ->
                intent.putExtra("EXTRA_PRODUCT_ID", product.productId)
                startActivity(intent)
            }
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun showDeleteDialog(productId: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_my_products_delete)
        val tvMyProductsDeleteDelete = dialog.findViewById<TextView>(R.id.tvMyProductsDeleteDelete)
        val tvMyProductsDeleteCancel = dialog.findViewById<TextView>(R.id.tvMyProductsDeleteCancel)

        tvMyProductsDeleteDelete.setOnClickListener {
            dialog.dismiss()
            try{
                FirebaseDatabase.getInstance().getReference("Products")
                    .child(productId).removeValue().addOnSuccessListener {
                        Toast.makeText(this@MyProductsActivity, "Product deleted successfully", Toast.LENGTH_LONG).show()
                    }.addOnFailureListener {
                        Toast.makeText(this@MyProductsActivity, "Couldn't delete", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Log.e("MyProductsActivityError", e.toString())
            }
        }

        tvMyProductsDeleteCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
    }
}