package com.example.findit.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.databinding.ActivityAdminBinding
import com.google.firebase.auth.FirebaseAuth

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            tvAdminProductsQueue.setOnClickListener {
                startActivity(Intent(this@AdminActivity, AdminProductsQueueActivity::class.java))
            }
            tvAdminProducts.setOnClickListener {
                startActivity(Intent(this@AdminActivity, AdminProductsActivity::class.java))
            }
            tvAdminLogOut.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this@AdminActivity, MainActivity::class.java))
                finishAffinity()
            }
        }
    }
}