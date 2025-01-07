package com.example.applora

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.UUID

class BluetoothDeviceManager(
    private val context: Context,
    private val device: BluetoothDevice,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onMessageReceived: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private val tag = "BluetoothDeviceManager"
    private val messageBuffer = StringBuilder()

    fun connect() {
        Log.d(tag, "connect: Attempting to connect to device")
        if (checkBluetoothConnectPermission()) {
            try {
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
            } catch (e: SecurityException) {
                Log.e(tag, "SecurityException during connectGatt: ${e.message}")
                onError("SecurityException during connectGatt: ${e.message}")
            }
        } else {
            Log.e(tag, "BLUETOOTH_CONNECT permission not granted")
            onError("BLUETOOTH_CONNECT permission not granted")
        }
    }

    fun disconnect() {
        Log.d(tag, "disconnect: Attempting to disconnect from device")
        if (bluetoothGatt != null) {
            if (checkBluetoothConnectPermission()) {
                try {
                    bluetoothGatt?.disconnect()
                    Log.d(tag, "disconnect: Disconnect called")
                } catch (e: SecurityException) {
                    Log.e(tag, "SecurityException during disconnect: ${e.message}")
                    onError("SecurityException during disconnect: ${e.message}")
                }
            } else {
                Log.e(tag, "BLUETOOTH_CONNECT permission not granted")
                onError("BLUETOOTH_CONNECT permission not granted")
            }
        } else {
            Log.d(tag, "disconnect: bluetoothGatt is null, nothing to disconnect")
        }
    }

    @Suppress("DEPRECATION")
    fun sendMessage(message: String) {
        val messageWithEndChar = "$message!"
        Log.d(tag, "sendMessage: Attempting to send message: $messageWithEndChar, message length: ${messageWithEndChar.length}")
        if (characteristic != null) {
            if (checkBluetoothConnectPermission()) {
                try {
                    characteristic?.setValue(messageWithEndChar.toByteArray())
                    characteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    bluetoothGatt?.writeCharacteristic(characteristic)
                } catch (e: SecurityException) {
                    Log.e(tag, "SecurityException during writeCharacteristic: ${e.message}")
                    onError("SecurityException during writeCharacteristic: ${e.message}")
                }
            } else {
                Log.e(tag, "BLUETOOTH_CONNECT permission not granted")
                onError("BLUETOOTH_CONNECT permission not granted")
            }
        } else {
            Log.e(tag, "sendMessage: Characteristic is null")
            onError("Characteristic is null")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(tag, "onConnectionStateChange: status=$status, newState=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Connected to GATT server.")
                if (checkBluetoothConnectPermission()) {
                    try {
                        bluetoothGatt?.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(tag, "SecurityException during discoverServices: ${e.message}")
                        onError("SecurityException during discoverServices: ${e.message}")
                    }
                } else {
                    Log.e(tag, "BLUETOOTH_CONNECT permission not granted")
                    onError("BLUETOOTH_CONNECT permission not granted")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(tag, "Disconnected from GATT server.")
                onDisconnected()
                if (bluetoothGatt != null) {
                    if (checkBluetoothConnectPermission()) {
                        try {
                            bluetoothGatt?.close()
                            Log.d(tag, "onConnectionStateChange: Close called")
                        } catch (e: SecurityException) {
                            Log.e(tag, "SecurityException during close: ${e.message}")
                            onError("SecurityException during close: ${e.message}")
                        }
                    } else {
                        Log.e(tag, "BLUETOOTH_CONNECT permission not granted")
                        onError("BLUETOOTH_CONNECT permission not granted")
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(tag, "onServicesDiscovered: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(tag, "Services discovered.")
                if (gatt != null) {
                    if (checkBluetoothConnectPermission()) {
                        try {
                            gatt.requestMtu(512)
                        } catch (e: SecurityException) {
                            Log.e(tag, "SecurityException during requestMtu: ${e.message}")
                            onError("SecurityException during requestMtu: ${e.message}")
                        }
                    } else {
                        Log.e(tag, "BLUETOOTH_CONNECT permission not granted")
                        onError("BLUETOOTH_CONNECT permission not granted")
                    }
                }
                val service = gatt?.getService(UUIDs.SERVICE_UUID)
                if (service == null) {
                    Log.e(tag, "Service not found.")
                    onError("Service not found.")
                    return
                }
                characteristic = service.getCharacteristic(UUIDs.CHARACTERISTIC_UUID)
                if (characteristic == null) {
                    Log.e(tag, "Characteristic not found.")
                    onError("Characteristic not found.")
                    return
                }
                val characteristicProperties = characteristic!!.properties
                if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    val descriptor = characteristic!!.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    if (descriptor != null) {
                        Log.d(tag, "Descriptor found.")
                        @Suppress("DEPRECATION")
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        if (checkBluetoothConnectPermission()) {
                            try {
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(descriptor)
                            } catch (e: SecurityException) {
                                Log.e(tag, "SecurityException during writeDescriptor: ${e.message}")
                                onError("SecurityException during writeDescriptor: ${e.message}")
                            }
                        } else {
                            Log.e(tag, "BLUETOOTH_CONNECT permission not granted")
                            onError("BLUETOOTH_CONNECT permission not granted")
                        }
                    } else {
                        Log.e(tag, "Descriptor not found.")
                    }
                    if (checkBluetoothConnectPermission()) {
                        try {
                            gatt.setCharacteristicNotification(characteristic, true)
                        } catch (e: SecurityException) {
                            Log.e(tag, "SecurityException during setCharacteristicNotification: ${e.message}")
                            onError("SecurityException during setCharacteristicNotification: ${e.message}")
                        }
                    } else {
                        Log.e(tag, "BLUETOOTH_CONNECT permission not granted")
                        onError("BLUETOOTH_CONNECT permission not granted")
                    }
                }
                onConnected()
            } else {
                Log.e(tag, "onServicesDiscovered: Failed to discover services.")
                onError("Failed to discover services.")
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.value != null) {
                val messageBytes = characteristic.value
                val messagePart = messageBytes.decodeToString()
                Log.d(tag, "onCharacteristicChanged: Received message part: $messagePart, part length: ${messagePart.length}")
                Log.d(tag, "onCharacteristicChanged: Received message part as bytes: ${messageBytes.contentToString()}")

                messageBuffer.append(messagePart)

                if (messagePart.contains("!")) {
                    val completeMessage = messageBuffer.toString().replace("!", "")
                    Log.d(tag, "onCharacteristicChanged: Complete message received: $completeMessage, message length: ${completeMessage.length}")
                    onMessageReceived(completeMessage)
                    messageBuffer.clear()
                }
            } else {
                Log.e(tag, "onCharacteristicChanged: characteristic.value is null")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.d(tag, "onCharacteristicWrite: status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(tag, "onCharacteristicWrite: Failed to write characteristic.")
                onError("Failed to write characteristic.")
            } else {
                Log.d(tag, "onCharacteristicWrite: Characteristic write successful.")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d(tag, "onMtuChanged: mtu=$mtu, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(tag, "MTU changed to $mtu")
            } else {
                Log.e(tag, "Failed to change MTU")
            }
        }
    }

    private fun checkBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }
}