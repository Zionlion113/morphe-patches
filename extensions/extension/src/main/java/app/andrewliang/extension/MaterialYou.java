package app.andrewliang.extension;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Insets;
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
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

final class MaterialYou {

    // Per-chat bubble mode values stored in LineSettings (custom ARGB colors always have alpha 0xFF, i.e. < 0)
    static final int MODE_AUTO = 0;
    static final int MODE_WALLPAPER = 1;
    static final int MODE_MATERIAL_YOU = 2;
    static final int MODE_LINE_DEFAULT = 3;

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

    private static final String[] BUBBLE_CONTAINER_IDS = {
            "chat_ui_text_message_balloon_root",
            "chat_ui_row_message",
            "chathistory_row_message",
            "chat_ui_row_timeline_message",
            "chat_ui_bubble_root",
            "chat_ui_row_gift_message_bubble",
    };

    private static final String[] BUBBLE_QUOTE_CONTAINER_IDS = {
            "chat_ui_message_source_balloon",
    };

    private static final String[] BUBBLE_TEXT_IDS = {
            "chat_ui_message_text",
            "chat_ui_row_file_title",
            "chat_ui_unavailable_message",
            "chat_ui_content_text",
            "chat_ui_row_voice_play_time",
            "chathistory_row_contact_message",
            "chat_ui_row_contact_message",
            "chat_ui_row_location_message_name",
            "chat_ui_label_text",
            "chat_ui_link_message",
            "chat_ui_link_linktext",
            "chat_ui_row_group_call_text_horizontal",
            "chat_ui_title_text",
            "chat_ui_service_name",
            "chat_ui_row_e2ee_undecrypted_title",
            "chat_ui_row_timeline_album_title",
            "chat_ui_row_timeline_message_text",
            "chat_ui_row_timeline_message_location_name_text",
            "chat_ui_groupboard_tv",
    };

    private static final String[] BUBBLE_SUBTEXT_IDS = {
            "chat_ui_row_file_duration",
            "chat_ui_row_file_size",
            "chat_ui_sender_name",
            "chat_ui_row_location_message_address",
            "chat_ui_link_subtext",
            "chat_ui_sub_text",
            "chat_ui_type_text",
            "chat_ui_type_sub_text",
            "chat_ui_row_e2ee_undecrypted_content",
            "chat_ui_row_timeline_message_album_status_text",
            "chat_ui_row_timeline_message_location_address_text",
            "bottom_service_name",
    };

    private static final String[] BUBBLE_DIVIDER_IDS = {
            "chat_ui_divider",
            "chat_ui_border",
            "chat_ui_bottom_divider",
            "bottom_divider",
            "chat_ui_service_name_line_divider",
    };

    private static final String[] BUBBLE_VOICE_ICON_IDS = {
            "chat_ui_row_voice_play_icon",
            "chat_ui_row_voice_pause_icon",
            "chat_ui_row_voice_play_progress",
    };

    private static final String[] BUBBLE_ACCENT_CIRCLE_IDS = {
            "chat_ui_row_groupcall_icon_horizontal",
            "chat_ui_row_timeline_message_location_icon_background",
    };

    static final int LINE_CLASSIC_GREEN = 0xFF8DE971;

    private static final int[] PRESET_COLORS = {
            0xFFD3E3FD, // Sky Mist
            0xFFC4EED0, // Mint Leaf
            0xFFEADDFF, // Lavender
            0xFFFFD8E4, // Rose Petal
            0xFFFFE0B2, // Warm Apricot
            LINE_CLASSIC_GREEN, // LINE Classic Green
            0xFF1A73E8, // Pixel Blue
            0xFF006D5B, // Deep Teal
            0xFF6750A4, // Royal Violet
            0xFFC2185B, // Berry Pink
            0xFF2B2D31, // Charcoal Glass
            0xFFFFFFFF, // Pure White
    };

    private static final int[] MY_SEED_PRESETS = {
            0xFF1A73E8, // Pixel Blue
            0xFF267D73, // Jade Teal
            0xFF2E7D32, // Forest Green
            0xFF6750A4, // Lavender Iris
            0xFFD85A88, // Sakura Rose
            0xFFD95D39, // Coral Sunset
            0xFFC68400, // Honey Amber
            0xFF00838F, // Ocean Cyan
            0xFF3949AB, // Royal Indigo
            0xFF8E24AA, // Berry Plum
            0xFF6D4C41, // Warm Mocha
            0xFF5F6368, // Monochrome Slate
    };

    private static final String CHAT_SETTINGS_ROW_TAG = "andrew_chat_setting_bubble_row";

    private static final Map<View, Integer> BUBBLE_APPLIED = new WeakHashMap<>();
    private static final Map<View, Drawable> BUBBLE_BG_REF = new WeakHashMap<>();
    private static final Map<View, int[]> ORIG_BUBBLE_PADDING = new WeakHashMap<>();
    private static final Map<TextView, ColorStateList> ORIG_BUBBLE_TEXT_COLORS = new WeakHashMap<>();
    private static final Map<TextView, ColorStateList> ORIG_BUBBLE_LINK_COLORS = new WeakHashMap<>();
    private static final Map<View, int[]> ORIG_BADGE_PADDING = new WeakHashMap<>();
    private static final Map<View, Integer> NAV_APPLIED = new WeakHashMap<>();
    private static final class NavExpandState {
        int extendedBottom;
        int setMarginBottom = Integer.MIN_VALUE;
    }

    private static final class ConstraintSaveState {
        Field botToTopField;
        int origBotToTop;
        Field botToBotField;
        int origBotToBot;
        int origHeight;
        int origBottomMargin;
        boolean explicitHeightMode;
    }

    private static final class LinearSaveState {
        int origHeight;
        float origWeight;
        WeakReference<View> trailingBarRef;
        int origTrailingTopMargin;
    }

    private static final Map<View, int[]> ORIG_NAV_PADDING = new WeakHashMap<>();
    private static final Map<View, Integer> ORIG_NAV_HEIGHT = new WeakHashMap<>();
    private static final Map<View, Integer> ORIG_NAV_LIST_PAD = new WeakHashMap<>();
    private static final Map<View, Integer> ORIG_NAV_FRAG_MARGIN = new WeakHashMap<>();
    private static final Map<View, NavExpandState> NAV_EXPAND_STATES = new WeakHashMap<>();
    private static final Map<View, ConstraintSaveState> NAV_CONSTRAINT_STATES = new WeakHashMap<>();
    private static final Map<View, LinearSaveState> NAV_LINEAR_STATES = new WeakHashMap<>();
    private static final Map<View, Integer> ORIG_NAV_CONTAINER_PAD_BOT = new WeakHashMap<>();
    private static final Map<View, Drawable> ORIG_NAV_CLEARED_BG = new WeakHashMap<>();
    private static final Map<ImageView, Integer> ORIG_NAV_IMAGE_ALPHA = new WeakHashMap<>();
    private static final Map<View, Float> ORIG_NAV_SIBLING_ALPHA = new WeakHashMap<>();
    private static final Map<TextView, Typeface> ORIG_NAV_TEXT_TYPEFACE = new WeakHashMap<>();
    private static final Map<View, float[]> ORIG_NAV_BADGE_TRANSFORM = new WeakHashMap<>();
    private static final Map<View, Float> ORIG_NAV_TAB_TX = new WeakHashMap<>();
    private static final Map<View, Boolean> COMPOSE_INSETS_STRIPPED = new WeakHashMap<>();
    private static final Map<View, Boolean> CONTENT_BG_APPLIED = new WeakHashMap<>();
    private static final Map<View, WeakReference<View>> NAV_SCROLL_HOST = new WeakHashMap<>();
    private static final Map<View, ViewTreeObserver.OnScrollChangedListener> NAV_SCROLL_LISTENERS = new WeakHashMap<>();
    private static final Map<View, ViewTreeObserver.OnPreDrawListener> NAV_PREDRAW_LISTENERS = new WeakHashMap<>();
    private static final Map<View, Integer> BADGE_APPLIED = new WeakHashMap<>();
    private static final Map<View, String> CHAT_IDS = new WeakHashMap<>();
    private static final Map<View, WeakReference<Activity>> DECOR_ACTIVITIES = new WeakHashMap<>();
    private static final Map<String, String> TITLE_TO_CHAT_ID = new ConcurrentHashMap<>();
    private static final Map<View, WallpaperPalette> SKIN_WALLPAPER_CACHE = new WeakHashMap<>();

    // Fast O(1) caches for Jetpack Compose color & theme-element hooks
    private static final Map<Integer, Integer> COMPOSE_RES_KIND = new ConcurrentHashMap<>();
    private static final Map<Set<?>, Integer> COMPOSE_SET_KIND = new ConcurrentHashMap<>();
    private static final int[][] BUBBLE_STATE_SETS = {
            new int[]{android.R.attr.state_pressed},
            new int[]{android.R.attr.state_selected},
            new int[]{android.R.attr.state_activated},
            new int[]{}
    };
    private static final Map<Integer, ColorStateList> BUBBLE_CSL_CACHE = new ConcurrentHashMap<>();

    private static volatile Context appContext;
    private static volatile Boolean lastActivityDark;
    private static volatile String lastActiveChatId = "";
    private static volatile String lastActiveChatTitle = "";
    private static volatile WallpaperPalette lastActiveWallpaperPalette;

    private MaterialYou() {}

    static void init(Application application) {
        if (application != null) {
            appContext = application.getApplicationContext();
        }
    }

    static void registerActivity(View decorView, Activity activity) {
        if (decorView != null && activity != null) {
            DECOR_ACTIVITIES.put(decorView, new WeakReference<>(activity));
        }
    }

    static boolean anyEnabled() {
        return LineSettings.materialYouTheme()
                || LineSettings.materialYouNavBar()
                || LineSettings.navBarPill()
                || LineSettings.chatBubbleWallpaper()
                || LineSettings.chatBubbleDefaultMode() != LineSettings.DEFAULT_BUBBLE_LINE_CLASSIC
                || LineSettings.chatHeaderWhite()
                || LineSettings.pixelFoldAdaptive()
                || LineSettings.hasAnyChatBubbleColors();
    }

    static void syncActivityUiMode(View view) {
        if (view != null) {
            lastActivityDark = isNightResources(view.getResources());
        }
    }

    private static boolean isNightResources(Resources res) {
        return res != null
                && (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static void onConfigurationChanged() {
        Palette.cachedLight = null;
        Palette.cachedDark = null;
        BUBBLE_APPLIED.clear();
        NAV_APPLIED.clear();
        BADGE_APPLIED.clear();
        SKIN_WALLPAPER_CACHE.clear();
    }

    static void onActivityResumed(View decorView) {
        syncActivityUiMode(decorView);
        BUBBLE_APPLIED.clear();
        BUBBLE_BG_REF.clear();
        NAV_APPLIED.clear();
        BADGE_APPLIED.clear();
        if (decorView != null) {
            CHAT_IDS.remove(decorView);
        }
    }

    static void onActivityDestroyed(View decorView) {
        if (decorView == null) {
            return;
        }
        CHAT_IDS.remove(decorView);
        DECOR_ACTIVITIES.remove(decorView);
        NAV_SCROLL_HOST.remove(decorView);
        ViewTreeObserver.OnScrollChangedListener scrollListener = NAV_SCROLL_LISTENERS.remove(decorView);
        if (scrollListener != null) {
            ViewTreeObserver vto = decorView.getViewTreeObserver();
            if (vto != null && vto.isAlive()) {
                vto.removeOnScrollChangedListener(scrollListener);
            }
        }
    }

    static final class Palette {
        final boolean dark;
        final int accent;
        final int pillBg;
        final int onPill;
        final int sendBubble;
        final int receiveBubble;
        final int navBarBg;
        final int navSelected;
        final int navUnselected;
        final int badgeBg;
        final int onBadge;
        final int blurSurface;

        private Palette(
                boolean dark,
                int accent,
                int pillBg,
                int onPill,
                int sendBubble,
                int receiveBubble,
                int navBarBg,
                int navSelected,
                int navUnselected,
                int badgeBg,
                int onBadge,
                int blurSurface
        ) {
            this.dark = dark;
            this.accent = accent;
            this.pillBg = pillBg;
            this.onPill = onPill;
            this.sendBubble = sendBubble;
            this.receiveBubble = receiveBubble;
            this.navBarBg = navBarBg;
            this.navSelected = navSelected;
            this.navUnselected = navUnselected;
            this.badgeBg = badgeBg;
            this.onBadge = onBadge;
            this.blurSurface = blurSurface;
        }

        static volatile Palette cachedLight;
        static volatile Palette cachedDark;
        static volatile int cachedKeyColor;
        static volatile int cachedCustomSeed;

        static Palette of(Context context) {
            boolean dark;
            if (context != null && context != appContext) {
                dark = isNightResources(context.getResources());
                if (activityOf(context) != null) {
                    lastActivityDark = dark;
                }
            } else {
                Boolean actDark = lastActivityDark;
                dark = actDark != null ? actDark : (context != null && isNightResources(context.getResources()));
            }
            return forMode(context != null ? context : appContext, dark);
        }

        static Palette of(Resources res) {
            boolean dark;
            if (res != null) {
                dark = isNightResources(res);
                lastActivityDark = dark;
            } else {
                Boolean actDark = lastActivityDark;
                dark = actDark != null && actDark;
            }
            return forMode(appContext, dark);
        }

        static Palette forCurrentUi() {
            Boolean actDark = lastActivityDark;
            boolean dark = actDark != null
                    ? actDark
                    : (appContext != null && isNightResources(appContext.getResources()));
            return forMode(appContext, dark);
        }

        private static Palette forMode(Context context, boolean dark) {
            int customSeed = LineSettings.materialYouSeedColor();
            if (customSeed != 0) {
                if (cachedCustomSeed != customSeed) {
                    cachedLight = null;
                    cachedDark = null;
                    cachedCustomSeed = customSeed;
                    cachedKeyColor = 0;
                }
                Palette hit = dark ? cachedDark : cachedLight;
                if (hit != null) {
                    return hit;
                }
                Palette created = fromSeed(customSeed, dark);
                if (dark) {
                    cachedDark = created;
                } else {
                    cachedLight = created;
                }
                return created;
            }
            if (cachedCustomSeed != 0) {
                cachedLight = null;
                cachedDark = null;
                cachedCustomSeed = 0;
            }
            if (context == null) {
                context = appContext;
            }
            if (context != null && Build.VERSION.SDK_INT >= 31) {
                try {
                    int a1_500 = context.getColor(android.R.color.system_accent1_500);
                    if (cachedKeyColor != a1_500) {
                        cachedLight = null;
                        cachedDark = null;
                        cachedKeyColor = a1_500;
                    }
                    Palette hit = dark ? cachedDark : cachedLight;
                    if (hit != null) {
                        return hit;
                    }
                    Palette created = systemPalette(context, dark);
                    if (dark) {
                        cachedDark = created;
                    } else {
                        cachedLight = created;
                    }
                    return created;
                } catch (Throwable ignored) {
                }
            }
            if (cachedKeyColor != 0) {
                cachedLight = null;
                cachedDark = null;
                cachedKeyColor = 0;
            }
            Palette hit = dark ? cachedDark : cachedLight;
            if (hit != null) {
                return hit;
            }
            Palette created = systemPalette(context, dark);
            if (dark) {
                cachedDark = created;
            } else {
                cachedLight = created;
            }
            return created;
        }

        static Palette systemPalette(Context context, boolean dark) {
            if (context == null) {
                context = appContext;
            }
            if (context != null && Build.VERSION.SDK_INT >= 31) {
                try {
                    int a1_500 = context.getColor(android.R.color.system_accent1_500);
                    if (dark) {
                        int a1_200 = context.getColor(android.R.color.system_accent1_200);
                        int a1_700 = context.getColor(android.R.color.system_accent1_700);
                        int a1_800 = context.getColor(android.R.color.system_accent1_800);
                        int a1_900 = context.getColor(android.R.color.system_accent1_900);
                        int a3_200 = context.getColor(android.R.color.system_accent3_200);
                        int n1_900 = context.getColor(android.R.color.system_neutral1_900);
                        int n2_300 = context.getColor(android.R.color.system_neutral2_300);
                        int n2_800 = context.getColor(android.R.color.system_neutral2_800);
                        int navBg = blend(n1_900, a1_800, 0.24f);
                        int blur = blend(n1_900, a1_900, 0.35f);
                        int hiBg = tuneDarkHighlight(a1_200, a3_200);
                        int onHi = tuneDarkOnHighlight(a1_900);
                        return new Palette(true, hiBg, hiBg, onHi, a1_700, n2_800, navBg, hiBg, n2_300, hiBg, onHi, blur);
                    } else {
                        int a1_100 = context.getColor(android.R.color.system_accent1_100);
                        int a1_600 = context.getColor(android.R.color.system_accent1_600);
                        int a2_50 = context.getColor(android.R.color.system_accent2_50);
                        int a2_100 = context.getColor(android.R.color.system_accent2_100);
                        int a3_600 = context.getColor(android.R.color.system_accent3_600);
                        int n1_50 = context.getColor(android.R.color.system_neutral1_50);
                        int n2_500 = context.getColor(android.R.color.system_neutral2_500);
                        int navBg = blend(n1_50, a2_100, 0.55f);
                        int recv = blend(n1_50, a2_50, 0.65f);
                        int pillBg = tuneLightHighlight(a1_600, a3_600);
                        int badgeBg = tuneLightHighlight(a1_500, a3_600);
                        return new Palette(false, pillBg, pillBg, 0xFFFFFFFF, a1_100, recv, navBg, pillBg, n2_500, badgeBg, 0xFFFFFFFF, navBg);
                    }
                } catch (Throwable ignored) {
                }
            }
            return dark
                    ? new Palette(true, 0xFFA8C7FA, 0xFFA8C7FA, 0xFF062E6F, 0xFF0842A0, 0xFF282A2F, 0xFF1B2029, 0xFFA8C7FA, 0xFFB0B5BE, 0xFF8AB4F8, 0xFF062E6F, 0xFF181C24)
                    : new Palette(false, 0xFF1A73E8, 0xFF1A73E8, 0xFFFFFFFF, 0xFFD3E3FD, 0xFFF0F4FA, 0xFFEBF2FC, 0xFF1A73E8, 0xFF5F6368, 0xFF1A73E8, 0xFFFFFFFF, 0xFFEBF2FC);
        }

        static Palette fromSeed(int seedArgb, boolean dark) {
            float[] hsv = new float[3];
            Color.colorToHSV(seedArgb | 0xFF000000, hsv);
            float h = hsv[0];
            float s = hsv[1];
            float tHue = (h + 52.0f) % 360.0f;
            boolean mono = s < 0.05f;
            if (dark) {
                if (mono) {
                    return new Palette(
                            true, 0xFFE2E2E6, 0xFFE2E2E6, 0xFF1A1C1E,
                            0xFF3E4044, 0xFF282A2E, 0xFF1E2024,
                            0xFFE2E2E6, 0xFFC4C6CA, 0xFFE2E2E6, 0xFF1A1C1E, 0xFF191B1F
                    );
                }
                int a1_200 = Color.HSVToColor(new float[]{h, Math.min(0.46f, Math.max(0.24f, s * 0.56f)), 0.94f}) | 0xFF000000;
                int a3_200 = Color.HSVToColor(new float[]{tHue, Math.min(0.42f, Math.max(0.20f, s * 0.50f)), 0.92f}) | 0xFF000000;
                int a1_700 = Color.HSVToColor(new float[]{h, Math.min(0.76f, Math.max(0.38f, s * 0.85f)), 0.52f}) | 0xFF000000;
                int a1_800 = Color.HSVToColor(new float[]{h, Math.min(0.78f, Math.max(0.38f, s * 0.86f)), 0.36f}) | 0xFF000000;
                int a1_900 = Color.HSVToColor(new float[]{h, Math.min(0.82f, Math.max(0.42f, s * 0.90f)), 0.22f}) | 0xFF000000;
                int n1_900 = Color.HSVToColor(new float[]{h, Math.min(0.10f, s * 0.14f), 0.12f}) | 0xFF000000;
                int n2_300 = Color.HSVToColor(new float[]{h, Math.min(0.14f, s * 0.22f), 0.78f}) | 0xFF000000;
                int n2_800 = Color.HSVToColor(new float[]{h, Math.min(0.16f, s * 0.24f), 0.20f}) | 0xFF000000;
                int navBg = blend(n1_900, a1_800, 0.24f);
                int blur = blend(n1_900, a1_900, 0.35f);
                int hiBg = tuneDarkHighlight(a1_200, a3_200);
                int onHi = tuneDarkOnHighlight(a1_900);
                return new Palette(true, hiBg, hiBg, onHi, a1_700, n2_800, navBg, hiBg, n2_300, hiBg, onHi, blur);
            } else {
                if (mono) {
                    return new Palette(
                            false, 0xFF2E3135, 0xFF2E3135, 0xFFFFFFFF,
                            0xFFE2E4E9, 0xFFF3F4F7, 0xFFECEEF2,
                            0xFF2E3135, 0xFF5F6368, 0xFF2E3135, 0xFFFFFFFF, 0xFFECEEF2
                    );
                }
                int a1_100 = Color.HSVToColor(new float[]{h, Math.min(0.26f, Math.max(0.12f, s * 0.30f)), 0.97f}) | 0xFF000000;
                int a1_500 = Color.HSVToColor(new float[]{h, Math.min(0.84f, Math.max(0.42f, s * 0.92f)), 0.68f}) | 0xFF000000;
                int a1_600 = Color.HSVToColor(new float[]{h, Math.min(0.86f, Math.max(0.45f, s * 0.95f)), 0.56f}) | 0xFF000000;
                int a2_50 = Color.HSVToColor(new float[]{h, Math.min(0.08f, Math.max(0.03f, s * 0.10f)), 0.99f}) | 0xFF000000;
                int a2_100 = Color.HSVToColor(new float[]{h, Math.min(0.16f, Math.max(0.07f, s * 0.20f)), 0.96f}) | 0xFF000000;
                int a3_600 = Color.HSVToColor(new float[]{tHue, Math.min(0.80f, Math.max(0.38f, s * 0.88f)), 0.56f}) | 0xFF000000;
                int n1_50 = Color.HSVToColor(new float[]{h, Math.min(0.03f, s * 0.05f), 0.99f}) | 0xFF000000;
                int n2_500 = Color.HSVToColor(new float[]{h, Math.min(0.14f, s * 0.20f), 0.44f}) | 0xFF000000;
                int navBg = blend(n1_50, a2_100, 0.55f);
                int recv = blend(n1_50, a2_50, 0.65f);
                int pillBg = tuneLightHighlight(a1_600, a3_600);
                int badgeBg = tuneLightHighlight(a1_500, a3_600);
                return new Palette(false, pillBg, pillBg, 0xFFFFFFFF, a1_100, recv, navBg, pillBg, n2_500, badgeBg, 0xFFFFFFFF, navBg);
            }
        }

        private static int tuneLightHighlight(int primary, int tertiary) {
            float[] hsv = new float[3];
            Color.colorToHSV(primary | 0xFF000000, hsv);
            if (hsv[1] < 0.05f && tertiary != 0) {
                float[] tHsv = new float[3];
                Color.colorToHSV(tertiary | 0xFF000000, tHsv);
                if (tHsv[1] >= 0.05f) {
                    hsv[0] = tHsv[0];
                    hsv[1] = tHsv[1];
                }
            }
            if (hsv[1] >= 0.04f) {
                if (hsv[1] < 0.34f) {
                    hsv[1] = Math.min(0.38f, hsv[1] * 2.2f + 0.11f);
                }
                hsv[2] = Math.min(hsv[2], 0.56f);
                int candidate = Color.HSVToColor(hsv) | 0xFF000000;
                while (Color.luminance(candidate) > 0.16f && hsv[2] > 0.18f) {
                    hsv[2] -= 0.03f;
                    candidate = Color.HSVToColor(hsv) | 0xFF000000;
                }
                return candidate;
            }
            // Pure greyscale wallpaper: crisp neutral-800 charcoal so #FFFFFF text exceeds 9:1 contrast
            hsv[2] = Math.min(hsv[2], 0.24f);
            int candidate = Color.HSVToColor(hsv) | 0xFF000000;
            while (Color.luminance(candidate) > 0.12f && hsv[2] > 0.14f) {
                hsv[2] -= 0.03f;
                candidate = Color.HSVToColor(hsv) | 0xFF000000;
            }
            return candidate;
        }

        private static int tuneDarkHighlight(int primary, int tertiary) {
            float[] hsv = new float[3];
            Color.colorToHSV(primary | 0xFF000000, hsv);
            if (hsv[1] < 0.05f && tertiary != 0) {
                float[] tHsv = new float[3];
                Color.colorToHSV(tertiary | 0xFF000000, tHsv);
                if (tHsv[1] >= 0.05f) {
                    hsv[0] = tHsv[0];
                    hsv[1] = tHsv[1];
                }
            }
            if (hsv[1] >= 0.04f && hsv[1] < 0.22f) {
                hsv[1] = Math.min(0.28f, hsv[1] * 1.8f + 0.06f);
            }
            hsv[2] = Math.max(hsv[2], 0.84f);
            int candidate = Color.HSVToColor(hsv) | 0xFF000000;
            while (Color.luminance(candidate) < 0.48f && hsv[2] < 0.96f) {
                hsv[2] += 0.03f;
                candidate = Color.HSVToColor(hsv) | 0xFF000000;
            }
            return candidate;
        }

        private static int tuneDarkOnHighlight(int darkTone) {
            float[] hsv = new float[3];
            Color.colorToHSV(darkTone | 0xFF000000, hsv);
            hsv[2] = Math.min(hsv[2], 0.22f);
            int candidate = Color.HSVToColor(hsv) | 0xFF000000;
            while (Color.luminance(candidate) > 0.045f && hsv[2] > 0.08f) {
                hsv[2] -= 0.03f;
                candidate = Color.HSVToColor(hsv) | 0xFF000000;
            }
            return candidate;
        }
    }

    /**
     * Synthesizes Material-3-harmonized sent &amp; received bubble colors from the chat room's
     * background wallpaper ({@code ChatHistorySkinImageView}), cached per drawable &amp; bitmap generation.
     */
    static final class WallpaperPalette {
        final WeakReference<Drawable> drawableRef;
        final int bitmapGenId;
        final int viewWidth;
        final int viewHeight;
        final boolean dark;
        final boolean valid;
        final int seedAccent;
        final int sendBubble;
        final int receiveBubble;

        private WallpaperPalette(
                Drawable drawableRef,
                int bitmapGenId,
                int viewWidth,
                int viewHeight,
                boolean dark,
                boolean valid,
                int seedAccent,
                int sendBubble,
                int receiveBubble
        ) {
            this.drawableRef = new WeakReference<>(drawableRef);
            this.bitmapGenId = bitmapGenId;
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;
            this.dark = dark;
            this.valid = valid;
            this.seedAccent = seedAccent;
            this.sendBubble = sendBubble;
            this.receiveBubble = receiveBubble;
        }

        static WallpaperPalette fromSkin(View skinView, boolean dark) {
            if (skinView == null || skinView.getVisibility() != View.VISIBLE) {
                return null;
            }
            if (!BlurBars.hasCoveringArtwork(skinView)) {
                SKIN_WALLPAPER_CACHE.remove(skinView);
                return null;
            }
            Drawable d = skinView instanceof ImageView ? ((ImageView) skinView).getDrawable() : null;
            if (d == null) {
                d = skinView.getBackground();
            }
            if (d == null || d instanceof NinePatchDrawable) {
                SKIN_WALLPAPER_CACHE.remove(skinView);
                return null;
            }
            int w = skinView.getWidth();
            int h = skinView.getHeight();
            if (w <= 0 || h <= 0) {
                return null;
            }
            Bitmap rawBmp = d instanceof BitmapDrawable ? ((BitmapDrawable) d).getBitmap() : null;
            int genId = (rawBmp != null && !rawBmp.isRecycled()) ? rawBmp.getGenerationId() : 0;

            WallpaperPalette cached = SKIN_WALLPAPER_CACHE.get(skinView);
            if (cached != null
                    && cached.drawableRef.get() == d
                    && cached.bitmapGenId == genId
                    && cached.viewWidth == w
                    && cached.viewHeight == h
                    && cached.dark == dark) {
                return cached.valid ? cached : null;
            }

            int[] colors = extractHarmonizedColors(d, w, h, dark);
            WallpaperPalette result;
            if (colors != null) {
                result = new WallpaperPalette(d, genId, w, h, dark, true, colors[0], colors[1], colors[2]);
            } else {
                result = new WallpaperPalette(d, genId, w, h, dark, false, 0, 0, 0);
            }
            SKIN_WALLPAPER_CACHE.put(skinView, result);
            return result.valid ? result : null;
        }

        private static int[] extractHarmonizedColors(Drawable drawable, int width, int height, boolean dark) {
            final int grid = 28;
            Bitmap bmp = Bitmap.createBitmap(grid, grid, Bitmap.Config.ARGB_8888);
            try {
                Canvas canvas = new Canvas(bmp);
                if (!drawDrawableSafe(canvas, drawable, width, height, grid)) {
                    return null;
                }
                int[] pixels = new int[grid * grid];
                bmp.getPixels(pixels, 0, grid, 0, 0, grid, grid);

                float[] histWeight = new float[36];
                float[] histSat = new float[36];
                float[] hsv = new float[3];
                long rSum = 0;
                long gSum = 0;
                long bSum = 0;
                int opaqueCount = 0;
                float totalChromaWeight = 0f;

                for (int px : pixels) {
                    int a = (px >>> 24) & 0xFF;
                    if (a < 96) {
                        continue;
                    }
                    opaqueCount++;
                    int r = (px >> 16) & 0xFF;
                    int g = (px >> 8) & 0xFF;
                    int b = px & 0xFF;
                    rSum += r;
                    gSum += g;
                    bSum += b;

                    Color.RGBToHSV(r, g, b, hsv);
                    float s = hsv[1];
                    float v = hsv[2];
                    if (s >= 0.07f && v >= 0.08f) {
                        int bin = ((int) (hsv[0] / 10.0f)) % 36;
                        if (bin < 0) bin = 0;
                        float weight = s * (0.35f + 0.65f * v);
                        histWeight[bin] += weight;
                        histSat[bin] += s * weight;
                        totalChromaWeight += weight;
                    }
                }

                if (opaqueCount < (grid * grid) / 6) {
                    return null;
                }

                float peakHue;
                float repSat;
                if (totalChromaWeight >= 0.8f) {
                    int bestBin = 0;
                    float bestScore = -1f;
                    for (int i = 0; i < 36; i++) {
                        int prev = (i + 35) % 36;
                        int next = (i + 1) % 36;
                        float smooth = histWeight[prev] * 0.5f + histWeight[i] + histWeight[next] * 0.5f;
                        if (smooth > bestScore) {
                            bestScore = smooth;
                            bestBin = i;
                        }
                    }
                    int prev = (bestBin + 35) % 36;
                    int next = (bestBin + 1) % 36;
                    float wSum = histWeight[prev] + histWeight[bestBin] + histWeight[next];
                    float sSum = histSat[prev] + histSat[bestBin] + histSat[next];
                    peakHue = (bestBin * 10.0f + 5.0f) % 360.0f;
                    repSat = wSum > 0f ? (sSum / wSum) : 0.35f;
                } else {
                    int avgR = (int) (rSum / opaqueCount);
                    int avgG = (int) (gSum / opaqueCount);
                    int avgB = (int) (bSum / opaqueCount);
                    Color.RGBToHSV(avgR, avgG, avgB, hsv);
                    peakHue = hsv[0];
                    repSat = hsv[1];
                }

                int seedAccent;
                int sendBubble;
                int recvBubble;
                if (dark) {
                    float sendSat = totalChromaWeight >= 0.8f ? clamp(repSat * 0.65f, 0.28f, 0.56f) : clamp(repSat * 0.4f, 0.04f, 0.14f);
                    float recvSat = totalChromaWeight >= 0.8f ? clamp(repSat * 0.22f, 0.06f, 0.18f) : 0.04f;
                    seedAccent = Color.HSVToColor(new float[]{peakHue, clamp(repSat * 0.55f, 0.25f, 0.65f), 0.88f});
                    sendBubble = Color.HSVToColor(new float[]{peakHue, sendSat, 0.46f});
                    recvBubble = Color.HSVToColor(new float[]{peakHue, recvSat, 0.21f});
                } else {
                    float sendSat = totalChromaWeight >= 0.8f ? clamp(repSat * 0.46f, 0.18f, 0.36f) : clamp(repSat * 0.3f, 0.04f, 0.12f);
                    float recvSat = totalChromaWeight >= 0.8f ? clamp(repSat * 0.14f, 0.04f, 0.12f) : 0.02f;
                    seedAccent = Color.HSVToColor(new float[]{peakHue, clamp(repSat * 0.78f, 0.35f, 0.82f), 0.54f});
                    sendBubble = Color.HSVToColor(new float[]{peakHue, sendSat, 0.95f});
                    int tintedWhite = Color.HSVToColor(new float[]{peakHue, recvSat, 0.99f});
                    recvBubble = blend(0xFFFFFFFF, tintedWhite, 0.65f);
                }
                return new int[]{seedAccent | 0xFF000000, sendBubble | 0xFF000000, recvBubble | 0xFF000000};
            } catch (Throwable ignored) {
                return null;
            } finally {
                bmp.recycle();
            }
        }

        private static boolean drawDrawableSafe(Canvas canvas, Drawable drawable, int viewW, int viewH, int grid) {
            if (drawable instanceof ColorDrawable) {
                canvas.drawColor(((ColorDrawable) drawable).getColor());
                return true;
            }
            if (drawable instanceof BitmapDrawable) {
                Bitmap src = ((BitmapDrawable) drawable).getBitmap();
                if (src == null || src.isRecycled()) {
                    return false;
                }
                Bitmap sw = (Build.VERSION.SDK_INT >= 26 && src.getConfig() == Bitmap.Config.HARDWARE)
                        ? src.copy(Bitmap.Config.ARGB_8888, false)
                        : src;
                if (sw == null) {
                    return false;
                }
                try {
                    canvas.drawBitmap(sw, null, new Rect(0, 0, grid, grid), new Paint(Paint.FILTER_BITMAP_FLAG));
                    return true;
                } finally {
                    if (sw != src) {
                        sw.recycle();
                    }
                }
            }
            if (drawable instanceof LayerDrawable) {
                LayerDrawable ld = (LayerDrawable) drawable;
                boolean drew = false;
                for (int i = 0; i < ld.getNumberOfLayers(); i++) {
                    Drawable child = ld.getDrawable(i);
                    if (child != null && drawDrawableSafe(canvas, child, viewW, viewH, grid)) {
                        drew = true;
                    }
                }
                return drew;
            }
            int save = canvas.save();
            try {
                Rect b = drawable.getBounds();
                int bw = b.width() > 0 ? b.width() : viewW;
                int bh = b.height() > 0 ? b.height() : viewH;
                canvas.scale((float) grid / Math.max(1, bw), (float) grid / Math.max(1, bh));
                canvas.translate(-b.left, -b.top);
                drawable.draw(canvas);
                return true;
            } catch (Throwable ignored) {
                return false;
            } finally {
                canvas.restoreToCount(save);
            }
        }

        private static float clamp(float v, float min, float max) {
            return Math.max(min, Math.min(max, v));
        }
    }

    static int accentColor(Context context) {
        return Palette.of(context).accent;
    }

    static int blurSurfaceColor(Context context) {
        return Palette.of(context).blurSurface;
    }

    static int sendButtonAccentColor(Context context, boolean bottomDarkIcons) {
        Palette p = Palette.forMode(context, !bottomDarkIcons);
        return bottomDarkIcons ? p.pillBg : p.accent;
    }

    // =========================================================================
    // Jetpack Compose Unread Badge & Header / SubTab Pill Color Hooks
    // =========================================================================

    static int resolveComposeColor(Resources res, int resId, Resources.Theme theme) {
        int original = res.getColor(resId, theme);
        if (!LineSettings.materialYouTheme()) {
            return original;
        }
        Integer kind = COMPOSE_RES_KIND.get(resId);
        if (kind == null) {
            int classified = 0;
            try {
                String name = res.getResourceEntryName(resId);
                if ("primaryFill".equals(name)) {
                    classified = 1;
                } else if ("primaryFill_pressed".equals(name) || "linegreen_pressed".equals(name)) {
                    classified = 2;
                } else if ("onPrimaryAccentFill".equals(name)) {
                    classified = 3;
                }
            } catch (Throwable ignored) {
            }
            kind = classified;
            COMPOSE_RES_KIND.put(resId, kind);
        }
        if (kind == 0) {
            return original;
        }
        Palette palette = Palette.of(res);
        switch (kind) {
            case 1:
                return palette.badgeBg | 0xFF000000;
            case 2:
                return blend(palette.badgeBg, 0xFF000000, 0.14f) | 0xFF000000;
            case 3:
                return palette.onBadge | 0xFF000000;
            default:
                return original;
        }
    }

    static int overrideComposeThemeColor(Set<?> keys, int slot) {
        if (keys == null || !LineSettings.materialYouTheme()) {
            return 0;
        }
        Integer kind = COMPOSE_SET_KIND.get(keys);
        if (kind == null) {
            if (COMPOSE_SET_KIND.size() > 512) {
                COMPOSE_SET_KIND.clear();
            }
            int classified = 0;
            try {
                String desc = keys.toString();
                if (desc.contains("componentKey=navigationBar.subTab,")
                        && !desc.contains("unselected.")
                        && (desc.contains("elementKey=selected.background)")
                                || desc.contains("elementKey=selected.text)")
                                || desc.contains("elementKey=selected.icon)"))) {
                    classified = 10;
                } else if (desc.contains("componentKey=list.common.tab,")
                        && !desc.contains("unselected.")
                        && (desc.contains("elementKey=folder.selected.background)")
                                || desc.contains("elementKey=folder.selected.text)")
                                || desc.contains("elementKey=folder.selected.icon)"))) {
                    classified = 10;
                } else if (desc.contains("newDotIcon")
                        || (desc.contains("componentKey=list.common.tab,")
                                && desc.contains("elementKey=new.icon)"))) {
                    classified = 11;
                } else if (desc.contains("messageCountText")
                        || desc.contains("newbadge.")
                        || desc.contains("noti.background")
                        || desc.contains("noti.text")) {
                    classified = 12;
                }
            } catch (Throwable ignored) {
            }
            kind = classified;
            COMPOSE_SET_KIND.put(keys, kind);
        }
        if (kind == 0) {
            return 0;
        }
        // Slot mapping from com.linecorp.line.compose.theme.i:
        // 1 = icon/dot (h, i), 2 = background (b, f), 3 = stroke (c, d), 4 = text (j, k)
        Palette palette = Palette.forCurrentUi();
        switch (kind) {
            case 10:
                if (slot == 2) {
                    return palette.pillBg | 0xFF000000;
                }
                if (slot == 4 || slot == 1) {
                    return palette.onPill | 0xFF000000;
                }
                return 0;
            case 11:
                return (slot == 1 || slot == 2) ? (palette.badgeBg | 0xFF000000) : 0;
            case 12:
                if (slot == 2) {
                    return palette.badgeBg | 0xFF000000;
                }
                if (slot == 4 || slot == 1) {
                    return palette.onBadge | 0xFF000000;
                }
                return 0;
            default:
                return 0;
        }
    }

    static long argbToComposeColor(int argb) {
        return ((long) argb) << 32;
    }

    // =========================================================================
    // Bottom Navigation Bar & Unread Badges ("999+")
    // =========================================================================

    private static final class BadgePillDrawable extends GradientDrawable {
        int appliedColor;
        int appliedStrokeColor;
        int appliedStrokeWidth;
    }

    private static final float[] NAV_FLOAT_PAIR = new float[2];

    static void computeOuterPillHorizontalBounds(
            View decorView,
            float hostWidth,
            int tabCount,
            float density,
            float[] outLeftRight
    ) {
        int safeCount = Math.max(1, tabCount);
        if (hostWidth <= density * 560.0f) {
            float margin = density * 32.0f;
            if (decorView != null && Build.VERSION.SDK_INT >= 31) {
                WindowInsets rootInsets = decorView.getRootWindowInsets();
                if (rootInsets != null) {
                    RoundedCorner bl = rootInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT);
                    RoundedCorner br = rootInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT);
                    int cornerPx = Math.max(
                            bl != null ? bl.getRadius() : 0,
                            br != null ? br.getRadius() : 0
                    );
                    if (cornerPx > 0) {
                        margin = Math.min(density * 38.0f, Math.max(margin, cornerPx * 0.68f));
                    }
                }
            }
            outLeftRight[0] = margin;
            outLeftRight[1] = Math.max(margin + density * 120.0f, hostWidth - margin);
        } else {
            float maxPillW = Math.min(
                    hostWidth - density * 64.0f,
                    safeCount * density * 120.0f + density * 16.0f
            );
            float left = Math.max(density * 32.0f, (hostWidth - maxPillW) * 0.5f);
            outLeftRight[0] = left;
            outLeftRight[1] = Math.max(left + density * 120.0f, hostWidth - left);
        }
    }

    private static final class PillSlotTouchDelegate extends TouchDelegate {
        private final List<WeakReference<View>> targets = new ArrayList<>(6);
        private float trackLeft;
        private float trackRight;
        private WeakReference<View> activeTargetRef;

        PillSlotTouchDelegate(View host) {
            super(new Rect(), host);
        }

        void update(List<View> visibleTabs, float trackLeft, float trackRight) {
            this.trackLeft = trackLeft;
            this.trackRight = trackRight;
            boolean same = this.targets.size() == visibleTabs.size();
            if (same) {
                for (int i = 0; i < visibleTabs.size(); i++) {
                    View tab = visibleTabs.get(i);
                    View click = BlurBars.find(tab, "bnb_button_clickable_area");
                    View want = (click != null && click.isClickable()) ? click : tab;
                    if (this.targets.get(i).get() != want) {
                        same = false;
                        break;
                    }
                }
            }
            if (!same) {
                this.targets.clear();
                for (int i = 0; i < visibleTabs.size(); i++) {
                    View tab = visibleTabs.get(i);
                    View click = BlurBars.find(tab, "bnb_button_clickable_area");
                    View want = (click != null && click.isClickable()) ? click : tab;
                    this.targets.add(new WeakReference<>(want));
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int count = this.targets.size();
            if (count == 0 || this.trackRight <= this.trackLeft) {
                return false;
            }
            int action = event.getActionMasked();
            float x = event.getX();
            if (action == MotionEvent.ACTION_DOWN) {
                this.activeTargetRef = null;
                if (x < this.trackLeft || x > this.trackRight) {
                    return false;
                }
                float slotW = (this.trackRight - this.trackLeft) / count;
                int idx = Math.min(count - 1, Math.max(0, (int) ((x - this.trackLeft) / slotW)));
                View target = this.targets.get(idx).get();
                if (target == null || !target.isAttachedToWindow()) {
                    return false;
                }
                this.activeTargetRef = new WeakReference<>(target);
                return forwardCentered(target, event);
            }
            View active = this.activeTargetRef != null ? this.activeTargetRef.get() : null;
            if (active == null) {
                return false;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                this.activeTargetRef = null;
            }
            return forwardCentered(active, event);
        }

        private static boolean forwardCentered(View target, MotionEvent event) {
            MotionEvent copy = MotionEvent.obtain(event);
            try {
                copy.setLocation(target.getWidth() * 0.5f, target.getHeight() * 0.5f);
                return target.dispatchTouchEvent(copy);
            } finally {
                copy.recycle();
            }
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return (rgb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static int resolvePageBackgroundColor(Context ctx, Palette palette) {
        int fallback = palette.dark ? 0xFF111111 : 0xFFFFFFFF;
        if (ctx == null) {
            return fallback;
        }
        try {
            Resources res = ctx.getResources();
            int id = res.getIdentifier("primaryBackground", "color", ctx.getPackageName());
            if (id != 0) {
                int c = ctx.getColor(id) | 0xFF000000;
                if (isLightColor(c) != palette.dark) {
                    return c;
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static void syncSystemNavigationBarBackdrop(View decorView, View anchorView, int pageBgColor) {
        Activity act = activityOf(anchorView.getContext());
        if (act == null) {
            act = activityOf(decorView.getContext());
        }
        if (act == null) {
            return;
        }
        Window window = act.getWindow();
        if (window == null) {
            return;
        }
        try {
            if (window.getNavigationBarColor() != Color.TRANSPARENT) {
                window.setNavigationBarColor(Color.TRANSPARENT);
            }
            if (Build.VERSION.SDK_INT >= 29) {
                window.setNavigationBarContrastEnforced(false);
            }
            boolean lightNav = isLightColor(pageBgColor);
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController wic = window.getInsetsController();
                if (wic != null) {
                    int flag = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    wic.setSystemBarsAppearance(lightNav ? flag : 0, flag);
                }
            }
            if (Build.VERSION.SDK_INT >= 26) {
                int vis = decorView.getSystemUiVisibility();
                int nextVis = lightNav
                        ? (vis | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                        : (vis & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                if (nextVis != vis) {
                    decorView.setSystemUiVisibility(nextVis);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    static void applyBottomNavBar(View decorView) {
        if (decorView == null) {
            return;
        }
        syncActivityUiMode(decorView);
        boolean pillMode = LineSettings.navBarPill();
        boolean styleNavBar = LineSettings.materialYouNavBar();
        boolean styleBadges = pillMode || styleNavBar || LineSettings.materialYouTheme();
        if (!styleBadges) {
            return;
        }
        View bnbBg = BlurBars.find(decorView, "bnb_background_image");
        if (bnbBg == null || !bnbBg.isAttachedToWindow()) {
            return;
        }
        Context ctx = bnbBg.getContext() != null ? bnbBg.getContext() : decorView.getContext();
        Palette palette = Palette.of(ctx);

        List<View> visibleTabs = new ArrayList<>(6);
        for (String tabName : BNB_TABS) {
            View tab = BlurBars.find(decorView, tabName);
            if (tab != null && tab.getVisibility() == View.VISIBLE) {
                visibleTabs.add(tab);
            }
        }

        View sysNavBg = BlurBars.find(decorView, "main_navigation_bar_background");
        ViewGroup tabsParent = resolveDedicatedTabsRow(visibleTabs, ctx);
        View pillHost = tabsParent != null ? tabsParent : bnbBg;

        int selectedIndex = 0;
        for (int i = 0; i < visibleTabs.size(); i++) {
            if (isTabSelected(visibleTabs.get(i))) {
                selectedIndex = i;
                break;
            }
        }

        int activePillFill = 0;
        if (pillMode) {
            if (styleNavBar) {
                activePillFill = palette.dark
                        ? withAlpha(blend(palette.navBarBg, palette.accent, 0.36f), 0xDF)
                        : withAlpha(blend(palette.sendBubble, palette.accent, 0.14f), 0xE8);
            } else {
                activePillFill = palette.dark ? 0x26FFFFFF : 0x15000000;
            }
        }

        if (pillMode && !visibleTabs.isEmpty()) {
            applyFloatingPillNavBar(
                    decorView, ctx, bnbBg, sysNavBg, tabsParent, pillHost,
                    visibleTabs, selectedIndex, activePillFill, palette, styleNavBar
            );
            extendMainContentBehindPill(decorView, ctx, pillHost, bnbBg, sysNavBg, visibleTabs, true);
            ensureNavScrollBlurObserver(decorView, pillHost);
        } else {
            restoreNonPillNavBar(decorView, bnbBg, sysNavBg, tabsParent, visibleTabs);
            extendMainContentBehindPill(decorView, ctx, pillHost, bnbBg, sysNavBg, visibleTabs, false);
            if (styleNavBar) {
                Drawable bg = bnbBg.getBackground();
                if (bg != null && !(bg instanceof ColorDrawable)) {
                    Integer prev = NAV_APPLIED.get(bnbBg);
                    if (prev == null || prev != palette.navBarBg || bg.getColorFilter() == null) {
                        bg.mutate().setColorFilter(palette.navBarBg, PorterDuff.Mode.SRC_IN);
                        NAV_APPLIED.put(bnbBg, palette.navBarBg);
                    }
                } else {
                    int barHeight = BlurBars.dp(ctx, 49.0f);
                    ViewGroup.LayoutParams lp = bnbBg.getLayoutParams();
                    if (lp != null && lp.height != barHeight) {
                        lp.height = barHeight;
                        bnbBg.setLayoutParams(lp);
                    }
                    if (!(bg instanceof ColorDrawable) || ((ColorDrawable) bg).getColor() != palette.navBarBg) {
                        bnbBg.setBackground(new ColorDrawable(palette.navBarBg));
                    }
                }
                if (bnbBg instanceof ImageView) {
                    ImageView img = (ImageView) bnbBg;
                    if (img.getDrawable() != null) {
                        img.setColorFilter(palette.navBarBg, PorterDuff.Mode.SRC_IN);
                    }
                }

                if (sysNavBg != null) {
                    if (sysNavBg.getVisibility() != View.VISIBLE) {
                        sysNavBg.setVisibility(View.VISIBLE);
                    }
                    if (sysNavBg.getAlpha() != 1f) {
                        sysNavBg.setAlpha(1f);
                    }
                    if (!(sysNavBg.getBackground() instanceof ColorDrawable)
                            || ((ColorDrawable) sysNavBg.getBackground()).getColor() != palette.navBarBg) {
                        sysNavBg.setBackground(new ColorDrawable(palette.navBarBg));
                    }
                }
            }
        }

        for (int i = 0; i < visibleTabs.size(); i++) {
            View tab = visibleTabs.get(i);
            if (pillMode && tab instanceof ViewGroup) {
                ViewGroup tvg = (ViewGroup) tab;
                tvg.setClipChildren(false);
                tvg.setClipToPadding(false);
            }
            View clickArea = BlurBars.find(tab, "bnb_button_clickable_area");
            View iconView = BlurBars.find(tab, "bnb_button_image");
            View textView = BlurBars.find(tab, "bnb_button_text");
            boolean selected = isTabSelected(tab, clickArea, iconView, textView);

            if (styleNavBar || pillMode) {
                int tint;
                if (styleNavBar) {
                    if (pillMode) {
                        tint = selected
                                ? (palette.dark
                                        ? blend(palette.navSelected, 0xFFFFFFFF, 0.22f)
                                        : blend(palette.navSelected, 0xFF000000, 0.24f))
                                : (palette.dark
                                        ? withAlpha(palette.navUnselected, 0xCC)
                                        : blend(palette.navUnselected, 0xFF282E34, 0.38f));
                    } else {
                        tint = selected ? palette.navSelected : palette.navUnselected;
                    }
                } else {
                    tint = selected
                            ? (palette.dark ? 0xFFFFFFFF : 0xFF111417)
                            : (palette.dark ? 0xB3FFFFFF : 0xA81D2228);
                }

                if (iconView instanceof ImageView) {
                    ImageView iv = (ImageView) iconView;
                    Integer applied = NAV_APPLIED.get(iv);
                    if (applied == null || applied != tint || iv.getColorFilter() == null) {
                        iv.setImageTintList(ColorStateList.valueOf(tint));
                        iv.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
                        NAV_APPLIED.put(iv, tint);
                    }
                }
                if (textView instanceof TextView) {
                    TextView tv = (TextView) textView;
                    if (tv.getCurrentTextColor() != tint) {
                        tv.setTextColor(tint);
                    }
                    if (tv.getShadowRadius() > 0f) {
                        tv.setShadowLayer(0f, 0f, 0f, 0);
                    }
                    if (pillMode) {
                        Typeface baseTf = ORIG_NAV_TEXT_TYPEFACE.get(tv);
                        if (baseTf == null) {
                            baseTf = tv.getTypeface() != null ? tv.getTypeface() : Typeface.DEFAULT;
                            ORIG_NAV_TEXT_TYPEFACE.put(tv, baseTf);
                        }
                        int targetStyle = selected ? Typeface.BOLD : Typeface.NORMAL;
                        Typeface curTf = tv.getTypeface();
                        if (curTf == null || curTf.getStyle() != targetStyle) {
                            tv.setTypeface(Typeface.create(baseTf, targetStyle));
                        }
                    }
                }
            }
            if (pillMode) {
                boolean useMyBadge = LineSettings.materialYouTheme() || styleNavBar;
                styleTabBadgeInPill(BlurBars.find(tab, "bnb_button_badge_num"), palette, useMyBadge);
                styleTabBadgeInPill(BlurBars.find(tab, "default_theme_bnb_button_badge_num"), palette, useMyBadge);
            } else if (LineSettings.materialYouTheme() || styleNavBar) {
                tintBadge(BlurBars.find(tab, "bnb_button_badge_num"), palette);
                tintBadge(BlurBars.find(tab, "default_theme_bnb_button_badge_num"), palette);
            }
        }
    }

    private static boolean isTabSelected(View tab) {
        return isTabSelected(
                tab,
                BlurBars.find(tab, "bnb_button_clickable_area"),
                BlurBars.find(tab, "bnb_button_image"),
                BlurBars.find(tab, "bnb_button_text")
        );
    }

    private static boolean isTabSelected(View tab, View clickArea, View iconView, View textView) {
        return tab.isSelected()
                || (clickArea != null && clickArea.isSelected())
                || (iconView != null && iconView.isSelected())
                || (textView != null && textView.isSelected());
    }

    private static boolean isAnyTab(View v, List<View> visibleTabs) {
        for (int i = 0; i < visibleTabs.size(); i++) {
            if (visibleTabs.get(i) == v) {
                return true;
            }
        }
        return false;
    }

    private static ViewGroup resolveDedicatedTabsRow(List<View> visibleTabs, Context ctx) {
        if (visibleTabs.isEmpty()) {
            return null;
        }
        ViewParent firstParent = visibleTabs.get(0).getParent();
        if (!(firstParent instanceof ViewGroup)) {
            return null;
        }
        for (int i = 1; i < visibleTabs.size(); i++) {
            if (visibleTabs.get(i).getParent() != firstParent) {
                return null;
            }
        }
        ViewGroup vg = (ViewGroup) firstParent;
        int h = vg.getHeight();
        int maxBarH = BlurBars.dp(ctx, 130.0f);
        if (h <= 0 || h > maxBarH) {
            return null;
        }
        return vg;
    }

    private static void clearNavWrapperBackground(View v) {
        if (v == null) {
            return;
        }
        Drawable bg = v.getBackground();
        if (bg != null && !(bg instanceof FloatingNavPillDrawable)) {
            if (!ORIG_NAV_CLEARED_BG.containsKey(v)) {
                ORIG_NAV_CLEARED_BG.put(v, bg);
            }
            v.setBackground(null);
        }
    }

    private static ViewGroup clearBottomBarWrapperChain(
            View startView,
            View pillHost,
            List<View> visibleTabs,
            int maxBarWrapperH,
            int maxDividerH
    ) {
        if (startView == null) {
            return null;
        }
        ViewParent curParent = startView.getParent();
        int depth = 0;
        while (curParent instanceof ViewGroup && depth < 4) {
            ViewGroup vg = (ViewGroup) curParent;
            if (vg.getHeight() <= 0 || vg.getHeight() > maxBarWrapperH) {
                return vg;
            }
            vg.setClipChildren(false);
            vg.setClipToPadding(false);
            vg.setClipToOutline(false);
            if (vg != pillHost) {
                clearNavWrapperBackground(vg);
            }
            int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = vg.getChildAt(i);
                if (child == null
                        || child == pillHost
                        || BlurBars.isSelfOrAncestor(child, pillHost)
                        || isAnyTab(child, visibleTabs)) {
                    continue;
                }
                clearNavWrapperBackground(child);
                if (!(child instanceof ViewGroup)
                        && child.getHeight() > 0
                        && child.getHeight() <= maxDividerH
                        && child.getAlpha() > 0f) {
                    if (!ORIG_NAV_SIBLING_ALPHA.containsKey(child)) {
                        ORIG_NAV_SIBLING_ALPHA.put(child, child.getAlpha());
                    }
                    child.setAlpha(0f);
                }
            }
            curParent = vg.getParent();
            depth++;
        }
        return curParent instanceof ViewGroup ? (ViewGroup) curParent : null;
    }

    private static void applyFloatingPillNavBar(
            View decorView,
            Context ctx,
            View bnbBg,
            View sysNavBg,
            ViewGroup tabsParent,
            View pillHost,
            List<View> visibleTabs,
            int selectedIndex,
            int activePillFill,
            Palette palette,
            boolean styleNavBar
    ) {
        int pageBgColor = resolvePageBackgroundColor(ctx, palette);
        syncSystemNavigationBarBackdrop(decorView, bnbBg, pageBgColor);

        // Paint contentRoot at the back of the hierarchy so a black DecorView never shows on static screens.
        View contentRoot = decorView.findViewById(android.R.id.content);
        if (contentRoot != null) {
            Drawable cBg = contentRoot.getBackground();
            boolean needsBackdrop = cBg == null
                    || Boolean.TRUE.equals(CONTENT_BG_APPLIED.get(contentRoot))
                    || (!palette.dark && cBg instanceof ColorDrawable && ((ColorDrawable) cBg).getColor() == 0xFF000000);
            if (needsBackdrop && (!(cBg instanceof ColorDrawable) || ((ColorDrawable) cBg).getColor() != pageBgColor)) {
                contentRoot.setBackground(new ColorDrawable(pageBgColor));
                CONTENT_BG_APPLIED.put(contentRoot, Boolean.TRUE);
            }
        }
        ViewParent anc = pillHost.getParent();
        int fullScreenMinH = BlurBars.dp(ctx, 200.0f);
        while (anc instanceof ViewGroup && anc != contentRoot && anc != decorView) {
            ViewGroup avg = (ViewGroup) anc;
            if (avg.getHeight() >= fullScreenMinH) {
                Drawable aBg = avg.getBackground();
                if (Boolean.TRUE.equals(CONTENT_BG_APPLIED.get(avg))
                        || (!palette.dark && aBg instanceof ColorDrawable && ((ColorDrawable) aBg).getColor() == 0xFF000000)) {
                    if (!(aBg instanceof ColorDrawable) || ((ColorDrawable) aBg).getColor() != pageBgColor) {
                        avg.setBackground(new ColorDrawable(pageBgColor));
                        CONTENT_BG_APPLIED.put(avg, Boolean.TRUE);
                    }
                }
            }
            anc = avg.getParent();
        }

        // Keep main_navigation_bar_background transparent so Home and Chat content reach the bottom edge.
        if (sysNavBg != null) {
            clearNavWrapperBackground(sysNavBg);
            if (sysNavBg.getAlpha() > 0f) {
                if (!ORIG_NAV_SIBLING_ALPHA.containsKey(sysNavBg)) {
                    ORIG_NAV_SIBLING_ALPHA.put(sysNavBg, sysNavBg.getAlpha());
                }
                sysNavBg.setAlpha(0f);
            }
        }

        int pillBarHeight = BlurBars.dp(ctx, 56.0f);
        if (pillHost == tabsParent && tabsParent != bnbBg) {
            if (bnbBg.getBackground() instanceof FloatingNavPillDrawable) {
                bnbBg.setBackground(null);
            }
            clearNavWrapperBackground(bnbBg);
            if (bnbBg.getAlpha() != 0f) {
                bnbBg.setAlpha(0f);
            }
            if (bnbBg.getTranslationY() != 0f) {
                bnbBg.setTranslationY(0f);
            }
        } else {
            if (bnbBg.getAlpha() != 1f) {
                bnbBg.setAlpha(1f);
            }
            ViewGroup.LayoutParams bgLp = bnbBg.getLayoutParams();
            if (bgLp != null && bgLp.height != pillBarHeight) {
                if (!ORIG_NAV_HEIGHT.containsKey(bnbBg)) {
                    ORIG_NAV_HEIGHT.put(bnbBg, bgLp.height);
                }
                bgLp.height = pillBarHeight;
                bnbBg.setLayoutParams(bgLp);
            }
            if (bnbBg instanceof ImageView) {
                ImageView img = (ImageView) bnbBg;
                if (img.getImageAlpha() != 0) {
                    img.setImageAlpha(0);
                }
            }
        }

        if (pillHost.willNotDraw()) {
            pillHost.setWillNotDraw(false);
        }
        pillHost.setOutlineProvider(null);
        float pillZ = BlurBars.dp(ctx, 4.0f);
        if (pillHost.getTranslationZ() != pillZ) {
            pillHost.setTranslationZ(pillZ);
        }

        // Clear rectangular backgrounds along all bottom-bar branches.
        int maxBarWrapperH = BlurBars.dp(ctx, 165.0f);
        int maxDividerH = BlurBars.dp(ctx, 110.0f);
        View mainTabContainer = BlurBars.find(decorView, "main_tab_container");
        View viewPager = BlurBars.find(decorView, "viewpager");
        if (mainTabContainer != null && mainTabContainer != pillHost) {
            clearNavWrapperBackground(mainTabContainer);
            if (mainTabContainer instanceof ViewGroup) {
                ViewGroup mtVg = (ViewGroup) mainTabContainer;
                mtVg.setClipChildren(false);
                mtVg.setClipToPadding(false);
                mtVg.setClipToOutline(false);
            }
        }
        if (pillHost instanceof ViewGroup) {
            ViewGroup hostVg = (ViewGroup) pillHost;
            hostVg.setClipChildren(false);
            hostVg.setClipToPadding(false);
            hostVg.setClipToOutline(false);
        }
        ViewGroup rootA = clearBottomBarWrapperChain(pillHost, pillHost, visibleTabs, maxBarWrapperH, maxDividerH);
        ViewGroup rootB = clearBottomBarWrapperChain(bnbBg, pillHost, visibleTabs, maxBarWrapperH, maxDividerH);
        clearBottomBarWrapperChain(mainTabContainer, pillHost, visibleTabs, maxBarWrapperH, maxDividerH);
        clearBottomBarWrapperChain(sysNavBg, pillHost, visibleTabs, maxBarWrapperH, maxDividerH);

        ViewGroup barParentRoot = rootA != null ? rootA : rootB;
        if (barParentRoot != null && barParentRoot.getHeight() > maxBarWrapperH) {
            barParentRoot.setClipChildren(false);
            barParentRoot.setClipToPadding(false);
            int rH = barParentRoot.getHeight();
            int count = barParentRoot.getChildCount();
            for (int i = 0; i < count; i++) {
                View sib = barParentRoot.getChildAt(i);
                if (sib == null
                        || sib == pillHost
                        || BlurBars.isSelfOrAncestor(sib, pillHost)
                        || isAnyTab(sib, visibleTabs)) {
                    continue;
                }
                int sH = sib.getHeight();
                if (sH > 0 && sH <= maxBarWrapperH && sib.getBottom() >= rH - maxBarWrapperH) {
                    clearNavWrapperBackground(sib);
                    if (!(sib instanceof ViewGroup) && sib.getAlpha() > 0f) {
                        if (!ORIG_NAV_SIBLING_ALPHA.containsKey(sib)) {
                            ORIG_NAV_SIBLING_ALPHA.put(sib, sib.getAlpha());
                        }
                        sib.setAlpha(0f);
                    }
                }
            }
        }

        if (tabsParent != null) {
            int[] origPad = ORIG_NAV_PADDING.get(tabsParent);
            if (origPad != null && (tabsParent.getPaddingLeft() != origPad[0] || tabsParent.getPaddingRight() != origPad[2])) {
                tabsParent.setPadding(origPad[0], origPad[1], origPad[2], origPad[3]);
            }
        }

        float density = ctx.getResources().getDisplayMetrics().density;
        int hostWidth = pillHost.getWidth() > 0
                ? pillHost.getWidth()
                : (decorView.getWidth() > 0 ? decorView.getWidth() : ctx.getResources().getDisplayMetrics().widthPixels);
        int tabCount = visibleTabs.size();
        computeOuterPillHorizontalBounds(decorView, hostWidth, tabCount, density, NAV_FLOAT_PAIR);
        float pillLeft = NAV_FLOAT_PAIR[0];
        float pillRight = NAV_FLOAT_PAIR[1];
        float concentricGap = density * 5.0f;
        float trackLeft = pillLeft + concentricGap;
        float trackRight = Math.max(trackLeft + density * 60.0f, pillRight - concentricGap);
        float slotW = (trackRight - trackLeft) / Math.max(1, tabCount);

        // Align every tab to the exact center of its equal Material Design 3 toolbar slot.
        if (pillHost.getWidth() > 0) {
            pillHost.getLocationInWindow(NAV_LOC_A);
            for (int i = 0; i < tabCount; i++) {
                View tab = visibleTabs.get(i);
                if (tab == null || tab.getWidth() <= 0) {
                    continue;
                }
                if (!ORIG_NAV_TAB_TX.containsKey(tab)) {
                    ORIG_NAV_TAB_TX.put(tab, tab.getTranslationX());
                }
                tab.getLocationInWindow(NAV_LOC_B);
                float naturalWinCenterX = (NAV_LOC_B[0] - tab.getTranslationX()) + tab.getWidth() * 0.5f;
                float targetWinCenterX = NAV_LOC_A[0] + trackLeft + (i + 0.5f) * slotW;
                float desiredTx = targetWinCenterX - naturalWinCenterX;
                if (Math.abs(tab.getTranslationX() - desiredTx) > 0.5f) {
                    tab.setTranslationX(desiredTx);
                }
            }
            TouchDelegate existingTd = pillHost.getTouchDelegate();
            PillSlotTouchDelegate slotTd;
            if (existingTd instanceof PillSlotTouchDelegate) {
                slotTd = (PillSlotTouchDelegate) existingTd;
            } else {
                slotTd = new PillSlotTouchDelegate(pillHost);
                pillHost.setTouchDelegate(slotTd);
            }
            slotTd.update(visibleTabs, trackLeft, trackRight);
        }

        float liftY = -BlurBars.dp(ctx, 2.0f);
        if (pillHost.getTranslationY() != liftY) {
            pillHost.setTranslationY(liftY);
        }

        Drawable curBg = pillHost.getBackground();
        FloatingNavPillDrawable pillDrawable;
        if (curBg instanceof FloatingNavPillDrawable) {
            pillDrawable = (FloatingNavPillDrawable) curBg;
        } else {
            pillDrawable = new FloatingNavPillDrawable(pillHost, decorView);
            pillHost.setBackground(pillDrawable);
        }
        int baseSurface;
        int rimAccent;
        if (styleNavBar) {
            baseSurface = palette.dark
                    ? blend(0xFF161920, palette.navBarBg, 0.45f)
                    : blend(0xFFFFFFFF, palette.navBarBg, 0.22f);
            rimAccent = palette.accent;
        } else {
            baseSurface = palette.dark ? 0xFF181B22 : 0xFFFAFBFD;
            rimAccent = palette.dark ? 0xFFFFFFFF : 0xFF3E4752;
        }
        pillDrawable.update(
                visibleTabs, selectedIndex, activePillFill, viewPager, mainTabContainer, bnbBg, sysNavBg,
                pageBgColor, baseSurface, rimAccent, palette.dark, Math.round(pillLeft)
        );
    }

    private static void restoreNonPillNavBar(
            View decorView,
            View bnbBg,
            View sysNavBg,
            ViewGroup tabsParent,
            List<View> visibleTabs
    ) {
        if (bnbBg.getBackground() instanceof FloatingNavPillDrawable) {
            bnbBg.setBackground(null);
            NAV_APPLIED.remove(bnbBg);
        }
        if (bnbBg.getTouchDelegate() instanceof PillSlotTouchDelegate) {
            bnbBg.setTouchDelegate(null);
        }
        if (bnbBg.getAlpha() != 1f) {
            bnbBg.setAlpha(1f);
        }
        if (bnbBg instanceof ImageView) {
            ImageView img = (ImageView) bnbBg;
            if (img.getImageAlpha() != 255) {
                img.setImageAlpha(255);
            }
        }
        Integer origBgH = ORIG_NAV_HEIGHT.remove(bnbBg);
        if (origBgH != null && bnbBg.getLayoutParams() != null) {
            bnbBg.getLayoutParams().height = origBgH;
            bnbBg.requestLayout();
        }
        if (bnbBg.getTranslationY() != 0f) {
            bnbBg.setTranslationY(0f);
        }
        if (bnbBg.getTranslationZ() != 0f) {
            bnbBg.setTranslationZ(0f);
        }
        if (sysNavBg != null && sysNavBg.getAlpha() != 1f) {
            sysNavBg.setAlpha(1f);
        }
        if (tabsParent != null) {
            if (tabsParent.getBackground() instanceof FloatingNavPillDrawable) {
                tabsParent.setBackground(null);
            }
            if (tabsParent.getTouchDelegate() instanceof PillSlotTouchDelegate) {
                tabsParent.setTouchDelegate(null);
            }
            int[] origPad = ORIG_NAV_PADDING.remove(tabsParent);
            if (origPad != null) {
                tabsParent.setPadding(origPad[0], origPad[1], origPad[2], origPad[3]);
            }
            Integer origTpH = ORIG_NAV_HEIGHT.remove(tabsParent);
            if (origTpH != null && tabsParent.getLayoutParams() != null) {
                tabsParent.getLayoutParams().height = origTpH;
                tabsParent.requestLayout();
            }
            if (tabsParent.getTranslationY() != 0f) {
                tabsParent.setTranslationY(0f);
            }
            if (tabsParent.getTranslationZ() != 0f) {
                tabsParent.setTranslationZ(0f);
            }
        }
        for (Map.Entry<View, Drawable> entry : ORIG_NAV_CLEARED_BG.entrySet()) {
            View v = entry.getKey();
            Drawable d = entry.getValue();
            if (v != null && d != null && v.getBackground() == null) {
                v.setBackground(d);
            }
        }
        ORIG_NAV_CLEARED_BG.clear();

        for (Map.Entry<ImageView, Integer> entry : ORIG_NAV_IMAGE_ALPHA.entrySet()) {
            ImageView iv = entry.getKey();
            Integer imgAlpha = entry.getValue();
            if (iv != null && imgAlpha != null) {
                iv.setImageAlpha(imgAlpha);
            }
        }
        ORIG_NAV_IMAGE_ALPHA.clear();

        for (Map.Entry<View, Float> entry : ORIG_NAV_SIBLING_ALPHA.entrySet()) {
            View sib = entry.getKey();
            Float alpha = entry.getValue();
            if (sib != null && alpha != null) {
                sib.setAlpha(alpha);
            }
        }
        ORIG_NAV_SIBLING_ALPHA.clear();

        for (Map.Entry<TextView, Typeface> entry : ORIG_NAV_TEXT_TYPEFACE.entrySet()) {
            TextView tv = entry.getKey();
            Typeface tf = entry.getValue();
            if (tv != null && tf != null) {
                tv.setTypeface(tf);
            }
        }
        ORIG_NAV_TEXT_TYPEFACE.clear();

        for (Map.Entry<View, float[]> entry : ORIG_NAV_BADGE_TRANSFORM.entrySet()) {
            View bv = entry.getKey();
            float[] tr = entry.getValue();
            if (bv != null && tr != null && tr.length == 3) {
                bv.setScaleX(tr[0]);
                bv.setScaleY(tr[1]);
                bv.setTranslationY(tr[2]);
            }
        }
        ORIG_NAV_BADGE_TRANSFORM.clear();

        for (Map.Entry<View, Float> entry : ORIG_NAV_TAB_TX.entrySet()) {
            View tv = entry.getKey();
            Float tx = entry.getValue();
            if (tv != null && tx != null) {
                tv.setTranslationX(tx);
            }
        }
        ORIG_NAV_TAB_TX.clear();

        if (decorView != null) {
            View contentRoot = decorView.findViewById(android.R.id.content);
            if (contentRoot != null && Boolean.TRUE.equals(CONTENT_BG_APPLIED.remove(contentRoot))) {
                contentRoot.setBackground(null);
            }
        }
    }

    private static void ensureNavScrollBlurObserver(View decorView, View pillHost) {
        NAV_SCROLL_HOST.put(decorView, new WeakReference<>(pillHost));
        ViewTreeObserver vto = decorView.getViewTreeObserver();
        if (!NAV_SCROLL_LISTENERS.containsKey(decorView)) {
            final WeakReference<View> decorRef = new WeakReference<>(decorView);
            ViewTreeObserver.OnScrollChangedListener scrollListener = () -> {
                View d = decorRef.get();
                if (d == null) {
                    return;
                }
                WeakReference<View> hostRef = NAV_SCROLL_HOST.get(d);
                View host = hostRef != null ? hostRef.get() : null;
                if (host != null && host.getBackground() instanceof FloatingNavPillDrawable) {
                    ((FloatingNavPillDrawable) host.getBackground()).markDirty();
                }
            };
            NAV_SCROLL_LISTENERS.put(decorView, scrollListener);
            if (vto != null && vto.isAlive()) {
                vto.addOnScrollChangedListener(scrollListener);
            }
        }
        if (!NAV_PREDRAW_LISTENERS.containsKey(decorView)) {
            final WeakReference<View> decorRef = new WeakReference<>(decorView);
            ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
                View d = decorRef.get();
                if (d == null || !LineSettings.navBarPill() || BlurBars.powerSave()) {
                    return true;
                }
                WeakReference<View> hostRef = NAV_SCROLL_HOST.get(d);
                View host = hostRef != null ? hostRef.get() : null;
                if (host != null && host.getBackground() instanceof FloatingNavPillDrawable) {
                    FloatingNavPillDrawable pill = (FloatingNavPillDrawable) host.getBackground();
                    if (pill.isSourceDirty()) {
                        pill.markDirty();
                    }
                }
                return true;
            };
            NAV_PREDRAW_LISTENERS.put(decorView, preDrawListener);
            if (vto != null && vto.isAlive()) {
                vto.addOnPreDrawListener(preDrawListener);
            }
        }
    }

    private static final int[] NAV_LOC_A = new int[2];
    private static final int[] NAV_LOC_B = new int[2];

    private static boolean isScrollableContainer(View v) {
        if (v instanceof ScrollView || v instanceof HorizontalScrollView) {
            return true;
        }
        String cn = v.getClass().getName();
        if (cn.contains("ViewPager2$RecyclerViewImpl")) {
            return false;
        }
        return cn.contains("RecyclerView")
                || cn.contains("NestedScrollView")
                || cn.contains("ListView")
                || cn.contains("WebView");
    }

    private static boolean hasVisibleTextOrClickable(View v) {
        if (v == null || v.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (v.isClickable() || v.hasOnClickListeners()) {
            return true;
        }
        if (v instanceof TextView) {
            CharSequence txt = ((TextView) v).getText();
            return txt != null && txt.length() > 0;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int n = vg.getChildCount();
            for (int i = 0; i < n; i++) {
                if (hasVisibleTextOrClickable(vg.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void hidePassiveScrimTree(View v, View pillHost, List<View> visibleTabs) {
        if (v == null
                || v == pillHost
                || BlurBars.isSelfOrAncestor(v, pillHost)
                || isAnyTab(v, visibleTabs)) {
            return;
        }
        clearNavWrapperBackground(v);
        if (v instanceof ImageView) {
            ImageView iv = (ImageView) v;
            if (iv.getImageAlpha() != 0) {
                if (!ORIG_NAV_IMAGE_ALPHA.containsKey(iv)) {
                    ORIG_NAV_IMAGE_ALPHA.put(iv, iv.getImageAlpha());
                }
                iv.setImageAlpha(0);
            }
        }
        if (!hasVisibleTextOrClickable(v)) {
            if (v.getAlpha() > 0f) {
                if (!ORIG_NAV_SIBLING_ALPHA.containsKey(v)) {
                    ORIG_NAV_SIBLING_ALPHA.put(v, v.getAlpha());
                }
                v.setAlpha(0f);
            }
        } else if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int n = vg.getChildCount();
            for (int i = 0; i < n; i++) {
                hidePassiveScrimTree(vg.getChildAt(i), pillHost, visibleTabs);
            }
        }
    }

    private static void stripContainerBottomInsetPadding(View v, int decorBottom, int minInsetPx) {
        if (!(v instanceof ViewGroup) || isScrollableContainer(v)) {
            return;
        }
        v.getLocationInWindow(NAV_LOC_A);
        int vBot = NAV_LOC_A[1] + v.getHeight();
        int padB = v.getPaddingBottom();
        if (vBot >= decorBottom - minInsetPx && (padB >= minInsetPx || ORIG_NAV_CONTAINER_PAD_BOT.containsKey(v))) {
            if (!ORIG_NAV_CONTAINER_PAD_BOT.containsKey(v)) {
                ORIG_NAV_CONTAINER_PAD_BOT.put(v, padB);
            }
            if (padB != 0) {
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 0);
            }
        }
    }

    private static Field findIntFieldOnLayoutParams(Class<?> cls, String name) {
        Class<?> cur = cls;
        while (cur != null
                && cur != ViewGroup.MarginLayoutParams.class
                && cur != ViewGroup.LayoutParams.class
                && cur != Object.class) {
            try {
                Field f = cur.getDeclaredField(name);
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    return f;
                }
            } catch (NoSuchFieldException ignored) {
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    private static Field findMatchingIdFieldOnLayoutParams(Object lp, int[] candidateIds, int idCount) {
        if (lp == null || idCount <= 0) {
            return null;
        }
        Class<?> cur = lp.getClass();
        while (cur != null
                && cur != ViewGroup.MarginLayoutParams.class
                && cur != ViewGroup.LayoutParams.class
                && cur != Object.class) {
            for (Field f : cur.getDeclaredFields()) {
                if (f.getType() != int.class || java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    int val = f.getInt(lp);
                    for (int i = 0; i < idCount; i++) {
                        if (val == candidateIds[i]) {
                            return f;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    private static void expandConstraintChildToBottom(
            View target,
            ViewGroup parent,
            View bnbBg,
            View sysNavBg,
            View pillHost,
            int decorBottom,
            int maxBarH
    ) {
        ViewGroup.LayoutParams rawLp = target.getLayoutParams();
        if (!(rawLp instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) rawLp;
        ConstraintSaveState cs = NAV_CONSTRAINT_STATES.get(target);
        if (cs != null) {
            try {
                boolean dirty = false;
                if (cs.botToTopField != null && cs.botToTopField.getInt(rawLp) != -1) {
                    cs.botToTopField.setInt(rawLp, -1);
                    dirty = true;
                }
                if (cs.botToBotField != null && cs.botToBotField.getInt(rawLp) != 0) {
                    cs.botToBotField.setInt(rawLp, 0);
                    dirty = true;
                }
                if (mlp.bottomMargin != 0) {
                    mlp.bottomMargin = 0;
                    dirty = true;
                }
                if (cs.explicitHeightMode) {
                    target.getLocationInWindow(NAV_LOC_A);
                    int desiredH = Math.max(1, decorBottom - NAV_LOC_A[1]);
                    if (rawLp.height != desiredH) {
                        rawLp.height = desiredH;
                        dirty = true;
                    }
                } else if (cs.botToBotField != null && rawLp.height > 0) {
                    rawLp.height = 0;
                    dirty = true;
                }
                if (dirty) {
                    target.setLayoutParams(rawLp);
                }
            } catch (Throwable ignored) {
            }
            return;
        }

        try {
            Field fBotToTop = findIntFieldOnLayoutParams(rawLp.getClass(), "bottomToTop");
            Field fBotToBot = findIntFieldOnLayoutParams(rawLp.getClass(), "bottomToBottom");
            if (fBotToTop != null && fBotToBot != null) {
                cs = new ConstraintSaveState();
                cs.botToTopField = fBotToTop;
                cs.origBotToTop = fBotToTop.getInt(rawLp);
                cs.botToBotField = fBotToBot;
                cs.origBotToBot = fBotToBot.getInt(rawLp);
                cs.origHeight = rawLp.height;
                cs.origBottomMargin = mlp.bottomMargin;
                cs.explicitHeightMode = false;
                NAV_CONSTRAINT_STATES.put(target, cs);

                fBotToTop.setInt(rawLp, -1);
                fBotToBot.setInt(rawLp, 0);
                mlp.bottomMargin = 0;
                if (rawLp.height > 0) {
                    rawLp.height = 0;
                }
                target.setLayoutParams(rawLp);
                return;
            }

            // If R8 renamed ConstraintLayout$LayoutParams fields, match the 0x7f resource ID of the bottom bar.
            int childCount = parent.getChildCount();
            int[] candidateIds = new int[childCount + 3];
            int idCount = 0;
            if (bnbBg != null && (bnbBg.getId() & 0xFF000000) == 0x7F000000) candidateIds[idCount++] = bnbBg.getId();
            if (sysNavBg != null && (sysNavBg.getId() & 0xFF000000) == 0x7F000000) candidateIds[idCount++] = sysNavBg.getId();
            if (pillHost != null && (pillHost.getId() & 0xFF000000) == 0x7F000000) candidateIds[idCount++] = pillHost.getId();
            for (int i = 0; i < childCount; i++) {
                View s = parent.getChildAt(i);
                if (s != null && s != target && (s.getId() & 0xFF000000) == 0x7F000000 && s.getHeight() <= maxBarH) {
                    candidateIds[idCount++] = s.getId();
                }
            }
            Field matchedAnchorField = findMatchingIdFieldOnLayoutParams(rawLp, candidateIds, idCount);
            if (matchedAnchorField != null) {
                cs = new ConstraintSaveState();
                cs.botToTopField = matchedAnchorField;
                cs.origBotToTop = matchedAnchorField.getInt(rawLp);
                cs.origHeight = rawLp.height;
                cs.origBottomMargin = mlp.bottomMargin;
                cs.explicitHeightMode = true;
                NAV_CONSTRAINT_STATES.put(target, cs);

                matchedAnchorField.setInt(rawLp, -1);
                mlp.bottomMargin = 0;
                target.getLocationInWindow(NAV_LOC_A);
                rawLp.height = Math.max(1, decorBottom - NAV_LOC_A[1]);
                target.setLayoutParams(rawLp);
                return;
            }

            if (mlp.bottomMargin > 0 || rawLp.height > 0) {
                cs = new ConstraintSaveState();
                cs.origHeight = rawLp.height;
                cs.origBottomMargin = mlp.bottomMargin;
                cs.explicitHeightMode = rawLp.height > 0;
                NAV_CONSTRAINT_STATES.put(target, cs);
                mlp.bottomMargin = 0;
                if (cs.explicitHeightMode) {
                    target.getLocationInWindow(NAV_LOC_A);
                    rawLp.height = Math.max(1, decorBottom - NAV_LOC_A[1]);
                }
                target.setLayoutParams(rawLp);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void expandLinearChildToBottom(View target, LinearLayout parent, int decorBottom) {
        if (parent.getOrientation() != LinearLayout.VERTICAL) {
            return;
        }
        int count = parent.getChildCount();
        int targetIdx = -1;
        for (int i = 0; i < count; i++) {
            if (parent.getChildAt(i) == target) {
                targetIdx = i;
                break;
            }
        }
        if (targetIdx < 0 || targetIdx + 1 >= count) {
            return;
        }
        View firstTrailing = null;
        int trailingH = 0;
        for (int j = targetIdx + 1; j < count; j++) {
            View s = parent.getChildAt(j);
            if (s != null && s.getVisibility() != View.GONE && s.getHeight() > 0) {
                if (firstTrailing == null) {
                    firstTrailing = s;
                }
                trailingH += s.getHeight();
            }
        }
        if (firstTrailing == null
                || trailingH <= 0
                || !(target.getLayoutParams() instanceof LinearLayout.LayoutParams)
                || !(firstTrailing.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return;
        }
        LinearLayout.LayoutParams tLp = (LinearLayout.LayoutParams) target.getLayoutParams();
        LinearLayout.LayoutParams bLp = (LinearLayout.LayoutParams) firstTrailing.getLayoutParams();
        LinearSaveState ls = NAV_LINEAR_STATES.get(target);
        if (ls == null) {
            ls = new LinearSaveState();
            ls.origHeight = tLp.height;
            ls.origWeight = tLp.weight;
            ls.trailingBarRef = new WeakReference<>(firstTrailing);
            ls.origTrailingTopMargin = bLp.topMargin;
            NAV_LINEAR_STATES.put(target, ls);
        }
        target.getLocationInWindow(NAV_LOC_A);
        int desiredH = Math.max(1, decorBottom - NAV_LOC_A[1]);
        int desiredTopMargin = ls.origTrailingTopMargin - trailingH;
        if (tLp.height != desiredH || tLp.weight != 0f) {
            tLp.height = desiredH;
            tLp.weight = 0f;
            target.setLayoutParams(tLp);
        }
        if (bLp.topMargin != desiredTopMargin) {
            bLp.topMargin = desiredTopMargin;
            firstTrailing.setLayoutParams(bLp);
        }
    }

    private static void expandViewToDecorBottom(View target, int decorBottom) {
        if (target == null || target.getHeight() <= 0 || target.isLayoutRequested()) {
            return;
        }
        if (!(target.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) target.getLayoutParams();
        NavExpandState st = NAV_EXPAND_STATES.get(target);
        if (st == null) {
            st = new NavExpandState();
            NAV_EXPAND_STATES.put(target, st);
        }
        target.getLocationInWindow(NAV_LOC_A);
        int naturalBottom = (NAV_LOC_A[1] + target.getHeight()) - st.extendedBottom;
        int needed = naturalBottom < decorBottom ? (decorBottom - naturalBottom) : 0;
        int baseMargin = (mlp.bottomMargin == st.setMarginBottom)
                ? (mlp.bottomMargin + st.extendedBottom)
                : mlp.bottomMargin;
        int nextMargin = baseMargin - needed;
        if (mlp.bottomMargin != nextMargin) {
            mlp.bottomMargin = nextMargin;
            target.setLayoutParams(mlp);
        }
        st.setMarginBottom = nextMargin;
        st.extendedBottom = needed;
    }

    private static void expandAnyContainerToBottom(
            View target,
            ViewGroup parent,
            View bnbBg,
            View sysNavBg,
            View pillHost,
            int decorBottom,
            int maxBarH
    ) {
        if (target == null || parent == null) {
            return;
        }
        String parentClass = parent.getClass().getName();
        if (parentClass.contains("ConstraintLayout")) {
            expandConstraintChildToBottom(target, parent, bnbBg, sysNavBg, pillHost, decorBottom, maxBarH);
        } else if (parent instanceof LinearLayout) {
            expandLinearChildToBottom(target, (LinearLayout) parent, decorBottom);
        } else if (!parentClass.contains("ViewPager") && !parentClass.contains("RecyclerView")) {
            expandViewToDecorBottom(target, decorBottom);
        }
    }

    private static boolean isMultiPageHost(View v) {
        String cn = v.getClass().getName();
        return cn.contains("ViewPager") || cn.contains("ViewPager2$RecyclerViewImpl");
    }

    private static void walkAndExpandContentTree(
            ViewGroup vg,
            View pillHost,
            View bnbBg,
            View sysNavBg,
            List<View> visibleTabs,
            int decorW,
            int decorH,
            int decorBottom,
            int maxBarH,
            int minInsetPx,
            int tolPx,
            int depth
    ) {
        if (vg == null || depth > 8 || isScrollableContainer(vg)) {
            return;
        }
        vg.setClipChildren(false);
        vg.setClipToPadding(false);
        vg.setClipToOutline(false);
        stripContainerBottomInsetPadding(vg, decorBottom, minInsetPx);

        if (isMultiPageHost(vg)) {
            int pCount = vg.getChildCount();
            for (int i = 0; i < pCount; i++) {
                View page = vg.getChildAt(i);
                if (page instanceof ViewGroup && page.getVisibility() == View.VISIBLE) {
                    walkAndExpandContentTree(
                            (ViewGroup) page, pillHost, bnbBg, sysNavBg, visibleTabs,
                            decorW, decorH, decorBottom, maxBarH, minInsetPx, tolPx, depth + 1
                    );
                }
            }
            return;
        }

        int count = vg.getChildCount();
        View bestContentChild = null;
        int bestH = Math.round(decorH * 0.35f);
        for (int i = 0; i < count; i++) {
            View c = vg.getChildAt(i);
            if (c == null || c.getVisibility() != View.VISIBLE || c == pillHost || isAnyTab(c, visibleTabs)) {
                continue;
            }
            int cH = c.getHeight();
            boolean wrapsBar = BlurBars.isSelfOrAncestor(c, pillHost)
                    || (bnbBg != null && BlurBars.isSelfOrAncestor(c, bnbBg))
                    || (sysNavBg != null && BlurBars.isSelfOrAncestor(c, sysNavBg));
            if (wrapsBar) {
                // If a full-screen wrapper holds both the page host and the bottom bar, descend into it.
                if (c instanceof ViewGroup && cH > maxBarH) {
                    walkAndExpandContentTree(
                            (ViewGroup) c, pillHost, bnbBg, sysNavBg, visibleTabs,
                            decorW, decorH, decorBottom, maxBarH, minInsetPx, tolPx, depth + 1
                    );
                }
                continue;
            }
            if (cH > 0 && cH <= maxBarH && c.getWidth() >= decorW / 2) {
                c.getLocationInWindow(NAV_LOC_A);
                int cBot = NAV_LOC_A[1] + cH;
                if (cBot >= decorBottom - maxBarH) {
                    hidePassiveScrimTree(c, pillHost, visibleTabs);
                }
                continue;
            }
            if (cH > bestH) {
                bestH = cH;
                bestContentChild = c;
            }
        }

        if (bestContentChild == null) {
            return;
        }
        if (bestContentChild.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) bestContentChild.getLayoutParams();
            if (mlp.bottomMargin > 0) {
                NavExpandState st = NAV_EXPAND_STATES.get(bestContentChild);
                if (st == null) {
                    st = new NavExpandState();
                    NAV_EXPAND_STATES.put(bestContentChild, st);
                }
                int base = (mlp.bottomMargin == st.setMarginBottom)
                        ? (mlp.bottomMargin + st.extendedBottom)
                        : mlp.bottomMargin;
                mlp.bottomMargin = 0;
                bestContentChild.setLayoutParams(mlp);
                st.setMarginBottom = 0;
                st.extendedBottom = base;
            }
        }
        bestContentChild.getLocationInWindow(NAV_LOC_A);
        int childBot = NAV_LOC_A[1] + bestContentChild.getHeight();
        boolean alreadyTracked = NAV_CONSTRAINT_STATES.containsKey(bestContentChild)
                || NAV_LINEAR_STATES.containsKey(bestContentChild)
                || NAV_EXPAND_STATES.containsKey(bestContentChild);
        if (alreadyTracked || (childBot >= decorBottom - maxBarH && childBot < decorBottom - tolPx)) {
            expandAnyContainerToBottom(bestContentChild, vg, bnbBg, sysNavBg, pillHost, decorBottom, maxBarH);
        }

        if (bestContentChild instanceof ViewGroup) {
            walkAndExpandContentTree(
                    (ViewGroup) bestContentChild, pillHost, bnbBg, sysNavBg, visibleTabs,
                    decorW, decorH, decorBottom, maxBarH, minInsetPx, tolPx, depth + 1
            );
        }
    }

    private static void collectPageContentLeaves(
            View view,
            int minHeight,
            List<View> outScrollLists,
            List<View> outComposeHosts
    ) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return;
        }
        String name = view.getClass().getName();
        if (isMultiPageHost(view) && view instanceof ViewGroup) {
            ViewGroup pager = (ViewGroup) view;
            int pCount = pager.getChildCount();
            for (int i = 0; i < pCount; i++) {
                View page = pager.getChildAt(i);
                if (page != null && page.getVisibility() == View.VISIBLE) {
                    if (!isMultiPageHost(page) && page.getHeight() >= minHeight) {
                        outComposeHosts.add(page);
                    }
                    collectPageContentLeaves(page, minHeight, outScrollLists, outComposeHosts);
                }
            }
            return;
        }
        boolean isScrollList = name.endsWith("RecyclerView")
                || view instanceof ScrollView
                || view instanceof android.widget.AbsListView
                || name.endsWith("NestedScrollView");
        if (isScrollList && view.getHeight() >= minHeight && view.getHeight() > 0) {
            outScrollLists.add(view);
            return;
        }
        boolean isComposeWrapper = (name.endsWith(".ComposeView") || name.contains("AbstractComposeView"))
                && !name.contains("AndroidComposeView");
        if (isComposeWrapper && view.getHeight() >= minHeight && view.getHeight() > 0) {
            outComposeHosts.add(view);
            return;
        }
        if (view instanceof ViewGroup && !name.contains("AndroidComposeView")) {
            ViewGroup group = (ViewGroup) view;
            int n = group.getChildCount();
            for (int i = 0; i < n; i++) {
                collectPageContentLeaves(group.getChildAt(i), minHeight, outScrollLists, outComposeHosts);
            }
        }
    }

    private static void stripComposeBottomNavInsets(View composeHost) {
        if (composeHost == null || Build.VERSION.SDK_INT < 30) {
            return;
        }
        if (Boolean.TRUE.equals(COMPOSE_INSETS_STRIPPED.get(composeHost))) {
            return;
        }
        try {
            Context c = composeHost.getContext();
            int compatTag = c.getResources().getIdentifier("tag_on_apply_window_insets", "id", c.getPackageName());
            if (compatTag != 0 && composeHost.getTag(compatTag) != null) {
                return;
            }
        } catch (Throwable ignored) {
        }
        COMPOSE_INSETS_STRIPPED.put(composeHost, Boolean.TRUE);
        composeHost.setOnApplyWindowInsetsListener((v, insets) -> {
            if (insets == null || !LineSettings.navBarPill()) {
                return v.onApplyWindowInsets(insets);
            }
            int type = WindowInsets.Type.navigationBars();
            Insets nav = insets.getInsets(type);
            Insets navIgn = insets.getInsetsIgnoringVisibility(type);
            if (nav.bottom > 0 || navIgn.bottom > 0) {
                insets = new WindowInsets.Builder(insets)
                        .setInsets(type, Insets.of(nav.left, nav.top, nav.right, 0))
                        .setInsetsIgnoringVisibility(type, Insets.of(navIgn.left, navIgn.top, navIgn.right, 0))
                        .build();
            }
            return v.onApplyWindowInsets(insets);
        });
        composeHost.requestApplyInsets();
    }

    private static void applyListPillPadding(View list, View stopAncestor, int desiredPad) {
        if (!(list instanceof ViewGroup)) {
            return;
        }
        ViewGroup lg = (ViewGroup) list;
        lg.setClipToPadding(false);
        Integer addedBefore = ORIG_NAV_LIST_PAD.get(lg);
        int prevAdded = addedBefore != null ? addedBefore : 0;
        if (prevAdded != desiredPad) {
            int nextBottom = Math.max(0, lg.getPaddingBottom() - prevAdded + desiredPad);
            lg.setPadding(lg.getPaddingLeft(), lg.getPaddingTop(), lg.getPaddingRight(), nextBottom);
            if (desiredPad == 0) {
                ORIG_NAV_LIST_PAD.remove(lg);
            } else {
                ORIG_NAV_LIST_PAD.put(lg, desiredPad);
            }
        }
        ViewParent p = lg.getParent();
        while (p instanceof ViewGroup && p != stopAncestor) {
            ViewGroup vg = (ViewGroup) p;
            vg.setClipToPadding(false);
            vg.setClipChildren(false);
            vg.setClipToOutline(false);
            p = vg.getParent();
        }
    }

    private static void stretchChainUpToDecor(
            View leaf,
            View stopView,
            View bnbBg,
            View sysNavBg,
            View pillHost,
            int decorBottom,
            int maxBarH,
            int minInsetPx,
            int tolPx
    ) {
        View cur = leaf;
        while (cur != null && cur != stopView && cur.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) cur.getParent();
            parent.setClipChildren(false);
            parent.setClipToPadding(false);
            parent.setClipToOutline(false);
            if (cur instanceof ViewGroup && !isScrollableContainer(cur)) {
                ViewGroup cvg = (ViewGroup) cur;
                cvg.setClipChildren(false);
                cvg.setClipToPadding(false);
                cvg.setClipToOutline(false);
                stripContainerBottomInsetPadding(cvg, decorBottom, minInsetPx);
            }
            stripContainerBottomInsetPadding(parent, decorBottom, minInsetPx);

            if (cur.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) cur.getLayoutParams();
                if (mlp.bottomMargin > 0) {
                    NavExpandState st = NAV_EXPAND_STATES.get(cur);
                    if (st == null) {
                        st = new NavExpandState();
                        NAV_EXPAND_STATES.put(cur, st);
                    }
                    int base = (mlp.bottomMargin == st.setMarginBottom)
                            ? (mlp.bottomMargin + st.extendedBottom)
                            : mlp.bottomMargin;
                    mlp.bottomMargin = 0;
                    cur.setLayoutParams(mlp);
                    st.setMarginBottom = 0;
                    st.extendedBottom = base;
                }
            }
            if (cur.getHeight() > 0) {
                cur.getLocationInWindow(NAV_LOC_A);
                int curBot = NAV_LOC_A[1] + cur.getHeight();
                parent.getLocationInWindow(NAV_LOC_B);
                int parBot = NAV_LOC_B[1] + parent.getHeight();
                boolean tracked = NAV_CONSTRAINT_STATES.containsKey(cur)
                        || NAV_LINEAR_STATES.containsKey(cur)
                        || NAV_EXPAND_STATES.containsKey(cur);
                if (tracked || (parBot >= decorBottom - tolPx && curBot >= decorBottom - maxBarH && curBot < decorBottom - tolPx)) {
                    expandAnyContainerToBottom(cur, parent, bnbBg, sysNavBg, pillHost, decorBottom, maxBarH);
                }
            }
            cur = parent;
        }
    }

    private static void extendMainContentBehindPill(
            View decorView,
            Context ctx,
            View pillHost,
            View bnbBg,
            View sysNavBg,
            List<View> visibleTabs,
            boolean pillActive
    ) {
        if (!pillActive) {
            for (View cv : COMPOSE_INSETS_STRIPPED.keySet()) {
                if (cv != null) {
                    cv.setOnApplyWindowInsetsListener(null);
                    cv.requestApplyInsets();
                }
            }
            COMPOSE_INSETS_STRIPPED.clear();

            for (Map.Entry<View, Integer> e : ORIG_NAV_LIST_PAD.entrySet()) {
                View v = e.getKey();
                Integer added = e.getValue();
                if (v != null && added != null && added != 0) {
                    int restored = Math.max(0, v.getPaddingBottom() - added);
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), restored);
                }
            }
            ORIG_NAV_LIST_PAD.clear();

            for (Map.Entry<View, Integer> e : ORIG_NAV_CONTAINER_PAD_BOT.entrySet()) {
                View v = e.getKey();
                Integer p = e.getValue();
                if (v != null && p != null) {
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), p);
                }
            }
            ORIG_NAV_CONTAINER_PAD_BOT.clear();

            for (Map.Entry<View, ConstraintSaveState> e : NAV_CONSTRAINT_STATES.entrySet()) {
                View v = e.getKey();
                ConstraintSaveState cs = e.getValue();
                if (v != null && cs != null && v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    try {
                        if (cs.botToTopField != null) cs.botToTopField.setInt(mlp, cs.origBotToTop);
                        if (cs.botToBotField != null) cs.botToBotField.setInt(mlp, cs.origBotToBot);
                    } catch (Throwable ignored) {
                    }
                    mlp.height = cs.origHeight;
                    mlp.bottomMargin = cs.origBottomMargin;
                    v.setLayoutParams(mlp);
                }
            }
            NAV_CONSTRAINT_STATES.clear();

            for (Map.Entry<View, LinearSaveState> e : NAV_LINEAR_STATES.entrySet()) {
                View v = e.getKey();
                LinearSaveState ls = e.getValue();
                if (v != null && ls != null && v.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams tLp = (LinearLayout.LayoutParams) v.getLayoutParams();
                    tLp.height = ls.origHeight;
                    tLp.weight = ls.origWeight;
                    v.setLayoutParams(tLp);
                }
                View trailing = ls != null && ls.trailingBarRef != null ? ls.trailingBarRef.get() : null;
                if (trailing != null && trailing.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams bLp = (LinearLayout.LayoutParams) trailing.getLayoutParams();
                    bLp.topMargin = ls.origTrailingTopMargin;
                    trailing.setLayoutParams(bLp);
                }
            }
            NAV_LINEAR_STATES.clear();

            for (Map.Entry<View, NavExpandState> e : NAV_EXPAND_STATES.entrySet()) {
                View v = e.getKey();
                NavExpandState st = e.getValue();
                if (v != null && st != null && v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    if (mlp.bottomMargin == st.setMarginBottom) {
                        mlp.bottomMargin = mlp.bottomMargin + st.extendedBottom;
                        v.setLayoutParams(mlp);
                    }
                }
            }
            NAV_EXPAND_STATES.clear();
            return;
        }

        int decorH = decorView.getHeight();
        int decorW = decorView.getWidth();
        if (decorH <= 0 || decorW <= 0 || pillHost.getHeight() <= 0) {
            return;
        }
        decorView.getLocationInWindow(NAV_LOC_A);
        pillHost.getLocationInWindow(NAV_LOC_B);
        int decorBottom = NAV_LOC_A[1] + decorH;
        int pillTopWinY = NAV_LOC_B[1];
        if (pillHost.getBackground() instanceof FloatingNavPillDrawable) {
            float drawnTop = ((FloatingNavPillDrawable) pillHost.getBackground()).lastPillTop;
            if (drawnTop > 0f) {
                pillTopWinY = NAV_LOC_B[1] + Math.round(drawnTop);
            }
        }

        int maxBarH = BlurBars.dp(ctx, 165.0f);
        int minInsetPx = BlurBars.dp(ctx, 12.0f);
        int tolPx = BlurBars.dp(ctx, 4.0f);
        int desiredListPad = Math.max(0, decorBottom - pillTopWinY) + BlurBars.dp(ctx, 10.0f);

        // Stretch the main tab ViewPager (R.id.viewpager) and every mounted scrollable or Compose leaf inside it.
        View viewPager = BlurBars.find(decorView, "viewpager");
        if (viewPager != null && viewPager.isAttachedToWindow()) {
            stretchChainUpToDecor(
                    viewPager, decorView, bnbBg, sysNavBg, pillHost, decorBottom, maxBarH, minInsetPx, tolPx
            );
            if (viewPager instanceof ViewGroup) {
                ViewGroup pagerGroup = (ViewGroup) viewPager;
                pagerGroup.setClipToPadding(false);
                pagerGroup.setClipChildren(false);
                pagerGroup.setClipToOutline(false);
                List<View> scrollLeaves = new ArrayList<>(4);
                List<View> composeLeaves = new ArrayList<>(4);
                collectPageContentLeaves(pagerGroup, Math.max(1, decorH / 3), scrollLeaves, composeLeaves);
                for (int i = 0; i < scrollLeaves.size(); i++) {
                    View list = scrollLeaves.get(i);
                    stretchChainUpToDecor(
                            list, decorView, bnbBg, sysNavBg, pillHost, decorBottom, maxBarH, minInsetPx, tolPx
                    );
                    applyListPillPadding(list, viewPager, desiredListPad);
                }
                for (int i = 0; i < composeLeaves.size(); i++) {
                    View composeHost = composeLeaves.get(i);
                    stretchChainUpToDecor(
                            composeHost, decorView, bnbBg, sysNavBg, pillHost, decorBottom, maxBarH, minInsetPx, tolPx
                    );
                    stripComposeBottomNavInsets(composeHost);
                }
                walkAndExpandContentTree(
                        pagerGroup, pillHost, bnbBg, sysNavBg, visibleTabs,
                        decorW, decorH, decorBottom, maxBarH, minInsetPx, tolPx, 0
                );
            }
        }

        // Walk full-screen wrappers from android.R.id.content to expand other page hosts and hide scrims.
        View contentRoot = decorView.findViewById(android.R.id.content);
        ViewGroup startGroup = contentRoot instanceof ViewGroup
                ? (ViewGroup) contentRoot
                : (decorView instanceof ViewGroup ? (ViewGroup) decorView : null);
        if (startGroup != null) {
            walkAndExpandContentTree(
                    startGroup, pillHost, bnbBg, sysNavBg, visibleTabs,
                    decorW, decorH, decorBottom, maxBarH, minInsetPx, tolPx, 0
            );
        }

        // Stretch the parent chain of chat_list_recycler_view and apply bottom scroll padding.
        int rvId = BlurBars.id(ctx, "chat_list_recycler_view");
        View singleList = rvId != 0 ? decorView.findViewById(rvId) : null;
        if (singleList instanceof ViewGroup && singleList.isAttachedToWindow() && singleList.isShown()) {
            stretchChainUpToDecor(
                    singleList, decorView, bnbBg, sysNavBg, pillHost, decorBottom, maxBarH, minInsetPx, tolPx
            );
            applyListPillPadding(singleList, decorView, desiredListPad);
        }
    }

    /**
     * Computes vertical bounds {@code [top, bottom]} in {@code tab} coordinates anchored to the bottom
     * of {@code bnb_button_image} and {@code bnb_button_text} so internal icon headroom never shifts
     * the pill above the text label.
     */
    static void computeActivePillVerticalBounds(
            View tab,
            View iconView,
            View textView,
            float density,
            int[] locA,
            int[] locB,
            float[] outTopBottom
    ) {
        int tabH = tab.getHeight();
        tab.getLocationInWindow(locA);

        float textBottom = -1f;
        if (textView != null && textView.getVisibility() == View.VISIBLE && textView.getHeight() > 0) {
            textView.getLocationInWindow(locB);
            float textRelTop = locB[1] - locA[1];
            if (textView instanceof TextView) {
                TextView tv = (TextView) textView;
                if (tv.getLayout() != null && tv.getLineCount() > 0) {
                    int lineBot = tv.getCompoundPaddingTop() + tv.getLayout().getLineBottom(tv.getLineCount() - 1);
                    textBottom = textRelTop + lineBot;
                }
            }
            if (textBottom < 0f) {
                textBottom = textRelTop + textView.getHeight() - textView.getPaddingBottom();
            }
        }
        if (textBottom <= 0f) {
            textBottom = tabH - density * 3.0f;
        }

        float iconVisualTop = -1f;
        if (iconView != null && iconView.getVisibility() == View.VISIBLE && iconView.getHeight() > 0) {
            iconView.getLocationInWindow(locB);
            float iconRelTop = locB[1] - locA[1] + iconView.getPaddingTop();
            float iconRelBot = locB[1] - locA[1] + iconView.getHeight() - iconView.getPaddingBottom();
            // LINE's bnb_button_image includes empty badge headroom above a ~20dp glyph aligned to iconRelBot.
            iconVisualTop = Math.max(iconRelTop, iconRelBot - density * 20.0f);
        }
        if (iconVisualTop < 0f || iconVisualTop >= textBottom - density * 14.0f) {
            iconVisualTop = textBottom - density * 32.0f;
        }

        float top = iconVisualTop - density * 8.0f;
        float bottom = textBottom + density * 4.0f;
        float minH = density * 42.0f;
        if (bottom - top < minH) {
            top = bottom - minH;
        }
        outTopBottom[0] = top;
        outTopBottom[1] = bottom;
    }

    private static final class TabRefs {
        final WeakReference<View> tabRef;
        final WeakReference<View> iconRef;
        final WeakReference<View> textRef;

        TabRefs(View tab) {
            this.tabRef = new WeakReference<>(tab);
            View icon = BlurBars.find(tab, "bnb_button_image");
            View text = BlurBars.find(tab, "bnb_button_text");
            this.iconRef = icon != null ? new WeakReference<>(icon) : null;
            this.textRef = text != null ? new WeakReference<>(text) : null;
        }
    }

    /**
     * Concentric floating stadium-capsule background for the LINE bottom navigation bar with real-time
     * Android 12+ {@link RenderNode} and {@link RenderEffect} frosted-glass blur, exterior ambient shadow,
     * inner specular catch-light, and subtle outer hairline.
     */
    private static final class FloatingNavPillDrawable extends Drawable {
        static boolean recordingNavBlur;
        private static final PathInterpolator SLOT_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
        private static final long SLIDE_DURATION_MS = 280L;

        private final WeakReference<View> hostRef;
        private final WeakReference<View> decorRef;
        private final float density;
        private final List<TabRefs> tabSlots = new ArrayList<>(6);
        private final List<WeakReference<View>> rawTabRefs = new ArrayList<>(6);
        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint activeFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint innerRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF pillRect = new RectF();
        private final RectF activeRect = new RectF();
        private final RectF innerRimRect = new RectF();
        private final RectF rimRect = new RectF();
        private final RectF shadowRect = new RectF();
        private final Path clipPath = new Path();
        private final int[] hostLoc = new int[2];
        private final int[] tabLoc = new int[2];
        private final int[] childLoc = new int[2];
        private final float[] tabTopBot = new float[2];
        private final float[] leftRight = new float[2];

        private Object gpu;
        private WeakReference<View> viewPagerRef;
        private WeakReference<View> mainTabRef;
        private WeakReference<View> bnbBgRef;
        private WeakReference<View> sysNavBgRef;
        private int pageBgColor;
        private int surfaceRgb;
        private int rimAccentRgb;
        private int activeFillColor;
        private boolean dark;
        private int fallbackSideInset;
        private int animToSlot = -1;
        private float animFromSlot = 0f;
        private float animatedSlot = -1f;
        private long animStartUptimeMs;
        private boolean animating;
        private boolean blurDirty = true;
        private float cachedLeft = -1f;
        private float cachedTop = -1f;
        private float cachedRight = -1f;
        private float cachedBottom = -1f;
        private int cachedStyleKey;
        float lastPillTop = -1f;

        FloatingNavPillDrawable(View host, View decor) {
            this.hostRef = new WeakReference<>(host);
            this.decorRef = new WeakReference<>(decor);
            this.density = host.getResources().getDisplayMetrics().density;
            this.shadowPaint.setStyle(Paint.Style.FILL);
            this.fillPaint.setStyle(Paint.Style.FILL);
            this.activeFillPaint.setStyle(Paint.Style.FILL);
            this.innerRimPaint.setStyle(Paint.Style.STROKE);
            this.innerRimPaint.setStrokeWidth(Math.max(1.0f, this.density * 1.0f));
            this.rimPaint.setStyle(Paint.Style.STROKE);
            this.rimPaint.setStrokeWidth(Math.max(1.0f, this.density * 0.75f));
        }

        void update(
                List<View> visibleTabs,
                int selectedIndex,
                int activeFillColor,
                View viewPager,
                View mainTabContainer,
                View bnbBg,
                View sysNavBg,
                int pageBgColor,
                int surfaceRgb,
                int rimAccentRgb,
                boolean dark,
                int sideInset
        ) {
            if (this.viewPagerRef == null || this.viewPagerRef.get() != viewPager) {
                this.viewPagerRef = viewPager != null ? new WeakReference<>(viewPager) : null;
            }
            if (this.mainTabRef == null || this.mainTabRef.get() != mainTabContainer) {
                this.mainTabRef = mainTabContainer != null ? new WeakReference<>(mainTabContainer) : null;
            }
            if (this.bnbBgRef == null || this.bnbBgRef.get() != bnbBg) {
                this.bnbBgRef = bnbBg != null ? new WeakReference<>(bnbBg) : null;
            }
            if (this.sysNavBgRef == null || this.sysNavBgRef.get() != sysNavBg) {
                this.sysNavBgRef = sysNavBg != null ? new WeakReference<>(sysNavBg) : null;
            }

            int newCount = visibleTabs.size();
            boolean countChanged = this.tabSlots.size() != newCount;
            int clampedSel = newCount > 0 ? Math.min(newCount - 1, Math.max(0, selectedIndex)) : 0;
            boolean slotChanged = false;
            if (this.animatedSlot < 0f || countChanged) {
                this.animFromSlot = clampedSel;
                this.animToSlot = clampedSel;
                this.animatedSlot = clampedSel;
                this.animating = false;
                slotChanged = true;
            } else if (clampedSel != this.animToSlot) {
                this.animFromSlot = this.animatedSlot;
                this.animToSlot = clampedSel;
                this.animStartUptimeMs = SystemClock.uptimeMillis();
                this.animating = true;
                slotChanged = true;
            }

            boolean changed = slotChanged
                    || this.activeFillColor != activeFillColor
                    || this.pageBgColor != pageBgColor
                    || this.surfaceRgb != surfaceRgb
                    || this.rimAccentRgb != rimAccentRgb
                    || this.dark != dark
                    || this.fallbackSideInset != sideInset
                    || countChanged;
            if (!changed) {
                for (int i = 0; i < newCount; i++) {
                    if (this.tabSlots.get(i).tabRef.get() != visibleTabs.get(i)) {
                        changed = true;
                        break;
                    }
                }
            }
            if (changed) {
                this.activeFillColor = activeFillColor;
                this.activeFillPaint.setColor(activeFillColor);
                this.pageBgColor = pageBgColor;
                this.surfaceRgb = surfaceRgb;
                this.rimAccentRgb = rimAccentRgb;
                this.dark = dark;
                this.fallbackSideInset = sideInset;
                this.tabSlots.clear();
                this.rawTabRefs.clear();
                for (int i = 0; i < newCount; i++) {
                    View t = visibleTabs.get(i);
                    this.tabSlots.add(new TabRefs(t));
                    this.rawTabRefs.add(new WeakReference<>(t));
                }
                this.blurDirty = true;
                invalidateSelf();
            }
        }

        boolean isSourceDirty() {
            View vp = this.viewPagerRef != null ? this.viewPagerRef.get() : null;
            if (vp != null && vp.isDirty()) {
                return true;
            }
            View decor = this.decorRef.get();
            if (decor != null && vp == null) {
                View contentRoot = decor.findViewById(android.R.id.content);
                return contentRoot != null && contentRoot.isDirty();
            }
            return false;
        }

        void markDirty() {
            if (!this.blurDirty) {
                this.blurDirty = true;
                View host = this.hostRef.get();
                if (host != null && host.isAttachedToWindow()) {
                    host.invalidate();
                }
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (recordingNavBlur || ProgressiveBlurDrawable.recording) {
                return;
            }
            Rect bounds = getBounds();
            View host = this.hostRef.get();
            if (bounds.isEmpty() || host == null) {
                return;
            }
            int w = bounds.width();
            int h = bounds.height();
            if (w <= 0 || h <= 0) {
                return;
            }

            host.getLocationInWindow(this.hostLoc);
            float minInnerTop = Float.MAX_VALUE;
            float maxInnerBottom = -Float.MAX_VALUE;

            int slotCount = this.tabSlots.size();
            for (int i = 0; i < slotCount; i++) {
                TabRefs slot = this.tabSlots.get(i);
                View tab = slot.tabRef.get();
                if (tab != null && tab.getVisibility() == View.VISIBLE && tab.getWidth() > 0 && tab.getHeight() > 0) {
                    View icon = slot.iconRef != null ? slot.iconRef.get() : null;
                    View text = slot.textRef != null ? slot.textRef.get() : null;
                    computeActivePillVerticalBounds(
                            tab, icon, text, this.density, this.tabLoc, this.childLoc, this.tabTopBot
                    );
                    float relTabY = this.tabLoc[1] - this.hostLoc[1];
                    float innerTop = relTabY + this.tabTopBot[0];
                    float innerBottom = relTabY + this.tabTopBot[1];
                    if (innerTop < minInnerTop) minInnerTop = innerTop;
                    if (innerBottom > maxInnerBottom) maxInnerBottom = innerBottom;
                }
            }

            computeOuterPillHorizontalBounds(this.decorRef.get(), w, slotCount, this.density, this.leftRight);
            float left = this.leftRight[0];
            float right = this.leftRight[1];
            float concentricGap = this.density * 5.0f;
            float top;
            float bottom;
            if (minInnerTop < maxInnerBottom) {
                top = minInnerTop - concentricGap;
                bottom = maxInnerBottom + concentricGap;
            } else {
                bottom = h - this.density * 2.0f;
                top = bottom - this.density * 52.0f;
            }
            if (right - left <= this.density * 40.0f || bottom - top <= this.density * 24.0f) {
                return;
            }
            this.lastPillTop = top;

            this.pillRect.set(left, top, right, bottom);
            float radius = Math.min(this.pillRect.width(), this.pillRect.height()) * 0.5f;

            boolean powerSave = BlurBars.powerSave();
            boolean canBlur = !powerSave && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated();

            ensureShaders(left, top, right, bottom, radius, canBlur);

            // Clip out the pill interior and draw a soft, natural, low-opacity exterior drop shadow.
            int shadowSave = canvas.save();
            if (Build.VERSION.SDK_INT >= 26) {
                canvas.clipOutPath(this.clipPath);
            }
            int stepAlpha = this.dark ? 0x08 : 0x03;
            for (int step = 6; step >= 1; step--) {
                float spread = step * this.density * 1.9f;
                float dy = step * this.density * 0.75f;
                this.shadowRect.set(
                        left - spread * 0.6f,
                        top + dy * 0.25f - spread * 0.2f,
                        right + spread * 0.6f,
                        bottom + dy + spread * 0.65f
                );
                this.shadowPaint.setColor(withAlpha(0x000000, stepAlpha));
                float sr = radius + spread * 0.6f;
                canvas.drawRoundRect(this.shadowRect, sr, sr, this.shadowPaint);
            }
            canvas.restoreToCount(shadowSave);

            float extraStretchFactor = 0f;
            if (this.animating) {
                long elapsed = SystemClock.uptimeMillis() - this.animStartUptimeMs;
                float rawT = Math.min(1.0f, Math.max(0.0f, (float) elapsed / SLIDE_DURATION_MS));
                float eased = SLOT_INTERPOLATOR.getInterpolation(rawT);
                this.animatedSlot = this.animFromSlot + (this.animToSlot - this.animFromSlot) * eased;
                float bell = 4.0f * rawT * (1.0f - rawT);
                float travel = Math.min(2.0f, Math.abs(this.animToSlot - this.animFromSlot));
                extraStretchFactor = 0.14f * bell * (0.65f + 0.35f * travel);
                if (rawT >= 1.0f) {
                    this.animatedSlot = this.animToSlot;
                    this.animating = false;
                }
            }

            int save = canvas.save();
            canvas.clipPath(this.clipPath);
            try {
                if (canBlur) {
                    View decor = this.decorRef.get();
                    if (decor != null) {
                        if (this.gpu == null) {
                            this.gpu = new NavBlurGpu();
                        }
                        View viewPager = this.viewPagerRef != null ? this.viewPagerRef.get() : null;
                        View mainTab = this.mainTabRef != null ? this.mainTabRef.get() : null;
                        View bnbBg = this.bnbBgRef != null ? this.bnbBgRef.get() : null;
                        View sysNavBg = this.sysNavBgRef != null ? this.sysNavBgRef.get() : null;
                        int pillWinX = Math.round(this.hostLoc[0] + left);
                        int pillWinY = Math.round(this.hostLoc[1] + top);
                        int pillW = Math.max(1, Math.round(right - left));
                        int pillH = Math.max(1, Math.round(bottom - top));
                        canvas.save();
                        canvas.translate(left, top);
                        ((NavBlurGpu) this.gpu).draw(
                                canvas, decor, viewPager, mainTab, host, bnbBg, sysNavBg,
                                this.rawTabRefs, this.pageBgColor, pillWinX, pillWinY,
                                pillW, pillH, this.density, this.blurDirty
                        );
                        canvas.restore();
                    }
                }
                this.blurDirty = false;
                canvas.drawRect(this.pillRect, this.fillPaint);

                // Draw the Material Design 3 sliding tonal capsule behind the selected tab slot.
                if (slotCount > 0 && (this.activeFillColor >>> 24) != 0) {
                    float trackLeft = left + concentricGap;
                    float trackRight = right - concentricGap;
                    float trackW = trackRight - trackLeft;
                    float slotW = trackW / slotCount;
                    float slotPos = this.animatedSlot >= 0f ? this.animatedSlot : 0f;
                    float activeCenterX = trackLeft + (slotPos + 0.5f) * slotW;
                    float baseInnerW = Math.min(slotW - this.density * 2.5f, this.density * 126.0f);
                    float curInnerW = Math.min(trackW, baseInnerW + slotW * extraStretchFactor);
                    float actLeft = Math.max(trackLeft, Math.min(trackRight - curInnerW, activeCenterX - curInnerW * 0.5f));
                    float actRight = actLeft + curInnerW;
                    float actTop = top + concentricGap;
                    float actBottom = bottom - concentricGap;
                    if (actRight > actLeft && actBottom > actTop) {
                        this.activeRect.set(actLeft, actTop, actRight, actBottom);
                        float actRadius = Math.min(this.activeRect.width(), this.activeRect.height()) * 0.5f;
                        canvas.drawRoundRect(this.activeRect, actRadius, actRadius, this.activeFillPaint);
                    }
                }
            } finally {
                canvas.restoreToCount(save);
            }

            float innerRimRadius = Math.max(0f, Math.min(this.innerRimRect.width(), this.innerRimRect.height()) * 0.5f);
            canvas.drawRoundRect(this.innerRimRect, innerRimRadius, innerRimRadius, this.innerRimPaint);
            float rimRadius = Math.max(0f, Math.min(this.rimRect.width(), this.rimRect.height()) * 0.5f);
            canvas.drawRoundRect(this.rimRect, rimRadius, rimRadius, this.rimPaint);

            if (this.animating) {
                host.postInvalidateOnAnimation();
            }
        }

        private void ensureShaders(float left, float top, float right, float bottom, float radius, boolean canBlur) {
            int styleKey = ((this.surfaceRgb & 0x00FFFFFF) ^ ((this.rimAccentRgb & 0x00FFFFFF) * 31))
                    ^ (this.dark ? 0x40000000 : 0)
                    ^ (canBlur ? 0x20000000 : 0);
            if (left == this.cachedLeft
                    && top == this.cachedTop
                    && right == this.cachedRight
                    && bottom == this.cachedBottom
                    && styleKey == this.cachedStyleKey) {
                return;
            }
            this.cachedLeft = left;
            this.cachedTop = top;
            this.cachedRight = right;
            this.cachedBottom = bottom;
            this.cachedStyleKey = styleKey;
            this.blurDirty = true;

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
            int topFill = withAlpha(blend(this.surfaceRgb, 0xFFFFFFFF, this.dark ? 0.05f : 0.35f), alphaTop);
            int botFill = withAlpha(this.surfaceRgb, alphaBottom);
            this.fillPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{topFill, botFill},
                    null,
                    Shader.TileMode.CLAMP
            ));

            float outerStroke = this.rimPaint.getStrokeWidth();
            float outerInset = outerStroke * 0.5f;
            this.rimRect.set(left + outerInset, top + outerInset, right - outerInset, bottom - outerInset);

            float innerInset = outerStroke + this.innerRimPaint.getStrokeWidth() * 0.5f;
            this.innerRimRect.set(left + innerInset, top + innerInset, right - innerInset, bottom - innerInset);

            if (this.dark) {
                this.innerRimPaint.setShader(new LinearGradient(
                        left, top, left, bottom,
                        new int[]{0x38FFFFFF, 0x0EFFFFFF, 0x1AFFFFFF},
                        new float[]{0f, 0.48f, 1f},
                        Shader.TileMode.CLAMP
                ));
                int topRim = withAlpha(blend(0xFFFFFFFF, this.rimAccentRgb, 0.20f), 0x26);
                int midRim = withAlpha(this.rimAccentRgb, 0x14);
                int botRim = 0x10FFFFFF;
                this.rimPaint.setShader(new LinearGradient(
                        left, top, left, bottom,
                        new int[]{topRim, midRim, botRim},
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP
                ));
            } else {
                this.innerRimPaint.setShader(new LinearGradient(
                        left, top, left, bottom,
                        new int[]{0xD4FFFFFF, 0x50FFFFFF, 0x88FFFFFF},
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP
                ));
                int hairlineBase = blend(0xFF0F1C22, this.rimAccentRgb, 0.25f);
                int topRim = withAlpha(hairlineBase, 0x0C);
                int midRim = withAlpha(hairlineBase, 0x10);
                int botRim = withAlpha(hairlineBase, 0x15);
                this.rimPaint.setShader(new LinearGradient(
                        left, top, left, bottom,
                        new int[]{topRim, midRim, botRim},
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP
                ));
            }
        }

        @Override
        public void setAlpha(int alpha) {
            this.fillPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            this.fillPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class NavBlurGpu {
        private final RenderNode node = new RenderNode("NavPillBlur");
        private final int[] childLoc = new int[2];
        private RenderEffect effect;
        private int lastW;
        private int lastH;

        void draw(
                Canvas canvas,
                View decor,
                View viewPager,
                View mainTab,
                View navHost,
                View bnbBg,
                View sysNavBg,
                List<WeakReference<View>> tabRefs,
                int pageBgColor,
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
                FloatingNavPillDrawable.recordingNavBlur = true;
                try {
                    rc.drawColor(pageBgColor != 0 ? pageBgColor : 0xFFFFFFFF);
                    if (viewPager != null
                            && viewPager.getVisibility() == View.VISIBLE
                            && viewPager.isAttachedToWindow()
                            && !BlurBars.isSelfOrAncestor(viewPager, navHost)) {
                        viewPager.getLocationInWindow(this.childLoc);
                        int s = rc.save();
                        rc.translate(this.childLoc[0] - pillWinX, this.childLoc[1] - pillWinY);
                        try {
                            viewPager.draw(rc);
                        } catch (Throwable ignored) {
                        } finally {
                            rc.restoreToCount(s);
                        }
                    } else {
                        View navBarRoot = resolveNavBarRoot(navHost, density);
                        View bnbBgRoot = bnbBg != null ? resolveNavBarRoot(bnbBg, density) : null;
                        View tabsRow = resolveTabsRow(tabRefs, density);
                        View contentRoot = decor.findViewById(android.R.id.content);
                        View sourceRoot = contentRoot != null ? contentRoot : decor;
                        recordBackgroundBranch(
                                rc, sourceRoot, navBarRoot, bnbBgRoot, mainTab, navHost, bnbBg, sysNavBg,
                                tabsRow, tabRefs, pillWinX, pillWinY
                        );
                    }
                } finally {
                    FloatingNavPillDrawable.recordingNavBlur = false;
                    this.node.endRecording();
                }
            }
            if (this.effect == null || sizeChanged) {
                float r = density * 28.0f;
                RenderEffect blur = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP);
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(1.42f);
                this.effect = RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(cm), blur);
                this.node.setRenderEffect(this.effect);
                this.lastW = w;
                this.lastH = h;
            }
            canvas.drawRenderNode(this.node);
        }

        private static View resolveNavBarRoot(View navHost, float density) {
            int maxH = Math.round(density * 165.0f);
            View cur = navHost;
            while (cur.getParent() instanceof ViewGroup) {
                ViewGroup p = (ViewGroup) cur.getParent();
                if (p.getHeight() <= 0 || p.getHeight() > maxH) {
                    break;
                }
                cur = p;
            }
            return cur;
        }

        private static View resolveTabsRow(List<WeakReference<View>> tabRefs, float density) {
            if (tabRefs.isEmpty()) {
                return null;
            }
            View first = tabRefs.get(0).get();
            if (first == null || !(first.getParent() instanceof ViewGroup)) {
                return null;
            }
            ViewGroup p = (ViewGroup) first.getParent();
            int maxH = Math.round(density * 165.0f);
            return (p.getHeight() > 0 && p.getHeight() <= maxH) ? p : null;
        }

        private static boolean isTabView(View view, List<WeakReference<View>> tabRefs) {
            for (int i = 0; i < tabRefs.size(); i++) {
                if (tabRefs.get(i).get() == view) {
                    return true;
                }
            }
            return false;
        }

        private void recordBackgroundBranch(
                Canvas rc,
                View current,
                View navBarRoot,
                View bnbBgRoot,
                View mainTab,
                View navHost,
                View bnbBg,
                View sysNavBg,
                View tabsRow,
                List<WeakReference<View>> tabRefs,
                int pillWinX,
                int pillWinY
        ) {
            if (current == null
                    || current.getVisibility() != View.VISIBLE
                    || current.getAlpha() <= 0f
                    || !current.isAttachedToWindow()) {
                return;
            }
            if (current == navBarRoot
                    || current == bnbBgRoot
                    || current == mainTab
                    || current == navHost
                    || current == bnbBg
                    || current == sysNavBg
                    || current == tabsRow
                    || isTabView(current, tabRefs)) {
                return;
            }
            if (current instanceof ViewGroup && (
                    BlurBars.isSelfOrAncestor(current, navBarRoot)
                            || (bnbBgRoot != null && BlurBars.isSelfOrAncestor(current, bnbBgRoot))
                            || (mainTab != null && BlurBars.isSelfOrAncestor(current, mainTab))
                            || (tabsRow != null && BlurBars.isSelfOrAncestor(current, tabsRow))
            )) {
                Drawable bg = current.getBackground();
                if (bg != null && !(bg instanceof FloatingNavPillDrawable)) {
                    current.getLocationInWindow(this.childLoc);
                    int s = rc.save();
                    rc.translate(this.childLoc[0] - pillWinX, this.childLoc[1] - pillWinY);
                    bg.draw(rc);
                    rc.restoreToCount(s);
                }
                ViewGroup vg = (ViewGroup) current;
                int count = vg.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = vg.getChildAt(i);
                    if (child != null) {
                        recordBackgroundBranch(
                                rc, child, navBarRoot, bnbBgRoot, mainTab, navHost, bnbBg, sysNavBg,
                                tabsRow, tabRefs, pillWinX, pillWinY
                        );
                    }
                }
                return;
            }
            current.getLocationInWindow(this.childLoc);
            int top = this.childLoc[1];
            int bottom = top + current.getHeight();
            if (bottom <= pillWinY - 32 || top >= pillWinY + rc.getHeight() + 32) {
                return;
            }
            int save = rc.save();
            rc.translate(this.childLoc[0] - pillWinX, top - pillWinY);
            try {
                current.draw(rc);
            } catch (Throwable ignored) {
            } finally {
                rc.restoreToCount(save);
            }
        }
    }

    static void applyChatListBadges(View decorView) {
        if (decorView == null || !LineSettings.materialYouTheme()) {
            return;
        }
        Context ctx = decorView.getContext();
        int rvId = BlurBars.id(ctx, "chat_list_recycler_view");
        if (rvId == 0) {
            return;
        }
        View singleList = decorView.findViewById(rvId);
        if (singleList instanceof ViewGroup && singleList.isAttachedToWindow()) {
            tintChatListRows((ViewGroup) singleList, Palette.of(ctx));
        }
    }

    private static void tintChatListRows(ViewGroup list, Palette palette) {
        int count = list.getChildCount();
        for (int i = 0; i < count; i++) {
            View row = list.getChildAt(i);
            if (row == null || row.getVisibility() != View.VISIBLE) {
                continue;
            }
            tintBadge(BlurBars.find(row, "unread_message_count"), palette);
            tintBadgeBg(BlurBars.find(row, "new_icon"), palette);
            View sqLayout = BlurBars.find(row, "square_chat_unread_message_count_layout");
            if (sqLayout != null && sqLayout.getVisibility() == View.VISIBLE) {
                tintBadgeBg(sqLayout, palette);
                View sqText = BlurBars.find(sqLayout, "square_chat_unread_message_count");
                if (sqText instanceof TextView && ((TextView) sqText).getCurrentTextColor() != palette.onBadge) {
                    ((TextView) sqText).setTextColor(palette.onBadge);
                }
            }
        }
    }

    private static void tintBadge(View view, Palette palette) {
        applyBadgeStyle(view, palette.badgeBg, palette.onBadge, 0, 0);
    }

    private static void styleTabBadgeInPill(View view, Palette palette, boolean useMyColors) {
        if (!(view instanceof TextView) || view.getVisibility() != View.VISIBLE) {
            return;
        }
        Context ctx = view.getContext();
        if (!ORIG_NAV_BADGE_TRANSFORM.containsKey(view)) {
            ORIG_NAV_BADGE_TRANSFORM.put(view, new float[]{
                    view.getScaleX(),
                    view.getScaleY(),
                    view.getTranslationY()
            });
        }
        float targetScale = 0.90f;
        float targetTy = BlurBars.dp(ctx, 1.5f);
        if (view.getScaleX() != targetScale) {
            view.setScaleX(targetScale);
        }
        if (view.getScaleY() != targetScale) {
            view.setScaleY(targetScale);
        }
        if (view.getTranslationY() != targetTy) {
            view.setTranslationY(targetTy);
        }
        int bgColor = useMyColors ? palette.badgeBg : 0xFFE03E36;
        int textColor = useMyColors ? palette.onBadge : 0xFFFFFFFF;
        int cutoutStrokeColor = palette.dark ? 0xFF1F242D : 0xFFF7FAFC;
        int strokeWidth = Math.max(2, Math.round(ctx.getResources().getDisplayMetrics().density * 1.4f));
        applyBadgeStyle(view, bgColor, textColor, strokeWidth, cutoutStrokeColor);
    }

    private static void applyBadgeStyle(
            View view,
            int bgColor,
            int textColor,
            int strokeWidth,
            int strokeColor
    ) {
        if (!(view instanceof TextView) || view.getVisibility() != View.VISIBLE) {
            return;
        }
        TextView tv = (TextView) view;
        if (tv.getCurrentTextColor() != textColor) {
            tv.setTextColor(textColor);
        }
        Drawable bg = tv.getBackground();
        int[] pad = ORIG_BADGE_PADDING.get(tv);
        if (pad == null) {
            Rect rect = new Rect();
            boolean hasDrawablePad = bg != null && bg.getPadding(rect)
                    && (rect.left > 0 || rect.right > 0 || rect.top > 0 || rect.bottom > 0);
            Context ctx = tv.getContext();
            int minH = BlurBars.dp(ctx, 5.0f);
            int minV = BlurBars.dp(ctx, 1.0f);
            int pl = Math.max(minH, Math.max(tv.getPaddingLeft(), hasDrawablePad ? rect.left : 0));
            int pt = Math.max(minV, Math.max(tv.getPaddingTop(), hasDrawablePad ? rect.top : 0));
            int pr = Math.max(minH, Math.max(tv.getPaddingRight(), hasDrawablePad ? rect.right : 0));
            int pb = Math.max(minV, Math.max(tv.getPaddingBottom(), hasDrawablePad ? rect.bottom : 0));
            pad = new int[]{pl, pt, pr, pb};
            ORIG_BADGE_PADDING.put(tv, pad);
        }
        if (bg instanceof BadgePillDrawable) {
            BadgePillDrawable pill = (BadgePillDrawable) bg;
            if (pill.appliedColor != bgColor) {
                pill.appliedColor = bgColor;
                pill.setColor(bgColor);
            }
            if (pill.appliedStrokeWidth != strokeWidth || pill.appliedStrokeColor != strokeColor) {
                pill.appliedStrokeWidth = strokeWidth;
                pill.appliedStrokeColor = strokeColor;
                pill.setStroke(strokeWidth, strokeColor);
            }
        } else {
            BadgePillDrawable pill = new BadgePillDrawable();
            pill.setShape(GradientDrawable.RECTANGLE);
            pill.setCornerRadius(BlurBars.dp(tv.getContext(), 999.0f));
            pill.appliedColor = bgColor;
            pill.setColor(bgColor);
            pill.appliedStrokeWidth = strokeWidth;
            pill.appliedStrokeColor = strokeColor;
            if (strokeWidth > 0) {
                pill.setStroke(strokeWidth, strokeColor);
            }
            tv.setBackgroundTintList(null);
            tv.setBackground(pill);
        }
        int extraPad = strokeWidth > 0 ? Math.max(1, strokeWidth / 2) : 0;
        int pL = pad[0] + extraPad;
        int pT = pad[1] + extraPad;
        int pR = pad[2] + extraPad;
        int pB = pad[3] + extraPad;
        if (tv.getPaddingLeft() != pL || tv.getPaddingTop() != pT
                || tv.getPaddingRight() != pR || tv.getPaddingBottom() != pB) {
            tv.setPadding(pL, pT, pR, pB);
        }
    }

    private static void tintBadgeBg(View view, Palette palette) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return;
        }
        Drawable bg = view.getBackground();
        if (bg == null) {
            return;
        }
        ColorStateList currentTint = view.getBackgroundTintList();
        Integer applied = BADGE_APPLIED.get(view);
        if (applied == null
                || applied != palette.badgeBg
                || currentTint == null
                || currentTint.getDefaultColor() != palette.badgeBg
                || bg.getColorFilter() == null) {
            Drawable mutated = bg.mutate();
            if (mutated instanceof GradientDrawable) {
                ((GradientDrawable) mutated).setColor(palette.badgeBg);
            }
            mutated.setColorFilter(palette.badgeBg, PorterDuff.Mode.SRC_IN);
            view.setBackgroundTintMode(PorterDuff.Mode.SRC_IN);
            view.setBackgroundTintList(ColorStateList.valueOf(palette.badgeBg));
            BADGE_APPLIED.put(view, palette.badgeBg);
        }
    }

    // =========================================================================
    // Chat Room Bubbles (Custom Per-Chat / Wallpaper Seed / System Material You)
    // =========================================================================

    static void applyChatRoom(View decorView, View header, View listView, View skinView) {
        if (decorView == null || header == null) {
            return;
        }
        syncActivityUiMode(decorView);
        String chatTitle = "";
        View titleView = BlurBars.find(header, "header_title");
        if (titleView instanceof TextView && ((TextView) titleView).getText() != null) {
            chatTitle = ((TextView) titleView).getText().toString();
        }
        String chatId = resolveChatId(decorView, header, chatTitle);
        if (chatId != null && !chatId.isEmpty()) {
            lastActiveChatId = chatId;
            if (!chatTitle.isEmpty() && !chatId.startsWith("title_") && !"default_chat".equals(chatId)) {
                TITLE_TO_CHAT_ID.put(chatTitle, chatId);
            }
        }
        if (!chatTitle.isEmpty()) {
            lastActiveChatTitle = chatTitle;
        }

        if (skinView == null) {
            skinView = BlurBars.find(decorView, "chat_ui_main_skin_view");
        }
        if (skinView != null) {
            BlurBars.disableForceDarkChainIfLight(skinView);
        }

        int sendRule = readStoredSendRule(chatId, chatTitle);
        int recvRule = readStoredRecvRule(chatId, chatTitle);
        boolean needsWallpaper = LineSettings.chatBubbleWallpaper()
                || sendRule == MODE_WALLPAPER
                || recvRule == MODE_WALLPAPER;
        WallpaperPalette wp = null;
        if (needsWallpaper) {
            boolean dark = isNightResources(decorView.getResources());
            wp = WallpaperPalette.fromSkin(skinView, dark);
        }
        lastActiveWallpaperPalette = wp;

        if (listView instanceof ViewGroup) {
            applyChatBubbles((ViewGroup) listView, sendRule, recvRule, wp);
        }
    }

    private static int readStoredSendRule(String chatId, String chatTitle) {
        int val = LineSettings.chatBubbleSendColor(chatId);
        if (val != 0) {
            return val;
        }
        if (chatTitle != null && !chatTitle.isEmpty()) {
            String mappedId = TITLE_TO_CHAT_ID.get(chatTitle);
            if (mappedId != null && !mappedId.equals(chatId)) {
                int mv = LineSettings.chatBubbleSendColor(mappedId);
                if (mv != 0) {
                    return mv;
                }
            }
            String titleKey = "title_" + chatTitle;
            if (!titleKey.equals(chatId)) {
                int tv = LineSettings.chatBubbleSendColor(titleKey);
                if (tv != 0) {
                    return tv;
                }
            }
        }
        return 0;
    }

    private static int readStoredRecvRule(String chatId, String chatTitle) {
        int val = LineSettings.chatBubbleReceiveColor(chatId);
        if (val != 0) {
            return val;
        }
        if (chatTitle != null && !chatTitle.isEmpty()) {
            String mappedId = TITLE_TO_CHAT_ID.get(chatTitle);
            if (mappedId != null && !mappedId.equals(chatId)) {
                int mv = LineSettings.chatBubbleReceiveColor(mappedId);
                if (mv != 0) {
                    return mv;
                }
            }
            String titleKey = "title_" + chatTitle;
            if (!titleKey.equals(chatId)) {
                int tv = LineSettings.chatBubbleReceiveColor(titleKey);
                if (tv != 0) {
                    return tv;
                }
            }
        }
        return 0;
    }

    private static void savePerChatBubbleRules(String chatId, String chatTitle, int sendRule, int recvRule) {
        LineSettings.setChatBubbleColors(chatId, sendRule, recvRule);
        String activeId = lastActiveChatId;
        if (activeId != null && !activeId.isEmpty() && !"default_chat".equals(activeId) && !activeId.equals(chatId)) {
            LineSettings.setChatBubbleColors(activeId, sendRule, recvRule);
        }
        if (chatTitle != null && !chatTitle.isEmpty()) {
            String titleKey = "title_" + chatTitle;
            if (!titleKey.equals(chatId)) {
                LineSettings.setChatBubbleColors(titleKey, sendRule, recvRule);
            }
            if (chatId != null && !chatId.isEmpty() && !chatId.startsWith("title_") && !"default_chat".equals(chatId)) {
                TITLE_TO_CHAT_ID.put(chatTitle, chatId);
            }
        }
        CHAT_IDS.clear();
        BUBBLE_APPLIED.clear();
        BUBBLE_BG_REF.clear();
        BlurBars.refreshAllChats();
    }

    private static void clearPerChatBubbleRules(String chatId, String chatTitle) {
        LineSettings.clearChatBubbleColors(chatId);
        String activeId = lastActiveChatId;
        if (activeId != null && !activeId.isEmpty() && !"default_chat".equals(activeId)) {
            LineSettings.clearChatBubbleColors(activeId);
        }
        if (chatTitle != null && !chatTitle.isEmpty()) {
            LineSettings.clearChatBubbleColors("title_" + chatTitle);
            String mappedId = TITLE_TO_CHAT_ID.get(chatTitle);
            if (mappedId != null) {
                LineSettings.clearChatBubbleColors(mappedId);
            }
        }
        CHAT_IDS.clear();
        BUBBLE_APPLIED.clear();
        BUBBLE_BG_REF.clear();
        BlurBars.refreshAllChats();
    }

    static int resolveGlobalDefaultBubbleColor(boolean isSend, Palette myPalette) {
        int mode = LineSettings.chatBubbleDefaultMode();
        if (mode == LineSettings.DEFAULT_BUBBLE_CUSTOM) {
            int custom = isSend
                    ? LineSettings.chatBubbleDefaultSendColor()
                    : LineSettings.chatBubbleDefaultReceiveColor();
            return (custom & 0xFF000000) == 0xFF000000 ? custom : 0;
        }
        if (mode == LineSettings.DEFAULT_BUBBLE_MATERIAL_YOU) {
            return myPalette != null ? (isSend ? myPalette.sendBubble : myPalette.receiveBubble) : 0;
        }
        return 0;
    }

    private static int resolveEffectiveBubbleColor(
            int rule,
            boolean isSend,
            Palette myPalette,
            WallpaperPalette wp
    ) {
        // Explicit opaque custom ARGB color (alpha == 0xFF)
        if ((rule & 0xFF000000) == 0xFF000000) {
            return rule;
        }
        if (rule == MODE_LINE_DEFAULT) {
            return 0;
        }
        if (rule == MODE_WALLPAPER) {
            if (wp != null && wp.valid) {
                return isSend ? wp.sendBubble : wp.receiveBubble;
            }
            return resolveGlobalDefaultBubbleColor(isSend, myPalette);
        }
        if (rule == MODE_MATERIAL_YOU) {
            return myPalette != null ? (isSend ? myPalette.sendBubble : myPalette.receiveBubble) : 0;
        }
        // MODE_AUTO (0): use the chat wallpaper color when an image is set, or the global default bubble color.
        if (LineSettings.chatBubbleWallpaper() && wp != null && wp.valid) {
            return isSend ? wp.sendBubble : wp.receiveBubble;
        }
        return resolveGlobalDefaultBubbleColor(isSend, myPalette);
    }

    static void applyChatBubbles(ViewGroup listView, int sendRule, int recvRule, WallpaperPalette wp) {
        boolean needsPalette = LineSettings.chatBubbleDefaultMode() == LineSettings.DEFAULT_BUBBLE_MATERIAL_YOU
                || sendRule == MODE_MATERIAL_YOU || recvRule == MODE_MATERIAL_YOU
                || sendRule == MODE_WALLPAPER || recvRule == MODE_WALLPAPER;
        Palette palette = needsPalette ? Palette.of(listView.getContext()) : null;

        int sendColor = resolveEffectiveBubbleColor(sendRule, true, palette, wp);
        int recvColor = resolveEffectiveBubbleColor(recvRule, false, palette, wp);

        int childCount = listView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View row = listView.getChildAt(i);
            if (row == null || row.getVisibility() != View.VISIBLE) {
                continue;
            }
            boolean isReceive = BlurBars.find(row, "chat_ui_row_thumbnail") != null;
            int targetColor = isReceive ? recvColor : sendColor;
            if (targetColor == 0) {
                clearRowBubbles(row);
            } else {
                tintRowBubbles(row, targetColor);
            }
        }
    }

    private static void clearRowBubbles(View row) {
        for (String idName : BUBBLE_CONTAINER_IDS) {
            clearBubbleContainer(BlurBars.find(row, idName));
        }
        for (String quoteId : BUBBLE_QUOTE_CONTAINER_IDS) {
            clearBubbleContainer(BlurBars.find(row, quoteId));
        }
    }

    private static void clearBubbleContainer(View container) {
        if (container == null) {
            return;
        }
        boolean hadApplied = BUBBLE_APPLIED.remove(container) != null;
        boolean hadOrigPad = ORIG_BUBBLE_PADDING.remove(container) != null;
        if (!hadApplied && !hadOrigPad) {
            return;
        }
        BUBBLE_BG_REF.remove(container);
        Drawable bg = container.getBackground();
        if (bg != null) {
            bg.clearColorFilter();
            bg.setTintList(null);
        }
        container.setBackgroundTintList(null);

        for (String textId : BUBBLE_TEXT_IDS) {
            View tv = BlurBars.find(container, textId);
            if (tv instanceof TextView) {
                TextView textView = (TextView) tv;
                ColorStateList origText = ORIG_BUBBLE_TEXT_COLORS.remove(textView);
                if (origText != null) {
                    textView.setTextColor(origText);
                }
                ColorStateList origLink = ORIG_BUBBLE_LINK_COLORS.remove(textView);
                if (origLink != null) {
                    textView.setLinkTextColor(origLink);
                }
            }
        }
        for (String subId : BUBBLE_SUBTEXT_IDS) {
            View tv = BlurBars.find(container, subId);
            if (tv instanceof TextView) {
                TextView textView = (TextView) tv;
                ColorStateList origSub = ORIG_BUBBLE_TEXT_COLORS.remove(textView);
                if (origSub != null) {
                    textView.setTextColor(origSub);
                }
            }
        }
        for (String voiceIconId : BUBBLE_VOICE_ICON_IDS) {
            View v = BlurBars.find(container, voiceIconId);
            if (v instanceof ImageView) {
                ((ImageView) v).setImageTintList(null);
            }
        }
        for (String circleId : BUBBLE_ACCENT_CIRCLE_IDS) {
            View circle = BlurBars.find(container, circleId);
            if (circle != null) {
                circle.setBackgroundTintList(null);
            }
        }
    }

    private static ColorStateList bubbleColorStateList(int bubbleColor) {
        ColorStateList cached = BUBBLE_CSL_CACHE.get(bubbleColor);
        if (cached != null) {
            return cached;
        }
        int pressedColor = blend(bubbleColor, 0xFF000000, 0.12f) | 0xFF000000;
        ColorStateList csl = new ColorStateList(
                BUBBLE_STATE_SETS,
                new int[]{pressedColor, pressedColor, pressedColor, bubbleColor}
        );
        if (BUBBLE_CSL_CACHE.size() >= 32) {
            BUBBLE_CSL_CACHE.clear();
        }
        BUBBLE_CSL_CACHE.put(bubbleColor, csl);
        return csl;
    }

    private static void tintRowBubbles(View row, int bubbleColor) {
        boolean lightBubble = isLightColor(bubbleColor);
        int textColor = lightBubble ? 0xFF1A1C1E : 0xFFFFFFFF;
        int subTextColor = lightBubble ? 0xBD1A1C1E : 0xD6FFFFFF;
        int linkColor = lightBubble ? 0xFF0B57D0 : 0xFFA8C7FA;
        int dividerColor = lightBubble ? 0x26000000 : 0x33FFFFFF;
        ColorStateList bubbleCsl = bubbleColorStateList(bubbleColor);

        for (String idName : BUBBLE_CONTAINER_IDS) {
            View bubble = BlurBars.find(row, idName);
            if (bubble == null) {
                continue;
            }
            applyBubbleBackgroundTint(bubble, bubbleColor, bubbleCsl);
            tintBubbleTexts(bubble, bubbleColor, textColor, subTextColor, linkColor, dividerColor);
        }

        int quoteColor = blend(bubbleColor, lightBubble ? 0xFF000000 : 0xFFFFFFFF, 0.08f) | 0xFF000000;
        ColorStateList quoteCsl = ColorStateList.valueOf(quoteColor);
        for (String quoteId : BUBBLE_QUOTE_CONTAINER_IDS) {
            View quote = BlurBars.find(row, quoteId);
            if (quote != null && quote.getVisibility() == View.VISIBLE) {
                applyBubbleBackgroundTint(quote, quoteColor, quoteCsl);
                tintBubbleTexts(quote, quoteColor, textColor, subTextColor, linkColor, dividerColor);
            }
        }
    }

    private static void applyBubbleBackgroundTint(View bubble, int targetColor, ColorStateList csl) {
        Drawable bg = bubble.getBackground();
        if (bg == null) {
            return;
        }
        int[] origPad = ORIG_BUBBLE_PADDING.get(bubble);
        if (origPad == null) {
            origPad = new int[]{
                    bubble.getPaddingLeft(),
                    bubble.getPaddingTop(),
                    bubble.getPaddingRight(),
                    bubble.getPaddingBottom()
            };
            ORIG_BUBBLE_PADDING.put(bubble, origPad);
        }

        Integer prevColor = BUBBLE_APPLIED.get(bubble);
        Drawable prevRef = BUBBLE_BG_REF.get(bubble);
        ColorStateList curViewTint = bubble.getBackgroundTintList();
        boolean needsApply = prevColor == null
                || prevColor != targetColor
                || prevRef != bg
                || bg.getColorFilter() == null
                || curViewTint == null
                || curViewTint.getDefaultColor() != targetColor;

        if (needsApply) {
            Drawable mutated = bg.mutate();
            if (mutated instanceof GradientDrawable) {
                ((GradientDrawable) mutated).setColor(csl);
            }
            mutated.setTintMode(PorterDuff.Mode.SRC_IN);
            mutated.setTintList(csl);
            mutated.setColorFilter(targetColor, PorterDuff.Mode.SRC_IN);

            // Override LINE's XML android:backgroundTint (@color/chat_ui_row_msg_highlight_color_selector src_atop)
            bubble.setBackgroundTintMode(PorterDuff.Mode.SRC_IN);
            bubble.setBackgroundTintList(csl);

            if (bubble.getPaddingLeft() != origPad[0] || bubble.getPaddingTop() != origPad[1]
                    || bubble.getPaddingRight() != origPad[2] || bubble.getPaddingBottom() != origPad[3]) {
                bubble.setPadding(origPad[0], origPad[1], origPad[2], origPad[3]);
            }
            BUBBLE_APPLIED.put(bubble, targetColor);
            BUBBLE_BG_REF.put(bubble, mutated);
        }
    }

    private static int deriveBubbleAccent(Context context, int bubbleColor, int fallbackTextColor) {
        float[] hsv = new float[3];
        Color.colorToHSV(bubbleColor | 0xFF000000, hsv);
        if (hsv[1] >= 0.08f) {
            return Color.HSVToColor(new float[]{
                    hsv[0],
                    Math.min(0.82f, Math.max(0.45f, hsv[1] * 1.8f)),
                    0.48f
            }) | 0xFF000000;
        }
        if (LineSettings.chatBubbleDefaultMode() == LineSettings.DEFAULT_BUBBLE_MATERIAL_YOU) {
            return Palette.forMode(context, false).pillBg;
        }
        return fallbackTextColor;
    }

    private static void tintBubbleTexts(
            View container,
            int bubbleColor,
            int textColor,
            int subTextColor,
            int linkColor,
            int dividerColor
    ) {
        for (String textId : BUBBLE_TEXT_IDS) {
            View tv = BlurBars.find(container, textId);
            if (tv instanceof TextView) {
                TextView textView = (TextView) tv;
                if (!ORIG_BUBBLE_TEXT_COLORS.containsKey(textView) && textView.getTextColors() != null) {
                    ORIG_BUBBLE_TEXT_COLORS.put(textView, textView.getTextColors());
                }
                if (textView.getCurrentTextColor() != textColor) {
                    textView.setTextColor(textColor);
                }
                ColorStateList curLink = textView.getLinkTextColors();
                if (!ORIG_BUBBLE_LINK_COLORS.containsKey(textView) && curLink != null) {
                    ORIG_BUBBLE_LINK_COLORS.put(textView, curLink);
                }
                if (curLink == null || curLink.getDefaultColor() != linkColor) {
                    textView.setLinkTextColor(linkColor);
                }
            }
        }
        for (String subId : BUBBLE_SUBTEXT_IDS) {
            View tv = BlurBars.find(container, subId);
            if (tv instanceof TextView) {
                TextView textView = (TextView) tv;
                if (!ORIG_BUBBLE_TEXT_COLORS.containsKey(textView) && textView.getTextColors() != null) {
                    ORIG_BUBBLE_TEXT_COLORS.put(textView, textView.getTextColors());
                }
                if (textView.getCurrentTextColor() != subTextColor) {
                    textView.setTextColor(subTextColor);
                }
            }
        }
        for (String divId : BUBBLE_DIVIDER_IDS) {
            View divider = BlurBars.find(container, divId);
            if (divider != null && divider.getVisibility() == View.VISIBLE) {
                Drawable divBg = divider.getBackground();
                if (!(divBg instanceof ColorDrawable) || ((ColorDrawable) divBg).getColor() != dividerColor) {
                    divider.setBackgroundColor(dividerColor);
                }
            }
        }
        int voiceIconTint = (textColor == 0xFF1A1C1E)
                ? deriveBubbleAccent(container.getContext(), bubbleColor, textColor)
                : textColor;
        for (String voiceIconId : BUBBLE_VOICE_ICON_IDS) {
            View v = BlurBars.find(container, voiceIconId);
            if (v instanceof ImageView) {
                ImageView iv = (ImageView) v;
                ColorStateList cur = iv.getImageTintList();
                if (cur == null || cur.getDefaultColor() != voiceIconTint) {
                    iv.setImageTintMode(PorterDuff.Mode.SRC_IN);
                    iv.setImageTintList(ColorStateList.valueOf(voiceIconTint));
                }
            }
        }
        int circleAccent = deriveBubbleAccent(container.getContext(), bubbleColor, 0xFF06C755);
        for (String circleId : BUBBLE_ACCENT_CIRCLE_IDS) {
            View circle = BlurBars.find(container, circleId);
            if (circle != null && circle.getVisibility() == View.VISIBLE && circle.getBackground() != null) {
                ColorStateList cur = circle.getBackgroundTintList();
                if (cur == null || cur.getDefaultColor() != circleAccent) {
                    circle.setBackgroundTintMode(PorterDuff.Mode.SRC_IN);
                    circle.setBackgroundTintList(ColorStateList.valueOf(circleAccent));
                }
            }
        }
    }

    // =========================================================================
    // Chat Room ID Resolution (ClassLoader-safe for Parcelable ChatHistoryRequest)
    // =========================================================================

    private static boolean isLineMid(String s) {
        if (s == null || s.length() != 33) {
            return false;
        }
        char prefix = s.charAt(0);
        if (prefix != 'u' && prefix != 'c' && prefix != 'r' && prefix != 'm' && prefix != 's') {
            return false;
        }
        for (int i = 1; i < 33; i++) {
            char ch = s.charAt(i);
            boolean hex = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFrameworkClassName(String clsName) {
        return clsName == null
                || clsName.startsWith("java.")
                || clsName.startsWith("javax.")
                || clsName.startsWith("android.")
                || clsName.startsWith("androidx.")
                || clsName.startsWith("com.android.")
                || clsName.startsWith("kotlin.");
    }

    private static String findLineMidDeep(Object obj, int depth, IdentityHashMap<Object, Boolean> visited) {
        if (obj == null || depth > 4) {
            return null;
        }
        if (obj instanceof String) {
            String str = (String) obj;
            return isLineMid(str) ? str : null;
        }
        if (visited.put(obj, Boolean.TRUE) != null) {
            return null;
        }
        try {
            if (obj instanceof Bundle) {
                Bundle b = (Bundle) obj;
                for (String k : b.keySet()) {
                    String hit = findLineMidDeep(b.get(k), depth + 1, visited);
                    if (hit != null) {
                        return hit;
                    }
                }
                return null;
            }
            Class<?> cls = obj.getClass();
            if (cls.isArray() || cls.isPrimitive() || cls.isEnum() || isFrameworkClassName(cls.getName())) {
                return null;
            }
            // First pass: direct String fields on the class hierarchy
            for (Class<?> c = cls; c != null && !isFrameworkClassName(c.getName()); c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || f.getType() != String.class) {
                        continue;
                    }
                    f.setAccessible(true);
                    Object v = f.get(obj);
                    if (v instanceof String && isLineMid((String) v)) {
                        return (String) v;
                    }
                }
            }
            // Second pass: nested non-framework objects (such as obfuscated ChatHistoryRequest wrappers)
            if (depth < 4) {
                for (Class<?> c = cls; c != null && !isFrameworkClassName(c.getName()); c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields()) {
                        if (Modifier.isStatic(f.getModifiers())) {
                            continue;
                        }
                        Class<?> ft = f.getType();
                        if (ft.isPrimitive() || ft == String.class || ft.isArray() || ft.isEnum()) {
                            continue;
                        }
                        if (ft != Bundle.class && isFrameworkClassName(ft.getName())) {
                            continue;
                        }
                        f.setAccessible(true);
                        String hit = findLineMidDeep(f.get(obj), depth + 1, visited);
                        if (hit != null) {
                            return hit;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String extractChatIdFromIntent(Activity activity) {
        if (activity == null) {
            return null;
        }
        String explicitFallback = null;
        try {
            Intent intent = activity.getIntent();
            if (intent != null) {
                intent.setExtrasClassLoader(activity.getClassLoader());
                for (String key : new String[]{"chatId", "chat_id", "mid", "squareChatMid", "EXTRA_CHAT_ID"}) {
                    String val = intent.getStringExtra(key);
                    if (val != null && !val.isEmpty()) {
                        if (isLineMid(val)) {
                            return val;
                        }
                        if (explicitFallback == null) {
                            explicitFallback = val;
                        }
                    }
                }
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    extras.setClassLoader(activity.getClassLoader());
                    IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
                    String mid = findLineMidDeep(extras, 0, visited);
                    if (mid != null) {
                        return mid;
                    }
                    for (String reqKey : new String[]{"chatHistoryRequest", "request", "CHAT_HISTORY_REQUEST"}) {
                        if (!extras.containsKey(reqKey)) {
                            continue;
                        }
                        Object req = extras.get(reqKey);
                        String id = extractUnobfuscatedChatId(req);
                        if (id != null && !id.isEmpty()) {
                            return id;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // Inspect Activity fields for obfuscated ChatHistoryRequest or ViewModel holders containing a LINE MID
        try {
            IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
            String mid = findLineMidDeep(activity, 1, visited);
            if (mid != null) {
                return mid;
            }
        } catch (Throwable ignored) {
        }
        return explicitFallback;
    }

    private static String extractUnobfuscatedChatId(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            Method m = obj.getClass().getMethod("getChatId");
            if (m.getReturnType() == String.class && m.getParameterCount() == 0) {
                Object res = m.invoke(obj);
                if (res instanceof String && !((String) res).isEmpty()) {
                    return (String) res;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (f.getType() == String.class && "chatId".equals(f.getName())) {
                    f.setAccessible(true);
                    Object res = f.get(obj);
                    if (res instanceof String && !((String) res).isEmpty()) {
                        return (String) res;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Activity resolveActivity(View decorView, View header) {
        if (decorView != null) {
            WeakReference<Activity> ref = DECOR_ACTIVITIES.get(decorView);
            if (ref != null && ref.get() != null) {
                return ref.get();
            }
        }
        if (header != null) {
            Activity act = activityOf(header.getContext());
            if (act != null) {
                return act;
            }
        }
        return decorView != null ? activityOf(decorView.getContext()) : null;
    }

    private static String resolveSettingsChatId(Activity activity) {
        String extracted = extractChatIdFromIntent(activity);
        if (extracted != null && !extracted.isEmpty()) {
            return extracted;
        }
        return (lastActiveChatId != null && !lastActiveChatId.isEmpty()) ? lastActiveChatId : "default_chat";
    }

    private static String resolveChatId(View decorView, View header, String chatTitle) {
        String cached = CHAT_IDS.get(decorView);
        if (cached != null && !cached.isEmpty()) {
            if (!cached.startsWith("title_")) {
                return cached;
            }
            String expectedFallback = (chatTitle != null && !chatTitle.isEmpty()) ? ("title_" + chatTitle) : null;
            if (cached.equals(expectedFallback)) {
                return cached;
            }
        }
        Activity activity = resolveActivity(decorView, header);
        String extracted = extractChatIdFromIntent(activity);
        if (extracted != null && !extracted.isEmpty()) {
            CHAT_IDS.put(decorView, extracted);
            return extracted;
        }
        if (chatTitle != null && !chatTitle.isEmpty()) {
            String mapped = TITLE_TO_CHAT_ID.get(chatTitle);
            if (mapped != null && !mapped.isEmpty()) {
                CHAT_IDS.put(decorView, mapped);
                return mapped;
            }
            String fallback = "title_" + chatTitle;
            CHAT_IDS.put(decorView, fallback);
            return fallback;
        }
        return "default_chat";
    }

    // =========================================================================
    // Chat Room Settings Row & Per-Chat Color / Mode Picker Dialog
    // =========================================================================

    static void attachChatSettingsRow(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        String name = activity.getClass().getName();
        if (!name.endsWith("ChatSettingActivity") && !name.endsWith("ChatSettingsActivity")) {
            return;
        }
        View root = activity.findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup)) {
            return;
        }
        final ViewGroup content = (ViewGroup) root;
        if (content.findViewWithTag(CHAT_SETTINGS_ROW_TAG) != null) {
            return;
        }

        final String chatId = resolveSettingsChatId(activity);
        final String chatTitle = lastActiveChatTitle != null ? lastActiveChatTitle : "";

        content.post(() -> {
            if (activity.isFinishing() || content.findViewWithTag(CHAT_SETTINGS_ROW_TAG) != null) {
                return;
            }
            Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
            boolean zh = locale != null && "zh".equals(locale.getLanguage());
            Palette palette = Palette.of(activity);
            int bgColor = palette.dark ? 0xFF111111 : 0xFFFFFFFF;
            int titleColor = palette.dark ? 0xFFF2F2F2 : 0xFF111111;
            int subColor = palette.dark ? 0xFF9E9E9E : 0xFF767676;

            LinearLayout wrapper = new LinearLayout(activity);
            wrapper.setTag(CHAT_SETTINGS_ROW_TAG);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setBackgroundColor(bgColor);
            wrapper.setElevation(BlurBars.dp(activity, 4.0f));

            View divider = new View(activity);
            divider.setBackgroundColor(palette.dark ? 0x22FFFFFF : 0x1A000000);
            wrapper.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BlurBars.dp(activity, 0.7f))));

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int basePadH = BlurBars.dp(activity, 20.0f);
            int adaptivePadH = PixelNativeEnhancements.computeAdaptiveSidePadding(activity, content.getWidth(), 640.0f);
            int padV = BlurBars.dp(activity, 15.0f);
            row.setPadding(basePadH + adaptivePadH, padV, basePadH + adaptivePadH, padV);
            row.setClickable(true);
            PixelNativeEnhancements.enhanceInteractive(row);
            android.util.TypedValue outValue = new android.util.TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
                row.setBackgroundResource(outValue.resourceId);
            }

            LinearLayout textCol = new LinearLayout(activity);
            textCol.setOrientation(LinearLayout.VERTICAL);
            TextView titleTv = new TextView(activity);
            titleTv.setText(zh ? "聊天氣泡顏色" : "Chat bubble color");
            titleTv.setTextSize(16.0f);
            titleTv.setTextColor(titleColor);
            textCol.addView(titleTv);

            final TextView subTv = new TextView(activity);
            subTv.setTextSize(12.5f);
            subTv.setTextColor(subColor);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = BlurBars.dp(activity, 2.0f);
            textCol.addView(subTv, subLp);

            row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            int dotSize = BlurBars.dp(activity, 18.0f);
            final View recvDot = new View(activity);
            LinearLayout.LayoutParams dotLp1 = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotLp1.setMarginEnd(BlurBars.dp(activity, 6.0f));
            row.addView(recvDot, dotLp1);

            final View sendDot = new View(activity);
            LinearLayout.LayoutParams dotLp2 = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotLp2.setMarginEnd(BlurBars.dp(activity, 10.0f));
            row.addView(sendDot, dotLp2);

            TextView chevron = new TextView(activity);
            chevron.setText("›");
            chevron.setTextSize(22.0f);
            chevron.setTextColor(subColor);
            row.addView(chevron);

            final Runnable refreshDots = () -> {
                Palette p = Palette.of(activity);
                WallpaperPalette wp = lastActiveWallpaperPalette;
                int sendRule = readStoredSendRule(chatId, chatTitle);
                int recvRule = readStoredRecvRule(chatId, chatTitle);
                int effSend = resolveEffectiveBubbleColor(sendRule, true, p, wp);
                int effRecv = resolveEffectiveBubbleColor(recvRule, false, p, wp);
                styleColorDot(activity, recvDot, effRecv != 0 ? effRecv : (p.dark ? 0xFF2B2D31 : 0xFFFFFFFF));
                styleColorDot(activity, sendDot, effSend != 0 ? effSend : LINE_CLASSIC_GREEN);
                subTv.setText(describeBubbleMode(sendRule, recvRule, zh));
            };
            refreshDots.run();

            row.setOnClickListener(v -> {
                PixelNativeEnhancements.tick(v);
                showBubbleColorDialog(activity, chatId, chatTitle, () -> {
                    BUBBLE_APPLIED.clear();
                    BUBBLE_BG_REF.clear();
                    refreshDots.run();
                });
            });

            wrapper.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            wrapper.setOnApplyWindowInsetsListener((v, insets) -> {
                int bottomInset = Build.VERSION.SDK_INT >= 30
                        ? insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                        : insets.getSystemWindowInsetBottom();
                int[] cutouts = PixelNativeEnhancements.safeHorizontalCutoutInsets(v);
                int side = Math.max(
                        PixelNativeEnhancements.computeAdaptiveSidePadding(activity, v.getWidth(), 640.0f),
                        Math.max(cutouts[0], cutouts[1])
                );
                row.setPadding(basePadH + side, padV, basePadH + side, padV);
                v.setPadding(0, 0, 0, bottomInset);
                return insets;
            });

            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );
            content.addView(wrapper, flp);
            wrapper.requestApplyInsets();
        });
    }

    private static String describeBubbleMode(int sendRule, int recvRule, boolean zh) {
        if ((sendRule & 0xFF000000) == 0xFF000000 || (recvRule & 0xFF000000) == 0xFF000000) {
            return zh ? "已套用此聊天室專屬自訂氣泡顏色" : "Custom colors active for this chat";
        }
        if (sendRule == MODE_WALLPAPER) {
            return zh ? "自動抓取此聊天室背景圖片主色" : "Using chat wallpaper seed color";
        }
        if (sendRule == MODE_MATERIAL_YOU) {
            return zh ? "使用 Material You 動態配色" : "Using Material You palette";
        }
        if (sendRule == MODE_LINE_DEFAULT) {
            return zh ? "維持 LINE 原版經典綠色氣泡" : "Using LINE classic green bubbles";
        }
        int defMode = LineSettings.chatBubbleDefaultMode();
        String fallbackLabel;
        if (defMode == LineSettings.DEFAULT_BUBBLE_CUSTOM) {
            fallbackLabel = zh ? "全域自訂色" : "Custom default";
        } else if (defMode == LineSettings.DEFAULT_BUBBLE_MATERIAL_YOU) {
            fallbackLabel = "Material You";
        } else {
            fallbackLabel = zh ? "LINE 經典綠色" : "LINE classic green";
        }
        if (LineSettings.chatBubbleWallpaper()) {
            return zh
                    ? ("自動（有背景圖抓主色，無背景圖用 " + fallbackLabel + "）")
                    : ("Auto (Chat wallpaper or " + fallbackLabel + ")");
        }
        return zh
                ? ("跟隨全域預設（" + fallbackLabel + "）· 點擊可單獨自訂")
                : ("Follows global default (" + fallbackLabel + ")");
    }

    private static void styleColorDot(Context ctx, View dot, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke(Math.max(1, BlurBars.dp(ctx, 1.0f)), 0x33888888);
        dot.setBackground(gd);
    }

    private static void showBubbleColorDialog(Activity activity, String chatId, String chatTitle, Runnable onChanged) {
        Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
        boolean zh = locale != null && "zh".equals(locale.getLanguage());
        final Palette palette = Palette.of(activity);
        final WallpaperPalette wp = lastActiveWallpaperPalette;

        final int[] selectedSend = {readStoredSendRule(chatId, chatTitle)};
        final int[] selectedRecv = {readStoredRecvRule(chatId, chatTitle)};

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = BlurBars.dp(activity, 18.0f);
        root.setPadding(pad, BlurBars.dp(activity, 12.0f), pad, BlurBars.dp(activity, 8.0f));
        scroll.addView(root);

        // Live preview card
        LinearLayout previewBox = new LinearLayout(activity);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        int previewPad = BlurBars.dp(activity, 12.0f);
        previewBox.setPadding(previewPad, previewPad, previewPad, previewPad);
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setCornerRadius(BlurBars.dp(activity, 16.0f));
        previewBg.setColor(palette.dark ? 0xFF1C1F26 : 0xFFE9EEF6);
        previewBox.setBackground(previewBg);

        TextView recvPreview = new TextView(activity);
        recvPreview.setText(zh ? "對方的訊息氣泡預覽" : "Received bubble preview");
        recvPreview.setTextSize(14f);
        recvPreview.setPadding(BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f), BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f));
        LinearLayout.LayoutParams recvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        recvLp.gravity = Gravity.START;
        recvLp.bottomMargin = BlurBars.dp(activity, 8f);
        previewBox.addView(recvPreview, recvLp);

        TextView sendPreview = new TextView(activity);
        sendPreview.setText(zh ? "我的訊息氣泡預覽" : "Sent bubble preview");
        sendPreview.setTextSize(14f);
        sendPreview.setPadding(BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f), BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f));
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sendLp.gravity = Gravity.END;
        previewBox.addView(sendPreview, sendLp);

        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewLp.bottomMargin = BlurBars.dp(activity, 14f);
        root.addView(previewBox, previewLp);

        final View[][] sendSwatches = new View[1][];
        final View[][] recvSwatches = new View[1][];
        final TextView[][] modeChipsHolder = new TextView[1][];

        final Runnable updatePreview = () -> {
            int effSend = resolveEffectiveBubbleColor(selectedSend[0], true, palette, wp);
            int effRecv = resolveEffectiveBubbleColor(selectedRecv[0], false, palette, wp);
            int previewSend = effSend != 0 ? effSend : 0xFF8DE971;
            int previewRecv = effRecv != 0 ? effRecv : (palette.dark ? 0xFF2B2D31 : 0xFFFFFFFF);
            stylePreviewBubble(sendPreview, previewSend, BlurBars.dp(activity, 16f));
            stylePreviewBubble(recvPreview, previewRecv, BlurBars.dp(activity, 16f));
            if (sendSwatches[0] != null) {
                refreshSwatchBorders(activity, sendSwatches[0], selectedSend[0]);
            }
            if (recvSwatches[0] != null) {
                refreshSwatchBorders(activity, recvSwatches[0], selectedRecv[0]);
            }
            if (modeChipsHolder[0] != null) {
                refreshModeChips(activity, modeChipsHolder[0], selectedSend[0], selectedRecv[0], palette);
            }
        };

        // Quick mode selector pills: Auto / Chat Wallpaper / System Material You / LINE Default
        root.addView(sectionLabel(activity, zh ? "配色來源模式" : "Color source mode", palette.accent));
        root.addView(buildModeChipBar(activity, selectedSend, selectedRecv, updatePreview, modeChipsHolder, palette, zh));

        // Sent bubble picker
        LinearLayout sendHeader = new LinearLayout(activity);
        sendHeader.setOrientation(LinearLayout.HORIZONTAL);
        sendHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams slLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slLp.topMargin = BlurBars.dp(activity, 12f);
        TextView sendLabel = sectionLabel(activity, zh ? "我的訊息氣泡自訂色（送出）" : "My bubble custom color (Sent)", palette.accent);
        sendHeader.addView(sendLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView sendDefBtn = new TextView(activity);
        sendDefBtn.setText(zh ? "維持經典綠" : "Classic green");
        sendDefBtn.setTextSize(12f);
        sendDefBtn.setTextColor(palette.dark ? 0xFFB8BDC7 : 0xFF5F6368);
        sendDefBtn.setPadding(BlurBars.dp(activity, 8f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 8f));
        PixelNativeEnhancements.enhanceInteractive(sendDefBtn);
        sendDefBtn.setOnClickListener(v -> {
            PixelNativeEnhancements.tick(v);
            selectedSend[0] = MODE_LINE_DEFAULT;
            updatePreview.run();
        });
        sendHeader.addView(sendDefBtn);
        root.addView(sendHeader, slLp);
        root.addView(buildSwatchGrid(activity, selectedSend, updatePreview, sendSwatches));
        root.addView(buildHexRow(activity, selectedSend, updatePreview, zh));

        // Received bubble picker
        LinearLayout recvHeader = new LinearLayout(activity);
        recvHeader.setOrientation(LinearLayout.HORIZONTAL);
        recvHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlLp.topMargin = BlurBars.dp(activity, 14f);
        TextView recvLabel = sectionLabel(activity, zh ? "對方訊息氣泡自訂色（接收）" : "Their bubble custom color (Received)", palette.accent);
        recvHeader.addView(recvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView recvDefBtn = new TextView(activity);
        recvDefBtn.setText(zh ? "維持原版" : "Default");
        recvDefBtn.setTextSize(12f);
        recvDefBtn.setTextColor(palette.dark ? 0xFFB8BDC7 : 0xFF5F6368);
        recvDefBtn.setPadding(BlurBars.dp(activity, 8f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 8f));
        PixelNativeEnhancements.enhanceInteractive(recvDefBtn);
        recvDefBtn.setOnClickListener(v -> {
            PixelNativeEnhancements.tick(v);
            selectedRecv[0] = MODE_LINE_DEFAULT;
            updatePreview.run();
        });
        recvHeader.addView(recvDefBtn);
        root.addView(recvHeader, rlLp);
        root.addView(buildSwatchGrid(activity, selectedRecv, updatePreview, recvSwatches));
        root.addView(buildHexRow(activity, selectedRecv, updatePreview, zh));

        updatePreview.run();

        String title = zh
                ? (chatTitle.isEmpty() ? "自訂聊天氣泡顏色" : "氣泡顏色 · " + chatTitle)
                : (chatTitle.isEmpty() ? "Custom chat bubble color" : "Bubble color · " + chatTitle);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(scroll)
                .setPositiveButton(zh ? "套用" : "Apply", (d, w) -> {
                    PixelNativeEnhancements.confirm(scroll);
                    savePerChatBubbleRules(chatId, chatTitle, selectedSend[0], selectedRecv[0]);
                    if (onChanged != null) onChanged.run();
                })
                .setNeutralButton(zh ? "重設自動" : "Reset", (d, w) -> {
                    PixelNativeEnhancements.confirm(scroll);
                    clearPerChatBubbleRules(chatId, chatTitle);
                    if (onChanged != null) onChanged.run();
                })
                .setNegativeButton(zh ? "取消" : "Cancel", null)
                .create();
        PixelNativeEnhancements.applyDialogBlur(dialog);
        dialog.show();
        PixelNativeEnhancements.constrainDialogWidth(dialog, 520.0f);
    }

    private static View buildModeChipBar(
            Context ctx,
            final int[] selectedSend,
            final int[] selectedRecv,
            final Runnable updatePreview,
            final TextView[][] holder,
            Palette palette,
            boolean zh
    ) {
        final int[] modes = {MODE_AUTO, MODE_WALLPAPER, MODE_MATERIAL_YOU, MODE_LINE_DEFAULT};
        final String[] labels = zh
                ? new String[]{"自動", "背景主色", "系統 Material You", "LINE 原版"}
                : new String[]{"Auto", "Chat Wallpaper", "Material You", "LINE Default"};

        HorizontalScrollView hsv = new HorizontalScrollView(ctx);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        TextView[] chips = new TextView[modes.length];
        holder[0] = chips;

        int padH = BlurBars.dp(ctx, 12f);
        int padV = BlurBars.dp(ctx, 7f);
        int gap = BlurBars.dp(ctx, 8f);
        for (int i = 0; i < modes.length; i++) {
            final int mode = modes[i];
            TextView chip = new TextView(ctx);
            chip.setText(labels[i]);
            chip.setTextSize(12.5f);
            chip.setPadding(padH, padV, padH, padV);
            chip.setGravity(Gravity.CENTER);
            PixelNativeEnhancements.enhanceInteractive(chip);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i < modes.length - 1) {
                lp.setMarginEnd(gap);
            }
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                PixelNativeEnhancements.tick(v);
                selectedSend[0] = mode;
                selectedRecv[0] = mode;
                updatePreview.run();
            });
            chips[i] = chip;
            bar.addView(chip);
        }
        hsv.addView(bar);
        refreshModeChips(ctx, chips, selectedSend[0], selectedRecv[0], palette);
        return hsv;
    }

    private static void refreshModeChips(Context ctx, TextView[] chips, int sendRule, int recvRule, Palette palette) {
        final int[] modes = {MODE_AUTO, MODE_WALLPAPER, MODE_MATERIAL_YOU, MODE_LINE_DEFAULT};
        float radius = BlurBars.dp(ctx, 999f);
        int strokePx = Math.max(1, BlurBars.dp(ctx, 1f));
        for (int i = 0; i < chips.length; i++) {
            boolean active = (sendRule == modes[i] && recvRule == modes[i]);
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(radius);
            if (active) {
                gd.setColor(palette.accent);
                chips[i].setTextColor(isLightColor(palette.accent) ? 0xFF1A1C1E : 0xFFFFFFFF);
            } else {
                gd.setColor(palette.dark ? 0xFF252830 : 0xFFF1F4F9);
                gd.setStroke(strokePx, palette.dark ? 0x33FFFFFF : 0x22000000);
                chips[i].setTextColor(palette.dark ? 0xFFE2E5EC : 0xFF33363C);
            }
            chips[i].setBackground(gd);
        }
    }

    private static void stylePreviewBubble(TextView tv, int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(radius);
        gd.setColor(color);
        gd.setStroke(Math.max(1, BlurBars.dp(tv.getContext(), 0.8f)), 0x22888888);
        tv.setBackground(gd);
        tv.setTextColor(isLightColor(color) ? 0xFF1A1C1E : 0xFFFFFFFF);
    }

    private static TextView sectionLabel(Context ctx, String text, int accent) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(accent);
        tv.setPadding(0, 0, 0, BlurBars.dp(ctx, 8f));
        return tv;
    }

    private static LinearLayout buildSwatchGrid(
            Context ctx,
            final int[] target,
            final Runnable updatePreview,
            final View[][] swatchHolder
    ) {
        LinearLayout grid = new LinearLayout(ctx);
        grid.setOrientation(LinearLayout.VERTICAL);
        View[] swatches = new View[PRESET_COLORS.length];
        swatchHolder[0] = swatches;

        int cols = 6;
        int size = BlurBars.dp(ctx, 36f);
        int gap = BlurBars.dp(ctx, 8f);
        LinearLayout row = null;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (i % cols == 0) {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = gap;
                grid.addView(row, rlp);
            }
            final int color = PRESET_COLORS[i];
            View circle = new View(ctx);
            PixelNativeEnhancements.enhanceInteractive(circle);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(size, size);
            clp.setMarginEnd(gap);
            circle.setLayoutParams(clp);
            swatches[i] = circle;
            circle.setOnClickListener(v -> {
                PixelNativeEnhancements.tick(v);
                target[0] = color;
                updatePreview.run();
            });
            row.addView(circle);
        }
        refreshSwatchBorders(ctx, swatches, target[0]);
        return grid;
    }

    private static void refreshSwatchBorders(Context ctx, View[] swatches, int selectedColor) {
        int strokeSelected = BlurBars.dp(ctx, 2.8f);
        int strokeNormal = Math.max(1, BlurBars.dp(ctx, 1.0f));
        for (int i = 0; i < swatches.length; i++) {
            int color = PRESET_COLORS[i];
            boolean isSelected = selectedColor == color;
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(color);
            gd.setStroke(isSelected ? strokeSelected : strokeNormal, isSelected ? 0xFF1A73E8 : 0x44888888);
            swatches[i].setBackground(gd);
        }
    }

    private static LinearLayout buildHexRow(
            Context ctx,
            final int[] target,
            final Runnable updatePreview,
            boolean zh
    ) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        EditText hexInput = new EditText(ctx);
        hexInput.setHint(zh ? "自訂色碼，例如 #FF6B9D" : "Custom hex, e.g. #FF6B9D");
        hexInput.setSingleLine(true);
        hexInput.setInputType(InputType.TYPE_CLASS_TEXT);
        hexInput.setTextSize(13f);
        if ((target[0] & 0xFF000000) == 0xFF000000) {
            hexInput.setText(String.format(Locale.US, "#%06X", 0xFFFFFF & target[0]));
        }
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!hexInput.hasFocus() || s == null) {
                    return;
                }
                String raw = s.toString().trim();
                if (raw.startsWith("#")) {
                    raw = raw.substring(1);
                }
                if (raw.length() == 6) {
                    try {
                        int parsed = Color.parseColor("#" + raw) | 0xFF000000;
                        if (target[0] != parsed) {
                            target[0] = parsed;
                            updatePreview.run();
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        });
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(hexInput, elp);

        TextView applyBtn = new TextView(ctx);
        applyBtn.setText(zh ? "套用色碼" : "Set");
        applyBtn.setTextSize(13f);
        applyBtn.setTextColor(accentColor(ctx));
        int bp = BlurBars.dp(ctx, 10f);
        applyBtn.setPadding(bp, bp, bp, bp);
        PixelNativeEnhancements.enhanceInteractive(applyBtn);
        applyBtn.setOnClickListener(v -> {
            String raw = hexInput.getText().toString().trim();
            if (!raw.startsWith("#")) raw = "#" + raw;
            try {
                int parsed = Color.parseColor(raw);
                target[0] = parsed | 0xFF000000;
                PixelNativeEnhancements.tick(v);
                updatePreview.run();
            } catch (IllegalArgumentException e) {
                Toast.makeText(ctx, zh ? "色碼格式錯誤（例如 #FF6B9D）" : "Invalid hex color (e.g. #FF6B9D)", Toast.LENGTH_SHORT).show();
            }
        });
        row.addView(applyBtn);
        return row;
    }

    static int defaultBubblePreviewDotColor(Context context) {
        int mode = LineSettings.chatBubbleDefaultMode();
        if (mode == LineSettings.DEFAULT_BUBBLE_CUSTOM) {
            int send = LineSettings.chatBubbleDefaultSendColor();
            if ((send & 0xFF000000) == 0xFF000000) {
                return send;
            }
            int recv = LineSettings.chatBubbleDefaultReceiveColor();
            if ((recv & 0xFF000000) == 0xFF000000) {
                return recv;
            }
            return LINE_CLASSIC_GREEN;
        }
        if (mode == LineSettings.DEFAULT_BUBBLE_MATERIAL_YOU) {
            return Palette.of(context).sendBubble;
        }
        return LINE_CLASSIC_GREEN;
    }

    static void showDefaultBubbleColorDialog(Activity activity, Runnable onChanged) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
        final boolean zh = locale != null && "zh".equals(locale.getLanguage());
        final Palette palette = Palette.of(activity);
        final boolean dark = palette.dark;

        final int[] selectedMode = {LineSettings.chatBubbleDefaultMode()};
        final int[] customSend = {LineSettings.chatBubbleDefaultSendColor()};
        final int[] customRecv = {LineSettings.chatBubbleDefaultReceiveColor()};

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = BlurBars.dp(activity, 18.0f);
        root.setPadding(pad, BlurBars.dp(activity, 12.0f), pad, BlurBars.dp(activity, 8.0f));
        scroll.addView(root);

        // Live preview card with the default LINE sky-blue backdrop in light mode.
        LinearLayout previewBox = new LinearLayout(activity);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        int previewPad = BlurBars.dp(activity, 14.0f);
        previewBox.setPadding(previewPad, previewPad, previewPad, previewPad);
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setCornerRadius(BlurBars.dp(activity, 16.0f));
        previewBg.setColor(dark ? 0xFF1A1D24 : 0xFF8CABD9);
        previewBg.setStroke(Math.max(1, BlurBars.dp(activity, 1f)), dark ? 0x26FFFFFF : 0x1A000000);
        previewBox.setBackground(previewBg);

        final TextView recvPreview = new TextView(activity);
        recvPreview.setText(zh ? "對方的訊息氣泡預覽" : "Received bubble preview");
        recvPreview.setTextSize(14f);
        recvPreview.setPadding(BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f), BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f));
        LinearLayout.LayoutParams recvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        recvLp.gravity = Gravity.START;
        recvLp.bottomMargin = BlurBars.dp(activity, 8f);
        previewBox.addView(recvPreview, recvLp);

        final TextView sendPreview = new TextView(activity);
        sendPreview.setText(zh ? "我的訊息氣泡預覽" : "Sent bubble preview");
        sendPreview.setTextSize(14f);
        sendPreview.setPadding(BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f), BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f));
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sendLp.gravity = Gravity.END;
        previewBox.addView(sendPreview, sendLp);

        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewLp.bottomMargin = BlurBars.dp(activity, 14f);
        root.addView(previewBox, previewLp);

        // Mode chips: LINE Classic Green / Custom Color / Follow Material You
        root.addView(sectionLabel(activity, zh ? "預設氣泡配色模式" : "Default bubble mode", palette.accent));

        final int[] modeValues = {
                LineSettings.DEFAULT_BUBBLE_LINE_CLASSIC,
                LineSettings.DEFAULT_BUBBLE_CUSTOM,
                LineSettings.DEFAULT_BUBBLE_MATERIAL_YOU,
        };
        final String[] modeLabels = zh
                ? new String[]{"LINE 經典綠色", "自訂顏色", "跟隨 Material You"}
                : new String[]{"LINE classic green", "Custom color", "Follow Material You"};

        HorizontalScrollView chipScroll = new HorizontalScrollView(activity);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipBar = new LinearLayout(activity);
        chipBar.setOrientation(LinearLayout.HORIZONTAL);
        final TextView[] modeChips = new TextView[modeValues.length];
        int chipPadH = BlurBars.dp(activity, 12f);
        int chipPadV = BlurBars.dp(activity, 7f);
        int chipGap = BlurBars.dp(activity, 8f);
        for (int i = 0; i < modeValues.length; i++) {
            TextView chip = new TextView(activity);
            chip.setText(modeLabels[i]);
            chip.setTextSize(12.5f);
            chip.setPadding(chipPadH, chipPadV, chipPadH, chipPadV);
            chip.setGravity(Gravity.CENTER);
            PixelNativeEnhancements.enhanceInteractive(chip);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i < modeValues.length - 1) {
                clp.setMarginEnd(chipGap);
            }
            chip.setLayoutParams(clp);
            modeChips[i] = chip;
            chipBar.addView(chip);
        }
        chipScroll.addView(chipBar);
        root.addView(chipScroll);

        final View[][] sendSwatches = new View[1][];
        final View[][] recvSwatches = new View[1][];

        final Runnable refreshDialogUi = () -> {
            int previewSend;
            int previewRecv;
            if (selectedMode[0] == LineSettings.DEFAULT_BUBBLE_MATERIAL_YOU) {
                previewSend = palette.sendBubble;
                previewRecv = palette.receiveBubble;
            } else if (selectedMode[0] == LineSettings.DEFAULT_BUBBLE_CUSTOM) {
                previewSend = (customSend[0] & 0xFF000000) == 0xFF000000 ? customSend[0] : LINE_CLASSIC_GREEN;
                previewRecv = (customRecv[0] & 0xFF000000) == 0xFF000000 ? customRecv[0] : (dark ? 0xFF2B2D31 : 0xFFFFFFFF);
            } else {
                previewSend = LINE_CLASSIC_GREEN;
                previewRecv = dark ? 0xFF2B2D31 : 0xFFFFFFFF;
            }
            stylePreviewBubble(sendPreview, previewSend, BlurBars.dp(activity, 16f));
            stylePreviewBubble(recvPreview, previewRecv, BlurBars.dp(activity, 16f));

            float radius = BlurBars.dp(activity, 999f);
            int strokePx = Math.max(1, BlurBars.dp(activity, 1f));
            for (int i = 0; i < modeChips.length; i++) {
                boolean active = selectedMode[0] == modeValues[i];
                GradientDrawable gd = new GradientDrawable();
                gd.setCornerRadius(radius);
                if (active) {
                    gd.setColor(palette.accent);
                    modeChips[i].setTextColor(isLightColor(palette.accent) ? 0xFF1A1C1E : 0xFFFFFFFF);
                } else {
                    gd.setColor(dark ? 0xFF252830 : 0xFFF1F4F9);
                    gd.setStroke(strokePx, dark ? 0x33FFFFFF : 0x22000000);
                    modeChips[i].setTextColor(dark ? 0xFFE2E5EC : 0xFF33363C);
                }
                modeChips[i].setBackground(gd);
            }

            boolean isCustom = selectedMode[0] == LineSettings.DEFAULT_BUBBLE_CUSTOM;
            if (sendSwatches[0] != null) {
                refreshSwatchBorders(activity, sendSwatches[0], isCustom ? customSend[0] : 0);
            }
            if (recvSwatches[0] != null) {
                refreshSwatchBorders(activity, recvSwatches[0], isCustom ? customRecv[0] : 0);
            }
        };

        for (int i = 0; i < modeValues.length; i++) {
            final int mode = modeValues[i];
            modeChips[i].setOnClickListener(v -> {
                PixelNativeEnhancements.tick(v);
                selectedMode[0] = mode;
                if (mode == LineSettings.DEFAULT_BUBBLE_CUSTOM && customSend[0] == 0 && customRecv[0] == 0) {
                    customSend[0] = PRESET_COLORS[0];
                }
                refreshDialogUi.run();
            });
        }

        final Runnable onCustomColorPicked = () -> {
            selectedMode[0] = LineSettings.DEFAULT_BUBBLE_CUSTOM;
            refreshDialogUi.run();
        };

        // Sent custom bubble picker header + optional classic green reset action
        LinearLayout sendHeader = new LinearLayout(activity);
        sendHeader.setOrientation(LinearLayout.HORIZONTAL);
        sendHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams shLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shLp.topMargin = BlurBars.dp(activity, 12f);
        TextView sendLabel = sectionLabel(activity, zh ? "自訂送出氣泡顏色" : "Custom sent bubble color", palette.accent);
        sendHeader.addView(sendLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView resetSendBtn = new TextView(activity);
        resetSendBtn.setText(zh ? "維持經典綠" : "Classic green");
        resetSendBtn.setTextSize(12f);
        resetSendBtn.setTextColor(dark ? 0xFFB8BDC7 : 0xFF5F6368);
        resetSendBtn.setPadding(BlurBars.dp(activity, 8f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 8f));
        PixelNativeEnhancements.enhanceInteractive(resetSendBtn);
        resetSendBtn.setOnClickListener(v -> {
            PixelNativeEnhancements.tick(v);
            customSend[0] = 0;
            refreshDialogUi.run();
        });
        sendHeader.addView(resetSendBtn);
        root.addView(sendHeader, shLp);
        root.addView(buildSwatchGrid(activity, customSend, onCustomColorPicked, sendSwatches));
        root.addView(buildHexRow(activity, customSend, onCustomColorPicked, zh));

        // Received custom bubble picker header + optional default white/dark reset action
        LinearLayout recvHeader = new LinearLayout(activity);
        recvHeader.setOrientation(LinearLayout.HORIZONTAL);
        recvHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rhLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rhLp.topMargin = BlurBars.dp(activity, 14f);
        TextView recvLabel = sectionLabel(activity, zh ? "自訂接收氣泡顏色" : "Custom received bubble color", palette.accent);
        recvHeader.addView(recvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView resetRecvBtn = new TextView(activity);
        resetRecvBtn.setText(zh ? "維持原版" : "Default");
        resetRecvBtn.setTextSize(12f);
        resetRecvBtn.setTextColor(dark ? 0xFFB8BDC7 : 0xFF5F6368);
        resetRecvBtn.setPadding(BlurBars.dp(activity, 8f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 8f));
        PixelNativeEnhancements.enhanceInteractive(resetRecvBtn);
        resetRecvBtn.setOnClickListener(v -> {
            PixelNativeEnhancements.tick(v);
            customRecv[0] = 0;
            refreshDialogUi.run();
        });
        recvHeader.addView(resetRecvBtn);
        root.addView(recvHeader, rhLp);
        root.addView(buildSwatchGrid(activity, customRecv, onCustomColorPicked, recvSwatches));
        root.addView(buildHexRow(activity, customRecv, onCustomColorPicked, zh));

        refreshDialogUi.run();

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(zh ? "預設聊天氣泡配色" : "Default chat bubble color")
                .setView(scroll)
                .setPositiveButton(zh ? "套用" : "Apply", (d, w) -> {
                    PixelNativeEnhancements.confirm(scroll);
                    int modeToSave = selectedMode[0];
                    if (modeToSave == LineSettings.DEFAULT_BUBBLE_CUSTOM && customSend[0] == 0 && customRecv[0] == 0) {
                        modeToSave = LineSettings.DEFAULT_BUBBLE_LINE_CLASSIC;
                    }
                    LineSettings.setChatBubbleDefaultMode(modeToSave);
                    LineSettings.setChatBubbleDefaultColors(customSend[0], customRecv[0]);
                    BUBBLE_APPLIED.clear();
                    BUBBLE_BG_REF.clear();
                    BlurBars.refreshAllChats();
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNeutralButton(zh ? "經典綠色" : "Classic green", (d, w) -> {
                    PixelNativeEnhancements.confirm(scroll);
                    LineSettings.setChatBubbleDefaultMode(LineSettings.DEFAULT_BUBBLE_LINE_CLASSIC);
                    LineSettings.setChatBubbleDefaultColors(0, 0);
                    BUBBLE_APPLIED.clear();
                    BUBBLE_BG_REF.clear();
                    BlurBars.refreshAllChats();
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNegativeButton(zh ? "取消" : "Cancel", null)
                .create();
        PixelNativeEnhancements.applyDialogBlur(dialog);
        dialog.show();
        PixelNativeEnhancements.constrainDialogWidth(dialog, 520.0f);
    }

    static void invalidatePaletteCache() {
        Palette.cachedLight = null;
        Palette.cachedDark = null;
        Palette.cachedKeyColor = 0;
        Palette.cachedCustomSeed = 0;
        BUBBLE_APPLIED.clear();
        BUBBLE_BG_REF.clear();
        NAV_APPLIED.clear();
        BADGE_APPLIED.clear();
        BUBBLE_CSL_CACHE.clear();
    }

    static void showMaterialYouPaletteDialog(Activity activity, Runnable onChanged) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
        final boolean zh = locale != null && "zh".equals(locale.getLanguage());
        final boolean dark = isNightResources(activity.getResources());

        int savedSeed = LineSettings.materialYouSeedColor();
        final boolean[] customMode = {savedSeed != 0};
        int initialSeed = savedSeed != 0 ? savedSeed : MY_SEED_PRESETS[0];
        final int[] currentSeed = {initialSeed | 0xFF000000};
        final float[] seedHsv = new float[3];
        Color.colorToHSV(currentSeed[0], seedHsv);
        if (seedHsv[2] < 0.25f) {
            seedHsv[2] = 0.72f;
        }

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = BlurBars.dp(activity, 18.0f);
        root.setPadding(pad, BlurBars.dp(activity, 12.0f), pad, BlurBars.dp(activity, 8.0f));
        scroll.addView(root);

        // Live Material You mini-LINE preview card
        LinearLayout previewBox = new LinearLayout(activity);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        int previewPad = BlurBars.dp(activity, 14.0f);
        previewBox.setPadding(previewPad, previewPad, previewPad, previewPad);
        final GradientDrawable previewBg = new GradientDrawable();
        previewBg.setCornerRadius(BlurBars.dp(activity, 18.0f));
        previewBox.setBackground(previewBg);

        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        final TextView headerPill = new TextView(activity);
        headerPill.setText(zh ? "聊天 ▾" : "Chats ▾");
        headerPill.setTextSize(13.5f);
        headerPill.setTypeface(Typeface.DEFAULT_BOLD);
        headerPill.setPadding(BlurBars.dp(activity, 12f), BlurBars.dp(activity, 5f), BlurBars.dp(activity, 12f), BlurBars.dp(activity, 5f));
        headerRow.addView(headerPill);

        final TextView filterChip = new TextView(activity);
        filterChip.setText(zh ? "全部" : "All");
        filterChip.setTextSize(12f);
        filterChip.setPadding(BlurBars.dp(activity, 10f), BlurBars.dp(activity, 4f), BlurBars.dp(activity, 10f), BlurBars.dp(activity, 4f));
        LinearLayout.LayoutParams fcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fcLp.setMarginStart(BlurBars.dp(activity, 8f));
        headerRow.addView(filterChip, fcLp);

        View headerSpacer = new View(activity);
        headerRow.addView(headerSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        final TextView badgeTv = new TextView(activity);
        badgeTv.setText("999+");
        badgeTv.setTextSize(11.5f);
        badgeTv.setTypeface(Typeface.DEFAULT_BOLD);
        badgeTv.setPadding(BlurBars.dp(activity, 9f), BlurBars.dp(activity, 3f), BlurBars.dp(activity, 9f), BlurBars.dp(activity, 3f));
        headerRow.addView(badgeTv);

        LinearLayout.LayoutParams hrLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hrLp.bottomMargin = BlurBars.dp(activity, 10f);
        previewBox.addView(headerRow, hrLp);

        final TextView recvPreview = new TextView(activity);
        recvPreview.setText(zh ? "對方的訊息氣泡" : "Received message");
        recvPreview.setTextSize(13f);
        recvPreview.setPadding(BlurBars.dp(activity, 12f), BlurBars.dp(activity, 7f), BlurBars.dp(activity, 12f), BlurBars.dp(activity, 7f));
        LinearLayout.LayoutParams recvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        recvLp.gravity = Gravity.START;
        recvLp.bottomMargin = BlurBars.dp(activity, 6f);
        previewBox.addView(recvPreview, recvLp);

        final TextView sendPreview = new TextView(activity);
        int bubbleModeForPreview = LineSettings.chatBubbleDefaultMode();
        if (bubbleModeForPreview == LineSettings.DEFAULT_BUBBLE_MATERIAL_YOU) {
            sendPreview.setText(zh ? "我的 Material You 氣泡" : "My Material You bubble");
        } else if (bubbleModeForPreview == LineSettings.DEFAULT_BUBBLE_CUSTOM) {
            sendPreview.setText(zh ? "我的自訂顏色氣泡" : "My custom bubble");
        } else {
            sendPreview.setText(zh ? "LINE 經典綠色氣泡" : "LINE classic green bubble");
        }
        sendPreview.setTextSize(13f);
        sendPreview.setPadding(BlurBars.dp(activity, 12f), BlurBars.dp(activity, 7f), BlurBars.dp(activity, 12f), BlurBars.dp(activity, 7f));
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sendLp.gravity = Gravity.END;
        sendLp.bottomMargin = BlurBars.dp(activity, 10f);
        previewBox.addView(sendPreview, sendLp);

        // Mini floating pill bottom bar preview
        final LinearLayout miniNavPill = new LinearLayout(activity);
        miniNavPill.setOrientation(LinearLayout.HORIZONTAL);
        miniNavPill.setGravity(Gravity.CENTER_VERTICAL);
        int navPad = BlurBars.dp(activity, 4f);
        miniNavPill.setPadding(navPad, navPad, navPad, navPad);
        final TextView navTabHome = new TextView(activity);
        final TextView navTabChat = new TextView(activity);
        final TextView navTabWallet = new TextView(activity);
        String[] navNames = zh ? new String[]{"主頁", "聊天", "錢包"} : new String[]{"Home", "Chats", "Wallet"};
        TextView[] navTabs = {navTabHome, navTabChat, navTabWallet};
        for (int i = 0; i < navTabs.length; i++) {
            navTabs[i].setText(navNames[i]);
            navTabs[i].setTextSize(12f);
            navTabs[i].setGravity(Gravity.CENTER);
            navTabs[i].setPadding(0, BlurBars.dp(activity, 6f), 0, BlurBars.dp(activity, 6f));
            if (i == 1) {
                navTabs[i].setTypeface(Typeface.DEFAULT_BOLD);
            }
            miniNavPill.addView(navTabs[i], new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        LinearLayout.LayoutParams mnpLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mnpLp.setMarginStart(BlurBars.dp(activity, 14f));
        mnpLp.setMarginEnd(BlurBars.dp(activity, 14f));
        previewBox.addView(miniNavPill, mnpLp);

        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewLp.bottomMargin = BlurBars.dp(activity, 14f);
        root.addView(previewBox, previewLp);

        // Source mode chips: System Wallpaper vs Custom Palette
        final TextView modeLabel = sectionLabel(activity, zh ? "調色盤來源" : "Palette source", accentColor(activity));
        root.addView(modeLabel);

        LinearLayout sourceBar = new LinearLayout(activity);
        sourceBar.setOrientation(LinearLayout.HORIZONTAL);
        final TextView sysChip = new TextView(activity);
        final TextView customChip = new TextView(activity);
        sysChip.setText(zh ? "跟隨系統桌布" : "System wallpaper");
        customChip.setText(zh ? "自選調色盤" : "Custom palette");
        TextView[] srcChips = {sysChip, customChip};
        for (int i = 0; i < srcChips.length; i++) {
            srcChips[i].setTextSize(13f);
            srcChips[i].setGravity(Gravity.CENTER);
            srcChips[i].setPadding(BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f), BlurBars.dp(activity, 14f), BlurBars.dp(activity, 8f));
            PixelNativeEnhancements.enhanceInteractive(srcChips[i]);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i == 0) {
                clp.setMarginEnd(BlurBars.dp(activity, 8f));
            }
            sourceBar.addView(srcChips[i], clp);
        }
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.bottomMargin = BlurBars.dp(activity, 14f);
        root.addView(sourceBar, sbLp);

        // Curated Material You tonal preset swatches (2 x 6)
        final TextView presetLabel = sectionLabel(activity, zh ? "精選 Material You 主題色" : "Curated Material You themes", accentColor(activity));
        root.addView(presetLabel);

        LinearLayout presetGrid = new LinearLayout(activity);
        presetGrid.setOrientation(LinearLayout.VERTICAL);
        final View[] presetCircles = new View[MY_SEED_PRESETS.length];
        int cols = 6;
        int swatchSize = BlurBars.dp(activity, 38f);
        int swatchGap = BlurBars.dp(activity, 8f);
        LinearLayout swatchRow = null;
        for (int i = 0; i < MY_SEED_PRESETS.length; i++) {
            if (i % cols == 0) {
                swatchRow = new LinearLayout(activity);
                swatchRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = swatchGap;
                presetGrid.addView(swatchRow, rlp);
            }
            View circle = new View(activity);
            PixelNativeEnhancements.enhanceInteractive(circle);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            clp.setMarginEnd(swatchGap);
            circle.setLayoutParams(clp);
            presetCircles[i] = circle;
            swatchRow.addView(circle);
        }
        LinearLayout.LayoutParams pgLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pgLp.bottomMargin = BlurBars.dp(activity, 10f);
        root.addView(presetGrid, pgLp);

        // Smooth Hue + Chroma sliders
        final TextView sliderLabel = sectionLabel(activity, zh ? "自由調配（色相與彩度）" : "Fine-tune hue & chroma", accentColor(activity));
        root.addView(sliderLabel);

        TextView hueTitle = new TextView(activity);
        hueTitle.setText(zh ? "色相（彩虹色輪）" : "Hue");
        hueTitle.setTextSize(12f);
        hueTitle.setTextColor(dark ? 0xFFB8BDC7 : 0xFF5F6368);
        root.addView(hueTitle);

        final SeekBar hueBar = new SeekBar(activity);
        hueBar.setMax(360);
        hueBar.setProgress(Math.round(seedHsv[0]));
        GradientDrawable rainbowBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF3B30, 0xFFFFCC00, 0xFF34C759, 0xFF00C7BE, 0xFF007AFF, 0xFFAF52DE, 0xFFFF2D55, 0xFFFF3B30}
        );
        rainbowBg.setCornerRadius(BlurBars.dp(activity, 999f));
        hueBar.setBackground(rainbowBg);
        int barPadV = BlurBars.dp(activity, 6f);
        int barPadH = BlurBars.dp(activity, 12f);
        hueBar.setPadding(barPadH, barPadV, barPadH, barPadV);
        LinearLayout.LayoutParams hbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hbLp.topMargin = BlurBars.dp(activity, 4f);
        hbLp.bottomMargin = BlurBars.dp(activity, 10f);
        root.addView(hueBar, hbLp);

        TextView chromaTitle = new TextView(activity);
        chromaTitle.setText(zh ? "彩度（柔和粉彩 ↔ 鮮明濃郁）" : "Chroma (Soft pastel to vibrant)");
        chromaTitle.setTextSize(12f);
        chromaTitle.setTextColor(dark ? 0xFFB8BDC7 : 0xFF5F6368);
        root.addView(chromaTitle);

        final SeekBar chromaBar = new SeekBar(activity);
        chromaBar.setMax(100);
        chromaBar.setProgress(Math.round(seedHsv[1] * 100.0f));
        final GradientDrawable chromaBg = new GradientDrawable();
        chromaBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        chromaBg.setCornerRadius(BlurBars.dp(activity, 999f));
        chromaBar.setBackground(chromaBg);
        chromaBar.setPadding(barPadH, barPadV, barPadH, barPadV);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.topMargin = BlurBars.dp(activity, 4f);
        cbLp.bottomMargin = BlurBars.dp(activity, 12f);
        root.addView(chromaBar, cbLp);

        // Optional Hex row
        LinearLayout hexRow = new LinearLayout(activity);
        hexRow.setOrientation(LinearLayout.HORIZONTAL);
        hexRow.setGravity(Gravity.CENTER_VERTICAL);
        final EditText hexInput = new EditText(activity);
        hexInput.setHint(zh ? "色碼，例如 #6750A4" : "Hex, e.g. #6750A4");
        hexInput.setSingleLine(true);
        hexInput.setInputType(InputType.TYPE_CLASS_TEXT);
        hexInput.setTextSize(13f);
        hexRow.addView(hexInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView hexBtn = new TextView(activity);
        hexBtn.setText(zh ? "套用色碼" : "Set hex");
        hexBtn.setTextSize(13f);
        int bp = BlurBars.dp(activity, 10f);
        hexBtn.setPadding(bp, bp, bp, bp);
        PixelNativeEnhancements.enhanceInteractive(hexBtn);
        hexRow.addView(hexBtn);
        root.addView(hexRow);

        final boolean[] syncingUi = {false};
        final long[] lastTickMs = {0L};

        final Runnable refreshAll = () -> {
            Palette previewPal = customMode[0]
                    ? Palette.fromSeed(currentSeed[0], dark)
                    : Palette.systemPalette(activity, dark);

            previewBg.setColor(dark ? blend(0xFF15181F, previewPal.navBarBg, 0.45f) : blend(0xFFEFF3F9, previewPal.navBarBg, 0.50f));
            previewBg.setStroke(Math.max(1, BlurBars.dp(activity, 1f)), dark ? 0x26FFFFFF : 0x18000000);

            GradientDrawable hpGd = new GradientDrawable();
            hpGd.setCornerRadius(BlurBars.dp(activity, 999f));
            hpGd.setColor(previewPal.pillBg);
            headerPill.setBackground(hpGd);
            headerPill.setTextColor(previewPal.onPill);

            GradientDrawable fcGd = new GradientDrawable();
            fcGd.setCornerRadius(BlurBars.dp(activity, 999f));
            fcGd.setColor(previewPal.pillBg);
            filterChip.setBackground(fcGd);
            filterChip.setTextColor(previewPal.onPill);

            GradientDrawable bgGd = new GradientDrawable();
            bgGd.setCornerRadius(BlurBars.dp(activity, 999f));
            bgGd.setColor(previewPal.badgeBg);
            badgeTv.setBackground(bgGd);
            badgeTv.setTextColor(previewPal.onBadge);

            int effPreviewSend = resolveGlobalDefaultBubbleColor(true, previewPal);
            int effPreviewRecv = resolveGlobalDefaultBubbleColor(false, previewPal);
            stylePreviewBubble(
                    recvPreview,
                    effPreviewRecv != 0 ? effPreviewRecv : (dark ? 0xFF2B2D31 : 0xFFFFFFFF),
                    BlurBars.dp(activity, 14f)
            );
            stylePreviewBubble(
                    sendPreview,
                    effPreviewSend != 0 ? effPreviewSend : LINE_CLASSIC_GREEN,
                    BlurBars.dp(activity, 14f)
            );

            GradientDrawable navOuterGd = new GradientDrawable();
            navOuterGd.setCornerRadius(BlurBars.dp(activity, 999f));
            int outerGlass = dark
                    ? blend(0xFF1A1D24, previewPal.navBarBg, 0.55f)
                    : blend(0xFFFFFFFF, previewPal.navBarBg, 0.36f);
            navOuterGd.setColor(outerGlass);
            navOuterGd.setStroke(Math.max(1, BlurBars.dp(activity, 1f)), dark ? 0x38FFFFFF : 0x55FFFFFF);
            miniNavPill.setBackground(navOuterGd);

            GradientDrawable navActGd = new GradientDrawable();
            navActGd.setCornerRadius(BlurBars.dp(activity, 999f));
            int activeCap = dark
                    ? blend(outerGlass, previewPal.pillBg, 0.28f)
                    : blend(0xFFFFFFFF, previewPal.pillBg, 0.20f);
            navActGd.setColor(activeCap);
            navTabChat.setBackground(navActGd);
            navTabChat.setTextColor(previewPal.navSelected);
            navTabHome.setTextColor(previewPal.navUnselected);
            navTabWallet.setTextColor(previewPal.navUnselected);

            modeLabel.setTextColor(previewPal.accent);
            presetLabel.setTextColor(previewPal.accent);
            sliderLabel.setTextColor(previewPal.accent);
            hexBtn.setTextColor(previewPal.accent);

            float chipRadius = BlurBars.dp(activity, 999f);
            int chipStroke = Math.max(1, BlurBars.dp(activity, 1f));
            boolean[] chipStates = {!customMode[0], customMode[0]};
            for (int i = 0; i < srcChips.length; i++) {
                GradientDrawable gd = new GradientDrawable();
                gd.setCornerRadius(chipRadius);
                if (chipStates[i]) {
                    gd.setColor(previewPal.pillBg);
                    srcChips[i].setTextColor(previewPal.onPill);
                } else {
                    gd.setColor(dark ? 0xFF252830 : 0xFFF1F4F9);
                    gd.setStroke(chipStroke, dark ? 0x33FFFFFF : 0x22000000);
                    srcChips[i].setTextColor(dark ? 0xFFE2E5EC : 0xFF33363C);
                }
                srcChips[i].setBackground(gd);
            }

            int strokeSelected = BlurBars.dp(activity, 3.0f);
            int strokeNormal = Math.max(1, BlurBars.dp(activity, 1.0f));
            for (int i = 0; i < MY_SEED_PRESETS.length; i++) {
                int c = MY_SEED_PRESETS[i];
                boolean sel = customMode[0] && ((currentSeed[0] & 0xFFFFFF) == (c & 0xFFFFFF));
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(c);
                gd.setStroke(
                        sel ? strokeSelected : strokeNormal,
                        sel ? (dark ? 0xFFFFFFFF : 0xFF111111) : 0x44888888
                );
                presetCircles[i].setBackground(gd);
            }

            int pureHueColor = Color.HSVToColor(new float[]{seedHsv[0], 0.90f, 0.80f}) | 0xFF000000;
            chromaBg.setColors(new int[]{0xFF80848A, pureHueColor});
            ColorStateList thumbTint = ColorStateList.valueOf(currentSeed[0]);
            hueBar.setThumbTintList(ColorStateList.valueOf(pureHueColor));
            chromaBar.setThumbTintList(thumbTint);

            if (!syncingUi[0]) {
                syncingUi[0] = true;
                hueBar.setProgress(Math.round(seedHsv[0]));
                chromaBar.setProgress(Math.round(seedHsv[1] * 100.0f));
                hexInput.setText(String.format(Locale.US, "#%06X", 0xFFFFFF & currentSeed[0]));
                syncingUi[0] = false;
            }
        };

        sysChip.setOnClickListener(v -> {
            PixelNativeEnhancements.tick(v);
            customMode[0] = false;
            refreshAll.run();
        });
        customChip.setOnClickListener(v -> {
            PixelNativeEnhancements.tick(v);
            customMode[0] = true;
            refreshAll.run();
        });

        for (int i = 0; i < MY_SEED_PRESETS.length; i++) {
            final int preset = MY_SEED_PRESETS[i];
            presetCircles[i].setOnClickListener(v -> {
                PixelNativeEnhancements.tick(v);
                customMode[0] = true;
                currentSeed[0] = preset | 0xFF000000;
                Color.colorToHSV(currentSeed[0], seedHsv);
                refreshAll.run();
            });
        }

        SeekBar.OnSeekBarChangeListener sliderListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || syncingUi[0]) {
                    return;
                }
                customMode[0] = true;
                seedHsv[0] = hueBar.getProgress();
                seedHsv[1] = chromaBar.getProgress() / 100.0f;
                seedHsv[2] = seedHsv[1] < 0.05f ? 0.42f : 0.72f;
                currentSeed[0] = Color.HSVToColor(seedHsv) | 0xFF000000;
                syncingUi[0] = true;
                hexInput.setText(String.format(Locale.US, "#%06X", 0xFFFFFF & currentSeed[0]));
                refreshAll.run();
                syncingUi[0] = false;
                long now = SystemClock.uptimeMillis();
                if (now - lastTickMs[0] >= 45L) {
                    lastTickMs[0] = now;
                    PixelNativeEnhancements.tick(seekBar);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                PixelNativeEnhancements.tick(seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        hueBar.setOnSeekBarChangeListener(sliderListener);
        chromaBar.setOnSeekBarChangeListener(sliderListener);

        hexBtn.setOnClickListener(v -> {
            String raw = hexInput.getText().toString().trim();
            if (!raw.startsWith("#")) {
                raw = "#" + raw;
            }
            try {
                int parsed = Color.parseColor(raw) | 0xFF000000;
                customMode[0] = true;
                currentSeed[0] = parsed;
                Color.colorToHSV(parsed, seedHsv);
                PixelNativeEnhancements.tick(v);
                refreshAll.run();
            } catch (IllegalArgumentException e) {
                Toast.makeText(
                        activity,
                        zh ? "色碼格式錯誤（例如 #6750A4）" : "Invalid hex color (e.g. #6750A4)",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        refreshAll.run();

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(zh ? "Material You 自選調色盤" : "Material You color palette")
                .setView(scroll)
                .setPositiveButton(zh ? "套用" : "Apply", (d, w) -> {
                    PixelNativeEnhancements.confirm(scroll);
                    int toSave = customMode[0] ? (currentSeed[0] | 0xFF000000) : 0;
                    LineSettings.setMaterialYouSeedColor(toSave);
                    if (toSave != 0 && !LineSettings.materialYouTheme()) {
                        LineSettings.set(LineSettings.MATERIAL_YOU_THEME, true);
                    }
                    invalidatePaletteCache();
                    if (activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
                        activity.getWindow().getDecorView().postInvalidate();
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNeutralButton(zh ? "跟隨桌布" : "Wallpaper", (d, w) -> {
                    PixelNativeEnhancements.confirm(scroll);
                    LineSettings.setMaterialYouSeedColor(0);
                    invalidatePaletteCache();
                    if (activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
                        activity.getWindow().getDecorView().postInvalidate();
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNegativeButton(zh ? "取消" : "Cancel", null)
                .create();
        PixelNativeEnhancements.applyDialogBlur(dialog);
        dialog.show();
        PixelNativeEnhancements.constrainDialogWidth(dialog, 520.0f);
    }

    static boolean isLightColor(int color) {
        return Color.luminance(color | 0xFF000000) > 0.42f;
    }

    static int blend(int base, int overlay, float ratio) {
        float inv = 1.0f - ratio;
        int r = Math.round(Color.red(base) * inv + Color.red(overlay) * ratio);
        int g = Math.round(Color.green(base) * inv + Color.green(overlay) * ratio);
        int b = Math.round(Color.blue(base) * inv + Color.blue(overlay) * ratio);
        return Color.rgb(r, g, b);
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
}
