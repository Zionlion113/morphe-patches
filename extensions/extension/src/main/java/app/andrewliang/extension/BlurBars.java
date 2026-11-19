package app.andrewliang.extension;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import kotlin.io.ConstantsKt;
import kotlin.jvm.internal.IntCompanionObject;

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
    private static final String[] HEADER_TEXT = {"header_title", "header_title_count"};
    private static final String[] HEADER_ICONS = {"header_up_button"};
    private static final Map<String, Integer> IDS = new HashMap();
    private static final Map<View, Boolean> HIDDEN = Collections.synchronizedMap(new WeakHashMap());
    private static final Map<View, MirroredArtworkDrawable> MIRRORED = new WeakHashMap();
    private static final Map<View, Boolean> WATCHED = new WeakHashMap();
    private static final Map<View, Boolean> PENDING = new WeakHashMap();
    private static final Map<View, Boolean> SKIN_PREPARE = new WeakHashMap();
    private static final Map<View, ChatScreen> CHATS = new WeakHashMap();
    private static final Map<View, Boolean> STROKE_CLEARED = new WeakHashMap();
    private static final Map<View, Integer> TINTED = new WeakHashMap();

    private BlurBars() {
    }

    public static void init(Application application) {
        if (registered || application == null) {
            return;
        }
        registered = true;
        watchPowerSave(application);
        watchConfiguration(application);
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() { // from class: app.andrewliang.extension.BlurBars.1
            @Override // android.app.Application.ActivityLifecycleCallbacks
            public void onActivityDestroyed(Activity activity) {
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
        if (enabled()) {
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

    public static void layoutChatSkin(final ImageView imageView) {
        if (!enabled() || imageView == null) {
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
        if (!enabled() || view == null) {
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

    public static void watch(Activity activity) {
        Window window;
        if (enabled() && (window = activity.getWindow()) != null) {
            if (Build.VERSION.SDK_INT >= 29) {
                window.setStatusBarContrastEnforced(false);
                window.setNavigationBarContrastEnforced(false);
            }
            final View decorView = window.getDecorView();
            Map<View, Boolean> map = WATCHED;
            if (map.containsKey(decorView)) {
                return;
            }
            map.put(decorView, Boolean.TRUE);
            final Runnable runnable = new Runnable() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    BlurBars.lambda$watch$2(decorView);
                }
            };
            ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda11
                @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
                public final void onGlobalLayout() {
                    BlurBars.lambda$watch$3(decorView, runnable);
                }
            });
            KeyboardMotion.onStop(decorView, new Runnable() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    BlurBars.lambda$watch$4(decorView, runnable);
                }
            });
            viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() { // from class: app.andrewliang.extension.BlurBars$$ExternalSyntheticLambda7
                @Override // android.view.ViewTreeObserver.OnPreDrawListener
                public final boolean onPreDraw() {
                    return BlurBars.lambda$watch$5(decorView);
                }
            });
            decorView.post(runnable);
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
        if (!enabled()) {
            return true;
        }
        keepHidden();
        keepMirrored();
        ChatScreen chatScreen = CHATS.get(view);
        if (chatScreen != null) {
            chatScreen.onPreDraw();
        }
        return true;
    }

    private static void scan(View view) {
        String[] strArr;
        if (enabled()) {
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
                    View viewFind = find(view, TABS_ARTWORK);
                    if (viewFind != null) {
                        mirror(view2, viewFind, false);
                    } else {
                        hide(view2);
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
            this.setMarginTop = IntCompanionObject.MIN_VALUE;
            this.setMarginBottom = IntCompanionObject.MIN_VALUE;
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
        }

        private View pill;
        private View pillBg;

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
                View viewFind = BlurBars.find(this.input, BlurBars.CHAT_INPUT_ROW);
                if (viewFind != null && viewFind.getBackground() != null) {
                    viewFind.setBackground(null);
                }
                View pillContainer = this.pill;
                if (pillContainer == null || !pillContainer.isAttachedToWindow()) {
                    pillContainer = BlurBars.find(this.input, BlurBars.CHAT_INPUT_PILL);
                    this.pill = pillContainer;
                }
                View pillBgView = this.pillBg;
                if (pillBgView == null || !pillBgView.isAttachedToWindow()) {
                    pillBgView = BlurBars.find(this.input, BlurBars.CHAT_INPUT_PILL_BG);
                    this.pillBg = pillBgView;
                }
                boolean needsPill = pillContainer != null
                    && (!(pillContainer.getBackground() instanceof GlassPillDrawable)
                        || (Build.VERSION.SDK_INT >= 23
                            && (!(pillContainer.getForeground() instanceof GlassRimDrawable)
                                || pillContainer.getForegroundGravity() != Gravity.FILL)));
                boolean needsClearBg = pillBgView != null
                    && (pillBgView.getBackground() != null
                        || (pillBgView instanceof ImageView && ((ImageView) pillBgView).getDrawable() != null));
                if (needsPill || needsClearBg) {
                    BlurBars.smoothInputPill(this.input, this.style);
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
                BlurBars.smoothInputPill(this.input, this.style);
                BlurBars.tintInputIcons(this.input, this.style);
            }
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
            if (!BlurBars.powerSave()) {
                View view6 = this.skin;
                boolean z = view6 != null && view6.isDirty();
                if (z || this.content.isDirty()) {
                    this.top.invalidate();
                }
                if (z || ((view = this.refresh) != null && view.isDirty())) {
                    this.bottom.invalidate();
                }
            }
            lightIcons();
        }

        private void lightIcons() {
            boolean z = this.style.topDarkIcons;
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
            int i3 = z ? systemUiVisibility | ConstantsKt.DEFAULT_BUFFER_SIZE : systemUiVisibility & (-8193);
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
                View view5 = (View) ((view4 == null || !(view4.getParent() instanceof View)) ? this.header : this.spacer).getParent();
                Object parent = this.refresh.getParent();
                while (true) {
                    View view6 = (View) parent;
                    if (!(view6 instanceof ViewGroup)) {
                        break;
                    }
                    ViewGroup viewGroup = (ViewGroup) view6;
                    viewGroup.setClipChildren(false);
                    viewGroup.setClipToPadding(false);
                    if (view6 == view5 || !(view6.getParent() instanceof View)) {
                        break;
                    } else {
                        parent = view6.getParent();
                    }
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
        View viewFind = find(view, CHAT_INPUT_PILL);
        View viewFind2 = find(view, CHAT_INPUT_PILL_BG);
        if (viewFind2 != null) {
            clearPillBg(viewFind2);
        }
        if (viewFind != null) {
            roundClip(viewFind);
            stylePillContainer(viewFind, chromeStyle.bottomDarkIcons);
            centerEmojiButton(viewFind);
        }
        View viewFind3 = find(view, INPUT_EDIT);
        if (viewFind3 instanceof TextView) {
            boolean z = chromeStyle.bottomDarkIcons;
            TextView textView = (TextView) viewFind3;
            int i = z ? -15658735 : -1;
            int i2 = z ? INPUT_HINT_DAY : INPUT_HINT_DARK;
            if (textView.getCurrentTextColor() != i) {
                textView.setTextColor(i);
            }
            if (textView.getCurrentHintTextColor() != i2) {
                textView.setHintTextColor(i2);
            }
        }
    }

    private static void roundClip(View view) {
        if (!(view.getOutlineProvider() instanceof PillOutline)) {
            view.setOutlineProvider(new PillOutline());
        }
        if (view.getClipToOutline()) {
            return;
        }
        view.setClipToOutline(true);
    }

    private static void clearPillBg(View view) {
        try {
            view.getClass().getMethod("setOverwrappingDrawable", Drawable.class).invoke(view, new Object[]{null});
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

    private static void stylePillContainer(View view, boolean darkIcons) {
        if (view.willNotDraw()) {
            view.setWillNotDraw(false);
        }
        float density = view.getResources().getDisplayMetrics().density;
        Drawable background = view.getBackground();
        GlassPillDrawable fill;
        if (background instanceof GlassPillDrawable) {
            fill = (GlassPillDrawable) background;
        } else {
            fill = new GlassPillDrawable(darkIcons);
            view.setBackground(fill);
        }
        fill.setDarkIcons(darkIcons);
        if (Build.VERSION.SDK_INT >= 23) {
            if (view.getForegroundGravity() != Gravity.FILL) {
                view.setForegroundGravity(Gravity.FILL);
            }
            Drawable foreground = view.getForeground();
            GlassRimDrawable rim;
            if (foreground instanceof GlassRimDrawable && ((GlassRimDrawable) foreground).host == view) {
                rim = (GlassRimDrawable) foreground;
            } else {
                rim = new GlassRimDrawable(view, density, darkIcons);
                view.setForeground(rim);
            }
            rim.setDarkIcons(darkIcons);
        }
    }

    public static void tintInputIcons(View view, ChromeStyle chromeStyle) {
        boolean z = chromeStyle.bottomDarkIcons;
        int i = z ? -15658735 : -1;
        int i2 = z ? ICON_EMOJI_DAY : -1;
        for (String str : INPUT_DARK_ICONS) {
            tintImage(find(view, str), i);
        }
        for (String str2 : INPUT_EMOJI_ICONS) {
            tintImage(find(view, str2), i2);
        }
        tintMic(find(view, INPUT_MIC), i);
    }

    private static void tintMic(View view, int i) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            Drawable drawable = imageView.getDrawable();
            if (drawable == null || hasGreenCircle(drawable)) {
                if (imageView.getImageTintList() != null) {
                    imageView.setImageTintList(null);
                }
                if (imageView.getColorFilter() != null) {
                    imageView.clearColorFilter();
                }
                TINTED.remove(imageView);
                return;
            }
            tintImage(imageView, i);
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
        int i = chromeStyle.topDarkIcons ? -15658735 : HEADER_ICON_LIGHT;
        for (String str : HEADER_TEXT) {
            View viewFind = find(view, str);
            if (viewFind instanceof TextView) {
                TextView textView = (TextView) viewFind;
                if (textView.getCurrentTextColor() != i) {
                    textView.setTextColor(i);
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

    private static void centerEmojiButton(View view) {
        View view2;
        String[] strArr = INPUT_EMOJI_ICONS;
        View viewFind = find(view, strArr[0]);
        if (viewFind == null) {
            viewFind = find(view, strArr[1]);
        }
        if (viewFind == null || !(viewFind.getParent() instanceof View) || (view2 = (View) viewFind.getParent()) == view || view2.getParent() != view) {
            return;
        }
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0 || view2.getWidth() <= 0 || view2.getHeight() <= 0) {
            return;
        }
        float fRound = Math.round((width - (Math.min(height, dp(view.getContext(), 38.0f)) / 2.0f)) - (view2.getLeft() + (view2.getWidth() / 2.0f)));
        float fRound2 = Math.round((height / 2.0f) - (view2.getTop() + (view2.getHeight() / 2.0f)));
        if (view2.getTranslationX() != fRound) {
            view2.setTranslationX(fRound);
        }
        if (view2.getTranslationY() != fRound2) {
            view2.setTranslationY(fRound2);
        }
    }

    private static final class PillOutline extends ViewOutlineProvider {
        private PillOutline() {
        }

        @Override // android.view.ViewOutlineProvider
        public void getOutline(View view, Outline outline) {
            int width = view.getWidth();
            int height = view.getHeight();
            if (width <= 0 || height <= 0) {
                outline.setEmpty();
            } else {
                outline.setRoundRect(0, 0, width, height, Math.min(width, height) / 2.0f);
            }
        }
    }

    private static final class GlassPillDrawable extends Drawable {
        private boolean darkIcons;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF fillRect = new RectF();
        private int shaderWidth = -1;
        private int shaderHeight = -1;
        private boolean shaderDarkIcons;

        GlassPillDrawable(boolean darkIcons) {
            this.darkIcons = darkIcons;
            this.fillPaint.setStyle(Paint.Style.FILL);
        }

        void setDarkIcons(boolean darkIcons) {
            if (this.darkIcons != darkIcons) {
                this.darkIcons = darkIcons;
                this.shaderWidth = -1;
                invalidateSelf();
            }
        }

        private void ensureShader(Rect bounds) {
            int w = bounds.width();
            int h = bounds.height();
            if (w == this.shaderWidth && h == this.shaderHeight && this.darkIcons == this.shaderDarkIcons) {
                return;
            }
            this.shaderWidth = w;
            this.shaderHeight = h;
            this.shaderDarkIcons = this.darkIcons;
            float left = bounds.left;
            float top = bounds.top;
            float right = bounds.right;
            float bottom = bounds.bottom;
            this.fillRect.set(left, top, right, bottom);
            if (this.darkIcons) {
                this.fillPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0x4EFFFFFF, 0x2AFFFFFF},
                    null,
                    Shader.TileMode.CLAMP
                ));
            } else {
                this.fillPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0x28FFFFFF, 0x12FFFFFF},
                    null,
                    Shader.TileMode.CLAMP
                ));
            }
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) {
                return;
            }
            ensureShader(bounds);
            float radius = Math.min(bounds.width(), bounds.height()) * 0.5f;
            canvas.drawRoundRect(this.fillRect, radius, radius, this.fillPaint);
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int alpha) {
            this.fillPaint.setAlpha(alpha);
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
            this.fillPaint.setColorFilter(colorFilter);
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class GlassRimDrawable extends Drawable {
        final View host;
        private final float density;
        private boolean darkIcons;
        private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint innerFacetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF sheenRect = new RectF();
        private final RectF innerFacetRect = new RectF();
        private final RectF rimRect = new RectF();
        private final RectF outerRect = new RectF();
        private int shaderWidth = -1;
        private int shaderHeight = -1;
        private boolean shaderDarkIcons;

        GlassRimDrawable(View host, float density, boolean darkIcons) {
            this.host = host;
            this.density = density;
            this.darkIcons = darkIcons;
            this.sheenPaint.setStyle(Paint.Style.STROKE);
            this.sheenPaint.setStrokeWidth(Math.max(2.4f, density * 2.2f));
            this.innerFacetPaint.setStyle(Paint.Style.STROKE);
            this.innerFacetPaint.setStrokeWidth(Math.max(1.0f, density * 0.75f));
            this.rimPaint.setStyle(Paint.Style.STROKE);
            this.rimPaint.setStrokeWidth(Math.max(1.8f, density * 1.45f));
            this.outerPaint.setStyle(Paint.Style.STROKE);
            this.outerPaint.setStrokeWidth(Math.max(1.0f, density * 0.85f));
        }

        void setDarkIcons(boolean darkIcons) {
            if (this.darkIcons != darkIcons) {
                this.darkIcons = darkIcons;
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

        private void ensureShaders(int w, int h) {
            if (w == this.shaderWidth && h == this.shaderHeight && this.darkIcons == this.shaderDarkIcons) {
                return;
            }
            this.shaderWidth = w;
            this.shaderHeight = h;
            this.shaderDarkIcons = this.darkIcons;
            float left = 0.0f;
            float top = 0.0f;
            float right = w;
            float bottom = h;
            float outerStroke = this.outerPaint.getStrokeWidth();
            float outerInset = outerStroke * 0.5f;
            this.outerRect.set(left + outerInset, top + outerInset, right - outerInset, bottom - outerInset);
            float rimStroke = this.rimPaint.getStrokeWidth();
            float rimInset = (this.darkIcons ? outerStroke * 0.85f : 0.5f) + rimStroke * 0.5f;
            this.rimRect.set(left + rimInset, top + rimInset, right - rimInset, bottom - rimInset);
            float sheenStroke = this.sheenPaint.getStrokeWidth();
            float sheenInset = rimInset + rimStroke * 0.5f + sheenStroke * 0.45f;
            this.sheenRect.set(left + sheenInset, top + sheenInset, right - sheenInset, bottom - sheenInset);
            float facetInset = rimInset + rimStroke * 0.5f + Math.max(2.0f, this.density * 1.9f);
            this.innerFacetRect.set(left + facetInset, top + facetInset, right - facetInset, bottom - facetInset);
            if (this.darkIcons) {
                // Light/default chat: crisp dark outer edge + pure white top-lit rim + 2dp refractive glass wall thickness
                this.outerPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0x2A000000, 0x44000000},
                    null,
                    Shader.TileMode.CLAMP
                ));
                this.rimPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0xFFFFFFFF, 0xCCFFFFFF, 0x58FFFFFF, 0x38FFFFFF},
                    new float[]{0.0f, 0.26f, 0.65f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
                this.sheenPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0x68FFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x36FFFFFF},
                    new float[]{0.0f, 0.20f, 0.82f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
                this.innerFacetPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0x1E000000, 0x08FFFFFF, 0x10FFFFFF, 0x4CFFFFFF},
                    new float[]{0.0f, 0.30f, 0.72f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
            } else {
                // Dark/photo chat: Antigravity top-bright to bottom-dim white rim + glass wall internal light reflection
                this.outerPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0x18000000, 0x38000000},
                    null,
                    Shader.TileMode.CLAMP
                ));
                this.rimPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0xFAFFFFFF, 0x9EFFFFFF, 0x3EFFFFFF, 0x24FFFFFF},
                    new float[]{0.0f, 0.26f, 0.65f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
                this.sheenPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0x4EFFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x2AFFFFFF},
                    new float[]{0.0f, 0.20f, 0.82f, 1.0f},
                    Shader.TileMode.CLAMP
                ));
                this.innerFacetPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{0x28FFFFFF, 0x08FFFFFF, 0x08FFFFFF, 0x3CFFFFFF},
                    new float[]{0.0f, 0.28f, 0.72f, 1.0f},
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
            ensureShaders(w, h);
            float sheenRadius = Math.max(0.0f, Math.min(this.sheenRect.width(), this.sheenRect.height()) * 0.5f);
            canvas.drawRoundRect(this.sheenRect, sheenRadius, sheenRadius, this.sheenPaint);
            float facetRadius = Math.max(0.0f, Math.min(this.innerFacetRect.width(), this.innerFacetRect.height()) * 0.5f);
            canvas.drawRoundRect(this.innerFacetRect, facetRadius, facetRadius, this.innerFacetPaint);
            float outerRadius = Math.max(0.0f, Math.min(this.outerRect.width(), this.outerRect.height()) * 0.5f);
            canvas.drawRoundRect(this.outerRect, outerRadius, outerRadius, this.outerPaint);
            float rimRadius = Math.max(0.0f, Math.min(this.rimRect.width(), this.rimRect.height()) * 0.5f);
            canvas.drawRoundRect(this.rimRect, rimRadius, rimRadius, this.rimPaint);
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int alpha) {
            this.sheenPaint.setAlpha(alpha);
            this.innerFacetPaint.setAlpha(alpha);
            this.rimPaint.setAlpha(alpha);
            this.outerPaint.setAlpha(alpha);
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
            this.sheenPaint.setColorFilter(colorFilter);
            this.innerFacetPaint.setColorFilter(colorFilter);
            this.rimPaint.setColorFilter(colorFilter);
            this.outerPaint.setColorFilter(colorFilter);
        }

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
