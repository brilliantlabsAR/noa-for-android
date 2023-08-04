package xyz.brilliant.argpt.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.adapter.DevicesAdapter
import xyz.brilliant.argpt.ui.fragment.ChatGptFragment
import xyz.brilliant.argpt.ui.fragment.ScanningFragment
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.concurrent.thread


class BaseActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BaseActivity"
        private const val REQUEST_FINE_LOCATION = 1001
        private const val PERMISSION_REQUEST_CODE = 5001
        private val REQUEST_ENABLE_BLUETOOTH = 1002
        private val REQUEST_ENABLE_GPS = 1003
        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private const val RX_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
        private const val TX_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

        private val RAW_SERVICE_UUID = UUID.fromString("e5700001-7bac-429a-b4ce-57ff900f479d")
        private const val RAW_RX_UUID = "e5700002-7bac-429a-b4ce-57ff900f479d"
        private const val RAW_TX_UUID = "e5700003-7bac-429a-b4ce-57ff900f479d"
        private const val GATT_MAX_MTU_SIZE = 100
        private val client = OkHttpClient()
        fun pushFragmentsStatic(
            fragmentManager: FragmentManager,
            fragment: Fragment,
            shouldAdd: Boolean,
            tag: String?
        ) {
            val ft: FragmentTransaction = fragmentManager.beginTransaction()
            //ft.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
            ft.replace(R.id.fragmentContainer, fragment, tag)
            if (shouldAdd) {
                ft.addToBackStack("ScreenStack")
            } else {
                fragmentManager.popBackStack(null, 0)
            }
            ft.commit()
        }
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var recyclerView: RecyclerView

    var apiKey = ""
    private val handler = Handler(Looper.getMainLooper())
    private var scanning: Boolean = false
    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var writingREPLProgress: Boolean = false
    val fragmentManager = supportFragmentManager

    private var rawRxCharacteristic: BluetoothGattCharacteristic? = null
    private var rawTxCharacteristic: BluetoothGattCharacteristic? = null
    private val mArrayList = ArrayList<ScanResult>()
    private var audioBuffer: ByteArray = byteArrayOf(0)
    private val PREFS_FILE_NAME = "MyPrefs"
    private val PREFS_FILE_NAME2 = "ApiKey"
    private val PREFS_KEY_DEVICE_ADDRESS = "DeviceAddress"
    private val PREFS_KEY_API_KEY = "DeviceAddress"
    private fun getStoredDeviceAddress(): String {
        val prefs = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_KEY_DEVICE_ADDRESS, "") ?: ""
    }
    public fun getStoredApiKey(): String {
        val prefs = getSharedPreferences(PREFS_FILE_NAME2, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_KEY_API_KEY, "") ?: ""
    }

    private fun storeDeviceAddress(deviceAddress: String) {
        val prefs = getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(PREFS_KEY_DEVICE_ADDRESS, deviceAddress)
        editor.apply()
    }
    public fun unpairMonocle(){
        storeDeviceAddress("")
        finish()
        startActivity(getIntent())
    }
    public fun storeApiKey(_apiKey: String) {
        val prefs = getSharedPreferences(PREFS_FILE_NAME2, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(PREFS_KEY_API_KEY, _apiKey)
        editor.apply()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        getAllPermission()
     }

    private fun firstCodeExecute(){
        try {
            val storedDeviceAddress = getStoredDeviceAddress()
            if(apiKey.isEmpty()) {
                apiKey = getStoredApiKey();
            }
            if(storedDeviceAddress.isNullOrBlank()){
                val fragment = ScanningFragment()
                pushFragmentsStatic(fragmentManager, fragment, false, "start_scan")
            }else{
                val fragment = ChatGptFragment()
                pushFragmentsStatic(fragmentManager, fragment, false, "chat_gpt")
            }

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT)
                    .show()
                finish()
                return
            }
            if(storedDeviceAddress.isNullOrBlank()) {
                updateProgressDialog("Bring your Device close","Searching")
                //  showProgressDialog("Bring your Device close") { isSuccess ->
                // Callback function invoked after the progress dialog is dismissed
            }

            if (storedDeviceAddress.isNotEmpty()) {
                // Auto-connect to the stored device
                connectDevice(storedDeviceAddress)
            } else {
                // No stored device, start the scanning process
                startScan()
            }
            // devicesAdapter = DevicesAdapter(this)
        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.FOREGROUND_SERVICE,
    )
    private fun getAllPermission(){
        try {
            val permissionsToRequest = mutableListOf<String>()
            for (permission in permissions) {
                val result = ContextCompat.checkSelfPermission(this, permission)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            } else {
                // All permissions are already granted. You can proceed with your operation here.
                Log.d(TAG, "getAllPermission: 12")
               // checkBluetoothAndGps()

            }
        }catch (ex:Exception){
            ex.printStackTrace()
        }

    }
    @SuppressLint("MissingPermission")
    private fun showBluetoothAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Bluetooth is disabled. Do you want to enable it?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH)
            }
            .setNegativeButton("No") { dialog: DialogInterface, _ ->
                dialog.dismiss()
                checkBluetoothAndGps()
            }
        builder.create().show()
    }

    private fun showGpsAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("GPS is disabled. Do you want to enable it?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, REQUEST_ENABLE_GPS)
            }
            .setNegativeButton("No") { dialog: DialogInterface, _ ->
                dialog.dismiss()
                checkBluetoothAndGps()
            }
        builder.create().show()
    }


    private fun isGpsEnabled(): Boolean {

        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var resultl = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                var result3 = locationManager.isLocationEnabled
                return locationManager.isLocationEnabled
            }
            else
            {
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            }

        }
    catch ( ex : Exception){
       var exception = ex.message
        return false
    }
    }
    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.isEnabled == true
    }

    private fun checkBluetoothAndGps() {
        val bluetoothEnabled = isBluetoothEnabled()
        val gpsEnabled = isGpsEnabled()

        if (bluetoothEnabled && gpsEnabled) {
            // Both Bluetooth and GPS are enabled, execute your first code
            firstCodeExecute()
        } else {

            if (!bluetoothEnabled) {
                showBluetoothAlertDialog()
               // showGpsAlertDialog()
            }
           else if (!gpsEnabled) {
                showGpsAlertDialog()
            }
        }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== REQUEST_ENABLE_BLUETOOTH){
            if(resultCode== RESULT_OK){

                checkBluetoothAndGps()

            }else{
                checkBluetoothAndGps()
                Toast.makeText(
                    this,
                    "Please turn on bluetooth!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (requestCode == REQUEST_ENABLE_GPS) {
            if (resultCode == RESULT_OK) {
                checkBluetoothAndGps()
            } else {

                checkBluetoothAndGps()
                Toast.makeText(
                    this,
                    "Please turn on GPS!",
                    Toast.LENGTH_SHORT
                ).show()
                // User didn't enable GPS, handle as needed
            }
        }


        else if(requestCode==PERMISSION_REQUEST_CODE){
            if(resultCode== RESULT_OK){

                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, REQUEST_ENABLE_GPS)
                checkBluetoothAndGps()
            }else{
                getAllPermission()
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions are granted
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                checkBluetoothAndGps()
                // All permissions are granted. You can proceed with your operation here.
                Log.d(TAG, "onRequestPermissionsResult: ")
               // firstCodeExecute()
            } else {
                // Handle the case where some permissions are not granted.
                // You may want to inform the user or handle the missing permissions accordingly.
                Log.d("BASE ACTIVITY","Permission Not given");
                showPermissionPopup()

            }
        }
    }
    override fun onResume() {
        super.onResume()

        // Check if the required permissions are granted when the activity resumes
//        val allPermissionsGranted = permissions.all {
//            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
//        }
//        Log.d(TAG, "onResume: "+allPermissionsGranted)
//        if (allPermissionsGranted) {
//            // All permissions are granted. You can proceed with your operation here.
//            firstCodeExecute()
//        } else {
//            // Handle the case where some permissions are not granted.
//            // You may want to inform the user or handle the missing permissions accordingly.
//            Log.d("BASE ACTIVITY", "Some Permissions Not Given")
//            //showPermissionPopup()
//        }
    }

    private fun showPermissionPopup(){
       try {
           AlertDialog.Builder(this)
               .setTitle("Permissions Required")
               .setMessage("Please grant all the required permissions to use this app.")
               .setPositiveButton("OK") { dialog, which ->
                   // You can take appropriate action here, such as redirecting the user to the app settings page.
                   // For example:
                    openAppSettings()
               }
               .setCancelable(false)
               .show()
       }catch (ex:Exception){
           ex.printStackTrace()
       }

    }
    private fun openAppSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_SETTINGS)
        }
        startActivity(intent)
    }
var connectionStatus = ""
    fun updateConnectionStatus(status: String) {
        connectionStatus = status
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is ChatGptFragment) {
            fragment.updateConnectionStatus(status)
        }
    }
    fun updatechatList( type : String , msg : String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is ChatGptFragment) {
            fragment.updatechatList( type, msg)
        }
    }
//    lateinit var dialog: Dialog;
    private lateinit var deviceCloseTextView: TextView
    private lateinit var popUpbtn: TextView
    private lateinit var searchBox: RelativeLayout

    fun connectDevice(){
        try {
            val firstItem: ScanResult? = if (mArrayList.size == 1) {
                mArrayList[0]
            } else if (mArrayList.size > 1) {
                mArrayList.minByOrNull { it.rssi }
            } else {
                null
            }
            if (firstItem != null) {
                stopScan()
                connectDevice(firstItem.device.address)
            }
        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }
    fun updateProgressDialog(deviceCloseText: String, btnText: String) {

        val newDeviceCloseText = deviceCloseText
        runOnUiThread {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (fragment is ScanningFragment) {
                fragment.updatePopUp(deviceCloseText,btnText)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
        disconnectGatt()
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (hasLocationPermission()) {
            scanning = true
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()
            val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BaseActivity.SERVICE_UUID))
                .build()
            bluetoothAdapter.bluetoothLeScanner.startScan(
                listOf(scanFilter),
                scanSettings,
                scanCallback
            )
        } else {
            requestLocationPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (scanning) {
            scanning = false
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            BaseActivity.REQUEST_FINE_LOCATION
        )
    }

    var popuptxt: String = "Bring your Device Close"
    var popUpbtntxt = "Searching"
    var showPopUp: Boolean = false
    var filesUploadStr:String=""
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission", "SuspiciousIndentation")
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val deviceName = result.device.name
            val storedDeviceAddress = getStoredDeviceAddress()
            if(storedDeviceAddress.isNotEmpty()){
                if(storedDeviceAddress==result.device.address){
                    connectDevice(result.device.address)
                    stopScan()
                }
                return
            }
            mArrayList.add(result)

            if (mArrayList.size > 0) {
                stopScan()
                updateProgressDialog("Monocle Found", "Connect")
            } else {
                updateProgressDialog("Bring your Device Close", "Searching")
            }
            //
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(BaseActivity.TAG, "Scan failed with error code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectDevice(deviceAddress: String) {
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                updateConnectionStatus("")

                runOnUiThread {

                    // logTextView.text = "Connected: ${gatt.device.name} ${gatt.device.address}"
                    Toast.makeText(this@BaseActivity, "Connected to device", Toast.LENGTH_SHORT)
                        .show()
                }

                gatt.requestMtu(256);
                // Handler(Looper.getMainLooper()).post {
                val storedDeviceAddress = getStoredDeviceAddress()
                if(storedDeviceAddress.isNullOrBlank()){
                    updateProgressDialog("Update Monocle", "Continue")
                }

                // }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                val storedDeviceAddress = getStoredDeviceAddress()

                disconnectGatt()
                if(storedDeviceAddress.isNullOrBlank()){

                    updateProgressDialog("Bring your Device Close", "Searching")
                }else{
                    updateConnectionStatus("not connected")
                }



                startScan()
                runOnUiThread {
                    //  logTextView.text = "Disconnected"
                    Toast.makeText(
                        this@BaseActivity,
                        "Disconnected from device",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {

                val service = gatt.getService(BaseActivity.SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic =
                        service.getCharacteristic(UUID.fromString(BaseActivity.RX_UUID))
                    txCharacteristic =
                        service.getCharacteristic(UUID.fromString(BaseActivity.TX_UUID))
                    if (rxCharacteristic != null && txCharacteristic != null) {
                        gatt.setCharacteristicNotification(txCharacteristic, true)
                        val descriptor =
                            txCharacteristic!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)

                    }
                }

                //rawDataObjects
                handler.postDelayed({
                    val rawservice = gatt.getService(BaseActivity.RAW_SERVICE_UUID)
                    if (rawservice != null) {
                        rawRxCharacteristic =
                            rawservice.getCharacteristic(UUID.fromString(BaseActivity.RAW_RX_UUID))
                        rawTxCharacteristic =
                            rawservice.getCharacteristic(UUID.fromString(BaseActivity.RAW_TX_UUID))
                        if (rawRxCharacteristic != null && rawTxCharacteristic != null) {
                            gatt.setCharacteristicNotification(rawTxCharacteristic, true)
                            val descriptor =
                                rawTxCharacteristic!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)

                        }
                    }
                    handler.postDelayed({
                        fileUploadOne()
                        // sendChatGptResponce("This is the response")
                    }, 1000)
                }, 500)


            }


        }

        @SuppressLint("MissingPermission", "SuspiciousIndentation")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS)

                runOnUiThread {
                    //   logTextView.append("MTU changed: $mtu\n")
                }

            gatt.discoverServices()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Characteristic read successfully
                // if (characteristic.uuid == YOUR_CHARACTERISTIC_UUID) {
                val receivedData = characteristic.value

                // Process the received data as needed

                // Example: Convert the received data to a string
                val receivedString = receivedData?.toString(Charsets.UTF_8)

                // Handle the received data or perform any necessary actions
                // You can update the UI, trigger further operations, etc.
                //  }
            } else {
                // Error occurred while reading the characteristic
            }
        }


        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                writingREPLProgress = false
                val r = characteristic.getValue()
                val value = String(characteristic.value)
                print(value)


//                runOnUiThread {
//                    logTextView.append("Received: $value\n")
//                }
            }
        }


        var statesUploaded: Boolean = false
        var graphicsUploaded: Boolean = false
        var mainUploaded: Boolean = false


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
//            super.onCharacteristicChanged(gatt, characteristic)
//            if()
            CoroutineScope(Dispatchers.Default).launch {
                if (characteristic.uuid == rawTxCharacteristic?.uuid) {
                    monocleRecieved(characteristic.value)
                    return@launch
                }
                val value = characteristic.value
                val receivedData = String(value)
                runOnUiThread {
                    //  logTextView.append("Received: $receivedData\n")
                }
                if (receivedData.contains("State:")) {

                    return@launch
                }
                if (receivedData.contains("raw REPL")) {
                    filesUploadStr = ""
                    val data = "import os;print('checkFile',os.listdir())"
                    bleWrite(data.toByteArray() + byteArrayOf(0x04))
                } else if (receivedData.contains("checkFile")) {
                    filesUploadStr = receivedData


                }
                if (filesUploadStr.contains("checkFile")) {
                    startFileUpload(receivedData)
                }
                if (filesUploadStr == "complete") {
                    bleWrite(byteArrayOf(0x03, 0x04))
                    val storedDeviceAddress = getStoredDeviceAddress()
                    if(storedDeviceAddress.isNullOrBlank()){
                        val fragment = ChatGptFragment()
//                        dialog.dismiss()
                        if(!fragmentManager.fragments.contains(fragment)){
                            pushFragmentsStatic(fragmentManager, fragment, false, "chat_gpt")
                        }
                        val apikeyStored =  getStoredApiKey()
                        if(apikeyStored.isNullOrEmpty()){
//                            fragment.openChangeApiKey()
                        }else{
                            apiKey = apikeyStored
                        }
                        updateConnectionStatus("")

                    }
                    filesUploadStr = ""
                    storeDeviceAddress(gatt.device.address)

                }
            }

        }

    }


    @SuppressLint("MissingPermission")
    private fun disconnectGatt() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
    }

    @SuppressLint("MissingPermission")
    private fun send(data: String) {
        val data = data + "\r\n"


        val characteristic = rxCharacteristic
        if (bluetoothGatt != null && characteristic != null) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

            //   characteristic.setValue(data.toByteArray(),BluetoothGattCharacteristic.FORMAT_SINT8, 0);

            val byteArray = byteArrayOf(0x03, 0x01)
            characteristic.value = data.toByteArray()

            bluetoothGatt!!.writeCharacteristic(characteristic)
        }
    }


    @SuppressLint("MissingPermission")
     fun fileUploadOne() {
        bleWrite(byteArrayOf(0x03,0x03,  0x01))
    }

    private fun readFileFromAssets(fileName: String): String {
        val assetManager: AssetManager = applicationContext.assets
        return assetManager.open(fileName).bufferedReader().use {
            it.readText()
        }
    }



    @SuppressLint("MissingPermission")
    private fun deleteAvailableFile(fileName: String) {


        val data = "import os ;os.remove('$fileName');print('Delete')"
        bleWrite(data.toByteArray() + byteArrayOf(0x04))
    }

    private fun  startFileUpload( currentStatus: String){
        if(currentStatus=="OK"){ return}
        if(currentStatus.contains("Error")){ return}
        if(currentStatus==filesUploadStr && !filesUploadStr.contains("states.py") ){
            fileUpload("states.py")
        }else if(currentStatus.contains("states.py") && !filesUploadStr.contains("main.py")){
            fileUpload("main.py")
        }else if(currentStatus.contains("main.py")  && !filesUploadStr.contains("graphics.py")){
            fileUpload("graphics.py")
        }else if(currentStatus.contains("graphics.py") || (filesUploadStr.contains("states.py") && filesUploadStr.contains("main.py") && filesUploadStr.contains("graphics.py"))){
            filesUploadStr = "complete"

        }

    }
    @SuppressLint("MissingPermission")
    private fun fileUpload(fileName: String) {
        val data = readFileFromAssets(fileName)
        val datasend = "f=open('$fileName','w');f.write('''$data''');f.close();print('$fileName')"


        val databytearray =  byteArrayOf(0x03)+datasend.toByteArray() + byteArrayOf(0x04)

        bleWrite(databytearray)
    }

    private fun monocleRecieved(data:ByteArray){
        val status = String(data.slice(0 until 4).toByteArray())
        if(status=="ast:"){
//            new audio buffer start, delete old one
            audioBuffer = byteArrayOf(0)
        }
        if (status=="dat:"){
//            audio buffer append
            audioBuffer += data.slice(4 until data.size).toByteArray()
        }
        if (status=="aen:"){
//            process audio buffer
            // Create an AudioTrack object with the same sample rate and format as the audio data
            var responseData = "pin:"+"hello"

            rawBleWrite(responseData.toByteArray())

            val sampleRate = 8000
            val bit_per_sample = 16
            val channels = 1
            val path = applicationContext.filesDir
            val f2 = File(path,"test.wav")
            f2.absolutePath
            try {
                rawToWave(signed8ToUnsigned16(audioBuffer),f2,sampleRate, bit_per_sample,channels) { success ->
                    if (success) {
                        // File created successfully, now upload it to Whisper API
                        uploadAudioFile(f2, byteCallback)
                    } else {
                        // File creation failed, handle the error
                        // ...
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace();
            }

        }
        if(status=="pon:"){
//            var responseData = "res:"+"Got response"
//
//            rawBleWrite(responseData.toByteArray())
        }
    }
    fun signed8ToUnsigned16(byteArray: ByteArray): ByteArray {
        val unsignedArray = ByteArray(byteArray.size * 2)
        for (i in byteArray.indices) {
            val unsigned = (byteArray[i].toInt() and 0xFF) shl 8
            unsignedArray[i * 2] = (unsigned ushr 8).toByte()
            unsignedArray[i * 2 + 1] = unsigned.toByte()
        }
        return unsignedArray
    }

    fun uploadAudioFile(audioFile: File, byteCallback: Callback) {
        val client = OkHttpClient()

        // Replace 'YOUR_API_KEY' with your actual OpenAI API key
//        val apiKey = "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"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", audioFile.asRequestBody())
            .addFormDataPart("model", "whisper-1")
            // Add additional parameters if required
            .build()


        Log.d("TAG", "uploadAudioFile: "+requestBody.toString())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/translations")
            //.addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()



        client.newCall(request).enqueue(byteCallback)
    }


    val byteCallback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            // Handle request failure
            updatechatList("S",e.message.toString())
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            val body=response.body?.string()
            if (body != null) {
                Log.v("data",body)
            }
            else{
                Log.v("data","empty")
            }
            val jsonObject= JSONObject(body)
            Log.d("TAG", "onResponse: "+jsonObject)
            if (jsonObject.has("text")){

                val textResult = jsonObject.getString("text")
                if(textResult.isNullOrEmpty()){
//                    Toast.makeText(this@BaseActivity,"blank text", Toast.LENGTH_SHORT).show()
                   // updatechatList("S","Text not readable... try again!!")

                    updatechatList("S"," ")
                    getResponse(" ")
                }else{
                    updatechatList("S",textResult.trim())
                    getResponse(textResult)
                }

            }else{
                val error: JSONObject = jsonObject.getJSONObject("error")
                val msg:String=error.getString("message")

                sendChatGptResponce(msg,"err:")

            }
        }
    }



    fun getResponse(question: String){
        try {

            val url="https://api.openai.com/v1/engines/text-davinci-003/completions"

            val requestBody="""
            {
            "prompt": "$question",
            "max_tokens": 500,
            "temperature": 0
            }
        """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("error","API failed",e)
                    updatechatList("R",e.message.toString())

                }

                override fun onResponse(call: Call, response: Response) {
                    val body=response.body?.string()
                    if (body != null) {
                        Log.v("data",body)
                    }
                    else{
                        Log.v("data","empty")
                    }
                    val jsonObject= JSONObject(body)
                    Log.d("TAG", "onResponse: "+jsonObject)

                    if (jsonObject.has("id")) {
                        val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
                        val textResult = jsonArray.getJSONObject(0).getString("text")

                        sendChatGptResponce(textResult,"res:")
//                        callback(textResult)
                    }else{
                        val error: JSONObject = jsonObject.getJSONObject("error")
                        val msg:String=error.getString("message")

                        sendChatGptResponce(msg,"err:")

                    }
                }
            })
        }catch (ex:Exception){
          sendChatGptResponce("getResponse: ${ex.message}","err:")
            Log.d("ChatGpt", "getResponse: $ex")
        }
    }








    @SuppressLint("MissingPermission")
    fun sendChatGptResponce(data: String ,prefix : String) {
        updatechatList("R",data)
        val data = prefix+data //err:
        rawBleWrite(data.toByteArray())
    }

    @SuppressLint("MissingPermission")
    private fun rawBleWrite(data: ByteArray){
        thread() {
            val characteristic = rawRxCharacteristic
            if (bluetoothGatt != null && characteristic != null) {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                var offset = 0
                val chunkSize = 100
                while (offset < data.size) {
                    if (writingREPLProgress) {
                        continue
                    }
                    val length = minOf(chunkSize, data.size - offset)
                    val chunkData = data.slice(offset until offset + length)

                    characteristic.value = chunkData.toByteArray()
                    writingREPLProgress = true
                    bluetoothGatt!!.writeCharacteristic(characteristic)
                    offset += length

                }

            }
        }

    }



    @SuppressLint("MissingPermission")
    private fun bleWrite( data : ByteArray){
        thread() {
            val characteristic = rxCharacteristic
            if (bluetoothGatt != null && characteristic != null) {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                var offset = 0
                val chunkSize = 100
                while (offset < data.size) {
                    if (writingREPLProgress) {
                        continue
                    }
                    val length = minOf(chunkSize, data.size - offset)
                    val chunkData = data.slice(offset until offset + length)

                    characteristic.value = chunkData.toByteArray()
                    writingREPLProgress = true
                    bluetoothGatt!!.writeCharacteristic(characteristic)
                    offset += length

                }

            }
        }
    }
    @Throws(IOException::class)
    private fun rawToWave( rawPCMBytes:ByteArray,waveFile: File,sample_rate:Int,bit_per_sample:Int,channel:Int, callback: (Boolean) -> Unit) {
        var output: DataOutputStream? = null
        try {
            // Write the audio bytes to a wav file with the appropriate header
            output = DataOutputStream(FileOutputStream(waveFile))
// WAVE header
            writeString(output, "RIFF") // chunk id
            writeIntToWave(output, 36 + rawPCMBytes.size) // chunk size
            writeString(output, "WAVE") // format
            writeString(output, "fmt ") // subchunk 1 id
            writeIntToWave(output, 16) // subchunk 1 size
            writeShort(output, 1.toShort()) // audio format (1 = PCM)
            writeShort(output, channel.toShort()) // number of channels
            writeIntToWave(output, sample_rate) // sample rate
            writeIntToWave(output, sample_rate * channel * bit_per_sample/8) // byte rate = sample rate * channels * bytes per sample
            writeShort(output, (channel * bit_per_sample/8).toShort()) // block align = channels * bytes per sample
            writeShort(output, bit_per_sample.toShort()) // bits per sample
            writeString(output, "data") // subchunk 2 id
            writeIntToWave(output, rawPCMBytes.size) // subchunk 2 size
// Audio data (conversion big endian -> little endian)
            val shorts = ShortArray(rawPCMBytes.size / 2)
            ByteBuffer.wrap(rawPCMBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            val bytes = ByteBuffer.allocate(shorts.size * 2)
            for (s in shorts) {
                bytes.putShort(s)
            }
            output.write(bytes.array())

        } finally {
            output?.close()
            callback(true)
        }
    }

    @Throws(IOException::class)
    private fun writeIntToWave(output: DataOutputStream, value: Int) {
        output.write(value shr 0)
        output.write(value shr 8)
        output.write(value shr 16)
        output.write(value shr 24)
    }

    @Throws(IOException::class)
    private fun writeShort(output: DataOutputStream, value: Short) {
        output.write(value.toInt() shr 0)
        output.write(value.toInt() shr 8)
    }

    @Throws(IOException::class)
    private fun writeString(output: DataOutputStream, value: String) {
        for (i in 0 until value.length) {
            output.write(value[i].code)
        }
    }


}
