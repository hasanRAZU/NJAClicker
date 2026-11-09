# NJAClicker

**NJAClicker** is an Android auto-clicker application that automates screen interactions using configurable tap and swipe actions. It provides a floating control panel and draggable action markers, allowing users to create and execute automation sequences over other applications.

## Features

- **Floating Control Panel:** Control automation without leaving the currently opened application.
- **Draggable Tap Markers:** Select and position tap locations anywhere on the screen.
- **Swipe Automation:** Configure swipe start and end positions.
- **Mixed Action Sequences:** Execute tap and swipe actions in a defined sequence.
- **Individual Action Delays:** Set a separate delay after each tap or swipe action.
- **Custom Swipe Duration:** Configure how long each swipe gesture takes.
- **Automatic Repetition:** Repeat the entire automation sequence automatically.
- **Save Configurations:** Save automation sequences with custom names.
- **Load Configurations:** Restore previously saved automation sequences.
- **Delete Configurations:** Remove saved configurations when they are no longer needed.
- **Action Management:** Remove the last action or clear all configured actions.
- **Action Settings:** Edit individual action delays and swipe durations.
- **Visual Execution Feedback:** Track the currently executing action through marker highlighting.
- **Minimized Floating Bubble:** Minimize the control panel into a movable floating bubble.

## Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Java |
| IDE | Android Studio |
| User Interface | XML and Android Views |
| Minimum Android Version | Android 14 (API 34) |
| Application ID | `com.example.njaclicker` |
| Automation | Android Accessibility Service |
| Floating Interface | Android Accessibility Overlay |
| Local Storage | SharedPreferences |
| Configuration Format | JSON |

## Project Structure

```text
com.example.njaclicker
│
├── MainActivity.java
│
├── accessibility/
│   └── AutoClickAccessibilityService.java
│
├── automation/
│   ├── AutomationController.java
│   └── GestureExecutor.java
│
├── model/
│   ├── ActionType.java
│   ├── AutomationAction.java
│   └── MarkerData.java
│
├── overlay/
│   ├── FloatingPanel.java
│   └── MarkerManager.java
│
├── storage/
│   └── AutomationStorage.java
│
└── util/
    └── ScreenUtils.java
```

## How It Works

1. Launch NJAClicker and enable its Accessibility Service when prompted.
2. Open the floating control panel.
3. Add tap or swipe actions.
4. Drag the markers to the desired screen positions.
5. Tap an action in the panel to configure its delay or swipe duration.
6. Enable automatic repetition if required.
7. Press **START** to execute the configured sequence.
8. Press **STOP** to stop automation.

The automation controller executes actions in their configured order. Each action can have its own delay before the next action is executed.

## Configuration Management

NJAClicker supports saving and managing multiple automation configurations.

- **Save Configuration:** Store the current sequence under a custom name.
- **Load Configuration:** Restore a previously saved sequence.
- **Delete Configuration:** Permanently remove a saved sequence after confirmation.

Configurations are stored locally using Android SharedPreferences in JSON format. No external database or internet connection is required for configuration storage.

## Permissions

NJAClicker requires the Android Accessibility Service to perform automated screen gestures and an accessibility overlay to display its floating controls and markers.

These permissions must be enabled by the user through Android system settings.

The Accessibility Service is used to execute user-configured automation actions. Users should only automate applications and activities they are authorized to control.

## Requirements

- Android Studio
- Java Development Kit compatible with the installed Android Gradle Plugin
- Android SDK API 34 or higher
- Android device running Android 14 or higher
- Accessibility Service enabled
- Display-over-other-apps permission, if required by the device configuration

## Build and Run

1. Clone the repository:

   ```bash
   git clone https://github.com/hasanRAZU/NJAClicker.git
   ```

2. Open the project in Android Studio.

3. Allow Gradle synchronization to finish.

4. Connect an Android device running Android 14 or higher.

5. Build and run the application.

6. Enable the NJAClicker Accessibility Service in Android Settings.

## Limitations

- Automation depends on Android Accessibility Service capabilities and system restrictions.
- Some applications may restrict automated gestures.
- Gesture execution may vary depending on screen size, display scaling, and application behavior.
- Android system dialogs and protected interfaces may not allow automation.
- The application must remain operational through its Accessibility Service while automation is running.

## Privacy

NJAClicker stores saved automation configurations locally on the device.

It does not require an account, backend server, or internet connection for its core automation and configuration features.

Accessibility access is required for automation functionality. Users should review the permissions and understand their purpose before enabling the service.

## License

No license has been specified for this project yet.
