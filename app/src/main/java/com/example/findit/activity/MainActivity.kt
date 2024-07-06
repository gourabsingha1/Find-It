package com.example.findit.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.findit.R
import com.example.findit.databinding.ActivityMainBinding
import com.example.findit.fragments.AddFragment
import com.example.findit.fragments.HomeFragment
import com.example.findit.fragments.AccountFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Set home fragment as default fragment
        setFragment(HomeFragment())

        // Bottom navigation view
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bnmHome -> {
                    setFragment(HomeFragment())
                }
                R.id.bnmAdd -> {
                    if(firebaseAuth.currentUser == null) {
                        startActivity(Intent(this, LoginOptionsActivity::class.java))
                    }
                    else {
                        setFragment(AddFragment())
                    }
                }
                else -> {
                    if(firebaseAuth.currentUser == null) {
                        startActivity(Intent(this, LoginOptionsActivity::class.java))
                    }
                    else {
                        setFragment(AccountFragment())
                    }
                }
            }
            true
        }

        // Bottom navigation view
//        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
//        val navController = findNavController(R.id.flFragment)
//        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.addFragment, R.id.accountFragment))
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        bottomNavigationView.setupWithNavController(navController)
    }

    // Set Fragment
    private fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragment, fragment).commit()
    }
}