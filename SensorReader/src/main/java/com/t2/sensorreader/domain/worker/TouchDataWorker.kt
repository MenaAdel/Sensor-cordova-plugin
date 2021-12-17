package com.t2.sensorreader.domain.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.t2.sensorreader.domain.datasource.repo.BiometricImp
import com.t2.sensorreader.domain.datasource.repo.IBiometric
import java.io.File

class TouchDataWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val biometricRepo: IBiometric = BiometricImp()

    companion object {
        private const val USER_ID = "userId"
        private const val FILE_PATH = "filePath"
        const val OUTPUT_KEY_TOUCH = "outputKeyTouch"
        fun startWorker(
            context: Context,
            userId: String,
            filePath: String,
        ) {
            val sensorDataWorker =
                OneTimeWorkRequestBuilder<TouchDataWorker>()
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
                .enqueueUniqueWork("TouchDataWorker", ExistingWorkPolicy.KEEP,
                    sensorDataWorker.build()
                )
        }
    }

    override suspend fun doWork(): Result {
        Log.d("Mena" ,"addTouchData 2")
        val file = File(inputData.getString(FILE_PATH).toString())
        val id = inputData.getString(USER_ID).toString()

        val touchApi = biometricRepo.addTouchData(id, file, "touch_data")

        return if (touchApi.status == 200) {
            Log.d("Worker", "Success sending touch data")
            val outputData = createOutputData("Success sending touch data" , OUTPUT_KEY_TOUCH)
            Result.success(outputData)
        } else {
            Log.d("Worker", "Fail sending touch: ${touchApi.message}")
            val outputData = createOutputData("Fail sending touch: ${touchApi.message}" ,
                OUTPUT_KEY_TOUCH)
            Result.failure(outputData)
        }
    }

}