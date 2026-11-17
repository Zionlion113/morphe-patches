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
    private static final Map<Window, TouchCallback> WINDOWS = new WeakHashMap();
    private static final Map<View, Boolean> WATCHED_BOXES = new WeakHashMap();

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

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onDown(MotionEvent motionEvent) {
            return true;
        }

        LongPress(Activity activity) {
            this.detector = new GestureDetector(activity, this);
        }

        @Override // app.andrewliang.extension.TouchCallback.Listener
        public void onTouch(MotionEvent motionEvent) {
            if (Haptics.enabled()) {
                this.detector.onTouchEvent(motionEvent);
            }
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

    private static final class SendDelegate extends View.AccessibilityDelegate {
        WeakReference<TextView> box;
        private final View.AccessibilityDelegate wrapped;

        SendDelegate(View.AccessibilityDelegate accessibilityDelegate, TextView textView) {
            this.wrapped = accessibilityDelegate;
            this.box = new WeakReference<>(textView);
        }

        @Override // android.view.View.AccessibilityDelegate
        public void sendAccessibilityEvent(View view, int i) {
            TextView textView;
            if (i == 1 && Haptics.enabled() && (textView = this.box.get()) != null && (textView.length() > 0 || SystemClock.uptimeMillis() - Haptics.boxClearedAt < Haptics.CLEARED_WINDOW_MS)) {
                view.performHapticFeedback(Build.VERSION.SDK_INT >= 30 ? 16 : 3);
            }
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            if (accessibilityDelegate != null) {
                accessibilityDelegate.sendAccessibilityEvent(view, i);
            } else {
                super.sendAccessibilityEvent(view, i);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            if (accessibilityDelegate != null) {
                accessibilityDelegate.sendAccessibilityEventUnchecked(view, accessibilityEvent);
            } else {
                super.sendAccessibilityEventUnchecked(view, accessibilityEvent);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            return accessibilityDelegate != null ? accessibilityDelegate.dispatchPopulateAccessibilityEvent(view, accessibilityEvent) : super.dispatchPopulateAccessibilityEvent(view, accessibilityEvent);
        }

        @Override // android.view.View.AccessibilityDelegate
        public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            if (accessibilityDelegate != null) {
                accessibilityDelegate.onPopulateAccessibilityEvent(view, accessibilityEvent);
            } else {
                super.onPopulateAccessibilityEvent(view, accessibilityEvent);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            if (accessibilityDelegate != null) {
                accessibilityDelegate.onInitializeAccessibilityEvent(view, accessibilityEvent);
            } else {
                super.onInitializeAccessibilityEvent(view, accessibilityEvent);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            if (accessibilityDelegate != null) {
                accessibilityDelegate.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            } else {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public void addExtraDataToAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo, String str, Bundle bundle) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            if (accessibilityDelegate != null) {
                accessibilityDelegate.addExtraDataToAccessibilityNodeInfo(view, accessibilityNodeInfo, str, bundle);
            } else {
                super.addExtraDataToAccessibilityNodeInfo(view, accessibilityNodeInfo, str, bundle);
            }
        }

        @Override // android.view.View.AccessibilityDelegate
        public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            return accessibilityDelegate != null ? accessibilityDelegate.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent) : super.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent);
        }

        @Override // android.view.View.AccessibilityDelegate
        public AccessibilityNodeProvider getAccessibilityNodeProvider(View view) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            return accessibilityDelegate != null ? accessibilityDelegate.getAccessibilityNodeProvider(view) : super.getAccessibilityNodeProvider(view);
        }

        @Override // android.view.View.AccessibilityDelegate
        public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
            View.AccessibilityDelegate accessibilityDelegate = this.wrapped;
            return accessibilityDelegate != null ? accessibilityDelegate.performAccessibilityAction(view, i, bundle) : super.performAccessibilityAction(view, i, bundle);
        }
    }
}
