package app.andrewliang.extension;

import android.content.res.Configuration;
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

    /** Blur radius at the screen edge. */
    private static final float MAX_RADIUS_DP = 22f;
    /** The blur is full over this part of the region from the edge, then falls to zero. */
    private static final float FULL_PART = 0.45f;
    /** Light tint opacity at the edge, with and without the blur. */
    private static final int LIGHT_ALPHA_BLUR = 0x73;
    private static final int LIGHT_ALPHA_PLAIN = 0xD9;
    /** Dark tint opacity at the edge, with and without the blur. */
    private static final int DARK_ALPHA_BLUR = 0x99;
    private static final int DARK_ALPHA_PLAIN = 0xB3;

    /** True while a region records its content. Pieces draw nothing then. */
    private static boolean recording;

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
    private final int[] hostLocation = new int[2];

    ProgressiveBlurDrawable(View host, Region region) {
        this.host = host;
        this.region = region;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || recording) return;
        Rect union = region.union();
        if (union.isEmpty()) return;
        host.getLocationInWindow(hostLocation);
        int save = canvas.save();
        canvas.clipRect(bounds);
        canvas.translate(union.left - hostLocation[0], union.top - hostLocation[1]);
        region.draw(canvas, union.width(), union.height());
        canvas.restoreToCount(save);
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
        private final View[] pieces;
        private final View[] sources;
        private final Rect union = new Rect();
        private final Rect part = new Rect();
        private final int[] location = new int[2];
        private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Object gpu;
        private int tintHeight = -1;
        private int tintKey = -1;

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

        /** The union of the visible pieces, in window coordinates. */
        Rect union() {
            union.setEmpty();
            for (View piece : pieces) {
                if (piece == null || !piece.isAttachedToWindow() || piece.getVisibility() != View.VISIBLE
                        || piece.getHeight() == 0) continue;
                piece.getLocationInWindow(location);
                part.set(location[0], location[1], location[0] + piece.getWidth(), location[1] + piece.getHeight());
                union.union(part);
            }
            return union;
        }

        void draw(Canvas canvas, int width, int height) {
            boolean saver = BlurBars.powerSave();
            boolean blurred = false;
            if (!saver && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
                if (gpu == null) gpu = new Gpu();
                blurred = ((Gpu) gpu).draw(this, canvas, width, height);
            }
            drawTint(canvas, width, height, blurred, saver);
        }

        private void drawTint(Canvas canvas, int width, int height, boolean blurred, boolean saver) {
            boolean dark = saver || (bottom ? LineSettings.blurBottomDarkTint() : LineSettings.blurTopDarkTint());
            int rgb;
            int alpha;
            if (dark) {
                rgb = Color.BLACK;
                alpha = blurred ? DARK_ALPHA_BLUR : DARK_ALPHA_PLAIN;
            } else {
                boolean night = (pieces[0].getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                rgb = night ? Color.BLACK : Color.WHITE;
                alpha = blurred ? LIGHT_ALPHA_BLUR : LIGHT_ALPHA_PLAIN;
            }
            int key = (rgb & 0x00FFFFFF) | (alpha << 24);
            if (height != tintHeight || key != tintKey) {
                tintHeight = height;
                tintKey = key;
                int edge = key;
                int middle = (rgb & 0x00FFFFFF) | ((alpha * 3 / 5) << 24);
                int clear = rgb & 0x00FFFFFF;
                float from = bottom ? height : 0;
                float to = bottom ? 0 : height;
                tint.setShader(new LinearGradient(0, from, 0, to, new int[] {edge, middle, clear},
                        new float[] {0f, FULL_PART, 1f}, Shader.TileMode.CLAMP));
            }
            canvas.drawRect(0, 0, width, height, tint);
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
            view.getLocationInWindow(viewLocation);
            int save = canvas.save();
            canvas.translate(viewLocation[0] - union.left, viewLocation[1] - union.top);
            view.draw(canvas);
            canvas.restoreToCount(save);
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
