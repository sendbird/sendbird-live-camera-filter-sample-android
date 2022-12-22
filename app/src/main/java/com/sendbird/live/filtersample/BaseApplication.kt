
/*
 * Copyright (c) 2022 Sendbird, Inc.
 */
package com.sendbird.live.filtersample

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sendbird.android.SendbirdChat
import com.sendbird.android.exception.SendbirdException
import com.sendbird.android.handler.InitResultHandler
import com.sendbird.android.params.InitParams
import com.sendbird.live.SendbirdLive

const val SENDBIRD_APP_ID = "YOUR_SENDBIRD_APPLICATION_ID"
const val BANUBA_TOKEN = "YOUR_BANUBA_TOKEN"

class BaseApplication : Application() {
    private val _initLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val initLiveData: LiveData<Boolean>
        get() = _initLiveData

    override fun onCreate() {
        super.onCreate()
        val params = InitParams(SENDBIRD_APP_ID, applicationContext, false)
        SendbirdChat.init(params, object : InitResultHandler {
            override fun onInitFailed(e: SendbirdException) {
                _initLiveData.value = false
            }

            override fun onInitSucceed() {
                _initLiveData.value = SendbirdLive.init(applicationContext, SENDBIRD_APP_ID)
            }

            override fun onMigrationStarted() {
            }
        })
    }
}