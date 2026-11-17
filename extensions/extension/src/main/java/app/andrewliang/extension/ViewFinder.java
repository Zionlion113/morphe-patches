package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

final class ViewFinder {
    private static final int LAYOUT_TRIES = 12;

    interface Found {
        void found(View view);
    }

    private ViewFinder() {
    }

    static void find(Activity activity, String str, final Found found) {
        if (activity.getWindow() == null) {
            return;
        }
        final View decorView = activity.getWindow().getDecorView();
        final int iId = BlurBars.id(activity, str);
        if (iId == 0) {
            return;
        }
        View viewFindViewById = decorView.findViewById(iId);
        if (viewFindViewById != null) {
            found.found(viewFindViewById);
            return;
        }
        final int[] iArr = {0};
        final ViewTreeObserver.OnGlobalLayoutListener[] onGlobalLayoutListenerArr = new ViewTreeObserver.OnGlobalLayoutListener[1];
        onGlobalLayoutListenerArr[0] = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: app.andrewliang.extension.ViewFinder$$ExternalSyntheticLambda0
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public final void onGlobalLayout() {
                ViewFinder.lambda$find$0(decorView, iId, iArr, onGlobalLayoutListenerArr, found);
            }
        };
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListenerArr[0]);
    }

    static /* synthetic */ void lambda$find$0(View view, int i, int[] iArr, ViewTreeObserver.OnGlobalLayoutListener[] onGlobalLayoutListenerArr, Found found) {
        View viewFindViewById = view.findViewById(i);
        if (viewFindViewById == null) {
            int i2 = iArr[0] + 1;
            iArr[0] = i2;
            if (i2 < LAYOUT_TRIES) {
                return;
            }
        }
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListenerArr[0]);
        }
        if (viewFindViewById != null) {
            found.found(viewFindViewById);
        }
    }

    static abstract class Lifecycle implements Application.ActivityLifecycleCallbacks {
        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityCreated(Activity activity, Bundle bundle) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityDestroyed(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityPaused(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityResumed(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStarted(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStopped(Activity activity) {
        }

        Lifecycle() {
        }
    }
}
