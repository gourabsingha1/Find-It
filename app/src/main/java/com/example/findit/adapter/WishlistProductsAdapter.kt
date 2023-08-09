package com.example.findit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.findit.R
import com.example.findit.model.Products
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WishlistProductsAdapter(
    private val productsFirebase: ArrayList<Products>
): RecyclerView.Adapter<WishlistProductsAdapter.WishlistProductsViewHolder>() {

    inner class WishlistProductsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvWishlistName = itemView.findViewById<TextView>(R.id.tvWishlistName)
        val tvWishlistPrice = itemView.findViewById<TextView>(R.id.tvWishlistPrice)
        val tvWishlistLocation = itemView.findViewById<TextView>(R.id.tvWishlistLocation)
        val ivWishlistImage = itemView.findViewById<ImageView>(R.id.ivWishlistImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistProductsViewHolder {
        val viewHolder = WishlistProductsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_wishlist_product, parent, false)
        )
        return viewHolder
    }

    override fun onBindViewHolder(holder: WishlistProductsViewHolder, position: Int) {
        val currentProduct = productsFirebase[position]
        holder.tvWishlistName.text = currentProduct.name
        holder.tvWishlistPrice.text = currentProduct.price.toString()
        holder.tvWishlistLocation.text = currentProduct.location

        // Load image from firebase storage
        loadProductImage(holder, currentProduct.productId!!)

        // Open product details when clicked on product
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(currentProduct)
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

    // Open product details on click
    var onItemClick : ((Products) -> Unit)? = null

    private fun loadProductImage(holder: WishlistProductsViewHolder, productId: String) {
        val reference = FirebaseDatabase.getInstance().getReference("Products")
        reference.child(productId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = "${snapshot.child("imageUrl").value}"
                Glide.with(holder.itemView).load(imageUrl).into(holder.ivWishlistImage)
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }
}