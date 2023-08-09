package com.example.findit.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import android.util.Log
import android.util.Patterns
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.bumptech.glide.Glide
import com.example.findit.R
import com.example.findit.databinding.ActivityEditProfileBinding
import com.example.findit.fragments.AccountFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.lang.Exception

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var myUserType = ""
    private var imageUri: Uri? = null

    private companion object {
        private const val TAG = "EDIT_PROFILE_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Setup ProgressDialogue to show after updating profile
        progressDialog = ProgressDialog(this)
        progressDialog.setCanceledOnTouchOutside(false)

        // Go back
        binding.ivEditProfileBack.setOnClickListener {
            onBackPressed()
        }

        // Load Data
        loadMyData()

        binding.ivEditProfileProfilePic.setOnClickListener {
            imagePickDialog()
        }

        // Update profile
        binding.btnEditProfileUpdate.setOnClickListener {
            validateData()
        }
    }

    private fun loadMyData() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val name = "${snapshot.child("name").value}"
                val email = "${snapshot.child("email").value}"
                var timestamp = "${snapshot.child("timestamp").value}"
                val phoneCode = "${snapshot.child("phoneCode").value}"
                val phoneNumber = "${snapshot.child("phoneNumber").value}"
                val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                myUserType = "${snapshot.child("userType").value}"
                val onlineStatus = "${snapshot.child("onlineStatus").value}"
                val phone = phoneCode + phoneNumber

                // Check user type. If email/google, then dont allow to update email
                if (myUserType.equals("Phone", true)) {
                    binding.ccpEditProfile.isEnabled = false
                    binding.tfEditProfilePhone.isEnabled = false
                    binding.etEditProfilePhone.isEnabled = false
                } else if (myUserType.equals("Email", true) || myUserType.equals("Google", true)) {
                    binding.tfEditProfileEmail.isEnabled = false
                    binding.etEditProfileEmail.isEnabled = false
                }

                // set data
                binding.etEditProfileName.setText(name)
                binding.etEditProfileEmail.setText(email)
                binding.etEditProfilePhone.setText(phoneNumber)
                try {
                    val phoneCodeInt = phoneCode.replace("+", "").toInt()
                    binding.ccpEditProfile.setCountryForPhoneCode(phoneCodeInt)
                } catch (e : Exception) {
                    Log.e(TAG, "onDataChange: ", e)
                }
                try {
                    Glide.with(this@EditProfileActivity)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.ivEditProfileProfilePic)
                } catch (e : Exception) {
                    Log.e(TAG, "onDataChange: ", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun imagePickDialog() {
        // Show popupmenu when clicked on imageView
        val popupMenu = PopupMenu(this, binding.ivEditProfileProfilePic)
        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if(itemId == 1) {
                Log.d(TAG, "imagePickDialog: Camera clicked. Check if camera permissions are granted")
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestCameraPermissions.launch(arrayOf(android.Manifest.permission.CAMERA))
                } else {
                    requestCameraPermissions.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            } else if(itemId == 2) {
                Log.d(TAG, "imagePickDialog: Gallery clicked. Check if storage permissions are granted")
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
            Log.d(TAG, "requestCameraPermissions: result: $result")
            var areAllGranted = true
            for(isGranted in result.values) {
                areAllGranted = areAllGranted && isGranted
            }
            if(areAllGranted) {
                Log.d(TAG, "requestCameraPermissions: All granted e.g. Camera, Storage")
                pickImageCamera()
            } else {
                Log.d(TAG, "requestCameraPermissions: All or either one is denied")
                Toast.makeText(this, "Camera or Storage or both permissions denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ")
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
                Log.d(TAG, "cameraActivityResultlauncher: Image captured: imageUri: $imageUri")
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.ivEditProfileProfilePic)
                } catch (e : Exception) {
                    Log.e(TAG, "cameraActivityResultlauncher: ", e)
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            }
        }


    private val requestStoragePermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "requestStoragePermissions: isGranted $isGranted")
            if(isGranted) {
                pickImageGallery()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultlauncher.launch(intent)
    }

    private val galleryActivityResultlauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "cameraActivityResultlauncher: Image captured: imageUri: $imageUri")
                val data = result.data
                imageUri = data!!.data
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.ivEditProfileProfilePic)
                } catch (e : Exception) {
                    Log.e(TAG, "cameraActivityResultlauncher: ", e)
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            }
        }

    // variables to hold updated product data
    var name = ""
    var email = ""
    var phoneCode = ""
    var phoneNumber = ""

    // Validate email and password
    private fun validateData() {
        name = binding.etEditProfileName.text.toString().trim()
        email = binding.etEditProfileEmail.text.toString().trim()
        phoneCode = binding.ccpEditProfile.selectedCountryCodeWithPlus
        phoneNumber = binding.etEditProfilePhone.text.toString().trim()

        if(imageUri == null) {
            updateProfileDb(null)
        } else {
            uploadProfileImageStorage()
        }
    }

    private fun uploadProfileImageStorage() {
        Log.d(TAG, "uploadProfileImageStorage: ")
        progressDialog.setMessage("Uploading")
        progressDialog.show()

        val filePathAndName = "UserProfile/profile_${firebaseAuth.uid}"
        val ref = FirebaseStorage.getInstance().getReference(filePathAndName)
        ref.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "uploadProfileImageStorage: Image uploaded")
                // set imageUrl to firebase realtime
                val uriTask = taskSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val uploadedImageUrl = uriTask.result.toString()
                if(uriTask.isSuccessful) {
                    updateProfileDb(uploadedImageUrl)
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "uploadProfileImageStorage: ", e)
                progressDialog.dismiss()
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun updateProfileDb(uploadedImageUrl: String?) {
        Log.d(TAG, "updateProfileDb: uploadedImageUrl: $uploadedImageUrl")
        progressDialog.setMessage("Updating user info")
        progressDialog.show()

        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = "$name"
        if (uploadedImageUrl != null) {
            hashMap["profileImageUrl"] = "$uploadedImageUrl"
        }
        if (myUserType.equals("Phone", true)) {
            hashMap["email"] = "$email"
        } else if (myUserType.equals("Email", true) || myUserType.equals("Google", true)) {
            hashMap["phoneCode"] = "$phoneCode"
            hashMap["phoneNumber"] = "$phoneNumber"
        }

        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child("${firebaseAuth.uid}").updateChildren(hashMap).addOnSuccessListener {
            progressDialog.dismiss()
            Log.d(TAG, "updateUserInfoDb: Profile Updated")
            Toast.makeText(this, "Profile Updated", Toast.LENGTH_LONG).show()
            imageUri = null
        }.addOnFailureListener { e ->
            progressDialog.dismiss()
            Log.e(TAG, "updateUserInfoDb: ", e)
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }
    }
}