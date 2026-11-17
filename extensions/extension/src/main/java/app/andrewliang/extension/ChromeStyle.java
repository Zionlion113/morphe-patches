package app.andrewliang.extension;

import android.graphics.Color;
import android.view.View;

final class ChromeStyle {
    static final int BOT_DAY_PERF = 200;
    static final int BOT_NIGHT_PERF = 176;
    static final int BOT_SAVER = 208;
    static final int TOP_DAY_PERF = 184;
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
        int i;
        boolean zPrefersDarkIcons;
        boolean zPrefersDarkIcons2;
        boolean zPowerSave = BlurBars.powerSave();
        int i2 = BOT_NIGHT_PERF;
        int i3 = TOP_NIGHT_PERF;
        boolean z = true;
        if (skinPalette != null && skinPalette.valid && LineSettings.blurAutoColor()) {
            i = skinPalette.top;
            i = skinPalette.bottom;
            zPrefersDarkIcons = prefersDarkIcons(i);
            zPrefersDarkIcons2 = prefersDarkIcons(i);
            if (zPowerSave) {
                i3 = TOP_SAVER;
            } else if (zPrefersDarkIcons) {
                i3 = TOP_DAY_PERF;
            }
            if (zPowerSave) {
                i2 = BOT_SAVER;
            } else if (zPrefersDarkIcons2) {
                i2 = BOT_DAY_PERF;
            }
        } else {
            i = -16777216;
            if (zPowerSave) {
                i2 = BOT_SAVER;
                i3 = TOP_SAVER;
            } else if (!BlurBars.chromeNight(view)) {
                i = -1;
                i = -1;
                zPrefersDarkIcons = true;
                zPrefersDarkIcons2 = true;
                i2 = BOT_DAY_PERF;
                i3 = TOP_DAY_PERF;
            }
            zPrefersDarkIcons = false;
            zPrefersDarkIcons2 = false;
        }
        if (i == this.topRgb && i == this.bottomRgb && i3 == this.topAlpha && i2 == this.bottomAlpha && zPrefersDarkIcons == this.topDarkIcons && zPrefersDarkIcons2 == this.bottomDarkIcons) {
            z = false;
        }
        this.topRgb = i;
        this.bottomRgb = i;
        this.topAlpha = i3;
        this.bottomAlpha = i2;
        this.topDarkIcons = zPrefersDarkIcons;
        this.bottomDarkIcons = zPrefersDarkIcons2;
        return z;
    }

    static boolean prefersDarkIcons(int i) {
        float fLuminance = Color.luminance(i | (-16777216)) + 0.05f;
        return fLuminance / 0.05f > 1.05f / fLuminance;
    }
}
