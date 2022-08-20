package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var settingsClient: SettingsClient
    lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            navigateToSelectLocation()

        }

        binding.saveReminder.setOnClickListener {



            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val reminderDataItem =
                ReminderDataItem(title, description, location, latitude, longitude)
            if (_viewModel.validateEnteredData(
                    reminderDataItem
                )
            ) {
                //TODO: use the user entered reminder details to:
                //1) add a geofencing request
                //2) save the reminder to the local db
                //3) navigate to the reminders list fragment
                //but before attempting to do this check
                // if the user has granted the permission to access the background location
                // and location setting is active
                //check if the user has granted the permission to access the background location and location setting is active
                // , if not request it
                if (!BACKGROUND_LOCATION_PERMISSION_STATUS || !LOCATION_SETTING_STATUS) {
                    checkRequiredPermissions()
                }
                if (BACKGROUND_LOCATION_PERMISSION_STATUS && LOCATION_SETTING_STATUS) {
                    val geofence = Geofence.Builder()
                        .setRequestId(reminderDataItem.id)// Set the circular region of this geofence.
                        .setCircularRegion(
                            reminderDataItem.latitude!!,
                            reminderDataItem.longitude!!,
                            GEOFENCE_RADIUS_IN_METERS
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build()
                    val geofencingRequest = GeofencingRequest.Builder().addGeofence(geofence)
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).build()
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                        addOnSuccessListener {
                            _viewModel.validateAndSaveReminder(reminderDataItem)

                        }
                        addOnFailureListener {
                            _viewModel.showErrorMessage.value = "Failed to add geofence"
                        }
                    }
                }

            }


            //reset permissions flags to false for the next time
            BACKGROUND_LOCATION_PERMISSION_STATUS = false
            LOCATION_SETTING_STATUS = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkRequiredPermissions() {
        //TODO("Not yet implemented")
        requestBackgroundLocationPermission()
        requestLocationSetting()


    }


    private fun navigateToSelectLocation() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            BACKGROUND_LOCATION_PERMISSION_STATUS = true
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                REQUEST_BACKGROUND_LOCATION_PERMISSION
            )

        }
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
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
            LOCATION_SETTING_STATUS = true
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


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            REQUEST_BACKGROUND_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BACKGROUND_LOCATION_PERMISSION_STATUS = true

                } else {
                    Snackbar.make(
                        binding.root,
                        "Please grant location permission",
                        Snackbar.LENGTH_LONG
                    ).show()

                }
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    LOCATION_SETTING_STATUS = true
                }
            }
        }
    }

    companion object {
        const val GEOFENCE_RADIUS_IN_METERS = 40f//recommended 20-50
        private const val REQUEST_BACKGROUND_LOCATION_PERMISSION = 0
        private var BACKGROUND_LOCATION_PERMISSION_STATUS = false
        private var LOCATION_SETTING_STATUS = false
        private const val REQUEST_CHECK_SETTINGS = 1


    }
}
