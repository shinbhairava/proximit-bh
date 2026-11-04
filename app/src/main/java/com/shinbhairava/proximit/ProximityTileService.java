package com.shinbhairava.proximit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

        if (!ProximityService.hasProximitySensor(this)) {
            Toast.makeText(this, R.string.no_sensor_tile, Toast.LENGTH_SHORT).show();
            refreshTile();
            return;
        }

        if (!ProximityService.supportsScreenOff(this)) {
            Toast.makeText(this, R.string.screen_off_unsupported_tile, Toast.LENGTH_SHORT).show();
            refreshTile();
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

    private void refreshTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        if (!ProximityService.hasProximitySensor(this)
                || !ProximityService.supportsScreenOff(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else if (ProximityService.isRunning(this)) {
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.setLabel(getString(R.string.tile_label));
        tile.updateTile();
    }
}
