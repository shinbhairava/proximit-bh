package com.shinbhairava.proximit;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, AdminReceiver.class);
    }

    public static boolean isActive(Context context) {
        DevicePolicyManager manager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return manager != null && manager.isAdminActive(getComponentName(context));
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        context.stopService(new Intent(context, ProximityService.class));
        ProximityTileService.requestUpdate(context);
        super.onDisabled(context, intent);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.admin_disable_warning);
    }
}
