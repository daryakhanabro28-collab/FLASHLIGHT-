package com.example.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.util.SoundManager
import com.example.util.SoundType
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    soundManager: SoundManager? = null,
    soundEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("Initializing secure vault engine...") }

    // Pulse animation for logo halo
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    LaunchedEffect(Unit) {
        soundManager?.play(SoundType.CLICK, soundEnabled = soundEnabled)

        val steps = listOf(
            0.20f to "Initializing cryptographic core...",
            0.45f to "Securing clock & hardware sensors...",
            0.75f to "Loading encrypted vault partition...",
            1.00f to "Stealth Utility Suite Ready"
        )

        for ((p, text) in steps) {
            val startP = progress
            val delta = p - startP
            val substeps = 10
            for (i in 1..substeps) {
                progress = startP + (delta * (i.toFloat() / substeps))
                delay(40L)
            }
            statusText = text
            delay(100L)
        }

        soundManager?.play(SoundType.UNLOCK, soundEnabled = soundEnabled)
        delay(250L)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060709),
                        Color(0xFF0B0E14),
                        Color(0xFF050608)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onSplashFinished()
            }
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Animated Cyber Logo Frame
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Outer Cyan Glow
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(haloPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF).copy(alpha = haloAlpha * 0.4f),
                                    Color(0xFF007A8A).copy(alpha = haloAlpha * 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Logo Container
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF0E131C))
                        .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                        .testTag("splash_logo"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_icon),
                        contentDescription = "UV Privacy Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "UV PRIVACY",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("splash_app_name")
            )

            Text(
                text = "ADVANCED STEALTH VAULT & UTILITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Progress Bar & Percentage
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.75f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF1E2638)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Creator & Team Attribution
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10141E).copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232B3C)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .testTag("splash_creator_text")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DEVELOPED & DESIGNED BY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF8A93A6)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Darya Khan abro",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "team Mr Pro PrimeX",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }
        }

        // Tap to skip hint at bottom
        Text(
            text = "Tap screen to continue",
            fontSize = 11.sp,
            color = Color.DarkGray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        )
    }
}
