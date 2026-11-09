
package com.example.njaclicker.overlay;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.njaclicker.model.ActionType;
import com.example.njaclicker.model.AutomationAction;

import java.util.ArrayList;
import java.util.List;

public class FloatingPanel {

    private final AccessibilityService service;
    private final WindowManager windowManager;
    private final Listener listener;
    private final int panelWidth;

    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams bubbleParams;

    private LinearLayout rootLayout;
    private LinearLayout tapListLayout;
    private LinearLayout swipeListLayout;

    private TextView statusText;
    private Button startStopButton;
    private TextView bubbleView;
    private CheckBox repeatCheckBox;

    private boolean isVisible = false;
    private boolean isMinimized = false;
    private boolean isRunning = false;

    private float initialTouchX;
    private float initialTouchY;
    private int initialWindowX;
    private int initialWindowY;

    private float bubbleTouchX;
    private float bubbleTouchY;
    private int bubbleInitialX;
    private int bubbleInitialY;

    private List<AutomationAction> actions = new ArrayList<>();

    public interface Listener {

        void onAddTapRequested();

        void onAddSwipeRequested();

        void onRemoveLastActionRequested();

        void onClearAllRequested();

        void onStartRequested();

        void onStopRequested();

        void onCloseRequested();

        // Save / Load callbacks
        default void onSaveRequested(String name) {
        }

        default List<String> getSavedConfigurations() {
            return new ArrayList<>();
        }

        default void onLoadRequested(String name) {
        }
        default void onDeleteRequested(String name) {
        }
    }

    public FloatingPanel(
            AccessibilityService service,
            Listener listener
    ) {
        this.service = service;
        this.listener = listener;

        windowManager = (WindowManager) service.getSystemService(
                AccessibilityService.WINDOW_SERVICE
        );

        panelWidth = dp(250);

        createPanel();
        createBubble();
    }

    private void createPanel() {

        rootLayout = new LinearLayout(service);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dp(6), dp(5), dp(6), dp(6));
        rootLayout.setBackground(createBackground());

        // Header
        LinearLayout headerRow = new LinearLayout(service);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView header = new TextView(service);
        header.setText("NJAClicker");
        header.setTextColor(Color.WHITE);
        header.setTextSize(17);
        header.setPadding(dp(6), dp(8), dp(6), dp(8));
        header.setOnTouchListener(this::handlePanelDrag);

        headerRow.addView(
                header,
                new LinearLayout.LayoutParams(0, dp(34), 1)
        );

        Button minimizeButton = createButton("−");
        minimizeButton.setTextSize(22);
        minimizeButton.setOnClickListener(v -> minimize());

        headerRow.addView(
                minimizeButton,
                new LinearLayout.LayoutParams(dp(36), dp(32))
        );

        rootLayout.addView(headerRow);

        // Main scroll area
        ScrollView mainScroll = new ScrollView(service);
        mainScroll.setFillViewport(false);
        mainScroll.setVerticalScrollBarEnabled(true);

        LinearLayout content = new LinearLayout(service);
        content.setOrientation(LinearLayout.VERTICAL);

        // Status
        statusText = new TextView(service);
        statusText.setText("Status: Stopped");
        statusText.setTextColor(Color.LTGRAY);
        statusText.setTextSize(12);
        statusText.setPadding(dp(6), dp(6), dp(6), dp(8));

        content.addView(statusText);

        // Add buttons
        LinearLayout addRow = new LinearLayout(service);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);

        Button addTapButton = createButton("Tap +");
        addTapButton.setTextSize(10);
        addTapButton.setPadding(0, 0, 0, 0);

        addTapButton.setOnClickListener(v -> {
            if (!isRunning && listener != null) {
                listener.onAddTapRequested();
            }
        });

        Button addSwipeButton = createButton("Add Swipe");
        addSwipeButton.setOnClickListener(v -> {
            if (!isRunning && listener != null) {
                listener.onAddSwipeRequested();
            }
        });

        LinearLayout.LayoutParams tapParams =
                new LinearLayout.LayoutParams(dp(58), dp(34));

        tapParams.setMargins(dp(2), dp(2), dp(5), dp(2));

        addRow.addView(addTapButton, tapParams);

        LinearLayout.LayoutParams swipeParams =
                new LinearLayout.LayoutParams(0, dp(34), 1);

        swipeParams.setMargins(dp(2), dp(2), dp(2), dp(2));

        addRow.addView(addSwipeButton, swipeParams);

        content.addView(addRow);

        // Tap actions section
        content.addView(createSectionHeading("Tap Actions"));

        tapListLayout = new LinearLayout(service);
        tapListLayout.setOrientation(LinearLayout.VERTICAL);

        ScrollView tapScroll = createActionScrollView(tapListLayout);

        content.addView(
                tapScroll,
                new LinearLayout.LayoutParams(-1, dp(65))
        );

        // Swipe actions section
        content.addView(createSectionHeading("Swipe Actions"));

        swipeListLayout = new LinearLayout(service);
        swipeListLayout.setOrientation(LinearLayout.VERTICAL);

        ScrollView swipeScroll = createActionScrollView(swipeListLayout);

        content.addView(
                swipeScroll,
                new LinearLayout.LayoutParams(-1, dp(65))
        );

        // Repeat checkbox
        repeatCheckBox = new CheckBox(service);
        repeatCheckBox.setText("Repeat sequence automatically");
        repeatCheckBox.setTextColor(Color.WHITE);
        repeatCheckBox.setTextSize(12);

        repeatCheckBox.setButtonTintList(
                android.content.res.ColorStateList.valueOf(Color.WHITE)
        );

        content.addView(repeatCheckBox);

        // Save configuration
        Button saveButton = createButton("Save Configuration");
        saveButton.setOnClickListener(v -> showSaveDialog());

        content.addView(saveButton, fullWidthButtonParams());

        // Load configuration
        Button loadButton = createButton("Load Configuration");
        loadButton.setOnClickListener(v -> showLoadDialog());

        content.addView(loadButton, fullWidthButtonParams());

        // Delete configuration
        Button deleteButton = createButton("Delete Configuration");

        deleteButton.setOnClickListener(v -> showDeleteDialog());

        content.addView(deleteButton, fullWidthButtonParams());

        // Remove last action
        Button removeLastButton = createButton("Remove Last Action");

        removeLastButton.setOnClickListener(v -> {
            if (isRunning) {
                showMessage("Stop automation before removing actions.");
                return;
            }

            if (listener != null) {
                listener.onRemoveLastActionRequested();
            }
        });

        content.addView(removeLastButton, fullWidthButtonParams());

        // Clear all
        Button clearButton = createButton("Clear All Actions");

        clearButton.setOnClickListener(v -> {
            if (isRunning) {
                showMessage("Stop automation before clearing actions.");
                return;
            }

            if (listener != null) {
                listener.onClearAllRequested();
            }
        });

        content.addView(clearButton, fullWidthButtonParams());

        // Start / Stop
        startStopButton = createButton("START");
        startStopButton.setBackgroundColor(Color.rgb(30, 145, 75));

        startStopButton.setOnClickListener(v -> {
            if (listener == null) {
                return;
            }

            if (isRunning) {
                listener.onStopRequested();
            } else {
                listener.onStartRequested();
            }
        });

        content.addView(startStopButton, fullWidthButtonParams());

        // Close
        Button closeButton = createButton("Close Panel");

        closeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCloseRequested();
            }
        });

        content.addView(closeButton, fullWidthButtonParams());

        mainScroll.addView(content);

        rootLayout.addView(
                mainScroll,
                new LinearLayout.LayoutParams(-1, dp(260))
        );

        params = new WindowManager.LayoutParams(
                panelWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(12);
        params.y = dp(80);

        updateActionLists();
    }

    /*
     * Creates independently scrollable action lists.
     * The parent scroll view is prevented from intercepting
     * touch events while the user scrolls these lists.
     */
    private ScrollView createActionScrollView(LinearLayout listLayout) {

        ScrollView scrollView = new ScrollView(service);

        scrollView.setFillViewport(false);
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        scrollView.addView(listLayout);

        scrollView.setOnTouchListener((view, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }

            return false;
        });

        return scrollView;
    }

    private TextView createSectionHeading(String text) {

        TextView heading = new TextView(service);
        heading.setText(text);
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(13);
        heading.setPadding(dp(6), dp(9), dp(6), dp(4));

        return heading;
    }

    private void createBubble() {

        bubbleView = new TextView(service);
        bubbleView.setText("⋮");
        bubbleView.setTextColor(Color.WHITE);
        bubbleView.setTextSize(30);
        bubbleView.setGravity(Gravity.CENTER);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(Color.rgb(40, 55, 80));
        background.setStroke(dp(2), Color.WHITE);

        bubbleView.setBackground(background);
        bubbleView.setElevation(dp(10));

        bubbleParams = new WindowManager.LayoutParams(
                dp(48),
                dp(48),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(12);
        bubbleParams.y = dp(100);

        bubbleView.setOnTouchListener(this::handleBubbleTouch);
    }

    public void show() {

        if (isVisible) {
            return;
        }

        try {
            windowManager.addView(rootLayout, params);

            isVisible = true;
            isMinimized = false;

        } catch (Exception e) {
            android.util.Log.e(
                    "FloatingPanel",
                    "Unable to show panel",
                    e
            );
        }
    }

    private void minimize() {

        if (!isVisible || isMinimized) {
            return;
        }

        try {
            windowManager.removeView(rootLayout);
            windowManager.addView(bubbleView, bubbleParams);

            isMinimized = true;

        } catch (Exception e) {
            android.util.Log.e(
                    "FloatingPanel",
                    "Unable to minimize panel",
                    e
            );
        }
    }

    private void expand() {

        if (!isVisible || !isMinimized) {
            return;
        }

        try {
            windowManager.removeView(bubbleView);

            params.x = bubbleParams.x;
            params.y = bubbleParams.y;

            windowManager.addView(rootLayout, params);

            isMinimized = false;

        } catch (Exception e) {
            android.util.Log.e(
                    "FloatingPanel",
                    "Unable to expand panel",
                    e
            );
        }
    }

    public void hide() {

        if (!isVisible) {
            return;
        }

        try {
            if (isMinimized) {
                windowManager.removeView(bubbleView);
            } else {
                windowManager.removeView(rootLayout);
            }

        } catch (Exception ignored) {
        }

        isVisible = false;
        isMinimized = false;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isRepeatEnabled() {
        return repeatCheckBox != null && repeatCheckBox.isChecked();
    }

    public void updateActions(List<AutomationAction> newActions) {

        actions = newActions == null
                ? new ArrayList<>()
                : new ArrayList<>(newActions);

        updateActionLists();
    }

    private void updateActionLists() {

        if (tapListLayout == null || swipeListLayout == null) {
            return;
        }

        tapListLayout.removeAllViews();
        swipeListLayout.removeAllViews();

        boolean hasTap = false;
        boolean hasSwipe = false;

        // Do not sort this list.
        // Global indexes preserve the original execution order.
        for (int i = 0; i < actions.size(); i++) {

            AutomationAction action = actions.get(i);

            if (action == null) {
                continue;
            }

            if (action.getType() == ActionType.TAP) {

                hasTap = true;

                addActionRow(
                        tapListLayout,
                        action,
                        i,
                        "TAP"
                );

            } else if (action.getType() == ActionType.SWIPE) {

                hasSwipe = true;

                addActionRow(
                        swipeListLayout,
                        action,
                        i,
                        "SWIPE"
                );
            }
        }

        if (!hasTap) {
            addEmptyText(tapListLayout, "No tap actions.");
        }

        if (!hasSwipe) {
            addEmptyText(swipeListLayout, "No swipe actions.");
        }
    }

    private void addActionRow(
            LinearLayout parent,
            AutomationAction action,
            int globalIndex,
            String type
    ) {

        LinearLayout row = new LinearLayout(service);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(7), dp(8), dp(7));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(48, 54, 68));
        background.setCornerRadius(dp(8));

        row.setBackground(background);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, -2);

        rowParams.bottomMargin = dp(5);

        TextView name = new TextView(service);
        name.setText("#" + (globalIndex + 1) + "  " + type);
        name.setTextColor(Color.WHITE);
        name.setTextSize(12);

        row.addView(
                name,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView delay = new TextView(service);
        delay.setText(action.getDelayAfterMs() + " ms");
        delay.setTextColor(Color.rgb(255, 205, 90));
        delay.setTextSize(11);

        row.addView(delay);

        row.setOnClickListener(v -> showActionSettings(globalIndex));

        parent.addView(row, rowParams);
    }

    private void addEmptyText(
            LinearLayout parent,
            String message
    ) {

        TextView empty = new TextView(service);
        empty.setText(message);
        empty.setTextColor(Color.LTGRAY);
        empty.setTextSize(11);
        empty.setPadding(dp(6), dp(10), dp(6), dp(10));

        parent.addView(empty);
    }

    private void showActionSettings(int actionIndex) {

        if (isRunning) {
            showMessage("Stop automation before editing actions.");
            return;
        }

        if (actionIndex < 0 || actionIndex >= actions.size()) {
            return;
        }

        AutomationAction action = actions.get(actionIndex);

        LinearLayout dialogLayout = new LinearLayout(service);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(20), dp(10), dp(20), dp(4));

        TextView delayLabel = new TextView(service);
        delayLabel.setText("Delay after this action (milliseconds)");

        EditText delayInput = createNumberInput(
                String.valueOf(action.getDelayAfterMs())
        );

        dialogLayout.addView(delayLabel);
        dialogLayout.addView(delayInput);

        EditText durationInput = null;

        if (action.getType() == ActionType.SWIPE) {

            TextView durationLabel = new TextView(service);
            durationLabel.setText("Swipe duration (milliseconds)");

            durationInput = createNumberInput(
                    String.valueOf(action.getSwipeDurationMs())
            );

            dialogLayout.addView(durationLabel);
            dialogLayout.addView(durationInput);
        }

        final EditText finalDurationInput = durationInput;

        AlertDialog dialog = new AlertDialog.Builder(service)
                .setTitle("Action " + (actionIndex + 1) + " Settings")
                .setView(dialogLayout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        configureDialogWindow(dialog);

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        try {

                            String delayValue =
                                    delayInput.getText().toString().trim();

                            if (delayValue.isEmpty()) {
                                showMessage("Please enter a delay.");
                                return;
                            }

                            long delay = Long.parseLong(delayValue);

                            if (delay < 0) {
                                showMessage("Delay cannot be negative.");
                                return;
                            }

                            long duration = 0;

                            if (finalDurationInput != null) {

                                String durationValue =
                                        finalDurationInput.getText()
                                                .toString().trim();

                                if (durationValue.isEmpty()) {
                                    showMessage("Enter swipe duration.");
                                    return;
                                }

                                duration = Long.parseLong(durationValue);

                                if (duration < 1) {
                                    showMessage(
                                            "Swipe duration must be at least 1 ms."
                                    );
                                    return;
                                }
                            }

                            action.setDelayAfterMs(delay);

                            if (finalDurationInput != null) {
                                action.setSwipeDurationMs(duration);
                            }

                            dialog.dismiss();
                            updateActionLists();

                            showMessage("Action settings saved.");

                        } catch (NumberFormatException e) {
                            showMessage("Please enter valid numbers.");
                        }
                    });
        });

        dialog.show();
    }

    // Save configuration dialog
    private void showSaveDialog() {

        if (isRunning) {
            showMessage("Stop automation before saving.");
            return;
        }

        if (actions.isEmpty()) {
            showMessage("Add at least one action before saving.");
            return;
        }

        EditText nameInput = new EditText(service);
        nameInput.setSingleLine(true);
        nameInput.setHint("Example: Game Level 1");
        nameInput.setTextSize(15);

        LinearLayout layout = new LinearLayout(service);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(4));
        layout.addView(nameInput);

        AlertDialog dialog = new AlertDialog.Builder(service)
                .setTitle("Save Configuration")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        configureDialogWindow(dialog);

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String name = nameInput.getText()
                                .toString()
                                .trim();

                        if (name.isEmpty()) {
                            showMessage("Please enter a configuration name.");
                            return;
                        }

                        if (listener != null) {
                            listener.onSaveRequested(name);
                        }

                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    // Load configuration selection dialog
    private void showLoadDialog() {

        if (isRunning) {
            showMessage("Stop automation before loading.");
            return;
        }

        if (listener == null) {
            return;
        }

        List<String> names = listener.getSavedConfigurations();

        if (names == null || names.isEmpty()) {
            showMessage("No saved configurations found.");
            return;
        }

        String[] items = names.toArray(new String[0]);

        AlertDialog dialog = new AlertDialog.Builder(service)
                .setTitle("Select Configuration")
                .setItems(items, (d, which) -> {

                    if (isRunning) {
                        showMessage("Stop automation before loading.");
                        return;
                    }

                    listener.onLoadRequested(items[which]);
                })
                .setNegativeButton("Cancel", null)
                .create();

        configureDialogWindow(dialog);

        dialog.show();
    }

    // Delete a saved configuration
    private void showDeleteDialog() {

        if (isRunning) {
            showMessage("Stop automation before deleting.");
            return;
        }

        if (listener == null) {
            return;
        }

        List<String> names = listener.getSavedConfigurations();

        if (names == null || names.isEmpty()) {
            showMessage("No saved configurations found.");
            return;
        }

        String[] items = names.toArray(new String[0]);

        AlertDialog dialog = new AlertDialog.Builder(service)
                .setTitle("Select Configuration to Delete")
                .setItems(items, (d, which) -> {

                    if (isRunning) {
                        showMessage("Stop automation before deleting.");
                        return;
                    }

                    String selectedName = items[which];

                    showDeleteConfirmation(selectedName);
                })
                .setNegativeButton("Cancel", null)
                .create();

        configureDialogWindow(dialog);

        dialog.show();
    }


    // Confirm before deleting
    private void showDeleteConfirmation(String name) {

        if (isRunning) {
            showMessage("Stop automation before deleting.");
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(service)
                .setTitle("Delete Configuration")
                .setMessage(
                        "Are you sure you want to delete \""
                                + name + "\"?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> {

                    if (isRunning) {
                        showMessage("Stop automation before deleting.");
                        return;
                    }

                    if (listener != null) {
                        listener.onDeleteRequested(name);
                    }
                })
                .create();

        configureDialogWindow(dialog);

        dialog.show();
    }

    private void configureDialogWindow(AlertDialog dialog) {

        Window window = dialog.getWindow();

        if (window != null) {

            // Set the correct window type BEFORE dialog.show().
            window.setType(
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            );

            window.clearFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            );

            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }


    public void setRunning(boolean running) {

        isRunning = running;

        if (statusText == null || startStopButton == null) {
            return;
        }

        if (running) {

            statusText.setText("Status: Running");
            startStopButton.setText("STOP");
            startStopButton.setBackgroundColor(Color.rgb(190, 50, 50));

        } else {

            statusText.setText("Status: Stopped");
            startStopButton.setText("START");
            startStopButton.setBackgroundColor(Color.rgb(30, 145, 75));
        }
    }

    private boolean handlePanelDrag(View view, MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();

                initialWindowX = params.x;
                initialWindowY = params.y;

                return true;

            case MotionEvent.ACTION_MOVE:

                params.x = initialWindowX
                        + Math.round(event.getRawX() - initialTouchX);

                params.y = initialWindowY
                        + Math.round(event.getRawY() - initialTouchY);

                try {
                    windowManager.updateViewLayout(rootLayout, params);
                } catch (Exception ignored) {
                }

                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
        }

        return false;
    }

    private boolean handleBubbleTouch(View view, MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                bubbleTouchX = event.getRawX();
                bubbleTouchY = event.getRawY();

                bubbleInitialX = bubbleParams.x;
                bubbleInitialY = bubbleParams.y;

                return true;

            case MotionEvent.ACTION_MOVE:

                bubbleParams.x = bubbleInitialX
                        + Math.round(event.getRawX() - bubbleTouchX);

                bubbleParams.y = bubbleInitialY
                        + Math.round(event.getRawY() - bubbleTouchY);

                try {
                    windowManager.updateViewLayout(bubbleView, bubbleParams);
                } catch (Exception ignored) {
                }

                return true;

            case MotionEvent.ACTION_UP:

                float movedX = Math.abs(
                        event.getRawX() - bubbleTouchX
                );

                float movedY = Math.abs(
                        event.getRawY() - bubbleTouchY
                );

                if (movedX < dp(10) && movedY < dp(10)) {
                    expand();
                }

                return true;

            case MotionEvent.ACTION_CANCEL:
                return true;
        }

        return false;
    }

    private Button createButton(String text) {

        Button button = new Button(service);
        button.setText(text);
        button.setTextSize(12);
        button.setTextColor(Color.WHITE);

        return button;
    }

    private EditText createNumberInput(String value) {

        EditText input = new EditText(service);
        input.setText(value);
        input.setTextSize(15);
        input.setSingleLine(true);
        input.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        return input;
    }

    private LinearLayout.LayoutParams fullWidthButtonParams() {

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(-1, dp(32));

        buttonParams.setMargins(0, dp(1), 0, dp(1));

        return buttonParams;
    }

    private GradientDrawable createBackground() {

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(25, 30, 42));
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), Color.rgb(85, 95, 115));

        return background;
    }

    private int dp(float value) {

        return Math.round(
                value * service.getResources()
                        .getDisplayMetrics().density
        );
    }

    private void showMessage(String message) {

        Toast.makeText(
                service,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    public void destroy() {
        hide();
        actions.clear();
    }
}
