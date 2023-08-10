package com.example.findit.activity

import android.widget.Filter
import com.example.findit.adapter.ProductsAdapter
import com.example.findit.model.Products
import java.util.Locale

class FilterProduct(
    private val adapter: ProductsAdapter,
    private val filterList: ArrayList<Products>
) : Filter() {
    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()
        if(!constraint.isNullOrEmpty()) {
            constraint = constraint.toString().uppercase(Locale.getDefault())
            val filteredProducts = ArrayList<Products>()
            for(i in filterList.indices) {
                if(filterList[i].name!!.uppercase(Locale.getDefault()).contains(constraint)) {
                    filteredProducts.add(filterList[i])
                }
            }
            results.count = filteredProducts.size
            results.values = filteredProducts
        } else {
            results.count = filterList.size
            results.values = filterList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        adapter.updateList(results?.values as ArrayList<Products>)
        adapter.notifyDataSetChanged()
    }
}