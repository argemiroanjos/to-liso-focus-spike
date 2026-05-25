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

    fun isShowing() = overlayView != null

    fun show(
        packageName: String,
        onAnalyze: () -> Unit,
        onRelease: () -> Unit,
    ) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(tag, "Permissão SYSTEM_ALERT_WINDOW não concedida")
            return
        }

        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_LAYOUT_IN_SCREEN: ocupa toda a tela incluindo status bar
            // Sem FLAG_NOT_TOUCH_MODAL: overlay captura TODOS os toques da tela
            // Sem FLAG_NOT_FOCUSABLE: overlay captura eventos de tecla (volume, back)
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = LayoutInflater.from(context)
            .inflate(R.layout.overlay_view, null)

        overlayView?.findViewById<TextView>(R.id.tv_package)
            ?.text = packageName

        overlayView?.findViewById<Button>(R.id.btn_analyze)
            ?.setOnClickListener {
                onAnalyze()
                hide()
            }

        overlayView?.findViewById<Button>(R.id.btn_release)
            ?.setOnClickListener {
                onRelease()
                hide()
            }

        windowManager.addView(overlayView, params)
        Log.d(tag, "Overlay modal exibido para: $packageName")
    }

    fun hide() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
            Log.d(tag, "Overlay removido")
        }
    }
}