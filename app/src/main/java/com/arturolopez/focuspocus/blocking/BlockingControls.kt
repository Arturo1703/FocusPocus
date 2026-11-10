package com.arturolopez.focuspocus.blocking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arturolopez.focuspocus.ui.theme.MysticPurple
import kotlin.math.ceil

internal sealed interface BlockingPendingAction {
    data object Apply : BlockingPendingAction
    data class Pause(val durationMinutes: Int) : BlockingPendingAction
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BlockingQuickActions(
    pendingAction: BlockingPendingAction?,
    pauseRemainingMillis: Long,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val waiting = pendingAction != null

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val buttonWidth = (maxWidth - 16.dp) / 3
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3,
        ) {
            BlockingActionButton(
                label = "Cargar perfil",
                icon = Icons.Default.Add,
                enabled = false,
                onClick = {},
                modifier = Modifier.width(buttonWidth),
            )
            BlockingActionButton(
                label = "Guardar perfil",
                icon = Icons.Default.BookmarkAdd,
                enabled = false,
                onClick = {},
                modifier = Modifier.width(buttonWidth),
            )
            BlockingActionButton(
                label = if (pauseRemainingMillis > 0L) {
                    "Pausa ${ceil(pauseRemainingMillis / 60_000.0).toInt()} min"
                } else {
                    "Pausar"
                },
                icon = Icons.Default.PauseCircle,
                enabled = !waiting && pauseRemainingMillis <= 0L,
                onClick = onPauseClick,
                modifier = Modifier.width(buttonWidth),
            )
        }
    }
}

@Composable
internal fun BlockingApplyButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MUTED_APPLY_PURPLE,
            contentColor = Color.Black,
            disabledContainerColor = MUTED_APPLY_PURPLE.copy(alpha = 0.42f),
            disabledContentColor = Color.Black.copy(alpha = 0.48f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
    ) {
        Text(text = "Aplicar", fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun BlockingWaitIndicator(
    pendingAction: BlockingPendingAction,
    secondsRemaining: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = when (pendingAction) {
                BlockingPendingAction.Apply ->
                    "Respira. Los cambios se aplicarán en $secondsRemaining segundos."
                is BlockingPendingAction.Pause ->
                    "Respira. La pausa comenzará en $secondsRemaining segundos."
            },
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        LinearProgressIndicator(
            progress = { (45 - secondsRemaining).coerceIn(0, 45) / 45f },
            modifier = Modifier.fillMaxWidth(),
            color = MysticPurple.copy(alpha = 0.72f),
            trackColor = Color.White.copy(alpha = 0.12f),
        )
        Text(
            text = "Mantén esta pantalla abierta",
            color = Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BlockingActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 72.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.24f else 0.10f)),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
internal fun PauseDurationDialog(
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duración de la pausa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$durationMinutes minutos",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MysticPurple,
                )
                Slider(
                    value = durationMinutes.toFloat(),
                    onValueChange = { onDurationChange(it.toInt().coerceIn(1, 15)) },
                    valueRange = 1f..15f,
                    steps = 13,
                )
                Text(
                    text = "La espera consciente de 45 segundos comenzará al confirmar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Comenzar espera") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private val MUTED_APPLY_PURPLE = Color(0xFF9B72B3)
