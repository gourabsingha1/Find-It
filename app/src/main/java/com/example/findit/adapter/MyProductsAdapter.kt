package com.example.findit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.findit.R
import com.example.findit.model.Products
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class MyProductsAdapter(
    private val productsFirebase: ArrayList<Products>
): RecyclerView.Adapter<MyProductsAdapter.MyProductsViewHolder>() {

    private val productCollectionRef = Firebase.firestore.collection("products")
    private var storageRef = FirebaseStorage.getInstance().reference.child("Images")

    inner class MyProductsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName = itemView.findViewById<TextView>(R.id.tvName)
        val tvPrice = itemView.findViewById<TextView>(R.id.tvPrice)
        val tvLocation = itemView.findViewById<TextView>(R.id.tvLocation)
        val ivImage = itemView.findViewById<ImageView>(R.id.ivImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyProductsViewHolder {
        val viewHolder = MyProductsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        )
        return viewHolder
    }

    override fun onBindViewHolder(holder: MyProductsViewHolder, position: Int) {
        val currentProduct = productsFirebase[position]
        holder.tvName.text = currentProduct.name
        holder.tvPrice.text = currentProduct.price.toString()
        holder.tvLocation.text = currentProduct.location

        // Load image from firebase storage

        // Open product details when clicked on product
        holder.itemView.setOnClickListener {
//            onItemClick?.invoke(currentProduct)
        }
    }

    override fun getItemCount(): Int {
        return productsFirebase.size
    }

    fun updateList(newList: List<Products>) {
        productsFirebase.clear()
        productsFirebase.addAll(newList)
        notifyDataSetChanged()
    }

    private fun loadAdImage(productId: String) {
        val reference = FirebaseDatabase.getInstance()
    }
}