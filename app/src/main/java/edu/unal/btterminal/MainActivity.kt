package edu.unal.btterminal

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import edu.unal.btterminal.bluetooth.BTManager
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.bluetooth.BluetoothDevice
import edu.unal.btterminal.adapter.BluetoothDeviceAdapter
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private lateinit var permissionManager: PermissionManager
    private val btManager by lazy { BTManager.getInstance(this) }
    private lateinit var btnScanDevices: Button
    private lateinit var rvDevices: RecyclerView
    private lateinit var deviceAdapter: BluetoothDeviceAdapter
    
    companion object {
        private const val TAG = "MainActivity"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            Log.d(TAG, "All permissions were granted")
            Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show()
            initializeBluetooth()
        } else {
            Log.w(TAG, "Some permissions were denied")
            val deniedPermissions = permissions.filterValues { !it }.keys
            Log.w(TAG, "Denied permissions: ${deniedPermissions.joinToString()}")
            Toast.makeText(
                this,
                "Bluetooth permissions are required for this app",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        btnScanDevices = findViewById(R.id.btnScanDevices)
        rvDevices = findViewById(R.id.rvDevices)
        
        // Setup RecyclerView and adapter
        deviceAdapter = BluetoothDeviceAdapter { device ->
            handleDeviceClick(device)
        }
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = deviceAdapter
        
        // Initialize managers
        permissionManager = PermissionManager(this)
        
        // Setup click listeners
        setupClickListeners()
        
        // Check and request permissions
        Log.d(TAG, "Checking permissions...")
        if (!permissionManager.hasRequiredPermissions()) {
            Log.d(TAG, "Permissions not granted, requesting them...")
            requestPermissionLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS)
        } else {
            Log.d(TAG, "All permissions already granted")
            initializeBluetooth()
        }
    }

    private fun handleDeviceClick(device: BluetoothDevice?) {
        btManager.stopDiscovery() // Stop scanning when a device is selected
        
        try {
            val deviceName = device?.name ?: "Unknown Device"
            Toast.makeText(this, "Connecting to $deviceName...", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Connecting to device...", Toast.LENGTH_SHORT).show()
        }

        btManager.connectToDevice(device) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Connected successfully!", Toast.LENGTH_SHORT).show()
                    // Launch Terminal Activity
                    val intent = Intent(this, TerminalActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnScanDevices.setOnClickListener {
            if (!btManager.isBluetoothEnabled()) {
                Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            // Clear previous devices and start scanning
            deviceAdapter.clear()
            btManager.startDiscovery { device ->
                runOnUiThread {
                    deviceAdapter.addDevice(device)
                }
            }
        }
    }

    private fun initializeBluetooth() {
        if (!btManager.isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled")
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        // Example of getting paired devices
        val pairedDevices = btManager.getPairedDevices()
        Log.d(TAG, "Found ${pairedDevices.size} paired devices")
    }

    override fun onDestroy() {
        super.onDestroy()
        btManager.destroy()
    }
}