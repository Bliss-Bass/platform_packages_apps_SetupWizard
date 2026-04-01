# Setup Wizard

This document describes how to remotely control the Setup Wizard and outlines its key features and compliance standards.

## Key Features & Compliance

### 1. California & Colorado Age Verification (AB 1043 / SB24-041)
Starting January 1, 2027, mandatory age assurance is required for account setup in specific jurisdictions.
- **Geo-Fencing:** The Age Verification screen automatically triggers only for users in **California** (Los_Angeles TZ) and **Colorado** (Denver TZ).
- **Privacy-First:** We do not store birthdates. We only set a persistent system signal (`persist.os.mandate.age_verification.is_minor`) for local security apps to act upon.
- **Anonymity:** Users outside regulated regions are never asked for this data, ensuring minimal data collection.

### 2. Zero-Gap Glass UI
The SetupWizard features a modern "Glass" aesthetic with perfectly synchronized background layers.
- **Aligned Layouts:** Left (Guidance) and Right (Actions) panes are synchronized via a shared `0.5833` guideline.
- **Seamless Transitions:** Persistent activity-level background layers eliminate visible seams or "hairline" gaps between screens.

---

## Remote Control via `Messenger`

https://developer.android.com/develop/background-work/services/bound-services#Messenger

1.  **Binding to the Service:**
    Clients must bind to the service using an `Intent` targeting the service component.

    ```java
    Intent intent = new Intent();
    intent.setClassName("com.droidlogic.setupwizard", "com.droidlogic.setupwizard.services.MessengerService");
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    ```

2.  **Obtaining the `Messenger`:**
    Once the service connection is established, the `onServiceConnected` callback will provide the service's `Messenger` object.

    ```java
    private Messenger mService = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    ```

3. **Required Permission**

    ```xml
    <uses-permission android:name="android.permission.CHANGE_CONFIGURATION" />
    ```

## Message Codes

| Code                              | Object (`Message.obj`)  / Argument (`Message.arg1`)                                                             |
|:----------------------------------|:----------------------------------------------------------------------------------------------------------------|
| `MSG_CHANGE_NAVIGATION_MODE = 1`  | Message.arg1 - GESTURE (0), TWO_BUTTON (1), THREE_BUTTON (2); |
| `MSG_CHANGE_DATE_AND_TIME = 2`    | Message.obj(long) - time in ms since Epoch.                                                       |
| `MSG_CHANGE_LANGUAGE = 3`         | Message.obj(LocaleList) - List of locales.                                                                  |
| `MSG_CHANGE_TIME_ZONE = 4`        | Message.obj(String) Olson ID (e.g., "America/Los_Angeles")          |
| `MSG_OVERRIDE_SETUP_COMPLETE = 5` | Message.arg1 - (1) Complete, (0) Incomplete                  |

## Testing the Setup Wizard

```bash
adb shell settings put secure user_setup_complete 0
adb shell pm enable com.droidlogic.setupwizard/com.droidlogic.setupwizard.MainActivity
adb shell am start-activity com.droidlogic.setupwizard/.MainActivity
```
