
/*
 * Copyright (c) 2022 Sendbird, Inc.
 */
package com.sendbird.live.filtersample

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import com.banuba.sdk.effect_player.ConsistencyMode
import com.banuba.sdk.effect_player.EffectPlayer
import com.banuba.sdk.effect_player.EffectPlayerConfiguration
import com.banuba.sdk.effect_player.NnMode
import com.banuba.sdk.internal.utils.OrientationHelper
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.offscreen.BufferAllocator
import com.banuba.sdk.offscreen.ImageProcessResult
import com.banuba.sdk.offscreen.OffscreenEffectPlayer
import com.banuba.sdk.offscreen.OffscreenSimpleConfig
import com.banuba.sdk.recognizer.FaceSearchMode
import com.banuba.sdk.types.FullImageData
import com.sendbird.live.*
import com.sendbird.live.filtersample.Constants.KEY_LIVE_EVENT_ID
import com.sendbird.live.filtersample.databinding.ActivityLiveEventBinding
import org.webrtc.JavaI420Buffer
import org.webrtc.JniCommon
import org.webrtc.VideoFrame
import java.nio.ByteBuffer
import java.util.*

class LiveEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveEventBinding
    private var liveEvent: LiveEvent? = null

    private var mOEP: OffscreenEffectPlayer? = null
    private val mBuffersQueue: BuffersQueue = BuffersQueue()
    private val handler = Handler(Looper.getMainLooper())
    private var useFilter = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SendbirdLive.currentUser ?: finish()
        val liveEventId = intent.getStringExtra(KEY_LIVE_EVENT_ID)
        if (liveEventId.isNullOrBlank()) return
        initOEP()
        initOEPFramesOutputToSendbirdLiveSDK()
        startLiveEvent(liveEventId)

        binding.ivClose.setOnClickListener {
            liveEvent?.endEvent(null)
            finish()
        }
        binding.ivFilterState.setOnClickListener {
            useFilter = !useFilter
            if (useFilter) {
                mOEP?.loadEffect("TrollGrandma")
                liveEvent?.startUsingExternalVideo()
            } else {
                mOEP?.unloadEffect()
                liveEvent?.stopUsingExternalVideo()
            }
        }
    }

    private fun startLiveEvent(liveEventId: String) {
        SendbirdLive.getLiveEvent(liveEventId) { liveEvent, e ->
            if (liveEvent == null || e != null) {
                return@getLiveEvent
            }
            this.liveEvent = liveEvent
            liveEvent.addVideoFrameListener(videoFrameListener)
            liveEvent.startUsingExternalVideo()

            liveEvent.startEvent {
                if (it != null) return@startEvent
                val hostId = liveEvent.host?.hostId ?: return@startEvent
                liveEvent.setVideoViewForLiveEvent(binding.svvLiveEvent, hostId)
            }
        }
    }

    private fun initOEP() {
        BanubaSdkManager.initialize(this, BANUBA_TOKEN)
        val size = getWindowSize()
        val effectPlayerConfig = EffectPlayerConfiguration(
            size.width,
            size.height,
            NnMode.ENABLE,
            FaceSearchMode.MEDIUM,
            false,
            false
        )
        OrientationHelper.getInstance(this).startDeviceOrientationUpdates()

        val effectPlayer = EffectPlayer.create(effectPlayerConfig) ?: return
        effectPlayer.setRenderConsistencyMode(ConsistencyMode.ASYNCHRONOUS_CONSISTENT)
        val oepConfig = OffscreenSimpleConfig.newBuilder(mBuffersQueue).build()
        mOEP = OffscreenEffectPlayer(applicationContext, effectPlayer, size, oepConfig)
    }

    private fun initOEPFramesOutputToSendbirdLiveSDK() {
        val mOEP = mOEP ?: return
        mOEP.setImageProcessListener({ oepImageResult: ImageProcessResult ->
            val width = oepImageResult.width
            val height = oepImageResult.height
            val buffer = oepImageResult.buffer
            mBuffersQueue.retainBuffer(buffer)
            val dataY = oepImageResult.getPlaneBuffer(0)
            val strideY = oepImageResult.getBytesPerRowOfPlane(0)
            val dataU = oepImageResult.getPlaneBuffer(1)
            val strideU = oepImageResult.getBytesPerRowOfPlane(1)
            val dataV = oepImageResult.getPlaneBuffer(2)
            val strideV = oepImageResult.getBytesPerRowOfPlane(2)
            val i420buffer =
                JavaI420Buffer.wrap(
                    width, height, dataY, strideY, dataU, strideU, dataV, strideV
                ) { JniCommon.nativeFreeByteBuffer(buffer) }
            val videoFrame = VideoFrame(
                i420buffer,
                oepImageResult.orientation.rotationAngle,
                oepImageResult.timestamp
            )
            liveEvent?.enqueueExternalVideoFrame(videoFrame)
        }, handler)
    }

    private fun getWindowSize(): Size {
        val displayMetrics = Resources.getSystem().displayMetrics
        return Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    private val videoFrameListener = VideoFrameListener { videoFrame ->
        val deviceOrientationAngle = OrientationHelper.getInstance(this).getDeviceOrientationAngle()
        val orientation = OrientationHelper.getOrientation(videoFrame.rotation, deviceOrientationAngle, false)
        val i420Buffer: VideoFrame.I420Buffer = videoFrame.buffer.toI420() ?: return@VideoFrameListener
        val fullImageData = FullImageData(
            Size(i420Buffer.width, i420Buffer.height), i420Buffer.dataY,
            i420Buffer.dataU, i420Buffer.dataV, i420Buffer.strideY,
            i420Buffer.strideU, i420Buffer.strideV, 1, 1, 1, orientation
        )
        mOEP?.processFullImageData(fullImageData, { i420Buffer.release() }, videoFrame.timestampNs)
    }

    private class BuffersQueue : BufferAllocator {
        private val capacity = 4
        private val queue: Queue<ByteBuffer> = LinkedList()

        @Synchronized
        override fun allocateBuffer(minimumCapacity: Int): ByteBuffer {
            val buffer = queue.poll()
            return if (buffer != null && buffer.capacity() >= minimumCapacity) {
                buffer.rewind().limit()
                buffer.limit(buffer.capacity())
                buffer
            } else {
                ByteBuffer.allocateDirect(minimumCapacity)
            }
        }

        @Synchronized
        fun retainBuffer(buffer: ByteBuffer) {
            if (queue.size < capacity) {
                queue.add(buffer)
            }
        }
    }

}