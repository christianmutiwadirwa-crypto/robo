package com.robotdash.accelerometer;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.robotdash.accelerometer.bluetooth.BluetoothConnectionManager;
import com.robotdash.accelerometer.data.AccelerometerDataParser;
import com.robotdash.accelerometer.data.AccelerometerReading;
import com.robotdash.accelerometer.network.HttpServerClient;
import com.robotdash.accelerometer.robot.RobotCommandController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnRefreshDevices;
    private Button btnSaveServer;
    private Button btnForward;
    private Button btnBackward;
    private Button btnLeft;
    private Button btnRight;
    private Button btnStop;
    private TextView tvStatus;
    private TextView tvXValue;
    private TextView tvYValue;
    private TextView tvZValue;
    private TextView tvLastUpdate;
    private TextView tvCommandStatus;
    private TextView tvSpeedValue;
    private SeekBar sbSpeedControl;
    private EditText etServerUrl;
    private ListView lvDevices;

    // Managers
    private BluetoothConnectionManager bluetoothManager;
    private AccelerometerDataParser dataParser;
    private HttpServerClient httpClient;
    private RobotCommandController robotCommandController;

    // Data
    private List<BluetoothDevice> pairedDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;
    private BluetoothDevice selectedDevice = null;

    private static final String DEFAULT_SERVER_URL = "http://192.168.1.100:3000/data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        initializeUI();

        // Check and request permissions
        checkAndRequestPermissions();

        // Initialize managers
        bluetoothManager = new BluetoothConnectionManager(this);
        dataParser = new AccelerometerDataParser();
        httpClient = new HttpServerClient(DEFAULT_SERVER_URL);
        httpClient.setSendFrequencyHz(10); // 10 Hz
        robotCommandController = new RobotCommandController(bluetoothManager);

        // Set Bluetooth callback
        bluetoothManager.setCallback(new BluetoothConnectionManager.BluetoothCallback() {
            @Override
            public void onConnectionStateChanged(boolean connected, String message) {
                runOnUiThread(() -> updateConnectionStatus(connected, message));
            }

            @Override
            public void onDataReceived(byte[] data) {
                processBluetoothData(data);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvStatus.setText("Error: " + error);
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                });
            }
        });

        // Set robot command callback
        robotCommandController.setCallback((command, success) -> {
            runOnUiThread(() -> {
                String status = RobotCommandController.getCommandName(command) + 
                    (success ? " ✓" : " ✗");
                tvCommandStatus.setText(status);
                tvCommandStatus.setTextColor(success ? 
                    getResources().getColor(android.R.color.holo_green_light) :
                    getResources().getColor(android.R.color.holo_red_light));
            });
        });

        // Refresh device list on startup
        refreshDeviceList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothManager != null && bluetoothManager.isConnected()) {
            bluetoothManager.disconnect();
        }
    }

    /**
     * Initialize all UI components
     */
    private void initializeUI() {
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnRefreshDevices = findViewById(R.id.btnRefreshDevices);
        btnSaveServer = findViewById(R.id.btnSaveServer);
        btnForward = findViewById(R.id.btnForward);
        btnBackward = findViewById(R.id.btnBackward);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnStop = findViewById(R.id.btnStop);
        tvStatus = findViewById(R.id.tvStatus);
        tvXValue = findViewById(R.id.tvXValue);
        tvYValue = findViewById(R.id.tvYValue);
        tvZValue = findViewById(R.id.tvZValue);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        tvCommandStatus = findViewById(R.id.tvCommandStatus);
        etServerUrl = findViewById(R.id.etServerUrl);
        lvDevices = findViewById(R.id.lvDevices);
        sbSpeedControl = findViewById(R.id.sbSpeedControl);
        tvSpeedValue = findViewById(R.id.tvSpeedValue);

        // Set default server URL
        etServerUrl.setText(DEFAULT_SERVER_URL);

        // Set click listeners
        btnConnect.setOnClickListener(v -> connectToDevice());
        btnDisconnect.setOnClickListener(v -> disconnectDevice());
        btnRefreshDevices.setOnClickListener(v -> refreshDeviceList());
        btnSaveServer.setOnClickListener(v -> saveServerUrl());

        // Robot control listeners
        btnForward.setOnClickListener(v -> robotCommandController.sendCommand(RobotCommandController.CMD_FORWARD));
        btnBackward.setOnClickListener(v -> robotCommandController.sendCommand(RobotCommandController.CMD_BACKWARD));
        btnLeft.setOnClickListener(v -> robotCommandController.sendCommand(RobotCommandController.CMD_LEFT));
        btnRight.setOnClickListener(v -> robotCommandController.sendCommand(RobotCommandController.CMD_RIGHT));
        btnStop.setOnClickListener(v -> robotCommandController.emergencyStop());

        // Device list adapter
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvDevices.setAdapter(deviceAdapter);
        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = pairedDevices.get(position);
            Toast.makeText(MainActivity.this, "Selected: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
        });

        // Initial status
        tvStatus.setText("Disconnected");
        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        tvCommandStatus.setText("Ready");
        btnDisconnect.setEnabled(false);

        // Speed control listener
        sbSpeedControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpeedValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int finalSpeed = seekBar.getProgress();
                robotCommandController.sendCommand("SPEED:" + finalSpeed);
            }
        });
    }

    /**
     * Check and request necessary permissions
     */
    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Bluetooth permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        // For Android 12+, need BLUETOOTH_CONNECT and BLUETOOTH_SCAN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }

        // Internet permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.INTERNET);
        }

        // Request all needed permissions
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Permissions denied. App may not work properly.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Refresh the list of paired Bluetooth devices
     */
    private void refreshDeviceList() {
        if (!bluetoothManager.isBluetoothAvailable()) {
            tvStatus.setText("Bluetooth not available");
            return;
        }

        if (!bluetoothManager.isBluetoothEnabled()) {
            tvStatus.setText("Bluetooth is disabled");
            return;
        }

        pairedDevices.clear();
        deviceAdapter.clear();

        Set<BluetoothDevice> devices = bluetoothManager.getPairedDevices();
        if (devices != null) {
            pairedDevices.addAll(devices);
            for (BluetoothDevice device : devices) {
                deviceAdapter.add(device.getName() + " (" + device.getAddress() + ")");
            }
            deviceAdapter.notifyDataSetChanged();
        }

        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Connect to the selected device
     */
    private void connectToDevice() {
        if (selectedDevice == null) {
            Toast.makeText(this, "Please select a device", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConnect.setEnabled(false);
        bluetoothManager.connect(selectedDevice);
    }

    /**
     * Disconnect from the current device
     */
    private void disconnectDevice() {
        bluetoothManager.disconnect();
        dataParser.clearBuffer();
        btnConnect.setEnabled(true);
    }

    /**
     * Update connection status display
     */
    private void updateConnectionStatus(boolean connected, String message) {
        tvStatus.setText(message);
        if (connected) {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            btnConnect.setEnabled(false);
            btnDisconnect.setEnabled(true);
            btnRefreshDevices.setEnabled(false);
        } else {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            btnConnect.setEnabled(true);
            btnDisconnect.setEnabled(false);
            btnRefreshDevices.setEnabled(true);
        }
    }

    /**
     * Process data received from Bluetooth
     */
    private void processBluetoothData(byte[] data) {
        AccelerometerReading reading = dataParser.processData(data);

        if (reading != null) {
            // Update UI
            runOnUiThread(() -> {
                tvXValue.setText(String.format("X: %.4f m/s²", reading.x));
                tvYValue.setText(String.format("Y: %.4f m/s²", reading.y));
                tvZValue.setText(String.format("Z: %.4f m/s²", reading.z));
                tvLastUpdate.setText("Last: " + java.text.SimpleDateFormat.getTimeInstance().format(new java.util.Date(reading.timestamp)));
            });

            // Send to server
            httpClient.sendDataAsync(reading);
        }
    }

    /**
     * Save the server URL
     */
    private void saveServerUrl() {
        String url = etServerUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
            return;
        }

        httpClient.setServerUrl(url);
        Toast.makeText(this, "Server URL saved: " + url, Toast.LENGTH_SHORT).show();
    }
}
