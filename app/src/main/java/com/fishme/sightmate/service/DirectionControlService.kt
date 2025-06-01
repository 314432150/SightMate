package com.fishme.sightmate.service

import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import com.fishme.sightmate.R
import com.fishme.sightmate.event.Event
import com.fishme.sightmate.event.EventBus
import kotlinx.coroutines.*

class DirectionControlService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var controlView: View
    private var isViewAttached = false

    // 长按相关变量
    private var moveJob: Job? = null
    private val movementDelay = 50L // 移动间隔，毫秒

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupControlView()
    }

    private fun setupControlView() {
        controlView = LayoutInflater.from(this).inflate(R.layout.layout_direction_control, null)

        // 启用硬件加速
        controlView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // 先测量view的尺寸
        controlView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val viewHeight = controlView.measuredHeight
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val navigationBarHeight = getNavigationBarHeight()
        val bottomSpacing = navigationBarHeight + (60 * resources.displayMetrics.density).toInt() // 调整为 60dp，适配新导航栏

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,  // 添加硬件加速标志
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            y = screenHeight / 2 - viewHeight / 2 - bottomSpacing // 从屏幕中心减去控制器高度的一半，再减去底部间距
        }

        // 为方向按钮设置触摸事件
        setupDirectionButton(R.id.btnUp) { 0 to -1 }
        setupDirectionButton(R.id.btnDown) { 0 to 1 }
        setupDirectionButton(R.id.btnLeft) { -1 to 0 }
        setupDirectionButton(R.id.btnRight) { 1 to 0 }

        // 修改关闭按钮的设置方式
        val closeButton = controlView.findViewById<ImageButton>(R.id.btnClose).apply {
            // 移除 setOnClickListener，改为在 onTouch 中处理点击
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(0.85f)
                            .scaleY(0.85f)
                            .alpha(0.7f)
                            .setDuration(50)
                            .start()
                        v.isPressed = true
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(0.6f)
                            .setDuration(100)
                            .start()
                        v.isPressed = false
                        // 处理点击事件
                        closeControlView()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(0.6f)
                            .setDuration(100)
                            .start()
                        v.isPressed = false
                        true
                    }
                    else -> false
                }
            }
        }

        try {
            windowManager.addView(controlView, params)
            isViewAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun getNavigationBarHeight(): Int {
        val resources = Resources.getSystem()
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun setupDirectionButton(buttonId: Int, getOffset: () -> Pair<Int, Int>) {
        val button = controlView.findViewById<ImageButton>(buttonId)
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startContinuousMovement(getOffset)
                    v.animate()
                        .scaleX(0.85f)
                        .scaleY(0.85f)
                        .setDuration(50)
                        .start()
                    v.isPressed = true
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopContinuousMovement()
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    v.isPressed = false
                    true
                }
                else -> false
            }
        }
    }

    private fun startContinuousMovement(getOffset: () -> Pair<Int, Int>) {
        moveJob = serviceScope.launch {
            while (isActive) {
                val (dx, dy) = getOffset()
                EventBus.post(Event.Move(dx, dy))
                delay(movementDelay)
            }
        }
    }

    private fun stopContinuousMovement() {
        moveJob?.cancel()
        moveJob = null
    }

    private fun closeControlView() {
        stopContinuousMovement()
        if (isViewAttached && ::controlView.isInitialized) {
            try {
                // 先发送事件
                EventBus.post(Event.ControlClosed)
                // 等待短暂延迟确保事件被处理
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (isViewAttached) {
                            windowManager.removeView(controlView)
                            isViewAttached = false
                        }
                        stopSelf()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 100)
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopContinuousMovement()
        try {
            // 确保发送关闭事件
            EventBus.post(Event.ControlClosed)
            if (isViewAttached && ::controlView.isInitialized) {
                windowManager.removeView(controlView)
                isViewAttached = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
