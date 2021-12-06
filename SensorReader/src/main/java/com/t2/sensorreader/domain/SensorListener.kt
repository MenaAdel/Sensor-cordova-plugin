package com.t2.sensorreader.domain

interface SensorListener {
    fun onApiValueChanged(response: String)
}