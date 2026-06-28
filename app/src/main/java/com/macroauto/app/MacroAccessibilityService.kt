package com.macroauto.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * Performs the recorded taps by dispatching gestures. No root required.
 * Holds a static instance so the overlay can trigger playback.
 */
class MacroAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var playing = false

    companion object {
        @Volatile var instance: MacroAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun stop() { playing = false }

    /** Play steps [repeat] times (0 = until stopped). */
    fun play(steps: List<MacroStep>, repeat: Int, onFinished: () -> Unit) {
        if (steps.isEmpty()) { onFinished(); return }
        playing = true
        var loop = 0
        fun runLoop() {
            if (!playing) { onFinished(); return }
            runSteps(steps, 0) {
                loop++
                if (!playing || (repeat > 0 && loop >= repeat)) { playing = false; onFinished() }
                else handler.postDelayed({ runLoop() }, 300)
            }
        }
        runLoop()
    }

    private fun runSteps(steps: List<MacroStep>, index: Int, done: () -> Unit) {
        if (!playing || index >= steps.size) { done(); return }
        val s = steps[index]
        handler.postDelayed({
            if (!playing) { done(); return@postDelayed }
            tap(s.x, s.y) { runSteps(steps, index + 1, done) }
        }, s.delayMs.coerceAtLeast(0))
    }

    private fun tap(x: Float, y: Float, after: () -> Unit) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { after() }
            override fun onCancelled(d: GestureDescription?) { after() }
        }, handler)
    }
}
