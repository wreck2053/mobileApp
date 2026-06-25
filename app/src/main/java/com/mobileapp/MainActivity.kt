package com.mobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Esp32Palette.Night.toArgb()
        window.navigationBarColor = Esp32Palette.Night.toArgb()

        setContent {
            Esp32ControllerApp()
        }
    }
}

private object Esp32Palette {
    val Night = Color(0xFF070605)
    val DeepPanel = Color(0xFF120E0C)
    val Panel = Color(0xFF1B1714)
    val PanelRaised = Color(0xFF261F1A)
    val Copper = Color(0xFF5A2D16)
    val CopperDark = Color(0xFF321B11)
    val Accent = Color(0xFFFF8733)
    val AccentSoft = Color(0xFFFFB27D)
    val Bone = Color(0xFFF6EFE9)
    val Muted = Color(0xFFC8B4A6)
    val Stroke = Color(0xFF514139)
    val Red = Color(0xFFEA1745)
    val Danger = Color(0xFFFF5E57)
    val Online = Color(0xFF40D98A)
    val Cyan = Color(0xFF55D6BE)
}

@Composable
private fun Esp32ControllerApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Esp32Palette.Night,
            surface = Esp32Palette.Panel,
            primary = Esp32Palette.Accent,
            onPrimary = Esp32Palette.Night,
            onSurface = Esp32Palette.Bone,
        ),
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val client = remember { Esp32CommandClient() }

        var temperature by remember { mutableIntStateOf(24) }
        var fanSpeed by remember { mutableStateOf(FanSpeed.LOW) }
        var acPower by remember { mutableStateOf(false) }
        var lastStatus by remember { mutableStateOf("READY") }
        var activeCommand by remember { mutableStateOf<String?>(null) }

        fun sendCommand(label: String, path: String) {
            activeCommand = label
            scope.launch {
                val result = client.send(path)
                if (result.isSuccess) {
                    lastStatus = "$label sent"
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Network error"
                    lastStatus = "$label failed"
                    snackbarHostState.showSnackbar("$label failed: $message")
                }
                activeCommand = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF050404),
                            Color(0xFF130C08),
                            Esp32Palette.Night,
                        ),
                    ),
                ),
        ) {
            ControllerScreen(
                temperature = temperature,
                fanSpeed = fanSpeed,
                acPower = acPower,
                lastStatus = lastStatus,
                activeCommand = activeCommand,
                onLight = { sendCommand("Light", Esp32Commands.TOGGLE_LIGHT) },
                onFan = { sendCommand("Fan", Esp32Commands.TOGGLE_FAN) },
                onNightLamp = { sendCommand("Night lamp", Esp32Commands.TOGGLE_NIGHT_LAMP) },
                onPower = {
                    val nextPower = !acPower
                    acPower = nextPower
                    sendCommand(
                        "A/C power",
                        if (nextPower) Esp32Commands.temperature(temperature) else Esp32Commands.POWER_OFF,
                    )
                },
                onTemperatureChange = { temperature = it },
                onTemperatureCommit = {
                    acPower = true
                    sendCommand("Temperature", Esp32Commands.temperature(temperature))
                },
                onCool = {
                    acPower = true
                    sendCommand("Cool", Esp32Commands.MODE_COOL)
                },
                onTurbo = { sendCommand("Turbo", Esp32Commands.STATE_TURBO) },
                onLed = { sendCommand("LED", Esp32Commands.STATE_LED) },
                onSwing = { sendCommand("Swing", Esp32Commands.STATE_SWING) },
                onFanSpeed = {
                    val nextSpeed = fanSpeed.next()
                    fanSpeed = nextSpeed
                    acPower = true
                    sendCommand("Fan speed ${nextSpeed.label}", Esp32Commands.fan(nextSpeed))
                },
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) { data ->
                Snackbar(
                    containerColor = Color(0xFF371513),
                    contentColor = Esp32Palette.Bone,
                    actionColor = Esp32Palette.Accent,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(data.visuals.message)
                }
            }
        }
    }
}

@Composable
private fun ControllerScreen(
    temperature: Int,
    fanSpeed: FanSpeed,
    acPower: Boolean,
    lastStatus: String,
    activeCommand: String?,
    onLight: () -> Unit,
    onFan: () -> Unit,
    onNightLamp: () -> Unit,
    onPower: () -> Unit,
    onTemperatureChange: (Int) -> Unit,
    onTemperatureCommit: () -> Unit,
    onCool: () -> Unit,
    onTurbo: () -> Unit,
    onLed: () -> Unit,
    onSwing: () -> Unit,
    onFanSpeed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Header(lastStatus = lastStatus, activeCommand = activeCommand)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CommandPill(
                label = "Light",
                symbol = "L",
                onClick = onLight,
                modifier = Modifier.weight(1f),
            )
            CommandPill(
                label = "Fan",
                symbol = "F",
                onClick = onFan,
                modifier = Modifier.weight(1f),
            )
        }

        AcPanel(
            temperature = temperature,
            fanSpeed = fanSpeed,
            acPower = acPower,
            onPower = onPower,
            onTemperatureChange = onTemperatureChange,
            onTemperatureCommit = onTemperatureCommit,
            onCool = onCool,
            onTurbo = onTurbo,
            onLed = onLed,
            onSwing = onSwing,
            onFanSpeed = onFanSpeed,
        )

        WideCommandCard(
            title = "Night Lamp",
            detail = "Toggle bedside glow",
            symbol = "NL",
            onClick = onNightLamp,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun Header(
    lastStatus: String,
    activeCommand: String?,
) {
    PanelFrame(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 30,
        contentPadding = 20,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                ChipIcon()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BEDROOM",
                        color = Esp32Palette.Bone,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = Esp32Commands.BASE_URL,
                        color = Esp32Palette.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Esp32Palette.Online),
                    )
                    Text(
                        text = "ONLINE",
                        color = Esp32Palette.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
                Text(
                    text = activeCommand ?: lastStatus,
                    color = if (lastStatus.contains("failed", ignoreCase = true)) {
                        Esp32Palette.Danger
                    } else {
                        Esp32Palette.Cyan
                    },
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChipIcon() {
    Box(
        modifier = Modifier
            .size(58.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Esp32Palette.Red)
            .clip(RoundedCornerShape(18.dp))
            .background(Esp32Palette.Red),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(38.dp)) {
            val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
                size = Size(size.width * 0.56f, size.height * 0.56f),
                style = stroke,
            )
            drawLine(Color.White, Offset(0f, size.height * 0.3f), Offset(size.width * 0.18f, size.height * 0.3f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(0f, size.height * 0.5f), Offset(size.width * 0.18f, size.height * 0.5f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(0f, size.height * 0.7f), Offset(size.width * 0.18f, size.height * 0.7f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.82f, size.height * 0.3f), Offset(size.width, size.height * 0.3f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.82f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.82f, size.height * 0.7f), Offset(size.width, size.height * 0.7f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.34f, 0f), Offset(size.width * 0.34f, size.height * 0.18f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.5f, 0f), Offset(size.width * 0.5f, size.height * 0.18f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.66f, size.height * 0.18f), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.34f, size.height * 0.82f), Offset(size.width * 0.34f, size.height), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.5f, size.height * 0.82f), Offset(size.width * 0.5f, size.height), strokeWidth = 3.5f)
            drawLine(Color.White, Offset(size.width * 0.66f, size.height * 0.82f), Offset(size.width * 0.66f, size.height), strokeWidth = 3.5f)
        }
    }
}

@Composable
private fun AcPanel(
    temperature: Int,
    fanSpeed: FanSpeed,
    acPower: Boolean,
    onPower: () -> Unit,
    onTemperatureChange: (Int) -> Unit,
    onTemperatureCommit: () -> Unit,
    onCool: () -> Unit,
    onTurbo: () -> Unit,
    onLed: () -> Unit,
    onSwing: () -> Unit,
    onFanSpeed: () -> Unit,
) {
    PanelFrame(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26,
        contentPadding = 18,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "AIR CONDITIONER",
                        color = Esp32Palette.Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                    )
                    Text(
                        text = if (acPower) "ACTIVE COOLING" else "STANDBY",
                        color = if (acPower) Esp32Palette.Cyan else Esp32Palette.Stroke,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
                PowerButton(isOn = acPower, onClick = onPower)
            }

            TemperatureCard(
                temperature = temperature,
                onTemperatureChange = onTemperatureChange,
                onTemperatureCommit = onTemperatureCommit,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionTile("Cool", "CO", onCool, Modifier.weight(1f))
                ActionTile("Turbo", "TB", onTurbo, Modifier.weight(1f))
            }

            FanSpeedSelector(
                selected = fanSpeed,
                onClick = onFanSpeed,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionTile("Swing", "SW", onSwing, Modifier.weight(1f))
                ActionTile("LED", "LD", onLed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TemperatureCard(
    temperature: Int,
    onTemperatureChange: (Int) -> Unit,
    onTemperatureCommit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF241F1B),
                        Color(0xFF171210),
                    ),
                ),
            )
            .border(1.dp, Esp32Palette.Stroke, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "TEMPERATURE",
                        color = Esp32Palette.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                    )
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = temperature.toString(),
                            color = Esp32Palette.Bone,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Light,
                            lineHeight = 74.sp,
                        )
                        Text(
                            text = "\u00B0",
                            color = Esp32Palette.Accent,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
                Text(
                    text = "17 - 30 C",
                    color = Esp32Palette.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Slider(
                value = temperature.toFloat(),
                onValueChange = { onTemperatureChange(it.roundToInt().coerceIn(17, 30)) },
                onValueChangeFinished = onTemperatureCommit,
                valueRange = 17f..30f,
                steps = 12,
                colors = SliderDefaults.colors(
                    thumbColor = Esp32Palette.Accent,
                    activeTrackColor = Esp32Palette.Accent,
                    inactiveTrackColor = Esp32Palette.Stroke,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun FanSpeedSelector(
    selected: FanSpeed,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "FAN",
            color = Esp32Palette.Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Esp32Palette.Night)
                .border(1.dp, Esp32Palette.Stroke, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            FanSpeed.entries.forEach { speed ->
                val selectedColor by animateColorAsState(
                    targetValue = if (speed == selected) Esp32Palette.Copper else Color.Transparent,
                    label = "fanSpeedColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(selectedColor)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = speed.label,
                        color = if (speed == selected) Esp32Palette.Bone else Esp32Palette.Muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerButton(
    isOn: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isOn) Esp32Palette.Online else Esp32Palette.Danger,
        label = "powerBorder",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isOn) Esp32Palette.Online else Esp32Palette.Danger,
        label = "powerContent",
    )

    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(Color(0xFF211815))
            .border(1.5.dp, borderColor.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "I/O",
            color = contentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun CommandPill(
    label: String,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Esp32Palette.CopperDark, Esp32Palette.Copper),
                ),
            )
            .border(1.dp, Esp32Palette.Accent.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SymbolBadge(symbol = symbol)
            Text(
                text = label,
                color = Esp32Palette.Bone,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionTile(
    label: String,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF16120F))
            .border(1.dp, Esp32Palette.Stroke, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SymbolBadge(symbol = symbol, compact = true)
            Text(
                text = label,
                color = Esp32Palette.Muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun WideCommandCard(
    title: String,
    detail: String,
    symbol: String,
    onClick: () -> Unit,
) {
    PanelFrame(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 24,
        contentPadding = 18,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = title,
                    color = Esp32Palette.Bone,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = detail,
                    color = Esp32Palette.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            SymbolBadge(symbol = symbol)
        }
    }
}

@Composable
private fun SymbolBadge(
    symbol: String,
    compact: Boolean = false,
) {
    val size = if (compact) 34.dp else 42.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(Color(0xFF100C0A))
            .border(1.dp, Esp32Palette.Accent.copy(alpha = 0.65f), RoundedCornerShape(if (compact) 12.dp else 14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = Esp32Palette.AccentSoft,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PanelFrame(
    modifier: Modifier = Modifier,
    cornerRadius: Int,
    contentPadding: Int,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(cornerRadius.dp),
                ambientColor = Esp32Palette.Accent.copy(alpha = 0.08f),
                spotColor = Esp32Palette.Accent.copy(alpha = 0.10f),
            )
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Esp32Palette.PanelRaised,
                        Esp32Palette.DeepPanel,
                    ),
                ),
            )
            .border(1.dp, Esp32Palette.Stroke, RoundedCornerShape(cornerRadius.dp))
            .padding(contentPadding.dp),
    ) {
        content()
    }
}
