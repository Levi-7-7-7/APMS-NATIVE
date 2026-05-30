package com.activitypoints.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Secure token + session storage powered by Jetpack DataStore.
 * Replaces AsyncStorage.getItem / setItem / multiRemove from the RN app.
 *
 * Tokens are stored with DataStore (encrypted at rest via the OS keystore on
 * Android 6+). For extra security on older devices you can wrap with
 * EncryptedSharedPreferences – see the comments below.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_STUDENT_TOKEN = stringPreferencesKey("student_token")
        private val KEY_TUTOR_TOKEN   = stringPreferencesKey("tutor_token")
        private val KEY_ROLE          = stringPreferencesKey("role")
        private val KEY_STUDENT_NAME  = stringPreferencesKey("student_name")
        private val KEY_TUTOR_NAME    = stringPreferencesKey("tutor_name")
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    suspend fun getStudentToken(): String? =
        context.dataStore.data.firstOrNull()?.get(KEY_STUDENT_TOKEN)

    suspend fun getTutorToken(): String? =
        context.dataStore.data.firstOrNull()?.get(KEY_TUTOR_TOKEN)

    suspend fun getRole(): String? =
        context.dataStore.data.firstOrNull()?.get(KEY_ROLE)

    suspend fun getStudentName(): String? =
        context.dataStore.data.firstOrNull()?.get(KEY_STUDENT_NAME)

    suspend fun getTutorName(): String? =
        context.dataStore.data.firstOrNull()?.get(KEY_TUTOR_NAME)

    // ── Reactive flows (for AuthViewModel) ────────────────────────────────────

    val roleFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ROLE] }

    // ── Write ──────────────────────────────────────────────────────────────────

    suspend fun saveStudentSession(token: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STUDENT_TOKEN] = token
            prefs[KEY_STUDENT_NAME]  = name
            prefs[KEY_ROLE]          = "student"
        }
    }

    suspend fun saveTutorSession(token: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TUTOR_TOKEN] = token
            prefs[KEY_TUTOR_NAME]  = name
            prefs[KEY_ROLE]        = "tutor"
        }
    }

    // ── Clear ──────────────────────────────────────────────────────────────────

    suspend fun clearStudentSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_STUDENT_TOKEN)
            prefs.remove(KEY_STUDENT_NAME)
            prefs.remove(KEY_ROLE)
        }
    }

    suspend fun clearTutorSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TUTOR_TOKEN)
            prefs.remove(KEY_TUTOR_NAME)
            prefs.remove(KEY_ROLE)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
