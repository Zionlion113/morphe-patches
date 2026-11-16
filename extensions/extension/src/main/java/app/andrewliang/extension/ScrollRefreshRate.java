package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime part of the "[Chat] High refresh rate while scrolling" patch for LINE.
 *
 * <p>While the message list of a chat scrolls or flings, the window asks for the highest refresh
 * rate of the screen. When the list has not moved for {@link #IDLE_MS}, the request is removed and
 * the system picks the rate again. In Battery Saver nothing is asked, and a running request ends at
 * once.
 *
 * <ul>
 *   <li>Android 16 (API 36) and later: {@link View#setRequestedFrameRate} with
 *       {@link View#REQUESTED_FRAME_RATE_CATEGORY_HIGH} on the list. This is a vote, with no call
 *       to the window manager.</li>
 *   <li>Older versions: the window's {@code preferredDisplayModeId} is set to the mode with the same
 *       resolution and the highest rate, and back to the old value when idle.</li>
 * </ul>
 *
 * <p>A scroll is seen through {@link ViewTreeObserver.OnScrollChangedListener}, which Android calls
 * before a frame is drawn when a view of the window scrolled. It costs nothing while idle.
 */
public final class ScrollRefreshRate {

    private ScrollRefreshRate() {}

    private static final String CHAT_LIST = "chathistory_message_list";
    /** The request ends after the list stays still this long. */
    private static final long IDLE_MS = 300;

    private static boolean registered;
    private static final Map<View, Booster> BOOSTERS = new WeakHashMap<>();

    /** Called by the patch at the start of {@code LineApplication.onCreate}. */
    public static void init(Application application) {
        if (registered || application == null) return;
        registered = true;
        PowerSave.init(application);
        PowerSave.addListener(() -> {
            if (!PowerSave.isOn()) return;
            for (Booster booster : new ArrayList<>(BOOSTERS.values())) {
                if (booster != null) booster.release();
            }
        });
        application.registerActivityLifecycleCallbacks(new ViewFinder.Lifecycle() {
            @Override
            public void onActivityResumed(Activity activity) {
                ViewFinder.find(activity, CHAT_LIST, list -> watch(activity, list));
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (activity.getWindow() == null) return;
                Booster booster = BOOSTERS.get(activity.getWindow().getDecorView());
                if (booster != null) booster.release();
            }
        });
    }

    private static void watch(Activity activity, View list) {
        Window window = activity.getWindow();
        if (window == null) return;
        View decor = window.getDecorView();
        Booster booster = BOOSTERS.get(decor);
        if (booster == null) {
            booster = new Booster(window, decor);
            BOOSTERS.put(decor, booster);
            decor.getViewTreeObserver().addOnScrollChangedListener(booster);
        }
        booster.list = new WeakReference<>(list);
    }

    private static final class Booster implements ViewTreeObserver.OnScrollChangedListener {
        private final WeakReference<Window> window;
        private final View decor;
        WeakReference<View> list = new WeakReference<>(null);
        private final Runnable idle = this::release;
        private boolean boosted;
        /** What this class replaced, to put it back. */
        private float oldFrameRate;
        private int oldModeId;
        private View votedOn;

        Booster(Window window, View decor) {
            this.window = new WeakReference<>(window);
            this.decor = decor;
        }

        @Override
        public void onScrollChanged() {
            if (!LineSettings.scrollRefreshRate() || PowerSave.isOn()) {
                if (boosted) release();
                return;
            }
            View list = this.list.get();
            if (list == null || !list.isAttachedToWindow() || !list.isShown()) return;
            if (!boosted) boost(list);
            decor.removeCallbacks(idle);
            decor.postDelayed(idle, IDLE_MS);
        }

        private void boost(View list) {
            if (Build.VERSION.SDK_INT >= 36) {
                oldFrameRate = list.getRequestedFrameRate();
                list.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH);
                votedOn = list;
                boosted = true;
                return;
            }
            Window window = this.window.get();
            if (window == null) return;
            int mode = fastestMode(window);
            if (mode == 0) return;
            WindowManager.LayoutParams params = window.getAttributes();
            oldModeId = params.preferredDisplayModeId;
            if (params.preferredDisplayModeId != mode) {
                params.preferredDisplayModeId = mode;
                window.setAttributes(params);
            }
            boosted = true;
        }

        void release() {
            decor.removeCallbacks(idle);
            if (!boosted) return;
            boosted = false;
            if (Build.VERSION.SDK_INT >= 36) {
                if (votedOn != null) votedOn.setRequestedFrameRate(oldFrameRate);
                votedOn = null;
                return;
            }
            Window window = this.window.get();
            if (window == null) return;
            WindowManager.LayoutParams params = window.getAttributes();
            if (params.preferredDisplayModeId != oldModeId) {
                params.preferredDisplayModeId = oldModeId;
                window.setAttributes(params);
            }
        }

        /** The mode with the resolution of the current one and the highest rate, or 0. */
        private static int fastestMode(Window window) {
            Display display;
            try {
                display = Build.VERSION.SDK_INT >= 30 ? window.getContext().getDisplay()
                        : window.getWindowManager().getDefaultDisplay();
            } catch (RuntimeException e) {
                return 0;
            }
            if (display == null) return 0;
            Display.Mode current = display.getMode();
            Display.Mode best = current;
            for (Display.Mode mode : display.getSupportedModes()) {
                if (mode.getPhysicalWidth() == current.getPhysicalWidth()
                        && mode.getPhysicalHeight() == current.getPhysicalHeight()
                        && mode.getRefreshRate() > best.getRefreshRate()) {
                    best = mode;
                }
            }
            return best.getModeId();
        }
    }
}
