package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

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
 *   <li>The chat background fills the whole window, from the status bar to the gesture bar, cropped
 *       to the window so it is not stretched. The rounded input box is clipped with
 *       {@link ViewOutlineProvider}, so its corners are drawn by the GPU and stay smooth.</li>
 * </ul>
 * The top bars of Compose screens are changed by the patch itself.
 *
 * <p>The chat header and the input bar follow a four-state frost (system dark mode crossed with Battery Saver). That look, and the icon colors that go with it, change at once, with no restart.
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
    private static final String CHAT_INPUT_PILL = "chat_ui_message_container";
    private static final String CHAT_INPUT_PILL_BG = "chat_ui_message_edit_text_background";

    /** Fill of the input capsule, as measured from the mockups, in every theme. */
    private static final int INPUT_PILL_FILL = 0xFFF5F5F5;
    private static final int ICON_DAY = 0xFF111111;
    private static final int ICON_EMOJI_DAY = 0xFF7B7B7B;
    private static final int ICON_NIGHT = 0xFFFFFFFF;
    private static final int HEADER_ICON_DAY_SAVER = 0xFF000000;
    private static final int HEADER_ICON_LIGHT = 0xFFFEFEFE;

    private static final String[] INPUT_DARK_ICONS = {
            "chathistory_attach_button",
            "chat_ui_camera_button",
            "chat_ui_gallery_button",
    };
    private static final String[] INPUT_EMOJI_ICONS = {
            "chat_ui_esk_button",
            "chat_ui_text_keyboard_button",
    };
    private static final String INPUT_MIC = "chat_ui_send_button_image";
    private static final String[] HEADER_TEXT = {
            "header_title",
            "header_title_count",
    };
    private static final String[] HEADER_ICONS = {
            "header_up_button",
    };
    private static final String HEADER_AVATAR = "header_title_left_image";

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

    /** Skin views with a layout fix posted from {@link #layoutChatSkin}. */
    private static final Map<View, Boolean> SKIN_PREPARE = new WeakHashMap<>();

    /** Chat screens by decor view. */
    private static final Map<View, ChatScreen> CHATS = new WeakHashMap<>();

    /** Called by the patch at the start of {@code LineApplication.onCreate}. */
    public static void init(Application application) {
        if (registered || application == null) return;
        registered = true;
        watchPowerSave(application);
        watchConfiguration(application);
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

    /**
     * Lays the chat background out to fill this view, cropped and without stretching. Called from
     * the patched {@code ChatHistorySkinImageView.onDraw}, after LINE has set the drawable bounds
     * to a shorter area that stops above the input bar and the gesture bar.
     */
    public static void layoutChatSkin(ImageView view) {
        if (!enabled() || view == null) return;
        if (needsPrepare(view) && SKIN_PREPARE.put(view, Boolean.TRUE) == null) {
            view.post(() -> {
                SKIN_PREPARE.remove(view);
                prepareChatSkin(view);
            });
        }
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) return;
        Drawable drawable = view.getDrawable() != null ? view.getDrawable() : view.getBackground();
        if (drawable == null) return;
        drawable.setBounds(cropBounds(drawable, width, height));
    }

    /**
     * The size of the chat background, for LINE's own capture of it. The view's size while it is
     * laid out and the switch is on, otherwise {@code null} so LINE's own size is kept.
     */
    public static Size chatSkinArea(ImageView view) {
        if (!enabled() || view == null) return null;
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) return null;
        return new Size(width, height);
    }

    /**
     * Makes the chat background and its parents fill the window. A solid color on the window is
     * cleared only when the skin is a covering image, so a default or solid-color skin keeps its
     * color and still reaches the gesture bar. Returns true while the switch is on, so the patch
     * can skip LINE's own top margin of the status bar height.
     */
    public static boolean prepareChatSkin(View view) {
        if (!enabled() || view == null) return false;
        clearWindowBackground(view);
        View root = view.getRootView();
        View current = view;
        while (current != null) {
            // The decor view's padding is left alone: the IME uses it when the window resizes.
            if (current != root) fillWindow(current);
            unclip(current);
            ViewParent parent = current.getParent();
            if (!(parent instanceof View) || current == root) break;
            current = (View) parent;
        }
        return true;
    }

    private static boolean needsPrepare(View view) {
        if (view.getFitsSystemWindows()) return true;
        if (hasPadding(view) && !imePadding(view)) return true;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams margin = (ViewGroup.MarginLayoutParams) params;
            if (margin.topMargin != 0 || margin.bottomMargin != 0
                    || margin.leftMargin != 0 || margin.rightMargin != 0) {
                return true;
            }
        }
        if (params != null && (params.width != ViewGroup.LayoutParams.MATCH_PARENT
                || params.height != ViewGroup.LayoutParams.MATCH_PARENT)) {
            return true;
        }
        return false;
    }

    /** Lets [view] fill its parent, with no inset padding or margin. */
    private static void fillWindow(View view) {
        if (view.getFitsSystemWindows()) view.setFitsSystemWindows(false);
        // Do not clear IME padding: the chat window uses adjustResize.
        if (hasPadding(view) && !imePadding(view)) view.setPadding(0, 0, 0, 0);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) return;
        boolean changed = false;
        if (params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            changed = true;
        }
        if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            changed = true;
        }
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams margin = (ViewGroup.MarginLayoutParams) params;
            if (margin.topMargin != 0 || margin.bottomMargin != 0
                    || margin.leftMargin != 0 || margin.rightMargin != 0) {
                margin.topMargin = 0;
                margin.bottomMargin = 0;
                margin.leftMargin = 0;
                margin.rightMargin = 0;
                changed = true;
            }
        }
        if (changed) view.setLayoutParams(params);
    }

    static void unclip(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (group.getClipChildren()) group.setClipChildren(false);
            if (group.getClipToPadding()) group.setClipToPadding(false);
        }
        if (view.getClipToOutline()) view.setClipToOutline(false);
    }

    private static boolean hasPadding(View view) {
        return view.getPaddingLeft() != 0 || view.getPaddingTop() != 0
                || view.getPaddingRight() != 0 || view.getPaddingBottom() != 0;
    }

    private static boolean imePadding(View view) {
        if (view.getPaddingBottom() == 0 || Build.VERSION.SDK_INT < 30) return false;
        android.view.WindowInsets insets = view.getRootWindowInsets();
        if (insets == null || !insets.isVisible(android.view.WindowInsets.Type.ime())) return false;
        return view.getPaddingBottom() >= insets.getInsets(android.view.WindowInsets.Type.ime()).bottom / 2;
    }

    /**
     * When the skin is a photo or a theme image, clear a solid background on the window, the decor
     * view, the content view and the chat root, so the image can fill the window. The skin's own
     * background is left alone: the default chat color and a solid-color skin live there, and
     * clearing them would leave the window black. Those colors still fill the window because
     * {@link #fillWindow} stretches the skin to the gesture bar.
     */
    private static void clearWindowBackground(View view) {
        if (!hasCoveringArtwork(view)) return;
        Activity activity = activityOf(view.getContext());
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;
        makeTransparent(window.peekDecorView());
        View content = window.findViewById(android.R.id.content);
        if (content != null) makeTransparent(content);
        Drawable background = window.getDecorView().getBackground();
        if (!isTransparent(background)) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        if (view.getParent() instanceof View) makeTransparent((View) view.getParent());
    }

    /** A photo or theme image that can cover the window, not a solid or the default color. */
    private static boolean hasCoveringArtwork(View view) {
        Drawable drawable = null;
        if (view instanceof ImageView) drawable = ((ImageView) view).getDrawable();
        if (!isArtwork(drawable)) drawable = view.getBackground();
        return isArtwork(drawable);
    }

    private static boolean isArtwork(Drawable drawable) {
        if (drawable == null || drawable instanceof ColorDrawable) return false;
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layer = (LayerDrawable) drawable;
            for (int i = 0; i < layer.getNumberOfLayers(); i++) {
                if (isArtwork(layer.getDrawable(i))) return true;
            }
            return false;
        }
        return true;
    }

    private static void makeTransparent(View view) {
        if (view == null) return;
        if (!isTransparent(view.getBackground())) view.setBackground(new ColorDrawable(Color.TRANSPARENT));
    }

    private static boolean isTransparent(Drawable drawable) {
        if (drawable == null) return true;
        return drawable instanceof ColorDrawable && ((ColorDrawable) drawable).getColor() == Color.TRANSPARENT;
    }

    private static Activity activityOf(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * Bounds that fill [width] x [height]: a 9-patch or a drawable with no size is stretched, and
     * any other image is scaled to fill the height and width, then cropped at the center.
     */
    private static Rect cropBounds(Drawable drawable, int width, int height) {
        if (drawable instanceof NinePatchDrawable) return new Rect(0, 0, width, height);
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) return new Rect(0, 0, width, height);
        float scale = Math.max(width / (float) intrinsicWidth, height / (float) intrinsicHeight);
        int drawnWidth = Math.round(intrinsicWidth * scale);
        int drawnHeight = Math.round(intrinsicHeight * scale);
        int left = (width - drawnWidth) / 2;
        int top = (height - drawnHeight) / 2;
        return new Rect(left, top, left + drawnWidth, top + drawnHeight);
    }

    static boolean enabled() {
        return LineSettings.blurSystemBars();
    }

    static boolean powerSave() {
        return PowerSave.isOn();
    }

    /** System dark mode, used live (no restart) for the four-state chat frost. */
    static boolean night(View view) {
        if (view == null) return false;
        return (view.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static void watchConfiguration(Application application) {
        application.registerComponentCallbacks(new ComponentCallbacks() {
            @Override
            public void onConfigurationChanged(Configuration configuration) {
                for (ChatScreen chat : new ArrayList<>(CHATS.values())) {
                    if (chat != null) chat.redraw();
                }
            }

            @Override
            public void onLowMemory() {}
        });
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
        final ProgressiveBlurDrawable topFade;
        final ProgressiveBlurDrawable inputBlur;
        final ProgressiveBlurDrawable spacerBlur;

        /** How far the list was moved up, and down. */
        int extendedTop;
        int extendedBottom;
        /** Padding added so scroll-to-bottom lands above the input, not only the extension. */
        int paddedTop;
        int paddedBottom;
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
            topFade = new ProgressiveBlurDrawable(content, top, true);
            inputBlur = input == null ? null : new ProgressiveBlurDrawable(input, bottom);
            spacerBlur = spacer == null ? null : new ProgressiveBlurDrawable(spacer, bottom);
        }

        void update() {
            Context context = header.getContext();
            for (String name : CHAT_HEADER_LAYERS) {
                int id = id(context, name);
                if (id != 0) hide(header.findViewById(id));
            }
            if (skin != null) prepareChatSkin(skin);
            applyBackgrounds();
            extendList();
        }

        void redraw() {
            top.invalidate();
            bottom.invalidate();
        }

        private void applyBackgrounds() {
            if (header.getBackground() != headerBlur) header.setBackground(headerBlur);
            layoutTopFade();
            if (input != null) {
                if (input.getBackground() != inputBlur) input.setBackground(inputBlur);
                // The row of the input box. The box and the buttons in it stay.
                View row = find(input, CHAT_INPUT_ROW);
                if (row != null && row.getBackground() != null) row.setBackground(null);
                smoothInputPill(input);
                tintInputIcons(input);
            }
            if (spacer != null) {
                if (spacer.getVisibility() == View.INVISIBLE) spacer.setVisibility(View.VISIBLE);
                if (spacer.getBackground() != spacerBlur) spacer.setBackground(spacerBlur);
            }
            tintHeader(header);
            ChatPanels.apply(decor, skin, content);
        }

        /** The part of the 145 dp top frost that continues into the messages, below the header. */
        private void layoutTopFade() {
            int fade = Math.max(0, dp(header.getContext(), ProgressiveBlurDrawable.TOP_FADE_DP) - header.getHeight());
            if (fade <= 0 || content.getWidth() <= 0) {
                content.getOverlay().remove(topFade);
                return;
            }
            topFade.setBounds(0, 0, content.getWidth(), fade);
            // ViewOverlay.add is a no-op when the drawable is already there.
            content.getOverlay().add(topFade);
        }

        void onPreDraw() {
            if (skin != null) prepareChatSkin(skin);
            if (header != null) unclipAncestors(header);
            if (input != null) unclipAncestors(input);
            if (spacer != null) unclipAncestors(spacer);
            applyBackgrounds();
            extendList();
            boolean skinDirty = skin != null && skin.isDirty();
            if (skinDirty || content.isDirty()) top.invalidate();
            if (skinDirty || (refresh != null && refresh.isDirty())) bottom.invalidate();
            lightIcons();
        }

        /**
         * Dark status and navigation icons only in day + Battery Saver (white frost, dark handle).
         * The other three states use a dark dim, so the icons and the gesture handle stay light.
         */
        private void lightIcons() {
            boolean light = !night(header) && powerSave();
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller == null) return;
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                int value = light ? mask : 0;
                if ((controller.getSystemBarsAppearance() & mask) != value) {
                    controller.setSystemBarsAppearance(value, mask);
                }
            } else {
                int flags = decor.getSystemUiVisibility();
                int mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
                int next = light ? (flags | mask) : (flags & ~mask);
                if (next != flags) decor.setSystemUiVisibility(next);
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
            // Extend the list behind the input, an open sticker/emoji/"+" panel or IME
            // placeholder, and the gesture bar, so messages and the wallpaper can still
            // scroll there. Padding, not the margin, is what LINE's scroll-to-bottom uses.
            int coverBottom = ChatPanels.coverBottom(input, spacer, decor);
            int wantedBottom = coverBottom > listBottom ? coverBottom - listBottom : 0;

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) refresh.getLayoutParams();
            int baseMarginTop = params.topMargin == setMarginTop ? params.topMargin + extendedTop : params.topMargin;
            int baseMarginBottom = params.bottomMargin == setMarginBottom
                    ? params.bottomMargin + extendedBottom : params.bottomMargin;
            int basePaddingTop = list.getPaddingTop() == setPaddingTop
                    ? setPaddingTop - paddedTop : list.getPaddingTop();
            int basePaddingBottom = list.getPaddingBottom() == setPaddingBottom
                    ? setPaddingBottom - paddedBottom : list.getPaddingBottom();
            int marginTop = baseMarginTop - wantedTop;
            int marginBottom = baseMarginBottom - wantedBottom;
            // After the list is extended, padding is the overlap with the header and with
            // everything from the input top down (input bar + IME/panel + gesture bar).
            // clipToPadding stays false, so messages still draw under those bars as they scroll.
            int paddingTop = basePaddingTop + wantedTop;
            int chromeTop = ChatPanels.chromeTop(input, decor);
            int listBottomAfter = listBottom + wantedBottom;
            int overlapBottom = chromeTop > 0 && listBottomAfter > chromeTop
                    ? listBottomAfter - chromeTop : wantedBottom;
            int paddingBottom = basePaddingBottom + overlapBottom;
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
            paddedTop = wantedTop;
            paddedBottom = overlapBottom;
            if (wantedTop > 0 || wantedBottom > 0 || overlapBottom > 0) {
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

    /** Lets a bar draw its saver fade a little past its own bounds, into the messages. */
    private static void unclipAncestors(View view) {
        View current = view;
        View root = view.getRootView();
        while (current != null) {
            unclip(current);
            if (current == root || !(current.getParent() instanceof View)) break;
            current = (View) current.getParent();
        }
    }

    /**
     * Clips the input box to a stadium (radius = height / 2, updated on layout) and paints the
     * measured #F5F5F5 fill with no border.
     */
    private static void smoothInputPill(View input) {
        View container = find(input, CHAT_INPUT_PILL);
        View background = find(input, CHAT_INPUT_PILL_BG);
        if (container != null) roundClip(container);
        if (background != null) {
            roundClip(background);
            fillPill(background);
        }
    }

    private static void roundClip(View view) {
        if (!(view.getOutlineProvider() instanceof PillOutline)) {
            view.setOutlineProvider(new PillOutline());
        }
        if (!view.getClipToOutline()) view.setClipToOutline(true);
    }

    private static void fillPill(View view) {
        // Hide LINE's 9-patch stroke if this is the overwrapping image view.
        try {
            view.getClass().getMethod("setOverwrappingDrawable", Drawable.class)
                    .invoke(view, new Object[] {null});
        } catch (Throwable ignored) {}
        if (view instanceof ImageView) {
            ImageView image = (ImageView) view;
            ColorStateList tint = ColorStateList.valueOf(INPUT_PILL_FILL);
            if (image.getImageTintList() == null || image.getImageTintList().getDefaultColor() != INPUT_PILL_FILL) {
                image.setImageTintList(tint);
                image.setImageTintMode(PorterDuff.Mode.SRC_IN);
            }
        }
        Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            ((GradientDrawable) background).setColor(INPUT_PILL_FILL);
        }
    }

    private static void tintInputIcons(View input) {
        boolean night = night(input);
        int dark = night ? ICON_NIGHT : ICON_DAY;
        int emoji = night ? ICON_NIGHT : ICON_EMOJI_DAY;
        for (String name : INPUT_DARK_ICONS) tintImage(find(input, name), dark);
        for (String name : INPUT_EMOJI_ICONS) tintImage(find(input, name), emoji);
        // The send button shares this ImageView with the mic. Tint a monochrome mic glyph, but
        // leave the green circle and the coloured send arrow alone.
        tintMic(find(input, INPUT_MIC), dark);
    }

    private static void tintMic(View view, int color) {
        if (!(view instanceof ImageView)) return;
        ImageView image = (ImageView) view;
        Drawable drawable = image.getDrawable();
        if (drawable == null || hasGreenCircle(drawable)) {
            // The green disc is in the src, or there is no glyph yet: do not tint the view.
            if (image.getImageTintList() != null) image.setImageTintList(null);
            if (image.getColorFilter() != null) image.clearColorFilter();
            return;
        }
        // A background disc is left as it is; only the src glyph is tinted.
        tintImage(image, color);
    }

    /** True when the drawable (or one of its layers) has a green fill, like the mic disc. */
    private static boolean hasGreenCircle(Drawable drawable) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layer = (LayerDrawable) drawable;
            for (int i = 0; i < layer.getNumberOfLayers(); i++) {
                if (hasGreenCircle(layer.getDrawable(i))) return true;
            }
            return false;
        }
        if (drawable instanceof ColorDrawable) {
            int color = ((ColorDrawable) drawable).getColor();
            int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
            return g > r + 20 && g > b + 20 && g > 80;
        }
        return false;
    }

    private static void tintHeader(View header) {
        boolean light = night(header) || !powerSave();
        int color = light ? HEADER_ICON_LIGHT : HEADER_ICON_DAY_SAVER;
        for (String name : HEADER_TEXT) {
            View view = find(header, name);
            if (view instanceof TextView) ((TextView) view).setTextColor(color);
        }
        for (String name : HEADER_ICONS) tintImage(find(header, name), color);
        // Action buttons on the right of the title. Skip the avatar.
        tintHeaderImages(header, color, id(header.getContext(), HEADER_AVATAR));
    }

    private static void tintHeaderImages(View view, int color, int skipId) {
        if (view == null) return;
        if (skipId != 0 && view.getId() == skipId) return;
        if (view instanceof ImageView) tintImage(view, color);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintHeaderImages(group.getChildAt(i), color, skipId);
            }
        }
    }

    private static void tintImage(View view, int color) {
        if (!(view instanceof ImageView)) return;
        ImageView image = (ImageView) view;
        ColorStateList tint = ColorStateList.valueOf(color);
        image.setImageTintList(tint);
        image.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private static final class PillOutline extends ViewOutlineProvider {
        @Override
        public void getOutline(View view, Outline outline) {
            int width = view.getWidth();
            int height = view.getHeight();
            if (width <= 0 || height <= 0) {
                outline.setEmpty();
                return;
            }
            float radius = Math.min(width, height) / 2f;
            outline.setRoundRect(0, 0, width, height, radius);
        }
    }
}
