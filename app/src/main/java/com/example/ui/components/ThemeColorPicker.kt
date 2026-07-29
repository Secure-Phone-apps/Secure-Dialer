package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun ThemeColorPicker(
    currentSelected: String,
    onColorSelected: (String) -> Unit
) {
    val options = listOf(
        Triple("classic_slate", Color(0xFF2563EB), "Royal Blue"),
        Triple("forest_green", Color(0xFF059669), "Emerald"),
        Triple("ocean_blue", Color(0xFF0284C7), "Cyan"),
        Triple("sunset_orange", Color(0xFFD97706), "Amber"),
        Triple("lavender_purple", Color(0xFF7C3AED), "Purple"),
        Triple("dark_crimson", Color(0xFFE11D48), "Ruby"),
        Triple("natural_gray", Color(0xFF525252), "Titanium")
    )
    val pickerShape = RoundedCornerShape(16.dp)

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.settings_accent_color),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (key, color, label) ->
                val isSelected = currentSelected == key
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    ),
                    label = "color_picker_scale"
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(scale)
                        .clip(pickerShape)
                        .background(color)
                        .clickable { onColorSelected(key) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
