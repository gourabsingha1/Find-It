package com.example.findit.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.findit.R
import com.example.findit.activity.MainActivity
import com.example.findit.activity.SignUpEmailActivity
import com.example.findit.databinding.FragmentAddBinding
import com.example.findit.model.Products
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// findNavController glitching

class AddFragment : Fragment() {

    private lateinit var binding: FragmentAddBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth
    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddBinding.inflate(layoutInflater)

        // Setup ProgressDialogue to show while signup
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setCanceledOnTouchOutside(false)

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Open gallery
        binding.ivUploadImage.setOnClickListener {
            resultLauncher.launch("image/*")
        }

        // Submit Product
        binding.btnSubmitProduct.setOnClickListener {
            val etAddName = binding.etAddName.text.toString().trim()
            val etAddPrice = binding.etAddPrice.text.toString().trim().toDouble()
            val etAddLocation = binding.etAddLocation.text.toString().trim()
            val etAddDescription = binding.etAddDescription.text.toString().trim()
            val ivUploadImage = System.currentTimeMillis().toString()
            validateData(etAddName, etAddPrice, etAddLocation, etAddDescription, ivUploadImage)
        }

        return binding.root
    }

    // Set image in app
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
        binding.ivUploadImage.setImageURI(it)
    }

    // Validate data
    private fun validateData(etAddName: String, etAddPrice: Double, etAddLocation: String, etAddDescription: String, ivUploadImage: String) {
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
            uploadProduct(etAddName, etAddPrice, etAddLocation, etAddDescription, ivUploadImage)
        }
    }

    private fun uploadProduct(etAddName: String, etAddPrice: Double, etAddLocation: String, etAddDescription: String, ivUploadImage: String) {
        progressDialog.setMessage("Uploading product")

        val reference = FirebaseDatabase.getInstance().getReference("Products")
        val keyId = reference.push().key
        val timestamp = System.currentTimeMillis()

        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = etAddName
        hashMap["price"] = etAddPrice
        hashMap["location"] = etAddLocation
        hashMap["description"] = etAddDescription
        hashMap["timestamp"] = timestamp
        hashMap["uid"] = firebaseAuth.uid
        hashMap["productId"] = keyId
        hashMap["imageUrl"] = ""
//        hashMap["latitude"] = latitude
//        hashMap["longitude"] = longitude

        // set data to firebase realtime db
        reference.child(keyId!!).setValue(hashMap).addOnSuccessListener {
            progressDialog.dismiss()
            // upload product images to keyId
            uploadImagesToStorage(keyId)
        }.addOnFailureListener { e ->
            progressDialog.dismiss()
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
                        val reference = FirebaseDatabase.getInstance().getReference("Products")
                        reference.child(productId).updateChildren(hashMap)
                    }

                    // erase previous data
                    binding.etAddName.text.clear()
                    binding.etAddPrice.text.clear()
                    binding.etAddLocation.text.clear()
                    binding.etAddDescription.text.clear()
                    imageUri = null
                    val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_image_24)
                    binding.ivUploadImage.setImageDrawable(drawable)
                    Toast.makeText(requireContext(), "Product added", Toast.LENGTH_LONG).show()
//                    findNavController().navigate(R.id.action_addFragment_to_homeFragment)
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch(e: Exception) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
        }
    }
}