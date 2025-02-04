package edu.unal.btterminal.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.unal.btterminal.R
import edu.unal.btterminal.utils.MockDeviceWrapper

class BluetoothDeviceAdapter(
    private val onDeviceClick: (BluetoothDevice?) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<DeviceItem>()
    private val deviceAddresses = mutableSetOf<String>()

    internal data class DeviceItem(
        val device: BluetoothDevice?,
        val isMock: Boolean = false,
        val name: String = "",
        val address: String = ""
    )

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        devices.clear()
        deviceAddresses.clear()
        notifyDataSetChanged()
        // Add mock device after clearing when scanning starts
        addMockDevice()
    }

    private fun addMockDevice() {
        devices.add(DeviceItem(
            device = null,
            isMock = true,
            name = "${MockDeviceWrapper.MOCK_NAME} (MOCK DEVICE - NOT REAL DEVICE)",
            address = MockDeviceWrapper.MOCK_ADDRESS
        ))
        deviceAddresses.add(MockDeviceWrapper.MOCK_ADDRESS)
        notifyItemInserted(devices.size - 1)
    }

    fun addDevice(device: BluetoothDevice) {
        try {
            val address = device.address
            if (!deviceAddresses.contains(address)) {
                deviceAddresses.add(address)
                devices.add(DeviceItem(
                    device = device,
                    name = device.name ?: "Unknown Device",
                    address = address
                ))
                notifyItemInserted(devices.size - 1)
            }
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return DeviceViewHolder(view, onDeviceClick)
    }

    override fun getItemCount() = devices.size

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    class DeviceViewHolder(
        view: View,
        private val onDeviceClick: (BluetoothDevice?) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = view.findViewById(R.id.tvDeviceName)
        private val deviceAddress: TextView = view.findViewById(R.id.tvDeviceAddress)

        internal fun bind(deviceItem: DeviceItem) {
            deviceName.text = deviceItem.name
            deviceAddress.text = deviceItem.address

            // Set different background for mock device
            if (deviceItem.isMock) {
                itemView.setBackgroundColor(0x1FFF0000) // Light red background
            } else {
                itemView.setBackgroundColor(0x00000000) // Transparent background
            }

            itemView.setOnClickListener {
                onDeviceClick(deviceItem.device)
            }
        }
    }
} 