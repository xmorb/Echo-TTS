package com.echotts.app.api

import com.echotts.app.api.models.DeviceListResponse
import com.echotts.app.api.models.TtsRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AlexaApiService {

    @GET("api/devices-v2/device")
    suspend fun getDevices(
        @Query("cached") cached: Boolean = false
    ): Response<DeviceListResponse>

    @POST("api/behaviors/preview")
    suspend fun sendTts(
        @Body request: TtsRequest
    ): Response<ResponseBody>
}
