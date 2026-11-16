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

/**
 * Runtime part of the "[Chat] Smooth keyboard animation" patch for LINE.
 *
 * <p>LINE already moves the chat input bar and the message list with the keyboard, frame by frame:
 * a {@code WindowInsetsAnimationCompat} callback on the chat root changes the height of
 * {@code chat_ui_keyboard_placeholder} on each frame. That callback stops the animation from going
 * further down the view tree, so this class listens one level above the chat root, with
 * {@link WindowInsetsAnimation.Callback#DISPATCH_MODE_CONTINUE_ON_SUBTREE}. LINE's callback still
 * gets every frame.
 *
 * <p>While the keyboard moves, the other patches skip their full view scans, which would run on
 * every frame because LINE lays out the chat on every frame. The blur keeps following the input bar
 * from its pre-draw pass. When the keyboard stops, one scan runs. Nothing runs between animations.
 *
 * <p>Android 11 (API 30) and later. Older versions have no inset animations, and nothing changes.
 */
public final class KeyboardMotion {

    private KeyboardMotion() {}

    private static final String CHAT_SPACER = "chat_ui_bottom_padding_spacer";

    private static boolean registered;
    /** The windows (decor views) where the keyboard is moving now. */
    private static final Map<View, Boolean> MOVING = new WeakHashMap<>();
    /** What runs in each window when the keyboard stops. */
    private static final Map<View, Runnable> ON_STOP = new WeakHashMap<>();
    /** The views that have the callback. */
    private static final Map<View, Boolean> INSTALLED = new WeakHashMap<>();

    /** Called by the patch at the start of {@code LineApplication.onCreate}. */
    public static void init(Application application) {
        if (registered || application == null || Build.VERSION.SDK_INT < 30) return;
        registered = true;
        application.registerActivityLifecycleCallbacks(new ViewFinder.Lifecycle() {
            @Override
            public void onActivityResumed(Activity activity) {
                watch(activity);
            }
        });
    }

    /** True while the keyboard moves in the window of [decor]. */
    static boolean isMoving(View decor) {
        return !MOVING.isEmpty() && MOVING.containsKey(decor);
    }

    /** Sets what runs in the window of [decor] each time the keyboard stops. Main thread only. */
    static void onStop(View decor, Runnable action) {
        ON_STOP.put(decor, action);
    }

    private static boolean enabled() {
        return LineSettings.smoothKeyboard();
    }

    private static void watch(Activity activity) {
        View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decor == null) return;
        ViewFinder.find(activity, CHAT_SPACER, spacer -> install(decor, spacer));
    }

    /** Puts the callback one level above the chat root, the parent of [spacer]. */
    private static void install(View decor, View spacer) {
        ViewParent root = spacer.getParent();
        ViewParent above = root == null ? null : root.getParent();
        if (!(above instanceof View)) return;
        View host = (View) above;
        if (INSTALLED.containsKey(host)) return;
        INSTALLED.put(host, Boolean.TRUE);
        host.setWindowInsetsAnimationCallback(new Callback(decor));
    }

    private static final class Callback extends WindowInsetsAnimation.Callback {
        private final View decor;
        private int running;

        Callback(View decor) {
            super(DISPATCH_MODE_CONTINUE_ON_SUBTREE);
            this.decor = decor;
        }

        @Override
        public void onPrepare(WindowInsetsAnimation animation) {
            if (!isIme(animation) || !enabled()) return;
            running++;
            MOVING.put(decor, Boolean.TRUE);
        }

        @Override
        public WindowInsets onProgress(WindowInsets insets, List<WindowInsetsAnimation> animations) {
            return insets;
        }

        @Override
        public void onEnd(WindowInsetsAnimation animation) {
            if (!isIme(animation) || running == 0) return;
            if (--running > 0) return;
            MOVING.remove(decor);
            Runnable action = ON_STOP.get(decor);
            if (action != null) action.run();
        }

        private static boolean isIme(WindowInsetsAnimation animation) {
            return (animation.getTypeMask() & WindowInsets.Type.ime()) != 0;
        }
    }
}
