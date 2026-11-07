
package com.example.njaclicker.automation;

import android.os.Handler;
import android.os.Looper;

import com.example.njaclicker.model.AutomationAction;

import java.util.ArrayList;
import java.util.List;

public class AutomationController {

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final GestureExecutor gestureExecutor;

    private List<AutomationAction> actions =
            new ArrayList<>();

    private int currentIndex = 0;

    private boolean isRunning = false;
    private boolean repeatEnabled = false;

    private int sessionId = 0;

    private ControllerListener listener;

    public AutomationController(GestureExecutor gestureExecutor) {
        this.gestureExecutor = gestureExecutor;
    }

    public interface ControllerListener {

        void onActionStarted(int actionIndex);

        void onActionCompleted(int actionIndex);

        void onError(String message);

        void onStopped();
    }

    public void setListener(ControllerListener listener) {
        this.listener = listener;
    }

    // =====================================
    // START WITHOUT REPEAT
    // =====================================

    public void start(List<AutomationAction> actionList) {
        start(actionList, false);
    }

    // =====================================
    // START WITH REPEAT OPTION
    // =====================================

    public void start(
            List<AutomationAction> actionList,
            boolean repeat
    ) {

        if (actionList == null || actionList.isEmpty()) {
            notifyError("No actions available.");
            return;
        }

        stop();

        actions = new ArrayList<>(actionList);

        currentIndex = 0;
        repeatEnabled = repeat;
        isRunning = true;

        final int currentSession = sessionId;

        executeCurrentAction(currentSession);
    }

    // =====================================
    // EXECUTE CURRENT ACTION
    // =====================================

    private void executeCurrentAction(int currentSession) {

        if (!isRunning || currentSession != sessionId) {
            return;
        }

        // End of sequence
        if (currentIndex >= actions.size()) {

            if (repeatEnabled) {

                // Start again from the first action
                currentIndex = 0;

            } else {

                stop();
                return;
            }
        }

        final int actionIndex = currentIndex;

        AutomationAction action = actions.get(actionIndex);

        if (action == null) {
            handleError(
                    "Invalid action at position " + (actionIndex + 1)
            );
            return;
        }

        if (listener != null) {
            listener.onActionStarted(actionIndex);
        }

        boolean accepted = gestureExecutor.execute(
                action,
                new GestureExecutor.GestureCallback() {

                    @Override
                    public void onCompleted() {

                        if (!isRunning ||
                                currentSession != sessionId) {
                            return;
                        }

                        if (listener != null) {
                            listener.onActionCompleted(actionIndex);
                        }

                        currentIndex++;

                        // Each action has its own delay.
                        long delay = Math.max(
                                0,
                                action.getDelayAfterMs()
                        );

                        handler.postDelayed(
                                () -> executeCurrentAction(currentSession),
                                delay
                        );
                    }

                    @Override
                    public void onCancelled() {

                        if (isRunning &&
                                currentSession == sessionId) {

                            handleError("Gesture was cancelled.");
                        }
                    }

                    @Override
                    public void onFailed() {

                        if (isRunning &&
                                currentSession == sessionId) {

                            handleError("Unable to perform gesture.");
                        }
                    }
                }
        );

        if (!accepted &&
                isRunning &&
                currentSession == sessionId) {

            handleError(
                    "Android rejected the gesture request."
            );
        }
    }

    // =====================================
    // STOP
    // =====================================

    public void stop() {

        boolean wasRunning = isRunning;

        isRunning = false;
        repeatEnabled = false;

        sessionId++;

        handler.removeCallbacksAndMessages(null);

        if (wasRunning && listener != null) {
            listener.onStopped();
        }
    }

    // =====================================
    // ERROR HANDLING
    // =====================================

    private void handleError(String message) {

        stop();

        notifyError(message);
    }

    private void notifyError(String message) {

        if (listener != null) {
            listener.onError(message);
        }
    }

    // =====================================
    // GETTERS
    // =====================================

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isRepeatEnabled() {
        return repeatEnabled;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
