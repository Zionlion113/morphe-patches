package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public final class Haptics {
    private static final String CHAT_LIST = "chathistory_message_list";
    private static final long CLEARED_WINDOW_MS = 500;
    private static final String MESSAGE_BOX = "chat_ui_message_edit";
    private static final String SEND_BUTTON = "chat_ui_send_button_image";
    private static long boxClearedAt;
    private static boolean registered;
    private static final String[] BNB_TABS = {
            "bnb_home_v2",
            "bnb_home26",
            "bnb_globalhome",
            "bnb_chat",
            "bnb_square",
            "bnb_timeline",
            "bnb_commerce",
            "bnb_commerce_tw",
            "bnb_news",
            "bnb_news_row",
            "bnb_call",
            "bnb_wallet",
            "bnb_mini",
    };
    private static final float SWIPE_REPLY_DP = 56.0f;
    private static final float SWIPE_RESET_DP = 20.0f;

    private static final Map<Window, TouchCallback> WINDOWS = new WeakHashMap<>();
    private static final Map<View, Boolean> WATCHED_BOXES = new WeakHashMap<>();
    private static final Map<View, Boolean> WATCHED_TABS = new WeakHashMap<>();
    private static final Map<View, Boolean> TOGGLE_STATE = new WeakHashMap<>();
    private static final Map<View, ViewTreeObserver.OnGlobalLayoutListener> SWITCH_SCANNERS = new WeakHashMap<>();

    private Haptics() {
    }

    public static void init(Application application) {
        if (registered || application == null) {
            return;
        }
        registered = true;
        application.registerActivityLifecycleCallbacks(new AnonymousClass1());
    }

    static class AnonymousClass1 extends ViewFinder.Lifecycle {
        AnonymousClass1() {
        }

        @Override // app.andrewliang.extension.ViewFinder.Lifecycle, android.app.Application.ActivityLifecycleCallbacks
        public void onActivityResumed(final Activity activity) {
            ViewFinder.find(activity, Haptics.CHAT_LIST, new ViewFinder.Found() { // from class: app.andrewliang.extension.Haptics$1$$ExternalSyntheticLambda0
                @Override // app.andrewliang.extension.ViewFinder.Found
                public final void found(View view) {
                    Haptics.watchLongPress(activity, view);
                }
            });
            ViewFinder.find(activity, Haptics.SEND_BUTTON, new ViewFinder.Found() { // from class: app.andrewliang.extension.Haptics$1$$ExternalSyntheticLambda1
                @Override // app.andrewliang.extension.ViewFinder.Found
                public final void found(View view) {
                    Haptics.watchSend(activity, view);
                }
            });
            ViewFinder.find(activity, "bnb_background_image", new ViewFinder.Found() {
                @Override
                public void found(View view) {
                    Haptics.watchBottomTabs(activity);
                }
            });
            Haptics.watchSwitches(activity);
        }

        @Override // app.andrewliang.extension.ViewFinder.Lifecycle, android.app.Application.ActivityLifecycleCallbacks
        public void onActivityDestroyed(Activity activity) {
            Window window = activity.getWindow();
            if (window == null) {
                return;
            }
            TouchCallback touchCallback = Haptics.WINDOWS.remove(window);
            if (touchCallback != null) {
                touchCallback.listener = null;
            }
            View decorView = window.getDecorView();
            ViewTreeObserver.OnGlobalLayoutListener scanner = Haptics.SWITCH_SCANNERS.remove(decorView);
            if (scanner != null) {
                ViewTreeObserver vto = decorView.getViewTreeObserver();
                if (vto != null && vto.isAlive()) {
                    vto.removeOnGlobalLayoutListener(scanner);
                }
            }
        }
    }

    public static void setUnsendClickListener(View view, final View.OnClickListener onClickListener) {
        if (onClickListener == null) {
            view.setOnClickListener(null);
        } else {
            view.setOnClickListener(new View.OnClickListener() { // from class: app.andrewliang.extension.Haptics$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    Haptics.lambda$setUnsendClickListener$0(onClickListener, view2);
                }
            });
        }
    }

    static /* synthetic */ void lambda$setUnsendClickListener$0(View.OnClickListener onClickListener, View view) {
        if (enabled()) {
            view.performHapticFeedback(Build.VERSION.SDK_INT >= 30 ? 17 : 1);
        }
        onClickListener.onClick(view);
    }

    public static boolean enabled() {
        return LineSettings.chatHaptics();
    }

    static void watchBottomTabs(Activity activity) {
        if (activity == null || activity.getWindow() == null) {
            return;
        }
        View decor = activity.getWindow().getDecorView();
        for (String tabName : BNB_TABS) {
            View tab = BlurBars.find(decor, tabName);
            if (tab == null) {
                continue;
            }
            View clickTarget = BlurBars.find(tab, "bnb_button_clickable_area");
            View target = clickTarget != null ? clickTarget : tab;
            if (WATCHED_TABS.containsKey(target)) {
                continue;
            }
            WATCHED_TABS.put(target, Boolean.TRUE);
            final WeakReference<View> tabRef = new WeakReference<>(tab);
            View.AccessibilityDelegate prev = Build.VERSION.SDK_INT >= 29 ? target.getAccessibilityDelegate() : null;
            target.setAccessibilityDelegate(new TabHapticDelegate(prev, tabRef));
        }
    }

    static void watchSwitches(Activity activity) {
        if (activity == null || activity.getWindow() == null || Build.VERSION.SDK_INT < 29) {
            return;
        }
        final View decor = activity.getWindow().getDecorView();
        if (SWITCH_SCANNERS.containsKey(decor)) {
            scanSwitches(decor);
            return;
        }
        final WeakReference<View> decorRef = new WeakReference<>(decor);
        ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private long lastScanMs;

            @Override
            public void onGlobalLayout() {
                long now = SystemClock.uptimeMillis();
                if (now - this.lastScanMs < 180L) {
                    return;
                }
                this.lastScanMs = now;
                View v = decorRef.get();
                if (v != null) {
                    scanSwitches(v);
                }
            }
        };
        SWITCH_SCANNERS.put(decor, listener);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        decor.post(() -> {
            View v = decorRef.get();
            if (v != null) {
                scanSwitches(v);
            }
        });
    }

    private static void scanSwitches(View root) {
        if (!(root instanceof ViewGroup)) {
            return;
        }
        try {
            ViewGroup group = (ViewGroup) root;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child == null || child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                if (child instanceof android.widget.Checkable && isSwitchWidget(child)) {
                    attachToggleHaptic(child, (android.widget.Checkable) child);
                } else if (child instanceof ViewGroup) {
                    scanSwitches(child);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isSwitchWidget(View view) {
        if (view instanceof android.widget.Switch) {
            return true;
        }
        String cls = view.getClass().getName();
        if (cls.contains("Switch")) {
            return true;
        }
        if (view instanceof android.widget.CheckedTextView) {
            int settingSwitchId = BlurBars.id(view.getContext(), "setting_switch");
            return settingSwitchId != 0 && view.getId() == settingSwitchId;
        }
        return false;
    }

    private static volatile long lastSwitchRowClickMs;

    private static void attachToggleHaptic(View view, final android.widget.Checkable checkable) {
        if (!TOGGLE_STATE.containsKey(view)) {
            TOGGLE_STATE.put(view, checkable.isChecked());
            View.AccessibilityDelegate prev = Build.VERSION.SDK_INT >= 29 ? view.getAccessibilityDelegate() : null;
            if (!(prev instanceof ToggleHapticDelegate)) {
                view.setAccessibilityDelegate(new ToggleHapticDelegate(prev));
            }
            attachSwitchRowDelegate(view, checkable);
        } else {
            TOGGLE_STATE.put(view, checkable.isChecked());
        }
    }

    private static void attachSwitchRowDelegate(View switchView, android.widget.Checkable checkable) {
        int containerId = BlurBars.id(switchView.getContext(), "setting_item_container");
        android.view.ViewParent p = switchView.getParent();
        int depth = 0;
        while (p instanceof View && depth < 5) {
            View pv = (View) p;
            if ((containerId != 0 && pv.getId() == containerId) || (pv.isClickable() && pv != switchView)) {
                View.AccessibilityDelegate prev = Build.VERSION.SDK_INT >= 29 ? pv.getAccessibilityDelegate() : null;
                if (!(prev instanceof SwitchRowClickDelegate)) {
                    pv.setAccessibilityDelegate(new SwitchRowClickDelegate(prev, switchView, checkable));
                }
                return;
            }
            p = pv.getParent();
            depth++;
        }
    }

    static void performToggleHaptic(View view, boolean checked) {
        if (view == null || !enabled()) {
            return;
        }
        int effect;
        if (Build.VERSION.SDK_INT >= 34) {
            effect = checked ? 21 /* HapticFeedbackConstants.TOGGLE_ON */ : 22 /* HapticFeedbackConstants.TOGGLE_OFF */;
        } else {
            effect = 4 /* HapticFeedbackConstants.CLOCK_TICK */;
        }
        view.performHapticFeedback(effect);
    }

    private static final class TabHapticDelegate extends ForwardingDelegate {
        private final WeakReference<View> tabRoot;

        TabHapticDelegate(View.AccessibilityDelegate wrapped, WeakReference<View> tabRoot) {
            super(wrapped);
            this.tabRoot = tabRoot;
        }

        @Override
        public void sendAccessibilityEvent(View host, int eventType) {
            if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && Haptics.enabled()) {
                View tab = this.tabRoot.get();
                boolean alreadySelected = (tab != null && tab.isSelected()) || host.isSelected();
                if (!alreadySelected) {
                    int effect = Build.VERSION.SDK_INT >= 34
                            ? 26 /* HapticFeedbackConstants.SEGMENT_TICK */
                            : 4 /* HapticFeedbackConstants.CLOCK_TICK */;
                    host.performHapticFeedback(effect);
                }
            }
            super.sendAccessibilityEvent(host, eventType);
        }
    }

    private static final class SwitchRowClickDelegate extends ForwardingDelegate {
        private final WeakReference<View> switchRef;
        private final WeakReference<android.widget.Checkable> checkableRef;

        SwitchRowClickDelegate(View.AccessibilityDelegate wrapped, View switchView, android.widget.Checkable checkable) {
            super(wrapped);
            this.switchRef = new WeakReference<>(switchView);
            this.checkableRef = new WeakReference<>(checkable);
        }

        @Override
        public void sendAccessibilityEvent(View host, int eventType) {
            if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && Haptics.enabled()) {
                Haptics.lastSwitchRowClickMs = SystemClock.uptimeMillis();
                final View sw = this.switchRef.get();
                final android.widget.Checkable ck = this.checkableRef.get();
                if (sw != null && ck != null) {
                    final boolean before = ck.isChecked();
                    sw.post(() -> {
                        boolean after = ck.isChecked();
                        Boolean prev = Haptics.TOGGLE_STATE.put(sw, after);
                        if (after != before && (prev == null || prev != after)) {
                            Haptics.performToggleHaptic(sw, after);
                        }
                    });
                }
            }
            super.sendAccessibilityEvent(host, eventType);
        }
    }

    private static final class ToggleHapticDelegate extends ForwardingDelegate {
        ToggleHapticDelegate(View.AccessibilityDelegate wrapped) {
            super(wrapped);
        }

        @Override
        public void sendAccessibilityEvent(View host, int eventType) {
            if (host instanceof android.widget.Checkable) {
                boolean isClick = eventType == AccessibilityEvent.TYPE_VIEW_CLICKED;
                boolean recentRowClick = SystemClock.uptimeMillis() - Haptics.lastSwitchRowClickMs < 250L;
                if (isClick || (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && recentRowClick)) {
                    boolean nowChecked = ((android.widget.Checkable) host).isChecked();
                    Boolean prev = Haptics.TOGGLE_STATE.put(host, nowChecked);
                    if (prev != null && prev != nowChecked) {
                        Haptics.performToggleHaptic(host, nowChecked);
                    }
                }
            }
            super.sendAccessibilityEvent(host, eventType);
        }
    }

    public static void watchLongPress(Activity activity, View view) {
        Window window = activity.getWindow();
        if (window == null || !(view instanceof ViewGroup)) {
            return;
        }
        Map<Window, TouchCallback> map = WINDOWS;
        TouchCallback touchCallback = map.get(window);
        if (touchCallback == null || window.getCallback() != touchCallback) {
            Window.Callback callback = window.getCallback();
            if (callback == null) {
                return;
            }
            if (touchCallback != null) {
                touchCallback.listener = null;
            }
            touchCallback = new TouchCallback(callback, new LongPress(activity));
            map.put(window, touchCallback);
            window.setCallback(touchCallback);
        }
        if (touchCallback.listener instanceof LongPress) {
            ((LongPress) touchCallback.listener).list = new WeakReference<>((ViewGroup) view);
        }
    }

    private static final class LongPress extends GestureDetector.SimpleOnGestureListener implements TouchCallback.Listener {
        final GestureDetector detector;
        WeakReference<ViewGroup> list = new WeakReference<>(null);
        private final int[] location = new int[2];
        private WeakReference<View> trackedRow = new WeakReference<>(null);
        private boolean swipeThresholdCrossed;

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onDown(MotionEvent motionEvent) {
            return true;
        }

        LongPress(Activity activity) {
            this.detector = new GestureDetector(activity, this);
        }

        @Override // app.andrewliang.extension.TouchCallback.Listener
        public void onTouch(MotionEvent motionEvent) {
            if (!Haptics.enabled()) {
                return;
            }
            this.detector.onTouchEvent(motionEvent);
            trackSwipeToReply(motionEvent);
        }

        private void trackSwipeToReply(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                this.swipeThresholdCrossed = false;
                this.trackedRow = new WeakReference<>(findMessageRowAt(event));
                return;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                this.swipeThresholdCrossed = false;
                this.trackedRow = new WeakReference<>(null);
                return;
            }
            if (action != MotionEvent.ACTION_MOVE) {
                return;
            }
            View row = this.trackedRow.get();
            if (row == null || !row.isAttachedToWindow()) {
                return;
            }
            float shiftedPx = maxHorizontalTranslation(row);
            float density = row.getResources().getDisplayMetrics().density;
            float activatePx = Haptics.SWIPE_REPLY_DP * density;
            float resetPx = Haptics.SWIPE_RESET_DP * density;
            if (!this.swipeThresholdCrossed && shiftedPx >= activatePx) {
                this.swipeThresholdCrossed = true;
                int effect = Build.VERSION.SDK_INT >= 34
                        ? 23 /* HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE */
                        : (Build.VERSION.SDK_INT >= 30 ? 16 /* CONFIRM */ : 4 /* CLOCK_TICK */);
                row.performHapticFeedback(effect);
            } else if (this.swipeThresholdCrossed && shiftedPx <= resetPx) {
                this.swipeThresholdCrossed = false;
                if (Build.VERSION.SDK_INT >= 34) {
                    row.performHapticFeedback(24 /* HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE */);
                }
            }
        }

        private View findMessageRowAt(MotionEvent event) {
            ViewGroup listGroup = this.list.get();
            if (listGroup == null || !listGroup.isAttachedToWindow() || !listGroup.isShown()) {
                return null;
            }
            listGroup.getLocationInWindow(this.location);
            float x = event.getX() - this.location[0];
            float y = event.getY() - this.location[1];
            if (x < 0.0f || y < 0.0f || x >= listGroup.getWidth() || y >= listGroup.getHeight()) {
                return null;
            }
            for (int i = listGroup.getChildCount() - 1; i >= 0; i--) {
                View child = listGroup.getChildAt(i);
                if (child != null && child.getVisibility() == View.VISIBLE
                        && x >= child.getLeft() && x < child.getRight()
                        && y >= child.getTop() && y < child.getBottom()) {
                    return child;
                }
            }
            return null;
        }

        private static float maxHorizontalTranslation(View view) {
            float max = Math.abs(view.getTranslationX());
            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                int count = vg.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = vg.getChildAt(i);
                    if (child != null && child.getVisibility() == View.VISIBLE) {
                        float childShift = Math.abs(child.getTranslationX());
                        if (childShift > max) {
                            max = childShift;
                        }
                    }
                }
            }
            return max;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public void onLongPress(MotionEvent motionEvent) {
            View childAt;
            ViewGroup viewGroup = this.list.get();
            if (viewGroup != null && viewGroup.isAttachedToWindow() && viewGroup.isShown()) {
                viewGroup.getLocationInWindow(this.location);
                float x = motionEvent.getX() - this.location[0];
                float y = motionEvent.getY() - this.location[1];
                if (x < 0.0f || y < 0.0f || x >= viewGroup.getWidth() || y >= viewGroup.getHeight()) {
                    return;
                }
                View view = viewGroup;
                boolean z = false;
                while (view instanceof ViewGroup) {
                    ViewGroup viewGroup2 = (ViewGroup) view;
                    int childCount = viewGroup2.getChildCount() - 1;
                    while (true) {
                        if (childCount < 0) {
                            childAt = null;
                            break;
                        }
                        childAt = viewGroup2.getChildAt(childCount);
                        if (childAt.getVisibility() == 0) {
                            float scrollX = ((viewGroup2.getScrollX() + x) - childAt.getLeft()) - childAt.getTranslationX();
                            float scrollY = ((viewGroup2.getScrollY() + y) - childAt.getTop()) - childAt.getTranslationY();
                            if (scrollX >= 0.0f && scrollY >= 0.0f && scrollX < childAt.getWidth() && scrollY < childAt.getHeight()) {
                                x = scrollX;
                                y = scrollY;
                                break;
                            }
                        }
                        childCount--;
                    }
                    if (childAt == null) {
                        break;
                    }
                    if (childAt.isLongClickable() && childAt.isHapticFeedbackEnabled()) {
                        return;
                    }
                    if (childAt.isClickable() || childAt.isLongClickable()) {
                        z = true;
                    }
                    view = childAt;
                }
                if (view == viewGroup || !z) {
                    return;
                }
                viewGroup.performHapticFeedback(0);
            }
        }
    }

    public static void watchSend(Activity activity, View view) {
        int iId = BlurBars.id(activity, MESSAGE_BOX);
        View viewFindViewById = (iId == 0 || activity.getWindow() == null) ? null : activity.getWindow().getDecorView().findViewById(iId);
        if (viewFindViewById instanceof TextView) {
            TextView textView = (TextView) viewFindViewById;
            Map<View, Boolean> map = WATCHED_BOXES;
            if (!map.containsKey(textView)) {
                map.put(textView, Boolean.TRUE);
                textView.addTextChangedListener(new TextWatcher() { // from class: app.andrewliang.extension.Haptics.2
                    boolean hadText;

                    @Override // android.text.TextWatcher
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    }

                    @Override // android.text.TextWatcher
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        this.hadText = charSequence.length() > 0;
                    }

                    @Override // android.text.TextWatcher
                    public void afterTextChanged(Editable editable) {
                        if (this.hadText && editable.length() == 0) {
                            Haptics.boxClearedAt = SystemClock.uptimeMillis();
                        }
                    }
                });
            }
            if (Build.VERSION.SDK_INT < 29) {
                return;
            }
            View.AccessibilityDelegate accessibilityDelegate = view.getAccessibilityDelegate();
            if (accessibilityDelegate instanceof SendDelegate) {
                ((SendDelegate) accessibilityDelegate).box = new WeakReference<>(textView);
            } else {
                view.setAccessibilityDelegate(new SendDelegate(accessibilityDelegate, textView));
            }
        }
    }

    private static class ForwardingDelegate extends View.AccessibilityDelegate {
        private final View.AccessibilityDelegate wrapped;

        ForwardingDelegate(View.AccessibilityDelegate wrapped) {
            this.wrapped = wrapped;
        }

        @Override // android.view.View.AccessibilityDelegate
        public void sendAccessibilityEvent(View view, int i) {
            if (this.wrapped != null) {
                this.wrapped.sendAccessibilityEvent(view, i);
            } else {
                super.sendAccessibilityEvent(view, i);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent) {
            if (this.wrapped != null) {
                this.wrapped.sendAccessibilityEventUnchecked(view, accessibilityEvent);
            } else {
                super.sendAccessibilityEventUnchecked(view, accessibilityEvent);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            return this.wrapped != null ? this.wrapped.dispatchPopulateAccessibilityEvent(view, accessibilityEvent) : super.dispatchPopulateAccessibilityEvent(view, accessibilityEvent);
        }

        @Override // android.view.View.AccessibilityDelegate
        public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            if (this.wrapped != null) {
                this.wrapped.onPopulateAccessibilityEvent(view, accessibilityEvent);
            } else {
                super.onPopulateAccessibilityEvent(view, accessibilityEvent);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            if (this.wrapped != null) {
                this.wrapped.onInitializeAccessibilityEvent(view, accessibilityEvent);
            } else {
                super.onInitializeAccessibilityEvent(view, accessibilityEvent);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            if (this.wrapped != null) {
                this.wrapped.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            } else {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public void addExtraDataToAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo, String str, Bundle bundle) {
            if (this.wrapped != null) {
                this.wrapped.addExtraDataToAccessibilityNodeInfo(view, accessibilityNodeInfo, str, bundle);
            } else {
                super.addExtraDataToAccessibilityNodeInfo(view, accessibilityNodeInfo, str, bundle);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
            return this.wrapped != null ? this.wrapped.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent) : super.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent);
        }

        @Override // android.view.View.AccessibilityDelegate
        public AccessibilityNodeProvider getAccessibilityNodeProvider(View view) {
            return this.wrapped != null ? this.wrapped.getAccessibilityNodeProvider(view) : super.getAccessibilityNodeProvider(view);
        }

        @Override // android.view.View.AccessibilityDelegate
        public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
            return this.wrapped != null ? this.wrapped.performAccessibilityAction(view, i, bundle) : super.performAccessibilityAction(view, i, bundle);
        }
    }

    private static final class SendDelegate extends ForwardingDelegate {
        WeakReference<TextView> box;

        SendDelegate(View.AccessibilityDelegate accessibilityDelegate, TextView textView) {
            super(accessibilityDelegate);
            this.box = new WeakReference<>(textView);
        }

        @Override // app.andrewliang.extension.Haptics.ForwardingDelegate, android.view.View.AccessibilityDelegate
        public void sendAccessibilityEvent(View view, int i) {
            TextView textView;
            if (i == 1 && Haptics.enabled() && (textView = this.box.get()) != null && (textView.length() > 0 || SystemClock.uptimeMillis() - Haptics.boxClearedAt < Haptics.CLEARED_WINDOW_MS)) {
                view.performHapticFeedback(Build.VERSION.SDK_INT >= 30 ? 16 : 3);
            }
            super.sendAccessibilityEvent(view, i);
        }
    }
}
