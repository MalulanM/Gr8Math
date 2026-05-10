# Gr8Math Mobile App

An interactive Android math learning application built with Jetpack Compose, featuring real-time collaboration, cloud storage, and gamification through Unity integration.

## Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [Configuration](#configuration)
- [Building the Application](#building-the-application)
- [Deployment](#deployment)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## Project Overview

Gr8Math is a comprehensive Android application designed for interactive math learning with:

- **Supabase Backend** - PostgreSQL database with real-time capabilities and authentication
- **AWS S3/Tigris Integration** - Cloud storage for user assets and content
- **Firebase Services** - Push messaging and analytics
- **Unity Integration** - Enhanced graphics and game mechanics
- **Jetpack Compose UI** - Modern, reactive user interface
- **Retrofit Networking** - Type-safe HTTP client for API communication

## Tech Stack

### Backend & Services

| Technology | Purpose | Version |
|-----------|---------|---------|
| **Supabase** | Database, Auth, Real-time APIs | - |
| **AWS S3/Tigris** | Cloud storage for files | 1.0.0+ |
| **Firebase** | Push notifications & analytics | 34.9.0 |

### Frontend & UI

| Technology | Purpose | Version |
|-----------|---------|---------|
| **Jetpack Compose** | Declarative UI framework | Latest (via BOM) |
| **Material 3** | Design system | Latest |
| **Glide** | Image loading and caching | 4.16.0 |
| **Konfetti** | Confetti animations | 2.0.4 |
| **ColorPickerView** | Color selection UI | 2.2.4 |

### Networking & Data

| Technology | Purpose | Version |
|-----------|---------|---------|
| **Retrofit** | HTTP client | 2.11.0 |
| **OkHttp** | HTTP engine with logging | 4.12.0 |
| **Gson** | JSON serialization | Latest |
| **Ktor Client** | Async HTTP client | Latest |

### Build & Other

| Technology | Purpose | Version |
|-----------|---------|---------|
| **Kotlin** | Primary language | Latest |
| **Unity** | Game engine integration | 3.0.5+ |
| **Android 25-35** | Target API range | 25 min, 35 target |

## Prerequisites

### System Requirements

- **OS**: Windows, macOS, or Linux
- **RAM**: Minimum 8GB (16GB recommended)
- **Disk Space**: At least 5GB free space
- **Network**: Internet connection for Supabase, Firebase, and AWS services

### Software Requirements

- **Android Studio** (Jellyfish or later recommended)
- **Java Development Kit (JDK)** 17 or higher
- **Android SDK** (API 25+ minimum, 35 recommended)
- **Gradle** (included with Android Studio)
- **Git** for version control
- **Unity** (if building Unity components separately)

### Accounts Required

- **Supabase Account** - https://supabase.com
- **Firebase Account** - https://firebase.google.com
- **AWS Account** (for Tigris/S3) - https://aws.amazon.com
- **Google Play Developer Account** (for deployment)

## Project Structure

```
Gr8Math/
├── app/                               # Main Android application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/gr8math/
│   │   │   │   ├── Activity/          # UI Activities
│   │   │   │   ├── Adapter/           # RecyclerView adapters
│   │   │   │   ├── Data/              # Repositories & data layer
│   │   │   │   ├── Helper/            # Utility helpers
│   │   │   │   ├── Model/             # Data classes & models
│   │   │   │   ├── Services/          # API services (Retrofit, Supabase)
│   │   │   │   ├── Utils/             # Utility functions
│   │   │   │   └── ViewModel/         # MVVM ViewModels
│   │   │   ├── res/                   # Resources (layouts, drawables, strings)
│   │   │   └── AndroidManifest.xml    # App manifest
│   │   ├── test/                      # Unit tests
│   │   └── androidTest/               # Instrumented tests
│   ├── build.gradle.kts               # App-level build configuration
│   ├── google-services.json           # Firebase configuration
│   └── proguard-rules.pro             # ProGuard rules
│
├── unityLibrary/                      # Unity project integration
│   ├── src/
│   ├── libs/                          # Native libraries
│   ├── symbols/                       # Debug symbols
│   └── build.gradle                   # Unity build config
│
├── shared/                            # Shared resources
│   ├── common.gradle                  # Shared Gradle configuration
│   └── keepUnitySymbols.gradle       # Unity symbol preservation
│
├── gradle/                            # Gradle wrapper & versions
│   ├── libs.versions.toml             # Centralized dependency versions
│   └── wrapper/
│
├── build.gradle.kts                   # Project-level build configuration
├── settings.gradle.kts                # Project settings & module includes
├── local.properties                   # Local SDK and API configurations
├── gradle.properties                  # Global Gradle properties
└── README.md                          # This file
```

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Gr8Math
```

### 2. Configure Android SDK

Edit `local.properties` and set the SDK path:

**Windows:**
```properties
sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

**macOS:**
```properties
sdk.dir=/Users/YourUsername/Library/Android/sdk
```

**Linux:**
```properties
sdk.dir=/home/YourUsername/Android/Sdk
```

### 3. Sync Gradle Dependencies

```bash
./gradlew build
```

On Windows:
```bash
gradlew.bat build
```

## Configuration

### Supabase Setup

1. Create a project at https://supabase.com
2. Get your project credentials from the API settings

### Firebase Setup

1. Create a project at https://console.firebase.google.com
2. Download `google-services.json` from Firebase Console
3. Place it in `app/` directory

### AWS/Tigris Configuration

1. Set up an AWS account or use Tigris for S3-compatible storage
2. Create access keys from AWS IAM console

### Environment Variables

Add the following to `local.properties` (⚠️ **DO NOT COMMIT THIS FILE**):

```properties
sdk.dir=/path/to/android/sdk

# Supabase Configuration
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-supabase-anon-key

# AWS/Tigris Configuration
accessKeyId=your-access-key-id
secretAccessKey=your-secret-access-key
```

### Secure Credentials in Production

For production builds:

1. Use Android Keystore for storing credentials
2. Implement secure credential retrieval (e.g., from backend API)
3. Never commit `local.properties` to version control

## Building the Application

### Clean Build

```bash
./gradlew clean build
```

### Debug Build

Build and deploy for testing:

```bash
./gradlew assembleDebug
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

Create optimized release build:

```bash
./gradlew assembleRelease
```

**Output**: `app/build/outputs/apk/release/app-release.apk`

### Android App Bundle (AAB)

For Google Play Store submission:

```bash
./gradlew bundleRelease
```

**Output**: `app/build/outputs/bundle/release/app-release.aab`

### Build with Specific Variant

```bash
./gradlew assemble[VariantName]
```

Example:
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Deployment

### Prerequisites for Release

1. **Keystore File** - For signing APKs
2. **Keystore Credentials** - Password and key alias
3. **Google Play Developer Account** - For publishing

### Step 1: Create Signing Keystore (if needed)

```bash
keytool -genkey -v -keystore gr8math-release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 -alias gr8math-key
```

You'll be prompted for passwords and key information.

### Step 2: Configure Signing in Build

Add to `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../gr8math-release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

### Step 3: Build Signed Release Bundle

```bash
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_ALIAS=gr8math-key
export KEY_PASSWORD=your_key_password

./gradlew bundleRelease
```

### Step 4: Deploy to Google Play Store

1. Open [Google Play Console](https://play.google.com/console)
2. Select your app
3. Navigate to **Release** → **Production**
4. Click **Create new release**
5. Upload the `.aab` file from `app/build/outputs/bundle/release/`
6. Fill in release notes and details
7. Submit for review (24-48 hours approval time)

### Deploy to Physical Device

#### Using ADB

```bash
adb devices                                          # List devices
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Using Android Studio

1. Connect device via USB and enable USB Debugging
2. Click **Run** → **Run 'app'** in toolbar
3. Select device from dialog
4. App installs and launches automatically

### Deploy to Emulator

1. Launch Android Emulator from Android Studio
2. Wait for full boot
3. Click **Run** → **Run 'app'**
4. Select emulator from device dialog

## Testing

### Run Unit Tests

```bash
./gradlew test
```

Tests will run from `app/src/test/`

### Run Instrumented Tests

Requires emulator or connected device:

```bash
./gradlew connectedAndroidTest
```

### Generate Test Reports

```bash
./gradlew testReport
```

Reports available at: `build/reports/tests/`

### Manual Testing Checklist

- [ ] Test Supabase authentication (login/signup)
- [ ] Test data sync and real-time updates
- [ ] Verify image uploads to AWS/Tigris storage
- [ ] Test Firebase messaging (push notifications)
- [ ] Verify analytics events are tracked
- [ ] Test offline functionality (if implemented)
- [ ] Verify Unity integration loads correctly
- [ ] Test on multiple Android versions (25, 30, 35)

### View Logcat

```bash
adb logcat
adb logcat -s "Gr8Math"                            # Filter by tag
adb logcat -c                                       # Clear logs
```

## Troubleshooting

### Build Issues

| Issue | Solution |
|-------|----------|
| **Gradle sync fails** | Invalidate caches: File → Invalidate Caches & Restart |
| **SDK not found** | Ensure `sdk.dir` is correctly set in `local.properties` |
| **Plugin not found** | Run `./gradlew clean build` |
| **Memory issues** | Increase Gradle heap: add `org.gradle.jvmargs=-Xmx2g` to `gradle.properties` |

### Dependency Issues

| Issue | Solution |
|-------|----------|
| **Supabase dependency failed** | Check internet connection and Maven Central access |
| **Firebase errors** | Verify `google-services.json` exists in `app/` directory |
| **Retrofit issues** | Ensure OkHttp and Retrofit versions are compatible |

### Runtime Issues

| Issue | Solution |
|-------|----------|
| **App crashes on startup** | Check Logcat: `adb logcat -s "Gr8Math"` |
| **Supabase auth fails** | Verify `SUPABASE_URL` and `SUPABASE_KEY` in `local.properties` |
| **Storage upload fails** | Check AWS credentials and bucket permissions |
| **Firebase messaging not working** | Verify `google-services.json` and ensure device has Google Play Services |

### Device Connection Issues

```bash
adb kill-server
adb start-server
adb devices
```

### Emulator Issues

```bash
# List emulators
emulator -list-avds

# Launch specific emulator
emulator -avd emulator-name

# Cold boot
emulator -avd emulator-name -no-snapshot
```

## Development Workflow

### Architecture

The app follows MVVM (Model-View-ViewModel) architecture:

- **Activity** - UI presentation layer
- **ViewModel** - Business logic and state management
- **Data/Services** - Data access and API communication
- **Model** - Data classes and entities

### Key Services

- **Supabase Client** - For database and authentication
- **Retrofit API** - For custom API endpoints
- **Storage Service** - For AWS S3/Tigris uploads
- **Firebase Service** - For messaging and analytics

### Dependencies Management

Edit `gradle/libs.versions.toml` to manage all dependency versions centrally.

## Performance Optimization

- **ProGuard** is enabled in release builds for code obfuscation
- **Compose** provides efficient UI rendering
- **Glide** caches images automatically
- **Firebase Analytics** tracks performance metrics

## Security Best Practices

⚠️ **Important**: Never commit `local.properties` or credentials to version control:

```bash
# Add to .gitignore
local.properties
*.keystore
*.jks
google-services.json
```

For sensitive data:
- Use Android Keystore for storing secrets
- Implement SSL pinning for API communication
- Use ProGuard to obfuscate code in release builds
- Never log sensitive information

## Useful Commands

```bash
# Quick build
./gradlew build -x test

# Build and deploy to device
./gradlew installDebug

# Run app with debugger
./gradlew installDebug -d

# Check dependencies
./gradlew dependencies

# Generate dependency report
./gradlew dependencyInsight --dependency [dependency-name]

# Update Gradle wrapper
./gradlew wrapper --gradle-version=8.0
```

## Additional Resources

- [Supabase Documentation](https://supabase.com/docs)
- [Firebase Documentation](https://firebase.google.com/docs)
- [AWS SDK for Kotlin](https://github.com/awslabs/aws-sdk-kotlin)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Retrofit Documentation](https://square.github.io/retrofit/)
- [Android Developer Guide](https://developer.android.com)
- [Gradle Documentation](https://gradle.org/docs/)

## Support & Contributing

For issues, questions, or contributions:

1. Check existing GitHub issues
2. Review project documentation
3. Create a new issue with detailed information

---

**App Version**: 1.0  
**Target SDK**: 35 (Android 15)  
**Minimum SDK**: 25 (Android 7.0)  
**Last Updated**: May 2026
