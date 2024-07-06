package com.example.findit.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        if(firebaseAuth.currentUser == null) {
            Handler(Looper.getMainLooper()).postDelayed({
                Intent(this@SplashActivity, MainActivity::class.java).also { intent ->
                    startActivity(intent)
                    finishAffinity()
                }
            }, 1000)
        } else {
            // Check if Admin is logged in
            checkAdmin()
        }
    }

    private fun checkAdmin() {
        FirebaseDatabase.getInstance().getReference("Admins").child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            Intent(this@SplashActivity, AdminActivity::class.java).also { intent ->
                                startActivity(intent)
                                finishAffinity()
                            }
                        }, 1000)
                    } else {
                        Handler(Looper.getMainLooper()).postDelayed({
                            Intent(this@SplashActivity, MainActivity::class.java).also { intent ->
                                startActivity(intent)
                                finishAffinity()
                            }
                        }, 1000)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}