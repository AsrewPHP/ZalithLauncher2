package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScalingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "ButtonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun IconTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    iconSize: Dp = 24.dp,
    painter: Painter,
    contentDescription: String?,
    text: String,
    style: TextStyle = MaterialTheme.typography.labelMedium
) {
    BaseIconTextButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        icon = { modifier1 ->
            Icon(
                modifier = modifier1.size(iconSize),
                painter = painter,
                contentDescription = contentDescription
            )
        },
        text = text,
        style = style
    )
}

@Composable
fun IconTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    iconSize: Dp = 24.dp,
    imageVector: ImageVector,
    contentDescription: String?,
    text: String,
    style: TextStyle = MaterialTheme.typography.labelMedium
) {
    BaseIconTextButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        icon = { modifier1 ->
            Icon(
                modifier = modifier1.size(iconSize),
                imageVector = imageVector,
                contentDescription = contentDescription
            )
        },
        text = text,
        style = style
    )
}

@Composable
private fun BaseIconTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    icon: @Composable (Modifier) -> Unit,
    text: String,
    style: TextStyle = MaterialTheme.typography.labelMedium
) {
    Row(
        modifier = modifier
            .clip(shape = shape)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 8.dp, vertical = 4.dp))
    ) {
        icon(Modifier.align(Alignment.CenterVertically))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 4.dp),
            text = text,
            style = style
        )
    }
}

@Composable
fun TouchableButton(
    modifier: Modifier = Modifier,
    onTouch: (isPressed: Boolean) -> Unit = {},
    text: String
) {
    Surface(
        shape = CircleShape,
        color = ButtonDefaults.buttonColors().containerColor,
        contentColor = ButtonDefaults.buttonColors().contentColor,
        modifier = modifier
            .semantics { role = Role.Button }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onTouch(true)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                    } while (change != null && change.pressed)

                    onTouch(false)
                }
            }
    ) {
        val mergedStyle = LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge)
        CompositionLocalProvider(
            LocalContentColor provides ButtonDefaults.buttonColors().contentColor,
            LocalTextStyle provides mergedStyle,
        ) {
            Row(
                Modifier.defaultMinSize(
                    minWidth = ButtonDefaults.MinWidth,
                    minHeight = ButtonDefaults.MinHeight
                )
                    .padding(ButtonDefaults.ContentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = text)
            }
        }
    }
}