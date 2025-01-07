package com.example.applora

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val deviceList: List<BluetoothDevice>, private val onItemClick: (BluetoothDevice) -> Unit) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private var context: Context? = null
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.deviceName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        context = parent.context
        return DeviceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val currentDevice = deviceList[position]
        val deviceName = if (checkBluetoothConnectPermission()) {
            try {
                currentDevice.name ?: "Unknown Device"
            } catch (e: SecurityException) {
                Log.e("DeviceAdapter", "SecurityException when getting device name: ${e.message}")
                "Unknown Device"
            }
        } else {
            "Unknown Device"
        }
        holder.deviceNameTextView.text = deviceName
        holder.itemView.setOnClickListener {
            onItemClick(currentDevice)
        }
    }

    override fun getItemCount() = deviceList.size
    private fun checkBluetoothConnectPermission(): Boolean {
        return context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            }
        } ?: false
    }
}