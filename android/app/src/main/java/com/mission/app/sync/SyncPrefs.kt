package com.mission.app.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class SyncConfig(
    val serverUrl: String = "",
    val token: String = "",
    val userName: String = "",
    val certImported: Boolean = false
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && token.isNotBlank()
}

data class SyncStatus(
    val state: String = "idle", // idle | syncing | ok | error | offline
    val lastSyncAt: Long? = null,
    val message: String = ""
) {
    val isError: Boolean get() = state == "error"
}

val SyncStatus.isOffline: Boolean get() = state == "offline"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_config")

class SyncPrefs(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val TOKEN = stringPreferencesKey("token")
        val USER_NAME = stringPreferencesKey("user_name")
        val CERT_IMPORTED = booleanPreferencesKey("cert_imported")
    }

    val config: Flow<SyncConfig> = context.dataStore.data.map { prefs ->
        SyncConfig(
            serverUrl = prefs[Keys.SERVER_URL] ?: "",
            token = prefs[Keys.TOKEN] ?: "",
            userName = prefs[Keys.USER_NAME] ?: "",
            certImported = prefs[Keys.CERT_IMPORTED] ?: false
        )
    }

    suspend fun currentConfig(): SyncConfig = config.first()

    suspend fun setConfig(cfg: SyncConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = cfg.serverUrl
            prefs[Keys.TOKEN] = cfg.token
            prefs[Keys.USER_NAME] = cfg.userName
            prefs[Keys.CERT_IMPORTED] = cfg.certImported
        }
    }
}