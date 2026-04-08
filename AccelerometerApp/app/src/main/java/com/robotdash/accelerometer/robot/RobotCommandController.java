package com.robotdash.accelerometer.robot;

import android.util.Log;
import com.robotdash.accelerometer.bluetooth.BluetoothConnectionManager;

/**
 * Handles robot command sending via Bluetooth
 * Supports basic movement commands: Forward, Backward, Left, Right, Stop
 */
public class RobotCommandController {
    private static final String TAG = "RobotCommandController";
    
    // Command codes (single character for efficiency)
    public static final String CMD_FORWARD = "f";
    public static final String CMD_BACKWARD = "b";
    public static final String CMD_LEFT = "l";
    public static final String CMD_RIGHT = "r";
    public static final String CMD_STOP = "s";
    
    private final BluetoothConnectionManager bluetoothManager;
    private String lastCommand = "";
    private long lastCommandTime = 0;
    private static final long COMMAND_THROTTLE_MS = 50; // Prevent spam (20 Hz max)
    
    public interface CommandCallback {
        void onCommandSent(String command, boolean success);
    }
    
    private CommandCallback callback;
    
    public RobotCommandController(BluetoothConnectionManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
    }
    
    public void setCallback(CommandCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Send a movement command to the robot
     * @param command One of: CMD_FORWARD, CMD_BACKWARD, CMD_LEFT, CMD_RIGHT, CMD_STOP
     * @return true if command was sent, false if failed or throttled
     */
    public boolean sendCommand(String command) {
        // Validate command
        if (!isValidCommand(command)) {
            Log.w(TAG, "Invalid command: " + command);
            return false;
        }
        
        // Apply command throttling to prevent Bluetooth spam
        long now = System.currentTimeMillis();
        if (now - lastCommandTime < COMMAND_THROTTLE_MS && lastCommand.equals(command)) {
            return false; // Command throttled
        }
        
        // Skip if not connected
        if (!bluetoothManager.isConnected()) {
            Log.w(TAG, "Not connected to device");
            notifyCallback(command, false);
            return false;
        }
        
        // Send the command
        boolean success = bluetoothManager.sendData(command);
        
        if (success) {
            lastCommand = command;
            lastCommandTime = now;
            Log.d(TAG, "Command sent: " + command);
        }
        
        notifyCallback(command, success);
        return success;
    }
    
    // Validate if the command is recognized
    private boolean isValidCommand(String command) {
        if (command == null) return false;
        
        // Speed commands have the prefix "SPEED:" followed by 0-9
        if (command.startsWith("SPEED:")) {
            return true;
        }

        return (
            command.equals(CMD_FORWARD) ||
            command.equals(CMD_BACKWARD) ||
            command.equals(CMD_LEFT) ||
            command.equals(CMD_RIGHT) ||
            command.equals(CMD_STOP)
        );
    }
    
    /**
     * Get human-readable command name
     */
    public static String getCommandName(String command) {
        if (command != null && command.startsWith("SPEED:")) {
            return "Speed: " + command.substring(6);
        }
        switch (command) {
            case CMD_FORWARD: return "Forward";
            case CMD_BACKWARD: return "Backward";
            case CMD_LEFT: return "Left";
            case CMD_RIGHT: return "Right";
            case CMD_STOP: return "Stop";
            default: return "Unknown";
        }
    }
    
    /**
     * Notify callback of command status
     */
    private void notifyCallback(String command, boolean success) {
        if (callback != null) {
            callback.onCommandSent(command, success);
        }
    }
    
    /**
     * Emergency stop - sends stop command
     */
    public boolean emergencyStop() {
        lastCommand = ""; // Clear throttle
        return sendCommand(CMD_STOP);
    }
}
