package com.lelesoftware.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.ui.theme.AmberPrimary
import com.lelesoftware.app.ui.theme.CyanAccent
import com.lelesoftware.app.ui.theme.MonoFontFamily
import com.lelesoftware.app.ui.theme.SlateCardBorder
import com.lelesoftware.app.ui.theme.SlateElevatedSurface
import com.lelesoftware.app.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.abs

data class NormalizedGridBounds(
    val uLeft: Float,
    val uRight: Float,
    val vTop: Float,
    val vBottom: Float
)

@Stable
class GridZoomPanState(
    val minScale: Float = 1.0f,
    val maxScale: Float = 5.0f
) {
    var scale by mutableFloatStateOf(1.0f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    var viewportSize by mutableStateOf(IntSize.Zero)

    val isZoomed: Boolean
        get() = scale > 1.02f

    fun clampOffset(candidate: Offset, targetScale: Float, size: IntSize): Offset {
        if (size.width <= 0 || size.height <= 0 || targetScale <= 1.005f) return Offset.Zero
        val maxX = ((size.width * targetScale - size.width) / 2f).coerceAtLeast(0f)
        val maxY = ((size.height * targetScale - size.height) / 2f).coerceAtLeast(0f)
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY)
        )
    }

    fun updateViewportSize(newSize: IntSize) {
        viewportSize = newSize
        offset = clampOffset(offset, scale, newSize)
    }

    fun applyTransform(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        size: IntSize
    ) {
        if (size.width <= 0 || size.height <= 0) return
        val oldScale = scale
        val newScale = (oldScale * zoom).coerceIn(minScale, maxScale)

        if (newScale <= 1.005f) {
            scale = 1.0f
            offset = Offset.Zero
            return
        }

        val center = Offset(size.width / 2f, size.height / 2f)
        val cFromCenter = if (centroid == Offset.Unspecified) Offset.Zero else (centroid - center)
        val ratio = newScale / oldScale
        val rawOffset = (offset - cFromCenter) * ratio + cFromCenter + pan

        scale = newScale
        offset = clampOffset(rawOffset, newScale, size)
    }

    fun zoomBy(factor: Float) {
        val size = viewportSize
        if (size.width <= 0 || size.height <= 0) {
            val next = (scale * factor).coerceIn(minScale, maxScale)
            scale = if (next <= 1.005f) 1.0f else next
            if (scale <= 1.005f) offset = Offset.Zero
            return
        }
        applyTransform(
            centroid = Offset(size.width / 2f, size.height / 2f),
            pan = Offset.Zero,
            zoom = factor,
            size = size
        )
    }

    fun reset() {
        scale = 1.0f
        offset = Offset.Zero
    }

    fun viewportToGridCell(
        tapPos: Offset,
        size: IntSize,
        gridWidth: Int,
        gridHeight: Int
    ): Pair<Int, Int> {
        val w = size.width.toFloat().coerceAtLeast(1f)
        val h = size.height.toFloat().coerceAtLeast(1f)
        val safeGridW = gridWidth.coerceAtLeast(1)
        val safeGridH = gridHeight.coerceAtLeast(1)

        val cx = w / 2f
        val cy = h / 2f
        val px = cx + (tapPos.x - cx - offset.x) / scale
        val py = cy + (tapPos.y - cy - offset.y) / scale

        val cellW = w / safeGridW.toFloat()
        val cellH = h / safeGridH.toFloat()
        val gx = (px / cellW).toInt().coerceIn(0, safeGridW - 1)
        val gy = (py / cellH).toInt().coerceIn(0, safeGridH - 1)
        return gx to gy
    }

    fun visibleNormalizedBounds(): NormalizedGridBounds {
        val w = viewportSize.width.toFloat()
        val h = viewportSize.height.toFloat()
        if (w <= 0f || h <= 0f || scale <= 1.005f) {
            return NormalizedGridBounds(0f, 1f, 0f, 1f)
        }
        val uLeft = (0.5f + (-w / 2f - offset.x) / (w * scale)).coerceIn(0f, 1f)
        val uRight = (0.5f + (w / 2f - offset.x) / (w * scale)).coerceIn(0f, 1f)
        val vTop = (0.5f + (-h / 2f - offset.y) / (h * scale)).coerceIn(0f, 1f)
        val vBottom = (0.5f + (h / 2f - offset.y) / (h * scale)).coerceIn(0f, 1f)
        return NormalizedGridBounds(uLeft, uRight, vTop, vBottom)
    }
}

@Composable
fun rememberGridZoomPanState(
    vararg keys: Any?
): GridZoomPanState {
    return remember(*keys) { GridZoomPanState() }
}

/**
 * Supports:
 * - Pinch-to-zoom (2+ fingers) around the gesture centroid without stealing single-finger page scroll at 1.0x
 * - Single-finger & two-finger pan whenever zoomed (> 1.02x), consuming drag events cleanly inside scrollable screens
 * - Instant single-tap mapped back through current scale & translation to the exact grid cell
 */
fun Modifier.geophysicalGridGestures(
    state: GridZoomPanState,
    key1: Any? = Unit,
    onTap: (tapOffset: Offset, viewportSize: IntSize) -> Unit
): Modifier = this
    .onSizeChanged { newSize ->
        state.updateViewportSize(newSize)
    }
    .pointerInput(state, key1) {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            val startPos = firstDown.position
            val downTime = System.currentTimeMillis()
            var pastTouchSlop = false
            var isMultiTouch = false
            var lastTapCandidatePos = startPos
            val touchSlop = viewConfiguration.touchSlop

            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (canceled) break

                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount >= 2) {
                    isMultiTouch = true
                }

                event.changes.firstOrNull { it.id == firstDown.id }?.let {
                    lastTapCandidatePos = it.position
                }

                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    val movedDist = (lastTapCandidatePos - startPos).getDistance()
                    if (
                        isMultiTouch ||
                        abs(1f - zoomChange) > 0.01f ||
                        (state.isZoomed && (panChange.getDistance() > touchSlop * 0.4f || movedDist > touchSlop))
                    ) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop && (isMultiTouch || state.isZoomed)) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    state.applyTransform(
                        centroid = centroid,
                        pan = panChange,
                        zoom = if (isMultiTouch) zoomChange else 1f,
                        size = size
                    )
                    event.changes.forEach { change ->
                        if (change.positionChanged()) {
                            change.consume()
                        }
                    }
                }
            } while (event.changes.any { it.pressed })

            if (!pastTouchSlop && !isMultiTouch) {
                val elapsed = System.currentTimeMillis() - downTime
                if ((lastTapCandidatePos - startPos).getDistance() <= touchSlop && elapsed < 450L) {
                    onTap(lastTapCandidatePos, size)
                }
            }
        }
    }

@Composable
fun GridZoomToolbar(
    state: GridZoomPanState,
    tagPrefix: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SlateElevatedSurface,
            border = BorderStroke(1.dp, if (state.isZoomed) CyanAccent.copy(alpha = 0.65f) else SlateCardBorder)
        ) {
            Text(
                text = String.format(Locale.US, "%.1f×", state.scale),
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (state.isZoomed) CyanAccent else Color.White,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("${tagPrefix}_zoom_level_badge")
            )
        }

        Text(
            text = if (state.isZoomed) "Kaydırmak için sürükleyin" else "İki parmakla yakınlaştırın",
            color = TextMuted,
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        if (state.isZoomed) {
            Surface(
                onClick = { state.reset() },
                shape = RoundedCornerShape(8.dp),
                color = AmberPrimary.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.7f)),
                modifier = Modifier.testTag("${tagPrefix}_zoom_reset_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Yakınlaştırmayı Sıfırla",
                        tint = AmberPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "1:1",
                        color = AmberPrimary,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(1.dp))
        }

        IconButton(
            onClick = { state.zoomBy(0.75f) },
            enabled = state.scale > state.minScale + 0.02f,
            modifier = Modifier
                .size(48.dp)
                .testTag("${tagPrefix}_zoom_out_button")
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = "Uzaklaştır",
                tint = if (state.scale > state.minScale + 0.02f) Color.White else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = { state.zoomBy(1.35f) },
            enabled = state.scale < state.maxScale - 0.02f,
            modifier = Modifier
                .size(48.dp)
                .testTag("${tagPrefix}_zoom_in_button")
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Yakınlaştır",
                tint = if (state.scale < state.maxScale - 0.02f) CyanAccent else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
