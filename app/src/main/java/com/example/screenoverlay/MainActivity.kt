package com.example.screenoverlay

import android.app.Activity
import android.content.Intent
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Display
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup

class MainActivity : Activity(), View.OnTouchListener {
    private lateinit var view: TextureView
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var virtualDisplay: VirtualDisplay? = null
    private val request = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == request) {
            if (resultCode == RESULT_OK) {
                val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
                virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                    view.width, view.height, 160,
                    VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    Surface(view.surfaceTexture), null, null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = applicationContext.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        view = TextureView(this)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(view)
        view.setOnTouchListener(this)
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), request)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        // Check if we are on a secondary display
        if (view.display.displayId != Display.DEFAULT_DISPLAY) {
            Input.injectInputEvent(event)
        }
        return super.onTouchEvent(event)
    }
}
