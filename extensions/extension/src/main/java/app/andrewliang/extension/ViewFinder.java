package app.andrewliang.extension;

import android.app.Activity;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Finds a view of an activity by resource name. Views that come with a fragment show up a few
 * layouts after the activity resumes, so the search runs again after each layout, for a few
 * layouts only. Screens without the view thus pay almost nothing.
 */
final class ViewFinder {

    private ViewFinder() {}

    /** How many layouts to wait for the view. */
    private static final int LAYOUT_TRIES = 12;

    interface Found {
        void found(View view);
    }

    /** Calls [found] once with the view [name] of [activity], if it shows up soon. */
    static void find(Activity activity, String name, Found found) {
        if (activity.getWindow() == null) return;
        View decor = activity.getWindow().getDecorView();
        int id = BlurBars.id(activity, name);
        if (id == 0) return;
        View view = decor.findViewById(id);
        if (view != null) {
            found.found(view);
            return;
        }
        int[] tries = {0};
        ViewTreeObserver.OnGlobalLayoutListener[] holder = new ViewTreeObserver.OnGlobalLayoutListener[1];
        holder[0] = () -> {
            View later = decor.findViewById(id);
            if (later != null || ++tries[0] >= LAYOUT_TRIES) {
                ViewTreeObserver observer = decor.getViewTreeObserver();
                if (observer.isAlive()) observer.removeOnGlobalLayoutListener(holder[0]);
                if (later != null) found.found(later);
            }
        };
        decor.getViewTreeObserver().addOnGlobalLayoutListener(holder[0]);
    }

    /** Lifecycle callbacks with empty methods, so a user overrides only what it needs. */
    abstract static class Lifecycle implements android.app.Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, android.os.Bundle state) {}

        @Override
        public void onActivityStarted(Activity activity) {}

        @Override
        public void onActivityResumed(Activity activity) {}

        @Override
        public void onActivityPaused(Activity activity) {}

        @Override
        public void onActivityStopped(Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(Activity activity, android.os.Bundle state) {}

        @Override
        public void onActivityDestroyed(Activity activity) {}
    }
}
