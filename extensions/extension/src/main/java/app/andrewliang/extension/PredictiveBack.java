package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime part of the "[General] Predictive back" patch for LINE.
 *
 * <p>The patch turns on {@code android:enableOnBackInvokedCallback} for each activity that LINE
 * has not set it on, and marks those activities with {@link #MARK}. With it, Android shows the
 * predictive back animation, and it stops calling {@code onBackPressed} and sending the back key
 * to the activity. Back then goes to {@code OnBackInvokedCallback}s: LINE's AndroidX back
 * callbacks, dialogs and the keyboard already use them.
 *
 * <p>Some activities still handle back in the old way: they override {@code onBackPressed},
 * {@code onKeyDown}, {@code onKeyUp} or {@code dispatchKeyEvent}. For each of them, this class
 * registers a bridge callback that replays the old path: the back key goes through
 * {@code dispatchKeyEvent}, and if nothing takes it, {@code onBackPressed} runs. Those screens keep
 * their old behavior, without the animation.
 *
 * <p>The same bridge goes on every marked activity on Android 13 (API 33), which reads the flag
 * but has no predictive animation, and when the switch is off. Older Android ignores the flag.
 */
public final class PredictiveBack {

    private PredictiveBack() {}

    /** The meta-data name that the patch puts on each activity that it opted in. */
    static final String MARK = "app.andrewliang.predictive_back";

    private static boolean registered;
    private static final Map<String, Boolean> MARKED = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> OLD_STYLE = new ConcurrentHashMap<>();

    /** Called by the patch at the start of {@code LineApplication.onCreate}. */
    public static void init(Application application) {
        if (registered || application == null || Build.VERSION.SDK_INT < 33) return;
        registered = true;
        application.registerActivityLifecycleCallbacks(new ViewFinder.Lifecycle() {
            @Override
            public void onActivityCreated(Activity activity, Bundle state) {
                try {
                    setUp(activity);
                } catch (RuntimeException ignored) {
                    // Back then works as Android does it for the flag.
                }
            }
        });
    }

    private static void setUp(Activity activity) {
        if (!isMarked(activity)) return;
        boolean bridge = Build.VERSION.SDK_INT < 34 || !LineSettings.predictiveBack()
                || handlesBackTheOldWay(activity.getClass());
        if (!bridge) return;
        OnBackInvokedCallback callback = () -> oldBack(activity);
        activity.getOnBackInvokedDispatcher()
                .registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
    }

    /** True if the patch opted this activity in. LINE's own opted-in activities are left alone. */
    private static boolean isMarked(Activity activity) {
        String name = activity.getClass().getName();
        Boolean cached = MARKED.get(name);
        if (cached != null) return cached;
        boolean marked = false;
        try {
            ActivityInfo info = activity.getPackageManager().getActivityInfo(
                    new ComponentName(activity, name), PackageManager.GET_META_DATA);
            marked = info.metaData != null && info.metaData.getBoolean(MARK, false);
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            // Not declared under its own name: leave it alone.
        }
        MARKED.put(name, marked);
        return marked;
    }

    /**
     * True if a class of LINE (not of Android, AndroidX or AppCompat) between [type] and
     * {@code Activity} overrides one of the old back methods.
     */
    static boolean handlesBackTheOldWay(Class<?> type) {
        Boolean cached = OLD_STYLE.get(type);
        if (cached != null) return cached;
        boolean found = false;
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (isLibrary(c)) break;
            if (declares(c, "onBackPressed")
                    || declares(c, "onKeyDown", int.class, KeyEvent.class)
                    || declares(c, "onKeyUp", int.class, KeyEvent.class)
                    || declares(c, "dispatchKeyEvent", KeyEvent.class)) {
                found = true;
                break;
            }
        }
        OLD_STYLE.put(type, found);
        return found;
    }

    private static boolean isLibrary(Class<?> c) {
        String name = c.getName();
        if (name.startsWith("android.") || name.startsWith("androidx.") || name.startsWith("java.")) return true;
        // AppCompatActivity is renamed by R8, but keeps this field. Its key methods do not
        // handle back.
        try {
            c.getDeclaredField("DELEGATE_TAG");
            return true;
        } catch (NoSuchFieldException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean declares(Class<?> c, String name, Class<?>... parameters) {
        try {
            Method method = c.getDeclaredMethod(name, parameters);
            return method != null;
        } catch (NoSuchMethodException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    /**
     * The back path from before the flag: a back key down and up through the activity, then
     * {@code onBackPressed} if no view and no key method took it. With the flag on,
     * {@code Activity.onKeyUp} no longer calls {@code onBackPressed} itself, so it is called here
     * when the up event is still tracked and not taken.
     */
    private static void oldBack(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        long now = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY, InputDevice.SOURCE_KEYBOARD);
        activity.dispatchKeyEvent(down);
        KeyEvent up = new KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY, InputDevice.SOURCE_KEYBOARD);
        boolean taken = activity.dispatchKeyEvent(up);
        if (!taken && up.isTracking() && !up.isCanceled()
                && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.onBackPressed();
        }
    }
}
