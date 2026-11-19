package app.andrewliang.extension;

import android.graphics.Color;
import android.view.View;

final class ChromeStyle {
    static final int BOT_DAY_PERF = 172;
    static final int BOT_NIGHT_PERF = 176;
    static final int BOT_SAVER = 208;
    static final int TOP_DAY_PERF = 172;
    static final int TOP_NIGHT_PERF = 160;
    static final int TOP_SAVER = 192;
    boolean bottomDarkIcons;
    boolean topDarkIcons;
    int topRgb = -16777216;
    int bottomRgb = -16777216;
    int topAlpha = TOP_SAVER;
    int bottomAlpha = BOT_SAVER;

    ChromeStyle() {
    }

    boolean update(View view, SkinPalette skinPalette) {
        int newTopRgb;
        int newBottomRgb;
        boolean newTopDarkIcons;
        boolean newBottomDarkIcons;
        boolean zPowerSave = BlurBars.powerSave();
        int newBottomAlpha = BOT_NIGHT_PERF;
        int newTopAlpha = TOP_NIGHT_PERF;
        if (skinPalette != null && skinPalette.valid && LineSettings.blurAutoColor()) {
            newTopRgb = skinPalette.top;
            newBottomRgb = skinPalette.bottom;
            newTopDarkIcons = prefersDarkIcons(newTopRgb);
            newBottomDarkIcons = prefersDarkIcons(newBottomRgb);
            if (zPowerSave) {
                newTopAlpha = TOP_SAVER;
            } else if (newTopDarkIcons) {
                newTopAlpha = TOP_DAY_PERF;
            }
            if (zPowerSave) {
                newBottomAlpha = BOT_SAVER;
            } else if (newBottomDarkIcons) {
                newBottomAlpha = BOT_DAY_PERF;
            }
        } else if (zPowerSave) {
            newTopRgb = -16777216;
            newBottomRgb = -16777216;
            newTopAlpha = TOP_SAVER;
            newBottomAlpha = BOT_SAVER;
            newTopDarkIcons = false;
            newBottomDarkIcons = false;
        } else if (!BlurBars.chromeNight(view)) {
            newTopRgb = -1;
            newBottomRgb = -1;
            newTopAlpha = TOP_DAY_PERF;
            newBottomAlpha = BOT_DAY_PERF;
            newTopDarkIcons = true;
            newBottomDarkIcons = true;
        } else {
            newTopRgb = -16777216;
            newBottomRgb = -16777216;
            newTopDarkIcons = false;
            newBottomDarkIcons = false;
        }
        boolean changed = newTopRgb != this.topRgb
            || newBottomRgb != this.bottomRgb
            || newTopAlpha != this.topAlpha
            || newBottomAlpha != this.bottomAlpha
            || newTopDarkIcons != this.topDarkIcons
            || newBottomDarkIcons != this.bottomDarkIcons;
        this.topRgb = newTopRgb;
        this.bottomRgb = newBottomRgb;
        this.topAlpha = newTopAlpha;
        this.bottomAlpha = newBottomAlpha;
        this.topDarkIcons = newTopDarkIcons;
        this.bottomDarkIcons = newBottomDarkIcons;
        return changed;
    }

    static boolean prefersDarkIcons(int i) {
        float fLuminance = Color.luminance(i | (-16777216)) + 0.05f;
        return fLuminance / 0.05f > 1.05f / fLuminance;
    }
}
