package com.shinbhairava.proximit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private TextView supportView;
    private Button notificationsButton;
    private Button toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        statusView = findViewById(R.id.status);
        sensorView = findViewById(R.id.sensor_status);
        supportView = findViewById(R.id.support_status);
        notificationsButton = findViewById(R.id.notifications_button);
        toggleButton = findViewById(R.id.toggle_button);

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
        boolean running = ProximityService.isRunning(this);
        boolean sensorAvailable = ProximityService.hasProximitySensor(this);
        boolean screenOffSupported = ProximityService.supportsScreenOff(this);
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
                getString(running ? R.string.running : R.string.stopped),
                notifications));

        sensorView.setText(
                sensorAvailable ? R.string.sensor_found : R.string.sensor_missing);
        supportView.setText(screenOffSupported
                ? R.string.screen_off_supported
                : R.string.screen_off_unsupported);
        supportView.setVisibility(View.VISIBLE);

        notificationsButton.setVisibility(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed
                        ? View.VISIBLE
                        : View.GONE);
        toggleButton.setText(running ? R.string.stop_gesture : R.string.start_gesture);
        toggleButton.setEnabled(sensorAvailable && screenOffSupported);
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

        if (!ProximityService.hasProximitySensor(this)) {
            Toast.makeText(this, R.string.sensor_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ProximityService.supportsScreenOff(this)) {
            Toast.makeText(this, R.string.screen_off_unavailable, Toast.LENGTH_SHORT).show();
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
}
