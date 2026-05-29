package com.swiss.android

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED }

object VpnState {
    val status = MutableStateFlow(VpnStatus.DISCONNECTED)
}

class VpnViewModel : ViewModel() {

    val status: StateFlow<VpnStatus> = VpnState.status.asStateFlow()

    fun connect(context: Context, permissionLauncher: ActivityResultLauncher<Intent>) {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            permissionLauncher.launch(intent)
        } else {
            startService(context)
        }
    }

    fun onPermissionGranted(context: Context) {
        startService(context)
    }

    fun disconnect(context: Context) {
        context.startService(
            Intent(context, SwissVpnService::class.java).apply {
                action = SwissVpnService.ACTION_STOP
            }
        )
    }

    private fun startService(context: Context) {
        VpnState.status.value = VpnStatus.CONNECTING
        context.startForegroundService(
            Intent(context, SwissVpnService::class.java).apply {
                action = SwissVpnService.ACTION_START
            }
        )
    }
}
