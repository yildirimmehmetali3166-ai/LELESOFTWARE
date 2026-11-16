package com.lelesoftware.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.R
import com.lelesoftware.app.ui.theme.AmberPrimary
import com.lelesoftware.app.ui.theme.CyanAccent
import com.lelesoftware.app.ui.theme.DisplayFontFamily
import com.lelesoftware.app.ui.theme.PlatinumRim
import com.lelesoftware.app.ui.theme.SlateDarkBackground
import kotlinx.coroutines.delay

/**
 * Minimal, animated corporate splash screen showcasing the animated `lele3dxx` artwork
 * and `LELE SOFTWARE` medallion emblem while keeping layout airy and focused.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isVisible by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_motion")

    // Smooth Ken Burns zoom & subtle drift for lele3dxx visual
    val bannerScale by infiniteTransition.animateFloat(
        initialValue = 1.02f,
        targetValue = 1.11f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_scale"
    )
    val bannerShiftX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_shift_x"
    )
    val emblemFloatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem_float_y"
    )
    val sweepFraction by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_fraction"
    )

    LaunchedEffect(Unit) {
        isVisible = true
        val totalMs = 2600L
        val tickMs = 50L
        val steps = (totalMs / tickMs).toInt()
        for (i in 1..steps) {
            progress = i.toFloat() / steps.toFloat()
            delay(tickMs)
        }
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
            .testTag("app_video_splash_screen")
    ) {
        // 1. Subtle animated cosmic background derived from lele3dxx artwork
        Image(
            painter = painterResource(id = R.drawable.lele3dxx_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bannerScale * 1.06f
                    scaleY = bannerScale * 1.06f
                    translationX = bannerShiftX * 1.4f
                    alpha = 0.24f
                }
        )

        // Executive dark vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SlateDarkBackground.copy(alpha = 0.88f),
                            SlateDarkBackground.copy(alpha = 0.62f),
                            SlateDarkBackground.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // 2. Clean centered corporate hierarchy
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top minimal corporate brand strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC0B1221),
                    border = BorderStroke(1.dp, PlatinumRim.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_lele_software),
                            contentDescription = "LELE SOFTWARE Simge",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "LELE SOFTWARE • KURUMSAL SÜRÜM",
                            style = MaterialTheme.typography.labelSmall,
                            color = PlatinumRim,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Centerpiece: Animated lele3dxx Showcase Frame + Floating Medallion
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(650)) + slideInVertically(tween(650)) { it / 10 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(22.dp))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        PlatinumRim.copy(alpha = 0.65f),
                                        CyanAccent.copy(alpha = 0.45f),
                                        AmberPrimary.copy(alpha = 0.65f)
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.lele3dxx_banner),
                            contentDescription = "LELE SOFTWARE Hareketli Görsel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = bannerScale
                                    scaleY = bannerScale
                                    translationX = bannerShiftX
                                    translationY = emblemFloatY * 0.6f
                                }
                        )

                        // Animated light sweep line across showcase banner
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sweepX = size.width * sweepFraction
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        CyanAccent.copy(alpha = 0.28f),
                                        PlatinumRim.copy(alpha = 0.36f),
                                        Color.Transparent
                                    ),
                                    startX = sweepX - 90f,
                                    endX = sweepX + 90f
                                ),
                                start = Offset(sweepX, 0f),
                                end = Offset(sweepX - size.width * 0.12f, size.height),
                                strokeWidth = 90f
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "LELE SOFTWARE",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 3.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "3D & 2D Jeofizik Yeraltı Görüntüleme Platformu",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PlatinumRim,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Minimal Progress + Single Enter Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = AmberPrimary,
                    trackColor = Color(0xFF1E293B)
                )

                Button(
                    onClick = onSplashFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("skip_video_splash_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = SlateDarkBackground
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "SİSTEME GİR",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
