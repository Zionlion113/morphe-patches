package app.andrewliang.extension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tells whether Battery Saver is on. Shared by the patches that change how much work LINE does
 * when it is on. The value changes at once, from {@link PowerManager#ACTION_POWER_SAVE_MODE_CHANGED}.
 */
final class PowerSave {

    private PowerSave() {}

    private static volatile boolean on;
    private static boolean registered;
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    /** Starts watching. Safe to call more than once. Main thread only. */
    static void init(Context context) {
        if (registered || context == null) return;
        Context application = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        PowerManager power = (PowerManager) application.getSystemService(Context.POWER_SERVICE);
        if (power == null) return;
        registered = true;
        on = power.isPowerSaveMode();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                on = power.isPowerSaveMode();
                for (Runnable listener : LISTENERS) listener.run();
            }
        };
        IntentFilter filter = new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                application.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                application.registerReceiver(receiver, filter);
            }
        } catch (RuntimeException ignored) {
            // Without the receiver, the value stays as it was at start.
        }
    }

    static boolean isOn() {
        return on;
    }

    /** [listener] runs on the main thread each time Battery Saver turns on or off. */
    static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }
}
