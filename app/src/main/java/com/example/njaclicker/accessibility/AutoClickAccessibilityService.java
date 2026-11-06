
package com.example.njaclicker.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoClickAccessibilityService
        extends AccessibilityService {

    private static AutoClickAccessibilityService instance;

    private WindowManager windowManager;

    private View floatingPanel;
    private WindowManager.LayoutParams panelParams;

    private TextView tvSettings;
    private TextView tvAutomationStatus;

    private int initialX;
    private int initialY;

    private float initialTouchX;
    private float initialTouchY;

    // Marker types
    private static final int TYPE_TAP = 1;
    private static final int TYPE_SWIPE_START = 2;
    private static final int TYPE_SWIPE_END = 3;

    // Automation action types
    private static final int ACTION_TAP = 1;
    private static final int ACTION_SWIPE = 2;

    // Automation settings
    private long actionIntervalMs = 1000;
    private long swipeDurationMs = 500;

    private int tapCounter = 0;
    private int swipeCounter = 0;

    private boolean isRunning = false;

    private int currentActionIndex = 0;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    // Marker information
    public static class MarkerData {

        public final String label;
        public final int type;

        public float x;
        public float y;

        MarkerData(String label, int type, float x, float y) {
            this.label = label;
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    // One complete automation action
    private static class AutomationAction {

        int type;

        MarkerData first;
        MarkerData second;

        AutomationAction(
                int type,
                MarkerData first,
                MarkerData second
        ) {
            this.type = type;
            this.first = first;
            this.second = second;
        }
    }

    // Floating marker view
    private static class MarkerView {

        MarkerData data;

        TextView view;

        WindowManager.LayoutParams params;
    }

    private final List<MarkerData> markers =
            new ArrayList<>();

    private final List<MarkerView> markerViews =
            new ArrayList<>();

    // Actions are stored in creation order.
    private final List<AutomationAction> actionSequence =
            new ArrayList<>();

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        instance = this;

        windowManager = getSystemService(WindowManager.class);
    }

    public static AutoClickAccessibilityService getInstance() {
        return instance;
    }

    // =====================================
    // FLOATING CONTROL PANEL
    // =====================================

    public void showControlPanel() {

        if (windowManager == null || floatingPanel != null) {
            return;
        }

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);

        root.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        GradientDrawable background = new GradientDrawable();

        background.setColor(Color.rgb(35, 38, 48));
        background.setCornerRadius(dp(16));

        root.setBackground(background);
        root.setElevation(dp(10));

        // Header
        TextView header = new TextView(this);

        header.setText("NJAClicker  ⋮⋮");
        header.setTextColor(Color.WHITE);
        header.setTextSize(18);
        header.setGravity(Gravity.CENTER);

        header.setPadding(0, dp(8), 0, dp(12));

        root.addView(header);

        // Settings information
        tvSettings = new TextView(this);

        tvSettings.setTextColor(Color.WHITE);
        tvSettings.setTextSize(12);
        tvSettings.setPadding(0, dp(5), 0, dp(8));

        root.addView(tvSettings);

        updateSettingsText();

        // Interval controls
        LinearLayout intervalRow = new LinearLayout(this);

        intervalRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnIntervalMinus = createButton("Delay -");

        btnIntervalMinus.setOnClickListener(v -> {

            actionIntervalMs = Math.max(
                    100,
                    actionIntervalMs - 100
            );

            updateSettingsText();
        });

        Button btnIntervalPlus = createButton("Delay +");

        btnIntervalPlus.setOnClickListener(v -> {

            actionIntervalMs = Math.min(
                    10000,
                    actionIntervalMs + 100
            );

            updateSettingsText();
        });

        intervalRow.addView(btnIntervalMinus);
        intervalRow.addView(btnIntervalPlus);

        root.addView(intervalRow);

        // Swipe duration controls
        LinearLayout swipeRow = new LinearLayout(this);

        swipeRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSwipeMinus = createButton("Swipe -");

        btnSwipeMinus.setOnClickListener(v -> {

            swipeDurationMs = Math.max(
                    100,
                    swipeDurationMs - 100
            );

            updateSettingsText();
        });

        Button btnSwipePlus = createButton("Swipe +");

        btnSwipePlus.setOnClickListener(v -> {

            swipeDurationMs = Math.min(
                    5000,
                    swipeDurationMs + 100
            );

            updateSettingsText();
        });

        swipeRow.addView(btnSwipeMinus);
        swipeRow.addView(btnSwipePlus);

        root.addView(swipeRow);

        // Add Tap
        Button btnAddTap = createButton("Add Tap");

        btnAddTap.setOnClickListener(v -> addTapMarker());

        root.addView(btnAddTap);

        // Add Swipe
        Button btnAddSwipe = createButton("Add Swipe");

        btnAddSwipe.setOnClickListener(v -> addSwipeMarkers());

        root.addView(btnAddSwipe);

        // Clear
        Button btnClear = createButton("Clear All Markers");

        btnClear.setOnClickListener(v -> {

            if (isRunning) {
                showMessage("Stop automation first.");
                return;
            }

            clearMarkers();
        });

        root.addView(btnClear);

        // Start
        Button btnStart = createButton("START");

        btnStart.setBackgroundColor(
                Color.rgb(22, 140, 85)
        );

        btnStart.setTextColor(Color.WHITE);

        btnStart.setOnClickListener(v -> startAutomation());

        root.addView(btnStart);

        // Stop
        Button btnStop = createButton("STOP");

        btnStop.setBackgroundColor(
                Color.rgb(190, 55, 55)
        );

        btnStop.setTextColor(Color.WHITE);

        btnStop.setOnClickListener(v -> stopAutomation());

        root.addView(btnStop);

        // Automation status
        tvAutomationStatus = new TextView(this);

        tvAutomationStatus.setTextColor(Color.WHITE);
        tvAutomationStatus.setTextSize(12);
        tvAutomationStatus.setPadding(0, dp(8), 0, dp(8));

        root.addView(tvAutomationStatus);

        updateStatusText();

        // Close panel
        Button btnClose = createButton("CLOSE PANEL");

        btnClose.setOnClickListener(v -> {

            if (isRunning) {
                stopAutomation();
            }

            removeControlPanel();
        });

        root.addView(btnClose);

        // Drag panel
        header.setOnTouchListener((view, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    initialX = panelParams.x;
                    initialY = panelParams.y;

                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();

                    return true;

                case MotionEvent.ACTION_MOVE:

                    panelParams.x = initialX
                            + (int) (event.getRawX() - initialTouchX);

                    panelParams.y = initialY
                            + (int) (event.getRawY() - initialTouchY);

                    windowManager.updateViewLayout(
                            floatingPanel,
                            panelParams
                    );

                    return true;

                case MotionEvent.ACTION_UP:
                    return true;
            }

            return false;
        });

        panelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        panelParams.gravity = Gravity.TOP | Gravity.START;

        panelParams.x = dp(20);
        panelParams.y = dp(100);

        floatingPanel = root;

        try {

            windowManager.addView(
                    floatingPanel,
                    panelParams
            );

        } catch (Exception e) {

            floatingPanel = null;

            showMessage("Unable to display controls.");
        }
    }

    // =====================================
    // ADD TAP ACTION
    // =====================================

    private void addTapMarker() {

        if (isRunning) return;

        tapCounter++;

        String label = "T" + tapCounter;

        MarkerData marker = addMarker(
                label,
                TYPE_TAP,
                getScreenWidth() / 2f,
                getScreenHeight() / 2f
        );

        if (marker != null) {

            actionSequence.add(
                    new AutomationAction(
                            ACTION_TAP,
                            marker,
                            null
                    )
            );

            showMessage(label + " added to sequence.");
        }
    }

    // =====================================
    // ADD SWIPE ACTION
    // =====================================

    private void addSwipeMarkers() {

        if (isRunning) return;

        swipeCounter++;

        String startLabel = "S" + swipeCounter;
        String endLabel = "E" + swipeCounter;

        float centerX = getScreenWidth() / 2f;
        float centerY = getScreenHeight() / 2f;

        MarkerData start = addMarker(
                startLabel,
                TYPE_SWIPE_START,
                centerX - dp(65),
                centerY
        );

        MarkerData end = addMarker(
                endLabel,
                TYPE_SWIPE_END,
                centerX + dp(65),
                centerY
        );

        if (start != null && end != null) {

            actionSequence.add(
                    new AutomationAction(
                            ACTION_SWIPE,
                            start,
                            end
                    )
            );

            showMessage("Swipe " + swipeCounter + " added.");
        }
    }

    // =====================================
    // CREATE MARKER
    // =====================================

    private MarkerData addMarker(
            String label,
            int type,
            float x,
            float y
    ) {

        MarkerData data = new MarkerData(
                label,
                type,
                x,
                y
        );

        TextView marker = new TextView(this);

        marker.setText(label);
        marker.setTextColor(Color.WHITE);
        marker.setTextSize(13);
        marker.setGravity(Gravity.CENTER);
        marker.setElevation(dp(8));

        marker.setBackground(
                createMarkerBackground(type)
        );

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        dp(48),
                        dp(48),
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.TOP | Gravity.START;

        params.x = Math.max(
                0,
                Math.min(
                        (int) x - dp(24),
                        getScreenWidth() - dp(48)
                )
        );

        params.y = Math.max(
                0,
                Math.min(
                        (int) y - dp(24),
                        getScreenHeight() - dp(48)
                )
        );

        MarkerView markerView = new MarkerView();

        markerView.data = data;
        markerView.view = marker;
        markerView.params = params;

        marker.setOnTouchListener((view, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    initialX = params.x;
                    initialY = params.y;

                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();

                    view.setTag(
                            new float[]{
                                    event.getRawX(),
                                    event.getRawY()
                            }
                    );

                    return true;

                case MotionEvent.ACTION_MOVE:

                    params.x = initialX
                            + (int) (event.getRawX() - initialTouchX);

                    params.y = initialY
                            + (int) (event.getRawY() - initialTouchY);

                    params.x = Math.max(
                            0,
                            Math.min(
                                    params.x,
                                    getScreenWidth() - dp(48)
                            )
                    );

                    params.y = Math.max(
                            0,
                            Math.min(
                                    params.y,
                                    getScreenHeight() - dp(48)
                            )
                    );

                    windowManager.updateViewLayout(
                            marker,
                            params
                    );

                    data.x = params.x + dp(24);
                    data.y = params.y + dp(24);

                    return true;

                case MotionEvent.ACTION_UP:

                    float[] start = (float[]) view.getTag();

                    float movedX = Math.abs(
                            event.getRawX() - start[0]
                    );

                    float movedY = Math.abs(
                            event.getRawY() - start[1]
                    );

                    if (event.getEventTime()
                            - event.getDownTime() > 600
                            && movedX < dp(10)
                            && movedY < dp(10)) {

                        removeMarker(markerView);

                        showMessage(label + " removed.");
                    }

                    return true;
            }

            return false;
        });

        try {

            windowManager.addView(marker, params);

            markers.add(data);
            markerViews.add(markerView);

            return data;

        } catch (Exception e) {

            showMessage("Unable to add marker.");

            return null;
        }
    }

    // =====================================
    // AUTOMATION ENGINE
    // =====================================

    private void startAutomation() {

        if (isRunning) {
            showMessage("Automation is already running.");
            return;
        }

        if (actionSequence.isEmpty()) {
            showMessage("Please add at least one action.");
            return;
        }

        isRunning = true;

        currentActionIndex = 0;

        // Make markers pass-through while running.
        setMarkersTouchable(false);

        updateStatusText();

        showMessage("Automation started.");

        executeNextAction();
    }

    private void executeNextAction() {

        if (!isRunning) {
            return;
        }

        if (actionSequence.isEmpty()) {
            stopAutomation();
            return;
        }

        if (currentActionIndex >= actionSequence.size()) {
            currentActionIndex = 0;
        }

        AutomationAction action =
                actionSequence.get(currentActionIndex);

        Path path = new Path();

        path.moveTo(
                action.first.x,
                action.first.y
        );

        long duration;

        if (action.type == ACTION_TAP) {

            // A very short stroke creates a single tap.
            path.lineTo(
                    action.first.x,
                    action.first.y
            );

            duration = 50;

        } else {

            // Swipe from starting point to ending point.
            path.lineTo(
                    action.second.x,
                    action.second.y
            );

            duration = swipeDurationMs;
        }

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(
                                new GestureDescription.StrokeDescription(
                                        path,
                                        0,
                                        duration
                                )
                        )
                        .build();

        boolean dispatched = dispatchGesture(
                gesture,
                new GestureResultCallback() {

                    @Override
                    public void onCompleted(
                            GestureDescription gestureDescription
                    ) {

                        super.onCompleted(gestureDescription);

                        if (!isRunning) {
                            return;
                        }

                        currentActionIndex++;

                        updateStatusText();

                        // Wait before executing the next action.
                        handler.postDelayed(
                                () -> executeNextAction(),
                                actionIntervalMs
                        );
                    }

                    @Override
                    public void onCancelled(
                            GestureDescription gestureDescription
                    ) {

                        super.onCancelled(gestureDescription);

                        if (isRunning) {

                            stopAutomation();

                            showMessage("Gesture was cancelled.");
                        }
                    }

                },
                handler
        );

        if (!dispatched) {

            stopAutomation();

            showMessage("Unable to dispatch gesture.");
        }
    }

    // =====================================
    // STOP AUTOMATION
    // =====================================

    private void stopAutomation() {

        if (!isRunning) {
            return;
        }

        isRunning = false;

        handler.removeCallbacksAndMessages(null);

        setMarkersTouchable(true);

        updateStatusText();

        showMessage("Automation stopped.");
    }

    // =====================================
    // MARKER TOUCH CONTROL
    // =====================================

    private void setMarkersTouchable(boolean touchable) {

        for (MarkerView marker : markerViews) {

            if (touchable) {

                marker.params.flags &=
                        ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

            } else {

                marker.params.flags |=
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }

            try {

                windowManager.updateViewLayout(
                        marker.view,
                        marker.params
                );

            } catch (Exception ignored) {
            }
        }
    }

    // =====================================
    // REMOVE MARKER
    // =====================================

    private void removeMarker(MarkerView markerView) {

        if (isRunning) return;

        try {

            windowManager.removeView(markerView.view);

        } catch (Exception ignored) {
        }

        markerViews.remove(markerView);
        markers.remove(markerView.data);

        // Remove any action using this marker.
        actionSequence.removeIf(action ->
                action.first == markerView.data
                        || action.second == markerView.data
        );

        updateCounters();
    }

    // =====================================
    // CLEAR ALL MARKERS
    // =====================================

    private void clearMarkers() {

        if (isRunning) return;

        for (MarkerView marker : new ArrayList<>(markerViews)) {

            try {
                windowManager.removeView(marker.view);
            } catch (Exception ignored) {
            }
        }

        markerViews.clear();
        markers.clear();
        actionSequence.clear();

        tapCounter = 0;
        swipeCounter = 0;

        updateStatusText();

        showMessage("All markers cleared.");
    }

    private void updateCounters() {

        tapCounter = 0;
        swipeCounter = 0;

        for (MarkerData marker : markers) {

            if (marker.type == TYPE_TAP) {

                tapCounter = Math.max(
                        tapCounter,
                        Integer.parseInt(marker.label.substring(1))
                );
            }

            if (marker.type == TYPE_SWIPE_END) {

                swipeCounter = Math.max(
                        swipeCounter,
                        Integer.parseInt(marker.label.substring(1))
                );
            }
        }
    }

    // =====================================
    // SETTINGS DISPLAY
    // =====================================

    private void updateSettingsText() {

        if (tvSettings == null) return;

        tvSettings.setText(
                "Interval: " + actionIntervalMs + " ms\n"
                        + "Swipe duration: " + swipeDurationMs + " ms"
        );
    }

    private void updateStatusText() {

        if (tvAutomationStatus == null) return;

        if (isRunning) {

            tvAutomationStatus.setText(
                    "RUNNING | Action "
                            + (currentActionIndex + 1)
                            + " / "
                            + actionSequence.size()
            );

        } else {

            tvAutomationStatus.setText(
                    "STOPPED | Actions: "
                            + actionSequence.size()
            );
        }
    }

    // =====================================
    // GET MARKER DATA
    // =====================================

    public List<MarkerData> getMarkers() {

        return Collections.unmodifiableList(
                new ArrayList<>(markers)
        );
    }

    // =====================================
    // UI HELPERS
    // =====================================


    private Button createButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dp(190),
                        dp(45)
                );

        params.setMargins(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        button.setLayoutParams(params);

        return button;
    }


    private GradientDrawable createMarkerBackground(int type) {

        int color;

        if (type == TYPE_TAP) {

            color = Color.rgb(245, 166, 35);

        } else if (type == TYPE_SWIPE_START) {

            color = Color.rgb(22, 150, 100);

        } else {

            color = Color.rgb(210, 65, 65);
        }

        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(dp(2), Color.WHITE);

        return drawable;
    }

    private int dp(float value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }

    private int getScreenWidth() {

        return getResources()
                .getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {

        return getResources()
                .getDisplayMetrics().heightPixels;
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // =====================================
    // REMOVE FLOATING PANEL
    // =====================================

    public void removeControlPanel() {

        if (floatingPanel != null && windowManager != null) {

            try {

                windowManager.removeView(floatingPanel);

            } catch (Exception ignored) {
            }

            floatingPanel = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No event inspection required.
    }

    @Override
    public void onInterrupt() {

        stopAutomation();
    }

    @Override
    public void onDestroy() {

        stopAutomation();

        removeControlPanel();

        clearMarkers();

        instance = null;

        super.onDestroy();
    }
}
