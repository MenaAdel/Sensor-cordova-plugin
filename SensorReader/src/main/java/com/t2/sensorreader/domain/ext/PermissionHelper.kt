package com.t2.sensorreader.domain.ext

import android.Manifest
import android.R
import android.content.Context
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener

fun Context.requestPermission():String{
    var id = ""
    Dexter.withContext(this)
        .withPermission(Manifest.permission.READ_PHONE_STATE)
        .withListener(object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                id = getDeviceIMEI()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                showPermissionDialog()
            }

            override fun onPermissionRationaleShouldBeShown(
                permission: PermissionRequest,
                token: PermissionToken,
            ) { /* ... */
            }
        }).check()
    return id
}

fun Context.showPermissionDialog() {
    val dialogPermissionListener: PermissionListener = DialogOnDeniedPermissionListener.Builder
        .withContext(this)
        .withTitle("Phone permission")
        .withMessage("Phone permission is needed to get device id")
        .withButtonText(R.string.ok)
        .build()
}