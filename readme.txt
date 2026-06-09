========================================================================
🚀 SPACEFINITY - REAL-TIME YOLOv8 OBJECT DETECTOR FOR ANDROID
========================================================================

Spacefinity is a real-time object detection Android application that utilizes
the YOLOv8 Nano deep learning model (trained on the 80-class COCO dataset) 
via ONNX Runtime and CameraX to detect and label everyday objects directly
on-device.

------------------------------------------------------------------------
✨ THE CUSTOM LOGO
------------------------------------------------------------------------
Branding Name: Spacefinity
App Logo: An infinity loop representation on a dark cosmic background.
The logo represents the infinite loop of live frames analyzed in real-time
by the neural network engine.

Logo Image Asset Location:
  - evidence/app_installed.png (Displays the icon on the phone home screen)

------------------------------------------------------------------------
🛠 KEY ARCHITECTURE FIXES & FEATURES
------------------------------------------------------------------------
1. Android 15 Compatibility:
   Upgraded ONNX Runtime to 1.22.0 and CameraX to 1.4.0 to resolve the
   16KB page-alignment native library dynamic loading requirements.

2. AppCompat Theme Resolution:
   Created custom theme resource style (AppTheme) to resolve the
   IllegalStateException crash on startup.

3. 80-Class Vocab Mapping:
   Loaded standard COCO labels (cat, tv, dog, oven, refrigerator, etc.) 
   to display actual name tags instead of generic class indexes.
========================================================================
