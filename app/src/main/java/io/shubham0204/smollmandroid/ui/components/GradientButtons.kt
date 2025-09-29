package io.shubham0204.smollmandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val GradientEnabled = Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFFF0066)))
private val GradientDisabled = Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF222222)))

@Composable
fun GradientPrimaryButton(
    text: String,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) GradientEnabled else GradientDisabled)
            .padding(2.dp), // border thickness
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF101010),
                disabledContainerColor = Color(0xFF181818),
                contentColor = Color.White,
                disabledContentColor = Color(0xFF444444),
            ),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(text)
        }
    }
}

@Composable
fun GradientCircularIconButton(
    icon: ImageVector,
    contentDescription: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val brush = if (enabled) GradientEnabled else GradientDisabled
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(brush)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (enabled) Color(0xFF101010) else Color(0xFF181818))
                .size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onClick,
                enabled = enabled,
            ) {
                Icon(
                    icon,
                    contentDescription = contentDescription,
                    tint = if (enabled) Color.White else Color(0xFF444444),
                )
            }
        }
    }
}
