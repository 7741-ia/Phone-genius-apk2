package com.example.phonegenius.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonegenius.data.model.DataSource
import com.example.phonegenius.data.model.Severity
import com.example.phonegenius.ui.theme.*

fun severityColor(sev: Severity): Color = when (sev) {
    Severity.OK -> SeverityOk
    Severity.LOW -> SeverityLow
    Severity.MEDIUM -> SeverityMedium
    Severity.HIGH -> SeverityHigh
    Severity.CRITICAL -> SeverityCritical
}

@Composable
fun SeverityBadge(
    severity: Severity,
    modifier: Modifier = Modifier
) {
    val color = severityColor(severity)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.testTag("badge_severity_${severity.name.lowercase()}")
    ) {
        Text(
            text = severity.labelFr,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SourceBadge(
    source: DataSource,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (source) {
        DataSource.DEMO -> "FICTIF" to SeverityMedium
        DataSource.WEB -> "WEB" to SeverityLow
        DataSource.NATIVE -> "RÉEL" to SeverityOk
        DataSource.UNAVAILABLE -> "INDISPONIBLE" to TextMuted
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.testTag("badge_source_${source.name.lowercase()}")
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CircularGauge(
    value: Float?,
    max: Float,
    label: String,
    display: String,
    severity: Severity,
    modifier: Modifier = Modifier
) {
    val progress = if (value != null && max > 0f) {
        (value / max).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "gauge_progress"
    )

    val color = if (value == null) TextMuted else severityColor(severity)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(8.dp)
            .testTag("gauge_${label.lowercase().replace(" ", "_")}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(76.dp)
        ) {
            Canvas(modifier = Modifier.size(70.dp)) {
                // Background Track
                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
                // Active Arc
                if (value != null) {
                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = display,
                style = MaterialTheme.typography.titleMedium,
                color = if (value == null) TextMuted else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
