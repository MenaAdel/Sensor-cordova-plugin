package info.androidabcd.plugins.custom.ll.domain.datasource.repo

import info.androidabcd.plugins.custom.ll.domain.datasource.network.ApiService
import info.androidabcd.plugins.custom.ll.domain.datasource.network.buildApi
import info.androidabcd.plugins.custom.ll.domain.entity.ApiResponse
import info.androidabcd.plugins.custom.ll.domain.entity.SensorBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class BiometricImp(private val apiService: ApiService = buildApi()) : IBiometric {

    override suspend fun addSensorData(body: SensorBody): ApiResponse {
        val file = File(body.file)
        return apiService.addSensorValues(userId = body.user_id.toRequestBody("text/plain".toMediaType()),
            file = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("application/json".toMediaType())
            ),
            type = "sensors_data".toRequestBody("text/plain".toMediaType())
        )
    }

    override suspend fun addTouchData(user_id: String, file: File ,type: String): ApiResponse {
        return apiService.addSensorValues(
            userId = user_id.toRequestBody("text/plain".toMediaType()),
            file = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("application/json".toMediaType())
            ),
            type = type.toRequestBody("text/plain".toMediaType())
        )
    }
}