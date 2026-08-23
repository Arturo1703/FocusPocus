package com.arturolopez.focuspocus.blocking

import android.app.TimePickerDialog
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arturolopez.focuspocus.R
import com.arturolopez.focuspocus.data.preferences.BlockingSchedule
import com.arturolopez.focuspocus.ui.theme.MysticPurple
import kotlin.math.roundToInt

@Composable
fun BlockingScreen(
    viewModel: ShortVideoAppsViewModel = viewModel(),
    appBlockingViewModel: AppBlockingViewModel = viewModel(),
) {
    val apps by viewModel.apps.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val blockableApps by appBlockingViewModel.apps.collectAsState()
    val appBlockingSchedule by appBlockingViewModel.schedule.collectAsState()
    val selectedCategory by appBlockingViewModel.selectedCategory.collectAsState()

    if (apps.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    ) {
        item(key = "short-blocking") {
            ShortBlockingSection(
                apps = apps,
                schedule = schedule,
                onToggle = viewModel::toggleSelection,
                onStartTimeSelected = viewModel::setStartMinutes,
                onEndTimeSelected = viewModel::setEndMinutes,
                onDayToggle = viewModel::toggleActiveDay,
            )
        }
        item(key = "app-blocking") {
            AppBlockingSection(
                apps = blockableApps,
                schedule = appBlockingSchedule,
                selectedCategory = selectedCategory,
                onCategorySelected = appBlockingViewModel::selectCategory,
                onToggle = appBlockingViewModel::toggleSelection,
                onStartTimeSelected = appBlockingViewModel::setStartMinutes,
                onEndTimeSelected = appBlockingViewModel::setEndMinutes,
                onDayToggle = appBlockingViewModel::toggleActiveDay,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppBlockingSection(
    apps: List<BlockableApp>,
    schedule: BlockingSchedule,
    selectedCategory: AppBlockCategory,
    onCategorySelected: (AppBlockCategory) -> Unit,
    onToggle: (String) -> Unit,
    onStartTimeSelected: (Int) -> Unit,
    onEndTimeSelected: (Int) -> Unit,
    onDayToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleApps = apps.filter { app -> app.category == selectedCategory }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Bloqueo de aplicaciones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            BlockingScheduleBar(
                schedule = schedule,
                onStartTimeSelected = onStartTimeSelected,
                onEndTimeSelected = onEndTimeSelected,
                onDayToggle = onDayToggle,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AppBlockCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        onClick = { onCategorySelected(category) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MysticPurple.copy(alpha = 0.82f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MysticPurple else Color.White.copy(alpha = 0.10f),
                        ),
                    ) {
                        Text(
                            text = category.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            if (visibleApps.isEmpty()) {
                Text(
                    text = "No hay aplicaciones en esta categoría.",
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val appCardWidth = (maxWidth - 20.dp) / 3

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        visibleApps.forEach { app ->
                            BlockableAppCard(
                                app = app,
                                onClick = { onToggle(app.packageName) },
                                modifier = Modifier.width(appCardWidth),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockableAppCard(
    app: BlockableApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grayscaleFilter = ColorFilter.colorMatrix(
        ColorMatrix().apply { setToSaturation(0f) },
    )

    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.72f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        border = if (app.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon.asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(48.dp),
                        colorFilter = if (app.isSelected) grayscaleFilter else null,
                    )
                }
                Text(
                    text = app.name,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = if (app.isSelected) "Bloqueada" else "Disponible",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (app.isSelected) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Bloqueada",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortBlockingSection(
    apps: List<ShortVideoApp>,
    schedule: BlockingSchedule,
    onToggle: (String) -> Unit,
    onStartTimeSelected: (Int) -> Unit,
    onEndTimeSelected: (Int) -> Unit,
    onDayToggle: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Bloqueo de shorts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            BlockingScheduleBar(
                schedule = schedule,
                onStartTimeSelected = onStartTimeSelected,
                onEndTimeSelected = onEndTimeSelected,
                onDayToggle = onDayToggle,
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Extra breathing room prevents pixel rounding from wrapping the third card.
                val appCardWidth = (maxWidth - 20.dp) / 3

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3,
                ) {
                    apps.forEach { app ->
                        ShortVideoAppCard(
                            app = app,
                            onClick = { onToggle(app.id) },
                            modifier = Modifier.width(appCardWidth),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockingScheduleBar(
    schedule: BlockingSchedule,
    onStartTimeSelected: (Int) -> Unit,
    onEndTimeSelected: (Int) -> Unit,
    onDayToggle: (Int) -> Unit,
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Días activos",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DAY_LABELS.forEach { (day, label) ->
                    val isSelected = day in schedule.activeDays
                    Surface(
                        onClick = { onDayToggle(day) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MysticPurple.copy(alpha = 0.82f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) {
                                MysticPurple
                            } else {
                                Color.White.copy(alpha = 0.10f)
                            },
                        ),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 7.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Text(
                text = "Horario",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeSelector(
                    label = "Desde",
                    minutes = schedule.startMinutes,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showTimePicker(context, schedule.startMinutes, onStartTimeSelected)
                    },
                )
                Text(text = "—", color = Color.White.copy(alpha = 0.65f))
                TimeSelector(
                    label = "Hasta",
                    minutes = schedule.endMinutes,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showTimePicker(context, schedule.endMinutes, onEndTimeSelected)
                    },
                )
            }
        }
    }
}

@Composable
private fun TimeSelector(
    label: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.60f),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = formatTime(minutes),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun showTimePicker(
    context: Context,
    initialMinutes: Int,
    onTimeSelected: (Int) -> Unit,
) {
    val dialog = TimePickerDialog(
        context,
        R.style.Theme_FocusPocus_TimePicker,
        { _, hour, minute -> onTimeSelected(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        true,
    )
    dialog.setOnShowListener {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val horizontalMargins = (48 * context.resources.displayMetrics.density).roundToInt()
        dialog.window?.setLayout(
            screenWidth - horizontalMargins,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }
    dialog.show()
}

private fun formatTime(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)

private val DAY_LABELS = listOf(
    1 to "L",
    2 to "M",
    3 to "X",
    4 to "J",
    5 to "V",
    6 to "S",
    7 to "D",
)

@Composable
private fun ShortVideoAppCard(
    app: ShortVideoApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grayscaleFilter = ColorFilter.colorMatrix(
        ColorMatrix().apply { setToSaturation(0f) },
    )
    val isGrayscale = !app.isInstalled || app.isSelected

    Card(
        onClick = onClick,
        enabled = app.isInstalled,
        modifier = modifier.aspectRatio(0.72f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        ),
        border = if (app.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (app.isInstalled) 1f else 0.45f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon.asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(48.dp),
                        colorFilter = if (isGrayscale) grayscaleFilter else null,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = app.name.take(1),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = app.name,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )

                Text(
                    text = when {
                        !app.isInstalled -> "No instalada"
                        app.isSelected -> "Seleccionada"
                        else -> "Disponible"
                    },
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (app.isSelected) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Seleccionada",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
