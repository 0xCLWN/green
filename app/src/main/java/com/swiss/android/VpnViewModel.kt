package com.swiss.android

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swiss.android.data.AppDatabase
import com.swiss.android.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import libswiss.Libswiss
import org.json.JSONObject

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED }

object VpnState {
    val status = MutableStateFlow(VpnStatus.DISCONNECTED)
}

class VpnViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).configDao()
    private val prefs: SharedPreferences =
        app.getSharedPreferences("swiss", Context.MODE_PRIVATE)

    init {
        viewModelScope.launch { seedDefaultConfigs() }
    }

    val configs: StateFlow<List<Config>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedId = MutableStateFlow<Int?>(null)
    val selectedId: StateFlow<Int?> = _selectedId.asStateFlow()

    private val _addError = MutableStateFlow<String?>(null)
    val addError: StateFlow<String?> = _addError.asStateFlow()

    val status: StateFlow<VpnStatus> = VpnState.status.asStateFlow()

    private var pendingConfigJson: String? = null
    private var pendingDnsServer: String = "1.1.1.1"

    private suspend fun seedDefaultConfigs() {
        if (prefs.getBoolean("defaults_seeded", false)) return
        BuildConfig.DEFAULT_VLESS_KEYS
            .split("|")
            .filter { it.isNotBlank() }
            .forEach { link ->
                runCatching {
                    val name = link.substringAfterLast("#", "").ifBlank {
                        link.substringAfter("@").substringBefore("?")
                    }
                    dao.insert(Config(name = name, vlessLink = link))
                }
            }
        prefs.edit().putBoolean("defaults_seeded", true).apply()
    }

    fun select(config: Config) {
        _selectedId.value = config.id
    }

    fun addConfig(input: String) {
        viewModelScope.launch {
            _addError.value = null
            try {
                val trimmed = input.trim()
                if (trimmed.startsWith("vless://")) {
                    withContext(Dispatchers.Default) { Libswiss.validateVlessKey(trimmed) }
                    val name = trimmed.substringAfterLast("#", "").ifBlank {
                        trimmed.substringAfter("@").substringBefore("?")
                    }
                    dao.insert(Config(name = name, vlessLink = trimmed))
                } else {
                    val name = nameFromJsonConfig(trimmed)
                    dao.insert(Config(name = name, configJson = trimmed))
                }
            } catch (e: Exception) {
                _addError.value = e.message ?: "Invalid input"
            }
        }
    }

    private fun dnsServerFromJson(json: String): String {
        return try {
            val servers = JSONObject(json).optJSONObject("dns")?.optJSONArray("servers")
            if (servers != null) {
                for (i in 0 until servers.length()) {
                    val addr = when (val s = servers.get(i)) {
                        is JSONObject -> s.optString("address", "")
                        is String -> s
                        else -> ""
                    }
                    if (addr.isNotBlank() && addr != "localhost" && !addr.startsWith("127.")) return addr
                }
            }
            "1.1.1.1"
        } catch (_: Exception) { "1.1.1.1" }
    }

    private fun nameFromJsonConfig(json: String): String {
        return try {
            val obj = JSONObject(json)
            val outbounds = obj.optJSONArray("outbounds")
            if (outbounds != null && outbounds.length() > 0) {
                val settings = outbounds.getJSONObject(0).optJSONObject("settings")
                val flat = settings?.optString("address", "")
                if (!flat.isNullOrBlank()) return flat
                val vnext = settings?.optJSONArray("vnext")
                val addr = vnext?.getJSONObject(0)?.optString("address", "")
                if (!addr.isNullOrBlank()) return addr
            }
            "JSON config"
        } catch (_: Exception) {
            "JSON config"
        }
    }

    fun clearAddError() { _addError.value = null }

    fun deleteConfig(config: Config) {
        viewModelScope.launch { dao.delete(config) }
        if (_selectedId.value == config.id) _selectedId.value = null
    }

    fun connect(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val config = configs.value.find { it.id == _selectedId.value } ?: return
        viewModelScope.launch {
            try {
                // Generate the xray config fresh from the vless key each time —
                // this picks up any config schema improvements from app updates,
                // and also performs DNS pre-resolution before the TUN is established.
                data class Resolved(val configJson: String, val dnsServer: String)
                val resolved = withContext(Dispatchers.IO) {
                    when {
                        config.vlessLink != null -> Resolved(
                            configJson = Libswiss.vlessKeyToXrayJson(config.vlessLink),
                            dnsServer = Libswiss.vlessKeyDnsServer(config.vlessLink),
                        )
                        config.configJson != null -> Resolved(
                            configJson = config.configJson,
                            dnsServer = dnsServerFromJson(config.configJson),
                        )
                        else -> throw IllegalStateException("config has neither vlessLink nor configJson")
                    }
                }
                pendingConfigJson = resolved.configJson
                pendingDnsServer = resolved.dnsServer
                val prepare = VpnService.prepare(context)
                if (prepare != null) launcher.launch(prepare)
                else startService(context, resolved.configJson, resolved.dnsServer)
            } catch (e: Exception) {
                VpnState.status.value = VpnStatus.DISCONNECTED
            }
        }
    }

    fun onPermissionGranted(context: Context) {
        startService(context, pendingConfigJson ?: return, pendingDnsServer)
    }

    fun disconnect(context: Context) {
        context.startService(
            Intent(context, SwissVpnService::class.java).apply {
                action = SwissVpnService.ACTION_STOP
            }
        )
    }

    private fun startService(context: Context, configJson: String, dnsServer: String) {
        VpnState.status.value = VpnStatus.CONNECTING
        context.startForegroundService(
            Intent(context, SwissVpnService::class.java).apply {
                action = SwissVpnService.ACTION_START
                putExtra(SwissVpnService.EXTRA_CONFIG_JSON, configJson)
                putExtra(SwissVpnService.EXTRA_DNS_SERVER, dnsServer)
            }
        )
    }
}
