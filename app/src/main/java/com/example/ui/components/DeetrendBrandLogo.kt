package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeetrendOrange
import com.example.ui.theme.DeetrendTeal
import com.example.ui.theme.DeetrendTealDark
import com.example.ui.theme.DeetrendTextPrimary
import com.example.ui.theme.DeetrendTextSecondary

/**
 * Official Brand Emblem for Deetrend Global Enterprise.
 * Faithful geometric recreation of the brand mark:
 * A rich turquoise disc with the upward-surging white chevron arrow.
 */
@Composable
fun DeetrendBrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    discColor: Color = DeetrendTeal,
    arrowColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = discColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val w = this.size.width
            val h = this.size.height

            // Draw the upward-right diagonal chevron and arrowhead
            val chevronPath = Path().apply {
                // Left wing base
                moveTo(w * 0.18f, h * 0.42f)
                // Down to bottom center-left crook
                lineTo(w * 0.46f, h * 0.72f)
                // Up to arrowhead peak (top-right)
                lineTo(w * 0.88f, h * 0.16f)
                // Arrowhead right wing
                lineTo(w * 0.88f, h * 0.48f)
                lineTo(w * 0.76f, h * 0.38f)
                // Back through thickness
                lineTo(w * 0.46f, h * 0.86f)
                lineTo(w * 0.08f, h * 0.48f)
                close()
            }

            drawPath(
                path = chevronPath,
                color = arrowColor,
                style = Fill
            )

            // Dynamic golden accent swoosh arc near the corner
            drawCircle(
                color = Color(0xFFFFA500),
                radius = w * 0.08f,
                center = Offset(w * 0.86f, h * 0.16f)
            )
        }
    }
}

/**
 * Full Deetrend Brand Logo Header:
 * Displays the official emblem, "DEETREND" bold wordmark, and "Deetrend Global Enterprise" subtext.
 */
@Composable
fun DeetrendBrandLogo(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 40.dp,
    showSubtitle: Boolean = true,
    showWebsite: Boolean = false,
    horizontal: Boolean = true,
    isDarkText: Boolean = true
) {
    val primaryTextColor = if (isDarkText) DeetrendTeal else Color.White
    val secondaryTextColor = if (isDarkText) DeetrendTextSecondary else Color.White.copy(alpha = 0.8f)

    if (horizontal) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertizingSafe(),
            horizontalArrangement = Arrangement.Start
        ) {
            DeetrendBrandEmblem(size = emblemSize)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DEETREND",
                        fontWeight = FontWeight.Black,
                        fontSize = if (emblemSize >= 44.dp) 18.sp else 16.sp,
                        letterSpacing = 1.2.sp,
                        color = primaryTextColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(DeetrendOrange, CircleShape)
                    )
                }
                if (showSubtitle) {
                    Text(
                        text = "Global Enterprise",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = secondaryTextColor
                    )
                }
                if (showWebsite) {
                    Text(
                        text = "www.deetrendglobal.com.ng",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = DeetrendTealDark
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DeetrendBrandEmblem(size = emblemSize)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DEETREND",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    color = primaryTextColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(DeetrendOrange, CircleShape)
                )
            }
            if (showSubtitle) {
                Text(
                    text = "Deetrend Global Enterprise",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = secondaryTextColor
                )
            }
            if (showWebsite) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "www.deetrendglobal.com.ng",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = DeetrendTealDark
                )
            }
        }
    }
}

// Alignment helper
private fun Alignment.Companion.CenterVertizingSafe(): Alignment.Vertical = Alignment.CenterVertically
