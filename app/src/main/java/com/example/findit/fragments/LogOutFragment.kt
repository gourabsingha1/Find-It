package com.example.findit.fragments

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.findit.R
import com.google.firebase.auth.FirebaseAuth

class LogOutFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            // Inflate the custom layout
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.fragment_log_out, null)

            // Set up the EditText and Button
            val btnLogOut = view.findViewById<Button>(R.id.btnLogOut)
            btnLogOut.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.flFragment, HomeFragment())?.commit()
                dismiss()
            }
            val btnLogOutCancel = view.findViewById<Button>(R.id.btnLogOutCancel)
            btnLogOutCancel.setOnClickListener {
                dismiss()
            }

            // Set the custom layout
            builder.setView(view)

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}