package com.robotdash.accelerometer.network;

import android.util.Log;

import com.robotdash.accelerometer.data.AccelerometerReading;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

/**
 * Handles HTTP communication with the Node.js backend server.
 * Sends accelerometer data via HTTP POST requests.
 */
public class HttpServerClient {
    private static final String TAG = "HttpServerClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int REQUEST_TIMEOUT_MS = 5000;

    private String serverUrl;
    private OkHttpClient httpClient;
    private long lastSentTime = 0;
    private int minIntervalMs = 100; // 10 Hz max by default

    public HttpServerClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(REQUEST_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(REQUEST_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build();
    }

    /**
     * Set the maximum frequency for sending data to the server.
     * 
     * @param frequencyHz Frequency in Hz (e.g., 10 for 10 Hz)
     */
    public void setSendFrequencyHz(int frequencyHz) {
        if (frequencyHz > 0) {
            this.minIntervalMs = 1000 / frequencyHz;
        }
    }

    /**
     * Send accelerometer reading to the server asynchronously.
     * Respects the rate-limiting frequency.
     * 
     * @param reading The accelerometer reading to send
     */
    public void sendDataAsync(AccelerometerReading reading) {
        // Rate limiting: skip if we're sending too fast
        long now = System.currentTimeMillis();
        if (now - lastSentTime < minIntervalMs) {
            return;
        }
        lastSentTime = now;

        // Send in background thread
        new Thread(() -> {
            try {
                sendData(reading);
            } catch (Exception e) {
                Log.e(TAG, "Error sending data", e);
            }
        }).start();
    }

    /**
     * Send accelerometer reading to the server (blocking).
     * 
     * @param reading The accelerometer reading to send
     */
    private void sendData(AccelerometerReading reading) {
        try {
            String jsonBody = reading.toJson();
            RequestBody body = RequestBody.create(jsonBody, JSON);

            Request request = new Request.Builder()
                .url(serverUrl)
                .post(body)
                .build();

            Response response = httpClient.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                Log.w(TAG, "Server returned status code: " + response.code());
            } else {
                Log.d(TAG, "Data sent successfully: " + jsonBody);
            }
            response.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to send data to server", e);
        }
    }

    /**
     * Update the server URL
     */
    public void setServerUrl(String url) {
        this.serverUrl = url;
    }

    /**
     * Get the current server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }
}
