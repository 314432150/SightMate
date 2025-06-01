package com.fishme.sightmate.ui.page

import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishme.sightmate.R
import com.fishme.sightmate.event.Event
import com.fishme.sightmate.event.EventBus
import com.fishme.sightmate.ui.component.BottomNavigation
import com.fishme.sightmate.ui.component.SwitchCard

@Composable
fun HomePage(
    hasOverlayPermission: Boolean,
    showSight: Boolean,
    directionControlShowing: Boolean = false,
    onRequestOverlayPermission: (Boolean) -> Unit,
    onToggleSight: (Boolean) -> Unit,
    onToggleDirectionControl: (Boolean) -> Unit = {}
) {
    var sightSize by remember { mutableStateOf(4) }
    var sightColor by remember { mutableStateOf(Color.RED) }
    var sightAlpha by remember { mutableStateOf(1f) }
    var currentPage by remember { mutableStateOf(0) }

    LaunchedEffect(showSight) {
        EventBus.events.collect { event ->
            when (event) {
                is Event.StyleUpdated -> {
                    sightSize = event.style.sizeDp
                    sightColor = event.style.color
                    sightAlpha = event.style.alpha
                }
                else -> {} // 忽略其他事件
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(
                    top = 24.dp,
                    bottom = if (hasOverlayPermission && showSight) 52.dp else 24.dp // 导航栏高度 + 内边距
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "🎯 准星助手",
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (hasOverlayPermission) {
                when (currentPage) {
                    0 -> {
                        // 基础设置页
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SwitchCard(
                                title = "准星显示",
                                checked = showSight,
                                onCheckedChange = onToggleSight,
                                painter = painterResource(id = R.drawable.ic_sight)
                            )

                            if (showSight) {
                                SwitchCard(
                                    title = "位置调节",
                                    checked = directionControlShowing,
                                    onCheckedChange = onToggleDirectionControl,
                                    painter = painterResource(id = R.drawable.ic_direction)
                                )
                            }
                        }
                    }
                    1 -> {
                        if (showSight) {
                            StylePage(
                                sightSize = sightSize,
                                sightColor = sightColor,
                                sightAlpha = sightAlpha,
                                onSizeChange = { sightSize = it },
                                onColorChange = { sightColor = it },
                                onAlphaChange = { sightAlpha = it }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "请先开启准星显示",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "需要开启悬浮窗权限以显示准星",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Button(
                            onClick = { onRequestOverlayPermission(true) },
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text("去开启悬浮窗权限")
                        }
                        
                        Text(
                            text = "1. 点击上方按钮打开设置\n2. 找到准星助手\n3. 打开开关",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // 底部导航栏
        if (hasOverlayPermission && showSight) {
            BottomNavigation(
                currentPage = currentPage,
                onPageChange = { currentPage = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
