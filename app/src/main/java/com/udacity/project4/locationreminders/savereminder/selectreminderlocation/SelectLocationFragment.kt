package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


//import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
//import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SelectLocationFragment : OnMapReadyCallback, GoogleMap.OnMarkerClickListener, BaseFragment() {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var _selectedPOI: PointOfInterest

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        /* if (activity is AppCompatActivity) {
             (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(
                 true
             )
         }*/

        //TODO: add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence

        _viewModel.apply {
            if (validateAndRegisterSelectedPoi()) {
                navigationCommand.postValue(
                    NavigationCommand.Back
                )
            }
        }


    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /*
    * I don't need to request any permissions as i am already sure needed permissions are granted in the SaveReminderFragment
    * i Don't request Permissions here Because it causes the map loading failures
    * until a later reopen of this fragment then every this works as expected
    * */
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(p0: GoogleMap?) {

        when {
            p0 != null -> {
                map = p0
                mapToCurrentLocation()

                try {
                    // Customise the styling of the base map using a JSON object defined
                    // in a raw resource file.
                    val success: Boolean = map.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            requireContext(), R.raw.map_style
                        )
                    )
                    if (!success) {
                        Log.e("MapsActivityRaw", "Style parsing failed.")
                    }
                } catch (e: Resources.NotFoundException) {
                    Log.e("MapsActivityRaw", "Can't find style.", e)
                }
                map.setOnPoiClickListener {
                    map.clear()
                    map.addMarker(MarkerOptions().position(it.latLng).title(it.name))
                        .showInfoWindow()
                    setSelectedPOI(it)
                }
                map.setOnMapLongClickListener {
                    map.clear()
                    map.addMarker(
                        MarkerOptions().position(it).title(getString(R.string.dropped_pin))
                    )
                    setSelectedPOI(
                        PointOfInterest(
                            it,
                            getString(R.string.lat_long_snippet, it.latitude, it.longitude),
                            getString(R.string.dropped_pin)
                        )
                    )
                }
                map.setOnMyLocationButtonClickListener {
                    mapToCurrentLocation()
                    true
                }
                map.setOnMyLocationClickListener { }

                val initialMarkerLatLng = LatLng(37.4221, -122.0841)//googleplex coordinates
                map.addMarker(MarkerOptions().position(initialMarkerLatLng).title("googleplex"))
                setSelectedPOI(PointOfInterest(initialMarkerLatLng, "googleplex", "googleplex"))

            }
        }


    }

    @SuppressLint("MissingPermission")
    fun getAndFoucsCurrentLocation() {


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    val currentLocation = LatLng(location.latitude, location.longitude)

                    val cameraUpdateFactory =
                        newLatLngZoom(currentLocation, 16f)
                    map.animateCamera(cameraUpdateFactory)

                }
            }

        //clear the flag to be ready to check if Location setting is changed
        // for the next time the user wants to get the location

        LOCATION_SETTING_STATUS = false
    }

    @SuppressLint("MissingPermission")
    fun mapToCurrentLocation() {
        requestForegroundLocationPermission()

    }


    private fun setSelectedPOI(POI: PointOfInterest) {
        _viewModel.selectedPOI.postValue(POI)
    }
/*
TODO:make use of this function to give more detailed address data
    private fun getAddress(lat: LatLng): String? {
        val geocoder = Geocoder(requireActivity())
        val list = geocoder.getFromLocation(lat.latitude, lat.longitude, 1)
        return list[0].getAddressLine(0)
    }
*/

    override fun onMarkerClick(p0: Marker?): Boolean {
        //TODO("Not yet implemented")
        return try {
            map.clear()
            true
        } catch (e: Exception) {
            false
        }

    }

    private fun requestForegroundLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            requestLocationSetting()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_LOCATION_PERMISSION
            )
        }
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            REQUEST_FOREGROUND_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                    requestLocationSetting()
                } else {
                    Snackbar.make(
                        binding.root,
                        "Please grant location permission",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("ENABLE") {
                        requestForegroundLocationPermission()
                    }.show()
                }
            }


        }

    }

    private fun requestLocationSetting() {
        //TODO("Not yet implemented")
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            getAndFoucsCurrentLocation()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    getAndFoucsCurrentLocation()
                }
            }
        }
    }


    companion object {


        private const val REQUEST_FOREGROUND_LOCATION_PERMISSION = 1
        private var FOREGROUND_LOCATION_PERMISSION_STATUS = false
        private var LOCATION_SETTING_STATUS = false
        private const val REQUEST_CHECK_SETTINGS = 2
    }

}
