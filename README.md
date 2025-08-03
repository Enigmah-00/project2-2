# ScanShot - OCR & Image Analysis App

## 1. Introduction
**ScanShot** is a feature-rich mobile application designed for fast and accurate text and object recognition. Powered by Google ML Kit, it leverages on-device machine learning to perform Optical Character Recognition (OCR), Object Detection, Barcode Scanning, and OMR (Optical Mark Recognition) without requiring an internet connection. The app enables users to easily scan text, detect objects, evaluate MCQ answer sheets, and decode QR/barcodes from images or real-time camera input.

## 2. Objective
The primary objective of **ScanShot** is to build a powerful, lightweight mobile app that can:
- Extract text from images and live camera input
- Recognize objects and label them in real-time
- Automatically check multiple-choice answer sheets using OMR
- Decode barcodes and QR codes
- Provide all features in an offline, on-device environment

## 3. Tools & Technologies
- **Google ML Kit**
  - Text Recognition (on-device)
  - Image Labeling
  - Object Detection and Tracking
  - Barcode Scanning
- **Android Studio** (Kotlin)
- **CameraX** (for live camera integration)
- **uCrop** (for cropping selected or captured images)
- **OpenCV** (for OMR sheet detection and comparison)
- **Git/GitHub** (for version control and collaboration)

## 4. Methodology
The app follows a modular and efficient pipeline:
1. **Image Input**: Capture image via camera or pick from gallery
2. **Image Preprocessing**: Crop or enhance images as needed
3. **ML Task Execution**:
   - OCR: Detect and extract text using ML Kit
   - Object Detection: Label objects using real-time camera feed
   - OMR: Compare answer sheets against the correct key
   - Barcode Scanning: Detect QR/barcodes from images or live feed
4. **Result Display**: Visualize results with intuitive UI and annotated feedback

## 5. Key Features
- 🔤 **Offline OCR**: Accurate text extraction in multiple languages
- 🧠 **Live Object Detection**: Real-time image labeling with camera
- ✅ **OMR Answer Sheet Checking**: Grade MCQs with visual feedback
- 🖼️ **Image Cropping**: Improve accuracy by selecting key regions
- 📷 **CameraX Integration**: Smooth live analysis experience
- 🔍 **QR & Barcode Scanning**: Fast and reliable scanning
- 💡 **Modern UI**: Simple, clean, and intuitive interface

## 6. Expected Outcomes
- A lightweight mobile solution for text and object recognition
- Accurate and fast results across a wide range of use-cases
- High performance on all modern Android devices
- Practical tool for students, educators, and professionals

## 7. Conclusion
**ScanShot** demonstrates the power of on-device machine learning in solving real-world tasks like OCR, object recognition, and test grading. With its offline capabilities and versatile features, it serves as a smart utility for education, business, and productivity needs.
