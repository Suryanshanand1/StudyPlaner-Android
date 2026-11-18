package com.studyplanner.app.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.MainActivity
import com.studyplanner.app.R
import com.studyplanner.app.data.FocusSessionManager
import com.studyplanner.app.data.SettingsRepository
import com.studyplanner.app.ui.EXTRA_OPEN_TAB
import com.studyplanner.app.ui.TAB_FOCUS
import com.studyplanner.app.ui.theme.StudyPlannerTheme

/**
 * Full-screen roadblock shown instead of a blocked app. There is intentionally no way back
 * to the blocked app: both buttons leave it, and Back returns to Study Planner.
 */
class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { returnToStudy() }

        val label = intent.getStringExtra(EXTRA_LABEL) ?: getString(R.string.block_youtube_label)
        val settings = SettingsRepository.get(this).settings.value

        setContent {
            val session by FocusSessionManager.session.collectAsStateWithLifecycle()
            LaunchedEffect(session) {
                if (session == null) finish()
            }
            StudyPlannerTheme(
                themeMode = settings.themeMode,
                accentHex = settings.accentColor,
            ) {
                BlockScreen(
                    label = label,
                    onReturn = ::returnToStudy,
                    onHome = ::goHome,
                )
            }
        }
    }

    private fun returnToStudy() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_FOCUS)
            },
        )
        finish()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "com.studyplanner.app.extra.BLOCKED_PACKAGE"
        const val EXTRA_LABEL = "com.studyplanner.app.extra.BLOCKED_LABEL"
    }
}

@Composable
private fun BlockScreen(label: String, onReturn: () -> Unit, onHome: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(42.dp),
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.block_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.block_message, label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = onReturn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.block_return),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onHome) {
                Text(androidx.compose.ui.res.stringResource(R.string.block_home))
            }
        }
    }
}
