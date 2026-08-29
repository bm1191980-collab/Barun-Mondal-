package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * SatisfyAnimatedLogo:
 * Renders the custom 3D cyan/yellow 'S' Satisfy logo with a continuous, hardware-accelerated
 * smooth left-and-right floating animation, subtle breathing scale, and gentle alpha fade.
 */
@Composable
fun SatisfyAnimatedLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    isAnimated: Boolean = true,
    withBackgroundBadge: Boolean = true,
    badgeColor: Color = Color(0xFFE0F2FE)
) {
    if (isAnimated) {
        val infiniteTransition = rememberInfiniteTransition(label = "SatisfyLogoInfiniteAnimation")

        // Smooth horizontal drift left & right
        val translationX by infiniteTransition.animateFloat(
            initialValue = -3.5f,
            targetValue = 3.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1800,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "translationX"
        )

        // Subtle scale pulse (0.96x to 1.04x)
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2200,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        // Soft opacity fade (0.90 to 1.0)
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Box(
            modifier = modifier
                .size(size)
                .then(
                    if (withBackgroundBadge) {
                        Modifier
                            .clip(RoundedCornerShape(size * 0.28f))
                            .background(badgeColor)
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer {
                    this.translationX = translationX * density
                    this.scaleX = scale
                    this.scaleY = scale
                    this.alpha = alpha
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_satisfy_logo),
                contentDescription = "Satisfy Custom Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (withBackgroundBadge) size * 0.08f else 0.dp),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .then(
                    if (withBackgroundBadge) {
                        Modifier
                            .clip(RoundedCornerShape(size * 0.28f))
                            .background(badgeColor)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_satisfy_logo),
                contentDescription = "Satisfy Custom Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (withBackgroundBadge) size * 0.08f else 0.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * SatisfyBrandedHeaderTitle:
 * Pairs the smooth animated custom logo next to any "Satisfy..." text display.
 */
@Composable
fun SatisfyBrandedHeaderTitle(
    text: String,
    modifier: Modifier = Modifier,
    logoSize: Dp = 26.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingBadgeText: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SatisfyAnimatedLogo(
            size = logoSize,
            withBackgroundBadge = true,
            badgeColor = Color(0xFFE0F2FE)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = textStyle,
            color = textColor
        )
        if (!trailingBadgeText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = trailingBadgeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
