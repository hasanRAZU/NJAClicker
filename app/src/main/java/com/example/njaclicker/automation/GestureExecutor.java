
package com.example.njaclicker.automation;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;

import com.example.njaclicker.model.ActionType;
import com.example.njaclicker.model.AutomationAction;

public class GestureExecutor {

    private static final long TAP_DURATION_MS = 60;

    private final AccessibilityService accessibilityService;

    public GestureExecutor(AccessibilityService accessibilityService) {
        this.accessibilityService = accessibilityService;
    }

    public interface GestureCallback {

        void onCompleted();

        void onCancelled();

        void onFailed();
    }

    public boolean execute(
            AutomationAction action,
            GestureCallback callback
    ) {

        if (action == null || action.getType() == null) {
            notifyFailure(callback);
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            notifyFailure(callback);
            return false;
        }

        Path path = new Path();
        long duration;

        if (action.getType() == ActionType.TAP) {

            // A short, stationary gesture represents one tap.
            path.moveTo(action.getX(), action.getY());

            duration = TAP_DURATION_MS;

        } else if (action.getType() == ActionType.SWIPE) {

            // Start at the swipe starting position.
            path.moveTo(
                    action.getStartX(),
                    action.getStartY()
            );

            // Move continuously to the ending position.
            path.lineTo(
                    action.getEndX(),
                    action.getEndY()
            );

            duration = Math.max(1, action.getSwipeDurationMs());

        } else {
            notifyFailure(callback);
            return false;
        }

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(
                        path,
                        0,
                        duration
                );

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        return accessibilityService.dispatchGesture(
                gesture,
                new AccessibilityService.GestureResultCallback() {

                    @Override
                    public void onCompleted(
                            GestureDescription gestureDescription
                    ) {
                        super.onCompleted(gestureDescription);

                        if (callback != null) {
                            callback.onCompleted();
                        }
                    }

                    @Override
                    public void onCancelled(
                            GestureDescription gestureDescription
                    ) {
                        super.onCancelled(gestureDescription);

                        if (callback != null) {
                            callback.onCancelled();
                        }
                    }

                },
                null
        );
    }

    private void notifyFailure(GestureCallback callback) {

        if (callback != null) {
            callback.onFailed();
        }
    }
}
