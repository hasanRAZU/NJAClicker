
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

    // Identifies the current execution session.
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

    // Start or restart the automation sequence.
    public void start(List<AutomationAction> actionList) {

        if (actionList == null || actionList.isEmpty()) {

            notifyError("No actions available.");
            return;
        }

        stop();

        // Copy the list to protect the running sequence
        // from changes to the original list structure.
        actions = new ArrayList<>(actionList);

        currentIndex = 0;
        isRunning = true;

        final int currentSession = sessionId;

        executeCurrentAction(currentSession);
    }

    private void executeCurrentAction(int currentSession) {

        if (!isRunning || currentSession != sessionId) {
            return;
        }

        if (actions.isEmpty()) {
            stop();
            return;
        }

        if (currentIndex >= actions.size()) {
            currentIndex = 0;
        }

        final int actionIndex = currentIndex;

        AutomationAction action = actions.get(actionIndex);

        if (action == null) {
            handleError("Invalid action at position " + (actionIndex + 1));
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

                        // Move to the next action.
                        currentIndex++;

                        // Wait for this action's own delay.
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

            handleError("Android rejected the gesture request.");
        }
    }

    // Stop future actions.
    public void stop() {

        boolean wasRunning = isRunning;

        isRunning = false;

        // Invalidate callbacks from the previous session.
        sessionId++;

        // Remove pending delays.
        handler.removeCallbacksAndMessages(null);

        if (wasRunning && listener != null) {
            listener.onStopped();
        }
    }

    private void handleError(String message) {

        stop();

        notifyError(message);
    }

    private void notifyError(String message) {

        if (listener != null) {
            listener.onError(message);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
