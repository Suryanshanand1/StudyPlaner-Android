package com.studyplanner.app.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.studyplanner.app.data.ActiveSession
import com.studyplanner.app.data.AppSettings
import com.studyplanner.app.data.YouTubeMode
import com.studyplanner.app.focus.FocusAccessibilityService
import com.studyplanner.app.focus.InstalledApp
import com.studyplanner.app.focus.builtinSounds
import com.studyplanner.app.focus.loadInstalledApps
import com.studyplanner.app.ui.components.ConfirmDialog
import com.studyplanner.app.ui.components.SectionTitle
import com.studyplanner.app.ui.components.StudyCard
import com.studyplanner.app.ui.theme.TimerFontFamily
import com.studyplanner.app.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Deep Focus sections shown under the stopwatch when Deep Focus is switched on in the
 * Focus tab. Kept separate so the main screen stays readable.
 */

@Composable
internal fun StrictModeCard(
    settings: AppSettings,
    dndGranted: Boolean,
    onStrictChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onDndChange: (Boolean) -> Unit,
    onConfirmStopChange: (Boolean) -> Unit,
    onOpenDndSettings: () -> Unit,
) {
    StudyCard {
        SectionTitle(text = "Strict Focus")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Make it hard to walk away while a session is running.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SwitchRow(
            title = "Strict focus screen",
            subtitle = "Full-screen timer that blocks Back while a session runs",
            checked = settings.deepFocusStrict,
            onCheckedChange = onStrictChange,
        )
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        SwitchRow(
            title = "Keep screen on",
            subtitle = "Stop the display from sleeping during strict focus",
            checked = settings.deepFocusKeepScreenOn,
            onCheckedChange = onKeepScreenOnChange,
            enabled = settings.deepFocusStrict,
        )
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        SwitchRow(
            title = "Do Not Disturb",
            subtitle = "Mute notifications, not your focus sound",
            checked = settings.deepFocusDnd,
            onCheckedChange = onDndChange,
        )
        if (settings.deepFocusDnd && !dndGranted) {
            Spacer(Modifier.height(8.dp))
            PermissionWarning(
                text = "Notification policy access is needed for Do Not Disturb.",
                actionLabel = "Allow access",
                onAction = onOpenDndSettings,
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        SwitchRow(
            title = "Confirm before stopping",
            subtitle = "Ask once before ending a session",
            checked = settings.deepFocusConfirmStop,
            onCheckedChange = onConfirmStopChange,
        )
    }
}

@Composable
internal fun FocusSoundCard(
    settings: AppSettings,
    onSoundEnabledChange: (Boolean) -> Unit,
    onSelectBuiltin: (String?) -> Unit,
    onImport: () -> Unit,
    onRemove: () -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    StudyCard {
        SectionTitle(text = "Focus sounds")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Loop a bundled ambience or import your own audio. Imported files are copied into the app, so they play offline and add nothing to the APK.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SwitchRow(
            title = "Play during focus",
            subtitle = "Starts and stops with the session",
            checked = settings.soundEnabled,
            onCheckedChange = onSoundEnabledChange,
        )
        Spacer(Modifier.height(10.dp))

        RadioRow(
            title = "No sound",
            subtitle = "Silent session",
            selected = settings.builtinSound == null && settings.soundPath.isNullOrBlank(),
            onClick = { onSelectBuiltin(null) },
        )
        builtinSounds.forEach { sound ->
            RadioRow(
                title = sound.label,
                subtitle = "Built-in · plays offline",
                selected = settings.builtinSound == sound.id,
                onClick = {
                    onSelectBuiltin(if (settings.builtinSound == sound.id) null else sound.id)
                },
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        val importedPath = settings.soundPath
        if (!importedPath.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = settings.soundName ?: "Imported audio",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Imported · plays offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Remove sound",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        TextButton(onClick = onImport) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (importedPath.isNullOrBlank()) "Import your own audio" else "Replace imported audio")
        }

        if (settings.builtinSound != null || !importedPath.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Volume",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = settings.soundVolume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text(
                    "${(settings.soundVolume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun BlockingCard(
    settings: AppSettings,
    accessibilityEnabled: Boolean,
    onBlockingChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onPickApps: () -> Unit,
    onRemovePackage: (String) -> Unit,
    onClearPackages: () -> Unit,
) {
    val context = LocalContext.current
    StudyCard {
        SectionTitle(text = "App blocking")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "While a session is active, chosen apps are replaced by a Deep Focus screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SwitchRow(
            title = "Block distracting apps",
            subtitle = "Uses Android's accessibility service",
            checked = settings.blockingEnabled,
            onCheckedChange = onBlockingChange,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (accessibilityEnabled) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint = if (accessibilityEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (accessibilityEnabled) {
                    "Accessibility service is on"
                } else {
                    "Accessibility service is off"
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenAccessibility) {
                Text(if (accessibilityEnabled) "Manage" else "Enable")
            }
        }
        if (settings.blockingEnabled && !accessibilityEnabled) {
            PermissionWarning(
                text = "Turn on Study Planner's accessibility service to enforce blocking.",
                actionLabel = "Open settings",
                onAction = onOpenAccessibility,
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Blocked apps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    if (settings.blockedPackages.isEmpty()) {
                        "No apps chosen yet"
                    } else {
                        "${settings.blockedPackages.size} app${if (settings.blockedPackages.size == 1) "" else "s"} blocked"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onPickApps, shape = RoundedCornerShape(12.dp)) {
                Text("Choose apps")
            }
        }
        if (settings.blockedPackages.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            settings.blockedPackages.sorted().forEach { packageName ->
                val label = remember(packageName) { appLabel(context, packageName) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onRemovePackage(packageName) }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remove $label",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(onClick = onClearPackages) {
                Text("Clear all", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun YouTubeCard(
    settings: AppSettings,
    onModeChange: (YouTubeMode) -> Unit,
    onAddChannel: (String) -> Unit,
    onRemoveChannel: (String) -> Unit,
) {
    var channelInput by rememberSaveable { mutableStateOf("") }

    StudyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            SectionTitle(text = "YouTube")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "YouTube changes its interface often. This is a best-effort helper, not a hard guarantee.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        RadioRow(
            title = "Block YouTube entirely",
            subtitle = "Simple and reliable",
            selected = settings.youtubeMode == YouTubeMode.BLOCK_ALL,
            onClick = { onModeChange(YouTubeMode.BLOCK_ALL) },
        )
        RadioRow(
            title = "Study channels only",
            subtitle = "Blocks Shorts and other channels, keeps browsing usable",
            selected = settings.youtubeMode == YouTubeMode.STUDY_CHANNELS,
            onClick = { onModeChange(YouTubeMode.STUDY_CHANNELS) },
        )

        if (settings.youtubeMode == YouTubeMode.STUDY_CHANNELS) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Allowed channels",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            if (settings.youtubeChannels.isEmpty()) {
                Text(
                    "No channels added yet. Until you add one, YouTube stays blocked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                settings.youtubeChannels.sorted().forEach { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = channel,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveChannel(channel) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Remove $channel",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = channelInput,
                    onValueChange = { channelInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Channel name") },
                    placeholder = { Text("e.g. Khan Academy") },
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        onAddChannel(channelInput)
                        channelInput = ""
                    },
                    enabled = channelInput.isNotBlank(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add channel")
                }
            }
        }
    }
}

@Composable
internal fun AppPickerDialog(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps by produceState(initialValue = emptyList<InstalledApp>(), context) {
        value = withContext(Dispatchers.IO) { loadInstalledApps(context) }
    }
    val filtered = remember(apps, query) {
        if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Block apps",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                checked = selected.contains(app.packageName),
                                onToggle = { onToggle(app.packageName) },
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit) {
    val bitmap: Bitmap? = remember(app.packageName) {
        runCatching { app.icon?.toBitmap(96, 96) }.getOrNull()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(app.label.take(1), style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun RadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionWarning(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * Full-screen strict mode overlay. Shown by the root scaffold whenever a Deep Focus session
 * is active with strict mode enabled, so it follows the user across tabs.
 */
@Composable
fun StrictFocusOverlay(
    session: ActiveSession,
    now: Long,
    keepScreenOn: Boolean,
    confirmBeforeStop: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
) {
    var confirmEnd by remember { mutableStateOf(false) }
    val elapsed = session.elapsed(now)
    val view = LocalView.current

    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "STRICT FOCUS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = session.label,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = Dates.clock(elapsed),
                    fontFamily = TimerFontFamily,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (session.isRunning) {
                        "Stay with it. You're doing great."
                    } else {
                        "Paused"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                if (session.isRunning) {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Pause, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Pause")
                    }
                } else {
                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Resume")
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        if (confirmBeforeStop) confirmEnd = true else onEnd()
                    },
                ) {
                    Text("End session", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Back and app switching are discouraged until the session ends.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (confirmEnd) {
        ConfirmDialog(
            title = "End this session?",
            message = "Your progress will be saved to your schedule and focus history.",
            confirmLabel = "End session",
            onConfirm = {
                confirmEnd = false
                onEnd()
            },
            onDismiss = { confirmEnd = false },
        )
    }
}

internal fun appLabel(context: Context, packageName: String): String {
    return runCatching {
        val manager = context.packageManager
        val info = manager.getApplicationInfo(packageName, 0)
        manager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)
}

internal fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, FocusAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
