package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class BlurBars {
    private static final int BANNER_GAP_DP = 4;
    private static final String CHAT_CONTENT = "chat_ui_main_content_container";
    private static final String CHAT_HEADER = "chat_ui_main_header";
    private static final String CHAT_INPUT = "chat_ui_message";
    private static final String CHAT_INPUT_PILL = "chat_ui_message_container";
    private static final String CHAT_INPUT_PILL_BG = "chat_ui_message_edit_text_background";
    private static final String CHAT_INPUT_ROW = "chat_ui_input_bar_container";
    private static final String CHAT_LIST = "chathistory_message_list";
    private static final String CHAT_REFRESH = "chat_ui_message_list_refresh_layout";
    private static final String CHAT_SKIN = "chat_ui_main_skin_view";
    private static final String CHAT_SPACER = "chat_ui_bottom_padding_spacer";
    private static final String HEADER_AVATAR = "header_title_left_image";
    private static final int HEADER_ICON_DARK = -15658735;
    private static final int HEADER_ICON_LIGHT = -65794;
    private static final int ICON_DAY = -15658735;
    private static final int ICON_EMOJI_DAY = -8684677;
    private static final int ICON_NIGHT = -1;
    private static final String INPUT_EDIT = "chat_ui_message_edit";
    private static final int INPUT_HINT_DARK = -1711276033;
    private static final int INPUT_HINT_DAY = -7697782;
    private static final String INPUT_MIC = "chat_ui_send_button_image";
    private static final int INPUT_PILL_FILL = -657931;
    private static final int INPUT_PILL_FILL_DARK = 0x33FFFFFF;
    private static final int INPUT_TEXT_DARK = -1;
    private static final int INPUT_TEXT_DAY = -15658735;
    private static final String NAVIGATION_BAR = "main_navigation_bar_background";
    private static final String STATUS_BAR = "status_bar_background";
    private static final String TABS_ARTWORK = "bnb_background_image";
    private static boolean registered;
    private static final String[] HIDE_VIEWS = {"edge_to_edge_top"};
    private static final String HEADER_SEAMLESS = "seamless_header_background";
    private static final String HEADER_ARTWORK = "fallback_header_background";
    private static final String[] CHAT_HEADER_LAYERS = {HEADER_SEAMLESS, HEADER_ARTWORK, "default_color_animation_layer"};
    private static final String[] INPUT_DARK_ICONS = {"chathistory_attach_button", "chat_ui_camera_button", "chat_ui_gallery_button"};
    private static final String[] INPUT_EMOJI_ICONS = {"chat_ui_esk_button", "chat_ui_text_keyboard_button"};
    private static final String[] HEADER_TEXT = {"header_title", "header_title_count", "header_oa_chat_status_bar"};
    private static final String[] HEADER_ICONS = {"header_up_button"};
    private static final Map<String, Integer> IDS = new ConcurrentHashMap<>();
    private static final Map<View, Boolean> HIDDEN = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, MirroredArtworkDrawable> MIRRORED = new WeakHashMap<>();
    private static final Map<View, Boolean> WATCHED = new WeakHashMap<>();
    private static final Map<View, ViewTreeObserver.OnGlobalLayoutListener> LAYOUT_LISTENERS = new WeakHashMap<>();
    private static final Map<View, ViewTreeObserver.OnPreDrawListener> PRE_DRAW_LISTENERS = new WeakHashMap<>();
    private static final Map<View, Boolean> PENDING = new WeakHashMap<>();
    private static final Map<View, Boolean> SKIN_PREPARE = new WeakHashMap<>();
    private static final Map<View, ChatScreen> CHATS = new WeakHashMap<>();
    private static final Map<View, Boolean> STROKE_CLEARED = new WeakHashMap<>();
    private static final Map<View, Integer> TINTED = new WeakHashMap<>();

    private BlurBars() {
    }

    public static void init(Application application) {
        if (registered || application == null) {
            return;
        }
        registered = true;
        MaterialYou.init(application);
        watchPowerSave(application);
        watchConfiguration(application);
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() { // from class: app.andrewliang.extension.BlurBars.1
            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityDestroyed(Activity activity) {
                BlurBars.unwatch(activity);
            }

            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityPaused(Activity activity) {
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

            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityCreated(Activity activity, Bundle bundle) {
                BlurBars.watch(activity);
            }

            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityResumed(Activity activity) {
                BlurBars.watch(activity);
            }
        });
    }

    public static void setStatusBarColor(Window window, int i) {
        if (enabled()) {
            i = 0;
        }
        window.setStatusBarColor(i);
    }

    public static void setNavigationBarColor(Window window, int i) {
        if (enabled() || LineSettings.navBarPill()) {
            i = 0;
        }
        window.setNavigationBarColor(i);
    }

    public static long composeStatusBarColor(long j) {
        if (enabled()) {
            return 0L;
        }
        return j;
    }

    public static Object composeOuterModifier(Object obj, Object obj2) {
        return enabled() ? obj : obj2;
    }

    public static Object composeInnerModifier(Object obj, Object obj2) {
        return enabled() ? obj2 : obj;
    }

    public static Object composeArtworkScale(Object obj, Object obj2) {
        return enabled() ? obj2 : obj;
    }

    public static int getComposeColor(Resources resources, int resId, Resources.Theme theme) {
        return MaterialYou.resolveComposeColor(resources, resId, theme);
    }

    public static int overrideComposeThemeColor(Set<?> keys, int slot) {
        return MaterialYou.overrideComposeThemeColor(keys, slot);
    }

    public static long argbToComposeColor(int argb) {
        return MaterialYou.argbToComposeColor(argb);
    }

    public static void layoutChatSkin(final ImageView imageView) {
        if (imageView == null) {
            return;
        }
        disableForceDarkIfLight(imageView);
        if (imageView.getAlpha() < 1.0f) {
            imageView.setAlpha(1.0f);
        }
        if (!enabled()) {
            return;
        }
        if (needsPrepare(imageView) && SKIN_PREPARE.put(imageView, Boolean.TRUE) == null) {
            imageView.post(new Runnable() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    BlurBars.lambda$layoutChatSkin$0(imageView);
                }
            });
        }
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        Drawable drawable = imageView.getDrawable() != null ? imageView.getDrawable() : imageView.getBackground();
        if (drawable == null) {
            return;
        }
        drawable.setBounds(cropBounds(drawable, width, height));
    }

    static /* synthetic */ void lambda$layoutChatSkin$0(ImageView imageView) {
        SKIN_PREPARE.remove(imageView);
        prepareChatSkin(imageView);
    }

    public static Size chatSkinArea(ImageView imageView) {
        if (!enabled() || imageView == null) {
            return null;
        }
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new Size(width, height);
    }

    public static boolean prepareChatSkin(View view) {
        if (view == null) {
            return false;
        }
        disableForceDarkChainIfLight(view);
        if (!enabled()) {
            return false;
        }
        clearWindowBackground(view);
        View rootView = view.getRootView();
        while (view != null) {
            if (view != rootView) {
                fillWindow(view);
            }
            unclip(view);
            Object parent = view.getParent();
            if (!(parent instanceof View) || view == rootView) {
                return true;
            }
            view = (View) parent;
        }
        return true;
    }

    static void disableForceDarkIfLight(View view) {
        if (view != null && Build.VERSION.SDK_INT >= 29 && !night(view)) {
            try {
                if (view.isForceDarkAllowed()) {
                    view.setForceDarkAllowed(false);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    static void disableForceDarkChainIfLight(View view) {
        if (view == null || Build.VERSION.SDK_INT < 29 || night(view)) {
            return;
        }
        View rootView = view.getRootView();
        View cur = view;
        while (cur != null) {
            disableForceDarkIfLight(cur);
            Object parent = cur.getParent();
            if (!(parent instanceof View) || cur == rootView) {
                break;
            }
            cur = (View) parent;
        }
    }

    public static int adjustChatScrollOffset(View view, int position, int offset) {
        if (!enabled() || !(view instanceof ViewGroup)) {
            return offset;
        }
        ViewGroup vg = (ViewGroup) view;
        int paddingTop = vg.getPaddingTop();
        int paddingBottom = vg.getPaddingBottom();
        int height = vg.getHeight();
        if ((paddingTop == 0 && paddingBottom == 0) || height <= 0) {
            return offset;
        }
        if (offset == 0 || offset <= -height * 2) {
            return offset;
        }
        int childCount = vg.getChildCount();
        if (childCount == 0) {
            return offset;
        }
        int visibleTop = paddingTop;
        int visibleBottom = height - paddingBottom;
        View lastChild = vg.getChildAt(childCount - 1);
        if (lastChild != null && offset == lastChild.getTop()) {
            View anchor = lastChild;
            for (int i = childCount - 1; i >= 0; i--) {
                View c = vg.getChildAt(i);
                if (c != null && c.getTop() < visibleBottom && c.getBottom() > visibleTop) {
                    anchor = c;
                    break;
                }
            }
            if (!vg.canScrollVertically(1)
                && anchor == lastChild
                && anchor.getTop() >= visibleTop
                && anchor.getHeight() <= (visibleBottom - visibleTop)) {
                return -height * 3;
            }
            return anchor.getTop() - paddingTop;
        }
        View firstChild = vg.getChildAt(0);
        if (firstChild != null && offset == firstChild.getTop()) {
            View anchor = firstChild;
            for (int i = 0; i < childCount; i++) {
                View c = vg.getChildAt(i);
                if (c != null && c.getTop() < visibleBottom && c.getBottom() > visibleTop) {
                    anchor = c;
                    break;
                }
            }
            return anchor.getTop() - paddingTop;
        }
        return offset;
    }

    private static boolean needsPrepare(View view) {
        if (view.getFitsSystemWindows()) {
            return true;
        }
        if (hasPadding(view) && !imePadding(view)) {
            return true;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            if (marginLayoutParams.topMargin != 0 || marginLayoutParams.bottomMargin != 0 || marginLayoutParams.leftMargin != 0 || marginLayoutParams.rightMargin != 0) {
                return true;
            }
        }
        if (layoutParams != null) {
            return (layoutParams.width == -1 && layoutParams.height == -1) ? false : true;
        }
        return false;
    }

    private static void fillWindow(View view) {
        boolean z;
        if (view.getFitsSystemWindows()) {
            view.setFitsSystemWindows(false);
        }
        if (hasPadding(view) && !imePadding(view)) {
            view.setPadding(0, 0, 0, 0);
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        boolean z2 = true;
        if (layoutParams.width != -1) {
            layoutParams.width = -1;
            z = true;
        } else {
            z = false;
        }
        if (layoutParams.height != -1) {
            layoutParams.height = -1;
            z = true;
        }
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            if (marginLayoutParams.topMargin == 0 && marginLayoutParams.bottomMargin == 0 && marginLayoutParams.leftMargin == 0 && marginLayoutParams.rightMargin == 0) {
                z2 = z;
            } else {
                marginLayoutParams.topMargin = 0;
                marginLayoutParams.bottomMargin = 0;
                marginLayoutParams.leftMargin = 0;
                marginLayoutParams.rightMargin = 0;
            }
        } else {
            z2 = z;
        }
        if (z2) {
            view.setLayoutParams(layoutParams);
        }
    }

    static void unclip(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            if (viewGroup.getClipChildren()) {
                viewGroup.setClipChildren(false);
            }
            if (viewGroup.getClipToPadding()) {
                viewGroup.setClipToPadding(false);
            }
        }
        if (view.getClipToOutline()) {
            view.setClipToOutline(false);
        }
    }

    private static boolean hasPadding(View view) {
        return (view.getPaddingLeft() == 0 && view.getPaddingTop() == 0 && view.getPaddingRight() == 0 && view.getPaddingBottom() == 0) ? false : true;
    }

    private static boolean imePadding(View view) {
        WindowInsets rootWindowInsets;
        return view.getPaddingBottom() != 0 && Build.VERSION.SDK_INT >= 30 && (rootWindowInsets = view.getRootWindowInsets()) != null && rootWindowInsets.isVisible(WindowInsets.Type.ime()) && view.getPaddingBottom() >= rootWindowInsets.getInsets(WindowInsets.Type.ime()).bottom / 2;
    }

    private static void clearWindowBackground(View view) {
        Activity activityActivityOf;
        Window window;
        if (!hasCoveringArtwork(view) || (activityActivityOf = activityOf(view.getContext())) == null || (window = activityActivityOf.getWindow()) == null) {
            return;
        }
        makeTransparent(window.peekDecorView());
        View viewFindViewById = window.findViewById(android.R.id.content);
        if (viewFindViewById != null) {
            makeTransparent(viewFindViewById);
        }
        if (!isTransparent(window.getDecorView().getBackground())) {
            window.setBackgroundDrawable(new ColorDrawable(0));
        }
        if (view.getParent() instanceof View) {
            makeTransparent((View) view.getParent());
        }
    }

    public static boolean hasCoveringArtwork(View view) {
        Drawable drawable = view instanceof ImageView ? ((ImageView) view).getDrawable() : null;
        if (!isArtwork(drawable)) {
            drawable = view.getBackground();
        }
        return isArtwork(drawable);
    }

    private static boolean isArtwork(Drawable drawable) {
        if (drawable == null || (drawable instanceof ColorDrawable)) {
            return false;
        }
        if (!(drawable instanceof LayerDrawable)) {
            return true;
        }
        LayerDrawable layerDrawable = (LayerDrawable) drawable;
        for (int i = 0; i < layerDrawable.getNumberOfLayers(); i++) {
            if (isArtwork(layerDrawable.getDrawable(i))) {
                return true;
            }
        }
        return false;
    }

    private static void makeTransparent(View view) {
        if (view == null || isTransparent(view.getBackground())) {
            return;
        }
        view.setBackground(new ColorDrawable(0));
    }

    private static boolean isTransparent(Drawable drawable) {
        if (drawable == null) {
            return true;
        }
        return (drawable instanceof ColorDrawable) && ((ColorDrawable) drawable).getColor() == 0;
    }

    private static Activity activityOf(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static Rect cropBounds(Drawable drawable, int i, int i2) {
        if (drawable instanceof NinePatchDrawable) {
            return new Rect(0, 0, i, i2);
        }
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            return new Rect(0, 0, i, i2);
        }
        float f = intrinsicWidth;
        float f2 = intrinsicHeight;
        float fMax = Math.max(i / f, i2 / f2);
        int iRound = Math.round(f * fMax);
        int iRound2 = Math.round(f2 * fMax);
        int i3 = (i - iRound) / 2;
        int i4 = (i2 - iRound2) / 2;
        return new Rect(i3, i4, iRound + i3, iRound2 + i4);
    }

    static boolean enabled() {
        return LineSettings.blurSystemBars();
    }

    static boolean powerSave() {
        return PowerSave.shouldDegradeEffects();
    }

    static boolean night(View view) {
        return view != null && (view.getResources().getConfiguration().uiMode & 48) == 32;
    }

    static boolean chromeNight(View view) {
        int iBlurDayNight = LineSettings.blurDayNight();
        if (iBlurDayNight == 1) {
            return false;
        }
        if (iBlurDayNight == 2) {
            return true;
        }
        return night(view);
    }

    private static void watchConfiguration(Application application) {
        application.registerComponentCallbacks(new ComponentCallbacks() { // from class: app.andrewliang.extension.BlurBars.2
            @Override // android.content.ComponentCallbacks
            public void onLowMemory() {
            }

            @Override // android.content.ComponentCallbacks
            public void onConfigurationChanged(Configuration configuration) {
                MaterialYou.onConfigurationChanged();
                for (ChatScreen chatScreen : new ArrayList<>(BlurBars.CHATS.values())) {
                    if (chatScreen != null) {
                        chatScreen.redraw();
                    }
                }
            }
        });
    }

    private static void watchPowerSave(Application application) {
        PowerSave.init(application);
        PowerSave.addListener(new Runnable() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                BlurBars.lambda$watchPowerSave$1();
            }
        });
    }

    static /* synthetic */ void lambda$watchPowerSave$1() {
        for (ChatScreen chatScreen : new ArrayList<>(CHATS.values())) {
            if (chatScreen != null) {
                chatScreen.redraw();
            }
        }
    }

    static void refreshAllChats() {
        for (ChatScreen chatScreen : new ArrayList<>(CHATS.values())) {
            if (chatScreen != null && chatScreen.decor.isAttachedToWindow()) {
                MaterialYou.applyChatRoom(chatScreen.decor, chatScreen.header, chatScreen.list, chatScreen.skin);
                chatScreen.decor.postInvalidate();
            }
        }
    }

    static void unwatch(Activity activity) {
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View decorView = window.getDecorView();
        WATCHED.remove(decorView);
        PENDING.remove(decorView);
        CHATS.remove(decorView);
        KeyboardMotion.clearOnStop(decorView);
        MaterialYou.onActivityDestroyed(decorView);
        ViewTreeObserver vto = decorView.getViewTreeObserver();
        ViewTreeObserver.OnGlobalLayoutListener layoutListener = LAYOUT_LISTENERS.remove(decorView);
        ViewTreeObserver.OnPreDrawListener preDrawListener = PRE_DRAW_LISTENERS.remove(decorView);
        if (vto != null && vto.isAlive()) {
            if (layoutListener != null) {
                vto.removeOnGlobalLayoutListener(layoutListener);
            }
            if (preDrawListener != null) {
                vto.removeOnPreDrawListener(preDrawListener);
            }
        }
    }

    public static void watch(Activity activity) {
        try {
            if (activity != null && activity.getWindow() != null) {
                View peek = activity.getWindow().peekDecorView();
                MaterialYou.registerActivity(peek != null ? peek : activity.getWindow().getDecorView(), activity);
            }
            PixelNativeEnhancements.applyActivity(activity);
            MaterialYou.attachChatSettingsRow(activity);
            Window window;
            if ((enabled() || MaterialYou.anyEnabled()) && (window = activity.getWindow()) != null) {
                if (enabled() && Build.VERSION.SDK_INT >= 29) {
                    window.setStatusBarContrastEnforced(false);
                    window.setNavigationBarContrastEnforced(false);
                }
                final View decorView = window.getDecorView();
                MaterialYou.registerActivity(decorView, activity);
                Map<View, Boolean> map = WATCHED;
                if (map.containsKey(decorView)) {
                    MaterialYou.onActivityResumed(decorView);
                    final WeakReference<View> decorRef = new WeakReference<>(decorView);
                    decorView.post(new Runnable() {
                        @Override
                        public void run() {
                            View v = decorRef.get();
                            if (v == null) {
                                return;
                            }
                            scan(v);
                            ChatScreen chatScreen = CHATS.get(v);
                            if (chatScreen != null) {
                                chatScreen.redraw();
                            }
                        }
                    });
                    return;
                }
                map.put(decorView, Boolean.TRUE);
                final WeakReference<View> decorRef = new WeakReference<>(decorView);
                final Runnable runnable = new Runnable() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        View v = decorRef.get();
                        if (v != null) {
                            BlurBars.lambda$watch$2(v);
                        }
                    }
                };
                ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
                ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda11
                    @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
                    public final void onGlobalLayout() {
                        View v = decorRef.get();
                        if (v != null) {
                            BlurBars.lambda$watch$3(v, runnable);
                        }
                    }
                };
                LAYOUT_LISTENERS.put(decorView, layoutListener);
                viewTreeObserver.addOnGlobalLayoutListener(layoutListener);
                KeyboardMotion.onStop(decorView, new Runnable() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda12
                    @Override // java.lang.Runnable
                    public final void run() {
                        View v = decorRef.get();
                        if (v != null) {
                            BlurBars.lambda$watch$4(v, runnable);
                        }
                    }
                });
                ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda7
                    @Override // android.view.ViewTreeObserver.OnPreDrawListener
                    public final boolean onPreDraw() {
                        View v = decorRef.get();
                        return v == null || BlurBars.lambda$watch$5(v);
                    }
                };
                PRE_DRAW_LISTENERS.put(decorView, preDrawListener);
                viewTreeObserver.addOnPreDrawListener(preDrawListener);
                decorView.post(runnable);
            }
        } catch (Throwable unused) {
        }
    }

    static /* synthetic */ void lambda$watch$2(View view) {
        PENDING.remove(view);
        scan(view);
    }

    static /* synthetic */ void lambda$watch$3(View view, Runnable runnable) {
        if (!KeyboardMotion.isMoving(view) && PENDING.put(view, Boolean.TRUE) == null) {
            view.post(runnable);
        }
    }

    static /* synthetic */ void lambda$watch$4(View view, Runnable runnable) {
        if (PENDING.put(view, Boolean.TRUE) == null) {
            view.post(runnable);
        }
    }

    static /* synthetic */ boolean lambda$watch$5(View view) {
        try {
            boolean blurOn = enabled();
            ChatScreen chatScreen = blurOn ? CHATS.get(view) : null;
            if (blurOn) {
                keepHidden();
                keepMirrored();
                if (chatScreen != null) {
                    chatScreen.onPreDraw();
                }
            }
            if (MaterialYou.anyEnabled()) {
                if (chatScreen == null) {
                    MaterialYou.applyBottomNavBar(view);
                    MaterialYou.applyChatListBadges(view);
                }
                if (!blurOn) {
                    View header = find(view, CHAT_HEADER);
                    if (header != null && header.isAttachedToWindow()) {
                        View list = find(view, CHAT_LIST);
                        MaterialYou.applyChatRoom(view, header, list, find(view, CHAT_SKIN));
                        PixelNativeEnhancements.applyFoldableChatLayout(view, list, find(view, CHAT_INPUT));
                        if (LineSettings.chatHeaderWhite()) {
                            tintHeader(header, null);
                        }
                    }
                }
            }
        } catch (Throwable unused) {
        }
        return true;
    }

    private static void scan(View view) {
        String[] strArr;
        if (MaterialYou.anyEnabled()) {
            MaterialYou.applyBottomNavBar(view);
            MaterialYou.applyChatListBadges(view);
        }
        if (!enabled()) {
            if (MaterialYou.anyEnabled()) {
                View header = find(view, CHAT_HEADER);
                if (header != null && header.isAttachedToWindow()) {
                    View list = find(view, CHAT_LIST);
                    MaterialYou.applyChatRoom(view, header, list, find(view, CHAT_SKIN));
                    PixelNativeEnhancements.applyFoldableChatLayout(view, list, find(view, CHAT_INPUT));
                    if (LineSettings.chatHeaderWhite()) {
                        tintHeader(header, null);
                    }
                }
            }
            return;
        }
        Context context = view.getContext();
        int iId = id(context, CHAT_HEADER);
        View viewFindViewById = iId == 0 ? null : view.findViewById(iId);
        int[] iArr = new int[HIDE_VIEWS.length + 2];
        int i = 0;
        while (true) {
            strArr = HIDE_VIEWS;
            if (i >= strArr.length) {
                break;
            }
            iArr[i] = id(context, strArr[i]);
            i++;
        }
        int iId2 = id(context, STATUS_BAR);
        int iId3 = id(context, NAVIGATION_BAR);
        iArr[strArr.length] = iId2;
        iArr[strArr.length + 1] = iId3;
        ArrayList<View> arrayList = new ArrayList();
        collect(view, iArr, id(context, CHAT_LIST), arrayList);
        for (View view2 : arrayList) {
            int id = view2.getId();
            if (id == iId2) {
                statusBar(view2, viewFindViewById);
            } else if (id == iId3) {
                if (LineSettings.navBarPill() || LineSettings.materialYouNavBar()) {
                    MIRRORED.remove(view2);
                    HIDDEN.remove(view2);
                } else {
                    View viewFind = find(view, TABS_ARTWORK);
                    if (viewFind != null) {
                        mirror(view2, viewFind, false);
                    } else {
                        hide(view2);
                    }
                }
            } else {
                hide(view2);
            }
        }
        if (viewFindViewById == null || !viewFindViewById.isAttachedToWindow()) {
            CHATS.remove(view);
            return;
        }
        Map<View, ChatScreen> map = CHATS;
        ChatScreen chatScreen = map.get(view);
        if (chatScreen == null || chatScreen.header != viewFindViewById) {
            View viewFind2 = find(view, CHAT_CONTENT);
            if (viewFind2 == null) {
                return;
            }
            ChatScreen chatScreen2 = new ChatScreen(view, viewFindViewById, viewFind2, find(view, CHAT_SKIN), find(view, CHAT_REFRESH), find(view, CHAT_LIST), find(view, CHAT_INPUT), find(view, CHAT_SPACER));
            map.put(view, chatScreen2);
            chatScreen = chatScreen2;
        }
        chatScreen.update();
    }

    private static void statusBar(View view, View view2) {
        if (view2 != null && isInside(view, view2)) {
            hide(view);
            return;
        }
        Object parent = view.getParent();
        if (!(parent instanceof View)) {
            hide(view);
            return;
        }
        View view3 = (View) parent;
        View viewFind = find(view3, HEADER_SEAMLESS);
        View viewFind2 = find(view3, HEADER_ARTWORK);
        boolean z = (viewFind == null || viewFind.getVisibility() != 0 || viewFind.getBackground() == null) ? false : true;
        if (viewFind2 != null && !z && viewFind2.getVisibility() == 0) {
            mirror(view, viewFind2, true);
        } else {
            hide(view);
        }
    }

    static boolean isSelfOrAncestor(View view, View view2) {
        if (view == null || view2 == null) {
            return false;
        }
        if (view == view2) {
            return true;
        }
        return isInside(view2, view);
    }

    private static boolean isInside(View view, View view2) {
        for (ViewParent parent = view.getParent(); parent != null; parent = parent.getParent()) {
            if (parent == view2) {
                return true;
            }
        }
        return false;
    }

    private static void mirror(View view, View view2, boolean z) {
        Map<View, MirroredArtworkDrawable> map = MIRRORED;
        MirroredArtworkDrawable mirroredArtworkDrawable = map.get(view);
        if (mirroredArtworkDrawable == null || mirroredArtworkDrawable.artwork() != view2) {
            mirroredArtworkDrawable = new MirroredArtworkDrawable(view2, view, z);
            map.put(view, mirroredArtworkDrawable);
        }
        HIDDEN.remove(view);
        if (view.getVisibility() == 4) {
            view.setVisibility(0);
        }
        if (view.getBackground() != mirroredArtworkDrawable) {
            view.setBackground(mirroredArtworkDrawable);
        }
    }

    private static void keepMirrored() {
        Map<View, MirroredArtworkDrawable> map = MIRRORED;
        if (map.isEmpty()) {
            return;
        }
        for (Map.Entry<View, MirroredArtworkDrawable> entry : map.entrySet()) {
            View view = entry.getKey();
            MirroredArtworkDrawable mirroredArtworkDrawable = entry.getValue();
            if (view != null && mirroredArtworkDrawable != null && view.isAttachedToWindow()) {
                if (view.getBackground() != mirroredArtworkDrawable) {
                    view.setBackground(mirroredArtworkDrawable);
                }
                if (mirroredArtworkDrawable.artwork().isDirty()) {
                    view.invalidate();
                }
            }
        }
    }

    private static void collect(View view, int[] iArr, int i, List<View> list) {
        int id = view.getId();
        if (id != -1) {
            for (int i2 : iArr) {
                if (i2 != 0 && i2 == id) {
                    list.add(view);
                    break;
                }
            }
            if (id == i) {
                return;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i3 = 0; i3 < viewGroup.getChildCount(); i3++) {
                collect(viewGroup.getChildAt(i3), iArr, i, list);
            }
        }
    }

    static void hide(View view) {
        if (view == null) {
            return;
        }
        HIDDEN.put(view, Boolean.TRUE);
        if (view.getVisibility() == 0) {
            view.setVisibility(4);
        }
    }

    private static void keepHidden() {
        Map<View, Boolean> map = HIDDEN;
        if (map.isEmpty()) {
            return;
        }
        synchronized (map) {
            for (View view : map.keySet()) {
                if (view != null && view.getVisibility() == 0) {
                    view.setVisibility(4);
                }
            }
        }
    }

    public static View find(View view, String str) {
        int iId = id(view.getContext(), str);
        if (iId == 0) {
            return null;
        }
        return view.findViewById(iId);
    }

    static int id(Context context, String str) {
        Map<String, Integer> map = IDS;
        Integer num = map.get(str);
        if (num != null) {
            return num.intValue();
        }
        int identifier = context.getResources().getIdentifier(str, "id", context.getPackageName());
        map.put(str, Integer.valueOf(identifier));
        return identifier;
    }

    static int dp(Context context, float f) {
        return Math.round(f * context.getResources().getDisplayMetrics().density);
    }

    static final class ChatScreen {
        final ProgressiveBlurDrawable.Region bottom;
        final ProgressiveBlurDrawable bottomFade;
        final View content;
        final View decor;
        int extendedBottom;
        int extendedTop;
        final View header;
        final ProgressiveBlurDrawable headerBlur;
        final View input;
        final ProgressiveBlurDrawable inputBlur;
        final View list;
        private final Runnable onSampled;
        int paddedBottom;
        int paddedTop;
        final SkinPalette palette;
        final View refresh;
        int setMarginBottom;
        int setMarginTop;
        int setPaddingBottom;
        int setPaddingTop;
        final View skin;
        final View spacer;
        final ProgressiveBlurDrawable spacerBlur;
        final ChromeStyle style;
        final ProgressiveBlurDrawable.Region top;
        final ProgressiveBlurDrawable topFade;

        ChatScreen(View view, final View view2, View view3, View view4, View view5, View view6, View view7, View view8) {
            ChromeStyle chromeStyle = new ChromeStyle();
            this.style = chromeStyle;
            this.palette = new SkinPalette();
            this.setPaddingTop = -1;
            this.setPaddingBottom = -1;
            this.setMarginTop = Integer.MIN_VALUE;
            this.setMarginBottom = Integer.MIN_VALUE;
            this.decor = view;
            this.header = view2;
            this.content = view3;
            this.skin = view4;
            this.refresh = view5;
            this.list = view6;
            this.input = view7;
            this.spacer = view8;
            ProgressiveBlurDrawable.Region region = new ProgressiveBlurDrawable.Region(false, new View[]{view2}, new View[]{view4, view3});
            this.top = region;
            ProgressiveBlurDrawable.Region region2 = new ProgressiveBlurDrawable.Region(true, new View[]{view7, view8}, new View[]{view4, view5});
            this.bottom = region2;
            region.style = chromeStyle;
            region2.style = chromeStyle;
            this.onSampled = new Runnable() { // from class: app.andrewliang.extension.BlurBars$ChatScreen$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    BlurBars.ChatScreen.lambda$new$0(view2);
                }
            };
            this.headerBlur = new ProgressiveBlurDrawable(view2, region);
            this.topFade = new ProgressiveBlurDrawable(view3, region, true);
            this.bottomFade = new ProgressiveBlurDrawable(view3, region2, true);
            this.inputBlur = view7 == null ? null : new ProgressiveBlurDrawable(view7, region2);
            this.spacerBlur = view8 != null ? new ProgressiveBlurDrawable(view8, region2) : null;
        }

        static /* synthetic */ void lambda$new$0(View view) {
            if (view.isAttachedToWindow()) {
                view.invalidate();
            }
        }

        private final int[] tmpLoc = new int[2];

        void update() {
            Context context = this.header.getContext();
            for (String str : BlurBars.CHAT_HEADER_LAYERS) {
                int iId = BlurBars.id(context, str);
                if (iId != 0) {
                    BlurBars.hide(this.header.findViewById(iId));
                }
            }
            BlurBars.disableForceDarkIfLight(this.decor);
            BlurBars.disableForceDarkIfLight(BlurBars.find(this.decor, "chat_ui_edge_to_edge_container"));
            View view = this.skin;
            if (view != null) {
                BlurBars.prepareChatSkin(view);
            }
            if (this.header != null) {
                BlurBars.unclipAncestors(this.header);
            }
            if (this.input != null) {
                BlurBars.unclipAncestors(this.input);
            }
            if (this.spacer != null) {
                BlurBars.unclipAncestors(this.spacer);
            }
            updateStyle();
            applyBackgrounds();
            extendList();
        }

        private void updateStyle() {
            View view;
            if (LineSettings.blurAutoColor() && (view = this.skin) != null) {
                this.palette.check(view, BlurBars.hasCoveringArtwork(view), this.onSampled);
            }
            if (this.style.update(this.header, this.palette)) {
                redraw();
                applyThemeChrome();
            }
        }

        void redraw() {
            this.top.invalidate();
            this.bottom.invalidate();
            if (this.input != null) {
                BlurBars.smoothInputPill(
                    this.input, this.style, this.decor, this.content, this.skin,
                    this.refresh != null ? this.refresh : this.list
                );
            }
            markInputPillDirty();
        }

        private View pill;
        private View pillHost;
        private View pillBg;
        private int lastPillWinX = Integer.MIN_VALUE;
        private int lastPillWinY = Integer.MIN_VALUE;
        private int lastPillW = -1;
        private int lastPillH = -1;

        private void markInputPillDirty() {
            View h = this.pillHost;
            if (h != null && h.getBackground() instanceof GlassPillDrawable) {
                ((GlassPillDrawable) h.getBackground()).markDirty();
            }
        }

        private void ensureBackgrounds() {
            Drawable background = this.header.getBackground();
            ProgressiveBlurDrawable progressiveBlurDrawable = this.headerBlur;
            if (background != progressiveBlurDrawable) {
                this.header.setBackground(progressiveBlurDrawable);
            }
            layoutTopFade();
            layoutBottomFade();
            View view = this.input;
            if (view != null) {
                Drawable background2 = view.getBackground();
                ProgressiveBlurDrawable progressiveBlurDrawable2 = this.inputBlur;
                if (background2 != progressiveBlurDrawable2) {
                    this.input.setBackground(progressiveBlurDrawable2);
                }
                View pillContainer = this.pill;
                if (pillContainer == null || !pillContainer.isAttachedToWindow()) {
                    pillContainer = BlurBars.find(this.input, BlurBars.CHAT_INPUT_PILL);
                    this.pill = pillContainer;
                }
                View hostView = BlurBars.resolvePillHost(this.input, pillContainer);
                this.pillHost = hostView;

                View inputRow = BlurBars.find(this.input, BlurBars.CHAT_INPUT_ROW);
                if (inputRow != null && inputRow != hostView && inputRow.getBackground() != null) {
                    inputRow.setBackground(null);
                }
                if (pillContainer != null && pillContainer != hostView && pillContainer.getBackground() != null) {
                    pillContainer.setBackground(null);
                }

                View pillBgView = this.pillBg;
                if (pillBgView == null || !pillBgView.isAttachedToWindow()) {
                    pillBgView = BlurBars.find(this.input, BlurBars.CHAT_INPUT_PILL_BG);
                    this.pillBg = pillBgView;
                }
                View msgSource = this.refresh != null ? this.refresh : this.list;
                boolean needsPill = pillContainer != null
                    && (hostView == null
                        || !(hostView.getBackground() instanceof GlassPillDrawable)
                        || (Build.VERSION.SDK_INT >= 23
                            && (!(pillContainer.getForeground() instanceof GlassRimDrawable)
                                || pillContainer.getForegroundGravity() != Gravity.FILL)));
                boolean needsClearBg = pillBgView != null
                    && (pillBgView.getBackground() != null
                        || (pillBgView instanceof ImageView && ((ImageView) pillBgView).getDrawable() != null));
                if (needsPill || needsClearBg) {
                    BlurBars.smoothInputPill(this.input, this.style, this.decor, this.content, this.skin, msgSource);
                } else if (hostView != null && hostView.getBackground() instanceof GlassPillDrawable) {
                    ((GlassPillDrawable) hostView.getBackground()).bindSources(
                        pillContainer, this.decor, this.content, this.skin, msgSource
                    );
                }
            }
            View view2 = this.spacer;
            if (view2 != null) {
                if (view2.getVisibility() == 4) {
                    this.spacer.setVisibility(0);
                }
                Drawable background3 = this.spacer.getBackground();
                ProgressiveBlurDrawable progressiveBlurDrawable3 = this.spacerBlur;
                if (background3 != progressiveBlurDrawable3) {
                    this.spacer.setBackground(progressiveBlurDrawable3);
                }
            }
        }

        private void applyThemeChrome() {
            if (this.input != null) {
                BlurBars.smoothInputPill(
                    this.input, this.style, this.decor, this.content, this.skin,
                    this.refresh != null ? this.refresh : this.list
                );
                BlurBars.tintInputIcons(this.input, this.style);
            }
            MaterialYou.applyChatRoom(this.decor, this.header, this.list, this.skin);
            BlurBars.tintHeader(this.header, this.style);
            ChatPanels.apply(this.decor, this.skin, this.refresh);
        }

        private void applyBackgrounds() {
            ensureBackgrounds();
            applyThemeChrome();
        }

        private void layoutTopFade() {
            int iMax = Math.max(0, BlurBars.dp(this.header.getContext(), 145.0f) - this.header.getHeight());
            if (iMax <= 0 || this.content.getWidth() <= 0) {
                this.content.getOverlay().remove(this.topFade);
            } else {
                setOverlayBounds(this.topFade, 0, 0, this.content.getWidth(), iMax);
                this.content.getOverlay().add(this.topFade);
            }
        }

        private void layoutBottomFade() {
            int iDp = BlurBars.dp(this.content.getContext(), 28.0f);
            View view = this.input;
            if (view == null || !view.isShown() || this.input.getHeight() == 0 || this.content.getWidth() <= 0 || iDp <= 0) {
                this.content.getOverlay().remove(this.bottomFade);
                return;
            }
            int[] iArr = this.tmpLoc;
            this.input.getLocationInWindow(iArr);
            int i = iArr[1];
            this.content.getLocationInWindow(iArr);
            int i2 = i - iArr[1];
            setOverlayBounds(this.bottomFade, 0, i2 - iDp, this.content.getWidth(), i2);
            this.content.getOverlay().add(this.bottomFade);
        }

        private void setOverlayBounds(ProgressiveBlurDrawable progressiveBlurDrawable, int i, int i2, int i3, int i4) {
            Rect bounds = progressiveBlurDrawable.getBounds();
            if (bounds.left == i && bounds.top == i2 && bounds.right == i3 && bounds.bottom == i4) {
                return;
            }
            progressiveBlurDrawable.setBounds(i, i2, i3, i4);
            progressiveBlurDrawable.invalidateSelf();
        }

        void onPreDraw() {
            View view;
            updateStyle();
            ensureBackgrounds();
            extendList();
            MaterialYou.applyChatRoom(this.decor, this.header, this.list, this.skin);
            PixelNativeEnhancements.applyFoldableChatLayout(this.decor, this.list, this.input);
            boolean pillMoved = false;
            if (this.pill != null) {
                BlurBars.centerEmojiButton(this.pill);
                this.pill.getLocationInWindow(this.tmpLoc);
                int px = this.tmpLoc[0];
                int py = this.tmpLoc[1];
                int pw = this.pill.getWidth();
                int ph = this.pill.getHeight();
                if (px != this.lastPillWinX || py != this.lastPillWinY || pw != this.lastPillW || ph != this.lastPillH) {
                    this.lastPillWinX = px;
                    this.lastPillWinY = py;
                    this.lastPillW = pw;
                    this.lastPillH = ph;
                    pillMoved = true;
                    markInputPillDirty();
                }
            }
            BlurBars.updateSendButtonTint(this.input, this.style);
            if (LineSettings.chatHeaderWhite()) {
                BlurBars.tintHeader(this.header, this.style);
            }
            if (!BlurBars.powerSave()) {
                View view6 = this.skin;
                boolean z = view6 != null && view6.isDirty();
                if (z || this.content.isDirty()) {
                    this.top.invalidate();
                }
                boolean refreshDirty = (view = this.refresh) != null && view.isDirty();
                boolean listDirty = this.list != null && this.list.isDirty();
                if (z || refreshDirty) {
                    this.bottom.invalidate();
                }
                if (!pillMoved && (z || refreshDirty || listDirty || KeyboardMotion.isMoving(this.decor))) {
                    markInputPillDirty();
                }
            }
            lightIcons();
        }

        private void lightIcons() {
            boolean z = !LineSettings.chatHeaderWhite() && this.style.topDarkIcons;
            boolean z2 = this.style.bottomDarkIcons;
            int i = Build.VERSION.SDK_INT;
            View view = this.decor;
            if (i >= 30) {
                WindowInsetsController windowInsetsController = view.getWindowInsetsController();
                if (windowInsetsController == null) {
                    return;
                }
                int i2 = (z ? 8 : 0) | (z2 ? 16 : 0);
                if ((windowInsetsController.getSystemBarsAppearance() & 24) != i2) {
                    windowInsetsController.setSystemBarsAppearance(i2, 24);
                    return;
                }
                return;
            }
            int systemUiVisibility = view.getSystemUiVisibility();
            int i3 = z ? systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : systemUiVisibility & (~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            int i4 = z2 ? i3 | 16 : i3 & (-17);
            if (i4 != systemUiVisibility) {
                this.decor.setSystemUiVisibility(i4);
            }
        }

        private void extendList() {
            boolean z;
            View view = this.refresh;
            if (view == null || this.list == null || !view.isAttachedToWindow() || !(this.refresh.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) || this.header.getHeight() == 0 || this.refresh.getHeight() == 0 || this.header.isLayoutRequested() || this.refresh.isLayoutRequested()) {
                return;
            }
            int[] iArr = this.tmpLoc;
            this.header.getLocationInWindow(iArr);
            boolean z2 = true;
            int i = iArr[1];
            int height = this.header.getHeight() + i;
            this.refresh.getLocationInWindow(iArr);
            int i2 = iArr[1];
            int i3 = this.extendedTop + i2;
            int height2 = (i2 + this.refresh.getHeight()) - this.extendedBottom;
            int i4 = (i3 > i && i3 - height <= BlurBars.dp(this.header.getContext(), 4.0f)) ? i3 - i : 0;
            int iCoverBottom = ChatPanels.coverBottom(this.input, this.spacer, this.decor);
            int i5 = iCoverBottom > height2 ? iCoverBottom - height2 : 0;
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.refresh.getLayoutParams();
            int i6 = marginLayoutParams.topMargin == this.setMarginTop ? marginLayoutParams.topMargin + this.extendedTop : marginLayoutParams.topMargin;
            int i7 = marginLayoutParams.bottomMargin == this.setMarginBottom ? marginLayoutParams.bottomMargin + this.extendedBottom : marginLayoutParams.bottomMargin;
            int paddingTop = this.list.getPaddingTop();
            int i8 = this.setPaddingTop;
            int paddingTop2 = paddingTop == i8 ? i8 - this.paddedTop : this.list.getPaddingTop();
            int paddingBottom = this.list.getPaddingBottom();
            int i9 = this.setPaddingBottom;
            int paddingBottom2 = paddingBottom == i9 ? i9 - this.paddedBottom : this.list.getPaddingBottom();
            int i10 = i6 - i4;
            int i11 = i7 - i5;
            int i12 = paddingTop2 + i4;
            int iChromeTop = ChatPanels.chromeTop(this.input, this.decor);
            int i13 = height2 + i5;
            int i14 = (iChromeTop <= 0 || i13 <= iChromeTop) ? i5 : i13 - iChromeTop;
            int i15 = paddingBottom2 + i14;
            if (marginLayoutParams.topMargin == i10 && marginLayoutParams.bottomMargin == i11) {
                z = false;
            } else {
                marginLayoutParams.topMargin = i10;
                marginLayoutParams.bottomMargin = i11;
                this.refresh.setLayoutParams(marginLayoutParams);
                z = true;
            }
            if (this.list.getPaddingTop() == i12 && this.list.getPaddingBottom() == i15) {
                z2 = z;
            } else {
                boolean wasAtBottom = (this.list instanceof ViewGroup)
                    && ((ViewGroup) this.list).getChildCount() > 0
                    && !this.list.canScrollVertically(1);
                int oldPaddingBottom = this.list.getPaddingBottom();
                View view2 = this.list;
                view2.setPadding(view2.getPaddingLeft(), i12, this.list.getPaddingRight(), i15);
                if (wasAtBottom && i15 > oldPaddingBottom && this.list.canScrollVertically(1)) {
                    this.list.scrollBy(0, i15 - oldPaddingBottom);
                }
            }
            this.setMarginTop = i10;
            this.setMarginBottom = i11;
            this.setPaddingTop = i12;
            this.setPaddingBottom = i15;
            this.extendedTop = i4;
            this.extendedBottom = i5;
            this.paddedTop = i4;
            this.paddedBottom = i14;
            if (z2 && (i4 > 0 || i5 > 0 || i14 > 0)) {
                View view3 = this.list;
                if (view3 instanceof ViewGroup) {
                    ((ViewGroup) view3).setClipToPadding(false);
                }
                View view4 = this.spacer;
                ViewParent stopParent = ((view4 == null || !(view4.getParent() instanceof View)) ? this.header : this.spacer).getParent();
                View view5 = stopParent instanceof View ? (View) stopParent : null;
                ViewParent parent = this.refresh.getParent();
                while (parent instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) parent;
                    viewGroup.setClipChildren(false);
                    viewGroup.setClipToPadding(false);
                    if (viewGroup == view5) {
                        break;
                    }
                    parent = viewGroup.getParent();
                }
            }
            if (z2) {
                redraw();
            }
        }
    }

    public static void unclipAncestors(View view) {
        View rootView = view.getRootView();
        while (view != null) {
            unclip(view);
            if (view == rootView || !(view.getParent() instanceof View)) {
                return;
            } else {
                view = (View) view.getParent();
            }
        }
    }

    public static void smoothInputPill(View view, ChromeStyle chromeStyle) {
        smoothInputPill(view, chromeStyle, null, null, null, null);
    }

    static View resolvePillHost(View inputRoot, View pill) {
        if (pill == null) {
            return null;
        }
        View inputRow = inputRoot != null ? find(inputRoot, CHAT_INPUT_ROW) : null;
        if (inputRow != null && isSelfOrAncestor(inputRow, pill)) {
            return inputRow;
        }
        ViewParent vp = pill.getParent();
        return vp instanceof View ? (View) vp : pill;
    }

    static void smoothInputPill(View view, ChromeStyle chromeStyle, View decor, View content, View skin, View msgSource) {
        View viewFind = find(view, CHAT_INPUT_PILL);
        View viewFind2 = find(view, CHAT_INPUT_PILL_BG);
        if (viewFind2 != null) {
            clearPillBg(viewFind2);
        }
        if (viewFind != null) {
            View pillHost = resolvePillHost(view, viewFind);
            if (pillHost != viewFind && viewFind.getBackground() != null) {
                viewFind.setBackground(null);
            }
            unclipAncestors(viewFind);
            stylePillContainer(pillHost, viewFind, decor, content, skin, msgSource);
            centerEmojiButton(viewFind);
        }
        View viewFind3 = find(view, INPUT_EDIT);
        if (viewFind3 instanceof TextView) {
            Context ctx = viewFind3.getContext();
            boolean pillDark = night(viewFind3);
            TextView textView = (TextView) viewFind3;
            int i = pillDark ? INPUT_TEXT_DARK : INPUT_TEXT_DAY;
            int i2 = pillDark ? INPUT_HINT_DARK : INPUT_HINT_DAY;
            if (textView.getCurrentTextColor() != i) {
                textView.setTextColor(i);
            }
            if (textView.getCurrentHintTextColor() != i2) {
                textView.setHintTextColor(i2);
            }
            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    Drawable cursor = textView.getTextCursorDrawable();
                    if (cursor != null) {
                        int cursorColor = LineSettings.materialYouTheme()
                            ? MaterialYou.Palette.of(ctx).accent
                            : i;
                        cursor.mutate().setTint(cursorColor);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static final Object NO_OVERWRAPPING_METHOD = new Object();
    private static final Map<Class<?>, Object> OVERWRAPPING_METHODS = new ConcurrentHashMap<>();

    private static void clearPillBg(View view) {
        try {
            Class<?> cls = view.getClass();
            Object cached = OVERWRAPPING_METHODS.get(cls);
            if (cached == null) {
                try {
                    cached = cls.getMethod("setOverwrappingDrawable", Drawable.class);
                } catch (Throwable unused) {
                    cached = NO_OVERWRAPPING_METHOD;
                }
                OVERWRAPPING_METHODS.put(cls, cached);
            }
            if (cached instanceof java.lang.reflect.Method) {
                ((java.lang.reflect.Method) cached).invoke(view, new Object[]{null});
            }
        } catch (Throwable unused) {
        }
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            if (imageView.getDrawable() != null) {
                imageView.setImageDrawable(null);
            }
            if (imageView.getImageTintList() != null) {
                imageView.setImageTintList(null);
            }
        }
        if (view.getBackground() != null) {
            view.setBackground(null);
        }
    }

    private static void stylePillContainer(View pillHost, View view, View decor, View content, View skin, View msgSource) {
        if (pillHost == null) {
            pillHost = view;
        }
        if (pillHost.willNotDraw()) {
            pillHost.setWillNotDraw(false);
        }
        if (view.willNotDraw()) {
            view.setWillNotDraw(false);
        }
        Context ctx = view.getContext();
        float density = view.getResources().getDisplayMetrics().density;
        boolean dark = night(view);
        boolean myOn = LineSettings.materialYouTheme();
        MaterialYou.Palette pal = myOn ? MaterialYou.Palette.of(ctx) : null;

        int surfaceRgb;
        int rimAccentRgb;
        if (myOn) {
            surfaceRgb = dark
                ? MaterialYou.blend(0xFF161920, pal.navBarBg, 0.45f)
                : MaterialYou.blend(0xFFFFFFFF, pal.navBarBg, 0.22f);
            rimAccentRgb = pal.accent;
        } else {
            surfaceRgb = dark ? 0xFF181B22 : 0xFFFAFBFD;
            rimAccentRgb = dark ? 0xFFFFFFFF : 0xFF3E4752;
        }

        Drawable background = pillHost.getBackground();
        GlassPillDrawable fill;
        if (background instanceof GlassPillDrawable) {
            fill = (GlassPillDrawable) background;
        } else {
            fill = new GlassPillDrawable(pillHost, view);
            pillHost.setBackground(fill);
        }
        fill.bindSources(view, decor, content, skin, msgSource);
        fill.updateStyle(dark, surfaceRgb);

        if (Build.VERSION.SDK_INT >= 23) {
            if (view.getForegroundGravity() != Gravity.FILL) {
                view.setForegroundGravity(Gravity.FILL);
            }
            Drawable foreground = view.getForeground();
            GlassRimDrawable rim;
            if (foreground instanceof GlassRimDrawable && ((GlassRimDrawable) foreground).host == view) {
                rim = (GlassRimDrawable) foreground;
            } else {
                rim = new GlassRimDrawable(view, density);
                view.setForeground(rim);
            }
            rim.updateStyle(dark, rimAccentRgb);
        }
    }

    public static void tintInputIcons(View view, ChromeStyle chromeStyle) {
        boolean z = chromeStyle.bottomDarkIcons;
        int i = z ? -15658735 : -1;
        boolean pillDark = night(view);
        int i2 = pillDark
            ? -1
            : (LineSettings.materialYouTheme() ? MaterialYou.Palette.of(view.getContext()).navUnselected : ICON_EMOJI_DAY);
        for (String str : INPUT_DARK_ICONS) {
            tintImage(find(view, str), i);
        }
        for (String str2 : INPUT_EMOJI_ICONS) {
            tintImage(find(view, str2), i2);
        }
        tintMic(view, find(view, INPUT_MIC), i, z);
    }

    static void updateSendButtonTint(View input, ChromeStyle chromeStyle) {
        if (input == null || chromeStyle == null) {
            return;
        }
        boolean darkIcons = chromeStyle.bottomDarkIcons;
        int iconTint = darkIcons ? -15658735 : -1;
        tintMic(input, find(input, INPUT_MIC), iconTint, darkIcons);
    }

    private static void tintMic(View inputRoot, View view, int i, boolean bottomDarkIcons) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            Drawable drawable = imageView.getDrawable();
            if (drawable == null) {
                return;
            }
            if (hasGreenCircle(drawable)) {
                if (imageView.getImageTintList() != null) {
                    imageView.setImageTintList(null);
                }
                if (imageView.getColorFilter() != null) {
                    imageView.clearColorFilter();
                }
                TINTED.remove(imageView);
                if (LineSettings.materialYouTheme()) {
                    int accent = MaterialYou.sendButtonAccentColor(imageView.getContext(), bottomDarkIcons);
                    recolorGreenCircle(drawable, accent);
                }
                return;
            }
            int targetTint = i;
            if (LineSettings.materialYouTheme() && hasTypedInputText(inputRoot)) {
                targetTint = MaterialYou.sendButtonAccentColor(imageView.getContext(), bottomDarkIcons);
            }
            tintImage(imageView, targetTint);
        }
    }

    private static boolean hasTypedInputText(View inputRoot) {
        if (inputRoot == null) {
            return false;
        }
        View edit = find(inputRoot, INPUT_EDIT);
        if (edit instanceof TextView) {
            CharSequence text = ((TextView) edit).getText();
            return text != null && text.length() > 0;
        }
        return false;
    }

    private static void recolorGreenCircle(Drawable drawable, int accentColor) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            for (int i = 0; i < layerDrawable.getNumberOfLayers(); i++) {
                recolorGreenCircle(layerDrawable.getDrawable(i), accentColor);
            }
            return;
        }
        if (drawable instanceof ColorDrawable) {
            ColorDrawable cd = (ColorDrawable) drawable;
            int color = cd.getColor();
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            if (g > r + 20 && g > b + 20 && g > 80) {
                cd.mutate();
                cd.setColor(accentColor | 0xFF000000);
            }
        }
    }

    private static boolean hasGreenCircle(Drawable drawable) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            for (int i = 0; i < layerDrawable.getNumberOfLayers(); i++) {
                if (hasGreenCircle(layerDrawable.getDrawable(i))) {
                    return true;
                }
            }
            return false;
        }
        if (drawable instanceof ColorDrawable) {
            int color = ((ColorDrawable) drawable).getColor();
            int iRed = Color.red(color);
            int iGreen = Color.green(color);
            int iBlue = Color.blue(color);
            if (iGreen > iRed + 20 && iGreen > iBlue + 20 && iGreen > 80) {
                return true;
            }
        }
        return false;
    }

    public static void tintHeader(View view, ChromeStyle chromeStyle) {
        boolean forceWhite = LineSettings.chatHeaderWhite();
        int i = forceWhite ? 0xFFFFFFFF : ((chromeStyle != null && chromeStyle.topDarkIcons) ? -15658735 : HEADER_ICON_LIGHT);
        float shadowRadius = forceWhite ? dp(view.getContext(), 2.0f) : 0.0f;
        float shadowDy = forceWhite ? dp(view.getContext(), 1.0f) : 0.0f;
        int shadowColor = forceWhite ? 0x55000000 : 0;
        for (String str : HEADER_TEXT) {
            View viewFind = find(view, str);
            if (viewFind instanceof TextView) {
                TextView textView = (TextView) viewFind;
                if (textView.getCurrentTextColor() != i) {
                    textView.setTextColor(i);
                }
                if (textView.getShadowRadius() != shadowRadius || textView.getShadowColor() != shadowColor) {
                    textView.setShadowLayer(shadowRadius, 0.0f, shadowDy, shadowColor);
                }
            }
        }
        for (String str2 : HEADER_ICONS) {
            tintImage(find(view, str2), i);
        }
        tintHeaderImages(view, i, id(view.getContext(), HEADER_AVATAR));
    }

    private static void tintHeaderImages(View view, int i, int i2) {
        if (view == null) {
            return;
        }
        if (i2 == 0 || view.getId() != i2) {
            if (view instanceof ImageView) {
                tintImage(view, i);
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i3 = 0; i3 < viewGroup.getChildCount(); i3++) {
                    tintHeaderImages(viewGroup.getChildAt(i3), i, i2);
                }
            }
        }
    }

    private static void tintImage(View view, int i) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            Map<View, Integer> map = TINTED;
            Integer num = map.get(imageView);
            ColorStateList imageTintList = imageView.getImageTintList();
            if (num == null || num.intValue() != i || imageView.getColorFilter() == null || imageTintList == null || imageTintList.getDefaultColor() != i) {
                imageView.setImageTintList(ColorStateList.valueOf(i));
                imageView.setColorFilter(i, PorterDuff.Mode.SRC_IN);
                map.put(imageView, Integer.valueOf(i));
            }
        }
    }

    private static final float INPUT_PILL_MAX_RADIUS_DP = 22.0f;

    static void centerEmojiButton(View view) {
        if (view == null) {
            return;
        }
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float maxCapDiameter = view.getResources().getDisplayMetrics().density * (INPUT_PILL_MAX_RADIUS_DP * 2.0f);
        float endCapRadius = Math.min((float) height, maxCapDiameter) * 0.5f;
        float targetCenterX = width - endCapRadius;
        float targetCenterY = height - endCapRadius;

        for (String idName : INPUT_EMOJI_ICONS) {
            View icon = find(view, idName);
            if (icon == null || icon.getVisibility() != View.VISIBLE || icon.getWidth() <= 0 || icon.getHeight() <= 0) {
                continue;
            }
            View target = icon;
            float relLeft = 0.0f;
            float relTop = 0.0f;
            View cur = icon;
            boolean insidePill = false;
            while (cur != null && cur != view) {
                relLeft += cur.getLeft();
                relTop += cur.getTop();
                ViewParent vp = cur.getParent();
                if (!(vp instanceof View)) {
                    break;
                }
                View parentView = (View) vp;
                if (parentView == view) {
                    target = cur;
                    insidePill = true;
                    break;
                }
                relLeft += cur.getTranslationX();
                relTop += cur.getTranslationY();
                cur = parentView;
            }
            if (!insidePill) {
                continue;
            }
            float naturalCenterX = relLeft + (icon.getPaddingLeft() + (icon.getWidth() - icon.getPaddingRight())) * 0.5f;
            float naturalCenterY = relTop + (icon.getPaddingTop() + (icon.getHeight() - icon.getPaddingBottom())) * 0.5f;
            float desiredTx = targetCenterX - naturalCenterX;
            float desiredTy = targetCenterY - naturalCenterY;
            if (Math.abs(target.getTranslationX() - desiredTx) > 0.2f) {
                target.setTranslationX(desiredTx);
            }
            if (Math.abs(target.getTranslationY() - desiredTy) > 0.2f) {
                target.setTranslationY(desiredTy);
            }
            break;
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static final class GlassPillDrawable extends Drawable {
        static boolean recordingPillBlur;

        private final WeakReference<View> hostRef;
        private final float density;
        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF pillRect = new RectF();
        private final RectF shadowRect = new RectF();
        private final Path clipPath = new Path();
        private final int[] hostLoc = new int[2];
        private final int[] pillLoc = new int[2];

        private Object gpu;
        private WeakReference<View> pillRef;
        private WeakReference<View> decorRef;
        private WeakReference<View> contentRef;
        private WeakReference<View> skinRef;
        private WeakReference<View> msgSourceRef;
        private boolean dark;
        private int surfaceRgb;
        private boolean blurDirty = true;
        private int lastWinX = Integer.MIN_VALUE;
        private int lastWinY = Integer.MIN_VALUE;
        private int cachedWidth = -1;
        private int cachedHeight = -1;
        private int cachedStyleKey = Integer.MIN_VALUE;

        GlassPillDrawable(View host, View pill) {
            this.hostRef = new WeakReference<>(host);
            this.pillRef = new WeakReference<>(pill);
            this.density = host.getResources().getDisplayMetrics().density;
            this.shadowPaint.setStyle(Paint.Style.FILL);
            this.fillPaint.setStyle(Paint.Style.FILL);
        }

        void bindSources(View pill, View decor, View content, View skin, View msgSource) {
            if (pill != null && (this.pillRef == null || this.pillRef.get() != pill)) {
                this.pillRef = new WeakReference<>(pill);
                this.blurDirty = true;
            }
            if (decor != null && (this.decorRef == null || this.decorRef.get() != decor)) {
                this.decorRef = new WeakReference<>(decor);
                this.blurDirty = true;
            }
            if (content != null && (this.contentRef == null || this.contentRef.get() != content)) {
                this.contentRef = new WeakReference<>(content);
                this.blurDirty = true;
            }
            if (skin != null && (this.skinRef == null || this.skinRef.get() != skin)) {
                this.skinRef = new WeakReference<>(skin);
                this.blurDirty = true;
            }
            if (msgSource != null && (this.msgSourceRef == null || this.msgSourceRef.get() != msgSource)) {
                this.msgSourceRef = new WeakReference<>(msgSource);
                this.blurDirty = true;
            }
        }

        void updateStyle(boolean dark, int surfaceRgb) {
            if (this.dark != dark || this.surfaceRgb != surfaceRgb) {
                this.dark = dark;
                this.surfaceRgb = surfaceRgb;
                this.cachedWidth = -1;
                this.blurDirty = true;
                invalidateSelf();
            }
        }

        void markDirty() {
            this.blurDirty = true;
            View host = this.hostRef.get();
            if (host != null && host.isAttachedToWindow()) {
                host.invalidate();
            }
        }

        private int resolveBaseChatBg() {
            View content = this.contentRef != null ? this.contentRef.get() : null;
            if (content != null && content.getBackground() instanceof ColorDrawable) {
                int c = ((ColorDrawable) content.getBackground()).getColor();
                if ((c >>> 24) > 0) {
                    return c | 0xFF000000;
                }
            }
            View decor = this.decorRef != null ? this.decorRef.get() : null;
            if (decor != null && decor.getBackground() instanceof ColorDrawable) {
                int c = ((ColorDrawable) decor.getBackground()).getColor();
                if ((c >>> 24) > 0) {
                    return c | 0xFF000000;
                }
            }
            return this.dark ? 0xFF14171D : 0xFFEFF3F8;
        }

        private void ensureShaders(int w, int h, float radius, boolean canBlur) {
            int styleKey = (this.surfaceRgb & 0x00FFFFFF)
                ^ (this.dark ? 0x40000000 : 0)
                ^ (canBlur ? 0x20000000 : 0);
            if (w == this.cachedWidth && h == this.cachedHeight && styleKey == this.cachedStyleKey) {
                return;
            }
            this.cachedWidth = w;
            this.cachedHeight = h;
            this.cachedStyleKey = styleKey;
            this.blurDirty = true;

            this.pillRect.set(0.0f, 0.0f, w, h);
            this.clipPath.reset();
            this.clipPath.addRoundRect(this.pillRect, radius, radius, Path.Direction.CW);

            int alphaTop;
            int alphaBottom;
            if (canBlur) {
                alphaTop = this.dark ? 0xAA : 0xA6;
                alphaBottom = this.dark ? 0xBE : 0xBA;
            } else {
                alphaTop = this.dark ? 0xEE : 0xF0;
                alphaBottom = this.dark ? 0xF5 : 0xF7;
            }
            int topFill = withAlpha(MaterialYou.blend(this.surfaceRgb, 0xFFFFFFFF, this.dark ? 0.05f : 0.35f), alphaTop);
            int botFill = withAlpha(this.surfaceRgb, alphaBottom);
            this.fillPaint.setColor(0xFFFFFFFF);
            this.fillPaint.setShader(new LinearGradient(
                0.0f, 0.0f, 0.0f, h,
                new int[]{topFill, botFill},
                null,
                Shader.TileMode.CLAMP
            ));
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            if (recordingPillBlur || ProgressiveBlurDrawable.recording) {
                return;
            }
            View host = this.hostRef.get();
            View pill = this.pillRef != null ? this.pillRef.get() : host;
            if (host == null || pill == null || pill.getVisibility() != View.VISIBLE) {
                return;
            }
            int w = pill.getWidth();
            int h = pill.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            float radius = Math.min(Math.min(w, h) * 0.5f, this.density * INPUT_PILL_MAX_RADIUS_DP);
            boolean canBlur = !powerSave() && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated();
            ensureShaders(w, h, radius, canBlur);

            host.getLocationInWindow(this.hostLoc);
            pill.getLocationInWindow(this.pillLoc);
            int winX = this.pillLoc[0];
            int winY = this.pillLoc[1];
            if (winX != this.lastWinX || winY != this.lastWinY) {
                this.lastWinX = winX;
                this.lastWinY = winY;
                this.blurDirty = true;
            }
            float offsetX = winX - this.hostLoc[0];
            float offsetY = winY - this.hostLoc[1];

            int outerSave = canvas.save();
            canvas.translate(offsetX, offsetY);
            try {
                // Clip out the pill interior and draw the same 6-step exterior shadow as FloatingNavPillDrawable.
                int shadowSave = canvas.save();
                if (Build.VERSION.SDK_INT >= 26) {
                    canvas.clipOutPath(this.clipPath);
                }
                int stepAlpha = this.dark ? 0x08 : 0x03;
                for (int step = 6; step >= 1; step--) {
                    float spread = step * this.density * 1.9f;
                    float dy = step * this.density * 0.75f;
                    this.shadowRect.set(
                        -spread * 0.6f,
                        dy * 0.25f - spread * 0.2f,
                        w + spread * 0.6f,
                        h + dy + spread * 0.65f
                    );
                    this.shadowPaint.setColor(withAlpha(0x000000, stepAlpha));
                    float sr = radius + spread * 0.6f;
                    canvas.drawRoundRect(this.shadowRect, sr, sr, this.shadowPaint);
                }
                canvas.restoreToCount(shadowSave);

                int save = canvas.save();
                canvas.clipPath(this.clipPath);
                try {
                    if (canBlur) {
                        View skin = this.skinRef != null ? this.skinRef.get() : null;
                        View msgSource = this.msgSourceRef != null ? this.msgSourceRef.get() : null;
                        if (skin != null || msgSource != null) {
                            if (this.gpu == null) {
                                this.gpu = new InputPillBlurGpu();
                            }
                            int baseBg = resolveBaseChatBg();
                            ((InputPillBlurGpu) this.gpu).draw(
                                canvas, host, skin, msgSource, baseBg, winX, winY, w, h, this.density, this.blurDirty
                            );
                        }
                    }
                    this.blurDirty = false;
                    canvas.drawRect(this.pillRect, this.fillPaint);
                } finally {
                    canvas.restoreToCount(save);
                }
            } finally {
                canvas.restoreToCount(outerSave);
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int alpha) {
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class InputPillBlurGpu {
        private final RenderNode node = new RenderNode("ChatInputPillGlass");
        private final int[] srcLoc = new int[2];
        private RenderEffect effect;
        private float lastDensity;
        private int lastW;
        private int lastH;

        void draw(
            Canvas canvas,
            View pillHost,
            View skin,
            View msgSource,
            int baseBgColor,
            int pillWinX,
            int pillWinY,
            int w,
            int h,
            float density,
            boolean dirty
        ) {
            boolean sizeChanged = w != this.lastW || h != this.lastH;
            if (dirty || sizeChanged || !this.node.hasDisplayList()) {
                this.node.setPosition(0, 0, w, h);
                RecordingCanvas rc = this.node.beginRecording(w, h);
                boolean prevProgRec = ProgressiveBlurDrawable.recording;
                GlassPillDrawable.recordingPillBlur = true;
                ProgressiveBlurDrawable.recording = true;
                try {
                    rc.drawColor(baseBgColor);
                    recordSource(rc, skin, pillHost, pillWinX, pillWinY);
                    recordSource(rc, msgSource, pillHost, pillWinX, pillWinY);
                } finally {
                    ProgressiveBlurDrawable.recording = prevProgRec;
                    GlassPillDrawable.recordingPillBlur = false;
                    this.node.endRecording();
                }
                this.lastW = w;
                this.lastH = h;
            }
            if (this.effect == null || density != this.lastDensity) {
                float r = density * 28.0f;
                RenderEffect blur = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP);
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(1.42f);
                this.effect = RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(cm), blur);
                this.node.setRenderEffect(this.effect);
                this.lastDensity = density;
            }
            canvas.drawRenderNode(this.node);
        }

        private void recordSource(Canvas rc, View src, View pillHost, int pillWinX, int pillWinY) {
            if (src == null
                || src.getVisibility() != View.VISIBLE
                || !src.isAttachedToWindow()
                || isSelfOrAncestor(src, pillHost)) {
                return;
            }
            src.getLocationInWindow(this.srcLoc);
            int s = rc.save();
            rc.translate(this.srcLoc[0] - pillWinX, this.srcLoc[1] - pillWinY);
            try {
                src.draw(rc);
            } catch (Throwable ignored) {
            } finally {
                rc.restoreToCount(s);
            }
        }
    }

    private static final class GlassRimDrawable extends Drawable {
        final View host;
        private final float density;
        private boolean dark;
        private int rimAccentRgb;
        private final Paint innerRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF innerRimRect = new RectF();
        private final RectF rimRect = new RectF();
        private int shaderWidth = -1;
        private int shaderHeight = -1;
        private int shaderStyleKey = Integer.MIN_VALUE;

        GlassRimDrawable(View host, float density) {
            this.host = host;
            this.density = density;
            this.innerRimPaint.setStyle(Paint.Style.STROKE);
            this.innerRimPaint.setStrokeWidth(Math.max(1.0f, density * 1.0f));
            this.rimPaint.setStyle(Paint.Style.STROKE);
            this.rimPaint.setStrokeWidth(Math.max(1.0f, density * 0.75f));
        }

        void updateStyle(boolean dark, int rimAccentRgb) {
            if (this.dark != dark || this.rimAccentRgb != rimAccentRgb) {
                this.dark = dark;
                this.rimAccentRgb = rimAccentRgb;
                this.shaderWidth = -1;
                invalidateSelf();
            }
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicWidth() {
            return this.host != null ? this.host.getWidth() : -1;
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicHeight() {
            return this.host != null ? this.host.getHeight() : -1;
        }

        private void ensureShader(int w, int h) {
            int styleKey = ((this.rimAccentRgb & 0x00FFFFFF) * 31) ^ (this.dark ? 0x40000000 : 0);
            if (w == this.shaderWidth && h == this.shaderHeight && styleKey == this.shaderStyleKey) {
                return;
            }
            this.shaderWidth = w;
            this.shaderHeight = h;
            this.shaderStyleKey = styleKey;

            float outerStroke = this.rimPaint.getStrokeWidth();
            float outerInset = outerStroke * 0.5f;
            this.rimRect.set(outerInset, outerInset, w - outerInset, h - outerInset);

            float innerInset = outerStroke + this.innerRimPaint.getStrokeWidth() * 0.5f;
            this.innerRimRect.set(innerInset, innerInset, w - innerInset, h - innerInset);

            if (this.dark) {
                this.innerRimPaint.setColor(0xFFFFFFFF);
                this.innerRimPaint.setShader(new LinearGradient(
                    0.0f, 0.0f, 0.0f, h,
                    new int[]{0x38FFFFFF, 0x0EFFFFFF, 0x1AFFFFFF},
                    new float[]{0.0f, 0.48f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
                int topRim = withAlpha(MaterialYou.blend(0xFFFFFFFF, this.rimAccentRgb, 0.20f), 0x26);
                int midRim = withAlpha(this.rimAccentRgb, 0x14);
                int botRim = 0x10FFFFFF;
                this.rimPaint.setColor(0xFFFFFFFF);
                this.rimPaint.setShader(new LinearGradient(
                    0.0f, 0.0f, 0.0f, h,
                    new int[]{topRim, midRim, botRim},
                    new float[]{0.0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
            } else {
                this.innerRimPaint.setColor(0xFFFFFFFF);
                this.innerRimPaint.setShader(new LinearGradient(
                    0.0f, 0.0f, 0.0f, h,
                    new int[]{0xD4FFFFFF, 0x50FFFFFF, 0x88FFFFFF},
                    new float[]{0.0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
                int hairlineBase = MaterialYou.blend(0xFF0F1C22, this.rimAccentRgb, 0.25f);
                int topRim = withAlpha(hairlineBase, 0x0C);
                int midRim = withAlpha(hairlineBase, 0x10);
                int botRim = withAlpha(hairlineBase, 0x15);
                this.rimPaint.setColor(0xFFFFFFFF);
                this.rimPaint.setShader(new LinearGradient(
                    0.0f, 0.0f, 0.0f, h,
                    new int[]{topRim, midRim, botRim},
                    new float[]{0.0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            int w = this.host != null ? this.host.getWidth() : getBounds().width();
            int h = this.host != null ? this.host.getHeight() : getBounds().height();
            if (w <= 0 || h <= 0) {
                return;
            }
            ensureShader(w, h);
            float baseRadius = Math.min(Math.min(w, h) * 0.5f, this.density * INPUT_PILL_MAX_RADIUS_DP);
            float outerStroke = this.rimPaint.getStrokeWidth();
            float outerInset = outerStroke * 0.5f;
            float innerInset = outerStroke + this.innerRimPaint.getStrokeWidth() * 0.5f;
            float innerRadius = Math.max(0.0f, baseRadius - innerInset);
            canvas.drawRoundRect(this.innerRimRect, innerRadius, innerRadius, this.innerRimPaint);
            float rimRadius = Math.max(0.0f, baseRadius - outerInset);
            canvas.drawRoundRect(this.rimRect, rimRadius, rimRadius, this.rimPaint);
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int alpha) {
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
