
package com.example.njaclicker.model;

public class MarkerData {

    private String id;
    private ActionType type;

    private float x;
    private float y;

    public MarkerData(String id, ActionType type, float x, float y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public ActionType getType() {
        return type;
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
}
