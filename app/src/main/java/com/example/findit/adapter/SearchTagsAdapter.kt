package com.example.findit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.findit.R

class SearchTagsAdapter(
    private var names: ArrayList<String>
): RecyclerView.Adapter<SearchTagsAdapter.SearchTagsViewHolder>() {

    inner class SearchTagsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSearchTagsName: TextView = itemView.findViewById(R.id.tvSearchTagsName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchTagsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_tags, parent, false)
        return SearchTagsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchTagsViewHolder, position: Int) {
        holder.tvSearchTagsName.text = names[position]
    }

    override fun getItemCount(): Int {
        return names.size
    }
}


