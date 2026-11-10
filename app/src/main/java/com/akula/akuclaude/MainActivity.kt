package com.akula.akuclaude

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AkuTheme {
                AkuApp(this)
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 24,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x26FFFFFF), Color(0x0DFFFFFF))
                )
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(corner.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun AkuApp(activity: ComponentActivity) {
    var showIntro by remember { mutableStateOf(true) }
    var zipUri by remember { mutableStateOf<Uri?>(null) }
    var zipName by remember { mutableStateOf<String?>(null) }
    var ymlText by remember { mutableStateOf("") }
    var showDisplayer by remember { mutableStateOf(true) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var shizukuStatus by remember { mutableStateOf(ShizukuBridge.checkStatus(activity)) }

    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            zipUri = uri
            zipName = queryFileName(activity, uri) ?: "project.zip"
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NightBlueDeep, NightBlue, Color(0xFF0C223F)))
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Header()
            Spacer(Modifier.height(20.dp))

            if (showDisplayer) {
                WideXButton(label = "Close preview") { showDisplayer = false }
                Spacer(Modifier.height(10.dp))
                Displayer(scanResult = scanResult, zipName = zipName)
            } else {
                WideXButton(label = "Show preview", filled = false) { showDisplayer = true }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Add file (.zip)")
            Spacer(Modifier.height(8.dp))
            FilePickerRow(
                fileName = zipName,
                onAdd = { zipPicker.launch("application/zip") },
                onDelete = { zipUri = null; zipName = null; scanResult = null }
            )

            Spacer(Modifier.height(22.dp))
            SectionLabel("build.yml")
            Spacer(Modifier.height(8.dp))
            YmlBox(ymlText) { ymlText = it }

            Spacer(Modifier.height(24.dp))
            RunButton(enabled = zipUri != null) {
                val tmp = File(activity.cacheDir, "incoming.zip")
                activity.contentResolver.openInputStream(zipUri!!)?.use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                scanResult = ProjectScanner.scan(tmp.absolutePath, ymlText)
                showDisplayer = true
            }

            Spacer(Modifier.height(28.dp))
            ShizukuRow(
                status = shizukuStatus,
                onRefresh = { shizukuStatus = ShizukuBridge.checkStatus(activity) }
            )
            Spacer(Modifier.height(24.dp))
        }

        if (showIntro) {
            IntroPopup(onClose = { showIntro = false })
        }
    }
}

@Composable
fun IntroPopup(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.85f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f)
        ) {
            GlassCard(corner = 28) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Hi 👋", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "We are Akula Team, thanks for using this app.",
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "All of this thanks to Borath Sovannroth and Claude AI.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun Header() {
    Column {
        Text(
            "GitHub-AkuClaude App",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "GitHub preview · save time · no ads",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Spacer(
                Modifier
                    .width(64.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(AccentBlue, AccentBlueSoft)))
            )
        }
    }
}

@Composable
fun WideXButton(label: String, filled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) GlassWhite else Color.Transparent)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .clickableNoRipple(onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun Displayer(scanResult: ScanResult?, zipName: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Color(0x33234A82), Color(0x1A0A1B33))))
            .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
            .padding(18.dp)
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (scanResult == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (zipName == null) "Add a .zip and tap Run it!!" else "Tap Run it!! to preview $zipName",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Text("Preview — ${scanResult.screens.size} screen(s) found", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                scanResult.screens.forEach { screen -> ScreenWireframe(screen) ; Spacer(Modifier.height(14.dp)) }

                Spacer(Modifier.height(6.dp))
                Text("Bug report", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                scanResult.bugs.forEach { bug -> BugRow(bug) }
            }
        }
    }
}

@Composable
fun ScreenWireframe(screen: ScreenGuess) {
    GlassCard(corner = 16, modifier = Modifier.fillMaxWidth()) {
        Text(screen.fileName, color = AccentBlueSoft, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        screen.texts.take(3).forEach {
            Spacer(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GlassWhite)
            )
            Spacer(Modifier.height(6.dp))
        }
        if (screen.lists > 0) {
            repeat(2) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassWhite)
                )
                Spacer(Modifier.height(6.dp))
            }
        }
        if (screen.buttons.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                screen.buttons.take(3).forEach {
                    Spacer(
                        Modifier
                            .width(54.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AccentBlue.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun BugRow(bug: BugReport) {
    val color = when (bug.level) {
        BugReport.Level.ERROR -> Color(0xFFFF6B6B)
        BugReport.Level.WARN -> Color(0xFFFFC94D)
        BugReport.Level.INFO -> TextSecondary
    }
    Row(Modifier.padding(vertical = 4.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
                .align(Alignment.CenterVertically)
        )
        Spacer(Modifier.width(8.dp))
        Text(bug.message, color = TextPrimary, fontSize = 12.sp)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun FilePickerRow(fileName: String?, onAdd: () -> Unit, onDelete: () -> Unit) {
    GlassCard(corner = 18, modifier = Modifier.fillMaxWidth()) {
        if (fileName == null) {
            Row(
                Modifier.fillMaxWidth().clickableNoRipple(onAdd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = AccentBlueSoft)
                Spacer(Modifier.width(10.dp))
                Text("Add file (.zip)", color = TextPrimary, fontSize = 14.sp)
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FolderZip, contentDescription = null, tint = AccentBlueSoft)
                    Spacer(Modifier.width(10.dp))
                    Text(fileName, color = TextPrimary, fontSize = 14.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove file", tint = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

@Composable
fun YmlBox(value: String, onChange: (String) -> Unit) {
    GlassCard(corner = 18, modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("paste your build.yml here", color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = AccentBlueSoft
            )
        )
    }
}

@Composable
fun RunButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    if (enabled) listOf(RunGreen, Color(0xFF1FAE7A)) else listOf(Color(0xFF2A3A4A), Color(0xFF223040))
                )
            )
            .border(1.dp, if (enabled) Color(0x5534D399) else GlassBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Run it!!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ShizukuRow(status: ShizukuBridge.Status, onRefresh: () -> Unit) {
    val (label, color) = when (status) {
        ShizukuBridge.Status.CONNECTED -> "Shizuku connected" to RunGreen
        ShizukuBridge.Status.NOT_CONNECTED -> "Shizuku not connected" to Color(0xFFFFC94D)
        ShizukuBridge.Status.NOT_INSTALLED -> "Shizuku not installed" to Color(0xFFFF6B6B)
    }
    GlassCard(corner = 16, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(label, color = TextPrimary, fontSize = 13.sp)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TextSecondary)
            }
        }
    }
}

fun queryFileName(activity: ComponentActivity, uri: Uri): String? {
    val cursor = activity.contentResolver.query(uri, null, null, null, null)
    cursor.use { c ->
        if (c != null && c.moveToFirst()) {
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return c.getString(idx)
        }
    }
    return uri.lastPathSegment
}
