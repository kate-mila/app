package com.mipycode.v2rayclient.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * کارت شیشه‌ای (Glassmorphism) با گرادیان ملایم و حاشیه‌ی نیمه‌شفاف.
 * برای پس‌زمینه‌ی واقعاً بلور، محتوای پشت کارت باید از blur ماژول
 * androidx.compose.ui:ui-graphics (Modifier.blur در Android 12+) استفاده کند؛
 * اینجا برای سازگاری با API 24 از افکت گرادیان-آلفا استفاده شده.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(GlassSurface, GlassSurface.copy(alpha = 0.08f))
                )
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}
