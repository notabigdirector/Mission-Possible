package com.mission.app.sync

import android.content.Context
import com.mission.app.R
import com.mission.app.data.repository.SyncRepository
import com.mission.app.data.model.RegisterRequest
import com.mission.app.data.model.RegisterResponse
import com.mission.app.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * 同步状态机（对齐 src/main/sync.ts）：
 * - 周期 60s / 后台 15min（WorkManager）+ 本地变更 3s debounce
 * - 全量上传 tasks+projects+deleted → 服务端合并 → applyRemote 应用回本地
 * - 状态：idle / syncing / ok / error / offline
 */
class SyncManager(
    private val prefs: SyncPrefs,
    private val syncRepository: SyncRepository,
    private val appContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    @Volatile
    private var syncing = false

    private var debounceJob: Job? = null

    fun start() {
        SyncWorker.schedulePeriodic(appContext)
        scope.launch {
            prefs.config.collect { cfg ->
                if (cfg.isConfigured) {
                    syncInternal()
                }
            }
        }
    }

    suspend fun currentConfig(): SyncConfig = prefs.currentConfig()

    suspend fun setConfig(cfg: SyncConfig) {
        prefs.setConfig(cfg)
        markDirty()
    }

    suspend fun importCert(bytes: ByteArray) {
        val file = File(appContext.filesDir, "sync-cert.pem")
        file.writeBytes(bytes)
        prefs.setConfig(prefs.currentConfig().copy(certImported = true))
    }

    fun markDirty() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(3_000)
            syncInternal()
        }
        SyncWorker.requestNow(appContext)
    }

    suspend fun sync(): SyncStatus {
        syncInternal()
        return _status.value
    }

    suspend fun registerUser(name: String): RegisterResponse {
        val cfg = prefs.currentConfig()
        if (cfg.serverUrl.isBlank()) throw IllegalStateException("请先填写服务器地址")
        val api = ApiClient.create(cfg.serverUrl, null, certBytes(cfg))
        return api.register(RegisterRequest(name.trim()))
    }

    private suspend fun syncInternal() {
        if (syncing) return
        val cfg = prefs.currentConfig()
        if (!cfg.isConfigured) {
            _status.value = SyncStatus(state = "idle")
            return
        }
        val url = cfg.serverUrl.trim()
        if (url.isEmpty()) {
            _status.value = SyncStatus(state = "error", message = "服务器地址无效")
            return
        }
        syncing = true
        _status.value = SyncStatus(state = "syncing", lastSyncAt = _status.value.lastSyncAt)
        try {
            val api = ApiClient.create(url, cfg.token, certBytes(cfg))
            val response = api.sync(syncRepository.buildRequest())
            syncRepository.applyResponse(response)
            _status.value = SyncStatus(state = "ok", lastSyncAt = System.currentTimeMillis())
        } catch (e: Exception) {
            val message = e.message ?: e.javaClass.simpleName
            _status.value = SyncStatus(
                state = "error",
                lastSyncAt = _status.value.lastSyncAt,
                message = message
            )
        } finally {
            syncing = false
        }
    }

    private suspend fun certBytes(cfg: SyncConfig): ByteArray? {
        return if (cfg.certImported) {
            val file = File(appContext.filesDir, "sync-cert.pem")
            if (file.exists()) file.readBytes() else bundledCert()
        } else {
            bundledCert()
        }
    }

    private fun bundledCert(): ByteArray? = try {
        appContext.resources.openRawResource(R.raw.sync_cert).use { it.readBytes() }
    } catch (e: Exception) {
        null
    }
}