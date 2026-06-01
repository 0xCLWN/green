package com.swiss.android

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swiss.android.data.AppDatabase
import com.swiss.android.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import libswiss.Libswiss
import org.json.JSONObject
import java.io.File
import java.net.ServerSocket
import java.util.UUID

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED }

object VpnState {
    val status = MutableStateFlow(VpnStatus.DISCONNECTED)
}

data class GeoState(
    val enabled: Boolean,
    val geoipUrl: String,
    val geositeUrl: String,
    val updating: Boolean = false,
    val filesVersion: Int = 0,
)

class VpnViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).configDao()
    private val prefs: SharedPreferences =
        app.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

    private val _autoConnect = MutableStateFlow(prefs.getBoolean(Prefs.AUTO_CONNECT, false))
    val autoConnect: StateFlow<Boolean> = _autoConnect.asStateFlow()
    fun setAutoConnect(v: Boolean) { _autoConnect.value = v; prefs.edit { putBoolean(Prefs.AUTO_CONNECT, v) } }

    private val _notify = MutableStateFlow(prefs.getBoolean(Prefs.NOTIFY, true))
    val notify: StateFlow<Boolean> = _notify.asStateFlow()
    fun setNotify(v: Boolean) { _notify.value = v; prefs.edit { putBoolean(Prefs.NOTIFY, v) } }

    private val _connectTimeMs = MutableStateFlow<Long?>(null)
    val connectTimeMs: StateFlow<Long?> = _connectTimeMs.asStateFlow()

    init {
        viewModelScope.launch { seedDefaultConfigs() }
        viewModelScope.launch {
            VpnState.status.collect { s ->
                _connectTimeMs.value = if (s == VpnStatus.CONNECTED) System.currentTimeMillis() else null
            }
        }
    }

    val configs: StateFlow<List<Config>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedId = MutableStateFlow<Int?>(
        prefs.getInt(Prefs.LAST_SELECTED, -1).takeIf { it != -1 }
    )
    val selectedId: StateFlow<Int?> = _selectedId.asStateFlow()

    private val _addError = MutableStateFlow<String?>(null)
    val addError: StateFlow<String?> = _addError.asStateFlow()

    val status: StateFlow<VpnStatus> = VpnState.status.asStateFlow()

    private val _allowedApps = MutableStateFlow<Set<String>>(
        prefs.getStringSet(Prefs.ALLOWED_APPS, emptySet()) ?: emptySet()
    )
    val allowedApps: StateFlow<Set<String>> = _allowedApps.asStateFlow()

    fun setAllowedApps(apps: Set<String>) {
        _allowedApps.value = apps
        prefs.edit { putStringSet(Prefs.ALLOWED_APPS, apps) }
    }

    private val _geo = MutableStateFlow(
        GeoState(
            enabled = prefs.getBoolean(Prefs.GEO_ENABLED, false),
            geoipUrl = prefs.getString(Prefs.GEO_GEOIP_URL, GeoUpdater.DEFAULT_GEOIP_URL) ?: GeoUpdater.DEFAULT_GEOIP_URL,
            geositeUrl = prefs.getString(Prefs.GEO_GEOSITE_URL, GeoUpdater.DEFAULT_GEOSITE_URL) ?: GeoUpdater.DEFAULT_GEOSITE_URL,
        )
    )
    val geo: StateFlow<GeoState> = _geo.asStateFlow()

    fun setGeoEnabled(v: Boolean) { _geo.update { it.copy(enabled = v) }; prefs.edit { putBoolean(Prefs.GEO_ENABLED, v) } }
    fun setGeoipUrl(v: String) { _geo.update { it.copy(geoipUrl = v) }; prefs.edit { putString(Prefs.GEO_GEOIP_URL, v) } }
    fun setGeositeUrl(v: String) { _geo.update { it.copy(geositeUrl = v) }; prefs.edit { putString(Prefs.GEO_GEOSITE_URL, v) } }

    private var geoUpdateJob: Job? = null

    fun skipGeoUpdate() { geoUpdateJob?.cancel() }

    fun updateGeoNow() {
        geoUpdateJob?.cancel()
        geoUpdateJob = viewModelScope.launch {
            _geo.update { it.copy(updating = true) }
            val filesDir = getApplication<Application>().filesDir
            runCatching { GeoUpdater.download(filesDir, _geo.value.geoipUrl, _geo.value.geositeUrl) }
            _geo.update { it.copy(updating = false, filesVersion = it.filesVersion + 1) }
        }
    }

    fun importGeoFile(context: Context, uri: Uri, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    File(getApplication<Application>().filesDir, name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            _geo.update { it.copy(filesVersion = it.filesVersion + 1) }
        }
    }

    private var connectJob: Job? = null
    private var pendingConfigJson: String? = null
    private var pendingDnsServer: String = "1.1.1.1"
    private var pendingSocksPort: Int = 10808
    private var pendingSocksUser: String = ""
    private var pendingSocksPass: String = ""

    private suspend fun seedDefaultConfigs() {
        if (prefs.getBoolean(Prefs.DEFAULTS_SEEDED, false)) return
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
        prefs.edit { putBoolean(Prefs.DEFAULTS_SEEDED, true) }
    }

    fun select(config: Config) {
        _selectedId.value = config.id
        prefs.edit { putInt(Prefs.LAST_SELECTED, config.id) }
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

    fun clearAddError() {
        _addError.value = null
    }

    fun updateConfig(config: Config) {
        viewModelScope.launch { dao.update(config) }
    }

    fun deleteConfig(config: Config) {
        viewModelScope.launch { dao.delete(config) }
        if (_selectedId.value == config.id) _selectedId.value = null
    }

    fun connect(context: Context, launcher: ActivityResultLauncher<Intent>) {
        connectJob?.cancel()
        val config = configs.value.find { it.id == _selectedId.value } ?: return
        connectJob = viewModelScope.launch {
            val geo = _geo.value
            val filesDir = getApplication<Application>().filesDir
            if (geo.enabled && GeoUpdater.isStale(filesDir)) {
                _geo.update { it.copy(updating = true) }
                geoUpdateJob = launch { runCatching { GeoUpdater.download(filesDir, geo.geoipUrl, geo.geositeUrl) } }
                geoUpdateJob?.join()
                _geo.update { it.copy(updating = false) }
            }

            try {
                // Generate the xray config fresh from the vless key each time —
                // this picks up any config schema improvements from app updates,
                // and also performs DNS pre-resolution before the TUN is established.
                data class Resolved(
                    val configJson: String,
                    val dnsServer: String,
                    val socksPort: Int,
                    val socksUser: String,
                    val socksPass: String,
                )

                val resolved = withContext(Dispatchers.IO) {
                    val rawJson = when {
                        config.vlessLink != null -> Libswiss.vlessKeyToXrayJson(config.vlessLink)
                        config.configJson != null -> config.configJson
                        else -> throw IllegalStateException("config has neither vlessLink nor configJson")
                    }
                    val dns = when {
                        config.vlessLink != null -> Libswiss.vlessKeyDnsServer(config.vlessLink)
                        config.configJson != null -> dnsServerFromJson(config.configJson)
                        else -> "1.1.1.1"
                    }
                    // Port 0 lets the OS pick a free port; prevents other apps from
                    // connecting to a hardcoded port even if they know our proxy is running.
                    val port = ServerSocket(0).use { it.localPort }
                    val user = UUID.randomUUID().toString().replace("-", "").take(16)
                    val pass = UUID.randomUUID().toString().replace("-", "").take(16)
                    Resolved(
                        configJson = patchXrayConfig(rawJson, port, user, pass),
                        dnsServer = dns,
                        socksPort = port,
                        socksUser = user,
                        socksPass = pass,
                    )
                }
                pendingConfigJson = resolved.configJson
                pendingDnsServer = resolved.dnsServer
                pendingSocksPort = resolved.socksPort
                pendingSocksUser = resolved.socksUser
                pendingSocksPass = resolved.socksPass
                val prepare = VpnService.prepare(context)
                if (prepare != null) launcher.launch(prepare)
                else startService(
                    context,
                    resolved.configJson,
                    resolved.dnsServer,
                    resolved.socksPort,
                    resolved.socksUser,
                    resolved.socksPass,
                    _notify.value,
                )
            } catch (e: Exception) {
                VpnState.status.value = VpnStatus.DISCONNECTED
            }
        }
    }

    fun onPermissionDenied() {
        connectJob?.cancel()
        connectJob = null
        VpnState.status.value = VpnStatus.DISCONNECTED
    }

    fun onPermissionGranted(context: Context) {
        startService(
            context,
            pendingConfigJson ?: return,
            pendingDnsServer,
            pendingSocksPort,
            pendingSocksUser,
            pendingSocksPass,
            _notify.value,
        )
    }

    fun disconnect(context: Context) {
        connectJob?.cancel()
        connectJob = null
        _geo.update { it.copy(updating = false) }
        context.startService(
            Intent(context, SwissVpnService::class.java).apply {
                action = SwissVpnService.ACTION_STOP
            }
        )
    }

    private fun startService(
        context: Context,
        configJson: String,
        dnsServer: String,
        socksPort: Int,
        socksUser: String,
        socksPass: String,
        notify: Boolean,
    ) {
        VpnState.status.value = VpnStatus.CONNECTING
        context.startForegroundService(
            Intent(context, SwissVpnService::class.java).apply {
                action = SwissVpnService.ACTION_START
                putExtra(SwissVpnService.EXTRA_CONFIG_JSON, configJson)
                putExtra(SwissVpnService.EXTRA_DNS_SERVER, dnsServer)
                putExtra(SwissVpnService.EXTRA_SOCKS_PORT, socksPort)
                putExtra(SwissVpnService.EXTRA_SOCKS_USER, socksUser)
                putExtra(SwissVpnService.EXTRA_SOCKS_PASS, socksPass)
                putExtra(SwissVpnService.EXTRA_NOTIFY, notify)
                putStringArrayListExtra(
                    SwissVpnService.EXTRA_ALLOWED_APPS,
                    ArrayList(_allowedApps.value)
                )
            }
        )
    }
}
