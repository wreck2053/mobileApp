package com.mobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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

private enum class ControlIcon {
    CHIP,
    LIGHT,
    COLOR,
    FAN,
    POWER,
    COOL,
    PRESET,
    TURBO,
    SWING,
    LED,
    NIGHT_LAMP,
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
        var lightPower by remember { mutableStateOf(false) }
        var roomFanPower by remember { mutableStateOf(false) }
        var acMode by remember { mutableStateOf("cool") }
        var swingOn by remember { mutableStateOf(false) }
        var ledOn by remember { mutableStateOf(false) }
        var turboOn by remember { mutableStateOf(false) }
        var connected by remember { mutableStateOf(false) }
        var lastStatus by remember { mutableStateOf("READY") }
        var activeCommand by remember { mutableStateOf<String?>(null) }

        fun applyState(state: Esp32State) {
            lightPower = state.light
            roomFanPower = state.fan
            connected = state.connected
            acPower = state.ac.power
            acMode = state.ac.mode
            temperature = state.ac.temperature
            fanSpeed = FanSpeed.fromLevel(state.ac.fanLevel)
            swingOn = state.ac.swing
            ledOn = state.ac.led
            turboOn = state.ac.turbo
        }

        fun syncState(showFailure: Boolean = false) {
            scope.launch {
                val result = client.loadState()
                result.onSuccess { state ->
                    applyState(state)
                    if (lastStatus == "READY" || lastStatus == "OFFLINE") {
                        lastStatus = "Synced"
                    }
                }.onFailure {
                    connected = false
                    if (showFailure) {
                        lastStatus = "OFFLINE"
                    }
                }
            }
        }

        fun sendCommand(label: String, path: String) {
            activeCommand = label
            scope.launch {
                val result = client.send(path)
                if (result.isSuccess) {
                    lastStatus = "$label sent"
                    delay(300)
                    client.loadState().onSuccess(::applyState)
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Network error"
                    lastStatus = "$label failed"
                    snackbarHostState.showSnackbar("$label failed: $message")
                }
                activeCommand = null
            }
        }

        LaunchedEffect(Unit) {
            syncState(showFailure = true)
            while (true) {
                delay(5_000)
                syncState()
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
                lightPower = lightPower,
                roomFanPower = roomFanPower,
                connected = connected,
                acMode = acMode,
                swingOn = swingOn,
                ledOn = ledOn,
                turboOn = turboOn,
                lastStatus = lastStatus,
                activeCommand = activeCommand,
                onLight = { sendCommand("Light", Esp32Commands.TOGGLE_LIGHT) },
                onColor = { sendCommand("Color", Esp32Commands.NEXT_COLOR) },
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
                onTemperatureStep = { nextTemperature ->
                    temperature = nextTemperature
                    acPower = true
                    sendCommand("Temperature", Esp32Commands.temperature(nextTemperature))
                },
                onCool = {
                    acPower = true
                    sendCommand("Cool", Esp32Commands.MODE_COOL)
                },
                onPreset = {
                    acPower = true
                    sendCommand("Preset", Esp32Commands.PRESET_AC)
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
                    actionContentColor = Esp32Palette.Accent,
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
    lightPower: Boolean,
    roomFanPower: Boolean,
    connected: Boolean,
    acMode: String,
    swingOn: Boolean,
    ledOn: Boolean,
    turboOn: Boolean,
    lastStatus: String,
    activeCommand: String?,
    onLight: () -> Unit,
    onColor: () -> Unit,
    onFan: () -> Unit,
    onNightLamp: () -> Unit,
    onPower: () -> Unit,
    onTemperatureChange: (Int) -> Unit,
    onTemperatureCommit: () -> Unit,
    onTemperatureStep: (Int) -> Unit,
    onCool: () -> Unit,
    onPreset: () -> Unit,
    onTurbo: () -> Unit,
    onLed: () -> Unit,
    onSwing: () -> Unit,
    onFanSpeed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Header(connected = connected, lastStatus = lastStatus, activeCommand = activeCommand)

        RoomControls(
            lightPower = lightPower,
            roomFanPower = roomFanPower,
            onLight = onLight,
            onColor = onColor,
            onFan = onFan,
        )

        AcPanel(
            temperature = temperature,
            fanSpeed = fanSpeed,
            acPower = acPower,
            acMode = acMode,
            swingOn = swingOn,
            ledOn = ledOn,
            turboOn = turboOn,
            onPower = onPower,
            onTemperatureChange = onTemperatureChange,
            onTemperatureCommit = onTemperatureCommit,
            onTemperatureStep = onTemperatureStep,
            onCool = onCool,
            onPreset = onPreset,
            onTurbo = onTurbo,
            onLed = onLed,
            onSwing = onSwing,
            onFanSpeed = onFanSpeed,
        )

        WideCommandCard(
            title = "Night Lamp",
            detail = "Toggle bedside glow",
            icon = ControlIcon.NIGHT_LAMP,
            onClick = onNightLamp,
        )
    }
}

@Composable
private fun RoomControls(
    lightPower: Boolean,
    roomFanPower: Boolean,
    onLight: () -> Unit,
    onColor: () -> Unit,
    onFan: () -> Unit,
) {
    PanelFrame(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        cornerRadius = 20,
        contentPadding = 10,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CommandPill(
                    label = "Light",
                    icon = ControlIcon.LIGHT,
                    active = lightPower,
                    onClick = onLight,
                    modifier = Modifier.width(132.dp),
                )
                CommandPill(
                    label = "Color",
                    icon = ControlIcon.COLOR,
                    active = lightPower,
                    enabled = lightPower,
                    onClick = onColor,
                    modifier = Modifier.width(132.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CommandPill(
                    label = "Fan",
                    icon = ControlIcon.FAN,
                    active = roomFanPower,
                    onClick = onFan,
                    modifier = Modifier.width(150.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(
    connected: Boolean,
    lastStatus: String,
    activeCommand: String?,
) {
    PanelFrame(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
        cornerRadius = 24,
        contentPadding = 14,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    IconBadge(icon = ControlIcon.CHIP, sizeDp = 52, cornerRadiusDp = 16, vivid = true)
                    Column {
                        Text(
                            text = "ESP32",
                            color = Esp32Palette.Bone,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                        )
                        Text(
                            text = "ROOM CONTROL NODE",
                            color = Esp32Palette.AccentSoft,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF120E0C))
                        .border(1.dp, Esp32Palette.Stroke, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (connected) Esp32Palette.Online else Esp32Palette.Stroke),
                    )
                    Text(
                        text = if (connected) "ONLINE" else "OFFLINE",
                        color = Esp32Palette.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Esp32Palette.Stroke.copy(alpha = 0.55f)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "LOCAL ENDPOINT",
                        color = Esp32Palette.Muted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        text = Esp32Commands.BASE_URL,
                        color = Esp32Palette.Bone,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
                Text(
                    text = activeCommand ?: lastStatus,
                    color = if (lastStatus.contains("failed", ignoreCase = true)) {
                        Esp32Palette.Danger
                    } else {
                        Esp32Palette.Cyan
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AcPanel(
    temperature: Int,
    fanSpeed: FanSpeed,
    acPower: Boolean,
    acMode: String,
    swingOn: Boolean,
    ledOn: Boolean,
    turboOn: Boolean,
    onPower: () -> Unit,
    onTemperatureChange: (Int) -> Unit,
    onTemperatureCommit: () -> Unit,
    onTemperatureStep: (Int) -> Unit,
    onCool: () -> Unit,
    onPreset: () -> Unit,
    onTurbo: () -> Unit,
    onLed: () -> Unit,
    onSwing: () -> Unit,
    onFanSpeed: () -> Unit,
) {
    PanelFrame(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22,
        contentPadding = 12,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "AIR CONDITIONER",
                        color = Esp32Palette.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                    )
                    Text(
                        text = if (acPower) "ACTIVE COOLING" else "STANDBY",
                        color = if (acPower) Esp32Palette.Cyan else Esp32Palette.Stroke,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
                PowerButton(isOn = acPower, onClick = onPower)
            }

            TemperatureCard(
                temperature = temperature,
                acPower = acPower,
                acMode = acMode,
                fanSpeed = fanSpeed,
                onTemperatureChange = onTemperatureChange,
                onTemperatureCommit = onTemperatureCommit,
                onTemperatureStep = onTemperatureStep,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionTile("Cool", ControlIcon.COOL, onCool, Modifier.weight(1f), active = acMode.equals("cool", ignoreCase = true))
                ActionTile("Preset", ControlIcon.PRESET, onPreset, Modifier.weight(1f))
                ActionTile("Turbo", ControlIcon.TURBO, onTurbo, Modifier.weight(1f), active = turboOn)
            }

            FanSpeedSelector(
                selected = fanSpeed,
                onClick = onFanSpeed,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionTile("Swing", ControlIcon.SWING, onSwing, Modifier.weight(1f), active = swingOn)
                ActionTile("LED", ControlIcon.LED, onLed, Modifier.weight(1f), active = ledOn)
            }
        }
    }
}

@Composable
private fun TemperatureCard(
    temperature: Int,
    acPower: Boolean,
    acMode: String,
    fanSpeed: FanSpeed,
    onTemperatureChange: (Int) -> Unit,
    onTemperatureCommit: () -> Unit,
    onTemperatureStep: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF241F1B),
                        Color(0xFF171210),
                    ),
                ),
            )
            .border(1.dp, Esp32Palette.Stroke, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "TARGET TEMPERATURE",
                        color = Esp32Palette.Muted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp,
                    )
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = temperature.toString(),
                            color = Esp32Palette.Bone,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Light,
                            lineHeight = 54.sp,
                        )
                        Text(
                            text = "\u00B0C",
                            color = Esp32Palette.Accent,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (acPower) acMode.uppercase() else "STANDBY",
                        color = if (acPower) Esp32Palette.Cyan else Esp32Palette.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        text = "${fanSpeed.label.uppercase()} FAN  |  17-30",
                        color = Esp32Palette.Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StepButton(icon = Icons.Rounded.Remove, description = "Decrease temperature", onClick = {
                    onTemperatureStep((temperature - 1).coerceIn(17, 30))
                })
                Slider(
                    modifier = Modifier.weight(1f),
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
                StepButton(icon = Icons.Rounded.Add, description = "Increase temperature", onClick = {
                    onTemperatureStep((temperature + 1).coerceIn(17, 30))
                })
            }
        }
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Esp32Palette.Night)
            .border(1.dp, Esp32Palette.Stroke, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Esp32Palette.AccentSoft,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun FanSpeedSelector(
    selected: FanSpeed,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Esp32Palette.Night)
                .border(1.dp, Esp32Palette.Stroke, RoundedCornerShape(15.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FanSpeed.entries.forEach { speed ->
                val selectedColor by animateColorAsState(
                    targetValue = if (speed == selected) Esp32Palette.Copper else Color.Transparent,
                    label = "fanSpeedColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(selectedColor)
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = speed.label,
                        color = if (speed == selected) Esp32Palette.Bone else Esp32Palette.Muted,
                        fontSize = 14.sp,
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
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF211815))
            .border(1.5.dp, borderColor.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ControlIcon.POWER.imageVector(),
            contentDescription = if (isOn) "Turn air conditioner off" else "Turn air conditioner on",
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CommandPill(
    label: String,
    icon: ControlIcon,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColors = when {
        active -> listOf(Esp32Palette.Copper, Color(0xFF8A421D))
        enabled -> listOf(Color(0xFF1D1714), Color(0xFF120E0C))
        else -> listOf(Color(0xFF120F0D), Color(0xFF0D0B0A))
    }
    val borderColor = when {
        active -> Esp32Palette.Accent
        enabled -> Esp32Palette.Stroke
        else -> Esp32Palette.Stroke.copy(alpha = 0.45f)
    }
    val contentColor = when {
        active -> Esp32Palette.Bone
        enabled -> Esp32Palette.Muted
        else -> Esp32Palette.Muted.copy(alpha = 0.42f)
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(backgroundColors))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            IconBadge(
                icon = icon,
                sizeDp = 30,
                cornerRadiusDp = 10,
                active = active,
                enabled = enabled,
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (label != "Color") {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (active) Esp32Palette.Online else Esp32Palette.Stroke),
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: ControlIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Esp32Palette.CopperDark else Color(0xFF16120F))
            .border(
                1.dp,
                if (active) Esp32Palette.Accent.copy(alpha = 0.72f) else Esp32Palette.Stroke,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            IconBadge(
                icon = icon,
                compact = true,
                sizeDp = 26,
                cornerRadiusDp = 9,
                active = active,
            )
            Text(
                text = label,
                color = Esp32Palette.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WideCommandCard(
    title: String,
    detail: String,
    icon: ControlIcon,
    onClick: () -> Unit,
) {
    PanelFrame(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable(onClick = onClick),
        cornerRadius = 20,
        contentPadding = 10,
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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = detail,
                    color = Esp32Palette.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconBadge(icon = icon, sizeDp = 34, cornerRadiusDp = 11)
        }
    }
}

@Composable
private fun IconBadge(
    icon: ControlIcon,
    compact: Boolean = false,
    sizeDp: Int = if (compact) 30 else 36,
    cornerRadiusDp: Int = if (compact) 10 else 12,
    vivid: Boolean = false,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val badgeColor = when {
        vivid -> Esp32Palette.Red
        active -> Esp32Palette.Accent
        else -> Color(0xFF100C0A)
    }
    val iconColor = when {
        vivid || active -> Color.White
        enabled -> Esp32Palette.AccentSoft
        else -> Esp32Palette.Muted.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(badgeColor)
            .border(
                1.dp,
                if (vivid || active) Color.White.copy(alpha = 0.18f) else Esp32Palette.Accent.copy(alpha = 0.65f),
                RoundedCornerShape(cornerRadiusDp.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon.imageVector(),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size((sizeDp * 0.58f).dp),
        )
    }
}

private fun ControlIcon.imageVector(): ImageVector =
    when (this) {
        ControlIcon.CHIP -> Icons.Rounded.Memory
        ControlIcon.LIGHT -> Icons.Rounded.Lightbulb
        ControlIcon.COLOR -> Icons.Rounded.Palette
        ControlIcon.FAN -> Icons.Rounded.Air
        ControlIcon.POWER -> Icons.Rounded.PowerSettingsNew
        ControlIcon.COOL -> Icons.Rounded.AcUnit
        ControlIcon.PRESET -> Icons.Rounded.Tune
        ControlIcon.TURBO -> Icons.Rounded.Bolt
        ControlIcon.SWING -> Icons.Rounded.SwapVert
        ControlIcon.LED -> Icons.Rounded.LightMode
        ControlIcon.NIGHT_LAMP -> Icons.Rounded.Bedtime
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
                elevation = 10.dp,
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
