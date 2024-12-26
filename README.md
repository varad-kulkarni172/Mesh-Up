# Emergency Mesh Network

This project demonstrates a mesh network solution for disaster-affected areas where traditional communication infrastructure is unavailable. The system leverages IoT devices (e.g., ESP8266), desktops, Raspberry Pi, and mobile devices to form an ad-hoc mesh network, enabling decentralized communication in emergency scenarios.

---

## 🚀 Features

- **Ad-hoc Mesh Networking**: Devices form a decentralized network, automatically discovering and connecting with peers.
- **UDP-based Communication**: Lightweight and efficient message exchange using UDP protocol.
- **Message Rebroadcasting**: Ensures messages propagate through the network.
- **Device Interoperability**: Supports platforms like ESP8266, Raspberry Pi, laptops, desktops, and Android devices.
- **Flood Protection**: Prevents message duplication using unique IDs and a history log.
- **Message Acknowledgment**: Ensures message reliability and traceability.

---

## 📜 Message Structure (UDP Header)

Messages transmitted over the UDP protocol include the following structure:
[Message ID] | [Origin Device ID] | [Message Content]

### Components:
1. **Message ID**: Unique identifier for each message, composed of the device’s `DEVICE_ID` and a random suffix (e.g., `123abc-5678`).
2. **Origin Device ID**: The unique ID of the device that originated the message.
3. **Message Content**: The actual communication payload.

### Example:
123abc-5678 | 123abc | Emergency at Location X

### Key Benefits:
- **Uniqueness**: Prevents message duplication.
- **Traceability**: Identifies the source of the message.
- **Data Integrity**: Ensures meaningful communication.

---

## 🛠️ Project Components

### 1. **IoT Device Code (ESP8266)**
- **Language**: Arduino C++
- **Functionality**:
  - Operates as a SoftAP (Hotspot) or connects to open Wi-Fi networks.
  - Sends and receives UDP messages using the specified header structure.
  - Rebroadcasts messages to ensure network-wide communication.
  - Tracks message history to prevent flooding.

---

### 2. **MeshNetworkClient.java (Java Swing App)**
- **Platform**: Java Swing
- **Purpose**: Provides a user-friendly interface for desktops, laptops, and Raspberry Pi.
- **Functionality**:
  - Acts as a client node in the mesh network.
  - Allows manual message input and displays received messages.
  - Visualizes network activity and message propagation.

---

### 3. **MainActivity.java (Android App)**
- **Platform**: Android
- **Purpose**: Extends the mesh network's functionality to mobile devices.
- **Functionality**:
  - Provides a mobile-friendly interface for sending and receiving messages.
  - Displays network activity in real-time.
  - Offers push notifications for incoming messages.
  - Integrates with Wi-Fi APIs for dynamic connection management.

---

## 🛴 Getting Started

### 1. Setting up the IoT Device (ESP8266)
1. Connect the ESP8266 to your computer.
2. Open the provided code (`ESP8266_MeshNetwork.ino`) in the Arduino IDE.
3. Configure the `SSID` and `UDP_PORT` if required.
4. Upload the code to the device.
5. Power the device and observe the serial monitor for network activity.

### 2. Using the Java Swing Client
1. Compile and run the `MeshNetworkClient.java` file.
2. Connect the device to the same Wi-Fi as the mesh network.
3. Start communicating via the GUI.

### 3. Using the Android App
1. Import the project into Android Studio.
2. Open `MainActivity.java` and ensure the necessary dependencies are installed.
3. Build and install the APK on your Android device.
4. Connect the mobile device to the mesh network Wi-Fi.
5. Start the app to send and receive messages.

---

## 🛡️ How It Works

1. **Network Formation**:
   - Each ESP device creates an open SoftAP network.
   - Devices scan for and connect to nearby open networks, forming a mesh.

2. **Message Transmission**:
   - Messages are tagged with unique IDs (`DEVICE_ID` + Random Suffix).
   - Sent messages are broadcasted over the mesh using UDP.

3. **Message Handling**:
   - Devices receiving messages check for duplication using the `Message ID`.
   - Non-duplicate messages are acknowledged and rebroadcasted.

4. **Java Client**:
   - Connects to the mesh network.
   - Allows users to view, send, and track messages in real-time.

5. **Android App**:
   - Acts as a portable node in the mesh.
   - Facilitates mobile-based interaction for disaster management.

---

## 🌟 Use Cases
- Real-time communication during disaster recovery.
- Ad-hoc networks for outdoor events or remote areas.
- Educational demonstrations of decentralized communication systems.

---

## 📚 Code Summaries

### **MainActivity.java** (Android App)
- **Wi-Fi Management**: Scans and connects to open networks.
- **Message Handling**: Displays, sends, and notifies users about messages.
- **Location Integration**: Tags messages with geographical context if needed.
- **Permissions**: Manages Android permissions for smooth functionality.
- **Broadcast Receivers**: Listens for network changes to maintain connection.

---

### **MeshNetworkClient.java** (Java Swing App)
- **Graphical Interface**: Displays and allows message inputs via GUI.
- **UDP Communication**: Sends, receives, and rebroadcasts messages.
- **Concurrency**: Uses multithreading for efficient message handling.
- **Network Visualization**: Displays live network activity and history.
- **Cross-Platform**: Compatible with laptops, desktops, and Raspberry Pi.

---

## 📞 Contact
Feel free to contribute or report issues in the GitHub repository. Happy coding! 🌐
