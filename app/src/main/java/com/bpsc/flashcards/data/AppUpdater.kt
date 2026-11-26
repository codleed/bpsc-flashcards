package com.bpsc.flashcards.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Where the published release manifest lives (same Netlify site as the cards).
 *
 * The `BuildConfig.DEBUG` branch is a test hook so the update flow can be
 * exercised without publishing a release: run `node tools/mock-update.mjs`, which
 * serves a fake version 9.9. Use 10.0.2.2 for an emulator, or this PC's LAN IP
 * for a real phone on the same Wi-Fi.
 */
// Not a `const`: BuildConfig.DEBUG is a build-time field, not a compile-time
// constant, so this is read once on first use.
val REMOTE_VERSION_URL: String =
    if (com.bpsc.flashcards.BuildConfig.DEBUG) "http://192.168.100.83:8787/version.json"
    else "https://bpsc-qbank.netlify.app/app/version.json"

/** A published release, as described by version.json. */
data class ReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String?,
    val url: String,
    val sha256: String?,
)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus

    /** A newer release than the installed one. */
    data class Available(val release: ReleaseInfo) : UpdateStatus

    /** Installed app is current (or the check failed; both mean "nothing to do"). */
    data object UpToDate : UpdateStatus

    /** Downloading, with the fraction of bytes received so far (0..1). */
    data class Downloading(val progress: Float) : UpdateStatus
    data class Downloaded(val file: File) : UpdateStatus
    data class Failed(val reason: String) : UpdateStatus
}

/**
 * Self-update. The app is not on Play, so it checks a small JSON manifest, and if
 * the published versionCode is higher it offers a Download button. Tapping it
 * downloads the APK and hands it to the system installer.
 *
 * Installing needs "Install unknown apps" allowed for this app; the system
 * prompts for that automatically the first time.
 */
class AppUpdater(private val context: Context) {

    private companion object {
        const val TAG = "AppUpdater"
    }

    private val appContext = context.applicationContext

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    /**
     * The newer release, once found. Kept separate from `status` so the banner can
     * say "available, downloading, ready to install, failed" without the
     * "available" fact being lost as the status moves on.
     */
    private val _release = MutableStateFlow<ReleaseInfo?>(null)
    val release: StateFlow<ReleaseInfo?> = _release.asStateFlow()

    /** Installed version, used to compare against the published one. */
    val installedCode: Int get() = com.bpsc.flashcards.BuildConfig.APP_VERSION_CODE
    val installedName: String get() = com.bpsc.flashcards.BuildConfig.APP_VERSION_NAME

    /** True when a newer release is waiting behind a Download button. */
    fun hasUpdate(): Boolean = _release.value != null

    fun currentRelease(): ReleaseInfo? = _release.value

    /** Read the manifest. Safe to call on every launch; it never blocks the UI. */
    suspend fun check() {
        if (_status.value is UpdateStatus.Checking) return
        _status.value = UpdateStatus.Checking
        val info = try {
            withContext(Dispatchers.IO) { downloadManifest() }
        } catch (e: Exception) {
            Log.w(TAG, "version check failed: ${e.message}")
            null
        }

        // A failed check must never discard a release we already know about: the banner
        // is mid-download, and losing `release` here would unmount it.
        val existing = _release.value
        val newer = info?.takeIf { it.versionCode > installedCode }
        _release.value = newer ?: existing
        val known = _release.value
        _status.value = if (known != null) UpdateStatus.Available(known) else UpdateStatus.UpToDate
    }

    private fun downloadManifest(): ReleaseInfo? {
        return try {
            val connection = URL(REMOTE_VERSION_URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.requestMethod = "GET"
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                ReleaseInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.optString("versionName"),
                    notes = json.optString("notes").ifBlank { null },
                    url = json.getString("url"),
                    sha256 = json.optString("sha256").ifBlank { null },
                )
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "version check failed: ${e.message}")
            null
        }
    }

    /** Download the APK to the app's cache. Call from a coroutine. */
    suspend fun download(release: ReleaseInfo): UpdateStatus {
        return withContext(Dispatchers.IO) {
            _status.value = UpdateStatus.Downloading(0f)
            try {
                val dir = File(appContext.cacheDir, "downloads").apply { mkdirs() }
                val file = File(dir, "bpsc-${release.versionCode}.apk")
                // version.json may give a path ("/app/x.apk") or a full URL.
                // Resolve a relative path against the manifest's host.
                val apkUrl = try {
                    URL(URL(REMOTE_VERSION_URL), release.url).toString()
                } catch (e: Exception) {
                    Log.w(TAG, "bad apk url: ${release.url}")
                    return@withContext UpdateStatus.Failed("bad download URL").also { _status.value = it }
                }
                val connection = URL(apkUrl).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 15000
                    connection.readTimeout = 20000
                    connection.requestMethod = "GET"
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@withContext UpdateStatus.Failed("download failed").also { _status.value = it }
                    }
                    // Copy by hand so the banner can show real progress: a 13 MB APK with
                    // only a spinner looks like nothing is happening.
                    val expected = try {
                        connection.contentLengthLong.takeIf { it > 0 } ?: connection.getHeaderField("Content-Length")?.toLongOrNull() ?: 0L
                    } catch (e: Exception) { 0L }
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read = 0L
                            var last = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                read += n
                                // Update at most ~10 times a second; the flow does not need more.
                                if (read - last > 1024 * 1024) {
                                    last = read
                                    if (expected > 0) {
                                        _status.value = UpdateStatus.Downloading((read.toFloat() / expected).coerceIn(0f, 0.99f))
                                    }
                                }
                            }
                            _status.value = UpdateStatus.Downloading(0.99f)
                        }
                    }
                } finally {
                    connection.disconnect()
                }

                // If the publisher recorded a checksum, refuse a file that does not match.
                if (!release.sha256.isNullOrBlank() && !sha256Matches(file, release.sha256)) {
                    file.delete()
                    return@withContext UpdateStatus.Failed("checksum mismatch").also { _status.value = it }
                }

                UpdateStatus.Downloaded(file).also { _status.value = it }
            } catch (e: Exception) {
                UpdateStatus.Failed(e.message ?: "download failed").also { _status.value = it }
            }
        }
    }

    /** Hand the downloaded APK to the system installer. */
    fun install(file: File) {
        // Android quietly drops the install intent when the app has not been allowed
        // to install unknown apps, so ask for that permission first instead of
        // leaving the user tapping a button that does nothing.
        if (!appContext.packageManager.canRequestPackageInstalls()) {
            openInstallPermissionSettings()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "could not launch installer: ${e.message}")
            openInstallPermissionSettings()
        }
    }

    private fun openInstallPermissionSettings() {
        // "Allow from this source" for this app, where the user flips the switch.
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${appContext.packageName}"))
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "could not open install-permission settings: ${e.message}")
        }
    }

    /** Forget a finished/failed download so the next check can offer it again. */
    fun reset() {
        _status.value = UpdateStatus.Idle
    }

    private fun sha256Matches(file: File, expected: String): Boolean {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expected.trim(), ignoreCase = true)
    }
}