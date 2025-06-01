package com.fishme.sightmate

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.fishme.sightmate.event.Event
import com.fishme.sightmate.event.EventBus
import com.fishme.sightmate.permission.*
import com.fishme.sightmate.service.ServiceManager
import com.fishme.sightmate.ui.page.HomePage
import com.fishme.sightmate.ui.theme.SightMateTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主活动类
 * 负责管理UI状态和用户交互
 */
class MainActivity : ComponentActivity() {

    // region 状态变量
    /** 悬浮窗权限状态 */
    private lateinit var overlayPermissionState: MutableState<Boolean>
    /** 准星显示状态 */
    private lateinit var showSightState: MutableState<Boolean>
    /** 方向控制显示状态 */
    private lateinit var directionControlState: MutableState<Boolean>
    // endregion

    // region 服务管理
    private lateinit var serviceManager: ServiceManager
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化状态
        initializeStates()
        // 初始化服务管理器
        serviceManager = ServiceManager(
            context = this,
            lifecycleScope = lifecycleScope,
            showSightState = showSightState,
            directionControlState = directionControlState
        )
        // 设置事件监听
        setupEventListener()
        // 设置UI
        setupUI()
    }

    /**
     * 初始化状态变量
     */
    private fun initializeStates() {
        overlayPermissionState = mutableStateOf(hasOverlayPermission(this))
        showSightState = mutableStateOf(false)
        directionControlState = mutableStateOf(false)
    }

    /**
     * 设置事件监听器
     */
    private fun setupEventListener() {
        lifecycleScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is Event.ControlClosed -> {
                        withContext(Dispatchers.Main) {
                            directionControlState.value = false
                            // 重置服务状态
                            serviceManager.resetDirectionControlState()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 设置UI界面
     */
    private fun setupUI() {
        setContent {
            SightMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomePage(
                            hasOverlayPermission = overlayPermissionState.value,
                            showSight = showSightState.value,
                            directionControlShowing = directionControlState.value,
                            onRequestOverlayPermission = {
                                startActivity(buildOverlayPermissionIntent(packageName))
                            },
                            onToggleSight = { show ->
                                showSightState.value = show
                                if (show) {
                                    serviceManager.startSightService()
                                } else {
                                    if (directionControlState.value) {
                                        serviceManager.stopDirectionControl()
                                        directionControlState.value = false
                                    }
                                    serviceManager.stopSightService()
                                }
                            },
                            onToggleDirectionControl = { show ->
                                if (show) {
                                    serviceManager.showDirectionControl()
                                    directionControlState.value = true
                                } else {
                                    serviceManager.stopDirectionControl()
                                    directionControlState.value = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionChange()
    }

    /**
     * 检查权限状态变化并更新UI
     */
    private fun checkPermissionChange() {
        val currentPermissionState = hasOverlayPermission(this)
        if (currentPermissionState && !overlayPermissionState.value) {
            overlayPermissionState.value = true
            Toast.makeText(
                this,
                "权限已开启，现在您可以使用准星功能了",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            overlayPermissionState.value = currentPermissionState
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceManager.cleanup()
    }
}
