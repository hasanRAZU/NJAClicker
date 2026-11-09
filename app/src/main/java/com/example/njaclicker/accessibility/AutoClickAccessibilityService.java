
package com.example.njaclicker.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.example.njaclicker.automation.AutomationController;
import com.example.njaclicker.automation.GestureExecutor;
import com.example.njaclicker.model.AutomationAction;
import com.example.njaclicker.model.MarkerData;
import com.example.njaclicker.overlay.FloatingPanel;
import com.example.njaclicker.overlay.MarkerManager;
import com.example.njaclicker.storage.AutomationStorage;
import com.example.njaclicker.util.ScreenUtils;

import java.util.List;

public class AutoClickAccessibilityService extends AccessibilityService {

    private static AutoClickAccessibilityService instance;

    private GestureExecutor gestureExecutor;
    private AutomationController automationController;

    private MarkerManager markerManager;
    private FloatingPanel floatingPanel;
    private AutomationStorage automationStorage;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        instance = this;

        automationStorage = new AutomationStorage(this);

        initializeAutomation();
        initializeMarkerManager();
        initializeFloatingPanel();

        showMessage("NJAClicker service connected.");
    }

    private void initializeAutomation() {

        gestureExecutor = new GestureExecutor(this);
        automationController = new AutomationController(gestureExecutor);

        automationController.setListener(
                new AutomationController.ControllerListener() {

                    @Override
                    public void onActionStarted(int actionIndex) {

                        if (floatingPanel != null) {
                            floatingPanel.setRunning(true);
                        }

                        if (markerManager != null) {
                            markerManager.setActiveActionIndex(actionIndex);
                        }
                    }

                    @Override
                    public void onActionCompleted(int actionIndex) {

                        if (markerManager != null) {
                            markerManager.setActiveActionIndex(-1);
                        }
                    }

                    @Override
                    public void onError(String message) {

                        if (markerManager != null) {
                            markerManager.setActiveActionIndex(-1);
                        }

                        restoreMarkerInteraction();
                        showMessage(message);
                    }

                    @Override
                    public void onStopped() {

                        if (markerManager != null) {
                            markerManager.setActiveActionIndex(-1);
                        }

                        restoreMarkerInteraction();
                    }
                }
        );
    }

    private void initializeMarkerManager() {

        markerManager = new MarkerManager(
                this,
                new MarkerManager.Listener() {

                    @Override
                    public void onActionsChanged(List<AutomationAction> actions) {
                        updateFloatingPanelActions(actions);
                    }

                    @Override
                    public void onMarkerMoved(MarkerData marker) {
                        updateFloatingPanelActions(markerManager.getActions());
                    }

                    @Override
                    public void onMarkerRemoved(String markerId) {
                        updateFloatingPanelActions(markerManager.getActions());
                    }
                }
        );
    }

    private void initializeFloatingPanel() {

        floatingPanel = new FloatingPanel(
                this,
                new FloatingPanel.Listener() {

                    @Override
                    public void onAddTapRequested() {
                        addTapMarker();
                    }

                    @Override
                    public void onAddSwipeRequested() {
                        addSwipeMarkers();
                    }

                    @Override
                    public void onRemoveLastActionRequested() {
                        removeLastAction();
                    }

                    @Override
                    public void onClearAllRequested() {
                        clearAllMarkers();
                    }

                    @Override
                    public void onStartRequested() {
                        startAutomation();
                    }

                    @Override
                    public void onStopRequested() {
                        stopAutomation();
                    }

                    @Override
                    public void onCloseRequested() {
                        removeControlPanel();
                    }

                    // SAVE CONFIGURATION
                    @Override
                    public void onSaveRequested(String name) {
                        saveConfiguration(name);
                    }

                    // GET SAVED CONFIGURATION NAMES
                    @Override
                    public List<String> getSavedConfigurations() {

                        if (automationStorage == null) {
                            return java.util.Collections.emptyList();
                        }

                        return automationStorage.getSavedConfigurationNames();
                    }

                    // LOAD CONFIGURATION
                    @Override
                    public void onLoadRequested(String name) {
                        loadConfiguration(name);
                    }
                    // DELETE CONFIGURATION
                    @Override
                    public void onDeleteRequested(String name) {
                        deleteConfiguration(name);
                    }
                }
        );

        updateFloatingPanelActions(markerManager.getActions());
    }

    public static AutoClickAccessibilityService getInstance() {
        return instance;
    }

    public void showControlPanel() {

        if (floatingPanel == null) {
            initializeFloatingPanel();
        }

        if (floatingPanel != null) {
            floatingPanel.show();
        }
    }

    public void removeControlPanel() {

        if (floatingPanel != null) {
            floatingPanel.hide();
        }
    }

    private void updateFloatingPanelActions(List<AutomationAction> actions) {

        if (floatingPanel != null) {
            floatingPanel.updateActions(actions);
        }
    }

    // SAVE CURRENT CONFIGURATION
    private void saveConfiguration(String name) {

        if (automationController.isRunning()) {
            showMessage("Stop automation before saving.");
            return;
        }

        if (markerManager == null || automationStorage == null) {
            showMessage("Storage is not initialized.");
            return;
        }

        List<AutomationAction> actions = markerManager.getActions();

        if (actions.isEmpty()) {
            showMessage("There are no actions to save.");
            return;
        }

        boolean saved = automationStorage.saveConfiguration(
                name,
                actions
        );

        if (saved) {
            showMessage("Configuration saved: " + name);
        } else {
            showMessage("Unable to save configuration.");
        }
    }

    // LOAD A SAVED CONFIGURATION
    private void loadConfiguration(String name) {

        if (automationController.isRunning()) {
            showMessage("Stop automation before loading.");
            return;
        }

        if (markerManager == null || automationStorage == null) {
            showMessage("Storage is not initialized.");
            return;
        }

        List<AutomationAction> loadedActions =
                automationStorage.loadConfiguration(name);

        if (loadedActions.isEmpty()) {
            showMessage("Configuration is empty or could not be loaded.");
            return;
        }

        // Restore all markers and actions in their original order.
        markerManager.replaceActions(loadedActions);

        updateFloatingPanelActions(markerManager.getActions());

        showMessage("Configuration loaded: " + name);
    }

    // DELETE A SAVED CONFIGURATION
    private void deleteConfiguration(String name) {

        if (automationController.isRunning()) {
            showMessage("Stop automation before deleting.");
            return;
        }

        if (automationStorage == null) {
            showMessage("Storage is not initialized.");
            return;
        }

        boolean deleted = automationStorage.deleteConfiguration(name);

        if (deleted) {
            showMessage("Configuration deleted: " + name);
        } else {
            showMessage("Unable to delete configuration.");
        }
    }

    private void addTapMarker() {

        if (automationController.isRunning()) {
            showMessage("Stop automation before adding markers.");
            return;
        }

        float centerX = ScreenUtils.getScreenWidth(this) / 2f;
        float centerY = ScreenUtils.getScreenHeight(this) / 2f;

        markerManager.addTapMarker(centerX, centerY);

        showMessage("Tap marker added.");
    }

    private void addSwipeMarkers() {

        if (automationController.isRunning()) {
            showMessage("Stop automation before adding markers.");
            return;
        }

        float centerX = ScreenUtils.getScreenWidth(this) / 2f;
        float centerY = ScreenUtils.getScreenHeight(this) / 2f;

        float offset = ScreenUtils.dpToPx(this, 65);

        float startX = ScreenUtils.clampX(this, centerX - offset);
        float endX = ScreenUtils.clampX(this, centerX + offset);
        float y = ScreenUtils.clampY(this, centerY);

        markerManager.addSwipeMarkers(startX, y, endX, y);

        showMessage("Swipe markers added.");
    }

    private void removeLastAction() {

        if (automationController.isRunning()) {
            showMessage("Stop automation before removing actions.");
            return;
        }

        boolean removed = markerManager.removeLastAction();

        if (removed) {
            showMessage("Last action removed.");
        } else {
            showMessage("There are no actions to remove.");
        }
    }

    private void clearAllMarkers() {

        if (automationController.isRunning()) {
            showMessage("Stop automation first.");
            return;
        }

        markerManager.clearMarkers();

        updateFloatingPanelActions(markerManager.getActions());

        showMessage("All actions cleared.");
    }

    private void startAutomation() {

        if (automationController == null || markerManager == null) {
            showMessage("Automation is not initialized.");
            return;
        }

        if (automationController.isRunning()) {
            showMessage("Automation is already running.");
            return;
        }

        List<AutomationAction> actions = markerManager.getActions();

        if (actions.isEmpty()) {
            showMessage("Please add at least one action.");
            return;
        }

        markerManager.setMarkersTouchable(false);

        if (floatingPanel != null) {
            floatingPanel.setRunning(true);
        }

        boolean repeat = floatingPanel != null
                && floatingPanel.isRepeatEnabled();

        automationController.start(actions, repeat);

        showMessage(
                repeat
                        ? "Automation started. Repeat is ON."
                        : "Automation started. Repeat is OFF."
        );
    }

    private void stopAutomation() {

        if (automationController == null) {
            return;
        }

        automationController.stop();

        restoreMarkerInteraction();

        showMessage("Automation stopped.");
    }

    private void restoreMarkerInteraction() {

        if (markerManager != null) {
            markerManager.setMarkersTouchable(true);
            markerManager.setActiveActionIndex(-1);
        }

        if (floatingPanel != null) {
            floatingPanel.setRunning(false);
        }
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No accessibility event inspection is required.
    }

    @Override
    public void onInterrupt() {

        if (automationController != null) {
            automationController.stop();
        }

        restoreMarkerInteraction();
    }

    @Override
    public void onDestroy() {

        if (automationController != null) {
            automationController.stop();
        }

        if (floatingPanel != null) {
            floatingPanel.destroy();
            floatingPanel = null;
        }

        if (markerManager != null) {
            markerManager.clearMarkers();
            markerManager = null;
        }

        gestureExecutor = null;
        automationStorage = null;

        if (instance == this) {
            instance = null;
        }

        super.onDestroy();
    }
}
