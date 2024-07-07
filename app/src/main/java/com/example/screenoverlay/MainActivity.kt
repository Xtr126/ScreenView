package com.example.screenoverlay

import android.app.Activity
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.os.RemoteException
import android.preference.PreferenceManager
import android.util.Log
import android.view.Display
import android.view.IRotationWatcher
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.LinearLayout

class MainActivity : Activity(), View.OnTouchListener, TextureView.SurfaceTextureListener,
    View.OnGenericMotionListener {
    private lateinit var windowManagerService: IInterface
    private lateinit var displayManager: DisplayManager
    private var displayRealHeight: Int = 0
    private var displayRealWidth: Int = 0
    private lateinit var view: TextureView
    private var useInput = false
    private var virtualDisplay: VirtualDisplay? = null
    private val TAG: String = "main_activity"


    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        
        windowManagerService.javaClass.getMethod(
            "removeRotationWatcher",
            IRotationWatcher::class.java
        ).invoke(windowManagerService, screenRotationChanged)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        view = findViewById(R.id.textureView)
        view.setOnTouchListener(this)
        view.setOnGenericMotionListener(this)
        view.surfaceTextureListener = this

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        useInput = sharedPreferences.getBoolean("use_input", false)
        Log.i(TAG, "Input enabled: $useInput")

        watchRotation()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        getDisplayMetrics()
        updateTextureViewSize(width, height)
        virtualDisplay = displayManager.createVirtualDisplay("screenview", view.layoutParams.width, view.layoutParams.height, 160, Surface(surface), VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR)
    }

    private fun getDisplayMetrics() {
        displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val outSize = Point()
        displayManager.getDisplay(Display.DEFAULT_DISPLAY).getRealSize(outSize)
        displayRealWidth = outSize.x
        displayRealHeight = outSize.y
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        // Check if we are on a secondary display
        if (view.display.displayId != Display.DEFAULT_DISPLAY &&
            event != null && useInput) {
            Input.injectInputEvent(transformEvent(event))
            return true
        }
        return false
    }


    override fun onGenericMotion(v: View?, event: MotionEvent?): Boolean {
        return onTouch(v, event)
    }

    private fun transformEvent(event: MotionEvent): MotionEvent {
        val pointerProperties = arrayOfNulls<PointerProperties>(event.pointerCount)
        val pointerCoords = arrayOfNulls<PointerCoords>(event.pointerCount)
        for (i in 0..<event.pointerCount) {
            pointerProperties[i] = PointerProperties()
            pointerCoords[i] = PointerCoords()
            event.getPointerProperties(i, pointerProperties[i])
            event.getPointerCoords(i, pointerCoords[i])
            pointerCoords[i]!!.x = pointerCoords[i]!!.x / view.width * displayRealWidth
            pointerCoords[i]!!.y = pointerCoords[i]!!.y / view.height * displayRealHeight
        }
        return MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            event.action,
            event.pointerCount,
            pointerProperties,
            pointerCoords,
            event.metaState,
            event.buttonState,
            event.xPrecision,
            event.yPrecision,
            event.deviceId,
            event.edgeFlags,
            event.source,
            event.flags
        )
    }

    private fun updateTextureViewSize(maxWidth: Int, maxHeight: Int) {
        Log.i(TAG, "display width: $displayRealWidth")
        Log.i(TAG, "display height: $displayRealHeight")
        val screenRatio =  displayRealWidth.toDouble() / displayRealHeight
        val viewRatio = maxWidth.toDouble() / maxHeight
        Log.i(TAG, "viewratio: $viewRatio")
        Log.i(TAG, "screenratio: $screenRatio")
        val viewWidth: Int
        val viewHeight: Int

        if (viewRatio > screenRatio) {
            // View is wider than screen
            viewWidth = (maxHeight * screenRatio).toInt()
            viewHeight = maxHeight
        } else {
            // Screen is wider than view
            viewHeight = (maxWidth / screenRatio).toInt()
            viewWidth = maxWidth
        }
        view.layoutParams = LinearLayout.LayoutParams(viewWidth, viewHeight)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    private val screenRotationChanged: IRotationWatcher.Stub = object : IRotationWatcher.Stub() {
        @Throws(RemoteException::class)
        override fun onRotationChanged(rotation: Int) {
            runOnUiThread { recreate() }
        }
    }
    
    private fun watchRotation() {
        val serviceManager = Class.forName("android.os.ServiceManager")
        val serviceBinder = serviceManager.getMethod("getService", String::class.java)
            .invoke(null, "window") as IBinder

        windowManagerService = Class.forName("android.view.IWindowManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, serviceBinder) as IInterface

        windowManagerService.javaClass.getMethod(
            "watchRotation",
            IRotationWatcher::class.java,
            Int::class.javaPrimitiveType
        ).invoke(
            windowManagerService,
            screenRotationChanged,
            Display.DEFAULT_DISPLAY
        )
    }
}
