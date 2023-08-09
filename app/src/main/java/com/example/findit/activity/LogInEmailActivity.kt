package com.example.findit.activity

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.example.findit.databinding.ActivityLoginEmailBinding
import com.google.firebase.auth.FirebaseAuth

class LogInEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private companion object {
        private const val TAG = "LOGIN_EMAIL_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Setup ProgressDialogue to show while login
        progressDialog = ProgressDialog(this)
        progressDialog.setCanceledOnTouchOutside(false)

        // Go back
        binding.ivLoginEmailBack.setOnClickListener {
            onBackPressed()
        }

        // Login
        binding.btnLogin.setOnClickListener {
            validateData()
        }

        // Sign up activity
        binding.tvLoginEmailSignup.setOnClickListener {
            startActivity(Intent(this, SignUpEmailActivity::class.java))
        }
    }


    // Validate email and password
    private fun validateData() {
        val email = binding.etLoginEmailEmail.text.toString().trim()
        val password = binding.etLoginEmailPassword.text.toString().trim()

        Log.d(TAG, "validateData: email: $email")
        Log.d(TAG, "validateData: password: $password")

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etLoginEmailEmail.error = "Invalid Email Format"
            binding.etLoginEmailEmail.requestFocus()
        } else if(password.isEmpty()) {
            binding.etLoginEmailPassword.error = "Enter Password"
            binding.etLoginEmailPassword.requestFocus()
        } else {
            loginFirebase()
        }
    }


    // Login Firebase
    private fun loginFirebase() {
        Log.d(TAG, "LoginUser: ")
        progressDialog.setMessage("Logging In")
        progressDialog.show()

        val email = binding.etLoginEmailEmail.text.toString()
        val password = binding.etLoginEmailPassword.text.toString()

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
                Intent(this, MainActivity::class.java).also { intent ->
                    startActivity(intent)
                    finishAffinity()
                }
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
        }
    }
}