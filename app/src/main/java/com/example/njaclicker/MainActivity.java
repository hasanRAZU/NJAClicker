
package com.example.njaclicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.njaclicker.accessibility.AutoClickAccessibilityService;

public class MainActivity extends Activity {

    private TextView tvServiceStatus;
    private Button btnEnableService;
    private Button btnOpenFloatingControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvServiceStatus = findViewById(R.id.tvServiceStatus);
        btnEnableService = findViewById(R.id.btnEnableService);
        btnOpenFloatingControls =
                findViewById(R.id.btnOpenFloatingControls);

        btnEnableService.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
            );
            startActivity(intent);
        });

        btnOpenFloatingControls.setOnClickListener(v -> {

            AutoClickAccessibilityService service =
                    AutoClickAccessibilityService.getInstance();

            if (service == null) {
                Toast.makeText(
                        this,
                        "Please enable Accessibility Service first.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            service.showControlPanel();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private void updateServiceStatus() {

        boolean enabled = isAccessibilityServiceEnabled();

        if (enabled) {
            tvServiceStatus.setText(
                    "Accessibility Service: Enabled"
            );
            btnEnableService.setText("Service Enabled");
        } else {
            tvServiceStatus.setText(
                    "Accessibility Service: Disabled"
            );
            btnEnableService.setText(
                    "Enable Accessibility Service"
            );
        }
    }

    private boolean isAccessibilityServiceEnabled() {

        String serviceName = getPackageName()
                + "/" + AutoClickAccessibilityService.class.getName();

        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        return enabledServices != null
                && enabledServices.toLowerCase().contains(
                serviceName.toLowerCase()
        );
    }
}
