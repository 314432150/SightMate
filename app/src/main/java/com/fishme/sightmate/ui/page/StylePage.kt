package com.fishme.sightmate.ui.page

import android.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.fishme.sightmate.event.Event
import com.fishme.sightmate.event.EventBus
import com.fishme.sightmate.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylePage(
    sightSize: Int,
    sightColor: Int,
    sightAlpha: Float,
    onSizeChange: (Int) -> Unit,
    onColorChange: (Int) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 大小设置
        StyleSection(title = "大小") {
            Column {
                Text(
                    text = "${sightSize}dp",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Slider(
                    value = sightSize.toFloat(),
                    onValueChange = {
                        onSizeChange(it.toInt())
                        EventBus.post(Event.UpdateStyle(size = it.toInt()))
                    },
                    valueRange = 2f..12f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 颜色选择
        StyleSection(title = "颜色") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val colors = listOf(
                    Color.RED,
                    Color.GREEN,
                    Color.BLUE,
                    Color.YELLOW,
                    Color.WHITE,
                    Color.CYAN
                )
                colors.forEach { color ->
                    val isSelected = color == sightColor
                    val scale by animateFloatAsState(if (isSelected) 1.2f else 1f)

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(color))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable {
                                onColorChange(color)
                                EventBus.post(Event.UpdateStyle(color = color))
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 可见度设置
        StyleSection(title = "可见度") {
            Column {
                Text(
                    text = "${(sightAlpha * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Slider(
                    value = sightAlpha,
                    onValueChange = {
                        onAlphaChange(it)
                        EventBus.post(Event.UpdateStyle(alpha = it))
                    },
                    valueRange = 0.2f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StyleSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
