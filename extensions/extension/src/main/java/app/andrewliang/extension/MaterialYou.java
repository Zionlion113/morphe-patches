package app.andrewliang.extension;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

final class MaterialYou {

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
            "chat_ui_bubble_root",
            "chat_ui_row_gift_message_bubble",
    };

    private static final String[] BUBBLE_TEXT_IDS = {
            "chat_ui_message_text",
            "chat_ui_row_file_title",
            "chat_ui_unavailable_message",
    };

    private static final String[] BUBBLE_SUBTEXT_IDS = {
            "chat_ui_row_file_duration",
            "chat_ui_row_file_size",
    };

    private static final int[] PRESET_COLORS = {
            0,          // Follow Material You / default
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
    private static final Map<View, Integer> NAV_APPLIED = new WeakHashMap<>();
    private static final Map<View, Integer> BADGE_APPLIED = new WeakHashMap<>();
    private static final Map<View, Boolean> COMPOSE_TINTED = new WeakHashMap<>();
    private static final Map<View, String> CHAT_IDS = new WeakHashMap<>();

    private static volatile String lastActiveChatId = "";
    private static volatile String lastActiveChatTitle = "";

    private MaterialYou() {}

    static boolean anyEnabled() {
        return LineSettings.materialYouTheme()
                || LineSettings.materialYouNavBar()
                || LineSettings.chatHeaderWhite();
    }

    static final class Palette {
        final boolean dark;
        final int accent;
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
            this.sendBubble = sendBubble;
            this.receiveBubble = receiveBubble;
            this.navBarBg = navBarBg;
            this.navSelected = navSelected;
            this.navUnselected = navUnselected;
            this.badgeBg = badgeBg;
            this.onBadge = onBadge;
            this.blurSurface = blurSurface;
        }

        private static volatile Palette cached;
        private static volatile boolean cachedDark;
        private static volatile int cachedKeyColor;

        static Palette of(Context context) {
            boolean dark = (context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    int a1_500 = context.getColor(android.R.color.system_accent1_500);
                    Palette hit = cached;
                    if (hit != null && cachedDark == dark && cachedKeyColor == a1_500) {
                        return hit;
                    }
                    Palette created;
                    if (dark) {
                        int a1_200 = context.getColor(android.R.color.system_accent1_200);
                        int a1_300 = context.getColor(android.R.color.system_accent1_300);
                        int a1_700 = context.getColor(android.R.color.system_accent1_700);
                        int a1_800 = context.getColor(android.R.color.system_accent1_800);
                        int a1_900 = context.getColor(android.R.color.system_accent1_900);
                        int n1_900 = context.getColor(android.R.color.system_neutral1_900);
                        int n2_300 = context.getColor(android.R.color.system_neutral2_300);
                        int n2_800 = context.getColor(android.R.color.system_neutral2_800);
                        int navBg = blend(n1_900, a1_800, 0.24f);
                        int blur = blend(n1_900, a1_900, 0.35f);
                        created = new Palette(true, a1_200, a1_700, n2_800, navBg, a1_200, n2_300, a1_300, a1_900, blur);
                    } else {
                        int a1_100 = context.getColor(android.R.color.system_accent1_100);
                        int a1_600 = context.getColor(android.R.color.system_accent1_600);
                        int a2_50 = context.getColor(android.R.color.system_accent2_50);
                        int a2_100 = context.getColor(android.R.color.system_accent2_100);
                        int n1_50 = context.getColor(android.R.color.system_neutral1_50);
                        int n2_500 = context.getColor(android.R.color.system_neutral2_500);
                        int navBg = blend(n1_50, a2_100, 0.55f);
                        int recv = blend(n1_50, a2_50, 0.65f);
                        created = new Palette(false, a1_600, a1_100, recv, navBg, a1_600, n2_500, a1_500, 0xFFFFFFFF, navBg);
                    }
                    cachedDark = dark;
                    cachedKeyColor = a1_500;
                    cached = created;
                    return created;
                } catch (Throwable ignored) {
                }
            }
            Palette hit = cached;
            if (hit != null && cachedDark == dark && cachedKeyColor == 0) {
                return hit;
            }
            Palette created = dark
                    ? new Palette(true, 0xFFA8C7FA, 0xFF0842A0, 0xFF282A2F, 0xFF1B2029, 0xFFA8C7FA, 0xFFB0B5BE, 0xFF8AB4F8, 0xFF062E6F, 0xFF181C24)
                    : new Palette(false, 0xFF1A73E8, 0xFFD3E3FD, 0xFFF0F4FA, 0xFFEBF2FC, 0xFF1A73E8, 0xFF5F6368, 0xFF1A73E8, 0xFFFFFFFF, 0xFFEBF2FC);
            cachedDark = dark;
            cachedKeyColor = 0;
            cached = created;
            return created;
        }
    }

    static int accentColor(Context context) {
        return Palette.of(context).accent;
    }

    static int blurSurfaceColor(Context context) {
        return Palette.of(context).blurSurface;
    }

    static void applyBottomNavBar(View decorView) {
        if (decorView == null || !LineSettings.materialYouNavBar()) {
            return;
        }
        View bnbBg = BlurBars.find(decorView, "bnb_background_image");
        if (bnbBg == null || !bnbBg.isAttachedToWindow()) {
            return;
        }
        Context ctx = decorView.getContext();
        Palette palette = Palette.of(ctx);

        // Keep bnb_background_image at its native 49dp bar height (do NOT fill the 85dp parent
        // main_tab_container, whose upper 36dp is transparent overhang for themed tab artwork).
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

        View sysNavBg = BlurBars.find(decorView, "main_navigation_bar_background");
        if (sysNavBg != null) {
            if (sysNavBg.getVisibility() != View.VISIBLE) {
                sysNavBg.setVisibility(View.VISIBLE);
            }
            if (!(sysNavBg.getBackground() instanceof ColorDrawable)
                    || ((ColorDrawable) sysNavBg.getBackground()).getColor() != palette.navBarBg) {
                sysNavBg.setBackground(new ColorDrawable(palette.navBarBg));
            }
        }

        for (String tabName : BNB_TABS) {
            View tab = BlurBars.find(decorView, tabName);
            if (tab == null || tab.getVisibility() != View.VISIBLE) {
                continue;
            }
            View iconView = BlurBars.find(tab, "bnb_button_image");
            View textView = BlurBars.find(tab, "bnb_button_text");
            boolean selected = tab.isSelected()
                    || (iconView != null && iconView.isSelected())
                    || (textView != null && textView.isSelected());
            int tint = selected ? palette.navSelected : palette.navUnselected;

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
            }
            tintBadge(BlurBars.find(tab, "bnb_button_badge_num"), palette);
            tintBadge(BlurBars.find(tab, "default_theme_bnb_button_badge_num"), palette);
        }
    }

    /**
     * Tints all chat list unread-count pills (e.g. "55", "37", "5", "185", "999+", "N") across
     * every ViewPager2 folder page, AND tints the Compose header pill ("聊天") and folder filter
     * pills ("全部 / 好友 / 群組 / 官方帳號 / 社群") with the Material You accent palette.
     */
    static void clearComposeTintIfNeeded() {
        if (COMPOSE_TINTED.isEmpty()) {
            return;
        }
        for (View v : new java.util.ArrayList<>(COMPOSE_TINTED.keySet())) {
            if (v != null) {
                v.setLayerType(View.LAYER_TYPE_NONE, null);
                BADGE_APPLIED.remove(v);
            }
        }
        COMPOSE_TINTED.clear();
    }

    static void applyChatListBadges(View decorView) {
        if (decorView == null) {
            return;
        }
        if (!LineSettings.materialYouTheme()) {
            clearComposeTintIfNeeded();
            return;
        }
        Context ctx = decorView.getContext();
        Palette palette = Palette.of(ctx);

        // 1. Tint Compose folder filter pills ("全部 / 好友 / 群組 / 官方帳號 / 社群")
        View tabLayout = BlurBars.find(decorView, "tab_layout_view");
        if (tabLayout != null && tabLayout.isAttachedToWindow()) {
            tintComposeSurface(tabLayout, palette);
        }

        // 2. Locate ChatListComposeHeader ("聊天 ▾ / 好友") via chat_list_view_pager's root, and
        //    walk ALL chat_list_recycler_view instances inside chat_list_view_pager (all folder tabs).
        int rvId = BlurBars.id(ctx, "chat_list_recycler_view");
        View pager = BlurBars.find(decorView, "chat_list_view_pager");
        if (pager instanceof ViewGroup && pager.isAttachedToWindow()) {
            View chatRoot = (pager.getParent() instanceof View && ((View) pager.getParent()).getParent() instanceof View)
                    ? (View) ((View) pager.getParent()).getParent()
                    : null;
            if (chatRoot != null) {
                View chatHeader = BlurBars.find(chatRoot, "header");
                if (chatHeader != null && chatHeader.getClass().getName().endsWith("ChatListComposeHeader")) {
                    tintComposeSurface(chatHeader, palette);
                }
            }
            tintAllChatListsInTree((ViewGroup) pager, rvId, palette);
        } else {
            View singleList = rvId != 0 ? decorView.findViewById(rvId) : null;
            if (singleList instanceof ViewGroup && singleList.isAttachedToWindow()) {
                tintChatListRows((ViewGroup) singleList, palette);
            }
        }
    }

    private static void tintAllChatListsInTree(ViewGroup group, int recyclerId, Palette palette) {
        if (recyclerId != 0 && group.getId() == recyclerId) {
            tintChatListRows(group, palette);
            return;
        }
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child instanceof ViewGroup && child.getVisibility() == View.VISIBLE) {
                tintAllChatListsInTree((ViewGroup) child, recyclerId, palette);
            }
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

    /**
     * Maps black selected pills ("聊天 ▾", "全部") and green unread dots in LINE's Compose header
     * and folder tabs to the Material You accent color while preserving pure white backgrounds.
     */
    private static void tintComposeSurface(View view, Palette palette) {
        int accent = palette.accent;
        Integer applied = BADGE_APPLIED.get(view);
        if (applied != null && applied == accent && view.getLayerType() == View.LAYER_TYPE_HARDWARE) {
            return;
        }
        float r = Color.red(accent) / 255.0f;
        float g = Color.green(accent) / 255.0f;
        float b = Color.blue(accent) / 255.0f;
        float[] m;
        if (!palette.dark) {
            // Light mode: White (255) stays pure White; Black (0) becomes exact Material You accent;
            // Green dots desaturate into a soft Material You tint.
            float lr = 0.299f;
            float lg = 0.587f;
            float lb = 0.114f;
            float kr = 1.0f - r;
            float kg = 1.0f - g;
            float kb = 1.0f - b;
            m = new float[]{
                    kr * lr, kr * lg, kr * lb, 0f, r * 255.0f,
                    kg * lr, kg * lg, kg * lb, 0f, g * 255.0f,
                    kb * lr, kb * lg, kb * lb, 0f, b * 255.0f,
                    0f,      0f,      0f,      1f, 0f
            };
        } else {
            // Dark mode: Black (0) stays Black; White selected pills become Material You light accent.
            float lr = 0.299f;
            float lg = 0.587f;
            float lb = 0.114f;
            m = new float[]{
                    r * lr, r * lg, r * lb, 0f, 0f,
                    g * lr, g * lg, g * lb, 0f, 0f,
                    b * lr, b * lg, b * lb, 0f, 0f,
                    0f,     0f,     0f,     1f, 0f
            };
        }
        Paint layerPaint = new Paint();
        layerPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(m));
        view.setLayerType(View.LAYER_TYPE_HARDWARE, layerPaint);
        BADGE_APPLIED.put(view, accent);
        COMPOSE_TINTED.put(view, Boolean.TRUE);
    }

    private static void tintBadge(View view, Palette palette) {
        if (!(view instanceof TextView) || view.getVisibility() != View.VISIBLE) {
            return;
        }
        TextView tv = (TextView) view;
        if (tv.getCurrentTextColor() != palette.onBadge) {
            tv.setTextColor(palette.onBadge);
        }
        tintBadgeBg(tv, palette);
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

    static void applyChatRoom(View decorView, View header, View listView) {
        if (decorView == null || header == null) {
            return;
        }
        String chatId = resolveChatId(decorView, header);
        if (chatId != null && !chatId.isEmpty()) {
            lastActiveChatId = chatId;
        }
        View titleView = BlurBars.find(header, "header_title");
        if (titleView instanceof TextView && ((TextView) titleView).getText() != null) {
            lastActiveChatTitle = ((TextView) titleView).getText().toString();
        }
        if (listView instanceof ViewGroup) {
            applyChatBubbles((ViewGroup) listView, chatId);
        }
    }

    /**
     * Adds a native-styled "Chat bubble color" row to LINE's Chat Room Settings screen
     * (opened from top-right menu -> Settings, alongside "Background").
     */
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
        final String chatTitle = chatId.equals(lastActiveChatId) ? lastActiveChatTitle : "";

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

            // Top hairline divider
            View divider = new View(activity);
            divider.setBackgroundColor(palette.dark ? 0x22FFFFFF : 0x1A000000);
            wrapper.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, BlurBars.dp(activity, 0.7f))));

            // Main clickable row matching LINE ChatSettingActivity style
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int padH = BlurBars.dp(activity, 20.0f);
            int padV = BlurBars.dp(activity, 15.0f);
            row.setPadding(padH, padV, padH, padV);
            row.setClickable(true);
            row.setFocusable(true);
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

            TextView subTv = new TextView(activity);
            subTv.setText(zh ? "自訂此聊天室的送出與接收氣泡顏色" : "Customize sent & received bubbles for this chat");
            subTv.setTextSize(12.5f);
            subTv.setTextColor(subColor);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = BlurBars.dp(activity, 2.0f);
            textCol.addView(subTv, subLp);

            row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            // Two live color preview dots (Received & Sent)
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
                int cSend = LineSettings.chatBubbleSendColor(chatId);
                int cRecv = LineSettings.chatBubbleReceiveColor(chatId);
                styleColorDot(activity, recvDot, cRecv != 0 ? cRecv : p.receiveBubble);
                styleColorDot(activity, sendDot, cSend != 0 ? cSend : p.sendBubble);
            };
            refreshDots.run();

            row.setOnClickListener(v -> {
                PixelNativeEnhancements.tick(v);
                showBubbleColorDialog(activity, chatId, chatTitle, () -> {
                    BUBBLE_APPLIED.clear();
                    refreshDots.run();
                });
            });

            wrapper.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Keep clear of system gesture/navigation bar
            wrapper.setOnApplyWindowInsetsListener((v, insets) -> {
                int bottomInset = Build.VERSION.SDK_INT >= 30
                        ? insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                        : insets.getSystemWindowInsetBottom();
                v.setPadding(0, 0, 0, bottomInset);
                return insets;
            });

            android.widget.FrameLayout.LayoutParams flp = new android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );
            content.addView(wrapper, flp);
            wrapper.requestApplyInsets();
        });
    }

    private static void styleColorDot(Context ctx, View dot, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke(Math.max(1, BlurBars.dp(ctx, 1.0f)), 0x33888888);
        dot.setBackground(gd);
    }

    private static String resolveSettingsChatId(Activity activity) {
        Intent intent = activity.getIntent();
        Bundle extras = intent == null ? null : intent.getExtras();
        if (extras != null) {
            for (String key : new String[]{"chatId", "chat_id", "mid", "squareChatMid", "EXTRA_CHAT_ID"}) {
                String val = extras.getString(key);
                if (val != null && !val.isEmpty()) {
                    return val;
                }
            }
        }
        return (lastActiveChatId != null && !lastActiveChatId.isEmpty()) ? lastActiveChatId : "default_chat";
    }

    private static String resolveChatId(View decorView, View header) {
        String cached = CHAT_IDS.get(decorView);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        Activity activity = activityOf(decorView.getContext());
        if (activity != null) {
            Intent intent = activity.getIntent();
            Bundle extras = intent == null ? null : intent.getExtras();
            if (extras != null) {
                try {
                    Object req = extras.get("chatHistoryRequest");
                    if (req != null) {
                        Object id = req.getClass().getMethod("getChatId").invoke(req);
                        if (id instanceof String && !((String) id).isEmpty()) {
                            CHAT_IDS.put(decorView, (String) id);
                            return (String) id;
                        }
                    }
                } catch (Throwable ignored) {
                }
                for (String key : new String[]{"chatId", "chat_id", "mid"}) {
                    String val = extras.getString(key);
                    if (val != null && !val.isEmpty()) {
                        CHAT_IDS.put(decorView, val);
                        return val;
                    }
                }
            }
        }
        View titleView = BlurBars.find(header, "header_title");
        if (titleView instanceof TextView) {
            CharSequence text = ((TextView) titleView).getText();
            if (text != null && text.length() > 0) {
                String fallback = "title_" + text.toString();
                CHAT_IDS.put(decorView, fallback);
                return fallback;
            }
        }
        return "default_chat";
    }

    static void applyChatBubbles(ViewGroup listView, String chatId) {
        int customSend = LineSettings.chatBubbleSendColor(chatId);
        int customRecv = LineSettings.chatBubbleReceiveColor(chatId);
        boolean myEnabled = LineSettings.materialYouTheme();
        Palette palette = (myEnabled || customSend != 0 || customRecv != 0) ? Palette.of(listView.getContext()) : null;
        int sendColor = customSend != 0 ? customSend : (myEnabled && palette != null ? palette.sendBubble : 0);
        int recvColor = customRecv != 0 ? customRecv : (myEnabled && palette != null ? palette.receiveBubble : 0);

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
            Drawable bg = bubble.getBackground();
            if (bg != null) {
                bg.clearColorFilter();
            }
        }
    }

    private static void tintRowBubbles(View row, int bubbleColor) {
        boolean lightBubble = isLightColor(bubbleColor);
        int textColor = lightBubble ? 0xFF1A1C1E : 0xFFFFFFFF;
        int subTextColor = lightBubble ? 0xB31A1C1E : 0xCCFFFFFF;
        int linkColor = lightBubble ? 0xFF0B57D0 : 0xFFA8C7FA;

        for (String idName : BUBBLE_CONTAINER_IDS) {
            View bubble = BlurBars.find(row, idName);
            if (bubble == null) {
                continue;
            }
            Drawable bg = bubble.getBackground();
            if (bg == null) {
                continue;
            }
            Integer prev = BUBBLE_APPLIED.get(bubble);
            if (prev == null || prev != bubbleColor || bg.getColorFilter() == null) {
                bg.mutate().setColorFilter(bubbleColor, PorterDuff.Mode.SRC_IN);
                BUBBLE_APPLIED.put(bubble, bubbleColor);
            }
            for (String textId : BUBBLE_TEXT_IDS) {
                View tv = BlurBars.find(bubble, textId);
                if (tv instanceof TextView) {
                    TextView textView = (TextView) tv;
                    if (textView.getCurrentTextColor() != textColor) {
                        textView.setTextColor(textColor);
                    }
                    textView.setLinkTextColor(linkColor);
                }
            }
            for (String subId : BUBBLE_SUBTEXT_IDS) {
                View tv = BlurBars.find(bubble, subId);
                if (tv instanceof TextView) {
                    TextView textView = (TextView) tv;
                    if (textView.getCurrentTextColor() != subTextColor) {
                        textView.setTextColor(subTextColor);
                    }
                }
            }
        }
    }

    private static void showBubbleColorDialog(Activity activity, String chatId, String chatTitle, Runnable onChanged) {
        Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
        boolean zh = locale != null && "zh".equals(locale.getLanguage());
        Palette palette = Palette.of(activity);

        final int[] selectedSend = {LineSettings.chatBubbleSendColor(chatId)};
        final int[] selectedRecv = {LineSettings.chatBubbleReceiveColor(chatId)};

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = BlurBars.dp(activity, 18.0f);
        root.setPadding(pad, BlurBars.dp(activity, 12.0f), pad, BlurBars.dp(activity, 8.0f));
        scroll.addView(root);

        // Live preview row
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
        previewLp.bottomMargin = BlurBars.dp(activity, 16f);
        root.addView(previewBox, previewLp);

        final Runnable updatePreview = () -> {
            int effSend = selectedSend[0] != 0 ? selectedSend[0] : palette.sendBubble;
            int effRecv = selectedRecv[0] != 0 ? selectedRecv[0] : palette.receiveBubble;
            stylePreviewBubble(sendPreview, effSend, BlurBars.dp(activity, 16f));
            stylePreviewBubble(recvPreview, effRecv, BlurBars.dp(activity, 16f));
        };

        // Sent bubble picker
        root.addView(sectionLabel(activity, zh ? "我的訊息氣泡顏色（送出）" : "My bubble color (Sent)", palette.accent));
        final View[][] sendSwatches = new View[1][];
        root.addView(buildSwatchGrid(activity, selectedSend, palette.sendBubble, updatePreview, sendSwatches, zh));
        root.addView(buildHexRow(activity, selectedSend, updatePreview, sendSwatches, zh));

        // Received bubble picker
        TextView recvLabel = sectionLabel(activity, zh ? "對方訊息氣泡顏色（接收）" : "Their bubble color (Received)", palette.accent);
        LinearLayout.LayoutParams rlLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlLp.topMargin = BlurBars.dp(activity, 14f);
        root.addView(recvLabel, rlLp);
        final View[][] recvSwatches = new View[1][];
        root.addView(buildSwatchGrid(activity, selectedRecv, palette.receiveBubble, updatePreview, recvSwatches, zh));
        root.addView(buildHexRow(activity, selectedRecv, updatePreview, recvSwatches, zh));

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
                    if (onChanged != null) onChanged.run();
                })
                .setNeutralButton(zh ? "重設預設" : "Reset", (d, w) -> {
                    PixelNativeEnhancements.confirm(scroll);
                    LineSettings.clearChatBubbleColors(chatId);
                    if (onChanged != null) onChanged.run();
                })
                .setNegativeButton(zh ? "取消" : "Cancel", null)
                .create();
        PixelNativeEnhancements.applyDialogBlur(dialog);
        dialog.show();
    }

    private static void stylePreviewBubble(TextView tv, int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(radius);
        gd.setColor(color);
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
            final int autoColor,
            final Runnable updatePreview,
            final View[][] swatchHolder,
            boolean zh
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
            TextView circle = new TextView(ctx);
            circle.setGravity(Gravity.CENTER);
            circle.setTextSize(10f);
            if (color == 0) {
                circle.setText(zh ? "自動" : "Auto");
                circle.setTextColor(isLightColor(autoColor) ? 0xFF1A1C1E : 0xFFFFFFFF);
            }
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(size, size);
            clp.setMarginEnd(gap);
            circle.setLayoutParams(clp);
            swatches[i] = circle;
            circle.setOnClickListener(v -> {
                PixelNativeEnhancements.tick(v);
                target[0] = color;
                refreshSwatchBorders(ctx, swatches, target[0], autoColor);
                updatePreview.run();
            });
            row.addView(circle);
        }
        refreshSwatchBorders(ctx, swatches, target[0], autoColor);
        return grid;
    }

    private static void refreshSwatchBorders(Context ctx, View[] swatches, int selectedColor, int autoColor) {
        int strokeSelected = BlurBars.dp(ctx, 2.5f);
        int strokeNormal = BlurBars.dp(ctx, 1.0f);
        for (int i = 0; i < swatches.length; i++) {
            int color = PRESET_COLORS[i];
            int fill = color == 0 ? autoColor : color;
            boolean isSelected = selectedColor == color;
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(fill);
            gd.setStroke(isSelected ? strokeSelected : strokeNormal, isSelected ? 0xFF1A73E8 : 0x44888888);
            swatches[i].setBackground(gd);
        }
    }

    private static LinearLayout buildHexRow(
            Context ctx,
            final int[] target,
            final Runnable updatePreview,
            final View[][] swatchHolder,
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
        if (target[0] != 0) {
            hexInput.setText(String.format(Locale.US, "#%06X", 0xFFFFFF & target[0]));
        }
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(hexInput, elp);

        TextView applyBtn = new TextView(ctx);
        applyBtn.setText(zh ? "預覽" : "Set");
        applyBtn.setTextSize(13f);
        applyBtn.setTextColor(accentColor(ctx));
        int bp = BlurBars.dp(ctx, 10f);
        applyBtn.setPadding(bp, bp, bp, bp);
        applyBtn.setOnClickListener(v -> {
            String raw = hexInput.getText().toString().trim();
            if (!raw.startsWith("#")) raw = "#" + raw;
            try {
                int parsed = Color.parseColor(raw);
                target[0] = parsed | 0xFF000000;
                if (swatchHolder[0] != null) {
                    refreshSwatchBorders(ctx, swatchHolder[0], target[0], Palette.of(ctx).sendBubble);
                }
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

    private static int blend(int base, int overlay, float ratio) {
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
