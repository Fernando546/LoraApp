package com.example.applora

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MessagingActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var editTextMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnDisconnect: ImageButton
    private lateinit var device: BluetoothDevice
    private val messageList = mutableListOf<Message>()
    private var bluetoothDeviceManager: BluetoothDeviceManager? = null
    private lateinit var connectedDeviceTextView: TextView

    companion object {
        private const val TAG = "MessagingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messaging)

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        editTextMessage = findViewById(R.id.editTextMessage)
        btnSend = findViewById(R.id.btnSend)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        connectedDeviceTextView = findViewById(R.id.connectedDeviceTextView)

        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messageAdapter = MessageAdapter(messageList)
        messagesRecyclerView.adapter = messageAdapter
        btnSend.isEnabled = false

        @Suppress("DEPRECATION")
        device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("device", BluetoothDevice::class.java)!!
        } else {
            intent.getParcelableExtra("device")!!
        }
        connectToDevice(device)
        val vectorDrawableSend = ResourcesCompat.getDrawable(resources, R.drawable.ic_send, null)
        btnSend.setImageDrawable(vectorDrawableSend)
        btnSend.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)

        val vectorDrawableDisconnect = ResourcesCompat.getDrawable(resources, R.drawable.ic_disconnect, null)
        btnDisconnect.setImageDrawable(vectorDrawableDisconnect)
        btnDisconnect.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)

        btnSend.setOnClickListener {
            Log.d(TAG, "btnSend clicked")
            val message = editTextMessage.text.toString()
            Log.d(TAG, "Sending message: $message")
            bluetoothDeviceManager?.sendMessage(message)
            messageList.add(Message(message, true, "You"))
            messageAdapter.notifyItemInserted(messageList.size - 1)
            messagesRecyclerView.scrollToPosition(messageList.size - 1)
            editTextMessage.text.clear()
        }

        btnDisconnect.setOnClickListener {
            Log.d(TAG, "btnDisconnect clicked")
            bluetoothDeviceManager?.disconnect()
            finish()
        }
    }

    private fun updateConnectedDeviceText(deviceName: String) {
        connectedDeviceTextView.text = getString(R.string.write_as, deviceName)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice: Attempting to connect to device: ${device.name}")
        val deviceName = if (checkBluetoothConnectPermission()) {
            try {
                device.name ?: "Unknown Device"
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException when getting device name: ${e.message}")
                "Unknown Device"
            }
        } else {
            "Unknown Device"
        }
        Log.d(TAG, "Connecting to device: $deviceName")
        bluetoothDeviceManager = BluetoothDeviceManager(
            this,
            device,
            onConnected = {
                Log.d(TAG, "Connected to BLE device")
                runOnUiThread {
                    Toast.makeText(this, "Connected to device", Toast.LENGTH_SHORT).show()
                    btnSend.isEnabled = true
                    updateConnectedDeviceText(deviceName)
                }
            },
            onDisconnected = {
                Log.d(TAG, "Disconnected from BLE device")
                runOnUiThread {
                    Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show()
                    btnSend.isEnabled = false
                    navigateToMainActivity()
                }
            },
            onMessageReceived = { message ->
                Log.d(TAG, "Received message: $message")
                runOnUiThread {
                    val parts = message.split(":", limit = 2)
                    val senderId = if (parts.size == 2) parts[0] else "Unknown"
                    val receivedMessage = if (parts.size == 2) parts[1] else parts[0]
                    if(senderId != "Unknown"){
                        messageList.add(Message(receivedMessage, false, senderId))
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        messagesRecyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
            },
            onError = { errorMessage ->
                Log.e(TAG, "Error: $errorMessage")
                runOnUiThread {
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
        bluetoothDeviceManager?.connect()
    }

    private fun checkBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Disconnecting from device")
        bluetoothDeviceManager?.disconnect()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}