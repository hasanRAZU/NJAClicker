
package com.example.njaclicker.model;

public class AutomationAction {

    private ActionType type;

    // Tap coordinates
    private float x;
    private float y;

    // Swipe coordinates
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    // Swipe duration
    private long swipeDurationMs = 500;

    // Waiting time after this action
    private long delayAfterMs = 1000;

    public AutomationAction(ActionType type) {
        this.type = type;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getStartX() {
        return startX;
    }

    public void setStartX(float startX) {
        this.startX = startX;
    }

    public float getStartY() {
        return startY;
    }

    public void setStartY(float startY) {
        this.startY = startY;
    }

    public float getEndX() {
        return endX;
    }

    public void setEndX(float endX) {
        this.endX = endX;
    }

    public float getEndY() {
        return endY;
    }

    public void setEndY(float endY) {
        this.endY = endY;
    }

    public long getSwipeDurationMs() {
        return swipeDurationMs;
    }

    public void setSwipeDurationMs(long swipeDurationMs) {
        this.swipeDurationMs = Math.max(1, swipeDurationMs);
    }

    public long getDelayAfterMs() {
        return delayAfterMs;
    }

    public void setDelayAfterMs(long delayAfterMs) {
        this.delayAfterMs = Math.max(0, delayAfterMs);
    }
}
