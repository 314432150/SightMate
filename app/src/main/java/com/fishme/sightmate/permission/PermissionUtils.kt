package com.fishme.sightmate.permission

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * 检查是否已授予悬浮窗权限
 *
 * @param context 上下文，用于调用系统API
 * @return 如果允许悬浮窗，则返回 true，否则 false
 */
fun hasOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

/**
 * 构建跳转到系统悬浮窗权限设置页面的 Intent
 *
 * @param packageName 当前应用的包名
 * @return Intent，用于 startActivity 调用
 */
fun buildOverlayPermissionIntent(packageName: String): Intent {
    return Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:$packageName".toUri()
    )
}


