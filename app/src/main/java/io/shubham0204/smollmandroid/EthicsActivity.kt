package io.shubham0204.smollmandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

class EthicsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                EthicsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun EthicsScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Ethics & Survival Policy") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(FeatherIcons.ArrowLeft, contentDescription = "Back") }
        })
    }) { pad ->
        EthicsBody(pad)
    }
}

@Composable
private fun EthicsBody(padding: PaddingValues) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(scroll),
    ) {
        Text("Crisis AI – Ethical Subscription Statement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Crisis AI prioritizes human survival over revenue. If networks fail for extended periods, offline Survival Mode keeps full capabilities available. Once stability returns, users renew on the honor system.",
            modifier = Modifier.padding(top = 12.dp),
        )
        Text("Principles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
        bullet("Never block life-critical offline features due to lost billing access.")
        bullet("Standard annual subscription in normal times; single SKU.")
        bullet("Manual refund window: 30 days on request.")
        bullet("Respect cancellations and refunds when verifiable online.")
        bullet("Gentle renewal prompts after prolonged offline usage.")
        bullet("Fail open on data corruption / clock anomalies.")
        Text("Survival Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
        Text("Activated automatically when expiry passes but verification is impossible. The app remains fully functional. When connectivity returns you’ll be nudged to renew.")
        Text("Honor System", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
        Text("If Crisis AI helped you through an outage or disaster, please renew to keep the project sustainable.")
        Text("Transparency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
        Text("Local subscription state is stored in simple JSON for resilience. Minimal tamper resistance is intentional to avoid accidental lockouts.")
        Text("Thank you for supporting a resilience‑first tool.", modifier = Modifier.padding(top = 24.dp))
    }
}

@Composable
private fun bullet(text: String) {
    Text("• $text", modifier = Modifier.padding(top = 6.dp))
}
