package com.example.findit.activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.R
import com.example.findit.databinding.ActivityLoginOptionsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginOptionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginOptionsBinding
    lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var progressDialog: ProgressDialog

    private companion object {
        private const val TAG = "LOGIN_OPTIONS_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Go back
        binding.ivLoginOptionsClose.setOnClickListener {
            onBackPressed()
        }

        // Setup ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setCanceledOnTouchOutside(false)

        // initializations
        firebaseAuth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google login
        binding.btnLoginOptionsGoogle.setOnClickListener {
            googleLogin()
        }

        // Phone login
        binding.btnLoginOptionsPhone.setOnClickListener {
            startActivity(Intent(this, LoginPhoneActivity::class.java))
        }

        // Email login
        binding.btnLoginOptionsEmail.setOnClickListener {
            startActivity(Intent(this, LogInEmailActivity::class.java))
        }

        // Admin login
        binding.btnLoginOptionsAdmin.setOnClickListener {
            Intent(this, LogInEmailActivity::class.java).also {
                it.putExtra("EXTRA_ADMIN", true)
                startActivity(it)
            }
        }
    }

    private fun googleLogin() {
        Log.d(TAG, "googleLogin: ")
        val googleSignInIntent = mGoogleSignInClient.signInIntent
        googleSignInARL.launch(googleSignInIntent)
    }

    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "googleSignInARL: ")

        if(result.resultCode == RESULT_OK) {
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "googleSignInARL: Account ID: ${account.id}")
                // SignIn Success. Let's signIn with Firebase Auth
                firebaseAuthWithGoogleAccount(account)
            } catch (e: Exception) {
                Log.e(TAG, "googleSignInARL: ", e)
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
        } else {
            // User has cancelled Google SignIn
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        }
    }

    // create / login to google account
    private fun firebaseAuthWithGoogleAccount(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnSuccessListener { authResult ->
            if(authResult.additionalUserInfo!!.isNewUser) {
                Log.d(TAG, "firebaseAuthWithGoogleAccount: New User, Account Created")
                updateUserInfoDb()
            } else {
                Log.d(TAG, "firebaseAuthWithGoogleAccount: Existing User, Logged In")
                Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "firebaseAuthWithGoogleAccount: ", e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    // If new google user, then push user in firebase realtime db
    private fun updateUserInfoDb() {
        Log.d(TAG, "updateUserInfoDb: ")
        progressDialog.setMessage("Saving User Info")
        progressDialog.show()

        val name = firebaseAuth.currentUser?.displayName
        val registeredUserEmail = firebaseAuth.currentUser!!.email
        val registeredUserId = firebaseAuth.uid
        val timestamp = System.currentTimeMillis()

        // setup data to save in firebase realtime db
        // most of the data will be empty and will set in edit profile
        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = name
        hashMap["email"] = registeredUserEmail
        hashMap["uid"] = registeredUserId
        hashMap["timestamp"] = timestamp
        hashMap["phoneCode"] = ""
        hashMap["phoneNumber"] = ""
        hashMap["profileImageUrl"] = ""
        hashMap["userType"] = "Google"
        hashMap["onlineStatus"] = true

        // set data to firebase realtime db
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(registeredUserId!!).setValue(hashMap).addOnSuccessListener {
            Log.d(TAG, "updateUserInfoDb: User Registered")
            progressDialog.dismiss()
            Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }.addOnFailureListener { e ->
            Log.e(TAG, "updateUserInfoDb: ", e)
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            progressDialog.dismiss()
        }
    }
}