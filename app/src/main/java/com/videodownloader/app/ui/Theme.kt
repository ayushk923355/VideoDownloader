package com.videodownloader.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Cyan = Color(0xFF22D3EE)
val Blue = Color(0xFF4F6BFF)
val Violet = Color(0xFFA855F7)
val Magenta = Color(0xFFEC4899)
val Ink = Color(0xFF070B1F)
val TextHi = Color(0xFFEEF3FF)
val TextLo = Color(0xB3EEF3FF)
val Accent = Brush.horizontalGradient(listOf(Cyan, Blue, Violet, Magenta))

@Composable
fun VDTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = Blue, background = Ink, surface = Color(0xFF171A32),
            onSurface = TextHi, onBackground = TextHi
        ) else lightColorScheme(
            primary = Blue, background = Color(0xFFF5F7FF), surface = Color.White,
            onSurface = Color(0xFF17172B), onBackground = Color(0xFF17172B),
            secondary = Violet
        ),
        content = content
    )
}

@Composable
fun Backdrop(content: @Composable BoxScope.() -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val base = if (dark) Ink else Color(0xFFF5F7FF)
    Box(Modifier.fillMaxSize().background(base)) {
        Box(Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Cyan.copy(if (dark) .28f else .20f), Color.Transparent), Offset(80f, 120f), 800f)))
        Box(Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Violet.copy(if (dark) .34f else .16f), Color.Transparent), Offset(1000f, 1100f), 900f)))
        Box(Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Magenta.copy(if (dark) .24f else .12f), Color.Transparent), Offset(100f, 2000f), 800f)))
        content()
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, glow: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth()
            .then(if (glow != null) Modifier.shadow(24.dp, shape, ambientColor = glow, spotColor = glow) else Modifier)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(.14f), Color.White.copy(.04f))))
            .border(1.dp, Color.White.copy(.2f), shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp)).background(Accent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.heightIn(min = 44.dp).clip(shape).background(Color.White.copy(.1f))
            .border(1.dp, Color.White.copy(.25f), shape).clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = TextHi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
fun Chip(text: String) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text, color = Color(0xFFA5F3FC), fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(shape).background(Cyan.copy(.16f)).border(1.dp, Cyan.copy(.4f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** fraction == null draws a moving (indeterminate) bar. */
@Composable
fun GlassProgress(fraction: Float?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    BoxWithConstraints(modifier.fillMaxWidth().height(10.dp).clip(shape).background(Color.White.copy(.12f))) {
        if (fraction != null) {
            val f by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "p")
            Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(Accent))
        } else {
            val t = rememberInfiniteTransition(label = "i")
            val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x")
            Box(Modifier.offset(x = maxWidth * (1.3f * x - 0.3f)).width(maxWidth * 0.3f).fillMaxHeight().background(Accent))
        }
    }
}

fun fmtBytes(b: Long): String =
    if (b >= 1_000_000_000) "%.2f GB".format(b / 1e9) else "%.1f MB".format(b / 1e6)

fun fmtTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
