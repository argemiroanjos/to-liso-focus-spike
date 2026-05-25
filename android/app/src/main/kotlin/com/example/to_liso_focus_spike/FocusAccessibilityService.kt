package com.example.to_liso_focus_spike

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {

    private val tag = "FocusService"

    private lateinit var overlayManager: OverlayManager

    private val monitoredPackages = setOf(
        "com.google.android.calculator"
    )

    private var lastForegroundPackage: String? = null

    override fun onServiceConnected() {

        overlayManager = OverlayManager(this)

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }

        Log.d(tag, "Serviço conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val pkg = event.packageName?.toString() ?: return

        if (pkg == lastForegroundPackage) return

        lastForegroundPackage = pkg

        Log.d(tag, "Foreground detectado: $pkg")

        if (pkg in monitoredPackages) {
            overlayManager.show(pkg)
        } else {
            overlayManager.hide()
        }
    }

    override fun onInterrupt() {
        overlayManager.hide()
    }
}