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
 * The background of the chat header: the content behind the header, blurred most at the top of
 * the screen and clear at the bottom edge of the header, with a light veil of the same shape.
 *
 * <ul>
 *   <li>Android 13 and later: the content is recorded into a {@link RenderNode}, and an AGSL
 *       {@link RuntimeShader} blurs it with a radius that falls from the top to zero at the bottom.
 *       It runs as two passes, across and down, chained with {@link RenderEffect}.</li>
 *   <li>Android 12: the same node with a fixed {@link RenderEffect} blur, faded out toward the
 *       bottom with an alpha gradient.</li>
 *   <li>Older Android, or a software canvas: only the veil, which fades the content out.</li>
 * </ul>
 *
 * <p>Only framework APIs are used. The drawable draws the content views again into its own node.
 * Those views keep their own display lists, so this costs one recording and the blur on the GPU.
 */
final class ProgressiveBlurDrawable extends Drawable {

    /** Blur radius at the top of the screen. */
    private static final float MAX_RADIUS_DP = 22f;
    /** The blur is full down to this part of the height, then falls to zero at the bottom. */
    private static final float FULL_PART = 0.45f;
    /** Veil opacity at the top, with and without the blur. */
    private static final int VEIL_ALPHA_BLUR = 0x73;
    private static final int VEIL_ALPHA_PLAIN = 0xD9;

    /**
     * One pass of a Gaussian blur along [dir]. The radius is [maxRadius] above [fullUntil] and
     * falls smoothly to zero at [clearAt]. The loop bound must be a constant in AGSL, so the 25
     * taps spread over the radius of each pixel.
     */
    static final String SHADER = ""
            + "uniform shader content;\n"
            + "uniform float2 size;\n"
            + "uniform float2 dir;\n"
            + "uniform float maxRadius;\n"
            + "uniform float fullUntil;\n"
            + "uniform float clearAt;\n"
            + "half4 main(float2 p) {\n"
            + "    float t = clamp((p.y - fullUntil) / max(clearAt - fullUntil, 1.0), 0.0, 1.0);\n"
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
    private final View content;
    private final View skin;
    private final Paint veil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Object gpu;
    private boolean drawing;
    private int veilHeight = -1;
    private boolean veilBlurred;

    /**
     * @param host the view that has this drawable as its background
     * @param content the content behind [host]
     * @param skin the chat background behind [content], or null
     */
    ProgressiveBlurDrawable(View host, View content, View skin) {
        this.host = host;
        this.content = content;
        this.skin = skin;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || drawing) return;
        drawing = true;
        try {
            boolean blurred = false;
            if (Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
                if (gpu == null) gpu = new Gpu();
                blurred = ((Gpu) gpu).draw(this, canvas, bounds);
            }
            drawVeil(canvas, bounds, blurred);
        } finally {
            drawing = false;
        }
    }

    private void drawVeil(Canvas canvas, Rect bounds, boolean blurred) {
        int height = bounds.height();
        if (height != veilHeight || blurred != veilBlurred) {
            veilHeight = height;
            veilBlurred = blurred;
            boolean night = (host.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            int rgb = night ? Color.BLACK : Color.WHITE;
            int alpha = blurred ? VEIL_ALPHA_BLUR : VEIL_ALPHA_PLAIN;
            int top = (rgb & 0x00FFFFFF) | (alpha << 24);
            int middle = (rgb & 0x00FFFFFF) | ((alpha * 3 / 5) << 24);
            veil.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                    new int[] {top, middle, rgb & 0x00FFFFFF},
                    new float[] {0f, FULL_PART, 1f}, Shader.TileMode.CLAMP));
        }
        canvas.drawRect(bounds, veil);
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /** The parts that need Android 12 (API 31) or later. Loaded only there. */
    private static final class Gpu {
        private final RenderNode node = new RenderNode("andrewBlurBars");
        private final Paint mask = new Paint();
        private final int[] hostLocation = new int[2];
        private final int[] viewLocation = new int[2];
        private RenderEffect effect;
        private int effectWidth;
        private int effectHeight;

        boolean draw(ProgressiveBlurDrawable drawable, Canvas canvas, Rect bounds) {
            int width = bounds.width();
            int height = bounds.height();
            drawable.host.getLocationInWindow(hostLocation);
            node.setPosition(0, 0, width, height);
            RecordingCanvas recording = node.beginRecording(width, height);
            try {
                recording.translate(-bounds.left, -bounds.top);
                record(recording, drawable.skin);
                record(recording, drawable.content);
            } finally {
                node.endRecording();
            }
            if (effect == null || width != effectWidth || height != effectHeight) {
                effect = effect(drawable.host, width, height);
                effectWidth = width;
                effectHeight = height;
                mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                mask.setShader(new LinearGradient(0, 0, 0, height,
                        new int[] {Color.BLACK, Color.BLACK, Color.TRANSPARENT},
                        new float[] {0f, FULL_PART, 1f}, Shader.TileMode.CLAMP));
            }
            node.setRenderEffect(effect);

            int save = canvas.save();
            canvas.translate(bounds.left, bounds.top);
            if (Build.VERSION.SDK_INT >= 33) {
                // The radius already falls to zero at the bottom edge.
                canvas.drawRenderNode(node);
            } else {
                int layer = canvas.saveLayer(0, 0, width, height, null);
                canvas.drawRenderNode(node);
                canvas.drawRect(0, 0, width, height, mask);
                canvas.restoreToCount(layer);
            }
            canvas.restoreToCount(save);
            return true;
        }

        /** Draws [view] where it is on the screen, in the coordinates of the host. */
        private void record(Canvas canvas, View view) {
            if (view == null || view.getVisibility() != View.VISIBLE || !view.isAttachedToWindow()) return;
            view.getLocationInWindow(viewLocation);
            int save = canvas.save();
            canvas.translate(viewLocation[0] - hostLocation[0], viewLocation[1] - hostLocation[1]);
            view.draw(canvas);
            canvas.restoreToCount(save);
        }

        private static RenderEffect effect(View host, int width, int height) {
            float radius = MAX_RADIUS_DP * host.getResources().getDisplayMetrics().density;
            if (Build.VERSION.SDK_INT < 33) {
                return RenderEffect.createBlurEffect(radius * 0.75f, radius * 0.75f, Shader.TileMode.CLAMP);
            }
            return Agsl.effect(width, height, radius);
        }
    }

    /** The progressive blur of Android 13 (API 33) and later. */
    private static final class Agsl {
        static RenderEffect effect(int width, int height, float radius) {
            RenderEffect across = RenderEffect.createRuntimeShaderEffect(pass(width, height, radius, 1f, 0f), "content");
            RenderEffect down = RenderEffect.createRuntimeShaderEffect(pass(width, height, radius, 0f, 1f), "content");
            // The inner effect runs first.
            return RenderEffect.createChainEffect(down, across);
        }

        private static RuntimeShader pass(int width, int height, float radius, float x, float y) {
            RuntimeShader shader = new RuntimeShader(SHADER);
            shader.setFloatUniform("size", width, height);
            shader.setFloatUniform("dir", x, y);
            shader.setFloatUniform("maxRadius", radius);
            shader.setFloatUniform("fullUntil", height * FULL_PART);
            shader.setFloatUniform("clearAt", height);
            return shader;
        }
    }
}
