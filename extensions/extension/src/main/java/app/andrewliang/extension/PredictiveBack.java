package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.window.OnBackInvokedCallback;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PredictiveBack {
    static final String MARK = "app.andrewliang.predictive_back";
    private static final Map<String, Boolean> MARKED = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> OLD_STYLE = new ConcurrentHashMap<>();
    private static boolean registered;

    private PredictiveBack() {
    }

    public static void init(Application application) {
        if (registered || application == null || Build.VERSION.SDK_INT < 33) {
            return;
        }
        registered = true;
        application.registerActivityLifecycleCallbacks(new ViewFinder.Lifecycle() { // from class: app.andrewliang.extension.PredictiveBack.1
            @Override // app.andrewliang.extension.ViewFinder.Lifecycle, android.app.Application.ActivityLifecycleCallbacks
            public void onActivityCreated(Activity activity, Bundle bundle) {
                try {
                    PredictiveBack.setUp(activity);
                } catch (RuntimeException unused) {
                }
            }
        });
    }

    public static void setUp(final Activity activity) {
        if (activity == null || Build.VERSION.SDK_INT < 33) {
            return;
        }
        if (isMarked(activity)) {
            if (Build.VERSION.SDK_INT < 34 || !LineSettings.predictiveBack() || handlesBackTheOldWay(activity.getClass())) {
                activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, new OnBackInvokedCallback() { // from class: app.andrewliang.extension.PredictiveBack$$ExternalSyntheticLambda2
                    @Override // android.window.OnBackInvokedCallback
                    public final void onBackInvoked() {
                        PredictiveBack.oldBack(activity);
                    }
                });
            }
        }
    }

    private static boolean isMarked(Activity activity) {
        String name = activity.getClass().getName();
        Boolean bool = MARKED.get(name);
        if (bool != null) {
            return bool.booleanValue();
        }
        boolean z = false;
        try {
            ActivityInfo activityInfo = activity.getPackageManager().getActivityInfo(new ComponentName(activity, name), PackageManager.GET_META_DATA);
            if (activityInfo.metaData != null && activityInfo.metaData.getBoolean(MARK, false)) {
                z = true;
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException unused) {
        }
        MARKED.put(name, Boolean.valueOf(z));
        return z;
    }

    static boolean handlesBackTheOldWay(Class<?> cls) {
        boolean z;
        Boolean bool = OLD_STYLE.get(cls);
        if (bool != null) {
            return bool.booleanValue();
        }
        Class<?> superclass = cls;
        while (true) {
            z = false;
            if (superclass == null || isLibrary(superclass)) {
                break;
            }
            if (declares(superclass, "onBackPressed", new Class[0]) || declares(superclass, "onKeyDown", Integer.TYPE, KeyEvent.class) || declares(superclass, "onKeyUp", Integer.TYPE, KeyEvent.class) || declares(superclass, "dispatchKeyEvent", KeyEvent.class)) {
                z = true;
                break;
            }
            superclass = superclass.getSuperclass();
        }
        OLD_STYLE.put(cls, Boolean.valueOf(z));
        return z;
    }

    private static boolean isLibrary(Class<?> cls) {
        String name = cls.getName();
        if (name.startsWith("android.") || name.startsWith("androidx.") || name.startsWith("java.")) {
            return true;
        }
        try {
            cls.getDeclaredField("DELEGATE_TAG");
            return true;
        } catch (LinkageError | NoSuchFieldException | RuntimeException unused) {
            return false;
        }
    }

    private static boolean declares(Class<?> cls, String str, Class<?>... clsArr) {
        try {
            return cls.getDeclaredMethod(str, clsArr) != null;
        } catch (LinkageError | NoSuchMethodException | RuntimeException unused) {
            return false;
        }
    }

    public static void oldBack(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        activity.dispatchKeyEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, 4, 0, 0, -1, 0, 72, 257));
        KeyEvent keyEvent = new KeyEvent(jUptimeMillis, SystemClock.uptimeMillis(), 1, 4, 0, 0, -1, 0, 72, 257);
        if (activity.dispatchKeyEvent(keyEvent) || !keyEvent.isTracking() || keyEvent.isCanceled() || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        activity.onBackPressed();
    }
}
