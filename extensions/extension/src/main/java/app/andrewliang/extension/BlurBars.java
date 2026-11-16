package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsetsController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime part of the "[General] Transparent system bars with blur" patch for LINE.
 *
 * <p>LINE draws edge to edge, and paints its own views under the system bars. A theme gives these
 * views its own colors, which shows as blocks of color. This class handles them like this:
 * <ul>
 *   <li>A status bar view above a header with artwork ({@code status_bar_background}) and the
 *       gesture bar strip below the bottom tabs ({@code main_navigation_bar_background}) show the
 *       artwork next to them, mirrored at the edge. See {@link MirroredArtworkDrawable}.</li>
 *   <li>Other status bar views ({@code edge_to_edge_top}, or one with no artwork next to it) are
 *       hidden, so the view behind them shows.</li>
 *   <li>In a chat, the header and the input bar stay on top, and the messages scroll behind them.
 *       The header, the input bar and the gesture bar strip get a {@link ProgressiveBlurDrawable}.
 *       The messages behind them are blurred most at the screen edge.</li>
 * </ul>
 * The top bars of Compose screens are changed by the patch itself.
 *
 * <p>In Battery Saver, the blur turns into a plain dark scrim. This changes at once, with no restart.
 *
 * <p>All views are found by resource name, because the ids change between LINE versions. A view
 * that this LINE version does not have is skipped.
 */
public final class BlurBars {

    private BlurBars() {}

    /** Status bar views to hide. */
    private static final String[] HIDE_VIEWS = {
            "edge_to_edge_top",
    };

    private static final String STATUS_BAR = "status_bar_background";
    private static final String HEADER_ARTWORK = "fallback_header_background";
    private static final String HEADER_SEAMLESS = "seamless_header_background";
    private static final String NAVIGATION_BAR = "main_navigation_bar_background";
    private static final String TABS_ARTWORK = "bnb_background_image";

    /** Solid layers of the chat header. The blur takes their place. */
    private static final String[] CHAT_HEADER_LAYERS = {
            HEADER_SEAMLESS,
            HEADER_ARTWORK,
            "default_color_animation_layer",
    };

    private static final String CHAT_HEADER = "chat_ui_main_header";
    private static final String CHAT_CONTENT = "chat_ui_main_content_container";
    private static final String CHAT_SKIN = "chat_ui_main_skin_view";
    private static final String CHAT_REFRESH = "chat_ui_message_list_refresh_layout";
    private static final String CHAT_LIST = "chathistory_message_list";
    private static final String CHAT_INPUT = "chat_ui_message";
    private static final String CHAT_INPUT_ROW = "chat_ui_input_bar_container";
    private static final String CHAT_SPACER = "chat_ui_bottom_padding_spacer";

    /** A list that starts lower than this below the header has a banner above it. */
    private static final int BANNER_GAP_DP = 4;

    private static boolean registered;

    /** Resource ids by name. 0 when this LINE version has no such id. */
    private static final Map<String, Integer> IDS = new java.util.HashMap<>();

    /** Views kept invisible. LINE can make them visible again, so each frame checks them. */
    private static final Map<View, Boolean> HIDDEN = Collections.synchronizedMap(new WeakHashMap<>());

    /** Bar views that show the artwork next to them. */
    private static final Map<View, MirroredArtworkDrawable> MIRRORED = new WeakHashMap<>();

    /** Decor views that already have the listeners. */
    private static final Map<View, Boolean> WATCHED = new WeakHashMap<>();

    /** Decor views with a scan posted. */
    private static final Map<View, Boolean> PENDING = new WeakHashMap<>();

    /** Chat screens by decor view. */
    private static final Map<View, ChatScreen> CHATS = new WeakHashMap<>();

    /** Called by the patch at the start of {@code LineApplication.onCreate}. */
    public static void init(Application application) {
        if (registered || application == null) return;
        registered = true;
        watchPowerSave(application);
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

    /**
     * The color of the status bar strip of a Compose top bar, as a packed Compose color. Clear
     * while the switch is on, so the artwork behind it shows.
     */
    public static long composeStatusBarColor(long color) {
        return enabled() ? 0L : color;
    }

    /**
     * The modifier of the whole Compose top bar, status bar strip included. While the switch is on
     * it is the artwork modifier, so the artwork fills the status bar too.
     */
    public static Object composeOuterModifier(Object artwork, Object empty) {
        return enabled() ? artwork : empty;
    }

    /** The modifier of the bar below the status bar strip. Empty while the switch is on. */
    public static Object composeInnerModifier(Object artwork, Object empty) {
        return enabled() ? empty : artwork;
    }

    /**
     * How the Compose top bar scales its artwork. While the switch is on the artwork is cropped
     * instead of stretched, because it now fills a taller area.
     */
    public static Object composeArtworkScale(Object fillBounds, Object crop) {
        return enabled() ? crop : fillBounds;
    }

    static boolean enabled() {
        return LineSettings.blurSystemBars();
    }

    static boolean powerSave() {
        return PowerSave.isOn();
    }

    private static void watchPowerSave(Application application) {
        PowerSave.init(application);
        PowerSave.addListener(() -> {
            for (ChatScreen chat : new ArrayList<>(CHATS.values())) {
                if (chat != null) chat.redraw();
            }
        });
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
            // While the keyboard moves, LINE lays out the chat on every frame. The blur follows
            // it from onPreDraw, and one scan runs when the keyboard stops.
            if (KeyboardMotion.isMoving(decor)) return;
            if (PENDING.put(decor, Boolean.TRUE) == null) decor.post(scan);
        });
        KeyboardMotion.onStop(decor, () -> {
            if (PENDING.put(decor, Boolean.TRUE) == null) decor.post(scan);
        });
        observer.addOnPreDrawListener(() -> {
            if (!enabled()) return true;
            keepHidden();
            keepMirrored();
            ChatScreen chat = CHATS.get(decor);
            if (chat != null) chat.onPreDraw();
            return true;
        });
        decor.post(scan);
    }

    private static void scan(View decor) {
        if (!enabled()) return;
        Context context = decor.getContext();
        int headerId = id(context, CHAT_HEADER);
        View chatHeader = headerId == 0 ? null : decor.findViewById(headerId);

        int[] hideIds = new int[HIDE_VIEWS.length + 2];
        for (int i = 0; i < HIDE_VIEWS.length; i++) hideIds[i] = id(context, HIDE_VIEWS[i]);
        int statusId = id(context, STATUS_BAR);
        int navigationId = id(context, NAVIGATION_BAR);
        hideIds[HIDE_VIEWS.length] = statusId;
        hideIds[HIDE_VIEWS.length + 1] = navigationId;
        List<View> found = new ArrayList<>();
        collect(decor, hideIds, id(context, CHAT_LIST), found);
        for (View view : found) {
            int id = view.getId();
            if (id == statusId) {
                statusBar(view, chatHeader);
            } else if (id == navigationId) {
                View artwork = find(decor, TABS_ARTWORK);
                if (artwork != null) mirror(view, artwork, false);
                else hide(view);
            } else {
                hide(view);
            }
        }

        if (chatHeader == null || !chatHeader.isAttachedToWindow()) {
            CHATS.remove(decor);
            return;
        }
        ChatScreen chat = CHATS.get(decor);
        if (chat == null || chat.header != chatHeader) {
            View content = find(decor, CHAT_CONTENT);
            if (content == null) return;
            chat = new ChatScreen(decor, chatHeader, content, find(decor, CHAT_SKIN), find(decor, CHAT_REFRESH),
                    find(decor, CHAT_LIST), find(decor, CHAT_INPUT), find(decor, CHAT_SPACER));
            CHATS.put(decor, chat);
        }
        chat.update();
    }

    /**
     * A header's status bar view. In a chat the blur covers it. Above artwork it shows the
     * artwork. A seamless header already draws behind the status bar, so the view is hidden.
     */
    private static void statusBar(View status, View chatHeader) {
        if (chatHeader != null && isInside(status, chatHeader)) {
            hide(status);
            return;
        }
        ViewParent parent = status.getParent();
        if (!(parent instanceof View)) {
            hide(status);
            return;
        }
        View root = (View) parent;
        View seamless = find(root, HEADER_SEAMLESS);
        View artwork = find(root, HEADER_ARTWORK);
        boolean seamlessShown = seamless != null && seamless.getVisibility() == View.VISIBLE
                && seamless.getBackground() != null;
        if (artwork != null && !seamlessShown && artwork.getVisibility() == View.VISIBLE) {
            mirror(status, artwork, true);
        } else {
            hide(status);
        }
    }

    private static boolean isInside(View view, View ancestor) {
        for (ViewParent parent = view.getParent(); parent != null; parent = parent.getParent()) {
            if (parent == ancestor) return true;
        }
        return false;
    }

    private static void mirror(View bar, View artwork, boolean barAbove) {
        MirroredArtworkDrawable drawable = MIRRORED.get(bar);
        if (drawable == null || drawable.artwork() != artwork) {
            drawable = new MirroredArtworkDrawable(artwork, bar, barAbove);
            MIRRORED.put(bar, drawable);
        }
        HIDDEN.remove(bar);
        if (bar.getVisibility() == View.INVISIBLE) bar.setVisibility(View.VISIBLE);
        if (bar.getBackground() != drawable) bar.setBackground(drawable);
    }

    /** LINE sets the theme color again on some changes. Each frame puts the artwork back. */
    private static void keepMirrored() {
        if (MIRRORED.isEmpty()) return;
        for (Map.Entry<View, MirroredArtworkDrawable> entry : new ArrayList<>(MIRRORED.entrySet())) {
            View bar = entry.getKey();
            MirroredArtworkDrawable drawable = entry.getValue();
            if (bar == null || drawable == null || !bar.isAttachedToWindow()) continue;
            if (bar.getBackground() != drawable) bar.setBackground(drawable);
            if (drawable.artwork().isDirty()) bar.invalidate();
        }
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
        if (HIDDEN.isEmpty()) return;
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
     * One chat screen. The header and the input bar are drawn above the content. LINE places the
     * message list between them, so the list is stretched behind both, and gets the same amount of
     * padding. Thus the first and the last message can still scroll clear of the bars.
     */
    private static final class ChatScreen {
        final View decor;
        final View header;
        final View content;
        final View skin;
        final View refresh;
        final View list;
        final View input;
        final View spacer;
        final ProgressiveBlurDrawable.Region top;
        final ProgressiveBlurDrawable.Region bottom;
        final ProgressiveBlurDrawable headerBlur;
        final ProgressiveBlurDrawable inputBlur;
        final ProgressiveBlurDrawable spacerBlur;

        /** How far the list was moved up, and down. */
        int extendedTop;
        int extendedBottom;
        /** The list padding and refresh layout margins that this class set, to see LINE's own. */
        int setPaddingTop = -1;
        int setPaddingBottom = -1;
        int setMarginTop = Integer.MIN_VALUE;
        int setMarginBottom = Integer.MIN_VALUE;

        ChatScreen(View decor, View header, View content, View skin, View refresh, View list, View input,
                   View spacer) {
            this.decor = decor;
            this.header = header;
            this.content = content;
            this.skin = skin;
            this.refresh = refresh;
            this.list = list;
            this.input = input;
            this.spacer = spacer;
            top = new ProgressiveBlurDrawable.Region(false, new View[] {header}, new View[] {skin, content});
            // The input bar is not drawn into its own blur, so the list is the bottom source.
            bottom = new ProgressiveBlurDrawable.Region(true, new View[] {input, spacer},
                    new View[] {skin, refresh});
            headerBlur = new ProgressiveBlurDrawable(header, top);
            inputBlur = input == null ? null : new ProgressiveBlurDrawable(input, bottom);
            spacerBlur = spacer == null ? null : new ProgressiveBlurDrawable(spacer, bottom);
        }

        void update() {
            Context context = header.getContext();
            for (String name : CHAT_HEADER_LAYERS) {
                int id = id(context, name);
                if (id != 0) hide(header.findViewById(id));
            }
            applyBackgrounds();
            extendList();
        }

        void redraw() {
            top.invalidate();
            bottom.invalidate();
        }

        private void applyBackgrounds() {
            if (header.getBackground() != headerBlur) header.setBackground(headerBlur);
            if (input != null) {
                if (input.getBackground() != inputBlur) input.setBackground(inputBlur);
                // The row of the input box. The box and the buttons in it stay.
                View row = find(input, CHAT_INPUT_ROW);
                if (row != null && row.getBackground() != null) row.setBackground(null);
            }
            if (spacer != null) {
                if (spacer.getVisibility() == View.INVISIBLE) spacer.setVisibility(View.VISIBLE);
                if (spacer.getBackground() != spacerBlur) spacer.setBackground(spacerBlur);
            }
        }

        void onPreDraw() {
            applyBackgrounds();
            boolean skinDirty = skin != null && skin.isDirty();
            if (skinDirty || content.isDirty()) top.invalidate();
            if (skinDirty || (refresh != null && refresh.isDirty())) bottom.invalidate();
            lightIcons();
        }

        /** White system bar icons over a dark tint. */
        private void lightIcons() {
            boolean saver = powerSave();
            boolean status = saver || LineSettings.blurTopDarkTint();
            boolean navigation = saver || LineSettings.blurBottomDarkTint();
            if (!status && !navigation) return;
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller == null) return;
                int mask = (status ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0)
                        | (navigation ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0);
                if ((controller.getSystemBarsAppearance() & mask) != 0) controller.setSystemBarsAppearance(0, mask);
            } else {
                int flags = decor.getSystemUiVisibility();
                int mask = (status ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0)
                        | (navigation && Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
                if ((flags & mask) != 0) decor.setSystemUiVisibility(flags & ~mask);
            }
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
            // Where the list would start and end if this class had not moved it.
            int listTop = location[1] + extendedTop;
            int listBottom = location[1] + refresh.getHeight() - extendedBottom;

            int wantedTop;
            if (listTop <= headerTop) {
                wantedTop = 0; // LINE already places the list behind the header.
            } else if (listTop - headerBottom > dp(header.getContext(), BANNER_GAP_DP)) {
                wantedTop = 0; // A banner is between the header and the list: keep it visible.
            } else {
                wantedTop = listTop - headerTop;
            }
            // Down to the bottom of the gesture bar strip, or of the input bar.
            int barsBottom = bottom.union().bottom;
            int wantedBottom = barsBottom > listBottom ? barsBottom - listBottom : 0;

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) refresh.getLayoutParams();
            int baseMarginTop = params.topMargin == setMarginTop ? params.topMargin + extendedTop : params.topMargin;
            int baseMarginBottom = params.bottomMargin == setMarginBottom
                    ? params.bottomMargin + extendedBottom : params.bottomMargin;
            int basePaddingTop = list.getPaddingTop() == setPaddingTop
                    ? setPaddingTop - extendedTop : list.getPaddingTop();
            int basePaddingBottom = list.getPaddingBottom() == setPaddingBottom
                    ? setPaddingBottom - extendedBottom : list.getPaddingBottom();
            int marginTop = baseMarginTop - wantedTop;
            int marginBottom = baseMarginBottom - wantedBottom;
            int paddingTop = basePaddingTop + wantedTop;
            int paddingBottom = basePaddingBottom + wantedBottom;
            boolean changed = false;
            if (params.topMargin != marginTop || params.bottomMargin != marginBottom) {
                params.topMargin = marginTop;
                params.bottomMargin = marginBottom;
                refresh.setLayoutParams(params);
                changed = true;
            }
            if (list.getPaddingTop() != paddingTop || list.getPaddingBottom() != paddingBottom) {
                list.setPadding(list.getPaddingLeft(), paddingTop, list.getPaddingRight(), paddingBottom);
                changed = true;
            }
            setMarginTop = marginTop;
            setMarginBottom = marginBottom;
            setPaddingTop = paddingTop;
            setPaddingBottom = paddingBottom;
            extendedTop = wantedTop;
            extendedBottom = wantedBottom;
            if (wantedTop > 0 || wantedBottom > 0) {
                if (list instanceof ViewGroup) ((ViewGroup) list).setClipToPadding(false);
                // The list now goes past its parents. Let them draw it there.
                View stop = spacer != null && spacer.getParent() instanceof View
                        ? (View) spacer.getParent() : (View) header.getParent();
                View parent = (View) refresh.getParent();
                while (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).setClipChildren(false);
                    ((ViewGroup) parent).setClipToPadding(false);
                    if (parent == stop || !(parent.getParent() instanceof View)) break;
                    parent = (View) parent.getParent();
                }
            }
            if (changed) redraw();
        }
    }
}
