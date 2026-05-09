# Android Optical Signal Detection

This project is an Android app that uses **OpenCV** and the device camera to detect and track bright optical signals in the camera feed.

In practice, the app is tuned to detect a **green light source** (for example, a blinking LED) in the live camera preview, group the detected blobs over time, and estimate:

- the signal’s position in the frame
- the signal’s apparent area
- a rough **frequency** value
- a rough **duty cycle** value

The values are drawn directly over the camera preview.

## What the app does

The main logic lives in `app/src/main/java/com/example/mrhs/opencvtest/MainActivity.java` and the helper classes beside it.

At a high level, each camera frame is processed like this:

1. Capture a frame from the camera.
2. Blur the image slightly to reduce noise.
3. Convert the frame to HSV color space.
4. Threshold the image to keep only pixels in a green range.
5. Apply erosion and dilation to clean up the mask.
6. Find contours in the filtered image.
7. Compute the center and area of each contour.
8. Track each signal over time with `SignalHandler` and `Signal`.
9. Draw the contour and overlay estimated frequency/duty-cycle text.

### Important details

- The app is configured for **landscape** mode and full-screen camera preview.
- It uses the older Android `Camera` API through OpenCV’s camera view classes.
- The OpenCV module is included in the repo as `openCVLibrary320`.
- There is a small native C++ stub in `app/src/main/cpp/native-lib.cpp`, but the detection logic itself is implemented in Java.

## Project structure

- `app/` — Android application code
- `app/src/main/java/com/example/mrhs/opencvtest/` — camera, detection, and tracking logic
- `app/src/main/cpp/` — JNI stub library
- `openCVLibrary320/` — bundled OpenCV 3.2.0 Android library module

## Requirements

- Android Studio with support for this older Gradle/Android plugin setup
- A physical Android device with a camera
- Camera permission granted at runtime / install time

## Setup

1. Clone the repository.
2. Open the project in Android Studio as an **existing project**.
3. Let Gradle sync and make sure both modules are present:
   - `:app`
   - `:openCVLibrary320`
4. Build and run the app on a real device.
5. Grant any requested permissions, especially the camera permission.

If you prefer the command line, try:

```bash
./gradlew assembleDebug
```

> Note: this is an older Android project that targets SDK 26 and depends on an included OpenCV 3.2.0 module. If you are using a newer Android Studio installation, you may need a compatible JDK/Gradle setup for the project to sync successfully.

## Using the app

1. Launch the app on a device.
2. Point the camera at a bright green blinking light or similar optical signal.
3. Watch the preview for detected contours and the frequency/duty-cycle overlay.

## Tuning

The detection range is currently hard-coded in `MainActivity.java`:

- Hue: `44` to `72`
- Saturation: `200` to `255`
- Value: `200` to `255`

If you want to detect a different color or lighting condition, those thresholds are the first place to adjust.

## Notes

- This looks like a prototype/research app rather than a polished consumer app.
- The signal tracking code is heuristic-based and may need tuning for different cameras, distances, or ambient light.
- The repo includes the OpenCV library source/module so the project can be built without downloading OpenCV separately.
