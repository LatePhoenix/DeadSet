package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R

// Colors of the five classic Owsley "Bear" Stanley / Bob Thomas Marching Bears (matching reference art)
val BearPink = Color(0xFFF472B6)
val BearGreen = Color(0xFF34D399)
val BearYellow = Color(0xFFFBBF24)
val BearBlue = Color(0xFF38BDF8)
val BearOrange = Color(0xFFFB923C)
val BearPurple = BearPink // Alias for compatibility

/**
 * Renders the authentic Grateful Dead Marching Bear artwork matching the requested color
 * (Pink w/ Green collar, Green w/ Blue collar, Yellow w/ Green collar, Blue w/ Yellow collar, Orange w/ Green collar).
 */
@Composable
fun DancingBear(
    color: Color,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val drawableRes = when (color) {
        BearPink -> R.drawable.art_bear_pink
        BearGreen -> R.drawable.art_bear_green
        BearYellow -> R.drawable.art_bear_yellow
        BearBlue -> R.drawable.art_bear_blue
        BearOrange -> R.drawable.art_bear_orange
        else -> R.drawable.art_bear_yellow
    }

    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = "Grateful Dead Marching Bear",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

/**
 * Animated parade row of the 5 iconic Grateful Dead Marching Bears
 * in their authentic sequence: Pink, Green, Yellow, Blue, Orange.
 */
@Composable
fun DancingBearsMarchRow(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BearsDance")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BearBounce"
    )

    Surface(
        color = StealieMidnight.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bears = listOf(
                Triple(R.drawable.art_bear_pink, BearPink, "Pink Marching Bear"),
                Triple(R.drawable.art_bear_green, BearGreen, "Green Marching Bear"),
                Triple(R.drawable.art_bear_yellow, BearYellow, "Yellow Marching Bear"),
                Triple(R.drawable.art_bear_blue, BearBlue, "Blue Marching Bear"),
                Triple(R.drawable.art_bear_orange, BearOrange, "Orange Marching Bear")
            )

            bears.forEachIndexed { index, (drawableRes, _, name) ->
                val offset = if (index % 2 == 0) bounceOffset else -bounceOffset
                Box(modifier = Modifier.offset(y = offset.dp)) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

/**
 * Banjo-strumming Terrapin Turtle badge celebrating Terrapin Station & Terrapin Flyer.
 */
@Composable
fun TerrapinTurtleBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        color = StealieCardDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, StealieGreen.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val w = size.width
                val h = size.height

                // Turtle Shell (domed emerald shell)
                drawOval(
                    color = StealieGreen,
                    topLeft = Offset(w * 0.15f, h * 0.2f),
                    size = Size(w * 0.65f, h * 0.55f)
                )
                // Shell Hexagon Pattern
                drawOval(
                    color = Color(0xFF065F46),
                    topLeft = Offset(w * 0.30f, h * 0.32f),
                    size = Size(w * 0.35f, h * 0.32f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Head
                drawCircle(
                    color = Color(0xFF6EE7B7),
                    radius = w * 0.15f,
                    center = Offset(w * 0.82f, h * 0.38f)
                )
                // Banjo Neck and Body
                drawLine(
                    color = Color(0xFFFBBF24),
                    start = Offset(w * 0.75f, h * 0.70f),
                    end = Offset(w * 0.20f, h * 0.85f),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFFFEF3C7),
                    radius = w * 0.16f,
                    center = Offset(w * 0.28f, h * 0.82f)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "TERRAPIN FLYER",
                    color = StealieGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Local Tribute & JGB Jams",
                    color = TextSecondaryDark,
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Bertha Rose & Vine ornament.
 */
@Composable
fun BerthaRose(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Rose Petals layers
        drawCircle(
            color = StealieCrimson,
            radius = w * 0.45f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
        drawCircle(
            color = StealieRose,
            radius = w * 0.30f,
            center = Offset(w * 0.45f, h * 0.45f)
        )
        drawCircle(
            color = Color(0xFFFECDD3),
            radius = w * 0.14f,
            center = Offset(w * 0.52f, h * 0.48f)
        )
    }
}

/**
 * Vintage Maxell XL-II / TDK Cassette Tape Card
 * Evokes the sacred culture of tape trading, J-cards, and Soundboard recordings.
 */
@Composable
fun CassetteTapeJCard(
    date: String,
    venue: String,
    tapeQuality: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E212D),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF475569)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Vintage Cassette Top Bar (Maxell Gold / Red Band)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF991B1B), Color(0xFFD97706), Color(0xFF991B1B))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "MAXELL XL-II 90",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = tapeQuality.ifBlank { "BETTY BOARD SBD" },
                    color = Color(0xFFFEF08A),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Handwritten-style Concert Info
            Text(
                text = "$date • $venue",
                color = Color(0xFFF1F5F9),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Cassette Window with Rotating Hubs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Reel Hub
                    CassetteReelHub()

                    // Center Tape Window with scale lines
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0..6) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(if (i == 3) 14.dp else 8.dp)
                                    .background(Color(0xFF64748B))
                            )
                        }
                    }

                    // Right Reel Hub
                    CassetteReelHub()
                }
            }
        }
    }
}

@Composable
private fun CassetteReelHub() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
        )
    }
}
