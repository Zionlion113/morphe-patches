package app.andrewliang.extension;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class PowerSave {
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList();
    private static volatile boolean on;
    private static volatile boolean lowRam;
    private static volatile boolean thermalThrottled;
    private static boolean registered;

    private PowerSave() {
    }

    static void init(Context context) {
        if (registered || context == null) {
            return;
        }
        if (context.getApplicationContext() != null) {
            context = context.getApplicationContext();
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        if (activityManager != null) {
            lowRam = activityManager.isLowRamDevice();
        }
        final PowerManager powerManager = (PowerManager) context.getSystemService("power");
        if (powerManager == null) {
            return;
        }
        registered = true;
        on = powerManager.isPowerSaveMode();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: app.andrewliang.extension.PowerSave.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                PowerSave.on = powerManager.isPowerSaveMode();
                PowerSave.notifyListeners();
            }
        };
        IntentFilter intentFilter = new IntentFilter("android.os.action.POWER_SAVE_MODE_CHANGED");
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(broadcastReceiver, intentFilter, 4);
            } else {
                context.registerReceiver(broadcastReceiver, intentFilter);
            }
        } catch (RuntimeException unused) {
        }
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                thermalThrottled = powerManager.getCurrentThermalStatus() >= PowerManager.THERMAL_STATUS_SEVERE;
                powerManager.addThermalStatusListener(new PowerManager.OnThermalStatusChangedListener() {
                    @Override
                    public void onThermalStatusChanged(int status) {
                        boolean throttled = status >= PowerManager.THERMAL_STATUS_SEVERE;
                        if (throttled != PowerSave.thermalThrottled) {
                            PowerSave.thermalThrottled = throttled;
                            PowerSave.notifyListeners();
                        }
                    }
                });
            } catch (RuntimeException unused) {
            }
        }
    }

    private static void notifyListeners() {
        Iterator it = LISTENERS.iterator();
        while (it.hasNext()) {
            ((Runnable) it.next()).run();
        }
    }

    static boolean isOn() {
        return on;
    }

    static boolean shouldDegradeEffects() {
        return on || lowRam || thermalThrottled;
    }

    static void addListener(Runnable runnable) {
        LISTENERS.add(runnable);
    }
}
