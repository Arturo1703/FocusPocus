package com.arturolopez.focuspocus.blocking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturolopez.focuspocus.ui.theme.MysticPurple
import kotlin.math.ceil

internal sealed interface BlockingPendingAction {
    class Apply : BlockingPendingAction
    data class Pause(val durationMinutes: Int) : BlockingPendingAction
}

internal const val APPLY_WAIT_SECONDS = 40
internal const val PAUSE_WAIT_SECONDS = 45

@Composable
internal fun BlockingQuickActions(
    pendingAction: BlockingPendingAction?,
    pauseRemainingMillis: Long,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val waiting = pendingAction != null

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BlockingActionButton(
            label = "Cargar perfil",
            icon = Icons.Default.Add,
            enabled = false,
            onClick = {},
            modifier = Modifier.weight(1f),
        )
        BlockingActionButton(
            label = "Guardar perfil",
            icon = Icons.Default.BookmarkAdd,
            enabled = false,
            onClick = {},
            modifier = Modifier.weight(1f),
        )
        BlockingActionButton(
            label = if (pauseRemainingMillis > 0L) {
                "En pausa\n${ceil(pauseRemainingMillis / 60_000.0).toInt()} min"
            } else {
                "Pausar"
            },
            icon = Icons.Default.PauseCircle,
            enabled = !waiting && pauseRemainingMillis <= 0L,
            onClick = onPauseClick,
            modifier = Modifier.weight(1f),
        )
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
            containerColor = ACTIVE_APPLY_YELLOW,
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
internal fun BlockingApplyCountdownDialog(
    secondsRemaining: Int,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Aplicar cambios") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$secondsRemaining s",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    progress = { (APPLY_WAIT_SECONDS - secondsRemaining) / APPLY_WAIT_SECONDS.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Los cambios se aplicarán al terminar la cuenta atrás. Si cierras el diálogo, quedarán pendientes.")
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Cerrar") }
        },
    )
}

@Composable
internal fun BlockingPauseWaitIndicator(
    secondsRemaining: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Respira. La pausa comenzará en $secondsRemaining segundos.",
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        LinearProgressIndicator(
            progress = { (PAUSE_WAIT_SECONDS - secondsRemaining) / PAUSE_WAIT_SECONDS.toFloat() },
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
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
                    text = "Tras $PAUSE_WAIT_SECONDS segundos de espera se pausarán los bloqueos de apps y vídeos cortos. " +
                        "Se reactivarán automáticamente al terminar la pausa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Iniciar cuenta atrás") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private val ACTIVE_APPLY_YELLOW = Color(0xFFFFD54F)
private val MUTED_APPLY_PURPLE = Color(0xFF9B72B3)
