package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime part of the "[Chat] Haptic feedback" patch for LINE.
 *
 * <ul>
 *   <li>Long press on a message: {@link HapticFeedbackConstants#LONG_PRESS}. A gesture detector
 *       reads the touches of the chat window. When a view under the finger already buzzes on its
 *       own long press, this class stays quiet, so there is never a double buzz.</li>
 *   <li>Send: {@link HapticFeedbackConstants#CONFIRM} (Android 11+), or a keyboard tap on older
 *       versions, when the send button is tapped with text in the box.</li>
 *   <li>Unsend: {@link HapticFeedbackConstants#REJECT} (Android 11+) when "Unsend" or "Unsend
 *       silently" is tapped. The patch wraps those two buttons in {@link #setUnsendClickListener}.</li>
 * </ul>
 *
 * <p>Only {@link View#performHapticFeedback(int)} is used, without
 * {@code FLAG_IGNORE_GLOBAL_SETTING}, so the system touch feedback setting is always respected.
 */
public final class Haptics {

    private Haptics() {}

    private static final String CHAT_LIST = "chathistory_message_list";
    private static final String SEND_BUTTON = "chat_ui_send_button_image";
    private static final String MESSAGE_BOX = "chat_ui_message_edit";
    /** A send that clears the box this recently still counts as a send with text. */
    private static final long CLEARED_WINDOW_MS = 500;

    private static boolean registered;
    private static final Map<Window, TouchCallback> WINDOWS = new WeakHashMap<>();
    private static final Map<View, Boolean> WATCHED_BOXES = new WeakHashMap<>();
    private static long boxClearedAt;

    /** Called by the patch at the start of {@code LineApplication.onCreate}. */
    public static void init(Application application) {
        if (registered || application == null) return;
        registered = true;
        application.registerActivityLifecycleCallbacks(new ViewFinder.Lifecycle() {
            @Override
            public void onActivityResumed(Activity activity) {
                ViewFinder.find(activity, CHAT_LIST, list -> watchLongPress(activity, list));
                ViewFinder.find(activity, SEND_BUTTON, button -> watchSend(activity, button));
            }
        });
    }

    /**
     * Sets [listener] on an unsend button of the unsend dialog, with a buzz first. Called by the
     * patch in place of {@code setOnClickListener}.
     */
    public static void setUnsendClickListener(View button, View.OnClickListener listener) {
        if (listener == null) {
            button.setOnClickListener(null);
            return;
        }
        button.setOnClickListener(v -> {
            if (enabled()) v.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                    ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.VIRTUAL_KEY);
            listener.onClick(v);
        });
    }

    private static boolean enabled() {
        return LineSettings.chatHaptics();
    }

    // ---- Long press ----

    private static void watchLongPress(Activity activity, View list) {
        Window window = activity.getWindow();
        if (window == null || !(list instanceof ViewGroup)) return;
        TouchCallback callback = WINDOWS.get(window);
        if (callback == null || window.getCallback() != callback) {
            Window.Callback original = window.getCallback();
            if (original == null) return;
            // Something set a new callback over ours: the old one goes quiet, so each touch is
            // read only once.
            if (callback != null) callback.listener = null;
            callback = new TouchCallback(original, new LongPress(activity));
            WINDOWS.put(window, callback);
            window.setCallback(callback);
        }
        if (callback.listener instanceof LongPress) {
            ((LongPress) callback.listener).list = new WeakReference<>((ViewGroup) list);
        }
    }

    /** Buzzes on a long press over a message of the chat list. */
    private static final class LongPress extends GestureDetector.SimpleOnGestureListener
            implements TouchCallback.Listener {
        final GestureDetector detector;
        WeakReference<ViewGroup> list = new WeakReference<>(null);
        private final int[] location = new int[2];

        LongPress(Activity activity) {
            detector = new GestureDetector(activity, this);
        }

        @Override
        public void onTouch(MotionEvent event) {
            if (!enabled()) return;
            detector.onTouchEvent(event);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            ViewGroup list = this.list.get();
            if (list == null || !list.isAttachedToWindow() || !list.isShown()) return;
            list.getLocationInWindow(location);
            float x = e.getX() - location[0];
            float y = e.getY() - location[1];
            if (x < 0 || y < 0 || x >= list.getWidth() || y >= list.getHeight()) return;
            // Walk down to the deepest view under the finger.
            boolean pressable = false;
            View view = list;
            while (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                View hit = null;
                for (int i = group.getChildCount() - 1; i >= 0; i--) {
                    View child = group.getChildAt(i);
                    if (child.getVisibility() != View.VISIBLE) continue;
                    float cx = x + group.getScrollX() - child.getLeft() - child.getTranslationX();
                    float cy = y + group.getScrollY() - child.getTop() - child.getTranslationY();
                    if (cx >= 0 && cy >= 0 && cx < child.getWidth() && cy < child.getHeight()) {
                        hit = child;
                        x = cx;
                        y = cy;
                        break;
                    }
                }
                if (hit == null) break;
                view = hit;
                // Android buzzes by itself for a long-clickable view.
                if (hit.isLongClickable() && hit.isHapticFeedbackEnabled()) return;
                if (hit.isClickable() || hit.isLongClickable()) pressable = true;
            }
            // Only over a message, not the empty space between messages.
            if (view == list || !pressable) return;
            list.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }

    // ---- Send ----

    private static void watchSend(Activity activity, View button) {
        int boxId = BlurBars.id(activity, MESSAGE_BOX);
        View box = boxId == 0 || activity.getWindow() == null ? null
                : activity.getWindow().getDecorView().findViewById(boxId);
        if (!(box instanceof TextView)) return;
        TextView text = (TextView) box;
        if (!WATCHED_BOXES.containsKey(text)) {
            WATCHED_BOXES.put(text, Boolean.TRUE);
            text.addTextChangedListener(new TextWatcher() {
                boolean hadText;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    hadText = s.length() > 0;
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (hadText && s.length() == 0) boxClearedAt = SystemClock.uptimeMillis();
                }
            });
        }
        if (Build.VERSION.SDK_INT < 29) return; // No way to read the delegate before Android 10.
        View.AccessibilityDelegate current = button.getAccessibilityDelegate();
        if (current instanceof SendDelegate) {
            ((SendDelegate) current).box = new WeakReference<>(text);
            return;
        }
        button.setAccessibilityDelegate(new SendDelegate(current, text));
    }

    /**
     * Android sends {@link AccessibilityEvent#TYPE_VIEW_CLICKED} through the view's delegate on
     * each click, even with accessibility off. This delegate buzzes then, and passes every call on
     * to the delegate that LINE had set, if any.
     */
    private static final class SendDelegate extends View.AccessibilityDelegate {
        private final View.AccessibilityDelegate wrapped;
        WeakReference<TextView> box;

        SendDelegate(View.AccessibilityDelegate wrapped, TextView box) {
            this.wrapped = wrapped;
            this.box = new WeakReference<>(box);
        }

        @Override
        public void sendAccessibilityEvent(View host, int eventType) {
            if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && enabled()) {
                TextView text = box.get();
                boolean withText = text != null && (text.length() > 0
                        || SystemClock.uptimeMillis() - boxClearedAt < CLEARED_WINDOW_MS);
                if (withText) host.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                        ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.KEYBOARD_TAP);
            }
            if (wrapped != null) wrapped.sendAccessibilityEvent(host, eventType);
            else super.sendAccessibilityEvent(host, eventType);
        }

        @Override
        public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
            if (wrapped != null) wrapped.sendAccessibilityEventUnchecked(host, event);
            else super.sendAccessibilityEventUnchecked(host, event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            return wrapped != null ? wrapped.dispatchPopulateAccessibilityEvent(host, event)
                    : super.dispatchPopulateAccessibilityEvent(host, event);
        }

        @Override
        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            if (wrapped != null) wrapped.onPopulateAccessibilityEvent(host, event);
            else super.onPopulateAccessibilityEvent(host, event);
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            if (wrapped != null) wrapped.onInitializeAccessibilityEvent(host, event);
            else super.onInitializeAccessibilityEvent(host, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
            if (wrapped != null) wrapped.onInitializeAccessibilityNodeInfo(host, info);
            else super.onInitializeAccessibilityNodeInfo(host, info);
        }

        @Override
        public void addExtraDataToAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info,
                                                        String extraDataKey, android.os.Bundle arguments) {
            if (wrapped != null) wrapped.addExtraDataToAccessibilityNodeInfo(host, info, extraDataKey, arguments);
            else super.addExtraDataToAccessibilityNodeInfo(host, info, extraDataKey, arguments);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
            return wrapped != null ? wrapped.onRequestSendAccessibilityEvent(host, child, event)
                    : super.onRequestSendAccessibilityEvent(host, child, event);
        }

        @Override
        public android.view.accessibility.AccessibilityNodeProvider getAccessibilityNodeProvider(View host) {
            return wrapped != null ? wrapped.getAccessibilityNodeProvider(host) : super.getAccessibilityNodeProvider(host);
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, android.os.Bundle args) {
            return wrapped != null ? wrapped.performAccessibilityAction(host, action, args)
                    : super.performAccessibilityAction(host, action, args);
        }
    }
}
