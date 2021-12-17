package com.t2.sensorreader.domain.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.t2.sensorreader.domain.datasource.repo.BiometricImp
import com.t2.sensorreader.domain.datasource.repo.IBiometric
import com.t2.sensorreader.domain.worker.InfoDataWorker.Companion.OUTPUT_KEY_INFO
import java.io.File

class InfoDataWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val biometricRepo: IBiometric = BiometricImp()

    companion object {
        const val USER_ID = "userId"
        const val FILE_PATH = "filePath"
        const val OUTPUT_KEY_INFO = "outputKeyInfo"
        fun startWorker(
            context: Context,
            userId: String,
            filePath: String,
        ) {
            val sensorDataWorker =
                OneTimeWorkRequestBuilder<InfoDataWorker>()
            sensorDataWorker.setConstraints(
                Constraints.Builder().setRequiredNetworkType(
                    NetworkType.CONNECTED
                ).build()
            )

            val inputData = Data.Builder()
            inputData.apply {
                putString(USER_ID, userId)
                putString(FILE_PATH, filePath)
            }
            sensorDataWorker.setInputData(inputData.build())

            WorkManager.getInstance(context)
                .enqueueUniqueWork("InfoDataWorker", ExistingWorkPolicy.KEEP,
                    sensorDataWorker.build()
                )
        }
    }

    override suspend fun doWork(): Result {
        Log.d("Mena" ,"add info data 2")

        val file = File(inputData.getString(FILE_PATH).toString())
        val id = inputData.getString(USER_ID).toString()

        val touchApi = biometricRepo.addTouchData(id, file, "info")

        return if (touchApi.status == 200) {
            Log.d("Worker", "Success sending info data")
            val outputData = createOutputData("Success sending info data")
            Result.success(outputData)
        } else {
            Log.d("Worker", "Fail sending info: ${touchApi.message}")
            val outputData = createOutputData("Fail sending info: ${touchApi.message}")
            Result.failure(outputData)
        }
    }

}

fun createOutputData(outputData: String ,key: String = OUTPUT_KEY_INFO): Data {
    return Data.Builder()
        .putString(key, outputData)
        .build()
}