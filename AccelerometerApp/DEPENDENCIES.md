# Dependencies & Resources Reference

## Android Dependencies

### Build Tools & Libraries

```gradle
// From app/build.gradle

compileSdk 33
targetSdk 33

dependencies {
    // AndroidX (Core compatibility layers)
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.android.material:material:1.9.0'

    // Networking (HTTP Client)
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}

// Min SDK: 21 (Android 5.0)
// Target SDK: 33 (Android 13)
```

### What Each Library Does

| Library | Purpose | Version | Link |
|---------|---------|---------|------|
| **appcompat** | Backward compatibility for UI | 1.6.1 | [GitHub](https://github.com/androidx/appcompat) |
| **constraintlayout** | Flexible UI layout system | 2.1.4 | [GitHub](https://github.com/androidx/constraintlayout) |
| **material** | Material Design components | 1.9.0 | [GitHub](https://github.com/material-components/material-components-android) |
| **okhttp3** | HTTP client for networking | 4.11.0 | [GitHub](https://github.com/square/okhttp) |
| **junit** | Unit testing framework | 4.13.2 | [JUnit](https://junit.org/) |

### Why These Libraries?

- **AndroidX**: Official support library for modern Android
- **Material Design**: Consistent, modern UI components
- **OkHttp**: Industry-standard HTTP client with great defaults
- **JUnit**: Standard for Android unit testing

---

## Node.js Dependencies

### Backend Server

```json
{
  "name": "accelerometer-server",
  "dependencies": {
    "express": "^4.18.2"
  }
}
```

### Install & Run

```bash
npm install
node server.js
```

### What Express Does
- HTTP server framework
- Route handling (POST, GET)
- Request/response middleware
- JSON parsing

---

## Arduino Libraries

### Required Libraries (Install via Arduino IDE)

1. **MPU6050** by Electronic Cats
   - Purpose: Accelerometer sensor control
   - Installation: Sketch → Include Library → Manage Libraries
   - Search: "MPU6050"
   - Version: 1.0.1+

2. **Wire** (Built-in)
   - Purpose: I2C communication
   - No installation needed (built-in)

3. **Serial** (Built-in)
   - Purpose: UART communication
   - No installation needed (built-in)

### Manual Library Installation

If automatic installation fails:

1. Download MPU6050 library from GitHub
2. Extract to `Arduino/libraries/`
3. Restart Arduino IDE

---

## Bluetooth Module Specification

### HC-05 Specifications

```
Bluetooth Standard: Bluetooth Classic (v2.1)
Protocol Profile: SPP (Serial Port Profile)
Default Baud Rate: 9600
Default PIN: 1234
UUID: 00001101-0000-1000-8000-00805F9B34FB
Range: ~10-100 meters
Power: 3.3V, ~35mA

Configuration Commands:
AT               - Test
AT+NAME=HC05     - Set name
AT+PSWD=1234     - Set PIN
AT+UART=9600,0,0 - Set baud rate
```

### Wiring Diagram

```
HC-05 Module      Arduino/Power
├─ VCC      →     5V (or 3.3V)
├─ GND      →     GND
├─ TX       →     RX1 (Pin 19 on Mega) or RX (Pin 0 on Uno)
├─ RX       →     TX1 (Pin 18 on Mega) or TX (Pin 1 on Uno)
│              [via 1:2 voltage divider for 5V→3.3V]
└─ EN (opt) →     5V

Voltage Divider for HC-05 RX (if using 5V Arduino):
Arduino TX → 10kΩ resistor → HC-05 RX
           ↓
           20kΩ resistor → GND
```

---

## Accelerometer Sensor Specification

### MPU6050 Specifications

```
I2C Address: 0x68 (or 0x69 with AD0 high)
Acceleration Range: ±2, ±4, ±8, ±16 g
Full-Scale Range: -32768 to +32767 LSBs
Scale Factor: 1/4096 LSBs per g (for ±8g range)
Sampling Rate: 8 kHz
I2C Clock Speed: 100-400 kHz

Included in Arduino Example:
- Range: ±8g
- Scale: 1/4096
- Sampling: 10 Hz (100ms)
```

### Data Format

```
Raw Acceleration: int16_t (ax, ay, az)
Conversion to m/s²:
  accel_x = ax * (1/4096) * 8 * 9.81
  accel_y = ay * (1/4096) * 8 * 9.81
  accel_z = az * (1/4096) * 8 * 9.81
```

---

## API Specifications

### HTTP Endpoints

#### POST /data
Receive accelerometer data

**Request:**
```
Content-Type: application/json
{
  "x": number (float),
  "y": number (float),
  "z": number (float),
  "timestamp": number (milliseconds)
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Data received",
  "id": number
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Invalid data format..."
}
```

#### GET /status
Get server status

**Response:**
```json
{
  "status": "running",
  "serverTime": "2024-03-31T12:00:00.000Z",
  "totalReadings": 1234,
  "totalReceived": 1234,
  "lastReading": {
    "x": 0.12,
    "y": -0.03,
    "z": 9.81,
    "timestamp": 1711881234567,
    "receivedAt": "2024-03-31T12:00:00.000Z"
  },
  "uptime": 3600
}
```

#### GET /readings?limit=100
Get recent readings

**Response:**
```json
[
  {
    "x": 0.12,
    "y": -0.03,
    "z": 9.81,
    "timestamp": 1711881234567,
    "receivedAt": "2024-03-31T12:00:00.000Z"
  },
  ...
]
```

#### GET /stats
Get statistical analysis

**Response:**
```json
{
  "x": {
    "avg": 0.05,
    "min": -5.2,
    "max": 5.8,
    "std": 0.3
  },
  "y": { ... },
  "z": { ... },
  "samples": 1234
}
```

---

## System Requirements

### Minimum Requirements

**Android Device:**
- Android 6.0+ (API 21+)
- 100 MB free storage
- Bluetooth support (Bluetooth 2.1+)
- 512 MB RAM minimum

**Backend Server:**
- Node.js 14.0+
- 50 MB disk space
- 256 MB RAM available
- Network connectivity

**Arduino:**
- Arduino Mega 2560 (or Uno with SoftwareSerial)
- 10 KB Flash memory for sketch
- I2C and UART support

### Recommended Requirements

**Android Device:**
- Android 10+ (API 29+)
- 200+ MB free storage
- 2+ GB RAM
- WiFi + Bluetooth

**Backend Server:**
- Node.js 16.0+
- 500 MB disk space
- 1+ GB RAM
- Dedicated power source

**Arduino:**
- Arduino Mega 2560 (preferred)
- 128 KB Flash memory
- Stable 5V power supply

---

## Build Tools & Versions

```
Android SDK Target: 33 (Android 13)
Android SDK Min: 21 (Android 5.0)
Build Tools: 33.0.2
Gradle Plugin: 7.4.2
Java Target: 11
Kotlin Not Used (Pure Java)
```

---

## Testing Tools & Frameworks

### Android Testing
- **JUnit 4** - Unit testing framework
- **Espresso** - UI testing framework
- **Logcat** - Android debugging tool

### Integration Testing
- **curl** - HTTP client testing
- **Device logs** - Bluetooth debugging
- **Android Monitor** - Performance monitoring

---

## Performance Characteristics

### OkHttp HTTP Client
- **Connection Pool**: Up to 5 idle connections
- **Timeout**: 5 seconds (configurable)
- **Retry Policy**: Automatic on network errors
- **Compression**: Gzip compression available
- **Throughput**: Handles HTTP/1.1 efficiently

### MPU6050 Sensor
- **I2C Speed**: 400 kHz typical
- **Data Rate**: 8 kHz internal
- **Sample Rate**: Configurable (10 Hz in example)
- **Accuracy**: ±0.5% typical

### Bluetooth SPP
- **Baud Rate**: 9600 bits/sec
- **Throughput**: ~960 bytes/sec maximum
- **Latency**: 10-100 ms typical
- **Range**: 10-100 meters (line of sight)

---

## External Resources

### Official Documentation Links

| Resource | URL |
|----------|-----|
| Android Developers | https://developer.android.com/ |
| AndroidX Libraries | https://developer.android.com/jetpack/androidx |
| OkHttp Documentation | https://square.github.io/okhttp/ |
| Express.js Guide | https://expressjs.com/ |
| Node.js Docs | https://nodejs.org/docs/ |
| Arduino Reference | https://www.arduino.cc/reference/en/ |
| MPU6050 Datasheet | [PDF](https://www.invensense.com) |
| HC-05 Datasheet | [PDF](https://components101.com) |

### Useful Tools

- **Android Studio**: IDE for Android development
- **VSCode**: Code editor for Node.js
- **Arduino IDE**: Firmware programming
- **curl**: Command-line HTTP client
- **Postman**: API testing tool (alternative to curl)

---

## Version History

| Version | Date | Android | Node.js | Status |
|---------|------|---------|---------|--------|
| 1.0.0 | 2024-03-31 | 11+ | 14+ | Current |

---

## License & Attribution

### Open Source Libraries Used

- **Android SDK** - Apache 2.0
- **AndroidX** - Apache 2.0
- **OkHttp** - Apache 2.0
- **Express.js** - MIT
- **Material Design** - Apache 2.0
- **MPU6050 Library** - MIT

### Attribution

All third-party libraries properly attributed in:
- `build.gradle` files
- Code comments
- LICENSE files

### Our Code

MIT License - Free to use, modify, and distribute

---

## Support & Community

### Official Resources
- Android Development: https://developer.android.com/
- Stack Overflow: https://stackoverflow.com/tags/android
- Node.js: https://nodejs.org/
- Arduino Community: https://forum.arduino.cc/

### Getting Help
1. Check documentation files
2. Review code comments
3. Search Stack Overflow
4. Check library documentation
5. Consult troubleshooting guide

---

**Last Updated**: March 31, 2026
**Compatibility**: Android 6.0-13.0, Node.js 14-18+, Arduino Boards
