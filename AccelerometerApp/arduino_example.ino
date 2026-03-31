/*
 * Arduino Example: Reading MPU6050 Accelerometer and Sending via HC-05
 * 
 * This sketch reads accelerometer data from an MPU6050 sensor
 * and sends it to the Android app via HC-05 Bluetooth module
 * 
 * Hardware:
 * - Arduino Mega 2560 (or Uno with SoftwareSerial)
 * - MPU6050 Accelerometer
 * - HC-05 Bluetooth Module
 * 
 * Connections:
 * MPU6050:
 *   VCC  → 5V
 *   GND  → GND
 *   SDA  → SDA (Pin 20 on Mega, A4 on Uno)
 *   SCL  → SCL (Pin 21 on Mega, A5 on Uno)
 * 
 * HC-05:
 *   VCC  → 5V (or 3.3V with voltage divider)
 *   GND  → GND
 *   TX   → RX1 (Pin 19 on Mega, Pin 0 on Uno)
 *   RX   → TX1 (Pin 18 on Mega, Pin 1 on Uno) via voltage divider
 * 
 * NOTE: HC-05 RX needs voltage divider (10k + 20k resistors) for 5V to 3.3V
 * 
 * Data Format: "x,y,z\n"
 * Baud Rate: 9600
 * Sampling Rate: 10 Hz (100ms delay)
 */

#include <Wire.h>
#include <MPU6050.h>

// Create MPU6050 object
MPU6050 mpu;

// For Arduino Mega: Serial1 is RX1/TX1 (pins 19/18)
// For Arduino Uno: Use SoftwareSerial
#ifdef __AVR_ATmega2560__
  #define btSerial Serial1  // Mega has built-in Serial1
#else
  #include <SoftwareSerial.h>
  SoftwareSerial btSerial(10, 11); // RX, TX pins for Uno
#endif

// MPU6050 calibration values (may need tuning for your sensor)
const float ACCEL_SCALE = 1.0 / 4096.0;  // For ±8g range
const float GRAVITY = 9.81;
const int SAMPLING_INTERVAL = 100;  // 10 Hz (100ms)
const int ACCEL_RANGE = 8;  // ±8g


void setup() {
  // Initialize serial for debugging
  Serial.begin(115200);
  delay(100);
  
  // Initialize HC-05 Bluetooth serial
  btSerial.begin(9600);  // HC-05 default baud rate
  delay(100);
  
  // Initialize I2C
  Wire.begin();
  delay(100);
  
  // Initialize MPU6050
  if (!mpu.testConnection()) {
    Serial.println("ERROR: MPU6050 not detected!");
    printError("MPU6050 not detected");
    while(1);  // Halt
  }
  
  Serial.println("MPU6050 initialized successfully");
  mpu.setFullScaleAccelRange(MPU6050_ACCEL_FS_8);  // ±8g range
  
  // Small delay to ensure everything is ready
  delay(500);
  
  Serial.println("System ready - sending accelerometer data");
  sendData("System initialized\n");
}


void loop() {
  // Read raw accelerometer values
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);
  
  // Convert to m/s² (assuming ±8g range)
  // Each LSB = 1/4096 g for ±8g range
  // 1 g = 9.81 m/s²
  float accelX = ax * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  float accelY = ay * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  float accelZ = az * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  
  // Limit precision to 4 decimal places
  accelX = round(accelX * 10000) / 10000.0;
  accelY = round(accelY * 10000) / 10000.0;
  accelZ = round(accelZ * 10000) / 10000.0;
  
  // Format and send data: "x,y,z\n"
  char buffer[50];
  sprintf(buffer, "%.4f,%.4f,%.4f", accelX, accelY, accelZ);
  sendData(buffer);
  
  // Also print to serial for debugging
  Serial.print("Accel: ");
  Serial.print(accelX);
  Serial.print(" ");
  Serial.print(accelY);
  Serial.print(" ");
  Serial.println(accelZ);
  
  // Wait for next sample (maintain consistent 10 Hz rate)
  delay(SAMPLING_INTERVAL);
}


/**
 * Send data to Bluetooth module
 * Automatically adds newline character
 */
void sendData(const char *data) {
  btSerial.print(data);
  btSerial.println();  // Add newline delimiter
  btSerial.flush();
}


/**
 * Send error message to Bluetooth
 */
void printError(const char *error) {
  btSerial.print("ERROR: ");
  btSerial.println(error);
  btSerial.flush();
}


/**
 * Alternative function to send raw acceleration values
 * with timestamp for additional accuracy
 */
void sendDataWithTimestamp() {
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);
  
  float accelX = ax * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  float accelY = ay * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  float accelZ = az * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  
  accelX = round(accelX * 10000) / 10000.0;
  accelY = round(accelY * 10000) / 10000.0;
  accelZ = round(accelZ * 10000) / 10000.0;
  
  unsigned long timestamp = millis();
  
  char buffer[60];
  sprintf(buffer, "%.4f,%.4f,%.4f,%lu", accelX, accelY, accelZ, timestamp);
  sendData(buffer);
}


/*
 * ADVANCED: Temperature-compensated reading
 * Uncomment to use instead of loop()
 * 
 * Some accelerometers provide temperature readings
 * This can help improve accuracy over time
 */
/*
void loop_with_temperature() {
  int16_t ax, ay, az, temp;
  mpu.getMotion6(&ax, &ay, &az, &temp);
  
  float accelX = ax * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  float accelY = ay * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  float accelZ = az * ACCEL_SCALE * ACCEL_RANGE * GRAVITY;
  float temperature = (temp / 340.0) + 36.53;  // Convert to Celsius
  
  accelX = round(accelX * 10000) / 10000.0;
  accelY = round(accelY * 10000) / 10000.0;
  accelZ = round(accelZ * 10000) / 10000.0;
  temperature = round(temperature * 100) / 100.0;
  
  char buffer[70];
  sprintf(buffer, "%.4f,%.4f,%.4f,%.2f", accelX, accelY, accelZ, temperature);
  sendData(buffer);
  
  Serial.println(buffer);
  delay(SAMPLING_INTERVAL);
}
*/


/*
 * SETUP INSTRUCTIONS:
 * 
 * 1. Install MPU6050 Library:
 *    Sketch → Include Library → Manage Libraries
 *    Search for "MPU6050" by Electronic Cats
 *    Click Install
 * 
 * 2. Configure I2C Address (if needed):
 *    Some MPU6050 modules have jumper to select address
 *    Default: 0x68
 *    With AD0 pulled high: 0x69
 * 
 * 3. Upload this sketch to Arduino
 * 
 * 4. Open Serial Monitor to verify:
 *    Tools → Serial Monitor
 *    Should see: "MPU6050 initialized successfully"
 * 
 * 5. Connect to HC-05:
 *    Pair HC-05 with Android device
 *    Open Android app
 *    Select HC-05 and click Connect
 * 
 * TROUBLESHOOTING:
 * - MPU6050 not detected: Check I2C connections
 * - No data received: Verify HC-05 baud rate is 9600
 * - Incorrect values: Calibrate by modifying ACCEL_SCALE
 * - Unstable readings: Add capacitors on power lines
 */
