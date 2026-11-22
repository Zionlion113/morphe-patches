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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

    private static final int[] PRESET_COLORS = {
            0xFFD3E3FD, // Sky Mist
            0xFFC4EED0, // Mint Leaf
            0xFFEADDFF, // Lavender
            0xFFFFD8E4, // Rose Petal
            0xFFFFE0B2, // Warm Apricot
            0xFF1A73E8, // Pixel Blue
            0xFF006D5B, // Deep Teal
            0xFF6750A4, // Royal Violet
            0xFFC2185B, // Berry Pink
            0xFF2B2D31, // Charcoal Glass
            0xFFFFFFFF, // Pure White
    };

    private static final String CHAT_SETTINGS_ROW_TAG = "andrew_chat_setting_bubble_row";

    private static final Map<View, Integer> BUBBLE_APPLIED = new WeakHashMap<>();
    private static final Map<View, Drawable> BUBBLE_BG_REF = new WeakHashMap<>();
    private static final Map<View, int[]> ORIG_BUBBLE_PADDING = new WeakHashMap<>();
    private static final Map<View, int[]> ORIG_BADGE_PADDING = new WeakHashMap<>();
    private static final Map<View, Integer> NAV_APPLIED = new WeakHashMap<>();
    private static final class NavExpandState {
        int extendedBottom;
        int setMarginBottom = Integer.MIN_VALUE;
    }

    private static final Map<View, int[]> ORIG_NAV_PADDING = new WeakHashMap<>();
    private static final Map<View, Integer> ORIG_NAV_HEIGHT = new WeakHashMap<>();
    private static final Map<View, Integer> ORIG_NAV_LIST_PAD = new WeakHashMap<>();
    private static final Map<View, Integer> ORIG_NAV_FRAG_MARGIN = new WeakHashMap<>();
    private static final Map<View, NavExpandState> NAV_EXPAND_STATES = new WeakHashMap<>();
    private static final Map<View, Integer> ORIG_NAV_CONTAINER_PAD_BOT = new WeakHashMap<>();
    private static final Map<View, Drawable> ORIG_NAV_CLEARED_BG = new WeakHashMap<>();
    private static final Map<View, Float> ORIG_NAV_SIBLING_ALPHA = new WeakHashMap<>();
    private static final Map<View, Boolean> CONTENT_BG_APPLIED = new WeakHashMap<>();
    private static final Map<View, WeakReference<View>> NAV_SCROLL_HOST = new WeakHashMap<>();
    private static final Map<View, ViewTreeObserver.OnScrollChangedListener> NAV_SCROLL_LISTENERS = new WeakHashMap<>();
    private static final Map<View, Integer> BADGE_APPLIED = new WeakHashMap<>();
    private static final Map<View, String> CHAT_IDS = new WeakHashMap<>();
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

    static boolean anyEnabled() {
        return LineSettings.materialYouTheme()
                || LineSettings.materialYouNavBar()
                || LineSettings.navBarPill()
                || LineSettings.chatBubbleWallpaper()
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
        private static volatile int cachedKeyColor;

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
                    Palette created;
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
                        created = new Palette(true, hiBg, hiBg, onHi, a1_700, n2_800, navBg, hiBg, n2_300, hiBg, onHi, blur);
                        cachedDark = created;
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
                        created = new Palette(false, pillBg, pillBg, 0xFFFFFFFF, a1_100, recv, navBg, pillBg, n2_500, badgeBg, 0xFFFFFFFF, navBg);
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
            Palette created = dark
                    ? new Palette(true, 0xFFA8C7FA, 0xFFA8C7FA, 0xFF062E6F, 0xFF0842A0, 0xFF282A2F, 0xFF1B2029, 0xFFA8C7FA, 0xFFB0B5BE, 0xFF8AB4F8, 0xFF062E6F, 0xFF181C24)
                    : new Palette(false, 0xFF1A73E8, 0xFF1A73E8, 0xFFFFFFFF, 0xFFD3E3FD, 0xFFF0F4FA, 0xFFEBF2FC, 0xFF1A73E8, 0xFF5F6368, 0xFF1A73E8, 0xFFFFFFFF, 0xFFEBF2FC);
            if (dark) {
                cachedDark = created;
            } else {
                cachedLight = created;
            }
            return created;
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
            Drawable d = skinView instanceof ImageView ? ((ImageView) skinView).getDrawable() : null;
            if (d == null) {
                d = skinView.getBackground();
            }
            if (d == null || d instanceof NinePatchDrawable) {
                SKIN_WALLPAPER_CACHE.remove(skinView);
                return null;
            }
            if (d instanceof ColorDrawable && Color.alpha(((ColorDrawable) d).getColor()) < 32) {
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

        if (pillMode && !visibleTabs.isEmpty()) {
            applyFloatingPillNavBar(
                    decorView, ctx, bnbBg, sysNavBg, tabsParent, pillHost, visibleTabs, palette, styleNavBar
            );
            extendMainContentBehindPill(decorView, ctx, pillHost, bnbBg, sysNavBg, true);
            ensureNavScrollBlurObserver(decorView, pillHost);
        } else {
            restoreNonPillNavBar(decorView, bnbBg, sysNavBg, tabsParent, visibleTabs);
            extendMainContentBehindPill(decorView, ctx, pillHost, bnbBg, sysNavBg, false);
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

        int activePillFill = 0;
        int activePillStroke = 0;
        if (pillMode) {
            if (styleNavBar) {
                activePillFill = palette.dark
                        ? withAlpha(blend(palette.navBarBg, palette.accent, 0.34f), 0xEE)
                        : withAlpha(blend(palette.sendBubble, palette.accent, 0.15f), 0xF0);
                activePillStroke = withAlpha(palette.accent, palette.dark ? 0x42 : 0x30);
            } else {
                activePillFill = palette.dark ? 0x32FFFFFF : 0x1E000000;
                activePillStroke = palette.dark ? 0x26FFFFFF : 0x16000000;
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

            if (pillMode) {
                if (selected) {
                    Drawable curBg = tab.getBackground();
                    ActiveTabPillDrawable ind;
                    if (curBg instanceof ActiveTabPillDrawable) {
                        ind = (ActiveTabPillDrawable) curBg;
                    } else {
                        ind = new ActiveTabPillDrawable(ctx);
                        tab.setBackground(ind);
                    }
                    ind.bindViews(tab, iconView, textView);
                    ind.updateColors(activePillFill, activePillStroke);
                } else if (tab.getBackground() instanceof ActiveTabPillDrawable) {
                    tab.setBackground(null);
                }
            }

            if (styleNavBar || pillMode) {
                int tint;
                if (styleNavBar) {
                    tint = selected ? palette.navSelected : palette.navUnselected;
                } else {
                    tint = selected
                            ? (palette.dark ? 0xFFFFFFFF : 0xFF141619)
                            : (palette.dark ? 0xA6FFFFFF : 0x8E141619);
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
                }
            }
            if (LineSettings.materialYouTheme() || styleNavBar) {
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
            Palette palette,
            boolean styleNavBar
    ) {
        int pageBgColor = resolvePageBackgroundColor(ctx, palette);
        syncSystemNavigationBarBackdrop(decorView, bnbBg, pageBgColor);

        // Paint contentRoot at the back of the hierarchy so DecorView's #000000 never shows on non-scrolling screens.
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

        // Keep main_navigation_bar_background transparent so page content extends behind the gesture bar.
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
        float pillZ = BlurBars.dp(ctx, 2.0f);
        if (pillHost.getTranslationZ() != pillZ) {
            pillHost.setTranslationZ(pillZ);
        }

        // Clear rectangular backgrounds along all bottom-bar branches (pillHost, bnbBg, sysNavBg) and siblings.
        int maxBarWrapperH = BlurBars.dp(ctx, 165.0f);
        int maxDividerH = BlurBars.dp(ctx, 110.0f);
        if (pillHost instanceof ViewGroup) {
            ViewGroup hostVg = (ViewGroup) pillHost;
            hostVg.setClipChildren(false);
            hostVg.setClipToPadding(false);
            hostVg.setClipToOutline(false);
        }
        ViewGroup rootA = clearBottomBarWrapperChain(pillHost, pillHost, visibleTabs, maxBarWrapperH, maxDividerH);
        ViewGroup rootB = clearBottomBarWrapperChain(bnbBg, pillHost, visibleTabs, maxBarWrapperH, maxDividerH);
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

        int totalWidth = decorView.getWidth() > 0
                ? decorView.getWidth()
                : ctx.getResources().getDisplayMetrics().widthPixels;
        int tabCount = visibleTabs.size();
        int desiredPillWidth = Math.min(
                totalWidth - BlurBars.dp(ctx, 28.0f),
                tabCount * BlurBars.dp(ctx, 100.0f) + BlurBars.dp(ctx, 16.0f)
        );
        int sideInset = Math.max(BlurBars.dp(ctx, 14.0f), (totalWidth - desiredPillWidth) / 2);

        if (tabsParent != null) {
            int[] origPad = ORIG_NAV_PADDING.get(tabsParent);
            if (origPad == null) {
                origPad = new int[]{
                        tabsParent.getPaddingLeft(),
                        tabsParent.getPaddingTop(),
                        tabsParent.getPaddingRight(),
                        tabsParent.getPaddingBottom()
                };
                ORIG_NAV_PADDING.put(tabsParent, origPad);
            }
            int innerSidePad = sideInset + BlurBars.dp(ctx, 4.0f);
            if (tabsParent.getPaddingLeft() != innerSidePad || tabsParent.getPaddingRight() != innerSidePad) {
                tabsParent.setPadding(innerSidePad, origPad[1], innerSidePad, origPad[3]);
            }
        }

        float liftY = -BlurBars.dp(ctx, 12.0f);
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
                    ? blend(palette.navBarBg, palette.accent, 0.09f)
                    : blend(palette.navBarBg, palette.accent, 0.13f);
            rimAccent = palette.accent;
        } else {
            baseSurface = palette.dark ? 0xFF21242B : 0xFFE8ECF2;
            rimAccent = palette.dark ? 0xFFFFFFFF : 0xFF5A6270;
        }
        pillDrawable.update(visibleTabs, bnbBg, sysNavBg, baseSurface, rimAccent, palette.dark, sideInset);
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

        for (Map.Entry<View, Float> entry : ORIG_NAV_SIBLING_ALPHA.entrySet()) {
            View sib = entry.getKey();
            Float alpha = entry.getValue();
            if (sib != null && alpha != null) {
                sib.setAlpha(alpha);
            }
        }
        ORIG_NAV_SIBLING_ALPHA.clear();

        if (decorView != null) {
            View contentRoot = decorView.findViewById(android.R.id.content);
            if (contentRoot != null && Boolean.TRUE.equals(CONTENT_BG_APPLIED.remove(contentRoot))) {
                contentRoot.setBackground(null);
            }
        }
        for (int i = 0; i < visibleTabs.size(); i++) {
            View tab = visibleTabs.get(i);
            if (tab.getBackground() instanceof ActiveTabPillDrawable) {
                tab.setBackground(null);
            }
        }
    }

    private static void ensureNavScrollBlurObserver(View decorView, View pillHost) {
        NAV_SCROLL_HOST.put(decorView, new WeakReference<>(pillHost));
        if (NAV_SCROLL_LISTENERS.containsKey(decorView)) {
            return;
        }
        final WeakReference<View> decorRef = new WeakReference<>(decorView);
        ViewTreeObserver.OnScrollChangedListener listener = () -> {
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
        NAV_SCROLL_LISTENERS.put(decorView, listener);
        ViewTreeObserver vto = decorView.getViewTreeObserver();
        if (vto != null && vto.isAlive()) {
            vto.addOnScrollChangedListener(listener);
        }
    }

    private static final int[] NAV_LOC_A = new int[2];
    private static final int[] NAV_LOC_B = new int[2];

    private static boolean isScrollableContainer(View v) {
        if (v instanceof ScrollView || v instanceof HorizontalScrollView) {
            return true;
        }
        String cn = v.getClass().getName();
        return cn.contains("RecyclerView")
                || cn.contains("NestedScrollView")
                || cn.contains("ListView")
                || cn.contains("WebView");
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

    private static View findMainRootAboveBar(View anchor, int minFullScreenH) {
        View cur = anchor;
        while (cur != null && cur.getParent() instanceof ViewGroup) {
            ViewGroup p = (ViewGroup) cur.getParent();
            if (p.getHeight() >= minFullScreenH) {
                return p;
            }
            cur = p;
        }
        return null;
    }

    private static View findInnerExpandableTarget(View listGroup, View stopAt) {
        View cur = listGroup;
        while (cur != null && cur != stopAt && cur.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) cur.getParent();
            String parentClass = parent.getClass().getName();
            boolean unsupportedParent = parent instanceof LinearLayout
                    || parentClass.contains("ViewPager")
                    || parentClass.contains("RecyclerView");
            if (!unsupportedParent && cur.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                return cur;
            }
            if (parent == stopAt) {
                break;
            }
            cur = parent;
        }
        return null;
    }

    private static void extendMainContentBehindPill(
            View decorView,
            Context ctx,
            View pillHost,
            View bnbBg,
            View sysNavBg,
            boolean pillActive
    ) {
        if (!pillActive) {
            for (Map.Entry<View, Integer> e : ORIG_NAV_LIST_PAD.entrySet()) {
                View v = e.getKey();
                Integer p = e.getValue();
                if (v != null && p != null) {
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), p);
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
        if (decorH <= 0 || pillHost.getHeight() <= 0) {
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

        int minInsetPx = BlurBars.dp(ctx, 32.0f);
        int minFullH = Math.round(decorH * 0.55f);
        View mainRootView = findMainRootAboveBar(bnbBg != null ? bnbBg : pillHost, minFullH);
        View mainContentBranch = null;
        if (mainRootView instanceof ViewGroup) {
            ViewGroup mainRoot = (ViewGroup) mainRootView;
            mainRoot.setClipChildren(false);
            mainRoot.setClipToPadding(false);
            stripContainerBottomInsetPadding(mainRoot, decorBottom, minInsetPx);

            int bestH = Math.round(decorH * 0.40f);
            int count = mainRoot.getChildCount();
            for (int i = 0; i < count; i++) {
                View c = mainRoot.getChildAt(i);
                if (c == null
                        || c.getVisibility() != View.VISIBLE
                        || BlurBars.isSelfOrAncestor(c, pillHost)
                        || (bnbBg != null && BlurBars.isSelfOrAncestor(c, bnbBg))
                        || (sysNavBg != null && BlurBars.isSelfOrAncestor(c, sysNavBg))) {
                    continue;
                }
                if (c.getHeight() > bestH) {
                    bestH = c.getHeight();
                    mainContentBranch = c;
                }
            }
            if (mainContentBranch != null) {
                if (mainContentBranch instanceof ViewGroup) {
                    ViewGroup mcv = (ViewGroup) mainContentBranch;
                    mcv.setClipChildren(false);
                    mcv.setClipToPadding(false);
                    mcv.setClipToOutline(false);
                }
                stripContainerBottomInsetPadding(mainContentBranch, decorBottom, minInsetPx);
                if (!(mainRoot instanceof LinearLayout)) {
                    expandViewToDecorBottom(mainContentBranch, decorBottom);
                }
            }
        }

        int rvId = BlurBars.id(ctx, "chat_list_recycler_view");
        View singleList = rvId != 0 ? decorView.findViewById(rvId) : null;
        if (singleList instanceof ViewGroup && singleList.isAttachedToWindow() && singleList.isShown()) {
            ViewGroup listGroup = (ViewGroup) singleList;
            Integer origPadBottom = ORIG_NAV_LIST_PAD.get(listGroup);
            if (origPadBottom == null) {
                origPadBottom = listGroup.getPaddingBottom();
                ORIG_NAV_LIST_PAD.put(listGroup, origPadBottom);
            }

            ViewParent p = listGroup.getParent();
            while (p instanceof ViewGroup && p != decorView) {
                ViewGroup vg = (ViewGroup) p;
                vg.setClipChildren(false);
                vg.setClipToPadding(false);
                vg.setClipToOutline(false);
                stripContainerBottomInsetPadding(vg, decorBottom, minInsetPx);
                if (vg == mainRootView) {
                    break;
                }
                p = vg.getParent();
            }
            listGroup.setClipToPadding(false);

            listGroup.getLocationInWindow(NAV_LOC_A);
            int listBottom = NAV_LOC_A[1] + listGroup.getHeight();
            View stopAt = mainContentBranch != null ? mainContentBranch : mainRootView;
            View innerTarget = findInnerExpandableTarget(listGroup, stopAt);
            if (innerTarget != null
                    && (mainContentBranch == null || !mainContentBranch.isLayoutRequested())
                    && (NAV_EXPAND_STATES.containsKey(innerTarget) || listBottom < decorBottom - BlurBars.dp(ctx, 6.0f))) {
                expandViewToDecorBottom(innerTarget, decorBottom);
                listGroup.getLocationInWindow(NAV_LOC_A);
                listBottom = NAV_LOC_A[1] + listGroup.getHeight();
            }

            int actualOverlap = Math.max(0, listBottom - pillTopWinY);
            int extraSafePad = BlurBars.dp(ctx, 12.0f);
            int targetPadBottom = origPadBottom + (actualOverlap > 0 ? actualOverlap + extraSafePad : 0);
            if (listGroup.getPaddingBottom() != targetPadBottom) {
                listGroup.setPadding(
                        listGroup.getPaddingLeft(),
                        listGroup.getPaddingTop(),
                        listGroup.getPaddingRight(),
                        targetPadBottom
                );
            }
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

        float top = iconVisualTop - density * 4.8f;
        float bottom = textBottom + density * 5.2f;
        float minH = density * 38.0f;
        if (bottom - top < minH) {
            top = bottom - minH;
        }
        outTopBottom[0] = top;
        outTopBottom[1] = bottom;
    }

    static float computeActivePillWidth(int tabWidth, float pillHeight, float density) {
        float desiredW = Math.max(pillHeight * 1.85f, density * 78.0f);
        float maxAllowedW = Math.max(density * 46.0f, tabWidth - density * 6.0f);
        return Math.min(desiredW, maxAllowedW);
    }

    /** Concentric Material 3 horizontal capsule indicator behind the selected bottom navigation tab. */
    private static final class ActiveTabPillDrawable extends Drawable {
        private final float density;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final int[] locA = new int[2];
        private final int[] locB = new int[2];
        private final float[] topBottom = new float[2];
        private WeakReference<View> tabRef;
        private WeakReference<View> iconRef;
        private WeakReference<View> textRef;
        private int fillColor;
        private int strokeColor;

        ActiveTabPillDrawable(Context ctx) {
            this.density = ctx.getResources().getDisplayMetrics().density;
            this.fillPaint.setStyle(Paint.Style.FILL);
            this.strokePaint.setStyle(Paint.Style.STROKE);
            this.strokePaint.setStrokeWidth(Math.max(1.0f, this.density * 0.85f));
        }

        void bindViews(View tab, View iconView, View textView) {
            if (this.tabRef == null || this.tabRef.get() != tab) {
                this.tabRef = new WeakReference<>(tab);
            }
            if (this.iconRef == null || this.iconRef.get() != iconView) {
                this.iconRef = iconView != null ? new WeakReference<>(iconView) : null;
            }
            if (this.textRef == null || this.textRef.get() != textView) {
                this.textRef = textView != null ? new WeakReference<>(textView) : null;
            }
        }

        void updateColors(int fill, int stroke) {
            if (this.fillColor != fill || this.strokeColor != stroke) {
                this.fillColor = fill;
                this.strokeColor = stroke;
                this.fillPaint.setColor(fill);
                this.strokePaint.setColor(stroke);
                invalidateSelf();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (FloatingNavPillDrawable.recordingNavBlur) {
                return;
            }
            Rect b = getBounds();
            int w = b.width();
            int h = b.height();
            if (w <= 0 || h <= 0) {
                return;
            }
            View tab = this.tabRef != null ? this.tabRef.get() : null;
            View icon = this.iconRef != null ? this.iconRef.get() : null;
            View text = this.textRef != null ? this.textRef.get() : null;
            float top;
            float bottom;
            if (tab != null && tab.getHeight() > 0) {
                computeActivePillVerticalBounds(tab, icon, text, this.density, this.locA, this.locB, this.topBottom);
                top = b.top + this.topBottom[0];
                bottom = b.top + this.topBottom[1];
            } else {
                bottom = b.bottom - this.density * 3.0f;
                top = Math.max(b.top, bottom - this.density * 42.0f);
            }
            float pillH = bottom - top;
            float pillW = computeActivePillWidth(w, pillH, this.density);
            float left = b.left + (w - pillW) * 0.5f;
            float right = left + pillW;
            if (right <= left || bottom <= top) {
                return;
            }
            this.rect.set(left, top, right, bottom);
            float radius = Math.min(this.rect.width(), this.rect.height()) * 0.5f;
            canvas.drawRoundRect(this.rect, radius, radius, this.fillPaint);
            if ((this.strokeColor >>> 24) != 0) {
                float halfStroke = this.strokePaint.getStrokeWidth() * 0.5f;
                this.rect.inset(halfStroke, halfStroke);
                float sr = Math.max(0f, Math.min(this.rect.width(), this.rect.height()) * 0.5f);
                canvas.drawRoundRect(this.rect, sr, sr, this.strokePaint);
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
     * Concentric floating stadium-capsule background for LINE's bottom navigation bar with real-time
     * Android 12+ {@link RenderNode} + {@link RenderEffect} frosted-glass blur, soft ambient shadow,
     * and crisp tonal rim border.
     */
    private static final class FloatingNavPillDrawable extends Drawable {
        static boolean recordingNavBlur;

        private final WeakReference<View> hostRef;
        private final WeakReference<View> decorRef;
        private final float density;
        private final List<TabRefs> tabSlots = new ArrayList<>(6);
        private final List<WeakReference<View>> rawTabRefs = new ArrayList<>(6);
        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF pillRect = new RectF();
        private final RectF rimRect = new RectF();
        private final RectF shadowRect = new RectF();
        private final Path clipPath = new Path();
        private final int[] hostLoc = new int[2];
        private final int[] tabLoc = new int[2];
        private final int[] childLoc = new int[2];
        private final float[] tabTopBot = new float[2];

        private Object gpu;
        private WeakReference<View> bnbBgRef;
        private WeakReference<View> sysNavBgRef;
        private int surfaceRgb;
        private int rimAccentRgb;
        private boolean dark;
        private int fallbackSideInset;
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
            this.rimPaint.setStyle(Paint.Style.STROKE);
            this.rimPaint.setStrokeWidth(Math.max(1.0f, this.density * 1.0f));
        }

        void update(
                List<View> visibleTabs,
                View bnbBg,
                View sysNavBg,
                int surfaceRgb,
                int rimAccentRgb,
                boolean dark,
                int sideInset
        ) {
            if (this.bnbBgRef == null || this.bnbBgRef.get() != bnbBg) {
                this.bnbBgRef = bnbBg != null ? new WeakReference<>(bnbBg) : null;
            }
            if (this.sysNavBgRef == null || this.sysNavBgRef.get() != sysNavBg) {
                this.sysNavBgRef = sysNavBg != null ? new WeakReference<>(sysNavBg) : null;
            }
            boolean changed = this.surfaceRgb != surfaceRgb
                    || this.rimAccentRgb != rimAccentRgb
                    || this.dark != dark
                    || this.fallbackSideInset != sideInset
                    || this.tabSlots.size() != visibleTabs.size();
            if (!changed) {
                for (int i = 0; i < visibleTabs.size(); i++) {
                    if (this.tabSlots.get(i).tabRef.get() != visibleTabs.get(i)) {
                        changed = true;
                        break;
                    }
                }
            }
            if (changed) {
                this.surfaceRgb = surfaceRgb;
                this.rimAccentRgb = rimAccentRgb;
                this.dark = dark;
                this.fallbackSideInset = sideInset;
                this.tabSlots.clear();
                this.rawTabRefs.clear();
                for (int i = 0; i < visibleTabs.size(); i++) {
                    View t = visibleTabs.get(i);
                    this.tabSlots.add(new TabRefs(t));
                    this.rawTabRefs.add(new WeakReference<>(t));
                }
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
            float minInnerLeft = Float.MAX_VALUE;
            float maxInnerRight = -Float.MAX_VALUE;
            float minInnerTop = Float.MAX_VALUE;
            float maxInnerBottom = -Float.MAX_VALUE;

            for (int i = 0; i < this.tabSlots.size(); i++) {
                TabRefs slot = this.tabSlots.get(i);
                View tab = slot.tabRef.get();
                if (tab != null && tab.getVisibility() == View.VISIBLE && tab.getWidth() > 0 && tab.getHeight() > 0) {
                    View icon = slot.iconRef != null ? slot.iconRef.get() : null;
                    View text = slot.textRef != null ? slot.textRef.get() : null;
                    computeActivePillVerticalBounds(
                            tab, icon, text, this.density, this.tabLoc, this.childLoc, this.tabTopBot
                    );
                    float relTabX = this.tabLoc[0] - this.hostLoc[0];
                    float relTabY = this.tabLoc[1] - this.hostLoc[1];
                    float innerTop = relTabY + this.tabTopBot[0];
                    float innerBottom = relTabY + this.tabTopBot[1];
                    float innerH = innerBottom - innerTop;
                    float innerW = computeActivePillWidth(tab.getWidth(), innerH, this.density);
                    float innerLeft = relTabX + (tab.getWidth() - innerW) * 0.5f;
                    float innerRight = innerLeft + innerW;

                    if (innerLeft < minInnerLeft) minInnerLeft = innerLeft;
                    if (innerRight > maxInnerRight) maxInnerRight = innerRight;
                    if (innerTop < minInnerTop) minInnerTop = innerTop;
                    if (innerBottom > maxInnerBottom) maxInnerBottom = innerBottom;
                }
            }

            float concentricGap = this.density * 4.5f;
            float minScreenMargin = this.density * 10.0f;
            float left;
            float right;
            float top;
            float bottom;
            if (minInnerLeft < maxInnerRight && minInnerTop < maxInnerBottom) {
                left = Math.max(minScreenMargin, minInnerLeft - concentricGap);
                right = Math.min(w - minScreenMargin, maxInnerRight + concentricGap);
                top = minInnerTop - concentricGap;
                bottom = maxInnerBottom + concentricGap;
            } else {
                left = Math.min(this.fallbackSideInset, w * 0.22f);
                right = Math.max(left + this.density * 140.0f, w - left);
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

            // Soft layered ambient shadow underneath the floating capsule
            int baseShadowAlpha = this.dark ? 0x30 : 0x1A;
            for (int step = 3; step >= 1; step--) {
                float spread = step * this.density * 1.15f;
                float dy = step * this.density * 1.25f;
                this.shadowRect.set(
                        left - spread * 0.45f,
                        top + dy * 0.5f,
                        right + spread * 0.45f,
                        bottom + dy + spread * 0.45f
                );
                this.shadowPaint.setColor(((baseShadowAlpha / step) & 0xFF) << 24);
                float sr = radius + spread * 0.45f;
                canvas.drawRoundRect(this.shadowRect, sr, sr, this.shadowPaint);
            }

            ensureShaders(left, top, right, bottom, radius, canBlur);

            int save = canvas.save();
            canvas.clipPath(this.clipPath);
            try {
                if (canBlur) {
                    View decor = this.decorRef.get();
                    if (decor != null) {
                        if (this.gpu == null) {
                            this.gpu = new NavBlurGpu();
                        }
                        View bnbBg = this.bnbBgRef != null ? this.bnbBgRef.get() : null;
                        View sysNavBg = this.sysNavBgRef != null ? this.sysNavBgRef.get() : null;
                        int pillWinX = Math.round(this.hostLoc[0] + left);
                        int pillWinY = Math.round(this.hostLoc[1] + top);
                        int pillW = Math.max(1, Math.round(right - left));
                        int pillH = Math.max(1, Math.round(bottom - top));
                        canvas.save();
                        canvas.translate(left, top);
                        ((NavBlurGpu) this.gpu).draw(
                                canvas, decor, host, bnbBg, sysNavBg, this.rawTabRefs,
                                pillWinX, pillWinY, pillW, pillH, this.density, this.blurDirty
                        );
                        canvas.restore();
                        this.blurDirty = false;
                    }
                }
                canvas.drawRect(this.pillRect, this.fillPaint);
            } finally {
                canvas.restoreToCount(save);
            }

            float rimRadius = Math.max(0f, Math.min(this.rimRect.width(), this.rimRect.height()) * 0.5f);
            canvas.drawRoundRect(this.rimRect, rimRadius, rimRadius, this.rimPaint);
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
                alphaTop = this.dark ? 0xC6 : 0xD4;
                alphaBottom = this.dark ? 0xDA : 0xE6;
            } else {
                alphaTop = this.dark ? 0xF0 : 0xF4;
                alphaBottom = this.dark ? 0xF6 : 0xFA;
            }
            int topFill = withAlpha(blend(this.surfaceRgb, 0xFFFFFFFF, this.dark ? 0.04f : 0.08f), alphaTop);
            int botFill = withAlpha(this.surfaceRgb, alphaBottom);
            this.fillPaint.setShader(new LinearGradient(
                    left, top, left, bottom,
                    new int[]{topFill, botFill},
                    null,
                    Shader.TileMode.CLAMP
            ));

            float stroke = this.rimPaint.getStrokeWidth();
            float inset = stroke * 0.5f;
            this.rimRect.set(left + inset, top + inset, right - inset, bottom - inset);
            if (this.dark) {
                int topRim = withAlpha(blend(0xFFFFFFFF, this.rimAccentRgb, 0.25f), 0x4C);
                int midRim = withAlpha(this.rimAccentRgb, 0x26);
                int botRim = withAlpha(0xFFFFFFFF, 0x18);
                this.rimPaint.setShader(new LinearGradient(
                        left, top, left, bottom,
                        new int[]{topRim, midRim, botRim},
                        new float[]{0f, 0.45f, 1f},
                        Shader.TileMode.CLAMP
                ));
            } else {
                int topRim = withAlpha(blend(this.rimAccentRgb, 0xFFFFFFFF, 0.35f), 0x42);
                int midRim = withAlpha(this.rimAccentRgb, 0x2E);
                int botRim = withAlpha(blend(this.rimAccentRgb, 0xFF000000, 0.25f), 0x38);
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
                View navHost,
                View bnbBg,
                View sysNavBg,
                List<WeakReference<View>> tabRefs,
                int pillWinX,
                int pillWinY,
                int w,
                int h,
                float density,
                boolean dirty
        ) {
            boolean sizeChanged = w != this.lastW || h != this.lastH;
            if (dirty || sizeChanged || !this.node.hasDisplayList()) {
                View navBarRoot = resolveNavBarRoot(navHost, density);
                View bnbBgRoot = bnbBg != null ? resolveNavBarRoot(bnbBg, density) : null;
                View tabsRow = resolveTabsRow(tabRefs, density);
                this.node.setPosition(0, 0, w, h);
                RecordingCanvas rc = this.node.beginRecording(w, h);
                FloatingNavPillDrawable.recordingNavBlur = true;
                try {
                    View contentRoot = decor.findViewById(android.R.id.content);
                    View sourceRoot = contentRoot != null ? contentRoot : decor;
                    recordBackgroundBranch(
                            rc, sourceRoot, navBarRoot, bnbBgRoot, navHost, bnbBg, sysNavBg,
                            tabsRow, tabRefs, pillWinX, pillWinY
                    );
                } finally {
                    FloatingNavPillDrawable.recordingNavBlur = false;
                    this.node.endRecording();
                }
            }
            if (this.effect == null || sizeChanged) {
                float r = density * 22.0f;
                this.effect = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP);
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
                                rc, child, navBarRoot, bnbBgRoot, navHost, bnbBg, sysNavBg,
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
        if (!(view instanceof TextView) || view.getVisibility() != View.VISIBLE) {
            return;
        }
        TextView tv = (TextView) view;
        if (tv.getCurrentTextColor() != palette.onBadge) {
            tv.setTextColor(palette.onBadge);
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
            if (pill.appliedColor != palette.badgeBg) {
                pill.appliedColor = palette.badgeBg;
                pill.setColor(palette.badgeBg);
            }
        } else {
            BadgePillDrawable pill = new BadgePillDrawable();
            pill.setShape(GradientDrawable.RECTANGLE);
            pill.setCornerRadius(BlurBars.dp(tv.getContext(), 999.0f));
            pill.appliedColor = palette.badgeBg;
            pill.setColor(palette.badgeBg);
            tv.setBackgroundTintList(null);
            tv.setBackground(pill);
        }
        if (tv.getPaddingLeft() != pad[0] || tv.getPaddingTop() != pad[1]
                || tv.getPaddingRight() != pad[2] || tv.getPaddingBottom() != pad[3]) {
            tv.setPadding(pad[0], pad[1], pad[2], pad[3]);
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
        if (val == 0 && chatTitle != null && !chatTitle.isEmpty()) {
            String legacyKey = "title_" + chatTitle;
            if (!legacyKey.equals(chatId)) {
                int legacy = LineSettings.chatBubbleSendColor(legacyKey);
                if (legacy != 0) {
                    LineSettings.setChatBubbleColors(chatId, legacy, LineSettings.chatBubbleReceiveColor(legacyKey));
                    LineSettings.clearChatBubbleColors(legacyKey);
                    return legacy;
                }
            }
        }
        return val;
    }

    private static int readStoredRecvRule(String chatId, String chatTitle) {
        int val = LineSettings.chatBubbleReceiveColor(chatId);
        if (val == 0 && chatTitle != null && !chatTitle.isEmpty()) {
            String legacyKey = "title_" + chatTitle;
            if (!legacyKey.equals(chatId)) {
                int legacy = LineSettings.chatBubbleReceiveColor(legacyKey);
                if (legacy != 0) {
                    LineSettings.setChatBubbleColors(chatId, LineSettings.chatBubbleSendColor(legacyKey), legacy);
                    LineSettings.clearChatBubbleColors(legacyKey);
                    return legacy;
                }
            }
        }
        return val;
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
            return myPalette != null ? (isSend ? myPalette.sendBubble : myPalette.receiveBubble) : 0;
        }
        if (rule == MODE_MATERIAL_YOU) {
            return myPalette != null ? (isSend ? myPalette.sendBubble : myPalette.receiveBubble) : 0;
        }
        // MODE_AUTO (0): follow global preferences
        if (LineSettings.chatBubbleWallpaper() && wp != null && wp.valid) {
            return isSend ? wp.sendBubble : wp.receiveBubble;
        }
        if (LineSettings.materialYouTheme() && myPalette != null) {
            return isSend ? myPalette.sendBubble : myPalette.receiveBubble;
        }
        return 0;
    }

    static void applyChatBubbles(ViewGroup listView, int sendRule, int recvRule, WallpaperPalette wp) {
        boolean needsPalette = LineSettings.materialYouTheme()
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
            View bubble = BlurBars.find(row, idName);
            if (bubble == null || BUBBLE_APPLIED.remove(bubble) == null) {
                continue;
            }
            BUBBLE_BG_REF.remove(bubble);
            Drawable bg = bubble.getBackground();
            if (bg != null) {
                bg.clearColorFilter();
                bg.setTintList(null);
            }
            bubble.setBackgroundTintList(null);
        }
        for (String quoteId : BUBBLE_QUOTE_CONTAINER_IDS) {
            View quote = BlurBars.find(row, quoteId);
            if (quote == null || BUBBLE_APPLIED.remove(quote) == null) {
                continue;
            }
            BUBBLE_BG_REF.remove(quote);
            Drawable bg = quote.getBackground();
            if (bg != null) {
                bg.clearColorFilter();
                bg.setTintList(null);
            }
            quote.setBackgroundTintList(null);
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
            tintBubbleTexts(bubble, textColor, subTextColor, linkColor, dividerColor);
        }

        int quoteColor = blend(bubbleColor, lightBubble ? 0xFF000000 : 0xFFFFFFFF, 0.08f) | 0xFF000000;
        ColorStateList quoteCsl = ColorStateList.valueOf(quoteColor);
        for (String quoteId : BUBBLE_QUOTE_CONTAINER_IDS) {
            View quote = BlurBars.find(row, quoteId);
            if (quote != null && quote.getVisibility() == View.VISIBLE) {
                applyBubbleBackgroundTint(quote, quoteColor, quoteCsl);
                tintBubbleTexts(quote, textColor, subTextColor, linkColor, dividerColor);
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

    private static void tintBubbleTexts(View container, int textColor, int subTextColor, int linkColor, int dividerColor) {
        for (String textId : BUBBLE_TEXT_IDS) {
            View tv = BlurBars.find(container, textId);
            if (tv instanceof TextView) {
                TextView textView = (TextView) tv;
                if (textView.getCurrentTextColor() != textColor) {
                    textView.setTextColor(textColor);
                }
                ColorStateList curLink = textView.getLinkTextColors();
                if (curLink == null || curLink.getDefaultColor() != linkColor) {
                    textView.setLinkTextColor(linkColor);
                }
            }
        }
        for (String subId : BUBBLE_SUBTEXT_IDS) {
            View tv = BlurBars.find(container, subId);
            if (tv instanceof TextView) {
                TextView textView = (TextView) tv;
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
        boolean materialYouOn = LineSettings.materialYouTheme();
        int voiceIconTint = (materialYouOn && textColor == 0xFF1A1C1E)
                ? Palette.forMode(container.getContext(), false).pillBg
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
        if (materialYouOn) {
            int circleAccent = Palette.of(container.getContext()).badgeBg;
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
    }

    // =========================================================================
    // Chat Room ID Resolution (ClassLoader-safe for Parcelable ChatHistoryRequest)
    // =========================================================================

    private static String extractChatIdFromIntent(Activity activity) {
        if (activity == null) {
            return null;
        }
        try {
            Intent intent = activity.getIntent();
            if (intent == null) {
                return null;
            }
            // Ensure Activity's APK ClassLoader is installed before unmarshalling Parcelables!
            intent.setExtrasClassLoader(activity.getClassLoader());
            for (String key : new String[]{"chatId", "chat_id", "mid", "squareChatMid", "EXTRA_CHAT_ID"}) {
                String val = intent.getStringExtra(key);
                if (val != null && !val.isEmpty()) {
                    return val;
                }
            }
            Bundle extras = intent.getExtras();
            if (extras != null) {
                extras.setClassLoader(activity.getClassLoader());
                for (String reqKey : new String[]{"chatHistoryRequest", "request", "CHAT_HISTORY_REQUEST"}) {
                    if (!extras.containsKey(reqKey)) {
                        continue;
                    }
                    Object req = extras.get(reqKey);
                    String id = extractChatIdFromRequestObject(req);
                    if (id != null && !id.isEmpty()) {
                        return id;
                    }
                }
                for (String key : extras.keySet()) {
                    Object val = extras.get(key);
                    if (val != null && val.getClass().getName().contains("ChatHistoryRequest")) {
                        String id = extractChatIdFromRequestObject(val);
                        if (id != null && !id.isEmpty()) {
                            return id;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // Fallback: inspect fields on the Activity for ChatHistoryRequest / ViewModel holders
        try {
            for (Class<?> cls = activity.getClass(); cls != null && cls != Activity.class; cls = cls.getSuperclass()) {
                for (Field f : cls.getDeclaredFields()) {
                    String typeName = f.getType().getName();
                    if (typeName.contains("ChatHistoryRequest")) {
                        f.setAccessible(true);
                        Object holder = f.get(activity);
                        String id = extractChatIdFromRequestObject(holder);
                        if (id != null && !id.isEmpty()) {
                            return id;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String extractChatIdFromRequestObject(Object obj) {
        if (obj == null) {
            return null;
        }
        for (String methodName : new String[]{"getChatId", "c", "a"}) {
            try {
                Method m = obj.getClass().getMethod(methodName);
                if (m.getReturnType() == String.class && m.getParameterCount() == 0) {
                    Object res = m.invoke(obj);
                    if (res instanceof String && !((String) res).isEmpty()) {
                        return (String) res;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (f.getType() == String.class && ("chatId".equals(f.getName()) || "a".equals(f.getName()))) {
                    f.setAccessible(true);
                    Object res = f.get(obj);
                    if (res instanceof String && !((String) res).isEmpty()) {
                        return (String) res;
                    }
                } else if (f.getType().getName().endsWith("ChatHistoryRequest")) {
                    f.setAccessible(true);
                    String nested = extractChatIdFromRequestObject(f.get(obj));
                    if (nested != null && !nested.isEmpty()) {
                        return nested;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
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
        Activity activity = activityOf(decorView.getContext());
        String extracted = extractChatIdFromIntent(activity);
        if (extracted != null && !extracted.isEmpty()) {
            CHAT_IDS.put(decorView, extracted);
            return extracted;
        }
        if (chatTitle != null && !chatTitle.isEmpty()) {
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
        if (!name.endsWith("ChatSettingActivity") && !name.endsWith("SquareChatSettingsActivity")) {
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
                styleColorDot(activity, sendDot, effSend != 0 ? effSend : 0xFF66D575);
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
            return zh ? "自動抓取此聊天室背景圖片主色" : "Using this chat's wallpaper seed color";
        }
        if (sendRule == MODE_MATERIAL_YOU) {
            return zh ? "使用系統 Material You 動態配色" : "Using Android Material You palette";
        }
        if (sendRule == MODE_LINE_DEFAULT) {
            return zh ? "維持 LINE 原版預設氣泡顏色" : "Using LINE default bubble colors";
        }
        if (LineSettings.chatBubbleWallpaper()) {
            return zh ? "自動（優先抓取聊天室背景主色 / Material You）" : "Auto (Chat wallpaper seed / Material You)";
        }
        return zh ? "跟隨全域設定 · 點擊可自訂送出與接收氣泡" : "Follows global setting · Tap to customize";
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
        TextView sendLabel = sectionLabel(activity, zh ? "我的訊息氣泡自訂色（送出）" : "My bubble custom color (Sent)", palette.accent);
        LinearLayout.LayoutParams slLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slLp.topMargin = BlurBars.dp(activity, 12f);
        root.addView(sendLabel, slLp);
        root.addView(buildSwatchGrid(activity, selectedSend, updatePreview, sendSwatches));
        root.addView(buildHexRow(activity, selectedSend, updatePreview, zh));

        // Received bubble picker
        TextView recvLabel = sectionLabel(activity, zh ? "對方訊息氣泡自訂色（接收）" : "Their bubble custom color (Received)", palette.accent);
        LinearLayout.LayoutParams rlLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlLp.topMargin = BlurBars.dp(activity, 14f);
        root.addView(recvLabel, rlLp);
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
                    LineSettings.setChatBubbleColors(chatId, selectedSend[0], selectedRecv[0]);
                    BUBBLE_APPLIED.clear();
                    BUBBLE_BG_REF.clear();
                    if (onChanged != null) onChanged.run();
                })
                .setNeutralButton(zh ? "重設自動" : "Reset", (d, w) -> {
                    PixelNativeEnhancements.confirm(scroll);
                    LineSettings.clearChatBubbleColors(chatId);
                    BUBBLE_APPLIED.clear();
                    BUBBLE_BG_REF.clear();
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
