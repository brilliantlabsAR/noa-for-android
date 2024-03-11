package xyz.brilliant.argpt.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import xyz.brilliant.argpt.ui.activity.BaseActivity

class PermissionHelper(private val activity: Activity) {


    interface PermissionCallback {
        fun onPermissionsGranted()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 5001
        private const val REQUEST_FINE_LOCATION = 1001
        private const val REQUEST_ENABLE_BLUETOOTH = 1002
        private const val REQUEST_ENABLE_GPS = 1003

        private val permissionsSDK33 = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.FOREGROUND_SERVICE,
            //Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private val permissionsSDK29 = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.FOREGROUND_SERVICE,
            // Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun requestAllPermissionAndCheckBluetoothAndGps(callback: PermissionCallback) {
        try {
            val permissions = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                permissionsSDK29
            } else {
                permissionsSDK33
            }

            val permissionsToRequest = mutableListOf<String>()
            for (permission in permissions) {
                val result = ContextCompat.checkSelfPermission(activity, permission)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                checkBluetoothAndGps(callback)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
     fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

     fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FINE_LOCATION
        )
    }
     fun checkBluetoothAndGps(callback: PermissionCallback) {
        val bluetoothEnabled = isBluetoothEnabled()
        val gpsEnabled = isGpsEnabled()

        if (bluetoothEnabled && gpsEnabled) {
            // Both Bluetooth and GPS are enabled, proceed with further actions
            callback.onPermissionsGranted()
        } else {
            if (!bluetoothEnabled) {
                showBluetoothAlertDialog()
            }
            if (!gpsEnabled) {
                showGpsAlertDialog()
            }
        }
    }

     fun isGpsEnabled(): Boolean {
        return try {
            val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            false
        }
    }

     fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
     fun showBluetoothAlertDialog() {
        val builder = android.app.AlertDialog.Builder(activity)
        builder.setMessage("Bluetooth is disabled. Do you want to enable it?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                activity.finish()
            }
        builder.setCancelable(false)
        builder.create().show()
    }

     fun showGpsAlertDialog() {
        val builder = android.app.AlertDialog.Builder(activity)
        builder.setMessage("GPS is disabled. Do you want to enable it?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                activity.startActivityForResult(intent, REQUEST_ENABLE_GPS)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                activity.finish()
            }
        builder.setCancelable(false)
        builder.create().show()
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        callback: PermissionCallback
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                callback.onPermissionsGranted()
            } else {
                showPermissionPopup()
            }
        }
    }

     fun showPermissionPopup() {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage("Please grant all the required permissions to use this app.")
            .setPositiveButton("OK") { dialog, which ->
                openAppSettings()
            }
            .setCancelable(false)
            .show()
    }

     fun openAppSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_SETTINGS)
        }
        activity.startActivity(intent)
    }

    // Inside PermissionHelper class

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?, callback: PermissionCallback) {
        when (requestCode) {
            REQUEST_ENABLE_BLUETOOTH -> {
                if (resultCode == Activity.RESULT_OK) {
                    checkBluetoothAndGps(callback)
                } else {
                    checkBluetoothAndGps(callback)
                }
            }
            REQUEST_ENABLE_GPS -> {
                if (resultCode == Activity.RESULT_OK) {
                    checkBluetoothAndGps(callback)
                } else {
                    checkBluetoothAndGps(callback)
                }
            }
            PERMISSION_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    activity.startActivityForResult(intent, REQUEST_ENABLE_GPS)
                    checkBluetoothAndGps(callback)
                } else {
                    requestAllPermissionAndCheckBluetoothAndGps(callback)
                }
            }
        }
    }

}

