package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class KeyboardMotion {
    private static final String CHAT_SPACER = "chat_ui_bottom_padding_spacer";
    private static boolean registered;
    private static final Map<View, Boolean> MOVING = new WeakHashMap();
    private static final Map<View, Runnable> ON_STOP = new WeakHashMap();
    private static final Map<View, Boolean> INSTALLED = new WeakHashMap();

    private KeyboardMotion() {
    }

    public static void init(Application application) {
        if (registered || application == null || Build.VERSION.SDK_INT < 30) {
            return;
        }
        registered = true;
        application.registerActivityLifecycleCallbacks(new ViewFinder.Lifecycle() { // from class: app.andrewliang.extension.KeyboardMotion.1
            @Override // app.andrewliang.extension.ViewFinder.Lifecycle, android.app.Application.ActivityLifecycleCallbacks
            public void onActivityResumed(Activity activity) {
                KeyboardMotion.watch(activity);
            }
        });
    }

    static boolean isMoving(View view) {
        Map<View, Boolean> map = MOVING;
        return !map.isEmpty() && map.containsKey(view);
    }

    static void onStop(View view, Runnable runnable) {
        ON_STOP.put(view, runnable);
    }

    public static boolean enabled() {
        return LineSettings.smoothKeyboard();
    }

    public static void watch(Activity activity) {
        final View decorView = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decorView == null) {
            return;
        }
        ViewFinder.find(activity, CHAT_SPACER, new ViewFinder.Found() { // from class: app.andrewliang.extension.KeyboardMotion$$ExternalSyntheticLambda1
            @Override // app.andrewliang.extension.ViewFinder.Found
            public final void found(View view) {
                KeyboardMotion.install(decorView, view);
            }
        });
    }

    public static void install(View view, View view2) {
        ViewParent parent = view2.getParent();
        Object parent2 = parent == null ? null : parent.getParent();
        if (parent2 instanceof View) {
            View view3 = (View) parent2;
            Map<View, Boolean> map = INSTALLED;
            if (map.containsKey(view3)) {
                return;
            }
            map.put(view3, Boolean.TRUE);
            view3.setWindowInsetsAnimationCallback(new Callback(view));
        }
    }

    private static final class Callback extends WindowInsetsAnimation.Callback {
        private final View decor;
        private int running;

        @Override // android.view.WindowInsetsAnimation.Callback
        public WindowInsets onProgress(WindowInsets windowInsets, List<WindowInsetsAnimation> list) {
            return windowInsets;
        }

        Callback(View view) {
            super(1);
            this.decor = view;
        }

        @Override // android.view.WindowInsetsAnimation.Callback
        public void onPrepare(WindowInsetsAnimation windowInsetsAnimation) {
            if (isIme(windowInsetsAnimation) && KeyboardMotion.enabled()) {
                this.running++;
                KeyboardMotion.MOVING.put(this.decor, Boolean.TRUE);
            }
        }

        @Override // android.view.WindowInsetsAnimation.Callback
        public void onEnd(WindowInsetsAnimation windowInsetsAnimation) {
            int i;
            if (!isIme(windowInsetsAnimation) || (i = this.running) == 0) {
                return;
            }
            int i2 = i - 1;
            this.running = i2;
            if (i2 > 0) {
                return;
            }
            KeyboardMotion.MOVING.remove(this.decor);
            Runnable runnable = (Runnable) KeyboardMotion.ON_STOP.get(this.decor);
            if (runnable != null) {
                runnable.run();
            }
        }

        private static boolean isIme(WindowInsetsAnimation windowInsetsAnimation) {
            return (windowInsetsAnimation.getTypeMask() & WindowInsets.Type.ime()) != 0;
        }
    }
}
