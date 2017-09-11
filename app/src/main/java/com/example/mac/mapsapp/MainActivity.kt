package com.example.mac.mapsapp

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.app.Fragment
import android.app.ProgressDialog
import android.support.constraint.ConstraintLayout
import android.util.Log
import android.view.View

import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import android.widget.Button
import com.google.android.gms.maps.model.*
import okhttp3.*

import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.HashMap


class MainActivity : FragmentActivity(), OnMapReadyCallback {

    private val MY_PERMISSIONS_REQUEST = 123
    private val MIN_TIME: Long = 400
    private val MIN_DISTANCE: Float = 1000F

    private var mapFragment: MapFragment = MapFragment()
    private var autocompleteFragment: PlaceAutocompleteFragment = PlaceAutocompleteFragment()
    private var secondAutocompleteFragment: PlaceAutocompleteFragment = PlaceAutocompleteFragment()
    private var localButton: Button? = null
    private var servicesList: ServicesList? = null
    private var progressBar: ConstraintLayout? = null

    private var googleMap: GoogleMap? = null
    private var locationManager: LocationManager? = null
    private var currentLocation: LatLng? = null
    private var originLocation: LatLng? = null
    private var destinationLocation: LatLng? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar4)

        servicesList = Fragment.instantiate(this@MainActivity,
                ServicesList::class.java!!.getName()) as ServicesList

        fragmentManager.beginTransaction()
                .replace(R.id.frametest, servicesList)
                .hide(servicesList)
                .commit()

        ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                MY_PERMISSIONS_REQUEST)

        autocompleteFragment = fragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as PlaceAutocompleteFragment
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                if (destinationLocation == null) {
                    googleMap?.clear()
                }

                originLocation = place.latLng
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(place.latLng, 15F))
                googleMap?.addMarker(MarkerOptions().position(place.latLng).title("Origem"))
                getDirections()
            }

            override fun onError(status: Status) {
                // Handler code here.
                Log.i("Autocomplete place", "Ocorreu um erro: " + status);
            }
        })

        secondAutocompleteFragment = fragmentManager.findFragmentById(R.id.second_place_autocomplete_fragment) as PlaceAutocompleteFragment
        secondAutocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                if (originLocation == null) {
                    googleMap?.clear()
                }

                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(place.latLng, 15F))
                googleMap?.addMarker(MarkerOptions().position(place.latLng).title("Destino"))
                destinationLocation = place.latLng
                getDirections()
            }

            override fun onError(status: Status) {
                // Handler code here.
                Log.i("Autocomplete place", "Ocorreu um erro: " + status);
            }
        })

        mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)

        // Create persistent LocationManager reference
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?;
        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener)

        localButton = findViewById(R.id.btn_local)
        localButton?.setOnClickListener {
            autocompleteFragment.setText("Local atual")
            if (destinationLocation == null) {
                googleMap?.clear()
            }

            originLocation = currentLocation
            if (originLocation == null) {
                return@setOnClickListener
            }
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(originLocation, 15F))
            googleMap?.addMarker(MarkerOptions().position(originLocation as LatLng).title("Origem"))
            getDirections()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        val location = LatLng(0.0, 0.0)
        map.moveCamera(CameraUpdateFactory.newLatLng(location))
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        map.isMyLocationEnabled = true

        this.googleMap = map
        googleMap?.setOnMapClickListener { point ->
            googleMap?.clear()
            originLocation = null
            autocompleteFragment.setText("")

            destinationLocation = null
            secondAutocompleteFragment.setText("")
        }
    }

    private fun getDirections(){
        val origem = originLocation
        val destino = destinationLocation
        if (origem == null || destino == null) {
            return
        }

        val url = getUrl(origem, destino)
        Log.d("onMapClick", url.toString())
        val FetchUrl = FetchUrl()

        FetchUrl.execute(url)

        googleMap?.addMarker(MarkerOptions().position(destino).title("Destino"))
        var bounds = LatLngBounds.builder()
        bounds.include(origem)
        bounds.include(destino)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), mapFragment.view.width, mapFragment.view.height - 150, 32))


        fragmentManager.beginTransaction().remove(servicesList).commit()
        progressBar?.visibility = View.VISIBLE
        val urlUber = "https://api.uber.com/v1.2/estimates/price?start_latitude="+origem.latitude.toString()+"&start_longitude="+origem.longitude.toString()+"&end_latitude="+destino.latitude.toString()+"&end_longitude="+destino.longitude.toString()
        run(urlUber, {
            call, response ->
            runOnUiThread {
                progressBar?.visibility = View.GONE

                val bundle = Bundle()
                bundle.putString("jsonServicos", response.body()?.string())
                servicesList?.arguments = bundle
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .replace(R.id.frametest, servicesList)
                        .show(servicesList)
                        .commit()
            }
        })
    }

    private fun getUrl(origin: LatLng, dest: LatLng): String {

        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude

        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude


        // Sensor enabled
        val sensor = "mode=driving"

        val api_key = "key=" + Constants.googleApiKey()

        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$sensor&$api_key"

        // Output format
        val output = "json"

        // Building the url to the web service
        val url = "https://maps.googleapis.com/maps/api/directions/$output?$parameters"


        return url
    }


    // Fetches data from url passed
    private inner class FetchUrl : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg url: String): String {

            // For storing data from web service
            var data = ""

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0])
                Log.d("Background Task data", data.toString())
            } catch (e: Exception) {
                Log.d("Background Task", e.toString())
            }

            return data
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            val parserTask = ParserTask()

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result)

        }
    }

    private inner class ParserTask : AsyncTask<String, Int, List<List<HashMap<String, String>>>>() {

        // Parsing the data in non-ui thread
        override fun doInBackground(vararg jsonData: String): List<List<HashMap<String, String>>> {

            val jObject: JSONObject

            try {
                jObject = JSONObject(jsonData[0])
                Log.d("ParserTask", jsonData[0])
                val parser = DataParser()
                Log.d("ParserTask", parser.toString())

                // Starts parsing data
                var routes: List<List<HashMap<String, String>>>  = parser.parse(jObject)
                Log.d("ParserTask", "Executing routes")
                Log.d("ParserTask", routes.toString())
                return routes

            } catch (e: Exception) {
                Log.d("ParserTask", e.toString())
                e.printStackTrace()
            }

            val r:List<List<HashMap<String, String>>> = ArrayList<ArrayList<HashMap<String, String>>>()
            return r
        }

        // Executes in UI thread, after the parsing process
        override fun onPostExecute(result: List<List<HashMap<String, String>>>) {
            var points: ArrayList<LatLng>
            var lineOptions: PolylineOptions? = null

            // Traversing through all the routes
            for (i in result.indices) {
                points = ArrayList<LatLng>()
                lineOptions = PolylineOptions()

                // Fetching i-th route
                val path = result[i]

                // Fetching all the points in i-th route
                for (j in path.indices) {
                    val point = path[j]

                    val lat = java.lang.Double.parseDouble(point["lat"])
                    val lng = java.lang.Double.parseDouble(point["lng"])
                    val position = LatLng(lat, lng)

                    points.add(position)
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points)
                lineOptions.width(5f)
                lineOptions.color(Color.BLUE)

                Log.d("onPostExecute", "onPostExecute lineoptions decoded")

            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                googleMap?.addPolyline(lineOptions)
            } else {
                Log.d("onPostExecute", "without Polylines drawn")
            }
        }
    }


    @Throws(IOException::class)
    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)

            // Creating an http connection to communicate with url
            urlConnection = url.openConnection() as HttpURLConnection

            // Connecting to url
            urlConnection.connect()

            // Reading data from url
            iStream = urlConnection.inputStream

            val br = BufferedReader(InputStreamReader(iStream!!))

            val sb = StringBuffer()

            var line: String? = ""
            while(line!=null){
                line = br.readLine()
                if (line == null) break

                sb.append(line)
            }

            data = sb.toString()
            Log.d("downloadUrl", data.toString())
            br.close()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        } finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        return data
    }

    fun onCurrentLocationChanged(location: Location?) {
        if (location == null) {
            return
        }
        val latLng = LatLng(location.latitude, location.longitude)
        if (currentLocation == null) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F))
        }
        currentLocation = latLng
    }

    //define the listener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("Location", "latitude: " + location.latitude + ", longitude: " + location.longitude)
            onCurrentLocationChanged(location)
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun run(url: String, callback: (Call, Response) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
                .url(url)
                .header("Authorization", "Token " + Constants.uberApiKey())
                .header("Accept-Language", "pt_BR")
                .header("Content-Type", "application/json")
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                callback(call, response)
            }
        })
    }
}
