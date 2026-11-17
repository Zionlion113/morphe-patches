package app.andrewliang.extension;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
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

final class ProgressiveBlurDrawable extends Drawable {
    static final float BOTTOM_FADE_DP = 28.0f;
    private static final int FADE_STEPS = 10;
    private static final float FULL_PART = 0.45f;
    private static final float MAX_RADIUS_DP = 22.0f;
    static final String SHADER = "uniform shader content;\nuniform float2 size;\nuniform float2 dir;\nuniform float fromBottom;\nuniform float maxRadius;\nuniform float fullUntil;\nuniform float clearAt;\nhalf4 main(float2 p) {\n    float d = mix(p.y, size.y - p.y, fromBottom);\n    float t = clamp((d - fullUntil) / max(clearAt - fullUntil, 1.0), 0.0, 1.0);\n    float r = maxRadius * (1.0 - t * t * (3.0 - 2.0 * t));\n    if (r < 0.5) {\n        return content.eval(p);\n    }\n    float sigma = r * 0.5;\n    float4 sum = float4(0.0);\n    float total = 0.0;\n    for (int i = -12; i <= 12; i++) {\n        float o = float(i) * r / 12.0;\n        float w = exp(-0.5 * o * o / (sigma * sigma));\n        float2 q = clamp(p + dir * o, float2(0.0), size - float2(1.0));\n        sum += float4(content.eval(q)) * w;\n        total += w;\n    }\n    return half4(sum / total);\n}\n";
    static final float TOP_FADE_DP = 145.0f;
    static View drawingHost;
    static boolean recording;
    private final View host;
    private final int[] hostLocation;
    private final Region region;
    private final boolean tail;

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
    }

    ProgressiveBlurDrawable(View view, Region region) {
        this(view, region, false);
    }

    ProgressiveBlurDrawable(View view, Region region, boolean z) {
        this.hostLocation = new int[2];
        this.host = view;
        this.region = region;
        this.tail = z;
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || recording) {
            return;
        }
        Rect rectUnion = this.region.union();
        if (rectUnion.isEmpty()) {
            return;
        }
        this.host.getLocationInWindow(this.hostLocation);
        boolean z = this.region.bottom;
        Region region = this.region;
        if (z) {
            int iBottomFadeExtension = region.bottomFadeExtension(this.host.getContext());
            int iSave = canvas.save();
            canvas.clipRect(bounds);
            canvas.translate(rectUnion.left - this.hostLocation[0], rectUnion.top - this.hostLocation[1]);
            boolean z2 = this.tail;
            int[] iArr = this.hostLocation;
            if (z2) {
                if (iArr[1] + bounds.bottom > rectUnion.top - iBottomFadeExtension && this.hostLocation[1] + bounds.top < rectUnion.top) {
                    this.region.drawBottomTail(canvas, rectUnion.width(), rectUnion.height(), iBottomFadeExtension);
                }
            } else if (iArr[1] + bounds.top < rectUnion.bottom && this.hostLocation[1] + bounds.bottom > rectUnion.top) {
                drawingHost = this.host;
                try {
                    this.region.draw(canvas, rectUnion.width(), rectUnion.height(), iBottomFadeExtension);
                    drawingHost = null;
                } catch (Throwable th) {
                    drawingHost = null;
                    throw th;
                }
            }
            canvas.restoreToCount(iSave);
            return;
        }
        int i = region.topFadeExtension(this.host.getContext(), rectUnion.height());
        int i2 = rectUnion.bottom + i;
        boolean z3 = this.tail;
        int[] iArr2 = this.hostLocation;
        if (z3) {
            if (iArr2[1] + bounds.bottom <= rectUnion.bottom || this.hostLocation[1] + bounds.top >= i2) {
                return;
            }
        } else if (iArr2[1] + bounds.top >= i2 || this.hostLocation[1] + bounds.bottom <= rectUnion.top) {
            return;
        }
        int iSave2 = canvas.save();
        canvas.clipRect(bounds);
        canvas.translate(rectUnion.left - this.hostLocation[0], rectUnion.top - this.hostLocation[1]);
        drawingHost = this.host;
        try {
            this.region.draw(canvas, rectUnion.width(), rectUnion.height(), i);
        } finally {
            drawingHost = null;
            canvas.restoreToCount(iSave2);
        }
    }

    static final class Region {
        final boolean bottom;
        private Object gpu;
        private final View[] pieces;
        private final View[] sources;
        boolean fadeAbove = true;
        private final Rect union = new Rect();
        private final Rect part = new Rect();
        private final int[] location = new int[2];
        private final Paint tint = new Paint(1);
        private int tintHeight = -1;
        private int tintKey = -1;
        private int tintRgbKey = -1;
        ChromeStyle style = new ChromeStyle();

        Region(boolean z, View[] viewArr, View[] viewArr2) {
            this.bottom = z;
            this.pieces = viewArr;
            this.sources = viewArr2;
        }

        void invalidate() {
            for (View view : this.pieces) {
                if (view != null) {
                    view.invalidate();
                }
            }
        }

        Rect union() {
            this.union.setEmpty();
            for (View view : this.pieces) {
                if (view != null && view.isAttachedToWindow() && view.getVisibility() == 0 && view.getHeight() != 0) {
                    view.getLocationInWindow(this.location);
                    Rect rect = this.part;
                    int[] iArr = this.location;
                    int i = iArr[0];
                    rect.set(i, iArr[1], view.getWidth() + i, this.location[1] + view.getHeight());
                    if (this.union.isEmpty() || (this.part.top <= this.union.bottom + 1 && this.part.bottom >= this.union.top - 1)) {
                        this.union.union(this.part);
                    }
                }
            }
            return this.union;
        }

        int topFadeExtension(Context context, int i) {
            if (this.bottom) {
                return 0;
            }
            return Math.max(0, BlurBars.dp(context, ProgressiveBlurDrawable.TOP_FADE_DP) - Math.max(i, 0));
        }

        int bottomFadeExtension(Context context) {
            if (this.bottom && this.fadeAbove) {
                return BlurBars.dp(context, ProgressiveBlurDrawable.BOTTOM_FADE_DP);
            }
            return 0;
        }

        void draw(Canvas canvas, int i, int i2, int i3) {
            boolean zPowerSave = BlurBars.powerSave();
            if (!zPowerSave && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
                if (this.gpu == null) {
                    this.gpu = new Gpu();
                }
                ((Gpu) this.gpu).draw(this, canvas, i, this.bottom ? i2 : Math.max(i3, 0) + i2);
            }
            if (this.bottom) {
                prepareBottomTint(i, i2, i3, zPowerSave);
                canvas.drawRect(0.0f, 0.0f, i, i2, this.tint);
            } else {
                drawTopTint(canvas, i, i2, i3, zPowerSave);
            }
        }

        void drawBottomTail(Canvas canvas, int i, int i2, int i3) {
            if (i3 <= 0) {
                return;
            }
            prepareBottomTint(i, i2, i3, BlurBars.powerSave());
            canvas.drawRect(0.0f, -i3, i, 0.0f, this.tint);
        }

        private void drawTopTint(Canvas canvas, int i, int i2, int i3, boolean z) {
            Region region;
            int iMax = i2 + Math.max(i3, 0);
            if (i <= 0 || iMax <= 0) {
                return;
            }
            int i4 = this.style.topRgb;
            int i5 = this.style.topAlpha;
            if (iMax == this.tintHeight && i5 == this.tintKey && i4 == this.tintRgbKey) {
                region = this;
            } else {
                this.tintHeight = iMax;
                this.tintKey = i5;
                this.tintRgbKey = i4;
                region = this;
                region.setFade(0.0f, iMax, 0.11f, i4, i5);
            }
            canvas.drawRect(0.0f, 0.0f, i, iMax, region.tint);
        }

        private void prepareBottomTint(int i, int i2, int i3, boolean z) {
            if (i <= 0 || i2 <= 0) {
                return;
            }
            int iMin = Math.min(Math.max(gestureBarHeight(), 0), i2);
            int i4 = this.style.bottomRgb;
            int i5 = this.style.bottomAlpha;
            int i6 = (iMin << 8) | i5;
            int iMax = Math.max(i3, 0) + i2;
            if (iMax == this.tintHeight && i6 == this.tintKey && i4 == this.tintRgbKey) {
                return;
            }
            this.tintHeight = iMax;
            this.tintKey = i6;
            this.tintRgbKey = i4;
            setFade(i2, -Math.max(i3, 0), iMin / iMax, i4, i5);
        }

        private void setFade(float f, float f2, float f3, int i, int i2) {
            float fMax = Math.max(0.0f, Math.min(f3, 0.95f));
            int[] iArr = new int[12];
            float[] fArr = new float[12];
            int i3 = i & 16777215;
            int i4 = 0;
            iArr[0] = (i2 << 24) | i3;
            fArr[0] = 0.0f;
            while (i4 <= ProgressiveBlurDrawable.FADE_STEPS) {
                float f4 = i4 / 10.0f;
                i4++;
                iArr[i4] = (Math.round(i2 * ((((float) Math.cos(((double) f4) * 3.141592653589793d)) * 0.5f) + 0.5f)) << 24) | i3;
                fArr[i4] = ((1.0f - fMax) * f4) + fMax;
            }
            this.tint.setColor(-16777216);
            this.tint.setShader(new LinearGradient(0.0f, f, 0.0f, f2, iArr, fArr, Shader.TileMode.CLAMP));
        }

        private int gestureBarHeight() {
            View view;
            View[] viewArr = this.pieces;
            if (viewArr.length != 0 && (view = viewArr[0]) != null && view.isAttachedToWindow()) {
                View view2 = this.pieces[0];
                if (view2.getVisibility() == 0 && view2.getHeight() != 0) {
                    view2.getLocationInWindow(this.location);
                    int height = this.union.bottom - (this.location[1] + view2.getHeight());
                    if (height <= 1) {
                        return 0;
                    }
                    return Math.min(height, Math.max(this.union.height(), 0));
                }
            }
            return 0;
        }
    }

    private static final class Gpu {
        private RenderEffect effect;
        private int effectHeight;
        private int effectWidth;
        private final Paint mask;
        private final RenderNode node;
        private final int[] viewLocation;

        private Gpu() {
            this.node = new RenderNode("andrewBlurBars");
            this.mask = new Paint();
            this.viewLocation = new int[2];
        }

        boolean draw(Region region, Canvas canvas, int i, int i2) {
            Rect rect = region.union;
            this.node.setPosition(0, 0, i, i2);
            RecordingCanvas recordingCanvasBeginRecording = this.node.beginRecording(i, i2);
            ProgressiveBlurDrawable.recording = true;
            try {
                for (View view : region.sources) {
                    record(recordingCanvasBeginRecording, view, rect);
                }
                ProgressiveBlurDrawable.recording = false;
                this.node.endRecording();
                if (this.effect == null || i != this.effectWidth || i2 != this.effectHeight) {
                    this.effect = effect(region, i, i2);
                    this.effectWidth = i;
                    this.effectHeight = i2;
                    this.mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                    this.mask.setShader(new LinearGradient(0.0f, region.bottom ? i2 : 0.0f, 0.0f, region.bottom ? 0.0f : i2, new int[]{-16777216, -16777216, 0}, new float[]{0.0f, ProgressiveBlurDrawable.FULL_PART, 1.0f}, Shader.TileMode.CLAMP));
                }
                this.node.setRenderEffect(this.effect);
                if (Build.VERSION.SDK_INT >= 33) {
                    canvas.drawRenderNode(this.node);
                } else {
                    float f = i;
                    float f2 = i2;
                    int iSaveLayer = canvas.saveLayer(0.0f, 0.0f, f, f2, null);
                    canvas.drawRenderNode(this.node);
                    canvas.drawRect(0.0f, 0.0f, f, f2, this.mask);
                    canvas.restoreToCount(iSaveLayer);
                }
                return true;
            } catch (Throwable th) {
                ProgressiveBlurDrawable.recording = false;
                this.node.endRecording();
                throw th;
            }
        }

        private void record(Canvas canvas, View view, Rect rect) {
            if (view != null && view.getVisibility() == 0 && view.isAttachedToWindow()) {
                View view2 = ProgressiveBlurDrawable.drawingHost;
                if (view2 == null || view == view2 || !BlurBars.isSelfOrAncestor(view, view2)) {
                    view.getLocationInWindow(this.viewLocation);
                    int iSave = canvas.save();
                    canvas.translate(this.viewLocation[0] - rect.left, this.viewLocation[1] - rect.top);
                    try {
                        view.draw(canvas);
                    } catch (IllegalStateException unused) {
                    } finally {
                        canvas.restoreToCount(iSave);
                    }
                }
            }
        }

        private static RenderEffect effect(Region region, int i, int i2) {
            float f = region.pieces[0].getResources().getDisplayMetrics().density * ProgressiveBlurDrawable.MAX_RADIUS_DP;
            if (Build.VERSION.SDK_INT < 33) {
                float f2 = f * 0.75f;
                return RenderEffect.createBlurEffect(f2, f2, Shader.TileMode.CLAMP);
            }
            return Agsl.effect(i, i2, f, region.bottom);
        }
    }

    private static final class Agsl {
        private Agsl() {
        }

        static RenderEffect effect(int i, int i2, float f, boolean z) {
            return RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(pass(i, i2, f, z, 0.0f, 1.0f), "content"), RenderEffect.createRuntimeShaderEffect(pass(i, i2, f, z, 1.0f, 0.0f), "content"));
        }

        private static RuntimeShader pass(int i, int i2, float f, boolean z, float f2, float f3) {
            RuntimeShader runtimeShaderM23m = new RuntimeShader(ProgressiveBlurDrawable.SHADER);
            float f4 = i2;
            runtimeShaderM23m.setFloatUniform("size", i, f4);
            runtimeShaderM23m.setFloatUniform("dir", f2, f3);
            runtimeShaderM23m.setFloatUniform("fromBottom", z ? 1.0f : 0.0f);
            runtimeShaderM23m.setFloatUniform("maxRadius", f);
            runtimeShaderM23m.setFloatUniform("fullUntil", ProgressiveBlurDrawable.FULL_PART * f4);
            runtimeShaderM23m.setFloatUniform("clearAt", f4);
            return runtimeShaderM23m;
        }
    }
}
