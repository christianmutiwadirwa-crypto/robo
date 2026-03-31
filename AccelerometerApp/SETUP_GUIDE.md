# Accelerometer Data Monitor - Android App
## Complete Setup and Deployment Guide

### Project Overview

This Android application connects to an **HC-05 Bluetooth Classic module**, reads real-time accelerometer data, and forwards it to a **Node.js backend server** via HTTP POST requests.

**Key Features:**
- ✅ Bluetooth Classic (SPP) connection to HC-05
- ✅ Real-time accelerometer data streaming
- ✅ Safe data parsing with error handling
- ✅ HTTP forwarding to Node.js server
- ✅ Rate limiting (10 Hz default)
- ✅ Background thread management
- ✅ Modern Android permissions handling
- ✅ Material Design UI

---

## Prerequisites

### Hardware
1. **Android Device** - Android 6.0+ (API level 21+)
2. **HC-05 Bluetooth Module** - Configured at 9600 baud
3. **Arduino with Accelerometer** - Sending data in format: `x,y,z\n`
4. **Laptop/Server** - Running Node.js server (same WiFi network)

### Software
1. **Android Studio** - Latest version (recommended: 2022.3+)
2. **JDK 11+** - Java Development Kit
3. **Node.js** - v14+ (for backend server)

---

## Architecture Overview

### Android App Structure

```
AccelerometerApp/
├── app/
│   ├── build.gradle                 # Dependencies & build config
│   ├── src/main/
│   │   ├── AndroidManifest.xml      # Permissions & app config
│   │   ├── java/com/robotdash/accelerometer/
│   │   │   ├── MainActivity.java    # UI & main orchestrator
│   │   │   ├── bluetooth/
│   │   │   │   └── BluetoothConnectionManager.java  # Bluetooth handling
│   │   │   ├── data/
│   │   │   │   ├── AccelerometerReading.java        # Data model
│   │   │   │   └── AccelerometerDataParser.java     # Data parsing logic
│   │   │   └── network/
│   │   │       └── HttpServerClient.java            # HTTP client
│   │   └── res/
│   │       ├── layout/activity_main.xml    # UI layout
│   │       ├── drawable/                   # Drawable resources
│   │       └── values/
│   │           ├── colors.xml
│   │           ├── strings.xml
│   │           └── styles.xml
│   └── proguard-rules.pro
├── settings.gradle
└── build.gradle
```

### Data Flow

```
Arduino (Accelerometer)
       ↓
   HC-05 Bluetooth Module
       ↓
   Android App (BluetoothConnectionManager)
       ↓
   AccelerometerDataParser
       ↓
   MainActivity (UI Update)
       ↓
   HttpServerClient
       ↓
   Node.js Server (HTTP POST)
```

---

## Part 1: Android App Setup

### Step 1.1: Clone/Download the Project

```bash
# Copy the AccelerometerApp folder to your workspace
cd /path/to/workspace
```

### Step 1.2: Open in Android Studio

1. Open **Android Studio**
2. Click **File** → **Open**
3. Navigate to `AccelerometerApp` folder
4. Click **OK**
5. Wait for Gradle sync to complete

### Step 1.3: Configure Build Settings

The `build.gradle` file includes all necessary dependencies:
- **AndroidX** - Compatibility library
- **OkHttp 4.11.0** - HTTP client for networking
- **Material Design** - UI components

No additional configuration needed.

### Step 1.4: Connect Android Device

1. **Enable Developer Mode:**
   - Go to **Settings** → **About Phone**
   - Tap **Build Number** 7 times
   - Go back to **Settings** → **Developer Options**
   - Enable **USB Debugging**

2. **Connect via USB:**
   - Connect Android device to computer via USB cable
   - Allow the device to install USB driver
   - In Android Studio, you should see your device in the device list

### Step 1.5: Configure Server URL

Before building, configure your Node.js server URL:

**In MainActivity.java** (Line ~42):
```java
private static final String DEFAULT_SERVER_URL = "http://192.168.1.100:3000/data";
```

Replace `192.168.1.100` with your laptop's IP address (same WiFi network).

**To find your laptop's IP:**
- **Windows**: Open Command Prompt, run `ipconfig`, look for IPv4 Address
- **Mac/Linux**: Open Terminal, run `hostname -I` or `ifconfig`

### Step 1.6: Build & Deploy

1. In Android Studio, click **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for build to complete
3. Android Studio will show **Build successful**
4. Connect your device and click **Run** → **Run 'app'**
5. Select your device and click **OK**

The app will install and launch automatically.

---

## Part 2: HC-05 Bluetooth Setup

### Step 2.1: Hardware Connections

**HC-05 Module Pins:**
```
HC-05 PIN  →  Arduino PIN
VCC        →  5V
GND        →  GND
TX         →  RX (Serial1 on Mega, or SoftwareSerial)
RX         →  TX
```

### Step 2.2: Arduino Code Example

```cpp
#include <Wire.h>
#include <MPU6050.h>  // Or your accelerometer library

MPU6050 mpu;
HardwareSerial &btSerial = Serial1;  // Use Serial1 for Mega, or SoftwareSerial

void setup() {
  btSerial.begin(9600);  // HC-05 baud rate
  mpu.initialize();
  if (!mpu.testConnection()) {
    while(1);  // Halt
  }
}

void loop() {
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);
  
  // Convert to m/s² (assuming ±8g range)
  float accelX = ax / 4096.0 * 9.81;
  float accelY = ay / 4096.0 * 9.81;
  float accelZ = az / 4096.0 * 9.81;
  
  // Send data
  btSerial.print(accelX);
  btSerial.print(",");
  btSerial.print(accelY);
  btSerial.print(",");
  btSerial.println(accelZ);
  
  delay(100);  // 10 Hz sampling
}
```

### Step 2.3: Pair HC-05 with Android Device

1. **Enable Bluetooth on Android device** (Settings → Bluetooth)
2. **Put HC-05 into pairing mode** (hold button for 3-5 seconds, LED blinks slowly)
3. **Scan for devices** on Android Bluetooth settings
4. **Select HC-05** when it appears (usually labeled "HC-05" or "linvor")
5. **PIN**: `1234` (standard HC-05 PIN)
6. Once paired, the connection icon appears in status bar

---

## Part 3: Node.js Backend Server

### Step 3.1: Create Node.js Server

Create a file `server.js`:

```javascript
const express = require('express');
const app = express();
const PORT = 3000;

// Middleware
app.use(express.json());

// Store recent readings
const readings = [];
const MAX_READINGS = 1000;

// HTTP Endpoint to receive data
app.post('/data', (req, res) => {
  const { x, y, z, timestamp } = req.body;

  if (typeof x === 'number' && typeof y === 'number' && typeof z === 'number') {
    const reading = {
      x: parseFloat(x.toFixed(4)),
      y: parseFloat(y.toFixed(4)),
      z: parseFloat(z.toFixed(4)),
      timestamp: timestamp || Date.now(),
      receivedAt: new Date().toISOString()
    };

    readings.push(reading);

    // Keep only recent readings
    if (readings.length > MAX_READINGS) {
      readings.shift();
    }

    console.log(`[${reading.receivedAt}] X: ${reading.x}, Y: ${reading.y}, Z: ${reading.z}`);
    res.json({ success: true, message: 'Data received' });
  } else {
    res.status(400).json({ success: false, message: 'Invalid data format' });
  }
});

// Status endpoint
app.get('/status', (req, res) => {
  res.json({
    status: 'running',
    serverTime: new Date().toISOString(),
    totalReadings: readings.length,
    lastReading: readings[readings.length - 1] || null
  });
});

// Get recent readings
app.get('/readings', (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 100, readings.length);
  res.json(readings.slice(-limit));
});

// Start server
app.listen(PORT, () => {
  console.log(`Server listening on http://0.0.0.0:${PORT}`);
  console.log(`Status: http://localhost:${PORT}/status`);
});
```

### Step 3.2: Install Dependencies

```bash
cd /path/to/server
npm init -y
npm install express
```

### Step 3.3: Start the Server

```bash
node server.js
```

**Expected output:**
```
Server listening on http://0.0.0.0:3000
Status: http://localhost:3000/status
```

### Step 3.4: Verify Server is Running

Open browser and navigate to:
```
http://127.0.0.1:3000/status
```

You should see:
```json
{
  "status": "running",
  "serverTime": "2024-03-31T12:00:00.000Z",
  "totalReadings": 0,
  "lastReading": null
}
```

---

## Part 4: Running the Complete System

### Prerequisites Checklist

- [ ] Arduino running with accelerometer and HC-05 connected
- [ ] HC-05 paired with Android device
- [ ] Android device on same WiFi network as laptop
- [ ] Node.js server running on laptop
- [ ] Server URL configured in Android app (MainActivity.java)
- [ ] All permissions granted to Android app

### Execution Steps

1. **Start Node.js Server:**
   ```bash
   node server.js
   ```

2. **Launch Android App:**
   - Open app on device
   - You should see status: **"Disconnected"** (red)

3. **Connect to HC-05:**
   - App automatically shows paired devices
   - Select HC-05 from the list
   - Click **"Connect"** button
   - Wait 2-3 seconds for connection
   - Status should change to: **"Connected to HC-05"** (green)

4. **Monitor Data:**
   - Real-time X, Y, Z values update on screen
   - Check server logs to see incoming data
   - Monitor with: `http://laptop-ip:3000/status`

---

## Part 5: Troubleshooting

### Issue: App Cannot Find HC-05

**Solutions:**
1. Ensure HC-05 is powered and LED is blinking
2. Verify HC-05 is paired in Android Bluetooth settings
3. Check baud rate: HC-05 default is 9600
4. Clear app cache: Settings → Apps → Accelerometer Monitor → Storage → Clear Cache

### Issue: Connection Established but No Data

**Solutions:**
1. Verify Arduino is sending data in correct format: `x,y,z\n`
2. Check HC-05 TX/RX connections to Arduino
3. Monitor Arduino serial output before connecting to app
4. Increase data sending frequency on Arduino (reduce `delay()` value)

### Issue: Data Not Reaching Server

**Solutions:**
1. Verify server URL in app matches your laptop IP
2. Check firewall settings - allow port 3000
3. Verify both devices on same WiFi network
4. Monitor app logs: Android Studio → Logcat (search for "HttpServerClient")
5. Test connectivity: Ping your laptop from Android device

### Issue: Data Values Invalid or Parsing Errors

**Solutions:**
1. Check Arduino data format - must be exactly: `x,y,z\n`
2. Verify accelerometer range (±8g, ±16g, etc.)
3. Monitor app logs for parse errors: "Failed to parse accelerometer data"
4. Increase buffer in AccelerometerDataParser if receiving large packets

### Issue: Connection Drops Randomly

**Solutions:**
1. Increase Bluetooth signal range - move closer or remove obstacles
2. Check for electromagnetic interference
3. Verify android device Bluetooth is stable
4. Reduce data sending frequency on Arduino
5. Clear paired devices and re-pair if connection is unstable

---

## Part 6: Performance Tuning

### Adjust Data Send Frequency

In **MainActivity.java** (Line ~65):
```java
httpClient.setSendFrequencyHz(10); // Change from 10 to desired Hz
```

**Recommended values:**
- 5 Hz: Low bandwidth, good for WiFi with latency
- 10 Hz: Standard, recommended
- 20 Hz: High frequency, requires stable WiFi

### Optimize Buffer Size

In **BluetoothConnectionManager.java** (Line ~28):
```java
private static final int READ_BUFFER_SIZE = 1024; // Adjust buffer
```

**Larger buffer = better throughput, higher latency**

### Modify Acceleration Range Validation

In **AccelerometerDataParser.java** (Line ~91):
```java
return value >= -100.0f && value <= 100.0f; // Adjust range
```

---

## Part 7: Building for Production

### Signing the APK

1. In Android Studio: **Build** → **Generate Signed Bundle / APK**
2. Click **APK**
3. Select or create a keystore
4. Fill in key information
5. Select **release** build type
6. Android Studio creates a release APK

### Using the APK

1. Move APK to Android device
2. Open file manager
3. Tap the APK file to install
4. Grant necessary permissions
5. Launch app from app drawer

---

## Summary

**What You Have:**

✅ **Complete Android App** with:
- Bluetooth Classic connection to HC-05
- Real-time data parsing
- HTTP client for server communication
- Material Design UI
- Proper permission handling

✅ **Bluetooth Connection Manager** with:
- RFCOMM socket with correct UUID
- Background read thread
- Error handling & reconnection logic
- Data buffering

✅ **Data Parser** with:
- Safe line-based parsing
- Partial packet handling
- Value validation
- Null/error safety

✅ **HTTP Client** with:
- OkHttp for reliable networking
- Rate limiting (10 Hz)
- Background execution
- Timeout handling

✅ **Node.js Backend** with:
- Express server
- JSON endpoint for data reception
- Status monitoring endpoint
- Reading retrieval endpoint

---

## Next Steps

1. **Deploy the Android app** to your device
2. **Run the Node.js server** on your laptop
3. **Test the complete system** with Arduino + HC-05
4. **Monitor logs** for any issues
5. **Adjust configuration** as needed

**For questions or issues, check the troubleshooting section above.**

Good luck! 🚀
