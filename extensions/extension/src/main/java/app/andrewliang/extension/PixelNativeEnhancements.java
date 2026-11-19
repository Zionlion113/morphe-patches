package app.andrewliang.extension;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Android 12-16, Google Pixel Pro / Fold, and Google Play Adaptive Quality Standards (Tier 1-3):
 * <ul>
 *   <li>Tier 3 (Adaptive ready): Unlocks portrait orientation locks on {@code sw >= 600dp}
 *       foldables, tablets, and desktop windowing so layouts reflow without letterboxing</li>
 *   <li>Tier 2 (Adaptive optimized): Multi-breakpoint &amp; aspect-ratio-aware widths
 *       ({@code <600dp} compact, {@code 1:1 / 4:3} unfolded foldable {@code 640dp},
 *       {@code 16:9} wide {@code 680dp}, {@code >=840dp} expanded {@code 720dp}), bounded dialog
 *       widths, and mouse / trackpad / stylus hover &amp; keyboard focus states</li>
 *   <li>Tier 1 (Adaptive differentiated): Camera cutout &amp; foldable hinge safe-inset awareness,
 *       Ultra HDR / Display P3, Android 15+ LTPO ARR touch-boost, and GPU cross-window blur</li>
 * </ul>
 */
final class PixelNativeEnhancements {

    private static final float COMPACT_MAX_DP = 600.0f;
    private static final float EXPANDED_MIN_DP = 840.0f;
    private static final float FOLD_SQUARE_MAX_WIDTH_DP = 640.0f;
    private static final float MEDIUM_WIDE_MAX_WIDTH_DP = 680.0f;
    private static final float EXPANDED_MAX_WIDTH_DP = 720.0f;

    private static final Map<View, int[]> ORIG_H_PADDING = new WeakHashMap<>();
    private static final Map<View, Boolean> INTERACTIVE_ENHANCED = new WeakHashMap<>();
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

        if (LineSettings.pixelFoldAdaptive()) {
            unlockLargeScreenOrientation(activity, className);
        }
        if (LineSettings.pixelUltraHdr() && Build.VERSION.SDK_INT >= 26) {
            applyColorModeAndCutout(window, className);
        }
        if (LineSettings.pixelArrTouchBoost() && Build.VERSION.SDK_INT >= 35) {
            applyArrTouchBoost(window);
        }
    }

    /**
     * Google Play Adaptive Quality Tier 3: removes forced portrait orientation locks on
     * {@code smallestScreenWidthDp >= 600} foldables, tablets, and desktop windowing, while
     * leaving dedicated camera / QR scanner capture screens untouched.
     */
    private static void unlockLargeScreenOrientation(Activity activity, String className) {
        if (isCameraCaptureActivity(className)) {
            return;
        }
        try {
            Configuration cfg = activity.getResources().getConfiguration();
            if (cfg == null || cfg.smallestScreenWidthDp < 600) {
                return;
            }
            int req = activity.getRequestedOrientation();
            if (req == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    || req == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    || req == ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    || req == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isCameraCaptureActivity(String name) {
        return name.contains("Camera")
                || name.contains("Scanner")
                || name.contains("QrCode")
                || name.contains("Barcode");
    }

    private static void applyColorModeAndCutout(Window window, String className) {
        if (isMediaViewerActivity(className)) {
            try {
                // COLOR_MODE_HDR = 2 enables Ultra HDR & Display P3 on full-screen media viewers
                window.setColorMode(ActivityInfo.COLOR_MODE_HDR);
            } catch (Throwable ignored) {
                try {
                    window.setColorMode(ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT);
                } catch (Throwable ignored2) {
                }
            }
        } else if (isChatActivity(className)) {
            try {
                window.setColorMode(ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT);
            } catch (Throwable ignored) {
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

    private static boolean isChatActivity(String name) {
        return name.contains("ChatHistory")
                || name.contains("ChatSkin");
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
     * Google Play Adaptive Quality Tier 1 &amp; Tier 2: centers the chat message list and input bar
     * across 9:16, 10:16, 16:9, 1:1, and 4:3 aspect ratios while respecting camera cutouts and
     * foldable hinge safe insets and keeping the wallpaper and blur full-bleed.
     */
    static void applyFoldableChatLayout(View decorView, View listView, View inputView) {
        if (decorView == null) {
            return;
        }
        Context ctx = decorView.getContext();
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int widthPx = decorView.getWidth() > 0 ? decorView.getWidth() : dm.widthPixels;
        int heightPx = decorView.getHeight() > 0 ? decorView.getHeight() : dm.heightPixels;
        float widthDp = widthPx / dm.density;

        boolean adaptiveOn = LineSettings.pixelFoldAdaptive();
        int cutoutLeft = 0;
        int cutoutRight = 0;
        if (adaptiveOn && Build.VERSION.SDK_INT >= 28) {
            int[] cutouts = safeHorizontalCutoutInsets(decorView);
            cutoutLeft = cutouts[0];
            cutoutRight = cutouts[1];
        }

        int centeredPad = 0;
        boolean wide = adaptiveOn && widthDp >= COMPACT_MAX_DP;
        if (wide) {
            float aspect = heightPx > 0
                    ? Math.max((float) widthPx / heightPx, (float) heightPx / Math.max(1, widthPx))
                    : 1.0f;
            float maxContentDp;
            if (widthDp >= EXPANDED_MIN_DP) {
                maxContentDp = EXPANDED_MAX_WIDTH_DP;
            } else if (aspect <= 1.45f) {
                // 1:1 and 4:3 unfolded inner foldable displays
                maxContentDp = FOLD_SQUARE_MAX_WIDTH_DP;
            } else {
                // 16:9 / 10:16 landscape windows
                maxContentDp = MEDIUM_WIDE_MAX_WIDTH_DP;
            }
            centeredPad = Math.max(0, (widthPx - BlurBars.dp(ctx, maxContentDp)) / 2);
        }

        int leftPad = Math.max(centeredPad, cutoutLeft);
        int rightPad = Math.max(centeredPad, cutoutRight);
        boolean active = adaptiveOn && (leftPad > 0 || rightPad > 0);

        applyHorizontalPadding(listView, leftPad, rightPad, active);
        if (inputView != null) {
            View inputRow = BlurBars.find(inputView, "chat_ui_input_bar_container");
            applyHorizontalPadding(inputRow != null ? inputRow : inputView, leftPad, rightPad, active);
        }
    }

    static int[] safeHorizontalCutoutInsets(View rootView) {
        int[] out = new int[]{0, 0};
        if (rootView == null || Build.VERSION.SDK_INT < 28) {
            return out;
        }
        try {
            WindowInsets insets = rootView.getRootWindowInsets();
            if (insets != null) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    out[0] = Math.max(0, cutout.getSafeInsetLeft());
                    out[1] = Math.max(0, cutout.getSafeInsetRight());
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    /**
     * Computes adaptive horizontal padding for full-screen settings lists so rows stay bounded to
     * {@code maxDp} on foldables, tablets, and desktop windows while respecting cutout safe insets.
     */
    static int computeAdaptiveSidePadding(Context ctx, int containerWidthPx, float maxDp) {
        if (ctx == null || !LineSettings.pixelFoldAdaptive()) {
            return 0;
        }
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int w = containerWidthPx > 0 ? containerWidthPx : dm.widthPixels;
        float wDp = w / dm.density;
        if (wDp < COMPACT_MAX_DP) {
            return 0;
        }
        return Math.max(0, (w - BlurBars.dp(ctx, maxDp)) / 2);
    }

    private static void applyHorizontalPadding(View view, int leftPad, int rightPad, boolean active) {
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
        int targetLeft = orig[0] + leftPad;
        int targetRight = orig[1] + rightPad;
        if (view.getPaddingLeft() != targetLeft || view.getPaddingRight() != targetRight) {
            view.setPadding(targetLeft, view.getPaddingTop(), targetRight, view.getPaddingBottom());
        }
    }

    /**
     * Bounds modal dialogs to a comfortable max width on unfolded foldables, tablets, and desktop
     * windowing (called after {@code dialog.show()}).
     */
    static void constrainDialogWidth(Dialog dialog, float maxWidthDp) {
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        try {
            Context ctx = dialog.getContext();
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            float widthDp = dm.widthPixels / dm.density;
            if (widthDp < COMPACT_MAX_DP) {
                return;
            }
            int targetWidth = Math.min(Math.round(dm.widthPixels * 0.90f), BlurBars.dp(ctx, maxWidthDp));
            window.setLayout(targetWidth, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Google Play Adaptive Quality Tier 2: adds external mouse / trackpad / stylus pointer cursor,
     * subtle hover feedback, and keyboard focus navigation to custom interactive controls.
     */
    static void enhanceInteractive(View view) {
        if (view == null || INTERACTIVE_ENHANCED.containsKey(view)) {
            return;
        }
        INTERACTIVE_ENHANCED.put(view, Boolean.TRUE);
        view.setFocusable(true);
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                view.setPointerIcon(PointerIcon.getSystemIcon(view.getContext(), PointerIcon.TYPE_HAND));
            } catch (Throwable ignored) {
            }
        }
        view.setOnHoverListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_HOVER_ENTER) {
                v.animate().alpha(0.86f).setDuration(90L).start();
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                v.animate().alpha(1.0f).setDuration(120L).start();
            }
            return false;
        });
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
