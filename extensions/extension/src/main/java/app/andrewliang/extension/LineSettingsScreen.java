package app.andrewliang.extension;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * The "Andrew's Patch Setting" screen: the runtime switches of {@link LineSettings}, then credits
 * and the license.
 *
 * <p>It is a full-screen {@link Dialog} over LINE's Settings activity, so it needs no manifest
 * entry and no fragment. It looks like LINE's own settings pages because it inflates LINE's own
 * row layouts and icons. It finds them by resource name, which R8 keeps, because the ids change
 * between LINE versions.
 *
 * <p>The colors come from the Settings page under the dialog. LINE's theme engine paints a theme
 * that the user picks, such as Black, over the resource colors, so resources alone give the
 * wrong colors. {@link Look} copies what LINE drew, and the resources are only the fallback.
 */
final class LineSettingsScreen {

    private LineSettingsScreen() {}

    private static final String SOURCE_URL = "https://github.com/andrewliang25/morphe-patches";
    private static final String LICENSE_URL = SOURCE_URL + "/blob/main/LICENSE";

    static final String LICENSE_NOTICE =
            "Andrew's Patches is free software: you can redistribute it and/or modify it under the "
                    + "terms of the GNU General Public License as published by the Free Software "
                    + "Foundation, either version 3 of the License, or (at your option) any later "
                    + "version.\n\n"
                    + "It is distributed in the hope that it will be useful, but WITHOUT ANY "
                    + "WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR "
                    + "A PARTICULAR PURPOSE. See the GNU General Public License for more details: "
                    + "https://www.gnu.org/licenses/gpl-3.0.html\n\n"
                    + "Morphe NOTICE\n\n"
                    + "7c. Project Name Restriction\n\n"
                    + "This license does not grant permission to use the name \"Morphe\", or any "
                    + "name, logo, or branding that is confusingly similar, as the name or primary "
                    + "identifying mark of any modified version, fork, or derivative work.\n\n"
                    + "Any redistribution or derivative work must adopt a distinct name and branding "
                    + "that clearly differentiates it from the Morphe project and does not cause "
                    + "confusion as to source or origin.\n\n"
                    + "References to Morphe are permitted solely for descriptive compatibility "
                    + "purposes (e.g., \"compatible with Morphe\" or \"XYZ patches for use with "
                    + "Morphe\"), provided that such references:\n\n"
                    + "- Are accurate, factual, and non-misleading;\n"
                    + "- Do not suggest authorship by the Morphe project; and\n"
                    + "- Appear only in a secondary or descriptive context, not as part of the "
                    + "product or project name.";

    private static boolean chinese;

    static void show(Context context) {
        Activity activity = activityOf(context);
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        look = Look.of(activity);
        Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
        chinese = locale != null && "zh".equals(locale.getLanguage());
        int fallback = activity.getColor(id(activity, "primaryBackground", "color"));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(look.background(activity, fallback));
        root.addView(header(activity, dialog));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addSwitches(activity, content);
        addCredits(activity, content);

        // The dialog draws behind the system bars, so the screen keeps clear of them and cutouts,
        // while bounding content width on foldables, tablets, and desktop windows.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            int side = PixelNativeEnhancements.computeAdaptiveSidePadding(
                    activity,
                    view.getWidth() - bars.left - bars.right,
                    640.0f
            );
            content.setPadding(side, 0, side, 0);
            return insets;
        });
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or2, ob) -> {
            int avail = (r - l) - v.getPaddingLeft() - v.getPaddingRight();
            int side = PixelNativeEnhancements.computeAdaptiveSidePadding(activity, avail, 640.0f);
            if (content.getPaddingLeft() != side || content.getPaddingRight() != side) {
                content.setPadding(side, 0, side, 0);
            }
        });

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(look.background(activity, fallback));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // Dark system bar icons on a light background, as on LINE's own pages.
            int light = look.isLight(fallback)
                    ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    : 0;
            WindowInsetsController bars = window.getInsetsController();
            if (bars != null) {
                bars.setSystemBarsAppearance(light, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
        dialog.show();
    }

    private static void addSwitches(Activity activity, LinearLayout content) {
        boolean chats = LineSettings.keepChatsUnreadIncluded() || LineSettings.keepUnsentMessagesIncluded()
                || LineSettings.smoothKeyboardIncluded() || LineSettings.chatHapticsIncluded()
                || LineSettings.scrollRefreshRateIncluded();
        boolean general = LineSettings.externalBrowserIncluded() || LineSettings.disableVoomIncluded()
                || LineSettings.customFontIncluded() || LineSettings.blurSystemBarsIncluded()
                || LineSettings.predictiveBackIncluded() || LineSettings.skipCustomSplashIncluded();
        boolean tabs = LineSettings.hideTodayTabIncluded() || LineSettings.hideShoppingTabIncluded()
                || LineSettings.hideVoomTabIncluded() || LineSettings.hideWalletTabIncluded();
        boolean buttons = LineSettings.hideAiFriendsIncluded() || LineSettings.hideCommunityButtonIncluded()
                || LineSettings.hideCalendarIncluded() || LineSettings.hideEventsIncluded()
                || LineSettings.hideGiftIncluded() || LineSettings.hideTransferIncluded()
                || LineSettings.hideAttachToolsIncluded() || LineSettings.hideChatTipsIncluded()
                || LineSettings.hideLineAiImageEditIncluded() || LineSettings.hideAgentIIncluded();
        boolean calls = LineSettings.autoRecordCallsIncluded() || LineSettings.silentCallRecordingIncluded()
                || LineSettings.saveCallRecordingsToDeviceIncluded() || LineSettings.removeCallRecordingLimitIncluded();

        if (!chats && !general && !tabs && !buttons && !calls) {
            content.addView(note(activity, t("No patches in this build have a switch.", "這個版本沒有可切換的修補程式。")));
            return;
        }

        if (chats) {
            content.addView(section(activity, t("Chats", "聊天")));
            if (LineSettings.keepChatsUnreadIncluded()) {
                content.addView(toggle(activity, LineSettings.KEEP_CHATS_UNREAD,
                        t("Keep chats unread", "保持聊天未讀"),
                        t("Opening a chat does not mark it read or send a read receipt.", "開啟聊天室不會標為已讀，也不會傳送已讀通知。"), null));
            }
            if (LineSettings.keepUnsentMessagesIncluded()) {
                content.addView(toggle(activity, LineSettings.KEEP_UNSENT_MESSAGES,
                        t("Keep unsent messages", "保留已收回的訊息"),
                        t("Keeps messages that others unsend, with a red \"Unsent\" label next to the time.", "對方收回的訊息會保留下來，並在時間旁顯示紅色的「已收回」標籤。"), null));
            }
            if (LineSettings.smoothKeyboardIncluded()) {
                content.addView(toggle(activity, LineSettings.SMOOTH_KEYBOARD,
                        t("Smooth keyboard animation", "流暢的鍵盤動畫"),
                        t("The blurred input bar follows the keyboard frame by frame, with less work on each frame. Android 11 and later.", "模糊的輸入列逐格跟著鍵盤移動，每格的運算更少。需要 Android 11 以上。"), null));
            }
            if (LineSettings.chatHapticsIncluded()) {
                content.addView(toggle(activity, LineSettings.CHAT_HAPTICS,
                        t("Haptic feedback", "觸覺回饋"),
                        t("A short vibration when you long-press a message, send, or unsend. Follows the system touch feedback setting.", "長按訊息、傳送或收回時短暫震動。依照系統的觸控回饋設定。"), null));
            }
            if (LineSettings.scrollRefreshRateIncluded()) {
                content.addView(toggle(activity, LineSettings.SCROLL_REFRESH_RATE,
                        t("High refresh rate while scrolling", "捲動時使用高更新率"),
                        t("Uses the highest screen refresh rate only while messages scroll. Never in Battery Saver.", "只在捲動訊息時使用最高的螢幕更新率。省電模式下不會啟用。"), null));
            }
        }

        if (buttons) {
            content.addView(section(activity, t("Chat buttons", "聊天按鈕")));
            View restart = row(activity, t("Restart LINE", "重新啟動 LINE"), t("Shows your button changes now.", "立即套用按鈕的變更。"), true);
            restart.setVisibility(View.GONE);
            restart.setOnClickListener(v -> restart(activity));
            Runnable showRestart = () -> restart.setVisibility(View.VISIBLE);

            if (LineSettings.hideAiFriendsIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_AI_FRIENDS,
                        t("Hide AI Friends button", "隱藏 AI 好友按鈕"),
                        t("In the Chats tab. The Album button shows in its place.", "在聊天分頁。改為顯示相簿按鈕。"), showRestart));
            }
            if (LineSettings.hideCommunityButtonIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_COMMUNITY_BUTTON,
                        t("Hide community button", "隱藏社群按鈕"),
                        t("In the Chats tab.", "在聊天分頁。"), showRestart));
            }
            if (LineSettings.hideCalendarIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_CALENDAR,
                        t("Hide calendar buttons", "隱藏行事曆按鈕"),
                        t("In the Chats tab, in chats and in the Home tab.", "在聊天分頁、聊天室和主頁分頁。"), showRestart));
            }
            if (LineSettings.hideEventsIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_EVENTS,
                        t("Hide Events button", "隱藏活動按鈕"),
                        t("In the chat menu.", "在聊天室選單。"), showRestart));
            }
            if (LineSettings.hideGiftIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_GIFT,
                        t("Hide LINE GIFT button", "隱藏 LINE 禮物按鈕"),
                        t("In the + menu of a chat.", "在聊天室的 + 選單。"), showRestart));
            }
            if (LineSettings.hideTransferIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_TRANSFER,
                        t("Hide Transfer button", "隱藏轉帳按鈕"),
                        t("In the + menu of a chat.", "在聊天室的 + 選單。"), showRestart));
            }
            if (LineSettings.hideAttachToolsIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_ATTACH_TOOLS,
                        t("Hide attach menu extra tools", "隱藏 + 選單的額外工具"),
                        t("Poll, Schedule and other server tools in the + menu of a chat.", "聊天室 + 選單中的投票、活動排程等伺服器工具。"), showRestart));
            }
            if (LineSettings.hideChatTipsIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_CHAT_TIPS,
                        t("Hide tips under messages", "隱藏訊息下方的提示"),
                        t("The \"CHECK\" tips under some messages.", "部分訊息下方的「CHECK」提示。"), showRestart));
            }
            if (LineSettings.hideLineAiImageEditIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_LINE_AI_IMAGE_EDIT,
                        t("Hide LINE AI image editing", "隱藏 LINE AI 圖片編輯"),
                        t("\"AI Edit\" in chats and the AI button in photo viewers.", "聊天室中的「AI 編輯」，以及看圖畫面的 AI 按鈕。"), showRestart));
            }
            if (LineSettings.hideAgentIIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_AGENT_I,
                        t("Hide Agent i", "隱藏 Agent i"),
                        t("In the Home tab, the search bar, chats, the + menu, the Wallet tab and Settings.", "在主頁分頁、搜尋列、聊天室、+ 選單、錢包分頁和設定。"),
                        showRestart));
            }
            content.addView(note(activity, t("Some button changes show only after LINE restarts.", "部分按鈕的變更要在 LINE 重新啟動後才會生效。")));
            content.addView(restart);
        }

        if (general) {
            content.addView(section(activity, t("General", "一般")));
            if (LineSettings.externalBrowserIncluded()) {
                content.addView(toggle(activity, LineSettings.EXTERNAL_BROWSER,
                        t("Open links in external browser", "用外部瀏覽器開啟連結"),
                        t("Opens web links in your default browser instead of LINE's browser.", "網頁連結改用預設瀏覽器開啟，而不是 LINE 的內建瀏覽器。"), null));
            }
            if (LineSettings.disableVoomIncluded()) {
                content.addView(toggle(activity, LineSettings.DISABLE_VOOM,
                        t("Disable VOOM", "停用 VOOM"),
                        t("VOOM links, shares and notifications do nothing.", "VOOM 的連結、分享和通知都不會有作用。"), null));
            }
            if (LineSettings.customFontIncluded()) {
                // A restart in a new task does not change the font of LINE's Compose screens.
                View close = row(activity, t("Close LINE", "關閉 LINE"), t("Open LINE again to apply your font change.", "重新開啟 LINE 以套用字型變更。"), true);
                close.setVisibility(View.GONE);
                close.setOnClickListener(v -> CustomFontPicker.exit(activity));
                content.addView(toggle(activity, LineSettings.CUSTOM_FONT,
                        t("Custom font", "自訂字型"),
                        t("Uses the font that you upload on LINE's Font page.", "使用你在 LINE 字型頁面上傳的字型。"),
                        () -> close.setVisibility(View.VISIBLE)));
                content.addView(close);
            }
            if (LineSettings.blurSystemBarsIncluded()) {
                View restart = row(activity, t("Restart LINE", "重新啟動 LINE"), t("Shows your system bar change now.", "立即套用系統列的變更。"), true);
                restart.setVisibility(View.GONE);
                restart.setOnClickListener(v -> restart(activity));
                content.addView(toggle(activity, LineSettings.BLUR_SYSTEM_BARS,
                        t("Transparent system bars with blur", "透明系統列與模糊效果"),
                        t("The theme artwork fills the status bar and the gesture bar. In chats, messages blur out behind the header and the input bar.", "主題圖片延伸到狀態列和手勢列。聊天室中，訊息在標題列和輸入列後方逐漸模糊。"),
                        () -> restart.setVisibility(View.VISIBLE)));
                content.addView(toggle(activity, LineSettings.BLUR_AUTO_COLOR,
                        t("Auto color", "自動配色"),
                        t("Tints the chat blur with the colors of the chat background image, at the top and at the bottom, and picks black or white icons to match. Chats with no background image keep the normal colors. Overrides Blur day/night.", "依聊天背景圖片上方與下方的顏色，為聊天室的模糊效果上色，並自動選用黑色或白色圖示。沒有背景圖片的聊天室維持一般配色。開啟時會取代「模糊日夜模式」。"),
                        false, null));
                content.addView(dayNightChoice(activity));
                content.addView(toggle(activity, LineSettings.CHAT_HEADER_WHITE,
                        t("White chat header text & icons", "聊天室標題列使用白色文字與圖示"),
                        t("Keeps the chat name and right-side header icons white so they stay easy to read on any background.", "將聊天室上方對方名字與右側圖示統一改為白色，確保在各種背景上都清晰易讀。"),
                        true, null));
                content.addView(toggle(activity, LineSettings.MATERIAL_YOU_THEME,
                        t("Material You dynamic colors", "Material You 動態配色"),
                        t("Applies your Android wallpaper palette across LINE, including chat bubbles, unread count badges, folder tabs, and settings accents. Open any chat's menu -> Settings -> Chat bubble color to customize that chat individually.", "將系統桌布的 Material You 個人調色套用到 LINE，包含聊天氣泡、未讀訊息數字膠囊、分類膠囊與設定強調色。可於聊天室右上角選單 → 設定 → 聊天氣泡顏色單獨自訂各聊天室氣泡。"),
                        true, null));
                content.addView(toggle(activity, LineSettings.CHAT_BUBBLE_WALLPAPER,
                        t("Auto-extract chat wallpaper color for bubbles", "聊天氣泡自動抓取聊天室背景主色"),
                        t("Automatically picks harmonious sent and received bubble colors from each chat room's background image. Chats with no background image fall back to Material You.", "自動從各聊天室的背景圖片擷取主色調來搭配送出與接收氣泡；無背景圖片的聊天室則自動使用系統 Material You 配色。"),
                        true, null));
                content.addView(toggle(activity, LineSettings.MATERIAL_YOU_NAV_BAR,
                        t("Material You bottom navigation bar", "底部導覽列使用 Material You 配色"),
                        t("Colors the main screen bottom tabs (Home, Chats, etc.) with Material You instead of black.", "主畫面底部的分頁列（主頁、聊天等）改用 Material You 配色取代原本的黑色。"),
                        true, () -> restart.setVisibility(View.VISIBLE)));
                content.addView(toggle(activity, LineSettings.PIXEL_ULTRA_HDR,
                        t("Ultra HDR & Wide Color Gamut", "Ultra HDR 與廣色域顯示"),
                        t("Enables HDR / Display P3 color mode and full cutout rendering on photo & video viewers (Pixel Pro / OLED displays).", "在圖片、影片瀏覽與聊天室啟用 HDR / 廣色域色彩模式與全螢幕挖孔延伸顯示。"),
                        true, null));
                content.addView(toggle(activity, LineSettings.PIXEL_ARR_TOUCH_BOOST,
                        t("Adaptive Refresh Rate touch boost", "自適應更新率觸控即時升頻（Android 15+）"),
                        t("Uses Android 15+ LTPO touch-boost so scrolling jumps smoothly to 120Hz on touch and idles cleanly. Disabled in Battery Saver.", "使用 Android 15+ 原生 LTPO 觸控升頻 API，觸碰瞬間平滑升至高更新率，靜止時自動降頻省電。"),
                        true, null));
                content.addView(toggle(activity, LineSettings.PIXEL_FOLD_ADAPTIVE,
                        t("Google Play Adaptive Quality layout", "摺疊機、平板與多視窗自適應排版"),
                        t("Unlocks large-screen rotation, centers chats & dialogs across 1:1, 4:3, and 16:9 windows, respects hinge & cutout insets, and supports mouse/stylus hover.", "依循 Google Play 自適應品質標準：大螢幕自動解鎖旋轉限制，針對 1:1、4:3 與 16:9 視窗置中最佳閱讀寬度、避開摺疊轉軸與挖孔，並支援滑鼠與觸控筆懸停回饋。"),
                        true, null));
                content.addView(toggle(activity, LineSettings.PIXEL_WINDOW_BLUR,
                        t("Hardware dialog blur", "系統硬體加速毛玻璃視窗（Android 12+）"),
                        t("Uses Android 12+ GPU cross-window blur behind settings and color picker dialogs.", "在設定與氣泡調色視窗後方啟用 Android 12+ 原生 GPU 視窗模糊。"),
                        true, null));
                content.addView(restart);
            }
            if (LineSettings.predictiveBackIncluded()) {
                View restart = row(activity, t("Restart LINE", "重新啟動 LINE"), t("Applies your back gesture change now.", "立即套用返回手勢的變更。"), true);
                restart.setVisibility(View.GONE);
                restart.setOnClickListener(v -> restart(activity));
                content.addView(toggle(activity, LineSettings.PREDICTIVE_BACK,
                        t("Predictive back", "預測返回手勢"),
                        t("Shows Android's back animation as you swipe back, on screens that allow it. Android 14 and later. Off keeps the old back behavior.", "在支援的畫面上，滑動返回時顯示 Android 的返回動畫。需要 Android 14 以上。關閉則維持原本的返回方式。"),
                        () -> restart.setVisibility(View.VISIBLE)));
                content.addView(restart);
            }
            if (LineSettings.skipCustomSplashIncluded()) {
                content.addView(toggle(activity, LineSettings.SKIP_CUSTOM_SPLASH,
                        t("Skip custom splash", "略過自訂啟動畫面"),
                        t("Skips LINE's extra splash screen after Android's launch screen.", "略過 Android 啟動畫面之後 LINE 的額外啟動畫面。"), null));
            }
        }

        if (calls) {
            content.addView(section(activity, t("Calls", "通話")));
            if (LineSettings.autoRecordCallsIncluded()) {
                content.addView(toggle(activity, LineSettings.AUTO_RECORD_CALLS,
                        t("Auto-record calls", "自動錄製通話"),
                        t("Starts recording a 1-to-1 call as soon as it connects.", "一對一通話接通後立即開始錄音。"), null));
            }
            if (LineSettings.silentCallRecordingIncluded()) {
                content.addView(toggle(activity, LineSettings.SILENT_CALL_RECORDING,
                        t("Record without notifying", "錄音時不通知對方"),
                        t("The other person is not told that you record, and hears no recording announcement.", "不會告知對方你正在錄音，對方也不會聽到錄音提示。"), null));
            }
            if (LineSettings.saveCallRecordingsToDeviceIncluded()) {
                content.addView(toggle(activity, LineSettings.SAVE_CALL_RECORDINGS_TO_DEVICE,
                        t("Save recordings to device", "將錄音儲存到裝置"),
                        t("On: saves each recording to Movies/LINE on your phone. Off: shares it to Keep Memo, which refuses files over 1 GB. Each recording also stays in the chat's Call recordings list.", "開啟：每段錄音都存到手機的 Movies/LINE。關閉：分享到 Keep 筆記，但超過 1 GB 的檔案會被拒絕。每段錄音也都會留在聊天室的通話錄音清單中。"),
                        null));
            }
            if (LineSettings.removeCallRecordingLimitIncluded()) {
                content.addView(toggle(activity, LineSettings.REMOVE_CALL_RECORDING_LIMIT,
                        t("Remove recording time limit", "移除錄音時間限制"),
                        t("Records a call until it ends. Without this, LINE stops voice recordings after 60 minutes and video recordings after 20 or 50 minutes.", "錄到通話結束為止。未開啟時，LINE 會在語音通話 60 分鐘、視訊通話 20 或 50 分鐘後停止錄音。"),
                        null));
            }
        }

        if (tabs) {
            content.addView(section(activity, t("Tabs", "分頁")));
            View restart = row(activity, t("Restart LINE", "重新啟動 LINE"), t("Shows your tab changes now.", "立即套用分頁的變更。"), true);
            restart.setVisibility(View.GONE);
            restart.setOnClickListener(v -> restart(activity));
            Runnable showRestart = () -> restart.setVisibility(View.VISIBLE);

            if (LineSettings.hideTodayTabIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_TODAY_TAB, t("Hide LINE TODAY tab", "隱藏 LINE TODAY 分頁"), null, showRestart));
            }
            if (LineSettings.hideShoppingTabIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_SHOPPING_TAB, t("Hide Shopping tab", "隱藏購物分頁"), null, showRestart));
            }
            if (LineSettings.hideVoomTabIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_VOOM_TAB, t("Hide VOOM tab", "隱藏 VOOM 分頁"), null, showRestart));
            }
            if (LineSettings.hideWalletTabIncluded()) {
                content.addView(toggle(activity, LineSettings.HIDE_WALLET_TAB, t("Hide Wallet tab", "隱藏錢包分頁"), null, showRestart));
            }
            content.addView(note(activity, t("Tab changes show after LINE restarts.", "分頁的變更要在 LINE 重新啟動後才會生效。")));
            content.addView(restart);
        }
    }

    private static View dayNightChoice(Activity activity) {
        String[] labels = {
                t("Follow system", "跟隨系統"),
                t("Always day", "固定日間"),
                t("Always night", "固定夜間"),
        };
        View row = row(activity, t("Blur day/night", "模糊日夜模式"),
                t("Day or night look for the chat blur and its icons only. LINE's own theme does not change. Auto color, when on, takes over.",
                        "只改變聊天室模糊效果與圖示的日夜樣式，不影響 LINE 本身的主題。開啟「自動配色」時以自動配色為準。"),
                true);
        TextView value = row.findViewById(id(activity, "setting_inlined_value", "id"));
        value.setText(labels[LineSettings.blurDayNight()]);
        value.setVisibility(View.VISIBLE);
        look.paint(value, look.secondary());
        row.setOnClickListener(v -> {
            PixelNativeEnhancements.tick(v);
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(t("Blur day/night", "模糊日夜模式"))
                    .setSingleChoiceItems(labels, LineSettings.blurDayNight(), (d, which) -> {
                        LineSettings.setNumber(LineSettings.BLUR_DAY_NIGHT, which);
                        value.setText(labels[which]);
                        d.dismiss();
                    })
                    .setNegativeButton(t("Cancel", "取消"), null)
                    .create();
            PixelNativeEnhancements.applyDialogBlur(dialog);
            dialog.show();
            PixelNativeEnhancements.constrainDialogWidth(dialog, 480.0f);
        });
        return row;
    }

    private static void addCredits(Activity activity, LinearLayout content) {
        content.addView(section(activity, t("Credits & license", "致謝與授權")));

        View about = row(activity, "Andrew's Patches", t("By Andrew Liang", "作者：Andrew Liang"), true);
        about.setOnClickListener(v -> open(activity, SOURCE_URL));
        content.addView(about);

        View license = row(activity, t("License", "授權條款"), "GNU General Public License v3.0", true);
        license.setOnClickListener(v -> showLicense(activity));
        content.addView(license);

        content.addView(note(activity, t("This project has no connection to the Morphe open source project, "
                + "LINE, or LY Corporation. They do not endorse it and did not write it.",
                "本專案與 Morphe 開源專案、LINE 及 LY Corporation 無任何關係，他們並未背書，也未參與開發。")));
    }

    private static String t(String en, String zh) {
        return chinese ? zh : en;
    }

    /** A back arrow and the title, like the header of LINE's own settings pages. */
    private static View header(Activity activity, Dialog dialog) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        int height = activity.getResources().getDimensionPixelSize(id(activity, "header_height", "dimen"));
        header.setMinimumHeight(height);

        ImageView back = new ImageView(activity);
        back.setImageResource(id(activity, "header_ic_back_semantic", "drawable"));
        // LINE's theme engine colors the arrow on its view, not in the drawable, so the arrow
        // takes the color of the header title, as on LINE's own pages.
        ColorStateList titleColors = look.headerTitle != null ? look.headerTitle : look.title;
        if (titleColors != null) back.setImageTintList(titleColors);
        back.setContentDescription(t("Back", "返回"));
        back.setPadding(dp(activity, 4), 0, 0, 0);
        PixelNativeEnhancements.enhanceInteractive(back);
        back.setOnClickListener(v -> dialog.dismiss());
        header.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height));

        TextView title = new TextView(activity);
        title.setTextAppearance(id(activity, "text_header_title_new_design", "style"));
        title.setText(t("Andrew's Patch Setting", "Andrew 修補程式設定"));
        look.paint(title, titleColors);
        header.addView(title);
        return header;
    }

    /** LINE's switch row. A tap anywhere on the row flips the switch. */
    private static View toggle(Activity activity, String key, String title, String summary, Runnable onChange) {
        return toggle(activity, key, title, summary, true, onChange);
    }

    private static View toggle(Activity activity, String key, String title, String summary,
                               boolean defaultOn, Runnable onChange) {
        View row = inflate(activity, "line_user_settings_switch_item_view");
        text(activity, row, "setting_title", title);
        if (summary != null) text(activity, row, "setting_description", summary);
        // LINE's own switch pages draw no line between rows.
        row.findViewById(id(activity, "setting_divider", "id")).setVisibility(View.GONE);
        View container = row.findViewById(id(activity, "setting_item_container", "id"));
        look.press(container);
        PixelNativeEnhancements.enhanceInteractive(container);
        CheckedTextView toggle = row.findViewById(id(activity, "setting_switch", "id"));
        toggle.setChecked(LineSettings.isOn(key, defaultOn));
        container.setOnClickListener(v -> {
            PixelNativeEnhancements.tick(v);
            boolean on = !toggle.isChecked();
            toggle.setChecked(on);
            LineSettings.set(key, on);
            if (onChange != null) onChange.run();
        });
        return row;
    }

    /** LINE's text row, with the arrow when the row opens something. */
    private static View row(Activity activity, String title, String summary, boolean arrow) {
        View row = inflate(activity, "line_user_settings_text_item_view");
        text(activity, row, "setting_title", title);
        text(activity, row, "setting_description", summary);
        // The arrow is placed after the inline value, so that empty view keeps the arrow at the end.
        ((TextView) row.findViewById(id(activity, "setting_inlined_value", "id"))).setText("");
        row.findViewById(id(activity, "setting_arrow", "id")).setVisibility(arrow ? View.VISIBLE : View.GONE);
        // The caller listens on the whole row, so the container shows the row's pressed state.
        View container = row.findViewById(id(activity, "setting_item_container", "id"));
        container.setDuplicateParentStateEnabled(true);
        look.press(container);
        PixelNativeEnhancements.enhanceInteractive(row);
        return row;
    }

    /** A section title, like LINE's group header row. */
    private static TextView section(Activity activity, String title) {
        TextView section = new TextView(activity);
        section.setTextAppearance(id(activity, "line_user_settings_group_header_item_text_semantic", "style"));
        int horizontal = dimen(activity, "line_user_settings_item_container_padding_horizontal");
        section.setPaddingRelative(horizontal, dimen(activity, "line_user_settings_item_header_small_padding_top"),
                horizontal, dimen(activity, "line_user_settings_item_header_padding_bottom"));
        section.setText(title);
        if (LineSettings.materialYouTheme()) {
            section.setTextColor(MaterialYou.accentColor(activity));
        } else {
            look.paint(section, look.sectionOrSecondary());
        }
        return section;
    }

    /** LINE's description row. */
    private static View note(Activity activity, String text) {
        View note = inflate(activity, "line_user_settings_description_item");
        text(activity, note, "setting_description", text);
        return note;
    }

    private static View inflate(Activity activity, String layout) {
        return LayoutInflater.from(activity).inflate(id(activity, layout, "layout"), null, false);
    }

    private static void text(Activity activity, View row, String view, String text) {
        TextView textView = row.findViewById(id(activity, view, "id"));
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
        look.paint(textView, view.equals("setting_title") ? look.title : look.secondary());
    }

    /** The look of the screen, set by {@link #show}. Only one screen is open at a time. */
    private static Look look = new Look();

    /**
     * Colors copied from the LINE Settings page under the dialog. A null
     * field means LINE's page had no such view, and the resource color stays.
     */
    private static final class Look {
        Drawable background;
        ColorStateList title;
        ColorStateList description;
        ColorStateList section;
        ColorStateList headerTitle;

        static Look of(Activity activity) {
            Look look = new Look();
            try {
                // The page is the main Settings list, or the settings search results when the
                // row is opened from search. Each value tries the view of the list, then the view
                // of the search page.
                View page = activity.getWindow().getDecorView();
                look.background = copy(activity, background(activity, page));
                look.title = colors(find(activity, page, "setting_title", "item_title", "empty_title_text"));
                look.description = colors(find(activity, page, "setting_description", "item_path",
                        "empty_description_text"));
                look.section = colors(findByClass(page, "LineUserSettingGroupHeaderItemView"));
                View header = find(page, idOrZero(activity, "header", "id"));
                look.headerTitle = header != null
                        ? colors(findByClass(header, "TextView"))
                        : colors(find(activity, page, "input_text"));
            } catch (Throwable ignored) {
                // Keep what was found. The resources cover the rest.
            }
            return look;
        }

        Drawable background(Activity activity, int fallback) {
            Drawable copy = copy(activity, background);
            return copy != null ? copy : new ColorDrawable(fallback);
        }

        /** Whether the background is light, judged from the text color drawn on it. */
        boolean isLight(int fallback) {
            if (title != null) return Color.luminance(title.getDefaultColor()) < 0.5f;
            return Color.luminance(fallback) > 0.5f;
        }

        /** Descriptions, or section titles when LINE's page shows no description. */
        ColorStateList secondary() {
            return description != null ? description : section;
        }

        /** Section titles, or descriptions when LINE's page shows no section title. */
        ColorStateList sectionOrSecondary() {
            return section != null ? section : description;
        }

        /**
         * Gives [row] a press effect in the text color. LINE's row background has a light pressed
         * state that its theme engine repaints on LINE's own rows, so on these rows it flashes
         * white under a dark theme.
         */
        void press(View row) {
            if (title == null) return;
            int color = (title.getDefaultColor() & 0x00FFFFFF) | 0x1F000000;
            row.setBackground(new android.graphics.drawable.RippleDrawable(
                    ColorStateList.valueOf(color), null, new ColorDrawable(Color.WHITE)));
        }

        void paint(TextView view, ColorStateList colors) {
            if (colors != null) view.setTextColor(colors);
        }

        private static ColorStateList colors(View view) {
            return view instanceof TextView ? ((TextView) view).getTextColors() : null;
        }

        private static Drawable copy(Activity activity, Drawable drawable) {
            Drawable.ConstantState state = drawable == null ? null : drawable.getConstantState();
            return state == null ? null : state.newDrawable(activity.getResources()).mutate();
        }

        /**
         * The background LINE painted on the page: the one of the Settings list, or else the
         * first one under the content view, which is the page of the search results.
         */
        private static Drawable background(Activity activity, View page) {
            View root = find(page, idOrZero(activity, "settings_root", "id"));
            if (root != null && root.getBackground() != null) return root.getBackground();
            View content = page.findViewById(android.R.id.content);
            return content == null ? null : firstBackground(content);
        }

        private static Drawable firstBackground(View view) {
            if (!view.isShown()) return null;
            Drawable background = view.getBackground();
            if (background != null && background.getOpacity() == android.graphics.PixelFormat.OPAQUE) {
                return background;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    Drawable found = firstBackground(group.getChildAt(i));
                    if (found != null) return found;
                }
            }
            return null;
        }

        /** The first shown view with one of the ids [names], tried in order. */
        private static View find(Activity activity, View page, String... names) {
            for (String name : names) {
                View found = find(page, idOrZero(activity, name, "id"));
                if (found != null) return found;
            }
            return null;
        }

        /** The first shown view with the id [id] in the tree of [view]. */
        private static View find(View view, int id) {
            if (id == 0 || !view.isShown()) return null;
            if (view.getId() == id) return view;
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View found = find(group.getChildAt(i), id);
                    if (found != null) return found;
                }
            }
            return null;
        }

        /** The first shown view whose class name ends with [name], such as "TextView". */
        private static View findByClass(View view, String name) {
            if (!view.isShown()) return null;
            if (view.getClass().getName().endsWith(name)
                    && (!(view instanceof TextView) || ((TextView) view).length() > 0)) return view;
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View found = findByClass(group.getChildAt(i), name);
                    if (found != null) return found;
                }
            }
            return null;
        }
    }

    private static void showLicense(Activity activity) {
        TextView text = new TextView(activity);
        text.setText(LICENSE_NOTICE);
        text.setTextIsSelectable(true);
        text.setTextAppearance(id(activity, "line_user_settings_text_item_description_semantic", "style"));
        Linkify.addLinks(text, Linkify.WEB_URLS);
        int padding = dp(activity, 20);
        text.setPadding(padding, padding, padding, padding);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(text);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(t("License", "授權條款"))
                .setView(scroll)
                .setNegativeButton(t("Full license", "完整授權條款"), (d, w) -> open(activity, LICENSE_URL))
                .setPositiveButton(t("Close", "關閉"), null)
                .create();
        PixelNativeEnhancements.applyDialogBlur(dialog);
        dialog.show();
        PixelNativeEnhancements.constrainDialogWidth(dialog, 540.0f);
    }

    private static void open(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable ignored) {
            // No app can open the link.
        }
    }

    /**
     * Starts LINE's launcher Activity as a new task, which creates each screen again. Do not end
     * the process here. Android starts the new task in the old process, and the exit removes it.
     */
    static void restart(Activity activity) {
        Intent launch = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (launch == null || launch.getComponent() == null) return;
        activity.startActivity(Intent.makeRestartActivityTask(launch.getComponent()));
    }

    private static Activity activityOf(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /** The id of LINE's resource [name]. Fails if LINE no longer has it. */
    private static int id(Context context, String name, String type) {
        int id = context.getResources().getIdentifier(name, type, context.getPackageName());
        if (id == 0) throw new IllegalStateException("LINE has no " + type + " " + name);
        return id;
    }

    private static int idOrZero(Context context, String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    private static int dimen(Context context, String name) {
        return context.getResources().getDimensionPixelSize(id(context, name, "dimen"));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
