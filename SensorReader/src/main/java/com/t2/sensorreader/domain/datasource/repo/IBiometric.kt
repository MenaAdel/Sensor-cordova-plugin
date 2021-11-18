package com.t2.sensorreader.domain.datasource.repo

import com.t2.sensorreader.domain.entity.ApiResponse
import com.t2.sensorreader.domain.entity.SensorBody
import java.io.File

interface IBiometric {
    suspend fun addSensorData(body: SensorBody): ApiResponse
    suspend fun addTouchData(user_id: String ,file: File ,type: String): ApiResponse
}