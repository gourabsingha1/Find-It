package com.example.findit.activity

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.example.findit.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth

    private companion object {
        private const val TAG = "SIGNUP_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Setup ProgressDialogue to show while signup
        progressDialog = ProgressDialog(this)
        progressDialog.setCanceledOnTouchOutside(false)

        // Go back
        binding.ivSignupBack.setOnClickListener {
            onBackPressed()
        }

        // Sign up
        binding.btnSignup.setOnClickListener {
            validateData()
        }
    }


    // Validate email and password
    private fun validateData() {
        val email = binding.etSignupEmail.text.toString().trim()
        val password = binding.etSignupPassword.text.toString().trim()
        val confirmPassword = binding.etSignupConfirmPassword.text.toString().trim()

        Log.d(TAG, "validateData: email: $email")
        Log.d(TAG, "validateData: password: $password")
        Log.d(TAG, "validateData: confirm password: $confirmPassword")

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etSignupEmail.error = "Invalid Email Format"
            binding.etSignupEmail.requestFocus()
        } else if(password.isEmpty()) {
            binding.etSignupPassword.error = "Enter Password"
            binding.etSignupPassword.requestFocus()
        } else if(confirmPassword.isEmpty()) {
            binding.etSignupConfirmPassword.error = "Enter Confirm Password"
            binding.etSignupConfirmPassword.requestFocus()
        } else if(password != confirmPassword) {
            binding.etSignupConfirmPassword.error = "Password Doesn't Match"
            binding.etSignupConfirmPassword.requestFocus()
        } else {
            signupFirebase()
        }
    }


    // Sign Up Firebase
    private fun signupFirebase() {
        Log.d(TAG, "signupUser: ")
        progressDialog.setMessage("Creating account")
        progressDialog.show()

        val email = binding.etSignupEmail.text.toString()
        val password = binding.etSignupPassword.text.toString()

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
            if(it.isSuccessful) {
                updateUserInfo()
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
        }
    }

    private fun updateUserInfo() {
        Log.d(TAG, "updateUserInfo: ")
        progressDialog.setMessage("Saving User Info")

        val registeredUserEmail = firebaseAuth.currentUser!!.email
        val registeredUserId = firebaseAuth.uid
        val timestamp = System.currentTimeMillis()

        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = ""
        hashMap["email"] = registeredUserEmail
        hashMap["uid"] = registeredUserId
        hashMap["timestamp"] = timestamp
        hashMap["phoneCode"] = ""
        hashMap["phoneNumber"] = ""
        hashMap["profileImageUrl"] = ""
        hashMap["userType"] = "Email"
        hashMap["onlineStatus"] = true

        // set data to firebase realtime db
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(registeredUserId!!).setValue(hashMap).addOnSuccessListener {
            progressDialog.dismiss()
            Log.d(TAG, "updateUserInfoDb: User Registered")
            Toast.makeText(this, "Successfully Signed Up", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }.addOnFailureListener { e ->
            Log.e(TAG, "updateUserInfoDb: ", e)
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            progressDialog.dismiss()
        }
    }
}