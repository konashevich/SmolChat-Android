package io.shubham0204.smollmandroid.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RenewalPromptDialog(onRenew: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onLater() },
        title = { Text("Subscription Ended While Offline") },
        text = { Text("Your paid period ended during offline use. Crisis AI stayed unlocked in Survival Mode. Please renew to support continued development.") },
        confirmButton = {
            Button(onClick = onRenew) { Text("Renew Now") }
        },
        dismissButton = {
            OutlinedButton(onClick = onLater) { Text("Later") }
        },
    )
}

@Composable
fun DebugSubscriptionOverlay(
    subscriptionManager: SubscriptionManager,
    isOnline: Boolean,
    cornerOffset: Dp = 8.dp,
) {
    var expanded by remember { mutableStateOf(false) }
    val state = subscriptionManager.currentState()
    val log = subscriptionManager.getTransitionLog()
    Box(
        modifier = Modifier
            .padding(cornerOffset)
            .shadow(4.dp, MaterialTheme.shapes.medium)
            .background(Color(0xCC111111))
            .padding(8.dp)
            .fillMaxWidth(0.9f),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SUB DEBUG: ${state.name} | online=${isOnline}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Log", fontSize = MaterialTheme.typography.labelSmall.fontSize) }
            }
            if (expanded) {
                Text(
                    text = log.ifBlank { "<no transitions>" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                )
            }
        }
    }
}