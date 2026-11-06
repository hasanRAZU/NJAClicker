
package com.example.njaclicker.overlay;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.njaclicker.model.ActionType;
import com.example.njaclicker.model.AutomationAction;
import com.example.njaclicker.model.MarkerData;

import java.util.ArrayList;
import java.util.List;

public class MarkerManager {

    private final AccessibilityService service;
    private final WindowManager windowManager;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final List<MarkerEntry> markers =
            new ArrayList<>();

    private final List<AutomationAction> actions =
            new ArrayList<>();

    private final int touchSlop;
    private final int markerSize;

    private int tapCounter = 0;
    private int swipeCounter = 0;

    private boolean markersTouchable = true;

    private Listener listener;

    public interface Listener {

        void onActionsChanged(List<AutomationAction> actions);

        void onMarkerMoved(MarkerData marker);

        void onMarkerRemoved(String markerId);
    }

    public MarkerManager(
            AccessibilityService service,
            Listener listener
    ) {

        this.service = service;

        this.windowManager =
                (WindowManager) service.getSystemService(
                        AccessibilityService.WINDOW_SERVICE
                );

        this.listener = listener;

        this.touchSlop =
                ViewConfiguration.get(service).getScaledTouchSlop();

        this.markerSize = dp(56);
    }

    // --------------------------------------------------
    // TAP MARKER
    // --------------------------------------------------

    public AutomationAction addTapMarker(float x, float y) {

        tapCounter++;

        String markerId = "T" + tapCounter;

        AutomationAction action =
                new AutomationAction(ActionType.TAP);

        action.setX(x);
        action.setY(y);

        // Default delay after this tap.
        action.setDelayAfterMs(1000);

        MarkerData markerData =
                new MarkerData(
                        markerId,
                        ActionType.TAP,
                        x,
                        y
                );

        actions.add(action);

        createMarker(
                markerData,
                action,
                MarkerRole.TAP
        );

        notifyActionsChanged();

        return action;
    }

    // --------------------------------------------------
    // SWIPE MARKERS
    // --------------------------------------------------

    public AutomationAction addSwipeMarkers(
            float startX,
            float startY,
            float endX,
            float endY
    ) {

        swipeCounter++;

        String swipeId = "S" + swipeCounter;

        AutomationAction action =
                new AutomationAction(ActionType.SWIPE);

        action.setStartX(startX);
        action.setStartY(startY);

        action.setEndX(endX);
        action.setEndY(endY);

        action.setSwipeDurationMs(500);
        action.setDelayAfterMs(1000);

        MarkerData startMarker =
                new MarkerData(
                        swipeId,
                        ActionType.SWIPE,
                        startX,
                        startY
                );

        MarkerData endMarker =
                new MarkerData(
                        "E" + swipeCounter,
                        ActionType.SWIPE,
                        endX,
                        endY
                );

        actions.add(action);

        createMarker(
                startMarker,
                action,
                MarkerRole.SWIPE_START
        );

        createMarker(
                endMarker,
                action,
                MarkerRole.SWIPE_END
        );

        notifyActionsChanged();

        return action;
    }

    // --------------------------------------------------
    // CREATE MARKER VIEW
    // --------------------------------------------------

    private void createMarker(
            MarkerData markerData,
            AutomationAction action,
            MarkerRole role
    ) {

        TextView markerView = new TextView(service);

        markerView.setText(markerData.getId());
        markerView.setTextColor(Color.WHITE);
        markerView.setTextSize(14);
        markerView.setGravity(Gravity.CENTER);
        markerView.setElevation(dp(8));

        int backgroundColor =
                role == MarkerRole.TAP
                        ? Color.rgb(230, 160, 0)
                        : role == MarkerRole.SWIPE_START
                        ? Color.rgb(20, 150, 90)
                        : Color.rgb(200, 55, 55);

        GradientDrawable background =
                new GradientDrawable();

        background.setShape(GradientDrawable.OVAL);
        background.setColor(backgroundColor);
        background.setStroke(dp(2), Color.WHITE);

        markerView.setBackground(background);

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        markerSize,
                        markerSize,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        getMarkerFlags(),
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.TOP | Gravity.LEFT;

        // MarkerData stores the center point.
        // WindowManager positions the top-left corner.
        params.x = Math.round(markerData.getX() - markerSize / 2f);
        params.y = Math.round(markerData.getY() - markerSize / 2f);

        MarkerEntry entry =
                new MarkerEntry(
                        markerData,
                        action,
                        role,
                        markerView,
                        params
                );

        markerView.setOnTouchListener(
                (view, event) -> handleMarkerTouch(entry, event)
        );

        try {

            windowManager.addView(markerView, params);

            markers.add(entry);

        } catch (Exception e) {

            android.util.Log.e(
                    "MarkerManager",
                    "Unable to create marker: " + markerData.getId(),
                    e
            );

            // If marker creation fails, remove its action.
            removeAction(action);
        }
    }

    // --------------------------------------------------
    // MARKER TOUCH AND DRAG
    // --------------------------------------------------

    private boolean handleMarkerTouch(
            MarkerEntry entry,
            MotionEvent event
    ) {

        if (!markersTouchable) {
            return false;
        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                entry.initialRawX = event.getRawX();
                entry.initialRawY = event.getRawY();

                entry.initialWindowX = entry.params.x;
                entry.initialWindowY = entry.params.y;

                entry.moved = false;
                entry.longPressed = false;

                entry.longPressRunnable = () -> {

                    if (!entry.moved) {

                        entry.longPressed = true;

                        removeAction(entry.action);
                    }
                };

                handler.postDelayed(
                        entry.longPressRunnable,
                        ViewConfiguration.getLongPressTimeout()
                );

                return true;

            case MotionEvent.ACTION_MOVE:

                float differenceX =
                        event.getRawX() - entry.initialRawX;

                float differenceY =
                        event.getRawY() - entry.initialRawY;

                if (Math.abs(differenceX) > touchSlop ||
                        Math.abs(differenceY) > touchSlop) {

                    entry.moved = true;

                    handler.removeCallbacks(
                            entry.longPressRunnable
                    );
                }

                if (entry.moved && !entry.longPressed) {

                    entry.params.x =
                            entry.initialWindowX +
                                    Math.round(differenceX);

                    entry.params.y =
                            entry.initialWindowY +
                                    Math.round(differenceY);

                    updateMarkerCoordinates(entry);

                    try {

                        windowManager.updateViewLayout(
                                entry.view,
                                entry.params
                        );

                    } catch (Exception e) {

                        android.util.Log.e(
                                "MarkerManager",
                                "Unable to move marker",
                                e
                        );
                    }
                }

                return true;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:

                handler.removeCallbacks(
                        entry.longPressRunnable
                );

                if (entry.moved && !entry.longPressed) {

                    notifyMarkerMoved(entry.markerData);
                }

                return true;
        }

        return true;
    }

    // --------------------------------------------------
    // UPDATE ACTION COORDINATES
    // --------------------------------------------------

    private void updateMarkerCoordinates(MarkerEntry entry) {

        float centerX =
                entry.params.x + markerSize / 2f;

        float centerY =
                entry.params.y + markerSize / 2f;

        entry.markerData.setX(centerX);
        entry.markerData.setY(centerY);

        switch (entry.role) {

            case TAP:

                entry.action.setX(centerX);
                entry.action.setY(centerY);

                break;

            case SWIPE_START:

                entry.action.setStartX(centerX);
                entry.action.setStartY(centerY);

                break;

            case SWIPE_END:

                entry.action.setEndX(centerX);
                entry.action.setEndY(centerY);

                break;
        }
    }

    // --------------------------------------------------
    // REMOVE MARKER / ACTION
    // --------------------------------------------------

    private void removeAction(AutomationAction action) {

        if (action == null) {
            return;
        }

        actions.remove(action);

        List<MarkerEntry> entriesToRemove =
                new ArrayList<>();

        for (MarkerEntry entry : markers) {

            if (entry.action == action) {
                entriesToRemove.add(entry);
            }
        }

        for (MarkerEntry entry : entriesToRemove) {

            handler.removeCallbacks(
                    entry.longPressRunnable
            );

            try {

                windowManager.removeView(entry.view);

            } catch (Exception ignored) {
            }

            markers.remove(entry);

            if (listener != null) {

                listener.onMarkerRemoved(
                        entry.markerData.getId()
                );
            }
        }

        notifyActionsChanged();
    }

    // --------------------------------------------------
    // CLEAR ALL
    // --------------------------------------------------

    public void clearMarkers() {

        for (MarkerEntry entry :
                new ArrayList<>(markers)) {

            handler.removeCallbacks(
                    entry.longPressRunnable
            );

            try {

                windowManager.removeView(entry.view);

            } catch (Exception ignored) {
            }

            if (listener != null) {

                listener.onMarkerRemoved(
                        entry.markerData.getId()
                );
            }
        }

        markers.clear();
        actions.clear();

        tapCounter = 0;
        swipeCounter = 0;

        notifyActionsChanged();
    }

    // --------------------------------------------------
    // TOUCHABLE / NON-TOUCHABLE MARKERS
    // --------------------------------------------------

    public void setMarkersTouchable(boolean touchable) {

        markersTouchable = touchable;

        for (MarkerEntry entry : markers) {

            entry.params.flags = getMarkerFlags();

            try {

                windowManager.updateViewLayout(
                        entry.view,
                        entry.params
                );

            } catch (Exception e) {

                android.util.Log.e(
                        "MarkerManager",
                        "Unable to update marker touch state",
                        e
                );
            }
        }
    }

    private int getMarkerFlags() {

        int flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        if (!markersTouchable) {

            flags |=
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        return flags;
    }

    // --------------------------------------------------
    // GET ACTIONS
    // --------------------------------------------------

    public List<AutomationAction> getActions() {

        return new ArrayList<>(actions);
    }

    public int getActionCount() {

        return actions.size();
    }

    // --------------------------------------------------
    // LISTENER NOTIFICATIONS
    // --------------------------------------------------

    private void notifyActionsChanged() {

        if (listener != null) {

            listener.onActionsChanged(
                    getActions()
            );
        }
    }

    private void notifyMarkerMoved(MarkerData marker) {

        if (listener != null) {

            listener.onMarkerMoved(marker);
        }
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private int dp(float value) {

        return Math.round(
                value * service.getResources()
                        .getDisplayMetrics().density
        );
    }

    // --------------------------------------------------
    // INTERNAL MARKER ENTRY
    // --------------------------------------------------

    private enum MarkerRole {

        TAP,
        SWIPE_START,
        SWIPE_END
    }

    private static class MarkerEntry {

        final MarkerData markerData;

        final AutomationAction action;

        final MarkerRole role;

        final TextView view;

        final WindowManager.LayoutParams params;

        float initialRawX;
        float initialRawY;

        int initialWindowX;
        int initialWindowY;

        boolean moved;
        boolean longPressed;

        Runnable longPressRunnable;

        MarkerEntry(
                MarkerData markerData,
                AutomationAction action,
                MarkerRole role,
                TextView view,
                WindowManager.LayoutParams params
        ) {

            this.markerData = markerData;
            this.action = action;
            this.role = role;
            this.view = view;
            this.params = params;
        }
    }
}
