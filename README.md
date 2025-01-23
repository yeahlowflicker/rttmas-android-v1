# RTTMAS Android Client

This is the Android client-side application for the Realtime Traffic Monitoring and Analysis System (RTTMAS) project.

The app is made for drivers to receive real-time traffic warnings to enhance road safety. The RTTMAS server continously monitor the road situation and sends alerts to nearby drivers.

As a return, the app would periodically capture the road situation in front and report back to the server for analysis.

Users are expected to install and run this app on their Android device(s) while driving. The device would be mounted on the dash facing forward.

## Features
* 📷 Real-time license plate recognition
* 📍 Periodic real-time data report
* ⚠️ Traffic alerts receiving and display

## Technologies
* YOLO11s real-time object detection
* Google ML Kit - Text Recognition
* Firebase Cloud Messaging (FCM)
* MQTT messaging

## Dependencies
* [ncnn](https://github.com/Tencent/ncnn)
* [opencv-mobile](https://github.com/nihui/opencv-mobile)

## How to build
### Step 1: Clone this repository
```bash
git clone https://github.com/yeahlowflicker/rttmas-android-v1
```

### Step 2: Download and import dependencies
Download and extract the `ncnn` and `opencv-mobile` releases into the `app/cpp/` folder.

This project uses `ncnn-20240410-android-vulkan/` and `opencv-mobile-2.4.13.7-android/` by default.

If you intent to use other versions, make sure to adjust the paths in `app/cpp/CMakeLists.txt`.

### Step 3: Sync gradle
Open the project in Android Studio. In the `build.gradle` file(s), press the `Sync Now` button to install all the dependencies automatically.

### Step 4: Build the project
Build the project using Android Studio and Android SDK.
