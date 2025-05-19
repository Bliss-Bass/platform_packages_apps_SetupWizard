This document describes how to remotely control Setup Wizard using `Messenger`.

## Getting Started

https://developer.android.com/develop/background-work/services/bound-services#Messenger

1.  **Binding to the Service:**
    Clients must bind to the service using an `Intent` targeting the service component.

    ```java
    Intent intent = new Intent();
    intent.setClassName("com.droidlogic.setupwizard", "com.droidlogic.setupwizard.services.MessengerService");
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    ```

2.  **Obtaining the `Messenger`:**
    Once the service connection is established, the `onServiceConnected` callback will provide the service's `Messenger` object. You'll use this to send messages.

    ```java
    private Messenger mService = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            // Now you can send messages using mService
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    ```

3. Required permission

        <uses-permission android:name="android.permission.CHANGE_CONFIGURATION" />

## Message Codes

| Code                              | Object (`Message.obj`)  / Argument (`Message.arg1`)                                                             |
|:----------------------------------|:----------------------------------------------------------------------------------------------------------------|
| `MSG_CHANGE_NAVIGATION_MODE = 1`  | Message.arg1 - NAVIGATION_MODE_GESTURE (0), NAVIGATION_MODE_TWO_BUTTON (1) or NAVIGATION_MODE_THREE_BUTTON (2); |
| `MSG_CHANGE_DATE_AND_TIME = 2`    | Message.obj(long) - time in milliseconds since the Epoch.                                                       |
| `MSG_CHANGE_LANGUAGE = 3`         | Message.obj(LocaleList) - The list of locales.                                                                  |
| `MSG_CHANGE_TIME_ZONE = 4`        | Message.obj(String) one of the Olson ids from the list returned by java.util.TimeZone.getAvailableIDs           |
| `MSG_OVERRIDE_SETUP_COMPLETE = 5` | Message.arg1 - Set to 1 or 0 to override<br/>(1) Setup is complete<br/>(0) Setup is incomplete                  |

## Testing the setup wizard

    adb shell settings put secure user_setup_complete 0
    adb shell pm enable com.droidlogic.setupwizard/com.droidlogic.setupwizard.MainActivity
    adb shell am start-activity com.droidlogic.setupwizard/.MainActivity        