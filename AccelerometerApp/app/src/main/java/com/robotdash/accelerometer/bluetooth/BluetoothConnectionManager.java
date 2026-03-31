package com.robotdash.accelerometer.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Bluetooth connection to HC-05 module using Bluetooth Classic (SPP).
 * Handles connection, disconnection, and data reading in background thread.
 */
public class BluetoothConnectionManager {
    private static final String TAG = "BluetoothConnMgr";
    
    // HC-05 uses standard SPP UUID
    private static final UUID HC05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int READ_BUFFER_SIZE = 1024;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BluetoothDevice connectedDevice;
    private boolean isConnected = false;
    private boolean shouldStopReading = false;

    // Callback interface
    public interface BluetoothCallback {
        void onConnectionStateChanged(boolean connected, String message);
        void onDataReceived(byte[] data);
        void onError(String error);
    }

    private BluetoothCallback callback;
    private Thread readThread;

    public BluetoothConnectionManager(Context context) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Set the callback for Bluetooth events
     */
    public void setCallback(BluetoothCallback callback) {
        this.callback = callback;
    }

    /**
     * Check if Bluetooth is available on this device
     */
    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    /**
     * Check if Bluetooth is currently enabled
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Get list of paired Bluetooth devices
     */
    public Set<BluetoothDevice> getPairedDevices() {
        if (bluetoothAdapter == null) {
            return null;
        }
        return bluetoothAdapter.getBondedDevices();
    }

    /**
     * Connect to a specific Bluetooth device (HC-05).
     * This should be called from a background thread as it's blocking.
     */
    public void connect(BluetoothDevice device) {
        if (device == null) {
            notifyError("Device is null");
            return;
        }

        new Thread(() -> {
            try {
                notifyStateChanged(false, "Connecting to " + device.getName() + "...");
                
                // Close existing connection if any
                disconnect();

                // Create the Bluetooth socket
                bluetoothSocket = device.createRfcommSocketToServiceRecord(HC05_UUID);
                
                // Close discovery to speed up connection
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.cancelDiscovery();
                }

                // Connect to the socket
                bluetoothSocket.connect();

                // Get streams
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();

                connectedDevice = device;
                isConnected = true;
                
                notifyStateChanged(true, "Connected to " + device.getName());
                
                // Start reading data
                startReadingData();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                isConnected = false;
                notifyError("Connection failed: " + e.getMessage());
                disconnect();
            }
        }).start();
    }

    /**
     * Disconnect from the Bluetooth device
     */
    public void disconnect() {
        try {
            shouldStopReading = true;
            
            // Stop the read thread
            if (readThread != null) {
                readThread.join(1000); // Wait up to 1 second
            }

            // Close streams
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            // Close socket
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }

            isConnected = false;
            connectedDevice = null;
            notifyStateChanged(false, "Disconnected");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during disconnect", e);
        }
    }

    /**
     * Check if currently connected to a device
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get the connected device
     */
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    /**
     * Send data to the Bluetooth device
     */
    public boolean sendData(String data) {
        if (!isConnected || outputStream == null) {
            notifyError("Not connected");
            return false;
        }

        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error sending data", e);
            notifyError("Send failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Start reading data from the Bluetooth stream in a background thread
     */
    private void startReadingData() {
        shouldStopReading = false;
        
        readThread = new Thread(() -> {
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            int bytes;

            while (!shouldStopReading && isConnected) {
                try {
                    // Read from the input stream
                    bytes = inputStream.read(buffer);
                    
                    if (bytes > 0) {
                        // Create a byte array with the actual data
                        byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        
                        // Notify callback
                        if (callback != null) {
                            callback.onDataReceived(data);
                        }
                    }
                } catch (IOException e) {
                    if (!shouldStopReading) {
                        Log.e(TAG, "Error reading from Bluetooth", e);
                        notifyError("Read error: " + e.getMessage());
                        isConnected = false;
                        break;
                    }
                }
            }
            
            Log.d(TAG, "Read thread stopped");
        });

        readThread.start();
    }

    /**
     * Notify connection state change
     */
    private void notifyStateChanged(boolean connected, String message) {
        Log.d(TAG, message);
        if (callback != null) {
            callback.onConnectionStateChanged(connected, message);
        }
    }

    /**
     * Notify error
     */
    private void notifyError(String error) {
        Log.e(TAG, error);
        if (callback != null) {
            callback.onError(error);
        }
    }
}
