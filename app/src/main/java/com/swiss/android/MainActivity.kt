package com.swiss.android

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swiss.android.VpnState
import com.swiss.android.VpnStatus
import com.swiss.android.VpnViewModel
import com.swiss.android.ui.theme.SwissTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VpnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwissTheme {
                VpnScreen(viewModel)
            }
        }
    }
}

@Composable
fun VpnScreen(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPermissionGranted(context)
        } else {
            VpnState.status.value = VpnStatus.DISCONNECTED
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (status) {
                    VpnStatus.DISCONNECTED -> "Disconnected"
                    VpnStatus.CONNECTING -> "Connecting…"
                    VpnStatus.CONNECTED -> "Connected"
                },
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    when (status) {
                        VpnStatus.CONNECTED -> viewModel.disconnect(context)
                        else -> viewModel.connect(context, permissionLauncher)
                    }
                },
                enabled = status != VpnStatus.CONNECTING
            ) {
                Text(if (status == VpnStatus.CONNECTED) "Disconnect" else "Connect")
            }
        }
    }
}
