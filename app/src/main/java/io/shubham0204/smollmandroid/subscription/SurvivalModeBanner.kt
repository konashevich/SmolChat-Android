package io.shubham0204.smollmandroid.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Rich Survival Mode banner shown when local entitlement expired but we allow continued access.
 * Shows rationale and offers optional actions.
 */
@Composable
fun SurvivalModeBanner(
    onRenewClick: (() -> Unit)?,
    onVerifyNow: (() -> Unit)? = null,
    showVerify: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Survival Mode: Can't reach Play to verify subscription. Full access preserved so you're never locked out. Please renew or verify when connectivity returns.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.height(0.dp))
        if (showVerify && onVerifyNow != null) {
            OutlinedButton(onClick = onVerifyNow, modifier = Modifier.padding(start = 8.dp)) { Text("Verify") }
        }
        if (onRenewClick != null) {
            Button(onClick = onRenewClick, modifier = Modifier.padding(start = 8.dp)) { Text("Renew") }
        }
    }
}