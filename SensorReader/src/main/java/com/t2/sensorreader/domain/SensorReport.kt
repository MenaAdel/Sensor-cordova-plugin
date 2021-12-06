@file:JvmName("Sensor123")
@file:JvmMultifileClass

package com.t2.sensorreader.domain

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asFlow
import androidx.work.WorkManager
import com.google.gson.Gson
import com.t2.sensorreader.domain.datasource.storage.writeToFile
import com.t2.sensorreader.domain.datasource.storage.writeToFileOnDisk
import com.t2.sensorreader.domain.entity.*
import com.t2.sensorreader.domain.ext.getDeviceName
import com.t2.sensorreader.domain.ext.getScreenDiameter
import com.t2.sensorreader.domain.ext.pxToMm
import com.t2.sensorreader.domain.worker.InfoDataWorker
import com.t2.sensorreader.domain.worker.InfoDataWorker.Companion.OUTPUT_KEY_INFO
import com.t2.sensorreader.domain.worker.SensorDataWorker
import com.t2.sensorreader.domain.worker.TouchDataWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.N)
open class SensorReport(private val context: Context, private val activity: Activity ,private val deviceId: String) : SensorEventListener {

    private val TAG = this.javaClass.name
    private val sensorFlow: MutableStateFlow<SensorEvent?> = MutableStateFlow(null)
    private val gravity: FloatArray = FloatArray(3)
    private var accelerometer: Sensor? = null
    private var accelerometerUncalibrated: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    private var rotation: Sensor? = null
    private var sensorData = FileData()
    private val accelerometerArray = mutableListOf<Coordinates>()
    private val gyroscopeArray = mutableListOf<Coordinates>()
    private val magnetometerArray = mutableListOf<Coordinates>()
    private val deviceMotionArray = mutableListOf<DeviceMotion>()
    private var deviceMotionObject: DeviceMotion = DeviceMotion()
    private var index = 0
    private var startX: Float = 0f
    private var endX: Float = 0f
    private var startY: Float = 0f
    private var endY: Float = 0f
    private var moveX: Float = 0f
    private var moveY: Float = 0f
    private var startTime: Long = 0L
    private var endTime: Long = 0L
    private var tap: MutableList<Movement> = mutableListOf()
    private var swipe: MutableList<Movement> = mutableListOf()
    private var touchData: MutableList<Data> = mutableListOf()
    private var touchSwipeData: MutableList<Data> = mutableListOf()
    private var touchBody: TouchBody? = null
    private val dateFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS",
            Locale.getDefault())
    }
    private var isActionMove = false
    var listener: SensorListener? = null

    init {
        dispatchTouchEventListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupSensors()
        }
        touchBody = TouchBody(user_id = "test", swipe = swipe, tap = tap)
        CoroutineScope(Dispatchers.Default).launch {
            sensorFlow.collectLatest {
                it?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_LINEAR_ACCELERATION -> {
                            deviceMotionObject.acceleration = CoordinatesDevice(
                                x = it.values?.get(0),
                                y = it.values?.get(1),
                                z = it.values?.get(2)
                            )
                        }
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            deviceMotionObject.apply {
                                rotation = Rotation(
                                    alpha = it.values?.get(0),
                                    beta = it.values?.get(1),
                                    gamma = it.values?.get(2)
                                )
                                time = dateFormat.format(Date())
                                orientation = getDeviceOrientation()
                            }
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            val alpha = 0.8f

                            gravity[0] = alpha * gravity[0] + (1 - alpha) * it.values[0]
                            gravity[1] = alpha * gravity[1] + (1 - alpha) * it.values[1]
                            gravity[2] = alpha * gravity[2] + (1 - alpha) * it.values[2]

                            deviceMotionObject.apply {
                                accelerationIncludingGravity = CoordinatesDevice(
                                    x = it.values?.get(0),
                                    y = it.values?.get(1),
                                    z = it.values?.get(2)
                                )
                                acceleration = CoordinatesDevice(
                                    x = it.values[0] - gravity[0],
                                    y = it.values[1] - gravity[1],
                                    z = it.values[2] - gravity[2]
                                )
                            }
                        }
                    }

                    delay(6000)
                    fillSensorData()
                }
            }
        }
        addInfoModel()
    }

    fun setSensorListener(listener: SensorListener) {
        this.listener = listener
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSensors() {
        val sensorManager =
            context.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        // accelerometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, 10000)
        } ?: Log.d(TAG, "accelerometer not supported")

        //gyroscope
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gyroscope?.let {
            sensorManager.registerListener(this, it, 10000)
        } ?: Log.d(TAG, "gyroscope not supported")

        //magnetometer
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        magnetometer?.let {
            sensorManager.registerListener(this, it, 10000)
        } ?: Log.d(TAG, "magnetometer not supported")

        //accelerometerUncalibrated
        accelerometerUncalibrated =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)
        accelerometerUncalibrated?.let {
            sensorManager.registerListener(this, it, 10000)
        } ?: Log.d(TAG, "accelerometerUncalibrated not supported")

        //rotation
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotation?.let {
            sensorManager.registerListener(this, it, 10000)
        } ?: Log.d(TAG, "rotation not supported")
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onSensorChanged(event: SensorEvent?) {
        Log.d(TAG, "sensor type is: ${event?.sensor?.type}")

        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerArray.add(
                    Coordinates(
                        x = event.values?.get(0),
                        y = event.values?.get(1),
                        z = event.values?.get(2),
                        time = dateFormat.format(Date())
                    )
                )
                CoroutineScope(Dispatchers.Default).launch { sensorFlow.emit(event) }
                Log.d(
                    TAG,
                    "onSensorChanged x: ${event.values?.get(0)} y: ${event.values?.get(1)} z: ${
                        event.values?.get(2)
                    }"
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroscopeArray.add(
                    Coordinates(
                        x = event.values?.get(0),
                        y = event.values?.get(1),
                        z = event.values?.get(2),
                        time = dateFormat.format(Date())
                    )
                )
                Log.d(
                    TAG,
                    "onSensorChanged x: ${event.values?.get(0)} y: ${event.values?.get(1)} z: ${
                        event.values?.get(2)
                    }"
                )

            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetometerArray.add(
                    Coordinates(
                        x = event.values?.get(0),
                        y = event.values?.get(1),
                        z = event.values?.get(2),
                        time = dateFormat.format(Date())
                    )
                )
                Log.d(
                    TAG,
                    "onSensorChanged x: ${event.values?.get(0)} y: ${event.values?.get(1)} z: ${
                        event.values?.get(2)
                    }"
                )
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                CoroutineScope(Dispatchers.Default).launch { sensorFlow.emit(event) }
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                CoroutineScope(Dispatchers.Default).launch { sensorFlow.emit(event) }
            }
            else -> {
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun fillSensorData() {
        Log.d("fillS", "${deviceMotionObject.isNotEmpty()}")
        if (deviceMotionObject.isNotEmpty()) {
            deviceMotionArray.add(index, deviceMotionObject)
            deviceMotionObject = DeviceMotion()
            index++
        }

        sensorData.apply {
            accelerometer = accelerometerArray
            gyroscope = gyroscopeArray
            magnetometer = magnetometerArray
            deviceMotion = deviceMotionArray
        }
        val jsonData = Gson().toJson(sensorData)
        val jsonTouchData = Gson().toJson(touchBody)
        //context.writeToFileOnDisk(jsonData ,"Sensor_${systemSecondTime()}.json")
        addSensorData(jsonData)
        addTouchData(jsonTouchData)
        Log.d("SENSOOR: ", jsonData)
        sensorData = FileData()
    }

    private fun getDeviceOrientation(): Int {
        return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            1
        } else {
            0
        }
    }

    private fun calculateVelocity(
        startDistance: Float,
        endDistance: Float,
        duration: Float,
    ): Float {
        val distance = abs(endDistance - startDistance)
        return distance / duration
    }

    private fun dispatchTouchEventListener() {
        val window = activity.window
        val windowCallBack = object : Window.Callback {
            override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                TODO("Not yet implemented")
            }

            override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
                TODO("Not yet implemented")
            }

            override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                Log.d(TAG, "finger area is ${event?.size}")
                Log.d(TAG, "pressure is ${event?.pressure}")
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        startTime = System.currentTimeMillis()
                        Log.d(TAG, "startPoint is ${event.x} ,${event.y}")
                    }
                    MotionEvent.ACTION_MOVE -> {
                        isActionMove = true
                        moveX = event.x
                        moveY = event.y
                    }
                    MotionEvent.ACTION_UP -> {
                        onEventUp(event)
                    }
                }
                return true
            }

            override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
                TODO("Not yet implemented")
            }

            override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
                TODO("Not yet implemented")
            }

            override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
                TODO("Not yet implemented")
            }

            override fun onCreatePanelView(featureId: Int): View? {
                return null
            }

            override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
                return true
            }

            override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean {
                return true
            }

            override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
                TODO("Not yet implemented")
            }

            override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
                TODO("Not yet implemented")
            }

            override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
            }

            override fun onContentChanged() {
            }

            override fun onWindowFocusChanged(hasFocus: Boolean) {
            }

            override fun onAttachedToWindow() {
            }

            override fun onDetachedFromWindow() {
            }

            override fun onPanelClosed(featureId: Int, menu: Menu) {
                TODO("Not yet implemented")
            }

            override fun onSearchRequested(): Boolean {
                TODO("Not yet implemented")
            }

            override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
                TODO("Not yet implemented")
            }

            override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
                TODO("Not yet implemented")
            }

            override fun onWindowStartingActionMode(
                callback: ActionMode.Callback?,
                type: Int,
            ): ActionMode? {
                TODO("Not yet implemented")
            }

            override fun onActionModeStarted(mode: ActionMode?) {
                TODO("Not yet implemented")
            }

            override fun onActionModeFinished(mode: ActionMode?) {
                TODO("Not yet implemented")
            }

        }
        window.callback = windowCallBack
    }

    private fun onEventUp(event: MotionEvent) {
        endX = event.x
        endY = event.y
        endTime = System.currentTimeMillis()
        Log.d(TAG, "endPoint is ${event.x} ,${event.y}")
        Log.d(TAG, "distance x is ${endX - startX}")
        Log.d(TAG, "distance y is ${endY - startY}")
        val xVelocity = calculateVelocity(startX, endX, (endTime - startTime).toFloat())
        val yVelocity = calculateVelocity(startY, endY, (endTime - startTime).toFloat())
        val data = Data(dx = endX - startX,
            dy = endY - startY,
            moveX = moveX,
            moveY = moveY,
            vx = xVelocity,
            vy = yVelocity,
            x0 = startX,
            y0 = startY,
            fingerArea = event.size,
            pressure = event.pressure,
            time = endTime - startTime)

        if (isActionMove) {
            touchSwipeData.add(data)
            fillTouchSwipe()
        } else {
            touchData.add(data)
            fillTouchData()
        }
        isActionMove = false
        resetData()
    }

    private fun fillTouchData() {
        val movement = Movement(time_start = dateFormat.format(startTime),
            time_stop = dateFormat.format(endTime),
            data = touchData,
            phone_orientation = getDeviceOrientation())
        tap.add(movement)
        val jsonData = Gson().toJson(touchBody)
        Log.d("jsonto ", jsonData)
    }

    private fun fillTouchSwipe() {
        val movement = Movement(time_start = dateFormat.format(startTime),
            time_stop = dateFormat.format(endTime),
            data = touchSwipeData,
            phone_orientation = getDeviceOrientation())
        swipe.add(movement)
        val jsonData = Gson().toJson(touchBody)
        Log.d("json touch body is: ", jsonData)
    }

    private fun resetData() {
        startX = 0f
        endX = 0f
        startY = 0f
        endY = 0f
        moveX = 0f
        moveY = 0f
        startTime = 0L
        endTime = 0L
    }

    private fun addInfoModel() {
        val carrierName =
            (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.networkOperatorName
                ?: "unknown"
        val deviceDetails = DeviceDetails(
            deviceId = deviceId,
            carrier = carrierName,
            userId = "MENA",
            phoneOS = "android API ${Build.VERSION.SDK_INT}",
            deviceType = getDeviceName(),
            screenSpecs = ScreenSpecs(
                safeAreaPaddingBottom = 0,
                safeAreaPaddingTop = 0,
                height = context.pxToMm(context.resources.displayMetrics.heightPixels).toInt(),
                width = context.pxToMm(context.resources.displayMetrics.widthPixels).toInt(),
                diameter = context.getScreenDiameter()
            )
        )

        addInfoData(context, Gson().toJson(deviceDetails))
    }

    private fun addSensorData(jsonData: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val filePath = context.writeToFile(jsonData, "sensor.json")
            val sensorBody = SensorBody(file = filePath)
            SensorDataWorker.startWorker(
                context,
                sensorBody
            )
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("SensorDataWorker").asFlow()
                .collectLatest {
                    if (it.size != 0) {
                        Log.d("reportRR",
                            it[0].outputData.getString(SensorDataWorker.OUTPUT_KEY_SENSOR)
                                .toString())
                        it[0].outputData.getString(SensorDataWorker.OUTPUT_KEY_SENSOR)
                        listener?.onApiValueChanged(it[0].outputData.getString(SensorDataWorker.OUTPUT_KEY_SENSOR).toString())
                    }
                }
        }
    }

    private fun addTouchData(jsonData: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val filePath = context.writeToFile(jsonData, "touch.json")
            TouchDataWorker.startWorker(
                context,
                "testId",
                filePath.toString()
            )
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("TouchDataWorker").asFlow()
                .collectLatest {
                    if (it.size != 0) {
                        Log.d("report",
                            it[0].outputData.getString(TouchDataWorker.OUTPUT_KEY_TOUCH).toString())
                        it[0].outputData.getString(TouchDataWorker.OUTPUT_KEY_TOUCH)
                        listener?.onApiValueChanged(it[0].outputData.getString(TouchDataWorker.OUTPUT_KEY_TOUCH).toString())
                    }
                }
        }
    }

    private fun addInfoData(context: Context, jsonData: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val filePath = context.writeToFile(jsonData, "info.json")
            context.writeToFileOnDisk(jsonData, "info.json")
            InfoDataWorker.startWorker(
                context,
                "testId",
                filePath.toString()
            )

            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("InfoDataWorker").asFlow()
                .collectLatest {
                    if (it.size != 0) {
                        Log.d("reportRR",
                            it[0].outputData.getString(OUTPUT_KEY_INFO).toString())
                        it[0].outputData.getString(OUTPUT_KEY_INFO)
                        listener?.onApiValueChanged(it[0].outputData.getString(OUTPUT_KEY_INFO).toString())
                    }
                }
        }
    }
}