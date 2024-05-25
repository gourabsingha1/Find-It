package com.example.findit.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.findit.R
import com.example.findit.databinding.ActivityLocationPickerBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityLocationPickerBinding
    private var mMap: GoogleMap? = null
    // Current place picker
    private var mPlaceClient: PlacesClient? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    // Current geographical location of device i.e the last known location retrieved by the fused location provider
    private var mLastKnownLocation: Location? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // We will show when user selects or searches location
        binding.doneLl.visibility = View.GONE

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        val fragmentMap = supportFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
        fragmentMap.getMapAsync(this)

        // Initialize the Places client
        Places.initialize(this, getString(R.string.my_google_map_api_key))

        // Create a new PlacesClient instance
        mPlaceClient = Places.createClient(this)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // To search place on map
//        val autocompleteSupportMapFragment = supportFragmentManager.findFragmentById(R.id.fragmentAutoComplete) as AutocompleteSupportFragment
//        // List of location fields we need in search result
//        val placesList = arrayOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
//        autocompleteSupportMapFragment.setPlaceFields(listOf(*placesList))
//        // Listen for place selections
//        autocompleteSupportMapFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener{
//            override fun onError(p0: Status) {
//
//            }
//
//            override fun onPlaceSelected(p0: Place) {
//                val name = p0.name
//                val latLng = p0.latLng
//                selectedLatitude = latLng?.latitude
//                selectedLongitude = latLng?.longitude
//                selectedAddress = p0.address ?: ""
//                addMarker(latLng, name, selectedAddress)
//            }
//
//        })

        // Go back
        binding.btnLocationBack.setOnClickListener {
            onBackPressed()
        }

        // If location is enabled, get and show user's current location
        binding.btnGps.setOnClickListener {
            if(isGPSEnabled()) {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                Toast.makeText(this, "Location is not enabled", Toast.LENGTH_LONG).show()
            }
        }

        // Get the selected location back to requesting activity
        binding.btnLocationDone.setOnClickListener {
            val intent = Intent()
            intent.putExtra("latitude", selectedLatitude)
            intent.putExtra("longitude", selectedLongitude)
            intent.putExtra("address", selectedAddress)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        mMap!!.setOnMapClickListener { latLng->
            selectedLatitude = latLng.latitude
            selectedLongitude = latLng.longitude
            addressFromLatLng(latLng)
        }
    }

    @SuppressLint("MissingPermission")
    private val requestLocationPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if(isGranted) {
                mMap!!.isMyLocationEnabled = true
                pickCurrentPlace()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun addressFromLatLng(latLng: LatLng) {
        val geocoder = Geocoder(this)
        try {
            val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val address = addressList!![0]
            val addressLine = address.getAddressLine(0)
            val subLocality = address.subLocality
            selectedAddress = "$addressLine"
            addMarker(latLng, "$subLocality", "$addressLine")
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickCurrentPlace() {
        if(mMap == null) {
            return
        }
        detectAndShowDeviceLocationMap()
    }

    @SuppressLint("MissingPermission")
    private fun detectAndShowDeviceLocationMap() {
        try {
            val locationResult = mFusedLocationProviderClient!!.lastLocation
            locationResult.addOnSuccessListener { location ->
                if(location != null) {
                    mLastKnownLocation = location
                    selectedLatitude = location.latitude
                    selectedLongitude = location.longitude
                    val latLng = LatLng(selectedLatitude!!, selectedLongitude!!)
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0f))
                    mMap!!.animateCamera(CameraUpdateFactory.zoomTo(12.0f))
                    addressFromLatLng(latLng)
                }
            }.addOnFailureListener { e->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isGPSEnabled() : Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        var networkEnabled = false
        // Check if GPS_PROVIDER is enabled
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
        // Check if NETWORK_PROVIDER is enabled
        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
        return !(!gpsEnabled && !networkEnabled)
    }

    private fun addMarker(latLng: LatLng, title: String, address: String) {
        mMap!!.clear()
        try {
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            markerOptions.title("$title")
            markerOptions.snippet("$address")
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            // Add marker to the map and move camera to the newly added marker
            mMap!!.addMarker(markerOptions)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0f))

            // Show doneLl
            binding.doneLl.visibility = View.VISIBLE
            // Set selected location complete address
            binding.tvSelectedPlaces.text = address
        } catch (e : Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }
}