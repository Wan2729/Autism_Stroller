# 🧩 Smart Autism Stroller System

This project consists of **ESP32 firmware** and an **Android mobile application** designed to control and monitor a smart stroller built specifically for children with autism.

The system enables real-time stroller control while also providing digital features such as achievements, social interaction, and a community forum.

---

# 📌 Overview

The Smart Autism Stroller integrates hardware and software to create a safer, smarter, and more engaging mobility solution.

## 🎮 Mobile App Features

- Remote stroller control
- Achievement & reward system
- Social features
- Community forum
- Music playback system

## 🛠️ Stroller Hardware Features

The stroller is powered by an **ESP32** and equipped with:

- 📷 Camera module  
- 🔊 Speaker  
- 📺 Display  
- 💡 LED lighting system  
- 📡 GPS sensor  
- 📏 Distance sensor  
- 🫁 Carbon Monoxide (CO) sensor  
- 🌡️ Temperature sensor  

The mobile application communicates with the ESP32 to send commands and receive sensor data.

---

# 🚀 How to Run the Android App

> ⚠️ The APK has not been deployed yet. The app must be run directly from Android Studio.

## 1️⃣ Requirements

- Android Studio (latest recommended version)
- Android device
- USB cable (must support **data transfer**, not charging-only)
- USB Debugging enabled on your phone
- Active internet connection

## 2️⃣ Steps to Run

1. Open the project in **Android Studio**
2. Enable **USB Debugging** on your Android device  
3. Connect your phone using a data-capable USB cable  
4. Run `MainActivity.kt` from Android Studio  
5. Allow all requested permissions when prompted  
6. Ensure internet connection is active for full functionality  

---

# 🗄️ Database Information

- Music files are stored using a **free-tier cloud database service**
- Free-tier services may automatically pause when inactive
- If music fails to play, the database may be temporarily suspended

📧 To request database reactivation, contact:  
**wanharithazdy12@gmail.com**

---

# 🔌 ESP32 Setup

The ESP32 handles:

- Hardware control (motor, display, speaker, lighting)
- Sensor data collection
- Communication with the mobile application

## How to Run the ESP32

1. Open the firmware code in Arduino IDE or PlatformIO
2. Select the correct ESP32 board
3. Connect the ESP32 via USB
4. Upload the firmware
5. Ensure proper wiring to all sensors and modules

---

# 🔮 Future Improvements

Planned enhancements include:

- Improved error handling when connection is lost
- Online display content similar to music streaming
- User customization based on achievement level
- Advanced reporting with data comparison and analytics
- More stable cloud infrastructure for media storage
- Improved security and communication reliability

---

# 📖 Project Purpose

This project aims to:

- Enhance safety for children with autism
- Provide environmental awareness through sensors
- Create an engaging experience via music and interactive features
- Support caregivers with remote monitoring and control

---

# ⚠️ Disclaimer

This project is currently under development and intended for academic or research purposes. Further optimization, security improvements, and deployment enhancements are planned.
