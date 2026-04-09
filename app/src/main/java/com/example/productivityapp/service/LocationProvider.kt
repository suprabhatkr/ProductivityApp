package com.example.productivityapp.service

import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest

interface LocationProvider {
    fun requestLocationUpdates(request: LocationRequest, callback: LocationCallback)
    fun removeLocationUpdates(callback: LocationCallback)
}

class FusedLocationProviderWrapper(private val context: Context) : LocationProvider {
    private val client by lazy { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    override fun requestLocationUpdates(request: LocationRequest, callback: LocationCallback) {
        client.requestLocationUpdates(request, callback, context.mainLooper)
    }

    override fun removeLocationUpdates(callback: LocationCallback) {
        client.removeLocationUpdates(callback)
    }
}

