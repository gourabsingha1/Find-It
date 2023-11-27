package com.example.findit.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.findit.activity.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log
import com.bumptech.glide.Glide
import com.example.findit.R
import com.example.findit.activity.MyProductsActivity
import com.example.findit.activity.WishlistProductsActivity
import com.example.findit.databinding.FragmentAccountBinding
import java.lang.Exception

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding

    private companion object {
        private const val TAG = "ACCOUNT_TAG"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAccountBinding.inflate(layoutInflater)

        // Load Data
        loadMyData()

        // My Products
        binding.tvAccountMyProducts.setOnClickListener {
            startActivity(Intent(requireContext(), MyProductsActivity::class.java))
        }

        // Wishlist
        binding.tvAccountWishlist.setOnClickListener {
//            Toast.makeText(requireContext(), "Wishlist products yet to implement", Toast.LENGTH_LONG).show()
            startActivity(Intent(requireContext(), WishlistProductsActivity::class.java))
        }

        // Settings
        binding.tvAccountSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        return binding.root
    }

    private fun loadMyData() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // get data
                val name = "${snapshot.child("name").value}"
                val email = "${snapshot.child("email").value}"
                var timestamp = "${snapshot.child("timestamp").value}"
                val phoneCode = "${snapshot.child("phoneCode").value}"
                val phoneNumber = "${snapshot.child("phoneNumber").value}"
                val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                val userType = "${snapshot.child("userType").value}"
                val onlineStatus = "${snapshot.child("onlineStatus").value}"
                val phone = phoneCode + phoneNumber

                // set data
                binding.tvAccountName.text = name
                binding.tvAccountEmail.text = email
                binding.tvAccountPhone.text = phone
                try {
                    Glide.with(requireContext())
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.ivAccountProfilePic)
                } catch (e : Exception) {
                    Log.e(TAG, "onDataChange: ", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }
}