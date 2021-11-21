package com.t2.sensorreader.domain.datasource.network

import com.t2.sensorreader.domain.entity.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("store_data")
    suspend fun addSensorValues(
        @Part("data_type") type: RequestBody,
        @Part("user_id") userId: RequestBody,
        @Part("account") account: RequestBody,
        @Part file: MultipartBody.Part
    ): ApiResponse
}