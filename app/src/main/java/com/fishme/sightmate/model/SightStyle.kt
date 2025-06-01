package com.fishme.sightmate.model

import android.graphics.Color

data class SightStyle(
    val sizeDp: Int = 4,              // 大小，单位 dp
    val color: Int = Color.RED,        // ARGB 颜色
    val alpha: Float = 1.0f,           // 透明度（0.0f ~ 1.0f）
    val offsetX: Int = 0,              // X轴偏移量，单位 dp
    val offsetY: Int = 0               // Y轴偏移量，单位 dp
)
