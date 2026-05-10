# Palm & Finger Detection App

## Overview
Native Android app that captures a palm image, detects fingers,
extracts minutiae-like data, and validates individual finger scans
against the palm record.

## Architecture
MVVM (Model-View-ViewModel) with:
- CameraX for camera management
- MediaPipe Hands for hand detection (on-device ML)
- Laplacian variance for blur detection
- Custom Views for camera overlays

## How to Build & Run
1. Clone: `git clone <repo-url>`
2. Open in Android Studio (Hedgehog or later)
3. Sync Gradle
4. Connect a physical Android device (API 24+)
5. Run the app

## Key Libraries
- CameraX 1.3.1
- MediaPipe Tasks Vision 0.10.9
- Jetpack ViewModel + LiveData
- Navigation Component

## Challenges & Solutions
- **Palm vs dorsal detection:** Used cross-product of wrist-to-finger
  vectors to determine hand orientation.
- **Blur detection:** Implemented Laplacian variance method for
  real-time blur assessment before saving.
- **Luminosity:** Built a custom CameraX ImageAnalysis.Analyzer
  that classifies lighting and auto-adjusts exposure compensation.
