package com.robotdash.accelerometer.data;

import android.util.Log;

/**
 * Parses accelerometer data from the HC-05 Bluetooth stream.
 * Expected format: "x,y,z\n"
 * Example: "0.12,-0.03,9.81\n"
 */
public class AccelerometerDataParser {
    private static final String TAG = "AccelParser";
    private StringBuilder buffer = new StringBuilder();

    /**
     * Add data to the parsing buffer.
     * Returns a complete AccelerometerReading when a full line is received.
     * 
     * @param data Raw data received from Bluetooth
     * @return AccelerometerReading if a complete line with valid data is found, null otherwise
     */
    public AccelerometerReading processData(byte[] data) {
        try {
            // Append received data to buffer
            for (byte b : data) {
                buffer.append((char) b);

                // Check if we have a complete line
                if (b == '\n') {
                    String line = buffer.toString().trim();
                    buffer.setLength(0); // Clear buffer

                    if (!line.isEmpty()) {
                        AccelerometerReading reading = parseLineData(line);
                        if (reading != null) {
                            Log.d(TAG, "Parsed: " + reading);
                            return reading;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing data", e);
            buffer.setLength(0); // Clear on error
        }

        return null;
    }

    /**
     * Parse a single line of accelerometer data.
     * Format: "x,y,z" where x, y, z are floats
     * 
     * @param line A trimmed line of data
     * @return AccelerometerReading or null if parsing fails
     */
    private AccelerometerReading parseLineData(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length != 3) {
                Log.w(TAG, "Invalid data format (expected 3 values, got " + parts.length + "): " + line);
                return null;
            }

            float x = Float.parseFloat(parts[0].trim());
            float y = Float.parseFloat(parts[1].trim());
            float z = Float.parseFloat(parts[2].trim());

            // Validate values are in reasonable range for accelerometers
            if (!isValidAccelValue(x) || !isValidAccelValue(y) || !isValidAccelValue(z)) {
                Log.w(TAG, "Invalid acceleration values: x=" + x + ", y=" + y + ", z=" + z);
                return null;
            }

            return new AccelerometerReading(x, y, z, System.currentTimeMillis());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse accelerometer data: " + line, e);
            return null;
        }
    }

    /**
     * Validate that an acceleration value is within reasonable bounds.
     * Typical accelerometers range from -100 to +100 m/s²
     */
    private boolean isValidAccelValue(float value) {
        return value >= -100.0f && value <= 100.0f;
    }

    /**
     * Get any remaining buffered data
     */
    public String getBufferContent() {
        return buffer.toString();
    }

    /**
     * Clear the buffer (useful on disconnect)
     */
    public void clearBuffer() {
        buffer.setLength(0);
    }
}
