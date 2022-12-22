
/*
 * Copyright (c) 2022 Sendbird, Inc.
 */
package com.sendbird.live.filtersample

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.sendbird.live.LiveEventCreateParams
import com.sendbird.live.MediaOptions
import com.sendbird.live.SendbirdLive
import com.sendbird.live.filtersample.Constants.KEY_LIVE_EVENT_ID
import com.sendbird.live.filtersample.databinding.ActivityStartLiveEventBinding

class CreateLiveEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartLiveEventBinding

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartLiveEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SendbirdLive.currentUser ?: finish()
        permissionLauncher.launch(permissions)
        binding.btStartNewLiveEvent.setOnClickListener {
            val hostId = SendbirdLive.currentUser?.userId ?: return@setOnClickListener
            goLiveEvent(hostId)
        }
    }

    private fun goLiveEvent(hostId: String) {
        val params = LiveEventCreateParams(listOf(hostId))
        SendbirdLive.createLiveEvent(params) { liveEvent, e ->
            if (liveEvent == null || e != null) {
                return@createLiveEvent
            }
            liveEvent.enterAsHost(MediaOptions()) {
                if (it != null) return@enterAsHost
                val intent = Intent(this, LiveEventActivity::class.java)
                intent.putExtra(KEY_LIVE_EVENT_ID, liveEvent.liveEventId)
                startActivity(intent)
            }
        }
    }
}