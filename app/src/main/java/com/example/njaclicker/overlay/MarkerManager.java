
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

    private static final long DEFAULT_DELAY_MS = 2000;
    private static final long DEFAULT_SWIPE_DURATION_MS = 500;

    private static final int ACTIVE_COLOR = Color.rgb(40, 130, 255);

    private final AccessibilityService service;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<MarkerEntry> markers = new ArrayList<>();
    private final List<AutomationAction> actions = new ArrayList<>();

    private final int touchSlop;
    private final int markerSize;
    private final Listener listener;

    private int tapCounter = 0;
    private int swipeCounter = 0;
    private int activeActionIndex = -1;

    private boolean markersTouchable = true;

    public interface Listener {
        void onActionsChanged(List<AutomationAction> actions);
        void onMarkerMoved(MarkerData marker);
        void onMarkerRemoved(String markerId);
    }

    public MarkerManager(AccessibilityService service, Listener listener) {
        this.service = service;
        this.listener = listener;

        windowManager = (WindowManager) service.getSystemService(
                AccessibilityService.WINDOW_SERVICE
        );

        touchSlop = ViewConfiguration.get(service).getScaledTouchSlop();
        markerSize = dp(56);
    }

    public AutomationAction addTapMarker(float x, float y) {
        tapCounter++;

        AutomationAction action = new AutomationAction(ActionType.TAP);
        action.setX(x);
        action.setY(y);
        action.setDelayAfterMs(DEFAULT_DELAY_MS);

        actions.add(action);

        MarkerData data = new MarkerData(
                "T" + tapCounter,
                ActionType.TAP,
                x,
                y
        );

        createMarker(data, action, MarkerRole.TAP);
        notifyActionsChanged();

        return action;
    }

    public AutomationAction addSwipeMarkers(
            float startX,
            float startY,
            float endX,
            float endY
    ) {
        swipeCounter++;

        AutomationAction action = new AutomationAction(ActionType.SWIPE);
        action.setStartX(startX);
        action.setStartY(startY);
        action.setEndX(endX);
        action.setEndY(endY);
        action.setSwipeDurationMs(DEFAULT_SWIPE_DURATION_MS);
        action.setDelayAfterMs(DEFAULT_DELAY_MS);

        actions.add(action);

        createMarker(
                new MarkerData("S" + swipeCounter, ActionType.SWIPE, startX, startY),
                action,
                MarkerRole.SWIPE_START
        );

        createMarker(
                new MarkerData("E" + swipeCounter, ActionType.SWIPE, endX, endY),
                action,
                MarkerRole.SWIPE_END
        );

        notifyActionsChanged();
        return action;
    }

    // Removes the newest action, regardless of whether it is a tap or swipe.
    public boolean removeLastAction() {
        if (actions.isEmpty()) {
            return false;
        }

        removeAction(actions.get(actions.size() - 1));
        return true;
    }

    // Highlight the action currently being executed.
    // For a swipe, both its start and end markers are highlighted.
    public void setActiveActionIndex(int index) {
        activeActionIndex = index;

        AutomationAction activeAction =
                index >= 0 && index < actions.size()
                        ? actions.get(index)
                        : null;

        for (MarkerEntry entry : markers) {
            if (activeAction != null && entry.action == activeAction) {
                setMarkerColor(entry, ACTIVE_COLOR);
            } else {
                setMarkerColor(entry, entry.normalColor);
            }
        }
    }

    private void setMarkerColor(MarkerEntry entry, int color) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        background.setStroke(dp(2), Color.WHITE);
        entry.view.setBackground(background);
    }

    public void replaceActions(List<AutomationAction> newActions) {
        clearMarkerViews();
        actions.clear();

        tapCounter = 0;
        swipeCounter = 0;
        activeActionIndex = -1;

        if (newActions != null) {
            for (AutomationAction action : newActions) {
                if (action == null || action.getType() == null) {
                    continue;
                }

                actions.add(action);

                if (action.getType() == ActionType.TAP) {
                    tapCounter++;

                    createMarker(
                            new MarkerData(
                                    "T" + tapCounter,
                                    ActionType.TAP,
                                    action.getX(),
                                    action.getY()
                            ),
                            action,
                            MarkerRole.TAP
                    );

                } else if (action.getType() == ActionType.SWIPE) {
                    swipeCounter++;

                    createMarker(
                            new MarkerData(
                                    "S" + swipeCounter,
                                    ActionType.SWIPE,
                                    action.getStartX(),
                                    action.getStartY()
                            ),
                            action,
                            MarkerRole.SWIPE_START
                    );

                    createMarker(
                            new MarkerData(
                                    "E" + swipeCounter,
                                    ActionType.SWIPE,
                                    action.getEndX(),
                                    action.getEndY()
                            ),
                            action,
                            MarkerRole.SWIPE_END
                    );
                }
            }
        }

        notifyActionsChanged();
    }

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

        int normalColor;

        if (role == MarkerRole.TAP) {
            normalColor = Color.rgb(230, 160, 0);
        } else if (role == MarkerRole.SWIPE_START) {
            normalColor = Color.rgb(20, 150, 90);
        } else {
            normalColor = Color.rgb(200, 55, 55);
        }

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(normalColor);
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
        params.x = Math.round(markerData.getX() - markerSize / 2f);
        params.y = Math.round(markerData.getY() - markerSize / 2f);

        MarkerEntry entry = new MarkerEntry(
                markerData,
                action,
                role,
                markerView,
                params,
                normalColor
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
        }
    }

    private boolean handleMarkerTouch(MarkerEntry entry, MotionEvent event) {
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
                float dx = event.getRawX() - entry.initialRawX;
                float dy = event.getRawY() - entry.initialRawY;

                if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                    entry.moved = true;
                    handler.removeCallbacks(entry.longPressRunnable);
                }

                if (entry.moved && !entry.longPressed) {
                    entry.params.x = entry.initialWindowX + Math.round(dx);
                    entry.params.y = entry.initialWindowY + Math.round(dy);

                    updateMarkerCoordinates(entry);

                    try {
                        windowManager.updateViewLayout(entry.view, entry.params);
                    } catch (Exception e) {
                        android.util.Log.e("MarkerManager", "Unable to move marker", e);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(entry.longPressRunnable);

                if (entry.moved && !entry.longPressed) {
                    notifyMarkerMoved(entry.markerData);
                }
                return true;
        }

        return true;
    }

    private void updateMarkerCoordinates(MarkerEntry entry) {
        float centerX = entry.params.x + markerSize / 2f;
        float centerY = entry.params.y + markerSize / 2f;

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

    private void removeAction(AutomationAction action) {
        if (action == null) {
            return;
        }

        actions.remove(action);

        List<MarkerEntry> removeList = new ArrayList<>();

        for (MarkerEntry entry : markers) {
            if (entry.action == action) {
                removeList.add(entry);
            }
        }

        for (MarkerEntry entry : removeList) {
            handler.removeCallbacks(entry.longPressRunnable);

            try {
                windowManager.removeView(entry.view);
            } catch (Exception ignored) {
            }

            markers.remove(entry);

            if (listener != null) {
                listener.onMarkerRemoved(entry.markerData.getId());
            }
        }

        activeActionIndex = -1;
        notifyActionsChanged();
    }

    public void clearMarkers() {
        clearMarkerViews();
        actions.clear();

        tapCounter = 0;
        swipeCounter = 0;
        activeActionIndex = -1;

        notifyActionsChanged();
    }

    private void clearMarkerViews() {
        for (MarkerEntry entry : new ArrayList<>(markers)) {
            handler.removeCallbacks(entry.longPressRunnable);

            try {
                windowManager.removeView(entry.view);
            } catch (Exception ignored) {
            }

            if (listener != null) {
                listener.onMarkerRemoved(entry.markerData.getId());
            }
        }

        markers.clear();
    }

    public void setMarkersTouchable(boolean touchable) {
        markersTouchable = touchable;

        for (MarkerEntry entry : markers) {
            entry.params.flags = getMarkerFlags();

            try {
                windowManager.updateViewLayout(entry.view, entry.params);
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        if (!markersTouchable) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        return flags;
    }

    public List<AutomationAction> getActions() {
        return new ArrayList<>(actions);
    }

    public int getActionCount() {
        return actions.size();
    }

    private void notifyActionsChanged() {
        if (listener != null) {
            listener.onActionsChanged(getActions());
        }
    }

    private void notifyMarkerMoved(MarkerData marker) {
        if (listener != null) {
            listener.onMarkerMoved(marker);
        }
    }

    private int dp(float value) {
        return Math.round(
                value * service.getResources().getDisplayMetrics().density
        );
    }

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
        final int normalColor;

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
                WindowManager.LayoutParams params,
                int normalColor
        ) {
            this.markerData = markerData;
            this.action = action;
            this.role = role;
            this.view = view;
            this.params = params;
            this.normalColor = normalColor;
        }
    }
}
