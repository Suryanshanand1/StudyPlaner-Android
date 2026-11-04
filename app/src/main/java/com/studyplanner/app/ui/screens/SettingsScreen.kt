package com.studyplanner.app.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.BuildConfig
import com.studyplanner.app.data.ThemeMode
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.ui.components.ACCENT_PRESETS
import com.studyplanner.app.ui.components.ConfirmDialog
import com.studyplanner.app.ui.components.StudyCard
import com.studyplanner.app.ui.theme.parseHexColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HEX_PATTERN = Regex("^#[0-9a-fA-F]{6}$")

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onMessage: (String) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCustomAccent by remember { mutableStateOf(false) }
    var hexInput by remember { mutableStateOf(settings.accentColor) }
    var clearConfirm by remember { mutableStateOf(false) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    var canExactAlarms by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms(),
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        canExactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        hexInput = settings.accentColor
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsEnabled = granted
        onMessage(if (granted) "Notifications enabled" else "Notification permission denied")
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportData { json ->
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val stream = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
                        stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        true
                    }.getOrDefault(false)
                }
                onMessage(if (ok) "Backup exported" else "Could not write the backup")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                onMessage("Could not read that file")
                return@launch
            }
            viewModel.importData(text) { ok ->
                scope.launch {
                    onMessage(if (ok) "Backup imported" else "Invalid backup file")
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SettingsSection(
                title = "Appearance",
                subtitle = "Choose your theme preference",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeOption(
                        label = "Light",
                        icon = Icons.Outlined.LightMode,
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setTheme(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        label = "Dark",
                        icon = Icons.Outlined.DarkMode,
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setTheme(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        label = "System",
                        icon = Icons.Outlined.SettingsBrightness,
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setTheme(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = "Accent color",
                subtitle = "Used for buttons, highlights and charts",
                icon = Icons.Outlined.Palette,
            ) {
                ACCENT_PRESETS.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (name, hex) ->
                            AccentSwatch(
                                name = name,
                                hex = hex,
                                selected = settings.accentColor.equals(hex, ignoreCase = true) && !showCustomAccent,
                                onClick = {
                                    showCustomAccent = false
                                    viewModel.setAccent(hex)
                                },
                            )
                        }
                        if (row == ACCENT_PRESETS.chunked(4).last()) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showCustomAccent = !showCustomAccent },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = "Custom color",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (showCustomAccent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(parseHexColor(hexInput)),
                        )
                        Spacer(Modifier.size(12.dp))
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { value ->
                                hexInput = value
                                if (HEX_PATTERN.matches(value)) {
                                    viewModel.setAccent(value.lowercase())
                                }
                            },
                            label = { Text("Hex color") },
                            singleLine = true,
                            isError = !HEX_PATTERN.matches(hexInput),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(parseHexColor(settings.accentColor)),
                    )
                    Spacer(Modifier.size(10.dp))
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            "Preview Button",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Preview Link",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = "Notifications",
                subtitle = "Get reminded when a scheduled session starts",
                icon = Icons.Outlined.Notifications,
            ) {
                Button(
                    onClick = {
                        if (notificationsEnabled) {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                },
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = if (notificationsEnabled) Icons.Outlined.CheckCircle
                        else Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(7.dp))
                    Text(if (notificationsEnabled) "Notifications enabled" else "Enable notifications")
                }
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open notification settings") }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canExactAlarms) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(7.dp))
                                Text(
                                    "Exact reminders are off",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Android may delay reminders until it wakes up. Allow exact alarms to fix this.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            TextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = "package:${context.packageName}".toUri()
                                            },
                                        )
                                    }
                                },
                            ) { Text("Allow exact alarms") }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "Data",
                subtitle = "Backups are compatible with the original web app",
                icon = Icons.Outlined.Download,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { exportLauncher.launch("study-planner-backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Export")
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream"),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Import")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Importing replaces all current subjects, chapters and plans.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { clearConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Delete all data", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            SettingsSection(
                title = "About",
                subtitle = null,
                icon = Icons.Outlined.Info,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Study Planner",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Native Android",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "All data stays on this device. The focus stopwatch is built in, " +
                        "and no data is sent anywhere except the optional update check.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        viewModel.checkForUpdates(force = true) { message -> onMessage(message) }
                    },
                ) { Text("Check for updates") }
            }
        }
    }

    if (clearConfirm) {
        ConfirmDialog(
            title = "Delete all data?",
            message = "This removes every subject, chapter, study plan and focus history on this device. Export a backup first if you need one.",
            confirmLabel = "Delete everything",
            onConfirm = {
                viewModel.clearAllData { onMessage("All data deleted") }
                clearConfirm = false
            },
            onDismiss = { clearConfirm = false },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String?,
    icon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    StudyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AccentSwatch(
    name: String,
    hex: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(parseHexColor(hex))
                .then(
                    if (selected) {
                        Modifier.padding(3.dp)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
