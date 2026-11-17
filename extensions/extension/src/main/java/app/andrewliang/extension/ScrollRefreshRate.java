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

public final class ScrollRefreshRate {
    private static final Map<View, Booster> BOOSTERS = new WeakHashMap();
    private static final String CHAT_LIST = "chathistory_message_list";
    private static final long IDLE_MS = 300;
    private static boolean registered;

    private ScrollRefreshRate() {
    }

    public static void init(Application application) {
        if (registered || application == null) {
            return;
        }
        registered = true;
        PowerSave.init(application);
        PowerSave.addListener(new Runnable() { // from class: app.andrewliang.extension.ScrollRefreshRate$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ScrollRefreshRate.lambda$init$0();
            }
        });
        application.registerActivityLifecycleCallbacks(new AnonymousClass1());
    }

    static /* synthetic */ void lambda$init$0() {
        if (PowerSave.isOn()) {
            for (Booster booster : new ArrayList<>(BOOSTERS.values())) {
                if (booster != null) {
                    booster.release();
                }
            }
        }
    }

    static class AnonymousClass1 extends ViewFinder.Lifecycle {
        AnonymousClass1() {
        }

        @Override // app.andrewliang.extension.ViewFinder.Lifecycle, android.app.Application.ActivityLifecycleCallbacks
        public void onActivityResumed(final Activity activity) {
            ViewFinder.find(activity, ScrollRefreshRate.CHAT_LIST, new ViewFinder.Found() { // from class: app.andrewliang.extension.ScrollRefreshRate$1$$ExternalSyntheticLambda0
                @Override // app.andrewliang.extension.ViewFinder.Found
                public final void found(View view) {
                    ScrollRefreshRate.watch(activity, view);
                }
            });
        }

        @Override // app.andrewliang.extension.ViewFinder.Lifecycle, android.app.Application.ActivityLifecycleCallbacks
        public void onActivityPaused(Activity activity) {
            Booster booster;
            if (activity.getWindow() == null || (booster = (Booster) ScrollRefreshRate.BOOSTERS.get(activity.getWindow().getDecorView())) == null) {
                return;
            }
            booster.release();
        }
    }

    public static void watch(Activity activity, View view) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View decorView = window.getDecorView();
        Map<View, Booster> map = BOOSTERS;
        Booster booster = map.get(decorView);
        if (booster == null) {
            booster = new Booster(window, decorView);
            map.put(decorView, booster);
            decorView.getViewTreeObserver().addOnScrollChangedListener(booster);
        }
        booster.list = new WeakReference<>(view);
    }

    static final class Booster implements ViewTreeObserver.OnScrollChangedListener {
        private boolean boosted;
        private final View decor;
        private float oldFrameRate;
        private int oldModeId;
        private View votedOn;
        private final WeakReference<Window> window;
        WeakReference<View> list = new WeakReference<>(null);
        private final Runnable idle = this::release;

        Booster(Window window, View view) {
            this.window = new WeakReference<>(window);
            this.decor = view;
        }

        @Override // android.view.ViewTreeObserver.OnScrollChangedListener
        public void onScrollChanged() {
            if (!LineSettings.scrollRefreshRate() || PowerSave.isOn()) {
                if (this.boosted) {
                    release();
                    return;
                }
                return;
            }
            View view = this.list.get();
            if (view != null && view.isAttachedToWindow() && view.isShown()) {
                if (!this.boosted) {
                    boost(view);
                }
                this.decor.removeCallbacks(this.idle);
                this.decor.postDelayed(this.idle, ScrollRefreshRate.IDLE_MS);
            }
        }

        private void boost(View view) {
            int iFastestMode;
            if (Build.VERSION.SDK_INT >= 36) {
                this.oldFrameRate = view.getRequestedFrameRate();
                view.setRequestedFrameRate(-4.0f);
                this.votedOn = view;
                this.boosted = true;
                return;
            }
            Window window = this.window.get();
            if (window == null || (iFastestMode = fastestMode(window)) == 0) {
                return;
            }
            WindowManager.LayoutParams attributes = window.getAttributes();
            this.oldModeId = attributes.preferredDisplayModeId;
            if (attributes.preferredDisplayModeId != iFastestMode) {
                attributes.preferredDisplayModeId = iFastestMode;
                window.setAttributes(attributes);
            }
            this.boosted = true;
        }

        void release() {
            this.decor.removeCallbacks(this.idle);
            if (this.boosted) {
                this.boosted = false;
                if (Build.VERSION.SDK_INT >= 36) {
                    View view = this.votedOn;
                    if (view != null) {
                        view.setRequestedFrameRate(this.oldFrameRate);
                    }
                    this.votedOn = null;
                    return;
                }
                Window window = this.window.get();
                if (window == null) {
                    return;
                }
                WindowManager.LayoutParams attributes = window.getAttributes();
                int i = attributes.preferredDisplayModeId;
                int i2 = this.oldModeId;
                if (i != i2) {
                    attributes.preferredDisplayModeId = i2;
                    window.setAttributes(attributes);
                }
            }
        }

        private static int fastestMode(Window window) {
            try {
                Display display = Build.VERSION.SDK_INT >= 30 ? window.getContext().getDisplay() : window.getWindowManager().getDefaultDisplay();
                if (display == null) {
                    return 0;
                }
                Display.Mode mode = display.getMode();
                Display.Mode mode2 = mode;
                for (Display.Mode mode3 : display.getSupportedModes()) {
                    if (mode3.getPhysicalWidth() == mode.getPhysicalWidth() && mode3.getPhysicalHeight() == mode.getPhysicalHeight() && mode3.getRefreshRate() > mode2.getRefreshRate()) {
                        mode2 = mode3;
                    }
                }
                return mode2.getModeId();
            } catch (RuntimeException unused) {
                return 0;
            }
        }
    }
}
