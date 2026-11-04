package com.shinbhairava.proximit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SetupActivity extends Activity {

    private static final int REQUEST_NOTIFICATIONS = 100;

    private TextView statusView;
    private TextView sensorView;
    private Button adminButton;
    private Button notificationsButton;
    private Button toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        statusView = findViewById(R.id.status);
        sensorView = findViewById(R.id.sensor_status);
        adminButton = findViewById(R.id.admin_button);
        notificationsButton = findViewById(R.id.notifications_button);
        toggleButton = findViewById(R.id.toggle_button);

        adminButton.setOnClickListener(view -> toggleDeviceAdmin());
        notificationsButton.setOnClickListener(view -> requestNotificationPermission());
        toggleButton.setOnClickListener(view -> toggleGesture());
        findViewById(R.id.close_button).setOnClickListener(view -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            refresh();
        }
    }

    private void refresh() {
        boolean adminActive = AdminReceiver.isActive(this);
        boolean running = ProximityService.isRunning(this);
        boolean sensorAvailable = hasProximitySensor();
        boolean notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;

        String notifications;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notifications = getString(R.string.not_required);
        } else {
            notifications = getString(
                    notificationsAllowed ? R.string.allowed : R.string.not_allowed);
        }

        statusView.setText(getString(
                R.string.status_template,
                getString(adminActive ? R.string.enabled : R.string.disabled),
                notifications,
                getString(running ? R.string.running : R.string.stopped)));

        sensorView.setText(sensorAvailable ? R.string.sensor_found : R.string.sensor_missing);
        sensorView.setVisibility(View.VISIBLE);

        adminButton.setText(adminActive ? R.string.disable_admin : R.string.enable_admin);
        notificationsButton.setVisibility(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed
                        ? View.VISIBLE
                        : View.GONE);
        toggleButton.setText(running ? R.string.stop_gesture : R.string.start_gesture);
        toggleButton.setEnabled(adminActive && sensorAvailable);
    }

    private void toggleDeviceAdmin() {
        DevicePolicyManager manager =
                (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (manager == null) {
            return;
        }

        ComponentName admin = AdminReceiver.getComponentName(this);
        if (manager.isAdminActive(admin)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.admin_disable_title)
                    .setMessage(R.string.admin_disable_message)
                    .setPositiveButton(R.string.disable, (dialog, which) -> {
                        manager.removeActiveAdmin(admin);
                        ProximityService.requestStop(this);
                        refresh();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.admin_explanation));
        startActivity(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    private void toggleGesture() {
        if (ProximityService.isRunning(this)) {
            ProximityService.requestStop(this);
            refresh();
            return;
        }

        if (!AdminReceiver.isActive(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.admin_required_title)
                    .setMessage(R.string.admin_required_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        if (!hasProximitySensor()) {
            Toast.makeText(this, R.string.sensor_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            startForegroundService(
                    new Intent(this, ProximityService.class)
                            .setAction(ProximityService.ACTION_START));
        } catch (RuntimeException exception) {
            Toast.makeText(this, R.string.start_failed, Toast.LENGTH_SHORT).show();
        }

        statusView.postDelayed(this::refresh, 500L);
    }

    private boolean hasProximitySensor() {
        SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        return manager != null
                && manager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null;
    }
}
