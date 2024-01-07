package com.example.findit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.findit.R
import com.example.findit.activity.FilterProduct
import com.example.findit.model.Products
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProductsAdapter(var productsFirebase: ArrayList<Products>
): RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>(), Filterable {

    private var filter: FilterProduct? = null
    private var filterList = productsFirebase

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentProduct = productsFirebase[position]
        holder.tvName.text = currentProduct.name
        holder.tvPrice.text = currentProduct.price.toString()
        holder.tvLocation.text = currentProduct.location

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

    // Open product details on click
    var onItemClick : ((Products) -> Unit)? = null

    private fun loadProductImage(holder: ProductViewHolder, productId: String) {
        FirebaseDatabase.getInstance().getReference("Products")
            .child(productId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = "${snapshot.child("imageUrl").value}"
                Glide.with(holder.itemView).load(imageUrl).into(holder.ivImage)
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    override fun getFilter(): Filter {
        if(filter == null) {
            filter = FilterProduct(this, filterList)
        }
        return filter as FilterProduct
    }
}


