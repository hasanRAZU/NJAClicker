
package com.example.njaclicker.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.njaclicker.model.ActionType;
import com.example.njaclicker.model.AutomationAction;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutomationStorage {

    private static final String PREF_NAME = "njaclicker_configurations";
    private static final String KEY_CONFIGURATIONS = "saved_configurations";

    private final SharedPreferences preferences;

    public AutomationStorage(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public boolean saveConfiguration(
            String name,
            List<AutomationAction> actions
    ) {
        if (name == null || name.trim().isEmpty() || actions == null) {
            return false;
        }

        try {
            JSONObject allConfigurations = getAllConfigurations();

            JSONArray actionArray = new JSONArray();

            // Preserve the original global action order.
            for (AutomationAction action : actions) {
                if (action == null) {
                    continue;
                }

                JSONObject object = new JSONObject();

                object.put("type", action.getType().name());

                object.put("x", action.getX());
                object.put("y", action.getY());

                object.put("startX", action.getStartX());
                object.put("startY", action.getStartY());

                object.put("endX", action.getEndX());
                object.put("endY", action.getEndY());

                object.put(
                        "delayAfterMs",
                        action.getDelayAfterMs()
                );

                object.put(
                        "swipeDurationMs",
                        action.getSwipeDurationMs()
                );

                actionArray.put(object);
            }

            JSONObject configuration = new JSONObject();
            configuration.put("name", name.trim());
            configuration.put("actions", actionArray);

            allConfigurations.put(name.trim(), configuration);

            return preferences.edit()
                    .putString(
                            KEY_CONFIGURATIONS,
                            allConfigurations.toString()
                    )
                    .commit();

        } catch (Exception e) {
            android.util.Log.e(
                    "AutomationStorage",
                    "Unable to save configuration",
                    e
            );

            return false;
        }
    }

    public List<String> getSavedConfigurationNames() {
        List<String> names = new ArrayList<>();

        try {
            JSONObject allConfigurations = getAllConfigurations();

            JSONArray keys = allConfigurations.names();

            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    names.add(keys.getString(i));
                }
            }

            Collections.sort(names);

        } catch (Exception e) {
            android.util.Log.e(
                    "AutomationStorage",
                    "Unable to read configuration names",
                    e
            );
        }

        return names;
    }

    public List<AutomationAction> loadConfiguration(String name) {
        List<AutomationAction> actions = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            return actions;
        }

        try {
            JSONObject allConfigurations = getAllConfigurations();

            if (!allConfigurations.has(name)) {
                return actions;
            }

            JSONObject configuration =
                    allConfigurations.getJSONObject(name);

            JSONArray actionArray =
                    configuration.getJSONArray("actions");

            for (int i = 0; i < actionArray.length(); i++) {

                JSONObject object = actionArray.getJSONObject(i);

                ActionType type = ActionType.valueOf(
                        object.getString("type")
                );

                AutomationAction action =
                        new AutomationAction(type);

                action.setX((float) object.getDouble("x"));
                action.setY((float) object.getDouble("y"));

                action.setStartX(
                        (float) object.getDouble("startX")
                );

                action.setStartY(
                        (float) object.getDouble("startY")
                );

                action.setEndX(
                        (float) object.getDouble("endX")
                );

                action.setEndY(
                        (float) object.getDouble("endY")
                );

                action.setDelayAfterMs(
                        object.getLong("delayAfterMs")
                );

                action.setSwipeDurationMs(
                        object.getLong("swipeDurationMs")
                );

                actions.add(action);
            }

        } catch (Exception e) {
            android.util.Log.e(
                    "AutomationStorage",
                    "Unable to load configuration: " + name,
                    e
            );

            actions.clear();
        }

        return actions;
    }

    public boolean deleteConfiguration(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject allConfigurations = getAllConfigurations();

            if (!allConfigurations.has(name)) {
                return false;
            }

            allConfigurations.remove(name);

            return preferences.edit()
                    .putString(
                            KEY_CONFIGURATIONS,
                            allConfigurations.toString()
                    )
                    .commit();

        } catch (Exception e) {
            android.util.Log.e(
                    "AutomationStorage",
                    "Unable to delete configuration",
                    e
            );

            return false;
        }
    }

    private JSONObject getAllConfigurations() {
        String savedData = preferences.getString(
                KEY_CONFIGURATIONS,
                "{}"
        );

        try {
            return new JSONObject(savedData);
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
