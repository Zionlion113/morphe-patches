package app.andrewliang.extension;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Frost for LINE's in-chat sticker, emoji, gallery and "+" panels. The system IME is left alone.
 *
 * <p>Performance mode: a semi-transparent fill (white-ish by day, dark at night) with a native
 * backdrop blur of the chat behind the panel. Battery Saver: a solid panel color with a short
 * fade from transparent at the top edge, so the panel meets the chat without a hard seam.
 */
final class ChatPanels {

    private static final String CONTAINER = "chat_ui_keyboard_container";
    private static final String[] PLACEHOLDERS = {
            "chat_ui_keyboard_placeholder",
            "chat_ui_custom_input_placeholder",
    };
    private static final float BLUR_RADIUS_DP = 20f;
    private static final float SAVER_FADE_DP = 20f;
    private static final int DAY_PERF_FILL = 0xCCF5F5F5;
    private static final int NIGHT_PERF_FILL = 0xCC1A1A1A;
    private static final int DAY_SAVER_FILL = 0xFFF5F5F5;
    private static final int NIGHT_SAVER_FILL = 0xFF1A1A1A;

    private static final Map<View, State> STATES = new WeakHashMap<>();

    private ChatPanels() {}

    static void apply(View decor, View skin, View content) {
        View container = find(decor, CONTAINER);
        if (container == null) return;
        View panel = visiblePanel(container);
        State state = STATES.get(container);
        if (panel == null) {
            if (state != null && state.applied) restore(container, state);
            return;
        }
        if (state == null) {
            state = new State();
            STATES.put(container, state);
        }
        if (!state.applied) {
            clearSolidBackgrounds(panel, state);
            state.applied = true;
        } else {
            // LINE can re-apply a solid color when the panel is shown again.
            clearSolidBackgrounds(panel, state);
        }
        FrostDrawable frost;
        boolean fresh = false;
        if (container.getBackground() instanceof FrostDrawable) {
            frost = (FrostDrawable) container.getBackground();
        } else {
            frost = new FrostDrawable(container);
            container.setBackground(frost);
            fresh = true;
        }
        if (frost.sources == null || frost.sources.length != 2 || frost.sources[0] != skin
                || frost.sources[1] != content) {
            frost.sources = new View[] {skin, content};
            fresh = true;
        }
        // Redraw the frost only when what is behind it changed, not before every frame, so an
        // open panel does not keep the screen drawing.
        boolean behindChanged = (skin != null && skin.isDirty()) || (content != null && content.isDirty());
        if (fresh || behindChanged) container.invalidate();
    }

    /** A child of the keyboard container that is a real panel, not the IME height placeholder. */
    static View visiblePanel(View container) {
        if (!(container instanceof ViewGroup) || !container.isShown() || container.getHeight() <= 0) {
            return null;
        }
        ViewGroup group = (ViewGroup) container;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child == null || !child.isShown() || child.getHeight() <= 0) continue;
            if (isPlaceholder(child)) continue;
            return child;
        }
        return null;
    }

    /** Top of the covering bottom chrome in window coordinates: the input bar, or the panel. */
    static int chromeTop(View input, View decor) {
        int[] loc = new int[2];
        int top = Integer.MAX_VALUE;
        if (input != null && input.isShown() && input.getHeight() > 0) {
            input.getLocationInWindow(loc);
            top = loc[1];
        }
        View container = decor == null ? null : find(decor, CONTAINER);
        // A panel or IME placeholder below the input still covers the list. Include it when
        // computing how far the list extends, but chromeTop for padding is the input top so
        // the last message sits above the input (and the panel/IME is included in the padding
        // as input-top to list-bottom).
        return top == Integer.MAX_VALUE ? 0 : top;
    }

    /** The bottom of the input, an open panel or IME placeholder, and the gesture spacer. */
    static int coverBottom(View input, View spacer, View decor) {
        int bottom = 0;
        int[] loc = new int[2];
        if (input != null && input.isShown() && input.getHeight() > 0) {
            input.getLocationInWindow(loc);
            bottom = Math.max(bottom, loc[1] + input.getHeight());
        }
        if (spacer != null && spacer.isShown() && spacer.getHeight() > 0) {
            spacer.getLocationInWindow(loc);
            bottom = Math.max(bottom, loc[1] + spacer.getHeight());
        }
        View container = decor == null ? null : find(decor, CONTAINER);
        if (container != null && container.isShown() && container.getHeight() > 0) {
            container.getLocationInWindow(loc);
            bottom = Math.max(bottom, loc[1] + container.getHeight());
        }
        return bottom;
    }

    private static boolean isPlaceholder(View view) {
        int id = view.getId();
        if (id == 0) return false;
        for (String name : PLACEHOLDERS) {
            if (id == BlurBars.id(view.getContext(), name)) return true;
        }
        return false;
    }

    private static void clearSolidBackgrounds(View view, State state) {
        if (view == null) return;
        Drawable background = view.getBackground();
        if (background instanceof ColorDrawable && !(background instanceof FrostDrawable)) {
            if (!state.backgrounds.containsKey(view)) state.backgrounds.put(view, background);
            view.setBackground(null);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = Math.min(group.getChildCount(), 16);
            for (int i = 0; i < count; i++) clearSolidBackgrounds(group.getChildAt(i), state);
        }
    }

    private static void restore(View container, State state) {
        if (container.getBackground() instanceof FrostDrawable) container.setBackground(null);
        for (Map.Entry<View, Drawable> entry : state.backgrounds.entrySet()) {
            View view = entry.getKey();
            if (view != null) view.setBackground(entry.getValue());
        }
        state.backgrounds.clear();
        state.applied = false;
    }

    private static View find(View root, String name) {
        if (root == null) return null;
        int id = BlurBars.id(root.getContext(), name);
        return id == 0 ? null : root.findViewById(id);
    }

    private static final class State {
        final Map<View, Drawable> backgrounds = new WeakHashMap<>();
        boolean applied;
    }

    private static final class FrostDrawable extends Drawable {
        private final View host;
        View[] sources;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] hostLocation = new int[2];
        private final int[] viewLocation = new int[2];
        private Object gpu;
        private int lastKey = -1;
        private int lastHeight;

        FrostDrawable(View host) {
            this.host = host;
        }

        @Override
        public void draw(Canvas canvas) {
            if (ProgressiveBlurDrawable.recording) return;
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;
            boolean saver = BlurBars.powerSave();
            boolean night = BlurBars.night(host);
            if (!saver && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
                if (gpu == null) gpu = new Gpu();
                ((Gpu) gpu).draw(this, canvas, bounds);
            }
            int color;
            if (saver) {
                color = night ? NIGHT_SAVER_FILL : DAY_SAVER_FILL;
                int fade = BlurBars.dp(host.getContext(), SAVER_FADE_DP);
                int key = color | (fade << 4);
                if (lastKey != key || lastHeight != bounds.height()) {
                    lastKey = key;
                    lastHeight = bounds.height();
                    if (fade >= bounds.height() || fade <= 0) {
                        fill.setShader(null);
                        fill.setColor(color);
                    } else {
                        // Opaque paint alpha: a shader is multiplied by it, so 0 hid the panel.
                        fill.setColor(Color.BLACK);
                        int clear = color & 0x00FFFFFF;
                        float until = fade / (float) bounds.height();
                        fill.setShader(new LinearGradient(0, bounds.top, 0, bounds.top + fade,
                                new int[] {clear, color}, new float[] {0f, 1f}, Shader.TileMode.CLAMP));
                    }
                }
            } else {
                color = night ? NIGHT_PERF_FILL : DAY_PERF_FILL;
                fill.setShader(null);
                fill.setColor(color);
            }
            canvas.drawRect(bounds, fill);
        }

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter colorFilter) {}

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class Gpu {
        private final RenderNode node = new RenderNode("andrewChatPanel");
        private RenderEffect effect;
        private int effectWidth;
        private int effectHeight;

        void draw(FrostDrawable drawable, Canvas canvas, Rect bounds) {
            int width = bounds.width();
            int height = bounds.height();
            if (width <= 0 || height <= 0 || drawable.sources == null) return;
            node.setPosition(0, 0, width, height);
            RecordingCanvas recorded = node.beginRecording(width, height);
            ProgressiveBlurDrawable.recording = true;
            try {
                drawable.host.getLocationInWindow(drawable.hostLocation);
                int left = drawable.hostLocation[0] + bounds.left;
                int top = drawable.hostLocation[1] + bounds.top;
                for (View source : drawable.sources) record(recorded, source, left, top, drawable.viewLocation);
            } finally {
                ProgressiveBlurDrawable.recording = false;
                node.endRecording();
            }
            if (effect == null || width != effectWidth || height != effectHeight) {
                float radius = BlurBars.dp(drawable.host.getContext(), BLUR_RADIUS_DP);
                effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP);
                effectWidth = width;
                effectHeight = height;
            }
            node.setRenderEffect(effect);
            int save = canvas.save();
            canvas.translate(bounds.left, bounds.top);
            canvas.drawRenderNode(node);
            canvas.restoreToCount(save);
        }

        private void record(Canvas canvas, View view, int left, int top, int[] viewLocation) {
            if (view == null || view.getVisibility() != View.VISIBLE || !view.isAttachedToWindow()) {
                return;
            }
            view.getLocationInWindow(viewLocation);
            int save = canvas.save();
            canvas.translate(viewLocation[0] - left, viewLocation[1] - top);
            view.draw(canvas);
            canvas.restoreToCount(save);
        }
    }
}
