# Accelerometer Monitor - Android Bluetooth Application

A complete, production-ready Android application that connects to an **HC-05 Bluetooth Classic module**, reads accelerometer data in real-time, and forwards it to a Node.js backend server via HTTP POST requests.

## ✨ Features

- **Bluetooth Classic (SPP)** - Direct connection to HC-05 using standard RFCOMM UUID
- **Real-time Data Streaming** - Continuous accelerometer reading up to 10+ Hz
- **Safe Data Parsing** - Handles partial packets, malformed data, and buffer management
- **HTTP Forwarding** - Posts parsed data to Node.js backend
- **Rate Limiting** - Configurable sending frequency (default 10 Hz)
- **Material Design UI** - Clean, intuitive interface
- **Background Threading** - Non-blocking Bluetooth operations
- **Android 6.0+** - Full compatibility with modern Android versions
- **Runtime Permissions** - Proper handling of Android 12+ permission requirements

## 🏗️ Project Structure

```
AccelerometerApp/
├── app/
│   ├── build.gradle
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/robotdash/accelerometer/
│   │   │   ├── MainActivity.java
│   │   │   ├── bluetooth/BluetoothConnectionManager.java
│   │   │   ├── data/AccelerometerDataParser.java
│   │   │   ├── data/AccelerometerReading.java
│   │   │   └── network/HttpServerClient.java
│   │   └── res/
│   │       ├── layout/activity_main.xml
│   │       ├── drawable/
│   │       └── values/
│   └── proguard-rules.pro
├── SETUP_GUIDE.md
├── README.md
├── server.js (Node.js backend)
├── package.json
└── arduino_example.ino
```

## 🚀 Quick Start

### Prerequisites
- Android Studio 2022.3+
- Android device (API 21+)
- Node.js 14+
- HC-05 Bluetooth module
- Arduino with accelerometer

### Setup Steps

1. **Clone/Download the project**
   ```bash
   cd AccelerometerApp
   ```

2. **Open in Android Studio and build**
   ```
   File → Open → Select AccelerometerApp folder
   Build → Build Bundle(s) / APK(s)
   ```

3. **Install and run on Android device**
   ```
   Run → Run 'app'
   ```

4. **Start Node.js server**
   ```bash
   npm install
   node server.js
   ```

5. **Connect to HC-05 and start streaming**
   - Select device from list
   - Click "Connect"
   - Real-time data appears on screen

## 📱 Android Components

### MainActivity
- UI orchestration
- Permission handling
- Device selection
- Real-time data display
- Server URL configuration

### BluetoothConnectionManager
- RFCOMM socket management
- Background read thread
- Connection/disconnection logic
- Error handling and callbacks

### AccelerometerDataParser
- Line-based data parsing (delimiter: `\n`)
- Format: `x,y,z` (float values)
- Partial packet handling
- Value validation (range: -100 to +100 m/s²)

### HttpServerClient
- OkHttp-based HTTP client
- JSON payload transmission
- Rate limiting (configurable Hz)
- Non-blocking async sending
- Timeout handling

## 🔌 Bluetooth Protocol

- **Module**: HC-05 (Bluetooth Classic, not BLE)
- **Connection Method**: RFCOMM Socket
- **UUID**: `00001101-0000-1000-8000-00805F9B34FB`
- **Baud Rate**: 9600 (standard HC-05)
- **Data Format**: `x,y,z\n`
  - Example: `0.12,-0.03,9.81\n`
  - Values are floats in m/s²

## 📡 HTTP API

### POST /data
Receives accelerometer data from Android app.

**Request Body:**
```json
{
  "x": 0.1234,
  "y": -0.0567,
  "z": 9.8100,
  "timestamp": 1711881234567
}
```

**Response:**
```json
{
  "success": true,
  "message": "Data received",
  "id": 1
}
```

## 🔧 Configuration

### Update Server URL
Edit `MainActivity.java` line 42:
```java
private static final String DEFAULT_SERVER_URL = "http://192.168.1.100:3000/data";
```

Replace IP with your laptop's IP address.

### Adjust Send Frequency
In `MainActivity.java` line 65:
```java
httpClient.setSendFrequencyHz(10); // Change to desired frequency
```

### Buffer Size
In `BluetoothConnectionManager.java` line 28:
```java
private static final int READ_BUFFER_SIZE = 1024;
```

## 📋 Permissions

- `BLUETOOTH` - Access to Bluetooth
- `BLUETOOTH_ADMIN` - Bluetooth device management
- `BLUETOOTH_CONNECT` - Android 12+ Bluetooth connection
- `BLUETOOTH_SCAN` - Android 12+ Bluetooth scanning
- `INTERNET` - HTTP requests

## 🛠️ Arduino Integration

The app expects data in format: `x,y,z\n` at 9600 baud.

See `arduino_example.ino` for a complete example with MPU6050.

## 📊 Node.js Backend

Included `server.js` provides:
- **POST /data** - Receive accelerometer data
- **GET /status** - Server status
- **GET /readings** - Get recent readings
- **GET /stats** - Statistical analysis
- **GET /health** - Health check

Start with:
```bash
npm install
node server.js
```

## 🐛 Troubleshooting

### Connection Issues
- Verify HC-05 is powered and paired
- Check baud rate is 9600
- Clear app cache and retry

### No Data Received
- Confirm Arduino sends data in correct format
- Verify HC-05 connections
- Check server URL is correct

### Server Unreachable
- Ensure both devices on same WiFi
- Check firewall allows port 3000
- Ping your laptop from Android device

See `SETUP_GUIDE.md` for detailed troubleshooting.

## 📄 License

MIT

## 📞 Support

For issues, check the SETUP_GUIDE.md comprehensive troubleshooting section.

---

**Built with:**
- Android SDK
- Java 11
- OkHttp 4.11
- Express.js
- Material Design
