package edu.rpi.shuttletracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.*
import org.json.JSONArray
import java.net.URL
import kotlinx.coroutines.*

/**
 * Implementation of App Widget functionality.
 */

const val SYNC = "SYNC"

class WidgetActivity : AppWidgetProvider() {

    private var alertPrompted : Boolean = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null

    // Periodically updates widget every 30 min (minimum set by Android)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // Updates widget based on user input
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        if(SYNC == intent?.action) {
            val appWidgetId = intent.getIntExtra("appWidgetId",0)
            updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)

        }

    }

    // Get coordinates of stops to add to static maps api url
    private fun drawStops(url: URL): String? {
        var stops: String? = null
        var flag: Boolean = false
        val thread = Thread(Runnable {
            kotlin.run {
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                var stopsCo = ""
                for (i in 0 until jsonArray.length()) {
                    val stop = jsonArray.getJSONObject(i)
                    val coordinate = stop.getJSONObject("coordinate")
                    val latitude = coordinate.getDouble("latitude")
                    val longitude = coordinate.getDouble("longitude")
                    val name = stop.getString("name")
                    stopsCo += "|$latitude,$longitude"

                }
                stops = stopsCo
                flag = true
            }
        })
        thread.start()
        while (!flag);

        return stops
    }
    // Get coordinates of route to add to static maps api url
    private fun drawRoute(url: URL): String? {
        var route: String? = null
        var flag: Boolean = false
        val thread = Thread(Runnable {
            kotlin.run {
                val jsonString = url.readText()
                var jsonArray = JSONArray(jsonString)
                var routeObject = jsonArray.getJSONObject(0)
                var coordArray = routeObject.getJSONArray("coordinates")
                var routeStr = ""
                for (i in 0 until coordArray.length()) {
                    val waypoint = coordArray.getJSONObject(i)
                    val latitude = waypoint.getDouble("latitude")
                    val longitude = waypoint.getDouble("longitude")
                    routeStr += "|$latitude,$longitude"
                }
                route = routeStr
                flag = true
            }
        })
        thread.start()
        while (!flag);

        return route
    }
    // Updates views displayed in home screen widget
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object

        val intent = Intent(context, WidgetActivity::class.java)
        intent.action = SYNC
        intent.putExtra("appWidgetId", appWidgetId)
        // fix: add flags
        val pintent = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // make service to get location
        // TODO: Make service to get user location. AppWidgetProvider class is not able to fetch user location

        val views = RemoteViews(context.packageName, R.layout.bus_location_widget)
        var stops: String? = drawStops(URL(context.resources.getString(R.string.stops_url)))
        var route: String? = drawRoute(URL(context.resources.getString(R.string.routes_url)))

        // URL used to fetch map of the region
        var apiKey: String? // get api key
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .apply {
                apiKey = metaData.getString("com.google.android.geo.API_KEY")
            }
        val mapUrl = ("https://maps.googleapis.com/maps/api/staticmap" +
                "?center=42.730426, -73.676573" +
                "&zoom=14" +
                "&path=color:0xff3838|weight:3" + route +
                "&size=400x400" +
                "&markers=icon:https://i.ibb.co/0sbs6Xd/simple-Circle18x18.png" + stops +
                "&style=feature:poi.business|element:labels|visibility:off" +
                "&style=feature:poi.medical|element:labels|visibility:off" +
                "&style=feature:poi.government|element:labels|visibility:off" +
                "&style=feature:poi.school|element:labels|visibility:off" +
                "&key=" + apiKey)

        // load map image from URL to ImageView
        val awt: AppWidgetTarget = object : AppWidgetTarget(context.applicationContext, R.id.mapImg, views, appWidgetId) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                super.onResourceReady(resource, transition)
            }
        }
        Glide.with(context.applicationContext).asBitmap().load(mapUrl).into(awt)

        views.setOnClickPendingIntent(R.id.sync, pintent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}