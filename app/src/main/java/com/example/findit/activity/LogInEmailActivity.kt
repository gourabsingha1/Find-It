package com.example.findit.activity

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.findit.databinding.ActivityLoginEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LogInEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var isAdmin = false

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

        // Check if Admin is trying to log in
        isAdmin = intent.getBooleanExtra("EXTRA_ADMIN", false)
        if(isAdmin) {
            binding.tvLoginEmailHaventSignedUp.visibility = View.INVISIBLE
            binding.tvLoginEmailSignup.visibility = View.INVISIBLE
        }

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
            if(isAdmin) {
                loginAdminFirebase()
            } else {
                loginUserFirebase()
            }
        }
    }

    // Login User Firebase
    private fun loginUserFirebase() {
        Log.d(TAG, "LoginUser: ")
        progressDialog.setMessage("Logging In")
        progressDialog.show()

        val email = binding.etLoginEmailEmail.text.toString()
        val password = binding.etLoginEmailPassword.text.toString()

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                FirebaseDatabase.getInstance().getReference("Users").child(firebaseAuth.uid!!)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(this@LogInEmailActivity, "Successfully Logged In", Toast.LENGTH_SHORT).show()
                                Intent(this@LogInEmailActivity, MainActivity::class.java).also { intent ->
                                    startActivity(intent)
                                    finishAffinity()
                                }
                            } else {
                                Toast.makeText(this@LogInEmailActivity, "No User found", Toast.LENGTH_LONG).show()
                                firebaseAuth.signOut()
                            }
                            progressDialog.dismiss()
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
            } else {
                Toast.makeText(this, "No User found", Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }
        }
    }

    // Login Admin Firebase
    private fun loginAdminFirebase() {
        progressDialog.setMessage("Logging In")
        progressDialog.show()

        val email = binding.etLoginEmailEmail.text.toString()
        val password = binding.etLoginEmailPassword.text.toString()

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                FirebaseDatabase.getInstance().getReference("Admins").child(firebaseAuth.uid!!)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(this@LogInEmailActivity, "Successfully Logged In", Toast.LENGTH_SHORT).show()
                                Intent(this@LogInEmailActivity, AdminActivity::class.java).also { intent ->
                                    startActivity(intent)
                                    finishAffinity()
                                }
                            } else {
                                Toast.makeText(this@LogInEmailActivity, "No Admin found", Toast.LENGTH_LONG).show()
                                firebaseAuth.signOut()
                            }
                            progressDialog.dismiss()
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
            } else {
                Toast.makeText(this, "No Admin found", Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }
        }
    }
}