package com.example.findit.fragments

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.findit.R
import com.example.findit.databinding.FragmentAddBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// findNavController glitching

class AddFragment : Fragment() {

    private lateinit var binding: FragmentAddBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var imageUri: Uri? = null
    private lateinit var etAddName: String
    private var etAddPrice = 0.0
    private lateinit var etAddLocation: String
    private lateinit var etAddDescription: String
    private lateinit var ivUploadImage: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddBinding.inflate(layoutInflater)

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Open gallery
        binding.ivUploadImage.setOnClickListener {
            resultLauncher.launch("image/*")
        }

        // Submit Product
        binding.btnSubmitProduct.setOnClickListener {
            etAddName = binding.etAddName.text.toString().trim()
            etAddPrice = binding.etAddPrice.text.toString().trim().toDouble()
            etAddLocation = binding.etAddLocation.text.toString().trim()
            etAddDescription = binding.etAddDescription.text.toString().trim()
            ivUploadImage = System.currentTimeMillis().toString()
            validateData()
        }

        return binding.root
    }

    // Set image in app
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
        binding.ivUploadImage.setImageURI(it)
    }

    // Validate data
    private fun validateData() {
        if(etAddName.isEmpty()) {
            binding.etAddName.error = "Enter product name"
            binding.etAddName.requestFocus()
        } else if(etAddLocation.isEmpty()) {
            binding.etAddLocation.error = "Enter product location"
            binding.etAddLocation.requestFocus()
        } else if(etAddDescription.isEmpty()) {
            binding.etAddDescription.error = "Enter product description"
            binding.etAddDescription.requestFocus()
        } else {
            // If AI is able to generate search tags out of given image, then only add the product
            getProductSearchTags()
        }
    }

    // Get product search tags
    private fun getProductSearchTags() {
        binding.pbAdd.visibility = View.VISIBLE
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-pro-vision",
                apiKey = "AIzaSyBwS-5ig3w_zxg14n5c7PgP85Ak4wwCvSo"
            )
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
            val size: Double = bitmap.allocationByteCount.toDouble() / (1024 * 1024)
            if(size < 40) {
                CoroutineScope(Dispatchers.IO).launch {
                    val inputContent = content {
                        image(bitmap)
                        text("Print JUST the name of the thing in first line. Then print few similar names in next lines.")
                    }
                    val response = generativeModel.generateContent(inputContent)
                    withContext(Dispatchers.Main) {
                        // Add AI generated names in the array, to search using these names
                        uploadProduct(sentenceToWords(response.text!!))
                    }
                }
            } else {
                binding.pbAdd.visibility = View.INVISIBLE
                Toast.makeText(requireContext(), "Image size too large", Toast.LENGTH_SHORT).show()
            }
        } catch (e : Exception) {
            binding.pbAdd.visibility = View.INVISIBLE
            Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sentenceToWords(text: String) : ArrayList<String> {
        val names = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            if (text[i].isLetter()) {
                val j = i
                while (i < text.length && (text[i].isLetter() || text[i].isDigit() || text[i] == ' ')) {
                    i++
                }
                val name = text.substring(j, i).trim().toLowerCase(Locale.ROOT)
                if(name.length < 20) {
                    names.add(name)
                }
            }
            i++
        }
        return names
    }

    private fun uploadProduct(searchTags : ArrayList<String>) {
        val reference = FirebaseDatabase.getInstance().getReference("Products")
        val productId = reference.push().key!!
        val timestamp = System.currentTimeMillis()
        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = etAddName
        hashMap["price"] = etAddPrice
        hashMap["location"] = etAddLocation
        hashMap["description"] = etAddDescription
        hashMap["timestamp"] = timestamp
        hashMap["uid"] = firebaseAuth.uid
        hashMap["productId"] = productId
        hashMap["imageUrl"] = ""
        hashMap["searchTags"] = searchTags
        //        hashMap["latitude"] = latitude
        //        hashMap["longitude"] = longitude

        // Set data to firebase realtime db
        reference.child(productId).setValue(hashMap).addOnSuccessListener {
            uploadImagesToStorage(productId)
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    // Upload product image to firebase storage
    private fun uploadImagesToStorage(productId: String) {
        var storageRef = FirebaseStorage.getInstance().reference.child("Images")
        try {
            storageRef = storageRef.child(productId)
            imageUri?.let {
                storageRef.putFile(it).addOnSuccessListener { taskSnapshot ->
                    // set imageUrl to firebase realtime
                    val uriTask = taskSnapshot.storage.downloadUrl
                    while(!uriTask.isSuccessful);
                    val uploadedImageUrl = uriTask.result.toString()
                    if(uriTask.isSuccessful) {
                        val hashMap = HashMap<String, Any?>()
                        hashMap["imageUrl"] = uploadedImageUrl
                        FirebaseDatabase.getInstance().getReference("Products")
                            .child(productId).updateChildren(hashMap)
                    }

                    // Erase previous data
                    binding.etAddName.text.clear()
                    binding.etAddPrice.text.clear()
                    binding.etAddLocation.text.clear()
                    binding.etAddDescription.text.clear()
                    imageUri = null
                    binding.pbAdd.visibility = View.INVISIBLE
                    val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_image_24)
                    binding.ivUploadImage.setImageDrawable(drawable)

                    Toast.makeText(requireContext(), "Product added", Toast.LENGTH_LONG).show()
//                    findNavController().navigate(R.id.action_addFragment_to_homeFragment)
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch(e: Exception) {
            binding.pbAdd.visibility = View.INVISIBLE
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
        }

    }
}