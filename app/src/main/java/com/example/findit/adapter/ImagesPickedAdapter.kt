package com.example.findit.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.findit.R
import com.example.findit.databinding.RowImagesPickedBinding
import com.example.findit.model.ImagesPicked

class ImagesPickedAdapter(
    private val context: Context,
    private val imagesPickedArrayList: ArrayList<ImagesPicked>
) : Adapter<ImagesPickedAdapter.HolderImagePicked>() {

    private lateinit var binding: RowImagesPickedBinding

    inner class HolderImagePicked(itemView: View) : ViewHolder(itemView) {
        val ivUploadImage = binding.ivUploadImageRowImagePicked
        val btnClose = binding.btnCloseRowImagePicked

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImagePicked {
        binding = RowImagesPickedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HolderImagePicked(binding.root)
    }

    override fun onBindViewHolder(holder: HolderImagePicked, position: Int) {
        val currentImage = imagesPickedArrayList[position]
        val imageUri = currentImage.imageUri
        try {
            Glide.with(context)
                .load(imageUri)
                .placeholder(R.drawable.baseline_image_24)
                .into(holder.ivUploadImage)
        } catch (e: Exception) {
            Log.e("", e.toString())
        }
        holder.btnClose.setOnClickListener {
            imagesPickedArrayList.remove(currentImage)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return imagesPickedArrayList.size
    }
}