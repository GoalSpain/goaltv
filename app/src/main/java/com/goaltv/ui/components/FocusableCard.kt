package com.goaltv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.SurfaceDarkCard

@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
    elevation: Dp = 4.dp,
    focusedBorderColor: Color? = null,
    unfocusedBorderColor: Color? = null,
    enableScale: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused && enableScale) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "card_scale"
    )

    val shadowElevation by animateFloatAsState(
        targetValue = if (isFocused) 12f else elevation.value,
        animationSpec = tween(durationMillis = 150),
        label = "card_shadow"
    )

    val activeBorderColor = focusedBorderColor ?: OrangePrimary
    val inactiveBorderColor = unfocusedBorderColor ?: Color(0xFF2E2E40)

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = shadowElevation.dp,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        colors = colors,
        border = if (isFocused) {
            BorderStroke(
                width = 2.5.dp,
                color = activeBorderColor
            )
        } else {
            BorderStroke(if (unfocusedBorderColor != null) 1.5.dp else 1.dp, inactiveBorderColor)
        }
    ) {
        Box {
            content()
        }
    }
}