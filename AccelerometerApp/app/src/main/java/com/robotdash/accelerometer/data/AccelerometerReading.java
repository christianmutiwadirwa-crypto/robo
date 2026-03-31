package com.robotdash.accelerometer.data;

/**
 * Represents a single accelerometer reading from the HC-05 Bluetooth module.
 */
public class AccelerometerReading {
    public float x;
    public float y;
    public float z;
    public long timestamp;

    public AccelerometerReading(float x, float y, float z, long timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("AccelerometerReading{x=%.2f, y=%.2f, z=%.2f, ts=%d}", 
            x, y, z, timestamp);
    }

    /**
     * Convert to JSON string for HTTP transmission
     */
    public String toJson() {
        return String.format("{\"x\":%.4f,\"y\":%.4f,\"z\":%.4f,\"timestamp\":%d}", 
            x, y, z, timestamp);
    }
}
