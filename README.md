# 👶 Smart Parental Control Kids – Android App

**Smart Parental Control Kids** is the child-side companion app for the **Smart Parental Control** system. It runs in the background on the child’s Android device and enables secure, real-time monitoring and control by the parent via the parent app.

Built using native Java and Android architecture best practices, the app provides camera access, app usage logging, SMS and call tracking, and more — while respecting device performance and user permissions.

---

## 🔐 Purpose

This app is **not standalone**. It is meant to be installed on the child’s device and paired with the **parent app** using a secure device binding code generated via Firebase.

---

## 🚀 Key Features

- 🎥 **Live Camera Stream** – Capture and stream one-way video to the parent device using WebRTC and Camera2 API.
- ⏳ **App Usage Stats** – Log app usage and install/uninstall events, uploaded to Firebase.
- ✉️ **SMS Monitoring** – Upload existing and incoming messages to Firestore.
- 📞 **Call Monitoring** – Detect, upload, and act on whitelisted/blacklisted numbers.
- 🔐 **Device Binding** – Connect with parent device securely via Firestore structure:  
  `/device_bindings/{parentUid}/children/{childN}`
- 🧠 **Background Services** – All monitoring services are optimized for continuous background operation without draining battery.
- ☁️ **Firebase + Cloudinary** – For real-time sync and storing media assets like app icons and profile images.

---

## 🛠️ Tech Stack

| Category        | Tools/Technologies                                |
|----------------|----------------------------------------------------|
| Language        | Java                                               |
| IDE             | Android Studio                                     |
| Architecture    | MVVM                                               |
| Backend         | Firebase Auth, Firestore, Realtime DB, Storage     |
| Media Streaming | WebRTC, Camera2 API, SurfaceViewRenderer           |
| Image Storage   | Cloudinary                                         |
| Others          | Glide, Firebase Messaging, Permissions Handling    |

---
© 2025 Muhammad Memoon (Maani). All rights reserved.
This code is proprietary and not licensed for redistribution or commercial use without permission.
## 📂 Project Structure

