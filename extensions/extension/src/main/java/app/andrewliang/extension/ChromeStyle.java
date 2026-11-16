package app.andrewliang.extension;

import android.graphics.Color;
import android.view.View;

/**
 * The colors of one chat's top and bottom chrome: the tint over the blur (or the plain fade in
 * Battery Saver), its alpha at the screen edge, and whether the icons on it are dark or white.
 *
 * <p>Worked out before each frame from a few cached values (no allocation):
 * <ul>
 *   <li>Auto color on and the chat has a background image: the top and bottom tints are the
 *       average colors of the image there ({@link SkinPalette}), and the icons pick black or
 *       white by contrast.</li>
 *   <li>Otherwise Battery Saver: a black fade with white icons, day and night.</li>
 *   <li>Otherwise performance: a white frost with dark icons by day, a black frost with white
 *       icons at night. Day or night follows the system, or the "Blur day/night" setting.</li>
 * </ul>
 */
final class ChromeStyle {

    /** Battery Saver: a black fade with no blur, strong enough for white icons. */
    static final int TOP_SAVER = 0xC0;
    static final int BOT_SAVER = 0xD0;
    /** Day performance: a white frost over the blur. */
    static final int TOP_DAY_PERF = 0xB8;
    static final int BOT_DAY_PERF = 0xC8;
    /** Night performance: a black frost over the blur. */
    static final int TOP_NIGHT_PERF = 0xA0;
    static final int BOT_NIGHT_PERF = 0xB0;

    int topRgb = Color.BLACK;
    int bottomRgb = Color.BLACK;
    int topAlpha = TOP_SAVER;
    int bottomAlpha = BOT_SAVER;
    /** Dark (#111111) icons on the top or the bottom chrome; white when false. */
    boolean topDarkIcons;
    boolean bottomDarkIcons;

    /** Works the style out again. True when anything changed, so the chrome must be redrawn. */
    boolean update(View view, SkinPalette palette) {
        boolean saver = BlurBars.powerSave();
        int tRgb, bRgb, tAlpha, bAlpha;
        boolean tDark, bDark;
        if (palette != null && palette.valid && LineSettings.blurAutoColor()) {
            tRgb = palette.top;
            bRgb = palette.bottom;
            tDark = prefersDarkIcons(tRgb);
            bDark = prefersDarkIcons(bRgb);
            tAlpha = saver ? TOP_SAVER : (tDark ? TOP_DAY_PERF : TOP_NIGHT_PERF);
            bAlpha = saver ? BOT_SAVER : (bDark ? BOT_DAY_PERF : BOT_NIGHT_PERF);
        } else if (saver) {
            tRgb = bRgb = Color.BLACK;
            tAlpha = TOP_SAVER;
            bAlpha = BOT_SAVER;
            tDark = bDark = false;
        } else if (BlurBars.chromeNight(view)) {
            tRgb = bRgb = Color.BLACK;
            tAlpha = TOP_NIGHT_PERF;
            bAlpha = BOT_NIGHT_PERF;
            tDark = bDark = false;
        } else {
            tRgb = bRgb = Color.WHITE;
            tAlpha = TOP_DAY_PERF;
            bAlpha = BOT_DAY_PERF;
            tDark = bDark = true;
        }
        boolean changed = tRgb != topRgb || bRgb != bottomRgb || tAlpha != topAlpha || bAlpha != bottomAlpha
                || tDark != topDarkIcons || bDark != bottomDarkIcons;
        topRgb = tRgb;
        bottomRgb = bRgb;
        topAlpha = tAlpha;
        bottomAlpha = bAlpha;
        topDarkIcons = tDark;
        bottomDarkIcons = bDark;
        return changed;
    }

    /**
     * True when black text has more contrast on [rgb] than white (WCAG contrast ratio). The
     * chrome near the edge is mostly the tint, so the tint color stands for what is under the
     * icons.
     */
    static boolean prefersDarkIcons(int rgb) {
        float l = Color.luminance(rgb | 0xFF000000);
        return (l + 0.05f) / 0.05f > 1.05f / (l + 0.05f);
    }
}
