package app.andrewliang.extension;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.ImageView;
import kotlin.KotlinVersion;

final class SkinPalette {
    private static final float BOTTOM_DP = 110.0f;
    private static final int SAMPLE_WIDTH = 32;
    private static final float TOP_DP = 145.0f;
    private Bitmap bitmap;
    private boolean checked;
    private Drawable drawable;
    private int generation;
    private int height;
    private boolean pending;
    boolean valid;
    private int width;
    int top = -16777216;
    int bottom = -16777216;
    private final Rect bounds = new Rect();

    SkinPalette() {
    }

    void check(final View view, boolean z, final Runnable runnable) {
        if (view == null) {
            return;
        }
        Drawable drawableDrawableOf = z ? drawableOf(view) : null;
        int generationId = 0;
        if (drawableDrawableOf == null) {
            if (this.valid || !this.checked) {
                this.valid = false;
                this.checked = true;
                this.drawable = null;
                this.bitmap = null;
                if (runnable != null) {
                    runnable.run();
                    return;
                }
                return;
            }
            return;
        }
        Bitmap bitmap = drawableDrawableOf instanceof BitmapDrawable ? ((BitmapDrawable) drawableDrawableOf).getBitmap() : null;
        if (bitmap != null && !bitmap.isRecycled()) {
            generationId = bitmap.getGenerationId();
        }
        if ((this.checked && drawableDrawableOf == this.drawable && bitmap == this.bitmap && generationId == this.generation && drawableDrawableOf.getBounds().equals(this.bounds) && view.getWidth() == this.width && view.getHeight() == this.height) || this.pending) {
            return;
        }
        this.pending = true;
        view.post(() -> runCheck(view, runnable));
    }

    private void runCheck(View view, Runnable runnable) {
        boolean z = false;
        this.pending = false;
        Drawable drawableDrawableOf = drawableOf(view);
        this.drawable = drawableDrawableOf;
        Bitmap bitmap = drawableDrawableOf instanceof BitmapDrawable ? ((BitmapDrawable) drawableDrawableOf).getBitmap() : null;
        this.bitmap = bitmap;
        this.generation = (bitmap == null || bitmap.isRecycled()) ? 0 : this.bitmap.getGenerationId();
        if (drawableDrawableOf != null) {
            this.bounds.set(drawableDrawableOf.getBounds());
        }
        this.width = view.getWidth();
        this.height = view.getHeight();
        this.checked = true;
        if (drawableDrawableOf != null && sample(view, drawableDrawableOf)) {
            z = true;
        }
        this.valid = z;
        if (runnable != null) {
            runnable.run();
        }
    }

    private static Drawable drawableOf(View view) {
        Drawable drawable = view instanceof ImageView ? ((ImageView) view).getDrawable() : null;
        return drawable != null ? drawable : view.getBackground();
    }

    private boolean sample(View view, Drawable drawable) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width > 0 && height > 0) {
            float f = width;
            int iMax = Math.max(8, Math.round((height * 32) / f));
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(32, iMax, Bitmap.Config.ARGB_8888);
            try {
                Canvas canvas = new Canvas(bitmapCreateBitmap);
                float f2 = 32.0f / f;
                float f3 = iMax;
                float f4 = height;
                canvas.scale(f2, f3 / f4);
                try {
                    drawable.draw(canvas);
                } catch (Throwable unused) {
                    bitmapCreateBitmap.eraseColor(0);
                    if (!drawSoftware(canvas, drawable)) {
                        bitmapCreateBitmap.recycle();
                        return false;
                    }
                }
                int[] iArr = new int[32 * iMax];
                bitmapCreateBitmap.getPixels(iArr, 0, 32, 0, 0, 32, iMax);
                float f5 = view.getResources().getDisplayMetrics().density;
                int iClampRows = clampRows(Math.round(((TOP_DP * f5) * f3) / f4), iMax);
                int iClampRows2 = clampRows(Math.round(((f5 * BOTTOM_DP) * f3) / f4), iMax);
                int iAverage = average(iArr, 32, 0, iClampRows);
                int iAverage2 = average(iArr, 32, iMax - iClampRows2, iMax);
                if (iAverage != 0 && iAverage2 != 0) {
                    this.top = iAverage;
                    this.bottom = iAverage2;
                    bitmapCreateBitmap.recycle();
                    return true;
                }
                bitmapCreateBitmap.recycle();
                return false;
            } catch (Throwable unused2) {
                bitmapCreateBitmap.recycle();
            }
        }
        return false;
    }

    private static int clampRows(int i, int i2) {
        return Math.max(1, Math.min(i, i2));
    }

    private static int average(int[] iArr, int i, int i2, int i3) {
        long j = 0;
        long j2 = 0;
        long j3 = 0;
        int i4 = i2;
        long j4 = 0;
        while (true) {
            int i5 = 0;
            if (i4 >= i3) {
                break;
            }
            while (i5 < i) {
                int i6 = iArr[(i4 * i) + i5];
                int i7 = i6 >>> 24;
                if (i7 != 0) {
                    long j5 = i7;
                    j += ((long) ((i6 >> 16) & KotlinVersion.MAX_COMPONENT_VALUE)) * j5;
                    j2 += ((long) ((i6 >> 8) & KotlinVersion.MAX_COMPONENT_VALUE)) * j5;
                    j3 += ((long) (i6 & KotlinVersion.MAX_COMPONENT_VALUE)) * j5;
                    j4 += j5;
                }
                i5++;
                i4 = i4;
            }
            i4++;
        }
        if (j4 < ((long) i) * 255) {
            return 0;
        }
        return Color.rgb((int) (j / j4), (int) (j2 / j4), (int) (j3 / j4));
    }

    private static boolean drawSoftware(Canvas canvas, Drawable drawable) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            boolean z = false;
            for (int i = 0; i < layerDrawable.getNumberOfLayers(); i++) {
                Drawable drawable2 = layerDrawable.getDrawable(i);
                if (drawable2 != null && drawSoftware(canvas, drawable2)) {
                    z = true;
                }
            }
            return z;
        }
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap == null || bitmap.isRecycled()) {
                return false;
            }
            Bitmap bitmapCopy = bitmap.getConfig() == Bitmap.Config.HARDWARE ? bitmap.copy(Bitmap.Config.ARGB_8888, false) : bitmap;
            if (bitmapCopy == null) {
                return false;
            }
            canvas.drawBitmap(bitmapCopy, (Rect) null, drawable.getBounds(), new Paint(2));
            if (bitmapCopy != bitmap) {
                bitmapCopy.recycle();
            }
            return true;
        }
        try {
            drawable.draw(canvas);
            return true;
        } catch (Throwable unused) {
            return false;
        }
    }
}
