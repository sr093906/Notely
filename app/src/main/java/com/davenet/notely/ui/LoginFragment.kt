package com.davenet.notely.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.davenet.notely.R
import com.davenet.notely.util.hideKeyboard
import com.davenet.notely.util.inputValidation
import com.davenet.notely.util.setupLoadingDialog
import com.davenet.notely.util.showErrorDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loadingDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().apply {
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            toolbar.isVisible = false
        }
        auth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        loadingDialog = setupLoadingDialog(requireContext(), requireActivity())
    }

    override fun onResume() {
        super.onResume()
        Log.d("login", "onResume called")
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onStart() {
        super.onStart()
        Log.d("login", "onStart called")
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun initListeners() {
        buttonRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        buttonLogin.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        if (!inputValidation(userEmailLogin, userPassword)) {
            hideKeyboard(view, requireContext())
            loadingDialog.show()
            auth.signInWithEmailAndPassword(
                userEmailLogin.text.toString(),
                userPassword.text.toString()
            )
                .addOnCompleteListener(requireActivity()) { task ->
                    loadingDialog.dismiss()
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        showErrorDialog(
                            task.exception?.message,
                            requireContext(),
                            requireActivity()
                        )
                    }
                }
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                findNavController().navigate(R.id.action_loginFragment_to_noteListFragment)
            } else {
                Toast.makeText(
                    requireContext(), "Please verify your  email.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}