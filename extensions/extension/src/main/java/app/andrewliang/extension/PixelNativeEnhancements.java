package app.andrewliang.extension;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Android 12-16 and Google Pixel Pro / Fold native API enhancements:
 * <ul>
 *   <li>Ultra HDR & Wide Color Gamut + edge-to-edge cutout mode on media viewers</li>
 *   <li>Android 15+ Adaptive Refresh Rate (ARR) touch-boost on LTPO displays</li>
 *   <li>Foldable / large-screen adaptive centered chat layout</li>
 *   <li>Android 12+ hardware-accelerated cross-window blur for extension dialogs</li>
 *   <li>Linear-motor micro-haptics (API 30+ CONFIRM / API 34+ SEGMENT_FREQUENT_TICK)</li>
 * </ul>
 */
final class PixelNativeEnhancements {

    private static final float FOLD_BREAKPOINT_DP = 620.0f;
    private static final float FOLD_CHAT_MAX_WIDTH_DP = 600.0f;

    private static final Map<View, int[]> ORIG_H_PADDING = new WeakHashMap<>();
    private static Method setFrameRateBoostMethod;
    private static boolean frameRateBoostLookedUp;

    private PixelNativeEnhancements() {}

    static void applyActivity(Activity activity) {
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        String className = activity.getClass().getName();

        if (LineSettings.pixelUltraHdr() && Build.VERSION.SDK_INT >= 26) {
            applyColorModeAndCutout(window, className);
        }
        if (LineSettings.pixelArrTouchBoost() && Build.VERSION.SDK_INT >= 35) {
            applyArrTouchBoost(window);
        }
    }

    private static void applyColorModeAndCutout(Window window, String className) {
        if (isMediaOrChatActivity(className)) {
            try {
                // COLOR_MODE_HDR = 2 enables Ultra HDR & Display P3 on Super Actua / HDR displays
                window.setColorMode(ActivityInfo.COLOR_MODE_HDR);
            } catch (Throwable ignored) {
                try {
                    window.setColorMode(ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT);
                } catch (Throwable ignored2) {
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 30 && isMediaViewerActivity(className)) {
            try {
                WindowManager.LayoutParams lp = window.getAttributes();
                if (lp != null && lp.layoutInDisplayCutoutMode != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS) {
                    lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                    window.setAttributes(lp);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean isMediaOrChatActivity(String name) {
        return name.contains("ChatHistory")
                || name.contains("ChatSkin")
                || isMediaViewerActivity(name);
    }

    private static boolean isMediaViewerActivity(String name) {
        return name.contains("Viewer")
                || name.contains("Media")
                || name.contains("Photo")
                || name.contains("Image")
                || name.contains("Video")
                || name.contains("Gallery")
                || name.contains("Album");
    }

    private static void applyArrTouchBoost(Window window) {
        if (!frameRateBoostLookedUp) {
            frameRateBoostLookedUp = true;
            try {
                setFrameRateBoostMethod = Window.class.getMethod("setFrameRateBoostOnTouchEnabled", boolean.class);
            } catch (Throwable ignored) {
            }
        }
        if (setFrameRateBoostMethod == null) {
            return;
        }
        try {
            boolean enable = !PowerSave.shouldDegradeEffects();
            setFrameRateBoostMethod.invoke(window, enable);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Centers the chat message list and input bar to a comfortable reading width when unfolded on
     * a Pixel Fold or tablet (window width >= 620dp), while keeping the wallpaper and blur full-bleed.
     */
    static void applyFoldableChatLayout(View decorView, View listView, View inputView) {
        if (decorView == null) {
            return;
        }
        Context ctx = decorView.getContext();
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int widthPx = decorView.getWidth() > 0 ? decorView.getWidth() : dm.widthPixels;
        float widthDp = widthPx / dm.density;
        boolean wide = LineSettings.pixelFoldAdaptive() && widthDp >= FOLD_BREAKPOINT_DP;
        int sidePad = wide ? Math.max(0, (widthPx - BlurBars.dp(ctx, FOLD_CHAT_MAX_WIDTH_DP)) / 2) : 0;

        applySymmetricPadding(listView, sidePad, wide);
        if (inputView != null) {
            View inputRow = BlurBars.find(inputView, "chat_ui_input_bar_container");
            applySymmetricPadding(inputRow != null ? inputRow : inputView, sidePad, wide);
        }
    }

    private static void applySymmetricPadding(View view, int sidePad, boolean active) {
        if (view == null) {
            return;
        }
        int[] orig = ORIG_H_PADDING.get(view);
        if (!active) {
            if (orig != null) {
                if (view.getPaddingLeft() != orig[0] || view.getPaddingRight() != orig[1]) {
                    view.setPadding(orig[0], view.getPaddingTop(), orig[1], view.getPaddingBottom());
                }
                ORIG_H_PADDING.remove(view);
            }
            return;
        }
        if (orig == null) {
            orig = new int[]{view.getPaddingLeft(), view.getPaddingRight()};
            ORIG_H_PADDING.put(view, orig);
        }
        int targetLeft = orig[0] + sidePad;
        int targetRight = orig[1] + sidePad;
        if (view.getPaddingLeft() != targetLeft || view.getPaddingRight() != targetRight) {
            view.setPadding(targetLeft, view.getPaddingTop(), targetRight, view.getPaddingBottom());
        }
    }

    /**
     * Enables real-time GPU cross-window blur behind dialogs on Android 12+ (API 31+) when supported.
     */
    static void applyDialogBlur(Dialog dialog) {
        if (dialog == null || Build.VERSION.SDK_INT < 31 || !LineSettings.pixelWindowBlur() || PowerSave.shouldDegradeEffects()) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        try {
            WindowManager wm = (WindowManager) dialog.getContext().getSystemService(Context.WINDOW_SERVICE);
            if (wm == null || !wm.isCrossWindowBlurEnabled()) {
                return;
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            WindowManager.LayoutParams lp = window.getAttributes();
            if (lp != null) {
                lp.setBlurBehindRadius(BlurBars.dp(dialog.getContext(), 20.0f));
                window.setAttributes(lp);
            }
            window.setBackgroundBlurRadius(BlurBars.dp(dialog.getContext(), 16.0f));
        } catch (Throwable ignored) {
        }
    }

    /** Subtle linear-motor tick when picking swatches or toggling switches. */
    static void tick(View view) {
        if (view == null || !LineSettings.chatHaptics()) {
            return;
        }
        int effect = Build.VERSION.SDK_INT >= 34
                ? 27 /* HapticFeedbackConstants.SEGMENT_FREQUENT_TICK */
                : HapticFeedbackConstants.CLOCK_TICK;
        view.performHapticFeedback(effect);
    }

    /** Crisp confirmation pulse when applying settings. */
    static void confirm(View view) {
        if (view == null || !LineSettings.chatHaptics()) {
            return;
        }
        int effect = Build.VERSION.SDK_INT >= 30
                ? HapticFeedbackConstants.CONFIRM
                : HapticFeedbackConstants.KEYBOARD_TAP;
        view.performHapticFeedback(effect);
    }
}
