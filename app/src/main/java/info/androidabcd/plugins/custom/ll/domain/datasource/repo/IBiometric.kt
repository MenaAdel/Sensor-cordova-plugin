package info.androidabcd.plugins.custom.ll.domain.datasource.repo

import info.androidabcd.plugins.custom.ll.domain.entity.ApiResponse
import info.androidabcd.plugins.custom.ll.domain.entity.SensorBody
import java.io.File

interface IBiometric {
    suspend fun addSensorData(body: SensorBody): ApiResponse
    suspend fun addTouchData(user_id: String ,file: File ,type: String): ApiResponse
}