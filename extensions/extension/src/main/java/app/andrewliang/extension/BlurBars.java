package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime part of the "[General] Transparent system bars with blur" patch for LINE.
 *
 * <p>LINE draws edge to edge, and paints its own views under the system bars: a status bar view in
 * each header ({@code status_bar_background}), a strip under the bottom tabs
 * ({@code main_navigation_bar_background}) and a strip under the chat input
 * ({@code chat_ui_bottom_padding_spacer}). A theme gives these views its own colors, which shows as
 * blocks of color. This class hides them, so the status bar and the gesture bar are clear.
 *
 * <p>In a chat, the header stays on top and the messages scroll behind it. The header gets a
 * {@link ProgressiveBlurDrawable}: the messages behind it are blurred most at the top of the screen
 * and less toward the bottom of the header.
 *
 * <p>All views are found by resource name, because the ids change between LINE versions. A view
 * that this LINE version does not have is skipped.
 */
public final class BlurBars {

    private BlurBars() {}

    /** Views that only paint a color under a system bar. */
    private static final String[] BAR_VIEWS = {
            "status_bar_background",
            "main_navigation_bar_background",
            "chat_ui_bottom_padding_spacer",
    };

    /** Solid layers of the chat header. The blur takes their place. */
    private static final String[] CHAT_HEADER_LAYERS = {
            "seamless_header_background",
            "fallback_header_background",
            "default_color_animation_layer",
    };

    private static final String CHAT_HEADER = "chat_ui_main_header";
    private static final String CHAT_CONTENT = "chat_ui_main_content_container";
    private static final String CHAT_SKIN = "chat_ui_main_skin_view";
    private static final String CHAT_REFRESH = "chat_ui_message_list_refresh_layout";
    private static final String CHAT_LIST = "chathistory_message_list";

    /** A list that starts lower than this below the header has a banner above it. */
    private static final int BANNER_GAP_DP = 4;

    private static boolean registered;

    /** Resource ids by name. 0 when this LINE version has no such id. */
    private static final Map<String, Integer> IDS = new java.util.HashMap<>();

    /** Views kept invisible. LINE can make them visible again, so each frame checks them. */
    private static final Map<View, Boolean> HIDDEN = Collections.synchronizedMap(new WeakHashMap<>());

    /** Decor views that already have the listeners. */
    private static final Map<View, Boolean> WATCHED = new WeakHashMap<>();

    /** Decor views with a scan posted. */
    private static final Map<View, Boolean> PENDING = new WeakHashMap<>();

    /** Chat screens: the header and the views that the blur draws. */
    private static final Map<View, ChatScreen> CHATS = new WeakHashMap<>();

    /** Called by the patch at the start of {@code LineApplication.onCreate}. */
    public static void init(Application application) {
        if (registered || application == null) return;
        registered = true;
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle state) {
                watch(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                watch(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle state) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    /**
     * The status bar color that LINE's edge-to-edge helper sets: clear while the switch is on.
     * Android 15 and later ignore this color, so it changes only older Android versions.
     */
    public static void setStatusBarColor(Window window, int color) {
        window.setStatusBarColor(enabled() ? 0 : color);
    }

    /** The navigation bar color that LINE's edge-to-edge helper sets: clear while the switch is on. */
    public static void setNavigationBarColor(Window window, int color) {
        window.setNavigationBarColor(enabled() ? 0 : color);
    }

    static boolean enabled() {
        return LineSettings.blurSystemBars();
    }

    private static void watch(Activity activity) {
        if (!enabled()) return;
        Window window = activity.getWindow();
        if (window == null) return;
        if (Build.VERSION.SDK_INT >= 29) {
            // No gray scrim behind the system bars.
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        View decor = window.getDecorView();
        if (WATCHED.containsKey(decor)) return;
        WATCHED.put(decor, Boolean.TRUE);

        Runnable scan = () -> {
            PENDING.remove(decor);
            scan(decor);
        };
        ViewTreeObserver observer = decor.getViewTreeObserver();
        // A layout pass can add a fragment with new headers. Scan once after it.
        observer.addOnGlobalLayoutListener(() -> {
            if (PENDING.put(decor, Boolean.TRUE) == null) decor.post(scan);
        });
        observer.addOnPreDrawListener(() -> {
            keepHidden();
            ChatScreen chat = CHATS.get(decor);
            if (chat != null) chat.onPreDraw();
            return true;
        });
        decor.post(scan);
    }

    private static void scan(View decor) {
        if (!enabled()) return;
        Context context = decor.getContext();
        List<View> found = new ArrayList<>();
        int[] barIds = new int[BAR_VIEWS.length];
        for (int i = 0; i < BAR_VIEWS.length; i++) barIds[i] = id(context, BAR_VIEWS[i]);
        int listId = id(context, CHAT_LIST);
        collect(decor, barIds, listId, found);
        for (View view : found) hide(view);

        int headerId = id(context, CHAT_HEADER);
        View header = headerId == 0 ? null : decor.findViewById(headerId);
        if (header == null || !header.isAttachedToWindow()) {
            CHATS.remove(decor);
            return;
        }
        ChatScreen chat = CHATS.get(decor);
        if (chat == null || chat.header != header) {
            View content = find(decor, CHAT_CONTENT);
            if (content == null) return;
            chat = new ChatScreen(header, content, find(decor, CHAT_SKIN), find(decor, CHAT_REFRESH),
                    find(decor, CHAT_LIST));
            CHATS.put(decor, chat);
        }
        chat.update();
    }

    /** Collects the views with one of [ids]. It does not walk into the message list. */
    private static void collect(View view, int[] ids, int skipId, List<View> out) {
        int id = view.getId();
        if (id != View.NO_ID) {
            for (int wanted : ids) {
                if (wanted != 0 && wanted == id) {
                    out.add(view);
                    break;
                }
            }
            if (id == skipId) return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), ids, skipId, out);
        }
    }

    /**
     * Makes [view] invisible. It keeps its size, so the views placed against it do not move. A
     * view that LINE made GONE stays GONE.
     */
    static void hide(View view) {
        if (view == null) return;
        HIDDEN.put(view, Boolean.TRUE);
        if (view.getVisibility() == View.VISIBLE) view.setVisibility(View.INVISIBLE);
    }

    private static void keepHidden() {
        if (HIDDEN.isEmpty() || !enabled()) return;
        synchronized (HIDDEN) {
            for (View view : HIDDEN.keySet()) {
                if (view != null && view.getVisibility() == View.VISIBLE) view.setVisibility(View.INVISIBLE);
            }
        }
    }

    private static View find(View root, String name) {
        int id = id(root.getContext(), name);
        return id == 0 ? null : root.findViewById(id);
    }

    static int id(Context context, String name) {
        Integer cached = IDS.get(name);
        if (cached != null) return cached;
        int id = context.getResources().getIdentifier(name, "id", context.getPackageName());
        IDS.put(name, id);
        return id;
    }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /**
     * One chat screen. The header is a sibling of the content and is drawn above it. LINE places
     * the content below the header, so the message list is moved up behind the header and gets the
     * same amount of top padding. Thus the first message still starts below the header.
     */
    private static final class ChatScreen {
        final View header;
        final View content;
        final View skin;
        final View refresh;
        final View list;
        final ProgressiveBlurDrawable blur;

        /** How far the list was moved up. */
        int extended;
        /** The list padding that this class set, to see when LINE sets its own. */
        int setPadding = -1;
        /** The refresh layout margin that this class set. */
        int setMargin = Integer.MIN_VALUE;

        ChatScreen(View header, View content, View skin, View refresh, View list) {
            this.header = header;
            this.content = content;
            this.skin = skin;
            this.refresh = refresh;
            this.list = list;
            this.blur = new ProgressiveBlurDrawable(header, content, skin);
        }

        void update() {
            Context context = header.getContext();
            for (String name : CHAT_HEADER_LAYERS) {
                int id = id(context, name);
                if (id != 0) hide(header.findViewById(id));
            }
            if (header.getBackground() != blur) header.setBackground(blur);
            extendList();
        }

        void onPreDraw() {
            if (header.getBackground() != blur) {
                if (!enabled()) return;
                header.setBackground(blur);
            }
            if (content.isDirty() || (skin != null && skin.isDirty())) blur.invalidateSelf();
        }

        private void extendList() {
            if (refresh == null || list == null || !refresh.isAttachedToWindow()) return;
            if (!(refresh.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;
            if (header.getHeight() == 0 || refresh.getHeight() == 0) return;
            // Positions are read after layout. A pending layout still has the old ones.
            if (header.isLayoutRequested() || refresh.isLayoutRequested()) return;

            int[] location = new int[2];
            header.getLocationInWindow(location);
            int headerTop = location[1];
            int headerBottom = headerTop + header.getHeight();
            refresh.getLocationInWindow(location);
            // Where the list would start if this class had not moved it.
            int listTop = location[1] + extended;

            int wanted;
            if (listTop <= headerTop) {
                wanted = 0; // LINE already places the list behind the header.
            } else if (listTop - headerBottom > dp(context(), BANNER_GAP_DP)) {
                wanted = 0; // A banner is between the header and the list: keep it visible.
            } else {
                wanted = listTop - headerTop;
            }

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) refresh.getLayoutParams();
            int baseMargin = params.topMargin == setMargin ? params.topMargin + extended : params.topMargin;
            int basePadding = list.getPaddingTop() == setPadding ? setPadding - extended : list.getPaddingTop();
            int margin = baseMargin - wanted;
            int padding = basePadding + wanted;
            boolean changed = false;
            if (params.topMargin != margin) {
                params.topMargin = margin;
                refresh.setLayoutParams(params);
                changed = true;
            }
            if (list.getPaddingTop() != padding) {
                list.setPadding(list.getPaddingLeft(), padding, list.getPaddingRight(), list.getPaddingBottom());
                changed = true;
            }
            setMargin = margin;
            setPadding = padding;
            extended = wanted;
            if (wanted > 0) {
                if (list instanceof ViewGroup) ((ViewGroup) list).setClipToPadding(false);
                // The list now starts above its parents. Let them draw it there.
                View parent = (View) refresh.getParent();
                View stop = (View) header.getParent();
                while (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).setClipChildren(false);
                    ((ViewGroup) parent).setClipToPadding(false);
                    if (parent == stop || !(parent.getParent() instanceof View)) break;
                    parent = (View) parent.getParent();
                }
            }
            if (changed) blur.invalidateSelf();
        }

        private Context context() {
            return header.getContext();
        }
    }
}
