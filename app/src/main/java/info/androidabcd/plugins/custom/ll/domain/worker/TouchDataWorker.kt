package info.androidabcd.plugins.custom.ll.domain.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import info.androidabcd.plugins.custom.ll.domain.datasource.repo.BiometricImp
import info.androidabcd.plugins.custom.ll.domain.datasource.repo.IBiometric
import java.io.File

class TouchDataWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val biometricRepo: IBiometric = BiometricImp()

    companion object {
        private const val USER_ID = "userId"
        private const val FILE_PATH = "filePath"
        fun startWorker(
            context: Context,
            userId: String,
            filePath: String
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

            WorkManager.getInstance(context).enqueue(
                sensorDataWorker.build()
            )
        }
    }

    override suspend fun doWork(): Result {
        val file = File(inputData.getString(FILE_PATH).toString())
        val id = inputData.getString(USER_ID).toString()

        val touchApi =  biometricRepo.addTouchData(id ,file ,"touch_data")

        return if (touchApi.status == 200) {
            Log.d("Worker" ,"Success sending touch data")
            Result.success()
        }
        else {
            Log.d("Worker" ,"Fail sending touch: ${touchApi.message}")
            Result.failure()
        }
    }

}