package com.example.findit.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.bumptech.glide.Glide
import com.example.findit.R
import com.example.findit.databinding.ActivityEditProductBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.lang.Exception

class EditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProductBinding
    private lateinit var progressDialog: ProgressDialog
    private var imageUri: Uri? = null
    private var productId = ""
    private lateinit var address: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private companion object {
        private const val TAG = "EDIT_PRODUCT_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Setup ProgressDialogue to show after updating profile
        progressDialog = ProgressDialog(this)
        progressDialog.setCanceledOnTouchOutside(false)

        // Go back
        binding.ivEditProductBack.setOnClickListener {
            onBackPressed()
        }

        // Get productId from adapter to edit
        productId = intent.getStringExtra("EXTRA_PRODUCT_ID")!!

        // Load Data
        loadMyData()

        // Get location
        binding.acEditProductLocation.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            locationPickerActivityResultLauncher.launch(intent)
        }

        binding.ivEditProductUploadImage.setOnClickListener {
            imagePickDialog()
        }

        // Update product
        binding.btnEditProductUpdate.setOnClickListener {
            uploadProductImageStorage()
        }
    }

    // Get location result
    private val locationPickerActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    latitude = data.getDoubleExtra("latitude", 0.0)
                    longitude = data.getDoubleExtra("longitude", 0.0)
                    address = data.getStringExtra("address") ?: ""
                    binding.acEditProductLocation.setText(address)
                } else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun loadMyData() {
        FirebaseDatabase.getInstance().getReference("Products")
            .child(productId).addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = "${snapshot.child("name").value}"
                    val price = "${snapshot.child("price").value}"
                    val address = "${snapshot.child("address").value}"
                    val description = "${snapshot.child("description").value}"
                    val imageUrl = "${snapshot.child("imageUrl").value}"

                    // set data
                    binding.etEditProductName.setText(name)
                    binding.etEditProductPrice.setText(price)
                    binding.acEditProductLocation.setText(address)
                    binding.etEditProductDescription.setText(description)
                    try {
                        Glide.with(this@EditProductActivity)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(binding.ivEditProductUploadImage)
                    } catch (e : Exception) {
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, error.message)
                }

        })
    }


    // **** Getting and setting image stuffs ****
    private fun imagePickDialog() {
        // Show popupmenu when clicked on imageView
        val popupMenu = PopupMenu(this, binding.ivEditProductUploadImage)
        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if(itemId == 1) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestCameraPermissions.launch(arrayOf(android.Manifest.permission.CAMERA))
                } else {
                    requestCameraPermissions.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            } else if(itemId == 2) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickImageGallery()
                } else {
                    requestStoragePermissions.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            true
        }
    }

    private val requestCameraPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            var areAllGranted = true
            for(isGranted in result.values) {
                areAllGranted = areAllGranted && isGranted
            }
            if(areAllGranted) {
                pickImageCamera()
            } else {
                Toast.makeText(this, "Camera or Storage or both permissions denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun pickImageCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_image_title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_image_description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultlauncher.launch(intent)
    }

    private val cameraActivityResultlauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == Activity.RESULT_OK) {
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.ivEditProductUploadImage)
                } catch (e : Exception) {
                    Log.e(TAG, "cameraActivityResultLauncher: ", e)
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            }
        }

    private val requestStoragePermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if(isGranted) {
                pickImageGallery()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageUri = data!!.data
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.ivEditProductUploadImage)
                } catch (e : Exception) {
                    Log.e(TAG, "cameraActivityResultLauncher: ", e)
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            }
        }

    private fun uploadProductImageStorage() {
        progressDialog.setMessage("Uploading")
        progressDialog.show()

        FirebaseStorage.getInstance().getReference("Images")
            .child(productId).putFile(imageUri!!).addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val imageUrl = uriTask.result.toString()
                if(uriTask.isSuccessful) {
                    // Set imageUrl to firebase realtime
                    updateProductDb(imageUrl)
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "uploadProfileImageStorage: ", e)
                progressDialog.dismiss()
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun updateProductDb(imageUrl: String?) {
        progressDialog.setMessage("Updating user info")
        progressDialog.show()

        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = binding.etEditProductName.text.toString().trim()
        hashMap["price"] = binding.etEditProductPrice.text.toString().trim().toDouble()
        hashMap["description"] = binding.etEditProductDescription.text.toString().trim()
        hashMap["imageUrl"] = imageUrl
        hashMap["latitude"] = latitude
        hashMap["longitude"] = longitude
        hashMap["address"] = address

        FirebaseDatabase.getInstance().getReference("Products")
            .child(productId).updateChildren(hashMap).addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_LONG).show()
                imageUri = null
            }.addOnFailureListener { e ->
                Log.e(TAG, "updateUserInfoDb: ", e)
                progressDialog.dismiss()
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            }
    }
}