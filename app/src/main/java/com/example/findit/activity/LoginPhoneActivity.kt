package com.example.findit.activity

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.findit.databinding.ActivityLoginPhoneBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

// https://stackoverflow.com/questions/65490555/this-request-is-missing-a-valid-app-identifier-meaning-that-neither-safetynet-c

class LoginPhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginPhoneBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var forceRefreshingToken: ForceResendingToken? = null
    private lateinit var mCallbacks: OnVerificationStateChangedCallbacks
    private var mVerificationId: String? = null
    private var phoneCode = ""
    private var phoneNumber = ""
    private var phoneNumberWithCode = ""

    private companion object {
        private const val TAG = "LOGIN_PHONE_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Visibility
        binding.rlLoginPhonePhoneInput.visibility = View.VISIBLE
        binding.rlOtpInput.visibility = View.GONE

        // Setup ProgressDialogue to show while login
        progressDialog = ProgressDialog(this)
        progressDialog.setCanceledOnTouchOutside(false)

        // Go back
        binding.ivLoginPhoneBack.setOnClickListener {
            onBackPressed()
        }

        // Listen for phone login callbacks
        phoneLoginCallbacks()

        // Send OTP
        binding.btnLoginPhoneSendOtp.setOnClickListener {
            validateData()
        }

        // Resend OTP
        binding.tvLoginPhoneResendOtp.setOnClickListener {
            resendVerificationCode(forceRefreshingToken)
        }

        // Verify OTP
        binding.btnLoginPhoneVerify.setOnClickListener {
            val otp = binding.etLoginPhoneOtp.text.toString().trim()
            Log.d(TAG, "onCreate: otp: $otp")
            if(otp.isEmpty()) {
                binding.etLoginPhoneOtp.error = "Enter OTP"
                binding.etLoginPhoneOtp.requestFocus()
            } else if(otp.length < 6) {
                binding.etLoginPhoneOtp.error = "OTP length must be 6 characters long"
                binding.etLoginPhoneOtp.requestFocus()
            } else {
                verifyPhoneNumberWithCode(mVerificationId, otp)
            }
        }
    }


    // Validate data before sending OTP
    private fun validateData() {
        phoneCode = binding.ccpLoginPhone.selectedCountryCodeWithPlus
        phoneNumber = binding.etLoginPhonePhone.text.toString()
        phoneNumberWithCode = phoneCode + phoneNumber

        Log.d(TAG, "validateData: phoneCode: $phoneCode")
        Log.d(TAG, "validateData: phoneNumber: $phoneNumber")
        Log.d(TAG, "validateData: phoneNumberWithCode: $phoneNumberWithCode")

        if(phoneNumber.isEmpty()) {
            binding.etLoginPhonePhone.error = "Enter Phone Number"
            binding.etLoginPhonePhone.requestFocus()
        } else {
            startPhoneNumberVerification()
        }
    }


    // Send OTP
    private fun startPhoneNumberVerification() {
        Log.d(TAG, "startPhoneNumberVerification: ")
        progressDialog.setMessage("Sending OTP")
        progressDialog.show()

        // Setup phone auth options
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumberWithCode)
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout = 60s
            .setActivity(this) // Activity for callback binding
            .setCallbacks(mCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    // Listen for phone login callbacks
    private fun phoneLoginCallbacks() {
        Log.d(TAG, "phoneLoginCallbacks: ")
        mCallbacks = object: OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: ")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                Log.e(TAG, "onVerificationFailed: ", e)
                Toast.makeText(this@LoginPhoneActivity, e.message, Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String, token: ForceResendingToken) {
                // Visibility
                binding.rlLoginPhonePhoneInput.visibility = View.GONE
                binding.rlOtpInput.visibility = View.VISIBLE

                mVerificationId = verificationId
                forceRefreshingToken = token
                progressDialog.dismiss()
                Log.d(TAG, "onCodeSent: verificationId: $verificationId")
                Toast.makeText(this@LoginPhoneActivity, "OTP is sent to $phoneNumberWithCode", Toast.LENGTH_LONG).show()
            }
        }
    }


    // Resend OTP
    private fun resendVerificationCode(token: ForceResendingToken?) {
        Log.d(TAG, "resendVerificationCode: ")
        progressDialog.setMessage("Resending OTP")
        progressDialog.show()

        // Setup phone auth options
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumberWithCode)
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout = 60s
            .setActivity(this) // Activity for callback binding
            .setCallbacks(mCallbacks)
            .setForceResendingToken(token!!)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    // Verify OTP
    private fun verifyPhoneNumberWithCode(verificationId: String?, otp: String) {
        Log.d(TAG, "verifyPhoneNumberWithCode: mVerificationId: $verificationId")
        Log.d(TAG, "verifyPhoneNumberWithCode: otp: $otp")

        progressDialog.setMessage("Verifying OTP")
        progressDialog.show()

        // PhoneAuthCredential with verification id and OTP to signIn user with signInWithPhoneAuthCredential
        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
        signInWithPhoneAuthCredential(credential)
    }


    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "signInWithPhoneAuthCredential: credential: $credential")

        progressDialog.setMessage("Logging In")
        progressDialog.show()

        firebaseAuth.signInWithCredential(credential).addOnSuccessListener { authResult ->
            if(authResult.additionalUserInfo!!.isNewUser) {
                Log.d(TAG, "signInWithPhoneAuthCredential: New User, Account Created")
                updateUserInfoDb()
            } else {
                Log.d(TAG, "signInWithPhoneAuthCredential: Existing User, Logged In")
                Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
        }.addOnFailureListener { e ->
            progressDialog.dismiss()
            Log.e(TAG, "signInWithPhoneAuthCredential: ", e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }


    // If new google user, then push user in firebase realtime db
    private fun updateUserInfoDb() {
        Log.d(TAG, "updateUserInfoDb: ")
        progressDialog.setMessage("Saving User Info")
        progressDialog.show()

        val registeredUserId = firebaseAuth.uid
        val timestamp = System.currentTimeMillis()

        // setup data to save in firebase realtime db
        // most of the data will be empty and will set in edit profile
        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = ""
        hashMap["email"] = ""
        hashMap["uid"] = registeredUserId
        hashMap["timestamp"] = timestamp
        hashMap["phoneCode"] = phoneCode
        hashMap["phoneNumber"] = phoneNumber
        hashMap["profileImageUrl"] = ""
        hashMap["userType"] = "Phone"
        hashMap["onlineStatus"] = true

        // set data to firebase realtime db
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(registeredUserId!!).setValue(hashMap).addOnSuccessListener {
            progressDialog.dismiss()
            Log.d(TAG, "updateUserInfoDb: User Registered")
            Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }.addOnFailureListener { e ->
            progressDialog.dismiss()
            Log.e(TAG, "updateUserInfoDb: ", e)
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }
    }
}