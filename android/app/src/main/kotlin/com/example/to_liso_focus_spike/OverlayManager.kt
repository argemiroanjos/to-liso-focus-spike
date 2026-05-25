package com.example.to_liso_focus_spike

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class OverlayManager(private val context: Context) {

    private val tag = "OverlayManager"

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null

    fun show(packageName: String) {

        if (!Settings.canDrawOverlays(context)) {
            Log.w(tag, "Permissão de overlay não concedida")
            return
        }

        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = LayoutInflater.from(context)
            .inflate(R.layout.overlay_view, null)

        overlayView?.findViewById<TextView>(R.id.tv_package)
            ?.text = packageName

        overlayView?.findViewById<Button>(R.id.btn_release)
            ?.setOnClickListener {
                hide()
            }

        windowManager.addView(overlayView, params)

        Log.d(tag, "Overlay exibido")
    }

    fun hide() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}