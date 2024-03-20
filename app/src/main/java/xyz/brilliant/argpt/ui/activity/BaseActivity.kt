@file:Suppress("NAME_SHADOWING")

package xyz.brilliant.argpt.ui.activity

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.helpers.PermissionHelper
import xyz.brilliant.argpt.helpers.SharedPreferencesHelper
import xyz.brilliant.argpt.network.RequestAPICallBack
import xyz.brilliant.argpt.service.ForegroundService
import xyz.brilliant.argpt.ui.fragment.ChatGptFragment
import xyz.brilliant.argpt.ui.fragment.DeleteProfileFragment
import xyz.brilliant.argpt.ui.fragment.HackFragment
import xyz.brilliant.argpt.ui.fragment.ProfileFragment
import xyz.brilliant.argpt.ui.fragment.ScanningFragment
import xyz.brilliant.argpt.ui.fragment.TuneFragment
import xyz.brilliant.argpt.ui.login.SocialLoginActivity
import xyz.brilliant.argpt.ui.model.ChatModel
import xyz.brilliant.argpt.utils.ActivityUtil
import xyz.brilliant.argpt.utils.Constant.API_CALL_UPLOAD_FILE
import xyz.brilliant.argpt.utils.Constant.API_DELETE_ACCOUNT
import xyz.brilliant.argpt.utils.Constant.API_SIGNOUT
import xyz.brilliant.argpt.utils.Constant.API_TRANSLATE
import xyz.brilliant.argpt.utils.Constant.AUDIO
import xyz.brilliant.argpt.utils.Constant.CONFIG
import xyz.brilliant.argpt.utils.Constant.ERROR_RESPONSE
import xyz.brilliant.argpt.utils.Constant.IMAGE
import xyz.brilliant.argpt.utils.Constant.MESSAGES
import xyz.brilliant.argpt.utils.Constant.PROMPT
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread
import kotlin.math.ceil


@Suppress("DEPRECATION", "NAME_SHADOWING")
class BaseActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BaseActivity"

        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private const val RX_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
        private const val TX_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

        private val RAW_SERVICE_UUID = UUID.fromString("e5700001-7bac-429a-b4ce-57ff900f479d")
        private const val RAW_RX_UUID = "e5700002-7bac-429a-b4ce-57ff900f479d"
        private const val RAW_TX_UUID = "e5700003-7bac-429a-b4ce-57ff900f479d"

        private val NORDIC_SERVICE_UUID = UUID.fromString("0000fe59-0000-1000-8000-00805f9b34fb")
        private const val NORDIC_CONTROL_UUID = "8ec90001-f315-4f60-9fb8-838830daea50"
        private const val NORDIC_PACKET_UUID = "8ec90002-f315-4f60-9fb8-838830daea50"
        private val FRAME_SERVICE_UUID = UUID.fromString("7A230001-5475-A6A4-654C-8431F6AD49C4")

        private const val FRAME_RX_UUID = "7A230002-5475-A6A4-654C-8431F6AD49C4"
        private const val FRAME_TX_UUID = "7A230003-5475-A6A4-654C-8431F6AD49C4"

        private val FILES =
            arrayListOf<String>("states.py", "graphics.py", "main.py", "audio.py", "photo.py")
        private val FRAME_FILES = arrayListOf<String>("main.lua", "graphics.lua", "state.lua")
        private const val GATT_MAX_MTU_SIZE = 256
        private const val sampleRate = 8000
        private const val bitPerSample = 16
        private const val channels = 1

        // For Debugging
        // For Debugging
        private const val NRFKIT = false
        private const val FIRMWARE_TEST = false
        private const val FPGA_TEST = false
        private const val BACKEND_URL = ""
        private const val USE_CUSTOM_SERVER = false

    }
    private lateinit var bluetoothAdapter: BluetoothAdapter
    var apiKey = ""
    var stabilityApiKey = ""
    private val handler = Handler(Looper.getMainLooper())
    private var scanning: Boolean = false
    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var frameRxCharacteristic: BluetoothGattCharacteristic? = null
    private var frameTxCharacteristic: BluetoothGattCharacteristic? = null
    private var writingREPLProgress: Boolean = false
    val fragmentManager = supportFragmentManager
    var translateEnabled: Boolean = false
    var isPairing: Boolean = false
    private var rawRxCharacteristic: BluetoothGattCharacteristic? = null
    private var rawTxCharacteristic: BluetoothGattCharacteristic? = null

    private var nordicControlCharacteristic: BluetoothGattCharacteristic? = null
    private var nordicPacketCharacteristic: BluetoothGattCharacteristic? = null
    var rawReplResponseCallback: ((String) -> Unit)? = null
    var frameResponseCallback: ((String) -> Unit)? = null
    var controlResponseCallback: ((ByteArray) -> Unit)? = null
    //   MONOCLE AUDIO
    private var globalJpegFilePath: String? = null
    private var bitmap: Bitmap? = null
    data class Fpga(val bin: ByteArray?, val version: String?)
    data class ExtractedData(
        val datBytes: ByteArray?,
        val binBytes: ByteArray?,
        val version: String?
    )

    enum class AppState {
        FIRST_PAIR, SOFTWARE_UPDATE, FPGA_UPDATE, SCRIPT_UPDATE, RUNNING
    }

    var currentAppState = AppState.FIRST_PAIR
    private var currentDevice: String = ""
    private var currentDeviceName: String = ""
    private var audioBuffer: ByteArray = byteArrayOf(0)
    private var imageBuffer: ByteArray = byteArrayOf(0)
    var audioJob: Job? = null
    var lastResponse: String = ""
    var connectionStatus = ""
    private var currentScannedDevice: BluetoothDevice? = null
    private var overlallSoftwareProgress = 0
    private var overlallSoftwareSize = 0
    private var currentConnectionStatus = false
    private var accessToken: String = ""
    private var lastAudioFile: File? = null

    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    private lateinit var permissionHelper: PermissionHelper
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ACTION_START_SCAN") {
                if (bluetoothGatt == null) {
                    val storedDeviceAddress = sharedPreferencesHelper.getStoredDeviceAddress()
                    if (storedDeviceAddress.isNotEmpty()) {
                        connectDevice(storedDeviceAddress)
                        println("[trying to connect in background]")
                    }

                }
            }
        }
    }
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    //  val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR
                    )

                    when (bondState) {
                        BluetoothDevice.BOND_BONDING -> {

                        }

                        BluetoothDevice.BOND_BONDED -> {
                            isPairing = true
                            bluetoothGatt!!.requestMtu(GATT_MAX_MTU_SIZE)
                        }

                        BluetoothDevice.BOND_NONE -> {
                            if (bluetoothGatt != null) {
                                bluetoothGatt!!.disconnect()
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * Method to unpair monocle device
     */
    fun unpairMonocle() {
        try {
            bluetoothGatt!!.device::class.java.getMethod("removeBond")
                .invoke(bluetoothGatt!!.device)
        } catch (e: Exception) {
            Log.e(TAG, "Removing bond has been failed. ${e.message}")
        }
        sharedPreferencesHelper.storeDeviceAddress("")
        disconnectGatt()
        currentAppState = AppState.FIRST_PAIR
        currentDevice = ""
        currentScannedDevice = null
        overlallSoftwareProgress = 0
        performLogout()
    }

    /**
     * Method to logout current Session
     */
    fun performLogout() {
        // Retrieve the token from SharedPreferences
        val token = sharedPreferencesHelper.getTokenFromSharedPreferences()
        val mediaType = "text/plain".toMediaType()
        val body = "".toRequestBody(mediaType)
        val signOutApiCallback = (object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                   // Finish the current activity if you want to
                    finish()
                }
                // Handle network errors or request failures
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Clear SharedPreferences when logout is successful
                    sharedPreferencesHelper.clearSharedPreferences()
                   ActivityUtil.navigateToActivity(this@BaseActivity,SocialLoginActivity::class.java)
                    // Finish the current activity if you want to
                    finish()
                    // Logout was successful, perform any necessary post-logout actions
                } else {
                    //  Toast.makeText(this@BaseActivity, "unauthorized", Toast.LENGTH_LONG).show()

                    // Handle the error response
                }
            }
        })

        RequestAPICallBack.apiCallBackRequest(body,token, API_SIGNOUT,signOutApiCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferencesHelper = SharedPreferencesHelper(this)
        permissionHelper = PermissionHelper(this)
        val intentFilter = IntentFilter("ACTION_START_SCAN")
        registerReceiver(scanReceiver, intentFilter)
        setContentView(R.layout.activity_base)
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
        // Request permissions and check Bluetooth/GPS, then execute firstCodeExecute() if everything is granted and enabled
        permissionHelper.requestAllPermissionAndCheckBluetoothAndGps(object :
            PermissionHelper.PermissionCallback {
            override fun onPermissionsGranted() {
                firstCodeExecute()
            }
        })
    }

    private fun firstCodeExecute() {
        try {
            accessToken = sharedPreferencesHelper.getStoredAccessToken()
            val storedDeviceAddress = sharedPreferencesHelper.getStoredDeviceAddress()
            if (apiKey.isEmpty()) {
                apiKey = sharedPreferencesHelper.getStoredApiKey()
            }
            if (stabilityApiKey.isEmpty()) {
                stabilityApiKey = sharedPreferencesHelper.getStoredStabilityApiKey()
            }
            if (storedDeviceAddress.isBlank()) {
                currentAppState = AppState.FIRST_PAIR
                val fragment = ScanningFragment()
                ActivityUtil.navigateToFragment(fragmentManager, fragment, false, "start_scan")
            } else {
                currentAppState = AppState.RUNNING
                val fragment = ChatGptFragment()
                ActivityUtil.navigateToFragment(fragmentManager, fragment, false, "chat_gpt")

            }

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {

                finish()
                return
            }

            if (currentAppState == AppState.FIRST_PAIR) {
                updateProgressDialog("Bring your device close.", "Searching")
                startScan()
            } else if (currentAppState == AppState.SCRIPT_UPDATE || currentAppState == AppState.RUNNING) {
                startBluetoothBackground()
                connectDevice(storedDeviceAddress)
                if (bluetoothGatt != null) {
                    updateConnectionStatus("not connected")
                } else {
                    updateConnectionStatus("")
                }
            }


        } catch (ex: Exception) {
            ex.printStackTrace()
        }

//        currentAppState = AppState.RUNNING
//        val fragment = ChatGptFragment()
//        pushFragmentsStatic(fragmentManager, fragment, false, "chat_gpt") // for testing**

    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionHelper.handleActivityResult(
            requestCode,
            resultCode,
            data,
            object : PermissionHelper.PermissionCallback {
                override fun onPermissionsGranted() {
                    // Proceed with your desired action after both Bluetooth and GPS are enabled
                    firstCodeExecute()
                }
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestPermissionsResult(
            requestCode,
            grantResults,
            object : PermissionHelper.PermissionCallback {
                override fun onPermissionsGranted() {
                    // Here, you can call the function from the helper class
                    // that checks Bluetooth and GPS after permissions are granted
                    permissionHelper.checkBluetoothAndGps(object :
                        PermissionHelper.PermissionCallback {
                        override fun onPermissionsGranted() {
                            // Proceed with your desired action after both Bluetooth and GPS are enabled
                            firstCodeExecute()
                        }
                    })
                }
            })
    }

    /**
     * Method to start bluetooth service in background
     */
    private fun startBluetoothBackground() {
        val foregroundServiceIntent = Intent(this, ForegroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(foregroundServiceIntent)
        } else {
            startService(foregroundServiceIntent)
        }
    }
    /**
     * Method to update connection status
     */
    fun updateConnectionStatus(status: String) {
        connectionStatus = status
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is ChatGptFragment) {
            fragment.updateConnectionStatus(status)
        }
    }
    /**
     * Method to update chat list with string data
     */
    fun updateChatList(type: String, msg: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is ChatGptFragment) {
            fragment.updatechatList(type, msg)
        }
    }

    /**
     * Method to update popup text
     */
    fun updateProgressDialog(deviceCloseText: String, btnText: String) {

        //   val newDeviceCloseText = deviceCloseText
        runOnUiThread {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (fragment is ScanningFragment) {
                fragment.updatePopUp(deviceCloseText, btnText)
            }
        }

    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        disconnectGatt()
        val stopServiceIntent = Intent(this, ForegroundService::class.java)
        stopService(stopServiceIntent)
        unregisterReceiver(scanReceiver)
        unregisterReceiver(bluetoothReceiver)
        super.onDestroy()
    }


    /**
     * Method to know application is background or foreground
     */
    fun isAppInBackground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses
        val packageName = context.packageName

        for (appProcess in appProcesses) {
            if (appProcess.processName == packageName) {
                // Check if the app is in the foreground or background
                return appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }
        }

        return true // App is considered in the background if the process is not found
    }


    /**
     * Method to start BLE scan
     */
    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (scanning) {
            return
        }
        if (permissionHelper.hasLocationPermission()) {
            val serviceUuids = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build(),
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(NORDIC_SERVICE_UUID))
                    .build(),
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(FRAME_SERVICE_UUID))
                    .build()

            )
            scanning = true
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()
            bluetoothAdapter.bluetoothLeScanner.startScan(
                serviceUuids,
                scanSettings,
                scanCallback
            )
        } else {
            permissionHelper.requestLocationPermission()
        }
    }

    /**
     * Method to stop BLE scan
     */
    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (scanning) {
            scanning = false
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
            currentScannedDevice = null
        }
    }
    /**
     * Method to show scan result
     */
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission", "SuspiciousIndentation")
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val deviceName = result.device.name
            println(deviceName)
            if (currentAppState == AppState.SOFTWARE_UPDATE || deviceName == "DfuTarg" || deviceName.contains(
                    "frame update",
                    true
                )
            ) {
                connectDevice(result.device.address)
                stopScan()
                return
            }
            val storedDeviceAddress = sharedPreferencesHelper.getStoredDeviceAddress()
            if (currentDevice.isNotEmpty()) {
                if (currentDevice == result.device.address) {
                    connectDevice(result.device.address)
                    stopScan()
                }
                return
            } else if (storedDeviceAddress.isNotEmpty()) {
                if (storedDeviceAddress == result.device.address) {
                    connectDevice(result.device.address)
                    stopScan()
                }
                return
            }
            println(result.device.name + result.rssi)
            if (result.rssi > -65) {
                currentScannedDevice = result.device
                updateProgressDialog(
                    "Bring your device close.",
                    "${currentScannedDevice?.name?.capitalize(Locale.getDefault())}. Connect"
                )

            } else if (result.rssi < -80) {
                currentScannedDevice = null
            }
            if (currentScannedDevice == null) {
                updateProgressDialog("Bring your device close.", "Searching")
            }

            //
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
        }
    }

    /**
     * Method to connect h/w device
     */
    fun connectDevice() {
        try {
            if (currentScannedDevice != null) {
                connectDevice(currentScannedDevice!!.address)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    /**
     * Method to connect h/w device with address
     */
    @SuppressLint("MissingPermission")
    private fun connectDevice(deviceAddress: String) {

        if (currentConnectionStatus) {
            return
        }
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        // Check if the device is already connected
        if (bluetoothGatt == null || bluetoothGatt?.device != device) {
            // Close the existing BluetoothGatt instance if any
            bluetoothGatt?.close()

            // Connect to the device
            bluetoothGatt = device.connectGatt(this, false, gattCallback)



        } else {
            println("Device is already connected")
        }
    }

    /**
     * Method to connected device result
     */
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                currentDeviceName = gatt.device.name
                writingREPLProgress = false
                currentScannedDevice = null
                currentConnectionStatus = true
                if (currentDeviceName.contains(
                        "frame",
                        true
                    ) && !currentDeviceName.contains("frame update", true)
                ) {
                    if (gatt.device.bondState == BluetoothDevice.BOND_BONDED) {
                        gatt.requestMtu(GATT_MAX_MTU_SIZE)
                    } else {
//                        gatt.device.createBond()
                    }
                } else {
                    gatt.requestMtu(GATT_MAX_MTU_SIZE)
                }
                val intent = Intent("ACTION_CONNECTION_STATUS")
                intent.putExtra("EXTRA_CONNECTION_STATUS", true)
                sendBroadcast(intent)

                // Handler(Looper.getMainLooper()).post {
                if (currentAppState == AppState.FIRST_PAIR) {
                    updateProgressDialog("Checking software update.", "Keep the app open")
                }
                if (currentAppState == AppState.SOFTWARE_UPDATE || currentAppState == AppState.FPGA_UPDATE) {
                    updateProgressDialog(
                        "Updating software $overlallSoftwareProgress%",
                        "Keep the app open"
                    )
                }
                if (currentAppState == AppState.RUNNING) {
                    updateConnectionStatus("")


                }
                // }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                isPairing = false
                currentDeviceName = ""
                disconnectGatt()
                if (currentAppState == AppState.FIRST_PAIR || currentAppState == AppState.RUNNING) {

                    updateProgressDialog("Bring your device close.", "Searching")

                }

                if (currentAppState == AppState.RUNNING) {

                    updateConnectionStatus("not connected")

                }
                if (isAppInBackground(applicationContext)) {
                    val triggerIntent = Intent("ACTION_START_SCAN")
                    sendBroadcast(triggerIntent)

                } else {
                    startScan()
                }
                if (currentConnectionStatus) {
                    val intent = Intent("ACTION_CONNECTION_STATUS")
                    intent.putExtra("EXTRA_CONNECTION_STATUS", false)
                    sendBroadcast(intent)
                }

                currentConnectionStatus = false


            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                lifecycleScope.launch {
                    val service = gatt.getService(SERVICE_UUID)
                    val rawService = gatt.getService(RAW_SERVICE_UUID)
                    val nordicService = gatt.getService(NORDIC_SERVICE_UUID)
                    val frameService = gatt.getService(FRAME_SERVICE_UUID)
                    if (service != null && rawService != null) {
                        // for REPL and data of monocle
                        println("[REPL SERVICE DISCOVERED] : ${service.uuid}\n")
                        println("[DATA SERVICE DISCOVERED] :  ${rawService.uuid}\n")
                        connectMonocleServices(service, rawService, gatt)
                        startBleProcess()
                    }
                    if (nordicService != null) {
                        // for firmware update
                        println("[NORDIC SERVICE UUID DISCOVERED] : ${nordicService.uuid}\n")
                        connectNordicService(nordicService, gatt)
                        startDfuProcess()
                    }

                    if (frameService != null) {
                        println("[FRAME SERVICE UUID DISCOVERED] : ${frameService.uuid}\n")
                        connectFrameService(frameService, gatt)
                        startFrameBleProcess()
                    }
                }
            } else {
                println("[DISCOVERY FAILED]\n")
            }
        }


        @SuppressLint("MissingPermission")
        suspend fun connectMonocleServices(
            service: BluetoothGattService,
            rawService: BluetoothGattService,
            gatt: BluetoothGatt
        ) {
            return coroutineScope {
                val resultDeferred = async {

                    rxCharacteristic = service.getCharacteristic(UUID.fromString(RX_UUID))
                    txCharacteristic = service.getCharacteristic(UUID.fromString(TX_UUID))

                    if (rxCharacteristic != null && txCharacteristic != null) {
                        println("[REPL RX CHARACTERISTICS CONNECTED ] : ${rxCharacteristic!!.uuid}\n")
                        println("[REPL TX CHARACTERISTICS CONNECTED ] : ${txCharacteristic!!.uuid}\n")
                        gatt.setCharacteristicNotification(txCharacteristic, true)
                        val descriptor =
                            txCharacteristic!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                        println("[REPL TX CHARACTERISTICS NOTIFICATION ENABLED ] : ${txCharacteristic!!.uuid}\n")
                    }
                    rawRxCharacteristic = rawService.getCharacteristic(UUID.fromString(RAW_RX_UUID))
                    rawTxCharacteristic = rawService.getCharacteristic(UUID.fromString(RAW_TX_UUID))
                    delay(500)
                    if (rawRxCharacteristic != null && rawTxCharacteristic != null) {
                        println("[DATA RX CHARACTERISTICS CONNECTED ] : ${rawRxCharacteristic!!.uuid}\n")
                        println("[DATA TX CHARACTERISTICS CONNECTED ] : ${rawTxCharacteristic!!.uuid}\n")
                        gatt.setCharacteristicNotification(rawTxCharacteristic, true)
                        val descriptor =
                            rawTxCharacteristic!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                        println("[DATA TX CHARACTERISTICS NOTIFICATION ENABLED ] : ${rawTxCharacteristic!!.uuid}\n")
                    }
                    delay(500)

                }
                resultDeferred.await()
            }
        }

        @SuppressLint("MissingPermission")
        suspend fun connectNordicService(service: BluetoothGattService, gatt: BluetoothGatt) {
            return coroutineScope {
                val resultDeferred = async {
                    nordicControlCharacteristic = service.getCharacteristic(
                        UUID.fromString(
                            NORDIC_CONTROL_UUID
                        )
                    )
                    nordicPacketCharacteristic = service.getCharacteristic(
                        UUID.fromString(
                            NORDIC_PACKET_UUID
                        )
                    )
                    if (nordicControlCharacteristic != null && nordicPacketCharacteristic != null) {
                        println("[NORDIC CONTROL CHARACTERISTICS CONNECTED ] : ${nordicControlCharacteristic!!.uuid}\n")
                        println("[NORDIC PACKET CHARACTERISTICS CONNECTED ] : ${nordicPacketCharacteristic!!.uuid}\n")
                        gatt.setCharacteristicNotification(nordicControlCharacteristic, true)
                        val descriptor =
                            nordicControlCharacteristic!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                        println("[NORDIC CONTROL CHARACTERISTICS NOTIFICATION ENABLED ] : ${nordicControlCharacteristic!!.uuid}\n")
                    }
                }
                delay(200)
                resultDeferred.await()
            }
        }

        @SuppressLint("MissingPermission")
        suspend fun connectFrameService(
            service: BluetoothGattService,
            gatt: BluetoothGatt
        ) {
            return coroutineScope {
                val resultDeferred = async {

                    frameRxCharacteristic =
                        service.getCharacteristic(UUID.fromString(FRAME_RX_UUID))
                    frameTxCharacteristic =
                        service.getCharacteristic(UUID.fromString(FRAME_TX_UUID))

                    if (frameRxCharacteristic != null && frameTxCharacteristic != null) {
                        println("[REPL RX CHARACTERISTICS CONNECTED ] : ${frameRxCharacteristic!!.uuid}\n")
                        println("[REPL TX CHARACTERISTICS CONNECTED ] : ${frameTxCharacteristic!!.uuid}\n")
                        gatt.setCharacteristicNotification(frameTxCharacteristic, true)
                        val descriptor =
                            frameTxCharacteristic!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                        println("[REPL TX CHARACTERISTICS NOTIFICATION ENABLED ] : ${frameTxCharacteristic!!.uuid}\n")
                    }
                    delay(500)


                }
                resultDeferred.await()
            }
        }


        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                gatt.discoverServices()

        }


        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                writingREPLProgress = false
//                println("[WRITE] ${characteristic.value}")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                if (characteristic.uuid == rawTxCharacteristic?.uuid) {
                    handleAudioData(characteristic.value)
                    return@launch
                }
                if (characteristic.uuid == txCharacteristic?.uuid) {
                    handleReplData(characteristic.value)
                    return@launch
                }
                if (characteristic.uuid == nordicControlCharacteristic?.uuid) {
                    handleNordicControlData(characteristic.value)
                    return@launch
                }
                if (characteristic.uuid == frameTxCharacteristic?.uuid) {
                    handleFrameData(characteristic.value)
                    return@launch
                }
            }

        }

        fun handleAudioData(value: ByteArray) {
            monocleReceived(value)
        }

        private fun handleReplData(value: ByteArray) {
            val receivedData = String(value)
            if (rawReplResponseCallback != null) {
                lastResponse += receivedData
                if (lastResponse.endsWith(">")) {
                    rawReplResponseCallback!!(lastResponse)
                    lastResponse = ""
                } else if (receivedData.startsWith("OK")) {
                    lastResponse = receivedData
                } else {
                    println("$receivedData\n")
                }
            }
        }

        private fun handleNordicControlData(value: ByteArray) {
            controlResponseCallback!!(value)
        }

        private fun handleFrameData(value: ByteArray) {
            // if first byte is 0x01, then it is audio or photo data except 01, it is response
            if (value[0]==0x01.toByte()){
                frameReceived(value.sliceArray(1 until value.size))
            }else{
                val receivedData = String(value)
                if (frameResponseCallback != null) {
                    frameResponseCallback!!(receivedData)
                }
            }

        }
    }

    /**
     * Method to disconnect device
     */
    @SuppressLint("MissingPermission")
    private fun disconnectGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            rxCharacteristic = null
            txCharacteristic = null
            rawTxCharacteristic = null
            rawRxCharacteristic = null
            nordicControlCharacteristic = null
            nordicPacketCharacteristic = null
            writingREPLProgress = false
            currentScannedDevice = null
        }
    }

    /**
     * Method to receive monocle data
     */
    fun monocleReceived(data: ByteArray) {
        if (data.size < 4) {
            println("Received on data " + String(data))
            return
        }
        val status = String(data.slice(0 until 4).toByteArray())


        if (status == "ist:") {
            imageBuffer = byteArrayOf(0)
            audioBuffer = byteArrayOf(0)
            println("[NEW_Image Starting to come]\n")
        }
        if (status == "idt:") {
            println("[NEW_Image RECEIVING]\n")
            imageBuffer += data.slice(4 until data.size).toByteArray()
        }
        if (status == "ien:") {
            println("[NEW_Image RECEIVED]\n")

            // create jpeg file .... Then ---

            bitmap = BitmapFactory.decodeByteArray(imageBuffer, 1, imageBuffer.size - 1)

            if (bitmap != null) {

                bitmap = resizeBitmapToMultipleOf64(bitmap!!)
                val jpegFile = saveBitmapAsJPEG(bitmap!!)
                if (jpegFile != null) {
                    globalJpegFilePath = jpegFile.absolutePath
                }
                updateChatList(1, "S", "", bitmap!!)
            }

            val responseData = "ick:"

            dataSendBle(responseData)
        }


        if (status == "ast:") {
//            new audio buffer start, delete old one
            println("[NEW_AUDIO STARTS]\n")
            audioBuffer = byteArrayOf(0)
            audioJob?.cancel()
        }
        if (status == "dat:") {
//            audio buffer append
            audioBuffer += data.slice(4 until data.size).toByteArray()
        }
        if (status == "aen:") {
            println("[AUDIO RECEIVED]\n")
//            process audio buffer
            val responseData = "pin:" + "hello"

            dataSendBle(responseData)
            // Start new coroutine
            audioJob = CoroutineScope(Dispatchers.IO).launch {
                val path = applicationContext.filesDir
                val f2 = File(path, "test.wav")
                if (f2.exists()) {
                    f2.delete()
                }
                try {
                    rawToWave(
                        signed8ToUnsigned16(audioBuffer),
                        f2,
                        sampleRate,
                        bitPerSample,
                        channels
                    ) { success ->
                        if (success) {
                            println("[AUDIO PARSED SENDING TO CHATGPT]\n")
                            if (USE_CUSTOM_SERVER) {
                                getGPTResult(f2)
                            } else {

                                lastAudioFile = f2

                                if (translateEnabled) {
                                    translateAudio(f2)
                                } else {

                                    if (globalJpegFilePath.isNullOrEmpty()) {
                                        uploadAudioToGpt(f2)
                                    } else {
                                        callStabilityApi(f2)
                                    }
                                }

                            }

                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
//        if (status == "pon:") {
////            var responseData = "res:"+"Got response"
////
////            dataSendBle(responseData)
//        }
    }
    fun frameReceived(data: ByteArray) {
        /*
MESSAGE_START_FLAG = "\x10"
MESSAGE_AUDIO_FLAG = "\x12"
MESSAGE_IMAGE_FLAG = "\x13"
MESSAGE_END_FLAG = "\x16"
*/
//        // take first byte as state
        val state = data[0]
        // message start , set  audio, photo buffer to empty
        if (state == 0x10.toByte()) {
            audioBuffer = byteArrayOf(0)
            imageBuffer = byteArrayOf(0)
            // start of message
            println("[FRAME MESSAGE START]\n")
        }
        // audio data
        if (state == 0x12.toByte()) {
            println("[FRAME AUDIO RECEIVING]\n")
            audioBuffer += data.sliceArray(1 until data.size)
        }
        // image data
        if (state == 0x13.toByte()) {
            println("[FRAME IMAGE RECEIVING]\n")
            imageBuffer += data.sliceArray(1 until data.size)
        }
        // end of message
        if (state == 0x16.toByte()) {
            println("[FRAME MESSAGE END]\n")
            if (audioBuffer.size > 0) {
                val path = applicationContext.filesDir
                val f2 = File(path, "test.wav")
                if (f2.exists()) {
                    f2.delete()
                }
                try {
                    rawToWave(
                        signed8ToUnsigned16(audioBuffer),
                        f2,
                        sampleRate,
                        bitPerSample,
                        channels
                    ) { success ->
                        if (success) {
                            println("[AUDIO PARSED SENDING TO CHATGPT]\n")
//                            if (USE_CUSTOM_SERVER) {
//                                getGPTResult(f2)
//                            } else {
                                lastAudioFile = f2
//                                if (translateEnabled) {
//                                    translateAudio(f2)
//                                } else {
//                                    if (globalJpegFilePath.isNullOrEmpty()) {
//                                        uploadAudioToGpt(f2)
//                                    } else {


                            if (imageBuffer.size > 0) {
                                bitmap = BitmapFactory.decodeByteArray(imageBuffer, 1, imageBuffer.size - 1)
                                if (bitmap != null) {
                                    bitmap = resizeBitmapToMultipleOf64(bitmap!!)
                                    val jpegFile = saveBitmapAsJPEG(bitmap!!)
                                    if (jpegFile != null) {
                                        globalJpegFilePath = jpegFile.absolutePath
                                    }
                                    updateChatList(1, "S", "", bitmap!!)
                                }
                            }

                                        callStabilityApi(f2)
                                   // }
                               // }
                           // }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (imageBuffer.size > 0) {
                bitmap = BitmapFactory.decodeByteArray(imageBuffer, 1, imageBuffer.size - 1)
                if (bitmap != null) {
                    bitmap = resizeBitmapToMultipleOf64(bitmap!!)
                    val jpegFile = saveBitmapAsJPEG(bitmap!!)
                    if (jpegFile != null) {
                        globalJpegFilePath = jpegFile.absolutePath
                    }
                    updateChatList(1, "S", "", bitmap!!)
                }
            }
        }

    }


    /**
     * Method to update chat list with bitmap image
     */
    private fun updateChatList(id: Int, type: String, msg: String, image: Bitmap?) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is ChatGptFragment) {
            fragment.updatechatList(id, type, msg, image)
        }
    }

    /**
     * Method to update chat list with network image
     */
    private fun updatechatListWithNetworkImg(id: Int, type: String, msg: String, image: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is ChatGptFragment) {
            fragment.updatechatList(id, type, msg, image)
        }
    }

    /**
     * Method to save bitmap image to JPEG
     */
    private fun saveBitmapAsJPEG(bitmap: Bitmap, quality: Int = 100): File? {
        try {
            // Generate a unique file name using a timestamp

            val fileName = "Output.jpg"

            // Create the JPEG file
            val file = File(cacheDir, fileName)
            val fileOutputStream = FileOutputStream(file)

            // Compress the Bitmap to JPEG format and save it to the file
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream)

            // Close the FileOutputStream
            fileOutputStream.close()

            return file
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Method to update bitmap image height/width
     */
    private fun resizeBitmapToMultipleOf64(bitmap: Bitmap): Bitmap {
// Get the original width and height of the bitmap
        val oldWidth = bitmap.width
        val oldHeight = bitmap.height

// Calculate the new width and height that are multiples of 64
        val newWidth = ceil(oldWidth / 64.0).toInt() * 64
        val newHeight = ceil(oldHeight / 64.0).toInt() * 64

// Create a new bitmap with the new dimensions
        val newBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)

// Create a canvas with the new bitmap as its target
        val canvas = Canvas(newBitmap)

// Calculate the x and y coordinates of the original bitmap on the new bitmap
        val x = (newWidth - oldWidth) / 2
        val y = (newHeight - oldHeight) / 2

// Draw the original bitmap on the new bitmap
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)

// Return or use the new bitmap as needed
        return newBitmap
    }

    @Throws(IOException::class)
    private fun rawToWave(
        rawPCMBytes: ByteArray,
        waveFile: File,
        sampleRate: Int,
        bitPerSample: Int,
        channel: Int,
        callback: (Boolean) -> Unit
    ) {
        var output: DataOutputStream? = null

        @Throws(IOException::class)
        fun writeIntToWave(output: DataOutputStream, value: Int) {
            output.write(value shr 0)
            output.write(value shr 8)
            output.write(value shr 16)
            output.write(value shr 24)
        }

        @Throws(IOException::class)
        fun writeShort(output: DataOutputStream, value: Short) {
            output.write(value.toInt() shr 0)
            output.write(value.toInt() shr 8)
        }

        @Throws(IOException::class)
        fun writeString(output: DataOutputStream, value: String) {
            for (i in 0 until value.length) {
                output.write(value[i].code)
            }
        }
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
            writeIntToWave(output, sampleRate) // sample rate
            writeIntToWave(
                output,
                sampleRate * channel * bitPerSample / 8
            ) // byte rate = sample rate * channels * bytes per sample
            writeShort(
                output,
                (channel * bitPerSample / 8).toShort()
            ) // block align = channels * bytes per sample
            writeShort(output, bitPerSample.toShort()) // bits per sample
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
            audioBuffer = byteArrayOf(0)
            callback(true)
        }

    }

    private fun signed8ToUnsigned16(byteArray: ByteArray): ByteArray {
        val unsignedArray = ByteArray(byteArray.size * 2)
        for (i in byteArray.indices) {
            val unsigned = (byteArray[i].toInt() and 0xFF) shl 8
            unsignedArray[i * 2] = (unsigned ushr 8).toByte()
            unsignedArray[i * 2 + 1] = unsigned.toByte()
        }
        return unsignedArray
    }



    /**
     * Method to upload audio to chat GPT
     */
    private fun uploadAudioToGpt(
        audioFile: File,
    ) {
        val jsonPayload = apiMessagePayload.toString()
        val jsonConfigPayload = apiConfigPayload.toString()

        val audioRequestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(AUDIO, "audio.wav", audioFile.asRequestBody())
                .addFormDataPart(MESSAGES, jsonPayload)
                .addFormDataPart(PROMPT, ".")
                .addFormDataPart(CONFIG, jsonConfigPayload)
                .build()
        /**
         * REST API upload audio response callback
         */
        val uploadAudioToGptCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                updateChatList("S", e.message.toString())
                sendChatGptResponce(e.message.toString(), "err:")
                // Handle failure
                // client.close()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        sendChatGptResponce(ERROR_RESPONSE, "err:")
                        // Handle unsuccessful response
                        if (response.code == 401) {
                            unpairMonocle()
                        }
                    } else {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            // Parse the JSON response
                            val jsonResponse = JSONObject(responseBody)

                            // Extract relevant information
//                        val completionId = jsonResponse.getString("id")
//                        val modelUsed = jsonResponse.getString("model")
                            val promptUsed = jsonResponse.getString("user_prompt")
                            addMessage("user", promptUsed)

                            updateChatList("S", promptUsed)
                            // Extract the assistant's response

                            val assistantResponse = jsonResponse.getString("response")

                            addMessage("assistant", assistantResponse)
                            //  updatechatList("R",assistantResponse)

                            sendChatGptResponce(assistantResponse.toString(), "res:")


                        }
                    }
                } catch (e: JSONException) {
                    sendChatGptResponce("Something is wrong...try again!!", "err:")
                    // Handle JSON parsing error
                } finally {
                    //client.close()
                }
            }
        }

        /**
         * REST API call to upload audio file
         */
        RequestAPICallBack.apiCallBackMultiPartRequest(audioRequestBody,accessToken,
            API_CALL_UPLOAD_FILE,uploadAudioToGptCallback)
    }

    /**
     * Method to translate audio
     */
    private fun translateAudio(
        audioFile: File,
    ) {
        val client = OkHttpClient()


        val audioRequestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(AUDIO, "audio.wav", audioFile.asRequestBody())
                .build()
        /**
         * REST API translate audio response callback
         */
        val translateAudioCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                sendChatGptResponce(e.message.toString(), "err:")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        //  updatechatList("S","Something is wrong!! try again..")
                        sendChatGptResponce(ERROR_RESPONSE, "err:")
                        // Handle unsuccessful response
                    } else {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            // Parse the JSON response
                            val jsonResponse = JSONObject(responseBody)

                            // Extract relevant information
                            val reply = jsonResponse.getString("reply")


                            sendChatGptResponce(reply, "res:")
                            // updatechatList("S",reply)
                            // Extract the assistant's response

                        }
                    }
                } catch (e: JSONException) {
                    sendChatGptResponce(ERROR_RESPONSE, "err:")
                    // Handle JSON parsing error
                } finally {
                    //client.close()
                }
            }
        }

        /**
         * REST API call to upload audio file to translate
         */
        RequestAPICallBack.apiCallBackMultiPartRequest(audioRequestBody,accessToken, API_TRANSLATE,translateAudioCallback)
    }


    val apiConfigPayload = JSONObject().apply {
        put("search_api", "serp")
        put("engine", "google")
        put("location", "kolkata")
        put("count", 5)

    }
    val apiMessagePayload = JSONArray().apply {
        // Initial system message
//            put(JSONObject().apply {
//                put("role", "system")
//                put("content", "You are a smart assistant that answers all user queries, questions, and statements with a single sentence.")
//            })
    }


    // Function to add a row to the "messages" array
    fun addMessage(role: String, content: String) {
        val messageObject = JSONObject().apply {
            put("role", role)
            put("content", content)
        }
        apiMessagePayload.put(messageObject)
    }


    private fun callStabilityApi(
        audioFile: File
    ) {
        val imageFilePath = globalJpegFilePath
        val jsonPayload = apiMessagePayload.toString()
        val jsonConfigPayload = apiConfigPayload.toString()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(AUDIO, "audio.wav", audioFile.asRequestBody())
            .addFormDataPart(MESSAGES, jsonPayload)
          //  .addFormDataPart(PROMPT, ".")
            .addFormDataPart(
                IMAGE, "Output.jpg",
                File(imageFilePath.toString()).asRequestBody("image/jpg".toMediaTypeOrNull())
            )
            .addFormDataPart(CONFIG, jsonConfigPayload)
            .build()
        val callStabilityApiCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {

                if(currentDeviceName.contains("Frame",true))
                {
                    // Send error response to Frame
                }
                else {
                    sendChatGptResponce(e.message.toString(), "err:")
                }
                globalJpegFilePath = null
           }

            override fun onResponse(call: Call, response: Response) {
                try {
                    println(response.body)
                    if (response.isSuccessful) {
                        globalJpegFilePath = null
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            // Parse JSON response
                            val jsonResponse = JSONObject(responseBody)

                            val promptUsed = jsonResponse.getString("user_prompt")
                            addMessage("user", promptUsed)

                            updateChatList("S", promptUsed)
                            // Extract the assistant's response

                            val assistantResponse = jsonResponse.getString("response")

                            addMessage("assistant", assistantResponse)
                            //  updatechatList("R",assistantResponse)



                            sendChatGptResponce(assistantResponse.toString(), "res:")

                        }


                    } else {
                        val responseData = response.body?.string()

                        try {
                            val jsonObject = responseData?.let { JSONObject(it) }
                            if (jsonObject != null) {
                                if (jsonObject.has("message")) {
                                    println(jsonObject.get("message"))
                                    sendChatGptResponce(jsonObject.get("message").toString(), "err:")
                                    globalJpegFilePath = null
                                }
                            }
                        } catch (e: java.lang.Exception) {
                            println(responseData)
                            println(e.printStackTrace())
                            sendChatGptResponce(e.message.toString(), "err:")
                            globalJpegFilePath = null
                        }

                        // Handle the API error here
                        // Log error or show an error message to the user
                    }

                } catch (e: JSONException) {
                    sendChatGptResponce(ERROR_RESPONSE, "err:")

                    globalJpegFilePath = null
                    // Handle JSON parsing error
                } finally {
                    //client.close()
                }
            }
        }
        RequestAPICallBack.apiCallBackMultiPartRequest(requestBody,accessToken, API_CALL_UPLOAD_FILE,callStabilityApiCallback)
    }

    /**
     * Method to delete account
     */
    fun deleteAccount() {
        val mediaType = "text/plain".toMediaType()
        val body = "".toRequestBody(mediaType)
        val deleteAccountCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                // e.message.toString()
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    // Handle Response
                        sharedPreferencesHelper.clearSharedPreferences()
                        ActivityUtil.navigateToActivity(this@BaseActivity, SocialLoginActivity::class.java)
                   } catch (e: Exception) {
                    // Handle exceptions
                    sharedPreferencesHelper.clearSharedPreferences()
                    ActivityUtil.navigateToActivity(this@BaseActivity, SocialLoginActivity::class.java)

                }
            }
        }
        RequestAPICallBack.apiCallBackRequest(body, accessToken,API_DELETE_ACCOUNT,deleteAccountCallback)

    }


    /**
     * Method to send data to chat GPT
     */
     fun sendChatGptResponce(data: String, prefix: String) {


        updateChatList("R", data)

        if(currentDeviceName.contains("Monocle",true)) {
            val dataSend = prefix + data //err:
            dataSendBle(dataSend)
        }
        else
        {
            lifecycleScope.launch {
                frameSendBle(byteArrayOf(0x11) + data.toByteArray())
            }
        }
    }




    // MONOCLE COMMUNICATION
    /**
     * Method to write raw bytearray to monocle
     */
    @SuppressLint("MissingPermission")
    private fun rawBleWrite(data: ByteArray) {
        val characteristic = rawRxCharacteristic
        if (bluetoothGatt != null && characteristic != null) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            characteristic.value = data
            if (bluetoothGatt != null) {

                bluetoothGatt!!.writeCharacteristic(characteristic)
            }
        }
    }

    /**
     * Method to send data to monocle
     */
    private fun dataSendBle(data: String) {
        thread {
            val chunkSize = 90
            var offset = 0
            val actualData = data.substring(4)
            val command = data.substring(0, 4)
            println(actualData)
            writingREPLProgress = false
            while (offset < actualData.length) {
                if (writingREPLProgress) {
                    continue
                }
                val length = kotlin.math.min(chunkSize, actualData.length - offset)
                val chunk = command + actualData.substring(offset, offset + length)
                writingREPLProgress = true
                rawBleWrite(chunk.toByteArray())
                offset += length
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun replWrite(data: ByteArray, resultDeferred: CompletableDeferred<String>) {

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
                if (bluetoothGatt != null) {

                    bluetoothGatt!!.writeCharacteristic(characteristic)
                } else {
                    resultDeferred.complete("Done")
                    break

                }
                offset += length
            }

        }
        resultDeferred.complete("Done")
    }

    @SuppressLint("MissingPermission")
    private fun frameWrite(data: ByteArray, resultDeferred: CompletableDeferred<String>) {

        val characteristic = frameRxCharacteristic
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
                if (bluetoothGatt != null) {

                    bluetoothGatt!!.writeCharacteristic(characteristic)
                } else {
                    resultDeferred.complete("Done")
                    break

                }
                offset += length
            }

        }
        resultDeferred.complete("Done")
    }

    private suspend fun replSendBle(data: String): String {
        return coroutineScope {


            val resultDeferred = CompletableDeferred<String>()
            val handler = Handler(Looper.getMainLooper())
            thread {
                replWrite(data.toByteArray() + byteArrayOf(0x04), resultDeferred)
            }

            // Set up the response handler callback
            val bleWriteComplete = CompletableDeferred<String>()
            rawReplResponseCallback = { responseString ->
                println("[RECEIVED]: $responseString\n")
                if (bleWriteComplete.isActive) {
                    bleWriteComplete.complete(responseString)
                }
//
            }

            // Resolve if the response handler callback isn't called
            launch {
                handler.postDelayed({
                    if (!resultDeferred.isCompleted) {
                        resultDeferred.complete("")
                    }
                    if (!bleWriteComplete.isCompleted) {
                        bleWriteComplete.complete("")
                    }
                }, 3000)
            }
            resultDeferred.await()
            bleWriteComplete.await()
        }
    }

    private suspend fun frameSendBle(data: String): String {
        return coroutineScope {


            val resultDeferred = CompletableDeferred<String>()
            val handler = Handler(Looper.getMainLooper())
            thread {
                frameWrite(data.toByteArray(), resultDeferred)
            }

            // Set up the response handler callback
            val bleWriteComplete = CompletableDeferred<String>()
            frameResponseCallback = { responseString ->
                println("[RECEIVED]: $responseString\n")
                if (bleWriteComplete.isActive) {
                    bleWriteComplete.complete(responseString)
                }
//
            }

            // Resolve if the response handler callback isn't called
            launch {
                handler.postDelayed({
                    if (!resultDeferred.isCompleted) {
                        resultDeferred.complete("")
                    }
                    if (!bleWriteComplete.isCompleted) {
                        bleWriteComplete.complete("")
                    }
                }, 1000)
            }
            resultDeferred.await()
            bleWriteComplete.await()
        }
    }

    private suspend fun replSendBle(data: ByteArray): String {
        return coroutineScope {


            val resultDeferred = CompletableDeferred<String>()
            val handler = Handler(Looper.getMainLooper())
            thread {
                replWrite(data, resultDeferred)
            }

            // Set up the response handler callback
            val bleWriteComplete = CompletableDeferred<String>()
            rawReplResponseCallback = { responseString ->
                println("[RECEIVED]: $responseString\n")
                if (bleWriteComplete.isActive) {
                    bleWriteComplete.complete(responseString)
                }
//
            }

            // Resolve if the response handler callback isn't called
            launch {
                handler.postDelayed({
                    if (!resultDeferred.isCompleted) {
                        resultDeferred.complete("")
                    }
                    if (!bleWriteComplete.isCompleted) {
                        bleWriteComplete.complete("")
                    }
                }, 3000)
            }
            resultDeferred.await()
            bleWriteComplete.await()
        }
    }

    private suspend fun frameSendBle(data: ByteArray): String {
        return coroutineScope {
            val resultDeferred = CompletableDeferred<String>()
            val handler = Handler(Looper.getMainLooper())
            thread {
                frameWrite(data, resultDeferred)
            }

            // Set up the response handler callback
            val bleWriteComplete = CompletableDeferred<String>()
            frameResponseCallback = { responseString ->
                println("[RECEIVED]: $responseString\n")
                if (bleWriteComplete.isActive) {
                    bleWriteComplete.complete(responseString)
                }
//
            }

            // Resolve if the response handler callback isn't called
            launch {
                handler.postDelayed({
                    if (!resultDeferred.isCompleted) {
                        resultDeferred.complete("")
                    }
                    if (!bleWriteComplete.isCompleted) {
                        bleWriteComplete.complete("")
                    }
                }, 3000)
            }
            resultDeferred.await()
            bleWriteComplete.await()
        }
    }


    // MAIN FLOW AFTER CONNECTION TO MONOCLE
    /**
     * Method to first send data after connect to monocle device
     */
    suspend fun startBleProcess() {
        replSendBle(byteArrayOf(0x3, 0x3, 0x1))
        //   firmware check and update
        if (currentAppState == AppState.FIRST_PAIR || currentAppState == AppState.FPGA_UPDATE) {
            if (currentAppState != AppState.FPGA_UPDATE && firmwareCheckUpdate() != "Updated") {
                currentAppState = AppState.SOFTWARE_UPDATE
                println("[STARTED FIRMWARE UPDATE]\n")
                return
            }
            if (currentAppState != AppState.FPGA_UPDATE) {
                updateProgressDialog("Checking software update..", "Keep the app open")
            }

            if (!NRFKIT && fpgaCheckUpdate() != "Updated") {
                return
            }

            currentDevice = ""
            currentAppState = AppState.SCRIPT_UPDATE
            startBluetoothBackground()
//            updateProgressDialog("Checking Sofware Update...", "Keep the app open")
            println("[FIRMWARE STABLE]\n")

        }
        //    file upload

        if (NRFKIT) {
            currentAppState =
                if (currentAppState == AppState.SOFTWARE_UPDATE || currentAppState == AppState.SCRIPT_UPDATE) AppState.SCRIPT_UPDATE else AppState.RUNNING

        } else {
            startFileUpload()
        }
        replSendBle(byteArrayOf(0x3, 0x4))
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment !is ChatGptFragment) {
            val fragment = ChatGptFragment()
            ActivityUtil.navigateToFragment(fragmentManager, fragment, false, "chat_gpt")
            val apikeyStored = sharedPreferencesHelper.getStoredApiKey()
            if (apikeyStored.isNotEmpty()) {
                apiKey = apikeyStored
            }
            updateConnectionStatus("")
            println("[CHAT READY]\n")
            currentAppState = AppState.RUNNING

            handler.postDelayed({
                showIntroMessages()
            }, 1000)


            if (bluetoothGatt != null) {
                sharedPreferencesHelper.storeDeviceAddress(bluetoothGatt!!.device.address)
            }
        }
        currentAppState = AppState.RUNNING
    }

    //MAIN FLOW AFTER CONNECTION TO FRAME
    /**
     * Method to first send data after connect to frame device
     */
    suspend fun startFrameBleProcess() {
        frameSendBle(byteArrayOf(0x03))
        frameSendBle("frame.imu.tap_callback(nil);print(nil)")
//        //   firmware check and update
        if (currentAppState == AppState.FIRST_PAIR) {
            if (frameFirmwareCheckUpdate() != "Updated") {
                currentAppState = AppState.SOFTWARE_UPDATE
                println("[STARTED FIRMWARE UPDATE]\n")
                return
            }

            currentDevice = ""
            currentAppState = AppState.SCRIPT_UPDATE
            startBluetoothBackground()
//            updateProgressDialog("Checking Sofware Update...", "Keep the app open")
            println("[FIRMWARE STABLE]\n")

        }
//        //    file upload
//
        if (NRFKIT) {
            currentAppState =
                if (currentAppState == AppState.SOFTWARE_UPDATE || currentAppState == AppState.SCRIPT_UPDATE) AppState.SCRIPT_UPDATE else AppState.RUNNING

        }
//        if (currentAppState == AppState.SCRIPT_UPDATE) {
        // only on first pair
        if (isPairing) {
            startFileUpload()
        }
        frameSendBle(byteArrayOf(0x4))
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment !is ChatGptFragment) {
            val fragment1 = ChatGptFragment()
            ActivityUtil.navigateToFragment(fragmentManager, fragment1, false, "chat_gpt")
            val apikeyStored = sharedPreferencesHelper.getStoredApiKey()
            if (apikeyStored.isNotEmpty()) {
                apiKey = apikeyStored
            }
            updateConnectionStatus("")
            println("[CHAT READY]\n")
            currentAppState = AppState.RUNNING

            handler.postDelayed({
                showIntroMessages()
            }, 1000)


            if (bluetoothGatt != null) {
                sharedPreferencesHelper.storeDeviceAddress(bluetoothGatt!!.device.address)
            }
        }
        currentAppState = AppState.RUNNING
    }

    fun getThumbnailUrl(url: String): String {
        try {
            val document: Document = Jsoup.connect(url).get()

            // Try to extract Open Graph Protocol image
            val ogImageUrl = document.select("meta[property=og:image]").attr("content")
            if (ogImageUrl.isNotEmpty()) {
                return ogImageUrl
            }

            // Try to extract Twitter Card image
            val twitterImageUrl = document.select("meta[name=twitter:image]").attr("content")
            if (twitterImageUrl.isNotEmpty()) {
                return twitterImageUrl
            }

            // Try to extract other common meta tags for images
            val commonImageUrls = document.select("meta[name=image], meta[itemprop=image]")
            for (element in commonImageUrls) {
                val imageUrl = element.attr("content")
                if (imageUrl.isNotEmpty()) {
                    return imageUrl
                }
            }

            // If no suitable metadata found, you might want to fallback to a default image
            // return a default URL here

        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }

        // Return null if no thumbnail URL found or an error occurred
        return ""
    }

    /**
     * Method to show intro messages to chat screen
     */
    fun showIntroMessages() {
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        Log.d(
            "Drawable Image====>>>>>",
            BitmapFactory.decodeResource(
                this@BaseActivity.resources,
                R.drawable.openai_website
            ).toString()
        )

        val messagelist = listOf(
            ChatModel(1, "R", "Hi, I’m Noa. Let’s show you around 🙂", false, ""),

            ChatModel(
                1,
                "R",
                "Tap either of the touch pads and speak.\n\n" +
                        "Ask me any question, and I’ll then respond directly on your Monocle.",
                false,
                getThumbnailUrl("https://platform.openai.com"),
                BitmapFactory.decodeResource(
                    this@BaseActivity.resources,
                    R.drawable.tutorial_image_one
                )

            ),
            ChatModel(
                1,
                "R",
                "I can also translate whatever I hear into English.\n\nToggle the translator mode from the menu like so.",
                false,
                getThumbnailUrl("https://platform.openai.com"),
                BitmapFactory.decodeResource(
                    this@BaseActivity.resources,
                    R.drawable.tutorial_image_single_tap_monocle
                )

            ),
            ChatModel(
                1,
                "R",
                "Did you know that I'm a fantastic artist?\nHold any touch pad, and Monocle will take a picture before listening.\n\nAsk me how to change the image and I'll return back a new image right here in the chat.",
                false,
                getThumbnailUrl("https://platform.openai.com"),
                BitmapFactory.decodeResource(
                    this@BaseActivity.resources,
                    R.drawable.tutorial_image_with_cam
                )

            ),

            ChatModel(
                1, "R", "Looks like you’re all set!\n" +
                        "\n" +
                        "Go ahead. Ask me anything you’d like ☺️", false, ""
            ),

            )


        // Call updateChatList three times with different messages
        coroutineScope.launch {
            for (message in messagelist) {
                if (message.image.isNotEmpty())
                    updatechatListWithNetworkImg(
                        message.id,
                        message.userInfo,
                        message.message,
                        message.image
                    )
                else
                    updateChatList(message.id, message.userInfo, message.message, message.bitmap)
                delay(1000) // Delay for 1 second between calls
            }
        }
    }

    /**
     * Method to read monocle script from assets folder
     */
    private fun readScriptFileFromAssets(fileName: String): String {
        val assetManager: AssetManager = applicationContext.assets
        return assetManager.open("Scripts/$fileName").bufferedReader().use {
            it.readText()
        }
    }

    /**
     * Method to read frame script from assets folder
     */
    private fun readFrameScriptFileFromAssets(fileName: String): String {
        val assetManager: AssetManager = applicationContext.assets
        return assetManager.open("Frame Scripts/$fileName").bufferedReader().use {
            it.readText()
        }
    }

    /**
     * Method to start file upload to devices
     */
    private suspend fun startFileUpload(): String {
        var fileNames = FILES
        if (currentDeviceName.equals("frame", true)) {
            fileNames = FRAME_FILES
        }
        val files: MutableList<Pair<String, String>> = mutableListOf()
        for (filename in fileNames) {
            if (currentDeviceName.equals("frame", true)) {
                val contents = readFrameScriptFileFromAssets(filename)
                files.add(filename to contents)
            } else {
                val contents = readScriptFileFromAssets(filename)
                files.add(filename to contents)
            }

        }
        val version = generateProgramVersionString(files)
        //        first version check if not matched then upload
        println("[VERSION]: $version\n")
        if (!currentDeviceName.equals("frame", true)) {
            val response = replSendBle("'NOA_VERSION' in globals() and print(NOA_VERSION)")
            if (!response.contains(version)) {
                println("[FILE  UPLOADING]\n")
                return fileUpload(files, version)  //  TO WORK WITH nrf52DK comment this line
            }
        } else {
            frameFileUpload(files)
            frameSendBle(byteArrayOf(0x3))
        }


        println("[FILE ALREADY UPLOADED]\n")
        currentAppState = AppState.SCRIPT_UPDATE


        return "Done"


    }

    // file upload for frame
    /**
     * Method to upload files to frame device
     */
    @SuppressLint("MissingPermission")
    private suspend fun frameFileUpload(
        files: MutableList<Pair<String, String>>
    ): String {

        val finalResults = mutableListOf<Int>()
        coroutineScope {
            for (file in files) {
                val deferItem = async {
                    // need send in chunks
                    val devicePath = file.first
                    val fileData = file.second
                    val chunkSize = 70
                    var response = frameSendBle("f = frame.file.open('$devicePath', 'w');print(f);")
                    if (response.contains("Error") || response.contains("Trace")) {
                        println("[FRAME FILE  UPLOAD FAILED] ${file.first}\n")
                        finalResults.add(0)
                    } else {
                        for (i in fileData.indices step chunkSize) {
                            val chunk = fileData.substring(i, minOf(i + chunkSize, fileData.length))
                            response = frameSendBle("f:write([[${chunk}]]);print(1);")
                            if (response.contains("Error") || response.contains("Trace")) {
                                println("[FRAME FILE  UPLOAD FAILED] ${file.first}\n")
                                finalResults.add(0)
                                break
                            }
                        }
                        response = frameSendBle("print(f:close());")
                        if (response.contains("Error") || response.contains("Trace")) {
                            println("[FRAME FILE  UPLOAD FAILED] ${file.first}\n")
                            finalResults.add(0)
                        } else {
                            println("[FRAME FILE  UPLOADED] ${file.first}\n")
                            finalResults.add(1)
                        }
                    }
                    println("[FRAME FILE  UPLOADING] ${file.first}\n")
                }
                deferItem.await()

            }
        }
//        println(finalResults)
        if (finalResults.contains(0)) {
            println("[FRAME FILE  UPLOADING FAILED]\n")
            return "Failed"
        }
        println("[FRAME FILE  UPLOADING DONE]\n")
        currentAppState = AppState.SCRIPT_UPDATE
        return "Done"

    }

    /**
     * Method to upload files to monocle device
     */
    @SuppressLint("MissingPermission")
    private suspend fun fileUpload(
        files: MutableList<Pair<String, String>>,
        version: String
    ): String {
        val finalResults = mutableListOf<Int>()
        coroutineScope {
            for (file in files) {
                val deferItem = async {
                    var dataSend =
                        "f=open('${file.first}','w');f.write('''${file.second}''');f.close();"
                    if (file.first == "main.py") {
                        dataSend =
                            "f=open('${file.first}','w');f.write('''NOA_VERSION='$version'\n${file.second}''');f.close();"
                    }
                    println("[FILE  UPLOADING] ${file.first}\n")
                    val response = replSendBle(dataSend)
                    if (response.contains("OK") && !response.contains("Error") && !response.contains(
                            "Trace"
                        )
                    ) {
                        println("[FILE  UPLOADED] ${file.first}\n")
                        finalResults.add(1)
                        finalResults.add(1)
                    } else {
                        println("[FILE  UPLOAD FAILED] ${file.first}\n")
                        finalResults.add(0)
                    }
                }
                deferItem.await()

            }
        }
//        println(finalResults)
        if (finalResults.contains(0)) {
            println("[FILE  UPLOADING FAILED]\n")
            return "Failed"
        }
        println("[FILE  UPLOADING DONE]\n")
        currentAppState = AppState.SCRIPT_UPDATE
        return "Done"

    }

    private fun generateProgramVersionString(files: List<Pair<String, String>>): String {
        val concatenatedScripts = files.joinToString("") { (filename, contents) ->
            "$filename$contents"
        }

        val data = concatenatedScripts.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        val versionString = StringBuilder()

        for (byte in digest) {
            versionString.append(String.format("%02x", byte))
        }

        return versionString.toString()
    }

    /**
     * Method to check firmware of monocle device
     */
    private suspend fun firmwareCheckUpdate(): String {

        val response = replSendBle("import device;print(device.VERSION)")
//        check version if not matched update
        val firmwareData = readFirmwareFromAssets()
        if (firmwareData.binBytes != null && firmwareData.datBytes != null) {
            return if (response.contains("Error") || !response.contains(firmwareData.version.toString()) || FIRMWARE_TEST) {
                //            start firmware update
                currentDevice = bluetoothGatt!!.device.address
                replSendBle("import update;update.micropython();")
                "Failed"
            } else {
                "Updated"
            }

        }

        return "Updated"
    }

    /**
     * Method to check firmware of frame device
     */
    @SuppressLint("SuspiciousIndentation")
    private suspend fun frameFirmwareCheckUpdate(): String {

        val response = frameSendBle("print(frame.FIRMWARE_VERSION);")
        println("FIRMWARE VERSION---$response")
//        check version if not matched update
        val firmwareData = readFirmwareFromAssets()
        if (firmwareData.binBytes != null && firmwareData.datBytes != null) {
            return if (response.contains("Error") || !response.contains(firmwareData.version.toString()) || FIRMWARE_TEST) {
                //            start firmware update
                currentDevice = bluetoothGatt!!.device.address
                frameSendBle("frame.update()")
                "Failed "
            } else {
                "Updated"
            }

        }

        return "Updated"
    }

    /**
     * Method to read FPGA files from assets folder
     */
    private suspend fun fpgaCheckUpdate(): String {
        val fpga = readFPGAFromAssets()
        val zipData = readFirmwareFromAssets()
        val response = replSendBle("import fpga;print(fpga.read(2,12));del(fpga)")
        if (fpga.bin != null && fpga.version != null) {
            if (response.contains("Error") || !response.contains(fpga.version.toString()) || FPGA_TEST) {
                currentAppState = AppState.FPGA_UPDATE
                val asciiFile = Base64.encodeToString(fpga.bin, Base64.NO_WRAP)
                var dfuSize = 0
                if (zipData.binBytes != null && zipData.datBytes != null) {
                    dfuSize = zipData.binBytes.size + zipData.datBytes.size
                    overlallSoftwareSize = dfuSize + asciiFile.length
                }
                // perform fpga update
                println("[UPDATING TO VERSION] : ${fpga.version} [FILE SIZE] : ${asciiFile.length}")
                return updateFPGA(asciiFile!!, dfuSize)
            }
        }
        return "Updated"
    }

    /**
     * Method to read FPGA files from assets folder
     */
    private fun readFPGAFromAssets(): Fpga {
        val assetManager: AssetManager = applicationContext.assets
        val packageZips = assetManager.list("FPGA/")
        if (!packageZips.isNullOrEmpty()) {
            val fileName: String = packageZips.first()
            val binFile = assetManager.open("FPGA/${fileName}")
            val pattern = Regex("monocle-fpga-v(\\d+\\.\\d+\\.\\d+)\\.bin")
            val matchResult = pattern.find(fileName)
            val version: String? = matchResult?.groupValues?.get(1)
            val byteData = binFile.readBytes()
            binFile.close()
            return Fpga(byteData, version)
        }
        return Fpga(null, null)
    }

    /**
     * Method to update FPGA files
     */
    suspend fun updateFPGA(asciiFile: String, dfuSize: Int): String {
        println("[Starting FPGA update]")
        replSendBle("import ubinascii, update, device, bluetooth, fpga")

        val response = replSendBle("print(bluetooth.max_length())")
        val maxMtu = response.replace("\\D+".toRegex(), "").toInt()

        val chunkSize = ((maxMtu - 45) / 3 / 4 * 4 * 3)
        val chunks = ceil(asciiFile.length.toDouble() / chunkSize).toInt()
        println("[Chunk] [size] = $chunkSize. [Total chunks] = $chunks")

        replSendBle("fpga.run(False)")
        replSendBle("update.Fpga.erase()")
        var chk = 0
        while (chk < chunks) {
            // last chunk can be small
            var thisChunk = chunkSize
            if (chk == chunks - 1 && asciiFile.length % chunkSize != 0) {
                thisChunk = asciiFile.length % chunkSize
            }
            val chunk = asciiFile.slice(chk * chunkSize until (chk * chunkSize) + thisChunk)

            val response = replSendBle("update.Fpga.write(ubinascii.a2b_base64(b'$chunk'))")

            if (response.contains("Error")) {
                println("Retrying this chunk")
                continue
            }
            if (response == "") {
                // return "Failed"
                break

            }

            chk++
            val perc = (100 / asciiFile.length.toDouble()) * chk * chunkSize
            println("[ PERCENT DONE ]: $perc")
            firmwareUpdateProgress(perc, dfuSize, chk * chunkSize)
        }

        replSendBle("update.Fpga.write(b'done')")
        replSendBle("device.reset()")

        println("[Completed FPGA update. Resetting]")
        return "Updated"
    }

    // NORDIC DFU

    // DFU COMMUNICATION
    /**
     * Method to nordic control write
     */
    @SuppressLint("MissingPermission")
    private fun nordicControlWrite(data: ByteArray, resultDeferred: CompletableDeferred<String>) {

        val characteristic = nordicControlCharacteristic
        if (bluetoothGatt != null && characteristic != null) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
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
                if (bluetoothGatt != null) {

                    bluetoothGatt!!.writeCharacteristic(characteristic)
                } else {

                    resultDeferred.complete("Done")
                    break
                }
                offset += length
            }

        }
        resultDeferred.complete("Done")
    }

    /**
     * Method to nordic packet write
     */
    @SuppressLint("MissingPermission")
    private fun nordicPacketWrite(data: ByteArray, resultDeferred: CompletableDeferred<String>) {

        val characteristic = nordicPacketCharacteristic
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
                if (bluetoothGatt != null) {

                    bluetoothGatt!!.writeCharacteristic(characteristic)
                } else {
                    resultDeferred.complete("Done")
                    break

                }
                offset += length
            }

        }
        resultDeferred.complete("Done")
    }

    /**
     * Method to send nordic control
     */
    private suspend fun nordicControlSend(data: ByteArray): ByteArray {
        return coroutineScope {


            val resultDeferred = CompletableDeferred<String>()
            val handler = Handler(Looper.getMainLooper())
            thread {
                nordicControlWrite(data, resultDeferred)
                val formattedString = data.joinToString(", ", "[", "]") { it.toString() }
                println("[NORDIC CONTROL SENT]: $formattedString\n")
            }

            // Set up the response handler callback
            val bleWriteComplete = CompletableDeferred<ByteArray>()
            controlResponseCallback = { controlChar ->
                val formattedString = controlChar.joinToString(", ", "[", "]") { it.toString() }
                println("[NORDIC CONTROL RECEIVED]: $formattedString\n")
                if (bleWriteComplete.isActive) {
                    bleWriteComplete.complete(controlChar)
                }
//
            }

            // Resolve if the response handler callback isn't called
            launch {
                handler.postDelayed({
                    if (!resultDeferred.isCompleted) {
                        resultDeferred.complete("")
                    }
                    if (!bleWriteComplete.isCompleted) {
                        bleWriteComplete.complete(byteArrayOf())
                    }
                }, 3000)
            }
            resultDeferred.await()
            bleWriteComplete.await()
        }
    }

    /**
     * Method to send nordic packet
     */
    private suspend fun nordicPacketSend(data: ByteArray): String {
        return coroutineScope {
            val resultDeferred = CompletableDeferred<String>()
            val handler = Handler(Looper.getMainLooper())
            thread {
                nordicPacketWrite(data, resultDeferred)
            }
            // Resolve if the response handler callback isn't called
            launch {
                handler.postDelayed({
                    if (!resultDeferred.isCompleted) {
                        resultDeferred.complete("")
                    }
                }, 3000)
            }
            resultDeferred.await()
        }
    }

    // MAIN FLOW AFTER CONNECTION TO DFU
    /**
     * Method to start DFU process after connection
     */
    @SuppressLint("MissingPermission")
    suspend fun startDfuProcess() {
        val zipData = readFirmwareFromAssets()
        val fpgaData = readFPGAFromAssets()

        if (zipData.datBytes != null && zipData.binBytes != null) {
            if (fpgaData.bin != null) {
                val asciiFile = Base64.encodeToString(fpgaData.bin, Base64.NO_WRAP)
                overlallSoftwareSize = if (currentDeviceName.contains("frame", true)) {
                    zipData.binBytes.size + zipData.datBytes.size
                } else {
                    zipData.binBytes.size + asciiFile.length + zipData.datBytes.size
                }
            }
            transferFile(zipData.datBytes, "init")
            transferFile(zipData.binBytes, "image")
            println("[NORDIC FIRMWARE UPDATE COMPLETE]")

            bluetoothGatt?.disconnect()
            currentAppState = if (currentDeviceName.contains("frame", true)) {
                AppState.SCRIPT_UPDATE
            } else {
                AppState.FPGA_UPDATE
            }

        }
    }

    /**
     * Method to update firmware progress
     */
    private fun firmwareUpdateProgress(perc: Double, fileSize: Int = 0, offset: Int) {
        val chunkComplete = offset + fileSize
        overlallSoftwareProgress = ((100.0 / overlallSoftwareSize) * chunkComplete).toInt()
        updateProgressDialog("Updating software ${overlallSoftwareProgress}%", "Keep the app open")
    }

    /**
     * Method to read firmware from assets
     */
    private fun readFirmwareFromAssets(): ExtractedData {
        val assetManager: AssetManager = applicationContext.assets

        var dir = "Firmware/"
        var pattern: Regex = Regex("monocle-micropython-v(\\d+\\.\\d+\\.\\d+)\\.zip")
        if (currentDeviceName != "" && currentDeviceName.contains("frame", true)) {
            dir = "Frame Firmware/"
            pattern = Regex("frame-firmware-v(\\d+\\.\\d+\\.\\d+)\\.zip")
        }
        val packageZips = assetManager.list(dir)
        if (!packageZips.isNullOrEmpty()) {
            val zipFile = assetManager.open("${dir}${packageZips.first()}")
            val zipInputStream = ZipInputStream(zipFile)
            var entry: ZipEntry?
            var datBytes: ByteArray? = null
            var binBytes: ByteArray? = null
            val fileName: String = packageZips.first()

            val matchResult = pattern.find(fileName)
            val version: String? = matchResult?.groupValues?.get(1)
            while (zipInputStream.nextEntry.also { entry = it } != null) {

                when (entry!!.name) {
                    "manifest.json" -> {
                        //   val manifestBytes = zipInputStream.readBytes()
                        // val manifest = String(manifestBytes, Charsets.UTF_8)
                        // Process the manifest JSON string
                    }

                    "application.dat" -> {
                        datBytes = zipInputStream.readBytes()
                        // Process the datBytes array
                    }

                    "application.bin" -> {
                        binBytes = zipInputStream.readBytes()
                        // Process the binBytes array
                    }

                    else -> {
                        // Handle other files if needed
                    }
                }

                zipInputStream.closeEntry()
            }

            zipInputStream.close()
            return ExtractedData(datBytes, binBytes, version)
        }
        return ExtractedData(null, null, null)
    }

    /**
     * Method to read firmware from assets
     */
    suspend fun transferFile(data: ByteArray, fileType: String) {
        var response: ByteArray
        // Select command
        response = when (fileType) {
            "init" -> {
                println("[Transferring init file]")
                nordicControlSend(byteArrayOf(0x06, 0x01))
            }

            "image" -> {
                println("[Transferring image file]")
                nordicControlSend(byteArrayOf(0x06, 0x02))
            }

            else -> return // Invalid file type
        }
        if (response.isEmpty()) {
            println("[TRANSFER FAILED]:$fileType")
            return
        }
        val fileSize = data.size

        println("fileSize: $fileSize")
        val responseBuffer: ByteBuffer = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN)
        val maxSize = responseBuffer.getInt(3)
        val offset = responseBuffer.getInt(7)
        val crc = responseBuffer.getInt(11)

        println("maxSize: $maxSize, offset: $offset, crc: $crc")

        val chunks = ceil(fileSize.toDouble() / maxSize).toInt()
        println("Sending file as $chunks chunks")

        var fileOffset = 0
        var chk = 0
        while (chk < chunks) {

            var chunkSize = Math.min(fileSize - fileOffset, maxSize)

            // The last chunk could be smaller
            if (chk == chunks - 1 && fileSize % maxSize != 0) {
                chunkSize = fileSize % maxSize
            }

            val chunkCrc = crc32(data.sliceArray(0 until (fileOffset + chunkSize))).toInt()
            println("[chunk] $chk, [fileOffset]: $fileOffset, [chunkSize]: $chunkSize, [chunkCrc]: $chunkCrc")

            // Create command with size
            val chunkSizeAsBytes = listOf(
                (chunkSize and 0xFF).toByte(),
                ((chunkSize shr 8) and 0xFF).toByte(),
                ((chunkSize shr 16) and 0xFF).toByte(),
                ((chunkSize shr 24) and 0xFF).toByte()
            )

            when (fileType) {
                "init" -> nordicControlSend(byteArrayOf(0x01, 0x01) + chunkSizeAsBytes)
                "image" -> nordicControlSend(byteArrayOf(0x01, 0x02) + chunkSizeAsBytes)
            }

            val currentOffset = fileOffset

            val fileSlice = data.sliceArray(fileOffset until (fileOffset + chunkSize))
            fileOffset += fileSlice.size
            nordicPacketSend(fileSlice)
            response = nordicControlSend(byteArrayOf(0x03))
            if (response.isEmpty()) {
                println("[TRANSFER FAILED]:$fileType")
                return
            }
            val responseBuffer = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN)
            val returnedOffset = responseBuffer.getInt(3)
            val returnedCrc = responseBuffer.getInt(7)

            println("returnedOffset: $returnedOffset, returnedCrc: $returnedCrc")

            if (returnedCrc != chunkCrc) {
                fileOffset = currentOffset
            } else {
                val perc = (100.0 / fileSize) * fileOffset
                println("[ PERCENT DONE ]: $perc")
                firmwareUpdateProgress(perc, 0, fileOffset)
                chk++
                nordicControlSend(byteArrayOf(0x04))
            }
            // Execute command
        }
    }

    private fun crc32(data: ByteArray): Long {
        val crc32 = CRC32()
        crc32.update(data)
        return crc32.value
    }
    // for server api
    /**
     * Method to get result from chat GPT
     */
    private fun getGPTResult(file: File) {
        val client = OkHttpClient()
        val mediaType = "application/octet-stream".toMediaType()
        println("[SERVER GPT: start]")
        val body = MultipartBody.Builder().setType((MultipartBody.FORM))
            .addFormDataPart("audio", file.absolutePath, file.asRequestBody(mediaType))
            .addFormDataPart("apiKey", apiKey)
            .build()
        val req = Request.Builder().url(BACKEND_URL).post(body).build()
        val response = client.newCall(req).execute()
        println("[SERVER GPT: complete]")
        if (response.isSuccessful && response.body != null) {

            val jsonResponse: String = response.body!!.string()
            val jsonObject = JSONObject(jsonResponse)
            if (jsonObject.has("message")) {
                sendChatGptResponce(jsonObject.get("message").toString(), "err:")
            }
            if (jsonObject.has("transcript")) {
                updateChatList("S", jsonObject.get("transcript").toString())
            }
            if (jsonObject.has("reply")) {
                sendChatGptResponce(jsonObject.get("reply").toString(), "res:")
            }
        } else {
            val jsonResponse: String = response.body!!.string()
            val jsonObject = JSONObject(jsonResponse)
            if (jsonObject.has("message")) {
                var msg = jsonObject.get("message")
                try {
                    // Code that might throw an exception
                    msg = JSONObject(jsonObject.get("message").toString()).get("message")

                } catch (e: Exception) {
                    // Code to handle the exception
                } finally {
                    sendChatGptResponce(msg.toString(), "err:")
                    // Code that will be executed regardless of whether an exception occurred or not
                }

            }
        }
    }

    /**
     * Method to delete profile
     */
    fun gotoDeleteProfile() {
        currentAppState = AppState.RUNNING
        val fragment = DeleteProfileFragment()
         ActivityUtil.navigateToFragment(fragmentManager, fragment, false, "delete_profile")//// for testing**
    }




    /**
     * Method to popup fragment from stack
     */
    fun closeFragment(){
        fragmentManager.popBackStack()
    }

    /**
     * Method to navigate chat screen
     */
    fun gotoNext(){
        val fragment = ChatGptFragment()
        ActivityUtil.navigateToFragment(fragmentManager, fragment, false, "chat_gpt")

    }

    /**
     * Method to navigate tune screen
     */
    fun gotoTuneScreen(){
        val fragment = TuneFragment()
        ActivityUtil.navigateToFragment(fragmentManager, fragment, false, "tune")

    }
    /**
     * Method to navigate hack screen
     */
    fun gotoHackScreen(){
        val fragment = HackFragment()
        ActivityUtil.navigateToFragment(fragmentManager, fragment, false, "hack")
    }
    /**
     * Method to navigate profile screen
     */
    fun gotoProfileScreen(){
        val fragment = ProfileFragment()
        ActivityUtil.navigateToFragment(fragmentManager, fragment, true, "profile")
    }




}
