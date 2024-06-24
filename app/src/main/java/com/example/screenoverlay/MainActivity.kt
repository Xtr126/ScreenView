package com.example.screenoverlay

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.view.Display
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup

class MainActivity : Activity(), View.OnTouchListener, TextureView.SurfaceTextureListener {
    private var displayRealHeight: Int = 0
    private var displayRealWidth: Int = 0
    private lateinit var view: TextureView
    private var virtualDisplay: VirtualDisplay? = null

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = TextureView(this)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(view)
        view.setOnTouchListener(this)
        view.surfaceTextureListener = this
    }

    private fun getDisplayMetrics() {
        val outSize = Point()
        view.display.getRealSize(outSize);
        displayRealWidth = outSize.x
        displayRealHeight = outSize.y
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        // Check if we are on a secondary display
        if (view.display.displayId != Display.DEFAULT_DISPLAY &&
            event != null) {
            val x = event.x / view.width * displayRealWidth
            val y = event.y / view.height * displayRealHeight
            val e: MotionEvent = MotionEvent.obtain(event.downTime, event.eventTime, event.action, x, y, event.pressure, event.size, event.metaState, event.xPrecision, event.yPrecision, event.deviceId, event.edgeFlags);
            Input.injectInputEvent(e)
            return true
        }
        return false
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        getDisplayMetrics()
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        virtualDisplay = displayManager.createVirtualDisplay("screenview", view.width, view.height, 160, Surface(surface), VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }
}
