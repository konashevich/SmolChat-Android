package io.shubham0204.smollmandroid.subscription

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

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