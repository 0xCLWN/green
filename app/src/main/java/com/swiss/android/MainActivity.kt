package com.swiss.android

import android.Manifest
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import com.swiss.android.data.Config
import com.swiss.android.ui.theme.SwissTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(val packageName: String, val label: String)

class MainActivity : ComponentActivity() {
    private val viewModel: VpnViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — foreground service keeps running either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            SwissTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val configs by viewModel.configs.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val addError by viewModel.addError.collectAsState()

    val allowedApps by viewModel.allowedApps.collectAsState()
    val geoUpdating by viewModel.geoUpdating.collectAsState() // GEO UPDATE — remove with GeoUpdater
    var showAddDialog by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.onPermissionGranted(context)
        else viewModel.onPermissionDenied()
    }

    Scaffold(
        floatingActionButton = {
            if (status == VpnStatus.DISCONNECTED) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add config")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = when (status) {
                    VpnStatus.DISCONNECTED -> "Disconnected"
                    VpnStatus.CONNECTING -> "Connecting…"
                    VpnStatus.CONNECTED -> "Connected"
                },
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (status == VpnStatus.CONNECTED) viewModel.disconnect(context)
                    else viewModel.connect(context, permissionLauncher)
                },
                enabled = !geoUpdating && status != VpnStatus.CONNECTING && (status == VpnStatus.CONNECTED || selectedId != null),
            ) {
                Text(if (status == VpnStatus.CONNECTED) "Disconnect" else "Connect")
            }

            // GEO UPDATE — remove this block with GeoUpdater
            if (geoUpdating) {
                Spacer(Modifier.height(12.dp))
                GeoUpdateBanner(onSkip = { viewModel.skipGeoUpdate() })
            }
            // END GEO UPDATE

            if (status == VpnStatus.DISCONNECTED) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppPicker = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Split tunneling", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (allowedApps.isEmpty()) "All apps" else "${allowedApps.size} apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                HorizontalDivider()
            }

            Spacer(Modifier.height(24.dp))

            if (configs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No configs yet. Tap + to add one.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(configs, key = { it.id }) { config ->
                        ConfigCard(
                            config = config,
                            selected = config.id == selectedId,
                            enabled = status == VpnStatus.DISCONNECTED,
                            onSelect = { viewModel.select(config) },
                            onDelete = { viewModel.deleteConfig(config) },
                        )
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            allowedApps = allowedApps,
            onDismiss = { showAppPicker = false },
            onConfirm = {
                viewModel.setAllowedApps(it)
                showAppPicker = false
            },
        )
    }

    if (showAddDialog) {
        AddConfigDialog(
            error = addError,
            onDismiss = { showAddDialog = false; viewModel.clearAddError() },
            onAdd = { link ->
                viewModel.addConfig(link)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ConfigCard(
    config: Config,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = { if (enabled) onSelect() },
        enabled = enabled,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(config.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    config.vlessLink ?: "JSON config",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (enabled) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// GEO UPDATE — remove this composable with GeoUpdater
@Composable
fun GeoUpdateBanner(onSkip: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                "Updating geo databases…",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    }
}
// END GEO UPDATE

@Composable
fun AppPickerDialog(
    allowedApps: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember(allowedApps) { mutableStateOf(allowedApps) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    it.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
                    it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                }
                .map { AppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
                .sortedBy { it.label.lowercase() }
        }
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column {
                Text(
                    "Split tunneling",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 24.dp,
                        end = 24.dp,
                        bottom = 16.dp
                    ),
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (loading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                } else {
                    LazyColumn(Modifier.heightIn(max = 480.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (app.packageName in selected)
                                            selected - app.packageName
                                        else
                                            selected + app.packageName
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val icon by produceState<BitmapPainter?>(null, app.packageName) {
                                    value = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val d = pm.getApplicationIcon(app.packageName)
                                            val bmp = createBitmap(
                                                d.intrinsicWidth.coerceAtLeast(1),
                                                d.intrinsicHeight.coerceAtLeast(1),
                                            )
                                            android.graphics.Canvas(bmp).also { c ->
                                                d.setBounds(0, 0, c.width, c.height)
                                                d.draw(c)
                                            }
                                            BitmapPainter(bmp.asImageBitmap())
                                        }.getOrNull()
                                    }
                                }
                                val painter = icon
                                if (painter != null) {
                                    Image(
                                        painter = painter,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                    )
                                } else {
                                    Spacer(Modifier.size(36.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Checkbox(
                                    checked = app.packageName in selected,
                                    onCheckedChange = { checked ->
                                        selected = if (checked) selected + app.packageName
                                        else selected - app.packageName
                                    },
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selected.isNotEmpty()) {
                        TextButton(onClick = { selected = emptySet() }) { Text("Clear") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selected) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddConfigDialog(error: String?, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var link by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add config") },
        text = {
            Column {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("vless:// link or JSON config") },
                    isError = error != null,
                    supportingText = error?.let {
                        {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(link.trim()) }, enabled = link.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
