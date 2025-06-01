package com.fishme.sightmate.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color

class SightPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val screenHeight = Resources.getSystem().displayMetrics.heightPixels

    fun saveSightPosition(offsetX: Int, offsetY: Int) {
        prefs.edit()
            .putInt(KEY_OFFSET_X, offsetX)
            .putInt(KEY_OFFSET_Y, offsetY)
            .apply()
    }

    fun getSightPosition(): Pair<Int, Int> {
        val offsetX = prefs.getInt(KEY_OFFSET_X, 0)
        val offsetY = prefs.getInt(KEY_OFFSET_Y, 0)
        return Pair(offsetX, offsetY)
    }

    fun saveSightStyle(sizeDp: Int, color: Int, alpha: Float) {
        prefs.edit()
            .putInt(KEY_SIZE, sizeDp)
            .putInt(KEY_COLOR, color)
            .putFloat(KEY_ALPHA, alpha)
            .apply()
    }

    fun getSightStyle(): Triple<Int, Int, Float> {
        val size = prefs.getInt(KEY_SIZE, 4)  // 默认4dp
        val color = prefs.getInt(KEY_COLOR, Color.RED)  // 默认红色
        val alpha = prefs.getFloat(KEY_ALPHA, 1.0f)  // 默认不透明
        return Triple(size, color, alpha)
    }

    fun saveControlPosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_CONTROL_X, x)
            .putInt(KEY_CONTROL_Y, y)
            .apply()
    }

    fun getControlPosition(): Pair<Int, Int> {
        // 默认位置在屏幕底部中间
        val defaultX = screenWidth / 2     // 水平居中
        val defaultY = screenHeight - 380  // 考虑控制器高度（约200px）+ 底部安全间距（180px）
        val x = prefs.getInt(KEY_CONTROL_X, defaultX)
        val y = prefs.getInt(KEY_CONTROL_Y, defaultY)
        return Pair(x, y)
    }

    companion object {
        private const val PREFS_NAME = "sight_preferences"
        private const val KEY_OFFSET_X = "offset_x"
        private const val KEY_OFFSET_Y = "offset_y"
        private const val KEY_CONTROL_X = "control_x"
        private const val KEY_CONTROL_Y = "control_y"
        private const val KEY_SIZE = "sight_size"
        private const val KEY_COLOR = "sight_color"
        private const val KEY_ALPHA = "sight_alpha"
    }
}
