package app.andrewliang.extension;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The bottom tab bar of LINE's main screen (Home, Chats, Wallet...), with the same four-state
 * chrome as a chat: a progressive blur under a white frost by day, a black frost at night, and a
 * plain black fade with no blur in Battery Saver. It reaches through the gesture bar. The pages
 * go on behind it: the page area is stretched to the bottom of the window, and each page gets
 * the same amount of bottom padding (on its list, with clipToPadding off, so the list scrolls
 * under the bar; or on the page itself when it is a Compose page), so nothing ends up hidden.
 */
final class HomeBar {

    private static final String TABS = "main_tab_container";
    private static final String NAV = "main_navigation_bar_background";
    private static final String PAGES = "viewpager";
    private static final String TABS_ARTWORK = "bnb_background_image";
    private static final String TAB_ICON = "bnb_button_image";
    private static final String TAB_TEXT = "bnb_button_text";
    private static final int ICON_LIGHT = 0xFFFFFFFF;

    private static final Map<View, HomeBar> BARS = new WeakHashMap<>();

    final View decor;
    final View tabs;
    final View nav;
    final View pages;
    final ChromeStyle style = new ChromeStyle();
    final ProgressiveBlurDrawable.Region region;
    final ProgressiveBlurDrawable tabsBlur;
    final ProgressiveBlurDrawable navBlur;
    /** How far the pages were stretched down, and the padding added for it, by view. */
    private int extended;
    private int setMargin = Integer.MIN_VALUE;
    private final Map<View, Integer> padded = new WeakHashMap<>();
    /** LINE's own tab text colors, to put back on a light frost. */
    private final Map<TextView, ColorStateList> textColors = new WeakHashMap<>();
    private final Map<ImageView, Boolean> tinted = new WeakHashMap<>();

    private HomeBar(View decor, View tabs, View nav, View pages) {
        this.decor = decor;
        this.tabs = tabs;
        this.nav = nav;
        this.pages = pages;
        region = new ProgressiveBlurDrawable.Region(true, new View[] {tabs, nav}, new View[] {pages});
        region.fadeAbove = false;
        region.style = style;
        tabsBlur = new ProgressiveBlurDrawable(tabs, region);
        navBlur = nav == null ? null : new ProgressiveBlurDrawable(nav, region);
    }

    /** True when [decor] is LINE's main screen; it then owns the gesture bar strip. */
    static boolean isMain(View decor) {
        return find(decor, TABS) != null && find(decor, PAGES) != null;
    }

    /** After a layout: set the bar up, and stretch the pages again if LINE moved them. */
    static void scan(View decor) {
        View tabs = find(decor, TABS);
        View pages = find(decor, PAGES);
        if (tabs == null || pages == null || !tabs.isAttachedToWindow()) {
            BARS.remove(decor);
            return;
        }
        HomeBar bar = BARS.get(decor);
        if (bar == null || bar.tabs != tabs || bar.pages != pages) {
            bar = new HomeBar(decor, tabs, find(decor, NAV), pages);
            BARS.put(decor, bar);
        }
        bar.apply();
        bar.extendPages();
    }

    static void onPreDraw(View decor) {
        HomeBar bar = BARS.get(decor);
        if (bar == null) return;
        if (bar.style.update(bar.tabs, null)) bar.region.invalidate();
        bar.apply();
        // Only the blur depends on what is behind; Battery Saver's plain fade does not.
        if (!BlurBars.powerSave() && bar.pages.isDirty()) bar.region.invalidate();
    }

    private void apply() {
        if (tabs.getBackground() != tabsBlur) tabs.setBackground(tabsBlur);
        View artwork = find(tabs, TABS_ARTWORK);
        if (artwork != null) BlurBars.hide(artwork);
        if (nav != null) {
            if (nav.getVisibility() == View.INVISIBLE) nav.setVisibility(View.VISIBLE);
            if (nav.getBackground() != navBlur) nav.setBackground(navBlur);
        }
        tintTabs(tabs);
        navHandle();
    }

    /** A dark gesture handle on the white frost, a light one on the black. */
    private void navHandle() {
        if (Build.VERSION.SDK_INT < 30) return;
        WindowInsetsController controller = decor.getWindowInsetsController();
        if (controller == null) return;
        int mask = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        int value = style.bottomDarkIcons ? mask : 0;
        if ((controller.getSystemBarsAppearance() & mask) != value) controller.setSystemBarsAppearance(value, mask);
    }

    /**
     * White tab icons and labels on the black frost. On the white frost LINE's own colors are
     * kept (they already suit a light bar, and keep the selected tab distinct). Theme icons that
     * are pictures (bitmaps) are never tinted.
     */
    private void tintTabs(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) tintTabs(group.getChildAt(i));
            return;
        }
        int id = view.getId();
        if (id == 0) return;
        boolean white = !style.bottomDarkIcons;
        if (id == BlurBars.id(view.getContext(), TAB_ICON) && view instanceof ImageView) {
            ImageView image = (ImageView) view;
            Drawable drawable = image.getDrawable();
            boolean picture = drawable instanceof BitmapDrawable;
            if (white && !picture) {
                ColorStateList current = image.getImageTintList();
                if (!Boolean.TRUE.equals(tinted.get(image)) || current == null
                        || current.getDefaultColor() != ICON_LIGHT || image.getColorFilter() == null) {
                    image.setImageTintList(ColorStateList.valueOf(ICON_LIGHT));
                    image.setColorFilter(ICON_LIGHT, PorterDuff.Mode.SRC_IN);
                    tinted.put(image, Boolean.TRUE);
                }
            } else if (tinted.remove(image) != null) {
                image.setImageTintList(null);
                image.clearColorFilter();
            }
        } else if (id == BlurBars.id(view.getContext(), TAB_TEXT) && view instanceof TextView) {
            TextView text = (TextView) view;
            if (white) {
                if (!textColors.containsKey(text)) textColors.put(text, text.getTextColors());
                if (text.getCurrentTextColor() != ICON_LIGHT) text.setTextColor(ICON_LIGHT);
            } else {
                ColorStateList original = textColors.remove(text);
                if (original != null) text.setTextColor(original);
            }
        }
    }

    /**
     * Stretches the page area to the bottom of the window, and pads each page by the same
     * amount, so every page still ends where LINE put it, but draws on behind the bar.
     */
    private void extendPages() {
        if (!(pages.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) pages.getLayoutParams();
        // LINE's own margin: the one it set, or ours plus what we took away.
        int base = params.bottomMargin == setMargin ? params.bottomMargin + extended : params.bottomMargin;
        int wanted = Math.max(base, 0);
        if (params.bottomMargin != 0) {
            params.bottomMargin = 0;
            pages.setLayoutParams(params);
        }
        setMargin = 0;
        extended = wanted;
        if (pages instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) pages;
            group.setClipToPadding(false);
            for (int i = 0; i < group.getChildCount(); i++) padPage(group.getChildAt(i), wanted);
        }
    }

    private void padPage(View page, int amount) {
        View list = largestList(page, page.getHeight() / 2);
        View target = list != null ? list : page;
        // Remove the padding added to the other view, if the list appeared or went away.
        if (target != page) setAdded(page, 0);
        setAdded(target, amount);
        if (target == list && list instanceof ViewGroup) {
            ((ViewGroup) list).setClipToPadding(false);
            unclipUpTo(list, page);
        }
    }

    /** Sets the padding this class adds to [view] to [amount], keeping the rest of it. */
    private void setAdded(View view, int amount) {
        Integer before = padded.get(view);
        int old = before == null ? 0 : before;
        if (old == amount) return;
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                Math.max(0, view.getPaddingBottom() - old + amount));
        if (amount == 0) padded.remove(view);
        else padded.put(view, amount);
    }

    private static void unclipUpTo(View view, View stop) {
        View current = view.getParent() instanceof View ? (View) view.getParent() : null;
        while (current instanceof ViewGroup) {
            ((ViewGroup) current).setClipToPadding(false);
            ((ViewGroup) current).setClipChildren(false);
            if (current == stop || !(current.getParent() instanceof View)) break;
            current = (View) current.getParent();
        }
    }

    /** The tallest vertical list (RecyclerView, ScrollView, ListView) at least [minHeight] tall. */
    private static View largestList(View view, int minHeight) {
        View best = null;
        if (view instanceof ViewGroup && view.getVisibility() == View.VISIBLE) {
            String name = view.getClass().getName();
            boolean list = name.endsWith("RecyclerView") || view instanceof android.widget.ScrollView
                    || view instanceof android.widget.AbsListView || name.endsWith("NestedScrollView");
            if (list && view.getHeight() >= minHeight && view.getHeight() > 0) return view;
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = largestList(group.getChildAt(i), minHeight);
                if (found != null && (best == null || found.getHeight() > best.getHeight())) best = found;
            }
        }
        return best;
    }

    private static View find(View root, String name) {
        int id = BlurBars.id(root.getContext(), name);
        return id == 0 ? null : root.findViewById(id);
    }
}
