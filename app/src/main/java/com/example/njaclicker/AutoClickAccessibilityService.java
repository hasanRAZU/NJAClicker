
package com.example.njaclicker;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
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

    private int initialX;
    private int initialY;

    private float initialTouchX;
    private float initialTouchY;

    private final List<MarkerData> markers = new ArrayList<>();

    private int tapCounter = 0;
    private int swipeCounter = 0;

    private static final int TYPE_TAP = 1;
    private static final int TYPE_SWIPE_START = 2;
    private static final int TYPE_SWIPE_END = 3;

    // Stores marker information for future automation.
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

    private static class MarkerView {

        MarkerData data;
        TextView view;
        WindowManager.LayoutParams params;
    }

    private final List<MarkerView> markerViews = new ArrayList<>();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        instance = this;

        windowManager = getSystemService(WindowManager.class);
    }

    public static AutoClickAccessibilityService getInstance() {
        return instance;
    }

    // ==============================
    // FLOATING CONTROL PANEL
    // ==============================

    public void showControlPanel() {

        if (windowManager == null || floatingPanel != null) {
            return;
        }

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(35, 38, 48));
        background.setCornerRadius(dp(16));

        root.setBackground(background);
        root.setElevation(dp(10));

        TextView header = new TextView(this);

        header.setText("NJAClicker  ⋮⋮");
        header.setTextColor(Color.WHITE);
        header.setTextSize(18);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp(8), 0, dp(16));

        root.addView(header);

        // Add Tap
        Button btnAddTap = createButton("Add Tap");

        btnAddTap.setOnClickListener(v -> addTapMarker());

        root.addView(btnAddTap);

        // Add Swipe
        Button btnAddSwipe = createButton("Add Swipe");

        btnAddSwipe.setOnClickListener(v -> addSwipeMarkers());

        root.addView(btnAddSwipe);

        // Clear markers
        Button btnClear = createButton("Clear All Markers");

        btnClear.setOnClickListener(v -> clearMarkers());

        root.addView(btnClear);

        // Start
        Button btnStart = createButton("START");

        btnStart.setOnClickListener(v ->
                showMessage("Automation engine will be added next.")
        );

        root.addView(btnStart);

        // Stop
        Button btnStop = createButton("STOP");

        btnStop.setOnClickListener(v ->
                showMessage("No automation running.")
        );

        root.addView(btnStop);

        // Close panel
        Button btnClose = createButton("CLOSE PANEL");

        btnClose.setOnClickListener(v ->
                removeControlPanel()
        );

        root.addView(btnClose);

        // Drag panel by header
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
        panelParams.y = dp(150);

        floatingPanel = root;

        try {
            windowManager.addView(floatingPanel, panelParams);
        } catch (Exception e) {
            floatingPanel = null;
            showMessage("Unable to display floating controls.");
        }
    }

    // ==============================
    // ADD TAP MARKER
    // ==============================

    private void addTapMarker() {

        tapCounter++;

        String label = "T" + tapCounter;

        float x = getScreenWidth() / 2f;
        float y = getScreenHeight() / 2f;

        addMarker(label, TYPE_TAP, x, y);

        showMessage(label + " added. Drag to position.");
    }

    // ==============================
    // ADD SWIPE MARKERS
    // ==============================

    private void addSwipeMarkers() {

        swipeCounter++;

        String startLabel = "S" + swipeCounter;
        String endLabel = "E" + swipeCounter;

        float centerX = getScreenWidth() / 2f;
        float centerY = getScreenHeight() / 2f;

        addMarker(
                startLabel,
                TYPE_SWIPE_START,
                centerX - dp(65),
                centerY
        );

        addMarker(
                endLabel,
                TYPE_SWIPE_END,
                centerX + dp(65),
                centerY
        );

        showMessage("Swipe " + swipeCounter + " added.");
    }

    // ==============================
    // CREATE DRAGGABLE MARKER
    // ==============================

    private void addMarker(
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

        marker.setBackground(createMarkerBackground(type));

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
                Math.min((int) x - dp(24),
                        getScreenWidth() - dp(48))
        );

        params.y = Math.max(
                0,
                Math.min((int) y - dp(24),
                        getScreenHeight() - dp(48))
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
                            Math.min(params.x,
                                    getScreenWidth() - dp(48))
                    );

                    params.y = Math.max(
                            0,
                            Math.min(params.y,
                                    getScreenHeight() - dp(48))
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

                    // Long press to remove marker.
                    if (event.getEventTime() - event.getDownTime() > 600
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

        } catch (Exception e) {

            showMessage("Unable to add marker.");
        }
    }

    // ==============================
    // MARKER COLORS
    // ==============================

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

    // ==============================
    // REMOVE MARKER
    // ==============================

    private void removeMarker(MarkerView markerView) {

        try {

            windowManager.removeView(markerView.view);

        } catch (Exception ignored) {
        }

        markerViews.remove(markerView);
        markers.remove(markerView.data);

        updateCounters();
    }

    // ==============================
    // CLEAR ALL MARKERS
    // ==============================

    private void clearMarkers() {

        for (MarkerView marker : new ArrayList<>(markerViews)) {

            try {
                windowManager.removeView(marker.view);
            } catch (Exception ignored) {
            }
        }

        markerViews.clear();
        markers.clear();

        tapCounter = 0;
        swipeCounter = 0;

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

    // ==============================
    // GET MARKER DATA
    // ==============================

    public List<MarkerData> getMarkers() {

        return Collections.unmodifiableList(
                new ArrayList<>(markers)
        );
    }

    // ==============================
    // HELPERS
    // ==============================

    private Button createButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(14);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dp(190),
                        dp(48)
                );

        params.bottomMargin = dp(6);

        button.setLayoutParams(params);

        return button;
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
        // Handle interruption.
    }

    @Override
    public void onDestroy() {

        removeControlPanel();
        clearMarkers();

        instance = null;

        super.onDestroy();
    }
}
