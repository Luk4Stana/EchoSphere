EchoSphere is a decentralized communication tool for Android. It allows users to broadcast and receive short text messages to nearby devices without using internet, cellular data, Wi-Fi, or Bluetooth pairing.

Operational Overview
The application uses Bluetooth Low Energy (BLE) Advertising technology. Instead of establishing a point-to-point connection, the app broadcasts small data packets into the surrounding environment. Any device within range running EchoSphere can detect these packets and display the message.

Range and Limitations
The effective communication radius is typically between 10 and 30 meters (30 to 100 feet).

The actual range depends on:

Smartphone hardware sensitivity.

Physical obstacles such as walls or metal structures.

Electronic interference in the area.

Key Features
Offline Operation: Works in remote areas or during network outages.

No Pairing Required: Messages are received instantly by any device in range.

Privacy Focused: No accounts, no data collection, and no logs.

Visual Interface: Real-time radar view to monitor local broadcasts.

Technical Specifications
Language: Kotlin

UI Framework: Jetpack Compose

Minimum Requirement: Android 8.0 (Oreo) or higher

Protocol: BLE Advertising (Manufacturer Specific Data)

License
This project is licensed under the GNU General Public License v3.0 (GPL-3.0).
