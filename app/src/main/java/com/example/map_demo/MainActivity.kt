package com.example.map_demo

import android.Manifest
import android.Manifest.permission
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerDragListener {

    private var mLocationPermissionGranted: Boolean = false
    private val PERMISSION_REQUEST_CODE: Int = 9001
    private val GPS_REQUEST_CODE: Int = 9003
    private val TAG: String = "GoogleMaps"

    private lateinit var mBtnLocate: ImageButton
    private lateinit var mMap: GoogleMap
    private lateinit var geocoder: Geocoder
    private lateinit var fab: FloatingActionButton
    private lateinit var mSearchAddress: EditText
    private lateinit var mLocationClient: FusedLocationProviderClient
    private lateinit var mlocationCallback: LocationCallback

    private val MYFLAT_LAT = 22.631413
    private val MYFLAT_LNG = 88.393155

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        geocoder = Geocoder(this, Locale.getDefault())
        // Fragment Implemantion
        val supportMapFragment = SupportMapFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.map_fragment, supportMapFragment)
            .commit()
        supportMapFragment.getMapAsync(this)

        // Floating Action Button
        fab = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            if (mMap != null) {
                val topBoundry = MYFLAT_LAT + 0.1
                val rightBoundry = MYFLAT_LNG + 0.1
                val bottomBoundry = MYFLAT_LAT - 0.1
                val leftBoundry = MYFLAT_LNG - 0.1

                val MYPLACE_BOUNDS: LatLngBounds = LatLngBounds(
                    LatLng(bottomBoundry, leftBoundry),
                    LatLng(topBoundry, rightBoundry)
                )
                //mMap.setLatLngBoundsForCameraTarget(MYPLACE_BOUNDS)
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(MYPLACE_BOUNDS, 1))
                showMarker(MYPLACE_BOUNDS.center)
            }
        }

        // Image Button and EditText Implementation
        mSearchAddress = findViewById(R.id.et_location_name)
        mBtnLocate = findViewById(R.id.bnt_location_search)
        mBtnLocate.setOnClickListener(this::geoLocate)

        // Aceass Current location
        mLocationClient = FusedLocationProviderClient(this) // LocationServices.getFusedLocationProviderClient(this) this the same line to initilize this
        // Update location used locationCallback interface
        mlocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (locationResult == null) {
                    return
                } else {
                    val location: Location = locationResult.lastLocation
                    Toast.makeText(this@MainActivity, "" + location.latitude + "\n" + location.longitude, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "onLocationResult : Location is : " + location.latitude + "\n" + location.longitude)
                }
            }
        }
        // Function Call
        isServicesOk()
    }

    // Working With geocoding api
    private fun geoLocate(view: View) {
        hideSoftKeyboard(view)

        val locationName: String = mSearchAddress.text.toString()
        // Implementation of Geocoder Api
        try {
            val addressList: List<Address> = geocoder.getFromLocationName(locationName, 1)
            if (addressList.size > 0) {
                // Store the Address in this place
                val address: Address = addressList.get(0)
                // In this line it will get the latitude and longitude from the address variable
                val streetAddress: String = address.getAddressLine(0)
                gotoLocation(address.latitude, address.longitude)
                mMap.addMarker(
                    MarkerOptions().position(LatLng(address.latitude, address.longitude))
                        .title(streetAddress).draggable(true)
                )
                Toast.makeText(this, address.locality, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "geoLocate: Country: " + address.countryName)
            } else {

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun hideSoftKeyboard(view: View) {
        val inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun isServicesOk() {
        if (isGPSEnabled()) {
            if (mLocationPermissionGranted) {
                Toast.makeText(this, "Ready to Map!", Toast.LENGTH_SHORT).show()
            } else {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermission(permission.ACCESS_FINE_LOCATION)
                    }
                }
            }

        }
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providerEnable: Boolean =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (providerEnable) {
            return true
        } else {
            val alertDialog: AlertDialog = AlertDialog.Builder(this).setTitle("Gps Permissions")
                .setMessage("Gps is must Enable")
                .setPositiveButton("Yes", (DialogInterface.OnClickListener { dialog, which ->
                    val intent: Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent, GPS_REQUEST_CODE)
                }))
                .setCancelable(false)
                .show()
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {

            val locationManager: LocationManager =
                getSystemService(LOCATION_SERVICE) as LocationManager
            val providerEnable: Boolean =
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (providerEnable) {
                Toast.makeText(this, "GPS is enable", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "GPS is not Enable, Unable to show user location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestPermission(permission: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
            Toast.makeText(this, "Permission granted...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Toast.makeText(this, "Map is showing", Toast.LENGTH_SHORT).show()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        googleMap.isMyLocationEnabled =
            true // take my current location and set the GPS icon in layout

        mMap = googleMap
        gotoLocation(MYFLAT_LAT, MYFLAT_LNG)

        // At this stape add the the zoom_in and zoom_out work in this place & the take the direction to google maps..
        mMap.uiSettings.isTiltGesturesEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true
        // Add a marker in Sydney and move the camera
        val myFlat = LatLng(22.631413, 88.393155)
        mMap.addMarker(MarkerOptions().position(myFlat).title("Marker in ARKA_FlAT "))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myFlat))

        //Marker Drag Listener -- it is a interface of google map api
        mMap.setOnMarkerDragListener(this)
    }

    private fun gotoLocation(lat: Double, lng: Double) {
        val latLng = LatLng(lat, lng)
        // Set the cameraview
        //val cameraUpdate : CameraUpdate = CameraUpdateFactory.newLatLng(latLng)
        // Set the camera using zoom view
        val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15F)
        mMap.moveCamera(cameraUpdate)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // In this place the change the map type from the menu....
        return when (item.itemId) {
            R.id.maptype_none -> {
                getCurrentLocation()
                getLocationUpdate()
                true
            }
            R.id.maptype_normal -> {
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }
            R.id.maptype_satellite -> {
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            }
            R.id.maptype_terrain -> {
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            }
            R.id.maptype_hybrid -> {
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }

        }
    }

    private fun getCurrentLocation() {
        mLocationClient.lastLocation.addOnCompleteListener {
            if (it.isSuccessful) {
                val location: Location = it.result!!
                gotoLocation(location.latitude, location.longitude)
                Log.d(TAG, "latitude" + location.latitude)
                Log.d(TAG, "latitude" + location.longitude)

            }
        }
    }

    private fun showMarker(latLng: LatLng) {
        val markerOptions: MarkerOptions = MarkerOptions()
        markerOptions.position(latLng)
        mMap.addMarker(markerOptions)
    }

    override fun onMarkerDragEnd(p0: Marker?) {
        Log.d(TAG, " OnMarkerDragEnd")
        val latLng: LatLng = p0!!.position
        try {
            val addresslist: List<Address> =
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresslist.size > 0) {
                val address: Address = addresslist.get(0)
                val streetAddress: String = address.getAddressLine(0)
                // In this stage i will change the markar titel address whare the markar are end..
                p0.title = streetAddress
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun onMarkerDragStart(p0: Marker?) {
        Log.d(TAG, " OnMarkerDragStart")
    }

    override fun onMarkerDrag(p0: Marker?) {
        Log.d(TAG, " OnMarkerDrag")
    }

    private fun getLocationUpdate(){
        val locationRequest : LocationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 2000
        mLocationClient.requestLocationUpdates(locationRequest, mlocationCallback, null)
    }
}


























































