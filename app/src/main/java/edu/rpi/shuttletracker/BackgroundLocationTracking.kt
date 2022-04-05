package edu.rpi.shuttletracker

import android.R
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*


class BackgroundLocationTracking : Service(){

    private var currentLocation: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
    //Null service
    //TODO: https://stackoverflow.com/questions/34573109/how-to-make-an-android-app-to-always-run-in-background

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)//This "this" means the location is only being updated when MapActivity is active
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5 * 1000 // refreshes every 5 seconds
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation
            }
        }

        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }
    var notificationIntent = Intent(this, MapsActivity::class.java)

    open fun startForeground() {
    var builder = NotificationCompat.Builder(this, "1")
        .setSmallIcon(R.drawable.arrow_up_float)
        .setContentTitle("My Service Notification")
        .setContentText("Much longer text that cannot fit one line...")
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("Much longer text that cannot fit one line..."))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(true)

}
}