package com.akula.akuclaude

import java.io.File
import java.util.zip.ZipFile

/** One wireframe "screen" guessed from a Compose source file. */
data class ScreenGuess(
    val fileName: String,
    val composables: List<String>,
    val buttons: List<String>,
    val texts: List<String>,
    val lists: Int,
    val images: Int
)

data class BugReport(val level: Level, val message: String) {
    enum class Level { ERROR, WARN, INFO }
}

data class ScanResult(
    val screens: List<ScreenGuess>,
    val bugs: List<BugReport>,
    val moduleName: String?,
    val applicationId: String?
)

/**
 * Reads an uploaded project .zip + a workflow .yml, with no Android SDK or emulator.
 * Produces a rough wireframe of each Compose screen plus a list of likely build problems.
 */
object ProjectScanner {

    private val composableRegex = Regex("""@Composable\s*\n\s*fun\s+(\w+)""")
    private val buttonRegex = Regex("""(Button|TextButton|IconButton|OutlinedButton|FloatingActionButton)\s*\(""")
    private val textLiteralRegex = Regex(""""((?:[^"\\]|\\.){1,60})"""")
    private val lazyListRegex = Regex("""LazyColumn|LazyRow|LazyVerticalGrid""")
    private val imageRegex = Regex("""\bImage\s*\(|AsyncImage\s*\(|Icon\s*\(""")

    fun scan(zipPath: String, ymlText: String): ScanResult {
        val bugs = mutableListOf<BugReport>()
        val screens = mutableListOf<ScreenGuess>()
        var applicationId: String? = null
        var moduleName: String? = null
        var manifestPackage: String? = null
        var hasManifest = false
        val kotlinFiles = mutableListOf<String>()

        val zip = try {
            ZipFile(zipPath)
        } catch (e: Exception) {
            bugs += BugReport(BugReport.Level.ERROR, "Could not open the .zip file: ${e.message}")
            return ScanResult(emptyList(), bugs, null, null)
        }

        zip.use { z ->
            val entries = z.entries().toList()
            if (entries.isEmpty()) {
                bugs += BugReport(BugReport.Level.ERROR, "The .zip file is empty.")
            }

            for (entry in entries) {
                if (entry.isDirectory) continue
                val name = entry.name

                if (name.endsWith("AndroidManifest.xml")) {
                    hasManifest = true
                    val text = z.getInputStream(entry).bufferedReader().readText()
                    Regex("""package="([\w.]+)"""").find(text)?.let { manifestPackage = it.groupValues[1] }
                    if (!text.contains("android.intent.action.MAIN")) {
                        bugs += BugReport(BugReport.Level.WARN, "$name has no LAUNCHER activity (MAIN/LAUNCHER intent-filter missing) — the app may not show an icon.")
                    }
                }

                if (name.endsWith("build.gradle.kts") || name.endsWith("build.gradle")) {
                    val text = z.getInputStream(entry).bufferedReader().readText()
                    Regex("""applicationId\s*=?\s*"([\w.]+)"""").find(text)?.let { applicationId = it.groupValues[1] }
                    if (name.contains("/app/") || name.startsWith("app/")) {
                        moduleName = "app"
                        if (!text.contains("compose = true") && text.contains("androidx.compose")) {
                            bugs += BugReport(BugReport.Level.WARN, "$name uses Compose dependencies but buildFeatures { compose = true } wasn't found.")
                        }
                    }
                }

                if (name.endsWith(".kt")) {
                    kotlinFiles += name
                    val text = try { z.getInputStream(entry).bufferedReader().readText() } catch (e: Exception) { "" }
                    if (text.contains("@Composable")) {
                        val composables = composableRegex.findAll(text).map { it.groupValues[1] }.toList()
                        if (composables.isNotEmpty()) {
                            val buttons = buttonRegex.findAll(text).map { it.groupValues[1] }.toList()
                            val texts = textLiteralRegex.findAll(text)
                                .map { it.groupValues[1] }
                                .filter { it.isNotBlank() && !it.contains("\\u") && it.length in 2..40 }
                                .distinct()
                                .take(6)
                                .toList()
                            val lists = lazyListRegex.findAll(text).count()
                            val images = imageRegex.findAll(text).count()
                            screens += ScreenGuess(
                                fileName = name.substringAfterLast('/'),
                                composables = composables,
                                buttons = buttons,
                                texts = texts,
                                lists = lists,
                                images = images
                            )
                        }
                    }
                    if (text.contains("findViewById") && text.contains("@Composable")) {
                        bugs += BugReport(BugReport.Level.WARN, "$name mixes findViewById with Compose code — check this isn't leftover View-system code.")
                    }
                    if (Regex("""context\s*=\s*null""").containsMatchIn(text)) {
                        bugs += BugReport(BugReport.Level.WARN, "$name passes a null Context — likely to NullPointerException at runtime.")
                    }
                }
            }
        }

        if (!hasManifest) {
            bugs += BugReport(BugReport.Level.ERROR, "No AndroidManifest.xml found in the .zip.")
        }
        if (screens.isEmpty()) {
            bugs += BugReport(BugReport.Level.WARN, "No @Composable screens were found — is this a Jetpack Compose project?")
        }
        if (applicationId != null && manifestPackage != null && applicationId != manifestPackage) {
            bugs += BugReport(
                BugReport.Level.INFO,
                "applicationId ($applicationId) differs from the manifest package ($manifestPackage) — usually fine, but check resource references."
            )
        }

        if (ymlText.isBlank()) {
            bugs += BugReport(BugReport.Level.WARN, "No build.yml content was provided — paste your workflow to check it against the .zip.")
        } else {
            if (!ymlText.contains("assembleDebug") && !ymlText.contains("assembleRelease") && !ymlText.contains("./gradlew") && !ymlText.contains("gradle ")) {
                bugs += BugReport(BugReport.Level.WARN, "build.yml doesn't seem to call a Gradle assemble task — the APK may never get built.")
            }
            if (!ymlText.contains("upload-artifact")) {
                bugs += BugReport(BugReport.Level.WARN, "build.yml has no upload-artifact step — you won't get a downloadable APK from the run.")
            }
            val javaVer = Regex("""java-version:\s*['"]?(\d+)""").find(ymlText)?.groupValues?.get(1)
            if (javaVer != null && javaVer.toInt() < 17) {
                bugs += BugReport(BugReport.Level.WARN, "build.yml sets up Java $javaVer, but AGP 8.x needs Java 17.")
            }
        }

        if (bugs.none { it.level == BugReport.Level.ERROR }) {
            bugs += BugReport(BugReport.Level.INFO, "No blocking errors found in this quick scan. This is not a full compile — real build errors can still occur.")
        }

        return ScanResult(screens, bugs, moduleName, applicationId)
    }
                          }
