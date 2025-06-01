package com.fishme.sightmate.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LifecycleCoroutineScope
import com.fishme.sightmate.event.Event
import com.fishme.sightmate.event.EventBus
import kotlinx.coroutines.launch

/**
 * 服务管理器
 * 负责管理准星服务和方向控制服务的生命周期
 */
class ServiceManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val showSightState: MutableState<Boolean>,
    private val directionControlState: MutableState<Boolean>
) {
    private var sightService: SightOverlayService? = null
    private var directionControlActive = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            sightService = (service as SightOverlayService.SightBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sightService = null
        }
    }

    private val serviceIntent by lazy { Intent(context, SightOverlayService::class.java) }

    /**
     * 启动准星服务
     */
    fun startSightService() {
        context.startService(serviceIntent)
        context.bindService(
            Intent(context, SightOverlayService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        lifecycleScope.launch {
            EventBus.emit(Event.RequestStyle)
        }
    }

    /**
     * 停止准星服务
     */
    fun stopSightService() {
        if (directionControlActive) {
            stopDirectionControl()
        }
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        context.stopService(serviceIntent)
    }

    /**
     * 显示方向控制
     */
    fun showDirectionControl() {
        if (!directionControlActive && sightService != null) {
            directionControlActive = true
            directionControlState.value = true
            context.startService(Intent(context, DirectionControlService::class.java))
        }
    }

    /**
     * 停止方向控制
     */
    fun stopDirectionControl() {
        if (directionControlActive) {
            val directionServiceIntent = Intent(context, DirectionControlService::class.java)
            context.stopService(directionServiceIntent)
            directionControlActive = false
            directionControlState.value = false
        }
    }

    /**
     * 重置方向控制服务的状态
     * 在服务被关闭时调用，确保状态同步
     */
    fun resetDirectionControlState() {
        directionControlActive = false
    }

    /**
     * 清理服务资源
     */
    fun cleanup() {
        stopDirectionControl()
        if (showSightState.value) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
