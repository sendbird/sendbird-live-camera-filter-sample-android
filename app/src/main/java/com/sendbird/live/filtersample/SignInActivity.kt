
/*
 * Copyright (c) 2022 Sendbird, Inc.
 */
package com.sendbird.live.filtersample

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import com.sendbird.live.AuthenticateParams
import com.sendbird.live.SendbirdLive
import com.sendbird.live.filtersample.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btSignIn.setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken, 0) // hide soft keyboard
            val userId = binding.etUserId.text.toString().ifBlank { return@setOnClickListener }
            sendbirdAuth(userId)
        }
    }

    private fun sendbirdAuth(userId: String, accessToken: String? = null) {
        val params = AuthenticateParams(userId, accessToken)
        SendbirdLive.authenticate(params
        ) { user, e ->
            if (user == null || e != null) {
                return@authenticate
            }
            startActivity(Intent(this, CreateLiveEventActivity::class.java))
            finish()
        }
    }
}