package com.macroauto.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MacroAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var playing = false
    @Volatile private var detectChromeMode = false
    @Volatile private var chromeClickedSuccessfully = false

    companion object {
        @Volatile var instance: MacroAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (detectChromeMode) {
            runDetectionCore()
        }
    }

    private fun runDetectionCore() {
        if (chromeClickedSuccessfully) return
        val rootNode = rootInActiveWindow ?: return
        findAndClickChrome(rootNode)
    }

    private fun findAndClickChrome(node: AccessibilityNodeInfo) {
        if (chromeClickedSuccessfully) return

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val containsChrome = text.contains("Chrome", ignoreCase = true) || 
                             desc.contains("Chrome", ignoreCase = true)

        if (containsChrome) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            tap(rect.centerX().toFloat(), rect.centerY().toFloat()) {
                chromeClickedSuccessfully = true
                detectChromeMode = false
            }
            return
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findAndClickChrome(child)
            child.recycle()
        }
    }

    fun startChromeDetection() {
        chromeClickedSuccessfully = false
        detectChromeMode = true
    }

    override fun onInterrupt() { stop() }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun stop() { 
        playing = false 
        detectChromeMode = false
    }

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

    private fun tap(x: Float, y: Float, after: () -> Unit = {}) {
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