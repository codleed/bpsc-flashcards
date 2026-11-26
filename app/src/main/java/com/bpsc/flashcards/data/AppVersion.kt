package com.bpsc.flashcards.data

/**
 * Single source of truth for the app version.
 *
 * Gradle reads these two numbers via `buildConfigField`, and the publish script
 * reads them to stamp `version.json`. So bumping a release is one edit here:
 *
 *   versionCode  must increase by 1 every release (Android uses it to compare).
 *   versionName  is what the user sees, e.g. "1.1".
 */
internal object AppVersion {
    const val CODE: Int = 6
    const val NAME: String = "1.5"
}