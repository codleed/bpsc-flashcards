package com.bpsc.flashcards.data

import android.content.Context
import android.content.SharedPreferences
import com.bpsc.flashcards.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Anonymous, first-party install counting. Nothing personal is collected:
 *
 *  - [installId]: a random UUID v4 generated once on this device, stored locally.
 *    It is NOT derived from any hardware id, name, IMEI, contacts, ad id, etc.,
 *    and it cannot be reset/uninstalled-and-recreated to become a NEW id only
 *    by reinstalling (a reinstall generates a fresh UUID). Clearing app data
 *    also generates a fresh UUID — it is not reversible, not traceable back
 *    to a person, and is never shared with third parties.
 *  - No IP/geo is stored by this app (the server may see a request IP transiently
 *    like any HTTP request; nothing is retained here).
 *
 * We report, once per condition:
 *  - `event = "install"` on first ever report after opt-in.
 *  - `event = "update"` when the app's versionCode increases since last report.
 *  - `appVersionCode`, `appVersionName`, `osVersion`, `sdkInt`, `model` (device
 *    model, not identifier), `installId`, `event`, `previousVersionCode?`.
 *
 * Counting installs = `COUNT(DISTINCT installId) WHERE event='install'`.
 * `COUNT(DISTINCT installId)` overall = total installs + reinstalls.
 *
 * SENDING IS OPT-IN. Nothing is sent until [setConsent] true. Failures are
 * swallowed; tracking must never crash or slow the app. It runs after the UI
 * is up, on a background dispatcher, fire-and-forget, with short timeouts.
 * If [endpoint] is blank, tracking is fully disabled.
 */
object InstallTracker {

    private const val PREFS = "bpsc_analytics"
    private const val KEY_INSTALL_ID = "install_id"
    private const val KEY_CONSENT = "analytics_consent"
    private const val KEY_INSTALL_REPORTED = "install_reported"
    private const val KEY_LAST_REPORTED_CODE = "last_reported_version_code"

    /** Anonymous POST endpoint. Empty disables tracking entirely. */
    @Volatile
    var endpoint: String =
        if (BuildConfig.DEBUG) "http://192.168.100.83:8787/api/track"
        else "https://bpsc-qbank.netlify.app/api/track"

    fun isConsented(context: Context): Boolean = prefs(context).getBoolean(KEY_CONSENT, false)

    fun setConsent(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONSENT, enabled).apply()
    }

    /** The random per-install UUID (created on first call). Not a device/user id. */
    fun installId(context: Context): String {
        val p = prefs(context)
        val existing = p.getString(KEY_INSTALL_ID, null)
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        p.edit().putString(KEY_INSTALL_ID, generated).apply()
        return generated
    }

    /**
     * Call on app start. No-op unless [isConsented] and [endpoint] is non-blank.
     * Sends `install` the first time we can report, then `update` on versionCode
     * increases. Marks state only after a successful send so nothing is lost.
     */
    suspend fun onAppStart(context: Context) = withContext(Dispatchers.IO) {
        try {
            val app = context.applicationContext
            if (!isConsented(app)) return@withContext
            if (endpoint.isBlank()) return@withContext
            val p = prefs(app)
            val id = installId(app)
            val currentCode = BuildConfig.APP_VERSION_CODE
            val installReported = p.getBoolean(KEY_INSTALL_REPORTED, false)
            val lastReported = p.getInt(KEY_LAST_REPORTED_CODE, -1)

            val event: String
            val previousCode: Int?
            when {
                !installReported -> { event = "install"; previousCode = null }
                currentCode > lastReported -> { event = "update"; previousCode = lastReported.takeIf { it >= 0 } }
                else -> return@withContext
            }

            val sent = send(id, event, currentCode, previousCode)
            if (sent) {
                val edit = p.edit()
                if (event == "install") edit.putBoolean(KEY_INSTALL_REPORTED, true)
                edit.putInt(KEY_LAST_REPORTED_CODE, currentCode)
                edit.apply()
            }
        } catch (_: Exception) {
            // tracking must never break app start
        }
    }

    private fun send(installId: String, event: String, currentCode: Int, previousCode: Int?): Boolean {
        return try {
            val body = JSONObject()
                .put("installId", installId)
                .put("event", event)
                .put("appVersionCode", currentCode)
                .put("appVersionName", BuildConfig.APP_VERSION_NAME)
                .put("osVersion", android.os.Build.VERSION.RELEASE ?: "")
                .put("sdkInt", android.os.Build.VERSION.SDK_INT)
                .put("model", android.os.Build.MODEL ?: "")
                .put("previousVersionCode", previousCode)
                .toString()
                .toByteArray(Charsets.UTF_8)

            val connection = URL(endpoint).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Content-Length", body.size.toString())
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.outputStream.use { it.write(body) }
                connection.responseCode in 200..299
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
