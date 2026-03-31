# Quick Test Guide

## Testing the Complete System

### 1. Test Node.js Server

```bash
# Start server
cd AccelerometerApp
npm install
node server.js

# In another terminal, test the endpoint
curl -X POST http://localhost:3000/data \
  -H "Content-Type: application/json" \
  -d '{"x":0.12,"y":-0.03,"z":9.81,"timestamp":'$(date +%s%3N)'}'

# Check status
curl http://localhost:3000/status

# Get statistics
curl http://localhost:3000/stats
```

### 2. Test Arduino + HC-05

1. Upload `arduino_example.ino` to Arduino
2. Open Serial Monitor (Tools → Serial Monitor, 115200 baud)
3. Should show: `System ready - sending accelerometer data`
4. Verify HC-05 sends data: Use HC-05 terminal app to test connection

### 3. Test Android App

1. Build and install app on device
2. Open app settings, verify server URL matches laptop IP
3. Pair HC-05 in Android Bluetooth settings
4. In app: Select HC-05 → Click Connect
5. Watch for:
   - Status changes to "Connected"
   - X, Y, Z values update in real-time
   - Last update timestamp updates

### 4. Full System Test

1. Start Node.js server
2. Connect Android to HC-05
3. Monitor server logs for incoming data
4. Check `http://laptop-ip:3000/status`

### 5. Stress Test

```bash
# Generate test data for 10 seconds
for i in {1..100}; do
  curl -X POST http://localhost:3000/data \
    -H "Content-Type: application/json" \
    -d '{"x":'$((RANDOM % 100 - 50))'.5,"y":'$((RANDOM % 100 - 50))'.5,"z":9.81}'
  sleep 0.1
done

# Check total received
curl http://localhost:3000/status
```

## Common Issues During Testing

| Issue | Solution |
|-------|----------|
| "Connection refused" | Ensure Node.js server is running |
| No data in server logs | Check Android app server URL configuration |
| APP crashes on connect | Grant all Bluetooth permissions |
| HC-05 not found | Pair device first in Bluetooth settings |
| Invalid data format error | Verify Arduino sends data as `x,y,z\n` |

## Performance Metrics

**Expected Performance:**
- Bluetooth connection: 1-3 seconds
- Data parsing: < 1ms per reading
- HTTP transmission: 50-200ms round trip
- UI update latency: < 100ms
- Memory usage: ~50-100 MB

## Data Validation Examples

✅ **Valid Data:**
```
0.12,-0.03,9.81
-5.5,6.3,9.5
0,0,9.81
-99.99,99.99,-50.5
```

❌ **Invalid Data:**
```
0.12;-0.03;9.81      (wrong delimiter)
0.12,-0.03           (missing value)
0.12,-0.03,9.81,123  (extra value)
abc,def,ghi          (not numbers)
0.12,-0.03,150       (out of range)
```
