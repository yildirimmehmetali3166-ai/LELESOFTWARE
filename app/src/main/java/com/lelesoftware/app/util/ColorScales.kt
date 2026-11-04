package com.lelesoftware.app.util

import androidx.compose.ui.graphics.Color
import com.lelesoftware.app.model.PaletteType
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

object ColorScales {

    fun getPaletteColor(value: Float, palette: PaletteType): Color {
        // Normalize value [-100, 100] to [0f, 1f]
        val t = ((value + 100f) / 200f).coerceIn(0f, 1f)

        return when (palette) {
            PaletteType.LELE_SPECTRUM -> {
                // Purple/Royal Blue (Void -100) -> Cyan -> Green (Ground 0) -> Yellow/Orange -> Radiant Red/Gold (+100)
                when {
                    t < 0.2f -> {
                        val sub = t / 0.2f
                        Color(
                            red = (0.12f + sub * 0.04f).coerceIn(0f, 1f),
                            green = (0.08f + sub * 0.24f).coerceIn(0f, 1f),
                            blue = (0.55f + sub * 0.45f).coerceIn(0f, 1f)
                        )
                    }
                    t < 0.4f -> {
                        val sub = (t - 0.2f) / 0.2f
                        Color(
                            red = (0.16f - sub * 0.16f).coerceIn(0f, 1f),
                            green = (0.32f + sub * 0.55f).coerceIn(0f, 1f),
                            blue = (1.0f - sub * 0.16f).coerceIn(0f, 1f)
                        )
                    }
                    t < 0.6f -> {
                        val sub = (t - 0.4f) / 0.2f
                        Color(
                            red = (0.0f + sub * 0.24f).coerceIn(0f, 1f),
                            green = (0.87f - sub * 0.08f).coerceIn(0f, 1f),
                            blue = (0.84f - sub * 0.76f).coerceIn(0f, 1f)
                        )
                    }
                    t < 0.8f -> {
                        val sub = (t - 0.6f) / 0.2f
                        Color(
                            red = (0.24f + sub * 0.74f).coerceIn(0f, 1f),
                            green = (0.79f - sub * 0.16f).coerceIn(0f, 1f),
                            blue = (0.08f - sub * 0.08f).coerceIn(0f, 1f)
                        )
                    }
                    else -> {
                        val sub = (t - 0.8f) / 0.2f
                        Color(
                            red = 1.0f,
                            green = (0.63f - sub * 0.51f).coerceIn(0f, 1f),
                            blue = (sub * 0.12f).coerceIn(0f, 1f)
                        )
                    }
                }
            }

            PaletteType.THERMAL_JET -> {
                // Classic Jet: Blue -> Cyan -> Green -> Yellow -> Red
                when {
                    t < 0.25f -> {
                        val sub = t / 0.25f
                        Color(0f, sub, 1f)
                    }
                    t < 0.5f -> {
                        val sub = (t - 0.25f) / 0.25f
                        Color(0f, 1f, 1f - sub)
                    }
                    t < 0.75f -> {
                        val sub = (t - 0.5f) / 0.25f
                        Color(sub, 1f, 0f)
                    }
                    else -> {
                        val sub = (t - 0.75f) / 0.25f
                        Color(1f, 1f - sub, 0f)
                    }
                }
            }

            PaletteType.MAGMA -> {
                val r = (1.18f * t.pow(1.2f)).coerceIn(0f, 1f)
                val g = (1.05f * t.pow(2.2f)).coerceIn(0f, 1f)
                val b = (0.47f + 0.53f * sin(t * PI.toFloat())).coerceIn(0f, 1f)
                Color(r, g, b)
            }

            PaletteType.EMERALD_VOID -> {
                when {
                    t < 0.4f -> {
                        val sub = t / 0.4f
                        Color(
                            red = (0.39f * (1f - sub)).coerceIn(0f, 1f),
                            green = (0.12f * (1f - sub)).coerceIn(0f, 1f),
                            blue = (0.94f * (1f - sub * 0.4f)).coerceIn(0f, 1f)
                        )
                    }
                    t < 0.7f -> {
                        val sub = (t - 0.4f) / 0.3f
                        Color(
                            red = (0.08f + sub * 0.12f).coerceIn(0f, 1f),
                            green = (0.47f + sub * 0.24f).coerceIn(0f, 1f),
                            blue = (0.24f - sub * 0.12f).coerceIn(0f, 1f)
                        )
                    }
                    else -> {
                        val sub = (t - 0.7f) / 0.3f
                        Color(
                            red = (0.20f + sub * 0.80f).coerceIn(0f, 1f),
                            green = (0.71f + sub * 0.12f).coerceIn(0f, 1f),
                            blue = (0.12f + sub * 0.08f).coerceIn(0f, 1f)
                        )
                    }
                }
            }

            PaletteType.GOLD_RUSH -> {
                if (t < 0.5f) {
                    val sub = t / 0.5f
                    Color(
                        red = (0.08f + sub * 0.70f).coerceIn(0f, 1f),
                        green = (0.10f + sub * 0.55f).coerceIn(0f, 1f),
                        blue = (0.14f - sub * 0.08f).coerceIn(0f, 1f)
                    )
                } else {
                    val sub = (t - 0.5f) / 0.5f
                    Color(
                        red = (0.78f + sub * 0.22f).coerceIn(0f, 1f),
                        green = (0.65f + sub * 0.29f).coerceIn(0f, 1f),
                        blue = (0.06f + sub * 0.78f).coerceIn(0f, 1f)
                    )
                }
            }
        }
    }
}
