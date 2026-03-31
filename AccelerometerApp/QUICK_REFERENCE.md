# Quick Reference Card

## 🔗 Bluetooth Connection Flow

```
HC-05 (9600 baud) 
  ↓ RFCOMM Socket
  ↓ UUID: 00001101-0000-1000-8000-00805F9B34FB
  ↓
Android App
  ├─ BluetoothAdapter.getBondedDevices()
  ├─ device.createRfcommSocketToServiceRecord(UUID)
  ├─ socket.connect()
  ├─ inputStream = socket.getInputStream()
  └─ background thread: inputStream.read(buffer)
```

## 📡 Data Format

**Input (from Arduino via HC-05):**
```
0.12,-0.03,9.81\n
x,y,z\n
```

**Processing:**
1. Buffer bytes until newline
2. Extract string: "0.12,-0.03,9.81"
3. Split by comma: ["0.12", "-0.03", "9.81"]
4. Parse as floats: [0.12f, -0.03f, 9.81f]
5. Validate: Each in range [-100, 100] ✓
6. Create timestamp: System.currentTimeMillis()

**Output (to server):**
```json
{
  "x": 0.12,
  "y": -0.03,
  "z": 9.81,
  "timestamp": 1711881234567
}
```

## 🔧 Key Configuration Parameters

| Parameter | File | Line | Default | Purpose |
|-----------|------|------|---------|---------|
| Server URL | MainActivity.java | 42 | http://192.168.1.100:3000/data | Backend endpoint |
| Send Frequency | MainActivity.java | 65 | 10 Hz | Rate limiting |
| Bluetooth Buffer | BluetoothConnectionManager.java | 28 | 1024 bytes | Read buffer size |
| Value Range | AccelerometerDataParser.java | 91 | -100 to +100 m/s² | Validation bounds |
| HTTP Timeout | HttpServerClient.java | 17 | 5000 ms | Request timeout |

## 🎛️ Main Classes

```
MainActivity
├─ BluetoothConnectionManager (manages connection)
├─ AccelerometerDataParser (parses data)
└─ HttpServerClient (sends to server)
```

## 🧵 Threading Model

| Thread | Purpose | Lifecycle |
|--------|---------|-----------|
| Main | UI updates | App lifetime |
| Connection | Connect to HC-05 (blocking) | ~2-3 seconds |
| Read Loop | Continuous Bluetooth reading | Until disconnect |
| Http Post | Send data (async) | On each reading |

## 📋 Critical Methods

### BluetoothConnectionManager
```java
connect(BluetoothDevice)          // NEW thread: blocking connection
disconnect()                      // Stop read thread, close socket
getPairedDevices()                // List bonded devices
sendData(String)                  // Write bytes to HC-05
isConnected()                     // Check connection state
```

### AccelerometerDataParser
```java
processData(byte[] data)           // Buffer & parse incoming bytes → AccelerometerReading
clearBuffer()                      // Reset on disconnect
```

### HttpServerClient
```java
sendDataAsync(AccelerometerReading) // Non-blocking HTTP POST
setSendFrequencyHz(int)             // Rate limiting
setServerUrl(String)                // Update endpoint
```

## ⚠️ Common Mistakes

❌ **Don't:**
- Call `connect()` on main thread (will freeze UI)
- Send HTTP synchronously on main thread
- Parse data without newline delimiter check
- Ignore `BluetoothException` errors
- Update UI from background thread without `runOnUiThread()`

✅ **Do:**
- Call `connect()` from new Thread or AsyncTask
- Send HTTP in background thread
- Buffer all incoming bytes until complete line
- Handle all exceptions gracefully
- Use `runOnUiThread()` for UI updates from callbacks

## 🔍 Debugging Commands

### Android Logcat
```bash
# Monitor all Accelerometer logs
adb logcat | grep -E "AccelParser|BluetoothConnMgr|HttpServerClient"

# View all app logs
adb logcat com.robotdash.accelerometer:*
```

### Node.js Server Testing
```bash
# Test POST endpoint
curl -X POST http://localhost:3000/data \
  -H "Content-Type: application/json" \
  -d '{"x":0.12,"y":-0.03,"z":9.81}'

# Get status
curl http://localhost:3000/status

# Get statistics
curl http://localhost:3000/stats
```

### Arduino Serial Monitor
```
Expected output at 115200 baud:
"System ready - sending accelerometer data"
"Accel: 0.1234 -0.0567 9.8100"
```

## 📊 Expected Log Output

### Successful Connection
```
D/BluetoothConnMgr: Connecting to HC-05...
D/BluetoothConnMgr: Connected to HC-05
D/AccelParser: Parsed: AccelerometerReading{x=0.1234, y=-0.0567, z=9.8100, ts=1711881234567}
D/HttpServerClient: Data sent successfully: {...}
```

### Error Scenarios
```
E/BluetoothConnMgr: Connection failed: Device not found
W/AccelParser: Invalid data format (expected 3 values, got 2): 0.12,-0.03
E/HttpServerClient: Failed to send data to server: Connection timeout
```

## 🛠️ Build & Deploy

```bash
# Build APK
./gradlew assembleRelease          # In app/ directory

# Install on connected device
adb install -r app/build/outputs/apk/release/app-release.apk

# Run directly from Android Studio
# View → Tool Windows → Logcat
```

## 🚨 Emergency Reset

### If app freezes:
1. Kill app: `adb shell am force-stop com.robotdash.accelerometer`
2. Clear data: `adb shell pm clear com.robotdash.accelerometer`
3. Reinstall

### If HC-05 not responding:
1. Power cycle HC-05
2. Check baud rate (should be 9600)
3. Verify TX/RX connections

### If server unreachable:
1. Verify server running: `curl http://laptop-ip:3000/health`
2. Check firewall allows port 3000
3. Verify both devices on same WiFi

## 📈 Performance Metrics

**Latency (ms):**
- Bluetooth connection: 1000-3000
- Data parsing: < 1
- UI update: 16-33 (60 FPS)
- HTTP POST: 50-500

**Throughput:**
- Bluetooth: 9600 baud ≈ 960 bytes/sec
- At 10 Hz: ~50 bytes/message = well within limits
- HTTP POST: < 1KB per message

**Resource Usage:**
- Memory: 60-100 MB
- CPU: Low (mostly sleeping on I/O)
- Battery: ~2-5% drain per hour (heavy streaming)

## 🎯 Configuration Tuning Guide

**For LOW latency:**
- Increase `setSendFrequencyHz()` to 20+
- Reduce buffer size to 256
- Use `http://` over `https://`

**For HIGH reliability:**
- Decrease frequency to 5 Hz
- Increase HTTP timeout to 10000 ms
- Add retry logic

**For LOW power:**
- Decrease frequency to 2-5 Hz
- Increase HTTP timeout (batch requests)
- Use range limits to skip invalid readings

---

## 📞 Quick Support

| Problem | Sol

ution |
|---------|----------|
| HC-05 not found | Pair first in Bluetooth settings |
| No data on screen | Check Arduino serial output |
| Server error 500 | Restart Node.js server |
| App crashes | Check logcat for exception |
| Slow connection | Move closer to HC-05 |

**For detailed help, see SETUP_GUIDE.md Part 5: Troubleshooting**
