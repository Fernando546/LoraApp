package com.example.applora

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private val deviceList = mutableListOf<BluetoothDevice>()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private lateinit var btnRefresh: ImageButton

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 2
        private const val TAG = "MainActivity"
    }

    private val enableBtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            checkBluetoothPermissions()
        } else {
            Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        // Dodanie separatora
        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        devicesRecyclerView.addItemDecoration(dividerItemDecoration)
        deviceAdapter = DeviceAdapter(deviceList) { device ->
            connectToDevice(device)
        }
        devicesRecyclerView.adapter = deviceAdapter
        btnRefresh = findViewById(R.id.btnRefresh)
        btnRefresh.setOnClickListener {
            refreshDeviceList()
        }

        @Suppress("DEPRECATION")
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        } else {
            checkBluetoothPermissions()
        }
    }
    private fun refreshDeviceList() {
        Log.d(TAG, "refreshDeviceList: Refreshing device list")
        deviceList.clear()
        deviceAdapter.notifyDataSetChanged()
        startScan()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        val deviceName = if (checkBluetoothConnectPermission()) {
            try {
                device.name ?: "Unknown Device"
            } catch (e: SecurityException){
                Log.e(TAG, "SecurityException when getting device name: ${e.message}")
                "Unknown Device"
            }
        } else {
            "Unknown Device"
        }
        Log.d(TAG, "connectToDevice: Attempting to connect to device: $deviceName")
        BluetoothDeviceManager(
            this,
            device,
            onConnected = {
                Log.d(TAG, "Connected to BLE device")
                runOnUiThread {
                    Toast.makeText(this, "Connected to device", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MessagingActivity::class.java)
                    intent.putExtra("device", device)
                    startActivity(intent)
                }
            },
            onDisconnected = {
                Log.d(TAG, "Disconnected from BLE device")
                runOnUiThread {
                    Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show()
                }
            },
            onMessageReceived = { message ->
                Log.d(TAG, "Received message: $message")
            },
            onError = { errorMessage ->
                Log.e(TAG, "Error: $errorMessage")
                runOnUiThread {
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        ).connect()
    }

    private fun checkBluetoothPermissions() {
        Log.d(TAG, "checkBluetoothPermissions() called")
        val bluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        val bluetoothScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        Log.d(TAG, "BLUETOOTH_CONNECT permission: ${bluetoothConnectPermission == PackageManager.PERMISSION_GRANTED}")
        Log.d(TAG, "BLUETOOTH_SCAN permission: ${bluetoothScanPermission == PackageManager.PERMISSION_GRANTED}")
        Log.d(TAG, "ACCESS_FINE_LOCATION permission: ${fineLocationPermission == PackageManager.PERMISSION_GRANTED}")
        Log.d(TAG, "ACCESS_COARSE_LOCATION permission: ${coarseLocationPermission == PackageManager.PERMISSION_GRANTED}")

        val permissionsToRequest = mutableListOf<String>()
        if (bluetoothConnectPermission != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (bluetoothScanPermission != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions")
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSION)
        } else {
            Log.d(TAG, "Permissions already granted")
            startScan()
        }
    }

    private fun startScan() {
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUIDs.SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val bluetoothScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_COARSE_LOCATION
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                bluetoothScanPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, leScanCallback)
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val bluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_CONNECT
            } else {
                Manifest.permission.BLUETOOTH
            }
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    bluetoothConnectPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            Log.d(TAG, "Found device: ${device.name}")
            if (!deviceList.contains(device)) {
                deviceList.add(device)
                deviceAdapter.notifyItemInserted(deviceList.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error code: $errorCode")
        }
    }

    override fun onResume() {
        super.onResume()
        checkBluetoothPermissions()
    }

    override fun onPause() {
        super.onPause()
        val bluetoothScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                bluetoothScanPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothLeScanner?.stopScan(leScanCallback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult() called")
        Log.d(TAG, "requestCode: $requestCode")
        Log.d(TAG, "permissions: ${permissions.joinToString()}")
        Log.d(TAG, "grantResults: ${grantResults.joinToString()}")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "Permissions granted")
                startScan()
            } else {
                Log.d(TAG, "Permissions denied")
                Toast.makeText(this, "Bluetooth and location permissions are required to discover devices", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun checkBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }
}