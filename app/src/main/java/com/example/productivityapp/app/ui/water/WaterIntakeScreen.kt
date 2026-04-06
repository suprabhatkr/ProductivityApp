package com.example.productivityapp.app.ui.water

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// icons replaced with emoji/text to avoid requiring material-icons dependency
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
// removed unused rememberSaveable import
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// removed compose viewModel import — ViewModel is provided by the Activity
import com.example.productivityapp.app.data.model.WaterEntry
import com.example.productivityapp.app.ui.theme.*
import com.example.productivityapp.app.viewmodel.WaterViewModel
import java.time.format.DateTimeFormatter

private val quickAmounts = listOf(
    150 to "\uD83C\uDF75", //small cup
    250 to "🥛",
    350 to "🍶",
    500 to "🥤"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterIntakeScreen(
    onBack: () -> Unit,
    viewModel: WaterViewModel
) {
    val data by viewModel.todayData.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedQuick by remember { mutableStateOf<Int?>(null) }
    var customText by remember { mutableStateOf("") }
    QuickSelectionClearEffect(selectedQuick) { selectedQuick = null }
    val keyboard = LocalSoftwareKeyboardController.current
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // If the ViewModel data refers to a previous date (e.g. app stayed open across
    // midnight), refresh immediately so UI shows the new empty day.
    LaunchedEffect(key1 = data.date) {
        val today = LocalDate.now().format(dateFormatter)
        if (data.date != today) {
            viewModel.refresh()
        }
    }

    // Date/time refresh is handled centrally by the Activity while the app is foreground.
    // We keep the local startup check (below) to ensure the UI shows the current day when
    // the composable is created.

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Water intake", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Today's progress", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = Color.White, fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue700,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top area: ring + quick add + controls (fixed, not part of scrollable log)
            Column(modifier = Modifier.fillMaxWidth()) {
                WaterRingHeader(
                    totalMl = data.totalMl,
                    goalMl = data.goalMl,
                    fraction = data.progressFraction
                )

                // Quick add section
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionLabel("QUICK ADD")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                                        quickAmounts.forEach { (ml, emoji) ->
                                            QuickAddButton(
                                                ml = ml,
                                                emoji = emoji,
                                                selected = selectedQuick == ml,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    // Immediately add the quick amount, scroll to top and show undo snackbar
                                                    selectedQuick = ml
                                                    coroutineScope.launch {
                                                        val id = viewModel.addWaterAndGetId(ml)
                                                        // scroll to top so the new item is visible
                                                        listState.animateScrollToItem(0)
                                                        // Auto-dismiss after 12 seconds: show as Indefinite and schedule a dismiss
                                                        val autoDismissJob = coroutineScope.launch {
                                                            delay(12_000)
                                                            snackbarHostState.currentSnackbarData?.dismiss()
                                                        }
                                                        val result = snackbarHostState.showSnackbar("Added ${ml}ml", actionLabel = "Undo", duration = SnackbarDuration.Indefinite)
                                                        autoDismissJob.cancel()
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.removeEntry(id)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Custom input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { customText = it.filter { c -> c.isDigit() } },
                            placeholder = { Text("Custom ml...", fontSize = 13.sp, color = TextTertiary) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                customText.toIntOrNull()?.let { viewModel.addWater(it) }
                                customText = ""
                                keyboard?.hide()
                            }),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue700,
                                unfocusedBorderColor = DividerColor,
                                focusedContainerColor = CardBg,
                                unfocusedContainerColor = CardBg
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        Button(
                                onClick = {
                                        val amt = selectedQuick ?: customText.toIntOrNull() ?: 0
                                        if (amt > 0) {
                                            selectedQuick = null
                                            customText = ""
                                            keyboard?.hide()
                                            coroutineScope.launch {
                                                val id = viewModel.addWaterAndGetId(amt)
                                                listState.animateScrollToItem(0)
                                                // Auto-dismiss after 12 seconds: show as Indefinite and schedule a dismiss
                                                val autoDismissJob = coroutineScope.launch {
                                                    delay(12_000)
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                }
                                                val result = snackbarHostState.showSnackbar("Added ${amt}ml", actionLabel = "Undo", duration = SnackbarDuration.Indefinite)
                                                autoDismissJob.cancel()
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.removeEntry(id)
                                                }
                                            }
                                        }
                                    },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = DividerColor
                )

                // Today's log header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("TODAY'S LOG")
                    Text("${data.entries.size} entries", fontSize = 11.sp, color = TextTertiary)
                }
            }

            // Scrollable log area — occupies remaining space and is independently scrollable
            // Provide the snackbar host above the scrollable log using a custom styled snackbar
            SnackbarHost(hostState = snackbarHostState) { data ->
                // Custom look: rounded white background, blue border, action button styled
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Blue700),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.visuals.message,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )

                        data.visuals.actionLabel?.let { action ->
                            TextButton(
                                onClick = { data.performAction() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Blue700),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 64.dp)
                            ) {
                                Text(action.uppercase(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (data.entries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No entries yet. Add water above!", fontSize = 13.sp, color = TextTertiary)
                        }
                    }
                }

                // Log entries — reversed so newest is first
                items(data.entries.reversed(), key = { it.id }) { entry ->
                    WaterLogItem(
                        entry = entry,
                        onDelete = { viewModel.removeEntry(entry.id) }
                    )
                }
            }
        }
    }
}

// Clear the quick selection shortly after a quick add so the UI doesn't stay selected
@Composable
private fun QuickSelectionClearEffect(selectedQuick: Int?, onClear: () -> Unit) {
    LaunchedEffect(selectedQuick) {
        if (selectedQuick != null) {
            delay(700)
            onClear()
        }
    }
}

@Composable
private fun WaterRingHeader(totalMl: Int, goalMl: Int, fraction: Float) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Blue700)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                RingChart(fraction = animatedFraction)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$totalMl",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text("ml", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${(fraction * 100).toInt()}% of $goalMl ml goal",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(goalMl - totalMl).coerceAtLeast(0)} ml remaining",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun RingChart(fraction: Float) {
    val sizeDp = 120
    val strokeDp = 12
    val sweepAngle = 360f * fraction
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(sizeDp.dp)
    ) {
        val stroke = strokeDp.dp.toPx()
        val inset = stroke / 2f

        // Background ring
        drawArc(
            color = Color.White.copy(alpha = 0.15f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = StrokeCap.Round
            ),
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        )

        // Progress ring
        if (sweepAngle > 0f) {
            drawArc(
                color = BlueLight,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = StrokeCap.Round
                ),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            )
        }
    }
}

@Composable
private fun QuickAddButton(ml: Int, emoji: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Blue50 else CardBg
        ),
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (selected) Modifier.border(1.dp, Blue700, RoundedCornerShape(10.dp))
                else Modifier.border(0.5.dp, DividerColor, RoundedCornerShape(10.dp))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                "${ml}ml",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) Blue700 else TextPrimary
            )
        }
    }
}

@Composable
private fun WaterLogItem(entry: WaterEntry, onDelete: () -> Unit) {
    val timeStr = entry.timestamp.format(DateTimeFormatter.ofPattern("h:mm a"))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
            .border(0.5.dp, DividerColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Blue50),
            contentAlignment = Alignment.Center
        ) {
            Text("💧", fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(timeStr, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            "+${entry.amountMl} ml",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Blue700
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Text("🗑️", fontSize = 16.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary,
        letterSpacing = 0.8.sp
    )
}
