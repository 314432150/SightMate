package com.fishme.sightmate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.fishme.sightmate.R
import com.fishme.sightmate.data.SightPreferences
import com.fishme.sightmate.event.Event
import com.fishme.sightmate.event.EventBus
import com.fishme.sightmate.model.SightStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SightOverlayService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "sight_overlay_channel"
    }

    private val binder = SightBinder()
    private lateinit var windowManager: WindowManager
    private lateinit var sightView: View
    private lateinit var sightPreferences: SightPreferences
    private var currentStyle = SightStyle()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 发送当前样式事件
    private fun postCurrentStyle() {
        serviceScope.launch {
            EventBus.emit(Event.StyleUpdated(currentStyle))
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sightPreferences = SightPreferences(this)

        // 创建通知渠道
        createNotificationChannel()

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())

        // 读取保存的位置和样式
        val (savedX, savedY) = sightPreferences.getSightPosition()
        val (savedSize, savedColor, savedAlpha) = sightPreferences.getSightStyle()

        // 创建准星，应用所有保存的设置
        addSightView(SightStyle(
            sizeDp = savedSize,
            color = savedColor,
            alpha = savedAlpha,
            offsetX = savedX,
            offsetY = savedY
        ))

        // 发送初始样式
        postCurrentStyle()

        // 监听移动事件和样式更新事件
        serviceScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is Event.Move -> updateSightPosition(event.dx, event.dy)
                    is Event.UpdateStyle -> {
                        val newStyle = currentStyle.copy(
                            sizeDp = event.size ?: currentStyle.sizeDp,
                            color = event.color ?: currentStyle.color,
                            alpha = event.alpha ?: currentStyle.alpha
                        )
                        updateSightStyle(newStyle)
                        postCurrentStyle() // 发送更新后的样式
                    }
                    is Event.RequestStyle -> postCurrentStyle() // 响应样式请求
                    else -> {} // 忽略其他事件
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "准星服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "用于保持准星显示的服务通知"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("准星服务")
            .setContentText("正在显示准星")
            .setSmallIcon(R.drawable.ic_notification)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun addSightView(style: SightStyle) {
        currentStyle = style
        val sizePx = (style.sizeDp * resources.displayMetrics.density).toInt()
        val offsetXPx = (style.offsetX * resources.displayMetrics.density).toInt()
        val offsetYPx = (style.offsetY * resources.displayMetrics.density).toInt()

        val view = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(style.color)
            }
            alpha = style.alpha
            setLayerType(View.LAYER_TYPE_HARDWARE, null)  // 启用硬件加速
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,  // 添加硬件加速标志
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = offsetXPx
            y = offsetYPx
        }

        windowManager.addView(view, params)
        sightView = view
    }

    fun updateSightPosition(offsetX: Int, offsetY: Int) {
        if (!::sightView.isInitialized) return

        currentStyle = currentStyle.copy(
            offsetX = currentStyle.offsetX + offsetX,
            offsetY = currentStyle.offsetY + offsetY
        )

        val layoutParams = sightView.layoutParams as WindowManager.LayoutParams
        layoutParams.x = (currentStyle.offsetX * resources.displayMetrics.density).toInt()
        layoutParams.y = (currentStyle.offsetY * resources.displayMetrics.density).toInt()

        try {
            windowManager.updateViewLayout(sightView, layoutParams)
            // 保存新的位置
            sightPreferences.saveSightPosition(currentStyle.offsetX, currentStyle.offsetY)
            Log.d("SightOverlayService", "✅ 准星位置已更新并保存: x=${layoutParams.x}px, y=${layoutParams.y}px")
        } catch (e: Exception) {
            Log.e("SightOverlayService", "更新准星位置失败", e)
        }
    }

    fun updateSightStyle(style: SightStyle) {
        if (!::sightView.isInitialized) return

        currentStyle = style
        val sizePx = (style.sizeDp * resources.displayMetrics.density).toInt()

        val view = sightView
        view.background = (view.background as? GradientDrawable)?.apply {
            setColor(style.color)
        }
        view.alpha = style.alpha

        val params = view.layoutParams as WindowManager.LayoutParams
        params.width = sizePx
        params.height = sizePx

        try {
            windowManager.updateViewLayout(view, params)
            // 保存新的样式
            sightPreferences.saveSightStyle(style.sizeDp, style.color, style.alpha)
            Log.d("SightOverlayService", "✅ 准星样式已更新并保存")
        } catch (e: Exception) {
            Log.e("SightOverlayService", "更新准星样式失败", e)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (::sightView.isInitialized) {
            windowManager.removeView(sightView)
        }
    }

    inner class SightBinder : Binder() {
        fun getService(): SightOverlayService = this@SightOverlayService
    }
}
