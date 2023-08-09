package com.example.findit.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.findit.R
import com.example.findit.databinding.ActivityMainBinding
import com.example.findit.fragments.AddFragment
import com.example.findit.fragments.HomeFragment
import com.example.findit.fragments.AccountFragment
import com.google.firebase.auth.FirebaseAuth

// fragment overlapping

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Set home fragment as default fragment
        setFragment(HomeFragment())

        // Get instance of firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Bottom navigation view
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bnmHome -> {
                    setFragment(HomeFragment())
                    true
                }
                R.id.bnmAdd -> {
                    if(firebaseAuth.currentUser == null) {
                        startActivity(Intent(this, LoginOptionsActivity::class.java))
                        false
                    }
                    else {
                        setFragment(AddFragment())
                        true
                    }
                }
                R.id.bnmAccount -> {
                    if(firebaseAuth.currentUser == null) {
                        startActivity(Intent(this, LoginOptionsActivity::class.java))
                        false
                    }
                    else {
                        setFragment(AccountFragment())
                        true
                    }
                }
                else -> {
                    false
                }
            }
        }

        // Bottom navigation view
//        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
//        val navController = findNavController(R.id.flFragment)
//        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.addFragment, R.id.profileFragment))
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        bottomNavigationView.setupWithNavController(navController)
    }

    // Set Fragment
    private fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragment, fragment).commit()
    }
}