package com.shinbhairava.proximit;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class ProximityTileService extends TileService {

    private final Handler handler = new Handler(Looper.getMainLooper());

    public static void requestUpdate(Context context) {
        try {
            requestListeningState(
                    context, new ComponentName(context, ProximityTileService.class));
        } catch (RuntimeException ignored) {
            // The tile may not exist on this device.
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        refreshTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        if (!hasProximitySensor()) {
            Toast.makeText(this, R.string.no_sensor_tile, Toast.LENGTH_SHORT).show();
            refreshTile();
            return;
        }

        if (!AdminReceiver.isActive(this)) {
            openSetup();
            return;
        }

        if (ProximityService.isRunning(this)) {
            ProximityService.requestStop(this);
        } else {
            try {
                startForegroundService(
                        new Intent(this, ProximityService.class)
                                .setAction(ProximityService.ACTION_START));
            } catch (RuntimeException exception) {
                Toast.makeText(this, R.string.start_failed_tile, Toast.LENGTH_SHORT).show();
            }
        }

        handler.postDelayed(this::refreshTile, 400L);
    }

    private void openSetup() {
        Intent intent = new Intent(this, SetupActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            startActivityAndCollapse(pendingIntent);
        } else {
            startActivityAndCollapse(intent);
        }
    }

    private void refreshTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        if (!hasProximitySensor()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else if (ProximityService.isRunning(this)) {
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.setLabel(getString(R.string.tile_label));
        tile.updateTile();
    }

    private boolean hasProximitySensor() {
        SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        return manager != null
                && manager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null;
    }
}
