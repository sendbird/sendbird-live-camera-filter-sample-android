package com.sendbird.live.filtersample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        (application as BaseApplication).initLiveData.observe(this) {
            if (it) startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}