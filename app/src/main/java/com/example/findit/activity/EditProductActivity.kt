package com.example.findit.activity

import android.Manifest
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
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.adapter.ImagesPickedAdapter
import com.example.findit.databinding.ActivityEditProductBinding
import com.example.findit.model.ImagesPicked
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class EditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProductBinding
    private lateinit var progressDialog: ProgressDialog
    private var imageUri: Uri? = null
    private var productId = ""
    private lateinit var address: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var imagePickedAdapter: ImagesPickedAdapter
    private var imagePickedArraylist: ArrayList<ImagesPicked> = ArrayList()

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
        binding.ivBackEditProduct.setOnClickListener {
            onBackPressed()
        }

        // Get productId from adapter to edit
        productId = intent.getStringExtra("EXTRA_PRODUCT_ID")!!

        // Load Data
        loadMyData()

        // Get location
        binding.acUpdateLocation.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            locationPickerActivityResultLauncher.launch(intent)
        }

        binding.ivAddImagesEditProduct.setOnClickListener {
            showImagePickOptions()
        }

        // Update product
        binding.btnUpdateEditProduct.setOnClickListener {
            updateProductDb()
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
                    binding.acUpdateLocation.setText(address)
                } else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun loadMyData() {
        val ref = FirebaseDatabase.getInstance().getReference("Products").child(productId)
        ref.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = "${snapshot.child("name").value}"
                val price = "${snapshot.child("price").value}"
                val description = "${snapshot.child("description").value}"
                address = "${snapshot.child("address").value}"

                // set data
                binding.etUpdateName.setText(name)
                binding.etUpdatePrice.setText(price)
                binding.acUpdateLocation.setText(address)
                binding.etUpdateDescription.setText(description)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
            }

        })

        // Load images
        ref.child("images").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                imagePickedArraylist.clear()
                for(ds in snapshot.children) {
                    try {
                        val imagePicked = ds.getValue(ImagesPicked::class.java)
                        imagePickedArraylist.add(imagePicked!!)
                    } catch (e : Exception) {
                        Log.e(TAG, e.toString())
                    }
                }
                imagePickedAdapter = ImagesPickedAdapter(this@EditProductActivity, imagePickedArraylist)
                binding.rvImagesEditProduct.adapter = imagePickedAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
            }

        })
    }


    private fun showImagePickOptions() {
        val popupMenu = PopupMenu(this, binding.ivAddImagesEditProduct)
        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if (itemId == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val cameraPermissions = arrayOf(Manifest.permission.CAMERA)
                    requestCameraPermission.launch(cameraPermissions)
                } else {
                    val cameraPermissions = arrayOf(
                        Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    requestCameraPermission.launch(cameraPermissions)
                }
            } else if (itemId == 2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickImageGallery()
                } else {
                    val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                    requestStoragePermission.launch(storagePermission)
                }
            }
            true
        }
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageGallery()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            imageUri = data!!.data
            val timestamp = "${System.currentTimeMillis()}"
            val imagePicked = ImagesPicked(timestamp, imageUri, null, false)
            imagePickedArraylist.add(imagePicked)
            loadImages()
        } else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Load images picked
    private fun loadImages() {
        imagePickedAdapter = ImagesPickedAdapter(this, imagePickedArraylist)
        binding.rvImagesEditProduct.adapter = imagePickedAdapter
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        var areAllGranted = true
        for (isGranted in result.values) {
            areAllGranted = areAllGranted && isGranted
        }
        if (areAllGranted) {
            pickImageCamera()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_IMAGE_TITLE")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMP_IMAGE_DESCRIPTION")
        imageUri = contentResolver?.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val timestamp = "${System.currentTimeMillis()}"
            val imagePicked = ImagesPicked(timestamp, imageUri, null, false)
            imagePickedArraylist.add(imagePicked)
            loadImages()
        } else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProductDb() {
        progressDialog.setMessage("Updating user info")
        progressDialog.show()

        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = binding.etUpdateName.text.toString().trim()
        hashMap["price"] = binding.etUpdatePrice.text.toString().trim().toDouble()
        hashMap["description"] = binding.etUpdateDescription.text.toString().trim()
        hashMap["latitude"] = latitude
        hashMap["longitude"] = longitude
        hashMap["address"] = address

        FirebaseDatabase.getInstance().getReference("Products")
            .child(productId).updateChildren(hashMap).addOnSuccessListener {
                progressDialog.dismiss()
                uploadImagesToStorage(productId)
            }.addOnFailureListener { e ->
                Log.e(TAG, "updateUserInfoDb: ", e)
                progressDialog.dismiss()
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            }
    }

    // Upload product image to firebase storage
    private fun uploadImagesToStorage(productId: String) {
        for (i in imagePickedArraylist.indices) {
            val imagePicked = imagePickedArraylist[i]
            val imageName = imagePicked.id
            val filePathAndName = "Products/$imageName"
            val imageIndexForProgress = i + 1
            val storageRef = FirebaseStorage.getInstance().getReference(filePathAndName)
            storageRef.putFile(imagePicked.imageUri!!).addOnProgressListener { snapshot ->
                val progress = 100.0 * snapshot.bytesTransferred / snapshot.totalByteCount
                val message =
                    "Uploading $imageIndexForProgress of ${imagePickedArraylist.size} images. Progress ${progress.toInt()}%"
                progressDialog.setMessage(message)
                progressDialog.show()
            }.addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedImageUrl = uriTask.result
                if (uriTask.isSuccessful) {
                    val hashMap = HashMap<String, Any>()
                    hashMap["id"] = "${imagePicked.id}"
                    hashMap["imageUrl"] = "$uploadedImageUrl"
                    val ref = FirebaseDatabase.getInstance().getReference("Products")
                    ref.child(productId).child("images").child(imageName)
                        .updateChildren(hashMap)
                }
                progressDialog.dismiss()
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                Log.e("", it.toString())
                progressDialog.dismiss()
            }
        }
    }
}