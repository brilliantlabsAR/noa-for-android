package xyz.brilliant.argpt

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import xyz.brilliant.argpt.service.ForegroundService
import xyz.brilliant.argpt.ui.activity.BaseActivity

class MainActivity : AppCompatActivity() {



    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val LOCATION_PERMISSION_REQUEST = 123
    private var listView: ListView? = null
    private val BLUETOOTH_REQUEST_CODE = 2
    private val bluetoothDevices: ArrayList<BluetoothDevice> = ArrayList()
    private val mDeviceList = ArrayList<String>()


    private lateinit var text_home: TextView
    private  lateinit var disConnect : Button
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanning = false
    private val handler = Handler()
    //private val leDeviceListAdapter = LeDeviceListAdapter()

    val progressDialog = ProgressDialog(this)
    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000
    val deviceList: MutableList<BluetoothDevice> = ArrayList()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)



        val foregroundServiceIntent = Intent(this, ForegroundService::class.java)

        bluetoothManager=getSystemService(BluetoothManager::class.java)
        bluetoothAdapter=bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            == PackageManager.PERMISSION_GRANTED
        ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(foregroundServiceIntent)
            } else {
             //   startService(foregroundServiceIntent)
            }
            // Permission already granted
            // Start using Bluetooth features
       //     startBluetoothOperation()
        } else {
            // Permission is not granted
            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
        /// Start Background Service
       // val serviceIntent = Intent(this, BluetoothBackgroundService::class.java)
      //  this.startService(serviceIntent)

        text_home = findViewById(R.id.text_home)
        disConnect = findViewById(R.id.disConnect)
        val fabScan: Button = findViewById(R.id.fabScan)
        fabScan.setOnClickListener {
          //  startBleDeviceDiscovery()
            showPopup(this@MainActivity,deviceList)
        }

        val fabStart: Button = findViewById(R.id.fabStart)
        fabStart.setOnClickListener {
            //  startBleDeviceDiscovery()
            //showPopup(this@MainActivity,deviceList)
          //  enableBluetooth()
        }

        val comunication : Button = findViewById(R.id.communication)

        comunication.setOnClickListener {
            val intent = Intent(this, BaseActivity::class.java)
            startActivity(intent)
        }


    }


    private val REQUEST_ENABLE_BLUETOOTH = 1
    private val REQUEST_PERMISSION_LOCATION = 2
    private fun startBluetoothOperation() {
        // Enable Bluetooth if it's not already enabled
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        } else {
            // Bluetooth is enabled, start the scan
//            val scanCallback = object : ScanCallback() {
//                @SuppressLint("MissingPermission", "SuspiciousIndentation")
//                override fun onScanResult(callbackType: Int, result: ScanResult?) {
//                    super.onScanResult(callbackType, result)
//
//                    result?.device?.let { bluetoothDevice ->
//                        // Process the found Bluetooth device
//                        val deviceName = bluetoothDevice.name
//                        val deviceAddress = bluetoothDevice.address
//
//                            if (!deviceList.contains(result.device)) {
//                                deviceList.add(result.device)
//                              //  deviceAdapter?.notifyDataSetChanged()
//                            }
//
//                        // Do something with the deviceName and deviceAddress
//                    }
//
//
//
//
//                }
//
//                override fun onScanFailed(errorCode: Int) {
//                    super.onScanFailed(errorCode)
//                    // Handle scan failure
//                }
            }

            val scanFilters = mutableListOf<ScanFilter>()
            val scanSettings = ScanSettings.Builder().build()

         //   bluetoothAdapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        }


//    private fun enableBluetooth() {
//        progressDialog.show()
//        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        if (bluetoothAdapter == null) {
//            // Device doesn't support Bluetooth
//            // Handle the error here
//        } else {
//            // Enable Bluetooth
//            if (!bluetoothAdapter.isEnabled) {
//                progressDialog.dismiss()
//                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                if (ActivityCompat.checkSelfPermission(
//                        this,
//                       // Manifest.permission.BLUETOOTH_SCAN,
//                      //  Manifest.permission.ACCESS_COARSE_LOCATION,
//                        Manifest.permission.BLUETOOTH_CONNECT
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return
//                }
//                startActivityForResult(enableBluetoothIntent, BLUETOOTH_REQUEST_CODE)
//            } else {
//                // Bluetooth is already enabled
//                // Continue with your logic here
//
//                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
//                pairedDevices?.forEach { device ->
//                    val deviceName = device.name
//                    val deviceHardwareAddress = device.address // MAC address
//                    Log.d("TAG", "enableBluetooth: "+deviceName)
//                    Log.d("TAG23", "enableBluetooth: "+deviceHardwareAddress)
//
//                    mDeviceList.add(device.getName() + "\n" + device.getAddress())
//                    Log.i("BT", device.getName() + "\n" + device.getAddress())
////                    listView!!.adapter = ArrayAdapter<String>(
////                        baseContext,
////                        android.R.layout.simple_list_item_1, mDeviceList
////                    )
//                }
//                Log.d("TAG", "enableBluetooth: ")
//                progressDialog.dismiss()
//               // scanLeDevice()
//            }
//        }
//        progressDialog.dismiss()
//    }

class DeviceAdapter(context: Context, resource: Int, private val devices: ArrayList<BluetoothDevice>) :
    ArrayAdapter<BluetoothDevice>(context, resource, devices) {

    @SuppressLint("MissingPermission")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(android.R.layout.simple_list_item_1, null)
        }

        val deviceNameTextView = view?.findViewById(android.R.id.text1) as TextView
        deviceNameTextView.text = devices[position].name ?: "Unknown"

        return view
    }
}
    private lateinit var alertDialog: AlertDialog
    fun showPopup(context: Context, deviceList: MutableList<BluetoothDevice>) {
        val dialogBuilder = AlertDialog.Builder(context)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.popup, null)
        dialogBuilder.setView(dialogView)

       var listView: ListView
//        val items = arrayOf("Item 1", "Item 2", "Item 3")
//        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, deviceList)


       var deviceAdapter = DeviceAdapter(this, android.R.layout.simple_list_item_1, ArrayList(deviceList))
         listView = findViewById(R.id.listView)
        listView.adapter = deviceAdapter



       // listView.adapter = adapter
        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedDevice = deviceList[position]
            // Call your function with the selected device
          //  yourFunction(selectedDevice)
            connectDevice(selectedDevice)
            alertDialog.dismiss()

        }
         alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    lateinit var selectedDevice: BluetoothDevice

    lateinit var bluetoothGatt: BluetoothGatt

    @SuppressLint("MissingPermission")
    private fun connectDevice(device: BluetoothDevice) {
        val gattCallback = object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                text_home.text =""
                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    Log.d("TAG", "Connected: ")

                    text_home.text = "Connected "

                 //   Toast.makeText(this@MainActivity,"Connected",Toast.LENGTH_LONG).show()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Disconnected from the device
                    // You can handle the disconnection event here
                    disConnect.visibility = View.GONE
                    text_home.text = "Disconnected"
                   // Toast.makeText(this@MainActivity,"DisConnected",Toast.LENGTH_LONG).show()
                    Log.d("TAG", "Connected: ")


                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)

                if (status == BluetoothGatt.GATT_SUCCESS) {

                    Log.d("TAG", "Connected: Discovered")
                  //  Toast.makeText(this@MainActivity,"Discovered",Toast.LENGTH_LONG).show()
                    // Services discovered successfully
                    // You can now access the services and characteristics of the device
                    // For example, get a reference to a specific characteristic using gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                } else {
                    Log.d("TAG", "Connected: ")
                   // Toast.makeText(this@MainActivity,"Not Discovered",Toast.LENGTH_LONG).show()
                    // Failed to discover services
                    // You can handle the failure here
                }
            }

            // Other callback methods such as onCharacteristicRead, onCharacteristicWrite, etc.
        }


        disConnect.visibility = View.VISIBLE
        disConnect.setOnClickListener {
          //  device.dis(this, false, gattCallback)
            bluetoothGatt.disconnect()
        }

        // Connect to the BLE device
         if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        )
         {

             Log.d("Bluetoothscan", "enableBluetoothscan: ")

            return
        } else {
             bluetoothGatt =  device.connectGatt(this, false, gattCallback)

        }


        // Note: Make sure to handle the BluetoothGatt object appropriately throughout the connection lifecycle
    }


   @SuppressLint("MissingPermission")
   fun connectDeviceOld(selectedDevice: BluetoothDevice) {
       val selectedDevice: BluetoothDevice = selectedDevice// Get the selected device from user interaction

       val gattCallback = object : BluetoothGattCallback() {
           override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
               if (newState == BluetoothProfile.STATE_CONNECTED) {
                   Log.d("TAG", "Connected: "+selectedDevice)
                   Toast.makeText(this@MainActivity,"Connected",Toast.LENGTH_LONG).show()
                  // Toast.makeText(this, "Connected: ", Toast.LENGTH_SHORT).show()
                   // Device connected, perform further operations
               } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                   // Device disconnected
                   Toast.makeText(this@MainActivity,"Disconnected",Toast.LENGTH_LONG).show()
                  // Toast.makeText(this@MainActivity,"Connected",Toast.LENGTH_LONG).show()
                  // Log.d("TAG", "Disconnected: "+selectedDevice)
               }
           }
       }


       selectedDevice?.connectGatt(this@MainActivity, false, gattCallback)
   }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BLUETOOTH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                // Bluetooth is enabled successfully
                // Continue with your logic here
            } else {
                // Bluetooth enabling was canceled or failed
                // Handle the error here
            }
        }
    }

}

class ProgressDialog(private val context: Context) {
    private var dialog: AlertDialog? = null

    fun show() {
        val builder = AlertDialog.Builder(context)
        val progressBar = ProgressBar(context)
        builder.setView(progressBar)
        builder.setCancelable(false)

        dialog = builder.create()
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}