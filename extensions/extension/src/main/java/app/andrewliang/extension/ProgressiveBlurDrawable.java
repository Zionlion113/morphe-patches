package app.andrewliang.extension;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

/**
 * One piece of a blurred bar: the background of one view in a {@link Region}. A region is the
 * area behind a screen edge, such as the chat header at the top, or the chat input and the gesture
 * bar at the bottom. The content behind the region is blurred most at the screen edge and clear at
 * the inner side, with a tint of the same shape.
 *
 * <ul>
 *   <li>Android 13 and later: the content is recorded into a {@link RenderNode}, and an AGSL
 *       {@link RuntimeShader} blurs it with a radius that falls from the edge to zero. It runs as
 *       two passes, across and along, chained with {@link RenderEffect}.</li>
 *   <li>Android 12: the same node with a fixed {@link RenderEffect} blur, faded out toward the
 *       inner side with an alpha gradient.</li>
 *   <li>Older Android, a software canvas, or Battery Saver: only the tint, which fades the content
 *       out. In Battery Saver the tint is always dark, like the scrim of the Google app.</li>
 * </ul>
 *
 * <p>Only framework APIs are used. The region draws the content views again into its own node.
 * Those views keep their own display lists, so this costs one recording and the blur on the GPU.
 */
final class ProgressiveBlurDrawable extends Drawable {

    /** Blur radius at the screen edge, as used in performance mode. */
    private static final float MAX_RADIUS_DP = 22f;
    /** The blur is full over this part of the region from the edge, then falls to zero. */
    private static final float FULL_PART = 0.45f;
    /**
     * Distance from the top of the header (status bar included) at which the top frost is
     * fully gone, as measured from the mockups.
     */
    static final float TOP_FADE_DP = 145f;
    /**
     * How far the bottom fade continues above the input bar, into the messages, so it ends
     * clear above the input instead of at its top edge.
     */
    static final float BOTTOM_FADE_DP = 28f;
    /** Samples of the eased fade. More stops give a smooth fall with no visible bands. */
    private static final int FADE_STEPS = 10;

    /** True while a region records its content. Pieces draw nothing then. */
    static boolean recording;
    /** The view whose background or overlay is drawing now, so its parents are not recorded. */
    static View drawingHost;

    /**
     * One pass of a Gaussian blur along [dir]. The distance from the edge is the y position, or
     * the height minus it when [fromBottom] is 1. The radius is [maxRadius] up to [fullUntil] from
     * the edge and falls smoothly to zero at [clearAt]. The loop bound must be a constant in AGSL,
     * so the 25 taps spread over the radius of each pixel.
     */
    static final String SHADER = ""
            + "uniform shader content;\n"
            + "uniform float2 size;\n"
            + "uniform float2 dir;\n"
            + "uniform float fromBottom;\n"
            + "uniform float maxRadius;\n"
            + "uniform float fullUntil;\n"
            + "uniform float clearAt;\n"
            + "half4 main(float2 p) {\n"
            + "    float d = mix(p.y, size.y - p.y, fromBottom);\n"
            + "    float t = clamp((d - fullUntil) / max(clearAt - fullUntil, 1.0), 0.0, 1.0);\n"
            + "    float r = maxRadius * (1.0 - t * t * (3.0 - 2.0 * t));\n"
            + "    if (r < 0.5) {\n"
            + "        return content.eval(p);\n"
            + "    }\n"
            + "    float sigma = r * 0.5;\n"
            + "    float4 sum = float4(0.0);\n"
            + "    float total = 0.0;\n"
            + "    for (int i = -12; i <= 12; i++) {\n"
            + "        float o = float(i) * r / 12.0;\n"
            + "        float w = exp(-0.5 * o * o / (sigma * sigma));\n"
            + "        float2 q = clamp(p + dir * o, float2(0.0), size - float2(1.0));\n"
            + "        sum += float4(content.eval(q)) * w;\n"
            + "        total += w;\n"
            + "    }\n"
            + "    return half4(sum / total);\n"
            + "}\n";

    private final View host;
    private final Region region;
    /** True when this drawable is the fade tail just below the header, not a region piece. */
    private final boolean tail;
    private final int[] hostLocation = new int[2];

    ProgressiveBlurDrawable(View host, Region region) {
        this(host, region, false);
    }

    ProgressiveBlurDrawable(View host, Region region, boolean tail) {
        this.host = host;
        this.region = region;
        this.tail = tail;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || recording) return;
        Rect union = region.union();
        if (union.isEmpty()) return;
        host.getLocationInWindow(hostLocation);
        if (region.bottom) {
            int above = region.bottomFadeExtension(host.getContext());
            int save = canvas.save();
            canvas.clipRect(bounds);
            canvas.translate(union.left - hostLocation[0], union.top - hostLocation[1]);
            if (tail) {
                // The tail sits just above the input and shows only the fade into the messages.
                if (hostLocation[1] + bounds.bottom > union.top - above
                        && hostLocation[1] + bounds.top < union.top) {
                    region.drawBottomTail(canvas, union.width(), union.height(), above);
                }
            } else if (hostLocation[1] + bounds.top < union.bottom
                    && hostLocation[1] + bounds.bottom > union.top) {
                // A piece outside the region, such as the gesture bar strip under an open
                // keyboard, draws nothing, so the content is not recorded for it.
                drawingHost = host;
                try {
                    region.draw(canvas, union.width(), union.height(), above);
                } finally {
                    drawingHost = null;
                }
            }
            canvas.restoreToCount(save);
            return;
        }
        int fade = region.topFadeExtension(host.getContext(), union.height());
        int fadeBottom = union.bottom + fade;
        if (tail) {
            // The tail sits just under the header and shows only the fade into the messages.
            if (hostLocation[1] + bounds.bottom <= union.bottom
                    || hostLocation[1] + bounds.top >= fadeBottom) return;
        } else if (hostLocation[1] + bounds.top >= fadeBottom
                || hostLocation[1] + bounds.bottom <= union.top) {
            return;
        }
        int save = canvas.save();
        canvas.clipRect(bounds);
        canvas.translate(union.left - hostLocation[0], union.top - hostLocation[1]);
        drawingHost = host;
        try {
            region.draw(canvas, union.width(), union.height(), fade);
        } finally {
            drawingHost = null;
            canvas.restoreToCount(save);
        }
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /**
     * The area behind one screen edge. Its pieces are views stacked along the edge. Their union is
     * blurred as one, so the pieces join without a seam.
     */
    static final class Region {
        final boolean bottom;
        /** The bottom fade goes on above the pieces (the chat input). Off for the home tab bar. */
        boolean fadeAbove = true;
        private final View[] pieces;
        private final View[] sources;
        private final Rect union = new Rect();
        private final Rect part = new Rect();
        private final int[] location = new int[2];
        private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Object gpu;
        private int tintHeight = -1;
        private int tintKey = -1;
        private int tintRgbKey = -1;
        /** The colors to use, owned and updated by the chat screen. */
        ChromeStyle style = new ChromeStyle();

        /**
         * @param bottom true for the bottom edge
         * @param pieces the views that show this region
         * @param sources the views behind the region, drawn in this order; null items are skipped
         */
        Region(boolean bottom, View[] pieces, View[] sources) {
            this.bottom = bottom;
            this.pieces = pieces;
            this.sources = sources;
        }

        /** Redraws every piece. */
        void invalidate() {
            for (View piece : pieces) {
                if (piece != null) piece.invalidate();
            }
        }

        /**
         * The union of the visible pieces, in window coordinates. A piece joins only if it touches
         * the pieces before it. When the keyboard opens in a chat, LINE puts a gap of the keyboard
         * height between the input bar and the gesture bar strip. The keyboard covers that gap and
         * the strip, so the region is then the input bar alone, and its blur ends at the keyboard.
         */
        Rect union() {
            union.setEmpty();
            for (View piece : pieces) {
                if (piece == null || !piece.isAttachedToWindow() || piece.getVisibility() != View.VISIBLE
                        || piece.getHeight() == 0) continue;
                piece.getLocationInWindow(location);
                part.set(location[0], location[1], location[0] + piece.getWidth(), location[1] + piece.getHeight());
                if (!union.isEmpty() && (part.top > union.bottom + 1 || part.bottom < union.top - 1)) continue;
                union.union(part);
            }
            return union;
        }

        /** Extra height of the top frost below the header, so the fade is gone at [TOP_FADE_DP]. */
        int topFadeExtension(android.content.Context context, int unionHeight) {
            if (bottom) return 0;
            return Math.max(0, BlurBars.dp(context, TOP_FADE_DP) - Math.max(unionHeight, 0));
        }

        /** Extra height of the bottom fade above the input bar. */
        int bottomFadeExtension(android.content.Context context) {
            return bottom && fadeAbove ? BlurBars.dp(context, BOTTOM_FADE_DP) : 0;
        }

        /**
         * Draws the region: the blur (performance mode only) and the tint. For the top region
         * [fade] is how far the frost goes below the union; for the bottom region it is how far
         * the fade goes above it (drawn by the tail, not here).
         */
        void draw(Canvas canvas, int width, int height, int fade) {
            boolean saver = BlurBars.powerSave();
            if (!saver && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
                if (gpu == null) gpu = new Gpu();
                int drawnHeight = bottom ? height : height + Math.max(fade, 0);
                ((Gpu) gpu).draw(this, canvas, width, drawnHeight);
            }
            if (bottom) {
                prepareBottomTint(width, height, fade, saver);
                canvas.drawRect(0, 0, width, height, tint);
            } else {
                drawTopTint(canvas, width, height, fade, saver);
            }
        }

        /** The part of the bottom fade above the input bar, from -[above] to 0. No blur. */
        void drawBottomTail(Canvas canvas, int width, int height, int above) {
            if (above <= 0) return;
            prepareBottomTint(width, height, above, BlurBars.powerSave());
            canvas.drawRect(0, -above, width, 0, tint);
        }

        /**
         * A frost from the status bar through the title and into the first messages, gone at
         * [TOP_FADE_DP] from the top. It holds over the status bar, then eases out. There is no
         * solid title bar.
         */
        private void drawTopTint(Canvas canvas, int width, int height, int fade, boolean saver) {
            int extended = height + Math.max(fade, 0);
            if (width <= 0 || extended <= 0) return;
            int rgb = style.topRgb;
            int edgeAlpha = style.topAlpha;
            int key = edgeAlpha;
            if (extended != tintHeight || key != tintKey || rgb != tintRgbKey) {
                tintHeight = extended;
                tintKey = key;
                tintRgbKey = rgb;
                // ~16 dp of 145 dp hold the edge alpha over the status bar.
                setFade(0, extended, 0.11f, rgb, edgeAlpha);
            }
            canvas.drawRect(0, 0, width, extended, tint);
        }

        /**
         * Strongest (solid) over the gesture bar, then easing out up through the input row and
         * [above] past it, so it is clear just above the input. Messages above are not washed.
         */
        private void prepareBottomTint(int width, int height, int above, boolean saver) {
            if (width <= 0 || height <= 0) return;
            int solid = Math.min(Math.max(gestureBarHeight(), 0), height);
            int rgb = style.bottomRgb;
            int edgeAlpha = style.bottomAlpha;
            int key = edgeAlpha | (solid << 8);
            int total = height + Math.max(above, 0);
            if (total != tintHeight || key != tintKey || rgb != tintRgbKey) {
                tintHeight = total;
                tintKey = key;
                tintRgbKey = rgb;
                setFade(height, -Math.max(above, 0), solid / (float) total, rgb, edgeAlpha);
            }
        }

        /**
         * Sets [tint] to a fade from [from] (edge alpha) to [to] (clear), holding the edge alpha
         * for the first [hold] of the way, then easing out along a cosine so it has no hard
         * start or end. The paint's own alpha stays 255: a paint with a shader still multiplies
         * by it, and a zero alpha would hide the whole gradient.
         */
        private void setFade(float from, float to, float hold, int rgb, int edgeAlpha) {
            hold = Math.max(0f, Math.min(hold, 0.95f));
            int[] colors = new int[FADE_STEPS + 2];
            float[] positions = new float[FADE_STEPS + 2];
            int base = rgb & 0x00FFFFFF;
            colors[0] = (edgeAlpha << 24) | base;
            positions[0] = 0f;
            for (int i = 0; i <= FADE_STEPS; i++) {
                float u = i / (float) FADE_STEPS;
                float a = edgeAlpha * (0.5f + 0.5f * (float) Math.cos(Math.PI * u));
                colors[i + 1] = (Math.round(a) << 24) | base;
                positions[i + 1] = hold + (1f - hold) * u;
            }
            tint.setColor(Color.BLACK);
            tint.setShader(new LinearGradient(0, from, 0, to, colors, positions, Shader.TileMode.CLAMP));
        }

        /** Height of the gesture bar strip at the bottom of this region, or 0 if it is not in it. */
        private int gestureBarHeight() {
            if (pieces.length == 0 || pieces[0] == null || !pieces[0].isAttachedToWindow()) return 0;
            View input = pieces[0];
            if (input.getVisibility() != View.VISIBLE || input.getHeight() == 0) return 0;
            input.getLocationInWindow(location);
            int solid = union.bottom - (location[1] + input.getHeight());
            if (solid <= 1) return 0;
            return Math.min(solid, Math.max(union.height(), 0));
        }

    }

    /** The parts that need Android 12 (API 31) or later. Loaded only there. */
    private static final class Gpu {
        private final RenderNode node = new RenderNode("andrewBlurBars");
        private final Paint mask = new Paint();
        private final int[] viewLocation = new int[2];
        private RenderEffect effect;
        private int effectWidth;
        private int effectHeight;

        boolean draw(Region region, Canvas canvas, int width, int height) {
            Rect union = region.union;
            node.setPosition(0, 0, width, height);
            RecordingCanvas canvasOfNode = node.beginRecording(width, height);
            recording = true;
            try {
                for (View source : region.sources) record(canvasOfNode, source, union);
            } finally {
                recording = false;
                node.endRecording();
            }
            if (effect == null || width != effectWidth || height != effectHeight) {
                effect = effect(region, width, height);
                effectWidth = width;
                effectHeight = height;
                mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                float from = region.bottom ? height : 0;
                float to = region.bottom ? 0 : height;
                mask.setShader(new LinearGradient(0, from, 0, to,
                        new int[] {Color.BLACK, Color.BLACK, Color.TRANSPARENT},
                        new float[] {0f, FULL_PART, 1f}, Shader.TileMode.CLAMP));
            }
            node.setRenderEffect(effect);

            if (Build.VERSION.SDK_INT >= 33) {
                // The radius already falls to zero at the inner side.
                canvas.drawRenderNode(node);
            } else {
                int layer = canvas.saveLayer(0, 0, width, height, null);
                canvas.drawRenderNode(node);
                canvas.drawRect(0, 0, width, height, mask);
                canvas.restoreToCount(layer);
            }
            return true;
        }

        /** Draws [view] where it is on the screen, in the coordinates of the region. */
        private void record(Canvas canvas, View view, Rect union) {
            if (view == null || view.getVisibility() != View.VISIBLE || !view.isAttachedToWindow()) return;
            // A parent of the view being drawn would make Android record that view again, inside
            // its own recording, which throws. The view itself is fine: its children are done.
            View host = drawingHost;
            if (host != null && view != host && BlurBars.isSelfOrAncestor(view, host)) return;
            view.getLocationInWindow(viewLocation);
            int save = canvas.save();
            canvas.translate(viewLocation[0] - union.left, viewLocation[1] - union.top);
            try {
                view.draw(canvas);
            } catch (IllegalStateException nested) {
                // Last resort if a view in [view] is mid-recording: skip it (only the blur behind
                // the bar misses it for one frame) rather than crash LINE.
            } finally {
                canvas.restoreToCount(save);
            }
        }

        private static RenderEffect effect(Region region, int width, int height) {
            float radius = MAX_RADIUS_DP * region.pieces[0].getResources().getDisplayMetrics().density;
            if (Build.VERSION.SDK_INT < 33) {
                return RenderEffect.createBlurEffect(radius * 0.75f, radius * 0.75f, Shader.TileMode.CLAMP);
            }
            return Agsl.effect(width, height, radius, region.bottom);
        }
    }

    /** The progressive blur of Android 13 (API 33) and later. */
    private static final class Agsl {
        static RenderEffect effect(int width, int height, float radius, boolean bottom) {
            RenderEffect across = RenderEffect.createRuntimeShaderEffect(pass(width, height, radius, bottom, 1f, 0f), "content");
            RenderEffect along = RenderEffect.createRuntimeShaderEffect(pass(width, height, radius, bottom, 0f, 1f), "content");
            // The inner effect runs first.
            return RenderEffect.createChainEffect(along, across);
        }

        private static RuntimeShader pass(int width, int height, float radius, boolean bottom, float x, float y) {
            RuntimeShader shader = new RuntimeShader(SHADER);
            shader.setFloatUniform("size", width, height);
            shader.setFloatUniform("dir", x, y);
            shader.setFloatUniform("fromBottom", bottom ? 1f : 0f);
            shader.setFloatUniform("maxRadius", radius);
            shader.setFloatUniform("fullUntil", height * FULL_PART);
            shader.setFloatUniform("clearAt", height);
            return shader;
        }
    }
}
