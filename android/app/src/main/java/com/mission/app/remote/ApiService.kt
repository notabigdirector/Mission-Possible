package com.mission.app.remote

import com.mission.app.data.model.HealthResponse
import com.mission.app.data.model.RegisterRequest
import com.mission.app.data.model.RegisterResponse
import com.mission.app.data.model.SyncRequest
import com.mission.app.data.model.SyncResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/v1/health")
    suspend fun health(): HealthResponse

    @POST("/api/v1/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("/api/v1/sync")
    suspend fun sync(@Body body: SyncRequest): SyncResponse
}