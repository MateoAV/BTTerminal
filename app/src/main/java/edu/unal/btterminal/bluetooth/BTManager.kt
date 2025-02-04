package edu.unal.btterminal.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.io.IOException
import java.util.UUID
import edu.unal.btterminal.PermissionManager
import edu.unal.btterminal.utils.LineEnding

class BTManager private constructor(private val context: Context) {
    companion object {
        private const val TAG = "BTManager"
        // Standard SerialPortService ID
        private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: BTManager? = null
        
        fun getInstance(context: Context): BTManager {
            return instance ?: synchronized(this) {
                instance ?: BTManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val permissionManager = PermissionManager(context)
    
    private var discoveryCallback: ((BluetoothDevice) -> Unit)? = null
    private var connectionCallback: ((Boolean) -> Unit)? = null
    var currentSocket: BluetoothSocket? = null
    private var messageCallback: ((String) -> Unit)? = null
    private var isReceiving = false
    private val buffer = ByteArray(1024)
    
    private var isMockDevice = false
    
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    
                    device?.let {
                        try {
                            val deviceName = if (checkPermissions()) {
                                it.name ?: "Unknown"
                            } else {
                                "Unknown"
                            }
                            val deviceAddress = if (checkPermissions()) {
                                it.address
                            } else {
                                "Unknown Address"
                            }
                            
                            Log.d(TAG, "Device found: $deviceName ($deviceAddress)")
                            discoveryCallback?.invoke(it)
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Security exception while accessing device properties", e)
                            discoveryCallback?.invoke(it)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "Discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Discovery finished")
                }
            }
        }
    }

    init {
        Log.d(TAG, "Initializing BTManager")
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth")
        } else {
            Log.d(TAG, "Bluetooth adapter found")
        }
    }

    private fun checkPermissions(): Boolean {
        val hasPermissions = permissionManager.hasRequiredPermissions()
        if (!hasPermissions) {
            Log.e(TAG, "Missing required Bluetooth permissions")
        }
        return hasPermissions
    }

    fun isBluetoothEnabled(): Boolean {
        if (!checkPermissions()) {
            Log.e(TAG, "Cannot check Bluetooth state - missing permissions")
            return false
        }
        
        return bluetoothAdapter?.isEnabled == true.also {
            Log.d(TAG, "Bluetooth enabled: $it")
        }
    }

    fun startDiscovery(callback: (BluetoothDevice) -> Unit) {
        Log.d(TAG, "Starting discovery...")
        
        if (!checkPermissions()) {
            Log.e(TAG, "Cannot start discovery - missing permissions")
            return
        }
        
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Cannot start discovery - Bluetooth is disabled")
            return
        }

        discoveryCallback = callback

        // Register for broadcasts
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)

        try {
            // Cancel any ongoing discovery
            if (bluetoothAdapter?.isDiscovering == true) {
                Log.d(TAG, "Canceling ongoing discovery...")
                bluetoothAdapter.cancelDiscovery()
            }

            // Start new discovery
            val started = bluetoothAdapter?.startDiscovery() ?: false
            Log.d(TAG, "Discovery start result: $started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during discovery operations", e)
        }
    }

    fun stopDiscovery() {
        Log.d(TAG, "Stopping discovery...")
        try {
            context.unregisterReceiver(discoveryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered")
        }
        
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
                Log.d(TAG, "Discovery cancelled")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while canceling discovery", e)
        }
    }

    fun connectToDevice(device: BluetoothDevice?, callback: (Boolean) -> Unit) {
        if (device == null) {
            // This is our mock device
            Log.d(TAG, "Connecting to mock device...")
            connectionCallback = callback
            currentSocket = null
            isMockDevice = true
            isReceiving = true // Set receiving flag for mock device
            // Simulate successful connection
            connectionCallback?.invoke(true)
            return
        }

        // Reset mock flag for real devices
        isMockDevice = false
        
        if (!checkPermissions()) {
            Log.e(TAG, "Cannot connect to device - missing permissions")
            callback(false)
            return
        }
        
        try {
            Log.d(TAG, "Attempting to connect to device: ${device.name ?: "Unknown"} (${device.address})")
        } catch (e: SecurityException) {
            Log.d(TAG, "Attempting to connect to device (name/address unavailable due to permissions)")
        }
        
        connectionCallback = callback
        
        // Stop any ongoing discovery
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while canceling discovery", e)
        }

        Thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(UUID_SPP)
                Log.d(TAG, "Socket created, attempting connection...")
                
                socket.connect()
                currentSocket = socket
                Log.d(TAG, "Connection successful")
                
                connectionCallback?.invoke(true)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception during connection", e)
                connectionCallback?.invoke(false)
                closeConnection()
            } catch (e: IOException) {
                Log.e(TAG, "Connection failed", e)
                connectionCallback?.invoke(false)
                closeConnection()
            }
        }.start()
    }

    fun sendData(data: String, lineEnding: LineEnding): Boolean {
        if (isMockDevice && messageCallback != null) {
            // Mock device connection - handle LED messages
            Log.d(TAG, "Mock device received: $data")
            Thread {
                Thread.sleep(100) // Small delay to simulate device processing
                when (data.trim()) {
                    "1" -> messageCallback?.invoke("Led1_On")
                    "0" -> messageCallback?.invoke("Led1_Off")
                    else -> messageCallback?.invoke("Echo: $data") // Default echo for other messages
                }
            }.start()
            return true
        }

        // Real device communication
        if (!checkPermissions()) {
            Log.e(TAG, "Cannot send data - missing permissions")
            return false
        }
        
        if (currentSocket == null) {
            Log.e(TAG, "Cannot send data - no active connection")
            return false
        }
        
        return try {
            Log.d(TAG, "Sending to real device: $data with line ending: ${lineEnding.name}")
            val messageBytes = (data + lineEnding.value).toByteArray(Charsets.UTF_8)
            Log.d(TAG, "Sending bytes: ${messageBytes.joinToString(", ") { it.toString() }}")
            
            currentSocket?.outputStream?.apply {
                write(messageBytes)
                flush() // Ensure data is sent immediately
            }
            
            Log.d(TAG, "Data sent successfully to real device")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to send data to real device", e)
            false
        }
    }

    fun startReceiving(callback: (String) -> Unit) {
        messageCallback = callback
        isReceiving = true

        if (isMockDevice) {
            Log.d(TAG, "Mock device receiver initialized")
            return // For mock device, we just store the callback
        }

        // Real device communication
        Thread {
            var errorCount = 0
            val maxErrors = 3
            
            while (isReceiving) {
                try {
                    if (currentSocket == null) {
                        Log.e(TAG, "No active socket connection")
                        break
                    }

                    val bytes = currentSocket?.inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        Log.d(TAG, "Received from real device: $message")
                        messageCallback?.invoke(message)
                        errorCount = 0 // Reset error count on successful read
                    } else if (bytes == -1) {
                        Log.w(TAG, "End of stream reached")
                        break
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading from real device socket", e)
                    errorCount++
                    if (errorCount >= maxErrors) {
                        Log.e(TAG, "Too many consecutive errors, stopping receiver")
                        break
                    }
                    // Add a small delay before retrying
                    Thread.sleep(1000)
                }
            }
            
            Log.d(TAG, "Receiving loop ended")
            isReceiving = false
        }.start()
    }

    fun closeConnection() {
        isReceiving = false
        isMockDevice = false
        Log.d(TAG, "Closing connection...")
        try {
            currentSocket?.close()
            currentSocket = null
            Log.d(TAG, "Connection closed successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection", e)
        }
    }

    fun getPairedDevices(): Set<BluetoothDevice> {
        if (!checkPermissions()) {
            Log.e(TAG, "Cannot get paired devices - missing permissions")
            return emptySet()
        }
        
        Log.d(TAG, "Getting paired devices...")
        try {
            val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            Log.d(TAG, "Found ${pairedDevices.size} paired devices")
            
            pairedDevices.forEach { device ->
                try {
                    Log.d(TAG, "Paired device: ${device.name ?: "Unknown"} (${device.address})")
                } catch (e: SecurityException) {
                    Log.d(TAG, "Paired device: <Cannot access device info due to permissions>")
                }
            }
            return pairedDevices
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while getting paired devices", e)
            return emptySet()
        }
    }

    fun destroy() {
        Log.d(TAG, "Destroying BTManager")
        stopDiscovery()
        closeConnection()
    }
} 