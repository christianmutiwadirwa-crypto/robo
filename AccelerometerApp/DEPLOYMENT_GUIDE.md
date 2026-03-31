# Accelerometer Android App - Deployment Checklist

## 🎯 Pre-Deployment Verification

### Code Quality
- [ ] All Java files compile without errors
- [ ] No warnings in Android Lint
- [ ] ProGuard rules properly configured
- [ ] Target SDK set to 33 (Android 13)
- [ ] Min SDK set to 21 (Android 5.0)

### Security
- [ ] All permissions properly declared in AndroidManifest.xml
- [ ] Runtime permissions handled for Android 6+
- [ ] No hardcoded credentials in code
- [ ] Network calls use appropriate timeouts
- [ ] No logging of sensitive data

### Testing
- [ ] App tested on real Android device (not just emulator)
- [ ] Bluetooth connection tested multiple times
- [ ] Data parsing validated with edge cases
- [ ] HTTP connectivity verified
- [ ] Error scenarios tested

## 🚀 Deployment Steps

### Step 1: Generate Signed APK

1. **Create Keystore** (if you don't have one):
   ```bash
   keytool -genkey -v -keystore my-release-key.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias my-key-alias
   ```

2. **In Android Studio**:
   - Build → Generate Signed Bundle / APK
   - Choose APK
   - Select your keystore file
   - Enter alias and passwords
   - Choose "release" build type
   - Finish

3. **Output Location**:
   ```
   app/release/app-release.apk
   ```

### Step 2: Testing Release APK

1. **Install on device**:
   ```bash
   adb install -r app/release/app-release.apk
   ```

2. **Run and verify**:
   - Open app
   - Connect to HC-05
   - Monitor data flow
   - Check logs

### Step 3: Deployment

**Option A: Direct Distribution**
- Share APK via email or cloud storage
- Users download and install
- Enable "Install from Unknown Sources" if needed

**Option B: Google Play Store**
- Create Google Play Developer Account ($25 one-time)
- Create app listing
- Upload APK to Play Store Console
- Set pricing and permissions
- Submit for review (~2-4 hours)

**Option C: Enterprise Distribution**
- Host APK on internal server
- Push to devices via MDM (Mobile Device Management)
- Track installations automatically

## 📋 Release Notes Template

```
Version 1.0.0 - Release Date: [DATE]

Features:
✓ Bluetooth Classic connection to HC-05
✓ Real-time accelerometer data streaming
✓ HTTP forwarding to Node.js backend
✓ Material Design UI
✓ Android 6.0+ support

Improvements:
- Optimized data parsing for 10+ Hz
- Enhanced error handling and recovery
- Improved Bluetooth connection stability

Bug Fixes:
- Fixed permission handling on Android 12+

Known Issues:
- None

Requirements:
- Android 6.0 or higher
- HC-05 Bluetooth module with accelerometer
- WiFi connectivity for server communication

Installation:
1. Download AccelerometerApp-v1.0.0.apk
2. Enable "Install from Unknown Sources"
3. Run APK and follow on-screen instructions
4. Grant requested permissions
5. Pair HC-05 in Bluetooth settings

Support:
See SETUP_GUIDE.md for troubleshooting
```

## 🔄 Update Process

### Creating Version 1.1.0

1. **Update version in build.gradle**:
   ```gradle
   versionCode 2
   versionName "1.1.0"
   ```

2. **Update changelog in code comments**:
   - Add version to documentation
   - Update CHANGELOG.md if exists

3. **Build and test**:
   ```bash
   ./gradlew clean assembleRelease
   ```

4. **Generate new signed APK** with same keystore

5. **Deploy using same process**

## 📊 Monitoring Post-Deployment

### User Feedback
- [ ] Monitor app logs for crashes
- [ ] Track common error patterns
- [ ] Collect user feedback and feature requests

### Performance Metrics
- [ ] Monitor Bluetooth connection success rate
- [ ] Track HTTP success rate to backend
- [ ] Monitor average latency
- [ ] Track app crash rate

### Server Monitoring
```bash
# Monitor error logs
tail -f server-errors.log

# Check memory usage
node --max-old-space-size=512 server.js

# Monitor request frequency
tail -f server.log | grep "POST /data" | wc -l
```

## 🐛 Troubleshooting After Deployment

### If users report crashes

1. **Collect crash logs**:
   ```bash
   adb logcat > crash_log.txt
   ```

2. **Analyze stack trace**:
   - Look for NullPointerException
   - Check permission denials
   - Review Bluetooth errors

3. **Create hotfix**:
   - Fix issue in code
   - Increment patch version (1.0.1)
   - Rebuild and redeploy

### If users report connection issues

1. **Verify Bluetooth requirements**:
   - Device supports Bluetooth Classic (older phones)
   - HC-05 correctly configured
   - User granted Bluetooth permissions

2. **Add diagnostics**:
   - Log Bluetooth adapter state
   - Log device discovery results
   - Log connection attempts

## 📦 Distribution Package Contents

```
AccelerometerApp-v1.0.0/
├── app-release.apk              # Signed APK for delivery
├── CHANGELOG.md                 # What's new
├── INSTALLATION.md              # User installation guide
├── README.md                    # Project overview
├── SETUP_GUIDE.md              # Setup instructions
├── QUICK_REFERENCE.md          # Quick start guide
├── server.js                   # Backend server
├── package.json                # Node.js dependencies
├── arduino_example.ino         # Arduino sketch
└── docs/                       # Additional documentation
    ├── architecture.md
    ├── api-specification.md
    └── troubleshooting.md
```

## ✅ Final Checklist Before Release

- [ ] All tests pass
- [ ] Code reviewed by team
- [ ] Documentation complete and clear
- [ ] Release notes written
- [ ] APK signed with production key
- [ ] APK tested on multiple devices
- [ ] Server endpoint verified working
- [ ] Arduino code verified working
- [ ] Permissions verified for target API
- [ ] No debug logging in final build
- [ ] ProGuard/R8 enabled for obfuscation
- [ ] Version numbers updated
- [ ] Changelog documented
- [ ] User guide prepared
- [ ] Support contact information ready

---

**Ready to deploy! 🚀**
