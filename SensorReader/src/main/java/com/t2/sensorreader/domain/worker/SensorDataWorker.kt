package com.t2.sensorreader.domain.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.t2.sensorreader.domain.datasource.repo.BiometricImp
import com.t2.sensorreader.domain.datasource.repo.IBiometric
import com.t2.sensorreader.domain.entity.SensorBody

class SensorDataWorker(val appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val biometricRepo: IBiometric = BiometricImp()

    companion object {
        private const val SENSOR_BODY = "sensorBody"
        const val OUTPUT_KEY_SENSOR = "outputKeySensor"
        fun startWorker(
            context: Context,
            sensorBody: SensorBody,
        ) {
            val sensorDataWorker =
                OneTimeWorkRequestBuilder<SensorDataWorker>()
            sensorDataWorker.setConstraints(
                Constraints.Builder().setRequiredNetworkType(
                    NetworkType.CONNECTED
                ).build()
            )

            val inputData = Data.Builder()
            inputData.apply {
                putString(SENSOR_BODY, Gson().toJson(sensorBody))
            }
            sensorDataWorker.setInputData(inputData.build())

            WorkManager.getInstance(context)
                .enqueueUniqueWork("SensorDataWorker", ExistingWorkPolicy.KEEP,
                    sensorDataWorker.build()
                )
        }
    }

    override suspend fun doWork(): Result {
        val sensorBody: SensorBody? = Gson().fromJson(inputData.getString(SENSOR_BODY),
            SensorBody::class.java)

        val sensorApi = sensorBody?.let { biometricRepo.addSensorData(it) }

        return if (sensorApi?.status == 200) {
            Log.d("Worker", "Success sending sensor data")
            val outputData = createOutputData("Success sending sensor data", OUTPUT_KEY_SENSOR)
            Result.success(outputData)
        } else {
            Log.d("Worker", "Fail sending sensor: ${sensorApi?.message}")
            val outputData =
                createOutputData("Fail sending sensor: ${sensorApi?.message}", OUTPUT_KEY_SENSOR)
            Result.failure(outputData)
        }
    }

}