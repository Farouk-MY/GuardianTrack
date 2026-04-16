# 🛡️ GuardianTrack: Your Pocket Bodyguard

Welcome to **GuardianTrack**! 

Whether you are a 10-year-old trying to understand how cool super-spy gadgets work, or a senior engineer looking for the architecture patterns, this README explains exactly *what* we built and *how* we built it!

---

## 👦 Explain Like I'm 10: How does it work?

Imagine you have an invisible bodyguard walking with you everywhere you go. If you trip and fall down hard, or if your phone is about to die and leave you stranded, this bodyguard instantly knows exactly where you are and sends a secret SOS text message to your family to come help you!

Here is how our bodyguard works:

1. **🧲 Fall Detection (The Sensor):** Your phone has a tiny little sensor inside called an **accelerometer**. It feels gravity and movement. If you drop your phone, it feels weightless for a split second, and then feels a giant **WHACK** when it hits the floor. Our app does some cool math to figure out when that exact pattern happens!
2. **📍 GPS Location (The Map):** The phone asks satellites far up in space exactly where you are standing right now. We attach a map pin to your SOS message so your mom or dad knows exactly where to find you.
3. **🔋 Battery Monitor:** If your phone battery drops to critical levels (like 15%), the app says *"Uh oh, I'm about to die!"* and sends a warning text to your family *before* it shuts down completely.
4. **✉️ Sending SMS:** The app works quietly in the background. It talks directly to your phone's antenna to fire off an emergency text message automatically, without even opening your texting app.
5. **🎨 The Glass Design:** We made the app look like a futuristic glass hologram (we call it **HoloGlass**!). Buttons bounce like jelly and everything glows like a premium sci-fi dashboard.

---

## 👨‍💻 How We Built It: Theory & Technical Details

For the engineers and developers, here is exactly how the magic works under the hood. GuardianTrack is built natively using **Kotlin**, **Jetpack Compose**, and the **MVVM Architecture**.

### 1. The Fall Detection Algorithm (Theory)
We use a **Foreground Service** (`SurveillanceService`). This tells the Android operating system: *"Hey, I'm doing important work, please don't kill my app when the user puts the phone in their pocket."*

We listen to the `Sensor.TYPE_ACCELEROMETER` in real-time. 
**The Math:** We calculate the Total Vector Magnitude using the 3D space axes: `Magnitude = √(x² + y² + z²)`
*   **Phase 1 (Free-fall):** When the magnitude drops below 3 m/s² for more than 100 milliseconds, we know the phone is falling (zero-gravity).
*   **Phase 2 (The Impact):** Right after the fall, we open a 200ms "Impact Window". If the magnitude suddenly spikes above our threshold (like 15 m/s²), boom! A fall is confirmed.
*   **Bonus (Low-Pass Filter & Sliding Window):** Your phone shakes when you walk. We use a **Low-Pass Filter** (`new = 0.8 * old + 0.2 * new`) to smooth out the noise, and a **sliding window** of arrays to spot patterns so we don't accidentally text your parents when you're just jogging!

### 2. Location Tracking (Theory)
We use Android's **LocationManager** and **FusedLocationProvider**.
When an emergency happens, the `LocationHelper` class wakes up and fetches your exact coordinates (Latitude and Longitude). To make the UI super responsive, we also use a `BroadcastReceiver` listening to `LocationManager.PROVIDERS_CHANGED_ACTION`. If you pull down your phone's shade and turn off GPS, our ViewModel hears it instantly and drops down an Alert Card in the UI telling you to turn it back on.

### 3. Battery Low Detection (Theory)
We don't constantly check your battery using a loop because that would waste your battery! Instead, Android has an efficient built-in system called a **Broadcast Intent**. 
We created a `BatteryReceiver`. When your battery drops to exactly 15%, the Android OS shouts out to all apps: `ACTION_BATTERY_LOW`! Our app hears this shout in the background, grabs your location, limits background work, and triggers the SMS workflow automatically. We also added a custom intent action `SIMULATE_BATTERY_LOW` so we can test the workflow instantly via a debug button on the dashboard.

### 4. Sending the SMS
We use Android's `SmsManager.getDefault()`. Building this requires the strict `<uses-permission android:name="android.permission.SEND_SMS" />` permission. 
Because we don't want to actually spam real people while developing or testing, we built an **SMS Simulation Mode** inside `SmsHelper`. If simulation is turned on, instead of charging your phone bill, it creates a local Android Notification and a database log pretending the SMS was sent.

### 5. The UI (Jetpack Compose)
Instead of boring old XML layouts, we used **Jetpack Compose** (Android's modern declarative UI toolkit) to build a "HoloGlass" theme.
*   **Glassmorphism:** We combine `Brush.verticalGradient` with semi-transparent alphas (e.g., `0.5f`) and multiple `Surface` elevations (shadows) to make it look like frosted glass floating over a liquid background.
*   **Animations:** We use `animateDpAsState` with `spring` physics so the navigation dock pills bounce and expand naturally when you tap them.
*   **Live Maps:** We use `Osmdroid` attached to an `AndroidView` inside Compose to render an offline-capable, highly customized map pin pointing to your live location.

---

## 📂 Architecture Overview (MVVM & Hilt)

We followed modern Clean Architecture principles to keep the codebase stable and scalable:

*   **UI Layer (Screens & Components):** Contains Jetpack Compose files and custom animated modules.
*   **ViewModel Layer (`DashboardViewModel`):** The brain that holds the `StateFlow`. It survives screen rotations and talks to the rest of the app.
*   **Repository Layer:** The single source of truth (`IncidentRepository`). It decides whether to fetch data from the local database or the cloud.
*   **Room Database:** An SQLite database embedded on your phone to remember exactly when and where an incident happened.
*   **WorkManager (`SyncWorker`):** A guaranteed background worker that syncs your local incidents to the cloud silently when you finally get an internet connection.
*   **Hilt (Dependency Injection):** The delivery system! It automatically provides our tools (like our Repository and SmsHelper) exactly where they are needed across Services, Receivers, and ViewModels without us having to pass them around manually.

---

### Developed By
**Farouk** — 2026
*Project for Master Professionnel en Développement d'Applications Mobiles — ISET Rades*
