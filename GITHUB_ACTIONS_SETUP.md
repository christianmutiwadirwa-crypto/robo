# GitHub Actions CI/CD Setup

## Overview

Your project now includes automated build pipelines using GitHub Actions for:
- ✅ **Android App Building** - Automatic APK builds (debug & release)
- ✅ **Unit Testing** - Automated test runs
- ✅ **Node.js Backend** - Multi-version Node.js testing
- ✅ **Artifact Management** - APK storage for downloads

---

## Workflows Included

### 1. Android Build Workflow (`android-build.yml`)

**Triggers on:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual trigger via GitHub Actions UI

**What it does:**
- Sets up JDK 11
- Installs Android SDK components
- Builds debug APK
- Builds release APK
- Runs unit tests
- Uploads APK artifacts (30-day retention)
- Uploads test results

**Output artifacts:**
- `accelerometer-app-debug` - Debug APK
- `accelerometer-app-release` - Release APK
- `test-results` - Unit test reports

---

### 2. Node.js Backend Workflow (`nodejs-build.yml`)

**Triggers on:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual trigger via GitHub Actions UI

**What it does:**
- Tests on Node.js 16.x, 18.x, and 20.x
- Installs npm dependencies
- Checks syntax
- Performs basic server startup test
- Archives production files

**Versions tested:** 16, 18, 20

---

## Setup Instructions

### 1. Create GitHub Repository

```bash
cd c:\Users\Lenovo\OneDrive\Desktop\IAES\robo
git init
git remote add origin https://github.com/YOUR_USERNAME/your-repo.git
```

### 2. Push Code to Repository

```bash
git add .
git commit -m "Initial commit: Complete accelerometer app with GitHub Actions"
git branch -M main
git push -u origin main
```

### 3. Verify Workflows

1. Go to: `https://github.com/YOUR_USERNAME/your-repo/actions`
2. You should see two workflows:
   - **Android Build**
   - **Node.js Backend Build**

### 4. Create Signing Configuration (Optional - for Release APK)

For signed release APKs, create `AccelerometerApp/keystore.properties`:

```properties
storeFile=accelerometer.keystore
storePassword=YOUR_PASSWORD
keyAlias=accelerometer-key
keyPassword=YOUR_PASSWORD
```

Then update `AccelerometerApp/app/build.gradle`:

```gradle
signingConfigs {
    release {
        storeFile file("keystore.properties")
        storePassword project.getProperties().getOrDefault('storePassword', '')
        keyAlias project.getProperties().getOrDefault('keyAlias', '')
        keyPassword project.getProperties().getOrDefault('keyPassword', '')
    }
}

buildTypes {
    release {
        signingConfig signingConfigs.release
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

---

## Monitoring Builds

### View Build Status

1. **Real-time**: Push code → GitHub Actions automatically starts building
2. **Dashboard**: https://github.com/YOUR_USERNAME/your-repo/actions
3. **Badge**: Add to README.md:

```markdown
![Android Build](https://github.com/YOUR_USERNAME/your-repo/workflows/Android%20Build/badge.svg)
![Node.js Build](https://github.com/YOUR_USERNAME/your-repo/workflows/Node.js%20Backend%20Build/badge.svg)
```

### Download APKs

1. Go to Actions → Select completed workflow
2. Scroll to "Artifacts" section
3. Download `accelerometer-app-debug` or `accelerometer-app-release`

---

## Workflow Customization

### Modify Build Targets

Edit `AccelerometerApp/app/build.gradle`:

```gradle
android {
    compileSdk 34  // Update target API
    defaultConfig {
        minSdk 21
        targetSdk 34
    }
}
```

Workflow auto-adapts to these changes.

### Add Code Quality Checks

Add to `android-build.yml` after `Run unit tests`:

```yaml
- name: Run Lint
  run: cd AccelerometerApp && ./gradlew lint

- name: Upload Lint Results
  uses: actions/upload-artifact@v3
  with:
    name: lint-results
    path: AccelerometerApp/app/build/reports/lint-results*.html
```

### Enable Code Coverage

Add to `AccelerometerApp/app/build.gradle`:

```gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.8"
}

task jacocoTestReport(type: JacocoReport) {
    dependsOn testDebugUnitTest
    reports {
        xml.required = true
        html.required = true
    }
}
```

Then in workflow:

```yaml
- name: Generate Coverage Report
  run: cd AccelerometerApp && ./gradlew jacocoTestReport
```

---

## Troubleshooting

### Build Fails

1. **Check logs**: Click workflow run → View logs
2. **Common issues**:
   - SDK version mismatch → Update `android.compileSdk` in `build.gradle`
   - Java version → Ensure JDK 11 is set
   - Gradle wrapper missing → Run locally first

### Increase Build Speed

Add caching to workflows (already included):

```yaml
cache: gradle  # Caches Gradle dependencies
```

### Large APK Size

Optimize in `app/build.gradle`:

```gradle
android {
    bundle {
        language.enableSplit = true
        density.enableSplit = true
        abi.enableSplit = true
    }
}
```

---

## Advanced: Deploy Releases

### Auto-Deploy to GitHub Releases

Add step to `android-build.yml`:

```yaml
- name: Create Release
  if: startsWith(github.ref, 'refs/tags/')
  uses: softprops/action-gh-release@v1
  with:
    files: AccelerometerApp/app/build/outputs/apk/release/**/*.apk
```

Then tag a version:

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Security Best Practices

1. **Never commit keystore files** to Git
2. **Use GitHub Secrets** for sensitive data:
   - Go to: Settings → Secrets and variables → Actions
   - Add: `KEYSTORE_PASSWORD`, `KEY_ALIAS`, etc.

3. **Restrict branch protection**:
   - Settings → Branches → Add rule
   - Require passing GitHub Actions checks before merge

---

## What's Next?

✅ Workflows created and committed  
✅ Automatic builds on every push  
✅ APK artifacts available for download  
✅ Tests run automatically  

**Next steps:**
1. Push to GitHub repository
2. Monitor first build at `Actions` tab
3. Download APK from artifacts
4. Deploy to your CI/CD system or device
