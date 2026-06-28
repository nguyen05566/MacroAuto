package com.macroauto.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var controlBar: View? = null
    private var captureView: View? = null

    private var recording = false
    private val recorded = mutableListOf<MacroStep>()
    private var lastTapTime = 0L

    private lateinit var btnRecord: Button
    private lateinit var btnPlay: Button
    private lateinit var btnStop: Button

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundCompat()
        showControlBar()
    }

    private fun startForegroundCompat() {
        val channelId = "macro_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(channelId, "MacroAuto", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notif: Notification = Notification.Builder(this, channelId)
            .setContentTitle("MacroAuto đang chạy")
            .setContentText("Điều khiển macro qua nút nổi")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        startForeground(1, notif)
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun showControlBar() {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xCC222222.toInt())
            setPadding(12, 12, 12, 12)
        }
        btnRecord = Button(this).apply { text = "● REC"; setOnClickListener { toggleRecord() } }
        btnPlay = Button(this).apply { text = "▶ PLAY"; setOnClickListener { playMacro() } }
        btnStop = Button(this).apply { text = "■ STOP"; setOnClickListener { stopAll() } }
        bar.addView(btnRecord); bar.addView(btnPlay); bar.addView(btnStop)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 200 }

        // make the bar draggable
        bar.setOnTouchListener(object : View.OnTouchListener {
            var ix = 0; var iy = 0; var tx = 0f; var ty = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { ix = lp.x; iy = lp.y; tx = e.rawX; ty = e.rawY }
                    MotionEvent.ACTION_MOVE -> {
                        lp.x = ix + (e.rawX - tx).toInt(); lp.y = iy + (e.rawY - ty).toInt()
                        wm.updateViewLayout(bar, lp)
                    }
                }
                return false
            }
        })
        controlBar = bar
        wm.addView(bar, lp)
    }

    private fun toggleRecord() {
        if (!recording) startRecording() else stopRecording()
    }

    private fun startRecording() {
        recording = true
        recorded.clear()
        lastTapTime = 0L
        btnRecord.text = "● REC..."
        Toast.makeText(this, "Bắt đầu ghi: chạm vào màn hình để lưu điểm", Toast.LENGTH_SHORT).show()

        val cap = View(this).apply {
            setBackgroundColor(0x33FF0000)
            setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_DOWN) {
                    val now = SystemClock.uptimeMillis()
                    val delay = if (lastTapTime == 0L) 500L else now - lastTapTime
                    lastTapTime = now
                    recorded.add(MacroStep(e.rawX, e.rawY, delay))
                }
                true // consume so taps don't reach apps underneath while recording
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        captureView = cap
        wm.addView(cap, lp)
    }

    private fun stopRecording() {
        recording = false
        btnRecord.text = "● REC"
        captureView?.let { wm.removeView(it) }
        captureView = null
        MacroStore.save(this, recorded)
        Toast.makeText(this, "Đã lưu ${recorded.size} điểm", Toast.LENGTH_SHORT).show()
    }

    private fun playMacro() {
        val svc = MacroAccessibilityService.instance
        if (svc == null) {
            Toast.makeText(this, "Hãy bật Accessibility cho MacroAuto trước", Toast.LENGTH_LONG).show()
            return
        }
        val steps = MacroStore.load(this)
        if (steps.isEmpty()) {
            Toast.makeText(this, "Chưa có macro nào. Ghi trước đã.", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Phát macro (${steps.size} điểm) x5", Toast.LENGTH_SHORT).show()
        svc.play(steps, repeat = 5) {
            // playback finished
        }
    }

    private fun stopAll() {
        if (recording) stopRecording()
        MacroAccessibilityService.instance?.stop()
        Toast.makeText(this, "Đã dừng", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        try { controlBar?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { captureView?.let { wm.removeView(it) } } catch (_: Exception) {}
        super.onDestroy()
    }
}
